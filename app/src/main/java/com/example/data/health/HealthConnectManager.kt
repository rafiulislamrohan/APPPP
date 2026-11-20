package com.example.data.health

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HydrationRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Mass
import androidx.health.connect.client.units.Volume
import com.example.data.local.HeartRateRecordEntity
import com.example.data.local.SleepRecordEntity
import com.example.data.local.SyncStatus
import com.example.data.local.WeightEntryEntity
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

enum class HealthConnectAvailability {
  AVAILABLE,
  NOT_INSTALLED,
  NOT_SUPPORTED,
}

data class HealthConnectSyncResult(
  val todaySteps: Long?,
  val heartRates: List<HeartRateRecordEntity>,
  val sleepRecords: List<SleepRecordEntity>,
  val weightEntries: List<WeightEntryEntity>,
)

class HealthConnectManager(private val context: Context) {
  companion object {
    private const val TAG = "HealthConnectManager"

    val REQUIRED_PERMISSIONS: Set<String> =
      setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getReadPermission(WeightRecord::class),
      )

    val WRITE_PERMISSIONS: Set<String> =
      setOf(
        HealthPermission.getWritePermission(StepsRecord::class),
        HealthPermission.getWritePermission(WeightRecord::class),
        HealthPermission.getWritePermission(HydrationRecord::class),
      )
  }

  fun getAvailability(): HealthConnectAvailability {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
      return HealthConnectAvailability.NOT_SUPPORTED
    }
    return when (HealthConnectClient.getSdkStatus(context)) {
      HealthConnectClient.SDK_AVAILABLE -> HealthConnectAvailability.AVAILABLE
      HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED ->
        HealthConnectAvailability.NOT_INSTALLED
      else -> HealthConnectAvailability.NOT_SUPPORTED
    }
  }

  private fun getClientOrNull(): HealthConnectClient? {
    if (getAvailability() != HealthConnectAvailability.AVAILABLE) return null
    return runCatching { HealthConnectClient.getOrCreate(context) }.getOrNull()
  }

  suspend fun hasAllPermissions(): Boolean {
    val client = getClientOrNull() ?: return false
    return runCatching {
      val granted = client.permissionController.getGrantedPermissions()
      granted.containsAll(REQUIRED_PERMISSIONS)
    }.getOrDefault(false)
  }

  suspend fun writeWellnessSnapshot(
    todaySteps: Int,
    todayWaterMl: Int,
    latestWeightKg: Double?,
  ): Result<Int> {
    val client =
      getClientOrNull()
        ?: return Result.failure(IllegalStateException("Health Connect unavailable on device."))
    return try {
      val granted = client.permissionController.getGrantedPermissions()
      val records = mutableListOf<androidx.health.connect.client.records.Record>()
      val now = Instant.now()
      val zone = ZoneOffset.systemDefault().rules.getOffset(now)
      val dayStart = now.truncatedTo(ChronoUnit.DAYS)

      if (todaySteps > 0 && granted.contains(HealthPermission.getWritePermission(StepsRecord::class))) {
        records +=
          StepsRecord(
            count = todaySteps.toLong(),
            startTime = dayStart,
            endTime = now,
            startZoneOffset = zone,
            endZoneOffset = zone,
            metadata = Metadata.manualEntry(),
          )
      }
      if (todayWaterMl > 0 && granted.contains(HealthPermission.getWritePermission(HydrationRecord::class))) {
        records +=
          HydrationRecord(
            startTime = dayStart,
            endTime = now,
            startZoneOffset = zone,
            endZoneOffset = zone,
            volume = Volume.liters(todayWaterMl / 1000.0),
            metadata = Metadata.manualEntry(),
          )
      }
      if (latestWeightKg != null && granted.contains(HealthPermission.getWritePermission(WeightRecord::class))) {
        records +=
          WeightRecord(
            time = now,
            zoneOffset = zone,
            weight = Mass.kilograms(latestWeightKg),
            metadata = Metadata.manualEntry(),
          )
      }
      if (records.isNotEmpty()) {
        client.insertRecords(records)
      }
      Result.success(records.size)
    } catch (e: Exception) {
      Log.e(TAG, "Failed writing Health Connect snapshot", e)
      Result.failure(e)
    }
  }

  suspend fun syncRecentHealthData(heightCm: Double): Result<HealthConnectSyncResult> {
    val client =
      getClientOrNull()
        ?: return Result.failure(
          IllegalStateException("Health Connect is not installed or supported on this device.")
        )

    return try {
      val granted = client.permissionController.getGrantedPermissions()
      val end = Instant.now()
      val start7Days = end.minus(7, ChronoUnit.DAYS)
      val todayStart =
        Instant.now().truncatedTo(ChronoUnit.DAYS)

      var todaySteps: Long? = null
      if (granted.contains(HealthPermission.getReadPermission(StepsRecord::class))) {
        val agg =
          client.aggregate(
            AggregateRequest(
              metrics = setOf(StepsRecord.COUNT_TOTAL),
              timeRangeFilter = TimeRangeFilter.between(todayStart, end),
            )
          )
        todaySteps = agg[StepsRecord.COUNT_TOTAL]
      }

      val heartRates = mutableListOf<HeartRateRecordEntity>()
      if (granted.contains(HealthPermission.getReadPermission(HeartRateRecord::class))) {
        val response =
          client.readRecords(
            ReadRecordsRequest(
              HeartRateRecord::class,
              timeRangeFilter = TimeRangeFilter.between(start7Days, end),
            )
          )
        response.records.forEach { record ->
          record.samples.forEachIndexed { idx, sample ->
            val millis = sample.time.toEpochMilli()
            heartRates.add(
              HeartRateRecordEntity(
                id = "hc_hr_${record.metadata.id}_$idx",
                bpm = sample.beatsPerMinute.toInt(),
                recordedAtMillis = millis,
                dateIso = WellnessCalculators.epochMillisToIsoDate(millis),
                source = "HEALTH_CONNECT",
                notes = record.metadata.dataOrigin.packageName,
                updatedAtMillis = System.currentTimeMillis(),
                syncStatus = SyncStatus.PENDING_SYNC,
              )
            )
          }
        }
      }

      val sleepRecords = mutableListOf<SleepRecordEntity>()
      if (granted.contains(HealthPermission.getReadPermission(SleepSessionRecord::class))) {
        val response =
          client.readRecords(
            ReadRecordsRequest(
              SleepSessionRecord::class,
              timeRangeFilter = TimeRangeFilter.between(start7Days, end),
            )
          )
        response.records.forEach { session ->
          val startMs = session.startTime.toEpochMilli()
          val endMs = session.endTime.toEpochMilli()
          val durationMin = Duration.between(session.startTime, session.endTime).toMinutes().toInt()
          var deepMin = 0
          var lightMin = 0
          var remMin = 0
          var awakeMin = 0
          session.stages.forEach { stage ->
            val mins = Duration.between(stage.startTime, stage.endTime).toMinutes().toInt()
            when (stage.stage) {
              SleepSessionRecord.STAGE_TYPE_DEEP -> deepMin += mins
              SleepSessionRecord.STAGE_TYPE_LIGHT -> lightMin += mins
              SleepSessionRecord.STAGE_TYPE_REM -> remMin += mins
              SleepSessionRecord.STAGE_TYPE_AWAKE,
              SleepSessionRecord.STAGE_TYPE_OUT_OF_BED -> awakeMin += mins
            }
          }
          sleepRecords.add(
            SleepRecordEntity(
              id = "hc_sleep_${session.metadata.id}",
              startTimeMillis = startMs,
              endTimeMillis = endMs,
              durationMinutes = durationMin,
              deepMinutes = deepMin,
              lightMinutes = lightMin,
              remMinutes = remMin,
              awakeMinutes = awakeMin,
              qualityScore = ((durationMin.coerceAtMost(480) / 480.0) * 100).toInt(),
              dateIso = WellnessCalculators.epochMillisToIsoDate(endMs),
              source = "HEALTH_CONNECT",
              notes = session.title ?: session.metadata.dataOrigin.packageName,
              updatedAtMillis = System.currentTimeMillis(),
              syncStatus = SyncStatus.PENDING_SYNC,
            )
          )
        }
      }

      val weightEntries = mutableListOf<WeightEntryEntity>()
      if (granted.contains(HealthPermission.getReadPermission(WeightRecord::class))) {
        val response =
          client.readRecords(
            ReadRecordsRequest(
              WeightRecord::class,
              timeRangeFilter = TimeRangeFilter.between(start7Days, end),
            )
          )
        response.records.forEach { record ->
          val kg = record.weight.inKilograms
          val millis = record.time.toEpochMilli()
          weightEntries.add(
            WeightEntryEntity(
              id = "hc_weight_${record.metadata.id}",
              weightKg = kg,
              bmi = WellnessCalculators.calculateBmi(kg, heightCm),
              recordedAtMillis = millis,
              dateIso = WellnessCalculators.epochMillisToIsoDate(millis),
              source = "HEALTH_CONNECT",
              notes = record.metadata.dataOrigin.packageName,
              updatedAtMillis = System.currentTimeMillis(),
              syncStatus = SyncStatus.PENDING_SYNC,
            )
          )
        }
      }

      Result.success(
        HealthConnectSyncResult(
          todaySteps = todaySteps,
          heartRates = heartRates,
          sleepRecords = sleepRecords,
          weightEntries = weightEntries,
        )
      )
    } catch (e: Exception) {
      Log.e(TAG, "Failed to sync Health Connect data", e)
      Result.failure(e)
    }
  }
}
