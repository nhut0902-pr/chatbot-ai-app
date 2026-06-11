package com.example.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tts_history")
data class TtsHistoryItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val text: String,
    val language: String,
    val gender: String,
    val voice: String,
    val localFilePath: String, // Absolute path to on-device audio binary (MP3 file)
    val timestamp: Long = System.currentTimeMillis()
)
