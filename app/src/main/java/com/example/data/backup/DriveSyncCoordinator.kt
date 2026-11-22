package com.example.data.backup

import com.example.data.local.AppDatabase
import com.example.data.local.UserPreferencesRepository
import kotlinx.coroutines.flow.first

enum class DriveSyncStatus {
    SIGNED_OUT,
    IDLE,
    SYNCING,
    RESTORING,
    SYNCED,
    PENDING_OFFLINE,
    NEEDS_PERMISSION,
    RESTRICTED_403,
    ERROR
}

data class DriveSyncUiState(
    val status: DriveSyncStatus = DriveSyncStatus.SIGNED_OUT,
    val lastSyncedAtMillis: Long = 0L,
    val hasPendingChanges: Boolean = false,
    val folderName: String = GoogleDriveBackupService.BACKUP_FOLDER_NAME,
    val isEncrypted: Boolean = true,
    val message: String? = null,
    val isError: Boolean = false
)

sealed interface DriveAutoRestoreOutcome {
    data class Restored(val transactionCount: Int, val updatedAtMillis: Long) : DriveAutoRestoreOutcome
    data class UploadedLocal(val updatedAtMillis: Long) : DriveAutoRestoreOutcome
    data object Unauthorized : DriveAutoRestoreOutcome
    data class Restricted403(val message: String) : DriveAutoRestoreOutcome
    data class Error(val message: String) : DriveAutoRestoreOutcome
}

/**
 * Coordinates encrypted Google Drive backup uploads and automatic login restore
 * using AES-256-GCM encryption via [GoogleDriveBackupService].
 */
class DriveSyncCoordinator(
    private val backupService: GoogleDriveBackupService = GoogleDriveBackupService()
) {
    suspend fun uploadLocalSnapshot(
        accessToken: String,
        userEmail: String,
        db: AppDatabase,
        prefsRepo: UserPreferencesRepository,
        nowMillis: Long = System.currentTimeMillis()
    ): DriveUploadOutcome {
        val prefs = prefsRepo.userPrefs.first()
        val exportJson = BackupManager.exportJson(db, prefs)
        val outcome = backupService.uploadEncryptedBackup(
            accessToken = accessToken,
            userEmail = userEmail,
            backupJson = exportJson,
            updatedAtMillis = nowMillis
        )
        if (outcome is DriveUploadOutcome.Success) {
            prefsRepo.markDriveSynced(outcome.updatedAtMillis, clearPending = true)
        }
        return outcome
    }

    suspend fun autoRestoreOrInitialSync(
        accessToken: String,
        userEmail: String,
        db: AppDatabase,
        prefsRepo: UserPreferencesRepository,
        forceRestore: Boolean = false,
        nowMillis: Long = System.currentTimeMillis()
    ): DriveAutoRestoreOutcome {
        val prefs = prefsRepo.userPrefs.first()
        val isLocalEmpty = isDatabaseEmpty(db)

        return when (val remote = backupService.downloadLatestEncryptedBackup(accessToken, userEmail)) {
            is DriveRestoreOutcome.Found -> {
                val shouldRestore = forceRestore ||
                    isLocalEmpty ||
                    remote.updatedAtMillis > prefs.driveLastLocalModifiedAtMillis

                if (shouldRestore) {
                    val txCount = BackupManager.restoreFromJson(remote.plainJson, db, prefsRepo)
                    val stamp = remote.updatedAtMillis.takeIf { it > 0L } ?: nowMillis
                    prefsRepo.markDriveSynced(stamp, clearPending = true)
                    DriveAutoRestoreOutcome.Restored(
                        transactionCount = txCount,
                        updatedAtMillis = stamp
                    )
                } else {
                    when (val upload = uploadLocalSnapshot(accessToken, userEmail, db, prefsRepo, nowMillis)) {
                        is DriveUploadOutcome.Success ->
                            DriveAutoRestoreOutcome.UploadedLocal(upload.updatedAtMillis)
                        is DriveUploadOutcome.Unauthorized ->
                            DriveAutoRestoreOutcome.Unauthorized
                        is DriveUploadOutcome.Restricted403 ->
                            DriveAutoRestoreOutcome.Restricted403(upload.message)
                        is DriveUploadOutcome.Error ->
                            DriveAutoRestoreOutcome.Error(upload.message)
                    }
                }
            }

            is DriveRestoreOutcome.NotFound -> {
                if (forceRestore) {
                    DriveAutoRestoreOutcome.Error("No encrypted backup found on Google Drive yet.")
                } else {
                    when (val upload = uploadLocalSnapshot(accessToken, userEmail, db, prefsRepo, nowMillis)) {
                        is DriveUploadOutcome.Success ->
                            DriveAutoRestoreOutcome.UploadedLocal(upload.updatedAtMillis)
                        is DriveUploadOutcome.Unauthorized ->
                            DriveAutoRestoreOutcome.Unauthorized
                        is DriveUploadOutcome.Restricted403 ->
                            DriveAutoRestoreOutcome.Restricted403(upload.message)
                        is DriveUploadOutcome.Error ->
                            DriveAutoRestoreOutcome.Error(upload.message)
                    }
                }
            }

            is DriveRestoreOutcome.Unauthorized -> DriveAutoRestoreOutcome.Unauthorized
            is DriveRestoreOutcome.Restricted403 -> DriveAutoRestoreOutcome.Restricted403(remote.message)
            is DriveRestoreOutcome.Error -> DriveAutoRestoreOutcome.Error(remote.message)
        }
    }

    private suspend fun isDatabaseEmpty(db: AppDatabase): Boolean {
        val hasAccounts = db.accountDao().getAllAccountsOnce().isNotEmpty()
        if (hasAccounts) return false
        val hasTx = db.transactionDao().getAllTransactionsOnce().isNotEmpty()
        if (hasTx) return false
        val hasGoals = db.savingsGoalDao().getAllGoalsOnce().isNotEmpty()
        if (hasGoals) return false
        val hasLoans = db.loanDao().getAllLoansOnce().isNotEmpty()
        return !hasLoans
    }
}
