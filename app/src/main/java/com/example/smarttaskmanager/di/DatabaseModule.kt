package com.example.smarttaskmanager.di

import android.content.Context
import androidx.room.Room
import com.example.smarttaskmanager.data.local.AppDatabase
import com.example.smarttaskmanager.data.local.dao.TaskDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.DATABASE_NAME)
            // Development mərhələsində schema dəyişəndə köhnə cədvəli təmizləyib yenidən qurur.
            // Tətbiq rəsmi buraxılışdan əvvəl olduğu üçün bu qəbul edilə bilər.
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    @Singleton
    fun provideTaskDao(database: AppDatabase): TaskDao = database.taskDao()
}
