package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.export.MonthlyReportExporter
import com.example.data.export.QrCodeBitmapGenerator
import com.example.data.export.ReportLocalizedStrings
import com.example.data.local.AccountEntity
import com.example.data.local.TransactionEntity
import com.example.data.market.MarketSnapshot
import com.example.data.model.AccountNameResolver
import com.example.data.model.TransactionType
import com.example.data.model.TransferNoteCodec
import com.example.localization.AppLanguage
import com.example.localization.AppStrings
import com.example.localization.BdtFormatter
import com.example.ui.components.CategoryIconBadge
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalCrystalColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionVoucherDetailSheet(
    tx: TransactionEntity,
    accountsById: Map<Long, AccountEntity>,
    language: AppLanguage,
    strings: AppStrings,
    marketSnapshot: MarketSnapshot = MarketSnapshot.defaultReference(),
    onDelete: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val crystal = LocalCrystalColors.current
    val isBn = language.isBangla
    val reportCopy = ReportLocalizedStrings.forLanguage(language)
    val activeTz = java.util.TimeZone.getDefault()
    val dateStr = remember(tx.timestamp, activeTz.id, language) {
        SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.forLanguageTag(language.localeTag)).apply {
            timeZone = activeTz
        }.format(Date(tx.timestamp))
    }

    val isTransfer = tx.type == TransactionType.TRANSFER
    val transferEndpoints = remember(tx, accountsById, language) {
        if (isTransfer) AccountNameResolver.resolveTransferEndpoints(tx, accountsById, language) else null
    }
    val parsedTransfer = remember(tx, accountsById, marketSnapshot) {
        if (isTransfer) TransferNoteCodec.parse(tx, tx.toAccountId?.let { accountsById[it] }, marketSnapshot) else null
    }
    val resolvedWallet = remember(tx, accountsById, language) {
        AccountNameResolver.resolveTransactionWalletDisplay(tx, accountsById, language)
    }
    val cleanNote = remember(tx.note, isTransfer) {
        if (isTransfer) TransferNoteCodec.stripEncodedSegment(tx.note) else tx.note.trim()
    }
    val voucherQrPayload = remember(tx, resolvedWallet, language) {
        MonthlyReportExporter.buildTransactionVoucherQrPayload(
            tx = tx,
            walletLabel = resolvedWallet,
            language = language
        )
    }
    val voucherQrBitmap = remember(voucherQrPayload) {
        QrCodeBitmapGenerator.generateQrBitmap(
            content = voucherQrPayload,
            sizePx = 280
        ).asImageBitmap()
    }

    val headerTitle = when {
        isTransfer && isBn -> "ট্রান্সফার ভাউচার"
        isTransfer -> "${reportCopy.transferShortLabel} • ${reportCopy.brandTitle}"
        isBn -> "লেনদেনের বিবরণ"
        else -> reportCopy.dailyReportTitle
    }

    val primaryAmount = BdtFormatter.formatCurrency(tx.amount, tx.currencyCode, language)
    val signedAmount = when (tx.type) {
        TransactionType.INCOME -> "+$primaryAmount"
        TransactionType.EXPENSE -> "-$primaryAmount"
        TransactionType.TRANSFER -> primaryAmount
    }
    val accentColor = when (tx.type) {
        TransactionType.INCOME -> crystal.assetGreen
        TransactionType.EXPENSE -> crystal.liabilityRed
        TransactionType.TRANSFER -> MaterialTheme.colorScheme.primary
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
                .testTag("activity_voucher_sheet"),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    if (isTransfer) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(accentColor.copy(alpha = if (crystal.isDark) 0.22f else 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.SwapHoriz,
                                contentDescription = headerTitle,
                                tint = accentColor,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    } else {
                        CategoryIconBadge(category = tx.category)
                    }
                    Column {
                        Text(
                            text = headerTitle,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = dateStr,
                            style = MaterialTheme.typography.bodySmall,
                            color = crystal.mutedText
                        )
                    }
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("voucher_close_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = strings.close,
                        tint = crystal.mutedText
                    )
                }
            }

            // Hero Amount Card
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (crystal.isDark) Color(0xFF162134) else Color(0xFFF2F7FE),
                border = BorderStroke(1.dp, accentColor.copy(alpha = if (crystal.isDark) 0.36f else 0.24f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = when {
                                isTransfer && isBn -> "পাঠানো পরিমাণ"
                                isTransfer -> "Transferred Amount"
                                tx.type == TransactionType.INCOME -> strings.incomeLabel
                                else -> strings.expenseLabel
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = crystal.mutedText
                        )
                        Surface(
                            shape = CircleShape,
                            color = accentColor.copy(alpha = if (crystal.isDark) 0.22f else 0.12f)
                        ) {
                            Text(
                                text = AccountNameResolver.currencyShortLabel(tx.currencyCode),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = JetBrainsMonoFamily,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = accentColor,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Text(
                        text = signedAmount,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = accentColor,
                        modifier = Modifier.testTag("voucher_primary_amount")
                    )

                    if (parsedTransfer != null && parsedTransfer.isCrossCurrency) {
                        val approxPrefix = if (parsedTransfer.isRecordedRate) "" else "≈ "
                        val receivedStr = BdtFormatter.formatCurrency(
                            parsedTransfer.receivedAmount,
                            parsedTransfer.destCurrencyCode,
                            language
                        )
                        val srcCurLabel = AccountNameResolver.currencyShortLabel(parsedTransfer.sourceCurrencyCode)
                        val rateStr = BdtFormatter.formatCurrency(
                            parsedTransfer.exchangeRate,
                            parsedTransfer.destCurrencyCode,
                            language
                        )
                        val oneDigit = if (isBn) "১" else "1"
                        HorizontalDivider(thickness = 0.7.dp, color = crystal.glassBorder)
                        Text(
                            text = if (isBn) {
                                "প্রাপ্ত পরিমাণ: $approxPrefix$receivedStr"
                            } else {
                                "Received Amount: $approxPrefix$receivedStr"
                            },
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontFamily = JetBrainsMonoFamily,
                                fontWeight = FontWeight.Bold
                            ),
                            color = crystal.assetGreen,
                            modifier = Modifier.testTag("voucher_received_amount")
                        )
                        Text(
                            text = if (isBn) {
                                "এক্সচেঞ্জ রেট: $oneDigit $srcCurLabel = $rateStr"
                            } else {
                                "Exchange Rate: $oneDigit $srcCurLabel = $rateStr"
                            },
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFamily),
                            color = crystal.mutedText,
                            modifier = Modifier.testTag("voucher_exchange_rate")
                        )
                    }
                }
            }

            // Details Breakdown Card
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = if (crystal.isDark) Color.White.copy(alpha = 0.04f) else Color.White,
                border = BorderStroke(1.dp, crystal.glassBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (isTransfer && transferEndpoints != null) {
                        VoucherDetailRow(
                            label = if (isBn) "প্রেরক ওয়ালেট (From)" else "From Wallet",
                            value = transferEndpoints.first,
                            testTagName = "voucher_from_wallet"
                        )
                        HorizontalDivider(thickness = 0.6.dp, color = crystal.glassBorder)
                        VoucherDetailRow(
                            label = if (isBn) "প্রাপক ওয়ালেট (To)" else "To Wallet",
                            value = transferEndpoints.second,
                            testTagName = "voucher_to_wallet"
                        )
                    } else {
                        VoucherDetailRow(
                            label = if (isBn) "বিবরণ" else "Description",
                            value = tx.title.ifBlank { tx.category.label(language) }
                        )
                        HorizontalDivider(thickness = 0.6.dp, color = crystal.glassBorder)
                        VoucherDetailRow(
                            label = if (isBn) "ওয়ালেট / অ্যাকাউন্ট" else "Wallet / Account",
                            value = resolvedWallet,
                            testTagName = "voucher_wallet_name"
                        )
                        HorizontalDivider(thickness = 0.6.dp, color = crystal.glassBorder)
                        VoucherDetailRow(
                            label = strings.categoryLabel,
                            value = listOf(tx.category.label(language), tx.subcategory)
                                .filter { it.isNotBlank() }
                                .joinToString(" • ")
                        )
                    }

                    if (cleanNote.isNotBlank()) {
                        HorizontalDivider(thickness = 0.6.dp, color = crystal.glassBorder)
                        VoucherDetailRow(
                            label = if (isBn) "নোট" else "Note",
                            value = cleanNote
                        )
                    }
                }
            }

            // Language-Aware Scannable Verification QR Code Card
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = if (crystal.isDark) Color(0xFF152033) else Color.White,
                border = BorderStroke(1.dp, accentColor.copy(alpha = if (crystal.isDark) 0.34f else 0.22f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("voucher_qr_code")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.size(88.dp)
                    ) {
                        Image(
                            bitmap = voucherQrBitmap,
                            contentDescription = reportCopy.voucherQrTitle,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .size(88.dp)
                                .padding(5.dp)
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = reportCopy.voucherQrTitle,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = reportCopy.voucherQrSubtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = crystal.mutedText
                        )
                        Surface(
                            shape = CircleShape,
                            color = accentColor.copy(alpha = if (crystal.isDark) 0.22f else 0.12f)
                        ) {
                            Text(
                                text = "${reportCopy.brandTitle} • ${language.nativeName}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 10.sp
                                ),
                                color = accentColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            if (onDelete != null) {
                OutlinedButton(
                    onClick = {
                        onDelete()
                        onDismiss()
                    },
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, crystal.liabilityRed.copy(alpha = 0.45f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("voucher_delete_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.DeleteOutline,
                        contentDescription = null,
                        tint = crystal.liabilityRed,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = if (isBn) "লেনদেন মুছুন" else "Delete Transaction",
                        color = crystal.liabilityRed,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}

@Composable
private fun VoucherDetailRow(
    label: String,
    value: String,
    testTagName: String? = null
) {
    val crystal = LocalCrystalColors.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = crystal.mutedText
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = if (testTagName != null) Modifier.testTag(testTagName) else Modifier
        )
    }
}
