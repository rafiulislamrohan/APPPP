package com.example

import android.app.Application
import com.example.data.health.HealthConnectManager
import com.example.data.health.StepSensorManager
import com.example.data.local.UserPreferencesDataStore
import com.example.data.local.VitalTrackDatabase
import com.example.data.remote.FirebaseAiCoachService
import com.example.data.remote.FirebaseStyleAdvisorService
import com.example.data.remote.FirestoreSyncRepository
import com.example.data.repository.ExportBackupRepository
import com.example.data.repository.VitalTrackRepository
import com.example.data.repository.WearableSyncBridge
import com.example.service.CloudSyncWorker
import com.example.service.ReminderNotificationHelper
import com.example.service.StepCounterWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration

class VitalTrackApplication : Application() {

  private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  val repository: VitalTrackRepository by lazy {
    val db = VitalTrackDatabase.getInstance(this)
    val prefs = UserPreferencesDataStore(this)
    val stepSensorManager = StepSensorManager(this, db.dailyStepDao(), prefs)
    val healthConnectManager = HealthConnectManager(this)
    val aiCoachService = FirebaseAiCoachService(this)
    val styleAdvisorService = FirebaseStyleAdvisorService(this)
    val firestoreSyncRepository = FirestoreSyncRepository(this, db, prefs)
    val exportBackupRepository = ExportBackupRepository(this, db, prefs)
    val wearableSyncBridge = WearableSyncBridge(this)
    VitalTrackRepository(
      context = this,
      db = db,
      prefs = prefs,
      stepSensorManager = stepSensorManager,
      healthConnectManager = healthConnectManager,
      aiCoachService = aiCoachService,
      styleAdvisorService = styleAdvisorService,
      firestoreSyncRepository = firestoreSyncRepository,
      exportBackupRepository = exportBackupRepository,
      wearableSyncBridge = wearableSyncBridge,
    )
  }

  override fun onCreate() {
    super.onCreate()
    // Configure OSMDroid user agent using application package name
    runCatching { Configuration.getInstance().userAgentValue = packageName }

    runCatching { ReminderNotificationHelper.createChannels(this) }
    StepCounterWorker.schedule(this)
    CloudSyncWorker.schedule(this)

    appScope.launch {
      runCatching { repository.ensureSeeded() }
    }
  }
}
