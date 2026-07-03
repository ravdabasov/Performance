package com.example.smarttaskmanager.domain.usecase

import com.example.smarttaskmanager.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

/** Dashboard ekranında göstərilən bütün statistik məlumatlar (Tələb #10). */
data class DashboardStats(
    val todayCount: Int = 0,
    val overdueCount: Int = 0,
    val completedCount: Int = 0,
    val activeCount: Int = 0,
    val totalCount: Int = 0
) {
    val completionPercentage: Int
        get() = if (totalCount == 0) 0 else ((completedCount * 100f) / totalCount).toInt()
}

class GetDashboardStatsUseCase @Inject constructor(
    private val repository: TaskRepository
) {
    operator fun invoke(): Flow<DashboardStats> = combine(
        repository.getTodayTaskCount(),
        repository.getOverdueCount(),
        repository.getCompletedCount(),
        repository.getActiveCount(),
        repository.getTotalCount()
    ) { today, overdue, completed, active, total ->
        DashboardStats(today, overdue, completed, active, total)
    }
}
