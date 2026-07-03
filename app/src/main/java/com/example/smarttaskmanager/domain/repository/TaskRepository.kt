package com.example.smarttaskmanager.domain.repository

import com.example.smarttaskmanager.domain.model.Task
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

/**
 * Domain qatının data mənbəyi ilə "razılaşması" (contract). ViewModel/UseCase-lər
 * yalnız bu interfeys ilə işləyir - konkret Room implementasiyasından xəbərsizdirlər
 * (Dependency Inversion Principle).
 */
interface TaskRepository {
    /** Bütün tasklar (tamamlanan/ləğv edilən daxil) - "Bütün Tasklar" bölməsi üçün. */
    fun getAllTasksSmartSorted(): Flow<List<Task>>
    fun getActiveTasksSmartSorted(): Flow<List<Task>>
    fun getCompletedTasks(): Flow<List<Task>>
    suspend fun getTaskById(taskId: Long): Task?
    fun searchTasks(query: String): Flow<List<Task>>
    fun getTasksInDateRange(start: LocalDateTime, end: LocalDateTime): Flow<List<Task>>

    suspend fun createTask(task: Task): Long
    suspend fun updateTask(task: Task)
    suspend fun deleteTask(task: Task)
    suspend fun markTaskCompleted(taskId: Long)
    /** Tamamlanmış task-ı yenidən aktivə (PENDING/OVERDUE) qaytarır - səhv seçimi geri götürmək üçün. */
    suspend fun toggleTaskCompleted(taskId: Long)
    suspend fun refreshOverdueStatuses()

    fun getTodayTaskCount(): Flow<Int>
    fun getOverdueCount(): Flow<Int>
    fun getCompletedCount(): Flow<Int>
    fun getActiveCount(): Flow<Int>
    fun getTotalCount(): Flow<Int>
}
