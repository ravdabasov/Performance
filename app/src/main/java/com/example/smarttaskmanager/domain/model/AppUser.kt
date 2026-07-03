package com.example.smarttaskmanager.domain.model

/** Firebase Authentication ilə daxil olmuş komanda üzvü. */
data class AppUser(
    val uid: String,
    val email: String,
    val displayName: String
)
