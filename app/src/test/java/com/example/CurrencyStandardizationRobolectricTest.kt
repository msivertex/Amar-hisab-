package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextInput
import com.example.data.market.MarketCurrency
import com.example.data.market.MarketSnapshot
import com.example.data.model.AccountNameResolver
import com.example.data.model.AppCurrency
import com.example.localization.AppLanguage
import com.example.localization.BdtFormatter
import com.example.ui.components.AppCurrencyPickerSheet
import com.example.ui.screens.formatLoanCurrencyAmount
import com.example.ui.screens.formatVoucherAmount
import com.example.ui.theme.AmarHisabTheme
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

    @Test
    fun kwdAlwaysUsesFixedIsoCodeThreeDecimalsAndArabicSymbolBetweenCodeAndAmount() {
        assertEquals("KWD", AppCurrency.KWD.code)
        assertEquals("KWD", AppCurrency.KWD.cleanBadgeLabel())
        assertEquals("د.ك", AppCurrency.KWD.symbol)
        assertEquals("KWD د.ك", AppCurrency.KWD.codeAndSymbolLabel())
        assertEquals("KWD", AccountNameResolver.currencyShortLabel("KWD"))
        assertEquals("د.ك", MarketCurrency.KWD.symbol)

        // English formatting: exact 3 decimal places and د.ك between KWD and amount
        assertEquals("KWD د.ك 244.000", BdtFormatter.formatCurrency(244.0, "KWD", AppLanguage.EN))
        assertEquals("KWD د.ك 5.500", BdtFormatter.formatCurrency(5.5, "KWD", AppLanguage.EN))
        assertEquals("KWD د.ك 500.000", BdtFormatter.formatCurrency(500.0, "KWD", AppLanguage.EN))
        assertEquals("-KWD د.ك 12.250", BdtFormatter.formatCurrency(-12.25, "KWD", AppLanguage.EN))
        assertEquals("د.ك 244.000", BdtFormatter.formatSymbolAndAmount(244.0, "KWD", AppLanguage.EN))

        // Bangla formatting: exact 3 decimal places in Bangla digits with KWD د.ك
        assertEquals("KWD د.ك ২৪৪.০০০", BdtFormatter.formatCurrency(244.0, "KWD", AppLanguage.BN))
        assertEquals("KWD د.ك ৫.৫০০", BdtFormatter.formatCurrency(5.5, "KWD", AppLanguage.BN))
        assertEquals("KWD د.ك ৫০০.০০০", BdtFormatter.formatCurrency(500.0, "KWD", AppLanguage.BN))

        // Loan & Voucher helpers preserve identical KWD د.ك 3-decimal standard
        assertEquals("KWD د.ك 58.500", formatLoanCurrencyAmount(58.5, "KWD", AppLanguage.EN))
        assertEquals("KWD د.ك 244.000", formatVoucherAmount(244.0, "KWD", AppLanguage.EN))
    }

    @Test
    fun uniformCurrencyFormatAppliesAcrossAllCountries() {
        assertEquals("BDT ৳ 31,28,880", BdtFormatter.formatCurrency(3128880.0, "BDT", AppLanguage.EN))
        assertEquals("BDT ৳ ৩১,২৮,৮৮০", BdtFormatter.formatCurrency(3128880.0, "BDT", AppLanguage.BN))
        assertEquals("INR ₹ 50,000", BdtFormatter.formatCurrency(50000.0, "INR", AppLanguage.EN))
        assertEquals("LKR Rs 10,000", BdtFormatter.formatCurrency(10000.0, "LKR", AppLanguage.EN))
        assertEquals("USD $ 1,250", BdtFormatter.formatCurrency(1250.0, "USD", AppLanguage.EN))
        assertEquals("৳ 31,28,880", BdtFormatter.formatSymbolAndAmount(3128880.0, "BDT", AppLanguage.EN))
        assertEquals("₹ 50,000", BdtFormatter.formatSymbolAndAmount(50000.0, "INR", AppLanguage.EN))
        assertEquals("Rs 10,000", BdtFormatter.formatSymbolAndAmount(10000.0, "LKR", AppLanguage.EN))

        // Uniform picker display label: Country Name • Code • Symbol
        assertEquals("Kuwait • KWD • د.ك", AppCurrency.KWD.pickerDisplayLabel(AppLanguage.EN))
        assertEquals("Bangladesh • BDT • ৳", AppCurrency.BDT.pickerDisplayLabel(AppLanguage.EN))
        assertEquals("India • INR • ₹", AppCurrency.INR.pickerDisplayLabel(AppLanguage.EN))
        assertEquals("Sri Lanka • LKR • Rs", AppCurrency.fromCode("LKR").pickerDisplayLabel(AppLanguage.EN))

        // Loan & Voucher decimal-BDT formatting keeps uniform BDT ৳ prefix
        assertEquals("BDT ৳ 18.50", formatVoucherAmount(18.5, "BDT", AppLanguage.EN))
        assertEquals("BDT ৳ 18.50", formatLoanCurrencyAmount(18.5, "BDT", AppLanguage.EN))

        // Never use legacy "KD" badge anywhere
        assertFalse(AppCurrency.KWD.cleanBadgeLabel() == "KD")
        assertFalse(AppCurrency.KWD.codeAndSymbolLabel().contains("KD"))
    }

    @Test
    fun currencyPickerSheetDisplaysUniformCountryCodeAndSymbolStyle() {
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
            composeRule.onAllNodesWithText("BDT ৳", substring = false).fetchSemanticsNodes().isNotEmpty()
        )
        assertTrue(
            composeRule.onAllNodesWithText("Bangladesh", substring = true).fetchSemanticsNodes().isNotEmpty()
        )

        // Filter to Kuwait / KWD and verify uniform Country Name + KWD د.ك badge style
        composeRule.onNodeWithTag("app_currency_picker_search").performTextInput("KWD")
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("app_currency_option_KWD").assertIsDisplayed()
        assertTrue(
            composeRule.onAllNodesWithText("KWD د.ك", substring = false).fetchSemanticsNodes().isNotEmpty()
        )
        assertTrue(
            composeRule.onAllNodesWithText("Kuwait", substring = true).fetchSemanticsNodes().isNotEmpty()
        )
    }
}
