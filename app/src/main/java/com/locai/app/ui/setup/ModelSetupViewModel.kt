package com.locai.app.ui.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.locai.app.LocAiContainer
import com.locai.app.data.llm.DownloadProgress
import com.locai.app.data.llm.ModelOption
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ModelSetupUiState(
    val availableModels: List<ModelOption> = ModelOption.entries,
    val selectedModel: ModelOption = ModelOption.DEFAULT,
    val isDownloading: Boolean = false,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long = -1L,
    val errorMessage: String? = null,
    val isReady: Boolean = false
)

class ModelSetupViewModel(private val container: LocAiContainer) : ViewModel() {

    private val _uiState = MutableStateFlow(ModelSetupUiState())
    val uiState: StateFlow<ModelSetupUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val selected = ModelOption.byId(container.preferences.selectedModelId.first())
            val alreadyDownloaded = container.preferences.isModelDownloaded.first() &&
                container.modelFile(selected).exists()
            _uiState.update { it.copy(selectedModel = selected, isReady = alreadyDownloaded) }
        }
    }

    fun onModelSelected(option: ModelOption) {
        if (_uiState.value.isDownloading || _uiState.value.isReady) return
        _uiState.update { it.copy(selectedModel = option, errorMessage = null) }
    }

    fun startDownload() {
        if (_uiState.value.isDownloading || _uiState.value.isReady) return
        val option = _uiState.value.selectedModel

        viewModelScope.launch {
            container.preferences.setSelectedModelId(option.id)
            _uiState.update {
                it.copy(isDownloading = true, errorMessage = null, downloadedBytes = 0L, totalBytes = -1L)
            }

            container.modelDownloader.download(
                url = option.downloadUrl,
                destination = container.modelFile(option)
            ).collect { progress ->
                when (progress) {
                    is DownloadProgress.InProgress -> _uiState.update {
                        it.copy(downloadedBytes = progress.bytesDownloaded, totalBytes = progress.totalBytes)
                    }

                    is DownloadProgress.Completed -> {
                        container.preferences.setModelDownloaded(true)
                        _uiState.update { it.copy(isDownloading = false, isReady = true) }
                    }

                    is DownloadProgress.Failed -> _uiState.update {
                        it.copy(isDownloading = false, errorMessage = progress.message)
                    }
                }
            }
        }
    }
}
