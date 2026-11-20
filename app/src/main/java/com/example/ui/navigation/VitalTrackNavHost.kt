package com.example.ui.navigation

import android.content.Intent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.outlined.DirectionsRun
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.R
import com.example.auth.AuthManager.authStateFlow
import com.example.data.local.TaskPriority
import com.example.data.local.TaskRecurrence
import com.example.ui.screens.ActiveWorkoutScreen
import com.example.ui.screens.ActivityDetailsScreen
import com.example.ui.screens.ActivityHistoryScreen
import com.example.ui.screens.AiCoachScreen
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.CalendarScreen
import com.example.ui.screens.DiagnosticsLabScreen
import com.example.ui.screens.ExerciseLibraryScreen
import com.example.ui.screens.HabitTrackerScreen
import com.example.ui.screens.HealthDashboardScreen
import com.example.ui.screens.HeartRateScreen
import com.example.ui.screens.HomeDashboardScreen
import com.example.ui.screens.LiveTrackingScreen
import com.example.ui.screens.MapTimelineScreen
import com.example.ui.screens.NotificationsScreen
import com.example.ui.screens.NutritionHubScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.PermissionSetupScreen
import com.example.ui.screens.PrivacyDataScreen
import com.example.ui.screens.ProfileHubScreen
import com.example.ui.screens.ProfileSetupScreen
import com.example.ui.screens.RecoveryStudioScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SleepDashboardScreen
import com.example.ui.screens.Spatial3DStudioScreen
import com.example.ui.screens.SpatialGalleryScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.StepDetailsScreen
import com.example.ui.screens.StyleAdvisorScreen
import com.example.ui.screens.VitalsClinicalScreen
import com.example.ui.screens.WardrobeClosetScreen
import com.example.ui.screens.WaterTrackerScreen
import com.example.ui.screens.WeightTrackerScreen
import com.example.ui.screens.WellnessTasksScreen
import com.example.ui.screens.WellnessToolboxScreen
import com.example.ui.screens.WorkoutDetailsScreen
import com.example.ui.viewmodel.VitalTrackViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

private data class TopLevelDestination(
  val route: VitalTrackRoute,
  val labelRes: Int,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector,
  val tag: String,
)

@Composable
fun VitalTrackRoot(viewModel: VitalTrackViewModel) {
  val navController = rememberNavController()
  val context = LocalContext.current
  val snackbarHostState = remember { SnackbarHostState() }

  val preferences by viewModel.preferences.collectAsStateWithLifecycle()
  val sensorStatus by viewModel.sensorStatus.collectAsStateWithLifecycle()
  val dailySteps by viewModel.dailySteps.collectAsStateWithLifecycle()
  val gpsSessions by viewModel.gpsSessions.collectAsStateWithLifecycle()
  val activeTracking by viewModel.activeTracking.collectAsStateWithLifecycle()
  val heartRates by viewModel.heartRates.collectAsStateWithLifecycle()
  val sleepRecords by viewModel.sleepRecords.collectAsStateWithLifecycle()
  val waterIntakes by viewModel.waterIntakes.collectAsStateWithLifecycle()
  val weightEntries by viewModel.weightEntries.collectAsStateWithLifecycle()
  val journalEntries by viewModel.journalEntries.collectAsStateWithLifecycle()
  val exercises by viewModel.exercises.collectAsStateWithLifecycle()
  val workoutPlans by viewModel.workoutPlans.collectAsStateWithLifecycle()
  val workoutSessions by viewModel.workoutSessions.collectAsStateWithLifecycle()
  val habits by viewModel.habits.collectAsStateWithLifecycle()
  val habitCompletions by viewModel.habitCompletions.collectAsStateWithLifecycle()
  val aiMessages by viewModel.aiMessages.collectAsStateWithLifecycle()
  val aiCoachState by viewModel.aiCoachState.collectAsStateWithLifecycle()
  val styleAdvisorState by viewModel.styleAdvisorState.collectAsStateWithLifecycle()
  val notificationConfigs by viewModel.notificationConfigs.collectAsStateWithLifecycle()
  val wellnessTasks by viewModel.wellnessTasks.collectAsStateWithLifecycle()
  val taskCategories by viewModel.taskCategories.collectAsStateWithLifecycle()
  val wardrobeItems by viewModel.wardrobeItems.collectAsStateWithLifecycle()
  val nutritionLogs by viewModel.nutritionLogs.collectAsStateWithLifecycle()
  val vitalsRecords by viewModel.vitalsRecords.collectAsStateWithLifecycle()
  val gearItems by viewModel.gearItems.collectAsStateWithLifecycle()
  val mealPhotoState by viewModel.mealPhotoState.collectAsStateWithLifecycle()
  val dressCheckState by viewModel.dressCheckState.collectAsStateWithLifecycle()
  val habitDigestState by viewModel.habitDigestState.collectAsStateWithLifecycle()
  val syncState by viewModel.syncState.collectAsStateWithLifecycle()
  val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
  val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()

  val firebaseUser by remember { Firebase.auth.authStateFlow() }.collectAsStateWithLifecycle(
    initialValue = Firebase.auth.currentUser
  )

  LaunchedEffect(snackbarMessage) {
    val msg = snackbarMessage
    if (!msg.isNullOrBlank()) {
      snackbarHostState.showSnackbar(msg)
      viewModel.consumeSnackbar()
    }
  }

  val topDestinations =
    remember {
      listOf(
        TopLevelDestination(
          route = VitalTrackRoute.Home,
          labelRes = R.string.nav_home,
          selectedIcon = Icons.Filled.Home,
          unselectedIcon = Icons.Outlined.Home,
          tag = "nav_tab_home",
        ),
        TopLevelDestination(
          route = VitalTrackRoute.ActivityHistory,
          labelRes = R.string.nav_activity,
          selectedIcon = Icons.AutoMirrored.Filled.DirectionsRun,
          unselectedIcon = Icons.AutoMirrored.Outlined.DirectionsRun,
          tag = "nav_tab_activity",
        ),
        TopLevelDestination(
          route = VitalTrackRoute.MapTimeline,
          labelRes = R.string.nav_map,
          selectedIcon = Icons.Filled.Map,
          unselectedIcon = Icons.Outlined.Map,
          tag = "nav_tab_map",
        ),
        TopLevelDestination(
          route = VitalTrackRoute.HealthDashboard,
          labelRes = R.string.nav_health,
          selectedIcon = Icons.Filled.Favorite,
          unselectedIcon = Icons.Outlined.FavoriteBorder,
          tag = "nav_tab_health",
        ),
        TopLevelDestination(
          route = VitalTrackRoute.ProfileHub,
          labelRes = R.string.nav_profile,
          selectedIcon = Icons.Filled.Person,
          unselectedIcon = Icons.Outlined.Person,
          tag = "nav_tab_profile",
        ),
      )
    }

  val navBackStackEntry by navController.currentBackStackEntryAsState()
  val currentDest = navBackStackEntry?.destination
  val showPrimaryChrome =
    currentDest != null &&
      topDestinations.any { dest -> currentDest.hasRoute(dest.route::class) }

  fun navigateTopLevel(route: VitalTrackRoute) {
    navController.navigate(route) {
      popUpTo(navController.graph.findStartDestination().id) { saveState = true }
      launchSingleTop = true
      restoreState = true
    }
  }

  val launchShareIntent: (Intent) -> Unit = { shareIntent ->
    runCatching {
      context.startActivity(Intent.createChooser(shareIntent, "Share VITALTRACK Report"))
    }.onFailure {
      viewModel.showMessage(it.localizedMessage ?: "No share target available")
    }
  }

  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val useRail = maxWidth >= 600.dp

    Scaffold(
      contentWindowInsets = WindowInsets.safeDrawing,
      snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
      bottomBar = {
        if (showPrimaryChrome && !useRail) {
          NavigationBar {
            topDestinations.forEach { dest ->
              val selected = currentDest?.hasRoute(dest.route::class) == true
              NavigationBarItem(
                selected = selected,
                onClick = { navigateTopLevel(dest.route) },
                icon = {
                  Icon(
                    imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                    contentDescription = stringResource(dest.labelRes),
                  )
                },
                label = { Text(stringResource(dest.labelRes)) },
                modifier = Modifier.testTag(dest.tag),
              )
            }
          }
        }
      },
    ) { innerPadding ->
      Row(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
        if (showPrimaryChrome && useRail) {
          NavigationRail {
            topDestinations.forEach { dest ->
              val selected = currentDest?.hasRoute(dest.route::class) == true
              NavigationRailItem(
                selected = selected,
                onClick = { navigateTopLevel(dest.route) },
                icon = {
                  Icon(
                    imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                    contentDescription = stringResource(dest.labelRes),
                  )
                },
                label = { Text(stringResource(dest.labelRes)) },
                modifier = Modifier.testTag(dest.tag),
              )
            }
          }
        }

        Box(
          modifier = Modifier.weight(1f).fillMaxSize(),
          contentAlignment = Alignment.TopCenter,
        ) {
          NavHost(
            navController = navController,
            startDestination = VitalTrackRoute.Splash,
          ) {
            // 1. Splash
            composable<VitalTrackRoute.Splash> {
              SplashScreen(
                preferences = preferences,
                onNavigateNext = { onboardingDone, profileDone ->
                  val target: VitalTrackRoute =
                    when {
                      !onboardingDone -> VitalTrackRoute.Onboarding
                      !profileDone -> VitalTrackRoute.ProfileSetup(fromSettings = false)
                      else -> VitalTrackRoute.Home
                    }
                  navController.navigate(target) {
                    popUpTo(VitalTrackRoute.Splash) { inclusive = true }
                  }
                },
              )
            }

            // 2. Onboarding
            composable<VitalTrackRoute.Onboarding> {
              OnboardingScreen(
                onComplete = {
                  viewModel.completeOnboarding()
                  navController.navigate(VitalTrackRoute.ProfileSetup(fromSettings = false)) {
                    popUpTo(VitalTrackRoute.Onboarding) { inclusive = true }
                  }
                }
              )
            }

            // 3. Profile Setup
            composable<VitalTrackRoute.ProfileSetup> { backStackEntry ->
              val args = backStackEntry.toRoute<VitalTrackRoute.ProfileSetup>()
              ProfileSetupScreen(
                preferences = preferences,
                fromSettings = args.fromSettings,
                onSaveProfile = {
                  name,
                  age,
                  height,
                  weight,
                  stride,
                  stepGoal,
                  waterGoal,
                  sleepGoal,
                  workoutGoal,
                  imperial,
                  lang,
                  eName,
                  ePhone ->
                  viewModel.saveProfileSetup(
                    displayName = name,
                    age = age,
                    heightCm = height,
                    weightKg = weight,
                    strideLengthMeters = stride,
                    dailyStepGoal = stepGoal,
                    dailyWaterGoalMl = waterGoal,
                    dailySleepGoalMinutes = sleepGoal,
                    dailyWorkoutGoalMinutes = workoutGoal,
                    useImperialUnits = imperial,
                    languageCode = lang,
                    emergencyContactName = eName,
                    emergencyContactPhone = ePhone,
                  )
                  if (args.fromSettings) {
                    navController.popBackStack()
                  } else {
                    navController.navigate(VitalTrackRoute.PermissionSetup(fromSettings = false)) {
                      popUpTo(VitalTrackRoute.ProfileSetup(fromSettings = false)) { inclusive = true }
                    }
                  }
                },
                onBack = { navController.popBackStack() },
              )
            }

            // 4. Permission Setup
            composable<VitalTrackRoute.PermissionSetup> { backStackEntry ->
              val args = backStackEntry.toRoute<VitalTrackRoute.PermissionSetup>()
              PermissionSetupScreen(
                fromSettings = args.fromSettings,
                healthConnectAvailability = viewModel.healthConnectAvailability(),
                onPermissionsUpdated = { viewModel.onStepPermissionsUpdated() },
                onContinue = {
                  if (args.fromSettings) {
                    navController.popBackStack()
                  } else {
                    navController.navigate(VitalTrackRoute.Home) {
                      popUpTo(VitalTrackRoute.PermissionSetup(fromSettings = false)) { inclusive = true }
                    }
                  }
                },
              )
            }

            // 5. Home Dashboard (Tab 1)
            composable<VitalTrackRoute.Home> {
              HomeDashboardScreen(
                preferences = preferences,
                sensorStatus = sensorStatus,
                dailySteps = dailySteps,
                waterIntakes = waterIntakes,
                sleepRecords = sleepRecords,
                gpsSessions = gpsSessions,
                workoutSessions = workoutSessions,
                isOnline = isOnline,
                pendingTasksCount = wellnessTasks.count { !it.isCompleted },
                onOpenStepDetails = { navController.navigate(VitalTrackRoute.StepDetails) },
                onOpenLiveTracking = { navController.navigate(VitalTrackRoute.LiveTracking()) },
                onQuickAddWater = { viewModel.addWater(it) },
                onOpenWater = { navController.navigate(VitalTrackRoute.WaterTracker) },
                onOpenSleep = { navController.navigate(VitalTrackRoute.SleepDashboard) },
                onOpenAiCoach = { navController.navigate(VitalTrackRoute.AiCoach) },
                onOpenStyleAdvisor = { navController.navigate(VitalTrackRoute.StyleAdvisor) },
                onOpenSpatial3D = { navController.navigate(VitalTrackRoute.Spatial3D) },
                onOpenToolbox = { navController.navigate(VitalTrackRoute.WellnessToolbox) },
                onOpenHabits = { navController.navigate(VitalTrackRoute.HabitTracker) },
                onOpenTasks = { navController.navigate(VitalTrackRoute.Tasks) },
                onOpenCalendar = { navController.navigate(VitalTrackRoute.Calendar) },
                onOpenAnalytics = { navController.navigate(VitalTrackRoute.Analytics) },
                onOpenSessionDetails = { id -> navController.navigate(VitalTrackRoute.ActivityDetails(id)) },
              )
            }

            // 6. Step Counter Details
            composable<VitalTrackRoute.StepDetails> {
              StepDetailsScreen(
                preferences = preferences,
                sensorStatus = sensorStatus,
                dailySteps = dailySteps,
                onUpdateStepGoal = { viewModel.setStepGoal(it) },
                onUpdateStrideLength = { viewModel.setStrideLength(it) },
                onBack = { navController.popBackStack() },
              )
            }

            // 7. Activity History (Tab 2)
            composable<VitalTrackRoute.ActivityHistory> {
              ActivityHistoryScreen(
                preferences = preferences,
                gpsSessions = gpsSessions,
                workoutSessions = workoutSessions,
                onOpenExerciseLibrary = { navController.navigate(VitalTrackRoute.ExerciseLibrary) },
                onOpenLiveTracking = { navController.navigate(VitalTrackRoute.LiveTracking()) },
                onOpenStepDetails = { navController.navigate(VitalTrackRoute.StepDetails) },
                onOpenSessionDetails = { id -> navController.navigate(VitalTrackRoute.ActivityDetails(id)) },
              )
            }

            // 8. Live GPS Tracking
            composable<VitalTrackRoute.LiveTracking> { backStackEntry ->
              val args = backStackEntry.toRoute<VitalTrackRoute.LiveTracking>()
              LiveTrackingScreen(
                initialMode = args.initialMode,
                preferences = preferences,
                trackingState = activeTracking,
                isOnline = isOnline,
                onStart = { viewModel.startGpsTracking(it) },
                onPause = { viewModel.pauseGpsTracking() },
                onResume = { viewModel.resumeGpsTracking() },
                onStop = {
                  viewModel.stopGpsTracking()
                  navController.popBackStack()
                },
                onBack = { navController.popBackStack() },
              )
            }

            // 9. Map Timeline (Tab 3)
            composable<VitalTrackRoute.MapTimeline> {
              MapTimelineScreen(
                preferences = preferences,
                gpsSessions = gpsSessions,
                isOnline = isOnline,
                onStartLiveTracking = { navController.navigate(VitalTrackRoute.LiveTracking()) },
                onOpenSessionDetails = { id -> navController.navigate(VitalTrackRoute.ActivityDetails(id)) },
                onMapRegionCached = { viewModel.recordCachedMapRegion(it) },
                onShowMessage = { viewModel.showMessage(it) },
                gearItems = gearItems,
                onAddGearItem = { name, brand, retireKm -> viewModel.addGearItem(name, brand, retireKm) },
                onAddGearKm = { id, km -> viewModel.addGearDistance(id, km) },
              )
            }

            // 10. Activity Details & Replay
            composable<VitalTrackRoute.ActivityDetails> { backStackEntry ->
              val args = backStackEntry.toRoute<VitalTrackRoute.ActivityDetails>()
              val session = gpsSessions.firstOrNull { it.id == args.sessionId }
              ActivityDetailsScreen(
                session = session,
                preferences = preferences,
                isOnline = isOnline,
                onExportGpsGpx = { viewModel.exportGpsGpx(it, launchShareIntent) },
                onDeleteSession = { viewModel.deleteGpsSession(it) },
                onBack = { navController.popBackStack() },
              )
            }

            // 11. Health Dashboard (Tab 4)
            composable<VitalTrackRoute.HealthDashboard> {
              HealthDashboardScreen(
                preferences = preferences,
                heartRates = heartRates,
                sleepRecords = sleepRecords,
                waterIntakes = waterIntakes,
                weightEntries = weightEntries,
                journalEntries = journalEntries,
                healthConnectAvailability = viewModel.healthConnectAvailability(),
                syncState = syncState,
                onSyncHealthConnect = { viewModel.syncHealthConnect() },
                onOpenPermissions = {
                  navController.navigate(VitalTrackRoute.PermissionSetup(fromSettings = true))
                },
                onOpenHeartRate = { navController.navigate(VitalTrackRoute.HeartRate) },
                onOpenSleep = { navController.navigate(VitalTrackRoute.SleepDashboard) },
                onOpenWater = { navController.navigate(VitalTrackRoute.WaterTracker) },
                onOpenWeight = { navController.navigate(VitalTrackRoute.WeightTracker) },
                onSaveJournal = { mood, energy, note -> viewModel.addJournalEntry(mood, energy, note) },
              )
            }

            // 12. Heart Rate History
            composable<VitalTrackRoute.HeartRate> {
              HeartRateScreen(
                heartRates = heartRates,
                healthConnectAvailability = viewModel.healthConnectAvailability(),
                onSyncHealthConnect = { viewModel.syncHealthConnect() },
                onBack = { navController.popBackStack() },
              )
            }

            // 13. Sleep Dashboard
            composable<VitalTrackRoute.SleepDashboard> {
              SleepDashboardScreen(
                preferences = preferences,
                sleepRecords = sleepRecords,
                onLogManualSleep = { dur, qual, notes ->
                  viewModel.addManualSleep(dur, qual, notes)
                },
                onSyncHealthConnect = { viewModel.syncHealthConnect() },
                onBack = { navController.popBackStack() },
              )
            }

            // 14. Water Tracker
            composable<VitalTrackRoute.WaterTracker> {
              WaterTrackerScreen(
                preferences = preferences,
                waterIntakes = waterIntakes,
                onAddWater = { viewModel.addWater(it) },
                onDeleteWater = { viewModel.deleteWater(it) },
                onBack = { navController.popBackStack() },
              )
            }

            // 15. Weight Tracker
            composable<VitalTrackRoute.WeightTracker> {
              WeightTrackerScreen(
                preferences = preferences,
                weightEntries = weightEntries,
                onAddWeight = { kg, notes -> viewModel.addWeight(kg, notes) },
                onBack = { navController.popBackStack() },
              )
            }

            // 16. Exercise Library
            composable<VitalTrackRoute.ExerciseLibrary> {
              ExerciseLibraryScreen(
                exercises = exercises,
                workoutPlans = workoutPlans,
                workoutSessions = workoutSessions,
                onOpenWorkoutDetails = { id -> navController.navigate(VitalTrackRoute.WorkoutDetails(id)) },
                onCreateCustomPlan = { title, cat, desc, ids ->
                  viewModel.createCustomWorkoutPlan(title, cat, desc, ids)
                },
                onBack = { navController.popBackStack() },
              )
            }

            // 17. Workout Details
            composable<VitalTrackRoute.WorkoutDetails> { backStackEntry ->
              val args = backStackEntry.toRoute<VitalTrackRoute.WorkoutDetails>()
              val plan = workoutPlans.firstOrNull { it.id == args.workoutId }
              WorkoutDetailsScreen(
                plan = plan,
                allExercises = exercises,
                onStartActiveWorkout = { id -> navController.navigate(VitalTrackRoute.ActiveWorkout(id)) },
                onBack = { navController.popBackStack() },
              )
            }

            // 18. Active Workout
            composable<VitalTrackRoute.ActiveWorkout> { backStackEntry ->
              val args = backStackEntry.toRoute<VitalTrackRoute.ActiveWorkout>()
              val plan = workoutPlans.firstOrNull { it.id == args.workoutId }
              ActiveWorkoutScreen(
                plan = plan,
                allExercises = exercises,
                onFinishSession = { p, durSec, sets, reps ->
                  viewModel.saveWorkoutSession(p, durSec, sets, reps)
                  navController.popBackStack()
                },
                onBack = { navController.popBackStack() },
              )
            }

            // 19. Habit Tracker
            composable<VitalTrackRoute.HabitTracker> {
              HabitTrackerScreen(
                habits = habits,
                completions = habitCompletions,
                onToggleHabitToday = { viewModel.toggleHabitToday(it) },
                onAddHabit = { title, cat, target, time ->
                  viewModel.addHabit(title, cat, target, time)
                },
                onDeleteHabit = { viewModel.deleteHabit(it) },
                preferences = preferences,
                habitDigestState = habitDigestState,
                isOnline = isOnline,
                onUseStreakFreeze = { viewModel.useStreakFreezeToken() },
                onSelectChallenge = { viewModel.setActiveChallenge(it) },
                onGenerateWeeklyDigest = { viewModel.generateWeeklyHabitDigest() },
                onAddHabitExtended = { title, cat, target, time, freq, stack, polarity ->
                  viewModel.addHabit(title, cat, target, time, freq, stack, polarity)
                },
                onBack = { navController.popBackStack() },
              )
            }

            // 20. Calendar
            composable<VitalTrackRoute.Calendar> {
              CalendarScreen(
                dailySteps = dailySteps,
                gpsSessions = gpsSessions,
                workoutSessions = workoutSessions,
                habitCompletions = habitCompletions,
                onBack = { navController.popBackStack() },
              )
            }

            // 21. Analytics
            composable<VitalTrackRoute.Analytics> {
              AnalyticsScreen(
                preferences = preferences,
                dailySteps = dailySteps,
                gpsSessions = gpsSessions,
                workoutSessions = workoutSessions,
                habitCompletions = habitCompletions,
                onExportCsv = { viewModel.exportCsv(launchShareIntent) },
                onExportPdf = { viewModel.exportPdf(launchShareIntent) },
                onBack = { navController.popBackStack() },
              )
            }

            // 22. AI Wellness Coach
            composable<VitalTrackRoute.AiCoach> {
              AiCoachScreen(
                messages = aiMessages,
                coachState = aiCoachState,
                isOnline = isOnline,
                onSendPrompt = { prompt, topic -> viewModel.askAiCoach(prompt, topic) },
                onClearHistory = { viewModel.clearAiHistory() },
                onBack = { navController.popBackStack() },
              )
            }

            // 23. Notifications & Reminders
            composable<VitalTrackRoute.Notifications> {
              NotificationsScreen(
                preferences = preferences,
                configs = notificationConfigs,
                onUpdateConfig = { viewModel.updateNotificationConfig(it) },
                onBack = { navController.popBackStack() },
              )
            }

            // 24a. Profile Hub (Tab 5)
            composable<VitalTrackRoute.ProfileHub> {
              ProfileHubScreen(
                preferences = preferences,
                firebaseUser = firebaseUser,
                onOpenHabits = { navController.navigate(VitalTrackRoute.HabitTracker) },
                onOpenTasks = { navController.navigate(VitalTrackRoute.Tasks) },
                onOpenCalendar = { navController.navigate(VitalTrackRoute.Calendar) },
                onOpenAnalytics = { navController.navigate(VitalTrackRoute.Analytics) },
                onOpenAiCoach = { navController.navigate(VitalTrackRoute.AiCoach) },
                onOpenStyleAdvisor = { navController.navigate(VitalTrackRoute.StyleAdvisor) },
                onOpenSpatial3D = { navController.navigate(VitalTrackRoute.Spatial3D) },
                onOpenSpatialGallery = { navController.navigate(VitalTrackRoute.SpatialGallery) },
                onOpenToolbox = { navController.navigate(VitalTrackRoute.WellnessToolbox) },
                onOpenNutritionHub = { navController.navigate(VitalTrackRoute.NutritionHub) },
                onOpenVitalsClinical = { navController.navigate(VitalTrackRoute.VitalsClinical) },
                onOpenRecoveryStudio = { navController.navigate(VitalTrackRoute.RecoveryStudio) },
                onOpenWardrobeCloset = { navController.navigate(VitalTrackRoute.WardrobeCloset) },
                onOpenDiagnosticsLab = { navController.navigate(VitalTrackRoute.DiagnosticsLab) },
                onOpenNotifications = { navController.navigate(VitalTrackRoute.Notifications) },
                onOpenSettings = { navController.navigate(VitalTrackRoute.Settings) },
                onOpenPrivacyData = { navController.navigate(VitalTrackRoute.PrivacyData) },
              )
            }

            // 24b. Settings
            composable<VitalTrackRoute.Settings> {
              SettingsScreen(
                preferences = preferences,
                firebaseUser = firebaseUser,
                syncState = syncState,
                wearOsStatus = viewModel.wearOsStatus(),
                wearSnapshotJsonProvider = { viewModel.buildWearOsDailySnapshotJson() },
                onThemeChange = { viewModel.setThemePreference(it) },
                onUnitsChange = { viewModel.setUseImperialUnits(it) },
                onLanguageChange = { viewModel.setLanguageCode(it) },
                onThreeDVisualsChange = { viewModel.setThreeDVisualsEnabled(it) },
                onEditProfile = {
                  navController.navigate(VitalTrackRoute.ProfileSetup(fromSettings = true))
                },
                onOpenPermissions = {
                  navController.navigate(VitalTrackRoute.PermissionSetup(fromSettings = true))
                },
                onSyncCloudNow = { viewModel.syncCloudNow() },
                onShowMessage = { viewModel.showMessage(it) },
                onBack = { navController.popBackStack() },
              )
            }

            // 25. Privacy & Data Control
            composable<VitalTrackRoute.PrivacyData> {
              PrivacyDataScreen(
                firebaseUser = firebaseUser,
                onExportBackupJson = { viewModel.exportBackupJson(launchShareIntent) },
                onRestoreBackupUri = { viewModel.restoreBackupFromUri(it) },
                onExportCsv = { viewModel.exportCsv(launchShareIntent) },
                onExportPdf = { viewModel.exportPdf(launchShareIntent) },
                onDeleteCloudData = { viewModel.deleteAllCloudData() },
                onDeleteAllLocalData = { viewModel.deleteAllLocalData() },
                onBack = { navController.popBackStack() },
              )
            }

            // 26. Scheduled Wellness Tasks (Room + WorkManager)
            composable<VitalTrackRoute.Tasks> {
              WellnessTasksScreen(
                tasks = wellnessTasks,
                categories = taskCategories,
                onSaveTask = { existingId, title, notes, category, priority, dueAtMillis, reminderEnabled ->
                  viewModel.saveWellnessTask(
                    existingId = existingId,
                    title = title,
                    notes = notes,
                    category = category,
                    priority = priority,
                    dueAtMillis = dueAtMillis,
                    reminderEnabled = reminderEnabled,
                  )
                },
                onToggleCompleted = { viewModel.toggleWellnessTaskCompleted(it) },
                onDeleteTask = { viewModel.deleteWellnessTask(it) },
                onSnoozeTask = { viewModel.snoozeWellnessTaskOneDay(it) },
                onAddCategory = { viewModel.addTaskCategory(it) },
                onSaveTaskAdvanced = { existingId, title, notes, category, priority, dueAtMillis, reminderEnabled, recurrenceRule, subtasksJson, geofenceLabel, autoEscalate ->
                  viewModel.saveWellnessTask(
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
                    autoEscalatePriority = autoEscalate,
                  )
                },
                onUpdateSubtasksAndFocus = { taskId, subtasksJson, addedFocusMin ->
                  viewModel.updateTaskSubtasksAndFocus(taskId, subtasksJson, addedFocusMin)
                },
                onBack = { navController.popBackStack() },
              )
            }

            // 27. Live Smart AI Clothing & Dress Color Combinations
            composable<VitalTrackRoute.StyleAdvisor> {
              StyleAdvisorScreen(
                preferences = preferences,
                advisorState = styleAdvisorState,
                isOnline = isOnline,
                onGenerateSuggestions = { viewModel.generateStyleSuggestions(it) },
                onOpenCloset = { navController.navigate(VitalTrackRoute.WardrobeCloset) },
                onBack = { navController.popBackStack() },
              )
            }

            // 28. Interactive 3D Wellness, Body & GPS Terrain Studio
            composable<VitalTrackRoute.Spatial3D> {
              Spatial3DStudioScreen(
                preferences = preferences,
                dailySteps = dailySteps,
                waterIntakes = waterIntakes,
                sleepRecords = sleepRecords,
                wellnessTasks = wellnessTasks,
                gpsSessions = gpsSessions,
                onBack = { navController.popBackStack() },
              )
            }

            // 29. 3D Guided Breathing & Smart Wellness Calculators
            composable<VitalTrackRoute.WellnessToolbox> {
              WellnessToolboxScreen(
                preferences = preferences,
                onApplyWaterGoalMl = { viewModel.setWaterGoalMl(it) },
                onLogBreathingSession = { protocol, cycles ->
                  viewModel.logBreathingSession(protocol, cycles)
                },
                onBack = { navController.popBackStack() },
              )
            }

            // 30. 3D Pro Spatial Gallery
            composable<VitalTrackRoute.SpatialGallery> {
              SpatialGalleryScreen(
                preferences = preferences,
                dailySteps = dailySteps,
                waterIntakes = waterIntakes,
                sleepRecords = sleepRecords,
                heartRates = heartRates,
                workoutSessions = workoutSessions,
                habitCompletions = habitCompletions,
                onSaveBodyMorph = { chest, waist, hip -> viewModel.setBodyMorphDimensions(chest, waist, hip) },
                onBack = { navController.popBackStack() },
              )
            }

            // 31. Nutrition, Fuel & Fasting Hub
            composable<VitalTrackRoute.NutritionHub> {
              NutritionHubScreen(
                preferences = preferences,
                nutritionLogs = nutritionLogs,
                mealPhotoState = mealPhotoState,
                isOnline = isOnline,
                onLogNutrition = { mealName, mealType, kcal, protein, carbs, fat, fiber, bevMl, hydFactor, caffeine, electrolytes ->
                  viewModel.addNutritionLog(
                    mealName = mealName,
                    mealType = mealType,
                    caloriesKcal = kcal,
                    proteinG = protein,
                    carbsG = carbs,
                    fatG = fat,
                    fiberG = fiber,
                    beverageMl = bevMl,
                    hydrationFactor = hydFactor,
                    caffeineMg = caffeine,
                    electrolytesMg = electrolytes,
                  )
                },
                onDeleteNutrition = { viewModel.deleteNutritionLog(it) },
                onAnalyzeMealPhoto = { viewModel.analyzeMealPhoto(it) },
                onSetFastingState = { active, hours -> viewModel.setFastingState(active, hours) },
                onScheduleMealPrepTask = {
                  viewModel.saveWellnessTask(
                    title = "Sunday Batch Meal Prep",
                    notes = "Prep high-protein meals & groceries for the week",
                    category = "NUTRITION",
                    priority = TaskPriority.MEDIUM.storageKey,
                    dueAtMillis = System.currentTimeMillis() + 86_400_000L,
                    reminderEnabled = true,
                    recurrenceRule = TaskRecurrence.WEEKLY.storageKey,
                  )
                },
                onBack = { navController.popBackStack() },
              )
            }

            // 32. Clinical Vitals & Physician PDF
            composable<VitalTrackRoute.VitalsClinical> {
              VitalsClinicalScreen(
                preferences = preferences,
                vitalsRecords = vitalsRecords,
                weightEntries = weightEntries,
                onAddVitalsRecord = { type, title, p1, p2, p3, unit, notes, refill ->
                  viewModel.addVitalsRecord(type, title, p1, p2, p3, unit, notes, refill)
                },
                onDecrementRefill = { viewModel.decrementMedicationRefill(it) },
                onDeleteVitalsRecord = { viewModel.deleteVitalsRecord(it) },
                onSaveBodyMeasurements = { chest, waist, hip -> viewModel.setBodyMorphDimensions(chest, waist, hip) },
                onExportPhysicianPdf = { viewModel.exportPhysicianPdf(launchShareIntent) },
                onBack = { navController.popBackStack() },
              )
            }

            // 33. Recovery Studio & Biofeedback Soundscapes
            composable<VitalTrackRoute.RecoveryStudio> {
              RecoveryStudioScreen(
                preferences = preferences,
                sleepRecords = sleepRecords,
                heartRates = heartRates,
                onSetWindDownTime = { h, m -> viewModel.setWindDownTime(h, m) },
                onToggleScreenBreakReminder = { viewModel.setScreenBreakReminderEnabled(it) },
                onSaveStressTriggerJournal = { tag, note ->
                  viewModel.addJournalEntry(mood = "RECOVERY", energyLevel = 3, note = "[$tag] $note")
                },
                onBack = { navController.popBackStack() },
              )
            }

            // 34. Digital Wardrobe & AI Dress Check
            composable<VitalTrackRoute.WardrobeCloset> {
              WardrobeClosetScreen(
                preferences = preferences,
                wardrobeItems = wardrobeItems,
                dressCheckState = dressCheckState,
                isOnline = isOnline,
                onAddWardrobeItem = { name, cat, colorName, colorHex, season, occasion ->
                  viewModel.addWardrobeItem(name, cat, colorName, colorHex, season, occasion)
                },
                onMarkWorn = { viewModel.markWardrobeItemWorn(it) },
                onToggleFavorite = { viewModel.toggleWardrobeFavorite(it) },
                onDeleteItem = { viewModel.deleteWardrobeItem(it) },
                onCheckOutfitPhoto = { bitmap, occasion -> viewModel.checkOutfitPhoto(bitmap, occasion) },
                onBack = { navController.popBackStack() },
              )
            }

            // 35. Platform Integrations, Security & Diagnostics Lab
            composable<VitalTrackRoute.DiagnosticsLab> {
              DiagnosticsLabScreen(
                preferences = preferences,
                sensorStatus = sensorStatus,
                healthConnectAvailability = viewModel.healthConnectAvailability(),
                wearOsStatus = viewModel.wearOsStatus(),
                onAccentPaletteChange = { viewModel.setAccentPalette(it) },
                onBiometricLockChange = { viewModel.setBiometricLockEnabled(it) },
                onAutoBackupChange = { viewModel.setAutoBackupEnabled(it) },
                onRunAutoBackupNow = { viewModel.triggerAutoBackupNow() },
                onNfcModeChange = { viewModel.setNfcQuickActionMode(it) },
                onSimulateNfcTap = { viewModel.handleNfcTrigger() },
                onWriteHealthConnect = { viewModel.writeHealthConnectSnapshot() },
                onImportCsvOrJsonUri = { viewModel.importCsvOrJsonFromUri(it) },
                onBack = { navController.popBackStack() },
              )
            }
          }
        }
      }
    }
  }
}
