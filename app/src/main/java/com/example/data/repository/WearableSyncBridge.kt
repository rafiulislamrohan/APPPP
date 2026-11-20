package com.example.data.repository

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class WearableDailySnapshotPayload(
  val dateIso: String,
  val steps: Int,
  val stepGoal: Int,
  val waterMl: Int,
  val waterGoalMl: Int,
  val activeMinutes: Int,
)

data class WearOsBridgeStatus(
  val companionAppDetected: Boolean,
  val dataLayerPath: String,
  val statusDescription: String,
)

/**
 * Future-ready Wear OS Data Layer integration contract.
 * Detects whether the Wear OS companion app is installed on the phone and
 * defines the canonical DataClient path (/vitaltrack/daily_snapshot) without
 * fabricating watch sensor readings.
 */
class WearableSyncBridge(private val context: Context) {
  private val json = Json { prettyPrint = true }

  companion object {
    const val DATA_LAYER_PATH = "/vitaltrack/daily_snapshot"
    private const val WEAR_COMPANION_PKG = "com.google.android.wearable.app"
  }

  fun buildDailySnapshotJson(
    dateIso: String,
    steps: Int,
    stepGoal: Int,
    waterMl: Int,
    waterGoalMl: Int,
    activeMinutes: Int,
  ): String {
    val payload =
      WearableDailySnapshotPayload(
        dateIso = dateIso,
        steps = steps,
        stepGoal = stepGoal,
        waterMl = waterMl,
        waterGoalMl = waterGoalMl,
        activeMinutes = activeMinutes,
      )
    return json.encodeToString(payload)
  }

  fun inspectBridgeStatus(): WearOsBridgeStatus {
    val installed =
      runCatching {
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(
              WEAR_COMPANION_PKG,
              PackageManager.PackageInfoFlags.of(0),
            )
          } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(WEAR_COMPANION_PKG, 0)
          }
          true
        }
        .getOrDefault(false)

    val desc =
      if (installed) {
        "Wear OS companion detected. Watch metrics sync through Health Connect & $DATA_LAYER_PATH when paired."
      } else {
        "Wear OS companion not installed on this device. Connect a wearable via Health Connect or pair a Wear OS watch."
      }
    return WearOsBridgeStatus(
      companionAppDetected = installed,
      dataLayerPath = DATA_LAYER_PATH,
      statusDescription = desc,
    )
  }
}
