package com.locai.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import com.locai.app.LocAiContainer
import com.locai.app.data.llm.DownloadProgress
import com.locai.app.data.llm.GeneratorModelOption
import com.locai.app.data.llm.VisionModelOption
import com.locai.app.data.prefs.AppPreferences
import com.locai.app.service.ModelDownloadService
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
    val modelDeleted: Boolean = false,
    // Optional vision (image-understanding) model — entirely separate download/lifecycle from
    // the base text model above.
    val visionModelOnDisk: Boolean = false,
    val visionModelSizeMb: Long = 0L,
    val isVisionDownloading: Boolean = false,
    val visionDownloadedBytes: Long = 0L,
    val visionTotalBytes: Long = -1L,
    val visionErrorMessage: String? = null,
    // Optional image generation model — must be manually placed; no automatic download.
    val generatorModelInstalled: Boolean = false
)

class SettingsViewModel(private val container: LocAiContainer) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch { refreshModelInfo() }
        viewModelScope.launch { refreshVisionModelInfo() }
        refreshGeneratorModelInfo()

        // Observe vision download progress from the shared state written by ModelDownloadService.
        viewModelScope.launch {
            container.visionDownloadProgress.collect { progress ->
                when (progress) {
                    null -> {}
                    is DownloadProgress.InProgress -> _uiState.update {
                        it.copy(isVisionDownloading = true, visionDownloadedBytes = progress.bytesDownloaded, visionTotalBytes = progress.totalBytes)
                    }
                    is DownloadProgress.Completed -> {
                        val file = progress.file
                        _uiState.update {
                            it.copy(isVisionDownloading = false, visionModelOnDisk = true, visionModelSizeMb = file.length() / (1024 * 1024))
                        }
                    }
                    is DownloadProgress.Failed -> _uiState.update {
                        it.copy(isVisionDownloading = false, visionErrorMessage = progress.message)
                    }
                }
            }
        }

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

    private suspend fun refreshVisionModelInfo() {
        val file = container.visionModelFile
        val onDisk = file.exists()
        _uiState.update {
            it.copy(
                visionModelOnDisk = onDisk,
                visionModelSizeMb = if (onDisk) file.length() / (1024 * 1024) else 0L
            )
        }
    }

    /** Starts a foreground-service download for the vision model so it survives app minimisation. */
    fun downloadVisionModel(context: Context) {
        if (_uiState.value.isVisionDownloading || _uiState.value.visionModelOnDisk) return
        _uiState.update { it.copy(isVisionDownloading = true, visionErrorMessage = null, visionDownloadedBytes = 0L, visionTotalBytes = -1L) }
        ModelDownloadService.startFor(
            context = context,
            type = ModelDownloadService.TYPE_VISION,
            url = VisionModelOption.DOWNLOAD_URL,
            destPath = container.visionModelFile.absolutePath,
            title = "Downloading Image Understanding"
        )
    }

    /** Deletes the cached vision weights and turns the "attach a photo" option back off. */
    fun deleteVisionModel() {
        viewModelScope.launch {
            val file = container.visionModelFile
            container.visionModelManager.unload()
            if (file.exists()) file.delete()
            container.preferences.setVisionModelDownloaded(false)
            _uiState.update { it.copy(visionModelOnDisk = false, visionModelSizeMb = 0L) }
        }
    }

    /** Checks the filesystem for the generator model directory and updates state. */
    fun refreshGeneratorModelInfo() {
        val installed = container.imageGeneratorManager.isInstalled(container.generatorModelDir)
        _uiState.update { it.copy(generatorModelInstalled = installed) }
    }

    /** Deletes the generator model directory and unloads the engine if it was loaded. */
    fun deleteGeneratorModel() {
        viewModelScope.launch {
            container.imageGeneratorManager.unload()
            container.generatorModelDir.deleteRecursively()
            _uiState.update { it.copy(generatorModelInstalled = false) }
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
