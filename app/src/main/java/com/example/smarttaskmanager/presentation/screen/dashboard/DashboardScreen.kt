package com.example.smarttaskmanager.presentation.screen.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.smarttaskmanager.domain.model.Task
import com.example.smarttaskmanager.domain.model.TaskEisenhowerGroup
import com.example.smarttaskmanager.presentation.components.StatCard
import com.example.smarttaskmanager.presentation.theme.StatusActive
import com.example.smarttaskmanager.presentation.theme.StatusCompleted
import com.example.smarttaskmanager.presentation.theme.StatusOverdue
import com.example.smarttaskmanager.presentation.theme.StatusToday
import com.example.smarttaskmanager.presentation.viewmodel.DashboardViewModel
import com.example.smarttaskmanager.presentation.viewmodel.AuthViewModel
import com.example.smarttaskmanager.util.matrixColor

/**
 * Tələb #10: Dashboard. Statistika kartları + 2x2 prioritetləşmə matrisi (Eisenhower).
 * Tasklar burada AYRICA siyahı kimi göstərilmir - hər task avtomatik uyğun matris
 * qutusuna düşür, ətraflı baxmaq üçün "Tasklar" tabına keçmək lazımdır.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToTaskList: () -> Unit,
    onAddTask: () -> Unit,
    onTaskClick: (Long) -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val authViewModel: AuthViewModel = hiltViewModel()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PSD / Performance Management Team") },
                actions = {
                    IconButton(onClick = { authViewModel.signOut() }) {
                        Icon(Icons.Filled.Logout, contentDescription = "Çıxış")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        StatCard(
                            title = "Bu gün",
                            value = uiState.stats.todayCount.toString(),
                            accentColor = StatusToday,
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = "Gecikmiş",
                            value = uiState.stats.overdueCount.toString(),
                            accentColor = StatusOverdue,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        StatCard(
                            title = "Aktiv",
                            value = uiState.stats.activeCount.toString(),
                            accentColor = StatusActive,
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = "Tamamlanan",
                            value = uiState.stats.completedCount.toString(),
                            accentColor = StatusCompleted,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            item {
                Column {
                    Text(
                        "Məhsuldarlıq: ${uiState.stats.completionPercentage}%",
                        style = MaterialTheme.typography.titleMedium
                    )
                    LinearProgressIndicator(
                        progress = { uiState.stats.completionPercentage / 100f },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    )
                }
            }

            item {
                OutlinedButton(onClick = onNavigateToTaskList, modifier = Modifier.fillMaxWidth()) {
                    Text("Bütün taskları göstər")
                }
            }

            item {
                Text("İşlərin prioritetləşməsi", style = MaterialTheme.typography.titleMedium)
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        MatrixQuadrant(
                            group = TaskEisenhowerGroup.URGENT_IMPORTANT,
                            tasks = uiState.matrixGroups[TaskEisenhowerGroup.URGENT_IMPORTANT].orEmpty(),
                            onTaskClick = onTaskClick,
                            modifier = Modifier.weight(1f)
                        )
                        MatrixQuadrant(
                            group = TaskEisenhowerGroup.IMPORTANT_NOT_URGENT,
                            tasks = uiState.matrixGroups[TaskEisenhowerGroup.IMPORTANT_NOT_URGENT].orEmpty(),
                            onTaskClick = onTaskClick,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        MatrixQuadrant(
                            group = TaskEisenhowerGroup.URGENT_NOT_IMPORTANT,
                            tasks = uiState.matrixGroups[TaskEisenhowerGroup.URGENT_NOT_IMPORTANT].orEmpty(),
                            onTaskClick = onTaskClick,
                            modifier = Modifier.weight(1f)
                        )
                        MatrixQuadrant(
                            group = TaskEisenhowerGroup.NEITHER,
                            tasks = uiState.matrixGroups[TaskEisenhowerGroup.NEITHER].orEmpty(),
                            onTaskClick = onTaskClick,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MatrixQuadrant(
    group: TaskEisenhowerGroup,
    tasks: List<Task>,
    onTaskClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val color = matrixColor(group)
    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .background(color.copy(alpha = 0.16f))
            .padding(10.dp)
            .height(140.dp)
    ) {
        Text(group.titleAz, style = MaterialTheme.typography.labelLarge, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(group.subtitleAz, style = MaterialTheme.typography.labelSmall, color = color)
        androidx.compose.foundation.layout.Spacer(Modifier.height(6.dp))
        if (tasks.isEmpty()) {
            Text("Task yoxdur", style = MaterialTheme.typography.labelSmall, color = color)
        } else {
            tasks.take(3).forEach { task ->
                Text(
                    "• ${task.title}",
                    style = MaterialTheme.typography.labelSmall,
                    color = color,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 1.dp)
                        .then(Modifier.clickable { onTaskClick(task.id) })
                )
            }
            if (tasks.size > 3) {
                Text("+${tasks.size - 3} daha", style = MaterialTheme.typography.labelSmall, color = color)
            }
        }
    }
}
