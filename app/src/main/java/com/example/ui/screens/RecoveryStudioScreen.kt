package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.RemoveRedEye
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.health.BiofeedbackAudioEngine
import com.example.data.health.SoundscapeMode
import com.example.data.health.WellnessAdvancedCalculators
import com.example.data.health.WellnessCalculators
import com.example.data.local.HeartRateRecordEntity
import com.example.data.local.SleepRecordEntity
import com.example.data.local.UserProfilePreferences
import com.example.ui.components.Tactile3DCard
import com.example.ui.components.VitalTopBar
import com.example.ui.theme.JetBrainsMonoFamily
import kotlinx.coroutines.delay
import java.util.Locale

/**
 * Sleep, Recovery, Biofeedback Soundscapes & Mindfulness Studio (#61-#70).
 */
@Composable
fun RecoveryStudioScreen(
  preferences: UserProfilePreferences,
  sleepRecords: List<SleepRecordEntity>,
  heartRates: List<HeartRateRecordEntity>,
  onSetWindDownTime: (hour: Int, minute: Int) -> Unit,
  onToggleScreenBreakReminder: (Boolean) -> Unit,
  onSaveStressTriggerJournal: (tag: String, note: String) -> Unit,
  onBack: () -> Unit,
) {
  BackHandler(onBack = onBack)
  val context = LocalContext.current

  var windHour by remember(preferences.windDownHour) { mutableIntStateOf(preferences.windDownHour) }
  var windMin by remember(preferences.windDownMinute) { mutableIntStateOf(preferences.windDownMinute) }

  var soreness by remember { mutableFloatStateOf(2f) }
  var mood by remember { mutableFloatStateOf(4f) }

  var soundMode by remember { mutableStateOf(SoundscapeMode.OFF) }
  var soundVolume by remember { mutableFloatStateOf(0.4f) }
  var hapticBreathActive by remember { mutableStateOf(false) }

  var napSecLeft by remember { mutableIntStateOf(0) }
  var pmrStep by remember { mutableIntStateOf(0) }
  var selectedStressTag by remember { mutableStateOf("Screen Fatigue") }

  val pmrSteps = remember {
    listOf(
      "Hands & Forearms: Clench fists gently for 5s, then release completely for 10s.",
      "Shoulders & Neck: Shrug shoulders toward ears for 5s, exhale and drop softly.",
      "Jaw & Brow: Soften teeth apart, unclench forehead tension for 10s.",
      "Core & Chest: Take a deep belly breath, brace lightly 4s, exhale slowly.",
      "Calves & Feet: Flex toes upward for 5s, relax into the floor.",
    )
  }

  DisposableEffect(Unit) {
    onDispose { BiofeedbackAudioEngine.stopSoundscape() }
  }

  LaunchedEffect(hapticBreathActive) {
    while (hapticBreathActive) {
      BiofeedbackAudioEngine.triggerHapticPulse(context, durationMs = 70L, amplitude = 110)
      delay(4000L)
    }
  }

  LaunchedEffect(napSecLeft) {
    while (napSecLeft > 0) {
      delay(1000L)
      napSecLeft--
      if (napSecLeft == 0) {
        BiofeedbackAudioEngine.playShortTone()
        BiofeedbackAudioEngine.triggerHapticPulse(context, 220L, 180)
      }
    }
  }

  val sleepMetrics = remember(sleepRecords, preferences.dailySleepGoalMinutes) {
    WellnessAdvancedCalculators.calculateSleepCircadianMetrics(sleepRecords, preferences.dailySleepGoalMinutes)
  }
  val latestHr = heartRates.firstOrNull()?.bpm ?: 66
  val latestSleepMin = sleepRecords.firstOrNull()?.durationMinutes ?: preferences.dailySleepGoalMinutes
  val readiness = WellnessAdvancedCalculators.calculateReadinessScore(
    sleepMinutes = latestSleepMin,
    sleepGoalMinutes = preferences.dailySleepGoalMinutes,
    restingHr = latestHr,
    sorenessLevel1To5 = soreness.toInt(),
    moodLevel1To5 = mood.toInt(),
  )

  Scaffold(topBar = { VitalTopBar(title = "Sleep & Recovery Studio", onBack = onBack) }) { innerPadding ->
    Column(
      modifier =
        Modifier.fillMaxSize()
          .padding(innerPadding)
          .verticalScroll(rememberScrollState())
          .padding(16.dp)
          .widthIn(max = 640.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      // #62 Sleep Debt & Circadian Consistency + #67 RHR Fatigue Check
      Tactile3DCard(enabled3D = preferences.threeDVisualsEnabled) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Outlined.Bedtime, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text("Circadian Consistency & Recovery", style = MaterialTheme.typography.titleMedium)
          }
          Text(
            "7-Night Avg: ${WellnessCalculators.formatMinutesToHours(sleepMetrics.avgDurationMinutes)} • Weekly Debt: ${WellnessCalculators.formatMinutesToHours(sleepMetrics.weeklyDebtMinutes)}",
            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = JetBrainsMonoFamily),
          )
          Text(
            "Bedtime Consistency: ${sleepMetrics.consistencyScore}% • Latest HR: $latestHr BPM" +
              if (latestHr > 78) " (Elevated resting pulse — favor Z1/Z2 recovery)" else " (Recovered cardiovascular baseline)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      // #63 Morning Readiness Score
      Tactile3DCard(enabled3D = preferences.threeDVisualsEnabled) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("Morning Readiness Score: $readiness / 100", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
          Text("Muscle Soreness (${soreness.toInt()}/5)", style = MaterialTheme.typography.labelMedium)
          Slider(value = soreness, onValueChange = { soreness = it }, valueRange = 1f..5f, steps = 3)
          Text("Mental Energy (${mood.toInt()}/5)", style = MaterialTheme.typography.labelMedium)
          Slider(value = mood, onValueChange = { mood = it }, valueRange = 1f..5f, steps = 3)
        }
      }

      // #64 Synthesized Ambient Soundscape Mixer + #65 Haptic Breathing + #66 NSDR Timer
      Tactile3DCard(enabled3D = preferences.threeDVisualsEnabled) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Outlined.GraphicEq, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text("Synthesized Soundscape & NSDR Nap", style = MaterialTheme.typography.titleMedium)
          }
          Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            SoundscapeMode.entries.forEach { mode ->
              FilterChip(
                selected = soundMode == mode,
                onClick = {
                  soundMode = mode
                  BiofeedbackAudioEngine.startSoundscape(mode, soundVolume)
                },
                label = { Text(mode.label) },
                modifier = Modifier.testTag("recovery_sound_${mode.name.lowercase()}"),
              )
            }
          }
          Slider(
            value = soundVolume,
            onValueChange = {
              soundVolume = it
              if (soundMode != SoundscapeMode.OFF) BiofeedbackAudioEngine.startSoundscape(soundMode, it)
            },
            valueRange = 0.05f..0.9f,
          )
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Outlined.Vibration, contentDescription = null)
              Spacer(modifier = Modifier.width(6.dp))
              Text("Eyes-Closed 4s Haptic Breath Cue", style = MaterialTheme.typography.bodyMedium)
            }
            Switch(checked = hapticBreathActive, onCheckedChange = { hapticBreathActive = it }, modifier = Modifier.testTag("recovery_haptic_switch"))
          }
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = { napSecLeft = 10 * 60 }) { Text("10m NSDR") }
            OutlinedButton(onClick = { napSecLeft = 20 * 60 }) { Text("20m Power Nap") }
            if (napSecLeft > 0) {
              OutlinedButton(onClick = { napSecLeft = 0 }) { Text("Stop (${WellnessCalculators.formatDurationMillis(napSecLeft * 1000L)})") }
            }
          }
        }
      }

      // #61 Smart Wind-Down + #68 20-20-20 Eye Break WorkManager
      Tactile3DCard(enabled3D = preferences.threeDVisualsEnabled) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Outlined.RemoveRedEye, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text("Wind-Down Bedtime & 20-20-20 Eye Breaks", style = MaterialTheme.typography.titleMedium)
          }
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
              String.format(Locale.US, "Wind-down target: %02d:%02d", windHour, windMin),
              style = MaterialTheme.typography.bodyMedium.copy(fontFamily = JetBrainsMonoFamily),
              modifier = Modifier.weight(1f),
            )
            OutlinedButton(onClick = { windHour = (windHour - 1 + 24) % 24 }) { Text("-1h") }
            OutlinedButton(onClick = { windHour = (windHour + 1) % 24 }) { Text("+1h") }
            Button(onClick = { onSetWindDownTime(windHour, windMin) }) { Text("Set") }
          }
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
              Text("20-20-20 Screen Eye-Break Reminders", style = MaterialTheme.typography.bodyMedium)
              Text("WorkManager gentle cue every ~2 hours during screen use", style = MaterialTheme.typography.labelSmall)
            }
            Switch(
              checked = preferences.screenBreakReminderEnabled,
              onCheckedChange = onToggleScreenBreakReminder,
              modifier = Modifier.testTag("recovery_eyebreak_switch"),
            )
          }
        }
      }

      // #69 Stress Trigger Tagging + #70 Guided PMR
      Tactile3DCard(enabled3D = preferences.threeDVisualsEnabled) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Outlined.SelfImprovement, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text("PMR Muscle Relaxation & Stress Tags", style = MaterialTheme.typography.titleMedium)
          }
          Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(10.dp)) {
            Text(
              "Step ${pmrStep + 1}/5: ${pmrSteps[pmrStep]}",
              style = MaterialTheme.typography.bodyMedium,
              modifier = Modifier.padding(12.dp),
            )
          }
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { pmrStep = (pmrStep + 4) % pmrSteps.size }, modifier = Modifier.weight(1f)) { Text("Previous") }
            Button(onClick = {
              pmrStep = (pmrStep + 1) % pmrSteps.size
              BiofeedbackAudioEngine.triggerHapticPulse(context, 45L, 90)
            }, modifier = Modifier.weight(1f).testTag("recovery_pmr_next")) { Text("Next Muscle Group") }
          }
          Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("Screen Fatigue", "Work Deadline", "Travel", "Poor Sleep").forEach { t ->
              FilterChip(selected = selectedStressTag == t, onClick = { selectedStressTag = t }, label = { Text(t) })
            }
          }
          OutlinedButton(
            onClick = { onSaveStressTriggerJournal(selectedStressTag, "Completed PMR sequence & logged stress tag: $selectedStressTag") },
            modifier = Modifier.fillMaxWidth().testTag("recovery_log_stress_btn"),
          ) { Text("Log Stress Recovery to Journal") }
        }
      }
      Spacer(modifier = Modifier.height(20.dp))
    }
  }
}
