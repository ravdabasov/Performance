package com.example.smarttaskmanager.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.smarttaskmanager.domain.model.NotificationTiming
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * AlarmManager tetiklədikdə çağırılan receiver. Xüsusi (custom) bildiriş vaxtı üçün
 * ayrıca mesaj, "Deadline zamanı" seçimi üçün ayrıca mesaj göstərir.
 */
@AndroidEntryPoint
class TaskAlarmReceiver : BroadcastReceiver() {

    @Inject lateinit var notificationHelper: NotificationHelper

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(AlarmSchedulerImpl.EXTRA_TASK_ID, -1L)
        val taskTitle = intent.getStringExtra(AlarmSchedulerImpl.EXTRA_TASK_TITLE) ?: "Task"
        val isCustom = intent.getBooleanExtra(AlarmSchedulerImpl.EXTRA_IS_CUSTOM, false)
        val timingName = intent.getStringExtra(AlarmSchedulerImpl.EXTRA_TIMING)
        val timing = runCatching { NotificationTiming.valueOf(timingName ?: "") }.getOrNull()

        val message = when {
            isCustom -> "\"$taskTitle\" üçün planlaşdırdığınız bildiriş vaxtı çatdı!"
            timing == NotificationTiming.AT_DEADLINE -> "\"$taskTitle\" tapşırığının deadline vaxtıdır!"
            else -> "\"$taskTitle\" tapşırığı ilə bağlı xatırlatma"
        }

        val slot = if (isCustom) 9 else (timing?.ordinal ?: 0)
        val notificationId = (taskId * 10 + slot).toInt()
        notificationHelper.showNotification(notificationId, "Smart Task Manager", message)
    }
}
