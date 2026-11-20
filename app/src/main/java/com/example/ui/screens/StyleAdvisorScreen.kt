package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.UserProfilePreferences
import com.example.data.remote.OutfitCombination
import com.example.data.remote.StyleColorSwatch
import com.example.data.remote.StyleProfileInput
import com.example.ui.components.Morphable3DBodyCanvas
import com.example.ui.components.OfflineStatusBanner
import com.example.ui.components.Tactile3DCard
import com.example.ui.components.VitalTopBar
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.viewmodel.StyleAdvisorUiState
import java.util.Locale
import kotlinx.coroutines.delay

private data class SkinToneOption(
  val key: String,
  val label: String,
  val defaultUndertone: String,
  val hex: String,
)

private val skinTonePresets =
  listOf(
    SkinToneOption("fair_cool", "Fair Ivory", "Cool", "#F5D6C6"),
    SkinToneOption("light_neutral", "Light Beige", "Neutral", "#E8BFA8"),
    SkinToneOption("medium_warm", "Medium Warm", "Warm", "#C9936B"),
    SkinToneOption("golden_olive", "Golden Olive", "Olive", "#B68756"),
    SkinToneOption("rich_warm", "Rich Deep Warm", "Warm", "#7C4B31"),
    SkinToneOption("cool_espresso", "Deep Espresso", "Cool", "#4E3024"),
  )

private val undertoneOptions = listOf("Warm", "Cool", "Neutral", "Olive")
private val bodyBuildOptions = listOf("Slim", "Athletic", "Balanced", "Broad Shoulders", "Curvy")
private val occasionOptions = listOf("Smart Casual", "Activewear", "Formal / Office", "Evening Out", "Outdoor")
private val weatherOptions = listOf("Warm / Sunny", "Mild / Breezy", "Cool / Layered")
private val styleVibeOptions = listOf("Minimalist", "Classic", "Athleisure", "Earthy Botanical")

private fun parseSafeColor(rawHex: String, fallback: Color): Color {
  val cleaned = rawHex.trim()
  val normalized = if (cleaned.startsWith("#")) cleaned else "#$cleaned"
  if (!Regex("^#[0-9A-Fa-f]{6}$").matches(normalized)) {
    return fallback
  }
  return runCatching { Color(android.graphics.Color.parseColor(normalized)) }.getOrDefault(fallback)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StyleAdvisorScreen(
  preferences: UserProfilePreferences,
  advisorState: StyleAdvisorUiState,
  isOnline: Boolean,
  onGenerateSuggestions: (StyleProfileInput) -> Unit,
  onOpenCloset: () -> Unit = {},
  onBack: () -> Unit,
) {
  BackHandler(onBack = onBack)

  var showJacketLayer by rememberSaveable { mutableStateOf(false) }
  var showScarfLayer by rememberSaveable { mutableStateOf(false) }
  var showHatLayer by rememberSaveable { mutableStateOf(false) }
  var showBeltLayer by rememberSaveable { mutableStateOf(true) }
  var bodyYawDeg by rememberSaveable { mutableStateOf(-20f) }

  var heightInput by rememberSaveable(preferences.heightCm) {
    mutableStateOf(preferences.heightCm.toInt().toString())
  }
  var weightInput by rememberSaveable(preferences.weightKg) {
    mutableStateOf(String.format(Locale.US, "%.1f", preferences.weightKg))
  }
  var selectedToneKey by rememberSaveable { mutableStateOf("medium_warm") }
  var selectedUndertone by rememberSaveable { mutableStateOf("Warm") }
  var selectedBuild by rememberSaveable { mutableStateOf("Balanced") }
  var selectedOccasion by rememberSaveable { mutableStateOf("Smart Casual") }
  var selectedWeather by rememberSaveable { mutableStateOf("Mild / Breezy") }
  var selectedVibe by rememberSaveable { mutableStateOf("Classic") }
  var customNotes by rememberSaveable { mutableStateOf("") }
  var liveAutoSuggest by rememberSaveable { mutableStateOf(false) }
  var hasUserTouchedLive by rememberSaveable { mutableStateOf(false) }
  var previewOutfitIdx by rememberSaveable { mutableStateOf(0) }

  val selectedTone =
    remember(selectedToneKey) {
      skinTonePresets.firstOrNull { it.key == selectedToneKey } ?: skinTonePresets[2]
    }

  val heightCm = heightInput.toDoubleOrNull()?.coerceIn(120.0, 230.0) ?: preferences.heightCm
  val weightKg = weightInput.toDoubleOrNull()?.coerceIn(30.0, 250.0) ?: preferences.weightKg
  val bmi =
    remember(heightCm, weightKg) {
      val m = heightCm / 100.0
      if (m > 0) weightKg / (m * m) else 22.0
    }

  val currentInput =
    remember(
      heightCm,
      weightKg,
      selectedTone,
      selectedUndertone,
      selectedBuild,
      selectedOccasion,
      selectedWeather,
      selectedVibe,
      customNotes,
    ) {
      StyleProfileInput(
        heightCm = heightCm,
        weightKg = weightKg,
        skinToneLabel = selectedTone.label,
        undertone = selectedUndertone,
        bodyBuild = selectedBuild,
        occasion = selectedOccasion,
        seasonOrWeather = selectedWeather,
        stylePreference = selectedVibe,
        notes = customNotes.trim(),
      )
    }

  LaunchedEffect(liveAutoSuggest, currentInput, isOnline) {
    if (liveAutoSuggest && isOnline && hasUserTouchedLive) {
      delay(900L)
      onGenerateSuggestions(currentInput)
    }
  }

  Scaffold(
    topBar = { VitalTopBar(title = "AI Style & Dress Colors", onBack = onBack) }
  ) { innerPadding ->
    Column(
      modifier =
        Modifier.fillMaxSize()
          .padding(innerPadding)
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 16.dp, vertical = 12.dp)
          .widthIn(max = 640.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      if (!isOnline) {
        OfflineStatusBanner()
      }

      // Header Intro Card
      Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(16.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(44.dp),
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Outlined.Checkroom,
                contentDescription = "AI Wardrobe Stylist",
                tint = MaterialTheme.colorScheme.onPrimary,
              )
            }
          }
          Column(modifier = Modifier.weight(1f)) {
            Text(
              "Smart Outfit & Color Harmony",
              style = MaterialTheme.typography.titleMedium,
              color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
              "Live Gemini styling calibrated to your height, weight, build, skin tone, and occasion.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
          }
        }
      }

      // Live Interactive 3D Outfit & Skin Tone Stage
      val previewOutfit =
        (advisorState as? StyleAdvisorUiState.Success)
          ?.result
          ?.outfitCombinations
          ?.getOrNull(previewOutfitIdx)
      val stageSkinColor = parseSafeColor(selectedTone.hex, Color(0xFFC9936B))
      val stageTopColor =
        previewOutfit?.let { parseSafeColor(it.topColorHex, MaterialTheme.colorScheme.primary) }
          ?: MaterialTheme.colorScheme.primary
      val stageBottomColor =
        previewOutfit?.let { parseSafeColor(it.bottomColorHex, MaterialTheme.colorScheme.secondary) }
          ?: Color(0xFFD8C6AE)
      val stageAccentColor =
        previewOutfit?.let { parseSafeColor(it.accentHex, MaterialTheme.colorScheme.tertiary) }
          ?: MaterialTheme.colorScheme.tertiary

      Tactile3DCard(
        enabled3D = preferences.threeDVisualsEnabled,
        modifier = Modifier.fillMaxWidth().testTag("style_3d_stage"),
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text(
            text = previewOutfit?.let { "3D Look: ${it.title}" } ?: "Live 3D Fit, Layering & Tone Preview",
            style = MaterialTheme.typography.titleSmall,
          )
          Morphable3DBodyCanvas(
            chestCm = preferences.bodyChestCm,
            waistCm = preferences.bodyWaistCm,
            hipCm = preferences.bodyHipCm,
            upperFatigueRatio = 0f,
            coreFatigueRatio = 0f,
            legFatigueRatio = 0f,
            showMuscleHeatmap = false,
            showJacket = showJacketLayer,
            showScarf = showScarfLayer,
            showHat = showHatLayer,
            showBelt = showBeltLayer,
            topColor = stageTopColor,
            bottomColor = stageBottomColor,
            accentColor = stageAccentColor,
            yawDeg = bodyYawDeg,
            onYawDrag = { bodyYawDeg += it },
          )
          Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(selected = showJacketLayer, onClick = { showJacketLayer = !showJacketLayer }, label = { Text("+Jacket") })
            FilterChip(selected = showScarfLayer, onClick = { showScarfLayer = !showScarfLayer }, label = { Text("+Scarf") })
            FilterChip(selected = showHatLayer, onClick = { showHatLayer = !showHatLayer }, label = { Text("+Hat") })
            FilterChip(selected = showBeltLayer, onClick = { showBeltLayer = !showBeltLayer }, label = { Text("+Belt") })
          }
          androidx.compose.material3.OutlinedButton(
            onClick = onOpenCloset,
            modifier = Modifier.fillMaxWidth().testTag("style_open_closet_btn"),
          ) {
            Text("Open Digital Closet, Color Eyedropper & AI Outfit Check")
          }
        }
      }

      // 1. Body Measurements & Silhouette Card
      Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Outlined.Straighten, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text("Height, Weight & Proportions", style = MaterialTheme.typography.titleMedium)
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            OutlinedTextField(
              value = heightInput,
              onValueChange = {
                heightInput = it
                hasUserTouchedLive = true
              },
              label = { Text("Height (cm)") },
              singleLine = true,
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              modifier = Modifier.weight(1f).testTag("style_height_input"),
            )
            OutlinedTextField(
              value = weightInput,
              onValueChange = {
                weightInput = it
                hasUserTouchedLive = true
              },
              label = { Text("Weight (kg)") },
              singleLine = true,
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
              modifier = Modifier.weight(1f).testTag("style_weight_input"),
            )
          }

          Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
          ) {
            Text(
              text = "Reference Fit Index: ${heightCm.toInt()} cm • ${String.format(Locale.US, "%.1f", weightKg)} kg (BMI ~${String.format(Locale.US, "%.1f", bmi)})",
              style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFamily),
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            )
          }

          Text("Body Silhouette", style = MaterialTheme.typography.labelLarge)
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            bodyBuildOptions.forEach { build ->
              FilterChip(
                selected = selectedBuild == build,
                onClick = {
                  selectedBuild = build
                  hasUserTouchedLive = true
                },
                label = { Text(build) },
                modifier = Modifier.testTag("style_build_${build.lowercase().replace(" ", "_")}"),
              )
            }
          }
        }
      }

      // 2. Skin Color & Undertone Swatch Selector
      Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Outlined.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text("Body / Skin Tone & Undertone", style = MaterialTheme.typography.titleMedium)
          }

          FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            skinTonePresets.forEach { option ->
              val isSelected = selectedToneKey == option.key
              val swatchColor = parseSafeColor(option.hex, Color(0xFFC9936B))
              Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier =
                  Modifier.clip(MaterialTheme.shapes.medium)
                    .clickable {
                      selectedToneKey = option.key
                      selectedUndertone = option.defaultUndertone
                      hasUserTouchedLive = true
                    }
                    .padding(6.dp)
                    .testTag("style_tone_${option.key}"),
              ) {
                Box(
                  modifier =
                    Modifier.size(48.dp)
                      .clip(CircleShape)
                      .background(swatchColor)
                      .border(
                        width = if (isSelected) 3.dp else 1.dp,
                        color =
                          if (isSelected) MaterialTheme.colorScheme.primary
                          else MaterialTheme.colorScheme.outlineVariant,
                        shape = CircleShape,
                      )
                      .semantics { contentDescription = "${option.label} skin tone swatch" },
                  contentAlignment = Alignment.Center,
                ) {
                  if (isSelected) {
                    Icon(
                      imageVector = Icons.Outlined.Check,
                      contentDescription = "Selected skin tone",
                      tint = if (swatchColor.red > 0.65f) Color(0xFF1F2421) else Color.White,
                      modifier = Modifier.size(20.dp),
                    )
                  }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = option.label,
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
            }
          }

          Text("Skin Undertone", style = MaterialTheme.typography.labelLarge)
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            undertoneOptions.forEach { tone ->
              FilterChip(
                selected = selectedUndertone == tone,
                onClick = {
                  selectedUndertone = tone
                  hasUserTouchedLive = true
                },
                label = { Text(tone) },
                modifier = Modifier.testTag("style_undertone_${tone.lowercase()}"),
              )
            }
          }
        }
      }

      // 3. Occasion, Weather, Style Aesthetic & Live Generation Controls
      Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Text("Occasion & Dress Context", style = MaterialTheme.typography.titleMedium)

          Text("Occasion", style = MaterialTheme.typography.labelLarge)
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            occasionOptions.forEach { occ ->
              FilterChip(
                selected = selectedOccasion == occ,
                onClick = {
                  selectedOccasion = occ
                  hasUserTouchedLive = true
                },
                label = { Text(occ) },
                modifier = Modifier.testTag("style_occasion_${occ.lowercase().replace(Regex("[^a-z0-9]+"), "_")}"),
              )
            }
          }

          Text("Weather / Season", style = MaterialTheme.typography.labelLarge)
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            weatherOptions.forEach { w ->
              FilterChip(
                selected = selectedWeather == w,
                onClick = {
                  selectedWeather = w
                  hasUserTouchedLive = true
                },
                label = { Text(w) },
              )
            }
          }

          Text("Style Aesthetic", style = MaterialTheme.typography.labelLarge)
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            styleVibeOptions.forEach { vibe ->
              FilterChip(
                selected = selectedVibe == vibe,
                onClick = {
                  selectedVibe = vibe
                  hasUserTouchedLive = true
                },
                label = { Text(vibe) },
              )
            }
          }

          OutlinedTextField(
            value = customNotes,
            onValueChange = {
              customNotes = it
              hasUserTouchedLive = true
            },
            label = { Text("Color / Dress Preference Notes (Optional)") },
            placeholder = { Text("e.g., prefer earth tones, breathable cotton, blazer...") },
            maxLines = 2,
            modifier = Modifier.fillMaxWidth().testTag("style_notes_input"),
          )

          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text("Live Auto-Suggest on Change", style = MaterialTheme.typography.titleSmall)
              Text(
                "Automatically refreshes AI outfits when inputs change",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            Switch(
              checked = liveAutoSuggest,
              onCheckedChange = { enabled ->
                liveAutoSuggest = enabled
                hasUserTouchedLive = true
              },
              modifier = Modifier.testTag("style_live_toggle"),
            )
          }

          Button(
            onClick = { onGenerateSuggestions(currentInput) },
            enabled = isOnline && advisorState !is StyleAdvisorUiState.Loading,
            modifier = Modifier.fillMaxWidth().testTag("style_generate_button"),
          ) {
            Icon(Icons.Outlined.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              if (advisorState is StyleAdvisorUiState.Loading) "Generating Smart Outfits..."
              else "Get Smart Outfit & Color Combos"
            )
          }
        }
      }

      // 4. AI Output Section
      when (advisorState) {
        StyleAdvisorUiState.Idle -> {
          Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
          ) {
            Column(
              modifier = Modifier.fillMaxWidth().padding(18.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Text("Ready to Curate Your Palette", style = MaterialTheme.typography.titleMedium)
              Text(
                "Adjust your height, weight, skin tone swatch, and occasion above, then tap Get Smart Outfit & Color Combos to receive live color-matched clothing combinations.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }

        StyleAdvisorUiState.Loading -> {
          Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(20.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
              CircularProgressIndicator(modifier = Modifier.size(28.dp))
              Column {
                Text("Analyzing color harmony & fit...", style = MaterialTheme.typography.titleSmall)
                Text(
                  "Pairing dress shades for ${selectedTone.label} (${selectedUndertone}) • ${heightCm.toInt()} cm",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }
        }

        is StyleAdvisorUiState.Error -> {
          Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
            modifier = Modifier.testTag("style_error_card"),
          ) {
            Column(
              modifier = Modifier.fillMaxWidth().padding(16.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Text(
                "Could not generate style recommendations",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
              )
              Text(
                advisorState.message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
              )
            }
          }
        }

        is StyleAdvisorUiState.Success -> {
          val result = advisorState.result

          Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
            modifier = Modifier.testTag("style_summary_card"),
          ) {
            Column(
              modifier = Modifier.fillMaxWidth().padding(16.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Text(
                "Your Tailored Color & Fit Profile",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
              )
              Text(
                result.profileSummary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
              )
            }
          }

          if (result.bestPaletteColors.isNotEmpty() || result.avoidColors.isNotEmpty()) {
            Card(
              shape = MaterialTheme.shapes.large,
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
              modifier = Modifier.testTag("style_palette_card"),
            ) {
              Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                Text("Best Dress & Wardrobe Colors", style = MaterialTheme.typography.titleMedium)
                Row(
                  modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                  horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                  result.bestPaletteColors.forEach { swatch ->
                    PaletteSwatchBadge(swatch)
                  }
                }
                if (result.avoidColors.isNotEmpty()) {
                  HorizontalDivider()
                  Text("Shades to Limit", style = MaterialTheme.typography.labelLarge)
                  FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                  ) {
                    result.avoidColors.forEach { shade ->
                      Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                      ) {
                        Text(
                          text = shade,
                          style = MaterialTheme.typography.labelMedium,
                          color = MaterialTheme.colorScheme.onSurfaceVariant,
                          modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                      }
                    }
                  }
                }
              }
            }
          }

          Text("Recommended Dress Color Combinations", style = MaterialTheme.typography.titleMedium)
          result.outfitCombinations.forEachIndexed { index, outfit ->
            OutfitCombinationCard(
              index = index,
              outfit = outfit,
              isPreviewedIn3D = previewOutfitIdx == index,
              onPreviewIn3D = { previewOutfitIdx = index },
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(20.dp))
    }
  }
}

@Composable
private fun PaletteSwatchBadge(swatch: StyleColorSwatch) {
  val color = parseSafeColor(swatch.hex, MaterialTheme.colorScheme.primary)
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier.width(82.dp),
  ) {
    Box(
      modifier =
        Modifier.size(44.dp)
          .clip(CircleShape)
          .background(color)
          .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
          .semantics { contentDescription = "${swatch.name} (${swatch.hex}) color swatch" }
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
      text = swatch.name,
      style = MaterialTheme.typography.labelMedium,
      fontWeight = FontWeight.SemiBold,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
    Text(
      text = swatch.role,
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
    Text(
      text = swatch.hex.uppercase(),
      style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
      color = MaterialTheme.colorScheme.primary,
    )
  }
}

@Composable
private fun OutfitCombinationCard(
  index: Int,
  outfit: OutfitCombination,
  isPreviewedIn3D: Boolean = false,
  onPreviewIn3D: () -> Unit = {},
) {
  val topColor = parseSafeColor(outfit.topColorHex, MaterialTheme.colorScheme.primary)
  val bottomColor = parseSafeColor(outfit.bottomColorHex, MaterialTheme.colorScheme.secondary)
  val accentColor = parseSafeColor(outfit.accentHex, MaterialTheme.colorScheme.tertiary)

  Tactile3DCard(
    shape = MaterialTheme.shapes.large,
    containerColor = MaterialTheme.colorScheme.surface,
    modifier = Modifier.fillMaxWidth().testTag("style_outfit_card_$index"),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = outfit.title,
          style = MaterialTheme.typography.titleMedium,
          modifier = Modifier.weight(1f),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = MaterialTheme.colorScheme.primaryContainer,
        ) {
          Text(
            text = outfit.occasion,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
          )
        }
      }

      // 3-Color Harmony Swatch Strip
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        GarmentColorPill(
          slotLabel = "Top",
          colorName = outfit.topColorName,
          hex = outfit.topColorHex,
          swatchColor = topColor,
          modifier = Modifier.weight(1f),
        )
        GarmentColorPill(
          slotLabel = "Bottom",
          colorName = outfit.bottomColorName,
          hex = outfit.bottomColorHex,
          swatchColor = bottomColor,
          modifier = Modifier.weight(1f),
        )
        GarmentColorPill(
          slotLabel = "Accent",
          colorName = outfit.accentColorName,
          hex = outfit.accentHex,
          swatchColor = accentColor,
          modifier = Modifier.weight(1f),
        )
      }

      HorizontalDivider()

      GarmentDetailRow(label = "Top / Dress", value = "${outfit.topPiece} (${outfit.topColorName})")
      GarmentDetailRow(label = "Bottom", value = "${outfit.bottomPiece} (${outfit.bottomColorName})")
      GarmentDetailRow(
        label = "Layer / Shoes",
        value = "${outfit.footwearOrLayer} (${outfit.accentColorName})",
      )

      Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(10.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          Text(
            "Fit & Proportion Tip: ${outfit.fitTip}",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
          )
          Text(
            "Why Color Combo Works: ${outfit.whyItWorks}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      FilterChip(
        selected = isPreviewedIn3D,
        onClick = onPreviewIn3D,
        label = { Text(if (isPreviewedIn3D) "Showing on 3D Model" else "Preview on 3D Model") },
        modifier = Modifier.testTag("style_preview_3d_$index"),
      )
    }
  }
}

@Composable
private fun GarmentColorPill(
  slotLabel: String,
  colorName: String,
  hex: String,
  swatchColor: Color,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(10.dp),
    color = MaterialTheme.colorScheme.surfaceVariant,
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Box(
        modifier =
          Modifier.size(24.dp)
            .clip(CircleShape)
            .background(swatchColor)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
            .semantics { contentDescription = "$slotLabel color $colorName" }
      )
      Column(modifier = Modifier.weight(1f)) {
        Text(slotLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
          text = colorName,
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.SemiBold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = hex.uppercase(),
          style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
          color = MaterialTheme.colorScheme.primary,
          maxLines = 1,
        )
      }
    }
  }
}

@Composable
private fun GarmentDetailRow(label: String, value: String) {
  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    Text(
      text = "$label:",
      style = MaterialTheme.typography.bodySmall,
      fontWeight = FontWeight.SemiBold,
      color = MaterialTheme.colorScheme.primary,
      modifier = Modifier.width(88.dp),
    )
    Text(
      text = value,
      style = MaterialTheme.typography.bodySmall,
      modifier = Modifier.weight(1f),
    )
  }
}
