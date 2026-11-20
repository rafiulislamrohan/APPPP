package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.Colorize
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Luggage
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.health.WellnessAdvancedCalculators
import com.example.data.local.UserProfilePreferences
import com.example.data.local.WardrobeItemEntity
import com.example.ui.components.OfflineStatusBanner
import com.example.ui.components.Tactile3DCard
import com.example.ui.components.VitalTopBar
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.viewmodel.DressCheckUiState

/**
 * Digital Closet, 12-Season Color Palette, Capsule Packing & AI Dress Checker (#21-#30).
 */
@Composable
fun WardrobeClosetScreen(
  preferences: UserProfilePreferences,
  wardrobeItems: List<WardrobeItemEntity>,
  dressCheckState: DressCheckUiState,
  isOnline: Boolean,
  onAddWardrobeItem: (name: String, category: String, colorName: String, colorHex: String, season: String, occasion: String) -> Unit,
  onMarkWorn: (String) -> Unit,
  onToggleFavorite: (String) -> Unit,
  onDeleteItem: (String) -> Unit,
  onCheckOutfitPhoto: (Bitmap, occasion: String) -> Unit,
  onBack: () -> Unit,
) {
  BackHandler(onBack = onBack)
  val context = LocalContext.current

  var itemName by remember { mutableStateOf("") }
  var itemCategory by remember { mutableStateOf("TOP") }
  var itemColorName by remember { mutableStateOf("Olive Green") }
  var itemColorHex by remember { mutableStateOf("#4A5D4E") }
  var itemSeason by remember { mutableStateOf("ALL_SEASON") }
  var itemOccasion by remember { mutableStateOf("CASUAL") }
  var checkOccasion by remember { mutableStateOf("Smart Casual") }
  var undertone by remember { mutableStateOf("Warm") }
  var skinToneLabel by remember { mutableStateOf("Medium Warm") }

  val seasonProfile = remember(undertone, skinToneLabel, preferences.heightCm, preferences.weightKg) {
    WellnessAdvancedCalculators.analyzeSeasonalPalette(undertone, skinToneLabel, preferences.heightCm, preferences.weightKg, "Balanced")
  }
  val capsuleList = remember(wardrobeItems) {
    WellnessAdvancedCalculators.generateCapsulePackingList(wardrobeItems, days = 7)
  }

  fun decodeBitmap(uri: Uri): Bitmap? = runCatching {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, _, _ ->
        decoder.isMutableRequired = true
      }
    } else {
      @Suppress("DEPRECATION")
      MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
    }
  }.getOrNull()

  val eyedropperLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
    if (uri != null) {
      val bmp = decodeBitmap(uri)
      if (bmp != null) {
        val px = bmp.getPixel(bmp.width / 2, bmp.height / 2)
        val hex = String.format("#%06X", 0xFFFFFF and px)
        itemColorHex = hex
        itemColorName = "Sampled Fabric ($hex)"
      }
    }
  }

  val dressCheckLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
    if (uri != null) {
      val bmp = decodeBitmap(uri)
      if (bmp != null) onCheckOutfitPhoto(bmp, checkOccasion)
    }
  }

  Scaffold(topBar = { VitalTopBar(title = "Closet, Color & AI Outfit Check", onBack = onBack) }) { innerPadding ->
    Column(
      modifier =
        Modifier.fillMaxSize()
          .padding(innerPadding)
          .verticalScroll(rememberScrollState())
          .padding(16.dp)
          .widthIn(max = 640.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      if (!isOnline) OfflineStatusBanner()

      // #25 & #26 12-Season Color & Tailoring Card
      Tactile3DCard(enabled3D = preferences.threeDVisualsEnabled) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("12-Season Color & Tailoring Profile: ${seasonProfile.seasonName}", style = MaterialTheme.typography.titleMedium)
          Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("Warm", "Cool", "Neutral").forEach { tone ->
              FilterChip(selected = undertone == tone, onClick = { undertone = tone }, label = { Text(tone) })
            }
            listOf("Fair Ivory", "Medium Warm", "Deep Rich").forEach { skin ->
              FilterChip(selected = skinToneLabel == skin, onClick = { skinToneLabel = skin }, label = { Text(skin) })
            }
          }
          Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            seasonProfile.swatches.forEach { (name, hex) ->
              val col = runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(MaterialTheme.colorScheme.primary)
              Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(18.dp).clip(CircleShape).background(col))
                Spacer(modifier = Modifier.width(4.dp))
                Text(name, style = MaterialTheme.typography.labelSmall)
              }
            }
          }
          Text(seasonProfile.tailoringGuidance, style = MaterialTheme.typography.bodySmall)
        }
      }

      // #21 Digital Closet + #22 Photo Color Eyedropper
      Tactile3DCard(enabled3D = preferences.threeDVisualsEnabled) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Outlined.Checkroom, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text("Add to Digital Closet", style = MaterialTheme.typography.titleMedium)
          }
          OutlinedTextField(
            value = itemName,
            onValueChange = { itemName = it },
            label = { Text("Garment Name (e.g., Linen Blazer)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("closet_item_name"),
          )
          Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("TOP", "BOTTOM", "DRESS", "OUTERWEAR", "FOOTWEAR").forEach { cat ->
              FilterChip(selected = itemCategory == cat, onClick = { itemCategory = cat }, label = { Text(cat) })
            }
          }
          Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("ALL_SEASON", "SUMMER", "WINTER", "RAIN").forEach { s ->
              FilterChip(selected = itemSeason == s, onClick = { itemSeason = s }, label = { Text(s) })
            }
            listOf("CASUAL", "OFFICE", "WORKOUT", "FORMAL").forEach { occ ->
              FilterChip(selected = itemOccasion == occ, onClick = { itemOccasion = occ }, label = { Text(occ) })
            }
          }
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            val previewCol = runCatching { Color(android.graphics.Color.parseColor(itemColorHex)) }.getOrDefault(MaterialTheme.colorScheme.primary)
            Box(modifier = Modifier.size(34.dp).clip(CircleShape).background(previewCol).border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape))
            OutlinedTextField(value = itemColorHex, onValueChange = { itemColorHex = it }, label = { Text("Hex") }, modifier = Modifier.weight(1f), singleLine = true)
            OutlinedButton(onClick = { eyedropperLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
              Icon(Icons.Outlined.Colorize, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Eyedropper")
            }
          }
          Button(
            enabled = itemName.isNotBlank(),
            onClick = {
              onAddWardrobeItem(itemName.trim(), itemCategory, itemColorName, itemColorHex, itemSeason, itemOccasion)
              itemName = ""
            },
            modifier = Modifier.fillMaxWidth().testTag("closet_add_btn"),
          ) { Text("Save Garment to Closet") }
        }
      }

      // #30 Live Gemini Dress-Code Photo Checker
      Tactile3DCard(enabled3D = preferences.threeDVisualsEnabled) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text("AI Dress-Code Photo Checker", style = MaterialTheme.typography.titleMedium)
          }
          Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("Smart Casual", "Formal Office", "Outdoor Sport", "Evening").forEach { occ ->
              FilterChip(selected = checkOccasion == occ, onClick = { checkOccasion = occ }, label = { Text(occ) })
            }
          }
          FilledTonalButton(
            enabled = isOnline && dressCheckState !is DressCheckUiState.Loading,
            onClick = { dressCheckLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            modifier = Modifier.fillMaxWidth().testTag("closet_ai_check_btn"),
          ) {
            Icon(Icons.Outlined.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Pick Outfit Photo for Gemini Verdict")
          }
          when (dressCheckState) {
            DressCheckUiState.Loading -> CircularProgressIndicator(modifier = Modifier.size(22.dp))
            is DressCheckUiState.Success -> {
              Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(10.dp)) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                  Text("Harmony Score: ${dressCheckState.result.harmonyScore}/10 — ${dressCheckState.result.paletteMatch}", style = MaterialTheme.typography.titleSmall)
                  Text("Proportions: ${dressCheckState.result.proportionFeedback}", style = MaterialTheme.typography.bodySmall)
                  Text("Polish Tip: ${dressCheckState.result.quickPolishTip}", style = MaterialTheme.typography.bodySmall)
                }
              }
            }
            is DressCheckUiState.Error -> Text(dressCheckState.message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            DressCheckUiState.Idle -> Unit
          }
        }
      }

      // #27 7-Day Capsule Packing List
      Tactile3DCard(enabled3D = preferences.threeDVisualsEnabled) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Outlined.Luggage, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text("7-Day Capsule Wardrobe & Packing", style = MaterialTheme.typography.titleMedium)
          }
          capsuleList.forEach { line -> Text(line, style = MaterialTheme.typography.bodySmall) }
        }
      }

      if (wardrobeItems.isNotEmpty()) {
        Text("Your Wardrobe (${wardrobeItems.size} items)", style = MaterialTheme.typography.titleMedium)
        wardrobeItems.forEach { item ->
          Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
              val col = runCatching { Color(android.graphics.Color.parseColor(item.colorHex)) }.getOrDefault(MaterialTheme.colorScheme.primary)
              Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(col))
              Spacer(modifier = Modifier.width(10.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(item.name, style = MaterialTheme.typography.titleSmall)
                Text("${item.category} • ${item.seasonTag} • Worn ${item.wornCount}x", style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily))
              }
              OutlinedButton(onClick = { onMarkWorn(item.id) }) { Text("Wear") }
              IconButton(onClick = { onToggleFavorite(item.id) }) {
                Icon(if (item.isFavorite) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder, contentDescription = "Favorite garment")
              }
              IconButton(onClick = { onDeleteItem(item.id) }) {
                Icon(Icons.Outlined.DeleteOutline, contentDescription = "Delete garment")
              }
            }
          }
        }
      }
      Spacer(modifier = Modifier.height(20.dp))
    }
  }
}
