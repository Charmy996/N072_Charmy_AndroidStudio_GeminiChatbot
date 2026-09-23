package com.fahim.mad_lab_compose.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fahim.mad_lab_compose.data.database.MemoryEntity
import com.fahim.mad_lab_compose.data.repository.ChatRepository
import com.fahim.mad_lab_compose.data.repository.ResultState
import com.fahim.mad_lab_compose.voice.VoiceManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel(
    private val repository: ChatRepository,
    private val voiceManager: VoiceManager? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ChatState(isApiKeyConfigured = repository.isApiKeyConfigured())
    )
    val uiState: StateFlow<ChatState> = _uiState.asStateFlow()

    init {
        observeMessages()
        observeMemories()
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

    fun onInputTextChanged(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun sendMessage(explicitText: String? = null) {
        val messageToSend = (explicitText ?: _uiState.value.inputText).trim()
        if (messageToSend.isBlank() || _uiState.value.isLoading) return

        // Stop any active speech when sending a new message
        stopSpeaking()

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

    // --- Voice Interaction Methods ---

    fun startListening() {
        if (voiceManager == null) return
        stopSpeaking()
        voiceManager.startListening(
            onResult = { recognizedText ->
                _uiState.update {
                    val currentText = it.inputText.trim()
                    val newText = if (currentText.isBlank()) recognizedText else "$currentText $recognizedText"
                    it.copy(
                        inputText = newText,
                        isListening = false
                    )
                }
            },
            onError = { error ->
                _uiState.update {
                    it.copy(
                        isListening = false,
                        errorMessage = error
                    )
                }
            },
            onListeningStateChanged = { listening ->
                _uiState.update { it.copy(isListening = listening) }
            }
        )
    }

    fun stopListening() {
        voiceManager?.stopListening()
        _uiState.update { it.copy(isListening = false) }
    }

    fun toggleSpeechForMessage(messageId: Long, text: String) {
        if (voiceManager == null) return

        if (_uiState.value.speakingMessageId == messageId) {
            stopSpeaking()
        } else {
            stopSpeaking()
            _uiState.update { it.copy(speakingMessageId = messageId) }
            voiceManager.speak(
                utteranceId = messageId.toString(),
                text = text,
                onStart = {
                    _uiState.update { it.copy(speakingMessageId = messageId) }
                },
                onDone = {
                    _uiState.update {
                        if (it.speakingMessageId == messageId) it.copy(speakingMessageId = null) else it
                    }
                },
                onError = { error ->
                    _uiState.update {
                        it.copy(
                            speakingMessageId = null,
                            errorMessage = error
                        )
                    }
                }
            )
        }
    }

    fun stopSpeaking() {
        voiceManager?.stopSpeaking()
        _uiState.update { it.copy(speakingMessageId = null) }
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
        stopSpeaking()
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

    override fun onCleared() {
        super.onCleared()
        voiceManager?.shutdown()
    }
}

class ChatViewModelFactory(
    private val repository: ChatRepository,
    private val voiceManager: VoiceManager? = null
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChatViewModel(repository, voiceManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}