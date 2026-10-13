package com.locai.app.ui.home

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.locai.app.LocAiContainer
import com.locai.app.data.db.ConversationEntity
import com.locai.app.data.db.MessageEntity
import com.locai.app.data.db.MessageRole
import com.locai.app.domain.Categories
import com.locai.app.domain.Category
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class HomeChatUiState(
    val activeConversationId: Long? = null,
    val activeCategory: Category = Categories.GENERAL,
    val messages: List<MessageEntity> = emptyList(),
    val allConversations: List<ConversationEntity> = emptyList(),
    val searchQuery: String = "",
    val inputText: String = "",
    val attachedImage: Bitmap? = null,
    val isVisionAvailable: Boolean = false,
    val streamingReply: String? = null,
    val isModelLoading: Boolean = true,
    val isGenerating: Boolean = false,
    val errorMessage: String? = null,
    val modelName: String = ""
)

@OptIn(ExperimentalCoroutinesApi::class)
class HomeChatViewModel(private val container: LocAiContainer) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeChatUiState())
    val uiState: StateFlow<HomeChatUiState> = _uiState.asStateFlow()

    private var generationJob: Job? = null

    init {
        viewModelScope.launch {
            val result = container.modelManager.ensureLoaded(container.activeModelFile())
            _uiState.update {
                it.copy(
                    isModelLoading = false,
                    modelName = container.selectedModelOption().displayName,
                    errorMessage = result.exceptionOrNull()?.let { e -> "Couldn't load model: ${e.message}" }
                )
            }
        }

        // Drawer list: everything, or search results while the search box has text.
        viewModelScope.launch {
            _uiState.map { it.searchQuery.trim() }
                .distinctUntilChanged()
                .flatMapLatest { q ->
                    if (q.isEmpty()) container.chatRepository.observeAllConversations()
                    else container.chatRepository.searchConversations(q)
                }
                .collect { convos -> _uiState.update { it.copy(allConversations = convos) } }
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
        generationJob?.cancel()
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
        generationJob?.cancel()
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

    /** Topic can only change before the first message — a conversation keeps the persona it started with. */
    fun selectCategory(category: Category) {
        if (_uiState.value.activeConversationId != null) return
        _uiState.update { it.copy(activeCategory = category) }
    }

    fun sendSuggestion(prompt: String) {
        _uiState.update { it.copy(inputText = prompt) }
        sendMessage()
    }

    fun onSearchChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun renameConversation(conversation: ConversationEntity, title: String) {
        viewModelScope.launch { container.chatRepository.renameConversation(conversation.id, title) }
    }

    fun togglePin(conversation: ConversationEntity) {
        viewModelScope.launch { container.chatRepository.setPinned(conversation.id, !conversation.isPinned) }
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

    fun sendMessage() {
        val current = _uiState.value
        val text = current.inputText.trim()
        if (text.isEmpty() || current.isGenerating || current.isModelLoading) return

        val image = current.attachedImage
        _uiState.update { it.copy(inputText = "", attachedImage = null) }
        generate(text, image)
    }

    fun stopGeneration() {
        generationJob?.cancel()
    }

    /** Re-asks the last user message: drops it and its reply, then sends it again. Also used as "Retry". */
    fun regenerate() {
        val current = _uiState.value
        val conversationId = current.activeConversationId ?: return
        if (current.isGenerating || current.isModelLoading) return
        _uiState.update { it.copy(isGenerating = true, errorMessage = null) }
        viewModelScope.launch {
            val last = container.chatRepository.popLastExchange(conversationId)
            if (last == null) {
                _uiState.update { it.copy(isGenerating = false) }
                return@launch
            }
            val image = last.imagePath?.let { path -> withContext(Dispatchers.IO) { BitmapFactory.decodeFile(path) } }
            generate(last.content, image)
        }
    }

    private fun generate(text: String, image: Bitmap?) {
        val current = _uiState.value
        _uiState.update { it.copy(isGenerating = true, streamingReply = "", errorMessage = null) }

        generationJob = viewModelScope.launch {
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
            } catch (e: CancellationException) {
                throw e // user hit Stop — not an error
            } catch (t: Throwable) {
                _uiState.update { it.copy(errorMessage = "Generation failed: ${t.message}") }
            } finally {
                // A stopped job can finish after a newer one started (switched chat, sent again) — don't clobber it.
                if (generationJob == coroutineContext.job) {
                    _uiState.update { it.copy(isGenerating = false, streamingReply = null) }
                }
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

    /** Current chat as Markdown, for Share / Save as file. */
    fun transcript(): String {
        val state = _uiState.value
        val title = state.allConversations.firstOrNull { it.id == state.activeConversationId }?.title ?: "LocAi chat"
        return buildString {
            append("# ").append(title).append("\n\n")
            state.messages.forEach { m ->
                append(if (m.role == MessageRole.USER) "**You:** " else "**LocAi:** ")
                append(m.content).append("\n\n")
            }
        }.trimEnd() + "\n"
    }

    /** Synthetic message entity used to display streaming output before it's persisted. */
    fun streamingMessageEntity(conversationId: Long, categoryId: String, text: String) = MessageEntity(
        id = -1L,
        conversationId = conversationId,
        categoryId = categoryId,
        role = MessageRole.ASSISTANT,
        content = text,
        timestamp = 0L
    )
}
