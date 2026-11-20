package com.example.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.speech.RecognizerIntent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.DragHandle
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardDoubleArrowUp
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.health.BiofeedbackAudioEngine
import com.example.data.health.WellnessCalculators
import com.example.data.local.SeedExerciseLibrary
import com.example.data.local.TaskCategoryEntity
import com.example.data.local.TaskPriority
import com.example.data.local.TaskRecurrence
import com.example.data.local.TaskSubtask
import com.example.data.local.WellnessTaskEntity
import com.example.ui.components.VitalTopBar
import com.example.ui.components.tactile3D
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.PriorityHighContainerDark
import com.example.ui.theme.PriorityHighContainerLight
import com.example.ui.theme.PriorityHighDark
import com.example.ui.theme.PriorityHighLight
import com.example.ui.theme.PriorityLowContainerDark
import com.example.ui.theme.PriorityLowContainerLight
import com.example.ui.theme.PriorityLowDark
import com.example.ui.theme.PriorityLowLight
import com.example.ui.theme.PriorityMediumContainerDark
import com.example.ui.theme.PriorityMediumContainerLight
import com.example.ui.theme.PriorityMediumDark
import com.example.ui.theme.PriorityMediumLight
import kotlinx.coroutines.delay
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Date
import java.util.Locale

private val taskSubJson = Json { ignoreUnknownKeys = true }

private fun decodeSubtasks(raw: String): List<TaskSubtask> =
  runCatching { taskSubJson.decodeFromString<List<TaskSubtask>>(raw) }.getOrDefault(emptyList())

private data class PriorityVisualStyle(
  val accent: Color,
  val container: Color,
  val icon: ImageVector,
)

@Composable
private fun priorityVisualStyle(priority: TaskPriority): PriorityVisualStyle {
  val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f || isSystemInDarkTheme() && MaterialTheme.colorScheme.background.luminance() < 0.5f
  return when (priority) {
    TaskPriority.HIGH ->
      PriorityVisualStyle(
        accent = if (isDark) PriorityHighDark else PriorityHighLight,
        container = if (isDark) PriorityHighContainerDark else PriorityHighContainerLight,
        icon = Icons.Outlined.KeyboardDoubleArrowUp,
      )
    TaskPriority.MEDIUM ->
      PriorityVisualStyle(
        accent = if (isDark) PriorityMediumDark else PriorityMediumLight,
        container = if (isDark) PriorityMediumContainerDark else PriorityMediumContainerLight,
        icon = Icons.Outlined.DragHandle,
      )
    TaskPriority.LOW ->
      PriorityVisualStyle(
        accent = if (isDark) PriorityLowDark else PriorityLowLight,
        container = if (isDark) PriorityLowContainerDark else PriorityLowContainerLight,
        icon = Icons.Outlined.KeyboardArrowDown,
      )
  }
}

@Composable
fun WellnessTasksScreen(
  tasks: List<WellnessTaskEntity>,
  categories: List<TaskCategoryEntity> = SeedExerciseLibrary.defaultTaskCategories,
  onSaveTask: (
    existingId: String?,
    title: String,
    notes: String,
    category: String,
    priority: String,
    dueAtMillis: Long,
    reminderEnabled: Boolean,
  ) -> Unit,
  onToggleCompleted: (String) -> Unit,
  onDeleteTask: (String) -> Unit,
  onSnoozeTask: (String) -> Unit = {},
  onAddCategory: (String) -> Unit = {},
  onSaveTaskAdvanced: ((
    existingId: String?,
    title: String,
    notes: String,
    category: String,
    priority: String,
    dueAtMillis: Long,
    reminderEnabled: Boolean,
    recurrenceRule: String,
    subtasksJson: String,
    geofenceLabel: String,
    autoEscalatePriority: Boolean,
  ) -> Unit)? = null,
  onUpdateSubtasksAndFocus: (taskId: String, subtasksJson: String, addedFocusMin: Int) -> Unit = { _, _, _ -> },
  onBack: () -> Unit,
) {
  BackHandler(onBack = onBack)

  val context = LocalContext.current
  var searchQuery by remember { mutableStateOf("") }
  var statusFilter by remember { mutableStateOf("PENDING") } // PENDING, COMPLETED, ALL
  var selectedCategoryFilter by remember { mutableStateOf("ALL") } // ALL or category name
  var selectedPriorityFilter by remember { mutableStateOf("ALL") } // ALL, HIGH, MEDIUM, LOW
  var viewMode by remember { mutableStateOf("LIST") } // LIST, TIMELINE, MATRIX
  var editingTask by remember { mutableStateOf<WellnessTaskEntity?>(null) }
  var prefillVoiceTitle by remember { mutableStateOf<String?>(null) }
  var focusTask by remember { mutableStateOf<WellnessTaskEntity?>(null) }
  var showEditor by remember { mutableStateOf(false) }
  var showAddCategoryDialog by remember { mutableStateOf(false) }

  val voiceLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
      if (result.resultCode == Activity.RESULT_OK) {
        val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        if (!spoken.isNullOrBlank()) {
          editingTask = null
          prefillVoiceTitle = spoken
          showEditor = true
        }
      }
    }

  val effectiveCategories =
    remember(categories, tasks) {
      val base = categories.ifEmpty { SeedExerciseLibrary.defaultTaskCategories }.map { it.name.uppercase() }
      val fromTasks = tasks.map { it.category.uppercase() }
      (base + fromTasks).distinct()
    }

  var notifGranted by remember {
    mutableStateOf(
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
          PackageManager.PERMISSION_GRANTED
      } else {
        true
      }
    )
  }

  val notifPermLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
      notifGranted = granted
    }

  val pendingCount = remember(tasks) { tasks.count { !it.isCompleted } }
  val scheduledAlertsCount = remember(tasks) { tasks.count { !it.isCompleted && it.reminderEnabled } }
  val completedCount = remember(tasks) { tasks.count { it.isCompleted } }

  val visibleTasks =
    remember(tasks, searchQuery, statusFilter, selectedCategoryFilter, selectedPriorityFilter) {
      val trimmedQuery = searchQuery.trim()
      tasks
        .filter { task ->
          val queryOk =
            trimmedQuery.isEmpty() ||
              task.title.contains(trimmedQuery, ignoreCase = true) ||
              task.notes.contains(trimmedQuery, ignoreCase = true) ||
              task.category.contains(trimmedQuery, ignoreCase = true)
          val statusOk =
            when (statusFilter) {
              "PENDING" -> !task.isCompleted
              "COMPLETED" -> task.isCompleted
              else -> true
            }
          val categoryOk =
            selectedCategoryFilter == "ALL" ||
              task.category.equals(selectedCategoryFilter, ignoreCase = true)
          val priorityOk =
            selectedPriorityFilter == "ALL" ||
              TaskPriority.fromKey(task.priority).storageKey == selectedPriorityFilter
          queryOk && statusOk && categoryOk && priorityOk
        }
        .sortedWith(
          compareBy<WellnessTaskEntity> { it.isCompleted }
            .thenBy { TaskPriority.fromKey(it.priority).sortRank }
            .thenBy { it.dueAtMillis }
        )
    }

  val dateFormat = remember { SimpleDateFormat("EEE, MMM d • h:mm a", Locale.getDefault()) }

  Scaffold(
    topBar = {
      VitalTopBar(
        title = "Wellness Tasks & Reminders",
        onBack = onBack,
        actions = {
          IconButton(
            onClick = {
              val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak a task (e.g. Evening 5km walk at 6pm)")
              }
              runCatching { voiceLauncher.launch(intent) }.onFailure {
                editingTask = null
                prefillVoiceTitle = null
                showEditor = true
              }
            },
            modifier = Modifier.testTag("task_voice_btn"),
          ) {
            Icon(Icons.Outlined.Mic, contentDescription = "Voice to task")
          }
        },
      )
    },
    floatingActionButton = {
      FloatingActionButton(
        onClick = {
          editingTask = null
          prefillVoiceTitle = null
          showEditor = true
        },
        modifier = Modifier.testTag("add_task_fab"),
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
      ) {
        Icon(Icons.Outlined.Add, contentDescription = "Add task")
      }
    },
  ) { innerPadding ->
    Column(
      modifier =
        Modifier.fillMaxSize()
          .padding(innerPadding)
          .widthIn(max = 640.dp),
    ) {
      OutlinedTextField(
        value = searchQuery,
        onValueChange = { searchQuery = it },
        placeholder = { Text("Search tasks by title or notes...") },
        leadingIcon = {
          Icon(
            imageVector = Icons.Outlined.Search,
            contentDescription = "Search tasks",
          )
        },
        trailingIcon = {
          if (searchQuery.isNotEmpty()) {
            IconButton(
              onClick = { searchQuery = "" },
              modifier = Modifier.testTag("task_search_clear"),
            ) {
              Icon(
                imageVector = Icons.Outlined.Clear,
                contentDescription = "Clear search query",
              )
            }
          }
        },
        singleLine = true,
        shape = MaterialTheme.shapes.medium,
        modifier =
          Modifier.fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp)
            .testTag("task_search_input"),
      )

      LazyColumn(
        modifier = Modifier.weight(1f).fillMaxWidth(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
      item {
        Surface(
          shape = MaterialTheme.shapes.medium,
          color = MaterialTheme.colorScheme.primaryContainer,
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              Icons.Outlined.TaskAlt,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onPrimaryContainer,
              modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              "$pendingCount pending • $scheduledAlertsCount reminder(s) queued • $completedCount completed",
              style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFamily),
              color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
          }
        }
      }

      // Compact 2-Row Filtering UI: Row 1 (Status + Priority), Row 2 (Categories)
      item {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
          Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            listOf(
              "PENDING" to "Pending ($pendingCount)",
              "COMPLETED" to "Done ($completedCount)",
              "ALL" to "All (${tasks.size})",
            ).forEach { (key, label) ->
              FilterChip(
                selected = statusFilter == key,
                onClick = { statusFilter = key },
                label = { Text(label) },
                modifier = Modifier.testTag("task_filter_${key.lowercase()}"),
              )
            }
            Spacer(modifier = Modifier.width(4.dp))
            FilterChip(
              selected = selectedPriorityFilter == "ALL",
              onClick = { selectedPriorityFilter = "ALL" },
              label = { Text("All Priorities") },
              modifier = Modifier.testTag("task_priority_filter_all"),
            )
            TaskPriority.entries.forEach { priority ->
              val pStyle = priorityVisualStyle(priority)
              FilterChip(
                selected = selectedPriorityFilter == priority.storageKey,
                onClick = { selectedPriorityFilter = priority.storageKey },
                leadingIcon = {
                  Box(
                    modifier =
                      Modifier.size(10.dp)
                        .clip(CircleShape)
                        .background(pStyle.accent)
                  )
                },
                label = { Text(priority.label) },
                modifier = Modifier.testTag("task_priority_filter_${priority.storageKey.lowercase()}"),
              )
            }
          }

          Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            FilterChip(
              selected = selectedCategoryFilter == "ALL",
              onClick = { selectedCategoryFilter = "ALL" },
              label = { Text("All Categories") },
              modifier = Modifier.testTag("task_category_filter_all"),
            )
            effectiveCategories.forEach { catName ->
              FilterChip(
                selected = selectedCategoryFilter.equals(catName, ignoreCase = true),
                onClick = { selectedCategoryFilter = catName },
                label = { Text(catName) },
                modifier = Modifier.testTag("task_category_filter_${catName.lowercase()}"),
              )
            }
            FilterChip(
              selected = false,
              onClick = { showAddCategoryDialog = true },
              leadingIcon = {
                Icon(Icons.Outlined.Add, contentDescription = "Add category", modifier = Modifier.size(16.dp))
              },
              label = { Text("Category") },
              modifier = Modifier.testTag("task_add_category_chip"),
            )
          }
          Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            listOf("LIST" to "List", "TIMELINE" to "Timeline", "MATRIX" to "2×2 Matrix").forEach { (mode, label) ->
              FilterChip(
                selected = viewMode == mode,
                onClick = { viewMode = mode },
                label = { Text(label) },
                modifier = Modifier.testTag("task_view_${mode.lowercase()}"),
              )
            }
          }
        }
      }

      if (viewMode == "MATRIX" && visibleTasks.isNotEmpty()) {
        item {
          val nowMs = System.currentTimeMillis()
          val urgentImportant = visibleTasks.filter { !it.isCompleted && it.priority == "HIGH" && it.dueAtMillis <= nowMs + 86_400_000L }
          val scheduledImportant = visibleTasks.filter { !it.isCompleted && it.priority == "HIGH" && it.dueAtMillis > nowMs + 86_400_000L }
          val delegateMed = visibleTasks.filter { !it.isCompleted && it.priority == "MEDIUM" }
          val deferLow = visibleTasks.filter { !it.isCompleted && it.priority == "LOW" }
          Card(shape = MaterialTheme.shapes.medium, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Text("Eisenhower 2×2 Priority Matrix", style = MaterialTheme.typography.titleSmall)
              Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surface) {
                  Text("Q1 Do First (${urgentImportant.size})\n" + urgentImportant.joinToString("\n") { "• ${it.title}" }.ifBlank { "—" }, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(8.dp))
                }
                Surface(modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surface) {
                  Text("Q2 Schedule (${scheduledImportant.size})\n" + scheduledImportant.joinToString("\n") { "• ${it.title}" }.ifBlank { "—" }, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(8.dp))
                }
              }
              Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surface) {
                  Text("Q3 Routine (${delegateMed.size})\n" + delegateMed.joinToString("\n") { "• ${it.title}" }.ifBlank { "—" }, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(8.dp))
                }
                Surface(modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surface) {
                  Text("Q4 Later (${deferLow.size})\n" + deferLow.joinToString("\n") { "• ${it.title}" }.ifBlank { "—" }, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(8.dp))
                }
              }
            }
          }
        }
      }

      if (visibleTasks.isEmpty()) {
        item {
          Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          ) {
            Column(
              modifier = Modifier.fillMaxWidth().padding(24.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Icon(
                Icons.Outlined.TaskAlt,
                contentDescription = "No tasks",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp),
              )
              Text("No tasks match these filters", style = MaterialTheme.typography.titleMedium)
              Text(
                "Tap + to add a task or adjust your status, category, or priority filters.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      } else {
        val displayTasks = if (viewMode == "TIMELINE") visibleTasks.sortedBy { it.dueAtMillis } else visibleTasks
        items(displayTasks, key = { it.id }) { task ->
          val isOverdue = !task.isCompleted && task.dueAtMillis < System.currentTimeMillis()
          val taskPriority = TaskPriority.fromKey(task.priority)
          val priorityStyle = priorityVisualStyle(taskPriority)
          Card(
            modifier =
              Modifier.fillMaxWidth()
                .tactile3D(elevationDp = 6.dp, cornerRadiusDp = 16.dp)
                .clip(MaterialTheme.shapes.medium)
                .clickable {
                  editingTask = task
                  showEditor = true
                }
                .testTag("task_card_${task.id}"),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(start = 10.dp, top = 10.dp, bottom = 10.dp, end = 8.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              // Color-coded priority indicator pill along the left edge
              Box(
                modifier =
                  Modifier.width(6.dp)
                    .height(48.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(
                      if (task.isCompleted) priorityStyle.accent.copy(alpha = 0.45f)
                      else priorityStyle.accent
                    )
                    .semantics { contentDescription = "${taskPriority.label} priority indicator" }
                    .testTag("task_priority_bar_${task.id}")
              )

              Spacer(modifier = Modifier.width(4.dp))

              Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Checkbox(
                  checked = task.isCompleted,
                  onCheckedChange = { onToggleCompleted(task.id) },
                  modifier = Modifier.testTag("task_check_${task.id}"),
                )
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                  Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                  )
                  if (task.notes.isNotBlank()) {
                    Text(
                      text = task.notes,
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                  Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Surface(
                      shape = RoundedCornerShape(8.dp),
                      color = priorityStyle.container,
                      modifier = Modifier.testTag("task_priority_badge_${task.id}"),
                    ) {
                      Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                      ) {
                        Icon(
                          imageVector = priorityStyle.icon,
                          contentDescription = "${taskPriority.label} priority",
                          tint = priorityStyle.accent,
                          modifier = Modifier.size(13.dp),
                        )
                        Text(
                          text = taskPriority.label,
                          style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                          color = priorityStyle.accent,
                        )
                      }
                    }

                    Surface(
                      shape = RoundedCornerShape(8.dp),
                      color = MaterialTheme.colorScheme.surfaceVariant,
                    ) {
                      Text(
                        text = task.category,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                      )
                    }

                    Text(
                      text = dateFormat.format(Date(task.dueAtMillis)),
                      style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                      color = if (isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    )
                    Icon(
                      imageVector =
                        if (task.reminderEnabled && !task.isCompleted) Icons.Outlined.NotificationsActive
                        else Icons.Outlined.NotificationsOff,
                      contentDescription = if (task.reminderEnabled) "Reminder scheduled" else "Reminder off",
                      modifier = Modifier.size(15.dp),
                      tint =
                        if (task.reminderEnabled && !task.isCompleted) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    if (!task.isCompleted) {
                      Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier =
                          Modifier.clip(RoundedCornerShape(8.dp))
                            .clickable { onSnoozeTask(task.id) }
                            .testTag("task_snooze_${task.id}"),
                      ) {
                        Text(
                          text = "+1d",
                          style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                          color = MaterialTheme.colorScheme.onSecondaryContainer,
                          modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                      }
                      Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier =
                          Modifier.clip(RoundedCornerShape(8.dp))
                            .clickable { focusTask = task }
                            .testTag("task_focus_${task.id}"),
                      ) {
                        Text(
                          text = if (task.focusMinutesLogged > 0) "Focus (${task.focusMinutesLogged}m)" else "Focus 25m",
                          style = MaterialTheme.typography.labelSmall,
                          color = MaterialTheme.colorScheme.onPrimaryContainer,
                          modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                      }
                    }
                    if (task.recurrenceRule != TaskRecurrence.NONE.storageKey) {
                      Icon(Icons.Outlined.Repeat, contentDescription = "Recurring task", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                    if (task.geofenceLabel.isNotBlank()) {
                      Text("@${task.geofenceLabel}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
                    }
                    val subs = remember(task.subtasksJson) { decodeSubtasks(task.subtasksJson) }
                    if (subs.isNotEmpty()) {
                      Text("${subs.count { it.done }}/${subs.size} steps", style = MaterialTheme.typography.labelSmall)
                    }
                  }
                }
                IconButton(
                  onClick = { onDeleteTask(task.id) },
                  modifier = Modifier.testTag("task_delete_${task.id}"),
                ) {
                  Icon(Icons.Outlined.DeleteOutline, contentDescription = "Delete task")
                }
              }
            }
          }
        }
      }

      if (!notifGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        item {
          Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
            shape = MaterialTheme.shapes.medium,
          ) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(14.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  "Enable Notification Permission",
                  style = MaterialTheme.typography.titleSmall,
                  color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
                Text(
                  "Allow alerts so WorkManager can deliver your scheduled due-date task reminders.",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
              }
              Spacer(modifier = Modifier.width(8.dp))
              OutlinedButton(
                onClick = { notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                modifier = Modifier.testTag("task_request_notif_perm"),
              ) {
                Text("Allow")
              }
            }
          }
        }
      }
    }
  }
  }

  if (showEditor) {
    TaskEditorDialog(
      initialTask = editingTask,
      prefillTitle = prefillVoiceTitle,
      availableCategories = effectiveCategories,
      onDismiss = {
        showEditor = false
        prefillVoiceTitle = null
      },
      onSave = { title, notes, category, priority, dueAtMillis, reminderEnabled, recurrence, subtasksJson, geofence, autoEscalate ->
        if (onSaveTaskAdvanced != null &&
          (recurrence != TaskRecurrence.NONE.storageKey || subtasksJson != "[]" || geofence.isNotBlank() || autoEscalate)
        ) {
          onSaveTaskAdvanced(editingTask?.id, title, notes, category, priority, dueAtMillis, reminderEnabled, recurrence, subtasksJson, geofence, autoEscalate)
        } else {
          onSaveTask(editingTask?.id, title, notes, category, priority, dueAtMillis, reminderEnabled)
        }
        showEditor = false
        prefillVoiceTitle = null
      },
    )
  }

  val activeFocusTask = focusTask
  if (activeFocusTask != null) {
    TaskFocusAndSubtasksDialog(
      task = activeFocusTask,
      onDismiss = { focusTask = null },
      onSaveProgress = { subtasksJson, addedMin ->
        onUpdateSubtasksAndFocus(activeFocusTask.id, subtasksJson, addedMin)
        focusTask = null
      },
    )
  }

  if (showAddCategoryDialog) {
    AddTaskCategoryDialog(
      onDismiss = { showAddCategoryDialog = false },
      onConfirm = { newCategoryName ->
        onAddCategory(newCategoryName)
        selectedCategoryFilter = newCategoryName.trim().uppercase()
        showAddCategoryDialog = false
      },
    )
  }
}

@Composable
private fun TaskFocusAndSubtasksDialog(
  task: WellnessTaskEntity,
  onDismiss: () -> Unit,
  onSaveProgress: (subtasksJson: String, addedFocusMin: Int) -> Unit,
) {
  val context = LocalContext.current
  var subtasks by remember(task) { mutableStateOf(decodeSubtasks(task.subtasksJson)) }
  var newSubtask by remember { mutableStateOf("") }
  var running by remember { mutableStateOf(false) }
  var secLeft by remember { mutableIntStateOf(25 * 60) }

  LaunchedEffect(running) {
    while (running && secLeft > 0) {
      delay(1000L)
      secLeft--
      if (secLeft == 0) {
        running = false
        BiofeedbackAudioEngine.playShortTone()
        BiofeedbackAudioEngine.triggerHapticPulse(context, 180L, 160)
      }
    }
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Focus & Subtasks: ${task.title}") },
    text = {
      Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(10.dp)) {
          Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(
              "Pomodoro: ${WellnessCalculators.formatDurationMillis(secLeft * 1000L)}",
              style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFamily),
            )
            OutlinedButton(onClick = { running = !running }) { Text(if (running) "Pause" else "Start") }
          }
        }
        subtasks.forEachIndexed { idx, sub ->
          Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = sub.done, onCheckedChange = { checked ->
              subtasks = subtasks.mapIndexed { i, item -> if (i == idx) item.copy(done = checked) else item }
            })
            Text(sub.title, style = MaterialTheme.typography.bodySmall)
          }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
          OutlinedTextField(value = newSubtask, onValueChange = { newSubtask = it }, label = { Text("Add subtask step") }, modifier = Modifier.weight(1f), singleLine = true)
          OutlinedButton(enabled = newSubtask.isNotBlank(), onClick = {
            subtasks = subtasks + TaskSubtask(newSubtask.trim(), false)
            newSubtask = ""
          }) { Text("Add") }
        }
      }
    },
    confirmButton = {
      Button(onClick = {
        val elapsedMin = ((25 * 60 - secLeft) / 60).coerceAtLeast(0)
        onSaveProgress(taskSubJson.encodeToString(subtasks), elapsedMin)
      }) { Text("Save Progress") }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Close") }
    },
  )
}

@Composable
private fun AddTaskCategoryDialog(
  onDismiss: () -> Unit,
  onConfirm: (String) -> Unit,
) {
  var name by remember { mutableStateOf("") }
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("New Task Category") },
    text = {
      OutlinedTextField(
        value = name,
        onValueChange = { name = it },
        label = { Text("Category Name (e.g., Nutrition)") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("task_new_category_input"),
      )
    },
    confirmButton = {
      Button(
        enabled = name.isNotBlank(),
        onClick = { onConfirm(name) },
        modifier = Modifier.testTag("task_new_category_save"),
      ) {
        Text(stringResource(R.string.save_label))
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel_label)) }
    },
  )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun TaskEditorDialog(
  initialTask: WellnessTaskEntity?,
  prefillTitle: String? = null,
  availableCategories: List<String>,
  onDismiss: () -> Unit,
  onSave: (
    title: String,
    notes: String,
    category: String,
    priority: String,
    dueAtMillis: Long,
    reminderEnabled: Boolean,
    recurrenceRule: String,
    subtasksJson: String,
    geofenceLabel: String,
    autoEscalatePriority: Boolean,
  ) -> Unit,
) {
  val defaultDueMillis = remember(initialTask) {
    initialTask?.dueAtMillis ?: (System.currentTimeMillis() + 60 * 60 * 1000L)
  }
  var title by remember(initialTask, prefillTitle) { mutableStateOf(initialTask?.title ?: prefillTitle.orEmpty()) }
  var notes by remember(initialTask) { mutableStateOf(initialTask?.notes.orEmpty()) }
  var category by remember(initialTask) { mutableStateOf(initialTask?.category ?: "WELLNESS") }
  var priority by remember(initialTask) {
    mutableStateOf(TaskPriority.fromKey(initialTask?.priority).storageKey)
  }
  var dueAtMillis by remember(initialTask) { mutableLongStateOf(defaultDueMillis) }
  var reminderEnabled by remember(initialTask) { mutableStateOf(initialTask?.reminderEnabled ?: true) }
  var recurrence by remember(initialTask) { mutableStateOf(TaskRecurrence.fromKey(initialTask?.recurrenceRule).storageKey) }
  var geofenceLabel by remember(initialTask) { mutableStateOf(initialTask?.geofenceLabel.orEmpty()) }
  var autoEscalate by remember(initialTask) { mutableStateOf(initialTask?.autoEscalatePriority ?: false) }
  var subtasksCsv by remember(initialTask) {
    mutableStateOf(decodeSubtasks(initialTask?.subtasksJson ?: "[]").joinToString(", ") { it.title })
  }
  var showAdvanced by remember { mutableStateOf(false) }

  var showDatePicker by remember { mutableStateOf(false) }
  var showTimePicker by remember { mutableStateOf(false) }

  val zoneId = remember { ZoneId.systemDefault() }
  val currentZoned = remember(dueAtMillis) { Instant.ofEpochMilli(dueAtMillis).atZone(zoneId) }
  val displayFormat = remember { SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault()) }
  val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }

  val categoryChoices =
    remember(availableCategories, category) {
      (availableCategories + category.uppercase()).distinct()
    }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(if (initialTask == null) "New Wellness Task" else "Edit Wellness Task") },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        OutlinedTextField(
          value = title,
          onValueChange = { title = it },
          label = { Text("Task Title") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("task_title_input"),
        )
        OutlinedTextField(
          value = notes,
          onValueChange = { notes = it },
          label = { Text("Notes (Optional)") },
          maxLines = 3,
          modifier = Modifier.fillMaxWidth().testTag("task_notes_input"),
        )
        Text("Priority Level", style = MaterialTheme.typography.labelLarge)
        FlowRow(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          TaskPriority.entries.forEach { p ->
            val pStyle = priorityVisualStyle(p)
            FilterChip(
              selected = priority == p.storageKey,
              onClick = { priority = p.storageKey },
              leadingIcon = {
                Box(
                  modifier =
                    Modifier.size(10.dp)
                      .clip(CircleShape)
                      .background(pStyle.accent)
                )
              },
              label = { Text(p.label) },
              modifier = Modifier.testTag("task_priority_${p.storageKey.lowercase()}"),
            )
          }
        }
        Text("Category", style = MaterialTheme.typography.labelLarge)
        FlowRow(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          categoryChoices.forEach { cat ->
            FilterChip(
              selected = category.equals(cat, ignoreCase = true),
              onClick = { category = cat },
              label = { Text(cat) },
              modifier = Modifier.testTag("task_category_${cat.lowercase()}"),
            )
          }
        }
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          OutlinedButton(
            onClick = { showDatePicker = true },
            modifier = Modifier.weight(1f).testTag("task_pick_date_button"),
          ) {
            Icon(Icons.Outlined.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(displayFormat.format(Date(dueAtMillis)))
          }
          OutlinedButton(
            onClick = { showTimePicker = true },
            modifier = Modifier.testTag("task_pick_time_button"),
          ) {
            Icon(Icons.Outlined.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(timeFormat.format(Date(dueAtMillis)))
          }
        }
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text("Schedule Local Notification", style = MaterialTheme.typography.titleSmall)
            Text(
              "Uses WorkManager to alert at due date & time",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          Switch(
            checked = reminderEnabled,
            onCheckedChange = { reminderEnabled = it },
            modifier = Modifier.testTag("task_reminder_switch"),
          )
        }
        if (initialTask == null) {
          Text("Quick Presets", style = MaterialTheme.typography.labelMedium)
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            listOf(
              Triple("Drink 500ml Water", "HYDRATION", TaskPriority.MEDIUM.storageKey),
              Triple("15m Mobility Stretch", "WORKOUT", TaskPriority.HIGH.storageKey),
              Triple("Evening Walk", "WELLNESS", TaskPriority.LOW.storageKey),
              Triple("Take Daily Vitamins", "MEDICATION", TaskPriority.HIGH.storageKey),
            ).forEach { (presetTitle, presetCat, presetPri) ->
              FilterChip(
                selected = title == presetTitle,
                onClick = {
                  title = presetTitle
                  category = presetCat
                  priority = presetPri
                },
                label = { Text(presetTitle) },
              )
            }
          }
          TextButton(onClick = { showAdvanced = !showAdvanced }) {
            Text(if (showAdvanced) "Hide Recurrence & Subtasks" else "Recurrence, Subtasks & Auto-Escalate")
          }
          if (showAdvanced) {
            Text("Recurrence", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
              TaskRecurrence.entries.forEach { r ->
                FilterChip(selected = recurrence == r.storageKey, onClick = { recurrence = r.storageKey }, label = { Text(r.label) })
              }
            }
            OutlinedTextField(
              value = geofenceLabel,
              onValueChange = { geofenceLabel = it },
              label = { Text("Location Context Tag (e.g., Gym, Office)") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
              value = subtasksCsv,
              onValueChange = { subtasksCsv = it },
              label = { Text("Subtasks (comma separated)") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth(),
            )
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
              Text("Auto-escalate to High 2h before due", style = MaterialTheme.typography.bodySmall)
              Switch(checked = autoEscalate, onCheckedChange = { autoEscalate = it })
            }
          }
        }
      }
    },
    confirmButton = {
      Button(
        enabled = title.isNotBlank(),
        onClick = {
          val subList = subtasksCsv.split(",").mapNotNull { s -> s.trim().takeIf { it.isNotEmpty() }?.let { TaskSubtask(it, false) } }
          val subJson = taskSubJson.encodeToString(subList)
          onSave(title, notes, category, priority, dueAtMillis, reminderEnabled, recurrence, subJson, geofenceLabel.trim(), autoEscalate)
        },
        modifier = Modifier.testTag("task_save_button"),
      ) {
        Text(stringResource(R.string.save_label))
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel_label)) }
    },
  )

  if (showDatePicker) {
    val initialUtcMillis =
      currentZoned.toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialUtcMillis)
    DatePickerDialog(
      onDismissRequest = { showDatePicker = false },
      confirmButton = {
        TextButton(
          onClick = {
            val selectedUtc = datePickerState.selectedDateMillis
            if (selectedUtc != null) {
              val chosenDate = Instant.ofEpochMilli(selectedUtc).atZone(ZoneOffset.UTC).toLocalDate()
              val localTime = currentZoned.toLocalTime()
              dueAtMillis = chosenDate.atTime(localTime).atZone(zoneId).toInstant().toEpochMilli()
            }
            showDatePicker = false
          }
        ) { Text("OK") }
      },
      dismissButton = {
        TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.cancel_label)) }
      },
    ) {
      DatePicker(state = datePickerState)
    }
  }

  if (showTimePicker) {
    val timePickerState =
      rememberTimePickerState(
        initialHour = currentZoned.hour,
        initialMinute = currentZoned.minute,
        is24Hour = false,
      )
    AlertDialog(
      onDismissRequest = { showTimePicker = false },
      title = { Text("Select Due Time") },
      text = { TimePicker(state = timePickerState) },
      confirmButton = {
        TextButton(
          onClick = {
            val chosenTime = LocalTime.of(timePickerState.hour, timePickerState.minute)
            val chosenDate: LocalDate = currentZoned.toLocalDate()
            dueAtMillis = chosenDate.atTime(chosenTime).atZone(zoneId).toInstant().toEpochMilli()
            showTimePicker = false
          }
        ) { Text("OK") }
      },
      dismissButton = {
        TextButton(onClick = { showTimePicker = false }) { Text(stringResource(R.string.cancel_label)) }
      },
    )
  }
}
