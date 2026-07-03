package com.example.smarttaskmanager.domain.usecase

import com.example.smarttaskmanager.domain.model.Task
import com.example.smarttaskmanager.domain.repository.TaskRepository
import javax.inject.Inject

/** Nəticə tipi - uğur/uğursuzluğu Exception atmadan, tip-təhlükəsiz şəkildə bildirir. */
sealed class TaskValidationResult {
    data class Success(val taskId: Long) : TaskValidationResult()
    data class Error(val messageAz: String) : TaskValidationResult()
}

/**
 * Yeni task yaratmadan əvvəl Input Validation aparır (Təhlükəsizlik tələbi #12).
 * Tək məsuliyyət: task yaratmaq + doğrulama. Bildiriş planlaşdırması ayrı use case-dədir.
 */
class CreateTaskUseCase @Inject constructor(
    private val repository: TaskRepository
) {
    suspend operator fun invoke(task: Task): TaskValidationResult {
        if (task.title.isBlank()) {
            return TaskValidationResult.Error("Task adı boş ola bilməz.")
        }
        if (task.title.length > 120) {
            return TaskValidationResult.Error("Task adı çox uzundur (maks. 120 simvol).")
        }
        if (task.priority !in 1..10) {
            return TaskValidationResult.Error("Prioritet 1 ilə 10 arasında olmalıdır.")
        }
        if (task.deadline.isBefore(task.date)) {
            return TaskValidationResult.Error("Deadline, başlanğıc tarixindən əvvəl ola bilməz.")
        }
        val id = repository.createTask(task)
        return TaskValidationResult.Success(id)
    }
}
