package com.fahim.mad_lab_compose.ui

import com.fahim.mad_lab_compose.data.database.MemoryEntity
import com.fahim.mad_lab_compose.data.database.MessageEntity
import com.fahim.mad_lab_compose.data.database.SummaryEntity

enum class AppScreen {
    CHAT,
    MEMORY
}

data class ChatState(
    val messages: List<MessageEntity> = emptyList(),
    val memories: List<MemoryEntity> = emptyList(),
    val summaries: List<SummaryEntity> = emptyList(),
    val inputText: String = "",
    val isLoading: Boolean = false,
    val isGeneratingSummary: Boolean = false,
    val errorMessage: String? = null,
    val isApiKeyConfigured: Boolean = true,
    val currentScreen: AppScreen = AppScreen.CHAT,
    val showApiKeyDialog: Boolean = false,
    val showAddMemoryDialog: Boolean = false,
    val showClearChatDialog: Boolean = false,
    val showClearMemoriesDialog: Boolean = false,
    val showSummaryDialog: Boolean = false,
    val currentSummary: String? = null
)