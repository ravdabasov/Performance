package com.example.smarttaskmanager.presentation.screen.statistics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.smarttaskmanager.domain.model.TaskEisenhowerGroup
import com.example.smarttaskmanager.presentation.components.StatCard
import com.example.smarttaskmanager.presentation.theme.StatusActive
import com.example.smarttaskmanager.presentation.theme.StatusCompleted
import com.example.smarttaskmanager.presentation.viewmodel.GroupStat
import com.example.smarttaskmanager.presentation.viewmodel.StatisticsViewModel
import com.example.smarttaskmanager.presentation.viewmodel.UserStat
import com.example.smarttaskmanager.util.matrixColor
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Tələb: Statistika bölməsi. Tarix aralığı seçilir, seçilən aralıqda ümumi/tamamlanan/
 * aktiv say + faizlər, və 4 Eisenhower qrupu üzrə icra faizləri göstərilir.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    viewModel: StatisticsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    Scaffold(topBar = { TopAppBar(title = { Text("Statistika") }) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Başlanğıc", style = MaterialTheme.typography.labelSmall)
                            TextButton(onClick = { showStartPicker = true }) { Text(state.startDate.formatAsDate()) }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Son", style = MaterialTheme.typography.labelSmall)
                            TextButton(onClick = { showEndPicker = true }) { Text(state.endDate.formatAsDate()) }
                        }
                    }
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    StatCard(
                        title = "Ümumi",
                        value = state.totalCount.toString(),
                        accentColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Tamamlanan · ${state.percentOf(state.completedCount)}%",
                        value = state.completedCount.toString(),
                        accentColor = StatusCompleted,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Aktiv · ${state.percentOf(state.activeCount)}%",
                        value = state.activeCount.toString(),
                        accentColor = StatusActive,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Text("Qruplar üzrə icra", style = MaterialTheme.typography.titleMedium)
            }

            items(state.groupStats) { stat ->
                GroupStatRow(stat)
            }

            item {
                Text(
                    "Komanda üzvləri üzrə",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (state.userStats.isEmpty()) {
                item {
                    Text(
                        "Bu aralıqda heç bir üzv task icra etməyib",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(state.userStats) { userStat ->
                    UserStatRow(userStat)
                }
            }
        }
    }

    if (showStartPicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = state.startDate.toEpochMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showStartPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { viewModel.onStartDateChanged(it.toLocalDate()) }
                    showStartPicker = false
                }) { Text("Təsdiqlə") }
            },
            dismissButton = { TextButton(onClick = { showStartPicker = false }) { Text("Ləğv et") } }
        ) { DatePicker(state = pickerState) }
    }

    if (showEndPicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = state.endDate.toEpochMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showEndPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { viewModel.onEndDateChanged(it.toLocalDate()) }
                    showEndPicker = false
                }) { Text("Təsdiqlə") }
            },
            dismissButton = { TextButton(onClick = { showEndPicker = false }) { Text("Ləğv et") } }
        ) { DatePicker(state = pickerState) }
    }
}

@Composable
private fun GroupStatRow(stat: GroupStat) {
    val color = matrixColor(stat.group)
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stat.group.titleAz, style = MaterialTheme.typography.bodyLarge)
            Text(
                "${stat.done}/${stat.total} · ${stat.donePercent}%",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        LinearProgressIndicator(
            progress = { if (stat.total == 0) 0f else stat.donePercent / 100f },
            color = color,
            modifier = Modifier.fillMaxWidth().height(6.dp).padding(top = 4.dp)
        )
    }
}

@Composable
private fun UserStatRow(stat: UserStat) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(stat.name, style = MaterialTheme.typography.bodyLarge)
        Text(
            "${stat.completedCount} task icra edib",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun LocalDate.formatAsDate(): String = format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy"))

private fun LocalDate.toEpochMillis(): Long =
    atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()

private fun Long.toLocalDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneId.of("UTC")).toLocalDate()
