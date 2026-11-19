package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.core.GoogleAccountProfile
import com.example.core.GoogleAuthManager
import com.example.core.GoogleSignInOutcome
import com.example.localization.AppLanguage
import com.example.localization.AppStrings
import com.example.ui.components.AmarHisabLogoBadge
import com.example.ui.components.CrystalCard
import com.example.ui.components.CrystalMeshBackground
import com.example.ui.components.GoogleGLogo
import com.example.ui.components.GooglePreviewAccountChooserDialog
import com.example.ui.theme.HindSiliguriFamily
import com.example.ui.theme.LocalCrystalColors
import com.example.ui.theme.OutfitFontFamily
import com.example.ui.theme.SkyCyanBright
import com.example.ui.theme.SkyCyanPrimary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun WelcomeScreen(
    language: AppLanguage,
    strings: AppStrings,
    onGoogleSignedIn: (GoogleAccountProfile) -> Unit,
    onContinueAsGuest: () -> Unit
) {
    val crystal = LocalCrystalColors.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val logoScale = remember { Animatable(0.80f) }
    val logoAlpha = remember { Animatable(0f) }
    val titleAlpha = remember { Animatable(0f) }
    val titleSlideY = remember { Animatable(22f) }
    val buttonsAlpha = remember { Animatable(0f) }
    val buttonsSlideY = remember { Animatable(32f) }

    var isSigningIn by remember { mutableStateOf(false) }
    var showPreviewAccountDialog by remember { mutableStateOf(false) }
    var statusNotice by remember { mutableStateOf<String?>(null) }

    val pulseTransition = rememberInfiniteTransition(label = "welcomeLogoPulse")
    val haloScale by pulseTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.07f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "welcomeHaloScale"
    )

    LaunchedEffect(Unit) {
        launch {
            logoScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
        launch {
            logoAlpha.animateTo(1f, animationSpec = tween(420, easing = FastOutSlowInEasing))
        }
        launch {
            delay(100L)
            launch { titleAlpha.animateTo(1f, animationSpec = tween(420, easing = FastOutSlowInEasing)) }
            launch { titleSlideY.animateTo(0f, animationSpec = tween(420, easing = FastOutSlowInEasing)) }
        }
        launch {
            delay(180L)
            launch { buttonsAlpha.animateTo(1f, animationSpec = tween(460, easing = FastOutSlowInEasing)) }
            launch { buttonsSlideY.animateTo(0f, animationSpec = tween(460, easing = FastOutSlowInEasing)) }
        }
    }

    fun formatWelcomeCopy(value: String): String =
        if (language == AppLanguage.EN) value.uppercase(Locale.ROOT) else value

    fun startGoogleSignIn() {
        if (isSigningIn) return
        statusNotice = null
        scope.launch {
            isSigningIn = true
            when (val outcome = GoogleAuthManager.signInWithGoogle(context)) {
                is GoogleSignInOutcome.Success -> {
                    isSigningIn = false
                    onGoogleSignedIn(outcome.profile)
                }

                is GoogleSignInOutcome.NeedsPreviewChooser -> {
                    isSigningIn = false
                    showPreviewAccountDialog = true
                }

                is GoogleSignInOutcome.Cancelled -> {
                    isSigningIn = false
                    statusNotice = formatWelcomeCopy(strings.googleSignInCancelled)
                }

                is GoogleSignInOutcome.Error -> {
                    isSigningIn = false
                    statusNotice = formatWelcomeCopy(outcome.message)
                }
            }
        }
    }

    CrystalMeshBackground {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .testTag("welcome_screen"),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 460.dp)
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Center Hero (Flexible area above bottom action buttons so buttons never push off-screen)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Box(
                                modifier = Modifier
                                    .size(144.dp)
                                    .scale(haloScale)
                                    .alpha(logoAlpha.value * 0.42f)
                                    .background(
                                        brush = Brush.radialGradient(
                                            listOf(
                                                SkyCyanBright.copy(alpha = 0.42f),
                                                SkyCyanPrimary.copy(alpha = 0.14f),
                                                Color.Transparent
                                            )
                                        ),
                                        shape = CircleShape
                                    )
                            )
                            AmarHisabLogoBadge(
                                size = 108.dp,
                                modifier = Modifier
                                    .scale(logoScale.value)
                                    .alpha(logoAlpha.value)
                            )
                        }

                        Spacer(modifier = Modifier.height(22.dp))

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.graphicsLayer {
                                alpha = titleAlpha.value
                                translationY = titleSlideY.value
                            }
                        ) {
                            Text(
                                text = strings.appTitleBangla,
                                style = MaterialTheme.typography.displayLarge.copy(
                                    fontFamily = HindSiliguriFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 34.sp
                                ),
                                color = MaterialTheme.colorScheme.onBackground,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = strings.appTitleEnglish.uppercase(Locale.ROOT),
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontFamily = OutfitFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 23.sp,
                                    letterSpacing = 3.2.sp
                                ),
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = formatWelcomeCopy(strings.splashTagline),
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontFamily = OutfitFontFamily,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 12.sp,
                                    letterSpacing = 1.6.sp
                                ),
                                color = crystal.mutedText,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bottom-Pinned Crystal Liquid Frosted-Glass Action Card
                CrystalCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp)
                        .graphicsLayer {
                            alpha = buttonsAlpha.value
                            translationY = buttonsSlideY.value
                        },
                    cornerRadius = 26.dp,
                    elevated = true
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (statusNotice != null) {
                            Surface(
                                color = crystal.warningAmberBg,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = statusNotice!!,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = OutfitFontFamily,
                                        fontWeight = FontWeight.Medium,
                                        letterSpacing = 0.8.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)
                                )
                            }
                        }

                        // Primary Button: CONTINUE WITH GOOGLE
                        ElevatedButton(
                            onClick = { startGoogleSignIn() },
                            enabled = !isSigningIn,
                            shape = RoundedCornerShape(18.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                            colors = ButtonDefaults.elevatedButtonColors(
                                containerColor = if (crystal.isDark) MaterialTheme.colorScheme.surfaceVariant else Color.White,
                                contentColor = if (crystal.isDark) Color.White else Color(0xFF0F172A)
                            ),
                            elevation = ButtonDefaults.elevatedButtonElevation(
                                defaultElevation = 6.dp,
                                pressedElevation = 2.dp
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .shadow(
                                    elevation = 12.dp,
                                    shape = RoundedCornerShape(18.dp),
                                    ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.24f),
                                    spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (crystal.isDark) Color.White.copy(alpha = 0.22f) else crystal.glassBorder,
                                    shape = RoundedCornerShape(18.dp)
                                )
                                .testTag("welcome_google_signin_button")
                        ) {
                            if (isSigningIn) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = formatWelcomeCopy(strings.signingInWithGoogle),
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontFamily = OutfitFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        letterSpacing = 0.8.sp
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            } else {
                                GoogleGLogo(size = 20.dp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = formatWelcomeCopy(strings.continueWithGoogle),
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontFamily = OutfitFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        letterSpacing = 0.8.sp
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Secondary Frosted-Glass Button: CONTINUE AS OFFLINE GUEST
                        OutlinedButton(
                            onClick = onContinueAsGuest,
                            enabled = !isSigningIn,
                            shape = RoundedCornerShape(18.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                            border = BorderStroke(1.2.dp, crystal.glassBorder),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = crystal.glassSurfaceElevated,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            crystal.glassSurfaceElevated,
                                            crystal.glassSurface
                                        )
                                    )
                                )
                                .testTag("welcome_guest_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PersonOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = formatWelcomeCopy(strings.continueAsGuest),
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontFamily = OutfitFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    letterSpacing = 0.7.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.developer_attribution),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.5.sp,
                        letterSpacing = 0.3.sp
                    ),
                    color = crystal.mutedText,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            alpha = buttonsAlpha.value
                            translationY = buttonsSlideY.value
                        }
                        .testTag("welcome_developer_attribution")
                )
            }
        }
    }

    if (showPreviewAccountDialog) {
        GooglePreviewAccountChooserDialog(
            strings = strings,
            onSelectAccount = { selectedProfile ->
                showPreviewAccountDialog = false
                scope.launch {
                    isSigningIn = true
                    delay(450L)
                    isSigningIn = false
                    onGoogleSignedIn(selectedProfile)
                }
            },
            onDismiss = { showPreviewAccountDialog = false }
        )
    }
}
