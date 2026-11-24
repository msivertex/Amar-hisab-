package com.example

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.export.MonthlyReportExporter
import com.example.data.export.ReportExportFormat
import com.example.data.local.AccountEntity
import com.example.data.local.AppDatabase
import com.example.data.local.TransactionEntity
import com.example.data.market.MarketSnapshot
import com.example.data.model.AccountNameResolver
import com.example.data.model.AccountType
import com.example.data.model.ExpenseCategory
import com.example.data.model.TransactionType
import com.example.data.model.TransferNoteCodec
import com.example.data.local.UserPrefs
import com.example.data.repository.FinanceRepository
import com.example.localization.AppLanguage
import com.example.localization.StringsProvider
import com.example.ui.screens.ActivityScreen
import com.example.ui.screens.AddAccountWizardSheet
import com.example.ui.theme.AmarHisabTheme
import com.example.ui.viewmodel.MainUiState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MultiCurrencyTransferReportRobolectricTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun accountNameResolver_assignsSmartAutoDefaults_andDisambiguatesDuplicateNames() {
        val blankBank = AccountEntity(
            id = 1L,
            name = "   ",
            type = AccountType.DEBIT_BANK,
            balance = 58.50,
            bankOrProvider = "KFH",
            accountSubtype = "Debit",
            colorArgb = 0xFF007E33L,
            currencyCode = "KWD"
        )
        assertEquals("KFH Debit", AccountNameResolver.resolveAccountName(blankBank, AppLanguage.EN))

        val blankCashBn = AccountEntity(
            id = 2L,
            name = "-",
            type = AccountType.CASH,
            balance = 5000.0,
            bankOrProvider = "নগদ টাকা",
            colorArgb = 0xFF10B981L,
            currencyCode = "KWD"
        )
        assertEquals("নগদ টাকা (KWD)", AccountNameResolver.resolveAccountName(blankCashBn, AppLanguage.BN))

        val kfhUser = AccountEntity(
            id = 10L,
            name = "Shariful Islam",
            type = AccountType.DEBIT_BANK,
            balance = 58.50,
            bankOrProvider = "KFH",
            accountSubtype = "Debit",
            colorArgb = 0xFF007E33L,
            currencyCode = "KWD"
        )
        val bkashUser = AccountEntity(
            id = 20L,
            name = "Shariful Islam",
            type = AccountType.MFS,
            balance = 12000.0,
            bankOrProvider = "bKash",
            colorArgb = 0xFFE91E79L,
            currencyCode = "BDT"
        )
        val routeTitle = AccountNameResolver.formatTransferRouteTitle(
            fromAccount = kfhUser,
            toAccount = bkashUser,
            allAccounts = listOf(kfhUser, bkashUser),
            language = AppLanguage.EN
        )
        assertEquals("Shariful Islam (KFH • KWD) → Shariful Islam (bKash • BDT)", routeTitle)
    }

    @Test
    fun repositoryTransfer_kwdToBdt_preservesSourceCurrency_recordsConversion_andReversesAccuratelyOnDelete() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val repo = FinanceRepository(db)

        runBlocking {
            // Blank name account should be auto-named on save
            val autoCashId = repo.addAccount(
                AccountEntity(
                    name = "   ",
                    type = AccountType.CASH,
                    balance = 2000.0,
                    bankOrProvider = "Physical Cash",
                    colorArgb = 0xFF10B981L,
                    currencyCode = "BDT"
                )
            )
            val savedAutoCash = db.accountDao().getAccountById(autoCashId)!!
            assertEquals("Cash (BDT)", savedAutoCash.name)

            val kfhId = repo.addAccount(
                AccountEntity(
                    name = "Shariful Islam",
                    type = AccountType.DEBIT_BANK,
                    balance = 58.50,
                    bankOrProvider = "KFH",
                    accountSubtype = "Debit",
                    colorArgb = 0xFF007E33L,
                    currencyCode = "KWD"
                )
            )
            val bkashId = repo.addAccount(
                AccountEntity(
                    name = "Shariful Islam",
                    type = AccountType.MFS,
                    balance = 1000.0,
                    bankOrProvider = "bKash",
                    colorArgb = 0xFFE91E79L,
                    currencyCode = "BDT"
                )
            )

            val ok = repo.transferFunds(
                fromAccountId = kfhId,
                toAccountId = bkashId,
                amount = 10.0,
                note = "Home support",
                conversionRate = 395.765
            )
            assertTrue(ok)

            val afterFrom = db.accountDao().getAccountById(kfhId)!!
            val afterTo = db.accountDao().getAccountById(bkashId)!!
            assertEquals(48.50, afterFrom.balance, 0.001)
            assertEquals(4957.65, afterTo.balance, 0.01)

            val transferTx = db.transactionDao().getAllTransactionsOnce().first()
            assertEquals("KWD", transferTx.currencyCode)
            assertEquals(10.0, transferTx.amount, 0.001)
            assertTrue(transferTx.title.contains("Shariful Islam (KFH • KWD)"))
            assertTrue(transferTx.title.contains("Shariful Islam (bKash • BDT)"))

            val parsed = TransferNoteCodec.parse(transferTx, afterTo)
            assertNotNull(parsed)
            assertTrue(parsed!!.isCrossCurrency)
            assertTrue(parsed.isRecordedRate)
            assertEquals("KWD", parsed.sourceCurrencyCode)
            assertEquals("BDT", parsed.destCurrencyCode)
            assertEquals(395.765, parsed.exchangeRate, 0.001)
            assertEquals(3957.65, parsed.receivedAmount, 0.01)
            assertEquals("Home support", parsed.userNote)

            // Deleting the cross-currency transfer must restore BOTH 10 KWD and 3957.65 BDT accurately
            repo.deleteTransaction(transferTx.id)
            assertEquals(58.50, db.accountDao().getAccountById(kfhId)!!.balance, 0.001)
            assertEquals(1000.0, db.accountDao().getAccountById(bkashId)!!.balance, 0.01)
        }
        db.close()
    }

    @Test
    fun monthlyReportExporter_preservesTrueCurrency_resolvesWalletsWithoutDashes_andSeparatesPerCurrencyTotals() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val nowMillis = System.currentTimeMillis()
        val kfhAcc = AccountEntity(
            id = 1L,
            name = "Shariful Islam",
            type = AccountType.DEBIT_BANK,
            balance = 48.50,
            bankOrProvider = "KFH",
            accountSubtype = "Debit",
            colorArgb = 0xFF007E33L,
            currencyCode = "KWD"
        )
        val bkashAcc = AccountEntity(
            id = 2L,
            name = "Shariful Islam",
            type = AccountType.MFS,
            balance = 4957.65,
            bankOrProvider = "bKash",
            colorArgb = 0xFFE91E79L,
            currencyCode = "BDT"
        )
        val blankWallet = AccountEntity(
            id = 3L,
            name = "",
            type = AccountType.CASH,
            balance = 2500.0,
            bankOrProvider = "Physical Cash",
            colorArgb = 0xFF10B981L,
            currencyCode = "BDT"
        )

        val encodedNote = TransferNoteCodec.encode(
            userNote = "Family remittance",
            sourceCurrencyCode = "KWD",
            destCurrencyCode = "BDT",
            exchangeRate = 395.765,
            receivedAmount = 3957.65
        )
        val kwdTransfer = TransactionEntity(
            id = 101L,
            title = "Shariful Islam (KFH • KD) → Shariful Islam (bKash • BDT)",
            amount = 10.0,
            type = TransactionType.TRANSFER,
            category = ExpenseCategory.TRANSFER,
            subcategory = "Wallet Transfer",
            accountId = kfhAcc.id,
            toAccountId = bkashAcc.id,
            note = encodedNote,
            currencyCode = "KWD",
            timestamp = nowMillis
        )
        val bdtExpense = TransactionEntity(
            id = 102L,
            title = "Grocery Bazaar",
            amount = 500.0,
            type = TransactionType.EXPENSE,
            category = ExpenseCategory.GROCERIES,
            accountId = blankWallet.id,
            note = "",
            currencyCode = "BDT",
            timestamp = nowMillis - 1_000L
        )
        // Transaction referencing a deleted account (id = 999L) must still resolve gracefully without "—"
        val deletedAccountExpense = TransactionEntity(
            id = 103L,
            title = "Coffee Shop",
            amount = 2.50,
            type = TransactionType.EXPENSE,
            category = ExpenseCategory.FOOD_DINING,
            accountId = 999L,
            note = "",
            currencyCode = "KWD",
            timestamp = nowMillis - 2_000L
        )

        val snapshot = MarketSnapshot.defaultReference()
        val state = MainUiState(
            prefs = UserPrefs(userName = "Shariful Islam", language = AppLanguage.EN, mainCurrencyCode = "BDT"),
            accounts = listOf(kfhAcc, bkashAcc, blankWallet),
            transactions = listOf(kwdTransfer, bdtExpense, deletedAccountExpense)
        )

        val payload = MonthlyReportExporter.buildMonthlyPayload(state, nowMillis, snapshot)
        // Per-currency totals must separate KWD and BDT accurately
        val kwdTotals = payload.perCurrencyTotals.first { it.currencyCode == "KWD" }
        val bdtTotals = payload.perCurrencyTotals.first { it.currencyCode == "BDT" }
        assertEquals(10.0, kwdTotals.transferOut, 0.001)
        assertEquals(2.50, kwdTotals.expense, 0.001)
        assertEquals(500.0, bdtTotals.expense, 0.001)
        // Aggregate BDT total must convert KWD expense rather than raw-adding 2.5 + 500 = 502.5
        val expectedKwdInBdt = snapshot.convertCurrencyCode(2.50, "KWD", "BDT")
        assertEquals(500.0 + expectedKwdInBdt, payload.totalExpense, 0.05)

        val csv = MonthlyReportExporter.generateMonthlyCsv(payload)
        assertTrue(csv.contains("Currency_Summary,KWD,0.00,2.50,-2.50,10.00"))
        assertTrue(csv.contains("Currency_Summary,BDT,0.00,500.00,-500.00,0.00"))
        assertTrue(csv.contains("Date,Title,Type,Category,Subcategory,Wallet,To_Wallet,Currency,Amount,Conversion_Details,Note"))
        assertTrue(csv.contains("\"Shariful Islam (KFH • KWD)\",\"Shariful Islam (bKash • BDT)\",KWD,10.00"))
        assertTrue(csv.contains("-> BDT 3957.65 @ 1 KWD = BDT 395.77"))
        assertTrue(csv.contains("\"Cash (BDT)\""))
        assertTrue(csv.contains("\"KWD Wallet\""))
        assertFalse(csv.contains("\"—\""))
        assertFalse(csv.contains("\"-\""))

        val pdfResult = MonthlyReportExporter.exportAndBuildShareIntent(
            context = context,
            state = state,
            format = ReportExportFormat.PDF,
            strings = StringsProvider.english,
            nowMillis = nowMillis,
            marketSnapshot = snapshot
        ).getOrThrow()
        assertTrue(pdfResult.file.exists())
        assertTrue(pdfResult.file.length() > 500L)
        val shareText = pdfResult.shareIntent.getStringExtra(android.content.Intent.EXTRA_TEXT).orEmpty()
        assertTrue(shareText.contains("KWD:"))
        assertTrue(shareText.contains("KWD د.ك 10.000"))
    }

    @Test
    fun activityScreen_showsTransferBadgeInsteadOfZeroDelta_andOpensVoucherDetailSheetInLightAndDark() {
        val nowMillis = System.currentTimeMillis()
        val kfhAcc = AccountEntity(
            id = 1L,
            name = "Shariful Islam",
            type = AccountType.DEBIT_BANK,
            balance = 48.50,
            bankOrProvider = "KFH",
            accountSubtype = "Debit",
            colorArgb = 0xFF007E33L,
            currencyCode = "KWD"
        )
        val bkashAcc = AccountEntity(
            id = 2L,
            name = "Shariful Islam",
            type = AccountType.MFS,
            balance = 4957.65,
            bankOrProvider = "bKash",
            colorArgb = 0xFFE91E79L,
            currencyCode = "BDT"
        )
        val transferTx = TransactionEntity(
            id = 501L,
            title = "Shariful Islam (KFH • KWD) → Shariful Islam (bKash • BDT)",
            amount = 10.0,
            type = TransactionType.TRANSFER,
            category = ExpenseCategory.TRANSFER,
            subcategory = "Wallet Transfer",
            accountId = kfhAcc.id,
            toAccountId = bkashAcc.id,
            note = TransferNoteCodec.encode("Home support", "KWD", "BDT", 395.765, 3957.65),
            currencyCode = "KWD",
            timestamp = nowMillis
        )
        val state = MainUiState(
            prefs = UserPrefs(language = AppLanguage.EN, mainCurrencyCode = "BDT"),
            accounts = listOf(kfhAcc, bkashAcc),
            transactions = listOf(transferTx)
        )
        var useDark by mutableStateOf(false)

        composeRule.setContent {
            AmarHisabTheme(darkTheme = useDark, isBangla = false) {
                ActivityScreen(
                    state = state,
                    strings = StringsProvider.english,
                    onOpenReceiptScan = {},
                    onDeleteTransaction = {},
                    marketSnapshot = MarketSnapshot.defaultReference()
                )
            }
        }

        // Transfer badge shown on Today header instead of misleading +৳ 0
        composeRule.onNodeWithTag("activity_transfer_pill_${state.deviceTimeContext.dayKey}_KWD")
            .assertIsDisplayed()
        assertTrue(
            composeRule.onAllNodesWithText("KWD د.ك 10.000", substring = true).fetchSemanticsNodes().isNotEmpty()
        )
        // Full From -> To route visible without truncation
        assertTrue(
            composeRule.onAllNodesWithText("Shariful Islam (KFH • KWD) → Shariful Islam (bKash • BDT)", substring = true)
                .fetchSemanticsNodes().isNotEmpty()
        )

        // Tap transaction card to open Transfer Voucher Detail Sheet
        composeRule.onNodeWithTag("activity_tx_card_501").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("activity_voucher_sheet").assertIsDisplayed()
        composeRule.onNodeWithTag("voucher_primary_amount").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("voucher_received_amount").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("voucher_exchange_rate").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("voucher_from_wallet").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("voucher_to_wallet").performScrollTo().assertIsDisplayed()

        // Toggle Dark Mode and confirm voucher sheet stays rendered cleanly
        useDark = true
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("activity_voucher_sheet").assertIsDisplayed()
    }

    @Test
    fun addAccountWizard_usesCustomNameWhenEntered_andAutoDefaultWhenLeftBlank() {
        val savedAccounts = mutableListOf<AccountEntity>()

        composeRule.setContent {
            AmarHisabTheme(darkTheme = false, isBangla = true) {
                AddAccountWizardSheet(
                    language = AppLanguage.BN,
                    strings = StringsProvider.bangla,
                    onDismiss = {},
                    onSaveAccount = { savedAccounts.add(it) },
                    defaultCurrencyCode = "BDT"
                )
            }
        }

        composeRule.onNodeWithTag("wizard_name_input").performScrollTo()
        composeRule.onNodeWithTag("wizard_name_auto_hint", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithTag("wizard_save_account_button").performScrollTo().performClick()
        composeRule.waitForIdle()

        assertEquals(1, savedAccounts.size)
        assertTrue(savedAccounts.first().name.isNotBlank())
        assertFalse(savedAccounts.first().name == "-")

        composeRule.onNodeWithTag("wizard_name_input").performScrollTo().performTextInput("  আমার প্রধান কার্ড  ")
        composeRule.onNodeWithTag("wizard_save_account_button").performScrollTo().performClick()
        composeRule.waitForIdle()

        assertEquals(2, savedAccounts.size)
        assertEquals("আমার প্রধান কার্ড", savedAccounts.last().name)
    }
}
