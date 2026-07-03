package com.example.smarttaskmanager.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.smarttaskmanager.domain.model.NotificationTiming
import com.example.smarttaskmanager.domain.model.Task
import com.example.smarttaskmanager.domain.repository.TaskNotificationScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AlarmManager ilə "dəqiq vaxtda" (exact) bildiriş planlaşdırması.
 * Hər task üçün seçilən hər NotificationTiming ayrıca alarm kimi qeydə alınır,
 * beləliklə "Deadline-dan əvvəl / zamanı / sonra" eyni anda seçilə bilər (#5).
 * Əlavə olaraq, istifadəçinin seçdiyi XÜSUSİ bildiriş tarixi/saatı da ayrıca planlaşdırılır.
 *
 * requestCode = taskId * 10 + slot → hər (task, bildiriş növü) cütü üçün unikaldır.
 * slot 0,1,2 -> NotificationTiming.ordinal; slot 9 -> xüsusi (custom) bildiriş.
 */
@Singleton
class AlarmSchedulerImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : TaskNotificationScheduler {

    private val alarmManager: AlarmManager by lazy {
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    }

    override fun scheduleForTask(task: Task) {
        cancelForTask(task.id) // köhnə alarmları təmizlə, təkrarların qarşısını al

        task.notificationTimings.forEach { timing ->
            val triggerTime = task.deadline
                .plusMinutes(timing.offsetMinutes)
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
            scheduleAlarm(
                requestCode = requestCodeFor(task.id, timing.ordinal),
                triggerTime = triggerTime,
                taskId = task.id,
                taskTitle = task.title,
                timingName = timing.name,
                isCustom = false
            )
        }

        task.customNotificationTime?.let { customTime ->
            val triggerTime = customTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            scheduleAlarm(
                requestCode = requestCodeFor(task.id, CUSTOM_SLOT),
                triggerTime = triggerTime,
                taskId = task.id,
                taskTitle = task.title,
                timingName = null,
                isCustom = true
            )
        }
    }

    private fun scheduleAlarm(
        requestCode: Int,
        triggerTime: Long,
        taskId: Long,
        taskTitle: String,
        timingName: String?,
        isCustom: Boolean
    ) {
        if (triggerTime <= System.currentTimeMillis()) return // keçmiş vaxt üçün alarm qurma

        val intent = Intent(context, TaskAlarmReceiver::class.java).apply {
            putExtra(EXTRA_TASK_ID, taskId)
            putExtra(EXTRA_TASK_TITLE, taskTitle)
            timingName?.let { putExtra(EXTRA_TIMING, it) }
            putExtra(EXTRA_IS_CUSTOM, isCustom)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                } else {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }
        } catch (_: SecurityException) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        }
    }

    override fun cancelForTask(taskId: Long) {
        val allSlots = NotificationTiming.entries.map { it.ordinal } + CUSTOM_SLOT
        allSlots.forEach { slot ->
            val requestCode = requestCodeFor(taskId, slot)
            val intent = Intent(context, TaskAlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        }
    }

    override fun rescheduleAll(tasks: List<Task>) {
        tasks.forEach { task ->
            if (task.notificationTimings.isNotEmpty() || task.customNotificationTime != null) {
                scheduleForTask(task)
            }
        }
    }

    private fun requestCodeFor(taskId: Long, slot: Int): Int = (taskId * 10 + slot).toInt()

    companion object {
        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_TASK_TITLE = "extra_task_title"
        const val EXTRA_TIMING = "extra_timing"
        const val EXTRA_IS_CUSTOM = "extra_is_custom"
        private const val CUSTOM_SLOT = 9
    }
}
