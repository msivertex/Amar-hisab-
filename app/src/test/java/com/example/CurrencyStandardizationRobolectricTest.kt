package com.example

import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.text.TextLayoutResult
import com.example.data.local.UserPrefs
import com.example.data.market.MarketSnapshot
import com.example.data.market.MarketSyncUiState
import com.example.data.model.AccountNameResolver
import com.example.data.model.AppCurrency
import com.example.data.model.CategorySpend
import com.example.data.model.DailyTrendPoint
import com.example.data.model.ExpenseCategory
import com.example.localization.AppLanguage
import com.example.localization.BdtFormatter
import com.example.localization.StringsProvider
import com.example.ui.components.AppCurrencyPickerSheet
import com.example.ui.screens.InsightsScreen
import com.example.ui.screens.formatLoanCurrencyAmount
import com.example.ui.screens.formatVoucherAmount
import com.example.ui.theme.AmarHisabTheme
import com.example.ui.viewmodel.AnalyticsSnapshot
import com.example.ui.viewmodel.MainUiState
import com.example.ui.viewmodel.SearchAiUiState
import com.example.ui.viewmodel.TtsUiState
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
class CurrencyStandardizationRobolectricTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val forbiddenSymbols = listOf("৳", "د.ك", "₹", "Rs", "$", "€", "£")

    private fun assertNoCurrencySymbols(rendered: String) {
        forbiddenSymbols.forEach { sym ->
            assertFalse("Expected no symbol '$sym' in '$rendered'", rendered.contains(sym))
        }
    }

    @Test
    fun kwdAlwaysUsesCodeOnlyThreeDecimalsWithoutAnySymbol() {
        assertEquals("KWD", AppCurrency.KWD.code)
        assertEquals("KWD", AppCurrency.KWD.cleanBadgeLabel())
        assertEquals("KWD", AppCurrency.KWD.codeAndSymbolLabel())
        assertEquals("KWD", AccountNameResolver.currencyShortLabel("KWD"))

        // English formatting: exact 3 decimal places, clean ISO code only
        assertEquals("KWD 244.000", BdtFormatter.formatCurrency(244.0, "KWD", AppLanguage.EN))
        assertEquals("KWD 5.500", BdtFormatter.formatCurrency(5.5, "KWD", AppLanguage.EN))
        assertEquals("KWD 500.000", BdtFormatter.formatCurrency(500.0, "KWD", AppLanguage.EN))
        assertEquals("-KWD 12.250", BdtFormatter.formatCurrency(-12.25, "KWD", AppLanguage.EN))
        assertEquals("KWD 0.000", BdtFormatter.formatBalance(0.0, "KWD", AppLanguage.EN))
        assertEquals("244.000", BdtFormatter.formatSymbolAndAmount(244.0, "KWD", AppLanguage.EN))

        // Bangla formatting: exact 3 decimal places in Bangla digits, clean ISO code only
        assertEquals("KWD ২৪৪.০০০", BdtFormatter.formatCurrency(244.0, "KWD", AppLanguage.BN))
        assertEquals("KWD ৫.৫০০", BdtFormatter.formatCurrency(5.5, "KWD", AppLanguage.BN))
        assertEquals("KWD ৫০০.০০০", BdtFormatter.formatCurrency(500.0, "KWD", AppLanguage.BN))
        assertEquals("KWD ০.০০০", BdtFormatter.formatBalance(0.0, "KWD", AppLanguage.BN))

        // Loan & Voucher helpers preserve identical KWD 3-decimal symbol-free standard
        assertEquals("KWD 58.500", formatLoanCurrencyAmount(58.5, "KWD", AppLanguage.EN))
        assertEquals("KWD 244.000", formatVoucherAmount(244.0, "KWD", AppLanguage.EN))

        assertNoCurrencySymbols(BdtFormatter.formatCurrency(244.0, "KWD", AppLanguage.EN))
        assertNoCurrencySymbols(BdtFormatter.formatCurrency(244.0, "KWD", AppLanguage.BN))
    }

    @Test
    fun uniformCodeOnlyCurrencyFormatAppliesAcrossAllCountries() {
        assertEquals("BDT 50,000", BdtFormatter.formatCurrency(50000.0, "BDT", AppLanguage.EN))
        assertEquals("BDT ৫০,০০০", BdtFormatter.formatCurrency(50000.0, "BDT", AppLanguage.BN))
        assertEquals("BDT 31,28,880", BdtFormatter.formatCurrency(3128880.0, "BDT", AppLanguage.EN))
        assertEquals("BDT ৩১,২৮,৮৮০", BdtFormatter.formatCurrency(3128880.0, "BDT", AppLanguage.BN))
        assertEquals("INR 25,000", BdtFormatter.formatCurrency(25000.0, "INR", AppLanguage.EN))
        assertEquals("LKR 10,000", BdtFormatter.formatCurrency(10000.0, "LKR", AppLanguage.EN))
        assertEquals("USD 1,250", BdtFormatter.formatCurrency(1250.0, "USD", AppLanguage.EN))
        assertEquals("EUR 950", BdtFormatter.formatCurrency(950.0, "EUR", AppLanguage.EN))
        assertEquals("GBP 420", BdtFormatter.formatCurrency(420.0, "GBP", AppLanguage.EN))

        assertEquals("31,28,880", BdtFormatter.formatSymbolAndAmount(3128880.0, "BDT", AppLanguage.EN))
        assertEquals("25,000", BdtFormatter.formatSymbolAndAmount(25000.0, "INR", AppLanguage.EN))
        assertEquals("10,000", BdtFormatter.formatSymbolAndAmount(10000.0, "LKR", AppLanguage.EN))

        // Uniform picker display label: Country Name • Code (symbol-free)
        assertEquals("Kuwait • KWD", AppCurrency.KWD.pickerDisplayLabel(AppLanguage.EN))
        assertEquals("Bangladesh • BDT", AppCurrency.BDT.pickerDisplayLabel(AppLanguage.EN))
        assertEquals("India • INR", AppCurrency.INR.pickerDisplayLabel(AppLanguage.EN))
        assertEquals("Sri Lanka • LKR", AppCurrency.fromCode("LKR").pickerDisplayLabel(AppLanguage.EN))

        // Loan & Voucher decimal-BDT formatting keeps uniform symbol-free BDT prefix
        assertEquals("BDT 18.50", formatVoucherAmount(18.5, "BDT", AppLanguage.EN))
        assertEquals("BDT 18.50", formatLoanCurrencyAmount(18.5, "BDT", AppLanguage.EN))

        listOf(
            BdtFormatter.formatCurrency(50000.0, "BDT", AppLanguage.EN),
            BdtFormatter.formatCurrency(50000.0, "BDT", AppLanguage.BN),
            BdtFormatter.formatCurrency(25000.0, "INR", AppLanguage.EN),
            BdtFormatter.formatCurrency(10000.0, "LKR", AppLanguage.EN),
            BdtFormatter.formatCurrency(1250.0, "USD", AppLanguage.EN),
            BdtFormatter.formatCurrency(950.0, "EUR", AppLanguage.EN),
            BdtFormatter.formatCurrency(420.0, "GBP", AppLanguage.EN),
            AppCurrency.BDT.pickerDisplayLabel(AppLanguage.EN),
            AppCurrency.KWD.pickerDisplayLabel(AppLanguage.EN)
        ).forEach(::assertNoCurrencySymbols)

        // Never use legacy "KD" badge anywhere
        assertFalse(AppCurrency.KWD.cleanBadgeLabel() == "KD")
        assertFalse(AppCurrency.KWD.codeAndSymbolLabel().contains("KD"))
    }

    @Test
    fun currencyPickerSheetDisplaysCleanCodeOnlyLabelsWithoutSymbols() {
        composeRule.setContent {
            AmarHisabTheme(darkTheme = false, isBangla = false) {
                AppCurrencyPickerSheet(
                    title = "Select Main Currency",
                    subtitle = "All 195 countries supported",
                    selectedCurrency = AppCurrency.KWD,
                    language = AppLanguage.EN,
                    referenceCurrency = AppCurrency.BDT,
                    marketSnapshot = MarketSnapshot.defaultReference(),
                    onSelect = {},
                    onDismiss = {}
                )
            }
        }

        composeRule.onNodeWithTag("app_currency_picker_sheet").assertIsDisplayed()
        composeRule.onNodeWithTag("app_currency_option_BDT").assertIsDisplayed()
        assertTrue(
            composeRule.onAllNodesWithText("BDT", substring = false).fetchSemanticsNodes().isNotEmpty()
        )
        assertTrue(
            composeRule.onAllNodesWithText("৳", substring = true).fetchSemanticsNodes().isEmpty()
        )
        assertTrue(
            composeRule.onAllNodesWithText("Bangladesh", substring = true).fetchSemanticsNodes().isNotEmpty()
        )

        // Filter to Kuwait / KWD and verify clean code-only KWD badge without د.ك
        composeRule.onNodeWithTag("app_currency_picker_search").performTextInput("KWD")
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("app_currency_option_KWD").assertIsDisplayed()
        assertTrue(
            composeRule.onAllNodesWithText("KWD", substring = false).fetchSemanticsNodes().isNotEmpty()
        )
        assertTrue(
            composeRule.onAllNodesWithText("د.ك", substring = true).fetchSemanticsNodes().isEmpty()
        )
        assertTrue(
            composeRule.onAllNodesWithText("Kuwait", substring = true).fetchSemanticsNodes().isNotEmpty()
        )
    }

    @Test
    fun homeCurrencyAmountTextDisplaysCodeOnlyBdtBlackAndKwdDarkBlueInEnglishAndBangla() {
        var bdtColor = androidx.compose.ui.graphics.Color.Unspecified
        var kwdColor = androidx.compose.ui.graphics.Color.Unspecified

        composeRule.setContent {
            AmarHisabTheme(darkTheme = false, isBangla = false) {
                val crystal = com.example.ui.theme.LocalCrystalColors.current
                bdtColor = crystal.amountColorForCurrency("BDT")
                kwdColor = crystal.amountColorForCurrency("KWD")

                androidx.compose.foundation.layout.Column {
                    com.example.ui.components.CurrencyAmountText(
                        amount = 3128880.0,
                        currencyCode = "BDT",
                        language = AppLanguage.EN,
                        style = com.example.ui.theme.NumericSummaryPrimaryStyle,
                        isBalance = true,
                        modifier = androidx.compose.ui.Modifier.testTag("test_bdt_full_en")
                    )
                    com.example.ui.components.CurrencyAmountText(
                        amount = 50000.0,
                        currencyCode = "BDT",
                        language = AppLanguage.BN,
                        style = com.example.ui.theme.NumericSummaryPrimaryStyle,
                        isBalance = true,
                        modifier = androidx.compose.ui.Modifier.testTag("test_bdt_full_bn")
                    )
                    com.example.ui.components.CurrencyAmountText(
                        amount = 244.0,
                        currencyCode = "KWD",
                        language = AppLanguage.EN,
                        style = com.example.ui.theme.NumericSummaryPrimaryStyle,
                        isBalance = true,
                        modifier = androidx.compose.ui.Modifier.testTag("test_kwd_en")
                    )
                    com.example.ui.components.CurrencyAmountText(
                        amount = 244.0,
                        currencyCode = "KWD",
                        language = AppLanguage.BN,
                        style = com.example.ui.theme.NumericSummaryPrimaryStyle,
                        isBalance = true,
                        modifier = androidx.compose.ui.Modifier.testTag("test_kwd_bn")
                    )
                }
            }
        }

        composeRule.waitForIdle()

        // 1. BDT stays in regular pure black color on light surfaces; KWD uses dark blue
        assertEquals(com.example.ui.theme.BdtPureBlackInk, bdtColor)
        assertEquals(com.example.ui.theme.KwdDarkBlueLight, kwdColor)

        // 2. Full BDT amount renders cleanly in both English & Bangla without symbols or "..." truncation
        composeRule.onNodeWithTag("test_bdt_full_en").assertIsDisplayed()
        assertTrue(
            composeRule.onAllNodesWithText("BDT 31,28,880", substring = false).fetchSemanticsNodes().isNotEmpty()
        )
        composeRule.onNodeWithTag("test_bdt_full_bn").assertIsDisplayed()
        assertTrue(
            composeRule.onAllNodesWithText("BDT ৫০,০০০", substring = false).fetchSemanticsNodes().isNotEmpty()
        )

        // 3. KWD renders fixed 3-decimal code-only amount cleanly in English & Bangla with zero symbols
        composeRule.onNodeWithTag("test_kwd_en").assertIsDisplayed()
        assertTrue(
            composeRule.onAllNodesWithText("KWD 244.000", substring = false).fetchSemanticsNodes().isNotEmpty()
        )
        composeRule.onNodeWithTag("test_kwd_bn").assertIsDisplayed()
        assertTrue(
            composeRule.onAllNodesWithText("KWD ২৪৪.০০০", substring = false).fetchSemanticsNodes().isNotEmpty()
        )
        assertTrue(
            composeRule.onAllNodesWithText("৳", substring = true).fetchSemanticsNodes().isEmpty()
        )
        assertTrue(
            composeRule.onAllNodesWithText("د.ك", substring = true).fetchSemanticsNodes().isEmpty()
        )
    }

    @Test
    fun financialInsightsDisplaysFullNonTruncatedDualCurrencyStackedMetrics() {
        val snapshot = MarketSnapshot.defaultReference()
        val analytics = AnalyticsSnapshot(
            periodLabel = "November 2026",
            totalIncome = 1_250_000.0,
            totalExpense = 385_000.0,
            netFlow = 865_000.0,
            dailyAverage = 12_833.0,
            peakAmount = 95_000.0,
            trendPoints = listOf(
                DailyTrendPoint("01", 25_000.0, isPeak = false),
                DailyTrendPoint("05", 95_000.0, isPeak = true),
                DailyTrendPoint("10", 18_500.0, isPeak = false)
            ),
            categoryBreakdown = listOf(
                CategorySpend(ExpenseCategory.GROCERIES, 220_000.0, 0.57f),
                CategorySpend(ExpenseCategory.FOOD_DINING, 165_000.0, 0.43f)
            ),
            expenseChangePercent = -12,
            savingsRatePercent = 69,
            topCategory = CategorySpend(ExpenseCategory.GROCERIES, 220_000.0, 0.57f),
            transactionCount = 14
        )
        val uiState = MainUiState(
            prefs = UserPrefs(
                userName = "MD Shariful Islam",
                language = AppLanguage.EN,
                mainCurrencyCode = "BDT",
                secondaryCurrencyCode = "KWD"
            ),
            analytics = analytics
        )

        composeRule.setContent {
            AmarHisabTheme(darkTheme = false, isBangla = false) {
                InsightsScreen(
                    state = uiState,
                    ttsState = TtsUiState.Idle,
                    searchAiState = SearchAiUiState.Idle,
                    strings = StringsProvider.english,
                    onSelectFilter = {},
                    onShiftMonth = {},
                    onPlayBriefingTts = {},
                    onStopTts = {},
                    onSearchMarket = {},
                    marketSyncState = MarketSyncUiState(snapshot = snapshot)
                )
            }
        }

        composeRule.waitForIdle()

        fun verifyStackedPair(primaryTag: String, secondaryTag: String, expectedPrimaryPrefix: String) {
            composeRule.onNodeWithTag("insights_list").performScrollToNode(hasTestTag(primaryTag))
            composeRule.onNodeWithTag("insights_list").performScrollToNode(hasTestTag(secondaryTag))
            val primaryNode = composeRule.onNodeWithTag(primaryTag)
            val secondaryNode = composeRule.onNodeWithTag(secondaryTag)
            primaryNode.assertIsDisplayed()
            secondaryNode.assertIsDisplayed()

            val primaryText = primaryNode.fetchSemanticsNode().config[SemanticsProperties.Text]
                .joinToString("") { it.text }
            val secondaryText = secondaryNode.fetchSemanticsNode().config[SemanticsProperties.Text]
                .joinToString("") { it.text }

            assertTrue("Expected '$primaryText' to start with '$expectedPrimaryPrefix'", primaryText.startsWith(expectedPrimaryPrefix))
            assertTrue("Expected '$secondaryText' to start with 'KWD '", secondaryText.startsWith("KWD "))
            assertTrue("Expected 3 decimals in KWD '$secondaryText'", Regex("KWD [\\d,]+\\.\\d{3}$").matches(secondaryText))
            assertNoCurrencySymbols(primaryText)
            assertNoCurrencySymbols(secondaryText)
            assertFalse("Expected no minus sign in '$primaryText'", primaryText.contains("-"))
            assertFalse("Expected no minus sign in '$secondaryText'", secondaryText.contains("-"))

            // Main Currency on top, Second Currency stacked directly below it
            val primaryBounds = primaryNode.getBoundsInRoot()
            val secondaryBounds = secondaryNode.getBoundsInRoot()
            assertTrue(
                "Expected $secondaryTag (${secondaryBounds.top}) stacked below $primaryTag (${primaryBounds.bottom})",
                secondaryBounds.top >= primaryBounds.bottom
            )

            // Zero ellipsis / visual truncation on both figures
            val primaryLayouts = mutableListOf<TextLayoutResult>()
            primaryNode.fetchSemanticsNode().config[SemanticsActions.GetTextLayoutResult].action?.invoke(primaryLayouts)
            assertTrue(primaryLayouts.isNotEmpty())
            assertFalse("Expected zero visual overflow for $primaryTag ($primaryText)", primaryLayouts.first().hasVisualOverflow)

            val secondaryLayouts = mutableListOf<TextLayoutResult>()
            secondaryNode.fetchSemanticsNode().config[SemanticsActions.GetTextLayoutResult].action?.invoke(secondaryLayouts)
            assertTrue(secondaryLayouts.isNotEmpty())
            assertFalse("Expected zero visual overflow for $secondaryTag ($secondaryText)", secondaryLayouts.first().hasVisualOverflow)
        }

        verifyStackedPair("insights_income_primary_amount", "insights_income_secondary_amount", "BDT ")
        verifyStackedPair("insights_expense_primary_amount", "insights_expense_secondary_amount", "BDT ")
        verifyStackedPair("insights_net_flow_primary_amount", "insights_net_flow_secondary_amount", "BDT ")
        verifyStackedPair("insights_trend_daily_avg_primary", "insights_trend_daily_avg_secondary", "BDT ")
        verifyStackedPair("insights_trend_peak_primary", "insights_trend_peak_secondary", "BDT ")
        verifyStackedPair("insights_donut_primary_amount", "insights_donut_secondary_amount", "BDT ")
    }
}
