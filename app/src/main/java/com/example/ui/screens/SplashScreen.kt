package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.localization.AppStrings
import com.example.ui.components.AmarHisabLogoBadge
import com.example.ui.components.CrystalMeshBackground
import com.example.ui.theme.HindSiliguriFamily
import com.example.ui.theme.LocalCrystalColors
import com.example.ui.theme.OutfitFontFamily
import com.example.ui.theme.SkyCyanBright
import com.example.ui.theme.SkyCyanPrimary
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun SplashScreen(
    strings: AppStrings,
    onSplashFinished: () -> Unit
) {
    val crystal = LocalCrystalColors.current
    val logoScale = remember { Animatable(0.74f) }
    val logoAlpha = remember { Animatable(0f) }
    var showText by remember { mutableStateOf(false) }

    val pulseTransition = rememberInfiniteTransition(label = "splashLogoPulse")
    val haloScale by pulseTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.07f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "haloScale"
    )

    LaunchedEffect(Unit) {
        logoScale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    }
    LaunchedEffect(Unit) {
        logoAlpha.animateTo(1f, animationSpec = tween(480, easing = FastOutSlowInEasing))
        showText = true
        delay(1520L)
        onSplashFinished()
    }

    CrystalMeshBackground {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .clickable(onClick = onSplashFinished)
                .padding(24.dp)
                .testTag("splash_screen"),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size(164.dp)
                            .scale(haloScale)
                            .alpha(logoAlpha.value * 0.45f)
                            .background(
                                brush = Brush.radialGradient(
                                    listOf(
                                        SkyCyanBright.copy(alpha = 0.40f),
                                        SkyCyanPrimary.copy(alpha = 0.14f),
                                        Color.Transparent
                                    )
                                ),
                                shape = CircleShape
                            )
                    )
                    AmarHisabLogoBadge(
                        size = 124.dp,
                        modifier = Modifier
                            .scale(logoScale.value)
                            .alpha(logoAlpha.value)
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                AnimatedVisibility(
                    visible = showText,
                    enter = fadeIn(tween(460)) + slideInVertically(tween(460, easing = FastOutSlowInEasing)) { it / 3 }
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = strings.appTitleBangla,
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontFamily = HindSiliguriFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 36.sp
                            ),
                            color = MaterialTheme.colorScheme.onBackground,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = strings.appTitleEnglish.uppercase(Locale.ROOT),
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontFamily = OutfitFontFamily,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 3.2.sp
                            ),
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = strings.splashTagline.uppercase(Locale.ROOT),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontFamily = OutfitFontFamily,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 1.6.sp
                            ),
                            color = crystal.mutedText,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
