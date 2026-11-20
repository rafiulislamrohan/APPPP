package com.example.data.health

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

enum class SoundscapeMode(val label: String) {
  OFF("Silent"),
  BROWN_NOISE("Deep Brown Noise"),
  RAIN("Gentle Rain"),
  FOREST_WIND("Forest Wind"),
}

/**
 * Lightweight zero-asset audio & haptic biofeedback synthesizer using Android AudioTrack,
 * ToneGenerator, and Vibrator APIs for soundscapes, interval beeps, running cadence, and
 * eyes-closed breathing cues.
 */
object BiofeedbackAudioEngine {
  private var soundJob: Job? = null
  private val engineScope = CoroutineScope(Dispatchers.Default)

  fun triggerHapticPulse(context: Context, durationMs: Long = 55L, amplitude: Int = 120) {
    runCatching {
      val vibrator =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
          val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
          vm?.defaultVibrator
        } else {
          @Suppress("DEPRECATION")
          context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        } ?: return

      if (!vibrator.hasVibrator()) return
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator.vibrate(
          VibrationEffect.createOneShot(durationMs.coerceIn(15L, 400L), amplitude.coerceIn(30, 255))
        )
      } else {
        @Suppress("DEPRECATION")
        vibrator.vibrate(durationMs.coerceIn(15L, 400L))
      }
    }
  }

  fun playShortTone(toneType: Int = ToneGenerator.TONE_PROP_BEEP, durationMs: Int = 80) {
    runCatching {
      val tg = ToneGenerator(AudioManager.STREAM_MUSIC, 65)
      tg.startTone(toneType, durationMs)
      engineScope.launch {
        kotlinx.coroutines.delay(durationMs + 40L)
        runCatching { tg.release() }
      }
    }
  }

  fun startSoundscape(mode: SoundscapeMode, volumeFraction: Float = 0.35f) {
    stopSoundscape()
    if (mode == SoundscapeMode.OFF) return

    soundJob =
      engineScope.launch {
        var track: AudioTrack? = null
        try {
          val sampleRate = 16000
          val minBuf =
            AudioTrack.getMinBufferSize(
              sampleRate,
              AudioFormat.CHANNEL_OUT_MONO,
              AudioFormat.ENCODING_PCM_16BIT,
            ).coerceAtLeast(2048)

          track =
            AudioTrack.Builder()
              .setAudioAttributes(
                AudioAttributes.Builder()
                  .setUsage(AudioAttributes.USAGE_MEDIA)
                  .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                  .build()
              )
              .setAudioFormat(
                AudioFormat.Builder()
                  .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                  .setSampleRate(sampleRate)
                  .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                  .build()
              )
              .setBufferSizeInBytes(minBuf)
              .setTransferMode(AudioTrack.MODE_STREAM)
              .build()

          track.play()
          val buffer = ShortArray(1024)
          var lastBrown = 0f
          var lfoPhase = 0.0
          val rng = Random(42)

          while (isActive) {
            val vol = volumeFraction.coerceIn(0.05f, 1.0f)
            for (i in buffer.indices) {
              val white = rng.nextFloat() * 2f - 1f
              val sample =
                when (mode) {
                  SoundscapeMode.BROWN_NOISE -> {
                    lastBrown = (lastBrown + 0.03f * white).coerceIn(-1f, 1f) * 0.992f
                    lastBrown * 0.75f
                  }
                  SoundscapeMode.RAIN -> {
                    lastBrown = (lastBrown + 0.08f * white).coerceIn(-1f, 1f) * 0.96f
                    val drop = if (rng.nextFloat() < 0.004f) white * 0.65f else 0f
                    (lastBrown * 0.45f + white * 0.14f + drop).coerceIn(-1f, 1f)
                  }
                  SoundscapeMode.FOREST_WIND -> {
                    lfoPhase += 0.00018
                    val swell = (0.55 + 0.45 * sin(lfoPhase)).toFloat()
                    lastBrown = (lastBrown + 0.025f * white).coerceIn(-1f, 1f) * 0.99f
                    (lastBrown * swell * 0.85f).coerceIn(-1f, 1f)
                  }
                  SoundscapeMode.OFF -> 0f
                }
              buffer[i] = (sample * vol * 14000f).toInt().coerceIn(-32767, 32767).toShort()
            }
            track.write(buffer, 0, buffer.size)
          }
        } catch (_: Throwable) {
          // AudioTrack may be unavailable in Robolectric or headless test containers.
        } finally {
          runCatching {
            track?.stop()
            track?.release()
          }
        }
      }
  }

  fun stopSoundscape() {
    soundJob?.cancel()
    soundJob = null
  }
}
