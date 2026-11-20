package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DirectionsRun
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import com.example.R
import com.example.data.health.HealthConnectAvailability
import com.example.data.health.HealthConnectManager
import com.example.data.local.UserProfilePreferences
import com.example.ui.components.VitalTopBar
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// 1. Splash Screen
@Composable
fun SplashScreen(
  preferences: UserProfilePreferences,
  onNavigateNext: (onboardingDone: Boolean, profileDone: Boolean) -> Unit,
) {
  val scale = remember { Animatable(0.85f) }
  val alpha = remember { Animatable(0f) }

  LaunchedEffect(Unit) {
    launch { scale.animateTo(1f, tween(550)) }
    launch { alpha.animateTo(1f, tween(550)) }
    delay(950L)
    onNavigateNext(preferences.onboardingCompleted, preferences.profileSetupCompleted)
  }

  Box(
    modifier =
      Modifier.fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
        .padding(32.dp),
    contentAlignment = Alignment.Center,
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier.scale(scale.value).alpha(alpha.value),
    ) {
      Image(
        painter = painterResource(id = R.drawable.ic_vitaltrack_fg_asset),
        contentDescription = "VITALTRACK Emblem",
        modifier = Modifier.size(104.dp).clip(CircleShape),
        contentScale = ContentScale.Crop,
      )
      Spacer(modifier = Modifier.height(20.dp))
      Text(
        text = stringResource(R.string.app_name),
        style = MaterialTheme.typography.displaySmall,
        color = MaterialTheme.colorScheme.primary,
      )
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = stringResource(R.string.app_tagline),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
      )
    }
  }
}

// 2. Onboarding Screen
@Composable
fun OnboardingScreen(
  onComplete: () -> Unit,
) {
  val pages =
    listOf(
      Triple(
        "Offline-First Activity & Hardware Steps",
        "VITALTRACK uses your device's hardware step sensors and local Room storage so every walk, run, and daily record stays private and works without internet.",
        Icons.AutoMirrored.Outlined.DirectionsRun,
      ),
      Triple(
        "Real GPS Routes & Personal Timeline",
        "Record outdoor walks, runs, and rides with OpenStreetMap routes, pause/resume controls, offline map tile caching, and chronological route replay.",
        Icons.Outlined.LocationOn,
      ),
      Triple(
        "Holistic Health, Habits & Optional AI Coach",
        "Track hydration, sleep, weight, workouts, and daily habits. Connect Health Connect or ask the optional Gemini AI coach whenever you are online.",
        Icons.Outlined.Shield,
      ),
    )
  var pageIndex by remember { mutableIntStateOf(0) }

  Scaffold { innerPadding ->
    Column(
      modifier =
        Modifier.fillMaxSize()
          .padding(innerPadding)
          .padding(horizontal = 24.dp, vertical = 16.dp)
          .widthIn(max = 560.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "VITALTRACK",
          style = MaterialTheme.typography.titleLarge,
          color = MaterialTheme.colorScheme.primary,
        )
        TextButton(onClick = onComplete, modifier = Modifier.testTag("onboarding_skip_button")) {
          Text("Skip")
        }
      }

      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Image(
          painter = painterResource(id = R.drawable.img_onboarding_journey),
          contentDescription = "Wellness journey illustration",
          modifier =
            Modifier.fillMaxWidth()
              .height(220.dp)
              .clip(MaterialTheme.shapes.large),
          contentScale = ContentScale.Crop,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Icon(
          imageVector = pages[pageIndex].third,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.tertiary,
          modifier = Modifier.size(36.dp),
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
          text = pages[pageIndex].first,
          style = MaterialTheme.typography.headlineSmall,
          textAlign = TextAlign.Center,
          color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
          text = pages[pageIndex].second,
          style = MaterialTheme.typography.bodyLarge,
          textAlign = TextAlign.Center,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      Column(modifier = Modifier.fillMaxWidth()) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.Center,
        ) {
          pages.indices.forEach { idx ->
            Box(
              modifier =
                Modifier.padding(horizontal = 4.dp)
                  .size(if (idx == pageIndex) 10.dp else 8.dp)
                  .clip(CircleShape)
                  .background(
                    if (idx == pageIndex) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outline
                  )
            )
          }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
          onClick = {
            if (pageIndex < pages.lastIndex) pageIndex++ else onComplete()
          },
          modifier = Modifier.fillMaxWidth().height(52.dp).testTag("onboarding_next_button"),
        ) {
          Text(if (pageIndex < pages.lastIndex) "Continue" else "Start Setup")
        }
      }
    }
  }
}

// 3. User Profile Setup Screen
@Composable
fun ProfileSetupScreen(
  preferences: UserProfilePreferences,
  fromSettings: Boolean,
  onSaveProfile: (
    displayName: String,
    age: Int,
    heightCm: Double,
    weightKg: Double,
    strideMeters: Double,
    stepGoal: Int,
    waterGoalMl: Int,
    sleepGoalMin: Int,
    workoutGoalMin: Int,
    useImperial: Boolean,
    languageCode: String,
    emergencyName: String,
    emergencyPhone: String,
  ) -> Unit,
  onBack: () -> Unit,
) {
  if (fromSettings) BackHandler(onBack = onBack)

  var name by remember(preferences) { mutableStateOf(preferences.displayName) }
  var ageText by remember(preferences) { mutableStateOf(preferences.age.toString()) }
  var heightText by remember(preferences) { mutableStateOf(preferences.heightCm.toInt().toString()) }
  var weightText by remember(preferences) { mutableStateOf(preferences.weightKg.toString()) }
  var strideText by remember(preferences) { mutableStateOf(preferences.strideLengthMeters.toString()) }
  var stepGoalText by remember(preferences) { mutableStateOf(preferences.dailyStepGoal.toString()) }
  var waterGoalText by remember(preferences) { mutableStateOf(preferences.dailyWaterGoalMl.toString()) }
  var useImperial by remember(preferences) { mutableStateOf(preferences.useImperialUnits) }
  var languageCode by remember(preferences) { mutableStateOf(preferences.languageCode) }
  var emergencyName by remember(preferences) { mutableStateOf(preferences.emergencyContactName) }
  var emergencyPhone by remember(preferences) { mutableStateOf(preferences.emergencyContactPhone) }

  Scaffold(
    topBar = {
      VitalTopBar(
        title = if (fromSettings) "Edit Wellness Profile" else "Personalize VITALTRACK",
        onBack = if (fromSettings) onBack else null,
      )
    }
  ) { innerPadding ->
    Column(
      modifier =
        Modifier.fillMaxSize()
          .padding(innerPadding)
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 20.dp, vertical = 12.dp)
          .widthIn(max = 600.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Text(
        text = "Used locally for stride distance, BMI, and MET calorie estimates. Stored only on your device unless you enable cloud sync.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      OutlinedTextField(
        value = name,
        onValueChange = { name = it },
        label = { Text("Display Name") },
        modifier = Modifier.fillMaxWidth().testTag("profile_name_input"),
        singleLine = true,
      )

      Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
          value = ageText,
          onValueChange = { ageText = it.filter(Char::isDigit) },
          label = { Text("Age") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          modifier = Modifier.weight(1f),
          singleLine = true,
        )
        OutlinedTextField(
          value = heightText,
          onValueChange = { heightText = it.filter(Char::isDigit) },
          label = { Text("Height (cm)") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          modifier = Modifier.weight(1f),
          singleLine = true,
        )
      }

      Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
          value = weightText,
          onValueChange = { weightText = it },
          label = { Text("Weight (kg)") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          modifier = Modifier.weight(1f),
          singleLine = true,
        )
        OutlinedTextField(
          value = strideText,
          onValueChange = { strideText = it },
          label = { Text("Stride (m)") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          modifier = Modifier.weight(1f),
          singleLine = true,
        )
      }

      Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
          value = stepGoalText,
          onValueChange = { stepGoalText = it.filter(Char::isDigit) },
          label = { Text("Daily Step Goal") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          modifier = Modifier.weight(1f),
          singleLine = true,
        )
        OutlinedTextField(
          value = waterGoalText,
          onValueChange = { waterGoalText = it.filter(Char::isDigit) },
          label = { Text("Water Goal (ml)") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          modifier = Modifier.weight(1f),
          singleLine = true,
        )
      }

      Text("Units & Language", style = MaterialTheme.typography.titleMedium)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        FilterChip(
          selected = !useImperial,
          onClick = { useImperial = false },
          label = { Text("Metric (km, kg)") },
        )
        FilterChip(
          selected = useImperial,
          onClick = { useImperial = true },
          label = { Text("Imperial (mi, lbs)") },
        )
      }
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        FilterChip(
          selected = languageCode == "en",
          onClick = { languageCode = "en" },
          label = { Text("English") },
        )
        FilterChip(
          selected = languageCode == "bn",
          onClick = { languageCode = "bn" },
          label = { Text("বাংলা (Bengali)") },
        )
      }

      Text("Emergency Contact Shortcut (Optional)", style = MaterialTheme.typography.titleMedium)
      Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
          value = emergencyName,
          onValueChange = { emergencyName = it },
          label = { Text("Contact Name") },
          modifier = Modifier.weight(1f),
          singleLine = true,
        )
        OutlinedTextField(
          value = emergencyPhone,
          onValueChange = { emergencyPhone = it },
          label = { Text("Phone Number") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
          modifier = Modifier.weight(1f),
          singleLine = true,
        )
      }

      Spacer(modifier = Modifier.height(8.dp))
      Button(
        onClick = {
          onSaveProfile(
            name.ifBlank { "Explorer" },
            ageText.toIntOrNull() ?: 28,
            heightText.toDoubleOrNull() ?: 172.0,
            weightText.toDoubleOrNull() ?: 70.0,
            strideText.toDoubleOrNull() ?: 0.75,
            stepGoalText.toIntOrNull() ?: 8000,
            waterGoalText.toIntOrNull() ?: 2200,
            preferences.dailySleepGoalMinutes,
            preferences.dailyWorkoutGoalMinutes,
            useImperial,
            languageCode,
            emergencyName,
            emergencyPhone,
          )
        },
        modifier = Modifier.fillMaxWidth().height(52.dp).testTag("save_profile_button"),
      ) {
        Text(if (fromSettings) "Save Changes" else "Continue to Permissions")
      }
      Spacer(modifier = Modifier.height(24.dp))
    }
  }
}

// 4. Permission Setup Screen
@Composable
fun PermissionSetupScreen(
  fromSettings: Boolean,
  healthConnectAvailability: HealthConnectAvailability,
  onPermissionsUpdated: () -> Unit,
  onContinue: () -> Unit,
) {
  if (fromSettings) BackHandler(onBack = onContinue)

  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  var refreshTick by remember { mutableIntStateOf(0) }
  var hcGranted by remember { mutableStateOf(false) }

  val activityGranted =
    remember(refreshTick) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) ==
          PackageManager.PERMISSION_GRANTED
      } else {
        true
      }
    }

  val locationGranted =
    remember(refreshTick) {
      ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED
    }

  val backgroundLocationGranted =
    remember(refreshTick) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) ==
          PackageManager.PERMISSION_GRANTED
      } else {
        true
      }
    }

  val notifGranted =
    remember(refreshTick) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
          PackageManager.PERMISSION_GRANTED
      } else {
        true
      }
    }

  LaunchedEffect(refreshTick) {
    if (healthConnectAvailability == HealthConnectAvailability.AVAILABLE) {
      runCatching {
        val client = HealthConnectClient.getOrCreate(context)
        hcGranted =
          client.permissionController.getGrantedPermissions()
            .containsAll(HealthConnectManager.REQUIRED_PERMISSIONS)
      }
    }
  }

  val singlePermLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
      refreshTick++
      onPermissionsUpdated()
    }

  val multiLocationLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
      refreshTick++
    }

  val hcLauncher =
    rememberLauncherForActivityResult(
      PermissionController.createRequestPermissionResultContract()
    ) {
      refreshTick++
    }

  Scaffold(
    topBar = {
      VitalTopBar(
        title = "Permissions & Privacy",
        onBack = if (fromSettings) onContinue else null,
      )
    }
  ) { innerPadding ->
    Column(
      modifier =
        Modifier.fillMaxSize()
          .padding(innerPadding)
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 20.dp, vertical = 12.dp)
          .widthIn(max = 600.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Text(
        text = "VITALTRACK only requests permissions for features you choose to enable. You can skip any permission and grant it later.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      PermissionItemCard(
        icon = Icons.AutoMirrored.Outlined.DirectionsRun,
        title = "Physical Activity (Step Counter)",
        rationale = "Reads your phone's built-in hardware step counter sensor without draining battery.",
        granted = activityGranted,
        actionLabel = "Allow Steps",
        testTag = "perm_steps_button",
        onRequest = {
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            singlePermLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
          }
        },
      )

      PermissionItemCard(
        icon = Icons.Outlined.LocationOn,
        title = "Location (GPS Route Recording)",
        rationale = "Used ONLY when you actively start a walk, run, or cycling session to draw your route.",
        granted = locationGranted,
        actionLabel = "Allow Location",
        testTag = "perm_location_button",
        onRequest = {
          multiLocationLauncher.launch(
            arrayOf(
              Manifest.permission.ACCESS_FINE_LOCATION,
              Manifest.permission.ACCESS_COARSE_LOCATION,
            )
          )
        },
      )

      if (locationGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        PermissionItemCard(
          icon = Icons.Outlined.LocationOn,
          title = "Background Location (Optional)",
          rationale = "Allows uninterrupted GPS tracking when the screen turns off during outdoor workouts.",
          granted = backgroundLocationGranted,
          actionLabel = "Open App Settings",
          testTag = "perm_bg_location_button",
          onRequest = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
              context.startActivity(
                Intent(
                  Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                  Uri.fromParts("package", context.packageName, null),
                )
              )
            } else {
              singlePermLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            }
          },
        )
      }

      PermissionItemCard(
        icon = Icons.Outlined.NotificationsNone,
        title = "Notifications",
        rationale = "Shows ongoing GPS session status and optional water/habit reminders.",
        granted = notifGranted,
        actionLabel = "Allow Alerts",
        testTag = "perm_notifications_button",
        onRequest = {
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            singlePermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
          }
        },
      )

      PermissionItemCard(
        icon = Icons.Outlined.FavoriteBorder,
        title = "Health Connect",
        rationale =
          when (healthConnectAvailability) {
            HealthConnectAvailability.AVAILABLE ->
              "Read heart rate, sleep stages, steps, and weight from connected wearables or Health Connect."
            HealthConnectAvailability.NOT_INSTALLED ->
              "Health Connect app is not installed. Manual logging remains available."
            HealthConnectAvailability.NOT_SUPPORTED ->
              "Health Connect requires Android 9+. Manual logging remains available."
          },
        granted = hcGranted,
        actionLabel =
          if (healthConnectAvailability == HealthConnectAvailability.AVAILABLE) "Connect Health"
          else "Unavailable",
        enabled = healthConnectAvailability == HealthConnectAvailability.AVAILABLE,
        testTag = "perm_health_connect_button",
        onRequest = {
          scope.launch { hcLauncher.launch(HealthConnectManager.REQUIRED_PERMISSIONS) }
        },
      )

      Spacer(modifier = Modifier.height(8.dp))
      Button(
        onClick = onContinue,
        modifier = Modifier.fillMaxWidth().height(52.dp).testTag("permissions_continue_button"),
      ) {
        Text(if (fromSettings) "Done" else "Enter VITALTRACK Dashboard")
      }
      Spacer(modifier = Modifier.height(24.dp))
    }
  }
}

@Composable
private fun PermissionItemCard(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  title: String,
  rationale: String,
  granted: Boolean,
  actionLabel: String,
  testTag: String,
  enabled: Boolean = true,
  onRequest: () -> Unit,
) {
  Card(
    shape = MaterialTheme.shapes.medium,
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = title, tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(10.dp))
        Text(
          text = title,
          style = MaterialTheme.typography.titleMedium,
          modifier = Modifier.weight(1f),
        )
        if (granted) {
          Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = "Granted",
            tint = MaterialTheme.colorScheme.primary,
          )
        }
      }
      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = rationale,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      if (!granted) {
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedButton(
          onClick = onRequest,
          enabled = enabled,
          modifier = Modifier.testTag(testTag),
        ) {
          Text(actionLabel)
        }
      }
    }
  }
}
