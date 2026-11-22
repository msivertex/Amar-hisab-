package com.example.data.backup

import android.accounts.Account
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.tasks.await

enum class DriveAuthErrorKind {
    OAUTH_RESTRICTED_403,
    OFFLINE,
    DECLINED,
    PLAY_SERVICES_UNAVAILABLE,
    GENERAL
}

sealed interface DriveAuthOutcome {
    data class Authorized(val accessToken: String) : DriveAuthOutcome
    data class NeedsUserConsent(val pendingIntent: PendingIntent) : DriveAuthOutcome
    data class Error(
        val message: String,
        val kind: DriveAuthErrorKind = DriveAuthErrorKind.GENERAL
    ) : DriveAuthOutcome
}

sealed interface DriveConsentExtraction {
    data class Token(val accessToken: String) : DriveConsentExtraction
    data object Declined : DriveConsentExtraction
    data class Restricted403(val rawMessage: String) : DriveConsentExtraction
    data class Failed(val rawMessage: String) : DriveConsentExtraction
}

/**
 * Acquires a least-privilege Google Drive OAuth access token (`drive.file` scope only)
 * via Google Play Services Identity AuthorizationClient scoped to the signed-in Gmail
 * account so Amar Hisab can create and access only its own dedicated encrypted backup folder.
 */
object GoogleDriveAuthHelper {
    const val DRIVE_FILE_SCOPE = "https://www.googleapis.com/auth/drive.file"

    suspend fun requestDriveAccessToken(
        context: Context,
        accountEmail: String? = null
    ): DriveAuthOutcome {
        return try {
            val builder = AuthorizationRequest.builder()
                .setRequestedScopes(listOf(Scope(DRIVE_FILE_SCOPE)))
            val trimmedEmail = accountEmail?.trim().orEmpty()
            if (trimmedEmail.contains("@")) {
                builder.setAccount(Account(trimmedEmail, "com.google"))
            }
            val request = builder.build()
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
                    DriveAuthOutcome.Error(
                        message = "Google Drive permission was not granted.",
                        kind = DriveAuthErrorKind.DECLINED
                    )
                }
            }
        } catch (e: Exception) {
            val kind = classifyAuthException(e)
            DriveAuthOutcome.Error(
                message = e.localizedMessage ?: "Unable to connect to Google Drive.",
                kind = kind
            )
        }
    }

    fun extractConsentResult(
        context: Context,
        resultOk: Boolean,
        data: Intent?
    ): DriveConsentExtraction {
        if (data != null) {
            try {
                val authResult = Identity.getAuthorizationClient(context.applicationContext)
                    .getAuthorizationResultFromIntent(data)
                val token = authResult.accessToken?.takeIf { it.isNotBlank() }
                if (token != null) {
                    return DriveConsentExtraction.Token(token)
                }
            } catch (e: Exception) {
                return when (classifyAuthException(e)) {
                    DriveAuthErrorKind.OAUTH_RESTRICTED_403 ->
                        DriveConsentExtraction.Restricted403(e.localizedMessage.orEmpty())
                    DriveAuthErrorKind.DECLINED ->
                        DriveConsentExtraction.Declined
                    else ->
                        if (!resultOk) DriveConsentExtraction.Declined
                        else DriveConsentExtraction.Failed(e.localizedMessage.orEmpty())
                }
            }
        }
        return if (!resultOk) {
            DriveConsentExtraction.Declined
        } else {
            DriveConsentExtraction.Failed("Missing Drive access token.")
        }
    }

    fun extractAccessTokenFromResult(context: Context, data: Intent?): String? {
        return when (val extracted = extractConsentResult(context, resultOk = true, data = data)) {
            is DriveConsentExtraction.Token -> extracted.accessToken
            else -> null
        }
    }

    fun classifyAuthException(error: Throwable?): DriveAuthErrorKind {
        val statusCode = (error as? ApiException)?.statusCode
        val lower = error?.localizedMessage.orEmpty().lowercase()
        return when {
            statusCode == CommonStatusCodes.DEVELOPER_ERROR ||
                lower.contains("403") ||
                lower.contains("access_denied") ||
                lower.contains("org_internal") ||
                lower.contains("unverified") ||
                lower.contains("developer_error") ||
                lower.contains("blocked") ||
                lower.contains("verification") -> DriveAuthErrorKind.OAUTH_RESTRICTED_403

            statusCode == CommonStatusCodes.NETWORK_ERROR ||
                lower.contains("network") ||
                lower.contains("offline") ||
                lower.contains("timeout") ||
                lower.contains("unable to resolve") -> DriveAuthErrorKind.OFFLINE

            statusCode == CommonStatusCodes.CANCELED ||
                lower.contains("cancel") ||
                lower.contains("declined") -> DriveAuthErrorKind.DECLINED

            statusCode == CommonStatusCodes.API_NOT_CONNECTED ||
                statusCode == CommonStatusCodes.SERVICE_DISABLED ||
                statusCode == CommonStatusCodes.SERVICE_VERSION_UPDATE_REQUIRED ||
                lower.contains("play services") -> DriveAuthErrorKind.PLAY_SERVICES_UNAVAILABLE

            else -> DriveAuthErrorKind.GENERAL
        }
    }
}
