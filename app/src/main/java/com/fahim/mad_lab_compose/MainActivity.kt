package com.fahim.mad_lab_compose

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fahim.mad_lab_compose.ui.AppScreen
import com.fahim.mad_lab_compose.ui.ChatScreen
import com.fahim.mad_lab_compose.ui.ChatViewModel
import com.fahim.mad_lab_compose.ui.ChatViewModelFactory
import com.fahim.mad_lab_compose.ui.MemoryScreen
import com.fahim.mad_lab_compose.ui.theme.GeminiChatbotTheme

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // Permission result will be handled by the voice helper
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val app = application as ChatbotApplication
            val chatViewModel: ChatViewModel = viewModel(
                factory = ChatViewModelFactory(app.repository, applicationContext)
            )

            GeminiChatbotTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ChatbotApp(
                        viewModel = chatViewModel,
                        onRequestMicPermission = { requestMicrophonePermission() }
                    )
                }
            }
        }
    }

    private fun requestMicrophonePermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }
}

@Composable
fun ChatbotApp(viewModel: ChatViewModel, onRequestMicPermission: () -> Unit) {
    val state by viewModel.uiState.collectAsState()

    // Handle back button when on Memory screen
    BackHandler(enabled = state.currentScreen == AppScreen.MEMORY) {
        viewModel.navigateTo(AppScreen.CHAT)
    }

    Crossfade(
        targetState = state.currentScreen,
        label = "ScreenTransition"
    ) { screen ->
        when (screen) {
            AppScreen.CHAT -> {
                ChatScreen(
                    state = state,
                    onInputTextChanged = { text -> viewModel.onInputTextChanged(text) },
                    onSendMessage = { explicit -> viewModel.sendMessage(explicit) },
                    onOpenMemories = { viewModel.navigateTo(AppScreen.MEMORY) },
                    onOpenApiKeyDialog = { viewModel.setShowApiKeyDialog(true) },
                    onShowClearChatDialog = { show -> viewModel.setShowClearChatDialog(show) },
                    onConfirmClearChat = { viewModel.clearChatHistory() },
                    onDismissError = { viewModel.dismissError() },
                    onSaveApiKey = { key -> viewModel.setApiKey(key) },
                    onStartVoiceRecognition = {
                        onRequestMicPermission()
                        viewModel.startVoiceRecognition()
                    },
                    onStopVoiceRecognition = { viewModel.stopVoiceRecognition() },
                    onSpeakText = { text -> viewModel.speakText(text) },
                    onStopSpeaking = { viewModel.stopSpeaking() }
                )
            }
            AppScreen.MEMORY -> {
                MemoryScreen(
                    state = state,
                    onBack = { viewModel.navigateTo(AppScreen.CHAT) },
                    onDeleteMemory = { mem -> viewModel.deleteMemory(mem) },
                    onAddMemory = { k, v -> viewModel.addOrUpdateMemory(k, v) },
                    onClearAllMemories = { viewModel.clearAllMemories() },
                    onShowAddDialog = { show -> viewModel.setShowAddMemoryDialog(show) },
                    onShowClearDialog = { show -> viewModel.setShowClearMemoriesDialog(show) }
                )
            }
        }
    }
}