package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.example.data.model.AppColourTheme

@Immutable
data class CrystalColors(
    val isDark: Boolean,
    val canvasTop: Color,
    val canvasMid: Color,
    val canvasBottom: Color,
    val glassSurface: Color,
    val glassSurfaceElevated: Color,
    val glassBorder: Color,
    val glassHighlight: Color,
    val orbPrimary: Color,
    val orbSecondary: Color,
    val assetGreen: Color,
    val assetGreenBg: Color,
    val liabilityRed: Color,
    val liabilityRedBg: Color,
    val warningAmber: Color,
    val warningAmberBg: Color,
    val navBarBackground: Color,
    val mutedText: Color,
    val numericInk: Color
)

private fun buildLightCrystalColors(colourTheme: AppColourTheme): CrystalColors {
    val tint = colourTheme.lightCanvasTint
    return CrystalColors(
        isDark = false,
        canvasTop = if (colourTheme == AppColourTheme.SKY_CYAN) LightCanvasTop else tint,
        canvasMid = if (colourTheme == AppColourTheme.SKY_CYAN) LightCanvasMid else lerp(tint, Color.White, 0.45f),
        canvasBottom = if (colourTheme == AppColourTheme.SKY_CYAN) LightCanvasBottom else lerp(tint, Color.White, 0.78f),
        glassSurface = Color.White.copy(alpha = 0.72f),
        glassSurfaceElevated = Color.White.copy(alpha = 0.88f),
        glassBorder = Color.White.copy(alpha = 0.92f),
        glassHighlight = colourTheme.lightSecondary.copy(alpha = 0.25f),
        orbPrimary = colourTheme.lightPrimary.copy(alpha = 0.16f),
        orbSecondary = colourTheme.lightSecondary.copy(alpha = 0.20f),
        assetGreen = AssetEmerald,
        assetGreenBg = AssetEmeraldSoft,
        liabilityRed = LiabilityCoral,
        liabilityRedBg = LiabilityCoralSoft,
        warningAmber = WarningAmber,
        warningAmberBg = WarningAmberSoft,
        navBarBackground = Color.White.copy(alpha = 0.86f),
        mutedText = SlateOcean,
        numericInk = CharcoalSlateInk
    )
}

private val DarkCrystalColors = CrystalColors(
    isDark = true,
    canvasTop = DarkCanvasTop,
    canvasMid = DarkCanvasMid,
    canvasBottom = DarkCanvasBottom,
    glassSurface = DarkSurfaceGlass,
    glassSurfaceElevated = DarkSurfaceElevated,
    glassBorder = Color.White.copy(alpha = 0.14f),
    glassHighlight = Color.White.copy(alpha = 0.06f),
    orbPrimary = Color.Transparent,
    orbSecondary = Color.Transparent,
    assetGreen = Color(0xFF34D399),
    assetGreenBg = Color(0xFF064E3B),
    liabilityRed = Color(0xFFFB7185),
    liabilityRedBg = Color(0xFF7F1D1D),
    warningAmber = Color(0xFFFBBF24),
    warningAmberBg = Color(0xFF78350F),
    navBarBackground = Color(0xFF0E131E),
    mutedText = DarkTextOffWhite,
    numericInk = CrispDarkNumericInk
)

private val DefaultLightCrystalColors = buildLightCrystalColors(AppColourTheme.SKY_CYAN)

val LocalCrystalColors = staticCompositionLocalOf { DefaultLightCrystalColors }

private fun buildLightColorScheme(colourTheme: AppColourTheme) = lightColorScheme(
    primary = colourTheme.lightPrimary,
    onPrimary = Color.White,
    primaryContainer = lerp(colourTheme.lightPrimary, Color.White, 0.76f),
    onPrimaryContainer = DeepOcean,
    secondary = colourTheme.lightSecondary,
    onSecondary = if (colourTheme == AppColourTheme.SKY_CYAN) DeepOcean else Color.White,
    secondaryContainer = lerp(colourTheme.lightSecondary, Color.White, 0.80f),
    onSecondaryContainer = DeepOcean,
    tertiary = IceMint,
    onTertiary = DeepOcean,
    background = if (colourTheme == AppColourTheme.SKY_CYAN) LightCanvasMid else lerp(colourTheme.lightCanvasTint, Color.White, 0.45f),
    onBackground = DeepOcean,
    surface = Color.White,
    onSurface = DeepOcean,
    surfaceVariant = if (colourTheme == AppColourTheme.SKY_CYAN) Color(0xFFE6F4FA) else lerp(colourTheme.lightCanvasTint, Color.White, 0.30f),
    onSurfaceVariant = SlateOcean,
    outline = lerp(colourTheme.lightPrimary, Color.White, 0.65f),
    error = LiabilityCoral,
    onError = Color.White
)

private fun buildDarkColorScheme(colourTheme: AppColourTheme) = darkColorScheme(
    primary = colourTheme.darkPrimary,
    onPrimary = DarkTextPrimary,
    primaryContainer = lerp(colourTheme.darkPrimary, Color(0xFF0B0F19), 0.72f),
    onPrimaryContainer = DarkTextPrimary,
    secondary = colourTheme.darkSecondary,
    onSecondary = Color(0xFF0B0F19),
    secondaryContainer = lerp(colourTheme.darkSecondary, Color(0xFF0B0F19), 0.72f),
    onSecondaryContainer = DarkTextPrimary,
    tertiary = IceMint,
    onTertiary = Color(0xFF0B0F19),
    background = DarkCanvasMid,
    onBackground = DarkTextPrimary,
    surface = DarkSurfaceGlass,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = DarkTextOffWhite,
    surfaceDim = DarkCanvasTop,
    surfaceBright = Color(0xFF1B2233),
    surfaceContainerLowest = DarkCanvasTop,
    surfaceContainerLow = Color(0xFF0E131E),
    surfaceContainer = DarkSurfaceGlass,
    surfaceContainerHigh = DarkSurfaceElevated,
    surfaceContainerHighest = Color(0xFF1D2434),
    outline = Color(0xFF475569),
    outlineVariant = Color(0xFF232C3F),
    inverseSurface = DarkTextOffWhite,
    inverseOnSurface = Color(0xFF0B0F19),
    error = Color(0xFFFB7185),
    onError = DarkTextPrimary
)

private val CrystalShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun AmarHisabTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    colourTheme: AppColourTheme = AppColourTheme.SKY_CYAN,
    isBangla: Boolean = false,
    isRtl: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) buildDarkColorScheme(colourTheme) else buildLightColorScheme(colourTheme)
    val crystalColors = if (darkTheme) DarkCrystalColors else buildLightCrystalColors(colourTheme)
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !darkTheme
                controller.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(
        LocalCrystalColors provides crystalColors,
        LocalContentColor provides colorScheme.onBackground,
        LocalLayoutDirection provides if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = appTypography(isBangla),
            shapes = CrystalShapes,
            content = content
        )
    }
}
