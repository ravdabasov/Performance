package com.example.smarttaskmanager.notification

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.smarttaskmanager.domain.repository.TaskNotificationScheduler
import com.example.smarttaskmanager.domain.repository.TaskRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

/**
 * Telefon restart olduqda (BootReceiver tərəfindən) planlaşdırılan tək dəfəlik iş.
 * WorkManager istifadə olunur çünki bu, sistemdən bağımsız, etibarlı şəkildə
 * icra olunmasını təmin edir (constraints, retry-lar daxil olmaqla) - tələb #5-ə uyğun.
 */
@HiltWorker
class RescheduleAllAlarmsWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: TaskRepository,
    private val scheduler: TaskNotificationScheduler
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            repository.refreshOverdueStatuses()
            val activeTasks = repository.getActiveTasksSmartSorted().first()
            scheduler.rescheduleAll(activeTasks)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "reschedule_all_alarms_work"
    }
}
