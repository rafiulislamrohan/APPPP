package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
  entities = [
    DailyStepEntity::class,
    GpsActivitySessionEntity::class,
    HeartRateRecordEntity::class,
    SleepRecordEntity::class,
    WaterIntakeEntity::class,
    WeightEntryEntity::class,
    JournalEntryEntity::class,
    ExerciseEntity::class,
    WorkoutPlanEntity::class,
    WorkoutSessionEntity::class,
    HabitEntity::class,
    HabitCompletionEntity::class,
    AiCoachMessageEntity::class,
    NotificationConfigEntity::class,
    WellnessTaskEntity::class,
    TaskCategoryEntity::class,
    WardrobeItemEntity::class,
    NutritionLogEntity::class,
    VitalsRecordEntity::class,
    GearItemEntity::class,
  ],
  version = 5,
  exportSchema = false,
)
abstract class VitalTrackDatabase : RoomDatabase() {
  abstract fun dailyStepDao(): DailyStepDao
  abstract fun gpsSessionDao(): GpsSessionDao
  abstract fun healthRecordDao(): HealthRecordDao
  abstract fun workoutDao(): WorkoutDao
  abstract fun habitDao(): HabitDao
  abstract fun aiAndNotificationDao(): AiAndNotificationDao
  abstract fun wellnessTaskDao(): WellnessTaskDao
  abstract fun taskCategoryDao(): TaskCategoryDao
  abstract fun wardrobeDao(): WardrobeDao
  abstract fun nutritionDao(): NutritionDao
  abstract fun vitalsDao(): VitalsDao
  abstract fun gearDao(): GearDao

  companion object {
    @Volatile private var INSTANCE: VitalTrackDatabase? = null

    /**
     * Explicit database migration example (v1 -> v2) adding indexing support cleanly without destructive data loss.
     */
    val MIGRATION_1_2 =
      object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
          db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_gps_sessions_dateIso ON gps_sessions(dateIso)"
          )
          db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_habit_completions_dateIso ON habit_completions(dateIso)"
          )
        }
      }

    val MIGRATION_2_3 =
      object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
          db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS wellness_tasks (
              id TEXT NOT NULL PRIMARY KEY,
              title TEXT NOT NULL,
              notes TEXT NOT NULL,
              category TEXT NOT NULL,
              dueAtMillis INTEGER NOT NULL,
              reminderEnabled INTEGER NOT NULL,
              isCompleted INTEGER NOT NULL,
              completedAtMillis INTEGER,
              createdAtMillis INTEGER NOT NULL,
              updatedAtMillis INTEGER NOT NULL
            )
            """.trimIndent()
          )
          db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_wellness_tasks_dueAtMillis ON wellness_tasks(dueAtMillis)"
          )
        }
      }

    val MIGRATION_3_4 =
      object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
          db.execSQL(
            "ALTER TABLE wellness_tasks ADD COLUMN priority TEXT NOT NULL DEFAULT 'MEDIUM'"
          )
          db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_wellness_tasks_category ON wellness_tasks(category)"
          )
          db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS task_categories (
              id TEXT NOT NULL PRIMARY KEY,
              name TEXT NOT NULL,
              isDefault INTEGER NOT NULL DEFAULT 0,
              createdAtMillis INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
          )
          db.execSQL("INSERT OR IGNORE INTO task_categories (id, name, isDefault, createdAtMillis) VALUES ('cat_wellness', 'WELLNESS', 1, 1)")
          db.execSQL("INSERT OR IGNORE INTO task_categories (id, name, isDefault, createdAtMillis) VALUES ('cat_workout', 'WORKOUT', 1, 2)")
          db.execSQL("INSERT OR IGNORE INTO task_categories (id, name, isDefault, createdAtMillis) VALUES ('cat_hydration', 'HYDRATION', 1, 3)")
          db.execSQL("INSERT OR IGNORE INTO task_categories (id, name, isDefault, createdAtMillis) VALUES ('cat_medication', 'MEDICATION', 1, 4)")
          db.execSQL("INSERT OR IGNORE INTO task_categories (id, name, isDefault, createdAtMillis) VALUES ('cat_general', 'GENERAL', 1, 5)")
        }
      }

    val MIGRATION_4_5 =
      object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
          db.execSQL("ALTER TABLE habits ADD COLUMN frequencyType TEXT NOT NULL DEFAULT 'DAILY'")
          db.execSQL("ALTER TABLE habits ADD COLUMN stackChainGroup TEXT NOT NULL DEFAULT ''")
          db.execSQL("ALTER TABLE habits ADD COLUMN habitPolarity TEXT NOT NULL DEFAULT 'BUILD'")

          db.execSQL("ALTER TABLE wellness_tasks ADD COLUMN recurrenceRule TEXT NOT NULL DEFAULT 'NONE'")
          db.execSQL("ALTER TABLE wellness_tasks ADD COLUMN subtasksJson TEXT NOT NULL DEFAULT '[]'")
          db.execSQL("ALTER TABLE wellness_tasks ADD COLUMN geofenceLabel TEXT NOT NULL DEFAULT ''")
          db.execSQL("ALTER TABLE wellness_tasks ADD COLUMN focusMinutesLogged INTEGER NOT NULL DEFAULT 0")
          db.execSQL("ALTER TABLE wellness_tasks ADD COLUMN autoEscalatePriority INTEGER NOT NULL DEFAULT 0")

          db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS wardrobe_items (
              id TEXT NOT NULL PRIMARY KEY,
              name TEXT NOT NULL,
              category TEXT NOT NULL,
              colorName TEXT NOT NULL,
              colorHex TEXT NOT NULL,
              seasonTag TEXT NOT NULL,
              occasionTag TEXT NOT NULL,
              isFavorite INTEGER NOT NULL,
              wornCount INTEGER NOT NULL,
              lastWornMillis INTEGER NOT NULL,
              createdAtMillis INTEGER NOT NULL
            )
            """.trimIndent()
          )
          db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS nutrition_logs (
              id TEXT NOT NULL PRIMARY KEY,
              dateIso TEXT NOT NULL,
              mealName TEXT NOT NULL,
              mealType TEXT NOT NULL,
              caloriesKcal INTEGER NOT NULL,
              proteinG REAL NOT NULL,
              carbsG REAL NOT NULL,
              fatG REAL NOT NULL,
              fiberG REAL NOT NULL,
              beverageMl INTEGER NOT NULL,
              hydrationFactor REAL NOT NULL,
              caffeineMg INTEGER NOT NULL,
              electrolytesMg INTEGER NOT NULL,
              notes TEXT NOT NULL,
              recordedAtMillis INTEGER NOT NULL
            )
            """.trimIndent()
          )
          db.execSQL("CREATE INDEX IF NOT EXISTS index_nutrition_logs_dateIso ON nutrition_logs(dateIso)")
          db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS vitals_records (
              id TEXT NOT NULL PRIMARY KEY,
              type TEXT NOT NULL,
              title TEXT NOT NULL,
              primaryValue REAL NOT NULL,
              secondaryValue REAL NOT NULL,
              tertiaryValue REAL NOT NULL,
              unit TEXT NOT NULL,
              notes TEXT NOT NULL,
              dateIso TEXT NOT NULL,
              refillRemaining INTEGER NOT NULL,
              recordedAtMillis INTEGER NOT NULL
            )
            """.trimIndent()
          )
          db.execSQL("CREATE INDEX IF NOT EXISTS index_vitals_records_type ON vitals_records(type)")
          db.execSQL("CREATE INDEX IF NOT EXISTS index_vitals_records_dateIso ON vitals_records(dateIso)")
          db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS gear_items (
              id TEXT NOT NULL PRIMARY KEY,
              name TEXT NOT NULL,
              brand TEXT NOT NULL,
              gearType TEXT NOT NULL,
              distanceMeters REAL NOT NULL,
              retireThresholdMeters REAL NOT NULL,
              isActive INTEGER NOT NULL,
              createdAtMillis INTEGER NOT NULL
            )
            """.trimIndent()
          )
        }
      }

    fun getInstance(context: Context): VitalTrackDatabase {
      return INSTANCE
        ?: synchronized(this) {
          INSTANCE
            ?: Room.databaseBuilder(
                context.applicationContext,
                VitalTrackDatabase::class.java,
                "vitaltrack_offline.db",
              )
              .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
              .fallbackToDestructiveMigration(dropAllTables = false)
              .build()
              .also { INSTANCE = it }
        }
    }
  }
}
