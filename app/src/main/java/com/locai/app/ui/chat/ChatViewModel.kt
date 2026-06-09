package com.locai.app.ui.chat

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.locai.app.LocAiContainer
import com.locai.app.data.db.MessageEntity
import com.locai.app.domain.Categories
import com.locai.app.domain.Category
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatUiState(
    val category: Category,
    val messages: List<MessageEntity> = emptyList(),
    val inputText: String = "",
    /** A photo the user picked but hasn't sent yet — shown as a preview above the input row. */
    val attachedImage: Bitmap? = null,
    /** Whether the optional vision model is downloaded, i.e. whether to offer "attach a photo" at all. */
    val isVisionAvailable: Boolean = false,
    /** Non-null while the assistant is actively producing a reply (not yet persisted). */
    val streamingReply: String? = null,
    val isModelLoading: Boolean = true,
    val isGenerating: Boolean = false,
    val errorMessage: String? = null
)

class ChatViewModel(
    private val container: LocAiContainer,
    private val conversationId: Long,
    categoryId: String
) : ViewModel() {

    private val category = Categories.byId(categoryId)
    private val _uiState = MutableStateFlow(ChatUiState(category = category))
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            container.chatRepository.observeMessages(conversationId).collect { messages ->
                _uiState.update { it.copy(messages = messages) }
            }
        }
        viewModelScope.launch {
            container.preferences.isVisionModelDownloaded.collect { downloaded ->
                _uiState.update { it.copy(isVisionAvailable = downloaded) }
            }
        }
        viewModelScope.launch {
            val result = container.modelManager.ensureLoaded(container.activeModelFile())
            _uiState.update {
                it.copy(
                    isModelLoading = false,
                    errorMessage = result.exceptionOrNull()?.let { e -> "Couldn't load the model: ${e.message}" }
                )
            }
        }
    }

    fun onInputChanged(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    /**
     * The user picked a photo to ask about. Kick off loading the (multi-GB) vision model right
     * away in the background so it's likely already warm by the time they actually hit send —
     * [com.locai.app.data.llm.VisionModelManager.ensureLoaded] is a no-op if it's already loaded.
     */
    fun onImageAttached(bitmap: Bitmap) {
        _uiState.update { it.copy(attachedImage = bitmap) }
        viewModelScope.launch { container.visionModelManager.ensureLoaded(container.visionModelFile) }
    }

    fun clearAttachedImage() {
        _uiState.update { it.copy(attachedImage = null) }
    }

    /** Tapping a suggestion chip sends it immediately rather than just filling the input box. */
    fun sendSuggestion(prompt: String) {
        _uiState.update { it.copy(inputText = prompt) }
        sendMessage()
    }

    fun sendMessage() {
        val current = _uiState.value
        val text = current.inputText.trim()
        if (text.isEmpty() || current.isGenerating || current.isModelLoading) return

        val image = current.attachedImage
        _uiState.update {
            it.copy(
                inputText = "",
                attachedImage = null,
                isGenerating = true,
                streamingReply = "",
                errorMessage = null
            )
        }

        viewModelScope.launch {
            try {
                if (image != null) {
                    container.visionModelManager.ensureLoaded(container.visionModelFile).onFailure { e ->
                        throw IllegalStateException("Couldn't load the vision model: ${e.message}")
                    }
                }
                container.chatRepository.sendMessage(conversationId, category, text, image).collect { partial ->
                    _uiState.update { it.copy(streamingReply = partial) }
                }
            } catch (t: Throwable) {
                _uiState.update { it.copy(errorMessage = "Generation failed: ${t.message}") }
            } finally {
                _uiState.update { it.copy(isGenerating = false, streamingReply = null) }
            }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
