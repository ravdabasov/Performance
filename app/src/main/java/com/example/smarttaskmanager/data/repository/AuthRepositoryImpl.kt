package com.example.smarttaskmanager.data.repository

import com.example.smarttaskmanager.domain.model.AppUser
import com.example.smarttaskmanager.domain.repository.AuthRepository
import com.example.smarttaskmanager.domain.repository.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * Firebase Authentication ilə e-poçt/parol əsaslı giriş sistemi.
 * Komanda üzvləri öz hesabları ilə daxil olur, "displayName" başqalarına
 * "kim yaratdı/kim tamamladı" göstərmək üçün istifadə olunur.
 */
class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : AuthRepository {

    override val currentUser: Flow<AppUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser?.let {
                AppUser(
                    uid = it.uid,
                    email = it.email.orEmpty(),
                    displayName = it.displayName?.ifBlank { it.email.orEmpty() } ?: it.email.orEmpty()
                )
            })
        }
        firebaseAuth.addAuthStateListener(listener)
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }

    override suspend fun signUp(email: String, password: String, displayName: String): AuthResult {
        return try {
            val result = firebaseAuth.createUserWithEmailAndPassword(email.trim(), password).await()
            val uid = result.user?.uid
            result.user?.updateProfile(
                userProfileChangeRequest { this.displayName = displayName.trim() }
            )?.await()
            // "users" kolleksiyasına yazır ki, digər üzvlər task yaradanda cavabdeh
            // seçimi siyahısında bu şəxsi görsün (Firebase Auth-un istifadəçi siyahısı
            // client tərəfindən oxuna bilmir, ona görə öz kolleksiyamızı saxlayırıq).
            if (uid != null) {
                firestore.collection("users").document(uid).set(
                    mapOf("uid" to uid, "email" to email.trim(), "displayName" to displayName.trim())
                ).await()
            }
            AuthResult.Success
        } catch (e: Exception) {
            AuthResult.Error(mapErrorAz(e))
        }
    }

    override suspend fun signIn(email: String, password: String): AuthResult {
        return try {
            val result = firebaseAuth.signInWithEmailAndPassword(email.trim(), password).await()
            val user = result.user
            // Köhnə hesablar üçün "users" sənədini bura da yazır (backfill) ki, cavabdeh
            // seçimi siyahısında əvvəldən qeydiyyatdan keçmiş şəxslər də görünsün.
            if (user != null) {
                firestore.collection("users").document(user.uid).set(
                    mapOf(
                        "uid" to user.uid,
                        "email" to (user.email ?: ""),
                        "displayName" to (user.displayName?.ifBlank { null } ?: user.email ?: "")
                    )
                ).await()
            }
            AuthResult.Success
        } catch (e: Exception) {
            AuthResult.Error(mapErrorAz(e))
        }
    }

    override fun signOut() {
        firebaseAuth.signOut()
    }

    override suspend fun validateSession() {
        val user = firebaseAuth.currentUser ?: return
        try {
            // reload() serverdən istifadəçinin hələ mövcud olub-olmadığını yoxlayır.
            user.reload().await()
        } catch (e: com.google.firebase.auth.FirebaseAuthInvalidUserException) {
            // Hesab Console-dan silinib - telefonu dərhal çıxış etdirir.
            firebaseAuth.signOut()
        } catch (e: Exception) {
            // İnternet yoxdursa və ya başqa müvəqqəti xəta - sükutla keç, sonra yenidən yoxlanacaq.
        }
    }

    /** Firebase-in ingiliscə xəta mesajlarını sadə, başa düşülən Azərbaycan dilinə çevirir. */
    private fun mapErrorAz(e: Exception): String {
        val msg = e.message.orEmpty()
        return when {
            msg.contains("badly formatted", ignoreCase = true) -> "E-poçt ünvanı düzgün formatda deyil."
            msg.contains("email address is already in use", ignoreCase = true) -> "Bu e-poçt artıq qeydiyyatdan keçib."
            msg.contains("password is invalid", ignoreCase = true) ||
                msg.contains("no user record", ignoreCase = true) ||
                msg.contains("INVALID_LOGIN_CREDENTIALS", ignoreCase = true) -> "E-poçt və ya parol yanlışdır."
            msg.contains("network", ignoreCase = true) -> "İnternet bağlantısını yoxlayın."
            msg.contains("at least 6 characters", ignoreCase = true) -> "Parol ən azı 6 simvol olmalıdır."
            else -> "Xəta baş verdi: $msg"
        }
    }
}
