package com.example.smarttaskmanager.domain.model

import java.time.Duration
import java.time.LocalDateTime

/**
 * Tələb: Tasklar hissəsində 4 qrup - Eisenhower matrisinə əsaslanır.
 * Təcililik (urgency) = deadline 24 saatdan az qalıb (və ya artıq gecikib).
 * Vaciblik (importance) = prioritet balı 6 və ya yuxarı.
 */
enum class TaskEisenhowerGroup(val titleAz: String, val subtitleAz: String) {
    URGENT_IMPORTANT("Təcili və Vacib", "Dərhal həll edin"),
    IMPORTANT_NOT_URGENT("Vacib", "Planlaşdırın"),
    URGENT_NOT_IMPORTANT("Təcili", "Tez bitirin"),
    NEITHER("Vacib və Təcili Olmayan", "Sonraya saxlaya bilərsiniz")
}

fun Task.eisenhowerGroup(): TaskEisenhowerGroup {
    val hoursUntilDeadline = Duration.between(LocalDateTime.now(), deadline).toHours()
    val urgent = isOverdue || hoursUntilDeadline <= 24
    val important = priority >= 6
    return when {
        urgent && important -> TaskEisenhowerGroup.URGENT_IMPORTANT
        important -> TaskEisenhowerGroup.IMPORTANT_NOT_URGENT
        urgent -> TaskEisenhowerGroup.URGENT_NOT_IMPORTANT
        else -> TaskEisenhowerGroup.NEITHER
    }
}

/** Bütün aktiv taskları 4 sabit qrupa bölür, qrup daxilində smart-sort sırası qorunur. */
fun List<Task>.groupByEisenhower(): Map<TaskEisenhowerGroup, List<Task>> {
    val grouped = groupBy { it.eisenhowerGroup() }
    return TaskEisenhowerGroup.entries.associateWith { grouped[it] ?: emptyList() }
}
