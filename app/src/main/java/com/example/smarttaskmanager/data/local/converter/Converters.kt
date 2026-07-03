package com.example.smarttaskmanager.data.local.converter

import androidx.room.TypeConverter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Room, java.time.LocalDateTime tipini birbaşa saxlaya bilmir - ISO-8601 String-ə çeviririk.
 * Bu, həm oxunaqlı (debug zamanı), həm də SQLite text sıralaması ilə tam uyğundur
 * çünki ISO-8601 formatı leksikoqrafik olaraq da xronoloji sıraya uyğundur.
 */
class Converters {
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    @TypeConverter
    fun fromTimestamp(value: String?): LocalDateTime? =
        value?.let { LocalDateTime.parse(it, formatter) }

    @TypeConverter
    fun dateTimeToTimestamp(dateTime: LocalDateTime?): String? =
        dateTime?.format(formatter)
}
