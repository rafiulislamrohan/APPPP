package com.example.data.remote

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class StyleProfileInput(
  val heightCm: Double,
  val weightKg: Double,
  val skinToneLabel: String,
  val undertone: String,
  val bodyBuild: String,
  val occasion: String,
  val seasonOrWeather: String,
  val stylePreference: String,
  val notes: String = "",
)

@Serializable
data class StyleColorSwatch(
  val name: String,
  val hex: String,
  val role: String,
)

@Serializable
data class OutfitCombination(
  val title: String,
  val occasion: String,
  val topPiece: String,
  val topColorName: String,
  val topColorHex: String,
  val bottomPiece: String,
  val bottomColorName: String,
  val bottomColorHex: String,
  val footwearOrLayer: String,
  val accentColorName: String,
  val accentHex: String,
  val fitTip: String,
  val whyItWorks: String,
)

@Serializable
data class StyleRecommendationResult(
  val profileSummary: String,
  val bestPaletteColors: List<StyleColorSwatch>,
  val avoidColors: List<String>,
  val outfitCombinations: List<OutfitCombination>,
)

interface StyleAdvisorService {
  fun isNetworkAvailable(): Boolean
  suspend fun generateOutfitSuggestions(input: StyleProfileInput): StyleRecommendationResult
}

class FirebaseStyleAdvisorService(private val context: Context) : StyleAdvisorService {

  companion object {
    private const val TAG = "FirebaseStyleAdvisor"
    private const val MODEL_NAME = "gemini-3.8-flash"
    private const val TIMEOUT_MS = 60_000L
  }

  private val json = Json { ignoreUnknownKeys = true }

  private val responseSchema by lazy {
    Schema.obj(
      properties =
        mapOf(
          "profileSummary" to
            Schema.string(
              description =
                "Concise 2-sentence styling rationale tailored to the user's height, weight/proportions, build, and skin undertone."
            ),
          "bestPaletteColors" to
            Schema.array(
              items =
                Schema.obj(
                  properties =
                    mapOf(
                      "name" to Schema.string(description = "Human-readable shade name, e.g., Deep Olive"),
                      "hex" to Schema.string(description = "Valid 6-digit hex color starting with #, e.g., #4B5D45"),
                      "role" to Schema.string(description = "Wardrobe role such as Base Neutral, Accent Pop, or Layering"),
                    )
                ),
              description = "4 to 5 flattering dress and clothing palette colors with valid #RRGGBB hex codes.",
            ),
          "avoidColors" to
            Schema.array(
              items = Schema.string(),
              description = "2 to 3 color shades less flattering for the specified skin undertone.",
            ),
          "outfitCombinations" to
            Schema.array(
              items =
                Schema.obj(
                  properties =
                    mapOf(
                      "title" to Schema.string(description = "Outfit look title"),
                      "occasion" to Schema.string(description = "When to wear this look"),
                      "topPiece" to Schema.string(description = "Specific top/shirt/dress/kurta/jacket description"),
                      "topColorName" to Schema.string(description = "Color name for the top garment"),
                      "topColorHex" to Schema.string(description = "Valid #RRGGBB hex code for the top color"),
                      "bottomPiece" to Schema.string(description = "Specific trousers/skirt/chinos/leggings or dress pairing"),
                      "bottomColorName" to Schema.string(description = "Color name for the bottom garment"),
                      "bottomColorHex" to Schema.string(description = "Valid #RRGGBB hex code for the bottom color"),
                      "footwearOrLayer" to Schema.string(description = "Recommended shoes, blazer, overshirt, or accessory"),
                      "accentColorName" to Schema.string(description = "Accent/accessory color name"),
                      "accentHex" to Schema.string(description = "Valid #RRGGBB hex code for footwear or accent"),
                      "fitTip" to Schema.string(description = "Proportion & tailoring guidance specific to height, weight, and build"),
                      "whyItWorks" to Schema.string(description = "Why this dress color combination flatters skin tone and occasion"),
                    )
                ),
              description = "3 distinct, complete outfit and dress color combinations.",
            ),
        )
    )
  }

  private val model by lazy {
    Firebase.ai.generativeModel(
      modelName = MODEL_NAME,
      generationConfig =
        generationConfig {
          responseMimeType = "application/json"
          responseSchema = this@FirebaseStyleAdvisorService.responseSchema
          temperature = 0.65f
        },
      systemInstruction =
        content {
          text(
            """
            You are VITALTRACK Personal Style & Color Harmony Stylist.
            Provide practical, body-positive, proportion-aware clothing and dress color combinations based on the user's height, weight, build, skin tone/undertone, occasion, weather, and preferences.
            Rules:
            1. Always return valid 6-digit hex color codes (#RRGGBB) that accurately depict each named garment shade.
            2. Focus on flattering cuts, garment proportions (e.g., monochrome elongation, waist/shoulder balance, breathable fabrics), and color contrast harmony.
            3. Keep recommendations respectful, inclusive, practical, and free of body shaming or medical claims.
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

  override suspend fun generateOutfitSuggestions(input: StyleProfileInput): StyleRecommendationResult {
    if (!isNetworkAvailable()) {
      throw IllegalStateException(
        "Internet connection required for Live Smart AI clothing & color suggestions."
      )
    }

    val bmi =
      if (input.heightCm > 0) {
        val m = input.heightCm / 100.0
        input.weightKg / (m * m)
      } else {
        22.0
      }

    val prompt =
      """
      Recommend 3 complete outfit & dress color combinations tailored for:
      - Height: ${input.heightCm.toInt()} cm
      - Weight: ${String.format(java.util.Locale.US, "%.1f", input.weightKg)} kg (reference BMI ~${String.format(java.util.Locale.US, "%.1f", bmi)})
      - Skin Tone & Undertone: ${input.skinToneLabel} (${input.undertone} undertone)
      - Body Silhouette / Build: ${input.bodyBuild}
      - Occasion / Setting: ${input.occasion}
      - Season / Weather: ${input.seasonOrWeather}
      - Style Aesthetic: ${input.stylePreference}
      ${if (input.notes.isNotBlank()) "- User Notes: ${input.notes.trim()}" else ""}

      Ensure each outfit lists exact garment pieces, matching #RRGGBB hex colors for top, bottom, and accent/layer, plus concise fit & color-harmony reasoning.
      """.trimIndent()

    return try {
      val response =
        withTimeout(TIMEOUT_MS) {
          model.generateContent(prompt)
        }
      val rawJson =
        response.text?.takeIf { it.isNotBlank() }
          ?: throw IllegalStateException("AI Stylist returned an empty response. Please try again.")
      json.decodeFromString<StyleRecommendationResult>(rawJson)
    } catch (e: TimeoutCancellationException) {
      Log.e(TAG, "AI Style suggestion request timed out", e)
      throw IllegalStateException("Style suggestion timed out after 60 seconds. Please try again.", e)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Log.e(TAG, "AI Style suggestion generation failed", e)
      throw e
    }
  }
}
