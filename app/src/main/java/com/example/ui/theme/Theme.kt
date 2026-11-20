package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val LightColorScheme =
  lightColorScheme(
    primary = ForestPrimary,
    onPrimary = OnForestPrimary,
    primaryContainer = ForestContainer,
    onPrimaryContainer = OnForestContainer,
    secondary = MossSecondary,
    onSecondary = OnMossSecondary,
    secondaryContainer = MossContainer,
    onSecondaryContainer = OnMossContainer,
    tertiary = GoldAccent,
    onTertiary = OnGoldAccent,
    tertiaryContainer = GoldContainer,
    onTertiaryContainer = OnGoldContainer,
    background = BackgroundLight,
    onBackground = TextPrimaryLight,
    surface = SurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = TextSecondaryLight,
    outline = OutlineLight,
    error = ErrorRed,
  )

private val DarkColorScheme =
  darkColorScheme(
    primary = ForestPrimaryDark,
    onPrimary = OnForestPrimaryDark,
    primaryContainer = ForestContainerDark,
    onPrimaryContainer = OnForestContainerDark,
    secondary = MossSecondaryDark,
    onSecondary = OnMossSecondaryDark,
    secondaryContainer = MossContainerDark,
    onSecondaryContainer = OnMossContainerDark,
    tertiary = GoldAccentDark,
    onTertiary = OnGoldAccentDark,
    tertiaryContainer = GoldContainerDark,
    onTertiaryContainer = OnGoldContainerDark,
    background = BackgroundDark,
    onBackground = TextPrimaryDark,
    surface = SurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = TextSecondaryDark,
    outline = OutlineDark,
    error = ErrorRedDark,
  )

val VitalTrackShapes =
  Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
  )

enum class ThemePreference {
  SYSTEM,
  LIGHT,
  DARK,
}

enum class AccentPalette(val label: String) {
  FOREST_CALM("Forest Calm"),
  SOLAR_VITALITY("Solar Vitality"),
  OCEAN_MIST("Ocean Mist"),
  AMOLED_DARK("AMOLED Dark"),
  DYNAMIC_WALLPAPER("Dynamic Material You"),
}

@Composable
fun VitalTrackTheme(
  themePreference: ThemePreference = ThemePreference.SYSTEM,
  accentPalette: AccentPalette = AccentPalette.FOREST_CALM,
  content: @Composable () -> Unit,
) {
  val context = LocalContext.current
  val darkTheme =
    when {
      accentPalette == AccentPalette.AMOLED_DARK -> true
      themePreference == ThemePreference.SYSTEM -> isSystemInDarkTheme()
      themePreference == ThemePreference.LIGHT -> false
      else -> true
    }

  val baseScheme =
    when (accentPalette) {
      AccentPalette.DYNAMIC_WALLPAPER ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
          if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        } else {
          if (darkTheme) DarkColorScheme else LightColorScheme
        }
      AccentPalette.SOLAR_VITALITY ->
        if (darkTheme) {
          DarkColorScheme.copy(
            primary = Color(0xFFFFB56B),
            primaryContainer = Color(0xFF5A3410),
            onPrimaryContainer = Color(0xFFFFDDB8),
            secondary = Color(0xFFE9967A),
          )
        } else {
          LightColorScheme.copy(
            primary = Color(0xFFB55D1E),
            primaryContainer = Color(0xFFFFE5CC),
            onPrimaryContainer = Color(0xFF3E1C00),
            secondary = Color(0xFFD67C49),
          )
        }
      AccentPalette.OCEAN_MIST ->
        if (darkTheme) {
          DarkColorScheme.copy(
            primary = Color(0xFF7FD0E4),
            primaryContainer = Color(0xFF134857),
            onPrimaryContainer = Color(0xFFC9F1FA),
            secondary = Color(0xFF8AA8D6),
          )
        } else {
          LightColorScheme.copy(
            primary = Color(0xFF1F6A7E),
            primaryContainer = Color(0xFFD0F0F7),
            onPrimaryContainer = Color(0xFF062933),
            secondary = Color(0xFF4A7298),
          )
        }
      AccentPalette.AMOLED_DARK ->
        DarkColorScheme.copy(
          background = Color(0xFF000000),
          surface = Color(0xFF0C100E),
          surfaceVariant = Color(0xFF171E1A),
        )
      AccentPalette.FOREST_CALM -> if (darkTheme) DarkColorScheme else LightColorScheme
    }

  MaterialTheme(
    colorScheme = baseScheme,
    typography = VitalTrackTypography,
    shapes = VitalTrackShapes,
    content = content,
  )
}
