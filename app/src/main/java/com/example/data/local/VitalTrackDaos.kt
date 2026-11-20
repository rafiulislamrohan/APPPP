package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyStepDao {
  @Query("SELECT * FROM daily_steps ORDER BY dateIso DESC")
  fun observeAllSteps(): Flow<List<DailyStepEntity>>

  @Query("SELECT * FROM daily_steps WHERE dateIso = :dateIso LIMIT 1")
  fun observeStepsForDate(dateIso: String): Flow<DailyStepEntity?>

  @Query("SELECT * FROM daily_steps WHERE dateIso = :dateIso LIMIT 1")
  suspend fun getStepsForDate(dateIso: String): DailyStepEntity?

  @Query("SELECT * FROM daily_steps ORDER BY dateIso DESC LIMIT :limit")
  suspend fun getRecentSteps(limit: Int): List<DailyStepEntity>

  @Query("SELECT * FROM daily_steps")
  suspend fun getAllSnapshot(): List<DailyStepEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsert(step: DailyStepEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertAll(steps: List<DailyStepEntity>)

  @Query("DELETE FROM daily_steps")
  suspend fun clearAll()
}

@Dao
interface GpsSessionDao {
  @Query("SELECT * FROM gps_sessions ORDER BY startTimeMillis DESC")
  fun observeAllSessions(): Flow<List<GpsActivitySessionEntity>>

  @Query("SELECT * FROM gps_sessions WHERE id = :id LIMIT 1")
  fun observeSessionById(id: String): Flow<GpsActivitySessionEntity?>

  @Query("SELECT * FROM gps_sessions WHERE dateIso = :dateIso ORDER BY startTimeMillis DESC")
  fun observeSessionsForDate(dateIso: String): Flow<List<GpsActivitySessionEntity>>

  @Query("SELECT * FROM gps_sessions ORDER BY startTimeMillis DESC")
  suspend fun getAllSnapshot(): List<GpsActivitySessionEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsert(session: GpsActivitySessionEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertAll(sessions: List<GpsActivitySessionEntity>)

  @Query("DELETE FROM gps_sessions WHERE id = :id")
  suspend fun deleteById(id: String)

  @Query("DELETE FROM gps_sessions")
  suspend fun clearAll()
}

@Dao
interface HealthRecordDao {
  @Query("SELECT * FROM heart_rate_records ORDER BY recordedAtMillis DESC")
  fun observeHeartRates(): Flow<List<HeartRateRecordEntity>>

  @Query("SELECT * FROM heart_rate_records ORDER BY recordedAtMillis DESC")
  suspend fun getHeartRatesSnapshot(): List<HeartRateRecordEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertHeartRates(items: List<HeartRateRecordEntity>)

  @Query("DELETE FROM heart_rate_records")
  suspend fun clearHeartRates()

  @Query("SELECT * FROM sleep_records ORDER BY endTimeMillis DESC")
  fun observeSleepRecords(): Flow<List<SleepRecordEntity>>

  @Query("SELECT * FROM sleep_records ORDER BY endTimeMillis DESC")
  suspend fun getSleepRecordsSnapshot(): List<SleepRecordEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertSleep(record: SleepRecordEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertSleepAll(records: List<SleepRecordEntity>)

  @Query("DELETE FROM sleep_records WHERE id = :id")
  suspend fun deleteSleepById(id: String)

  @Query("DELETE FROM sleep_records")
  suspend fun clearSleep()

  @Query("SELECT * FROM water_intakes ORDER BY recordedAtMillis DESC")
  fun observeAllWater(): Flow<List<WaterIntakeEntity>>

  @Query("SELECT * FROM water_intakes WHERE dateIso = :dateIso ORDER BY recordedAtMillis DESC")
  fun observeWaterForDate(dateIso: String): Flow<List<WaterIntakeEntity>>

  @Query("SELECT * FROM water_intakes ORDER BY recordedAtMillis DESC")
  suspend fun getWaterSnapshot(): List<WaterIntakeEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertWater(entry: WaterIntakeEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertWaterAll(entries: List<WaterIntakeEntity>)

  @Query("DELETE FROM water_intakes WHERE id = :id")
  suspend fun deleteWaterById(id: String)

  @Query("DELETE FROM water_intakes")
  suspend fun clearWater()

  @Query("SELECT * FROM weight_entries ORDER BY recordedAtMillis DESC")
  fun observeWeightEntries(): Flow<List<WeightEntryEntity>>

  @Query("SELECT * FROM weight_entries ORDER BY recordedAtMillis DESC")
  suspend fun getWeightSnapshot(): List<WeightEntryEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertWeight(entry: WeightEntryEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertWeightAll(entries: List<WeightEntryEntity>)

  @Query("DELETE FROM weight_entries WHERE id = :id")
  suspend fun deleteWeightById(id: String)

  @Query("DELETE FROM weight_entries")
  suspend fun clearWeights()

  @Query("SELECT * FROM journal_entries ORDER BY createdAtMillis DESC")
  fun observeJournalEntries(): Flow<List<JournalEntryEntity>>

  @Query("SELECT * FROM journal_entries ORDER BY createdAtMillis DESC")
  suspend fun getJournalSnapshot(): List<JournalEntryEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertJournal(entry: JournalEntryEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertJournalAll(entries: List<JournalEntryEntity>)

  @Query("DELETE FROM journal_entries WHERE id = :id")
  suspend fun deleteJournalById(id: String)

  @Query("DELETE FROM journal_entries")
  suspend fun clearJournal()
}

@Dao
interface WorkoutDao {
  @Query("SELECT * FROM exercises ORDER BY category ASC, name ASC")
  fun observeExercises(): Flow<List<ExerciseEntity>>

  @Query("SELECT COUNT(*) FROM exercises")
  suspend fun exerciseCount(): Int

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertExercises(exercises: List<ExerciseEntity>)

  @Query("SELECT * FROM workout_plans ORDER BY isCustom DESC, title ASC")
  fun observeWorkoutPlans(): Flow<List<WorkoutPlanEntity>>

  @Query("SELECT * FROM workout_plans WHERE id = :id LIMIT 1")
  fun observeWorkoutPlanById(id: String): Flow<WorkoutPlanEntity?>

  @Query("SELECT * FROM workout_plans WHERE id = :id LIMIT 1")
  suspend fun getWorkoutPlanById(id: String): WorkoutPlanEntity?

  @Query("SELECT * FROM workout_plans")
  suspend fun getWorkoutPlansSnapshot(): List<WorkoutPlanEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertWorkoutPlan(plan: WorkoutPlanEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertWorkoutPlans(plans: List<WorkoutPlanEntity>)

  @Query("SELECT * FROM workout_sessions ORDER BY startTimeMillis DESC")
  fun observeWorkoutSessions(): Flow<List<WorkoutSessionEntity>>

  @Query("SELECT * FROM workout_sessions ORDER BY startTimeMillis DESC")
  suspend fun getWorkoutSessionsSnapshot(): List<WorkoutSessionEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertWorkoutSession(session: WorkoutSessionEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertWorkoutSessions(sessions: List<WorkoutSessionEntity>)

  @Query("DELETE FROM workout_sessions")
  suspend fun clearWorkoutSessions()

  @Query("DELETE FROM workout_plans WHERE isCustom = 1")
  suspend fun clearCustomPlans()
}

@Dao
interface HabitDao {
  @Query("SELECT * FROM habits WHERE isArchived = 0 ORDER BY createdAtMillis ASC")
  fun observeActiveHabits(): Flow<List<HabitEntity>>

  @Query("SELECT COUNT(*) FROM habits")
  suspend fun habitCount(): Int

  @Query("SELECT * FROM habits")
  suspend fun getHabitsSnapshot(): List<HabitEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertHabit(habit: HabitEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertHabits(habits: List<HabitEntity>)

  @Query("DELETE FROM habits WHERE id = :id")
  suspend fun deleteHabitById(id: String)

  @Query("DELETE FROM habits")
  suspend fun clearHabits()

  @Query("SELECT * FROM habit_completions ORDER BY dateIso DESC")
  fun observeAllCompletions(): Flow<List<HabitCompletionEntity>>

  @Query("SELECT * FROM habit_completions")
  suspend fun getCompletionsSnapshot(): List<HabitCompletionEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertCompletion(completion: HabitCompletionEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertCompletions(completions: List<HabitCompletionEntity>)

  @Query("DELETE FROM habit_completions WHERE id = :id")
  suspend fun deleteCompletionById(id: String)

  @Query("DELETE FROM habit_completions")
  suspend fun clearCompletions()
}

@Dao
interface AiAndNotificationDao {
  @Query("SELECT * FROM ai_coach_messages ORDER BY createdAtMillis ASC")
  fun observeMessages(): Flow<List<AiCoachMessageEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertMessage(message: AiCoachMessageEntity)

  @Query("DELETE FROM ai_coach_messages")
  suspend fun clearMessages()

  @Query("SELECT * FROM notification_configs ORDER BY id ASC")
  fun observeNotificationConfigs(): Flow<List<NotificationConfigEntity>>

  @Query("SELECT * FROM notification_configs ORDER BY id ASC")
  suspend fun getNotificationConfigsSnapshot(): List<NotificationConfigEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertNotificationConfigs(configs: List<NotificationConfigEntity>)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertNotificationConfig(config: NotificationConfigEntity)
}

@Dao
interface WellnessTaskDao {
  @Query(
    """
    SELECT * FROM wellness_tasks
    ORDER BY isCompleted ASC,
      CASE priority WHEN 'HIGH' THEN 0 WHEN 'MEDIUM' THEN 1 ELSE 2 END ASC,
      dueAtMillis ASC
    """
  )
  fun observeAllTasks(): Flow<List<WellnessTaskEntity>>

  @Query(
    """
    SELECT * FROM wellness_tasks
    WHERE category = :category
    ORDER BY isCompleted ASC,
      CASE priority WHEN 'HIGH' THEN 0 WHEN 'MEDIUM' THEN 1 ELSE 2 END ASC,
      dueAtMillis ASC
    """
  )
  fun observeTasksByCategory(category: String): Flow<List<WellnessTaskEntity>>

  @Query(
    """
    SELECT * FROM wellness_tasks
    WHERE title LIKE '%' || :query || '%' OR notes LIKE '%' || :query || '%'
    ORDER BY isCompleted ASC,
      CASE priority WHEN 'HIGH' THEN 0 WHEN 'MEDIUM' THEN 1 ELSE 2 END ASC,
      dueAtMillis ASC
    """
  )
  fun searchTasks(query: String): Flow<List<WellnessTaskEntity>>

  @Query("SELECT * FROM wellness_tasks WHERE isCompleted = 0 AND reminderEnabled = 1 ORDER BY dueAtMillis ASC")
  suspend fun getPendingReminderTasksSnapshot(): List<WellnessTaskEntity>

  @Query(
    """
    SELECT * FROM wellness_tasks
    ORDER BY isCompleted ASC,
      CASE priority WHEN 'HIGH' THEN 0 WHEN 'MEDIUM' THEN 1 ELSE 2 END ASC,
      dueAtMillis ASC
    """
  )
  suspend fun getAllTasksSnapshot(): List<WellnessTaskEntity>

  @Query("SELECT * FROM wellness_tasks WHERE id = :id LIMIT 1")
  suspend fun getTaskById(id: String): WellnessTaskEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertTask(task: WellnessTaskEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertTasks(tasks: List<WellnessTaskEntity>)

  @Query("DELETE FROM wellness_tasks WHERE id = :id")
  suspend fun deleteTaskById(id: String)

  @Query("DELETE FROM wellness_tasks")
  suspend fun clearAll()
}

@Dao
interface TaskCategoryDao {
  @Query("SELECT * FROM task_categories ORDER BY isDefault DESC, createdAtMillis ASC, name ASC")
  fun observeCategories(): Flow<List<TaskCategoryEntity>>

  @Query("SELECT * FROM task_categories ORDER BY isDefault DESC, createdAtMillis ASC, name ASC")
  suspend fun getCategoriesSnapshot(): List<TaskCategoryEntity>

  @Query("SELECT COUNT(*) FROM task_categories")
  suspend fun categoryCount(): Int

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertCategory(category: TaskCategoryEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertCategories(categories: List<TaskCategoryEntity>)

  @Query("DELETE FROM task_categories WHERE id = :id AND isDefault = 0")
  suspend fun deleteCustomCategoryById(id: String)

  @Query("DELETE FROM task_categories")
  suspend fun clearAll()
}

@Dao
interface WardrobeDao {
  @Query("SELECT * FROM wardrobe_items ORDER BY isFavorite DESC, createdAtMillis DESC")
  fun observeAll(): Flow<List<WardrobeItemEntity>>

  @Query("SELECT * FROM wardrobe_items")
  suspend fun getSnapshot(): List<WardrobeItemEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsert(item: WardrobeItemEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertAll(items: List<WardrobeItemEntity>)

  @Query("DELETE FROM wardrobe_items WHERE id = :id")
  suspend fun deleteById(id: String)

  @Query("DELETE FROM wardrobe_items")
  suspend fun clearAll()
}

@Dao
interface NutritionDao {
  @Query("SELECT * FROM nutrition_logs ORDER BY recordedAtMillis DESC")
  fun observeAll(): Flow<List<NutritionLogEntity>>

  @Query("SELECT * FROM nutrition_logs ORDER BY recordedAtMillis DESC")
  suspend fun getSnapshot(): List<NutritionLogEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsert(item: NutritionLogEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertAll(items: List<NutritionLogEntity>)

  @Query("DELETE FROM nutrition_logs WHERE id = :id")
  suspend fun deleteById(id: String)

  @Query("DELETE FROM nutrition_logs")
  suspend fun clearAll()
}

@Dao
interface VitalsDao {
  @Query("SELECT * FROM vitals_records ORDER BY recordedAtMillis DESC")
  fun observeAll(): Flow<List<VitalsRecordEntity>>

  @Query("SELECT * FROM vitals_records ORDER BY recordedAtMillis DESC")
  suspend fun getSnapshot(): List<VitalsRecordEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsert(record: VitalsRecordEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertAll(records: List<VitalsRecordEntity>)

  @Query("DELETE FROM vitals_records WHERE id = :id")
  suspend fun deleteById(id: String)

  @Query("DELETE FROM vitals_records")
  suspend fun clearAll()
}

@Dao
interface GearDao {
  @Query("SELECT * FROM gear_items ORDER BY isActive DESC, createdAtMillis DESC")
  fun observeAll(): Flow<List<GearItemEntity>>

  @Query("SELECT * FROM gear_items")
  suspend fun getSnapshot(): List<GearItemEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsert(item: GearItemEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertAll(items: List<GearItemEntity>)

  @Query("DELETE FROM gear_items WHERE id = :id")
  suspend fun deleteById(id: String)

  @Query("DELETE FROM gear_items")
  suspend fun clearAll()
}
