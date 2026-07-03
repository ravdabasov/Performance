# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Hilt / Dagger
-dontwarn com.google.errorprone.annotations.**

# Keep entity/model classes (Room reflection safety)
-keep class com.example.smarttaskmanager.data.local.entity.** { *; }
-keep class com.example.smarttaskmanager.domain.model.** { *; }
