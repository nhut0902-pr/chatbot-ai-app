package com.example.data

import android.content.Context
import androidx.room.*
import com.example.models.Message
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<Message>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: Message): Long

    @Query("DELETE FROM chat_messages")
    suspend fun clearHistory()

    @Query("DELETE FROM chat_messages WHERE id = :id")
    suspend fun deleteMessageById(id: Long)
}

@Database(entities = [Message::class], version = 1, exportSchema = false)
abstract class ChatDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao

    companion object {
        @Volatile
        private var INSTANCE: ChatDatabase? = null

        fun getDatabase(context: Context): ChatDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ChatDatabase::class.java,
                    "cerebras_chat_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class ChatRepository(private val messageDao: MessageDao) {
    val allMessages: Flow<List<Message>> = messageDao.getAllMessages()

    suspend fun insert(message: Message): Long {
        return messageDao.insertMessage(message)
    }

    suspend fun clearHistory() {
        messageDao.clearHistory()
    }

    suspend fun deleteById(id: Long) {
        messageDao.deleteMessageById(id)
    }
}
