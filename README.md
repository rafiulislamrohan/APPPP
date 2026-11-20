# VITALTRACK — Intelligent Personal Wellness & Activity Tracker

> **Move Better. Live Healthier. Understand Your Journey.**

VITALTRACK is an offline-first, privacy-focused Android wellness, activity, GPS mapping, health tracking, workout, habit, and optional Gemini AI coaching application built with **Kotlin**, **Jetpack Compose**, **Material 3**, **Room**, **DataStore**, **WorkManager**, **Health Connect**, **OSMDroid**, and **Firebase**.

---

## Key Features

### 1. Offline-First & Privacy-First Architecture
- **100% functional without internet or login**: hardware step counting, GPS route recording, workouts, hydration, sleep, weight/BMI, health journal, habits, calendar, and analytics are stored locally in encrypted-at-rest Android **Room** (`vitaltrack_offline.db`) and **Preferences DataStore**.
- Zero fabricated sensor measurements, zero fake heart-rate camera readings, and transparent offline notices whenever network connectivity is absent.

### 2. Five-Tab Adaptive Navigation & 29 Screens (with 3D Depth System)
- **Home**: Hero greeting with live gyroscope/accelerometer 3D parallax (`rememberSensorTilt3D`), extruded 3D step progress ring, 3D metric cards, 3D isometric 7-day bar chart, recent activity, and shortcuts to AI Coach, **AI Outfit & Dress Colors**, **3D Studio**, **Breath & Calc**, Tasks, Habits, Calendar, and Analytics.
- **Activity**: Unified GPS & workout session history, workout launcher, and offline 8-category Exercise Library (`Walking`, `Running`, `Cycling`, `Strength`, `Bodyweight`, `Stretching`, `Yoga`, `Indoor`), custom routine builder, guided workout timer, and rest countdowns.
- **Map**: Personal Map Timeline powered by **OSMDroid** (OpenStreetMap), interactive foreground-service GPS tracking (`Start`, `Pause`, `Resume`, `Stop & Save`), route polylines, route replay with waypoint scrubber, **GPX 1.1 route export**, and visible-region offline tile caching via `CacheManager`.
- **Health**: **Health Connect** sync (Android 9+ with graceful fallback), Heart Rate history (explicitly sourced from connected devices/Health Connect only), Sleep stage dashboard + manual sleep logger, Water tracker, Weight & BMI trend charts with 3D shaded nodes, Daily Health Journal (mood + 1–5 energy + notes), and medical non-diagnostic disclaimer.
- **Profile & Studios**: Habits & streaks (`14-day` completion strips), **Wellness Tasks & Due-Time Reminders** (Room `wellness_tasks` + `task_categories` persistence, real-time search bar, quick task presets, +1 day snooze, color-coded High/Medium/Low priorities, category/priority/status filtering, and WorkManager scheduled local notifications), **AI Style & Dress Colors** (live smart Gemini clothing & dress color combinations with interactive 360° 3D outfit mannequin preview), **3D Wellness & Body Studio** (interactive 3D Vitality Orb, 3D Body Model, and 3D GPS Route Terrain), **3D Breathing & Calculators** (Box/4-7-8/Recovery 3D sphere breathing, Heart-Rate Training Zones, Hydration Goal & TDEE calculators), Activity Calendar, Analytics & Challenges with CSV & PDF exports, AI Wellness Coach, Settings (Theme, **3D Depth & Gyro Parallax toggle**, English/Bengali `bn`, Metric/Imperial, optional Google Sign-In + Firestore sync, Wear OS bridge), and Privacy & Data Control.

### 3. Hardware, System & Cloud Integrations
- **Hardware Step Sensor (`StepSensorManager`)**: Uses `Sensor.TYPE_STEP_COUNTER` (persistent boot-delta tracking across restarts & midnight rollovers) with automatic fallback to `Sensor.TYPE_STEP_DETECTOR` and explicit unavailability reporting.
- **Background GPS Tracking (`ActivityTrackingForegroundService`)**: Android `foregroundServiceType="location"` service using `FusedLocationProviderClient` with persistent Pause/Resume/Stop notification actions.
- **Scheduled Task Notifications (`TaskReminderWorker` & `TaskReminderScheduler`)**: Uses **WorkManager** unique `OneTimeWorkRequest`s computed from each Room task's due date & time, cancelling automatically on completion/deletion and rescheduling after device reboot (`BootCompletedReceiver`).
- **Home Screen Widget (`VitalTrackWidget`)**: Jetpack Glance widget displaying today's steps and hydration total.
- **Optional Gemini AI Wellness Coach & Smart Style Advisor (`FirebaseAiCoachService`, `FirebaseStyleAdvisorService`)**: Powered by **Firebase AI Logic** (`Firebase.ai` with `gemini-3.8-flash` and App Check) — streams contextual, non-diagnostic wellness insights and generates structured color-matched clothing & dress combinations tailored to height, weight, build, and skin tone.
- **Optional Cloud Sync (`FirestoreSyncRepository`)**: Google Sign-In via Jetpack `CredentialManager` (`GetSignInWithGoogleOption`) + strict owner-scoped Firestore rules under `/users/{userId}/...`.

---

## Project Structure

- `app/src/main/java/com/example/VitalTrackApplication.kt` — Dependency graph, WorkManager scheduling, OSMDroid config, Room seeding
- `app/src/main/java/com/example/MainActivity.kt` — App Check init, edge-to-edge UI, runtime English/Bengali locale switching
- `app/src/main/java/com/example/data/local/` — Room entities, DAOs, migrations, preloaded exercises, Preferences DataStore
- `app/src/main/java/com/example/data/health/` — `StepSensorManager`, `HealthConnectManager`, `WellnessCalculators`
- `app/src/main/java/com/example/data/remote/` — `AppCheckInitializer`, `FirebaseAiCoachService`, `FirebaseStyleAdvisorService`, `FirestoreSyncRepository`, `FirestoreErrorHandler`
- `app/src/main/java/com/example/data/repository/` — `VitalTrackRepository`, `ExportBackupRepository`, `WearableSyncBridge`
- `app/src/main/java/com/example/service/` — `ActivityTrackingForegroundService`, `VitalTrackWorkers`, `ReminderScheduler`, `VitalTrackWidget`
- `app/src/main/java/com/example/ui/` — Material 3 theme/typography, 3D tactile components (`ThreeDComponents`), 3D charts, OSMDroid map view, `VitalTrackViewModel`, `VitalTrackNavHost`, and 29 screen composables

---

## Build & Verification Commands

- **Compile Kotlin & Resources / Build Debug APK**:
  ```bash
  gradle :app:assembleDebug
  ```
- **Run Unit & Robolectric Tests**:
  ```bash
  gradle :app:testDebugUnitTest
  ```
- **Run Firestore Security Rules Tests (with Firestore Emulator on `127.0.0.1:8085`)**:
  ```bash
  FIRESTORE_EMULATOR_HOST="127.0.0.1:8085" node --test firestore.test.js
  ```

---

## Medical & Accuracy Disclaimer
VITALTRACK is an informational wellness, fitness, and activity journal application. It is **not a medical diagnostic device** and does not treat, diagnose, or prevent any medical condition. Smartphone cameras and GPS receivers are never used as medical heart-rate monitors; heart-rate records come strictly from supported wearables or Health Connect integrations.
