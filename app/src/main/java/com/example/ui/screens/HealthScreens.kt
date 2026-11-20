package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.MonitorWeight
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.health.HealthConnectAvailability
import com.example.data.health.WellnessCalculators
import com.example.data.local.HeartRateRecordEntity
import com.example.data.local.JournalEntryEntity
import com.example.data.local.SleepRecordEntity
import com.example.data.local.UserProfilePreferences
import com.example.data.local.WaterIntakeEntity
import com.example.data.local.WeightEntryEntity
import com.example.ui.components.LineTrendChart
import com.example.ui.components.MedicalDisclaimerBanner
import com.example.ui.components.MetricSummaryCard
import com.example.ui.components.VitalTopBar
import com.example.ui.theme.HeartRateCoral
import com.example.ui.theme.HydrationBlue
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.SleepIndigo
import com.example.ui.viewmodel.SyncUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// 11. Health Dashboard Screen (Bottom Tab 4)
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HealthDashboardScreen(
  preferences: UserProfilePreferences,
  heartRates: List<HeartRateRecordEntity>,
  sleepRecords: List<SleepRecordEntity>,
  waterIntakes: List<WaterIntakeEntity>,
  weightEntries: List<WeightEntryEntity>,
  journalEntries: List<JournalEntryEntity>,
  healthConnectAvailability: HealthConnectAvailability,
  syncState: SyncUiState,
  onSyncHealthConnect: () -> Unit,
  onOpenPermissions: () -> Unit,
  onOpenHeartRate: () -> Unit,
  onOpenSleep: () -> Unit,
  onOpenWater: () -> Unit,
  onOpenWeight: () -> Unit,
  onSaveJournal: (mood: String, energyLevel: Int, note: String) -> Unit,
) {
  val todayIso = remember { WellnessCalculators.todayIso() }
  val todayWaterMl = waterIntakes.filter { it.dateIso == todayIso }.sumOf { it.amountMl }
  val latestHr = heartRates.firstOrNull()
  val latestSleep = sleepRecords.firstOrNull()
  val latestWeight = weightEntries.firstOrNull()

  val moods = listOf("GREAT", "GOOD", "CALM", "TIRED", "STRESSED")
  var selectedMood by remember { mutableStateOf("GOOD") }
  var energyLevel by remember { mutableIntStateOf(4) }
  var journalNote by remember { mutableStateOf("") }

  Column(
    modifier =
      Modifier.fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 16.dp, vertical = 12.dp)
        .widthIn(max = 640.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    Text("Personal Health", style = MaterialTheme.typography.headlineMedium)

    MedicalDisclaimerBanner()

    // Health Connect Integration Card
    Card(
      shape = MaterialTheme.shapes.medium,
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
      Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text("Health Connect Sync", style = MaterialTheme.typography.titleMedium)
            Text(
              text =
                when (healthConnectAvailability) {
                  HealthConnectAvailability.AVAILABLE ->
                    "Reads verified steps, heart rate, sleep stages, and weight from connected wearables or Health Connect."
                  HealthConnectAvailability.NOT_INSTALLED ->
                    "Health Connect app is not installed on this device. Manual tracking remains active."
                  HealthConnectAvailability.NOT_SUPPORTED ->
                    "Health Connect requires Android 9+. Use manual entries below."
                },
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          FilledTonalButton(
            onClick = onSyncHealthConnect,
            enabled =
              healthConnectAvailability == HealthConnectAvailability.AVAILABLE &&
                syncState !is SyncUiState.Syncing,
            modifier = Modifier.testTag("health_sync_hc_button"),
          ) {
            if (syncState is SyncUiState.Syncing) {
              CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            } else {
              Icon(Icons.Outlined.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text("Sync Health Connect")
          }
          OutlinedButton(
            onClick = onOpenPermissions,
            modifier = Modifier.testTag("health_permissions_button"),
          ) {
            Text("Permissions")
          }
        }
      }
    }

    // 2x2 Health Modules Grid
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      MetricSummaryCard(
        title = stringResource(R.string.heart_rate_label),
        value = latestHr?.let { "${it.bpm} bpm" } ?: "No data",
        subtitle = latestHr?.source ?: "Connect wearable",
        icon = Icons.Outlined.FavoriteBorder,
        accentColor = HeartRateCoral,
        onClick = onOpenHeartRate,
        modifier = Modifier.weight(1f).testTag("health_card_heart_rate"),
      )
      MetricSummaryCard(
        title = stringResource(R.string.sleep_label),
        value =
          latestSleep?.let { WellnessCalculators.formatMinutesToHours(it.durationMinutes) }
            ?: "No record",
        subtitle = latestSleep?.let { "${it.qualityScore}% • ${it.source}" } ?: "Tap to log sleep",
        icon = Icons.Outlined.Bedtime,
        accentColor = SleepIndigo,
        onClick = onOpenSleep,
        modifier = Modifier.weight(1f).testTag("health_card_sleep"),
      )
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      MetricSummaryCard(
        title = stringResource(R.string.water_label),
        value = WellnessCalculators.formatVolume(todayWaterMl, preferences.useImperialUnits),
        subtitle = "Goal ${WellnessCalculators.formatVolume(preferences.dailyWaterGoalMl, preferences.useImperialUnits)}",
        icon = Icons.Outlined.WaterDrop,
        accentColor = HydrationBlue,
        onClick = onOpenWater,
        modifier = Modifier.weight(1f).testTag("health_card_water"),
      )
      val weightDisplayKg = latestWeight?.weightKg ?: preferences.weightKg
      val bmiVal =
        latestWeight?.bmi
          ?: WellnessCalculators.calculateBmi(preferences.weightKg, preferences.heightCm)
      MetricSummaryCard(
        title = stringResource(R.string.weight_label),
        value = WellnessCalculators.formatWeight(weightDisplayKg, preferences.useImperialUnits),
        subtitle = "BMI ${String.format(Locale.US, "%.1f", bmiVal)} (${WellnessCalculators.bmiCategory(bmiVal)})",
        icon = Icons.Outlined.MonitorWeight,
        accentColor = MaterialTheme.colorScheme.primary,
        onClick = onOpenWeight,
        modifier = Modifier.weight(1f).testTag("health_card_weight"),
      )
    }

    // Daily Health Journal
    Card(
      shape = MaterialTheme.shapes.large,
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
      Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Daily Health Journal", style = MaterialTheme.typography.titleMedium)
        Text("Mood", style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          moods.forEach { mood ->
            FilterChip(
              selected = selectedMood == mood,
              onClick = { selectedMood = mood },
              label = { Text(mood.lowercase().replaceFirstChar { it.uppercase() }) },
            )
          }
        }
        Text(
          text = "Energy Level: $energyLevel / 5",
          style = MaterialTheme.typography.labelLarge,
        )
        Slider(
          value = energyLevel.toFloat(),
          onValueChange = { energyLevel = it.toInt().coerceIn(1, 5) },
          valueRange = 1f..5f,
          steps = 3,
        )
        OutlinedTextField(
          value = journalNote,
          onValueChange = { journalNote = it },
          label = { Text("Wellness reflection or observations") },
          modifier = Modifier.fillMaxWidth().testTag("journal_note_input"),
          minLines = 2,
          maxLines = 4,
        )
        Button(
          onClick = {
            onSaveJournal(selectedMood, energyLevel, journalNote)
            journalNote = ""
          },
          modifier = Modifier.fillMaxWidth().height(48.dp).testTag("save_journal_button"),
        ) {
          Text("Save Journal Entry")
        }

        if (journalEntries.isNotEmpty()) {
          Spacer(modifier = Modifier.height(4.dp))
          Text("Recent Reflections", style = MaterialTheme.typography.titleSmall)
          journalEntries.take(4).forEach { entry ->
            Surface(
              color = MaterialTheme.colorScheme.surfaceVariant,
              shape = MaterialTheme.shapes.small,
              modifier = Modifier.fillMaxWidth(),
            ) {
              Column(modifier = Modifier.padding(12.dp)) {
                Text(
                  text = "${entry.dateIso} • Mood: ${entry.mood} • Energy ${entry.energyLevel}/5",
                  style = MaterialTheme.typography.labelMedium,
                  color = MaterialTheme.colorScheme.primary,
                )
                if (entry.note.isNotBlank()) {
                  Spacer(modifier = Modifier.height(2.dp))
                  Text(entry.note, style = MaterialTheme.typography.bodySmall)
                }
              }
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(16.dp))
  }
}

// 12. Heart Rate History Screen
@Composable
fun HeartRateScreen(
  heartRates: List<HeartRateRecordEntity>,
  healthConnectAvailability: HealthConnectAvailability,
  onSyncHealthConnect: () -> Unit,
  onBack: () -> Unit,
) {
  BackHandler(onBack = onBack)

  val recentValues = remember(heartRates) { heartRates.take(16).reversed().map { it.bpm.toFloat() } }
  val recentLabels =
    remember(heartRates) {
      heartRates.take(16).reversed().map { it.dateIso }
    }
  val timeFormat = remember { SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()) }

  Scaffold(topBar = { VitalTopBar(title = "Heart Rate History", onBack = onBack) }) { innerPadding ->
    Column(
      modifier =
        Modifier.fillMaxSize()
          .padding(innerPadding)
          .verticalScroll(rememberScrollState())
          .padding(16.dp)
          .widthIn(max = 640.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
        shape = MaterialTheme.shapes.medium,
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text(
            text = "Data Source & Device Accuracy Notice",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
          )
          Text(
            text = "VITALTRACK displays heart-rate measurements ONLY from connected wearables or Health Connect. An ordinary smartphone camera or GPS sensor is never a continuous medical-grade heart-rate monitor.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
          )
        }
      }

      MedicalDisclaimerBanner()

      if (healthConnectAvailability == HealthConnectAvailability.AVAILABLE) {
        FilledTonalButton(
          onClick = onSyncHealthConnect,
          modifier = Modifier.fillMaxWidth().height(48.dp).testTag("hr_sync_button"),
        ) {
          Icon(Icons.Outlined.Sync, contentDescription = null)
          Spacer(modifier = Modifier.width(8.dp))
          Text("Refresh from Health Connect")
        }
      }

      if (heartRates.isEmpty()) {
        Card(
          shape = MaterialTheme.shapes.medium,
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
          Column(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("No Heart-Rate Readings Yet", style = MaterialTheme.typography.titleMedium)
            Text(
              text = "Grant Health Connect permission and sync a supported wearable or fitness band to view historical BPM measurements here.",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      } else {
        val avgBpm = heartRates.map { it.bpm }.average().toInt()
        val minBpm = heartRates.minOf { it.bpm }
        val maxBpm = heartRates.maxOf { it.bpm }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          MetricSummaryCard("Average", "$avgBpm bpm", "Recorded", Icons.Outlined.FavoriteBorder, HeartRateCoral, modifier = Modifier.weight(1f))
          MetricSummaryCard("Range", "$minBpm–$maxBpm", "Min–Max BPM", Icons.Outlined.FavoriteBorder, MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
        }

        Card(
          shape = MaterialTheme.shapes.medium,
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Text("Recent Trend", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            LineTrendChart(values = recentValues, labels = recentLabels, lineColor = HeartRateCoral)
          }
        }

        heartRates.take(30).forEach { record ->
          Card(
            shape = MaterialTheme.shapes.small,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(14.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Column {
                Text(
                  "${record.bpm} bpm",
                  style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFamily),
                )
                Text(
                  "${timeFormat.format(Date(record.recordedAtMillis))} • ${record.source}",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              if (record.notes.isNotBlank()) {
                Text(
                  record.notes,
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }
        }
      }
    }
  }
}

// 13. Sleep Dashboard Screen
@Composable
fun SleepDashboardScreen(
  preferences: UserProfilePreferences,
  sleepRecords: List<SleepRecordEntity>,
  onLogManualSleep: (durationMinutes: Int, qualityScore: Int, notes: String) -> Unit,
  onSyncHealthConnect: () -> Unit,
  onBack: () -> Unit,
) {
  BackHandler(onBack = onBack)

  var showAddDialog by remember { mutableStateOf(false) }
  var hoursDraft by remember { mutableStateOf("7.5") }
  var qualityDraft by remember { mutableIntStateOf(80) }
  var notesDraft by remember { mutableStateOf("") }

  Scaffold(topBar = { VitalTopBar(title = "Sleep Dashboard", onBack = onBack) }) { innerPadding ->
    Column(
      modifier =
        Modifier.fillMaxSize()
          .padding(innerPadding)
          .verticalScroll(rememberScrollState())
          .padding(16.dp)
          .widthIn(max = 640.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      MedicalDisclaimerBanner()

      Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Button(
          onClick = { showAddDialog = true },
          modifier = Modifier.weight(1f).height(48.dp).testTag("sleep_log_manual_button"),
        ) {
          Icon(Icons.Outlined.Add, contentDescription = null)
          Spacer(modifier = Modifier.width(6.dp))
          Text("Log Sleep")
        }
        OutlinedButton(
          onClick = onSyncHealthConnect,
          modifier = Modifier.weight(1f).height(48.dp).testTag("sleep_sync_hc_button"),
        ) {
          Icon(Icons.Outlined.Sync, contentDescription = null)
          Spacer(modifier = Modifier.width(6.dp))
          Text("Sync Wearable")
        }
      }

      val latest = sleepRecords.firstOrNull()
      if (latest == null) {
        Card(
          shape = MaterialTheme.shapes.medium,
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
          Column(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("No Sleep Sessions Recorded", style = MaterialTheme.typography.titleMedium)
            Text(
              "Goal: ${WellnessCalculators.formatMinutesToHours(preferences.dailySleepGoalMinutes)} per night. Log sleep manually or sync from Health Connect.",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      } else {
        Card(
          shape = MaterialTheme.shapes.large,
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
          Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Latest Sleep (${latest.dateIso})", style = MaterialTheme.typography.titleMedium)
            Text(
              text = WellnessCalculators.formatMinutesToHours(latest.durationMinutes),
              style = MaterialTheme.typography.displaySmall.copy(fontFamily = JetBrainsMonoFamily),
              color = SleepIndigo,
            )
            Text(
              text = "Quality score: ${latest.qualityScore}% • Source: ${latest.source}",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val total = latest.durationMinutes.coerceAtLeast(1).toFloat()
            SleepStageBar("Deep", latest.deepMinutes, total, SleepIndigo)
            SleepStageBar("Light", latest.lightMinutes, total, MaterialTheme.colorScheme.secondary)
            SleepStageBar("REM", latest.remMinutes, total, MaterialTheme.colorScheme.tertiary)
            SleepStageBar("Awake", latest.awakeMinutes, total, MaterialTheme.colorScheme.outline)
          }
        }

        sleepRecords.forEach { rec ->
          Card(
            shape = MaterialTheme.shapes.small,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(14.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column {
                Text(
                  "${rec.dateIso} — ${WellnessCalculators.formatMinutesToHours(rec.durationMinutes)}",
                  style = MaterialTheme.typography.titleSmall,
                )
                Text(
                  "Source: ${rec.source} • Quality ${rec.qualityScore}%" +
                    if (rec.notes.isNotBlank()) " • ${rec.notes}" else "",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }
        }
      }
    }
  }

  if (showAddDialog) {
    AlertDialog(
      onDismissRequest = { showAddDialog = false },
      title = { Text("Log Sleep Manually") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          OutlinedTextField(
            value = hoursDraft,
            onValueChange = { hoursDraft = it },
            label = { Text("Duration (hours, e.g. 7.5)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.testTag("sleep_hours_input"),
          )
          Text("Restfulness Score: $qualityDraft%")
          Slider(
            value = qualityDraft.toFloat(),
            onValueChange = { qualityDraft = it.toInt() },
            valueRange = 20f..100f,
          )
          OutlinedTextField(
            value = notesDraft,
            onValueChange = { notesDraft = it },
            label = { Text("Notes (optional)") },
            singleLine = true,
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            val mins = ((hoursDraft.toDoubleOrNull() ?: 7.5) * 60).toInt().coerceIn(30, 960)
            onLogManualSleep(mins, qualityDraft, notesDraft)
            notesDraft = ""
            showAddDialog = false
          },
          modifier = Modifier.testTag("sleep_confirm_button"),
        ) {
          Text(stringResource(R.string.save_label))
        }
      },
      dismissButton = {
        TextButton(onClick = { showAddDialog = false }) {
          Text(stringResource(R.string.cancel_label))
        }
      },
    )
  }
}

@Composable
private fun SleepStageBar(
  label: String,
  minutes: Int,
  totalMinutes: Float,
  color: androidx.compose.ui.graphics.Color,
) {
  Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
      Text(label, style = MaterialTheme.typography.labelMedium)
      Text("${minutes}m", style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFamily))
    }
    LinearProgressIndicator(
      progress = { (minutes / totalMinutes).coerceIn(0f, 1f) },
      modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(4.dp)),
      color = color,
    )
  }
}

// 14. Water Tracker Screen
@Composable
fun WaterTrackerScreen(
  preferences: UserProfilePreferences,
  waterIntakes: List<WaterIntakeEntity>,
  onAddWater: (Int) -> Unit,
  onDeleteWater: (String) -> Unit,
  onBack: () -> Unit,
) {
  BackHandler(onBack = onBack)

  val todayIso = remember { WellnessCalculators.todayIso() }
  val todayEntries = remember(waterIntakes) { waterIntakes.filter { it.dateIso == todayIso } }
  val todayMl = todayEntries.sumOf { it.amountMl }
  val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

  var showCustomDialog by remember { mutableStateOf(false) }
  var customAmountText by remember { mutableStateOf("350") }

  Scaffold(topBar = { VitalTopBar(title = "Hydration Tracker", onBack = onBack) }) { innerPadding ->
    Column(
      modifier =
        Modifier.fillMaxSize()
          .padding(innerPadding)
          .verticalScroll(rememberScrollState())
          .padding(16.dp)
          .widthIn(max = 640.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text("Today's Hydration", style = MaterialTheme.typography.titleMedium)
          Text(
            text = "${WellnessCalculators.formatVolume(todayMl, preferences.useImperialUnits)} / ${WellnessCalculators.formatVolume(preferences.dailyWaterGoalMl, preferences.useImperialUnits)}",
            style = MaterialTheme.typography.headlineMedium.copy(fontFamily = JetBrainsMonoFamily),
            color = HydrationBlue,
          )
          LinearProgressIndicator(
            progress = {
              (todayMl.toFloat() / preferences.dailyWaterGoalMl.coerceAtLeast(1)).coerceIn(0f, 1f)
            },
            modifier = Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(6.dp)),
            color = HydrationBlue,
          )
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(150, 250, 500).forEach { ml ->
              FilledTonalButton(
                onClick = { onAddWater(ml) },
                modifier = Modifier.weight(1f).height(48.dp).testTag("water_add_$ml"),
              ) {
                Text("+${ml}ml")
              }
            }
          }
          OutlinedButton(
            onClick = { showCustomDialog = true },
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("water_custom_button"),
          ) {
            Icon(Icons.Outlined.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Custom Amount (ml)")
          }
        }
      }

      Text("Today's Log (${todayEntries.size})", style = MaterialTheme.typography.titleMedium)
      if (todayEntries.isEmpty()) {
        Text(
          "No water logged today yet.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      } else {
        todayEntries.forEach { entry ->
          Card(
            shape = MaterialTheme.shapes.small,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.WaterDrop, contentDescription = null, tint = HydrationBlue)
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                  Text(
                    WellnessCalculators.formatVolume(entry.amountMl, preferences.useImperialUnits),
                    style = MaterialTheme.typography.titleSmall,
                  )
                  Text(
                    "Logged at ${timeFormat.format(Date(entry.recordedAtMillis))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
              IconButton(onClick = { onDeleteWater(entry.id) }) {
                Icon(Icons.Outlined.DeleteOutline, contentDescription = "Delete entry")
              }
            }
          }
        }
      }
    }
  }

  if (showCustomDialog) {
    AlertDialog(
      onDismissRequest = { showCustomDialog = false },
      title = { Text("Add Custom Water Amount") },
      text = {
        OutlinedTextField(
          value = customAmountText,
          onValueChange = { customAmountText = it.filter(Char::isDigit) },
          label = { Text("Amount (ml)") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          singleLine = true,
        )
      },
      confirmButton = {
        Button(
          onClick = {
            val ml = customAmountText.toIntOrNull()?.coerceIn(10, 3000) ?: 250
            onAddWater(ml)
            showCustomDialog = false
          }
        ) {
          Text(stringResource(R.string.save_label))
        }
      },
      dismissButton = {
        TextButton(onClick = { showCustomDialog = false }) {
          Text(stringResource(R.string.cancel_label))
        }
      },
    )
  }
}

// 15. Weight Tracker Screen
@Composable
fun WeightTrackerScreen(
  preferences: UserProfilePreferences,
  weightEntries: List<WeightEntryEntity>,
  onAddWeight: (weightKg: Double, notes: String) -> Unit,
  onBack: () -> Unit,
) {
  BackHandler(onBack = onBack)

  var showAddDialog by remember { mutableStateOf(false) }
  var weightText by remember(preferences.weightKg) { mutableStateOf(preferences.weightKg.toString()) }
  var notesText by remember { mutableStateOf("") }

  val currentWeightKg = weightEntries.firstOrNull()?.weightKg ?: preferences.weightKg
  val currentBmi = WellnessCalculators.calculateBmi(currentWeightKg, preferences.heightCm)
  val chartValues = remember(weightEntries) { weightEntries.take(15).reversed().map { it.weightKg.toFloat() } }
  val chartLabels = remember(weightEntries) { weightEntries.take(15).reversed().map { it.dateIso } }

  Scaffold(topBar = { VitalTopBar(title = "Weight & BMI", onBack = onBack) }) { innerPadding ->
    Column(
      modifier =
        Modifier.fillMaxSize()
          .padding(innerPadding)
          .verticalScroll(rememberScrollState())
          .padding(16.dp)
          .widthIn(max = 640.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      MedicalDisclaimerBanner()

      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        MetricSummaryCard(
          title = "Current Weight",
          value = WellnessCalculators.formatWeight(currentWeightKg, preferences.useImperialUnits),
          subtitle = "Height ${preferences.heightCm.toInt()} cm",
          icon = Icons.Outlined.MonitorWeight,
          accentColor = MaterialTheme.colorScheme.primary,
          modifier = Modifier.weight(1f),
        )
        MetricSummaryCard(
          title = "BMI Estimate",
          value = String.format(Locale.US, "%.1f", currentBmi),
          subtitle = WellnessCalculators.bmiCategory(currentBmi),
          icon = Icons.Outlined.MonitorWeight,
          accentColor = MaterialTheme.colorScheme.tertiary,
          modifier = Modifier.weight(1f),
        )
      }

      Button(
        onClick = { showAddDialog = true },
        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("weight_add_button"),
      ) {
        Icon(Icons.Outlined.Add, contentDescription = null)
        Spacer(modifier = Modifier.width(6.dp))
        Text("Log Weight Entry")
      }

      if (chartValues.isNotEmpty()) {
        Card(
          shape = MaterialTheme.shapes.medium,
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Text("Weight Progress", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            LineTrendChart(values = chartValues, labels = chartLabels)
          }
        }
      }

      weightEntries.forEach { entry ->
        Card(
          shape = MaterialTheme.shapes.small,
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column {
              Text(
                WellnessCalculators.formatWeight(entry.weightKg, preferences.useImperialUnits),
                style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFamily),
              )
              Text(
                "${entry.dateIso} • BMI ${String.format(Locale.US, "%.1f", entry.bmi)} • ${entry.source}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      }
    }
  }

  if (showAddDialog) {
    AlertDialog(
      onDismissRequest = { showAddDialog = false },
      title = { Text("Log Weight") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          OutlinedTextField(
            value = weightText,
            onValueChange = { weightText = it },
            label = { Text("Weight (kg)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.testTag("weight_input"),
          )
          OutlinedTextField(
            value = notesText,
            onValueChange = { notesText = it },
            label = { Text("Notes (optional)") },
            singleLine = true,
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            val kg = (weightText.toDoubleOrNull() ?: preferences.weightKg).coerceIn(25.0, 250.0)
            onAddWeight(kg, notesText)
            notesText = ""
            showAddDialog = false
          },
          modifier = Modifier.testTag("weight_confirm_button"),
        ) {
          Text(stringResource(R.string.save_label))
        }
      },
      dismissButton = {
        TextButton(onClick = { showAddDialog = false }) {
          Text(stringResource(R.string.cancel_label))
        }
      },
    )
  }
}
