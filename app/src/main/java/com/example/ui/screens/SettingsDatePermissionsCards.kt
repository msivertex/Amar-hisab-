package com.example.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.core.AppPermissionKind
import com.example.core.DevicePermissionsHelper
import com.example.core.DevicePermissionsSnapshot
import com.example.core.DeviceTimeContext
import com.example.core.LocationRegionContext
import com.example.localization.AppLanguage
import com.example.localization.AppStrings
import com.example.ui.components.CrystalCard
import com.example.ui.theme.HindSiliguriFamily
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalCrystalColors

/**
 * Displays the device's active local date, time, and timezone inside the Settings
 * "Date & Time" Crystal Liquid card—reacting live whenever system clock or travel timezone changes.
 */
@Composable
fun LiveDeviceClockSyncBanner(
    timeContext: DeviceTimeContext,
    language: AppLanguage,
    strings: AppStrings,
    onRefreshTime: () -> Unit
) {
    val crystal = LocalCrystalColors.current
    val primary = MaterialTheme.colorScheme.primary
    val dtStrings = strings.dateTimePermissions

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (crystal.isDark) Color.White.copy(alpha = 0.05f) else Color(0xFFF3F8FF),
        border = BorderStroke(1.dp, primary.copy(alpha = if (crystal.isDark) 0.34f else 0.22f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("settings_live_timezone_tile")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(primary.copy(alpha = if (crystal.isDark) 0.22f else 0.13f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Public,
                    contentDescription = dtStrings.liveDeviceClockTitle,
                    tint = primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = dtStrings.liveDeviceClockTitle,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = HindSiliguriFamily
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Surface(
                        shape = CircleShape,
                        color = crystal.assetGreen.copy(alpha = if (crystal.isDark) 0.20f else 0.12f)
                    ) {
                        Text(
                            text = dtStrings.autoTimeZoneBadge,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = HindSiliguriFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 10.sp
                            ),
                            color = crystal.assetGreen,
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = timeContext.formattedLocalDateTime(language),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = HindSiliguriFamily,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = timeContext.displayTimeZoneSummary,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = JetBrainsMonoFamily
                    ),
                    color = crystal.mutedText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("settings_live_timezone_value")
                )
            }

            IconButton(
                onClick = onRefreshTime,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("settings_refresh_time_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = dtStrings.refreshClockDesc,
                    tint = primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * Dedicated Crystal Liquid card in Settings managing Microphone, Camera, and Google Location
 * permissions with clear Bangla/English rationales, live status badges, ON_RESUME sync,
 * and graceful manual/offline guarantees.
 */
@Composable
fun DevicePermissionsSettingsCard(
    strings: AppStrings,
    permissionsOverride: DevicePermissionsSnapshot? = null,
    locationContextOverride: LocationRegionContext? = null,
    onRequestPermissionOverride: ((AppPermissionKind) -> Unit)? = null
) {
    val crystal = LocalCrystalColors.current
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val primary = MaterialTheme.colorScheme.primary
    val dtStrings = strings.dateTimePermissions

    var permissionsState by remember(context, permissionsOverride) {
        mutableStateOf(permissionsOverride ?: DevicePermissionsHelper.inspectPermissions(context))
    }
    var locationContext by remember(locationContextOverride) {
        mutableStateOf(locationContextOverride ?: DevicePermissionsHelper.fallbackTimezoneRegionContext())
    }
    var statusNotice by remember { mutableStateOf<String?>(null) }

    fun refreshLocationNow() {
        if (locationContextOverride != null) {
            locationContext = locationContextOverride
            return
        }
        DevicePermissionsHelper.fetchGoogleLocationContext(context) { resolved ->
            locationContext = resolved
        }
    }

    LaunchedEffect(permissionsOverride) {
        if (permissionsOverride != null) {
            permissionsState = permissionsOverride
        }
    }

    LaunchedEffect(permissionsState.locationGranted, locationContextOverride) {
        if (locationContextOverride != null) {
            locationContext = locationContextOverride
        } else if (permissionsState.locationGranted) {
            refreshLocationNow()
        } else {
            locationContext = DevicePermissionsHelper.fallbackTimezoneRegionContext()
        }
    }

    DisposableEffect(lifecycleOwner, context, permissionsOverride) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && permissionsOverride == null) {
                permissionsState = DevicePermissionsHelper.inspectPermissions(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val micLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionsState = permissionsState.copy(microphoneGranted = granted)
        statusNotice = if (granted) null else dtStrings.microphoneDeniedNotice
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionsState = permissionsState.copy(cameraGranted = granted)
        statusNotice = if (granted) null else dtStrings.cameraDeniedNotice
    }

    val locationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = result[Manifest.permission.ACCESS_COARSE_LOCATION] == true ||
            result[Manifest.permission.ACCESS_FINE_LOCATION] == true
        permissionsState = permissionsState.copy(locationGranted = granted)
        if (granted) {
            statusNotice = null
            refreshLocationNow()
        } else {
            statusNotice = dtStrings.locationDeniedNotice
            locationContext = DevicePermissionsHelper.fallbackTimezoneRegionContext()
        }
    }

    CrystalCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("settings_permissions_card"),
        elevated = true
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.VerifiedUser,
                contentDescription = null,
                tint = primary,
                modifier = Modifier.size(20.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = dtStrings.permissionsSectionTitle,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = HindSiliguriFamily
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = dtStrings.permissionsSectionSubtitle,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = HindSiliguriFamily),
                    color = crystal.mutedText
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        PermissionStatusRow(
            title = dtStrings.microphoneTitle,
            rationale = dtStrings.microphoneRationale,
            icon = Icons.Filled.Mic,
            accent = primary,
            granted = permissionsState.microphoneGranted,
            grantedBadge = dtStrings.permissionGrantedBadge,
            neededBadge = dtStrings.permissionNeededBadge,
            allowLabel = dtStrings.grantPermissionBtn,
            settingsLabel = dtStrings.openSettingsBtn,
            testTagPrefix = "settings_permission_microphone",
            onGrantClick = {
                if (onRequestPermissionOverride != null) {
                    onRequestPermissionOverride(AppPermissionKind.MICROPHONE)
                } else {
                    micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            },
            onOpenSettingsClick = {
                runCatching { context.startActivity(DevicePermissionsHelper.createAppSettingsIntent(context)) }
            }
        )

        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider(thickness = 0.6.dp, color = crystal.glassBorder)
        Spacer(modifier = Modifier.height(10.dp))

        PermissionStatusRow(
            title = dtStrings.cameraTitle,
            rationale = dtStrings.cameraRationale,
            icon = Icons.Filled.PhotoCamera,
            accent = crystal.assetGreen,
            granted = permissionsState.cameraGranted,
            grantedBadge = dtStrings.permissionGrantedBadge,
            neededBadge = dtStrings.permissionNeededBadge,
            allowLabel = dtStrings.grantPermissionBtn,
            settingsLabel = dtStrings.openSettingsBtn,
            testTagPrefix = "settings_permission_camera",
            onGrantClick = {
                if (onRequestPermissionOverride != null) {
                    onRequestPermissionOverride(AppPermissionKind.CAMERA)
                } else {
                    cameraLauncher.launch(Manifest.permission.CAMERA)
                }
            },
            onOpenSettingsClick = {
                runCatching { context.startActivity(DevicePermissionsHelper.createAppSettingsIntent(context)) }
            }
        )

        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider(thickness = 0.6.dp, color = crystal.glassBorder)
        Spacer(modifier = Modifier.height(10.dp))

        PermissionStatusRow(
            title = dtStrings.locationTitle,
            rationale = dtStrings.locationRationale,
            icon = Icons.Filled.LocationOn,
            accent = crystal.warningAmber,
            granted = permissionsState.locationGranted,
            grantedBadge = dtStrings.permissionGrantedBadge,
            neededBadge = dtStrings.permissionNeededBadge,
            allowLabel = dtStrings.grantPermissionBtn,
            settingsLabel = dtStrings.openSettingsBtn,
            testTagPrefix = "settings_permission_location",
            onGrantClick = {
                if (onRequestPermissionOverride != null) {
                    onRequestPermissionOverride(AppPermissionKind.LOCATION)
                } else {
                    locationLauncher.launch(DevicePermissionsHelper.locationPermissions)
                }
            },
            onOpenSettingsClick = {
                runCatching { context.startActivity(DevicePermissionsHelper.createAppSettingsIntent(context)) }
            }
        )

        // Active Travel & Region Context Pill (Google Location when granted, Device Timezone fallback otherwise)
        Spacer(modifier = Modifier.height(10.dp))
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = if (permissionsState.locationGranted) {
                crystal.assetGreen.copy(alpha = if (crystal.isDark) 0.16f else 0.10f)
            } else {
                if (crystal.isDark) Color.White.copy(alpha = 0.05f) else Color(0xFFF4F7FC)
            },
            border = BorderStroke(
                0.8.dp,
                if (permissionsState.locationGranted) crystal.assetGreen.copy(alpha = 0.36f) else crystal.glassBorder
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("settings_location_context_badge")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (permissionsState.locationGranted) Icons.Filled.MyLocation else Icons.Filled.Schedule,
                    contentDescription = null,
                    tint = if (permissionsState.locationGranted) crystal.assetGreen else crystal.mutedText,
                    modifier = Modifier.size(16.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (locationContext.isFromGoogleLocation) {
                            "${dtStrings.locationActiveContextPrefix}: ${locationContext.label}"
                        } else {
                            "${dtStrings.locationFallbackContextPrefix}: ${locationContext.label}"
                        },
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = HindSiliguriFamily
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = locationContext.timeZoneId,
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                        color = crystal.mutedText,
                        maxLines = 1
                    )
                }
                if (permissionsState.locationGranted) {
                    IconButton(
                        onClick = { refreshLocationNow() },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = dtStrings.refreshLocationDesc,
                            tint = crystal.assetGreen,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        if (!statusNotice.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = crystal.warningAmberBg,
                border = BorderStroke(0.8.dp, crystal.warningAmber.copy(alpha = 0.35f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_permission_notice")
            ) {
                Text(
                    text = statusNotice!!,
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = HindSiliguriFamily),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = dtStrings.manualOfflineGuaranteeNote,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = HindSiliguriFamily,
                lineHeight = 16.sp
            ),
            color = crystal.mutedText
        )
    }
}

@Composable
private fun PermissionStatusRow(
    title: String,
    rationale: String,
    icon: ImageVector,
    accent: Color,
    granted: Boolean,
    grantedBadge: String,
    neededBadge: String,
    allowLabel: String,
    settingsLabel: String,
    testTagPrefix: String,
    onGrantClick: () -> Unit,
    onOpenSettingsClick: () -> Unit
) {
    val crystal = LocalCrystalColors.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTagPrefix),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(accent.copy(alpha = if (crystal.isDark) 0.22f else 0.13f))
                    .border(0.8.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(11.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accent,
                    modifier = Modifier.size(19.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = HindSiliguriFamily
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = CircleShape,
                        color = if (granted) {
                            crystal.assetGreen.copy(alpha = if (crystal.isDark) 0.20f else 0.12f)
                        } else {
                            crystal.warningAmberBg
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (granted) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = crystal.assetGreen,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                            Text(
                                text = if (granted) grantedBadge else neededBadge,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = HindSiliguriFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 10.sp
                                ),
                                color = if (granted) crystal.assetGreen else crystal.warningAmber,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
                Text(
                    text = rationale,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = HindSiliguriFamily,
                        lineHeight = 16.sp
                    ),
                    color = crystal.mutedText
                )
            }
        }

        if (!granted) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onGrantClick,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accent),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 44.dp)
                        .testTag("${testTagPrefix}_allow_btn")
                ) {
                    Text(
                        text = allowLabel,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = HindSiliguriFamily
                        ),
                        maxLines = 1
                    )
                }
                OutlinedButton(
                    onClick = onOpenSettingsClick,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier
                        .heightIn(min = 44.dp)
                        .testTag("${testTagPrefix}_settings_btn")
                ) {
                    Text(
                        text = settingsLabel,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFamily = HindSiliguriFamily,
                            fontWeight = FontWeight.SemiBold
                        ),
                        maxLines = 1
                    )
                }
            }
        }
    }
}
