package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.export.MonthlyReportPayload
import com.example.data.export.QrCodeBitmapGenerator
import com.example.data.export.ReportExportFormat
import com.example.data.export.ReportLocalizedStrings
import com.example.data.export.ReportPeriodType
import com.example.data.model.AccountNameResolver
import com.example.localization.AppLanguage
import com.example.localization.AppStrings
import com.example.localization.BdtFormatter
import com.example.ui.components.CrystalCard
import com.example.ui.theme.HindSiliguriFamily
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalCrystalColors

@Composable
fun InsightsExportHeaderButton(
    label: String,
    isBusy: Boolean,
    onClick: () -> Unit
) {
    val crystal = LocalCrystalColors.current
    val primary = MaterialTheme.colorScheme.primary

    Surface(
        onClick = onClick,
        enabled = !isBusy,
        shape = CircleShape,
        color = primary.copy(alpha = if (crystal.isDark) 0.22f else 0.13f),
        border = BorderStroke(1.dp, primary.copy(alpha = if (crystal.isDark) 0.45f else 0.30f)),
        modifier = Modifier
            .heightIn(min = 48.dp)
            .testTag("insights_header_export_button")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (isBusy) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = primary
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.IosShare,
                    contentDescription = label,
                    tint = primary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = HindSiliguriFamily
                ),
                color = primary,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
fun MonthlyReportExportCard(
    payload: MonthlyReportPayload,
    strings: AppStrings,
    exportingFormat: ReportExportFormat?,
    statusMessage: String?,
    isError: Boolean,
    onQuickExport: (ReportExportFormat) -> Unit,
    onOpenExportSheet: () -> Unit,
    selectedPeriodType: ReportPeriodType = payload.periodType,
    onSelectPeriodType: (ReportPeriodType) -> Unit = {}
) {
    val crystal = LocalCrystalColors.current
    val primary = MaterialTheme.colorScheme.primary
    val exp = strings.reportExport
    val reportCopy = ReportLocalizedStrings.forLanguage(payload.language)
    val pdfAccent = Color(0xFFE11D48)
    val csvAccent = crystal.assetGreen
    val qrAccent = primary
    val isBusy = exportingFormat != null

    CrystalCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("insights_export_card")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(primary.copy(alpha = if (crystal.isDark) 0.22f else 0.13f))
                    .border(
                        width = 1.dp,
                        color = primary.copy(alpha = if (crystal.isDark) 0.42f else 0.26f),
                        shape = RoundedCornerShape(14.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Description,
                    contentDescription = reportCopy.reportTitleFor(selectedPeriodType),
                    tint = primary,
                    modifier = Modifier.size(23.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = reportCopy.reportTitleFor(selectedPeriodType),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = HindSiliguriFamily
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${payload.language.nativeName} • PDF / CSV / QR",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = HindSiliguriFamily
                    ),
                    color = crystal.mutedText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(
                onClick = onOpenExportSheet,
                enabled = !isBusy,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("insights_card_open_sheet_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.QrCode2,
                    contentDescription = reportCopy.qrSectionTitle,
                    tint = primary
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Daily / Weekly / Monthly Period Switcher
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ReportPeriodType.entries.forEach { period ->
                val selected = period == selectedPeriodType
                FilterChip(
                    selected = selected,
                    onClick = { onSelectPeriodType(period) },
                    label = {
                        Text(
                            text = reportCopy.periodTabLabel(period),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                fontFamily = HindSiliguriFamily
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = primary.copy(alpha = if (crystal.isDark) 0.28f else 0.16f),
                        selectedLabelColor = primary
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 40.dp)
                        .testTag("export_period_${period.name}")
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Period & Transaction Count Strip
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (crystal.isDark) Color.White.copy(alpha = 0.05f) else Color(0xFFF3F7FC),
            border = BorderStroke(1.dp, crystal.glassBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${reportCopy.periodLabel}: ${payload.periodLabel}",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = HindSiliguriFamily
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                val txCountFormatted = if (payload.language.isBangla) {
                    BdtFormatter.toBanglaDigits(payload.transactions.size.toString())
                } else {
                    payload.transactions.size.toString()
                }
                Surface(
                    shape = CircleShape,
                    color = primary.copy(alpha = if (crystal.isDark) 0.20f else 0.11f)
                ) {
                    Text(
                        text = "$txCountFormatted ${reportCopy.txCountSuffix}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = HindSiliguriFamily,
                            fontSize = 11.sp
                        ),
                        color = primary,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 1-Tap PDF, CSV & QR Receipt Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { onQuickExport(ReportExportFormat.PDF) },
                enabled = !isBusy,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = pdfAccent,
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 12.dp),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .testTag("insights_quick_export_pdf")
            ) {
                if (exportingFormat == ReportExportFormat.PDF) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.PictureAsPdf,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = exp.quickPdfBtn,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = HindSiliguriFamily
                    ),
                    maxLines = 1,
                    softWrap = false
                )
            }

            OutlinedButton(
                onClick = { onQuickExport(ReportExportFormat.CSV) },
                enabled = !isBusy,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.2.dp, csvAccent.copy(alpha = 0.65f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = csvAccent),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 12.dp),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .testTag("insights_quick_export_csv")
            ) {
                if (exportingFormat == ReportExportFormat.CSV) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = csvAccent
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.TableChart,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = exp.quickCsvBtn,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = HindSiliguriFamily
                    ),
                    maxLines = 1,
                    softWrap = false
                )
            }

            OutlinedButton(
                onClick = onOpenExportSheet,
                enabled = !isBusy,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.2.dp, qrAccent.copy(alpha = 0.65f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = qrAccent),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 12.dp),
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("insights_quick_export_qr")
            ) {
                if (exportingFormat == ReportExportFormat.QR_PNG) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = qrAccent
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.QrCode2,
                        contentDescription = reportCopy.qrSectionTitle,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "QR",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = JetBrainsMonoFamily
                    ),
                    maxLines = 1,
                    softWrap = false
                )
            }
        }

        if (!statusMessage.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(10.dp))
            val bannerBg = if (isError) crystal.warningAmberBg else crystal.assetGreen.copy(alpha = if (crystal.isDark) 0.16f else 0.10f)
            val bannerBorder = if (isError) crystal.warningAmber.copy(alpha = 0.4f) else crystal.assetGreen.copy(alpha = 0.35f)
            val bannerText = if (isError) crystal.warningAmber else crystal.assetGreen

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = bannerBg,
                border = BorderStroke(1.dp, bannerBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("insights_export_status_banner")
            ) {
                Text(
                    text = statusMessage,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = HindSiliguriFamily,
                        fontWeight = FontWeight.Medium
                    ),
                    color = bannerText,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthlyReportExportSheet(
    payload: MonthlyReportPayload,
    strings: AppStrings,
    exportingFormat: ReportExportFormat?,
    onExport: (ReportExportFormat) -> Unit,
    onDismiss: () -> Unit,
    selectedPeriodType: ReportPeriodType = payload.periodType,
    onSelectPeriodType: (ReportPeriodType) -> Unit = {}
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val crystal = LocalCrystalColors.current
    val primary = MaterialTheme.colorScheme.primary
    val exp = strings.reportExport
    val lang = payload.language
    val reportCopy = ReportLocalizedStrings.forLanguage(lang)
    val isBusy = exportingFormat != null

    val qrBitmap = remember(payload.qrPayload) {
        QrCodeBitmapGenerator.generateQrBitmap(
            content = payload.qrPayload,
            sizePx = 320
        ).asImageBitmap()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = if (crystal.isDark) Color(0xFF101726) else Color(0xFFFBFDFF),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 6.dp)
                .testTag("insights_export_sheet"),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Sheet Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = reportCopy.reportTitleFor(selectedPeriodType),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = HindSiliguriFamily
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${payload.language.nativeName} • ${payload.periodLabel}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = HindSiliguriFamily
                        ),
                        color = crystal.mutedText
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("export_sheet_close_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = strings.close,
                        tint = crystal.mutedText
                    )
                }
            }

            // Daily / Weekly / Monthly Period Switcher inside Sheet
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ReportPeriodType.entries.forEach { period ->
                    val selected = period == selectedPeriodType
                    FilterChip(
                        selected = selected,
                        onClick = { onSelectPeriodType(period) },
                        label = {
                            Text(
                                text = reportCopy.periodTabLabel(period),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    fontFamily = HindSiliguriFamily
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = primary.copy(alpha = if (crystal.isDark) 0.28f else 0.16f),
                            selectedLabelColor = primary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 42.dp)
                            .testTag("export_sheet_period_${period.name}")
                    )
                }
            }

            // Period Financial Preview Card
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = if (crystal.isDark) Color(0xFF172235) else Color(0xFFF1F6FD),
                border = BorderStroke(1.dp, crystal.glassBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("export_sheet_preview_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = payload.periodLabel,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = HindSiliguriFamily
                            ),
                            color = primary
                        )
                        val countStr = if (lang.isBangla) {
                            BdtFormatter.toBanglaDigits(payload.transactions.size.toString())
                        } else {
                            payload.transactions.size.toString()
                        }
                        Text(
                            text = "$countStr ${reportCopy.txCountSuffix}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = HindSiliguriFamily,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = crystal.mutedText
                        )
                    }

                    HorizontalDivider(thickness = 0.7.dp, color = crystal.glassBorder)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SheetMetricColumn(
                            label = reportCopy.totalIncomeLabel,
                            value = BdtFormatter.formatCurrency(payload.totalIncome, payload.mainCurrencyCode, lang),
                            valueColor = crystal.assetGreen,
                            modifier = Modifier.weight(1f)
                        )
                        SheetMetricColumn(
                            label = reportCopy.totalExpenseLabel,
                            value = BdtFormatter.formatCurrency(payload.totalExpense, payload.mainCurrencyCode, lang),
                            valueColor = crystal.liabilityRed,
                            modifier = Modifier.weight(1f)
                        )
                        SheetMetricColumn(
                            label = reportCopy.netFlowLabel,
                            value = BdtFormatter.formatCurrency(payload.netFlow, payload.mainCurrencyCode, lang, includeSign = true),
                            valueColor = if (payload.netFlow >= 0) crystal.assetGreen else crystal.liabilityRed,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    val showPerCur = payload.perCurrencyTotals.size > 1 ||
                        payload.perCurrencyTotals.any { !it.currencyCode.equals(payload.mainCurrencyCode, ignoreCase = true) }
                    if (showPerCur && payload.perCurrencyTotals.isNotEmpty()) {
                        HorizontalDivider(thickness = 0.6.dp, color = crystal.glassBorder)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            payload.perCurrencyTotals.forEach { cur ->
                                val curBadge = AccountNameResolver.currencyShortLabel(cur.currencyCode)
                                val incStr = BdtFormatter.formatCurrency(cur.income, cur.currencyCode, lang)
                                val expStr = BdtFormatter.formatCurrency(cur.expense, cur.currencyCode, lang)
                                val trfStr = BdtFormatter.formatCurrency(cur.transferOut, cur.currencyCode, lang)
                                val lineText = "$curBadge • ${reportCopy.totalIncomeLabel}: $incStr | ${reportCopy.totalExpenseLabel}: $expStr | ${reportCopy.transferShortLabel}: $trfStr"
                                Text(
                                    text = lineText,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = JetBrainsMonoFamily
                                    ),
                                    color = crystal.mutedText,
                                    maxLines = 2
                                )
                            }
                        }
                    }
                }
            }

            // Live Scannable Verification QR Code Preview Card
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = if (crystal.isDark) Color(0xFF152033) else Color.White,
                border = BorderStroke(1.dp, primary.copy(alpha = if (crystal.isDark) 0.36f else 0.24f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("export_receipt_qr_preview")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.size(96.dp)
                    ) {
                        Image(
                            bitmap = qrBitmap,
                            contentDescription = reportCopy.qrSectionTitle,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .size(96.dp)
                                .padding(6.dp)
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = reportCopy.qrSectionTitle,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = HindSiliguriFamily
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = reportCopy.qrScanCaption,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = HindSiliguriFamily
                            ),
                            color = crystal.mutedText,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = primary.copy(alpha = if (crystal.isDark) 0.24f else 0.12f)
                            ) {
                                Text(
                                    text = "${reportCopy.qrVerifyCodeLabel}: ${payload.verificationId}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = JetBrainsMonoFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    ),
                                    color = primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            Surface(
                                shape = CircleShape,
                                color = crystal.assetGreen.copy(alpha = if (crystal.isDark) 0.22f else 0.12f)
                            ) {
                                Text(
                                    text = lang.nativeName,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = HindSiliguriFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 10.sp
                                    ),
                                    color = crystal.assetGreen,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }
            }

            // PDF Option Tile (with embedded QR code)
            ExportFormatOptionTile(
                title = "${reportCopy.reportTitleFor(selectedPeriodType)} (PDF + QR)",
                subtitle = if (exportingFormat == ReportExportFormat.PDF) exp.exportingPdfLabel else exp.pdfOptionSubtitle,
                badgeText = "PDF",
                icon = Icons.Filled.PictureAsPdf,
                accentColor = Color(0xFFE11D48),
                isLoading = exportingFormat == ReportExportFormat.PDF,
                enabled = !isBusy,
                testTagName = "export_pdf_option",
                onClick = { onExport(ReportExportFormat.PDF) }
            )

            // Scannable Verification QR Receipt PNG Option Tile
            ExportFormatOptionTile(
                title = reportCopy.qrShareOptionTitle,
                subtitle = reportCopy.qrShareOptionSubtitle,
                badgeText = "QR PNG",
                icon = Icons.Filled.QrCode2,
                accentColor = primary,
                isLoading = exportingFormat == ReportExportFormat.QR_PNG,
                enabled = !isBusy,
                testTagName = "export_qr_option",
                onClick = { onExport(ReportExportFormat.QR_PNG) }
            )

            // CSV Option Tile
            ExportFormatOptionTile(
                title = exp.csvOptionTitle,
                subtitle = if (exportingFormat == ReportExportFormat.CSV) exp.exportingCsvLabel else exp.csvOptionSubtitle,
                badgeText = "CSV",
                icon = Icons.Filled.TableChart,
                accentColor = crystal.assetGreen,
                isLoading = exportingFormat == ReportExportFormat.CSV,
                enabled = !isBusy,
                testTagName = "export_csv_option",
                onClick = { onExport(ReportExportFormat.CSV) }
            )

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
private fun SheetMetricColumn(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    val crystal = LocalCrystalColors.current
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = HindSiliguriFamily),
            color = crystal.mutedText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge.copy(
                fontFamily = JetBrainsMonoFamily,
                fontWeight = FontWeight.Bold
            ),
            color = valueColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ExportFormatOptionTile(
    title: String,
    subtitle: String,
    badgeText: String,
    icon: ImageVector,
    accentColor: Color,
    isLoading: Boolean,
    enabled: Boolean,
    testTagName: String,
    onClick: () -> Unit
) {
    val crystal = LocalCrystalColors.current

    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(18.dp),
        color = if (crystal.isDark) Color.White.copy(alpha = 0.05f) else Color.White,
        border = BorderStroke(1.dp, accentColor.copy(alpha = if (crystal.isDark) 0.42f else 0.28f)),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .testTag(testTagName)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(accentColor.copy(alpha = if (crystal.isDark) 0.22f else 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.2.dp,
                        color = accentColor
                    )
                } else {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = accentColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = HindSiliguriFamily
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Surface(
                        shape = CircleShape,
                        color = accentColor.copy(alpha = if (crystal.isDark) 0.24f else 0.14f)
                    ) {
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = JetBrainsMonoFamily,
                                fontSize = 10.sp
                            ),
                            color = accentColor,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = HindSiliguriFamily
                    ),
                    color = crystal.mutedText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(
                imageVector = Icons.Filled.IosShare,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
