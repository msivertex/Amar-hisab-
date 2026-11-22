package com.example.notifications

import com.example.data.local.TransactionEntity
import com.example.localization.AppLanguage
import com.example.localization.BdtFormatter
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * Pure, deterministic local-timezone scheduling calculator for Amar Hisab notifications.
 * Strictly honors the user's current local device time and timezone so all triggers
 * adapt automatically when traveling or changing device timezone.
 */
object NotificationTimeCalculator {

    const val DEFAULT_DAILY_REMINDER_HOUR = 21   // 9:00 PM local time
    const val DEFAULT_DAILY_REMINDER_MINUTE = 0
    const val DEFAULT_DUE_REMINDER_HOUR = 10     // 10:00 AM local time
    const val DEFAULT_DUE_REMINDER_MINUTE = 0

    /**
     * Computes the next trigger timestamp (epoch millis) for the daily logging reminder
     * at [hour]:[minute]:00.000 in [timeZone]. If today's target time has already passed
     * (<= [nowMillis]), automatically advances to tomorrow in [timeZone].
     */
    fun computeNextDailyReminderMillis(
        nowMillis: Long,
        hour: Int,
        minute: Int,
        timeZone: TimeZone = TimeZone.getDefault()
    ): Long {
        val safeHour = hour.coerceIn(0, 23)
        val safeMinute = minute.coerceIn(0, 59)
        val cal = Calendar.getInstance(timeZone).apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, safeHour)
            set(Calendar.MINUTE, safeMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (cal.timeInMillis <= nowMillis) {
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }
        return cal.timeInMillis
    }

    /**
     * Computes the local-timezone trigger timestamp for a due date or planned bill.
     * Extracts the due date's calendar day in [timeZone], subtracts [daysBefore]
     * (0 = due date itself, 1 = optional 1-day-before reminder), and pins the trigger
     * to [triggerHour]:[triggerMinute] (default 10:00 AM local time).
     */
    fun computeDueDateReminderMillis(
        dueDateMillis: Long,
        daysBefore: Int = 0,
        timeZone: TimeZone = TimeZone.getDefault(),
        triggerHour: Int = DEFAULT_DUE_REMINDER_HOUR,
        triggerMinute: Int = DEFAULT_DUE_REMINDER_MINUTE
    ): Long {
        val safeHour = triggerHour.coerceIn(0, 23)
        val safeMinute = triggerMinute.coerceIn(0, 59)
        val cal = Calendar.getInstance(timeZone).apply {
            timeInMillis = dueDateMillis
            add(Calendar.DAY_OF_MONTH, -daysBefore.coerceAtLeast(0))
            set(Calendar.HOUR_OF_DAY, safeHour)
            set(Calendar.MINUTE, safeMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    /**
     * Returns true if at least one transaction falls within the user's current local calendar day
     * [00:00:00.000, nextDay 00:00:00.000) in [timeZone].
     */
    fun hasTransactionLoggedToday(
        transactions: List<TransactionEntity>,
        nowMillis: Long,
        timeZone: TimeZone = TimeZone.getDefault()
    ): Boolean {
        if (transactions.isEmpty()) return false
        val startCal = Calendar.getInstance(timeZone).apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val dayStart = startCal.timeInMillis
        startCal.add(Calendar.DAY_OF_MONTH, 1)
        val nextDayStart = startCal.timeInMillis
        return transactions.any { it.timestamp in dayStart until nextDayStart }
    }

    /**
     * Returns the signed difference in local calendar days between [dueDateMillis] and [nowMillis]
     * in [timeZone]: 0 = due today, 1 = due tomorrow, negative = overdue by N days.
     */
    fun localCalendarDaysUntil(
        dueDateMillis: Long,
        nowMillis: Long,
        timeZone: TimeZone = TimeZone.getDefault()
    ): Int {
        val todayStart = Calendar.getInstance(timeZone).apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val dueStart = Calendar.getInstance(timeZone).apply {
            timeInMillis = dueDateMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val diffMillis = dueStart.timeInMillis - todayStart.timeInMillis
        // Round to nearest whole day to safely absorb DST +/- 1h transitions
        return kotlin.math.round(diffMillis / 86_400_000.0).toInt()
    }

    /**
     * Stable local calendar day key (YYYY-MM-DD) in [timeZone] for deduplicating daily/threshold alerts.
     */
    fun localDayKey(nowMillis: Long, timeZone: TimeZone = TimeZone.getDefault()): String {
        val cal = Calendar.getInstance(timeZone).apply { timeInMillis = nowMillis }
        return String.format(
            Locale.US,
            "%04d-%02d-%02d",
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    /**
     * Stable local calendar month key (YYYY-MM) in [timeZone].
     */
    fun localMonthKey(nowMillis: Long, timeZone: TimeZone = TimeZone.getDefault()): String {
        val cal = Calendar.getInstance(timeZone).apply { timeInMillis = nowMillis }
        return String.format(
            Locale.US,
            "%04d-%02d",
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1
        )
    }

    /**
     * Formats [hour]:[minute] in natural localized 12-hour format:
     * - Bangla: "রাত ৯:০০টা", "সকাল ১০:০০টা", "দুপুর ২:৩০টা", "সন্ধ্যা ৬:১৫টা"
     * - English: "9:00 PM", "10:00 AM"
     */
    fun formatLocalTimeLabel(hour: Int, minute: Int, language: AppLanguage): String {
        val safeHour = hour.coerceIn(0, 23)
        val safeMinute = minute.coerceIn(0, 59)
        val hour12 = when {
            safeHour == 0 -> 12
            safeHour > 12 -> safeHour - 12
            else -> safeHour
        }
        val minuteStr = String.format(Locale.US, "%02d", safeMinute)
        return if (language == AppLanguage.BN) {
            val period = when (safeHour) {
                in 0..4 -> "রাত"
                in 5..11 -> "সকাল"
                in 12..15 -> "দুপুর"
                in 16..17 -> "বিকাল"
                in 18..19 -> "সন্ধ্যা"
                else -> "রাত"
            }
            val bnDigits = BdtFormatter.toBanglaDigits("$hour12:$minuteStr")
            "$period ${bnDigits}টা"
        } else {
            val amPm = if (safeHour < 12) "AM" else "PM"
            "$hour12:$minuteStr $amPm"
        }
    }
}
