package com.example.ui.viewmodel

import android.app.Application
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.DeviceTimeContext
import com.example.core.GoogleAccountProfile
import com.example.core.GoogleAuthManager
import com.example.core.NetworkConnectivityMonitor
import com.example.core.SystemClockMonitor
import com.example.data.ai.GeminiAiService
import com.example.data.ai.InsightsSummaryHelper
import com.example.data.ai.LocalReceiptTextParser
import com.example.data.ai.LocalVoiceTransactionParser
import com.example.data.ai.MarketErrorCategory
import com.example.data.ai.ReceiptScanErrorCategory
import com.example.data.ai.SpeechEngineSource
import com.example.data.ai.TtsAudioPlayer
import com.example.data.ai.VoiceSummaryPlaybackListener
import com.example.data.ai.VoiceSummarySpeechEngine
import com.example.data.backup.BackupManager
import com.example.data.backup.DriveAuthErrorKind
import com.example.data.backup.DriveAuthOutcome
import com.example.data.backup.DriveAutoRestoreOutcome
import com.example.data.backup.DriveConsentExtraction
import com.example.data.backup.DriveSyncCoordinator
import com.example.data.backup.DriveSyncStatus
import com.example.data.backup.DriveSyncUiState
import com.example.data.backup.DriveUploadOutcome
import com.example.data.backup.GoogleDriveAuthHelper
import com.example.data.backup.GoogleDriveBackupService
import com.example.data.firebase.FirestoreFinanceRepository
import com.example.data.local.AccountEntity
import com.example.data.local.AppDatabase
import com.example.data.local.CategoryBudgetEntity
import com.example.data.local.LoanEntity
import com.example.data.local.SavingsGoalEntity
import com.example.data.local.TransactionEntity
import com.example.data.local.UserPreferencesRepository
import com.example.data.local.UserPrefs
import com.example.data.market.MarketDataRepository
import com.example.data.market.MarketSnapshot
import com.example.data.market.MarketSyncUiState
import com.example.data.model.AccountType
import com.example.data.model.AppColourTheme
import com.example.data.model.AppCurrency
import com.example.data.model.AppTab
import com.example.data.model.AppThemeMode
import com.example.data.model.AuthMode
import com.example.data.model.CategorySortMode
import com.example.data.model.CategorySpend
import com.example.data.model.DailyTrendPoint
import com.example.data.model.ExpenseCategory
import com.example.data.model.HomeLayoutStyle
import com.example.data.model.HomeSectionId
import com.example.data.model.InsightTimeFilter
import com.example.data.model.LoanType
import com.example.data.model.ManagedCategoryItem
import com.example.data.model.NetWorthSummary
import com.example.data.model.ReceiptScanResult
import com.example.data.model.SearchGroundingResult
import com.example.data.model.SmartAlert
import com.example.data.model.StartOfWeek
import com.example.data.model.TransactionType
import com.example.data.model.VoiceParsedTransaction
import com.example.data.repository.FinanceRepository
import com.example.data.support.CustomerSupportEngine
import com.example.data.support.SUPPORT_GREETING_REPLY_BN
import com.example.data.support.SUPPORT_INITIAL_GREETING_BN
import com.example.data.support.SUPPORT_OFFLINE_AI_NOTICE_BN
import com.example.data.support.SUPPORT_OFFLINE_GUIDE_FOOTER_BN
import com.example.data.support.SupportCategoryId
import com.example.data.support.SupportChatMessage
import com.example.data.support.SupportQuickOption
import com.example.data.support.SupportSender
import com.example.data.support.SupportUiState
import com.example.localization.AppLanguage
import com.example.localization.BdtFormatter
import com.example.notifications.AmarHisabNotificationManager
import com.example.notifications.NotificationActionRoute
import com.example.notifications.NotificationCategory
import com.example.notifications.NotificationLogItem
import com.example.notifications.NotificationTimeCalculator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

private const val TAG = "MainViewModel"

data class AnalyticsSnapshot(
    val periodLabel: String = "",
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val netFlow: Double = 0.0,
    val dailyAverage: Double = 0.0,
    val peakAmount: Double = 0.0,
    val trendPoints: List<DailyTrendPoint> = emptyList(),
    val categoryBreakdown: List<CategorySpend> = emptyList(),
    val previousPeriodExpense: Double = 0.0,
    val previousPeriodIncome: Double = 0.0,
    val expenseChangePercent: Int? = null,
    val savingsRatePercent: Int? = null,
    val topCategory: CategorySpend? = null,
    val transactionCount: Int = 0,
    val incomeBreakdown: List<CategorySpend> = emptyList()
)

data class MainUiState(
    val isPrefsLoaded: Boolean = false,
    val prefs: UserPrefs = UserPrefs(),
    val accounts: List<AccountEntity> = emptyList(),
    val transactions: List<TransactionEntity> = emptyList(),
    val goals: List<SavingsGoalEntity> = emptyList(),
    val loans: List<LoanEntity> = emptyList(),
    val categoryBudgets: List<CategoryBudgetEntity> = emptyList(),
    val totalLiquidBalance: Double = 0.0,
    val liquidBalancesByCurrency: Map<String, Double> = emptyMap(),
    val netWorth: NetWorthSummary = NetWorthSummary(),
    val spentThisMonth: Double = 0.0,
    val incomeThisMonth: Double = 0.0,
    val dailyAverageSpend: Double = 0.0,
    val todaySpend: Double = 0.0,
    val streakDays: Int = 0,
    val topCategories: List<CategorySpend> = emptyList(),
    val categorySpentMap: Map<ExpenseCategory, Double> = emptyMap(),
    val smartAlerts: List<SmartAlert> = emptyList(),
    val selectedTab: AppTab = AppTab.HOME,
    val showAccountsScreen: Boolean = false,
    val showSettingsScreen: Boolean = false,
    val showLiveMarketScreen: Boolean = false,
    val insightFilter: InsightTimeFilter = InsightTimeFilter.THIS_MONTH,
    val insightMonthOffset: Int = 0,
    val analytics: AnalyticsSnapshot = AnalyticsSnapshot(),
    val deviceTimeContext: DeviceTimeContext = DeviceTimeContext.current()
)

sealed interface ReceiptScanUiState {
    data object Idle : ReceiptScanUiState
    data object Loading : ReceiptScanUiState
    data class Success(val result: ReceiptScanResult) : ReceiptScanUiState
    data class Error(
        val message: String,
        val guidance: String = "",
        val category: ReceiptScanErrorCategory = ReceiptScanErrorCategory.GENERAL
    ) : ReceiptScanUiState
}

sealed interface SearchAiUiState {
    data object Idle : SearchAiUiState
    data object Loading : SearchAiUiState
    data class Success(val result: SearchGroundingResult) : SearchAiUiState
    data class Error(
        val message: String,
        val category: MarketErrorCategory = MarketErrorCategory.GENERAL,
        val failedQuery: String = ""
    ) : SearchAiUiState
}

sealed interface VoiceParseUiState {
    data object Idle : VoiceParseUiState
    data object Loading : VoiceParseUiState
    data class Success(val result: VoiceParsedTransaction) : VoiceParseUiState
    data class Error(val message: String) : VoiceParseUiState
}

sealed interface TtsUiState {
    data object Idle : TtsUiState
    data object Generating : TtsUiState
    data class Playing(
        val script: String,
        val progress: Float = 0f,
        val currentChunkIndex: Int = 0,
        val totalChunks: Int = 1,
        val engineLabel: String = "",
        val voiceLocaleLabel: String = ""
    ) : TtsUiState
    data class Paused(
        val script: String,
        val progress: Float = 0f,
        val currentChunkIndex: Int = 0,
        val totalChunks: Int = 1,
        val engineLabel: String = "",
        val voiceLocaleLabel: String = ""
    ) : TtsUiState
    data class Completed(
        val script: String,
        val totalChunks: Int = 1,
        val engineLabel: String = "",
        val voiceLocaleLabel: String = ""
    ) : TtsUiState
    data class Error(
        val message: String,
        val category: MarketErrorCategory = MarketErrorCategory.GENERAL,
        val script: String = ""
    ) : TtsUiState
}

data class BackupMessage(val text: String, val isError: Boolean = false)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    private val repository = FinanceRepository(db)
    private val prefsRepo = UserPreferencesRepository(application)
    private val aiService = GeminiAiService()
    private val ttsPlayer = TtsAudioPlayer(application)
    private val voiceSummaryEngine = VoiceSummarySpeechEngine.createDefault(application, aiService, ttsPlayer)
    private val connectivityMonitor = NetworkConnectivityMonitor(application)
    private val systemClockMonitor = SystemClockMonitor(application)
    private val marketRepository = MarketDataRepository(application)
    private val driveSyncCoordinator = DriveSyncCoordinator()
    private val firestoreRepo = FirestoreFinanceRepository(application)
    private var firestoreSyncDebounceJob: Job? = null
    @Volatile
    private var hasPendingFirestoreSync: Boolean = false

    private val _selectedTab = MutableStateFlow(AppTab.HOME)
    private val _showAccountsScreen = MutableStateFlow(false)
    private val _showSettingsScreen = MutableStateFlow(false)
    private val _showLiveMarketScreen = MutableStateFlow(false)
    private val _insightFilter = MutableStateFlow(InsightTimeFilter.THIS_MONTH)
    private val _insightMonthOffset = MutableStateFlow(0)

    private val _isAppUnlocked = MutableStateFlow(false)
    val isAppUnlocked: StateFlow<Boolean> = _isAppUnlocked.asStateFlow()

    private var lastBackgroundedAtMillis: Long = 0L

    private val _backupStatus = MutableStateFlow<BackupMessage?>(null)
    val backupStatus: StateFlow<BackupMessage?> = _backupStatus.asStateFlow()

    private enum class DrivePendingAction {
        AUTO_RESTORE_OR_SYNC,
        FORCE_RESTORE,
        SYNC_NOW
    }

    private val _driveSyncState = MutableStateFlow(DriveSyncUiState())
    val driveSyncState: StateFlow<DriveSyncUiState> = _driveSyncState.asStateFlow()

    private val _pendingDriveConsent = MutableStateFlow<PendingIntent?>(null)
    val pendingDriveConsent: StateFlow<PendingIntent?> = _pendingDriveConsent.asStateFlow()

    private var cachedDriveAccessToken: String? = null
    private var pendingDriveConsentAction: DrivePendingAction = DrivePendingAction.AUTO_RESTORE_OR_SYNC
    @Volatile
    private var isDriveRestoreInProgress: Boolean = false
    @Volatile
    private var isDriveSyncRunning: Boolean = false
    @Volatile
    private var driveSyncRequestedWhileRunning: Boolean = false
    private var driveAutoSyncDebounceJob: Job? = null
    private var lastObservedDbSignature: Int? = null
    private var lastObservedPrefSignature: Int? = null
    private var hasCheckedStartupDriveSync: Boolean = false

    private val _receiptState = MutableStateFlow<ReceiptScanUiState>(ReceiptScanUiState.Idle)
    val receiptState: StateFlow<ReceiptScanUiState> = _receiptState.asStateFlow()

    private val _searchAiState = MutableStateFlow<SearchAiUiState>(SearchAiUiState.Idle)
    val searchAiState: StateFlow<SearchAiUiState> = _searchAiState.asStateFlow()

    private val _voiceState = MutableStateFlow<VoiceParseUiState>(VoiceParseUiState.Idle)
    val voiceState: StateFlow<VoiceParseUiState> = _voiceState.asStateFlow()

    private val _ttsState = MutableStateFlow<TtsUiState>(TtsUiState.Idle)
    val ttsState: StateFlow<TtsUiState> = _ttsState.asStateFlow()

    private val _isMarketSyncing = MutableStateFlow(false)
    private val _lastMarketSyncFailed = MutableStateFlow(false)
    private val _lastMarketSyncAttemptedMillis = MutableStateFlow(0L)
    val marketSyncState: StateFlow<MarketSyncUiState> = combine(
        marketRepository.snapshotFlow,
        connectivityMonitor.isOnline,
        _isMarketSyncing,
        _lastMarketSyncFailed,
        _lastMarketSyncAttemptedMillis
    ) { snapshot, isOnline, isSyncing, lastSyncFailed, lastAttemptedMillis ->
        MarketSyncUiState(
            snapshot = snapshot,
            isOnline = isOnline,
            isSyncing = isSyncing,
            lastSyncFailed = lastSyncFailed && isOnline,
            lastAttemptedMillis = lastAttemptedMillis
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MarketSyncUiState(isOnline = connectivityMonitor.isOnline.value)
    )

    private var supportMessageCounter = 0L
    private val _supportState = MutableStateFlow(
        SupportUiState(isOnline = connectivityMonitor.isOnline.value)
    )
    val supportUiState: StateFlow<SupportUiState> = combine(
        _supportState,
        connectivityMonitor.isOnline
    ) { state, online ->
        state.copy(isOnline = online)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SupportUiState(isOnline = connectivityMonitor.isOnline.value)
    )

    private data class PendingDashboardLayout(
        val layoutStyle: HomeLayoutStyle,
        val order: List<HomeSectionId>,
        val hidden: Set<HomeSectionId>
    )

    private val _pendingDashboardLayout = MutableStateFlow<PendingDashboardLayout?>(null)

    private val screenNavFlow = combine(
        _selectedTab,
        _showAccountsScreen,
        _showSettingsScreen,
        _showLiveMarketScreen
    ) { tab, showAccounts, showSettings, showLiveMarket ->
        ScreenNavState(tab, showAccounts, showSettings, showLiveMarket)
    }

    private val navAndFilterFlow = combine(
        screenNavFlow,
        _insightFilter,
        _insightMonthOffset
    ) { screenNav, filter, monthOffset ->
        NavFilterState(
            tab = screenNav.tab,
            showAccounts = screenNav.showAccounts,
            showSettings = screenNav.showSettings,
            showLiveMarket = screenNav.showLiveMarket,
            filter = filter,
            monthOffset = monthOffset
        )
    }

    private val dbFlow = combine(
        repository.accounts,
        repository.transactions,
        repository.goals,
        repository.loans,
        repository.categoryBudgets
    ) { accounts, transactions, goals, loans, categoryBudgets ->
        DbSnapshot(accounts, transactions, goals, loans, categoryBudgets)
    }

    private val timeAndMarketFlow = combine(
        systemClockMonitor.timeContext,
        marketRepository.snapshotFlow
    ) { timeCtx, snapshot -> timeCtx to snapshot }

    val uiState: StateFlow<MainUiState> = combine(
        prefsRepo.userPrefs,
        _pendingDashboardLayout,
        dbFlow,
        navAndFilterFlow,
        timeAndMarketFlow
    ) { prefs, pendingLayout, dbData, nav, (timeCtx, marketSnapshot) ->
        val effectivePrefs = if (pendingLayout != null) {
            prefs.copy(
                homeLayoutStyle = pendingLayout.layoutStyle,
                homeSectionOrder = pendingLayout.order,
                hiddenHomeSections = pendingLayout.hidden
            )
        } else {
            prefs
        }
        buildMainUiState(effectivePrefs, dbData, nav, timeCtx, marketSnapshot)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MainUiState()
    )

    init {
        runCatching { connectivityMonitor.start() }
            .onFailure { Log.w(TAG, "Connectivity monitor failed to start", it) }
        runCatching { systemClockMonitor.start() }
            .onFailure { Log.w(TAG, "System clock monitor failed to start", it) }
        runCatching { AmarHisabNotificationManager.ensureChannels(application) }
            .onFailure { Log.w(TAG, "Notification channels init skipped", it) }
        // Automatically sync live exchange rates & XAU gold spot on launch
        // and whenever internet connectivity is restored (15-min freshness window).
        viewModelScope.launch {
            connectivityMonitor.isOnline.collect { online ->
                if (online) {
                    val cached = marketRepository.getCachedSnapshot()
                    if (cached.isFreshSyncNeeded()) {
                        refreshMarketRates(force = false)
                    }
                    if (hasPendingFirestoreSync && firestoreRepo.currentAuthenticatedUid() != null) {
                        scheduleCentralFirestoreSync(debounceMillis = 600L)
                    }
                }
            }
        }
        // Automatically schedule/reschedule local-timezone reminders whenever notification settings,
        // loans, or the device's timezone change.
        viewModelScope.launch {
            combine(
                prefsRepo.userPrefs,
                repository.loans,
                systemClockMonitor.timeContext
            ) { prefs, loans, timeCtx ->
                Triple(prefs, loans, timeCtx.timeZoneId)
            }
                .distinctUntilChanged { old, new ->
                    old.third == new.third &&
                        old.second == new.second &&
                        old.first.language == new.first.language &&
                        old.first.notifDailyReminderEnabled == new.first.notifDailyReminderEnabled &&
                        old.first.notifDailyReminderHour == new.first.notifDailyReminderHour &&
                        old.first.notifDailyReminderMinute == new.first.notifDailyReminderMinute &&
                        old.first.notifLoanReminders == new.first.notifLoanReminders &&
                        old.first.notifLoanDayBeforeEnabled == new.first.notifLoanDayBeforeEnabled
                }
                .collect { (prefs, loans, tzId) ->
                    runCatching {
                        AmarHisabNotificationManager.scheduleAllReminders(
                            context = getApplication(),
                            prefs = prefs,
                            loans = loans,
                            nowMillis = System.currentTimeMillis(),
                            timeZone = TimeZone.getTimeZone(tzId)
                        )
                    }.onFailure { e ->
                        Log.w(TAG, "Reminder scheduling skipped", e)
                    }
                }
        }
    }

    fun refreshSystemTime(
        nowMillis: Long = System.currentTimeMillis(),
        timeZone: TimeZone = TimeZone.getDefault()
    ) {
        systemClockMonitor.refreshNow(nowMillis, timeZone)
    }

    fun onAppResumed() {
        refreshSystemTime()
        refreshMarketRates(force = false)
    }

    fun refreshMarketRates(force: Boolean = true) {
        if (_isMarketSyncing.value) return
        val online = connectivityMonitor.refreshNow()
        if (!online) return
        viewModelScope.launch {
            if (!force) {
                val cached = marketRepository.getCachedSnapshot()
                if (!cached.isFreshSyncNeeded()) return@launch
            }
            _isMarketSyncing.value = true
            _lastMarketSyncAttemptedMillis.value = System.currentTimeMillis()
            try {
                val updated = marketRepository.syncLiveMarketRates()
                _lastMarketSyncFailed.value = (updated == null)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _lastMarketSyncFailed.value = true
                Log.w(TAG, "Market rate auto-sync fell back to cache", e)
            } finally {
                _isMarketSyncing.value = false
            }
        }
    }

    fun selectTab(tab: AppTab) {
        _showAccountsScreen.value = false
        _showSettingsScreen.value = false
        _showLiveMarketScreen.value = false
        _selectedTab.value = tab
    }

    fun openAccountsScreen() {
        _showSettingsScreen.value = false
        _showLiveMarketScreen.value = false
        _showAccountsScreen.value = true
    }

    fun closeAccountsScreen() {
        _showAccountsScreen.value = false
    }

    fun openSettingsScreen() {
        _showAccountsScreen.value = false
        _showLiveMarketScreen.value = false
        _showSettingsScreen.value = true
    }

    fun closeSettingsScreen() {
        _showSettingsScreen.value = false
    }

    fun openLiveMarketScreen() {
        _showAccountsScreen.value = false
        _showSettingsScreen.value = false
        _showLiveMarketScreen.value = true
        refreshMarketRates(force = false)
    }

    fun closeLiveMarketScreen() {
        _showLiveMarketScreen.value = false
    }

    fun setInsightFilter(filter: InsightTimeFilter) {
        _insightFilter.value = filter
        if (filter == InsightTimeFilter.THIS_MONTH) _insightMonthOffset.value = 0
        if (filter == InsightTimeFilter.LAST_MONTH) _insightMonthOffset.value = -1
    }

    fun shiftInsightMonth(delta: Int) {
        _insightFilter.value = InsightTimeFilter.THIS_MONTH
        _insightMonthOffset.value = (_insightMonthOffset.value + delta).coerceIn(-12, 0)
    }

    fun unlockApp() {
        _isAppUnlocked.value = true
    }

    fun markLaunchPermissionsRequested() = viewModelScope.launch {
        prefsRepo.setLaunchPermissionsRequested(true)
    }

    fun lockAppNow() {
        _isAppUnlocked.value = false
    }

    fun toggleLanguage() = viewModelScope.launch {
        prefsRepo.toggleLanguage(uiState.value.prefs.language)
    }

    fun setLanguage(language: AppLanguage) = viewModelScope.launch {
        prefsRepo.setLanguage(language)
    }

    fun toggleDarkTheme(currentResolvedDark: Boolean = uiState.value.prefs.isDarkTheme) = viewModelScope.launch {
        prefsRepo.toggleDarkTheme(currentResolvedDark)
    }

    fun setDarkTheme(isDark: Boolean) = viewModelScope.launch {
        prefsRepo.setDarkTheme(isDark)
    }

    fun setThemeMode(mode: AppThemeMode) = viewModelScope.launch {
        prefsRepo.setThemeMode(mode)
    }

    fun setColourTheme(theme: AppColourTheme) = viewModelScope.launch {
        prefsRepo.setColourTheme(theme)
    }

    fun updateProfileAndBudget(name: String, budget: Double) = viewModelScope.launch {
        prefsRepo.updateProfileAndBudget(name, budget)
        scheduleCentralFirestoreSync()
    }

    fun updateDisplayName(name: String) = viewModelScope.launch {
        prefsRepo.updateDisplayName(name)
        scheduleCentralFirestoreSync()
    }

    fun updateProfileAvatarFromUri(uri: Uri) = viewModelScope.launch {
        val currentMode = uiState.value.prefs.authMode
        val prefix = if (currentMode == AuthMode.GOOGLE) "google_avatar_" else "guest_avatar_"
        val savedUri = withContext(Dispatchers.IO) {
            try {
                val app = getApplication<Application>()
                val avatarDir = File(app.filesDir, "avatars").apply { mkdirs() }
                avatarDir.listFiles()?.forEach { existing ->
                    if (existing.isFile && existing.name.startsWith(prefix)) {
                        existing.delete()
                    }
                }
                val destFile = File(avatarDir, "${prefix}${System.currentTimeMillis()}.jpg")
                app.contentResolver.openInputStream(uri)?.use { input ->
                    destFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                } ?: return@withContext null
                if (destFile.length() > 0L) Uri.fromFile(destFile).toString() else null
            } catch (e: Exception) {
                Log.w(TAG, "Failed to save profile avatar locally", e)
                null
            }
        }
        if (savedUri != null) {
            prefsRepo.setCustomAvatarForMode(savedUri, currentMode)
        }
    }

    fun selectPresetAvatar(presetId: String) = viewModelScope.launch {
        val currentMode = uiState.value.prefs.authMode
        prefsRepo.setCustomAvatarForMode(presetId, currentMode)
    }

    fun resetProfileAvatar() = viewModelScope.launch {
        val currentMode = uiState.value.prefs.authMode
        val prefix = if (currentMode == AuthMode.GOOGLE) "google_avatar_" else "guest_avatar_"
        withContext(Dispatchers.IO) {
            try {
                val avatarDir = File(getApplication<Application>().filesDir, "avatars")
                avatarDir.listFiles()?.forEach { existing ->
                    if (existing.isFile && existing.name.startsWith(prefix)) {
                        existing.delete()
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to delete profile avatar files", e)
            }
        }
        prefsRepo.setCustomAvatarForMode("", currentMode)
    }

    fun updateGuestAvatarFromUri(uri: Uri) = updateProfileAvatarFromUri(uri)

    fun clearGuestAvatar() = resetProfileAvatar()

    fun continueAsGuest(language: AppLanguage) = viewModelScope.launch {
        prefsRepo.completeWelcomeAsGuest(null)
    }

    fun completeLocalProfileSetup(displayName: String) = viewModelScope.launch {
        _backupStatus.value = null
        prefsRepo.completeWelcomeAsGuest(displayName.trim().ifBlank { null })
    }

    fun linkGoogleProfile(profile: GoogleAccountProfile) = viewModelScope.launch {
        driveAutoSyncDebounceJob?.cancel()
        driveAutoSyncDebounceJob = null
        cachedDriveAccessToken = null
        _pendingDriveConsent.value = null

        val currentPrefs = prefsRepo.userPrefs.first()
        val incomingEmail = profile.email.trim().lowercase()
        val previousOwner = currentPrefs.lastLocalOwnerEmail
            .ifBlank { currentPrefs.userEmail }
            .trim()
            .lowercase()
        val isSwitchingToDifferentUser = previousOwner.isNotBlank() &&
            incomingEmail.isNotBlank() &&
            previousOwner != incomingEmail

        if (isSwitchingToDifferentUser) {
            // Strictly isolate each Gmail user's local workspace so User B never sees or uploads User A's records
            isDriveRestoreInProgress = true
            try {
                BackupManager.clearAllFinancialData(db)
                prefsRepo.resetUserScopedPrefsForAccountSwitch()
                lastObservedDbSignature = null
                lastObservedPrefSignature = null
                _pendingDashboardLayout.value = null
            } finally {
                isDriveRestoreInProgress = false
            }
        }

        prefsRepo.linkGoogleAccount(
            displayName = profile.displayName,
            email = profile.email,
            avatarUrl = profile.avatarUrl
        )
        hasCheckedStartupDriveSync = true
        // Connect central data store to Firestore in the background (non-blocking).
        // Hydrates cloud cards/entries onto an empty device or mirrors local entries up cleanly.
        viewModelScope.launch(Dispatchers.IO) {
            val latestPrefs = prefsRepo.userPrefs.first()
            firestoreRepo.reconcileOnSignIn(
                roomDb = db,
                prefsRepo = prefsRepo,
                currentPrefs = latestPrefs
            ).onFailure { err ->
                Log.w(TAG, "Central Firestore reconcile deferred (local store stays active)", err)
                hasPendingFirestoreSync = true
            }
        }
    }

    fun unlinkGoogleAccount() = viewModelScope.launch {
        driveAutoSyncDebounceJob?.cancel()
        driveAutoSyncDebounceJob = null
        firestoreSyncDebounceJob?.cancel()
        firestoreSyncDebounceJob = null
        hasPendingFirestoreSync = false
        cachedDriveAccessToken = null
        _pendingDriveConsent.value = null
        hasCheckedStartupDriveSync = false
        // Detach Firestore listeners inside NonCancellable BEFORE signing out of Firebase Auth
        firestoreRepo.detachAllListeners()
        GoogleAuthManager.signOut(getApplication())
        prefsRepo.unlinkGoogleAccount()
        _driveSyncState.value = DriveSyncUiState(status = DriveSyncStatus.SIGNED_OUT)
    }

    /**
     * Cleanly logs out of the active session and navigates back to the startup welcome screen
     * without clearing any local financial records (accounts, transactions, goals, or loans).
     */
    fun logOut() = viewModelScope.launch {
        driveAutoSyncDebounceJob?.cancel()
        driveAutoSyncDebounceJob = null
        firestoreSyncDebounceJob?.cancel()
        firestoreSyncDebounceJob = null
        hasPendingFirestoreSync = false
        cachedDriveAccessToken = null
        _pendingDriveConsent.value = null
        hasCheckedStartupDriveSync = false
        firestoreRepo.detachAllListeners()
        runCatching { GoogleAuthManager.signOut(getApplication()) }
        prefsRepo.logOut()
        _driveSyncState.value = DriveSyncUiState(status = DriveSyncStatus.SIGNED_OUT)
        _showSettingsScreen.value = false
        _showAccountsScreen.value = false
    }

    /**
     * Non-blocking central data store sync: persists accounts/cards, transactions (including
     * Voice Entry & Receipt Scan entries), goals, loans, budgets, and profile prefs to Firebase
     * Firestore in the background whenever signed in with Google.
     */
    private fun scheduleCentralFirestoreSync(debounceMillis: Long = 800L) {
        if (firestoreRepo.currentAuthenticatedUid() == null) return
        hasPendingFirestoreSync = true
        firestoreSyncDebounceJob?.cancel()
        firestoreSyncDebounceJob = viewModelScope.launch(Dispatchers.IO) {
            delay(debounceMillis)
            if (firestoreRepo.currentAuthenticatedUid() == null) {
                hasPendingFirestoreSync = false
                return@launch
            }
            if (!connectivityMonitor.refreshNow()) return@launch
            val latestPrefs = prefsRepo.userPrefs.first()
            firestoreRepo.pushCentralStoreSnapshot(db, latestPrefs)
                .onSuccess { hasPendingFirestoreSync = false }
                .onFailure { err ->
                    Log.w(TAG, "Background Firestore sync deferred; local data remains intact", err)
                }
        }
    }

    // --- Encrypted Google Drive Backup, Immediate Auto-Sync & Login Auto-Restore ---

    fun consumePendingDriveConsent() {
        _pendingDriveConsent.value = null
    }

    fun onDriveConsentResult(resultOk: Boolean, data: Intent?) {
        _pendingDriveConsent.value = null
        val isBn = uiState.value.prefs.language == AppLanguage.BN
        when (val extracted = GoogleDriveAuthHelper.extractConsentResult(getApplication(), resultOk, data)) {
            is DriveConsentExtraction.Token -> {
                cachedDriveAccessToken = extracted.accessToken
                when (pendingDriveConsentAction) {
                    DrivePendingAction.AUTO_RESTORE_OR_SYNC ->
                        initiateDriveAutoRestoreOrSync(forceRestore = false, allowConsentPrompt = false)
                    DrivePendingAction.FORCE_RESTORE ->
                        initiateDriveAutoRestoreOrSync(forceRestore = true, allowConsentPrompt = false)
                    DrivePendingAction.SYNC_NOW ->
                        syncToGoogleDriveNow(allowConsentPrompt = false)
                }
            }

            is DriveConsentExtraction.Declined -> {
                _driveSyncState.value = _driveSyncState.value.copy(
                    status = DriveSyncStatus.NEEDS_PERMISSION,
                    message = if (isBn) {
                        "Google Drive অনুমতি দেওয়া হয়নি। আপনার সব হিসাব লোকালি নিরাপদ আছে—যেকোনো সময় অনুমতি দিয়ে অটো-ব্যাকআপ চালু করতে পারবেন।"
                    } else {
                        "Google Drive access was declined. Your local accounts and transactions remain safe—you can grant permission anytime."
                    },
                    isError = false
                )
            }

            is DriveConsentExtraction.Restricted403 -> {
                _driveSyncState.value = _driveSyncState.value.copy(
                    status = DriveSyncStatus.RESTRICTED_403,
                    message = driveRestricted403Message(isBn),
                    isError = true
                )
            }

            is DriveConsentExtraction.Failed -> {
                _driveSyncState.value = _driveSyncState.value.copy(
                    status = DriveSyncStatus.NEEDS_PERMISSION,
                    message = if (isBn) {
                        "Google Drive টোকেন যাচাই করা যায়নি। লোকাল অ্যাপ স্বাভাবিকভাবে চলছে—আবার চেষ্টা করুন।"
                    } else {
                        "Could not verify Google Drive permission. Local tracking continues normally—please try again."
                    },
                    isError = true
                )
            }
        }
    }

    fun syncToGoogleDriveNow(allowConsentPrompt: Boolean = true) {
        viewModelScope.launch {
            executeEncryptedDriveUpload(allowConsentPrompt = allowConsentPrompt)
        }
    }

    fun restoreFromGoogleDriveNow() {
        initiateDriveAutoRestoreOrSync(forceRestore = true, allowConsentPrompt = true)
    }

    fun requestDrivePermissionAndSync() {
        initiateDriveAutoRestoreOrSync(forceRestore = false, allowConsentPrompt = true)
    }

    private fun onLocalFinancialDataMutated() {
        viewModelScope.launch {
            val prefs = prefsRepo.userPrefs.first()
            prefsRepo.markDriveSyncPending(System.currentTimeMillis())
            if (prefs.authMode == AuthMode.GOOGLE && prefs.userEmail.isNotBlank()) {
                if (connectivityMonitor.refreshNow()) {
                    scheduleImmediateDriveAutoSync(debounceMillis = 1500L)
                } else {
                    val isBn = prefs.language == AppLanguage.BN
                    _driveSyncState.value = _driveSyncState.value.copy(
                        status = DriveSyncStatus.PENDING_OFFLINE,
                        hasPendingChanges = true,
                        message = if (isBn) {
                            "অফলাইন — ইন্টারনেট এলেই Google Drive-এ এনক্রিপ্টেড সিঙ্ক হবে।"
                        } else {
                            "Offline — encrypted Google Drive sync queued until online."
                        },
                        isError = false
                    )
                }
            }
        }
    }

    private fun scheduleImmediateDriveAutoSync(debounceMillis: Long = 1500L) {
        if (isDriveSyncRunning) {
            driveSyncRequestedWhileRunning = true
            return
        }
        driveAutoSyncDebounceJob?.cancel()
        driveAutoSyncDebounceJob = viewModelScope.launch {
            delay(debounceMillis)
            executeEncryptedDriveUpload(allowConsentPrompt = false)
        }
    }

    private fun flushPendingDriveSyncIfSignedIn(allowConsentPrompt: Boolean) {
        viewModelScope.launch {
            val prefs = prefsRepo.userPrefs.first()
            if (prefs.authMode == AuthMode.GOOGLE &&
                prefs.userEmail.isNotBlank() &&
                (prefs.drivePendingSync || !prefs.driveFolderLinked)
            ) {
                executeEncryptedDriveUpload(allowConsentPrompt = allowConsentPrompt)
            }
        }
    }

    private fun initiateDriveAutoRestoreOrSync(
        forceRestore: Boolean,
        allowConsentPrompt: Boolean
    ) {
        viewModelScope.launch {
            performDriveAutoRestoreOrSync(
                forceRestore = forceRestore,
                allowConsentPrompt = allowConsentPrompt
            )
        }
    }

    private suspend fun performDriveAutoRestoreOrSync(
        forceRestore: Boolean,
        allowConsentPrompt: Boolean
    ) {
        val prefs = prefsRepo.userPrefs.first()
        val isBn = prefs.language == AppLanguage.BN
        if (prefs.authMode != AuthMode.GOOGLE || prefs.userEmail.isBlank()) {
            _driveSyncState.value = DriveSyncUiState(status = DriveSyncStatus.SIGNED_OUT)
            return
        }
        if (!connectivityMonitor.refreshNow()) {
            _driveSyncState.value = _driveSyncState.value.copy(
                status = DriveSyncStatus.PENDING_OFFLINE,
                message = if (isBn) {
                    "ইন্টারনেট সংযোগ নেই — অফলাইনে স্বাভাবিকভাবে হিসাব রাখুন, অনলাইন হলে অটো-রিস্টোর/সিঙ্ক হবে।"
                } else {
                    "Offline — local tracking works normally; Drive restore & encrypted sync will run once online."
                },
                isError = false
            )
            return
        }

        val token = obtainDriveAccessToken(
            userEmail = prefs.userEmail,
            actionOnConsent = if (forceRestore) {
                DrivePendingAction.FORCE_RESTORE
            } else {
                DrivePendingAction.AUTO_RESTORE_OR_SYNC
            },
            allowConsentPrompt = allowConsentPrompt
        ) ?: return

        _driveSyncState.value = _driveSyncState.value.copy(
            status = DriveSyncStatus.RESTORING,
            message = if (isBn) {
                "Google Drive-এ এনক্রিপ্টেড ব্যাকআপ যাচাই করা হচ্ছে..."
            } else {
                "Checking encrypted Google Drive vault..."
            },
            isError = false
        )

        isDriveRestoreInProgress = true
        try {
            when (
                val outcome = driveSyncCoordinator.autoRestoreOrInitialSync(
                    accessToken = token,
                    userEmail = prefs.userEmail,
                    db = db,
                    prefsRepo = prefsRepo,
                    forceRestore = forceRestore
                )
            ) {
                is DriveAutoRestoreOutcome.Restored -> {
                    _pendingDashboardLayout.value = null
                    val msg = if (isBn) {
                        "Google Drive থেকে এনক্রিপ্টেড ব্যাকআপ স্বয়ংক্রিয়ভাবে রিস্টোর হয়েছে।"
                    } else {
                        "Encrypted backup restored automatically from Google Drive."
                    }
                    _driveSyncState.value = DriveSyncUiState(
                        status = DriveSyncStatus.SYNCED,
                        lastSyncedAtMillis = outcome.updatedAtMillis,
                        hasPendingChanges = false,
                        message = msg,
                        isError = false
                    )
                    _backupStatus.value = BackupMessage(msg, isError = false)
                }

                is DriveAutoRestoreOutcome.UploadedLocal -> {
                    val msg = if (isBn) {
                        "Google Drive-এ এনক্রিপ্টেড ব্যাকআপ ফোল্ডার সিঙ্ক হয়েছে।"
                    } else {
                        "Encrypted backup synced to Google Drive."
                    }
                    _driveSyncState.value = DriveSyncUiState(
                        status = DriveSyncStatus.SYNCED,
                        lastSyncedAtMillis = outcome.updatedAtMillis,
                        hasPendingChanges = false,
                        message = msg,
                        isError = false
                    )
                }

                is DriveAutoRestoreOutcome.Unauthorized -> {
                    cachedDriveAccessToken = null
                    if (allowConsentPrompt) {
                        obtainDriveAccessToken(
                            userEmail = prefs.userEmail,
                            actionOnConsent = if (forceRestore) {
                                DrivePendingAction.FORCE_RESTORE
                            } else {
                                DrivePendingAction.AUTO_RESTORE_OR_SYNC
                            },
                            allowConsentPrompt = true
                        )
                    } else {
                        _driveSyncState.value = _driveSyncState.value.copy(
                            status = DriveSyncStatus.NEEDS_PERMISSION,
                            isError = false
                        )
                    }
                }

                is DriveAutoRestoreOutcome.Restricted403 -> {
                    cachedDriveAccessToken = null
                    val msg = driveRestricted403Message(isBn)
                    _driveSyncState.value = _driveSyncState.value.copy(
                        status = DriveSyncStatus.RESTRICTED_403,
                        message = msg,
                        isError = true
                    )
                    if (forceRestore) {
                        _backupStatus.value = BackupMessage(msg, isError = true)
                    }
                }

                is DriveAutoRestoreOutcome.Error -> {
                    val errMsg = if (isBn) {
                        "Google Drive ব্যাকআপ যাচাই করা যায়নি — লোকাল হিসাব নিরাপদ আছে। (${outcome.message})"
                    } else {
                        "Unable to reach Google Drive right now; local records remain safe. (${outcome.message})"
                    }
                    _driveSyncState.value = _driveSyncState.value.copy(
                        status = DriveSyncStatus.ERROR,
                        message = errMsg,
                        isError = true
                    )
                    if (forceRestore) {
                        _backupStatus.value = BackupMessage(errMsg, isError = true)
                    }
                }
            }
        } finally {
            isDriveRestoreInProgress = false
        }
    }

    private suspend fun executeEncryptedDriveUpload(allowConsentPrompt: Boolean) {
        if (isDriveRestoreInProgress) return
        if (isDriveSyncRunning) {
            driveSyncRequestedWhileRunning = true
            return
        }

        val prefs = prefsRepo.userPrefs.first()
        val isBn = prefs.language == AppLanguage.BN
        if (prefs.authMode != AuthMode.GOOGLE || prefs.userEmail.isBlank()) {
            _driveSyncState.value = DriveSyncUiState(status = DriveSyncStatus.SIGNED_OUT)
            return
        }
        if (!connectivityMonitor.refreshNow()) {
            _driveSyncState.value = _driveSyncState.value.copy(
                status = DriveSyncStatus.PENDING_OFFLINE,
                hasPendingChanges = true,
                message = if (isBn) {
                    "অফলাইন — ইন্টারনেট এলেই এনক্রিপ্টেড ব্যাকআপ আপলোড হবে।"
                } else {
                    "Offline — encrypted Drive backup will upload automatically when online."
                },
                isError = false
            )
            return
        }

        val token = obtainDriveAccessToken(
            userEmail = prefs.userEmail,
            actionOnConsent = DrivePendingAction.SYNC_NOW,
            allowConsentPrompt = allowConsentPrompt
        ) ?: return

        isDriveSyncRunning = true
        driveSyncRequestedWhileRunning = false
        _driveSyncState.value = _driveSyncState.value.copy(
            status = DriveSyncStatus.SYNCING,
            message = if (isBn) {
                "এনক্রিপ্টেড ব্যাকআপ Google Drive-এ সিঙ্ক হচ্ছে..."
            } else {
                "Syncing encrypted vault to Google Drive..."
            },
            isError = false
        )

        var shouldFollowUpSync = false
        try {
            when (
                val outcome = driveSyncCoordinator.uploadLocalSnapshot(
                    accessToken = token,
                    userEmail = prefs.userEmail,
                    db = db,
                    prefsRepo = prefsRepo
                )
            ) {
                is DriveUploadOutcome.Success -> {
                    val syncedMsg = if (isBn) {
                        "Google Drive-এ এনক্রিপ্টেড সিঙ্ক সম্পন্ন হয়েছে।"
                    } else {
                        "Encrypted backup synced to Google Drive."
                    }
                    _driveSyncState.value = DriveSyncUiState(
                        status = DriveSyncStatus.SYNCED,
                        lastSyncedAtMillis = outcome.updatedAtMillis,
                        hasPendingChanges = driveSyncRequestedWhileRunning,
                        message = syncedMsg,
                        isError = false
                    )
                    if (allowConsentPrompt) {
                        _backupStatus.value = BackupMessage(syncedMsg, isError = false)
                    }
                }

                is DriveUploadOutcome.Unauthorized -> {
                    cachedDriveAccessToken = null
                    _driveSyncState.value = _driveSyncState.value.copy(
                        status = DriveSyncStatus.NEEDS_PERMISSION,
                        hasPendingChanges = true,
                        message = if (isBn) {
                            "Google Drive অনুমতি পুনরায় নিশ্চিত করুন।"
                        } else {
                            "Please re-confirm Google Drive access to resume encrypted sync."
                        },
                        isError = false
                    )
                    if (allowConsentPrompt) {
                        obtainDriveAccessToken(
                            userEmail = prefs.userEmail,
                            actionOnConsent = DrivePendingAction.SYNC_NOW,
                            allowConsentPrompt = true
                        )
                    }
                }

                is DriveUploadOutcome.Restricted403 -> {
                    cachedDriveAccessToken = null
                    val msg = driveRestricted403Message(isBn)
                    _driveSyncState.value = _driveSyncState.value.copy(
                        status = DriveSyncStatus.RESTRICTED_403,
                        hasPendingChanges = true,
                        message = msg,
                        isError = true
                    )
                    if (allowConsentPrompt) {
                        _backupStatus.value = BackupMessage(msg, isError = true)
                    }
                }

                is DriveUploadOutcome.Error -> {
                    _driveSyncState.value = _driveSyncState.value.copy(
                        status = DriveSyncStatus.ERROR,
                        hasPendingChanges = true,
                        message = if (isBn) {
                            "সিঙ্ক সাময়িকভাবে ব্যর্থ হয়েছে — লোকাল হিসাব নিরাপদ আছে, পরে স্বয়ংক্রিয়ভাবে আবার চেষ্টা হবে।"
                        } else {
                            "Sync paused — local data is safe and will retry automatically."
                        },
                        isError = true
                    )
                }
            }
        } finally {
            shouldFollowUpSync = driveSyncRequestedWhileRunning
            driveSyncRequestedWhileRunning = false
            isDriveSyncRunning = false
        }

        if (shouldFollowUpSync && !isDriveRestoreInProgress) {
            scheduleImmediateDriveAutoSync(debounceMillis = 1200L)
        }
    }

    private suspend fun obtainDriveAccessToken(
        userEmail: String,
        actionOnConsent: DrivePendingAction,
        allowConsentPrompt: Boolean
    ): String? {
        cachedDriveAccessToken?.takeIf { it.isNotBlank() }?.let { return it }

        val isBn = uiState.value.prefs.language == AppLanguage.BN
        return when (
            val auth = GoogleDriveAuthHelper.requestDriveAccessToken(
                context = getApplication(),
                accountEmail = userEmail
            )
        ) {
            is DriveAuthOutcome.Authorized -> {
                cachedDriveAccessToken = auth.accessToken
                auth.accessToken
            }

            is DriveAuthOutcome.NeedsUserConsent -> {
                pendingDriveConsentAction = actionOnConsent
                _driveSyncState.value = _driveSyncState.value.copy(
                    status = DriveSyncStatus.NEEDS_PERMISSION,
                    message = if (isBn) {
                        "Google Drive এনক্রিপ্টেড ব্যাকআপের জন্য অনুমতি প্রয়োজন।"
                    } else {
                        "Google Drive permission required for automatic encrypted sync."
                    },
                    isError = false
                )
                if (allowConsentPrompt) {
                    _pendingDriveConsent.value = auth.pendingIntent
                }
                null
            }

            is DriveAuthOutcome.Error -> {
                when (auth.kind) {
                    DriveAuthErrorKind.OAUTH_RESTRICTED_403 -> {
                        _driveSyncState.value = _driveSyncState.value.copy(
                            status = DriveSyncStatus.RESTRICTED_403,
                            message = driveRestricted403Message(isBn),
                            isError = true
                        )
                    }

                    DriveAuthErrorKind.OFFLINE -> {
                        _driveSyncState.value = _driveSyncState.value.copy(
                            status = DriveSyncStatus.PENDING_OFFLINE,
                            message = if (isBn) {
                                "অফলাইন — অনলাইন হলে Google Drive অটো-সিঙ্ক চালু হবে।"
                            } else {
                                "Offline — encrypted Google Drive sync will resume once online."
                            },
                            isError = false
                        )
                    }

                    DriveAuthErrorKind.DECLINED -> {
                        _driveSyncState.value = _driveSyncState.value.copy(
                            status = DriveSyncStatus.NEEDS_PERMISSION,
                            message = if (isBn) {
                                "Google Drive অনুমতি বাতিল হয়েছে। লোকাল হিসাব নিরাপদ আছে—যেকোনো সময় অনুমতি দিতে পারবেন।"
                            } else {
                                "Google Drive permission was declined. Local records remain safe."
                            },
                            isError = false
                        )
                    }

                    DriveAuthErrorKind.PLAY_SERVICES_UNAVAILABLE,
                    DriveAuthErrorKind.GENERAL -> {
                        _driveSyncState.value = _driveSyncState.value.copy(
                            status = DriveSyncStatus.NEEDS_PERMISSION,
                            message = if (isBn) {
                                "এই মুহূর্তে Google Drive সংযোগ করা যায়নি। লোকাল অ্যাপ ও JSON/CSV ব্যাকআপ সম্পূর্ণ সচল আছে।"
                            } else {
                                "Unable to connect to Google Drive right now. Local tracking and JSON/CSV backup remain fully available."
                            },
                            isError = false
                        )
                    }
                }
                null
            }
        }
    }

    private fun driveRestricted403Message(isBn: Boolean): String = if (isBn) {
        "Google OAuth/Drive যাচাইকরণ সীমাবদ্ধতার (Error 403) কারণে ক্লাউড সিঙ্ক সাময়িকভাবে বন্ধ আছে। আপনার সব হিসাব লোকালি নিরাপদ আছে এবং JSON/CSV ব্যাকআপ সম্পূর্ণ সচল রয়েছে।"
    } else {
        "Google Drive sync is temporarily blocked by external OAuth verification settings (Error 403). Your local accounts, transactions, AI tools, and JSON/CSV backup continue working safely."
    }

    fun setEnableSubcategories(enabled: Boolean) = viewModelScope.launch {
        prefsRepo.setEnableSubcategories(enabled)
    }

    fun setAutoOpenKeypad(enabled: Boolean) = viewModelScope.launch {
        prefsRepo.setAutoOpenKeypad(enabled)
    }

    fun setDefaultAccountId(accountId: Long) = viewModelScope.launch {
        prefsRepo.setDefaultAccountId(accountId)
    }

    fun setStartOfWeek(startOfWeek: StartOfWeek) = viewModelScope.launch {
        prefsRepo.setStartOfWeek(startOfWeek)
    }

    fun setMainCurrency(currencyCode: String) = viewModelScope.launch {
        prefsRepo.setMainCurrency(currencyCode)
        scheduleCentralFirestoreSync()
    }

    fun setSecondaryCurrency(currencyCode: String) = viewModelScope.launch {
        prefsRepo.setSecondaryCurrency(currencyCode)
        scheduleCentralFirestoreSync()
    }

    fun setUserCountry(countryCode: String) = viewModelScope.launch {
        prefsRepo.setUserCountryCode(countryCode)
        scheduleCentralFirestoreSync()
    }

    fun setHapticFeedback(enabled: Boolean) = viewModelScope.launch {
        prefsRepo.setHapticFeedback(enabled)
    }

    fun setNotificationPreferences(budget: Boolean, credit: Boolean, loan: Boolean) = viewModelScope.launch {
        prefsRepo.setNotificationPreferences(budget, credit, loan)
    }

    fun setDailyReminderConfig(enabled: Boolean, hour: Int, minute: Int) = viewModelScope.launch {
        prefsRepo.setDailyReminderConfig(enabled, hour, minute)
    }

    fun setLoanDayBeforeEnabled(enabled: Boolean) = viewModelScope.launch {
        prefsRepo.setLoanDayBeforeEnabled(enabled)
    }

    fun setHighSpendAlertEnabled(enabled: Boolean) = viewModelScope.launch {
        prefsRepo.setHighSpendAlertConfig(enabled)
    }

    fun clearNotificationHistory() = viewModelScope.launch {
        prefsRepo.clearNotificationLog()
    }

    fun setAppLock(enabled: Boolean, pin: String) = viewModelScope.launch {
        prefsRepo.setAppLock(enabled, pin)
        if (enabled) {
            // Keep current authenticated session unlocked after user explicitly enables PIN lock;
            // next cold launch or background return will require authentication.
            _isAppUnlocked.value = true
        } else if (!uiState.value.prefs.biometricUnlockEnabled) {
            _isAppUnlocked.value = true
        }
    }

    fun setBiometricUnlock(enabled: Boolean) = viewModelScope.launch {
        prefsRepo.setBiometricUnlockEnabled(enabled)
        if (enabled) {
            // User already verified biometrics when toggling ON; stay unlocked in this session,
            // and enforce biometric authentication on launch and background return.
            _isAppUnlocked.value = true
        } else if (!uiState.value.prefs.appLockEnabled) {
            _isAppUnlocked.value = true
        }
    }

    fun onAppBackgrounded(nowMillis: Long = System.currentTimeMillis()) {
        if (uiState.value.prefs.isLaunchLockActive && _isAppUnlocked.value) {
            lastBackgroundedAtMillis = nowMillis
        }
    }

    fun onAppForegrounded(
        nowMillis: Long = System.currentTimeMillis(),
        graceWindowMillis: Long = 2_000L
    ) {
        val bgAt = lastBackgroundedAtMillis
        lastBackgroundedAtMillis = 0L
        if (bgAt > 0L && uiState.value.prefs.isLaunchLockActive) {
            if (nowMillis - bgAt >= graceWindowMillis) {
                _isAppUnlocked.value = false
            }
        }
    }

    fun updateHomeDashboardLayout(
        layoutStyle: HomeLayoutStyle,
        order: List<HomeSectionId>,
        hidden: Set<HomeSectionId>
    ) {
        val cleanOrder = order.distinct().let { list ->
            list + HomeSectionId.entries.filterNot { it in list }
        }
        val cleanHidden = hidden.toSet()
        _pendingDashboardLayout.value = PendingDashboardLayout(
            layoutStyle = layoutStyle,
            order = cleanOrder,
            hidden = cleanHidden
        )
        viewModelScope.launch {
            prefsRepo.updateHomeDashboardLayout(layoutStyle, cleanOrder, cleanHidden)
        }
    }

    fun updateCategoryOrder(type: TransactionType, orderedIds: List<String>) = viewModelScope.launch {
        prefsRepo.setCategoryOrder(type, orderedIds)
    }

    fun setCategorySortMode(mode: CategorySortMode) = viewModelScope.launch {
        prefsRepo.setCategorySortMode(mode)
    }

    fun upsertManagedCategory(item: ManagedCategoryItem) = viewModelScope.launch {
        prefsRepo.upsertManagedCategory(item)
        scheduleCentralFirestoreSync()
    }

    fun deleteManagedCategory(categoryId: String, isBuiltIn: Boolean) = viewModelScope.launch {
        prefsRepo.deleteManagedCategory(categoryId, isBuiltIn)
        scheduleCentralFirestoreSync()
    }

    fun resetCategoriesToDefault() = viewModelScope.launch {
        prefsRepo.resetCategoriesToDefault()
        scheduleCentralFirestoreSync()
    }

    fun addAccount(account: AccountEntity) = viewModelScope.launch {
        repository.addAccount(account)
        scheduleCentralFirestoreSync()
    }

    fun deleteAccount(accountId: Long) = viewModelScope.launch {
        repository.deleteAccount(accountId)
        viewModelScope.launch(Dispatchers.IO) { firestoreRepo.deleteAccountDoc(accountId) }
    }

    fun addTransaction(
        title: String,
        amount: Double,
        type: TransactionType,
        category: ExpenseCategory,
        accountId: Long,
        subcategory: String = "",
        note: String = "",
        timestamp: Long = System.currentTimeMillis()
    ) = viewModelScope.launch {
        repository.addTransaction(title, amount, type, category, subcategory, accountId, note, timestamp)
        scheduleCentralFirestoreSync()
        if (type == TransactionType.EXPENSE) {
            evaluateImmediateThresholdAlerts()
        }
    }

    private suspend fun evaluateImmediateThresholdAlerts() {
        runCatching {
            val prefs = prefsRepo.userPrefs.first()
            val accounts = db.accountDao().getAllAccountsOnce()
            val transactions = db.transactionDao().getAllTransactionsOnce()
            if (transactions.isEmpty()) return

            val timeCtx = systemClockMonitor.timeContext.value
            val activeTz = timeCtx.timeZone
            val snapshot = marketSyncState.value.snapshot
            val mainCur = prefs.mainCurrencyCode

            val cal = Calendar.getInstance(activeTz).apply { timeInMillis = System.currentTimeMillis() }
            val currentMonth = cal.get(Calendar.MONTH)
            val currentYear = cal.get(Calendar.YEAR)

            val spentThisMonth = transactions
                .filter { tx ->
                    if (tx.type != TransactionType.EXPENSE) return@filter false
                    val txCal = Calendar.getInstance(activeTz).apply { timeInMillis = tx.timestamp }
                    txCal.get(Calendar.MONTH) == currentMonth && txCal.get(Calendar.YEAR) == currentYear
                }
                .sumOf { snapshot.convertCurrencyCode(it.amount, it.currencyCode, mainCur) }

            val todayStartCal = Calendar.getInstance(activeTz).apply {
                timeInMillis = System.currentTimeMillis()
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val todayStart = todayStartCal.timeInMillis
            todayStartCal.add(Calendar.DAY_OF_MONTH, 1)
            val tomorrowStart = todayStartCal.timeInMillis

            val todaySpend = transactions
                .filter { it.type == TransactionType.EXPENSE && it.timestamp in todayStart until tomorrowStart }
                .sumOf { snapshot.convertCurrencyCode(it.amount, it.currencyCode, mainCur) }

            val triggered = AmarHisabNotificationManager.evaluateAndTriggerImmediateThresholdAlerts(
                context = getApplication(),
                prefs = prefs,
                accounts = accounts,
                transactions = transactions,
                spentThisMonth = spentThisMonth,
                todaySpend = todaySpend,
                nowMillis = System.currentTimeMillis(),
                timeZone = activeTz
            )
            triggered.forEach { item ->
                prefsRepo.appendNotificationLogEntry(item)
            }
        }.onFailure { e ->
            Log.w(TAG, "Threshold alert evaluation skipped", e)
        }
    }

    fun transferFunds(
        fromAccountId: Long,
        toAccountId: Long,
        amount: Double,
        note: String,
        timestamp: Long = System.currentTimeMillis(),
        customConversionRate: Double? = null
    ) = viewModelScope.launch {
        val currentAccounts = uiState.value.accounts
        val fromCode = currentAccounts.firstOrNull { it.id == fromAccountId }?.currencyCode ?: "BDT"
        val toCode = currentAccounts.firstOrNull { it.id == toAccountId }?.currencyCode ?: fromCode
        val conversionRate = if (fromCode.equals(toCode, ignoreCase = true)) {
            1.0
        } else {
            customConversionRate?.takeIf { it > 0.0 && it.isFinite() }
                ?: marketSyncState.value.snapshot.convertCurrencyCode(1.0, fromCode, toCode)
        }
        repository.transferFunds(
            fromAccountId = fromAccountId,
            toAccountId = toAccountId,
            amount = amount,
            note = note,
            timestamp = timestamp,
            conversionRate = conversionRate
        )
        scheduleCentralFirestoreSync()
    }

    fun deleteTransaction(id: Long) = viewModelScope.launch {
        repository.deleteTransaction(id)
        viewModelScope.launch(Dispatchers.IO) { firestoreRepo.deleteTransactionDoc(id) }
        scheduleCentralFirestoreSync()
    }

    fun addSavingsGoal(name: String, target: Double, dueDateMillis: Long?, colorArgb: Long) =
        viewModelScope.launch {
            repository.addSavingsGoal(name, target, dueDateMillis, colorArgb)
            scheduleCentralFirestoreSync()
        }

    fun fundGoalFromWallet(goal: SavingsGoalEntity, amount: Double, walletId: Long) =
        viewModelScope.launch {
            repository.fundGoalFromWallet(goal, amount, walletId)
            scheduleCentralFirestoreSync()
        }

    fun deleteGoal(goalId: Long) = viewModelScope.launch {
        repository.deleteGoal(goalId)
        viewModelScope.launch(Dispatchers.IO) { firestoreRepo.deleteGoalDoc(goalId) }
    }

    fun addLoan(
        personName: String,
        loanType: LoanType,
        totalAmount: Double,
        note: String,
        dueDateMillis: Long?,
        currencyCode: String = uiState.value.prefs.secondaryCurrencyCode.ifBlank {
            uiState.value.prefs.mainCurrencyCode
        }
    ) = viewModelScope.launch {
        repository.addLoan(personName, loanType, totalAmount, note, dueDateMillis, currencyCode)
        scheduleCentralFirestoreSync()
    }

    fun updateLoan(loan: LoanEntity) = viewModelScope.launch {
        repository.updateLoan(loan)
        scheduleCentralFirestoreSync()
    }

    fun settleLoan(loan: LoanEntity, amount: Double, walletId: Long?) = viewModelScope.launch {
        val targetAcc = walletId?.let { id -> uiState.value.accounts.firstOrNull { it.id == id } }
        val rate = if (targetAcc != null && !targetAcc.currencyCode.equals(loan.currencyCode, ignoreCase = true)) {
            marketSyncState.value.snapshot.convertCurrencyCode(1.0, loan.currencyCode, targetAcc.currencyCode)
        } else {
            1.0
        }
        repository.settleLoan(loan, amount, walletId, conversionRate = rate)
        scheduleCentralFirestoreSync()
    }

    fun deleteLoan(loanId: Long) = viewModelScope.launch {
        repository.deleteLoan(loanId)
        viewModelScope.launch(Dispatchers.IO) { firestoreRepo.deleteLoanDoc(loanId) }
    }

    fun upsertCategoryBudget(category: ExpenseCategory, limitAmount: Double) = viewModelScope.launch {
        repository.upsertCategoryBudget(category, limitAmount)
        scheduleCentralFirestoreSync()
    }

    // --- Offline Backup & Restore ---
    fun exportJsonToUri(uri: Uri, successMessage: String) {
        viewModelScope.launch {
            try {
                val json = withContext(Dispatchers.IO) {
                    BackupManager.exportJson(db, uiState.value.prefs)
                }
                withContext(Dispatchers.IO) {
                    getApplication<Application>().contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(json.toByteArray(Charsets.UTF_8))
                    } ?: throw IllegalStateException("Cannot open output stream.")
                }
                _backupStatus.value = BackupMessage(successMessage)
            } catch (e: Exception) {
                Log.e(TAG, "JSON Export failed", e)
                _backupStatus.value = BackupMessage(e.localizedMessage ?: "Export failed", isError = true)
            }
        }
    }

    fun exportCsvToUri(uri: Uri, successMessage: String) {
        viewModelScope.launch {
            try {
                val csv = withContext(Dispatchers.IO) {
                    BackupManager.exportCsv(db)
                }
                withContext(Dispatchers.IO) {
                    getApplication<Application>().contentResolver.openOutputStream(uri)?.use { out ->
                        // Write UTF-8 BOM so Excel displays Bangla script properly
                        out.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
                        out.write(csv.toByteArray(Charsets.UTF_8))
                    } ?: throw IllegalStateException("Cannot open output stream.")
                }
                _backupStatus.value = BackupMessage(successMessage)
            } catch (e: Exception) {
                Log.e(TAG, "CSV Export failed", e)
                _backupStatus.value = BackupMessage(e.localizedMessage ?: "CSV Export failed", isError = true)
            }
        }
    }

    fun restoreJsonFromUri(
        uri: Uri,
        successMessage: String,
        completeWelcome: Boolean = false
    ) {
        viewModelScope.launch {
            try {
                val jsonString = withContext(Dispatchers.IO) {
                    getApplication<Application>().contentResolver.openInputStream(uri)?.bufferedReader()
                        ?.use { it.readText() }
                        ?: throw IllegalStateException("Cannot read backup file.")
                }
                val parsedRoot = org.json.JSONObject(jsonString)
                val looksValid = parsedRoot.has("accounts") ||
                    parsedRoot.has("transactions") ||
                    parsedRoot.optString("app").equals("Amar Hisab", ignoreCase = true)
                if (!looksValid) {
                    throw IllegalArgumentException(
                        if (uiState.value.prefs.language == AppLanguage.BN) {
                            "এটি সঠিক 'আমার হিসাব' ব্যাকআপ ফাইল নয় (.json ফাইল নির্বাচন করুন)।"
                        } else {
                            "Not a valid Amar Hisab backup file. Please select a .json backup."
                        }
                    )
                }
                withContext(Dispatchers.IO) {
                    BackupManager.restoreFromJson(jsonString, db, prefsRepo)
                }
                if (completeWelcome) {
                    prefsRepo.completeWelcomeAsGuest(null)
                }
                _pendingDashboardLayout.value = null
                _backupStatus.value = BackupMessage(successMessage)
                scheduleCentralFirestoreSync()
            } catch (e: Exception) {
                Log.e(TAG, "JSON Restore failed", e)
                val isBn = uiState.value.prefs.language == AppLanguage.BN
                _backupStatus.value = BackupMessage(
                    e.localizedMessage ?: if (isBn) {
                        "ব্যাকআপ ফাইল রিস্টোর করা যায়নি—সঠিক JSON ফাইল দিন।"
                    } else {
                        "Invalid backup JSON file."
                    },
                    isError = true
                )
            }
        }
    }

    fun prepareShareBackupJson(onReady: (File) -> Unit) {
        viewModelScope.launch {
            try {
                val json = withContext(Dispatchers.IO) {
                    BackupManager.exportJson(db, uiState.value.prefs)
                }
                val file = withContext(Dispatchers.IO) {
                    val dir = File(getApplication<Application>().cacheDir, "backups").apply { mkdirs() }
                    dir.listFiles()?.forEach { old ->
                        if (old.isFile && old.name.endsWith(".json")) old.delete()
                    }
                    val stamp = java.text.SimpleDateFormat("yyyyMMdd_HHmm", java.util.Locale.US)
                        .format(java.util.Date())
                    val outFile = File(dir, "amar_hisab_backup_$stamp.json")
                    outFile.writeText(json, Charsets.UTF_8)
                    outFile
                }
                onReady(file)
            } catch (e: Exception) {
                Log.e(TAG, "Share backup preparation failed", e)
                _backupStatus.value = BackupMessage(
                    e.localizedMessage ?: "Unable to prepare backup file.",
                    isError = true
                )
            }
        }
    }

    fun exportSampleTemplateToUri(uri: Uri, successMessage: String) {
        viewModelScope.launch {
            try {
                val templateCsv = BackupManager.generateSampleTemplateCsv()
                withContext(Dispatchers.IO) {
                    getApplication<Application>().contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
                        out.write(templateCsv.toByteArray(Charsets.UTF_8))
                    } ?: throw IllegalStateException("Cannot open output stream.")
                }
                _backupStatus.value = BackupMessage(successMessage)
            } catch (e: Exception) {
                Log.e(TAG, "Template export failed", e)
                _backupStatus.value = BackupMessage(
                    e.localizedMessage ?: "Template download failed.",
                    isError = true
                )
            }
        }
    }

    fun deleteAllFinancialData(successMessage: String) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    BackupManager.clearAllFinancialData(db)
                    firestoreRepo.clearAllUserSubcollections()
                }
                _backupStatus.value = BackupMessage(successMessage)
            } catch (e: Exception) {
                Log.e(TAG, "Clear all financial data failed", e)
                _backupStatus.value = BackupMessage(
                    e.localizedMessage ?: "Failed to clear data.",
                    isError = true
                )
            }
        }
    }

    fun clearBackupStatus() {
        _backupStatus.value = null
    }

    // --- AI Receipt Scan (Gemini Vision with automatic on-device ML Kit OCR fallback) ---
    fun analyzeReceiptUri(uri: Uri) {
        val snapshot = uiState.value
        val lang = snapshot.prefs.language
        val defaultCurrency = snapshot.prefs.secondaryCurrencyCode.ifBlank {
            snapshot.prefs.mainCurrencyCode
        }
        viewModelScope.launch {
            _receiptState.value = ReceiptScanUiState.Loading
            val bitmap = try {
                withContext(Dispatchers.IO) { loadBitmapFromUri(uri) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Unable to decode receipt bitmap", e)
                val sanitized = InsightsSummaryHelper.sanitizeReceiptScanError(
                    rawMessage = e.localizedMessage,
                    throwable = e,
                    language = lang
                )
                _receiptState.value = ReceiptScanUiState.Error(
                    message = sanitized.message,
                    guidance = sanitized.guidance,
                    category = sanitized.category
                )
                return@launch
            }

            val mergedCategories = com.example.data.model.BuiltInCategoriesCatalog.mergedAll(
                overrides = snapshot.prefs.managedCategoryOverrides,
                deletedIds = snapshot.prefs.deletedCategoryIds
            )

            // 1. Try cloud Gemini Vision AI first (when online)
            val aiResult = if (connectivityMonitor.refreshNow()) {
                try {
                    aiService.analyzeReceiptImage(bitmap, lang)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w(TAG, "Cloud receipt AI unavailable; switching seamlessly to on-device OCR", e)
                    null
                }
            } else {
                null
            }

            if (aiResult != null && aiResult.amountBdt > 0.0) {
                val unified = com.example.data.ai.UnifiedCategoryDetector.detectCategory(
                    text = "${aiResult.merchantName} ${aiResult.itemsSummary} ${aiResult.detectedDirectionKeyword}",
                    type = aiResult.transactionType,
                    managedCategories = mergedCategories,
                    language = lang
                )
                val resolvedManaged = if (unified.isExactCustomMatch ||
                    (unified.managedCategory.id != "exp_other" && unified.managedCategory.id != "inc_other")
                ) {
                    unified.managedCategory
                } else {
                    com.example.data.model.BuiltInCategoriesCatalog.resolveForLegacy(
                        type = aiResult.transactionType,
                        legacyCategory = aiResult.category,
                        mergedItems = mergedCategories
                    ) ?: unified.managedCategory
                }
                val enriched = aiResult.copy(
                    category = resolvedManaged.legacyCategory ?: aiResult.category,
                    managedCategoryId = resolvedManaged.id,
                    managedCategoryLabel = resolvedManaged.label(lang)
                )
                _receiptState.value = ReceiptScanUiState.Success(enriched)
                return@launch
            }

            // 2. Seamless on-device ML Kit OCR fallback focusing on Total Amount & Currency
            val localResult = try {
                LocalReceiptTextParser.scanBitmapLocally(
                    bitmap = bitmap,
                    defaultCurrencyCode = defaultCurrency,
                    managedCategories = mergedCategories,
                    language = lang
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "On-device receipt OCR fallback failed", e)
                null
            }

            if (localResult != null && localResult.amountBdt > 0.0) {
                _receiptState.value = ReceiptScanUiState.Success(localResult)
                return@launch
            }

            // 3. Calm, non-blocking manual fallback prompt (never expose raw App Check errors)
            val isBn = lang == AppLanguage.BN
            _receiptState.value = ReceiptScanUiState.Error(
                message = if (isBn) {
                    "রসিদের মোট টাকা স্পষ্টভাবে পড়া যায়নি।"
                } else {
                    "Could not clearly detect the total amount on this receipt."
                },
                guidance = if (isBn) {
                    "রসিদের ছবিটি দেখে নিচে ম্যানুয়ালি টাকা ও মুদ্রা লিখে সেভ করুন, অথবা স্পষ্ট ছবি তুলে আবার স্ক্যান করুন।"
                } else {
                    "Tap below to enter the total amount and currency manually while viewing the receipt, or retake a clearer photo."
                },
                category = ReceiptScanErrorCategory.GENERAL
            )
        }
    }

    fun resetReceiptScan() {
        _receiptState.value = ReceiptScanUiState.Idle
        com.example.data.ai.TempReceiptImageCleaner.cleanupAllTempReceipts(getApplication())
    }

    // --- Gemini Search Grounding (gemini-3.8-flash + googleSearch) ---
    fun searchMarketInfo(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return
        val lang = uiState.value.prefs.language
        viewModelScope.launch {
            _searchAiState.value = SearchAiUiState.Loading
            try {
                val result = aiService.askFinancialSearch(trimmed, lang)
                _searchAiState.value = SearchAiUiState.Success(result)
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Search grounding timed out", e)
                val sanitized = InsightsSummaryHelper.sanitizeAiError(
                    rawMessage = "timeout",
                    throwable = e,
                    language = lang,
                    isVoice = false
                )
                _searchAiState.value = SearchAiUiState.Error(
                    message = sanitized.message,
                    category = sanitized.category,
                    failedQuery = trimmed
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Search grounding failed", e)
                val sanitized = InsightsSummaryHelper.sanitizeAiError(
                    rawMessage = e.localizedMessage,
                    throwable = e,
                    language = lang,
                    isVoice = false
                )
                _searchAiState.value = SearchAiUiState.Error(
                    message = sanitized.message,
                    category = sanitized.category,
                    failedQuery = trimmed
                )
            }
        }
    }

    // --- AI Voice / Text Entry Parser (Gemini AI with automatic local Bangla/English fallback) ---
    fun parseVoiceEntry(text: String) {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return
        val snapshot = uiState.value
        val lang = snapshot.prefs.language
        val accounts = snapshot.accounts
        val defaultCurrency = snapshot.prefs.secondaryCurrencyCode.ifBlank {
            snapshot.prefs.mainCurrencyCode
        }
        val accountLabels = accounts.map { acc ->
            "${acc.name} (${acc.bankOrProvider.ifBlank { acc.type.name }}, ${acc.currencyCode})"
        }

        viewModelScope.launch {
            _voiceState.value = VoiceParseUiState.Loading

            val mergedCategories = com.example.data.model.BuiltInCategoriesCatalog.mergedAll(
                overrides = snapshot.prefs.managedCategoryOverrides,
                deletedIds = snapshot.prefs.deletedCategoryIds
            )

            // 1. Try cloud Gemini AI first when online
            val aiParsed = if (connectivityMonitor.refreshNow()) {
                try {
                    aiService.parseVoiceOrTextEntry(
                        utterance = trimmed,
                        language = lang,
                        availableAccountNames = accountLabels,
                        defaultCurrencyCode = defaultCurrency
                    )
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w(TAG, "Cloud voice AI unavailable; switching seamlessly to local parser", e)
                    null
                }
            } else {
                null
            }

            if (aiParsed != null && aiParsed.amountBdt > 0.0) {
                val unified = com.example.data.ai.UnifiedCategoryDetector.detectCategory(
                    text = "$trimmed ${aiParsed.title}",
                    type = aiParsed.type,
                    managedCategories = mergedCategories,
                    language = lang
                )
                val resolvedManaged = if (unified.isExactCustomMatch ||
                    (unified.managedCategory.id != "exp_other" && unified.managedCategory.id != "inc_other")
                ) {
                    unified.managedCategory
                } else {
                    com.example.data.model.BuiltInCategoriesCatalog.resolveForLegacy(
                        type = aiParsed.type,
                        legacyCategory = aiParsed.category,
                        mergedItems = mergedCategories
                    ) ?: unified.managedCategory
                }
                val enriched = aiParsed.copy(
                    category = resolvedManaged.legacyCategory ?: aiParsed.category,
                    managedCategoryId = resolvedManaged.id,
                    managedCategoryLabel = resolvedManaged.label(lang)
                )
                _voiceState.value = VoiceParseUiState.Success(enriched)
                return@launch
            }

            // 2. Seamless local Bangla/English/Banglish parser fallback (supports transfers, bKash, multi-currency)
            val localParsed = LocalVoiceTransactionParser.parse(
                utterance = trimmed,
                accounts = accounts,
                language = lang,
                defaultCurrencyCode = defaultCurrency,
                managedCategories = mergedCategories
            )
            if (localParsed.amountBdt > 0.0) {
                _voiceState.value = VoiceParseUiState.Success(localParsed)
                return@launch
            }

            // 3. Friendly prompt if no amount was spoken/typed
            val isBn = lang == AppLanguage.BN
            _voiceState.value = VoiceParseUiState.Error(
                if (isBn) {
                    "টাকার পরিমাণ স্পষ্টভাবে বলুন বা লিখুন — যেমন: \"বিকাশ থেকে বাজার খরচ ১৫০০ টাকা\" বা \"Trust থেকে Dutch-Bangla তে ৫০০০ টাকা পাঠিয়েছি\""
                } else {
                    "Please include an amount — e.g. \"bKash groceries 1500 BDT\" or \"Transfer 5000 from Trust to Dutch-Bangla\"."
                }
            )
        }
    }

    fun resetVoiceParse() {
        _voiceState.value = VoiceParseUiState.Idle
    }

    // --- Full Detailed Voice Financial Summary (Chunked Native TTS + Built-In Audio Fallback) ---
    fun playFinancialBriefingTts() {
        val snapshot = uiState.value
        val lang = snapshot.prefs.language
        val currencyCode = snapshot.prefs.mainCurrencyCode
        val fullSummaryScript = InsightsSummaryHelper.buildPeriodSummary(snapshot)

        val engineLabelFor: (SpeechEngineSource) -> String = { source ->
            when (source) {
                SpeechEngineSource.NATIVE_TTS -> if (lang == AppLanguage.BN) "ডিভাইস ভয়েস" else "Device Voice"
                SpeechEngineSource.BUILT_IN_AUDIO -> if (lang == AppLanguage.BN) "বিল্ট-ইন অডিও" else "Built-in Audio"
            }
        }

        voiceSummaryEngine.play(
            displayScript = fullSummaryScript,
            language = lang,
            currencyCode = currencyCode,
            listener = object : VoiceSummaryPlaybackListener {
                override fun onPreparing(script: String) {
                    _ttsState.value = TtsUiState.Generating
                }

                override fun onPlaying(
                    script: String,
                    progress: Float,
                    currentChunkIndex: Int,
                    totalChunks: Int,
                    engineSource: SpeechEngineSource,
                    voiceLabel: String
                ) {
                    _ttsState.value = TtsUiState.Playing(
                        script = script,
                        progress = progress,
                        currentChunkIndex = currentChunkIndex,
                        totalChunks = totalChunks,
                        engineLabel = engineLabelFor(engineSource),
                        voiceLocaleLabel = voiceLabel
                    )
                }

                override fun onPaused(
                    script: String,
                    progress: Float,
                    currentChunkIndex: Int,
                    totalChunks: Int,
                    engineSource: SpeechEngineSource,
                    voiceLabel: String
                ) {
                    _ttsState.value = TtsUiState.Paused(
                        script = script,
                        progress = progress,
                        currentChunkIndex = currentChunkIndex,
                        totalChunks = totalChunks,
                        engineLabel = engineLabelFor(engineSource),
                        voiceLocaleLabel = voiceLabel
                    )
                }

                override fun onCompleted(
                    script: String,
                    totalChunks: Int,
                    engineSource: SpeechEngineSource,
                    voiceLabel: String
                ) {
                    _ttsState.value = TtsUiState.Completed(
                        script = script,
                        totalChunks = totalChunks,
                        engineLabel = engineLabelFor(engineSource),
                        voiceLocaleLabel = voiceLabel
                    )
                }

                override fun onStopped() {
                    _ttsState.value = TtsUiState.Idle
                }

                override fun onUnavailable(script: String, reason: String) {
                    Log.w(TAG, "Voice summary playback fallback notice: $reason")
                    val sanitized = InsightsSummaryHelper.sanitizeAiError(
                        rawMessage = reason,
                        language = lang,
                        isVoice = true
                    )
                    _ttsState.value = TtsUiState.Error(
                        message = sanitized.message,
                        category = sanitized.category,
                        script = script
                    )
                }
            }
        )
    }

    fun pauseTtsPlayback() {
        if (_ttsState.value is TtsUiState.Playing) {
            voiceSummaryEngine.pause()
        }
    }

    fun resumeTtsPlayback() {
        if (_ttsState.value is TtsUiState.Paused) {
            voiceSummaryEngine.resume()
        }
    }

    fun stopTtsPlayback() {
        voiceSummaryEngine.stop()
        _ttsState.value = TtsUiState.Idle
    }

    // ─── Customer Support (কাস্টমার সাপোর্ট) ───────────────────────────────────────────

    private fun nextSupportMsgId(): String {
        supportMessageCounter += 1L
        return "sup_${supportMessageCounter}_${System.nanoTime()}"
    }

    fun ensureCustomerSupportInitialized() {
        val current = _supportState.value
        if (current.messages.isNotEmpty()) return
        val now = systemClockMonitor.timeContext.value.epochMillis
        _supportState.value = current.copy(
            messages = listOf(
                SupportChatMessage(
                    id = nextSupportMsgId(),
                    sender = SupportSender.ASSISTANT,
                    text = SUPPORT_INITIAL_GREETING_BN,
                    timestampMillis = now
                )
            )
        )
    }

    fun resetCustomerSupportChat() {
        val now = systemClockMonitor.timeContext.value.epochMillis
        _supportState.value = SupportUiState(
            messages = listOf(
                SupportChatMessage(
                    id = nextSupportMsgId(),
                    sender = SupportSender.ASSISTANT,
                    text = SUPPORT_INITIAL_GREETING_BN,
                    timestampMillis = now
                )
            ),
            selectedCategoryId = null,
            isTyping = false,
            isOnline = connectivityMonitor.isOnline.value,
            hasAttemptedTroubleshooting = false
        )
    }

    fun selectSupportCategory(categoryId: SupportCategoryId?) {
        ensureCustomerSupportInitialized()
        _supportState.value = _supportState.value.copy(selectedCategoryId = categoryId)
    }

    fun selectSupportQuickOption(option: SupportQuickOption) {
        ensureCustomerSupportInitialized()
        val now = System.currentTimeMillis()
        val prev = _supportState.value
        val userMsg = SupportChatMessage(
            id = nextSupportMsgId(),
            sender = SupportSender.USER,
            text = option.labelBn,
            timestampMillis = now
        )

        val escalateNow = option.isLiveCareTrigger && prev.hasAttemptedTroubleshooting
        val assistantMsg = if (escalateNow) {
            SupportChatMessage(
                id = nextSupportMsgId(),
                sender = SupportSender.ASSISTANT,
                text = CustomerSupportEngine.buildWhatsAppEscalationMessage(isCritical = false),
                timestampMillis = now + 1L,
                showWhatsAppCard = true
            )
        } else {
            SupportChatMessage(
                id = nextSupportMsgId(),
                sender = SupportSender.ASSISTANT,
                text = option.guideBn,
                timestampMillis = now + 1L,
                showWhatsAppCard = false,
                offerNotResolvedAction = true
            )
        }

        _supportState.value = prev.copy(
            messages = prev.messages + userMsg + assistantMsg,
            selectedCategoryId = null,
            hasAttemptedTroubleshooting = true
        )
    }

    fun requestLiveCareAfterTroubleshooting() {
        ensureCustomerSupportInitialized()
        val prev = _supportState.value
        val now = System.currentTimeMillis()
        val userMsg = SupportChatMessage(
            id = nextSupportMsgId(),
            sender = SupportSender.USER,
            text = "সমাধান হয়নি — লাইভ সাপোর্ট চাই",
            timestampMillis = now
        )
        val escalationMsg = SupportChatMessage(
            id = nextSupportMsgId(),
            sender = SupportSender.ASSISTANT,
            text = CustomerSupportEngine.buildWhatsAppEscalationMessage(isCritical = false),
            timestampMillis = now + 1L,
            showWhatsAppCard = true
        )
        _supportState.value = prev.copy(
            messages = prev.messages + userMsg + escalationMsg,
            selectedCategoryId = null,
            hasAttemptedTroubleshooting = true
        )
    }

    fun sendCustomerSupportMessage(rawMessage: String) {
        val trimmed = rawMessage.trim()
        if (trimmed.isEmpty()) return
        ensureCustomerSupportInitialized()

        val prev = _supportState.value
        val now = System.currentTimeMillis()
        val userMsg = SupportChatMessage(
            id = nextSupportMsgId(),
            sender = SupportSender.USER,
            text = trimmed,
            timestampMillis = now
        )

        // 1. Non-blocking polite greeting response (never leaks WhatsApp)
        if (CustomerSupportEngine.isPureGreeting(trimmed)) {
            val reply = SupportChatMessage(
                id = nextSupportMsgId(),
                sender = SupportSender.ASSISTANT,
                text = SUPPORT_GREETING_REPLY_BN,
                timestampMillis = now + 1L,
                showWhatsAppCard = false
            )
            _supportState.value = prev.copy(
                messages = prev.messages + userMsg + reply,
                selectedCategoryId = null
            )
            return
        }

        // 2. Explicit live-care request AFTER prior troubleshooting
        val isCritical = CustomerSupportEngine.isCriticalIssue(trimmed)
        if (prev.hasAttemptedTroubleshooting && CustomerSupportEngine.isExplicitHumanRequest(trimmed)) {
            val reply = SupportChatMessage(
                id = nextSupportMsgId(),
                sender = SupportSender.ASSISTANT,
                text = CustomerSupportEngine.buildWhatsAppEscalationMessage(isCritical = isCritical),
                timestampMillis = now + 1L,
                showWhatsAppCard = true
            )
            _supportState.value = prev.copy(
                messages = prev.messages + userMsg + reply,
                selectedCategoryId = null
            )
            return
        }

        val onlineNow = connectivityMonitor.refreshNow()
        val updatedMessages = prev.messages + userMsg

        // 3. Offline handling: built-in guide match or polite Bangla offline notice
        if (!onlineNow) {
            val matchedGuide = CustomerSupportEngine.matchLocalGuide(trimmed)
            val escalate = CustomerSupportEngine.shouldEscalateToWhatsApp(
                userMessage = trimmed,
                hasAttemptedTroubleshooting = prev.hasAttemptedTroubleshooting
            )
            val assistantMsg = when {
                matchedGuide != null -> SupportChatMessage(
                    id = nextSupportMsgId(),
                    sender = SupportSender.ASSISTANT,
                    text = matchedGuide.guideBn + SUPPORT_OFFLINE_GUIDE_FOOTER_BN,
                    timestampMillis = now + 1L,
                    showWhatsAppCard = escalate,
                    offerNotResolvedAction = !escalate
                )

                escalate -> SupportChatMessage(
                    id = nextSupportMsgId(),
                    sender = SupportSender.ASSISTANT,
                    text = CustomerSupportEngine.buildWhatsAppEscalationMessage(isCritical = isCritical),
                    timestampMillis = now + 1L,
                    showWhatsAppCard = true
                )

                else -> SupportChatMessage(
                    id = nextSupportMsgId(),
                    sender = SupportSender.ASSISTANT,
                    text = SUPPORT_OFFLINE_AI_NOTICE_BN,
                    timestampMillis = now + 1L,
                    showWhatsAppCard = false,
                    isOfflineNotice = true
                )
            }
            _supportState.value = prev.copy(
                messages = updatedMessages + assistantMsg,
                selectedCategoryId = null,
                isTyping = false,
                hasAttemptedTroubleshooting = prev.hasAttemptedTroubleshooting || matchedGuide != null || escalate
            )
            return
        }

        // 4. Online custom query via Firebase AI Logic (gemini-3.8-flash)
        _supportState.value = prev.copy(
            messages = updatedMessages,
            selectedCategoryId = null,
            isTyping = true
        )

        viewModelScope.launch {
            try {
                val aiReply = aiService.askCustomerSupport(
                    userMessage = trimmed,
                    recentHistory = updatedMessages,
                    hasAttemptedTroubleshooting = prev.hasAttemptedTroubleshooting
                )
                val escalate = CustomerSupportEngine.shouldEscalateToWhatsApp(
                    userMessage = trimmed,
                    hasAttemptedTroubleshooting = prev.hasAttemptedTroubleshooting,
                    aiSuggestedEscalation = aiReply.needsHumanEscalation
                )
                val assistantMsg = SupportChatMessage(
                    id = nextSupportMsgId(),
                    sender = SupportSender.ASSISTANT,
                    text = aiReply.replyText,
                    timestampMillis = System.currentTimeMillis(),
                    showWhatsAppCard = escalate,
                    offerNotResolvedAction = !escalate,
                    followUpChips = aiReply.suggestedFollowUps
                )
                val latest = _supportState.value
                _supportState.value = latest.copy(
                    messages = latest.messages + assistantMsg,
                    isTyping = false,
                    hasAttemptedTroubleshooting = true
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Customer support AI fallback engaged", e)
                val matchedGuide = CustomerSupportEngine.matchLocalGuide(trimmed)
                val escalate = CustomerSupportEngine.shouldEscalateToWhatsApp(
                    userMessage = trimmed,
                    hasAttemptedTroubleshooting = prev.hasAttemptedTroubleshooting
                )
                val fallbackText = when {
                    matchedGuide != null -> matchedGuide.guideBn
                    escalate -> CustomerSupportEngine.buildWhatsAppEscalationMessage(isCritical = isCritical)
                    e is TimeoutCancellationException ->
                        "এই মুহূর্তে লাইভ এআই সাড়া দিতে একটু সময় নিচ্ছে। আপনি নিচের ক্যাটাগরি গাইড থেকে তাৎক্ষণিক উত্তর দেখতে পারেন অথবা আবার চেষ্টা করুন।"
                    else ->
                        "এই মুহূর্তে লাইভ এআই সংযোগে সাময়িক সমস্যা হয়েছে। নিচের ক্যাটাগরি গাইডগুলো অফলাইনেও কাজ করে—অথবা কিছুক্ষণ পর আবার চেষ্টা করুন।"
                }
                val assistantMsg = SupportChatMessage(
                    id = nextSupportMsgId(),
                    sender = SupportSender.ASSISTANT,
                    text = fallbackText,
                    timestampMillis = System.currentTimeMillis(),
                    showWhatsAppCard = escalate,
                    offerNotResolvedAction = !escalate && matchedGuide != null
                )
                val latest = _supportState.value
                _supportState.value = latest.copy(
                    messages = latest.messages + assistantMsg,
                    isTyping = false,
                    hasAttemptedTroubleshooting = latest.hasAttemptedTroubleshooting || matchedGuide != null || escalate
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        voiceSummaryEngine.shutdown()
        ttsPlayer.stop()
        connectivityMonitor.stop()
        systemClockMonitor.stop()
    }

    private fun loadBitmapFromUri(uri: Uri): Bitmap {
        val resolver = getApplication<Application>().contentResolver
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(resolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, _, _ -> decoder.isMutableRequired = false }
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(resolver, uri)
        }
    }

    private data class DbSnapshot(
        val accounts: List<AccountEntity>,
        val transactions: List<TransactionEntity>,
        val goals: List<SavingsGoalEntity>,
        val loans: List<LoanEntity>,
        val categoryBudgets: List<CategoryBudgetEntity>
    )

    private data class ScreenNavState(
        val tab: AppTab,
        val showAccounts: Boolean,
        val showSettings: Boolean,
        val showLiveMarket: Boolean
    )

    private data class NavFilterState(
        val tab: AppTab,
        val showAccounts: Boolean,
        val showSettings: Boolean,
        val showLiveMarket: Boolean,
        val filter: InsightTimeFilter,
        val monthOffset: Int
    )

    private fun buildMainUiState(
        prefs: UserPrefs,
        dbData: DbSnapshot,
        nav: NavFilterState,
        timeContext: DeviceTimeContext,
        marketSnapshot: MarketSnapshot = MarketSnapshot.defaultReference()
    ): MainUiState {
        val accounts = dbData.accounts
        val transactions = dbData.transactions
        val activeTz = timeContext.timeZone
        val mainCurCode = prefs.mainCurrencyCode

        fun toMainCurrency(amount: Double, currencyCode: String): Double =
            marketSnapshot.convertCurrencyCode(amount, currencyCode, mainCurCode)

        val liquidBalancesByCurrency: Map<String, Double> = accounts
            .filter { it.type != AccountType.CREDIT_CARD }
            .groupBy { AppCurrency.fromCode(it.currencyCode).code }
            .mapValues { (_, list) -> list.sumOf { it.balance.coerceAtLeast(0.0) } }

        val assets = accounts
            .filter { it.type != AccountType.CREDIT_CARD }
            .sumOf { toMainCurrency(it.balance.coerceAtLeast(0.0), it.currencyCode) }
        val liabilities = accounts
            .filter { it.type == AccountType.CREDIT_CARD }
            .sumOf { toMainCurrency(it.balance.coerceAtLeast(0.0), it.currencyCode) }

        val cal = Calendar.getInstance(activeTz).apply { timeInMillis = timeContext.epochMillis }
        val currentMonth = cal.get(Calendar.MONTH)
        val currentYear = cal.get(Calendar.YEAR)
        val currentDay = cal.get(Calendar.DAY_OF_MONTH).coerceAtLeast(1)

        val monthTx = transactions.filter { tx ->
            val txCal = Calendar.getInstance(activeTz).apply { timeInMillis = tx.timestamp }
            txCal.get(Calendar.MONTH) == currentMonth && txCal.get(Calendar.YEAR) == currentYear
        }
        val spentThisMonth = monthTx
            .filter { it.type == TransactionType.EXPENSE }
            .sumOf { toMainCurrency(it.amount, it.currencyCode) }
        val incomeThisMonth = monthTx
            .filter { it.type == TransactionType.INCOME }
            .sumOf { toMainCurrency(it.amount, it.currencyCode) }
        val dailyAverage = if (spentThisMonth > 0) spentThisMonth / currentDay else 0.0

        val todayStartCal = Calendar.getInstance(activeTz).apply {
            timeInMillis = timeContext.epochMillis
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val todayStart = todayStartCal.timeInMillis
        val tomorrowStart = (todayStartCal.clone() as Calendar).apply {
            add(Calendar.DAY_OF_MONTH, 1)
        }.timeInMillis

        val todaySpend = transactions
            .filter { it.type == TransactionType.EXPENSE && it.timestamp in todayStart until tomorrowStart }
            .sumOf { toMainCurrency(it.amount, it.currencyCode) }

        var streak = 0
        if (transactions.isNotEmpty()) {
            val dayCap = maxOf(dailyAverage * 1.15, prefs.monthlyBudget / 30.0)
            for (offset in 0..29) {
                val dayStartCal = Calendar.getInstance(activeTz).apply {
                    timeInMillis = timeContext.epochMillis
                    add(Calendar.DAY_OF_MONTH, -offset)
                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                }
                val dayStart = dayStartCal.timeInMillis
                val nextDayStart = (dayStartCal.clone() as Calendar).apply {
                    add(Calendar.DAY_OF_MONTH, 1)
                }.timeInMillis
                val daySpent = transactions
                    .filter { it.type == TransactionType.EXPENSE && it.timestamp in dayStart until nextDayStart }
                    .sumOf { toMainCurrency(it.amount, it.currencyCode) }
                if (daySpent <= dayCap) streak++ else break
            }
        }

        val catSpentMap = monthTx
            .filter { it.type == TransactionType.EXPENSE }
            .groupBy { it.category }
            .mapValues { (_, list) -> list.sumOf { toMainCurrency(it.amount, it.currencyCode) } }

        val topCategories = catSpentMap.entries
            .map { (cat, sum) ->
                CategorySpend(cat, sum, if (spentThisMonth > 0) (sum / spentThisMonth).toFloat() else 0f)
            }
            .sortedByDescending { it.amount }
            .take(3)

        val analytics = computeAnalytics(
            transactions = transactions,
            filter = nav.filter,
            monthOffset = nav.monthOffset,
            startOfWeek = prefs.startOfWeek,
            timeContext = timeContext,
            mainCurrencyCode = mainCurCode,
            marketSnapshot = marketSnapshot
        )

        val alerts = buildSmartAlerts(
            prefs = prefs,
            accounts = accounts,
            transactions = transactions,
            loans = dbData.loans,
            spentThisMonth = spentThisMonth,
            todaySpend = todaySpend,
            timeContext = timeContext
        )

        return MainUiState(
            isPrefsLoaded = true,
            prefs = prefs,
            accounts = accounts,
            transactions = transactions,
            goals = dbData.goals,
            loans = dbData.loans,
            categoryBudgets = dbData.categoryBudgets,
            totalLiquidBalance = assets,
            liquidBalancesByCurrency = liquidBalancesByCurrency,
            netWorth = NetWorthSummary(assets, liabilities),
            spentThisMonth = spentThisMonth,
            incomeThisMonth = incomeThisMonth,
            dailyAverageSpend = dailyAverage,
            todaySpend = todaySpend,
            streakDays = if (transactions.isEmpty()) 0 else streak.coerceAtLeast(1),
            topCategories = topCategories,
            categorySpentMap = catSpentMap,
            smartAlerts = alerts,
            selectedTab = nav.tab,
            showAccountsScreen = nav.showAccounts,
            showSettingsScreen = nav.showSettings,
            showLiveMarketScreen = nav.showLiveMarket,
            insightFilter = nav.filter,
            insightMonthOffset = nav.monthOffset,
            analytics = analytics,
            deviceTimeContext = timeContext
        )
    }

    private fun computeAnalytics(
        transactions: List<TransactionEntity>,
        filter: InsightTimeFilter,
        monthOffset: Int,
        startOfWeek: StartOfWeek,
        timeContext: DeviceTimeContext,
        mainCurrencyCode: String = "BDT",
        marketSnapshot: MarketSnapshot = MarketSnapshot.defaultReference()
    ): AnalyticsSnapshot {
        val activeTz = timeContext.timeZone
        val startCal = Calendar.getInstance(activeTz).apply {
            timeInMillis = timeContext.epochMillis
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val endCal = Calendar.getInstance(activeTz)

        val periodLabel: String
        when (filter) {
            InsightTimeFilter.DAILY -> {
                endCal.timeInMillis = startCal.timeInMillis
                endCal.add(Calendar.DAY_OF_MONTH, 1)
                periodLabel = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                    .apply { timeZone = activeTz }
                    .format(startCal.time)
            }
            InsightTimeFilter.THIS_WEEK -> {
                startCal.firstDayOfWeek = startOfWeek.calendarDay
                while (startCal.get(Calendar.DAY_OF_WEEK) != startOfWeek.calendarDay) {
                    startCal.add(Calendar.DAY_OF_MONTH, -1)
                }
                endCal.timeInMillis = startCal.timeInMillis
                endCal.add(Calendar.DAY_OF_MONTH, 7)
                val fmt = SimpleDateFormat("dd MMM", Locale.getDefault()).apply { timeZone = activeTz }
                periodLabel = "${fmt.format(startCal.time)} – ${fmt.format(endCal.timeInMillis - 1)}"
            }
            InsightTimeFilter.LAST_MONTH -> {
                startCal.add(Calendar.MONTH, -1)
                startCal.set(Calendar.DAY_OF_MONTH, 1)
                endCal.timeInMillis = startCal.timeInMillis
                endCal.add(Calendar.MONTH, 1)
                periodLabel = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
                    .apply { timeZone = activeTz }
                    .format(startCal.time)
            }
            InsightTimeFilter.THIS_MONTH -> {
                startCal.add(Calendar.MONTH, monthOffset)
                startCal.set(Calendar.DAY_OF_MONTH, 1)
                endCal.timeInMillis = startCal.timeInMillis
                endCal.add(Calendar.MONTH, 1)
                periodLabel = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
                    .apply { timeZone = activeTz }
                    .format(startCal.time)
            }
        }

        val prevStartCal = Calendar.getInstance(activeTz).apply { timeInMillis = startCal.timeInMillis }
        val prevEndMillis = startCal.timeInMillis
        when (filter) {
            InsightTimeFilter.DAILY -> prevStartCal.add(Calendar.DAY_OF_MONTH, -1)
            InsightTimeFilter.THIS_WEEK -> prevStartCal.add(Calendar.DAY_OF_MONTH, -7)
            InsightTimeFilter.LAST_MONTH, InsightTimeFilter.THIS_MONTH -> prevStartCal.add(Calendar.MONTH, -1)
        }
        val prevStartMillis = prevStartCal.timeInMillis

        val periodTx = transactions.filter {
            it.timestamp >= startCal.timeInMillis && it.timestamp < endCal.timeInMillis
        }
        val prevPeriodTx = transactions.filter {
            it.timestamp >= prevStartMillis && it.timestamp < prevEndMillis
        }
        fun toMain(tx: TransactionEntity): Double =
            marketSnapshot.convertCurrencyCode(tx.amount, tx.currencyCode, mainCurrencyCode)

        val totalIncome = periodTx.filter { it.type == TransactionType.INCOME }.sumOf { toMain(it) }
        val totalExpense = periodTx.filter { it.type == TransactionType.EXPENSE }.sumOf { toMain(it) }
        val prevIncome = prevPeriodTx.filter { it.type == TransactionType.INCOME }.sumOf { toMain(it) }
        val prevExpense = prevPeriodTx.filter { it.type == TransactionType.EXPENSE }.sumOf { toMain(it) }
        val expenseChangePercent = if (prevExpense > 0.0) {
            (((totalExpense - prevExpense) / prevExpense) * 100.0).toInt()
        } else {
            null
        }
        val savingsRatePercent = if (totalIncome > 0.0) {
            (((totalIncome - totalExpense) / totalIncome) * 100.0).toInt().coerceIn(-999, 100)
        } else {
            null
        }

        val breakdown = periodTx
            .filter { it.type == TransactionType.EXPENSE }
            .groupBy { it.category }
            .map { (cat, list) ->
                val sum = list.sumOf { toMain(it) }
                CategorySpend(cat, sum, if (totalExpense > 0) (sum / totalExpense).toFloat() else 0f)
            }
            .sortedByDescending { it.amount }

        val incomeBreakdown = periodTx
            .filter { it.type == TransactionType.INCOME }
            .groupBy { it.category }
            .map { (cat, list) ->
                val sum = list.sumOf { toMain(it) }
                CategorySpend(cat, sum, if (totalIncome > 0) (sum / totalIncome).toFloat() else 0f)
            }
            .sortedByDescending { it.amount }

        // Build trend buckets (up to 10 buckets or daily buckets for week/month) in active local timezone
        val dayFormat = SimpleDateFormat("dd", Locale.getDefault()).apply { timeZone = activeTz }
        val expenseByDayKey = periodTx
            .filter { it.type == TransactionType.EXPENSE }
            .groupBy { dayFormat.format(it.timestamp) }
            .mapValues { (_, list) -> list.sumOf { toMain(it) } }

        val rawPoints = mutableListOf<DailyTrendPoint>()
        if (filter == InsightTimeFilter.DAILY) {
            val labels = listOf("Morning", "Noon", "Evening", "Night")
            val hourSlices = listOf(0..11, 12..15, 16..19, 20..23)
            hourSlices.forEachIndexed { i, range ->
                val amt = periodTx.filter { tx ->
                    if (tx.type != TransactionType.EXPENSE) return@filter false
                    val c = Calendar.getInstance(activeTz).apply { timeInMillis = tx.timestamp }
                    c.get(Calendar.HOUR_OF_DAY) in range
                }.sumOf { toMain(it) }
                rawPoints += DailyTrendPoint(labels[i], amt)
            }
        } else {
            val totalDays = (((endCal.timeInMillis - startCal.timeInMillis + 43_200_000L) / 86_400_000L).toInt())
                .coerceIn(1, 31)
            val step = if (totalDays > 10) 3 else 1
            val iter = Calendar.getInstance(activeTz).apply { timeInMillis = startCal.timeInMillis }
            var dayIndex = 0
            while (dayIndex < totalDays) {
                var bucketSum = 0.0
                val label = dayFormat.format(iter.time)
                repeat(step) {
                    if (dayIndex < totalDays) {
                        val key = dayFormat.format(iter.time)
                        bucketSum += expenseByDayKey[key] ?: 0.0
                        iter.add(Calendar.DAY_OF_MONTH, 1)
                        dayIndex++
                    }
                }
                rawPoints += DailyTrendPoint(label, bucketSum)
            }
        }

        val maxAmt = rawPoints.maxOfOrNull { it.amount } ?: 0.0
        val pointsWithPeak = rawPoints.map { pt ->
            pt.copy(isPeak = maxAmt > 0.0 && pt.amount == maxAmt)
        }
        val avg = if (pointsWithPeak.isNotEmpty()) pointsWithPeak.sumOf { it.amount } / pointsWithPeak.size else 0.0

        return AnalyticsSnapshot(
            periodLabel = periodLabel,
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            netFlow = totalIncome - totalExpense,
            dailyAverage = avg,
            peakAmount = maxAmt,
            trendPoints = pointsWithPeak,
            categoryBreakdown = breakdown,
            previousPeriodExpense = prevExpense,
            previousPeriodIncome = prevIncome,
            expenseChangePercent = expenseChangePercent,
            savingsRatePercent = savingsRatePercent,
            topCategory = breakdown.firstOrNull(),
            transactionCount = periodTx.size,
            incomeBreakdown = incomeBreakdown
        )
    }

    private fun buildSmartAlerts(
        prefs: UserPrefs,
        accounts: List<AccountEntity>,
        transactions: List<TransactionEntity>,
        loans: List<LoanEntity>,
        spentThisMonth: Double,
        todaySpend: Double,
        timeContext: DeviceTimeContext
    ): List<SmartAlert> {
        val isBn = prefs.language == AppLanguage.BN
        val activeTz = timeContext.timeZone
        val nowMillis = timeContext.epochMillis
        val mainCur = prefs.mainCurrencyCode
        val alerts = mutableListOf<SmartAlert>()
        val seenIds = mutableSetOf<String>()

        fun addUnique(alert: SmartAlert) {
            if (seenIds.add(alert.id)) {
                alerts += alert
            }
        }

        // Strict zero-state discipline: emit only real, earned alerts based on actual user data.
        // 1. Immediate Monthly Budget Limit Alert (80% / 100%)
        if (prefs.notifBudgetAlerts && prefs.monthlyBudget > 0.0 && spentThisMonth > 0.0) {
            val budgetPct = ((spentThisMonth / prefs.monthlyBudget) * 100.0).toInt()
            if (budgetPct >= 80) {
                val pctStr = if (isBn) BdtFormatter.toBanglaDigits("$budgetPct%") else "$budgetPct%"
                val spentStr = BdtFormatter.formatCurrency(spentThisMonth, mainCur, prefs.language)
                val limitStr = BdtFormatter.formatCurrency(prefs.monthlyBudget, mainCur, prefs.language)
                addUnique(
                    SmartAlert(
                        id = "budget_high",
                        title = if (budgetPct >= 100) {
                            if (isBn) "মাসিক বাজেট সীমা ছাড়িয়েছে ($pctStr)" else "Monthly Budget Exceeded ($pctStr)"
                        } else {
                            if (isBn) "মাসিক বাজেট সতর্কতা ($pctStr)" else "Monthly Budget Alert ($pctStr)"
                        },
                        subtitle = if (isBn) {
                            "খরচ $spentStr / বাজেট $limitStr — বিস্তারিত দেখতে ট্যাপ করুন।"
                        } else {
                            "Spent $spentStr of $limitStr monthly budget. Tap to inspect Insights."
                        },
                        isWarning = true,
                        actionRoute = NotificationActionRoute.OPEN_INSIGHTS_BUDGET,
                        category = NotificationCategory.BUDGET_THRESHOLD,
                        timestampMillis = nowMillis
                    )
                )
            }
        }

        // 2. Credit Card Utilization Alert (> 30%)
        if (prefs.notifCreditAlerts) {
            accounts.filter { it.type == AccountType.CREDIT_CARD && it.creditLimit > 0.0 }.forEach { cc ->
                val util = cc.utilizationPercent
                if (util > 30) {
                    val utilStr = if (isBn) BdtFormatter.toBanglaDigits("$util%") else "$util%"
                    addUnique(
                        SmartAlert(
                            id = "cc_${cc.id}",
                            title = if (isBn) "${cc.name} ক্রেডিট ব্যবহার $utilStr" else "${cc.name} Utilization at $utilStr",
                            subtitle = if (isBn) {
                                "সুস্থ ক্রেডিট স্কোরের জন্য ৩০%-এর নিচে রাখুন। কার্ড দেখতে ট্যাপ করুন।"
                            } else {
                                "Keep utilization under 30% for optimal credit health. Tap to view Accounts."
                            },
                            isWarning = true,
                            actionRoute = NotificationActionRoute.OPEN_ACCOUNTS,
                            category = NotificationCategory.HIGH_SPEND_CARD,
                            timestampMillis = nowMillis
                        )
                    )
                }
            }
        }

        // 3. High Daily Spend Alert (when real daily spending exceeds user's daily pace or custom limit)
        if (prefs.notifHighSpendAlertEnabled && todaySpend > 0.0) {
            val daysInMonth = Calendar.getInstance(activeTz).apply { timeInMillis = nowMillis }
                .getActualMaximum(Calendar.DAY_OF_MONTH)
                .coerceAtLeast(28)
            val dailyLimit = when {
                prefs.notifDailySpendLimit > 0.0 -> prefs.notifDailySpendLimit
                prefs.monthlyBudget > 0.0 -> (prefs.monthlyBudget / daysInMonth) * 1.5
                else -> 0.0
            }
            if (dailyLimit > 0.0 && todaySpend >= dailyLimit) {
                val spentStr = BdtFormatter.formatCurrency(todaySpend, mainCur, prefs.language)
                val limitStr = BdtFormatter.formatCurrency(dailyLimit, mainCur, prefs.language)
                addUnique(
                    SmartAlert(
                        id = "high_daily_spend",
                        title = if (isBn) "আজকের খরচ দৈনিক সীমার উপরে" else "High Daily Spend Today",
                        subtitle = if (isBn) {
                            "আজ $spentStr খরচ হয়েছে (লক্ষ্য $limitStr)। ইনসাইটস দেখতে ট্যাপ করুন।"
                        } else {
                            "Spent $spentStr today vs $limitStr daily pace. Tap to review."
                        },
                        isWarning = true,
                        actionRoute = NotificationActionRoute.OPEN_INSIGHTS_BUDGET,
                        category = NotificationCategory.HIGH_SPEND_CARD,
                        timestampMillis = nowMillis
                    )
                )
            }
        }

        // 4. Dues, Loans & Planned Bills (Overdue, Due Today, or Due Tomorrow in exact loan currency)
        if (prefs.notifLoanReminders) {
            loans.filter { !it.isSettled && it.remainingAmount > 0.0 }.forEach { loan ->
                val dueMillis = loan.dueDateMillis ?: return@forEach
                val daysUntil = NotificationTimeCalculator.localCalendarDaysUntil(dueMillis, nowMillis, activeTz)
                val shouldShow = daysUntil <= 0 || (daysUntil == 1 && prefs.notifLoanDayBeforeEnabled)
                if (shouldShow) {
                    val amtStr = BdtFormatter.formatCurrency(loan.remainingAmount, loan.currencyCode, prefs.language)
                    val kindLabel = when (loan.loanType) {
                        LoanType.I_OWE -> if (isBn) "দেনা" else "Payable"
                        LoanType.OWED_TO_ME -> if (isBn) "পাওনা" else "Receivable"
                    }
                    val title = when {
                        daysUntil < 0 -> if (isBn) "মেয়াদোত্তীর্ণ $kindLabel: ${loan.personName}" else "Overdue $kindLabel: ${loan.personName}"
                        daysUntil == 0 -> if (isBn) "আজ নির্ধারিত $kindLabel: ${loan.personName}" else "Due Today ($kindLabel): ${loan.personName}"
                        else -> if (isBn) "আগামীকাল নির্ধারিত $kindLabel: ${loan.personName}" else "Due Tomorrow ($kindLabel): ${loan.personName}"
                    }
                    val subtitle = if (isBn) {
                        "বাকি $amtStr • সকাল ১০:০০টা রিমাইন্ডার। পরিশোধ করতে ট্যাপ করুন।"
                    } else {
                        "Remaining $amtStr • 10:00 AM reminder. Tap to open Dues & Loans."
                    }
                    addUnique(
                        SmartAlert(
                            id = "loan_due_${loan.id}",
                            title = title,
                            subtitle = subtitle,
                            isWarning = daysUntil <= 0,
                            actionRoute = NotificationActionRoute.OPEN_PLAN_LOANS,
                            category = NotificationCategory.DUE_LOAN,
                            timestampMillis = dueMillis
                        )
                    )
                }
            }
        }

        // 5. Persisted System-Delivered Notification History (keeps system bar & in-app center in sync)
        val loggedItems = NotificationLogItem.decodeListFromJson(prefs.notificationLogJson)
        loggedItems.forEach { item ->
            addUnique(
                SmartAlert(
                    id = item.id,
                    title = item.title,
                    subtitle = item.body,
                    isWarning = item.isWarning,
                    actionRoute = item.route,
                    category = item.category,
                    timestampMillis = item.timestampMillis.takeIf { it > 0L }
                )
            )
        }

        return alerts
    }
}
