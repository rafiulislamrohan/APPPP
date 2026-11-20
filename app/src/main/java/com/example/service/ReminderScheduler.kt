package com.example.service

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.NotificationConfigEntity
import com.example.data.local.VitalTrackDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

object ReminderNotificationHelper {
  const val CHANNEL_REMINDERS = "vitaltrack_reminders"
  const val EXTRA_REMINDER_TITLE = "extra_reminder_title"
  const val EXTRA_REMINDER_BODY = "extra_reminder_body"
  const val EXTRA_REMINDER_ID = "extra_reminder_id"

  fun createChannels(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val nm = context.getSystemService(NotificationManager::class.java)
      val channel =
        NotificationChannel(
          CHANNEL_REMINDERS,
          context.getString(R.string.channel_reminders_name),
          NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { description = context.getString(R.string.channel_reminders_desc) }
      nm.createNotificationChannel(channel)
    }
  }

  fun showReminder(context: Context, id: Int, title: String, body: String) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      val granted =
        ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) ==
          PackageManager.PERMISSION_GRANTED
      if (!granted) return
    }
    createChannels(context)
    val openIntent =
      PendingIntent.getActivity(
        context,
        id,
        Intent(context, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_SINGLE_TOP },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )
    val notification =
      NotificationCompat.Builder(context, CHANNEL_REMINDERS)
        .setSmallIcon(R.drawable.ic_notification_cue)
        .setContentTitle(title)
        .setContentText(body)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setAutoCancel(true)
        .setContentIntent(openIntent)
        .build()
    NotificationManagerCompat.from(context).notify(id, notification)
  }
}

object ReminderScheduler {
  fun scheduleConfig(context: Context, config: NotificationConfigEntity) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val reqCode = config.id.hashCode()
    val intent =
      Intent(context, ReminderBroadcastReceiver::class.java).apply {
        putExtra(ReminderNotificationHelper.EXTRA_REMINDER_ID, reqCode)
        putExtra(ReminderNotificationHelper.EXTRA_REMINDER_TITLE, config.title)
        putExtra(
          ReminderNotificationHelper.EXTRA_REMINDER_BODY,
          when (config.type) {
            "WATER" -> "Time for a mindful glass of water to stay hydrated."
            "HABIT" -> "Check off today's wellness habits and keep your streak alive."
            "STEPS" -> "Take a brisk 5-minute walk toward your daily step goal."
            else -> "Ready for your scheduled workout session?"
          },
        )
      }
    val pi =
      PendingIntent.getBroadcast(
        context,
        reqCode,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )

    if (!config.enabled) {
      alarmManager.cancel(pi)
      return
    }

    val triggerAt =
      if (config.intervalMinutes < 1440) {
        System.currentTimeMillis() + config.intervalMinutes * 60_000L
      } else {
        Calendar.getInstance()
          .apply {
            set(Calendar.HOUR_OF_DAY, config.hourOfDay)
            set(Calendar.MINUTE, config.minute)
            set(Calendar.SECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
              add(Calendar.DAY_OF_YEAR, 1)
            }
          }
          .timeInMillis
      }

    val intervalMs = config.intervalMinutes.coerceAtLeast(30) * 60_000L
    alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, triggerAt, intervalMs, pi)
  }

  fun rescheduleAll(context: Context, configs: List<NotificationConfigEntity>) {
    configs.forEach { scheduleConfig(context, it) }
  }
}

class ReminderBroadcastReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    val id = intent.getIntExtra(ReminderNotificationHelper.EXTRA_REMINDER_ID, 3001)
    val title =
      intent.getStringExtra(ReminderNotificationHelper.EXTRA_REMINDER_TITLE)
        ?: "VITALTRACK Reminder"
    val body =
      intent.getStringExtra(ReminderNotificationHelper.EXTRA_REMINDER_BODY)
        ?: "Stay consistent with your wellness routine."
    ReminderNotificationHelper.showReminder(context, id, title, body)
  }
}

class BootCompletedReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
      val pendingResult = goAsync()
      CoroutineScope(Dispatchers.IO).launch {
        try {
          val db = VitalTrackDatabase.getInstance(context)
          val configs = db.aiAndNotificationDao().getNotificationConfigsSnapshot()
          ReminderScheduler.rescheduleAll(context, configs)
          val pendingTasks = db.wellnessTaskDao().getPendingReminderTasksSnapshot()
          TaskReminderScheduler.reschedulePending(context, pendingTasks)
        } finally {
          pendingResult.finish()
        }
      }
    }
  }
}
