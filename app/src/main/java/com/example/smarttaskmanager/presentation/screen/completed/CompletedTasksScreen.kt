package com.example.smarttaskmanager.presentation.screen.completed

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.smarttaskmanager.domain.model.Task
import com.example.smarttaskmanager.presentation.viewmodel.CompletedTasksViewModel
import com.example.smarttaskmanager.util.formatFull

/**
 * Tələb #11: Tamamlanan tasklar sadə siyahı formatındadır. Üzərinə vuranda
 * ətraflı görünüş (ad, yaranma tarixi, tamamlanma tarixi) dialoq şəklində açılır.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompletedTasksScreen(
    viewModel: CompletedTasksViewModel = hiltViewModel()
) {
    val tasks by viewModel.completedTasks.collectAsState()
    var selectedTask by remember { mutableStateOf<Task?>(null) }

    Scaffold(topBar = { TopAppBar(title = { Text("Tamamlanan Tasklar") }) }) { padding ->
        if (tasks.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Hələ tamamlanan task yoxdur.")
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(vertical = 4.dp)) {
                items(tasks, key = { it.id }) { task ->
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedTask = task }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(task.title, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    task.category,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (task.createdByUid == viewModel.currentUid) {
                                IconButton(onClick = { viewModel.onDelete(task) }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Sil")
                                }
                            }
                        }
                        Divider()
                    }
                }
            }
        }
    }

    selectedTask?.let { task ->
        AlertDialog(
            onDismissRequest = { selectedTask = null },
            confirmButton = { TextButton(onClick = { selectedTask = null }) { Text("Bağla") } },
            title = { Text(task.title) },
            text = {
                Column {
                    if (task.description.isNotBlank()) {
                        Text(task.description, style = MaterialTheme.typography.bodyMedium)
                        androidx.compose.foundation.layout.Spacer(Modifier.padding(top = 8.dp))
                    }
                    Text("Kateqoriya: ${task.category}", style = MaterialTheme.typography.bodyMedium)
                    if (task.createdByName.isNotBlank()) {
                        Text("Yaradan: ${task.createdByName}", style = MaterialTheme.typography.bodyMedium)
                    }
                    task.completedByName?.let {
                        Text("Tamamlayan: $it", style = MaterialTheme.typography.bodyMedium)
                    }
                    Text("Yaradılma: ${task.createdAt.formatFull()}", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "Tamamlanma: ${task.completedAt?.formatFull() ?: "-"}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        )
    }
}
