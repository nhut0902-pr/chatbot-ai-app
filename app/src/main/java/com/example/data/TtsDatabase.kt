package com.example.data

import android.content.Context
import androidx.room.*
import com.example.models.TtsHistoryItem
import kotlinx.coroutines.flow.Flow

@Dao
interface TtsHistoryDao {
    @Query("SELECT * FROM tts_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<TtsHistoryItem>>

    @Query("SELECT * FROM tts_history ORDER BY timestamp DESC")
    suspend fun getAllHistoryList(): List<TtsHistoryItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: TtsHistoryItem): Long

    @Delete
    suspend fun deleteItem(item: TtsHistoryItem)

    @Query("DELETE FROM tts_history WHERE id = :id")
    suspend fun deleteItemById(id: Long)

    @Query("DELETE FROM tts_history")
    suspend fun clearAllHistory()
}

@Database(entities = [TtsHistoryItem::class], version = 1, exportSchema = false)
abstract class TtsDatabase : RoomDatabase() {
    abstract fun ttsDao(): TtsHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: TtsDatabase? = null

        fun getDatabase(context: Context): TtsDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TtsDatabase::class.java,
                    "tts_voice_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class TtsRepository(private val dao: TtsHistoryDao) {
    val allHistory: Flow<List<TtsHistoryItem>> = dao.getAllHistory()

    suspend fun insert(item: TtsHistoryItem): Long {
        return dao.insertItem(item)
    }

    suspend fun delete(item: TtsHistoryItem) {
        dao.deleteItem(item)
    }

    suspend fun deleteById(id: Long) {
        dao.deleteItemById(id)
    }

    suspend fun clearHistory() {
        dao.clearAllHistory()
    }

    suspend fun getAllHistoryList(): List<TtsHistoryItem> {
        return dao.getAllHistoryList()
    }
}
