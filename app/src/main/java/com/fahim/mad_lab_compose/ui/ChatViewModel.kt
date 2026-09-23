package com.fahim.mad_lab_compose.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fahim.mad_lab_compose.data.database.MemoryEntity
import com.fahim.mad_lab_compose.data.database.SummaryEntity
import com.fahim.mad_lab_compose.data.repository.ChatRepository
import com.fahim.mad_lab_compose.data.repository.ResultState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel(
    private val repository: ChatRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ChatState(isApiKeyConfigured = repository.isApiKeyConfigured())
    )
    val uiState: StateFlow<ChatState> = _uiState.asStateFlow()

    init {
        observeMessages()
        observeMemories()
        observeSummaries()
    }

    private fun observeMessages() {
        viewModelScope.launch {
            repository.getAllMessages()
                .catch { e ->
                    _uiState.update { it.copy(errorMessage = "Failed to load messages: ${e.message}") }
                }
                .collect { messagesList ->
                    _uiState.update { it.copy(messages = messagesList) }
                }
        }
    }

    private fun observeMemories() {
        viewModelScope.launch {
            repository.getAllMemories()
                .catch { e ->
                    _uiState.update { it.copy(errorMessage = "Failed to load memories: ${e.message}") }
                }
                .collect { memoriesList ->
                    _uiState.update { it.copy(memories = memoriesList) }
                }
        }
    }

    private fun observeSummaries() {
        viewModelScope.launch {
            repository.getAllSummaries()
                .catch { e ->
                    _uiState.update { it.copy(errorMessage = "Failed to load summaries: ${e.message}") }
                }
                .collect { summariesList ->
                    _uiState.update { it.copy(summaries = summariesList) }
                }
        }
    }

    fun onInputTextChanged(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun sendMessage(explicitText: String? = null) {
        val messageToSend = (explicitText ?: _uiState.value.inputText).trim()
        if (messageToSend.isBlank() || _uiState.value.isLoading) return

        // Clear input text and set loading
        _uiState.update {
            it.copy(
                inputText = "",
                isLoading = true,
                errorMessage = null
            )
        }

        viewModelScope.launch {
            when (val result = repository.sendMessage(messageToSend)) {
                is ResultState.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                }
                is ResultState.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.message,
                            isApiKeyConfigured = repository.isApiKeyConfigured()
                        )
                    }
                }
            }
        }
    }

    // --- Summary Methods ---

    fun generateConversationSummary() {
        if (_uiState.value.messages.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "No conversation messages to summarize yet. Chat first!") }
            return
        }

        _uiState.update {
            it.copy(
                isGeneratingSummary = true,
                showSummaryDialog = true,
                currentSummary = null,
                errorMessage = null
            )
        }

        viewModelScope.launch {
            when (val result = repository.generateConversationSummary()) {
                is ResultState.Success -> {
                    _uiState.update {
                        it.copy(
                            isGeneratingSummary = false,
                            currentSummary = result.data
                        )
                    }
                }
                is ResultState.Error -> {
                    _uiState.update {
                        it.copy(
                            isGeneratingSummary = false,
                            errorMessage = result.message
                        )
                    }
                }
            }
        }
    }

    fun saveSummary(title: String, summaryText: String) {
        viewModelScope.launch {
            repository.saveSummary(title, summaryText)
        }
    }

    fun deleteSummary(summary: SummaryEntity) {
        viewModelScope.launch {
            repository.deleteSummary(summary)
        }
    }

    fun clearAllSummaries() {
        viewModelScope.launch {
            repository.clearAllSummaries()
        }
    }

    fun setShowSummaryDialog(show: Boolean) {
        _uiState.update { it.copy(showSummaryDialog = show) }
    }

    // --- Memory Operations ---

    fun deleteMemory(memory: MemoryEntity) {
        viewModelScope.launch {
            repository.deleteMemory(memory)
        }
    }

    fun deleteMemoryById(id: Long) {
        viewModelScope.launch {
            repository.deleteMemoryById(id)
        }
    }

    fun addOrUpdateMemory(key: String, value: String) {
        val trimmedKey = key.trim()
        val trimmedValue = value.trim()
        if (trimmedKey.isBlank() || trimmedValue.isBlank()) return

        viewModelScope.launch {
            repository.saveOrUpdateMemory(trimmedKey, trimmedValue)
            setShowAddMemoryDialog(false)
        }
    }

    fun clearAllMemories() {
        viewModelScope.launch {
            repository.clearAllMemories()
            setShowClearMemoriesDialog(false)
        }
    }

    fun clearChatHistory() {
        viewModelScope.launch {
            repository.clearChatHistory()
            setShowClearChatDialog(false)
        }
    }

    fun setApiKey(apiKey: String) {
        repository.setCustomApiKey(apiKey)
        _uiState.update {
            it.copy(
                isApiKeyConfigured = repository.isApiKeyConfigured(),
                showApiKeyDialog = false,
                errorMessage = null
            )
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun navigateTo(screen: AppScreen) {
        _uiState.update { it.copy(currentScreen = screen) }
    }

    fun setShowApiKeyDialog(show: Boolean) {
        _uiState.update { it.copy(showApiKeyDialog = show) }
    }

    fun setShowAddMemoryDialog(show: Boolean) {
        _uiState.update { it.copy(showAddMemoryDialog = show) }
    }

    fun setShowClearChatDialog(show: Boolean) {
        _uiState.update { it.copy(showClearChatDialog = show) }
    }

    fun setShowClearMemoriesDialog(show: Boolean) {
        _uiState.update { it.copy(showClearMemoriesDialog = show) }
    }
}

class ChatViewModelFactory(
    private val repository: ChatRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChatViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}