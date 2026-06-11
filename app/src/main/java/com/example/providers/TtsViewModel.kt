package com.example.providers

import android.app.Application
import android.media.MediaPlayer
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.TtsDatabase
import com.example.data.TtsRepository
import com.example.models.TtsHistoryItem
import com.example.services.TtsService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class TtsViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "TtsViewModel"

    private val database = TtsDatabase.getDatabase(application)
    private val repository = TtsRepository(database.ttsDao())

    // --- UI/STATEFLOWS ---
    private val _textInput = MutableStateFlow("")
    val textInput: StateFlow<String> = _textInput.asStateFlow()

    private val _selectedLanguage = MutableStateFlow("Vietnamese")
    val selectedLanguage: StateFlow<String> = _selectedLanguage.asStateFlow()

    private val _selectedGender = MutableStateFlow("female")
    val selectedGender: StateFlow<String> = _selectedGender.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _playingItemId = MutableStateFlow<Long?>(null)
    val playingItemId: StateFlow<Long?> = _playingItemId.asStateFlow()

    val historyList: StateFlow<List<TtsHistoryItem>> = repository.allHistory
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // --- PLAYBACK PLAYER ---
    private var mediaPlayer: MediaPlayer? = null

    init {
        // Log initialization
        Log.d(TAG, "TtsViewModel Initialized")
    }

    fun onTextInputChanged(text: String) {
        _textInput.value = text
    }

    fun onLanguageChanged(lang: String) {
        _selectedLanguage.value = lang
    }

    fun onGenderChanged(gender: String) {
        _selectedGender.value = gender
    }

    fun dismissError() {
        _errorMessage.value = null
    }

    /**
     * Translates selected Language + Gender into the specific voice name requested.
     */
    private fun getVoiceId(language: String, gender: String): String {
        return when (language.lowercase()) {
            "vietnamese" -> if (gender == "male") "vi-VN-NamMinhNeural" else "vi-VN-HoaiMyNeural"
            "english" -> if (gender == "male") "en-US-GuyNeural" else "en-US-AriaNeural"
            "japanese" -> if (gender == "male") "ja-JP-KeitaNeural" else "ja-JP-NanamiNeural"
            "korean" -> if (gender == "male") "ko-KR-InJoonNeural" else "ko-KR-SunHiNeural"
            else -> if (gender == "male") "vi-VN-NamMinhNeural" else "vi-VN-HoaiMyNeural"
        }
    }

    /**
     * Generates Speech from text input using Retrofit and caches the output locally, maintaining a 10-item limit.
     */
    fun generateVoice() {
        val text = _textInput.value.trim()
        val language = _selectedLanguage.value
        val gender = _selectedGender.value

        if (text.isEmpty()) {
            _errorMessage.value = "Vui lòng nhập văn bản để chuyển đổi!"
            return
        }

        if (text.length > 300) {
            _errorMessage.value = "Văn bản quá dài! Vui lòng nhập dưới 300 ký tự."
            return
        }

        val voiceId = getVoiceId(language, gender)
        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                // Call Retrofit service and download binary payload on Dispatchers.IO
                val response = withContext(Dispatchers.IO) {
                    TtsService.getVoiceAudio(text, voiceId)
                }

                if (response.isSuccessful) {
                    val responseBody = response.body()
                    val contentType = response.headers()["Content-Type"] ?: responseBody?.contentType()?.toString() ?: ""

                    if (responseBody != null) {
                        if (contentType.contains("json", ignoreCase = true)) {
                            // High defensive check: parse JSON error payload instead of treating as binary audio
                            val responseString = withContext(Dispatchers.IO) {
                                responseBody.string()
                            }
                            Log.e(TAG, "Server returned 200 OK with JSON instead of audio binary: $responseString")
                            _errorMessage.value = "Lỗi từ máy chủ: $responseString"
                        } else {
                            // Write binary file to disk
                            val localFile = withContext(Dispatchers.IO) {
                                saveAudioStreamToCache(responseBody.byteStream())
                            }

                            if (localFile != null && localFile.exists()) {
                                // Save to Room Database
                                val newItem = TtsHistoryItem(
                                    text = text,
                                    language = language,
                                    gender = gender,
                                    voice = voiceId,
                                    localFilePath = localFile.absolutePath,
                                    timestamp = System.currentTimeMillis()
                                )

                                val insertedId = repository.insert(newItem)
                                val insertedItem = newItem.copy(id = insertedId)

                                // Play generated audio immediately after generation
                                playAudio(insertedItem)

                                // Prune history list to retain exactly top 10 items
                                pruneCacheToMaxCount(10)
                            } else {
                                _errorMessage.value = "Lỗi khi ghi dữ liệu âm thanh vào tệp nhớ tạm!"
                            }
                        }
                    } else {
                        _errorMessage.value = "Không nhận được phản hồi dữ liệu từ máy chủ."
                    }
                } else {
                    val errorPayload = response.errorBody()?.string() ?: ""
                    Log.e(TAG, "API failed: ${response.code()} -> $errorPayload")
                    _errorMessage.value = "Lỗi kết nối từ Máy Chủ (${response.code()}). Thử lại sau!"
                }
            } catch (e: java.io.IOException) {
                Log.e(TAG, "Network timeout / IO error during TTS", e)
                _errorMessage.value = "Lỗi mạng hoặc Timeout (60s). Hãy kiểm tra lại kết nối của bạn!"
            } catch (e: Exception) {
                Log.e(TAG, "Unhandled error generating voice", e)
                _errorMessage.value = "Đã xảy ra lỗi không xác định: ${e.localizedMessage ?: e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Reads the input stream and writes it as an MP3 file to device system.
     */
    private fun saveAudioStreamToCache(inputStream: java.io.InputStream): File? {
        return try {
            val context = getApplication<Application>()
            // Creating folder specifically for cached audios on filesDir
            val audioDir = File(context.filesDir, "tts_audios")
            if (!audioDir.exists()) {
                audioDir.mkdirs()
            }

            val filename = "tts_${System.currentTimeMillis()}.mp3"
            val file = File(audioDir, filename)

            inputStream.use { source ->
                FileOutputStream(file).use { target ->
                    val buffer = ByteArray(4096)
                    var bytesRead: Int
                    while (source.read(buffer).also { bytesRead = it } != -1) {
                        target.write(buffer, 0, bytesRead)
                    }
                    target.flush()
                }
            }
            file
        } catch (e: Exception) {
            Log.e(TAG, "Error saving audio to disk local cache", e)
            null
        }
    }

    /**
     * Plays the audio using MediaPlayer via asynchronous streaming/local loading.
     */
    fun playAudio(item: TtsHistoryItem) {
        val file = File(item.localFilePath)
        
        // Streaming fallback path if local file is missing/deleted
        val isLocal = file.exists()
        val dataSourcePath = if (isLocal) {
            item.localFilePath
        } else {
            try {
                val encodedText = java.net.URLEncoder.encode(item.text, "UTF-8").replace("+", "%20")
                "https://tts-voice-ai.onrender.com/tts?text=$encodedText&voice=${item.voice}"
            } catch (e: Exception) {
                _errorMessage.value = "Tệp âm thanh không còn sẵn dùng!"
                return
            }
        }

        stopAudio() // Stop current playback

        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(dataSourcePath)
                _playingItemId.value = item.id
                
                setOnPreparedListener {
                    start()
                }

                setOnCompletionListener {
                    _playingItemId.value = null
                    stopAudio()
                }

                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "MediaPlayer error: what = $what, extra = $extra")
                    _playingItemId.value = null
                    stopAudio()
                    _errorMessage.value = "Đã xảy ra lỗi khi phát âm thanh tệp này."
                    true
                }

                prepareAsync() // ASYNC prepare is mandatory for robust streaming & local performance
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error playing audio path: $dataSourcePath", e)
            _errorMessage.value = "Không thể phát âm thanh: ${e.localizedMessage ?: e.message}"
            _playingItemId.value = null
        }
    }

    /**
     * Stops currently playing audio and releases resource.
     */
    fun stopAudio() {
        try {
            mediaPlayer?.let { player ->
                if (player.isPlaying) {
                    player.stop()
                }
                player.release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing MediaPlayer", e)
        } finally {
            mediaPlayer = null
            _playingItemId.value = null
        }
    }

    /**
     * Controls pruning the history to maximum allowed count.
     */
    private suspend fun pruneCacheToMaxCount(maxLimit: Int) {
        withContext(Dispatchers.IO) {
            val fullList = repository.getAllHistoryList()
            if (fullList.size > maxLimit) {
                val subListToDelete = fullList.subList(maxLimit, fullList.size)
                for (item in subListToDelete) {
                    try {
                        val file = File(item.localFilePath)
                        if (file.exists()) {
                            file.delete()
                            Log.d(TAG, "Pruned local cached file: ${item.localFilePath}")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error removing file during pruning cache", e)
                    }
                    repository.deleteById(item.id)
                    Log.d(TAG, "Pruned Room Database row ID: ${item.id}")
                }
            }
        }
    }

    /**
     * Deletes a specific history record and its on-device cached file.
     */
    fun deleteHistoryItem(item: TtsHistoryItem) {
        viewModelScope.launch {
            if (_playingItemId.value == item.id) {
                stopAudio()
            }
            withContext(Dispatchers.IO) {
                try {
                    val file = File(item.localFilePath)
                    if (file.exists()) {
                        file.delete()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error deleting audio file", e)
                }
                repository.delete(item)
            }
        }
    }

    /**
     * Clears all history items, deletes files, and resets player.
     */
    fun clearAllHistory() {
        viewModelScope.launch {
            stopAudio()
            withContext(Dispatchers.IO) {
                val list = repository.getAllHistoryList()
                for (item in list) {
                    try {
                        val file = File(item.localFilePath)
                        if (file.exists()) {
                            file.delete()
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error purging files", e)
                    }
                }
                repository.clearHistory()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopAudio()
    }
}
