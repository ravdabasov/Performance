package com.example.smarttaskmanager.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smarttaskmanager.domain.model.Task
import com.example.smarttaskmanager.domain.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Tələb: "Söhbətlərdə yazışma olmalıdı, task yox" - hər task bir "söhbət" kimi
 * göstərilir, amma WhatsApp/Messenger məntiqi ilə: SON MESAJI olan söhbətlər
 * yuxarıda, hər sətirdə son mesaj mətni + kim yazıb + nə vaxt görünür.
 */
@HiltViewModel
class ChatListViewModel @Inject constructor(
    repository: TaskRepository
) : ViewModel() {

    val conversations: StateFlow<List<Task>> = repository.getAllTasksSmartSorted()
        .map { tasks ->
            tasks.filter { it.lastMessageText != null }
                .sortedByDescending { it.lastMessageAt }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
