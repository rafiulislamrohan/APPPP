package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.ui.theme.AccentPalette
import com.example.ui.theme.ThemePreference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable

private val Context.vitalTrackDataStore: DataStore<Preferences> by
  preferencesDataStore(name = "vitaltrack_preferences")

@Serializable
data class UserProfilePreferences(
  val onboardingCompleted: Boolean = false,
  val profileSetupCompleted: Boolean = false,
  val displayName: String = "Explorer",
  val age: Int = 28,
  val heightCm: Double = 172.0,
  val weightKg: Double = 70.0,
  val strideLengthMeters: Double = 0.75,
  val dailyStepGoal: Int = 8000,
  val dailyWaterGoalMl: Int = 2200,
  val dailySleepGoalMinutes: Int = 480,
  val dailyWorkoutGoalMinutes: Int = 30,
  val useImperialUnits: Boolean = false,
  val languageCode: String = "en", // "en" or "bn"
  val themePreference: ThemePreference = ThemePreference.SYSTEM,
  val accentPalette: AccentPalette = AccentPalette.FOREST_CALM,
  val emergencyContactName: String = "",
  val emergencyContactPhone: String = "",
  val cloudSyncEnabled: Boolean = false,
  val lastCloudSyncMillis: Long = 0L,
  val backgroundStepWorkerEnabled: Boolean = true,
  val lastSensorBootSteps: Long = -1L,
  val lastSensorDateIso: String = "",
  val todaySensorAccumulatedSteps: Int = 0,
  val lastCachedMapRegionLabel: String = "",
  val lastCachedMapMillis: Long = 0L,
  val threeDVisualsEnabled: Boolean = true,
  val biometricLockEnabled: Boolean = false,
  val autoBackupEnabled: Boolean = false,
  val lastAutoBackupMillis: Long = 0L,
  val fastingActive: Boolean = false,
  val fastingStartMillis: Long = 0L,
  val fastingTargetHours: Int = 16,
  val screenBreakReminderEnabled: Boolean = false,
  val windDownHour: Int = 22,
  val windDownMinute: Int = 15,
  val wellnessXp: Int = 120,
  val streakFreezeTokens: Int = 2,
  val activeChallengeId: String = "challenge_steps_30",
  val nfcQuickActionMode: String = "WATER_250",
  val bodyChestCm: Double = 96.0,
  val bodyWaistCm: Double = 78.0,
  val bodyHipCm: Double = 96.0,
)

class UserPreferencesDataStore(private val context: Context) {

  private object Keys {
    val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
    val PROFILE_SETUP_COMPLETED = booleanPreferencesKey("profile_setup_completed")
    val DISPLAY_NAME = stringPreferencesKey("display_name")
    val AGE = intPreferencesKey("age")
    val HEIGHT_CM = doublePreferencesKey("height_cm")
    val WEIGHT_KG = doublePreferencesKey("weight_kg")
    val STRIDE_LENGTH_METERS = doublePreferencesKey("stride_length_meters")
    val DAILY_STEP_GOAL = intPreferencesKey("daily_step_goal")
    val DAILY_WATER_GOAL_ML = intPreferencesKey("daily_water_goal_ml")
    val DAILY_SLEEP_GOAL_MINUTES = intPreferencesKey("daily_sleep_goal_minutes")
    val DAILY_WORKOUT_GOAL_MINUTES = intPreferencesKey("daily_workout_goal_minutes")
    val USE_IMPERIAL_UNITS = booleanPreferencesKey("use_imperial_units")
    val LANGUAGE_CODE = stringPreferencesKey("language_code")
    val THEME_PREFERENCE = stringPreferencesKey("theme_preference")
    val ACCENT_PALETTE = stringPreferencesKey("accent_palette")
    val EMERGENCY_CONTACT_NAME = stringPreferencesKey("emergency_contact_name")
    val EMERGENCY_CONTACT_PHONE = stringPreferencesKey("emergency_contact_phone")
    val CLOUD_SYNC_ENABLED = booleanPreferencesKey("cloud_sync_enabled")
    val LAST_CLOUD_SYNC_MILLIS = longPreferencesKey("last_cloud_sync_millis")
    val BACKGROUND_STEP_WORKER = booleanPreferencesKey("background_step_worker")
    val LAST_SENSOR_BOOT_STEPS = longPreferencesKey("last_sensor_boot_steps")
    val LAST_SENSOR_DATE_ISO = stringPreferencesKey("last_sensor_date_iso")
    val TODAY_SENSOR_ACCUMULATED_STEPS = intPreferencesKey("today_sensor_accumulated_steps")
    val LAST_CACHED_MAP_REGION = stringPreferencesKey("last_cached_map_region")
    val LAST_CACHED_MAP_MILLIS = longPreferencesKey("last_cached_map_millis")
    val THREE_D_VISUALS = booleanPreferencesKey("three_d_visuals_enabled")
    val BIOMETRIC_LOCK = booleanPreferencesKey("biometric_lock_enabled")
    val AUTO_BACKUP = booleanPreferencesKey("auto_backup_enabled")
    val LAST_AUTO_BACKUP_MILLIS = longPreferencesKey("last_auto_backup_millis")
    val FASTING_ACTIVE = booleanPreferencesKey("fasting_active")
    val FASTING_START_MILLIS = longPreferencesKey("fasting_start_millis")
    val FASTING_TARGET_HOURS = intPreferencesKey("fasting_target_hours")
    val SCREEN_BREAK_ENABLED = booleanPreferencesKey("screen_break_enabled")
    val WIND_DOWN_HOUR = intPreferencesKey("wind_down_hour")
    val WIND_DOWN_MINUTE = intPreferencesKey("wind_down_minute")
    val WELLNESS_XP = intPreferencesKey("wellness_xp")
    val STREAK_FREEZE_TOKENS = intPreferencesKey("streak_freeze_tokens")
    val ACTIVE_CHALLENGE_ID = stringPreferencesKey("active_challenge_id")
    val NFC_QUICK_ACTION = stringPreferencesKey("nfc_quick_action_mode")
    val BODY_CHEST_CM = doublePreferencesKey("body_chest_cm")
    val BODY_WAIST_CM = doublePreferencesKey("body_waist_cm")
    val BODY_HIP_CM = doublePreferencesKey("body_hip_cm")
  }

  val preferencesFlow: Flow<UserProfilePreferences> =
    context.vitalTrackDataStore.data.map { prefs ->
      val themeName = prefs[Keys.THEME_PREFERENCE] ?: ThemePreference.SYSTEM.name
      val theme =
        ThemePreference.entries.firstOrNull { it.name == themeName } ?: ThemePreference.SYSTEM
      val paletteName = prefs[Keys.ACCENT_PALETTE] ?: AccentPalette.FOREST_CALM.name
      val palette =
        AccentPalette.entries.firstOrNull { it.name == paletteName } ?: AccentPalette.FOREST_CALM
      UserProfilePreferences(
        onboardingCompleted = prefs[Keys.ONBOARDING_COMPLETED] ?: false,
        profileSetupCompleted = prefs[Keys.PROFILE_SETUP_COMPLETED] ?: false,
        displayName = prefs[Keys.DISPLAY_NAME] ?: "Explorer",
        age = prefs[Keys.AGE] ?: 28,
        heightCm = prefs[Keys.HEIGHT_CM] ?: 172.0,
        weightKg = prefs[Keys.WEIGHT_KG] ?: 70.0,
        strideLengthMeters = prefs[Keys.STRIDE_LENGTH_METERS] ?: 0.75,
        dailyStepGoal = prefs[Keys.DAILY_STEP_GOAL] ?: 8000,
        dailyWaterGoalMl = prefs[Keys.DAILY_WATER_GOAL_ML] ?: 2200,
        dailySleepGoalMinutes = prefs[Keys.DAILY_SLEEP_GOAL_MINUTES] ?: 480,
        dailyWorkoutGoalMinutes = prefs[Keys.DAILY_WORKOUT_GOAL_MINUTES] ?: 30,
        useImperialUnits = prefs[Keys.USE_IMPERIAL_UNITS] ?: false,
        languageCode = prefs[Keys.LANGUAGE_CODE] ?: "en",
        themePreference = theme,
        accentPalette = palette,
        emergencyContactName = prefs[Keys.EMERGENCY_CONTACT_NAME] ?: "",
        emergencyContactPhone = prefs[Keys.EMERGENCY_CONTACT_PHONE] ?: "",
        cloudSyncEnabled = prefs[Keys.CLOUD_SYNC_ENABLED] ?: false,
        lastCloudSyncMillis = prefs[Keys.LAST_CLOUD_SYNC_MILLIS] ?: 0L,
        backgroundStepWorkerEnabled = prefs[Keys.BACKGROUND_STEP_WORKER] ?: true,
        lastSensorBootSteps = prefs[Keys.LAST_SENSOR_BOOT_STEPS] ?: -1L,
        lastSensorDateIso = prefs[Keys.LAST_SENSOR_DATE_ISO] ?: "",
        todaySensorAccumulatedSteps = prefs[Keys.TODAY_SENSOR_ACCUMULATED_STEPS] ?: 0,
        lastCachedMapRegionLabel = prefs[Keys.LAST_CACHED_MAP_REGION] ?: "",
        lastCachedMapMillis = prefs[Keys.LAST_CACHED_MAP_MILLIS] ?: 0L,
        threeDVisualsEnabled = prefs[Keys.THREE_D_VISUALS] ?: true,
        biometricLockEnabled = prefs[Keys.BIOMETRIC_LOCK] ?: false,
        autoBackupEnabled = prefs[Keys.AUTO_BACKUP] ?: false,
        lastAutoBackupMillis = prefs[Keys.LAST_AUTO_BACKUP_MILLIS] ?: 0L,
        fastingActive = prefs[Keys.FASTING_ACTIVE] ?: false,
        fastingStartMillis = prefs[Keys.FASTING_START_MILLIS] ?: 0L,
        fastingTargetHours = prefs[Keys.FASTING_TARGET_HOURS] ?: 16,
        screenBreakReminderEnabled = prefs[Keys.SCREEN_BREAK_ENABLED] ?: false,
        windDownHour = prefs[Keys.WIND_DOWN_HOUR] ?: 22,
        windDownMinute = prefs[Keys.WIND_DOWN_MINUTE] ?: 15,
        wellnessXp = prefs[Keys.WELLNESS_XP] ?: 120,
        streakFreezeTokens = prefs[Keys.STREAK_FREEZE_TOKENS] ?: 2,
        activeChallengeId = prefs[Keys.ACTIVE_CHALLENGE_ID] ?: "challenge_steps_30",
        nfcQuickActionMode = prefs[Keys.NFC_QUICK_ACTION] ?: "WATER_250",
        bodyChestCm = prefs[Keys.BODY_CHEST_CM] ?: 96.0,
        bodyWaistCm = prefs[Keys.BODY_WAIST_CM] ?: 78.0,
        bodyHipCm = prefs[Keys.BODY_HIP_CM] ?: 96.0,
      )
    }

  suspend fun getCurrent(): UserProfilePreferences = preferencesFlow.first()

  suspend fun setOnboardingCompleted(completed: Boolean) {
    context.vitalTrackDataStore.edit { it[Keys.ONBOARDING_COMPLETED] = completed }
  }

  suspend fun saveProfile(
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
    markCompleted: Boolean = true,
  ) {
    context.vitalTrackDataStore.edit { prefs ->
      prefs[Keys.DISPLAY_NAME] = displayName.trim().ifEmpty { "Explorer" }
      prefs[Keys.AGE] = age.coerceIn(10, 110)
      prefs[Keys.HEIGHT_CM] = heightCm.coerceIn(100.0, 240.0)
      prefs[Keys.WEIGHT_KG] = weightKg.coerceIn(25.0, 250.0)
      prefs[Keys.STRIDE_LENGTH_METERS] = strideLengthMeters.coerceIn(0.35, 1.35)
      prefs[Keys.DAILY_STEP_GOAL] = dailyStepGoal.coerceIn(1000, 50000)
      prefs[Keys.DAILY_WATER_GOAL_ML] = dailyWaterGoalMl.coerceIn(500, 6000)
      prefs[Keys.DAILY_SLEEP_GOAL_MINUTES] = dailySleepGoalMinutes.coerceIn(240, 720)
      prefs[Keys.DAILY_WORKOUT_GOAL_MINUTES] = dailyWorkoutGoalMinutes.coerceIn(10, 240)
      prefs[Keys.USE_IMPERIAL_UNITS] = useImperialUnits
      prefs[Keys.LANGUAGE_CODE] = languageCode
      prefs[Keys.EMERGENCY_CONTACT_NAME] = emergencyContactName.trim()
      prefs[Keys.EMERGENCY_CONTACT_PHONE] = emergencyContactPhone.trim()
      if (markCompleted) {
        prefs[Keys.PROFILE_SETUP_COMPLETED] = true
      }
    }
  }

  suspend fun setThemePreference(theme: ThemePreference) {
    context.vitalTrackDataStore.edit { it[Keys.THEME_PREFERENCE] = theme.name }
  }

  suspend fun setUseImperialUnits(imperial: Boolean) {
    context.vitalTrackDataStore.edit { it[Keys.USE_IMPERIAL_UNITS] = imperial }
  }

  suspend fun setLanguageCode(code: String) {
    context.vitalTrackDataStore.edit { it[Keys.LANGUAGE_CODE] = code }
  }

  suspend fun setStepGoal(goal: Int) {
    context.vitalTrackDataStore.edit { it[Keys.DAILY_STEP_GOAL] = goal.coerceIn(1000, 50000) }
  }

  suspend fun setWaterGoalMl(goalMl: Int) {
    context.vitalTrackDataStore.edit { it[Keys.DAILY_WATER_GOAL_ML] = goalMl.coerceIn(500, 6000) }
  }

  suspend fun setThreeDVisualsEnabled(enabled: Boolean) {
    context.vitalTrackDataStore.edit { it[Keys.THREE_D_VISUALS] = enabled }
  }

  suspend fun setAccentPalette(palette: AccentPalette) {
    context.vitalTrackDataStore.edit { it[Keys.ACCENT_PALETTE] = palette.name }
  }

  suspend fun setBiometricLockEnabled(enabled: Boolean) {
    context.vitalTrackDataStore.edit { it[Keys.BIOMETRIC_LOCK] = enabled }
  }

  suspend fun setAutoBackupEnabled(enabled: Boolean) {
    context.vitalTrackDataStore.edit { it[Keys.AUTO_BACKUP] = enabled }
  }

  suspend fun setLastAutoBackupMillis(millis: Long) {
    context.vitalTrackDataStore.edit { it[Keys.LAST_AUTO_BACKUP_MILLIS] = millis }
  }

  suspend fun setFastingState(active: Boolean, startMillis: Long, targetHours: Int) {
    context.vitalTrackDataStore.edit {
      it[Keys.FASTING_ACTIVE] = active
      it[Keys.FASTING_START_MILLIS] = startMillis
      it[Keys.FASTING_TARGET_HOURS] = targetHours.coerceIn(12, 24)
    }
  }

  suspend fun setScreenBreakReminderEnabled(enabled: Boolean) {
    context.vitalTrackDataStore.edit { it[Keys.SCREEN_BREAK_ENABLED] = enabled }
  }

  suspend fun setWindDownTime(hour: Int, minute: Int) {
    context.vitalTrackDataStore.edit {
      it[Keys.WIND_DOWN_HOUR] = hour.coerceIn(0, 23)
      it[Keys.WIND_DOWN_MINUTE] = minute.coerceIn(0, 59)
    }
  }

  suspend fun addWellnessXp(delta: Int) {
    context.vitalTrackDataStore.edit { prefs ->
      val current = prefs[Keys.WELLNESS_XP] ?: 120
      prefs[Keys.WELLNESS_XP] = (current + delta).coerceAtLeast(0)
    }
  }

  suspend fun consumeStreakFreezeToken(): Boolean {
    var consumed = false
    context.vitalTrackDataStore.edit { prefs ->
      val tokens = prefs[Keys.STREAK_FREEZE_TOKENS] ?: 2
      if (tokens > 0) {
        prefs[Keys.STREAK_FREEZE_TOKENS] = tokens - 1
        consumed = true
      }
    }
    return consumed
  }

  suspend fun setActiveChallengeId(challengeId: String) {
    context.vitalTrackDataStore.edit { it[Keys.ACTIVE_CHALLENGE_ID] = challengeId }
  }

  suspend fun setNfcQuickActionMode(mode: String) {
    context.vitalTrackDataStore.edit { it[Keys.NFC_QUICK_ACTION] = mode }
  }

  suspend fun setBodyMorphDimensions(chestCm: Double, waistCm: Double, hipCm: Double) {
    context.vitalTrackDataStore.edit {
      it[Keys.BODY_CHEST_CM] = chestCm.coerceIn(60.0, 160.0)
      it[Keys.BODY_WAIST_CM] = waistCm.coerceIn(50.0, 150.0)
      it[Keys.BODY_HIP_CM] = hipCm.coerceIn(60.0, 165.0)
    }
  }

  suspend fun setStrideLengthMeters(stride: Double) {
    context.vitalTrackDataStore.edit { it[Keys.STRIDE_LENGTH_METERS] = stride.coerceIn(0.35, 1.35) }
  }

  suspend fun setCloudSyncEnabled(enabled: Boolean) {
    context.vitalTrackDataStore.edit { it[Keys.CLOUD_SYNC_ENABLED] = enabled }
  }

  suspend fun setLastCloudSyncMillis(timestampMillis: Long) {
    context.vitalTrackDataStore.edit { it[Keys.LAST_CLOUD_SYNC_MILLIS] = timestampMillis }
  }

  suspend fun updateSensorBaseline(
    dateIso: String,
    bootCounterValue: Long,
    accumulatedToday: Int,
  ) {
    context.vitalTrackDataStore.edit { prefs ->
      prefs[Keys.LAST_SENSOR_DATE_ISO] = dateIso
      prefs[Keys.LAST_SENSOR_BOOT_STEPS] = bootCounterValue
      prefs[Keys.TODAY_SENSOR_ACCUMULATED_STEPS] = accumulatedToday
    }
  }

  suspend fun setCachedMapRegion(label: String, timestampMillis: Long) {
    context.vitalTrackDataStore.edit { prefs ->
      prefs[Keys.LAST_CACHED_MAP_REGION] = label
      prefs[Keys.LAST_CACHED_MAP_MILLIS] = timestampMillis
    }
  }

  suspend fun restoreFromBackup(profile: UserProfilePreferences) {
    saveProfile(
      displayName = profile.displayName,
      age = profile.age,
      heightCm = profile.heightCm,
      weightKg = profile.weightKg,
      strideLengthMeters = profile.strideLengthMeters,
      dailyStepGoal = profile.dailyStepGoal,
      dailyWaterGoalMl = profile.dailyWaterGoalMl,
      dailySleepGoalMinutes = profile.dailySleepGoalMinutes,
      dailyWorkoutGoalMinutes = profile.dailyWorkoutGoalMinutes,
      useImperialUnits = profile.useImperialUnits,
      languageCode = profile.languageCode,
      emergencyContactName = profile.emergencyContactName,
      emergencyContactPhone = profile.emergencyContactPhone,
      markCompleted = true,
    )
    setThemePreference(profile.themePreference)
    setAccentPalette(profile.accentPalette)
  }

  suspend fun resetAllPreferences() {
    context.vitalTrackDataStore.edit { it.clear() }
  }
}
