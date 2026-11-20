package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.health.BiofeedbackAudioEngine
import com.example.data.health.WellnessAdvancedCalculators
import com.example.data.health.WellnessCalculators
import com.example.data.local.DailyStepEntity
import com.example.data.local.ExerciseEntity
import com.example.data.local.GpsActivitySessionEntity
import com.example.data.local.HabitCompletionEntity
import com.example.data.local.HabitEntity
import com.example.data.local.UserProfilePreferences
import com.example.data.local.WorkoutPlanEntity
import com.example.data.local.WorkoutSessionEntity
import com.example.data.repository.VitalTrackRepository
import com.example.ui.components.VitalTopBar
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.viewmodel.HabitDigestUiState
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs

private val exerciseJson = Json { ignoreUnknownKeys = true }

private fun parseExerciseIds(raw: String): List<String> =
  runCatching { exerciseJson.decodeFromString<List<String>>(raw) }.getOrDefault(emptyList())

// 16. Exercise Library & Custom Routine Builder
@Composable
fun ExerciseLibraryScreen(
  exercises: List<ExerciseEntity>,
  workoutPlans: List<WorkoutPlanEntity>,
  workoutSessions: List<WorkoutSessionEntity>,
  onOpenWorkoutDetails: (String) -> Unit,
  onCreateCustomPlan: (title: String, category: String, description: String, exerciseIds: List<String>) -> Unit,
  onBack: () -> Unit,
) {
  BackHandler(onBack = onBack)

  val categories =
    listOf(
      "ALL" to "All",
      "WALKING" to "Walking",
      "RUNNING" to "Running",
      "CYCLING" to "Cycling",
      "STRENGTH" to "Strength",
      "BODYWEIGHT" to "Bodyweight",
      "STRETCHING" to "Stretching",
      "YOGA" to "Yoga",
      "INDOOR" to "Indoor",
    )
  var selectedCategory by remember { mutableStateOf("ALL") }
  var expandedExerciseId by remember { mutableStateOf<String?>(null) }
  var showBuilderDialog by remember { mutableStateOf(false) }

  val filteredPlans =
    remember(workoutPlans, selectedCategory) {
      if (selectedCategory == "ALL") workoutPlans
      else workoutPlans.filter { it.category == selectedCategory }
    }
  val filteredExercises =
    remember(exercises, selectedCategory) {
      if (selectedCategory == "ALL") exercises
      else exercises.filter { it.category == selectedCategory }
    }

  Scaffold(
    topBar = {
      VitalTopBar(
        title = "Exercise & Workouts",
        onBack = onBack,
        actions = {
          TextButton(
            onClick = { showBuilderDialog = true },
            modifier = Modifier.testTag("create_custom_workout_button"),
          ) {
            Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Text("Custom")
          }
        },
      )
    }
  ) { innerPadding ->
    LazyColumn(
      modifier =
        Modifier.fillMaxSize()
          .padding(innerPadding)
          .padding(horizontal = 16.dp)
          .widthIn(max = 640.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      item {
        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          categories.forEach { (key, label) ->
            FilterChip(
              selected = selectedCategory == key,
              onClick = { selectedCategory = key },
              label = { Text(label) },
            )
          }
        }
      }

      if (workoutSessions.isNotEmpty()) {
        item {
          val weekDates = remember { WellnessCalculators.lastNDaysIso(7).toSet() }
          val monthDates = remember { WellnessCalculators.lastNDaysIso(28).toSet() }
          val weekSessions = workoutSessions.filter { it.dateIso in weekDates }
          val monthSessions = workoutSessions.filter { it.dateIso in monthDates }
          val totalMin = weekSessions.sumOf { it.durationSeconds } / 60
          val monthMin = monthSessions.sumOf { it.durationSeconds } / 60
          val (acwr, acwrStatus) = WellnessAdvancedCalculators.calculateAcwr(totalMin, monthMin)
          Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.FitnessCenter, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                  "This week: ${weekSessions.size} workouts • $totalMin active minutes",
                  style = MaterialTheme.typography.titleSmall,
                  color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
              }
              Text(
                "ACWR Training Load: ${String.format(Locale.US, "%.2f", acwr)} ($acwrStatus)",
                style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFamily),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
              )
            }
          }
        }
      }

      // #34 1-Rep Max & #39 VO2 Max Performance Calculator Card
      item {
        var oneRmWeight by remember { mutableStateOf("60") }
        var oneRmReps by remember { mutableStateOf("8") }
        var rhrForVo2 by remember { mutableStateOf("62") }
        val est1Rm = WellnessAdvancedCalculators.estimateOneRepMaxKg(
          oneRmWeight.toDoubleOrNull() ?: 60.0,
          oneRmReps.toIntOrNull() ?: 8,
        )
        val estVo2 = WellnessAdvancedCalculators.estimateVo2Max(30, rhrForVo2.toIntOrNull() ?: 62)
        Card(shape = MaterialTheme.shapes.medium, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
          Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Strength 1RM & VO2 Max Estimator", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              OutlinedTextField(value = oneRmWeight, onValueChange = { oneRmWeight = it }, label = { Text("Load kg") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), singleLine = true)
              OutlinedTextField(value = oneRmReps, onValueChange = { oneRmReps = it }, label = { Text("Reps") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), singleLine = true)
              OutlinedTextField(value = rhrForVo2, onValueChange = { rhrForVo2 = it }, label = { Text("RHR bpm") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), singleLine = true)
            }
            Text(
              "Est. 1RM: ${String.format(Locale.US, "%.1f", est1Rm)} kg • Est. VO2 Max: ${String.format(Locale.US, "%.1f", estVo2)} mL/kg/min",
              style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFamily),
              color = MaterialTheme.colorScheme.primary,
            )
          }
        }
      }

      item {
        Text("Workout Plans (${filteredPlans.size})", style = MaterialTheme.typography.titleMedium)
      }

      items(filteredPlans, key = { it.id }) { plan ->
        Card(
          modifier =
            Modifier.fillMaxWidth()
              .clip(MaterialTheme.shapes.medium)
              .clickable { onOpenWorkoutDetails(plan.id) }
              .testTag("workout_plan_${plan.id}"),
          shape = MaterialTheme.shapes.medium,
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
          Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(plan.title, style = MaterialTheme.typography.titleMedium)
              Text(
                "${plan.category} • ~${plan.estimatedMinutes} min" +
                  if (plan.isCustom) " • Custom" else " • Offline Ready",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                plan.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            FilledTonalButton(onClick = { onOpenWorkoutDetails(plan.id) }) {
              Text("View")
            }
          }
        }
      }

      item {
        Spacer(modifier = Modifier.height(4.dp))
        Text("Offline Exercise Library (${filteredExercises.size})", style = MaterialTheme.typography.titleMedium)
      }

      items(filteredExercises, key = { it.id }) { ex ->
        val isExpanded = expandedExerciseId == ex.id
        Card(
          modifier =
            Modifier.fillMaxWidth()
              .clip(MaterialTheme.shapes.medium)
              .clickable { expandedExerciseId = if (isExpanded) null else ex.id },
          shape = MaterialTheme.shapes.medium,
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(ex.name, style = MaterialTheme.typography.titleSmall)
                Text(
                  "${ex.category} • ${ex.difficulty} • ${ex.defaultSets} sets × ${if (ex.defaultDurationSec > 0) "${ex.defaultDurationSec}s" else "${ex.defaultReps} reps"}",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              Icon(
                if (isExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                contentDescription = if (isExpanded) "Collapse instructions" else "Expand instructions",
              )
            }
            AnimatedVisibility(visible = isExpanded) {
              Column(modifier = Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(ex.description, style = MaterialTheme.typography.bodySmall)
                Surface(
                  color = MaterialTheme.colorScheme.surfaceVariant,
                  shape = MaterialTheme.shapes.small,
                  modifier = Modifier.fillMaxWidth(),
                ) {
                  Text(
                    ex.instructions,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(10.dp),
                  )
                }
              }
            }
          }
        }
      }

      item { Spacer(modifier = Modifier.height(24.dp)) }
    }
  }

  if (showBuilderDialog) {
    CustomWorkoutDialog(
      exercises = exercises,
      onDismiss = { showBuilderDialog = false },
      onCreate = { title, category, desc, ids ->
        onCreateCustomPlan(title, category, desc, ids)
        showBuilderDialog = false
      },
    )
  }
}

@Composable
private fun CustomWorkoutDialog(
  exercises: List<ExerciseEntity>,
  onDismiss: () -> Unit,
  onCreate: (title: String, category: String, description: String, exerciseIds: List<String>) -> Unit,
) {
  var title by remember { mutableStateOf("") }
  var description by remember { mutableStateOf("") }
  var selectedCategory by remember { mutableStateOf("STRENGTH") }
  val selectedIds = remember { mutableStateListOf<String>() }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Create Custom Routine") },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        OutlinedTextField(
          value = title,
          onValueChange = { title = it },
          label = { Text("Routine Name") },
          singleLine = true,
        )
        OutlinedTextField(
          value = description,
          onValueChange = { description = it },
          label = { Text("Goal / Notes") },
          singleLine = true,
        )
        Row(
          modifier = Modifier.horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          listOf("STRENGTH", "BODYWEIGHT", "YOGA", "INDOOR", "STRETCHING").forEach { cat ->
            FilterChip(
              selected = selectedCategory == cat,
              onClick = { selectedCategory = cat },
              label = { Text(cat) },
            )
          }
        }
        Text("Select Exercises (${selectedIds.size})", style = MaterialTheme.typography.labelLarge)
        exercises.forEach { ex ->
          val checked = ex.id in selectedIds
          Row(
            modifier =
              Modifier.fillMaxWidth()
                .clickable {
                  if (checked) selectedIds.remove(ex.id) else selectedIds.add(ex.id)
                },
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Checkbox(
              checked = checked,
              onCheckedChange = { if (it) selectedIds.add(ex.id) else selectedIds.remove(ex.id) },
            )
            Text(ex.name, style = MaterialTheme.typography.bodySmall)
          }
        }
      }
    },
    confirmButton = {
      Button(
        enabled = selectedIds.isNotEmpty(),
        onClick = { onCreate(title, selectedCategory, description, selectedIds.toList()) },
      ) {
        Text(stringResource(R.string.save_label))
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel_label)) }
    },
  )
}

// 17. Workout Details Screen
@Composable
fun WorkoutDetailsScreen(
  plan: WorkoutPlanEntity?,
  allExercises: List<ExerciseEntity>,
  onStartActiveWorkout: (String) -> Unit,
  onBack: () -> Unit,
) {
  BackHandler(onBack = onBack)

  if (plan == null) {
    Scaffold(topBar = { VitalTopBar(title = "Workout Details", onBack = onBack) }) { innerPadding ->
      Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
        Text("Workout plan not found.")
      }
    }
    return
  }

  val planExercises =
    remember(plan, allExercises) {
      val ids = parseExerciseIds(plan.exerciseIdsJson)
      val byId = allExercises.associateBy { it.id }
      ids.mapNotNull { byId[it] }
    }

  Scaffold(topBar = { VitalTopBar(title = plan.title, onBack = onBack) }) { innerPadding ->
    Column(
      modifier =
        Modifier.fillMaxSize()
          .padding(innerPadding)
          .verticalScroll(rememberScrollState())
          .padding(16.dp)
          .widthIn(max = 640.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text(
            "${plan.category} • ~${plan.estimatedMinutes} min • ${planExercises.size} exercises",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
          )
          Text(
            plan.description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
          )
        }
      }

      Button(
        onClick = { onStartActiveWorkout(plan.id) },
        modifier = Modifier.fillMaxWidth().height(52.dp).testTag("start_workout_button"),
      ) {
        Icon(Icons.Outlined.PlayArrow, contentDescription = null)
        Spacer(modifier = Modifier.width(6.dp))
        Text("Start Guided Workout")
      }

      planExercises.forEachIndexed { idx, ex ->
        Card(
          shape = MaterialTheme.shapes.medium,
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
          Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("${idx + 1}. ${ex.name}", style = MaterialTheme.typography.titleMedium)
            Text(
              "${ex.defaultSets} sets • ${if (ex.defaultDurationSec > 0) "${ex.defaultDurationSec}s hold" else "${ex.defaultReps} reps"} • ${ex.restSeconds}s rest",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.primary,
            )
            Text(ex.instructions, style = MaterialTheme.typography.bodySmall)
          }
        }
      }
    }
  }
}

// 18. Active Workout Timer & Rest Screen
@Composable
fun ActiveWorkoutScreen(
  plan: WorkoutPlanEntity?,
  allExercises: List<ExerciseEntity>,
  onFinishSession: (plan: WorkoutPlanEntity, durationSeconds: Int, completedSets: Int, totalReps: Int) -> Unit,
  onBack: () -> Unit,
) {
  BackHandler(onBack = onBack)

  if (plan == null) {
    Scaffold(topBar = { VitalTopBar(title = "Active Workout", onBack = onBack) }) { innerPadding ->
      Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
        Text("Workout plan unavailable.")
      }
    }
    return
  }

  val exercises =
    remember(plan, allExercises) {
      val ids = parseExerciseIds(plan.exerciseIdsJson)
      val byId = allExercises.associateBy { it.id }
      ids.mapNotNull { byId[it] }
    }

  val context = LocalContext.current
  var elapsedSec by remember { mutableIntStateOf(0) }
  var isPaused by remember { mutableStateOf(false) }
  var currentIdx by remember { mutableIntStateOf(0) }
  var currentSet by remember { mutableIntStateOf(1) }
  var restSecondsLeft by remember { mutableIntStateOf(0) }
  var completedSets by remember { mutableIntStateOf(0) }
  var totalReps by remember { mutableIntStateOf(0) }
  var autoRepSensorEnabled by remember { mutableStateOf(false) }
  var cadenceBpmEnabled by remember { mutableStateOf(false) }
  var cadenceBpm by remember { mutableFloatStateOf(168f) }

  // #35 Hardware Accelerometer Squat/Rep Peak Detector
  DisposableEffect(autoRepSensorEnabled) {
    if (!autoRepSensorEnabled) return@DisposableEffect onDispose {}
    val sm = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    val acc = sm?.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION) ?: sm?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    if (sm == null || acc == null) return@DisposableEffect onDispose {}
    var lastRepMs = 0L
    val listener = object : SensorEventListener {
      override fun onSensorChanged(event: SensorEvent) {
        val mag = abs(event.values.getOrNull(1) ?: 0f)
        val threshold = if (acc.type == Sensor.TYPE_LINEAR_ACCELERATION) 3.8f else 13.5f
        val now = System.currentTimeMillis()
        if (mag > threshold && now - lastRepMs > 750L) {
          lastRepMs = now
          totalReps++
          BiofeedbackAudioEngine.triggerHapticPulse(context, 40L, 100)
        }
      }
      override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }
    runCatching { sm.registerListener(listener, acc, SensorManager.SENSOR_DELAY_GAME) }
    onDispose { runCatching { sm.unregisterListener(listener) } }
  }

  // #37 Running Cadence Metronome BPM audio/haptic beat
  LaunchedEffect(cadenceBpmEnabled, cadenceBpm) {
    while (cadenceBpmEnabled) {
      BiofeedbackAudioEngine.playShortTone(durationMs = 35)
      delay((60_000f / cadenceBpm.coerceIn(120f, 200f)).toLong())
    }
  }

  LaunchedEffect(isPaused, restSecondsLeft) {
    while (true) {
      delay(1000L)
      if (!isPaused) {
        elapsedSec++
        if (restSecondsLeft > 0) {
          restSecondsLeft--
          if (restSecondsLeft == 0) {
            BiofeedbackAudioEngine.playShortTone()
            BiofeedbackAudioEngine.triggerHapticPulse(context, 120L, 150)
          }
        }
      }
    }
  }

  val currentExercise = exercises.getOrNull(currentIdx)

  Scaffold(topBar = { VitalTopBar(title = plan.title, onBack = onBack) }) { innerPadding ->
    Column(
      modifier =
        Modifier.fillMaxSize()
          .padding(innerPadding)
          .padding(16.dp)
          .widthIn(max = 600.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(16.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Elapsed Time", style = MaterialTheme.typography.labelLarge)
          }
          Text(
            text = WellnessCalculators.formatDurationMillis(elapsedSec * 1000L),
            style = MaterialTheme.typography.displayMedium.copy(fontFamily = JetBrainsMonoFamily),
            color = MaterialTheme.colorScheme.primary,
          )
          Text(
            text = "Completed Sets: $completedSets • Total Reps: $totalReps",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      // Sensor Auto-Rep & Cadence Metronome Controls (#35, #37)
      Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Accelerometer Auto-Rep Counter", style = MaterialTheme.typography.labelLarge)
            Switch(checked = autoRepSensorEnabled, onCheckedChange = { autoRepSensorEnabled = it })
          }
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Cadence Metronome (${cadenceBpm.toInt()} BPM)", style = MaterialTheme.typography.labelLarge)
            Switch(checked = cadenceBpmEnabled, onCheckedChange = { cadenceBpmEnabled = it })
          }
          if (cadenceBpmEnabled) {
            Slider(value = cadenceBpm, onValueChange = { cadenceBpm = it }, valueRange = 140f..190f)
          }
        }
      }

      if (restSecondsLeft > 0) {
        Card(
          shape = MaterialTheme.shapes.large,
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
        ) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Text("Rest Interval", style = MaterialTheme.typography.titleMedium)
            Text(
              "${restSecondsLeft}s",
              style = MaterialTheme.typography.displayMedium.copy(fontFamily = JetBrainsMonoFamily),
              color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
            OutlinedButton(onClick = { restSecondsLeft = 0 }) { Text("Skip Rest") }
          }
        }
      }

      if (currentExercise != null) {
        Card(
          shape = MaterialTheme.shapes.large,
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          modifier = Modifier.weight(1f),
        ) {
          Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Text(
              "Exercise ${currentIdx + 1} of ${exercises.size}",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.primary,
            )
            Text(currentExercise.name, style = MaterialTheme.typography.headlineSmall)
            Text(
              "Set $currentSet of ${currentExercise.defaultSets} • Target: " +
                if (currentExercise.defaultDurationSec > 0) "${currentExercise.defaultDurationSec} sec"
                else "${currentExercise.defaultReps} reps",
              style = MaterialTheme.typography.titleMedium,
            )
            Text(currentExercise.instructions, style = MaterialTheme.typography.bodyMedium)
          }
        }

        Button(
          onClick = {
            BiofeedbackAudioEngine.playShortTone()
            completedSets++
            totalReps += currentExercise.defaultReps
            if (currentSet < currentExercise.defaultSets) {
              currentSet++
              restSecondsLeft = currentExercise.restSeconds
            } else if (currentIdx < exercises.lastIndex) {
              currentIdx++
              currentSet = 1
              restSecondsLeft = currentExercise.restSeconds
            } else {
              onFinishSession(plan, elapsedSec.coerceAtLeast(1), completedSets, totalReps)
            }
          },
          modifier = Modifier.fillMaxWidth().height(52.dp).testTag("complete_set_button"),
        ) {
          Text(
            if (currentIdx == exercises.lastIndex && currentSet >= currentExercise.defaultSets)
              "Complete Final Set & Finish"
            else "Complete Set & Rest (${currentExercise.restSeconds}s)"
          )
        }
      }

      Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedButton(
          onClick = { isPaused = !isPaused },
          modifier = Modifier.weight(1f).height(48.dp),
        ) {
          Text(if (isPaused) "Resume Timer" else "Pause Timer")
        }
        FilledTonalButton(
          onClick = {
            onFinishSession(
              plan,
              elapsedSec.coerceAtLeast(1),
              completedSets.coerceAtLeast(1),
              totalReps,
            )
          },
          modifier = Modifier.weight(1f).height(48.dp).testTag("finish_workout_button"),
        ) {
          Text("Finish & Save")
        }
      }
    }
  }
}

// 19. Habit Tracker Screen
@Composable
fun HabitTrackerScreen(
  habits: List<HabitEntity>,
  completions: List<HabitCompletionEntity>,
  onToggleHabitToday: (String) -> Unit,
  onAddHabit: (title: String, category: String, target: String, reminderTime: String) -> Unit,
  onDeleteHabit: (String) -> Unit,
  preferences: UserProfilePreferences = UserProfilePreferences(),
  habitDigestState: HabitDigestUiState = HabitDigestUiState.Idle,
  isOnline: Boolean = true,
  onUseStreakFreeze: () -> Unit = {},
  onSelectChallenge: (String) -> Unit = {},
  onGenerateWeeklyDigest: () -> Unit = {},
  onAddHabitExtended: ((title: String, category: String, target: String, reminderTime: String, freq: String, stack: String, polarity: String) -> Unit)? = null,
  onBack: () -> Unit,
) {
  BackHandler(onBack = onBack)

  val context = LocalContext.current
  val todayIso = remember { WellnessCalculators.todayIso() }
  val last14Days = remember { WellnessCalculators.lastNDaysIso(14) }
  var showAddDialog by remember { mutableStateOf(false) }
  var titleDraft by remember { mutableStateOf("") }
  var targetDraft by remember { mutableStateOf("") }
  var categoryDraft by remember { mutableStateOf("CUSTOM") }
  var frequencyDraft by remember { mutableStateOf("DAILY") }
  var polarityDraft by remember { mutableStateOf("BUILD") }
  var stackDraft by remember { mutableStateOf("") }

  val completedTodayCount =
    remember(habits, completions) {
      habits.count { habit -> completions.any { it.habitId == habit.id && it.dateIso == todayIso } }
    }
  val (xpLevel, xpInLevel, xpGoal) = WellnessAdvancedCalculators.xpToLevelProgress(preferences.wellnessXp)
  val weekdayStats = remember(completions) { WellnessAdvancedCalculators.habitCompletionsByWeekday(completions) }

  Scaffold(
    topBar = {
      VitalTopBar(
        title = stringResource(R.string.habits_label),
        onBack = onBack,
        actions = {
          IconButton(
            onClick = {
              val shareText = "VITALTRACK Milestone: Level $xpLevel (${preferences.wellnessXp} XP) • $completedTodayCount/${habits.size} habits done today!"
              val send = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, shareText)
              }
              runCatching { context.startActivity(Intent.createChooser(send, "Share Habit Milestone")) }
            },
            modifier = Modifier.testTag("habit_share_milestone_btn"),
          ) {
            Icon(Icons.Outlined.Share, contentDescription = "Share milestone")
          }
          IconButton(
            onClick = { showAddDialog = true },
            modifier = Modifier.testTag("add_habit_button"),
          ) {
            Icon(Icons.Outlined.Add, contentDescription = "Add habit")
          }
        },
      )
    }
  ) { innerPadding ->
    LazyColumn(
      modifier =
        Modifier.fillMaxSize()
          .padding(innerPadding)
          .padding(horizontal = 16.dp)
          .widthIn(max = 640.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      item {
        Card(
          shape = MaterialTheme.shapes.large,
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        ) {
          Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
              Text(
                "Today's Progress: $completedTodayCount / ${habits.size} completed",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
              )
              Surface(color = MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(12.dp)) {
                Text(
                  "Lv.$xpLevel • ${preferences.wellnessXp} XP",
                  style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFamily),
                  color = MaterialTheme.colorScheme.onPrimary,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                )
              }
            }
            LinearProgressIndicator(
              progress = {
                if (habits.isEmpty()) 0f else completedTodayCount.toFloat() / habits.size
              },
              modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
            )
            LinearProgressIndicator(
              progress = { (xpInLevel.toFloat() / xpGoal).coerceIn(0f, 1f) },
              modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
              color = GoldAccent,
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
              OutlinedButton(
                onClick = onUseStreakFreeze,
                enabled = preferences.streakFreezeTokens > 0,
                modifier = Modifier.testTag("habit_freeze_btn"),
              ) {
                Icon(Icons.Outlined.AcUnit, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Streak Freeze (${preferences.streakFreezeTokens})")
              }
              FilledTonalButton(
                onClick = onGenerateWeeklyDigest,
                enabled = isOnline && habitDigestState !is HabitDigestUiState.Loading,
                modifier = Modifier.weight(1f).testTag("habit_ai_digest_btn"),
              ) {
                Icon(Icons.Outlined.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("AI Weekly Digest")
              }
            }
          }
        }
      }

      if (habitDigestState !is HabitDigestUiState.Idle) {
        item {
          Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(12.dp)) {
            Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
              Text("AI Habit Reflection Digest", style = MaterialTheme.typography.titleSmall)
              when (habitDigestState) {
                HabitDigestUiState.Loading -> CircularProgressIndicator(modifier = Modifier.size(20.dp))
                is HabitDigestUiState.Success -> Text(habitDigestState.digest, style = MaterialTheme.typography.bodySmall)
                is HabitDigestUiState.Error -> Text(habitDigestState.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                HabitDigestUiState.Idle -> Unit
              }
            }
          }
        }
      }

      // #76 Monthly Challenges & #77 Weekday Consistency strip
      item {
        Card(shape = MaterialTheme.shapes.medium, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
          Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Outlined.EmojiEvents, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Active Challenge & Weekday Analytics", style = MaterialTheme.typography.titleSmall)
            }
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              listOf("challenge_steps_75k" to "75K Steps Sprint", "challenge_water_21" to "21-Day Hydration").forEach { (id, title) ->
                FilterChip(selected = preferences.activeChallengeId == id, onClick = { onSelectChallenge(id) }, label = { Text(title) })
              }
            }
            Text(
              "By Weekday: " + weekdayStats.entries.joinToString(" • ") { "${it.key}:${it.value}" },
              style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }

      items(habits, key = { it.id }) { habit ->
        val doneToday = completions.any { it.habitId == habit.id && it.dateIso == todayIso }
        val streak = VitalTrackRepository.calculateHabitStreak(habit.id, completions)

        Card(
          shape = MaterialTheme.shapes.medium,
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
          Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Checkbox(
                checked = doneToday,
                onCheckedChange = { onToggleHabitToday(habit.id) },
                modifier = Modifier.testTag("habit_check_${habit.id}"),
              )
              Column(modifier = Modifier.weight(1f)) {
                Text(habit.title, style = MaterialTheme.typography.titleMedium)
                Text(
                  "${habit.category} • ${habit.frequencyType} • ${habit.habitPolarity}" +
                    if (habit.stackChainGroup.isNotBlank()) " • Stack: ${habit.stackChainGroup}" else "",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              Surface(
                color = MaterialTheme.colorScheme.tertiaryContainer,
                shape = RoundedCornerShape(20.dp),
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Icon(
                    Icons.Outlined.LocalFireDepartment,
                    contentDescription = "Streak",
                    tint = GoldAccent,
                    modifier = Modifier.size(16.dp),
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(
                    "${streak}d",
                    style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFamily),
                  )
                }
              }
              if (habit.id.startsWith("habit_") && habit.category == "CUSTOM") {
                IconButton(onClick = { onDeleteHabit(habit.id) }) {
                  Icon(Icons.Outlined.DeleteOutline, contentDescription = "Delete habit")
                }
              }
            }

            // 14-day completion strip
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              last14Days.forEach { dayIso ->
                val completed = completions.any { it.habitId == habit.id && it.dateIso == dayIso }
                Box(
                  modifier =
                    Modifier.size(14.dp)
                      .clip(CircleShape)
                      .background(
                        if (completed) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant
                      )
                )
              }
            }
          }
        }
      }

      item { Spacer(modifier = Modifier.height(24.dp)) }
    }
  }

  if (showAddDialog) {
    AlertDialog(
      onDismissRequest = { showAddDialog = false },
      title = { Text("Add Daily Habit") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = titleDraft,
            onValueChange = { titleDraft = it },
            label = { Text("Habit Title") },
            singleLine = true,
            modifier = Modifier.testTag("habit_title_input"),
          )
          OutlinedTextField(
            value = targetDraft,
            onValueChange = { targetDraft = it },
            label = { Text("Daily Target") },
            singleLine = true,
          )
          Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            listOf("CUSTOM", "WATER", "WALKING", "SLEEP", "EXERCISE").forEach { cat ->
              FilterChip(
                selected = categoryDraft == cat,
                onClick = { categoryDraft = cat },
                label = { Text(cat) },
              )
            }
          }
          Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            listOf("DAILY", "WEEKDAYS", "THREE_PER_WEEK").forEach { freq ->
              FilterChip(selected = frequencyDraft == freq, onClick = { frequencyDraft = freq }, label = { Text(freq) })
            }
            listOf("BUILD", "QUIT").forEach { pol ->
              FilterChip(selected = polarityDraft == pol, onClick = { polarityDraft = pol }, label = { Text(pol) })
            }
          }
          OutlinedTextField(
            value = stackDraft,
            onValueChange = { stackDraft = it },
            label = { Text("Stack Chain (e.g., Morning Routine)") },
            singleLine = true,
          )
        }
      },
      confirmButton = {
        Button(
          enabled = titleDraft.isNotBlank(),
          onClick = {
            if (onAddHabitExtended != null) {
              onAddHabitExtended(titleDraft, categoryDraft, targetDraft.ifBlank { "Daily check-in" }, "09:00", frequencyDraft, stackDraft.trim(), polarityDraft)
            } else {
              onAddHabit(titleDraft, categoryDraft, targetDraft.ifBlank { "Daily check-in" }, "09:00")
            }
            titleDraft = ""
            targetDraft = ""
            stackDraft = ""
            showAddDialog = false
          },
          modifier = Modifier.testTag("habit_save_button"),
        ) {
          Text(stringResource(R.string.save_label))
        }
      },
      dismissButton = {
        TextButton(onClick = { showAddDialog = false }) { Text(stringResource(R.string.cancel_label)) }
      },
    )
  }
}

// 20. Personal Activity Calendar Screen
@Composable
fun CalendarScreen(
  dailySteps: List<DailyStepEntity>,
  gpsSessions: List<GpsActivitySessionEntity>,
  workoutSessions: List<WorkoutSessionEntity>,
  habitCompletions: List<HabitCompletionEntity>,
  onBack: () -> Unit,
) {
  BackHandler(onBack = onBack)

  var currentMonth by remember { mutableStateOf(YearMonth.now()) }
  var selectedDateIso by remember { mutableStateOf(WellnessCalculators.todayIso()) }

  val stepsMap = remember(dailySteps) { dailySteps.associateBy { it.dateIso } }
  val gpsByDate = remember(gpsSessions) { gpsSessions.groupBy { it.dateIso } }
  val workoutsByDate = remember(workoutSessions) { workoutSessions.groupBy { it.dateIso } }
  val habitsByDate = remember(habitCompletions) { habitCompletions.groupBy { it.dateIso } }

  val firstOfMonth = currentMonth.atDay(1)
  val startOffset = (firstOfMonth.dayOfWeek.value - 1) // Mon=0..Sun=6
  val daysInMonth = currentMonth.lengthOfMonth()

  Scaffold(topBar = { VitalTopBar(title = "Activity Calendar", onBack = onBack) }) { innerPadding ->
    Column(
      modifier =
        Modifier.fillMaxSize()
          .padding(innerPadding)
          .verticalScroll(rememberScrollState())
          .padding(16.dp)
          .widthIn(max = 640.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        IconButton(onClick = { currentMonth = currentMonth.minusMonths(1) }) {
          Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous month")
        }
        Text(
          "${currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${currentMonth.year}",
          style = MaterialTheme.typography.titleLarge,
        )
        IconButton(onClick = { currentMonth = currentMonth.plusMonths(1) }) {
          Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next month")
        }
      }

      Row(modifier = Modifier.fillMaxWidth()) {
        listOf("M", "T", "W", "T", "F", "S", "S").forEach { d ->
          Text(
            text = d,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      val totalCells = startOffset + daysInMonth
      val rows = (totalCells + 6) / 7
      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        for (r in 0 until rows) {
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            for (c in 0 until 7) {
              val cellIndex = r * 7 + c
              val dayNum = cellIndex - startOffset + 1
              if (dayNum < 1 || dayNum > daysInMonth) {
                Spacer(modifier = Modifier.weight(1f).height(46.dp))
              } else {
                val iso = currentMonth.atDay(dayNum).toString()
                val hasActivity =
                  (stepsMap[iso]?.steps ?: 0) > 0 ||
                    gpsByDate.containsKey(iso) ||
                    workoutsByDate.containsKey(iso) ||
                    habitsByDate.containsKey(iso)
                val isSelected = selectedDateIso == iso
                Surface(
                  modifier =
                    Modifier.weight(1f)
                      .height(48.dp)
                      .clip(RoundedCornerShape(10.dp))
                      .clickable { selectedDateIso = iso },
                  color =
                    when {
                      isSelected -> MaterialTheme.colorScheme.primary
                      hasActivity -> MaterialTheme.colorScheme.primaryContainer
                      else -> MaterialTheme.colorScheme.surface
                    },
                ) {
                  Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                  ) {
                    Text(
                      text = dayNum.toString(),
                      style = MaterialTheme.typography.labelLarge,
                      fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                      color =
                        if (isSelected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurface,
                    )
                    if (hasActivity) {
                      Box(
                        modifier =
                          Modifier.size(5.dp)
                            .clip(CircleShape)
                            .background(
                              if (isSelected) MaterialTheme.colorScheme.onPrimary
                              else MaterialTheme.colorScheme.tertiary
                            )
                      )
                    }
                  }
                }
              }
            }
          }
        }
      }

      // Selected Date Details
      val dayStep = stepsMap[selectedDateIso]
      val dayGps = gpsByDate[selectedDateIso].orEmpty()
      val dayWorkouts = workoutsByDate[selectedDateIso].orEmpty()
      val dayHabits = habitsByDate[selectedDateIso].orEmpty()

      Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("Summary for $selectedDateIso", style = MaterialTheme.typography.titleMedium)
          Text(
            "Steps: ${dayStep?.steps ?: 0} • GPS Sessions: ${dayGps.size} • Workouts: ${dayWorkouts.size} • Habits: ${dayHabits.size}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          dayGps.forEach { g ->
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                "${g.title} (${WellnessCalculators.formatDistance(g.distanceMeters, false)})",
                style = MaterialTheme.typography.bodySmall,
              )
            }
          }
          dayWorkouts.forEach { w ->
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Outlined.FitnessCenter, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("${w.title} (${w.durationSeconds / 60} min)", style = MaterialTheme.typography.bodySmall)
            }
          }
        }
      }
    }
  }
}
