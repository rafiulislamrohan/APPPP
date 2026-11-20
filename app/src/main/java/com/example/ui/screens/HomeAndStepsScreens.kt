package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.ViewInAr
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.health.StepSensorStatus
import com.example.data.health.StepSensorType
import com.example.data.health.WellnessCalculators
import com.example.data.local.DailyStepEntity
import com.example.data.local.GpsActivitySessionEntity
import com.example.data.local.SleepRecordEntity
import com.example.data.local.UserProfilePreferences
import com.example.data.local.WaterIntakeEntity
import com.example.data.local.WorkoutSessionEntity
import com.example.ui.components.BarChartDatum
import com.example.ui.components.MetricSummaryCard
import com.example.ui.components.OfflineStatusBanner
import com.example.ui.components.StepProgressRing
import com.example.ui.components.VitalTopBar
import com.example.ui.components.WeeklyBarChart
import com.example.ui.components.rememberSensorTilt3D
import com.example.ui.components.tactile3D
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.HydrationBlue
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.SleepIndigo
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

// 5. Home Dashboard Screen
@Composable
fun HomeDashboardScreen(
  preferences: UserProfilePreferences,
  sensorStatus: StepSensorStatus,
  dailySteps: List<DailyStepEntity>,
  waterIntakes: List<WaterIntakeEntity>,
  sleepRecords: List<SleepRecordEntity>,
  gpsSessions: List<GpsActivitySessionEntity>,
  workoutSessions: List<WorkoutSessionEntity>,
  isOnline: Boolean,
  pendingTasksCount: Int = 0,
  onOpenStepDetails: () -> Unit,
  onOpenLiveTracking: () -> Unit,
  onQuickAddWater: (Int) -> Unit,
  onOpenWater: () -> Unit,
  onOpenSleep: () -> Unit,
  onOpenAiCoach: () -> Unit,
  onOpenStyleAdvisor: () -> Unit = {},
  onOpenSpatial3D: () -> Unit = {},
  onOpenToolbox: () -> Unit = {},
  onOpenHabits: () -> Unit,
  onOpenTasks: () -> Unit = {},
  onOpenCalendar: () -> Unit,
  onOpenAnalytics: () -> Unit,
  onOpenSessionDetails: (String) -> Unit,
) {
  val todayIso = remember { WellnessCalculators.todayIso() }
  val todayStepEntity = dailySteps.firstOrNull { it.dateIso == todayIso }
  val todaySteps = todayStepEntity?.steps ?: 0
  val todayDistance =
    todayStepEntity?.distanceMeters
      ?: WellnessCalculators.estimateDistanceMeters(todaySteps, preferences.strideLengthMeters)
  val todayCalories =
    todayStepEntity?.caloriesKcal
      ?: WellnessCalculators.estimateStepCalories(todaySteps, preferences.weightKg)
  val todayActiveMin =
    todayStepEntity?.activeMinutes ?: WellnessCalculators.estimateActiveMinutesFromSteps(todaySteps)

  val todayWaterMl = waterIntakes.filter { it.dateIso == todayIso }.sumOf { it.amountMl }
  val latestSleep = sleepRecords.firstOrNull()
  val recentGps = gpsSessions.firstOrNull()
  val recentWorkout = workoutSessions.firstOrNull()

  val last7 = remember(dailySteps) {
    val map = dailySteps.associateBy { it.dateIso }
    WellnessCalculators.lastNDaysIso(7).map { iso ->
      val dayName =
        runCatching {
          LocalDate.parse(iso).dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault())
        }.getOrDefault(iso.takeLast(2))
      BarChartDatum(
        label = dayName,
        value = (map[iso]?.steps ?: 0).toFloat(),
        isToday = iso == todayIso,
      )
    }
  }

  val sensorTilt by rememberSensorTilt3D(enabled = preferences.threeDVisualsEnabled)
  val tiltX = if (preferences.threeDVisualsEnabled) sensorTilt.first else 0f
  val tiltY = if (preferences.threeDVisualsEnabled) sensorTilt.second else 0f

  Column(
    modifier =
      Modifier.fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 16.dp, vertical = 12.dp)
        .widthIn(max = 640.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    // Editorial Hero Banner with live 3D gyro parallax
    Box(
      modifier =
        Modifier.fillMaxWidth()
          .height(156.dp)
          .tactile3D(
            enabled = preferences.threeDVisualsEnabled,
            tiltX = tiltX,
            tiltY = tiltY,
            elevationDp = 12.dp,
            cornerRadiusDp = 24.dp,
          )
          .clip(MaterialTheme.shapes.large),
    ) {
      Image(
        painter = painterResource(id = R.drawable.img_wellness_hero),
        contentDescription = "Morning wellness trail",
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Crop,
      )
      Box(
        modifier =
          Modifier.fillMaxSize()
            .background(
              Brush.verticalGradient(
                listOf(Color.Black.copy(alpha = 0.25f), Color.Black.copy(alpha = 0.70f))
              )
            )
            .padding(18.dp),
      ) {
        Column(modifier = Modifier.align(Alignment.BottomStart)) {
          Text(
            text = "Hello, ${preferences.displayName}",
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
          )
          Text(
            text = stringResource(R.string.app_tagline),
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.9f),
          )
        }
      }
    }

    if (!isOnline) {
      OfflineStatusBanner()
    }

    // Primary Steps Ring Card
    Card(
      modifier =
        Modifier.fillMaxWidth()
          .tactile3D(
            enabled = preferences.threeDVisualsEnabled,
            tiltX = tiltX * 0.4f,
            tiltY = tiltY * 0.4f,
            elevationDp = 8.dp,
            cornerRadiusDp = 24.dp,
          )
          .clip(MaterialTheme.shapes.large)
          .clickable(onClick = onOpenStepDetails)
          .testTag("home_steps_card"),
      shape = MaterialTheme.shapes.large,
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
      Row(
        modifier = Modifier.padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        StepProgressRing(steps = todaySteps, goal = preferences.dailyStepGoal, size = 140.dp)
        Spacer(modifier = Modifier.width(18.dp))
        Column(
          modifier = Modifier.weight(1f),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              Icons.Outlined.Sensors,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text =
                when (sensorStatus.sensorType) {
                  StepSensorType.TYPE_STEP_COUNTER -> "Hardware Step Counter"
                  StepSensorType.TYPE_STEP_DETECTOR -> "Step Detector Active"
                  StepSensorType.UNAVAILABLE -> "Sensor Unavailable"
                },
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.primary,
            )
          }
          Text(
            text =
              "${WellnessCalculators.formatDistance(todayDistance, preferences.useImperialUnits)} • ${todayCalories.toInt()} kcal",
            style = MaterialTheme.typography.titleMedium,
          )
          Text(
            text = "$todayActiveMin ${stringResource(R.string.active_minutes_label)}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          FilledTonalButton(
            onClick = onOpenLiveTracking,
            modifier = Modifier.fillMaxWidth().testTag("home_start_gps_button"),
          ) {
            Icon(Icons.Outlined.LocationOn, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(stringResource(R.string.start_gps_session))
          }
        }
      }
    }

    // 2x2 Summary Metrics
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      MetricSummaryCard(
        title = stringResource(R.string.water_label),
        value = WellnessCalculators.formatVolume(todayWaterMl, preferences.useImperialUnits),
        subtitle = "Goal: ${WellnessCalculators.formatVolume(preferences.dailyWaterGoalMl, preferences.useImperialUnits)}",
        icon = Icons.Outlined.WaterDrop,
        accentColor = HydrationBlue,
        onClick = onOpenWater,
        modifier = Modifier.weight(1f),
      )
      MetricSummaryCard(
        title = stringResource(R.string.sleep_label),
        value =
          if (latestSleep != null) WellnessCalculators.formatMinutesToHours(latestSleep.durationMinutes)
          else "No record",
        subtitle = latestSleep?.let { "${it.source} • ${it.dateIso}" } ?: "Tap to log sleep",
        icon = Icons.Outlined.Bedtime,
        accentColor = SleepIndigo,
        onClick = onOpenSleep,
        modifier = Modifier.weight(1f),
      )
    }

    // Quick Water Add Bar
    Card(
      shape = MaterialTheme.shapes.medium,
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
      Column(modifier = Modifier.padding(14.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text("Quick Hydration", style = MaterialTheme.typography.titleMedium)
          TextButton(onClick = onOpenWater) { Text("Details") }
        }
        val waterProgress =
          (todayWaterMl.toFloat() / preferences.dailyWaterGoalMl.coerceAtLeast(1)).coerceIn(0f, 1f)
        LinearProgressIndicator(
          progress = { waterProgress },
          modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
          color = HydrationBlue,
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          listOf(150, 250, 500).forEach { ml ->
            OutlinedButton(
              onClick = { onQuickAddWater(ml) },
              modifier = Modifier.weight(1f).testTag("home_water_$ml"),
            ) {
              Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
              Text("${ml}ml")
            }
          }
        }
      }
    }

    // Weekly Progress Chart
    Card(
      shape = MaterialTheme.shapes.medium,
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text("7-Day Step Progress", style = MaterialTheme.typography.titleMedium)
          TextButton(onClick = onOpenAnalytics) { Text("Analytics") }
        }
        Spacer(modifier = Modifier.height(8.dp))
        WeeklyBarChart(data = last7, goalValue = preferences.dailyStepGoal.toFloat())
      }
    }

    // Today's Timeline & Recent Workout
    Card(
      shape = MaterialTheme.shapes.medium,
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
      Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Text("Recent Activity & Shortcuts", style = MaterialTheme.typography.titleMedium)
        if (recentGps != null) {
          Surface(
            modifier =
              Modifier.fillMaxWidth()
                .clip(MaterialTheme.shapes.small)
                .clickable { onOpenSessionDetails(recentGps.id) },
            color = MaterialTheme.colorScheme.surfaceVariant,
          ) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Outlined.Route, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
              Spacer(modifier = Modifier.width(10.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(recentGps.title, style = MaterialTheme.typography.titleSmall)
                Text(
                  "${WellnessCalculators.formatDistance(recentGps.distanceMeters, preferences.useImperialUnits)} • ${WellnessCalculators.formatDurationMillis(recentGps.activeDurationMillis)}",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }
        }
        if (recentWorkout != null) {
          Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.small,
          ) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Outlined.FitnessCenter, contentDescription = null, tint = GoldAccent)
              Spacer(modifier = Modifier.width(10.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(recentWorkout.title, style = MaterialTheme.typography.titleSmall)
                Text(
                  "${recentWorkout.durationSeconds / 60} min • ${recentWorkout.caloriesKcal.toInt()} kcal • ${recentWorkout.dateIso}",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }
        }
        if (recentGps == null && recentWorkout == null) {
          Text(
            text = "No GPS routes or workouts recorded yet. Start a walk, run, or indoor session anytime.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          FilledTonalButton(
            onClick = onOpenAiCoach,
            modifier = Modifier.weight(1f).testTag("home_ai_coach_button"),
          ) {
            Icon(Icons.Outlined.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("AI Coach")
          }
          OutlinedButton(
            onClick = onOpenHabits,
            modifier = Modifier.weight(1f).testTag("home_habits_button"),
          ) {
            Icon(Icons.Outlined.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(stringResource(R.string.habits_label))
          }
        }
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          OutlinedButton(
            onClick = onOpenTasks,
            modifier = Modifier.weight(1f).testTag("home_tasks_button"),
          ) {
            Icon(Icons.Outlined.TaskAlt, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(if (pendingTasksCount > 0) "Tasks ($pendingTasksCount)" else "Tasks")
          }
          OutlinedButton(
            onClick = onOpenCalendar,
            modifier = Modifier.weight(1f).testTag("home_calendar_button"),
          ) {
            Icon(Icons.Outlined.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Calendar")
          }
        }
        FilledTonalButton(
          onClick = onOpenStyleAdvisor,
          modifier = Modifier.fillMaxWidth().testTag("home_style_advisor_button"),
        ) {
          Icon(Icons.Outlined.Checkroom, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("AI Outfit & Dress Colors")
        }
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          OutlinedButton(
            onClick = onOpenSpatial3D,
            modifier = Modifier.weight(1f).testTag("home_3d_studio_button"),
          ) {
            Icon(Icons.Outlined.ViewInAr, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("3D Studio")
          }
          OutlinedButton(
            onClick = onOpenToolbox,
            modifier = Modifier.weight(1f).testTag("home_toolbox_button"),
          ) {
            Icon(Icons.Outlined.Air, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Breath & Calc")
          }
        }
      }
    }
    Spacer(modifier = Modifier.height(16.dp))
  }
}

// 6. Step Counter Details Screen
@Composable
fun StepDetailsScreen(
  preferences: UserProfilePreferences,
  sensorStatus: StepSensorStatus,
  dailySteps: List<DailyStepEntity>,
  onUpdateStepGoal: (Int) -> Unit,
  onUpdateStrideLength: (Double) -> Unit,
  onBack: () -> Unit,
) {
  BackHandler(onBack = onBack)

  val todayIso = remember { WellnessCalculators.todayIso() }
  val todayEntity = dailySteps.firstOrNull { it.dateIso == todayIso }
  val todaySteps = todayEntity?.steps ?: 0
  var periodDays by remember { mutableIntStateOf(7) } // 1 = Daily, 7 = Weekly, 30 = Monthly
  var goalDraft by remember(preferences.dailyStepGoal) { mutableIntStateOf(preferences.dailyStepGoal) }
  var strideDraft by remember(preferences.strideLengthMeters) {
    mutableStateOf(preferences.strideLengthMeters.toFloat())
  }

  val chartData =
    remember(dailySteps, periodDays) {
      val map = dailySteps.associateBy { it.dateIso }
      WellnessCalculators.lastNDaysIso(periodDays).mapIndexed { idx, iso ->
        val showLabel =
          when (periodDays) {
            1 -> "Today"
            7 -> iso.takeLast(2)
            else -> if (idx % 5 == 0 || idx == periodDays - 1) iso.takeLast(2) else ""
          }
        BarChartDatum(
          label = showLabel,
          value = (map[iso]?.steps ?: 0).toFloat(),
          isToday = iso == todayIso,
        )
      }
    }

  Scaffold(topBar = { VitalTopBar(title = "Step Counter Details", onBack = onBack) }) { innerPadding ->
    Column(
      modifier =
        Modifier.fillMaxSize()
          .padding(innerPadding)
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 16.dp, vertical = 12.dp)
          .widthIn(max = 640.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      // Sensor Availability Card
      Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
      ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
          Icon(
            Icons.Outlined.Sensors,
            contentDescription = "Sensor Status",
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
          )
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text(
              text = "Sensor: ${sensorStatus.sensorType.name}",
              style = MaterialTheme.typography.titleSmall,
              color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
              text = sensorStatus.statusMessage,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
          }
        }
      }

      Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        StepProgressRing(steps = todaySteps, goal = preferences.dailyStepGoal, size = 176.dp)
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        MetricSummaryCard(
          title = stringResource(R.string.distance_label),
          value =
            WellnessCalculators.formatDistance(
              WellnessCalculators.estimateDistanceMeters(todaySteps, preferences.strideLengthMeters),
              preferences.useImperialUnits,
            ),
          subtitle = "Stride ${String.format(Locale.US, "%.2f m", preferences.strideLengthMeters)}",
          icon = Icons.Outlined.Route,
          accentColor = MaterialTheme.colorScheme.primary,
          modifier = Modifier.weight(1f),
        )
        MetricSummaryCard(
          title = stringResource(R.string.calories_label),
          value = "${WellnessCalculators.estimateStepCalories(todaySteps, preferences.weightKg).toInt()} kcal",
          subtitle = "0.04 kcal/step × mass ratio",
          icon = Icons.Outlined.LocalFireDepartment,
          accentColor = GoldAccent,
          modifier = Modifier.weight(1f),
        )
      }

      // Period Switcher & Chart
      Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(1 to "Daily", 7 to "Weekly", 30 to "Monthly").forEach { (days, label) ->
              FilterChip(
                selected = periodDays == days,
                onClick = { periodDays = days },
                label = { Text(label) },
              )
            }
          }
          Spacer(modifier = Modifier.height(8.dp))
          WeeklyBarChart(data = chartData, goalValue = preferences.dailyStepGoal.toFloat())
        }
      }

      // Configurable Goal & Stride
      Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Text("Custom Step Goal & Stride", style = MaterialTheme.typography.titleMedium)
          Text(
            "Daily Goal: $goalDraft steps",
            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = JetBrainsMonoFamily),
          )
          Slider(
            value = goalDraft.toFloat(),
            onValueChange = { goalDraft = ((it / 500).toInt() * 500).coerceIn(2000, 25000) },
            valueRange = 2000f..25000f,
            onValueChangeFinished = { onUpdateStepGoal(goalDraft) },
          )
          Text(
            "Stride Length: ${String.format(Locale.US, "%.2f m", strideDraft)}",
            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = JetBrainsMonoFamily),
          )
          Slider(
            value = strideDraft,
            onValueChange = { strideDraft = it },
            valueRange = 0.45f..1.10f,
            onValueChangeFinished = { onUpdateStrideLength(strideDraft.toDouble()) },
          )
          Text(
            text = "Calorie assumption: ~0.04 kcal per step at 70 kg body weight, scaled by your profile weight (${preferences.weightKg} kg).",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }
  }
}
