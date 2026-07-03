package com.example.smarttaskmanager.domain.model

/**
 * Task-ın təkrarlanma tezliyi. Task TAMAMLANANDA (checkbox vurulanda) əgər bu
 * NONE-dan fərqlidirsə, sistem avtomatik olaraq növbəti dövr üçün eyni parametrlərlə
 * yeni bir task yaradır (tarix/deadline müvafiq olaraq irəli sürüşdürülür).
 */
enum class RecurrenceType(val displayNameAz: String) {
    NONE("Təkrarlanmır"),
    DAILY("Gündəlik"),
    WEEKLY("Həftəlik"),
    MONTHLY("Aylıq")
}
