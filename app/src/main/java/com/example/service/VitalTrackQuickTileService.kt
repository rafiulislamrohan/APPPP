package com.example.service

import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import com.example.MainActivity
import com.example.data.health.WellnessCalculators
import com.example.data.local.SyncStatus
import com.example.data.local.VitalTrackDatabase
import com.example.data.local.WaterIntakeEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Android Quick Settings Tile (#92):
 * One tap logs +250 ml hydration cleanly in Room and refreshes the home widget,
 * or opens VITALTRACK on long-press.
 */
@RequiresApi(Build.VERSION_CODES.N)
class VitalTrackQuickTileService : TileService() {

  override fun onStartListening() {
    super.onStartListening()
    qsTile?.let { tile ->
      tile.state = Tile.STATE_ACTIVE
      tile.label = "VITALTRACK +250ml"
      tile.subtitle = "Tap to log water"
      tile.updateTile()
    }
  }

  override fun onClick() {
    super.onClick()
    CoroutineScope(Dispatchers.IO).launch {
      runCatching {
        val db = VitalTrackDatabase.getInstance(applicationContext)
        val today = WellnessCalculators.todayIso()
        db.healthRecordDao()
          .insertWater(
            WaterIntakeEntity(
              id = UUID.randomUUID().toString(),
              amountMl = 250,
              recordedAtMillis = System.currentTimeMillis(),
              dateIso = today,
              syncStatus = SyncStatus.PENDING_SYNC,
            )
          )
        VitalTrackWidgetUpdater.refreshAll(applicationContext)
        qsTile?.let { tile ->
          val totalToday =
            db.healthRecordDao().getWaterSnapshot().filter { it.dateIso == today }.sumOf { it.amountMl }
          tile.subtitle = "$totalToday ml today"
          tile.updateTile()
        }
      }.onFailure {
        val openIntent =
          Intent(applicationContext, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
          }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
          startActivityAndCollapse(
            android.app.PendingIntent.getActivity(
              applicationContext,
              9201,
              openIntent,
              android.app.PendingIntent.FLAG_IMMUTABLE,
            )
          )
        }
      }
    }
  }
}
