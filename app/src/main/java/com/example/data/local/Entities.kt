package com.example.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

object SyncStatus {
  const val LOCAL_ONLY = "LOCAL_ONLY"
  const val PENDING_SYNC = "PENDING_SYNC"
  const val SYNCED = "SYNCED"
}

@Serializable
data class GpsPoint(
  val latitude: Double,
  val longitude: Double,
  val altitudeMeters: Double = 0.0,
  val speedMps: Float = 0f,
  val accuracyMeters: Float = 0f,
  val timestampMillis: Long = 0L,
)

@Serializable
@Entity(tableName = "daily_steps")
data class DailyStepEntity(
  @PrimaryKey val dateIso: String,
  val steps: Int,
  val goal: Int,
  val distanceMeters: Double,
  val caloriesKcal: Double,
  val activeMinutes: Int,
  val source: String = "SENSOR_COUNTER",
  val updatedAtMillis: Long = System.currentTimeMillis(),
  val syncStatus: String = SyncStatus.PENDING_SYNC,
)

@Serializable
@Entity(tableName = "gps_sessions")
data class GpsActivitySessionEntity(
  @PrimaryKey val id: String,
  val activityType: String, // WALKING, RUNNING, CYCLING
  val title: String,
  val startTimeMillis: Long,
  val endTimeMillis: Long,
  val activeDurationMillis: Long,
  val distanceMeters: Double,
  val avgSpeedMps: Double,
  val caloriesKcal: Double,
  val steps: Int,
  val dateIso: String,
  val routePolylineJson: String,
  val notes: String = "",
  val updatedAtMillis: Long = System.currentTimeMillis(),
  val syncStatus: String = SyncStatus.PENDING_SYNC,
)

@Serializable
@Entity(tableName = "heart_rate_records")
data class HeartRateRecordEntity(
  @PrimaryKey val id: String,
  val bpm: Int,
  val recordedAtMillis: Long,
  val dateIso: String,
  val source: String, // HEALTH_CONNECT, CONNECTED_DEVICE, MANUAL
  val notes: String = "",
  val updatedAtMillis: Long = System.currentTimeMillis(),
  val syncStatus: String = SyncStatus.PENDING_SYNC,
)

@Serializable
@Entity(tableName = "sleep_records")
data class SleepRecordEntity(
  @PrimaryKey val id: String,
  val startTimeMillis: Long,
  val endTimeMillis: Long,
  val durationMinutes: Int,
  val deepMinutes: Int = 0,
  val lightMinutes: Int = 0,
  val remMinutes: Int = 0,
  val awakeMinutes: Int = 0,
  val qualityScore: Int = 0,
  val dateIso: String,
  val source: String, // HEALTH_CONNECT, MANUAL
  val notes: String = "",
  val updatedAtMillis: Long = System.currentTimeMillis(),
  val syncStatus: String = SyncStatus.PENDING_SYNC,
)

@Serializable
@Entity(tableName = "water_intakes")
data class WaterIntakeEntity(
  @PrimaryKey val id: String,
  val amountMl: Int,
  val recordedAtMillis: Long,
  val dateIso: String,
  val updatedAtMillis: Long = System.currentTimeMillis(),
  val syncStatus: String = SyncStatus.PENDING_SYNC,
)

@Serializable
@Entity(tableName = "weight_entries")
data class WeightEntryEntity(
  @PrimaryKey val id: String,
  val weightKg: Double,
  val bmi: Double,
  val recordedAtMillis: Long,
  val dateIso: String,
  val source: String = "MANUAL",
  val notes: String = "",
  val updatedAtMillis: Long = System.currentTimeMillis(),
  val syncStatus: String = SyncStatus.PENDING_SYNC,
)

@Serializable
@Entity(tableName = "journal_entries")
data class JournalEntryEntity(
  @PrimaryKey val id: String,
  val dateIso: String,
  val mood: String, // GREAT, GOOD, CALM, TIRED, STRESSED
  val energyLevel: Int, // 1..5
  val note: String,
  val createdAtMillis: Long,
  val updatedAtMillis: Long = System.currentTimeMillis(),
  val syncStatus: String = SyncStatus.PENDING_SYNC,
)

@Serializable
@Entity(tableName = "exercises")
data class ExerciseEntity(
  @PrimaryKey val id: String,
  val name: String,
  val category: String, // WALKING, RUNNING, CYCLING, STRENGTH, BODYWEIGHT, STRETCHING, YOGA, INDOOR
  val description: String,
  val instructions: String,
  val difficulty: String, // Beginner, Intermediate, Advanced
  val defaultSets: Int,
  val defaultReps: Int,
  val defaultDurationSec: Int,
  val restSeconds: Int,
  val metValue: Double,
  val isCustom: Boolean = false,
)

@Serializable
@Entity(tableName = "workout_plans")
data class WorkoutPlanEntity(
  @PrimaryKey val id: String,
  val title: String,
  val category: String,
  val description: String,
  val exerciseIdsJson: String,
  val estimatedMinutes: Int,
  val isCustom: Boolean = false,
  val createdAtMillis: Long = System.currentTimeMillis(),
  val updatedAtMillis: Long = System.currentTimeMillis(),
  val syncStatus: String = SyncStatus.PENDING_SYNC,
)

@Serializable
@Entity(tableName = "workout_sessions")
data class WorkoutSessionEntity(
  @PrimaryKey val id: String,
  val workoutPlanId: String,
  val title: String,
  val category: String,
  val dateIso: String,
  val startTimeMillis: Long,
  val endTimeMillis: Long,
  val durationSeconds: Int,
  val completedSets: Int,
  val totalReps: Int,
  val caloriesKcal: Double,
  val notes: String = "",
  val updatedAtMillis: Long = System.currentTimeMillis(),
  val syncStatus: String = SyncStatus.PENDING_SYNC,
)

@Serializable
@Entity(tableName = "habits")
data class HabitEntity(
  @PrimaryKey val id: String,
  val title: String,
  val category: String, // WATER, SLEEP, EXERCISE, WALKING, CUSTOM
  val targetDescription: String,
  val reminderTime: String = "",
  @ColumnInfo(defaultValue = "DAILY") val frequencyType: String = "DAILY", // DAILY, WEEKDAYS, THREE_PER_WEEK
  @ColumnInfo(defaultValue = "") val stackChainGroup: String = "",
  @ColumnInfo(defaultValue = "BUILD") val habitPolarity: String = "BUILD", // BUILD or QUIT
  val createdAtMillis: Long = System.currentTimeMillis(),
  val isArchived: Boolean = false,
  val updatedAtMillis: Long = System.currentTimeMillis(),
  val syncStatus: String = SyncStatus.PENDING_SYNC,
)

@Serializable
@Entity(tableName = "habit_completions")
data class HabitCompletionEntity(
  @PrimaryKey val id: String, // "$habitId|$dateIso"
  val habitId: String,
  val dateIso: String,
  val completedAtMillis: Long,
  val syncStatus: String = SyncStatus.PENDING_SYNC,
)

@Serializable
@Entity(tableName = "ai_coach_messages")
data class AiCoachMessageEntity(
  @PrimaryKey val id: String,
  val role: String, // user, assistant
  val content: String,
  val topic: String = "GENERAL",
  val createdAtMillis: Long = System.currentTimeMillis(),
)

@Serializable
@Entity(tableName = "notification_configs")
data class NotificationConfigEntity(
  @PrimaryKey val id: String,
  val type: String, // WATER, HABIT, STEPS, WORKOUT
  val title: String,
  val enabled: Boolean,
  val intervalMinutes: Int = 120,
  val hourOfDay: Int = 9,
  val minute: Int = 0,
  val updatedAtMillis: Long = System.currentTimeMillis(),
)

enum class TaskPriority(val storageKey: String, val label: String, val sortRank: Int) {
  HIGH("HIGH", "High", 0),
  MEDIUM("MEDIUM", "Medium", 1),
  LOW("LOW", "Low", 2);

  companion object {
    fun fromKey(raw: String?): TaskPriority =
      entries.firstOrNull { it.storageKey.equals(raw, ignoreCase = true) } ?: MEDIUM
  }
}

enum class TaskRecurrence(val storageKey: String, val label: String) {
  NONE("NONE", "One-time"),
  DAILY("DAILY", "Daily"),
  WEEKDAYS("WEEKDAYS", "Weekdays"),
  WEEKLY("WEEKLY", "Weekly");

  companion object {
    fun fromKey(raw: String?): TaskRecurrence =
      entries.firstOrNull { it.storageKey.equals(raw, ignoreCase = true) } ?: NONE
  }
}

@Serializable
data class TaskSubtask(
  val title: String,
  val done: Boolean = false,
)

@Serializable
@Entity(
  tableName = "task_categories",
)
data class TaskCategoryEntity(
  @PrimaryKey val id: String,
  val name: String,
  @ColumnInfo(defaultValue = "0") val isDefault: Boolean = false,
  @ColumnInfo(defaultValue = "0") val createdAtMillis: Long = System.currentTimeMillis(),
)

@Serializable
@Entity(
  tableName = "wellness_tasks",
  indices = [
    Index(value = ["dueAtMillis"]),
    Index(value = ["category"]),
  ],
)
data class WellnessTaskEntity(
  @PrimaryKey val id: String,
  val title: String,
  val notes: String = "",
  val category: String = "WELLNESS",
  @ColumnInfo(defaultValue = "MEDIUM") val priority: String = TaskPriority.MEDIUM.storageKey,
  val dueAtMillis: Long,
  val reminderEnabled: Boolean = true,
  val isCompleted: Boolean = false,
  val completedAtMillis: Long? = null,
  @ColumnInfo(defaultValue = "NONE") val recurrenceRule: String = TaskRecurrence.NONE.storageKey,
  @ColumnInfo(defaultValue = "[]") val subtasksJson: String = "[]",
  @ColumnInfo(defaultValue = "") val geofenceLabel: String = "",
  @ColumnInfo(defaultValue = "0") val focusMinutesLogged: Int = 0,
  @ColumnInfo(defaultValue = "0") val autoEscalatePriority: Boolean = false,
  val createdAtMillis: Long = System.currentTimeMillis(),
  val updatedAtMillis: Long = System.currentTimeMillis(),
)

@Serializable
@Entity(tableName = "wardrobe_items")
data class WardrobeItemEntity(
  @PrimaryKey val id: String,
  val name: String,
  val category: String, // TOP, BOTTOM, DRESS, OUTERWEAR, FOOTWEAR, ACCESSORY
  val colorName: String,
  val colorHex: String,
  val seasonTag: String = "ALL_SEASON", // SUMMER, WINTER, RAIN, ALL_SEASON
  val occasionTag: String = "CASUAL", // CASUAL, OFFICE, WORKOUT, FORMAL
  val isFavorite: Boolean = false,
  val wornCount: Int = 0,
  val lastWornMillis: Long = 0L,
  val createdAtMillis: Long = System.currentTimeMillis(),
)

@Serializable
@Entity(tableName = "nutrition_logs", indices = [Index(value = ["dateIso"])])
data class NutritionLogEntity(
  @PrimaryKey val id: String,
  val dateIso: String,
  val mealName: String,
  val mealType: String = "MEAL", // BREAKFAST, LUNCH, DINNER, SNACK, BEVERAGE, SUPPLEMENT
  val caloriesKcal: Int = 0,
  val proteinG: Double = 0.0,
  val carbsG: Double = 0.0,
  val fatG: Double = 0.0,
  val fiberG: Double = 0.0,
  val beverageMl: Int = 0,
  val hydrationFactor: Double = 1.0,
  val caffeineMg: Int = 0,
  val electrolytesMg: Int = 0,
  val notes: String = "",
  val recordedAtMillis: Long = System.currentTimeMillis(),
)

@Serializable
@Entity(tableName = "vitals_records", indices = [Index(value = ["type"]), Index(value = ["dateIso"])])
data class VitalsRecordEntity(
  @PrimaryKey val id: String,
  val type: String, // BP, GLUCOSE, MEASUREMENT, CYCLE, MEDICATION, SYMPTOM, LAB, BENCHMARK
  val title: String,
  val primaryValue: Double,
  val secondaryValue: Double = 0.0,
  val tertiaryValue: Double = 0.0,
  val unit: String = "",
  val notes: String = "",
  val dateIso: String,
  val refillRemaining: Int = -1,
  val recordedAtMillis: Long = System.currentTimeMillis(),
)

@Serializable
@Entity(tableName = "gear_items")
data class GearItemEntity(
  @PrimaryKey val id: String,
  val name: String,
  val brand: String = "",
  val gearType: String = "RUNNING_SHOES",
  val distanceMeters: Double = 0.0,
  val retireThresholdMeters: Double = 650_000.0,
  val isActive: Boolean = true,
  val createdAtMillis: Long = System.currentTimeMillis(),
)
