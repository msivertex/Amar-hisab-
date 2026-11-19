package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.LoanEntity
import com.example.data.local.SavingsGoalEntity
import com.example.data.model.ExpenseCategory
import com.example.data.model.LoanType
import com.example.localization.AppStrings
import com.example.localization.BdtFormatter
import com.example.ui.components.CategoryIconBadge
import com.example.ui.components.CrystalCard
import com.example.ui.theme.AssetEmerald
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalCrystalColors
import com.example.ui.theme.SkyCyanPrimary
import com.example.ui.viewmodel.MainUiState
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun PlanScreen(
    state: MainUiState,
    strings: AppStrings,
    onOpenAllAccounts: () -> Unit,
    onOpenAddWizard: () -> Unit,
    onEditBudget: () -> Unit,
    onUpsertCategoryBudget: (ExpenseCategory, Double) -> Unit,
    onAddGoal: (String, Double, Long?, Long) -> Unit,
    onFundGoalFromWallet: (SavingsGoalEntity, Double, Long) -> Unit,
    onDeleteGoal: (Long) -> Unit,
    onAddLoan: (String, LoanType, Double, String, Long?) -> Unit,
    onSettleLoan: (LoanEntity, Double, Long) -> Unit,
    onDeleteLoan: (Long) -> Unit
) {
    val crystal = LocalCrystalColors.current
    val lang = state.prefs.language
    val context = LocalContext.current
    val dateFmt = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    var activePlanTab by remember { mutableIntStateOf(0) } // 0: Budgets, 1: Goals, 2: Loans
    var showNewGoalDialog by remember { mutableStateOf(false) }
    var fundingGoal by remember { mutableStateOf<SavingsGoalEntity?>(null) }
    var editingCatBudget by remember { mutableStateOf<ExpenseCategory?>(null) }
    var showNewLoanDialog by remember { mutableStateOf(false) }
    var settlingLoan by remember { mutableStateOf<LoanEntity?>(null) }
    var loanFilterType by remember { mutableStateOf(LoanType.I_OWE) }

    val categoryBudgetMap = remember(state.categoryBudgets) {
        state.categoryBudgets.associate { it.categoryName to it.limitAmount }
    }

    val budgetCategories = remember {
        ExpenseCategory.entries.filter {
            it != ExpenseCategory.SALARY_INCOME && it != ExpenseCategory.TRANSFER
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 620.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 112.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text(
                    text = strings.planTitle,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            item {
                NetWorthHeaderCard(
                    netWorth = state.netWorth,
                    language = lang,
                    strings = strings
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onOpenAllAccounts,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.size(6.dp))
                        Text(strings.accountsTitle, maxLines = 1)
                    }
                    Button(
                        onClick = onOpenAddWizard,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("plan_add_account_button")
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.size(6.dp))
                        Text(strings.addAccount, maxLines = 1)
                    }
                }
            }

            // Segmented Sub-Tab Switcher: Budgets | Savings Goals | Loans & Debt
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = activePlanTab == 0,
                        onClick = { activePlanTab = 0 },
                        label = { Text(strings.planTabBudgets) },
                        modifier = Modifier.testTag("plan_tab_budgets")
                    )
                    FilterChip(
                        selected = activePlanTab == 1,
                        onClick = { activePlanTab = 1 },
                        label = { Text(strings.planTabGoals) },
                        modifier = Modifier.testTag("plan_tab_goals")
                    )
                    FilterChip(
                        selected = activePlanTab == 2,
                        onClick = { activePlanTab = 2 },
                        label = { Text(strings.planTabLoans) },
                        modifier = Modifier.testTag("plan_tab_loans")
                    )
                }
            }

            when (activePlanTab) {
                0 -> {
                    item {
                        CrystalCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = strings.adjustBudget,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = crystal.mutedText
                                    )
                                    Text(
                                        text = BdtFormatter.format(state.prefs.monthlyBudget, lang),
                                        style = MaterialTheme.typography.headlineSmall.copy(
                                            fontFamily = JetBrainsMonoFamily,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                                FilledTonalButton(onClick = onEditBudget) {
                                    Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.size(6.dp))
                                    Text(strings.monthlyBudget)
                                }
                            }
                        }
                    }

                    item {
                        Text(
                            text = strings.categoryBudgetsTitle,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    items(budgetCategories, key = { it.name }) { cat ->
                        val limit = categoryBudgetMap[cat.name] ?: 0.0
                        val spent = state.categorySpentMap[cat] ?: 0.0
                        val ratio = if (limit > 0.0) (spent / limit).toFloat().coerceIn(0f, 1f) else 0f

                        CrystalCard(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { editingCatBudget = cat }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    CategoryIconBadge(category = cat, size = 38.dp)
                                    Column {
                                        Text(cat.label(lang), style = MaterialTheme.typography.titleSmall)
                                        Text(
                                            text = if (limit > 0.0) {
                                                "${BdtFormatter.format(spent, lang)} / ${BdtFormatter.format(limit, lang)}"
                                            } else {
                                                "${strings.spentLabel}: ${BdtFormatter.format(spent, lang)}"
                                            },
                                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFamily),
                                            color = crystal.mutedText
                                        )
                                    }
                                }
                                TextButton(onClick = { editingCatBudget = cat }) {
                                    Text(strings.setCategoryBudget)
                                }
                            }
                            if (limit > 0.0) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(CircleShape)
                                        .background(cat.color.copy(alpha = 0.16f))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(ratio)
                                            .height(8.dp)
                                            .clip(CircleShape)
                                            .background(if (spent > limit) crystal.liabilityRed else cat.color)
                                    )
                                }
                            }
                        }
                    }
                }

                1 -> {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = strings.savingsGoals,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Button(onClick = { showNewGoalDialog = true }, shape = RoundedCornerShape(12.dp)) {
                                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.size(4.dp))
                                Text(strings.addGoal)
                            }
                        }
                    }

                    items(state.goals, key = { it.id }) { goal ->
                        val ratio = if (goal.targetAmount > 0) {
                            (goal.savedAmount / goal.targetAmount).toFloat().coerceIn(0f, 1f)
                        } else 0f
                        val pct = (ratio * 100).toInt()
                        val animatedRatio by animateFloatAsState(
                            targetValue = ratio,
                            animationSpec = spring(),
                            label = "goalProgress${goal.id}"
                        )
                        val goalColor = Color(goal.colorArgb)

                        CrystalCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(goalColor.copy(alpha = 0.16f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Filled.Savings, contentDescription = null, tint = goalColor)
                                    }
                                    Column {
                                        Text(goal.name, style = MaterialTheme.typography.titleSmall)
                                        Text(
                                            text = "${BdtFormatter.format(goal.savedAmount, lang)} / ${BdtFormatter.format(goal.targetAmount, lang)} (${BdtFormatter.formatPercent(pct, lang)})",
                                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFamily),
                                            color = crystal.mutedText
                                        )
                                        if (goal.dueDateMillis != null) {
                                            Text(
                                                text = "${strings.dueDateLabel}: ${dateFmt.format(Date(goal.dueDateMillis))}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = crystal.mutedText
                                            )
                                        }
                                    }
                                }

                                IconButton(onClick = { onDeleteGoal(goal.id) }) {
                                    Icon(Icons.Filled.DeleteOutline, contentDescription = strings.deleteAccount, tint = crystal.mutedText)
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .clip(CircleShape)
                                    .background(goalColor.copy(alpha = 0.16f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(animatedRatio)
                                        .height(10.dp)
                                        .clip(CircleShape)
                                        .background(goalColor)
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            FilledTonalButton(
                                onClick = { fundingGoal = goal },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(strings.fundFromWallet)
                            }
                        }
                    }
                }

                2 -> {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = strings.loansTitle,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Button(
                                onClick = { showNewLoanDialog = true },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("add_loan_button")
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.size(4.dp))
                                Text(strings.addLoan)
                            }
                        }
                    }

                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = loanFilterType == LoanType.I_OWE,
                                onClick = { loanFilterType = LoanType.I_OWE },
                                label = { Text(strings.moneyIOwe) }
                            )
                            FilterChip(
                                selected = loanFilterType == LoanType.OWED_TO_ME,
                                onClick = { loanFilterType = LoanType.OWED_TO_ME },
                                label = { Text(strings.moneyOwedToMe) }
                            )
                        }
                    }

                    val filteredLoans = state.loans.filter { it.loanType == loanFilterType }
                    items(filteredLoans, key = { it.id }) { loan ->
                        val ratio = if (loan.totalAmount > 0) {
                            (loan.paidAmount / loan.totalAmount).toFloat().coerceIn(0f, 1f)
                        } else 0f
                        val accent = if (loan.loanType == LoanType.I_OWE) crystal.liabilityRed else crystal.assetGreen

                        CrystalCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(accent.copy(alpha = 0.14f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (loan.isSettled) Icons.Filled.CheckCircle else Icons.Filled.Handshake,
                                            contentDescription = null,
                                            tint = accent
                                        )
                                    }
                                    Column {
                                        Text(loan.personName, style = MaterialTheme.typography.titleSmall)
                                        Text(
                                            text = "${strings.remainingDueLabel}: ${BdtFormatter.format(loan.remainingAmount, lang)} / ${BdtFormatter.format(loan.totalAmount, lang)}",
                                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFamily),
                                            color = crystal.mutedText
                                        )
                                        if (loan.note.isNotBlank()) {
                                            Text(loan.note, style = MaterialTheme.typography.bodySmall, color = crystal.mutedText)
                                        }
                                        if (loan.dueDateMillis != null) {
                                            Text(
                                                text = "${strings.dueDateLabel}: ${dateFmt.format(Date(loan.dueDateMillis))}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = accent
                                            )
                                        }
                                    }
                                }
                                IconButton(onClick = { onDeleteLoan(loan.id) }) {
                                    Icon(Icons.Filled.DeleteOutline, contentDescription = strings.deleteAccount, tint = crystal.mutedText)
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
                                    shape = CircleShape
                                ) {
                                    Text(
                                        text = strings.settledBadge,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = crystal.assetGreen,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            } else {
                                FilledTonalButton(
                                    onClick = { settlingLoan = loan },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(strings.settleLoan)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialog: New Savings Goal with Due Date
    if (showNewGoalDialog) {
        var goalName by remember { mutableStateOf("") }
        var targetText by remember { mutableStateOf("") }
        var dueMillis by remember { mutableStateOf<Long?>(null) }

        AlertDialog(
            onDismissRequest = { showNewGoalDialog = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurface,
            title = { Text(strings.addGoal) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = goalName,
                        onValueChange = { goalName = it },
                        label = { Text(strings.goalNameHint) },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = targetText,
                        onValueChange = { targetText = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text(strings.targetAmountHint) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    OutlinedButton(
                        onClick = {
                            val cal = Calendar.getInstance()
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
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.size(6.dp))
                        Text(
                            dueMillis?.let { "${strings.dueDateLabel}: ${dateFmt.format(Date(it))}" }
                                ?: "${strings.dueDateLabel} (${strings.pickDateLabel})"
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val target = targetText.toDoubleOrNull() ?: 0.0
                        if (goalName.isNotBlank() && target > 0.0) {
                            onAddGoal(goalName, target, dueMillis, SkyCyanPrimary.toArgb().toLong() and 0xFFFFFFFFL)
                            showNewGoalDialog = false
                        }
                    }
                ) {
                    Text(strings.saveChanges)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewGoalDialog = false }) { Text(strings.cancel) }
            }
        )
    }

    // Dialog: Fund Savings Goal from Wallet
    fundingGoal?.let { goal ->
        var amountText by remember { mutableStateOf("1000") }
        var chosenWalletId by remember(state.accounts) {
            mutableLongStateOf(state.accounts.firstOrNull()?.id ?: 0L)
        }

        AlertDialog(
            onDismissRequest = { fundingGoal = null },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurface,
            title = { Text("${strings.fundGoalTitle}: ${goal.name}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text(strings.amountLabel) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    Text(strings.selectWalletLabel, style = MaterialTheme.typography.labelLarge)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        state.accounts.forEach { acc ->
                            FilterChip(
                                selected = chosenWalletId == acc.id,
                                onClick = { chosenWalletId = acc.id },
                                label = { Text("${acc.name} (${BdtFormatter.format(acc.balance, lang)})") }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = amountText.toDoubleOrNull() ?: 0.0
                        if (amt > 0.0 && chosenWalletId > 0L) {
                            onFundGoalFromWallet(goal, amt, chosenWalletId)
                            fundingGoal = null
                        }
                    }
                ) { Text(strings.saveChanges) }
            },
            dismissButton = {
                TextButton(onClick = { fundingGoal = null }) { Text(strings.cancel) }
            }
        )
    }

    // Dialog: Set Category-Wise Budget
    editingCatBudget?.let { cat ->
        var limitText by remember(cat) {
            mutableStateOf((categoryBudgetMap[cat.name]?.toLong() ?: 5000L).toString())
        }
        AlertDialog(
            onDismissRequest = { editingCatBudget = null },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurface,
            title = { Text(cat.label(lang)) },
            text = {
                OutlinedTextField(
                    value = limitText,
                    onValueChange = { limitText = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text(strings.monthlyBudgetInput) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val lim = limitText.toDoubleOrNull() ?: 0.0
                        if (lim > 0.0) {
                            onUpsertCategoryBudget(cat, lim)
                            editingCatBudget = null
                        }
                    }
                ) { Text(strings.saveChanges) }
            },
            dismissButton = {
                TextButton(onClick = { editingCatBudget = null }) { Text(strings.cancel) }
            }
        )
    }

    // Dialog: Add Loan / Debt Entry
    if (showNewLoanDialog) {
        var person by remember { mutableStateOf("") }
        var amountText by remember { mutableStateOf("") }
        var note by remember { mutableStateOf("") }
        var type by remember { mutableStateOf(loanFilterType) }
        var dueMillis by remember { mutableStateOf<Long?>(null) }

        AlertDialog(
            onDismissRequest = { showNewLoanDialog = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurface,
            title = { Text(strings.addLoan) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = type == LoanType.I_OWE,
                            onClick = { type = LoanType.I_OWE },
                            label = { Text(strings.moneyIOwe) }
                        )
                        FilterChip(
                            selected = type == LoanType.OWED_TO_ME,
                            onClick = { type = LoanType.OWED_TO_ME },
                            label = { Text(strings.moneyOwedToMe) }
                        )
                    }
                    OutlinedTextField(
                        value = person,
                        onValueChange = { person = it },
                        label = { Text(strings.personNameLabel) },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text(strings.loanAmountLabel) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text(strings.noteOptionalLabel) },
                        singleLine = true
                    )
                    OutlinedButton(
                        onClick = {
                            val cal = Calendar.getInstance()
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
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            dueMillis?.let { "${strings.dueDateLabel}: ${dateFmt.format(Date(it))}" }
                                ?: "${strings.dueDateLabel} (${strings.pickDateLabel})"
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = amountText.toDoubleOrNull() ?: 0.0
                        if (person.isNotBlank() && amt > 0.0) {
                            onAddLoan(person, type, amt, note, dueMillis)
                            loanFilterType = type
                            showNewLoanDialog = false
                        }
                    }
                ) { Text(strings.saveChanges) }
            },
            dismissButton = {
                TextButton(onClick = { showNewLoanDialog = false }) { Text(strings.cancel) }
            }
        )
    }

    // Dialog: Partial or Full Loan Settlement
    settlingLoan?.let { loan ->
        var settleText by remember(loan) { mutableStateOf(loan.remainingAmount.toLong().toString()) }
        var chosenWalletId by remember(state.accounts) {
            mutableLongStateOf(state.accounts.firstOrNull()?.id ?: 0L)
        }

        AlertDialog(
            onDismissRequest = { settlingLoan = null },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurface,
            title = { Text("${strings.settleLoan}: ${loan.personName}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = settleText,
                        onValueChange = { settleText = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text(strings.settlementAmountLabel) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    Text(strings.selectWalletLabel, style = MaterialTheme.typography.labelLarge)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        state.accounts.forEach { acc ->
                            FilterChip(
                                selected = chosenWalletId == acc.id,
                                onClick = { chosenWalletId = acc.id },
                                label = { Text(acc.name) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = settleText.toDoubleOrNull() ?: 0.0
                        if (amt > 0.0 && chosenWalletId > 0L) {
                            onSettleLoan(loan, amt, chosenWalletId)
                            settlingLoan = null
                        }
                    }
                ) { Text(strings.settleLoan) }
            },
            dismissButton = {
                TextButton(onClick = { settlingLoan = null }) { Text(strings.cancel) }
            }
        )
    }
}
