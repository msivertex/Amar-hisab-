package com.example.data.backup

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.tasks.await

sealed interface DriveAuthOutcome {
    data class Authorized(val accessToken: String) : DriveAuthOutcome
    data class NeedsUserConsent(val pendingIntent: PendingIntent) : DriveAuthOutcome
    data class Error(val message: String) : DriveAuthOutcome
}

/**
 * Acquires a least-privilege Google Drive OAuth access token (`drive.file` scope only)
 * via Google Play Services Identity AuthorizationClient so Amar Hisab can only create and
 * access its own dedicated encrypted backup folder and vault file.
 */
object GoogleDriveAuthHelper {
    const val DRIVE_FILE_SCOPE = "https://www.googleapis.com/auth/drive.file"

    suspend fun requestDriveAccessToken(context: Context): DriveAuthOutcome {
        return try {
            val request = AuthorizationRequest.builder()
                .setRequestedScopes(listOf(Scope(DRIVE_FILE_SCOPE)))
                .build()
            val result = Identity.getAuthorizationClient(context.applicationContext)
                .authorize(request)
                .await()

            when {
                result.hasResolution() && result.pendingIntent != null -> {
                    DriveAuthOutcome.NeedsUserConsent(result.pendingIntent!!)
                }

                !result.accessToken.isNullOrBlank() -> {
                    DriveAuthOutcome.Authorized(result.accessToken!!)
                }

                else -> {
                    DriveAuthOutcome.Error("Google Drive permission was not granted.")
                }
            }
        } catch (e: Exception) {
            DriveAuthOutcome.Error(e.localizedMessage ?: "Unable to connect to Google Drive.")
        }
    }

    fun extractAccessTokenFromResult(context: Context, data: Intent?): String? {
        if (data == null) return null
        return runCatching {
            val authResult = Identity.getAuthorizationClient(context.applicationContext)
                .getAuthorizationResultFromIntent(data)
            authResult.accessToken?.takeIf { it.isNotBlank() }
        }.getOrNull()
    }
}
