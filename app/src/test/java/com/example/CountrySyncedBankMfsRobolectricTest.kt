package com.example

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import com.example.data.local.AccountEntity
import com.example.data.local.UserPrefs
import com.example.data.model.AccountType
import com.example.data.model.AppCountry
import com.example.data.model.BankCategory
import com.example.data.model.InternationalBankCatalog
import com.example.data.model.InternationalMfsCatalog
import com.example.localization.AppLanguage
import com.example.localization.StringsProvider
import com.example.ui.screens.AddAccountWizardSheet
import com.example.ui.screens.DetailedProfileSubPage
import com.example.ui.theme.AmarHisabTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CountrySyncedBankMfsRobolectricTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun catalogsProvideAccurateCountrySpecificBanksAndMfsAcrossSupportedCountries() {
        // Bangladesh
        val bdBanks = InternationalBankCatalog.banksForCountry(AppCountry.BD)
        assertEquals(22, bdBanks.size)
        assertEquals(5, bdBanks.count { it.category == BankCategory.GOVERNMENT })
        assertEquals(17, bdBanks.count { it.category == BankCategory.PRIVATE })
        assertTrue(bdBanks.any { it.en.contains("Sonali") })
        assertTrue(bdBanks.any { it.en.contains("Janata") })
        assertTrue(bdBanks.any { it.en.contains("BRAC") })
        assertTrue(bdBanks.any { it.en.contains("Dutch-Bangla") })

        val bdMfs = InternationalMfsCatalog.providersForCountry(AppCountry.BD)
        assertEquals(10, bdMfs.size)
        assertTrue(bdMfs.any { it.en == "bKash" })
        assertTrue(bdMfs.any { it.en == "Nagad" })
        assertTrue(bdMfs.any { it.en == "Rocket" })
        assertTrue(bdMfs.any { it.en == "Upay" })

        // India
        val inBanks = InternationalBankCatalog.banksForCountry(AppCountry.IN)
        assertTrue(inBanks.any { it.en.contains("SBI") })
        assertTrue(inBanks.any { it.en.contains("HDFC") })
        assertTrue(inBanks.any { it.en.contains("ICICI") })
        assertTrue(inBanks.any { it.en.contains("Axis") })
        val inMfs = InternationalMfsCatalog.providersForCountry(AppCountry.IN)
        assertTrue(inMfs.any { it.en == "Paytm" })
        assertTrue(inMfs.any { it.en == "PhonePe" })
        assertTrue(inMfs.any { it.en == "Google Pay" })
        assertTrue(inMfs.any { it.en == "BHIM UPI" })

        // Kuwait
        val kwBanks = InternationalBankCatalog.banksForCountry(AppCountry.KW)
        assertTrue(kwBanks.any { it.en.contains("NBK") })
        assertTrue(kwBanks.any { it.en.contains("KFH") })
        assertTrue(kwBanks.any { it.en.contains("Gulf Bank") })
        assertTrue(kwBanks.any { it.en.contains("Boubyan") })
        assertTrue(kwBanks.any { it.en.contains("Burgan") })
        val kwMfs = InternationalMfsCatalog.providersForCountry(AppCountry.KW)
        assertTrue(kwMfs.any { it.en == "KNET" })
        assertTrue(kwMfs.any { it.en == "MyFatoorah" })
        assertTrue(kwMfs.any { it.en == "Weyay" })

        // Saudi Arabia & UAE
        val saMfs = InternationalMfsCatalog.providersForCountry(AppCountry.SA)
        assertTrue(saMfs.any { it.en.contains("stc pay", ignoreCase = true) })
        val aeMfs = InternationalMfsCatalog.providersForCountry(AppCountry.AE)
        assertTrue(aeMfs.any { it.en.contains("e& money", ignoreCase = true) })

        // Every single country in the 195-country catalog must resolve non-empty banks and MFS wallets
        AppCountry.entries.forEach { country ->
            assertTrue("Expected non-empty banks for ${country.code}", InternationalBankCatalog.banksForCountry(country).isNotEmpty())
            assertTrue("Expected non-empty MFS for ${country.code}", InternationalMfsCatalog.providersForCountry(country).isNotEmpty())
        }
    }

    @Test
    fun profileDetailsBankAndMfsDirectoryDynamicallySyncsWhenCountryChanges() {
        var currentPrefs by mutableStateOf(
            UserPrefs(
                userName = "Shariful",
                language = AppLanguage.EN,
                userCountryCode = "BD",
                mainCurrencyCode = "BDT"
            )
        )

        composeRule.setContent {
            AmarHisabTheme(darkTheme = false, isBangla = false) {
                DetailedProfileSubPage(
                    prefs = currentPrefs,
                    strings = StringsProvider.english,
                    isGoogleSignInLoading = false,
                    googleNotice = null,
                    onBack = {},
                    onOpenAvatarSelector = {},
                    onUpdateDisplayName = {},
                    onTriggerGoogleSignIn = {},
                    onUnlinkGoogleAccount = {},
                    onSetUserCountry = { code -> currentPrefs = currentPrefs.copy(userCountryCode = code) }
                )
            }
        }

        composeRule.waitForIdle()

        // 1. Default Bangladesh -> BD banks & bKash/Nagad/Rocket
        composeRule.onNodeWithTag("detailed_profile_list")
            .performScrollToNode(hasTestTag("detailed_profile_bank_mfs_db_card"))
        composeRule.onNodeWithText("Bangladesh • 22 banks (5 Govt • 17 Private)").assertIsDisplayed()
        composeRule.onNodeWithText("bKash", substring = true).assertIsDisplayed()

        // 2. Switch Country to India -> Bank & MFS Database immediately updates to India
        composeRule.onNodeWithTag("detailed_profile_list")
            .performScrollToNode(hasTestTag("detailed_profile_region_currency_card"))
        composeRule.onNodeWithTag("detailed_profile_country_row")
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("app_country_picker_search").performTextReplacement("India")
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("app_country_option_IN")
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()
        assertEquals("IN", currentPrefs.userCountryCode)

        composeRule.onNodeWithTag("detailed_profile_list")
            .performScrollToNode(hasTestTag("detailed_profile_bank_mfs_db_card"))
        composeRule.onNodeWithText("India • 8 banks (3 Govt • 5 Private)").assertIsDisplayed()
        composeRule.onNodeWithText("Paytm, PhonePe, Google Pay", substring = true).assertIsDisplayed()

        // Open Local Bank Directory dialog -> verifies Indian banks (SBI, HDFC), NOT Bangladesh banks
        composeRule.onNodeWithTag("bank_directory_updates_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("State Bank of India (SBI)").assertIsDisplayed()
        composeRule.onNodeWithText("Sonali Bank").assertDoesNotExist()
        composeRule.onNodeWithText("Close").performClick()
        composeRule.waitForIdle()

        // Open MFS Provider Directory dialog -> verifies Indian UPI/wallets (Paytm, PhonePe, BHIM UPI), NOT bKash
        composeRule.onNodeWithTag("mfs_directory_browse_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Paytm (UPI & Wallet)").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("PhonePe (UPI)").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("BHIM UPI (NPCI)").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("bKash", substring = true).assertDoesNotExist()
        composeRule.onNodeWithText("Close").performClick()
        composeRule.waitForIdle()

        // 3. Switch Country to Kuwait -> verifies NBK/KFH & KNET/MyFatoorah/Weyay
        composeRule.onNodeWithTag("detailed_profile_list")
            .performScrollToNode(hasTestTag("detailed_profile_region_currency_card"))
        composeRule.onNodeWithTag("detailed_profile_country_row")
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("app_country_picker_search").performTextReplacement("Kuwait")
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("app_country_option_KW")
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("detailed_profile_list")
            .performScrollToNode(hasTestTag("detailed_profile_bank_mfs_db_card"))
        composeRule.onNodeWithText("Kuwait • 8 banks", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("KNET, MyFatoorah, Weyay", substring = true).assertIsDisplayed()

        composeRule.onNodeWithTag("mfs_directory_browse_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("KNET (Kuwait National Switch)").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("MyFatoorah (Digital Payments)").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Weyay (NBK Digital)").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Close").performClick()
    }

    @Test
    fun addAndEditAccountWizardSyncsBanksAndMfsWalletsToSelectedCountry() {
        var savedAccount: AccountEntity? = null
        var countryCode by mutableStateOf("KW")
        var editTarget by mutableStateOf<AccountEntity?>(null)

        composeRule.setContent {
            AmarHisabTheme(darkTheme = false, isBangla = false) {
                AddAccountWizardSheet(
                    language = AppLanguage.EN,
                    strings = StringsProvider.english,
                    onDismiss = {},
                    onSaveAccount = { savedAccount = it },
                    defaultCurrencyCode = "KWD",
                    userCountryCode = countryCode,
                    existingAccount = editTarget
                )
            }
        }

        composeRule.waitForIdle()

        // 1. Default country KW loads Kuwait banks (NBK, KFH, Gulf Bank, Boubyan) immediately
        composeRule.onNodeWithTag("wizard_bank_kw_nbk").assertExists()
        composeRule.onNodeWithTag("wizard_bank_kw_kfh").assertExists()
        composeRule.onNodeWithTag("wizard_bank_bd_sonali").assertDoesNotExist()

        // 2. Switch account type to MFS -> automatically loads Kuwait MFS/payment wallets (KNET, MyFatoorah, Weyay) & KWD
        composeRule.onNodeWithTag("wizard_type_MFS")
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("wizard_mfs_kw_knet").assertExists()
        composeRule.onNodeWithTag("wizard_mfs_kw_myfatoorah").assertExists()
        composeRule.onNodeWithTag("wizard_mfs_kw_weyay").assertExists()
        composeRule.onNodeWithTag("wizard_mfs_bd_bkash").assertDoesNotExist()

        composeRule.onNodeWithTag("wizard_mfs_kw_weyay")
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.onNodeWithTag("wizard_balance_input").performScrollTo().performTextReplacement("125.750")
        composeRule.onNodeWithTag("wizard_save_account_button")
            .performScrollTo()
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()

        val firstSaved = requireNotNull(savedAccount)
        assertEquals(AccountType.MFS, firstSaved.type)
        assertEquals("Weyay", firstSaved.bankOrProvider)
        assertEquals("KWD", firstSaved.currencyCode)
        assertEquals(125.75, firstSaved.balance, 0.001)

        // 3. Edit an existing Indian MFS account -> loads Indian UPI/wallets & preserves account ID
        countryCode = "IN"
        editTarget = AccountEntity(
            id = 55L,
            name = "My Paytm",
            type = AccountType.MFS,
            balance = 4500.0,
            bankOrProvider = "Paytm",
            colorArgb = 0xFF002E6EL,
            currencyCode = "INR"
        )
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("wizard_mfs_in_phonepe")
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.onNodeWithTag("wizard_save_account_button")
            .performScrollTo()
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()

        val editedSaved = requireNotNull(savedAccount)
        assertEquals(55L, editedSaved.id)
        assertEquals("PhonePe", editedSaved.bankOrProvider)
        assertEquals("INR", editedSaved.currencyCode)
    }
}
