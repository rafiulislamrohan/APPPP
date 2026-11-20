package com.example.data.health

import com.example.data.local.GpsPoint
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

data class HeartRateZone(
  val zoneNumber: Int,
  val label: String,
  val minBpm: Int,
  val maxBpm: Int,
  val purpose: String,
)

object WellnessCalculators {
  private val isoFormatter = DateTimeFormatter.ISO_LOCAL_DATE

  fun todayIso(): String = LocalDate.now().format(isoFormatter)

  fun epochMillisToIsoDate(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDate().format(isoFormatter)

  fun lastNDaysIso(days: Int): List<String> {
    val today = LocalDate.now()
    return (0 until days).map { offset -> today.minusDays((days - 1 - offset).toLong()).format(isoFormatter) }
  }

  /**
   * Distance in meters from step count and user stride length.
   */
  fun estimateDistanceMeters(steps: Int, strideMeters: Double): Double =
    (steps.coerceAtLeast(0) * strideMeters.coerceIn(0.35, 1.35))

  /**
   * Documented assumption: ~0.04 kcal per step at 70 kg reference mass, scaled linearly by body mass.
   */
  fun estimateStepCalories(steps: Int, weightKg: Double): Double {
    val factor = (weightKg.coerceIn(30.0, 220.0) / 70.0)
    return steps.coerceAtLeast(0) * 0.04 * factor
  }

  /**
   * Estimate active minutes assuming ~100 steps/minute brisk cadence.
   */
  fun estimateActiveMinutesFromSteps(steps: Int): Int = (steps.coerceAtLeast(0) / 100)

  /**
   * MET-based calorie burn: calories = MET * weightKg * hours.
   */
  fun estimateActivityCalories(met: Double, weightKg: Double, durationMillis: Long): Double {
    val hours = durationMillis.coerceAtLeast(0L) / 3_600_000.0
    return met * weightKg.coerceIn(30.0, 220.0) * hours
  }

  fun metForActivityType(activityType: String): Double =
    when (activityType.uppercase(Locale.US)) {
      "RUNNING" -> 9.0
      "CYCLING" -> 6.8
      else -> 3.8
    }

  fun haversineMeters(a: GpsPoint, b: GpsPoint): Double {
    val r = 6_371_000.0
    val dLat = Math.toRadians(b.latitude - a.latitude)
    val dLon = Math.toRadians(b.longitude - a.longitude)
    val lat1 = Math.toRadians(a.latitude)
    val lat2 = Math.toRadians(b.latitude)
    val h = sin(dLat / 2).pow(2) + cos(lat1) * cos(lat2) * sin(dLon / 2).pow(2)
    return 2 * r * atan2(sqrt(h), sqrt(1 - h))
  }

  fun calculateBmi(weightKg: Double, heightCm: Double): Double {
    val hMeters = (heightCm.coerceIn(100.0, 240.0) / 100.0)
    return weightKg.coerceIn(25.0, 250.0) / (hMeters * hMeters)
  }

  fun bmiCategory(bmi: Double): String =
    when {
      bmi < 18.5 -> "Underweight"
      bmi < 25.0 -> "Healthy Range"
      bmi < 30.0 -> "Overweight"
      else -> "Obesity Range"
    }

  fun formatDistance(meters: Double, imperial: Boolean): String {
    return if (imperial) {
      val miles = meters / 1609.344
      String.format(Locale.US, "%.2f mi", miles)
    } else {
      val km = meters / 1000.0
      String.format(Locale.US, "%.2f km", km)
    }
  }

  fun formatSpeed(metersPerSecond: Double, imperial: Boolean): String {
    return if (imperial) {
      val mph = metersPerSecond * 2.23694
      String.format(Locale.US, "%.1f mph", mph)
    } else {
      val kmh = metersPerSecond * 3.6
      String.format(Locale.US, "%.1f km/h", kmh)
    }
  }

  fun formatWeight(weightKg: Double, imperial: Boolean): String {
    return if (imperial) {
      val lbs = weightKg * 2.20462
      String.format(Locale.US, "%.1f lbs", lbs)
    } else {
      String.format(Locale.US, "%.1f kg", weightKg)
    }
  }

  fun formatVolume(ml: Int, imperial: Boolean): String {
    return if (imperial) {
      val oz = ml / 29.5735
      String.format(Locale.US, "%.1f oz", oz)
    } else {
      "$ml ml"
    }
  }

  fun formatDurationMillis(millis: Long): String {
    val totalSeconds = (millis / 1000L).coerceAtLeast(0L)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
      String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
      String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
  }

  fun formatMinutesToHours(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return "${h}h ${m}m"
  }

  /**
   * 5-Zone heart rate model from estimated Max HR (220 - age).
   */
  fun calculateHeartRateZones(age: Int): List<HeartRateZone> {
    val maxHr = 220 - age.coerceIn(10, 100)
    fun pct(p: Double) = (maxHr * p).toInt()
    return listOf(
      HeartRateZone(1, "Z1 Warm-Up", pct(0.50), pct(0.60), "Recovery & gentle mobility"),
      HeartRateZone(2, "Z2 Endurance", pct(0.60), pct(0.70), "Aerobic base & steady stamina"),
      HeartRateZone(3, "Z3 Tempo", pct(0.70), pct(0.80), "Cardio efficiency & pacing"),
      HeartRateZone(4, "Z4 Threshold", pct(0.80), pct(0.90), "Speed endurance & lactate tolerance"),
      HeartRateZone(5, "Z5 Peak", pct(0.90), maxHr, "Short maximal intervals"),
    )
  }

  /**
   * Reference daily hydration target (~33 ml/kg baseline + ~10 ml per active exercise minute).
   */
  fun calculateHydrationTargetMl(weightKg: Double, activeMinutes: Int): Int {
    val base = weightKg.coerceIn(30.0, 220.0) * 33.0
    val exerciseAdd = activeMinutes.coerceIn(0, 300) * 10.0
    return ((base + exerciseAdd).toInt() / 50 * 50).coerceIn(1500, 6000)
  }

  /**
   * Gender-neutral Mifflin-St Jeor reference BMR * activity multiplier for daily calorie estimation.
   */
  fun calculateDailyCalorieTdee(
    age: Int,
    heightCm: Double,
    weightKg: Double,
    activityMultiplier: Double,
  ): Int {
    val bmr =
      10.0 * weightKg.coerceIn(30.0, 220.0) +
        6.25 * heightCm.coerceIn(100.0, 240.0) -
        5.0 * age.coerceIn(10, 100) -
        78.0
    return (bmr * activityMultiplier.coerceIn(1.2, 2.1)).toInt().coerceAtLeast(1100)
  }
}
