package com.example.smarttaskmanager.domain.model

import java.time.LocalDateTime

/**
 * Tətbiqin bütün qatlarında (UI, ViewModel, UseCase) istifadə olunan təmiz domain modeli.
 * Room Entity-dən asılı deyil ki, Clean Architecture qaydasına (data qatı domain-dən asılıdır,
 * əksinə yox) əməl olunsun.
 */
data class Task(
    val id: Long = 0L,
    val title: String,
    val description: String = "",
    val date: LocalDateTime,          // Tarix + saat
    val deadline: LocalDateTime,      // Ayrıca deadline
    val priority: Int,                // 1..10
    val category: String = "Ümumi",
    val tags: List<String> = emptyList(),
    val status: TaskStatus = TaskStatus.PENDING,
    val notificationTimings: Set<NotificationTiming> = emptySet(),
    val customNotificationTime: LocalDateTime? = null, // istifadəçinin seçdiyi xüsusi bildiriş vaxtı
    val recurrenceType: RecurrenceType = RecurrenceType.NONE, // gündəlik/həftəlik/aylıq təkrarlanma
    val createdByUid: String = "",
    val createdByName: String = "",
    val completedByName: String? = null,
    val lastMessageText: String? = null,
    val lastMessageAuthor: String? = null,
    val lastMessageAt: LocalDateTime? = null,
    val assigneeUid: String? = null,
    val assigneeName: String? = null,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val completedAt: LocalDateTime? = null
) {
    val isOverdue: Boolean
        get() = status != TaskStatus.COMPLETED &&
                status != TaskStatus.CANCELLED &&
                LocalDateTime.now().isAfter(deadline)

    /**
     * Effektiv status: DB-də saxlanılan status ilə real-time overdue vəziyyətini
     * uzlaşdırır. UI həmişə bunu göstərməlidir.
     */
    val effectiveStatus: TaskStatus
        get() = if (isOverdue) TaskStatus.OVERDUE else status
}
