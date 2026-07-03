package com.example.smarttaskmanager.domain.repository

import com.example.smarttaskmanager.domain.model.Task

/**
 * Domain qatı AlarmManager/WorkManager kimi Android framework detallarını bilmək
 * istəmir - buna görə abstraksiya yaradılıb (Dependency Inversion). Konkret
 * implementasiya notification paketindədir.
 */
interface TaskNotificationScheduler {
    /** Task-ın bütün seçilmiş NotificationTiming-ləri üçün alarm planlaşdırır. */
    fun scheduleForTask(task: Task)

    /** Task silinəndə/tamamlananda əvvəlki planlaşdırılmış bütün alarmları ləğv edir. */
    fun cancelForTask(taskId: Long)

    /** Telefon restart olduqdan sonra bütün aktiv tasklar üçün alarmları yenidən qurur. */
    fun rescheduleAll(tasks: List<Task>)
}
