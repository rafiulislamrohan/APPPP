package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessibilityNew
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Terrain
import androidx.compose.material.icons.outlined.ViewInAr
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.health.WellnessCalculators
import com.example.data.local.DailyStepEntity
import com.example.data.local.GpsActivitySessionEntity
import com.example.data.local.GpsPoint
import com.example.data.local.SleepRecordEntity
import com.example.data.local.UserProfilePreferences
import com.example.data.local.WaterIntakeEntity
import com.example.data.local.WellnessTaskEntity
import com.example.ui.components.Breathing3DSphereCanvas
import com.example.ui.components.MedicalDisclaimerBanner
import com.example.ui.components.Spatial3DGpsTerrainCanvas
import com.example.ui.components.Spatial3DMannequinCanvas
import com.example.ui.components.Spatial3DVitalityOrbCanvas
import com.example.ui.components.Tactile3DCard
import com.example.ui.components.VitalTopBar
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.HeartRateCoral
import com.example.ui.theme.HydrationBlue
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.SleepIndigo
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json

private val terrainJson = Json { ignoreUnknownKeys = true }

private enum class SpatialMode(val key: String, val label: String) {
  ORB("orb", "3D Vitality Orb"),
  BODY("body", "3D Body & Fit"),
  TERRAIN("terrain", "3D GPS Terrain"),
}

// 28. Interactive 3D Wellness, Body & GPS Terrain Studio
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Spatial3DStudioScreen(
  preferences: UserProfilePreferences,
  dailySteps: List<DailyStepEntity>,
  waterIntakes: List<WaterIntakeEntity>,
  sleepRecords: List<SleepRecordEntity>,
  wellnessTasks: List<WellnessTaskEntity>,
  gpsSessions: List<GpsActivitySessionEntity>,
  onBack: () -> Unit,
) {
  BackHandler(onBack = onBack)

  var mode by remember { mutableStateOf(SpatialMode.ORB) }
  var yawDeg by remember { mutableFloatStateOf(-24f) }
  var pitchDeg by remember { mutableFloatStateOf(16f) }
  var selectedOutfitLook by remember { mutableIntStateOf(0) }
  var selectedGpsIdx by remember { mutableIntStateOf(0) }

  val todayIso = remember { WellnessCalculators.todayIso() }
  val todaySteps = dailySteps.firstOrNull { it.dateIso == todayIso }?.steps ?: 0
  val todayWater = waterIntakes.filter { it.dateIso == todayIso }.sumOf { it.amountMl }
  val latestSleepMin = sleepRecords.firstOrNull()?.durationMinutes ?: 0
  val completedTasks = wellnessTasks.count { it.isCompleted }
  val totalTasks = wellnessTasks.size.coerceAtLeast(1)

  val stepProgress = (todaySteps.toFloat() / preferences.dailyStepGoal.coerceAtLeast(1)).coerceIn(0f, 1f)
  val waterProgress = (todayWater.toFloat() / preferences.dailyWaterGoalMl.coerceAtLeast(1)).coerceIn(0f, 1f)
  val sleepProgress = (latestSleepMin.toFloat() / preferences.dailySleepGoalMinutes.coerceAtLeast(1)).coerceIn(0f, 1f)
  val tasksProgress = (completedTasks.toFloat() / totalTasks).coerceIn(0f, 1f)

  val outfitSwatches =
    remember {
      listOf(
        Triple("Forest Casual", Color(0xFF2D6A4F), Color(0xFFD8C6AE)),
        Triple("Ocean Active", Color(0xFF1D4E89), Color(0xFF2B2D42)),
        Triple("Earthy Sunset", Color(0xFFB55D3D), Color(0xFF3E362E)),
      )
    }

  val activeGpsSession = gpsSessions.getOrNull(selectedGpsIdx) ?: gpsSessions.firstOrNull()
  val gpsPoints =
    remember(activeGpsSession) {
      activeGpsSession?.let {
        runCatching { terrainJson.decodeFromString<List<GpsPoint>>(it.routePolylineJson) }.getOrNull()
      }.orEmpty()
    }

  Scaffold(
    topBar = { VitalTopBar(title = "3D Wellness Studio", onBack = onBack) }
  ) { innerPadding ->
    Column(
      modifier =
        Modifier.fillMaxSize()
          .padding(innerPadding)
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 16.dp, vertical = 12.dp)
          .widthIn(max = 640.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        SpatialMode.entries.forEach { m ->
          FilterChip(
            selected = mode == m,
            onClick = { mode = m },
            leadingIcon = {
              Icon(
                imageVector =
                  when (m) {
                    SpatialMode.ORB -> Icons.Outlined.ViewInAr
                    SpatialMode.BODY -> Icons.Outlined.AccessibilityNew
                    SpatialMode.TERRAIN -> Icons.Outlined.Terrain
                  },
                contentDescription = null,
                modifier = Modifier.size(16.dp),
              )
            },
            label = { Text(m.label) },
            modifier = Modifier.testTag("spatial_mode_${m.key}"),
          )
        }
      }

      Tactile3DCard(
        enabled3D = preferences.threeDVisualsEnabled,
        modifier = Modifier.fillMaxWidth().testTag("spatial_3d_stage_card"),
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = "Drag to Orbit 360° • Yaw ${yawDeg.toInt()}° / Pitch ${pitchDeg.toInt()}°",
              style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFamily),
              color = MaterialTheme.colorScheme.primary,
            )
            OutlinedButton(
              onClick = {
                yawDeg = -24f
                pitchDeg = 16f
              },
              modifier = Modifier.testTag("spatial_3d_reset"),
            ) {
              Icon(Icons.Outlined.Refresh, contentDescription = "Reset 3D angle", modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Reset")
            }
          }

          when (mode) {
            SpatialMode.ORB -> {
              Spatial3DVitalityOrbCanvas(
                stepsProgress = stepProgress,
                waterProgress = waterProgress,
                sleepProgress = sleepProgress,
                tasksProgress = tasksProgress,
                stepColor = MaterialTheme.colorScheme.primary,
                waterColor = HydrationBlue,
                sleepColor = SleepIndigo,
                taskColor = GoldAccent,
                yawDeg = yawDeg,
                pitchDeg = pitchDeg,
                onDragDelta = { dx, dy ->
                  yawDeg += dx
                  pitchDeg = (pitchDeg + dy).coerceIn(-65f, 65f)
                },
                modifier = Modifier.testTag("spatial_orb_canvas"),
              )
            }
            SpatialMode.BODY -> {
              val look = outfitSwatches[selectedOutfitLook]
              Spatial3DMannequinCanvas(
                heightCm = preferences.heightCm,
                weightKg = preferences.weightKg,
                bodyBuild = "Balanced",
                skinColor = Color(0xFFC9936B),
                topColor = look.second,
                bottomColor = look.third,
                accentColor = GoldAccent,
                modifier = Modifier.testTag("spatial_body_canvas"),
              )
              Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                outfitSwatches.forEachIndexed { idx, spec ->
                  FilterChip(
                    selected = selectedOutfitLook == idx,
                    onClick = { selectedOutfitLook = idx },
                    label = { Text(spec.first) },
                  )
                }
              }
            }
            SpatialMode.TERRAIN -> {
              Spatial3DGpsTerrainCanvas(
                points = gpsPoints,
                routeColor = MaterialTheme.colorScheme.primary,
                accentColor = GoldAccent,
                yawDeg = yawDeg,
                pitchDeg = pitchDeg,
                onDragDelta = { dx, dy ->
                  yawDeg += dx
                  pitchDeg = (pitchDeg + dy).coerceIn(-55f, 55f)
                },
                modifier = Modifier.testTag("spatial_terrain_canvas"),
              )
              if (gpsSessions.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                  gpsSessions.take(3).forEachIndexed { idx, s ->
                    FilterChip(
                      selected = selectedGpsIdx == idx,
                      onClick = { selectedGpsIdx = idx },
                      label = { Text(s.title) },
                    )
                  }
                }
              } else {
                Text(
                  "No GPS sessions saved yet — showing sample 3D elevation preview. Record a GPS walk or run to view your route in 3D.",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            OutlinedButton(onClick = { yawDeg -= 30f }, modifier = Modifier.weight(1f)) {
              Text("Rotate -30°")
            }
            OutlinedButton(onClick = { yawDeg += 30f }, modifier = Modifier.weight(1f)) {
              Text("Rotate +30°")
            }
          }
        }
      }

      // Live Telemetry Summary Rings Legend
      Tactile3DCard(enabled3D = preferences.threeDVisualsEnabled) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("Live 3D Ring Telemetry", style = MaterialTheme.typography.titleMedium)
          SpatialLegendRow("Steps Orbit", "$todaySteps / ${preferences.dailyStepGoal}", MaterialTheme.colorScheme.primary)
          SpatialLegendRow("Hydration Orbit", "$todayWater / ${preferences.dailyWaterGoalMl} ml", HydrationBlue)
          SpatialLegendRow("Sleep Orbit", WellnessCalculators.formatMinutesToHours(latestSleepMin), SleepIndigo)
          SpatialLegendRow("Tasks Orbit", "$completedTasks / ${wellnessTasks.size} done", GoldAccent)
        }
      }
    }
  }
}

@Composable
private fun SpatialLegendRow(label: String, value: String, color: Color) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(color))
      Text(label, style = MaterialTheme.typography.bodyMedium)
    }
    Text(
      text = value,
      style = MaterialTheme.typography.labelLarge.copy(fontFamily = JetBrainsMonoFamily),
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

private data class BreathingProtocol(
  val key: String,
  val name: String,
  val inhaleSec: Int,
  val holdSec: Int,
  val exhaleSec: Int,
  val holdEmptySec: Int,
  val benefit: String,
)

private val breathingProtocols =
  listOf(
    BreathingProtocol("box", "Box 4-4-4-4", 4, 4, 4, 4, "Focus & steady nervous system"),
    BreathingProtocol("calm", "Calm 4-7-8", 4, 7, 8, 0, "Evening wind-down & stress relief"),
    BreathingProtocol("recovery", "Recovery 5-2-6", 5, 2, 6, 0, "Post-workout heart rate cooldown"),
  )

// 29. 3D Guided Breathing & Smart Wellness Calculators
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WellnessToolboxScreen(
  preferences: UserProfilePreferences,
  onApplyWaterGoalMl: (Int) -> Unit,
  onLogBreathingSession: (protocolName: String, cycles: Int) -> Unit,
  onBack: () -> Unit,
) {
  BackHandler(onBack = onBack)

  // Breathing State
  var selectedProtocol by remember { mutableStateOf(breathingProtocols.first()) }
  var isBreathingActive by remember { mutableStateOf(false) }
  var phaseIndex by remember { mutableIntStateOf(0) } // 0=Inhale, 1=Hold, 2=Exhale, 3=HoldEmpty
  var secondsLeft by remember(selectedProtocol) { mutableIntStateOf(selectedProtocol.inhaleSec) }
  var cyclesCompleted by remember { mutableIntStateOf(0) }

  val phaseDurations =
    remember(selectedProtocol) {
      listOf(
        "Inhale" to selectedProtocol.inhaleSec,
        "Hold" to selectedProtocol.holdSec,
        "Exhale" to selectedProtocol.exhaleSec,
        "Rest" to selectedProtocol.holdEmptySec,
      ).filter { it.second > 0 }
    }

  val activePhase = phaseDurations.getOrElse(phaseIndex % phaseDurations.size) { "Inhale" to 4 }

  LaunchedEffect(isBreathingActive, selectedProtocol) {
    if (!isBreathingActive) return@LaunchedEffect
    while (isBreathingActive) {
      delay(1000L)
      if (secondsLeft > 1) {
        secondsLeft -= 1
      } else {
        val nextIdx = phaseIndex + 1
        if (nextIdx >= phaseDurations.size) {
          cyclesCompleted += 1
          phaseIndex = 0
        } else {
          phaseIndex = nextIdx
        }
        secondsLeft = phaseDurations[phaseIndex].second
      }
    }
  }

  val sphereScale =
    remember(activePhase, secondsLeft) {
      val total = activePhase.second.coerceAtLeast(1).toFloat()
      val elapsedFrac = ((total - secondsLeft + 1) / total).coerceIn(0f, 1f)
      when (activePhase.first) {
        "Inhale" -> elapsedFrac
        "Hold" -> 1f
        "Exhale" -> 1f - elapsedFrac
        else -> 0.15f
      }
    }

  // Smart Calculators State
  var ageInput by remember(preferences.age) { mutableIntStateOf(preferences.age) }
  var extraActiveMin by remember(preferences.dailyWorkoutGoalMinutes) {
    mutableIntStateOf(preferences.dailyWorkoutGoalMinutes)
  }
  var activityFactorIdx by remember { mutableIntStateOf(2) }
  val activityLevels =
    remember {
      listOf(
        "Sedentary" to 1.2,
        "Light" to 1.375,
        "Moderate" to 1.55,
        "Athlete" to 1.725,
      )
    }

  val hrZones = remember(ageInput) { WellnessCalculators.calculateHeartRateZones(ageInput) }
  val recommendedWaterMl =
    remember(preferences.weightKg, extraActiveMin) {
      WellnessCalculators.calculateHydrationTargetMl(preferences.weightKg, extraActiveMin)
    }
  val estimatedTdee =
    remember(ageInput, preferences.heightCm, preferences.weightKg, activityFactorIdx) {
      WellnessCalculators.calculateDailyCalorieTdee(
        age = ageInput,
        heightCm = preferences.heightCm,
        weightKg = preferences.weightKg,
        activityMultiplier = activityLevels[activityFactorIdx].second,
      )
    }

  Scaffold(
    topBar = { VitalTopBar(title = "3D Breathing & Calculators", onBack = onBack) }
  ) { innerPadding ->
    Column(
      modifier =
        Modifier.fillMaxSize()
          .padding(innerPadding)
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 16.dp, vertical = 12.dp)
          .widthIn(max = 640.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      MedicalDisclaimerBanner()

      // 1. 3D Guided Breathing Studio
      Tactile3DCard(enabled3D = preferences.threeDVisualsEnabled) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Outlined.Air, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text("3D Guided Breathing", style = MaterialTheme.typography.titleMedium)
          }

          FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            breathingProtocols.forEach { p ->
              FilterChip(
                selected = selectedProtocol.key == p.key,
                onClick = {
                  isBreathingActive = false
                  selectedProtocol = p
                  phaseIndex = 0
                  secondsLeft = p.inhaleSec
                },
                label = { Text(p.name) },
                modifier = Modifier.testTag("breathing_protocol_${p.key}"),
              )
            }
          }
          Text(
            text = "${selectedProtocol.benefit} • $cyclesCompleted cycle(s) done",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )

          Breathing3DSphereCanvas(
            scaleFraction = sphereScale,
            phaseLabel = if (isBreathingActive) activePhase.first else "Ready",
            secondsLeft = if (isBreathingActive) secondsLeft else selectedProtocol.inhaleSec,
            coreColor = MaterialTheme.colorScheme.primary,
            haloColor = HydrationBlue,
            modifier = Modifier.testTag("breathing_3d_sphere"),
          )

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Button(
              onClick = { isBreathingActive = !isBreathingActive },
              modifier = Modifier.weight(1f).testTag("breathing_toggle_button"),
            ) {
              Icon(
                imageVector = if (isBreathingActive) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                contentDescription = null,
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(if (isBreathingActive) "Pause" else "Start")
            }
            OutlinedButton(
              onClick = {
                isBreathingActive = false
                phaseIndex = 0
                secondsLeft = selectedProtocol.inhaleSec
                cyclesCompleted = 0
              },
              modifier = Modifier.testTag("breathing_reset_button"),
            ) {
              Text("Reset")
            }
          }

          FilledTonalButton(
            onClick = { onLogBreathingSession(selectedProtocol.name, cyclesCompleted.coerceAtLeast(1)) },
            modifier = Modifier.fillMaxWidth().testTag("breathing_log_button"),
          ) {
            Text("Log Breathing Session to Journal")
          }
        }
      }

      // 2. Hydration & TDEE Energy Calculator
      Tactile3DCard(enabled3D = preferences.threeDVisualsEnabled) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Outlined.WaterDrop, contentDescription = null, tint = HydrationBlue)
            Text("Hydration & Calorie Calculators", style = MaterialTheme.typography.titleMedium)
          }

          Text(
            "Planned Daily Exercise: $extraActiveMin min (Weight ${preferences.weightKg.toInt()} kg)",
            style = MaterialTheme.typography.bodyMedium,
          )
          Slider(
            value = extraActiveMin.toFloat(),
            onValueChange = { extraActiveMin = it.toInt() },
            valueRange = 0f..180f,
            modifier = Modifier.testTag("calc_active_min_slider"),
          )

          Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(12.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column {
                Text("Recommended Daily Water", style = MaterialTheme.typography.labelMedium)
                Text(
                  text = WellnessCalculators.formatVolume(recommendedWaterMl, preferences.useImperialUnits),
                  style = MaterialTheme.typography.headlineSmall.copy(fontFamily = JetBrainsMonoFamily),
                  modifier = Modifier.testTag("calc_recommended_water_text"),
                )
              }
              Button(
                onClick = { onApplyWaterGoalMl(recommendedWaterMl) },
                modifier = Modifier.testTag("calc_apply_water_goal"),
              ) {
                Text("Apply Goal")
              }
            }
          }

          HorizontalDivider()

          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Outlined.LocalFireDepartment, contentDescription = null, tint = GoldAccent)
            Text("Reference Daily Energy (TDEE): $estimatedTdee kcal/day", style = MaterialTheme.typography.titleSmall)
          }
          FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            activityLevels.forEachIndexed { idx, (label, _) ->
              FilterChip(
                selected = activityFactorIdx == idx,
                onClick = { activityFactorIdx = idx },
                label = { Text(label) },
              )
            }
          }
        }
      }

      // 3. Heart-Rate Training Zones Calculator
      Tactile3DCard(enabled3D = preferences.threeDVisualsEnabled) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              Icon(Icons.Outlined.FavoriteBorder, contentDescription = null, tint = HeartRateCoral)
              Text("Heart-Rate Training Zones", style = MaterialTheme.typography.titleMedium)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
              IconButton(
                onClick = { ageInput = (ageInput - 1).coerceAtLeast(12) },
                modifier = Modifier.testTag("calc_age_minus"),
              ) {
                Icon(Icons.Outlined.Remove, contentDescription = "Decrease age")
              }
              Text(
                text = "Age $ageInput",
                style = MaterialTheme.typography.labelLarge.copy(fontFamily = JetBrainsMonoFamily),
              )
              IconButton(
                onClick = { ageInput = (ageInput + 1).coerceAtMost(95) },
                modifier = Modifier.testTag("calc_age_plus"),
              ) {
                Icon(Icons.Outlined.Add, contentDescription = "Increase age")
              }
            }
          }

          hrZones.forEach { z ->
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = MaterialTheme.colorScheme.surfaceVariant,
              modifier = Modifier.fillMaxWidth(),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(z.label, style = MaterialTheme.typography.titleSmall)
                  Text(
                    z.purpose,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
                Text(
                  text = "${z.minBpm}–${z.maxBpm} bpm",
                  style = MaterialTheme.typography.labelLarge.copy(fontFamily = JetBrainsMonoFamily),
                  color = HeartRateCoral,
                )
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))
    }
  }
}
