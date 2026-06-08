package com.locai.app.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.locai.app.LocAiContainer
import com.locai.app.data.db.ConversationEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class HistoryViewModel(private val container: LocAiContainer) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    val conversations: StateFlow<List<ConversationEntity>> = _query
        .debounce(250)
        .flatMapLatest { q ->
            if (q.isBlank()) container.chatRepository.observeAllConversations()
            else container.chatRepository.searchConversations(q.trim())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun onQueryChanged(value: String) {
        _query.value = value
    }

    fun delete(conversation: ConversationEntity) {
        viewModelScope.launch { container.chatRepository.deleteConversation(conversation) }
    }
}
