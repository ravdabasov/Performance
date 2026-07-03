package com.example.smarttaskmanager.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestoreSettings
import com.google.firebase.firestore.memoryCacheSettings
import com.google.firebase.firestore.persistentCacheSettings
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirestore(): FirebaseFirestore {
        val firestore = FirebaseFirestore.getInstance()
        // Offline persistence AKTİV - internet olmadıqda da oxunub-yazıla bilər,
        // internet qayıdanda avtomatik sinxronlaşır (Firestore-un daxili funksiyası).
        firestore.firestoreSettings = firestoreSettings {
            setLocalCacheSettings(persistentCacheSettings {})
        }
        return firestore
    }
}
