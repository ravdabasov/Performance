package com.example.smarttaskmanager.util

import androidx.compose.ui.graphics.Color
import com.example.smarttaskmanager.domain.model.Task
import com.example.smarttaskmanager.domain.model.TaskEisenhowerGroup
import com.example.smarttaskmanager.domain.model.TaskStatus
import com.example.smarttaskmanager.presentation.theme.PriorityHigh
import com.example.smarttaskmanager.presentation.theme.PriorityLow
import com.example.smarttaskmanager.presentation.theme.PriorityMedium
import com.example.smarttaskmanager.presentation.theme.StatusCancelled
import com.example.smarttaskmanager.presentation.theme.StatusCompleted
import com.example.smarttaskmanager.presentation.theme.StatusInProgress
import com.example.smarttaskmanager.presentation.theme.StatusOverdue
import com.example.smarttaskmanager.presentation.theme.StatusPending
import com.example.smarttaskmanager.presentation.theme.StatusToday
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val dateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

fun LocalDateTime.formatFull(): String = format(dateTimeFormatter)
fun LocalDateTime.formatTime(): String = format(timeFormatter)
fun LocalDateTime.formatDate(): String = format(dateFormatter)

/**
 * Prioritet balına (1-10) uyğun rəng. Tələb: task-lar üçün ayrıca rəng seçimi yoxdur,
 * task-ın rəngi HƏMİŞƏ cari prioritet balına görə avtomatik hesablanır.
 */
fun priorityColor(priority: Int): Color = when {
    priority >= 8 -> PriorityHigh
    priority >= 4 -> PriorityMedium
    else -> PriorityLow
}

fun statusColor(status: TaskStatus): Color = when (status) {
    TaskStatus.OVERDUE -> StatusOverdue
    TaskStatus.COMPLETED -> StatusCompleted
    TaskStatus.IN_PROGRESS -> StatusInProgress
    TaskStatus.PENDING -> StatusPending
    TaskStatus.CANCELLED -> StatusCancelled
}

/** Ana səhifədəki 2x2 prioritetləşmə matrisinin hər qutusu üçün rəng. */
fun matrixColor(group: TaskEisenhowerGroup): Color = when (group) {
    TaskEisenhowerGroup.URGENT_IMPORTANT -> StatusOverdue      // Dərhal et - qırmızı
    TaskEisenhowerGroup.IMPORTANT_NOT_URGENT -> StatusToday    // Planlaşdır - firuzəyi
    TaskEisenhowerGroup.URGENT_NOT_IMPORTANT -> PriorityMedium // Həvalə et - kəhrəba
    TaskEisenhowerGroup.NEITHER -> StatusCompleted             // Təxirə sal - yaşıl
}

/** Deadline-a nə qədər qaldığını insan dilində göstərir (Dashboard və list üçün). */
fun Task.timeRemainingLabel(): String {
    val now = LocalDateTime.now()
    if (effectiveStatus == TaskStatus.OVERDUE) return "Vaxtı keçib"
    val minutes = java.time.Duration.between(now, deadline).toMinutes()
    return when {
        minutes < 60 -> "$minutes dəq qalıb"
        minutes < 1440 -> "${minutes / 60} saat qalıb"
        else -> "${minutes / 1440} gün qalıb"
    }
}
