package com.example.smarttaskmanager.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.smarttaskmanager.data.local.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    /**
     * Smart Prioritetləşdirmə alqoritminin DB səviyyəsindəki hissəsi:
     * 1) deadline-a ən yaxın olan əvvəl gəlir
     * 2) bərabərlikdə prioritet balı yüksək olan əvvəl gəlir
     * 3) sonda daha əvvəl yaradılan əvvəl gəlir
     * Tamamlanmış/ləğv edilmiş tasklar bu siyahıya düşmür (ayrıca bölmədədir).
     */
    /**
     * Bütün taskları (tamamlanan/ləğv edilən daxil) smart-sort sırası ilə qaytarır.
     * "Bütün Tasklar" bölməsi bunu istifadə edir - status filtri YOXDUR.
     */
    @Query(
        """
        SELECT * FROM tasks 
        ORDER BY deadline ASC, priority DESC, createdAt ASC
        """
    )
    fun getAllTasksSmartSorted(): Flow<List<TaskEntity>>

    @Query(
        """
        SELECT * FROM tasks 
        WHERE status NOT IN ('COMPLETED', 'CANCELLED')
        ORDER BY deadline ASC, priority DESC, createdAt ASC
        """
    )
    fun getActiveTasksSmartSorted(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE status = 'COMPLETED' ORDER BY completedAt DESC")
    fun getCompletedTasks(): Flow<List<TaskEntity>>

    /** Statistika bölməsi üçün: seçilən tarix aralığında deadline-ı olan bütün tasklar. */
    @Query("SELECT * FROM tasks WHERE deadline BETWEEN :startIso AND :endIso ORDER BY deadline ASC")
    fun getTasksInDateRange(startIso: String, endIso: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :taskId LIMIT 1")
    suspend fun getTaskById(taskId: Long): TaskEntity?

    @Query(
        """
        SELECT * FROM tasks 
        WHERE (title LIKE '%' || :query || '%' 
           OR description LIKE '%' || :query || '%'
           OR tags LIKE '%' || :query || '%'
           OR category LIKE '%' || :query || '%')
        ORDER BY deadline ASC, priority DESC
        """
    )
    fun searchTasks(query: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE category = :category ORDER BY deadline ASC, priority DESC")
    fun getTasksByCategory(category: String): Flow<List<TaskEntity>>

    @Query("SELECT DISTINCT category FROM tasks ORDER BY category ASC")
    fun getAllCategories(): Flow<List<String>>

    @Query(
        """
        UPDATE tasks SET status = 'OVERDUE' 
        WHERE status IN ('PENDING', 'IN_PROGRESS') AND deadline < :now
        """
    )
    suspend fun markOverdueTasks(now: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Delete
    suspend fun deleteTask(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteTaskById(taskId: Long)

    // --- Dashboard statistikası üçün ---
    @Query("SELECT COUNT(*) FROM tasks WHERE date(deadline) = date(:today) AND status NOT IN ('COMPLETED','CANCELLED')")
    fun getTodayTaskCount(today: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM tasks WHERE status = 'OVERDUE'")
    fun getOverdueCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM tasks WHERE status = 'COMPLETED'")
    fun getCompletedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM tasks WHERE status NOT IN ('COMPLETED','CANCELLED')")
    fun getActiveCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM tasks")
    fun getTotalCount(): Flow<Int>
}
