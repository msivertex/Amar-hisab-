package com.example

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.FragmentActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.isSystemInDarkTheme
import com.example.core.DevicePermissionsHelper
import com.example.data.model.AppTab
import com.example.data.model.AppThemeMode
import com.example.data.model.TransactionType
import com.example.localization.AppLanguage
import com.example.localization.StringsProvider
import com.example.ui.components.CrystalMeshBackground
import com.example.ui.components.FloatingCrystalBottomBar
import com.example.ui.screens.AccountsScreen
import com.example.ui.screens.ActivityScreen
import com.example.ui.screens.AddAccountWizardSheet
import com.example.ui.screens.AppLockScreen
import com.example.ui.screens.EditProfileBudgetDialog
import com.example.ui.screens.GlobalSearchSheet
import com.example.ui.screens.HomeDashboardScreen
import com.example.ui.screens.InsightsScreen
import com.example.ui.screens.LiveMarketScreen
import com.example.ui.screens.NotificationsSheet
import com.example.ui.screens.PlanScreen
import com.example.ui.screens.ReceiptScanSheet
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SmartTransactionModal
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.VoiceEntrySheet
import com.example.ui.screens.WelcomeScreen
import com.example.notifications.AmarHisabNotificationManager
import com.example.notifications.NotificationActionRoute
import com.example.ui.theme.AmarHisabTheme
import com.example.ui.viewmodel.MainViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MainActivity : FragmentActivity() {

    private val _pendingNotificationRoute = MutableStateFlow<NotificationActionRoute?>(null)
    private val pendingNotificationRoute: StateFlow<NotificationActionRoute?> =
        _pendingNotificationRoute.asStateFlow()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AmarHisabNotificationManager.ensureChannels(this)
        dispatchNotificationRoute(intent)

        enableEdgeToEdge()
        setContent {
            val externalRoute by pendingNotificationRoute.collectAsStateWithLifecycle()
            AmarHisabApp(
                pendingNotificationRoute = externalRoute,
                onConsumeNotificationRoute = { _pendingNotificationRoute.value = null }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        dispatchNotificationRoute(intent)
    }

    private fun dispatchNotificationRoute(intent: Intent?) {
        val rawRoute = intent?.getStringExtra(AmarHisabNotificationManager.EXTRA_NOTIFICATION_ROUTE)
        val parsed = NotificationActionRoute.fromWireName(rawRoute)
        if (parsed != null) {
            _pendingNotificationRoute.value = parsed
            intent?.removeExtra(AmarHisabNotificationManager.EXTRA_NOTIFICATION_ROUTE)
        }
    }
}

@Composable
fun AmarHisabApp(
    viewModel: MainViewModel = viewModel(),
    pendingNotificationRoute: NotificationActionRoute? = null,
    onConsumeNotificationRoute: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val receiptState by viewModel.receiptState.collectAsStateWithLifecycle()
    val searchAiState by viewModel.searchAiState.collectAsStateWithLifecycle()
    val voiceState by viewModel.voiceState.collectAsStateWithLifecycle()
    val ttsState by viewModel.ttsState.collectAsStateWithLifecycle()
    val marketSyncState by viewModel.marketSyncState.collectAsStateWithLifecycle()
    val supportUiState by viewModel.supportUiState.collectAsStateWithLifecycle()
    val isAppUnlocked by viewModel.isAppUnlocked.collectAsStateWithLifecycle()
    val backupStatus by viewModel.backupStatus.collectAsStateWithLifecycle()

    val strings = remember(uiState.prefs.language) {
        StringsProvider.get(uiState.prefs.language)
    }

    var showSplash by remember { mutableStateOf(true) }
    var showSmartTxModal by remember { mutableStateOf(false) }
    var smartModalInitialMode by remember { mutableStateOf(TransactionType.EXPENSE) }
    var showReceiptScanSheet by remember { mutableStateOf(false) }
    var showVoiceEntrySheet by remember { mutableStateOf(false) }
    var showAddAccountWizard by remember { mutableStateOf(false) }
    var showGlobalSearch by remember { mutableStateOf(false) }
    var showNotifications by remember { mutableStateOf(false) }
    var showProfileBudgetDialog by remember { mutableStateOf(false) }
    var requestedPlanTab by remember { mutableStateOf<Int?>(null) }

    val navigateNotificationRoute: (NotificationActionRoute) -> Unit = { route ->
        when (route) {
            NotificationActionRoute.QUICK_ADD_TX -> {
                viewModel.selectTab(AppTab.HOME)
                smartModalInitialMode = TransactionType.EXPENSE
                showSmartTxModal = true
            }
            NotificationActionRoute.OPEN_PLAN_LOANS -> {
                requestedPlanTab = 2
                viewModel.selectTab(AppTab.PLAN)
            }
            NotificationActionRoute.OPEN_INSIGHTS_BUDGET -> {
                viewModel.selectTab(AppTab.INSIGHTS)
            }
            NotificationActionRoute.OPEN_ACCOUNTS -> {
                viewModel.openAccountsScreen()
            }
            NotificationActionRoute.OPEN_HOME -> {
                viewModel.selectTab(AppTab.HOME)
            }
        }
    }

    LaunchedEffect(
        pendingNotificationRoute,
        uiState.isPrefsLoaded,
        uiState.prefs.hasCompletedWelcome,
        showSplash,
        isAppUnlocked
    ) {
        val route = pendingNotificationRoute
        val canNavigate = route != null &&
            uiState.isPrefsLoaded &&
            !showSplash &&
            uiState.prefs.hasCompletedWelcome &&
            (!uiState.prefs.isLaunchLockActive || isAppUnlocked)
        if (canNavigate && route != null) {
            navigateNotificationRoute(route)
            onConsumeNotificationRoute()
        }
    }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val isLaunchLockActive = uiState.prefs.isLaunchLockActive

    // Standard system runtime permissions requested once on initial app launch/setup
    // (Microphone for Voice Entry, Camera for Receipt Scan, Google Location for travel/region context).
    // Denying or modifying later still preserves full manual & offline functionality gracefully.
    var launchPermissionPromptDispatched by remember { mutableStateOf(false) }
    val launchPermissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        viewModel.markLaunchPermissionsRequested()
        val locationGranted = results.entries.any { (perm, granted) ->
            granted && perm in DevicePermissionsHelper.locationPermissions
        }
        if (locationGranted) {
            viewModel.refreshSystemTime()
            DevicePermissionsHelper.fetchGoogleLocationContext(context) { }
        }
    }

    LaunchedEffect(
        uiState.isPrefsLoaded,
        showSplash,
        uiState.prefs.hasRequestedLaunchPermissions
    ) {
        if (
            uiState.isPrefsLoaded &&
            !showSplash &&
            !uiState.prefs.hasRequestedLaunchPermissions &&
            !launchPermissionPromptDispatched
        ) {
            launchPermissionPromptDispatched = true
            val missing = DevicePermissionsHelper.missingLaunchPermissions(context)
            if (missing.isEmpty()) {
                viewModel.markLaunchPermissionsRequested()
            } else {
                launchPermissionsLauncher.launch(missing)
            }
        }
    }

    // Protect sensitive financial data from Recent Apps screenshots whenever vault lock is active
    DisposableEffect(context, isLaunchLockActive) {
        val activity = context.findHostActivity()
        if (isLaunchLockActive) {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
        onDispose { }
    }

    // Automatically re-lock financial vault when returning from background & refresh live device date/timezone
    DisposableEffect(lifecycleOwner, isLaunchLockActive) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> viewModel.onAppBackgrounded()
                Lifecycle.Event.ON_START -> viewModel.onAppForegrounded()
                Lifecycle.Event.ON_RESUME -> viewModel.onAppResumed()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val rootScreenKey = when {
        showSplash || !uiState.isPrefsLoaded -> "splash"
        isLaunchLockActive && !isAppUnlocked -> "lock"
        !uiState.prefs.hasCompletedWelcome -> "welcome"
        else -> "main"
    }

    val systemDark = isSystemInDarkTheme()
    val resolvedDark = when (uiState.prefs.themeMode) {
        AppThemeMode.AUTO -> systemDark
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
    }

    AmarHisabTheme(
        darkTheme = resolvedDark,
        colourTheme = uiState.prefs.colourTheme,
        isBangla = uiState.prefs.language.isBangla,
        isRtl = uiState.prefs.language.isRtl
    ) {
        AnimatedContent(
            targetState = rootScreenKey,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "rootScreenTransition"
        ) { screen ->
            when (screen) {
                "splash" -> {
                    SplashScreen(
                        strings = strings,
                        onSplashFinished = { showSplash = false }
                    )
                }

                "lock" -> {
                    AppLockScreen(
                        expectedPin = uiState.prefs.appLockPin,
                        biometricEnabled = uiState.prefs.biometricUnlockEnabled,
                        hapticEnabled = uiState.prefs.hapticFeedbackEnabled,
                        strings = strings,
                        onUnlocked = { viewModel.unlockApp() }
                    )
                }

                "welcome" -> {
                    WelcomeScreen(
                        language = uiState.prefs.language,
                        strings = strings,
                        backupErrorMessage = backupStatus?.takeIf { it.isError }?.text,
                        onGoogleSignedIn = { profile -> viewModel.linkGoogleProfile(profile) },
                        onContinueAsGuest = { viewModel.continueAsGuest(uiState.prefs.language) },
                        onRestoreBackupUri = { uri ->
                            viewModel.restoreJsonFromUri(
                                uri = uri,
                                successMessage = strings.restoreSuccessMsg,
                                completeWelcome = true
                            )
                        }
                    )
                }

                else -> {
                    CrystalMeshBackground {
                    Scaffold(
                        containerColor = Color.Transparent,
                        contentWindowInsets = WindowInsets.statusBars,
                        bottomBar = {
                            if (!uiState.showAccountsScreen && !uiState.showSettingsScreen && !uiState.showLiveMarketScreen) {
                                FloatingCrystalBottomBar(
                                    selectedTab = uiState.selectedTab,
                                    strings = strings,
                                    onSelectTab = { viewModel.selectTab(it) },
                                    onAddClick = {
                                        smartModalInitialMode = TransactionType.EXPENSE
                                        showSmartTxModal = true
                                    }
                                )
                            }
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            when {
                                uiState.showAccountsScreen -> {
                                    AccountsScreen(
                                        accounts = uiState.accounts,
                                        netWorth = uiState.netWorth,
                                        language = uiState.prefs.language,
                                        strings = strings,
                                        onBack = { viewModel.closeAccountsScreen() },
                                        onOpenAddWizard = { showAddAccountWizard = true },
                                        onDeleteAccount = { viewModel.deleteAccount(it) },
                                        mainCurrencyCode = uiState.prefs.mainCurrencyCode,
                                        secondaryCurrencyCode = uiState.prefs.secondaryCurrencyCode,
                                        marketSnapshot = marketSyncState.snapshot
                                    )
                                }

                                uiState.showSettingsScreen -> {
                                    SettingsScreen(
                                        prefs = uiState.prefs,
                                        accounts = uiState.accounts,
                                        backupStatus = backupStatus,
                                        strings = strings,
                                        onBack = { viewModel.closeSettingsScreen() },
                                        onEditProfileBudget = { showProfileBudgetDialog = true },
                                        onLinkGoogleProfile = { viewModel.linkGoogleProfile(it) },
                                        onUnlinkGoogleAccount = { viewModel.unlinkGoogleAccount() },
                                        onOpenAccounts = { viewModel.openAccountsScreen() },
                                        onSetLanguage = { viewModel.setLanguage(it) },
                                        onSetDarkTheme = { viewModel.setDarkTheme(it) },
                                        resolvedDarkTheme = resolvedDark,
                                        onSetThemeMode = { viewModel.setThemeMode(it) },
                                        onSetColourTheme = { viewModel.setColourTheme(it) },
                                        onSetEnableSubcategories = { viewModel.setEnableSubcategories(it) },
                                        onSetAutoOpenKeypad = { viewModel.setAutoOpenKeypad(it) },
                                        onSetDefaultAccount = { viewModel.setDefaultAccountId(it) },
                                        onSetStartOfWeek = { viewModel.setStartOfWeek(it) },
                                        onSetHapticFeedback = { viewModel.setHapticFeedback(it) },
                                        onSetNotifications = { budget, credit, loan ->
                                            viewModel.setNotificationPreferences(budget, credit, loan)
                                        },
                                        onUpdateDashboardLayout = { layoutStyle, order, hidden ->
                                            viewModel.updateHomeDashboardLayout(layoutStyle, order, hidden)
                                        },
                                        onSetAppLock = { enabled, pin ->
                                            viewModel.setAppLock(enabled, pin)
                                        },
                                        onSetBiometricUnlock = { enabled ->
                                            viewModel.setBiometricUnlock(enabled)
                                        },
                                        onLockNow = {
                                            viewModel.lockAppNow()
                                        },
                                        onExportJson = { uri ->
                                            viewModel.exportJsonToUri(uri, strings.backupSuccessMsg)
                                        },
                                        onExportCsv = { uri ->
                                            viewModel.exportCsvToUri(uri, strings.backupSuccessMsg)
                                        },
                                        onRestoreJson = { uri ->
                                            viewModel.restoreJsonFromUri(uri, strings.restoreSuccessMsg)
                                        },
                                        onExportSampleTemplate = { uri ->
                                            viewModel.exportSampleTemplateToUri(
                                                uri,
                                                strings.backupRestore.templateDownloadSuccessMsg
                                            )
                                        },
                                        onDeleteAllFinancialData = {
                                            viewModel.deleteAllFinancialData(strings.backupRestore.deleteSuccessMsg)
                                        },
                                        onClearBackupStatus = { viewModel.clearBackupStatus() },
                                        onUpdateDisplayName = { newName -> viewModel.updateDisplayName(newName) },
                                        onPickGuestAvatar = { uri -> viewModel.updateProfileAvatarFromUri(uri) },
                                        onSelectPresetAvatar = { presetId -> viewModel.selectPresetAvatar(presetId) },
                                        onClearGuestAvatar = { viewModel.resetProfileAvatar() },
                                        onUpdateCategoryOrder = { type, ids -> viewModel.updateCategoryOrder(type, ids) },
                                        transactions = uiState.transactions,
                                        onSetCategorySortMode = { mode -> viewModel.setCategorySortMode(mode) },
                                        onUpsertManagedCategory = { item -> viewModel.upsertManagedCategory(item) },
                                        onDeleteManagedCategory = { id, isBuiltIn -> viewModel.deleteManagedCategory(id, isBuiltIn) },
                                        onResetCategoriesToDefault = { viewModel.resetCategoriesToDefault() },
                                        deviceTimeContext = uiState.deviceTimeContext,
                                        onRefreshSystemTime = { viewModel.refreshSystemTime() },
                                        supportState = supportUiState,
                                        onEnsureSupportInitialized = { viewModel.ensureCustomerSupportInitialized() },
                                        onSelectSupportCategory = { viewModel.selectSupportCategory(it) },
                                        onSelectSupportQuickOption = { viewModel.selectSupportQuickOption(it) },
                                        onSendSupportMessage = { viewModel.sendCustomerSupportMessage(it) },
                                        onRequestLiveCareAfterTroubleshooting = { viewModel.requestLiveCareAfterTroubleshooting() },
                                        onResetSupportChat = { viewModel.resetCustomerSupportChat() },
                                        marketSnapshot = marketSyncState.snapshot,
                                        onSetUserCountry = { viewModel.setUserCountry(it) },
                                        onSetMainCurrency = { viewModel.setMainCurrency(it) },
                                        onSetSecondaryCurrency = { viewModel.setSecondaryCurrency(it) },
                                        onLogOut = { viewModel.logOut() },
                                        onSetDailyReminder = { enabled, hour, minute ->
                                            viewModel.setDailyReminderConfig(enabled, hour, minute)
                                        },
                                        onSetLoanDayBefore = { enabled ->
                                            viewModel.setLoanDayBeforeEnabled(enabled)
                                        },
                                        onSetHighSpendAlert = { enabled ->
                                            viewModel.setHighSpendAlertEnabled(enabled)
                                        },
                                        onShareBackupJson = { onReady ->
                                            viewModel.prepareShareBackupJson(onReady)
                                        }
                                    )
                                }

                                uiState.showLiveMarketScreen -> {
                                    LiveMarketScreen(
                                        language = uiState.prefs.language,
                                        marketSyncState = marketSyncState,
                                        strings = strings,
                                        onRefreshMarketRates = { viewModel.refreshMarketRates(force = true) },
                                        onBack = { viewModel.closeLiveMarketScreen() }
                                    )
                                }

                                else -> {
                                    AnimatedContent(
                                        targetState = uiState.selectedTab,
                                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                                        label = "tabTransition"
                                    ) { tab ->
                                        when (tab) {
                                            AppTab.HOME -> HomeDashboardScreen(
                                                state = uiState,
                                                strings = strings,
                                                onOpenSearch = { showGlobalSearch = true },
                                                onOpenNotifications = { showNotifications = true },
                                                onOpenSettings = { viewModel.openSettingsScreen() },
                                                onOpenProfileEdit = { showProfileBudgetDialog = true },
                                                onOpenAllAccounts = { viewModel.openAccountsScreen() },
                                                onOpenAddAccount = { showAddAccountWizard = true },
                                                onOpenTransfer = {
                                                    smartModalInitialMode = TransactionType.TRANSFER
                                                    showSmartTxModal = true
                                                },
                                                onOpenReceiptScan = {
                                                    viewModel.resetReceiptScan()
                                                    showReceiptScanSheet = true
                                                },
                                                onOpenQuickExpense = {
                                                    smartModalInitialMode = TransactionType.EXPENSE
                                                    showSmartTxModal = true
                                                },
                                                onOpenVoiceEntry = {
                                                    viewModel.resetVoiceParse()
                                                    showVoiceEntrySheet = true
                                                },
                                                onSeeAllActivity = { viewModel.selectTab(AppTab.ACTIVITY) },
                                                onOpenPlan = { viewModel.selectTab(AppTab.PLAN) },
                                                onUpdateName = { newName -> viewModel.updateDisplayName(newName) },
                                                onPickGuestAvatar = { uri -> viewModel.updateGuestAvatarFromUri(uri) },
                                                onClearGuestAvatar = { viewModel.clearGuestAvatar() },
                                                marketSyncState = marketSyncState,
                                                onOpenLiveMarket = { viewModel.openLiveMarketScreen() }
                                            )

                                            AppTab.ACTIVITY -> ActivityScreen(
                                                state = uiState,
                                                strings = strings,
                                                onOpenReceiptScan = {
                                                    viewModel.resetReceiptScan()
                                                    showReceiptScanSheet = true
                                                },
                                                onDeleteTransaction = { viewModel.deleteTransaction(it) },
                                                marketSnapshot = marketSyncState.snapshot
                                            )

                                            AppTab.PLAN -> PlanScreen(
                                                state = uiState,
                                                strings = strings,
                                                onOpenAllAccounts = { viewModel.openAccountsScreen() },
                                                onOpenAddWizard = { showAddAccountWizard = true },
                                                onEditBudget = { showProfileBudgetDialog = true },
                                                onUpsertCategoryBudget = { cat, limit ->
                                                    viewModel.upsertCategoryBudget(cat, limit)
                                                },
                                                onAddGoal = { name, target, dueDate, color ->
                                                    viewModel.addSavingsGoal(name, target, dueDate, color)
                                                },
                                                onFundGoalFromWallet = { goal, amt, walletId ->
                                                    viewModel.fundGoalFromWallet(goal, amt, walletId)
                                                },
                                                onDeleteGoal = { viewModel.deleteGoal(it) },
                                                onAddLoan = { person, type, amount, note, dueDate ->
                                                    viewModel.addLoan(person, type, amount, note, dueDate)
                                                },
                                                onAddLoanWithCurrency = { person, type, amount, note, dueDate, currencyCode ->
                                                    viewModel.addLoan(person, type, amount, note, dueDate, currencyCode)
                                                },
                                                onUpdateLoan = { updated ->
                                                    viewModel.updateLoan(updated)
                                                },
                                                onSettleLoan = { loan, amount, walletId ->
                                                    viewModel.settleLoan(loan, amount, walletId)
                                                },
                                                onSettleLoanOptionalWallet = { loan, amount, walletId ->
                                                    viewModel.settleLoan(loan, amount, walletId)
                                                },
                                                onDeleteLoan = { viewModel.deleteLoan(it) },
                                                marketSnapshot = marketSyncState.snapshot,
                                                requestedTab = requestedPlanTab,
                                                onConsumeRequestedTab = { requestedPlanTab = null }
                                            )

                                            AppTab.INSIGHTS -> InsightsScreen(
                                                state = uiState,
                                                ttsState = ttsState,
                                                searchAiState = searchAiState,
                                                strings = strings,
                                                onSelectFilter = { viewModel.setInsightFilter(it) },
                                                onShiftMonth = { viewModel.shiftInsightMonth(it) },
                                                onPlayBriefingTts = { viewModel.playFinancialBriefingTts() },
                                                onPauseTts = { viewModel.pauseTtsPlayback() },
                                                onResumeTts = { viewModel.resumeTtsPlayback() },
                                                onStopTts = { viewModel.stopTtsPlayback() },
                                                onSearchMarket = { viewModel.searchMarketInfo(it) },
                                                marketSyncState = marketSyncState,
                                                onRefreshMarketRates = { viewModel.refreshMarketRates(force = true) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Interactive Modals & Wizards
                    if (showSmartTxModal) {
                        SmartTransactionModal(
                            accounts = uiState.accounts,
                            initialMode = smartModalInitialMode,
                            defaultAccountId = uiState.prefs.defaultAccountId,
                            autoOpenKeypad = uiState.prefs.autoOpenKeypad,
                            enableSubcategories = uiState.prefs.enableSubcategories,
                            hapticEnabled = uiState.prefs.hapticFeedbackEnabled,
                            language = uiState.prefs.language,
                            strings = strings,
                            onDismiss = { showSmartTxModal = false },
                            onSaveExpenseOrIncome = { title, amount, type, category, subcat, accountId, note, timestamp ->
                                viewModel.addTransaction(
                                    title = title,
                                    amount = amount,
                                    type = type,
                                    category = category,
                                    accountId = accountId,
                                    subcategory = subcat,
                                    note = note,
                                    timestamp = timestamp
                                )
                            },
                            onExecuteTransfer = { fromId, toId, amount, note, timestamp ->
                                viewModel.transferFunds(
                                    fromAccountId = fromId,
                                    toAccountId = toId,
                                    amount = amount,
                                    note = note,
                                    timestamp = timestamp
                                )
                            },
                            onExecuteTransferWithRate = { fromId, toId, amount, note, timestamp, customRate ->
                                viewModel.transferFunds(
                                    fromAccountId = fromId,
                                    toAccountId = toId,
                                    amount = amount,
                                    note = note,
                                    timestamp = timestamp,
                                    customConversionRate = customRate
                                )
                            },
                            defaultCurrencyCode = uiState.prefs.mainCurrencyCode,
                            marketSnapshot = marketSyncState.snapshot,
                            managedCategoryOverrides = uiState.prefs.managedCategoryOverrides,
                            deletedCategoryIds = uiState.prefs.deletedCategoryIds,
                            onAddCustomCategory = { item -> viewModel.upsertManagedCategory(item) }
                        )
                    }

                    if (showReceiptScanSheet) {
                        ReceiptScanSheet(
                            accounts = uiState.accounts,
                            receiptState = receiptState,
                            language = uiState.prefs.language,
                            strings = strings,
                            defaultCurrencyCode = uiState.prefs.mainCurrencyCode,
                            onPickImageUri = { viewModel.analyzeReceiptUri(it) },
                            onSaveExpense = { title, amount, category, accountId, note ->
                                viewModel.addTransaction(
                                    title = title,
                                    amount = amount,
                                    type = TransactionType.EXPENSE,
                                    category = category,
                                    accountId = accountId,
                                    note = note
                                )
                            },
                            onSaveExpenseWithDate = { title, amount, category, accountId, note, timestamp ->
                                viewModel.addTransaction(
                                    title = title,
                                    amount = amount,
                                    type = TransactionType.EXPENSE,
                                    category = category,
                                    accountId = accountId,
                                    note = note,
                                    timestamp = timestamp
                                )
                            },
                            onSaveVoucherTransaction = { title, amount, type, category, accountId, note, timestamp ->
                                viewModel.addTransaction(
                                    title = title,
                                    amount = amount,
                                    type = type,
                                    category = category,
                                    accountId = accountId,
                                    note = note,
                                    timestamp = timestamp
                                )
                            },
                            onSaveVoucherWithSubcategory = { title, amount, type, category, subcat, accountId, note, timestamp ->
                                viewModel.addTransaction(
                                    title = title,
                                    amount = amount,
                                    type = type,
                                    category = category,
                                    accountId = accountId,
                                    subcategory = subcat,
                                    note = note,
                                    timestamp = timestamp
                                )
                            },
                            onExecuteTransfer = { fromId, toId, amount, note, timestamp ->
                                viewModel.transferFunds(
                                    fromAccountId = fromId,
                                    toAccountId = toId,
                                    amount = amount,
                                    note = note,
                                    timestamp = timestamp
                                )
                            },
                            managedCategoryOverrides = uiState.prefs.managedCategoryOverrides,
                            deletedCategoryIds = uiState.prefs.deletedCategoryIds,
                            onAddCustomCategory = { item -> viewModel.upsertManagedCategory(item) },
                            onAddAccount = {
                                showReceiptScanSheet = false
                                viewModel.resetReceiptScan()
                                showAddAccountWizard = true
                            },
                            onDismiss = {
                                showReceiptScanSheet = false
                                viewModel.resetReceiptScan()
                            }
                        )
                    }

                    if (showVoiceEntrySheet) {
                        VoiceEntrySheet(
                            accounts = uiState.accounts,
                            voiceState = voiceState,
                            ttsState = ttsState,
                            language = uiState.prefs.language,
                            strings = strings,
                            onParseUtterance = { viewModel.parseVoiceEntry(it) },
                            onPlayBriefingTts = { viewModel.playFinancialBriefingTts() },
                            onStopTts = { viewModel.stopTtsPlayback() },
                            onSaveParsedTransaction = { title, amount, type, category, accountId ->
                                viewModel.addTransaction(
                                    title = title,
                                    amount = amount,
                                    type = type,
                                    category = category,
                                    accountId = accountId
                                )
                            },
                            onSaveParsedTransactionWithSubcategory = { title, amount, type, category, subcat, accountId, note ->
                                viewModel.addTransaction(
                                    title = title,
                                    amount = amount,
                                    type = type,
                                    category = category,
                                    accountId = accountId,
                                    subcategory = subcat,
                                    note = note
                                )
                            },
                            onDismiss = { showVoiceEntrySheet = false },
                            defaultCurrencyCode = uiState.prefs.secondaryCurrencyCode.ifBlank {
                                uiState.prefs.mainCurrencyCode
                            },
                            onExecuteTransfer = { fromId, toId, amount, note ->
                                viewModel.transferFunds(
                                    fromAccountId = fromId,
                                    toAccountId = toId,
                                    amount = amount,
                                    note = note
                                )
                            },
                            managedCategoryOverrides = uiState.prefs.managedCategoryOverrides,
                            deletedCategoryIds = uiState.prefs.deletedCategoryIds,
                            onAddCustomCategory = { item -> viewModel.upsertManagedCategory(item) }
                        )
                    }

                    if (showAddAccountWizard) {
                        AddAccountWizardSheet(
                            language = uiState.prefs.language,
                            strings = strings,
                            onDismiss = { showAddAccountWizard = false },
                            onSaveAccount = { viewModel.addAccount(it) },
                            defaultCurrencyCode = uiState.prefs.mainCurrencyCode,
                            secondaryCurrencyCode = uiState.prefs.secondaryCurrencyCode,
                            userCountryCode = uiState.prefs.userCountryCode,
                            marketSnapshot = marketSyncState.snapshot
                        )
                    }

                    if (showGlobalSearch) {
                        GlobalSearchSheet(
                            accounts = uiState.accounts,
                            transactions = uiState.transactions,
                            language = uiState.prefs.language,
                            strings = strings,
                            onOpenAccountList = { viewModel.openAccountsScreen() },
                            onDismiss = { showGlobalSearch = false }
                        )
                    }

                    if (showNotifications) {
                        NotificationsSheet(
                            alerts = uiState.smartAlerts,
                            strings = strings,
                            language = uiState.prefs.language,
                            onSelectActionRoute = { route ->
                                navigateNotificationRoute(route)
                            },
                            onClearHistory = { viewModel.clearNotificationHistory() },
                            onDismiss = { showNotifications = false }
                        )
                    }

                    if (showProfileBudgetDialog) {
                        EditProfileBudgetDialog(
                            currentName = uiState.prefs.userName.ifBlank { strings.profileDetails.guestUserFallback },
                            currentBudget = uiState.prefs.monthlyBudget,
                            strings = strings,
                            onDismiss = { showProfileBudgetDialog = false },
                            onSave = { name, budget ->
                                viewModel.updateProfileAndBudget(name, budget)
                            }
                        )
                    }
                }
            }
        }
        }
    }
}

private tailrec fun Context.findHostActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findHostActivity()
    else -> null
}
