package com.locai.app.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "locai_prefs")

/**
 * Local-only settings: model download state and generation parameters.
 * Nothing here ever leaves the device.
 */
class AppPreferences(private val context: Context) {

    private object Keys {
        val MODEL_DOWNLOADED = booleanPreferencesKey("model_downloaded")
        val SELECTED_MODEL_ID = stringPreferencesKey("selected_model_id")
        val TEMPERATURE = floatPreferencesKey("temperature")
        val MAX_TOKENS = intPreferencesKey("max_tokens")
        val TOP_K = intPreferencesKey("top_k")
    }

    val isModelDownloaded: Flow<Boolean> = context.dataStore.data.map {
        it[Keys.MODEL_DOWNLOADED] ?: false
    }

    /** The id of the [com.locai.app.data.llm.ModelOption] the user picked in Setup, if any. */
    val selectedModelId: Flow<String?> = context.dataStore.data.map {
        it[Keys.SELECTED_MODEL_ID]
    }

    val temperature: Flow<Float> = context.dataStore.data.map {
        it[Keys.TEMPERATURE] ?: DEFAULT_TEMPERATURE
    }

    val maxTokens: Flow<Int> = context.dataStore.data.map {
        it[Keys.MAX_TOKENS] ?: DEFAULT_MAX_TOKENS
    }

    val topK: Flow<Int> = context.dataStore.data.map {
        it[Keys.TOP_K] ?: DEFAULT_TOP_K
    }

    suspend fun setModelDownloaded(downloaded: Boolean) {
        context.dataStore.edit { it[Keys.MODEL_DOWNLOADED] = downloaded }
    }

    suspend fun setSelectedModelId(id: String) {
        context.dataStore.edit { it[Keys.SELECTED_MODEL_ID] = id }
    }

    suspend fun setTemperature(value: Float) {
        context.dataStore.edit { it[Keys.TEMPERATURE] = value }
    }

    suspend fun setMaxTokens(value: Int) {
        context.dataStore.edit { it[Keys.MAX_TOKENS] = value }
    }

    suspend fun setTopK(value: Int) {
        context.dataStore.edit { it[Keys.TOP_K] = value }
    }

    companion object {
        const val DEFAULT_TEMPERATURE = 0.8f
        const val DEFAULT_MAX_TOKENS = 1024
        const val DEFAULT_TOP_K = 40
    }
}
