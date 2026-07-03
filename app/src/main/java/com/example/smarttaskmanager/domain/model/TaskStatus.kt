package com.example.smarttaskmanager.domain.model

/**
 * Task-in ola biləcəyi bütün statuslar.
 * Overdue statusu avtomatik hesablanır (deadline keçəndə) - istifadəçi əl ilə seçmir.
 */
enum class TaskStatus(val displayNameAz: String) {
    PENDING("Gözləyir"),
    IN_PROGRESS("İcra olunur"),
    COMPLETED("Tamamlandı"),
    OVERDUE("Gecikmiş"),
    CANCELLED("Ləğv edilib")
}
