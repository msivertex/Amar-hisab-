package com.example

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.height
import androidx.test.core.app.ApplicationProvider
import com.example.core.DeviceTimeContext
import com.example.data.local.AccountEntity
import com.example.data.local.LoanEntity
import com.example.data.local.TransactionEntity
import com.example.data.local.UserPrefs
import com.example.data.model.AccountType
import com.example.data.model.ExpenseCategory
import com.example.data.model.LoanType
import com.example.data.model.SmartAlert
import com.example.data.model.TransactionType
import com.example.localization.AppLanguage
import com.example.localization.StringsProvider
import com.example.notifications.AmarHisabNotificationManager
import com.example.notifications.NotificationActionRoute
import com.example.notifications.NotificationCategory
import com.example.notifications.NotificationLogItem
import com.example.notifications.NotificationTimeCalculator
import com.example.ui.screens.NotificationSettingsSection
import com.example.ui.screens.NotificationsSheet
import com.example.ui.theme.AmarHisabTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import java.util.Calendar
import java.util.TimeZone

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NotificationSystemRobolectricTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        AmarHisabNotificationManager.clearDedupStateForTesting(context)
        com.example.notifications.NotificationVisualProvider.clearCacheForTesting(context)
    }

    @Test
    fun dailyReminderStrictlyFollowsLocalTimezoneAndRollsOverAfter9Pm() {
        val dhakaTz = TimeZone.getTimeZone("Asia/Dhaka")
        val kuwaitTz = TimeZone.getTimeZone("Asia/Kuwait")

        // 2026-05-10 18:00 in Asia/Dhaka -> same day 21:00 Asia/Dhaka
        val beforeNineDhaka = Calendar.getInstance(dhakaTz).apply {
            set(2026, Calendar.MAY, 10, 18, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val dhakaTrigger = NotificationTimeCalculator.computeNextDailyReminderMillis(
            nowMillis = beforeNineDhaka,
            hour = NotificationTimeCalculator.DEFAULT_DAILY_REMINDER_HOUR,
            minute = NotificationTimeCalculator.DEFAULT_DAILY_REMINDER_MINUTE,
            timeZone = dhakaTz
        )
        val dhakaCal = Calendar.getInstance(dhakaTz).apply { timeInMillis = dhakaTrigger }
        assertEquals(2026, dhakaCal.get(Calendar.YEAR))
        assertEquals(Calendar.MAY, dhakaCal.get(Calendar.MONTH))
        assertEquals(10, dhakaCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(21, dhakaCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, dhakaCal.get(Calendar.MINUTE))

        // Same UTC instant in Asia/Kuwait (UTC+3, 3 hours behind Dhaka -> 15:00 Kuwait time):
        // Trigger must be 21:00 Kuwait time (3 hours AFTER Dhaka's 21:00 trigger!)
        val kuwaitTrigger = NotificationTimeCalculator.computeNextDailyReminderMillis(
            nowMillis = beforeNineDhaka,
            hour = 21,
            minute = 0,
            timeZone = kuwaitTz
        )
        assertEquals(3L * 3600L * 1000L, kuwaitTrigger - dhakaTrigger)

        // After 21:00 local time (21:15) -> must roll over to next calendar day at 21:00
        val afterNineDhaka = Calendar.getInstance(dhakaTz).apply {
            set(2026, Calendar.MAY, 10, 21, 15, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val nextDayTrigger = NotificationTimeCalculator.computeNextDailyReminderMillis(
            nowMillis = afterNineDhaka,
            hour = 21,
            minute = 0,
            timeZone = dhakaTz
        )
        val nextDayCal = Calendar.getInstance(dhakaTz).apply { timeInMillis = nextDayTrigger }
        assertEquals(11, nextDayCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(21, nextDayCal.get(Calendar.HOUR_OF_DAY))

        // Localized labels for 9:00 PM & 10:00 AM
        assertEquals("9:00 PM", NotificationTimeCalculator.formatLocalTimeLabel(21, 0, AppLanguage.EN))
        assertEquals("রাত ৯:০০টা", NotificationTimeCalculator.formatLocalTimeLabel(21, 0, AppLanguage.BN))
        assertEquals("সকাল ১০:০০টা", NotificationTimeCalculator.formatLocalTimeLabel(10, 0, AppLanguage.BN))
    }

    @Test
    fun dueDateAndDayBeforeRemindersPinTo10AmLocalTimeAndScheduleAlarms() {
        val kuwaitTz = TimeZone.getTimeZone("Asia/Kuwait")
        val now = Calendar.getInstance(kuwaitTz).apply {
            set(2026, Calendar.MAY, 10, 9, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val dueDate = Calendar.getInstance(kuwaitTz).apply {
            set(2026, Calendar.MAY, 15, 19, 45, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val dueDay10Am = NotificationTimeCalculator.computeDueDateReminderMillis(
            dueDateMillis = dueDate,
            daysBefore = 0,
            timeZone = kuwaitTz
        )
        val dayBefore10Am = NotificationTimeCalculator.computeDueDateReminderMillis(
            dueDateMillis = dueDate,
            daysBefore = 1,
            timeZone = kuwaitTz
        )
        val dueCal = Calendar.getInstance(kuwaitTz).apply { timeInMillis = dueDay10Am }
        val beforeCal = Calendar.getInstance(kuwaitTz).apply { timeInMillis = dayBefore10Am }

        assertEquals(15, dueCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(10, dueCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, dueCal.get(Calendar.MINUTE))

        assertEquals(14, beforeCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(10, beforeCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, beforeCal.get(Calendar.MINUTE))

        val kwdLoan = LoanEntity(
            id = 42L,
            personName = "Farhan",
            loanType = LoanType.I_OWE,
            totalAmount = 25.50,
            paidAmount = 0.0,
            note = "Rent split",
            dueDateMillis = dueDate,
            currencyCode = "KWD"
        )
        val prefs = UserPrefs(
            language = AppLanguage.BN,
            notifDailyReminderEnabled = true,
            notifDailyReminderHour = 21,
            notifDailyReminderMinute = 0,
            notifLoanReminders = true,
            notifLoanDayBeforeEnabled = true
        )

        AmarHisabNotificationManager.scheduleAllReminders(
            context = context,
            prefs = prefs,
            loans = listOf(kwdLoan),
            nowMillis = now,
            timeZone = kuwaitTz
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val scheduled = Shadows.shadowOf(alarmManager).scheduledAlarms
        // Expect 3 alarms: 1 daily 9:00 PM + 1 due-day 10:00 AM + 1 day-before 10:00 AM
        assertEquals(3, scheduled.size)
        val triggerTimes = scheduled.map { it.triggerAtTime }.toSet()
        assertTrue(dueDay10Am in triggerTimes)
        assertTrue(dayBefore10Am in triggerTimes)
    }

    @Test
    fun zeroStateEmitsZeroThresholdNotificationsAndImmediateAlertsFireAndDedupOnRealSpend() {
        // Grant POST_NOTIFICATIONS in Robolectric shadow so system notifications post cleanly on SDK 33
        val shadowApp = Shadows.shadowOf(ApplicationProvider.getApplicationContext<android.app.Application>())
        shadowApp.grantPermissions(android.Manifest.permission.POST_NOTIFICATIONS)

        val tz = TimeZone.getTimeZone("Asia/Dhaka")
        val now = Calendar.getInstance(tz).apply {
            set(2026, Calendar.MAY, 10, 14, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        // 1. Fresh Zero-State install -> ZERO notifications
        val zeroPrefs = UserPrefs(monthlyBudget = 0.0, notifBudgetAlerts = true, notifCreditAlerts = true)
        val zeroTriggered = AmarHisabNotificationManager.evaluateAndTriggerImmediateThresholdAlerts(
            context = context,
            prefs = zeroPrefs,
            accounts = emptyList(),
            transactions = emptyList(),
            spentThisMonth = 0.0,
            todaySpend = 0.0,
            nowMillis = now,
            timeZone = tz
        )
        assertTrue(zeroTriggered.isEmpty())
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        assertTrue(Shadows.shadowOf(nm).allNotifications.isEmpty())

        // 2. Real spending crosses 85% monthly budget AND 45% credit card utilization -> fires immediately!
        val activePrefs = UserPrefs(
            language = AppLanguage.BN,
            monthlyBudget = 10_000.0,
            notifBudgetAlerts = true,
            notifCreditAlerts = true,
            notifHighSpendAlertEnabled = true
        )
        val cc = AccountEntity(
            id = 7L,
            name = "City Amex",
            bankOrProvider = "City Bank",
            type = AccountType.CREDIT_CARD,
            balance = 45_000.0,
            creditLimit = 100_000.0,
            colorArgb = 0xFF0EA5E9,
            currencyCode = "BDT"
        )
        val tx = TransactionEntity(
            id = 1L,
            title = "Grocery & Bills",
            amount = 8_500.0,
            type = TransactionType.EXPENSE,
            category = ExpenseCategory.GROCERIES,
            accountId = 7L,
            timestamp = now,
            currencyCode = "BDT"
        )
        val triggered = AmarHisabNotificationManager.evaluateAndTriggerImmediateThresholdAlerts(
            context = context,
            prefs = activePrefs,
            accounts = listOf(cc),
            transactions = listOf(tx),
            spentThisMonth = 8_500.0,
            todaySpend = 8_500.0,
            nowMillis = now,
            timeZone = tz
        )
        // Budget 85% + Credit Card 45% + High Daily Spend -> 3 immediate alerts
        assertEquals(3, triggered.size)
        assertTrue(triggered.any { it.category == NotificationCategory.BUDGET_THRESHOLD && it.route == NotificationActionRoute.OPEN_INSIGHTS_BUDGET })
        assertTrue(triggered.any { it.category == NotificationCategory.HIGH_SPEND_CARD && it.route == NotificationActionRoute.OPEN_ACCOUNTS })
        assertEquals(3, Shadows.shadowOf(nm).allNotifications.size)

        // 3. Re-evaluating immediately on the same day must deduplicate cleanly (no spam)
        val duplicateCheck = AmarHisabNotificationManager.evaluateAndTriggerImmediateThresholdAlerts(
            context = context,
            prefs = activePrefs,
            accounts = listOf(cc),
            transactions = listOf(tx),
            spentThisMonth = 8_500.0,
            todaySpend = 8_500.0,
            nowMillis = now + 5_000L,
            timeZone = tz
        )
        assertTrue(duplicateCheck.isEmpty())

        // 4. Multi-currency KWD loan due reminder formats cleanly with KWD د.ك label & 3 decimals
        val kwdLoan = LoanEntity(
            id = 99L,
            personName = "Imran",
            loanType = LoanType.OWED_TO_ME,
            totalAmount = 58.50,
            paidAmount = 0.0,
            currencyCode = "KWD"
        )
        val loanLog = AmarHisabNotificationManager.postLoanDueReminder(
            context = context,
            prefs = activePrefs,
            loan = kwdLoan,
            isDayBefore = false,
            nowMillis = now
        )
        assertEquals(NotificationActionRoute.OPEN_PLAN_LOANS, loanLog.route)
        assertTrue(
            "Expected symbol-free KWD 3-decimal formatted amount in '${loanLog.body}'",
            loanLog.body.contains("KWD ৫৮.৫০০") && !loanLog.body.contains("د.ك")
        )

        // JSON codec round-trip for in-app notification history
        val encoded = NotificationLogItem.encodeListToJson(triggered + loanLog)
        val decoded = NotificationLogItem.decodeListFromJson(encoded)
        assertEquals(4, decoded.size)
        assertEquals(NotificationActionRoute.OPEN_PLAN_LOANS, decoded.last().route)
    }

    @Test
    fun notificationSettingsSectionDisplaysLocalTimezone9PmDefaultAndToggles() {
        var dailyToggled: Boolean? = null
        var dayBeforeToggled: Boolean? = null
        var highSpendToggled: Boolean? = null

        val prefs = UserPrefs(
            language = AppLanguage.BN,
            notifDailyReminderEnabled = true,
            notifDailyReminderHour = 21,
            notifDailyReminderMinute = 0,
            notifLoanReminders = true,
            notifLoanDayBeforeEnabled = true,
            notifBudgetAlerts = true,
            notifCreditAlerts = true,
            notifHighSpendAlertEnabled = true
        )
        val tzCtx = DeviceTimeContext.current(timeZone = TimeZone.getTimeZone("Asia/Kuwait"))

        composeRule.setContent {
            AmarHisabTheme {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    NotificationSettingsSection(
                        prefs = prefs,
                        strings = StringsProvider.get(AppLanguage.BN),
                        deviceTimeContext = tzCtx,
                        onSetNotifications = { _, _, _ -> },
                        onSetDailyReminder = { enabled, _, _ -> dailyToggled = enabled },
                        onSetLoanDayBefore = { dayBeforeToggled = it },
                        onSetHighSpendAlert = { highSpendToggled = it }
                    )
                }
            }
        }

        // Visible local timezone status banner is removed across main list and both reminder detail views
        composeRule.onNodeWithTag("settings_notif_timezone_badge").assertDoesNotExist()
        composeRule.onNodeWithText("ডিভাইসের স্থানীয় সময় ও টাইমজোন অনুযায়ী সক্রিয়", substring = true).assertDoesNotExist()
        composeRule.onNodeWithText(tzCtx.displayTimeZoneSummary, substring = true).assertDoesNotExist()
        composeRule.onNodeWithTag("settings_test_notification_button").assertDoesNotExist()
        composeRule.onNodeWithTag("settings_test_notification_feedback").assertDoesNotExist()

        // Main Financial Alerts list keeps reminder sub-options hidden until tapped
        composeRule.onNodeWithTag("settings_daily_reminder_card").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("settings_toggle_daily_reminder").assertDoesNotExist()
        composeRule.onNodeWithTag("settings_daily_reminder_time_row").assertDoesNotExist()
        composeRule.onNodeWithTag("settings_toggle_loan_day_before").assertDoesNotExist()
        composeRule.onNodeWithTag("settings_loan_reminder_time_row").assertDoesNotExist()

        // High Daily Spend Alert stays directly toggleable on main list
        composeRule.onNodeWithTag("settings_toggle_high_spend_alert").performScrollTo().performClick()
        assertEquals(false, highSpendToggled)

        // Tap Daily Expense & Income Reminder -> opens detail view with ON/OFF toggle + 9:00 PM time row (no timezone banner)
        composeRule.onNodeWithTag("settings_daily_reminder_card").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("settings_daily_reminder_detail_panel").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_notif_timezone_badge").assertDoesNotExist()
        composeRule.onNodeWithText("ডিভাইসের স্থানীয় সময় ও টাইমজোন অনুযায়ী সক্রিয়", substring = true).assertDoesNotExist()
        composeRule.onNodeWithText(tzCtx.displayTimeZoneSummary, substring = true).assertDoesNotExist()
        composeRule.onNodeWithTag("settings_daily_reminder_time_row").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("রাত ৯:০০টা").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_toggle_daily_reminder").performScrollTo().performClick()
        assertEquals(false, dailyToggled)

        // Return to main Financial Alerts list and open Dues & Payment Reminder detail view (no timezone banner)
        composeRule.onNodeWithTag("settings_alert_detail_back_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("settings_loan_reminder_card").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("settings_loan_reminder_detail_panel").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_notif_timezone_badge").assertDoesNotExist()
        composeRule.onNodeWithText("ডিভাইসের স্থানীয় সময় ও টাইমজোন অনুযায়ী সক্রিয়", substring = true).assertDoesNotExist()
        composeRule.onNodeWithText(tzCtx.displayTimeZoneSummary, substring = true).assertDoesNotExist()
        composeRule.onNodeWithTag("settings_toggle_loan_day_before").performScrollTo().performClick()
        assertEquals(false, dayBeforeToggled)
    }

    @Test
    fun notificationsSheetZeroStateDisplaysCleanEmptyCard() {
        composeRule.setContent {
            AmarHisabTheme {
                NotificationsSheet(
                    alerts = emptyList(),
                    strings = StringsProvider.get(AppLanguage.BN),
                    language = AppLanguage.BN,
                    onDismiss = {}
                )
            }
        }

        composeRule.onNodeWithTag("notifications_empty_state").assertIsDisplayed()
    }

    @Test
    fun notificationsSheetShowsZeroStateEmptyAndDeepLinksOnAlertTap() {
        var routedTo: NotificationActionRoute? = null
        val dueAlert = SmartAlert(
            id = "loan_due_5",
            title = "আজ নির্ধারিত দেনা: রহিম",
            subtitle = "বাকি KD ৫৮.৫০ • সকাল ১০:০০টা রিমাইন্ডার।",
            isWarning = true,
            actionRoute = NotificationActionRoute.OPEN_PLAN_LOANS,
            category = NotificationCategory.DUE_LOAN
        )

        composeRule.setContent {
            AmarHisabTheme {
                NotificationsSheet(
                    alerts = listOf(dueAlert),
                    strings = StringsProvider.get(AppLanguage.BN),
                    language = AppLanguage.BN,
                    onSelectActionRoute = { routedTo = it },
                    onDismiss = {}
                )
            }
        }

        composeRule.onNodeWithTag("notification_item_loan_due_5").assertIsDisplayed().performClick()
        assertEquals(NotificationActionRoute.OPEN_PLAN_LOANS, routedTo)
    }

    @Test
    fun financialAlertsRemoveManualTestButtonAndNestSubOptionsInsideDetailViews() {
        val shadowApp = Shadows.shadowOf(ApplicationProvider.getApplicationContext<android.app.Application>())
        shadowApp.grantPermissions(android.Manifest.permission.POST_NOTIFICATIONS)

        var savedLoanTime: Pair<Int, Int>? = null
        var dailyToggleResult: Boolean? = null
        var loanToggleResult: Boolean? = null
        val prefs = UserPrefs(
            language = AppLanguage.EN,
            notifDailyReminderEnabled = true,
            notifDailyReminderHour = 21,
            notifDailyReminderMinute = 0,
            notifLoanReminders = true,
            notifLoanDayBeforeEnabled = true,
            notifLoanReminderHour = 8,
            notifLoanReminderMinute = 30,
            notifBudgetAlerts = true,
            notifCreditAlerts = true,
            notifHighSpendAlertEnabled = true
        )
        val tzCtx = DeviceTimeContext.current(timeZone = TimeZone.getTimeZone("Asia/Dhaka"))

        composeRule.setContent {
            AmarHisabTheme(darkTheme = false, isBangla = false) {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    NotificationSettingsSection(
                        prefs = prefs,
                        strings = StringsProvider.english,
                        deviceTimeContext = tzCtx,
                        onSetNotifications = { _, _, loan -> loanToggleResult = loan },
                        onSetDailyReminder = { enabled, _, _ -> dailyToggleResult = enabled },
                        onSetLoanDayBefore = {},
                        onSetLoanReminderTime = { h, m -> savedLoanTime = h to m },
                        onSetHighSpendAlert = {}
                    )
                }
            }
        }

        composeRule.waitForIdle()

        // 1. Manual Test Notification button & banner are completely removed
        composeRule.onNodeWithTag("settings_test_notification_button").assertDoesNotExist()
        composeRule.onNodeWithTag("settings_test_notification_pill").assertDoesNotExist()
        composeRule.onNodeWithTag("settings_test_notification_feedback").assertDoesNotExist()

        // 2. High-priority Android notification channels are initialized automatically
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        assertEquals(
            NotificationManager.IMPORTANCE_HIGH,
            nm.getNotificationChannel(AmarHisabNotificationManager.CHANNEL_DAILY_REMINDER)?.importance
        )
        assertEquals(
            NotificationManager.IMPORTANCE_HIGH,
            nm.getNotificationChannel(AmarHisabNotificationManager.CHANNEL_DUES_REMINDERS)?.importance
        )
        assertEquals(
            NotificationManager.IMPORTANCE_HIGH,
            nm.getNotificationChannel(AmarHisabNotificationManager.CHANNEL_THRESHOLD_ALERTS)?.importance
        )

        // 3. Main Financial Alerts list does NOT expose reminder toggles/time pickers when collapsed
        composeRule.onNodeWithTag("settings_financial_alerts_main_list").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_daily_reminder_card").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("settings_loan_reminder_card").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("settings_toggle_daily_reminder").assertDoesNotExist()
        composeRule.onNodeWithTag("settings_daily_reminder_time_row").assertDoesNotExist()
        composeRule.onNodeWithTag("settings_toggle_loan_reminder").assertDoesNotExist()
        composeRule.onNodeWithTag("settings_loan_day_before_row").assertDoesNotExist()
        composeRule.onNodeWithTag("settings_loan_reminder_time_row").assertDoesNotExist()

        // 4. Tap Daily Expense & Income Reminder -> detail view shows ON/OFF toggle with Daily Reminder Time right below
        composeRule.onNodeWithTag("settings_daily_reminder_card").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("settings_daily_reminder_detail_panel").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_toggle_daily_reminder").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_daily_reminder_time_row").assertIsDisplayed()
        val dailyToggleBounds = composeRule.onNodeWithTag("settings_toggle_daily_reminder").getBoundsInRoot()
        val dailyTimeBounds = composeRule.onNodeWithTag("settings_daily_reminder_time_row").getBoundsInRoot()
        assertTrue(
            "Expected Daily Reminder Time (${dailyTimeBounds.top}) directly below ON/OFF switch (${dailyToggleBounds.bottom})",
            dailyTimeBounds.top >= dailyToggleBounds.bottom
        )
        composeRule.onNodeWithTag("settings_toggle_daily_reminder").performClick()
        assertEquals(false, dailyToggleResult)

        // Return to main list
        composeRule.onNodeWithTag("settings_alert_detail_back_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("settings_financial_alerts_main_list").assertIsDisplayed()

        // 5. Tap Dues & Payment Reminder -> detail view shows ON/OFF toggle, 1-Day-Before Reminder, and Dues Reminder Time
        composeRule.onNodeWithTag("settings_loan_reminder_card").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("settings_loan_reminder_detail_panel").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_toggle_loan_reminder").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_loan_day_before_row").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_loan_reminder_time_row").assertIsDisplayed()
        val loanToggleBounds = composeRule.onNodeWithTag("settings_toggle_loan_reminder").getBoundsInRoot()
        val dayBeforeBounds = composeRule.onNodeWithTag("settings_loan_day_before_row").getBoundsInRoot()
        val loanTimeBounds = composeRule.onNodeWithTag("settings_loan_reminder_time_row").getBoundsInRoot()
        assertTrue(dayBeforeBounds.top >= loanToggleBounds.bottom)
        assertTrue(loanTimeBounds.top >= dayBeforeBounds.bottom)

        composeRule.onNodeWithTag("settings_toggle_loan_reminder").performClick()
        assertEquals(false, loanToggleResult)

        // Tapping Dues Reminder Time opens picker and saves custom dues time
        composeRule.onNodeWithTag("settings_loan_reminder_time_row").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("settings_time_picker_confirm").performClick()
        assertEquals(8 to 30, savedLoanTime)
    }

    @Test
    fun customDuesReminderTimeSchedulesDueDayAndDayBeforeAtChosenLocalTime() {
        val dhakaTz = TimeZone.getTimeZone("Asia/Dhaka")
        val now = Calendar.getInstance(dhakaTz).apply {
            set(2026, Calendar.MAY, 10, 7, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val dueDate = Calendar.getInstance(dhakaTz).apply {
            set(2026, Calendar.MAY, 15, 19, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val prefs = UserPrefs(
            language = AppLanguage.EN,
            notifDailyReminderEnabled = false,
            notifLoanReminders = true,
            notifLoanDayBeforeEnabled = true,
            notifLoanReminderHour = 8,
            notifLoanReminderMinute = 30
        )
        val loan = LoanEntity(
            id = 77L,
            personName = "Karim",
            loanType = LoanType.I_OWE,
            totalAmount = 5000.0,
            paidAmount = 0.0,
            dueDateMillis = dueDate,
            currencyCode = "BDT"
        )

        AmarHisabNotificationManager.scheduleAllReminders(
            context = context,
            prefs = prefs,
            loans = listOf(loan),
            nowMillis = now,
            timeZone = dhakaTz
        )

        val expectedDueDay = NotificationTimeCalculator.computeDueDateReminderMillis(
            dueDateMillis = dueDate,
            daysBefore = 0,
            timeZone = dhakaTz,
            triggerHour = 8,
            triggerMinute = 30
        )
        val expectedDayBefore = NotificationTimeCalculator.computeDueDateReminderMillis(
            dueDateMillis = dueDate,
            daysBefore = 1,
            timeZone = dhakaTz,
            triggerHour = 8,
            triggerMinute = 30
        )
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerTimes = Shadows.shadowOf(alarmManager).scheduledAlarms.map { it.triggerAtTime }.toSet()
        assertTrue(expectedDueDay in triggerTimes)
        assertTrue(expectedDayBefore in triggerTimes)
    }

    @Test
    fun notificationsSheetIsInboxOnlyWithSingleLineClearAllAndDirectTapActions() {
        var routedTo: NotificationActionRoute? = null
        var alertsState by mutableStateOf(
            listOf(
                SmartAlert(
                    id = "daily_unlogged_2026-11-26",
                    title = "Today's Entries Not Logged Yet",
                    subtitle = "Daily 9:00 PM reminder — tap to log today's expense or income.",
                    isWarning = false,
                    actionRoute = NotificationActionRoute.QUICK_ADD_TX,
                    category = NotificationCategory.DAILY_REMINDER
                ),
                SmartAlert(
                    id = "budget_high",
                    title = "Monthly Budget Alert (85%)",
                    subtitle = "Spent BDT 8,500 of BDT 10,000 monthly budget. Tap to inspect Insights.",
                    isWarning = true,
                    actionRoute = NotificationActionRoute.OPEN_INSIGHTS_BUDGET,
                    category = NotificationCategory.BUDGET_THRESHOLD
                ),
                SmartAlert(
                    id = "loan_due_11",
                    title = "Due Today (Payable): Rahim",
                    subtitle = "Remaining KWD 58.500 • 10:00 AM reminder. Tap to open Dues & Loans.",
                    isWarning = true,
                    actionRoute = NotificationActionRoute.OPEN_PLAN_LOANS,
                    category = NotificationCategory.DUE_LOAN
                )
            )
        )

        composeRule.setContent {
            AmarHisabTheme(darkTheme = false, isBangla = false) {
                NotificationsSheet(
                    alerts = alertsState,
                    strings = StringsProvider.english,
                    language = AppLanguage.EN,
                    onSelectActionRoute = { routedTo = it },
                    onClearHistory = { alertsState = emptyList() },
                    onDismiss = {}
                )
            }
        }

        composeRule.waitForIdle()

        // 1. Inbox-only role: zero settings toggles or reminder time pickers duplicated inside bell sheet
        composeRule.onNodeWithTag("notifications_inbox_sheet").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_toggle_daily_reminder").assertDoesNotExist()
        composeRule.onNodeWithTag("settings_daily_reminder_time_row").assertDoesNotExist()
        composeRule.onNodeWithTag("settings_toggle_loan_reminder").assertDoesNotExist()
        composeRule.onNodeWithTag("settings_loan_reminder_time_row").assertDoesNotExist()

        // 2. Single-line Clear All button does not wrap onto two lines
        val clearLabelNode = composeRule.onNodeWithTag("notifications_clear_all_label", useUnmergedTree = true)
        clearLabelNode.assertIsDisplayed()
        val clearLayouts = mutableListOf<TextLayoutResult>()
        clearLabelNode.fetchSemanticsNode().config[SemanticsActions.GetTextLayoutResult].action?.invoke(clearLayouts)
        assertTrue(clearLayouts.isNotEmpty())
        val clearBtnBounds = composeRule.onNodeWithTag("notifications_clear_all").getBoundsInRoot()
        val clearLayout = clearLayouts.first()
        assertEquals(1, clearLayout.lineCount)
        assertFalse(clearLayout.isLineEllipsized(0))
        assertFalse(clearLayout.didOverflowHeight)
        assertTrue(
            "Expected Clear All allocated width (${clearLayout.size.width}) to fit full text width (${clearLayout.multiParagraph.maxIntrinsicWidth})",
            clearLayout.size.width >= clearLayout.multiParagraph.maxIntrinsicWidth.toInt()
        )
        assertTrue(
            "Expected Clear All button to meet 48dp accessible touch target height, got ${clearBtnBounds.height}",
            clearBtnBounds.height.value >= 48f
        )

        // 3. Direct action on tap: unlogged daily reminder -> QUICK_ADD_TX, budget -> OPEN_INSIGHTS_BUDGET
        composeRule.onNodeWithTag("notification_item_daily_unlogged_2026-11-26").performClick()
        assertEquals(NotificationActionRoute.QUICK_ADD_TX, routedTo)

        composeRule.onNodeWithTag("notification_item_budget_high").performClick()
        assertEquals(NotificationActionRoute.OPEN_INSIGHTS_BUDGET, routedTo)

        // 4. Tapping Clear All immediately clears inbox to clean empty state and hides Clear All
        composeRule.onNodeWithTag("notifications_clear_all").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("notifications_empty_state").assertIsDisplayed()
        composeRule.onNodeWithTag("notifications_clear_all").assertDoesNotExist()
    }

    @Test
    fun notificationBarShowsBrandedLargeIconOrProfileAvatarAndLauncherIconIsHighResAdaptive() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        Shadows.shadowOf(app).grantPermissions(android.Manifest.permission.POST_NOTIFICATIONS)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // 1. High-resolution lossless 1024x1024 brand master & adaptive launcher icon layers
        val decodeOpts = android.graphics.BitmapFactory.Options().apply { inScaled = false }
        val masterLogo = android.graphics.BitmapFactory.decodeResource(
            context.resources,
            R.drawable.img_amar_hisab_logo,
            decodeOpts
        )
        assertTrue("Expected lossless 1024x1024 app logo master", masterLogo != null && masterLogo.width == 1024 && masterLogo.height == 1024)

        val adaptiveIcon = androidx.core.content.ContextCompat.getDrawable(context, R.mipmap.ic_launcher)
        val adaptiveRoundIcon = androidx.core.content.ContextCompat.getDrawable(context, R.mipmap.ic_launcher_round)
        assertTrue(
            "Expected AdaptiveIconDrawable for ic_launcher on API 34",
            adaptiveIcon is android.graphics.drawable.AdaptiveIconDrawable
        )
        assertTrue(
            "Expected AdaptiveIconDrawable for ic_launcher_round on API 34",
            adaptiveRoundIcon is android.graphics.drawable.AdaptiveIconDrawable
        )

        // 2. Notification without profile photo uses crisp 192x192 Amar Hisab app logo large icon + brand accent color
        val guestPrefs = UserPrefs(language = AppLanguage.EN)
        AmarHisabNotificationManager.postDailyLoggingReminder(context = context, prefs = guestPrefs)

        val notifications = Shadows.shadowOf(nm).allNotifications
        assertTrue(notifications.isNotEmpty())
        val postedLogoNotif = notifications.last()
        assertEquals(
            com.example.notifications.NotificationVisualProvider.BRAND_ACCENT_COLOR,
            postedLogoNotif.color
        )
        assertTrue("Expected branded large icon on system notification", postedLogoNotif.getLargeIcon() != null)
        val resolvedLogo = com.example.notifications.NotificationVisualProvider.resolveNotificationLargeIcon(
            context = context,
            rawAvatarUrl = ""
        )
        assertTrue(
            "Expected crisp 192x192 app logo large icon",
            resolvedLogo != null &&
                resolvedLogo.width == com.example.notifications.NotificationVisualProvider.LARGE_ICON_SIZE_PX &&
                resolvedLogo.height == com.example.notifications.NotificationVisualProvider.LARGE_ICON_SIZE_PX
        )

        // 3. When user has a high-resolution profile image, notification uses the circular 192x192 profile visual
        val avatarFile = java.io.File(context.cacheDir, "test_profile_avatar_hq.png")
        val sourceAvatar = android.graphics.Bitmap.createBitmap(400, 400, android.graphics.Bitmap.Config.ARGB_8888)
        sourceAvatar.eraseColor(android.graphics.Color.rgb(20, 184, 166))
        java.io.FileOutputStream(avatarFile).use { out ->
            sourceAvatar.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
        }
        sourceAvatar.recycle()

        val avatarUri = android.net.Uri.fromFile(avatarFile).toString()
        com.example.notifications.NotificationVisualProvider.syncProfileAvatarCache(context, avatarUri)
        val persistentAvatarCache = java.io.File(context.filesDir, "amar_hisab_notif_avatar.png")
        assertTrue("Expected cached circular profile avatar on disk for offline alarms", persistentAvatarCache.exists() && persistentAvatarCache.length() > 0L)

        val resolvedAvatar = com.example.notifications.NotificationVisualProvider.resolveNotificationLargeIcon(
            context = context,
            rawAvatarUrl = avatarUri
        )
        assertTrue(
            "Expected crisp 192x192 ARGB_8888 circular profile avatar for notification banner",
            resolvedAvatar != null &&
                resolvedAvatar !== resolvedLogo &&
                resolvedAvatar.width == com.example.notifications.NotificationVisualProvider.LARGE_ICON_SIZE_PX &&
                resolvedAvatar.height == com.example.notifications.NotificationVisualProvider.LARGE_ICON_SIZE_PX
        )

        val profilePrefs = UserPrefs(
            language = AppLanguage.EN,
            guestAvatarUrl = avatarUri
        )
        AmarHisabNotificationManager.postDailyLoggingReminder(context = context, prefs = profilePrefs)
        assertTrue(Shadows.shadowOf(nm).allNotifications.last().getLargeIcon() != null)

        // 4. Clearing avatar removes persistent cache and reverts notifications cleanly to the Amar Hisab app logo
        com.example.notifications.NotificationVisualProvider.syncProfileAvatarCache(context, "")
        assertFalse(persistentAvatarCache.exists())
        val revertedLogo = com.example.notifications.NotificationVisualProvider.resolveNotificationLargeIcon(context, "")
        assertTrue(revertedLogo === resolvedLogo)
    }
}
