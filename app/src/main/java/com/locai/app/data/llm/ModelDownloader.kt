package com.locai.app.data.llm

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

sealed interface DownloadProgress {
    data class InProgress(val bytesDownloaded: Long, val totalBytes: Long) : DownloadProgress
    data class Completed(val file: File) : DownloadProgress
    data class Failed(val message: String) : DownloadProgress
}

/**
 * One-time, resumable-by-retry download of the (openly-licensed, ~520MB) model weights —
 * no account, login, or token required, similar to pulling a model with Ollama. This is the
 * only network operation LocAi ever performs — once the file lands in app-internal storage,
 * the model runs fully offline forever after.
 */
class ModelDownloader {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(2, TimeUnit.MINUTES)
        .build()

    fun download(url: String, destination: File): Flow<DownloadProgress> = flow {
        val partFile = File(destination.parentFile, "${destination.name}.part")
        try {
            val request = Request.Builder().url(url).build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    emit(DownloadProgress.Failed("Download failed: HTTP ${response.code}"))
                    return@flow
                }

                val body = response.body
                if (body == null) {
                    emit(DownloadProgress.Failed("Download failed: empty response from server"))
                    return@flow
                }

                val totalBytes = body.contentLength()
                destination.parentFile?.mkdirs()

                body.byteStream().use { input ->
                    partFile.outputStream().use { output ->
                        val buffer = ByteArray(BUFFER_SIZE)
                        var bytesCopied = 0L
                        var lastEmitted = 0L
                        var read = input.read(buffer)
                        while (read >= 0) {
                            if (read > 0) {
                                output.write(buffer, 0, read)
                                bytesCopied += read
                            }
                            if (bytesCopied - lastEmitted >= PROGRESS_STEP_BYTES) {
                                emit(DownloadProgress.InProgress(bytesCopied, totalBytes))
                                lastEmitted = bytesCopied
                            }
                            read = input.read(buffer)
                        }
                        emit(DownloadProgress.InProgress(bytesCopied, totalBytes))
                    }
                }
            }

            if (destination.exists()) destination.delete()
            if (!partFile.renameTo(destination)) {
                emit(DownloadProgress.Failed("Could not finalize the downloaded model file"))
                return@flow
            }
            emit(DownloadProgress.Completed(destination))
        } catch (io: IOException) {
            partFile.delete()
            emit(DownloadProgress.Failed(io.message ?: "Network error while downloading the model"))
        }
    }.flowOn(Dispatchers.IO)

    companion object {
        private const val BUFFER_SIZE = 64 * 1024
        private const val PROGRESS_STEP_BYTES = 512 * 1024L
    }
}
