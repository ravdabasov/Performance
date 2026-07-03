package com.example.smarttaskmanager.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smarttaskmanager.domain.model.Task
import com.example.smarttaskmanager.domain.repository.TaskRepository
import com.example.smarttaskmanager.domain.usecase.CancelTaskNotificationsUseCase
import com.example.smarttaskmanager.domain.usecase.DeleteTaskUseCase
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Tələb #6: Tamamlanan tasklar ayrıca bölmədə göstərilir, YALNIZ yaradan silə bilər. */
@HiltViewModel
class CompletedTasksViewModel @Inject constructor(
    repository: TaskRepository,
    private val deleteTask: DeleteTaskUseCase,
    private val cancelNotifications: CancelTaskNotificationsUseCase,
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {

    val completedTasks: StateFlow<List<Task>> = repository.getCompletedTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentUid: String? get() = firebaseAuth.currentUser?.uid

    fun onDelete(task: Task) {
        if (task.createdByUid != firebaseAuth.currentUser?.uid) return
        viewModelScope.launch {
            cancelNotifications(task.id)
            deleteTask(task)
        }
    }
}
