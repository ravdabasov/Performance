package com.example.smarttaskmanager.notification

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.MetadataChanges
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Tələb: "əlavə edilən taskı proqramı istifadə edən şəxslər görsün, dəyişiklikləri
 * də görsünlər" - Firestore real-vaxt listener vasitəsilə, komanda üzvlərindən biri
 * yeni task yaradanda və ya tamamlayanda, DİGƏR üzvlərin telefonunda (tətbiq açıq/arxa
 * planda olduğu müddətdə) bildiriş göstərilir.
 *
 * QEYD: Bu, tətbiq TAM bağlandıqda (process öldürüldükdə) işləmir - bunun üçün
 * Firebase Cloud Functions + FCM push tələb olunur (əlavə mərhələ, Blaze planı istəyir).
 */
@Singleton
class TeamActivityNotifier @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth,
    private val notificationHelper: NotificationHelper
) {
    private var taskRegistration: ListenerRegistration? = null
    private var chatRegistration: ListenerRegistration? = null

    fun start() {
        startTaskListener()
        startChatListener()
    }

    private fun startTaskListener() {
        if (taskRegistration != null) return
        taskRegistration = firestore.collection("tasks")
            .addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener

                val myUid = firebaseAuth.currentUser?.uid
                val myName = firebaseAuth.currentUser?.displayName?.ifBlank { null }
                    ?: firebaseAuth.currentUser?.email

                snapshot.documentChanges.forEach { change ->
                    // Öz cihazımızdan gedən (hələ serverə təsdiqlənməmiş) yazıları görməzdən gəl
                    if (change.document.metadata.hasPendingWrites()) return@forEach

                    val data = change.document.data
                    val title = data["title"] as? String ?: return@forEach
                    val createdByUid = data["createdByUid"] as? String

                    when (change.type) {
                        DocumentChange.Type.ADDED -> {
                            if (createdByUid != null && createdByUid != myUid) {
                                val creator = data["createdByName"] as? String ?: "Komanda üzvü"
                                notify(change.document.id, 0, "Yeni task əlavə olundu", "$creator: \"$title\"")
                            }
                        }
                        DocumentChange.Type.MODIFIED -> {
                            val status = data["status"] as? String
                            val completedBy = data["completedByName"] as? String
                            if (status == "COMPLETED" && completedBy != null && completedBy != myName) {
                                notify(change.document.id, 1, "Task tamamlandı", "$completedBy: \"$title\" tamamladı")
                            }
                        }
                        DocumentChange.Type.REMOVED -> Unit
                    }
                }
            }
    }

    private fun startChatListener() {
        if (chatRegistration != null) return
        chatRegistration = firestore.collection("team_chat")
            .addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                val myUid = firebaseAuth.currentUser?.uid

                snapshot.documentChanges.forEach { change ->
                    if (change.document.metadata.hasPendingWrites()) return@forEach
                    if (change.type != DocumentChange.Type.ADDED) return@forEach

                    val data = change.document.data
                    val text = data["text"] as? String ?: return@forEach
                    val authorUid = data["authorUid"] as? String
                    val authorName = data["authorName"] as? String ?: "Komanda üzvü"

                    if (authorUid != null && authorUid != myUid) {
                        val notificationId = (change.document.id.hashCode() and 0x0FFFFFFF) or (2 shl 28)
                        notificationHelper.showNotification(notificationId, "Yeni mesaj: $authorName", text)
                    }
                }
            }
    }

    fun stop() {
        taskRegistration?.remove()
        taskRegistration = null
        chatRegistration?.remove()
        chatRegistration = null
    }

    private fun notify(docId: String, variant: Int, title: String, message: String) {
        // Şəxsi deadline bildirişləri ilə eyni ID diapazonuna düşməsin deyə fərqli aralıq istifadə olunur.
        val notificationId = (docId.hashCode() and 0x0FFFFFFF) or (variant shl 28)
        notificationHelper.showNotification(notificationId, title, message)
    }
}
