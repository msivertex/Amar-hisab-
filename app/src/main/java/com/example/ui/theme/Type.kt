package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

// Static single-weight TTFs register only FontWeight.Normal so Compose's FontSynthesis
// renders genuine crisp bold/extrabold strokes when FontWeight.Bold / ExtraBold is requested.
val PlusJakartaSansFamily = FontFamily(
    Font(R.font.plus_jakarta_sans, FontWeight.Normal)
)

val ManropeFontFamily = FontFamily(
    Font(R.font.manrope, FontWeight.Normal)
)

val NumericDisplayFontFamily = ManropeFontFamily

val HindSiliguriFamily = FontFamily(
    Font(R.font.hind_siliguri, FontWeight.Normal)
)

val OutfitFontFamily = FontFamily(
    Font(R.font.outfit, FontWeight.Normal)
)

val JetBrainsMonoFamily = FontFamily(
    Font(R.font.jetbrains_mono, FontWeight.Normal)
)

// Premium tabular-nums numeric display styles for Home balances, spend metrics, and account cards
val NumericHeroBalanceStyle = TextStyle(
    fontFamily = NumericDisplayFontFamily,
    fontWeight = FontWeight.ExtraBold,
    fontSize = 30.sp,
    lineHeight = 36.sp,
    letterSpacing = (-0.6).sp,
    fontFeatureSettings = "tnum"
)

val NumericSummaryPrimaryStyle = TextStyle(
    fontFamily = NumericDisplayFontFamily,
    fontWeight = FontWeight.ExtraBold,
    fontSize = 21.sp,
    lineHeight = 26.sp,
    letterSpacing = (-0.45).sp,
    fontFeatureSettings = "tnum"
)

val NumericSummarySecondaryStyle = TextStyle(
    fontFamily = NumericDisplayFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 15.5.sp,
    lineHeight = 20.sp,
    letterSpacing = (-0.25).sp,
    fontFeatureSettings = "tnum"
)

val NumericInlineMetricStyle = TextStyle(
    fontFamily = NumericDisplayFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 13.5.sp,
    lineHeight = 18.sp,
    letterSpacing = (-0.2).sp,
    fontFeatureSettings = "tnum"
)

val NumericCardBalanceStyle = TextStyle(
    fontFamily = NumericDisplayFontFamily,
    fontWeight = FontWeight.ExtraBold,
    fontSize = 26.5.sp,
    lineHeight = 32.sp,
    letterSpacing = (-0.5).sp,
    fontFeatureSettings = "tnum"
)

val NumericCompactCardBalanceStyle = TextStyle(
    fontFamily = NumericDisplayFontFamily,
    fontWeight = FontWeight.ExtraBold,
    fontSize = 24.5.sp,
    lineHeight = 30.sp,
    letterSpacing = (-0.45).sp,
    fontFeatureSettings = "tnum"
)

val NumericPeekingBadgeStyle = TextStyle(
    fontFamily = NumericDisplayFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 12.5.sp,
    lineHeight = 16.sp,
    letterSpacing = (-0.15).sp,
    fontFeatureSettings = "tnum"
)

fun appTypography(isBangla: Boolean): Typography {
    val displayFont = if (isBangla) HindSiliguriFamily else PlusJakartaSansFamily
    val bodyFont = if (isBangla) HindSiliguriFamily else PlusJakartaSansFamily

    return Typography(
        displayLarge = TextStyle(
            fontFamily = displayFont,
            fontWeight = FontWeight.Bold,
            fontSize = 36.sp,
            lineHeight = 44.sp,
            letterSpacing = (-0.5).sp
        ),
        headlineLarge = TextStyle(
            fontFamily = displayFont,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            lineHeight = 36.sp,
            letterSpacing = (-0.25).sp
        ),
        headlineMedium = TextStyle(
            fontFamily = displayFont,
            fontWeight = FontWeight.SemiBold,
            fontSize = 22.sp,
            lineHeight = 28.sp
        ),
        titleLarge = TextStyle(
            fontFamily = displayFont,
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
            lineHeight = 26.sp
        ),
        titleMedium = TextStyle(
            fontFamily = displayFont,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            lineHeight = 22.sp
        ),
        titleSmall = TextStyle(
            fontFamily = displayFont,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 20.sp
        ),
        bodyLarge = TextStyle(
            fontFamily = bodyFont,
            fontWeight = FontWeight.Normal,
            fontSize = 15.sp,
            lineHeight = 22.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = bodyFont,
            fontWeight = FontWeight.Normal,
            fontSize = 13.sp,
            lineHeight = 19.sp
        ),
        bodySmall = TextStyle(
            fontFamily = bodyFont,
            fontWeight = FontWeight.Normal,
            fontSize = 11.sp,
            lineHeight = 16.sp
        ),
        labelLarge = TextStyle(
            fontFamily = bodyFont,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            lineHeight = 20.sp
        ),
        labelMedium = TextStyle(
            fontFamily = bodyFont,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 16.sp
        ),
        labelSmall = TextStyle(
            fontFamily = bodyFont,
            fontWeight = FontWeight.Medium,
            fontSize = 10.sp,
            lineHeight = 14.sp
        )
    )
}
