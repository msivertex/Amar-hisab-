package com.example

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.VoiceEntrySheet
import com.example.ui.screens.WelcomeScreen
import com.example.ui.theme.AmarHisabTheme
import com.example.ui.viewmodel.TtsUiState
import com.example.ui.viewmodel.VoiceParseUiState
import androidx.compose.ui.test.performTextReplacement
import kotlinx.coroutines.flow.first
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
    fun welcome_screen_shows_exact_two_entry_options_and_guest_proceeds_smoothly() {
        var guestContinued = false

        composeRule.setContent {
            AmarHisabTheme(darkTheme = false, isBangla = false) {
                WelcomeScreen(
                    language = AppLanguage.EN,
                    strings = StringsProvider.english,
                    onGoogleSignedIn = {},
                    onContinueAsGuest = { guestContinued = true }
                )
            }
        }

        composeRule.mainClock.advanceTimeBy(1200L)
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("welcome_google_signin_button")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag("welcome_guest_button")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag("welcome_developer_attribution")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag("welcome_guest_button")
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()

        assertTrue(guestContinued)
    }

    @Test
    fun settings_has_no_google_signin_and_profile_details_supports_name_avatar_and_adaptive_google_account() {
        var updatedName: String? = null
        var switchedToGuest = false

        composeRule.setContent {
            AmarHisabTheme(darkTheme = false, isBangla = false) {
                SettingsScreen(
                    prefs = UserPrefs(
                        userName = "Shariful Islam",
                        userEmail = "shariful@gmail.com",
                        language = AppLanguage.EN,
                        authMode = AuthMode.GOOGLE
                    ),
                    accounts = emptyList(),
                    backupStatus = null,
                    strings = StringsProvider.english,
                    onBack = {},
                    onEditProfileBudget = {},
                    onLinkGoogleProfile = {},
                    onUnlinkGoogleAccount = { switchedToGuest = true },
                    onOpenAccounts = {},
                    onSetLanguage = {},
                    onSetDarkTheme = {},
                    onSetEnableSubcategories = {},
                    onSetAutoOpenKeypad = {},
                    onSetDefaultAccount = {},
                    onSetStartOfWeek = {},
                    onSetHapticFeedback = {},
                    onSetNotifications = { _, _, _ -> },
                    onUpdateDashboardLayout = { _, _, _ -> },
                    onSetAppLock = { _, _ -> },
                    onSetBiometricUnlock = {},
                    onLockNow = {},
                    onExportJson = {},
                    onExportCsv = {},
                    onRestoreJson = {},
                    onClearBackupStatus = {},
                    onUpdateDisplayName = { updatedName = it }
                )
            }
        }

        composeRule.waitForIdle()

        // 1. Settings Profile summary card: shows avatar, name, signed-in email — NO Google Sign-In button
        composeRule.onNodeWithTag("settings_profile_card").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_profile_google_signin_button").assertDoesNotExist()
        composeRule.onNodeWithTag("settings_profile_signed_in_email").assertIsDisplayed()

        // 2. Open Profile Details -> verify avatar edit icon, Display Name edit, adaptive Google Account card, and Region & Currencies
        composeRule.onNodeWithTag("settings_open_detailed_profile_button")
            .assertIsDisplayed()
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("detailed_profile_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("detailed_profile_avatar_edit_button").assertIsDisplayed()

        composeRule.onNodeWithTag("detailed_profile_list")
            .performScrollToNode(hasTestTag("detailed_profile_user_info_card"))
        composeRule.onNodeWithTag("detailed_profile_edit_name_button")
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("detailed_profile_name_input")
            .performTextReplacement("MD Shariful Islam")
        composeRule.onNodeWithTag("detailed_profile_name_save_button")
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()
        assertEquals("MD Shariful Islam", updatedName)

        composeRule.onNodeWithTag("detailed_profile_list")
            .performScrollToNode(hasTestTag("detailed_profile_google_card"))
        composeRule.onNodeWithTag("detailed_profile_google_card").assertIsDisplayed()
        composeRule.onNodeWithTag("detailed_profile_unlink_google_button")
            .assertIsDisplayed()
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()
        assertTrue(switchedToGuest)

        composeRule.onNodeWithTag("detailed_profile_list")
            .performScrollToNode(hasTestTag("detailed_profile_region_currency_card"))
        composeRule.onNodeWithTag("detailed_profile_region_currency_card").assertIsDisplayed()
    }

    @Test
    fun backup_restore_subpage_renders_local_export_share_import_and_transfer_guide_without_oauth() {
        var exportJsonClicked = false
        var shareJsonClicked = false
        var importClicked = false

        composeRule.setContent {
            AmarHisabTheme(darkTheme = false, isBangla = true) {
                BackupRestoreSubPage(
                    prefs = UserPrefs(
                        language = AppLanguage.BN,
                        authMode = AuthMode.GUEST
                    ),
                    backupStatus = null,
                    isGoogleSignInLoading = false,
                    googleNotice = null,
                    strings = StringsProvider.bangla,
                    onBack = {},
                    onExportJsonClick = { exportJsonClicked = true },
                    onExportCsvClick = {},
                    onImportRestoreClick = { importClicked = true },
                    onDownloadSampleTemplateClick = {},
                    onTriggerGoogleSignIn = {},
                    onDeleteAllFinancialData = {},
                    onClearBackupStatus = {},
                    onShareBackupJson = { shareJsonClicked = true }
                )
            }
        }

        composeRule.onNodeWithTag("backup_export_json_button")
            .assertIsDisplayed()
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.onNodeWithTag("backup_share_json_button")
            .assertIsDisplayed()
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.onNodeWithTag("backup_import_restore_button")
            .assertIsDisplayed()
            .performSemanticsAction(SemanticsActions.OnClick)

        composeRule.onNodeWithTag("backup_restore_list")
            .performScrollToNode(hasTestTag("backup_transfer_guide_card"))
        composeRule.onNodeWithTag("backup_transfer_guide_card").assertIsDisplayed()
        composeRule.onNodeWithTag("backup_guide_export_button").assertIsDisplayed()
        composeRule.onNodeWithTag("backup_guide_import_button").assertIsDisplayed()
        composeRule.waitForIdle()

        assertTrue(exportJsonClicked)
        assertTrue(shareJsonClicked)
        assertTrue(importClicked)
    }

    @Test
    fun local_json_backup_export_and_import_round_trip_restores_accounts_transactions_and_prefs() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val prefsRepo = UserPreferencesRepository(context)
        try {
            prefsRepo.updateDisplayName("তানভীর আহমেদ")
            db.accountDao().insertAccount(
                AccountEntity(
                    id = 21L,
                    name = "KFH Kuwait Wallet",
                    type = AccountType.DEBIT_BANK,
                    balance = 145.500,
                    bankOrProvider = "Kuwait Finance House",
                    colorArgb = 0xFF059669L,
                    currencyCode = "KWD"
                )
            )
            db.transactionDao().insertTransaction(
                TransactionEntity(
                    id = 201L,
                    title = "Supermarket",
                    amount = 12.250,
                    type = TransactionType.EXPENSE,
                    category = ExpenseCategory.GROCERIES,
                    accountId = 21L,
                    note = "Local JSON round-trip",
                    timestamp = 1_763_800_000_000L,
                    currencyCode = "KWD"
                )
            )

            val exportedJson = BackupManager.exportJson(db, prefsRepo.userPrefs.first())
            assertTrue(exportedJson.contains("KFH Kuwait Wallet"))
            assertTrue(exportedJson.contains("Supermarket"))

            BackupManager.clearAllFinancialData(db)
            assertTrue(db.accountDao().getAllAccountsOnce().isEmpty())
            assertTrue(db.transactionDao().getAllTransactionsOnce().isEmpty())

            BackupManager.restoreFromJson(exportedJson, db, prefsRepo)

            val restoredAccounts = db.accountDao().getAllAccountsOnce()
            val restoredTxs = db.transactionDao().getAllTransactionsOnce()
            assertEquals(1, restoredAccounts.size)
            assertEquals("KFH Kuwait Wallet", restoredAccounts.first().name)
            assertEquals("KWD", restoredAccounts.first().currencyCode)
            assertEquals(145.500, restoredAccounts.first().balance, 0.001)
            assertEquals(1, restoredTxs.size)
            assertEquals("Supermarket", restoredTxs.first().title)
            assertEquals(12.250, restoredTxs.first().amount, 0.001)
        } finally {
            db.close()
        }
    }

    @Test
    fun cold_launch_main_viewmodel_and_main_activity_start_without_crashing() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()

        // 1. Direct MainViewModel construction (reproduces the exact init {} property-order NPE)
        val viewModel = com.example.ui.viewmodel.MainViewModel(app)
        org.robolectric.shadows.ShadowLooper.idleMainLooper()
        assertNotNull(viewModel.uiState.value)
        assertNotNull(viewModel.driveSyncState.value)
        assertNotNull(viewModel.marketSyncState.value)

        // 2. Full MainActivity lifecycle (onCreate -> onStart -> onResume -> Compose first frame)
        val controller = org.robolectric.Robolectric.buildActivity(MainActivity::class.java)
            .create()
            .start()
            .resume()
            .visible()
        org.robolectric.shadows.ShadowLooper.idleMainLooper()
        val activity = controller.get()
        assertNotNull(activity)
        assertFalse(activity.isFinishing)
        controller.pause().stop().destroy()
    }

    @Test
    fun oauth_error_403_and_offline_return_clear_bangla_feedback_without_breaking_local_app() = runBlocking {
        // 1. GoogleAuthManager classifies Error 403 / access_denied and provides calm Bangla feedback
        val reason403 = com.example.core.GoogleAuthManager.classifyErrorReason(
            "Error 403: access_denied — app is currently unverified"
        )
        assertEquals(com.example.core.GoogleSignInErrorReason.OAUTH_RESTRICTED_403, reason403)
        val bnMsg403 = com.example.core.GoogleAuthManager.friendlyErrorMessage(reason403, AppLanguage.BN)
        assertTrue(bnMsg403.contains("403"))
        assertTrue(bnMsg403.contains("লোকালি"))

        // 2. GoogleDriveAuthHelper classifies Error 403 & offline cleanly
        val driveKind403 = com.example.data.backup.GoogleDriveAuthHelper.classifyAuthException(
            RuntimeException("HTTP 403 access_denied: org_internal")
        )
        assertEquals(com.example.data.backup.DriveAuthErrorKind.OAUTH_RESTRICTED_403, driveKind403)

        val driveKindOffline = com.example.data.backup.GoogleDriveAuthHelper.classifyAuthException(
            java.io.IOException("Unable to resolve host www.googleapis.com")
        )
        assertEquals(com.example.data.backup.DriveAuthErrorKind.OFFLINE, driveKindOffline)

        // 3. DriveSyncCoordinator returns Restricted403 when Drive API responds with HTTP 403
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val prefsRepo = UserPreferencesRepository(context)
        val restrictedTransport = object : DriveCloudTransport {
            override suspend fun findBackupFolder(accessToken: String): String? {
                throw com.example.data.backup.DriveAccessRestrictedException("HTTP 403 access_denied")
            }

            override suspend fun createBackupFolder(accessToken: String): String = error("unused")
            override suspend fun findLatestVaultFile(accessToken: String, folderId: String): DriveFileMetadata? = null
            override suspend fun uploadVaultFile(
                accessToken: String,
                folderId: String,
                existingFileId: String?,
                encryptedPayload: String
            ): String = error("unused")

            override suspend fun downloadVaultFile(accessToken: String, fileId: String): String = error("unused")
        }
        try {
            val coordinator = DriveSyncCoordinator(GoogleDriveBackupService(restrictedTransport))
            val outcome = coordinator.autoRestoreOrInitialSync(
                accessToken = "token",
                userEmail = "user@gmail.com",
                db = db,
                prefsRepo = prefsRepo
            )
            assertTrue(outcome is DriveAutoRestoreOutcome.Restricted403)
        } finally {
            db.close()
        }
    }

    @Test
    fun settings_removes_currency_tiles_and_profile_details_has_interactive_country_currency_pickers_and_logout() {
        var currentPrefs by androidx.compose.runtime.mutableStateOf(
            UserPrefs(
                userName = "MD Shariful Islam",
                userEmail = "shariful@gmail.com",
                language = AppLanguage.EN,
                authMode = AuthMode.GOOGLE,
                userCountryCode = "BD",
                mainCurrencyCode = "BDT",
                secondaryCurrencyCode = ""
            )
        )
        var loggedOut = false

        composeRule.setContent {
            AmarHisabTheme(darkTheme = false, isBangla = false) {
                SettingsScreen(
                    prefs = currentPrefs,
                    accounts = emptyList(),
                    backupStatus = null,
                    strings = StringsProvider.english,
                    onBack = {},
                    onEditProfileBudget = {},
                    onLinkGoogleProfile = {},
                    onUnlinkGoogleAccount = {},
                    onOpenAccounts = {},
                    onSetLanguage = {},
                    onSetDarkTheme = {},
                    onSetEnableSubcategories = {},
                    onSetAutoOpenKeypad = {},
                    onSetDefaultAccount = {},
                    onSetStartOfWeek = {},
                    onSetHapticFeedback = {},
                    onSetNotifications = { _, _, _ -> },
                    onUpdateDashboardLayout = { _, _, _ -> },
                    onSetAppLock = { _, _ -> },
                    onSetBiometricUnlock = {},
                    onLockNow = {},
                    onExportJson = {},
                    onExportCsv = {},
                    onRestoreJson = {},
                    onClearBackupStatus = {},
                    onSetUserCountry = { code -> currentPrefs = currentPrefs.copy(userCountryCode = code) },
                    onSetMainCurrency = { code -> currentPrefs = currentPrefs.copy(mainCurrencyCode = code) },
                    onSetSecondaryCurrency = { code -> currentPrefs = currentPrefs.copy(secondaryCurrencyCode = code) },
                    onLogOut = { loggedOut = true }
                )
            }
        }

        composeRule.waitForIdle()

        // 1. Settings > Wallet, Currency & Data must NOT duplicate currency pickers
        composeRule.onNodeWithTag("settings_list")
            .performScrollToNode(hasTestTag("settings_backup_restore_tile"))
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("settings_main_currency_tile").assertDoesNotExist()
        composeRule.onNodeWithTag("settings_secondary_currency_tile").assertDoesNotExist()

        // 2. Open Profile Details
        composeRule.onNodeWithTag("settings_list")
            .performScrollToNode(hasTestTag("settings_profile_card"))
        composeRule.onNodeWithTag("settings_open_detailed_profile_button")
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()

        // 3. Interactive Country Picker (select Kuwait)
        composeRule.onNodeWithTag("detailed_profile_list")
            .performScrollToNode(hasTestTag("detailed_profile_region_currency_card"))
        composeRule.onNodeWithTag("detailed_profile_country_row")
            .assertIsDisplayed()
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("app_country_picker_sheet").assertIsDisplayed()
        composeRule.onNodeWithTag("app_country_picker_search").performTextReplacement("Kuwait")
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("app_country_option_KW")
            .assertIsDisplayed()
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()
        assertEquals("KW", currentPrefs.userCountryCode)

        // 4. Interactive Main Currency Picker (select KWD)
        composeRule.onNodeWithTag("detailed_profile_main_currency_row")
            .assertIsDisplayed()
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("app_currency_picker_sheet").assertIsDisplayed()
        composeRule.onNodeWithTag("app_currency_picker_search").performTextReplacement("Kuwait")
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("app_currency_option_KWD")
            .assertIsDisplayed()
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()
        assertEquals("KWD", currentPrefs.mainCurrencyCode)

        // 5. Interactive Second Currency Picker (select SAR, then clear)
        composeRule.onNodeWithTag("detailed_profile_secondary_currency_row")
            .assertIsDisplayed()
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("app_currency_picker_sheet").assertIsDisplayed()
        composeRule.onNodeWithTag("app_currency_picker_search").performTextReplacement("Saudi")
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("app_currency_option_SAR")
            .assertIsDisplayed()
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()
        assertEquals("SAR", currentPrefs.secondaryCurrencyCode)

        composeRule.onNodeWithTag("detailed_profile_secondary_currency_row")
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("app_currency_picker_clear_btn")
            .assertIsDisplayed()
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()
        assertEquals("", currentPrefs.secondaryCurrencyCode)

        // 6. Scroll to very bottom -> dedicated "Log Out" button
        composeRule.onNodeWithTag("detailed_profile_list")
            .performScrollToNode(hasTestTag("detailed_profile_logout_button"))
        composeRule.onNodeWithTag("detailed_profile_logout_button")
            .assertIsDisplayed()
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()
        assertTrue(loggedOut)
    }

    @Test
    fun expanded_bank_and_mfs_directories_and_non_destructive_logout_persistence() = runBlocking {
        // 1. Verify exact expanded Bangladeshi bank directory (22 total: 5 Govt + 17 Private)
        val banks = com.example.data.model.BangladeshiBank.entries
        assertEquals(22, banks.size)
        val codes = banks.map { it.shortCode }.toSet()
        val expectedBankCodes = setOf(
            "CITY", "DBBL", "TRUST", "IBBL", "SONALI", "BRAC",
            "JANATA", "AGRANI", "RUPALI", "BKB",
            "EBL", "PRIME", "MTB", "DHAKA", "PUBALI", "BANKASIA",
            "SEBL", "ONE", "UTTARA", "JAMUNA", "UCB", "MBL"
        )
        assertEquals(expectedBankCodes, codes)
        assertEquals(5, banks.count { it.category == com.example.data.model.BankCategory.GOVERNMENT })
        assertEquals(17, banks.count { it.category == com.example.data.model.BankCategory.PRIVATE })

        // 2. Verify exact expanded MFS provider directory (10 total)
        val mfsProviders = com.example.data.model.MfsProvider.entries
        assertEquals(10, mfsProviders.size)
        val mfsLabelsEn = mfsProviders.map { it.directoryLabel(AppLanguage.EN) }.toSet()
        assertTrue(mfsLabelsEn.contains("bKash"))
        assertTrue(mfsLabelsEn.contains("Nagad"))
        assertTrue(mfsLabelsEn.contains("Rocket (DBBL)"))
        assertTrue(mfsLabelsEn.contains("Upay (UCB)"))
        assertTrue(mfsLabelsEn.contains("Tap (Trust Axiata Pay)"))
        assertTrue(mfsLabelsEn.contains("OK Wallet (One Bank)"))
        assertTrue(mfsLabelsEn.contains("mCash (Islami Bank)"))
        assertTrue(mfsLabelsEn.contains("SureCash"))
        assertTrue(mfsLabelsEn.contains("MyCash (Mercantile Bank)"))
        assertTrue(mfsLabelsEn.contains("Islamic Wallet (Islami Bank)"))

        // 3. Verify country/currency persistence and non-destructive logout preserving Room financial records
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val prefsRepo = UserPreferencesRepository(context)
        try {
            prefsRepo.linkGoogleAccount("MD Shariful Islam", "shariful@gmail.com", "")
            prefsRepo.setUserCountryCode("KW")
            prefsRepo.setMainCurrency("KWD")
            prefsRepo.setSecondaryCurrency("BDT")

            db.accountDao().insertAccount(
                AccountEntity(
                    id = 55L,
                    name = "NBK Salary",
                    type = AccountType.DEBIT_BANK,
                    balance = 5500.0,
                    bankOrProvider = "National Bank of Kuwait",
                    colorArgb = 0xFF003B73L,
                    currencyCode = "KWD"
                )
            )

            val savedBeforeLogout = prefsRepo.userPrefs.first()
            assertEquals("KW", savedBeforeLogout.userCountryCode)
            assertEquals("KWD", savedBeforeLogout.mainCurrencyCode)
            assertEquals("BDT", savedBeforeLogout.secondaryCurrencyCode)
            assertTrue(savedBeforeLogout.hasCompletedWelcome)

            prefsRepo.logOut()

            val afterLogout = prefsRepo.userPrefs.first()
            assertFalse("Log out must return user to startup welcome screen", afterLogout.hasCompletedWelcome)
            assertEquals(AuthMode.GUEST, afterLogout.authMode)
            assertEquals("", afterLogout.userEmail)
            // Country & currency preferences and local Room records stay intact!
            assertEquals("KW", afterLogout.userCountryCode)
            assertEquals("KWD", afterLogout.mainCurrencyCode)
            assertEquals(1, db.accountDao().getAllAccountsOnce().size)
            assertEquals(5500.0, db.accountDao().getAllAccountsOnce().first().balance, 0.001)
        } finally {
            db.close()
        }
    }
}
