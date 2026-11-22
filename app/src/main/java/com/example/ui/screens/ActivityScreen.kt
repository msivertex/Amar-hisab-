package com.example.ui.screens

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import com.example.data.local.TransactionEntity
import com.example.data.market.MarketSnapshot
import com.example.data.model.AccountNameResolver
import com.example.data.model.AppCurrency
import com.example.data.model.TransactionSortOption
import com.example.data.model.TransactionType
import com.example.data.model.TransferNoteCodec
import com.example.localization.AppStrings
import com.example.localization.BdtFormatter
import com.example.ui.components.CrystalCard
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalCrystalColors
import com.example.ui.viewmodel.MainUiState
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private data class DateGroup(
    val dateKey: String,
    val displayHeader: String,
    val netDeltasByCurrency: Map<String, Double>,
    val transferTotalsByCurrency: Map<String, Double>,
    val items: List<TransactionEntity>
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ActivityScreen(
    state: MainUiState,
    strings: AppStrings,
    onOpenReceiptScan: () -> Unit,
    onDeleteTransaction: (Long) -> Unit,
    marketSnapshot: MarketSnapshot = MarketSnapshot.defaultReference()
) {
    val crystal = LocalCrystalColors.current
    val lang = state.prefs.language
    val mainCurCode = state.prefs.mainCurrencyCode

    var searchQuery by remember { mutableStateOf("") }
    var selectedTypeFilter by remember { mutableStateOf<TransactionType?>(null) }
    var sortOption by remember { mutableStateOf(TransactionSortOption.NEWEST) }
    var showSortMenu by remember { mutableStateOf(false) }
    var selectedVoucherTx by remember { mutableStateOf<TransactionEntity?>(null) }

    val accountsById = remember(state.accounts) {
        state.accounts.associateBy { it.id }
    }

    val filteredTransactions = remember(state.transactions, accountsById, searchQuery, selectedTypeFilter, sortOption, lang) {
        val base = state.transactions.filter { tx ->
            val matchesType = selectedTypeFilter == null || tx.type == selectedTypeFilter
            val walletDisplay = AccountNameResolver.resolveTransactionWalletDisplay(tx, accountsById, lang)
            val matchesQuery = searchQuery.isBlank() ||
                tx.title.contains(searchQuery, ignoreCase = true) ||
                tx.note.contains(searchQuery, ignoreCase = true) ||
                tx.subcategory.contains(searchQuery, ignoreCase = true) ||
                walletDisplay.contains(searchQuery, ignoreCase = true) ||
                tx.category.en.contains(searchQuery, ignoreCase = true) ||
                tx.category.bn.contains(searchQuery, ignoreCase = true)
            matchesType && matchesQuery
        }
        when (sortOption) {
            TransactionSortOption.NEWEST -> base.sortedByDescending { it.timestamp }
            TransactionSortOption.OLDEST -> base.sortedBy { it.timestamp }
            TransactionSortOption.HIGHEST -> base.sortedByDescending { it.amount }
            TransactionSortOption.LOWEST -> base.sortedBy { it.amount }
        }
    }

    val groupedByDate = remember(
        filteredTransactions,
        lang,
        mainCurCode,
        state.deviceTimeContext.dayKey,
        state.deviceTimeContext.timeZoneId
    ) {
        val activeTz = state.deviceTimeContext.timeZone
        val dayKeyFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = activeTz }
        val headerFmt = SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault()).apply { timeZone = activeTz }
        val todayKey = state.deviceTimeContext.dayKey
        val yestCal = Calendar.getInstance(activeTz).apply {
            timeInMillis = state.deviceTimeContext.epochMillis
            add(Calendar.DAY_OF_MONTH, -1)
        }
        val yestKey = dayKeyFmt.format(yestCal.time)

        val linked = linkedMapOf<String, MutableList<TransactionEntity>>()
        filteredTransactions.forEach { tx ->
            val key = dayKeyFmt.format(Date(tx.timestamp))
            linked.getOrPut(key) { mutableListOf() }.add(tx)
        }

        linked.map { (key, list) ->
            val rawHeader = headerFmt.format(Date(list.first().timestamp))
            val label = when (key) {
                todayKey -> "${strings.todayLabel} • $rawHeader"
                yestKey -> "${strings.yesterdayLabel} • $rawHeader"
                else -> rawHeader
            }
            val netByCur = linkedMapOf<String, Double>()
            val transfersByCur = linkedMapOf<String, Double>()
            list.forEach { tx ->
                val cur = AppCurrency.fromCode(tx.currencyCode).code
                when (tx.type) {
                    TransactionType.INCOME -> netByCur[cur] = (netByCur[cur] ?: 0.0) + tx.amount
                    TransactionType.EXPENSE -> netByCur[cur] = (netByCur[cur] ?: 0.0) - tx.amount
                    TransactionType.TRANSFER -> transfersByCur[cur] = (transfersByCur[cur] ?: 0.0) + tx.amount
                }
            }
            DateGroup(
                dateKey = key,
                displayHeader = label,
                netDeltasByCurrency = netByCur.filterValues { kotlin.math.abs(it) >= 0.005 },
                transferTotalsByCurrency = transfersByCur.filterValues { it >= 0.005 },
                items = list
            )
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.activityTitle,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    OutlinedButton(
                        onClick = onOpenReceiptScan,
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DocumentScanner,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.size(6.dp))
                        Text(strings.actionReceiptScan)
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(strings.searchTransactionsHint) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Filled.Close, contentDescription = strings.close)
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("activity_search_input")
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedTypeFilter == null,
                            onClick = { selectedTypeFilter = null },
                            label = { Text(strings.filterAll) }
                        )
                        FilterChip(
                            selected = selectedTypeFilter == TransactionType.EXPENSE,
                            onClick = { selectedTypeFilter = TransactionType.EXPENSE },
                            label = { Text(strings.expenseLabel) }
                        )
                        FilterChip(
                            selected = selectedTypeFilter == TransactionType.INCOME,
                            onClick = { selectedTypeFilter = TransactionType.INCOME },
                            label = { Text(strings.incomeLabel) }
                        )
                        FilterChip(
                            selected = selectedTypeFilter == TransactionType.TRANSFER,
                            onClick = { selectedTypeFilter = TransactionType.TRANSFER },
                            label = { Text(strings.transferLabel) }
                        )
                    }

                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(
                                imageVector = Icons.Filled.Sort,
                                contentDescription = strings.sortLabel,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false },
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        ) {
                            TransactionSortOption.entries.forEach { option ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = option.label(lang),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    onClick = {
                                        sortOption = option
                                        showSortMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            if (groupedByDate.isEmpty()) {
                item {
                    CrystalCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ReceiptLong,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(44.dp)
                            )
                            Text(
                                text = strings.noTransactionsTitle,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = strings.noTransactionsSubtitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = crystal.mutedText
                            )
                        }
                    }
                }
            } else {
                groupedByDate.forEach { group ->
                    item(key = "header_${group.dateKey}") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = group.displayHeader,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = crystal.mutedText,
                                    modifier = Modifier.weight(1f)
                                )
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    group.netDeltasByCurrency.forEach { (curCode, deltaVal) ->
                                        Surface(
                                            color = if (deltaVal >= 0) crystal.assetGreenBg else crystal.liabilityRedBg,
                                            shape = CircleShape,
                                            modifier = Modifier.testTag("activity_delta_pill_${group.dateKey}_$curCode")
                                        ) {
                                            Text(
                                                text = "${strings.dailyNetDelta} ${BdtFormatter.formatCurrency(deltaVal, curCode, lang, includeSign = true)}",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontFamily = JetBrainsMonoFamily,
                                                    fontWeight = FontWeight.Bold
                                                ),
                                                color = if (deltaVal >= 0) crystal.assetGreen else crystal.liabilityRed,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                    group.transferTotalsByCurrency.forEach { (curCode, transferAmt) ->
                                        Surface(
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = if (crystal.isDark) 0.22f else 0.12f),
                                            shape = CircleShape,
                                            modifier = Modifier.testTag("activity_transfer_pill_${group.dateKey}_$curCode")
                                        ) {
                                            Text(
                                                text = "⇄ ${BdtFormatter.formatCurrency(transferAmt, curCode, lang)}",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontFamily = JetBrainsMonoFamily,
                                                    fontWeight = FontWeight.Bold
                                                ),
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                    if (group.netDeltasByCurrency.isEmpty() && group.transferTotalsByCurrency.isEmpty()) {
                                        Surface(
                                            color = crystal.assetGreenBg,
                                            shape = CircleShape
                                        ) {
                                            Text(
                                                text = "${strings.dailyNetDelta} ${BdtFormatter.formatCurrency(0.0, mainCurCode, lang)}",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontFamily = JetBrainsMonoFamily,
                                                    fontWeight = FontWeight.Bold
                                                ),
                                                color = crystal.assetGreen,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    items(group.items, key = { it.id }) { tx ->
                        val accName = accountsById[tx.accountId]?.let {
                            AccountNameResolver.resolveAccountName(it, lang)
                        }.orEmpty()
                        val displayNote = if (tx.type == TransactionType.TRANSFER) {
                            TransferNoteCodec.stripEncodedSegment(tx.note)
                        } else {
                            tx.note.trim()
                        }
                        val displaySubcat = if (tx.type == TransactionType.TRANSFER) "" else tx.subcategory.trim()
                        CrystalCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("activity_tx_card_${tx.id}"),
                            onClick = { selectedVoucherTx = tx }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    TransactionRowItem(
                                        tx = tx,
                                        accountName = accName,
                                        language = lang,
                                        accountsById = accountsById,
                                        marketSnapshot = marketSnapshot
                                    )
                                }
                                IconButton(
                                    onClick = { onDeleteTransaction(tx.id) },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.DeleteOutline,
                                        contentDescription = strings.deleteAccount,
                                        tint = crystal.mutedText,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            if (displaySubcat.isNotBlank() || displayNote.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                val meta = listOf(displaySubcat, displayNote).filter { it.isNotBlank() }.joinToString(" • ")
                                Text(
                                    text = meta,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = crystal.mutedText
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    selectedVoucherTx?.let { voucherTx ->
        TransactionVoucherDetailSheet(
            tx = voucherTx,
            accountsById = accountsById,
            language = lang,
            strings = strings,
            marketSnapshot = marketSnapshot,
            onDelete = { onDeleteTransaction(voucherTx.id) },
            onDismiss = { selectedVoucherTx = null }
        )
    }
}
