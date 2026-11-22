package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.core.DevicePermissionsHelper
import com.example.core.DeviceTimeContext
import com.example.data.local.UserPrefs
import com.example.localization.AppLanguage
import com.example.localization.AppStrings
import com.example.notifications.NotificationTimeCalculator
import com.example.ui.theme.LocalCrystalColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsSection(
    prefs: UserPrefs,
    strings: AppStrings,
    deviceTimeContext: DeviceTimeContext,
    onSetNotifications: (budget: Boolean, credit: Boolean, loan: Boolean) -> Unit,
    onSetDailyReminder: (enabled: Boolean, hour: Int, minute: Int) -> Unit,
    onSetLoanDayBefore: (enabled: Boolean) -> Unit,
    onSetHighSpendAlert: (enabled: Boolean) -> Unit
) {
    val crystal = LocalCrystalColors.current
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val isBn = prefs.language == AppLanguage.BN
    val refined = strings.settingsRefinement

    var isSystemNotifGranted by remember {
        mutableStateOf(DevicePermissionsHelper.isNotificationGranted(context))
    }
    var showTimePickerDialog by remember { mutableStateOf(false) }

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        isSystemNotifGranted = granted || DevicePermissionsHelper.isNotificationGranted(context)
    }

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isSystemNotifGranted = DevicePermissionsHelper.isNotificationGranted(context)
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

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Section Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 2.dp)
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
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Active Local Device Timezone Indicator
        Surface(
            color = crystal.glassSurfaceElevated,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, crystal.glassBorder),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("settings_notif_timezone_badge")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Public,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isBn) "ডিভাইসের স্থানীয় সময় ও টাইমজোন অনুযায়ী সক্রিয়" else "Synced to Local Device Time & Timezone",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = deviceTimeContext.displayTimeZoneSummary,
                        style = MaterialTheme.typography.bodySmall,
                        color = crystal.mutedText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // System Notification Permission Prompt (Android 13+ when denied)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !isSystemNotifGranted) {
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
                                "মোবাইল নোটিফিকেশন বারে রিমাইন্ডার পেতে অনুমতি দিন।"
                            } else {
                                "Allow notifications to receive alerts in your phone's status bar."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = crystal.mutedText
                        )
                    }
                    FilledTonalButton(
                        onClick = {
                            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        },
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("settings_notif_permission_button")
                    ) {
                        Text(if (isBn) "অনুমতি দিন" else "Allow")
                    }
                }
            }
        }

        // 1. Daily Expense & Income Logging Reminder + Custom Local Time Picker
        NotifToggleRow(
            label = if (isBn) "দৈনন্দিন হিসাব যোগ করার রিমাইন্ডার" else "Daily Expense & Income Reminder",
            subtitle = if (isBn) {
                "সারাদিন কোনো হিসাব না লিখলে প্রতিদিন $formattedDailyTime-য় রিমাইন্ডার পাঠাবে"
            } else {
                "Reminds you at $formattedDailyTime local time if no entry is logged that day"
            },
            icon = Icons.Filled.Alarm,
            iconTint = MaterialTheme.colorScheme.primary,
            checked = prefs.notifDailyReminderEnabled,
            testTag = "settings_toggle_daily_reminder",
            onCheckedChange = { enabled ->
                ensureNotifPermissionIfEnabling(enabled)
                onSetDailyReminder(enabled, prefs.notifDailyReminderHour, prefs.notifDailyReminderMinute)
            }
        )

        AnimatedVisibility(visible = prefs.notifDailyReminderEnabled) {
            Surface(
                color = crystal.glassSurfaceElevated,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp)
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { showTimePickerDialog = true }
                    .testTag("settings_daily_reminder_time_row")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AccessTime,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Column {
                            Text(
                                text = if (isBn) "দৈনিক রিমাইন্ডারের সময়" else "Daily Reminder Time",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isBn) "ডিফল্ট: রাত ৯:০০টা (স্থানীয় সময়)" else "Default: 9:00 PM (local time)",
                                style = MaterialTheme.typography.bodySmall,
                                color = crystal.mutedText
                            )
                        }
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = formattedDailyTime,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        // 2. Dues, Loans & Planned Bills Reminders (10:00 AM local time + optional 1-day-before)
        NotifToggleRow(
            label = strings.notifLoanReminders,
            subtitle = if (isBn) {
                "নির্ধারিত তারিখে সকাল ১০:০০টায় সব সক্রিয় মুদ্রার (BDT, KD, SAR) দেনা-পাওনার রিমাইন্ডার"
            } else {
                "10:00 AM local-time alerts on due dates across active currencies (BDT, KD, SAR, etc.)"
            },
            icon = Icons.Filled.EventNote,
            iconTint = crystal.assetGreen,
            checked = prefs.notifLoanReminders,
            testTag = "settings_toggle_loan_reminder",
            onCheckedChange = { enabled ->
                ensureNotifPermissionIfEnabling(enabled)
                onSetNotifications(prefs.notifBudgetAlerts, prefs.notifCreditAlerts, enabled)
            }
        )

        AnimatedVisibility(visible = prefs.notifLoanReminders) {
            Surface(
                color = crystal.glassSurfaceElevated,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, crystal.glassBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .padding(horizontal = 14.dp, vertical = 8.dp),
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
                                text = if (isBn) "তারিখের আগের দিন সকাল ১০:০০টায় মনে করিয়ে দেবে" else "Also remind at 10:00 AM the day before",
                                style = MaterialTheme.typography.bodySmall,
                                color = crystal.mutedText
                            )
                        }
                    }
                    Switch(
                        checked = prefs.notifLoanDayBeforeEnabled,
                        onCheckedChange = onSetLoanDayBefore,
                        modifier = Modifier.testTag("settings_toggle_loan_day_before"),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            checkedTrackColor = crystal.assetGreen
                        )
                    )
                }
            }
        }

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

    if (showTimePickerDialog) {
        val timePickerState = rememberTimePickerState(
            initialHour = prefs.notifDailyReminderHour,
            initialMinute = prefs.notifDailyReminderMinute,
            is24Hour = false
        )
        AlertDialog(
            onDismissRequest = { showTimePickerDialog = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            title = {
                Text(
                    text = if (isBn) "দৈনিক রিমাইন্ডারের সময় বেছে নিন" else "Choose Daily Reminder Time",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (isBn) {
                            "স্থানীয় টাইমজোন: ${deviceTimeContext.displayTimeZoneSummary}"
                        } else {
                            "Local timezone: ${deviceTimeContext.displayTimeZoneSummary}"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = crystal.mutedText
                    )
                    TimePicker(state = timePickerState)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSetDailyReminder(true, timePickerState.hour, timePickerState.minute)
                        showTimePickerDialog = false
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
                    onClick = { showTimePickerDialog = false },
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text(if (isBn) "বাতিল" else "Cancel")
                }
            }
        )
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
