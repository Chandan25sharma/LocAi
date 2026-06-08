package com.locai.app.`data`.db

import androidx.room.InvalidationTracker
import androidx.room.RoomOpenDelegate
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.room.util.TableInfo
import androidx.room.util.TableInfo.Companion.read
import androidx.room.util.dropFtsSyncTriggers
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import javax.`annotation`.processing.Generated
import kotlin.Lazy
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.MutableList
import kotlin.collections.MutableMap
import kotlin.collections.MutableSet
import kotlin.collections.Set
import kotlin.collections.mutableListOf
import kotlin.collections.mutableMapOf
import kotlin.collections.mutableSetOf
import kotlin.reflect.KClass

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class LocAiDatabase_Impl : LocAiDatabase() {
  private val _conversationDao: Lazy<ConversationDao> = lazy {
    ConversationDao_Impl(this)
  }

  private val _messageDao: Lazy<MessageDao> = lazy {
    MessageDao_Impl(this)
  }

  protected override fun createOpenDelegate(): RoomOpenDelegate {
    val _openDelegate: RoomOpenDelegate = object : RoomOpenDelegate(1,
        "116b7c2ffc25353fa8d5dc888028f8ba", "105d1bc3e6b1d1af5d838e9035e767b1") {
      public override fun createAllTables(connection: SQLiteConnection) {
        connection.execSQL("CREATE TABLE IF NOT EXISTS `conversations` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `categoryId` TEXT NOT NULL, `title` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `messages` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `conversationId` INTEGER NOT NULL, `categoryId` TEXT NOT NULL, `role` TEXT NOT NULL, `content` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, FOREIGN KEY(`conversationId`) REFERENCES `conversations`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_messages_conversationId` ON `messages` (`conversationId`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_messages_categoryId` ON `messages` (`categoryId`)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
        connection.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '116b7c2ffc25353fa8d5dc888028f8ba')")
      }

      public override fun dropAllTables(connection: SQLiteConnection) {
        connection.execSQL("DROP TABLE IF EXISTS `conversations`")
        connection.execSQL("DROP TABLE IF EXISTS `messages`")
      }

      public override fun onCreate(connection: SQLiteConnection) {
      }

      public override fun onOpen(connection: SQLiteConnection) {
        connection.execSQL("PRAGMA foreign_keys = ON")
        internalInitInvalidationTracker(connection)
      }

      public override fun onPreMigrate(connection: SQLiteConnection) {
        dropFtsSyncTriggers(connection)
      }

      public override fun onPostMigrate(connection: SQLiteConnection) {
      }

      public override fun onValidateSchema(connection: SQLiteConnection):
          RoomOpenDelegate.ValidationResult {
        val _columnsConversations: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsConversations.put("id", TableInfo.Column("id", "INTEGER", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsConversations.put("categoryId", TableInfo.Column("categoryId", "TEXT", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsConversations.put("title", TableInfo.Column("title", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsConversations.put("createdAt", TableInfo.Column("createdAt", "INTEGER", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsConversations.put("updatedAt", TableInfo.Column("updatedAt", "INTEGER", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysConversations: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesConversations: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoConversations: TableInfo = TableInfo("conversations", _columnsConversations,
            _foreignKeysConversations, _indicesConversations)
        val _existingConversations: TableInfo = read(connection, "conversations")
        if (!_infoConversations.equals(_existingConversations)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |conversations(com.locai.app.data.db.ConversationEntity).
              | Expected:
              |""".trimMargin() + _infoConversations + """
              |
              | Found:
              |""".trimMargin() + _existingConversations)
        }
        val _columnsMessages: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsMessages.put("id", TableInfo.Column("id", "INTEGER", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsMessages.put("conversationId", TableInfo.Column("conversationId", "INTEGER", true,
            0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsMessages.put("categoryId", TableInfo.Column("categoryId", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsMessages.put("role", TableInfo.Column("role", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsMessages.put("content", TableInfo.Column("content", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsMessages.put("timestamp", TableInfo.Column("timestamp", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysMessages: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        _foreignKeysMessages.add(TableInfo.ForeignKey("conversations", "CASCADE", "NO ACTION",
            listOf("conversationId"), listOf("id")))
        val _indicesMessages: MutableSet<TableInfo.Index> = mutableSetOf()
        _indicesMessages.add(TableInfo.Index("index_messages_conversationId", false,
            listOf("conversationId"), listOf("ASC")))
        _indicesMessages.add(TableInfo.Index("index_messages_categoryId", false,
            listOf("categoryId"), listOf("ASC")))
        val _infoMessages: TableInfo = TableInfo("messages", _columnsMessages, _foreignKeysMessages,
            _indicesMessages)
        val _existingMessages: TableInfo = read(connection, "messages")
        if (!_infoMessages.equals(_existingMessages)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |messages(com.locai.app.data.db.MessageEntity).
              | Expected:
              |""".trimMargin() + _infoMessages + """
              |
              | Found:
              |""".trimMargin() + _existingMessages)
        }
        return RoomOpenDelegate.ValidationResult(true, null)
      }
    }
    return _openDelegate
  }

  protected override fun createInvalidationTracker(): InvalidationTracker {
    val _shadowTablesMap: MutableMap<String, String> = mutableMapOf()
    val _viewTables: MutableMap<String, Set<String>> = mutableMapOf()
    return InvalidationTracker(this, _shadowTablesMap, _viewTables, "conversations", "messages")
  }

  public override fun clearAllTables() {
    super.performClear(true, "conversations", "messages")
  }

  protected override fun getRequiredTypeConverterClasses(): Map<KClass<*>, List<KClass<*>>> {
    val _typeConvertersMap: MutableMap<KClass<*>, List<KClass<*>>> = mutableMapOf()
    _typeConvertersMap.put(ConversationDao::class, ConversationDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(MessageDao::class, MessageDao_Impl.getRequiredConverters())
    return _typeConvertersMap
  }

  public override fun getRequiredAutoMigrationSpecClasses(): Set<KClass<out AutoMigrationSpec>> {
    val _autoMigrationSpecsSet: MutableSet<KClass<out AutoMigrationSpec>> = mutableSetOf()
    return _autoMigrationSpecsSet
  }

  public override
      fun createAutoMigrations(autoMigrationSpecs: Map<KClass<out AutoMigrationSpec>, AutoMigrationSpec>):
      List<Migration> {
    val _autoMigrations: MutableList<Migration> = mutableListOf()
    return _autoMigrations
  }

  public override fun conversationDao(): ConversationDao = _conversationDao.value

  public override fun messageDao(): MessageDao = _messageDao.value
}
