package com.example.providers

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.config.AppConfig
import com.example.data.ChatDatabase
import com.example.data.ChatRepository
import com.example.models.AiModel
import com.example.models.ChatCompletionRequest
import com.example.models.ChatMessage
import com.example.models.Message
import com.example.models.MessageRole
import com.example.models.ProviderType
import com.example.services.ChatService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val sharedPrefs = application.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    private val database = ChatDatabase.getDatabase(application)
    private val repository = ChatRepository(database.messageDao())

    // --- UI STATES ---
    val messagesState: StateFlow<List<Message>> = repository.allMessages
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _selectedModel = MutableStateFlow(getSavedModel())
    val selectedModel: StateFlow<String> = _selectedModel.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _apiError = MutableStateFlow<String?>(null)
    val apiError: StateFlow<String?> = _apiError.asStateFlow()

    private val _showApiKeyError = MutableStateFlow(false)
    val showApiKeyError: StateFlow<Boolean> = _showApiKeyError.asStateFlow()

    init {
        checkApiKeyStatus()
    }

    // --- SHARED PREFERENCES HELPER ---
    private fun getSavedModel(): String {
        return sharedPrefs.getString("selected_model", "gpt-oss-120b") ?: "gpt-oss-120b"
    }

    fun selectModel(model: String) {
        _selectedModel.value = model
        sharedPrefs.edit().putString("selected_model", model).apply()
        checkApiKeyStatus()
    }

    // --- KEY CONFIG CHECK ---
    fun checkApiKeyStatus(): Boolean {
        val model = AiModel.findById(_selectedModel.value)
        val isConfigured = when (model.provider) {
            ProviderType.CEREBRAS -> AppConfig.isCerebrasAvailable()
            ProviderType.NVIDIA -> AppConfig.isNvidiaAvailable()
        }
        _showApiKeyError.value = !isConfigured
        return isConfigured
    }

    // --- CHAT ACTIONS ---
    fun sendMessage(content: String) {
        if (content.trim().isEmpty()) return

        // Validate API Key for the active provider
        if (!checkApiKeyStatus()) {
            _showApiKeyError.value = true
            return
        }

        viewModelScope.launch {
            val activeModel = _selectedModel.value
            val userMessage = Message(
                role = MessageRole.USER,
                content = content,
                model = activeModel
            )
            repository.insert(userMessage)

            performApiRequestAndStream(content, activeModel)
        }
    }

    /**
     * Re-runs the request for the last user message.
     */
    fun regenerateMessage() {
        val history = messagesState.value
        val lastUserMessage = history.lastOrNull { it.role == MessageRole.USER } ?: return

        // Optional: Remove last assistant message if it exists
        viewModelScope.launch {
            if (history.isNotEmpty() && history.last().role == MessageRole.ASSISTANT) {
                repository.deleteById(history.last().id)
            }
            if (!checkApiKeyStatus()) {
                _showApiKeyError.value = true
                return@launch
            }
            performApiRequestAndStream(lastUserMessage.content, _selectedModel.value)
        }
    }

    private suspend fun performApiRequestAndStream(prompt: String, activeModel: String) {
        _isLoading.value = true
        _apiError.value = null

        try {
            // Include history context for standard completion
            val currentHistory = messagesState.value.takeLast(10)
            val apiMessages = currentHistory.map {
                ChatMessage(role = it.role, content = it.content)
            } + ChatMessage(role = MessageRole.USER, content = prompt)

            val response = ChatService.fetchCompletion(
                modelId = activeModel,
                request = ChatCompletionRequest(
                    model = activeModel,
                    messages = apiMessages
                )
            )

            val fullAssistantReply = response.choices.firstOrNull()?.message?.content
                ?: "Không nhận được phản hồi từ máy chủ."

            // Insert temporary message to stream typewriter effect
            val assistantMessageId = repository.insert(
                Message(
                    role = MessageRole.ASSISTANT,
                    content = "",
                    model = activeModel
                )
            )

            // Simulate typewriter streaming response for ultra polish
            val words = fullAssistantReply.split(" ")
            var currentText = ""
            for (i in words.indices) {
                currentText += (if (i == 0) "" else " ") + words[i]
                repository.insert(
                    Message(
                        id = assistantMessageId,
                        role = MessageRole.ASSISTANT,
                        content = currentText,
                        model = activeModel
                    )
                )
                delay(30L) // smooth progressive output
            }

        } catch (e: retrofit2.HttpException) {
            val errorCode = e.code()
            val errorBody = e.response()?.errorBody()?.string() ?: ""
            val activeModelObj = AiModel.findById(activeModel)
            val friendlyMessage = when (errorCode) {
                401, 403 -> "Lỗi xác thực (401/403): Vui lòng kiểm tra lại cấu hình ${activeModelObj.provider} API Key trong panel Secrets."
                404 -> "Không tìm thấy Endpoint: Model '$activeModel' có thể chưa được kích hoạt hoặc không tồn tại trong danh sách."
                429 -> "Giới hạn tần suất (Rate Limit): Bạn đã gửi quá nhiều yêu cầu cùng lúc. Vui lòng thử lại sau ít phút."
                else -> "Lỗi API ($errorCode): $errorBody"
            }
            _apiError.value = friendlyMessage
        } catch (e: java.io.IOException) {
            _apiError.value = "Lỗi mạng hoặc Timeout (60s): Hãy kiểm tra lại kết nối internet của thiết bị."
        } catch (e: Exception) {
            _apiError.value = "Đã xảy ra lỗi không xác định: ${e.localizedMessage ?: e.message}"
        } finally {
            _isLoading.value = false
        }
    }

    fun clearChatHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    fun dismissError() {
        _apiError.value = null
    }

    fun dismissApiKeyError() {
        _showApiKeyError.value = false
    }
}
