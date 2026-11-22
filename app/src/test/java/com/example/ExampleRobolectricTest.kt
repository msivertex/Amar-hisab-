package com.example

import android.content.Context
import android.content.Intent
import androidx.compose.ui.test.assertAny
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import androidx.fragment.app.FragmentActivity
import androidx.test.core.app.ApplicationProvider
import android.Manifest
import com.example.core.AppPermissionKind
import com.example.core.BiometricAuthFailureReason
import com.example.core.BiometricAuthHelper
import com.example.core.BiometricAvailability
import com.example.core.BiometricCapabilityInfo
import com.example.core.DevicePermissionsHelper
import com.example.core.DevicePermissionsSnapshot
import com.example.core.DeviceTimeContext
import com.example.core.LocationRegionContext
import com.example.data.ai.InsightsSummaryHelper
import com.example.data.ai.MarketErrorCategory
import com.example.data.export.ExportSharePayload
import com.example.data.export.MonthlyReportExporter
import com.example.data.export.ReportExportFormat
import com.example.data.local.AccountEntity
import com.example.data.local.TransactionEntity
import com.example.data.local.UserPrefs
import com.example.data.market.GoldCarat
import com.example.data.market.MarketCalculators
import com.example.data.market.MarketCurrency
import com.example.data.market.MarketDataRepository
import com.example.data.market.MarketSnapshot
import com.example.data.market.MarketSyncUiState
import com.example.data.market.MarketToolTab
import com.example.data.market.MfsChannel
import com.example.data.model.AccountType
import com.example.data.model.AppColourTheme
import com.example.data.model.AppCurrency
import com.example.data.model.AppThemeMode
import com.example.data.model.AuthMode
import com.example.data.model.BankRegion
import com.example.data.model.CategorySortMode
import com.example.data.model.CategorySpend
import com.example.data.model.DailyTrendPoint
import com.example.data.model.ExpenseCategory
import com.example.data.model.HomeLayoutStyle
import com.example.data.model.HomeSectionId
import com.example.data.model.InsightTimeFilter
import com.example.data.model.InternationalBankCatalog
import com.example.data.model.StartOfWeek
import com.example.data.model.TransactionType
import com.example.data.repository.FinanceRepository
import com.example.data.support.CustomerSupportCatalog
import com.example.data.support.CustomerSupportEngine
import com.example.data.support.SUPPORT_GREETING_REPLY_BN
import com.example.data.support.SUPPORT_INITIAL_GREETING_BN
import com.example.data.support.SUPPORT_OFFLINE_AI_NOTICE_BN
import com.example.data.support.SUPPORT_WHATSAPP_DISPLAY
import com.example.data.support.SupportCategoryId
import com.example.data.support.SupportChatMessage
import com.example.data.support.SupportSender
import com.example.data.support.SupportUiState
import com.example.localization.AppLanguage
import com.example.localization.BdtFormatter
import com.example.localization.StringsProvider
import com.example.ui.screens.AddAccountWizardSheet
import com.example.ui.screens.AppLockScreen
import com.example.ui.screens.BiometricPromptRequest
import com.example.ui.screens.CategoriesManagementSubPage
import com.example.ui.screens.HomeDashboardScreen
import com.example.ui.screens.InsightsScreen
import com.example.ui.screens.LiveMarketScreen
import com.example.ui.screens.ReceiptScanSheet
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.WelcomeScreen
import com.example.ui.viewmodel.AnalyticsSnapshot
import com.example.ui.viewmodel.MainUiState
import com.example.ui.viewmodel.ReceiptScanUiState
import com.example.ui.viewmodel.SearchAiUiState
import com.example.ui.viewmodel.TtsUiState
import androidx.compose.ui.test.performScrollTo
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import com.example.ui.theme.AmarHisabTheme
import com.example.ui.theme.LocalCrystalColors
import com.example.ui.components.resolveAccountCardGradient
import com.example.data.local.UserPreferencesRepository
import com.example.ui.viewmodel.MainViewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.os.Looper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.robolectric.Shadows
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
@Config(sdk = [36])
class ExampleRobolectricTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Amar Hisab", appName)
    }

    @Test
    fun `biometric helper reports valid status and MainActivity supports BiometricPrompt`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val availability = BiometricAuthHelper.checkBiometricAvailability(context)
        assertTrue(availability in BiometricAvailability.entries)
        assertTrue(FragmentActivity::class.java.isAssignableFrom(MainActivity::class.java))
    }

    @Test
    fun `welcome screen shows minimalist uppercase UI and Google or Guest options`() {
        val defaults = UserPrefs()
        assertFalse(defaults.hasCompletedWelcome)
        assertEquals(AuthMode.GUEST, defaults.authMode)

        var guestSelected = false
        composeRule.setContent {
            AmarHisabTheme {
                WelcomeScreen(
                    language = AppLanguage.EN,
                    strings = StringsProvider.english,
                    onGoogleSignedIn = {},
                    onContinueAsGuest = { guestSelected = true }
                )
            }
        }

        // Advance animations so staggered title and bottom buttons settle cleanly
        composeRule.mainClock.advanceTimeBy(1200L)
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("welcome_screen").assertIsDisplayed()
        composeRule.onNodeWithText("আমার হিসাব").assertIsDisplayed()
        composeRule.onNodeWithText("AMAR HISAB").assertIsDisplayed()
        composeRule.onNodeWithText("SMART PERSONAL FINANCE VAULT").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("welcome_google_signin_button").assertIsDisplayed()
        composeRule.onNodeWithText("CONTINUE WITH GOOGLE").assertIsDisplayed()
        composeRule.onNodeWithTag("welcome_guest_button").assertIsDisplayed()
        composeRule.onNodeWithText("CONTINUE AS OFFLINE GUEST").assertIsDisplayed()
        composeRule.onNodeWithTag("welcome_developer_attribution").assertIsDisplayed()
        composeRule.onNodeWithText("Developed by MD Shariful Islam").assertIsDisplayed()
        composeRule.onNodeWithTag("welcome_guest_button").performClick()
        assertTrue(guestSelected)
    }

    @Test
    fun `dark theme uses deep charcoal background and crisp white text`() {
        var bg = Color.Transparent
        var surface = Color.Transparent
        var onBg = Color.Transparent
        var onSurface = Color.Transparent
        var onSurfaceVariant = Color.Transparent
        var muted = Color.Transparent
        var orb = Color.White
        var primary = Color.Transparent

        composeRule.setContent {
            AmarHisabTheme(darkTheme = true) {
                bg = MaterialTheme.colorScheme.background
                surface = MaterialTheme.colorScheme.surface
                onBg = MaterialTheme.colorScheme.onBackground
                onSurface = MaterialTheme.colorScheme.onSurface
                onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
                muted = LocalCrystalColors.current.mutedText
                orb = LocalCrystalColors.current.orbPrimary
                primary = MaterialTheme.colorScheme.primary
            }
        }

        assertEquals(Color(0xFF0B0F19), bg)
        assertEquals(Color(0xFF111622), surface)
        assertEquals(Color(0xFFFFFFFF), onBg)
        assertEquals(Color(0xFFFFFFFF), onSurface)
        assertEquals(Color(0xFFF8FAFC), onSurfaceVariant)
        assertEquals(Color(0xFFF8FAFC), muted)
        assertEquals(Color.Transparent, orb)
        assertEquals(Color(0xFF06B6D4), primary)
    }

    @Test
    fun `receipt scanner modal shows camera and gallery options`() {
        composeRule.setContent {
            AmarHisabTheme(darkTheme = true) {
                ReceiptScanSheet(
                    accounts = emptyList(),
                    receiptState = ReceiptScanUiState.Idle,
                    language = AppLanguage.EN,
                    strings = StringsProvider.english,
                    onPickImageUri = {},
                    onSaveExpense = { _, _, _, _, _ -> },
                    onDismiss = {}
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("scan_receipt_camera_button").assertIsDisplayed()
        composeRule.onNodeWithText("Scan with Camera").assertIsDisplayed()
        composeRule.onNodeWithTag("pick_receipt_photo_button").assertIsDisplayed()
        composeRule.onNodeWithText("Choose Receipt Photo").assertIsDisplayed()
    }

    @Test
    fun `home profile section supports clean avatar settings tap, inline name editing, and dynamic photo controls`() {
        var openedSettings = false
        var savedName = ""
        val guestPrefs = UserPrefs(
            userName = "Tahmid Hasan",
            userAvatarUrl = "https://example.com/google-ignored.jpg",
            guestAvatarUrl = "",
            authMode = AuthMode.GUEST
        )
        assertEquals("", guestPrefs.effectiveAvatarUrl)

        val googlePrefs = guestPrefs.copy(authMode = AuthMode.GOOGLE)
        assertEquals("https://example.com/google-ignored.jpg", googlePrefs.effectiveAvatarUrl)

        composeRule.setContent {
            AmarHisabTheme(darkTheme = true) {
                HomeDashboardScreen(
                    state = MainUiState(prefs = guestPrefs, isPrefsLoaded = true),
                    strings = StringsProvider.english,
                    onOpenSearch = {},
                    onOpenNotifications = {},
                    onOpenSettings = { openedSettings = true },
                    onOpenProfileEdit = {},
                    onOpenAllAccounts = {},
                    onOpenTransfer = {},
                    onOpenReceiptScan = {},
                    onOpenQuickExpense = {},
                    onOpenVoiceEntry = {},
                    onSeeAllActivity = {},
                    onUpdateName = { savedName = it }
                )
            }
        }

        composeRule.waitForIdle()

        // Minimal top header retains Search & Notifications only (no language or theme quick toggles)
        composeRule.onNodeWithTag("global_search_button").assertIsDisplayed()
        composeRule.onNodeWithTag("notifications_button").assertIsDisplayed()
        composeRule.onNodeWithTag("toggle_language_button").assertDoesNotExist()
        composeRule.onNodeWithTag("toggle_theme_button").assertDoesNotExist()

        // Tapping clean avatar opens Settings
        composeRule.onNodeWithTag("profile_settings_button").assertIsDisplayed().performClick()
        assertTrue(openedSettings)

        // Tapping name / edit icon expands inline frosted editor with Guest photo upload
        composeRule.onNodeWithTag("profile_name_edit_trigger").assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("inline_profile_editor_card").assertIsDisplayed()
        composeRule.onNodeWithTag("home_guest_upload_avatar_button").assertIsDisplayed()
        composeRule.onNodeWithTag("inline_name_input").performTextReplacement("Nusrat Jahan")
        composeRule.onNodeWithTag("inline_name_save_button").performClick()

        assertEquals("Nusrat Jahan", savedName)
    }

    @Test
    fun `home profile inline editor shows google synced avatar note for Google users`() {
        val googlePrefs = UserPrefs(
            userName = "Nusrat Jahan",
            userEmail = "nusrat@gmail.com",
            userAvatarUrl = "https://example.com/avatar.png",
            authMode = AuthMode.GOOGLE
        )

        composeRule.setContent {
            AmarHisabTheme(darkTheme = false) {
                HomeDashboardScreen(
                    state = MainUiState(prefs = googlePrefs, isPrefsLoaded = true),
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
        composeRule.onNodeWithTag("profile_name_edit_trigger").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Google account photo synced automatically").assertIsDisplayed()
        composeRule.onNodeWithTag("home_guest_upload_avatar_button").assertDoesNotExist()
    }

    @Test
    fun `settings profile card and detailed profile subpage match finalized layout and support editing`() {
        var savedName = ""
        var selectedPreset = ""
        val googleWithOverride = UserPrefs(
            userName = "Tahmid Hasan",
            userEmail = "tahmid@gmail.com",
            userAvatarUrl = "https://example.com/google.jpg",
            googleCustomAvatarUrl = "preset:sky_vault",
            authMode = AuthMode.GOOGLE
        )
        assertEquals("preset:sky_vault", googleWithOverride.effectiveAvatarUrl)
        assertEquals("https://example.com/google.jpg", googleWithOverride.copy(googleCustomAvatarUrl = "").effectiveAvatarUrl)

        // Default UserPrefs must never hardcode a personal name like "Tahmid Hasan"
        val freshDefaults = UserPrefs()
        assertEquals("", freshDefaults.userName)
        assertEquals("Guest", StringsProvider.english.profileDetails.guestUserFallback)
        assertEquals("গেস্ট ইউজার", StringsProvider.bangla.profileDetails.guestUserFallback)

        val guestPrefs = UserPrefs(
            language = AppLanguage.BN,
            authMode = AuthMode.GUEST
        )

        composeRule.setContent {
            AmarHisabTheme(darkTheme = true) {
                SettingsScreen(
                    prefs = guestPrefs,
                    accounts = emptyList(),
                    backupStatus = null,
                    strings = StringsProvider.bangla,
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
                    onUpdateDisplayName = { savedName = it },
                    onSelectPresetAvatar = { selectedPreset = it }
                )
            }
        }

        composeRule.waitForIdle()
        // 1. Main Settings Profile Card defaults to "গেস্ট ইউজার" and neutral Guest avatar icon
        composeRule.onNodeWithTag("settings_profile_card").assertIsDisplayed()
        composeRule.onNodeWithText("গেস্ট ইউজার").assertIsDisplayed()
        composeRule.onNodeWithText("Tahmid Hasan").assertDoesNotExist()
        composeRule.onNodeWithTag("default_guest_avatar_icon", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithText("Bangladesh").assertIsDisplayed()
        composeRule.onNodeWithText("BDT (৳)").assertIsDisplayed()
        composeRule.onNodeWithText("গেস্ট মোড (অফলাইন) - ক্লাউড ব্যাকআপ ও মাল্টি-ডিভাইস সিঙ্ক চালু করতে গুগল সাইন ইন করুন")
            .assertIsDisplayed()
        composeRule.onNodeWithTag("settings_profile_google_signin_button").assertIsDisplayed()
        composeRule.onNodeWithText("গুগল দিয়ে সাইন ইন করুন").assertIsDisplayed()

        // Confirm lower Security & System section has no duplicate Google Sign-In row
        composeRule.onNodeWithTag("settings_list")
            .performScrollToNode(hasTestTag("settings_security_card"))
        composeRule.onNodeWithTag("settings_security_card").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_google_signin_button").assertDoesNotExist()
        composeRule.onNodeWithTag("settings_unlink_google_button").assertDoesNotExist()

        // 2. Tap arrow to enter Detailed Profile Sub-Page
        composeRule.onNodeWithTag("settings_list")
            .performScrollToNode(hasTestTag("settings_profile_card"))
        composeRule.onNodeWithTag("settings_open_detailed_profile_button").assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("detailed_profile_screen").assertIsDisplayed()

        // Prominent avatar + User Information ("ব্যবহারকারীর তথ্য") + Google Account status + Region/Currencies
        composeRule.onNodeWithTag("detailed_profile_avatar_edit_button").assertIsDisplayed()
        composeRule.onNodeWithTag("detailed_profile_list")
            .performScrollToNode(hasTestTag("detailed_profile_user_info_card"))
        composeRule.onNodeWithText("ব্যবহারকারীর তথ্য").assertIsDisplayed()
        composeRule.onNodeWithText("সম্পাদনা").assertIsDisplayed()

        composeRule.onNodeWithTag("detailed_profile_list")
            .performScrollToNode(hasTestTag("detailed_profile_google_card"))
        composeRule.onNodeWithTag("detailed_profile_google_card").assertIsDisplayed()
        composeRule.onNodeWithText("সাইন ইন করা নেই (গেস্ট মোড)").assertIsDisplayed()
        composeRule.onNodeWithTag("detailed_profile_google_signin_button").assertIsDisplayed()
        composeRule.onNodeWithText("গুগল সাইন ইন").assertIsDisplayed()

        composeRule.onNodeWithTag("detailed_profile_list")
            .performScrollToNode(hasTestTag("detailed_profile_region_currency_card"))
        composeRule.onNodeWithTag("detailed_profile_region_currency_card").assertIsDisplayed()
        composeRule.onNodeWithText("Bangladesh").assertIsDisplayed()
        composeRule.onNodeWithText("BDT (৳)").assertIsDisplayed()
        composeRule.onNodeWithText("USD ($)").assertIsDisplayed()

        // Database & Extra Section ("ব্যাংক ও এমএফএস ডাটাবেস")
        composeRule.onNodeWithTag("detailed_profile_list")
            .performScrollToNode(hasTestTag("detailed_profile_bank_mfs_db_card"))
        composeRule.onNodeWithTag("detailed_profile_bank_mfs_db_card").assertIsDisplayed()
        composeRule.onNodeWithText("ব্যাংক ও এমএফএস ডাটাবেস").assertIsDisplayed()
        composeRule.onNodeWithText("স্থানীয় ব্যাংক ডিরেক্টরি").assertIsDisplayed()
        composeRule.onNodeWithText("এমএফএস প্রদানকারী ডিরেক্টরি").assertIsDisplayed()

        composeRule.onNodeWithTag("bank_directory_updates_button").assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("bank_directory_dialog_content").assertIsDisplayed()
        composeRule.onNodeWithText("বন্ধ করুন").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("mfs_directory_browse_button").assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("mfs_directory_dialog_content").assertIsDisplayed()
        composeRule.onNodeWithText("বন্ধ করুন").performClick()
        composeRule.waitForIdle()

        // Inline name editing via "সম্পাদনা"
        composeRule.onNodeWithTag("detailed_profile_list")
            .performScrollToNode(hasTestTag("detailed_profile_user_info_card"))
        composeRule.onNodeWithTag("detailed_profile_edit_name_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("detailed_profile_name_input").performTextReplacement("Farhana Rahman")
        composeRule.onNodeWithTag("detailed_profile_name_save_button").performClick()
        assertEquals("Farhana Rahman", savedName)

        // Avatar edit opens 3-option bottom sheet with gallery, camera, and preset avatars
        composeRule.onNodeWithTag("detailed_profile_list")
            .performScrollToNode(hasTestTag("detailed_profile_hero_card"))
        composeRule.onNodeWithTag("detailed_profile_avatar_edit_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("avatar_selector_sheet").assertIsDisplayed()
        composeRule.onNodeWithTag("avatar_option_upload_photo").assertIsDisplayed()
        composeRule.onNodeWithTag("avatar_option_take_photo").assertIsDisplayed()
        composeRule.onNodeWithTag("avatar_option_choose_avatar").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("preset_avatar_grid").assertIsDisplayed()
        composeRule.onNodeWithTag("preset_avatar_preset:taka_emblem").performClick()
        assertEquals("preset:taka_emblem", selectedPreset)
    }

    @Test
    fun `wallet card stack shows top-peeking cards and tapping peeking card brings it to front`() {
        val sampleAccounts = listOf(
            AccountEntity(
                id = 1L,
                name = "City Bank Salary",
                type = AccountType.DEBIT_BANK,
                balance = 74500.0,
                bankOrProvider = "City Bank",
                cardNetwork = "Visa",
                accountNumberLast4 = "4829",
                colorArgb = 0xFFC8102EL
            ),
            AccountEntity(
                id = 2L,
                name = "bKash Personal",
                type = AccountType.MFS,
                balance = 14250.0,
                bankOrProvider = "bKash",
                accountNumberLast4 = "8429",
                colorArgb = 0xFFE2136EL
            ),
            AccountEntity(
                id = 3L,
                name = "BRAC Platinum Card",
                type = AccountType.CREDIT_CARD,
                balance = 16500.0,
                creditLimit = 100000.0,
                bankOrProvider = "BRAC Bank",
                cardNetwork = "Mastercard",
                accountNumberLast4 = "9042",
                colorArgb = 0xFF0072BCL
            )
        )

        composeRule.setContent {
            AmarHisabTheme(darkTheme = true) {
                HomeDashboardScreen(
                    state = MainUiState(
                        isPrefsLoaded = true,
                        prefs = UserPrefs(homeLayoutStyle = HomeLayoutStyle.WALLET_STACK),
                        accounts = sampleAccounts,
                        totalLiquidBalance = 88750.0
                    ),
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
        composeRule.onNodeWithTag("home_dashboard_list")
            .performScrollToNode(hasTestTag("wallet_stack_container"))
        composeRule.onNodeWithTag("wallet_stack_container").assertIsDisplayed()
        composeRule.onNodeWithTag("wallet_stack_carousel").assertIsDisplayed()
        // Unified frosted container header shows "+2 more" on top-left and "All accounts >" on top-right
        val moreBadgeBounds = composeRule.onNodeWithTag("more_wallets_badge").assertIsDisplayed().getUnclippedBoundsInRoot()
        val allAccountsBounds = composeRule.onNodeWithTag("all_accounts_button").assertIsDisplayed().getUnclippedBoundsInRoot()
        assertTrue(moreBadgeBounds.left < allAccountsBounds.left)
        // Front card uses compact realistic debit/credit card height (<= 196.dp)
        val frontCardBounds = composeRule.onNodeWithTag("wallet_stack_front_card").getUnclippedBoundsInRoot()
        assertTrue(frontCardBounds.height <= 198.dp)

        // All 3 cards in stack are visible: front card + 2 peeking background cards
        composeRule.onNodeWithTag("wallet_stack_card_1").assertIsDisplayed()
        composeRule.onNodeWithTag("wallet_stack_card_2").assertIsDisplayed()
        composeRule.onNodeWithTag("wallet_stack_card_3").assertIsDisplayed()
        // Front card displays full masked number for account 1 initially
        composeRule.onNodeWithText("••••  ••••  ••••  4829").assertIsDisplayed()

        // Tap peeking card 3 (BRAC Platinum Card) -> smoothly brings card 3 to the front!
        composeRule.onNodeWithTag("wallet_stack_card_3").performClick()
        composeRule.mainClock.advanceTimeBy(600L)
        composeRule.waitForIdle()

        // Now card 3 is front active card showing its masked account number & credit limit
        composeRule.onNodeWithText("••••  ••••  ••••  9042").assertIsDisplayed()
    }

    @Test
    fun `home dashboard switches cleanly to balance card view when selected`() {
        val sampleAccounts = listOf(
            AccountEntity(
                id = 1L,
                name = "Cash Wallet",
                type = AccountType.CASH,
                balance = 8450.0,
                bankOrProvider = "Cash",
                colorArgb = 0xFF0D9488L
            )
        )

        composeRule.setContent {
            AmarHisabTheme(darkTheme = true) {
                HomeDashboardScreen(
                    state = MainUiState(
                        isPrefsLoaded = true,
                        prefs = UserPrefs(homeLayoutStyle = HomeLayoutStyle.BALANCE_CARD),
                        accounts = sampleAccounts,
                        totalLiquidBalance = 8450.0,
                        incomeThisMonth = 50000.0,
                        spentThisMonth = 12000.0
                    ),
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
        composeRule.onNodeWithTag("home_dashboard_list")
            .performScrollToNode(hasTestTag("compact_wallet_strip"))
        composeRule.onNodeWithTag("compact_wallet_strip").assertIsDisplayed()
        composeRule.onNodeWithTag("wallet_stack_carousel").assertDoesNotExist()
    }

    @Test
    fun `settings customize home opens bottom sheet with layout style selector, toggles, and reordering`() {
        var latestStyle = HomeLayoutStyle.WALLET_STACK
        var latestOrder: List<HomeSectionId> = HomeSectionId.entries
        var latestHidden = emptySet<HomeSectionId>()

        composeRule.setContent {
            AmarHisabTheme(darkTheme = true) {
                SettingsScreen(
                    prefs = UserPrefs(isDarkTheme = true),
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
                    onUpdateDashboardLayout = { style, order, hidden ->
                        latestStyle = style
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
        composeRule.onNodeWithTag("customize_home_button").performScrollTo().performClick()
        composeRule.waitForIdle()

        // Bottom sheet opens with Visual Layout Style Selector and all 6 sections
        composeRule.onNodeWithTag("dashboard_customizer_sheet").assertIsDisplayed()
        composeRule.onNodeWithTag("layout_option_balance_card").assertIsDisplayed()
        composeRule.onNodeWithTag("layout_option_wallet_stack").assertIsDisplayed()
        HomeSectionId.entries.forEach { section ->
            composeRule.onNodeWithTag("customizer_section_row_${section.name}")
                .performScrollTo()
                .assertIsDisplayed()
        }

        // Toggle layout style to Balance Card
        composeRule.onNodeWithTag("layout_option_balance_card").performClick()
        composeRule.waitForIdle()
        assertEquals(HomeLayoutStyle.BALANCE_CARD, latestStyle)

        // Move BUDGET up from index 2 to index 1
        composeRule.onNodeWithTag("move_up_BUDGET").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertEquals(HomeSectionId.BUDGET, latestOrder[1])

        // Toggle STREAK visibility off
        composeRule.onNodeWithTag("toggle_section_STREAK").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertTrue(HomeSectionId.STREAK in latestHidden)

        // Done dismisses sheet cleanly
        composeRule.onNodeWithTag("customizer_done_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("dashboard_customizer_sheet").assertDoesNotExist()
    }

    @Test
    fun `settings theme tile opens dedicated theme customization screen with live preview, mode buttons, and colour themes`() {
        var selectedMode = AppThemeMode.LIGHT
        var selectedColour = AppColourTheme.SKY_CYAN

        composeRule.setContent {
            AmarHisabTheme(darkTheme = false, colourTheme = AppColourTheme.SKY_CYAN) {
                SettingsScreen(
                    prefs = UserPrefs(
                        language = AppLanguage.BN,
                        isDarkTheme = false,
                        themeMode = AppThemeMode.LIGHT,
                        colourTheme = AppColourTheme.SKY_CYAN
                    ),
                    accounts = emptyList(),
                    backupStatus = null,
                    strings = StringsProvider.bangla,
                    onBack = {},
                    onEditProfileBudget = {},
                    onLinkGoogleProfile = {},
                    onUnlinkGoogleAccount = {},
                    onOpenAccounts = {},
                    onSetLanguage = {},
                    onSetDarkTheme = {},
                    onSetThemeMode = { selectedMode = it },
                    onSetColourTheme = { selectedColour = it },
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
        // Settings displays the dedicated "থিম" tile with "Sky Cyan · Light" subtitle
        composeRule.onNodeWithTag("settings_theme_tile").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("থিম").assertIsDisplayed()
        composeRule.onNodeWithText("Sky Cyan · Light").assertIsDisplayed()

        // Tap Theme tile -> navigates smoothly to ThemeCustomizationScreen
        composeRule.onNodeWithTag("settings_theme_tile").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("theme_customization_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("theme_live_preview_card").assertIsDisplayed()

        // Mode selection buttons: Auto, Light, Dark
        composeRule.onNodeWithTag("theme_customization_list")
            .performScrollToNode(hasTestTag("theme_mode_AUTO"))
        composeRule.onNodeWithTag("theme_mode_AUTO").assertIsDisplayed()
        composeRule.onNodeWithTag("theme_mode_LIGHT").assertIsDisplayed()
        composeRule.onNodeWithTag("theme_mode_DARK").assertIsDisplayed().performClick()
        assertEquals(AppThemeMode.DARK, selectedMode)

        // Colour Theme options: Indigo ("দ্য হিসাব পাতি ব্র্যান্ড" / "The Hisab Pati brand"), Bangla Green, etc.
        composeRule.onNodeWithTag("theme_customization_list")
            .performScrollToNode(hasTestTag("colour_theme_INDIGO"))
        composeRule.onNodeWithTag("colour_theme_INDIGO").assertIsDisplayed().performClick()
        assertEquals(AppColourTheme.INDIGO, selectedColour)

        composeRule.onNodeWithTag("theme_customization_list")
            .performScrollToNode(hasTestTag("colour_theme_BANGLA_GREEN"))
        composeRule.onNodeWithTag("colour_theme_BANGLA_GREEN").assertIsDisplayed().performClick()
        assertEquals(AppColourTheme.BANGLA_GREEN, selectedColour)
    }

    @Test
    fun `bangla home customization sheet shows top of home options and all 9 sections`() {
        composeRule.setContent {
            AmarHisabTheme(darkTheme = true, colourTheme = AppColourTheme.INDIGO) {
                SettingsScreen(
                    prefs = UserPrefs(
                        language = AppLanguage.BN,
                        isDarkTheme = true,
                        themeMode = AppThemeMode.DARK,
                        colourTheme = AppColourTheme.INDIGO
                    ),
                    accounts = emptyList(),
                    backupStatus = null,
                    strings = StringsProvider.bangla,
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
        composeRule.onNodeWithTag("customize_home_button").performScrollTo().performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("dashboard_customizer_sheet").assertIsDisplayed()
        composeRule.onAllNodesWithText("হোম কাস্টমাইজেশন করুন").assertAny(hasText("হোম কাস্টমাইজেশন করুন"))
        composeRule.onNodeWithText("হোমের শীর্ষে").assertIsDisplayed()
        composeRule.onNodeWithTag("layout_option_balance_card").assertIsDisplayed()
        composeRule.onNodeWithTag("layout_option_wallet_stack").assertIsDisplayed()
        composeRule.onAllNodesWithText("ব্যালেন্স কার্ড").assertAny(hasText("ব্যালেন্স কার্ড"))
        composeRule.onNodeWithText("কার্ড স্ট্যাক").assertIsDisplayed()
        composeRule.onNodeWithText("সেকশনসমূহ").assertIsDisplayed()

        composeRule.onNodeWithTag("customizer_section_row_UPCOMING_BILLS")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("আসন্ন বিলসমূহ").assertIsDisplayed()

        composeRule.onNodeWithTag("customizer_section_row_GOALS")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("সঞ্চয় লক্ষ্য").assertIsDisplayed()

        composeRule.onNodeWithTag("customizer_section_row_FINANCIAL_HEALTH")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("আর্থিক স্বাস্থ্য").assertIsDisplayed()
    }

    @Test
    fun `selecting balance card in customizer switches home dashboard cleanly even when summary was hidden`() {
        val sampleAccounts = listOf(
            AccountEntity(
                id = 1L,
                name = "City Bank Salary",
                type = AccountType.DEBIT_BANK,
                balance = 74500.0,
                bankOrProvider = "City Bank",
                cardNetwork = "Visa",
                accountNumberLast4 = "4829",
                colorArgb = 0xFFC8102EL
            ),
            AccountEntity(
                id = 2L,
                name = "bKash Personal",
                type = AccountType.MFS,
                balance = 14250.0,
                bankOrProvider = "bKash",
                accountNumberLast4 = "8429",
                colorArgb = 0xFFE2136EL
            )
        )

        var currentPrefs by mutableStateOf(
            UserPrefs(
                language = AppLanguage.BN,
                isDarkTheme = true,
                themeMode = AppThemeMode.DARK,
                homeLayoutStyle = HomeLayoutStyle.WALLET_STACK,
                hiddenHomeSections = setOf(HomeSectionId.SUMMARY)
            )
        )
        var showSettings by mutableStateOf(true)

        composeRule.setContent {
            AmarHisabTheme(darkTheme = true) {
                if (showSettings) {
                    SettingsScreen(
                        prefs = currentPrefs,
                        accounts = sampleAccounts,
                        backupStatus = null,
                        strings = StringsProvider.bangla,
                        onBack = { showSettings = false },
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
                        onUpdateDashboardLayout = { style, order, hidden ->
                            currentPrefs = currentPrefs.copy(
                                homeLayoutStyle = style,
                                homeSectionOrder = order,
                                hiddenHomeSections = hidden
                            )
                        },
                        onSetAppLock = { _, _ -> },
                        onSetBiometricUnlock = {},
                        onLockNow = {},
                        onExportJson = {},
                        onExportCsv = {},
                        onRestoreJson = {},
                        onClearBackupStatus = {}
                    )
                } else {
                    HomeDashboardScreen(
                        state = MainUiState(
                            isPrefsLoaded = true,
                            prefs = currentPrefs,
                            accounts = sampleAccounts,
                            totalLiquidBalance = 88750.0,
                            incomeThisMonth = 95000.0,
                            spentThisMonth = 18500.0
                        ),
                        strings = StringsProvider.bangla,
                        onOpenSearch = {},
                        onOpenNotifications = {},
                        onOpenSettings = { showSettings = true },
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
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("customize_home_button").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("dashboard_customizer_sheet").assertIsDisplayed()

        // Tap "ব্যালেন্স কার্ড" (Balance Card)
        composeRule.onNodeWithTag("layout_option_balance_card").performClick()
        composeRule.waitForIdle()
        assertEquals(HomeLayoutStyle.BALANCE_CARD, currentPrefs.homeLayoutStyle)
        assertFalse(HomeSectionId.SUMMARY in currentPrefs.hiddenHomeSections)

        // Tap Done & navigate back to Home Dashboard
        composeRule.onNodeWithTag("customizer_done_button").performClick()
        composeRule.waitForIdle()
        showSettings = false
        composeRule.waitForIdle()

        // Home renders focused Balance Card + budget overview + compact wallet strip (and no card stack)
        composeRule.onNodeWithTag("hero_balance_card").assertIsDisplayed()
        composeRule.onNodeWithTag("hero_balance_budget_overview", useUnmergedTree = true)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag("home_dashboard_list")
            .performScrollToNode(hasTestTag("compact_wallet_strip"))
        composeRule.onNodeWithTag("compact_wallet_strip").assertIsDisplayed()
        composeRule.onNodeWithTag("wallet_stack_carousel").assertDoesNotExist()
    }

    @Test
    fun `main viewmodel updates balance card layout synchronously and persists to datastore`() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val prefsRepo = UserPreferencesRepository(app)
        val viewModel = MainViewModel(app)
        var latestLayout = HomeLayoutStyle.WALLET_STACK

        composeRule.setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            latestLayout = state.prefs.homeLayoutStyle
            AmarHisabTheme(darkTheme = true) {
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
        viewModel.updateHomeDashboardLayout(
            layoutStyle = HomeLayoutStyle.BALANCE_CARD,
            order = HomeSectionId.entries,
            hidden = setOf(HomeSectionId.STREAK)
        )
        composeRule.waitUntil(timeoutMillis = 5000L) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            latestLayout == HomeLayoutStyle.BALANCE_CARD
        }
        composeRule.onNodeWithTag("hero_balance_card").assertIsDisplayed()
        composeRule.onNodeWithTag("wallet_stack_carousel").assertDoesNotExist()

        composeRule.waitUntil(timeoutMillis = 5000L) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            runBlocking(Dispatchers.IO) {
                val p = prefsRepo.userPrefs.first()
                p.homeLayoutStyle == HomeLayoutStyle.BALANCE_CARD && HomeSectionId.STREAK in p.hiddenHomeSections
            }
        }

        // Restore default WALLET_STACK cleanly
        viewModel.updateHomeDashboardLayout(
            layoutStyle = HomeLayoutStyle.WALLET_STACK,
            order = HomeSectionId.entries,
            hidden = emptySet()
        )
        composeRule.waitUntil(timeoutMillis = 5000L) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            latestLayout == HomeLayoutStyle.WALLET_STACK
        }
    }

    @Test
    fun `settings backup and restore tile opens dedicated subpage with local, cloud, and danger zone sections`() {
        var deleteAllTriggered = false

        composeRule.setContent {
            AmarHisabTheme(darkTheme = true) {
                SettingsScreen(
                    prefs = UserPrefs(
                        language = AppLanguage.BN,
                        isDarkTheme = true,
                        themeMode = AppThemeMode.DARK,
                        authMode = AuthMode.GUEST
                    ),
                    accounts = emptyList(),
                    backupStatus = null,
                    strings = StringsProvider.bangla,
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
                    onDeleteAllFinancialData = { deleteAllTriggered = true }
                )
            }
        }

        composeRule.waitForIdle()

        // 1. Settings list displays dedicated Backup & Restore tile
        composeRule.onNodeWithTag("settings_list")
            .performScrollToNode(hasTestTag("settings_backup_restore_tile"))
        composeRule.onNodeWithTag("settings_backup_restore_tile").assertIsDisplayed()
        composeRule.onNodeWithText("ব্যাকআপ ও রিস্টোর").assertIsDisplayed()
        composeRule.onNodeWithText("এক্সপোর্ট, রিস্টোর, ক্লাউড ব্যাকআপ ও সিঙ্ক").assertIsDisplayed()

        // 2. Tap tile -> navigates to BackupRestoreSubPage
        composeRule.onNodeWithTag("settings_backup_restore_tile").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("backup_restore_screen").assertIsDisplayed()
        composeRule.onNodeWithText("ব্যাকআপ ও রিস্টোর").assertIsDisplayed()
        composeRule.onNodeWithText("ডাটা ব্যাকআপ, রিস্টোর ও সিঙ্ক").assertIsDisplayed()

        // Local Backup & Restore section
        composeRule.onNodeWithTag("backup_restore_list")
            .performScrollToNode(hasTestTag("backup_local_section_card"))
        composeRule.onNodeWithText("লোকাল ব্যাকআপ ও রিস্টোর").assertIsDisplayed()
        composeRule.onNodeWithText("ডাটা এক্সপোর্ট").assertIsDisplayed()
        composeRule.onNodeWithTag("backup_export_json_button").assertIsDisplayed()
        composeRule.onNodeWithTag("backup_export_csv_button").assertIsDisplayed()
        composeRule.onNodeWithText("ইম্পোর্ট ও রিস্টোর").assertIsDisplayed()
        composeRule.onNodeWithTag("backup_import_restore_button").assertIsDisplayed()
        composeRule.onNodeWithText("নমুনা টেম্পলেট").assertIsDisplayed()
        composeRule.onNodeWithTag("backup_template_download_button").assertIsDisplayed()
        composeRule.onNodeWithTag("backup_template_share_button").assertIsDisplayed()

        // Cloud Backup & Sync section
        composeRule.onNodeWithTag("backup_restore_list")
            .performScrollToNode(hasTestTag("backup_cloud_section_card"))
        composeRule.onNodeWithText("ক্লাউড ব্যাকআপ ও সিঙ্ক").assertIsDisplayed()
        composeRule.onNodeWithTag("backup_google_signin_required_card").assertIsDisplayed()
        composeRule.onNodeWithText("গুগল সাইন-ইন আবশ্যক").assertIsDisplayed()
        composeRule.onNodeWithTag("backup_cloud_google_signin_button").assertIsDisplayed()

        // Danger Zone section + confirmation dialog
        composeRule.onNodeWithTag("backup_restore_list")
            .performScrollToNode(hasTestTag("backup_danger_zone_card"))
        composeRule.onNodeWithTag("backup_danger_zone_card").assertIsDisplayed()
        composeRule.onNodeWithText("বিপদ অঞ্চল").assertIsDisplayed()
        composeRule.onNodeWithText("ডিভাইসের সব ওয়ালেট ও লেনদেন স্থায়ীভাবে ডিলিট হবে").assertIsDisplayed()
        composeRule.onNodeWithTag("backup_delete_all_data_button").assertIsDisplayed().performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("delete_all_confirm_dialog").assertIsDisplayed()
        composeRule.onNodeWithTag("confirm_delete_all_data_button").performClick()
        composeRule.waitForIdle()
        assertTrue(deleteAllTriggered)
    }

    @Test
    fun `balance card mode preserves vibrant brand gradients, masked numbers, and contrast across light and dark modes`() {
        val bkash = AccountEntity(
            id = 1L,
            name = "bKash Personal",
            type = AccountType.MFS,
            balance = 14250.0,
            bankOrProvider = "bKash",
            accountNumberLast4 = "01711-8429",
            colorArgb = 0xFFE2136EL
        )
        val nagad = AccountEntity(
            id = 2L,
            name = "Nagad Wallet",
            type = AccountType.MFS,
            balance = 6800.0,
            bankOrProvider = "Nagad",
            accountNumberLast4 = "01819-3310",
            colorArgb = 0xFFEA580CL
        )
        val trustBank = AccountEntity(
            id = 3L,
            name = "Trust Bank Savings",
            type = AccountType.DEBIT_BANK,
            balance = 42000.0,
            bankOrProvider = "Trust Bank",
            cardNetwork = "Visa",
            accountNumberLast4 = "7721",
            colorArgb = 0xFF1E5631L
        )
        val cash = AccountEntity(
            id = 4L,
            name = "Cash Wallet",
            type = AccountType.CASH,
            balance = 8450.0,
            bankOrProvider = "Cash",
            colorArgb = 0xFF0D9488L
        )

        // Verify distinct vibrant brand gradients are preserved for Balance Card wallets
        assertEquals(Color(0xFFE91E79), resolveAccountCardGradient(bkash).first())
        assertEquals(Color(0xFFFB923C), resolveAccountCardGradient(nagad).first())
        assertEquals(Color(0xFF16A34A), resolveAccountCardGradient(trustBank).first())
        assertEquals(Color(0xFF10B981), resolveAccountCardGradient(cash).first())

        var useDark by mutableStateOf(false)
        composeRule.setContent {
            AmarHisabTheme(darkTheme = useDark) {
                HomeDashboardScreen(
                    state = MainUiState(
                        isPrefsLoaded = true,
                        prefs = UserPrefs(
                            isDarkTheme = useDark,
                            homeLayoutStyle = HomeLayoutStyle.BALANCE_CARD,
                            monthlyBudget = 50000.0
                        ),
                        accounts = listOf(bkash, nagad, trustBank, cash),
                        totalLiquidBalance = 71500.0,
                        incomeThisMonth = 85000.0,
                        spentThisMonth = 18500.0
                    ),
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

        // Light mode assertions
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("hero_balance_card").assertIsDisplayed()
        composeRule.onNodeWithTag("home_dashboard_list")
            .performScrollToNode(hasTestTag("compact_wallet_strip"))
        composeRule.onNodeWithTag("compact_wallet_strip").assertIsDisplayed()
        composeRule.onNodeWithTag("wallet_stack_carousel").assertDoesNotExist()
        composeRule.onNodeWithTag("compact_wallet_card_1").assertIsDisplayed()
        composeRule.onNodeWithText("bKash Personal").assertIsDisplayed()
        composeRule.onNodeWithText("•••• 8429").assertIsDisplayed()

        // Verify sleek horizontal carousel proportions (~82-88% of strip width & ~166dp height)
        val stripBounds = composeRule.onNodeWithTag("compact_wallet_strip").getUnclippedBoundsInRoot()
        val firstCardBounds = composeRule.onNodeWithTag("compact_wallet_card_1").getUnclippedBoundsInRoot()
        val widthRatio = firstCardBounds.width.value / stripBounds.width.value
        assertTrue("Expected card width ratio ~0.82-0.90, got $widthRatio", widthRatio in 0.80f..0.92f)
        assertTrue("Expected sleek card height ~160-170dp, got ${firstCardBounds.height}", firstCardBounds.height in 160.dp..172.dp)

        // Scroll to Nagad card
        composeRule.onNodeWithTag("compact_wallet_strip")
            .performScrollToNode(hasTestTag("compact_wallet_card_2"))
        composeRule.onNodeWithTag("compact_wallet_card_2").assertIsDisplayed()
        composeRule.onNodeWithText("Nagad Wallet").assertIsDisplayed()
        composeRule.onNodeWithText("•••• 3310").assertIsDisplayed()

        // Scroll to Trust Bank card
        composeRule.onNodeWithTag("compact_wallet_strip")
            .performScrollToNode(hasTestTag("compact_wallet_card_3"))
        composeRule.onNodeWithTag("compact_wallet_card_3").assertIsDisplayed()
        composeRule.onNodeWithText("Trust Bank Savings").assertIsDisplayed()
        composeRule.onNodeWithText("•••• 7721").assertIsDisplayed()

        // Scroll to Cash card and confirm duplicate uppercase "CASH" badge is removed
        composeRule.onNodeWithTag("compact_wallet_strip")
            .performScrollToNode(hasTestTag("compact_wallet_card_4"))
        composeRule.onNodeWithTag("compact_wallet_card_4").assertIsDisplayed()
        composeRule.onNodeWithText("Cash Wallet").assertIsDisplayed()
        composeRule.onNodeWithText("CASH").assertDoesNotExist()

        // Switch to Dark mode and verify both hero balance card & branded wallet strip remain displayed
        useDark = true
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("compact_wallet_card_4").assertIsDisplayed()
        composeRule.onNodeWithTag("home_dashboard_list")
            .performScrollToNode(hasTestTag("hero_balance_card"))
        composeRule.onNodeWithTag("hero_balance_card").assertIsDisplayed()
    }

    @Test
    fun `settings displays finance alert toggles with subtext, removes stray toggles, and navigates to language subpage`() {
        var currentPrefs by mutableStateOf(
            UserPrefs(
                language = AppLanguage.BN,
                isDarkTheme = true,
                themeMode = AppThemeMode.DARK,
                notifBudgetAlerts = true,
                notifCreditAlerts = true,
                notifLoanReminders = true
            )
        )

        composeRule.setContent {
            val activeStrings = StringsProvider.get(currentPrefs.language)
            AmarHisabTheme(darkTheme = true, isBangla = currentPrefs.language == AppLanguage.BN) {
                SettingsScreen(
                    prefs = currentPrefs,
                    accounts = emptyList(),
                    backupStatus = null,
                    strings = activeStrings,
                    onBack = {},
                    onEditProfileBudget = {},
                    onLinkGoogleProfile = {},
                    onUnlinkGoogleAccount = {},
                    onOpenAccounts = {},
                    onSetLanguage = { currentPrefs = currentPrefs.copy(language = it) },
                    onSetDarkTheme = {},
                    onSetEnableSubcategories = {},
                    onSetAutoOpenKeypad = {},
                    onSetDefaultAccount = {},
                    onSetStartOfWeek = {},
                    onSetHapticFeedback = {},
                    onSetNotifications = { b, c, l ->
                        currentPrefs = currentPrefs.copy(
                            notifBudgetAlerts = b,
                            notifCreditAlerts = c,
                            notifLoanReminders = l
                        )
                    },
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

        // Scroll to General & Customization section
        composeRule.onNodeWithTag("settings_list")
            .performScrollToNode(hasTestTag("settings_general_customization_card"))

        // Verify Language list tile with active subtitle "বাংলা"
        composeRule.onNodeWithTag("settings_language_tile").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("ভাষা").assertIsDisplayed()
        composeRule.onNodeWithText("বাংলা").assertIsDisplayed()

        // Verify the 3 essential financial alert toggles with descriptive subtext
        composeRule.onNodeWithTag("settings_toggle_budget_alert").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("বাজেট সীমা সতর্কতা").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_toggle_credit_alert").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("ক্রেডিট কার্ড সতর্কতা").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_toggle_loan_reminder").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("দেনা-পাওনা পরিশোধের স্মারক").assertIsDisplayed()

        // Toggle each financial alert switch and confirm state updates
        composeRule.onNodeWithTag("settings_toggle_budget_alert").performClick()
        composeRule.onNodeWithTag("settings_toggle_credit_alert").performClick()
        composeRule.onNodeWithTag("settings_toggle_loan_reminder").performClick()
        composeRule.waitForIdle()
        assertFalse(currentPrefs.notifBudgetAlerts)
        assertFalse(currentPrefs.notifCreditAlerts)
        assertFalse(currentPrefs.notifLoanReminders)

        // Confirm stray toggles have been removed from the Settings list
        composeRule.onNodeWithText("উপ-খাত (Subcategories) চালু করুন").assertDoesNotExist()
        composeRule.onNodeWithText("স্বয়ংক্রিয় ক্যালকুলেটর কিপ্যাড").assertDoesNotExist()

        // Tap "ভাষা" tile -> navigates to dedicated LanguageSelectionSubPage
        composeRule.onNodeWithTag("settings_language_tile").performScrollTo().performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("language_selection_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("language_option_BN").assertIsDisplayed()
        composeRule.onNodeWithTag("language_check_BN", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithTag("language_option_EN").assertIsDisplayed()
        composeRule.onNodeWithText("ইংরেজি").assertIsDisplayed()

        // Switch to English -> checkmark updates & state persists
        composeRule.onNodeWithTag("language_option_EN").performClick()
        composeRule.waitForIdle()
        assertEquals(AppLanguage.EN, currentPrefs.language)
        composeRule.onNodeWithTag("language_check_EN", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithTag("language_check_BN", useUnmergedTree = true).assertDoesNotExist()

        // Navigate back to Settings and confirm Language tile subtitle reflects "English"
        composeRule.onNodeWithTag("language_back_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("settings_list")
            .performScrollToNode(hasTestTag("settings_general_customization_card"))
        composeRule.onNodeWithTag("settings_language_tile").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Language").assertIsDisplayed()
        composeRule.onNodeWithText("English").assertIsDisplayed()
    }

    @Test
    fun `settings about tile opens centered about subpage with exact bangla copy and developer credits`() {
        var useDark by mutableStateOf(false)

        composeRule.setContent {
            AmarHisabTheme(darkTheme = useDark, isBangla = true) {
                SettingsScreen(
                    prefs = UserPrefs(
                        language = AppLanguage.BN,
                        isDarkTheme = useDark,
                        themeMode = if (useDark) AppThemeMode.DARK else AppThemeMode.LIGHT
                    ),
                    accounts = emptyList(),
                    backupStatus = null,
                    strings = StringsProvider.bangla,
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

        // Scroll to About tile in Settings and open AboutSubPage
        composeRule.onNodeWithTag("settings_list")
            .performScrollToNode(hasTestTag("settings_about_tile"))
        composeRule.onNodeWithTag("settings_about_tile").assertIsDisplayed().performClick()
        composeRule.waitForIdle()

        // Verify top bar, main card, logo, title, tagline, version pill badge, description, credits, and footer
        composeRule.onNodeWithTag("about_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("about_top_bar").assertIsDisplayed()
        composeRule.onNodeWithText("সম্পর্কে").assertIsDisplayed()
        composeRule.onNodeWithTag("about_main_card").assertIsDisplayed()
        composeRule.onNodeWithTag("about_app_logo").assertIsDisplayed()
        composeRule.onNodeWithText("আমার হিসাব").assertIsDisplayed()
        composeRule.onNodeWithText("আপনার অর্থ, আপনার নিয়ন্ত্রণ।").assertIsDisplayed()
        composeRule.onNodeWithTag("about_version_badge").assertIsDisplayed()
        composeRule.onNodeWithText("সংস্করণ 1.0.0").assertIsDisplayed()
        composeRule.onNodeWithText(
            "আমার হিসাব একটি আধুনিক ও সুরক্ষিত ব্যক্তিগত অর্থ ব্যবস্থাপনা অ্যাপ। সম্পূর্ণ অফলাইনে আপনার আয়, ব্যয়, ঋণ, বাজেট ও ওয়ালেট নিয়ন্ত্রণ করুন সহজ ও স্বচ্ছভাবে।"
        ).assertIsDisplayed()

        composeRule.onNodeWithTag("about_developer_credits").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("ডেভেলপার: মোঃ শরিফুল ইসলাম").assertIsDisplayed()
        composeRule.onNodeWithText("প্রোফেশন: টেকনোলজিস্ট").assertIsDisplayed()
        composeRule.onNodeWithText("পাইকগাছা, খুলনা").assertIsDisplayed()
        composeRule.onNodeWithText("বাংলাদেশে তৈরি ❤️").assertDoesNotExist()

        composeRule.onNodeWithTag("about_footer_note").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("গোপনীয় ও নিরাপদ • আপনার সমস্ত তথ্য ডিভাইসে নিরাপদে থাকে")
            .assertIsDisplayed()

        // Switch to Dark mode and verify main card and footer stay displayed cleanly
        useDark = true
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("about_main_card").assertIsDisplayed()
        composeRule.onNodeWithTag("about_footer_note").performScrollTo().assertIsDisplayed()

        // Back button returns to Settings main screen
        composeRule.onNodeWithTag("about_back_button").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("settings_list").assertIsDisplayed()
    }

    @Test
    fun `settings categories tile opens full-page tabbed view with pill tabs, drag banner, built-in tags, and drag handles`() {
        var useDark by mutableStateOf(false)

        composeRule.setContent {
            AmarHisabTheme(darkTheme = useDark, isBangla = true) {
                SettingsScreen(
                    prefs = UserPrefs(
                        language = AppLanguage.BN,
                        isDarkTheme = useDark,
                        themeMode = if (useDark) AppThemeMode.DARK else AppThemeMode.LIGHT
                    ),
                    accounts = emptyList(),
                    backupStatus = null,
                    strings = StringsProvider.bangla,
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

        // 1. Settings tile renamed to "ক্যাটাগরি" with exact subtitle
        composeRule.onNodeWithTag("settings_list")
            .performScrollToNode(hasTestTag("settings_categories_tile"))
        composeRule.onNodeWithTag("settings_categories_tile").assertIsDisplayed()
        composeRule.onNodeWithText("ক্যাটাগরি").assertIsDisplayed()
        composeRule.onNodeWithText("ক্যাটাগরি ও সাব-ক্যাটাগরি পরিচালনা করুন").assertIsDisplayed()
        composeRule.onNodeWithText("খাত ও উপ-খাতসমূহ").assertDoesNotExist()

        // 2. Tap tile -> navigates to dedicated full-page Categories screen
        composeRule.onNodeWithTag("settings_categories_tile").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("categories_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("categories_top_bar").assertIsDisplayed()
        composeRule.onNodeWithTag("categories_tab_bar").assertIsDisplayed()
        composeRule.onNodeWithTag("categories_tab_EXPENSE").assertIsDisplayed()
        composeRule.onNodeWithTag("categories_tab_INCOME").assertIsDisplayed()
        composeRule.onNodeWithTag("categories_tab_TRANSFER").assertIsDisplayed()
        composeRule.onNodeWithText("খরচ").assertIsDisplayed()
        composeRule.onNodeWithText("আয়").assertIsDisplayed()
        composeRule.onNodeWithText("ট্রান্সফার").assertIsDisplayed()

        // Instruction banner below tabs
        composeRule.onNodeWithTag("categories_drag_instruction").assertIsDisplayed()
        composeRule.onNodeWithText("সাজাতে লং প্রেস করে ড্র্যাগ করুন").assertIsDisplayed()

        // 3. Expense category card design: title, "বিল্ট-ইন" tag, and drag handle
        composeRule.onNodeWithTag("category_card_exp_food_dining").assertIsDisplayed()
        composeRule.onNodeWithText("খাবার ও ডাইনিং").assertIsDisplayed()
        composeRule.onNodeWithTag("category_builtin_badge_exp_food_dining", useUnmergedTree = true)
            .assertIsDisplayed()
        composeRule.onNodeWithTag("category_drag_handle_exp_food_dining", useUnmergedTree = true)
            .assertIsDisplayed()

        // Switch to Income ("আয়") tab
        composeRule.onNodeWithTag("categories_tab_INCOME").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("category_card_inc_salary").assertIsDisplayed()
        composeRule.onNodeWithText("বেতন ও আয়").assertIsDisplayed()
        composeRule.onNodeWithTag("category_builtin_badge_inc_salary", useUnmergedTree = true)
            .assertIsDisplayed()
        composeRule.onNodeWithTag("category_drag_handle_inc_salary", useUnmergedTree = true)
            .assertIsDisplayed()

        // Switch to Transfer ("ট্রান্সফার") tab and verify Dark mode consistency
        composeRule.onNodeWithTag("categories_tab_TRANSFER").performClick()
        useDark = true
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("category_card_trf_wallet").assertIsDisplayed()
        composeRule.onNodeWithText("ওয়ালেট ট্রান্সফার").assertIsDisplayed()
        composeRule.onNodeWithTag("category_drag_handle_trf_wallet", useUnmergedTree = true)
            .assertIsDisplayed()

        // Back button returns smoothly to Settings
        composeRule.onNodeWithTag("categories_back_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("settings_list").assertIsDisplayed()
    }

    @Test
    fun `settings week start tile opens bottom sheet with all 7 days, radio selection, and updates subtitle`() {
        assertEquals(
            listOf("শনিবার", "রবিবার", "সোমবার", "মঙ্গলবার", "বুধবার", "বৃহস্পতিবার", "শুক্রবার"),
            StartOfWeek.entries.map { it.bn }
        )

        var currentPrefs by mutableStateOf(
            UserPrefs(
                language = AppLanguage.BN,
                isDarkTheme = false,
                themeMode = AppThemeMode.LIGHT,
                startOfWeek = StartOfWeek.SATURDAY
            )
        )

        composeRule.setContent {
            AmarHisabTheme(darkTheme = currentPrefs.isDarkTheme, isBangla = true) {
                SettingsScreen(
                    prefs = currentPrefs,
                    accounts = emptyList(),
                    backupStatus = null,
                    strings = StringsProvider.bangla,
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
                    onSetStartOfWeek = { currentPrefs = currentPrefs.copy(startOfWeek = it) },
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

        // 1. Scroll to "সপ্তাহের শুরু" tile and verify current subtitle "শনিবার"
        composeRule.onNodeWithTag("settings_list")
            .performScrollToNode(hasTestTag("settings_start_of_week_tile"))
        composeRule.onNodeWithTag("settings_start_of_week_tile").assertIsDisplayed()
        composeRule.onNodeWithText("সপ্তাহের শুরু").assertIsDisplayed()
        composeRule.onNodeWithText("শনিবার").assertIsDisplayed()

        // 2. Tap tile -> opens bottom sheet with exact title, subtitle, close button, and all 7 days
        composeRule.onNodeWithTag("settings_start_of_week_tile").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("week_start_bottom_sheet").assertIsDisplayed()
        composeRule.onNodeWithText("সপ্তাহের শুরু নির্বাচন করুন").assertIsDisplayed()
        composeRule.onNodeWithText("ক্যালেন্ডার এবং সাপ্তাহিক সারাংশের জন্য প্রথম দিন বেছে নিন")
            .assertIsDisplayed()
        composeRule.onNodeWithTag("week_start_sheet_close_button").assertIsDisplayed()
        composeRule.onNodeWithTag("week_start_radio_selected_SATURDAY", useUnmergedTree = true)
            .assertIsDisplayed()

        // Verify all 7 day options exist and are reachable
        StartOfWeek.entries.forEach { day ->
            composeRule.onNodeWithTag("week_start_option_${day.name}")
                .performScrollTo()
                .assertIsDisplayed()
        }

        // 3. Select "বৃহস্পতিবার" -> updates state, dismisses sheet, and updates Settings tile subtitle
        composeRule.onNodeWithTag("week_start_option_THURSDAY").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertEquals(StartOfWeek.THURSDAY, currentPrefs.startOfWeek)
        composeRule.onNodeWithTag("week_start_bottom_sheet").assertDoesNotExist()
        composeRule.onNodeWithTag("settings_start_of_week_tile").assertIsDisplayed()
        composeRule.onNodeWithText("বৃহস্পতিবার").assertIsDisplayed()

        // 4. Re-open in Dark mode and verify THURSDAY radio indicator is active and close button dismisses
        currentPrefs = currentPrefs.copy(isDarkTheme = true, themeMode = AppThemeMode.DARK)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("settings_start_of_week_tile").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("week_start_option_THURSDAY").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("week_start_radio_selected_THURSDAY", useUnmergedTree = true)
            .assertIsDisplayed()
        composeRule.onNodeWithTag("week_start_sheet_close_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("week_start_bottom_sheet").assertDoesNotExist()
    }

    @Test
    fun `categories sort dropdown, create, edit, subcategory removal, delete, and reset to defaults flow works end to end`() {
        var currentPrefs by mutableStateOf(
            UserPrefs(
                language = AppLanguage.BN,
                isDarkTheme = false,
                themeMode = AppThemeMode.LIGHT,
                categorySortMode = CategorySortMode.DEFAULT
            )
        )

        val sampleTransactions = listOf(
            TransactionEntity(
                id = 1L,
                title = "Daraz Order",
                amount = 1500.0,
                type = TransactionType.EXPENSE,
                category = ExpenseCategory.SHOPPING,
                accountId = 1L
            ),
            TransactionEntity(
                id = 2L,
                title = "Aarong Mall",
                amount = 2400.0,
                type = TransactionType.EXPENSE,
                category = ExpenseCategory.SHOPPING,
                accountId = 1L
            ),
            TransactionEntity(
                id = 3L,
                title = "Coffee Shop",
                amount = 350.0,
                type = TransactionType.EXPENSE,
                category = ExpenseCategory.FOOD_DINING,
                accountId = 1L
            )
        )

        composeRule.setContent {
            AmarHisabTheme(darkTheme = currentPrefs.isDarkTheme, isBangla = true) {
                CategoriesManagementSubPage(
                    prefs = currentPrefs,
                    strings = StringsProvider.bangla,
                    transactions = sampleTransactions,
                    onReorderCategories = { type, ids ->
                        currentPrefs = when (type) {
                            TransactionType.EXPENSE -> currentPrefs.copy(
                                categoryOrderExpense = ids,
                                categorySortMode = CategorySortMode.CUSTOM
                            )
                            TransactionType.INCOME -> currentPrefs.copy(
                                categoryOrderIncome = ids,
                                categorySortMode = CategorySortMode.CUSTOM
                            )
                            TransactionType.TRANSFER -> currentPrefs.copy(
                                categoryOrderTransfer = ids,
                                categorySortMode = CategorySortMode.CUSTOM
                            )
                        }
                    },
                    onSetSortMode = { mode ->
                        currentPrefs = currentPrefs.copy(categorySortMode = mode)
                    },
                    onUpsertCategory = { item ->
                        val updated = currentPrefs.managedCategoryOverrides.toMutableList()
                        val idx = updated.indexOfFirst { it.id == item.id }
                        if (idx >= 0) updated[idx] = item else updated.add(item)
                        currentPrefs = currentPrefs.copy(
                            managedCategoryOverrides = updated,
                            deletedCategoryIds = currentPrefs.deletedCategoryIds - item.id
                        )
                    },
                    onDeleteCategory = { id, isBuiltIn ->
                        currentPrefs = currentPrefs.copy(
                            managedCategoryOverrides = currentPrefs.managedCategoryOverrides.filterNot { it.id == id },
                            deletedCategoryIds = if (isBuiltIn) currentPrefs.deletedCategoryIds + id else currentPrefs.deletedCategoryIds
                        )
                    },
                    onResetDefaults = {
                        currentPrefs = currentPrefs.copy(
                            categoryOrderExpense = emptyList(),
                            categoryOrderIncome = emptyList(),
                            categoryOrderTransfer = emptyList(),
                            managedCategoryOverrides = emptyList(),
                            deletedCategoryIds = emptySet(),
                            categorySortMode = CategorySortMode.DEFAULT
                        )
                    },
                    onBack = {}
                )
            }
        }

        composeRule.waitForIdle()

        // 1. Open Sort / Filter dropdown menu and verify all 3 sort options + reset option
        composeRule.onNodeWithTag("categories_sort_button").assertIsDisplayed().performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("categories_sort_menu").assertIsDisplayed()
        composeRule.onNodeWithText("ডিফল্ট ক্রম").assertIsDisplayed()
        composeRule.onNodeWithText("বিল্ট-ইন ক্রম").assertIsDisplayed()
        composeRule.onNodeWithText("কাস্টম ক্রম").assertIsDisplayed()
        composeRule.onNodeWithText("হ্যান্ডেল ধরে সাজানো").assertIsDisplayed()
        composeRule.onNodeWithText("সবচেয়ে বেশি ব্যবহৃত").assertIsDisplayed()
        composeRule.onNodeWithText("ব্যবহারের ফ্রিকোয়েন্সি অনুযায়ী").assertIsDisplayed()
        composeRule.onNodeWithText("ডিফল্ট পুনরুদ্ধার করুন").assertIsDisplayed()
        composeRule.onNodeWithTag("categories_sort_check_DEFAULT", useUnmergedTree = true).assertIsDisplayed()

        // 2. Select "সবচেয়ে বেশি ব্যবহৃত" -> switches mode & moves Shopping above Food & Dining
        composeRule.onNodeWithTag("categories_sort_option_MOST_USED").performClick()
        composeRule.waitForIdle()
        assertEquals(CategorySortMode.MOST_USED, currentPrefs.categorySortMode)

        val shoppingBounds = composeRule.onNodeWithTag("category_card_exp_shopping").getUnclippedBoundsInRoot()
        val foodBounds = composeRule.onNodeWithTag("category_card_exp_food_dining").getUnclippedBoundsInRoot()
        assertTrue(
            "Expected Shopping (2 txs) to appear above Food (1 tx) in MOST_USED mode",
            shoppingBounds.top < foodBounds.top
        )

        // Verify checkmark moved to MOST_USED when reopening sort menu
        composeRule.onNodeWithTag("categories_sort_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("categories_sort_check_MOST_USED", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithTag("categories_sort_option_DEFAULT").performClick()
        composeRule.waitForIdle()

        // 3. Tap '+' button -> Create Category screen
        composeRule.onNodeWithTag("categories_add_button").assertIsDisplayed().performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("category_editor_screen").assertIsDisplayed()
        composeRule.onNodeWithText("নতুন ক্যাটাগরি").assertIsDisplayed()
        composeRule.onNodeWithTag("editor_type_INCOME").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("editor_name_input").performTextReplacement("ফ্রিল্যান্স বোনাস")

        // Pick color swatch & icon
        composeRule.onNodeWithTag("category_editor_form").performScrollToNode(hasTestTag("editor_color_4"))
        composeRule.onNodeWithTag("editor_color_4").performClick()
        composeRule.onNodeWithTag("category_editor_form").performScrollToNode(hasTestTag("editor_icon_savings"))
        composeRule.onNodeWithTag("editor_icon_savings").performClick()

        // Add a subcategory tag
        composeRule.onNodeWithTag("category_editor_form").performScrollToNode(hasTestTag("editor_subcategory_input"))
        composeRule.onNodeWithTag("editor_subcategory_input").performTextReplacement("ক্লায়েন্ট টিপ")
        composeRule.onNodeWithTag("editor_add_subcategory_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("ক্লায়েন্ট টিপ").assertIsDisplayed()

        // Save new category -> returns to list on Income tab with live count incremented (5 -> 6)
        composeRule.onNodeWithTag("editor_save_category_button").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("categories_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("categories_list").performScrollToNode(hasText("ফ্রিল্যান্স বোনাস"))
        composeRule.onNodeWithText("ফ্রিল্যান্স বোনাস").assertIsDisplayed()
        composeRule.onNodeWithText("৬").assertIsDisplayed()

        // 4. Tap the newly created category -> opens Edit screen pre-filled with subcategory & delete icon
        composeRule.onNodeWithText("ফ্রিল্যান্স বোনাস").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("category_editor_screen").assertIsDisplayed()
        composeRule.onNodeWithText("ক্যাটাগরি সম্পাদনা").assertIsDisplayed()
        composeRule.onNodeWithTag("category_editor_delete_button").assertIsDisplayed()

        // Remove subcategory chip via '×' button
        val addedSubKey = currentPrefs.managedCategoryOverrides.last().subcategories.first().key
        composeRule.onNodeWithTag("category_editor_form")
            .performScrollToNode(hasTestTag("editor_remove_subcategory_$addedSubKey"))
        composeRule.onNodeWithTag("editor_remove_subcategory_$addedSubKey").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("ক্লায়েন্ট টিপ").assertDoesNotExist()

        // Delete category via top-right trash button + confirmation dialog
        composeRule.onNodeWithTag("category_editor_delete_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("category_delete_confirm_dialog").assertIsDisplayed()
        composeRule.onNodeWithTag("confirm_delete_category_button").performClick()
        composeRule.waitForIdle()

        // Returns to list, deleted category is gone, live Income count back to 5 ("৫")
        composeRule.onNodeWithTag("categories_screen").assertIsDisplayed()
        composeRule.onNodeWithText("ফ্রিল্যান্স বোনাস").assertDoesNotExist()
        composeRule.onNodeWithText("৫").assertIsDisplayed()

        // 5. Delete a built-in category, then test "ডিফল্ট পুনরুদ্ধার করুন" restores it
        composeRule.onNodeWithTag("categories_tab_EXPENSE").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("category_card_exp_food_dining").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("category_editor_delete_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("confirm_delete_category_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("category_card_exp_food_dining").assertDoesNotExist()

        // Open Sort menu -> tap "ডিফল্ট পুনরুদ্ধার করুন" -> built-in category restored
        composeRule.onNodeWithTag("categories_sort_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("categories_reset_defaults_option").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("category_card_exp_food_dining").assertIsDisplayed()
        assertEquals(CategorySortMode.DEFAULT, currentPrefs.categorySortMode)
    }

    @Test
    fun `financial insights modernizes voice summary controls, live market chips, quota sanitization, and smart highlights`() {
        // 1. Unit-verify error sanitization never leaks raw quota text or URLs
        val rawQuotaError = "You exceeded your current quota, please check your plan at https://ai.google.dev/gemini-api/docs/rate-limits (429 RESOURCE_EXHAUSTED)"
        val sanitizedMarket = InsightsSummaryHelper.sanitizeAiError(
            rawMessage = rawQuotaError,
            language = AppLanguage.BN,
            isVoice = false
        )
        assertEquals(MarketErrorCategory.QUOTA_EXCEEDED, sanitizedMarket.category)
        assertFalse(sanitizedMarket.message.contains("quota", ignoreCase = true))
        assertFalse(sanitizedMarket.message.contains("https://", ignoreCase = true))
        assertFalse(sanitizedMarket.message.contains("429"))

        val foodSpend = CategorySpend(ExpenseCategory.FOOD_DINING, 11000.0, 0.55f)
        val grocerySpend = CategorySpend(ExpenseCategory.GROCERIES, 9000.0, 0.45f)
        val analytics = AnalyticsSnapshot(
            periodLabel = "November 2026",
            totalIncome = 60000.0,
            totalExpense = 20000.0,
            netFlow = 40000.0,
            dailyAverage = 1111.0,
            peakAmount = 4200.0,
            trendPoints = listOf(
                DailyTrendPoint("01", 1200.0),
                DailyTrendPoint("05", 4200.0, isPeak = true),
                DailyTrendPoint("10", 1500.0)
            ),
            categoryBreakdown = listOf(foodSpend, grocerySpend),
            previousPeriodExpense = 23500.0,
            expenseChangePercent = -15,
            savingsRatePercent = 66,
            topCategory = foodSpend,
            transactionCount = 12
        )

        val baseState = MainUiState(
            isPrefsLoaded = true,
            prefs = UserPrefs(language = AppLanguage.BN, isDarkTheme = false, userName = "শরিফুল"),
            totalLiquidBalance = 85000.0,
            insightFilter = InsightTimeFilter.THIS_MONTH,
            analytics = analytics
        )

        // Verify period-aware summary includes top categories, daily avg, net surplus, and delta
        val generatedSummary = InsightsSummaryHelper.buildPeriodSummary(baseState)
        assertTrue(generatedSummary.contains("খাবার ও ডাইনিং"))
        assertTrue(generatedSummary.contains("দৈনিক গড় খরচ"))
        assertTrue(generatedSummary.contains("নিট উদ্বৃত্ত"))
        assertTrue(generatedSummary.contains("১৫%"))

        var ttsState by mutableStateOf<TtsUiState>(TtsUiState.Idle)
        var searchState by mutableStateOf<SearchAiUiState>(SearchAiUiState.Idle)
        var searchedQuery by mutableStateOf("")
        var useDark by mutableStateOf(false)

        composeRule.setContent {
            AmarHisabTheme(darkTheme = useDark, isBangla = true) {
                InsightsScreen(
                    state = baseState.copy(prefs = baseState.prefs.copy(isDarkTheme = useDark)),
                    ttsState = ttsState,
                    searchAiState = searchState,
                    strings = StringsProvider.bangla,
                    onSelectFilter = {},
                    onShiftMonth = {},
                    onPlayBriefingTts = { ttsState = TtsUiState.Playing(generatedSummary, progress = 0.42f) },
                    onPauseTts = {
                        val curr = ttsState as? TtsUiState.Playing
                        if (curr != null) ttsState = TtsUiState.Paused(curr.script, curr.progress)
                    },
                    onResumeTts = {
                        val curr = ttsState as? TtsUiState.Paused
                        if (curr != null) ttsState = TtsUiState.Playing(curr.script, curr.progress)
                    },
                    onStopTts = { ttsState = TtsUiState.Idle },
                    onSearchMarket = { q ->
                        searchedQuery = q
                        searchState = SearchAiUiState.Error(
                            message = rawQuotaError,
                            category = MarketErrorCategory.QUOTA_EXCEEDED,
                            failedQuery = q
                        )
                    }
                )
            }
        }

        composeRule.waitForIdle()

        // 2. Verify Smart Insights Highlights Card
        composeRule.onNodeWithTag("insights_list")
            .performScrollToNode(hasTestTag("insights_highlights_card"))
        composeRule.onNodeWithTag("insights_highlights_card").assertIsDisplayed()
        composeRule.onNodeWithTag("insight_highlight_delta").assertIsDisplayed()
        composeRule.onNodeWithText("↓ ১৫% কম খরচ").assertIsDisplayed()
        composeRule.onNodeWithTag("insight_highlight_top_category").assertIsDisplayed()

        // 3. Verify Voice Financial Summary: collapsible text preview + Play / Pause / Resume / Stop
        composeRule.onNodeWithTag("insights_list")
            .performScrollToNode(hasTestTag("voice_summary_card"))
        composeRule.onNodeWithTag("voice_summary_card").assertIsDisplayed()

        // Toggle text summary preview open & closed
        composeRule.onNodeWithTag("tts_summary_preview_box").assertDoesNotExist()
        composeRule.onNodeWithTag("tts_summary_toggle_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tts_summary_preview_box").assertIsDisplayed()

        // Tap Play -> Playing with waveform visualizer, Pause & Stop buttons
        composeRule.onNodeWithTag("tts_briefing_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tts_audio_visualizer").assertIsDisplayed()
        composeRule.onNodeWithTag("tts_pause_button").assertIsDisplayed().performClick()
        composeRule.waitForIdle()

        // Paused -> Resume & Stop buttons
        composeRule.onNodeWithTag("tts_resume_button").assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tts_stop_button").assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tts_briefing_button").assertIsDisplayed()

        // 4. Verify Live Market & Rates has been completely removed from Insights
        composeRule.onNodeWithTag("live_market_card").assertDoesNotExist()
        composeRule.onNodeWithTag("market_interactive_tools_card").assertDoesNotExist()
        composeRule.onNodeWithTag("market_offline_guide_card").assertDoesNotExist()

        // Verify Dark mode rendering on Insights analytics & voice summary
        useDark = true
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tts_briefing_button").assertIsDisplayed()
    }

    @Test
    fun `insights exports monthly financial summaries as PDF and CSV with FileProvider share intents`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val nowMillis = System.currentTimeMillis()
        val prevMonthMillis = nowMillis - (32L * 86_400_000L)

        val accounts = listOf(
            AccountEntity(
                id = 1L,
                name = "City Bank Salary",
                type = AccountType.DEBIT_BANK,
                balance = 85000.0,
                bankOrProvider = "City Bank",
                colorArgb = 0xFF0284C7L
            ),
            AccountEntity(
                id = 2L,
                name = "bKash Personal",
                type = AccountType.MFS,
                balance = 14200.0,
                bankOrProvider = "bKash",
                colorArgb = 0xFFE2136EL
            )
        )

        val txList = buildList {
            add(
                TransactionEntity(
                    id = 1L,
                    title = "Monthly Salary Credit",
                    amount = 70000.0,
                    type = TransactionType.INCOME,
                    category = ExpenseCategory.SALARY_INCOME,
                    subcategory = "Payroll",
                    accountId = 1L,
                    note = "November salary",
                    timestamp = nowMillis - 3_600_000L
                )
            )
            add(
                TransactionEntity(
                    id = 2L,
                    title = "Shwapno \"Super\" Shop",
                    amount = 8500.0,
                    type = TransactionType.EXPENSE,
                    category = ExpenseCategory.GROCERIES,
                    subcategory = "Super Shop",
                    accountId = 2L,
                    note = "Weekly groceries, veggies",
                    timestamp = nowMillis - 1_800_000L
                )
            )
            add(
                TransactionEntity(
                    id = 3L,
                    title = "DESCO Electricity Bill",
                    amount = 4200.0,
                    type = TransactionType.EXPENSE,
                    category = ExpenseCategory.RENT_UTILITIES,
                    subcategory = "Electricity",
                    accountId = 2L,
                    note = "Prepaid meter",
                    timestamp = nowMillis - 900_000L
                )
            )
            // Previous month transaction to verify delta calculation & scoping
            add(
                TransactionEntity(
                    id = 4L,
                    title = "Last Month Rent",
                    amount = 20000.0,
                    type = TransactionType.EXPENSE,
                    category = ExpenseCategory.RENT_UTILITIES,
                    subcategory = "Rent",
                    accountId = 1L,
                    note = "Previous month",
                    timestamp = prevMonthMillis
                )
            )
        }

        val state = MainUiState(
            isPrefsLoaded = true,
            prefs = UserPrefs(
                userName = "Shariful Islam",
                language = AppLanguage.BN,
                monthlyBudget = 35000.0,
                isDarkTheme = false
            ),
            accounts = accounts,
            transactions = txList,
            insightFilter = InsightTimeFilter.THIS_MONTH,
            insightMonthOffset = 0,
            analytics = AnalyticsSnapshot(
                periodLabel = "November 2026",
                totalIncome = 70000.0,
                totalExpense = 12700.0,
                netFlow = 57300.0,
                dailyAverage = 1270.0,
                peakAmount = 8500.0,
                trendPoints = listOf(DailyTrendPoint("01", 8500.0, true), DailyTrendPoint("02", 4200.0, false)),
                categoryBreakdown = listOf(
                    CategorySpend(ExpenseCategory.GROCERIES, 8500.0, 0.67f),
                    CategorySpend(ExpenseCategory.RENT_UTILITIES, 4200.0, 0.33f)
                ),
                previousPeriodExpense = 20000.0,
                expenseChangePercent = -36,
                savingsRatePercent = 81,
                topCategory = CategorySpend(ExpenseCategory.GROCERIES, 8500.0, 0.67f),
                transactionCount = 3
            )
        )

        // 1. Direct Exporter Verification (CSV & PDF + FileProvider Share Intent)
        val payload = MonthlyReportExporter.buildMonthlyPayload(state, nowMillis)
        assertEquals(70000.0, payload.totalIncome, 0.01)
        assertEquals(12700.0, payload.totalExpense, 0.01)
        assertEquals(57300.0, payload.netFlow, 0.01)
        assertEquals(3, payload.transactions.size)

        val csvExport = MonthlyReportExporter.exportAndBuildShareIntent(
            context = context,
            state = state,
            format = ReportExportFormat.CSV,
            strings = StringsProvider.bangla,
            nowMillis = nowMillis
        ).getOrThrow()

        assertTrue(csvExport.file.exists())
        assertTrue(csvExport.file.name.endsWith(".csv"))
        assertEquals("text/csv", csvExport.mimeType)
        assertEquals(Intent.ACTION_SEND, csvExport.shareIntent.action)
        assertEquals("text/csv", csvExport.shareIntent.type)
        assertTrue((csvExport.shareIntent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0)
        assertTrue(csvExport.uri.toString().contains(".fileprovider"))
        assertTrue(csvExport.shareIntent.clipData != null)
        val csvText = csvExport.file.readText(Charsets.UTF_8)
        assertTrue(csvText.contains("Total_Income_BDT,70000.00"))
        assertTrue(csvText.contains("Total_Expense_BDT,12700.00"))
        assertTrue(csvText.contains("\"Shwapno \"\"Super\"\" Shop\""))
        assertFalse(csvText.contains("Last Month Rent"))

        val pdfExport = MonthlyReportExporter.exportAndBuildShareIntent(
            context = context,
            state = state,
            format = ReportExportFormat.PDF,
            strings = StringsProvider.bangla,
            nowMillis = nowMillis
        ).getOrThrow()

        assertTrue(pdfExport.file.exists())
        assertTrue(pdfExport.file.name.endsWith(".pdf"))
        assertTrue(pdfExport.file.length() > 500L)
        val pdfHeader = pdfExport.file.inputStream().use { ins ->
            val buf = ByteArray(5)
            ins.read(buf)
            String(buf, Charsets.US_ASCII)
        }
        assertEquals("%PDF-", pdfHeader)
        assertEquals("application/pdf", pdfExport.mimeType)
        assertEquals(Intent.ACTION_SEND, pdfExport.shareIntent.action)
        assertTrue((pdfExport.shareIntent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0)
        assertTrue(pdfExport.uri.toString().contains(".fileprovider"))

        // 2. InsightsScreen Compose UI Flow: Header Export Sheet (PDF) + Inline Quick Export Card (CSV)
        val capturedShares = mutableListOf<ExportSharePayload>()
        var useDark by mutableStateOf(false)

        composeRule.setContent {
            AmarHisabTheme(darkTheme = useDark, isBangla = true) {
                InsightsScreen(
                    state = state.copy(prefs = state.prefs.copy(isDarkTheme = useDark)),
                    ttsState = TtsUiState.Idle,
                    searchAiState = SearchAiUiState.Idle,
                    strings = StringsProvider.bangla,
                    onSelectFilter = {},
                    onShiftMonth = {},
                    onPlayBriefingTts = {},
                    onStopTts = {},
                    onSearchMarket = {},
                    onShareExportPayload = { capturedShares += it }
                )
            }
        }

        composeRule.waitForIdle()

        // Tap top-right header Export button -> Bottom sheet opens with PDF & CSV options
        composeRule.onNodeWithTag("insights_header_export_button").assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("insights_export_sheet").assertIsDisplayed()
        composeRule.onNodeWithTag("export_sheet_preview_card").assertIsDisplayed()
        composeRule.onNodeWithTag("export_pdf_option").assertIsDisplayed().performClick()

        composeRule.waitUntil(timeoutMillis = 5_000L) {
            capturedShares.any { it.format == ReportExportFormat.PDF }
        }
        val sharedPdf = capturedShares.first { it.format == ReportExportFormat.PDF }
        assertEquals("application/pdf", sharedPdf.mimeType)
        assertTrue(sharedPdf.file.exists())

        // Scroll to MonthlyReportExportCard and tap 1-tap CSV share button
        composeRule.onNodeWithTag("insights_list")
            .performScrollToNode(hasTestTag("insights_export_card"))
        composeRule.onNodeWithTag("insights_export_card").assertIsDisplayed()
        composeRule.onNodeWithTag("insights_quick_export_csv").assertIsDisplayed().performClick()

        composeRule.waitUntil(timeoutMillis = 5_000L) {
            capturedShares.any { it.format == ReportExportFormat.CSV }
        }
        composeRule.waitForIdle()
        val sharedCsv = capturedShares.first { it.format == ReportExportFormat.CSV }
        assertEquals("text/csv", sharedCsv.mimeType)
        assertTrue(sharedCsv.file.exists())
        composeRule.onNodeWithTag("insights_export_status_banner").assertIsDisplayed()

        // Verify Dark mode rendering
        useDark = true
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("insights_export_card").assertIsDisplayed()
    }

    @Test
    fun `biometric launch lock verifies via BiometricPrompt in Settings, secures AppLockScreen, and auto-locks on background return`() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        // 1. Helper capability, enroll intent & error mapping checks
        val capabilities = BiometricAuthHelper.inspectCapabilities(context)
        assertTrue(capabilities.availability in BiometricAvailability.entries)
        val enrollIntent = BiometricAuthHelper.createEnrollIntent()
        assertEquals(android.provider.Settings.ACTION_BIOMETRIC_ENROLL, enrollIntent.action)
        assertEquals(
            BiometricAuthFailureReason.LOCKOUT,
            BiometricAuthHelper.mapErrorCode(androidx.biometric.BiometricPrompt.ERROR_LOCKOUT)
        )
        assertEquals(
            BiometricAuthFailureReason.CANCELED,
            BiometricAuthHelper.mapErrorCode(androidx.biometric.BiometricPrompt.ERROR_NEGATIVE_BUTTON)
        )

        // 2. SettingsScreen: Direct Biometric Unlock toggle works without requiring PIN first
        var prefs by mutableStateOf(
            UserPrefs(
                language = AppLanguage.BN,
                appLockEnabled = false,
                biometricUnlockEnabled = false
            )
        )
        var verifyPromptTriggered by mutableStateOf(false)
        var lockedNowClicked by mutableStateOf(false)
        val readyCapability = BiometricCapabilityInfo(
            availability = BiometricAvailability.AVAILABLE,
            canUseDeviceCredential = true,
            hasFingerprint = true,
            hasFace = true
        )

        composeRule.setContent {
            AmarHisabTheme(darkTheme = false, isBangla = true) {
                SettingsScreen(
                    prefs = prefs,
                    accounts = emptyList(),
                    backupStatus = null,
                    strings = StringsProvider.bangla,
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
                    onSetAppLock = { enabled, pin -> prefs = prefs.copy(appLockEnabled = enabled, appLockPin = pin) },
                    onSetBiometricUnlock = { enabled -> prefs = prefs.copy(biometricUnlockEnabled = enabled) },
                    onLockNow = { lockedNowClicked = true },
                    onExportJson = {},
                    onExportCsv = {},
                    onRestoreJson = {},
                    onClearBackupStatus = {},
                    biometricCapabilityOverride = readyCapability,
                    biometricVerifyOverride = { onSucceeded, _ ->
                        verifyPromptTriggered = true
                        onSucceeded()
                    }
                )
            }
        }

        composeRule.onNodeWithTag("settings_list")
            .performScrollToNode(hasTestTag("settings_security_card"))
        composeRule.onNodeWithTag("settings_toggle_biometric").assertIsDisplayed().performClick()
        composeRule.waitForIdle()

        assertTrue(verifyPromptTriggered)
        assertTrue(prefs.biometricUnlockEnabled)
        assertTrue(prefs.isLaunchLockActive)
        composeRule.onNodeWithTag("settings_vault_protected_badge").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_biometric_feedback").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_list").performTouchInput { swipeUp() }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("lock_vault_now_button").assertIsDisplayed().performClick()
        assertTrue(lockedNowClicked)

        // 3. MainViewModel background auto-lock with 2s grace window
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val viewModel = MainViewModel(app)
        viewModel.setBiometricUnlock(true)
        runBlocking {
            viewModel.uiState.first { it.prefs.biometricUnlockEnabled }
        }
        assertTrue(viewModel.isAppUnlocked.value)

        // Short background switch (< 2s grace window) stays unlocked
        viewModel.onAppBackgrounded(nowMillis = 10_000L)
        viewModel.onAppForegrounded(nowMillis = 10_800L)
        assertTrue(viewModel.isAppUnlocked.value)

        // Longer background switch (>= 2s) automatically re-locks the vault
        viewModel.onAppBackgrounded(nowMillis = 20_000L)
        viewModel.onAppForegrounded(nowMillis = 25_000L)
        assertFalse(viewModel.isAppUnlocked.value)

        // Re-authenticating unlocks again
        viewModel.unlockApp()
        assertTrue(viewModel.isAppUnlocked.value)
    }

    @Test
    fun `app lock screen auto-prompts BiometricPrompt on launch, supports device credential fallback, and enforces strict PIN`() {
        val readyCapability = BiometricCapabilityInfo(
            availability = BiometricAvailability.AVAILABLE,
            canUseDeviceCredential = true,
            hasFingerprint = true,
            hasFace = false
        )

        var unlocked by mutableStateOf(false)
        var capturedRequest by mutableStateOf<BiometricPromptRequest?>(null)
        var expectedPin by mutableStateOf("")

        composeRule.setContent {
            AmarHisabTheme(darkTheme = true, isBangla = true) {
                AppLockScreen(
                    expectedPin = expectedPin,
                    biometricEnabled = true,
                    hapticEnabled = false,
                    strings = StringsProvider.bangla,
                    onUnlocked = { unlocked = true },
                    capabilityOverride = readyCapability,
                    biometricAuthenticatorOverride = { req -> capturedRequest = req }
                )
            }
        }

        composeRule.waitForIdle()

        // Biometric-only mode auto-prompts on launch with device credential fallback enabled
        assertTrue(capturedRequest != null)
        assertTrue(capturedRequest!!.allowDeviceCredentialFallback)
        composeRule.onNodeWithTag("biometric_first_panel").assertIsDisplayed()
        composeRule.onNodeWithTag("biometric_unlock_button").assertIsDisplayed()
        composeRule.onNodeWithTag("lock_key_1").assertDoesNotExist()

        // Lockout failure shows polite Bangla message without unlocking
        capturedRequest!!.onFailure(BiometricAuthFailureReason.LOCKOUT, "Locked")
        composeRule.waitForIdle()
        assertFalse(unlocked)
        composeRule.onNodeWithTag("biometric_error_notice").assertIsDisplayed()

        // Biometric success unlocks the vault
        capturedRequest!!.onSucceeded()
        composeRule.waitForIdle()
        assertTrue(unlocked)

        // Now test Biometric + PIN mode with strict PIN verification
        unlocked = false
        expectedPin = "2580"
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("switch_to_pin_button").performScrollTo().assertIsDisplayed().performClick()
        composeRule.waitForIdle()

        // Wrong PIN fails with error
        listOf("1", "1", "1", "1").forEach { d ->
            composeRule.onNodeWithTag("lock_key_$d").performScrollTo().performClick()
        }
        composeRule.waitForIdle()
        assertFalse(unlocked)
        composeRule.onNodeWithTag("pin_error_text").performScrollTo().assertIsDisplayed()

        // Correct PIN unlocks
        listOf("2", "5", "8", "0").forEach { d ->
            composeRule.onNodeWithTag("lock_key_$d").performScrollTo().performClick()
        }
        composeRule.waitForIdle()
        assertTrue(unlocked)
    }

    @Test
    fun `live market hub supports 15-currency converter from 1, offline cache notice auto-hide, and all calculators`() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        // 1. Verify all 15 requested multi-region currencies exist and cross-convert accurately starting at 1
        val expectedCodes = setOf(
            "KWD", "SAR", "AED", "QAR", "BHD", "INR", "LKR", "MYR",
            "NPR", "BTN", "PKR", "BDT", "USD", "EUR", "GBP"
        )
        assertEquals(expectedCodes, MarketCurrency.entries.map { it.code }.toSet())

        val snapshot = MarketSnapshot(
            ratesPerUsd = mapOf(
                "USD" to 1.0,
                "BDT" to 122.0,
                "KWD" to 0.305,
                "SAR" to 3.75,
                "AED" to 3.6725,
                "EUR" to 0.92,
                "INR" to 84.0
            ),
            gold22kPerVoriBdt = 144_000.0,
            lastSyncedMillis = System.currentTimeMillis(),
            isLiveSynced = true,
            sourceLabel = "Test Cache"
        )
        // 1 KWD -> BDT = (1 / 0.305) * 122.0 = 400.0
        assertEquals(400.0, snapshot.convert(1.0, MarketCurrency.KWD, MarketCurrency.BDT), 0.01)
        // Cross conversion 1 EUR -> SAR works without BDT or USD hardcoding
        assertEquals((1.0 / 0.92) * 3.75, snapshot.convert(1.0, MarketCurrency.EUR, MarketCurrency.SAR), 0.01)
        // Daily staleness check
        assertFalse(snapshot.isStale(nowMillis = snapshot.lastSyncedMillis + 3_600_000L))
        assertTrue(snapshot.isStale(nowMillis = snapshot.lastSyncedMillis + MarketSnapshot.DAILY_SYNC_INTERVAL_MS + 1000L))

        // 2. Verify DataStore offline market cache persistence & calculator math
        val marketRepo = MarketDataRepository(context)
        runBlocking {
            marketRepo.saveSnapshot(
                rates = snapshot.ratesPerUsd,
                gold22kVoriBdt = 145_500.0,
                syncedAtMillis = 1_700_000_000_000L,
                sourceLabel = "Cached Test"
            )
            val restored = marketRepo.getCachedSnapshot()
            assertTrue(restored.isLiveSynced)
            assertEquals(122.0, restored.ratePerUsd(MarketCurrency.BDT), 0.01)
            assertEquals(145_500.0, restored.gold22kPerVoriBdt, 1.0)
        }

        val fdr = MarketCalculators.calculateFdr(100_000.0, 10.0, 12, hasTinCertificate = true)
        assertEquals(10_000.0, fdr.grossProfit, 0.01)
        assertEquals(109_000.0, fdr.maturityAmount, 0.01)

        val rem = MarketCalculators.calculateRemittance(1.0, MarketCurrency.KWD, snapshot, includeGovtIncentive = true)
        assertEquals(400.0, rem.baseBdt, 0.01)
        assertEquals(410.0, rem.totalReceivableBdt, 0.01)

        val mfs = MarketCalculators.calculateMfsCashOut(1000.0, MfsChannel.BKASH_PRIYO)
        assertEquals(14.90, mfs.feeBdt, 0.01)

        // 3. Compose UI verification: Home -> highlighted Live Market directly below Financial Health (options collapsed) -> 5-option Hub -> Single-option Detail View
        var syncState by mutableStateOf(MarketSyncUiState(snapshot = snapshot, isOnline = false, isSyncing = false))
        var useDark by mutableStateOf(false)
        var showLiveMarket by mutableStateOf(false)

        composeRule.setContent {
            AmarHisabTheme(darkTheme = useDark, isBangla = true) {
                if (!showLiveMarket) {
                    HomeDashboardScreen(
                        state = MainUiState(
                            prefs = UserPrefs(language = AppLanguage.BN),
                            incomeThisMonth = 50_000.0,
                            spentThisMonth = 20_000.0
                        ),
                        strings = StringsProvider.bangla,
                        onOpenSearch = {},
                        onOpenNotifications = {},
                        onOpenSettings = {},
                        onOpenProfileEdit = {},
                        onOpenAllAccounts = {},
                        onOpenTransfer = {},
                        onOpenReceiptScan = {},
                        onOpenQuickExpense = {},
                        onOpenVoiceEntry = {},
                        onSeeAllActivity = {},
                        marketSyncState = syncState,
                        onOpenLiveMarket = { showLiveMarket = true }
                    )
                } else {
                    LiveMarketScreen(
                        language = AppLanguage.BN,
                        marketSyncState = syncState,
                        strings = StringsProvider.bangla,
                        onRefreshMarketRates = {},
                        onBack = { showLiveMarket = false }
                    )
                }
            }
        }

        // Scroll Home to Financial Health -> verify highlighted Live Market card sits directly below it
        composeRule.onNodeWithTag("home_dashboard_list")
            .performScrollToNode(hasTestTag("home_live_market_entry_card"))
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("home_section_FINANCIAL_HEALTH").assertIsDisplayed()
        composeRule.onNodeWithTag("home_live_market_entry_card").assertIsDisplayed()

        val healthBounds = composeRule.onNodeWithTag("home_section_FINANCIAL_HEALTH").getUnclippedBoundsInRoot()
        val liveMarketBounds = composeRule.onNodeWithTag("home_live_market_entry_card").getUnclippedBoundsInRoot()
        assertTrue(
            "Expected Live Market card directly below Financial Health (${liveMarketBounds.top} >= ${healthBounds.bottom})",
            liveMarketBounds.top >= healthBounds.bottom
        )

        // All 5 inner options & calculators remain completely hidden while on Home
        composeRule.onNodeWithTag("live_market_options_menu").assertDoesNotExist()
        composeRule.onNodeWithTag("live_market_option_CURRENCY_RATE").assertDoesNotExist()
        composeRule.onNodeWithTag("market_currency_converter_panel").assertDoesNotExist()

        // Tap highlighted Home Live Market card -> opens Live Market hub showing ONLY the 5 core options
        composeRule.onNodeWithTag("home_live_market_entry_card").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("live_market_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("live_market_options_menu").assertIsDisplayed()

        // Offline mode shows the smart Bangla offline notice banner
        composeRule.onNodeWithTag("market_offline_notice_banner").assertIsDisplayed()

        // Switching online immediately hides the offline notice banner
        syncState = syncState.copy(isOnline = true)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("market_offline_notice_banner").assertDoesNotExist()

        // Verify all 5 options exist and NO calculator is stacked underneath on the menu view
        composeRule.onNodeWithTag("live_market_option_CURRENCY_RATE").assertIsDisplayed()
        composeRule.onNodeWithTag("live_market_option_GOLD_PRICE").assertIsDisplayed()
        composeRule.onNodeWithTag("live_market_option_ESSENTIALS").assertIsDisplayed()
        composeRule.onNodeWithTag("live_market_option_BANK_FDR_DPS").assertIsDisplayed()
        composeRule.onNodeWithTag("live_market_option_REMITTANCE_WALLET").assertIsDisplayed()
        composeRule.onNodeWithTag("market_currency_converter_panel").assertDoesNotExist()
        composeRule.onNodeWithTag("market_gold_panel").assertDoesNotExist()

        // Option 1: Currency Rate -> opens ONLY Currency Converter (starts at 1, supports KWD & swap)
        composeRule.onNodeWithTag("live_market_option_CURRENCY_RATE").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("live_market_option_detail").assertIsDisplayed()
        composeRule.onNodeWithTag("live_market_options_menu").assertDoesNotExist()
        composeRule.onNodeWithTag("market_currency_converter_panel").assertIsDisplayed()
        composeRule.onNodeWithTag("market_gold_panel").assertDoesNotExist()
        composeRule.onNodeWithTag("currency_swap_button").assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("currency_from_selector").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("currency_picker_sheet").assertIsDisplayed()
        composeRule.onNodeWithTag("currency_picker_search").performTextReplacement("KWD")
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("currency_option_KWD").assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("currency_conversion_result").assertIsDisplayed()

        // Back to 5-option menu -> Option 2: Gold Price
        composeRule.onNodeWithTag("live_market_back_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("live_market_options_menu").assertIsDisplayed()
        composeRule.onNodeWithTag("live_market_option_GOLD_PRICE").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("market_gold_panel").assertIsDisplayed()
        composeRule.onNodeWithTag("market_currency_converter_panel").assertDoesNotExist()
        composeRule.onNodeWithTag("gold_unit_gram").performClick()
        composeRule.waitForIdle()

        // Back to 5-option menu -> Option 3: Essentials / Groceries
        composeRule.onNodeWithTag("live_market_all_options_chip").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("live_market_option_ESSENTIALS").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("market_essentials_panel").assertIsDisplayed()
        composeRule.onNodeWithTag("essential_item_rice_miniket").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("essentials_basket_summary").assertIsDisplayed()

        // Back to 5-option menu -> Option 4: Bank FDR & DPS
        composeRule.onNodeWithTag("live_market_back_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("live_market_option_BANK_FDR_DPS").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("market_bank_panel").assertIsDisplayed()

        // Back to 5-option menu -> Option 5: Remittance & Wallet Fee (Remittance + bKash/Nagad switcher)
        composeRule.onNodeWithTag("live_market_back_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("live_market_option_REMITTANCE_WALLET").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("market_remittance_panel").assertIsDisplayed()
        composeRule.onNodeWithTag("market_mfs_panel").assertDoesNotExist()
        composeRule.onNodeWithTag("remittance_wallet_subtab_mfs").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("market_mfs_panel").assertIsDisplayed()
        composeRule.onNodeWithTag("market_remittance_panel").assertDoesNotExist()

        // Verify Dark Mode contrast & layout inside Live Market detail & back to Home
        useDark = true
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("market_mfs_panel").assertIsDisplayed()
        composeRule.onNodeWithTag("live_market_back_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("live_market_options_menu").assertIsDisplayed()
        composeRule.onNodeWithTag("live_market_back_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("home_live_market_entry_card").assertIsDisplayed()
    }

    @Test
    fun `system time and timezone reactivity updates period boundaries and Settings manages mic, camera, and Google location permissions`() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()

        // 1. Verify DeviceTimeContext dynamically adapts to different travel timezones without hardcoding
        val sampleInstant = 1_763_590_000_000L // Fixed UTC instant
        val dhakaCtx = DeviceTimeContext.current(sampleInstant, java.util.TimeZone.getTimeZone("Asia/Dhaka"))
        val riyadhCtx = DeviceTimeContext.current(sampleInstant, java.util.TimeZone.getTimeZone("Asia/Riyadh"))
        val nyCtx = DeviceTimeContext.current(sampleInstant, java.util.TimeZone.getTimeZone("America/New_York"))

        assertEquals("Asia/Dhaka", dhakaCtx.timeZoneId)
        assertEquals("UTC+06:00", dhakaCtx.utcOffsetLabel)
        assertEquals("Asia/Riyadh", riyadhCtx.timeZoneId)
        assertEquals("UTC+03:00", riyadhCtx.utcOffsetLabel)
        assertEquals("America/New_York", nyCtx.timeZoneId)
        assertEquals("UTC-05:00", nyCtx.utcOffsetLabel)

        // 2. Verify MainViewModel reacts immediately when timezone changes across midnight boundary
        val viewModel = MainViewModel(app)
        // 2026-11-19 02:00 UTC -> Nov 19 in Asia/Dhaka (UTC+6), Nov 18 in America/New_York (UTC-5)
        val crossMidnightUtc = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
            set(2026, java.util.Calendar.NOVEMBER, 19, 2, 0, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis

        viewModel.setInsightFilter(InsightTimeFilter.DAILY)
        viewModel.refreshSystemTime(crossMidnightUtc, java.util.TimeZone.getTimeZone("Asia/Dhaka"))
        val dhakaState = runBlocking {
            viewModel.uiState.first { it.deviceTimeContext.timeZoneId == "Asia/Dhaka" && it.insightFilter == InsightTimeFilter.DAILY }
        }
        assertEquals("2026-11-19", dhakaState.deviceTimeContext.dayKey)
        assertTrue(dhakaState.analytics.periodLabel.contains("19"))

        viewModel.refreshSystemTime(crossMidnightUtc, java.util.TimeZone.getTimeZone("America/New_York"))
        val nyState = runBlocking {
            viewModel.uiState.first { it.deviceTimeContext.timeZoneId == "America/New_York" && it.insightFilter == InsightTimeFilter.DAILY }
        }
        assertEquals("2026-11-18", nyState.deviceTimeContext.dayKey)
        assertTrue(nyState.analytics.periodLabel.contains("18"))

        // 3. Verify launch permission set includes Microphone, Camera, and Google Location
        assertEquals(
            setOf(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.CAMERA,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            ),
            DevicePermissionsHelper.initialLaunchPermissions.toSet()
        )
        assertTrue(DevicePermissionsHelper.missingLaunchPermissions(app).isNotEmpty())

        // 4. Verify fresh installs start cleanly with zero demo data & 0 balances
        assertTrue(dhakaState.accounts.isEmpty())
        assertTrue(dhakaState.transactions.isEmpty())
        assertTrue(dhakaState.goals.isEmpty())
        assertTrue(dhakaState.loans.isEmpty())
        assertEquals(0.0, dhakaState.totalLiquidBalance, 0.001)
        assertEquals(0.0, dhakaState.dailyAverageSpend, 0.001)
        assertEquals(0.0, dhakaState.prefs.monthlyBudget, 0.001)

        // 5. Verify SettingsScreen keeps Live Device Clock & Timezone sync while removing Device Permissions UI
        var refreshedClock by mutableStateOf(false)
        var useDark by mutableStateOf(false)
        var showEmptyHome by mutableStateOf(false)
        var addAccountClicked by mutableStateOf(false)

        composeRule.setContent {
            AmarHisabTheme(darkTheme = useDark, isBangla = true) {
                if (!showEmptyHome) {
                    SettingsScreen(
                        prefs = UserPrefs(language = AppLanguage.BN, startOfWeek = StartOfWeek.SATURDAY),
                        accounts = emptyList(),
                        backupStatus = null,
                        strings = StringsProvider.bangla,
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
                        deviceTimeContext = riyadhCtx,
                        onRefreshSystemTime = { refreshedClock = true }
                    )
                } else {
                    HomeDashboardScreen(
                        state = MainUiState(prefs = UserPrefs(language = AppLanguage.BN)),
                        strings = StringsProvider.bangla,
                        onOpenSearch = {},
                        onOpenNotifications = {},
                        onOpenSettings = {},
                        onOpenProfileEdit = {},
                        onOpenAllAccounts = {},
                        onOpenAddAccount = { addAccountClicked = true },
                        onOpenTransfer = {},
                        onOpenReceiptScan = {},
                        onOpenQuickExpense = {},
                        onOpenVoiceEntry = {},
                        onSeeAllActivity = {}
                    )
                }
            }
        }

        // Verify live device clock & timezone card is present while Device Permissions card is completely removed from Settings
        composeRule.onNodeWithTag("settings_list")
            .performScrollToNode(hasTestTag("settings_date_time_card"))
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("settings_live_timezone_tile").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_refresh_time_button").performClick()
        assertTrue(refreshedClock)
        composeRule.onNodeWithTag("settings_permissions_card").assertDoesNotExist()
        composeRule.onNodeWithTag("settings_permission_microphone_allow_btn").assertDoesNotExist()

        // Verify Home blank placeholder card & Add Account prompt when accounts is empty (Light & Dark)
        showEmptyHome = true
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("home_dashboard_list")
            .performScrollToNode(hasTestTag("empty_wallet_placeholder_card"))
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("empty_wallet_placeholder_card").assertIsDisplayed()
        composeRule.onNodeWithTag("empty_wallet_add_button").assertIsDisplayed().performClick()
        assertTrue(addAccountClicked)

        useDark = true
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("empty_wallet_placeholder_card").assertIsDisplayed()
    }

    @Test
    fun `customer support inside Settings greets immediately, handles 6 categories offline, and controls WhatsApp escalation`() {
        // 1. Unit-verify CustomerSupportEngine greeting, matching & strict WhatsApp escalation rules
        assertTrue(CustomerSupportEngine.isPureGreeting("ওয়ালাইকুম আসসালাম"))
        assertTrue(CustomerSupportEngine.isPureGreeting("সালাম"))
        assertFalse(CustomerSupportEngine.isPureGreeting("ওয়ালাইকুম আসসালাম, রসিদ স্ক্যান কাজ করছে না"))
        assertEquals(6, CustomerSupportCatalog.categories.size)

        // Never escalate on initial greeting or standard how-to questions
        assertFalse(
            CustomerSupportEngine.shouldEscalateToWhatsApp(
                userMessage = "ওয়ালাইকুম আসসালাম",
                hasAttemptedTroubleshooting = false,
                aiSuggestedEscalation = true
            )
        )
        assertFalse(
            CustomerSupportEngine.shouldEscalateToWhatsApp(
                userMessage = "কীভাবে PDF এক্সপোর্ট করব?",
                hasAttemptedTroubleshooting = false,
                aiSuggestedEscalation = false
            )
        )
        // Escalate on critical/emergency issue OR explicit human request AFTER troubleshooting
        assertTrue(
            CustomerSupportEngine.shouldEscalateToWhatsApp(
                userMessage = "অ্যাপ ক্র্যাশ করছে এবং ডাটা মুছে গেছে",
                hasAttemptedTroubleshooting = false
            )
        )
        assertTrue(
            CustomerSupportEngine.shouldEscalateToWhatsApp(
                userMessage = "সমাধান হয়নি, লাইভ সাপোর্ট চাই",
                hasAttemptedTroubleshooting = true
            )
        )
        assertNotNull(CustomerSupportEngine.matchLocalGuide("আজকের KWD রেট ও সোনার দাম"))
        assertNotNull(CustomerSupportEngine.matchLocalGuide("মাসিক PDF এক্সপোর্ট"))
        assertNotNull(CustomerSupportEngine.matchLocalGuide("সপ্তাহের শুরু পরিবর্তন"))

        // 2. Compose UI flow: open from Settings bottom tile -> immediate self-initiated greeting -> non-blocking replies -> controlled escalation
        var supportState by mutableStateOf(SupportUiState(isOnline = false))
        var msgCounter = 0L
        fun nextId(): String = "t_${++msgCounter}"

        fun ensureInit() {
            if (supportState.messages.isEmpty()) {
                supportState = supportState.copy(
                    messages = listOf(
                        SupportChatMessage(
                            id = nextId(),
                            sender = SupportSender.ASSISTANT,
                            text = SUPPORT_INITIAL_GREETING_BN,
                            timestampMillis = 1_763_539_200_000L
                        )
                    )
                )
            }
        }

        var useDark by mutableStateOf(false)

        composeRule.setContent {
            AmarHisabTheme(darkTheme = useDark, isBangla = true) {
                SettingsScreen(
                    prefs = UserPrefs(language = AppLanguage.BN),
                    accounts = emptyList(),
                    backupStatus = null,
                    strings = StringsProvider.bangla,
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
                    supportState = supportState,
                    onEnsureSupportInitialized = { ensureInit() },
                    onSelectSupportCategory = { cat ->
                        ensureInit()
                        supportState = supportState.copy(selectedCategoryId = cat)
                    },
                    onSelectSupportQuickOption = { opt ->
                        ensureInit()
                        supportState = supportState.copy(
                            messages = supportState.messages +
                                SupportChatMessage(nextId(), SupportSender.USER, opt.labelBn, 1_763_539_210_000L) +
                                SupportChatMessage(
                                    id = nextId(),
                                    sender = SupportSender.ASSISTANT,
                                    text = opt.guideBn,
                                    timestampMillis = 1_763_539_211_000L,
                                    showWhatsAppCard = false,
                                    offerNotResolvedAction = true
                                ),
                            selectedCategoryId = null,
                            hasAttemptedTroubleshooting = true
                        )
                    },
                    onSendSupportMessage = { text ->
                        ensureInit()
                        val reply = when {
                            CustomerSupportEngine.isPureGreeting(text) -> SupportChatMessage(
                                id = nextId(),
                                sender = SupportSender.ASSISTANT,
                                text = SUPPORT_GREETING_REPLY_BN,
                                timestampMillis = 1_763_539_221_000L
                            )
                            !supportState.isOnline -> SupportChatMessage(
                                id = nextId(),
                                sender = SupportSender.ASSISTANT,
                                text = SUPPORT_OFFLINE_AI_NOTICE_BN,
                                timestampMillis = 1_763_539_221_000L,
                                isOfflineNotice = true
                            )
                            else -> SupportChatMessage(
                                id = nextId(),
                                sender = SupportSender.ASSISTANT,
                                text = "সাহায্য করা হয়েছে",
                                timestampMillis = 1_763_539_221_000L,
                                offerNotResolvedAction = true
                            )
                        }
                        supportState = supportState.copy(
                            messages = supportState.messages +
                                SupportChatMessage(nextId(), SupportSender.USER, text, 1_763_539_220_000L) +
                                reply
                        )
                    },
                    onRequestLiveCareAfterTroubleshooting = {
                        supportState = supportState.copy(
                            messages = supportState.messages +
                                SupportChatMessage(
                                    id = nextId(),
                                    sender = SupportSender.ASSISTANT,
                                    text = CustomerSupportEngine.buildWhatsAppEscalationMessage(false),
                                    timestampMillis = 1_763_539_230_000L,
                                    showWhatsAppCard = true
                                )
                        )
                    },
                    onResetSupportChat = {
                        supportState = SupportUiState(isOnline = supportState.isOnline)
                        ensureInit()
                    }
                )
            }
        }

        // Scroll to bottom of Settings and tap "কাস্টমার সাপোর্ট"
        composeRule.onNodeWithTag("settings_list")
            .performScrollToNode(hasTestTag("settings_support_about_card"))
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("settings_customer_support_tile").assertIsDisplayed().performClick()
        composeRule.waitForIdle()

        // Verify immediate self-initiated greeting appears once with NO WhatsApp card yet
        composeRule.onNodeWithTag("customer_support_screen").assertIsDisplayed()
        composeRule.onNodeWithText(SUPPORT_INITIAL_GREETING_BN).assertIsDisplayed()
        assertEquals(1, supportState.messages.size)
        composeRule.onNodeWithTag("support_whatsapp_card").assertDoesNotExist()
        composeRule.onNodeWithTag("support_offline_banner").assertIsDisplayed()

        // Reply with greeting -> polite Bangla response, still no WhatsApp card
        composeRule.onNodeWithTag("support_message_input").performTextReplacement("ওয়ালাইকুম আসসালাম")
        composeRule.onNodeWithTag("support_send_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText(SUPPORT_GREETING_REPLY_BN).assertIsDisplayed()
        composeRule.onNodeWithTag("support_whatsapp_card").assertDoesNotExist()

        // Tap category chip -> expands sub-options -> tap sub-option -> built-in guide works offline
        composeRule.onNodeWithTag("support_cat_${SupportCategoryId.TRANSACTIONS_ENTRY.name.lowercase()}")
            .performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("support_opt_tx_manual").assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("support_whatsapp_card").assertDoesNotExist()

        // Escalate ONLY after troubleshooting -> WhatsApp Customer Care card shows +8801708901947
        composeRule.onNodeWithTag("support_messages_list")
            .performScrollToNode(hasTestTag("support_not_resolved_chip"))
        composeRule.onNodeWithTag("support_not_resolved_chip").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("support_messages_list")
            .performScrollToNode(hasTestTag("support_whatsapp_card"))
        composeRule.onNodeWithTag("support_whatsapp_card").assertIsDisplayed()
        composeRule.onNodeWithText(SUPPORT_WHATSAPP_DISPLAY).assertIsDisplayed()
        composeRule.onNodeWithTag("support_whatsapp_button").assertIsDisplayed()

        // Switch online -> offline notice auto-hides, and verify Dark mode rendering
        supportState = supportState.copy(isOnline = true)
        useDark = true
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("support_offline_banner").assertDoesNotExist()
        composeRule.onNodeWithTag("customer_support_screen").assertIsDisplayed()
    }

    @Test
    fun `multi-currency picker supports all 10 currencies, international banks across 9 regions, and multi-currency tracking`() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        // 1. Verify all 10 requested AppCurrency codes & flags
        val expectedCurrencies = setOf("BDT", "KWD", "SAR", "AED", "QAR", "MYR", "EUR", "GBP", "USD", "INR")
        assertEquals(expectedCurrencies, AppCurrency.entries.map { it.code }.toSet())
        AppCurrency.entries.forEach { cur ->
            assertTrue("Currency ${cur.code} must have a flag", cur.flag.isNotBlank())
            assertTrue("Currency ${cur.code} must have EN & BN names", cur.nameEn.isNotBlank() && cur.nameBn.isNotBlank())
            assertTrue("Currency ${cur.code} must format cleanly", BdtFormatter.formatCurrency(1250.5, cur.code, AppLanguage.EN).isNotBlank())
            assertTrue("Currency ${cur.code} must format cleanly in BN", BdtFormatter.formatCurrency(1250.5, cur.code, AppLanguage.BN).isNotBlank())
        }

        // 2. Verify all 9 banking regions (Bangladesh + 8 international expat hubs) have banks
        assertEquals(
            setOf(
                BankRegion.BANGLADESH,
                BankRegion.SAUDI_ARABIA,
                BankRegion.QATAR,
                BankRegion.KUWAIT,
                BankRegion.UAE,
                BankRegion.MALAYSIA,
                BankRegion.ITALY,
                BankRegion.EUROPE,
                BankRegion.UK
            ),
            BankRegion.entries.toSet()
        )
        BankRegion.entries.forEach { region ->
            val banks = InternationalBankCatalog.banksForRegion(region)
            assertTrue("Region ${region.name} must provide banks", banks.size >= 4)
        }

        // 3. Verify FinanceRepository stamps account currencyCode & converts cross-currency transfers
        val db = androidx.room.Room.inMemoryDatabaseBuilder(
            context,
            com.example.data.local.AppDatabase::class.java
        ).allowMainThreadQueries().build()
        val repo = FinanceRepository(db)
        runBlocking {
            val sarAccountId = repo.addAccount(
                AccountEntity(
                    name = "Al Rajhi Salary",
                    type = AccountType.DEBIT_BANK,
                    balance = 2000.0,
                    bankOrProvider = "Al Rajhi Bank",
                    colorArgb = 0xFF006C35L,
                    currencyCode = "SAR"
                )
            )
            val bdtAccountId = repo.addAccount(
                AccountEntity(
                    name = "bKash Family",
                    type = AccountType.MFS,
                    balance = 5000.0,
                    bankOrProvider = "bKash",
                    colorArgb = 0xFFE2136EL,
                    currencyCode = "BDT"
                )
            )
            // Expense in SAR stamps currencyCode = "SAR" and deducts SAR balance
            repo.addTransaction(
                title = "Riyadh Groceries",
                amount = 150.0,
                type = TransactionType.EXPENSE,
                category = ExpenseCategory.GROCERIES,
                accountId = sarAccountId
            )
            val afterExpenseSar = db.accountDao().getAccountById(sarAccountId)!!
            assertEquals(1850.0, afterExpenseSar.balance, 0.01)
            val txs = db.transactionDao().getAllTransactionsOnce()
            assertEquals("SAR", txs.first().currencyCode)

            // Cross-currency transfer 100 SAR -> BDT at rate 32.4 credits 3,240 BDT
            val transferred = repo.transferFunds(
                fromAccountId = sarAccountId,
                toAccountId = bdtAccountId,
                amount = 100.0,
                note = "Family Remittance",
                conversionRate = 32.4
            )
            assertTrue(transferred)
            assertEquals(1750.0, db.accountDao().getAccountById(sarAccountId)!!.balance, 0.01)
            assertEquals(8240.0, db.accountDao().getAccountById(bdtAccountId)!!.balance, 0.01)
        }
        db.close()

        // 4. UI verification: Settings Main & Second Currency pickers + International Card Wizard
        var prefs by mutableStateOf(UserPrefs(language = AppLanguage.EN, mainCurrencyCode = "BDT", secondaryCurrencyCode = ""))
        var showWizard by mutableStateOf(false)
        var savedAccount by mutableStateOf<AccountEntity?>(null)
        var useDark by mutableStateOf(false)

        composeRule.setContent {
            AmarHisabTheme(darkTheme = useDark, isBangla = false) {
                if (!showWizard) {
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
                        onUpdateDashboardLayout = { _, _, _ -> },
                        onSetAppLock = { _, _ -> },
                        onSetBiometricUnlock = {},
                        onLockNow = {},
                        onExportJson = {},
                        onExportCsv = {},
                        onRestoreJson = {},
                        onClearBackupStatus = {},
                        onSetMainCurrency = { code -> prefs = prefs.copy(mainCurrencyCode = code) },
                        onSetSecondaryCurrency = { code -> prefs = prefs.copy(secondaryCurrencyCode = code) }
                    )
                } else {
                    AddAccountWizardSheet(
                        language = AppLanguage.EN,
                        strings = StringsProvider.english,
                        defaultCurrencyCode = prefs.mainCurrencyCode,
                        secondaryCurrencyCode = prefs.secondaryCurrencyCode,
                        onDismiss = { showWizard = false },
                        onSaveAccount = { savedAccount = it }
                    )
                }
            }
        }

        // Scroll to Main & Second Currency tiles in Settings and select SAR as Second Currency
        composeRule.onNodeWithTag("settings_list")
            .performScrollToNode(hasTestTag("settings_secondary_currency_tile"))
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("settings_main_currency_tile").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_secondary_currency_tile").assertIsDisplayed().performClick()
        composeRule.waitForIdle()

        // Currency picker opens with search and selects SAR (Saudi Arabia)
        composeRule.onNodeWithTag("app_currency_picker_sheet").assertIsDisplayed()
        composeRule.onNodeWithTag("app_currency_picker_search").performTextReplacement("Saudi")
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("app_currency_option_SAR").assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        assertEquals("SAR", prefs.secondaryCurrencyCode)

        // Open AddAccountWizardSheet in Dark mode, choose Kuwait -> NBK -> auto-sets KWD and saves card
        useDark = true
        showWizard = true
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("wizard_region_KUWAIT")
            .performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("wizard_bank_kw_nbk")
            .performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick)
        composeRule.onNodeWithTag("wizard_balance_input").performScrollTo().performTextReplacement("450")
        composeRule.onNodeWithTag("wizard_save_account_button")
            .performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick)
        composeRule.waitForIdle()

        assertTrue("Expected international card to be saved", savedAccount != null)
        assertEquals("KWD", savedAccount!!.currencyCode)
        assertEquals(450.0, savedAccount!!.balance, 0.01)
        assertTrue(savedAccount!!.bankOrProvider.contains("Kuwait", ignoreCase = true))
    }

    @Test
    fun `home total balance displays true per-currency totals with clean flag-free KD and BDT badges`() {
        val sampleAccounts = listOf(
            AccountEntity(
                id = 1L,
                name = "City Bank",
                type = AccountType.DEBIT_BANK,
                balance = 200000.0,
                bankOrProvider = "City Bank",
                colorArgb = 0xFFDC2626L,
                currencyCode = "BDT"
            ),
            AccountEntity(
                id = 2L,
                name = "bKash",
                type = AccountType.MFS,
                balance = 50000.0,
                bankOrProvider = "bKash",
                colorArgb = 0xFFE2136EL,
                currencyCode = "BDT"
            ),
            AccountEntity(
                id = 3L,
                name = "KFH Account",
                type = AccountType.DEBIT_BANK,
                balance = 58.50,
                bankOrProvider = "Kuwait Finance House",
                colorArgb = 0xFF059669L,
                currencyCode = "KWD"
            )
        )
        var uiState by mutableStateOf(
            MainUiState(
                isPrefsLoaded = true,
                prefs = UserPrefs(
                    language = AppLanguage.BN,
                    mainCurrencyCode = "BDT",
                    secondaryCurrencyCode = "KWD",
                    homeLayoutStyle = HomeLayoutStyle.BALANCE_CARD
                ),
                accounts = sampleAccounts,
                totalLiquidBalance = 273107.50, // intentionally different converted figure to verify BDT uses true BDT sum
                liquidBalancesByCurrency = mapOf("BDT" to 250000.0, "KWD" to 58.50),
                netWorth = com.example.data.model.NetWorthSummary(273107.50, 0.0)
            )
        )
        var darkTheme by mutableStateOf(false)

        composeRule.setContent {
            AmarHisabTheme(darkTheme = darkTheme, isBangla = true) {
                HomeDashboardScreen(
                    state = uiState,
                    strings = StringsProvider.bangla,
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

        // Verify true BDT total (৳ ২,৫০,০০০) and true KD total (KD + ৫৮.৫০) without flag or ≈ conversion
        val insideHeroCard = androidx.compose.ui.test.hasAnyAncestor(hasTestTag("hero_balance_card"))
        composeRule.onNodeWithTag("hero_balance_card").assertIsDisplayed()
        composeRule.onNodeWithTag("hero_balance_currency_BDT", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithTag("hero_balance_currency_KWD", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNode(insideHeroCard and hasText("BDT"), useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNode(insideHeroCard and hasText("৳ ২,৫০,০০০"), useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNode(insideHeroCard and hasText("KD"), useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNode(insideHeroCard and hasText("৫৮.৫০"), useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNode(insideHeroCard and hasText("≈", substring = true), useUnmergedTree = true).assertDoesNotExist()
        composeRule.onNode(insideHeroCard and hasText("🇰🇼", substring = true), useUnmergedTree = true).assertDoesNotExist()

        // Verify clean zero-state in Dark mode when accounts are empty
        darkTheme = true
        uiState = uiState.copy(
            accounts = emptyList(),
            totalLiquidBalance = 0.0,
            liquidBalancesByCurrency = emptyMap(),
            netWorth = com.example.data.model.NetWorthSummary(0.0, 0.0)
        )
        composeRule.waitForIdle()
        val insideBdtRow = androidx.compose.ui.test.hasAnyAncestor(hasTestTag("hero_balance_currency_BDT"))
        val insideKwdRow = androidx.compose.ui.test.hasAnyAncestor(hasTestTag("hero_balance_currency_KWD"))
        composeRule.onNodeWithTag("hero_balance_currency_BDT", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithTag("hero_balance_currency_KWD", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNode(insideBdtRow and hasText("৳ ০"), useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNode(insideKwdRow and hasText("০"), useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNode(insideHeroCard and hasText("🇰🇼", substring = true), useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun `receipt scanner handles App Check error with clear Bangla guidance and one-tap manual entry fallback`() {
        // 1. Verify error sanitization for "Firebase App Check token is invalid." and offline/timeout errors
        val appCheckErr = InsightsSummaryHelper.sanitizeReceiptScanError(
            rawMessage = "Firebase App Check token is invalid.",
            language = AppLanguage.BN
        )
        assertEquals(com.example.data.ai.ReceiptScanErrorCategory.APP_CHECK_INVALID, appCheckErr.category)
        assertFalse(appCheckErr.message.contains("Firebase App Check token is invalid", ignoreCase = true))
        assertTrue(appCheckErr.guidance.contains("ম্যানুয়ালি"))

        val offlineErr = InsightsSummaryHelper.sanitizeReceiptScanError(
            rawMessage = "Unable to resolve host",
            language = AppLanguage.BN
        )
        assertEquals(com.example.data.ai.ReceiptScanErrorCategory.OFFLINE, offlineErr.category)

        // 2. Verify ReceiptScanSheet error recovery UI and one-tap "ম্যানুয়ালি হিসাব যোগ করুন" workflow
        val wallets = listOf(
            AccountEntity(
                id = 1L,
                name = "City Bank",
                type = AccountType.DEBIT_BANK,
                balance = 100000.0,
                bankOrProvider = "City Bank",
                colorArgb = 0xFFDC2626L,
                currencyCode = "BDT"
            ),
            AccountEntity(
                id = 2L,
                name = "KFH Account",
                type = AccountType.DEBIT_BANK,
                balance = 120.0,
                bankOrProvider = "Kuwait Finance House",
                colorArgb = 0xFF059669L,
                currencyCode = "KWD"
            )
        )
        var savedTitle = ""
        var savedAmount = 0.0
        var savedAccountId = 0L
        var savedNote = ""
        var savedTimestamp = 0L

        composeRule.setContent {
            AmarHisabTheme(darkTheme = false, isBangla = true) {
                ReceiptScanSheet(
                    accounts = wallets,
                    receiptState = ReceiptScanUiState.Error(
                        message = appCheckErr.message,
                        guidance = appCheckErr.guidance,
                        category = appCheckErr.category
                    ),
                    language = AppLanguage.BN,
                    strings = StringsProvider.bangla,
                    defaultCurrencyCode = "BDT",
                    onPickImageUri = {},
                    onSaveExpense = { _, _, _, _, _ -> },
                    onSaveExpenseWithDate = { title, amount, _, accId, note, ts ->
                        savedTitle = title
                        savedAmount = amount
                        savedAccountId = accId
                        savedNote = note
                        savedTimestamp = ts
                    },
                    onDismiss = {}
                )
            }
        }

        // Error card shows clear Bangla explanation and one-tap "ম্যানুয়ালি হিসাব যোগ করুন" button
        composeRule.onNodeWithTag("receipt_error_card").assertIsDisplayed()
        composeRule.onNodeWithText(appCheckErr.message).assertIsDisplayed()
        composeRule.onNodeWithTag("receipt_manual_entry_button")
            .assertIsDisplayed()
            .performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick)
        composeRule.waitForIdle()

        // Inline manual entry form opens with wallet/currency, amount, merchant, note, and date controls
        composeRule.onNodeWithTag("receipt_wallet_chip_2")
            .performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick)
        composeRule.onNodeWithTag("receipt_amount_input").performTextReplacement("58.50")
        composeRule.onNodeWithTag("receipt_merchant_input").performTextReplacement("Lulu Hypermarket")
        composeRule.onNodeWithTag("receipt_note_input").performTextReplacement("Weekly groceries")
        composeRule.onNodeWithTag("receipt_date_yesterday")
            .performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick)
        composeRule.onNodeWithTag("receipt_save_expense_button")
            .performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick)
        composeRule.waitForIdle()

        assertEquals("Lulu Hypermarket", savedTitle)
        assertEquals(58.50, savedAmount, 0.001)
        assertEquals(2L, savedAccountId)
        assertEquals("Weekly groceries", savedNote)
        assertTrue("Expected valid timestamp", savedTimestamp > 0L)
    }

    @Test
    fun receipt_voucher_matcher_classifies_direction_auto_matches_cards_and_cleans_temp_files() {
        // 1. Money In / Income (+) classification (English + Bangla)
        listOf(
            "Cash In",
            "Received Money",
            "Add Money",
            "Remittance",
            "Deposit",
            "Credit",
            "ক্যাশ ইন",
            "জমা"
        ).forEach { kw ->
            assertEquals(
                "Expected INCOME for keyword '$kw'",
                TransactionType.INCOME,
                com.example.data.ai.ReceiptVoucherMatcher.classifyTransactionDirection(detectedKeyword = kw)
            )
        }

        // 2. Money Out / Expense (-) classification (English + Bangla) + Credit Card POS guard
        listOf(
            "Send Money",
            "Payment",
            "Cash Out",
            "Pay Bill",
            "Mobile Recharge",
            "Transfer",
            "SALE",
            "Debit",
            "Credit Card SALE",
            "সেন্ড মানি",
            "পেমেন্ট",
            "ক্যাশ আউট"
        ).forEach { kw ->
            assertEquals(
                "Expected EXPENSE for keyword '$kw'",
                TransactionType.EXPENSE,
                com.example.data.ai.ReceiptVoucherMatcher.classifyTransactionDirection(detectedKeyword = kw)
            )
        }

        // 3. Auto-matching across bKash, Nagad, Rocket, Bank last-4, and KD currency
        val accounts = listOf(
            AccountEntity(
                id = 10L,
                name = "bKash Personal",
                type = AccountType.MFS,
                balance = 4500.0,
                bankOrProvider = "bKash",
                accountNumberLast4 = "1947",
                colorArgb = 0xFFE2136EL,
                currencyCode = "BDT"
            ),
            AccountEntity(
                id = 20L,
                name = "নগদ ওয়ালেট",
                type = AccountType.MFS,
                balance = 2000.0,
                bankOrProvider = "Nagad",
                accountNumberLast4 = "4411",
                colorArgb = 0xFFEA580CL,
                currencyCode = "BDT"
            ),
            AccountEntity(
                id = 30L,
                name = "Rocket DBBL",
                type = AccountType.MFS,
                balance = 1200.0,
                bankOrProvider = "Rocket",
                accountNumberLast4 = "8899",
                colorArgb = 0xFF7C3AEDL,
                currencyCode = "BDT"
            ),
            AccountEntity(
                id = 40L,
                name = "City Bank Visa",
                type = AccountType.DEBIT_BANK,
                balance = 50000.0,
                bankOrProvider = "City Bank",
                accountNumberLast4 = "7721",
                colorArgb = 0xFFDC2626L,
                currencyCode = "BDT"
            ),
            AccountEntity(
                id = 50L,
                name = "KFH Kuwait",
                type = AccountType.DEBIT_BANK,
                balance = 140.5,
                bankOrProvider = "Kuwait Finance House (KFH)",
                accountNumberLast4 = "3308",
                colorArgb = 0xFF007A3DL,
                currencyCode = "KWD"
            )
        )

        val bkashMatch = com.example.data.ai.ReceiptVoucherMatcher.matchAccount(accounts, providerHint = "বিকাশ", currencyCode = "BDT")
        assertTrue(bkashMatch.isAutoMatched)
        assertEquals(10L, bkashMatch.account?.id)

        val nagadMatch = com.example.data.ai.ReceiptVoucherMatcher.matchAccount(accounts, providerHint = "Nagad", currencyCode = "BDT")
        assertTrue(nagadMatch.isAutoMatched)
        assertEquals(20L, nagadMatch.account?.id)

        val rocketMatch = com.example.data.ai.ReceiptVoucherMatcher.matchAccount(accounts, providerHint = "রকেট", currencyCode = "BDT")
        assertTrue(rocketMatch.isAutoMatched)
        assertEquals(30L, rocketMatch.account?.id)

        val last4Match = com.example.data.ai.ReceiptVoucherMatcher.matchAccount(accounts, providerHint = "", accountLast4Hint = "**** 7721", currencyCode = "BDT")
        assertTrue(last4Match.isAutoMatched)
        assertEquals(40L, last4Match.account?.id)

        val kwdCurrencyMatch = com.example.data.ai.ReceiptVoucherMatcher.matchAccount(accounts, providerHint = "POS Terminal", currencyCode = "KD")
        assertTrue(kwdCurrencyMatch.isAutoMatched)
        assertEquals(50L, kwdCurrencyMatch.account?.id)

        // 4. Temporary camera image cleanup removes cacheDir/receipts files completely
        val appContext = org.robolectric.RuntimeEnvironment.getApplication()
        val cacheDir = com.example.data.ai.TempReceiptImageCleaner.receiptCacheDir(appContext)
        val tempFile = java.io.File(cacheDir, "receipt_test_temp.jpg").apply { writeText("temp-bytes") }
        assertTrue("Temp receipt capture file should exist before cleanup", tempFile.exists())
        com.example.data.ai.TempReceiptImageCleaner.cleanupAllTempReceipts(appContext)
        assertFalse("Temp receipt capture file must be deleted on cleanup", tempFile.exists())
    }

    @Test
    fun receipt_scanner_confirmation_preview_auto_matches_card_and_updates_balance_for_income_and_expense() = runBlocking {
        val accounts = listOf(
            AccountEntity(
                id = 1L,
                name = "City Bank",
                type = AccountType.DEBIT_BANK,
                balance = 25000.0,
                bankOrProvider = "City Bank",
                colorArgb = 0xFFDC2626L,
                currencyCode = "BDT"
            ),
            AccountEntity(
                id = 2L,
                name = "bKash Personal",
                type = AccountType.MFS,
                balance = 3000.0,
                bankOrProvider = "bKash",
                accountNumberLast4 = "1947",
                colorArgb = 0xFFE2136EL,
                currencyCode = "BDT"
            )
        )

        // Create a temporary camera file to confirm ReceiptScanSheet cleans it up on save
        val appContext = org.robolectric.RuntimeEnvironment.getApplication()
        val staleTempFile = java.io.File(
            com.example.data.ai.TempReceiptImageCleaner.receiptCacheDir(appContext),
            "receipt_stale_capture.jpg"
        ).apply { writeText("img") }

        val cashInScan = com.example.data.model.ReceiptScanResult(
            merchantName = "bKash Agent Uttara",
            amountBdt = 5000.0,
            category = ExpenseCategory.SALARY_INCOME,
            dateLabel = "Today",
            itemsSummary = "Cash In from Agent",
            transactionType = TransactionType.INCOME,
            feeAmount = 0.0,
            currencyCode = "BDT",
            providerHint = "bKash",
            accountLast4Hint = "1947",
            referenceId = "BKS987654",
            detectedDirectionKeyword = "Cash In"
        )

        var capturedType: TransactionType? = null
        var capturedAmount = 0.0
        var capturedAccountId = 0L
        var capturedNote = ""
        var dismissed = false

        composeRule.setContent {
            AmarHisabTheme(darkTheme = false, isBangla = true) {
                ReceiptScanSheet(
                    accounts = accounts,
                    receiptState = ReceiptScanUiState.Success(cashInScan),
                    language = AppLanguage.BN,
                    strings = StringsProvider.bangla,
                    defaultCurrencyCode = "BDT",
                    onPickImageUri = {},
                    onSaveExpense = { _, _, _, _, _ -> },
                    onSaveVoucherTransaction = { _, amount, type, _, accId, note, _ ->
                        capturedAmount = amount
                        capturedType = type
                        capturedAccountId = accId
                        capturedNote = note
                    },
                    onDismiss = { dismissed = true }
                )
            }
        }
        composeRule.waitForIdle()

        // Confirmation preview card, auto-matched bKash badge, projected balance, and temp storage notice render cleanly
        composeRule.onNodeWithTag("receipt_confirmation_preview").assertIsDisplayed()
        composeRule.onNodeWithTag("receipt_matched_account_badge").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("receipt_projected_balance").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("receipt_temp_storage_notice").assertExists()

        // Tap Save -> confirms INCOME (+5000) to auto-matched bKash account (id=2) and cleans temp files
        composeRule.onNodeWithTag("receipt_save_expense_button")
            .performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick)
        composeRule.waitForIdle()

        assertEquals(TransactionType.INCOME, capturedType)
        assertEquals(5000.0, capturedAmount, 0.001)
        assertEquals(2L, capturedAccountId)
        assertTrue("Note should preserve detected TrxID", capturedNote.contains("BKS987654"))
        assertTrue("Sheet should dismiss cleanly after save", dismissed)
        assertFalse("Temp camera cache file must be cleared automatically", staleTempFile.exists())

        // Verify FinanceRepository updates card balance accurately (+ for Income, - for Expense with Fee)
        val db = androidx.room.Room.inMemoryDatabaseBuilder(
            appContext,
            com.example.data.local.AppDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()
        try {
            val repo = com.example.data.repository.FinanceRepository(db)
            val bkashId = repo.addAccount(accounts[1].copy(id = 0L))
            // 1. Apply scanned Cash In (+5000) -> 3000 + 5000 = 8000
            repo.addTransaction(
                title = "bKash Agent Uttara",
                amount = 5000.0,
                type = TransactionType.INCOME,
                category = ExpenseCategory.SALARY_INCOME,
                accountId = bkashId,
                note = "TrxID: BKS987654"
            )
            assertEquals(8000.0, db.accountDao().getAccountById(bkashId)!!.balance, 0.001)

            // 2. Apply scanned Cash Out (-1000 - 18.50 fee = -1018.50) -> 8000 - 1018.50 = 6981.50
            repo.addTransaction(
                title = "bKash Cash Out",
                amount = 1018.50,
                type = TransactionType.EXPENSE,
                category = ExpenseCategory.OTHER,
                accountId = bkashId,
                note = "Fee: ৳ 18.50"
            )
            assertEquals(6981.50, db.accountDao().getAccountById(bkashId)!!.balance, 0.001)
        } finally {
            db.close()
        }
    }

    @Test
    fun multi_currency_dues_and_loans_group_by_currency_support_picker_edit_and_settlement() = runBlocking {
        // 1. Verify smart default currency & clean zero-state per-currency summary
        assertEquals(
            "KWD",
            com.example.ui.screens.LoanCurrencyAggregator.defaultLoanCurrencyCode(
                mainCurrencyCode = "BDT",
                secondaryCurrencyCode = "KWD"
            )
        )
        assertEquals(
            "BDT",
            com.example.ui.screens.LoanCurrencyAggregator.defaultLoanCurrencyCode(
                mainCurrencyCode = "BDT",
                secondaryCurrencyCode = ""
            )
        )

        val zeroSummaries = com.example.ui.screens.LoanCurrencyAggregator.computeSummaries(
            loans = emptyList(),
            mainCurrencyCode = "BDT",
            secondaryCurrencyCode = "KWD"
        )
        assertEquals(listOf("KWD", "BDT"), zeroSummaries.map { it.currency.code })
        zeroSummaries.forEach { s ->
            assertEquals(0.0, s.totalPayableRemaining, 0.001)
            assertEquals(0.0, s.totalReceivableRemaining, 0.001)
        }

        // 2. Verify strictly separate per-currency totals (no mixing KD and BDT!)
        val sampleLoans = listOf(
            com.example.data.local.LoanEntity(
                id = 1L,
                personName = "Faisal Kuwait",
                loanType = com.example.data.model.LoanType.I_OWE,
                totalAmount = 40.50,
                paidAmount = 10.50,
                currencyCode = "KWD"
            ),
            com.example.data.local.LoanEntity(
                id = 2L,
                personName = "Rahim Dhaka",
                loanType = com.example.data.model.LoanType.I_OWE,
                totalAmount = 12000.0,
                paidAmount = 2000.0,
                currencyCode = "BDT"
            ),
            com.example.data.local.LoanEntity(
                id = 3L,
                personName = "Kamal Kuwait",
                loanType = com.example.data.model.LoanType.OWED_TO_ME,
                totalAmount = 75.0,
                paidAmount = 0.0,
                currencyCode = "KWD"
            )
        )
        val multiSummaries = com.example.ui.screens.LoanCurrencyAggregator.computeSummaries(
            loans = sampleLoans,
            mainCurrencyCode = "BDT",
            secondaryCurrencyCode = "KWD"
        )
        val kdSummary = multiSummaries.first { it.currency == com.example.data.model.AppCurrency.KWD }
        val bdtSummary = multiSummaries.first { it.currency == com.example.data.model.AppCurrency.BDT }
        assertEquals(30.0, kdSummary.totalPayableRemaining, 0.001)
        assertEquals(75.0, kdSummary.totalReceivableRemaining, 0.001)
        assertEquals(10000.0, bdtSummary.totalPayableRemaining, 0.001)
        assertEquals(0.0, bdtSummary.totalReceivableRemaining, 0.001)

        // 3. Verify Room persistence, edit, partial & full settlement across KWD and SAR
        val appContext = org.robolectric.RuntimeEnvironment.getApplication()
        val db = androidx.room.Room.inMemoryDatabaseBuilder(
            appContext,
            com.example.data.local.AppDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()
        try {
            val repo = com.example.data.repository.FinanceRepository(db)
            val kfhId = repo.addAccount(
                AccountEntity(
                    name = "KFH Kuwait",
                    type = AccountType.DEBIT_BANK,
                    balance = 100.0,
                    bankOrProvider = "KFH",
                    colorArgb = 0xFF007A3DL,
                    currencyCode = "KWD"
                )
            )

            repo.addLoan(
                personName = "Tariq Kuwait",
                loanType = com.example.data.model.LoanType.I_OWE,
                totalAmount = 40.50,
                note = "Room rent share",
                dueDateMillis = null,
                currencyCode = "KWD"
            )
            val inserted = db.loanDao().getAllLoansOnce().single()
            assertEquals("KWD", inserted.currencyCode)
            assertEquals(40.50, inserted.remainingAmount, 0.001)

            // Edit loan total & note cleanly
            repo.updateLoan(inserted.copy(totalAmount = 45.50, note = "Updated rent"))
            val edited = db.loanDao().getAllLoansOnce().single()
            assertEquals(45.50, edited.totalAmount, 0.001)
            assertEquals("KWD", edited.currencyCode)

            // Partial settlement from matching KWD wallet: 15.50 -> remaining 30.00, wallet 84.50
            repo.settleLoan(edited, settlementAmount = 15.50, walletId = kfhId)
            val afterPartial = db.loanDao().getAllLoansOnce().single()
            assertEquals(30.0, afterPartial.remainingAmount, 0.001)
            assertFalse(afterPartial.isSettled)
            assertEquals(84.50, db.accountDao().getAccountById(kfhId)!!.balance, 0.001)

            // Full settlement of remaining 30.00 without wallet -> settled cleanly
            repo.settleLoan(afterPartial, settlementAmount = 30.0, walletId = null)
            val afterFull = db.loanDao().getAllLoansOnce().single()
            assertTrue(afterFull.isSettled)
            assertEquals(0.0, afterFull.remainingAmount, 0.001)
            assertEquals(84.50, db.accountDao().getAccountById(kfhId)!!.balance, 0.001)
        } finally {
            db.close()
        }

        // 4. Verify UI renders separate KD and BDT summaries & supports adding a SAR due via currency picker
        var addedPerson = ""
        var addedAmount = 0.0
        var addedCurrency = ""
        val uiState = MainUiState(
            prefs = UserPrefs(
                language = AppLanguage.BN,
                mainCurrencyCode = "BDT",
                secondaryCurrencyCode = "KWD"
            ),
            loans = sampleLoans
        )

        composeRule.setContent {
            AmarHisabTheme(darkTheme = false, isBangla = true) {
                com.example.ui.screens.PlanScreen(
                    state = uiState,
                    strings = StringsProvider.bangla,
                    onOpenAllAccounts = {},
                    onOpenAddWizard = {},
                    onEditBudget = {},
                    onUpsertCategoryBudget = { _, _ -> },
                    onAddGoal = { _, _, _, _ -> },
                    onFundGoalFromWallet = { _, _, _ -> },
                    onDeleteGoal = {},
                    onAddLoan = { _, _, _, _, _ -> },
                    onAddLoanWithCurrency = { person, _, amt, _, _, curCode ->
                        addedPerson = person
                        addedAmount = amt
                        addedCurrency = curCode
                    },
                    onUpdateLoan = {},
                    onSettleLoan = { _, _, _ -> },
                    onSettleLoanOptionalWallet = { _, _, _ -> },
                    onDeleteLoan = {}
                )
            }
        }
        composeRule.waitForIdle()

        // Switch to Loans tab
        composeRule.onNodeWithTag("plan_tab_loans")
            .performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick)
        composeRule.waitForIdle()

        // Scroll LazyColumn so Loans summary card is composed & visible in Robolectric viewport
        composeRule.onNodeWithTag("plan_lazy_column")
            .performScrollToNode(hasTestTag("loans_currency_summary_card"))
        composeRule.waitForIdle()

        // Per-currency summary shows separate KWD (KD) and BDT rows
        composeRule.onNodeWithTag("loans_currency_summary_card").assertExists()
        composeRule.onNodeWithTag("loans_summary_currency_KWD").assertExists()
        composeRule.onNodeWithTag("loans_summary_currency_BDT").assertExists()

        // Open Add Loan dialog, pick SAR via full currency picker, enter amount & save
        composeRule.onNodeWithTag("plan_lazy_column")
            .performScrollToNode(hasTestTag("add_loan_button"))
        composeRule.onNodeWithTag("add_loan_button")
            .performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick)
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("loan_currency_picker_button")
            .performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick)
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("app_currency_option_SAR")
            .performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick)
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("loan_person_input").performTextReplacement("Saeed Riyadh")
        composeRule.onNodeWithTag("loan_amount_input").performTextReplacement("250.50")
        composeRule.onNodeWithTag("loan_save_button")
            .performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick)
        composeRule.waitForIdle()

        assertEquals("Saeed Riyadh", addedPerson)
        assertEquals(250.50, addedAmount, 0.001)
        assertEquals("SAR", addedCurrency)
    }
}



