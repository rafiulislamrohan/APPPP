package com.example.ui.components

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.local.GpsPoint
import com.example.ui.theme.JetBrainsMonoFamily
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Live hardware gyroscope/accelerometer tilt state for subtle 3D parallax.
 * Safely degrades to (0f, 0f) when sensors are absent or 3D effects are toggled off.
 */
@Composable
fun rememberSensorTilt3D(enabled: Boolean = true): State<Pair<Float, Float>> {
  val context = LocalContext.current
  val tiltState = remember { mutableStateOf(0f to 0f) }

  DisposableEffect(enabled) {
    if (!enabled) {
      tiltState.value = 0f to 0f
      return@DisposableEffect onDispose {}
    }

    val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    val sensor =
      sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        ?: sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    if (sensorManager == null || sensor == null) {
      return@DisposableEffect onDispose {}
    }

    val listener =
      object : SensorEventListener {
        private var smoothPitch = 0f
        private var smoothRoll = 0f

        override fun onSensorChanged(event: SensorEvent) {
          val (rawPitch, rawRoll) =
            if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR && event.values.size >= 3) {
              val rotMatrix = FloatArray(9)
              val orientation = FloatArray(3)
              SensorManager.getRotationMatrixFromVector(rotMatrix, event.values)
              SensorManager.getOrientation(rotMatrix, orientation)
              val pitchDeg = Math.toDegrees(orientation[1].toDouble()).toFloat().coerceIn(-12f, 12f)
              val rollDeg = Math.toDegrees(orientation[2].toDouble()).toFloat().coerceIn(-12f, 12f)
              (pitchDeg * 0.45f) to (rollDeg * 0.45f)
            } else if (event.values.size >= 3) {
              val x = (event.values[0] / 9.81f).coerceIn(-1f, 1f) * 6f
              val y = (event.values[1] / 9.81f).coerceIn(-1f, 1f) * 6f
              y to -x
            } else {
              0f to 0f
            }

          smoothPitch = smoothPitch * 0.85f + rawPitch * 0.15f
          smoothRoll = smoothRoll * 0.85f + rawRoll * 0.15f
          tiltState.value = smoothPitch.coerceIn(-7f, 7f) to smoothRoll.coerceIn(-7f, 7f)
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
      }

    runCatching {
      sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
    }

    onDispose {
      runCatching { sensorManager.unregisterListener(listener) }
    }
  }

  return tiltState
}

/**
 * Adds 3D perspective camera distance, shadow depth, bevel edge lighting, and optional tilt.
 * Non-intercepting so clickable children and accessibility tests remain 100% reliable.
 */
fun Modifier.tactile3D(
  enabled: Boolean = true,
  tiltX: Float = 0f,
  tiltY: Float = 0f,
  elevationDp: Dp = 8.dp,
  cornerRadiusDp: Dp = 18.dp,
  highlightColor: Color = Color.White.copy(alpha = 0.28f),
  depthShadeColor: Color = Color.Black.copy(alpha = 0.16f),
): Modifier {
  if (!enabled) return this
  return this
    .graphicsLayer {
      cameraDistance = 14f * density
      rotationX = tiltX
      rotationY = tiltY
      shadowElevation = elevationDp.toPx()
      shape = RoundedCornerShape(cornerRadiusDp)
      clip = false
    }
    .drawBehind {
      val radius = cornerRadiusDp.toPx()
      // Subtle lower-right 3D extruded bevel rim
      drawRoundRect(
        color = depthShadeColor,
        topLeft = Offset(1.dp.toPx(), 2.5.dp.toPx()),
        size = size,
        cornerRadius = CornerRadius(radius, radius),
      )
      // Top specular rim highlight
      drawRoundRect(
        brush =
          Brush.verticalGradient(
            listOf(highlightColor, Color.Transparent),
            startY = 0f,
            endY = size.height * 0.32f,
          ),
        cornerRadius = CornerRadius(radius, radius),
        style = Stroke(width = 1.2.dp.toPx()),
      )
    }
}

@Composable
fun Tactile3DCard(
  modifier: Modifier = Modifier,
  enabled3D: Boolean = true,
  tiltX: Float = 0f,
  tiltY: Float = 0f,
  containerColor: Color = MaterialTheme.colorScheme.surface,
  shape: Shape = MaterialTheme.shapes.large,
  elevationDp: Dp = 8.dp,
  content: @Composable ColumnScope.() -> Unit,
) {
  Card(
    modifier =
      modifier.tactile3D(
        enabled = enabled3D,
        tiltX = tiltX,
        tiltY = tiltY,
        elevationDp = elevationDp,
      ),
    shape = shape,
    colors = CardDefaults.cardColors(containerColor = containerColor),
    elevation = CardDefaults.cardElevation(defaultElevation = if (enabled3D) 4.dp else 1.dp),
    border =
      if (enabled3D) {
        BorderStroke(
          width = 1.dp,
          brush =
            Brush.linearGradient(
              listOf(
                Color.White.copy(alpha = 0.35f),
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
              )
            ),
        )
      } else {
        null
      },
    content = content,
  )
}

// --- 3D Mathematical Projection Engine for Interactive Compose Canvases ---

private data class Vec3(val x: Float, val y: Float, val z: Float)

private fun rotateVec3(p: Vec3, yawDeg: Float, pitchDeg: Float): Vec3 {
  val yaw = (yawDeg * PI / 180.0).toFloat()
  val pitch = (pitchDeg * PI / 180.0).toFloat()

  // Yaw around Y
  val cosY = cos(yaw)
  val sinY = sin(yaw)
  val x1 = p.x * cosY + p.z * sinY
  val z1 = -p.x * sinY + p.z * cosY

  // Pitch around X
  val cosP = cos(pitch)
  val sinP = sin(pitch)
  val y2 = p.y * cosP - z1 * sinP
  val z2 = p.y * sinP + z1 * cosP
  return Vec3(x1, y2, z2)
}

private fun projectVec3(p: Vec3, center: Offset, scale: Float, cameraZ: Float = 4.2f): Pair<Offset, Float> {
  val denom = (cameraZ - p.z).coerceAtLeast(0.8f)
  val perspective = cameraZ / denom
  return Offset(
    x = center.x + p.x * scale * perspective,
    y = center.y + p.y * scale * perspective,
  ) to perspective
}

private fun DrawScope.drawShaded3DSphere(
  center: Offset,
  radius: Float,
  baseColor: Color,
  shadowAlpha: Float = 0.25f,
) {
  if (radius <= 0.5f) return
  drawCircle(
    color = Color.Black.copy(alpha = shadowAlpha),
    radius = radius * 1.05f,
    center = Offset(center.x + radius * 0.14f, center.y + radius * 0.18f),
  )
  val lightOffset = Offset(center.x - radius * 0.34f, center.y - radius * 0.34f)
  drawCircle(
    brush =
      Brush.radialGradient(
        colors =
          listOf(
            lerp(baseColor, Color.White, 0.52f),
            baseColor,
            lerp(baseColor, Color.Black, 0.40f),
          ),
        center = lightOffset,
        radius = radius * 1.45f,
      ),
    radius = radius,
    center = center,
  )
}

private fun DrawScope.draw3DLimb(
  a: Offset,
  b: Offset,
  thickness: Float,
  baseColor: Color,
) {
  // Depth shadow edge
  drawLine(
    color = lerp(baseColor, Color.Black, 0.42f),
    start = Offset(a.x + 2.dp.toPx(), a.y + 2.dp.toPx()),
    end = Offset(b.x + 2.dp.toPx(), b.y + 2.dp.toPx()),
    strokeWidth = thickness,
    cap = StrokeCap.Round,
  )
  // Main lit capsule
  drawLine(
    brush =
      Brush.linearGradient(
        colors = listOf(lerp(baseColor, Color.White, 0.28f), baseColor, lerp(baseColor, Color.Black, 0.22f)),
        start = a,
        end = b,
      ),
    start = a,
    end = b,
    strokeWidth = thickness,
    cap = StrokeCap.Round,
  )
}

/**
 * Interactive 360-degree rotatable 3D body & dress color mannequin.
 */
@Composable
fun Spatial3DMannequinCanvas(
  heightCm: Double,
  weightKg: Double,
  bodyBuild: String,
  skinColor: Color,
  topColor: Color,
  bottomColor: Color,
  accentColor: Color,
  modifier: Modifier = Modifier,
) {
  var yawDeg by remember { mutableFloatStateOf(-22f) }
  var pitchDeg by remember { mutableFloatStateOf(8f) }

  val heightFactor = ((heightCm.toFloat() - 140f) / 60f).coerceIn(0f, 1f)
  val bmi =
    remember(heightCm, weightKg) {
      val m = (heightCm / 100.0).coerceAtLeast(1.0)
      (weightKg / (m * m)).toFloat()
    }
  val widthFactor =
    remember(bmi, bodyBuild) {
      val base = ((bmi - 17f) / 16f).coerceIn(0f, 1f)
      when {
        bodyBuild.contains("Broad", ignoreCase = true) -> (base + 0.18f).coerceAtMost(1.1f)
        bodyBuild.contains("Curvy", ignoreCase = true) -> (base + 0.14f).coerceAtMost(1.1f)
        bodyBuild.contains("Slim", ignoreCase = true) -> (base - 0.14f).coerceAtLeast(0f)
        else -> base
      }
    }

  Box(
    modifier =
      modifier
        .fillMaxWidth()
        .height(250.dp)
        .pointerInput(Unit) {
          detectDragGestures { change, dragAmount ->
            change.consume()
            yawDeg += dragAmount.x * 0.55f
            pitchDeg = (pitchDeg + dragAmount.y * 0.35f).coerceIn(-35f, 35f)
          }
        },
  ) {
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
    val glowColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)

    Canvas(modifier = Modifier.fillMaxSize()) {
      val center = Offset(size.width / 2f, size.height * 0.48f)
      val scale = size.minDimension * 0.34f

      // 3D Floor stage turntable ellipse
      val floorY = 1.15f + heightFactor * 0.12f
      drawOval(
        brush = Brush.radialGradient(listOf(glowColor, Color.Transparent)),
        topLeft = Offset(center.x - scale * 1.15f, center.y + scale * floorY - 22.dp.toPx()),
        size = Size(scale * 2.3f, 44.dp.toPx()),
      )
      for (ringIdx in 1..3) {
        val r = 0.32f * ringIdx
        val pts =
          (0..24).map { step ->
            val a = (step / 24f) * (2f * PI.toFloat())
            val v = rotateVec3(Vec3(cos(a) * r, floorY, sin(a) * r), yawDeg, pitchDeg)
            projectVec3(v, center, scale).first
          }
        for (i in 0 until pts.size - 1) {
          drawLine(gridColor, pts[i], pts[i + 1], strokeWidth = 1.dp.toPx())
        }
      }

      // 3D Proportional Body Skeleton
      val shoulderW = 0.30f + widthFactor * 0.11f
      val hipW = 0.20f + widthFactor * 0.10f
      val waistW = if (bodyBuild.contains("Curvy", true)) hipW * 0.78f else shoulderW * 0.78f
      val torsoTop = -0.62f - heightFactor * 0.08f
      val hipY = 0.10f + heightFactor * 0.03f
      val ankleY = 1.05f + heightFactor * 0.12f

      val joints =
        mapOf(
          "head" to Vec3(0f, torsoTop - 0.30f, 0f),
          "neck" to Vec3(0f, torsoTop - 0.12f, 0f),
          "lShoulder" to Vec3(-shoulderW, torsoTop, 0f),
          "rShoulder" to Vec3(shoulderW, torsoTop, 0f),
          "lWaist" to Vec3(-waistW, (torsoTop + hipY) * 0.52f, 0.02f),
          "rWaist" to Vec3(waistW, (torsoTop + hipY) * 0.52f, 0.02f),
          "lElbow" to Vec3(-shoulderW - 0.12f, -0.18f, 0.06f),
          "rElbow" to Vec3(shoulderW + 0.12f, -0.18f, 0.06f),
          "lHand" to Vec3(-shoulderW - 0.14f, 0.18f, 0.12f),
          "rHand" to Vec3(shoulderW + 0.14f, 0.18f, 0.12f),
          "lHip" to Vec3(-hipW, hipY, 0f),
          "rHip" to Vec3(hipW, hipY, 0f),
          "lKnee" to Vec3(-hipW * 0.92f, (hipY + ankleY) * 0.52f, 0.04f),
          "rKnee" to Vec3(hipW * 0.92f, (hipY + ankleY) * 0.52f, 0.04f),
          "lFoot" to Vec3(-hipW * 0.88f, ankleY, 0.10f),
          "rFoot" to Vec3(hipW * 0.88f, ankleY, 0.10f),
        )

      val rotated = joints.mapValues { rotateVec3(it.value, yawDeg, pitchDeg) }
      val projected = rotated.mapValues { projectVec3(it.value, center, scale) }

      data class Primitive3D(val depth: Float, val drawAction: DrawScope.() -> Unit)

      val drawQueue = mutableListOf<Primitive3D>()

      // Torso Polygon (Top garment)
      val torsoKeys = listOf("lShoulder", "rShoulder", "rWaist", "rHip", "lHip", "lWaist")
      val torsoAvgZ = torsoKeys.map { rotated.getValue(it).z }.average().toFloat()
      drawQueue +=
        Primitive3D(torsoAvgZ) {
          val path =
            Path().apply {
              torsoKeys.forEachIndexed { i, k ->
                val pt = projected.getValue(k).first
                if (i == 0) moveTo(pt.x, pt.y) else lineTo(pt.x, pt.y)
              }
              close()
            }
          val topPt = projected.getValue("neck").first
          val botPt = projected.getValue("lHip").first
          drawPath(
            path = path,
            brush =
              Brush.linearGradient(
                colors = listOf(lerp(topColor, Color.White, 0.24f), topColor, lerp(topColor, Color.Black, 0.26f)),
                start = topPt,
                end = botPt,
              ),
          )
        }

      fun addLimb(k1: String, k2: String, radiusDp: Float, color: Color) {
        val z = (rotated.getValue(k1).z + rotated.getValue(k2).z) * 0.5f
        val (p1, s1) = projected.getValue(k1)
        val (p2, s2) = projected.getValue(k2)
        drawQueue +=
          Primitive3D(z) {
            draw3DLimb(p1, p2, radiusDp.dp.toPx() * ((s1 + s2) * 0.5f), color)
          }
      }

      // Neck & Head
      addLimb("neck", "lShoulder", 9f, topColor)
      addLimb("neck", "rShoulder", 9f, topColor)
      drawQueue +=
        Primitive3D(rotated.getValue("head").z + 0.04f) {
          val (headPt, pScale) = projected.getValue("head")
          drawShaded3DSphere(headPt, 18.dp.toPx() * pScale, skinColor)
        }
      addLimb("neck", "head", 10f, skinColor)

      // Sleeves / Arms & Skin Forearms
      val armThick = 11f + widthFactor * 4f
      addLimb("lShoulder", "lElbow", armThick, topColor)
      addLimb("rShoulder", "rElbow", armThick, topColor)
      addLimb("lElbow", "lHand", armThick * 0.85f, skinColor)
      addLimb("rElbow", "rHand", armThick * 0.85f, skinColor)
      drawQueue +=
        Primitive3D(rotated.getValue("lHand").z) {
          val (pt, sc) = projected.getValue("lHand")
          drawShaded3DSphere(pt, 5.5.dp.toPx() * sc, skinColor)
        }
      drawQueue +=
        Primitive3D(rotated.getValue("rHand").z) {
          val (pt, sc) = projected.getValue("rHand")
          drawShaded3DSphere(pt, 5.5.dp.toPx() * sc, skinColor)
        }

      // Legs (Bottom garment)
      val legThick = 14f + widthFactor * 5f
      addLimb("lHip", "lKnee", legThick, bottomColor)
      addLimb("rHip", "rKnee", legThick, bottomColor)
      addLimb("lKnee", "lFoot", legThick * 0.84f, bottomColor)
      addLimb("rKnee", "rFoot", legThick * 0.84f, bottomColor)

      // Accent Belt / Collar & Shoes
      addLimb("lHip", "rHip", 9f, accentColor)
      drawQueue +=
        Primitive3D(rotated.getValue("lFoot").z + 0.03f) {
          val (pt, sc) = projected.getValue("lFoot")
          drawShaded3DSphere(pt, 8.dp.toPx() * sc, accentColor)
        }
      drawQueue +=
        Primitive3D(rotated.getValue("rFoot").z + 0.03f) {
          val (pt, sc) = projected.getValue("rFoot")
          drawShaded3DSphere(pt, 8.dp.toPx() * sc, accentColor)
        }

      // Painter's algorithm sort: back to front
      drawQueue.sortedBy { it.depth }.forEach { it.drawAction(this) }
    }

    Surface(
      shape = RoundedCornerShape(10.dp),
      color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.88f),
      modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
    ) {
      Text(
        text = "360° 3D Drag • ${yawDeg.toInt()}°",
        style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
      )
    }
  }
}

/**
 * Interactive 3D Wellness Vitality Sphere + 4 Orbital Progress Rings (Steps, Hydration, Sleep, Tasks).
 */
@Composable
fun Spatial3DVitalityOrbCanvas(
  stepsProgress: Float,
  waterProgress: Float,
  sleepProgress: Float,
  tasksProgress: Float,
  stepColor: Color,
  waterColor: Color,
  sleepColor: Color,
  taskColor: Color,
  yawDeg: Float,
  pitchDeg: Float,
  onDragDelta: (dx: Float, dy: Float) -> Unit,
  modifier: Modifier = Modifier,
) {
  val wireColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.30f)

  Canvas(
    modifier =
      modifier
        .fillMaxWidth()
        .height(260.dp)
        .pointerInput(Unit) {
          detectDragGestures { change, dragAmount ->
            change.consume()
            onDragDelta(dragAmount.x * 0.5f, dragAmount.y * 0.35f)
          }
        },
  ) {
    val center = Offset(size.width / 2f, size.height / 2f)
    val scale = size.minDimension * 0.34f

    // Wireframe 3D latitude & longitude grid sphere
    for (latStep in -2..2) {
      val phi = latStep * (PI.toFloat() / 6f)
      val r = cos(phi) * 0.85f
      val y = sin(phi) * 0.85f
      val ringPts =
        (0..28).map { i ->
          val theta = (i / 28f) * 2f * PI.toFloat()
          val v = rotateVec3(Vec3(cos(theta) * r, y, sin(theta) * r), yawDeg, pitchDeg)
          projectVec3(v, center, scale)
        }
      for (i in 0 until ringPts.size - 1) {
        drawLine(wireColor, ringPts[i].first, ringPts[i + 1].first, strokeWidth = 1.dp.toPx())
      }
    }

    // Tilted 3D Progress Orbits
    data class OrbitSpec(val tiltDeg: Float, val radius: Float, val progress: Float, val color: Color)
    val orbits =
      listOf(
        OrbitSpec(18f, 1.08f, stepsProgress, stepColor),
        OrbitSpec(-24f, 1.24f, waterProgress, waterColor),
        OrbitSpec(48f, 1.38f, sleepProgress, sleepColor),
        OrbitSpec(-52f, 1.50f, tasksProgress, taskColor),
      )

    // Back half of orbits + core sphere + front half of orbits
    val avgOverall = ((stepsProgress + waterProgress + sleepProgress + tasksProgress) / 4f).coerceIn(0f, 1f)
    val coreRadius = (28f + 16f * avgOverall).dp.toPx()

    fun drawOrbits(frontHalf: Boolean) {
      orbits.forEach { orbit ->
        val tiltRad = (orbit.tiltDeg * PI / 180.0).toFloat()
        val stepsCount = 36
        val activeSteps = (stepsCount * orbit.progress.coerceIn(0.05f, 1f)).toInt()
        for (i in 0 until stepsCount) {
          val a1 = (i / stepsCount.toFloat()) * 2f * PI.toFloat()
          val a2 = ((i + 1) / stepsCount.toFloat()) * 2f * PI.toFloat()
          val raw1 = Vec3(cos(a1) * orbit.radius, sin(a1) * orbit.radius * sin(tiltRad), sin(a1) * orbit.radius * cos(tiltRad))
          val raw2 = Vec3(cos(a2) * orbit.radius, sin(a2) * orbit.radius * sin(tiltRad), sin(a2) * orbit.radius * cos(tiltRad))
          val r1 = rotateVec3(raw1, yawDeg, pitchDeg)
          val r2 = rotateVec3(raw2, yawDeg, pitchDeg)
          val isFront = (r1.z + r2.z) * 0.5f >= 0f
          if (isFront == frontHalf) {
            val p1 = projectVec3(r1, center, scale).first
            val p2 = projectVec3(r2, center, scale).first
            val isLit = i <= activeSteps
            drawLine(
              color = if (isLit) orbit.color else orbit.color.copy(alpha = 0.22f),
              start = p1,
              end = p2,
              strokeWidth = if (isLit) 4.dp.toPx() else 1.8.dp.toPx(),
              cap = StrokeCap.Round,
            )
            if (isFront && i == activeSteps) {
              drawShaded3DSphere(p2, 6.dp.toPx(), orbit.color)
            }
          }
        }
      }
    }

    drawOrbits(frontHalf = false)
    drawShaded3DSphere(center, coreRadius, lerp(stepColor, waterColor, 0.35f))
    drawOrbits(frontHalf = true)
  }
}

/**
 * Projects GPS route latitude/longitude/altitude into a 3D isometric terrain path.
 */
@Composable
fun Spatial3DGpsTerrainCanvas(
  points: List<GpsPoint>,
  routeColor: Color,
  accentColor: Color,
  yawDeg: Float,
  pitchDeg: Float,
  onDragDelta: (dx: Float, dy: Float) -> Unit,
  modifier: Modifier = Modifier,
) {
  val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)

  Canvas(
    modifier =
      modifier
        .fillMaxWidth()
        .height(250.dp)
        .pointerInput(Unit) {
          detectDragGestures { change, dragAmount ->
            change.consume()
            onDragDelta(dragAmount.x * 0.5f, dragAmount.y * 0.35f)
          }
        },
  ) {
    val center = Offset(size.width / 2f, size.height * 0.54f)
    val scale = size.minDimension * 0.36f

    // 3D Terrain reference grid
    for (g in -3..3) {
      val pos = g * 0.4f
      val p1 = projectVec3(rotateVec3(Vec3(-1.2f, 0.45f, pos), yawDeg, pitchDeg), center, scale).first
      val p2 = projectVec3(rotateVec3(Vec3(1.2f, 0.45f, pos), yawDeg, pitchDeg), center, scale).first
      val q1 = projectVec3(rotateVec3(Vec3(pos, 0.45f, -1.2f), yawDeg, pitchDeg), center, scale).first
      val q2 = projectVec3(rotateVec3(Vec3(pos, 0.45f, 1.2f), yawDeg, pitchDeg), center, scale).first
      drawLine(gridColor, p1, p2, strokeWidth = 1.dp.toPx())
      drawLine(gridColor, q1, q2, strokeWidth = 1.dp.toPx())
    }

    val safePoints =
      if (points.size >= 2) {
        points
      } else {
        listOf(
          GpsPoint(latitude = 0.0, longitude = 0.0, altitudeMeters = 12.0),
          GpsPoint(latitude = 0.002, longitude = 0.003, altitudeMeters = 28.0),
          GpsPoint(latitude = 0.005, longitude = 0.004, altitudeMeters = 42.0),
          GpsPoint(latitude = 0.007, longitude = 0.001, altitudeMeters = 20.0),
          GpsPoint(latitude = 0.009, longitude = 0.005, altitudeMeters = 36.0),
        )
      }

    val minLat = safePoints.minOf { it.latitude }
    val maxLat = safePoints.maxOf { it.latitude }
    val minLon = safePoints.minOf { it.longitude }
    val maxLon = safePoints.maxOf { it.longitude }
    val minAlt = safePoints.minOf { it.altitudeMeters }
    val maxAlt = safePoints.maxOf { it.altitudeMeters }
    val span = maxOf(maxLat - minLat, maxLon - minLon, 0.0001)
    val altSpan = (maxAlt - minAlt).coerceAtLeast(1.0)

    val vecs =
      safePoints.map { pt ->
        val nx = (((pt.longitude - minLon) / span).toFloat() - 0.5f) * 2.0f
        val nz = (((pt.latitude - minLat) / span).toFloat() - 0.5f) * 2.0f
        val ny = 0.40f - (((pt.altitudeMeters - minAlt) / altSpan).toFloat() * 0.85f)
        Vec3(nx, ny, nz) to Vec3(nx, 0.45f, nz)
      }

    val projectedElev = vecs.map { projectVec3(rotateVec3(it.first, yawDeg, pitchDeg), center, scale) }
    val projectedFloor = vecs.map { projectVec3(rotateVec3(it.second, yawDeg, pitchDeg), center, scale) }

    // Elevation vertical drop curtains
    for (i in projectedElev.indices) {
      drawLine(
        color = routeColor.copy(alpha = 0.26f),
        start = projectedElev[i].first,
        end = projectedFloor[i].first,
        strokeWidth = 1.5.dp.toPx(),
      )
    }

    // 3D Terrain Route Ribbon
    for (i in 0 until projectedElev.size - 1) {
      draw3DLimb(
        a = projectedElev[i].first,
        b = projectedElev[i + 1].first,
        thickness = 4.5.dp.toPx(),
        baseColor = routeColor,
      )
    }

    drawShaded3DSphere(projectedElev.first().first, 7.dp.toPx(), routeColor)
    drawShaded3DSphere(projectedElev.last().first, 8.dp.toPx(), accentColor)
  }
}

/**
 * Expanding/contracting 3D volumetric sphere for guided breathing sessions.
 */
@Composable
fun Breathing3DSphereCanvas(
  scaleFraction: Float,
  phaseLabel: String,
  secondsLeft: Int,
  coreColor: Color,
  haloColor: Color,
  modifier: Modifier = Modifier,
) {
  Box(
    modifier = modifier.fillMaxWidth().height(230.dp),
    contentAlignment = Alignment.Center,
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val center = Offset(size.width / 2f, size.height / 2f)
      val baseRadius = size.minDimension * 0.20f
      val currentRadius = baseRadius + size.minDimension * 0.19f * scaleFraction.coerceIn(0f, 1f)

      // Outer 3D radial aura
      drawCircle(
        brush = Brush.radialGradient(listOf(haloColor.copy(alpha = 0.42f), Color.Transparent)),
        radius = currentRadius * 1.48f,
        center = center,
      )

      // 3D Geodesic Rings
      for (angleDeg in listOf(-32f, 28f)) {
        val pts =
          (0..32).map { step ->
            val a = (step / 32f) * 2f * PI.toFloat()
            val v = rotateVec3(Vec3(cos(a), sin(a) * 0.35f, sin(a) * 0.92f), angleDeg, 22f)
            Offset(center.x + v.x * currentRadius * 1.15f, center.y + v.y * currentRadius * 1.15f)
          }
        for (i in 0 until pts.size - 1) {
          drawLine(
            color = coreColor.copy(alpha = 0.50f),
            start = pts[i],
            end = pts[i + 1],
            strokeWidth = 2.dp.toPx(),
          )
        }
      }

      drawShaded3DSphere(center, currentRadius, coreColor, shadowAlpha = 0.28f)
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
      Text(
        text = phaseLabel,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = Color.White,
      )
      if (secondsLeft > 0) {
        Text(
          text = "${secondsLeft}s",
          style = MaterialTheme.typography.headlineMedium.copy(fontFamily = JetBrainsMonoFamily),
          color = Color.White,
        )
      }
    }
  }
}
