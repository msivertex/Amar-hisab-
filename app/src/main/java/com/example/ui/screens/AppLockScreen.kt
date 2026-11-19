package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.core.BiometricAuthFailureReason
import com.example.core.BiometricAuthHelper
import com.example.core.BiometricAvailability
import com.example.core.BiometricCapabilityInfo
import com.example.localization.AppStrings
import com.example.ui.components.CrystalCard
import com.example.ui.components.CrystalMeshBackground
import com.example.ui.theme.HindSiliguriFamily
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalCrystalColors
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

data class BiometricPromptRequest(
    val title: String,
    val subtitle: String,
    val negativeButtonText: String,
    val allowDeviceCredentialFallback: Boolean,
    val onSucceeded: () -> Unit,
    val onFailure: (BiometricAuthFailureReason, String) -> Unit
)

@Composable
fun AppLockScreen(
    expectedPin: String,
    biometricEnabled: Boolean,
    hapticEnabled: Boolean,
    strings: AppStrings,
    onUnlocked: () -> Unit,
    capabilityOverride: BiometricCapabilityInfo? = null,
    biometricAuthenticatorOverride: ((BiometricPromptRequest) -> Unit)? = null
) {
    val crystal = LocalCrystalColors.current
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val bioStrings = strings.biometricSecurity

    val capability = remember(context, capabilityOverride) {
        capabilityOverride ?: BiometricAuthHelper.inspectCapabilities(context)
    }
    val hasValidPin = expectedPin.length == 4
    val canRunBiometrics = biometricEnabled && capability.canAuthenticate

    var showPinPad by remember(hasValidPin, canRunBiometrics) {
        mutableStateOf(hasValidPin && !canRunBiometrics)
    }
    var enteredPin by remember { mutableStateOf("") }
    var showPinError by remember { mutableStateOf(false) }
    var biometricNotice by remember { mutableStateOf<String?>(null) }

    val shakeOffset = remember { Animatable(0f) }

    fun localizedBiometricMessage(reason: BiometricAuthFailureReason, raw: String): String = when (reason) {
        BiometricAuthFailureReason.LOCKOUT -> bioStrings.lockoutErrorMsg
        BiometricAuthFailureReason.LOCKOUT_PERMANENT -> bioStrings.permanentLockoutErrorMsg
        BiometricAuthFailureReason.NO_BIOMETRICS -> bioStrings.notEnrolledErrorMsg
        BiometricAuthFailureReason.HARDWARE_UNAVAILABLE -> bioStrings.hardwareUnavailableErrorMsg
        BiometricAuthFailureReason.CANCELED -> ""
        BiometricAuthFailureReason.GENERIC -> raw.ifBlank { bioStrings.hardwareUnavailableErrorMsg }
    }

    fun triggerBiometricPrompt() {
        biometricNotice = null
        showPinError = false
        val allowDeviceCredential = !hasValidPin
        val request = BiometricPromptRequest(
            title = strings.unlockVaultTitle,
            subtitle = if (hasValidPin) strings.enterPinToUnlock else bioStrings.biometricOnlySubtitle,
            negativeButtonText = strings.usePinFallback,
            allowDeviceCredentialFallback = allowDeviceCredential,
            onSucceeded = {
                if (hapticEnabled) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
                onUnlocked()
            },
            onFailure = { reason, raw ->
                if (reason == BiometricAuthFailureReason.CANCELED) {
                    if (hasValidPin) {
                        showPinPad = true
                    }
                } else {
                    biometricNotice = localizedBiometricMessage(reason, raw)
                    if (hasValidPin && (reason == BiometricAuthFailureReason.LOCKOUT || reason == BiometricAuthFailureReason.LOCKOUT_PERMANENT)) {
                        showPinPad = true
                    }
                }
            }
        )

        if (biometricAuthenticatorOverride != null) {
            biometricAuthenticatorOverride(request)
        } else {
            BiometricAuthHelper.authenticate(
                context = context,
                title = request.title,
                subtitle = request.subtitle,
                negativeButtonText = request.negativeButtonText,
                allowDeviceCredentialFallback = request.allowDeviceCredentialFallback,
                onSucceeded = request.onSucceeded,
                onError = { /* handled via onFailureReason */ },
                onFailureReason = request.onFailure
            )
        }
    }

    // Automatically launch Android BiometricPrompt when vault lock appears
    LaunchedEffect(canRunBiometrics) {
        if (canRunBiometrics) {
            triggerBiometricPrompt()
        }
    }

    fun onDigit(digit: String) {
        if (!hasValidPin) return
        showPinError = false
        biometricNotice = null
        if (hapticEnabled) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
        if (enteredPin.length < 4) {
            val next = enteredPin + digit
            enteredPin = next
            if (next.length == 4) {
                if (next == expectedPin) {
                    onUnlocked()
                } else {
                    if (hapticEnabled) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                    showPinError = true
                    enteredPin = ""
                    scope.launch {
                        for (x in listOf(-18f, 18f, -12f, 12f, -6f, 0f)) {
                            shakeOffset.animateTo(x, animationSpec = tween(45))
                        }
                    }
                }
            }
        }
    }

    val hardwareBadgeText = when {
        capability.availability == BiometricAvailability.AVAILABLE && capability.hasFingerprint && capability.hasFace ->
            bioStrings.fingerprintAndFaceReady
        capability.availability == BiometricAvailability.AVAILABLE && capability.hasFace ->
            bioStrings.faceReady
        capability.availability == BiometricAvailability.AVAILABLE ->
            bioStrings.fingerprintReady
        capability.canUseDeviceCredential ->
            bioStrings.deviceLockFallbackReady
        capability.availability == BiometricAvailability.NONE_ENROLLED ->
            bioStrings.notEnrolledErrorMsg
        else ->
            bioStrings.hardwareUnavailableErrorMsg
    }

    val isPinModeActive = showPinPad && hasValidPin
    val scrollState = rememberScrollState()

    CrystalMeshBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CrystalCard(
                modifier = Modifier
                    .widthIn(max = 420.dp)
                    .testTag("app_lock_card"),
                elevated = true
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(if (isPinModeActive) 10.dp else 14.dp)
                ) {
                    // Pulsing Biometric / Vault Emblem
                    val pulseTransition = rememberInfiniteTransition(label = "vaultPulse")
                    val pulseScale by pulseTransition.animateFloat(
                        initialValue = 1f,
                        targetValue = 1.07f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1400, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "pulseScale"
                    )

                    Box(
                        modifier = Modifier
                            .size(if (isPinModeActive) 56.dp else 76.dp)
                            .scale(if (!isPinModeActive && canRunBiometrics) pulseScale else 1f)
                            .clip(CircleShape)
                            .background(
                                MaterialTheme.colorScheme.primary.copy(
                                    alpha = if (crystal.isDark) 0.22f else 0.14f
                                )
                            )
                            .border(
                                width = 1.2.dp,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (!isPinModeActive && canRunBiometrics) {
                                Icons.Filled.Fingerprint
                            } else {
                                Icons.Filled.Lock
                            },
                            contentDescription = strings.unlockVaultTitle,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(if (isPinModeActive) 28.dp else 36.dp)
                        )
                    }

                    Text(
                        text = strings.unlockVaultTitle,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = HindSiliguriFamily
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = if (showPinPad) strings.enterPinToUnlock else bioStrings.biometricOnlySubtitle,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = HindSiliguriFamily
                        ),
                        color = crystal.mutedText,
                        textAlign = TextAlign.Center
                    )

                    // Hardware Readiness Pill
                    if (biometricEnabled) {
                        Surface(
                            shape = CircleShape,
                            color = if (capability.canAuthenticate) {
                                crystal.assetGreen.copy(alpha = if (crystal.isDark) 0.18f else 0.11f)
                            } else {
                                crystal.warningAmberBg
                            },
                            border = BorderStroke(
                                0.8.dp,
                                if (capability.canAuthenticate) {
                                    crystal.assetGreen.copy(alpha = 0.36f)
                                } else {
                                    crystal.warningAmber.copy(alpha = 0.4f)
                                }
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.VerifiedUser,
                                    contentDescription = null,
                                    tint = if (capability.canAuthenticate) crystal.assetGreen else crystal.warningAmber,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = hardwareBadgeText,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontFamily = HindSiliguriFamily
                                    ),
                                    color = if (capability.canAuthenticate) crystal.assetGreen else crystal.warningAmber
                                )
                            }
                        }
                    }

                    if (!biometricNotice.isNullOrBlank()) {
                        Surface(
                            color = crystal.warningAmberBg,
                            border = BorderStroke(0.8.dp, crystal.warningAmber.copy(alpha = 0.35f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("biometric_error_notice")
                        ) {
                            Text(
                                text = biometricNotice!!,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = HindSiliguriFamily
                                ),
                                color = crystal.warningAmber,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }

                    AnimatedContent(
                        targetState = showPinPad && hasValidPin,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "lockModeTransition"
                    ) { isPinMode ->
                        if (isPinMode) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // 4 PIN Dots with subtle error shake
                                Row(
                                    modifier = Modifier.offset { IntOffset(shakeOffset.value.roundToInt(), 0) },
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    repeat(4) { idx ->
                                        val filled = idx < enteredPin.length
                                        Box(
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    when {
                                                        showPinError -> crystal.liabilityRed
                                                        filled -> MaterialTheme.colorScheme.primary
                                                        else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                                                    }
                                                )
                                                .border(1.dp, crystal.glassBorder, CircleShape)
                                        )
                                    }
                                }

                                if (showPinError) {
                                    Text(
                                        text = strings.incorrectPinError,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontFamily = HindSiliguriFamily
                                        ),
                                        color = crystal.liabilityRed,
                                        modifier = Modifier.testTag("pin_error_text")
                                    )
                                }

                                val padRows = listOf(
                                    listOf("1", "2", "3"),
                                    listOf("4", "5", "6"),
                                    listOf("7", "8", "9"),
                                    listOf("", "0", "⌫")
                                )
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    padRows.forEach { row ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            row.forEach { key ->
                                                if (key.isEmpty()) {
                                                    Spacer(modifier = Modifier.weight(1f))
                                                } else {
                                                    Box(
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .height(52.dp)
                                                            .clip(RoundedCornerShape(14.dp))
                                                            .background(
                                                                if (crystal.isDark) MaterialTheme.colorScheme.surfaceVariant
                                                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                                            )
                                                            .border(1.dp, crystal.glassBorder, RoundedCornerShape(14.dp))
                                                            .clickable {
                                                                if (key == "⌫") {
                                                                    if (hapticEnabled) {
                                                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                                    }
                                                                    if (enteredPin.isNotEmpty()) {
                                                                        enteredPin = enteredPin.dropLast(1)
                                                                    }
                                                                } else {
                                                                    onDigit(key)
                                                                }
                                                            }
                                                            .testTag("lock_key_$key"),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        if (key == "⌫") {
                                                            Icon(
                                                                imageVector = Icons.AutoMirrored.Filled.Backspace,
                                                                contentDescription = "Delete",
                                                                tint = MaterialTheme.colorScheme.onSurface
                                                            )
                                                        } else {
                                                            Text(
                                                                text = key,
                                                                style = MaterialTheme.typography.titleLarge.copy(
                                                                    fontFamily = JetBrainsMonoFamily,
                                                                    fontWeight = FontWeight.Bold
                                                                ),
                                                                color = MaterialTheme.colorScheme.onSurface
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                if (canRunBiometrics) {
                                    OutlinedButton(
                                        onClick = {
                                            showPinPad = false
                                            triggerBiometricPrompt()
                                        },
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 48.dp)
                                            .testTag("biometric_unlock_button")
                                    ) {
                                        Icon(Icons.Filled.Fingerprint, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = bioStrings.switchToBiometricBtn,
                                            style = MaterialTheme.typography.labelLarge.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = HindSiliguriFamily
                                            )
                                        )
                                    }
                                }
                            }
                        } else {
                            // Biometric-First Hero Action Panel
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("biometric_first_panel"),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Spacer(modifier = Modifier.height(4.dp))

                                if (canRunBiometrics) {
                                    Button(
                                        onClick = ::triggerBiometricPrompt,
                                        shape = RoundedCornerShape(16.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = Color.White
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 52.dp)
                                            .testTag("biometric_unlock_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Fingerprint,
                                            contentDescription = null,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = bioStrings.tapToUnlockBiometrics,
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = HindSiliguriFamily
                                            )
                                        )
                                    }
                                } else if (capability.availability == BiometricAvailability.NONE_ENROLLED) {
                                    OutlinedButton(
                                        onClick = {
                                            runCatching {
                                                context.startActivity(BiometricAuthHelper.createEnrollIntent())
                                            }
                                        },
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 48.dp)
                                            .testTag("lock_enroll_biometric_button")
                                    ) {
                                        Icon(Icons.Filled.Fingerprint, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = bioStrings.enrollBiometricsBtn,
                                            style = MaterialTheme.typography.labelLarge.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = HindSiliguriFamily
                                            )
                                        )
                                    }
                                }

                                if (hasValidPin) {
                                    TextButton(
                                        onClick = {
                                            biometricNotice = null
                                            showPinPad = true
                                        },
                                        modifier = Modifier
                                            .heightIn(min = 48.dp)
                                            .testTag("switch_to_pin_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Dialpad,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = bioStrings.switchToPinBtn,
                                            style = MaterialTheme.typography.labelLarge.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                fontFamily = HindSiliguriFamily
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
