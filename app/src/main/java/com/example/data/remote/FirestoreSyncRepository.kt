package com.example.data.remote

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.health.WellnessCalculators
import com.example.data.local.DailyStepEntity
import com.example.data.local.GpsActivitySessionEntity
import com.example.data.local.HabitCompletionEntity
import com.example.data.local.JournalEntryEntity
import com.example.data.local.SleepRecordEntity
import com.example.data.local.SyncStatus
import com.example.data.local.UserPreferencesDataStore
import com.example.data.local.VitalTrackDatabase
import com.example.data.local.WaterIntakeEntity
import com.example.data.local.WeightEntryEntity
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

data class CloudSyncSummary(
  val uploadedCount: Int,
  val downloadedCount: Int,
  val syncedAtMillis: Long,
)

class FirestoreSyncRepository(
  private val context: Context,
  private val db: VitalTrackDatabase,
  private val prefs: UserPreferencesDataStore,
) {
  companion object {
    private const val TAG = "FirestoreSyncRepo"
  }

  private val firestore: FirebaseFirestore by lazy {
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    )
  }

  private fun requireUid(): String {
    val uid = runCatching { Firebase.auth.currentUser?.uid }.getOrNull()
    if (uid.isNullOrBlank()) {
      throw IllegalStateException("Google Sign-In required for cloud synchronization.")
    }
    return uid
  }

  private fun sanitizeDocId(raw: String): String =
    raw.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").ifBlank { "doc" }.take(120)

  suspend fun syncNow(expectedUid: String? = null): Result<CloudSyncSummary> {
    val uid =
      try {
        val current = requireUid()
        if (expectedUid != null && current != expectedUid) {
          throw IllegalStateException("Authenticated account changed during sync.")
        }
        current
      } catch (e: Exception) {
        return Result.failure(e)
      }

    var uploaded = 0
    var downloaded = 0
    val now = System.currentTimeMillis()

    return try {
      val userDoc = firestore.collection("users").document(uid)

      // 1. Sync Profile Settings
      val profile = prefs.getCurrent()
      val profilePath = "users/$uid/profile/settings"
      val profileDocRef = userDoc.collection("profile").document("settings")
      val existingProfile =
        try {
          profileDocRef.get().await()
        } catch (e: CancellationException) {
          throw e
        } catch (e: Exception) {
          handleFirestoreError(e, OperationType.GET, profilePath)
          null
        }
      val profileData =
        mutableMapOf<String, Any>(
          "userId" to uid,
          "displayName" to profile.displayName.trim().ifEmpty { "Explorer" }.take(60),
          "dailyStepGoal" to profile.dailyStepGoal.coerceIn(1000, 50000),
          "dailyWaterGoalMl" to profile.dailyWaterGoalMl.coerceIn(500, 6000),
          "useImperialUnits" to profile.useImperialUnits,
          "languageCode" to if (profile.languageCode == "bn") "bn" else "en",
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      if (existingProfile == null || !existingProfile.exists()) {
        profileData["createdAt"] = FieldValue.serverTimestamp()
      }
      try {
        profileDocRef.set(profileData, SetOptions.merge()).await()
        uploaded++
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        handleFirestoreError(e, OperationType.WRITE, profilePath)
        throw e
      }

      // 2. Upload pending Daily Steps
      val stepDao = db.dailyStepDao()
      val localSteps = stepDao.getAllSnapshot()
      val stepsColl = userDoc.collection("daily_steps")
      for (step in localSteps.filter { it.syncStatus != SyncStatus.SYNCED }) {
        val safeDate = sanitizeDocId(step.dateIso)
        val docPath = "users/$uid/daily_steps/$safeDate"
        val existing = stepsColl.document(safeDate).get().await()
        val payload =
          mutableMapOf<String, Any>(
            "userId" to uid,
            "dateIso" to step.dateIso.take(10),
            "steps" to step.steps.coerceAtLeast(0),
            "goal" to step.goal.coerceAtLeast(1000),
            "distanceMeters" to step.distanceMeters.coerceAtLeast(0.0),
            "caloriesKcal" to step.caloriesKcal.coerceAtLeast(0.0),
            "activeMinutes" to step.activeMinutes.coerceAtLeast(0),
            "source" to step.source.ifBlank { "SENSOR_COUNTER" }.take(40),
            "clientUpdatedAtMillis" to step.updatedAtMillis.coerceAtLeast(0L),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        if (!existing.exists()) {
          payload["createdAt"] = FieldValue.serverTimestamp()
        }
        try {
          stepsColl.document(safeDate).set(payload, SetOptions.merge()).await()
          stepDao.upsert(step.copy(syncStatus = SyncStatus.SYNCED))
          uploaded++
        } catch (e: CancellationException) {
          throw e
        } catch (e: Exception) {
          handleFirestoreError(e, OperationType.WRITE, docPath)
          throw e
        }
      }

      // Download remote Daily Steps (conflict resolution: keep higher step count or newer timestamp)
      val remoteStepsSnap =
        try {
          stepsColl.get().await()
        } catch (e: CancellationException) {
          throw e
        } catch (e: Exception) {
          handleFirestoreError(e, OperationType.LIST, "users/$uid/daily_steps")
          throw e
        }
      for (doc in remoteStepsSnap.documents) {
        val data = doc.data ?: continue
        val dateIso = data["dateIso"] as? String ?: doc.id
        val remoteSteps = (data["steps"] as? Number)?.toInt() ?: 0
        val remoteGoal = (data["goal"] as? Number)?.toInt() ?: 8000
        val remoteDist = (data["distanceMeters"] as? Number)?.toDouble() ?: 0.0
        val remoteKcal = (data["caloriesKcal"] as? Number)?.toDouble() ?: 0.0
        val remoteMin = (data["activeMinutes"] as? Number)?.toInt() ?: 0
        val remoteSource = data["source"] as? String ?: "SENSOR_COUNTER"
        val remoteUpdated = (data["clientUpdatedAtMillis"] as? Number)?.toLong() ?: 0L
        val local = stepDao.getStepsForDate(dateIso)
        if (local == null || remoteUpdated > local.updatedAtMillis || remoteSteps > local.steps) {
          stepDao.upsert(
            DailyStepEntity(
              dateIso = dateIso,
              steps = maxOf(remoteSteps, local?.steps ?: 0),
              goal = remoteGoal,
              distanceMeters = maxOf(remoteDist, local?.distanceMeters ?: 0.0),
              caloriesKcal = maxOf(remoteKcal, local?.caloriesKcal ?: 0.0),
              activeMinutes = maxOf(remoteMin, local?.activeMinutes ?: 0),
              source = remoteSource,
              updatedAtMillis = maxOf(remoteUpdated, local?.updatedAtMillis ?: 0L),
              syncStatus = SyncStatus.SYNCED,
            )
          )
          downloaded++
        }
      }

      // 3. Upload pending GPS Activity Summaries (safe metadata without duplicates)
      val gpsDao = db.gpsSessionDao()
      val gpsColl = userDoc.collection("gps_sessions")
      for (session in gpsDao.getAllSnapshot().filter { it.syncStatus != SyncStatus.SYNCED }) {
        val safeId = sanitizeDocId(session.id)
        val docPath = "users/$uid/gps_sessions/$safeId"
        val existing = gpsColl.document(safeId).get().await()
        val payload =
          mutableMapOf<String, Any>(
            "userId" to uid,
            "activityType" to session.activityType,
            "title" to session.title.ifBlank { session.activityType }.take(100),
            "dateIso" to session.dateIso.take(10),
            "startTimeMillis" to session.startTimeMillis.coerceAtLeast(0L),
            "endTimeMillis" to session.endTimeMillis.coerceAtLeast(session.startTimeMillis.coerceAtLeast(0L)),
            "activeDurationMillis" to session.activeDurationMillis.coerceAtLeast(0L),
            "distanceMeters" to session.distanceMeters.coerceAtLeast(0.0),
            "avgSpeedMps" to session.avgSpeedMps.coerceAtLeast(0.0),
            "caloriesKcal" to session.caloriesKcal.coerceAtLeast(0.0),
            "steps" to session.steps.coerceAtLeast(0),
            "notes" to session.notes.take(400),
            "clientUpdatedAtMillis" to session.updatedAtMillis.coerceAtLeast(0L),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        if (!existing.exists()) {
          payload["createdAt"] = FieldValue.serverTimestamp()
        }
        try {
          gpsColl.document(safeId).set(payload, SetOptions.merge()).await()
          gpsDao.upsert(session.copy(syncStatus = SyncStatus.SYNCED))
          uploaded++
        } catch (e: CancellationException) {
          throw e
        } catch (e: Exception) {
          handleFirestoreError(e, OperationType.WRITE, docPath)
          throw e
        }
      }

      // 4. Upload pending Health Logs (Water, Weight, Sleep, Journal)
      val healthDao = db.healthRecordDao()
      val healthColl = userDoc.collection("health_logs")
      for (water in healthDao.getWaterSnapshot().filter { it.syncStatus != SyncStatus.SYNCED }) {
        val docId = sanitizeDocId("water_${water.id}")
        val existing = healthColl.document(docId).get().await()
        val payload =
          mutableMapOf<String, Any>(
            "userId" to uid,
            "logType" to "WATER",
            "dateIso" to water.dateIso,
            "numericValue" to water.amountMl.toDouble(),
            "secondaryValue" to 0.0,
            "label" to "Water Intake",
            "notes" to "",
            "recordedAtMillis" to water.recordedAtMillis.coerceAtLeast(0L),
            "clientUpdatedAtMillis" to water.updatedAtMillis.coerceAtLeast(0L),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        if (!existing.exists()) payload["createdAt"] = FieldValue.serverTimestamp()
        healthColl.document(docId).set(payload, SetOptions.merge()).await()
        healthDao.insertWater(water.copy(syncStatus = SyncStatus.SYNCED))
        uploaded++
      }

      for (weight in healthDao.getWeightSnapshot().filter { it.syncStatus != SyncStatus.SYNCED }) {
        val docId = sanitizeDocId("weight_${weight.id}")
        val existing = healthColl.document(docId).get().await()
        val payload =
          mutableMapOf<String, Any>(
            "userId" to uid,
            "logType" to "WEIGHT",
            "dateIso" to weight.dateIso,
            "numericValue" to weight.weightKg,
            "secondaryValue" to weight.bmi,
            "label" to weight.source.take(40),
            "notes" to weight.notes.take(400),
            "recordedAtMillis" to weight.recordedAtMillis.coerceAtLeast(0L),
            "clientUpdatedAtMillis" to weight.updatedAtMillis.coerceAtLeast(0L),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        if (!existing.exists()) payload["createdAt"] = FieldValue.serverTimestamp()
        healthColl.document(docId).set(payload, SetOptions.merge()).await()
        healthDao.insertWeight(weight.copy(syncStatus = SyncStatus.SYNCED))
        uploaded++
      }

      for (sleep in healthDao.getSleepRecordsSnapshot().filter { it.syncStatus != SyncStatus.SYNCED }) {
        val docId = sanitizeDocId("sleep_${sleep.id}")
        val existing = healthColl.document(docId).get().await()
        val payload =
          mutableMapOf<String, Any>(
            "userId" to uid,
            "logType" to "SLEEP",
            "dateIso" to sleep.dateIso,
            "numericValue" to sleep.durationMinutes.toDouble(),
            "secondaryValue" to sleep.qualityScore.toDouble(),
            "label" to sleep.source.take(40),
            "notes" to sleep.notes.take(400),
            "recordedAtMillis" to sleep.endTimeMillis.coerceAtLeast(0L),
            "clientUpdatedAtMillis" to sleep.updatedAtMillis.coerceAtLeast(0L),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        if (!existing.exists()) payload["createdAt"] = FieldValue.serverTimestamp()
        healthColl.document(docId).set(payload, SetOptions.merge()).await()
        healthDao.upsertSleep(sleep.copy(syncStatus = SyncStatus.SYNCED))
        uploaded++
      }

      for (journal in healthDao.getJournalSnapshot().filter { it.syncStatus != SyncStatus.SYNCED }) {
        val docId = sanitizeDocId("journal_${journal.id}")
        val existing = healthColl.document(docId).get().await()
        val payload =
          mutableMapOf<String, Any>(
            "userId" to uid,
            "logType" to "JOURNAL",
            "dateIso" to journal.dateIso,
            "numericValue" to journal.energyLevel.toDouble(),
            "secondaryValue" to 0.0,
            "label" to journal.mood.take(40),
            "notes" to journal.note.take(400),
            "recordedAtMillis" to journal.createdAtMillis.coerceAtLeast(0L),
            "clientUpdatedAtMillis" to journal.updatedAtMillis.coerceAtLeast(0L),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        if (!existing.exists()) payload["createdAt"] = FieldValue.serverTimestamp()
        healthColl.document(docId).set(payload, SetOptions.merge()).await()
        healthDao.upsertJournal(journal.copy(syncStatus = SyncStatus.SYNCED))
        uploaded++
      }

      // Restore missing water/weight/sleep/journal logs from cloud safely without duplicates
      val remoteHealthSnap = healthColl.get().await()
      val existingWaterIds = healthDao.getWaterSnapshot().map { "water_${it.id}" }.toSet()
      val existingWeightIds = healthDao.getWeightSnapshot().map { "weight_${it.id}" }.toSet()
      val existingSleepIds = healthDao.getSleepRecordsSnapshot().map { "sleep_${it.id}" }.toSet()
      val existingJournalIds = healthDao.getJournalSnapshot().map { "journal_${it.id}" }.toSet()

      for (doc in remoteHealthSnap.documents) {
        val data = doc.data ?: continue
        val logType = data["logType"] as? String ?: continue
        val dateIso = data["dateIso"] as? String ?: WellnessCalculators.todayIso()
        val num1 = (data["numericValue"] as? Number)?.toDouble() ?: 0.0
        val num2 = (data["secondaryValue"] as? Number)?.toDouble() ?: 0.0
        val label = data["label"] as? String ?: ""
        val notes = data["notes"] as? String ?: ""
        val recordedAt = (data["recordedAtMillis"] as? Number)?.toLong() ?: now
        val updatedAt = (data["clientUpdatedAtMillis"] as? Number)?.toLong() ?: now

        when (logType) {
          "WATER" ->
            if (doc.id !in existingWaterIds) {
              healthDao.insertWater(
                WaterIntakeEntity(
                  id = doc.id.removePrefix("water_"),
                  amountMl = num1.toInt(),
                  recordedAtMillis = recordedAt,
                  dateIso = dateIso,
                  updatedAtMillis = updatedAt,
                  syncStatus = SyncStatus.SYNCED,
                )
              )
              downloaded++
            }
          "WEIGHT" ->
            if (doc.id !in existingWeightIds) {
              healthDao.insertWeight(
                WeightEntryEntity(
                  id = doc.id.removePrefix("weight_"),
                  weightKg = num1,
                  bmi = num2,
                  recordedAtMillis = recordedAt,
                  dateIso = dateIso,
                  source = label.ifEmpty { "CLOUD" },
                  notes = notes,
                  updatedAtMillis = updatedAt,
                  syncStatus = SyncStatus.SYNCED,
                )
              )
              downloaded++
            }
          "SLEEP" ->
            if (doc.id !in existingSleepIds) {
              val dur = num1.toInt()
              healthDao.upsertSleep(
                SleepRecordEntity(
                  id = doc.id.removePrefix("sleep_"),
                  startTimeMillis = recordedAt - dur * 60_000L,
                  endTimeMillis = recordedAt,
                  durationMinutes = dur,
                  qualityScore = num2.toInt(),
                  dateIso = dateIso,
                  source = label.ifEmpty { "CLOUD" },
                  notes = notes,
                  updatedAtMillis = updatedAt,
                  syncStatus = SyncStatus.SYNCED,
                )
              )
              downloaded++
            }
          "JOURNAL" ->
            if (doc.id !in existingJournalIds) {
              healthDao.upsertJournal(
                JournalEntryEntity(
                  id = doc.id.removePrefix("journal_"),
                  dateIso = dateIso,
                  mood = label.ifEmpty { "CALM" },
                  energyLevel = num1.toInt().coerceIn(1, 5),
                  note = notes,
                  createdAtMillis = recordedAt,
                  updatedAtMillis = updatedAt,
                  syncStatus = SyncStatus.SYNCED,
                )
              )
              downloaded++
            }
        }
      }

      prefs.setLastCloudSyncMillis(now)
      Result.success(CloudSyncSummary(uploaded, downloaded, now))
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Log.e(TAG, "Cloud sync failed; local data preserved", e)
      Result.failure(e)
    }
  }

  suspend fun deleteAllCloudData(): Result<Unit> {
    return try {
      val uid = requireUid()
      val userDoc = firestore.collection("users").document(uid)
      val collections = listOf("daily_steps", "gps_sessions", "health_logs", "profile")
      for (col in collections) {
        val snap = userDoc.collection(col).get().await()
        for (doc in snap.documents) {
          doc.reference.delete().await()
        }
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, "users/cloud_purge")
      Result.failure(e)
    }
  }
}
