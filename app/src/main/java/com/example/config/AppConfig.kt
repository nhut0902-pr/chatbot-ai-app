package com.example.config

object AppConfig {
    /**
     * Retrieves the Cerebras API Key.
     */
    val cerebrasKey: String
        get() = "csk-rxw4j4eh8d8h45myx6cjte8ey95pdww5hdw664exetxx682h"

    /**
     * Retrieves the NVIDIA API Key.
     */
    val nvidiaKey: String
        get() = "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"

    /**
     * Checks if Cerebras API Key is configured.
     */
    fun isCerebrasAvailable(): Boolean {
        val key = cerebrasKey.trim()
        return key.isNotEmpty() && 
               key != "MY_CEREBRAS_API_KEY" && 
               key != "CEREBRAS_API_KEY" &&
               key.startsWith("csk-")
    }

    /**
     * Checks if NVIDIA API Key is configured.
     */
    fun isNvidiaAvailable(): Boolean {
        val key = nvidiaKey.trim()
        return key.isNotEmpty() && 
               key != "MY_NVIDIA_API_KEY" && 
               key != "NVIDIA_API_KEY" &&
               key.startsWith("nvapi-")
    }

    /**
     * Helper to keep compatibility or general status check.
     */
    fun isApiKeyConfigured(): Boolean {
        return isCerebrasAvailable() || isNvidiaAvailable()
    }
}
