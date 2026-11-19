package com.example.ui.screens

import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.core.content.FileProvider
import com.example.data.backup.BackupManager
import com.example.data.local.UserPrefs
import com.example.data.model.AuthMode
import com.example.localization.AppStrings
import com.example.ui.components.CrystalCard
import com.example.ui.components.GoogleGLogo
import com.example.ui.theme.LocalCrystalColors
import com.example.ui.viewmodel.BackupMessage
import java.io.File

@Composable
fun BackupRestoreSubPage(
    prefs: UserPrefs,
    backupStatus: BackupMessage?,
    isGoogleSignInLoading: Boolean,
    googleNotice: String?,
    strings: AppStrings,
    onBack: () -> Unit,
    onExportJsonClick: () -> Unit,
    onExportCsvClick: () -> Unit,
    onImportRestoreClick: () -> Unit,
    onDownloadSampleTemplateClick: () -> Unit,
    onTriggerGoogleSignIn: () -> Unit,
    onDeleteAllFinancialData: () -> Unit,
    onClearBackupStatus: () -> Unit
) {
    BackHandler(onBack = onBack)
    val crystal = LocalCrystalColors.current
    val context = LocalContext.current
    val br = strings.backupRestore

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("backup_restore_screen"),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 620.dp)
                .fillMaxSize()
                .testTag("backup_restore_list"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header: "ব্যাকআপ ও রিস্টোর" + subtitle "ডাটা ব্যাকআপ, রিস্টোর ও সিঙ্ক"
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("backup_restore_back_button")
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = crystal.glassSurfaceElevated,
                            border = BorderStroke(1.dp, crystal.glassBorder),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = strings.close,
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = br.pageTitle,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = br.pageSubtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = crystal.mutedText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Status Banner (Export / Restore / Delete feedback)
            if (backupStatus != null) {
                item {
                    Surface(
                        color = if (backupStatus.isError) crystal.liabilityRedBg else crystal.assetGreenBg,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(
                            width = 1.dp,
                            color = (if (backupStatus.isError) crystal.liabilityRed else crystal.assetGreen)
                                .copy(alpha = 0.35f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("backup_restore_status_banner")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = backupStatus.text,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                color = if (backupStatus.isError) crystal.liabilityRed else crystal.assetGreen,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(
                                onClick = onClearBackupStatus,
                                modifier = Modifier.heightIn(min = 48.dp)
                            ) {
                                Text(strings.close)
                            }
                        }
                    }
                }
            }

            // 1. লোকাল ব্যাকআপ ও রিস্টোর (Local Backup & Restore)
            item {
                CrystalCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("backup_local_section_card"),
                    elevated = true
                ) {
                    Text(
                        text = br.localSectionTitle,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    // Row 1: ডাটা এক্সপোর্ট (JSON / CSV)
                    BackupActionRow(
                        icon = Icons.Filled.FileUpload,
                        iconTint = MaterialTheme.colorScheme.primary,
                        title = br.exportDataTitle,
                        subtitle = br.exportDataSubtitle
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilledTonalButton(
                                onClick = onExportJsonClick,
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primary.copy(
                                        alpha = if (crystal.isDark) 0.22f else 0.14f
                                    ),
                                    contentColor = if (crystal.isDark) Color.White else MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("backup_export_json_button")
                            ) {
                                Text(
                                    text = br.jsonBtnLabel,
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                                )
                            }

                            OutlinedButton(
                                onClick = onExportCsvClick,
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (crystal.isDark) {
                                        Color.White.copy(alpha = 0.24f)
                                    } else {
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                                    }
                                ),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                ),
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("backup_export_csv_button")
                            ) {
                                Text(
                                    text = br.csvBtnLabel,
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = crystal.glassBorder
                    )

                    // Row 2: ইম্পোর্ট ও রিস্টোর (বাছাই)
                    BackupActionRow(
                        icon = Icons.Filled.FileOpen,
                        iconTint = crystal.assetGreen,
                        title = br.importRestoreTitle,
                        subtitle = br.importRestoreSubtitle
                    ) {
                        FilledTonalButton(
                            onClick = onImportRestoreClick,
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = crystal.assetGreen.copy(
                                    alpha = if (crystal.isDark) 0.22f else 0.14f
                                ),
                                contentColor = if (crystal.isDark) Color.White else crystal.assetGreen
                            ),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("backup_import_restore_button")
                        ) {
                            Text(
                                text = br.chooseFileBtnLabel,
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = crystal.glassBorder
                    )

                    // Row 3: নমুনা টেম্পলেট (ডাউনলোড / শেয়ার)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(crystal.warningAmber.copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Description,
                                    contentDescription = null,
                                    tint = crystal.warningAmber,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = br.sampleTemplateTitle,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = br.sampleTemplateSubtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = crystal.mutedText,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            FilledTonalButton(
                                onClick = onDownloadSampleTemplateClick,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primary.copy(
                                        alpha = if (crystal.isDark) 0.20f else 0.12f
                                    ),
                                    contentColor = if (crystal.isDark) Color.White else MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("backup_template_download_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Download,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = br.downloadBtnLabel,
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                    maxLines = 1
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    shareSampleTemplateCsv(
                                        context = context,
                                        chooserTitle = br.sampleTemplateTitle
                                    )
                                },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (crystal.isDark) {
                                        Color.White.copy(alpha = 0.24f)
                                    } else {
                                        crystal.glassBorder
                                    }
                                ),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = crystal.glassSurfaceElevated,
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("backup_template_share_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Share,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = br.shareBtnLabel,
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            // 2. ক্লাউড ব্যাকআপ ও সিঙ্ক (Cloud Backup & Sync)
            item {
                CrystalCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("backup_cloud_section_card"),
                    elevated = true
                ) {
                    Text(
                        text = br.cloudSectionTitle,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (prefs.authMode == AuthMode.GUEST) {
                        Surface(
                            color = if (crystal.isDark) {
                                Color(0xFF161C2B)
                            } else {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)
                            },
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (crystal.isDark) {
                                    Color.White.copy(alpha = 0.14f)
                                } else {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                                }
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("backup_google_signin_required_card")
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.CloudOff,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = br.googleSignInRequiredTitle,
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = br.googleSignInRequiredSubtitle,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = crystal.mutedText
                                        )
                                    }
                                }

                                Button(
                                    onClick = onTriggerGoogleSignIn,
                                    enabled = !isGoogleSignInLoading,
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = Color.White
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                        .testTag("backup_cloud_google_signin_button")
                                ) {
                                    if (isGoogleSignInLoading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            strokeWidth = 2.dp,
                                            color = Color.White
                                        )
                                    } else {
                                        GoogleGLogo(size = 18.dp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = strings.linkGoogleAccountBtn,
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Surface(
                            color = crystal.assetGreenBg,
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, crystal.assetGreen.copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(crystal.assetGreen.copy(alpha = 0.18f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.CloudDone,
                                            contentDescription = null,
                                            tint = crystal.assetGreen,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = br.cloudReadyTitle,
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = prefs.userEmail.ifBlank { br.cloudReadySubtitle },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = crystal.mutedText,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Button(
                                    onClick = onExportJsonClick,
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = Color.White
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                        .testTag("backup_cloud_backup_now_button")
                                ) {
                                    Text(
                                        text = br.cloudBackupNowBtn,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                            }
                        }
                    }

                    if (googleNotice != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = googleNotice,
                            style = MaterialTheme.typography.bodySmall,
                            color = crystal.warningAmber
                        )
                    }
                }
            }

            // 3. বিপদ অঞ্চল (Danger Zone — Pinkish frosted warning container + Red permanent wipe button)
            item {
                val dangerContainerColor = if (crystal.isDark) {
                    Color(0xFF28121B)
                } else {
                    Color(0xFFFFF1F2)
                }
                val dangerBorderColor = if (crystal.isDark) {
                    Color(0xFFFB7185).copy(alpha = 0.48f)
                } else {
                    Color(0xFFFDA4AF)
                }
                val dangerTitleColor = if (crystal.isDark) {
                    Color(0xFFFDA4AF)
                } else {
                    Color(0xFFBE123C)
                }
                val dangerDescColor = if (crystal.isDark) {
                    Color(0xFFF8FAFC).copy(alpha = 0.88f)
                } else {
                    Color(0xFF881337)
                }

                Surface(
                    color = dangerContainerColor,
                    shape = RoundedCornerShape(22.dp),
                    border = BorderStroke(1.2.dp, dangerBorderColor),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("backup_danger_zone_card")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFDC2626).copy(alpha = if (crystal.isDark) 0.28f else 0.14f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Warning,
                                    contentDescription = null,
                                    tint = if (crystal.isDark) Color(0xFFFCA5A5) else Color(0xFFDC2626),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Text(
                                text = br.dangerZoneTitle,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = dangerTitleColor
                            )
                        }

                        Button(
                            onClick = { showDeleteConfirmDialog = true },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFDC2626),
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 50.dp)
                                .testTag("backup_delete_all_data_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DeleteForever,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = br.deleteAllDataBtn,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        Text(
                            text = br.deleteAllDataDesc,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = dangerDescColor
                        )
                    }
                }
            }
        }
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            containerColor = if (crystal.isDark) Color(0xFF161C2B) else MaterialTheme.colorScheme.surfaceContainerHigh,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurface,
            icon = {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = null,
                    tint = Color(0xFFDC2626)
                )
            },
            title = {
                Text(
                    text = br.deleteConfirmTitle,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = br.deleteConfirmMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = crystal.mutedText,
                    modifier = Modifier.testTag("delete_all_confirm_dialog")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDeleteAllFinancialData()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFDC2626),
                        contentColor = Color.White
                    ),
                    modifier = Modifier.testTag("confirm_delete_all_data_button")
                ) {
                    Text(br.deleteConfirmBtn, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }
}

@Composable
private fun BackupActionRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    trailingAction: @Composable () -> Unit
) {
    val crystal = LocalCrystalColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(iconTint.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = crystal.mutedText,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        trailingAction()
    }
}

private fun shareSampleTemplateCsv(context: Context, chooserTitle: String) {
    runCatching {
        val dir = File(context.cacheDir, "templates").apply { mkdirs() }
        val file = File(dir, "amar_hisab_sample_template.csv")
        val content = BackupManager.generateSampleTemplateCsv()
        file.outputStream().use { out ->
            out.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
            out.write(content.toByteArray(Charsets.UTF_8))
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, chooserTitle)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, chooserTitle))
    }
}
