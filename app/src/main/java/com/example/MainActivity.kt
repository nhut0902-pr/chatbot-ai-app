package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.example.providers.ChatViewModel
import com.example.screens.ChatScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private val viewModel: ChatViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // This supports drawing the application behind top status bars and bottom navigation gesture bars.
        enableEdgeToEdge()
        
        setContent {
            MyApplicationTheme(
                darkTheme = true, // Force Dark Theme matching ChatGPT style
                dynamicColor = false // Keep consistent ChatGPT sleek dark colors
            ) {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    ChatScreen(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Safeguard to check API Key configuration on window focus/resume
        viewModel.checkApiKeyStatus()
    }
}
