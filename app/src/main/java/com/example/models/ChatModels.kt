package com.example.models

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ProviderType {
    CEREBRAS,
    NVIDIA
}

data class AiModel(
    val id: String,
    val name: String,
    val provider: ProviderType
) {
    companion object {
        val ALL_MODELS = listOf(
            // Cerebras
            AiModel("gpt-oss-120b", "GPT OSS 120B", ProviderType.CEREBRAS),
            AiModel("zai-glm-4.7", "GLM 4.7", ProviderType.CEREBRAS),
            // NVIDIA
            AiModel("GLM-5", "GLM 5", ProviderType.NVIDIA),
            AiModel("Kimi K2.6", "Kimi K2.6", ProviderType.NVIDIA),
            AiModel("Qwen3.6 35B", "Qwen3.6 35B", ProviderType.NVIDIA),
            AiModel("Mistral Small 4", "Mistral Small 4", ProviderType.NVIDIA),
            AiModel("Gemma 4 31B", "Gemma 4 31B", ProviderType.NVIDIA),
            AiModel("Nemotron Super 120B", "Nemotron Super 120B", ProviderType.NVIDIA),
            AiModel("DeepSeek Coder V2 Lite", "DeepSeek Coder V2 Lite", ProviderType.NVIDIA)
        )

        fun findById(id: String): AiModel {
            return ALL_MODELS.find { it.id.equals(id, ignoreCase = true) } 
                ?: ALL_MODELS.first()
        }
    }
}

// --- ROOM ENTITY FOR CHAT HISTORY ---
@Entity(tableName = "chat_messages")
data class Message(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val role: String, // "user", "assistant", "system"
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val model: String = "" // Under which model this message was created/sent
)

// --- SYSTEM ROLE HELPER ---
object MessageRole {
    const val USER = "user"
    const val ASSISTANT = "assistant"
    const val SYSTEM = "system"
}

// --- CEREBRAS API MODELS ---
data class ChatMessage(
    val role: String,
    val content: String
)

data class ChatCompletionRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val stream: Boolean = false
)

data class ChatChoice(
    val index: Int,
    val message: ChatMessage,
    val finish_reason: String?
)

data class ChatCompletionUsage(
    val prompt_tokens: Int?,
    val completion_tokens: Int?,
    val total_tokens: Int?
)

data class ChatCompletionResponse(
    val id: String,
    val `object`: String?,
    val created: Long,
    val model: String,
    val choices: List<ChatChoice>,
    val usage: ChatCompletionUsage?
)
