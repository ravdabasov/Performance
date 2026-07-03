package com.example.smarttaskmanager.domain.usecase

import com.example.smarttaskmanager.domain.model.Task
import com.example.smarttaskmanager.domain.repository.TaskNotificationScheduler
import javax.inject.Inject

class ScheduleTaskNotificationsUseCase @Inject constructor(
    private val scheduler: TaskNotificationScheduler
) {
    operator fun invoke(task: Task) {
        // Tamamlanmış/ləğv edilmiş taskalar üçün bildiriş lazım deyil.
        if (task.status.name == "COMPLETED" || task.status.name == "CANCELLED") {
            scheduler.cancelForTask(task.id)
        } else {
            scheduler.scheduleForTask(task)
        }
    }
}

class CancelTaskNotificationsUseCase @Inject constructor(
    private val scheduler: TaskNotificationScheduler
) {
    operator fun invoke(taskId: Long) = scheduler.cancelForTask(taskId)
}
