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
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import java.security.SecureRandom

private const val TAG = "GoogleAuthManager"

data class GoogleAccountProfile(
    val displayName: String,
    val email: String,
    val avatarUrl: String,
    val idToken: String = "",
    val isPreviewSession: Boolean = false
)

sealed interface GoogleSignInOutcome {
    data class Success(val profile: GoogleAccountProfile) : GoogleSignInOutcome
    data class NeedsPreviewChooser(val reason: String) : GoogleSignInOutcome
    data object Cancelled : GoogleSignInOutcome
    data class Error(val message: String) : GoogleSignInOutcome
}

/**
 * Structures Sign in with Google using Android Jetpack Credential Manager (`GetSignInWithGoogleOption`).
 * When running inside a preview or emulator environment without Play Services / device Google
 * accounts configured, gracefully signals [GoogleSignInOutcome.NeedsPreviewChooser] so the UI
 * can present a smooth preview Google account selection flow while keeping production OAuth
 * plumbing intact.
 */
object GoogleAuthManager {

    val previewAccounts: List<GoogleAccountProfile> = listOf(
        GoogleAccountProfile(
            displayName = "Tahmid Hasan",
            email = "tahmid.hasan@gmail.com",
            avatarUrl = "",
            isPreviewSession = true
        ),
        GoogleAccountProfile(
            displayName = "Nusrat Jahan",
            email = "nusrat.jahan@gmail.com",
            avatarUrl = "",
            isPreviewSession = true
        )
    )

    suspend fun signInWithGoogle(context: Context): GoogleSignInOutcome {
        val webClientId = resolveWebClientId(context)
        if (webClientId.isBlank()) {
            return GoogleSignInOutcome.NeedsPreviewChooser("Missing default_web_client_id")
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
            Log.i(TAG, "No Google credential available on device; switching to preview chooser", e)
            GoogleSignInOutcome.NeedsPreviewChooser(e.localizedMessage ?: "No Google account on device")
        } catch (e: GetCredentialUnsupportedException) {
            Log.i(TAG, "Credential Manager unsupported on preview device", e)
            GoogleSignInOutcome.NeedsPreviewChooser(e.localizedMessage ?: "Credential Manager unavailable")
        } catch (e: GetCredentialProviderConfigurationException) {
            Log.i(TAG, "Credential provider unconfigured on preview device", e)
            GoogleSignInOutcome.NeedsPreviewChooser(e.localizedMessage ?: "Provider unconfigured")
        } catch (e: GetCredentialException) {
            Log.w(TAG, "Credential Manager failed; falling back to preview chooser", e)
            GoogleSignInOutcome.NeedsPreviewChooser(e.localizedMessage ?: "Google Sign-In unavailable in preview")
        } catch (e: Exception) {
            Log.w(TAG, "Unexpected Credential Manager exception", e)
            GoogleSignInOutcome.NeedsPreviewChooser(e.localizedMessage ?: "Google Sign-In unavailable")
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
                        email = googleId.id,
                        avatarUrl = googleId.profilePictureUri?.toString().orEmpty(),
                        idToken = googleId.idToken,
                        isPreviewSession = false
                    )
                )
            } catch (e: GoogleIdTokenParsingException) {
                Log.e(TAG, "Failed to parse GoogleIdTokenCredential", e)
                GoogleSignInOutcome.Error(e.localizedMessage ?: "Invalid Google ID token")
            }
        }
        return GoogleSignInOutcome.Error("Unexpected credential type returned.")
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
