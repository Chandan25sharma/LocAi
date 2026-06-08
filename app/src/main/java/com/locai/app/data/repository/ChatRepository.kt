package com.locai.app.data.repository

import com.locai.app.data.db.ConversationDao
import com.locai.app.data.db.ConversationEntity
import com.locai.app.data.db.MessageDao
import com.locai.app.data.db.MessageEntity
import com.locai.app.data.db.MessageRole
import com.locai.app.data.llm.LlmModelManager
import com.locai.app.data.llm.PromptBuilder
import com.locai.app.data.prefs.AppPreferences
import com.locai.app.data.retrieval.HistoryRetriever
import com.locai.app.domain.Category
import com.locai.app.domain.UserPersona
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow

/**
 * The single place that wires local storage, the on-device "memory" (history retrieval), and
 * the LLM together. The UI only ever talks to this class.
 */
class ChatRepository(
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao,
    private val modelManager: LlmModelManager,
    private val historyRetriever: HistoryRetriever,
    private val preferences: AppPreferences
) {
    fun observeConversations(categoryId: String): Flow<List<ConversationEntity>> =
        conversationDao.observeByCategory(categoryId)

    fun observeAllConversations(): Flow<List<ConversationEntity>> =
        conversationDao.observeAll()

    fun searchConversations(query: String): Flow<List<ConversationEntity>> =
        conversationDao.search(query)

    fun observeConversation(id: Long): Flow<ConversationEntity?> =
        conversationDao.observeById(id)

    fun observeMessages(conversationId: Long): Flow<List<MessageEntity>> =
        messageDao.observeForConversation(conversationId)

    suspend fun createConversation(categoryId: String): Long {
        val now = System.currentTimeMillis()
        return conversationDao.insert(
            ConversationEntity(categoryId = categoryId, title = NEW_CHAT_TITLE, createdAt = now, updatedAt = now)
        )
    }

    suspend fun deleteConversation(conversation: ConversationEntity) {
        conversationDao.delete(conversation)
    }

    /**
     * Persists [userText], retrieves relevant past exchanges from this category's history,
     * builds the full prompt, and streams the model's reply (each emission is the reply
     * accumulated so far). The final reply is persisted once generation completes.
     */
    fun sendMessage(conversationId: Long, category: Category, userText: String): Flow<String> = flow {
        val sentAt = System.currentTimeMillis()
        messageDao.insert(
            MessageEntity(
                conversationId = conversationId,
                categoryId = category.id,
                role = MessageRole.USER,
                content = userText,
                timestamp = sentAt
            )
        )
        touchConversation(conversationId, titleSeed = userText, timestamp = sentAt)

        val recentMessages = messageDao.getForConversation(conversationId)
        val retrieved = historyRetriever.retrieve(
            categoryId = category.id,
            excludeConversationId = conversationId,
            query = userText
        )
        val prompt = PromptBuilder.build(
            category = category,
            recentMessages = recentMessages,
            retrievedContext = retrieved,
            newUserMessage = userText,
            userName = preferences.userName.first(),
            userPersona = UserPersona.byId(preferences.userPersonaId.first())
        )

        val topK = preferences.topK.first()
        val temperature = preferences.temperature.first()

        var fullReply = ""
        modelManager.generateResponseStream(prompt, topK = topK, topP = DEFAULT_TOP_P, temperature = temperature)
            .collect { partial ->
                fullReply = partial
                emit(partial)
            }

        val repliedAt = System.currentTimeMillis()
        messageDao.insert(
            MessageEntity(
                conversationId = conversationId,
                categoryId = category.id,
                role = MessageRole.ASSISTANT,
                content = fullReply,
                timestamp = repliedAt
            )
        )
        touchConversation(conversationId, titleSeed = null, timestamp = repliedAt)
    }

    private suspend fun touchConversation(conversationId: Long, titleSeed: String?, timestamp: Long) {
        val conversation = conversationDao.getById(conversationId) ?: return
        val title = if (conversation.title == NEW_CHAT_TITLE && titleSeed != null) {
            deriveTitle(titleSeed)
        } else {
            conversation.title
        }
        conversationDao.update(conversation.copy(title = title, updatedAt = timestamp))
    }

    private fun deriveTitle(text: String): String {
        val firstLine = text.trim().lineSequence().first().trim()
        return if (firstLine.length > MAX_TITLE_CHARS) "${firstLine.take(MAX_TITLE_CHARS)}…" else firstLine
    }

    companion object {
        const val NEW_CHAT_TITLE = "New chat"
        private const val MAX_TITLE_CHARS = 48
        private const val DEFAULT_TOP_P = 0.95f
    }
}
