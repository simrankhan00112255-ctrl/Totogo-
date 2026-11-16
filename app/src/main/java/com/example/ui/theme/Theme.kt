package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val TotoLightColorScheme = lightColorScheme(
  primary = TotoDeepEmerald,
  onPrimary = TotoPureWhite,
  primaryContainer = TotoSoftMintContainer,
  onPrimaryContainer = TotoDeepEmerald,
  secondary = TotoEmeraldPrimary,
  onSecondary = TotoPureWhite,
  secondaryContainer = TotoLightMintSurface,
  onSecondaryContainer = TotoInkDark,
  tertiary = TotoAmber,
  onTertiary = TotoPureWhite,
  tertiaryContainer = TotoAmberContainer,
  onTertiaryContainer = TotoInkDark,
  background = TotoOffWhiteBg,
  onBackground = TotoInkDark,
  surface = TotoPureWhite,
  onSurface = TotoInkDark,
  surfaceVariant = TotoLightMintSurface,
  onSurfaceVariant = TotoInkMuted,
  outline = TotoCardBorder,
  error = TotoRedError,
  onError = TotoPureWhite,
  errorContainer = TotoRedContainer,
  onErrorContainer = TotoRedError,
)

private val TotoDarkColorScheme = darkColorScheme(
  primary = TotoMintAccent,
  onPrimary = TotoInkDark,
  primaryContainer = TotoDeepEmerald,
  onPrimaryContainer = TotoPureWhite,
  secondary = TotoEmeraldPrimary,
  onSecondary = TotoPureWhite,
  secondaryContainer = TotoDarkSurfaceVariant,
  onSecondaryContainer = TotoDarkOnSurface,
  tertiary = TotoAmber,
  onTertiary = TotoInkDark,
  background = TotoDarkBg,
  onBackground = TotoDarkOnSurface,
  surface = TotoDarkSurface,
  onSurface = TotoDarkOnSurface,
  surfaceVariant = TotoDarkSurfaceVariant,
  onSurfaceVariant = TotoSoftMintContainer,
  outline = TotoDarkSurfaceVariant,
  error = TotoRedError,
  onError = TotoPureWhite,
)

val TotoShapes = Shapes(
  small = RoundedCornerShape(10.dp),
  medium = RoundedCornerShape(16.dp),
  large = RoundedCornerShape(22.dp),
  extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun TotoGoTheme(
  isBengali: Boolean = true,
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) TotoDarkColorScheme else TotoLightColorScheme
  MaterialTheme(
    colorScheme = colorScheme,
    typography = totoTypography(isBengali),
    shapes = TotoShapes,
    content = content,
  )
}
