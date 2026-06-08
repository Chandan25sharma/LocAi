package com.locai.app.ui.chat

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

    fun sendMessage() {
        val current = _uiState.value
        val text = current.inputText.trim()
        if (text.isEmpty() || current.isGenerating || current.isModelLoading) return

        _uiState.update {
            it.copy(inputText = "", isGenerating = true, streamingReply = "", errorMessage = null)
        }

        viewModelScope.launch {
            try {
                container.chatRepository.sendMessage(conversationId, category, text).collect { partial ->
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
