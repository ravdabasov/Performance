package com.example.smarttaskmanager.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smarttaskmanager.domain.model.NotificationTiming
import com.example.smarttaskmanager.domain.model.RecurrenceType
import com.example.smarttaskmanager.domain.model.Task
import com.example.smarttaskmanager.domain.model.TaskStatus
import com.example.smarttaskmanager.domain.model.AppUser
import com.example.smarttaskmanager.domain.model.eisenhowerGroup
import com.example.smarttaskmanager.domain.repository.UserRepository
import com.example.smarttaskmanager.domain.usecase.CreateTaskUseCase
import com.example.smarttaskmanager.domain.usecase.GetTaskByIdUseCase
import com.example.smarttaskmanager.domain.usecase.ScheduleTaskNotificationsUseCase
import com.example.smarttaskmanager.domain.usecase.TaskValidationResult
import com.example.smarttaskmanager.domain.usecase.UpdateTaskUseCase
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

data class AddEditTaskUiState(
    val id: Long = 0L,
    val title: String = "",
    val description: String = "",
    val date: LocalDateTime = LocalDateTime.now(),
    val deadline: LocalDateTime = LocalDateTime.now().plusHours(1),
    val priority: Int = 5,
    val tagsInput: String = "",
    val notificationTimings: Set<NotificationTiming> = setOf(NotificationTiming.AT_DEADLINE),
    val customNotificationEnabled: Boolean = false,
    val customNotificationTime: LocalDateTime = LocalDateTime.now().plusMinutes(30),
    val recurrenceType: RecurrenceType = RecurrenceType.NONE,
    val status: TaskStatus = TaskStatus.PENDING,
    val createdByUid: String = "",
    val createdByName: String = "",
    val completedByName: String? = null,
    val assigneeUid: String? = null,
    val assigneeName: String? = null,
    val isEditMode: Boolean = false,
    val currentUid: String? = null,
    val errorMessage: String? = null,
    val isSaved: Boolean = false
) {
    /**
     * Kateqoriya artıq əl ilə yazılmır - prioritet balı + deadline-a görə 4 sabit
     * qrupdan (Eisenhower matrisi) birinə avtomatik təyin olunur. Formada canlı
     * göstərilir ki, istifadəçi task-ın hansı qrupa düşəcəyini əvvəlcədən görsün.
     */
    val computedCategory: String
        get() = Task(
            title = title.ifBlank { "-" },
            date = date,
            deadline = deadline,
            priority = priority,
            status = status
        ).eisenhowerGroup().titleAz

    /**
     * Tələb: task-ı YALNIZ yaradan redaktə edə bilər (deadline, prioritet, adı və s.
     * dəyişdirə bilər). Yeni task yaradarkən (hələ isEditMode=false) hər kəs sərbəstdir.
     */
    val canEdit: Boolean
        get() = !isEditMode || createdByUid == currentUid
}

/**
 * Həm "yeni task yarat", həm də "mövcud task-ı redaktə et" ssenarilərini idarə edir.
 * SavedStateHandle-dan gələn "taskId" naviqasiya arqumenti edit rejimini müəyyən edir.
 */
@HiltViewModel
class AddEditTaskViewModel @Inject constructor(
    private val createTask: CreateTaskUseCase,
    private val updateTask: UpdateTaskUseCase,
    private val getTaskById: GetTaskByIdUseCase,
    private val scheduleNotifications: ScheduleTaskNotificationsUseCase,
    private val firebaseAuth: FirebaseAuth,
    userRepository: UserRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddEditTaskUiState())
    val uiState: StateFlow<AddEditTaskUiState> = _uiState.asStateFlow()

    /** Cavabdeh seçimi üçün qeydiyyatdan keçmiş bütün komanda üzvləri. */
    val users: StateFlow<List<AppUser>> = userRepository.getAllUsers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        _uiState.value = _uiState.value.copy(currentUid = firebaseAuth.currentUser?.uid)
        val taskId: Long = savedStateHandle.get<Long>("taskId") ?: 0L
        if (taskId != 0L) {
            viewModelScope.launch {
                getTaskById(taskId)?.let { task ->
                    _uiState.value = AddEditTaskUiState(
                        id = task.id,
                        title = task.title,
                        description = task.description,
                        date = task.date,
                        deadline = task.deadline,
                        priority = task.priority,
                        tagsInput = task.tags.joinToString(", "),
                        notificationTimings = task.notificationTimings,
                        customNotificationEnabled = task.customNotificationTime != null,
                        customNotificationTime = task.customNotificationTime ?: LocalDateTime.now().plusMinutes(30),
                        recurrenceType = task.recurrenceType,
                        status = task.status,
                        createdByUid = task.createdByUid,
                        createdByName = task.createdByName,
                        completedByName = task.completedByName,
                        assigneeUid = task.assigneeUid,
                        assigneeName = task.assigneeName,
                        isEditMode = true,
                        currentUid = firebaseAuth.currentUser?.uid
                    )
                }
            }
        }
    }

    fun onTitleChanged(v: String) = update { it.copy(title = v, errorMessage = null) }
    fun onDescriptionChanged(v: String) = update { it.copy(description = v) }
    fun onDateChanged(v: LocalDateTime) = update { it.copy(date = v) }
    fun onDeadlineChanged(v: LocalDateTime) = update { it.copy(deadline = v) }
    fun onPriorityChanged(v: Int) = update { it.copy(priority = v.coerceIn(1, 10)) }
    fun onTagsChanged(v: String) = update { it.copy(tagsInput = v) }
    fun onToggleNotificationTiming(timing: NotificationTiming) = update {
        val current = it.notificationTimings
        it.copy(notificationTimings = if (timing in current) current - timing else current + timing)
    }
    fun onToggleCustomNotification() = update { it.copy(customNotificationEnabled = !it.customNotificationEnabled) }
    fun onCustomNotificationTimeChanged(v: LocalDateTime) = update { it.copy(customNotificationTime = v) }
    fun onRecurrenceChanged(v: RecurrenceType) = update { it.copy(recurrenceType = v) }
    fun onAssigneeChanged(user: AppUser?) = update {
        it.copy(assigneeUid = user?.uid, assigneeName = user?.displayName)
    }

    fun save() {
        val state = _uiState.value
        if (!state.canEdit) return // icazəsiz redaktəyə qarşı əlavə qoruma
        val category = state.computedCategory
        val task = Task(
            id = state.id,
            title = state.title.trim(),
            description = state.description.trim(),
            date = state.date,
            deadline = state.deadline,
            priority = state.priority,
            category = category,
            tags = state.tagsInput.split(",").map { it.trim() }.filter { it.isNotBlank() },
            status = state.status,
            notificationTimings = state.notificationTimings,
            customNotificationTime = if (state.customNotificationEnabled) state.customNotificationTime else null,
            recurrenceType = state.recurrenceType,
            createdByUid = if (state.isEditMode) state.createdByUid else (firebaseAuth.currentUser?.uid ?: ""),
            createdByName = if (state.isEditMode) state.createdByName else currentDisplayName(),
            completedByName = state.completedByName,
            assigneeUid = state.assigneeUid,
            assigneeName = state.assigneeName
        )

        viewModelScope.launch {
            val result = if (state.isEditMode) updateTask(task) else createTask(task)
            when (result) {
                is TaskValidationResult.Success -> {
                    scheduleNotifications(task.copy(id = result.taskId))
                    _uiState.value = state.copy(isSaved = true, errorMessage = null)
                }
                is TaskValidationResult.Error -> {
                    _uiState.value = state.copy(errorMessage = result.messageAz)
                }
            }
        }
    }

    private inline fun update(block: (AddEditTaskUiState) -> AddEditTaskUiState) {
        _uiState.value = block(_uiState.value)
    }

    private fun currentDisplayName(): String =
        firebaseAuth.currentUser?.displayName?.ifBlank { null }
            ?: firebaseAuth.currentUser?.email
            ?: "Naməlum"
}
