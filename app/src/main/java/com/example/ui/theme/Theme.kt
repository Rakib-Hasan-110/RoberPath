package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
  primary = EmeraldDarkPrimary,
  onPrimary = EmeraldDarkOnPrimary,
  primaryContainer = EmeraldDarkPrimaryContainer,
  onPrimaryContainer = EmeraldDarkOnPrimaryContainer,
  secondary = GoldDarkSecondary,
  onSecondary = GoldDarkOnSecondary,
  secondaryContainer = GoldDarkSecondaryContainer,
  onSecondaryContainer = GoldDarkOnSecondaryContainer,
  tertiary = TealTertiaryContainer,
  onTertiary = TealOnTertiaryContainer,
  background = ObsidianBackground,
  onBackground = ParchmentOnDark,
  surface = ObsidianSurface,
  onSurface = ParchmentOnDark,
  surfaceVariant = ObsidianSurfaceVariant,
  onSurfaceVariant = ParchmentOnDark.copy(alpha = 0.78f),
)

private val LightColorScheme = lightColorScheme(
  primary = EmeraldPrimary,
  onPrimary = EmeraldOnPrimary,
  primaryContainer = EmeraldPrimaryContainer,
  onPrimaryContainer = EmeraldOnPrimaryContainer,
  secondary = GoldSecondary,
  onSecondary = GoldOnSecondary,
  secondaryContainer = GoldSecondaryContainer,
  onSecondaryContainer = GoldOnSecondaryContainer,
  tertiary = TealTertiary,
  onTertiary = TealOnTertiary,
  tertiaryContainer = TealTertiaryContainer,
  onTertiaryContainer = TealOnTertiaryContainer,
  background = IvoryBackground,
  onBackground = InkOnBackground,
  surface = IvorySurface,
  onSurface = InkOnBackground,
  surfaceVariant = IvorySurfaceVariant,
  onSurfaceVariant = MutedOnSurfaceVariant,
  surfaceContainerLow = SandSurfaceContainer,
  outline = OutlineWarm,
)

val RoberPathShapes = Shapes(
  small = RoundedCornerShape(10.dp),
  medium = RoundedCornerShape(16.dp),
  large = RoundedCornerShape(22.dp),
)

@Composable
fun RoberPathTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    shapes = RoberPathShapes,
    content = content,
  )
}
