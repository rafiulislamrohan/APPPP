package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.example.data.health.HealthConnectAvailability
import com.example.data.health.StepSensorStatus
import com.example.data.health.StepSensorType
import com.example.data.health.WellnessCalculators
import com.example.data.local.DailyStepEntity
import com.example.data.local.GpsActivitySessionEntity
import com.example.data.local.HabitCompletionEntity
import com.example.data.local.HabitEntity
import com.example.data.local.UserProfilePreferences
import com.example.data.repository.WearOsBridgeStatus
import com.example.ui.screens.ActivityDetailsScreen
import com.example.ui.screens.AiCoachScreen
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.HabitTrackerScreen
import com.example.ui.screens.HealthDashboardScreen
import com.example.ui.screens.HomeDashboardScreen
import com.example.data.remote.OutfitCombination
import com.example.data.remote.StyleColorSwatch
import com.example.data.remote.StyleProfileInput
import com.example.data.remote.StyleRecommendationResult
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.Spatial3DStudioScreen
import com.example.ui.screens.StyleAdvisorScreen
import com.example.ui.screens.WellnessToolboxScreen
import com.example.ui.theme.VitalTrackTheme
import com.example.ui.viewmodel.AiCoachUiState
import com.example.ui.viewmodel.StyleAdvisorUiState
import com.example.ui.viewmodel.SyncUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class VitalTrackUiFlowTest {

  @get:Rule
  val composeRule = createComposeRule()

  @Test
  fun homeDashboard_displaysRealMetrics_offlineBanner_andQuickWaterCallback() {
    val today = WellnessCalculators.todayIso()
    var quickAddedMl = 0

    composeRule.setContent {
      VitalTrackTheme {
        HomeDashboardScreen(
          preferences = UserProfilePreferences(displayName = "Amina", dailyStepGoal = 8000),
          sensorStatus =
            StepSensorStatus(
              sensorType = StepSensorType.TYPE_STEP_COUNTER,
              permissionGranted = true,
              isListening = true,
              statusMessage = "Hardware counter active",
            ),
          dailySteps =
            listOf(
              DailyStepEntity(
                dateIso = today,
                steps = 4250,
                goal = 8000,
                distanceMeters = 3187.5,
                caloriesKcal = 170.0,
                activeMinutes = 42,
                source = "SENSOR_COUNTER",
              )
            ),
          waterIntakes = emptyList(),
          sleepRecords = emptyList(),
          gpsSessions = emptyList(),
          workoutSessions = emptyList(),
          isOnline = false,
          onOpenStepDetails = {},
          onOpenLiveTracking = {},
          onQuickAddWater = { quickAddedMl = it },
          onOpenWater = {},
          onOpenSleep = {},
          onOpenAiCoach = {},
          onOpenHabits = {},
          onOpenCalendar = {},
          onOpenAnalytics = {},
          onOpenSessionDetails = {},
        )
      }
    }

    composeRule.onNodeWithText("Hello, Amina").assertIsDisplayed()
    composeRule.onNodeWithTag("offline_status_banner").assertIsDisplayed()
    composeRule.onNodeWithTag("home_steps_card").assertIsDisplayed()
    composeRule.onNodeWithTag("home_water_250").performScrollTo().performClick()
    assertEquals(250, quickAddedMl)
  }

  @Test
  fun healthDashboard_showsMedicalDisclaimer_andSavesJournal() {
    var savedMood = ""
    var savedNote = ""

    composeRule.setContent {
      VitalTrackTheme {
        HealthDashboardScreen(
          preferences = UserProfilePreferences(),
          heartRates = emptyList(),
          sleepRecords = emptyList(),
          waterIntakes = emptyList(),
          weightEntries = emptyList(),
          journalEntries = emptyList(),
          healthConnectAvailability = HealthConnectAvailability.NOT_INSTALLED,
          syncState = SyncUiState.Idle,
          onSyncHealthConnect = {},
          onOpenPermissions = {},
          onOpenHeartRate = {},
          onOpenSleep = {},
          onOpenWater = {},
          onOpenWeight = {},
          onSaveJournal = { mood, _, note ->
            savedMood = mood
            savedNote = note
          },
        )
      }
    }

    composeRule.onNodeWithTag("medical_disclaimer_banner").assertIsDisplayed()
    composeRule.onNodeWithTag("save_journal_button").performScrollTo().performClick()
    assertEquals("GOOD", savedMood)
    assertEquals("", savedNote)
  }

  @Test
  fun habitTracker_rendersProgress_andTogglesHabit() {
    var toggledHabit = ""
    val habit =
      HabitEntity(
        id = "h_water",
        title = "Drink 2L Water",
        category = "WATER",
        targetDescription = "2000 ml",
        reminderTime = "10:00",
        createdAtMillis = 1000L,
      )

    composeRule.setContent {
      VitalTrackTheme {
        HabitTrackerScreen(
          habits = listOf(habit),
          completions =
            listOf(
              HabitCompletionEntity(
                id = "h_water|${WellnessCalculators.todayIso()}",
                habitId = "h_water",
                dateIso = WellnessCalculators.todayIso(),
                completedAtMillis = 2000L,
              )
            ),
          onToggleHabitToday = { toggledHabit = it },
          onAddHabit = { _, _, _, _ -> },
          onDeleteHabit = {},
          onBack = {},
        )
      }
    }

    composeRule.onNodeWithText("Today's Progress: 1 / 1 completed").assertIsDisplayed()
    composeRule.onNodeWithTag("habit_check_h_water").performClick()
    assertEquals("h_water", toggledHabit)
  }

  @Test
  fun aiCoach_whenOffline_disablesInputAndShowsOfflineNotice() {
    composeRule.setContent {
      VitalTrackTheme {
        AiCoachScreen(
          messages = emptyList(),
          coachState = AiCoachUiState.Idle,
          isOnline = false,
          onSendPrompt = { _, _ -> },
          onClearHistory = {},
          onBack = {},
        )
      }
    }

    composeRule.onNodeWithTag("medical_disclaimer_banner").assertIsDisplayed()
    composeRule.onNodeWithTag("offline_status_banner").assertIsDisplayed()
    composeRule.onNodeWithTag("ai_coach_send_button").assertIsNotEnabled()
  }

  @Test
  fun analyticsScreen_triggersCsvAndPdfExports() {
    var csvCalled = false
    var pdfCalled = false

    composeRule.setContent {
      VitalTrackTheme {
        AnalyticsScreen(
          preferences = UserProfilePreferences(),
          dailySteps = emptyList(),
          gpsSessions = emptyList(),
          workoutSessions = emptyList(),
          habitCompletions = emptyList(),
          onExportCsv = { csvCalled = true },
          onExportPdf = { pdfCalled = true },
          onBack = {},
        )
      }
    }

    composeRule.onNodeWithTag("analytics_export_csv").performScrollTo().performClick()
    composeRule.onNodeWithTag("analytics_export_pdf").performScrollTo().performClick()
    assertTrue(csvCalled)
    assertTrue(pdfCalled)
  }

  @Test
  fun activityDetailsScreen_triggersGpxExport() {
    var gpxSessionId = ""
    val session =
      GpsActivitySessionEntity(
        id = "gps_demo_gpx",
        activityType = "WALKING",
        title = "Lakefront Stroll",
        startTimeMillis = 1_700_000_000_000L,
        endTimeMillis = 1_700_000_600_000L,
        activeDurationMillis = 600_000L,
        distanceMeters = 1200.0,
        avgSpeedMps = 2.0,
        caloriesKcal = 85.0,
        steps = 1500,
        dateIso = "2026-11-19",
        routePolylineJson =
          """[{"latitude":23.8103,"longitude":90.4125,"altitudeMeters":10.0,"timestampMillis":1700000000000}]""",
      )

    composeRule.setContent {
      VitalTrackTheme {
        ActivityDetailsScreen(
          session = session,
          preferences = UserProfilePreferences(),
          isOnline = false,
          onExportGpsGpx = { gpxSessionId = it },
          onDeleteSession = {},
          onBack = {},
        )
      }
    }

    composeRule.onNodeWithTag("export_gpx_button").performScrollTo().performClick()
    assertEquals("gps_demo_gpx", gpxSessionId)
  }

  @Test
  fun settingsScreen_togglesWearOsSnapshotPayloadPreview() {
    composeRule.setContent {
      VitalTrackTheme {
        SettingsScreen(
          preferences = UserProfilePreferences(),
          firebaseUser = null,
          syncState = SyncUiState.Idle,
          wearOsStatus =
            WearOsBridgeStatus(
              companionAppDetected = false,
              dataLayerPath = "/vitaltrack/daily_snapshot",
              statusDescription = "Wear OS companion not installed on this device.",
            ),
          wearSnapshotJsonProvider = { """{"dateIso":"2026-11-19","steps":5200}""" },
          onThemeChange = {},
          onUnitsChange = {},
          onLanguageChange = {},
          onEditProfile = {},
          onOpenPermissions = {},
          onSyncCloudNow = {},
          onShowMessage = {},
          onBack = {},
        )
      }
    }

    composeRule.onNodeWithTag("wear_snapshot_preview_button").performScrollTo().performClick()
    composeRule
      .onNodeWithText("""{"dateIso":"2026-11-19","steps":5200}""")
      .performScrollTo()
      .assertIsDisplayed()
  }

  @Test
  fun wellnessTasksScreen_createsFiltersTogglesAndDeletesTasks() {
    var savedTitle = ""
    var savedCategory = ""
    var savedPriority = ""
    var savedReminder = false
    var addedCategoryName = ""
    var toggledId = ""
    var deletedId = ""

    val now = System.currentTimeMillis()
    val sampleTasks =
      listOf(
        com.example.data.local.WellnessTaskEntity(
          id = "t_pending",
          title = "Take Vitamin D",
          notes = "With breakfast",
          category = "MEDICATION",
          priority = "HIGH",
          dueAtMillis = now + 3_600_000L,
          reminderEnabled = true,
          isCompleted = false,
        ),
        com.example.data.local.WellnessTaskEntity(
          id = "t_low",
          title = "Evening Walk",
          category = "WELLNESS",
          priority = "LOW",
          dueAtMillis = now + 7_200_000L,
          reminderEnabled = false,
          isCompleted = false,
        ),
        com.example.data.local.WellnessTaskEntity(
          id = "t_done",
          title = "Stretch Quads",
          category = "WORKOUT",
          priority = "MEDIUM",
          dueAtMillis = now - 3_600_000L,
          reminderEnabled = false,
          isCompleted = true,
          completedAtMillis = now - 1_800_000L,
        ),
      )

    composeRule.setContent {
      VitalTrackTheme {
        com.example.ui.screens.WellnessTasksScreen(
          tasks = sampleTasks,
          onSaveTask = { _, title, _, category, priority, _, reminderEnabled ->
            savedTitle = title
            savedCategory = category
            savedPriority = priority
            savedReminder = reminderEnabled
          },
          onToggleCompleted = { toggledId = it },
          onDeleteTask = { deletedId = it },
          onAddCategory = { addedCategoryName = it },
          onBack = {},
        )
      }
    }

    // Default filter is PENDING: both high and low tasks visible with priority indicators
    composeRule.onNodeWithText("Take Vitamin D").assertIsDisplayed()
    composeRule.onNodeWithText("2 pending • 1 reminder(s) queued • 1 completed").assertIsDisplayed()
    composeRule
      .onNodeWithTag("task_priority_bar_t_pending", useUnmergedTree = true)
      .performScrollTo()
      .assertIsDisplayed()
    composeRule
      .onNodeWithTag("task_priority_badge_t_pending", useUnmergedTree = true)
      .performScrollTo()
      .assertIsDisplayed()

    // Filter by Priority = LOW
    composeRule.onNodeWithTag("task_priority_filter_low").performScrollTo().performClick()
    composeRule.onNodeWithText("Evening Walk").performScrollTo().assertIsDisplayed()
    composeRule.onNodeWithTag("task_priority_filter_all").performScrollTo().performClick()

    // Filter by Category = MEDICATION
    composeRule.onNodeWithTag("task_category_filter_medication").performScrollTo().performClick()
    composeRule.onNodeWithText("Take Vitamin D").performScrollTo().assertIsDisplayed()

    // Toggle completion & delete on filtered pending task
    composeRule.onNodeWithTag("task_check_t_pending").performScrollTo().performClick()
    assertEquals("t_pending", toggledId)
    composeRule.onNodeWithTag("task_delete_t_pending").performScrollTo().performClick()
    assertEquals("t_pending", deletedId)

    // Reset category filter and add a custom category
    composeRule.onNodeWithTag("task_add_category_chip").performScrollTo().performClick()
    composeRule.onNodeWithTag("task_new_category_input").performTextInput("Nutrition")
    composeRule.onNodeWithTag("task_new_category_save").performClick()
    assertEquals("Nutrition", addedCategoryName)
    composeRule.onNodeWithTag("task_category_filter_all").performScrollTo().performClick()

    // Switch to COMPLETED filter
    composeRule.onNodeWithTag("task_filter_completed").performScrollTo().performClick()
    composeRule.onNodeWithText("Stretch Quads").performScrollTo().assertIsDisplayed()

    // Open New Task dialog, select HIGH priority & HYDRATION category, enter title, and save
    composeRule.onNodeWithTag("add_task_fab").performClick()
    composeRule.onNodeWithTag("task_title_input").performTextInput("Evening Hydration Check")
    composeRule.onNodeWithTag("task_priority_high").performClick()
    composeRule.onNodeWithTag("task_category_hydration").performScrollTo().performClick()
    composeRule.onNodeWithTag("task_save_button").performClick()

    assertEquals("Evening Hydration Check", savedTitle)
    assertEquals("HIGH", savedPriority)
    assertEquals("HYDRATION", savedCategory)
    assertTrue(savedReminder)
  }

  @Test
  fun wellnessTasksScreen_searchBarFiltersByTitleAndContentAndClears() {
    val now = System.currentTimeMillis()
    val sampleTasks =
      listOf(
        com.example.data.local.WellnessTaskEntity(
          id = "t_vit",
          title = "Take Vitamin D",
          notes = "With breakfast and water",
          category = "MEDICATION",
          priority = "HIGH",
          dueAtMillis = now + 3_600_000L,
          reminderEnabled = true,
          isCompleted = false,
        ),
        com.example.data.local.WellnessTaskEntity(
          id = "t_walk",
          title = "Evening Stroll",
          notes = "Park trail loop after dinner",
          category = "WELLNESS",
          priority = "LOW",
          dueAtMillis = now + 7_200_000L,
          reminderEnabled = false,
          isCompleted = false,
        ),
      )

    composeRule.setContent {
      VitalTrackTheme {
        com.example.ui.screens.WellnessTasksScreen(
          tasks = sampleTasks,
          onSaveTask = { _, _, _, _, _, _, _ -> },
          onToggleCompleted = {},
          onDeleteTask = {},
          onAddCategory = {},
          onBack = {},
        )
      }
    }

    composeRule.onNodeWithText("Take Vitamin D").assertIsDisplayed()
    composeRule.onNodeWithText("Evening Stroll").assertIsDisplayed()

    // Filter by content/notes keyword ("breakfast")
    composeRule.onNodeWithTag("task_search_input").performTextInput("breakfast")
    composeRule.onNodeWithText("Take Vitamin D").assertIsDisplayed()
    composeRule.onNodeWithText("Evening Stroll").assertDoesNotExist()

    // Clear search query restores all pending items
    composeRule.onNodeWithTag("task_search_clear").performClick()
    composeRule.onNodeWithText("Evening Stroll").assertIsDisplayed()

    // Filter by title keyword ("Stroll")
    composeRule.onNodeWithTag("task_search_input").performTextInput("Stroll")
    composeRule.onNodeWithText("Evening Stroll").assertIsDisplayed()
    composeRule.onNodeWithText("Take Vitamin D").assertDoesNotExist()
  }

  @Test
  fun styleAdvisorScreen_rendersProfileSwatchesAndOutfits_andHandlesOfflineMode() {
    var capturedInput: StyleProfileInput? = null
    val sampleResult =
      StyleRecommendationResult(
        profileSummary = "Warm olive tones and clean vertical lines balance your 174 cm frame.",
        bestPaletteColors =
          listOf(
            StyleColorSwatch(name = "Forest Olive", hex = "#4A5D4E", role = "Base"),
            StyleColorSwatch(name = "Terracotta", hex = "#C26A4A", role = "Accent"),
          ),
        avoidColors = listOf("Icy Neon Pink"),
        outfitCombinations =
          listOf(
            OutfitCombination(
              title = "Earth-Tone Smart Casual",
              occasion = "Smart Casual",
              topPiece = "Relaxed linen overshirt",
              topColorName = "Forest Olive",
              topColorHex = "#4A5D4E",
              bottomPiece = "Straight-leg chinos",
              bottomColorName = "Warm Sand",
              bottomColorHex = "#D8C6AE",
              footwearOrLayer = "Tan leather loafers",
              accentColorName = "Cognac Tan",
              accentHex = "#9F5F3B",
              fitTip = "Slightly cropped overshirt elongates legs.",
              whyItWorks = "Warm earth shades brighten golden-warm skin undertones.",
            )
          ),
      )

    composeRule.setContent {
      VitalTrackTheme {
        StyleAdvisorScreen(
          preferences = UserProfilePreferences(heightCm = 174.0, weightKg = 70.0),
          advisorState = StyleAdvisorUiState.Success(sampleResult),
          isOnline = true,
          onGenerateSuggestions = { capturedInput = it },
          onBack = {},
        )
      }
    }

    composeRule.onNodeWithTag("style_tone_golden_olive").performScrollTo().performClick()
    composeRule.onNodeWithTag("style_occasion_evening_out").performScrollTo().performClick()
    composeRule.onNodeWithTag("style_generate_button").performScrollTo().performClick()

    assertEquals("Golden Olive", capturedInput?.skinToneLabel)
    assertEquals("Olive", capturedInput?.undertone)
    assertEquals("Evening Out", capturedInput?.occasion)
    assertEquals(174.0, capturedInput?.heightCm ?: 0.0, 0.1)

    composeRule.onNodeWithTag("style_summary_card").performScrollTo().assertIsDisplayed()
    composeRule.onNodeWithTag("style_outfit_card_0").performScrollTo().assertIsDisplayed()
    composeRule.onNodeWithText("Earth-Tone Smart Casual").assertIsDisplayed()
  }

  @Test
  fun styleAdvisorScreen_whenOffline_disablesGenerateButtonAndShowsBanner() {
    composeRule.setContent {
      VitalTrackTheme {
        StyleAdvisorScreen(
          preferences = UserProfilePreferences(),
          advisorState = StyleAdvisorUiState.Idle,
          isOnline = false,
          onGenerateSuggestions = {},
          onBack = {},
        )
      }
    }

    composeRule.onNodeWithTag("offline_status_banner").assertIsDisplayed()
    composeRule.onNodeWithTag("style_generate_button").performScrollTo().assertIsNotEnabled()
  }

  @Test
  fun spatial3DStudioScreen_switchesAcrossAllThree3DModesAndResetsAngle() {
    composeRule.setContent {
      VitalTrackTheme {
        Spatial3DStudioScreen(
          preferences = UserProfilePreferences(heightCm = 175.0, weightKg = 70.0),
          dailySteps = emptyList(),
          waterIntakes = emptyList(),
          sleepRecords = emptyList(),
          wellnessTasks = emptyList(),
          gpsSessions = emptyList(),
          onBack = {},
        )
      }
    }

    composeRule.onNodeWithTag("spatial_orb_canvas").assertIsDisplayed()
    composeRule.onNodeWithTag("spatial_mode_body").performScrollTo().performClick()
    composeRule.onNodeWithTag("spatial_body_canvas").assertIsDisplayed()
    composeRule.onNodeWithTag("spatial_mode_terrain").performScrollTo().performClick()
    composeRule.onNodeWithTag("spatial_terrain_canvas").assertIsDisplayed()
    composeRule.onNodeWithTag("spatial_3d_reset").performScrollTo().performClick()
  }

  @Test
  fun wellnessToolboxScreen_appliesHydrationGoal_andLogs3DBreathingSession() {
    var appliedGoalMl = 0
    var loggedProtocol = ""
    var loggedCycles = 0

    composeRule.setContent {
      VitalTrackTheme {
        WellnessToolboxScreen(
          preferences = UserProfilePreferences(age = 30, weightKg = 70.0, dailyWorkoutGoalMinutes = 30),
          onApplyWaterGoalMl = { appliedGoalMl = it },
          onLogBreathingSession = { protocol, cycles ->
            loggedProtocol = protocol
            loggedCycles = cycles
          },
          onBack = {},
        )
      }
    }

    composeRule.onNodeWithTag("breathing_protocol_calm").performScrollTo().performClick()
    composeRule.onNodeWithTag("breathing_log_button").performScrollTo().performClick()
    assertEquals("Calm 4-7-8", loggedProtocol)
    assertEquals(1, loggedCycles)

    composeRule.onNodeWithTag("calc_apply_water_goal").performScrollTo().performClick()
    assertEquals(2600, appliedGoalMl)
  }

  @Test
  fun wellnessTasksScreen_snoozePlusOneDayTriggersCallback() {
    var snoozedId = ""
    val now = System.currentTimeMillis()
    val task =
      com.example.data.local.WellnessTaskEntity(
        id = "t_snooze",
        title = "Evening Hydration",
        category = "HYDRATION",
        priority = "MEDIUM",
        dueAtMillis = now + 3_600_000L,
        reminderEnabled = true,
        isCompleted = false,
      )

    composeRule.setContent {
      VitalTrackTheme {
        com.example.ui.screens.WellnessTasksScreen(
          tasks = listOf(task),
          onSaveTask = { _, _, _, _, _, _, _ -> },
          onToggleCompleted = {},
          onDeleteTask = {},
          onSnoozeTask = { snoozedId = it },
          onAddCategory = {},
          onBack = {},
        )
      }
    }

    composeRule.onNodeWithTag("task_snooze_t_snooze", useUnmergedTree = true).performClick()
    assertEquals("t_snooze", snoozedId)
  }

  @Test
  fun wellnessTasksScreen_switchesToEisenhowerMatrixView() {
    val now = System.currentTimeMillis()
    val tasks =
      listOf(
        com.example.data.local.WellnessTaskEntity(
          id = "t_m1",
          title = "Hydrate Immediately",
          category = "HYDRATION",
          priority = "HIGH",
          dueAtMillis = now + 3_600_000L,
          reminderEnabled = true,
          isCompleted = false,
        )
      )
    composeRule.setContent {
      VitalTrackTheme {
        com.example.ui.screens.WellnessTasksScreen(
          tasks = tasks,
          onSaveTask = { _, _, _, _, _, _, _ -> },
          onToggleCompleted = {},
          onDeleteTask = {},
          onAddCategory = {},
          onBack = {},
        )
      }
    }

    composeRule.onNodeWithTag("task_view_matrix").performScrollTo().performClick()
    composeRule.onNodeWithText("Eisenhower 2×2 Priority Matrix").performScrollTo().assertIsDisplayed()
    composeRule.onNodeWithText("Q1 Do First (1)\n• Hydrate Immediately").performScrollTo().assertIsDisplayed()
  }

  @Test
  fun nutritionHubScreen_logsMealAndSchedulesMealPrep() {
    var loggedMeal = ""
    var prepScheduled = false

    composeRule.setContent {
      VitalTrackTheme {
        com.example.ui.screens.NutritionHubScreen(
          preferences = UserProfilePreferences(age = 30, heightCm = 175.0, weightKg = 70.0),
          nutritionLogs = emptyList(),
          mealPhotoState = com.example.ui.viewmodel.MealPhotoUiState.Idle,
          isOnline = false,
          onLogNutrition = { mealName, _, _, _, _, _, _, _, _, _, _ -> loggedMeal = mealName },
          onDeleteNutrition = {},
          onAnalyzeMealPhoto = {},
          onSetFastingState = { _, _ -> },
          onScheduleMealPrepTask = { prepScheduled = true },
          onBack = {},
        )
      }
    }

    composeRule.onNodeWithTag("nutrition_meal_name").performScrollTo().performTextInput("Chicken Bowl")
    composeRule.onNodeWithTag("nutrition_log_meal_btn").performScrollTo().performClick()
    assertEquals("Chicken Bowl", loggedMeal)

    composeRule.onNodeWithTag("nutrition_schedule_prep_btn").performScrollTo().performClick()
    assertTrue(prepScheduled)
  }

  @Test
  fun vitalsClinicalScreen_savesBloodPressureAndExportsPhysicianPdf() {
    var savedBpSys = 0.0
    var pdfExported = false

    composeRule.setContent {
      VitalTrackTheme {
        com.example.ui.screens.VitalsClinicalScreen(
          preferences = UserProfilePreferences(heightCm = 175.0),
          vitalsRecords = emptyList(),
          weightEntries = emptyList(),
          onAddVitalsRecord = { type, _, p1, _, _, _, _, _ -> if (type == "BP") savedBpSys = p1 },
          onDecrementRefill = {},
          onDeleteVitalsRecord = {},
          onSaveBodyMeasurements = { _, _, _ -> },
          onExportPhysicianPdf = { pdfExported = true },
          onBack = {},
        )
      }
    }

    composeRule.onNodeWithTag("vitals_save_bp_btn").performScrollTo().performClick()
    assertEquals(118.0, savedBpSys, 0.1)

    composeRule.onNodeWithTag("vitals_export_physician_pdf").performClick()
    assertTrue(pdfExported)
  }

  @Test
  fun spatialGalleryScreen_switchesTo3DCityCanvas() {
    composeRule.setContent {
      VitalTrackTheme {
        com.example.ui.screens.SpatialGalleryScreen(
          preferences = UserProfilePreferences(),
          dailySteps = emptyList(),
          waterIntakes = emptyList(),
          sleepRecords = emptyList(),
          heartRates = emptyList(),
          workoutSessions = emptyList(),
          habitCompletions = emptyList(),
          onSaveBodyMorph = { _, _, _ -> },
          onBack = {},
        )
      }
    }
    composeRule.onNodeWithTag("gallery_tab_city").performScrollTo().performClick()
    composeRule.onNodeWithTag("gallery_city_canvas").assertIsDisplayed()
  }

  @Test
  fun wardrobeClosetScreen_addsGarmentItem() {
    var addedClosetItem = ""

    composeRule.setContent {
      VitalTrackTheme {
        com.example.ui.screens.WardrobeClosetScreen(
          preferences = UserProfilePreferences(),
          wardrobeItems = emptyList(),
          dressCheckState = com.example.ui.viewmodel.DressCheckUiState.Idle,
          isOnline = false,
          onAddWardrobeItem = { name, _, _, _, _, _ -> addedClosetItem = name },
          onMarkWorn = {},
          onToggleFavorite = {},
          onDeleteItem = {},
          onCheckOutfitPhoto = { _, _ -> },
          onBack = {},
        )
      }
    }
    composeRule.onNodeWithTag("closet_item_name").performScrollTo().performTextInput("Linen Shirt")
    composeRule.onNodeWithTag("closet_add_btn").performScrollTo().performClick()
    assertEquals("Linen Shirt", addedClosetItem)
  }

  @Test
  fun recoveryStudioScreen_togglesEyeBreakAndLogsStressTrigger() {
    var eyeBreakEnabled = false
    var stressLoggedTag = ""

    composeRule.setContent {
      VitalTrackTheme {
        com.example.ui.screens.RecoveryStudioScreen(
          preferences = UserProfilePreferences(),
          sleepRecords = emptyList(),
          heartRates = emptyList(),
          onSetWindDownTime = { _, _ -> },
          onToggleScreenBreakReminder = { eyeBreakEnabled = it },
          onSaveStressTriggerJournal = { tag, _ -> stressLoggedTag = tag },
          onBack = {},
        )
      }
    }

    composeRule.onNodeWithTag("recovery_eyebreak_switch").performScrollTo().performClick()
    composeRule.onNodeWithTag("recovery_log_stress_btn").performScrollTo().performClick()
    assertTrue(eyeBreakEnabled)
    assertEquals("Screen Fatigue", stressLoggedTag)
  }

  @Test
  fun diagnosticsLabScreen_togglesBiometricLockAndSimulatesNfc() {
    var toggledBiometric = false
    var simulatedNfc = false

    composeRule.setContent {
      VitalTrackTheme {
        com.example.ui.screens.DiagnosticsLabScreen(
          preferences = UserProfilePreferences(),
          sensorStatus = StepSensorStatus(StepSensorType.TYPE_STEP_COUNTER, true, true, "OK"),
          healthConnectAvailability = HealthConnectAvailability.AVAILABLE,
          wearOsStatus = WearOsBridgeStatus(true, "/vitaltrack/daily_snapshot", "Connected"),
          onAccentPaletteChange = {},
          onBiometricLockChange = { toggledBiometric = it },
          onAutoBackupChange = {},
          onRunAutoBackupNow = {},
          onNfcModeChange = {},
          onSimulateNfcTap = { simulatedNfc = true },
          onWriteHealthConnect = {},
          onImportCsvOrJsonUri = {},
          onBack = {},
        )
      }
    }
    composeRule.onNodeWithTag("diag_biometric_switch").performScrollTo().performClick()
    composeRule.onNodeWithTag("diag_nfc_test_btn").performScrollTo().performClick()
    assertTrue(toggledBiometric)
    assertTrue(simulatedNfc)
  }
}
