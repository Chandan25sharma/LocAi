package com.locai.app.data.db

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromMessageRole(role: MessageRole): String = role.name

    @TypeConverter
    fun toMessageRole(value: String): MessageRole = MessageRole.valueOf(value)
}
