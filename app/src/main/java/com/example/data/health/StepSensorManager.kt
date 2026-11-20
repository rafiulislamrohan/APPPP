package com.example.data.health

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.data.local.DailyStepDao
import com.example.data.local.DailyStepEntity
import com.example.data.local.SyncStatus
import com.example.data.local.UserPreferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.resume

enum class StepSensorType {
  TYPE_STEP_COUNTER,
  TYPE_STEP_DETECTOR,
  UNAVAILABLE,
}

data class StepSensorStatus(
  val sensorType: StepSensorType,
  val permissionGranted: Boolean,
  val isListening: Boolean,
  val lastEventTimestampMillis: Long = 0L,
  val statusMessage: String,
)

class StepSensorManager(
  private val context: Context,
  private val dailyStepDao: DailyStepDao,
  private val prefs: UserPreferencesDataStore,
) {
  companion object {
    private const val TAG = "StepSensorManager"
  }

  private val sensorManager =
    context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
  private val stepCounterSensor: Sensor? =
    sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
  private val stepDetectorSensor: Sensor? =
    sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)

  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private val mutex = Mutex()

  private val _sensorStatus = MutableStateFlow(computeInitialStatus())
  val sensorStatus: StateFlow<StepSensorStatus> = _sensorStatus.asStateFlow()

  private var activeListener: SensorEventListener? = null

  fun availableSensorType(): StepSensorType =
    when {
      stepCounterSensor != null -> StepSensorType.TYPE_STEP_COUNTER
      stepDetectorSensor != null -> StepSensorType.TYPE_STEP_DETECTOR
      else -> StepSensorType.UNAVAILABLE
    }

  fun hasActivityRecognitionPermission(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) ==
        PackageManager.PERMISSION_GRANTED
    } else {
      true
    }
  }

  private fun computeInitialStatus(): StepSensorStatus {
    val type = availableSensorType()
    val permitted = hasActivityRecognitionPermission()
    val msg =
      when {
        type == StepSensorType.UNAVAILABLE ->
          "Hardware step sensor unavailable on this device. Use GPS sessions or Health Connect."
        !permitted -> "Activity recognition permission required to read hardware step sensors."
        type == StepSensorType.TYPE_STEP_COUNTER ->
          "Hardware step counter ready (TYPE_STEP_COUNTER)."
        else -> "Hardware step detector fallback ready (TYPE_STEP_DETECTOR)."
      }
    return StepSensorStatus(type, permitted, isListening = false, statusMessage = msg)
  }

  fun startListening() {
    val type = availableSensorType()
    val permitted = hasActivityRecognitionPermission()
    if (type == StepSensorType.UNAVAILABLE || !permitted || sensorManager == null) {
      _sensorStatus.value = computeInitialStatus()
      return
    }
    if (activeListener != null) return

    val targetSensor =
      if (type == StepSensorType.TYPE_STEP_COUNTER) stepCounterSensor else stepDetectorSensor
    if (targetSensor == null) return

    val listener =
      object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
          if (event == null) return
          val now = System.currentTimeMillis()
          scope.launch {
            if (type == StepSensorType.TYPE_STEP_COUNTER) {
              val bootSteps = event.values.firstOrNull()?.toLong() ?: return@launch
              recordBootCounterReading(bootSteps)
            } else {
              val increment = event.values.firstOrNull()?.toInt()?.coerceAtLeast(1) ?: 1
              recordStepDetectorIncrement(increment)
            }
            _sensorStatus.value =
              _sensorStatus.value.copy(
                permissionGranted = true,
                isListening = true,
                lastEventTimestampMillis = now,
                statusMessage =
                  if (type == StepSensorType.TYPE_STEP_COUNTER) {
                    "Live hardware step counter active"
                  } else {
                    "Live step detector fallback active"
                  },
              )
          }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
          Log.d(TAG, "Step sensor accuracy changed: $accuracy")
        }
      }

    val registered =
      sensorManager.registerListener(listener, targetSensor, SensorManager.SENSOR_DELAY_UI)
    if (registered) {
      activeListener = listener
      _sensorStatus.value =
        computeInitialStatus().copy(
          isListening = true,
          statusMessage =
            if (type == StepSensorType.TYPE_STEP_COUNTER) {
              "Listening to hardware TYPE_STEP_COUNTER"
            } else {
              "Listening to hardware TYPE_STEP_DETECTOR"
            },
        )
    } else {
      _sensorStatus.value =
        computeInitialStatus().copy(
          isListening = false,
          statusMessage = "Unable to register step sensor listener on this device.",
        )
    }
  }

  fun stopListening() {
    activeListener?.let {
      sensorManager?.unregisterListener(it)
      activeListener = null
    }
    _sensorStatus.value = computeInitialStatus().copy(isListening = false)
  }

  /**
   * One-shot sensor snapshot used by WorkManager periodic background sync.
   */
  suspend fun sampleOnceIfCounterAvailable(): Long? {
    if (!hasActivityRecognitionPermission() || sensorManager == null || stepCounterSensor == null) {
      return null
    }
    return suspendCancellableCoroutine { cont ->
      var listener: SensorEventListener? = null
      listener =
        object : SensorEventListener {
          override fun onSensorChanged(event: SensorEvent?) {
            val value = event?.values?.firstOrNull()?.toLong()
            sensorManager.unregisterListener(this)
            if (cont.isActive) cont.resume(value)
          }

          override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
      val ok =
        sensorManager.registerListener(
          listener,
          stepCounterSensor,
          SensorManager.SENSOR_DELAY_NORMAL,
        )
      if (!ok) {
        if (cont.isActive) cont.resume(null)
      }
      cont.invokeOnCancellation { sensorManager.unregisterListener(listener) }
    }
  }

  /**
   * Handles midnight reset, device reboot detection, and persistent daily totals without
   * losing any historical records.
   */
  suspend fun recordBootCounterReading(stepsSinceBoot: Long) = mutex.withLock {
    val today = WellnessCalculators.todayIso()
    val profile = prefs.getCurrent()
    val existingToday = dailyStepDao.getStepsForDate(today)

    val newTodaySteps =
      when {
        // First ever sensor reading: establish baseline at current boot value
        profile.lastSensorBootSteps < 0L -> existingToday?.steps ?: 0
        // Date rolled over to a new day: reset today's counter while preserving previous day's Room row
        profile.lastSensorDateIso.isNotEmpty() && profile.lastSensorDateIso != today -> {
          val delta =
            if (stepsSinceBoot >= profile.lastSensorBootSteps) {
              (stepsSinceBoot - profile.lastSensorBootSteps).toInt().coerceAtLeast(0)
            } else {
              stepsSinceBoot.toInt().coerceAtLeast(0)
            }
          delta
        }
        // Same day, normal monotonic boot counter increase
        stepsSinceBoot >= profile.lastSensorBootSteps -> {
          val delta = (stepsSinceBoot - profile.lastSensorBootSteps).toInt()
          (existingToday?.steps ?: profile.todaySensorAccumulatedSteps) + delta
        }
        // Same day, device rebooted (boot counter reset below last recorded value)
        else -> {
          (existingToday?.steps ?: profile.todaySensorAccumulatedSteps) +
            stepsSinceBoot.toInt().coerceAtLeast(0)
        }
      }

    prefs.updateSensorBaseline(
      dateIso = today,
      bootCounterValue = stepsSinceBoot,
      accumulatedToday = newTodaySteps,
    )

    persistDailyStepRow(
      dateIso = today,
      steps = newTodaySteps,
      goal = profile.dailyStepGoal,
      strideMeters = profile.strideLengthMeters,
      weightKg = profile.weightKg,
      source = "SENSOR_COUNTER",
    )
  }

  private suspend fun recordStepDetectorIncrement(delta: Int) = mutex.withLock {
    val today = WellnessCalculators.todayIso()
    val profile = prefs.getCurrent()
    val existingToday = dailyStepDao.getStepsForDate(today)
    val base =
      if (profile.lastSensorDateIso == today) {
        existingToday?.steps ?: profile.todaySensorAccumulatedSteps
      } else {
        0
      }
    val updated = base + delta
    prefs.updateSensorBaseline(
      dateIso = today,
      bootCounterValue = profile.lastSensorBootSteps,
      accumulatedToday = updated,
    )
    persistDailyStepRow(
      dateIso = today,
      steps = updated,
      goal = profile.dailyStepGoal,
      strideMeters = profile.strideLengthMeters,
      weightKg = profile.weightKg,
      source = "SENSOR_DETECTOR",
    )
  }

  suspend fun ensureTodayRowExists() = mutex.withLock {
    val today = WellnessCalculators.todayIso()
    val profile = prefs.getCurrent()
    if (dailyStepDao.getStepsForDate(today) == null) {
      persistDailyStepRow(
        dateIso = today,
        steps = 0,
        goal = profile.dailyStepGoal,
        strideMeters = profile.strideLengthMeters,
        weightKg = profile.weightKg,
        source = availableSensorType().name,
      )
    }
  }

  suspend fun addGpsDerivedStepsIfHigher(gpsSteps: Int) = mutex.withLock {
    if (gpsSteps <= 0) return@withLock
    val today = WellnessCalculators.todayIso()
    val profile = prefs.getCurrent()
    val existing = dailyStepDao.getStepsForDate(today)
    if (availableSensorType() == StepSensorType.UNAVAILABLE) {
      val updated = (existing?.steps ?: 0) + gpsSteps
      persistDailyStepRow(
        dateIso = today,
        steps = updated,
        goal = profile.dailyStepGoal,
        strideMeters = profile.strideLengthMeters,
        weightKg = profile.weightKg,
        source = "GPS_DERIVED",
      )
    }
  }

  private suspend fun persistDailyStepRow(
    dateIso: String,
    steps: Int,
    goal: Int,
    strideMeters: Double,
    weightKg: Double,
    source: String,
  ) {
    val dist = WellnessCalculators.estimateDistanceMeters(steps, strideMeters)
    val kcal = WellnessCalculators.estimateStepCalories(steps, weightKg)
    val activeMin = WellnessCalculators.estimateActiveMinutesFromSteps(steps)
    dailyStepDao.upsert(
      DailyStepEntity(
        dateIso = dateIso,
        steps = steps,
        goal = goal,
        distanceMeters = dist,
        caloriesKcal = kcal,
        activeMinutes = activeMin,
        source = source,
        updatedAtMillis = System.currentTimeMillis(),
        syncStatus = SyncStatus.PENDING_SYNC,
      )
    )
  }
}
