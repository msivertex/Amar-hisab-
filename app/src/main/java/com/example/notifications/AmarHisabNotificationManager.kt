package com.example.notifications

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R
import com.example.core.DevicePermissionsHelper
import com.example.data.local.AccountEntity
import com.example.data.local.LoanEntity
import com.example.data.local.TransactionEntity
import com.example.data.local.UserPrefs
import com.example.data.model.AccountType
import com.example.data.model.LoanType
import com.example.localization.AppLanguage
import com.example.localization.BdtFormatter
import java.util.Calendar
import java.util.TimeZone

/**
 * Central Android notification manager and local-timezone scheduler for "আমার হিসাব" (Amar Hisab).
 *
 * Uses Google Play-compliant inexact Doze-safe alarms (`AlarmManager.setAndAllowWhileIdle`)
 * paired with `TIMEZONE_CHANGED` / `TIME_SET` / `BOOT_COMPLETED` rescheduling so all
 * reminders strictly follow each user's local device clock and timezone without requiring
 * restricted exact-alarm permissions.
 */
object AmarHisabNotificationManager {

    private const val TAG = "AmarHisabNotifMgr"

    const val CHANNEL_DAILY_REMINDER = "amar_hisab_daily_reminder_high"
    const val CHANNEL_DUES_REMINDERS = "amar_hisab_dues_reminders"
    const val CHANNEL_THRESHOLD_ALERTS = "amar_hisab_threshold_alerts"

    const val ACTION_DAILY_LOGGING_REMINDER = "com.example.action.DAILY_LOGGING_REMINDER"
    const val ACTION_LOAN_DUE_REMINDER = "com.example.action.LOAN_DUE_REMINDER"

    const val EXTRA_NOTIFICATION_ROUTE = "extra_notification_route"
    const val EXTRA_LOAN_ID = "extra_loan_id"
    const val EXTRA_IS_DAY_BEFORE = "extra_is_day_before"

    private const val REQ_CODE_DAILY_REMINDER = 41001
    private const val REQ_CODE_LOAN_DUE_BASE = 42000
    private const val REQ_CODE_LOAN_DAY_BEFORE_BASE = 46000

    private const val NOTIF_ID_DAILY_REMINDER = 51001
    private const val NOTIF_ID_BUDGET_ALERT = 52001
    private const val NOTIF_ID_HIGH_SPEND = 52002
    private const val NOTIF_ID_CREDIT_BASE = 53000
    private const val NOTIF_ID_LOAN_BASE = 54000

    private const val PREFS_SCHEDULER = "amar_hisab_notif_scheduler_prefs"
    private const val KEY_SCHEDULED_LOAN_IDS = "scheduled_loan_ids"
    private const val KEY_DEDUP_PREFIX = "dedup_"

    fun ensureChannels(context: Context, language: AppLanguage = AppLanguage.BN) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        val isBn = language == AppLanguage.BN

        val dailyChannel = NotificationChannel(
            CHANNEL_DAILY_REMINDER,
            if (isBn) "দৈনন্দিন হিসাব রিমাইন্ডার" else "Daily Logging Reminders",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = if (isBn) {
                "প্রতিদিন নির্ধারিত স্থানীয় সময়ে আয়-ব্যয় যোগ করার রিমাইন্ডার।"
            } else {
                "Daily local-time reminder to log expenses and income."
            }
            enableVibration(true)
            enableLights(true)
            lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
        }

        val duesChannel = NotificationChannel(
            CHANNEL_DUES_REMINDERS,
            if (isBn) "দেনা-পাওনা ও বিল রিমাইন্ডার" else "Dues, Loans & Planned Bills",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = if (isBn) {
                "নির্ধারিত স্থানীয় সময়ে দেনা-পাওনা ও বিলের রিমাইন্ডার।"
            } else {
                "Scheduled local-time reminders for dues, loans, and planned bills."
            }
            enableVibration(true)
            enableLights(true)
            lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
        }

        val thresholdChannel = NotificationChannel(
            CHANNEL_THRESHOLD_ALERTS,
            if (isBn) "বাজেট ও অতিরিক্ত খরচ সতর্কতা" else "Budget & High Spend Alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = if (isBn) {
                "বাজেট সীমা, কার্ড ব্যবহার বা দৈনিক অতিরিক্ত খরচের তাৎক্ষণিক সতর্কতা।"
            } else {
                "Immediate alerts when spending reaches budget, card, or daily limits."
            }
            enableVibration(true)
            enableLights(true)
            lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
        }

        nm.createNotificationChannels(listOf(dailyChannel, duesChannel, thresholdChannel))
    }

    /**
     * Schedules (or cancels) all local-timezone recurring & due-date alarms using [timeZone].
     * Safe to invoke repeatedly whenever preferences, loans, or device timezone change.
     */
    fun scheduleAllReminders(
        context: Context,
        prefs: UserPrefs,
        loans: List<LoanEntity>,
        nowMillis: Long = System.currentTimeMillis(),
        timeZone: TimeZone = TimeZone.getDefault()
    ) {
        ensureChannels(context, prefs.language)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        // 1. Daily Expense & Income Logging Reminder (default 9:00 PM / custom user local time)
        if (prefs.notifDailyReminderEnabled) {
            val triggerAt = NotificationTimeCalculator.computeNextDailyReminderMillis(
                nowMillis = nowMillis,
                hour = prefs.notifDailyReminderHour,
                minute = prefs.notifDailyReminderMinute,
                timeZone = timeZone
            )
            val pi = buildDailyReminderPendingIntent(context)
            scheduleDozeSafeAlarm(alarmManager, triggerAt, pi)
        } else {
            alarmManager.cancel(buildDailyReminderPendingIntent(context))
        }

        // 2. Dues, Loans & Planned Bills Reminders (10:00 AM local time on due date + optional 1 day before)
        val store = context.getSharedPreferences(PREFS_SCHEDULER, Context.MODE_PRIVATE)
        val previouslyScheduledIds = store.getStringSet(KEY_SCHEDULED_LOAN_IDS, emptySet())
            .orEmpty()
            .mapNotNull { it.toLongOrNull() }
            .toSet()

        val activeLoanIds = mutableSetOf<Long>()
        if (prefs.notifLoanReminders) {
            loans.filter { !it.isSettled && it.dueDateMillis != null }.forEach { loan ->
                val dueMillis = loan.dueDateMillis ?: return@forEach
                activeLoanIds += loan.id

            val dueDayTrigger = NotificationTimeCalculator.computeDueDateReminderMillis(
                    dueDateMillis = dueMillis,
                    daysBefore = 0,
                    timeZone = timeZone,
                    triggerHour = prefs.notifLoanReminderHour,
                    triggerMinute = prefs.notifLoanReminderMinute
                )
                val dueDayPi = buildLoanReminderPendingIntent(context, loan.id, isDayBefore = false)
                if (dueDayTrigger > nowMillis) {
                    scheduleDozeSafeAlarm(alarmManager, triggerAtMillis = dueDayTrigger, pendingIntent = dueDayPi)
                } else {
                    alarmManager.cancel(dueDayPi)
                }

                val dayBeforePi = buildLoanReminderPendingIntent(context, loan.id, isDayBefore = true)
                if (prefs.notifLoanDayBeforeEnabled) {
                    val dayBeforeTrigger = NotificationTimeCalculator.computeDueDateReminderMillis(
                        dueDateMillis = dueMillis,
                        daysBefore = 1,
                        timeZone = timeZone,
                        triggerHour = prefs.notifLoanReminderHour,
                        triggerMinute = prefs.notifLoanReminderMinute
                    )
                    if (dayBeforeTrigger > nowMillis) {
                        scheduleDozeSafeAlarm(alarmManager, triggerAtMillis = dayBeforeTrigger, pendingIntent = dayBeforePi)
                    } else {
                        alarmManager.cancel(dayBeforePi)
                    }
                } else {
                    alarmManager.cancel(dayBeforePi)
                }
            }
        }

        // Cancel stale alarms for settled, deleted, or disabled loans
        val staleIds = previouslyScheduledIds - activeLoanIds
        staleIds.forEach { oldId ->
            alarmManager.cancel(buildLoanReminderPendingIntent(context, oldId, isDayBefore = false))
            alarmManager.cancel(buildLoanReminderPendingIntent(context, oldId, isDayBefore = true))
        }
        store.edit()
            .putStringSet(KEY_SCHEDULED_LOAN_IDS, activeLoanIds.map { it.toString() }.toSet())
            .apply()
    }

    /**
     * Evaluates immediate threshold conditions (budget limit, credit card utilization, high daily spend)
     * after real transactions are recorded. Strictly enforces zero-state discipline — never fires when
     * no real transactions exist. Returns newly triggered [NotificationLogItem]s for in-app history.
     */
    fun evaluateAndTriggerImmediateThresholdAlerts(
        context: Context,
        prefs: UserPrefs,
        accounts: List<AccountEntity>,
        transactions: List<TransactionEntity>,
        spentThisMonth: Double,
        todaySpend: Double,
        nowMillis: Long = System.currentTimeMillis(),
        timeZone: TimeZone = TimeZone.getDefault()
    ): List<NotificationLogItem> {
        // Zero-state discipline: never emit threshold alerts when there is no real activity
        if (transactions.isEmpty() && accounts.isEmpty()) return emptyList()

        ensureChannels(context, prefs.language)
        val isBn = prefs.language == AppLanguage.BN
        val mainCurrency = prefs.mainCurrencyCode
        val dayKey = NotificationTimeCalculator.localDayKey(nowMillis, timeZone)
        val monthKey = NotificationTimeCalculator.localMonthKey(nowMillis, timeZone)
        val store = context.getSharedPreferences(PREFS_SCHEDULER, Context.MODE_PRIVATE)
        val newlyTriggered = mutableListOf<NotificationLogItem>()

        // 1. Monthly Budget Limit Threshold Alert (80% warning & 100% exceeded)
        if (prefs.notifBudgetAlerts && prefs.monthlyBudget > 0.0 && spentThisMonth > 0.0) {
            val pct = ((spentThisMonth / prefs.monthlyBudget) * 100.0).toInt()
            val tier = when {
                pct >= 100 -> "100"
                pct >= 80 -> "80"
                else -> null
            }
            if (tier != null) {
                val dedupKey = "${KEY_DEDUP_PREFIX}budget_${monthKey}_$tier"
                if (!store.getBoolean(dedupKey, false)) {
                    store.edit().putBoolean(dedupKey, true).apply()
                    val spentStr = BdtFormatter.formatCurrency(spentThisMonth, mainCurrency, prefs.language)
                    val budgetStr = BdtFormatter.formatCurrency(prefs.monthlyBudget, mainCurrency, prefs.language)
                    val pctLabel = if (isBn) BdtFormatter.toBanglaDigits("$pct%") else "$pct%"
                    val title = if (pct >= 100) {
                        if (isBn) "মাসিক বাজেট সীমা অতিক্রম করেছে ($pctLabel)" else "Monthly Budget Limit Reached ($pctLabel)"
                    } else {
                        if (isBn) "মাসিক বাজেট সতর্কতা ($pctLabel)" else "Monthly Budget Warning ($pctLabel)"
                    }
                    val body = if (isBn) {
                        "এই মাসে $spentStr খরচ হয়েছে (বাজেট $budgetStr)। বিস্তারিত দেখতে ট্যাপ করুন।"
                    } else {
                        "Spent $spentStr of your $budgetStr monthly budget. Tap to review insights."
                    }
                    postSystemNotification(
                        context = context,
                        notificationId = NOTIF_ID_BUDGET_ALERT,
                        channelId = CHANNEL_THRESHOLD_ALERTS,
                        title = title,
                        body = body,
                        route = NotificationActionRoute.OPEN_INSIGHTS_BUDGET,
                        avatarUrl = prefs.effectiveAvatarUrl
                    )
                    newlyTriggered += NotificationLogItem(
                        id = "threshold_budget_${monthKey}_$tier",
                        title = title,
                        body = body,
                        category = NotificationCategory.BUDGET_THRESHOLD,
                        route = NotificationActionRoute.OPEN_INSIGHTS_BUDGET,
                        timestampMillis = nowMillis,
                        isWarning = true
                    )
                }
            }
        }

        // 2. Credit Card Utilization Alert (> 30%)
        if (prefs.notifCreditAlerts) {
            accounts.filter { it.type == AccountType.CREDIT_CARD && it.creditLimit > 0.0 }.forEach { cc ->
                val util = cc.utilizationPercent
                if (util > 30) {
                    val dedupKey = "${KEY_DEDUP_PREFIX}cc_${cc.id}_$dayKey"
                    if (!store.getBoolean(dedupKey, false)) {
                        store.edit().putBoolean(dedupKey, true).apply()
                        val utilLabel = if (isBn) BdtFormatter.toBanglaDigits("$util%") else "$util%"
                        val balStr = BdtFormatter.formatCurrency(cc.balance, cc.currencyCode, prefs.language)
                        val title = if (isBn) {
                            "${cc.name} কার্ড ব্যবহার $utilLabel"
                        } else {
                            "${cc.name} Card Utilization at $utilLabel"
                        }
                        val body = if (isBn) {
                            "বর্তমান বকেয়া $balStr। নিরাপদ সীমার জন্য ৩০%-এর নিচে রাখুন।"
                        } else {
                            "Current balance $balStr. Keep utilization below 30% for healthy credit."
                        }
                        postSystemNotification(
                            context = context,
                            notificationId = NOTIF_ID_CREDIT_BASE + (cc.id % 900).toInt(),
                            channelId = CHANNEL_THRESHOLD_ALERTS,
                            title = title,
                            body = body,
                            route = NotificationActionRoute.OPEN_ACCOUNTS,
                            avatarUrl = prefs.effectiveAvatarUrl
                        )
                        newlyTriggered += NotificationLogItem(
                            id = "threshold_cc_${cc.id}_$dayKey",
                            title = title,
                            body = body,
                            category = NotificationCategory.HIGH_SPEND_CARD,
                            route = NotificationActionRoute.OPEN_ACCOUNTS,
                            timestampMillis = nowMillis,
                            isWarning = true
                        )
                    }
                }
            }
        }

        // 3. High Daily Spend Alert (triggers immediately when today's spend crosses configured daily limit
        //    or 1.5x daily pace of the user's configured monthly budget)
        if (prefs.notifHighSpendAlertEnabled && todaySpend > 0.0) {
            val daysInMonth = Calendar.getInstance(timeZone).apply { timeInMillis = nowMillis }
                .getActualMaximum(Calendar.DAY_OF_MONTH)
                .coerceAtLeast(28)
            val effectiveDailyLimit = when {
                prefs.notifDailySpendLimit > 0.0 -> prefs.notifDailySpendLimit
                prefs.monthlyBudget > 0.0 -> (prefs.monthlyBudget / daysInMonth) * 1.5
                else -> 0.0
            }
            if (effectiveDailyLimit > 0.0 && todaySpend >= effectiveDailyLimit) {
                val dedupKey = "${KEY_DEDUP_PREFIX}high_daily_$dayKey"
                if (!store.getBoolean(dedupKey, false)) {
                    store.edit().putBoolean(dedupKey, true).apply()
                    val spentStr = BdtFormatter.formatCurrency(todaySpend, mainCurrency, prefs.language)
                    val limitStr = BdtFormatter.formatCurrency(effectiveDailyLimit, mainCurrency, prefs.language)
                    val title = if (isBn) "আজকের খরচ সীমা ছাড়িয়েছে" else "High Daily Spend Alert"
                    val body = if (isBn) {
                        "আজ ইতিমধ্যে $spentStr খরচ হয়েছে (দৈনিক লক্ষ্য $limitStr)। খরচ পর্যালোচনা করুন।"
                    } else {
                        "You have spent $spentStr today (above daily threshold $limitStr). Tap to review."
                    }
                    postSystemNotification(
                        context = context,
                        notificationId = NOTIF_ID_HIGH_SPEND,
                        channelId = CHANNEL_THRESHOLD_ALERTS,
                        title = title,
                        body = body,
                        route = NotificationActionRoute.OPEN_INSIGHTS_BUDGET,
                        avatarUrl = prefs.effectiveAvatarUrl
                    )
                    newlyTriggered += NotificationLogItem(
                        id = "threshold_daily_$dayKey",
                        title = title,
                        body = body,
                        category = NotificationCategory.HIGH_SPEND_CARD,
                        route = NotificationActionRoute.OPEN_INSIGHTS_BUDGET,
                        timestampMillis = nowMillis,
                        isWarning = true
                    )
                }
            }
        }

        return newlyTriggered
    }

    /**
     * Builds and delivers the Daily Expense & Income Logging Reminder when no transaction
     * has been logged for today in the user's local timezone.
     */
    fun postDailyLoggingReminder(
        context: Context,
        prefs: UserPrefs,
        nowMillis: Long = System.currentTimeMillis()
    ): NotificationLogItem {
        ensureChannels(context, prefs.language)
        val isBn = prefs.language == AppLanguage.BN
        val title = if (isBn) "আজকের আয়-ব্যয় যোগ করতে ভুলবেন না" else "Log Today's Expenses & Income"
        val body = if (isBn) {
            "আজ এখনও কোনো হিসাব যোগ করা হয়নি। এক ট্যাপে দ্রুত আজকের খরচ বা আয় লিখে রাখুন।"
        } else {
            "No transactions logged today yet. Tap to quickly record today's expenses or income."
        }
        postSystemNotification(
            context = context,
            notificationId = NOTIF_ID_DAILY_REMINDER,
            channelId = CHANNEL_DAILY_REMINDER,
            title = title,
            body = body,
            route = NotificationActionRoute.QUICK_ADD_TX,
            avatarUrl = prefs.effectiveAvatarUrl,
            allowNetworkAvatarFetch = true
        )
        val dayKey = NotificationTimeCalculator.localDayKey(nowMillis, TimeZone.getDefault())
        return NotificationLogItem(
            id = "daily_log_$dayKey",
            title = title,
            body = body,
            category = NotificationCategory.DAILY_REMINDER,
            route = NotificationActionRoute.QUICK_ADD_TX,
            timestampMillis = nowMillis,
            isWarning = false
        )
    }

    /**
     * Builds and delivers a Due-Date or 1-Day-Before reminder for an unsettled loan/due
     * in its exact currency (`BDT`, `KD`, `SAR`, `AED`, etc.).
     */
    fun postLoanDueReminder(
        context: Context,
        prefs: UserPrefs,
        loan: LoanEntity,
        isDayBefore: Boolean,
        nowMillis: Long = System.currentTimeMillis()
    ): NotificationLogItem {
        ensureChannels(context, prefs.language)
        val isBn = prefs.language == AppLanguage.BN
        val formattedRemaining = BdtFormatter.formatCurrency(
            amount = loan.remainingAmount,
            currencyCode = loan.currencyCode,
            language = prefs.language
        )
        val typeLabel = when (loan.loanType) {
            LoanType.I_OWE -> if (isBn) "দেনা পরিশোধ" else "Payable Due"
            LoanType.OWED_TO_ME -> if (isBn) "পাওনা আদায়" else "Receivable Due"
        }
        val title = if (isDayBefore) {
            if (isBn) "আগামীকাল $typeLabel: ${loan.personName}" else "Due Tomorrow ($typeLabel): ${loan.personName}"
        } else {
            if (isBn) "আজ $typeLabel: ${loan.personName}" else "Due Today ($typeLabel): ${loan.personName}"
        }
        val body = if (isDayBefore) {
            if (isBn) {
                "বাকি পরিমাণ $formattedRemaining — আগামীকাল নির্ধারিত তারিখ। দেনা-পাওনা দেখতে ট্যাপ করুন।"
            } else {
                "Remaining $formattedRemaining is due tomorrow. Tap to review or settle in Plan."
            }
        } else {
            if (isBn) {
                "বাকি পরিমাণ $formattedRemaining — আজই পরিশোধ/আদায়ের নির্ধারিত তারিখ। ট্যাপ করুন।"
            } else {
                "Remaining $formattedRemaining is due today. Tap to record settlement."
            }
        }
        val notifId = NOTIF_ID_LOAN_BASE + ((loan.id * 2 + if (isDayBefore) 1 else 0) % 5000).toInt()
        postSystemNotification(
            context = context,
            notificationId = notifId,
            channelId = CHANNEL_DUES_REMINDERS,
            title = title,
            body = body,
            route = NotificationActionRoute.OPEN_PLAN_LOANS,
            avatarUrl = prefs.effectiveAvatarUrl,
            allowNetworkAvatarFetch = true
        )
        return NotificationLogItem(
            id = "loan_${loan.id}_${if (isDayBefore) "day_before" else "due_day"}",
            title = title,
            body = body,
            category = NotificationCategory.DUE_LOAN,
            route = NotificationActionRoute.OPEN_PLAN_LOANS,
            timestampMillis = nowMillis,
            isWarning = !isDayBefore
        )
    }

    @SuppressLint("MissingPermission")
    fun postSystemNotification(
        context: Context,
        notificationId: Int,
        channelId: String,
        title: String,
        body: String,
        route: NotificationActionRoute,
        avatarUrl: String? = null,
        allowNetworkAvatarFetch: Boolean = false
    ): Boolean {
        ensureChannels(context)
        if (!DevicePermissionsHelper.isNotificationGranted(context)) {
            return false
        }
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_NOTIFICATION_ROUTE, route.name)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val largeIconBitmap = NotificationVisualProvider.resolveNotificationLargeIcon(
            context = context,
            rawAvatarUrl = avatarUrl,
            allowNetworkFetch = allowNetworkAvatarFetch
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notif_amar_hisab)
            .setColor(NotificationVisualProvider.BRAND_ACCENT_COLOR)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)

        if (largeIconBitmap != null) {
            builder.setLargeIcon(largeIconBitmap)
        }

        val notification = builder.build()

        return runCatching {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
            true
        }.onFailure { e ->
            Log.w(TAG, "Unable to post system notification", e)
        }.getOrDefault(false)
    }

    fun clearDedupStateForTesting(context: Context) {
        context.getSharedPreferences(PREFS_SCHEDULER, Context.MODE_PRIVATE).edit().clear().apply()
    }

    @SuppressLint("ScheduleExactAlarm")
    private fun scheduleDozeSafeAlarm(
        alarmManager: AlarmManager,
        triggerAtMillis: Long,
        pendingIntent: PendingIntent
    ) {
        runCatching {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerAtMillis,
                            pendingIntent
                        )
                    } else {
                        alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerAtMillis,
                            pendingIntent
                        )
                    }
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                }
                else -> {
                    alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                }
            }
        }.onFailure { e ->
            Log.w(TAG, "Exact alarm failed; falling back to Doze-safe alarm at $triggerAtMillis", e)
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                } else {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                }
            }
        }
    }

    private fun buildDailyReminderPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, AmarHisabNotificationReceiver::class.java).apply {
            action = ACTION_DAILY_LOGGING_REMINDER
        }
        return PendingIntent.getBroadcast(
            context,
            REQ_CODE_DAILY_REMINDER,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun buildLoanReminderPendingIntent(
        context: Context,
        loanId: Long,
        isDayBefore: Boolean
    ): PendingIntent {
        val intent = Intent(context, AmarHisabNotificationReceiver::class.java).apply {
            action = ACTION_LOAN_DUE_REMINDER
            putExtra(EXTRA_LOAN_ID, loanId)
            putExtra(EXTRA_IS_DAY_BEFORE, isDayBefore)
        }
        val base = if (isDayBefore) REQ_CODE_LOAN_DAY_BEFORE_BASE else REQ_CODE_LOAN_DUE_BASE
        val reqCode = base + (loanId % 3500).toInt()
        return PendingIntent.getBroadcast(
            context,
            reqCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
