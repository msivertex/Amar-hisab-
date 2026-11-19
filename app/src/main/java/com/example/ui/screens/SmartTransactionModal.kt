package com.example.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.CalculatorEngine
import com.example.data.local.AccountEntity
import com.example.data.model.ExpenseCategory
import com.example.data.model.TransactionType
import com.example.localization.AppLanguage
import com.example.localization.AppStrings
import com.example.localization.BdtFormatter
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalCrystalColors
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartTransactionModal(
    accounts: List<AccountEntity>,
    initialMode: TransactionType = TransactionType.EXPENSE,
    defaultAccountId: Long = 0L,
    autoOpenKeypad: Boolean = true,
    enableSubcategories: Boolean,
    hapticEnabled: Boolean,
    language: AppLanguage,
    strings: AppStrings,
    onDismiss: () -> Unit,
    onSaveExpenseOrIncome: (
        title: String,
        amount: Double,
        type: TransactionType,
        category: ExpenseCategory,
        subcategory: String,
        accountId: Long,
        note: String,
        timestamp: Long
    ) -> Unit,
    onExecuteTransfer: (
        fromId: Long,
        toId: Long,
        amount: Double,
        note: String,
        timestamp: Long
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val crystal = LocalCrystalColors.current
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    var mode by remember { mutableStateOf(initialMode) }
    var expression by remember { mutableStateOf("") }
    var keypadVisible by remember { mutableStateOf(autoOpenKeypad) }
    var title by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var selectedCategory by remember {
        mutableStateOf(
            if (initialMode == TransactionType.INCOME) ExpenseCategory.SALARY_INCOME
            else ExpenseCategory.FOOD_DINING
        )
    }
    var selectedSubcategory by remember(selectedCategory) {
        mutableStateOf(selectedCategory.subcategories().firstOrNull()?.key.orEmpty())
    }
    val resolvedDefaultId = remember(accounts, defaultAccountId) {
        accounts.firstOrNull { it.id == defaultAccountId }?.id ?: accounts.firstOrNull()?.id ?: 0L
    }
    var selectedAccountId by remember(resolvedDefaultId) { mutableLongStateOf(resolvedDefaultId) }
    var toAccountId by remember(accounts, resolvedDefaultId) {
        mutableLongStateOf(
            accounts.firstOrNull { it.id != resolvedDefaultId }?.id ?: resolvedDefaultId
        )
    }
    var selectedTimestamp by remember { mutableLongStateOf(System.currentTimeMillis()) }

    val evaluatedAmount = remember(expression) {
        CalculatorEngine.evaluate(expression) ?: 0.0
    }

    val availableCategories = remember(mode) {
        when (mode) {
            TransactionType.INCOME -> listOf(ExpenseCategory.SALARY_INCOME, ExpenseCategory.OTHER)
            TransactionType.EXPENSE -> ExpenseCategory.entries.filter {
                it != ExpenseCategory.SALARY_INCOME && it != ExpenseCategory.TRANSFER
            }
            TransactionType.TRANSFER -> listOf(ExpenseCategory.TRANSFER)
        }
    }

    fun triggerHaptic() {
        if (hapticEnabled) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    fun onKeypadPress(key: String) {
        triggerHaptic()
        when (key) {
            "C" -> expression = ""
            "⌫" -> if (expression.isNotEmpty()) expression = expression.dropLast(1)
            "=" -> {
                CalculatorEngine.evaluate(expression)?.let { valRes ->
                    expression = CalculatorEngine.formatResult(valRes)
                }
            }
            "+", "-", "×", "÷" -> {
                if (expression.isEmpty() && key == "-") {
                    expression = "-"
                } else if (expression.isNotEmpty()) {
                    val last = expression.last().toString()
                    expression = if (last in listOf("+", "-", "×", "÷")) {
                        expression.dropLast(1) + key
                    } else {
                        expression + key
                    }
                }
            }
            "." -> {
                val lastSegment = expression.split("+", "-", "×", "÷").lastOrNull().orEmpty()
                if (!lastSegment.contains(".")) {
                    expression += if (lastSegment.isEmpty()) "0." else "."
                }
            }
            else -> {
                if (expression.length < 22) expression += key
            }
        }
    }

    val activeTzId = java.util.TimeZone.getDefault().id
    val dateFormatter = remember(activeTzId) {
        SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).apply {
            timeZone = java.util.TimeZone.getTimeZone(activeTzId)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = strings.addTransactionTitle,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            // 3 Transaction Modes: Expense, Income, Transfer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    TransactionType.EXPENSE to strings.expenseLabel,
                    TransactionType.INCOME to strings.incomeLabel,
                    TransactionType.TRANSFER to strings.transferLabel
                ).forEach { (m, label) ->
                    FilterChip(
                        selected = mode == m,
                        onClick = {
                            triggerHaptic()
                            mode = m
                            selectedCategory = when (m) {
                                TransactionType.INCOME -> ExpenseCategory.SALARY_INCOME
                                TransactionType.TRANSFER -> ExpenseCategory.TRANSFER
                                TransactionType.EXPENSE -> ExpenseCategory.FOOD_DINING
                            }
                        },
                        label = { Text(label) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("tx_mode_${m.name}")
                    )
                }
            }

            // Calculator Display + Built-in Smart Keypad
            Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, crystal.glassBorder, RoundedCornerShape(18.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Calculate,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = strings.amountLabel,
                                style = MaterialTheme.typography.labelMedium,
                                color = crystal.mutedText
                            )
                        }
                        Text(
                            text = expression.ifBlank { strings.calculatorExprHint },
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFamily),
                            color = crystal.mutedText
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = BdtFormatter.format(evaluatedAmount, language),
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = when (mode) {
                            TransactionType.EXPENSE -> crystal.liabilityRed
                            TransactionType.INCOME -> crystal.assetGreen
                            TransactionType.TRANSFER -> MaterialTheme.colorScheme.primary
                        },
                        modifier = Modifier.testTag("calc_display_amount")
                    )
                }
            }

            if (keypadVisible) {
                SmartCalculatorKeypad(
                    onKeyPress = ::onKeypadPress
                )
            } else {
                OutlinedTextField(
                    value = expression,
                    onValueChange = { expression = it.filter { c -> c.isDigit() || c == '.' || c in listOf('+', '-', '×', '÷', '*', '/') } },
                    label = { Text(strings.amountLabel) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            androidx.compose.material3.TextButton(
                onClick = { keypadVisible = !keypadVisible },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(if (keypadVisible) strings.hideKeypadBtn else strings.showKeypadBtn)
            }

            // Account / Wallet Selectors
            if (mode == TransactionType.TRANSFER) {
                Text(text = strings.fromWalletLabel, style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    accounts.forEach { acc ->
                        FilterChip(
                            selected = selectedAccountId == acc.id,
                            onClick = { selectedAccountId = acc.id },
                            label = { Text("${acc.name} • ${BdtFormatter.format(acc.balance, language)}") }
                        )
                    }
                }

                Text(text = strings.toWalletLabel, style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    accounts.forEach { acc ->
                        FilterChip(
                            selected = toAccountId == acc.id,
                            onClick = { toAccountId = acc.id },
                            label = { Text("${acc.name} • ${BdtFormatter.format(acc.balance, language)}") }
                        )
                    }
                }
            } else {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(strings.titleOrMerchantLabel) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tx_title_input")
                )

                Text(text = strings.selectWalletLabel, style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    accounts.forEach { acc ->
                        FilterChip(
                            selected = selectedAccountId == acc.id,
                            onClick = { selectedAccountId = acc.id },
                            label = { Text("${acc.name} (${BdtFormatter.format(acc.balance, language)})") }
                        )
                    }
                }

                Text(text = strings.categoryLabel, style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    availableCategories.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat.label(language)) }
                        )
                    }
                }

                if (enableSubcategories) {
                    val subs = selectedCategory.subcategories()
                    if (subs.isNotEmpty()) {
                        Text(text = strings.subcategoryLabel, style = MaterialTheme.typography.labelLarge)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            subs.forEach { sub ->
                                FilterChip(
                                    selected = selectedSubcategory == sub.key,
                                    onClick = { selectedSubcategory = sub.key },
                                    label = { Text(sub.label(language)) }
                                )
                            }
                        }
                    }
                }
            }

            // Date & Time Selector
            Text(text = strings.dateTimeLabel, style = MaterialTheme.typography.labelLarge)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = false,
                    onClick = { selectedTimestamp = System.currentTimeMillis() },
                    label = { Text(strings.todayLabel) }
                )
                FilterChip(
                    selected = false,
                    onClick = {
                        selectedTimestamp = System.currentTimeMillis() - 86_400_000L
                    },
                    label = { Text(strings.yesterdayLabel) }
                )
                FilterChip(
                    selected = true,
                    onClick = {
                        val cal = Calendar.getInstance().apply { timeInMillis = selectedTimestamp }
                        DatePickerDialog(
                            context,
                            { _, y, m, d ->
                                cal.set(Calendar.YEAR, y)
                                cal.set(Calendar.MONTH, m)
                                cal.set(Calendar.DAY_OF_MONTH, d)
                                TimePickerDialog(
                                    context,
                                    { _, hr, min ->
                                        cal.set(Calendar.HOUR_OF_DAY, hr)
                                        cal.set(Calendar.MINUTE, min)
                                        selectedTimestamp = cal.timeInMillis
                                    },
                                    cal.get(Calendar.HOUR_OF_DAY),
                                    cal.get(Calendar.MINUTE),
                                    false
                                ).show()
                            },
                            cal.get(Calendar.YEAR),
                            cal.get(Calendar.MONTH),
                            cal.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    },
                    leadingIcon = {
                        Icon(Icons.Filled.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                    },
                    label = { Text(dateFormatter.format(Date(selectedTimestamp))) }
                )
            }

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text(strings.noteOptionalLabel) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            val canSave = evaluatedAmount > 0.0 && selectedAccountId > 0L &&
                (mode != TransactionType.TRANSFER || (toAccountId > 0L && toAccountId != selectedAccountId))

            Button(
                onClick = {
                    triggerHaptic()
                    if (mode == TransactionType.TRANSFER) {
                        onExecuteTransfer(selectedAccountId, toAccountId, evaluatedAmount, note, selectedTimestamp)
                    } else {
                        val finalTitle = title.ifBlank {
                            if (enableSubcategories && selectedSubcategory.isNotBlank()) selectedSubcategory
                            else selectedCategory.label(language)
                        }
                        onSaveExpenseOrIncome(
                            finalTitle,
                            evaluatedAmount,
                            mode,
                            selectedCategory,
                            if (enableSubcategories) selectedSubcategory else "",
                            selectedAccountId,
                            note,
                            selectedTimestamp
                        )
                    }
                    onDismiss()
                },
                enabled = canSave,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("tx_save_button")
            ) {
                Text(
                    if (mode == TransactionType.TRANSFER) strings.executeTransfer
                    else strings.saveTransaction
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
        }
    }
}

@Composable
private fun SmartCalculatorKeypad(
    onKeyPress: (String) -> Unit
) {
    val rows = listOf(
        listOf("7", "8", "9", "÷"),
        listOf("4", "5", "6", "×"),
        listOf("1", "2", "3", "-"),
        listOf("C", "0", ".", "+"),
        listOf("⌫", "=")
    )

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                row.forEach { key ->
                    val isOp = key in listOf("+", "-", "×", "÷", "=")
                    val isClear = key == "C" || key == "⌫"
                    val weight = if (row.size == 2 && key == "=") 3f else 1f
                    val crystal = LocalCrystalColors.current
                    val bgColor = when {
                        key == "=" -> MaterialTheme.colorScheme.primary
                        isOp -> MaterialTheme.colorScheme.primary.copy(alpha = if (crystal.isDark) 0.20f else 0.14f)
                        isClear -> crystal.liabilityRedBg
                        else -> if (crystal.isDark) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                    }
                    val textColor = when {
                        key == "=" -> MaterialTheme.colorScheme.onPrimary
                        isOp -> MaterialTheme.colorScheme.primary
                        isClear -> crystal.liabilityRed
                        else -> MaterialTheme.colorScheme.onSurface
                    }

                    Box(
                        modifier = Modifier
                            .weight(weight)
                            .height(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(bgColor)
                            .clickable { onKeyPress(key) }
                            .testTag("calc_key_$key"),
                        contentAlignment = Alignment.Center
                    ) {
                        if (key == "⌫") {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Backspace,
                                contentDescription = "Backspace",
                                tint = textColor,
                                modifier = Modifier.size(18.dp)
                            )
                        } else {
                            Text(
                                text = key,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = JetBrainsMonoFamily,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = textColor
                            )
                        }
                    }
                }
            }
        }
    }
}
