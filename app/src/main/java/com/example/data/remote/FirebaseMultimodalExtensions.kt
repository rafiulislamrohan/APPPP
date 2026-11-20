package com.example.data.remote

import android.content.Context
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class MealPhotoEstimate(
  val mealName: String,
  val caloriesKcal: Int,
  val proteinG: Double,
  val carbsG: Double,
  val fatG: Double,
  val fiberG: Double,
  val nutritionNote: String,
)

@Serializable
data class DressCheckResult(
  val harmonyScore: Int,
  val paletteMatch: String,
  val proportionFeedback: String,
  val quickPolishTip: String,
)

class FirebaseMultimodalExtensions(private val context: Context) {
  companion object {
    private const val MODEL_NAME = "gemini-3.8-flash"
    private const val TIMEOUT_MS = 60_000L
  }

  private val json = Json { ignoreUnknownKeys = true }

  fun isNetworkAvailable(): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
    val net = cm.activeNetwork ?: return false
    val caps = cm.getNetworkCapabilities(net) ?: return false
    return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
      caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
  }

  suspend fun analyzeMealPhoto(bitmap: Bitmap): MealPhotoEstimate {
    if (!isNetworkAvailable()) {
      throw IllegalStateException("Internet connection required to analyze meal photos with Gemini.")
    }
    val schema =
      Schema.obj(
        properties =
          mapOf(
            "mealName" to Schema.string(description = "Short descriptive name of the meal"),
            "caloriesKcal" to Schema.integer(description = "Estimated total calories in kcal"),
            "proteinG" to Schema.double(description = "Estimated protein in grams"),
            "carbsG" to Schema.double(description = "Estimated carbohydrates in grams"),
            "fatG" to Schema.double(description = "Estimated fat in grams"),
            "fiberG" to Schema.double(description = "Estimated dietary fiber in grams"),
            "nutritionNote" to Schema.string(description = "Brief 1-sentence portion/balance note"),
          )
      )
    val model =
      Firebase.ai.generativeModel(
        modelName = MODEL_NAME,
        generationConfig =
          generationConfig {
            responseMimeType = "application/json"
            responseSchema = schema
            temperature = 0.35f
          },
      )
    val response =
      withTimeout(TIMEOUT_MS) {
        model.generateContent(
          content {
            image(bitmap)
            text("Identify this meal and estimate realistic calories and macronutrients (protein, carbs, fat, fiber in grams).")
          }
        )
      }
    val raw = response.text?.takeIf { it.isNotBlank() } ?: error("AI returned empty meal analysis.")
    return json.decodeFromString(raw)
  }

  suspend fun checkOutfitPhoto(bitmap: Bitmap, occasion: String): DressCheckResult {
    if (!isNetworkAvailable()) {
      throw IllegalStateException("Internet connection required for AI dress-code photo feedback.")
    }
    val schema =
      Schema.obj(
        properties =
          mapOf(
            "harmonyScore" to Schema.integer(description = "1 to 10 color and fit harmony score"),
            "paletteMatch" to Schema.string(description = "Summary of garment colors and contrast harmony"),
            "proportionFeedback" to Schema.string(description = "Fit, layering, and occasion suitability note"),
            "quickPolishTip" to Schema.string(description = "One actionable accessory or styling tweak"),
          )
      )
    val model =
      Firebase.ai.generativeModel(
        modelName = MODEL_NAME,
        generationConfig =
          generationConfig {
            responseMimeType = "application/json"
            responseSchema = schema
            temperature = 0.5f
          },
      )
    val response =
      withTimeout(TIMEOUT_MS) {
        model.generateContent(
          content {
            image(bitmap)
            text("Evaluate this outfit for '$occasion'. Score color harmony (1-10), note garment proportions, and offer one practical styling tweak.")
          }
        )
      }
    val raw = response.text?.takeIf { it.isNotBlank() } ?: error("AI returned empty outfit check.")
    return json.decodeFromString(raw)
  }

  suspend fun generateWeeklyHabitDigest(summaryText: String): String {
    if (!isNetworkAvailable()) {
      throw IllegalStateException("Internet connection required for AI Weekly Habit Digest.")
    }
    val model = Firebase.ai.generativeModel(modelName = MODEL_NAME)
    val response =
      withTimeout(TIMEOUT_MS) {
        model.generateContent(
          "You are a concise wellness habit coach. Review this user's week and output: (1) Top Win, (2) Slip Pattern, and (3) One Tiny Adjustment for Monday.\n\n$summaryText"
        )
      }
    return response.text?.takeIf { it.isNotBlank() } ?: error("AI returned empty weekly digest.")
  }
}
