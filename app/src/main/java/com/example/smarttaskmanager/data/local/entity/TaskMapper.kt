package com.example.smarttaskmanager.data.local.entity

import com.example.smarttaskmanager.domain.model.NotificationTiming
import com.example.smarttaskmanager.domain.model.RecurrenceType
import com.example.smarttaskmanager.domain.model.Task
import com.example.smarttaskmanager.domain.model.TaskStatus

/**
 * Data qatı (Entity) ilə Domain qatı (Task) arasında çevrilmə.
 * Clean Architecture-da bu ayrım vacibdir: domain heç vaxt Room annotasiyalarından xəbərsizdir.
 */
fun TaskEntity.toDomain(): Task = Task(
    id = id,
    title = title,
    description = description,
    date = date,
    deadline = deadline,
    priority = priority,
    category = category,
    tags = if (tags.isBlank()) emptyList() else tags.split(",").map { it.trim() },
    status = runCatching { TaskStatus.valueOf(status) }.getOrDefault(TaskStatus.PENDING),
    notificationTimings = if (notificationTimings.isBlank()) emptySet()
        else notificationTimings.split(",")
            .mapNotNull { runCatching { NotificationTiming.valueOf(it.trim()) }.getOrNull() }
            .toSet(),
    customNotificationTime = customNotificationTime,
    recurrenceType = runCatching { RecurrenceType.valueOf(recurrenceType) }.getOrDefault(RecurrenceType.NONE),
    createdAt = createdAt,
    completedAt = completedAt
)

fun Task.toEntity(): TaskEntity = TaskEntity(
    id = id,
    title = title,
    description = description,
    date = date,
    deadline = deadline,
    priority = priority,
    category = category,
    tags = tags.joinToString(","),
    status = status.name,
    notificationTimings = notificationTimings.joinToString(",") { it.name },
    customNotificationTime = customNotificationTime,
    recurrenceType = recurrenceType.name,
    createdAt = createdAt,
    completedAt = completedAt
)
