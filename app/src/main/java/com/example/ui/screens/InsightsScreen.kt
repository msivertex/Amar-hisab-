package com.example.ui.screens

import android.app.Activity
import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.export.ExportSharePayload
import com.example.data.export.MonthlyReportExporter
import com.example.data.export.ReportExportFormat
import com.example.data.market.MarketSyncUiState
import com.example.data.market.MarketToolTab
import com.example.data.model.CategorySpend
import com.example.data.model.DailyTrendPoint
import com.example.data.model.InsightTimeFilter
import com.example.localization.AppLanguage
import com.example.localization.AppStrings
import com.example.localization.BdtFormatter
import com.example.ui.components.CrystalCard
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

    var showExportSheet by remember { mutableStateOf(false) }
    var exportingFormat by remember { mutableStateOf<ReportExportFormat?>(null) }
    var exportStatusMessage by remember { mutableStateOf<String?>(null) }
    var exportStatusIsError by remember { mutableStateOf(false) }
    var selectedMarketToolTab by remember { mutableStateOf(MarketToolTab.CURRENCY) }

    val monthlyReportPayload = remember(
        state.transactions,
        state.accounts,
        state.insightFilter,
        state.insightMonthOffset,
        state.prefs.language,
        state.prefs.userName,
        state.prefs.monthlyBudget
    ) {
        MonthlyReportExporter.buildMonthlyPayload(state)
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
                    strings = strings
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
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
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

            // Overview: Income, Expenses, Net Flow
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CrystalCard(modifier = Modifier.weight(1f)) {
                        Text(
                            text = strings.incomeLabel,
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = HindSiliguriFamily),
                            color = crystal.mutedText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = BdtFormatter.format(analytics.totalIncome, lang),
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontFamily = JetBrainsMonoFamily,
                                fontWeight = FontWeight.Bold
                            ),
                            color = crystal.assetGreen,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    CrystalCard(modifier = Modifier.weight(1f)) {
                        Text(
                            text = strings.expenseLabel,
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = HindSiliguriFamily),
                            color = crystal.mutedText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = BdtFormatter.format(analytics.totalExpense, lang),
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontFamily = JetBrainsMonoFamily,
                                fontWeight = FontWeight.Bold
                            ),
                            color = crystal.liabilityRed,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    CrystalCard(modifier = Modifier.weight(1f)) {
                        Text(
                            text = strings.netFlowLabel,
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = HindSiliguriFamily),
                            color = crystal.mutedText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = BdtFormatter.format(analytics.netFlow, lang, includeSign = true),
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontFamily = JetBrainsMonoFamily,
                                fontWeight = FontWeight.Bold
                            ),
                            color = if (analytics.netFlow >= 0) crystal.assetGreen else crystal.liabilityRed,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Smart High-Value Insight Highlights (period-over-period change, top expense share, savings rate)
            item {
                SmartInsightsHighlightStrip(
                    analytics = analytics,
                    language = lang,
                    strings = strings
                )
            }

            // Interactive Spending Trend Line Chart (preserved layout)
            item {
                SpendingTrendLineCard(
                    points = analytics.trendPoints,
                    dailyAverage = analytics.dailyAverage,
                    peakAmount = analytics.peakAmount,
                    language = lang,
                    strings = strings
                )
            }

            // Category Donut Breakdown (preserved layout)
            item {
                SpendingDonutCard(
                    breakdown = analytics.categoryBreakdown,
                    totalSpent = analytics.totalExpense,
                    language = lang,
                    strings = strings
                )
            }

            // Monthly Financial Summary Export (PDF / CSV via Share Intent)
            item {
                MonthlyReportExportCard(
                    payload = monthlyReportPayload,
                    strings = strings,
                    exportingFormat = exportingFormat,
                    statusMessage = exportStatusMessage,
                    isError = exportStatusIsError,
                    onQuickExport = { format -> triggerExport(format) },
                    onOpenExportSheet = { showExportSheet = true }
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

            // Upgraded Live Market & Rates (Connectivity badge, smart offline notice, wrapping quick chips, aligned search bar, graceful fallback)
            item {
                LiveMarketRatesCard(
                    language = lang,
                    searchAiState = searchAiState,
                    strings = strings,
                    onSearchMarket = onSearchMarket,
                    marketSyncState = marketSyncState,
                    onSyncMarketRates = onRefreshMarketRates,
                    onSelectToolTab = { selectedMarketToolTab = it }
                )
            }

            // Smart Interactive Market Tools (15-Currency Converter starting at 1, Gold Carats, Essentials, FDR/DPS, Remittance, bKash/Nagad Fee)
            item {
                MarketInteractiveToolsCard(
                    language = lang,
                    marketSyncState = marketSyncState,
                    selectedTab = selectedMarketToolTab,
                    onSelectTab = { selectedMarketToolTab = it },
                    strings = strings
                )
            }

            // Helpful Offline Reference Guide Card when offline or live market lookup is unavailable / quota-limited
            if (!marketSyncState.isOnline || searchAiState is SearchAiUiState.Error) {
                item {
                    OfflineMarketReferenceCard(
                        language = lang,
                        strings = strings,
                        snapshot = marketSyncState.snapshot
                    )
                }
            }
        }
    }

    if (showExportSheet) {
        MonthlyReportExportSheet(
            payload = monthlyReportPayload,
            strings = strings,
            exportingFormat = exportingFormat,
            onExport = { format -> triggerExport(format) },
            onDismiss = { if (exportingFormat == null) showExportSheet = false }
        )
    }
}

@Composable
private fun SpendingTrendLineCard(
    points: List<DailyTrendPoint>,
    dailyAverage: Double,
    peakAmount: Double,
    language: AppLanguage,
    strings: AppStrings
) {
    val crystal = LocalCrystalColors.current
    var selectedIdx by remember(points) { mutableIntStateOf(-1) }
    val activePoint = points.getOrNull(selectedIdx)

    CrystalCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = strings.spendingTrendTitle,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = HindSiliguriFamily
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${strings.dailyAverageLabel}: ${BdtFormatter.format(dailyAverage, language)}",
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFamily),
                    color = crystal.mutedText
                )
            }
            Surface(
                color = crystal.warningAmberBg,
                shape = CircleShape
            ) {
                Text(
                    text = "${strings.peakSpendingLabel}: ${BdtFormatter.format(peakAmount, language)}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.Bold
                    ),
                    color = crystal.warningAmber,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        if (activePoint != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${activePoint.dayLabel}: ${BdtFormatter.format(activePoint.amount, language)}",
                style = MaterialTheme.typography.labelLarge.copy(fontFamily = JetBrainsMonoFamily),
                color = MaterialTheme.colorScheme.primary
            )
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
    strings: AppStrings
) {
    val crystal = LocalCrystalColors.current

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
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier.size(136.dp),
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
                            style = Stroke(width = 24f, cap = StrokeCap.Round)
                        )
                        startAngle += sweep
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = strings.spentLabel,
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = HindSiliguriFamily),
                        color = crystal.mutedText
                    )
                    Text(
                        text = BdtFormatter.format(totalSpent, language),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
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
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = BdtFormatter.formatPercent((item.share * 100).toInt(), language),
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                            color = crystal.mutedText
                        )
                    }
                }
            }
        }
    }
}
