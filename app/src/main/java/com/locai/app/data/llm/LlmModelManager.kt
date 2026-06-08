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
                    trySend(accumulated.toString())
                    if (done) {
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

    companion object {
        /** Generous context window: persona + retrieved history + recent turns + new question + reply. */
        private const val MAX_CONTEXT_TOKENS = 2048
    }
}
