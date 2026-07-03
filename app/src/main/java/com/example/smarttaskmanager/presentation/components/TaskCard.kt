package com.example.smarttaskmanager.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.smarttaskmanager.domain.model.Task
import com.example.smarttaskmanager.domain.model.TaskStatus
import com.example.smarttaskmanager.util.formatFull
import com.example.smarttaskmanager.util.priorityColor
import com.example.smarttaskmanager.util.statusColor
import com.example.smarttaskmanager.util.timeRemainingLabel

/**
 * Task siyahısı boyunca istifadə olunan universal kart komponenti.
 * Rəngli sol zolaq task-ın prioritet balına görə avtomatik hesablanan rəngini, sağ tərəfdəki nöqtə isə
 * prioritet balını göstərir - Smart Prioritetləşdirməni vizual olaraq gücləndirir.
 */
@Composable
fun TaskCard(
    task: Task,
    onClick: () -> Unit,
    onToggleComplete: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    isOwner: Boolean = true
) {
    val isCompleted = task.effectiveStatus == TaskStatus.COMPLETED
    // Hər kəs task-ı tamamlaya bilər, amma yalnız yaradan geri qaytara/silə bilər.
    val canToggle = !isCompleted || isOwner
    val canDelete = isOwner

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // Sol rəng zolağı - task-ın prioritet balına görə avtomatik rəng
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .height(96.dp)
                    .background(priorityColor(task.priority))
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = isCompleted,
                    onCheckedChange = { onToggleComplete() },
                    enabled = canToggle
                )

                Column(modifier = Modifier.weight(1f).padding(horizontal = 4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = task.title,
                            style = MaterialTheme.typography.titleMedium,
                            textDecoration = if (isCompleted) TextDecoration.LineThrough else null,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (task.recurrenceType != com.example.smarttaskmanager.domain.model.RecurrenceType.NONE) {
                            Icon(
                                Icons.Filled.Repeat,
                                contentDescription = task.recurrenceType.displayNameAz,
                                modifier = Modifier.size(14.dp).padding(start = 4.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "${task.category} • ${task.deadline.formatFull()}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (task.createdByName.isNotBlank()) {
                        Text(
                            text = "Yaradan: ${task.createdByName}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    task.assigneeName?.let { assignee ->
                        Text(
                            text = "Cavabdeh: $assignee",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StatusChip(status = task.effectiveStatus)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = task.timeRemainingLabel(),
                            style = MaterialTheme.typography.labelSmall,
                            color = statusColor(task.effectiveStatus)
                        )
                    }
                }

                PriorityDot(priority = task.priority)

                if (canDelete) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Filled.Delete, contentDescription = "Sil")
                    }
                }
            }
        }
    }
}

@Composable
fun PriorityDot(priority: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(priorityColor(priority)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = priority.toString(),
            color = androidx.compose.ui.graphics.Color.White,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
fun StatusChip(status: TaskStatus, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(statusColor(status).copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = status.displayNameAz,
            style = MaterialTheme.typography.labelSmall,
            color = statusColor(status)
        )
    }
}
