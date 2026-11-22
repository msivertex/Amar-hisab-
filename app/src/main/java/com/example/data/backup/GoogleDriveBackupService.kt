package com.example.data.backup

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

data class DriveFileMetadata(
    val id: String,
    val name: String,
    val modifiedTime: String = ""
)

sealed interface DriveUploadOutcome {
    data class Success(
        val folderId: String,
        val fileId: String,
        val updatedAtMillis: Long
    ) : DriveUploadOutcome

    data object Unauthorized : DriveUploadOutcome
    data class Restricted403(val message: String) : DriveUploadOutcome
    data class Error(val message: String) : DriveUploadOutcome
}

sealed interface DriveRestoreOutcome {
    data class Found(
        val plainJson: String,
        val updatedAtMillis: Long,
        val fileId: String
    ) : DriveRestoreOutcome

    data object NotFound : DriveRestoreOutcome
    data object Unauthorized : DriveRestoreOutcome
    data class Restricted403(val message: String) : DriveRestoreOutcome
    data class Error(val message: String) : DriveRestoreOutcome
}

class DriveUnauthorizedException : IOException("Google Drive access token expired or revoked.")
class DriveAccessRestrictedException(message: String) : IOException(message)

interface DriveCloudTransport {
    suspend fun findBackupFolder(accessToken: String): String?
    suspend fun createBackupFolder(accessToken: String): String
    suspend fun findLatestVaultFile(accessToken: String, folderId: String): DriveFileMetadata?
    suspend fun uploadVaultFile(
        accessToken: String,
        folderId: String,
        existingFileId: String?,
        encryptedPayload: String
    ): String

    suspend fun downloadVaultFile(accessToken: String, fileId: String): String
}

/**
 * Manages Amar Hisab's dedicated encrypted backup folder & vault file in Google Drive.
 *
 * Folder: "Amar Hisab Encrypted Backups"
 * File:   "amar_hisab_vault.enc" (AES-256-GCM ciphertext envelope — never plaintext JSON)
 */
class GoogleDriveBackupService(
    private val transport: DriveCloudTransport = HttpGoogleDriveTransport()
) {
    companion object {
        const val BACKUP_FOLDER_NAME = "Amar Hisab Encrypted Backups"
        const val VAULT_FILE_NAME = "amar_hisab_vault.enc"
        const val FOLDER_MIME_TYPE = "application/vnd.google-apps.folder"
        const val VAULT_MIME_TYPE = "application/octet-stream"
    }

    suspend fun uploadEncryptedBackup(
        accessToken: String,
        userEmail: String,
        backupJson: String,
        updatedAtMillis: Long = System.currentTimeMillis()
    ): DriveUploadOutcome = withContext(Dispatchers.IO) {
        try {
            val encryptedEnvelope = BackupCrypto.encrypt(
                plainJson = backupJson,
                userEmail = userEmail,
                updatedAtMillis = updatedAtMillis
            )
            val folderId = transport.findBackupFolder(accessToken)
                ?: transport.createBackupFolder(accessToken)
            val existingFile = transport.findLatestVaultFile(accessToken, folderId)
            val fileId = transport.uploadVaultFile(
                accessToken = accessToken,
                folderId = folderId,
                existingFileId = existingFile?.id,
                encryptedPayload = encryptedEnvelope
            )
            DriveUploadOutcome.Success(
                folderId = folderId,
                fileId = fileId,
                updatedAtMillis = updatedAtMillis
            )
        } catch (_: DriveUnauthorizedException) {
            DriveUploadOutcome.Unauthorized
        } catch (e: DriveAccessRestrictedException) {
            DriveUploadOutcome.Restricted403(
                e.localizedMessage ?: "Google Drive access restricted (Error 403)."
            )
        } catch (e: Exception) {
            DriveUploadOutcome.Error(e.localizedMessage ?: "Failed to sync encrypted backup.")
        }
    }

    suspend fun downloadLatestEncryptedBackup(
        accessToken: String,
        userEmail: String
    ): DriveRestoreOutcome = withContext(Dispatchers.IO) {
        try {
            val folderId = transport.findBackupFolder(accessToken)
                ?: return@withContext DriveRestoreOutcome.NotFound
            val latestFile = transport.findLatestVaultFile(accessToken, folderId)
                ?: return@withContext DriveRestoreOutcome.NotFound
            val encryptedEnvelope = transport.downloadVaultFile(accessToken, latestFile.id)
            val decrypted = BackupCrypto.decrypt(
                envelopeJson = encryptedEnvelope,
                userEmail = userEmail
            )
            DriveRestoreOutcome.Found(
                plainJson = decrypted.plainJson,
                updatedAtMillis = decrypted.updatedAtMillis,
                fileId = latestFile.id
            )
        } catch (_: DriveUnauthorizedException) {
            DriveRestoreOutcome.Unauthorized
        } catch (e: DriveAccessRestrictedException) {
            DriveRestoreOutcome.Restricted403(
                e.localizedMessage ?: "Google Drive access restricted (Error 403)."
            )
        } catch (e: Exception) {
            DriveRestoreOutcome.Error(e.localizedMessage ?: "Failed to restore encrypted backup.")
        }
    }
}

class HttpGoogleDriveTransport(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
) : DriveCloudTransport {

    companion object {
        private const val DRIVE_FILES_URL = "https://www.googleapis.com/drive/v3/files"
        private const val DRIVE_UPLOAD_URL = "https://www.googleapis.com/upload/drive/v3/files"
        private const val MAX_DOWNLOAD_BYTES = 16L * 1024L * 1024L
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        private val OCTET_MEDIA_TYPE = "application/octet-stream".toMediaType()
    }

    override suspend fun findBackupFolder(accessToken: String): String? {
        val query = "mimeType='${GoogleDriveBackupService.FOLDER_MIME_TYPE}' " +
            "and trashed=false " +
            "and (appProperties has { key='amarHisabFolder' and value='true' } " +
            "or name='${GoogleDriveBackupService.BACKUP_FOLDER_NAME}')"
        val url = DRIVE_FILES_URL.toHttpUrl().newBuilder()
            .addQueryParameter("q", query)
            .addQueryParameter("spaces", "drive")
            .addQueryParameter("fields", "files(id,name,modifiedTime)")
            .addQueryParameter("orderBy", "modifiedTime desc")
            .addQueryParameter("pageSize", "5")
            .build()

        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $accessToken")
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            checkStatus(response.code)
            val body = response.body?.string().orEmpty()
            val files = JSONObject(body).optJSONArray("files") ?: return null
            if (files.length() == 0) return null
            return files.getJSONObject(0).optString("id").takeIf { it.isNotBlank() }
        }
    }

    override suspend fun createBackupFolder(accessToken: String): String {
        val metadata = JSONObject().apply {
            put("name", GoogleDriveBackupService.BACKUP_FOLDER_NAME)
            put("mimeType", GoogleDriveBackupService.FOLDER_MIME_TYPE)
            put("appProperties", JSONObject().apply {
                put("amarHisabFolder", "true")
            })
        }
        val url = DRIVE_FILES_URL.toHttpUrl().newBuilder()
            .addQueryParameter("fields", "id")
            .build()

        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $accessToken")
            .post(metadata.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()

        client.newCall(request).execute().use { response ->
            checkStatus(response.code)
            val body = response.body?.string().orEmpty()
            return JSONObject(body).getString("id")
        }
    }

    override suspend fun findLatestVaultFile(accessToken: String, folderId: String): DriveFileMetadata? {
        val safeFolderId = folderId.replace("'", "")
        val query = "'$safeFolderId' in parents " +
            "and trashed=false " +
            "and (appProperties has { key='amarHisabVault' and value='v1' } " +
            "or name='${GoogleDriveBackupService.VAULT_FILE_NAME}')"
        val url = DRIVE_FILES_URL.toHttpUrl().newBuilder()
            .addQueryParameter("q", query)
            .addQueryParameter("spaces", "drive")
            .addQueryParameter("fields", "files(id,name,modifiedTime)")
            .addQueryParameter("orderBy", "modifiedTime desc")
            .addQueryParameter("pageSize", "5")
            .build()

        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $accessToken")
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            checkStatus(response.code)
            val body = response.body?.string().orEmpty()
            val files = JSONObject(body).optJSONArray("files") ?: return null
            if (files.length() == 0) return null
            val first = files.getJSONObject(0)
            val id = first.optString("id")
            if (id.isBlank()) return null
            return DriveFileMetadata(
                id = id,
                name = first.optString("name", GoogleDriveBackupService.VAULT_FILE_NAME),
                modifiedTime = first.optString("modifiedTime", "")
            )
        }
    }

    override suspend fun uploadVaultFile(
        accessToken: String,
        folderId: String,
        existingFileId: String?,
        encryptedPayload: String
    ): String {
        return if (!existingFileId.isNullOrBlank()) {
            val url = "$DRIVE_UPLOAD_URL/$existingFileId".toHttpUrl().newBuilder()
                .addQueryParameter("uploadType", "media")
                .addQueryParameter("fields", "id")
                .build()
            val request = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer $accessToken")
                .patch(encryptedPayload.toRequestBody(OCTET_MEDIA_TYPE))
                .build()
            client.newCall(request).execute().use { response ->
                checkStatus(response.code)
                val body = response.body?.string().orEmpty()
                JSONObject(body).optString("id", existingFileId)
            }
        } else {
            val boundary = "amar_hisab_vault_boundary_${System.nanoTime()}"
            val metadata = JSONObject().apply {
                put("name", GoogleDriveBackupService.VAULT_FILE_NAME)
                put("mimeType", GoogleDriveBackupService.VAULT_MIME_TYPE)
                put("parents", JSONArray().put(folderId))
                put("appProperties", JSONObject().apply {
                    put("amarHisabVault", "v1")
                })
            }
            val multipartBody = buildString {
                append("--").append(boundary).append("\r\n")
                append("Content-Type: application/json; charset=UTF-8\r\n\r\n")
                append(metadata.toString()).append("\r\n")
                append("--").append(boundary).append("\r\n")
                append("Content-Type: application/octet-stream\r\n\r\n")
                append(encryptedPayload).append("\r\n")
                append("--").append(boundary).append("--")
            }

            val url = DRIVE_UPLOAD_URL.toHttpUrl().newBuilder()
                .addQueryParameter("uploadType", "multipart")
                .addQueryParameter("fields", "id")
                .build()
            val request = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer $accessToken")
                .post(multipartBody.toRequestBody("multipart/related; boundary=$boundary".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                checkStatus(response.code)
                val body = response.body?.string().orEmpty()
                JSONObject(body).getString("id")
            }
        }
    }

    override suspend fun downloadVaultFile(accessToken: String, fileId: String): String {
        val url = "$DRIVE_FILES_URL/$fileId".toHttpUrl().newBuilder()
            .addQueryParameter("alt", "media")
            .build()
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $accessToken")
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            checkStatus(response.code)
            val body = response.body ?: throw IOException("Empty encrypted backup response.")
            if (body.contentLength() > MAX_DOWNLOAD_BYTES) {
                throw IOException("Encrypted backup file exceeds safety size limit.")
            }
            val raw = body.string()
            if (raw.length.toLong() > MAX_DOWNLOAD_BYTES) {
                throw IOException("Encrypted backup file exceeds safety size limit.")
            }
            return raw
        }
    }

    private fun checkStatus(code: Int) {
        if (code == 401) {
            throw DriveUnauthorizedException()
        }
        if (code == 403) {
            throw DriveAccessRestrictedException("Google Drive API access restricted (HTTP 403).")
        }
        if (code !in 200..299) {
            throw IOException("Google Drive request failed (HTTP $code).")
        }
    }
}
