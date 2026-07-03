package com.example.smarttaskmanager.domain.model

import java.time.LocalDateTime

/** Komandanın ümumi (tasklardan asılı olmayan) söhbət mesajı. */
data class ChatMessage(
    val id: String = "",
    val authorUid: String,
    val authorName: String,
    val text: String,
    val createdAt: LocalDateTime = LocalDateTime.now()
)
