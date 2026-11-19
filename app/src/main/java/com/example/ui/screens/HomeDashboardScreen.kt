package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.R
import com.example.data.local.AccountEntity
import com.example.data.local.TransactionEntity
import com.example.data.model.AccountType
import com.example.data.model.AuthMode
import com.example.data.model.HomeLayoutStyle
import com.example.data.model.HomeSectionId
import com.example.data.model.LoanType
import com.example.data.model.TransactionType
import com.example.localization.AppLanguage
import com.example.localization.AppStrings
import com.example.localization.BdtFormatter
import com.example.ui.components.AccountVisualCard
import com.example.ui.components.CardNetworkOrProviderBadge
import com.example.ui.components.CategoryIconBadge
import com.example.ui.components.CrystalCard
import com.example.ui.components.GoogleGLogo
import com.example.ui.components.UserProfileAvatar
import com.example.ui.components.resolveAccountCardGradient
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalCrystalColors
import com.example.ui.theme.SkyCyanBright
import com.example.ui.theme.SkyCyanPrimary
import com.example.ui.viewmodel.MainUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    onOpenPlan: () -> Unit = {}
) {
    val crystal = LocalCrystalColors.current
    val lang = state.prefs.language
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
            // 1. Top Header (Clean Profile Avatar -> Settings, Inline Name Edit, Search & Notifications)
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
                    },
                    onUpdateName = { newName ->
                        triggerHaptic()
                        onUpdateName(newName)
                    },
                    onPickGuestAvatar = { uri ->
                        triggerHaptic()
                        onPickGuestAvatar(uri)
                    },
                    onClearGuestAvatar = {
                        triggerHaptic()
                        onClearGuestAvatar()
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
                        key(state.prefs.homeLayoutStyle) {
                            if (state.prefs.homeLayoutStyle == HomeLayoutStyle.BALANCE_CARD) {
                                HeroBalanceSummaryCard(
                                    totalBalance = state.totalLiquidBalance,
                                    incomeThisMonth = state.incomeThisMonth,
                                    spentThisMonth = state.spentThisMonth,
                                    monthlyBudget = state.prefs.monthlyBudget,
                                    netWorth = state.netWorth.netWorth,
                                    dailyAverage = state.dailyAverageSpend,
                                    language = lang,
                                    strings = strings,
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
                                    .testTag("summary_dual_cards"),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CrystalCard(
                                    modifier = Modifier.weight(1f),
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
                                            style = MaterialTheme.typography.labelMedium,
                                            color = crystal.mutedText,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = BdtFormatter.format(state.totalLiquidBalance, lang),
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontFamily = JetBrainsMonoFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 19.sp
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = strings.liquidAssetsHint,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = crystal.assetGreen,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                CrystalCard(
                                    modifier = Modifier.weight(1f),
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
                                            style = MaterialTheme.typography.labelMedium,
                                            color = crystal.mutedText,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = BdtFormatter.format(state.spentThisMonth, lang),
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontFamily = JetBrainsMonoFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 19.sp
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${strings.dailyAverageLabel}: ${BdtFormatter.format(state.dailyAverageSpend, lang)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = crystal.mutedText,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
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
                            onEditBudget = onOpenProfileEdit
                        )
                    }

                    HomeSectionId.STREAK -> {
                        SpendingStreakCard(
                            streakDays = state.streakDays,
                            todaySpend = state.todaySpend,
                            dailyAvg = state.dailyAverageSpend,
                            language = lang,
                            strings = strings
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
                                recent.forEachIndexed { index, tx ->
                                    TransactionRowItem(
                                        tx = tx,
                                        accountName = state.accounts.firstOrNull { it.id == tx.accountId }?.name ?: "",
                                        language = lang
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
                                            text = BdtFormatter.format(loan.remainingAmount, lang),
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
                                    Text(
                                        text = BdtFormatter.format(state.netWorth.netWorth, lang),
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontFamily = JetBrainsMonoFamily,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = strings.dashboard.incomePillLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = crystal.mutedText
                                    )
                                    Text(
                                        text = BdtFormatter.format(state.incomeThisMonth, lang),
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontFamily = JetBrainsMonoFamily,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = crystal.assetGreen
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
    onOpenSettings: () -> Unit,
    onUpdateName: (String) -> Unit,
    onPickGuestAvatar: (Uri) -> Unit,
    onClearGuestAvatar: () -> Unit
) {
    val crystal = LocalCrystalColors.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var isEditingName by remember { mutableStateOf(false) }
    var draftName by remember(userName) { mutableStateOf(userName) }
    var showNameError by remember { mutableStateOf(false) }

    val guestPhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onPickGuestAvatar(uri)
        }
    }

    fun commitInlineName() {
        val trimmed = draftName.trim()
        if (trimmed.isEmpty()) {
            showNameError = true
            return
        }
        onUpdateName(trimmed)
        showNameError = false
        isEditingName = false
        focusManager.clearFocus()
        keyboardController?.hide()
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Clean Profile Avatar (opens Settings) + Inline-Editable Name Area
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
                        .minimumInteractiveComponentSize()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            draftName = userName
                            showNameError = false
                            isEditingName = !isEditingName
                        }
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                        .testTag("profile_name_edit_trigger")
                ) {
                    Text(
                        text = greeting,
                        style = MaterialTheme.typography.bodySmall,
                        color = crystal.mutedText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            text = userName,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = strings.editNameDesc,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(14.dp)
                                .testTag("profile_name_edit_icon")
                        )
                    }
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

        // Smooth expandable inline name & dynamic profile photo editor
        AnimatedVisibility(
            visible = isEditingName,
            enter = fadeIn() + expandVertically(
                animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy)
            ),
            exit = fadeOut() + shrinkVertically()
        ) {
            CrystalCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .testTag("inline_profile_editor_card"),
                elevated = true
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = draftName,
                        onValueChange = {
                            if (it.length <= 40) {
                                draftName = it
                                showNameError = false
                            }
                        },
                        label = { Text(strings.yourNameLabel) },
                        isError = showNameError,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { commitInlineName() }),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = if (crystal.isDark) Color.White.copy(alpha = 0.24f) else crystal.glassBorder,
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor = crystal.mutedText,
                            cursorColor = MaterialTheme.colorScheme.primary,
                            focusedContainerColor = crystal.glassSurfaceElevated,
                            unfocusedContainerColor = crystal.glassSurfaceElevated
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("inline_name_input")
                    )

                    Surface(
                        onClick = { commitInlineName() },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("inline_name_save_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = strings.saveNameDesc,
                                tint = Color.White
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            draftName = userName
                            showNameError = false
                            isEditingName = false
                            focusManager.clearFocus()
                            keyboardController?.hide()
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("inline_name_cancel_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = strings.cancelNameDesc,
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                if (showNameError) {
                    Text(
                        text = strings.nameEmptyError,
                        style = MaterialTheme.typography.bodySmall,
                        color = crystal.liabilityRed,
                        modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (authMode == AuthMode.GUEST) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                guestPhotoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (crystal.isDark) {
                                    Color.White.copy(alpha = 0.24f)
                                } else {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                                }
                            ),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = crystal.glassSurfaceElevated,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("home_guest_upload_avatar_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AddPhotoAlternate,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (avatarUrl.isNotBlank()) {
                                    strings.changeProfilePhoto
                                } else {
                                    strings.uploadProfilePhoto
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (avatarUrl.isNotBlank()) {
                            TextButton(
                                onClick = onClearGuestAvatar,
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("home_guest_reset_avatar_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Refresh,
                                    contentDescription = null,
                                    tint = crystal.mutedText,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = strings.resetProfilePhoto,
                                    color = crystal.mutedText
                                )
                            }
                        }
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        GoogleGLogo(size = 15.dp)
                        Text(
                            text = strings.googleAvatarSyncedNote,
                            style = MaterialTheme.typography.bodySmall,
                            color = crystal.mutedText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
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

@Composable
private fun HeroBalanceSummaryCard(
    totalBalance: Double,
    incomeThisMonth: Double,
    spentThisMonth: Double,
    monthlyBudget: Double,
    netWorth: Double,
    dailyAverage: Double,
    language: AppLanguage,
    strings: AppStrings,
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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
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
                    Column {
                        Text(
                            text = strings.totalBalance,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = strings.liquidAssetsHint,
                            style = MaterialTheme.typography.bodySmall,
                            color = crystal.mutedText
                        )
                    }
                }

                Surface(
                    color = primaryColor.copy(alpha = if (crystal.isDark) 0.22f else 0.13f),
                    shape = CircleShape,
                    border = BorderStroke(0.9.dp, primaryColor.copy(alpha = 0.38f))
                ) {
                    Text(
                        text = "${strings.netWorth}: ${BdtFormatter.format(netWorth, language)}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = primaryColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = BdtFormatter.format(totalBalance, language),
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontFamily = JetBrainsMonoFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 29.sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    color = crystal.assetGreenBg,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(0.8.dp, crystal.assetGreen.copy(alpha = 0.25f)),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable(onClick = onOpenActivity)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
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
                        Column {
                            Text(
                                text = strings.dashboard.incomePillLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = crystal.mutedText,
                                maxLines = 1
                            )
                            Text(
                                text = BdtFormatter.format(incomeThisMonth, language),
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontFamily = JetBrainsMonoFamily,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = crystal.assetGreen,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
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
                        .clip(RoundedCornerShape(14.dp))
                        .clickable(onClick = onOpenActivity)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
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
                        Column {
                            Text(
                                text = strings.spentThisMonth,
                                style = MaterialTheme.typography.labelSmall,
                                color = crystal.mutedText,
                                maxLines = 1
                            )
                            Text(
                                text = BdtFormatter.format(spentThisMonth, language),
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontFamily = JetBrainsMonoFamily,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = crystal.liabilityRed,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
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
                            text = "${strings.monthlyBudget}: ${BdtFormatter.format(monthlyBudget, language)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = crystal.mutedText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "${BdtFormatter.formatNumber(budgetPercent, language)}%",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = JetBrainsMonoFamily,
                                fontWeight = FontWeight.Bold
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

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "${strings.dailyAverageLabel}: ${BdtFormatter.format(dailyAverage, language)}",
                style = MaterialTheme.typography.bodySmall,
                color = crystal.mutedText
            )
        }
    }
}

@Composable
private fun CompactWalletStrip(
    accounts: List<AccountEntity>,
    language: AppLanguage,
    strings: AppStrings,
    onOpenAllAccounts: () -> Unit
) {
    if (accounts.isEmpty()) {
        CrystalCard(
            modifier = Modifier.fillMaxWidth(),
            onClick = onOpenAllAccounts
        ) {
            Text(
                text = strings.addAccount,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
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
                            if (account.type != AccountType.CASH) {
                                Spacer(modifier = Modifier.width(8.dp))
                                CardNetworkOrProviderBadge(account = account)
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
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.82f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = BdtFormatter.format(account.balance, language),
                                    style = MaterialTheme.typography.headlineSmall.copy(
                                        fontFamily = JetBrainsMonoFamily,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 24.sp
                                    ),
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
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
    onOpenAllAccounts: () -> Unit
) {
    val crystal = LocalCrystalColors.current
    if (accounts.isEmpty()) {
        CrystalCard(
            modifier = Modifier.fillMaxWidth(),
            onClick = onOpenAllAccounts
        ) {
            Text(
                text = strings.addAccount,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
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
                text = "${strings.spentLabel}: ${BdtFormatter.format(spent, language)}",
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = JetBrainsMonoFamily),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${strings.budgetLabel}: ${BdtFormatter.format(safeBudget, language)}",
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
                "${strings.remainingLabel}: ${BdtFormatter.format(remaining, language)}"
            } else {
                "${strings.overBudgetLabel}: ${BdtFormatter.format(-remaining, language)}"
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
                            text = BdtFormatter.format(item.amount, language),
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
    strings: AppStrings
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
                text = "${BdtFormatter.format(todaySpend, language)} / ${BdtFormatter.format(dailyAvg, language)}",
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
    onClick: (() -> Unit)? = null
) {
    val crystal = LocalCrystalColors.current
    val dateFmt = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }
    val dateLabel = remember(tx.timestamp) { dateFmt.format(Date(tx.timestamp)) }

    val amountText = when (tx.type) {
        TransactionType.EXPENSE -> "-${BdtFormatter.format(tx.amount, language)}"
        TransactionType.INCOME -> "+${BdtFormatter.format(tx.amount, language)}"
        TransactionType.TRANSFER -> "⇄ ${BdtFormatter.format(tx.amount, language)}"
    }
    val amountColor = when (tx.type) {
        TransactionType.EXPENSE -> crystal.liabilityRed
        TransactionType.INCOME -> crystal.assetGreen
        TransactionType.TRANSFER -> MaterialTheme.colorScheme.primary
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
                    text = tx.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val subPrefix = buildString {
                    if (accountName.isNotBlank()) append(accountName)
                    if (tx.subcategory.isNotBlank()) {
                        if (isNotEmpty()) append(" • ")
                        append(tx.subcategory)
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
        Text(
            text = amountText,
            style = MaterialTheme.typography.titleSmall.copy(
                fontFamily = JetBrainsMonoFamily,
                fontWeight = FontWeight.SemiBold
            ),
            color = amountColor
        )
    }
}
