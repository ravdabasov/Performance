package com.example.smarttaskmanager.domain.model

/**
 * Tələb #9 (Filter) üçün. TODAY/TOMORROW/THIS_WEEK/THIS_MONTH/OVERDUE/COMPLETED
 * tarix əsaslıdır və client-side (Flow.map) tətbiq olunur - əlavə DB sorğusu yazmadan
 * mövcud smart-sorted axını üzərində süzülür (performans + kod təkrarının qarşısı).
 */
sealed class TaskFilter {
    data object All : TaskFilter()
    data object Today : TaskFilter()
    data object Tomorrow : TaskFilter()
    data object ThisWeek : TaskFilter()
    data object ThisMonth : TaskFilter()
    data object Overdue : TaskFilter()
    data object Completed : TaskFilter()
    data class ByPriority(val minPriority: Int) : TaskFilter()
}
