package com.locai.app.data.llm

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Owns the single on-device [LlmInference] instance. Loading a ~500MB model is too expensive to
 * do per-message, so it's loaded once lazily and reused for every chat turn for the app's
 * lifetime; a fresh [LlmInferenceSession] is created per generation (that's how sampling
 * parameters like topK/temperature are applied per-request).
 */
class LlmModelManager(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val lock = Mutex()
    private var llmInference: LlmInference? = null
    private var loadedModelPath: String? = null

    val isLoaded: Boolean
        get() = llmInference != null

    suspend fun ensureLoaded(modelFile: File): Result<Unit> = withContext(ioDispatcher) {
        lock.withLock {
            if (llmInference != null && loadedModelPath == modelFile.absolutePath) {
                return@withLock Result.success(Unit)
            }
            runCatching {
                llmInference?.close()
                llmInference = null
                loadedModelPath = null

                val options = LlmInference.LlmInferenceOptions.builder()
                    .setModelPath(modelFile.absolutePath)
                    .setMaxTokens(MAX_CONTEXT_TOKENS)
                    .build()
                llmInference = LlmInference.createFromOptions(context, options)
                loadedModelPath = modelFile.absolutePath
            }
        }
    }

    /**
     * Streams the model's reply for [prompt]. Each emitted value is the full reply accumulated
     * so far (MediaPipe hands back incremental chunks; we concatenate them for an easy-to-render
     * "typing" effect in the UI). Completes when the model reports it's done.
     */
    fun generateResponseStream(prompt: String, topK: Int, topP: Float, temperature: Float): Flow<String> =
        callbackFlow {
            val inference = llmInference
            if (inference == null) {
                close(IllegalStateException("Model is not loaded yet"))
                return@callbackFlow
            }

            val sessionOptions = LlmInferenceSession.LlmInferenceSessionOptions.builder()
                .setTopK(topK.coerceAtLeast(1))
                .setTopP(topP)
                .setTemperature(temperature)
                .build()

            val session = LlmInferenceSession.createFromOptions(inference, sessionOptions)
            val accumulated = StringBuilder()
            try {
                session.addQueryChunk(prompt)
                session.generateResponseAsync { partialResult, done ->
                    accumulated.append(partialResult)
                    val text = normalizeEscapedWhitespace(accumulated.toString())
                    trySend(text)
                    if (done) {
                        // If the model produced nothing but whitespace (safety filter fired),
                        // emit a plain fallback rather than showing a blank bubble.
                        if (text.isBlank()) {
                            trySend("I don't have specific information on that topic in my current knowledge. Could you rephrase your question or ask something related?")
                        }
                        close()
                    }
                }
            } catch (t: Throwable) {
                close(t)
            }

            awaitClose { session.close() }
        }.flowOn(ioDispatcher)

    suspend fun unload() = withContext(ioDispatcher) {
        lock.withLock {
            llmInference?.close()
            llmInference = null
            loadedModelPath = null
        }
    }

    /**
     * The model sometimes emits literal backslash-escape sequences (`\n`, `\t`, `\r\n`) as plain
     * text rather than real line breaks — Compose's [androidx.compose.material3.Text] then shows
     * them verbatim as "\n" instead of wrapping the line. Turning them back into real whitespace
     * here, on the accumulated stream, makes every reply render the way the model intended.
     */
    private fun normalizeEscapedWhitespace(text: String): String =
        text.replace("\\r\\n", "\n")
            .replace("\\n", "\n")
            .replace("\\t", "\t")

    companion object {
        // This budgets the WHOLE session — persona + general guidance + retrieved history +
        // recent turns + the new question AND the model's reply all draw from the same pool.
        // At 2048 a longer-running chat (lots of recent turns, retrieved snippets, a meaty
        // question like "help me with this assignment...") could fill the entire budget on
        // input alone, leaving the model no tokens left to generate — which produced the
        // empty / blank-newline replies. Gemma 3 1B comfortably supports far larger contexts,
        // so size this generously enough that there's always real room left to answer.
        private const val MAX_CONTEXT_TOKENS = 4096
    }
}
