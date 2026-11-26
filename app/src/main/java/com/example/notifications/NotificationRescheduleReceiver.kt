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
 * Automatically reschedules all Amar Hisab reminders whenever the device boots,
 * updates its app package, or changes system time / timezone (e.g. when traveling
 * between Bangladesh, Kuwait, UAE, Saudi Arabia, Europe, or North America).
 */
class NotificationRescheduleReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action !in SUPPORTED_ACTIONS) return

        val pendingResult = goAsync()
        val appContext = context.applicationContext

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val prefs = UserPreferencesRepository(appContext).userPrefs.first()
                val loans = AppDatabase.getInstance(appContext).loanDao().getAllLoansOnce()
                AmarHisabNotificationManager.scheduleAllReminders(
                    context = appContext,
                    prefs = prefs,
                    loans = loans,
                    nowMillis = System.currentTimeMillis(),
                    timeZone = TimeZone.getDefault()
                )
            } catch (e: Exception) {
                Log.w(TAG, "Failed to reschedule notifications on $action", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "NotifRescheduleRx"
        private val SUPPORTED_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED"
        )
    }
}
