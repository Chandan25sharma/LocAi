package com.locai.app.ui.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.locai.app.LocAiContainer
import com.locai.app.domain.Category
import kotlinx.coroutines.launch

class CategoryViewModel(private val container: LocAiContainer) : ViewModel() {

    /** Every tap starts a fresh conversation in that domain; old ones live in History. */
    fun startNewChat(category: Category, onCreated: (conversationId: Long) -> Unit) {
        viewModelScope.launch {
            val conversationId = container.chatRepository.createConversation(category.id)
            onCreated(conversationId)
        }
    }
}
