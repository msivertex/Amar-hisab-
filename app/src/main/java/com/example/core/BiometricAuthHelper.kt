package com.example.core

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

enum class BiometricAvailability {
    AVAILABLE,
    NONE_ENROLLED,
    UNAVAILABLE
}

enum class BiometricAuthFailureReason {
    CANCELED,
    LOCKOUT,
    LOCKOUT_PERMANENT,
    NO_BIOMETRICS,
    HARDWARE_UNAVAILABLE,
    GENERIC
}

data class BiometricCapabilityInfo(
    val availability: BiometricAvailability,
    val canUseDeviceCredential: Boolean,
    val hasFingerprint: Boolean,
    val hasFace: Boolean
) {
    val canAuthenticate: Boolean
        get() = availability == BiometricAvailability.AVAILABLE || canUseDeviceCredential
}

object BiometricAuthHelper {

    fun checkBiometricAvailability(context: Context): BiometricAvailability {
        val manager = BiometricManager.from(context)
        val result = manager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.BIOMETRIC_WEAK
        )
        return when (result) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricAvailability.AVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricAvailability.NONE_ENROLLED
            else -> BiometricAvailability.UNAVAILABLE
        }
    }

    fun canUseDeviceCredentialOrBiometric(context: Context): Boolean {
        val manager = BiometricManager.from(context)
        val authenticators = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.BIOMETRIC_WEAK or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
        } else {
            BiometricManager.Authenticators.BIOMETRIC_WEAK or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
        }
        return manager.canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun inspectCapabilities(context: Context): BiometricCapabilityInfo {
        val pm = context.packageManager
        val hasFingerprint = runCatching {
            pm.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)
        }.getOrDefault(false)
        val hasFace = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            runCatching { pm.hasSystemFeature(PackageManager.FEATURE_FACE) }.getOrDefault(false)
        } else {
            false
        }
        return BiometricCapabilityInfo(
            availability = checkBiometricAvailability(context),
            canUseDeviceCredential = canUseDeviceCredentialOrBiometric(context),
            hasFingerprint = hasFingerprint,
            hasFace = hasFace
        )
    }

    fun createEnrollIntent(): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent(Settings.ACTION_BIOMETRIC_ENROLL).apply {
                putExtra(
                    Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED,
                    BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        BiometricManager.Authenticators.BIOMETRIC_WEAK
                )
            }
        } else {
            Intent(Settings.ACTION_SECURITY_SETTINGS)
        }
    }

    fun authenticate(
        context: Context,
        title: String,
        subtitle: String,
        negativeButtonText: String,
        allowDeviceCredentialFallback: Boolean = false,
        onSucceeded: () -> Unit,
        onError: (String) -> Unit,
        onFailureReason: ((BiometricAuthFailureReason, String) -> Unit)? = null
    ) {
        val activity = context.findFragmentActivity()
        if (activity == null) {
            val msg = "Biometric host activity unavailable."
            onFailureReason?.invoke(BiometricAuthFailureReason.HARDWARE_UNAVAILABLE, msg)
            onError(msg)
            return
        }

        val executor = ContextCompat.getMainExecutor(activity)
        val biometricPrompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSucceeded()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    val message = errString.toString()
                    val reason = mapErrorCode(errorCode)
                    if (reason == BiometricAuthFailureReason.CANCELED) {
                        onFailureReason?.invoke(reason, message)
                        return
                    }
                    onFailureReason?.invoke(reason, message)
                    if (message.isNotBlank()) {
                        onError(message)
                    }
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    // System prompt stays open so the user can retry or switch to PIN/credential.
                }
            }
        )

        val availability = checkBiometricAvailability(activity)
        val canUseDeviceCred = canUseDeviceCredentialOrBiometric(activity)
        val promptBuilder = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setConfirmationRequired(false)

        if (allowDeviceCredentialFallback && canUseDeviceCred) {
            // Biometric-only mode (no separate app PIN): allow Strong/Weak biometrics + device lock fallback.
            // Note: AndroidX forbids setting negativeButtonText when DEVICE_CREDENTIAL is included.
            val authenticators = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.BIOMETRIC_WEAK or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
            } else {
                BiometricManager.Authenticators.BIOMETRIC_WEAK or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
            }
            promptBuilder.setAllowedAuthenticators(authenticators)
        } else if (availability == BiometricAvailability.AVAILABLE) {
            // Biometric + custom 4-digit App PIN fallback: negative button drops straight into App PIN pad.
            val biometricAuthenticators = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.BIOMETRIC_WEAK
            } else {
                BiometricManager.Authenticators.BIOMETRIC_WEAK
            }
            promptBuilder
                .setAllowedAuthenticators(biometricAuthenticators)
                .setNegativeButtonText(negativeButtonText)
        } else if (canUseDeviceCred) {
            val authenticators = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.BIOMETRIC_WEAK or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
            } else {
                BiometricManager.Authenticators.BIOMETRIC_WEAK or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
            }
            promptBuilder.setAllowedAuthenticators(authenticators)
        } else {
            val reason = if (availability == BiometricAvailability.NONE_ENROLLED) {
                BiometricAuthFailureReason.NO_BIOMETRICS
            } else {
                BiometricAuthFailureReason.HARDWARE_UNAVAILABLE
            }
            val msg = "Biometrics or device credential not configured."
            onFailureReason?.invoke(reason, msg)
            onError(msg)
            return
        }

        try {
            biometricPrompt.authenticate(promptBuilder.build())
        } catch (e: Exception) {
            val msg = e.localizedMessage ?: "Biometric prompt failed."
            onFailureReason?.invoke(BiometricAuthFailureReason.GENERIC, msg)
            onError(msg)
        }
    }

    internal fun mapErrorCode(errorCode: Int): BiometricAuthFailureReason = when (errorCode) {
        BiometricPrompt.ERROR_NEGATIVE_BUTTON,
        BiometricPrompt.ERROR_USER_CANCELED,
        BiometricPrompt.ERROR_CANCELED -> BiometricAuthFailureReason.CANCELED
        BiometricPrompt.ERROR_LOCKOUT -> BiometricAuthFailureReason.LOCKOUT
        BiometricPrompt.ERROR_LOCKOUT_PERMANENT -> BiometricAuthFailureReason.LOCKOUT_PERMANENT
        BiometricPrompt.ERROR_NO_BIOMETRICS,
        BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL -> BiometricAuthFailureReason.NO_BIOMETRICS
        BiometricPrompt.ERROR_HW_NOT_PRESENT,
        BiometricPrompt.ERROR_HW_UNAVAILABLE -> BiometricAuthFailureReason.HARDWARE_UNAVAILABLE
        else -> BiometricAuthFailureReason.GENERIC
    }

    private tailrec fun Context.findFragmentActivity(): FragmentActivity? = when (this) {
        is FragmentActivity -> this
        is ContextWrapper -> baseContext.findFragmentActivity()
        else -> null
    }
}
