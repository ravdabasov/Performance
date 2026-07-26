# Smart Task Manager — Android (Kotlin + Jetpack Compose)

Offline-first, tam funksional gündəlik tapşırıq idarəetmə tətbiqi. Login yoxdur, bütün data
cihazda (Room) saxlanılır, internet tələb olunmur.

---

## 1. Ümumi arxitektura

**MVVM + Clean Architecture (3 qat) + SOLID**

```
UI (Compose) → ViewModel → UseCase → Repository (interface) → RepositoryImpl → Room DAO
                                            ↑
                                   domain/model/Task (təmiz model)
```

- **presentation/** — Compose ekranları, ViewModel-lər, UI state-lər. Yalnız domain modelini görür.
- **domain/** — model, repository *interfeysləri*, use case-lər. Heç bir Android/Room importu yoxdur
  (test edilməsi asan, framework-dən asılı deyil).
- **data/** — Room Entity, DAO, Database, Repository implementasiyası, Entity↔Domain mapper.
- **notification/** — AlarmManager + WorkManager + BroadcastReceiver-lər (bildiriş sistemi).
- **di/** — Hilt modulları (Dependency Inversion-ı bağlayır: interfeys → implementasiya).

SOLID tətbiqi:
- **S**: hər use case tək iş görür (CreateTaskUseCase, DeleteTaskUseCase, ...).
- **O/L**: `TaskFilter` sealed class üzərindən yeni filter tipi əlavə etmək mövcud kodu pozmur.
- **I**: `TaskRepository` və `TaskNotificationScheduler` ayrı, kiçik interfeyslərdir.
- **D**: ViewModel-lər Room-u yox, `TaskRepository` interfeysini tanıyır (Hilt `@Binds` ilə bağlanır).

---

## 2. Qovluq strukturu

```
app/src/main/java/com/example/smarttaskmanager/
 ├─ SmartTaskManagerApp.kt        (Hilt + WorkManager konfiqurasiyası)
 ├─ MainActivity.kt               (Bottom Nav + FAB host)
 ├─ data/
 │   ├─ local/entity/TaskEntity.kt, TaskMapper.kt
 │   ├─ local/dao/TaskDao.kt
 │   ├─ local/converter/Converters.kt
 │   ├─ local/AppDatabase.kt
 │   └─ repository/TaskRepositoryImpl.kt
 ├─ domain/
 │   ├─ model/ (Task, TaskStatus, TaskFilter, NotificationTiming)
 │   ├─ repository/ (TaskRepository, TaskNotificationScheduler — interfeyslər)
 │   └─ usecase/ (Create/Update/Delete/Complete/Search/Filter/Dashboard/Notification)
 ├─ notification/
 │   ├─ NotificationHelper.kt, AlarmSchedulerImpl.kt
 │   ├─ TaskAlarmReceiver.kt, BootReceiver.kt, RescheduleAllAlarmsWorker.kt
 ├─ di/ (DatabaseModule, RepositoryModule)
 ├─ presentation/
 │   ├─ navigation/NavGraph.kt
 │   ├─ viewmodel/ (Dashboard, TaskList, AddEditTask, CompletedTasks)
 │   ├─ screen/ (dashboard, tasklist, addedit, completed)
 │   ├─ components/ (TaskCard, SearchBar, FilterChips, StatCard)
 │   └─ theme/ (Color, Type, Theme — Material 3, Dynamic Color, Dark/Light)
 └─ util/TaskUiUtils.kt
```

---

## 3. Database modeli

**Room, tək cədvəl: `tasks`.** `deadline`, `status`, `category` üzərində index var ki, Smart
Prioritetləşdirmə sorğusu (`ORDER BY deadline ASC, priority DESC, createdAt ASC`) böyük data
həcmində də sürətli qalsın. `LocalDateTime` ISO-8601 string kimi saxlanılır (`Converters.kt`).

## 4. Smart Prioritetləşdirmə alqoritmi

`TaskDao.getActiveTasksSmartSorted()`:
```sql
SELECT * FROM tasks
WHERE status NOT IN ('COMPLETED','CANCELLED')
ORDER BY deadline ASC, priority DESC, createdAt ASC
```
Yəni: 1) deadline-a ən yaxın → 2) prioritet ↓ → 3) əvvəl yaradılan. `Task.isOverdue` /
`effectiveStatus` real-time overdue vəziyyətini UI-də dərhal göstərir, DB-dəki status isə
`refreshOverdueStatuses()` ilə (Dashboard/List açılışında və boot-worker-də) sinxronlaşdırılır.

## 5. Bildiriş sistemi

- **AlarmManager** (`AlarmSchedulerImpl`) hər task üçün seçilən hər `NotificationTiming`-i
  (deadline-dan əvvəl/zamanı/sonra) ayrıca `setExactAndAllowWhileIdle` alarmı kimi planlaşdırır.
- **TaskAlarmReceiver** alarm tetiklənəndə bildirişi göstərir (`NotificationHelper`).
- **BootReceiver** + **WorkManager** (`RescheduleAllAlarmsWorker`, `@HiltWorker`) telefon
  restart olduqda bütün aktiv tasklar üçün alarmları DB-dən oxuyub yenidən qurur — bildirişlər
  itmir.

## 6. Naviqasiya

Bottom Navigation: **Ana səhifə (Dashboard) → Tasklar (List+Search+Filter) → Tamamlanan**.
FAB hər zaman "Yeni task" ekranına aparır. Edit üçün `edit_task/{taskId}` route-u eyni formanı
(`AddEditTaskScreen`) doldurulmuş vəziyyətdə açır.

## 7. Təhlükəsizlik və keyfiyyət

- **Input validation**: `CreateTaskUseCase`/`UpdateTaskUseCase` başlıq boşluğu, uzunluq,
  prioritet aralığı (1–10), deadline < tarix yoxlamalarını edir və `TaskValidationResult`
  sealed class ilə xəta mesajını UI-ə ötürür (heç bir crash riski yoxdur).
- **Null safety**: bütün model sahələri Kotlin non-null, opsional sahələr `?`/default dəyərlərlə.
- Room + Hilt sayəsində boilerplate minimaldır, kod modul-modul genişlənə bilər (yeni ekran =
  yeni use case + yeni ViewModel + yeni composable, mövcud qatlara toxunmadan).

---

## 8. Build təlimatı (Android Studio)

1. Android Studio Koala/Ladybug (və ya daha yeni) açın → **Open** → bu qovluğu seçin.
2. Gradle sync avtomatik başlayacaq (Kotlin 1.9.24, AGP 8.5.0, compileSdk 34, minSdk 26).
3. `local.properties` faylını yaradın (Android Studio bunu avtomatik edir) və SDK yolunu göstərin
   — nümunə `local.properties.example` faylındadır.
4. Run konfiqurasiyasında `app` modulunu seçib ▶ düyməsini basın (emulator və ya real cihaz).

## 9. APK yaratmaq

**Debug APK (sürətli test üçün):**
```bash
./gradlew assembleDebug
# Nəticə: app/build/outputs/apk/debug/app-debug.apk
```

**Release APK (imzalanmış, Play Store-a hazır):**
1. `Build → Generate Signed Bundle / APK` → APK seçin.
2. Yeni keystore yaradın (və ya mövcud olanı seçin), parolları daxil edin.
3. `release` build type seçib Finish edin.
4. Nəticə: `app/build/outputs/apk/release/app-release.apk`.

Terminal ilə (keystore artıq varsa, `key.properties` vasitəsilə `app/build.gradle.kts`-ə
`signingConfigs` əlavə edilməlidir):
```bash
./gradlew assembleRelease
```

---

## 10. Əlavə təklif olunan optimallaşdırmalar (artıq tətbiq olunub)

- Dynamic Color (Material You) Android 12+ üçün avtomatik aktivdir.
- `Index` annotasiyaları ilə DB sorğu performansı optimallaşdırılıb.
- `StateFlow` + `WhileSubscribed(5000)` ilə lazımsız ekran arxası hesablamalar dayandırılır
  (batareya qənaəti).
- Notification kanalı `IMPORTANCE_HIGH` ilə yaradılıb ki, deadline bildirişləri gözə çarpsın.
