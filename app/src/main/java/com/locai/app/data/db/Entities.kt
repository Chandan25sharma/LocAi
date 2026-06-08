package com.locai.app.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val categoryId: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = ConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["conversationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("conversationId"), Index("categoryId")]
)
data class MessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val conversationId: Long,
    /** Denormalized so retrieval can scan messages by category without a join. */
    val categoryId: String,
    val role: MessageRole,
    val content: String,
    val timestamp: Long
)

enum class MessageRole {
    USER,
    ASSISTANT
}
