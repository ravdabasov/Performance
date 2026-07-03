package com.example.smarttaskmanager.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

/**
 * Telefon yenidən başladıqda AlarmManager-dəki bütün alarmlar sistem tərəfindən silinir.
 * Bu receiver o anda WorkManager vasitəsilə bütün aktiv tasklar üçün alarmları
 * yenidən qurur - beləliklə bildirişlər restart-dan sonra da itmir (Tələb #5).
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_LOCKED_BOOT_COMPLETED
        ) {
            val request = OneTimeWorkRequestBuilder<RescheduleAllAlarmsWorker>().build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                RescheduleAllAlarmsWorker.WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }
}
