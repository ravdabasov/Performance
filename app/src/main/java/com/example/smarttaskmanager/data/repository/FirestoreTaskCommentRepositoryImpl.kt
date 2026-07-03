package com.example.smarttaskmanager.data.repository

import com.example.smarttaskmanager.domain.model.TaskComment
import com.example.smarttaskmanager.domain.repository.TaskCommentRepository
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Hər task-ın öz "comments" alt-kolleksiyası var (tasks/{taskId}/comments) - komanda
 * üzvləri task barədə real-vaxt müzakirə apara bilir (Tələb: "tapşırıq barədə daxili chat").
 */
@Singleton
class FirestoreTaskCommentRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : TaskCommentRepository {

    private val isoFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    override fun getComments(taskId: Long): Flow<List<TaskComment>> = callbackFlow {
        val registration = firestore.collection("tasks").document(taskId.toString())
            .collection("comments")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val comments = snapshot?.documents?.mapNotNull { it.toComment() } ?: emptyList()
                trySend(comments)
            }
        awaitClose { registration.remove() }
    }

    override suspend fun sendComment(taskId: Long, authorUid: String, authorName: String, text: String) {
        val now = LocalDateTime.now()
        val data = mapOf(
            "authorUid" to authorUid,
            "authorName" to authorName,
            "text" to text,
            "createdAt" to now.format(isoFormatter)
        )
        val taskDoc = firestore.collection("tasks").document(taskId.toString())
        taskDoc.collection("comments").add(data).await()
        // Chat siyahısında son mesaj önizləməsi göstərmək üçün task sənədini də yeniləyir.
        taskDoc.update(
            mapOf(
                "lastMessageText" to text,
                "lastMessageAuthor" to authorName,
                "lastMessageAt" to now.format(isoFormatter)
            )
        ).await()
    }

    private fun DocumentSnapshot.toComment(): TaskComment? {
        val text = getString("text") ?: return null
        val createdAtStr = getString("createdAt")
        val createdAt = createdAtStr?.let { runCatching { LocalDateTime.parse(it, isoFormatter) }.getOrNull() }
            ?: LocalDateTime.now()
        return TaskComment(
            id = id,
            authorUid = getString("authorUid") ?: "",
            authorName = getString("authorName") ?: "Naməlum",
            text = text,
            createdAt = createdAt
        )
    }

    override suspend fun clearComments(taskId: Long) {
        val taskDoc = firestore.collection("tasks").document(taskId.toString())
        val snapshot = taskDoc.collection("comments").get().await()
        if (!snapshot.isEmpty) {
            snapshot.documents.chunked(450).forEach { chunk ->
                val batch = firestore.batch()
                chunk.forEach { doc -> batch.delete(doc.reference) }
                batch.commit().await()
            }
        }
        taskDoc.update(
            mapOf(
                "lastMessageText" to null,
                "lastMessageAuthor" to null,
                "lastMessageAt" to null
            )
        ).await()
    }
}
