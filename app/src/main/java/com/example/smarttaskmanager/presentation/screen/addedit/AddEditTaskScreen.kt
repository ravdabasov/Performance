package com.example.smarttaskmanager.presentation.screen.addedit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.matchParentSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.smarttaskmanager.domain.model.NotificationTiming
import com.example.smarttaskmanager.domain.model.RecurrenceType
import com.example.smarttaskmanager.presentation.viewmodel.AddEditTaskViewModel
import com.example.smarttaskmanager.util.formatDate
import com.example.smarttaskmanager.util.formatTime
import com.example.smarttaskmanager.util.priorityColor
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * Tələb #1 (Task yaratmaq) + #2 (Deadline) + #3 (Prioritet) + #5 (Bildiriş seçimi)-i
 * tək formada birləşdirir. Kateqoriya sahəsi YOXDUR - avtomatik prioritet+deadline-a
 * görə hesablanır və istifadəçiyə məlumat kimi göstərilir.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTaskScreen(
    onDone: () -> Unit,
    onOpenChat: (Long) -> Unit,
    viewModel: AddEditTaskViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val users by viewModel.users.collectAsState()

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) onDone()
    }

    var showDatePickerFor by remember { mutableStateOf<PickerTarget?>(null) }
    var showTimePickerFor by remember { mutableStateOf<PickerTarget?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isEditMode) "Task-ı redaktə et" else "Yeni Task") },
                actions = {
                    if (state.isEditMode) {
                        IconButton(onClick = { onOpenChat(state.id) }) {
                            Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Müzakirə")
                        }
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
                OutlinedTextField(
                    value = state.title,
                    onValueChange = viewModel::onTitleChanged,
                    label = { Text("Task adı *") },
                    isError = state.errorMessage != null,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                OutlinedTextField(
                    value = state.description,
                    onValueChange = viewModel::onDescriptionChanged,
                    label = { Text("Ətraflı qeyd (opsional)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
            }

            item {
                DateTimeRow(
                    label = "Tarix / Saat",
                    dateTime = state.date,
                    onDateClick = { showDatePickerFor = PickerTarget.DATE },
                    onTimeClick = { showTimePickerFor = PickerTarget.DATE }
                )
            }
            item {
                DateTimeRow(
                    label = "Deadline",
                    dateTime = state.deadline,
                    onDateClick = { showDatePickerFor = PickerTarget.DEADLINE },
                    onTimeClick = { showTimePickerFor = PickerTarget.DEADLINE }
                )
            }

            item {
                Column {
                    Text("Prioritet: ${state.priority}/10", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Task-ın rəngi prioritetə görə avtomatik təyin olunur",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Slider(
                        value = state.priority.toFloat(),
                        onValueChange = { viewModel.onPriorityChanged(it.toInt()) },
                        valueRange = 1f..10f,
                        steps = 8,
                        colors = androidx.compose.material3.SliderDefaults.colors(
                            thumbColor = priorityColor(state.priority),
                            activeTrackColor = priorityColor(state.priority)
                        )
                    )
                }
            }

            item {
                // Kateqoriya artıq əl ilə yazılmır - prioritet+deadline-a görə avtomatik hesablanır
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Kateqoriya (avtomatik)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(state.computedCategory, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = state.tagsInput,
                    onValueChange = viewModel::onTagsChanged,
                    label = { Text("Tag-lar (vergüllə ayırın)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                AssigneeSelector(
                    users = users,
                    selectedName = state.assigneeName,
                    onUserSelected = viewModel::onAssigneeChanged
                )
            }

            item {
                Column {
                    Text("Təkrarlanma", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Task tamamlandıqda seçilən dövr üçün avtomatik yeni task yaradılır",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RecurrenceType.entries.forEach { recurrence ->
                            FilterChip(
                                selected = state.recurrenceType == recurrence,
                                onClick = { viewModel.onRecurrenceChanged(recurrence) },
                                label = { Text(recurrence.displayNameAz) }
                            )
                        }
                    }
                }
            }

            item {
                Column {
                    Text("Bildiriş vaxtı", style = MaterialTheme.typography.titleMedium)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        NotificationTiming.entries.forEach { timing ->
                            FilterChip(
                                selected = timing in state.notificationTimings,
                                onClick = { viewModel.onToggleNotificationTiming(timing) },
                                label = { Text(timing.displayNameAz) }
                            )
                        }
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Deadline-dan əvvəl bildiriş", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "Özünüz tarix/saat seçin",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = state.customNotificationEnabled,
                                onCheckedChange = { viewModel.onToggleCustomNotification() }
                            )
                        }
                        if (state.customNotificationEnabled) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                TextButton(onClick = { showDatePickerFor = PickerTarget.CUSTOM_NOTIFICATION }) {
                                    Text(state.customNotificationTime.formatDate())
                                }
                                TextButton(onClick = { showTimePickerFor = PickerTarget.CUSTOM_NOTIFICATION }) {
                                    Text(state.customNotificationTime.formatTime())
                                }
                            }
                        }
                    }
                }
            }

            state.errorMessage?.let { error ->
                item {
                    Text(error, color = MaterialTheme.colorScheme.error)
                }
            }

            item {
                Button(onClick = viewModel::save, modifier = Modifier.fillMaxWidth()) {
                    Text(if (state.isEditMode) "Yadda saxla" else "Task yarat")
                }
            }
        }
    }

    fun currentValueFor(target: PickerTarget): LocalDateTime = when (target) {
        PickerTarget.DATE -> state.date
        PickerTarget.DEADLINE -> state.deadline
        PickerTarget.CUSTOM_NOTIFICATION -> state.customNotificationTime
    }

    fun applyValue(target: PickerTarget, value: LocalDateTime) = when (target) {
        PickerTarget.DATE -> viewModel.onDateChanged(value)
        PickerTarget.DEADLINE -> viewModel.onDeadlineChanged(value)
        PickerTarget.CUSTOM_NOTIFICATION -> viewModel.onCustomNotificationTimeChanged(value)
    }

    showDatePickerFor?.let { target ->
        val initialMillis = currentValueFor(target)
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePickerFor = null },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val newDate = Instant.ofEpochMilli(millis).atZone(ZoneId.of("UTC")).toLocalDate()
                        val current = currentValueFor(target)
                        applyValue(target, LocalDateTime.of(newDate, current.toLocalTime()))
                    }
                    showDatePickerFor = null
                }) { Text("Təsdiqlə") }
            },
            dismissButton = { TextButton(onClick = { showDatePickerFor = null }) { Text("Ləğv et") } }
        ) { DatePicker(state = datePickerState) }
    }

    showTimePickerFor?.let { target ->
        val current = currentValueFor(target)
        var useKeyboardInput by remember(target) { mutableStateOf(false) }
        val timePickerState = rememberTimePickerState(
            initialHour = current.hour, initialMinute = current.minute, is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTimePickerFor = null },
            confirmButton = {
                TextButton(onClick = {
                    val updated = LocalDateTime.of(
                        current.toLocalDate(),
                        LocalTime.of(timePickerState.hour, timePickerState.minute)
                    )
                    applyValue(target, updated)
                    showTimePickerFor = null
                }) { Text("Təsdiqlə") }
            },
            dismissButton = { TextButton(onClick = { showTimePickerFor = null }) { Text("Ləğv et") } },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // İki rejim: saat əqrəbi (dial) və ya birbaşa rəqəm yazma (klaviatura)
                    if (useKeyboardInput) {
                        TimeInput(state = timePickerState)
                    } else {
                        TimePicker(state = timePickerState)
                    }
                    TextButton(onClick = { useKeyboardInput = !useKeyboardInput }) {
                        Icon(
                            if (useKeyboardInput) Icons.Filled.Schedule else Icons.Filled.Keyboard,
                            contentDescription = null
                        )
                        androidx.compose.foundation.layout.Spacer(Modifier.width(4.dp))
                        Text(if (useKeyboardInput) "Saat əqrəbi ilə seç" else "Klaviatura ilə yaz")
                    }
                }
            }
        )
    }
}

private enum class PickerTarget { DATE, DEADLINE, CUSTOM_NOTIFICATION }

@Composable
private fun AssigneeSelector(
    users: List<com.example.smarttaskmanager.domain.model.AppUser>,
    selectedName: String?,
    onUserSelected: (com.example.smarttaskmanager.domain.model.AppUser?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = selectedName ?: "Təyin edilməyib",
            onValueChange = {},
            readOnly = true,
            label = { Text("Cavabdeh") },
            trailingIcon = {
                Icon(
                    if (expanded) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown,
                    contentDescription = null
                )
            },
            modifier = Modifier.fillMaxWidth()
        )
        // OutlinedTextField "readOnly" olsa da klik hadisələrini birbaşa vermir,
        // ona görə üzərinə şəffaf, klikə həssas bir təbəqə qoyulur.
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { expanded = !expanded }
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            DropdownMenuItem(
                text = { Text("Təyin edilməyib") },
                onClick = { onUserSelected(null); expanded = false }
            )
            users.forEach { user ->
                DropdownMenuItem(
                    text = { Text(user.displayName) },
                    onClick = { onUserSelected(user); expanded = false }
                )
            }
        }
    }
}

@Composable
private fun DateTimeRow(
    label: String,
    dateTime: LocalDateTime,
    onDateClick: () -> Unit,
    onTimeClick: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextButton(onClick = onDateClick) { Text(dateTime.formatDate()) }
                TextButton(onClick = onTimeClick) { Text(dateTime.formatTime()) }
            }
        }
    }
}
