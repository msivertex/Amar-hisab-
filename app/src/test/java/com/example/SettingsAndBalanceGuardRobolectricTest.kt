package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AccountEntity
import com.example.data.local.AppDatabase
import com.example.data.model.AccountBalanceGuard
import com.example.data.model.AccountType
import com.example.data.model.ExpenseCategory
import com.example.data.model.HomeLayoutStyle
import com.example.data.model.HomeSectionId
import com.example.data.model.LoanType
import com.example.data.model.NetWorthSummary
import com.example.data.model.TransactionType
import com.example.data.local.UserPrefs
import com.example.data.repository.FinanceRepository
import com.example.localization.AppLanguage
import com.example.localization.BdtFormatter
import com.example.localization.StringsProvider
import com.example.ui.screens.AccountsScreen
import com.example.ui.screens.ActivityScreen
import com.example.ui.screens.HomeDashboardScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SmartTransactionModal
import com.example.ui.theme.AmarHisabTheme
import com.example.ui.viewmodel.MainUiState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SettingsAndBalanceGuardRobolectricTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun settingsMainScreenShowsOnlyMainOptionsAndOpensDedicatedSubOptionScreens() {
        val prefs = UserPrefs(
            userName = "Shariful Islam",
            language = AppLanguage.EN,
            userCountryCode = "KW",
            mainCurrencyCode = "KWD",
            secondaryCurrencyCode = "BDT"
        )
        val accounts = listOf(
            AccountEntity(
                id = 1L,
                name = "KFH Wallet",
                bankOrProvider = "KFH",
                type = AccountType.DEBIT_BANK,
                balance = 244.0,
                colorArgb = 0xFF00C49A,
                currencyCode = "KWD"
            )
        )

        composeRule.setContent {
            AmarHisabTheme(darkTheme = false, isBangla = false) {
                SettingsScreen(
                    prefs = prefs,
                    accounts = accounts,
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
                    onClearBackupStatus = {}
                )
            }
        }

        composeRule.waitForIdle()

        // Main screen shows profile + main section tiles, NOT expanded sub-options inline
        composeRule.onNodeWithTag("settings_profile_card").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_list")
            .performScrollToNode(hasTestTag("settings_main_options_card"))
        composeRule.onNodeWithTag("settings_general_customization_card").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_financial_alerts_tile").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_wallet_currency_data_tile").assertIsDisplayed()

        composeRule.onNodeWithTag("settings_theme_tile").assertDoesNotExist()
        composeRule.onNodeWithTag("settings_language_tile").assertDoesNotExist()
        composeRule.onNodeWithTag("settings_toggle_budget_alert").assertDoesNotExist()
        composeRule.onNodeWithTag("settings_backup_restore_tile").assertDoesNotExist()
        composeRule.onNodeWithTag("settings_start_of_week_tile").assertDoesNotExist()
        composeRule.onNodeWithTag("settings_toggle_biometric").assertDoesNotExist()

        // 1. General & Customization -> opens dedicated screen with Theme, Language, Home Layout
        composeRule.onNodeWithTag("settings_general_customization_card").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("settings_section_page_general").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_theme_tile").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_language_tile").assertIsDisplayed()
        composeRule.onNodeWithTag("customize_home_button").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_section_back_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("settings_list").assertIsDisplayed()

        // 2. Financial Alerts -> opens dedicated screen with alert switches
        composeRule.onNodeWithTag("settings_list")
            .performScrollToNode(hasTestTag("settings_financial_alerts_tile"))
        composeRule.onNodeWithTag("settings_financial_alerts_tile").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("settings_section_page_alerts").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_toggle_budget_alert").assertExists()
        composeRule.onNodeWithTag("settings_section_back_button").performClick()
        composeRule.waitForIdle()

        // 3. Wallet, Currency & Data -> opens dedicated screen with Categories & Backup
        composeRule.onNodeWithTag("settings_list")
            .performScrollToNode(hasTestTag("settings_wallet_currency_data_tile"))
        composeRule.onNodeWithTag("settings_wallet_currency_data_tile").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("settings_section_page_wallet_data").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_categories_tile").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_section_list")
            .performScrollToNode(hasTestTag("settings_backup_restore_tile"))
        composeRule.onNodeWithTag("settings_backup_restore_tile").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_section_back_button").performClick()
        composeRule.waitForIdle()

        // 4. Date & Time -> opens dedicated screen with Live Timezone & Start of Week
        composeRule.onNodeWithTag("settings_list")
            .performScrollToNode(hasTestTag("settings_date_time_card"))
        composeRule.onNodeWithTag("settings_date_time_card").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("settings_section_page_date_time").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_live_timezone_tile").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_start_of_week_tile").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_section_back_button").performClick()
        composeRule.waitForIdle()

        // 5. Security & System -> opens dedicated screen with Biometric & Haptics
        composeRule.onNodeWithTag("settings_list")
            .performScrollToNode(hasTestTag("settings_security_card"))
        composeRule.onNodeWithTag("settings_security_card").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("settings_section_page_security").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_toggle_biometric").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_section_back_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("settings_list").assertIsDisplayed()
    }

    @Test
    fun repositoryBlocksInsufficientBalanceAndClampsZeroWithNoNegativeSigns() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val repo = FinanceRepository(db)
            val kwdWalletId = repo.addAccount(
                AccountEntity(
                    name = "KFH Kuwait",
                    bankOrProvider = "KFH",
                    type = AccountType.DEBIT_BANK,
                    balance = 50.0,
                    colorArgb = 0xFF00C49A,
                    currencyCode = "KWD"
                )
            )
            val bdtWalletId = repo.addAccount(
                AccountEntity(
                    name = "Cash BDT",
                    bankOrProvider = "Cash",
                    type = AccountType.CASH,
                    balance = 1000.0,
                    colorArgb = 0xFF2563EB,
                    currencyCode = "BDT"
                )
            )

            // 1. Expense exceeding available balance is rejected; balance & transaction list unchanged
            val rejectedExpense = repo.addTransaction(
                title = "Expensive Watch",
                amount = 60.0,
                type = TransactionType.EXPENSE,
                category = ExpenseCategory.SHOPPING,
                accountId = kwdWalletId
            )
            assertFalse(rejectedExpense)
            assertEquals(50.0, db.accountDao().getAccountById(kwdWalletId)!!.balance, 0.0001)
            assertTrue(db.transactionDao().observeAllTransactions().first().isEmpty())

            // 2. Transfer exceeding available balance is rejected
            val rejectedTransfer = repo.transferFunds(
                fromAccountId = kwdWalletId,
                toAccountId = bdtWalletId,
                amount = 55.0,
                note = "Overdraft transfer"
            )
            assertFalse(rejectedTransfer)
            assertEquals(50.0, db.accountDao().getAccountById(kwdWalletId)!!.balance, 0.0001)
            assertEquals(1000.0, db.accountDao().getAccountById(bdtWalletId)!!.balance, 0.0001)

            // 3. Goal funding exceeding available balance is rejected
            repo.addSavingsGoal(
                name = "Emergency Fund",
                targetAmount = 500.0,
                dueDateMillis = null,
                colorArgb = 0xFF00C49A
            )
            val goal = db.savingsGoalDao().observeAllGoals().first().first()
            val rejectedGoalFund = repo.fundGoalFromWallet(goal, 80.0, kwdWalletId)
            assertFalse(rejectedGoalFund)
            assertEquals(50.0, db.accountDao().getAccountById(kwdWalletId)!!.balance, 0.0001)

            // 4. Debt repayment (I_OWE) exceeding available wallet balance is rejected
            repo.addLoan(
                personName = "Karim",
                loanType = LoanType.I_OWE,
                totalAmount = 100.0,
                note = "Test loan",
                dueDateMillis = null,
                currencyCode = "KWD"
            )
            val loan = db.loanDao().observeAllLoans().first().first()
            val rejectedLoanPay = repo.settleLoan(
                loan = loan,
                settlementAmount = 75.0,
                walletId = kwdWalletId
            )
            assertFalse(rejectedLoanPay)
            assertEquals(50.0, db.accountDao().getAccountById(kwdWalletId)!!.balance, 0.0001)
            assertEquals(0.0, db.loanDao().observeAllLoans().first().first().paidAmount, 0.0001)

            // 5. Spending exact balance stops cleanly at 0.0 with no negative sign
            val exactSpend = repo.addTransaction(
                title = "Monthly Groceries",
                amount = 50.0,
                type = TransactionType.EXPENSE,
                category = ExpenseCategory.GROCERIES,
                accountId = kwdWalletId
            )
            assertTrue(exactSpend)
            val afterExactBalance = db.accountDao().getAccountById(kwdWalletId)!!.balance
            assertEquals(0.0, afterExactBalance, 0.0001)
            assertEquals(
                "KWD 0.000",
                BdtFormatter.formatBalance(afterExactBalance, "KWD", AppLanguage.EN)
            )

            // 6. Any further expense at zero balance is rejected and balance remains 0.0
            val rejectedAtZero = repo.addTransaction(
                title = "Tea",
                amount = 1.0,
                type = TransactionType.EXPENSE,
                category = ExpenseCategory.FOOD_DINING,
                accountId = kwdWalletId
            )
            assertFalse(rejectedAtZero)
            assertEquals(0.0, db.accountDao().getAccountById(kwdWalletId)!!.balance, 0.0001)

            // 7. Formatting and NetWorth guards never emit negative/minus signs
            assertEquals("KWD 0.000", BdtFormatter.formatBalance(-25.5, "KWD", AppLanguage.EN))
            assertFalse(BdtFormatter.formatBalance(-999.0, "BDT", AppLanguage.EN).contains("-"))
            assertEquals(0.0, NetWorthSummary(totalAssets = 100.0, totalLiabilities = 500.0).netWorth, 0.0001)
            assertEquals(0.0, AccountBalanceGuard.clampNonNegative(-0.0001), 0.0001)
        } finally {
            db.close()
        }
    }

    @Test
    fun smartTransactionModalBlocksInsufficientExpenseAndShowsClearMessage() {
        val accounts = listOf(
            AccountEntity(
                id = 1L,
                name = "KFH Wallet",
                bankOrProvider = "KFH",
                type = AccountType.DEBIT_BANK,
                balance = 10.0,
                colorArgb = 0xFF00C49A,
                currencyCode = "KWD"
            )
        )
        var submitted = false

        composeRule.setContent {
            AmarHisabTheme(darkTheme = false, isBangla = false) {
                SmartTransactionModal(
                    accounts = accounts,
                    initialMode = TransactionType.EXPENSE,
                    defaultAccountId = 1L,
                    autoOpenKeypad = true,
                    enableSubcategories = false,
                    hapticEnabled = false,
                    language = AppLanguage.EN,
                    strings = StringsProvider.english,
                    defaultCurrencyCode = "KWD",
                    onDismiss = {},
                    onSaveExpenseOrIncome = { _, _, _, _, _, _, _, _ -> submitted = true },
                    onExecuteTransfer = { _, _, _, _, _ -> }
                )
            }
        }

        composeRule.waitForIdle()

        // Enter amount 25 (greater than available KWD 10.000) -> error shown & save disabled
        composeRule.onNodeWithTag("calc_key_2").performClick()
        composeRule.onNodeWithTag("calc_key_5").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tx_insufficient_balance_error").assertExists()
        composeRule.onNodeWithTag("tx_save_button").assertIsNotEnabled()
        assertFalse(submitted)

        // Clear and enter 10 (within available balance 10.000) -> error disappears & save enabled
        composeRule.onNodeWithTag("calc_key_C").performClick()
        composeRule.onNodeWithTag("calc_key_1").performClick()
        composeRule.onNodeWithTag("calc_key_0").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tx_insufficient_balance_error").assertDoesNotExist()
        composeRule.onNodeWithTag("tx_save_button").assertIsEnabled()
    }

    @Test
    fun googleProfilePhotoUrlUpgradesToHighResAndPreservesAcrossUpdates() {
        val baseGoogleUrl = "https://lh3.googleusercontent.com/a/ACg8ocSampleAvatarToken"
        assertEquals(
            "$baseGoogleUrl=s400-c",
            com.example.core.ProfilePhotoUrlNormalizer.toHighResUrl("$baseGoogleUrl=s96-c")
        )
        assertEquals(
            "$baseGoogleUrl=s400-c",
            com.example.core.ProfilePhotoUrlNormalizer.toHighResUrl("$baseGoogleUrl=s64")
        )
        assertEquals(
            "$baseGoogleUrl=s400-c",
            com.example.core.ProfilePhotoUrlNormalizer.toHighResUrl("$baseGoogleUrl=w96-h96-c")
        )
        assertEquals(
            "https://lh3.googleusercontent.com/a-/AOh14GjToken/s400-c/photo.jpg",
            com.example.core.ProfilePhotoUrlNormalizer.toHighResUrl(
                "https://lh3.googleusercontent.com/a-/AOh14GjToken/s96-c/photo.jpg"
            )
        )
        assertEquals(
            "$baseGoogleUrl?sz=400",
            com.example.core.ProfilePhotoUrlNormalizer.toHighResUrl("$baseGoogleUrl?sz=96")
        )
        assertEquals(
            "$baseGoogleUrl=s400-c",
            com.example.core.ProfilePhotoUrlNormalizer.toHighResUrl(baseGoogleUrl)
        )
        // Already high-res Google URLs remain untouched
        assertEquals(
            "$baseGoogleUrl=s512-c",
            com.example.core.ProfilePhotoUrlNormalizer.toHighResUrl("$baseGoogleUrl=s512-c")
        )
        // Local & non-Google URIs remain untouched
        assertEquals(
            "file:///data/user/0/com.example/files/avatars/google_avatar_1.jpg",
            com.example.core.ProfilePhotoUrlNormalizer.toHighResUrl(
                "file:///data/user/0/com.example/files/avatars/google_avatar_1.jpg"
            )
        )
        assertEquals(
            "preset:sky_vault",
            com.example.core.ProfilePhotoUrlNormalizer.toHighResUrl("preset:sky_vault")
        )

        // Blank incoming URL preserves existing high-res Google URL
        assertEquals(
            "$baseGoogleUrl=s400-c",
            com.example.core.ProfilePhotoUrlNormalizer.mergePreservingHighRes(
                incomingUrl = "",
                existingUrl = "$baseGoogleUrl=s96-c"
            )
        )

        // UserPrefs.effectiveAvatarUrl upgrades legacy stored =s96-c automatically
        val googlePrefs = UserPrefs(
            userName = "MD Shariful Islam",
            userEmail = "msivertex@gmail.com",
            userAvatarUrl = "$baseGoogleUrl=s96-c",
            authMode = com.example.data.model.AuthMode.GOOGLE
        )
        assertEquals("$baseGoogleUrl=s400-c", googlePrefs.effectiveAvatarUrl)
    }

    @Test
    fun profileDetailsAndSettingsRenderHighResAvatarWhileKeepingEmailHiddenInHeaderCards() {
        val email = "msivertex@gmail.com"
        val prefs = UserPrefs(
            userName = "MD Shariful Islam",
            userEmail = email,
            userAvatarUrl = "https://lh3.googleusercontent.com/a/ACg8ocTest=s96-c",
            authMode = com.example.data.model.AuthMode.GOOGLE,
            language = AppLanguage.BN,
            userCountryCode = "BD",
            mainCurrencyCode = "BDT"
        )

        composeRule.setContent {
            AmarHisabTheme(darkTheme = false, isBangla = true) {
                com.example.ui.screens.DetailedProfileSubPage(
                    prefs = prefs,
                    strings = StringsProvider.bangla,
                    isGoogleSignInLoading = false,
                    googleNotice = null,
                    onBack = {},
                    onOpenAvatarSelector = {},
                    onUpdateDisplayName = {},
                    onTriggerGoogleSignIn = {},
                    onUnlinkGoogleAccount = {}
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("detailed_profile_hero_card").assertIsDisplayed()
        composeRule.onNodeWithTag("detailed_profile_avatar_tap").assertIsDisplayed()
        composeRule.onNodeWithTag("detailed_profile_hero_name").assertIsDisplayed()
        // Email pill remains hidden from top header card, and stays inside Google Account section
        composeRule.onNodeWithTag("detailed_profile_list")
            .performScrollToNode(hasTestTag("detailed_profile_google_card"))
        composeRule.onNodeWithTag("detailed_profile_google_card").assertIsDisplayed()
        composeRule.onNodeWithText(email).assertIsDisplayed()
    }

    @Test
    fun homeSummaryTwoCurrenciesRendersEqualHeightDualCards() {
        val accounts = listOf(
            AccountEntity(
                id = 1L,
                name = "City Bank",
                bankOrProvider = "City Bank",
                type = AccountType.DEBIT_BANK,
                balance = 50000.0,
                colorArgb = 0xFF0EA5E9,
                currencyCode = "BDT"
            ),
            AccountEntity(
                id = 2L,
                name = "KFH Wallet",
                bankOrProvider = "KFH",
                type = AccountType.DEBIT_BANK,
                balance = 244.0,
                colorArgb = 0xFF1E3A8A,
                currencyCode = "KWD"
            )
        )
        val state = MainUiState(
            prefs = UserPrefs(
                userName = "MD Shariful Islam",
                language = AppLanguage.EN,
                mainCurrencyCode = "BDT",
                secondaryCurrencyCode = "KWD",
                homeLayoutStyle = HomeLayoutStyle.WALLET_STACK
            ),
            accounts = accounts,
            totalLiquidBalance = 50000.0,
            liquidBalancesByCurrency = mapOf("BDT" to 50000.0, "KWD" to 244.0),
            spentThisMonth = 12500.0,
            incomeThisMonth = 65000.0,
            dailyAverageSpend = 416.0
        )

        composeRule.setContent {
            AmarHisabTheme(darkTheme = false, isBangla = false) {
                HomeDashboardScreen(
                    state = state,
                    strings = StringsProvider.english,
                    onOpenSearch = {},
                    onOpenNotifications = {},
                    onOpenSettings = {},
                    onOpenProfileEdit = {},
                    onOpenAllAccounts = {},
                    onOpenTransfer = {},
                    onOpenReceiptScan = {},
                    onOpenQuickExpense = {},
                    onOpenVoiceEntry = {},
                    onSeeAllActivity = {}
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("summary_dual_cards").assertIsDisplayed()
        composeRule.onNodeWithTag("summary_dual_balance_card").assertIsDisplayed()
        composeRule.onNodeWithTag("summary_dual_spent_card").assertIsDisplayed()
        composeRule.onNodeWithTag("hero_balance_card").assertDoesNotExist()
        composeRule.onNodeWithTag("home_total_balance_primary_amount", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithTag("dual_balance_currency_KWD", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithTag("home_spent_month_amount", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithTag("home_daily_avg_text", useUnmergedTree = true).assertIsDisplayed()

        val balanceBounds = composeRule.onNodeWithTag("summary_dual_balance_card").getBoundsInRoot()
        val spentBounds = composeRule.onNodeWithTag("summary_dual_spent_card").getBoundsInRoot()
        assertEquals(balanceBounds.top.value, spentBounds.top.value, 0.5f)
        assertEquals(balanceBounds.height.value, spentBounds.height.value, 1.0f)
    }

    @Test
    fun homeSummaryFiveCurrenciesMergesIntoUnifiedHeroCard() {
        val accounts = listOf(
            AccountEntity(id = 1L, name = "City Bank", bankOrProvider = "City", type = AccountType.DEBIT_BANK, balance = 50000.0, colorArgb = 0xFF0EA5E9, currencyCode = "BDT"),
            AccountEntity(id = 2L, name = "KFH Wallet", bankOrProvider = "KFH", type = AccountType.DEBIT_BANK, balance = 244.0, colorArgb = 0xFF1E3A8A, currencyCode = "KWD"),
            AccountEntity(id = 3L, name = "USD Card", bankOrProvider = "Wise", type = AccountType.DEBIT_BANK, balance = 1250.0, colorArgb = 0xFF10B981, currencyCode = "USD"),
            AccountEntity(id = 4L, name = "SAR Wallet", bankOrProvider = "AlRajhi", type = AccountType.DEBIT_BANK, balance = 3400.0, colorArgb = 0xFFF59E0B, currencyCode = "SAR"),
            AccountEntity(id = 5L, name = "EUR Vault", bankOrProvider = "Revolut", type = AccountType.DEBIT_BANK, balance = 820.0, colorArgb = 0xFF6366F1, currencyCode = "EUR")
        )
        val state = MainUiState(
            prefs = UserPrefs(
                userName = "MD Shariful Islam",
                language = AppLanguage.EN,
                mainCurrencyCode = "BDT",
                secondaryCurrencyCode = "KWD",
                homeLayoutStyle = HomeLayoutStyle.WALLET_STACK
            ),
            accounts = accounts,
            totalLiquidBalance = 50000.0,
            liquidBalancesByCurrency = mapOf(
                "BDT" to 50000.0,
                "KWD" to 244.0,
                "USD" to 1250.0,
                "SAR" to 3400.0,
                "EUR" to 820.0
            ),
            spentThisMonth = 12500.0,
            incomeThisMonth = 65000.0,
            dailyAverageSpend = 416.0
        )

        composeRule.setContent {
            AmarHisabTheme(darkTheme = false, isBangla = false) {
                HomeDashboardScreen(
                    state = state,
                    strings = StringsProvider.english,
                    onOpenSearch = {},
                    onOpenNotifications = {},
                    onOpenSettings = {},
                    onOpenProfileEdit = {},
                    onOpenAllAccounts = {},
                    onOpenTransfer = {},
                    onOpenReceiptScan = {},
                    onOpenQuickExpense = {},
                    onOpenVoiceEntry = {},
                    onSeeAllActivity = {}
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("hero_balance_card").assertIsDisplayed()
        composeRule.onNodeWithTag("summary_dual_cards").assertDoesNotExist()
        composeRule.onNodeWithTag("hero_balance_currency_BDT", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithTag("hero_balance_currency_KWD", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithTag("hero_balance_currency_USD", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithTag("hero_balance_currency_SAR", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithTag("hero_balance_currency_EUR", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithTag("hero_income_box", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithTag("hero_spent_box", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithTag("hero_daily_avg_text", useUnmergedTree = true).assertIsDisplayed()

        val incomeBounds = composeRule.onNodeWithTag("hero_income_box", useUnmergedTree = true).getBoundsInRoot()
        val spentBounds = composeRule.onNodeWithTag("hero_spent_box", useUnmergedTree = true).getBoundsInRoot()
        assertEquals(incomeBounds.top.value, spentBounds.top.value, 0.5f)
        assertEquals(incomeBounds.height.value, spentBounds.height.value, 1.0f)
    }

    @Test
    fun accountsHeaderHasCompactSingleLineAddAccountButtonWithCleanGap() {
        var addClicked = false
        composeRule.setContent {
            AmarHisabTheme(darkTheme = false, isBangla = false) {
                AccountsScreen(
                    accounts = emptyList(),
                    netWorth = NetWorthSummary(),
                    language = AppLanguage.EN,
                    strings = StringsProvider.english,
                    onBack = {},
                    onOpenAddWizard = { addClicked = true },
                    onDeleteAccount = {}
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("accounts_header_title").assertIsDisplayed()
        composeRule.onNodeWithTag("add_account_wizard_button").assertIsDisplayed()
        composeRule.onNodeWithTag("add_account_wizard_pill", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithTag("add_account_wizard_label", useUnmergedTree = true).assertIsDisplayed()

        val titleBounds = composeRule.onNodeWithTag("accounts_header_title").getBoundsInRoot()
        val buttonBounds = composeRule.onNodeWithTag("add_account_wizard_button").getBoundsInRoot()
        val pillBounds = composeRule.onNodeWithTag("add_account_wizard_pill", useUnmergedTree = true).getBoundsInRoot()

        assertTrue(
            "Expected clean horizontal gap between Accounts title (${titleBounds.right}) and button (${buttonBounds.left})",
            (buttonBounds.left - titleBounds.right).value >= 11.5f
        )
        assertTrue(
            "Expected sleek compact single-line Add Account pill height (<= 34dp), got ${pillBounds.height}",
            pillBounds.height.value <= 34f
        )
        assertTrue(
            "Expected accessible >= 48dp touch height, got ${buttonBounds.height}",
            buttonBounds.height.value >= 47.5f
        )

        composeRule.onNodeWithTag("add_account_wizard_button").performClick()
        assertTrue(addClicked)
    }

    @Test
    fun activityHeaderHasCompactReceiptScanButtonWithCleanGap() {
        var scanClicked = false
        val state = MainUiState(
            prefs = UserPrefs(
                userName = "MD Shariful Islam",
                language = AppLanguage.EN,
                mainCurrencyCode = "BDT"
            )
        )
        composeRule.setContent {
            AmarHisabTheme(darkTheme = false, isBangla = false) {
                ActivityScreen(
                    state = state,
                    strings = StringsProvider.english,
                    onOpenReceiptScan = { scanClicked = true },
                    onDeleteTransaction = {}
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("activity_header_title").assertIsDisplayed()
        composeRule.onNodeWithTag("activity_receipt_scan_button").assertIsDisplayed()
        composeRule.onNodeWithTag("activity_receipt_scan_pill", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithTag("activity_receipt_scan_label", useUnmergedTree = true).assertIsDisplayed()

        val titleBounds = composeRule.onNodeWithTag("activity_header_title").getBoundsInRoot()
        val buttonBounds = composeRule.onNodeWithTag("activity_receipt_scan_button").getBoundsInRoot()
        val pillBounds = composeRule.onNodeWithTag("activity_receipt_scan_pill", useUnmergedTree = true).getBoundsInRoot()

        assertTrue(
            "Expected clean horizontal gap between Activity title (${titleBounds.right}) and button (${buttonBounds.left})",
            (buttonBounds.left - titleBounds.right).value >= 11.5f
        )
        assertTrue(
            "Expected sleek compact single-line Receipt Scan pill height (<= 34dp), got ${pillBounds.height}",
            pillBounds.height.value <= 34f
        )
        assertTrue(
            "Expected accessible >= 48dp touch height, got ${buttonBounds.height}",
            buttonBounds.height.value >= 47.5f
        )

        composeRule.onNodeWithTag("activity_receipt_scan_button").performClick()
        assertTrue(scanClicked)
    }

    @Test
    fun customizeHomeSectionLabelsDisplayFullyWithoutEllipsisAndKeepReorderControlsFunctional() {
        var latestOrder: List<HomeSectionId> = HomeSectionId.entries
        var latestHidden: Set<HomeSectionId> = emptySet()

        val prefs = UserPrefs(
            userName = "MD Shariful Islam",
            language = AppLanguage.EN,
            mainCurrencyCode = "BDT"
        )

        composeRule.setContent {
            AmarHisabTheme(darkTheme = false, isBangla = false) {
                SettingsScreen(
                    prefs = prefs,
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
                    onUpdateDashboardLayout = { _, order, hidden ->
                        latestOrder = order
                        latestHidden = hidden
                    },
                    onSetAppLock = { _, _ -> },
                    onSetBiometricUnlock = {},
                    onLockNow = {},
                    onExportJson = {},
                    onExportCsv = {},
                    onRestoreJson = {},
                    onClearBackupStatus = {}
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("settings_list")
            .performScrollToNode(hasTestTag("settings_main_options_card"))
        composeRule.onNodeWithTag("settings_general_customization_card").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("customize_home_button").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("dashboard_customizer_sheet").assertIsDisplayed()

        HomeSectionId.entries.forEach { section ->
            val labelNode = composeRule.onNodeWithTag("customizer_section_label_${section.name}")
            labelNode.performScrollTo().assertIsDisplayed()
            val labelBounds = labelNode.getBoundsInRoot()
            val moveUpBounds = composeRule.onNodeWithTag("move_up_${section.name}").getBoundsInRoot()

            assertTrue(
                "Expected generous width (>= 85dp) for section ${section.name}, got ${labelBounds.width}",
                labelBounds.width.value >= 85f
            )
            assertTrue(
                "Expected clean separation between label (${labelBounds.right}) and Up arrow (${moveUpBounds.left}) for ${section.name}",
                labelBounds.right <= moveUpBounds.left
            )

            val layouts = mutableListOf<TextLayoutResult>()
            labelNode.fetchSemanticsNode().config[SemanticsActions.GetTextLayoutResult].action?.invoke(layouts)
            assertTrue("Expected measured TextLayoutResult for ${section.name}", layouts.isNotEmpty())
            assertFalse(
                "Expected no visual truncation/ellipsis overflow for section ${section.name}",
                layouts.first().hasVisualOverflow
            )
        }

        composeRule.onNodeWithTag("move_up_BUDGET").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertTrue(latestOrder.indexOf(HomeSectionId.BUDGET) < HomeSectionId.entries.indexOf(HomeSectionId.BUDGET))

        composeRule.onNodeWithTag("toggle_section_STREAK").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertTrue(HomeSectionId.STREAK in latestHidden)
    }
}
