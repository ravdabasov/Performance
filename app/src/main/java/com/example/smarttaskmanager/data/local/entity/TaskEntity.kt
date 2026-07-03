package com.example.smarttaskmanager.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDateTime

/**
 * Room database cədvəlinin sətri. Yalnız data qatında istifadə olunur;
 * domain/UI qatları bu sinifi birbaşa görmür (mapper vasitəsilə Task-a çevrilir).
 *
 * Deadline üzrə index qoyulub ki, "Smart Prioritetləşdirmə" sorğusu (ORDER BY deadline)
 * çoxlu task olduqda da sürətli işləsin.
 */
@Entity(
    tableName = "tasks",
    indices = [Index(value = ["deadline"]), Index(value = ["status"]), Index(value = ["category"])]
)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    val description: String,
    val date: LocalDateTime,
    val deadline: LocalDateTime,
    val priority: Int,
    val category: String,
    val tags: String,              // vergüllə ayrılmış tag-lar (TypeConverter yox, sadə saxlama)
    val status: String,            // TaskStatus.name
    val notificationTimings: String, // vergüllə ayrılmış NotificationTiming.name-lər
    val customNotificationTime: LocalDateTime?, // istifadəçinin seçdiyi xüsusi bildiriş vaxtı
    val recurrenceType: String, // RecurrenceType.name
    val createdAt: LocalDateTime,
    val completedAt: LocalDateTime?
)
