package com.example.smarttaskmanager.domain.repository

import com.example.smarttaskmanager.domain.model.AppUser
import kotlinx.coroutines.flow.Flow

/** Qeydiyyatdan keçmiş komanda üzvlərinin siyahısı - task-a cavabdeh təyin etmək üçün. */
interface UserRepository {
    fun getAllUsers(): Flow<List<AppUser>>
}
