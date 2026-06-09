package com.locai.app.ui.home

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.locai.app.LocAiContainer
import com.locai.app.data.db.ConversationEntity
import com.locai.app.data.db.MessageEntity
import com.locai.app.data.db.MessageRole
import com.locai.app.domain.Categories
import com.locai.app.domain.Category
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeChatUiState(
    val activeConversationId: Long? = null,
    val activeCategory: Category = Categories.GENERAL,
    val messages: List<MessageEntity> = emptyList(),
    val allConversations: List<ConversationEntity> = emptyList(),
    val inputText: String = "",
    val attachedImage: Bitmap? = null,
    val isVisionAvailable: Boolean = false,
    val streamingReply: String? = null,
    val isModelLoading: Boolean = true,
    val isGenerating: Boolean = false,
    val errorMessage: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class HomeChatViewModel(private val container: LocAiContainer) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeChatUiState())
    val uiState: StateFlow<HomeChatUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val result = container.modelManager.ensureLoaded(container.activeModelFile())
            _uiState.update {
                it.copy(
                    isModelLoading = false,
                    errorMessage = result.exceptionOrNull()?.let { e -> "Couldn't load model: ${e.message}" }
                )
            }
        }

        viewModelScope.launch {
            container.chatRepository.observeAllConversations().collect { convos ->
                _uiState.update { it.copy(allConversations = convos) }
            }
        }

        viewModelScope.launch {
            container.preferences.isVisionModelDownloaded.collect { downloaded ->
                _uiState.update { it.copy(isVisionAvailable = downloaded) }
            }
        }

        // Automatically switch which messages are loaded whenever activeConversationId changes.
        viewModelScope.launch {
            _uiState.map { it.activeConversationId }
                .distinctUntilChanged()
                .flatMapLatest { id ->
                    if (id == null) flowOf(emptyList()) else container.chatRepository.observeMessages(id)
                }
                .collect { messages -> _uiState.update { it.copy(messages = messages) } }
        }
    }

    fun newChat() {
        _uiState.update {
            it.copy(
                activeConversationId = null,
                activeCategory = Categories.GENERAL,
                messages = emptyList(),
                inputText = "",
                attachedImage = null,
                streamingReply = null,
                isGenerating = false,
                errorMessage = null
            )
        }
    }

    fun selectConversation(conversation: ConversationEntity) {
        if (conversation.id == _uiState.value.activeConversationId) return
        _uiState.update {
            it.copy(
                activeConversationId = conversation.id,
                activeCategory = Categories.byId(conversation.categoryId),
                messages = emptyList(),
                streamingReply = null,
                isGenerating = false,
                errorMessage = null
            )
        }
    }

    fun onInputChanged(text: String) {
        _uiState.update { it.copy(inputText = text, errorMessage = null) }
    }

    fun onImageAttached(bitmap: Bitmap) {
        _uiState.update { it.copy(attachedImage = bitmap) }
        viewModelScope.launch { container.visionModelManager.ensureLoaded(container.visionModelFile) }
    }

    fun clearAttachedImage() {
        _uiState.update { it.copy(attachedImage = null) }
    }

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
            it.copy(inputText = "", attachedImage = null, isGenerating = true, streamingReply = "", errorMessage = null)
        }

        viewModelScope.launch {
            try {
                // Create a conversation on the first message if none is active.
                val conversationId = current.activeConversationId
                    ?: container.chatRepository.createConversation(current.activeCategory.id).also { newId ->
                        _uiState.update { it.copy(activeConversationId = newId) }
                    }

                if (image != null) {
                    container.visionModelManager.ensureLoaded(container.visionModelFile).onFailure { e ->
                        throw IllegalStateException("Vision model unavailable: ${e.message}")
                    }
                }

                container.chatRepository.sendMessage(conversationId, current.activeCategory, text, image)
                    .collect { partial -> _uiState.update { it.copy(streamingReply = partial) } }
            } catch (t: Throwable) {
                _uiState.update { it.copy(errorMessage = "Generation failed: ${t.message}") }
            } finally {
                _uiState.update { it.copy(isGenerating = false, streamingReply = null) }
            }
        }
    }

    fun deleteConversation(conversation: ConversationEntity) {
        viewModelScope.launch {
            if (conversation.id == _uiState.value.activeConversationId) newChat()
            container.chatRepository.deleteConversation(conversation)
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    /** Synthetic message entity used to display streaming output before it's persisted. */
    fun streamingMessageEntity(conversationId: Long, categoryId: String, text: String) = MessageEntity(
        id = -1L,
        conversationId = conversationId,
        categoryId = categoryId,
        role = MessageRole.ASSISTANT,
        content = text.ifEmpty { "…" },
        timestamp = 0L
    )
}
