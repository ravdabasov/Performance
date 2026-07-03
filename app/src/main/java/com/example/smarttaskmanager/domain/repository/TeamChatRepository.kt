package com.example.smarttaskmanager.domain.repository

import com.example.smarttaskmanager.domain.model.ChatMessage
import kotlinx.coroutines.flow.Flow

/** Tələb: "Söhbətlər" bölməsi tasklardan asılı deyil - komandanın ümumi kanalı. */
interface TeamChatRepository {
    fun getMessages(): Flow<List<ChatMessage>>
    suspend fun sendMessage(authorUid: String, authorName: String, text: String)
    /** Bütün komanda üzvləri üçün söhbəti tamamilə təmizləyir (geri qaytarıla bilməz). */
    suspend fun clearAllMessages()
}
