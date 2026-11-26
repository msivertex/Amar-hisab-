package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.core.ProfilePhotoUrlNormalizer
import com.example.data.model.AppColourTheme
import com.example.data.model.AppCountry
import com.example.data.model.AppCurrency
import com.example.data.model.AppThemeMode
import com.example.data.model.AuthMode
import com.example.data.model.BuiltInCategoriesCatalog
import com.example.data.model.CategorySortMode
import com.example.data.model.HomeLayoutStyle
import com.example.data.model.HomeSectionId
import com.example.data.model.ManagedCategoryItem
import com.example.data.model.StartOfWeek
import com.example.data.model.TransactionType
import com.example.localization.AppLanguage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "amar_hisab_prefs")

data class UserPrefs(
    val language: AppLanguage = AppLanguage.EN,
    val isDarkTheme: Boolean = false,
    val themeMode: AppThemeMode = AppThemeMode.LIGHT,
    val colourTheme: AppColourTheme = AppColourTheme.SKY_CYAN,
    val monthlyBudget: Double = 0.0,
    val userName: String = "",
    val userEmail: String = "",
    val userAvatarUrl: String = "",
    val guestAvatarUrl: String = "",
    val googleCustomAvatarUrl: String = "",
    val authMode: AuthMode = AuthMode.GUEST,
    val hasCompletedWelcome: Boolean = false,
    val hasRequestedLaunchPermissions: Boolean = false,
    val enableSubcategories: Boolean = true,
    val autoOpenKeypad: Boolean = true,
    val defaultAccountId: Long = 0L,
    val startOfWeek: StartOfWeek = StartOfWeek.SATURDAY,
    val hapticFeedbackEnabled: Boolean = true,
    val notifBudgetAlerts: Boolean = true,
    val notifCreditAlerts: Boolean = true,
    val notifLoanReminders: Boolean = true,
    val appLockEnabled: Boolean = false,
    val appLockPin: String = "",
    val biometricUnlockEnabled: Boolean = false,
    val homeSectionOrder: List<HomeSectionId> = HomeSectionId.entries,
    val hiddenHomeSections: Set<HomeSectionId> = emptySet(),
    val homeLayoutStyle: HomeLayoutStyle = HomeLayoutStyle.WALLET_STACK,
    val categoryOrderExpense: List<String> = emptyList(),
    val categoryOrderIncome: List<String> = emptyList(),
    val categoryOrderTransfer: List<String> = emptyList(),
    val categorySortMode: CategorySortMode = CategorySortMode.DEFAULT,
    val managedCategoryOverrides: List<ManagedCategoryItem> = emptyList(),
    val deletedCategoryIds: Set<String> = emptySet(),
    val mainCurrencyCode: String = "BDT",
    val secondaryCurrencyCode: String = "",
    val userCountryCode: String = "BD",
    val notifDailyReminderEnabled: Boolean = true,
    val notifDailyReminderHour: Int = 21,
    val notifDailyReminderMinute: Int = 0,
    val notifLoanDayBeforeEnabled: Boolean = true,
    val notifLoanReminderHour: Int = 10,
    val notifLoanReminderMinute: Int = 0,
    val notifHighSpendAlertEnabled: Boolean = true,
    val notifDailySpendLimit: Double = 0.0,
    val notificationLogJson: String = "",
    val driveLastSyncedAtMillis: Long = 0L,
    val drivePendingSync: Boolean = false,
    val driveLastLocalModifiedAtMillis: Long = 0L,
    val driveFolderLinked: Boolean = false,
    val lastLocalOwnerEmail: String = ""
) {
    val userCountry: AppCountry
        get() = AppCountry.fromCode(userCountryCode)

    val mainCurrency: AppCurrency
        get() = AppCurrency.fromCode(mainCurrencyCode)

    val secondaryCurrency: AppCurrency?
        get() = AppCurrency.fromCodeOrNull(secondaryCurrencyCode)
            ?.takeIf { it != mainCurrency }

    val isLaunchLockActive: Boolean
        get() = appLockEnabled || biometricUnlockEnabled

    fun categoryOrderFor(type: TransactionType): List<String> = when (type) {
        TransactionType.EXPENSE -> categoryOrderExpense
        TransactionType.INCOME -> categoryOrderIncome
        TransactionType.TRANSFER -> categoryOrderTransfer
    }

    val effectiveAvatarUrl: String
        get() = when (authMode) {
            AuthMode.GOOGLE -> googleCustomAvatarUrl.ifBlank {
                ProfilePhotoUrlNormalizer.toHighResUrl(userAvatarUrl)
            }
            AuthMode.GUEST -> guestAvatarUrl
        }

    val hasCustomAvatarOverride: Boolean
        get() = when (authMode) {
            AuthMode.GOOGLE -> googleCustomAvatarUrl.isNotBlank()
            AuthMode.GUEST -> guestAvatarUrl.isNotBlank()
        }
}

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val LANGUAGE = stringPreferencesKey("language_code")
        val IS_DARK_THEME = booleanPreferencesKey("is_dark_theme")
        val MONTHLY_BUDGET = doublePreferencesKey("monthly_budget")
        val USER_NAME = stringPreferencesKey("user_name")
        val USER_EMAIL = stringPreferencesKey("user_email")
        val USER_AVATAR_URL = stringPreferencesKey("user_avatar_url")
        val GUEST_AVATAR_URL = stringPreferencesKey("guest_avatar_url")
        val GOOGLE_CUSTOM_AVATAR_URL = stringPreferencesKey("google_custom_avatar_url")
        val AUTH_MODE = stringPreferencesKey("auth_mode")
        val HAS_COMPLETED_WELCOME = booleanPreferencesKey("has_completed_welcome")
        val HAS_REQUESTED_LAUNCH_PERMISSIONS = booleanPreferencesKey("has_requested_launch_permissions")
        val ENABLE_SUBCATEGORIES = booleanPreferencesKey("enable_subcategories")
        val AUTO_OPEN_KEYPAD = booleanPreferencesKey("auto_open_keypad")
        val DEFAULT_ACCOUNT_ID = longPreferencesKey("default_account_id")
        val START_OF_WEEK = stringPreferencesKey("start_of_week")
        val HAPTIC_ENABLED = booleanPreferencesKey("haptic_feedback_enabled")
        val NOTIF_BUDGET = booleanPreferencesKey("notif_budget_alerts")
        val NOTIF_CREDIT = booleanPreferencesKey("notif_credit_alerts")
        val NOTIF_LOAN = booleanPreferencesKey("notif_loan_reminders")
        val APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")
        val APP_LOCK_PIN = stringPreferencesKey("app_lock_pin")
        val BIOMETRIC_UNLOCK_ENABLED = booleanPreferencesKey("biometric_unlock_enabled")
        val HOME_SECTION_ORDER = stringPreferencesKey("home_section_order")
        val HIDDEN_HOME_SECTIONS = stringPreferencesKey("hidden_home_sections")
        val HOME_LAYOUT_STYLE = stringPreferencesKey("home_layout_style")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val COLOUR_THEME = stringPreferencesKey("colour_theme")
        val CATEGORY_ORDER_EXPENSE = stringPreferencesKey("category_order_expense")
        val CATEGORY_ORDER_INCOME = stringPreferencesKey("category_order_income")
        val CATEGORY_ORDER_TRANSFER = stringPreferencesKey("category_order_transfer")
        val CATEGORY_SORT_MODE = stringPreferencesKey("category_sort_mode")
        val MANAGED_CATEGORIES_JSON = stringPreferencesKey("managed_categories_json")
        val DELETED_CATEGORY_IDS = stringPreferencesKey("deleted_category_ids")
        val MAIN_CURRENCY_CODE = stringPreferencesKey("main_currency_code")
        val SECONDARY_CURRENCY_CODE = stringPreferencesKey("secondary_currency_code")
        val USER_COUNTRY_CODE = stringPreferencesKey("user_country_code")
        val NOTIF_DAILY_ENABLED = booleanPreferencesKey("notif_daily_reminder_enabled")
        val NOTIF_DAILY_HOUR = intPreferencesKey("notif_daily_reminder_hour")
        val NOTIF_DAILY_MINUTE = intPreferencesKey("notif_daily_reminder_minute")
        val NOTIF_LOAN_DAY_BEFORE = booleanPreferencesKey("notif_loan_day_before_enabled")
        val NOTIF_LOAN_HOUR = intPreferencesKey("notif_loan_reminder_hour")
        val NOTIF_LOAN_MINUTE = intPreferencesKey("notif_loan_reminder_minute")
        val NOTIF_HIGH_SPEND = booleanPreferencesKey("notif_high_spend_alert_enabled")
        val NOTIF_DAILY_SPEND_LIMIT = doublePreferencesKey("notif_daily_spend_limit")
        val NOTIFICATION_LOG_JSON = stringPreferencesKey("notification_log_json")
        val DRIVE_LAST_SYNCED_AT = longPreferencesKey("drive_last_synced_at_millis")
        val DRIVE_PENDING_SYNC = booleanPreferencesKey("drive_pending_sync")
        val DRIVE_LAST_LOCAL_MODIFIED_AT = longPreferencesKey("drive_last_local_modified_at_millis")
        val DRIVE_FOLDER_LINKED = booleanPreferencesKey("drive_folder_linked")
        val LAST_LOCAL_OWNER_EMAIL = stringPreferencesKey("last_local_owner_email")
    }

    val userPrefs: Flow<UserPrefs> = context.dataStore.data.map { prefs ->
        val langCode = prefs[Keys.LANGUAGE] ?: AppLanguage.EN.code
        val lang = AppLanguage.fromCode(langCode)

        val startOfWeek = prefs[Keys.START_OF_WEEK]?.let { name ->
            runCatching { StartOfWeek.valueOf(name) }.getOrNull()
        } ?: StartOfWeek.SATURDAY

        val orderedSections = prefs[Keys.HOME_SECTION_ORDER]
            ?.split(",")
            ?.mapNotNull { runCatching { HomeSectionId.valueOf(it) }.getOrNull() }
            ?.let { list ->
                val missing = HomeSectionId.entries.filterNot { it in list }
                list + missing
            }
            ?: HomeSectionId.entries

        val hiddenSections = prefs[Keys.HIDDEN_HOME_SECTIONS]
            ?.split(",")
            ?.filter { it.isNotBlank() }
            ?.mapNotNull { runCatching { HomeSectionId.valueOf(it) }.getOrNull() }
            ?.toSet()
            ?: emptySet()

        val authMode = prefs[Keys.AUTH_MODE]?.let { raw ->
            runCatching { AuthMode.valueOf(raw) }.getOrNull()
        } ?: AuthMode.GUEST

        val homeLayoutStyle = prefs[Keys.HOME_LAYOUT_STYLE]?.let { raw ->
            runCatching { HomeLayoutStyle.valueOf(raw) }.getOrNull()
        } ?: HomeLayoutStyle.WALLET_STACK

        val legacyDark = prefs[Keys.IS_DARK_THEME] ?: false
        val themeMode = prefs[Keys.THEME_MODE]?.let { raw ->
            runCatching { AppThemeMode.valueOf(raw) }.getOrNull()
        } ?: if (legacyDark) AppThemeMode.DARK else AppThemeMode.LIGHT

        val colourTheme = prefs[Keys.COLOUR_THEME]?.let { raw ->
            runCatching { AppColourTheme.valueOf(raw) }.getOrNull()
        } ?: AppColourTheme.SKY_CYAN

        UserPrefs(
            language = lang,
            isDarkTheme = legacyDark,
            themeMode = themeMode,
            colourTheme = colourTheme,
            monthlyBudget = prefs[Keys.MONTHLY_BUDGET] ?: 0.0,
            userName = (prefs[Keys.USER_NAME] ?: "").let { stored ->
                if (authMode == AuthMode.GUEST && stored == "Tahmid Hasan") "" else stored
            },
            userEmail = prefs[Keys.USER_EMAIL] ?: "",
            userAvatarUrl = prefs[Keys.USER_AVATAR_URL] ?: "",
            guestAvatarUrl = prefs[Keys.GUEST_AVATAR_URL] ?: "",
            googleCustomAvatarUrl = prefs[Keys.GOOGLE_CUSTOM_AVATAR_URL] ?: "",
            authMode = authMode,
            hasCompletedWelcome = prefs[Keys.HAS_COMPLETED_WELCOME] ?: false,
            hasRequestedLaunchPermissions = prefs[Keys.HAS_REQUESTED_LAUNCH_PERMISSIONS] ?: false,
            enableSubcategories = prefs[Keys.ENABLE_SUBCATEGORIES] ?: true,
            autoOpenKeypad = prefs[Keys.AUTO_OPEN_KEYPAD] ?: true,
            defaultAccountId = prefs[Keys.DEFAULT_ACCOUNT_ID] ?: 0L,
            startOfWeek = startOfWeek,
            hapticFeedbackEnabled = prefs[Keys.HAPTIC_ENABLED] ?: true,
            notifBudgetAlerts = prefs[Keys.NOTIF_BUDGET] ?: true,
            notifCreditAlerts = prefs[Keys.NOTIF_CREDIT] ?: true,
            notifLoanReminders = prefs[Keys.NOTIF_LOAN] ?: true,
            appLockEnabled = prefs[Keys.APP_LOCK_ENABLED] ?: false,
            appLockPin = prefs[Keys.APP_LOCK_PIN] ?: "",
            biometricUnlockEnabled = prefs[Keys.BIOMETRIC_UNLOCK_ENABLED] ?: false,
            homeSectionOrder = orderedSections,
            hiddenHomeSections = hiddenSections,
            homeLayoutStyle = homeLayoutStyle,
            categoryOrderExpense = prefs[Keys.CATEGORY_ORDER_EXPENSE]
                ?.split(",")
                ?.filter { it.isNotBlank() }
                ?: emptyList(),
            categoryOrderIncome = prefs[Keys.CATEGORY_ORDER_INCOME]
                ?.split(",")
                ?.filter { it.isNotBlank() }
                ?: emptyList(),
            categoryOrderTransfer = prefs[Keys.CATEGORY_ORDER_TRANSFER]
                ?.split(",")
                ?.filter { it.isNotBlank() }
                ?: emptyList(),
            categorySortMode = prefs[Keys.CATEGORY_SORT_MODE]?.let { raw ->
                runCatching { CategorySortMode.valueOf(raw) }.getOrNull()
            } ?: CategorySortMode.DEFAULT,
            managedCategoryOverrides = BuiltInCategoriesCatalog.decodeOverridesFromJson(
                prefs[Keys.MANAGED_CATEGORIES_JSON] ?: ""
            ),
            deletedCategoryIds = prefs[Keys.DELETED_CATEGORY_IDS]
                ?.split(",")
                ?.filter { it.isNotBlank() }
                ?.toSet()
                ?: emptySet(),
            mainCurrencyCode = AppCurrency.fromCode(prefs[Keys.MAIN_CURRENCY_CODE]).code,
            secondaryCurrencyCode = AppCurrency.fromCodeOrNull(prefs[Keys.SECONDARY_CURRENCY_CODE])?.code ?: "",
            userCountryCode = AppCountry.fromCode(prefs[Keys.USER_COUNTRY_CODE]).code,
            notifDailyReminderEnabled = prefs[Keys.NOTIF_DAILY_ENABLED] ?: true,
            notifDailyReminderHour = (prefs[Keys.NOTIF_DAILY_HOUR] ?: 21).coerceIn(0, 23),
            notifDailyReminderMinute = (prefs[Keys.NOTIF_DAILY_MINUTE] ?: 0).coerceIn(0, 59),
            notifLoanDayBeforeEnabled = prefs[Keys.NOTIF_LOAN_DAY_BEFORE] ?: true,
            notifLoanReminderHour = (prefs[Keys.NOTIF_LOAN_HOUR] ?: 10).coerceIn(0, 23),
            notifLoanReminderMinute = (prefs[Keys.NOTIF_LOAN_MINUTE] ?: 0).coerceIn(0, 59),
            notifHighSpendAlertEnabled = prefs[Keys.NOTIF_HIGH_SPEND] ?: true,
            notifDailySpendLimit = (prefs[Keys.NOTIF_DAILY_SPEND_LIMIT] ?: 0.0).coerceAtLeast(0.0),
            notificationLogJson = prefs[Keys.NOTIFICATION_LOG_JSON] ?: "",
            driveLastSyncedAtMillis = prefs[Keys.DRIVE_LAST_SYNCED_AT] ?: 0L,
            drivePendingSync = prefs[Keys.DRIVE_PENDING_SYNC] ?: false,
            driveLastLocalModifiedAtMillis = prefs[Keys.DRIVE_LAST_LOCAL_MODIFIED_AT] ?: 0L,
            driveFolderLinked = prefs[Keys.DRIVE_FOLDER_LINKED] ?: false,
            lastLocalOwnerEmail = prefs[Keys.LAST_LOCAL_OWNER_EMAIL] ?: ""
        )
    }

    suspend fun toggleLanguage(current: AppLanguage) {
        val next = if (current == AppLanguage.EN) AppLanguage.BN else AppLanguage.EN
        context.dataStore.edit { it[Keys.LANGUAGE] = next.code }
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.dataStore.edit { it[Keys.LANGUAGE] = language.code }
    }

    suspend fun toggleDarkTheme(currentDark: Boolean) {
        val nextDark = !currentDark
        context.dataStore.edit { prefs ->
            prefs[Keys.IS_DARK_THEME] = nextDark
            prefs[Keys.THEME_MODE] = if (nextDark) AppThemeMode.DARK.name else AppThemeMode.LIGHT.name
        }
    }

    suspend fun setDarkTheme(isDark: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.IS_DARK_THEME] = isDark
            prefs[Keys.THEME_MODE] = if (isDark) AppThemeMode.DARK.name else AppThemeMode.LIGHT.name
        }
    }

    suspend fun setThemeMode(mode: AppThemeMode) {
        context.dataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = mode.name
            if (mode != AppThemeMode.AUTO) {
                prefs[Keys.IS_DARK_THEME] = (mode == AppThemeMode.DARK)
            }
        }
    }

    suspend fun setColourTheme(theme: AppColourTheme) {
        context.dataStore.edit { it[Keys.COLOUR_THEME] = theme.name }
    }

    suspend fun updateProfileAndBudget(userName: String, monthlyBudget: Double) {
        context.dataStore.edit { prefs ->
            if (userName.isNotBlank()) prefs[Keys.USER_NAME] = userName.trim()
            if (monthlyBudget > 0.0) prefs[Keys.MONTHLY_BUDGET] = monthlyBudget
        }
    }

    suspend fun updateDisplayName(userName: String) {
        val trimmed = userName.trim()
        if (trimmed.isNotEmpty()) {
            context.dataStore.edit { it[Keys.USER_NAME] = trimmed }
        }
    }

    suspend fun setGuestAvatarUrl(avatarUrl: String) {
        context.dataStore.edit { it[Keys.GUEST_AVATAR_URL] = avatarUrl.trim() }
    }

    suspend fun setCustomAvatarForMode(avatarValue: String, authMode: AuthMode) {
        context.dataStore.edit { prefs ->
            when (authMode) {
                AuthMode.GOOGLE -> prefs[Keys.GOOGLE_CUSTOM_AVATAR_URL] = avatarValue.trim()
                AuthMode.GUEST -> prefs[Keys.GUEST_AVATAR_URL] = avatarValue.trim()
            }
        }
    }

    suspend fun completeWelcomeAsGuest(guestDisplayName: String? = null) {
        context.dataStore.edit { prefs ->
            prefs[Keys.HAS_COMPLETED_WELCOME] = true
            prefs[Keys.AUTH_MODE] = AuthMode.GUEST.name
            if (!guestDisplayName.isNullOrBlank()) {
                prefs[Keys.USER_NAME] = guestDisplayName.trim()
            }
        }
    }

    suspend fun setLaunchPermissionsRequested(requested: Boolean = true) {
        context.dataStore.edit { prefs ->
            prefs[Keys.HAS_REQUESTED_LAUNCH_PERMISSIONS] = requested
        }
    }

    suspend fun linkGoogleAccount(
        displayName: String,
        email: String,
        avatarUrl: String
    ) {
        val normalizedEmail = email.trim()
        context.dataStore.edit { prefs ->
            val existingEmail = prefs[Keys.USER_EMAIL].orEmpty().trim().lowercase()
            val isSameAccount = existingEmail.isNotBlank() &&
                normalizedEmail.isNotBlank() &&
                existingEmail == normalizedEmail.lowercase()
            val existingAvatar = if (isSameAccount) prefs[Keys.USER_AVATAR_URL].orEmpty() else ""
            val resolvedHighResAvatar = ProfilePhotoUrlNormalizer.mergePreservingHighRes(
                incomingUrl = avatarUrl,
                existingUrl = existingAvatar
            )

            prefs[Keys.HAS_COMPLETED_WELCOME] = true
            prefs[Keys.AUTH_MODE] = AuthMode.GOOGLE.name
            if (displayName.isNotBlank()) prefs[Keys.USER_NAME] = displayName.trim()
            prefs[Keys.USER_EMAIL] = normalizedEmail
            prefs[Keys.LAST_LOCAL_OWNER_EMAIL] = normalizedEmail.lowercase()
            prefs[Keys.USER_AVATAR_URL] = resolvedHighResAvatar
            if (!isSameAccount) {
                prefs[Keys.GOOGLE_CUSTOM_AVATAR_URL] = ""
            }
        }
    }

    /**
     * Silently upgrades any previously stored low-resolution Google avatar URL (e.g. `=s96-c`)
     * to a sharp high-resolution URL (`=s400-c`) or backfills from Firebase Auth if blank,
     * without overwriting any custom avatar override.
     */
    suspend fun upgradeStoredGoogleAvatarIfNeeded(fallbackPhotoUrl: String = "") {
        context.dataStore.edit { prefs ->
            if (prefs[Keys.AUTH_MODE] != AuthMode.GOOGLE.name) return@edit
            val currentStored = prefs[Keys.USER_AVATAR_URL].orEmpty()
            val upgraded = ProfilePhotoUrlNormalizer.mergePreservingHighRes(
                incomingUrl = currentStored,
                existingUrl = fallbackPhotoUrl
            )
            if (upgraded.isNotBlank() && upgraded != currentStored) {
                prefs[Keys.USER_AVATAR_URL] = upgraded
            }
        }
    }

    /**
     * Clears user-scoped financial preferences and Drive sync watermarks when switching
     * from one Gmail user to another on the same device so each user's local state stays isolated.
     */
    suspend fun resetUserScopedPrefsForAccountSwitch() {
        context.dataStore.edit { prefs ->
            prefs[Keys.MONTHLY_BUDGET] = 0.0
            prefs[Keys.DEFAULT_ACCOUNT_ID] = 0L
            prefs[Keys.NOTIFICATION_LOG_JSON] = ""
            prefs[Keys.DRIVE_PENDING_SYNC] = false
            prefs[Keys.DRIVE_FOLDER_LINKED] = false
            prefs[Keys.DRIVE_LAST_SYNCED_AT] = 0L
            prefs[Keys.DRIVE_LAST_LOCAL_MODIFIED_AT] = 0L
        }
    }

    suspend fun unlinkGoogleAccount() {
        context.dataStore.edit { prefs ->
            val currentEmail = prefs[Keys.USER_EMAIL].orEmpty().trim().lowercase()
            if (currentEmail.isNotBlank()) {
                prefs[Keys.LAST_LOCAL_OWNER_EMAIL] = currentEmail
            }
            prefs[Keys.AUTH_MODE] = AuthMode.GUEST.name
            prefs[Keys.USER_NAME] = ""
            prefs[Keys.USER_EMAIL] = ""
            prefs[Keys.USER_AVATAR_URL] = ""
            prefs[Keys.GOOGLE_CUSTOM_AVATAR_URL] = ""
            prefs[Keys.DRIVE_PENDING_SYNC] = false
            prefs[Keys.DRIVE_FOLDER_LINKED] = false
            prefs[Keys.DRIVE_LAST_SYNCED_AT] = 0L
        }
    }

    /**
     * Cleanly logs out the active session and returns to the initial startup screen
     * without touching or deleting any locally stored financial records.
     */
    suspend fun logOut() {
        context.dataStore.edit { prefs ->
            val currentEmail = prefs[Keys.USER_EMAIL].orEmpty().trim().lowercase()
            if (currentEmail.isNotBlank()) {
                prefs[Keys.LAST_LOCAL_OWNER_EMAIL] = currentEmail
            }
            prefs[Keys.HAS_COMPLETED_WELCOME] = false
            prefs[Keys.AUTH_MODE] = AuthMode.GUEST.name
            prefs[Keys.USER_EMAIL] = ""
            prefs[Keys.USER_AVATAR_URL] = ""
            prefs[Keys.GOOGLE_CUSTOM_AVATAR_URL] = ""
            prefs[Keys.DRIVE_PENDING_SYNC] = false
            prefs[Keys.DRIVE_FOLDER_LINKED] = false
            prefs[Keys.DRIVE_LAST_SYNCED_AT] = 0L
        }
    }

    suspend fun markDriveSyncPending(modifiedAtMillis: Long = System.currentTimeMillis()) {
        context.dataStore.edit { prefs ->
            prefs[Keys.DRIVE_PENDING_SYNC] = true
            prefs[Keys.DRIVE_LAST_LOCAL_MODIFIED_AT] = modifiedAtMillis
        }
    }

    suspend fun markDriveSynced(syncedAtMillis: Long, clearPending: Boolean = true) {
        context.dataStore.edit { prefs ->
            prefs[Keys.DRIVE_LAST_SYNCED_AT] = syncedAtMillis
            prefs[Keys.DRIVE_FOLDER_LINKED] = true
            if (clearPending) {
                prefs[Keys.DRIVE_PENDING_SYNC] = false
            }
            if ((prefs[Keys.DRIVE_LAST_LOCAL_MODIFIED_AT] ?: 0L) < syncedAtMillis) {
                prefs[Keys.DRIVE_LAST_LOCAL_MODIFIED_AT] = syncedAtMillis
            }
        }
    }

    suspend fun clearDriveSyncState() {
        context.dataStore.edit { prefs ->
            prefs[Keys.DRIVE_PENDING_SYNC] = false
            prefs[Keys.DRIVE_FOLDER_LINKED] = false
            prefs[Keys.DRIVE_LAST_SYNCED_AT] = 0L
        }
    }

    suspend fun setEnableSubcategories(enabled: Boolean) {
        context.dataStore.edit { it[Keys.ENABLE_SUBCATEGORIES] = enabled }
    }

    suspend fun setAutoOpenKeypad(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_OPEN_KEYPAD] = enabled }
    }

    suspend fun setDefaultAccountId(accountId: Long) {
        context.dataStore.edit { it[Keys.DEFAULT_ACCOUNT_ID] = accountId }
    }

    suspend fun setStartOfWeek(startOfWeek: StartOfWeek) {
        context.dataStore.edit { it[Keys.START_OF_WEEK] = startOfWeek.name }
    }

    suspend fun setMainCurrency(currencyCode: String) {
        val normalized = AppCurrency.fromCode(currencyCode).code
        context.dataStore.edit { prefs ->
            prefs[Keys.MAIN_CURRENCY_CODE] = normalized
            // If user picked the same currency as secondary, clear secondary so they don't duplicate
            if (prefs[Keys.SECONDARY_CURRENCY_CODE]?.equals(normalized, ignoreCase = true) == true) {
                prefs[Keys.SECONDARY_CURRENCY_CODE] = ""
            }
        }
    }

    suspend fun setSecondaryCurrency(currencyCode: String) {
        val normalized = AppCurrency.fromCodeOrNull(currencyCode)?.code ?: ""
        context.dataStore.edit { prefs ->
            val currentMain = AppCurrency.fromCode(prefs[Keys.MAIN_CURRENCY_CODE]).code
            prefs[Keys.SECONDARY_CURRENCY_CODE] = if (normalized.equals(currentMain, ignoreCase = true)) {
                ""
            } else {
                normalized
            }
        }
    }

    suspend fun setUserCountryCode(countryCode: String) {
        val normalized = AppCountry.fromCode(countryCode).code
        context.dataStore.edit { prefs ->
            prefs[Keys.USER_COUNTRY_CODE] = normalized
        }
    }

    suspend fun setHapticFeedback(enabled: Boolean) {
        context.dataStore.edit { it[Keys.HAPTIC_ENABLED] = enabled }
    }

    suspend fun setNotificationPreferences(budget: Boolean, credit: Boolean, loan: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.NOTIF_BUDGET] = budget
            prefs[Keys.NOTIF_CREDIT] = credit
            prefs[Keys.NOTIF_LOAN] = loan
        }
    }

    suspend fun setDailyReminderConfig(enabled: Boolean, hour: Int, minute: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.NOTIF_DAILY_ENABLED] = enabled
            prefs[Keys.NOTIF_DAILY_HOUR] = hour.coerceIn(0, 23)
            prefs[Keys.NOTIF_DAILY_MINUTE] = minute.coerceIn(0, 59)
        }
    }

    suspend fun setLoanDayBeforeEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.NOTIF_LOAN_DAY_BEFORE] = enabled
        }
    }

    suspend fun setLoanReminderTime(hour: Int, minute: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.NOTIF_LOAN_HOUR] = hour.coerceIn(0, 23)
            prefs[Keys.NOTIF_LOAN_MINUTE] = minute.coerceIn(0, 59)
        }
    }

    suspend fun setHighSpendAlertConfig(enabled: Boolean, dailyLimit: Double = 0.0) {
        context.dataStore.edit { prefs ->
            prefs[Keys.NOTIF_HIGH_SPEND] = enabled
            if (dailyLimit >= 0.0) {
                prefs[Keys.NOTIF_DAILY_SPEND_LIMIT] = dailyLimit
            }
        }
    }

    suspend fun appendNotificationLogEntry(item: com.example.notifications.NotificationLogItem) {
        context.dataStore.edit { prefs ->
            val current = com.example.notifications.NotificationLogItem
                .decodeListFromJson(prefs[Keys.NOTIFICATION_LOG_JSON] ?: "")
                .filterNot { it.id == item.id }
            val updated = (listOf(item) + current)
                .take(com.example.notifications.NotificationLogItem.MAX_STORED_ITEMS)
            prefs[Keys.NOTIFICATION_LOG_JSON] =
                com.example.notifications.NotificationLogItem.encodeListToJson(updated)
        }
    }

    suspend fun clearNotificationLog() {
        context.dataStore.edit { prefs ->
            prefs[Keys.NOTIFICATION_LOG_JSON] = ""
        }
    }

    suspend fun setAppLock(enabled: Boolean, pin: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.APP_LOCK_ENABLED] = enabled
            if (pin.length == 4) prefs[Keys.APP_LOCK_PIN] = pin
        }
    }

    suspend fun setBiometricUnlockEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.BIOMETRIC_UNLOCK_ENABLED] = enabled
        }
    }

    suspend fun updateHomeDashboardLayout(
        layoutStyle: HomeLayoutStyle,
        order: List<HomeSectionId>,
        hidden: Set<HomeSectionId>
    ) {
        context.dataStore.edit { prefs ->
            prefs[Keys.HOME_LAYOUT_STYLE] = layoutStyle.name
            prefs[Keys.HOME_SECTION_ORDER] = order.joinToString(",") { it.name }
            prefs[Keys.HIDDEN_HOME_SECTIONS] = hidden.joinToString(",") { it.name }
        }
    }

    suspend fun setCategoryOrder(type: TransactionType, orderedIds: List<String>) {
        val key = when (type) {
            TransactionType.EXPENSE -> Keys.CATEGORY_ORDER_EXPENSE
            TransactionType.INCOME -> Keys.CATEGORY_ORDER_INCOME
            TransactionType.TRANSFER -> Keys.CATEGORY_ORDER_TRANSFER
        }
        context.dataStore.edit { prefs ->
            prefs[key] = orderedIds.joinToString(",")
            prefs[Keys.CATEGORY_SORT_MODE] = CategorySortMode.CUSTOM.name
        }
    }

    suspend fun setCategorySortMode(mode: CategorySortMode) {
        context.dataStore.edit { prefs ->
            prefs[Keys.CATEGORY_SORT_MODE] = mode.name
        }
    }

    suspend fun upsertManagedCategory(item: ManagedCategoryItem) {
        context.dataStore.edit { prefs ->
            val current = BuiltInCategoriesCatalog
                .decodeOverridesFromJson(prefs[Keys.MANAGED_CATEGORIES_JSON] ?: "")
                .toMutableList()
            val existingIdx = current.indexOfFirst { it.id == item.id }
            if (existingIdx >= 0) {
                current[existingIdx] = item
            } else {
                current.add(item)
            }
            prefs[Keys.MANAGED_CATEGORIES_JSON] = BuiltInCategoriesCatalog.encodeOverridesToJson(current)

            // Ensure restored/updated category is not marked deleted
            val deleted = (prefs[Keys.DELETED_CATEGORY_IDS] ?: "")
                .split(",")
                .filter { it.isNotBlank() && it != item.id }
            prefs[Keys.DELETED_CATEGORY_IDS] = deleted.joinToString(",")
        }
    }

    suspend fun deleteManagedCategory(categoryId: String, isBuiltIn: Boolean) {
        context.dataStore.edit { prefs ->
            val current = BuiltInCategoriesCatalog
                .decodeOverridesFromJson(prefs[Keys.MANAGED_CATEGORIES_JSON] ?: "")
                .filterNot { it.id == categoryId }
            prefs[Keys.MANAGED_CATEGORIES_JSON] = BuiltInCategoriesCatalog.encodeOverridesToJson(current)

            if (isBuiltIn) {
                val deleted = (prefs[Keys.DELETED_CATEGORY_IDS] ?: "")
                    .split(",")
                    .filter { it.isNotBlank() }
                    .toMutableSet()
                deleted.add(categoryId)
                prefs[Keys.DELETED_CATEGORY_IDS] = deleted.joinToString(",")
            }
        }
    }

    suspend fun restoreManagedCategories(
        overrides: List<ManagedCategoryItem>,
        deletedIds: Set<String>
    ) {
        context.dataStore.edit { prefs ->
            prefs[Keys.MANAGED_CATEGORIES_JSON] = BuiltInCategoriesCatalog.encodeOverridesToJson(overrides)
            prefs[Keys.DELETED_CATEGORY_IDS] = deletedIds.filter { it.isNotBlank() }.joinToString(",")
        }
    }

    suspend fun resetCategoriesToDefault() {
        context.dataStore.edit { prefs ->
            prefs.remove(Keys.CATEGORY_ORDER_EXPENSE)
            prefs.remove(Keys.CATEGORY_ORDER_INCOME)
            prefs.remove(Keys.CATEGORY_ORDER_TRANSFER)
            prefs.remove(Keys.MANAGED_CATEGORIES_JSON)
            prefs.remove(Keys.DELETED_CATEGORY_IDS)
            prefs[Keys.CATEGORY_SORT_MODE] = CategorySortMode.DEFAULT.name
        }
    }
}
