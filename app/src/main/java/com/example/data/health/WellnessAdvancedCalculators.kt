package com.example.data.health

import com.example.data.local.DailyStepEntity
import com.example.data.local.GpsPoint
import com.example.data.local.HabitCompletionEntity
import com.example.data.local.SleepRecordEntity
import com.example.data.local.TaskPriority
import com.example.data.local.WardrobeItemEntity
import com.example.data.local.WeightEntryEntity
import com.example.data.local.WellnessTaskEntity
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale
import java.util.UUID
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

data class MacroTargets(
  val caloriesKcal: Int,
  val proteinG: Int,
  val carbsG: Int,
  val fatG: Int,
  val fiberG: Int,
)

data class GpsElevationStats(
  val elevationGainMeters: Double,
  val elevationLossMeters: Double,
  val maxGradePercent: Double,
  val minAltMeters: Double,
  val maxAltMeters: Double,
)

data class SleepCircadianMetrics(
  val avgDurationMinutes: Int,
  val weeklyDebtMinutes: Int,
  val consistencyScore: Int,
)

data class SeasonalPaletteProfile(
  val seasonName: String,
  val swatches: List<Pair<String, String>>,
  val tailoringGuidance: String,
)

object WellnessAdvancedCalculators {

  // #34 Epley 1-Rep Max
  fun estimateOneRepMaxKg(weightKg: Double, reps: Int): Double {
    val cleanW = weightKg.coerceAtLeast(0.0)
    val cleanR = reps.coerceIn(1, 30)
    if (cleanR == 1) return cleanW
    return cleanW * (1.0 + cleanR / 30.0)
  }

  // #39 Resting HR / Cooper VO2 Max estimator
  fun estimateVo2Max(age: Int, restingHrBpm: Int): Double {
    val maxHr = (220 - age.coerceIn(12, 95)).toDouble()
    val rhr = restingHrBpm.coerceIn(38, 110).toDouble()
    return (15.3 * (maxHr / rhr)).coerceIn(20.0, 82.0)
  }

  // #40 Acute:Chronic Workload Ratio (ACWR)
  fun calculateAcwr(last7DaysMinutes: Int, last28DaysMinutes: Int): Pair<Double, String> {
    val chronicWeekly = (last28DaysMinutes / 4.0).coerceAtLeast(15.0)
    val ratio = (last7DaysMinutes / chronicWeekly).coerceIn(0.0, 3.0)
    val status =
      when {
        ratio < 0.8 -> "Undertraining"
        ratio <= 1.3 -> "Optimal Sweet Spot"
        ratio <= 1.5 -> "High Load Caution"
        else -> "Overreaching Risk"
      }
    return ratio to status
  }

  // #36 Stride length calibration from known distance
  fun calibrateStrideMeters(steps: Int, distanceMeters: Double): Double =
    if (steps <= 0 || distanceMeters <= 0.0) 0.75
    else (distanceMeters / steps).coerceIn(0.35, 1.35)

  // #43 Elevation Gain / Loss / Grade %
  fun calculateGpsElevationStats(points: List<GpsPoint>): GpsElevationStats {
    if (points.size < 2) return GpsElevationStats(0.0, 0.0, 0.0, 0.0, 0.0)
    var gain = 0.0
    var loss = 0.0
    var maxGrade = 0.0
    var minAlt = points.first().altitudeMeters
    var maxAlt = points.first().altitudeMeters
    for (i in 1 until points.size) {
      val prev = points[i - 1]
      val curr = points[i]
      val dAlt = curr.altitudeMeters - prev.altitudeMeters
      if (dAlt > 0.4) gain += dAlt
      if (dAlt < -0.4) loss += -dAlt
      val horiz = WellnessCalculators.haversineMeters(prev, curr).coerceAtLeast(1.0)
      val grade = abs(dAlt / horiz) * 100.0
      if (grade in 0.0..45.0 && grade > maxGrade) maxGrade = grade
      if (curr.altitudeMeters < minAlt) minAlt = curr.altitudeMeters
      if (curr.altitudeMeters > maxAlt) maxAlt = curr.altitudeMeters
    }
    return GpsElevationStats(gain, loss, maxGrade, minAlt, maxAlt)
  }

  // #45 Round-trip Loop Route Waypoints generator around a base coordinate
  fun generateLoopRoutePoints(
    centerLat: Double,
    centerLon: Double,
    targetKm: Double,
  ): List<GpsPoint> {
    val radiusMeters = (targetKm.coerceIn(1.0, 25.0) * 1000.0) / (2.0 * Math.PI)
    val dLatPerM = 1.0 / 111_320.0
    val dLonPerM = 1.0 / (111_320.0 * cos(Math.toRadians(centerLat)).coerceAtLeast(0.2))
    val now = System.currentTimeMillis()
    return (0..12).map { idx ->
      val angle = (idx / 12.0) * 2.0 * Math.PI
      val lat = centerLat + sin(angle) * radiusMeters * dLatPerM
      val lon = centerLon + (cos(angle) - 1.0) * radiusMeters * dLonPerM
      GpsPoint(
        latitude = lat,
        longitude = lon,
        altitudeMeters = 20.0 + sin(angle * 2.0) * 8.0,
        timestampMillis = now + idx * 60_000L,
      )
    }
  }

  // #51 Daily Macro Targets from TDEE & Goal
  fun calculateMacroTargets(tdeeKcal: Int, weightKg: Double, goalMode: String): MacroTargets {
    val targetCal =
      when (goalMode.uppercase(Locale.US)) {
        "CUT" -> (tdeeKcal - 400).coerceAtLeast(1300)
        "BULK" -> tdeeKcal + 300
        else -> tdeeKcal
      }
    val proteinG = (weightKg.coerceIn(35.0, 200.0) * 1.8).roundToInt()
    val fatG = ((targetCal * 0.27) / 9.0).roundToInt()
    val remainingCal = (targetCal - (proteinG * 4) - (fatG * 9)).coerceAtLeast(200)
    val carbsG = (remainingCal / 4.0).roundToInt()
    val fiberG = ((targetCal / 1000.0) * 14.0).roundToInt().coerceIn(20, 55)
    return MacroTargets(targetCal, proteinG, carbsG, fatG, fiberG)
  }

  // #54 Sweat-loss Electrolyte & Hydration Offset
  fun calculateElectrolyteHydrationOffset(
    baseWaterMl: Int,
    ambientTempCelsius: Double,
    workoutMinutes: Int,
  ): Pair<Int, Int> {
    val heatExtraMl = ((ambientTempCelsius - 22.0).coerceAtLeast(0.0) * 45.0).roundToInt()
    val sweatExtraMl = workoutMinutes.coerceIn(0, 240) * 11
    val totalWaterMl = (baseWaterMl + heatExtraMl + sweatExtraMl).coerceIn(1500, 6500)
    val sodiumMg = ((heatExtraMl + sweatExtraMl) * 0.55).roundToInt()
    return totalWaterMl to sodiumMg
  }

  // #57 Caffeine half-life (~5h) remaining at bedtime
  fun estimateRemainingCaffeineMg(
    doses: List<Pair<Int, Long>>,
    targetTimeMillis: Long,
  ): Double {
    val halfLifeHours = 5.0
    return doses.sumOf { (mg, timeMillis) ->
      val hoursElapsed = ((targetTimeMillis - timeMillis) / 3_600_000.0).coerceAtLeast(0.0)
      if (timeMillis > targetTimeMillis) 0.0 else mg * 0.5.pow(hoursElapsed / halfLifeHours)
    }
  }

  // #62 Sleep Debt & Circadian Consistency Score
  fun calculateSleepCircadianMetrics(
    records: List<SleepRecordEntity>,
    targetMinutes: Int,
  ): SleepCircadianMetrics {
    val recent = records.take(7)
    if (recent.isEmpty()) return SleepCircadianMetrics(0, 0, 100)
    val avgDur = recent.sumOf { it.durationMinutes } / recent.size
    val debt = recent.sumOf { (targetMinutes - it.durationMinutes).coerceAtLeast(0) }
    val bedtimeMinutes =
      recent.map {
        val z = Instant.ofEpochMilli(it.startTimeMillis).atZone(ZoneId.systemDefault())
        val m = z.hour * 60 + z.minute
        if (m < 720) m + 1440 else m
      }
    val meanBed = bedtimeMinutes.average()
    val stdDev = sqrt(bedtimeMinutes.map { (it - meanBed).pow(2) }.average())
    val consistency = (100.0 - (stdDev * 0.7)).roundToInt().coerceIn(20, 100)
    return SleepCircadianMetrics(avgDur, debt, consistency)
  }

  // #63 Morning Readiness Score (0..100)
  fun calculateReadinessScore(
    sleepMinutes: Int,
    sleepGoalMinutes: Int,
    restingHr: Int,
    sorenessLevel1To5: Int,
    moodLevel1To5: Int,
  ): Int {
    val sleepPart = ((sleepMinutes.toDouble() / sleepGoalMinutes.coerceAtLeast(300)) * 42.0).coerceIn(0.0, 45.0)
    val rhrPart = ((85 - restingHr.coerceIn(42, 95)) * 0.7).coerceIn(5.0, 30.0)
    val recoveryPart = ((6 - sorenessLevel1To5.coerceIn(1, 5)) * 3.0) + (moodLevel1To5.coerceIn(1, 5) * 2.5)
    return (sleepPart + rhrPart + recoveryPart).roundToInt().coerceIn(15, 100)
  }

  // #75 Wellness XP Level
  fun xpToLevelProgress(totalXp: Int): Triple<Int, Int, Int> {
    val level = (totalXp / 250) + 1
    val currentInLevel = totalXp % 250
    return Triple(level, currentInLevel, 250)
  }

  // #77 Day-of-Week Habit Consistency Analytics
  fun habitCompletionsByWeekday(completions: List<HabitCompletionEntity>): Map<String, Int> {
    val counts = linkedMapOf<String, Int>()
    DayOfWeek.entries.forEach { d ->
      counts[d.getDisplayName(TextStyle.SHORT, Locale.US)] = 0
    }
    completions.forEach { c ->
      runCatching {
        val d = LocalDate.parse(c.dateIso).dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.US)
        counts[d] = (counts[d] ?: 0) + 1
      }
    }
    return counts
  }

  // #81 Blood Pressure AHA Classification
  fun classifyBloodPressure(systolic: Int, diastolic: Int): String =
    when {
      systolic >= 180 || diastolic >= 120 -> "Hypertensive Crisis"
      systolic >= 140 || diastolic >= 90 -> "Stage 2 Elevated"
      systolic >= 130 || diastolic >= 80 -> "Stage 1 Elevated"
      systolic >= 120 && diastolic < 80 -> "Slightly Elevated"
      systolic < 90 || diastolic < 60 -> "Low Range"
      else -> "Normal Range"
    }

  // #83 Waist-to-Height Ratio
  fun waistToHeightRatio(waistCm: Double, heightCm: Double): Pair<Double, String> {
    val ratio = (waistCm / heightCm.coerceAtLeast(100.0)).coerceIn(0.25, 1.0)
    val label =
      when {
        ratio < 0.40 -> "Lean"
        ratio < 0.50 -> "Healthy Proportion"
        ratio < 0.58 -> "Moderate Central Carry"
        else -> "High Ratio Focus"
      }
    return ratio to label
  }

  // #89 7-Day EMA Weight Plateau Smoothing
  fun calculateWeightEmaSeries(entriesChronological: List<WeightEntryEntity>): List<Pair<String, Double>> {
    if (entriesChronological.isEmpty()) return emptyList()
    val alpha = 2.0 / (7.0 + 1.0)
    var ema = entriesChronological.first().weightKg
    return entriesChronological.mapIndexed { idx, entry ->
      ema = if (idx == 0) entry.weightKg else alpha * entry.weightKg + (1.0 - alpha) * ema
      entry.dateIso to ema
    }
  }

  // #25 & #26 12-Season Color & Tailoring Analyzer
  fun analyzeSeasonalPalette(
    undertone: String,
    skinToneLabel: String,
    heightCm: Double,
    weightKg: Double,
    bodyBuild: String,
  ): SeasonalPaletteProfile {
    val bmi = WellnessCalculators.calculateBmi(weightKg, heightCm)
    val darkSkin =
      skinToneLabel.contains("Deep", true) ||
        skinToneLabel.contains("Rich", true) ||
        skinToneLabel.contains("Tan", true)
    val season =
      when {
        undertone.equals("Warm", true) && darkSkin -> "Deep Autumn"
        undertone.equals("Warm", true) -> "Warm Spring"
        undertone.equals("Cool", true) && darkSkin -> "Deep Winter"
        undertone.equals("Cool", true) -> "Cool Summer"
        darkSkin -> "True Olive Autumn"
        else -> "Soft Neutral Summer"
      }
    val swatches =
      when (season) {
        "Deep Autumn" ->
          listOf("Terracotta" to "#B55239", "Forest Olive" to "#3D5A45", "Warm Mustard" to "#C99635", "Espresso" to "#463128")
        "Warm Spring" ->
          listOf("Coral Peach" to "#E37A62", "Sage Green" to "#6D9773", "Camel" to "#C9A171", "Warm Ivory" to "#F1E9DA")
        "Deep Winter" ->
          listOf("Emerald" to "#1B6B50", "Cobalt Blue" to "#1F4E8C", "Burgundy" to "#7A203A", "Crisp White" to "#F5F7FA")
        "Cool Summer" ->
          listOf("Dusty Rose" to "#B67385", "Slate Blue" to "#4B688A", "Plum Berry" to "#69416B", "Cool Grey" to "#8C96A0")
        else ->
          listOf("Sage Teal" to "#487B75", "Sand Beige" to "#C7B59B", "Navy Ink" to "#24374F", "Oatmeal" to "#E7E0D3")
      }
    val tailoring = buildString {
      if (heightCm < 166) {
        append("Monochrome tonal columns & high-rise bottoms elongate vertical lines. ")
      } else if (heightCm > 180) {
        append("Color-blocked layers & mid-weight structured fabrics balance vertical frame. ")
      } else {
        append("Balanced golden-ratio 1/3 top to 2/3 bottom proportions flatter your height. ")
      }
      if (bodyBuild.contains("Broad", true) || bmi > 26.5) {
        append("Choose V/open collars, vertical seams, and breathable drape.")
      } else {
        append("Relaxed tailored shoulders and textured knits add crisp structure.")
      }
    }
    return SeasonalPaletteProfile(season, swatches, tailoring)
  }

  // #27 7-Day Capsule Wardrobe & Packing List
  fun generateCapsulePackingList(items: List<WardrobeItemEntity>, days: Int = 7): List<String> {
    val tops = items.filter { it.category == "TOP" || it.category == "DRESS" }.take(4)
    val bottoms = items.filter { it.category == "BOTTOM" }.take(3)
    val outer = items.filter { it.category == "OUTERWEAR" }.take(2)
    val shoes = items.filter { it.category == "FOOTWEAR" }.take(2)
    val list = mutableListOf<String>()
    list += "Capsule for $days days (mix-and-match up to ${maxOf(1, tops.size) * maxOf(1, bottoms.size)} outfits):"
    tops.forEach { list += "• Top: ${it.name} (${it.colorName})" }
    bottoms.forEach { list += "• Bottom: ${it.name} (${it.colorName})" }
    outer.forEach { list += "• Layer: ${it.name} (${it.colorName})" }
    shoes.forEach { list += "• Footwear: ${it.name}" }
    if (list.size <= 1) {
      list += "• 4 breathable tops in 2 base neutrals + 2 accent tones"
      list += "• 2 versatile tailored bottoms + 1 athleisure pair"
      list += "• 1 weatherproof outer jacket + 2 comfortable footwear pairs"
    }
    return list
  }

  // #58 Smart Grocery List Compiler
  fun buildWeeklyGroceryList(proteinGoalG: Int, activeMinutesGoal: Int): Map<String, List<String>> =
    mapOf(
      "Produce & Greens" to listOf("Spinach / Kale", "Avocados", "Blueberries", "Bell Peppers", "Sweet Potatoes"),
      "Lean Protein (~${proteinGoalG}g/day)" to listOf("Greek Yogurt", "Eggs", "Chicken Breast / Tofu", "Salmon / Lentils"),
      "Whole Grains & Fuel" to listOf("Rolled Oats", "Quinoa / Brown Rice", "Chia Seeds", "Almonds"),
      "Hydration (${activeMinutesGoal}m/day active)" to listOf("Sparkling Mineral Water", "Lemons", "Sea Salt + Potassium Electrolytes", "Herbal Tea"),
    )

  // #99 CSV / JSON Selective Step & Task Importer
  fun parseCsvImportLines(
    rawText: String,
    strideMeters: Double,
    weightKg: Double,
  ): Pair<List<DailyStepEntity>, List<WellnessTaskEntity>> {
    val steps = mutableListOf<DailyStepEntity>()
    val tasks = mutableListOf<WellnessTaskEntity>()
    val now = System.currentTimeMillis()
    rawText.lineSequence().forEach { line ->
      val trimmed = line.trim()
      if (trimmed.isEmpty() || trimmed.startsWith("RecordType", true) || trimmed.startsWith("date", true)) {
        return@forEach
      }
      val parts = trimmed.split(",").map { it.trim().removeSurrounding("\"") }
      if (parts.size >= 2 && Regex("""\d{4}-\d{2}-\d{2}""").matches(parts[0]) && parts[1].toIntOrNull() != null) {
        val dateIso = parts[0]
        val stepCount = parts[1].toInt().coerceIn(0, 120_000)
        steps += DailyStepEntity(
          dateIso = dateIso,
          steps = stepCount,
          goal = 8000,
          distanceMeters = WellnessCalculators.estimateDistanceMeters(stepCount, strideMeters),
          caloriesKcal = WellnessCalculators.estimateStepCalories(stepCount, weightKg),
          activeMinutes = WellnessCalculators.estimateActiveMinutesFromSteps(stepCount),
          source = "CSV_IMPORT",
          updatedAtMillis = now,
        )
      } else if (parts.firstOrNull()?.startsWith("TASK", true) == true && parts.size >= 2) {
        tasks += WellnessTaskEntity(
          id = "task_imp_${UUID.randomUUID()}",
          title = parts[1].ifBlank { "Imported Task" },
          notes = parts.getOrNull(2).orEmpty(),
          category = parts.getOrNull(3)?.uppercase()?.ifBlank { "GENERAL" } ?: "GENERAL",
          priority = TaskPriority.fromKey(parts.getOrNull(4)).storageKey,
          dueAtMillis = now + 3_600_000L,
          reminderEnabled = false,
        )
      }
    }
    return steps to tasks
  }
}
