package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.AccountEntity
import com.example.data.market.MarketSnapshot
import com.example.data.model.AccountType
import com.example.data.model.AppCurrency
import com.example.data.model.NetWorthSummary
import com.example.localization.AppLanguage
import com.example.localization.AppStrings
import com.example.localization.BdtFormatter
import com.example.ui.components.AccountVisualCard
import com.example.ui.components.CrystalCard
import com.example.ui.components.CurrencyAmountText
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalCrystalColors

@Composable
fun AccountsScreen(
    accounts: List<AccountEntity>,
    netWorth: NetWorthSummary,
    language: AppLanguage,
    strings: AppStrings,
    onBack: () -> Unit,
    onOpenAddWizard: () -> Unit,
    onDeleteAccount: (Long) -> Unit,
    mainCurrencyCode: String = "BDT",
    secondaryCurrencyCode: String = "",
    marketSnapshot: MarketSnapshot = MarketSnapshot.defaultReference()
) {
    BackHandler(onBack = onBack)
    val crystal = LocalCrystalColors.current

    val currencyBreakdowns = remember(accounts) {
        accounts
            .groupBy { AppCurrency.fromCode(it.currencyCode) }
            .entries
            .map { (cur, list) ->
                val netInCur = list.sumOf { acc ->
                    if (acc.type == AccountType.CREDIT_CARD) -acc.balance else acc.balance
                }
                cur to netInCur
            }
            .sortedByDescending { it.first.code == mainCurrencyCode }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 620.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 112.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                val addAccountInteractionSource = remember { MutableInteractionSource() }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("accounts_header_row"),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("accounts_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = strings.close,
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        Text(
                            text = strings.accountsTitle,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontSize = 18.5.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.testTag("accounts_header_title")
                        )
                    }

                    Box(
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .clickable(
                                interactionSource = addAccountInteractionSource,
                                indication = null,
                                role = Role.Button,
                                onClick = onOpenAddWizard
                            )
                            .testTag("add_account_wizard_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(11.dp),
                            color = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            shadowElevation = 2.dp,
                            modifier = Modifier
                                .height(32.dp)
                                .clip(RoundedCornerShape(11.dp))
                                .indication(addAccountInteractionSource, LocalIndication.current)
                                .testTag("add_account_wizard_pill")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = strings.addAccount,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        lineHeight = 14.sp,
                                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                                    ),
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Clip,
                                    modifier = Modifier.testTag("add_account_wizard_label")
                                )
                            }
                        }
                    }
                }
            }

            // Net Worth Header with Green (Assets) vs. Red (Liabilities) bar + Multi-Currency breakdown
            item {
                NetWorthHeaderCard(
                    netWorth = netWorth,
                    language = language,
                    strings = strings,
                    mainCurrencyCode = mainCurrencyCode,
                    secondaryCurrencyCode = secondaryCurrencyCode,
                    marketSnapshot = marketSnapshot,
                    currencyBreakdowns = currencyBreakdowns
                )
            }

            if (accounts.isEmpty()) {
                item {
                    CrystalCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("accounts_empty_placeholder"),
                        elevated = true
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 18.dp, horizontal = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AccountBalanceWallet,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Text(
                                text = if (language == AppLanguage.BN) {
                                    "এখনও কোনো ব্যাংক বা কার্ড যুক্ত করা হয়নি"
                                } else {
                                    "No Wallets or Cards Added Yet"
                                },
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = if (language == AppLanguage.BN) {
                                    "বাংলাদেশ, সৌদি আরব, কাতার, কুয়েত, দুবাই, মালয়েশিয়া, ইতালি, ইউরোপ বা যুক্তরাজ্যের ব্যাংক, কার্ড বা মোবাইল ওয়ালেট যুক্ত করুন।"
                                } else {
                                    "Add banks, cards, cash, or MFS wallets across Bangladesh, Saudi Arabia, Qatar, Kuwait, UAE, Malaysia, Italy, Europe, or UK."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = crystal.mutedText,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Button(
                                onClick = onOpenAddWizard,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.testTag("accounts_empty_add_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.size(6.dp))
                                Text(strings.addAccount)
                            }
                        }
                    }
                }
            }

            items(accounts, key = { it.id }) { account ->
                Box(modifier = Modifier.fillMaxWidth()) {
                    AccountVisualCard(
                        account = account,
                        language = language,
                        strings = strings
                    )
                    Surface(
                        color = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.36f),
                        shape = CircleShape,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 12.dp, end = 124.dp)
                    ) {
                        IconButton(
                            onClick = { onDeleteAccount(account.id) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DeleteOutline,
                                contentDescription = strings.deleteAccount,
                                tint = androidx.compose.ui.graphics.Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NetWorthHeaderCard(
    netWorth: NetWorthSummary,
    language: AppLanguage,
    strings: AppStrings,
    mainCurrencyCode: String = "BDT",
    secondaryCurrencyCode: String = "",
    marketSnapshot: MarketSnapshot = MarketSnapshot.defaultReference(),
    currencyBreakdowns: List<Pair<AppCurrency, Double>> = emptyList()
) {
    val crystal = LocalCrystalColors.current
    val mainCur = AppCurrency.fromCode(mainCurrencyCode)
    val secCur = AppCurrency.fromCodeOrNull(secondaryCurrencyCode)?.takeIf { it != mainCur }
    val animatedAssetRatio by animateFloatAsState(
        targetValue = netWorth.assetRatio,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "netWorthRatio"
    )

    CrystalCard(
        modifier = Modifier.fillMaxWidth(),
        elevated = true
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = strings.netWorth,
                style = MaterialTheme.typography.labelMedium,
                color = crystal.mutedText
            )
            Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = if (crystal.isDark) 0.22f else 0.12f),
                shape = RoundedCornerShape(99.dp)
            ) {
                Text(
                    text = mainCur.shortBadge(),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        CurrencyAmountText(
            amount = netWorth.netWorth,
            currencyCode = mainCur.code,
            language = language,
            style = MaterialTheme.typography.headlineLarge.copy(
                fontFamily = JetBrainsMonoFamily,
                fontWeight = FontWeight.Bold
            ),
            isBalance = true,
            bdtFontScale = 0.84f,
            minFontSize = 16.sp,
            modifier = Modifier.fillMaxWidth()
        )

        if (secCur != null) {
            Spacer(modifier = Modifier.height(6.dp))
            val secNetWorth = marketSnapshot.convertCurrencyCode(netWorth.netWorth, mainCur.code, secCur.code)
            Surface(
                color = if (crystal.isDark) crystal.glassSurface else crystal.glassSurfaceElevated,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, crystal.glassBorder)
            ) {
                CurrencyAmountText(
                    amount = secNetWorth,
                    currencyCode = secCur.code,
                    language = language,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.SemiBold
                    ),
                    isBalance = true,
                    prefix = "≈ ${secCur.flag} ",
                    colorOverride = if (secCur == AppCurrency.KWD) crystal.kwdNumericInk else MaterialTheme.colorScheme.primary,
                    bdtFontScale = 0.92f,
                    minFontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Visual Green (Assets) vs Red (Liabilities) Ratio Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(CircleShape)
                .background(crystal.liabilityRed.copy(alpha = 0.25f))
        ) {
            if (animatedAssetRatio > 0f) {
                Box(
                    modifier = Modifier
                        .weight(animatedAssetRatio.coerceAtLeast(0.02f))
                        .height(12.dp)
                        .background(crystal.assetGreen)
                )
            }
            val liabilityRatio = (1f - animatedAssetRatio)
            if (liabilityRatio > 0.01f) {
                Box(
                    modifier = Modifier
                        .weight(liabilityRatio.coerceAtLeast(0.02f))
                        .height(12.dp)
                        .background(crystal.liabilityRed)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(crystal.assetGreen)
                    )
                    Text(
                        text = strings.assetsLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = crystal.mutedText
                    )
                }
                Text(
                    text = BdtFormatter.formatBalance(netWorth.totalAssets, mainCur.code, language),
                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFamily),
                    color = crystal.assetGreen
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(crystal.liabilityRed)
                    )
                    Text(
                        text = strings.liabilitiesLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = crystal.mutedText
                    )
                }
                Text(
                    text = BdtFormatter.formatBalance(netWorth.totalLiabilities, mainCur.code, language),
                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFamily),
                    color = crystal.liabilityRed
                )
            }
        }

        if (currencyBreakdowns.size > 1) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                currencyBreakdowns.forEach { (cur, totalInCur) ->
                    Surface(
                        color = if (crystal.isDark) crystal.glassSurface else crystal.glassSurfaceElevated,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, crystal.glassBorder)
                    ) {
                        Text(
                            text = "${cur.flag} ${BdtFormatter.formatCurrency(totalInCur, cur.code, language)}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = JetBrainsMonoFamily,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        }
    }
}
