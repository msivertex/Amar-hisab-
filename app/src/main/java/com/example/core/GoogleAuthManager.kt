package com.example.core

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.MutableContextWrapper
import android.util.Base64
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import androidx.credentials.exceptions.GetCredentialUnsupportedException
import androidx.credentials.exceptions.NoCredentialException
import com.example.localization.AppLanguage
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import java.security.SecureRandom

private const val TAG = "GoogleAuthManager"

data class GoogleAccountProfile(
    val displayName: String,
    val email: String,
    val avatarUrl: String,
    val idToken: String = ""
)

enum class GoogleSignInErrorReason {
    OFFLINE,
    NO_GOOGLE_ACCOUNT,
    OAUTH_RESTRICTED_403,
    PLAY_SERVICES_UNAVAILABLE,
    GENERAL
}

sealed interface GoogleSignInOutcome {
    data class Success(val profile: GoogleAccountProfile) : GoogleSignInOutcome
    data object Cancelled : GoogleSignInOutcome
    data class Error(
        val reason: GoogleSignInErrorReason,
        val rawMessage: String = ""
    ) : GoogleSignInOutcome
}

/**
 * Production Google Sign-In using Android Jetpack Credential Manager (`GetSignInWithGoogleOption`).
 * Operates completely independently for any user's Gmail account without testing-mode/mock
 * fallbacks, and maps OAuth, Error 403, offline, and Play Services issues into calm localized
 * feedback while keeping local usage 100% functional.
 */
object GoogleAuthManager {

    suspend fun signInWithGoogle(context: Context): GoogleSignInOutcome {
        val webClientId = resolveWebClientId(context)
        if (webClientId.isBlank()) {
            return GoogleSignInOutcome.Error(
                reason = GoogleSignInErrorReason.OAUTH_RESTRICTED_403,
                rawMessage = "Missing default_web_client_id"
            )
        }

        val credentialManager = CredentialManager.create(context)
        val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = webClientId)
            .setNonce(generateSecureRandomNonce())
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(signInOption)
            .build()

        val resolvedContext = findActivity(context) ?: context
        val activityContext = MutableContextWrapper(resolvedContext)

        return try {
            val response = credentialManager.getCredential(
                request = request,
                context = activityContext
            )
            parseCredentialResponse(response)
        } catch (_: GetCredentialCancellationException) {
            GoogleSignInOutcome.Cancelled
        } catch (e: NoCredentialException) {
            Log.i(TAG, "No Google account credential available on device", e)
            GoogleSignInOutcome.Error(
                reason = classifyErrorReason(e.localizedMessage, fallback = GoogleSignInErrorReason.NO_GOOGLE_ACCOUNT),
                rawMessage = e.localizedMessage.orEmpty()
            )
        } catch (e: GetCredentialUnsupportedException) {
            Log.i(TAG, "Credential Manager unsupported on device", e)
            GoogleSignInOutcome.Error(
                reason = GoogleSignInErrorReason.PLAY_SERVICES_UNAVAILABLE,
                rawMessage = e.localizedMessage.orEmpty()
            )
        } catch (e: GetCredentialProviderConfigurationException) {
            Log.i(TAG, "Credential provider configuration unavailable", e)
            GoogleSignInOutcome.Error(
                reason = classifyErrorReason(
                    e.localizedMessage,
                    fallback = GoogleSignInErrorReason.PLAY_SERVICES_UNAVAILABLE
                ),
                rawMessage = e.localizedMessage.orEmpty()
            )
        } catch (e: GetCredentialException) {
            Log.w(TAG, "Credential Manager sign-in failed", e)
            GoogleSignInOutcome.Error(
                reason = classifyErrorReason(e.localizedMessage, fallback = GoogleSignInErrorReason.GENERAL),
                rawMessage = e.localizedMessage.orEmpty()
            )
        } catch (e: Exception) {
            Log.w(TAG, "Unexpected Google Sign-In exception", e)
            GoogleSignInOutcome.Error(
                reason = classifyErrorReason(e.localizedMessage, fallback = GoogleSignInErrorReason.GENERAL),
                rawMessage = e.localizedMessage.orEmpty()
            )
        }
    }

    suspend fun signOut(context: Context) {
        try {
            val credentialManager = CredentialManager.create(context)
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: Exception) {
            Log.d(TAG, "clearCredentialState skipped: ${e.localizedMessage}")
        }
    }

    fun classifyErrorReason(
        rawMessage: String?,
        fallback: GoogleSignInErrorReason = GoogleSignInErrorReason.GENERAL
    ): GoogleSignInErrorReason {
        val lower = rawMessage.orEmpty().lowercase()
        return when {
            lower.contains("403") ||
                lower.contains("access_denied") ||
                lower.contains("org_internal") ||
                lower.contains("unverified") ||
                lower.contains("developer_error") ||
                lower.contains("10:") ||
                lower.contains("28444") ||
                lower.contains("oauth") -> GoogleSignInErrorReason.OAUTH_RESTRICTED_403

            lower.contains("network") ||
                lower.contains("offline") ||
                lower.contains("timeout") ||
                lower.contains("unable to resolve") ||
                lower.contains("7:") -> GoogleSignInErrorReason.OFFLINE

            lower.contains("no credential") ||
                lower.contains("no account") -> GoogleSignInErrorReason.NO_GOOGLE_ACCOUNT

            lower.contains("play services") ||
                lower.contains("unsupported") ||
                lower.contains("provider") -> GoogleSignInErrorReason.PLAY_SERVICES_UNAVAILABLE

            else -> fallback
        }
    }

    fun friendlyErrorMessage(
        reason: GoogleSignInErrorReason,
        language: AppLanguage
    ): String {
        val isBn = language == AppLanguage.BN
        return when (reason) {
            GoogleSignInErrorReason.OFFLINE -> if (isBn) {
                "ইন্টারনেট সংযোগ নেই। আপনি এখন অফলাইনে স্বাভাবিকভাবে হিসাব রাখতে পারবেন এবং অনলাইন হলে Google সাইন-ইন করতে পারবেন।"
            } else {
                "No internet connection. You can continue tracking locally offline and sign in with Google once online."
            }

            GoogleSignInErrorReason.NO_GOOGLE_ACCOUNT -> if (isBn) {
                "এই ফোনে কোনো Gmail অ্যাকাউন্ট পাওয়া যায়নি। ফোনের Settings-এ Gmail যোগ করুন অথবা লোকাল/গেস্ট মোডে ব্যবহার চালিয়ে যান।"
            } else {
                "No Google account was found on this device. Add a Gmail account in phone Settings or continue smoothly in local mode."
            }

            GoogleSignInErrorReason.OAUTH_RESTRICTED_403 -> if (isBn) {
                "Google OAuth/যাচাইকরণ সীমাবদ্ধতার (Error 403) কারণে এই মুহূর্তে সাইন-ইন সম্পন্ন হয়নি। আপনার সব হিসাব লোকালি সম্পূর্ণ নিরাপদে ব্যবহার ও JSON/CSV ব্যাকআপ করতে পারবেন।"
            } else {
                "Google Sign-In is currently blocked by external OAuth verification settings (Error 403). All local tracking, AI features, and JSON/CSV backup continue to work normally."
            }

            GoogleSignInErrorReason.PLAY_SERVICES_UNAVAILABLE -> if (isBn) {
                "এই ডিভাইসে Google Play Services পাওয়া যায়নি। আপনি লোকাল মোডে সব হিসাব ও ব্যাকআপ সম্পূর্ণ স্বাভাবিকভাবে ব্যবহার করতে পারবেন।"
            } else {
                "Google Play Services is unavailable on this device. You can continue using all local features and JSON/CSV backup normally."
            }

            GoogleSignInErrorReason.GENERAL -> if (isBn) {
                "Google সাইন-ইন সম্পন্ন করা যায়নি। কিছুক্ষণ পর আবার চেষ্টা করুন অথবা লোকাল মোডে ব্যবহার চালিয়ে যান।"
            } else {
                "Could not complete Google Sign-In right now. Please try again later or continue in local mode."
            }
        }
    }

    private fun parseCredentialResponse(response: GetCredentialResponse): GoogleSignInOutcome {
        val credential = response.credential
        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            return try {
                val googleId = GoogleIdTokenCredential.createFrom(credential.data)
                GoogleSignInOutcome.Success(
                    GoogleAccountProfile(
                        displayName = googleId.displayName?.takeIf { it.isNotBlank() }
                            ?: googleId.id.substringBefore("@"),
                        email = googleId.id.trim(),
                        avatarUrl = googleId.profilePictureUri?.toString().orEmpty(),
                        idToken = googleId.idToken
                    )
                )
            } catch (e: GoogleIdTokenParsingException) {
                Log.e(TAG, "Failed to parse GoogleIdTokenCredential", e)
                GoogleSignInOutcome.Error(
                    reason = GoogleSignInErrorReason.GENERAL,
                    rawMessage = e.localizedMessage ?: "Invalid Google ID token"
                )
            }
        }
        return GoogleSignInOutcome.Error(
            reason = GoogleSignInErrorReason.GENERAL,
            rawMessage = "Unexpected credential type returned."
        )
    }

    private fun findActivity(context: Context): Activity? {
        var ctx: Context? = context
        while (ctx is ContextWrapper) {
            if (ctx is Activity) return ctx
            ctx = ctx.baseContext
        }
        return null
    }

    private fun resolveWebClientId(context: Context): String {
        val resId = context.resources.getIdentifier(
            "default_web_client_id",
            "string",
            context.packageName
        )
        return if (resId != 0) context.getString(resId) else ""
    }

    private fun generateSecureRandomNonce(byteLength: Int = 32): String {
        val randomBytes = ByteArray(byteLength)
        SecureRandom().nextBytes(randomBytes)
        return Base64.encodeToString(
            randomBytes,
            Base64.NO_WRAP or Base64.URL_SAFE or Base64.NO_PADDING
        )
    }
}
