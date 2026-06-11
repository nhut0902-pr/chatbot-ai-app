package com.example.services

import android.util.Log
import com.example.models.AiModel
import com.example.models.ChatCompletionRequest
import com.example.models.ChatCompletionResponse
import com.example.models.ProviderType

object ChatService {
    private const val TAG = "ChatService"

    /**
     * Executes the chat completion request by dynamically selecting the correct service
     * representing the corresponding AI Provider based on the model's provider type.
     */
    suspend fun fetchCompletion(
        modelId: String,
        request: ChatCompletionRequest,
        maxRetries: Int = 3
    ): ChatCompletionResponse {
        val model = AiModel.findById(modelId)
        Log.d(TAG, "Routing request for model '$modelId' (Provider: ${model.provider})")

        return when (model.provider) {
            ProviderType.CEREBRAS -> {
                CerebrasService.fetchCompletion(request, maxRetries = maxRetries)
            }
            ProviderType.NVIDIA -> {
                NvidiaService.fetchCompletion(request, maxRetries = maxRetries)
            }
        }
    }
}
