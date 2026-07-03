package com.example.smarttaskmanager

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Tətbiqin Application sinifi. Hilt-in bütün asılılıq qrafı burada başlayır.
 * Configuration.Provider - WorkManager-in Hilt-injected worker-ləri (RescheduleAllAlarmsWorker)
 * yarada bilməsi üçün lazımdır.
 */
@HiltAndroidApp
class SmartTaskManagerApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
