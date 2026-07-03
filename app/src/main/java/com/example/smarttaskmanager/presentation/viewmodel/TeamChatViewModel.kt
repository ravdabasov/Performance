package com.example.smarttaskmanager.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smarttaskmanager.domain.model.ChatMessage
import com.example.smarttaskmanager.domain.repository.TeamChatRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TeamChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = ""
)

/** Komandanın ümumi (tasklardan asılı olmayan) söhbət kanalı. */
@HiltViewModel
class TeamChatViewModel @Inject constructor(
    private val chatRepository: TeamChatRepository,
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {

    private val _inputText = MutableStateFlow("")

    val uiState: StateFlow<TeamChatUiState> = combine(
        chatRepository.getMessages(), _inputText
    ) { messages, input ->
        TeamChatUiState(messages = messages, inputText = input)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TeamChatUiState())

    val currentUid: String? get() = firebaseAuth.currentUser?.uid

    fun onInputChanged(v: String) {
        _inputText.value = v
    }

    fun sendMessage() {
        val text = _inputText.value.trim()
        if (text.isBlank()) return
        val uid = firebaseAuth.currentUser?.uid ?: return
        val name = firebaseAuth.currentUser?.displayName?.ifBlank { null }
            ?: firebaseAuth.currentUser?.email ?: "Naməlum"
        _inputText.value = ""
        viewModelScope.launch {
            chatRepository.sendMessage(uid, name, text)
        }
    }

    fun clearChat() {
        viewModelScope.launch {
            chatRepository.clearAllMessages()
        }
    }
}
