package com.example.data.repository

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.health.WellnessAdvancedCalculators
import com.example.data.health.WellnessCalculators
import com.example.data.local.DailyStepEntity
import com.example.data.local.GearItemEntity
import com.example.data.local.GpsActivitySessionEntity
import com.example.data.local.GpsPoint
import com.example.data.local.HabitCompletionEntity
import com.example.data.local.HabitEntity
import com.example.data.local.HeartRateRecordEntity
import com.example.data.local.JournalEntryEntity
import com.example.data.local.NutritionLogEntity
import com.example.data.local.SleepRecordEntity
import com.example.data.local.TaskCategoryEntity
import com.example.data.local.UserPreferencesDataStore
import com.example.data.local.UserProfilePreferences
import com.example.data.local.VitalTrackDatabase
import com.example.data.local.VitalsRecordEntity
import com.example.data.local.WardrobeItemEntity
import com.example.data.local.WaterIntakeEntity
import com.example.data.local.WeightEntryEntity
import com.example.data.local.WellnessTaskEntity
import com.example.data.local.WorkoutPlanEntity
import com.example.data.local.WorkoutSessionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.time.Instant

@Serializable
data class VitalTrackBackupPayload(
  val schemaVersion: Int = 2,
  val exportedAtMillis: Long,
  val profile: UserProfilePreferences,
  val dailySteps: List<DailyStepEntity>,
  val gpsSessions: List<GpsActivitySessionEntity>,
  val heartRates: List<HeartRateRecordEntity>,
  val sleepRecords: List<SleepRecordEntity>,
  val waterIntakes: List<WaterIntakeEntity>,
  val weightEntries: List<WeightEntryEntity>,
  val journalEntries: List<JournalEntryEntity>,
  val customPlans: List<WorkoutPlanEntity>,
  val workoutSessions: List<WorkoutSessionEntity>,
  val habits: List<HabitEntity>,
  val habitCompletions: List<HabitCompletionEntity>,
  val wellnessTasks: List<WellnessTaskEntity> = emptyList(),
  val taskCategories: List<TaskCategoryEntity> = emptyList(),
  val wardrobeItems: List<WardrobeItemEntity> = emptyList(),
  val nutritionLogs: List<NutritionLogEntity> = emptyList(),
  val vitalsRecords: List<VitalsRecordEntity> = emptyList(),
  val gearItems: List<GearItemEntity> = emptyList(),
)

class ExportBackupRepository(
  private val context: Context,
  private val db: VitalTrackDatabase,
  private val prefs: UserPreferencesDataStore,
) {
  private val json = Json {
    prettyPrint = true
    ignoreUnknownKeys = true
  }

  private fun exportsDir(): File = File(context.cacheDir, "exports").apply { mkdirs() }

  private fun uriForFile(file: File): Uri =
    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

  suspend fun exportActivityCsv(): Result<Uri> =
    withContext(Dispatchers.IO) {
      runCatching {
        val steps = db.dailyStepDao().getAllSnapshot()
        val sessions = db.gpsSessionDao().getAllSnapshot()
        val workouts = db.workoutDao().getWorkoutSessionsSnapshot()
        val file = File(exportsDir(), "vitaltrack_activity_${WellnessCalculators.todayIso()}.csv")
        file.bufferedWriter().use { w ->
          w.appendLine("RecordType,Date,Title,DurationSec,DistanceMeters,Steps,CaloriesKcal,Source")
          steps.forEach { s ->
            w.appendLine(
              "DAILY_STEPS,${s.dateIso},Daily Steps,${s.activeMinutes * 60},${s.distanceMeters},${s.steps},${s.caloriesKcal},${s.source}"
            )
          }
          sessions.forEach { g ->
            w.appendLine(
              "GPS_${g.activityType},${g.dateIso},\"${g.title.replace("\"", "'")}\",${g.activeDurationMillis / 1000},${g.distanceMeters},${g.steps},${g.caloriesKcal},GPS"
            )
          }
          workouts.forEach { wk ->
            w.appendLine(
              "WORKOUT_${wk.category},${wk.dateIso},\"${wk.title.replace("\"", "'")}\",${wk.durationSeconds},0,0,${wk.caloriesKcal},LOCAL"
            )
          }
        }
        uriForFile(file)
      }
    }

  suspend fun exportWeeklyPdfReport(): Result<Uri> =
    withContext(Dispatchers.IO) {
      runCatching {
        val profile = prefs.getCurrent()
        val last7Days = WellnessCalculators.lastNDaysIso(7)
        val stepsMap = db.dailyStepDao().getAllSnapshot().associateBy { it.dateIso }
        val waterList = db.healthRecordDao().getWaterSnapshot().filter { it.dateIso in last7Days }
        val gpsList = db.gpsSessionDao().getAllSnapshot().filter { it.dateIso in last7Days }
        val workoutList = db.workoutDao().getWorkoutSessionsSnapshot().filter { it.dateIso in last7Days }

        val pdf = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdf.startPage(pageInfo)
        val canvas = page.canvas

        val titlePaint = Paint().apply { textSize = 22f; isFakeBoldText = true; color = 0xFF526B57.toInt() }
        val headingPaint = Paint().apply { textSize = 14f; isFakeBoldText = true; color = 0xFF202820.toInt() }
        val bodyPaint = Paint().apply { textSize = 12f; color = 0xFF202820.toInt() }
        val mutedPaint = Paint().apply { textSize = 10f; color = 0xFF545E54.toInt() }

        var y = 52f
        canvas.drawText("VITALTRACK — Weekly Wellness Report", 40f, y, titlePaint)
        y += 22f
        canvas.drawText(
          "Profile: ${profile.displayName}  •  Generated: ${WellnessCalculators.todayIso()}",
          40f,
          y,
          mutedPaint,
        )
        y += 28f

        val totalSteps = last7Days.sumOf { stepsMap[it]?.steps ?: 0 }
        val totalDist = last7Days.sumOf { stepsMap[it]?.distanceMeters ?: 0.0 }
        val totalWater = waterList.sumOf { it.amountMl }

        canvas.drawText("7-Day Summary", 40f, y, headingPaint)
        y += 20f
        canvas.drawText("• Total Steps: $totalSteps (Avg ${totalSteps / 7}/day)", 48f, y, bodyPaint)
        y += 18f
        canvas.drawText(
          "• Estimated Step Distance: ${WellnessCalculators.formatDistance(totalDist, profile.useImperialUnits)}",
          48f,
          y,
          bodyPaint,
        )
        y += 18f
        canvas.drawText(
          "• Total Hydration Logged: ${WellnessCalculators.formatVolume(totalWater, profile.useImperialUnits)}",
          48f,
          y,
          bodyPaint,
        )
        y += 18f
        canvas.drawText("• Recorded GPS Sessions: ${gpsList.size}", 48f, y, bodyPaint)
        y += 18f
        canvas.drawText("• Completed Workouts: ${workoutList.size}", 48f, y, bodyPaint)
        y += 30f

        canvas.drawText("Daily Breakdown", 40f, y, headingPaint)
        y += 20f
        last7Days.forEach { day ->
          val st = stepsMap[day]?.steps ?: 0
          val wMl = waterList.filter { it.dateIso == day }.sumOf { it.amountMl }
          canvas.drawText("$day  —  Steps: $st  |  Water: ${wMl} ml", 48f, y, bodyPaint)
          y += 18f
        }

        y += 28f
        canvas.drawText(
          "Informational only: VITALTRACK is not a medical diagnostic device.",
          40f,
          y,
          mutedPaint,
        )

        pdf.finishPage(page)
        val file = File(exportsDir(), "vitaltrack_report_${WellnessCalculators.todayIso()}.pdf")
        FileOutputStream(file).use { pdf.writeTo(it) }
        pdf.close()
        uriForFile(file)
      }
    }

  suspend fun createBackupJsonString(): String =
    withContext(Dispatchers.IO) {
      val payload =
        VitalTrackBackupPayload(
          exportedAtMillis = System.currentTimeMillis(),
          profile = prefs.getCurrent(),
          dailySteps = db.dailyStepDao().getAllSnapshot(),
          gpsSessions = db.gpsSessionDao().getAllSnapshot(),
          heartRates = db.healthRecordDao().getHeartRatesSnapshot(),
          sleepRecords = db.healthRecordDao().getSleepRecordsSnapshot(),
          waterIntakes = db.healthRecordDao().getWaterSnapshot(),
          weightEntries = db.healthRecordDao().getWeightSnapshot(),
          journalEntries = db.healthRecordDao().getJournalSnapshot(),
          customPlans = db.workoutDao().getWorkoutPlansSnapshot().filter { it.isCustom },
          workoutSessions = db.workoutDao().getWorkoutSessionsSnapshot(),
          habits = db.habitDao().getHabitsSnapshot(),
          habitCompletions = db.habitDao().getCompletionsSnapshot(),
          wellnessTasks = db.wellnessTaskDao().getAllTasksSnapshot(),
          taskCategories = db.taskCategoryDao().getCategoriesSnapshot(),
          wardrobeItems = db.wardrobeDao().getSnapshot(),
          nutritionLogs = db.nutritionDao().getSnapshot(),
          vitalsRecords = db.vitalsDao().getSnapshot(),
          gearItems = db.gearDao().getSnapshot(),
        )
      json.encodeToString(payload)
    }

  suspend fun writeAutoBackupSnapshot(): Result<File> =
    withContext(Dispatchers.IO) {
      runCatching {
        val raw = createBackupJsonString()
        val autoDir = File(context.filesDir, "auto_backups").apply { mkdirs() }
        val file = File(autoDir, "vitaltrack_autobackup_${WellnessCalculators.todayIso()}.json")
        file.writeText(raw)
        prefs.setLastAutoBackupMillis(System.currentTimeMillis())
        file
      }
    }

  suspend fun exportJsonBackupFile(): Result<Uri> =
    withContext(Dispatchers.IO) {
      runCatching {
        val raw = createBackupJsonString()
        val file = File(exportsDir(), "vitaltrack_backup_${WellnessCalculators.todayIso()}.json")
        file.writeText(raw)
        uriForFile(file)
      }
    }

  suspend fun restoreFromBackupJson(rawJson: String): Result<Int> =
    withContext(Dispatchers.IO) {
      runCatching {
        val payload = json.decodeFromString<VitalTrackBackupPayload>(rawJson)
        prefs.restoreFromBackup(payload.profile)
        if (payload.dailySteps.isNotEmpty()) db.dailyStepDao().upsertAll(payload.dailySteps)
        if (payload.gpsSessions.isNotEmpty()) db.gpsSessionDao().upsertAll(payload.gpsSessions)
        if (payload.heartRates.isNotEmpty()) db.healthRecordDao().upsertHeartRates(payload.heartRates)
        if (payload.sleepRecords.isNotEmpty()) db.healthRecordDao().upsertSleepAll(payload.sleepRecords)
        if (payload.waterIntakes.isNotEmpty()) db.healthRecordDao().upsertWaterAll(payload.waterIntakes)
        if (payload.weightEntries.isNotEmpty()) db.healthRecordDao().upsertWeightAll(payload.weightEntries)
        if (payload.journalEntries.isNotEmpty()) db.healthRecordDao().upsertJournalAll(payload.journalEntries)
        if (payload.customPlans.isNotEmpty()) db.workoutDao().upsertWorkoutPlans(payload.customPlans)
        if (payload.workoutSessions.isNotEmpty()) db.workoutDao().upsertWorkoutSessions(payload.workoutSessions)
        if (payload.habits.isNotEmpty()) db.habitDao().upsertHabits(payload.habits)
        if (payload.habitCompletions.isNotEmpty()) db.habitDao().upsertCompletions(payload.habitCompletions)
        if (payload.taskCategories.isNotEmpty()) db.taskCategoryDao().upsertCategories(payload.taskCategories)
        if (payload.wellnessTasks.isNotEmpty()) db.wellnessTaskDao().upsertTasks(payload.wellnessTasks)
        if (payload.wardrobeItems.isNotEmpty()) db.wardrobeDao().upsertAll(payload.wardrobeItems)
        if (payload.nutritionLogs.isNotEmpty()) db.nutritionDao().upsertAll(payload.nutritionLogs)
        if (payload.vitalsRecords.isNotEmpty()) db.vitalsDao().upsertAll(payload.vitalsRecords)
        if (payload.gearItems.isNotEmpty()) db.gearDao().upsertAll(payload.gearItems)
        payload.dailySteps.size +
          payload.gpsSessions.size +
          payload.waterIntakes.size +
          payload.weightEntries.size +
          payload.sleepRecords.size +
          payload.workoutSessions.size +
          payload.wellnessTasks.size +
          payload.vitalsRecords.size +
          payload.nutritionLogs.size +
          payload.wardrobeItems.size
      }
    }

  suspend fun importCsvOrJson(rawText: String): Result<Int> =
    withContext(Dispatchers.IO) {
      if (rawText.trimStart().startsWith("{")) {
        restoreFromBackupJson(rawText)
      } else {
        runCatching {
          val profile = prefs.getCurrent()
          val (steps, tasks) =
            WellnessAdvancedCalculators.parseCsvImportLines(
              rawText,
              profile.strideLengthMeters,
              profile.weightKg,
            )
          if (steps.isNotEmpty()) db.dailyStepDao().upsertAll(steps)
          if (tasks.isNotEmpty()) db.wellnessTaskDao().upsertTasks(tasks)
          steps.size + tasks.size
        }
      }
    }

  suspend fun exportPhysicianSummaryPdf(): Result<Uri> =
    withContext(Dispatchers.IO) {
      runCatching {
        val profile = prefs.getCurrent()
        val bpRecords = db.vitalsDao().getSnapshot().filter { it.type == "BP" }.take(5)
        val glucoseRecords = db.vitalsDao().getSnapshot().filter { it.type == "GLUCOSE" }.take(5)
        val medRecords = db.vitalsDao().getSnapshot().filter { it.type == "MEDICATION" }.take(5)
        val labs = db.vitalsDao().getSnapshot().filter { it.type == "LAB" }.take(5)
        val sleepAvgMin =
          db.healthRecordDao().getSleepRecordsSnapshot().take(7).let { list ->
            if (list.isEmpty()) 0 else list.sumOf { it.durationMinutes } / list.size
          }

        val pdf = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdf.startPage(pageInfo)
        val canvas = page.canvas

        val titlePaint = Paint().apply { textSize = 20f; isFakeBoldText = true; color = 0xFF526B57.toInt() }
        val headingPaint = Paint().apply { textSize = 13f; isFakeBoldText = true; color = 0xFF202820.toInt() }
        val bodyPaint = Paint().apply { textSize = 11f; color = 0xFF202820.toInt() }
        val mutedPaint = Paint().apply { textSize = 9.5f; color = 0xFF545E54.toInt() }

        var y = 48f
        canvas.drawText("VITALTRACK — Physician Clinical Visit Summary", 40f, y, titlePaint)
        y += 20f
        canvas.drawText(
          "Patient: ${profile.displayName} (Age ${profile.age}, ${profile.heightCm.toInt()} cm, ${profile.weightKg} kg) • ${WellnessCalculators.todayIso()}",
          40f,
          y,
          mutedPaint,
        )
        y += 26f
        canvas.drawText("Baseline Lifestyle: Avg Sleep ${WellnessCalculators.formatMinutesToHours(sleepAvgMin)} • Step Goal ${profile.dailyStepGoal}", 40f, y, bodyPaint)
        y += 24f

        canvas.drawText("Recent Blood Pressure Readings", 40f, y, headingPaint)
        y += 16f
        if (bpRecords.isEmpty()) {
          canvas.drawText("• No BP logs recorded", 48f, y, bodyPaint); y += 16f
        } else {
          bpRecords.forEach { r ->
            val cls = WellnessAdvancedCalculators.classifyBloodPressure(r.primaryValue.toInt(), r.secondaryValue.toInt())
            canvas.drawText("• ${r.dateIso}: ${r.primaryValue.toInt()}/${r.secondaryValue.toInt()} mmHg ($cls)", 48f, y, bodyPaint)
            y += 15f
          }
        }
        y += 14f

        canvas.drawText("Recent Blood Glucose Logs", 40f, y, headingPaint)
        y += 16f
        if (glucoseRecords.isEmpty()) {
          canvas.drawText("• No glucose logs recorded", 48f, y, bodyPaint); y += 16f
        } else {
          glucoseRecords.forEach { r ->
            canvas.drawText("• ${r.dateIso}: ${r.primaryValue.toInt()} ${r.unit.ifBlank { "mg/dL" }} (${r.title})", 48f, y, bodyPaint)
            y += 15f
          }
        }
        y += 14f

        canvas.drawText("Active Medications & Refill Status", 40f, y, headingPaint)
        y += 16f
        if (medRecords.isEmpty()) {
          canvas.drawText("• None listed", 48f, y, bodyPaint); y += 16f
        } else {
          medRecords.forEach { r ->
            val refillTxt = if (r.refillRemaining >= 0) " (${r.refillRemaining} left)" else ""
            canvas.drawText("• ${r.title} — ${r.primaryValue} ${r.unit}$refillTxt", 48f, y, bodyPaint)
            y += 15f
          }
        }
        y += 14f

        canvas.drawText("Lab Panel Vault Notes", 40f, y, headingPaint)
        y += 16f
        if (labs.isEmpty()) {
          canvas.drawText("• None recorded", 48f, y, bodyPaint); y += 16f
        } else {
          labs.forEach { r ->
            canvas.drawText("• ${r.dateIso} ${r.title}: ${r.primaryValue} ${r.unit} (${r.notes.take(50)})", 48f, y, bodyPaint)
            y += 15f
          }
        }

        y += 28f
        canvas.drawText("Self-tracked wellness data for physician review; not a diagnostic instrument.", 40f, y, mutedPaint)
        pdf.finishPage(page)
        val file = File(exportsDir(), "vitaltrack_physician_summary_${WellnessCalculators.todayIso()}.pdf")
        FileOutputStream(file).use { pdf.writeTo(it) }
        pdf.close()
        uriForFile(file)
      }
    }

  suspend fun exportGpsSessionGpx(sessionId: String): Result<Uri> =
    withContext(Dispatchers.IO) {
      runCatching {
        val session =
          db.gpsSessionDao().getAllSnapshot().firstOrNull { it.id == sessionId }
            ?: error("GPS session not found")
        val points =
          runCatching { json.decodeFromString<List<GpsPoint>>(session.routePolylineJson) }
            .getOrDefault(emptyList())
        val xml = buildGpxXml(session, points)
        val safeDate = session.dateIso.replace(Regex("[^A-Za-z0-9_-]"), "_")
        val file = File(exportsDir(), "vitaltrack_route_${safeDate}_${session.id.take(8)}.gpx")
        file.writeText(xml)
        uriForFile(file)
      }
    }

  private fun buildGpxXml(session: GpsActivitySessionEntity, points: List<GpsPoint>): String {
    fun escapeXml(raw: String): String =
      raw.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")

    return buildString {
      appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
      appendLine(
        """<gpx version="1.1" creator="VITALTRACK" xmlns="http://www.topografix.com/GPX/1/1">"""
      )
      appendLine("  <metadata>")
      appendLine("    <name>${escapeXml(session.title)}</name>")
      appendLine("    <time>${Instant.ofEpochMilli(session.startTimeMillis)}</time>")
      appendLine("  </metadata>")
      appendLine("  <trk>")
      appendLine("    <name>${escapeXml(session.title)}</name>")
      appendLine("    <type>${escapeXml(session.activityType)}</type>")
      appendLine("    <trkseg>")
      points.forEach { pt ->
        appendLine("""      <trkpt lat="${pt.latitude}" lon="${pt.longitude}">""")
        appendLine("        <ele>${pt.altitudeMeters}</ele>")
        appendLine("        <time>${Instant.ofEpochMilli(pt.timestampMillis)}</time>")
        appendLine("      </trkpt>")
      }
      appendLine("    </trkseg>")
      appendLine("  </trk>")
      appendLine("</gpx>")
    }
  }

  fun createShareIntent(uri: Uri, mimeType: String, title: String): Intent {
    return Intent(Intent.ACTION_SEND).apply {
      type = mimeType
      putExtra(Intent.EXTRA_STREAM, uri)
      putExtra(Intent.EXTRA_SUBJECT, title)
      addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
  }
}
