package com.example.smarttaskmanager.di

import com.example.smarttaskmanager.data.repository.AuthRepositoryImpl
import com.example.smarttaskmanager.data.repository.FirestoreTaskCommentRepositoryImpl
import com.example.smarttaskmanager.data.repository.FirestoreTaskRepositoryImpl
import com.example.smarttaskmanager.data.repository.FirestoreTeamChatRepositoryImpl
import com.example.smarttaskmanager.data.repository.FirestoreUserRepositoryImpl
import com.example.smarttaskmanager.domain.repository.AuthRepository
import com.example.smarttaskmanager.domain.repository.TaskCommentRepository
import com.example.smarttaskmanager.domain.repository.TaskNotificationScheduler
import com.example.smarttaskmanager.domain.repository.TaskRepository
import com.example.smarttaskmanager.domain.repository.TeamChatRepository
import com.example.smarttaskmanager.domain.repository.UserRepository
import com.example.smarttaskmanager.notification.AlarmSchedulerImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dependency Inversion Principle-in Hilt vasitəsilə tətbiqi: domain interfeysləri
 * konkret data/notification implementasiyalarına burada "bağlanır" (bind).
 *
 * TaskRepository indi Firestore-a bağlanır (əvvəllər Room idi) - komanda daxilində
 * real-vaxt paylaşılan tasklar üçün. Room faylları toxunulmayıb (istifadə olunmur).
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindTaskRepository(impl: FirestoreTaskRepositoryImpl): TaskRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindTaskCommentRepository(impl: FirestoreTaskCommentRepositoryImpl): TaskCommentRepository

    @Binds
    @Singleton
    abstract fun bindTeamChatRepository(impl: FirestoreTeamChatRepositoryImpl): TeamChatRepository

    @Binds
    @Singleton
    abstract fun bindUserRepository(impl: FirestoreUserRepositoryImpl): UserRepository

    @Binds
    @Singleton
    abstract fun bindTaskNotificationScheduler(impl: AlarmSchedulerImpl): TaskNotificationScheduler
}
