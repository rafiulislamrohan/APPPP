package com.example.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.health.WellnessCalculators
import com.example.data.local.GpsActivitySessionEntity
import com.example.data.local.GpsPoint
import com.example.data.local.SyncStatus
import com.example.data.local.UserPreferencesDataStore
import com.example.data.local.VitalTrackDatabase
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

enum class TrackingRunState {
  IDLE,
  ACTIVE,
  PAUSED,
}

data class ActiveTrackingState(
  val runState: TrackingRunState = TrackingRunState.IDLE,
  val activityType: String = "WALKING",
  val startTimeMillis: Long = 0L,
  val activeDurationMillis: Long = 0L,
  val distanceMeters: Double = 0.0,
  val currentSpeedMps: Double = 0.0,
  val avgSpeedMps: Double = 0.0,
  val caloriesKcal: Double = 0.0,
  val points: List<GpsPoint> = emptyList(),
  val lastLocationError: String? = null,
)

object TrackingSessionHolder {
  private val _state = MutableStateFlow(ActiveTrackingState())
  val state: StateFlow<ActiveTrackingState> = _state.asStateFlow()

  fun update(newState: ActiveTrackingState) {
    _state.value = newState
  }

  fun reset() {
    _state.value = ActiveTrackingState()
  }
}

class ActivityTrackingForegroundService : Service() {

  companion object {
    private const val TAG = "ActivityTrackingSvc"
    const val CHANNEL_ID = "vitaltrack_gps_tracking"
    const val NOTIFICATION_ID = 4201

    const val ACTION_START = "com.example.vitaltrack.ACTION_START_TRACKING"
    const val ACTION_PAUSE = "com.example.vitaltrack.ACTION_PAUSE_TRACKING"
    const val ACTION_RESUME = "com.example.vitaltrack.ACTION_RESUME_TRACKING"
    const val ACTION_STOP = "com.example.vitaltrack.ACTION_STOP_TRACKING"
    const val EXTRA_ACTIVITY_TYPE = "extra_activity_type"

    fun startTracking(context: Context, activityType: String) {
      val intent =
        Intent(context, ActivityTrackingForegroundService::class.java).apply {
          action = ACTION_START
          putExtra(EXTRA_ACTIVITY_TYPE, activityType)
        }
      ContextCompat.startForegroundService(context, intent)
    }

    fun pauseTracking(context: Context) {
      context.startService(
        Intent(context, ActivityTrackingForegroundService::class.java).apply {
          action = ACTION_PAUSE
        }
      )
    }

    fun resumeTracking(context: Context) {
      context.startService(
        Intent(context, ActivityTrackingForegroundService::class.java).apply {
          action = ACTION_RESUME
        }
      )
    }

    fun stopTracking(context: Context) {
      context.startService(
        Intent(context, ActivityTrackingForegroundService::class.java).apply {
          action = ACTION_STOP
        }
      )
    }
  }

  private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private val json = Json { ignoreUnknownKeys = true }
  private lateinit var fusedClient: FusedLocationProviderClient
  private var tickerJob: Job? = null
  private var lastResumeTickMillis: Long = 0L
  private var accumulatedDurationBeforePause: Long = 0L
  private var userWeightKg: Double = 70.0
  private var userStrideMeters: Double = 0.75

  private val locationCallback =
    object : LocationCallback() {
      override fun onLocationResult(result: LocationResult) {
        val current = TrackingSessionHolder.state.value
        if (current.runState != TrackingRunState.ACTIVE) return

        val newPoints = current.points.toMutableList()
        var addedDistance = 0.0
        var latestSpeed = current.currentSpeedMps

        result.locations.forEach { loc ->
          // Filter out wildly inaccurate spikes (> 45m accuracy)
          if (loc.accuracy > 45f && newPoints.isNotEmpty()) return@forEach
          val point =
            GpsPoint(
              latitude = loc.latitude,
              longitude = loc.longitude,
              altitudeMeters = if (loc.hasAltitude()) loc.altitude else 0.0,
              speedMps = if (loc.hasSpeed()) loc.speed else 0f,
              accuracyMeters = loc.accuracy,
              timestampMillis = loc.time,
            )
          val prev = newPoints.lastOrNull()
          if (prev != null) {
            val delta = WellnessCalculators.haversineMeters(prev, point)
            // Ignore jitter < 2m
            if (delta >= 2.0) {
              addedDistance += delta
              newPoints.add(point)
            }
          } else {
            newPoints.add(point)
          }
          if (loc.hasSpeed()) {
            latestSpeed = loc.speed.toDouble()
          }
        }

        val totalDistance = current.distanceMeters + addedDistance
        val elapsed = computeCurrentActiveDuration()
        val avgSpeed = if (elapsed > 0L) totalDistance / (elapsed / 1000.0) else 0.0
        val calories =
          WellnessCalculators.estimateActivityCalories(
            met = WellnessCalculators.metForActivityType(current.activityType),
            weightKg = userWeightKg,
            durationMillis = elapsed,
          )

        TrackingSessionHolder.update(
          current.copy(
            distanceMeters = totalDistance,
            activeDurationMillis = elapsed,
            currentSpeedMps = latestSpeed,
            avgSpeedMps = avgSpeed,
            caloriesKcal = calories,
            points = newPoints,
            lastLocationError = null,
          )
        )
        updateNotification()
      }
    }

  override fun onCreate() {
    super.onCreate()
    fusedClient = LocationServices.getFusedLocationProviderClient(this)
    createNotificationChannel()
    serviceScope.launch {
      val prefs = UserPreferencesDataStore(applicationContext).getCurrent()
      userWeightKg = prefs.weightKg
      userStrideMeters = prefs.strideLengthMeters
    }
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    when (intent?.action) {
      ACTION_START -> {
        val type = intent.getStringExtra(EXTRA_ACTIVITY_TYPE) ?: "WALKING"
        beginSession(type)
      }
      ACTION_PAUSE -> pauseSession()
      ACTION_RESUME -> resumeSession()
      ACTION_STOP -> stopAndPersistSession()
    }
    return START_NOT_STICKY
  }

  private fun hasLocationPermission(): Boolean {
    val fine =
      ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED
    val coarse =
      ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED
    return fine || coarse
  }

  private fun beginSession(activityType: String) {
    val now = System.currentTimeMillis()
    accumulatedDurationBeforePause = 0L
    lastResumeTickMillis = now
    TrackingSessionHolder.update(
      ActiveTrackingState(
        runState = TrackingRunState.ACTIVE,
        activityType = activityType,
        startTimeMillis = now,
        activeDurationMillis = 0L,
        distanceMeters = 0.0,
        points = emptyList(),
        lastLocationError = if (!hasLocationPermission()) "Location permission not granted" else null,
      )
    )
    startAsForeground(buildNotification())
    if (hasLocationPermission()) {
      startLocationUpdates()
      startDurationTicker()
    } else {
      Log.w(TAG, "Cannot start GPS updates without location permission")
      stopSelf()
    }
  }

  private fun pauseSession() {
    val current = TrackingSessionHolder.state.value
    if (current.runState != TrackingRunState.ACTIVE) return
    accumulatedDurationBeforePause = computeCurrentActiveDuration()
    stopLocationUpdates()
    tickerJob?.cancel()
    TrackingSessionHolder.update(
      current.copy(
        runState = TrackingRunState.PAUSED,
        activeDurationMillis = accumulatedDurationBeforePause,
      )
    )
    updateNotification()
  }

  private fun resumeSession() {
    val current = TrackingSessionHolder.state.value
    if (current.runState != TrackingRunState.PAUSED) return
    lastResumeTickMillis = System.currentTimeMillis()
    TrackingSessionHolder.update(current.copy(runState = TrackingRunState.ACTIVE))
    startLocationUpdates()
    startDurationTicker()
    updateNotification()
  }

  private fun stopAndPersistSession() {
    val snapshot = TrackingSessionHolder.state.value
    stopLocationUpdates()
    tickerJob?.cancel()
    val finalDuration =
      if (snapshot.runState == TrackingRunState.ACTIVE) {
        computeCurrentActiveDuration()
      } else {
        snapshot.activeDurationMillis
      }
    val endTime = System.currentTimeMillis()

    if (snapshot.startTimeMillis > 0L && (finalDuration >= 5_000L || snapshot.points.isNotEmpty())) {
      val avgSpeed =
        if (finalDuration > 0L) snapshot.distanceMeters / (finalDuration / 1000.0) else 0.0
      val calories =
        WellnessCalculators.estimateActivityCalories(
          met = WellnessCalculators.metForActivityType(snapshot.activityType),
          weightKg = userWeightKg,
          durationMillis = finalDuration,
        )
      val estSteps =
        if (snapshot.activityType == "CYCLING") {
          0
        } else {
          (snapshot.distanceMeters / userStrideMeters).toInt().coerceAtLeast(0)
        }
      val label =
        when (snapshot.activityType) {
          "RUNNING" -> "Outdoor Run"
          "CYCLING" -> "Cycling Ride"
          else -> "Outdoor Walk"
        }
      val entity =
        GpsActivitySessionEntity(
          id = UUID.randomUUID().toString(),
          activityType = snapshot.activityType,
          title = label,
          startTimeMillis = snapshot.startTimeMillis,
          endTimeMillis = endTime,
          activeDurationMillis = finalDuration,
          distanceMeters = snapshot.distanceMeters,
          avgSpeedMps = avgSpeed,
          caloriesKcal = calories,
          steps = estSteps,
          dateIso = WellnessCalculators.epochMillisToIsoDate(snapshot.startTimeMillis),
          routePolylineJson = json.encodeToString(snapshot.points),
          updatedAtMillis = endTime,
          syncStatus = SyncStatus.PENDING_SYNC,
        )
      serviceScope.launch {
        runCatching {
          VitalTrackDatabase.getInstance(applicationContext).gpsSessionDao().upsert(entity)
        }.onFailure { Log.e(TAG, "Failed saving GPS session", it) }
        TrackingSessionHolder.reset()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
      }
    } else {
      TrackingSessionHolder.reset()
      stopForeground(STOP_FOREGROUND_REMOVE)
      stopSelf()
    }
  }

  private fun computeCurrentActiveDuration(): Long {
    val current = TrackingSessionHolder.state.value
    return if (current.runState == TrackingRunState.ACTIVE) {
      accumulatedDurationBeforePause + (System.currentTimeMillis() - lastResumeTickMillis)
    } else {
      accumulatedDurationBeforePause
    }
  }

  private fun startDurationTicker() {
    tickerJob?.cancel()
    tickerJob =
      serviceScope.launch {
        while (true) {
          delay(1000L)
          val cur = TrackingSessionHolder.state.value
          if (cur.runState != TrackingRunState.ACTIVE) break
          val elapsed = computeCurrentActiveDuration()
          val avgSpeed = if (elapsed > 0L) cur.distanceMeters / (elapsed / 1000.0) else 0.0
          val calories =
            WellnessCalculators.estimateActivityCalories(
              met = WellnessCalculators.metForActivityType(cur.activityType),
              weightKg = userWeightKg,
              durationMillis = elapsed,
            )
          TrackingSessionHolder.update(
            cur.copy(
              activeDurationMillis = elapsed,
              avgSpeedMps = avgSpeed,
              caloriesKcal = calories,
            )
          )
          updateNotification()
        }
      }
  }

  @SuppressLint("MissingPermission")
  private fun startLocationUpdates() {
    if (!hasLocationPermission()) return
    val request =
      LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000L)
        .setMinUpdateIntervalMillis(1500L)
        .setMinUpdateDistanceMeters(2f)
        .build()
    fusedClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
  }

  private fun stopLocationUpdates() {
    fusedClient.removeLocationUpdates(locationCallback)
  }

  private fun startAsForeground(notification: Notification) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      startForeground(
        NOTIFICATION_ID,
        notification,
        ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION,
      )
    } else {
      startForeground(NOTIFICATION_ID, notification)
    }
  }

  private fun createNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val manager = getSystemService(NotificationManager::class.java)
      val channel =
        NotificationChannel(
          CHANNEL_ID,
          getString(R.string.channel_tracking_name),
          NotificationManager.IMPORTANCE_LOW,
        ).apply { description = getString(R.string.channel_tracking_desc) }
      manager.createNotificationChannel(channel)
    }
  }

  private fun updateNotification() {
    val manager = getSystemService(NotificationManager::class.java)
    manager.notify(NOTIFICATION_ID, buildNotification())
  }

  private fun buildNotification(): Notification {
    val state = TrackingSessionHolder.state.value
    val openIntent =
      PendingIntent.getActivity(
        this,
        0,
        Intent(this, MainActivity::class.java).apply {
          flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )

    val toggleActionIntent =
      PendingIntent.getService(
        this,
        1,
        Intent(this, ActivityTrackingForegroundService::class.java).apply {
          action = if (state.runState == TrackingRunState.ACTIVE) ACTION_PAUSE else ACTION_RESUME
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )

    val stopIntent =
      PendingIntent.getService(
        this,
        2,
        Intent(this, ActivityTrackingForegroundService::class.java).apply {
          action = ACTION_STOP
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )

    val distKm = String.format(java.util.Locale.US, "%.2f km", state.distanceMeters / 1000.0)
    val dur = WellnessCalculators.formatDurationMillis(state.activeDurationMillis)
    val statusLabel =
      if (state.runState == TrackingRunState.PAUSED) "Paused" else state.activityType.lowercase().replaceFirstChar { it.uppercase() }

    return NotificationCompat.Builder(this, CHANNEL_ID)
      .setSmallIcon(R.drawable.ic_notification_cue)
      .setContentTitle(getString(R.string.tracking_notif_title))
      .setContentText("$statusLabel • $distKm • $dur")
      .setOngoing(true)
      .setOnlyAlertOnce(true)
      .setContentIntent(openIntent)
      .addAction(
        0,
        if (state.runState == TrackingRunState.ACTIVE) getString(R.string.tracking_action_pause)
        else getString(R.string.tracking_action_resume),
        toggleActionIntent,
      )
      .addAction(0, getString(R.string.tracking_action_stop), stopIntent)
      .build()
  }

  override fun onBind(intent: Intent?): IBinder? = null

  override fun onDestroy() {
    stopLocationUpdates()
    tickerJob?.cancel()
    serviceScope.cancel()
    super.onDestroy()
  }
}
