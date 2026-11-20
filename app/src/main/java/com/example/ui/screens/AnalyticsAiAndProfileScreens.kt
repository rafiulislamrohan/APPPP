package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Style
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.ViewInAr
import androidx.compose.material.icons.outlined.Watch
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.credentials.CredentialManager
import com.example.R
import com.example.auth.AuthManager
import com.example.data.health.WellnessCalculators
import com.example.data.local.AiCoachMessageEntity
import com.example.data.local.DailyStepEntity
import com.example.data.local.GpsActivitySessionEntity
import com.example.data.local.HabitCompletionEntity
import com.example.data.local.NotificationConfigEntity
import com.example.data.local.UserProfilePreferences
import com.example.data.local.WorkoutSessionEntity
import com.example.data.repository.WearOsBridgeStatus
import com.example.ui.components.BarChartDatum
import com.example.ui.components.MedicalDisclaimerBanner
import com.example.ui.components.OfflineStatusBanner
import com.example.ui.components.VitalTopBar
import com.example.ui.components.WeeklyBarChart
import com.example.ui.components.tactile3D
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.ThemePreference
import com.example.ui.viewmodel.AiCoachUiState
import com.example.ui.viewmodel.SyncUiState
import com.google.firebase.auth.FirebaseUser
import java.util.Locale

// 21. Analytics, Challenges, Badges & Reports Screen
@Composable
fun AnalyticsScreen(
  preferences: UserProfilePreferences,
  dailySteps: List<DailyStepEntity>,
  gpsSessions: List<GpsActivitySessionEntity>,
  workoutSessions: List<WorkoutSessionEntity>,
  habitCompletions: List<HabitCompletionEntity>,
  onExportCsv: () -> Unit,
  onExportPdf: () -> Unit,
  onBack: () -> Unit,
) {
  BackHandler(onBack = onBack)

  val todayIso = remember { WellnessCalculators.todayIso() }
  val last7 = remember(dailySteps) {
    val map = dailySteps.associateBy { it.dateIso }
    WellnessCalculators.lastNDaysIso(7).map { iso ->
      BarChartDatum(iso.takeLast(2), (map[iso]?.steps ?: 0).toFloat(), iso == todayIso)
    }
  }
  val last30Steps = remember(dailySteps) {
    val dates = WellnessCalculators.lastNDaysIso(30).toSet()
    dailySteps.filter { it.dateIso in dates }.sumOf { it.steps }
  }
  val totalGpsMeters = remember(gpsSessions) { gpsSessions.sumOf { it.distanceMeters } }
  val longestStepDay = remember(dailySteps) { dailySteps.maxByOrNull { it.steps } }
  val longestGps = remember(gpsSessions) { gpsSessions.maxByOrNull { it.distanceMeters } }

  // Monthly challenge: 150,000 steps in 30 days
  val challengeGoal = (preferences.dailyStepGoal * 20).coerceAtLeast(100_000)

  val earnedBadges =
    remember(dailySteps, gpsSessions, workoutSessions, habitCompletions) {
      buildList {
        if (dailySteps.any { it.steps >= preferences.dailyStepGoal }) {
          add("Daily Goal Finisher" to "Reached ${preferences.dailyStepGoal}+ steps in a day")
        }
        if (gpsSessions.isNotEmpty()) {
          add("Trail Explorer" to "Completed ${gpsSessions.size} recorded GPS session(s)")
        }
        if (workoutSessions.isNotEmpty()) {
          add("Strength & Mobility" to "Completed ${workoutSessions.size} guided workout(s)")
        }
        if (habitCompletions.size >= 5) {
          add("Habit Builder" to "${habitCompletions.size} total habit check-ins recorded")
        }
      }
    }

  Scaffold(topBar = { VitalTopBar(title = "Fitness Analytics", onBack = onBack) }) { innerPadding ->
    Column(
      modifier =
        Modifier.fillMaxSize()
          .padding(innerPadding)
          .verticalScroll(rememberScrollState())
          .padding(16.dp)
          .widthIn(max = 640.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Text("7-Day Step Trend", style = MaterialTheme.typography.titleMedium)
          Spacer(modifier = Modifier.height(8.dp))
          WeeklyBarChart(data = last7, goalValue = preferences.dailyStepGoal.toFloat())
        }
      }

      // Monthly Challenge Card
      Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.EmojiEvents, contentDescription = null, tint = GoldAccent)
            Spacer(modifier = Modifier.width(8.dp))
            Text("30-Day Movement Challenge", style = MaterialTheme.typography.titleMedium)
          }
          Text(
            "$last30Steps / $challengeGoal steps • Total GPS: ${WellnessCalculators.formatDistance(totalGpsMeters, preferences.useImperialUnits)}",
            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = JetBrainsMonoFamily),
          )
          LinearProgressIndicator(
            progress = { (last30Steps.toFloat() / challengeGoal.coerceAtLeast(1)).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
          )
        }
      }

      // Personal Records
      Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text("Personal Records", style = MaterialTheme.typography.titleMedium)
          Text(
            "Best Day Steps: ${longestStepDay?.let { "${it.steps} (${it.dateIso})" } ?: "No data yet"}",
            style = MaterialTheme.typography.bodyMedium,
          )
          Text(
            "Longest GPS Route: ${longestGps?.let { "${WellnessCalculators.formatDistance(it.distanceMeters, preferences.useImperialUnits)} (${it.dateIso})" } ?: "No GPS sessions yet"}",
            style = MaterialTheme.typography.bodyMedium,
          )
          Text(
            "Workouts Completed: ${workoutSessions.size}",
            style = MaterialTheme.typography.bodyMedium,
          )
        }
      }

      // Achievement Badges
      Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("Earned Achievements (${earnedBadges.size})", style = MaterialTheme.typography.titleMedium)
          if (earnedBadges.isEmpty()) {
            Text(
              "Complete your first daily step goal, GPS route, workout, or 5 habits to unlock achievement badges.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          } else {
            earnedBadges.forEach { (title, desc) ->
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.EmojiEvents, contentDescription = null, tint = GoldAccent)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                  Text(title, style = MaterialTheme.typography.titleSmall)
                  Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
              }
            }
          }
        }
      }

      // CSV & PDF Report Exports
      Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        FilledTonalButton(
          onClick = onExportCsv,
          modifier = Modifier.weight(1f).height(48.dp).testTag("analytics_export_csv"),
        ) {
          Icon(Icons.Outlined.Download, contentDescription = null)
          Spacer(modifier = Modifier.width(6.dp))
          Text("Export CSV")
        }
        Button(
          onClick = onExportPdf,
          modifier = Modifier.weight(1f).height(48.dp).testTag("analytics_export_pdf"),
        ) {
          Icon(Icons.Outlined.Download, contentDescription = null)
          Spacer(modifier = Modifier.width(6.dp))
          Text("Weekly PDF")
        }
      }
    }
  }
}

// 22. AI Personal Wellness Coach Screen (Gemini 3.8 Flash via Firebase AI Logic)
@Composable
fun AiCoachScreen(
  messages: List<AiCoachMessageEntity>,
  coachState: AiCoachUiState,
  isOnline: Boolean,
  onSendPrompt: (prompt: String, topic: String) -> Unit,
  onClearHistory: () -> Unit,
  onBack: () -> Unit,
) {
  BackHandler(onBack = onBack)

  var promptDraft by remember { mutableStateOf("") }
  val quickPrompts =
    listOf(
      "Analyze my 7-day activity & hydration" to "SUMMARY",
      "Suggest a gentle recovery stretch" to "WORKOUT",
      "How can I build a sleep routine?" to "SLEEP",
      "Give me a balanced step goal check-in" to "COACH",
    )

  Scaffold(
    topBar = {
      VitalTopBar(
        title = stringResource(R.string.ai_coach_label),
        onBack = onBack,
        actions = {
          if (messages.isNotEmpty()) {
            IconButton(onClick = onClearHistory) {
              Icon(Icons.Outlined.DeleteOutline, contentDescription = "Clear AI conversation")
            }
          }
        },
      )
    }
  ) { innerPadding ->
    Column(
      modifier =
        Modifier.fillMaxSize()
          .padding(innerPadding)
          .padding(horizontal = 16.dp, vertical = 10.dp)
          .widthIn(max = 640.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      MedicalDisclaimerBanner()

      if (!isOnline) {
        OfflineStatusBanner()
      }

      Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        quickPrompts.forEach { (prompt, topic) ->
          FilterChip(
            selected = false,
            enabled = isOnline && coachState !is AiCoachUiState.Loading && coachState !is AiCoachUiState.Streaming,
            onClick = { onSendPrompt(prompt, topic) },
            label = { Text(prompt) },
          )
        }
      }

      LazyColumn(
        modifier = Modifier.weight(1f).fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        if (messages.isEmpty() && coachState is AiCoachUiState.Idle) {
          item {
            Card(
              shape = MaterialTheme.shapes.large,
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
              Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Image(
                  painter = painterResource(id = R.drawable.img_ai_coach_avatar),
                  contentDescription = "AI Wellness Coach emblem",
                  modifier = Modifier.size(60.dp).clip(CircleShape),
                  contentScale = ContentScale.Crop,
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                  Text("Optional Personal Wellness Coach", style = MaterialTheme.typography.titleMedium)
                  Text(
                    "Ask about your recorded step trends, sleep, hydration, or workout plans. All core tracking remains 100% offline.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
            }
          }
        }

        items(messages, key = { it.id }) { msg ->
          val isUser = msg.role == "user"
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
          ) {
            Surface(
              color =
                if (isUser) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surface,
              shape = MaterialTheme.shapes.medium,
              tonalElevation = 1.dp,
              modifier = Modifier.widthIn(max = 320.dp),
            ) {
              Text(
                text = msg.content,
                style = MaterialTheme.typography.bodyMedium,
                color =
                  if (isUser) MaterialTheme.colorScheme.onPrimary
                  else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(12.dp),
              )
            }
          }
        }

        if (coachState is AiCoachUiState.Loading) {
          item {
            Row(verticalAlignment = Alignment.CenterVertically) {
              CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
              Spacer(modifier = Modifier.width(8.dp))
              Text("Reviewing your wellness log...", style = MaterialTheme.typography.bodySmall)
            }
          }
        }

        if (coachState is AiCoachUiState.Streaming) {
          item {
            Surface(
              color = MaterialTheme.colorScheme.surface,
              shape = MaterialTheme.shapes.medium,
              tonalElevation = 1.dp,
            ) {
              Text(
                text = coachState.text,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(12.dp),
              )
            }
          }
        }

        if (coachState is AiCoachUiState.Error) {
          item {
            Surface(
              color = MaterialTheme.colorScheme.errorContainer,
              shape = MaterialTheme.shapes.small,
              modifier = Modifier.fillMaxWidth(),
            ) {
              Text(
                text = coachState.message,
                color = MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(12.dp),
              )
            }
          }
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        OutlinedTextField(
          value = promptDraft,
          onValueChange = { promptDraft = it },
          label = { Text(if (isOnline) "Ask your wellness coach" else "Reconnect to chat with AI") },
          enabled = isOnline,
          modifier = Modifier.weight(1f).testTag("ai_coach_input"),
          maxLines = 3,
        )
        IconButton(
          enabled = isOnline && promptDraft.isNotBlank() && coachState !is AiCoachUiState.Loading,
          onClick = {
            val txt = promptDraft
            promptDraft = ""
            onSendPrompt(txt, "CUSTOM")
          },
          modifier = Modifier.size(48.dp).testTag("ai_coach_send_button"),
        ) {
          Icon(Icons.AutoMirrored.Outlined.Send, contentDescription = "Send prompt")
        }
      }
    }
  }
}

// 23. Notifications & Reminders Screen
@Composable
fun NotificationsScreen(
  preferences: UserProfilePreferences,
  configs: List<NotificationConfigEntity>,
  onUpdateConfig: (NotificationConfigEntity) -> Unit,
  onBack: () -> Unit,
) {
  BackHandler(onBack = onBack)
  val context = LocalContext.current

  Scaffold(topBar = { VitalTopBar(title = "Reminders & Safety", onBack = onBack) }) { innerPadding ->
    Column(
      modifier =
        Modifier.fillMaxSize()
          .padding(innerPadding)
          .verticalScroll(rememberScrollState())
          .padding(16.dp)
          .widthIn(max = 640.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Text(
        "Gentle reminders run locally via WorkManager without requiring background network calls.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      configs.forEach { cfg ->
        Card(
          shape = MaterialTheme.shapes.medium,
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(cfg.title, style = MaterialTheme.typography.titleMedium)
              Text(
                if (cfg.intervalMinutes < 1440) "Every ${cfg.intervalMinutes / 60} hour(s)"
                else String.format(Locale.US, "Daily around %02d:%02d", cfg.hourOfDay, cfg.minute),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            Switch(
              checked = cfg.enabled,
              onCheckedChange = { onUpdateConfig(cfg.copy(enabled = it)) },
              modifier = Modifier.testTag("notif_switch_${cfg.id}"),
            )
          }
        }
      }

      if (preferences.emergencyContactPhone.isNotBlank()) {
        Card(
          shape = MaterialTheme.shapes.medium,
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text("Emergency Contact Shortcut", style = MaterialTheme.typography.titleSmall)
              Text(
                "${preferences.emergencyContactName.ifBlank { "Contact" }} (${preferences.emergencyContactPhone})",
                style = MaterialTheme.typography.bodySmall,
              )
            }
            FilledTonalButton(
              onClick = {
                val dial = Intent(Intent.ACTION_DIAL, "tel:${preferences.emergencyContactPhone}".toUri())
                context.startActivity(dial)
              },
            ) {
              Icon(Icons.Outlined.Phone, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Dial")
            }
          }
        }
      }
    }
  }
}

// 24a. Profile Hub (Bottom Tab 5)
@Composable
fun ProfileHubScreen(
  preferences: UserProfilePreferences,
  firebaseUser: FirebaseUser?,
  onOpenHabits: () -> Unit,
  onOpenTasks: () -> Unit = {},
  onOpenCalendar: () -> Unit,
  onOpenAnalytics: () -> Unit,
  onOpenAiCoach: () -> Unit,
  onOpenStyleAdvisor: () -> Unit = {},
  onOpenSpatial3D: () -> Unit = {},
  onOpenSpatialGallery: () -> Unit = {},
  onOpenToolbox: () -> Unit = {},
  onOpenNutritionHub: () -> Unit = {},
  onOpenVitalsClinical: () -> Unit = {},
  onOpenRecoveryStudio: () -> Unit = {},
  onOpenWardrobeCloset: () -> Unit = {},
  onOpenDiagnosticsLab: () -> Unit = {},
  onOpenNotifications: () -> Unit,
  onOpenSettings: () -> Unit,
  onOpenPrivacyData: () -> Unit,
) {
  Column(
    modifier =
      Modifier.fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 16.dp, vertical = 12.dp)
        .widthIn(max = 640.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Card(
      shape = MaterialTheme.shapes.large,
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
      Row(modifier = Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
        Image(
          painter = painterResource(id = R.drawable.ic_vitaltrack_fg_asset),
          contentDescription = "VITALTRACK Avatar",
          modifier = Modifier.size(58.dp).clip(CircleShape),
          contentScale = ContentScale.Crop,
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column {
          Text(preferences.displayName, style = MaterialTheme.typography.headlineSmall)
          Text(
            "Goal: ${preferences.dailyStepGoal} steps • ${WellnessCalculators.formatWeight(preferences.weightKg, preferences.useImperialUnits)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
          )
          Text(
            text = if (firebaseUser != null) "Cloud account: ${firebaseUser.email ?: "Signed in"}" else "Offline local profile active",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
          )
        }
      }
    }

    HubActionRow(Icons.Outlined.CheckCircle, "Daily Habits & Streaks", "Track water, sleep, and custom routines", onOpenHabits, "hub_habits")
    HubActionRow(Icons.Outlined.TaskAlt, "Tasks & Due Reminders", "Local Room tasks with WorkManager due-time alerts", onOpenTasks, "hub_tasks")
    HubActionRow(Icons.Outlined.CalendarMonth, "Activity Calendar", "Browse steps, GPS routes, and workouts by date", onOpenCalendar, "hub_calendar")
    HubActionRow(Icons.Outlined.Analytics, "Analytics, Challenges & Reports", "View 30-day trends, badges, CSV & PDF exports", onOpenAnalytics, "hub_analytics")
    HubActionRow(Icons.Outlined.AutoAwesome, stringResource(R.string.ai_coach_label), "Optional Gemini 3.8 Flash wellness coaching", onOpenAiCoach, "hub_ai_coach")
    HubActionRow(Icons.Outlined.Checkroom, "AI Style & Dress Colors", "Live smart outfit & color combos by tone, height & weight", onOpenStyleAdvisor, "hub_style_advisor")
    HubActionRow(Icons.Outlined.Style, "Digital Wardrobe & Dress Check", "Closet items, 12-season palette, RGB eyedropper & AI outfit check", onOpenWardrobeCloset, "hub_wardrobe_closet")
    HubActionRow(Icons.Outlined.ViewInAr, "3D Wellness & Body Studio", "360° rotatable vitality orb, body model & 3D GPS terrain", onOpenSpatial3D, "hub_spatial_3d")
    HubActionRow(Icons.Outlined.ViewInAr, "3D Pro Spatial Gallery", "Morphable chest/waist/hip avatar, spine posture, 3D city & trophies", onOpenSpatialGallery, "hub_spatial_gallery")
    HubActionRow(Icons.Outlined.Air, "3D Breathing & Calculators", "Guided 3D breathing sphere, HR zones, hydration & TDEE", onOpenToolbox, "hub_toolbox")
    HubActionRow(Icons.Outlined.Restaurant, "Nutrition, Fasting & Grocery", "Macro targets, AI meal photo estimator, caffeine & sweat sodium", onOpenNutritionHub, "hub_nutrition_hub")
    HubActionRow(Icons.Outlined.MonitorHeart, "Clinical Vitals & Physician PDF", "BP classifier, glucose, meds refill, EMA weight & doctor PDF", onOpenVitalsClinical, "hub_vitals_clinical")
    HubActionRow(Icons.Outlined.SelfImprovement, "Recovery, Soundscapes & NSDR", "Readiness score, brown noise/binaural mixer, 20-20-20 breaks & PMR", onOpenRecoveryStudio, "hub_recovery_studio")
    HubActionRow(Icons.Outlined.Tune, "Platform, NFC & Diagnostics Lab", "Theme palettes, biometric lock, auto-backup, CSV/JSON import & NFC", onOpenDiagnosticsLab, "hub_diagnostics_lab")
    HubActionRow(Icons.Outlined.NotificationsNone, "Reminders & Emergency Shortcut", "Configure local hydration and habit notifications", onOpenNotifications, "hub_notifications")
    HubActionRow(Icons.Outlined.Settings, "Settings & Cloud Sync", "Theme, 3D visuals, Bengali/English, Metric/Imperial, Cloud", onOpenSettings, "hub_settings")
    HubActionRow(Icons.Outlined.Shield, "Privacy & Data Management", "JSON backup/restore, permissions & data deletion", onOpenPrivacyData, "hub_privacy")

    Spacer(modifier = Modifier.height(16.dp))
  }
}

@Composable
private fun HubActionRow(
  icon: ImageVector,
  title: String,
  subtitle: String,
  onClick: () -> Unit,
  tag: String,
) {
  Card(
    modifier =
      Modifier.fillMaxWidth()
        .tactile3D(elevationDp = 5.dp, cornerRadiusDp = 16.dp)
        .clip(MaterialTheme.shapes.medium)
        .clickable(onClick = onClick)
        .testTag(tag),
    shape = MaterialTheme.shapes.medium,
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
  ) {
    Row(
      modifier = Modifier.padding(14.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
      Spacer(modifier = Modifier.width(12.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
      Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
    }
  }
}

// 24b. Settings Screen
@Composable
fun SettingsScreen(
  preferences: UserProfilePreferences,
  firebaseUser: FirebaseUser?,
  syncState: SyncUiState,
  wearOsStatus: WearOsBridgeStatus,
  wearSnapshotJsonProvider: () -> String = { "{}" },
  onThemeChange: (ThemePreference) -> Unit,
  onUnitsChange: (Boolean) -> Unit,
  onLanguageChange: (String) -> Unit,
  onThreeDVisualsChange: (Boolean) -> Unit = {},
  onEditProfile: () -> Unit,
  onOpenPermissions: () -> Unit,
  onSyncCloudNow: () -> Unit,
  onShowMessage: (String) -> Unit,
  onBack: () -> Unit,
) {
  BackHandler(onBack = onBack)

  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember { CredentialManager.create(context) }
  var showWearSnapshot by remember { mutableStateOf(false) }

  Scaffold(topBar = { VitalTopBar(title = "Settings", onBack = onBack) }) { innerPadding ->
    Column(
      modifier =
        Modifier.fillMaxSize()
          .padding(innerPadding)
          .verticalScroll(rememberScrollState())
          .padding(16.dp)
          .widthIn(max = 640.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("Appearance", style = MaterialTheme.typography.titleMedium)
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ThemePreference.entries.forEach { pref ->
              FilterChip(
                selected = preferences.themePreference == pref,
                onClick = { onThemeChange(pref) },
                label = { Text(pref.name.lowercase().replaceFirstChar { it.uppercase() }) },
              )
            }
          }

          Text("Units", style = MaterialTheme.typography.titleMedium)
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
              selected = !preferences.useImperialUnits,
              onClick = { onUnitsChange(false) },
              label = { Text("Metric (km, kg, ml)") },
            )
            FilterChip(
              selected = preferences.useImperialUnits,
              onClick = { onUnitsChange(true) },
              label = { Text("Imperial (mi, lbs, oz)") },
            )
          }

          Text("Language", style = MaterialTheme.typography.titleMedium)
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
              selected = preferences.languageCode == "en",
              onClick = { onLanguageChange("en") },
              label = { Text("English") },
            )
            FilterChip(
              selected = preferences.languageCode == "bn",
              onClick = { onLanguageChange("bn") },
              label = { Text("বাংলা (Bengali)") },
            )
          }

          Row(
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text("3D Depth & Gyro Parallax", style = MaterialTheme.typography.titleSmall)
              Text(
                "Adds 3D bevels, isometric charts & subtle motion tilt",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            androidx.compose.material3.Switch(
              checked = preferences.threeDVisualsEnabled,
              onCheckedChange = onThreeDVisualsChange,
              modifier = Modifier.testTag("settings_3d_toggle"),
            )
          }
        }
      }

      Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedButton(onClick = onEditProfile, modifier = Modifier.weight(1f).height(48.dp)) {
          Text("Edit Profile")
        }
        OutlinedButton(onClick = onOpenPermissions, modifier = Modifier.weight(1f).height(48.dp)) {
          Text("Permissions")
        }
      }

      // Optional Google Sign-In + Firestore Cloud Backup
      Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(stringResource(R.string.cloud_sync_title), style = MaterialTheme.typography.titleMedium)
          Text(
            stringResource(R.string.cloud_sync_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          if (firebaseUser == null) {
            Button(
              onClick = {
                AuthManager.onGoogleSignInClicked(
                  context = context,
                  credentialManager = credentialManager,
                  onAuthSuccess = { onShowMessage("Signed in with Google") },
                  onAuthError = { msg -> onShowMessage(msg) },
                  scope = scope,
                )
              },
              modifier = Modifier.fillMaxWidth().height(48.dp).testTag("settings_google_signin"),
            ) {
              Icon(Icons.Outlined.CloudUpload, contentDescription = null)
              Spacer(modifier = Modifier.width(8.dp))
              Text(stringResource(R.string.sign_in_button))
            }
          } else {
            Text(
              "Connected: ${firebaseUser.email ?: firebaseUser.uid}",
              style = MaterialTheme.typography.labelLarge,
              color = MaterialTheme.colorScheme.primary,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              Button(
                onClick = onSyncCloudNow,
                enabled = syncState !is SyncUiState.Syncing,
                modifier = Modifier.weight(1f).height(48.dp).testTag("settings_sync_now"),
              ) {
                Icon(Icons.Outlined.Sync, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (syncState is SyncUiState.Syncing) "Syncing..." else "Sync Now")
              }
              OutlinedButton(
                onClick = {
                  AuthManager.signOut(credentialManager, scope) {
                    onShowMessage("Signed out — local offline data stays intact")
                  }
                },
                modifier = Modifier.height(48.dp).testTag("settings_sign_out"),
              ) {
                Text(stringResource(R.string.sign_out_label))
              }
            }
          }
        }
      }

      // Wear OS Companion Bridge Architecture Card
      Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Watch, contentDescription = "Wear OS Companion", tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Text("Wearable Companion Bridge", style = MaterialTheme.typography.titleSmall)
              Text(
                wearOsStatus.statusDescription,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
          OutlinedButton(
            onClick = { showWearSnapshot = !showWearSnapshot },
            modifier = Modifier.fillMaxWidth().testTag("wear_snapshot_preview_button"),
          ) {
            Text(if (showWearSnapshot) "Hide Data Layer Payload" else "Inspect Data Layer Payload (${wearOsStatus.dataLayerPath})")
          }
          if (showWearSnapshot) {
            Surface(
              color = MaterialTheme.colorScheme.surfaceVariant,
              shape = MaterialTheme.shapes.small,
              modifier = Modifier.fillMaxWidth(),
            ) {
              Text(
                text = wearSnapshotJsonProvider(),
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFamily),
                modifier = Modifier.padding(12.dp),
              )
            }
          }
        }
      }
    }
  }
}

// 25. Privacy & Data Management Screen
@Composable
fun PrivacyDataScreen(
  firebaseUser: FirebaseUser?,
  onExportBackupJson: () -> Unit,
  onRestoreBackupUri: (Uri) -> Unit,
  onExportCsv: () -> Unit,
  onExportPdf: () -> Unit,
  onDeleteCloudData: () -> Unit,
  onDeleteAllLocalData: () -> Unit,
  onBack: () -> Unit,
) {
  BackHandler(onBack = onBack)

  var showDeleteConfirm by remember { mutableStateOf(false) }

  val restoreLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
      if (uri != null) onRestoreBackupUri(uri)
    }

  Scaffold(topBar = { VitalTopBar(title = "Privacy & Data Control", onBack = onBack) }) { innerPadding ->
    Column(
      modifier =
        Modifier.fillMaxSize()
          .padding(innerPadding)
          .verticalScroll(rememberScrollState())
          .padding(16.dp)
          .widthIn(max = 640.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text("How VITALTRACK Handles Your Data", style = MaterialTheme.typography.titleMedium)
          Text(
            "• All step counts, GPS tracks, water, sleep, weight, workouts, habits, and journal notes live in encrypted-at-rest Android Room & DataStore storage.\n• Cloud backup and Gemini AI coaching never run unless you trigger them online.\n• Export or erase your data completely at any time.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Button(
          onClick = onExportBackupJson,
          modifier = Modifier.weight(1f).height(48.dp).testTag("privacy_backup_json"),
        ) {
          Icon(Icons.Outlined.Download, contentDescription = null)
          Spacer(modifier = Modifier.width(6.dp))
          Text("Backup JSON")
        }
        OutlinedButton(
          onClick = { restoreLauncher.launch(arrayOf("application/json", "*/*")) },
          modifier = Modifier.weight(1f).height(48.dp).testTag("privacy_restore_json"),
        ) {
          Icon(Icons.Outlined.Restore, contentDescription = null)
          Spacer(modifier = Modifier.width(6.dp))
          Text("Restore JSON")
        }
      }

      Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        FilledTonalButton(onClick = onExportCsv, modifier = Modifier.weight(1f).height(48.dp)) {
          Text("Export CSV")
        }
        FilledTonalButton(onClick = onExportPdf, modifier = Modifier.weight(1f).height(48.dp)) {
          Text("Export PDF")
        }
      }

      if (firebaseUser != null) {
        OutlinedButton(
          onClick = onDeleteCloudData,
          modifier = Modifier.fillMaxWidth().height(48.dp).testTag("privacy_delete_cloud"),
        ) {
          Text("Delete Cloud Backup Data")
        }
      }

      Button(
        onClick = { showDeleteConfirm = true },
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("privacy_delete_local"),
      ) {
        Icon(Icons.Outlined.DeleteOutline, contentDescription = null)
        Spacer(modifier = Modifier.width(6.dp))
        Text("Erase All Local Data")
      }
    }
  }

  if (showDeleteConfirm) {
    AlertDialog(
      onDismissRequest = { showDeleteConfirm = false },
      title = { Text("Erase All Local Wellness Data?") },
      text = {
        Text("This permanently removes your recorded steps, GPS sessions, health logs, habits, and custom workouts from this device.")
      },
      confirmButton = {
        Button(
          onClick = {
            onDeleteAllLocalData()
            showDeleteConfirm = false
          },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
        ) {
          Text("Erase Everything")
        }
      },
      dismissButton = {
        TextButton(onClick = { showDeleteConfirm = false }) { Text(stringResource(R.string.cancel_label)) }
      },
    )
  }
}
