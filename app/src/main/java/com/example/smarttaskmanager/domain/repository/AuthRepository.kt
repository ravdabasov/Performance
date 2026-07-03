package com.example.smarttaskmanager.domain.repository

import com.example.smarttaskmanager.domain.model.AppUser
import kotlinx.coroutines.flow.Flow

/** Nəticə tipi - uğur/uğursuzluğu tip-təhlükəsiz şəkildə bildirir. */
sealed class AuthResult {
    data object Success : AuthResult()
    data class Error(val messageAz: String) : AuthResult()
}

interface AuthRepository {
    /** Cari daxil olmuş istifadəçi (null = daxil olmayıb). Auth vəziyyəti dəyişəndə canlı yenilənir. */
    val currentUser: Flow<AppUser?>

    suspend fun signUp(email: String, password: String, displayName: String): AuthResult
    suspend fun signIn(email: String, password: String): AuthResult
    fun signOut()

    /**
     * Cari istifadəçinin hesabının hələ də Firebase-də mövcud olub-olmadığını yoxlayır
     * (serverdən). Əgər hesab (Console-dan) silinibsə, telefonu avtomatik çıxış etdirir.
     */
    suspend fun validateSession()
}
