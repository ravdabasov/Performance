package com.example.smarttaskmanager.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smarttaskmanager.domain.model.Task
import com.example.smarttaskmanager.domain.model.TaskEisenhowerGroup
import com.example.smarttaskmanager.domain.model.groupByEisenhower
import com.example.smarttaskmanager.domain.repository.TaskRepository
import com.example.smarttaskmanager.domain.usecase.DashboardStats
import com.example.smarttaskmanager.domain.usecase.GetDashboardStatsUseCase
import com.example.smarttaskmanager.domain.usecase.RefreshOverdueStatusesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val stats: DashboardStats = DashboardStats(),
    val matrixGroups: Map<TaskEisenhowerGroup, List<Task>> = emptyMap()
)

/**
 * Ana səhifə: statistika kartları + prioritetləşmə matrisi (2x2, Eisenhower).
 * Matris yalnız AKTİV (hələ tamamlanmamış) taskları göstərir - "nə etməli" sualının
 * cavabı olduğu üçün tamamlanan/ləğv edilən tasklar burada yer almır.
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    getDashboardStats: GetDashboardStatsUseCase,
    repository: TaskRepository,
    private val refreshOverdueStatuses: RefreshOverdueStatusesUseCase
) : ViewModel() {

    init {
        viewModelScope.launch { refreshOverdueStatuses() }
    }

    val uiState: StateFlow<DashboardUiState> = combine(
        getDashboardStats(),
        repository.getActiveTasksSmartSorted().map { it.groupByEisenhower() }
    ) { stats, matrix ->
        DashboardUiState(stats = stats, matrixGroups = matrix)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState()
    )
}
