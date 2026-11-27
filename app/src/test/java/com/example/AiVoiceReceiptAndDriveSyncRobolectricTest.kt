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
import androidx.compose.ui.test.onNodeWithText
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
    fun smart_local_voice_parser_fixes_income_food_dining_and_kuwait_dinar_dbbl_transfer_mapping() {
        val accounts = listOf(
            AccountEntity(
                id = 1L,
                name = "bKash Personal",
                type = AccountType.MFS,
                balance = 8500.0,
                bankOrProvider = "bKash",
                colorArgb = 0xFFE2136EL,
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
                name = "Upay Wallet",
                type = AccountType.MFS,
                balance = 1200.0,
                bankOrProvider = "Upay",
                colorArgb = 0xFFF59E0BL,
                currencyCode = "BDT"
            ),
            AccountEntity(
                id = 4L,
                name = "ABK Kuwait Card",
                type = AccountType.DEBIT_BANK,
                balance = 950.0,
                bankOrProvider = "Al Ahli Bank of Kuwait",
                colorArgb = 0xFF00529BL,
                currencyCode = "KWD"
            )
        )

        // 1. Income vs Expense classification for incoming Bangla & English phrases
        val bkashDhukeche = LocalVoiceTransactionParser.parse(
            utterance = "আমার বিকাশে ৫০০০ টাকা ঢুকেছে",
            accounts = accounts,
            language = AppLanguage.BN,
            defaultCurrencyCode = "BDT"
        )
        assertEquals(TransactionType.INCOME, bkashDhukeche.type)
        assertEquals(5000.0, bkashDhukeche.amountBdt, 0.001)
        assertEquals("BDT", bkashDhukeche.currencyCode)
        assertEquals(1L, LocalVoiceTransactionParser.matchAccountByHint(accounts, bkashDhukeche.accountHint, "BDT")?.id)

        val peyechiIncome = LocalVoiceTransactionParser.parse(
            utterance = "বিকাশে ২০০০ টাকা পেয়েছি",
            accounts = accounts,
            language = AppLanguage.BN
        )
        assertEquals(TransactionType.INCOME, peyechiIncome.type)
        assertEquals(2000.0, peyechiIncome.amountBdt, 0.001)

        val jomaIncome = LocalVoiceTransactionParser.parse(
            utterance = "ডাচ বাংলা একাউন্টে ৩৫০০০ টাকা জমা হয়েছে",
            accounts = accounts,
            language = AppLanguage.BN
        )
        assertEquals(TransactionType.INCOME, jomaIncome.type)
        assertEquals(2L, LocalVoiceTransactionParser.matchAccountByHint(accounts, jomaIncome.accountHint, "BDT")?.id)

        val upayIncome = LocalVoiceTransactionParser.parse(
            utterance = "Upay তে ৫০০ টাকা ঢুকেছে",
            accounts = accounts,
            language = AppLanguage.BN
        )
        assertEquals("Upay must not falsely trigger 'pay' expense keyword", TransactionType.INCOME, upayIncome.type)
        assertEquals(3L, LocalVoiceTransactionParser.matchAccountByHint(accounts, upayIncome.accountHint, "BDT")?.id)

        val englishCredited = LocalVoiceTransactionParser.parse(
            utterance = "Received 120 KD salary credited to ABK",
            accounts = accounts,
            language = AppLanguage.EN
        )
        assertEquals(TransactionType.INCOME, englishCredited.type)
        assertEquals("KWD", englishCredited.currencyCode)
        assertEquals(4L, LocalVoiceTransactionParser.matchAccountByHint(accounts, englishCredited.accountHint, "KWD")?.id)

        // 2. Category accuracy: Food & Dining vs Groceries
        val lunchExpense = LocalVoiceTransactionParser.parse(
            utterance = "দুপুরের খাবার ২০০ টাকা বিকাশ থেকে",
            accounts = accounts,
            language = AppLanguage.BN
        )
        assertEquals(TransactionType.EXPENSE, lunchExpense.type)
        assertEquals(ExpenseCategory.FOOD_DINING, lunchExpense.category)

        val hotelMeal = LocalVoiceTransactionParser.parse(
            utterance = "হোটেলে মাছ ভাত খেয়েছি ২৫০ টাকা",
            accounts = accounts,
            language = AppLanguage.BN
        )
        assertEquals(TransactionType.EXPENSE, hotelMeal.type)
        assertEquals(ExpenseCategory.FOOD_DINING, hotelMeal.category)

        val dinnerEn = LocalVoiceTransactionParser.parse(
            utterance = "Dinner at restaurant 4.5 KD from ABK",
            accounts = accounts,
            language = AppLanguage.EN
        )
        assertEquals(TransactionType.EXPENSE, dinnerEn.type)
        assertEquals(ExpenseCategory.FOOD_DINING, dinnerEn.category)
        assertEquals("KWD", dinnerEn.currencyCode)

        val teaSnacks = LocalVoiceTransactionParser.parse(
            utterance = "চা নাস্তা খেয়েছি ৮০ টাকা",
            accounts = accounts,
            language = AppLanguage.BN
        )
        assertEquals(ExpenseCategory.FOOD_DINING, teaSnacks.category)

        val groceryBazar = LocalVoiceTransactionParser.parse(
            utterance = "কাঁচাবাজার করেছি ৮০০ টাকা",
            accounts = accounts,
            language = AppLanguage.BN
        )
        assertEquals(ExpenseCategory.GROCERIES, groceryBazar.category)

        val mobileRecharge = LocalVoiceTransactionParser.parse(
            utterance = "মোবাইল রিচার্জ ২০০ টাকা",
            accounts = accounts,
            language = AppLanguage.BN
        )
        assertEquals(ExpenseCategory.RENT_UTILITIES, mobileRecharge.category)

        // 3. Cross-border Kuwait -> Dutch-Bangla 500 Dinar transfer detection, KD currency & exact From/To mapping
        val kuwaitToDbbl = LocalVoiceTransactionParser.parse(
            utterance = "আমি কুয়েত থেকে বাংলাদেশের ডাচ বাংলা একাউন্টে ৫০০ দিনার পাঠিয়েছি",
            accounts = accounts,
            language = AppLanguage.BN,
            defaultCurrencyCode = "BDT"
        )
        assertEquals(TransactionType.TRANSFER, kuwaitToDbbl.type)
        assertEquals(500.0, kuwaitToDbbl.amountBdt, 0.001)
        assertEquals("KWD", kuwaitToDbbl.currencyCode)
        assertEquals(ExpenseCategory.TRANSFER, kuwaitToDbbl.category)
        val matchedTo = LocalVoiceTransactionParser.matchAccountByHint(accounts, kuwaitToDbbl.toAccountHint)
        val matchedFrom = LocalVoiceTransactionParser.matchAccountByHint(
            accounts = accounts,
            hint = kuwaitToDbbl.accountHint,
            preferredCurrency = kuwaitToDbbl.currencyCode,
            excludeAccountId = matchedTo?.id
        )
        assertEquals("Destination must be Dutch-Bangla Savings", 2L, matchedTo?.id)
        assertEquals("Source must be ABK Kuwait Card (KWD)", 4L, matchedFrom?.id)

        // Unlisted bank hint must NOT fall back to unrelated bKash BDT wallet
        val unlistedMatch = LocalVoiceTransactionParser.matchAccountByHint(accounts, "City Bank")
        assertEquals(null, unlistedMatch)

        // 4. End-to-end VoiceEntrySheet UI check for Kuwait -> DBBL 500 Dinar transfer
        var executedFromId = -1L
        var executedToId = -1L
        var executedAmount = 0.0
        composeRule.setContent {
            AmarHisabTheme(darkTheme = false, isBangla = true) {
                VoiceEntrySheet(
                    accounts = accounts,
                    voiceState = VoiceParseUiState.Success(kuwaitToDbbl),
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
                        executedFromId = fromId
                        executedToId = toId
                        executedAmount = amount
                    }
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("voice_save_button")
            .performScrollTo()
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()

        assertEquals(4L, executedFromId)
        assertEquals(2L, executedToId)
        assertEquals(500.0, executedAmount, 0.001)
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

        // 1. Settings Profile summary card: shows avatar, name, country/currency pills — NO email line and NO Google Sign-In button
        composeRule.onNodeWithTag("settings_profile_card").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_profile_google_signin_button").assertDoesNotExist()
        composeRule.onNodeWithTag("settings_profile_signed_in_email").assertDoesNotExist()
        composeRule.onNodeWithText("shariful@gmail.com").assertDoesNotExist()

        // 2. Open Profile Details -> verify avatar edit icon, Display Name without duplicate email pill under name, and email kept inside Google Account card
        composeRule.onNodeWithTag("settings_open_detailed_profile_button")
            .assertIsDisplayed()
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("detailed_profile_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("detailed_profile_avatar_edit_button").assertIsDisplayed()
        composeRule.onNodeWithTag("detailed_profile_hero_name").assertIsDisplayed()
        composeRule.onNodeWithTag("detailed_profile_user_info_card").assertDoesNotExist()

        composeRule.onNodeWithTag("detailed_profile_edit_name_button")
            .assertIsDisplayed()
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("detailed_profile_edit_name_dialog").assertIsDisplayed()
        composeRule.onNodeWithTag("detailed_profile_name_input")
            .performTextReplacement("MD Shariful Islam")
        composeRule.onNodeWithTag("detailed_profile_name_save_button")
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()
        assertEquals("MD Shariful Islam", updatedName)

        composeRule.onNodeWithTag("detailed_profile_list")
            .performScrollToNode(hasTestTag("detailed_profile_google_card"))
        composeRule.onNodeWithTag("detailed_profile_google_card").assertIsDisplayed()
        composeRule.onNodeWithTag("detailed_profile_google_email").assertIsDisplayed()
        composeRule.onNodeWithText("shariful@gmail.com").assertIsDisplayed()
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
            .performScrollToNode(hasTestTag("settings_wallet_currency_data_tile"))
        composeRule.onNodeWithTag("settings_wallet_currency_data_tile")
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("settings_section_list")
            .performScrollToNode(hasTestTag("settings_backup_restore_tile"))
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("settings_main_currency_tile").assertDoesNotExist()
        composeRule.onNodeWithTag("settings_secondary_currency_tile").assertDoesNotExist()
        composeRule.onNodeWithTag("settings_section_back_button")
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()

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

    @Test
    fun firestore_central_store_payload_mappers_match_security_rules_schema_and_decoupled_ai_has_no_appcheck() {
        // 1. Verify GeminiAiService is decoupled from Firebase AI Logic & handles unset key cleanly
        val aiService = com.example.data.ai.GeminiAiService()
        assertNotNull(aiService)

        // 2. Verify FirestorePayloadMapper emits valid rule-aligned documents without "id" or null keys
        val uid = "uid_shariful_001"
        val account = AccountEntity(
            id = 7L,
            name = "Trust Bank Payroll",
            type = AccountType.DEBIT_BANK,
            balance = 42000.0,
            creditLimit = 0.0,
            bankOrProvider = "Trust Bank",
            accountSubtype = "Savings",
            cardNetwork = "Visa",
            accountNumberLast4 = "9012",
            colorArgb = 0xFF0284C7L,
            currencyCode = "BDT",
            createdAt = 1_763_800_000_000L
        )
        val accCreate = com.example.data.firebase.FirestorePayloadMapper.accountCreateMap(uid, account)
        assertEquals("acc_7", com.example.data.firebase.FirestorePayloadMapper.accountDocId(account.id))
        assertFalse("Firestore create payload must never include 'id' key", accCreate.containsKey("id"))
        assertEquals(uid, accCreate["userId"])
        assertEquals(7L, accCreate["localId"])
        assertEquals("DEBIT_BANK", accCreate["type"])
        assertTrue(accCreate.containsKey("createdAt"))
        assertTrue(accCreate.containsKey("updatedAt"))

        val accUpdate = com.example.data.firebase.FirestorePayloadMapper.accountUpdateMap(account)
        assertFalse("Update map must not mutate immutable userId", accUpdate.containsKey("userId"))
        assertFalse("Update map must not mutate immutable createdAt", accUpdate.containsKey("createdAt"))
        assertTrue(accUpdate.containsKey("updatedAt"))

        // 3. Verify optional null fields (toAccountLocalId) are stripped for expense, kept for transfer
        val expenseTx = TransactionEntity(
            id = 12L,
            title = "Bazaar",
            amount = 1500.0,
            type = TransactionType.EXPENSE,
            category = ExpenseCategory.GROCERIES,
            accountId = 7L,
            toAccountId = null,
            note = "Voice entry",
            currencyCode = "BDT",
            timestamp = 1_763_800_100_000L
        )
        val expenseMap = com.example.data.firebase.FirestorePayloadMapper.transactionCreateMap(uid, expenseTx)
        assertFalse(expenseMap.containsKey("id"))
        assertFalse("Optional null toAccountLocalId must be omitted", expenseMap.containsKey("toAccountLocalId"))
        assertEquals(1500.0, expenseMap["amount"])

        val transferTx = expenseTx.copy(
            id = 13L,
            type = TransactionType.TRANSFER,
            category = ExpenseCategory.TRANSFER,
            toAccountId = 8L
        )
        val transferMap = com.example.data.firebase.FirestorePayloadMapper.transactionCreateMap(uid, transferTx)
        assertEquals(8L, transferMap["toAccountLocalId"])
    }

    @Test
    fun unified_195_country_dataset_and_major_banks_are_synchronized_across_currency_and_card_wizard() {
        val countries = com.example.data.model.AppCountry.entries
        assertEquals("Unified dataset must contain all 195 sovereign countries", 195, countries.size)
        assertEquals("Every country ISO code must be unique", 195, countries.map { it.code }.toSet().size)
        assertEquals(7, com.example.data.model.CountryRegionGroup.entries.size)
        assertTrue("Expected at least 140 distinct world currencies", com.example.data.model.AppCurrency.entries.size >= 140)

        // 1. Verify all 28 core expatriate/neighboring + global countries map to exact currencies
        val expectedCoreCountryToCurrency = mapOf(
            // South Asia & Neighboring Countries
            "BD" to "BDT",
            "IN" to "INR",
            "NP" to "NPR",
            "LK" to "LKR",
            "BT" to "BTN",
            "MM" to "MMK",
            "MV" to "MVR",
            // Middle East
            "SA" to "SAR",
            "AE" to "AED",
            "KW" to "KWD",
            "QA" to "QAR",
            "OM" to "OMR",
            "BH" to "BHD",
            // Asia-Pacific & Oceania
            "MY" to "MYR",
            "SG" to "SGD",
            "KR" to "KRW",
            "JP" to "JPY",
            "AU" to "AUD",
            // North America & UK
            "US" to "USD",
            "GB" to "GBP",
            "CA" to "CAD",
            // Europe & Cyprus
            "IT" to "EUR",
            "ES" to "EUR",
            "PT" to "EUR",
            "GR" to "EUR",
            "DE" to "EUR",
            "FR" to "EUR",
            "CY" to "EUR",
            // Expanded global 195-country coverage spot checks
            "TR" to "TRY",
            "PH" to "PHP",
            "BR" to "BRL",
            "EG" to "EGP",
            "ID" to "IDR",
            "CN" to "CNY",
            "NG" to "NGN",
            "ZA" to "ZAR",
            "PS" to "ILS",
            "VA" to "EUR"
        )
        val actualMap = countries.associate { it.code to it.suggestedCurrency.code }
        expectedCoreCountryToCurrency.forEach { (iso, cur) ->
            assertEquals("Mismatch for country $iso", cur, actualMap[iso])
        }

        // Verify live/offline conversion works for ALL 195 countries' currencies (zero NaN/0)
        val snapshot = com.example.data.market.MarketSnapshot.defaultReference()
        com.example.data.model.AppCurrency.entries.forEach { cur ->
            val converted = snapshot.convertCurrencyCode(100.0, cur.code, "BDT")
            assertTrue("Currency ${cur.code} must convert positively to BDT", converted > 0.0 && converted.isFinite())
        }

        // 2. Verify dynamic country headings
        assertEquals("Select Bangladeshi Bank", com.example.data.model.AppCountry.BD.bankSelectionHeading(AppLanguage.EN))
        assertEquals("Select Kuwait Bank", com.example.data.model.AppCountry.KW.bankSelectionHeading(AppLanguage.EN))
        assertEquals("Select Saudi Arabia Bank", com.example.data.model.AppCountry.SA.bankSelectionHeading(AppLanguage.EN))
        assertEquals("Select India Bank", com.example.data.model.AppCountry.IN.bankSelectionHeading(AppLanguage.EN))

        // 3. Verify every quick-select expatriate country has major preset banks in InternationalBankCatalog
        com.example.data.model.AppCountry.quickSelectCountries.forEach { country ->
            val banks = com.example.data.model.InternationalBankCatalog.banksForCountry(country)
            assertTrue("Country ${country.nameEn} must provide major banks", banks.isNotEmpty())
        }

        // 4. Verify exact requested major bank coverage per country
        assertEquals(22, com.example.data.model.InternationalBankCatalog.banksForCountry(com.example.data.model.AppCountry.BD).size)

        val kuwaitNames = com.example.data.model.InternationalBankCatalog.banksForCountry(com.example.data.model.AppCountry.KW).map { it.en }
        assertTrue(kuwaitNames.any { it.contains("ABK") })
        assertTrue(kuwaitNames.any { it.contains("NBK") })
        assertTrue(kuwaitNames.any { it.contains("KFH") })
        assertTrue(kuwaitNames.any { it.contains("Gulf Bank") })
        assertTrue(kuwaitNames.any { it.contains("Boubyan") })
        assertTrue(kuwaitNames.any { it.contains("Burgan") })
        assertTrue(kuwaitNames.any { it.contains("Al Tijari") })

        val saudiNames = com.example.data.model.InternationalBankCatalog.banksForCountry(com.example.data.model.AppCountry.SA).map { it.en }
        assertTrue(saudiNames.any { it.contains("Al Rajhi") })
        assertTrue(saudiNames.any { it.contains("SNB") })
        assertTrue(saudiNames.any { it.contains("Riyad Bank") })
        assertTrue(saudiNames.any { it.contains("Alinma") })
        assertTrue(saudiNames.any { it.contains("ANB") })
        assertTrue(saudiNames.any { it.contains("BSF") })
        assertTrue(saudiNames.any { it.contains("SAB") })

        assertTrue(com.example.data.model.InternationalBankCatalog.banksForCountry(com.example.data.model.AppCountry.AE).size >= 5)
        assertTrue(com.example.data.model.InternationalBankCatalog.banksForCountry(com.example.data.model.AppCountry.QA).size >= 5)
        assertTrue(com.example.data.model.InternationalBankCatalog.banksForCountry(com.example.data.model.AppCountry.OM).size >= 4)
        assertTrue(com.example.data.model.InternationalBankCatalog.banksForCountry(com.example.data.model.AppCountry.BH).size >= 3)
        assertTrue(com.example.data.model.InternationalBankCatalog.banksForCountry(com.example.data.model.AppCountry.MY).size >= 5)
        assertTrue(com.example.data.model.InternationalBankCatalog.banksForCountry(com.example.data.model.AppCountry.SG).size >= 3)
        assertTrue(com.example.data.model.InternationalBankCatalog.banksForCountry(com.example.data.model.AppCountry.US).size >= 4)
        assertTrue(com.example.data.model.InternationalBankCatalog.banksForCountry(com.example.data.model.AppCountry.GB).size >= 4)
        assertTrue(com.example.data.model.InternationalBankCatalog.banksForCountry(com.example.data.model.AppCountry.CA).size >= 4)
        assertTrue(com.example.data.model.InternationalBankCatalog.banksForCountry(com.example.data.model.AppCountry.IT).size >= 4)
        assertTrue(com.example.data.model.InternationalBankCatalog.banksForCountry(com.example.data.model.AppCountry.CY).any { it.en.contains("Bank of Cyprus") })
        assertTrue(com.example.data.model.InternationalBankCatalog.banksForCountry(com.example.data.model.AppCountry.CY).any { it.en.contains("Hellenic Bank") })
        assertTrue(com.example.data.model.InternationalBankCatalog.banksForCountry(com.example.data.model.AppCountry.DE).any { it.en.contains("Deutsche Bank") })
        assertTrue(com.example.data.model.InternationalBankCatalog.banksForCountry(com.example.data.model.AppCountry.FR).any { it.en.contains("BNP Paribas") })
        assertTrue(com.example.data.model.InternationalBankCatalog.banksForCountry(com.example.data.model.AppCountry.ES).any { it.en.contains("Banco Santander") })
        assertTrue(com.example.data.model.InternationalBankCatalog.banksForCountry(com.example.data.model.AppCountry.AU).size >= 4)
        assertTrue(com.example.data.model.InternationalBankCatalog.banksForCountry(com.example.data.model.AppCountry.JP).size >= 3)
        assertTrue(com.example.data.model.InternationalBankCatalog.banksForCountry(com.example.data.model.AppCountry.KR).size >= 3)
        assertTrue(com.example.data.model.InternationalBankCatalog.banksForCountry(com.example.data.model.AppCountry.MV).any { it.en.contains("Bank of Maldives") })
    }

    @Test
    fun add_account_wizard_switches_countries_shows_dynamic_heading_and_saves_manual_custom_bank_with_card_color() {
        var savedAccount: AccountEntity? = null

        composeRule.setContent {
            AmarHisabTheme(darkTheme = false, isBangla = false) {
                com.example.ui.screens.AddAccountWizardSheet(
                    language = AppLanguage.EN,
                    strings = StringsProvider.english,
                    onDismiss = {},
                    onSaveAccount = { savedAccount = it },
                    defaultCurrencyCode = "BDT",
                    secondaryCurrencyCode = "KWD",
                    userCountryCode = "BD"
                )
            }
        }

        composeRule.waitForIdle()
        // 1. Default country BD shows Bangladeshi Bank heading
        composeRule.onNodeWithTag("wizard_bank_section_heading")
            .performScrollTo()
            .assertIsDisplayed()

        // 2. Switch to Kuwait via synchronized country chip -> dynamic heading updates & Kuwait major banks (ABK) appear
        composeRule.onNodeWithTag("wizard_country_KW")
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("wizard_bank_section_heading")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag("wizard_bank_kw_abk").assertExists()

        // 3. Choose Manual / Custom Bank -> enter unlisted private bank name + choose custom Card Color + balance -> save
        composeRule.onNodeWithTag("wizard_bank_custom")
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("wizard_custom_bank_input")
            .performScrollTo()
            .assertIsDisplayed()
            .performTextReplacement("Al-Noor Private Bank")
        composeRule.onNodeWithTag("wizard_card_color_3")
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.onNodeWithTag("wizard_balance_input")
            .performScrollTo()
            .performTextReplacement("250.500")
        composeRule.onNodeWithTag("wizard_save_account_button")
            .performScrollTo()
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()

        val result = requireNotNull(savedAccount) { "Expected custom bank card to save" }
        assertEquals("Al-Noor Private Bank", result.bankOrProvider)
        assertEquals("KWD", result.currencyCode)
        assertEquals(250.5, result.balance, 0.001)
        val swatchColor = com.example.data.model.CardColorCatalog.swatches[3]
        val expectedLong = ((swatchColor.alpha * 255f + 0.5f).toInt().toLong() shl 24) or
            ((swatchColor.red * 255f + 0.5f).toInt().toLong() shl 16) or
            ((swatchColor.green * 255f + 0.5f).toInt().toLong() shl 8) or
            (swatchColor.blue * 255f + 0.5f).toInt().toLong()
        assertEquals(expectedLong, result.colorArgb)
    }

    @Test
    fun global_language_catalog_has_gboard_suggested_all_indian_languages_rtl_flags_and_persists_across_reloads() = runBlocking {
        // 1. Primary BN & EN plus Gboard-style Suggested / Popular Languages section (single consolidated Bangla)
        assertEquals(listOf(AppLanguage.BN, AppLanguage.EN), AppLanguage.primaryLanguages)
        assertTrue(AppLanguage.suggestedLanguages.containsAll(
            listOf(AppLanguage.BN, AppLanguage.EN, AppLanguage.AR, AppLanguage.HI, AppLanguage.UR, AppLanguage.MS, AppLanguage.IT, AppLanguage.ZH)
        ))
        assertEquals("Bangla must appear only once in AppLanguage", 1, AppLanguage.entries.count { it.isBangla })
        assertTrue("Expected at least 40 curated global & Indian languages", AppLanguage.entries.size >= 40)

        // Verify all requested global, expatriate & major Indian languages exist with native names & codes
        val expectedCodes = setOf(
            // Core & Expatriate / Global
            "bn", "en", "ar", "ur", "ms", "id", "fil", "it", "es", "fr",
            "de", "pt", "zh", "ja", "ko", "tr", "ru", "ne", "si", "fa",
            // Major Indian Languages (Bangla consolidated under single bn entry)
            "hi", "mr", "ta", "te", "gu", "kn", "ml", "pa", "or", "as"
        )
        assertTrue(AppLanguage.entries.map { it.code }.toSet().containsAll(expectedCodes))
        assertEquals("বাংলা", AppLanguage.BN.nativeName)
        assertEquals("Bangladesh • India (West Bengal • Tripura)", AppLanguage.BN.regionHint)
        assertTrue(AppLanguage.BN.isBangla)
        assertEquals(AppLanguage.BN, AppLanguage.fromCode("bn-IN"))

        // Verify Urdu lists Pakistan • South Asia (India • Nepal) without Middle East
        assertTrue(AppLanguage.UR.regionHint.contains("Pakistan • South Asia"))
        assertTrue(AppLanguage.UR.regionHint.contains("India"))
        assertTrue(AppLanguage.UR.regionHint.contains("Nepal"))
        assertFalse(AppLanguage.UR.regionHint.contains("Middle East", ignoreCase = true))
        assertFalse(AppLanguage.UR.searchKeywords.contains("middle east", ignoreCase = true))

        // Verify RTL metadata on Arabic, Urdu & Persian; LTR on BN, EN & Indian languages
        assertTrue(AppLanguage.AR.isRtl)
        assertTrue(AppLanguage.UR.isRtl)
        assertTrue(AppLanguage.FA.isRtl)
        assertFalse(AppLanguage.BN.isRtl)
        assertFalse(AppLanguage.EN.isRtl)
        assertFalse(AppLanguage.MR.isRtl)

        // Verify search matches English name, native script, ISO code, and regional/script variants
        assertTrue(AppLanguage.BN.matchesQuery("West Bengal"))
        assertTrue(AppLanguage.BN.matchesQuery("Tripura"))
        assertTrue(AppLanguage.BN.matchesQuery("ভারত"))
        assertTrue(AppLanguage.BN.matchesQuery("Bangladesh"))
        assertTrue(AppLanguage.UR.matchesQuery("Pakistan"))
        assertTrue(AppLanguage.UR.matchesQuery("South Asia"))
        assertTrue(AppLanguage.AR.matchesQuery("Arabic"))
        assertTrue(AppLanguage.AR.matchesQuery("العربية"))
        assertTrue(AppLanguage.AR.matchesQuery("Kuwait"))
        assertTrue(AppLanguage.HI.matchesQuery("हिन्दी"))
        assertTrue(AppLanguage.MR.matchesQuery("मराठी"))
        assertTrue(AppLanguage.TA.matchesQuery("தமிழ்"))
        assertTrue(AppLanguage.TE.matchesQuery("తెలుగు"))
        assertTrue(AppLanguage.GU.matchesQuery("ગુજરાતી"))
        assertTrue(AppLanguage.KN.matchesQuery("ಕನ್ನಡ"))
        assertTrue(AppLanguage.ML.matchesQuery("മലയാളം"))
        assertTrue(AppLanguage.PA.matchesQuery("ਪੰਜਾਬੀ"))
        assertTrue(AppLanguage.OR.matchesQuery("ଓଡ଼ିଆ"))
        assertTrue(AppLanguage.AS.matchesQuery("অসমীয়া"))
        assertTrue(AppLanguage.IT.matchesQuery("Italy"))
        assertTrue(AppLanguage.BN.matchesQuery("Bangla"))

        // Verify every single language resolves non-blank core, export, and fallback UI labels
        AppLanguage.entries.forEach { lang ->
            val str = StringsProvider.get(lang)
            assertTrue("navHome blank for $lang", str.navHome.isNotBlank())
            assertTrue("settingsTitle blank for $lang", str.settingsTitle.isNotBlank())
            assertTrue("totalBalance blank for $lang", str.totalBalance.isNotBlank())
            assertTrue("saveTransaction blank for $lang", str.saveTransaction.isNotBlank())
            assertTrue("languagePageSubtitle blank for $lang", str.settingsRefinement.languagePageSubtitle.isNotBlank())
            assertTrue("reportExport cardTitle blank for $lang", str.reportExport.cardTitle.isNotBlank())
        }
        assertEquals("الرئيسية", StringsProvider.get(AppLanguage.AR).navHome)
        assertEquals("ہوم", StringsProvider.get(AppLanguage.UR).navHome)
        assertEquals("होम", StringsProvider.get(AppLanguage.HI).navHome)
        assertEquals("முகப்பு", StringsProvider.get(AppLanguage.TA).navHome)
        assertEquals("హోమ్", StringsProvider.get(AppLanguage.TE).navHome)
        assertEquals("ഹോം", StringsProvider.get(AppLanguage.ML).navHome)
        assertEquals(StringsProvider.bangla.navHome, StringsProvider.get(AppLanguage.BN).navHome)

        // 2. Verify DataStore persistence & BackupManager round-trip for global and Indian languages
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val prefsRepo = UserPreferencesRepository(context)
        try {
            prefsRepo.setLanguage(AppLanguage.BN)
            assertEquals(AppLanguage.BN, prefsRepo.userPrefs.first().language)

            prefsRepo.setLanguage(AppLanguage.AR)
            assertEquals(AppLanguage.AR, prefsRepo.userPrefs.first().language)

            prefsRepo.setLanguage(AppLanguage.MR)
            val exported = BackupManager.exportJson(db, prefsRepo.userPrefs.first())
            prefsRepo.setLanguage(AppLanguage.EN)
            assertEquals(AppLanguage.EN, prefsRepo.userPrefs.first().language)

            BackupManager.restoreFromJson(exported, db, prefsRepo)
            assertEquals(AppLanguage.MR, prefsRepo.userPrefs.first().language)
        } finally {
            prefsRepo.setLanguage(AppLanguage.EN)
            db.close()
        }
    }

    @Test
    fun language_selection_screen_supports_gboard_search_switches_rtl_layout_and_returns_to_bn_en() {
        var activeLanguage by mutableStateOf(AppLanguage.EN)
        var observedDirection: androidx.compose.ui.unit.LayoutDirection? = null

        composeRule.setContent {
            AmarHisabTheme(
                darkTheme = false,
                isBangla = activeLanguage == AppLanguage.BN,
                isRtl = activeLanguage.isRtl
            ) {
                observedDirection = androidx.compose.ui.platform.LocalLayoutDirection.current
                com.example.ui.screens.LanguageSelectionSubPage(
                    activeLanguage = activeLanguage,
                    strings = StringsProvider.get(activeLanguage),
                    onSelectLanguage = { activeLanguage = it },
                    onBack = {}
                )
            }
        }

        composeRule.waitForIdle()
        assertEquals(androidx.compose.ui.unit.LayoutDirection.Ltr, observedDirection)

        // 1. Primary options BN & EN visible at top alongside search bar
        composeRule.onNodeWithTag("language_search_input").assertIsDisplayed()
        composeRule.onNodeWithTag("language_option_BN").assertIsDisplayed()
        composeRule.onNodeWithTag("language_option_EN").assertIsDisplayed()
        composeRule.onNodeWithTag("language_check_EN", useUnmergedTree = true).assertIsDisplayed()

        // 2. Search "Arabic" -> select Arabic -> switches immediately to RTL
        composeRule.onNodeWithTag("language_search_input").performTextReplacement("Arabic")
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("language_option_AR")
            .assertIsDisplayed()
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()
        assertEquals(AppLanguage.AR, activeLanguage)
        assertEquals(androidx.compose.ui.unit.LayoutDirection.Rtl, observedDirection)
        composeRule.onNodeWithTag("language_check_AR", useUnmergedTree = true).assertIsDisplayed()

        // 3. Search "اردو" (native script) -> select Urdu -> stays RTL
        composeRule.onNodeWithTag("language_search_input").performTextReplacement("اردو")
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("language_option_UR")
            .assertIsDisplayed()
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()
        assertEquals(AppLanguage.UR, activeLanguage)
        assertEquals(androidx.compose.ui.unit.LayoutDirection.Rtl, observedDirection)

        // 4. Clear search -> select Bangla -> restores LTR cleanly
        composeRule.onNodeWithTag("language_search_clear_button")
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("language_option_BN")
            .assertIsDisplayed()
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()
        assertEquals(AppLanguage.BN, activeLanguage)
        assertEquals(androidx.compose.ui.unit.LayoutDirection.Ltr, observedDirection)
        composeRule.onNodeWithTag("language_check_BN", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun daily_weekly_monthly_receipts_pdfs_and_qr_codes_render_in_active_app_language() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val now = System.currentTimeMillis()
        val accounts = listOf(
            AccountEntity(
                id = 1L,
                name = "NBK Kuwait",
                type = AccountType.DEBIT_BANK,
                balance = 450.0,
                bankOrProvider = "National Bank of Kuwait",
                colorArgb = 0xFF003B73L,
                currencyCode = "KWD"
            )
        )
        val txToday = TransactionEntity(
            id = 101L,
            title = "Grocery Market",
            amount = 18.500,
            type = TransactionType.EXPENSE,
            category = ExpenseCategory.GROCERIES,
            accountId = 1L,
            note = "Fresh fruits",
            currencyCode = "KWD",
            timestamp = now
        )
        val txSalary = TransactionEntity(
            id = 102L,
            title = "Monthly Salary",
            amount = 320.000,
            type = TransactionType.INCOME,
            category = ExpenseCategory.SALARY_INCOME,
            accountId = 1L,
            note = "",
            currencyCode = "KWD",
            timestamp = now - 60_000L
        )

        val languagesToTest = listOf(
            AppLanguage.BN,
            AppLanguage.EN,
            AppLanguage.AR,
            AppLanguage.HI,
            AppLanguage.UR,
            AppLanguage.MR,
            AppLanguage.TA,
            AppLanguage.ML,
            AppLanguage.ZH
        )

        languagesToTest.forEach { lang ->
            val strings = StringsProvider.get(lang)
            val state = com.example.ui.viewmodel.MainUiState(
                prefs = UserPrefs(
                    userName = "MD Shariful Islam",
                    language = lang,
                    mainCurrencyCode = "KWD",
                    userCountryCode = "KW"
                ),
                accounts = accounts,
                transactions = listOf(txToday, txSalary)
            )
            val copy = com.example.data.export.ReportLocalizedStrings.forLanguage(lang)
            val expectedLangTag = lang.code.lowercase(java.util.Locale.US).replace("-", "_")

            com.example.data.export.ReportPeriodType.entries.forEach { periodType ->
                val payload = com.example.data.export.MonthlyReportExporter.buildReportPayload(
                    state = state,
                    periodType = periodType,
                    nowMillis = now
                )
                assertEquals(periodType, payload.periodType)
                assertEquals(lang, payload.language)
                assertTrue(payload.verificationId.startsWith("AH-"))
                assertTrue(
                    "QR payload for $lang/$periodType must contain localized title",
                    payload.qrPayload.contains(copy.reportTitleFor(periodType))
                )
                assertTrue(
                    "QR payload must embed verification ID",
                    payload.qrPayload.contains(payload.verificationId)
                )

                // Verify real scannable QR bitmap contains both dark and light modules
                val bitmap = com.example.data.export.QrCodeBitmapGenerator.generateQrBitmap(
                    content = payload.qrPayload,
                    sizePx = 220
                )
                assertEquals(220, bitmap.width)
                assertEquals(220, bitmap.height)
                var hasDark = false
                var hasLight = false
                for (y in 0 until bitmap.height step 4) {
                    for (x in 0 until bitmap.width step 4) {
                        val px = bitmap.getPixel(x, y)
                        if (px == android.graphics.Color.WHITE) hasLight = true else hasDark = true
                    }
                }
                assertTrue("QR bitmap must contain dark modules for $lang/$periodType", hasDark)
                assertTrue("QR bitmap must contain light modules for $lang/$periodType", hasLight)

                // Export PDF, CSV, and QR PNG in the active language and verify generated files
                listOf(
                    com.example.data.export.ReportExportFormat.PDF,
                    com.example.data.export.ReportExportFormat.CSV,
                    com.example.data.export.ReportExportFormat.QR_PNG
                ).forEach { format ->
                    val result = com.example.data.export.MonthlyReportExporter.exportAndBuildShareIntent(
                        context = context,
                        state = state,
                        format = format,
                        strings = strings,
                        periodType = periodType,
                        nowMillis = now
                    )
                    assertTrue("Export $format failed for $lang/$periodType: ${result.exceptionOrNull()}", result.isSuccess)
                    val exported = result.getOrThrow()
                    assertTrue("Exported file must exist", exported.file.exists())
                    assertTrue("Exported $format file must be non-empty", exported.file.length() > 100L)
                    assertTrue(
                        "Filename should tag active language $expectedLangTag: ${exported.file.name}",
                        exported.file.name.contains("_${expectedLangTag}.")
                    )
                }
            }

            // Also verify individual transaction voucher QR payload in active language
            val voucherQr = com.example.data.export.MonthlyReportExporter.buildTransactionVoucherQrPayload(
                tx = txToday,
                walletLabel = "NBK Kuwait",
                language = lang
            )
            assertTrue(voucherQr.contains(copy.brandTitle))
            assertTrue(voucherQr.contains("Grocery Market"))
        }
    }

    @Test
    fun expandedIncomeAndExpenseCategories_andUnifiedDetectionWorkIdenticallyAcrossAllFlows() {
        val incomeCategories = com.example.data.model.BuiltInCategoriesCatalog.defaultsFor(TransactionType.INCOME)
        val expenseCategories = com.example.data.model.BuiltInCategoriesCatalog.defaultsFor(TransactionType.EXPENSE)

        // 1. Verify all 5 Income Categories
        val expectedIncomeBn = listOf(
            "বেতন ও ভাতা",
            "বোনাস ও ইনসেনটিভ",
            "ব্যবসা ও সাইড ইনকাম",
            "রিফান্ড ও ক্যাশব্যাক",
            "অন্যান্য আয়"
        )
        assertEquals(5, incomeCategories.size)
        expectedIncomeBn.forEach { expected ->
            assertTrue(
                "Income catalog must include '$expected'",
                incomeCategories.any { it.bn == expected }
            )
        }

        // 2. Verify all 12 Expense Categories
        val expectedExpenseBn = listOf(
            "খাবার ও ডাইনিং",
            "বাজার ও গ্রোসারি",
            "বাসা ভাড়া ও আবাসন",
            "ইউটিলিটি ও বিল",
            "যাতায়াত ও পরিবহন",
            "পরিবার ও রেমিট্যান্স",
            "স্বাস্থ্য ও চিকিৎসা",
            "কেনাকাটা ও পোশাক",
            "সাবস্ক্রিপশন ও ডিজিটাল সেবা",
            "ভ্রমণ ও ছুটি",
            "দান ও উপহার",
            "অন্যান্য খরচ"
        )
        assertEquals(12, expenseCategories.size)
        expectedExpenseBn.forEach { expected ->
            assertTrue(
                "Expense catalog must include '$expected'",
                expenseCategories.any { it.bn == expected }
            )
        }

        val accounts = listOf(
            AccountEntity(
                id = 1L,
                name = "bKash Personal",
                type = AccountType.MFS,
                balance = 20_000.0,
                bankOrProvider = "bKash",
                colorArgb = 0xFFE2136EL,
                currencyCode = "BDT"
            )
        )

        // 3. Verify identical detection across UnifiedCategoryDetector (+ Entry), Voice, and Receipt Scan
        val voiceCases = listOf(
            "ঈদ বোনাস ১০০০০ টাকা পেয়েছি" to ("inc_bonus" to TransactionType.INCOME),
            "দারাজ রিফান্ড ৫০০ টাকা ঢুকেছে" to ("inc_refund" to TransactionType.INCOME),
            "ফ্রিল্যান্সিং থেকে ১৫০০০ টাকা পেয়েছি" to ("inc_business" to TransactionType.INCOME),
            "বাসা ভাড়া ১২০০০ টাকা দিয়েছি" to ("exp_housing_rent" to TransactionType.EXPENSE),
            "বিদ্যুৎ বিল ১৫০০ টাকা বিকাশ থেকে" to ("exp_utilities_bills" to TransactionType.EXPENSE),
            "পরিবারের জন্য ৫০০০ টাকা খরচ" to ("exp_family_remittance" to TransactionType.EXPENSE),
            "নেটফ্লিক্স সাবস্ক্রিপশন ৮০০ টাকা" to ("exp_subscriptions" to TransactionType.EXPENSE),
            "কক্সবাজার ভ্রমণ ফ্লাইট টিকেট ৬০০০ টাকা" to ("exp_travel" to TransactionType.EXPENSE),
            "মসজিদে দান ৫০০ টাকা" to ("exp_charity_gifts" to TransactionType.EXPENSE),
            "দুপুরের খাবার ২৫০ টাকা" to ("exp_food_dining" to TransactionType.EXPENSE),
            "কাঁচাবাজার ৮০০ টাকা" to ("exp_groceries" to TransactionType.EXPENSE)
        )
        voiceCases.forEach { (phrase, expected) ->
            val (expectedId, expectedType) = expected
            val voiceResult = com.example.data.ai.LocalVoiceTransactionParser.parse(
                utterance = phrase,
                accounts = accounts,
                language = AppLanguage.BN
            )
            assertEquals("Voice type mismatch for '$phrase'", expectedType, voiceResult.type)
            assertEquals("Voice category mismatch for '$phrase'", expectedId, voiceResult.managedCategoryId)

            val quickMatch = com.example.data.ai.UnifiedCategoryDetector.detectCategory(
                text = phrase,
                type = expectedType,
                managedCategories = com.example.data.model.BuiltInCategoriesCatalog.all,
                language = AppLanguage.BN
            )
            assertEquals("Quick (+) category mismatch for '$phrase'", expectedId, quickMatch.managedCategory.id)
        }

        // 4. Verify Receipt Scan uses the exact same unified expanded categories
        val netflixReceipt = com.example.data.ai.LocalReceiptTextParser.parseRawReceiptText(
            rawText = "Netflix Subscription\nTotal Paid: BDT 850.00\nDate: 2026-11-24",
            defaultCurrencyCode = "BDT",
            language = AppLanguage.BN
        )
        assertNotNull(netflixReceipt)
        assertEquals("exp_subscriptions", netflixReceipt?.managedCategoryId)
        assertEquals("সাবস্ক্রিপশন ও ডিজিটাল সেবা", netflixReceipt?.managedCategoryLabel)

        val refundReceipt = com.example.data.ai.LocalReceiptTextParser.parseRawReceiptText(
            rawText = "Daraz Order Refund Cash In\nAmount: BDT 1250\nTrxID: RFND9912",
            defaultCurrencyCode = "BDT",
            language = AppLanguage.BN
        )
        assertNotNull(refundReceipt)
        assertEquals(TransactionType.INCOME, refundReceipt?.transactionType)
        assertEquals("inc_refund", refundReceipt?.managedCategoryId)
    }

    @Test
    fun manualCustomCategoryAdd_synchronizesInstantlyAcrossVoiceReceiptQuickEntryAndBackup() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefsRepo = com.example.data.local.UserPreferencesRepository(context)
        prefsRepo.resetCategoriesToDefault()

        // 1. User manually adds custom Expense & Income categories from any entry flow
        val customGym = com.example.data.model.BuiltInCategoriesCatalog.createCustomCategory(
            type = TransactionType.EXPENSE,
            name = "জিম ও ফিটনেস"
        )
        val customTuition = com.example.data.model.BuiltInCategoriesCatalog.createCustomCategory(
            type = TransactionType.INCOME,
            name = "টিউশনি ইনকাম"
        )
        prefsRepo.upsertManagedCategory(customGym)
        prefsRepo.upsertManagedCategory(customTuition)

        val savedPrefs = prefsRepo.userPrefs.first()
        val mergedAll = com.example.data.model.BuiltInCategoriesCatalog.mergedAll(
            overrides = savedPrefs.managedCategoryOverrides,
            deletedIds = savedPrefs.deletedCategoryIds
        )

        // 2. Confirm custom categories are immediately available in merged catalog used by +, Voice, Receipt & Settings
        assertTrue(mergedAll.any { it.id == customGym.id && it.bn == "জিম ও ফিটনেস" })
        assertTrue(mergedAll.any { it.id == customTuition.id && it.bn == "টিউশনি ইনকাম" })

        val accounts = listOf(
            AccountEntity(
                id = 1L,
                name = "bKash",
                type = AccountType.MFS,
                balance = 10_000.0,
                bankOrProvider = "bKash",
                colorArgb = 0xFFE2136EL,
                currencyCode = "BDT"
            )
        )

        // 3. Voice Entry, Receipt Scan & Quick (+) Entry all automatically detect the newly synced custom categories
        val voiceCustom = com.example.data.ai.LocalVoiceTransactionParser.parse(
            utterance = "জিম ও ফিটনেস ফি ২০০০ টাকা দিয়েছি",
            accounts = accounts,
            language = AppLanguage.BN,
            managedCategories = mergedAll
        )
        assertEquals(customGym.id, voiceCustom.managedCategoryId)
        assertEquals("জিম ও ফিটনেস", voiceCustom.managedCategoryLabel)

        val receiptCustom = com.example.data.ai.LocalReceiptTextParser.parseRawReceiptText(
            rawText = "জিম ও ফিটনেস Monthly Pass\nTotal: BDT 2000",
            defaultCurrencyCode = "BDT",
            managedCategories = mergedAll,
            language = AppLanguage.BN
        )
        assertNotNull(receiptCustom)
        assertEquals(customGym.id, receiptCustom?.managedCategoryId)

        val quickCustomIncome = com.example.data.ai.UnifiedCategoryDetector.detectCategory(
            text = "টিউশনি ইনকাম ৮০০০ টাকা পেয়েছি",
            type = TransactionType.INCOME,
            managedCategories = mergedAll,
            language = AppLanguage.BN
        )
        assertEquals(customTuition.id, quickCustomIncome.managedCategory.id)
        assertTrue(quickCustomIncome.isExactCustomMatch)

        // 4. BackupManager JSON export & restore preserves custom categories intact
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val backupJson = com.example.data.backup.BackupManager.exportJson(db, savedPrefs)
            prefsRepo.resetCategoriesToDefault()
            assertTrue(prefsRepo.userPrefs.first().managedCategoryOverrides.isEmpty())

            com.example.data.backup.BackupManager.restoreFromJson(backupJson, db, prefsRepo)
            val restoredPrefs = prefsRepo.userPrefs.first()
            assertTrue(restoredPrefs.managedCategoryOverrides.any { it.id == customGym.id })
            assertTrue(restoredPrefs.managedCategoryOverrides.any { it.id == customTuition.id })
        } finally {
            db.close()
        }
    }
}
