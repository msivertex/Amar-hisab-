package com.example.core

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import com.example.data.market.MarketCalculators
import com.example.localization.AppLanguage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs

/**
 * Snapshot of the device's live local date, time, and timezone.
 * Never hardcodes a fixed timezone — always reflects active device settings and travel changes.
 */
data class DeviceTimeContext(
    val epochMillis: Long = System.currentTimeMillis(),
    val timeZoneId: String = TimeZone.getDefault().id,
    val offsetMillis: Int = TimeZone.getDefault().getOffset(System.currentTimeMillis())
) {
    val timeZone: TimeZone
        get() = TimeZone.getTimeZone(timeZoneId)

    val utcOffsetLabel: String
        get() {
            val sign = if (offsetMillis >= 0) "+" else "-"
            val totalMinutes = abs(offsetMillis) / 60_000
            val hours = totalMinutes / 60
            val mins = totalMinutes % 60
            return String.format(Locale.US, "UTC%s%02d:%02d", sign, hours, mins)
        }

    val displayTimeZoneSummary: String
        get() = "$timeZoneId • $utcOffsetLabel"

    val dayKey: String
        get() = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = this@DeviceTimeContext.timeZone
        }.format(Date(epochMillis))

    fun formattedLocalDateTime(language: AppLanguage): String {
        val locale = Locale.forLanguageTag(language.localeTag)
        val fmt = SimpleDateFormat("EEE, dd MMM yyyy • hh:mm a", locale).apply {
            timeZone = this@DeviceTimeContext.timeZone
        }
        val text = fmt.format(Date(epochMillis))
        return if (language == AppLanguage.BN) MarketCalculators.toBanglaDigits(text) else text
    }

    fun formattedLocalDate(language: AppLanguage): String {
        val locale = Locale.forLanguageTag(language.localeTag)
        val fmt = SimpleDateFormat("EEE, dd MMM yyyy", locale).apply {
            timeZone = this@DeviceTimeContext.timeZone
        }
        val text = fmt.format(Date(epochMillis))
        return if (language == AppLanguage.BN) MarketCalculators.toBanglaDigits(text) else text
    }

    companion object {
        fun current(
            nowMillis: Long = System.currentTimeMillis(),
            timeZone: TimeZone = TimeZone.getDefault()
        ): DeviceTimeContext = DeviceTimeContext(
            epochMillis = nowMillis,
            timeZoneId = timeZone.id,
            offsetMillis = timeZone.getOffset(nowMillis)
        )
    }
}

/**
 * Observes Android system time, date, and timezone broadcasts (ACTION_TIME_CHANGED,
 * ACTION_TIMEZONE_CHANGED, ACTION_DATE_CHANGED) so date boundaries, weekly summaries,
 * and timestamps react immediately when traveling across regions or crossing midnight.
 */
class SystemClockMonitor(context: Context) {

    private val appContext = context.applicationContext
    private val _timeContext = MutableStateFlow(DeviceTimeContext.current())
    val timeContext: StateFlow<DeviceTimeContext> = _timeContext.asStateFlow()

    private var registered = false

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            refreshNow()
        }
    }

    fun start() {
        if (registered) return
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
            addAction(Intent.ACTION_DATE_CHANGED)
        }
        runCatching {
            ContextCompat.registerReceiver(
                appContext,
                receiver,
                filter,
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
            registered = true
        }
        refreshNow()
    }

    fun stop() {
        if (!registered) return
        runCatching {
            appContext.unregisterReceiver(receiver)
        }
        registered = false
    }

    fun refreshNow(
        nowMillis: Long = System.currentTimeMillis(),
        timeZone: TimeZone = TimeZone.getDefault()
    ): DeviceTimeContext {
        val updated = DeviceTimeContext.current(nowMillis, timeZone)
        _timeContext.value = updated
        return updated
    }
}
