package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.local.UserPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.TimeZone

/**
 * Delivers scheduled local-timezone reminders when triggered by AlarmManager:
 * 1. Daily Expense & Income Logging Reminder (default 9:00 PM / custom local time)
 *    — only fires when user has completed onboarding AND no transaction was logged today.
 * 2. Dues, Loans & Planned Bills Reminder (10:00 AM local time on due date or 1 day before)
 *    — verifies the loan is still unsettled and formats in its exact currency.
 */
class AmarHisabNotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val pendingResult = goAsync()
        val appContext = context.applicationContext

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val prefsRepo = UserPreferencesRepository(appContext)
                val prefs = prefsRepo.userPrefs.first()
                val db = AppDatabase.getInstance(appContext)
                val nowMillis = System.currentTimeMillis()
                val localTz = TimeZone.getDefault()

                when (action) {
                    AmarHisabNotificationManager.ACTION_DAILY_LOGGING_REMINDER -> {
                        if (prefs.notifDailyReminderEnabled && prefs.hasCompletedWelcome) {
                            val transactions = db.transactionDao().getAllTransactionsOnce()
                            val accounts = db.accountDao().getAllAccountsOnce()
                            // Zero-state discipline: only nudge once user has at least 1 wallet/transaction or completed setup,
                            // and strictly skip if any transaction was already logged today in local timezone.
                            val hasStartedUsingApp = accounts.isNotEmpty() || transactions.isNotEmpty()
                            val loggedToday = NotificationTimeCalculator.hasTransactionLoggedToday(
                                transactions = transactions,
                                nowMillis = nowMillis,
                                timeZone = localTz
                            )
                            if (hasStartedUsingApp && !loggedToday) {
                                val logEntry = AmarHisabNotificationManager.postDailyLoggingReminder(
                                    context = appContext,
                                    prefs = prefs,
                                    nowMillis = nowMillis
                                )
                                prefsRepo.appendNotificationLogEntry(logEntry)
                            }
                        }
                        // Always schedule tomorrow's alarm in the user's current local timezone
                        val loans = db.loanDao().getAllLoansOnce()
                        AmarHisabNotificationManager.scheduleAllReminders(
                            context = appContext,
                            prefs = prefs,
                            loans = loans,
                            nowMillis = nowMillis + 60_000L, // ensure rollover to next day
                            timeZone = localTz
                        )
                    }

                    AmarHisabNotificationManager.ACTION_LOAN_DUE_REMINDER -> {
                        if (!prefs.notifLoanReminders) return@launch
                        val loanId = intent.getLongExtra(AmarHisabNotificationManager.EXTRA_LOAN_ID, -1L)
                        val isDayBefore = intent.getBooleanExtra(AmarHisabNotificationManager.EXTRA_IS_DAY_BEFORE, false)
                        if (loanId <= 0L) return@launch
                        if (isDayBefore && !prefs.notifLoanDayBeforeEnabled) return@launch

                        val loan = db.loanDao().getAllLoansOnce().firstOrNull { it.id == loanId }
                        if (loan != null && !loan.isSettled && loan.remainingAmount > 0.0) {
                            val logEntry = AmarHisabNotificationManager.postLoanDueReminder(
                                context = appContext,
                                prefs = prefs,
                                loan = loan,
                                isDayBefore = isDayBefore,
                                nowMillis = nowMillis
                            )
                            prefsRepo.appendNotificationLogEntry(logEntry)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error handling notification broadcast $action", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "AmarHisabNotifRx"
    }
}
