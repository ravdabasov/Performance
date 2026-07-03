package com.example.smarttaskmanager.presentation.screen.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.smarttaskmanager.domain.model.Task
import com.example.smarttaskmanager.presentation.viewmodel.ChatListViewModel
import com.example.smarttaskmanager.util.priorityColor
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Tələb: "Söhbətlərdə yazışma olmalıdı, task yox" - hər sətir bir SÖHBƏTDİR
 * (WhatsApp üslubunda): task adı + son mesajın mətni + kim yazıb + nə vaxt.
 * Mesajı olmayan tasklar "Hələ mesaj yoxdur" ilə aşağıda, mesajı olanlar
 * son yazışma vaxtına görə yuxarıda göstərilir.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    onOpenChat: (Long) -> Unit,
    viewModel: ChatListViewModel = hiltViewModel()
) {
    val conversations by viewModel.conversations.collectAsState()

    Scaffold(topBar = { TopAppBar(title = { Text("Söhbətlər") }) }) { padding ->
        if (conversations.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(
                    "Hələ heç bir task yoxdur.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(conversations, key = { it.id }) { task ->
                    ConversationRow(task = task, onClick = { onOpenChat(task.id) })
                }
            }
        }
    }
}

@Composable
private fun ConversationRow(task: Task, onClick: () -> Unit) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(priorityColor(task.priority).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Chat,
                    contentDescription = null,
                    tint = priorityColor(task.priority)
                )
            }
            Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                Text(task.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (task.lastMessageText != null) {
                    Text(
                        "${task.lastMessageAuthor}: ${task.lastMessageText}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Text(
                        "Hələ mesaj yoxdur",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            task.lastMessageAt?.let {
                Text(
                    it.toRelativeLabel(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Divider()
    }
}

private fun LocalDateTime.toRelativeLabel(): String {
    val now = LocalDateTime.now()
    val minutes = Duration.between(this, now).toMinutes()
    return when {
        minutes < 1 -> "indicə"
        minutes < 60 -> "$minutes dəq"
        minutes < 1440 -> "${minutes / 60} saat"
        toLocalDate() == now.toLocalDate().minusDays(1) -> "dünən"
        else -> format(DateTimeFormatter.ofPattern("dd.MM"))
    }
}
