package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBasket
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.market.EssentialGroceryItem
import com.example.data.market.EssentialGroup
import com.example.data.market.GoldCarat
import com.example.data.market.MarketCalculators
import com.example.data.market.MarketCurrency
import com.example.data.market.MarketDataStatus
import com.example.data.market.MarketSnapshot
import com.example.data.market.MarketSyncUiState
import com.example.data.market.MarketToolTab
import com.example.data.market.MfsChannel
import com.example.localization.AppLanguage
import com.example.localization.AppStrings
import com.example.localization.BdtFormatter
import com.example.ui.components.CrystalCard
import com.example.ui.theme.HindSiliguriFamily
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalCrystalColors

private enum class CurrencyPickerTarget {
    CONVERTER_FROM,
    CONVERTER_TO,
    GOLD_DISPLAY,
    REMITTANCE_FROM
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketInteractiveToolsCard(
    language: AppLanguage,
    marketSyncState: MarketSyncUiState,
    selectedTab: MarketToolTab,
    onSelectTab: (MarketToolTab) -> Unit,
    strings: AppStrings
) {
    val crystal = LocalCrystalColors.current
    val primary = MaterialTheme.colorScheme.primary
    val hub = strings.marketHub
    val snapshot = marketSyncState.snapshot

    // Shared currency state across panels (never hardcoded — freely switchable across all 15 currencies)
    var fromCurrency by remember { mutableStateOf(MarketCurrency.USD) }
    var toCurrency by remember { mutableStateOf(MarketCurrency.BDT) }
    var goldCurrency by remember { mutableStateOf(MarketCurrency.BDT) }
    var remittanceCurrency by remember { mutableStateOf(MarketCurrency.SAR) }
    var activePickerTarget by remember { mutableStateOf<CurrencyPickerTarget?>(null) }

    val toolTabs = remember(hub, crystal.assetGreen, crystal.warningAmber, primary) {
        listOf(
            Triple(MarketToolTab.CURRENCY, hub.tabCurrency, Icons.Filled.CurrencyExchange to crystal.assetGreen),
            Triple(MarketToolTab.GOLD, hub.tabGold, Icons.Filled.Diamond to crystal.warningAmber),
            Triple(MarketToolTab.ESSENTIALS, hub.tabEssentials, Icons.Filled.ShoppingBasket to Color(0xFF0D9488)),
            Triple(MarketToolTab.BANK_FDR_DPS, hub.tabBank, Icons.Filled.AccountBalance to primary),
            Triple(MarketToolTab.REMITTANCE, hub.tabRemittance, Icons.Filled.VolunteerActivism to Color(0xFF7C3AED)),
            Triple(MarketToolTab.MFS_FEE, hub.tabMfs, Icons.Filled.PhoneAndroid to Color(0xFFE11D48))
        )
    }

    CrystalCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("market_interactive_tools_card")
    ) {
        // Section Header + Timestamp Pill
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = hub.toolsSectionTitle,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = HindSiliguriFamily
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                shape = CircleShape,
                color = if (marketSyncState.isOnline && snapshot.isLiveSynced) {
                    crystal.assetGreen.copy(alpha = if (crystal.isDark) 0.20f else 0.12f)
                } else {
                    primary.copy(alpha = if (crystal.isDark) 0.20f else 0.10f)
                }
            ) {
                Text(
                    text = MarketCalculators.formatTimestamp(snapshot.lastSyncedMillis, language),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = HindSiliguriFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 10.5.sp
                    ),
                    color = if (marketSyncState.isOnline && snapshot.isLiveSynced) crystal.assetGreen else primary,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Scrollable Tool Selector Pills (Zero clipping)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .testTag("market_tools_tab_bar"),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            toolTabs.forEach { (tab, label, iconTint) ->
                val isSelected = selectedTab == tab
                val (icon, tint) = iconTint
                Surface(
                    onClick = { onSelectTab(tab) },
                    shape = CircleShape,
                    color = if (isSelected) {
                        tint.copy(alpha = if (crystal.isDark) 0.26f else 0.15f)
                    } else if (crystal.isDark) {
                        Color.White.copy(alpha = 0.06f)
                    } else {
                        Color(0xFFF3F7FC)
                    },
                    border = BorderStroke(
                        width = if (isSelected) 1.2.dp else 0.8.dp,
                        color = if (isSelected) tint.copy(alpha = 0.72f) else crystal.glassBorder
                    ),
                    modifier = Modifier
                        .heightIn(min = 40.dp)
                        .testTag("market_tool_tab_${tab.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isSelected) tint else crystal.mutedText,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontFamily = HindSiliguriFamily
                            ),
                            color = if (isSelected) MaterialTheme.colorScheme.onSurface else crystal.mutedText,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
        HorizontalDivider(thickness = 0.7.dp, color = crystal.glassBorder)
        Spacer(modifier = Modifier.height(14.dp))

        AnimatedContent(
            targetState = selectedTab,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "marketToolTransition"
        ) { tab ->
            when (tab) {
                MarketToolTab.CURRENCY -> CurrencyConverterPanel(
                    language = language,
                    snapshot = snapshot,
                    isOnline = marketSyncState.isOnline,
                    fromCurrency = fromCurrency,
                    toCurrency = toCurrency,
                    onPickFrom = { activePickerTarget = CurrencyPickerTarget.CONVERTER_FROM },
                    onPickTo = { activePickerTarget = CurrencyPickerTarget.CONVERTER_TO },
                    onSwap = {
                        val oldFrom = fromCurrency
                        fromCurrency = toCurrency
                        toCurrency = oldFrom
                    },
                    onQuickSelectFrom = { cur ->
                        if (cur == toCurrency) {
                            toCurrency = fromCurrency
                        }
                        fromCurrency = cur
                    },
                    strings = strings
                )

                MarketToolTab.GOLD -> GoldPricePanel(
                    language = language,
                    snapshot = snapshot,
                    isOnline = marketSyncState.isOnline,
                    displayCurrency = goldCurrency,
                    onPickCurrency = { activePickerTarget = CurrencyPickerTarget.GOLD_DISPLAY },
                    strings = strings
                )

                MarketToolTab.ESSENTIALS -> EssentialsMarketPanel(
                    language = language,
                    strings = strings
                )

                MarketToolTab.BANK_FDR_DPS -> BankFdrDpsPanel(
                    language = language,
                    snapshot = snapshot,
                    strings = strings
                )

                MarketToolTab.REMITTANCE -> RemittanceCalculatorPanel(
                    language = language,
                    snapshot = snapshot,
                    isOnline = marketSyncState.isOnline,
                    fromCurrency = remittanceCurrency,
                    onSelectCurrency = { remittanceCurrency = it },
                    onOpenPicker = { activePickerTarget = CurrencyPickerTarget.REMITTANCE_FROM },
                    strings = strings
                )

                MarketToolTab.MFS_FEE -> MfsFeeCalculatorPanel(
                    language = language,
                    strings = strings
                )
            }
        }
    }

    if (activePickerTarget != null) {
        val currentSelected = when (activePickerTarget!!) {
            CurrencyPickerTarget.CONVERTER_FROM -> fromCurrency
            CurrencyPickerTarget.CONVERTER_TO -> toCurrency
            CurrencyPickerTarget.GOLD_DISPLAY -> goldCurrency
            CurrencyPickerTarget.REMITTANCE_FROM -> remittanceCurrency
        }
        CurrencyPickerModalSheet(
            language = language,
            snapshot = snapshot,
            selected = currentSelected,
            strings = strings,
            onSelect = { picked ->
                when (activePickerTarget!!) {
                    CurrencyPickerTarget.CONVERTER_FROM -> fromCurrency = picked
                    CurrencyPickerTarget.CONVERTER_TO -> toCurrency = picked
                    CurrencyPickerTarget.GOLD_DISPLAY -> goldCurrency = picked
                    CurrencyPickerTarget.REMITTANCE_FROM -> remittanceCurrency = picked
                }
                activePickerTarget = null
            },
            onDismiss = { activePickerTarget = null }
        )
    }
}

@Composable
internal fun MarketSourceTransparencyChip(
    status: MarketDataStatus,
    language: AppLanguage,
    sourceLabel: String? = null,
    timestampMillis: Long = 0L,
    customLabelBn: String? = null,
    customLabelEn: String? = null,
    testTag: String = "market_source_transparency_badge"
) {
    val crystal = LocalCrystalColors.current
    val primary = MaterialTheme.colorScheme.primary

    val (tint, bg, icon, defaultLabel) = when (status) {
        MarketDataStatus.LIVE_SYNCED -> Quad(
            crystal.assetGreen,
            crystal.assetGreen.copy(alpha = if (crystal.isDark) 0.18f else 0.11f),
            Icons.Filled.CloudDone,
            if (language == AppLanguage.BN) "লাইভ সিঙ্কড" else "Live Synced"
        )
        MarketDataStatus.CACHED_OFFLINE -> Quad(
            crystal.warningAmber,
            crystal.warningAmberBg,
            Icons.Filled.CloudOff,
            if (language == AppLanguage.BN) "ক্যাশড / অফলাইন" else "Cached / Offline"
        )
        MarketDataStatus.REFERENCE_ESTIMATE -> Quad(
            primary,
            primary.copy(alpha = if (crystal.isDark) 0.18f else 0.10f),
            Icons.Filled.Info,
            if (language == AppLanguage.BN) "রেফারেন্স / আনুমানিক হিসাব" else "Reference / Estimate"
        )
    }

    val title = if (language == AppLanguage.BN) (customLabelBn ?: defaultLabel) else (customLabelEn ?: defaultLabel)
    val detailParts = buildList {
        if (!sourceLabel.isNullOrBlank() && status != MarketDataStatus.REFERENCE_ESTIMATE) {
            add(sourceLabel)
        }
        if (timestampMillis > 0L) {
            add(MarketCalculators.formatTimestamp(timestampMillis, language))
        }
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = bg,
        border = BorderStroke(0.8.dp, tint.copy(alpha = 0.36f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = if (detailParts.isEmpty()) title else "$title • ${detailParts.joinToString(" • ")}",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = HindSiliguriFamily,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 15.sp
                ),
                color = tint,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CurrencyConverterPanel(
    language: AppLanguage,
    snapshot: MarketSnapshot,
    fromCurrency: MarketCurrency,
    toCurrency: MarketCurrency,
    onPickFrom: () -> Unit,
    onPickTo: () -> Unit,
    onSwap: () -> Unit,
    onQuickSelectFrom: (MarketCurrency) -> Unit,
    strings: AppStrings,
    isOnline: Boolean = true
) {
    val crystal = LocalCrystalColors.current
    val primary = MaterialTheme.colorScheme.primary
    val hub = strings.marketHub

    // Default amount starts at "1" (never hardcoded to a single pair; editable to any value >= 1)
    var amountText by remember { mutableStateOf("1") }
    var useCustomRate by remember { mutableStateOf(false) }
    var customRateText by remember(fromCurrency, toCurrency) { mutableStateOf("") }

    val parsedAmount = amountText.toDoubleOrNull()?.takeIf { it > 0.0 } ?: 1.0
    val marketUnitRate = snapshot.convert(1.0, fromCurrency, toCurrency)
    val customUnitRate = customRateText.toDoubleOrNull()?.takeIf { it > 0.0 }
    val effectiveUnitRate = if (useCustomRate && customUnitRate != null) customUnitRate else marketUnitRate
    val converted = parsedAmount * effectiveUnitRate
    val inverseUnit = if (effectiveUnitRate > 0.0) 1.0 / effectiveUnitRate else 0.0

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("market_currency_converter_panel"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        MarketSourceTransparencyChip(
            status = snapshot.resolveCurrencyStatus(isOnline),
            language = language,
            sourceLabel = snapshot.sourceLabel,
            timestampMillis = snapshot.lastSyncedMillis,
            testTag = "currency_source_transparency_badge"
        )

        // From <-> To Selector Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CurrencyDropdownButton(
                label = hub.fromCurrencyLabel,
                currency = fromCurrency,
                language = language,
                onClick = onPickFrom,
                modifier = Modifier
                    .weight(1f)
                    .testTag("currency_from_selector")
            )

            Surface(
                onClick = onSwap,
                shape = CircleShape,
                color = primary.copy(alpha = if (crystal.isDark) 0.24f else 0.13f),
                border = BorderStroke(1.dp, primary.copy(alpha = 0.42f)),
                modifier = Modifier
                    .size(48.dp)
                    .testTag("currency_swap_button")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.SwapHoriz,
                        contentDescription = hub.swapCurrenciesDesc,
                        tint = primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            CurrencyDropdownButton(
                label = hub.toCurrencyLabel,
                currency = toCurrency,
                language = language,
                onClick = onPickTo,
                modifier = Modifier
                    .weight(1f)
                    .testTag("currency_to_selector")
            )
        }

        // Amount Input (starts from 1) + Quick Amount Pills
        OutlinedTextField(
            value = amountText,
            onValueChange = { raw ->
                val cleaned = raw.filter { it.isDigit() || it == '.' }
                if (cleaned.count { it == '.' } <= 1) {
                    amountText = cleaned
                }
            },
            label = {
                Text(
                    text = hub.amountLabel,
                    style = MaterialTheme.typography.labelMedium.copy(fontFamily = HindSiliguriFamily)
                )
            },
            prefix = {
                Text(
                    text = "${fromCurrency.symbol} ",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.Bold
                    ),
                    color = primary
                )
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = primary,
                unfocusedBorderColor = crystal.glassBorder
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("currency_amount_input")
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("1", "10", "100", "500", "1000").forEach { preset ->
                val selected = amountText == preset
                SmallPresetChip(
                    text = if (language == AppLanguage.BN) MarketCalculators.toBanglaDigits(preset) else preset,
                    selected = selected,
                    onClick = { amountText = preset },
                    testTag = "currency_amount_preset_$preset"
                )
            }
        }

        // Optional Custom Exchange Rate Override (e.g., local money exchange / bank counter rate)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (language == AppLanguage.BN) {
                    "কাস্টম এক্সচেঞ্জ রেট ব্যবহার করুন"
                } else {
                    "Use custom bank / exchange rate"
                },
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = HindSiliguriFamily),
                color = crystal.mutedText,
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = useCustomRate,
                onCheckedChange = { enabled ->
                    useCustomRate = enabled
                    if (enabled && customRateText.isBlank()) {
                        customRateText = String.format(java.util.Locale.US, "%.2f", marketUnitRate)
                    }
                },
                modifier = Modifier.testTag("currency_custom_rate_toggle")
            )
        }

        if (useCustomRate) {
            OutlinedTextField(
                value = customRateText,
                onValueChange = { raw ->
                    val cleaned = raw.filter { it.isDigit() || it == '.' }
                    if (cleaned.count { it == '.' } <= 1) customRateText = cleaned
                },
                label = {
                    Text(
                        text = if (language == AppLanguage.BN) {
                            "১ ${fromCurrency.code} = কত ${toCurrency.code}?"
                        } else {
                            "Custom rate: 1 ${fromCurrency.code} in ${toCurrency.code}"
                        },
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = HindSiliguriFamily)
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("currency_custom_rate_input")
            )
        }

        // Result Display Card
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (crystal.isDark) Color(0xFF132238) else Color(0xFFEFF6FF),
            border = BorderStroke(1.dp, primary.copy(alpha = if (crystal.isDark) 0.42f else 0.28f)),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("currency_conversion_result")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val formattedInput = if (parsedAmount == parsedAmount.toLong().toDouble()) {
                    val intText = parsedAmount.toLong().toString()
                    if (language == AppLanguage.BN) MarketCalculators.toBanglaDigits(intText) else intText
                } else {
                    MarketCalculators.formatDecimal(parsedAmount, 2, language)
                }
                val formattedOutput = MarketCalculators.formatDecimal(converted, 2, language)
                val inverseFormatted = MarketCalculators.formatDecimal(
                    inverseUnit,
                    decimals = if (inverseUnit < 0.05) 4 else 2,
                    language = language
                )

                Text(
                    text = "${fromCurrency.flag} $formattedInput ${fromCurrency.code} =",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = HindSiliguriFamily,
                        fontWeight = FontWeight.Medium
                    ),
                    color = crystal.mutedText
                )
                Text(
                    text = "${toCurrency.symbol}$formattedOutput ${toCurrency.code}",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.ExtraBold
                    ),
                    color = primary
                )
                Text(
                    text = "1 ${toCurrency.code} = ${fromCurrency.symbol}$inverseFormatted ${fromCurrency.code} • ${fromCurrency.countryName(language)} → ${toCurrency.countryName(language)}",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = HindSiliguriFamily),
                    color = crystal.mutedText
                )
            }
        }

        // Quick 1-Unit Expat & Regional Comparison Pills
        Text(
            text = hub.popularCorridorsLabel,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontFamily = HindSiliguriFamily
            ),
            color = crystal.mutedText
        )

        val corridorList = remember(toCurrency) {
            listOf(
                MarketCurrency.KWD,
                MarketCurrency.SAR,
                MarketCurrency.AED,
                MarketCurrency.QAR,
                MarketCurrency.BHD,
                MarketCurrency.USD,
                MarketCurrency.EUR,
                MarketCurrency.GBP,
                MarketCurrency.MYR,
                MarketCurrency.INR
            ).filter { it != toCurrency }
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            corridorList.forEach { cur ->
                val oneUnit = snapshot.convert(1.0, cur, toCurrency)
                val valText = MarketCalculators.formatDecimal(oneUnit, 2, language)
                val isCurrent = cur == fromCurrency
                Surface(
                    onClick = {
                        amountText = "1"
                        onQuickSelectFrom(cur)
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isCurrent) {
                        crystal.assetGreen.copy(alpha = if (crystal.isDark) 0.22f else 0.14f)
                    } else if (crystal.isDark) {
                        Color.White.copy(alpha = 0.05f)
                    } else {
                        Color(0xFFF6F9FD)
                    },
                    border = BorderStroke(
                        width = 0.9.dp,
                        color = if (isCurrent) crystal.assetGreen.copy(alpha = 0.55f) else crystal.glassBorder
                    ),
                    modifier = Modifier.testTag("currency_corridor_${cur.code}")
                ) {
                    Text(
                        text = "${cur.flag} 1 ${cur.code} = ${toCurrency.symbol}$valText",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CurrencyDropdownButton(
    label: String,
    currency: MarketCurrency,
    language: AppLanguage,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val crystal = LocalCrystalColors.current
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (crystal.isDark) Color.White.copy(alpha = 0.06f) else Color(0xFFF4F8FE),
        border = BorderStroke(1.dp, crystal.glassBorder),
        modifier = modifier.heightIn(min = 58.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = HindSiliguriFamily,
                        fontSize = 10.5.sp
                    ),
                    color = crystal.mutedText,
                    maxLines = 1
                )
                Text(
                    text = "${currency.flag} ${currency.code}",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Text(
                    text = currency.countryName(language),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = HindSiliguriFamily,
                        fontSize = 10.sp
                    ),
                    color = crystal.mutedText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                tint = crystal.mutedText,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun GoldPricePanel(
    language: AppLanguage,
    snapshot: MarketSnapshot,
    displayCurrency: MarketCurrency,
    onPickCurrency: () -> Unit,
    strings: AppStrings,
    isOnline: Boolean = true
) {
    val crystal = LocalCrystalColors.current
    val primary = MaterialTheme.colorScheme.primary
    val hub = strings.marketHub

    var perGram by remember { mutableStateOf(false) }
    var selectedCarat by remember { mutableStateOf(GoldCarat.K22) }
    var weightText by remember { mutableStateOf("1") }
    var includeVatMaking by remember { mutableStateOf(false) }
    var vatMakingPercentText by remember { mutableStateOf("5") }

    val qty = weightText.toDoubleOrNull()?.takeIf { it > 0.0 } ?: 1.0
    val markupPct = vatMakingPercentText.toDoubleOrNull()?.coerceIn(0.0, 40.0) ?: 5.0
    val unitPrice = snapshot.goldPriceInCurrency(selectedCarat, perGram, displayCurrency)
    val baseTotal = unitPrice * qty
    val finalTotal = if (includeVatMaking) baseTotal * (1.0 + markupPct / 100.0) else baseTotal

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("market_gold_panel"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        MarketSourceTransparencyChip(
            status = snapshot.resolveGoldStatus(isOnline),
            language = language,
            sourceLabel = snapshot.goldSourceLabel,
            timestampMillis = snapshot.goldLastSyncedMillis.takeIf { it > 0L } ?: snapshot.lastSyncedMillis,
            testTag = "gold_source_transparency_badge"
        )

        if (snapshot.xauUsdPerOunce > 0.0) {
            val spotText = MarketCalculators.formatDecimal(snapshot.xauUsdPerOunce, 2, language)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = crystal.warningAmberBg,
                border = BorderStroke(0.8.dp, crystal.warningAmber.copy(alpha = 0.42f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("gold_xau_spot_pill")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (language == AppLanguage.BN) "আন্তর্জাতিক XAU স্পট (প্রতি আউন্স)" else "Global XAU Spot (per troy oz)",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = HindSiliguriFamily),
                        color = crystal.mutedText
                    )
                    Text(
                        text = "$$spotText USD",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = crystal.warningAmber
                    )
                }
            }
        }

        // Unit toggle (Vori vs Gram) + Currency Selector
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            SmallPresetChip(
                text = hub.goldPerVori,
                selected = !perGram,
                onClick = { perGram = false },
                testTag = "gold_unit_vori"
            )
            SmallPresetChip(
                text = hub.goldPerGram,
                selected = perGram,
                onClick = { perGram = true },
                testTag = "gold_unit_gram"
            )
            Surface(
                onClick = onPickCurrency,
                shape = CircleShape,
                color = primary.copy(alpha = if (crystal.isDark) 0.22f else 0.12f),
                border = BorderStroke(1.dp, primary.copy(alpha = 0.42f)),
                modifier = Modifier
                    .heightIn(min = 36.dp)
                    .testTag("gold_currency_selector")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "${displayCurrency.flag} ${displayCurrency.code}",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = primary
                    )
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = hub.goldDisplayCurrency,
                        tint = primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Carat Cards (24K Pure, 22K, 21K, 18K, Traditional)
        val rows = GoldCarat.entries.chunked(2)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            rows.forEach { pair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    pair.forEach { carat ->
                        val isSelected = selectedCarat == carat
                        val price = snapshot.goldPriceInCurrency(carat, perGram, displayCurrency)
                        val formatted = "${displayCurrency.symbol}${MarketCalculators.formatDecimal(price, if (displayCurrency == MarketCurrency.BDT) 0 else 2, language)}"
                        Surface(
                            onClick = { selectedCarat = carat },
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) {
                                crystal.warningAmber.copy(alpha = if (crystal.isDark) 0.22f else 0.14f)
                            } else if (crystal.isDark) {
                                Color.White.copy(alpha = 0.05f)
                            } else {
                                Color(0xFFFFFBF2)
                            },
                            border = BorderStroke(
                                width = if (isSelected) 1.3.dp else 0.8.dp,
                                color = if (isSelected) crystal.warningAmber else crystal.glassBorder
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("gold_carat_${carat.name}")
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    text = carat.label(language),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = HindSiliguriFamily,
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = crystal.mutedText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = formatted,
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontFamily = JetBrainsMonoFamily,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                    if (pair.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        // Weight Estimator + VAT Switch
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = weightText,
                onValueChange = { raw ->
                    val cleaned = raw.filter { it.isDigit() || it == '.' }
                    if (cleaned.count { it == '.' } <= 1) weightText = cleaned
                },
                label = {
                    Text(
                        text = "${hub.goldWeightLabel} (${if (perGram) "g" else if (language == AppLanguage.BN) "ভরি" else "vori"})",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = HindSiliguriFamily)
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("gold_weight_input")
            )

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = crystal.warningAmberBg,
                border = BorderStroke(1.dp, crystal.warningAmber.copy(alpha = 0.42f)),
                modifier = Modifier.weight(1.3f)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = hub.goldEstimatedTotal,
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = HindSiliguriFamily),
                        color = crystal.mutedText,
                        maxLines = 1
                    )
                    Text(
                        text = "${displayCurrency.symbol}${MarketCalculators.formatDecimal(finalTotal, if (displayCurrency == MarketCurrency.BDT) 0 else 2, language)}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.ExtraBold
                        ),
                        color = crystal.warningAmber,
                        maxLines = 1
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = hub.goldIncludeVatMaking,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = HindSiliguriFamily),
                color = crystal.mutedText,
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = includeVatMaking,
                onCheckedChange = { includeVatMaking = it }
            )
        }

        if (includeVatMaking) {
            OutlinedTextField(
                value = vatMakingPercentText,
                onValueChange = { raw ->
                    val cleaned = raw.filter { it.isDigit() || it == '.' }
                    if (cleaned.count { it == '.' } <= 1) vatMakingPercentText = cleaned
                },
                label = {
                    Text(
                        text = if (language == AppLanguage.BN) {
                            "ভ্যাট ও মজুরি হার (%) — কাস্টমাইজযোগ্য"
                        } else {
                            "Custom VAT & Making Charge (%)"
                        },
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = HindSiliguriFamily)
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("gold_custom_markup_input")
            )
        }
    }
}

@Composable
internal fun EssentialsMarketPanel(
    language: AppLanguage,
    strings: AppStrings
) {
    val crystal = LocalCrystalColors.current
    val primary = MaterialTheme.colorScheme.primary
    val hub = strings.marketHub

    val catalog = remember { MarketCalculators.defaultEssentialsCatalog() }
    var selectedGroup by remember { mutableStateOf(EssentialGroup.ALL) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedIds by remember { mutableStateOf(setOf<String>()) }
    val customPrices = remember { mutableStateMapOf<String, Int>() }
    var editingItem by remember { mutableStateOf<EssentialGroceryItem?>(null) }
    var editPriceText by remember { mutableStateOf("") }

    val filtered = remember(catalog, selectedGroup, searchQuery, language) {
        catalog.filter { item ->
            val groupMatch = selectedGroup == EssentialGroup.ALL || item.group == selectedGroup
            val q = searchQuery.trim()
            val queryMatch = q.isEmpty() ||
                item.nameEn.contains(q, ignoreCase = true) ||
                item.nameBn.contains(q, ignoreCase = true)
            groupMatch && queryMatch
        }
    }

    val basketTotal = catalog.filter { it.id in selectedIds }.sumOf { item ->
        customPrices[item.id]?.toDouble() ?: item.midPriceBdt
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("market_essentials_panel"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        MarketSourceTransparencyChip(
            status = MarketDataStatus.REFERENCE_ESTIMATE,
            language = language,
            customLabelBn = "রেফারেন্স বাজার দর • যেকোনো পণ্যের দাম এডিট করুন",
            customLabelEn = "Reference Market Estimates • Tap edit to customize local prices",
            testTag = "essentials_source_transparency_badge"
        )

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = {
                Text(
                    text = hub.essentialsSearchHint,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = HindSiliguriFamily),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            leadingIcon = {
                Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(18.dp))
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Filled.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("essentials_search_input")
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            EssentialGroup.entries.forEach { group ->
                SmallPresetChip(
                    text = group.label(language),
                    selected = selectedGroup == group,
                    onClick = { selectedGroup = group },
                    testTag = "essentials_group_${group.name}"
                )
            }
        }

        if (selectedIds.isNotEmpty()) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = crystal.assetGreen.copy(alpha = if (crystal.isDark) 0.18f else 0.12f),
                border = BorderStroke(0.9.dp, crystal.assetGreen.copy(alpha = 0.4f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("essentials_basket_summary")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${hub.essentialsSelectedTotal} (${selectedIds.size}): ${BdtFormatter.format(basketTotal, language)}",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = HindSiliguriFamily
                        ),
                        color = crystal.assetGreen
                    )
                    TextButton(
                        onClick = { selectedIds = emptySet() },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Text(
                            text = hub.essentialsClearBasket,
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = HindSiliguriFamily)
                        )
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            filtered.take(8).forEach { item ->
                val picked = item.id in selectedIds
                val customPrice = customPrices[item.id]
                val priceLabel = if (customPrice != null) {
                    val cStr = if (language == AppLanguage.BN) MarketCalculators.toBanglaDigits(customPrice.toString()) else customPrice.toString()
                    "৳$cStr"
                } else {
                    val minStr = if (language == AppLanguage.BN) MarketCalculators.toBanglaDigits(item.minPriceBdt.toString()) else item.minPriceBdt.toString()
                    val maxStr = if (language == AppLanguage.BN) MarketCalculators.toBanglaDigits(item.maxPriceBdt.toString()) else item.maxPriceBdt.toString()
                    "৳$minStr–৳$maxStr"
                }
                Surface(
                    onClick = {
                        selectedIds = if (picked) selectedIds - item.id else selectedIds + item.id
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = if (picked) {
                        primary.copy(alpha = if (crystal.isDark) 0.18f else 0.10f)
                    } else if (crystal.isDark) {
                        Color.White.copy(alpha = 0.04f)
                    } else {
                        Color(0xFFF8FAFE)
                    },
                    border = BorderStroke(
                        width = 0.8.dp,
                        color = if (picked) primary.copy(alpha = 0.5f) else crystal.glassBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("essential_item_${item.id}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 12.dp, end = 6.dp, top = 7.dp, bottom = 7.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.name(language),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = HindSiliguriFamily
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = buildString {
                                    append("${item.group.label(language)} • /${item.unit(language)}")
                                    if (customPrice != null) {
                                        append(if (language == AppLanguage.BN) " • কাস্টম" else " • Custom")
                                    }
                                },
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = HindSiliguriFamily),
                                color = if (customPrice != null) crystal.assetGreen else crystal.mutedText
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = priceLabel,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontFamily = JetBrainsMonoFamily,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = if (customPrice != null) crystal.assetGreen else primary
                            )
                            IconButton(
                                onClick = {
                                    editingItem = item
                                    editPriceText = (customPrice ?: item.midPriceBdt.toInt()).toString()
                                },
                                modifier = Modifier
                                    .size(40.dp)
                                    .testTag("essential_edit_price_${item.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Edit,
                                    contentDescription = if (language == AppLanguage.BN) "দাম পরিবর্তন করুন" else "Edit local price",
                                    tint = crystal.mutedText,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            if (picked) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    editingItem?.let { item ->
        AlertDialog(
            onDismissRequest = { editingItem = null },
            title = {
                Text(
                    text = item.name(language),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = HindSiliguriFamily
                    )
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (language == AppLanguage.BN) {
                            "আপনার এলাকার বর্তমান বাজার দর (প্রতি ${item.unit(language)}) লিখুন:"
                        } else {
                            "Enter your local market price (per ${item.unit(language)}):"
                        },
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = HindSiliguriFamily)
                    )
                    OutlinedTextField(
                        value = editPriceText,
                        onValueChange = { editPriceText = it.filter { ch -> ch.isDigit() } },
                        prefix = { Text("৳ ") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("essential_price_dialog_input")
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val v = editPriceText.toIntOrNull()?.takeIf { it > 0 }
                        if (v != null) {
                            customPrices[item.id] = v
                        } else {
                            customPrices.remove(item.id)
                        }
                        editingItem = null
                    },
                    modifier = Modifier.testTag("essential_price_dialog_save")
                ) {
                    Text(if (language == AppLanguage.BN) "সংরক্ষণ" else "Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingItem = null }) {
                    Text(if (language == AppLanguage.BN) "বাতিল" else "Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun BankFdrDpsPanel(
    language: AppLanguage,
    snapshot: MarketSnapshot,
    strings: AppStrings
) {
    val crystal = LocalCrystalColors.current
    val primary = MaterialTheme.colorScheme.primary
    val hub = strings.marketHub

    var isDps by remember { mutableStateOf(false) }
    var amountText by remember { mutableStateOf("100000") }
    var months by remember { mutableIntStateOf(12) }
    var customMonthsText by remember { mutableStateOf("12") }
    var ratePercent by remember { mutableDoubleStateOf(snapshot.avgBankFdrRatePercent) }
    var customRateText by remember { mutableStateOf(String.format(java.util.Locale.US, "%.1f", snapshot.avgBankFdrRatePercent)) }
    var hasTin by remember { mutableStateOf(true) }

    val amount = amountText.toDoubleOrNull()?.takeIf { it > 0.0 } ?: 0.0
    val effectiveMonths = customMonthsText.toIntOrNull()?.coerceIn(1, 360) ?: months
    val effectiveRate = customRateText.toDoubleOrNull()?.coerceIn(0.1, 35.0) ?: ratePercent
    val fdr = MarketCalculators.calculateFdr(amount, effectiveRate, effectiveMonths, hasTin)
    val dps = MarketCalculators.calculateDps(amount, effectiveRate, effectiveMonths, hasTin)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("market_bank_panel"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        MarketSourceTransparencyChip(
            status = MarketDataStatus.REFERENCE_ESTIMATE,
            language = language,
            customLabelBn = "ক্যালকুলেটর আনুমানিক হিসাব • রেট ও মেয়াদ কাস্টমাইজযোগ্য",
            customLabelEn = "Calculator Estimate • Customize interest rate & tenure below",
            testTag = "bank_source_transparency_badge"
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SmallPresetChip(
                text = hub.bankFdrTab,
                selected = !isDps,
                onClick = {
                    isDps = false
                    if (amountText == "5000") amountText = "100000"
                    ratePercent = snapshot.avgBankFdrRatePercent
                    customRateText = String.format(java.util.Locale.US, "%.1f", snapshot.avgBankFdrRatePercent)
                },
                testTag = "bank_mode_fdr"
            )
            SmallPresetChip(
                text = hub.bankDpsTab,
                selected = isDps,
                onClick = {
                    isDps = true
                    if (amountText == "100000") amountText = "5000"
                    ratePercent = snapshot.avgBankDpsRatePercent
                    customRateText = String.format(java.util.Locale.US, "%.1f", snapshot.avgBankDpsRatePercent)
                },
                testTag = "bank_mode_dps"
            )
        }

        OutlinedTextField(
            value = amountText,
            onValueChange = { amountText = it.filter { ch -> ch.isDigit() } },
            label = {
                Text(
                    text = if (isDps) hub.dpsMonthlyLabel else hub.fdrPrincipalLabel,
                    style = MaterialTheme.typography.labelMedium.copy(fontFamily = HindSiliguriFamily)
                )
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("bank_amount_input")
        )

        // Tenure Chips + Custom Months Input
        Text(
            text = hub.tenureLabel,
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = HindSiliguriFamily),
            color = crystal.mutedText
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val tenures = if (isDps) listOf(12, 36, 60, 120) else listOf(6, 12, 24, 36)
            tenures.forEach { m ->
                val label = when {
                    m < 12 -> if (language == AppLanguage.BN) "${MarketCalculators.toBanglaDigits(m.toString())} মাস" else "${m}M"
                    else -> {
                        val y = m / 12
                        if (language == AppLanguage.BN) "${MarketCalculators.toBanglaDigits(y.toString())} বছর" else "${y}Y"
                    }
                }
                SmallPresetChip(
                    text = label,
                    selected = effectiveMonths == m,
                    onClick = {
                        months = m
                        customMonthsText = m.toString()
                    }
                )
            }
        }

        // Rate Chips
        Text(
            text = hub.interestRateLabel,
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = HindSiliguriFamily),
            color = crystal.mutedText
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(8.5, 9.0, 9.5, 10.0, 10.5).forEach { r ->
                val label = "${MarketCalculators.formatDecimal(r, 1, language)}%"
                SmallPresetChip(
                    text = label,
                    selected = kotlin.math.abs(effectiveRate - r) < 0.01,
                    onClick = {
                        ratePercent = r
                        customRateText = String.format(java.util.Locale.US, "%.1f", r)
                    }
                )
            }
        }

        // Custom Tenure (Months) & Custom Interest Rate (%) Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = customMonthsText,
                onValueChange = { raw ->
                    val cleaned = raw.filter { it.isDigit() }.take(3)
                    customMonthsText = cleaned
                    cleaned.toIntOrNull()?.takeIf { it > 0 }?.let { months = it }
                },
                label = {
                    Text(
                        text = if (language == AppLanguage.BN) "কাস্টম মাস" else "Months",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = HindSiliguriFamily)
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("bank_custom_tenure_input")
            )
            OutlinedTextField(
                value = customRateText,
                onValueChange = { raw ->
                    val cleaned = raw.filter { it.isDigit() || it == '.' }
                    if (cleaned.count { it == '.' } <= 1) {
                        customRateText = cleaned
                        cleaned.toDoubleOrNull()?.takeIf { it > 0.0 }?.let { ratePercent = it }
                    }
                },
                label = {
                    Text(
                        text = if (language == AppLanguage.BN) "কাস্টম হার (%)" else "Rate (%)",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = HindSiliguriFamily)
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("bank_custom_rate_input")
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = hub.tinTaxToggleLabel,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = HindSiliguriFamily),
                color = crystal.mutedText,
                modifier = Modifier.weight(1f)
            )
            Switch(checked = hasTin, onCheckedChange = { hasTin = it })
        }

        Surface(
            shape = RoundedCornerShape(14.dp),
            color = if (crystal.isDark) Color(0xFF132238) else Color(0xFFEFF6FF),
            border = BorderStroke(1.dp, primary.copy(alpha = 0.35f)),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("bank_estimate_result_card")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = hub.grossProfitLabel,
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = HindSiliguriFamily),
                        color = crystal.mutedText
                    )
                    Text(
                        text = BdtFormatter.format(if (isDps) dps.grossProfit else fdr.grossProfit, language),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = crystal.assetGreen
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = hub.netMaturityLabel,
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = HindSiliguriFamily),
                        color = crystal.mutedText
                    )
                    Text(
                        text = BdtFormatter.format(if (isDps) dps.maturityAmount else fdr.maturityAmount, language),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.ExtraBold
                        ),
                        color = primary
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun RemittanceCalculatorPanel(
    language: AppLanguage,
    snapshot: MarketSnapshot,
    fromCurrency: MarketCurrency,
    onSelectCurrency: (MarketCurrency) -> Unit,
    onOpenPicker: () -> Unit,
    strings: AppStrings,
    isOnline: Boolean = true
) {
    val crystal = LocalCrystalColors.current
    val hub = strings.marketHub
    val accent = Color(0xFF7C3AED)

    var amountText by remember { mutableStateOf("1") }
    var includeIncentive by remember { mutableStateOf(true) }
    var customRateText by remember(fromCurrency) { mutableStateOf("") }
    var feeBdtText by remember { mutableStateOf("") }

    val amount = amountText.toDoubleOrNull()?.takeIf { it > 0.0 } ?: 1.0
    val customRate = customRateText.toDoubleOrNull()?.takeIf { it > 0.0 }
    val feeBdt = feeBdtText.toDoubleOrNull()?.takeIf { it > 0.0 } ?: 0.0
    val estimate = MarketCalculators.calculateRemittance(
        foreignAmount = amount,
        fromCurrency = fromCurrency,
        snapshot = snapshot,
        includeGovtIncentive = includeIncentive,
        customRateOverride = customRate,
        transferFeeBdt = feeBdt
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("market_remittance_panel"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        MarketSourceTransparencyChip(
            status = snapshot.resolveCurrencyStatus(isOnline),
            language = language,
            sourceLabel = snapshot.sourceLabel,
            timestampMillis = snapshot.lastSyncedMillis,
            testTag = "remittance_source_transparency_badge"
        )

        // Indicative Estimate Disclaimer (explains market estimate before bank/exchange fees)
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = accent.copy(alpha = if (crystal.isDark) 0.14f else 0.07f),
            border = BorderStroke(0.8.dp, accent.copy(alpha = 0.28f)),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("remittance_estimate_disclaimer")
        ) {
            Text(
                text = if (language == AppLanguage.BN) {
                    "আনুমানিক বাজার রেট — ব্যাংক বা এক্সচেঞ্জ হাউসের ফি ও রেট অনুযায়ী চূড়ান্ত টাকা ভিন্ন হতে পারে।"
                } else {
                    "Indicative market estimate before bank or exchange-house fees & spreads. Customize rate/fee below."
                },
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = HindSiliguriFamily,
                    lineHeight = 16.sp
                ),
                color = crystal.mutedText,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
            )
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(
                MarketCurrency.SAR,
                MarketCurrency.AED,
                MarketCurrency.KWD,
                MarketCurrency.QAR,
                MarketCurrency.MYR,
                MarketCurrency.USD,
                MarketCurrency.EUR,
                MarketCurrency.GBP
            ).forEach { cur ->
                SmallPresetChip(
                    text = "${cur.flag} ${cur.code}",
                    selected = fromCurrency == cur,
                    onClick = { onSelectCurrency(cur) }
                )
            }
            SmallPresetChip(
                text = if (language == AppLanguage.BN) "আরও..." else "More...",
                selected = false,
                onClick = onOpenPicker
            )
        }

        OutlinedTextField(
            value = amountText,
            onValueChange = { raw ->
                val cleaned = raw.filter { it.isDigit() || it == '.' }
                if (cleaned.count { it == '.' } <= 1) amountText = cleaned
            },
            label = {
                Text(
                    text = "${hub.remittanceSendCurrency} (${fromCurrency.code})",
                    style = MaterialTheme.typography.labelMedium.copy(fontFamily = HindSiliguriFamily)
                )
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("remittance_amount_input")
        )

        // Optional Exchange House Rate Override + Transfer Fee
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = customRateText,
                onValueChange = { raw ->
                    val cleaned = raw.filter { it.isDigit() || it == '.' }
                    if (cleaned.count { it == '.' } <= 1) customRateText = cleaned
                },
                label = {
                    Text(
                        text = if (language == AppLanguage.BN) "কাস্টম রেট (৳)" else "Custom Rate (৳)",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = HindSiliguriFamily)
                    )
                },
                placeholder = {
                    Text(
                        text = MarketCalculators.formatDecimal(snapshot.convert(1.0, fromCurrency, MarketCurrency.BDT), 2, language),
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFamily)
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("remittance_custom_rate_input")
            )
            OutlinedTextField(
                value = feeBdtText,
                onValueChange = { raw ->
                    val cleaned = raw.filter { it.isDigit() || it == '.' }
                    if (cleaned.count { it == '.' } <= 1) feeBdtText = cleaned
                },
                label = {
                    Text(
                        text = if (language == AppLanguage.BN) "এক্সচেঞ্জ ফি (৳)" else "Fee (BDT)",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = HindSiliguriFamily)
                    )
                },
                placeholder = { Text("0") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("remittance_fee_input")
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = hub.remittanceIncentiveToggle,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = HindSiliguriFamily),
                color = crystal.mutedText,
                modifier = Modifier.weight(1f)
            )
            Switch(checked = includeIncentive, onCheckedChange = { includeIncentive = it })
        }

        Surface(
            shape = RoundedCornerShape(14.dp),
            color = accent.copy(alpha = if (crystal.isDark) 0.18f else 0.09f),
            border = BorderStroke(1.dp, accent.copy(alpha = 0.36f)),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("remittance_estimate_result_card")
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = hub.remittanceBaseLabel,
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = HindSiliguriFamily),
                        color = crystal.mutedText
                    )
                    Text(
                        text = BdtFormatter.format(estimate.baseBdt, language),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                if (includeIncentive) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = hub.remittanceIncentiveBonus,
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = HindSiliguriFamily),
                            color = crystal.assetGreen
                        )
                        Text(
                            text = "+${BdtFormatter.format(estimate.incentiveBdt, language)}",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontFamily = JetBrainsMonoFamily,
                                fontWeight = FontWeight.Bold
                            ),
                            color = crystal.assetGreen
                        )
                    }
                }
                if (estimate.feeBdt > 0.0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (language == AppLanguage.BN) "এক্সচেঞ্জ / ব্যাংক ফি" else "Transfer / Exchange Fee",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = HindSiliguriFamily),
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = "-${BdtFormatter.format(estimate.feeBdt, language)}",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontFamily = JetBrainsMonoFamily,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                HorizontalDivider(thickness = 0.6.dp, color = crystal.glassBorder)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = hub.remittanceTotalReceivable,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = HindSiliguriFamily
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = BdtFormatter.format(estimate.totalReceivableBdt, language),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.ExtraBold
                        ),
                        color = accent
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun MfsFeeCalculatorPanel(
    language: AppLanguage,
    strings: AppStrings
) {
    val crystal = LocalCrystalColors.current
    val hub = strings.marketHub
    val accent = Color(0xFFE11D48)

    var selectedChannel by remember { mutableStateOf(MfsChannel.BKASH_APP) }
    var amountText by remember { mutableStateOf("1000") }

    val amount = amountText.toDoubleOrNull()?.takeIf { it > 0.0 } ?: 0.0
    val feeEst = MarketCalculators.calculateMfsCashOut(amount, selectedChannel)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("market_mfs_panel"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        MarketSourceTransparencyChip(
            status = MarketDataStatus.REFERENCE_ESTIMATE,
            language = language,
            customLabelBn = "রেফারেন্স ক্যাশ-আউট চার্জ ক্যালকুলেটর",
            customLabelEn = "Reference Cash-Out Tariff Calculator",
            testTag = "mfs_source_transparency_badge"
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            MfsChannel.entries.forEach { channel ->
                SmallPresetChip(
                    text = "${channel.label(language)} (${MarketCalculators.formatDecimal(channel.ratePercent, 2, language)}%)",
                    selected = selectedChannel == channel,
                    onClick = { selectedChannel = channel },
                    testTag = "mfs_channel_${channel.name}"
                )
            }
        }

        OutlinedTextField(
            value = amountText,
            onValueChange = { amountText = it.filter { ch -> ch.isDigit() } },
            label = {
                Text(
                    text = hub.mfsAmountLabel,
                    style = MaterialTheme.typography.labelMedium.copy(fontFamily = HindSiliguriFamily)
                )
            },
            supportingText = {
                Text(
                    text = selectedChannel.note(language),
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = HindSiliguriFamily)
                )
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("mfs_amount_input")
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("500", "1000", "5000", "10000").forEach { preset ->
                SmallPresetChip(
                    text = "৳${if (language == AppLanguage.BN) MarketCalculators.toBanglaDigits(preset) else preset}",
                    selected = amountText == preset,
                    onClick = { amountText = preset }
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(14.dp),
            color = accent.copy(alpha = if (crystal.isDark) 0.16f else 0.08f),
            border = BorderStroke(1.dp, accent.copy(alpha = 0.34f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = hub.mfsChargeLabel,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFamily = HindSiliguriFamily,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "৳${MarketCalculators.formatDecimal(feeEst.feeBdt, 2, language)}",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.ExtraBold
                        ),
                        color = accent
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = hub.mfsTotalDeductedLabel,
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = HindSiliguriFamily),
                        color = crystal.mutedText
                    )
                    Text(
                        text = "৳${MarketCalculators.formatDecimal(feeEst.totalDeductedBdt, 2, language)}",
                        style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFamily),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = hub.mfsNetReceivedLabel,
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = HindSiliguriFamily),
                        color = crystal.mutedText
                    )
                    Text(
                        text = "৳${MarketCalculators.formatDecimal(feeEst.netReceivedBdt, 2, language)}",
                        style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFamily),
                        color = crystal.assetGreen
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CurrencyPickerModalSheet(
    language: AppLanguage,
    snapshot: MarketSnapshot,
    selected: MarketCurrency,
    strings: AppStrings,
    onSelect: (MarketCurrency) -> Unit,
    onDismiss: () -> Unit
) {
    val crystal = LocalCrystalColors.current
    val primary = MaterialTheme.colorScheme.primary
    val hub = strings.marketHub
    var query by remember { mutableStateOf("") }

    val filtered = remember(query) {
        val q = query.trim()
        if (q.isEmpty()) {
            MarketCurrency.entries
        } else {
            MarketCurrency.entries.filter { cur ->
                cur.code.contains(q, ignoreCase = true) ||
                    cur.nameEn.contains(q, ignoreCase = true) ||
                    cur.nameBn.contains(q, ignoreCase = true) ||
                    cur.countryEn.contains(q, ignoreCase = true) ||
                    cur.countryBn.contains(q, ignoreCase = true)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("currency_picker_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = hub.pickCurrencyTitle,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = HindSiliguriFamily
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Close")
                }
            }

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = {
                    Text(
                        text = hub.searchCurrencyHint,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = HindSiliguriFamily),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("currency_picker_search")
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(filtered, key = { it.code }) { currency ->
                    val isSelected = currency == selected
                    val bdtPerUnit = snapshot.convert(1.0, currency, MarketCurrency.BDT)
                    Surface(
                        onClick = { onSelect(currency) },
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) {
                            primary.copy(alpha = if (crystal.isDark) 0.22f else 0.12f)
                        } else if (crystal.isDark) {
                            Color.White.copy(alpha = 0.04f)
                        } else {
                            Color(0xFFF6F9FD)
                        },
                        border = BorderStroke(
                            width = if (isSelected) 1.2.dp else 0.8.dp,
                            color = if (isSelected) primary else crystal.glassBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("currency_option_${currency.code}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(text = currency.flag, fontSize = 22.sp)
                                Column {
                                    Text(
                                        text = "${currency.code} • ${currency.displayName(language)}",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = HindSiliguriFamily
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = currency.countryName(language),
                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = HindSiliguriFamily),
                                        color = crystal.mutedText
                                    )
                                }
                            }
                            Text(
                                text = "৳${MarketCalculators.formatDecimal(bdtPerUnit, 2, language)}",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontFamily = JetBrainsMonoFamily,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun SmallPresetChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String? = null
) {
    val crystal = LocalCrystalColors.current
    val primary = MaterialTheme.colorScheme.primary
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (selected) {
            primary.copy(alpha = if (crystal.isDark) 0.26f else 0.15f)
        } else if (crystal.isDark) {
            Color.White.copy(alpha = 0.06f)
        } else {
            Color(0xFFF3F7FC)
        },
        border = BorderStroke(
            width = if (selected) 1.1.dp else 0.8.dp,
            color = if (selected) primary.copy(alpha = 0.65f) else crystal.glassBorder
        ),
        modifier = Modifier
            .heightIn(min = 36.dp)
            .let { if (testTag != null) it.testTag(testTag) else it }
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    fontFamily = HindSiliguriFamily
                ),
                color = if (selected) primary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}
