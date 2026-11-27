package com.example

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import com.example.data.local.UserPrefs
import com.example.data.model.AuthMode
import com.example.localization.AppLanguage
import com.example.localization.StringsProvider
import com.example.ui.screens.DetailedProfileSubPage
import com.example.ui.screens.HomeDashboardScreen
import com.example.ui.theme.AmarHisabTheme
import com.example.ui.viewmodel.MainUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ProfileNameEditingRobolectricTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun homeHeaderOmitsNameEditPencilAndInlineEditorWhileAvatarStillOpensProfile() {
        var openedSettings = false
        var savedName = ""
        val prefs = UserPrefs(
            userName = "শরিফুল ইসলাম",
            language = AppLanguage.BN,
            authMode = AuthMode.GUEST
        )

        composeRule.setContent {
            AmarHisabTheme(darkTheme = true, isBangla = true) {
                HomeDashboardScreen(
                    state = MainUiState(prefs = prefs, isPrefsLoaded = true),
                    strings = StringsProvider.bangla,
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
        composeRule.onNodeWithTag("home_header_user_name").assertIsDisplayed()
        composeRule.onNodeWithText("শরিফুল ইসলাম").assertIsDisplayed()
        composeRule.onNodeWithTag("profile_name_edit_icon").assertDoesNotExist()
        composeRule.onNodeWithTag("profile_name_edit_trigger").assertDoesNotExist()
        composeRule.onNodeWithTag("inline_profile_editor_card").assertDoesNotExist()
        composeRule.onNodeWithTag("inline_name_input").assertDoesNotExist()
        assertEquals("", savedName)

        composeRule.onNodeWithTag("profile_settings_button").assertIsDisplayed().performClick()
        assertTrue(openedSettings)
    }

    @Test
    fun profileDetailsRemovesUserInfoCardAndEditsNameViaHeroPencilIcon() {
        var savedName = ""
        val prefs = UserPrefs(
            userName = "",
            language = AppLanguage.BN,
            authMode = AuthMode.GUEST
        )

        composeRule.setContent {
            AmarHisabTheme(darkTheme = true, isBangla = true) {
                DetailedProfileSubPage(
                    prefs = prefs,
                    strings = StringsProvider.bangla,
                    isGoogleSignInLoading = false,
                    googleNotice = null,
                    onBack = {},
                    onOpenAvatarSelector = {},
                    onUpdateDisplayName = { savedName = it },
                    onTriggerGoogleSignIn = {},
                    onUnlinkGoogleAccount = {}
                )
            }
        }

        composeRule.waitForIdle()

        // Separate User Info card is completely removed
        composeRule.onNodeWithTag("detailed_profile_user_info_card").assertDoesNotExist()
        composeRule.onNodeWithText("ব্যবহারকারীর তথ্য").assertDoesNotExist()

        // Hero display name and small pencil icon directly below avatar are displayed
        composeRule.onNodeWithTag("detailed_profile_hero_name").assertIsDisplayed()
        composeRule.onNodeWithTag("detailed_profile_edit_name_button").assertIsDisplayed()
        composeRule.onNodeWithTag("detailed_profile_hero_name_edit_icon", useUnmergedTree = true)
            .assertIsDisplayed()

        // Tap pencil icon -> opens clean edit dialog
        composeRule.onNodeWithTag("detailed_profile_edit_name_button")
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("detailed_profile_edit_name_dialog").assertIsDisplayed()

        // Submitting empty name shows error and does not persist
        composeRule.onNodeWithTag("detailed_profile_name_save_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("detailed_profile_name_error").assertIsDisplayed()
        assertEquals("", savedName)

        // Cancel dismisses dialog cleanly
        composeRule.onNodeWithTag("detailed_profile_name_cancel_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("detailed_profile_edit_name_dialog").assertDoesNotExist()

        // Re-open, enter name, and save
        composeRule.onNodeWithTag("detailed_profile_edit_name_button")
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("detailed_profile_name_input")
            .performTextReplacement("ফারহানা রহমান")
        composeRule.onNodeWithTag("detailed_profile_name_save_button").performClick()
        composeRule.waitForIdle()

        assertEquals("ফারহানা রহমান", savedName)
        composeRule.onNodeWithTag("detailed_profile_edit_name_dialog").assertDoesNotExist()
    }
}
