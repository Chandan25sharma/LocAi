package com.locai.app

import android.content.Context
import com.locai.app.data.db.LocAiDatabase
import com.locai.app.data.llm.LlmModelManager
import com.locai.app.data.llm.ModelDownloader
import com.locai.app.data.llm.ModelOption
import com.locai.app.data.prefs.AppPreferences
import com.locai.app.data.repository.ChatRepository
import com.locai.app.data.retrieval.HistoryRetriever
import kotlinx.coroutines.flow.first
import java.io.File

/**
 * Minimal hand-rolled dependency container (the app is small enough that a DI framework would
 * be more ceremony than help). Built once in [LocAiApplication] and handed to ViewModels.
 */
class LocAiContainer(private val appContext: Context) {

    val preferences: AppPreferences by lazy { AppPreferences(appContext) }
    val modelDownloader: ModelDownloader by lazy { ModelDownloader() }
    val modelManager: LlmModelManager by lazy { LlmModelManager(appContext) }

    private val database by lazy { LocAiDatabase.getInstance(appContext) }
    private val historyRetriever by lazy { HistoryRetriever(database.messageDao()) }

    val chatRepository: ChatRepository by lazy {
        ChatRepository(
            conversationDao = database.conversationDao(),
            messageDao = database.messageDao(),
            modelManager = modelManager,
            historyRetriever = historyRetriever,
            preferences = preferences
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
}
