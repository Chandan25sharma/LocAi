package com.locai.app.`data`.db

import androidx.room.EntityDeleteOrUpdateAdapter
import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import javax.`annotation`.processing.Generated
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.mutableListOf
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.Flow

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class ConversationDao_Impl(
  __db: RoomDatabase,
) : ConversationDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfConversationEntity: EntityInsertAdapter<ConversationEntity>

  private val __deleteAdapterOfConversationEntity: EntityDeleteOrUpdateAdapter<ConversationEntity>

  private val __updateAdapterOfConversationEntity: EntityDeleteOrUpdateAdapter<ConversationEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfConversationEntity = object : EntityInsertAdapter<ConversationEntity>() {
      protected override fun createQuery(): String =
          "INSERT OR ABORT INTO `conversations` (`id`,`categoryId`,`title`,`createdAt`,`updatedAt`) VALUES (nullif(?, 0),?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: ConversationEntity) {
        statement.bindLong(1, entity.id)
        statement.bindText(2, entity.categoryId)
        statement.bindText(3, entity.title)
        statement.bindLong(4, entity.createdAt)
        statement.bindLong(5, entity.updatedAt)
      }
    }
    this.__deleteAdapterOfConversationEntity = object :
        EntityDeleteOrUpdateAdapter<ConversationEntity>() {
      protected override fun createQuery(): String = "DELETE FROM `conversations` WHERE `id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: ConversationEntity) {
        statement.bindLong(1, entity.id)
      }
    }
    this.__updateAdapterOfConversationEntity = object :
        EntityDeleteOrUpdateAdapter<ConversationEntity>() {
      protected override fun createQuery(): String =
          "UPDATE OR ABORT `conversations` SET `id` = ?,`categoryId` = ?,`title` = ?,`createdAt` = ?,`updatedAt` = ? WHERE `id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: ConversationEntity) {
        statement.bindLong(1, entity.id)
        statement.bindText(2, entity.categoryId)
        statement.bindText(3, entity.title)
        statement.bindLong(4, entity.createdAt)
        statement.bindLong(5, entity.updatedAt)
        statement.bindLong(6, entity.id)
      }
    }
  }

  public override suspend fun insert(conversation: ConversationEntity): Long =
      performSuspending(__db, false, true) { _connection ->
    val _result: Long = __insertAdapterOfConversationEntity.insertAndReturnId(_connection,
        conversation)
    _result
  }

  public override suspend fun delete(conversation: ConversationEntity): Unit =
      performSuspending(__db, false, true) { _connection ->
    __deleteAdapterOfConversationEntity.handle(_connection, conversation)
  }

  public override suspend fun update(conversation: ConversationEntity): Unit =
      performSuspending(__db, false, true) { _connection ->
    __updateAdapterOfConversationEntity.handle(_connection, conversation)
  }

  public override fun observeById(id: Long): Flow<ConversationEntity?> {
    val _sql: String = "SELECT * FROM conversations WHERE id = ?"
    return createFlow(__db, false, arrayOf("conversations")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, id)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfCategoryId: Int = getColumnIndexOrThrow(_stmt, "categoryId")
        val _columnIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _result: ConversationEntity?
        if (_stmt.step()) {
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpCategoryId: String
          _tmpCategoryId = _stmt.getText(_columnIndexOfCategoryId)
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_columnIndexOfTitle)
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          val _tmpUpdatedAt: Long
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
          _result = ConversationEntity(_tmpId,_tmpCategoryId,_tmpTitle,_tmpCreatedAt,_tmpUpdatedAt)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getById(id: Long): ConversationEntity? {
    val _sql: String = "SELECT * FROM conversations WHERE id = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, id)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfCategoryId: Int = getColumnIndexOrThrow(_stmt, "categoryId")
        val _columnIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _result: ConversationEntity?
        if (_stmt.step()) {
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpCategoryId: String
          _tmpCategoryId = _stmt.getText(_columnIndexOfCategoryId)
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_columnIndexOfTitle)
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          val _tmpUpdatedAt: Long
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
          _result = ConversationEntity(_tmpId,_tmpCategoryId,_tmpTitle,_tmpCreatedAt,_tmpUpdatedAt)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeByCategory(categoryId: String): Flow<List<ConversationEntity>> {
    val _sql: String = "SELECT * FROM conversations WHERE categoryId = ? ORDER BY updatedAt DESC"
    return createFlow(__db, false, arrayOf("conversations")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, categoryId)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfCategoryId: Int = getColumnIndexOrThrow(_stmt, "categoryId")
        val _columnIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _result: MutableList<ConversationEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: ConversationEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpCategoryId: String
          _tmpCategoryId = _stmt.getText(_columnIndexOfCategoryId)
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_columnIndexOfTitle)
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          val _tmpUpdatedAt: Long
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
          _item = ConversationEntity(_tmpId,_tmpCategoryId,_tmpTitle,_tmpCreatedAt,_tmpUpdatedAt)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeAll(): Flow<List<ConversationEntity>> {
    val _sql: String = "SELECT * FROM conversations ORDER BY updatedAt DESC"
    return createFlow(__db, false, arrayOf("conversations")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfCategoryId: Int = getColumnIndexOrThrow(_stmt, "categoryId")
        val _columnIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _result: MutableList<ConversationEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: ConversationEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpCategoryId: String
          _tmpCategoryId = _stmt.getText(_columnIndexOfCategoryId)
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_columnIndexOfTitle)
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          val _tmpUpdatedAt: Long
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
          _item = ConversationEntity(_tmpId,_tmpCategoryId,_tmpTitle,_tmpCreatedAt,_tmpUpdatedAt)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun search(query: String): Flow<List<ConversationEntity>> {
    val _sql: String =
        "SELECT DISTINCT c.* FROM conversations c JOIN messages m ON m.conversationId = c.id WHERE m.content LIKE '%' || ? || '%' ORDER BY c.updatedAt DESC"
    return createFlow(__db, false, arrayOf("conversations", "messages")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, query)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfCategoryId: Int = getColumnIndexOrThrow(_stmt, "categoryId")
        val _columnIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _result: MutableList<ConversationEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: ConversationEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpCategoryId: String
          _tmpCategoryId = _stmt.getText(_columnIndexOfCategoryId)
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_columnIndexOfTitle)
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          val _tmpUpdatedAt: Long
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
          _item = ConversationEntity(_tmpId,_tmpCategoryId,_tmpTitle,_tmpCreatedAt,_tmpUpdatedAt)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
