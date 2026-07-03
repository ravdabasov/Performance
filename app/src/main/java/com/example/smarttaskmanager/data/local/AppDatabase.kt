package com.example.smarttaskmanager.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.smarttaskmanager.data.local.converter.Converters
import com.example.smarttaskmanager.data.local.dao.TaskDao
import com.example.smarttaskmanager.data.local.entity.TaskEntity

/**
 * Tətbiqin tək Room database-i. Bütün data cihazda saxlanılır, internet tələb olunmur.
 * exportSchema=true - gələcəkdə migration yazmaq üçün schema JSON-ları saxlanılır.
 */
@Database(
    entities = [TaskEntity::class],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao

    companion object {
        const val DATABASE_NAME = "smart_task_manager_db"
    }
}
