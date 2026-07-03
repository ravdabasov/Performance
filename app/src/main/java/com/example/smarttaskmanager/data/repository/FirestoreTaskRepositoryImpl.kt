package com.example.smarttaskmanager.data.repository

import com.example.smarttaskmanager.domain.model.NotificationTiming
import com.example.smarttaskmanager.domain.model.RecurrenceType
import com.example.smarttaskmanager.domain.model.Task
import com.example.smarttaskmanager.domain.model.TaskStatus
import com.example.smarttaskmanager.domain.repository.TaskRepository
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

/**
 * TaskRepository-nin Firestore-a əsaslanan implementasiyası - komanda üzvləri arasında
 * REAL-VAXT paylaşılan tasklar (Tələb: "əlavə edilən taskı proqramı istifadə edən
 * şəxslər görsün"). Firestore-un daxili offline cache-i sayəsində internet olmadıqda
 * da işləyir, internet qayıdanda avtomatik sinxronlaşır.
 *
 * Sadəlik və etibarlılıq üçün TƏK bir real-vaxt listener (bütün "tasks" kolleksiyası)
 * saxlanılır, digər bütün sorğular (aktiv/tamamlanan/axtarış/tarix aralığı/saylar)
 * bunun üzərində client-side (in-memory) süzgəcdən keçir - komanda ölçüsündə (onlarla-
 * yüzlərlə task) bu tam performanslıdır və Firestore composite index tələb etmir.
 */
@Singleton
class FirestoreTaskRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : TaskRepository {

    private val tasksCollection = firestore.collection("tasks")
    private val isoFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    private val allTasksFlow: Flow<List<Task>> = callbackFlow {
        val registration = tasksCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            val tasks = snapshot?.documents?.mapNotNull { it.toTask() } ?: emptyList()
            trySend(tasks)
        }
        awaitClose { registration.remove() }
    }

    private val smartComparator = compareBy<Task>({ it.deadline }, { -it.priority }, { it.createdAt })

    override fun getAllTasksSmartSorted(): Flow<List<Task>> =
        allTasksFlow.map { it.sortedWith(smartComparator) }

    override fun getActiveTasksSmartSorted(): Flow<List<Task>> = allTasksFlow.map { list ->
        list.filter { it.status != TaskStatus.COMPLETED && it.status != TaskStatus.CANCELLED }
            .sortedWith(smartComparator)
    }

    override fun getCompletedTasks(): Flow<List<Task>> = allTasksFlow.map { list ->
        list.filter { it.status == TaskStatus.COMPLETED }
            .sortedByDescending { it.completedAt ?: it.createdAt }
    }

    override suspend fun getTaskById(taskId: Long): Task? =
        tasksCollection.document(taskId.toString()).get().await().toTask()

    override fun searchTasks(query: String): Flow<List<Task>> = allTasksFlow.map { list ->
        list.filter { task ->
            task.title.contains(query, ignoreCase = true) ||
                task.description.contains(query, ignoreCase = true) ||
                task.category.contains(query, ignoreCase = true) ||
                task.tags.any { it.contains(query, ignoreCase = true) }
        }.sortedWith(smartComparator)
    }

    override fun getTasksInDateRange(start: LocalDateTime, end: LocalDateTime): Flow<List<Task>> =
        allTasksFlow.map { list ->
            list.filter { !it.deadline.isBefore(start) && !it.deadline.isAfter(end) }
        }

    override suspend fun createTask(task: Task): Long {
        val id = if (task.id != 0L) task.id else generateTaskId()
        tasksCollection.document(id.toString()).set(task.copy(id = id).toFirestoreMap()).await()
        return id
    }

    override suspend fun updateTask(task: Task) {
        tasksCollection.document(task.id.toString()).set(task.toFirestoreMap()).await()
    }

    override suspend fun deleteTask(task: Task) {
        tasksCollection.document(task.id.toString()).delete().await()
    }

    override suspend fun markTaskCompleted(taskId: Long) {
        tasksCollection.document(taskId.toString()).update(
            mapOf(
                "status" to TaskStatus.COMPLETED.name,
                "completedAt" to LocalDateTime.now().format(isoFormatter)
            )
        ).await()
    }

    override suspend fun toggleTaskCompleted(taskId: Long) {
        val current = getTaskById(taskId) ?: return
        if (current.status == TaskStatus.COMPLETED) {
            val newStatus = if (LocalDateTime.now().isAfter(current.deadline)) {
                TaskStatus.OVERDUE
            } else {
                TaskStatus.PENDING
            }
            tasksCollection.document(taskId.toString()).update(
                mapOf("status" to newStatus.name, "completedAt" to null)
            ).await()
        } else {
            tasksCollection.document(taskId.toString()).update(
                mapOf(
                    "status" to TaskStatus.COMPLETED.name,
                    "completedAt" to LocalDateTime.now().format(isoFormatter)
                )
            ).await()
        }
    }

    override suspend fun refreshOverdueStatuses() {
        val now = LocalDateTime.now()
        val snapshot = tasksCollection.get().await()
        val batch = firestore.batch()
        var hasUpdates = false
        snapshot.documents.forEach { doc ->
            val status = doc.getString("status")
            val deadlineStr = doc.getString("deadline")
            if (status in listOf(TaskStatus.PENDING.name, TaskStatus.IN_PROGRESS.name) && deadlineStr != null) {
                val deadline = parseIso(deadlineStr)
                if (deadline != null && now.isAfter(deadline)) {
                    batch.update(doc.reference, "status", TaskStatus.OVERDUE.name)
                    hasUpdates = true
                }
            }
        }
        if (hasUpdates) batch.commit().await()
    }

    override fun getTodayTaskCount(): Flow<Int> = allTasksFlow.map { list ->
        val today = LocalDate.now()
        list.count {
            it.status != TaskStatus.COMPLETED && it.status != TaskStatus.CANCELLED &&
                it.deadline.toLocalDate() == today
        }
    }

    override fun getOverdueCount(): Flow<Int> =
        allTasksFlow.map { list -> list.count { it.effectiveStatus == TaskStatus.OVERDUE } }

    override fun getCompletedCount(): Flow<Int> =
        allTasksFlow.map { list -> list.count { it.status == TaskStatus.COMPLETED } }

    override fun getActiveCount(): Flow<Int> = allTasksFlow.map { list ->
        list.count { it.status != TaskStatus.COMPLETED && it.status != TaskStatus.CANCELLED }
    }

    override fun getTotalCount(): Flow<Int> = allTasksFlow.map { it.size }

    private fun generateTaskId(): Long = System.currentTimeMillis() * 1000L + Random.nextInt(1000)

    private fun parseIso(value: String?): LocalDateTime? =
        value?.let { runCatching { LocalDateTime.parse(it, isoFormatter) }.getOrNull() }

    private fun Task.toFirestoreMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "title" to title,
        "description" to description,
        "date" to date.format(isoFormatter),
        "deadline" to deadline.format(isoFormatter),
        "priority" to priority,
        "category" to category,
        "tags" to tags,
        "status" to status.name,
        "notificationTimings" to notificationTimings.map { it.name },
        "customNotificationTime" to customNotificationTime?.format(isoFormatter),
        "recurrenceType" to recurrenceType.name,
        "createdByUid" to createdByUid,
        "createdByName" to createdByName,
        "completedByName" to completedByName,
        "lastMessageText" to lastMessageText,
        "lastMessageAuthor" to lastMessageAuthor,
        "lastMessageAt" to lastMessageAt?.format(isoFormatter),
        "assigneeUid" to assigneeUid,
        "assigneeName" to assigneeName,
        "createdAt" to createdAt.format(isoFormatter),
        "completedAt" to completedAt?.format(isoFormatter)
    )

    private fun DocumentSnapshot.toTask(): Task? {
        if (!exists()) return null
        return try {
            Task(
                id = getLong("id") ?: id.toLongOrNull() ?: return null,
                title = getString("title") ?: return null,
                description = getString("description") ?: "",
                date = parseIso(getString("date")) ?: return null,
                deadline = parseIso(getString("deadline")) ?: return null,
                priority = (getLong("priority") ?: 5L).toInt(),
                category = getString("category") ?: "Ümumi",
                tags = (get("tags") as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                status = runCatching { TaskStatus.valueOf(getString("status") ?: "PENDING") }
                    .getOrDefault(TaskStatus.PENDING),
                notificationTimings = (get("notificationTimings") as? List<*>)
                    ?.mapNotNull { name -> runCatching { NotificationTiming.valueOf(name as String) }.getOrNull() }
                    ?.toSet() ?: emptySet(),
                customNotificationTime = parseIso(getString("customNotificationTime")),
                recurrenceType = runCatching { RecurrenceType.valueOf(getString("recurrenceType") ?: "NONE") }
                    .getOrDefault(RecurrenceType.NONE),
                createdByUid = getString("createdByUid") ?: "",
                createdByName = getString("createdByName") ?: "",
                completedByName = getString("completedByName"),
                lastMessageText = getString("lastMessageText"),
                lastMessageAuthor = getString("lastMessageAuthor"),
                lastMessageAt = parseIso(getString("lastMessageAt")),
                assigneeUid = getString("assigneeUid"),
                assigneeName = getString("assigneeName"),
                createdAt = parseIso(getString("createdAt")) ?: LocalDateTime.now(),
                completedAt = parseIso(getString("completedAt"))
            )
        } catch (e: Exception) {
            null
        }
    }
}
