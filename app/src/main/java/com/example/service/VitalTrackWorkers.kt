package com.example.service

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.health.StepSensorManager
import com.example.data.local.UserPreferencesDataStore
import com.example.data.local.VitalTrackDatabase
import com.example.data.remote.FirestoreSyncRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import java.util.concurrent.TimeUnit

class StepCounterWorker(
  appContext: Context,
  workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

  override suspend fun doWork(): Result {
    return try {
      val db = VitalTrackDatabase.getInstance(applicationContext)
      val prefs = UserPreferencesDataStore(applicationContext)
      val sensorManager = StepSensorManager(applicationContext, db.dailyStepDao(), prefs)
      val sample = sensorManager.sampleOnceIfCounterAvailable()
      if (sample != null) {
        sensorManager.recordBootCounterReading(sample)
      } else {
        sensorManager.ensureTodayRowExists()
      }
      VitalTrackWidgetUpdater.refreshAll(applicationContext)
      Result.success()
    } catch (e: Exception) {
      Log.e("StepCounterWorker", "Step snapshot failed", e)
      Result.retry()
    }
  }

  companion object {
    private const val WORK_NAME = "vitaltrack_periodic_steps"

    fun schedule(context: Context) {
      runCatching {
        val req = PeriodicWorkRequestBuilder<StepCounterWorker>(15, TimeUnit.MINUTES).build()
        WorkManager.getInstance(context)
          .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, req)
      }.onFailure {
        Log.w("StepCounterWorker", "WorkManager unavailable in current context: ${it.message}")
      }
    }
  }
}

class CloudSyncWorker(
  appContext: Context,
  workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

  override suspend fun doWork(): Result {
    val user = runCatching { Firebase.auth.currentUser }.getOrNull() ?: return Result.success()
    val prefs = UserPreferencesDataStore(applicationContext)
    if (!prefs.getCurrent().cloudSyncEnabled) return Result.success()

    val db = VitalTrackDatabase.getInstance(applicationContext)
    val syncRepo = FirestoreSyncRepository(applicationContext, db, prefs)
    return syncRepo.syncNow(user.uid).fold(
      onSuccess = { Result.success() },
      onFailure = {
        Log.w("CloudSyncWorker", "Sync failed, retrying later: ${it.message}")
        Result.retry()
      },
    )
  }

  companion object {
    private const val WORK_NAME = "vitaltrack_cloud_sync"

    fun schedule(context: Context) {
      runCatching {
        val constraints =
          Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
        val req =
          PeriodicWorkRequestBuilder<CloudSyncWorker>(6, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(context)
          .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, req)
      }.onFailure {
        Log.w("CloudSyncWorker", "WorkManager unavailable in current context: ${it.message}")
      }
    }
  }
}

class AutomatedBackupWorker(
  appContext: Context,
  workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

  override suspend fun doWork(): Result {
    val prefs = UserPreferencesDataStore(applicationContext)
    if (!prefs.getCurrent().autoBackupEnabled) return Result.success()
    val db = VitalTrackDatabase.getInstance(applicationContext)
    val exportRepo = com.example.data.repository.ExportBackupRepository(applicationContext, db, prefs)
    return exportRepo.writeAutoBackupSnapshot().fold(
      onSuccess = { Result.success() },
      onFailure = { Result.retry() },
    )
  }

  companion object {
    const val WORK_NAME = "vitaltrack_auto_backup"

    fun setEnabled(context: Context, enabled: Boolean) {
      runCatching {
        val wm = WorkManager.getInstance(context)
        if (enabled) {
          val req = PeriodicWorkRequestBuilder<AutomatedBackupWorker>(24, TimeUnit.HOURS).build()
          wm.enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, req)
        } else {
          wm.cancelUniqueWork(WORK_NAME)
        }
      }
    }
  }
}

class ScreenBreakReminderWorker(
  appContext: Context,
  workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

  override suspend fun doWork(): Result {
    val prefs = UserPreferencesDataStore(applicationContext)
    if (!prefs.getCurrent().screenBreakReminderEnabled) return Result.success()
    ReminderNotificationHelper.showReminder(
      context = applicationContext,
      id = 4020,
      title = "20-20-20 Eye & Posture Break",
      body = "Look 20 ft away for 20 seconds, relax shoulders, and unclench jaw.",
    )
    return Result.success()
  }

  companion object {
    const val WORK_NAME = "vitaltrack_screen_break_20"

    fun setEnabled(context: Context, enabled: Boolean) {
      runCatching {
        val wm = WorkManager.getInstance(context)
        if (enabled) {
          val req = PeriodicWorkRequestBuilder<ScreenBreakReminderWorker>(20, TimeUnit.MINUTES).build()
          wm.enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, req)
        } else {
          wm.cancelUniqueWork(WORK_NAME)
        }
      }
    }
  }
}
