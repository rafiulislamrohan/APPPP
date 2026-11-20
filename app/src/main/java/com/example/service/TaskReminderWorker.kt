package com.example.service

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.MainActivity
import com.example.R
import com.example.data.local.TaskPriority
import com.example.data.local.TaskRecurrence
import com.example.data.local.VitalTrackDatabase
import com.example.data.local.WellnessTaskEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * WorkManager CoroutineWorker that delivers actionable local notifications for Room-persisted
 * WellnessTasks based on their scheduled due date and time.
 */
class TaskReminderWorker(
  appContext: Context,
  workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

  override suspend fun doWork(): Result {
    val taskId = inputData.getString(KEY_TASK_ID) ?: return Result.failure()
    return try {
      val dao = VitalTrackDatabase.getInstance(applicationContext).wellnessTaskDao()
      var task = dao.getTaskById(taskId) ?: return Result.success()
      if (task.isCompleted || !task.reminderEnabled) {
        return Result.success()
      }

      // Auto-escalate priority when within deadline window if enabled
      val now = System.currentTimeMillis()
      if (
        task.autoEscalatePriority &&
          task.priority == TaskPriority.MEDIUM.storageKey &&
          task.dueAtMillis - now <= 2L * 3_600_000L
      ) {
        task = task.copy(priority = TaskPriority.HIGH.storageKey, updatedAtMillis = now)
        dao.upsertTask(task)
      }

      val timeLabel =
        SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(task.dueAtMillis))
      val priorityLabel = TaskPriority.fromKey(task.priority).label
      val body =
        buildString {
          append("[$priorityLabel Priority • ${task.category}] Due $timeLabel")
          if (task.geofenceLabel.isNotBlank()) append(" @ ${task.geofenceLabel}")
          if (task.notes.isNotBlank()) append(" — ${task.notes}")
        }
      showActionableTaskNotification(applicationContext, task, body)
      Result.success()
    } catch (e: Exception) {
      Log.e(TAG, "Failed delivering task reminder for $taskId", e)
      Result.retry()
    }
  }

  private fun showActionableTaskNotification(
    context: Context,
    task: WellnessTaskEntity,
    body: String,
  ) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      val granted =
        ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) ==
          PackageManager.PERMISSION_GRANTED
      if (!granted) return
    }
    ReminderNotificationHelper.createChannels(context)
    val notifId = task.id.hashCode()
    val openPi =
      PendingIntent.getActivity(
        context,
        notifId,
        Intent(context, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_SINGLE_TOP },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )
    val donePi =
      PendingIntent.getBroadcast(
        context,
        notifId + 1,
        Intent(context, TaskActionBroadcastReceiver::class.java).apply {
          action = TaskActionBroadcastReceiver.ACTION_MARK_DONE
          putExtra(KEY_TASK_ID, task.id)
          putExtra(TaskActionBroadcastReceiver.EXTRA_NOTIF_ID, notifId)
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )
    val snoozePi =
      PendingIntent.getBroadcast(
        context,
        notifId + 2,
        Intent(context, TaskActionBroadcastReceiver::class.java).apply {
          action = TaskActionBroadcastReceiver.ACTION_SNOOZE_1H
          putExtra(KEY_TASK_ID, task.id)
          putExtra(TaskActionBroadcastReceiver.EXTRA_NOTIF_ID, notifId)
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )
    val notification =
      NotificationCompat.Builder(context, ReminderNotificationHelper.CHANNEL_REMINDERS)
        .setSmallIcon(R.drawable.ic_notification_cue)
        .setContentTitle("Task Due: ${task.title}")
        .setContentText(body)
        .setStyle(NotificationCompat.BigTextStyle().bigText(body))
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setAutoCancel(true)
        .setContentIntent(openPi)
        .addAction(0, "Mark Done", donePi)
        .addAction(0, "Snooze 1h", snoozePi)
        .build()
    NotificationManagerCompat.from(context).notify(notifId, notification)
  }

  companion object {
    const val KEY_TASK_ID = "task_id"
    private const val TAG = "TaskReminderWorker"
  }
}

class TaskActionBroadcastReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    val taskId = intent.getStringExtra(TaskReminderWorker.KEY_TASK_ID) ?: return
    val notifId = intent.getIntExtra(EXTRA_NOTIF_ID, taskId.hashCode())
    NotificationManagerCompat.from(context).cancel(notifId)

    val pending = goAsync()
    CoroutineScope(Dispatchers.IO).launch {
      try {
        val dao = VitalTrackDatabase.getInstance(context).wellnessTaskDao()
        val task = dao.getTaskById(taskId) ?: return@launch
        val now = System.currentTimeMillis()
        when (intent.action) {
          ACTION_MARK_DONE -> {
            val done = task.copy(isCompleted = true, completedAtMillis = now, updatedAtMillis = now)
            dao.upsertTask(done)
            TaskReminderScheduler.cancelTaskReminder(context, task.id)
            TaskReminderScheduler.createNextRecurringOccurrence(task, now)?.let { next ->
              dao.upsertTask(next)
              TaskReminderScheduler.scheduleTaskReminder(context, next, now)
            }
          }
          ACTION_SNOOZE_1H -> {
            val snoozed =
              task.copy(
                dueAtMillis = maxOf(now, task.dueAtMillis) + 3_600_000L,
                updatedAtMillis = now,
              )
            dao.upsertTask(snoozed)
            TaskReminderScheduler.scheduleTaskReminder(context, snoozed, now)
          }
        }
      } finally {
        pending.finish()
      }
    }
  }

  companion object {
    const val ACTION_MARK_DONE = "com.example.action.TASK_MARK_DONE"
    const val ACTION_SNOOZE_1H = "com.example.action.TASK_SNOOZE_1H"
    const val EXTRA_NOTIF_ID = "extra_notif_id"
  }
}

object TaskReminderScheduler {
  const val TAG_TASK_REMINDERS = "vitaltrack_task_reminders"

  fun uniqueWorkName(taskId: String): String = "vitaltrack_task_reminder_$taskId"

  fun createNextRecurringOccurrence(
    completedTask: WellnessTaskEntity,
    nowMillis: Long = System.currentTimeMillis(),
  ): WellnessTaskEntity? {
    val rule = TaskRecurrence.fromKey(completedTask.recurrenceRule)
    if (rule == TaskRecurrence.NONE) return null
    val cal = Calendar.getInstance().apply { timeInMillis = maxOf(nowMillis, completedTask.dueAtMillis) }
    when (rule) {
      TaskRecurrence.DAILY -> cal.add(Calendar.DAY_OF_YEAR, 1)
      TaskRecurrence.WEEKLY -> cal.add(Calendar.WEEK_OF_YEAR, 1)
      TaskRecurrence.WEEKDAYS -> {
        do {
          cal.add(Calendar.DAY_OF_YEAR, 1)
        } while (cal.get(Calendar.DAY_OF_WEEK) in listOf(Calendar.SATURDAY, Calendar.SUNDAY))
      }
      TaskRecurrence.NONE -> return null
    }
    return completedTask.copy(
      id = "task_${UUID.randomUUID()}",
      dueAtMillis = cal.timeInMillis,
      isCompleted = false,
      completedAtMillis = null,
      createdAtMillis = nowMillis,
      updatedAtMillis = nowMillis,
    )
  }

  fun scheduleTaskReminder(
    context: Context,
    task: WellnessTaskEntity,
    nowMillis: Long = System.currentTimeMillis(),
  ) {
    runCatching {
      val wm = WorkManager.getInstance(context)
      scheduleWithWorkManager(wm, task, nowMillis)
    }.onFailure {
      Log.w("TaskReminderScheduler", "WorkManager unavailable in current context: ${it.message}")
    }
  }

  internal fun scheduleWithWorkManager(
    workManager: WorkManager,
    task: WellnessTaskEntity,
    nowMillis: Long = System.currentTimeMillis(),
  ) {
    val workName = uniqueWorkName(task.id)
    if (task.isCompleted || !task.reminderEnabled) {
      workManager.cancelUniqueWork(workName)
      return
    }
    val delayMillis = (task.dueAtMillis - nowMillis).coerceAtLeast(0L)
    val request =
      OneTimeWorkRequestBuilder<TaskReminderWorker>()
        .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
        .setInputData(workDataOf(TaskReminderWorker.KEY_TASK_ID to task.id))
        .addTag(TAG_TASK_REMINDERS)
        .addTag(workName)
        .build()

    workManager.enqueueUniqueWork(workName, ExistingWorkPolicy.REPLACE, request)
  }

  fun cancelTaskReminder(context: Context, taskId: String) {
    runCatching {
      WorkManager.getInstance(context).cancelUniqueWork(uniqueWorkName(taskId))
    }.onFailure {
      Log.w("TaskReminderScheduler", "WorkManager unavailable in current context: ${it.message}")
    }
  }

  fun reschedulePending(context: Context, tasks: List<WellnessTaskEntity>) {
    val now = System.currentTimeMillis()
    tasks.forEach { task -> scheduleTaskReminder(context, task, now) }
  }
}
