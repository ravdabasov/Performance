package com.example.smarttaskmanager.domain.usecase

import com.example.smarttaskmanager.domain.model.Task
import com.example.smarttaskmanager.domain.model.TaskFilter
import com.example.smarttaskmanager.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

/**
 * Smart Prioritetləşdirmə sırasını (deadline > priority > createdAt) qoruyaraq üzərinə
 * istifadəçi filtrini tətbiq edir. Search sorğusu boş olduqda normal siyahı, doluysa
 * axtarış nəticələri göstərilir - beləliklə Axtarış (#8) və Filter (#9) birlikdə işləyir.
 */
class GetFilteredTasksUseCase @Inject constructor(
    private val repository: TaskRepository
) {
    operator fun invoke(filter: TaskFilter, searchQuery: String = ""): Flow<List<Task>> {
        val baseFlow: Flow<List<Task>> = when {
            searchQuery.isNotBlank() -> repository.searchTasks(searchQuery)
            filter is TaskFilter.Completed -> repository.getCompletedTasks()
            else -> repository.getActiveTasksSmartSorted()
        }
        return baseFlow.combine(kotlinx.coroutines.flow.flowOf(filter)) { tasks, f ->
            applyFilter(tasks, f)
        }
    }

    private fun applyFilter(tasks: List<Task>, filter: TaskFilter): List<Task> {
        val today = LocalDate.now()
        return when (filter) {
            is TaskFilter.All -> tasks
            is TaskFilter.Today -> tasks.filter { it.deadline.toLocalDate() == today }
            is TaskFilter.Tomorrow -> tasks.filter { it.deadline.toLocalDate() == today.plusDays(1) }
            is TaskFilter.ThisWeek -> {
                val endOfWeek = today.with(TemporalAdjusters.nextOrSame(java.time.DayOfWeek.SUNDAY))
                tasks.filter { it.deadline.toLocalDate() in today..endOfWeek }
            }
            is TaskFilter.ThisMonth -> tasks.filter {
                it.deadline.month == today.month && it.deadline.year == today.year
            }
            is TaskFilter.Overdue -> tasks.filter { it.isOverdue }
            is TaskFilter.Completed -> tasks // artıq getCompletedTasks-dan gəlir
            is TaskFilter.ByPriority -> tasks.filter { it.priority >= filter.minPriority }
        }
    }
}
