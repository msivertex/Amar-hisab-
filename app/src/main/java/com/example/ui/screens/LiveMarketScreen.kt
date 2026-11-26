package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShoppingBasket
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.market.GoldCarat
import com.example.data.market.MarketCalculators
import com.example.data.market.MarketCurrency
import com.example.data.market.MarketDataStatus
import com.example.data.market.MarketSnapshot
import com.example.data.market.MarketSyncUiState
import com.example.localization.AppLanguage
import com.example.localization.AppStrings
import com.example.ui.components.CrystalCard
import com.example.ui.theme.HindSiliguriFamily
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalCrystalColors
import com.example.ui.theme.SkyCyanBright
import com.example.ui.theme.SkyCyanPrimary

/**
 * The 5 core Live Market tools shown cleanly inside [LiveMarketScreen].
 * Kept collapsed/hidden on Home until the user taps [HomeLiveMarketEntryCard].
 */
enum class LiveMarketOption {
    CURRENCY_RATE,
    GOLD_PRICE,
    ESSENTIALS,
    BANK_FDR_DPS,
    REMITTANCE_WALLET
}

private enum class LiveMarketPickerTarget {
    CONVERTER_FROM,
    CONVERTER_TO,
    GOLD_DISPLAY,
    REMITTANCE_FROM
}

private enum class RemittanceWalletSubTab {
    REMITTANCE,
    WALLET_FEE
}

/**
 * Prominent, clearly highlighted Crystal Liquid frosted-glass entry card displayed
 * directly below Financial Health on the Home screen.
 * Keeps all 5 inner Live Market tools hidden until tapped.
 */
@Composable
fun HomeLiveMarketEntryCard(
    language: AppLanguage,
    marketSyncState: MarketSyncUiState,
    strings: AppStrings,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val crystal = LocalCrystalColors.current
    val primary = MaterialTheme.colorScheme.primary
    val hub = strings.marketHub
    val snapshot = marketSyncState.snapshot

    val usdToBdt = MarketCalculators.formatDecimal(
        snapshot.convert(1.0, MarketCurrency.USD, MarketCurrency.BDT),
        decimals = 2,
        language = language
    )
    val kwdToBdt = MarketCalculators.formatDecimal(
        snapshot.convert(1.0, MarketCurrency.KWD, MarketCurrency.BDT),
        decimals = 0,
        language = language
    )
    val gold22k = MarketCalculators.formatDecimal(
        snapshot.goldPriceInCurrency(GoldCarat.K22, perGram = false, MarketCurrency.BDT),
        decimals = 0,
        language = language
    )

    val containerGradient = if (crystal.isDark) {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFF0E2942).copy(alpha = 0.94f),
                Color(0xFF10374F).copy(alpha = 0.88f),
                Color(0xFF0C2438).copy(alpha = 0.94f)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFFE6F6FF).copy(alpha = 0.96f),
                Color(0xFFF0FAFF).copy(alpha = 0.94f),
                Color(0xFFE5F8F2).copy(alpha = 0.95f)
            )
        )
    }

    val borderGradient = Brush.linearGradient(
        colors = listOf(
            SkyCyanBright.copy(alpha = if (crystal.isDark) 0.72f else 0.78f),
            primary.copy(alpha = if (crystal.isDark) 0.55f else 0.62f),
            crystal.assetGreen.copy(alpha = if (crystal.isDark) 0.55f else 0.62f)
        )
    )

    val cardShape = RoundedCornerShape(24.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(containerGradient)
            .border(width = 1.4.dp, brush = borderGradient, shape = cardShape)
            .clickable(onClick = onClick)
            .testTag("home_live_market_entry_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                            .clip(RoundedCornerShape(15.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(SkyCyanPrimary, Color(0xFF059669))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.TrendingUp,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column(
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = hub.homeCardTitle,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = HindSiliguriFamily
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            LiveStatusMicroPill(
                                isOnline = marketSyncState.isOnline,
                                isSyncing = marketSyncState.isSyncing,
                                status = snapshot.resolveCurrencyStatus(marketSyncState.isOnline),
                                language = language,
                                strings = strings
                            )
                        }
                        Text(
                            text = hub.homeCardSubtitle,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = HindSiliguriFamily,
                                lineHeight = 16.sp
                            ),
                            color = crystal.mutedText,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.size(6.dp))

                Surface(
                    shape = CircleShape,
                    color = primary,
                    modifier = Modifier.heightIn(min = 38.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(start = 12.dp, end = 8.dp, top = 7.dp, bottom = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = hub.homeCardTapCta,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = HindSiliguriFamily
                            ),
                            color = Color.White,
                            maxLines = 1,
                            softWrap = false
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = hub.homeCardTitle,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Compact 1-line live teaser strip (keeps all 5 inner options collapsed until tapped)
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (crystal.isDark) Color.White.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.72f),
                border = BorderStroke(0.8.dp, crystal.glassBorder),
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
                        text = "🇺🇸 1 USD = BDT $usdToBdt",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = primary,
                        maxLines = 1
                    )
                    Text(
                        text = "🇰🇼 1 KWD = BDT $kwdToBdt",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                    Text(
                        text = "✨ 22K BDT $gold22k",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = crystal.warningAmber,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/**
 * Full-screen Crystal Liquid Live Market Hub entered from Home.
 *
 * Navigation flow:
 * - Level 1 (`selectedOption == null`): Shows ONLY the 5 core option cards cleanly (zero duplicate stacking).
 * - Level 2 (`selectedOption != null`): Shows ONLY the selected option's dedicated detail & calculator view.
 */
@Composable
fun LiveMarketScreen(
    language: AppLanguage,
    marketSyncState: MarketSyncUiState,
    strings: AppStrings,
    onRefreshMarketRates: () -> Unit,
    onBack: () -> Unit
) {
    val crystal = LocalCrystalColors.current
    val primary = MaterialTheme.colorScheme.primary
    val hub = strings.marketHub
    val snapshot = marketSyncState.snapshot

    var selectedOption by remember { mutableStateOf<LiveMarketOption?>(null) }

    // Persistent currency selections across single-option detail visits
    var fromCurrency by remember { mutableStateOf(MarketCurrency.USD) }
    var toCurrency by remember { mutableStateOf(MarketCurrency.BDT) }
    var goldCurrency by remember { mutableStateOf(MarketCurrency.BDT) }
    var remittanceCurrency by remember { mutableStateOf(MarketCurrency.SAR) }
    var activePickerTarget by remember { mutableStateOf<LiveMarketPickerTarget?>(null) }
    var remittanceWalletSubTab by remember { mutableStateOf(RemittanceWalletSubTab.REMITTANCE) }

    val handleBack: () -> Unit = {
        if (selectedOption != null) {
            selectedOption = null
        } else {
            onBack()
        }
    }

    BackHandler(onBack = handleBack)

    val headerTitle = when (selectedOption) {
        null -> hub.homeCardTitle
        LiveMarketOption.CURRENCY_RATE -> hub.tabCurrency
        LiveMarketOption.GOLD_PRICE -> hub.tabGold
        LiveMarketOption.ESSENTIALS -> hub.tabEssentials
        LiveMarketOption.BANK_FDR_DPS -> hub.tabBank
        LiveMarketOption.REMITTANCE_WALLET -> hub.optionRemittanceWalletTitle
    }

    val updatedText = remember(snapshot.lastSyncedMillis, snapshot.sourceLabel, language) {
        if (snapshot.lastSyncedMillis > 0L) {
            val ts = MarketCalculators.formatTimestamp(snapshot.lastSyncedMillis, language)
            "${hub.lastUpdatedPrefix}: $ts • ${snapshot.sourceLabel}"
        } else {
            if (language == AppLanguage.BN) {
                "রেফারেন্স মান • লাইভ রেটের জন্য সিঙ্ক করুন"
            } else {
                "Reference Estimate • Sync for live rates"
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("live_market_screen"),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 620.dp)
                .fillMaxSize()
                .testTag("live_market_list"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Clean Top Header Bar with Back Arrow, Live/Offline Status & Sync Trigger
            item(key = "header") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            onClick = handleBack,
                            shape = CircleShape,
                            color = crystal.glassSurface,
                            border = BorderStroke(1.dp, crystal.glassBorder),
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("live_market_back_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = if (language == AppLanguage.BN) "ফিরে যান" else "Back",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = headerTitle,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = HindSiliguriFamily
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = updatedText,
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = HindSiliguriFamily),
                                color = crystal.mutedText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        LiveStatusBadge(
                            isOnline = marketSyncState.isOnline,
                            isSyncing = marketSyncState.isSyncing,
                            status = snapshot.resolveCurrencyStatus(marketSyncState.isOnline),
                            lastSyncFailed = marketSyncState.lastSyncFailed,
                            language = language,
                            strings = strings
                        )
                        if (marketSyncState.isOnline && !marketSyncState.isSyncing) {
                            Surface(
                                onClick = onRefreshMarketRates,
                                shape = CircleShape,
                                color = primary.copy(alpha = if (crystal.isDark) 0.22f else 0.12f),
                                border = BorderStroke(1.dp, primary.copy(alpha = 0.4f)),
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("market_sync_button")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Filled.Refresh,
                                        contentDescription = hub.syncNowBtn,
                                        tint = primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2. Smart Offline / Sync-Fallback Notice Banner — hides automatically once online & synced
            if (!marketSyncState.isOnline || marketSyncState.lastSyncFailed) {
                item(key = "offline_notice") {
                    val isSyncFailureOnline = marketSyncState.isOnline && marketSyncState.lastSyncFailed
                    val bannerTitle = if (isSyncFailureOnline) {
                        if (language == AppLanguage.BN) {
                            "লাইভ সার্ভার সাময়িকভাবে অনুপলব্ধ — ক্যাশড ডেটা দেখানো হচ্ছে"
                        } else {
                            "Live feed temporarily unavailable — showing cached rates"
                        }
                    } else {
                        hub.offlineNoticeTitle
                    }
                    val bannerBody = if (isSyncFailureOnline) {
                        if (language == AppLanguage.BN) {
                            "সর্বশেষ সংরক্ষিত রেট দেখানো হচ্ছে। লাইভ রেট আনতে নিচের রিফ্রেশ বাটনে ট্যাপ করুন।"
                        } else {
                            "Showing your latest saved rates. Tap Retry to fetch fresh live market quotes."
                        }
                    } else {
                        hub.offlineNoticeBody
                    }

                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = crystal.warningAmberBg,
                        border = BorderStroke(1.dp, crystal.warningAmber.copy(alpha = 0.48f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("market_offline_notice_banner")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = if (isSyncFailureOnline) Icons.Filled.CloudOff else Icons.Filled.WifiOff,
                                contentDescription = null,
                                tint = crystal.warningAmber,
                                modifier = Modifier
                                    .padding(top = 2.dp)
                                    .size(20.dp)
                            )
                            Column(
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = bannerTitle,
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = HindSiliguriFamily
                                    ),
                                    color = crystal.warningAmber
                                )
                                Text(
                                    text = bannerBody,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = HindSiliguriFamily,
                                        lineHeight = 18.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (isSyncFailureOnline && !marketSyncState.isSyncing) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Surface(
                                        onClick = onRefreshMarketRates,
                                        shape = CircleShape,
                                        color = crystal.warningAmber.copy(alpha = if (crystal.isDark) 0.24f else 0.16f),
                                        border = BorderStroke(0.9.dp, crystal.warningAmber.copy(alpha = 0.6f)),
                                        modifier = Modifier
                                            .heightIn(min = 36.dp)
                                            .testTag("market_retry_sync_button")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Refresh,
                                                contentDescription = null,
                                                tint = crystal.warningAmber,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Text(
                                                text = if (language == AppLanguage.BN) "পুনরায় চেষ্টা করুন" else "Retry Sync",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = HindSiliguriFamily
                                                ),
                                                color = crystal.warningAmber
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. Clean Animated Switch: Either the 5-Option Menu OR ONLY the Selected Option Detail View
            item(key = "content") {
                AnimatedContent(
                    targetState = selectedOption,
                    transitionSpec = {
                        if (targetState != null) {
                            (slideInHorizontally { it / 5 } + fadeIn()) togetherWith
                                (slideOutHorizontally { -it / 5 } + fadeOut())
                        } else {
                            (slideInHorizontally { -it / 5 } + fadeIn()) togetherWith
                                (slideOutHorizontally { it / 5 } + fadeOut())
                        }
                    },
                    label = "liveMarketOptionTransition"
                ) { activeOption ->
                    if (activeOption == null) {
                        LiveMarketOptionsMenu(
                            language = language,
                            snapshot = snapshot,
                            isOnline = marketSyncState.isOnline,
                            strings = strings,
                            onSelectOption = { selectedOption = it }
                        )
                    } else {
                        LiveMarketOptionDetailCard(
                            option = activeOption,
                            language = language,
                            snapshot = snapshot,
                            isOnline = marketSyncState.isOnline,
                            strings = strings,
                            fromCurrency = fromCurrency,
                            toCurrency = toCurrency,
                            goldCurrency = goldCurrency,
                            remittanceCurrency = remittanceCurrency,
                            remittanceWalletSubTab = remittanceWalletSubTab,
                            onSelectRemittanceWalletSubTab = { remittanceWalletSubTab = it },
                            onPickFromCurrency = { activePickerTarget = LiveMarketPickerTarget.CONVERTER_FROM },
                            onPickToCurrency = { activePickerTarget = LiveMarketPickerTarget.CONVERTER_TO },
                            onSwapCurrencies = {
                                val old = fromCurrency
                                fromCurrency = toCurrency
                                toCurrency = old
                            },
                            onQuickSelectFrom = { cur ->
                                if (cur == toCurrency) {
                                    toCurrency = fromCurrency
                                }
                                fromCurrency = cur
                            },
                            onPickGoldCurrency = { activePickerTarget = LiveMarketPickerTarget.GOLD_DISPLAY },
                            onSelectRemittanceCurrency = { remittanceCurrency = it },
                            onPickRemittanceCurrency = { activePickerTarget = LiveMarketPickerTarget.REMITTANCE_FROM },
                            onBackToAllOptions = { selectedOption = null }
                        )
                    }
                }
            }
        }
    }

    if (activePickerTarget != null) {
        val currentSelected = when (activePickerTarget!!) {
            LiveMarketPickerTarget.CONVERTER_FROM -> fromCurrency
            LiveMarketPickerTarget.CONVERTER_TO -> toCurrency
            LiveMarketPickerTarget.GOLD_DISPLAY -> goldCurrency
            LiveMarketPickerTarget.REMITTANCE_FROM -> remittanceCurrency
        }
        CurrencyPickerModalSheet(
            language = language,
            snapshot = snapshot,
            selected = currentSelected,
            strings = strings,
            onSelect = { picked ->
                when (activePickerTarget!!) {
                    LiveMarketPickerTarget.CONVERTER_FROM -> fromCurrency = picked
                    LiveMarketPickerTarget.CONVERTER_TO -> toCurrency = picked
                    LiveMarketPickerTarget.GOLD_DISPLAY -> goldCurrency = picked
                    LiveMarketPickerTarget.REMITTANCE_FROM -> remittanceCurrency = picked
                }
                activePickerTarget = null
            },
            onDismiss = { activePickerTarget = null }
        )
    }
}

@Composable
private fun LiveMarketOptionsMenu(
    language: AppLanguage,
    snapshot: MarketSnapshot,
    isOnline: Boolean,
    strings: AppStrings,
    onSelectOption: (LiveMarketOption) -> Unit
) {
    val crystal = LocalCrystalColors.current
    val primary = MaterialTheme.colorScheme.primary
    val hub = strings.marketHub

    val usdRate = MarketCalculators.formatDecimal(
        snapshot.convert(1.0, MarketCurrency.USD, MarketCurrency.BDT),
        decimals = 2,
        language = language
    )
    val kwdRate = MarketCalculators.formatDecimal(
        snapshot.convert(1.0, MarketCurrency.KWD, MarketCurrency.BDT),
        decimals = 0,
        language = language
    )
    val gold22k = MarketCalculators.formatDecimal(
        snapshot.goldPriceInCurrency(GoldCarat.K22, perGram = false, MarketCurrency.BDT),
        decimals = 0,
        language = language
    )
    val fdrRate = MarketCalculators.formatDecimal(
        snapshot.avgBankFdrRatePercent,
        decimals = 1,
        language = language
    )
    val essentialsCount = MarketCalculators.defaultEssentialsCatalog().size
    val essentialsCountStr = if (language == AppLanguage.BN) {
        "রেফারেন্স • ${MarketCalculators.toBanglaDigits(essentialsCount.toString())} পণ্য"
    } else {
        "Ref • $essentialsCount items"
    }
    val goldBadge = if (snapshot.isGoldLiveSynced && snapshot.xauUsdPerOunce > 0.0 && isOnline) {
        "XAU • 22K BDT $gold22k"
    } else {
        "22K BDT $gold22k"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("live_market_options_menu"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = hub.hubSubtitle,
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = HindSiliguriFamily),
            color = crystal.mutedText,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        LiveMarketOptionRowCard(
            title = hub.tabCurrency,
            description = hub.optionCurrencyDesc,
            badgeText = "1 USD BDT $usdRate • 1 KWD BDT $kwdRate",
            icon = Icons.Filled.CurrencyExchange,
            accentColor = primary,
            onClick = { onSelectOption(LiveMarketOption.CURRENCY_RATE) },
            testTag = "live_market_option_${LiveMarketOption.CURRENCY_RATE.name}"
        )

        LiveMarketOptionRowCard(
            title = hub.tabGold,
            description = hub.optionGoldDesc,
            badgeText = goldBadge,
            icon = Icons.Filled.Diamond,
            accentColor = crystal.warningAmber,
            onClick = { onSelectOption(LiveMarketOption.GOLD_PRICE) },
            testTag = "live_market_option_${LiveMarketOption.GOLD_PRICE.name}"
        )

        LiveMarketOptionRowCard(
            title = hub.tabEssentials,
            description = hub.optionEssentialsDesc,
            badgeText = essentialsCountStr,
            icon = Icons.Filled.ShoppingBasket,
            accentColor = crystal.assetGreen,
            onClick = { onSelectOption(LiveMarketOption.ESSENTIALS) },
            testTag = "live_market_option_${LiveMarketOption.ESSENTIALS.name}"
        )

        LiveMarketOptionRowCard(
            title = hub.tabBank,
            description = hub.optionBankDesc,
            badgeText = "Est. FDR ~$fdrRate%",
            icon = Icons.Filled.AccountBalance,
            accentColor = Color(0xFF0EA5E9),
            onClick = { onSelectOption(LiveMarketOption.BANK_FDR_DPS) },
            testTag = "live_market_option_${LiveMarketOption.BANK_FDR_DPS.name}"
        )

        LiveMarketOptionRowCard(
            title = hub.optionRemittanceWalletTitle,
            description = hub.optionRemittanceWalletDesc,
            badgeText = "+2.5% • bKash/Nagad",
            icon = Icons.Filled.Public,
            accentColor = Color(0xFF8B5CF6),
            onClick = { onSelectOption(LiveMarketOption.REMITTANCE_WALLET) },
            testTag = "live_market_option_${LiveMarketOption.REMITTANCE_WALLET.name}"
        )
    }
}

@Composable
private fun LiveMarketOptionRowCard(
    title: String,
    description: String,
    badgeText: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit,
    testTag: String
) {
    val crystal = LocalCrystalColors.current

    CrystalCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(accentColor.copy(alpha = if (crystal.isDark) 0.22f else 0.13f))
                    .border(
                        width = 1.dp,
                        color = accentColor.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(14.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = HindSiliguriFamily
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Surface(
                        shape = CircleShape,
                        color = accentColor.copy(alpha = if (crystal.isDark) 0.18f else 0.11f)
                    ) {
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = JetBrainsMonoFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.5.sp
                            ),
                            color = accentColor,
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = HindSiliguriFamily,
                        lineHeight = 16.sp
                    ),
                    color = crystal.mutedText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = title,
                tint = crystal.mutedText,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun LiveMarketOptionDetailCard(
    option: LiveMarketOption,
    language: AppLanguage,
    snapshot: MarketSnapshot,
    isOnline: Boolean,
    strings: AppStrings,
    fromCurrency: MarketCurrency,
    toCurrency: MarketCurrency,
    goldCurrency: MarketCurrency,
    remittanceCurrency: MarketCurrency,
    remittanceWalletSubTab: RemittanceWalletSubTab,
    onSelectRemittanceWalletSubTab: (RemittanceWalletSubTab) -> Unit,
    onPickFromCurrency: () -> Unit,
    onPickToCurrency: () -> Unit,
    onSwapCurrencies: () -> Unit,
    onQuickSelectFrom: (MarketCurrency) -> Unit,
    onPickGoldCurrency: () -> Unit,
    onSelectRemittanceCurrency: (MarketCurrency) -> Unit,
    onPickRemittanceCurrency: () -> Unit,
    onBackToAllOptions: () -> Unit
) {
    val crystal = LocalCrystalColors.current
    val primary = MaterialTheme.colorScheme.primary
    val hub = strings.marketHub

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("live_market_option_detail"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Quick pill back to the 5-option menu
        Surface(
            onClick = onBackToAllOptions,
            shape = CircleShape,
            color = primary.copy(alpha = if (crystal.isDark) 0.16f else 0.09f),
            border = BorderStroke(0.9.dp, primary.copy(alpha = 0.32f)),
            modifier = Modifier
                .heightIn(min = 38.dp)
                .testTag("live_market_all_options_chip")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    tint = primary,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = hub.backToOptionsLabel,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = HindSiliguriFamily
                    ),
                    color = primary
                )
            }
        }

        CrystalCard(
            modifier = Modifier.fillMaxWidth(),
            elevated = true
        ) {
            when (option) {
                LiveMarketOption.CURRENCY_RATE -> {
                    CurrencyConverterPanel(
                        language = language,
                        snapshot = snapshot,
                        isOnline = isOnline,
                        fromCurrency = fromCurrency,
                        toCurrency = toCurrency,
                        onPickFrom = onPickFromCurrency,
                        onPickTo = onPickToCurrency,
                        onSwap = onSwapCurrencies,
                        onQuickSelectFrom = onQuickSelectFrom,
                        strings = strings
                    )
                }

                LiveMarketOption.GOLD_PRICE -> {
                    GoldPricePanel(
                        language = language,
                        snapshot = snapshot,
                        isOnline = isOnline,
                        displayCurrency = goldCurrency,
                        onPickCurrency = onPickGoldCurrency,
                        strings = strings
                    )
                }

                LiveMarketOption.ESSENTIALS -> {
                    EssentialsMarketPanel(
                        language = language,
                        strings = strings
                    )
                }

                LiveMarketOption.BANK_FDR_DPS -> {
                    BankFdrDpsPanel(
                        language = language,
                        snapshot = snapshot,
                        strings = strings
                    )
                }

                LiveMarketOption.REMITTANCE_WALLET -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SmallPresetChip(
                                text = hub.tabRemittance,
                                selected = remittanceWalletSubTab == RemittanceWalletSubTab.REMITTANCE,
                                onClick = { onSelectRemittanceWalletSubTab(RemittanceWalletSubTab.REMITTANCE) },
                                testTag = "remittance_wallet_subtab_remittance"
                            )
                            SmallPresetChip(
                                text = hub.tabMfs,
                                selected = remittanceWalletSubTab == RemittanceWalletSubTab.WALLET_FEE,
                                onClick = { onSelectRemittanceWalletSubTab(RemittanceWalletSubTab.WALLET_FEE) },
                                testTag = "remittance_wallet_subtab_mfs"
                            )
                        }

                        HorizontalDivider(thickness = 0.6.dp, color = crystal.glassBorder)

                        if (remittanceWalletSubTab == RemittanceWalletSubTab.REMITTANCE) {
                            RemittanceCalculatorPanel(
                                language = language,
                                snapshot = snapshot,
                                isOnline = isOnline,
                                fromCurrency = remittanceCurrency,
                                onSelectCurrency = onSelectRemittanceCurrency,
                                onOpenPicker = onPickRemittanceCurrency,
                                strings = strings
                            )
                        } else {
                            MfsFeeCalculatorPanel(
                                language = language,
                                strings = strings
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveStatusMicroPill(
    isOnline: Boolean,
    isSyncing: Boolean,
    status: MarketDataStatus,
    language: AppLanguage,
    strings: AppStrings
) {
    val crystal = LocalCrystalColors.current
    val primary = MaterialTheme.colorScheme.primary
    val hub = strings.marketHub
    val bgColor = when {
        isSyncing -> primary.copy(alpha = if (crystal.isDark) 0.24f else 0.14f)
        !isOnline -> crystal.warningAmberBg
        status == MarketDataStatus.LIVE_SYNCED -> crystal.assetGreen.copy(alpha = if (crystal.isDark) 0.22f else 0.14f)
        else -> primary.copy(alpha = if (crystal.isDark) 0.20f else 0.12f)
    }
    val tint = when {
        isSyncing -> primary
        !isOnline -> crystal.warningAmber
        status == MarketDataStatus.LIVE_SYNCED -> crystal.assetGreen
        else -> primary
    }
    val label = when {
        isSyncing -> hub.syncingBadge
        !isOnline -> hub.offlineBadge
        status == MarketDataStatus.LIVE_SYNCED -> hub.onlineLiveBadge
        status == MarketDataStatus.CACHED_OFFLINE -> if (language == AppLanguage.BN) "ক্যাশড" else "Cached"
        else -> if (language == AppLanguage.BN) "রেফারেন্স" else "Reference"
    }

    Surface(
        shape = CircleShape,
        color = bgColor
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontFamily = HindSiliguriFamily,
                fontSize = 10.sp
            ),
            color = tint,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun LiveStatusBadge(
    isOnline: Boolean,
    isSyncing: Boolean,
    status: MarketDataStatus,
    lastSyncFailed: Boolean,
    language: AppLanguage,
    strings: AppStrings
) {
    val crystal = LocalCrystalColors.current
    val primary = MaterialTheme.colorScheme.primary
    val hub = strings.marketHub

    val badgeColor = when {
        isSyncing -> primary.copy(alpha = if (crystal.isDark) 0.22f else 0.13f)
        !isOnline || lastSyncFailed -> crystal.warningAmberBg
        status == MarketDataStatus.LIVE_SYNCED -> crystal.assetGreen.copy(alpha = if (crystal.isDark) 0.22f else 0.13f)
        else -> primary.copy(alpha = if (crystal.isDark) 0.20f else 0.11f)
    }
    val badgeTint = when {
        isSyncing -> primary
        !isOnline || lastSyncFailed -> crystal.warningAmber
        status == MarketDataStatus.LIVE_SYNCED -> crystal.assetGreen
        else -> primary
    }
    val badgeText = when {
        isSyncing -> hub.syncingBadge
        !isOnline -> hub.offlineBadge
        lastSyncFailed -> if (language == AppLanguage.BN) "ক্যাশড" else "Cached"
        status == MarketDataStatus.LIVE_SYNCED -> hub.onlineLiveBadge
        status == MarketDataStatus.CACHED_OFFLINE -> if (language == AppLanguage.BN) "ক্যাশড" else "Cached"
        else -> if (language == AppLanguage.BN) "রেফারেন্স" else "Reference"
    }

    Surface(
        shape = CircleShape,
        color = badgeColor,
        border = BorderStroke(0.8.dp, badgeTint.copy(alpha = 0.4f)),
        modifier = Modifier.testTag("live_market_status_badge")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            if (isSyncing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(12.dp),
                    strokeWidth = 1.8.dp,
                    color = badgeTint
                )
            } else {
                Icon(
                    imageVector = if (isOnline && !lastSyncFailed && status == MarketDataStatus.LIVE_SYNCED) {
                        Icons.Filled.CloudDone
                    } else {
                        Icons.Filled.CloudOff
                    },
                    contentDescription = null,
                    tint = badgeTint,
                    modifier = Modifier.size(13.dp)
                )
            }
            Text(
                text = badgeText,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = HindSiliguriFamily
                ),
                color = badgeTint,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}
