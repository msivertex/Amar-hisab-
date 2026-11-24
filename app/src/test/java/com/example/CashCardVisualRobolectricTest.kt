package com.example

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.example.data.local.AccountEntity
import com.example.data.model.AccountType
import com.example.localization.AppLanguage
import com.example.localization.StringsProvider
import com.example.ui.components.AccountVisualCard
import com.example.ui.theme.AmarHisabTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class CashCardVisualRobolectricTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun cashAccountCardShowsBanknoteAndCoinsWithoutCardChipOrCardDigits() {
        val cashAccount = AccountEntity(
            id = 101L,
            name = "নগদ টাকা",
            bankOrProvider = "নগদ টাকা",
            type = AccountType.CASH,
            balance = 58.50,
            currencyCode = "KWD",
            colorArgb = 0xFF10B981
        )

        composeRule.setContent {
            AmarHisabTheme(darkTheme = false) {
                AccountVisualCard(
                    account = cashAccount,
                    language = AppLanguage.BN,
                    strings = StringsProvider.get(AppLanguage.BN)
                )
            }
        }

        composeRule.onNodeWithTag("cash_wallet_visual_card_101").assertIsDisplayed()
        composeRule.onNodeWithTag("cash_banknote_coin_graphic_101").assertIsDisplayed()
        composeRule.onNodeWithText("KWD").assertIsDisplayed()
        // Must NOT have plastic bank-card elements (no contactless icon, no •••• digits)
        assertEquals(
            0,
            composeRule.onAllNodesWithContentDescription("Contactless Payment").fetchSemanticsNodes().size
        )
        assertEquals(
            0,
            composeRule.onAllNodesWithText("••••", substring = true).fetchSemanticsNodes().size
        )
    }

    @Test
    fun bankAndMfsCardsRemainCompletelyUntouched() {
        val bankAccount = AccountEntity(
            id = 202L,
            name = "City Bank Debit",
            bankOrProvider = "City Bank",
            type = AccountType.DEBIT_BANK,
            balance = 25_000.0,
            accountNumberLast4 = "4821",
            currencyCode = "BDT",
            colorArgb = 0xFF2563EB
        )
        val bkashAccount = AccountEntity(
            id = 203L,
            name = "bKash Personal",
            bankOrProvider = "bKash",
            type = AccountType.MFS,
            balance = 4_200.0,
            accountNumberLast4 = "9912",
            currencyCode = "BDT",
            colorArgb = 0xFFE91E79
        )

        composeRule.setContent {
            AmarHisabTheme(darkTheme = true) {
                Column {
                    AccountVisualCard(
                        account = bankAccount,
                        language = AppLanguage.EN,
                        strings = StringsProvider.get(AppLanguage.EN)
                    )
                    AccountVisualCard(
                        account = bkashAccount,
                        language = AppLanguage.EN,
                        strings = StringsProvider.get(AppLanguage.EN)
                    )
                }
            }
        }

        // Non-cash accounts must NOT use CashWalletVisualCard
        assertEquals(
            0,
            composeRule.onAllNodesWithTag("cash_wallet_visual_card_202").fetchSemanticsNodes().size
        )
        assertEquals(
            0,
            composeRule.onAllNodesWithTag("cash_wallet_visual_card_203").fetchSemanticsNodes().size
        )
        // Bank & MFS cards preserve their original contactless icon and masked digits
        assertTrue(
            composeRule.onAllNodesWithContentDescription("Contactless Payment").fetchSemanticsNodes().size >= 2
        )
        assertTrue(
            composeRule.onAllNodesWithText("4821", substring = true).fetchSemanticsNodes().isNotEmpty()
        )
        assertTrue(
            composeRule.onAllNodesWithText("9912", substring = true).fetchSemanticsNodes().isNotEmpty()
        )
    }
}
