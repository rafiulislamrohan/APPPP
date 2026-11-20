package com.example.data.repository

import android.content.Context
import com.example.data.health.HealthConnectAvailability
import com.example.data.health.HealthConnectManager
import com.example.data.health.StepSensorManager
import com.example.data.health.StepSensorStatus
import com.example.data.health.WellnessCalculators
import com.example.data.local.AiCoachMessageEntity
import com.example.data.local.DailyStepEntity
import com.example.data.local.ExerciseEntity
import com.example.data.local.GearItemEntity
import com.example.data.local.GpsActivitySessionEntity
import com.example.data.local.HabitCompletionEntity
import com.example.data.local.HabitEntity
import com.example.data.local.HeartRateRecordEntity
import com.example.data.local.JournalEntryEntity
import com.example.data.local.NotificationConfigEntity
import com.example.data.local.NutritionLogEntity
import com.example.data.local.SeedExerciseLibrary
import com.example.data.local.SleepRecordEntity
import com.example.data.local.SyncStatus
import com.example.data.local.TaskCategoryEntity
import com.example.data.local.TaskPriority
import com.example.data.local.TaskRecurrence
import com.example.data.local.UserPreferencesDataStore
import com.example.data.local.UserProfilePreferences
import com.example.data.local.VitalTrackDatabase
import com.example.data.local.VitalsRecordEntity
import com.example.data.local.WardrobeItemEntity
import com.example.data.local.WaterIntakeEntity
import com.example.data.local.WeightEntryEntity
import com.example.data.local.WellnessTaskEntity
import com.example.data.local.WorkoutPlanEntity
import com.example.data.local.WorkoutSessionEntity
import com.example.data.remote.AiCoachService
import com.example.data.remote.AiStreamChunk
import com.example.data.remote.CloudSyncSummary
import com.example.data.remote.FirebaseAiCoachService
import com.example.data.remote.FirestoreSyncRepository
import com.example.data.remote.StyleAdvisorService
import com.example.data.remote.StyleProfileInput
import com.example.data.remote.StyleRecommendationResult
import com.example.service.ReminderScheduler
import com.example.service.TaskReminderScheduler
import com.example.service.VitalTrackWidgetUpdater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.util.UUID

class VitalTrackRepository(
  private val context: Context,
  val db: VitalTrackDatabase,
  val prefs: UserPreferencesDataStore,
  val stepSensorManager: StepSensorManager,
  val healthConnectManager: HealthConnectManager,
  val aiCoachService: AiCoachService,
  val styleAdvisorService: StyleAdvisorService,
  val firestoreSyncRepository: FirestoreSyncRepository,
  val exportBackupRepository: ExportBackupRepository,
  val wearableSyncBridge: WearableSyncBridge,
) {
  private val json = Json { ignoreUnknownKeys = true }

  val preferencesFlow: Flow<UserProfilePreferences> = prefs.preferencesFlow
  val sensorStatusFlow: StateFlow<StepSensorStatus> = stepSensorManager.sensorStatus

  val allDailyStepsFlow: Flow<List<DailyStepEntity>> = db.dailyStepDao().observeAllSteps()
  val gpsSessionsFlow: Flow<List<GpsActivitySessionEntity>> = db.gpsSessionDao().observeAllSessions()
  val heartRatesFlow: Flow<List<HeartRateRecordEntity>> = db.healthRecordDao().observeHeartRates()
  val sleepRecordsFlow: Flow<List<SleepRecordEntity>> = db.healthRecordDao().observeSleepRecords()
  val waterIntakesFlow: Flow<List<WaterIntakeEntity>> = db.healthRecordDao().observeAllWater()
  val weightEntriesFlow: Flow<List<WeightEntryEntity>> = db.healthRecordDao().observeWeightEntries()
  val journalEntriesFlow: Flow<List<JournalEntryEntity>> = db.healthRecordDao().observeJournalEntries()
  val exercisesFlow: Flow<List<ExerciseEntity>> = db.workoutDao().observeExercises()
  val workoutPlansFlow: Flow<List<WorkoutPlanEntity>> = db.workoutDao().observeWorkoutPlans()
  val workoutSessionsFlow: Flow<List<WorkoutSessionEntity>> = db.workoutDao().observeWorkoutSessions()
  val habitsFlow: Flow<List<HabitEntity>> = db.habitDao().observeActiveHabits()
  val habitCompletionsFlow: Flow<List<HabitCompletionEntity>> = db.habitDao().observeAllCompletions()
  val aiMessagesFlow: Flow<List<AiCoachMessageEntity>> = db.aiAndNotificationDao().observeMessages()
  val notificationConfigsFlow: Flow<List<NotificationConfigEntity>> =
    db.aiAndNotificationDao().observeNotificationConfigs()
  val wellnessTasksFlow: Flow<List<WellnessTaskEntity>> =
    db.wellnessTaskDao().observeAllTasks()
  val taskCategoriesFlow: Flow<List<TaskCategoryEntity>> =
    db.taskCategoryDao().observeCategories()
  val wardrobeItemsFlow: Flow<List<WardrobeItemEntity>> =
    db.wardrobeDao().observeAll()
  val nutritionLogsFlow: Flow<List<NutritionLogEntity>> =
    db.nutritionDao().observeAll()
  val vitalsRecordsFlow: Flow<List<VitalsRecordEntity>> =
    db.vitalsDao().observeAll()
  val gearItemsFlow: Flow<List<GearItemEntity>> =
    db.gearDao().observeAll()

  fun observeGpsSession(id: String): Flow<GpsActivitySessionEntity?> =
    db.gpsSessionDao().observeSessionById(id)

  fun observeWorkoutPlan(id: String): Flow<WorkoutPlanEntity?> =
    db.workoutDao().observeWorkoutPlanById(id)

  suspend fun ensureSeeded() =
    withContext(Dispatchers.IO) {
      if (db.workoutDao().exerciseCount() == 0) {
        db.workoutDao().upsertExercises(SeedExerciseLibrary.defaultExercises)
        db.workoutDao().upsertWorkoutPlans(SeedExerciseLibrary.defaultPlans)
      }
      if (db.habitDao().habitCount() == 0) {
        db.habitDao().upsertHabits(SeedExerciseLibrary.defaultHabits)
      }
      if (db.aiAndNotificationDao().getNotificationConfigsSnapshot().isEmpty()) {
        db.aiAndNotificationDao().upsertNotificationConfigs(SeedExerciseLibrary.defaultNotifications)
      }
      if (db.taskCategoryDao().categoryCount() == 0) {
        db.taskCategoryDao().upsertCategories(SeedExerciseLibrary.defaultTaskCategories)
      }
      stepSensorManager.ensureTodayRowExists()
    }

  suspend fun addWater(amountMl: Int) {
    val today = WellnessCalculators.todayIso()
    db.healthRecordDao()
      .insertWater(
        WaterIntakeEntity(
          id = UUID.randomUUID().toString(),
          amountMl = amountMl.coerceIn(10, 3000),
          recordedAtMillis = System.currentTimeMillis(),
          dateIso = today,
          syncStatus = SyncStatus.PENDING_SYNC,
        )
      )
    prefs.addWellnessXp(10)
    VitalTrackWidgetUpdater.refreshAll(context)
  }

  suspend fun deleteWater(id: String) {
    db.healthRecordDao().deleteWaterById(id)
    VitalTrackWidgetUpdater.refreshAll(context)
  }

  suspend fun addWeight(weightKg: Double, notes: String = "") {
    val profile = prefs.getCurrent()
    val bmi = WellnessCalculators.calculateBmi(weightKg, profile.heightCm)
    val now = System.currentTimeMillis()
    db.healthRecordDao()
      .insertWeight(
        WeightEntryEntity(
          id = UUID.randomUUID().toString(),
          weightKg = weightKg.coerceIn(25.0, 250.0),
          bmi = bmi,
          recordedAtMillis = now,
          dateIso = WellnessCalculators.todayIso(),
          source = "MANUAL",
          notes = notes.trim(),
          updatedAtMillis = now,
          syncStatus = SyncStatus.PENDING_SYNC,
        )
      )
    prefs.saveProfile(
      displayName = profile.displayName,
      age = profile.age,
      heightCm = profile.heightCm,
      weightKg = weightKg,
      strideLengthMeters = profile.strideLengthMeters,
      dailyStepGoal = profile.dailyStepGoal,
      dailyWaterGoalMl = profile.dailyWaterGoalMl,
      dailySleepGoalMinutes = profile.dailySleepGoalMinutes,
      dailyWorkoutGoalMinutes = profile.dailyWorkoutGoalMinutes,
      useImperialUnits = profile.useImperialUnits,
      languageCode = profile.languageCode,
      emergencyContactName = profile.emergencyContactName,
      emergencyContactPhone = profile.emergencyContactPhone,
      markCompleted = profile.profileSetupCompleted,
    )
  }

  suspend fun addManualSleep(
    durationMinutes: Int,
    qualityScore: Int,
    notes: String = "",
  ) {
    val end = System.currentTimeMillis()
    val dur = durationMinutes.coerceIn(30, 960)
    val start = end - dur * 60_000L
    db.healthRecordDao()
      .upsertSleep(
        SleepRecordEntity(
          id = UUID.randomUUID().toString(),
          startTimeMillis = start,
          endTimeMillis = end,
          durationMinutes = dur,
          deepMinutes = (dur * 0.22).toInt(),
          lightMinutes = (dur * 0.55).toInt(),
          remMinutes = (dur * 0.18).toInt(),
          awakeMinutes = (dur * 0.05).toInt(),
          qualityScore = qualityScore.coerceIn(1, 100),
          dateIso = WellnessCalculators.todayIso(),
          source = "MANUAL",
          notes = notes.trim(),
          updatedAtMillis = end,
          syncStatus = SyncStatus.PENDING_SYNC,
        )
      )
  }

  suspend fun addJournalEntry(mood: String, energyLevel: Int, note: String) {
    val now = System.currentTimeMillis()
    db.healthRecordDao()
      .upsertJournal(
        JournalEntryEntity(
          id = UUID.randomUUID().toString(),
          dateIso = WellnessCalculators.todayIso(),
          mood = mood,
          energyLevel = energyLevel.coerceIn(1, 5),
          note = note.trim(),
          createdAtMillis = now,
          updatedAtMillis = now,
          syncStatus = SyncStatus.PENDING_SYNC,
        )
      )
  }

  suspend fun deleteGpsSession(id: String) {
    db.gpsSessionDao().deleteById(id)
  }

  suspend fun createCustomWorkoutPlan(
    title: String,
    category: String,
    description: String,
    exerciseIds: List<String>,
  ) {
    val now = System.currentTimeMillis()
    val estMin = (exerciseIds.size * 5).coerceAtLeast(10)
    db.workoutDao()
      .upsertWorkoutPlan(
        WorkoutPlanEntity(
          id = "custom_${UUID.randomUUID()}",
          title = title.trim().ifEmpty { "Custom Routine" },
          category = category,
          description = description.trim().ifEmpty { "Personalized workout plan" },
          exerciseIdsJson = json.encodeToString(exerciseIds),
          estimatedMinutes = estMin,
          isCustom = true,
          createdAtMillis = now,
          updatedAtMillis = now,
          syncStatus = SyncStatus.PENDING_SYNC,
        )
      )
  }

  suspend fun saveWorkoutSession(
    plan: WorkoutPlanEntity,
    durationSeconds: Int,
    completedSets: Int,
    totalReps: Int,
    notes: String = "",
  ) {
    val profile = prefs.getCurrent()
    val now = System.currentTimeMillis()
    val met =
      when (plan.category) {
        "RUNNING" -> 9.0
        "CYCLING" -> 6.8
        "STRENGTH" -> 5.0
        "BODYWEIGHT" -> 4.5
        "YOGA", "STRETCHING" -> 2.8
        else -> 4.2
      }
    val kcal = WellnessCalculators.estimateActivityCalories(met, profile.weightKg, durationSeconds * 1000L)
    db.workoutDao()
      .insertWorkoutSession(
        WorkoutSessionEntity(
          id = UUID.randomUUID().toString(),
          workoutPlanId = plan.id,
          title = plan.title,
          category = plan.category,
          dateIso = WellnessCalculators.todayIso(),
          startTimeMillis = now - durationSeconds * 1000L,
          endTimeMillis = now,
          durationSeconds = durationSeconds,
          completedSets = completedSets,
          totalReps = totalReps,
          caloriesKcal = kcal,
          notes = notes,
          updatedAtMillis = now,
          syncStatus = SyncStatus.PENDING_SYNC,
        )
      )
  }

  suspend fun addHabit(
    title: String,
    category: String,
    targetDescription: String,
    reminderTime: String,
    frequencyType: String = "DAILY",
    stackChainGroup: String = "",
    habitPolarity: String = "BUILD",
  ) {
    val now = System.currentTimeMillis()
    db.habitDao()
      .upsertHabit(
        HabitEntity(
          id = "habit_${UUID.randomUUID()}",
          title = title.trim(),
          category = category,
          targetDescription = targetDescription.trim(),
          reminderTime = reminderTime.trim(),
          frequencyType = frequencyType,
          stackChainGroup = stackChainGroup.trim(),
          habitPolarity = habitPolarity,
          createdAtMillis = now,
          updatedAtMillis = now,
          syncStatus = SyncStatus.PENDING_SYNC,
        )
      )
  }

  suspend fun toggleHabitCompletion(habitId: String, dateIso: String = WellnessCalculators.todayIso()) {
    val id = "$habitId|$dateIso"
    val existing = db.habitDao().getCompletionsSnapshot().any { it.id == id }
    if (existing) {
      db.habitDao().deleteCompletionById(id)
    } else {
      db.habitDao()
        .insertCompletion(
          HabitCompletionEntity(
            id = id,
            habitId = habitId,
            dateIso = dateIso,
            completedAtMillis = System.currentTimeMillis(),
            syncStatus = SyncStatus.PENDING_SYNC,
          )
        )
      prefs.addWellnessXp(25)
    }
  }

  suspend fun deleteHabit(habitId: String) {
    db.habitDao().deleteHabitById(habitId)
  }

  suspend fun updateNotificationConfig(config: NotificationConfigEntity) {
    val updated = config.copy(updatedAtMillis = System.currentTimeMillis())
    db.aiAndNotificationDao().upsertNotificationConfig(updated)
    ReminderScheduler.scheduleConfig(context, updated)
  }

  // --- Wellness Tasks (Room + WorkManager Local Notifications) ---
  suspend fun addTaskCategory(rawName: String): TaskCategoryEntity {
    val clean = rawName.trim().uppercase().take(24)
    val now = System.currentTimeMillis()
    val existing = db.taskCategoryDao().getCategoriesSnapshot().firstOrNull {
      it.name.equals(clean, ignoreCase = true)
    }
    if (existing != null) return existing
    val created =
      TaskCategoryEntity(
        id = "cat_${UUID.randomUUID()}",
        name = clean.ifEmpty { "CUSTOM" },
        isDefault = false,
        createdAtMillis = now,
      )
    db.taskCategoryDao().upsertCategory(created)
    return created
  }

  suspend fun deleteCustomTaskCategory(categoryId: String) {
    db.taskCategoryDao().deleteCustomCategoryById(categoryId)
  }

  suspend fun saveWellnessTask(
    existingId: String?,
    title: String,
    notes: String,
    category: String,
    priority: String = TaskPriority.MEDIUM.storageKey,
    dueAtMillis: Long,
    reminderEnabled: Boolean,
    recurrenceRule: String = TaskRecurrence.NONE.storageKey,
    subtasksJson: String = "[]",
    geofenceLabel: String = "",
    autoEscalatePriority: Boolean = false,
  ): WellnessTaskEntity {
    val now = System.currentTimeMillis()
    val cleanCategory = category.trim().uppercase().ifEmpty { "WELLNESS" }
    val cleanPriority = TaskPriority.fromKey(priority).storageKey
    val cleanRecurrence = TaskRecurrence.fromKey(recurrenceRule).storageKey
    val existing = existingId?.let { db.wellnessTaskDao().getTaskById(it) }
    val task =
      if (existing != null) {
        existing.copy(
          title = title.trim(),
          notes = notes.trim(),
          category = cleanCategory,
          priority = cleanPriority,
          dueAtMillis = dueAtMillis,
          reminderEnabled = reminderEnabled,
          recurrenceRule = cleanRecurrence,
          subtasksJson = subtasksJson,
          geofenceLabel = geofenceLabel.trim(),
          autoEscalatePriority = autoEscalatePriority,
          updatedAtMillis = now,
        )
      } else {
        WellnessTaskEntity(
          id = "task_${UUID.randomUUID()}",
          title = title.trim(),
          notes = notes.trim(),
          category = cleanCategory,
          priority = cleanPriority,
          dueAtMillis = dueAtMillis,
          reminderEnabled = reminderEnabled,
          isCompleted = false,
          completedAtMillis = null,
          recurrenceRule = cleanRecurrence,
          subtasksJson = subtasksJson,
          geofenceLabel = geofenceLabel.trim(),
          autoEscalatePriority = autoEscalatePriority,
          createdAtMillis = now,
          updatedAtMillis = now,
        )
      }
    db.wellnessTaskDao().upsertTask(task)
    TaskReminderScheduler.scheduleTaskReminder(context, task, now)
    VitalTrackWidgetUpdater.refreshAll(context)
    return task
  }

  suspend fun toggleWellnessTaskCompleted(taskId: String) {
    val existing = db.wellnessTaskDao().getTaskById(taskId) ?: return
    val now = System.currentTimeMillis()
    val nextCompleted = !existing.isCompleted
    val updated =
      existing.copy(
        isCompleted = nextCompleted,
        completedAtMillis = if (nextCompleted) now else null,
        updatedAtMillis = now,
      )
    db.wellnessTaskDao().upsertTask(updated)
    TaskReminderScheduler.scheduleTaskReminder(context, updated, now)
    if (nextCompleted) {
      prefs.addWellnessXp(30)
      TaskReminderScheduler.createNextRecurringOccurrence(existing, now)?.let { next ->
        db.wellnessTaskDao().upsertTask(next)
        TaskReminderScheduler.scheduleTaskReminder(context, next, now)
      }
    }
    VitalTrackWidgetUpdater.refreshAll(context)
  }

  suspend fun updateTaskSubtasksAndFocus(taskId: String, subtasksJson: String, addedFocusMin: Int = 0) {
    val existing = db.wellnessTaskDao().getTaskById(taskId) ?: return
    db.wellnessTaskDao()
      .upsertTask(
        existing.copy(
          subtasksJson = subtasksJson,
          focusMinutesLogged = existing.focusMinutesLogged + addedFocusMin.coerceAtLeast(0),
          updatedAtMillis = System.currentTimeMillis(),
        )
      )
    if (addedFocusMin > 0) prefs.addWellnessXp(addedFocusMin * 2)
  }

  suspend fun deleteWellnessTask(taskId: String) {
    TaskReminderScheduler.cancelTaskReminder(context, taskId)
    db.wellnessTaskDao().deleteTaskById(taskId)
    VitalTrackWidgetUpdater.refreshAll(context)
  }

  // --- Wardrobe, Nutrition, Vitals & Gear CRUD ---
  suspend fun addWardrobeItem(
    name: String,
    category: String,
    colorName: String,
    colorHex: String,
    seasonTag: String,
    occasionTag: String,
  ) {
    db.wardrobeDao()
      .upsert(
        WardrobeItemEntity(
          id = "cloth_${UUID.randomUUID()}",
          name = name.trim(),
          category = category,
          colorName = colorName.trim(),
          colorHex = colorHex,
          seasonTag = seasonTag,
          occasionTag = occasionTag,
        )
      )
  }

  suspend fun markWardrobeItemWorn(id: String) {
    val item = db.wardrobeDao().getSnapshot().firstOrNull { it.id == id } ?: return
    db.wardrobeDao()
      .upsert(item.copy(wornCount = item.wornCount + 1, lastWornMillis = System.currentTimeMillis()))
  }

  suspend fun toggleWardrobeFavorite(id: String) {
    val item = db.wardrobeDao().getSnapshot().firstOrNull { it.id == id } ?: return
    db.wardrobeDao().upsert(item.copy(isFavorite = !item.isFavorite))
  }

  suspend fun deleteWardrobeItem(id: String) = db.wardrobeDao().deleteById(id)

  suspend fun addNutritionLog(
    mealName: String,
    mealType: String,
    caloriesKcal: Int,
    proteinG: Double,
    carbsG: Double,
    fatG: Double,
    fiberG: Double = 0.0,
    beverageMl: Int = 0,
    hydrationFactor: Double = 1.0,
    caffeineMg: Int = 0,
    electrolytesMg: Int = 0,
    notes: String = "",
  ) {
    val today = WellnessCalculators.todayIso()
    db.nutritionDao()
      .upsert(
        NutritionLogEntity(
          id = "nutr_${UUID.randomUUID()}",
          dateIso = today,
          mealName = mealName.trim(),
          mealType = mealType,
          caloriesKcal = caloriesKcal,
          proteinG = proteinG,
          carbsG = carbsG,
          fatG = fatG,
          fiberG = fiberG,
          beverageMl = beverageMl,
          hydrationFactor = hydrationFactor,
          caffeineMg = caffeineMg,
          electrolytesMg = electrolytesMg,
          notes = notes.trim(),
        )
      )
    if (beverageMl > 0) {
      val effectiveMl = (beverageMl * hydrationFactor).toInt().coerceAtLeast(10)
      addWater(effectiveMl)
    } else {
      prefs.addWellnessXp(15)
    }
  }

  suspend fun deleteNutritionLog(id: String) = db.nutritionDao().deleteById(id)

  suspend fun addVitalsRecord(
    type: String,
    title: String,
    primaryValue: Double,
    secondaryValue: Double = 0.0,
    tertiaryValue: Double = 0.0,
    unit: String,
    notes: String = "",
    refillRemaining: Int = -1,
  ) {
    db.vitalsDao()
      .upsert(
        VitalsRecordEntity(
          id = "vital_${UUID.randomUUID()}",
          type = type,
          title = title.trim(),
          primaryValue = primaryValue,
          secondaryValue = secondaryValue,
          tertiaryValue = tertiaryValue,
          unit = unit,
          notes = notes.trim(),
          dateIso = WellnessCalculators.todayIso(),
          refillRemaining = refillRemaining,
        )
      )
    prefs.addWellnessXp(15)
  }

  suspend fun decrementMedicationRefill(id: String) {
    val rec = db.vitalsDao().getSnapshot().firstOrNull { it.id == id } ?: return
    if (rec.refillRemaining > 0) {
      db.vitalsDao().upsert(rec.copy(refillRemaining = rec.refillRemaining - 1))
    }
  }

  suspend fun deleteVitalsRecord(id: String) = db.vitalsDao().deleteById(id)

  suspend fun addGearItem(name: String, brand: String, retireKm: Double) {
    db.gearDao()
      .upsert(
        GearItemEntity(
          id = "gear_${UUID.randomUUID()}",
          name = name.trim(),
          brand = brand.trim(),
          retireThresholdMeters = retireKm.coerceIn(100.0, 5000.0) * 1000.0,
        )
      )
  }

  suspend fun addGearDistance(gearId: String, addMeters: Double) {
    val item = db.gearDao().getSnapshot().firstOrNull { it.id == gearId } ?: return
    db.gearDao().upsert(item.copy(distanceMeters = item.distanceMeters + addMeters.coerceAtLeast(0.0)))
  }

  suspend fun deleteGearItem(id: String) = db.gearDao().deleteById(id)

  suspend fun syncHealthConnect(): Result<String> {
    val availability = healthConnectManager.getAvailability()
    if (availability != HealthConnectAvailability.AVAILABLE) {
      return Result.failure(
        IllegalStateException(
          if (availability == HealthConnectAvailability.NOT_INSTALLED) {
            "Health Connect app is not installed on this device."
          } else {
            "Health Connect is not supported on this Android version."
          }
        )
      )
    }
    if (!healthConnectManager.hasAllPermissions()) {
      return Result.failure(IllegalStateException("Health Connect permissions have not been granted yet."))
    }
    val profile = prefs.getCurrent()
    return healthConnectManager.syncRecentHealthData(profile.heightCm).map { res ->
      if (res.heartRates.isNotEmpty()) db.healthRecordDao().upsertHeartRates(res.heartRates)
      if (res.sleepRecords.isNotEmpty()) db.healthRecordDao().upsertSleepAll(res.sleepRecords)
      if (res.weightEntries.isNotEmpty()) db.healthRecordDao().upsertWeightAll(res.weightEntries)

      val today = WellnessCalculators.todayIso()
      val hcSteps = res.todaySteps?.toInt()
      if (hcSteps != null && hcSteps > 0) {
        val local = db.dailyStepDao().getStepsForDate(today)
        if (local == null || hcSteps > local.steps) {
          db.dailyStepDao()
            .upsert(
              DailyStepEntity(
                dateIso = today,
                steps = hcSteps,
                goal = profile.dailyStepGoal,
                distanceMeters = WellnessCalculators.estimateDistanceMeters(hcSteps, profile.strideLengthMeters),
                caloriesKcal = WellnessCalculators.estimateStepCalories(hcSteps, profile.weightKg),
                activeMinutes = WellnessCalculators.estimateActiveMinutesFromSteps(hcSteps),
                source = "HEALTH_CONNECT",
                updatedAtMillis = System.currentTimeMillis(),
                syncStatus = SyncStatus.PENDING_SYNC,
              )
            )
        }
      }
      VitalTrackWidgetUpdater.refreshAll(context)
      "Synced ${res.heartRates.size} heart-rate, ${res.sleepRecords.size} sleep, and ${res.weightEntries.size} weight records."
    }
  }

  suspend fun buildAiWellnessContextSummary(): String {
    val profile = prefs.getCurrent()
    val today = WellnessCalculators.todayIso()
    val last7 = WellnessCalculators.lastNDaysIso(7)
    val stepsList = db.dailyStepDao().getAllSnapshot().filter { it.dateIso in last7 }
    val todaySteps = stepsList.firstOrNull { it.dateIso == today }?.steps ?: 0
    val weeklyAvgSteps = if (stepsList.isEmpty()) 0 else stepsList.sumOf { it.steps } / stepsList.size
    val waterToday =
      db.healthRecordDao().getWaterSnapshot().filter { it.dateIso == today }.sumOf { it.amountMl }
    val latestSleep = db.healthRecordDao().getSleepRecordsSnapshot().firstOrNull()
    val latestWeight = db.healthRecordDao().getWeightSnapshot().firstOrNull()
    val recentGps = db.gpsSessionDao().getAllSnapshot().take(3)
    val recentWorkouts = db.workoutDao().getWorkoutSessionsSnapshot().take(3)
    val recentJournals = db.healthRecordDao().getJournalSnapshot().take(3)

    return buildString {
      appendLine("User: ${profile.displayName}, Age: ${profile.age}, Step Goal: ${profile.dailyStepGoal}, Water Goal: ${profile.dailyWaterGoalMl} ml")
      appendLine("Today ($today): $todaySteps steps, $waterToday ml water logged.")
      appendLine("Last 7 recorded days avg steps: $weeklyAvgSteps (${stepsList.size} days recorded).")
      if (latestSleep != null) {
        appendLine("Latest sleep (${latestSleep.dateIso}): ${latestSleep.durationMinutes} min (source: ${latestSleep.source}).")
      } else {
        appendLine("Sleep: no records logged yet.")
      }
      if (latestWeight != null) {
        appendLine("Latest weight (${latestWeight.dateIso}): ${String.format(java.util.Locale.US, "%.1f", latestWeight.weightKg)} kg, BMI ${String.format(java.util.Locale.US, "%.1f", latestWeight.bmi)}.")
      }
      if (recentGps.isNotEmpty()) {
        appendLine("Recent GPS activities: " + recentGps.joinToString("; ") { "${it.title} (${String.format(java.util.Locale.US, "%.2f", it.distanceMeters / 1000.0)} km on ${it.dateIso})" })
      }
      if (recentWorkouts.isNotEmpty()) {
        appendLine("Recent workouts: " + recentWorkouts.joinToString("; ") { "${it.title} (${it.durationSeconds / 60} min on ${it.dateIso})" })
      }
      if (recentJournals.isNotEmpty()) {
        appendLine("Recent journal entries: " + recentJournals.joinToString("; ") { "${it.dateIso} mood=${it.mood}, energy=${it.energyLevel}/5: ${it.note.take(120)}" })
      }
    }
  }

  suspend fun saveAiMessage(role: String, content: String, topic: String) {
    db.aiAndNotificationDao()
      .insertMessage(
        AiCoachMessageEntity(
          id = UUID.randomUUID().toString(),
          role = role,
          content = content,
          topic = topic,
          createdAtMillis = System.currentTimeMillis(),
        )
      )
  }

  suspend fun clearAiHistory() {
    db.aiAndNotificationDao().clearMessages()
  }

  fun streamAiCoach(prompt: String): Flow<AiStreamChunk> =
    aiCoachService.streamWellnessCoaching(prompt, "")

  suspend fun generateOutfitSuggestions(input: StyleProfileInput): StyleRecommendationResult =
    styleAdvisorService.generateOutfitSuggestions(input)

  suspend fun syncCloudNow(): Result<CloudSyncSummary> = firestoreSyncRepository.syncNow()

  suspend fun deleteAllLocalData() =
    withContext(Dispatchers.IO) {
      db.dailyStepDao().clearAll()
      db.gpsSessionDao().clearAll()
      db.healthRecordDao().clearHeartRates()
      db.healthRecordDao().clearSleep()
      db.healthRecordDao().clearWater()
      db.healthRecordDao().clearWeights()
      db.healthRecordDao().clearJournal()
      db.workoutDao().clearWorkoutSessions()
      db.workoutDao().clearCustomPlans()
      db.habitDao().clearCompletions()
      db.habitDao().clearHabits()
      db.wellnessTaskDao().clearAll()
      db.taskCategoryDao().clearAll()
      db.wardrobeDao().clearAll()
      db.nutritionDao().clearAll()
      db.vitalsDao().clearAll()
      db.gearDao().clearAll()
      db.aiAndNotificationDao().clearMessages()
      prefs.resetAllPreferences()
      ensureSeeded()
    }

  companion object {
    fun calculateHabitStreak(habitId: String, completions: List<HabitCompletionEntity>): Int {
      val completedDates = completions.filter { it.habitId == habitId }.map { it.dateIso }.toSet()
      if (completedDates.isEmpty()) return 0
      var streak = 0
      var cursor = LocalDate.now()
      if (cursor.toString() !in completedDates) {
        cursor = cursor.minusDays(1)
      }
      while (cursor.toString() in completedDates) {
        streak++
        cursor = cursor.minusDays(1)
      }
      return streak
    }
  }
}
