package com.example.service

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.MainActivity
import com.example.data.health.WellnessCalculators
import com.example.data.local.UserPreferencesDataStore
import com.example.data.local.VitalTrackDatabase
import com.example.data.local.WaterIntakeEntity
import com.example.data.local.SyncStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

object VitalTrackWidgetKeys {
  val STEPS = intPreferencesKey("widget_steps")
  val STEP_GOAL = intPreferencesKey("widget_step_goal")
  val WATER_ML = intPreferencesKey("widget_water_ml")
  val WATER_GOAL_ML = intPreferencesKey("widget_water_goal_ml")
  val STREAK_DAYS = intPreferencesKey("widget_streak_days")
  val DATE_LABEL = stringPreferencesKey("widget_date_label")
  val TOP_TASK_LABEL = stringPreferencesKey("widget_top_task_label")
}

class RefreshAndAddWaterAction : ActionCallback {
  override suspend fun onAction(
    context: Context,
    glanceId: GlanceId,
    parameters: ActionParameters,
  ) {
    withContext(Dispatchers.IO) {
      val db = VitalTrackDatabase.getInstance(context)
      val today = WellnessCalculators.todayIso()
      db.healthRecordDao()
        .insertWater(
          WaterIntakeEntity(
            id = UUID.randomUUID().toString(),
            amountMl = 250,
            recordedAtMillis = System.currentTimeMillis(),
            dateIso = today,
            syncStatus = SyncStatus.PENDING_SYNC,
          )
        )
    }
    VitalTrackWidgetUpdater.refreshAll(context)
  }
}

class VitalTrackGlanceWidget : GlanceAppWidget() {
  override suspend fun provideGlance(context: Context, id: GlanceId) {
    provideContent {
      GlanceTheme {
        WidgetContent()
      }
    }
  }

  @Composable
  private fun WidgetContent() {
    val prefs = currentState<Preferences>()
    val steps = prefs[VitalTrackWidgetKeys.STEPS] ?: 0
    val stepGoal = prefs[VitalTrackWidgetKeys.STEP_GOAL] ?: 8000
    val waterMl = prefs[VitalTrackWidgetKeys.WATER_ML] ?: 0
    val waterGoalMl = prefs[VitalTrackWidgetKeys.WATER_GOAL_ML] ?: 2200
    val dateLabel = prefs[VitalTrackWidgetKeys.DATE_LABEL] ?: WellnessCalculators.todayIso()
    val topTask = prefs[VitalTrackWidgetKeys.TOP_TASK_LABEL] ?: "No urgent tasks"

    Column(
      modifier =
        GlanceModifier.fillMaxSize()
          .background(Color(0xFFF7F8F4))
          .padding(14.dp)
          .clickable(actionStartActivity<MainActivity>()),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "VITALTRACK",
          style =
            TextStyle(
              color = ColorProvider(Color(0xFF526B57)),
              fontWeight = FontWeight.Bold,
              fontSize = 13.sp,
            ),
        )
        Spacer(modifier = GlanceModifier.defaultWeight())
        Text(
          text = dateLabel,
          style = TextStyle(color = ColorProvider(Color(0xFF545E54)), fontSize = 11.sp),
        )
      }
      Spacer(modifier = GlanceModifier.height(8.dp))
      Row(modifier = GlanceModifier.fillMaxWidth()) {
        Column(modifier = GlanceModifier.defaultWeight()) {
          Text(
            text = "$steps",
            style =
              TextStyle(
                color = ColorProvider(Color(0xFF202820)),
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
              ),
          )
          Text(
            text = "/ $stepGoal steps",
            style = TextStyle(color = ColorProvider(Color(0xFF545E54)), fontSize = 11.sp),
          )
        }
        Spacer(modifier = GlanceModifier.width(8.dp))
        Column(modifier = GlanceModifier.defaultWeight()) {
          Text(
            text = "${waterMl}ml",
            style =
              TextStyle(
                color = ColorProvider(Color(0xFF4A8FA3)),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
              ),
          )
          Text(
            text = "/ ${waterGoalMl}ml water",
            style = TextStyle(color = ColorProvider(Color(0xFF545E54)), fontSize = 11.sp),
          )
        }
      }
      Spacer(modifier = GlanceModifier.height(6.dp))
      Text(
        text = "Next: $topTask",
        style = TextStyle(color = ColorProvider(Color(0xFF545E54)), fontSize = 11.sp),
        maxLines = 1,
      )
      Spacer(modifier = GlanceModifier.height(6.dp))
      Row(
        modifier =
          GlanceModifier.fillMaxWidth()
            .background(Color(0xFFD6E5D8))
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .clickable(actionRunCallback<RefreshAndAddWaterAction>()),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Text(
          text = "+250 ml Water",
          style =
            TextStyle(
              color = ColorProvider(Color(0xFF132216)),
              fontWeight = FontWeight.Medium,
              fontSize = 12.sp,
            ),
        )
      }
    }
  }
}

class VitalTrackWidgetReceiver : GlanceAppWidgetReceiver() {
  override val glanceAppWidget: GlanceAppWidget = VitalTrackGlanceWidget()
}

object VitalTrackWidgetUpdater {
  suspend fun refreshAll(context: Context) {
    runCatching {
      val db = VitalTrackDatabase.getInstance(context)
      val prefs = UserPreferencesDataStore(context).getCurrent()
      val today = WellnessCalculators.todayIso()
      val todaySteps = db.dailyStepDao().getStepsForDate(today)?.steps ?: 0
      val todayWater =
        db.healthRecordDao().getWaterSnapshot().filter { it.dateIso == today }.sumOf { it.amountMl }

      val nextTask =
        db.wellnessTaskDao().getAllTasksSnapshot().firstOrNull { !it.isCompleted }?.let {
          "[${it.priority}] ${it.title}"
        } ?: "All tasks clear"

      val manager = GlanceAppWidgetManager(context)
      val ids = manager.getGlanceIds(VitalTrackGlanceWidget::class.java)
      val widget = VitalTrackGlanceWidget()
      ids.forEach { glanceId ->
        updateAppWidgetState(context, glanceId) { state ->
          state[VitalTrackWidgetKeys.STEPS] = todaySteps
          state[VitalTrackWidgetKeys.STEP_GOAL] = prefs.dailyStepGoal
          state[VitalTrackWidgetKeys.WATER_ML] = todayWater
          state[VitalTrackWidgetKeys.WATER_GOAL_ML] = prefs.dailyWaterGoalMl
          state[VitalTrackWidgetKeys.DATE_LABEL] = today
          state[VitalTrackWidgetKeys.TOP_TASK_LABEL] = nextTask
        }
        widget.update(context, glanceId)
      }
    }
  }
}
