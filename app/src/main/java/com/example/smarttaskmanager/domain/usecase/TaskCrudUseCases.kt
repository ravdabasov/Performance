package com.example.smarttaskmanager.domain.usecase

import com.example.smarttaskmanager.domain.model.RecurrenceType
import com.example.smarttaskmanager.domain.model.Task
import com.example.smarttaskmanager.domain.model.TaskStatus
import com.example.smarttaskmanager.domain.repository.TaskNotificationScheduler
import com.example.smarttaskmanager.domain.repository.TaskRepository
import com.google.firebase.auth.FirebaseAuth
import java.time.LocalDateTime
import javax.inject.Inject

class UpdateTaskUseCase @Inject constructor(private val repository: TaskRepository) {
    suspend operator fun invoke(task: Task): TaskValidationResult {
        if (task.title.isBlank()) return TaskValidationResult.Error("Task adı boş ola bilməz.")
        if (task.priority !in 1..10) return TaskValidationResult.Error("Prioritet 1-10 arasında olmalıdır.")
        if (task.deadline.isBefore(task.date)) {
            return TaskValidationResult.Error("Deadline, başlanğıc tarixindən əvvəl ola bilməz.")
        }
        repository.updateTask(task)
        return TaskValidationResult.Success(task.id)
    }
}

class DeleteTaskUseCase @Inject constructor(private val repository: TaskRepository) {
    suspend operator fun invoke(task: Task) = repository.deleteTask(task)
}

class CompleteTaskUseCase @Inject constructor(private val repository: TaskRepository) {
    suspend operator fun invoke(taskId: Long) = repository.markTaskCompleted(taskId)
}

/**
 * Checkbox-un hər iki istiqamətdə işləməsi üçün: tamamla / geri qaytar.
 * Tələb: task təkrarlanan (Gündəlik/Həftəlik/Aylıq) olaraq işarələnibsə, TAMAMLANDIQDA
 * (geri qaytarılanda YOX) avtomatik olaraq növbəti dövr üçün eyni parametrlərlə yeni
 * task yaradılır və onun bildirişləri planlaşdırılır.
 */
class ToggleTaskCompletionUseCase @Inject constructor(
    private val repository: TaskRepository,
    private val scheduler: TaskNotificationScheduler,
    private val firebaseAuth: FirebaseAuth
) {
    suspend operator fun invoke(taskId: Long) {
        val before = repository.getTaskById(taskId) ?: return
        repository.toggleTaskCompleted(taskId)
        val after = repository.getTaskById(taskId) ?: return

        val justCompleted = before.status != TaskStatus.COMPLETED && after.status == TaskStatus.COMPLETED
        if (justCompleted) {
            // Kim tamamladığını komanda üzvlərinə göstərmək üçün qeyd edir.
            val completerName = firebaseAuth.currentUser?.displayName?.ifBlank { null }
                ?: firebaseAuth.currentUser?.email ?: "Naməlum"
            repository.updateTask(after.copy(completedByName = completerName))

            if (after.recurrenceType != RecurrenceType.NONE) {
                val next = buildNextOccurrence(after)
                val newId = repository.createTask(next)
                scheduler.scheduleForTask(next.copy(id = newId))
            }
        } else if (before.status == TaskStatus.COMPLETED && after.status != TaskStatus.COMPLETED) {
            // Geri qaytarılanda "kim tamamladı" işarəsi təmizlənir.
            repository.updateTask(after.copy(completedByName = null))
        }
    }

    private fun buildNextOccurrence(task: Task): Task {
        val shift: (LocalDateTime) -> LocalDateTime = when (task.recurrenceType) {
            RecurrenceType.DAILY -> { dt -> dt.plusDays(1) }
            RecurrenceType.WEEKLY -> { dt -> dt.plusWeeks(1) }
            RecurrenceType.MONTHLY -> { dt -> dt.plusMonths(1) }
            RecurrenceType.NONE -> { dt -> dt }
        }
        return task.copy(
            id = 0L,
            date = shift(task.date),
            deadline = shift(task.deadline),
            status = TaskStatus.PENDING,
            completedAt = null,
            createdAt = LocalDateTime.now(),
            customNotificationTime = task.customNotificationTime?.let(shift)
        )
    }
}

class GetTaskByIdUseCase @Inject constructor(private val repository: TaskRepository) {
    suspend operator fun invoke(taskId: Long): Task? = repository.getTaskById(taskId)
}

class RefreshOverdueStatusesUseCase @Inject constructor(private val repository: TaskRepository) {
    suspend operator fun invoke() = repository.refreshOverdueStatuses()
}
