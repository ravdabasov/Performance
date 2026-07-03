package com.example.smarttaskmanager.data.repository

import com.example.smarttaskmanager.domain.model.ChatMessage
import com.example.smarttaskmanager.domain.repository.TeamChatRepository
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
 * Komandanın ümumi söhbət kanalı - "team_chat" kolleksiyası, HEÇ bir task-a bağlı deyil.
 */
@Singleton
class FirestoreTeamChatRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : TeamChatRepository {

    private val isoFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
    private val collection = firestore.collection("team_chat")

    override fun getMessages(): Flow<List<ChatMessage>> = callbackFlow {
        val registration = collection
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val messages = snapshot?.documents?.mapNotNull { it.toMessage() } ?: emptyList()
                trySend(messages)
            }
        awaitClose { registration.remove() }
    }

    override suspend fun sendMessage(authorUid: String, authorName: String, text: String) {
        val data = mapOf(
            "authorUid" to authorUid,
            "authorName" to authorName,
            "text" to text,
            "createdAt" to LocalDateTime.now().format(isoFormatter)
        )
        collection.add(data).await()
    }

    override suspend fun clearAllMessages() {
        val snapshot = collection.get().await()
        if (snapshot.isEmpty) return
        // Firestore batch-lər maksimum 500 əməliyyat qəbul edir - lazım olarsa hissələrə bölür.
        snapshot.documents.chunked(450).forEach { chunk ->
            val batch = firestore.batch()
            chunk.forEach { doc -> batch.delete(doc.reference) }
            batch.commit().await()
        }
    }

    private fun DocumentSnapshot.toMessage(): ChatMessage? {
        val text = getString("text") ?: return null
        val createdAtStr = getString("createdAt")
        val createdAt = createdAtStr?.let { runCatching { LocalDateTime.parse(it, isoFormatter) }.getOrNull() }
            ?: LocalDateTime.now()
        return ChatMessage(
            id = id,
            authorUid = getString("authorUid") ?: "",
            authorName = getString("authorName") ?: "Naməlum",
            text = text,
            createdAt = createdAt
        )
    }
}
