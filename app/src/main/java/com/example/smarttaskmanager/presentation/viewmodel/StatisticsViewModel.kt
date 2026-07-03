package com.example.smarttaskmanager.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smarttaskmanager.domain.model.Task
import com.example.smarttaskmanager.domain.model.TaskEisenhowerGroup
import com.example.smarttaskmanager.domain.model.TaskStatus
import com.example.smarttaskmanager.domain.model.groupByEisenhower
import com.example.smarttaskmanager.domain.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

data class GroupStat(val group: TaskEisenhowerGroup, val total: Int, val done: Int) {
    val notDone: Int get() = total - done
    val donePercent: Int get() = if (total == 0) 0 else (done * 100) / total
}

data class UserStat(val name: String, val completedCount: Int)

data class StatisticsUiState(
    val startDate: LocalDate = LocalDate.now().minusDays(30),
    val endDate: LocalDate = LocalDate.now(),
    val totalCount: Int = 0,
    val completedCount: Int = 0,
    val activeCount: Int = 0,
    val groupStats: List<GroupStat> = emptyList(),
    val userStats: List<UserStat> = emptyList()
) {
    fun percentOf(count: Int): Int = if (totalCount == 0) 0 else (count * 100) / totalCount
}

/**
 * Tələb: Statistika bölməsi - tarix aralığı seçilir, seçilən aralıqda deadline-ı olan
 * tasklar üzərindən ümumi/tamamlanan/aktiv say və hər Eisenhower qrupu üzrə icra faizi
 * hesablanır.
 */
@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val repository: TaskRepository
) : ViewModel() {

    private val startDate = MutableStateFlow(LocalDate.now().minusDays(30))
    private val endDate = MutableStateFlow(LocalDate.now())

    val uiState: StateFlow<StatisticsUiState> = combine(startDate, endDate) { s, e -> s to e }
        .flatMapLatest { (s, e) ->
            repository.getTasksInDateRange(s.atStartOfDay(), e.atTime(LocalTime.of(23, 59, 59)))
                .map { tasks -> buildState(s, e, tasks) }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = StatisticsUiState()
        )

    fun onStartDateChanged(date: LocalDate) {
        startDate.value = date
    }

    fun onEndDateChanged(date: LocalDate) {
        endDate.value = date
    }

    private fun buildState(start: LocalDate, end: LocalDate, tasks: List<Task>): StatisticsUiState {
        val completed = tasks.count { it.status == TaskStatus.COMPLETED }
        val active = tasks.count { it.status != TaskStatus.COMPLETED && it.status != TaskStatus.CANCELLED }
        val grouped = tasks.groupByEisenhower()
        val groupStats = TaskEisenhowerGroup.entries.map { group ->
            val groupTasks = grouped[group].orEmpty()
            GroupStat(
                group = group,
                total = groupTasks.size,
                done = groupTasks.count { it.status == TaskStatus.COMPLETED }
            )
        }
        val userStats = tasks
            .filter { it.status == TaskStatus.COMPLETED && !it.completedByName.isNullOrBlank() }
            .groupBy { it.completedByName!! }
            .map { (name, doneTasks) -> UserStat(name = name, completedCount = doneTasks.size) }
            .sortedByDescending { it.completedCount }
        return StatisticsUiState(
            startDate = start,
            endDate = end,
            totalCount = tasks.size,
            completedCount = completed,
            activeCount = active,
            groupStats = groupStats,
            userStats = userStats
        )
    }
}
