package com.example.smarttaskmanager.data.repository

import com.example.smarttaskmanager.domain.model.AppUser
import com.example.smarttaskmanager.domain.repository.UserRepository
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreUserRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : UserRepository {

    override fun getAllUsers(): Flow<List<AppUser>> = callbackFlow {
        val registration = firestore.collection("users")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val users = snapshot?.documents?.mapNotNull { it.toAppUser() } ?: emptyList()
                trySend(users.sortedBy { it.displayName })
            }
        awaitClose { registration.remove() }
    }

    private fun DocumentSnapshot.toAppUser(): AppUser? {
        val uid = getString("uid") ?: id
        val email = getString("email") ?: return null
        val displayName = getString("displayName")?.ifBlank { null } ?: email
        return AppUser(uid = uid, email = email, displayName = displayName)
    }
}
