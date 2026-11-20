package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.health.WellnessCalculators
import com.example.data.local.DailyStepEntity
import com.example.data.local.GpsActivitySessionEntity
import com.example.data.local.SeedExerciseLibrary
import com.example.data.local.UserPreferencesDataStore
import com.example.data.local.VitalTrackDatabase
import com.example.data.local.WaterIntakeEntity
import com.example.data.repository.ExportBackupRepository
import com.example.data.repository.WearableSyncBridge
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class VitalTrackDatabaseTest {

  private lateinit var context: Context
  private lateinit var db: VitalTrackDatabase
  private lateinit var prefs: UserPreferencesDataStore

  @Before
  fun setup() {
    context = ApplicationProvider.getApplicationContext()
    db = Room.inMemoryDatabaseBuilder(context, VitalTrackDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    prefs = UserPreferencesDataStore(context)
  }

  @After
  fun tearDown() {
    db.close()
  }

  @Test
  fun dailySteps_andWaterIntakes_persistAcrossQueries() = runTest {
    val today = WellnessCalculators.todayIso()
    db.dailyStepDao().upsert(
      DailyStepEntity(
        dateIso = today,
        steps = 6420,
        goal = 8000,
        distanceMeters = 4815.0,
        caloriesKcal = 256.8,
        activeMinutes = 64,
        source = "SENSOR_COUNTER",
      )
    )
    db.healthRecordDao().insertWater(
      WaterIntakeEntity(
        id = "w1",
        amountMl = 250,
        recordedAtMillis = 1000L,
        dateIso = today,
      )
    )
    db.healthRecordDao().insertWater(
      WaterIntakeEntity(
        id = "w2",
        amountMl = 500,
        recordedAtMillis = 2000L,
        dateIso = today,
      )
    )

    val stepRow = db.dailyStepDao().getStepsForDate(today)
    assertEquals(6420, stepRow?.steps)

    val waterTotal = db.healthRecordDao().observeWaterForDate(today).first().sumOf { it.amountMl }
    assertEquals(750, waterTotal)
  }

  @Test
  fun seedLibrary_andJsonBackupRestore_roundTripsDataSafely() = runTest {
    db.workoutDao().upsertExercises(SeedExerciseLibrary.defaultExercises)
    db.workoutDao().upsertWorkoutPlans(SeedExerciseLibrary.defaultPlans)
    db.gpsSessionDao().upsert(
      GpsActivitySessionEntity(
        id = "gps_1",
        activityType = "WALKING",
        title = "Evening Park Loop",
        startTimeMillis = 1_700_000_000_000L,
        endTimeMillis = 1_700_001_200_000L,
        activeDurationMillis = 1_200_000L,
        distanceMeters = 1950.0,
        avgSpeedMps = 1.62,
        caloriesKcal = 110.0,
        steps = 2500,
        dateIso = "2026-11-19",
        routePolylineJson = "[]",
      )
    )

    val backupRepo = ExportBackupRepository(context, db, prefs)
    val backupJson = backupRepo.createBackupJsonString()
    assertTrue(backupJson.contains("Evening Park Loop"))

    db.gpsSessionDao().clearAll()
    assertEquals(0, db.gpsSessionDao().getAllSnapshot().size)

    val restoreResult = backupRepo.restoreFromBackupJson(backupJson)
    assertTrue(restoreResult.isSuccess)
    assertEquals(1, db.gpsSessionDao().getAllSnapshot().size)
    assertEquals("Evening Park Loop", db.gpsSessionDao().getAllSnapshot().first().title)
  }

  @Test
  fun gpxExport_andWearOsSnapshot_generateValidFormats() = runTest {
    val session =
      GpsActivitySessionEntity(
        id = "gps_gpx_1",
        activityType = "RUNNING",
        title = "Morning <Trail> & Run",
        startTimeMillis = 1_700_000_000_000L,
        endTimeMillis = 1_700_000_300_000L,
        activeDurationMillis = 300_000L,
        distanceMeters = 850.0,
        avgSpeedMps = 2.83,
        caloriesKcal = 65.0,
        steps = 1100,
        dateIso = "2026-11-19",
        routePolylineJson =
          """[{"latitude":23.8103,"longitude":90.4125,"altitudeMeters":12.0,"timestampMillis":1700000000000},{"latitude":23.8150,"longitude":90.4180,"altitudeMeters":14.0,"timestampMillis":1700000150000}]""",
      )
    db.gpsSessionDao().upsert(session)

    val backupRepo = ExportBackupRepository(context, db, prefs)
    val gpxResult = backupRepo.exportGpsSessionGpx("gps_gpx_1")
    assertTrue(gpxResult.isSuccess)
    val gpxFile = java.io.File(context.cacheDir, "exports/vitaltrack_route_2026-11-19_gps_gpx_.gpx")
    assertTrue(gpxFile.exists())
    val gpxContent = gpxFile.readText()
    assertTrue(gpxContent.contains("<gpx version=\"1.1\""))
    assertTrue(gpxContent.contains("Morning &lt;Trail&gt; &amp; Run"))
    assertTrue(gpxContent.contains("lat=\"23.8103\" lon=\"90.4125\""))

    val wearBridge = WearableSyncBridge(context)
    val snapshotJson =
      wearBridge.buildDailySnapshotJson(
        dateIso = "2026-11-19",
        steps = 6400,
        stepGoal = 8000,
        waterMl = 1500,
        waterGoalMl = 2200,
        activeMinutes = 48,
      )
    assertTrue(snapshotJson.contains("\"steps\": 6400"))
    assertTrue(snapshotJson.contains("\"waterGoalMl\": 2200"))
  }

  @Test
  fun wellnessTaskDao_persistsTasksPrioritiesAndCategories_andSurvivesBackupRestore() = runTest {
    val now = System.currentTimeMillis()
    db.taskCategoryDao().upsertCategories(SeedExerciseLibrary.defaultTaskCategories)
    val customCategory =
      com.example.data.local.TaskCategoryEntity(
        id = "cat_nutrition",
        name = "NUTRITION",
        isDefault = false,
        createdAtMillis = now,
      )
    db.taskCategoryDao().upsertCategory(customCategory)
    assertEquals(6, db.taskCategoryDao().getCategoriesSnapshot().size)

    val lowPriority =
      com.example.data.local.WellnessTaskEntity(
        id = "task_low",
        title = "Restock Water Filter",
        category = "HYDRATION",
        priority = com.example.data.local.TaskPriority.LOW.storageKey,
        dueAtMillis = now + 1_800_000L,
        reminderEnabled = false,
        isCompleted = false,
      )
    val highPriority =
      com.example.data.local.WellnessTaskEntity(
        id = "task_high",
        title = "Evening Mobility Stretch",
        notes = "10 min hip & shoulder routine",
        category = "NUTRITION",
        priority = com.example.data.local.TaskPriority.HIGH.storageKey,
        dueAtMillis = now + 7_200_000L,
        reminderEnabled = true,
        isCompleted = false,
      )
    val completedTask =
      com.example.data.local.WellnessTaskEntity(
        id = "task_done",
        title = "Morning Weigh-In",
        category = "WELLNESS",
        priority = com.example.data.local.TaskPriority.MEDIUM.storageKey,
        dueAtMillis = now - 3_600_000L,
        reminderEnabled = true,
        isCompleted = true,
        completedAtMillis = now - 1_800_000L,
      )

    db.wellnessTaskDao().upsertTasks(listOf(lowPriority, highPriority, completedTask))

    // Verify HIGH priority sorts ahead of LOW priority even when due later, and completed last
    val ordered = db.wellnessTaskDao().observeAllTasks().first()
    assertEquals(listOf("task_high", "task_low", "task_done"), ordered.map { it.id })

    // Verify category-filtered query
    val nutritionTasks = db.wellnessTaskDao().observeTasksByCategory("NUTRITION").first()
    assertEquals(listOf("task_high"), nutritionTasks.map { it.id })

    // Verify searchTasks matches both title and notes
    val titleMatches = db.wellnessTaskDao().searchTasks("Filter").first()
    assertEquals(listOf("task_low"), titleMatches.map { it.id })
    val noteMatches = db.wellnessTaskDao().searchTasks("shoulder routine").first()
    assertEquals(listOf("task_high"), noteMatches.map { it.id })

    val pendingAlerts = db.wellnessTaskDao().getPendingReminderTasksSnapshot()
    assertEquals(listOf("task_high"), pendingAlerts.map { it.id })

    // Default categories cannot be deleted via deleteCustomCategoryById, custom ones can
    db.taskCategoryDao().deleteCustomCategoryById("cat_wellness")
    assertEquals(6, db.taskCategoryDao().getCategoriesSnapshot().size)

    // Backup -> clear -> restore round-trip
    val backupRepo = ExportBackupRepository(context, db, prefs)
    val json = backupRepo.createBackupJsonString()
    assertTrue(json.contains("Evening Mobility Stretch"))
    assertTrue(json.contains("NUTRITION"))

    db.wellnessTaskDao().clearAll()
    db.taskCategoryDao().clearAll()
    assertEquals(0, db.wellnessTaskDao().getAllTasksSnapshot().size)
    assertEquals(0, db.taskCategoryDao().getCategoriesSnapshot().size)

    assertTrue(backupRepo.restoreFromBackupJson(json).isSuccess)
    assertEquals(3, db.wellnessTaskDao().getAllTasksSnapshot().size)
    assertEquals(6, db.taskCategoryDao().getCategoriesSnapshot().size)
    assertEquals("HIGH", db.wellnessTaskDao().getTaskById("task_high")?.priority)

    db.wellnessTaskDao().deleteTaskById("task_low")
    assertEquals(2, db.wellnessTaskDao().getAllTasksSnapshot().size)
  }

  @Test
  fun migration2To3And3To4_preserveTasksAddPriorityAndSeedCategories() = runTest {
    val dbFile = context.getDatabasePath("migration_test_v2_v4.db")
    if (dbFile.exists()) dbFile.delete()

    val openHelper =
      androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory().create(
        androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(context)
          .name(dbFile.name)
          .callback(
            object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(2) {
              override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL(
                  "CREATE TABLE IF NOT EXISTS daily_steps (dateIso TEXT NOT NULL PRIMARY KEY, steps INTEGER NOT NULL, goal INTEGER NOT NULL, distanceMeters REAL NOT NULL, caloriesKcal REAL NOT NULL, activeMinutes INTEGER NOT NULL, source TEXT NOT NULL, updatedAtMillis INTEGER NOT NULL)"
                )
              }

              override fun onUpgrade(
                db: androidx.sqlite.db.SupportSQLiteDatabase,
                oldVersion: Int,
                newVersion: Int,
              ) = Unit
            }
          )
          .build()
      )
    val rawDb = openHelper.writableDatabase
    VitalTrackDatabase.MIGRATION_2_3.migrate(rawDb)
    rawDb.execSQL(
      "INSERT INTO wellness_tasks (id, title, notes, category, dueAtMillis, reminderEnabled, isCompleted, completedAtMillis, createdAtMillis, updatedAtMillis) VALUES ('m1', 'Migrated Task', '', 'GENERAL', 1800000000000, 1, 0, NULL, 1700000000000, 1700000000000)"
    )

    VitalTrackDatabase.MIGRATION_3_4.migrate(rawDb)

    rawDb.query("SELECT title, priority FROM wellness_tasks WHERE id = 'm1'").use { cursor ->
      assertTrue(cursor.moveToFirst())
      assertEquals("Migrated Task", cursor.getString(0))
      assertEquals("MEDIUM", cursor.getString(1))
    }
    rawDb.query("SELECT COUNT(*) FROM task_categories").use { cursor ->
      assertTrue(cursor.moveToFirst())
      assertEquals(5, cursor.getInt(0))
    }
    rawDb.close()
    openHelper.close()
    dbFile.delete()
  }

  @Test
  fun taskReminderScheduler_enqueuesUpdatesAndCancelsUniqueWorkRequests() {
    runCatching {
      androidx.work.WorkManager.initialize(
        context,
        androidx.work.Configuration.Builder().build(),
      )
    }
    val workManager = androidx.work.WorkManager.getInstance(context)
    val now = System.currentTimeMillis()
    val task =
      com.example.data.local.WellnessTaskEntity(
        id = "wm_task_1",
        title = "Evening Vitamin",
        category = "MEDICATION",
        dueAtMillis = now + 900_000L,
        reminderEnabled = true,
        isCompleted = false,
      )

    com.example.service.TaskReminderScheduler.scheduleWithWorkManager(workManager, task, now)
    val workName = com.example.service.TaskReminderScheduler.uniqueWorkName(task.id)
    val enqueuedInfos = workManager.getWorkInfosForUniqueWork(workName).get()
    assertEquals(1, enqueuedInfos.size)
    assertEquals(androidx.work.WorkInfo.State.ENQUEUED, enqueuedInfos.first().state)
    assertTrue(
      enqueuedInfos.first().tags.contains(com.example.service.TaskReminderScheduler.TAG_TASK_REMINDERS)
    )

    // Completing task cancels scheduled reminder work
    com.example.service.TaskReminderScheduler.scheduleWithWorkManager(
      workManager,
      task.copy(isCompleted = true, completedAtMillis = now),
      now,
    )
    val cancelledInfos = workManager.getWorkInfosForUniqueWork(workName).get()
    assertEquals(androidx.work.WorkInfo.State.CANCELLED, cancelledInfos.first().state)
  }

  @Test
  fun migration4To5_andNewDaos_persistWardrobeNutritionVitalsAndGear() = runTest {
    val dbFile = context.getDatabasePath("migration_test_v4_v5.db")
    if (dbFile.exists()) dbFile.delete()

    val openHelper =
      androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory().create(
        androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(context)
          .name(dbFile.name)
          .callback(
            object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(4) {
              override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL(
                  "CREATE TABLE IF NOT EXISTS daily_steps (dateIso TEXT NOT NULL PRIMARY KEY, steps INTEGER NOT NULL, goal INTEGER NOT NULL, distanceMeters REAL NOT NULL, caloriesKcal REAL NOT NULL, activeMinutes INTEGER NOT NULL, source TEXT NOT NULL, updatedAtMillis INTEGER NOT NULL)"
                )
              }

              override fun onUpgrade(
                db: androidx.sqlite.db.SupportSQLiteDatabase,
                oldVersion: Int,
                newVersion: Int,
              ) = Unit
            }
          )
          .build()
      )
    val rawDb = openHelper.writableDatabase
    VitalTrackDatabase.MIGRATION_2_3.migrate(rawDb)
    VitalTrackDatabase.MIGRATION_3_4.migrate(rawDb)
    rawDb.execSQL(
      "CREATE TABLE IF NOT EXISTS habits (id TEXT NOT NULL PRIMARY KEY, title TEXT NOT NULL, category TEXT NOT NULL, targetDescription TEXT NOT NULL, reminderTime TEXT NOT NULL, createdAtMillis INTEGER NOT NULL)"
    )
    VitalTrackDatabase.MIGRATION_4_5.migrate(rawDb)

    rawDb.query("SELECT recurrenceRule, focusMinutesLogged FROM wellness_tasks").use { cursor ->
      assertEquals(0, cursor.count)
    }
    rawDb.query("SELECT COUNT(*) FROM wardrobe_items").use { cursor ->
      assertTrue(cursor.moveToFirst())
      assertEquals(0, cursor.getInt(0))
    }
    rawDb.close()
    openHelper.close()
    dbFile.delete()

    // In-memory DAO persistence verification for Room v5 tables
    db.wardrobeDao().upsertItem(
      com.example.data.local.WardrobeItemEntity(
        id = "w_item_1",
        name = "Olive Linen Blazer",
        category = "OUTERWEAR",
        colorName = "Forest Olive",
        colorHex = "#4A5D4E",
        season = "All-Season",
        occasion = "Smart Casual",
      )
    )
    db.nutritionDao().upsertLog(
      com.example.data.local.NutritionLogEntity(
        id = "n_log_1",
        dateIso = "2026-11-20",
        mealName = "Salmon & Quinoa",
        mealType = "LUNCH",
        caloriesKcal = 620,
        proteinG = 42.0,
        carbsG = 48.0,
        fatG = 24.0,
      )
    )
    db.vitalsDao().upsertRecord(
      com.example.data.local.VitalsRecordEntity(
        id = "v_bp_1",
        type = "BP",
        title = "Morning BP",
        primaryValue = 118.0,
        secondaryValue = 76.0,
        unit = "mmHg",
      )
    )
    db.gearDao().upsertGear(
      com.example.data.local.GearItemEntity(
        id = "g_shoe_1",
        name = "Pegasus 41",
        brand = "Nike",
        distanceMeters = 12_000.0,
        retireThresholdKm = 650.0,
      )
    )

    assertEquals(1, db.wardrobeDao().getAllSnapshot().size)
    assertEquals(1, db.nutritionDao().getAllSnapshot().size)
    assertEquals(1, db.vitalsDao().getAllSnapshot().size)
    assertEquals(1, db.gearDao().getAllSnapshot().size)
  }
}
