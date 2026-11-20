package com.example.data.remote

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.content
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout

interface AiCoachService {
  fun isNetworkAvailable(): Boolean

  fun streamWellnessCoaching(
    userPrompt: String,
    wellnessContextSummary: String,
  ): Flow<AiStreamChunk>
}

sealed interface AiStreamChunk {
  data class Partial(val accumulatedText: String) : AiStreamChunk
  data class Complete(val fullText: String) : AiStreamChunk
}

class FirebaseAiCoachService(private val context: Context) : AiCoachService {

  companion object {
    private const val TAG = "FirebaseAiCoach"
    private const val MODEL_NAME = "gemini-3.8-flash"
    private const val TIMEOUT_MS = 60_000L
  }

  private val model by lazy {
    Firebase.ai.generativeModel(
      modelName = MODEL_NAME,
      systemInstruction =
        content {
          text(
            """
            You are VITALTRACK Coach, a supportive, evidence-aligned personal wellness assistant.
            Rules you must strictly follow:
            1. Base your observations ONLY on the real recorded user metrics provided in context. Never invent or fabricate measurements, heart rates, distances, or sleep figures.
            2. Clearly remind users when appropriate that advice is informational only and not medical diagnosis or treatment. Never diagnose medical conditions.
            3. Avoid extreme, dangerous, or crash exercise/diet recommendations. Recommend safe, progressive, sustainable habits.
            4. Keep answers structured, concise, warm, and actionable.
            """.trimIndent()
          )
        },
    )
  }

  override fun isNetworkAvailable(): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
    val network = cm.activeNetwork ?: return false
    val caps = cm.getNetworkCapabilities(network) ?: return false
    return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
      caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
  }

  override fun streamWellnessCoaching(
    userPrompt: String,
    wellnessContextSummary: String,
  ): Flow<AiStreamChunk> = flow {
    if (!isNetworkAvailable()) {
      throw IllegalStateException(
        "You are currently offline. AI Wellness Coach requires an internet connection, while all local tracking continues offline."
      )
    }

    val composedPrompt =
      """
      User's recorded VITALTRACK summary (actual measurements & entries only):
      $wellnessContextSummary

      User request:
      $userPrompt
      """.trimIndent()

    val accumulated = StringBuilder()
    try {
      withTimeout(TIMEOUT_MS) {
        model.generateContentStream(composedPrompt).collect { chunk ->
          val part = chunk.text.orEmpty()
          if (part.isNotEmpty()) {
            accumulated.append(part)
            emit(AiStreamChunk.Partial(accumulated.toString()))
          }
        }
      }
      val finalOutput = accumulated.toString().trim()
      if (finalOutput.isBlank()) {
        throw IllegalStateException("AI Coach returned an empty response. Please try again.")
      }
      emit(AiStreamChunk.Complete(finalOutput))
    } catch (e: TimeoutCancellationException) {
      Log.e(TAG, "AI Coach request timed out", e)
      throw IllegalStateException("AI Coach timed out after 60 seconds. Please try again.", e)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Log.e(TAG, "AI Coach generation failed", e)
      throw e
    }
  }
}
