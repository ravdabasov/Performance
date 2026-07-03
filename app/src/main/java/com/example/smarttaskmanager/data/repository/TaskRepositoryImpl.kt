package com.example.smarttaskmanager.data.repository

import com.example.smarttaskmanager.data.local.dao.TaskDao
import com.example.smarttaskmanager.data.local.entity.toDomain
import com.example.smarttaskmanager.data.local.entity.toEntity
import com.example.smarttaskmanager.domain.model.Task
import com.example.smarttaskmanager.domain.model.TaskStatus
import com.example.smarttaskmanager.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * TaskRepository-nin Room-a əsaslanan implementasiyası.
 * Bütün əməliyyatlar tamamilə lokaldır - şəbəkə çağırışı yoxdur (offline-first tələbinə uyğun).
 */
class TaskRepositoryImpl @Inject constructor(
    private val dao: TaskDao
) : TaskRepository {

    private val isoFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    override fun getAllTasksSmartSorted(): Flow<List<Task>> =
        dao.getAllTasksSmartSorted().map { list -> list.map { it.toDomain() } }

    override fun getActiveTasksSmartSorted(): Flow<List<Task>> =
        dao.getActiveTasksSmartSorted().map { list -> list.map { it.toDomain() } }

    override fun getCompletedTasks(): Flow<List<Task>> =
        dao.getCompletedTasks().map { list -> list.map { it.toDomain() } }

    override suspend fun getTaskById(taskId: Long): Task? =
        dao.getTaskById(taskId)?.toDomain()

    override fun searchTasks(query: String): Flow<List<Task>> =
        dao.searchTasks(query).map { list -> list.map { it.toDomain() } }

    override fun getTasksInDateRange(start: LocalDateTime, end: LocalDateTime): Flow<List<Task>> =
        dao.getTasksInDateRange(start.format(isoFormatter), end.format(isoFormatter))
            .map { list -> list.map { it.toDomain() } }

    override suspend fun createTask(task: Task): Long = dao.insertTask(task.toEntity())

    override suspend fun updateTask(task: Task) = dao.updateTask(task.toEntity())

    override suspend fun deleteTask(task: Task) = dao.deleteTask(task.toEntity())

    override suspend fun markTaskCompleted(taskId: Long) {
        val entity = dao.getTaskById(taskId) ?: return
        dao.updateTask(
            entity.copy(
                status = TaskStatus.COMPLETED.name,
                completedAt = LocalDateTime.now()
            )
        )
    }

    override suspend fun toggleTaskCompleted(taskId: Long) {
        val entity = dao.getTaskById(taskId) ?: return
        if (entity.status == TaskStatus.COMPLETED.name) {
            // Geri götür: deadline keçibsə OVERDUE, əks halda PENDING statusuna qaytarır.
            val newStatus = if (LocalDateTime.now().isAfter(entity.deadline)) {
                TaskStatus.OVERDUE.name
            } else {
                TaskStatus.PENDING.name
            }
            dao.updateTask(entity.copy(status = newStatus, completedAt = null))
        } else {
            dao.updateTask(entity.copy(status = TaskStatus.COMPLETED.name, completedAt = LocalDateTime.now()))
        }
    }

    /**
     * Deadline-i keçmiş, hələ tamamlanmamış taskları OVERDUE-ə çevirir.
     */
    override suspend fun refreshOverdueStatuses() {
        val now = LocalDateTime.now().format(isoFormatter)
        dao.markOverdueTasks(now)
    }

    override fun getTodayTaskCount(): Flow<Int> {
        val today = LocalDateTime.now().format(isoFormatter)
        return dao.getTodayTaskCount(today)
    }

    override fun getOverdueCount(): Flow<Int> = dao.getOverdueCount()
    override fun getCompletedCount(): Flow<Int> = dao.getCompletedCount()
    override fun getActiveCount(): Flow<Int> = dao.getActiveCount()
    override fun getTotalCount(): Flow<Int> = dao.getTotalCount()
}
