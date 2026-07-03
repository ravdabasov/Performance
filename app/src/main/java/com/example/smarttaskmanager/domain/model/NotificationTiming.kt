package com.example.smarttaskmanager.domain.model

/**
 * Bildirişin nə vaxt göndəriləcəyini müəyyən edir.
 * Yalnız "Deadline zamanı" sabit seçimdir. Deadline-dan əvvəl bildiriş üçün
 * istifadəçi Add/Edit ekranındakı "Xüsusi bildiriş tarixi/saatı" seçimi ilə
 * özü istənilən tarix/saat seçir (sabit 30/15 dəqiqəlik variantlar yoxdur).
 */
enum class NotificationTiming(val displayNameAz: String, val offsetMinutes: Long) {
    AT_DEADLINE("Deadline zamanı", 0L)
}
