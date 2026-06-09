package com.locai.app.data.llm

import android.content.Context
import android.graphics.Bitmap
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.genai.llminference.GraphOptions
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
 * Owns the optional on-device multimodal model used for "attach a photo and ask about it".
 * Mirrors [LlmModelManager]'s lifecycle (single lazily-loaded [LlmInference], fresh
 * [LlmInferenceSession] per turn) but additionally enables MediaPipe's vision modality so an
 * image can ride alongside the text prompt.
 */
class VisionModelManager(
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
                    .setMaxNumImages(MAX_IMAGES_PER_SESSION)
                    .build()
                llmInference = LlmInference.createFromOptions(context, options)
                loadedModelPath = modelFile.absolutePath
            }
        }
    }

    /**
     * Streams the model's reply for [prompt], optionally about [image]. Each emitted value is
     * the full reply accumulated so far, matching [LlmModelManager.generateResponseStream] so the
     * chat UI can treat both the same way.
     */
    fun generateResponseStream(
        prompt: String,
        image: Bitmap?,
        topK: Int,
        topP: Float,
        temperature: Float
    ): Flow<String> = callbackFlow {
        val inference = llmInference
        if (inference == null) {
            close(IllegalStateException("Vision model is not loaded yet"))
            return@callbackFlow
        }

        val sessionOptions = LlmInferenceSession.LlmInferenceSessionOptions.builder()
            .setTopK(topK.coerceAtLeast(1))
            .setTopP(topP)
            .setTemperature(temperature)
            .setGraphOptions(GraphOptions.builder().setEnableVisionModality(image != null).build())
            .build()

        val session = LlmInferenceSession.createFromOptions(inference, sessionOptions)
        val accumulated = StringBuilder()
        try {
            if (image != null) {
                // BitmapImageBuilder requires ARGB_8888; copy if the bitmap uses a different config
                // (e.g. HARDWARE that slipped through, or RGB_565 from older devices).
                val safeImage = if (image.config == Bitmap.Config.ARGB_8888) image
                                else image.copy(Bitmap.Config.ARGB_8888, false)
                session.addImage(BitmapImageBuilder(safeImage).build())
            }
            session.addQueryChunk(prompt)
            session.generateResponseAsync { partialResult, done ->
                accumulated.append(partialResult)
                trySend(normalizeEscapedWhitespace(accumulated.toString()))
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

    /** Same literal-escape cleanup as [LlmModelManager] — small instruct models emit `\n` as text. */
    private fun normalizeEscapedWhitespace(text: String): String =
        text.replace("\\r\\n", "\n")
            .replace("\\n", "\n")
            .replace("\\t", "\t")

    companion object {
        private const val MAX_CONTEXT_TOKENS = 4096
        private const val MAX_IMAGES_PER_SESSION = 1
    }
}
