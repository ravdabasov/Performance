package com.example.smarttaskmanager.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smarttaskmanager.domain.model.Task
import com.example.smarttaskmanager.domain.model.TaskEisenhowerGroup
import com.example.smarttaskmanager.domain.model.TaskStatus
import com.example.smarttaskmanager.domain.model.groupByEisenhower
import com.example.smarttaskmanager.domain.repository.TaskRepository
import com.example.smarttaskmanager.domain.usecase.CancelTaskNotificationsUseCase
import com.example.smarttaskmanager.domain.usecase.DeleteTaskUseCase
import com.example.smarttaskmanager.domain.usecase.GetTaskByIdUseCase
import com.example.smarttaskmanager.domain.usecase.RefreshOverdueStatusesUseCase
import com.example.smarttaskmanager.domain.usecase.ScheduleTaskNotificationsUseCase
import com.example.smarttaskmanager.domain.usecase.ToggleTaskCompletionUseCase
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TaskListUiState(
    val groupedTasks: Map<TaskEisenhowerGroup, List<Task>> = emptyMap(),
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val currentUid: String? = null
)

/**
 * "Bütün Tasklar" bölməsi - adına uyğun olaraq HƏQİQƏTƏN bütün taskları (tamamlanan/
 * ləğv edilən daxil) göstərir, 4 sabit qrupa (Eisenhower matrisi) bölünmüş şəkildə.
 *
 * İcazə qaydası: task-ı yalnız YARADAN silə bilər və tamamlanmış task-ı geri (aktivə)
 * qaytara bilər. Digər komanda üzvləri task-ı tamamlana (checkbox-u işarələyə) bilər,
 * amma silə və ya geri qaytara bilməz.
 */
@HiltViewModel
class TaskListViewModel @Inject constructor(
    private val repository: TaskRepository,
    private val toggleTaskCompletion: ToggleTaskCompletionUseCase,
    private val deleteTask: DeleteTaskUseCase,
    private val cancelNotifications: CancelTaskNotificationsUseCase,
    private val scheduleNotifications: ScheduleTaskNotificationsUseCase,
    private val getTaskById: GetTaskByIdUseCase,
    private val refreshOverdueStatuses: RefreshOverdueStatusesUseCase,
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")

    init {
        viewModelScope.launch { refreshOverdueStatuses() }
    }

    private val tasksFlow = searchQuery.flatMapLatest { query ->
        if (query.isBlank()) repository.getAllTasksSmartSorted() else repository.searchTasks(query)
    }

    val uiState: StateFlow<TaskListUiState> = combine(tasksFlow, searchQuery) { tasks, query ->
        TaskListUiState(
            groupedTasks = tasks.groupByEisenhower(),
            searchQuery = query,
            isLoading = false,
            currentUid = firebaseAuth.currentUser?.uid
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TaskListUiState()
    )

    fun onSearchQueryChanged(query: String) {
        searchQuery.value = query
    }

    /**
     * Checkbox: hər kəs task-ı TAMAMLAYA bilər. Amma geri (aktivə) qaytarmaq
     * yalnız task-ı yaradan şəxsə icazəlidir.
     */
    fun onToggleComplete(task: Task) {
        val isOwner = task.createdByUid == firebaseAuth.currentUser?.uid
        if (task.status == TaskStatus.COMPLETED && !isOwner) return // icazə yoxdur

        viewModelScope.launch {
            toggleTaskCompletion(task.id)
            val updated = getTaskById(task.id) ?: return@launch
            scheduleNotifications(updated)
        }
    }

    /** Silmə yalnız task-ı yaradan şəxsə icazəlidir. */
    fun onDelete(task: Task) {
        val isOwner = task.createdByUid == firebaseAuth.currentUser?.uid
        if (!isOwner) return

        viewModelScope.launch {
            cancelNotifications(task.id)
            deleteTask(task)
        }
    }
}
