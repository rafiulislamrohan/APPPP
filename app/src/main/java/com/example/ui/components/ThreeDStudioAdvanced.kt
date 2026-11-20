package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.data.local.SleepRecordEntity
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.HeartRateCoral
import com.example.ui.theme.HydrationBlue
import com.example.ui.theme.SleepIndigo
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private data class SpatialPt3(val x: Float, val y: Float, val z: Float)

private fun rot3(p: SpatialPt3, yawDeg: Float, pitchDeg: Float): SpatialPt3 {
  val yRad = (yawDeg * PI / 180.0).toFloat()
  val pRad = (pitchDeg * PI / 180.0).toFloat()
  val x1 = p.x * cos(yRad) + p.z * sin(yRad)
  val z1 = -p.x * sin(yRad) + p.z * cos(yRad)
  val y2 = p.y * cos(pRad) - z1 * sin(pRad)
  val z2 = p.y * sin(pRad) + z1 * cos(pRad)
  return SpatialPt3(x1, y2, z2)
}

private fun proj3(p: SpatialPt3, c: Offset, scale: Float): Pair<Offset, Float> {
  val cam = 4.4f
  val persp = cam / (cam - p.z).coerceAtLeast(0.8f)
  return Offset(c.x + p.x * scale * persp, c.y + p.y * scale * persp) to persp
}

private fun DrawScope.drawLitSphere(center: Offset, r: Float, color: Color) {
  if (r <= 1f) return
  drawCircle(Color.Black.copy(alpha = 0.22f), r * 1.04f, Offset(center.x + 2.dp.toPx(), center.y + 3.dp.toPx()))
  drawCircle(
    brush =
      Brush.radialGradient(
        colors = listOf(lerp(color, Color.White, 0.55f), color, lerp(color, Color.Black, 0.42f)),
        center = Offset(center.x - r * 0.35f, center.y - r * 0.35f),
        radius = r * 1.4f,
      ),
    radius = r,
    center = center,
  )
}

private fun DrawScope.drawLitBone(a: Offset, b: Offset, width: Float, color: Color) {
  drawLine(lerp(color, Color.Black, 0.4f), Offset(a.x + 2.dp.toPx(), a.y + 2.dp.toPx()), Offset(b.x + 2.dp.toPx(), b.y + 2.dp.toPx()), width, StrokeCap.Round)
  drawLine(
    brush = Brush.linearGradient(listOf(lerp(color, Color.White, 0.28f), color, lerp(color, Color.Black, 0.2f)), a, b),
    start = a,
    end = b,
    strokeWidth = width,
    cap = StrokeCap.Round,
  )
}

/**
 * #1 Morphable 3D Body Composition + #2 Muscle Recovery Heatmap + #28 3D Outfit Accessory Layers.
 */
@Composable
fun Morphable3DBodyCanvas(
  chestCm: Double,
  waistCm: Double,
  hipCm: Double,
  upperFatigueRatio: Float,
  coreFatigueRatio: Float,
  legFatigueRatio: Float,
  showMuscleHeatmap: Boolean,
  showJacket: Boolean,
  showScarf: Boolean,
  showHat: Boolean,
  showBelt: Boolean,
  topColor: Color,
  bottomColor: Color,
  accentColor: Color,
  yawDeg: Float,
  onYawDrag: (Float) -> Unit,
  modifier: Modifier = Modifier,
) {
  Canvas(
    modifier =
      modifier
        .fillMaxWidth()
        .height(250.dp)
        .pointerInput(Unit) {
          detectDragGestures { change, drag ->
            change.consume()
            onYawDrag(drag.x * 0.55f)
          }
        },
  ) {
    val c = Offset(size.width / 2f, size.height * 0.48f)
    val sc = size.minDimension * 0.35f
    val chestW = ((chestCm.toFloat() - 75f) / 60f).coerceIn(0.24f, 0.48f)
    val waistW = ((waistCm.toFloat() - 60f) / 60f).coerceIn(0.18f, 0.46f)
    val hipW = ((hipCm.toFloat() - 75f) / 60f).coerceIn(0.22f, 0.46f)

    val skin = Color(0xFFC9936B)
    fun heatColor(fatigue: Float, fallback: Color): Color =
      if (!showMuscleHeatmap) fallback else lerp(Color(0xFF4CAF50), HeartRateCoral, fatigue.coerceIn(0f, 1f))

    val upperCol = heatColor(upperFatigueRatio, topColor)
    val coreCol = heatColor(coreFatigueRatio, if (showJacket) lerp(topColor, Color.Black, 0.24f) else topColor)
    val legCol = heatColor(legFatigueRatio, bottomColor)

    val pts =
      mapOf(
        "head" to SpatialPt3(0f, -0.92f, 0f),
        "hat" to SpatialPt3(0f, -1.07f, 0f),
        "neck" to SpatialPt3(0f, -0.68f, 0f),
        "ls" to SpatialPt3(-chestW, -0.58f, 0f),
        "rs" to SpatialPt3(chestW, -0.58f, 0f),
        "lw" to SpatialPt3(-waistW, -0.18f, 0.03f),
        "rw" to SpatialPt3(waistW, -0.18f, 0.03f),
        "lh" to SpatialPt3(-hipW, 0.12f, 0f),
        "rh" to SpatialPt3(hipW, 0.12f, 0f),
        "le" to SpatialPt3(-chestW - 0.12f, -0.16f, 0.08f),
        "re" to SpatialPt3(chestW + 0.12f, -0.16f, 0.08f),
        "lhand" to SpatialPt3(-chestW - 0.15f, 0.20f, 0.12f),
        "rhand" to SpatialPt3(chestW + 0.15f, 0.20f, 0.12f),
        "lk" to SpatialPt3(-hipW * 0.85f, 0.56f, 0.05f),
        "rk" to SpatialPt3(hipW * 0.85f, 0.56f, 0.05f),
        "lf" to SpatialPt3(-hipW * 0.80f, 1.02f, 0.10f),
        "rf" to SpatialPt3(hipW * 0.80f, 1.02f, 0.10f),
      ).mapValues { proj3(rot3(it.value, yawDeg, 8f), c, sc) }

    val torsoPath =
      Path().apply {
        listOf("ls", "rs", "rw", "rh", "lh", "lw").forEachIndexed { idx, k ->
          val p = pts.getValue(k).first
          if (idx == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
        }
        close()
      }
    drawPath(torsoPath, coreCol)
    if (showBelt) {
      drawLitBone(pts.getValue("lh").first, pts.getValue("rh").first, 9.dp.toPx(), GoldAccent)
    }
    drawLitBone(pts.getValue("ls").first, pts.getValue("le").first, (if (showJacket) 15f else 11f).dp.toPx(), upperCol)
    drawLitBone(pts.getValue("rs").first, pts.getValue("re").first, (if (showJacket) 15f else 11f).dp.toPx(), upperCol)
    drawLitBone(pts.getValue("le").first, pts.getValue("lhand").first, 9.dp.toPx(), if (showJacket) upperCol else skin)
    drawLitBone(pts.getValue("re").first, pts.getValue("rhand").first, 9.dp.toPx(), if (showJacket) upperCol else skin)
    drawLitBone(pts.getValue("lh").first, pts.getValue("lk").first, 15.dp.toPx(), legCol)
    drawLitBone(pts.getValue("rh").first, pts.getValue("rk").first, 15.dp.toPx(), legCol)
    drawLitBone(pts.getValue("lk").first, pts.getValue("lf").first, 12.dp.toPx(), legCol)
    drawLitBone(pts.getValue("rk").first, pts.getValue("rf").first, 12.dp.toPx(), legCol)
    drawLitSphere(pts.getValue("lf").first, 8.dp.toPx(), accentColor)
    drawLitSphere(pts.getValue("rf").first, 8.dp.toPx(), accentColor)
    drawLitBone(pts.getValue("neck").first, pts.getValue("head").first, 10.dp.toPx(), skin)
    drawLitSphere(pts.getValue("head").first, 18.dp.toPx() * pts.getValue("head").second, skin)
    if (showScarf) {
      drawLitBone(pts.getValue("ls").first, pts.getValue("neck").first, 11.dp.toPx(), accentColor)
      drawLitBone(pts.getValue("neck").first, pts.getValue("rs").first, 11.dp.toPx(), accentColor)
    }
    if (showHat) {
      val hp = pts.getValue("hat").first
      drawLitBone(Offset(hp.x - 20.dp.toPx(), hp.y + 4.dp.toPx()), Offset(hp.x + 20.dp.toPx(), hp.y + 4.dp.toPx()), 6.dp.toPx(), accentColor)
      drawLitSphere(hp, 13.dp.toPx(), accentColor)
    }
  }
}

/**
 * #3 3D Spine & Posture Tilt Visualizer reacting to sensor pitch/roll.
 */
@Composable
fun SpinePosture3DCanvas(
  slouchDeg: Float,
  goodColor: Color,
  warnColor: Color,
  modifier: Modifier = Modifier,
) {
  Canvas(modifier = modifier.fillMaxWidth().height(165.dp)) {
    val c = Offset(size.width / 2f, size.height * 0.85f)
    val curveColor = if (kotlin.math.abs(slouchDeg) <= 4.5f) goodColor else warnColor
    val segs = 9
    var prev = c
    for (i in 1..segs) {
      val t = i / segs.toFloat()
      val leanX = (slouchDeg * 4.5f) * t * t
      val next = Offset(c.x + leanX, c.y - t * size.height * 0.72f)
      drawLitBone(prev, next, (12f - t * 4f).dp.toPx(), curveColor)
      drawLitSphere(next, (8f - t * 2f).dp.toPx(), curveColor)
      prev = next
    }
  }
}

/**
 * #4 Animated 3D Exercise Form Canvas (Squat, Push-Up, Plank, Reach).
 */
@Composable
fun ExerciseForm3DCanvas(
  exerciseMode: String,
  phase01: Float,
  primaryColor: Color,
  accentColor: Color,
  modifier: Modifier = Modifier,
) {
  Canvas(modifier = modifier.fillMaxWidth().height(180.dp)) {
    val c = Offset(size.width / 2f, size.height * 0.60f)
    val sc = size.minDimension * 0.34f
    val squatDrop = if (exerciseMode == "SQUAT") sin(phase01 * PI.toFloat()) * 0.38f else 0f
    val pushDrop = if (exerciseMode == "PUSHUP") sin(phase01 * PI.toFloat()) * 0.25f else 0f
    val stretchSway = if (exerciseMode == "STRETCH") cos(phase01 * 2f * PI.toFloat()) * 0.24f else 0f

    val pts =
      if (exerciseMode == "PUSHUP" || exerciseMode == "PLANK") {
        mapOf(
          "head" to SpatialPt3(-0.75f, -0.35f + pushDrop, 0f),
          "shoulder" to SpatialPt3(-0.48f, -0.24f + pushDrop, 0f),
          "hip" to SpatialPt3(0.15f, -0.15f + pushDrop * 0.4f, 0f),
          "ankle" to SpatialPt3(0.85f, 0.25f, 0f),
          "hand" to SpatialPt3(-0.52f, 0.28f, 0.10f),
        )
      } else {
        mapOf(
          "head" to SpatialPt3(stretchSway, -0.82f + squatDrop, -squatDrop * 0.3f),
          "shoulder" to SpatialPt3(stretchSway * 0.6f, -0.52f + squatDrop, -squatDrop * 0.2f),
          "hip" to SpatialPt3(0f, 0.04f + squatDrop, squatDrop * 0.4f),
          "ankle" to SpatialPt3(0f, 0.78f, 0f),
          "hand" to SpatialPt3(0.35f + stretchSway, -0.32f + squatDrop, -0.45f * squatDrop),
        )
      }.mapValues { proj3(rot3(it.value, 28f, 12f), c, sc) }

    drawLitBone(pts.getValue("ankle").first, pts.getValue("hip").first, 10.dp.toPx(), primaryColor)
    drawLitBone(pts.getValue("hip").first, pts.getValue("shoulder").first, 12.dp.toPx(), primaryColor)
    drawLitBone(pts.getValue("shoulder").first, pts.getValue("hand").first, 8.dp.toPx(), accentColor)
    drawLitBone(pts.getValue("shoulder").first, pts.getValue("head").first, 8.dp.toPx(), primaryColor)
    drawLitSphere(pts.getValue("head").first, 14.dp.toPx(), accentColor)
  }
}

/**
 * #5 3D Monthly Habit & Streak City Skyline.
 */
@Composable
fun HabitCity3DCanvas(
  dayCountsLast14: List<Int>,
  towerColor: Color,
  highlightColor: Color,
  modifier: Modifier = Modifier,
) {
  Canvas(modifier = modifier.fillMaxWidth().height(175.dp)) {
    val cols = 7
    val rows = 2
    val cellW = size.width / (cols + 1.5f)
    val baseY = size.height * 0.80f
    for (r in 0 until rows) {
      for (col in 0 until cols) {
        val idx = r * cols + col
        val count = dayCountsLast14.getOrElse(idx) { 0 }.coerceIn(0, 6)
        val h = (14f + count * 17f).dp.toPx()
        val x = (col + 0.9f) * cellW + (r * 14.dp.toPx())
        val y = baseY - (r * 22.dp.toPx())
        val c = if (count >= 3) highlightColor else towerColor
        // 3D isometric bar
        drawRect(lerp(c, Color.Black, 0.22f), Offset(x, y - h), Size(18.dp.toPx(), h))
        drawRect(c, Offset(x - 6.dp.toPx(), y - h), Size(6.dp.toPx(), h))
        val roof = Path().apply {
          moveTo(x - 6.dp.toPx(), y - h)
          lineTo(x + 12.dp.toPx(), y - h)
          lineTo(x + 18.dp.toPx(), y - h - 7.dp.toPx())
          lineTo(x, y - h - 7.dp.toPx())
          close()
        }
        drawPath(roof, lerp(c, Color.White, 0.35f))
      }
    }
  }
}

/**
 * #6 3D Sleep Architecture Mountain Terrain.
 */
@Composable
fun SleepMountain3DCanvas(
  records: List<SleepRecordEntity>,
  modifier: Modifier = Modifier,
) {
  Canvas(modifier = modifier.fillMaxWidth().height(165.dp)) {
    val sample = records.take(7).reversed()
    val cols = 7
    val stepX = size.width / (cols + 1f)
    val baseY = size.height * 0.85f
    val path = Path().apply { moveTo(stepX * 0.5f, baseY) }
    for (i in 0 until cols) {
      val rec = sample.getOrNull(i)
      val durRatio = ((rec?.durationMinutes ?: 360) / 540f).coerceIn(0.15f, 1.1f)
      val px = stepX * (i + 1)
      val py = baseY - durRatio * size.height * 0.62f
      path.lineTo(px, py)
      drawLitSphere(Offset(px, py), 6.dp.toPx(), if ((rec?.deepMinutes ?: 0) > 70) GoldAccent else SleepIndigo)
    }
    path.lineTo(stepX * (cols + 0.5f), baseY)
    path.close()
    drawPath(
      path = path,
      brush = Brush.verticalGradient(listOf(SleepIndigo.copy(alpha = 0.65f), SleepIndigo.copy(alpha = 0.12f))),
    )
  }
}

/**
 * #7 3D Sloshing Hydration Flask Gauge.
 */
@Composable
fun HydrationFlask3DCanvas(
  waterMl: Int,
  goalMl: Int,
  yawDeg: Float,
  modifier: Modifier = Modifier,
) {
  val fillFrac = (waterMl.toFloat() / goalMl.coerceAtLeast(500)).coerceIn(0.05f, 1f)
  Canvas(modifier = modifier.fillMaxWidth().height(170.dp)) {
    val w = 78.dp.toPx()
    val h = 128.dp.toPx()
    val left = size.width / 2f - w / 2f
    val top = size.height / 2f - h / 2f
    val shift = sin(yawDeg * PI / 180.0).toFloat() * 7.dp.toPx()
    // Flask body glass
    drawRoundRect(
      brush = Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0.30f), HydrationBlue.copy(alpha = 0.12f), Color.White.copy(alpha = 0.25f))),
      topLeft = Offset(left, top),
      size = Size(w, h),
      cornerRadius = androidx.compose.ui.geometry.CornerRadius(22.dp.toPx()),
      style = Stroke(width = 2.5.dp.toPx()),
    )
    // Liquid fill
    val fillH = (h - 10.dp.toPx()) * fillFrac
    val fillTop = top + h - 5.dp.toPx() - fillH
    drawRoundRect(
      brush = Brush.horizontalGradient(listOf(lerp(HydrationBlue, Color.White, 0.25f), HydrationBlue, lerp(HydrationBlue, Color.Black, 0.25f))),
      topLeft = Offset(left + 4.dp.toPx(), fillTop),
      size = Size(w - 8.dp.toPx(), fillH),
      cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx()),
    )
    // Sloshing meniscus ellipse
    drawOval(
      color = lerp(HydrationBlue, Color.White, 0.45f),
      topLeft = Offset(left + 4.dp.toPx(), fillTop - 6.dp.toPx() + shift * 0.2f),
      size = Size(w - 8.dp.toPx(), 12.dp.toPx()),
    )
  }
}

/**
 * #8 Pulsing 3D Heart-Rate Sphere + #10 3D Trophy Shelf.
 */
@Composable
fun HeartPulse3DSphereCanvas(
  pulsePhase01: Float,
  modifier: Modifier = Modifier,
) {
  Canvas(modifier = modifier.fillMaxWidth().height(150.dp)) {
    val c = Offset(size.width / 2f, size.height / 2f)
    val baseR = 32.dp.toPx()
    val r = baseR + 12.dp.toPx() * sin(pulsePhase01 * PI.toFloat())
    drawCircle(HeartRateCoral.copy(alpha = 0.18f), r * 1.45f, c)
    drawLitSphere(c, r, HeartRateCoral)
  }
}

@Composable
fun TrophyShelf3DCanvas(
  lifetimeSteps: Int,
  yawDeg: Float,
  modifier: Modifier = Modifier,
) {
  val milestones = listOf(10_000 to "10K", 100_000 to "100K", 1_000_000 to "1M")
  val muted = MaterialTheme.colorScheme.surfaceVariant
  Canvas(modifier = modifier.fillMaxWidth().height(150.dp)) {
    val shelfY = size.height * 0.72f
    drawLine(Color.Gray.copy(alpha = 0.45f), Offset(24.dp.toPx(), shelfY), Offset(size.width - 24.dp.toPx(), shelfY), 5.dp.toPx(), StrokeCap.Round)
    milestones.forEachIndexed { idx, (target, _) ->
      val unlocked = lifetimeSteps >= target
      val cx = size.width * ((idx + 1) / 4f) + sin(yawDeg * PI / 180.0).toFloat() * 8.dp.toPx()
      val cupColor = if (unlocked) GoldAccent else muted
      drawLitBone(Offset(cx, shelfY), Offset(cx, shelfY - 34.dp.toPx()), 8.dp.toPx(), cupColor)
      drawLitSphere(Offset(cx, shelfY - 46.dp.toPx()), 16.dp.toPx(), cupColor)
    }
  }
}
