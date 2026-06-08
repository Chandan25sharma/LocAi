package com.locai.app.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {

    @Insert
    suspend fun insert(conversation: ConversationEntity): Long

    @Update
    suspend fun update(conversation: ConversationEntity)

    @Delete
    suspend fun delete(conversation: ConversationEntity)

    @Query("SELECT * FROM conversations WHERE id = :id")
    fun observeById(id: Long): Flow<ConversationEntity?>

    @Query("SELECT * FROM conversations WHERE id = :id")
    suspend fun getById(id: Long): ConversationEntity?

    @Query("SELECT * FROM conversations WHERE categoryId = :categoryId ORDER BY updatedAt DESC")
    fun observeByCategory(categoryId: String): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<ConversationEntity>>

    @Query(
        "SELECT DISTINCT c.* FROM conversations c " +
            "JOIN messages m ON m.conversationId = c.id " +
            "WHERE m.content LIKE '%' || :query || '%' " +
            "ORDER BY c.updatedAt DESC"
    )
    fun search(query: String): Flow<List<ConversationEntity>>
}
