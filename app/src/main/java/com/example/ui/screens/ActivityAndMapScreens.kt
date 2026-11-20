package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DirectionsBike
import androidx.compose.material.icons.automirrored.outlined.DirectionsRun
import androidx.compose.material.icons.automirrored.outlined.DirectionsWalk
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Sos
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.outlined.Terrain
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.health.BiofeedbackAudioEngine
import com.example.data.health.WellnessAdvancedCalculators
import com.example.data.health.WellnessCalculators
import com.example.data.local.GearItemEntity
import com.example.data.local.GpsActivitySessionEntity
import com.example.data.local.GpsPoint
import com.example.data.local.UserProfilePreferences
import com.example.data.local.WorkoutSessionEntity
import com.example.service.ActiveTrackingState
import com.example.service.TrackingRunState
import com.example.ui.components.OsmInteractiveMap
import com.example.ui.components.VitalTopBar
import com.example.ui.components.downloadVisibleMapRegionTiles
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.JetBrainsMonoFamily
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import org.osmdroid.views.MapView
import java.util.Locale

private val routeJson = Json { ignoreUnknownKeys = true }

private fun decodePoints(raw: String): List<GpsPoint> =
  runCatching { routeJson.decodeFromString<List<GpsPoint>>(raw) }.getOrDefault(emptyList())

// 7. Activity History Screen (Bottom Tab 2)
@Composable
fun ActivityHistoryScreen(
  preferences: UserProfilePreferences,
  gpsSessions: List<GpsActivitySessionEntity>,
  workoutSessions: List<WorkoutSessionEntity>,
  onOpenExerciseLibrary: () -> Unit,
  onOpenLiveTracking: () -> Unit,
  onOpenStepDetails: () -> Unit,
  onOpenSessionDetails: (String) -> Unit,
) {
  var filter by remember { mutableStateOf("ALL") } // ALL, GPS, WORKOUT

  LazyColumn(
    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp).widthIn(max = 640.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      Spacer(modifier = Modifier.height(8.dp))
      Text("Activity & Workouts", style = MaterialTheme.typography.headlineMedium)
      Spacer(modifier = Modifier.height(10.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Button(
          onClick = onOpenExerciseLibrary,
          modifier = Modifier.weight(1f).testTag("activity_open_exercises"),
        ) {
          Icon(Icons.Outlined.FitnessCenter, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Workouts")
        }
        FilledTonalButton(
          onClick = onOpenLiveTracking,
          modifier = Modifier.weight(1f).testTag("activity_open_gps"),
        ) {
          Icon(Icons.Outlined.Route, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Live GPS")
        }
      }
      Spacer(modifier = Modifier.height(8.dp))
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("ALL" to "All", "GPS" to "GPS Routes", "WORKOUT" to "Workouts").forEach { (k, label) ->
          FilterChip(selected = filter == k, onClick = { filter = k }, label = { Text(label) })
        }
        Spacer(modifier = Modifier.weight(1f))
        OutlinedButton(onClick = onOpenStepDetails) { Text(stringResource(R.string.steps_label)) }
      }
    }

    if (filter == "ALL" || filter == "GPS") {
      items(gpsSessions, key = { "gps_${it.id}" }) { session ->
        Card(
          modifier =
            Modifier.fillMaxWidth()
              .clip(MaterialTheme.shapes.medium)
              .clickable { onOpenSessionDetails(session.id) },
          shape = MaterialTheme.shapes.medium,
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
          Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector =
                when (session.activityType) {
                  "RUNNING" -> Icons.AutoMirrored.Outlined.DirectionsRun
                  "CYCLING" -> Icons.AutoMirrored.Outlined.DirectionsBike
                  else -> Icons.AutoMirrored.Outlined.DirectionsWalk
                },
              contentDescription = session.activityType,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(30.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(session.title, style = MaterialTheme.typography.titleMedium)
              Text(
                "${session.dateIso} • ${WellnessCalculators.formatDistance(session.distanceMeters, preferences.useImperialUnits)} • ${WellnessCalculators.formatDurationMillis(session.activeDurationMillis)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Text(
                "Avg ${WellnessCalculators.formatSpeed(session.avgSpeedMps, preferences.useImperialUnits)} • ${session.caloriesKcal.toInt()} kcal",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
              )
            }
          }
        }
      }
    }

    if (filter == "ALL" || filter == "WORKOUT") {
      items(workoutSessions, key = { "wk_${it.id}" }) { wk ->
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = MaterialTheme.shapes.medium,
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
          Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
              Icons.Outlined.FitnessCenter,
              contentDescription = wk.category,
              tint = GoldAccent,
              modifier = Modifier.size(28.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(wk.title, style = MaterialTheme.typography.titleMedium)
              Text(
                "${wk.dateIso} • ${wk.durationSeconds / 60} min • ${wk.completedSets} sets • ${wk.caloriesKcal.toInt()} kcal",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      }
    }

    if (gpsSessions.isEmpty() && workoutSessions.isEmpty()) {
      item {
        Card(
          shape = MaterialTheme.shapes.large,
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
          ) {
            Image(
              painter = painterResource(id = R.drawable.img_empty_timeline),
              contentDescription = "No activities recorded yet",
              modifier = Modifier.fillMaxWidth().height(160.dp).clip(MaterialTheme.shapes.medium),
              contentScale = ContentScale.Crop,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text("No activities recorded yet", style = MaterialTheme.typography.titleMedium)
            Text(
              "Start a GPS walk/run/ride or choose a guided workout from the Exercise Library.",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    }

    item { Spacer(modifier = Modifier.height(24.dp)) }
  }
}

// 8. Live GPS Tracking Screen
@Composable
fun LiveTrackingScreen(
  initialMode: String,
  preferences: UserProfilePreferences,
  trackingState: ActiveTrackingState,
  isOnline: Boolean,
  onStart: (String) -> Unit,
  onPause: () -> Unit,
  onResume: () -> Unit,
  onStop: () -> Unit,
  onBack: () -> Unit,
) {
  BackHandler(onBack = onBack)

  val context = LocalContext.current
  var selectedMode by remember { mutableStateOf(initialMode) }
  var hasLocationPermission by remember {
    mutableStateOf(
      ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED
    )
  }

  val permLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { res ->
      hasLocationPermission =
        (res[Manifest.permission.ACCESS_FINE_LOCATION] == true) ||
          (res[Manifest.permission.ACCESS_COARSE_LOCATION] == true)
    }

  var splitChimeEnabled by remember { mutableStateOf(true) }
  var ghostPaceMps by remember { mutableDoubleStateOf(2.6) } // ~6:24 min/km target
  var lastAnnouncedKm by remember { mutableIntStateOf(0) }

  LaunchedEffect(trackingState.distanceMeters, splitChimeEnabled) {
    val kmCount = (trackingState.distanceMeters / 1000.0).toInt()
    if (splitChimeEnabled && kmCount > lastAnnouncedKm && kmCount > 0) {
      lastAnnouncedKm = kmCount
      BiofeedbackAudioEngine.playShortTone()
      BiofeedbackAudioEngine.triggerHapticPulse(context, 140L, 170)
    }
  }

  Scaffold(
    topBar = {
      VitalTopBar(
        title = "Live GPS Tracking",
        onBack = onBack,
        actions = {
          // #47 Play-Compliant Safety SOS Beacon via ACTION_SENDTO / ACTION_SEND
          IconButton(
            onClick = {
              val lastPt = trackingState.points.lastOrNull()
              val coords = if (lastPt != null) String.format(Locale.US, "https://maps.google.com/?q=%.5f,%.5f", lastPt.latitude, lastPt.longitude) else "GPS standby"
              val msg = "VITALTRACK Safety Check-In from ${preferences.displayName}: Active ${selectedMode}, Distance ${WellnessCalculators.formatDistance(trackingState.distanceMeters, preferences.useImperialUnits)}. Location: $coords"
              val intent = if (preferences.emergencyContactPhone.isNotBlank()) {
                Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${preferences.emergencyContactPhone}")).apply {
                  putExtra("sms_body", msg)
                }
              } else {
                Intent(Intent.ACTION_SEND).apply {
                  type = "text/plain"
                  putExtra(Intent.EXTRA_TEXT, msg)
                }
              }
              runCatching { context.startActivity(Intent.createChooser(intent, "Send Safety Check-In")) }
            },
            modifier = Modifier.testTag("gps_sos_beacon_btn"),
          ) {
            Icon(Icons.Outlined.Sos, contentDescription = "Share emergency location beacon", tint = MaterialTheme.colorScheme.error)
          }
        },
      )
    }
  ) { innerPadding ->
    Column(
      modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp).widthIn(max = 640.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      if (!isOnline) {
        Surface(
          color = MaterialTheme.colorScheme.tertiaryContainer,
          shape = MaterialTheme.shapes.small,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text(
            text = stringResource(R.string.map_offline_notice),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier.padding(10.dp),
          )
        }
      }

      if (!hasLocationPermission) {
        Card(
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
          shape = MaterialTheme.shapes.medium,
        ) {
          Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
              "Location Permission Required",
              style = MaterialTheme.typography.titleMedium,
              color = MaterialTheme.colorScheme.onErrorContainer,
            )
            Text(
              "VITALTRACK needs foreground location access to record your GPS route only while an activity session is active.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onErrorContainer,
            )
            Button(
              onClick = {
                permLauncher.launch(
                  arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                  )
                )
              },
              modifier = Modifier.testTag("live_gps_grant_perm"),
            ) {
              Text("Grant Location Permission")
            }
          }
        }
      }

      // Activity Mode Selector (locked while active)
      if (trackingState.runState == TrackingRunState.IDLE) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          listOf("WALKING" to "Walking", "RUNNING" to "Running", "CYCLING" to "Cycling").forEach {
            (mode, label) ->
            FilterChip(
              selected = selectedMode == mode,
              onClick = { selectedMode = mode },
              label = { Text(label) },
            )
          }
        }
      }

      // Real OpenStreetMap View
      Box(
        modifier =
          Modifier.fillMaxWidth()
            .weight(1f)
            .clip(MaterialTheme.shapes.large),
      ) {
        OsmInteractiveMap(
          points = trackingState.points,
          isOnline = isOnline,
          modifier = Modifier.fillMaxSize(),
        )
      }

      // #42 Audio Split Chimes & #44 Ghost Pacer vs Live Speed
      Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("km Split Haptic/Tone Chimes", style = MaterialTheme.typography.labelLarge)
            Switch(checked = splitChimeEnabled, onCheckedChange = { splitChimeEnabled = it })
          }
          val delta = trackingState.avgSpeedMps - ghostPaceMps
          val ghostMsg = if (trackingState.avgSpeedMps == 0.0) "Ghost Target: ${WellnessCalculators.formatSpeed(ghostPaceMps, preferences.useImperialUnits)}" else if (delta >= 0) "Ghost Pacer: Ahead (+${String.format(Locale.US, "%.1f", delta * 3.6)} km/h)" else "Ghost Pacer: Trail by ${String.format(Locale.US, "%.1f", -delta * 3.6)} km/h"
          Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(ghostMsg, style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily))
            FilterChip(selected = ghostPaceMps == 1.6, onClick = { ghostPaceMps = 1.6 }, label = { Text("Walk 5.8") })
            FilterChip(selected = ghostPaceMps == 2.6, onClick = { ghostPaceMps = 2.6 }, label = { Text("Jog 9.4") })
            FilterChip(selected = ghostPaceMps == 3.4, onClick = { ghostPaceMps = 3.4 }, label = { Text("Tempo 12.2") })
          }
        }
      }

      // Live Telemetry Card
      Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            TelemetryStat(
              label = "Duration",
              value = WellnessCalculators.formatDurationMillis(trackingState.activeDurationMillis),
            )
            TelemetryStat(
              label = "Distance",
              value =
                WellnessCalculators.formatDistance(
                  trackingState.distanceMeters,
                  preferences.useImperialUnits,
                ),
            )
            TelemetryStat(
              label = "Avg Speed",
              value =
                WellnessCalculators.formatSpeed(
                  trackingState.avgSpeedMps,
                  preferences.useImperialUnits,
                ),
            )
            TelemetryStat(
              label = "Est. kcal",
              value = "${trackingState.caloriesKcal.toInt()}",
            )
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            when (trackingState.runState) {
              TrackingRunState.IDLE -> {
                Button(
                  onClick = { onStart(selectedMode) },
                  enabled = hasLocationPermission,
                  modifier = Modifier.fillMaxWidth().height(52.dp).testTag("gps_start_button"),
                ) {
                  Icon(Icons.Outlined.PlayArrow, contentDescription = null)
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("Start ${selectedMode.lowercase().replaceFirstChar { it.uppercase() }}")
                }
              }
              TrackingRunState.ACTIVE -> {
                OutlinedButton(
                  onClick = onPause,
                  modifier = Modifier.weight(1f).height(52.dp).testTag("gps_pause_button"),
                ) {
                  Icon(Icons.Outlined.Pause, contentDescription = null)
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("Pause")
                }
                Button(
                  onClick = onStop,
                  colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                  modifier = Modifier.weight(1f).height(52.dp).testTag("gps_stop_button"),
                ) {
                  Icon(Icons.Outlined.Stop, contentDescription = null)
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("Stop & Save")
                }
              }
              TrackingRunState.PAUSED -> {
                Button(
                  onClick = onResume,
                  modifier = Modifier.weight(1f).height(52.dp).testTag("gps_resume_button"),
                ) {
                  Icon(Icons.Outlined.PlayArrow, contentDescription = null)
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("Resume")
                }
                Button(
                  onClick = onStop,
                  colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                  modifier = Modifier.weight(1f).height(52.dp).testTag("gps_stop_button"),
                ) {
                  Icon(Icons.Outlined.Stop, contentDescription = null)
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("Stop & Save")
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun TelemetryStat(label: String, value: String) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Text(
      text = value,
      style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFamily),
    )
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

// 9. Map Timeline Screen (Bottom Tab 3)
@Composable
fun MapTimelineScreen(
  preferences: UserProfilePreferences,
  gpsSessions: List<GpsActivitySessionEntity>,
  isOnline: Boolean,
  onStartLiveTracking: () -> Unit,
  onOpenSessionDetails: (String) -> Unit,
  onMapRegionCached: (String) -> Unit,
  onShowMessage: (String) -> Unit,
  gearItems: List<GearItemEntity> = emptyList(),
  onAddGearItem: (name: String, brand: String, retireKm: Double) -> Unit = { _, _, _ -> },
  onAddGearKm: (String, Double) -> Unit = { _, _ -> },
) {
  var selectedSession by remember(gpsSessions) { mutableStateOf(gpsSessions.firstOrNull()) }
  var loopTargetKm by remember { mutableIntStateOf(0) }
  var showHeatmap by remember { mutableStateOf(false) }
  var gearNameDraft by remember { mutableStateOf("") }
  val selectedPoints =
    remember(selectedSession, loopTargetKm) {
      if (loopTargetKm > 0) {
        val basePt = selectedSession?.let { decodePoints(it.routePolylineJson).firstOrNull() }
        WellnessAdvancedCalculators.generateLoopRoutePoints(
          basePt?.latitude ?: 23.8103,
          basePt?.longitude ?: 90.4125,
          loopTargetKm.toDouble(),
        )
      } else {
        selectedSession?.let { decodePoints(it.routePolylineJson) } ?: emptyList()
      }
    }
  var mapViewRef by remember { mutableStateOf<MapView?>(null) }
  var isCaching by remember { mutableStateOf(false) }

  val groupedByDate = remember(gpsSessions) { gpsSessions.groupBy { it.dateIso } }

  Box(modifier = Modifier.fillMaxSize()) {
    Column(
      modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 10.dp).widthIn(max = 640.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column {
          Text("Personal Map Timeline", style = MaterialTheme.typography.headlineSmall)
          if (preferences.lastCachedMapRegionLabel.isNotBlank()) {
            Text(
              "Offline cache: ${preferences.lastCachedMapRegionLabel}",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.primary,
            )
          }
        }
        OutlinedButton(
          enabled = isOnline && !isCaching && mapViewRef != null,
          onClick = {
            val mv = mapViewRef ?: return@OutlinedButton
            isCaching = true
            downloadVisibleMapRegionTiles(
              mapView = mv,
              onCompleted = { _, errors ->
                isCaching = false
                if (errors == 0) {
                  val center = mv.mapCenter
                  onMapRegionCached(
                    String.format(
                      java.util.Locale.US,
                      "%.3f, %.3f (z%d)",
                      center.latitude,
                      center.longitude,
                      mv.zoomLevelDouble.toInt(),
                    )
                  )
                } else {
                  onShowMessage("Map tile caching completed with $errors tile errors.")
                }
              },
              onError = { msg ->
                isCaching = false
                onShowMessage(msg)
              },
            )
          },
          modifier = Modifier.testTag("map_cache_region_button"),
        ) {
          Icon(Icons.Outlined.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(if (isCaching) "Caching..." else "Cache Area")
        }
      }

      if (!isOnline) {
        Surface(
          color = MaterialTheme.colorScheme.tertiaryContainer,
          shape = MaterialTheme.shapes.small,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text(
            text = stringResource(R.string.map_offline_notice),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier.padding(8.dp),
          )
        }
      }

      Box(
        modifier =
          Modifier.fillMaxWidth()
            .height(250.dp)
            .clip(MaterialTheme.shapes.large),
      ) {
        OsmInteractiveMap(
          points = selectedPoints,
          isOnline = isOnline,
          modifier = Modifier.fillMaxSize(),
          onMapViewReady = { mapViewRef = it },
        )
      }

      LazyColumn(
        modifier = Modifier.fillMaxWidth().weight(1f),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        if (groupedByDate.isEmpty()) {
          item {
            Card(
              shape = MaterialTheme.shapes.medium,
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
              Column(
                modifier = Modifier.fillMaxWidth().padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
              ) {
                Image(
                  painter = painterResource(id = R.drawable.img_empty_timeline),
                  contentDescription = "Empty map timeline",
                  modifier = Modifier.fillMaxWidth().height(130.dp).clip(MaterialTheme.shapes.medium),
                  contentScale = ContentScale.Crop,
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text("No GPS routes recorded yet", style = MaterialTheme.typography.titleMedium)
                Text(
                  "Tap the GPS button below to record a live walk, run, or ride with route geometry.",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }
        } else {
          groupedByDate.forEach { (dateIso, list) ->
            item(key = "header_$dateIso") {
              Text(
                text = dateIso,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 4.dp),
              )
            }
            items(list, key = { it.id }) { session ->
              val isSelected = selectedSession?.id == session.id
              Card(
                modifier =
                  Modifier.fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .clickable { selectedSession = session },
                shape = MaterialTheme.shapes.medium,
                colors =
                  CardDefaults.cardColors(
                    containerColor =
                      if (isSelected) MaterialTheme.colorScheme.primaryContainer
                      else MaterialTheme.colorScheme.surface,
                  ),
              ) {
                Row(
                  modifier = Modifier.padding(14.dp),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    Text(session.title, style = MaterialTheme.typography.titleMedium)
                    Text(
                      "${WellnessCalculators.formatDistance(session.distanceMeters, preferences.useImperialUnits)} • ${WellnessCalculators.formatDurationMillis(session.activeDurationMillis)} • ${session.caloriesKcal.toInt()} kcal",
                      style = MaterialTheme.typography.bodySmall,
                    )
                  }
                  OutlinedButton(onClick = { onOpenSessionDetails(session.id) }) {
                    Text("Replay")
                  }
                }
              }
            }
          }
        }
        item {
          // #45 Round-Trip Loop Route Generator, #48 Shoe/Gear Mileage Tracker, #50 Coordinate Heatmap
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              FilterChip(selected = loopTargetKm == 0, onClick = { loopTargetKm = 0 }, label = { Text("Recorded Route") })
              listOf(3, 5, 10).forEach { km ->
                FilterChip(selected = loopTargetKm == km, onClick = { loopTargetKm = km }, label = { Text("${km}km Loop") })
              }
              FilterChip(selected = showHeatmap, onClick = { showHeatmap = !showHeatmap }, label = { Text("Heatmap Density") })
            }
            if (showHeatmap) {
              val allPts = remember(gpsSessions) { gpsSessions.flatMap { decodePoints(it.routePolylineJson) } }
              Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.fillMaxWidth().padding(10.dp)) {
                  Text("Multi-Session GPS Heatmap (${allPts.size} coordinates)", style = MaterialTheme.typography.labelLarge)
                  val heatColor = MaterialTheme.colorScheme.primary
                  Canvas(modifier = Modifier.fillMaxWidth().height(90.dp)) {
                    if (allPts.isNotEmpty()) {
                      val minLat = allPts.minOf { it.latitude }
                      val maxLat = (allPts.maxOf { it.latitude } - minLat).coerceAtLeast(0.0001)
                      val minLon = allPts.minOf { it.longitude }
                      val maxLon = (allPts.maxOf { it.longitude } - minLon).coerceAtLeast(0.0001)
                      allPts.forEach { p ->
                        val x = (((p.longitude - minLon) / maxLon).toFloat()) * size.width * 0.9f + size.width * 0.05f
                        val y = (1f - ((p.latitude - minLat) / maxLat).toFloat()) * size.height * 0.85f + size.height * 0.08f
                        drawCircle(heatColor.copy(alpha = 0.25f), 10.dp.toPx(), Offset(x, y))
                        drawCircle(heatColor, 3.dp.toPx(), Offset(x, y))
                      }
                    }
                  }
                }
              }
            }
            Card(shape = MaterialTheme.shapes.medium, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
              Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Gear & Running Shoe Mileage Tracker", style = MaterialTheme.typography.titleSmall)
                gearItems.forEach { g ->
                  val km = g.distanceMeters / 1000.0
                  val maxKm = g.retireThresholdMeters / 1000.0
                  Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(modifier = Modifier.weight(1f)) {
                      Text("${g.name}: ${String.format(Locale.US, "%.1f", km)} / ${maxKm.toInt()} km", style = MaterialTheme.typography.bodySmall)
                      LinearProgressIndicator(progress = { (km / maxKm.coerceAtLeast(1.0)).toFloat().coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth().height(5.dp))
                    }
                    OutlinedButton(onClick = { onAddGearKm(g.id, 5.0) }) { Text("+5 km") }
                  }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                  OutlinedTextField(value = gearNameDraft, onValueChange = { gearNameDraft = it }, label = { Text("Add shoe / bike...") }, modifier = Modifier.weight(1f), singleLine = true)
                  Button(enabled = gearNameDraft.isNotBlank(), onClick = {
                    onAddGearItem(gearNameDraft.trim(), "Running Gear", 650.0)
                    gearNameDraft = ""
                  }) { Text("Add") }
                }
              }
            }
          }
        }
        item { Spacer(modifier = Modifier.height(72.dp)) }
      }
    }

    FloatingActionButton(
      onClick = onStartLiveTracking,
      modifier =
        Modifier.align(Alignment.BottomEnd)
          .padding(20.dp)
          .testTag("map_start_tracking_fab"),
      containerColor = MaterialTheme.colorScheme.primary,
      contentColor = MaterialTheme.colorScheme.onPrimary,
    ) {
      Icon(Icons.Outlined.Route, contentDescription = stringResource(R.string.start_gps_session))
    }
  }
}

// 10. Activity Details & Route Replay Screen
@Composable
fun ActivityDetailsScreen(
  session: GpsActivitySessionEntity?,
  preferences: UserProfilePreferences,
  isOnline: Boolean,
  onExportGpsGpx: (String) -> Unit = {},
  onDeleteSession: (String) -> Unit,
  onBack: () -> Unit,
) {
  BackHandler(onBack = onBack)

  if (session == null) {
    Scaffold(topBar = { VitalTopBar(title = "Activity Details", onBack = onBack) }) { innerPadding ->
      Box(
        modifier = Modifier.fillMaxSize().padding(innerPadding),
        contentAlignment = Alignment.Center,
      ) {
        Text("Session not found.")
      }
    }
    return
  }

  val points = remember(session) { decodePoints(session.routePolylineJson) }
  var replayIndex by remember(points) { mutableIntStateOf(points.lastIndex.coerceAtLeast(0)) }
  var isReplaying by remember { mutableStateOf(false) }

  LaunchedEffect(isReplaying, points.size) {
    if (isReplaying && points.size > 1) {
      if (replayIndex >= points.lastIndex) replayIndex = 0
      while (isReplaying && replayIndex < points.lastIndex) {
        delay(350L)
        replayIndex++
      }
      isReplaying = false
    }
  }

  Scaffold(
    topBar = {
      VitalTopBar(
        title = session.title,
        onBack = onBack,
        actions = {
          IconButton(
            onClick = {
              onDeleteSession(session.id)
              onBack()
            }
          ) {
            Icon(Icons.Outlined.DeleteOutline, contentDescription = "Delete session")
          }
        },
      )
    }
  ) { innerPadding ->
    Column(
      modifier =
        Modifier.fillMaxSize()
          .padding(innerPadding)
          .verticalScroll(rememberScrollState())
          .padding(16.dp)
          .widthIn(max = 640.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Box(
        modifier =
          Modifier.fillMaxWidth()
            .height(270.dp)
            .clip(MaterialTheme.shapes.large),
      ) {
        OsmInteractiveMap(
          points = points,
          currentHighlightIndex = if (points.isNotEmpty()) replayIndex else null,
          isOnline = isOnline,
          modifier = Modifier.fillMaxSize(),
        )
      }

      if (points.size > 1) {
        Card(
          shape = MaterialTheme.shapes.medium,
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Text("Route Replay", style = MaterialTheme.typography.titleMedium)
              FilledTonalButton(
                onClick = { isReplaying = !isReplaying },
                modifier = Modifier.testTag("replay_toggle_button"),
              ) {
                Icon(
                  if (isReplaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                  contentDescription = null,
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (isReplaying) "Pause" else "Replay Route")
              }
            }
            Slider(
              value = replayIndex.toFloat(),
              onValueChange = {
                isReplaying = false
                replayIndex = it.toInt().coerceIn(0, points.lastIndex)
              },
              valueRange = 0f..points.lastIndex.toFloat(),
            )
            Text(
              "Waypoint ${replayIndex + 1} / ${points.size}",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }

      Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Text("Session Summary (${session.dateIso})", style = MaterialTheme.typography.titleMedium)
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            TelemetryStat(
              "Distance",
              WellnessCalculators.formatDistance(session.distanceMeters, preferences.useImperialUnits),
            )
            TelemetryStat("Duration", WellnessCalculators.formatDurationMillis(session.activeDurationMillis))
            TelemetryStat(
              "Avg Speed",
              WellnessCalculators.formatSpeed(session.avgSpeedMps, preferences.useImperialUnits),
            )
            TelemetryStat("Est. kcal", "${session.caloriesKcal.toInt()}")
          }
          if (session.steps > 0) {
            Text(
              "Estimated Steps: ${session.steps} • GPS Points Recorded: ${points.size}",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          if (points.size >= 2) {
            val elev = remember(points) { WellnessAdvancedCalculators.calculateGpsElevationStats(points) }
            Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(10.dp)) {
              Row(modifier = Modifier.fillMaxWidth().padding(10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Terrain, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(
                  "Elev +${elev.elevationGainMeters.toInt()}m / -${elev.elevationLossMeters.toInt()}m • Max Grade ${String.format(Locale.US, "%.1f", elev.maxGradePercent)}%",
                  style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFamily),
                )
              }
            }
          }
          OutlinedButton(
            onClick = { onExportGpsGpx(session.id) },
            enabled = points.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("export_gpx_button"),
          ) {
            Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Export Route (.gpx)")
          }
        }
      }
    }
  }
}
