package com.example.smarttaskmanager.domain.model

import java.time.LocalDateTime

/** Task daxilindəki chat (müzakirə) mesajı. */
data class TaskComment(
    val id: String = "",
    val authorUid: String,
    val authorName: String,
    val text: String,
    val createdAt: LocalDateTime = LocalDateTime.now()
)
