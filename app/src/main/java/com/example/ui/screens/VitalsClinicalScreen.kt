package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.health.WellnessAdvancedCalculators
import com.example.data.health.WellnessCalculators
import com.example.data.local.UserProfilePreferences
import com.example.data.local.VitalsRecordEntity
import com.example.data.local.WeightEntryEntity
import com.example.ui.components.MedicalDisclaimerBanner
import com.example.ui.components.Tactile3DCard
import com.example.ui.components.VitalTopBar
import com.example.ui.theme.JetBrainsMonoFamily
import kotlinx.coroutines.delay
import java.util.Locale

/**
 * Clinical Vitals, Labs, Medications & Physician Summary Export (#81-#90).
 */
@Composable
fun VitalsClinicalScreen(
  preferences: UserProfilePreferences,
  vitalsRecords: List<VitalsRecordEntity>,
  weightEntries: List<WeightEntryEntity>,
  onAddVitalsRecord: (
    type: String,
    title: String,
    primaryValue: Double,
    secondaryValue: Double,
    tertiaryValue: Double,
    unit: String,
    notes: String,
    refillRemaining: Int,
  ) -> Unit,
  onDecrementRefill: (String) -> Unit,
  onDeleteVitalsRecord: (String) -> Unit,
  onSaveBodyMeasurements: (chestCm: Double, waistCm: Double, hipCm: Double) -> Unit,
  onExportPhysicianPdf: () -> Unit,
  onBack: () -> Unit,
) {
  BackHandler(onBack = onBack)

  var sysDraft by remember { mutableStateOf("118") }
  var diaDraft by remember { mutableStateOf("76") }
  var glucoseDraft by remember { mutableStateOf("94") }
  var glucoseTiming by remember { mutableStateOf("Fasting") }

  var waistDraft by remember { mutableStateOf(preferences.bodyWaistCm.toInt().toString()) }
  var chestDraft by remember { mutableStateOf(preferences.bodyChestCm.toInt().toString()) }
  var hipDraft by remember { mutableStateOf(preferences.bodyHipCm.toInt().toString()) }

  var medNameDraft by remember { mutableStateOf("") }
  var medRefillDraft by remember { mutableStateOf("30") }
  var triggerTag by remember { mutableStateOf("Headache") }
  var labNameDraft by remember { mutableStateOf("Vitamin D") }
  var labValDraft by remember { mutableStateOf("38.0") }

  var plankRunning by remember { mutableStateOf(false) }
  var plankSec by remember { mutableIntStateOf(0) }
  LaunchedEffect(plankRunning) {
    while (plankRunning) {
      delay(1000L)
      plankSec++
    }
  }

  val sysInt = sysDraft.toIntOrNull() ?: 118
  val diaInt = diaDraft.toIntOrNull() ?: 76
  val bpCategory = WellnessAdvancedCalculators.classifyBloodPressure(sysInt, diaInt)

  val waistCm = waistDraft.toDoubleOrNull() ?: preferences.bodyWaistCm
  val (whtRatio, whtLabel) = WellnessAdvancedCalculators.waistToHeightRatio(waistCm, preferences.heightCm)
  val weightEma = remember(weightEntries) {
    WellnessAdvancedCalculators.calculateWeightEmaSeries(weightEntries.reversed())
  }

  Scaffold(topBar = { VitalTopBar(title = "Clinical Vitals & Labs", onBack = onBack) }) { innerPadding ->
    Column(
      modifier =
        Modifier.fillMaxSize()
          .padding(innerPadding)
          .verticalScroll(rememberScrollState())
          .padding(16.dp)
          .widthIn(max = 640.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      MedicalDisclaimerBanner()

      // #88 Physician 1-Page Export CTA
      Button(
        onClick = onExportPhysicianPdf,
        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("vitals_export_physician_pdf"),
      ) {
        Icon(Icons.Outlined.Description, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Export Physician Summary (1-Page PDF)")
      }

      // #81 Blood Pressure + #82 Glucose
      Tactile3DCard(enabled3D = preferences.threeDVisualsEnabled) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Outlined.MonitorHeart, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text("Blood Pressure & Glucose", style = MaterialTheme.typography.titleMedium)
          }
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = sysDraft, onValueChange = { sysDraft = it }, label = { Text("Systolic") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f).testTag("vitals_bp_sys"))
            OutlinedTextField(value = diaDraft, onValueChange = { diaDraft = it }, label = { Text("Diastolic") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f).testTag("vitals_bp_dia"))
          }
          Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(8.dp)) {
            Text("AHA Classification: $bpCategory", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(8.dp))
          }
          Button(
            onClick = { onAddVitalsRecord("BP", "Blood Pressure", sysInt.toDouble(), diaInt.toDouble(), 0.0, "mmHg", bpCategory, -1) },
            modifier = Modifier.fillMaxWidth().testTag("vitals_save_bp_btn"),
          ) { Text("Log Blood Pressure Reading") }

          Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(value = glucoseDraft, onValueChange = { glucoseDraft = it }, label = { Text("Glucose (mg/dL)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
            FilterChip(selected = glucoseTiming == "Fasting", onClick = { glucoseTiming = "Fasting" }, label = { Text("Fasting") })
            FilterChip(selected = glucoseTiming == "Post-Meal", onClick = { glucoseTiming = "Post-Meal" }, label = { Text("Post-Meal") })
          }
          OutlinedButton(
            onClick = { onAddVitalsRecord("GLUCOSE", "Blood Glucose ($glucoseTiming)", glucoseDraft.toDoubleOrNull() ?: 95.0, 0.0, 0.0, "mg/dL", glucoseTiming, -1) },
            modifier = Modifier.fillMaxWidth(),
          ) { Text("Log Blood Glucose") }
        }
      }

      // #83 Waist-to-Height & Circumferences + #89 7-Day EMA Weight Smoothing
      Tactile3DCard(enabled3D = preferences.threeDVisualsEnabled) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Outlined.Straighten, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text("Waist-to-Height Ratio & EMA Weight Trend", style = MaterialTheme.typography.titleMedium)
          }
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = chestDraft, onValueChange = { chestDraft = it }, label = { Text("Chest cm") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
            OutlinedTextField(value = waistDraft, onValueChange = { waistDraft = it }, label = { Text("Waist cm") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
            OutlinedTextField(value = hipDraft, onValueChange = { hipDraft = it }, label = { Text("Hip cm") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
          }
          Text(
            "Waist-to-Height: ${String.format(Locale.US, "%.2f", whtRatio)} ($whtLabel)",
            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = JetBrainsMonoFamily),
          )
          OutlinedButton(onClick = {
            val ch = chestDraft.toDoubleOrNull() ?: preferences.bodyChestCm
            val wa = waistDraft.toDoubleOrNull() ?: preferences.bodyWaistCm
            val hi = hipDraft.toDoubleOrNull() ?: preferences.bodyHipCm
            onSaveBodyMeasurements(ch, wa, hi)
            onAddVitalsRecord("MEASUREMENT", "Body Circumference", wa, ch, hi, "cm", whtLabel, -1)
          }, modifier = Modifier.fillMaxWidth()) { Text("Save Measurements") }

          if (weightEma.isNotEmpty()) {
            val latestEma = weightEma.last()
            Text(
              "7-Day Smoothed Weight (EMA): ${String.format(Locale.US, "%.1f", latestEma.second)} kg (${latestEma.first})",
              style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFamily),
              color = MaterialTheme.colorScheme.primary,
            )
          }
        }
      }

      // #84 Cycle/Energy, #85 Medications & Refill Countdown, #86 Allergy/Symptom, #87 Labs, #90 Benchmark
      Tactile3DCard(enabled3D = preferences.threeDVisualsEnabled) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Outlined.Medication, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text("Medications, Cycle & Symptom Triggers", style = MaterialTheme.typography.titleMedium)
          }
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = medNameDraft, onValueChange = { medNameDraft = it }, label = { Text("Medication / Supplement") }, modifier = Modifier.weight(1.4f))
            OutlinedTextField(value = medRefillDraft, onValueChange = { medRefillDraft = it }, label = { Text("Doses Left") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(0.8f))
          }
          Button(enabled = medNameDraft.isNotBlank(), onClick = {
            onAddVitalsRecord("MEDICATION", medNameDraft.trim(), 1.0, 0.0, 0.0, "dose", "Scheduled daily", medRefillDraft.toIntOrNull() ?: 30)
            medNameDraft = ""
          }, modifier = Modifier.fillMaxWidth()) { Text("Add Medication & Refill Tracker") }

          Text("Cycle / Recovery Phase & Symptom Quick-Log", style = MaterialTheme.typography.labelLarge)
          Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("Follicular", "Ovulatory", "Luteal", "Menstrual", "Baseline").forEach { phase ->
              FilterChip(selected = false, onClick = { onAddVitalsRecord("CYCLE", "Cycle Phase: $phase", 1.0, 0.0, 0.0, "phase", phase, -1) }, label = { Text(phase) })
            }
          }
          Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("Headache", "Seasonal Pollen", "Screen Eye Strain", "Muscle Tightness").forEach { tag ->
              FilterChip(selected = triggerTag == tag, onClick = { triggerTag = tag }, label = { Text(tag) })
            }
          }
          OutlinedButton(onClick = { onAddVitalsRecord("SYMPTOM", triggerTag, 1.0, 0.0, 0.0, "level", "Logged trigger", -1) }, modifier = Modifier.fillMaxWidth()) {
            Text("Log Symptom Trigger ($triggerTag)")
          }
        }
      }

      Tactile3DCard(enabled3D = preferences.threeDVisualsEnabled) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Outlined.Science, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text("Lab Vault & Core Benchmark", style = MaterialTheme.typography.titleMedium)
          }
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = labNameDraft, onValueChange = { labNameDraft = it }, label = { Text("Marker (e.g. HbA1c)") }, modifier = Modifier.weight(1f))
            OutlinedTextField(value = labValDraft, onValueChange = { labValDraft = it }, label = { Text("Value") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(0.7f))
          }
          OutlinedButton(enabled = labNameDraft.isNotBlank(), onClick = {
            onAddVitalsRecord("LAB", labNameDraft.trim(), labValDraft.toDoubleOrNull() ?: 0.0, 0.0, 0.0, "units", "Annual checkup", -1)
          }, modifier = Modifier.fillMaxWidth()) { Text("Save Lab Biomarker") }

          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Outlined.Timer, contentDescription = null)
              Spacer(modifier = Modifier.width(6.dp))
              Text("Plank Benchmark: ${WellnessCalculators.formatDurationMillis(plankSec * 1000L)}", style = MaterialTheme.typography.titleSmall.copy(fontFamily = JetBrainsMonoFamily))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              FilledButtonPair(
                running = plankRunning,
                onToggle = { plankRunning = !plankRunning },
                onSave = {
                  if (plankSec > 0) {
                    onAddVitalsRecord("BENCHMARK", "Core Plank Hold", plankSec.toDouble(), 0.0, 0.0, "sec", "Personal benchmark", -1)
                    plankRunning = false
                    plankSec = 0
                  }
                },
              )
            }
          }
        }
      }

      if (vitalsRecords.isNotEmpty()) {
        Text("Clinical & Benchmark History (${vitalsRecords.size})", style = MaterialTheme.typography.titleMedium)
        vitalsRecords.forEach { rec ->
          Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
              Column(modifier = Modifier.weight(1f)) {
                Text("${rec.title} (${rec.dateIso})", style = MaterialTheme.typography.titleSmall)
                val valStr = if (rec.secondaryValue > 0) "${rec.primaryValue.toInt()}/${rec.secondaryValue.toInt()} ${rec.unit}" else "${rec.primaryValue} ${rec.unit}"
                val refillStr = if (rec.refillRemaining >= 0) " • Refill: ${rec.refillRemaining} left" else ""
                Text("$valStr • ${rec.notes}$refillStr", style = MaterialTheme.typography.bodySmall)
              }
              if (rec.type == "MEDICATION" && rec.refillRemaining > 0) {
                OutlinedButton(onClick = { onDecrementRefill(rec.id) }) { Text("-1 Dose") }
              }
              IconButton(onClick = { onDeleteVitalsRecord(rec.id) }) {
                Icon(Icons.Outlined.DeleteOutline, contentDescription = "Delete vitals entry")
              }
            }
          }
        }
      }
      Spacer(modifier = Modifier.height(20.dp))
    }
  }
}

@Composable
private fun FilledButtonPair(running: Boolean, onToggle: () -> Unit, onSave: () -> Unit) {
  Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
    OutlinedButton(onClick = onToggle) { Text(if (running) "Pause" else "Start") }
    Button(onClick = onSave) { Text("Save") }
  }
}
