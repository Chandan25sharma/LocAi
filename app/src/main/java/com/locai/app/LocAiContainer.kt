package com.locai.app

import android.content.Context
import com.locai.app.data.db.LocAiDatabase
import com.locai.app.data.llm.GeneratorModelOption
import com.locai.app.data.llm.ImageGeneratorManager
import com.locai.app.data.llm.LlmModelManager
import com.locai.app.data.llm.ModelDownloader
import com.locai.app.data.llm.ModelOption
import com.locai.app.data.llm.VisionModelManager
import com.locai.app.data.llm.VisionModelOption
import com.locai.app.data.llm.DownloadProgress
import com.locai.app.data.prefs.AppPreferences
import com.locai.app.data.repository.ChatRepository
import com.locai.app.data.retrieval.HistoryRetriever
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import java.io.File

/**
 * Minimal hand-rolled dependency container (the app is small enough that a DI framework would
 * be more ceremony than help). Built once in [LocAiApplication] and handed to ViewModels.
 */
class LocAiContainer(private val appContext: Context) {

    val preferences: AppPreferences by lazy { AppPreferences(appContext) }
    val modelDownloader: ModelDownloader by lazy { ModelDownloader() }

    /** Shared progress state written by [com.locai.app.service.ModelDownloadService] and read by ViewModels. */
    val textDownloadProgress: MutableStateFlow<DownloadProgress?> = MutableStateFlow(null)
    val visionDownloadProgress: MutableStateFlow<DownloadProgress?> = MutableStateFlow(null)
    val modelManager: LlmModelManager by lazy { LlmModelManager(appContext) }
    val visionModelManager: VisionModelManager by lazy { VisionModelManager(appContext) }
    val imageGeneratorManager: ImageGeneratorManager by lazy { ImageGeneratorManager(appContext) }

    private val database by lazy { LocAiDatabase.getInstance(appContext) }
    private val historyRetriever by lazy { HistoryRetriever(database.messageDao()) }

    val chatRepository: ChatRepository by lazy {
        ChatRepository(
            conversationDao = database.conversationDao(),
            messageDao = database.messageDao(),
            modelManager = modelManager,
            visionModelManager = visionModelManager,
            historyRetriever = historyRetriever,
            preferences = preferences,
            imagesDir = File(appContext.filesDir, "images")
        )
    }

    /** Where a given model's weights are cached, in app-internal storage (no permissions needed). */
    fun modelFile(option: ModelOption): File =
        File(File(appContext.filesDir, "models"), option.fileName)

    /** The model the user picked in Setup (falls back to [ModelOption.DEFAULT] if none chosen yet). */
    suspend fun selectedModelOption(): ModelOption =
        ModelOption.byId(preferences.selectedModelId.first())

    /** Convenience for the common case of "the file for whichever model is currently selected". */
    suspend fun activeModelFile(): File = modelFile(selectedModelOption())

    /** Where the optional vision model's weights are cached, kept apart from the text models. */
    val visionModelFile: File by lazy {
        File(File(appContext.filesDir, "models/vision"), VisionModelOption.FILE_NAME)
    }

    /** The directory where the user must manually place converted SD model files. */
    val generatorModelDir: File by lazy {
        File(File(appContext.filesDir, "models"), GeneratorModelOption.DIR_NAME)
    }
}
