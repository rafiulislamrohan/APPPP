package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.JetBrainsMonoFamily

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VitalTopBar(
  title: String,
  onBack: (() -> Unit)? = null,
  actions: @Composable () -> Unit = {},
) {
  TopAppBar(
    title = {
      Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    },
    navigationIcon = {
      if (onBack != null) {
        IconButton(onClick = onBack) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = stringResource(R.string.back_label),
          )
        }
      }
    },
    actions = { actions() },
    colors =
      TopAppBarDefaults.topAppBarColors(
        containerColor = MaterialTheme.colorScheme.background,
        titleContentColor = MaterialTheme.colorScheme.onBackground,
      ),
  )
}

@Composable
fun StepProgressRing(
  steps: Int,
  goal: Int,
  modifier: Modifier = Modifier,
  size: Dp = 158.dp,
  ringColor: Color = MaterialTheme.colorScheme.primary,
  accentColor: Color = MaterialTheme.colorScheme.tertiary,
  trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
  subtitle: String = "steps today",
) {
  val rawProgress = if (goal > 0) (steps.toFloat() / goal.toFloat()).coerceIn(0f, 1f) else 0f
  val animatedProgress by animateFloatAsState(
    targetValue = rawProgress,
    animationSpec = tween(durationMillis = 700),
    label = "stepRing",
  )

  Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val strokeWidth = 15.dp.toPx()
      val inset = strokeWidth / 2f + 2.dp.toPx()
      val arcSize = Size(this.size.width - inset * 2f, this.size.height - inset * 2f)

      // 3D Extruded drop shadow base underneath torus track
      drawArc(
        color = Color.Black.copy(alpha = 0.14f),
        startAngle = 0f,
        sweepAngle = 360f,
        useCenter = false,
        topLeft = Offset(inset + 1.5.dp.toPx(), inset + 4.dp.toPx()),
        size = arcSize,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
      )

      // Recessed 3D track torus
      drawArc(
        color = trackColor,
        startAngle = 0f,
        sweepAngle = 360f,
        useCenter = false,
        topLeft = Offset(inset, inset),
        size = arcSize,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
      )

      if (animatedProgress > 0f) {
        val sweep = animatedProgress * 360f
        // Progress shadow ridge
        drawArc(
          color = Color.Black.copy(alpha = 0.22f),
          startAngle = -90f,
          sweepAngle = sweep,
          useCenter = false,
          topLeft = Offset(inset + 1.dp.toPx(), inset + 3.dp.toPx()),
          size = arcSize,
          style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
        )
        // Lit gradient arc
        drawArc(
          brush = Brush.sweepGradient(listOf(ringColor, accentColor, ringColor)),
          startAngle = -90f,
          sweepAngle = sweep,
          useCenter = false,
          topLeft = Offset(inset, inset),
          size = arcSize,
          style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
        )
        // Top specular glossy highlight strand
        drawArc(
          color = Color.White.copy(alpha = 0.34f),
          startAngle = -90f,
          sweepAngle = sweep,
          useCenter = false,
          topLeft = Offset(inset + 1.5.dp.toPx(), inset - 1.5.dp.toPx()),
          size = Size((arcSize.width - 3.dp.toPx()).coerceAtLeast(1f), (arcSize.height - 3.dp.toPx()).coerceAtLeast(1f)),
          style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
        )
      }
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Text(
        text = steps.toString(),
        style = MaterialTheme.typography.headlineMedium.copy(fontFamily = JetBrainsMonoFamily),
        color = MaterialTheme.colorScheme.onSurface,
      )
      Text(
        text = "/ $goal $subtitle",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

data class BarChartDatum(
  val label: String,
  val value: Float,
  val isToday: Boolean = false,
)

@Composable
fun WeeklyBarChart(
  data: List<BarChartDatum>,
  goalValue: Float,
  modifier: Modifier = Modifier,
  barColor: Color = MaterialTheme.colorScheme.primary,
  todayColor: Color = MaterialTheme.colorScheme.tertiary,
) {
  val maxVal = maxOf(goalValue, data.maxOfOrNull { it.value } ?: 1f, 1f)
  Column(modifier = modifier.fillMaxWidth()) {
    Canvas(
      modifier =
        Modifier.fillMaxWidth()
          .height(132.dp)
          .padding(horizontal = 4.dp, vertical = 6.dp)
    ) {
      val count = data.size.coerceAtLeast(1)
      val slotWidth = size.width / count
      val barWidth = (slotWidth * 0.52f).coerceAtMost(28.dp.toPx())

      // Goal dashed/subtle reference line
      val goalY = size.height - ((goalValue / maxVal).coerceIn(0f, 1f) * size.height)
      drawLine(
        color = todayColor.copy(alpha = 0.45f),
        start = Offset(0f, goalY),
        end = Offset(size.width, goalY),
        strokeWidth = 1.5.dp.toPx(),
      )

      data.forEachIndexed { idx, item ->
        val fraction = (item.value / maxVal).coerceIn(0f, 1f)
        val barHeight = (fraction * (size.height - 8.dp.toPx())).coerceAtLeast(if (item.value > 0f) 6.dp.toPx() else 3.dp.toPx())
        val left = idx * slotWidth + (slotWidth - barWidth) / 2f
        val top = size.height - barHeight
        val baseCol = if (item.isToday) todayColor else barColor
        val depthDx = 6.dp.toPx()
        val depthDy = 5.dp.toPx()

        // 3D Floor shadow
        drawRoundRect(
          color = Color.Black.copy(alpha = 0.12f),
          topLeft = Offset(left + depthDx * 0.6f, size.height - 3.dp.toPx()),
          size = Size(barWidth + depthDx, 4.dp.toPx()),
          cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx()),
        )

        // 3D Top isometric cap face
        val topCap =
          Path().apply {
            moveTo(left, top)
            lineTo(left + depthDx, top - depthDy)
            lineTo(left + barWidth + depthDx, top - depthDy)
            lineTo(left + barWidth, top)
            close()
          }
        drawPath(
          path = topCap,
          color = androidx.compose.ui.graphics.lerp(baseCol, Color.White, 0.34f),
        )

        // 3D Right side extruded depth face
        val rightFace =
          Path().apply {
            moveTo(left + barWidth, top)
            lineTo(left + barWidth + depthDx, top - depthDy)
            lineTo(left + barWidth + depthDx, size.height - depthDy)
            lineTo(left + barWidth, size.height)
            close()
          }
        drawPath(
          path = rightFace,
          color = androidx.compose.ui.graphics.lerp(baseCol, Color.Black, 0.28f),
        )

        // Lit front column face
        drawRect(
          brush =
            Brush.verticalGradient(
              listOf(
                androidx.compose.ui.graphics.lerp(baseCol, Color.White, 0.14f),
                baseCol,
                androidx.compose.ui.graphics.lerp(baseCol, Color.Black, 0.12f),
              ),
              startY = top,
              endY = size.height,
            ),
          topLeft = Offset(left, top),
          size = Size(barWidth, barHeight),
        )
      }
    }
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
      data.forEach { item ->
        Text(
          text = item.label,
          style = MaterialTheme.typography.labelSmall,
          fontWeight = if (item.isToday) FontWeight.Bold else FontWeight.Normal,
          color =
            if (item.isToday) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

@Composable
fun LineTrendChart(
  values: List<Float>,
  labels: List<String>,
  modifier: Modifier = Modifier,
  lineColor: Color = MaterialTheme.colorScheme.primary,
) {
  if (values.isEmpty()) return
  val minV = values.minOrNull() ?: 0f
  val maxV = values.maxOrNull() ?: 1f
  val span = (maxV - minV).takeIf { it > 0.01f } ?: 1f

  Column(modifier = modifier.fillMaxWidth()) {
    Canvas(
      modifier =
        Modifier.fillMaxWidth()
          .height(110.dp)
          .padding(vertical = 8.dp, horizontal = 8.dp)
    ) {
      val stepX = if (values.size > 1) size.width / (values.size - 1) else 0f
      val pts =
        values.mapIndexed { idx, v ->
          val x = if (values.size == 1) size.width / 2f else idx * stepX
          val norm = (v - minV) / span
          val y = size.height - (norm * size.height * 0.80f + size.height * 0.10f)
          Offset(x, y)
        }
      val path =
        Path().apply {
          pts.forEachIndexed { idx, pt ->
            if (idx == 0) moveTo(pt.x, pt.y) else lineTo(pt.x, pt.y)
          }
        }
      if (pts.size > 1) {
        // 3D Extruded gradient curtain under ribbon
        val areaPath =
          Path().apply {
            moveTo(pts.first().x, size.height)
            pts.forEach { lineTo(it.x, it.y) }
            lineTo(pts.last().x, size.height)
            close()
          }
        drawPath(
          path = areaPath,
          brush = Brush.verticalGradient(listOf(lineColor.copy(alpha = 0.28f), Color.Transparent)),
        )
        // Drop shadow curve
        drawPath(
          path = path,
          color = Color.Black.copy(alpha = 0.18f),
          style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round),
        )
        // Lit curve
        drawPath(
          path = path,
          color = lineColor,
          style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
        )
      }
      pts.forEach { pt ->
        val r = 5.dp.toPx()
        drawCircle(
          color = Color.Black.copy(alpha = 0.20f),
          radius = r,
          center = Offset(pt.x + 1.5.dp.toPx(), pt.y + 2.dp.toPx()),
        )
        drawCircle(
          brush =
            Brush.radialGradient(
              listOf(
                androidx.compose.ui.graphics.lerp(lineColor, Color.White, 0.55f),
                lineColor,
                androidx.compose.ui.graphics.lerp(lineColor, Color.Black, 0.30f),
              ),
              center = Offset(pt.x - r * 0.3f, pt.y - r * 0.3f),
              radius = r * 1.4f,
            ),
          radius = r,
          center = pt,
        )
      }
    }
    if (labels.isNotEmpty()) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(
          text = labels.first(),
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (labels.size > 1) {
          Text(
            text = labels.last(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }
  }
}

@Composable
fun MetricSummaryCard(
  title: String,
  value: String,
  subtitle: String,
  icon: ImageVector,
  accentColor: Color,
  onClick: (() -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  Card(
    modifier =
      modifier
        .tactile3D(elevationDp = 6.dp, cornerRadiusDp = 18.dp)
        .then(
          if (onClick != null) Modifier.clip(MaterialTheme.shapes.medium).clickable(onClick = onClick)
          else Modifier
        ),
    shape = MaterialTheme.shapes.medium,
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier =
            Modifier.size(36.dp)
              .clip(RoundedCornerShape(12.dp))
              .background(
                Brush.linearGradient(
                  listOf(accentColor.copy(alpha = 0.28f), accentColor.copy(alpha = 0.10f))
                )
              ),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = icon,
            contentDescription = title,
            tint = accentColor,
            modifier = Modifier.size(18.dp),
          )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = title,
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
      Spacer(modifier = Modifier.height(10.dp))
      Text(
        text = value,
        style = MaterialTheme.typography.titleLarge.copy(fontFamily = JetBrainsMonoFamily),
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = subtitle,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
fun OfflineStatusBanner(modifier: Modifier = Modifier) {
  Surface(
    modifier = modifier.fillMaxWidth().testTag("offline_status_banner"),
    color = MaterialTheme.colorScheme.tertiaryContainer,
    shape = MaterialTheme.shapes.small,
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(
        imageVector = Icons.Outlined.CloudOff,
        contentDescription = "Offline",
        tint = MaterialTheme.colorScheme.onTertiaryContainer,
        modifier = Modifier.size(18.dp),
      )
      Spacer(modifier = Modifier.width(10.dp))
      Text(
        text = stringResource(R.string.offline_banner),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onTertiaryContainer,
      )
    }
  }
}

@Composable
fun MedicalDisclaimerBanner(modifier: Modifier = Modifier) {
  Surface(
    modifier = modifier.fillMaxWidth().testTag("medical_disclaimer_banner"),
    color = MaterialTheme.colorScheme.surfaceVariant,
    shape = MaterialTheme.shapes.small,
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(
        imageVector = Icons.Outlined.Info,
        contentDescription = "Disclaimer",
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(18.dp),
      )
      Spacer(modifier = Modifier.width(10.dp))
      Text(
        text = stringResource(R.string.medical_disclaimer),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}
