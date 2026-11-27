package com.example.ui.screens

import android.net.Uri
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.R
import com.example.data.local.AccountEntity
import com.example.data.local.TransactionEntity
import com.example.data.market.MarketSnapshot
import com.example.data.market.MarketSyncUiState
import com.example.data.model.AccountNameResolver
import com.example.data.model.AccountType
import com.example.data.model.AppCurrency
import com.example.data.model.AuthMode
import com.example.data.model.HomeLayoutStyle
import com.example.data.model.HomeSectionId
import com.example.data.model.LoanType
import com.example.data.model.TransactionType
import com.example.data.model.TransferNoteCodec
import com.example.localization.AppLanguage
import com.example.localization.AppStrings
import com.example.localization.BdtFormatter
import com.example.ui.components.AccountVisualCard
import com.example.ui.components.CardNetworkOrProviderBadge
import com.example.ui.components.CashWalletCompactCard
import com.example.ui.components.CategoryIconBadge
import com.example.ui.components.CrystalCard
import com.example.ui.components.CurrencyAmountText
import com.example.ui.components.UserProfileAvatar
import com.example.ui.components.resolveAccountCardGradient
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalCrystalColors
import com.example.ui.theme.NumericCompactCardBalanceStyle
import com.example.ui.theme.NumericDisplayFontFamily
import com.example.ui.theme.NumericHeroBalanceStyle
import com.example.ui.theme.NumericInlineMetricStyle
import com.example.ui.theme.NumericSummaryPrimaryStyle
import com.example.ui.theme.NumericSummarySecondaryStyle
import com.example.ui.theme.SkyCyanBright
import com.example.ui.theme.SkyCyanPrimary
import com.example.ui.viewmodel.MainUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Suppress("UNUSED_PARAMETER")
@Composable
fun HomeDashboardScreen(
    state: MainUiState,
    strings: AppStrings,
    onOpenSearch: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenProfileEdit: () -> Unit,
    onOpenAllAccounts: () -> Unit,
    onOpenTransfer: () -> Unit,
    onOpenReceiptScan: () -> Unit,
    onOpenQuickExpense: () -> Unit,
    onOpenVoiceEntry: () -> Unit,
    onSeeAllActivity: () -> Unit,
    onUpdateName: (String) -> Unit = {},
    onPickGuestAvatar: (Uri) -> Unit = {},
    onClearGuestAvatar: () -> Unit = {},
    onOpenPlan: () -> Unit = {},
    marketSyncState: MarketSyncUiState = MarketSyncUiState(),
    onOpenLiveMarket: () -> Unit = {},
    onOpenAddAccount: () -> Unit = onOpenAllAccounts
) {
    val crystal = LocalCrystalColors.current
    val lang = state.prefs.language
    val mainCurCode = state.prefs.mainCurrencyCode
    val secondaryCur = state.prefs.secondaryCurrency
    val marketSnapshot = marketSyncState.snapshot
    val haptic = LocalHapticFeedback.current
    val hapticsEnabled = state.prefs.hapticFeedbackEnabled

    fun triggerHaptic() {
        if (hapticsEnabled) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    val visibleSections = remember(state.prefs.homeSectionOrder, state.prefs.hiddenHomeSections) {
        state.prefs.homeSectionOrder.filter { it !in state.prefs.hiddenHomeSections }
    }
    val homeCurrencyBalances = remember(
        state.accounts,
        state.liquidBalancesByCurrency,
        state.totalLiquidBalance,
        mainCurCode,
        secondaryCur
    ) {
        resolveHomeCurrencyBalances(
            accounts = state.accounts,
            liquidBalancesByCurrency = state.liquidBalancesByCurrency,
            fallbackTotalLiquidBalance = state.totalLiquidBalance,
            mainCurrencyCode = mainCurCode,
            secondaryCurrency = secondaryCur
        )
    }
    val mainCurrencyBalance = homeCurrencyBalances.firstOrNull()?.amount ?: 0.0
    val mainCurrencyLiabilities = remember(state.accounts, mainCurCode) {
        val mainNormalized = AppCurrency.fromCode(mainCurCode).code
        state.accounts
            .filter {
                it.type == AccountType.CREDIT_CARD &&
                    AppCurrency.fromCode(it.currencyCode).code == mainNormalized
            }
            .sumOf { it.balance.coerceAtLeast(0.0) }
    }
    val hasMultiCurrencyAccounts = remember(state.accounts, mainCurCode) {
        val mainNormalized = AppCurrency.fromCode(mainCurCode).code
        state.accounts.any { AppCurrency.fromCode(it.currencyCode).code != mainNormalized }
    }
    val displayNetWorth = if (hasMultiCurrencyAccounts) {
        mainCurrencyBalance - mainCurrencyLiabilities
    } else {
        state.netWorth.netWorth
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 620.dp)
                .fillMaxSize()
                .testTag("home_dashboard_list"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 112.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Top Header (Clean Profile Avatar -> Profile & Settings, Read-Only Greeting & Name, Search & Notifications)
            item(key = "header") {
                HomeTopHeader(
                    userName = state.prefs.userName.ifBlank { strings.profileDetails.guestUserFallback },
                    avatarUrl = state.prefs.effectiveAvatarUrl,
                    authMode = state.prefs.authMode,
                    greeting = strings.currentGreeting(),
                    language = lang,
                    hasUnreadAlert = state.smartAlerts.any { it.isWarning },
                    strings = strings,
                    onOpenSearch = {
                        triggerHaptic()
                        onOpenSearch()
                    },
                    onOpenNotifications = {
                        triggerHaptic()
                        onOpenNotifications()
                    },
                    onOpenSettings = {
                        triggerHaptic()
                        onOpenSettings()
                    }
                )
            }

            // 2. User-ordered and toggled dashboard sections (Module G customization)
            items(
                items = visibleSections,
                key = { it.name }
            ) { section ->
                when (section) {
                    HomeSectionId.SUMMARY -> {
                        val useUnifiedSummaryCard =
                            state.prefs.homeLayoutStyle == HomeLayoutStyle.BALANCE_CARD ||
                                homeCurrencyBalances.size > 2
                        key(state.prefs.homeLayoutStyle, useUnifiedSummaryCard) {
                            if (useUnifiedSummaryCard) {
                                HeroBalanceSummaryCard(
                                    currencyBalances = homeCurrencyBalances,
                                    incomeThisMonth = state.incomeThisMonth,
                                    spentThisMonth = state.spentThisMonth,
                                    monthlyBudget = state.prefs.monthlyBudget,
                                    netWorth = displayNetWorth,
                                    dailyAverage = state.dailyAverageSpend,
                                    language = lang,
                                    strings = strings,
                                    mainCurrencyCode = mainCurCode,
                                    onOpenAllAccounts = {
                                        triggerHaptic()
                                        onOpenAllAccounts()
                                    },
                                    onOpenActivity = {
                                        triggerHaptic()
                                        onSeeAllActivity()
                                    }
                                )
                            } else {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(IntrinsicSize.Max)
                                        .testTag("summary_dual_cards"),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    CrystalCard(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .testTag("summary_dual_balance_card"),
                                        elevated = true,
                                        onClick = {
                                            triggerHaptic()
                                            onOpenAllAccounts()
                                        }
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .clip(RoundedCornerShape(9.dp))
                                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.AccountBalanceWallet,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                            Text(
                                                text = strings.totalBalance,
                                                style = MaterialTheme.typography.labelMedium.copy(
                                                    fontWeight = FontWeight.Normal
                                                ),
                                                color = crystal.mutedText,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(10.dp))
                                        val mainEntry = homeCurrencyBalances.first()
                                        val secondaryEntries = homeCurrencyBalances.drop(1)
                                        CurrencyAmountText(
                                            amount = mainEntry.amount,
                                            currencyCode = mainEntry.currency.code,
                                            language = lang,
                                            style = NumericSummaryPrimaryStyle.copy(
                                                fontSize = 18.5.sp,
                                                fontWeight = FontWeight.ExtraBold
                                            ),
                                            isBalance = true,
                                            colorOverride = crystal.bdtNumericInk,
                                            bdtFontScale = 0.82f,
                                            minFontSize = 11.sp,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("home_total_balance_primary_amount")
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        if (secondaryEntries.isNotEmpty()) {
                                            val firstExtra = secondaryEntries.first()
                                            CurrencyAmountText(
                                                amount = firstExtra.amount,
                                                currencyCode = firstExtra.currency.code,
                                                language = lang,
                                                style = NumericSummarySecondaryStyle.copy(
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.Bold
                                                ),
                                                isBalance = true,
                                                colorOverride = crystal.kwdNumericInk,
                                                bdtFontScale = 0.86f,
                                                minFontSize = 10.5.sp,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .testTag("dual_balance_currency_${firstExtra.currency.code}")
                                            )
                                        } else {
                                            Text(
                                                text = strings.liquidAssetsHint,
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontWeight = FontWeight.Normal
                                                ),
                                                color = crystal.mutedText,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    CrystalCard(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .testTag("summary_dual_spent_card"),
                                        elevated = true,
                                        onClick = {
                                            triggerHaptic()
                                            onSeeAllActivity()
                                        }
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .clip(RoundedCornerShape(9.dp))
                                                    .background(crystal.liabilityRed.copy(alpha = 0.14f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.TrendingDown,
                                                    contentDescription = null,
                                                    tint = crystal.liabilityRed,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                            Text(
                                                text = strings.spentThisMonth,
                                                style = MaterialTheme.typography.labelMedium.copy(
                                                    fontWeight = FontWeight.Normal
                                                ),
                                                color = crystal.mutedText,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(10.dp))
                                        CurrencyAmountText(
                                            amount = state.spentThisMonth,
                                            currencyCode = mainCurCode,
                                            language = lang,
                                            style = NumericSummaryPrimaryStyle.copy(
                                                fontSize = 18.5.sp,
                                                fontWeight = FontWeight.ExtraBold
                                            ),
                                            isBalance = true,
                                            colorOverride = crystal.bdtNumericInk,
                                            bdtFontScale = 0.82f,
                                            minFontSize = 11.sp,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("home_spent_month_amount")
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .semantics(mergeDescendants = true) {}
                                                .testTag("home_daily_avg_text"),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                                        ) {
                                            Text(
                                                text = "${strings.dailyAverageLabel}:",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Normal
                                                ),
                                                color = crystal.mutedText,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                            CurrencyAmountText(
                                                amount = state.dailyAverageSpend,
                                                currencyCode = mainCurCode,
                                                language = lang,
                                                style = NumericInlineMetricStyle.copy(
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.Medium
                                                ),
                                                isBalance = true,
                                                colorOverride = crystal.mutedText.copy(alpha = 0.92f),
                                                bdtFontScale = 0.90f,
                                                minFontSize = 9.5.sp,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    HomeSectionId.SHORTCUTS -> {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = strings.quickActions,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                QuickShortcutTile(
                                    label = strings.actionTransfer,
                                    icon = Icons.AutoMirrored.Filled.CompareArrows,
                                    tint = MaterialTheme.colorScheme.primary,
                                    testTag = "quick_transfer",
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        triggerHaptic()
                                        onOpenTransfer()
                                    }
                                )
                                QuickShortcutTile(
                                    label = strings.actionReceiptScan,
                                    icon = Icons.Filled.DocumentScanner,
                                    tint = Color(0xFF0EA5E9),
                                    testTag = "quick_receipt_scan",
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        triggerHaptic()
                                        onOpenReceiptScan()
                                    }
                                )
                                QuickShortcutTile(
                                    label = strings.actionExpense,
                                    icon = Icons.Filled.ShoppingCart,
                                    tint = Color(0xFFF97316),
                                    testTag = "quick_expense",
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        triggerHaptic()
                                        onOpenQuickExpense()
                                    }
                                )
                                QuickShortcutTile(
                                    label = strings.actionVoiceEntry,
                                    icon = Icons.Filled.Mic,
                                    tint = Color(0xFF10B981),
                                    testTag = "quick_voice_entry",
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        triggerHaptic()
                                        onOpenVoiceEntry()
                                    }
                                )
                            }
                        }
                    }

                    HomeSectionId.WALLETS -> {
                        key(state.prefs.homeLayoutStyle) {
                            if (state.prefs.homeLayoutStyle == HomeLayoutStyle.BALANCE_CARD) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = strings.myWallets,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                        TextButton(
                                            onClick = {
                                                triggerHaptic()
                                                onOpenAllAccounts()
                                            },
                                            modifier = Modifier.testTag("all_accounts_button")
                                        ) {
                                            Text(
                                                text = strings.allAccounts,
                                                style = MaterialTheme.typography.labelLarge,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }

                                    CompactWalletStrip(
                                        accounts = state.accounts,
                                        language = lang,
                                        strings = strings,
                                        onOpenAllAccounts = {
                                            triggerHaptic()
                                            onOpenAllAccounts()
                                        },
                                        onOpenAddAccount = {
                                            triggerHaptic()
                                            onOpenAddAccount()
                                        }
                                    )
                                }
                            } else {
                                StackedWalletCarousel(
                                    accounts = state.accounts,
                                    language = lang,
                                    strings = strings,
                                    onCardSwitched = { triggerHaptic() },
                                    onOpenAllAccounts = {
                                        triggerHaptic()
                                        onOpenAllAccounts()
                                    },
                                    onOpenAddAccount = {
                                        triggerHaptic()
                                        onOpenAddAccount()
                                    }
                                )
                            }
                        }
                    }

                    HomeSectionId.BUDGET -> {
                        MonthlyBudgetCard(
                            spent = state.spentThisMonth,
                            budget = state.prefs.monthlyBudget,
                            topCategories = state.topCategories,
                            language = lang,
                            strings = strings,
                            currencyCode = mainCurCode,
                            onEditBudget = onOpenProfileEdit
                        )
                    }

                    HomeSectionId.STREAK -> {
                        SpendingStreakCard(
                            streakDays = state.streakDays,
                            todaySpend = state.todaySpend,
                            dailyAvg = state.dailyAverageSpend,
                            language = lang,
                            strings = strings,
                            currencyCode = mainCurCode
                        )
                    }

                    HomeSectionId.RECENT -> {
                        CrystalCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = strings.recentTransactions,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                TextButton(onClick = onSeeAllActivity) {
                                    Text(strings.seeAll)
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            val recent = state.transactions.take(4)
                            if (recent.isEmpty()) {
                                Text(
                                    text = strings.noTransactionsSubtitle,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = crystal.mutedText,
                                    modifier = Modifier.padding(vertical = 12.dp)
                                )
                            } else {
                                val accountsById = remember(state.accounts) {
                                    state.accounts.associateBy { it.id }
                                }
                                recent.forEachIndexed { index, tx ->
                                    val acc = accountsById[tx.accountId]
                                    TransactionRowItem(
                                        tx = tx,
                                        accountName = acc?.let { AccountNameResolver.resolveAccountName(it, lang) }.orEmpty(),
                                        language = lang,
                                        accountsById = accountsById,
                                        marketSnapshot = marketSyncState.snapshot
                                    )
                                    if (index < recent.lastIndex) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                    }
                                }
                            }
                        }
                    }

                    HomeSectionId.UPCOMING_BILLS -> {
                        val pendingLoans = state.loans.filter { !it.isSettled }.take(3)
                        CrystalCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("home_section_UPCOMING_BILLS"),
                            onClick = {
                                triggerHaptic()
                                onOpenPlan()
                            }
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
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(crystal.warningAmberBg),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.EventNote,
                                            contentDescription = null,
                                            tint = crystal.warningAmber,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = HomeSectionId.UPCOMING_BILLS.label(lang),
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = strings.dashboard.upcomingBillsSubtitle,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = crystal.mutedText
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = crystal.mutedText
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            if (pendingLoans.isEmpty()) {
                                Text(
                                    text = strings.dashboard.noUpcomingBillsMsg,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = crystal.mutedText
                                )
                            } else {
                                pendingLoans.forEachIndexed { idx, loan ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = loan.personName,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.weight(1f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = BdtFormatter.formatCurrency(loan.remainingAmount, loan.currencyCode, lang),
                                            style = MaterialTheme.typography.labelLarge.copy(
                                                fontFamily = JetBrainsMonoFamily,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = if (loan.loanType == LoanType.I_OWE) crystal.liabilityRed else crystal.assetGreen
                                        )
                                    }
                                    if (idx < pendingLoans.lastIndex) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                    }
                                }
                            }
                        }
                    }

                    HomeSectionId.GOALS -> {
                        val activeGoals = state.goals.take(2)
                        CrystalCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("home_section_GOALS"),
                            onClick = {
                                triggerHaptic()
                                onOpenPlan()
                            }
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
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Savings,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Text(
                                        text = HomeSectionId.GOALS.label(lang),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = crystal.mutedText
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            if (activeGoals.isEmpty()) {
                                Text(
                                    text = strings.dashboard.goalsEmptyMsg,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = crystal.mutedText
                                )
                            } else {
                                activeGoals.forEachIndexed { idx, goal ->
                                    val progress = if (goal.targetAmount > 0.0) {
                                        (goal.savedAmount / goal.targetAmount).toFloat().coerceIn(0f, 1f)
                                    } else 0f
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = goal.name,
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Text(
                                                text = "${BdtFormatter.format(goal.savedAmount, lang)} / ${BdtFormatter.format(goal.targetAmount, lang)}",
                                                style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFamily),
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(7.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f))
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth(progress)
                                                    .height(7.dp)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.primary)
                                            )
                                        }
                                    }
                                    if (idx < activeGoals.lastIndex) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                    }
                                }
                            }
                        }
                    }

                    HomeSectionId.FINANCIAL_HEALTH -> {
                        val isHealthy = state.incomeThisMonth >= state.spentThisMonth || state.netWorth.netWorth >= 0.0
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CrystalCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("home_section_FINANCIAL_HEALTH")
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
                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(if (isHealthy) crystal.assetGreenBg else crystal.warningAmberBg),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Favorite,
                                                contentDescription = null,
                                                tint = if (isHealthy) crystal.assetGreen else crystal.warningAmber,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Column {
                                            Text(
                                                text = HomeSectionId.FINANCIAL_HEALTH.label(lang),
                                                style = MaterialTheme.typography.titleMedium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = strings.dashboard.savingsRateLabel,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = crystal.mutedText
                                            )
                                        }
                                    }
                                    Surface(
                                        color = if (isHealthy) crystal.assetGreenBg else crystal.warningAmberBg,
                                        shape = CircleShape
                                    ) {
                                        Text(
                                            text = if (isHealthy) strings.dashboard.financialHealthHealthy else strings.dashboard.financialHealthWatch,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = if (isHealthy) crystal.assetGreen else crystal.warningAmber,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            text = strings.netWorth,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = crystal.mutedText
                                        )
                                        CurrencyAmountText(
                                            amount = state.netWorth.netWorth,
                                            currencyCode = mainCurCode,
                                            language = lang,
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontFamily = NumericDisplayFontFamily,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            isBalance = true,
                                            bdtFontScale = 0.90f,
                                            minFontSize = 12.sp
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = strings.dashboard.incomePillLabel,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = crystal.mutedText
                                        )
                                        Text(
                                            text = BdtFormatter.formatCurrency(state.incomeThisMonth, mainCurCode, lang),
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontFamily = JetBrainsMonoFamily,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = crystal.assetGreen
                                        )
                                    }
                                }
                            }

                            // Prominent Highlighted Live Market Entry Card directly below Financial Health
                            HomeLiveMarketEntryCard(
                                language = lang,
                                marketSyncState = marketSyncState,
                                strings = strings,
                                onClick = {
                                    triggerHaptic()
                                    onOpenLiveMarket()
                                }
                            )
                        }
                    }
                }
            }

            if (HomeSectionId.FINANCIAL_HEALTH !in visibleSections) {
                item(key = "home_live_market_fallback") {
                    HomeLiveMarketEntryCard(
                        language = lang,
                        marketSyncState = marketSyncState,
                        strings = strings,
                        onClick = {
                            triggerHaptic()
                            onOpenLiveMarket()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeTopHeader(
    userName: String,
    avatarUrl: String,
    authMode: AuthMode,
    greeting: String,
    language: AppLanguage,
    hasUnreadAlert: Boolean,
    strings: AppStrings,
    onOpenSearch: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val crystal = LocalCrystalColors.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Clean Profile Avatar (opens Settings / Profile) + Display-Only Greeting & Name
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onOpenSettings)
                    .testTag("profile_settings_button"),
                contentAlignment = Alignment.Center
            ) {
                UserProfileAvatar(
                    userName = userName,
                    avatarUrl = avatarUrl,
                    authMode = authMode,
                    size = 46.dp,
                    contentDescription = stringsAvatarDesc(language)
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text(
                    text = greeting,
                    style = MaterialTheme.typography.bodySmall,
                    color = crystal.mutedText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = userName,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("home_header_user_name")
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            IconButton(
                onClick = onOpenSearch,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("global_search_button")
            ) {
                Surface(
                    shape = CircleShape,
                    color = crystal.glassSurfaceElevated,
                    border = BorderStroke(1.dp, crystal.glassBorder),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = strings.globalSearchTitle,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Box {
                IconButton(
                    onClick = onOpenNotifications,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("notifications_button")
                ) {
                    Surface(
                        shape = CircleShape,
                        color = crystal.glassSurfaceElevated,
                        border = BorderStroke(1.dp, crystal.glassBorder),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.Notifications,
                                contentDescription = strings.notificationsTitle,
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
                if (hasUnreadAlert) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 9.dp, end = 9.dp)
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(crystal.liabilityRed)
                            .border(1.5.dp, MaterialTheme.colorScheme.background, CircleShape)
                    )
                }
            }
        }
    }
}

private fun stringsAvatarDesc(language: AppLanguage): String =
    if (language == AppLanguage.BN) "প্রোফাইল ও সেটিংস খুলুন" else "Open Profile & Settings"

@Composable
private fun QuickShortcutTile(
    label: String,
    icon: ImageVector,
    tint: Color,
    testTag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val crystal = LocalCrystalColors.current
    Column(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(18.dp))
            .background(crystal.glassSurface)
            .border(1.dp, crystal.glassBorder, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp)
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(tint.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(22.dp)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

private data class CurrencyBalanceEntry(
    val currency: AppCurrency,
    val amount: Double
)

private fun resolveHomeCurrencyBalances(
    accounts: List<AccountEntity>,
    liquidBalancesByCurrency: Map<String, Double>,
    fallbackTotalLiquidBalance: Double,
    mainCurrencyCode: String,
    secondaryCurrency: AppCurrency?
): List<CurrencyBalanceEntry> {
    val mainCur = AppCurrency.fromCode(mainCurrencyCode)
    val totalsMap: Map<String, Double> = when {
        liquidBalancesByCurrency.isNotEmpty() -> liquidBalancesByCurrency
        accounts.isNotEmpty() -> accounts
            .filter { it.type != AccountType.CREDIT_CARD }
            .groupBy { AppCurrency.fromCode(it.currencyCode).code }
            .mapValues { (_, list) -> list.sumOf { it.balance.coerceAtLeast(0.0) } }
        else -> emptyMap()
    }

    val hasAnyAccounts = accounts.isNotEmpty() || liquidBalancesByCurrency.isNotEmpty()
    val mainAmount = if (hasAnyAccounts) {
        totalsMap[mainCur.code] ?: 0.0
    } else {
        fallbackTotalLiquidBalance
    }

    val result = mutableListOf(CurrencyBalanceEntry(mainCur, mainAmount))
    val seen = mutableSetOf(mainCur.code)

    if (secondaryCurrency != null && seen.add(secondaryCurrency.code)) {
        result += CurrencyBalanceEntry(
            currency = secondaryCurrency,
            amount = totalsMap[secondaryCurrency.code] ?: 0.0
        )
    }

    totalsMap.entries
        .filter { it.key !in seen }
        .sortedBy { it.key }
        .forEach { (code, sum) ->
            val cur = AppCurrency.fromCode(code)
            if (seen.add(cur.code)) {
                result += CurrencyBalanceEntry(cur, sum)
            }
        }

    return result
}

private fun formatBalanceForBadge(
    amount: Double,
    currency: AppCurrency,
    language: AppLanguage
): String {
    return BdtFormatter.formatSymbolAndBalance(amount, currency.code, language)
}

@Composable
private fun CleanCurrencyBadge(
    label: String,
    primaryColor: Color,
    compact: Boolean = false
) {
    val crystal = LocalCrystalColors.current
    val badgeBg = primaryColor.copy(alpha = if (crystal.isDark) 0.24f else 0.13f)
    val badgeBorder = primaryColor.copy(alpha = if (crystal.isDark) 0.50f else 0.34f)
    val badgeTextColor = if (crystal.isDark) Color(0xFF7DD3FC) else primaryColor

    Surface(
        color = badgeBg,
        shape = RoundedCornerShape(if (compact) 6.dp else 8.dp),
        border = BorderStroke(0.9.dp, badgeBorder)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = NumericDisplayFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = if (compact) 10.sp else 11.sp,
                letterSpacing = 0.5.sp
            ),
            color = badgeTextColor,
            maxLines = 1,
            modifier = Modifier.padding(
                horizontal = if (compact) 6.dp else 8.dp,
                vertical = if (compact) 2.dp else 4.dp
            )
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HeroBalanceSummaryCard(
    currencyBalances: List<CurrencyBalanceEntry>,
    incomeThisMonth: Double,
    spentThisMonth: Double,
    monthlyBudget: Double,
    netWorth: Double,
    dailyAverage: Double,
    language: AppLanguage,
    strings: AppStrings,
    mainCurrencyCode: String = "BDT",
    onOpenAllAccounts: () -> Unit,
    onOpenActivity: () -> Unit
) {
    val crystal = LocalCrystalColors.current
    val budgetRatio = if (monthlyBudget > 0.0) (spentThisMonth / monthlyBudget).toFloat().coerceIn(0f, 1f) else 0f
    val budgetPercent = if (monthlyBudget > 0.0) ((spentThisMonth / monthlyBudget) * 100.0).toInt().coerceAtLeast(0) else 0
    val isOverBudget = monthlyBudget > 0.0 && spentThisMonth > monthlyBudget
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val heroShape = RoundedCornerShape(24.dp)
    val heroGradient = if (crystal.isDark) {
        listOf(
            lerp(Color(0xFF131A2C), primaryColor, 0.22f),
            Color(0xFF121828),
            lerp(Color(0xFF0E131E), secondaryColor, 0.14f)
        )
    } else {
        listOf(
            lerp(Color.White, primaryColor, 0.10f).copy(alpha = 0.94f),
            Color.White.copy(alpha = 0.92f),
            lerp(Color.White, secondaryColor, 0.14f).copy(alpha = 0.90f)
        )
    }
    val heroBorderBrush = Brush.linearGradient(
        colors = if (crystal.isDark) {
            listOf(
                Color.White.copy(alpha = 0.24f),
                primaryColor.copy(alpha = 0.34f)
            )
        } else {
            listOf(
                Color.White.copy(alpha = 0.98f),
                primaryColor.copy(alpha = 0.30f)
            )
        }
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (crystal.isDark) 16.dp else 12.dp,
                shape = heroShape,
                ambientColor = if (crystal.isDark) primaryColor.copy(alpha = 0.25f) else primaryColor.copy(alpha = 0.20f),
                spotColor = if (crystal.isDark) Color.Black.copy(alpha = 0.72f) else primaryColor.copy(alpha = 0.24f)
            )
            .clip(heroShape)
            .background(
                brush = Brush.linearGradient(
                    colors = heroGradient,
                    start = Offset(0f, 0f),
                    end = Offset(900f, 620f)
                )
            )
            .border(width = 1.2.dp, brush = heroBorderBrush, shape = heroShape)
            .clickable(onClick = onOpenAllAccounts)
            .testTag("hero_balance_card")
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = if (crystal.isDark) 0.18f else 0.11f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.92f, size.height * 0.08f),
                    radius = size.width * 0.52f
                ),
                center = Offset(size.width * 0.92f, size.height * 0.08f),
                radius = size.width * 0.52f
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        secondaryColor.copy(alpha = if (crystal.isDark) 0.12f else 0.09f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.10f, size.height * 0.92f),
                    radius = size.width * 0.44f
                ),
                center = Offset(size.width * 0.10f, size.height * 0.92f),
                radius = size.width * 0.44f
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(11.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        primaryColor.copy(alpha = if (crystal.isDark) 0.28f else 0.18f),
                                        secondaryColor.copy(alpha = if (crystal.isDark) 0.22f else 0.14f)
                                    )
                                )
                            )
                            .border(
                                width = 0.8.dp,
                                color = primaryColor.copy(alpha = 0.35f),
                                shape = RoundedCornerShape(11.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AccountBalanceWallet,
                            contentDescription = null,
                            tint = primaryColor,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = strings.totalBalance,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Normal),
                            color = crystal.mutedText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = strings.liquidAssetsHint,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Normal),
                            color = crystal.mutedText.copy(alpha = 0.82f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Surface(
                    color = primaryColor.copy(alpha = if (crystal.isDark) 0.22f else 0.13f),
                    shape = CircleShape,
                    border = BorderStroke(0.9.dp, primaryColor.copy(alpha = 0.38f))
                ) {
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(fontWeight = FontWeight.Normal)) {
                                append("${strings.netWorth}: ")
                            }
                            withStyle(
                                SpanStyle(
                                    fontFamily = NumericDisplayFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFeatureSettings = "tnum",
                                    letterSpacing = (-0.15).sp
                                )
                            ) {
                                append(BdtFormatter.formatBalance(netWorth, mainCurrencyCode, language))
                            }
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = primaryColor,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            val primaryEntry = currencyBalances.firstOrNull()
                ?: CurrencyBalanceEntry(AppCurrency.fromCode(mainCurrencyCode), 0.0)
            val extraEntries = currencyBalances.drop(1)

            // Primary Currency Actual Balance Row (Boldest hero typography in dark tone)
            val primaryLabel = primaryEntry.currency.cleanBadgeLabel()
            val primaryBadgeColor = if (primaryEntry.currency == AppCurrency.KWD) {
                crystal.kwdNumericInk
            } else {
                primaryColor
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics(mergeDescendants = true) {
                        contentDescription = BdtFormatter.formatBalance(
                            primaryEntry.amount,
                            primaryEntry.currency.code,
                            language
                        )
                    }
                    .testTag("hero_balance_currency_${primaryEntry.currency.code}"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CleanCurrencyBadge(
                    label = primaryLabel,
                    primaryColor = primaryBadgeColor
                )
                CurrencyAmountText(
                    amount = primaryEntry.amount,
                    currencyCode = primaryEntry.currency.code,
                    language = language,
                    style = NumericHeroBalanceStyle.copy(
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold
                    ),
                    includeIsoCode = false,
                    isBalance = true,
                    colorOverride = if (primaryEntry.currency == AppCurrency.KWD) {
                        crystal.kwdNumericInk
                    } else {
                        crystal.bdtNumericInk
                    },
                    bdtFontScale = 0.85f,
                    minFontSize = 14.sp,
                    modifier = Modifier.weight(1f)
                )
            }

            // Secondary / Additional Active Currencies (All 2..N currencies cleanly wrapped with zero clipping)
            if (extraEntries.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("hero_secondary_currency_balances"),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    extraEntries.forEachIndexed { idx, entry ->
                        val entryLabel = entry.currency.cleanBadgeLabel()
                        val useDarkBlue = idx == 0 || entry.currency == AppCurrency.KWD
                        val entryBadgeColor = if (useDarkBlue) {
                            crystal.kwdNumericInk
                        } else {
                            primaryColor
                        }
                        Row(
                            modifier = Modifier
                                .semantics(mergeDescendants = true) {
                                    contentDescription = BdtFormatter.formatBalance(
                                        entry.amount,
                                        entry.currency.code,
                                        language
                                    )
                                }
                                .testTag("hero_balance_currency_${entry.currency.code}"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            CleanCurrencyBadge(
                                label = entryLabel,
                                primaryColor = entryBadgeColor,
                                compact = true
                            )
                            CurrencyAmountText(
                                amount = entry.amount,
                                currencyCode = entry.currency.code,
                                language = language,
                                style = NumericSummarySecondaryStyle.copy(
                                    fontSize = 15.5.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                includeIsoCode = false,
                                isBalance = true,
                                colorOverride = if (useDarkBlue) {
                                    crystal.kwdNumericInk
                                } else {
                                    crystal.bdtNumericInk
                                },
                                bdtFontScale = 0.88f,
                                minFontSize = 11.5.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    color = crystal.assetGreenBg,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(0.8.dp, crystal.assetGreen.copy(alpha = 0.25f)),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable(onClick = onOpenActivity)
                        .testTag("hero_income_box")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(crystal.assetGreen.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.TrendingUp,
                                contentDescription = null,
                                tint = crystal.assetGreen,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.dashboard.incomePillLabel,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Normal),
                                color = crystal.mutedText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            CurrencyAmountText(
                                amount = incomeThisMonth,
                                currencyCode = mainCurrencyCode,
                                language = language,
                                style = NumericInlineMetricStyle.copy(
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                isBalance = true,
                                colorOverride = crystal.assetGreen,
                                bdtFontScale = 0.86f,
                                minFontSize = 10.sp,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                Surface(
                    color = crystal.liabilityRedBg,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(0.8.dp, crystal.liabilityRed.copy(alpha = 0.25f)),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable(onClick = onOpenActivity)
                        .testTag("hero_spent_box")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(crystal.liabilityRed.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.TrendingDown,
                                contentDescription = null,
                                tint = crystal.liabilityRed,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.spentThisMonth,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Normal),
                                color = crystal.mutedText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            CurrencyAmountText(
                                amount = spentThisMonth,
                                currencyCode = mainCurrencyCode,
                                language = language,
                                style = NumericInlineMetricStyle.copy(
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                isBalance = true,
                                colorOverride = if (mainCurrencyCode.equals("BDT", ignoreCase = true)) {
                                    crystal.bdtNumericInk
                                } else if (mainCurrencyCode.equals("KWD", ignoreCase = true)) {
                                    crystal.kwdNumericInk
                                } else {
                                    crystal.liabilityRed
                                },
                                bdtFontScale = 0.86f,
                                minFontSize = 10.sp,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            if (monthlyBudget > 0.0) {
                Spacer(modifier = Modifier.height(9.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("hero_balance_budget_overview"),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${strings.monthlyBudget}: ${BdtFormatter.formatCurrency(monthlyBudget, mainCurrencyCode, language)}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Normal),
                            color = crystal.mutedText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "${BdtFormatter.formatNumber(budgetPercent, language)}%",
                            style = NumericInlineMetricStyle.copy(
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            color = if (isOverBudget) crystal.liabilityRed else primaryColor
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape)
                            .background(
                                if (crystal.isDark) Color.White.copy(alpha = 0.12f)
                                else primaryColor.copy(alpha = 0.14f)
                            )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(budgetRatio)
                                .fillMaxHeight()
                                .clip(CircleShape)
                                .background(
                                    if (isOverBudget) {
                                        Brush.horizontalGradient(listOf(crystal.warningAmber, crystal.liabilityRed))
                                    } else {
                                        Brush.horizontalGradient(listOf(secondaryColor, primaryColor))
                                    }
                                )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = buildAnnotatedString {
                    withStyle(
                        SpanStyle(
                            color = crystal.mutedText,
                            fontWeight = FontWeight.Normal
                        )
                    ) {
                        append("${strings.dailyAverageLabel}: ")
                    }
                    withStyle(
                        SpanStyle(
                            fontFamily = NumericDisplayFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontFeatureSettings = "tnum",
                            letterSpacing = (-0.15).sp,
                            color = crystal.mutedText.copy(alpha = 0.92f)
                        )
                    ) {
                        append(BdtFormatter.formatBalance(dailyAverage, mainCurrencyCode, language))
                    }
                },
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.testTag("hero_daily_avg_text")
            )
        }
    }
}

@Composable
private fun EmptyWalletPlaceholderCard(
    language: AppLanguage,
    strings: AppStrings,
    onAddAccount: () -> Unit
) {
    val crystal = LocalCrystalColors.current
    val primary = MaterialTheme.colorScheme.primary
    val title = if (language == AppLanguage.BN) {
        "কোনো ওয়ালেট বা ব্যাংক কার্ড নেই"
    } else {
        "No Wallets or Bank Cards Yet"
    }
    val subtitle = if (language == AppLanguage.BN) {
        "নগদ, ব্যাংক অ্যাকাউন্ট, কার্ড বা বিকাশ/নগদ যোগ করে আপনার হিসাব শুরু করুন"
    } else {
        "Add Cash, Bank, Card, or bKash/Nagad to start tracking your real balances"
    }
    val blankHint = if (language == AppLanguage.BN) "ব্যালেন্স" else "Balance"
    val cardShape = RoundedCornerShape(24.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(
                if (crystal.isDark) {
                    Brush.linearGradient(
                        listOf(
                            Color(0xFF102033).copy(alpha = 0.88f),
                            Color(0xFF142A40).copy(alpha = 0.84f)
                        )
                    )
                } else {
                    Brush.linearGradient(
                        listOf(
                            Color(0xFFF3F8FF).copy(alpha = 0.94f),
                            Color(0xFFFFFFFF).copy(alpha = 0.90f)
                        )
                    )
                }
            )
            .border(
                width = 1.2.dp,
                brush = Brush.linearGradient(
                    listOf(
                        primary.copy(alpha = if (crystal.isDark) 0.45f else 0.36f),
                        crystal.glassBorder
                    )
                ),
                shape = cardShape
            )
            .clickable(onClick = onAddAccount)
            .testTag("empty_wallet_placeholder_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(primary.copy(alpha = if (crystal.isDark) 0.22f else 0.12f))
                            .border(1.dp, primary.copy(alpha = 0.34f), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AccountBalanceWallet,
                            contentDescription = null,
                            tint = primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "$blankHint: ${BdtFormatter.format(0.0, language)}",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontFamily = JetBrainsMonoFamily,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = crystal.mutedText
                        )
                    }
                }
            }

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = crystal.mutedText
            )

            Surface(
                onClick = onAddAccount,
                shape = CircleShape,
                color = primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("empty_wallet_add_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = strings.addAccount,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun CompactWalletStrip(
    accounts: List<AccountEntity>,
    language: AppLanguage,
    strings: AppStrings,
    onOpenAllAccounts: () -> Unit,
    onOpenAddAccount: () -> Unit = onOpenAllAccounts
) {
    if (accounts.isEmpty()) {
        EmptyWalletPlaceholderCard(
            language = language,
            strings = strings,
            onAddAccount = onOpenAddAccount
        )
        return
    }

    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth()
    ) {
        val cardWidth = if (accounts.size > 1) {
            (maxWidth * 0.84f).coerceIn(252.dp, 360.dp)
        } else {
            maxWidth
        }

        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("compact_wallet_strip"),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(accounts, key = { it.id }) { account ->
                if (account.type == AccountType.CASH) {
                    CashWalletCompactCard(
                        account = account,
                        cardWidth = cardWidth,
                        language = language,
                        strings = strings,
                        onClick = onOpenAllAccounts
                    )
                    return@items
                }
                val gradientColors = resolveAccountCardGradient(account)
                val shadowTint = gradientColors.first()
                val cardShape = RoundedCornerShape(22.dp)
                val cardTypeLabel = when (account.type) {
                    AccountType.DEBIT_BANK -> strings.dashboard.cardTypeDebit
                    AccountType.CREDIT_CARD -> strings.dashboard.cardTypeCredit
                    AccountType.MFS -> strings.dashboard.cardTypeMfs
                    AccountType.CASH -> strings.dashboard.cardTypeCash
                }
                val subLabel = if (
                    account.bankOrProvider.isNotBlank() &&
                    !account.bankOrProvider.equals(account.name, ignoreCase = true) &&
                    account.type != AccountType.CASH
                ) {
                    "$cardTypeLabel • ${account.bankOrProvider}"
                } else {
                    cardTypeLabel
                }
                val last4 = account.accountNumberLast4.takeLast(4).ifBlank { "1947" }
                val showMaskedDigits = account.type != AccountType.CASH

                Box(
                    modifier = Modifier
                        .width(cardWidth)
                        .height(166.dp)
                        .shadow(
                            elevation = 12.dp,
                            shape = cardShape,
                            ambientColor = shadowTint.copy(alpha = 0.40f),
                            spotColor = shadowTint.copy(alpha = 0.46f)
                        )
                        .clip(cardShape)
                        .background(
                            brush = Brush.linearGradient(
                                colors = gradientColors,
                                start = Offset(0f, 0f),
                                end = Offset(860f, 520f)
                            )
                        )
                        .border(
                            width = 1.2.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.65f),
                                    Color.White.copy(alpha = 0.16f)
                                )
                            ),
                            shape = cardShape
                        )
                        .clickable(onClick = onOpenAllAccounts)
                        .testTag("compact_wallet_card_${account.id}")
                ) {
                    // Brand card metallic/frosted-glass geometric arcs
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(
                            color = Color.White.copy(alpha = 0.14f),
                            radius = size.height * 0.85f,
                            center = Offset(size.width * 0.90f, size.height * 0.08f)
                        )
                        drawCircle(
                            color = Color.White.copy(alpha = 0.08f),
                            radius = size.height * 0.56f,
                            center = Offset(size.width * 0.72f, size.height * 0.96f)
                        )
                        drawCircle(
                            color = Color.Black.copy(alpha = 0.15f),
                            radius = size.height * 0.52f,
                            center = Offset(size.width * 0.04f, size.height * 0.94f)
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp, vertical = 18.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    color = Color.White.copy(alpha = 0.22f),
                                    shape = RoundedCornerShape(11.dp),
                                    border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.38f))
                                ) {
                                    Icon(
                                        imageVector = when (account.type) {
                                            AccountType.DEBIT_BANK -> Icons.Filled.AccountBalance
                                            AccountType.CREDIT_CARD -> Icons.Filled.CreditCard
                                            AccountType.MFS -> Icons.Filled.PhoneAndroid
                                            AccountType.CASH -> Icons.Filled.Payments
                                        },
                                        contentDescription = cardTypeLabel,
                                        tint = Color.White,
                                        modifier = Modifier
                                            .padding(8.dp)
                                            .size(18.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = account.name.ifBlank { account.bankOrProvider },
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = subLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.86f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            val accCur = AppCurrency.fromCode(account.currencyCode)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    color = Color.Black.copy(alpha = 0.24f),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(0.7.dp, Color.White.copy(alpha = 0.34f))
                                ) {
                                    Text(
                                        text = accCur.shortBadge(),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp
                                        ),
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                }
                                if (account.type != AccountType.CASH) {
                                    CardNetworkOrProviderBadge(account = account)
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (account.type == AccountType.CREDIT_CARD) {
                                        strings.outstandingBalanceLabel
                                    } else {
                                        strings.availableBalance
                                    },
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Medium
                                    ),
                                    color = Color.White.copy(alpha = 0.78f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                CurrencyAmountText(
                                    amount = account.balance,
                                    currencyCode = account.currencyCode,
                                    language = language,
                                    style = NumericCompactCardBalanceStyle,
                                    isBalance = true,
                                    colorOverride = Color.White,
                                    bdtFontScale = 0.86f,
                                    minFontSize = 14.sp,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            if (showMaskedDigits) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    color = Color.Black.copy(alpha = 0.24f),
                                    shape = CircleShape,
                                    border = BorderStroke(0.7.dp, Color.White.copy(alpha = 0.28f))
                                ) {
                                    Text(
                                        text = "•••• $last4",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = JetBrainsMonoFamily,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 11.sp
                                        ),
                                        color = Color.White.copy(alpha = 0.94f),
                                        maxLines = 1,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StackedWalletCarousel(
    accounts: List<AccountEntity>,
    language: AppLanguage,
    strings: AppStrings,
    onCardSwitched: () -> Unit = {},
    onOpenAllAccounts: () -> Unit,
    onOpenAddAccount: () -> Unit = onOpenAllAccounts
) {
    val crystal = LocalCrystalColors.current
    if (accounts.isEmpty()) {
        EmptyWalletPlaceholderCard(
            language = language,
            strings = strings,
            onAddAccount = onOpenAddAccount
        )
        return
    }

    var frontIndex by remember(accounts.size) { mutableIntStateOf(0) }
    val safeFrontIndex = frontIndex.coerceIn(0, accounts.lastIndex)
    val visibleCount = minOf(3, accounts.size)
    val extraCount = (accounts.size - 1).coerceAtLeast(0)

    // Order visible stack items back-to-front so draw & z-ordering matches physical card layering
    val stackedItems = remember(accounts, safeFrontIndex, visibleCount) {
        (visibleCount - 1 downTo 0).map { depth ->
            val accountIdx = (safeFrontIndex + depth) % accounts.size
            Triple(depth, accountIdx, accounts[accountIdx])
        }
    }

    // Unified elevated Crystal Liquid frosted-glass container wrapping header + card stack
    CrystalCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("wallet_stack_container"),
        cornerRadius = 26.dp,
        elevated = true
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("wallet_stack_carousel"),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Container Header: "+X more" on top-left & "All accounts >" on top-right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (extraCount > 0) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(
                            alpha = if (crystal.isDark) 0.22f else 0.13f
                        ),
                        shape = CircleShape,
                        border = BorderStroke(
                            width = 0.8.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.34f)
                        ),
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable(onClick = onOpenAllAccounts)
                            .testTag("more_wallets_badge")
                    ) {
                        Text(
                            text = "+${BdtFormatter.formatNumber(extraCount, language)} ${strings.moreSuffix}",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                } else {
                    Text(
                        text = strings.myWallets,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                TextButton(
                    onClick = onOpenAllAccounts,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("all_accounts_button")
                ) {
                    Text(
                        text = strings.allAccounts,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Centered Card Stack with realistic compact debit/credit card aspect ratio
            BoxWithConstraints(
                modifier = Modifier.fillMaxWidth()
            ) {
                val cardHeight = (maxWidth / 1.78f).coerceIn(168.dp, 196.dp)
                val peekStep = 50.dp
                val containerHeight = cardHeight + (peekStep * (visibleCount - 1))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(containerHeight),
                    contentAlignment = Alignment.TopCenter
                ) {
                    stackedItems.forEach { (depth, accountIdx, account) ->
                        key(account.id) {
                            val slotFromTop = (visibleCount - 1) - depth
                            val scale by animateFloatAsState(
                                targetValue = 1f - (depth * 0.042f),
                                animationSpec = spring(
                                    dampingRatio = 0.84f,
                                    stiffness = Spring.StiffnessMediumLow
                                ),
                                label = "cardStackScale_${account.id}"
                            )
                            val yOffset by animateDpAsState(
                                targetValue = peekStep * slotFromTop,
                                animationSpec = spring(
                                    dampingRatio = 0.84f,
                                    stiffness = Spring.StiffnessMediumLow
                                ),
                                label = "cardStackOffset_${account.id}"
                            )
                            val alphaVal by animateFloatAsState(
                                targetValue = 1f - (depth * 0.06f),
                                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                label = "cardStackAlpha_${account.id}"
                            )
                            val layerElevation = when (depth) {
                                0 -> 20.dp
                                1 -> 11.dp
                                else -> 5.dp
                            }
                            val isPeeking = depth > 0

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .offset(y = yOffset)
                                    .scale(scale)
                                    .alpha(alphaVal)
                                    .zIndex((visibleCount - depth).toFloat())
                                    .testTag("wallet_stack_card_${account.id}")
                            ) {
                                AccountVisualCard(
                                    account = account,
                                    language = language,
                                    strings = strings,
                                    compact = true,
                                    isPeekingBackground = isPeeking,
                                    elevation = layerElevation,
                                    modifier = Modifier
                                        .height(cardHeight)
                                        .testTag(
                                            if (isPeeking) "wallet_stack_peek_$depth"
                                            else "wallet_stack_front_card"
                                        ),
                                    onClick = {
                                        if (isPeeking) {
                                            onCardSwitched()
                                            frontIndex = accountIdx
                                        } else {
                                            onOpenAllAccounts()
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            if (accounts.size > 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    accounts.forEachIndexed { idx, _ ->
                        val active = idx == safeFrontIndex
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 3.dp)
                                .width(if (active) 22.dp else 8.dp)
                                .height(8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (active) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
                                )
                                .clickable {
                                    onCardSwitched()
                                    frontIndex = idx
                                }
                                .testTag("wallet_dot_$idx")
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthlyBudgetCard(
    spent: Double,
    budget: Double,
    topCategories: List<com.example.data.model.CategorySpend>,
    language: AppLanguage,
    strings: AppStrings,
    currencyCode: String = "BDT",
    onEditBudget: () -> Unit
) {
    val crystal = LocalCrystalColors.current
    val safeBudget = budget.coerceAtLeast(1.0)
    val ratio = (spent / safeBudget).toFloat().coerceIn(0f, 1f)
    val animatedRatio by animateFloatAsState(
        targetValue = ratio,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "budgetProgress"
    )
    val pct = ((spent / safeBudget) * 100.0).toInt()
    val remaining = safeBudget - spent

    CrystalCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onEditBudget
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = strings.monthlyBudget,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Surface(
                color = if (pct >= 85) crystal.liabilityRedBg else crystal.assetGreenBg,
                shape = CircleShape
            ) {
                Text(
                    text = BdtFormatter.formatPercent(pct, language),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (pct >= 85) crystal.liabilityRed else crystal.assetGreen,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${strings.spentLabel}: ${BdtFormatter.formatCurrency(spent, currencyCode, language)}",
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = JetBrainsMonoFamily),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${strings.budgetLabel}: ${BdtFormatter.formatCurrency(safeBudget, currencyCode, language)}",
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = JetBrainsMonoFamily),
                color = crystal.mutedText
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedRatio)
                    .height(12.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.horizontalGradient(
                            if (pct >= 90) listOf(crystal.warningAmber, crystal.liabilityRed)
                            else listOf(MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.primary, crystal.assetGreen)
                        )
                    )
            )
        }

        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = if (remaining >= 0) {
                "${strings.remainingLabel}: ${BdtFormatter.formatCurrency(remaining, currencyCode, language)}"
            } else {
                "${strings.overBudgetLabel}: ${BdtFormatter.formatCurrency(-remaining, currencyCode, language)}"
            },
            style = MaterialTheme.typography.bodySmall,
            color = if (remaining >= 0) crystal.assetGreen else crystal.liabilityRed
        )

        if (topCategories.isNotEmpty()) {
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = strings.topCategories,
                style = MaterialTheme.typography.labelMedium,
                color = crystal.mutedText
            )
            Spacer(modifier = Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                topCategories.forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CategoryIconBadge(category = item.category, size = 34.dp)
                            Text(
                                text = item.category.label(language),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = BdtFormatter.formatCurrency(item.amount, currencyCode, language),
                            style = MaterialTheme.typography.labelLarge.copy(fontFamily = JetBrainsMonoFamily),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SpendingStreakCard(
    streakDays: Int,
    todaySpend: Double,
    dailyAvg: Double,
    language: AppLanguage,
    strings: AppStrings,
    currencyCode: String = "BDT"
) {
    val crystal = LocalCrystalColors.current
    val dayNumber = BdtFormatter.formatNumber(streakDays, language)
    val ratio = if (dailyAvg > 0) (todaySpend / dailyAvg).toFloat().coerceIn(0f, 1f) else 0f

    CrystalCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFFF59E0B), Color(0xFFF97316))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.LocalFireDepartment,
                        contentDescription = strings.streakTitle,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Column {
                    Text(
                        text = strings.streakTitle,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = strings.streakSubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = crystal.mutedText,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                color = crystal.warningAmberBg,
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = dayNumber,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = crystal.warningAmber
                    )
                    Text(
                        text = strings.streakDaysSuffix,
                        style = MaterialTheme.typography.labelSmall,
                        color = crystal.warningAmber
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = strings.todaySpendVsAvg,
                style = MaterialTheme.typography.labelSmall,
                color = crystal.mutedText
            )
            Text(
                text = "${BdtFormatter.formatCurrency(todaySpend, currencyCode, language)} / ${BdtFormatter.formatCurrency(dailyAvg, currencyCode, language)}",
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape)
                .background(crystal.warningAmber.copy(alpha = 0.16f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(ratio)
                    .height(8.dp)
                    .clip(CircleShape)
                    .background(crystal.warningAmber)
            )
        }
    }
}

@Composable
fun TransactionRowItem(
    tx: TransactionEntity,
    accountName: String,
    language: AppLanguage,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    accountsById: Map<Long, AccountEntity> = emptyMap(),
    marketSnapshot: MarketSnapshot = MarketSnapshot.defaultReference()
) {
    val crystal = LocalCrystalColors.current
    val activeTzId = java.util.TimeZone.getDefault().id
    val dateFmt = remember(activeTzId) {
        SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).apply {
            timeZone = java.util.TimeZone.getTimeZone(activeTzId)
        }
    }
    val dateLabel = remember(tx.timestamp, activeTzId) { dateFmt.format(Date(tx.timestamp)) }

    val formattedAmt = BdtFormatter.formatCurrency(tx.amount, tx.currencyCode, language)
    val amountText = when (tx.type) {
        TransactionType.EXPENSE -> "-$formattedAmt"
        TransactionType.INCOME -> "+$formattedAmt"
        TransactionType.TRANSFER -> "⇄ $formattedAmt"
    }
    val amountColor = when (tx.type) {
        TransactionType.EXPENSE -> crystal.liabilityRed
        TransactionType.INCOME -> crystal.assetGreen
        TransactionType.TRANSFER -> MaterialTheme.colorScheme.primary
    }

    val isTransfer = tx.type == TransactionType.TRANSFER
    val displayTitle = remember(tx, accountsById, language) {
        if (isTransfer) {
            val (fromLabel, toLabel) = AccountNameResolver.resolveTransferEndpoints(tx, accountsById, language)
            "$fromLabel → $toLabel"
        } else {
            tx.title.ifBlank { tx.category.label(language) }
        }
    }
    val resolvedWalletLabel = remember(tx, accountName, accountsById, language) {
        when {
            isTransfer -> ""
            accountsById.containsKey(tx.accountId) ->
                AccountNameResolver.resolveAccountName(accountsById.getValue(tx.accountId), language)
            accountName.isNotBlank() -> accountName.trim()
            else -> AccountNameResolver.defaultWalletFallback(tx.currencyCode, language)
        }
    }
    val parsedTransfer = remember(tx, accountsById, marketSnapshot) {
        if (isTransfer) {
            TransferNoteCodec.parse(tx, tx.toAccountId?.let { accountsById[it] }, marketSnapshot)
        } else {
            null
        }
    }
    val conversionBadge = remember(parsedTransfer, language) {
        if (parsedTransfer != null && parsedTransfer.isCrossCurrency) {
            TransferNoteCodec.formatConversionSummary(parsedTransfer, language)
        } else {
            ""
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .let { m -> if (onClick != null) m.clickable(onClick = onClick) else m }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CategoryIconBadge(category = tx.category)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = displayTitle,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (conversionBadge.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = conversionBadge,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = crystal.assetGreen,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                val subPrefix = buildString {
                    if (resolvedWalletLabel.isNotBlank()) append(resolvedWalletLabel)
                    if (!isTransfer && tx.subcategory.isNotBlank()) {
                        if (isNotEmpty()) append(" • ")
                        append(tx.subcategory)
                    } else if (isTransfer) {
                        val transferTag = if (language == AppLanguage.BN) "ওয়ালেট ট্রান্সফার" else "Wallet Transfer"
                        append(transferTag)
                    }
                    if (isNotEmpty()) append(" • ")
                    append(dateLabel)
                }
                Text(
                    text = subPrefix,
                    style = MaterialTheme.typography.bodySmall,
                    color = crystal.mutedText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))
        val txPrefix = when (tx.type) {
            TransactionType.EXPENSE -> "-"
            TransactionType.INCOME -> "+"
            TransactionType.TRANSFER -> "⇄ "
        }
        val effectiveTxAmountColor = if (
            tx.currencyCode.equals("KWD", ignoreCase = true) && tx.type == TransactionType.TRANSFER
        ) {
            crystal.kwdNumericInk
        } else {
            amountColor
        }
        CurrencyAmountText(
            amount = tx.amount,
            currencyCode = tx.currencyCode,
            language = language,
            style = MaterialTheme.typography.titleSmall.copy(
                fontFamily = NumericDisplayFontFamily,
                fontWeight = FontWeight.SemiBold
            ),
            isBalance = false,
            prefix = txPrefix,
            colorOverride = effectiveTxAmountColor,
            bdtFontScale = 0.92f,
            minFontSize = 11.sp
        )
    }
}
