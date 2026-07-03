package com.example.smarttaskmanager.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smarttaskmanager.domain.model.Task
import com.example.smarttaskmanager.domain.model.TaskComment
import com.example.smarttaskmanager.domain.repository.TaskCommentRepository
import com.example.smarttaskmanager.domain.usecase.GetTaskByIdUseCase
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TaskChatUiState(
    val task: Task? = null,
    val messages: List<TaskComment> = emptyList(),
    val inputText: String = ""
)

/** Tapşırıq daxilindəki real-vaxt müzakirə (chat). */
@HiltViewModel
class TaskChatViewModel @Inject constructor(
    private val commentRepository: TaskCommentRepository,
    private val getTaskById: GetTaskByIdUseCase,
    private val firebaseAuth: FirebaseAuth,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val taskId: Long = savedStateHandle.get<Long>("taskId") ?: 0L

    private val _inputText = MutableStateFlow("")
    private val _task = MutableStateFlow<Task?>(null)

    val uiState: StateFlow<TaskChatUiState> = combine(
        commentRepository.getComments(taskId), _task, _inputText
    ) { messages, task, input ->
        TaskChatUiState(task = task, messages = messages, inputText = input)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TaskChatUiState())

    init {
        viewModelScope.launch {
            _task.value = getTaskById(taskId)
        }
    }

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
            commentRepository.sendComment(taskId, uid, name, text)
        }
    }

    fun clearChat() {
        viewModelScope.launch {
            commentRepository.clearComments(taskId)
        }
    }

    val currentUid: String? get() = firebaseAuth.currentUser?.uid
}
