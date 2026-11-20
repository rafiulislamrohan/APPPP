package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessibilityNew
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.LocationCity
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.health.WellnessCalculators
import com.example.data.local.DailyStepEntity
import com.example.data.local.HabitCompletionEntity
import com.example.data.local.HeartRateRecordEntity
import com.example.data.local.SleepRecordEntity
import com.example.data.local.UserProfilePreferences
import com.example.data.local.WaterIntakeEntity
import com.example.data.local.WorkoutSessionEntity
import com.example.ui.components.ExerciseForm3DCanvas
import com.example.ui.components.HabitCity3DCanvas
import com.example.ui.components.HeartPulse3DSphereCanvas
import com.example.ui.components.HydrationFlask3DCanvas
import com.example.ui.components.Morphable3DBodyCanvas
import com.example.ui.components.SleepMountain3DCanvas
import com.example.ui.components.SpinePosture3DCanvas
import com.example.ui.components.Tactile3DCard
import com.example.ui.components.TrophyShelf3DCanvas
import com.example.ui.components.VitalTopBar
import com.example.ui.components.rememberSensorTilt3D
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.JetBrainsMonoFamily
import kotlin.math.roundToInt

/**
 * Interactive 3D Wellness Gallery (#1-#10).
 */
@Composable
fun SpatialGalleryScreen(
  preferences: UserProfilePreferences,
  dailySteps: List<DailyStepEntity>,
  waterIntakes: List<WaterIntakeEntity>,
  sleepRecords: List<SleepRecordEntity>,
  heartRates: List<HeartRateRecordEntity>,
  workoutSessions: List<WorkoutSessionEntity>,
  habitCompletions: List<HabitCompletionEntity>,
  onSaveBodyMorph: (chestCm: Double, waistCm: Double, hipCm: Double) -> Unit,
  onBack: () -> Unit,
) {
  BackHandler(onBack = onBack)

  var selectedTab by remember { mutableStateOf("BODY") } // BODY, POSTURE, FORM, CITY, SLEEP_HYDRO, PULSE_TROPHY
  var yawDeg by remember { mutableFloatStateOf(-18f) }

  var chestCm by remember(preferences.bodyChestCm) { mutableDoubleStateOf(preferences.bodyChestCm) }
  var waistCm by remember(preferences.bodyWaistCm) { mutableDoubleStateOf(preferences.bodyWaistCm) }
  var hipCm by remember(preferences.bodyHipCm) { mutableDoubleStateOf(preferences.bodyHipCm) }
  var showHeatmap by remember { mutableStateOf(false) }
  var showBeforeAfter by remember { mutableStateOf(false) }
  var showJacket by remember { mutableStateOf(false) }
  var showScarf by remember { mutableStateOf(false) }
  var showHat by remember { mutableStateOf(false) }
  var showBelt by remember { mutableStateOf(true) }

  var exerciseMode by remember { mutableStateOf("SQUAT") }

  val tilt by rememberSensorTilt3D(enabled = true)
  val todayIso = remember { WellnessCalculators.todayIso() }
  val todayWater = remember(waterIntakes) { waterIntakes.filter { it.dateIso == todayIso }.sumOf { it.amountMl } }
  val lifetimeSteps = remember(dailySteps) { dailySteps.sumOf { it.steps } }
  val latestHr = heartRates.firstOrNull()?.bpm ?: 68
  val recent14Habits = remember(habitCompletions) {
    WellnessCalculators.lastNDaysIso(14).map { iso -> habitCompletions.count { it.dateIso == iso } }
  }
  val recentWorkoutRatio = (workoutSessions.take(4).size / 4f).coerceIn(0.15f, 1f)

  val infinite = rememberInfiniteTransition(label = "spatial_anim")
  val formPhase by infinite.animateFloat(
    initialValue = 0f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Restart),
    label = "form_phase",
  )
  val pulsePeriodMs = (60_000 / latestHr.coerceIn(45, 160)).coerceIn(380, 1300)
  val pulsePhase by infinite.animateFloat(
    initialValue = 0f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(tween(pulsePeriodMs, easing = LinearEasing), RepeatMode.Restart),
    label = "hr_pulse",
  )

  Scaffold(topBar = { VitalTopBar(title = "3D Wellness Gallery (10 Engines)", onBack = onBack) }) { innerPadding ->
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
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        listOf(
          "BODY" to "Body & Heatmap",
          "POSTURE" to "3D Spine",
          "FORM" to "Exercise Form",
          "CITY" to "Habit City",
          "SLEEP_HYDRO" to "Sleep & Flask",
          "PULSE_TROPHY" to "Heart & Trophy",
        ).forEach { (key, label) ->
          FilterChip(
            selected = selectedTab == key,
            onClick = { selectedTab = key },
            label = { Text(label) },
            modifier = Modifier.testTag("gallery_tab_${key.lowercase()}"),
          )
        }
      }

      when (selectedTab) {
        "BODY" -> {
          Tactile3DCard(enabled3D = preferences.threeDVisualsEnabled) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.AccessibilityNew, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("Morphable 3D Body, Heatmap & Layers", style = MaterialTheme.typography.titleMedium)
              }
              if (showBeforeAfter) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                  Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Baseline (+8cm waist)", style = MaterialTheme.typography.labelSmall)
                    Morphable3DBodyCanvas(
                      chestCm = chestCm + 4.0,
                      waistCm = waistCm + 8.0,
                      hipCm = hipCm + 4.0,
                      upperFatigueRatio = 0.2f,
                      coreFatigueRatio = 0.2f,
                      legFatigueRatio = 0.2f,
                      showMuscleHeatmap = false,
                      showJacket = showJacket,
                      showScarf = showScarf,
                      showHat = showHat,
                      showBelt = showBelt,
                      topColor = MaterialTheme.colorScheme.secondary,
                      bottomColor = MaterialTheme.colorScheme.outline,
                      accentColor = GoldAccent,
                      yawDeg = yawDeg,
                      onYawDrag = { yawDeg += it },
                    )
                  }
                  Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Current Goal Form", style = MaterialTheme.typography.labelSmall)
                    Morphable3DBodyCanvas(
                      chestCm = chestCm,
                      waistCm = waistCm,
                      hipCm = hipCm,
                      upperFatigueRatio = recentWorkoutRatio,
                      coreFatigueRatio = recentWorkoutRatio * 0.8f,
                      legFatigueRatio = (dailySteps.firstOrNull()?.steps ?: 0) / 10000f,
                      showMuscleHeatmap = showHeatmap,
                      showJacket = showJacket,
                      showScarf = showScarf,
                      showHat = showHat,
                      showBelt = showBelt,
                      topColor = MaterialTheme.colorScheme.primary,
                      bottomColor = MaterialTheme.colorScheme.secondary,
                      accentColor = GoldAccent,
                      yawDeg = yawDeg,
                      onYawDrag = { yawDeg += it },
                    )
                  }
                }
              } else {
                Morphable3DBodyCanvas(
                  chestCm = chestCm,
                  waistCm = waistCm,
                  hipCm = hipCm,
                  upperFatigueRatio = recentWorkoutRatio,
                  coreFatigueRatio = recentWorkoutRatio * 0.75f,
                  legFatigueRatio = ((dailySteps.firstOrNull()?.steps ?: 2000) / 10000f),
                  showMuscleHeatmap = showHeatmap,
                  showJacket = showJacket,
                  showScarf = showScarf,
                  showHat = showHat,
                  showBelt = showBelt,
                  topColor = MaterialTheme.colorScheme.primary,
                  bottomColor = MaterialTheme.colorScheme.secondary,
                  accentColor = GoldAccent,
                  yawDeg = yawDeg,
                  onYawDrag = { yawDeg += it },
                  modifier = Modifier.testTag("gallery_body_canvas"),
                )
              }

              Text("Chest: ${chestCm.roundToInt()} cm", style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFamily))
              Slider(value = chestCm.toFloat(), onValueChange = { chestCm = it.toDouble() }, valueRange = 75f..135f)
              Text("Waist: ${waistCm.roundToInt()} cm", style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFamily))
              Slider(value = waistCm.toFloat(), onValueChange = { waistCm = it.toDouble() }, valueRange = 60f..125f)
              Text("Hip: ${hipCm.roundToInt()} cm", style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFamily))
              Slider(value = hipCm.toFloat(), onValueChange = { hipCm = it.toDouble() }, valueRange = 75f..135f)

              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Muscle Recovery Heatmap", style = MaterialTheme.typography.bodyMedium)
                Switch(checked = showHeatmap, onCheckedChange = { showHeatmap = it }, modifier = Modifier.testTag("gallery_heatmap_switch"))
              }
              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("3D Before / After Split", style = MaterialTheme.typography.bodyMedium)
                Switch(checked = showBeforeAfter, onCheckedChange = { showBeforeAfter = it }, modifier = Modifier.testTag("gallery_before_after_switch"))
              }
              Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(selected = showJacket, onClick = { showJacket = !showJacket }, label = { Text("Jacket") })
                FilterChip(selected = showScarf, onClick = { showScarf = !showScarf }, label = { Text("Scarf") })
                FilterChip(selected = showHat, onClick = { showHat = !showHat }, label = { Text("Hat") })
                FilterChip(selected = showBelt, onClick = { showBelt = !showBelt }, label = { Text("Belt") })
              }
              Button(
                onClick = { onSaveBodyMorph(chestCm, waistCm, hipCm) },
                modifier = Modifier.fillMaxWidth().testTag("gallery_save_morph_btn"),
              ) {
                Text("Save 3D Body Proportions")
              }
            }
          }
        }

        "POSTURE" -> {
          Tactile3DCard(enabled3D = preferences.threeDVisualsEnabled) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Text("Sensor-Reactive 3D Spine & Posture Visualizer", style = MaterialTheme.typography.titleMedium)
              SpinePosture3DCanvas(
                slouchDeg = tilt.second * 1.4f,
                goodColor = MaterialTheme.colorScheme.primary,
                warnColor = MaterialTheme.colorScheme.error,
                modifier = Modifier.testTag("gallery_spine_canvas"),
              )
              Text(
                "Live tilt offset: ${String.format(java.util.Locale.US, "%.1f", tilt.second)}° • Hold your phone eye-level to reduce cervical strain.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }

        "FORM" -> {
          Tactile3DCard(enabled3D = preferences.threeDVisualsEnabled) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.FitnessCenter, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("3D Exercise Form Animator", style = MaterialTheme.typography.titleMedium)
              }
              Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("SQUAT", "PUSHUP", "PLANK", "STRETCH").forEach { mode ->
                  FilterChip(selected = exerciseMode == mode, onClick = { exerciseMode = mode }, label = { Text(mode) })
                }
              }
              ExerciseForm3DCanvas(
                exerciseMode = exerciseMode,
                phase01 = formPhase,
                primaryColor = MaterialTheme.colorScheme.primary,
                accentColor = GoldAccent,
                modifier = Modifier.testTag("gallery_form_canvas"),
              )
              Text("Maintain neutral spine and controlled tempo across each phase.", style = MaterialTheme.typography.bodySmall)
            }
          }
        }

        "CITY" -> {
          Tactile3DCard(enabled3D = preferences.threeDVisualsEnabled) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.LocationCity, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("3D Monthly Habit City Skyline", style = MaterialTheme.typography.titleMedium)
              }
              HabitCity3DCanvas(
                dayCountsLast14 = recent14Habits,
                towerColor = MaterialTheme.colorScheme.primary,
                highlightColor = GoldAccent,
                modifier = Modifier.testTag("gallery_city_canvas"),
              )
              Text(
                "Each isometric tower rises with daily habit check-ins over the last 14 days (${habitCompletions.size} total check-ins).",
                style = MaterialTheme.typography.bodySmall,
              )
            }
          }
        }

        "SLEEP_HYDRO" -> {
          Tactile3DCard(enabled3D = preferences.threeDVisualsEnabled) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Text("3D Sleep Architecture Terrain (Last 7 Nights)", style = MaterialTheme.typography.titleMedium)
              SleepMountain3DCanvas(records = sleepRecords, modifier = Modifier.testTag("gallery_sleep_canvas"))
            }
          }
          Tactile3DCard(enabled3D = preferences.threeDVisualsEnabled) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.WaterDrop, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("3D Sloshing Hydration Flask ($todayWater / ${preferences.dailyWaterGoalMl} ml)", style = MaterialTheme.typography.titleMedium)
              }
              HydrationFlask3DCanvas(
                waterMl = todayWater,
                goalMl = preferences.dailyWaterGoalMl,
                yawDeg = tilt.second * 6f,
                modifier = Modifier.testTag("gallery_flask_canvas"),
              )
            }
          }
        }

        "PULSE_TROPHY" -> {
          Tactile3DCard(enabled3D = preferences.threeDVisualsEnabled) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.FavoriteBorder, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Text("Pulsing 3D Heart-Rate Sphere ($latestHr BPM)", style = MaterialTheme.typography.titleMedium)
              }
              HeartPulse3DSphereCanvas(pulsePhase01 = pulsePhase, modifier = Modifier.testTag("gallery_pulse_canvas"))
            }
          }
          Tactile3DCard(enabled3D = preferences.threeDVisualsEnabled) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.EmojiEvents, contentDescription = null, tint = GoldAccent)
                Text("3D Milestone Trophy Shelf ($lifetimeSteps lifetime steps)", style = MaterialTheme.typography.titleMedium)
              }
              TrophyShelf3DCanvas(
                lifetimeSteps = lifetimeSteps,
                yawDeg = yawDeg,
                modifier = Modifier.testTag("gallery_trophy_canvas"),
              )
              OutlinedButton(onClick = { yawDeg = -18f }, modifier = Modifier.fillMaxWidth()) {
                Text("Recenter 3D Angle")
              }
            }
          }
        }
      }
    }
  }
}
