package com.example

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
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
@Config(sdk = [33])
class NotificationSystemRobolectricTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        AmarHisabNotificationManager.clearDedupStateForTesting(context)
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

        // 4. Multi-currency KWD loan due reminder formats cleanly with KD label
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
        assertTrue("Expected KD formatted amount in '${loanLog.body}'", loanLog.body.contains("KD"))

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

        composeRule.onNodeWithTag("settings_notif_timezone_badge").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_daily_reminder_time_row").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("রাত ৯:০০টা").assertIsDisplayed()

        composeRule.onNodeWithTag("settings_toggle_daily_reminder").performScrollTo().performClick()
        assertEquals(false, dailyToggled)

        composeRule.onNodeWithTag("settings_toggle_loan_day_before").performScrollTo().performClick()
        assertEquals(false, dayBeforeToggled)

        composeRule.onNodeWithTag("settings_toggle_high_spend_alert").performScrollTo().performClick()
        assertEquals(false, highSpendToggled)
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
}
