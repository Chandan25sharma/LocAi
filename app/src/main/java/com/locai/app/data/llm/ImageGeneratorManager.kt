package com.locai.app.data.llm

import android.content.Context
import android.graphics.Bitmap
import com.google.mediapipe.framework.image.BitmapExtractor
import com.google.mediapipe.tasks.vision.imagegenerator.ImageGenerator
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

class ImageGeneratorManager(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val lock = Mutex()
    private var generator: ImageGenerator? = null
    private var loadedDirPath: String? = null

    val isLoaded: Boolean get() = generator != null

    /** Returns true only when all [GeneratorModelOption.REQUIRED_FILES] exist in [modelDir]. */
    fun isInstalled(modelDir: File): Boolean =
        modelDir.isDirectory && GeneratorModelOption.REQUIRED_FILES.all { File(modelDir, it).exists() }

    suspend fun ensureLoaded(modelDir: File): Result<Unit> = withContext(ioDispatcher) {
        lock.withLock {
            if (generator != null && loadedDirPath == modelDir.absolutePath) {
                return@withLock Result.success(Unit)
            }
            runCatching {
                generator?.close()
                generator = null
                loadedDirPath = null

                val options = ImageGenerator.ImageGeneratorOptions.builder()
                    .setImageGeneratorModelDirectory(modelDir.absolutePath)
                    .build()
                generator = ImageGenerator.createFromOptions(context, options)
                loadedDirPath = modelDir.absolutePath
            }
        }
    }

    /**
     * Generates an image from [prompt]. Runs on [ioDispatcher] — generation can take tens of
     * seconds to minutes on device; do not call from the main thread.
     */
    suspend fun generate(
        prompt: String,
        iterations: Int = DEFAULT_ITERATIONS,
        seed: Int = DEFAULT_SEED
    ): Result<Bitmap> = withContext(ioDispatcher) {
        val gen = generator
            ?: return@withContext Result.failure(IllegalStateException("Image generator is not loaded"))
        runCatching {
            val result = gen.generate(prompt, iterations, seed)
            BitmapExtractor.extract(result.generatedImage())
        }
    }

    suspend fun unload() = withContext(ioDispatcher) {
        lock.withLock {
            generator?.close()
            generator = null
            loadedDirPath = null
        }
    }

    companion object {
        const val DEFAULT_ITERATIONS = 20
        const val DEFAULT_SEED = 0
    }
}
