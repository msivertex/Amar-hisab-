package com.example

import android.content.Context
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.ai.LocalReceiptTextParser
import com.example.data.ai.LocalVoiceTransactionParser
import com.example.data.backup.BackupCrypto
import com.example.data.backup.BackupManager
import com.example.data.backup.DriveAutoRestoreOutcome
import com.example.data.backup.DriveCloudTransport
import com.example.data.backup.DriveFileMetadata
import com.example.data.backup.DriveSyncCoordinator
import com.example.data.backup.DriveSyncStatus
import com.example.data.backup.DriveSyncUiState
import com.example.data.backup.DriveUploadOutcome
import com.example.data.backup.GoogleDriveBackupService
import com.example.data.local.AccountEntity
import com.example.data.local.AppDatabase
import com.example.data.local.TransactionEntity
import com.example.data.local.UserPreferencesRepository
import com.example.data.local.UserPrefs
import com.example.data.model.AccountType
import com.example.data.model.AuthMode
import com.example.data.model.ExpenseCategory
import com.example.data.model.TransactionType
import com.example.data.model.VoiceParsedTransaction
import com.example.localization.AppLanguage
import com.example.localization.StringsProvider
import com.example.ui.screens.BackupRestoreSubPage
import com.example.ui.screens.VoiceEntrySheet
import com.example.ui.theme.AmarHisabTheme
import com.example.ui.viewmodel.TtsUiState
import com.example.ui.viewmodel.VoiceParseUiState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.security.GeneralSecurityException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AiVoiceReceiptAndDriveSyncRobolectricTest {

    @get:Rule
    val composeRule = createComposeRule()

    private class FakeDriveCloudTransport : DriveCloudTransport {
        var folderId: String? = null
        var folderCreateCount: Int = 0
        var uploadCount: Int = 0
        var lastUploadedPayload: String? = null
        private var storedFileId: String? = null

        override suspend fun findBackupFolder(accessToken: String): String? = folderId

        override suspend fun createBackupFolder(accessToken: String): String {
            folderCreateCount += 1
            val id = "folder_amar_hisab_$folderCreateCount"
            folderId = id
            return id
        }

        override suspend fun findLatestVaultFile(accessToken: String, folderId: String): DriveFileMetadata? {
            val id = storedFileId ?: return null
            return DriveFileMetadata(
                id = id,
                name = GoogleDriveBackupService.VAULT_FILE_NAME,
                modifiedTime = "2026-11-22T10:00:00Z"
            )
        }

        override suspend fun uploadVaultFile(
            accessToken: String,
            folderId: String,
            existingFileId: String?,
            encryptedPayload: String
        ): String {
            uploadCount += 1
            lastUploadedPayload = encryptedPayload
            val id = existingFileId ?: "vault_file_1"
            storedFileId = id
            return id
        }

        override suspend fun downloadVaultFile(accessToken: String, fileId: String): String {
            return requireNotNull(lastUploadedPayload) { "No vault stored." }
        }
    }

    @Test
    fun local_voice_parser_handles_bangla_transfers_bkash_expenses_and_foreign_currencies() {
        val accounts = listOf(
            AccountEntity(
                id = 1L,
                name = "Trust Bank Salary",
                type = AccountType.DEBIT_BANK,
                balance = 45000.0,
                bankOrProvider = "Trust Bank",
                colorArgb = 0xFF0284C7L,
                currencyCode = "BDT"
            ),
            AccountEntity(
                id = 2L,
                name = "Dutch-Bangla Savings",
                type = AccountType.DEBIT_BANK,
                balance = 12000.0,
                bankOrProvider = "Dutch-Bangla Bank",
                colorArgb = 0xFF10B981L,
                currencyCode = "BDT"
            ),
            AccountEntity(
                id = 3L,
                name = "bKash Personal",
                type = AccountType.MFS,
                balance = 8500.0,
                bankOrProvider = "bKash",
                colorArgb = 0xFFE2136EL,
                currencyCode = "BDT"
            ),
            AccountEntity(
                id = 4L,
                name = "KFH Kuwait Card",
                type = AccountType.DEBIT_BANK,
                balance = 240.0,
                bankOrProvider = "Kuwait Finance House",
                colorArgb = 0xFF059669L,
                currencyCode = "KWD"
            )
        )

        // 1. Bangla account-to-account transfer (Trust -> Dutch-Bangla)
        val transfer = LocalVoiceTransactionParser.parse(
            utterance = "Trust থেকে Dutch-Bangla তে ৫০০০ টাকা পাঠিয়েছি",
            accounts = accounts,
            language = AppLanguage.BN,
            defaultCurrencyCode = "BDT"
        )
        assertEquals(TransactionType.TRANSFER, transfer.type)
        assertEquals(5000.0, transfer.amountBdt, 0.001)
        assertEquals("BDT", transfer.currencyCode)
        val matchedFrom = LocalVoiceTransactionParser.matchAccountByHint(accounts, transfer.accountHint, transfer.currencyCode)
        val matchedTo = LocalVoiceTransactionParser.matchAccountByHint(accounts, transfer.toAccountHint, null, matchedFrom?.id)
        assertEquals(1L, matchedFrom?.id)
        assertEquals(2L, matchedTo?.id)

        // 2. Bangla bKash groceries/market expense
        val bkashExpense = LocalVoiceTransactionParser.parse(
            utterance = "বিকাশ থেকে বাজার খরচ ১৫০০ টাকা",
            accounts = accounts,
            language = AppLanguage.BN,
            defaultCurrencyCode = "BDT"
        )
        assertEquals(TransactionType.EXPENSE, bkashExpense.type)
        assertEquals(1500.0, bkashExpense.amountBdt, 0.001)
        assertEquals(ExpenseCategory.GROCERIES, bkashExpense.category)
        assertEquals("BDT", bkashExpense.currencyCode)
        val matchedBkash = LocalVoiceTransactionParser.matchAccountByHint(accounts, bkashExpense.accountHint, bkashExpense.currencyCode)
        assertEquals(3L, matchedBkash?.id)

        // 3. Kuwaiti Dinar (KD) expense
        val kdExpense = LocalVoiceTransactionParser.parse(
            utterance = "Paid KD 12.750 for groceries from KFH",
            accounts = accounts,
            language = AppLanguage.EN,
            defaultCurrencyCode = "BDT"
        )
        assertEquals(TransactionType.EXPENSE, kdExpense.type)
        assertEquals(12.75, kdExpense.amountBdt, 0.001)
        assertEquals("KWD", kdExpense.currencyCode)
        val matchedKfh = LocalVoiceTransactionParser.matchAccountByHint(accounts, kdExpense.accountHint, kdExpense.currencyCode)
        assertEquals(4L, matchedKfh?.id)
    }

    @Test
    fun local_receipt_ocr_parser_prioritizes_total_amount_and_currency_across_currencies() {
        // 1. Kuwait receipt (KD) with multiple line items and GRAND TOTAL
        val kuwaitReceipt = """
            LULU HYPERMARKET KUWAIT
            Milk 1.250
            Bread 0.650
            Rice 5kg 3.550
            SUBTOTAL 5.450
            GRAND TOTAL KD 18.450
            CARD **** 4321
        """.trimIndent()
        val parsedKd = LocalReceiptTextParser.parseRawReceiptText(kuwaitReceipt, "BDT")
        assertNotNull(parsedKd)
        assertEquals(18.45, parsedKd!!.amountBdt, 0.001)
        assertEquals("KWD", parsedKd.currencyCode)
        assertEquals(TransactionType.EXPENSE, parsedKd.transactionType)
        assertTrue(parsedKd.isLocalFallback)
        assertEquals("4321", parsedKd.accountLast4Hint)

        // 2. Bangladesh bKash Cash Out voucher
        val bkashVoucher = """
            bKash
            Cash Out Successful
            Amount: ৳ 2,500.00
            Fee: ৳ 46.25
            TrxID: 9K8A7B6C5D
        """.trimIndent()
        val parsedBdt = LocalReceiptTextParser.parseRawReceiptText(bkashVoucher, "BDT")
        assertNotNull(parsedBdt)
        assertEquals(2500.0, parsedBdt!!.amountBdt, 0.001)
        assertEquals("BDT", parsedBdt.currencyCode)
        assertEquals("bKash", parsedBdt.providerHint)
        assertEquals(46.25, parsedBdt.feeAmount, 0.01)
        assertEquals("9K8A7B6C5D", parsedBdt.referenceId)

        // 3. Euro receipt (€)
        val euroReceipt = """
            CARREFOUR EXPRESS
            TOTAL € 45.90
        """.trimIndent()
        val parsedEur = LocalReceiptTextParser.parseRawReceiptText(euroReceipt, "BDT")
        assertNotNull(parsedEur)
        assertEquals(45.90, parsedEur!!.amountBdt, 0.001)
        assertEquals("EUR", parsedEur.currencyCode)
    }

    @Test
    fun backup_crypto_encrypts_with_aes256_gcm_and_blocks_wrong_account_or_tampering() {
        val sampleJson = """{"app":"Amar Hisab","accounts":[{"name":"Secret Vault Card","balance":987654.25}]}"""
        val email = "Shariful.Test@Gmail.com"
        val stamp = 1_763_800_000_000L

        val encrypted1 = BackupCrypto.encrypt(sampleJson, email, stamp)
        val encrypted2 = BackupCrypto.encrypt(sampleJson, email, stamp)

        // Must never leak plaintext financial details in Drive file
        assertFalse(encrypted1.contains("Secret Vault Card"))
        assertFalse(encrypted1.contains("987654"))
        // Distinct random IV & salt every upload
        assertNotEquals(encrypted1, encrypted2)

        // Same Gmail (different casing/whitespace) decrypts identically
        val decrypted = BackupCrypto.decrypt(encrypted1, "  shariful.test@gmail.com ")
        assertEquals(sampleJson, decrypted.plainJson)
        assertEquals(stamp, decrypted.updatedAtMillis)

        // Different Gmail fails GCM authentication
        var wrongEmailFailed = false
        try {
            BackupCrypto.decrypt(encrypted1, "other.user@gmail.com")
        } catch (_: GeneralSecurityException) {
            wrongEmailFailed = true
        }
        assertTrue("Expected wrong Gmail decryption to fail", wrongEmailFailed)

        // Tampered ciphertext fails GCM authentication
        val tampered = encrypted1.replaceFirst("\"ciphertext\":\"", "\"ciphertext\":\"AA")
        var tamperFailed = false
        try {
            BackupCrypto.decrypt(tampered, email)
        } catch (_: Exception) {
            tamperFailed = true
        }
        assertTrue("Expected tampered ciphertext to fail authentication", tamperFailed)
    }

    @Test
    fun google_drive_sync_coordinator_encrypts_uploads_and_auto_restores_on_relogin() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val prefsRepo = UserPreferencesRepository(context)
        val transport = FakeDriveCloudTransport()
        val service = GoogleDriveBackupService(transport)
        val coordinator = DriveSyncCoordinator(service)
        val email = "user.drive.test@gmail.com"
        val token = "test_oauth_access_token"

        try {
            // 1. Insert real financial data
            db.accountDao().insertAccount(
                AccountEntity(
                    id = 10L,
                    name = "Trust Bank Primary",
                    type = AccountType.DEBIT_BANK,
                    balance = 75000.0,
                    bankOrProvider = "Trust Bank",
                    colorArgb = 0xFF0284C7L,
                    currencyCode = "BDT"
                )
            )
            db.transactionDao().insertTransaction(
                TransactionEntity(
                    id = 100L,
                    title = "Weekly Bazaar",
                    amount = 2450.0,
                    type = TransactionType.EXPENSE,
                    category = ExpenseCategory.GROCERIES,
                    accountId = 10L,
                    note = "Encrypted Drive sync test",
                    timestamp = 1_763_800_000_000L,
                    currencyCode = "BDT"
                )
            )

            // 2. Sync to Drive (creates encrypted folder + vault)
            val uploadOutcome = coordinator.uploadLocalSnapshot(
                accessToken = token,
                userEmail = email,
                db = db,
                prefsRepo = prefsRepo,
                nowMillis = 1_763_800_005_000L
            )
            assertTrue(uploadOutcome is DriveUploadOutcome.Success)
            assertEquals(1, transport.folderCreateCount)
            assertEquals(1, transport.uploadCount)
            assertNotNull(transport.lastUploadedPayload)
            assertFalse(
                "Drive payload must be encrypted ciphertext, never plaintext JSON",
                transport.lastUploadedPayload!!.contains("Trust Bank Primary")
            )

            // 3. Simulate logout / app reinstall / new device (empty local DB)
            BackupManager.clearAllFinancialData(db)
            assertTrue(db.accountDao().getAllAccountsOnce().isEmpty())
            assertTrue(db.transactionDao().getAllTransactionsOnce().isEmpty())

            // 4. Re-login with same Gmail -> automatic restore from encrypted Drive vault
            val restoreOutcome = coordinator.autoRestoreOrInitialSync(
                accessToken = token,
                userEmail = email,
                db = db,
                prefsRepo = prefsRepo,
                forceRestore = false
            )
            assertTrue(restoreOutcome is DriveAutoRestoreOutcome.Restored)
            val restoredAccounts = db.accountDao().getAllAccountsOnce()
            val restoredTx = db.transactionDao().getAllTransactionsOnce()
            assertEquals(1, restoredAccounts.size)
            assertEquals("Trust Bank Primary", restoredAccounts.first().name)
            assertEquals(75000.0, restoredAccounts.first().balance, 0.001)
            assertEquals(1, restoredTx.size)
            assertEquals("Weekly Bazaar", restoredTx.first().title)
            assertEquals(2450.0, restoredTx.first().amount, 0.001)
        } finally {
            db.close()
        }
    }

    @Test
    fun voice_entry_sheet_executes_parsed_transfer_and_backup_page_displays_encrypted_drive_controls() {
        val wallets = listOf(
            AccountEntity(
                id = 1L,
                name = "Trust Bank",
                type = AccountType.DEBIT_BANK,
                balance = 30000.0,
                bankOrProvider = "Trust Bank",
                colorArgb = 0xFF0284C7L,
                currencyCode = "BDT"
            ),
            AccountEntity(
                id = 2L,
                name = "Dutch-Bangla Bank",
                type = AccountType.DEBIT_BANK,
                balance = 15000.0,
                bankOrProvider = "Dutch-Bangla Bank",
                colorArgb = 0xFF10B981L,
                currencyCode = "BDT"
            )
        )
        var executedFrom = 0L
        var executedTo = 0L
        var executedAmount = 0.0

        composeRule.setContent {
            AmarHisabTheme(darkTheme = false, isBangla = true) {
                VoiceEntrySheet(
                    accounts = wallets,
                    voiceState = VoiceParseUiState.Success(
                        VoiceParsedTransaction(
                            title = "Trust Bank → Dutch-Bangla Bank",
                            amountBdt = 5000.0,
                            type = TransactionType.TRANSFER,
                            category = ExpenseCategory.TRANSFER,
                            accountHint = "Trust Bank",
                            currencyCode = "BDT",
                            toAccountHint = "Dutch-Bangla Bank",
                            note = "Trust থেকে Dutch-Bangla তে ৫০০০ টাকা পাঠিয়েছি",
                            isLocalFallback = false
                        )
                    ),
                    ttsState = TtsUiState.Idle,
                    language = AppLanguage.BN,
                    strings = StringsProvider.bangla,
                    onParseUtterance = {},
                    onPlayBriefingTts = {},
                    onStopTts = {},
                    onSaveParsedTransaction = { _, _, _, _, _ -> },
                    onDismiss = {},
                    defaultCurrencyCode = "BDT",
                    onExecuteTransfer = { fromId, toId, amount, _ ->
                        executedFrom = fromId
                        executedTo = toId
                        executedAmount = amount
                    }
                )
            }
        }

        composeRule.onNodeWithTag("voice_parsed_card").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("voice_save_button")
            .performScrollTo()
            .assertIsDisplayed()
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()

        assertEquals(1L, executedFrom)
        assertEquals(2L, executedTo)
        assertEquals(5000.0, executedAmount, 0.001)
    }

    @Test
    fun backup_restore_subpage_renders_encrypted_google_drive_folder_and_sync_restore_buttons() {
        var syncClicked = false
        var restoreClicked = false

        composeRule.setContent {
            AmarHisabTheme(darkTheme = false, isBangla = true) {
                BackupRestoreSubPage(
                    prefs = UserPrefs(
                        language = AppLanguage.BN,
                        authMode = AuthMode.GOOGLE,
                        userEmail = "shariful@gmail.com",
                        driveLastSyncedAtMillis = 1_763_800_000_000L
                    ),
                    backupStatus = null,
                    isGoogleSignInLoading = false,
                    googleNotice = null,
                    strings = StringsProvider.bangla,
                    onBack = {},
                    onExportJsonClick = {},
                    onExportCsvClick = {},
                    onImportRestoreClick = {},
                    onDownloadSampleTemplateClick = {},
                    onTriggerGoogleSignIn = {},
                    onDeleteAllFinancialData = {},
                    onClearBackupStatus = {},
                    driveSyncState = DriveSyncUiState(
                        status = DriveSyncStatus.SYNCED,
                        lastSyncedAtMillis = 1_763_800_000_000L
                    ),
                    onSyncDriveNow = { syncClicked = true },
                    onRestoreFromDrive = { restoreClicked = true }
                )
            }
        }

        composeRule.onNodeWithTag("backup_restore_list")
            .performScrollToNode(hasTestTag("backup_cloud_section_card"))
        composeRule.onNodeWithTag("backup_drive_encrypted_folder_badge").assertIsDisplayed()
        composeRule.onNodeWithTag("backup_cloud_backup_now_button")
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.onNodeWithTag("backup_cloud_restore_now_button")
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()

        assertTrue(syncClicked)
        assertTrue(restoreClicked)
    }
}
