package com.example

import com.example.data.health.WellnessCalculators
import com.example.data.local.GpsPoint
import com.example.data.local.HabitCompletionEntity
import com.example.data.repository.VitalTrackRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class WellnessCalculatorsTest {

  @Test
  fun estimateDistanceAndCalories_usesConfiguredStrideAndMass() {
    val distance = WellnessCalculators.estimateDistanceMeters(steps = 1000, strideMeters = 0.80)
    assertEquals(800.0, distance, 0.01)

    val kcal70 = WellnessCalculators.estimateStepCalories(steps = 1000, weightKg = 70.0)
    assertEquals(40.0, kcal70, 0.01)

    val kcal84 = WellnessCalculators.estimateStepCalories(steps = 1000, weightKg = 84.0)
    assertEquals(48.0, kcal84, 0.01)
  }

  @Test
  fun calculateBmiAndCategory_returnsAccurateClassifications() {
    val healthy = WellnessCalculators.calculateBmi(weightKg = 68.0, heightCm = 175.0)
    assertEquals(22.2, healthy, 0.1)
    assertEquals("Healthy Range", WellnessCalculators.bmiCategory(healthy))

    val overweight = WellnessCalculators.calculateBmi(weightKg = 85.0, heightCm = 175.0)
    assertEquals("Overweight", WellnessCalculators.bmiCategory(overweight))
  }

  @Test
  fun haversineDistance_computesRealisticMetersBetweenPoints() {
    // Approx 111 meters per 0.001 degree latitude
    val p1 = GpsPoint(latitude = 23.8103, longitude = 90.4125)
    val p2 = GpsPoint(latitude = 23.8113, longitude = 90.4125)
    val meters = WellnessCalculators.haversineMeters(p1, p2)
    assertTrue("Expected ~111m, got $meters", meters in 105.0..118.0)
  }

  @Test
  fun habitStreakCalculation_countsConsecutiveDaysIncludingToday() {
    val today = LocalDate.now()
    val completions =
      listOf(
        HabitCompletionEntity("h1|$today", "h1", today.toString(), 1000L),
        HabitCompletionEntity("h1|${today.minusDays(1)}", "h1", today.minusDays(1).toString(), 900L),
        HabitCompletionEntity("h1|${today.minusDays(2)}", "h1", today.minusDays(2).toString(), 800L),
        HabitCompletionEntity("h1|${today.minusDays(5)}", "h1", today.minusDays(5).toString(), 500L),
      )
    assertEquals(3, VitalTrackRepository.calculateHabitStreak("h1", completions))
  }

  @Test
  fun formatHelpers_respectMetricAndImperialUnits() {
    assertEquals("5.00 km", WellnessCalculators.formatDistance(5000.0, imperial = false))
    assertEquals("3.11 mi", WellnessCalculators.formatDistance(5000.0, imperial = true))
    assertEquals("70.0 kg", WellnessCalculators.formatWeight(70.0, imperial = false))
    assertEquals("154.3 lbs", WellnessCalculators.formatWeight(70.0, imperial = true))
    assertEquals("05:30", WellnessCalculators.formatDurationMillis(330_000L))
  }

  @Test
  fun hrZonesHydrationAndTdeeCalculators_returnConsistentPhysiologicalValues() {
    val zones = WellnessCalculators.calculateHeartRateZones(age = 30)
    assertEquals(5, zones.size)
    assertEquals(95, zones.first().minBpm)
    assertEquals(190, zones.last().maxBpm)

    val waterRest = WellnessCalculators.calculateHydrationTargetMl(weightKg = 70.0, activeMinutes = 0)
    val waterActive = WellnessCalculators.calculateHydrationTargetMl(weightKg = 70.0, activeMinutes = 60)
    assertEquals(2300, waterRest)
    assertEquals(2900, waterActive)

    val tdeeSedentary = WellnessCalculators.calculateDailyCalorieTdee(30, 175.0, 70.0, 1.2)
    val tdeeModerate = WellnessCalculators.calculateDailyCalorieTdee(30, 175.0, 70.0, 1.55)
    assertTrue(tdeeModerate > tdeeSedentary)
    assertTrue(tdeeSedentary in 1500..2300)
  }

  @Test
  fun advancedPerformanceCalculators_computeOneRepMaxVo2AcwrAndLoopPoints() {
    val oneRm = com.example.data.health.WellnessAdvancedCalculators.estimateOneRepMaxKg(100.0, 5)
    assertEquals(116.67, oneRm, 0.1)

    val vo2 = com.example.data.health.WellnessAdvancedCalculators.estimateVo2Max(age = 30, restingHrBpm = 60)
    assertEquals(48.45, vo2, 0.1)

    val (acwr, status) = com.example.data.health.WellnessAdvancedCalculators.calculateAcwr(150, 560)
    assertTrue(acwr in 1.0..1.15)
    assertEquals("Optimal Sweet Spot", status)

    val loopPts = com.example.data.health.WellnessAdvancedCalculators.generateLoopRoutePoints(23.8103, 90.4125, 5.0)
    assertEquals(13, loopPts.size)
  }

  @Test
  fun nutritionVitalsAndWardrobeCalculators_computeAccurateOutputs() {
    val macros = com.example.data.health.WellnessAdvancedCalculators.calculateMacroTargets(2400, 75.0, "CUT")
    assertEquals(2000, macros.caloriesKcal)
    assertEquals(135, macros.proteinG)

    val now = 1_700_050_000_000L
    val remCaffeine =
      com.example.data.health.WellnessAdvancedCalculators.estimateRemainingCaffeineMg(
        doses = listOf(100 to (now - 5L * 3_600_000L)),
        targetTimeMillis = now,
      )
    assertEquals(50.0, remCaffeine, 0.1)

    assertEquals("Normal Range", com.example.data.health.WellnessAdvancedCalculators.classifyBloodPressure(116, 74))
    assertEquals("Stage 2 Elevated", com.example.data.health.WellnessAdvancedCalculators.classifyBloodPressure(145, 92))

    val (whtr, whtrLabel) = com.example.data.health.WellnessAdvancedCalculators.waistToHeightRatio(80.0, 175.0)
    assertTrue(whtr in 0.44..0.47)
    assertEquals("Healthy Proportion", whtrLabel)

    val season =
      com.example.data.health.WellnessAdvancedCalculators.analyzeSeasonalPalette(
        undertone = "Warm",
        skinToneLabel = "Deep Warm",
        heightCm = 174.0,
        weightKg = 70.0,
        bodyBuild = "Athletic",
      )
    assertEquals("Deep Autumn", season.seasonName)

    val (steps, tasks) =
      com.example.data.health.WellnessAdvancedCalculators.parseCsvImportLines(
        rawText = "date,steps\n2026-11-18,7500\nTASK,Morning Mobility,10m,WORKOUT,HIGH",
        strideMeters = 0.75,
        weightKg = 70.0,
      )
    assertEquals(1, steps.size)
    assertEquals(7500, steps.first().steps)
    assertEquals(1, tasks.size)
    assertEquals("Morning Mobility", tasks.first().title)
    assertEquals("HIGH", tasks.first().priority)
  }
}
