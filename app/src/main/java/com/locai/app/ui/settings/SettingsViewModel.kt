package com.locai.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.locai.app.LocAiContainer
import com.locai.app.data.prefs.AppPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val modelDisplayName: String = "",
    val modelFileName: String = "",
    val modelSizeMb: Long = 0L,
    val modelOnDisk: Boolean = false,
    val temperature: Float = AppPreferences.DEFAULT_TEMPERATURE,
    val maxTokens: Int = AppPreferences.DEFAULT_MAX_TOKENS,
    val topK: Int = AppPreferences.DEFAULT_TOP_K,
    val historyCleared: Boolean = false,
    val modelDeleted: Boolean = false
)

class SettingsViewModel(private val container: LocAiContainer) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch { refreshModelInfo() }

        viewModelScope.launch {
            combine(
                container.preferences.temperature,
                container.preferences.maxTokens,
                container.preferences.topK
            ) { temperature, maxTokens, topK -> Triple(temperature, maxTokens, topK) }
                .collect { (temperature, maxTokens, topK) ->
                    _uiState.update { it.copy(temperature = temperature, maxTokens = maxTokens, topK = topK) }
                }
        }
    }

    private suspend fun refreshModelInfo() {
        val option = container.selectedModelOption()
        val file = container.modelFile(option)
        val onDisk = file.exists()
        _uiState.update {
            it.copy(
                modelDisplayName = option.displayName,
                modelFileName = option.fileName,
                modelSizeMb = if (onDisk) file.length() / (1024 * 1024) else 0L,
                modelOnDisk = onDisk
            )
        }
    }

    fun setTemperature(value: Float) {
        _uiState.update { it.copy(temperature = value) }
        viewModelScope.launch { container.preferences.setTemperature(value) }
    }

    fun setMaxTokens(value: Int) {
        _uiState.update { it.copy(maxTokens = value) }
        viewModelScope.launch { container.preferences.setMaxTokens(value) }
    }

    fun setTopK(value: Int) {
        _uiState.update { it.copy(topK = value) }
        viewModelScope.launch { container.preferences.setTopK(value) }
    }

    fun clearHistory() {
        viewModelScope.launch {
            container.chatRepository.observeAllConversations().first().forEach { conversation ->
                container.chatRepository.deleteConversation(conversation)
            }
            _uiState.update { it.copy(historyCleared = true) }
        }
    }

    fun acknowledgeHistoryCleared() {
        _uiState.update { it.copy(historyCleared = false) }
    }

    /** Deletes the cached weights and resets the "downloaded" flag so the setup screen reappears. */
    fun deleteModel() {
        viewModelScope.launch {
            val file = container.modelFile(container.selectedModelOption())
            container.modelManager.unload()
            if (file.exists()) file.delete()
            container.preferences.setModelDownloaded(false)
            _uiState.update { it.copy(modelOnDisk = false, modelSizeMb = 0L, modelDeleted = true) }
        }
    }
}
