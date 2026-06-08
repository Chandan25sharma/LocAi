package com.locai.app.ui.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.locai.app.LocAiContainer
import com.locai.app.data.llm.DownloadProgress
import com.locai.app.data.llm.ModelOption
import com.locai.app.domain.UserPersona
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class OnboardingStep { WELCOME, MODEL }

data class ModelSetupUiState(
    val step: OnboardingStep = OnboardingStep.WELCOME,
    val userName: String = "",
    val selectedPersona: UserPersona = UserPersona.DEFAULT,
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
            val onboarded = container.preferences.isOnboarded.first()
            val selected = ModelOption.byId(container.preferences.selectedModelId.first())
            val alreadyDownloaded = container.preferences.isModelDownloaded.first() &&
                container.modelFile(selected).exists()
            val step = if (onboarded) OnboardingStep.MODEL else OnboardingStep.WELCOME

            _uiState.update { it.copy(step = step, selectedModel = selected, isReady = alreadyDownloaded) }

            // Ollama-style "just works": once we know which model to fetch (immediately for a
            // returning user, right after personalization for a new one) the pull starts on its
            // own — no button tap required.
            if (step == OnboardingStep.MODEL && !alreadyDownloaded) startDownload()
        }
    }

    fun onNameChanged(name: String) {
        _uiState.update { it.copy(userName = name) }
    }

    fun onPersonaSelected(persona: UserPersona) {
        _uiState.update { it.copy(selectedPersona = persona, selectedModel = persona.recommendedModel) }
    }

    fun continueFromWelcome() {
        if (_uiState.value.step != OnboardingStep.WELCOME) return
        val state = _uiState.value
        val name = state.userName.trim()

        viewModelScope.launch {
            if (name.isNotEmpty()) container.preferences.setUserName(name)
            container.preferences.setUserPersonaId(state.selectedPersona.id)
            container.preferences.setOnboarded(true)
            _uiState.update { it.copy(step = OnboardingStep.MODEL, userName = name) }
            startDownload()
        }
    }

    fun onModelSelected(option: ModelOption) {
        if (_uiState.value.isDownloading || _uiState.value.isReady) return
        _uiState.update { it.copy(selectedModel = option, errorMessage = null) }
        startDownload()
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
