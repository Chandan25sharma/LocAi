package com.locai.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {

    @Insert
    suspend fun insert(message: MessageEntity): Long

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    fun observeForConversation(conversationId: Long): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    suspend fun getForConversation(conversationId: Long): List<MessageEntity>

    /**
     * All messages for a category, excluding the conversation currently in progress
     * (so retrieval pulls from *past* chats, not the live one).
     */
    @Query(
        "SELECT * FROM messages WHERE categoryId = :categoryId AND conversationId != :excludeConversationId " +
            "ORDER BY timestamp DESC LIMIT :limit"
    )
    suspend fun getRecentForCategory(categoryId: String, excludeConversationId: Long, limit: Int): List<MessageEntity>

    @Query("DELETE FROM messages WHERE conversationId = :conversationId")
    suspend fun deleteForConversation(conversationId: Long)
}
