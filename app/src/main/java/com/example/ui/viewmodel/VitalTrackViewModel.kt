package com.example.ui.viewmodel

import android.app.Application
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.VitalTrackApplication
import com.example.data.health.HealthConnectAvailability
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
import com.example.data.local.SleepRecordEntity
import com.example.data.local.TaskCategoryEntity
import com.example.data.local.TaskPriority
import com.example.data.local.TaskRecurrence
import com.example.data.local.UserProfilePreferences
import com.example.data.local.VitalsRecordEntity
import com.example.data.local.WardrobeItemEntity
import com.example.data.local.WaterIntakeEntity
import com.example.data.local.WeightEntryEntity
import com.example.data.local.WellnessTaskEntity
import com.example.data.local.WorkoutPlanEntity
import com.example.data.local.WorkoutSessionEntity
import com.example.data.remote.AiStreamChunk
import com.example.data.remote.CloudSyncSummary
import com.example.data.remote.DressCheckResult
import com.example.data.remote.FirebaseMultimodalExtensions
import com.example.data.remote.MealPhotoEstimate
import com.example.data.remote.StyleProfileInput
import com.example.data.remote.StyleRecommendationResult
import com.example.data.repository.VitalTrackRepository
import com.example.data.repository.WearOsBridgeStatus
import com.example.service.ActiveTrackingState
import com.example.service.ActivityTrackingForegroundService
import com.example.service.AutomatedBackupWorker
import com.example.service.ScreenBreakReminderWorker
import com.example.service.TrackingSessionHolder
import com.example.ui.theme.AccentPalette
import com.example.ui.theme.ThemePreference
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface AiCoachUiState {
  data object Idle : AiCoachUiState
  data object Loading : AiCoachUiState
  data class Streaming(val text: String) : AiCoachUiState
  data object Success : AiCoachUiState
  data class Error(val message: String) : AiCoachUiState
}

sealed interface SyncUiState {
  data object Idle : SyncUiState
  data object Syncing : SyncUiState
  data class Success(val message: String) : SyncUiState
  data class Error(val message: String) : SyncUiState
}

sealed interface StyleAdvisorUiState {
  data object Idle : StyleAdvisorUiState
  data object Loading : StyleAdvisorUiState
  data class Success(val result: StyleRecommendationResult) : StyleAdvisorUiState
  data class Error(val message: String) : StyleAdvisorUiState
}

sealed interface MealPhotoUiState {
  data object Idle : MealPhotoUiState
  data object Loading : MealPhotoUiState
  data class Success(val estimate: MealPhotoEstimate) : MealPhotoUiState
  data class Error(val message: String) : MealPhotoUiState
}

sealed interface DressCheckUiState {
  data object Idle : DressCheckUiState
  data object Loading : DressCheckUiState
  data class Success(val result: DressCheckResult) : DressCheckUiState
  data class Error(val message: String) : DressCheckUiState
}

sealed interface HabitDigestUiState {
  data object Idle : HabitDigestUiState
  data object Loading : HabitDigestUiState
  data class Success(val digest: String) : HabitDigestUiState
  data class Error(val message: String) : HabitDigestUiState
}

class VitalTrackViewModel(application: Application) : AndroidViewModel(application) {
  companion object {
    private const val TAG = "VitalTrackVM"
  }

  private val app = application as VitalTrackApplication
  val repository: VitalTrackRepository = app.repository

  val preferences: StateFlow<UserProfilePreferences> =
    repository.preferencesFlow.stateIn(
      viewModelScope,
      SharingStarted.WhileSubscribed(5000L),
      UserProfilePreferences(),
    )

  val sensorStatus: StateFlow<StepSensorStatus> = repository.sensorStatusFlow

  val dailySteps: StateFlow<List<DailyStepEntity>> =
    repository.allDailyStepsFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  val gpsSessions: StateFlow<List<GpsActivitySessionEntity>> =
    repository.gpsSessionsFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  val activeTracking: StateFlow<ActiveTrackingState> = TrackingSessionHolder.state

  val heartRates: StateFlow<List<HeartRateRecordEntity>> =
    repository.heartRatesFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  val sleepRecords: StateFlow<List<SleepRecordEntity>> =
    repository.sleepRecordsFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  val waterIntakes: StateFlow<List<WaterIntakeEntity>> =
    repository.waterIntakesFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  val weightEntries: StateFlow<List<WeightEntryEntity>> =
    repository.weightEntriesFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  val journalEntries: StateFlow<List<JournalEntryEntity>> =
    repository.journalEntriesFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  val exercises: StateFlow<List<ExerciseEntity>> =
    repository.exercisesFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  val workoutPlans: StateFlow<List<WorkoutPlanEntity>> =
    repository.workoutPlansFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  val workoutSessions: StateFlow<List<WorkoutSessionEntity>> =
    repository.workoutSessionsFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  val habits: StateFlow<List<HabitEntity>> =
    repository.habitsFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  val habitCompletions: StateFlow<List<HabitCompletionEntity>> =
    repository.habitCompletionsFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  val aiMessages: StateFlow<List<AiCoachMessageEntity>> =
    repository.aiMessagesFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  val notificationConfigs: StateFlow<List<NotificationConfigEntity>> =
    repository.notificationConfigsFlow.stateIn(
      viewModelScope,
      SharingStarted.WhileSubscribed(5000L),
      emptyList(),
    )

  val wellnessTasks: StateFlow<List<WellnessTaskEntity>> =
    repository.wellnessTasksFlow.stateIn(
      viewModelScope,
      SharingStarted.WhileSubscribed(5000L),
      emptyList(),
    )

  val taskCategories: StateFlow<List<TaskCategoryEntity>> =
    repository.taskCategoriesFlow.stateIn(
      viewModelScope,
      SharingStarted.WhileSubscribed(5000L),
      emptyList(),
    )

  val wardrobeItems: StateFlow<List<WardrobeItemEntity>> =
    repository.wardrobeItemsFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  val nutritionLogs: StateFlow<List<NutritionLogEntity>> =
    repository.nutritionLogsFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  val vitalsRecords: StateFlow<List<VitalsRecordEntity>> =
    repository.vitalsRecordsFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  val gearItems: StateFlow<List<GearItemEntity>> =
    repository.gearItemsFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  private val multimodalAi by lazy { FirebaseMultimodalExtensions(app) }

  private val _aiCoachState = MutableStateFlow<AiCoachUiState>(AiCoachUiState.Idle)
  val aiCoachState: StateFlow<AiCoachUiState> = _aiCoachState.asStateFlow()

  private val _styleAdvisorState = MutableStateFlow<StyleAdvisorUiState>(StyleAdvisorUiState.Idle)
  val styleAdvisorState: StateFlow<StyleAdvisorUiState> = _styleAdvisorState.asStateFlow()

  private val _mealPhotoState = MutableStateFlow<MealPhotoUiState>(MealPhotoUiState.Idle)
  val mealPhotoState: StateFlow<MealPhotoUiState> = _mealPhotoState.asStateFlow()

  private val _dressCheckState = MutableStateFlow<DressCheckUiState>(DressCheckUiState.Idle)
  val dressCheckState: StateFlow<DressCheckUiState> = _dressCheckState.asStateFlow()

  private val _habitDigestState = MutableStateFlow<HabitDigestUiState>(HabitDigestUiState.Idle)
  val habitDigestState: StateFlow<HabitDigestUiState> = _habitDigestState.asStateFlow()

  private val _syncState = MutableStateFlow<SyncUiState>(SyncUiState.Idle)
  val syncState: StateFlow<SyncUiState> = _syncState.asStateFlow()

  private val _snackbarMessage = MutableStateFlow<String?>(null)
  val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

  private val _isOnline = MutableStateFlow(repository.aiCoachService.isNetworkAvailable())
  val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

  init {
    refreshConnectivity()
    repository.stepSensorManager.startListening()
  }

  fun refreshConnectivity() {
    _isOnline.value = repository.aiCoachService.isNetworkAvailable()
  }

  fun onStepPermissionsUpdated() {
    repository.stepSensorManager.startListening()
  }

  fun consumeSnackbar() {
    _snackbarMessage.value = null
  }

  fun showMessage(msg: String) {
    _snackbarMessage.value = msg
  }

  fun completeOnboarding() {
    viewModelScope.launch { repository.prefs.setOnboardingCompleted(true) }
  }

  fun saveProfileSetup(
    displayName: String,
    age: Int,
    heightCm: Double,
    weightKg: Double,
    strideLengthMeters: Double,
    dailyStepGoal: Int,
    dailyWaterGoalMl: Int,
    dailySleepGoalMinutes: Int,
    dailyWorkoutGoalMinutes: Int,
    useImperialUnits: Boolean,
    languageCode: String,
    emergencyContactName: String,
    emergencyContactPhone: String,
  ) {
    viewModelScope.launch {
      repository.prefs.saveProfile(
        displayName = displayName,
        age = age,
        heightCm = heightCm,
        weightKg = weightKg,
        strideLengthMeters = strideLengthMeters,
        dailyStepGoal = dailyStepGoal,
        dailyWaterGoalMl = dailyWaterGoalMl,
        dailySleepGoalMinutes = dailySleepGoalMinutes,
        dailyWorkoutGoalMinutes = dailyWorkoutGoalMinutes,
        useImperialUnits = useImperialUnits,
        languageCode = languageCode,
        emergencyContactName = emergencyContactName,
        emergencyContactPhone = emergencyContactPhone,
        markCompleted = true,
      )
      _snackbarMessage.value = "Profile saved"
    }
  }

  fun setThemePreference(theme: ThemePreference) {
    viewModelScope.launch { repository.prefs.setThemePreference(theme) }
  }

  fun setUseImperialUnits(imperial: Boolean) {
    viewModelScope.launch { repository.prefs.setUseImperialUnits(imperial) }
  }

  fun setLanguageCode(code: String) {
    viewModelScope.launch { repository.prefs.setLanguageCode(code) }
  }

  fun setStepGoal(goal: Int) {
    viewModelScope.launch { repository.prefs.setStepGoal(goal) }
  }

  fun setWaterGoalMl(goalMl: Int) {
    viewModelScope.launch {
      repository.prefs.setWaterGoalMl(goalMl)
      _snackbarMessage.value = "Hydration goal updated to $goalMl ml"
    }
  }

  fun setThreeDVisualsEnabled(enabled: Boolean) {
    viewModelScope.launch { repository.prefs.setThreeDVisualsEnabled(enabled) }
  }

  fun setAccentPalette(palette: AccentPalette) {
    viewModelScope.launch { repository.prefs.setAccentPalette(palette) }
  }

  fun setBiometricLockEnabled(enabled: Boolean) {
    viewModelScope.launch {
      repository.prefs.setBiometricLockEnabled(enabled)
      _snackbarMessage.value = if (enabled) "Biometric app lock enabled" else "Biometric lock disabled"
    }
  }

  fun setAutoBackupEnabled(enabled: Boolean) {
    viewModelScope.launch {
      repository.prefs.setAutoBackupEnabled(enabled)
      AutomatedBackupWorker.setEnabled(app, enabled)
      _snackbarMessage.value = if (enabled) "Nightly auto-backup scheduled" else "Nightly auto-backup paused"
    }
  }

  fun triggerAutoBackupNow() {
    viewModelScope.launch {
      repository.exportBackupRepository.writeAutoBackupSnapshot().fold(
        onSuccess = { file -> _snackbarMessage.value = "Snapshot saved: ${file.name}" },
        onFailure = { _snackbarMessage.value = "Snapshot failed: ${it.localizedMessage}" },
      )
    }
  }

  fun setScreenBreakReminderEnabled(enabled: Boolean) {
    viewModelScope.launch {
      repository.prefs.setScreenBreakReminderEnabled(enabled)
      ScreenBreakReminderWorker.setEnabled(app, enabled)
      _snackbarMessage.value = if (enabled) "20-20-20 Eye Break reminders active" else "20-20-20 reminders paused"
    }
  }

  fun setWindDownTime(hour: Int, minute: Int) {
    viewModelScope.launch {
      repository.prefs.setWindDownTime(hour, minute)
      _snackbarMessage.value = String.format(java.util.Locale.US, "Wind-down set for %02d:%02d", hour, minute)
    }
  }

  fun setBodyMorphDimensions(chestCm: Double, waistCm: Double, hipCm: Double) {
    viewModelScope.launch {
      repository.prefs.setBodyMorphDimensions(chestCm, waistCm, hipCm)
      _snackbarMessage.value = "3D body proportions updated"
    }
  }

  fun setFastingState(active: Boolean, targetHours: Int) {
    viewModelScope.launch {
      val start = if (active) System.currentTimeMillis() else 0L
      repository.prefs.setFastingState(active, start, targetHours)
      _snackbarMessage.value = if (active) "${targetHours}h intermittent fast started" else "Fasting window completed"
    }
  }

  fun useStreakFreezeToken() {
    viewModelScope.launch {
      val ok = repository.prefs.consumeStreakFreezeToken()
      _snackbarMessage.value = if (ok) "Streak Freeze shield applied for today" else "No Streak Freeze tokens left"
    }
  }

  fun setActiveChallenge(id: String) {
    viewModelScope.launch {
      repository.prefs.setActiveChallengeId(id)
      _snackbarMessage.value = "Monthly challenge activated"
    }
  }

  fun setNfcQuickActionMode(mode: String) {
    viewModelScope.launch { repository.prefs.setNfcQuickActionMode(mode) }
  }

  fun handleNfcTrigger() {
    when (preferences.value.nfcQuickActionMode) {
      "WATER_250" -> addWater(250)
      "START_WALK" -> startGpsTracking("WALKING")
      else -> showMessage("NFC Wellness Tag Triggered: ${preferences.value.nfcQuickActionMode}")
    }
  }

  fun setStrideLength(strideMeters: Double) {
    viewModelScope.launch { repository.prefs.setStrideLengthMeters(strideMeters) }
  }

  fun setCloudSyncEnabled(enabled: Boolean) {
    viewModelScope.launch { repository.prefs.setCloudSyncEnabled(enabled) }
  }

  fun recordCachedMapRegion(label: String) {
    viewModelScope.launch {
      repository.prefs.setCachedMapRegion(label, System.currentTimeMillis())
      _snackbarMessage.value = "Map region cached for offline viewing"
    }
  }

  // --- GPS Tracking Foreground Service Controls ---
  fun startGpsTracking(activityType: String) {
    ActivityTrackingForegroundService.startTracking(app, activityType)
  }

  fun pauseGpsTracking() {
    ActivityTrackingForegroundService.pauseTracking(app)
  }

  fun resumeGpsTracking() {
    ActivityTrackingForegroundService.resumeTracking(app)
  }

  fun stopGpsTracking() {
    ActivityTrackingForegroundService.stopTracking(app)
    _snackbarMessage.value = "Activity session saved to your timeline"
  }

  fun deleteGpsSession(id: String) {
    viewModelScope.launch {
      repository.deleteGpsSession(id)
      _snackbarMessage.value = "Session deleted"
    }
  }

  // --- Health Logging ---
  fun addWater(amountMl: Int) {
    viewModelScope.launch {
      repository.addWater(amountMl)
      _snackbarMessage.value = "+$amountMl ml logged"
    }
  }

  fun deleteWater(id: String) {
    viewModelScope.launch { repository.deleteWater(id) }
  }

  fun addWeight(weightKg: Double, notes: String = "") {
    viewModelScope.launch {
      repository.addWeight(weightKg, notes)
      _snackbarMessage.value = "Weight entry saved"
    }
  }

  fun addManualSleep(durationMinutes: Int, qualityScore: Int, notes: String = "") {
    viewModelScope.launch {
      repository.addManualSleep(durationMinutes, qualityScore, notes)
      _snackbarMessage.value = "Sleep log saved"
    }
  }

  fun addJournalEntry(mood: String, energyLevel: Int, note: String) {
    viewModelScope.launch {
      repository.addJournalEntry(mood, energyLevel, note)
      _snackbarMessage.value = "Journal entry saved"
    }
  }

  // --- Health Connect ---
  fun healthConnectAvailability(): HealthConnectAvailability =
    repository.healthConnectManager.getAvailability()

  fun syncHealthConnect() {
    viewModelScope.launch {
      _syncState.value = SyncUiState.Syncing
      repository.syncHealthConnect().fold(
        onSuccess = { msg ->
          _syncState.value = SyncUiState.Success(msg)
          _snackbarMessage.value = msg
        },
        onFailure = { err ->
          val msg = err.localizedMessage ?: "Health Connect sync unavailable"
          _syncState.value = SyncUiState.Error(msg)
          _snackbarMessage.value = msg
        },
      )
    }
  }

  // --- Workouts & Exercises ---
  fun createCustomWorkoutPlan(
    title: String,
    category: String,
    description: String,
    exerciseIds: List<String>,
  ) {
    viewModelScope.launch {
      repository.createCustomWorkoutPlan(title, category, description, exerciseIds)
      _snackbarMessage.value = "Custom workout routine created"
    }
  }

  fun saveWorkoutSession(
    plan: WorkoutPlanEntity,
    durationSeconds: Int,
    completedSets: Int,
    totalReps: Int,
    notes: String = "",
  ) {
    viewModelScope.launch {
      repository.saveWorkoutSession(plan, durationSeconds, completedSets, totalReps, notes)
      _snackbarMessage.value = "Workout completed & saved"
    }
  }

  // --- Habits & Notifications ---
  fun addHabit(
    title: String,
    category: String,
    targetDescription: String,
    reminderTime: String,
    frequencyType: String = "DAILY",
    stackChainGroup: String = "",
    habitPolarity: String = "BUILD",
  ) {
    viewModelScope.launch {
      repository.addHabit(title, category, targetDescription, reminderTime, frequencyType, stackChainGroup, habitPolarity)
      _snackbarMessage.value = "Habit added"
    }
  }

  fun toggleHabitToday(habitId: String, dateIso: String = WellnessCalculators.todayIso()) {
    viewModelScope.launch { repository.toggleHabitCompletion(habitId, dateIso) }
  }

  fun deleteHabit(habitId: String) {
    viewModelScope.launch { repository.deleteHabit(habitId) }
  }

  fun updateNotificationConfig(config: NotificationConfigEntity) {
    viewModelScope.launch {
      repository.updateNotificationConfig(config)
      _snackbarMessage.value = "${config.title} ${if (config.enabled) "enabled" else "paused"}"
    }
  }

  // --- Wellness Tasks (Room + WorkManager Reminders) ---
  fun addTaskCategory(name: String) {
    if (name.isBlank()) return
    viewModelScope.launch {
      val cat = repository.addTaskCategory(name)
      _snackbarMessage.value = "Category '${cat.name}' saved"
    }
  }

  fun deleteCustomTaskCategory(categoryId: String) {
    viewModelScope.launch {
      repository.deleteCustomTaskCategory(categoryId)
      _snackbarMessage.value = "Custom category removed"
    }
  }

  fun saveWellnessTask(
    existingId: String? = null,
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
  ) {
    if (title.isBlank()) return
    viewModelScope.launch {
      repository.saveWellnessTask(
        existingId = existingId,
        title = title,
        notes = notes,
        category = category,
        priority = priority,
        dueAtMillis = dueAtMillis,
        reminderEnabled = reminderEnabled,
        recurrenceRule = recurrenceRule,
        subtasksJson = subtasksJson,
        geofenceLabel = geofenceLabel,
        autoEscalatePriority = autoEscalatePriority,
      )
      _snackbarMessage.value =
        if (reminderEnabled) "Task saved & reminder scheduled"
        else "Task saved locally"
    }
  }

  fun toggleWellnessTaskCompleted(taskId: String) {
    viewModelScope.launch { repository.toggleWellnessTaskCompleted(taskId) }
  }

  fun updateTaskSubtasksAndFocus(taskId: String, subtasksJson: String, addedFocusMin: Int = 0) {
    viewModelScope.launch {
      repository.updateTaskSubtasksAndFocus(taskId, subtasksJson, addedFocusMin)
      if (addedFocusMin > 0) _snackbarMessage.value = "+${addedFocusMin}m focus logged"
    }
  }

  fun deleteWellnessTask(taskId: String) {
    viewModelScope.launch {
      repository.deleteWellnessTask(taskId)
      _snackbarMessage.value = "Task deleted"
    }
  }

  fun snoozeWellnessTaskOneDay(taskId: String) {
    val existing = wellnessTasks.value.firstOrNull { it.id == taskId } ?: return
    val nextDue = maxOf(System.currentTimeMillis(), existing.dueAtMillis) + 24L * 60L * 60L * 1000L
    viewModelScope.launch {
      repository.saveWellnessTask(
        existingId = existing.id,
        title = existing.title,
        notes = existing.notes,
        category = existing.category,
        priority = existing.priority,
        dueAtMillis = nextDue,
        reminderEnabled = existing.reminderEnabled,
        recurrenceRule = existing.recurrenceRule,
        subtasksJson = existing.subtasksJson,
        geofenceLabel = existing.geofenceLabel,
        autoEscalatePriority = existing.autoEscalatePriority,
      )
      _snackbarMessage.value = "Rescheduled \"${existing.title}\" +1 day"
    }
  }

  fun logBreathingSession(protocolName: String, cyclesCompleted: Int) {
    viewModelScope.launch {
      repository.addJournalEntry(
        mood = "CALM",
        energyLevel = 4,
        note = "Completed $cyclesCompleted cycle(s) of $protocolName 3D guided breathing.",
      )
      repository.prefs.addWellnessXp(20)
      _snackbarMessage.value = "Logged $protocolName session to Health Journal"
    }
  }

  // --- Wardrobe, Nutrition, Vitals, Gear Actions ---
  fun addWardrobeItem(name: String, category: String, colorName: String, colorHex: String, season: String, occasion: String) {
    if (name.isBlank()) return
    viewModelScope.launch {
      repository.addWardrobeItem(name, category, colorName, colorHex, season, occasion)
      _snackbarMessage.value = "Added $name to Closet"
    }
  }

  fun markWardrobeItemWorn(id: String) = viewModelScope.launch { repository.markWardrobeItemWorn(id) }
  fun toggleWardrobeFavorite(id: String) = viewModelScope.launch { repository.toggleWardrobeFavorite(id) }
  fun deleteWardrobeItem(id: String) = viewModelScope.launch { repository.deleteWardrobeItem(id) }

  fun addNutritionLog(
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
    if (mealName.isBlank()) return
    viewModelScope.launch {
      repository.addNutritionLog(
        mealName, mealType, caloriesKcal, proteinG, carbsG, fatG, fiberG,
        beverageMl, hydrationFactor, caffeineMg, electrolytesMg, notes,
      )
      _snackbarMessage.value = "Logged $mealName"
    }
  }

  fun deleteNutritionLog(id: String) = viewModelScope.launch { repository.deleteNutritionLog(id) }

  fun addVitalsRecord(
    type: String,
    title: String,
    primaryValue: Double,
    secondaryValue: Double = 0.0,
    tertiaryValue: Double = 0.0,
    unit: String,
    notes: String = "",
    refillRemaining: Int = -1,
  ) {
    viewModelScope.launch {
      repository.addVitalsRecord(type, title, primaryValue, secondaryValue, tertiaryValue, unit, notes, refillRemaining)
      _snackbarMessage.value = "Saved $title"
    }
  }

  fun decrementMedicationRefill(id: String) = viewModelScope.launch { repository.decrementMedicationRefill(id) }
  fun deleteVitalsRecord(id: String) = viewModelScope.launch { repository.deleteVitalsRecord(id) }

  fun addGearItem(name: String, brand: String, retireKm: Double) {
    if (name.isBlank()) return
    viewModelScope.launch {
      repository.addGearItem(name, brand, retireKm)
      _snackbarMessage.value = "Gear added: $name"
    }
  }

  fun addGearDistance(gearId: String, km: Double) {
    viewModelScope.launch { repository.addGearDistance(gearId, km * 1000.0) }
  }

  fun deleteGearItem(id: String) = viewModelScope.launch { repository.deleteGearItem(id) }

  // --- Multimodal Gemini AI Features (#30 Dress Check, #53 Meal Estimator, #80 Habit Digest) ---
  fun analyzeMealPhoto(bitmap: Bitmap) {
    refreshConnectivity()
    viewModelScope.launch {
      _mealPhotoState.value = MealPhotoUiState.Loading
      try {
        val res = multimodalAi.analyzeMealPhoto(bitmap)
        _mealPhotoState.value = MealPhotoUiState.Success(res)
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        _mealPhotoState.value = MealPhotoUiState.Error(e.localizedMessage ?: "Meal photo analysis failed")
      }
    }
  }

  fun checkOutfitPhoto(bitmap: Bitmap, occasion: String) {
    refreshConnectivity()
    viewModelScope.launch {
      _dressCheckState.value = DressCheckUiState.Loading
      try {
        val res = multimodalAi.checkOutfitPhoto(bitmap, occasion)
        _dressCheckState.value = DressCheckUiState.Success(res)
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        _dressCheckState.value = DressCheckUiState.Error(e.localizedMessage ?: "Outfit check failed")
      }
    }
  }

  fun generateWeeklyHabitDigest() {
    refreshConnectivity()
    viewModelScope.launch {
      _habitDigestState.value = HabitDigestUiState.Loading
      try {
        val summary =
          buildString {
            appendLine(repository.buildAiWellnessContextSummary())
            appendLine("Active Habits (${habits.value.size}): " + habits.value.joinToString { it.title })
            appendLine("Total Habit Check-ins: ${habitCompletions.value.size}")
          }
        val digest = multimodalAi.generateWeeklyHabitDigest(summary)
        _habitDigestState.value = HabitDigestUiState.Success(digest)
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        _habitDigestState.value = HabitDigestUiState.Error(e.localizedMessage ?: "Weekly digest unavailable")
      }
    }
  }

  fun writeHealthConnectSnapshot() {
    viewModelScope.launch {
      val today = WellnessCalculators.todayIso()
      val steps = dailySteps.value.firstOrNull { it.dateIso == today }?.steps ?: 0
      val water = waterIntakes.value.filter { it.dateIso == today }.sumOf { it.amountMl }
      val weight = weightEntries.value.firstOrNull()?.weightKg
      repository.healthConnectManager.writeWellnessSnapshot(steps, water, weight).fold(
        onSuccess = { n -> _snackbarMessage.value = "Bidirectional Health Connect wrote $n record(s)" },
        onFailure = { _snackbarMessage.value = it.localizedMessage ?: "Health Connect write failed" },
      )
    }
  }

  fun importCsvOrJsonFromUri(uri: Uri) {
    viewModelScope.launch {
      val raw =
        withContext(Dispatchers.IO) {
          runCatching { app.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } }.getOrNull()
        }
      if (raw.isNullOrBlank()) {
        _snackbarMessage.value = "File unreadable or empty"
        return@launch
      }
      repository.exportBackupRepository.importCsvOrJson(raw).fold(
        onSuccess = { n -> _snackbarMessage.value = "Imported $n records into VITALTRACK" },
        onFailure = { _snackbarMessage.value = "Import error: ${it.localizedMessage}" },
      )
    }
  }

  fun exportPhysicianPdf(onIntentReady: (Intent) -> Unit) {
    viewModelScope.launch {
      repository.exportBackupRepository.exportPhysicianSummaryPdf().fold(
        onSuccess = { uri ->
          onIntentReady(
            repository.exportBackupRepository.createShareIntent(
              uri,
              "application/pdf",
              "VITALTRACK Physician Summary (PDF)",
            )
          )
        },
        onFailure = { _snackbarMessage.value = it.localizedMessage ?: "Physician PDF export failed" },
      )
    }
  }

  // --- AI Wellness Coach (Firebase AI Logic + Gemini 3.8 Flash) ---
  fun askAiCoach(prompt: String, topic: String = "GENERAL") {
    val cleanPrompt = prompt.trim()
    if (cleanPrompt.isEmpty()) return

    refreshConnectivity()
    viewModelScope.launch {
      repository.saveAiMessage("user", cleanPrompt, topic)
      _aiCoachState.value = AiCoachUiState.Loading
      try {
        val contextSummary = repository.buildAiWellnessContextSummary()
        var finalReply = ""
        repository.aiCoachService.streamWellnessCoaching(cleanPrompt, contextSummary).collect { chunk ->
          when (chunk) {
            is AiStreamChunk.Partial -> _aiCoachState.value = AiCoachUiState.Streaming(chunk.accumulatedText)
            is AiStreamChunk.Complete -> finalReply = chunk.fullText
          }
        }
        if (finalReply.isNotBlank()) {
          repository.saveAiMessage("assistant", finalReply, topic)
          _aiCoachState.value = AiCoachUiState.Success
        } else {
          _aiCoachState.value = AiCoachUiState.Error("Coach returned an empty response.")
        }
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        Log.e(TAG, "AI Coach error", e)
        _aiCoachState.value =
          AiCoachUiState.Error(e.localizedMessage ?: "Unable to reach AI Wellness Coach.")
      }
    }
  }

  fun clearAiHistory() {
    viewModelScope.launch {
      repository.clearAiHistory()
      _aiCoachState.value = AiCoachUiState.Idle
    }
  }

  // --- Live Smart AI Clothing & Dress Color Suggestions ---
  fun generateStyleSuggestions(input: StyleProfileInput) {
    refreshConnectivity()
    viewModelScope.launch {
      _styleAdvisorState.value = StyleAdvisorUiState.Loading
      try {
        val result = repository.generateOutfitSuggestions(input)
        _styleAdvisorState.value = StyleAdvisorUiState.Success(result)
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        Log.e(TAG, "AI Style Advisor error", e)
        _styleAdvisorState.value =
          StyleAdvisorUiState.Error(
            e.localizedMessage ?: "Unable to generate AI clothing suggestions."
          )
      }
    }
  }

  // --- Cloud Sync, Exports, Backup/Restore, Wear OS ---
  fun syncCloudNow() {
    viewModelScope.launch {
      _syncState.value = SyncUiState.Syncing
      repository.syncCloudNow().fold(
        onSuccess = { summary: CloudSyncSummary ->
          val msg = "Synced: ${summary.uploadedCount} uploaded, ${summary.downloadedCount} restored"
          _syncState.value = SyncUiState.Success(msg)
          _snackbarMessage.value = msg
        },
        onFailure = { err ->
          val msg = err.localizedMessage ?: "Cloud sync failed; local data preserved"
          _syncState.value = SyncUiState.Error(msg)
          _snackbarMessage.value = msg
        },
      )
    }
  }

  fun exportCsv(onIntentReady: (Intent) -> Unit) {
    viewModelScope.launch {
      repository.exportBackupRepository.exportActivityCsv().fold(
        onSuccess = { uri ->
          onIntentReady(
            repository.exportBackupRepository.createShareIntent(
              uri,
              "text/csv",
              "VITALTRACK Activity Report (CSV)",
            )
          )
        },
        onFailure = { _snackbarMessage.value = it.localizedMessage ?: "CSV export failed" },
      )
    }
  }

  fun exportPdf(onIntentReady: (Intent) -> Unit) {
    viewModelScope.launch {
      repository.exportBackupRepository.exportWeeklyPdfReport().fold(
        onSuccess = { uri ->
          onIntentReady(
            repository.exportBackupRepository.createShareIntent(
              uri,
              "application/pdf",
              "VITALTRACK Weekly Wellness Report (PDF)",
            )
          )
        },
        onFailure = { _snackbarMessage.value = it.localizedMessage ?: "PDF export failed" },
      )
    }
  }

  fun exportBackupJson(onIntentReady: (Intent) -> Unit) {
    viewModelScope.launch {
      repository.exportBackupRepository.exportJsonBackupFile().fold(
        onSuccess = { uri ->
          onIntentReady(
            repository.exportBackupRepository.createShareIntent(
              uri,
              "application/json",
              "VITALTRACK Backup (.json)",
            )
          )
        },
        onFailure = { _snackbarMessage.value = it.localizedMessage ?: "Backup export failed" },
      )
    }
  }

  fun restoreBackupFromUri(uri: Uri) {
    viewModelScope.launch {
      val raw =
        withContext(Dispatchers.IO) {
          runCatching {
            app.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
          }.getOrNull()
        }
      if (raw.isNullOrBlank()) {
        _snackbarMessage.value = "Unable to read backup file"
        return@launch
      }
      repository.exportBackupRepository.restoreFromBackupJson(raw).fold(
        onSuccess = { count -> _snackbarMessage.value = "Restored $count wellness records" },
        onFailure = { _snackbarMessage.value = "Invalid backup file: ${it.localizedMessage}" },
      )
    }
  }

  fun deleteAllLocalData() {
    viewModelScope.launch {
      repository.deleteAllLocalData()
      _snackbarMessage.value = "All local wellness records deleted"
    }
  }

  fun deleteAllCloudData() {
    viewModelScope.launch {
      repository.firestoreSyncRepository.deleteAllCloudData().fold(
        onSuccess = { _snackbarMessage.value = "Cloud backup deleted" },
        onFailure = { _snackbarMessage.value = it.localizedMessage ?: "Cloud delete failed" },
      )
    }
  }

  fun exportGpsGpx(sessionId: String, onIntentReady: (Intent) -> Unit) {
    viewModelScope.launch {
      repository.exportBackupRepository.exportGpsSessionGpx(sessionId).fold(
        onSuccess = { uri ->
          onIntentReady(
            repository.exportBackupRepository.createShareIntent(
              uri,
              "application/gpx+xml",
              "VITALTRACK GPS Route (.gpx)",
            )
          )
        },
        onFailure = { _snackbarMessage.value = it.localizedMessage ?: "GPX export failed" },
      )
    }
  }

  fun buildWearOsDailySnapshotJson(): String {
    val today = WellnessCalculators.todayIso()
    val prefsNow = preferences.value
    val todayStep = dailySteps.value.firstOrNull { it.dateIso == today }
    val todayWaterMl = waterIntakes.value.filter { it.dateIso == today }.sumOf { it.amountMl }
    return repository.wearableSyncBridge.buildDailySnapshotJson(
      dateIso = today,
      steps = todayStep?.steps ?: 0,
      stepGoal = prefsNow.dailyStepGoal,
      waterMl = todayWaterMl,
      waterGoalMl = prefsNow.dailyWaterGoalMl,
      activeMinutes = todayStep?.activeMinutes ?: 0,
    )
  }

  fun wearOsStatus(): WearOsBridgeStatus = repository.wearableSyncBridge.inspectBridgeStatus()

  override fun onCleared() {
    repository.stepSensorManager.stopListening()
    super.onCleared()
  }
}
