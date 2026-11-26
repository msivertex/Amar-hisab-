package com.example.ui.screens

import android.app.Activity
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.export.ExportSharePayload
import com.example.data.export.MonthlyReportExporter
import com.example.data.export.ReportExportFormat
import com.example.data.export.ReportPeriodType
import com.example.data.market.MarketSnapshot
import com.example.data.market.MarketSyncUiState
import com.example.data.model.AppCurrency
import com.example.data.model.CategorySpend
import com.example.data.model.DailyTrendPoint
import com.example.data.model.InsightTimeFilter
import com.example.localization.AppLanguage
import com.example.localization.AppStrings
import com.example.localization.BdtFormatter
import com.example.ui.components.CrystalCard
import com.example.ui.components.CurrencyAmountText
import com.example.ui.theme.HindSiliguriFamily
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalCrystalColors
import com.example.ui.viewmodel.MainUiState
import com.example.ui.viewmodel.SearchAiUiState
import com.example.ui.viewmodel.TtsUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun InsightsScreen(
    state: MainUiState,
    ttsState: TtsUiState,
    searchAiState: SearchAiUiState,
    strings: AppStrings,
    onSelectFilter: (InsightTimeFilter) -> Unit,
    onShiftMonth: (Int) -> Unit,
    onPlayBriefingTts: () -> Unit,
    onStopTts: () -> Unit,
    onSearchMarket: (String) -> Unit,
    onPauseTts: () -> Unit = {},
    onResumeTts: () -> Unit = {},
    onShareExportPayload: ((ExportSharePayload) -> Unit)? = null,
    marketSyncState: MarketSyncUiState = MarketSyncUiState(),
    onRefreshMarketRates: () -> Unit = {}
) {
    val crystal = LocalCrystalColors.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lang = state.prefs.language
    val analytics = state.analytics
    val mainCurrencyCode = state.prefs.mainCurrencyCode
    val marketSnapshot = marketSyncState.snapshot

    val secondaryCurrencyCode = remember(
        mainCurrencyCode,
        state.prefs.secondaryCurrencyCode,
        state.liquidBalancesByCurrency,
        state.accounts
    ) {
        AppCurrency.fromCodeOrNull(state.prefs.secondaryCurrencyCode)
            ?.code
            ?.takeIf { !it.equals(mainCurrencyCode, ignoreCase = true) }
            ?: state.liquidBalancesByCurrency.keys
                .firstOrNull { !it.equals(mainCurrencyCode, ignoreCase = true) }
            ?: state.accounts
                .map { it.currencyCode }
                .firstOrNull { !it.equals(mainCurrencyCode, ignoreCase = true) }
    }

    var showExportSheet by remember { mutableStateOf(false) }
    var selectedExportPeriod by remember { mutableStateOf(ReportPeriodType.MONTHLY) }
    var exportingFormat by remember { mutableStateOf<ReportExportFormat?>(null) }
    var exportStatusMessage by remember { mutableStateOf<String?>(null) }
    var exportStatusIsError by remember { mutableStateOf(false) }

    val monthlyReportPayload = remember(
        state.transactions,
        state.accounts,
        state.insightFilter,
        state.insightMonthOffset,
        state.prefs.language,
        state.prefs.userName,
        state.prefs.monthlyBudget,
        state.prefs.mainCurrencyCode,
        state.prefs.startOfWeek,
        marketSyncState.snapshot,
        selectedExportPeriod
    ) {
        MonthlyReportExporter.buildReportPayload(
            state = state,
            periodType = selectedExportPeriod,
            marketSnapshot = marketSyncState.snapshot
        )
    }

    fun triggerExport(format: ReportExportFormat) {
        if (exportingFormat != null) return
        exportingFormat = format
        exportStatusMessage = null
        exportStatusIsError = false
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                MonthlyReportExporter.exportAndBuildShareIntent(
                    context = context,
                    state = state,
                    format = format,
                    strings = strings,
                    periodType = selectedExportPeriod,
                    marketSnapshot = marketSyncState.snapshot
                )
            }
            exportingFormat = null
            result.onSuccess { sharePayload ->
                showExportSheet = false
                exportStatusIsError = false
                exportStatusMessage = strings.reportExport.exportReadyNotice
                if (onShareExportPayload != null) {
                    onShareExportPayload(sharePayload)
                } else {
                    runCatching {
                        val chooser = Intent.createChooser(
                            sharePayload.shareIntent,
                            sharePayload.chooserTitle
                        ).apply {
                            if (context !is Activity) {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                        }
                        context.startActivity(chooser)
                    }.onFailure {
                        exportStatusIsError = true
                        exportStatusMessage = strings.reportExport.exportFailedNotice
                    }
                }
            }.onFailure {
                exportStatusIsError = true
                exportStatusMessage = strings.reportExport.exportFailedNotice
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("insights_screen"),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 620.dp)
                .fillMaxSize()
                .testTag("insights_list"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 112.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.insightsTitle,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = HindSiliguriFamily
                        ),
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 2,
                        softWrap = true,
                        overflow = TextOverflow.Clip,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    InsightsExportHeaderButton(
                        label = strings.reportExport.headerExportBtn,
                        isBusy = exportingFormat != null,
                        onClick = { showExportSheet = true }
                    )
                }
            }

            // Time Filters + Period Navigation
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        InsightTimeFilter.entries.forEach { filter ->
                            FilterChip(
                                selected = state.insightFilter == filter,
                                onClick = { onSelectFilter(filter) },
                                label = {
                                    Text(
                                        text = filter.label(lang),
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontFamily = HindSiliguriFamily
                                        )
                                    )
                                },
                                modifier = Modifier.testTag("insight_filter_${filter.name}")
                            )
                        }
                    }

                    CrystalCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { onShiftMonth(-1) },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous Month")
                            }
                            Text(
                                text = analytics.periodLabel,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = HindSiliguriFamily
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            IconButton(
                                onClick = { onShiftMonth(1) },
                                enabled = state.insightMonthOffset < 0,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next Month")
                            }
                        }
                    }
                }
            }

            // Overview: Income & Expenses side-by-side + Full-width Net Flow (full non-truncated dual-currency figures)
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("insights_overview_metrics"),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Max),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        InsightSummaryMetricCard(
                            label = strings.incomeLabel,
                            amountInMain = analytics.totalIncome,
                            mainCurrencyCode = mainCurrencyCode,
                            secondaryCurrencyCode = secondaryCurrencyCode,
                            marketSnapshot = marketSnapshot,
                            language = lang,
                            icon = Icons.AutoMirrored.Filled.TrendingUp,
                            accentColor = crystal.assetGreen,
                            cardTestTag = "insights_income_card",
                            primaryTag = "insights_income_primary_amount",
                            secondaryTag = "insights_income_secondary_amount",
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                        InsightSummaryMetricCard(
                            label = strings.expenseLabel,
                            amountInMain = analytics.totalExpense,
                            mainCurrencyCode = mainCurrencyCode,
                            secondaryCurrencyCode = secondaryCurrencyCode,
                            marketSnapshot = marketSnapshot,
                            language = lang,
                            icon = Icons.AutoMirrored.Filled.TrendingDown,
                            accentColor = crystal.liabilityRed,
                            cardTestTag = "insights_expense_card",
                            primaryTag = "insights_expense_primary_amount",
                            secondaryTag = "insights_expense_secondary_amount",
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                    }

                    InsightSummaryMetricCard(
                        label = strings.netFlowLabel,
                        amountInMain = analytics.netFlow,
                        mainCurrencyCode = mainCurrencyCode,
                        secondaryCurrencyCode = secondaryCurrencyCode,
                        marketSnapshot = marketSnapshot,
                        language = lang,
                        icon = Icons.Filled.AccountBalanceWallet,
                        accentColor = crystal.assetGreen,
                        cardTestTag = "insights_net_flow_card",
                        primaryTag = "insights_net_flow_primary_amount",
                        secondaryTag = "insights_net_flow_secondary_amount",
                        primaryFontSize = 18.sp,
                        secondaryFontSize = 14.5.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Smart High-Value Insight Highlights (period-over-period change, top expense share, savings rate, dual-currency daily avg)
            item {
                SmartInsightsHighlightStrip(
                    analytics = analytics,
                    language = lang,
                    strings = strings,
                    mainCurrencyCode = mainCurrencyCode,
                    secondaryCurrencyCode = secondaryCurrencyCode,
                    marketSnapshot = marketSnapshot
                )
            }

            // Interactive Spending Trend Line Chart (dual-currency Daily Avg & Peak Spending without truncation)
            item {
                SpendingTrendLineCard(
                    points = analytics.trendPoints,
                    dailyAverage = analytics.dailyAverage,
                    peakAmount = analytics.peakAmount,
                    language = lang,
                    strings = strings,
                    currencyCode = mainCurrencyCode,
                    secondaryCurrencyCode = secondaryCurrencyCode,
                    marketSnapshot = marketSnapshot
                )
            }

            // Category Donut Breakdown (dual-currency total spent without truncation)
            item {
                SpendingDonutCard(
                    breakdown = analytics.categoryBreakdown,
                    totalSpent = analytics.totalExpense,
                    language = lang,
                    strings = strings,
                    currencyCode = mainCurrencyCode,
                    secondaryCurrencyCode = secondaryCurrencyCode,
                    marketSnapshot = marketSnapshot
                )
            }

            // Daily / Weekly / Monthly Financial Receipt Export (PDF / CSV / QR via Share Intent)
            item {
                MonthlyReportExportCard(
                    payload = monthlyReportPayload,
                    strings = strings,
                    exportingFormat = exportingFormat,
                    statusMessage = exportStatusMessage,
                    isError = exportStatusIsError,
                    onQuickExport = { format -> triggerExport(format) },
                    onOpenExportSheet = { showExportSheet = true },
                    selectedPeriodType = selectedExportPeriod,
                    onSelectPeriodType = { selectedExportPeriod = it }
                )
            }

            // Modernized Voice Financial Summary (Play / Pause / Stop, Waveform, Collapsible Bangla Text Preview)
            item {
                VoiceFinancialSummaryCard(
                    state = state,
                    ttsState = ttsState,
                    strings = strings,
                    onPlayBriefingTts = onPlayBriefingTts,
                    onPauseTts = onPauseTts,
                    onResumeTts = onResumeTts,
                    onStopTts = onStopTts
                )
            }
        }
    }

    if (showExportSheet) {
        MonthlyReportExportSheet(
            payload = monthlyReportPayload,
            strings = strings,
            exportingFormat = exportingFormat,
            onExport = { format -> triggerExport(format) },
            onDismiss = { if (exportingFormat == null) showExportSheet = false },
            selectedPeriodType = selectedExportPeriod,
            onSelectPeriodType = { selectedExportPeriod = it }
        )
    }
}

@Composable
private fun InsightSummaryMetricCard(
    label: String,
    amountInMain: Double,
    mainCurrencyCode: String,
    secondaryCurrencyCode: String?,
    marketSnapshot: MarketSnapshot,
    language: AppLanguage,
    icon: ImageVector,
    accentColor: Color,
    cardTestTag: String,
    primaryTag: String,
    secondaryTag: String,
    primaryFontSize: TextUnit = 16.5.sp,
    secondaryFontSize: TextUnit = 13.5.sp,
    modifier: Modifier = Modifier
) {
    val crystal = LocalCrystalColors.current
    val secondaryAmount = remember(amountInMain, mainCurrencyCode, secondaryCurrencyCode, marketSnapshot) {
        if (secondaryCurrencyCode != null) {
            marketSnapshot.convertCurrencyCode(amountInMain, mainCurrencyCode, secondaryCurrencyCode)
        } else {
            null
        }
    }

    CrystalCard(
        modifier = modifier.testTag(cardTestTag),
        elevated = true
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(accentColor.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(15.dp)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontFamily = HindSiliguriFamily,
                    fontWeight = FontWeight.Medium
                ),
                color = crystal.mutedText,
                maxLines = 2,
                softWrap = true,
                overflow = TextOverflow.Clip,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Main Currency (top line, full non-truncated figure)
        CurrencyAmountText(
            amount = amountInMain,
            currencyCode = mainCurrencyCode,
            language = language,
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = JetBrainsMonoFamily,
                fontSize = primaryFontSize,
                fontWeight = FontWeight.ExtraBold
            ),
            isBalance = true,
            colorOverride = accentColor,
            bdtFontScale = 0.88f,
            minFontSize = 10.sp,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(primaryTag)
        )

        // Second Currency (stacked directly below Main Currency)
        if (secondaryCurrencyCode != null && secondaryAmount != null) {
            Spacer(modifier = Modifier.height(4.dp))
            CurrencyAmountText(
                amount = secondaryAmount,
                currencyCode = secondaryCurrencyCode,
                language = language,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontFamily = JetBrainsMonoFamily,
                    fontSize = secondaryFontSize,
                    fontWeight = FontWeight.Bold
                ),
                isBalance = true,
                colorOverride = crystal.kwdNumericInk,
                bdtFontScale = 0.88f,
                minFontSize = 9.5.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(secondaryTag)
            )
        }
    }
}

@Composable
private fun SpendingTrendLineCard(
    points: List<DailyTrendPoint>,
    dailyAverage: Double,
    peakAmount: Double,
    language: AppLanguage,
    strings: AppStrings,
    currencyCode: String = "BDT",
    secondaryCurrencyCode: String? = null,
    marketSnapshot: MarketSnapshot = MarketSnapshot.defaultReference()
) {
    val crystal = LocalCrystalColors.current
    var selectedIdx by remember(points) { mutableIntStateOf(-1) }
    val activePoint = points.getOrNull(selectedIdx)
    val secondaryDailyAvg = remember(dailyAverage, currencyCode, secondaryCurrencyCode, marketSnapshot) {
        secondaryCurrencyCode?.let { marketSnapshot.convertCurrencyCode(dailyAverage, currencyCode, it) }
    }
    val secondaryPeak = remember(peakAmount, currencyCode, secondaryCurrencyCode, marketSnapshot) {
        secondaryCurrencyCode?.let { marketSnapshot.convertCurrencyCode(peakAmount, currencyCode, it) }
    }

    CrystalCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = strings.spendingTrendTitle,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontFamily = HindSiliguriFamily
            ),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            softWrap = true,
            overflow = TextOverflow.Clip
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Equal-height Daily Average & Peak Spending dual-currency strip (no ellipsis clipping)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Max),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = if (crystal.isDark) 0.14f else 0.08f),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.24f)),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .testTag("insights_trend_daily_avg_box")
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = strings.dailyAverageLabel,
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = HindSiliguriFamily),
                        color = crystal.mutedText,
                        maxLines = 2,
                        softWrap = true,
                        overflow = TextOverflow.Clip
                    )
                    CurrencyAmountText(
                        amount = dailyAverage,
                        currencyCode = currencyCode,
                        language = language,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontFamily = JetBrainsMonoFamily,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        isBalance = true,
                        colorOverride = MaterialTheme.colorScheme.onSurface,
                        bdtFontScale = 0.90f,
                        minFontSize = 9.5.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("insights_trend_daily_avg_primary")
                    )
                    if (secondaryCurrencyCode != null && secondaryDailyAvg != null) {
                        CurrencyAmountText(
                            amount = secondaryDailyAvg,
                            currencyCode = secondaryCurrencyCode,
                            language = language,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontFamily = JetBrainsMonoFamily,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            isBalance = true,
                            colorOverride = crystal.kwdNumericInk,
                            bdtFontScale = 0.90f,
                            minFontSize = 9.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("insights_trend_daily_avg_secondary")
                        )
                    }
                }
            }

            Surface(
                color = crystal.warningAmberBg,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(0.8.dp, crystal.warningAmber.copy(alpha = 0.30f)),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .testTag("insights_trend_peak_box")
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = strings.peakSpendingLabel,
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = HindSiliguriFamily),
                        color = crystal.mutedText,
                        maxLines = 2,
                        softWrap = true,
                        overflow = TextOverflow.Clip
                    )
                    CurrencyAmountText(
                        amount = peakAmount,
                        currencyCode = currencyCode,
                        language = language,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontFamily = JetBrainsMonoFamily,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        isBalance = true,
                        colorOverride = crystal.warningAmber,
                        bdtFontScale = 0.90f,
                        minFontSize = 9.5.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("insights_trend_peak_primary")
                    )
                    if (secondaryCurrencyCode != null && secondaryPeak != null) {
                        CurrencyAmountText(
                            amount = secondaryPeak,
                            currencyCode = secondaryCurrencyCode,
                            language = language,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontFamily = JetBrainsMonoFamily,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            isBalance = true,
                            colorOverride = crystal.kwdNumericInk,
                            bdtFontScale = 0.90f,
                            minFontSize = 9.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("insights_trend_peak_secondary")
                        )
                    }
                }
            }
        }

        if (activePoint != null) {
            Spacer(modifier = Modifier.height(8.dp))
            val secPointAmt = secondaryCurrencyCode?.let {
                marketSnapshot.convertCurrencyCode(activePoint.amount, currencyCode, it)
            }
            Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = if (crystal.isDark) 0.16f else 0.08f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("insights_trend_selected_point")
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = activePoint.dayLabel,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = HindSiliguriFamily,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = crystal.mutedText
                    )
                    CurrencyAmountText(
                        amount = activePoint.amount,
                        currencyCode = currencyCode,
                        language = language,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontFamily = JetBrainsMonoFamily,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        isBalance = true,
                        colorOverride = MaterialTheme.colorScheme.primary,
                        bdtFontScale = 0.90f,
                        minFontSize = 10.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (secondaryCurrencyCode != null && secPointAmt != null) {
                        CurrencyAmountText(
                            amount = secPointAmt,
                            currencyCode = secondaryCurrencyCode,
                            language = language,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontFamily = JetBrainsMonoFamily,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            isBalance = true,
                            colorOverride = crystal.kwdNumericInk,
                            bdtFontScale = 0.90f,
                            minFontSize = 9.5.sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        if (points.isEmpty()) {
            Text(
                text = strings.noTransactionsSubtitle,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = HindSiliguriFamily),
                color = crystal.mutedText
            )
            return@CrystalCard
        }

        val maxVal = maxOf(peakAmount, dailyAverage, 1.0)
        val lineColor = MaterialTheme.colorScheme.primary
        val fillTopColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.34f)
        val peakColor = crystal.warningAmber
        val avgColor = crystal.assetGreen

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(146.dp)
                .pointerInput(points) {
                    detectTapGestures { tapOffset ->
                        if (points.isNotEmpty()) {
                            val stepX = if (points.size > 1) size.width.toFloat() / (points.size - 1) else size.width.toFloat()
                            val idx = (tapOffset.x / stepX).toInt().coerceIn(0, points.lastIndex)
                            selectedIdx = idx
                        }
                    }
                }
        ) {
            val padTop = 14f
            val padBottom = 14f
            val chartH = (size.height - padTop - padBottom).coerceAtLeast(10f)
            val stepX = if (points.size > 1) size.width / (points.size - 1) else size.width

            // Dashed Daily Average Line
            val avgY = padTop + chartH * (1f - (dailyAverage / maxVal).toFloat().coerceIn(0f, 1f))
            drawLine(
                color = avgColor.copy(alpha = 0.7f),
                start = Offset(0f, avgY),
                end = Offset(size.width, avgY),
                strokeWidth = 3f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f))
            )

            val coords = points.mapIndexed { i, pt ->
                val x = if (points.size == 1) size.width / 2f else i * stepX
                val y = padTop + chartH * (1f - (pt.amount / maxVal).toFloat().coerceIn(0f, 1f))
                Offset(x, y)
            }

            if (coords.size >= 2) {
                val strokePath = Path().apply {
                    moveTo(coords.first().x, coords.first().y)
                    for (i in 1 until coords.size) {
                        val prev = coords[i - 1]
                        val curr = coords[i]
                        val midX = (prev.x + curr.x) / 2f
                        cubicTo(midX, prev.y, midX, curr.y, curr.x, curr.y)
                    }
                }
                val fillPath = Path().apply {
                    addPath(strokePath)
                    lineTo(coords.last().x, size.height)
                    lineTo(coords.first().x, size.height)
                    close()
                }
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        listOf(fillTopColor, Color.Transparent)
                    )
                )
                drawPath(
                    path = strokePath,
                    color = lineColor,
                    style = Stroke(width = 6f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }

            coords.forEachIndexed { idx, ptOffset ->
                val isPeak = points[idx].isPeak
                val isSelected = idx == selectedIdx
                val radius = when {
                    isSelected -> 12f
                    isPeak -> 10f
                    else -> 6f
                }
                drawCircle(
                    color = if (isPeak) peakColor else lineColor,
                    radius = radius,
                    center = ptOffset
                )
                drawCircle(
                    color = Color.White,
                    radius = radius * 0.45f,
                    center = ptOffset
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            points.forEach { pt ->
                Text(
                    text = pt.dayLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (pt.isPeak) crystal.warningAmber else crystal.mutedText
                )
            }
        }
    }
}

@Composable
private fun SpendingDonutCard(
    breakdown: List<CategorySpend>,
    totalSpent: Double,
    language: AppLanguage,
    strings: AppStrings,
    currencyCode: String = "BDT",
    secondaryCurrencyCode: String? = null,
    marketSnapshot: MarketSnapshot = MarketSnapshot.defaultReference()
) {
    val crystal = LocalCrystalColors.current
    val secondaryTotalSpent = remember(totalSpent, currencyCode, secondaryCurrencyCode, marketSnapshot) {
        secondaryCurrencyCode?.let { marketSnapshot.convertCurrencyCode(totalSpent, currencyCode, it) }
    }

    CrystalCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = strings.spendingByCategory,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontFamily = HindSiliguriFamily
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(14.dp))

        if (breakdown.isEmpty()) {
            Text(
                text = strings.noTransactionsSubtitle,
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = HindSiliguriFamily),
                color = crystal.mutedText
            )
            return@CrystalCard
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier.size(144.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                    var startAngle = -90f
                    breakdown.forEach { item ->
                        val sweep = (item.share * 360f).coerceAtLeast(4f)
                        drawArc(
                            color = item.category.color,
                            startAngle = startAngle,
                            sweepAngle = (sweep - 3f).coerceAtLeast(2f),
                            useCenter = false,
                            style = Stroke(width = 22f, cap = StrokeCap.Round)
                        )
                        startAngle += sweep
                    }
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(1.dp),
                    modifier = Modifier.padding(horizontal = 18.dp)
                ) {
                    Text(
                        text = strings.spentLabel,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = HindSiliguriFamily,
                            fontSize = 10.sp
                        ),
                        color = crystal.mutedText
                    )
                    CurrencyAmountText(
                        amount = totalSpent,
                        currencyCode = currencyCode,
                        language = language,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontFamily = JetBrainsMonoFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        isBalance = true,
                        colorOverride = MaterialTheme.colorScheme.onSurface,
                        bdtFontScale = 0.88f,
                        minFontSize = 8.5.sp,
                        modifier = Modifier.testTag("insights_donut_primary_amount")
                    )
                    if (secondaryCurrencyCode != null && secondaryTotalSpent != null) {
                        CurrencyAmountText(
                            amount = secondaryTotalSpent,
                            currencyCode = secondaryCurrencyCode,
                            language = language,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = JetBrainsMonoFamily,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            isBalance = true,
                            colorOverride = crystal.kwdNumericInk,
                            bdtFontScale = 0.88f,
                            minFontSize = 8.sp,
                            modifier = Modifier.testTag("insights_donut_secondary_amount")
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                breakdown.take(5).forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(9.dp)
                                    .clip(CircleShape)
                                    .background(item.category.color)
                            )
                            Text(
                                text = item.category.label(language),
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = HindSiliguriFamily),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                                softWrap = true,
                                overflow = TextOverflow.Clip
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = BdtFormatter.formatPercent((item.share * 100).toInt(), language),
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                            color = crystal.mutedText,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }
}
