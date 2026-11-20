package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.health.WellnessAdvancedCalculators
import com.example.data.health.WellnessCalculators
import com.example.data.local.NutritionLogEntity
import com.example.data.local.UserProfilePreferences
import com.example.ui.components.OfflineStatusBanner
import com.example.ui.components.Tactile3DCard
import com.example.ui.components.VitalTopBar
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.viewmodel.MealPhotoUiState
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.Locale

/**
 * Nutrition, Fuel & Bio-Fueling Hub (#51-#60).
 */
@Composable
fun NutritionHubScreen(
  preferences: UserProfilePreferences,
  nutritionLogs: List<NutritionLogEntity>,
  mealPhotoState: MealPhotoUiState,
  isOnline: Boolean,
  onLogNutrition: (
    mealName: String,
    mealType: String,
    caloriesKcal: Int,
    proteinG: Double,
    carbsG: Double,
    fatG: Double,
    fiberG: Double,
    beverageMl: Int,
    hydrationFactor: Double,
    caffeineMg: Int,
    electrolytesMg: Int,
  ) -> Unit,
  onDeleteNutrition: (String) -> Unit,
  onAnalyzeMealPhoto: (Bitmap) -> Unit,
  onSetFastingState: (active: Boolean, targetHours: Int) -> Unit,
  onScheduleMealPrepTask: () -> Unit,
  onBack: () -> Unit,
) {
  BackHandler(onBack = onBack)

  val context = LocalContext.current
  val todayIso = remember { WellnessCalculators.todayIso() }
  val todayLogs = remember(nutritionLogs, todayIso) { nutritionLogs.filter { it.dateIso == todayIso } }

  var goalMode by remember { mutableStateOf("MAINTAIN") }
  val tdee = remember(preferences) {
    WellnessCalculators.calculateDailyCalorieTdee(preferences.age, preferences.heightCm, preferences.weightKg, 1.55)
  }
  val targets = remember(tdee, preferences.weightKg, goalMode) {
    WellnessAdvancedCalculators.calculateMacroTargets(tdee, preferences.weightKg, goalMode)
  }

  val totalKcal = todayLogs.sumOf { it.caloriesKcal }
  val totalProtein = todayLogs.sumOf { it.proteinG }
  val totalCarbs = todayLogs.sumOf { it.carbsG }
  val totalFat = todayLogs.sumOf { it.fatG }

  var mealNameDraft by remember { mutableStateOf("") }
  var kcalDraft by remember { mutableStateOf("450") }
  var proteinDraft by remember { mutableStateOf("30") }
  var carbsDraft by remember { mutableStateOf("45") }
  var fatDraft by remember { mutableStateOf("14") }

  var tempC by remember { mutableFloatStateOf(26f) }
  var activeMin by remember { mutableFloatStateOf(45f) }
  val (sweatWaterMl, sweatSodiumMg) = remember(tempC, activeMin) {
    WellnessAdvancedCalculators.calculateElectrolyteHydrationOffset(2200, tempC.toDouble(), activeMin.toInt())
  }

  var fastingTargetHours by remember { mutableIntStateOf(preferences.fastingTargetHours.coerceIn(12, 24)) }
  var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
  LaunchedEffect(preferences.fastingActive) {
    while (preferences.fastingActive) {
      delay(1000L)
      nowMs = System.currentTimeMillis()
    }
  }

  val caffeineTonightMg = remember(todayLogs, nowMs) {
    val tonightMs = LocalDate.now().atTime(LocalTime.of(22, 0)).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    WellnessAdvancedCalculators.estimateRemainingCaffeineMg(
      todayLogs.filter { it.caffeineMg > 0 }.map { it.caffeineMg to it.recordedAtMillis },
      tonightMs,
    )
  }

  val groceryMap = remember(targets.proteinG, preferences.dailyWorkoutGoalMinutes) {
    WellnessAdvancedCalculators.buildWeeklyGroceryList(targets.proteinG, preferences.dailyWorkoutGoalMinutes)
  }
  val checkedGrocery = remember { mutableStateListOf<String>() }

  val mealPhotoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
    if (uri != null) {
      runCatching {
        val bmp = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
          ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
        } else {
          @Suppress("DEPRECATION")
          MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        }
        onAnalyzeMealPhoto(bmp)
      }
    }
  }

  Scaffold(topBar = { VitalTopBar(title = "Nutrition & Bio-Fuel Hub", onBack = onBack) }) { innerPadding ->
    Column(
      modifier =
        Modifier.fillMaxSize()
          .padding(innerPadding)
          .verticalScroll(rememberScrollState())
          .padding(16.dp)
          .widthIn(max = 640.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      if (!isOnline) OfflineStatusBanner()

      // #51 Macro Targets & Daily Rings
      Tactile3DCard(enabled3D = preferences.threeDVisualsEnabled) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Outlined.Restaurant, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text("Daily Macro & Calorie Targets", style = MaterialTheme.typography.titleMedium)
          }
          Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("CUT" to "Cut (-400)", "MAINTAIN" to "Maintain", "BULK" to "Gain (+300)").forEach { (k, label) ->
              FilterChip(selected = goalMode == k, onClick = { goalMode = k }, label = { Text(label) })
            }
          }
          MacroProgressRow("Calories", totalKcal, targets.caloriesKcal, "kcal")
          MacroProgressRow("Protein", totalProtein.toInt(), targets.proteinG, "g")
          MacroProgressRow("Carbs", totalCarbs.toInt(), targets.carbsG, "g")
          MacroProgressRow("Fat", totalFat.toInt(), targets.fatG, "g")
        }
      }

      // #52 Quick Macro Meal Logger + #53 AI Meal Photo Estimator
      Tactile3DCard(enabled3D = preferences.threeDVisualsEnabled) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("Log Meal or Estimate From Photo", style = MaterialTheme.typography.titleMedium)
          OutlinedTextField(
            value = mealNameDraft,
            onValueChange = { mealNameDraft = it },
            label = { Text("Meal / Food Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("nutrition_meal_name"),
          )
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
              value = kcalDraft,
              onValueChange = { kcalDraft = it },
              label = { Text("kcal") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
              value = proteinDraft,
              onValueChange = { proteinDraft = it },
              label = { Text("Prot (g)") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
              value = carbsDraft,
              onValueChange = { carbsDraft = it },
              label = { Text("Carb (g)") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
              value = fatDraft,
              onValueChange = { fatDraft = it },
              label = { Text("Fat (g)") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              modifier = Modifier.weight(1f),
            )
          }
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
              enabled = mealNameDraft.isNotBlank(),
              onClick = {
                onLogNutrition(
                  mealNameDraft.trim(),
                  "MEAL",
                  kcalDraft.toIntOrNull() ?: 400,
                  proteinDraft.toDoubleOrNull() ?: 25.0,
                  carbsDraft.toDoubleOrNull() ?: 40.0,
                  fatDraft.toDoubleOrNull() ?: 12.0,
                  5.0, 0, 1.0, 0, 0,
                )
                mealNameDraft = ""
              },
              modifier = Modifier.weight(1f).testTag("nutrition_log_meal_btn"),
            ) {
              Text("Log Meal")
            }
            FilledTonalButton(
              enabled = isOnline && mealPhotoState !is MealPhotoUiState.Loading,
              onClick = {
                mealPhotoLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
              },
              modifier = Modifier.weight(1f).testTag("nutrition_photo_ai_btn"),
            ) {
              Icon(Icons.Outlined.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("AI Photo Scan")
            }
          }
          when (mealPhotoState) {
            MealPhotoUiState.Loading -> CircularProgressIndicator(modifier = Modifier.size(22.dp))
            is MealPhotoUiState.Success -> {
              val est = mealPhotoState.estimate
              Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(10.dp)) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                  Text("AI Estimate: ${est.mealName} (${est.caloriesKcal} kcal)", style = MaterialTheme.typography.titleSmall)
                  Text("P: ${est.proteinG}g • C: ${est.carbsG}g • F: ${est.fatG}g — ${est.nutritionNote}", style = MaterialTheme.typography.bodySmall)
                  Button(onClick = {
                    onLogNutrition(est.mealName, "MEAL", est.caloriesKcal, est.proteinG, est.carbsG, est.fatG, est.fiberG, 0, 1.0, 0, 0)
                  }) { Text("Save AI Meal to Log") }
                }
              }
            }
            is MealPhotoUiState.Error -> Text(mealPhotoState.message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            MealPhotoUiState.Idle -> Unit
          }
        }
      }

      // #55 Beverage Hydration Factors & #57 Caffeine Half-Life Tracker
      Tactile3DCard(enabled3D = preferences.threeDVisualsEnabled) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Outlined.LocalCafe, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text("Beverages & Caffeine Half-Life (~5h)", style = MaterialTheme.typography.titleMedium)
          }
          Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedButton(onClick = { onLogNutrition("Coffee (240ml)", "BEVERAGE", 5, 0.0, 0.0, 0.0, 0.0, 240, 0.80, 95, 0) }) {
              Text("+Coffee (95mg)")
            }
            OutlinedButton(onClick = { onLogNutrition("Green Tea (250ml)", "BEVERAGE", 0, 0.0, 0.0, 0.0, 0.0, 250, 0.95, 35, 0) }) {
              Text("+Green Tea (35mg)")
            }
            OutlinedButton(onClick = { onLogNutrition("Electrolyte Mix", "BEVERAGE", 25, 0.0, 6.0, 0.0, 0.0, 350, 1.15, 0, 450) }) {
              Text("+Electrolyte (1.15x)")
            }
          }
          Text(
            "Projected caffeine remaining at 22:00: ${String.format(Locale.US, "%.1f", caffeineTonightMg)} mg" +
              if (caffeineTonightMg > 50.0) " (May affect deep sleep)" else " (Clear for restful sleep)",
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFamily),
          )
        }
      }

      // #54 Sweat-Loss Electrolyte Calculator & #56 Fasting Window
      Tactile3DCard(enabled3D = preferences.threeDVisualsEnabled) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Outlined.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text("Intermittent Fasting & Sweat Electrolytes", style = MaterialTheme.typography.titleMedium)
          }
          Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(16, 18, 20).forEach { hrs ->
              FilterChip(selected = fastingTargetHours == hrs, onClick = { fastingTargetHours = hrs }, label = { Text("${hrs}:${24 - hrs}") })
            }
          }
          if (preferences.fastingActive && preferences.fastingStartMillis > 0L) {
            val elapsedMs = (nowMs - preferences.fastingStartMillis).coerceAtLeast(0L)
            Text(
              "Fasting Active: ${WellnessCalculators.formatDurationMillis(elapsedMs)} / ${preferences.fastingTargetHours}h goal",
              style = MaterialTheme.typography.titleSmall.copy(fontFamily = JetBrainsMonoFamily),
              color = MaterialTheme.colorScheme.primary,
            )
          }
          Button(
            onClick = { onSetFastingState(!preferences.fastingActive, fastingTargetHours) },
            modifier = Modifier.fillMaxWidth().testTag("nutrition_fasting_toggle"),
          ) {
            Text(if (preferences.fastingActive) "Complete Fasting Window" else "Start ${fastingTargetHours}h Fast Now")
          }
          Text("Ambient Heat: ${tempC.toInt()}°C • Exercise: ${activeMin.toInt()}m", style = MaterialTheme.typography.labelMedium)
          Slider(value = tempC, onValueChange = { tempC = it }, valueRange = 12f..42f)
          Slider(value = activeMin, onValueChange = { activeMin = it }, valueRange = 0f..180f)
          Text(
            "Sweat Target: $sweatWaterMl ml water + $sweatSodiumMg mg sodium",
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFamily),
            color = MaterialTheme.colorScheme.primary,
          )
        }
      }

      // #58 Grocery List, #59 Meal Prep WorkManager, #60 Micronutrients
      Tactile3DCard(enabled3D = preferences.threeDVisualsEnabled) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Outlined.ShoppingCart, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text("Supplements, Meal Prep & Grocery List", style = MaterialTheme.typography.titleMedium)
          }
          Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("Vitamin D3", "Magnesium", "Omega-3", "Iron + B12").forEach { supp ->
              OutlinedButton(onClick = { onLogNutrition(supp, "SUPPLEMENT", 0, 0.0, 0.0, 0.0, 0.0, 0, 1.0, 0, 0) }) {
                Text("+ $supp")
              }
            }
          }
          OutlinedButton(onClick = onScheduleMealPrepTask, modifier = Modifier.fillMaxWidth().testTag("nutrition_schedule_prep_btn")) {
            Icon(Icons.Outlined.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Schedule Weekly Sunday Meal-Prep Task")
          }
          groceryMap.forEach { (section, items) ->
            Text(section, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            items.forEach { item ->
              val checked = item in checkedGrocery
              Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = checked, onCheckedChange = { if (it) checkedGrocery.add(item) else checkedGrocery.remove(item) })
                Text(item, style = MaterialTheme.typography.bodySmall)
              }
            }
          }
        }
      }

      if (todayLogs.isNotEmpty()) {
        Text("Today's Fuel Log (${todayLogs.size})", style = MaterialTheme.typography.titleMedium)
        todayLogs.forEach { log ->
          Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
              Column(modifier = Modifier.weight(1f)) {
                Text(log.mealName, style = MaterialTheme.typography.titleSmall)
                Text("${log.mealType} • ${log.caloriesKcal} kcal • P:${log.proteinG.toInt()}g C:${log.carbsG.toInt()}g F:${log.fatG.toInt()}g", style = MaterialTheme.typography.bodySmall)
              }
              IconButton(onClick = { onDeleteNutrition(log.id) }) {
                Icon(Icons.Outlined.DeleteOutline, contentDescription = "Delete nutrition entry")
              }
            }
          }
        }
      }
      Spacer(modifier = Modifier.height(20.dp))
    }
  }
}

@Composable
private fun MacroProgressRow(label: String, current: Int, target: Int, unit: String) {
  Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
      Text(label, style = MaterialTheme.typography.labelMedium)
      Text("$current / $target $unit", style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFamily))
    }
    LinearProgressIndicator(
      progress = { (current.toFloat() / target.coerceAtLeast(1)).coerceIn(0f, 1f) },
      modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(4.dp)),
    )
  }
}
