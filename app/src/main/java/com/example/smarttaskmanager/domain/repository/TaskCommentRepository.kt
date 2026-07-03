package com.example.smarttaskmanager.domain.repository

import com.example.smarttaskmanager.domain.model.TaskComment
import kotlinx.coroutines.flow.Flow

/** Task daxilindəki chat mesajları üçün paylaşılan, real-vaxt kolleksiya. */
interface TaskCommentRepository {
    fun getComments(taskId: Long): Flow<List<TaskComment>>
    suspend fun sendComment(taskId: Long, authorUid: String, authorName: String, text: String)
    /** Bu task-ın söhbətini bütün üzvlər üçün tamamilə təmizləyir. */
    suspend fun clearComments(taskId: Long)
}
