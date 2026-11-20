package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Nfc
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material.icons.outlined.SyncAlt
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.health.HealthConnectAvailability
import com.example.data.health.StepSensorStatus
import com.example.data.local.UserProfilePreferences
import com.example.data.repository.WearOsBridgeStatus
import com.example.ui.components.Tactile3DCard
import com.example.ui.components.VitalTopBar
import com.example.ui.theme.AccentPalette
import com.example.ui.theme.JetBrainsMonoFamily
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Platform Integrations, Security, Theme Accents, Importer & Sensor Diagnostics Lab (#91-#100).
 */
@Composable
fun DiagnosticsLabScreen(
  preferences: UserProfilePreferences,
  sensorStatus: StepSensorStatus,
  healthConnectAvailability: HealthConnectAvailability,
  wearOsStatus: WearOsBridgeStatus,
  onAccentPaletteChange: (AccentPalette) -> Unit,
  onBiometricLockChange: (Boolean) -> Unit,
  onAutoBackupChange: (Boolean) -> Unit,
  onRunAutoBackupNow: () -> Unit,
  onNfcModeChange: (String) -> Unit,
  onSimulateNfcTap: () -> Unit,
  onWriteHealthConnect: () -> Unit,
  onImportCsvOrJsonUri: (Uri) -> Unit,
  onBack: () -> Unit,
) {
  BackHandler(onBack = onBack)

  val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
    if (uri != null) onImportCsvOrJsonUri(uri)
  }

  Scaffold(topBar = { VitalTopBar(title = "Platform & Diagnostics Lab", onBack = onBack) }) { innerPadding ->
    Column(
      modifier =
        Modifier.fillMaxSize()
          .padding(innerPadding)
          .verticalScroll(rememberScrollState())
          .padding(16.dp)
          .widthIn(max = 640.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      // #98 Custom Dynamic Theme Accent Palettes
      Tactile3DCard(enabled3D = preferences.threeDVisualsEnabled) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Outlined.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text("Accent Theme Palette & Material You", style = MaterialTheme.typography.titleMedium)
          }
          Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            AccentPalette.entries.forEach { pal ->
              FilterChip(
                selected = preferences.accentPalette == pal,
                onClick = { onAccentPaletteChange(pal) },
                label = { Text(pal.label) },
                modifier = Modifier.testTag("diag_palette_${pal.name.lowercase()}"),
              )
            }
          }
        }
      }

      // #97 Biometric App Lock & #96 Automated Local Snapshots
      Tactile3DCard(enabled3D = preferences.threeDVisualsEnabled) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
              Icon(Icons.Outlined.Fingerprint, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
              Spacer(modifier = Modifier.width(8.dp))
              Column {
                Text("Biometric & Credential App Lock", style = MaterialTheme.typography.titleSmall)
                Text("Guards app resume using Android BiometricPrompt / Keyguard", style = MaterialTheme.typography.labelSmall)
              }
            }
            Switch(
              checked = preferences.biometricLockEnabled,
              onCheckedChange = onBiometricLockChange,
              modifier = Modifier.testTag("diag_biometric_switch"),
            )
          }

          Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
              Icon(Icons.Outlined.Backup, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
              Spacer(modifier = Modifier.width(8.dp))
              Column {
                Text("Nightly Auto-Backup Snapshots", style = MaterialTheme.typography.titleSmall)
                val lastStr = if (preferences.lastAutoBackupMillis > 0L) SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(preferences.lastAutoBackupMillis)) else "Not run yet"
                Text("WorkManager 24h cycle • Last: $lastStr", style = MaterialTheme.typography.labelSmall)
              }
            }
            Switch(
              checked = preferences.autoBackupEnabled,
              onCheckedChange = onAutoBackupChange,
              modifier = Modifier.testTag("diag_autobackup_switch"),
            )
          }
          OutlinedButton(onClick = onRunAutoBackupNow, modifier = Modifier.fillMaxWidth().testTag("diag_run_backup_now")) {
            Text("Save Backup Snapshot Now")
          }
        }
      }

      // #95 Bidirectional Health Connect & #99 Selective CSV/JSON Importer
      Tactile3DCard(enabled3D = preferences.threeDVisualsEnabled) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Outlined.SyncAlt, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text("Bidirectional Health Connect & Data Importer", style = MaterialTheme.typography.titleMedium)
          }
          Text("Health Connect Status: ${healthConnectAvailability.name}", style = MaterialTheme.typography.bodySmall)
          Button(onClick = onWriteHealthConnect, modifier = Modifier.fillMaxWidth().testTag("diag_hc_write_btn")) {
            Text("Write Today's Steps, Water & Weight to Health Connect")
          }
          OutlinedButton(
            onClick = { importLauncher.launch(arrayOf("text/csv", "text/comma-separated-values", "application/json", "*/*")) },
            modifier = Modifier.fillMaxWidth().testTag("diag_csv_import_btn"),
          ) {
            Icon(Icons.Outlined.UploadFile, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Import Steps / Tasks from CSV or JSON")
          }
        }
      }

      // #94 NFC Quick Tag + #91/#92/#93 Widget, Quick Tile, Wear OS & #100 Sensor Diagnostics
      Tactile3DCard(enabled3D = preferences.threeDVisualsEnabled) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Outlined.Nfc, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text("NFC Wellness Tag Trigger (vitaltrack://trigger)", style = MaterialTheme.typography.titleMedium)
          }
          Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("WATER_250" to "Log +250ml Water", "START_WALK" to "Start GPS Walk", "LOG_MINDFUL" to "Mindful Ping").forEach { (k, label) ->
              FilterChip(selected = preferences.nfcQuickActionMode == k, onClick = { onNfcModeChange(k) }, label = { Text(label) })
            }
          }
          OutlinedButton(onClick = onSimulateNfcTap, modifier = Modifier.fillMaxWidth().testTag("diag_nfc_test_btn")) {
            Text("Test Trigger Configured NFC Action Now")
          }
        }
      }

      Tactile3DCard(enabled3D = preferences.threeDVisualsEnabled) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Outlined.Widgets, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text("Widgets, Quick Tile & Wear OS Bridge", style = MaterialTheme.typography.titleMedium)
          }
          Text("• Glance Home Widget: Displays today's steps, water, +250ml quick action & top pending task.", style = MaterialTheme.typography.bodySmall)
          Text("• Android Quick Settings Tile: Edit Quick Settings -> drag 'VITALTRACK Water' for 1-tap +250ml logging.", style = MaterialTheme.typography.bodySmall)
          Text("• Wear OS Data Layer: ${wearOsStatus.dataLayerPath} (${if (wearOsStatus.companionAppDetected) "Companion Linked" else "Ready for Companion"})", style = MaterialTheme.typography.bodySmall)
        }
      }

      Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(14.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Icons.Outlined.Sensors, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text("Sensor & Scheduler Diagnostics", style = MaterialTheme.typography.titleSmall)
          }
          Text(
            "Step Sensor: ${sensorStatus.sensorType.name} • Active: ${sensorStatus.isListening} • ${sensorStatus.statusMessage}",
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFamily),
          )
        }
      }
      Spacer(modifier = Modifier.height(20.dp))
    }
  }
}
