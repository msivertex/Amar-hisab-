package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.ai.MatchedAccountResult
import com.example.data.local.AccountEntity
import com.example.data.model.AccountType
import com.example.data.model.AppCurrency
import com.example.data.model.TransactionType
import com.example.localization.AppLanguage
import com.example.localization.BdtFormatter
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalCrystalColors
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * Crystal Liquid Confirmation Preview card displayed inside the AI Receipt & Voucher Scanner.
 * Shows Money In (+) vs Money Out (-) toggle, extracted amount & optional fee breakdown,
 * clean flag-free currency badge, auto-matched wallet/card, and live projected balance update.
 */
@Composable
fun ReceiptVoucherPreviewCard(
    transactionType: TransactionType,
    onSelectTransactionType: (TransactionType) -> Unit,
    primaryAmount: Double,
    feeAmount: Double,
    includeFeeInTotal: Boolean,
    effectiveTotalAmount: Double,
    currency: AppCurrency,
    selectedAccount: AccountEntity?,
    matchedAccountResult: MatchedAccountResult?,
    detectedDirectionKeyword: String,
    detectedProviderHint: String,
    referenceId: String,
    isAiDetected: Boolean,
    language: AppLanguage,
    modifier: Modifier = Modifier,
    showTransferOption: Boolean = false
) {
    val crystal = LocalCrystalColors.current
    val isBn = language.isBangla
    val isIncome = transactionType == TransactionType.INCOME
    val isTransfer = transactionType == TransactionType.TRANSFER

    val accentColor = when (transactionType) {
        TransactionType.INCOME -> crystal.assetGreen
        TransactionType.TRANSFER -> MaterialTheme.colorScheme.primary
        TransactionType.EXPENSE -> crystal.liabilityRed
    }
    val accentBg = when (transactionType) {
        TransactionType.INCOME -> crystal.assetGreenBg
        TransactionType.TRANSFER -> MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
        TransactionType.EXPENSE -> crystal.liabilityRedBg
    }
    val signPrefix = when (transactionType) {
        TransactionType.INCOME -> "+"
        TransactionType.TRANSFER -> "↔"
        TransactionType.EXPENSE -> "-"
    }
    val shape = RoundedCornerShape(20.dp)

    val cardBrush = if (crystal.isDark) {
        Brush.linearGradient(
            listOf(
                accentColor.copy(alpha = 0.16f),
                crystal.glassSurfaceElevated
            )
        )
    } else {
        Brush.linearGradient(
            listOf(
                accentBg.copy(alpha = 0.72f),
                crystal.glassSurfaceElevated
            )
        )
    }

    val projectedBalance: Double? = selectedAccount?.let { acc ->
        when {
            acc.type == AccountType.CREDIT_CARD && isIncome ->
                (acc.balance - effectiveTotalAmount).coerceAtLeast(0.0)
            acc.type == AccountType.CREDIT_CARD && !isIncome ->
                acc.balance + effectiveTotalAmount
            isIncome -> acc.balance + effectiveTotalAmount
            else -> acc.balance - effectiveTotalAmount
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(cardBrush)
            .border(1.2.dp, accentColor.copy(alpha = if (crystal.isDark) 0.45f else 0.34f), shape)
            .padding(14.dp)
            .testTag("receipt_confirmation_preview"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header Row: Preview label + AI detected keyword pill + clean flag-free currency badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = if (isAiDetected) Icons.Filled.AutoAwesome else Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = if (isBn) "নিশ্চিতকরণ প্রিভিউ" else "Confirmation Preview",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (detectedDirectionKeyword.isNotBlank()) {
                    Surface(
                        color = accentColor.copy(alpha = if (crystal.isDark) 0.24f else 0.14f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = detectedDirectionKeyword,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = accentColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Clean flag-free currency badge (matches Home balance card KD / BDT style)
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = if (crystal.isDark) 0.65f else 0.85f),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(0.8.dp, crystal.glassBorder)
            ) {
                Text(
                    text = currency.cleanBadgeLabel(),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }

        // Direction Selector Toggle: + Add to Card (Income), - Deduct from Card (Expense), and optional Transfer
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DirectionToggleChip(
                selected = isIncome,
                label = if (isBn) "+ টাকা যোগ (আয়)" else "+ Money In (Add)",
                icon = Icons.Filled.ArrowDownward,
                activeColor = crystal.assetGreen,
                activeBg = crystal.assetGreenBg,
                testTag = "receipt_type_income",
                modifier = Modifier.weight(1f),
                onClick = { onSelectTransactionType(TransactionType.INCOME) }
            )
            DirectionToggleChip(
                selected = transactionType == TransactionType.EXPENSE,
                label = if (isBn) "- টাকা খরচ (ব্যয়)" else "- Money Out (Deduct)",
                icon = Icons.Filled.ArrowUpward,
                activeColor = crystal.liabilityRed,
                activeBg = crystal.liabilityRedBg,
                testTag = "receipt_type_expense",
                modifier = Modifier.weight(1f),
                onClick = { onSelectTransactionType(TransactionType.EXPENSE) }
            )
        }
        if (showTransferOption) {
            DirectionToggleChip(
                selected = isTransfer,
                label = if (isBn) "↔ অ্যাকাউন্ট ট্রান্সফার" else "↔ Account Transfer",
                icon = Icons.Filled.SwapHoriz,
                activeColor = MaterialTheme.colorScheme.primary,
                activeBg = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                testTag = "receipt_type_transfer",
                modifier = Modifier.fillMaxWidth(),
                onClick = { onSelectTransactionType(TransactionType.TRANSFER) }
            )
        }

        // Hero Amount & Fee Summary
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = "$signPrefix ${formatVoucherAmount(effectiveTotalAmount, currency.code, language)}",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontFamily = JetBrainsMonoFamily,
                    fontWeight = FontWeight.ExtraBold
                ),
                color = accentColor,
                modifier = Modifier.testTag("receipt_preview_total_amount")
            )
            if (feeAmount > 0.0) {
                val baseStr = formatVoucherAmount(primaryAmount, currency.code, language)
                val feeStr = formatVoucherAmount(feeAmount, currency.code, language)
                val feeSummary = if (isBn) {
                    if (includeFeeInTotal) {
                        "মূল পরিমাণ: $baseStr  •  ফি/চার্জ: $feeStr (মোটের সাথে যুক্ত)"
                    } else {
                        "মূল পরিমাণ: $baseStr  •  ফি/চার্জ: $feeStr (মোট থেকে বাদ)"
                    }
                } else {
                    if (includeFeeInTotal) {
                        "Amount: $baseStr  •  Fee: $feeStr (included in balance update)"
                    } else {
                        "Amount: $baseStr  •  Fee: $feeStr (excluded from total)"
                    }
                }
                Text(
                    text = feeSummary,
                    style = MaterialTheme.typography.bodySmall,
                    color = crystal.mutedText,
                    modifier = Modifier.testTag("receipt_preview_fee_line")
                )
            }
            if (referenceId.isNotBlank()) {
                Text(
                    text = if (isBn) "রেফারেন্স / TrxID: $referenceId" else "Ref / TrxID: $referenceId",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                    color = crystal.mutedText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        HorizontalDivider(color = crystal.glassBorder.copy(alpha = 0.7f))

        // Auto-Matched Card/Wallet & Live Projected Balance
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("receipt_matched_account_badge"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        (selectedAccount?.let { Color(it.colorArgb) } ?: MaterialTheme.colorScheme.primary)
                            .copy(alpha = if (crystal.isDark) 0.25f else 0.16f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.AccountBalanceWallet,
                    contentDescription = null,
                    tint = selectedAccount?.let { Color(it.colorArgb) } ?: MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                val matchHeadline = when {
                    selectedAccount == null -> if (isBn) {
                        "কোনো কার্ড বা ওয়ালেট নির্বাচিত নেই"
                    } else {
                        "No card or wallet selected"
                    }
                    matchedAccountResult != null &&
                        matchedAccountResult.isAutoMatched &&
                        matchedAccountResult.account?.id == selectedAccount.id -> {
                        matchedAccountResult.reason(language)
                    }
                    else -> {
                        val suffix = selectedAccount.accountNumberLast4
                            .takeIf { it.isNotBlank() }
                            ?.let { " •••• $it" }
                            .orEmpty()
                        "${selectedAccount.name}$suffix"
                    }
                }
                Text(
                    text = matchHeadline,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (selectedAccount != null && projectedBalance != null) {
                    val accCurrency = AppCurrency.fromCode(selectedAccount.currencyCode)
                    val curBal = formatVoucherAmount(selectedAccount.balance, accCurrency.code, language)
                    val newBal = formatVoucherAmount(projectedBalance, accCurrency.code, language)
                    Text(
                        text = if (isBn) {
                            "ব্যালেন্স আপডেট: $curBal → $newBal"
                        } else {
                            "Balance update: $curBal → $newBal"
                        },
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = accentColor,
                        modifier = Modifier.testTag("receipt_projected_balance")
                    )
                } else if (detectedProviderHint.isNotBlank()) {
                    Text(
                        text = if (isBn) {
                            "শনাক্তকৃত মাধ্যম: $detectedProviderHint"
                        } else {
                            "Detected provider: $detectedProviderHint"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = crystal.mutedText
                    )
                }
            }
        }
    }
}

@Composable
private fun DirectionToggleChip(
    selected: Boolean,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    activeColor: Color,
    activeBg: Color,
    testTag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val crystal = LocalCrystalColors.current
    val shape = RoundedCornerShape(14.dp)
    val bgColor = when {
        selected && crystal.isDark -> activeColor.copy(alpha = 0.24f)
        selected -> activeBg
        else -> MaterialTheme.colorScheme.surface.copy(alpha = if (crystal.isDark) 0.55f else 0.72f)
    }
    val borderColor = if (selected) activeColor else crystal.glassBorder
    val contentColor = if (selected) activeColor else crystal.mutedText

    Row(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .heightIn(min = 48.dp)
            .clip(shape)
            .background(bgColor)
            .border(if (selected) 1.4.dp else 0.9.dp, borderColor, shape)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            ),
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Compact privacy & storage hygiene notice informing the user that scanned voucher images
 * are processed temporarily and cleaned up automatically without bloating device storage.
 */
@Composable
fun ReceiptTempStorageNoticeBanner(
    language: AppLanguage,
    modifier: Modifier = Modifier
) {
    val crystal = LocalCrystalColors.current
    val isBn = language == AppLanguage.BN
    Surface(
        color = crystal.glassSurface.copy(alpha = if (crystal.isDark) 0.72f else 0.85f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(0.8.dp, crystal.glassBorder),
        modifier = modifier
            .fillMaxWidth()
            .testTag("receipt_temp_storage_notice")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.CleaningServices,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = if (isBn) {
                    "রসিদ বা স্ক্রিনশট স্থায়ীভাবে ফোনে সেভ হয় না — হিসাব সেভ বা বন্ধ করার সাথে সাথে অস্থায়ী ছবি মুছে যায়।"
                } else {
                    "Scanned images are temporary only — auto-cleared on save or dismiss with zero storage bloat."
                },
                style = MaterialTheme.typography.bodySmall,
                color = crystal.mutedText
            )
        }
    }
}

/**
 * Formats voucher amounts cleanly while preserving decimal fees (e.g. BDT ১৮.৫০ or KWD ৫৮.৫০০).
 */
internal fun formatVoucherAmount(
    amount: Double,
    currencyCode: String,
    language: AppLanguage
): String {
    val currency = AppCurrency.fromCode(currencyCode)
    val absVal = abs(amount)
    val hasDecimals = abs(absVal - absVal.roundToLong()) >= 0.005
    if (currency == AppCurrency.BDT && hasDecimals) {
        val sign = if (amount < 0) "-" else ""
        val formattedUs = String.format(Locale.US, "%.2f", absVal)
        val dot = formattedUs.indexOf('.')
        val whole = formattedUs.substring(0, dot).toLongOrNull() ?: absVal.toLong()
        val frac = formattedUs.substring(dot + 1)
        val wholeFormatted = BdtFormatter.formatAmountOnly(whole.toDouble(), "BDT", language, includeSign = false)
        val fracLocalized = if (language == AppLanguage.BN) BdtFormatter.toBanglaDigits(frac) else frac
        return "$sign${currency.code} $wholeFormatted.$fracLocalized"
    }
    return BdtFormatter.formatCurrency(amount, currencyCode, language)
}
