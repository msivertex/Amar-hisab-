package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.core.DevicePermissionsHelper
import com.example.core.DeviceTimeContext
import com.example.data.local.UserPrefs
import com.example.localization.AppLanguage
import com.example.localization.AppStrings
import com.example.notifications.AmarHisabNotificationManager
import com.example.notifications.NotificationTimeCalculator
import com.example.ui.theme.LocalCrystalColors

enum class FinancialAlertDetail {
    DAILY_REMINDER,
    DUES_REMINDER
}

private enum class ReminderTimePickerTarget {
    DAILY_LOGGING,
    DUES_PAYMENT
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsSection(
    prefs: UserPrefs,
    strings: AppStrings,
    @Suppress("UNUSED_PARAMETER") deviceTimeContext: DeviceTimeContext,
    onSetNotifications: (budget: Boolean, credit: Boolean, loan: Boolean) -> Unit,
    onSetDailyReminder: (enabled: Boolean, hour: Int, minute: Int) -> Unit,
    onSetLoanDayBefore: (enabled: Boolean) -> Unit,
    onSetHighSpendAlert: (enabled: Boolean) -> Unit,
    onSetLoanReminderTime: (hour: Int, minute: Int) -> Unit = { _, _ -> },
    activeDetail: FinancialAlertDetail? = null,
    onActiveDetailChange: ((FinancialAlertDetail?) -> Unit)? = null
) {
    val crystal = LocalCrystalColors.current
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val isBn = prefs.language == AppLanguage.BN
    val refined = strings.settingsRefinement

    var internalActiveDetail by rememberSaveable { mutableStateOf<FinancialAlertDetail?>(null) }
    val isHoistedNavigation = onActiveDetailChange != null
    val currentDetail = if (isHoistedNavigation) activeDetail else internalActiveDetail
    val setDetail: (FinancialAlertDetail?) -> Unit = { next ->
        if (onActiveDetailChange != null) {
            onActiveDetailChange(next)
        } else {
            internalActiveDetail = next
        }
    }

    if (!isHoistedNavigation) {
        BackHandler(enabled = currentDetail != null) {
            setDetail(null)
        }
    }

    var isSystemNotifGranted by remember {
        mutableStateOf(DevicePermissionsHelper.isNotificationGranted(context))
    }
    var canScheduleExactAlarms by remember {
        mutableStateOf(DevicePermissionsHelper.canScheduleExactAlarms(context))
    }
    var activeTimePickerTarget by remember { mutableStateOf<ReminderTimePickerTarget?>(null) }

    // Ensure high-priority Android notification channels stay initialized and localized automatically
    LaunchedEffect(context, prefs.language) {
        AmarHisabNotificationManager.ensureChannels(context, prefs.language)
    }

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        isSystemNotifGranted = granted || DevicePermissionsHelper.isNotificationGranted(context)
    }

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isSystemNotifGranted = DevicePermissionsHelper.isNotificationGranted(context)
                canScheduleExactAlarms = DevicePermissionsHelper.canScheduleExactAlarms(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val ensureNotifPermissionIfEnabling: (Boolean) -> Unit = { enabling ->
        if (enabling && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !isSystemNotifGranted) {
            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val formattedDailyTime = remember(prefs.notifDailyReminderHour, prefs.notifDailyReminderMinute, prefs.language) {
        NotificationTimeCalculator.formatLocalTimeLabel(
            hour = prefs.notifDailyReminderHour,
            minute = prefs.notifDailyReminderMinute,
            language = prefs.language
        )
    }
    val formattedLoanTime = remember(prefs.notifLoanReminderHour, prefs.notifLoanReminderMinute, prefs.language) {
        NotificationTimeCalculator.formatLocalTimeLabel(
            hour = prefs.notifLoanReminderHour,
            minute = prefs.notifLoanReminderMinute,
            language = prefs.language
        )
    }

    val dailyReminderTitle = if (isBn) "দৈনন্দিন হিসাব যোগ করার রিমাইন্ডার" else "Daily Expense & Income Reminder"
    val duesReminderTitle = strings.notifLoanReminders

    AnimatedContent(
        targetState = currentDetail,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "financialAlertDetailTransition"
    ) { detail ->
        when (detail) {
            null -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_financial_alerts_main_list"),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Clean Section Header (no manual Test Notification button)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 2.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.NotificationsActive,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = refined.financialAlertsGroupTitle,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // System Notification Permission / Exact Timing Prompts (only shown when OS access is missing)
                    NotificationPermissionBanners(
                        isBn = isBn,
                        isSystemNotifGranted = isSystemNotifGranted,
                        canScheduleExactAlarms = canScheduleExactAlarms,
                        onRequestNotifPermission = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                runCatching {
                                    context.startActivity(DevicePermissionsHelper.createNotificationSettingsIntent(context))
                                }
                            }
                        },
                        onOpenExactAlarmSettings = {
                            runCatching {
                                context.startActivity(DevicePermissionsHelper.createExactAlarmSettingsIntent(context))
                            }
                        }
                    )

                    // 1. Daily Expense & Income Reminder — Clean Main Option (tapping opens detail panel)
                    ReminderOptionNavCard(
                        title = dailyReminderTitle,
                        subtitle = if (prefs.notifDailyReminderEnabled) {
                            if (isBn) {
                                "প্রতিদিন $formattedDailyTime • স্থানীয় সময়"
                            } else {
                                "Every day at $formattedDailyTime • local time"
                            }
                        } else {
                            if (isBn) "রিমাইন্ডার বন্ধ আছে — সময় সেট করতে ট্যাপ করুন" else "Paused — tap to enable and set time"
                        },
                        icon = Icons.Filled.Alarm,
                        iconTint = MaterialTheme.colorScheme.primary,
                        enabled = prefs.notifDailyReminderEnabled,
                        isBn = isBn,
                        cardTestTag = "settings_daily_reminder_card",
                        onClick = { setDetail(FinancialAlertDetail.DAILY_REMINDER) }
                    )

                    // 2. Dues & Payment Reminder — Clean Main Option (tapping opens detail panel)
                    ReminderOptionNavCard(
                        title = duesReminderTitle,
                        subtitle = if (prefs.notifLoanReminders) {
                            if (prefs.notifLoanDayBeforeEnabled) {
                                if (isBn) {
                                    "$formattedLoanTime • ১ দিন আগে আগাম রিমাইন্ডার চালু"
                                } else {
                                    "$formattedLoanTime • 1-day-before reminder on"
                                }
                            } else {
                                if (isBn) {
                                    "নির্ধারিত তারিখে $formattedLoanTime • স্থানীয় সময়"
                                } else {
                                    "Due dates at $formattedLoanTime • local time"
                                }
                            }
                        } else {
                            if (isBn) "রিমাইন্ডার বন্ধ আছে — কনফিগার করতে ট্যাপ করুন" else "Paused — tap to enable and configure"
                        },
                        icon = Icons.AutoMirrored.Filled.EventNote,
                        iconTint = crystal.assetGreen,
                        enabled = prefs.notifLoanReminders,
                        isBn = isBn,
                        cardTestTag = "settings_loan_reminder_card",
                        onClick = { setDetail(FinancialAlertDetail.DUES_REMINDER) }
                    )

                    // 3. Monthly Budget Limit Threshold Alert (Immediate)
                    NotifToggleRow(
                        label = strings.notifBudgetAlerts,
                        subtitle = if (isBn) {
                            "মাসিক বাজেট ৮০% বা ১০০% স্পর্শ করলে তাৎক্ষণিক সতর্কতা"
                        } else {
                            "Immediate alert the moment spending reaches 80% or 100% of monthly budget"
                        },
                        icon = Icons.Filled.PieChart,
                        iconTint = MaterialTheme.colorScheme.primary,
                        checked = prefs.notifBudgetAlerts,
                        testTag = "settings_toggle_budget_alert",
                        onCheckedChange = { enabled ->
                            ensureNotifPermissionIfEnabling(enabled)
                            onSetNotifications(enabled, prefs.notifCreditAlerts, prefs.notifLoanReminders)
                        }
                    )

                    // 4. Credit Card Utilization Alert (Immediate)
                    NotifToggleRow(
                        label = strings.notifCreditAlerts,
                        subtitle = if (isBn) {
                            "ক্রেডিট কার্ড ব্যবহার ৩০% ছাড়ালে তাৎক্ষণিক সতর্কতা"
                        } else {
                            "Immediate alert when credit card utilization crosses 30%"
                        },
                        icon = Icons.Filled.CreditCard,
                        iconTint = crystal.warningAmber,
                        checked = prefs.notifCreditAlerts,
                        testTag = "settings_toggle_credit_alert",
                        onCheckedChange = { enabled ->
                            ensureNotifPermissionIfEnabling(enabled)
                            onSetNotifications(prefs.notifBudgetAlerts, enabled, prefs.notifLoanReminders)
                        }
                    )

                    // 5. High Daily Spend Alert (Immediate)
                    NotifToggleRow(
                        label = if (isBn) "দৈনিক অতিরিক্ত খরচ সতর্কতা" else "High Daily Spend Alert",
                        subtitle = if (isBn) {
                            "একদিনে নির্ধারিত দৈনিক খরচের সীমা ছাড়ালে তাৎক্ষণিক জানাবে"
                        } else {
                            "Immediate alert when today's real spending crosses your daily budget pace"
                        },
                        icon = Icons.Filled.LocalFireDepartment,
                        iconTint = crystal.liabilityRed,
                        checked = prefs.notifHighSpendAlertEnabled,
                        testTag = "settings_toggle_high_spend_alert",
                        onCheckedChange = { enabled ->
                            ensureNotifPermissionIfEnabling(enabled)
                            onSetHighSpendAlert(enabled)
                        }
                    )
                }
            }

            FinancialAlertDetail.DAILY_REMINDER -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_daily_reminder_detail_panel"),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (!isHoistedNavigation) {
                        StandaloneDetailHeader(
                            title = dailyReminderTitle,
                            closeDescription = strings.close,
                            onBack = { setDetail(null) }
                        )
                    }

                    // Permission prompt if system notifications are currently blocked
                    NotificationPermissionBanners(
                        isBn = isBn,
                        isSystemNotifGranted = isSystemNotifGranted,
                        canScheduleExactAlarms = canScheduleExactAlarms,
                        onRequestNotifPermission = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                runCatching {
                                    context.startActivity(DevicePermissionsHelper.createNotificationSettingsIntent(context))
                                }
                            }
                        },
                        onOpenExactAlarmSettings = {
                            runCatching {
                                context.startActivity(DevicePermissionsHelper.createExactAlarmSettingsIntent(context))
                            }
                        }
                    )

                    // Integrated Daily Reminder Switch + Daily Reminder Time right below it
                    Surface(
                        color = crystal.glassSurfaceElevated,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, crystal.glassBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 60.dp)
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Alarm,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(9.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = dailyReminderTitle,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (isBn) {
                                            "সারাদিন কোনো হিসাব না লিখলে প্রতিদিন $formattedDailyTime-য় ফোনের নোটিফিকেশন বারে মনে করিয়ে দেবে"
                                        } else {
                                            "Reminds you in your status bar at $formattedDailyTime local time if no entry is logged that day"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = crystal.mutedText
                                    )
                                }
                                Switch(
                                    checked = prefs.notifDailyReminderEnabled,
                                    onCheckedChange = { enabled ->
                                        ensureNotifPermissionIfEnabling(enabled)
                                        onSetDailyReminder(
                                            enabled,
                                            prefs.notifDailyReminderHour,
                                            prefs.notifDailyReminderMinute
                                        )
                                    },
                                    modifier = Modifier.testTag("settings_toggle_daily_reminder")
                                )
                            }

                            HorizontalDivider(
                                color = crystal.glassBorder.copy(alpha = 0.65f),
                                thickness = 0.8.dp,
                                modifier = Modifier.padding(horizontal = 14.dp)
                            )

                            NestedReminderTimeRow(
                                title = if (isBn) "দৈনিক রিমাইন্ডারের সময়" else "Daily Reminder Time",
                                subtitle = if (isBn) {
                                    "ডিফল্ট: রাত ৯:০০টা"
                                } else {
                                    "Default: 9:00 PM"
                                },
                                formattedTime = formattedDailyTime,
                                accentColor = MaterialTheme.colorScheme.primary,
                                badgeContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                badgeContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                dimmed = !prefs.notifDailyReminderEnabled,
                                testTag = "settings_daily_reminder_time_row",
                                onClick = { activeTimePickerTarget = ReminderTimePickerTarget.DAILY_LOGGING }
                            )
                        }
                    }
                }
            }

            FinancialAlertDetail.DUES_REMINDER -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_loan_reminder_detail_panel"),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (!isHoistedNavigation) {
                        StandaloneDetailHeader(
                            title = duesReminderTitle,
                            closeDescription = strings.close,
                            onBack = { setDetail(null) }
                        )
                    }

                    NotificationPermissionBanners(
                        isBn = isBn,
                        isSystemNotifGranted = isSystemNotifGranted,
                        canScheduleExactAlarms = canScheduleExactAlarms,
                        onRequestNotifPermission = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                runCatching {
                                    context.startActivity(DevicePermissionsHelper.createNotificationSettingsIntent(context))
                                }
                            }
                        },
                        onOpenExactAlarmSettings = {
                            runCatching {
                                context.startActivity(DevicePermissionsHelper.createExactAlarmSettingsIntent(context))
                            }
                        }
                    )

                    // Integrated Dues & Payment Switch + 1-Day-Before Reminder + Dues Reminder Time
                    Surface(
                        color = crystal.glassSurfaceElevated,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, crystal.glassBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 60.dp)
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    color = crystal.assetGreen.copy(alpha = 0.14f),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.EventNote,
                                        contentDescription = null,
                                        tint = crystal.assetGreen,
                                        modifier = Modifier.padding(9.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = duesReminderTitle,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (isBn) {
                                            "নির্ধারিত তারিখে $formattedLoanTime-য় সব সক্রিয় মুদ্রার (BDT, KWD, SAR) দেনা-পাওনার রিমাইন্ডার"
                                        } else {
                                            "$formattedLoanTime local-time alerts on due dates across active currencies (BDT, KWD, SAR, etc.)"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = crystal.mutedText
                                    )
                                }
                                Switch(
                                    checked = prefs.notifLoanReminders,
                                    onCheckedChange = { enabled ->
                                        ensureNotifPermissionIfEnabling(enabled)
                                        onSetNotifications(prefs.notifBudgetAlerts, prefs.notifCreditAlerts, enabled)
                                    },
                                    modifier = Modifier.testTag("settings_toggle_loan_reminder"),
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                        checkedTrackColor = crystal.assetGreen
                                    )
                                )
                            }

                            HorizontalDivider(
                                color = crystal.glassBorder.copy(alpha = 0.65f),
                                thickness = 0.8.dp,
                                modifier = Modifier.padding(horizontal = 14.dp)
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 56.dp)
                                    .alpha(if (prefs.notifLoanReminders) 1f else 0.6f)
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                                    .testTag("settings_loan_day_before_row"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Schedule,
                                        contentDescription = null,
                                        tint = crystal.assetGreen,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Column {
                                        Text(
                                            text = if (isBn) "১ দিন আগে আগাম রিমাইন্ডার" else "1-Day-Before Reminder",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = if (isBn) {
                                                "তারিখের আগের দিন $formattedLoanTime-য় মনে করিয়ে দেবে"
                                            } else {
                                                "Also remind at $formattedLoanTime the day before"
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = crystal.mutedText
                                        )
                                    }
                                }
                                Switch(
                                    checked = prefs.notifLoanDayBeforeEnabled,
                                    onCheckedChange = { enabled ->
                                        ensureNotifPermissionIfEnabling(enabled)
                                        onSetLoanDayBefore(enabled)
                                    },
                                    modifier = Modifier.testTag("settings_toggle_loan_day_before"),
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                        checkedTrackColor = crystal.assetGreen
                                    )
                                )
                            }

                            HorizontalDivider(
                                color = crystal.glassBorder.copy(alpha = 0.65f),
                                thickness = 0.8.dp,
                                modifier = Modifier.padding(horizontal = 14.dp)
                            )

                            NestedReminderTimeRow(
                                title = if (isBn) "দেনা-পাওনা রিমাইন্ডারের সময়" else "Dues Reminder Time",
                                subtitle = if (isBn) {
                                    "নির্ধারিত তারিখ ও ১ দিন আগের রিমাইন্ডারে প্রযোজ্য"
                                } else {
                                    "Applies to due-day & 1-day-before alerts"
                                },
                                formattedTime = formattedLoanTime,
                                accentColor = crystal.assetGreen,
                                badgeContainerColor = crystal.assetGreenBg,
                                badgeContentColor = crystal.assetGreen,
                                dimmed = !prefs.notifLoanReminders,
                                testTag = "settings_loan_reminder_time_row",
                                onClick = { activeTimePickerTarget = ReminderTimePickerTarget.DUES_PAYMENT }
                            )
                        }
                    }
                }
            }
        }
    }

    activeTimePickerTarget?.let { target ->
        val isDailyTarget = target == ReminderTimePickerTarget.DAILY_LOGGING
        val timePickerState = rememberTimePickerState(
            initialHour = if (isDailyTarget) prefs.notifDailyReminderHour else prefs.notifLoanReminderHour,
            initialMinute = if (isDailyTarget) prefs.notifDailyReminderMinute else prefs.notifLoanReminderMinute,
            is24Hour = false
        )
        AlertDialog(
            onDismissRequest = { activeTimePickerTarget = null },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            title = {
                Text(
                    text = if (isDailyTarget) {
                        if (isBn) "দৈনিক রিমাইন্ডারের সময় বেছে নিন" else "Choose Daily Reminder Time"
                    } else {
                        if (isBn) "দেনা-পাওনা রিমাইন্ডারের সময় বেছে নিন" else "Choose Dues Reminder Time"
                    },
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    TimePicker(state = timePickerState)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        ensureNotifPermissionIfEnabling(true)
                        if (isDailyTarget) {
                            onSetDailyReminder(true, timePickerState.hour, timePickerState.minute)
                        } else {
                            onSetLoanReminderTime(timePickerState.hour, timePickerState.minute)
                        }
                        activeTimePickerTarget = null
                    },
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("settings_time_picker_confirm")
                ) {
                    Text(if (isBn) "সংরক্ষণ করুন" else "Save Time")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { activeTimePickerTarget = null },
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text(if (isBn) "বাতিল" else "Cancel")
                }
            }
        )
    }
}

@Composable
private fun StandaloneDetailHeader(
    title: String,
    closeDescription: String,
    onBack: () -> Unit
) {
    val crystal = LocalCrystalColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(48.dp)
                .testTag("settings_alert_detail_back_button")
        ) {
            Surface(
                shape = CircleShape,
                color = crystal.glassSurfaceElevated,
                border = BorderStroke(1.dp, crystal.glassBorder),
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = closeDescription,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun NotificationPermissionBanners(
    isBn: Boolean,
    isSystemNotifGranted: Boolean,
    canScheduleExactAlarms: Boolean,
    onRequestNotifPermission: () -> Unit,
    onOpenExactAlarmSettings: () -> Unit
) {
    val crystal = LocalCrystalColors.current
    if (!isSystemNotifGranted) {
        Surface(
            color = crystal.warningAmberBg,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, crystal.warningAmber.copy(alpha = 0.45f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.NotificationsOff,
                    contentDescription = null,
                    tint = crystal.warningAmber,
                    modifier = Modifier.size(20.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isBn) "সিস্টেম নোটিফিকেশন বন্ধ আছে" else "System Notifications Paused",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isBn) {
                            "অ্যাপ বন্ধ থাকলেও ফোনের স্ট্যাটাস বারে স্বয়ংক্রিয় রিমাইন্ডার পেতে অনুমতি দিন।"
                        } else {
                            "Allow notifications so reminders arrive automatically in your phone's status bar even when the app is closed."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = crystal.mutedText
                    )
                }
                FilledTonalButton(
                    onClick = onRequestNotifPermission,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("settings_notif_permission_button")
                ) {
                    Text(if (isBn) "অনুমতি দিন" else "Allow")
                }
            }
        }
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !canScheduleExactAlarms) {
        Surface(
            color = crystal.glassSurfaceElevated,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("settings_exact_alarm_banner")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Alarm,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isBn) "সঠিক সময়ে অ্যালার্ম অনুমতি" else "Exact Timing Permission",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isBn) {
                            "ব্যাকগ্রাউন্ডে নির্ধারিত মিনিটেই রিমাইন্ডার পেতে অ্যালার্ম অনুমতি চালু করুন।"
                        } else {
                            "Enable Alarms & Reminders access for exact-minute delivery while minimized."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = crystal.mutedText
                    )
                }
                TextButton(
                    onClick = onOpenExactAlarmSettings,
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text(if (isBn) "চালু করুন" else "Enable")
                }
            }
        }
    }
}

@Composable
private fun ReminderOptionNavCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    enabled: Boolean,
    isBn: Boolean,
    cardTestTag: String,
    onClick: () -> Unit
) {
    val crystal = LocalCrystalColors.current
    Surface(
        color = crystal.glassSurfaceElevated,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, crystal.glassBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .testTag(cardTestTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 60.dp)
                .padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                color = iconTint.copy(alpha = 0.14f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(38.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.padding(9.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = crystal.mutedText
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (enabled) iconTint.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(
                        width = 0.8.dp,
                        color = if (enabled) iconTint.copy(alpha = 0.3f) else crystal.glassBorder
                    )
                ) {
                    Text(
                        text = if (enabled) {
                            if (isBn) "চালু" else "ON"
                        } else {
                            if (isBn) "বন্ধ" else "OFF"
                        },
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            platformStyle = PlatformTextStyle(includeFontPadding = false)
                        ),
                        color = if (enabled) iconTint else crystal.mutedText,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = crystal.mutedText,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun NestedReminderTimeRow(
    title: String,
    subtitle: String,
    formattedTime: String,
    accentColor: Color,
    badgeContainerColor: Color,
    badgeContentColor: Color,
    dimmed: Boolean = false,
    testTag: String,
    onClick: () -> Unit
) {
    val crystal = LocalCrystalColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .alpha(if (dimmed) 0.6f else 1f)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.AccessTime,
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier.size(18.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = crystal.mutedText
            )
        }
        Surface(
            color = badgeContainerColor,
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(0.8.dp, accentColor.copy(alpha = 0.25f))
        ) {
            Text(
                text = formattedTime,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                ),
                color = badgeContentColor,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
private fun NotifToggleRow(
    label: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    checked: Boolean,
    testTag: String,
    onCheckedChange: (Boolean) -> Unit
) {
    val crystal = LocalCrystalColors.current
    Surface(
        color = crystal.glassSurfaceElevated,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, crystal.glassBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                color = iconTint.copy(alpha = 0.14f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(38.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.padding(9.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = crystal.mutedText
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                modifier = Modifier.testTag(testTag)
            )
        }
    }
}
