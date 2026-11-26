package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.ai.ReceiptVoucherMatcher
import com.example.data.local.AccountEntity
import com.example.data.local.LoanEntity
import com.example.data.market.MarketSnapshot
import com.example.data.model.AppCurrency
import com.example.data.model.LoanType
import com.example.localization.AppLanguage
import com.example.localization.AppStrings
import com.example.localization.BdtFormatter
import com.example.ui.components.AppCurrencyPickerSheet
import com.example.ui.components.CrystalCard
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalCrystalColors
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

data class LoanCurrencySummary(
    val currency: AppCurrency,
    val totalPayableRemaining: Double,
    val totalReceivableRemaining: Double,
    val openPayableCount: Int,
    val openReceivableCount: Int
) {
    val hasAnyActivity: Boolean
        get() = totalPayableRemaining > 0.001 || totalReceivableRemaining > 0.001
}

/**
 * Computes strictly per-currency summaries for Dues / Loans (দেনা-পাওনা).
 * Never merges distinct currencies into a single misleading figure.
 */
object LoanCurrencyAggregator {

    fun defaultLoanCurrencyCode(
        mainCurrencyCode: String,
        secondaryCurrencyCode: String
    ): String {
        val secondary = AppCurrency.fromCodeOrNull(secondaryCurrencyCode)
        return secondary?.code ?: AppCurrency.fromCode(mainCurrencyCode).code
    }

    fun computeSummaries(
        loans: List<LoanEntity>,
        mainCurrencyCode: String,
        secondaryCurrencyCode: String
    ): List<LoanCurrencySummary> {
        val mainCur = AppCurrency.fromCode(mainCurrencyCode)
        val secondaryCur = AppCurrency.fromCodeOrNull(secondaryCurrencyCode)
            ?.takeIf { it != mainCur }

        val grouped = loans.groupBy { AppCurrency.fromCode(it.currencyCode) }
        val orderedCurrencies = linkedSetOf<AppCurrency>().apply {
            if (secondaryCur != null) add(secondaryCur)
            add(mainCur)
            grouped.keys.sortedBy { it.code }.forEach { add(it) }
        }

        val summaries = orderedCurrencies.map { cur ->
            val entries = grouped[cur].orEmpty()
            val openPayables = entries.filter { it.loanType == LoanType.I_OWE && !it.isSettled }
            val openReceivables = entries.filter { it.loanType == LoanType.OWED_TO_ME && !it.isSettled }
            LoanCurrencySummary(
                currency = cur,
                totalPayableRemaining = openPayables.sumOf { it.remainingAmount },
                totalReceivableRemaining = openReceivables.sumOf { it.remainingAmount },
                openPayableCount = openPayables.size,
                openReceivableCount = openReceivables.size
            )
        }

        // Keep zero-state clean: when there are loans, prioritize currencies that actually have loans
        // alongside active user currencies; when empty, display user's active currency(ies) with 0.
        return if (loans.isEmpty()) {
            summaries.filter { it.currency == mainCur || it.currency == secondaryCur }
        } else {
            val activeWithLoans = summaries.filter { curSummary ->
                grouped.containsKey(curSummary.currency)
            }
            activeWithLoans.ifEmpty { summaries.take(1) }
        }
    }
}

/**
 * Crystal Liquid Per-Currency Summary Card for দেনা-পাওনা (Payables & Receivables).
 * Displays clean separate rows per currency (e.g., KD and BDT) without mixing amounts.
 */
@Composable
fun LoansMultiCurrencySummaryCard(
    summaries: List<LoanCurrencySummary>,
    hasAnyLoans: Boolean,
    language: AppLanguage,
    strings: AppStrings,
    modifier: Modifier = Modifier
) {
    val crystal = LocalCrystalColors.current
    val isBn = language == AppLanguage.BN
    val shape = RoundedCornerShape(22.dp)
    val cardBrush = if (crystal.isDark) {
        Brush.linearGradient(
            listOf(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                crystal.glassSurfaceElevated
            )
        )
    } else {
        Brush.linearGradient(
            listOf(
                crystal.glassSurfaceElevated,
                MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)
            )
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(cardBrush)
            .border(1.1.dp, crystal.glassBorder, shape)
            .padding(16.dp)
            .testTag("loans_currency_summary_card"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isBn) "মুদ্রাভিত্তিক দেনা-পাওনা সারসংক্ষেপ" else "Per-Currency Dues Summary",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (isBn) {
                        "প্রতিটি মুদ্রার হিসাব আলাদাভাবে দেখানো হয়েছে"
                    } else {
                        "Separate totals per currency without mixing balances"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = crystal.mutedText
                )
            }
        }

        summaries.forEachIndexed { idx, summary ->
            val cur = summary.currency
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("loans_summary_currency_${cur.code}"),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = if (crystal.isDark) 0.22f else 0.12f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.34f))
                    ) {
                        Text(
                            text = cur.cleanBadgeLabel(),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontFamily = JetBrainsMonoFamily,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                        )
                    }
                    Text(
                        text = cur.currencyName(language),
                        style = MaterialTheme.typography.labelMedium,
                        color = crystal.mutedText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Payable (দেনা / I Owe)
                    Surface(
                        color = if (crystal.isDark) {
                            crystal.liabilityRed.copy(alpha = 0.14f)
                        } else {
                            crystal.liabilityRedBg.copy(alpha = 0.78f)
                        },
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(0.8.dp, crystal.liabilityRed.copy(alpha = 0.28f)),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("loans_summary_payable_${cur.code}")
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = strings.moneyIOwe,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = crystal.mutedText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = formatLoanCurrencyAmount(summary.totalPayableRemaining, cur.code, language),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = JetBrainsMonoFamily,
                                    fontWeight = FontWeight.ExtraBold
                                ),
                                color = crystal.liabilityRed,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Receivable (পাওনা / Owed to Me)
                    Surface(
                        color = if (crystal.isDark) {
                            crystal.assetGreen.copy(alpha = 0.14f)
                        } else {
                            crystal.assetGreenBg.copy(alpha = 0.78f)
                        },
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(0.8.dp, crystal.assetGreen.copy(alpha = 0.28f)),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("loans_summary_receivable_${cur.code}")
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = strings.moneyOwedToMe,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = crystal.mutedText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = formatLoanCurrencyAmount(summary.totalReceivableRemaining, cur.code, language),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = JetBrainsMonoFamily,
                                    fontWeight = FontWeight.ExtraBold
                                ),
                                color = crystal.assetGreen,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
            if (idx < summaries.lastIndex) {
                HorizontalDivider(color = crystal.glassBorder.copy(alpha = 0.65f))
            }
        }

        if (!hasAnyLoans) {
            Text(
                text = if (isBn) {
                    "এখনো কোনো দেনা বা পাওনা যোগ করা হয়নি। উপরে 'নতুন এন্ট্রি' ট্যাপ করে BDT, KD, SAR বা অন্য যেকোনো মুদ্রায় হিসাব শুরু করুন।"
                } else {
                    "No dues or loans recorded yet. Tap Add Entry above to track payables or receivables in KD, SAR, BDT, or any currency."
                },
                style = MaterialTheme.typography.bodySmall,
                color = crystal.mutedText,
                modifier = Modifier.testTag("loans_empty_zero_state_notice")
            )
        }
    }
}

/**
 * Individual Crystal Liquid card for a single Due/Loan displayed in its own currency,
 * with Edit, Delete, and Partial/Full Settlement actions.
 */
@Composable
fun LoanCurrencyEntryCard(
    loan: LoanEntity,
    language: AppLanguage,
    strings: AppStrings,
    dateFormatter: SimpleDateFormat,
    onEdit: () -> Unit,
    onSettle: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val crystal = LocalCrystalColors.current
    val isBn = language == AppLanguage.BN
    val loanCurrency = AppCurrency.fromCode(loan.currencyCode)
    val ratio = if (loan.totalAmount > 0.0) {
        (loan.paidAmount / loan.totalAmount).toFloat().coerceIn(0f, 1f)
    } else 0f
    val accent = if (loan.loanType == LoanType.I_OWE) crystal.liabilityRed else crystal.assetGreen

    CrystalCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("loan_card_${loan.id}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(accent.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (loan.isSettled) Icons.Filled.CheckCircle else Icons.Filled.Handshake,
                        contentDescription = null,
                        tint = accent
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = loan.personName,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Surface(
                            color = accent.copy(alpha = if (crystal.isDark) 0.22f else 0.12f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = loanCurrency.cleanBadgeLabel(),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = JetBrainsMonoFamily,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = accent,
                                modifier = Modifier
                                    .padding(horizontal = 7.dp, vertical = 2.dp)
                                    .testTag("loan_card_currency_badge_${loan.id}")
                            )
                        }
                    }

                    Text(
                        text = "${strings.remainingDueLabel}: ${
                            formatLoanCurrencyAmount(loan.remainingAmount, loanCurrency.code, language)
                        } / ${
                            formatLoanCurrencyAmount(loan.totalAmount, loanCurrency.code, language)
                        }",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = crystal.mutedText,
                        modifier = Modifier.testTag("loan_card_amount_${loan.id}")
                    )

                    if (loan.paidAmount > 0.001 && !loan.isSettled) {
                        Text(
                            text = if (isBn) {
                                "পরিশোধিত: ${formatLoanCurrencyAmount(loan.paidAmount, loanCurrency.code, language)}"
                            } else {
                                "Paid so far: ${formatLoanCurrencyAmount(loan.paidAmount, loanCurrency.code, language)}"
                            },
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                            color = crystal.assetGreen
                        )
                    }

                    if (loan.note.isNotBlank()) {
                        Text(
                            text = loan.note,
                            style = MaterialTheme.typography.bodySmall,
                            color = crystal.mutedText,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (loan.dueDateMillis != null) {
                        Text(
                            text = "${strings.dueDateLabel}: ${dateFormatter.format(Date(loan.dueDateMillis))}",
                            style = MaterialTheme.typography.labelSmall,
                            color = accent
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("loan_edit_button_${loan.id}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = if (isBn) "সম্পাদনা করুন" else "Edit entry",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("loan_delete_button_${loan.id}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.DeleteOutline,
                        contentDescription = strings.deleteAccount,
                        tint = crystal.mutedText
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.16f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(ratio)
                    .height(8.dp)
                    .clip(CircleShape)
                    .background(accent)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))
        if (loan.isSettled) {
            Surface(
                color = crystal.assetGreenBg,
                shape = CircleShape,
                modifier = Modifier.testTag("loan_settled_badge_${loan.id}")
            ) {
                Text(
                    text = strings.settledBadge,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = crystal.assetGreen,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        } else {
            FilledTonalButton(
                onClick = onSettle,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("loan_settle_button_${loan.id}")
            ) {
                Text(strings.settleLoan)
            }
        }
    }
}

/**
 * Add or Edit a Multi-Currency Due/Loan entry (দেনা / পাওনা).
 * Defaults smartly to Current/Second Currency (if configured) or Main Currency,
 * with quick currency chips and full AppCurrencyPickerSheet access.
 */
@Composable
fun LoanEditorDialog(
    existingLoan: LoanEntity?,
    initialType: LoanType,
    mainCurrencyCode: String,
    secondaryCurrencyCode: String,
    marketSnapshot: MarketSnapshot,
    language: AppLanguage,
    strings: AppStrings,
    dateFormatter: SimpleDateFormat,
    onSaveNew: (person: String, type: LoanType, amount: Double, note: String, dueDate: Long?, currencyCode: String) -> Unit,
    onSaveEdit: (LoanEntity) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val crystal = LocalCrystalColors.current
    val isBn = language == AppLanguage.BN
    val isEditing = existingLoan != null

    val mainCur = AppCurrency.fromCode(mainCurrencyCode)
    val secondaryCur = AppCurrency.fromCodeOrNull(secondaryCurrencyCode)?.takeIf { it != mainCur }
    val smartDefaultCode = remember(existingLoan, mainCurrencyCode, secondaryCurrencyCode) {
        existingLoan?.currencyCode ?: LoanCurrencyAggregator.defaultLoanCurrencyCode(
            mainCurrencyCode = mainCurrencyCode,
            secondaryCurrencyCode = secondaryCurrencyCode
        )
    }

    var person by remember(existingLoan) { mutableStateOf(existingLoan?.personName ?: "") }
    var amountText by remember(existingLoan) {
        mutableStateOf(existingLoan?.let { formatEditableLoanDecimal(it.totalAmount) } ?: "")
    }
    var note by remember(existingLoan) { mutableStateOf(existingLoan?.note ?: "") }
    var type by remember(existingLoan, initialType) { mutableStateOf(existingLoan?.loanType ?: initialType) }
    var dueMillis by remember(existingLoan) { mutableStateOf(existingLoan?.dueDateMillis) }
    var selectedCurrency by remember(existingLoan, smartDefaultCode) {
        mutableStateOf(AppCurrency.fromCode(smartDefaultCode))
    }
    var showCurrencyPicker by remember { mutableStateOf(false) }

    val quickCurrencies = remember(mainCur, secondaryCur, selectedCurrency) {
        linkedSetOf<AppCurrency>().apply {
            if (secondaryCur != null) add(secondaryCur)
            add(mainCur)
            add(selectedCurrency)
        }.toList()
    }

    if (showCurrencyPicker) {
        AppCurrencyPickerSheet(
            title = if (isBn) "দেনা-পাওনার মুদ্রা বাছাই করুন" else "Select Due / Loan Currency",
            subtitle = if (isBn) {
                "KWD, SAR, AED, BDT বা অন্য যেকোনো মুদ্রা নির্বাচন করুন"
            } else {
                "Pick KWD, SAR, AED, BDT, or any supported currency"
            },
            selectedCurrency = selectedCurrency,
            language = language,
            referenceCurrency = mainCur,
            marketSnapshot = marketSnapshot,
            allowClear = false,
            onSelect = { picked ->
                selectedCurrency = picked
                showCurrencyPicker = false
            },
            onDismiss = { showCurrencyPicker = false }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.testTag("loan_editor_dialog"),
        title = {
            Text(
                text = if (isEditing) {
                    if (isBn) "দেনা-পাওনা সম্পাদনা করুন" else "Edit Due / Loan"
                } else {
                    strings.addLoan
                }
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Type toggle: Payable vs Receivable
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = type == LoanType.I_OWE,
                        onClick = { type = LoanType.I_OWE },
                        label = { Text(strings.moneyIOwe) },
                        modifier = Modifier.testTag("loan_type_i_owe")
                    )
                    FilterChip(
                        selected = type == LoanType.OWED_TO_ME,
                        onClick = { type = LoanType.OWED_TO_ME },
                        label = { Text(strings.moneyOwedToMe) },
                        modifier = Modifier.testTag("loan_type_owed_to_me")
                    )
                }

                // Multi-Currency Selector Row (Quick Active Currencies + Full Picker Sheet)
                Text(
                    text = if (isBn) "মুদ্রা নির্বাচন করুন" else "Currency",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    quickCurrencies.forEach { cur ->
                        FilterChip(
                            selected = selectedCurrency == cur,
                            onClick = { selectedCurrency = cur },
                            label = {
                                Text(
                                    text = cur.cleanBadgeLabel(),
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontFamily = JetBrainsMonoFamily,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            },
                            modifier = Modifier.testTag("loan_currency_chip_${cur.code}")
                        )
                    }
                    OutlinedButton(
                        onClick = { showCurrencyPicker = true },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .heightIn(min = 40.dp)
                            .testTag("loan_currency_picker_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CurrencyExchange,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = if (isBn) "সব মুদ্রা" else "All Currencies",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }

                OutlinedTextField(
                    value = person,
                    onValueChange = { person = it },
                    label = { Text(strings.personNameLabel) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("loan_person_input")
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = sanitizeLoanDecimal(it) },
                    label = { Text("${strings.loanAmountLabel} (${selectedCurrency.cleanBadgeLabel()})") },
                    prefix = {
                        Text(
                            text = "${selectedCurrency.code} ",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontFamily = JetBrainsMonoFamily,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("loan_amount_input")
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(strings.noteOptionalLabel) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("loan_note_input")
                )

                OutlinedButton(
                    onClick = {
                        val cal = Calendar.getInstance().apply {
                            dueMillis?.let { timeInMillis = it }
                        }
                        DatePickerDialog(
                            context,
                            { _, y, m, d ->
                                cal.set(y, m, d)
                                dueMillis = cal.timeInMillis
                            },
                            cal.get(Calendar.YEAR),
                            cal.get(Calendar.MONTH),
                            cal.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                ) {
                    Icon(Icons.Filled.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        dueMillis?.let { "${strings.dueDateLabel}: ${dateFormatter.format(Date(it))}" }
                            ?: "${strings.dueDateLabel} (${strings.pickDateLabel})"
                    )
                }

                if (isEditing && existingLoan != null && existingLoan.paidAmount > 0.001) {
                    Text(
                        text = if (isBn) {
                            "ইতিমধ্যে পরিশোধিত: ${
                                formatLoanCurrencyAmount(existingLoan.paidAmount, selectedCurrency.code, language)
                            }"
                        } else {
                            "Already settled: ${
                                formatLoanCurrencyAmount(existingLoan.paidAmount, selectedCurrency.code, language)
                            }"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = crystal.mutedText
                    )
                }
            }
        },
        confirmButton = {
            val amt = amountText.toDoubleOrNull() ?: 0.0
            Button(
                onClick = {
                    if (person.isNotBlank() && amt > 0.0) {
                        if (existingLoan != null) {
                            onSaveEdit(
                                existingLoan.copy(
                                    personName = person.trim(),
                                    loanType = type,
                                    totalAmount = amt,
                                    note = note.trim(),
                                    dueDateMillis = dueMillis,
                                    currencyCode = selectedCurrency.code
                                )
                            )
                        } else {
                            onSaveNew(person.trim(), type, amt, note.trim(), dueMillis, selectedCurrency.code)
                        }
                    }
                },
                enabled = person.isNotBlank() && amt > 0.0,
                modifier = Modifier.testTag("loan_save_button")
            ) {
                Text(strings.saveChanges)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
        }
    )
}

/**
 * Currency-aware Partial or Full Settlement Dialog for a Due/Loan.
 * Preserves decimals (e.g., KD 12.50), offers quick partial/full chips, prioritizes same-currency wallets,
 * and allows settling without modifying a wallet when desired.
 */
@Composable
fun LoanSettlementDialog(
    loan: LoanEntity,
    accounts: List<AccountEntity>,
    marketSnapshot: MarketSnapshot,
    language: AppLanguage,
    strings: AppStrings,
    onConfirmSettle: (loan: LoanEntity, amount: Double, walletId: Long?) -> Unit,
    onDismiss: () -> Unit
) {
    val crystal = LocalCrystalColors.current
    val isBn = language == AppLanguage.BN
    val loanCurrency = AppCurrency.fromCode(loan.currencyCode)

    var settleText by remember(loan) {
        mutableStateOf(formatEditableLoanDecimal(loan.remainingAmount))
    }

    // Prefer a wallet matching the loan's exact currency; otherwise allow settling without a wallet or picking any wallet
    val matchingWallet = remember(loan, accounts) {
        accounts.firstOrNull { it.currencyCode.equals(loan.currencyCode, ignoreCase = true) }
    }
    var chosenWalletId by remember(loan, accounts) {
        mutableStateOf<Long?>(matchingWallet?.id ?: accounts.firstOrNull()?.id)
    }

    val chosenWallet = accounts.firstOrNull { it.id == chosenWalletId }
    val amountVal = settleText.toDoubleOrNull() ?: 0.0
    val effectiveAmount = amountVal.coerceAtMost(loan.remainingAmount)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.testTag("loan_settle_dialog"),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "${strings.settleLoan}: ${loan.personName}",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = if (crystal.isDark) 0.22f else 0.12f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = loanCurrency.cleanBadgeLabel(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "${strings.remainingDueLabel}: ${
                        formatLoanCurrencyAmount(loan.remainingAmount, loanCurrency.code, language)
                    }",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = crystal.mutedText
                )

                OutlinedTextField(
                    value = settleText,
                    onValueChange = { settleText = sanitizeLoanDecimal(it) },
                    label = { Text("${strings.settlementAmountLabel} (${loanCurrency.cleanBadgeLabel()})") },
                    prefix = {
                        Text(
                            text = "${loanCurrency.code} ",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontFamily = JetBrainsMonoFamily,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("loan_settle_amount_input")
                )

                // Quick partial / full settlement shortcuts
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = false,
                        onClick = { settleText = formatEditableLoanDecimal(loan.remainingAmount * 0.25) },
                        label = { Text(if (isBn) "২৫%" else "25%") },
                        modifier = Modifier.testTag("loan_settle_25_chip")
                    )
                    FilterChip(
                        selected = false,
                        onClick = { settleText = formatEditableLoanDecimal(loan.remainingAmount * 0.5) },
                        label = { Text(if (isBn) "৫০% (আংশিক)" else "50% (Partial)") },
                        modifier = Modifier.testTag("loan_settle_50_chip")
                    )
                    FilterChip(
                        selected = abs(amountVal - loan.remainingAmount) < 0.005,
                        onClick = { settleText = formatEditableLoanDecimal(loan.remainingAmount) },
                        label = { Text(if (isBn) "১০০% (সম্পূর্ণ)" else "100% (Full)") },
                        modifier = Modifier.testTag("loan_settle_full_chip")
                    )
                }

                Text(strings.selectWalletLabel, style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = chosenWalletId == null,
                        onClick = { chosenWalletId = null },
                        label = {
                            Text(if (isBn) "ওয়ালেট ছাড়া" else "No Wallet")
                        },
                        modifier = Modifier.testTag("loan_settle_no_wallet_chip")
                    )
                    // Show matching-currency wallets first
                    val sortedWallets = accounts.sortedByDescending {
                        it.currencyCode.equals(loan.currencyCode, ignoreCase = true)
                    }
                    sortedWallets.forEach { acc ->
                        val accCur = AppCurrency.fromCode(acc.currencyCode)
                        FilterChip(
                            selected = chosenWalletId == acc.id,
                            onClick = { chosenWalletId = acc.id },
                            label = { Text("${acc.name} • ${accCur.cleanBadgeLabel()}") },
                            modifier = Modifier.testTag("loan_settle_wallet_${acc.id}")
                        )
                    }
                }

                val walletImpactAmount = if (chosenWallet != null && effectiveAmount > 0.0) {
                    if (chosenWallet.currencyCode.equals(loan.currencyCode, ignoreCase = true)) {
                        effectiveAmount
                    } else {
                        marketSnapshot.convertCurrencyCode(
                            amount = effectiveAmount,
                            fromCode = loan.currencyCode,
                            toCode = chosenWallet.currencyCode
                        )
                    }
                } else {
                    0.0
                }
                val isInsufficientLoanWallet = loan.loanType == LoanType.I_OWE &&
                    chosenWallet != null &&
                    com.example.data.model.AccountBalanceGuard.isInsufficientForDeduction(chosenWallet, walletImpactAmount)

                if (chosenWallet != null && !chosenWallet.currencyCode.equals(loan.currencyCode, ignoreCase = true) && effectiveAmount > 0.0) {
                    val targetFormatted = formatLoanCurrencyAmount(walletImpactAmount, chosenWallet.currencyCode, language)
                    Surface(
                        color = crystal.warningAmberBg,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (isBn) {
                                "ভিন্ন মুদ্রার ওয়ালেট নির্বাচিত: ${chosenWallet.name}-এ আনুমানিক $targetFormatted আপডেট হবে।"
                            } else {
                                "Cross-currency wallet selected: ${chosenWallet.name} will be updated by approx. $targetFormatted."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                if (isInsufficientLoanWallet && chosenWallet != null) {
                    Surface(
                        color = crystal.liabilityRedBg,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, crystal.liabilityRed.copy(alpha = 0.45f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("loan_settle_insufficient_balance_error")
                    ) {
                        Text(
                            text = com.example.data.model.AccountBalanceGuard.insufficientBalanceMessage(
                                account = chosenWallet,
                                requestedAmount = walletImpactAmount,
                                language = language
                            ),
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = crystal.liabilityRed,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            val walletImpactForConfirm = if (chosenWallet != null && effectiveAmount > 0.0) {
                if (chosenWallet.currencyCode.equals(loan.currencyCode, ignoreCase = true)) {
                    effectiveAmount
                } else {
                    marketSnapshot.convertCurrencyCode(effectiveAmount, loan.currencyCode, chosenWallet.currencyCode)
                }
            } else {
                0.0
            }
            val blockedByBalance = loan.loanType == LoanType.I_OWE &&
                chosenWallet != null &&
                com.example.data.model.AccountBalanceGuard.isInsufficientForDeduction(chosenWallet, walletImpactForConfirm)
            Button(
                onClick = {
                    if (effectiveAmount > 0.0 && !blockedByBalance) {
                        onConfirmSettle(loan, effectiveAmount, chosenWalletId)
                    }
                },
                enabled = effectiveAmount > 0.0 && !blockedByBalance,
                modifier = Modifier.testTag("loan_settle_confirm_button")
            ) {
                Text(strings.settleLoan)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
        }
    )
}

internal fun formatLoanCurrencyAmount(
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

private fun formatEditableLoanDecimal(amount: Double): String = when {
    amount <= 0.0 -> ""
    abs(amount - amount.roundToLong()) < 0.005 -> amount.roundToLong().toString()
    abs(amount * 100.0 - (amount * 100.0).roundToLong()) >= 0.05 -> String.format(Locale.US, "%.3f", amount)
    else -> String.format(Locale.US, "%.2f", amount)
}

private fun sanitizeLoanDecimal(input: String): String {
    val ascii = ReceiptVoucherMatcher.toAsciiDigits(input)
    var dotSeen = false
    return buildString {
        for (c in ascii) {
            if (c.isDigit()) {
                append(c)
            } else if (c == '.' && !dotSeen) {
                dotSeen = true
                append(c)
            }
        }
    }
}
