package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.GoogleAccountProfile
import com.example.core.GoogleAuthManager
import com.example.core.NetworkConnectivityMonitor
import com.example.data.ai.GeminiAiService
import com.example.data.ai.InsightsSummaryHelper
import com.example.data.ai.MarketErrorCategory
import com.example.data.ai.TtsAudioPlayer
import com.example.data.backup.BackupManager
import com.example.data.local.AccountEntity
import com.example.data.local.AppDatabase
import com.example.data.local.CategoryBudgetEntity
import com.example.data.local.LoanEntity
import com.example.data.local.SavingsGoalEntity
import com.example.data.local.TransactionEntity
import com.example.data.local.UserPreferencesRepository
import com.example.data.local.UserPrefs
import com.example.data.market.MarketDataRepository
import com.example.data.market.MarketSyncUiState
import com.example.data.model.AccountType
import com.example.data.model.AppColourTheme
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
import com.example.localization.AppLanguage
import com.example.localization.BdtFormatter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

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
    val transactionCount: Int = 0
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
    val netWorth: NetWorthSummary = NetWorthSummary(),
    val spentThisMonth: Double = 0.0,
    val incomeThisMonth: Double = 0.0,
    val dailyAverageSpend: Double = 0.0,
    val todaySpend: Double = 0.0,
    val streakDays: Int = 1,
    val topCategories: List<CategorySpend> = emptyList(),
    val categorySpentMap: Map<ExpenseCategory, Double> = emptyMap(),
    val smartAlerts: List<SmartAlert> = emptyList(),
    val selectedTab: AppTab = AppTab.HOME,
    val showAccountsScreen: Boolean = false,
    val showSettingsScreen: Boolean = false,
    val insightFilter: InsightTimeFilter = InsightTimeFilter.THIS_MONTH,
    val insightMonthOffset: Int = 0,
    val analytics: AnalyticsSnapshot = AnalyticsSnapshot()
)

sealed interface ReceiptScanUiState {
    data object Idle : ReceiptScanUiState
    data object Loading : ReceiptScanUiState
    data class Success(val result: ReceiptScanResult) : ReceiptScanUiState
    data class Error(val message: String) : ReceiptScanUiState
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
    data class Playing(val script: String, val progress: Float = 0f) : TtsUiState
    data class Paused(val script: String, val progress: Float = 0f) : TtsUiState
    data class Error(
        val message: String,
        val category: MarketErrorCategory = MarketErrorCategory.GENERAL
    ) : TtsUiState
}

data class BackupMessage(val text: String, val isError: Boolean = false)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    private val repository = FinanceRepository(db)
    private val prefsRepo = UserPreferencesRepository(application)
    private val aiService = GeminiAiService()
    private val ttsPlayer = TtsAudioPlayer(application)
    private val connectivityMonitor = NetworkConnectivityMonitor(application)
    private val marketRepository = MarketDataRepository(application)

    private val _selectedTab = MutableStateFlow(AppTab.HOME)
    private val _showAccountsScreen = MutableStateFlow(false)
    private val _showSettingsScreen = MutableStateFlow(false)
    private val _insightFilter = MutableStateFlow(InsightTimeFilter.THIS_MONTH)
    private val _insightMonthOffset = MutableStateFlow(0)

    private val _isAppUnlocked = MutableStateFlow(false)
    val isAppUnlocked: StateFlow<Boolean> = _isAppUnlocked.asStateFlow()

    private var lastBackgroundedAtMillis: Long = 0L

    private val _backupStatus = MutableStateFlow<BackupMessage?>(null)
    val backupStatus: StateFlow<BackupMessage?> = _backupStatus.asStateFlow()

    private val _receiptState = MutableStateFlow<ReceiptScanUiState>(ReceiptScanUiState.Idle)
    val receiptState: StateFlow<ReceiptScanUiState> = _receiptState.asStateFlow()

    private val _searchAiState = MutableStateFlow<SearchAiUiState>(SearchAiUiState.Idle)
    val searchAiState: StateFlow<SearchAiUiState> = _searchAiState.asStateFlow()

    private val _voiceState = MutableStateFlow<VoiceParseUiState>(VoiceParseUiState.Idle)
    val voiceState: StateFlow<VoiceParseUiState> = _voiceState.asStateFlow()

    private val _ttsState = MutableStateFlow<TtsUiState>(TtsUiState.Idle)
    val ttsState: StateFlow<TtsUiState> = _ttsState.asStateFlow()

    private val _isMarketSyncing = MutableStateFlow(false)
    val marketSyncState: StateFlow<MarketSyncUiState> = combine(
        marketRepository.snapshotFlow,
        connectivityMonitor.isOnline,
        _isMarketSyncing
    ) { snapshot, isOnline, isSyncing ->
        MarketSyncUiState(
            snapshot = snapshot,
            isOnline = isOnline,
            isSyncing = isSyncing
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MarketSyncUiState(isOnline = connectivityMonitor.isOnline.value)
    )

    init {
        connectivityMonitor.start()
        // Automatically sync live exchange rates & gold benchmarks at least once per day
        // and whenever internet connectivity is restored.
        viewModelScope.launch {
            connectivityMonitor.isOnline.collect { online ->
                if (online) {
                    val cached = marketRepository.getCachedSnapshot()
                    if (cached.isStale()) {
                        refreshMarketRates(force = false)
                    }
                }
            }
        }
    }

    fun refreshMarketRates(force: Boolean = true) {
        if (_isMarketSyncing.value) return
        val online = connectivityMonitor.refreshNow()
        if (!online) return
        viewModelScope.launch {
            if (!force) {
                val cached = marketRepository.getCachedSnapshot()
                if (!cached.isStale()) return@launch
            }
            _isMarketSyncing.value = true
            try {
                marketRepository.syncLiveMarketRates()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Market rate auto-sync fell back to cache", e)
            } finally {
                _isMarketSyncing.value = false
            }
        }
    }

    private data class PendingDashboardLayout(
        val layoutStyle: HomeLayoutStyle,
        val order: List<HomeSectionId>,
        val hidden: Set<HomeSectionId>
    )

    private val _pendingDashboardLayout = MutableStateFlow<PendingDashboardLayout?>(null)

    private val navAndFilterFlow = combine(
        _selectedTab,
        _showAccountsScreen,
        _showSettingsScreen,
        _insightFilter,
        _insightMonthOffset
    ) { tab, showAccounts, showSettings, filter, monthOffset ->
        NavFilterState(tab, showAccounts, showSettings, filter, monthOffset)
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

    val uiState: StateFlow<MainUiState> = combine(
        prefsRepo.userPrefs,
        _pendingDashboardLayout,
        dbFlow,
        navAndFilterFlow
    ) { prefs, pendingLayout, dbData, nav ->
        val effectivePrefs = if (pendingLayout != null) {
            prefs.copy(
                homeLayoutStyle = pendingLayout.layoutStyle,
                homeSectionOrder = pendingLayout.order,
                hiddenHomeSections = pendingLayout.hidden
            )
        } else {
            prefs
        }
        buildMainUiState(effectivePrefs, dbData, nav)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MainUiState()
    )

    fun selectTab(tab: AppTab) {
        _showAccountsScreen.value = false
        _showSettingsScreen.value = false
        _selectedTab.value = tab
    }

    fun openAccountsScreen() {
        _showSettingsScreen.value = false
        _showAccountsScreen.value = true
    }

    fun closeAccountsScreen() {
        _showAccountsScreen.value = false
    }

    fun openSettingsScreen() {
        _showAccountsScreen.value = false
        _showSettingsScreen.value = true
    }

    fun closeSettingsScreen() {
        _showSettingsScreen.value = false
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
    }

    fun updateDisplayName(name: String) = viewModelScope.launch {
        prefsRepo.updateDisplayName(name)
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

    fun linkGoogleProfile(profile: GoogleAccountProfile) = viewModelScope.launch {
        prefsRepo.linkGoogleAccount(
            displayName = profile.displayName,
            email = profile.email,
            avatarUrl = profile.avatarUrl
        )
    }

    fun unlinkGoogleAccount() = viewModelScope.launch {
        GoogleAuthManager.signOut(getApplication())
        prefsRepo.unlinkGoogleAccount()
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

    fun setHapticFeedback(enabled: Boolean) = viewModelScope.launch {
        prefsRepo.setHapticFeedback(enabled)
    }

    fun setNotificationPreferences(budget: Boolean, credit: Boolean, loan: Boolean) = viewModelScope.launch {
        prefsRepo.setNotificationPreferences(budget, credit, loan)
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
    }

    fun deleteManagedCategory(categoryId: String, isBuiltIn: Boolean) = viewModelScope.launch {
        prefsRepo.deleteManagedCategory(categoryId, isBuiltIn)
    }

    fun resetCategoriesToDefault() = viewModelScope.launch {
        prefsRepo.resetCategoriesToDefault()
    }

    fun addAccount(account: AccountEntity) = viewModelScope.launch {
        repository.addAccount(account)
    }

    fun deleteAccount(accountId: Long) = viewModelScope.launch {
        repository.deleteAccount(accountId)
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
    }

    fun transferFunds(
        fromAccountId: Long,
        toAccountId: Long,
        amount: Double,
        note: String,
        timestamp: Long = System.currentTimeMillis()
    ) = viewModelScope.launch {
        repository.transferFunds(fromAccountId, toAccountId, amount, note, timestamp)
    }

    fun deleteTransaction(id: Long) = viewModelScope.launch {
        repository.deleteTransaction(id)
    }

    fun addSavingsGoal(name: String, target: Double, dueDateMillis: Long?, colorArgb: Long) =
        viewModelScope.launch {
            repository.addSavingsGoal(name, target, dueDateMillis, colorArgb)
        }

    fun fundGoalFromWallet(goal: SavingsGoalEntity, amount: Double, walletId: Long) =
        viewModelScope.launch {
            repository.fundGoalFromWallet(goal, amount, walletId)
        }

    fun deleteGoal(goalId: Long) = viewModelScope.launch {
        repository.deleteGoal(goalId)
    }

    fun addLoan(
        personName: String,
        loanType: LoanType,
        totalAmount: Double,
        note: String,
        dueDateMillis: Long?
    ) = viewModelScope.launch {
        repository.addLoan(personName, loanType, totalAmount, note, dueDateMillis)
    }

    fun settleLoan(loan: LoanEntity, amount: Double, walletId: Long) = viewModelScope.launch {
        repository.settleLoan(loan, amount, walletId)
    }

    fun deleteLoan(loanId: Long) = viewModelScope.launch {
        repository.deleteLoan(loanId)
    }

    fun upsertCategoryBudget(category: ExpenseCategory, limitAmount: Double) = viewModelScope.launch {
        repository.upsertCategoryBudget(category, limitAmount)
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

    fun restoreJsonFromUri(uri: Uri, successMessage: String) {
        viewModelScope.launch {
            try {
                val jsonString = withContext(Dispatchers.IO) {
                    getApplication<Application>().contentResolver.openInputStream(uri)?.bufferedReader()
                        ?.use { it.readText() }
                        ?: throw IllegalStateException("Cannot read backup file.")
                }
                withContext(Dispatchers.IO) {
                    BackupManager.restoreFromJson(jsonString, db, prefsRepo)
                }
                _pendingDashboardLayout.value = null
                _backupStatus.value = BackupMessage(successMessage)
            } catch (e: Exception) {
                Log.e(TAG, "JSON Restore failed", e)
                _backupStatus.value = BackupMessage(
                    e.localizedMessage ?: "Invalid backup JSON file.",
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

    // --- Gemini Vision: Receipt Scan (gemini-4-argon) ---
    fun analyzeReceiptUri(uri: Uri) {
        viewModelScope.launch {
            _receiptState.value = ReceiptScanUiState.Loading
            try {
                val bitmap = withContext(Dispatchers.IO) { loadBitmapFromUri(uri) }
                val result = aiService.analyzeReceiptImage(bitmap, uiState.value.prefs.language)
                _receiptState.value = ReceiptScanUiState.Success(result)
            } catch (e: TimeoutCancellationException) {
                _receiptState.value = ReceiptScanUiState.Error("Receipt scan timed out.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Receipt analysis failed", e)
                _receiptState.value = ReceiptScanUiState.Error(e.localizedMessage ?: "Receipt scan failed.")
            }
        }
    }

    fun resetReceiptScan() {
        _receiptState.value = ReceiptScanUiState.Idle
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

    // --- Gemini Voice / Text Entry Parser ---
    fun parseVoiceEntry(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            _voiceState.value = VoiceParseUiState.Loading
            try {
                val parsed = aiService.parseVoiceOrTextEntry(text.trim(), uiState.value.prefs.language)
                _voiceState.value = VoiceParseUiState.Success(parsed)
            } catch (e: TimeoutCancellationException) {
                _voiceState.value = VoiceParseUiState.Error("Voice entry timed out.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Voice parse failed", e)
                _voiceState.value = VoiceParseUiState.Error(e.localizedMessage ?: "Voice parse failed.")
            }
        }
    }

    fun resetVoiceParse() {
        _voiceState.value = VoiceParseUiState.Idle
    }

    // --- Gemini TTS Briefing (gemini-3.1-flash-tts-preview) ---
    fun playFinancialBriefingTts() {
        val snapshot = uiState.value
        val lang = snapshot.prefs.language
        val script = InsightsSummaryHelper.buildPeriodSummary(snapshot)

        viewModelScope.launch {
            _ttsState.value = TtsUiState.Generating
            try {
                val payload = aiService.generateBriefingTts(script)
                _ttsState.value = TtsUiState.Playing(payload.spokenScript, progress = 0f)
                ttsPlayer.playAudio(
                    audioBytes = payload.audioBytes,
                    mimeType = payload.mimeType,
                    onProgress = { pct ->
                        when (val curr = _ttsState.value) {
                            is TtsUiState.Playing -> _ttsState.value = curr.copy(progress = pct)
                            is TtsUiState.Paused -> _ttsState.value = curr.copy(progress = pct)
                            else -> Unit
                        }
                    },
                    onComplete = { _ttsState.value = TtsUiState.Idle },
                    onError = { msg ->
                        Log.e(TAG, "TTS playback error: $msg")
                        val sanitized = InsightsSummaryHelper.sanitizeAiError(
                            rawMessage = msg,
                            language = lang,
                            isVoice = true
                        )
                        _ttsState.value = TtsUiState.Error(sanitized.message, sanitized.category)
                    }
                )
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "TTS timed out", e)
                val sanitized = InsightsSummaryHelper.sanitizeAiError(
                    rawMessage = "timeout",
                    throwable = e,
                    language = lang,
                    isVoice = true
                )
                _ttsState.value = TtsUiState.Error(sanitized.message, sanitized.category)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "TTS failed", e)
                val sanitized = InsightsSummaryHelper.sanitizeAiError(
                    rawMessage = e.localizedMessage,
                    throwable = e,
                    language = lang,
                    isVoice = true
                )
                _ttsState.value = TtsUiState.Error(sanitized.message, sanitized.category)
            }
        }
    }

    fun pauseTtsPlayback() {
        val current = _ttsState.value
        if (current is TtsUiState.Playing) {
            ttsPlayer.pause()
            _ttsState.value = TtsUiState.Paused(current.script, current.progress)
        }
    }

    fun resumeTtsPlayback() {
        val current = _ttsState.value
        if (current is TtsUiState.Paused) {
            ttsPlayer.resume()
            _ttsState.value = TtsUiState.Playing(current.script, current.progress)
        }
    }

    fun stopTtsPlayback() {
        ttsPlayer.stop()
        _ttsState.value = TtsUiState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        ttsPlayer.stop()
        connectivityMonitor.stop()
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

    private data class NavFilterState(
        val tab: AppTab,
        val showAccounts: Boolean,
        val showSettings: Boolean,
        val filter: InsightTimeFilter,
        val monthOffset: Int
    )

    private fun buildMainUiState(
        prefs: UserPrefs,
        dbData: DbSnapshot,
        nav: NavFilterState
    ): MainUiState {
        val accounts = dbData.accounts
        val transactions = dbData.transactions

        val assets = accounts
            .filter { it.type != AccountType.CREDIT_CARD }
            .sumOf { it.balance.coerceAtLeast(0.0) }
        val liabilities = accounts
            .filter { it.type == AccountType.CREDIT_CARD }
            .sumOf { it.balance.coerceAtLeast(0.0) }

        val cal = Calendar.getInstance()
        val currentMonth = cal.get(Calendar.MONTH)
        val currentYear = cal.get(Calendar.YEAR)
        val currentDay = cal.get(Calendar.DAY_OF_MONTH).coerceAtLeast(1)

        val monthTx = transactions.filter { tx ->
            val txCal = Calendar.getInstance().apply { timeInMillis = tx.timestamp }
            txCal.get(Calendar.MONTH) == currentMonth && txCal.get(Calendar.YEAR) == currentYear
        }
        val spentThisMonth = monthTx.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val incomeThisMonth = monthTx.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val dailyAverage = if (spentThisMonth > 0) spentThisMonth / currentDay else 1200.0

        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val todaySpend = transactions
            .filter { it.type == TransactionType.EXPENSE && it.timestamp >= todayStart }
            .sumOf { it.amount }

        var streak = 0
        val dayCap = maxOf(dailyAverage * 1.15, prefs.monthlyBudget / 30.0)
        for (offset in 0..29) {
            val dayStart = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_MONTH, -offset)
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val daySpent = transactions
                .filter { it.type == TransactionType.EXPENSE && it.timestamp in dayStart until (dayStart + 86_400_000L) }
                .sumOf { it.amount }
            if (daySpent <= dayCap) streak++ else break
        }

        val catSpentMap = monthTx
            .filter { it.type == TransactionType.EXPENSE }
            .groupBy { it.category }
            .mapValues { (_, list) -> list.sumOf { it.amount } }

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
            startOfWeek = prefs.startOfWeek
        )

        val alerts = buildSmartAlerts(prefs, accounts, dbData.loans, spentThisMonth, streak)

        return MainUiState(
            isPrefsLoaded = true,
            prefs = prefs,
            accounts = accounts,
            transactions = transactions,
            goals = dbData.goals,
            loans = dbData.loans,
            categoryBudgets = dbData.categoryBudgets,
            totalLiquidBalance = assets,
            netWorth = NetWorthSummary(assets, liabilities),
            spentThisMonth = spentThisMonth,
            incomeThisMonth = incomeThisMonth,
            dailyAverageSpend = dailyAverage,
            todaySpend = todaySpend,
            streakDays = streak.coerceAtLeast(1),
            topCategories = topCategories,
            categorySpentMap = catSpentMap,
            smartAlerts = alerts,
            selectedTab = nav.tab,
            showAccountsScreen = nav.showAccounts,
            showSettingsScreen = nav.showSettings,
            insightFilter = nav.filter,
            insightMonthOffset = nav.monthOffset,
            analytics = analytics
        )
    }

    private fun computeAnalytics(
        transactions: List<TransactionEntity>,
        filter: InsightTimeFilter,
        monthOffset: Int,
        startOfWeek: StartOfWeek
    ): AnalyticsSnapshot {
        val startCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val endCal = Calendar.getInstance()

        val periodLabel: String
        when (filter) {
            InsightTimeFilter.DAILY -> {
                endCal.timeInMillis = startCal.timeInMillis + 86_400_000L
                periodLabel = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(startCal.time)
            }
            InsightTimeFilter.THIS_WEEK -> {
                startCal.firstDayOfWeek = startOfWeek.calendarDay
                while (startCal.get(Calendar.DAY_OF_WEEK) != startOfWeek.calendarDay) {
                    startCal.add(Calendar.DAY_OF_MONTH, -1)
                }
                endCal.timeInMillis = startCal.timeInMillis + 7 * 86_400_000L
                val fmt = SimpleDateFormat("dd MMM", Locale.getDefault())
                periodLabel = "${fmt.format(startCal.time)} – ${fmt.format(endCal.timeInMillis - 1)}"
            }
            InsightTimeFilter.LAST_MONTH -> {
                startCal.add(Calendar.MONTH, -1)
                startCal.set(Calendar.DAY_OF_MONTH, 1)
                endCal.timeInMillis = startCal.timeInMillis
                endCal.add(Calendar.MONTH, 1)
                periodLabel = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(startCal.time)
            }
            InsightTimeFilter.THIS_MONTH -> {
                startCal.add(Calendar.MONTH, monthOffset)
                startCal.set(Calendar.DAY_OF_MONTH, 1)
                endCal.timeInMillis = startCal.timeInMillis
                endCal.add(Calendar.MONTH, 1)
                periodLabel = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(startCal.time)
            }
        }

        val prevStartCal = Calendar.getInstance().apply { timeInMillis = startCal.timeInMillis }
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
        val totalIncome = periodTx.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val totalExpense = periodTx.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val prevIncome = prevPeriodTx.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val prevExpense = prevPeriodTx.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
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
                val sum = list.sumOf { it.amount }
                CategorySpend(cat, sum, if (totalExpense > 0) (sum / totalExpense).toFloat() else 0f)
            }
            .sortedByDescending { it.amount }

        // Build trend buckets (up to 10 buckets or daily buckets for week/month)
        val dayFormat = SimpleDateFormat("dd", Locale.getDefault())
        val expenseByDayKey = periodTx
            .filter { it.type == TransactionType.EXPENSE }
            .groupBy { dayFormat.format(it.timestamp) }
            .mapValues { (_, list) -> list.sumOf { it.amount } }

        val rawPoints = mutableListOf<DailyTrendPoint>()
        if (filter == InsightTimeFilter.DAILY) {
            val labels = listOf("Morning", "Noon", "Evening", "Night")
            val hourSlices = listOf(0..11, 12..15, 16..19, 20..23)
            hourSlices.forEachIndexed { i, range ->
                val amt = periodTx.filter { tx ->
                    if (tx.type != TransactionType.EXPENSE) return@filter false
                    val c = Calendar.getInstance().apply { timeInMillis = tx.timestamp }
                    c.get(Calendar.HOUR_OF_DAY) in range
                }.sumOf { it.amount }
                rawPoints += DailyTrendPoint(labels[i], amt)
            }
        } else {
            val totalDays = (((endCal.timeInMillis - startCal.timeInMillis) / 86_400_000L).toInt())
                .coerceIn(1, 31)
            val step = if (totalDays > 10) 3 else 1
            val iter = Calendar.getInstance().apply { timeInMillis = startCal.timeInMillis }
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
            transactionCount = periodTx.size
        )
    }

    private fun buildSmartAlerts(
        prefs: UserPrefs,
        accounts: List<AccountEntity>,
        loans: List<LoanEntity>,
        spentThisMonth: Double,
        streakDays: Int
    ): List<SmartAlert> {
        val isBn = prefs.language == AppLanguage.BN
        val alerts = mutableListOf<SmartAlert>()
        val budgetPct = if (prefs.monthlyBudget > 0) ((spentThisMonth / prefs.monthlyBudget) * 100).toInt() else 0

        if (prefs.notifBudgetAlerts) {
            if (budgetPct >= 80) {
                alerts += SmartAlert(
                    id = "budget_high",
                    title = if (isBn) "মাসিক বাজেট সতর্কতা ($budgetPct%)" else "Monthly Budget Alert ($budgetPct%)",
                    subtitle = if (isBn) "আপনি নির্ধারিত বাজেটের কাছাকাছি পৌঁছেছেন।" else "You have utilized $budgetPct% of your monthly budget.",
                    isWarning = true
                )
            } else {
                alerts += SmartAlert(
                    id = "budget_ok",
                    title = if (isBn) "বাজেট নিয়ন্ত্রণে আছে ($budgetPct%)" else "Budget On Track ($budgetPct%)",
                    subtitle = if (isBn) "চলতি মাসে আপনার খরচ স্বাভাবিক সীমায় রয়েছে।" else "Your monthly spending pace looks healthy.",
                    isWarning = false
                )
            }
        }

        if (prefs.notifCreditAlerts) {
            accounts.filter { it.type == AccountType.CREDIT_CARD }.forEach { cc ->
                val util = cc.utilizationPercent
                if (util > 30) {
                    alerts += SmartAlert(
                        id = "cc_${cc.id}",
                        title = if (isBn) "${cc.name} ক্রেডিট ব্যবহার $util%" else "${cc.name} Utilization at $util%",
                        subtitle = if (isBn) "সুস্থ ক্রেডিট স্কোরের জন্য ৩০%-এর নিচে রাখুন।" else "Keep utilization under 30% for optimal credit health.",
                        isWarning = true
                    )
                }
            }
        }

        if (prefs.notifLoanReminders) {
            val pendingLoans = loans.count { !it.isSettled }
            if (pendingLoans > 0) {
                alerts += SmartAlert(
                    id = "loans_pending",
                    title = if (isBn) "$pendingLoans টি দেনা-পাওনা বাকি আছে" else "$pendingLoans Pending Loans / Debts",
                    subtitle = if (isBn) "পরিকল্পনা ট্যাবে যেয়ে আংশিক বা সম্পূর্ণ পরিশোধ করুন।" else "Track or settle partially from the Plan tab.",
                    isWarning = false
                )
            }
        }

        alerts += SmartAlert(
            id = "streak_info",
            title = if (isBn) "$streakDays দিনের খরচ নিয়ন্ত্রণ ধারা!" else "$streakDays-Day Spending Streak!",
            subtitle = if (isBn) "সব হিসাব ১০০% অফলাইনে নিরাপদে সংরক্ষিত আছে।" else "All vault records remain 100% offline on this device.",
            isWarning = false
        )
        return alerts
    }
}
