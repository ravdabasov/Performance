package com.example.smarttaskmanager.presentation.screen.tasklist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.smarttaskmanager.domain.model.TaskEisenhowerGroup
import com.example.smarttaskmanager.presentation.components.TaskCard
import com.example.smarttaskmanager.presentation.components.TaskSearchBar
import com.example.smarttaskmanager.presentation.viewmodel.TaskListViewModel

/**
 * Tələb: Tasklar 4 qrupda (Eisenhower matrisi) sadə başlıq formatında göstərilir,
 * hər qrup daxilində sıra nömrəli, prioritet+deadline-a görə sıralanmış siyahı.
 * Checkbox iki istiqamətlidir, silmə düyməsi işləkdir. TAMAMLANAN/LƏĞV EDİLƏN
 * tasklar da daxil olmaqla HƏQİQƏTƏN bütün tasklar göstərilir.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(
    onAddTask: () -> Unit,
    onTaskClick: (Long) -> Unit,
    viewModel: TaskListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val hasAnyTask = uiState.groupedTasks.values.any { it.isNotEmpty() }

    Scaffold(topBar = { TopAppBar(title = { Text("Bütün Tasklar") }) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TaskSearchBar(
                query = uiState.searchQuery,
                onQueryChange = viewModel::onSearchQueryChanged
            )

            when {
                uiState.isLoading -> androidx.compose.foundation.layout.Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }

                !hasAnyTask -> androidx.compose.foundation.layout.Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) { Text("Uyğun task tapılmadı.") }

                else -> LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp, ),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TaskEisenhowerGroup.entries.forEach { group ->
                        val tasksInGroup = uiState.groupedTasks[group].orEmpty()
                        item(key = "header_${group.name}") {
                            GroupHeader(group = group, count = tasksInGroup.size)
                        }
                        if (tasksInGroup.isEmpty()) {
                            item(key = "empty_${group.name}") {
                                Text(
                                    "Bu qrupda task yoxdur",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                            }
                        } else {
                            itemsIndexed(tasksInGroup, key = { _, task -> task.id }) { index, task ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "${index + 1}",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.width(24.dp)
                                    )
                                    TaskCard(
                                        task = task,
                                        onClick = { onTaskClick(task.id) },
                                        onToggleComplete = { viewModel.onToggleComplete(task) },
                                        onDelete = { viewModel.onDelete(task) },
                                        modifier = Modifier.weight(1f),
                                        isOwner = task.createdByUid == uiState.currentUid
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupHeader(group: TaskEisenhowerGroup, count: Int) {
    Column(modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)) {
        Text(
            "${group.titleAz} ($count)",
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            group.subtitleAz,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
