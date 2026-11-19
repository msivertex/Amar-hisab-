package com.example.ui.screens

import android.content.Context
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.DashboardCustomize
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.R
import com.example.core.BiometricAuthFailureReason
import com.example.core.BiometricAuthHelper
import com.example.core.BiometricAvailability
import com.example.core.BiometricCapabilityInfo
import com.example.core.GoogleAccountProfile
import com.example.core.GoogleAuthManager
import com.example.core.GoogleSignInOutcome
import com.example.data.local.AccountEntity
import com.example.data.local.TransactionEntity
import com.example.data.local.UserPrefs
import com.example.data.model.AppColourTheme
import com.example.data.model.AppThemeMode
import com.example.data.model.AuthMode
import com.example.data.model.CategorySortMode
import com.example.data.model.ExpenseCategory
import com.example.data.model.HomeLayoutStyle
import com.example.data.model.HomeSectionId
import com.example.data.model.ManagedCategoryItem
import com.example.data.model.StartOfWeek
import com.example.data.model.TransactionType
import com.example.localization.AppLanguage
import com.example.localization.AppStrings
import com.example.localization.BdtFormatter
import com.example.ui.components.BuiltInPresetAvatars
import com.example.ui.components.CategoryIconBadge
import com.example.ui.components.CrystalCard
import com.example.ui.components.GoogleGLogo
import com.example.ui.components.GooglePreviewAccountChooserDialog
import com.example.ui.components.PresetAvatarOption
import com.example.ui.components.UserProfileAvatar
import com.example.ui.theme.LocalCrystalColors
import com.example.ui.viewmodel.BackupMessage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class SettingsSubPage {
    MAIN,
    DETAILED_PROFILE,
    THEME_CUSTOMIZATION,
    LANGUAGE,
    BACKUP_RESTORE,
    CATEGORIES,
    ABOUT
}

@Composable
fun SettingsScreen(
    prefs: UserPrefs,
    accounts: List<AccountEntity>,
    backupStatus: BackupMessage?,
    strings: AppStrings,
    onBack: () -> Unit,
    onEditProfileBudget: () -> Unit,
    onLinkGoogleProfile: (GoogleAccountProfile) -> Unit,
    onUnlinkGoogleAccount: () -> Unit,
    onOpenAccounts: () -> Unit,
    onSetLanguage: (AppLanguage) -> Unit,
    onSetDarkTheme: (Boolean) -> Unit,
    onSetEnableSubcategories: (Boolean) -> Unit,
    onSetAutoOpenKeypad: (Boolean) -> Unit,
    onSetDefaultAccount: (Long) -> Unit,
    onSetStartOfWeek: (StartOfWeek) -> Unit,
    onSetHapticFeedback: (Boolean) -> Unit,
    onSetNotifications: (budget: Boolean, credit: Boolean, loan: Boolean) -> Unit,
    onUpdateDashboardLayout: (HomeLayoutStyle, List<HomeSectionId>, Set<HomeSectionId>) -> Unit,
    onSetAppLock: (enabled: Boolean, pin: String) -> Unit,
    onSetBiometricUnlock: (Boolean) -> Unit,
    onLockNow: () -> Unit,
    onExportJson: (android.net.Uri) -> Unit,
    onExportCsv: (android.net.Uri) -> Unit,
    onRestoreJson: (android.net.Uri) -> Unit,
    onClearBackupStatus: () -> Unit,
    onUpdateDisplayName: (String) -> Unit = {},
    onPickGuestAvatar: (Uri) -> Unit = {},
    onSelectPresetAvatar: (String) -> Unit = {},
    onClearGuestAvatar: () -> Unit = {},
    resolvedDarkTheme: Boolean = prefs.isDarkTheme,
    onSetThemeMode: (AppThemeMode) -> Unit = { mode ->
        if (mode != AppThemeMode.AUTO) onSetDarkTheme(mode == AppThemeMode.DARK)
    },
    onSetColourTheme: (AppColourTheme) -> Unit = {},
    onExportSampleTemplate: (android.net.Uri) -> Unit = {},
    onDeleteAllFinancialData: () -> Unit = {},
    onUpdateCategoryOrder: (TransactionType, List<String>) -> Unit = { _, _ -> },
    transactions: List<TransactionEntity> = emptyList(),
    onSetCategorySortMode: (CategorySortMode) -> Unit = {},
    onUpsertManagedCategory: (ManagedCategoryItem) -> Unit = {},
    onDeleteManagedCategory: (String, Boolean) -> Unit = { _, _ -> },
    onResetCategoriesToDefault: () -> Unit = {},
    biometricCapabilityOverride: BiometricCapabilityInfo? = null,
    biometricVerifyOverride: ((onSucceeded: () -> Unit, onFailure: (BiometricAuthFailureReason, String) -> Unit) -> Unit)? = null
) {
    BackHandler(onBack = onBack)
    val crystal = LocalCrystalColors.current
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val lang = prefs.language
    val dateStamp = remember { SimpleDateFormat("yyyyMMdd", Locale.US).format(Date()) }
    var biometricCapability by remember(context, biometricCapabilityOverride) {
        mutableStateOf(biometricCapabilityOverride ?: BiometricAuthHelper.inspectCapabilities(context))
    }
    val biometricAvailability = biometricCapability.availability
    var biometricFeedback by remember { mutableStateOf<String?>(null) }
    var biometricFeedbackIsError by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner, context, biometricCapabilityOverride) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && biometricCapabilityOverride == null) {
                biometricCapability = BiometricAuthHelper.inspectCapabilities(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var showDashboardCustomizer by remember { mutableStateOf(false) }
    var showWeekStartSheet by remember { mutableStateOf(false) }
    var showPinSetupDialog by remember { mutableStateOf(false) }
    var isGoogleSignInLoading by remember { mutableStateOf(false) }
    var showGooglePreviewChooser by remember { mutableStateOf(false) }
    var googleNotice by remember { mutableStateOf<String?>(null) }

    var showAvatarSheet by remember { mutableStateOf(false) }
    var activeSubPage by rememberSaveable { mutableStateOf(SettingsSubPage.MAIN) }

    fun triggerGoogleLink() {
        if (isGoogleSignInLoading) return
        googleNotice = null
        scope.launch {
            isGoogleSignInLoading = true
            when (val outcome = GoogleAuthManager.signInWithGoogle(context)) {
                is GoogleSignInOutcome.Success -> {
                    isGoogleSignInLoading = false
                    onLinkGoogleProfile(outcome.profile)
                }

                is GoogleSignInOutcome.NeedsPreviewChooser -> {
                    isGoogleSignInLoading = false
                    showGooglePreviewChooser = true
                }

                is GoogleSignInOutcome.Cancelled -> {
                    isGoogleSignInLoading = false
                    googleNotice = strings.googleSignInCancelled
                }

                is GoogleSignInOutcome.Error -> {
                    isGoogleSignInLoading = false
                    googleNotice = outcome.message
                }
            }
        }
    }

    // Play-compliant zero-permission Storage Access Framework launchers
    val exportJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> uri?.let(onExportJson) }

    val exportCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri -> uri?.let(onExportCsv) }

    val restoreJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let(onRestoreJson) }

    val exportTemplateLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri -> uri?.let(onExportSampleTemplate) }

    AnimatedContent(
        targetState = activeSubPage,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "settingsSubPageTransition"
    ) { subPage ->
        when (subPage) {
            SettingsSubPage.DETAILED_PROFILE -> {
                DetailedProfileSubPage(
                    prefs = prefs,
                    strings = strings,
                    isGoogleSignInLoading = isGoogleSignInLoading,
                    googleNotice = googleNotice,
                    onBack = { activeSubPage = SettingsSubPage.MAIN },
                    onOpenAvatarSelector = { showAvatarSheet = true },
                    onUpdateDisplayName = onUpdateDisplayName,
                    onTriggerGoogleSignIn = { triggerGoogleLink() },
                    onUnlinkGoogleAccount = onUnlinkGoogleAccount
                )
            }

            SettingsSubPage.THEME_CUSTOMIZATION -> {
                ThemeCustomizationScreen(
                    prefs = prefs,
                    strings = strings,
                    onBack = { activeSubPage = SettingsSubPage.MAIN },
                    onSelectThemeMode = onSetThemeMode,
                    onSelectColourTheme = onSetColourTheme
                )
            }

            SettingsSubPage.LANGUAGE -> {
                LanguageSelectionSubPage(
                    activeLanguage = prefs.language,
                    strings = strings,
                    onSelectLanguage = onSetLanguage,
                    onBack = { activeSubPage = SettingsSubPage.MAIN }
                )
            }

            SettingsSubPage.BACKUP_RESTORE -> {
                BackupRestoreSubPage(
                    prefs = prefs,
                    backupStatus = backupStatus,
                    isGoogleSignInLoading = isGoogleSignInLoading,
                    googleNotice = googleNotice,
                    strings = strings,
                    onBack = { activeSubPage = SettingsSubPage.MAIN },
                    onExportJsonClick = {
                        exportJsonLauncher.launch("amar_hisab_backup_$dateStamp.json")
                    },
                    onExportCsvClick = {
                        exportCsvLauncher.launch("amar_hisab_ledger_$dateStamp.csv")
                    },
                    onImportRestoreClick = {
                        restoreJsonLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                    },
                    onDownloadSampleTemplateClick = {
                        exportTemplateLauncher.launch("amar_hisab_sample_template.csv")
                    },
                    onTriggerGoogleSignIn = { triggerGoogleLink() },
                    onDeleteAllFinancialData = onDeleteAllFinancialData,
                    onClearBackupStatus = onClearBackupStatus
                )
            }

            SettingsSubPage.CATEGORIES -> {
                CategoriesManagementSubPage(
                    prefs = prefs,
                    strings = strings,
                    transactions = transactions,
                    onReorderCategories = onUpdateCategoryOrder,
                    onSetSortMode = onSetCategorySortMode,
                    onUpsertCategory = onUpsertManagedCategory,
                    onDeleteCategory = onDeleteManagedCategory,
                    onResetDefaults = onResetCategoriesToDefault,
                    onBack = { activeSubPage = SettingsSubPage.MAIN }
                )
            }

            SettingsSubPage.ABOUT -> {
                AboutSubPage(
                    strings = strings,
                    onBack = { activeSubPage = SettingsSubPage.MAIN }
                )
            }

            SettingsSubPage.MAIN -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter
            ) {
                LazyColumn(
                    modifier = Modifier
                        .widthIn(max = 620.dp)
                        .fillMaxSize()
                        .testTag("settings_list"),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 40.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("settings_back_button")
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = crystal.glassSurfaceElevated,
                            border = BorderStroke(1.dp, crystal.glassBorder),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = strings.close,
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = strings.settingsTitle,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            if (backupStatus != null) {
                item {
                    Surface(
                        color = if (backupStatus.isError) crystal.liabilityRedBg else crystal.assetGreenBg,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = backupStatus.text,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (backupStatus.isError) crystal.liabilityRed else crystal.assetGreen,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = onClearBackupStatus) {
                                Text(strings.close)
                            }
                        }
                    }
                }
            }

            // 1. Main Settings Profile Card (Avatar, Name, Country, Currency, Sync Prompt, Primary Google Sign-In, Arrow -> Detailed Profile)
            item {
                val effectiveName = prefs.userName.ifBlank { strings.profileDetails.guestUserFallback }
                CrystalCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_profile_card"),
                    elevated = true
                ) {
                    Text(
                        text = strings.settingsProfileSection,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { activeSubPage = SettingsSubPage.DETAILED_PROFILE },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .clickable { showAvatarSheet = true }
                                .testTag("settings_profile_avatar_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            UserProfileAvatar(
                                userName = effectiveName,
                                avatarUrl = prefs.effectiveAvatarUrl,
                                authMode = prefs.authMode,
                                size = 58.dp,
                                contentDescription = strings.chooseProfilePhotoTitle
                            )
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = effectiveName,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = if (crystal.isDark) Color(0xFF1B2335) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
                                    shape = CircleShape,
                                    border = BorderStroke(0.8.dp, crystal.glassBorder)
                                ) {
                                    Text(
                                        text = strings.profileDetails.countryValue,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                    )
                                }
                                Surface(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = if (crystal.isDark) 0.18f else 0.12f),
                                    shape = CircleShape,
                                    border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                                ) {
                                    Text(
                                        text = strings.profileDetails.primaryCurrencyValue,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        IconButton(
                            onClick = { activeSubPage = SettingsSubPage.DETAILED_PROFILE },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("settings_open_detailed_profile_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = strings.profileDetails.openProfileDetailsDesc,
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = crystal.glassBorder
                    )

                    if (prefs.authMode == AuthMode.GUEST) {
                        Text(
                            text = strings.profileDetails.guestSyncPrompt,
                            style = MaterialTheme.typography.bodySmall,
                            color = crystal.mutedText
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { triggerGoogleLink() },
                            enabled = !isGoogleSignInLoading,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("settings_profile_google_signin_button")
                        ) {
                            if (isGoogleSignInLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                            } else {
                                GoogleGLogo(size = 18.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = strings.linkGoogleAccountBtn,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    } else {
                        Text(
                            text = prefs.userEmail.ifBlank { strings.profileDetails.googleSignedInSummary },
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = crystal.assetGreen,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (googleNotice != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = googleNotice!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = crystal.warningAmber
                        )
                    }
                }
            }

            // 2. General & Customization (Verified Tiles + Financial Alert Toggles)
            item {
                val refined = strings.settingsRefinement
                val modeDisplayLabel = when (prefs.themeMode) {
                    AppThemeMode.AUTO -> "Auto"
                    AppThemeMode.LIGHT -> "Light"
                    AppThemeMode.DARK -> "Dark"
                }
                val activeLanguageDisplay = if (prefs.language == AppLanguage.BN) {
                    refined.languageActiveBangla
                } else {
                    refined.languageActiveEnglish
                }

                CrystalCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_general_customization_card"),
                    elevated = true
                ) {
                    SettingsSectionHeader(
                        title = strings.settingsCustomization,
                        icon = Icons.Filled.Tune
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    // Dedicated "Theme" List Tile
                    SettingsNavTile(
                        title = strings.themeSettings.themeTileTitle,
                        subtitle = "${prefs.colourTheme.label(lang)} · $modeDisplayLabel",
                        icon = Icons.Filled.Palette,
                        testTag = "settings_theme_tile",
                        onClick = { activeSubPage = SettingsSubPage.THEME_CUSTOMIZATION }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Dedicated "Language" List Tile -> navigates to LanguageSelectionSubPage
                    SettingsNavTile(
                        title = refined.languageTileTitle,
                        subtitle = activeLanguageDisplay,
                        icon = Icons.Filled.Translate,
                        testTag = "settings_language_tile",
                        onClick = { activeSubPage = SettingsSubPage.LANGUAGE }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Dedicated "Customize Home" List Tile -> opens HomeDashboardCustomizerSheet
                    SettingsNavTile(
                        title = strings.customizeDashboard,
                        subtitle = refined.customizeHomeSubtitle,
                        icon = Icons.Filled.DashboardCustomize,
                        testTag = "customize_home_button",
                        onClick = { showDashboardCustomizer = true }
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 14.dp),
                        color = crystal.glassBorder
                    )

                    // Financial Alerts Sub-header
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(horizontal = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.NotificationsActive,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = refined.financialAlertsGroupTitle,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    SettingSwitchRow(
                        label = strings.notifBudgetAlerts,
                        subtitle = refined.notifBudgetAlertsSubtitle,
                        icon = Icons.Filled.PieChart,
                        iconTint = MaterialTheme.colorScheme.primary,
                        checked = prefs.notifBudgetAlerts,
                        testTag = "settings_toggle_budget_alert",
                        onCheckedChange = {
                            onSetNotifications(it, prefs.notifCreditAlerts, prefs.notifLoanReminders)
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    SettingSwitchRow(
                        label = strings.notifCreditAlerts,
                        subtitle = refined.notifCreditAlertsSubtitle,
                        icon = Icons.Filled.CreditCard,
                        iconTint = crystal.warningAmber,
                        checked = prefs.notifCreditAlerts,
                        testTag = "settings_toggle_credit_alert",
                        onCheckedChange = {
                            onSetNotifications(prefs.notifBudgetAlerts, it, prefs.notifLoanReminders)
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    SettingSwitchRow(
                        label = strings.notifLoanReminders,
                        subtitle = refined.notifLoanRemindersSubtitle,
                        icon = Icons.Filled.EventNote,
                        iconTint = crystal.assetGreen,
                        checked = prefs.notifLoanReminders,
                        testTag = "settings_toggle_loan_reminder",
                        onCheckedChange = {
                            onSetNotifications(prefs.notifBudgetAlerts, prefs.notifCreditAlerts, it)
                        }
                    )
                }
            }

            // 3. Wallet, Currency & Data
            item {
                val refined = strings.settingsRefinement
                CrystalCard(
                    modifier = Modifier.fillMaxWidth(),
                    elevated = true
                ) {
                    SettingsSectionHeader(
                        title = strings.settingsWalletCurrencyData,
                        icon = Icons.Filled.AccountBalanceWallet
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    if (accounts.isNotEmpty()) {
                        Text(
                            text = strings.defaultWalletLabel,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        val effectiveDefaultId = accounts.firstOrNull { it.id == prefs.defaultAccountId }?.id
                            ?: accounts.firstOrNull()?.id ?: 0L
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            accounts.forEach { acc ->
                                FilterChip(
                                    selected = acc.id == effectiveDefaultId,
                                    onClick = { onSetDefaultAccount(acc.id) },
                                    label = { Text("${acc.name} (${BdtFormatter.format(acc.balance, lang)})") }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Main Currency (BDT)
                    Surface(
                        color = if (crystal.isDark) Color(0xFF161C2B) else crystal.glassSurfaceElevated,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, crystal.glassBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            MaterialTheme.colorScheme.primary.copy(
                                                alpha = if (crystal.isDark) 0.22f else 0.14f
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.CurrencyExchange,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Text(
                                    text = strings.mainCurrencyLabel,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text = strings.mainCurrencyValue,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    SettingsNavTile(
                        title = strings.categoriesOverviewLabel,
                        subtitle = refined.categoriesSubtitle,
                        icon = Icons.Filled.Category,
                        testTag = "settings_categories_tile",
                        onClick = { activeSubPage = SettingsSubPage.CATEGORIES }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    SettingsNavTile(
                        title = "${strings.manageAccountsLabel} (${BdtFormatter.formatNumber(accounts.size, lang)})",
                        subtitle = refined.manageAccountsSubtitle,
                        icon = Icons.Filled.AccountBalanceWallet,
                        testTag = "settings_manage_accounts_tile",
                        onClick = onOpenAccounts
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Dedicated "Backup & Restore" List Tile
                    SettingsNavTile(
                        title = strings.backupRestore.tileTitle,
                        subtitle = strings.backupRestore.tileSubtitle,
                        icon = Icons.Filled.Sync,
                        testTag = "settings_backup_restore_tile",
                        onClick = { activeSubPage = SettingsSubPage.BACKUP_RESTORE }
                    )
                }
            }

            // 4. Date & Time
            item {
                CrystalCard(
                    modifier = Modifier.fillMaxWidth(),
                    elevated = true
                ) {
                    SettingsSectionHeader(
                        title = strings.settingsDateTimeSection,
                        icon = Icons.Filled.Schedule
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    SettingsNavTile(
                        title = strings.startOfWeekLabel,
                        subtitle = prefs.startOfWeek.label(lang),
                        icon = Icons.Filled.EventNote,
                        testTag = "settings_start_of_week_tile",
                        onClick = { showWeekStartSheet = true }
                    )
                }
            }

            // 5. Security & System
            item {
                val refined = strings.settingsRefinement
                val bioStrings = strings.biometricSecurity
                CrystalCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_security_card"),
                    elevated = true
                ) {
                    SettingsSectionHeader(
                        title = strings.settingsSecuritySystem,
                        icon = Icons.Filled.Security
                    )

                    if (prefs.isLaunchLockActive) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = crystal.assetGreen.copy(alpha = if (crystal.isDark) 0.16f else 0.10f),
                            border = BorderStroke(1.dp, crystal.assetGreen.copy(alpha = 0.35f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_vault_protected_badge")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.VerifiedUser,
                                    contentDescription = null,
                                    tint = crystal.assetGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = bioStrings.vaultProtectedBadge,
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                        color = crystal.assetGreen
                                    )
                                    Text(
                                        text = bioStrings.autoLockPrivacyHint,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = crystal.mutedText
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Direct Biometric Unlock on Launch (Fingerprint / Face) Toggle
                    SettingSwitchRow(
                        label = bioStrings.biometricLaunchTitle,
                        subtitle = bioStrings.biometricLaunchSubtitle,
                        icon = Icons.Filled.Fingerprint,
                        iconTint = crystal.assetGreen,
                        checked = prefs.biometricUnlockEnabled,
                        testTag = "settings_toggle_biometric",
                        onCheckedChange = { enable ->
                            biometricFeedback = null
                            biometricFeedbackIsError = false
                            if (!enable) {
                                onSetBiometricUnlock(false)
                                biometricFeedback = bioStrings.biometricDisabledNotice
                                biometricFeedbackIsError = false
                            } else if (!biometricCapability.canAuthenticate) {
                                biometricFeedbackIsError = true
                                biometricFeedback = if (biometricAvailability == BiometricAvailability.NONE_ENROLLED) {
                                    bioStrings.notEnrolledErrorMsg
                                } else {
                                    bioStrings.hardwareUnavailableErrorMsg
                                }
                            } else {
                                val handleSuccess = {
                                    onSetBiometricUnlock(true)
                                    biometricFeedbackIsError = false
                                    biometricFeedback = bioStrings.biometricEnabledNotice
                                }
                                val handleFailure: (BiometricAuthFailureReason, String) -> Unit = { reason, raw ->
                                    if (reason != BiometricAuthFailureReason.CANCELED) {
                                        biometricFeedbackIsError = true
                                        biometricFeedback = when (reason) {
                                            BiometricAuthFailureReason.LOCKOUT -> bioStrings.lockoutErrorMsg
                                            BiometricAuthFailureReason.LOCKOUT_PERMANENT -> bioStrings.permanentLockoutErrorMsg
                                            BiometricAuthFailureReason.NO_BIOMETRICS -> bioStrings.notEnrolledErrorMsg
                                            BiometricAuthFailureReason.HARDWARE_UNAVAILABLE -> bioStrings.hardwareUnavailableErrorMsg
                                            else -> raw.ifBlank { bioStrings.hardwareUnavailableErrorMsg }
                                        }
                                    }
                                }
                                if (biometricVerifyOverride != null) {
                                    biometricVerifyOverride(handleSuccess, handleFailure)
                                } else {
                                    BiometricAuthHelper.authenticate(
                                        context = context,
                                        title = bioStrings.verifyBiometricTitle,
                                        subtitle = bioStrings.verifyBiometricSubtitle,
                                        negativeButtonText = bioStrings.verifyCancelBtn,
                                        allowDeviceCredentialFallback = biometricAvailability != BiometricAvailability.AVAILABLE,
                                        onSucceeded = handleSuccess,
                                        onError = { /* handled via onFailureReason */ },
                                        onFailureReason = handleFailure
                                    )
                                }
                            }
                        }
                    )

                    val bioStatusText = when {
                        biometricAvailability == BiometricAvailability.AVAILABLE &&
                            biometricCapability.hasFingerprint && biometricCapability.hasFace ->
                            bioStrings.fingerprintAndFaceReady
                        biometricAvailability == BiometricAvailability.AVAILABLE && biometricCapability.hasFace ->
                            bioStrings.faceReady
                        biometricAvailability == BiometricAvailability.AVAILABLE ->
                            bioStrings.fingerprintReady
                        biometricCapability.canUseDeviceCredential ->
                            bioStrings.deviceLockFallbackReady
                        biometricAvailability == BiometricAvailability.NONE_ENROLLED ->
                            strings.biometricNotEnrolledBadge
                        else ->
                            strings.biometricUnavailableBadge
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = bioStatusText,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (biometricCapability.canAuthenticate) {
                            crystal.assetGreen
                        } else {
                            crystal.mutedText
                        },
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .testTag("settings_biometric_status_text")
                    )

                    if (biometricAvailability == BiometricAvailability.NONE_ENROLLED) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                runCatching {
                                    context.startActivity(BiometricAuthHelper.createEnrollIntent())
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("settings_enroll_biometric_button")
                        ) {
                            Icon(Icons.Filled.Fingerprint, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(bioStrings.enrollBiometricsBtn, maxLines = 1)
                        }
                    }

                    if (!biometricFeedback.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (biometricFeedbackIsError) {
                                crystal.warningAmberBg
                            } else {
                                crystal.assetGreen.copy(alpha = if (crystal.isDark) 0.16f else 0.10f)
                            },
                            border = BorderStroke(
                                1.dp,
                                if (biometricFeedbackIsError) {
                                    crystal.warningAmber.copy(alpha = 0.4f)
                                } else {
                                    crystal.assetGreen.copy(alpha = 0.35f)
                                }
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_biometric_feedback")
                        ) {
                            Text(
                                text = biometricFeedback!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (biometricFeedbackIsError) crystal.warningAmber else crystal.assetGreen,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Optional 4-Digit App PIN Lock Toggle
                    SettingSwitchRow(
                        label = strings.appScreenLockLabel,
                        subtitle = refined.appScreenLockSubtitle,
                        icon = Icons.Filled.Lock,
                        iconTint = MaterialTheme.colorScheme.primary,
                        checked = prefs.appLockEnabled,
                        testTag = "settings_toggle_app_lock",
                        onCheckedChange = { enable ->
                            if (enable) {
                                showPinSetupDialog = true
                            } else {
                                onSetAppLock(false, prefs.appLockPin)
                            }
                        }
                    )

                    if (prefs.isLaunchLockActive) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (prefs.appLockEnabled) {
                                OutlinedButton(
                                    onClick = { showPinSetupDialog = true },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp)
                                ) {
                                    Text(strings.changePinLabel, maxLines = 1)
                                }
                            }
                            FilledTonalButton(
                                onClick = onLockNow,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("lock_vault_now_button")
                            ) {
                                Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.size(6.dp))
                                Text(strings.lockNowLabel, maxLines = 1)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    SettingSwitchRow(
                        label = strings.hapticFeedbackLabel,
                        subtitle = refined.hapticFeedbackSubtitle,
                        icon = Icons.Filled.Vibration,
                        iconTint = MaterialTheme.colorScheme.secondary,
                        checked = prefs.hapticFeedbackEnabled,
                        testTag = "settings_toggle_haptic",
                        onCheckedChange = onSetHapticFeedback
                    )
                }
            }

            // 6. About
            item {
                CrystalCard(
                    modifier = Modifier.fillMaxWidth(),
                    elevated = true
                ) {
                    SettingsNavTile(
                        title = strings.aboutPage.pageTitle,
                        subtitle = strings.aboutPage.tileSubtitle,
                        icon = Icons.Filled.Info,
                        testTag = "settings_about_tile",
                        onClick = { activeSubPage = SettingsSubPage.ABOUT }
                    )
                }
            }
        }
            }
        }
        }
    }

    if (showDashboardCustomizer) {
        HomeDashboardCustomizerSheet(
            prefs = prefs,
            strings = strings,
            onUpdateLayout = { layoutStyle, order, hidden ->
                onUpdateDashboardLayout(layoutStyle, order, hidden)
            },
            onDismiss = { showDashboardCustomizer = false }
        )
    }

    if (showWeekStartSheet) {
        WeekStartSelectorSheet(
            selectedDay = prefs.startOfWeek,
            language = lang,
            strings = strings,
            onSelectDay = onSetStartOfWeek,
            onDismiss = { showWeekStartSheet = false }
        )
    }

    if (showPinSetupDialog) {
        var pinInput by remember { mutableStateOf("") }
        var confirmInput by remember { mutableStateOf("") }
        val pinMismatch = confirmInput.length == 4 && pinInput != confirmInput

        AlertDialog(
            onDismissRequest = { showPinSetupDialog = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurface,
            icon = { Icon(Icons.Filled.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text(strings.setupPinTitle) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { pinInput = it.filter { c -> c.isDigit() }.take(4) },
                        label = { Text(strings.enter4DigitPin) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.testTag("pin_setup_input")
                    )
                    OutlinedTextField(
                        value = confirmInput,
                        onValueChange = { confirmInput = it.filter { c -> c.isDigit() }.take(4) },
                        label = { Text(strings.confirm4DigitPin) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        isError = pinMismatch,
                        supportingText = if (pinMismatch) {
                            { Text(strings.pinsDoNotMatch) }
                        } else null,
                        singleLine = true,
                        modifier = Modifier.testTag("pin_confirm_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pinInput.length == 4 && pinInput == confirmInput) {
                            onSetAppLock(true, pinInput)
                            showPinSetupDialog = false
                        }
                    },
                    enabled = pinInput.length == 4 && pinInput == confirmInput
                ) { Text(strings.saveChanges) }
            },
            dismissButton = {
                TextButton(onClick = { showPinSetupDialog = false }) { Text(strings.cancel) }
            }
        )
    }

    if (showGooglePreviewChooser) {
        GooglePreviewAccountChooserDialog(
            strings = strings,
            onSelectAccount = { selectedProfile ->
                showGooglePreviewChooser = false
                scope.launch {
                    isGoogleSignInLoading = true
                    delay(350L)
                    isGoogleSignInLoading = false
                    onLinkGoogleProfile(selectedProfile)
                }
            },
            onDismiss = { showGooglePreviewChooser = false }
        )
    }

    if (showAvatarSheet) {
        ProfileAvatarSelectorSheet(
            prefs = prefs,
            strings = strings,
            onSelectPhotoUri = { uri ->
                onPickGuestAvatar(uri)
                showAvatarSheet = false
            },
            onSelectPresetAvatar = { presetId ->
                onSelectPresetAvatar(presetId)
                showAvatarSheet = false
            },
            onRestoreDefault = {
                onClearGuestAvatar()
                showAvatarSheet = false
            },
            onDismiss = { showAvatarSheet = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileAvatarSelectorSheet(
    prefs: UserPrefs,
    strings: AppStrings,
    onSelectPhotoUri: (Uri) -> Unit,
    onSelectPresetAvatar: (String) -> Unit,
    onRestoreDefault: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val crystal = LocalCrystalColors.current
    val context = LocalContext.current
    val lang = prefs.language

    var showPresetGrid by remember { mutableStateOf(false) }
    var pendingCameraUriString by rememberSaveable { mutableStateOf<String?>(null) }
    var cameraNotice by remember { mutableStateOf<String?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                cameraNotice = null
                onSelectPhotoUri(uri)
            }
        }
    )

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { success ->
            val captured = pendingCameraUriString?.let(Uri::parse)
            if (success && captured != null) {
                cameraNotice = null
                onSelectPhotoUri(captured)
            }
        }
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .testTag("avatar_selector_sheet"),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                UserProfileAvatar(
                    userName = prefs.userName.ifBlank { strings.profileDetails.guestUserFallback },
                    avatarUrl = prefs.effectiveAvatarUrl,
                    authMode = prefs.authMode,
                    size = 50.dp,
                    contentDescription = strings.chooseProfilePhotoTitle
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = strings.chooseProfilePhotoTitle,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = strings.chooseProfilePhotoSubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = crystal.mutedText
                    )
                }
            }

            if (cameraNotice != null) {
                Surface(
                    color = crystal.warningAmberBg,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = cameraNotice!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = crystal.warningAmber,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }

            AnimatedContent(
                targetState = showPresetGrid,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "avatarSheetStepTransition"
            ) { inPresetStep ->
                if (!inPresetStep) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        AvatarSelectorActionRow(
                            title = strings.uploadPhotoOption,
                            subtitle = strings.uploadPhotoSubtitle,
                            icon = Icons.Filled.PhotoLibrary,
                            testTag = "avatar_option_upload_photo",
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                        )

                        AvatarSelectorActionRow(
                            title = strings.takePhotoOption,
                            subtitle = strings.takePhotoSubtitle,
                            icon = Icons.Filled.PhotoCamera,
                            testTag = "avatar_option_take_photo",
                            onClick = {
                                val outputUri = runCatching { createAvatarCaptureUri(context) }.getOrNull()
                                if (outputUri == null) {
                                    cameraNotice = strings.cameraUnavailableNotice
                                } else {
                                    pendingCameraUriString = outputUri.toString()
                                    val launched = runCatching { cameraLauncher.launch(outputUri) }.isSuccess
                                    if (!launched) {
                                        cameraNotice = strings.cameraUnavailableNotice
                                    }
                                }
                            }
                        )

                        AvatarSelectorActionRow(
                            title = strings.chooseAvatarOption,
                            subtitle = strings.chooseAvatarSubtitle,
                            icon = Icons.Filled.AutoAwesome,
                            testTag = "avatar_option_choose_avatar",
                            onClick = {
                                cameraNotice = null
                                showPresetGrid = true
                            }
                        )

                        if (prefs.hasCustomAvatarOverride) {
                            TextButton(
                                onClick = onRestoreDefault,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("avatar_option_restore_default")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Refresh,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (prefs.authMode == AuthMode.GOOGLE) {
                                        strings.restoreGooglePhoto
                                    } else {
                                        strings.resetProfilePhoto
                                    },
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            IconButton(
                                onClick = { showPresetGrid = false },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("avatar_picker_back_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = strings.backToOptions,
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text = strings.chooseAvatarOption,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("preset_avatar_grid"),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            BuiltInPresetAvatars.all.chunked(4).forEach { rowItems ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    rowItems.forEach { preset ->
                                        val isSelected = prefs.effectiveAvatarUrl == preset.id
                                        PresetAvatarGridItem(
                                            preset = preset,
                                            language = lang,
                                            selected = isSelected,
                                            modifier = Modifier.weight(1f),
                                            onClick = { onSelectPresetAvatar(preset.id) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
private fun AvatarSelectorActionRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    testTag: String,
    onClick: () -> Unit
) {
    val crystal = LocalCrystalColors.current
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = crystal.glassSurfaceElevated,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(
            width = 1.dp,
            color = if (crystal.isDark) Color.White.copy(alpha = 0.16f) else crystal.glassBorder
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = crystal.mutedText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = crystal.mutedText
            )
        }
    }
}

@Composable
private fun PresetAvatarGridItem(
    preset: PresetAvatarOption,
    language: AppLanguage,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val crystal = LocalCrystalColors.current
    val label = if (language == AppLanguage.BN) preset.labelBn else preset.labelEn

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp, horizontal = 4.dp)
            .testTag("preset_avatar_${preset.id}"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .border(
                    width = if (selected) 2.5.dp else 1.dp,
                    color = if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else if (crystal.isDark) {
                        Color.White.copy(alpha = 0.2f)
                    } else {
                        crystal.glassBorder
                    },
                    shape = CircleShape
                )
                .background(Brush.linearGradient(preset.gradient)),
            contentAlignment = Alignment.Center
        ) {
            if (preset.glyphText != null) {
                Text(
                    text = preset.glyphText,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = Color.White
                )
            } else if (preset.icon != null) {
                Icon(
                    imageVector = preset.icon,
                    contentDescription = label,
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            ),
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

private fun createAvatarCaptureUri(context: Context): Uri {
    val captureDir = File(context.cacheDir, "avatars_capture").apply { mkdirs() }
    val imageFile = File(captureDir, "avatar_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        imageFile
    )
}

@Composable
private fun SettingsSectionHeader(
    title: String,
    icon: ImageVector
) {
    val crystal = LocalCrystalColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    MaterialTheme.colorScheme.primary.copy(
                        alpha = if (crystal.isDark) 0.22f else 0.14f
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(17.dp)
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun SettingsNavTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    testTag: String,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit
) {
    val crystal = LocalCrystalColors.current
    val shape = RoundedCornerShape(16.dp)
    Surface(
        onClick = onClick,
        shape = shape,
        color = if (crystal.isDark) Color(0xFF161C2B) else crystal.glassSurfaceElevated,
        border = BorderStroke(
            width = 1.dp,
            color = if (crystal.isDark) Color.White.copy(alpha = 0.14f) else crystal.glassBorder
        ),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 62.dp)
            .shadow(
                elevation = if (crystal.isDark) 6.dp else 3.dp,
                shape = shape,
                ambientColor = if (crystal.isDark) Color.Black.copy(alpha = 0.45f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                spotColor = if (crystal.isDark) Color.Black.copy(alpha = 0.50f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            )
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconTint.copy(alpha = if (crystal.isDark) 0.22f else 0.14f))
                    .border(
                        width = 0.7.dp,
                        color = iconTint.copy(alpha = if (crystal.isDark) 0.34f else 0.22f),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = crystal.mutedText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = title,
                tint = crystal.mutedText
            )
        }
    }
}

@Composable
private fun SettingSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    subtitle: String? = null,
    icon: ImageVector? = null,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    testTag: String? = null
) {
    val crystal = LocalCrystalColors.current
    val shape = RoundedCornerShape(16.dp)
    Surface(
        onClick = { onCheckedChange(!checked) },
        shape = shape,
        color = if (crystal.isDark) Color(0xFF161C2B) else crystal.glassSurfaceElevated,
        border = BorderStroke(
            width = 1.dp,
            color = if (checked) {
                MaterialTheme.colorScheme.primary.copy(alpha = if (crystal.isDark) 0.34f else 0.26f)
            } else if (crystal.isDark) {
                Color.White.copy(alpha = 0.12f)
            } else {
                crystal.glassBorder
            }
        ),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .let { m -> if (testTag != null) m.testTag(testTag) else m }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 13.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(iconTint.copy(alpha = if (crystal.isDark) 0.22f else 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = crystal.mutedText,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    uncheckedThumbColor = if (crystal.isDark) Color(0xFFF8FAFC) else MaterialTheme.colorScheme.outline,
                    uncheckedTrackColor = if (crystal.isDark) Color(0xFF1B2335) else MaterialTheme.colorScheme.surfaceVariant,
                    uncheckedBorderColor = MaterialTheme.colorScheme.outline
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeDashboardCustomizerSheet(
    prefs: UserPrefs,
    strings: AppStrings,
    onUpdateLayout: (HomeLayoutStyle, List<HomeSectionId>, Set<HomeSectionId>) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val crystal = LocalCrystalColors.current
    val lang = prefs.language
    val density = LocalDensity.current
    val rowSwapThresholdPx = with(density) { 56.dp.toPx() }

    var selectedStyle by remember { mutableStateOf(prefs.homeLayoutStyle) }
    var workingOrder by remember { mutableStateOf(prefs.homeSectionOrder) }
    var workingHidden by remember { mutableStateOf(prefs.hiddenHomeSections) }

    fun applyChange(
        style: HomeLayoutStyle = selectedStyle,
        order: List<HomeSectionId> = workingOrder,
        hidden: Set<HomeSectionId> = workingHidden
    ) {
        val nextHidden = if (style == HomeLayoutStyle.BALANCE_CARD && selectedStyle != HomeLayoutStyle.BALANCE_CARD) {
            hidden - HomeSectionId.SUMMARY
        } else {
            hidden
        }
        selectedStyle = style
        workingOrder = order
        workingHidden = nextHidden
        onUpdateLayout(style, order, nextHidden)
    }

    ModalBottomSheet(
        onDismissRequest = {
            onUpdateLayout(selectedStyle, workingOrder, workingHidden)
            onDismiss()
        },
        sheetState = sheetState,
        containerColor = if (crystal.isDark) Color(0xFF111622) else MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = if (crystal.isDark) Color.White.copy(alpha = 0.28f) else crystal.glassBorder
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 16.dp)
                .testTag("dashboard_customizer_sheet"),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header with Reset Default action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DashboardCustomize,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Column {
                        Text(
                            text = strings.customizeDashboard,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = strings.dashboard.customizerSubtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = crystal.mutedText,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                TextButton(
                    onClick = {
                        applyChange(
                            style = HomeLayoutStyle.WALLET_STACK,
                            order = HomeSectionId.entries,
                            hidden = emptySet()
                        )
                    },
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("customizer_reset_button")
                ) {
                    Text(
                        text = strings.dashboard.resetDefaultLabel,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // 1. Visual Layout Style Selector (Balance Card vs. Wallet Card Stack)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = strings.dashboard.layoutStyleLabel,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    LayoutStylePreviewCard(
                        title = strings.dashboard.balanceCardViewTitle,
                        subtitle = strings.dashboard.balanceCardViewDesc,
                        isSelected = selectedStyle == HomeLayoutStyle.BALANCE_CARD,
                        isWalletStackPreview = false,
                        testTag = "layout_option_balance_card",
                        modifier = Modifier.weight(1f),
                        onClick = { applyChange(style = HomeLayoutStyle.BALANCE_CARD) }
                    )
                    LayoutStylePreviewCard(
                        title = strings.dashboard.walletStackViewTitle,
                        subtitle = strings.dashboard.walletStackViewDesc,
                        isSelected = selectedStyle == HomeLayoutStyle.WALLET_STACK,
                        isWalletStackPreview = true,
                        testTag = "layout_option_wallet_stack",
                        modifier = Modifier.weight(1f),
                        onClick = { applyChange(style = HomeLayoutStyle.WALLET_STACK) }
                    )
                }
            }

            // 2. Dashboard Sections Toggle & Reordering
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.dashboard.sectionsOrderLabel,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = strings.dashboard.reorderHint,
                        style = MaterialTheme.typography.labelSmall,
                        color = crystal.mutedText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    workingOrder.forEachIndexed { idx, section ->
                        val isVisible = section !in workingHidden
                        val sectionIcon = when (section) {
                            HomeSectionId.SUMMARY -> Icons.Filled.AccountBalance
                            HomeSectionId.WALLETS -> Icons.Filled.CreditCard
                            HomeSectionId.BUDGET -> Icons.Filled.PieChart
                            HomeSectionId.STREAK -> Icons.Filled.LocalFireDepartment
                            HomeSectionId.SHORTCUTS -> Icons.Filled.Bolt
                            HomeSectionId.RECENT -> Icons.Filled.ReceiptLong
                            HomeSectionId.UPCOMING_BILLS -> Icons.Filled.EventNote
                            HomeSectionId.GOALS -> Icons.Filled.Savings
                            HomeSectionId.FINANCIAL_HEALTH -> Icons.Filled.Favorite
                        }
                        var dragDeltaY by remember(section) { mutableFloatStateOf(0f) }

                        Surface(
                            color = if (crystal.isDark) {
                                if (isVisible) Color(0xFF161C2B) else Color(0xFF121724)
                            } else {
                                if (isVisible) crystal.glassSurfaceElevated else crystal.glassSurface
                            },
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (isVisible) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = if (crystal.isDark) 0.36f else 0.28f)
                                } else {
                                    crystal.glassBorder
                                }
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("customizer_section_row_${section.name}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 8.dp, end = 10.dp, top = 6.dp, bottom = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // Drag handle supporting direct vertical drag-to-reorder
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .pointerInput(workingOrder, idx) {
                                            detectVerticalDragGestures(
                                                onDragStart = { dragDeltaY = 0f },
                                                onDragEnd = { dragDeltaY = 0f },
                                                onDragCancel = { dragDeltaY = 0f },
                                                onVerticalDrag = { _, dragAmount ->
                                                    dragDeltaY += dragAmount
                                                    val currentIdx = workingOrder.indexOf(section)
                                                    if (currentIdx >= 0) {
                                                        if (dragDeltaY > rowSwapThresholdPx && currentIdx < workingOrder.lastIndex) {
                                                            val mutable = workingOrder.toMutableList()
                                                            val tmp = mutable[currentIdx + 1]
                                                            mutable[currentIdx + 1] = mutable[currentIdx]
                                                            mutable[currentIdx] = tmp
                                                            dragDeltaY -= rowSwapThresholdPx
                                                            applyChange(order = mutable)
                                                        } else if (dragDeltaY < -rowSwapThresholdPx && currentIdx > 0) {
                                                            val mutable = workingOrder.toMutableList()
                                                            val tmp = mutable[currentIdx - 1]
                                                            mutable[currentIdx - 1] = mutable[currentIdx]
                                                            mutable[currentIdx] = tmp
                                                            dragDeltaY += rowSwapThresholdPx
                                                            applyChange(order = mutable)
                                                        }
                                                    }
                                                }
                                            )
                                        }
                                        .testTag("drag_handle_${section.name}"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.DragIndicator,
                                        contentDescription = strings.dashboard.dragHandleDesc,
                                        tint = crystal.mutedText,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (isVisible) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                                            else crystal.mutedText.copy(alpha = 0.12f)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = sectionIcon,
                                        contentDescription = null,
                                        tint = if (isVisible) MaterialTheme.colorScheme.primary else crystal.mutedText,
                                        modifier = Modifier.size(17.dp)
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = section.label(lang),
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = if (isVisible) MaterialTheme.colorScheme.onSurface else crystal.mutedText,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                // Accessible Up / Down Move Controls (48dp touch targets)
                                IconButton(
                                    onClick = {
                                        if (idx > 0) {
                                            val mutable = workingOrder.toMutableList()
                                            val tmp = mutable[idx - 1]
                                            mutable[idx - 1] = mutable[idx]
                                            mutable[idx] = tmp
                                            applyChange(order = mutable)
                                        }
                                    },
                                    enabled = idx > 0,
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .size(36.dp)
                                        .testTag("move_up_${section.name}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.ArrowUpward,
                                        contentDescription = strings.dashboard.moveUpDesc,
                                        tint = if (idx > 0) MaterialTheme.colorScheme.onSurface else crystal.mutedText.copy(alpha = 0.32f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        if (idx < workingOrder.lastIndex) {
                                            val mutable = workingOrder.toMutableList()
                                            val tmp = mutable[idx + 1]
                                            mutable[idx + 1] = mutable[idx]
                                            mutable[idx] = tmp
                                            applyChange(order = mutable)
                                        }
                                    },
                                    enabled = idx < workingOrder.lastIndex,
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .size(36.dp)
                                        .testTag("move_down_${section.name}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.ArrowDownward,
                                        contentDescription = strings.dashboard.moveDownDesc,
                                        tint = if (idx < workingOrder.lastIndex) MaterialTheme.colorScheme.onSurface else crystal.mutedText.copy(alpha = 0.32f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Switch(
                                    checked = isVisible,
                                    onCheckedChange = { visible ->
                                        val nextHidden = if (visible) workingHidden - section else workingHidden + section
                                        applyChange(hidden = nextHidden)
                                    },
                                    modifier = Modifier.testTag("toggle_section_${section.name}"),
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                                        uncheckedThumbColor = if (crystal.isDark) Color(0xFFF8FAFC) else MaterialTheme.colorScheme.outline,
                                        uncheckedTrackColor = if (crystal.isDark) Color(0xFF1B2335) else MaterialTheme.colorScheme.surfaceVariant,
                                        uncheckedBorderColor = MaterialTheme.colorScheme.outline
                                    )
                                )
                            }
                        }
                    }
                }
            }

            Button(
                onClick = {
                    onUpdateLayout(selectedStyle, workingOrder, workingHidden)
                    onDismiss()
                },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 50.dp)
                    .testTag("customizer_done_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = strings.dashboard.doneLabel,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

@Composable
private fun LayoutStylePreviewCard(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    isWalletStackPreview: Boolean,
    testTag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val crystal = LocalCrystalColors.current
    val borderColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else if (crystal.isDark) {
        Color.White.copy(alpha = 0.16f)
    } else {
        crystal.glassBorder
    }
    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.primary.copy(alpha = if (crystal.isDark) 0.16f else 0.10f)
    } else if (crystal.isDark) {
        Color(0xFF161C2B)
    } else {
        crystal.glassSurfaceElevated
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = containerColor,
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor),
        modifier = modifier.testTag(testTag)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Visual mini illustration of Balance Card vs Wallet Card Stack
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (crystal.isDark) Color(0xFF0B0F19) else MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
                        )
                        .padding(6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isWalletStackPreview) {
                        // Layered stacked physical card wireframe peeking from top
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.82f)
                                    .height(14.dp)
                                    .clip(RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp))
                                    .background(Color(0xFFEC4899).copy(alpha = 0.78f))
                            )
                            Box(
                                modifier = Modifier
                                    .padding(top = 8.dp)
                                    .fillMaxWidth(0.91f)
                                    .height(14.dp)
                                    .clip(RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp))
                                    .background(Color(0xFFF97316).copy(alpha = 0.88f))
                            )
                            Box(
                                modifier = Modifier
                                    .padding(top = 16.dp)
                                    .fillMaxWidth()
                                    .height(22.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(
                                                MaterialTheme.colorScheme.primary,
                                                Color(0xFF0284C7)
                                            )
                                        )
                                    )
                            )
                        }
                    } else {
                        // Prominent Balance Card hero + compact strip wireframe
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(22.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.88f),
                                                Color(0xFF0EA5E9).copy(alpha = 0.65f)
                                            )
                                        )
                                    )
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                repeat(3) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(10.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(
                                                if (crystal.isDark) Color.White.copy(alpha = 0.18f)
                                                else MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                                            )
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .border(
                                width = 1.5.dp,
                                color = crystal.mutedText.copy(alpha = 0.45f),
                                shape = CircleShape
                            )
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = crystal.mutedText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
