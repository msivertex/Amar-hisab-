package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBasket
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ai.InsightsSummaryHelper
import com.example.data.ai.MarketErrorCategory
import com.example.data.market.MarketCalculators
import com.example.data.market.MarketSnapshot
import com.example.data.market.MarketSyncUiState
import com.example.data.market.MarketToolTab
import com.example.localization.AppLanguage
import com.example.localization.AppStrings
import com.example.localization.BdtFormatter
import com.example.ui.components.CrystalCard
import com.example.ui.components.CurrencyAmountText
import com.example.ui.theme.HindSiliguriFamily
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalCrystalColors
import com.example.ui.viewmodel.AnalyticsSnapshot
import com.example.ui.viewmodel.MainUiState
import com.example.ui.viewmodel.SearchAiUiState
import com.example.ui.viewmodel.TtsUiState
import kotlin.math.abs

private data class MarketQuickPreset(
    val shortLabel: String,
    val fullQuery: String,
    val icon: ImageVector,
    val tint: Color,
    val toolTab: MarketToolTab
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SmartInsightsHighlightStrip(
    analytics: AnalyticsSnapshot,
    language: AppLanguage,
    strings: AppStrings,
    mainCurrencyCode: String = "BDT",
    secondaryCurrencyCode: String? = null,
    marketSnapshot: MarketSnapshot = MarketSnapshot.defaultReference()
) {
    val crystal = LocalCrystalColors.current
    val modern = strings.insightsModern
    val primary = MaterialTheme.colorScheme.primary
    val secondaryDailyAvg = remember(analytics.dailyAverage, mainCurrencyCode, secondaryCurrencyCode, marketSnapshot) {
        secondaryCurrencyCode?.let { marketSnapshot.convertCurrencyCode(analytics.dailyAverage, mainCurrencyCode, it) }
    }

    CrystalCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("insights_highlights_card")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.AutoAwesome,
                contentDescription = null,
                tint = primary,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = modern.smartHighlightsTitle,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = HindSiliguriFamily
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. Period-over-Period Spending Change
            val changePct = analytics.expenseChangePercent
            val isLower = changePct != null && changePct < 0
            val isHigher = changePct != null && changePct > 0
            val deltaColor = when {
                isLower -> crystal.assetGreen
                isHigher -> crystal.liabilityRed
                else -> primary
            }
            val deltaValueText = when {
                changePct == null -> modern.firstPeriodLabel
                changePct < 0 -> "↓ ${BdtFormatter.formatPercent(abs(changePct), language)} ${modern.spendingLowerSuffix}"
                changePct > 0 -> "↑ ${BdtFormatter.formatPercent(changePct, language)} ${modern.spendingHigherSuffix}"
                else -> modern.spendingSameLabel
            }

            HighlightMetricTile(
                label = modern.vsPreviousPeriod,
                value = deltaValueText,
                accentColor = deltaColor,
                icon = if (isHigher) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                testTag = "insight_highlight_delta",
                modifier = Modifier.weight(1f)
            )

            // 2. Top Expense Category Share (or Savings Rate / Daily Avg if no expenses)
            val topCat = analytics.topCategory
            if (topCat != null) {
                val pctStr = BdtFormatter.formatPercent((topCat.share * 100).toInt(), language)
                HighlightMetricTile(
                    label = modern.topCategoryShareLabel,
                    value = "${topCat.category.label(language)} ($pctStr)",
                    accentColor = topCat.category.color,
                    icon = Icons.Filled.Savings,
                    testTag = "insight_highlight_top_category",
                    modifier = Modifier.weight(1f)
                )
            } else if (analytics.savingsRatePercent != null) {
                HighlightMetricTile(
                    label = modern.savingsRateShortLabel,
                    value = BdtFormatter.formatPercent(analytics.savingsRatePercent, language),
                    accentColor = crystal.assetGreen,
                    icon = Icons.Filled.Savings,
                    testTag = "insight_highlight_top_category",
                    modifier = Modifier.weight(1f)
                )
            } else {
                val secondaryText = if (secondaryCurrencyCode != null && secondaryDailyAvg != null) {
                    BdtFormatter.formatCurrency(secondaryDailyAvg, secondaryCurrencyCode, language)
                } else {
                    null
                }
                HighlightMetricTile(
                    label = modern.dailyAvgShortLabel,
                    value = BdtFormatter.formatCurrency(analytics.dailyAverage, mainCurrencyCode, language),
                    secondaryValue = secondaryText,
                    secondaryColor = crystal.kwdNumericInk,
                    accentColor = crystal.assetGreen,
                    icon = Icons.Filled.Savings,
                    testTag = "insight_highlight_top_category",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Secondary Savings Rate & Dual-Currency Daily Avg pill bar when both are meaningful
        if (analytics.topCategory != null && (analytics.savingsRatePercent != null || analytics.dailyAverage > 0.0)) {
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                analytics.savingsRatePercent?.let { rate ->
                    val rateColor = if (rate >= 0) crystal.assetGreen else crystal.liabilityRed
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = rateColor.copy(alpha = if (crystal.isDark) 0.18f else 0.10f),
                        border = BorderStroke(0.8.dp, rateColor.copy(alpha = 0.30f))
                    ) {
                        Text(
                            text = "${modern.savingsRateShortLabel}: ${BdtFormatter.formatPercent(rate, language)}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = HindSiliguriFamily
                            ),
                            color = rateColor,
                            maxLines = 2,
                            softWrap = true,
                            overflow = TextOverflow.Clip,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
                if (analytics.dailyAverage > 0.0) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = primary.copy(alpha = if (crystal.isDark) 0.18f else 0.10f),
                        border = BorderStroke(0.8.dp, primary.copy(alpha = 0.28f)),
                        modifier = Modifier.testTag("insight_highlight_daily_avg")
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "${modern.dailyAvgShortLabel}:",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontFamily = HindSiliguriFamily
                                    ),
                                    color = primary,
                                    maxLines = 1,
                                    softWrap = false
                                )
                                CurrencyAmountText(
                                    amount = analytics.dailyAverage,
                                    currencyCode = mainCurrencyCode,
                                    language = language,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = JetBrainsMonoFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.5.sp
                                    ),
                                    isBalance = true,
                                    colorOverride = primary,
                                    bdtFontScale = 0.92f,
                                    minFontSize = 9.5.sp
                                )
                            }
                            if (secondaryCurrencyCode != null && secondaryDailyAvg != null) {
                                CurrencyAmountText(
                                    amount = secondaryDailyAvg,
                                    currencyCode = secondaryCurrencyCode,
                                    language = language,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = JetBrainsMonoFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 10.5.sp
                                    ),
                                    isBalance = true,
                                    colorOverride = crystal.kwdNumericInk,
                                    bdtFontScale = 0.92f,
                                    minFontSize = 9.sp,
                                    modifier = Modifier.testTag("insight_highlight_daily_avg_secondary")
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HighlightMetricTile(
    label: String,
    value: String,
    accentColor: Color,
    icon: ImageVector,
    testTag: String,
    modifier: Modifier = Modifier,
    secondaryValue: String? = null,
    secondaryColor: Color? = null
) {
    val crystal = LocalCrystalColors.current
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = accentColor.copy(alpha = if (crystal.isDark) 0.14f else 0.08f),
        border = BorderStroke(1.dp, accentColor.copy(alpha = if (crystal.isDark) 0.32f else 0.22f)),
        modifier = modifier.testTag(testTag)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 11.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = HindSiliguriFamily
                    ),
                    color = crystal.mutedText,
                    maxLines = 2,
                    softWrap = true,
                    overflow = TextOverflow.Clip
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = HindSiliguriFamily,
                    fontSize = 13.sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 3,
                softWrap = true,
                overflow = TextOverflow.Clip
            )
            if (!secondaryValue.isNullOrBlank()) {
                Text(
                    text = secondaryValue,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = JetBrainsMonoFamily,
                        fontSize = 11.5.sp
                    ),
                    color = secondaryColor ?: crystal.kwdNumericInk,
                    maxLines = 2,
                    softWrap = true,
                    overflow = TextOverflow.Clip
                )
            }
        }
    }
}

@Composable
fun VoiceFinancialSummaryCard(
    state: MainUiState,
    ttsState: TtsUiState,
    strings: AppStrings,
    onPlayBriefingTts: () -> Unit,
    onPauseTts: () -> Unit,
    onResumeTts: () -> Unit,
    onStopTts: () -> Unit
) {
    val crystal = LocalCrystalColors.current
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val modern = strings.insightsModern
    val lang = state.prefs.language

    val periodSummaryText = remember(
        state.analytics,
        state.insightFilter,
        state.prefs.language,
        state.prefs.userName,
        state.prefs.mainCurrencyCode,
        state.totalLiquidBalance,
        state.accounts,
        state.transactions,
        state.goals,
        state.loans
    ) {
        InsightsSummaryHelper.buildPeriodSummary(state)
    }

    var isSummaryExpanded by remember { mutableStateOf(false) }

    // Auto-expand full readable summary when playback runs, finishes, or falls back so user can always read along
    LaunchedEffect(ttsState) {
        if (ttsState is TtsUiState.Playing ||
            ttsState is TtsUiState.Paused ||
            ttsState is TtsUiState.Completed ||
            ttsState is TtsUiState.Error
        ) {
            isSummaryExpanded = true
        }
    }

    val activeScript = when (ttsState) {
        is TtsUiState.Playing -> ttsState.script.ifBlank { periodSummaryText }
        is TtsUiState.Paused -> ttsState.script.ifBlank { periodSummaryText }
        is TtsUiState.Completed -> ttsState.script.ifBlank { periodSummaryText }
        is TtsUiState.Error -> ttsState.script.ifBlank { periodSummaryText }
        else -> periodSummaryText
    }

    val replayLabel = if (lang == AppLanguage.BN) "আবার শুনুন" else "Replay"
    val completedBadgeLabel = if (lang == AppLanguage.BN) "সম্পন্ন" else "Completed"
    val completedHintLabel = if (lang == AppLanguage.BN) {
        "সম্পূর্ণ বিস্তারিত সারসংক্ষেপ শোনা সম্পন্ন হয়েছে।"
    } else {
        "Finished reading the complete financial summary."
    }

    CrystalCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("voice_summary_card"),
        elevated = true
    ) {
        // Header Row: Animated Icon + Title + Period/Status Pill
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                primary.copy(alpha = if (crystal.isDark) 0.30f else 0.16f),
                                secondary.copy(alpha = if (crystal.isDark) 0.24f else 0.12f)
                            )
                        )
                    )
                    .border(
                        width = 1.dp,
                        color = primary.copy(alpha = if (crystal.isDark) 0.45f else 0.28f),
                        shape = RoundedCornerShape(15.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.GraphicEq,
                    contentDescription = strings.aiBriefingTitle,
                    tint = primary,
                    modifier = Modifier.size(26.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = strings.aiBriefingTitle,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = HindSiliguriFamily
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }
                Text(
                    text = "${state.insightFilter.label(lang)} • ${state.analytics.periodLabel.ifBlank { modern.periodSummaryTitle }}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = HindSiliguriFamily
                    ),
                    color = crystal.mutedText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Live State Badge
            val statusBadgeText = when (ttsState) {
                is TtsUiState.Playing -> strings.playingAudio
                is TtsUiState.Paused -> modern.pausedLabel
                is TtsUiState.Generating -> strings.generatingAudio
                is TtsUiState.Completed -> completedBadgeLabel
                else -> state.insightFilter.label(lang)
            }
            val statusBadgeColor = when (ttsState) {
                is TtsUiState.Playing -> crystal.assetGreen
                is TtsUiState.Paused -> crystal.warningAmber
                is TtsUiState.Completed -> crystal.assetGreen
                is TtsUiState.Generating -> primary
                else -> primary
            }
            Surface(
                shape = CircleShape,
                color = statusBadgeColor.copy(alpha = if (crystal.isDark) 0.20f else 0.12f),
                border = BorderStroke(0.8.dp, statusBadgeColor.copy(alpha = 0.34f))
            ) {
                Text(
                    text = statusBadgeText,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = HindSiliguriFamily,
                        fontSize = 11.sp
                    ),
                    color = statusBadgeColor,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                )
            }
        }

        // Animated Audio Equalizer & Chunk Progress Indicator when Playing or Paused
        AnimatedVisibility(
            visible = ttsState is TtsUiState.Playing || ttsState is TtsUiState.Paused,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            val isSpeaking = ttsState is TtsUiState.Playing
            val progressValue = when (ttsState) {
                is TtsUiState.Playing -> ttsState.progress
                is TtsUiState.Paused -> ttsState.progress
                else -> 0f
            }
            val currentChunk = when (ttsState) {
                is TtsUiState.Playing -> ttsState.currentChunkIndex
                is TtsUiState.Paused -> ttsState.currentChunkIndex
                else -> 0
            }
            val totalChunks = when (ttsState) {
                is TtsUiState.Playing -> ttsState.totalChunks.coerceAtLeast(1)
                is TtsUiState.Paused -> ttsState.totalChunks.coerceAtLeast(1)
                else -> 1
            }
            val voiceMeta = when (ttsState) {
                is TtsUiState.Playing -> listOf(ttsState.engineLabel, ttsState.voiceLocaleLabel)
                    .filter { it.isNotBlank() }
                    .joinToString(" • ")
                is TtsUiState.Paused -> listOf(ttsState.engineLabel, ttsState.voiceLocaleLabel)
                    .filter { it.isNotBlank() }
                    .joinToString(" • ")
                else -> ""
            }
            val partLabel = if (totalChunks > 1) {
                val curStr = BdtFormatter.formatNumber((currentChunk + 1).coerceIn(1, totalChunks), lang)
                val totStr = BdtFormatter.formatNumber(totalChunks, lang)
                if (lang == AppLanguage.BN) "অংশ $curStr/$totStr" else "Part $curStr/$totStr"
            } else {
                ""
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .testTag("tts_audio_visualizer"),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = primary.copy(alpha = if (crystal.isDark) 0.14f else 0.08f),
                    border = BorderStroke(1.dp, primary.copy(alpha = 0.24f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AnimatedAudioEqualizerBars(
                            isAnimating = isSpeaking,
                            barColor = primary
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isSpeaking) strings.playingAudio else modern.pausedLabel,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontFamily = HindSiliguriFamily
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (partLabel.isNotBlank()) {
                                    Text(
                                        text = partLabel,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = HindSiliguriFamily
                                        ),
                                        color = primary,
                                        modifier = Modifier.testTag("tts_chunk_progress_label")
                                    )
                                }
                            }
                            if (voiceMeta.isNotBlank()) {
                                Text(
                                    text = voiceMeta,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = HindSiliguriFamily,
                                        fontSize = 11.sp
                                    ),
                                    color = crystal.mutedText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (progressValue > 0.01f) {
                                LinearProgressIndicator(
                                    progress = { progressValue.coerceIn(0f, 1f) },
                                    color = primary,
                                    trackColor = primary.copy(alpha = 0.20f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(5.dp)
                                        .clip(CircleShape)
                                )
                            } else if (isSpeaking) {
                                LinearProgressIndicator(
                                    color = primary,
                                    trackColor = primary.copy(alpha = 0.20f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(5.dp)
                                        .clip(CircleShape)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Playback Controls (Play / Pause / Resume / Stop / Replay / Completed / Error)
        when (ttsState) {
            is TtsUiState.Idle -> {
                Button(
                    onClick = onPlayBriefingTts,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("tts_briefing_button")
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = strings.listenBriefingTts,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = HindSiliguriFamily
                        )
                    )
                }
            }

            is TtsUiState.Generating -> {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = primary.copy(alpha = if (crystal.isDark) 0.16f else 0.09f),
                    border = BorderStroke(1.dp, primary.copy(alpha = 0.28f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("tts_generating_indicator")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.4.dp,
                            color = primary
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = strings.generatingAudio,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = HindSiliguriFamily
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            is TtsUiState.Playing -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalButton(
                        onClick = onPauseTts,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("tts_pause_button")
                    ) {
                        Icon(Icons.Filled.Pause, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = modern.pauseAudio,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = HindSiliguriFamily
                            ),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                    FilledTonalIconButton(
                        onClick = onPlayBriefingTts,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("tts_replay_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Replay,
                            contentDescription = replayLabel,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Button(
                        onClick = onStopTts,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = crystal.liabilityRed,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("tts_stop_button")
                    ) {
                        Icon(Icons.Filled.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = modern.stopAudioShort,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = HindSiliguriFamily
                            ),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            is TtsUiState.Paused -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onResumeTts,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("tts_resume_button")
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = modern.resumeAudio,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = HindSiliguriFamily
                            ),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                    FilledTonalIconButton(
                        onClick = onPlayBriefingTts,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("tts_replay_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Replay,
                            contentDescription = replayLabel,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    OutlinedButton(
                        onClick = onStopTts,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, crystal.liabilityRed.copy(alpha = 0.55f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = crystal.liabilityRed),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("tts_stop_button")
                    ) {
                        Icon(Icons.Filled.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = modern.stopAudioShort,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = HindSiliguriFamily
                            ),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            is TtsUiState.Completed -> {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = crystal.assetGreen.copy(alpha = if (crystal.isDark) 0.16f else 0.10f),
                        border = BorderStroke(1.dp, crystal.assetGreen.copy(alpha = 0.34f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("tts_completed_notice")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = completedBadgeLabel,
                                tint = crystal.assetGreen,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = completedHintLabel,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = HindSiliguriFamily,
                                    fontWeight = FontWeight.Medium
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Button(
                        onClick = onPlayBriefingTts,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("tts_replay_button")
                    ) {
                        Icon(Icons.Filled.Replay, contentDescription = null, modifier = Modifier.size(19.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = replayLabel,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = HindSiliguriFamily
                            )
                        )
                    }
                }
            }

            is TtsUiState.Error -> {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = crystal.warningAmberBg,
                        border = BorderStroke(1.dp, crystal.warningAmber.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("tts_error_notice")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Info,
                                contentDescription = null,
                                tint = crystal.warningAmber,
                                modifier = Modifier
                                    .padding(top = 2.dp)
                                    .size(18.dp)
                            )
                            Text(
                                text = ttsState.message,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = HindSiliguriFamily,
                                    lineHeight = 19.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Button(
                        onClick = onPlayBriefingTts,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("tts_retry_button")
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = modern.retrySearchBtn,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = HindSiliguriFamily
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Toggleable / Collapsible Full Text Summary Preview Trigger
        Surface(
            onClick = { isSummaryExpanded = !isSummaryExpanded },
            shape = RoundedCornerShape(12.dp),
            color = if (crystal.isDark) Color.White.copy(alpha = 0.05f) else Color(0xFFF3F7FC),
            border = BorderStroke(1.dp, crystal.glassBorder),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .testTag("tts_summary_toggle_button")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = primary.copy(alpha = if (crystal.isDark) 0.22f else 0.12f)
                    ) {
                        Text(
                            text = modern.readAlongBadge,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = HindSiliguriFamily,
                                fontSize = 10.5.sp
                            ),
                            color = primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = if (isSummaryExpanded) modern.hideSummaryPreview else modern.showSummaryPreview,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = HindSiliguriFamily
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Icon(
                    imageVector = if (isSummaryExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (isSummaryExpanded) modern.hideSummaryPreview else modern.showSummaryPreview,
                    tint = crystal.mutedText,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        AnimatedVisibility(
            visible = isSummaryExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            val paragraphs = remember(activeScript) {
                activeScript.split(Regex("\\r?\\n+")).map { it.trim() }.filter { it.isNotEmpty() }
            }
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (crystal.isDark) Color(0xFF141B2B) else Color(0xFFF7FAFF),
                border = BorderStroke(1.dp, primary.copy(alpha = if (crystal.isDark) 0.28f else 0.18f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .testTag("tts_summary_preview_box")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    paragraphs.forEachIndexed { idx, para ->
                        Text(
                            text = para,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = HindSiliguriFamily,
                                fontWeight = if (idx == 0) FontWeight.SemiBold else FontWeight.Normal,
                                lineHeight = 22.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AnimatedAudioEqualizerBars(
    isAnimating: Boolean,
    barColor: Color
) {
    val transition = rememberInfiniteTransition(label = "voiceEqualizer")
    val b1 by transition.animateFloat(
        initialValue = 0.28f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(420, easing = LinearEasing), RepeatMode.Reverse),
        label = "eq1"
    )
    val b2 by transition.animateFloat(
        initialValue = 0.85f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(tween(520, easing = LinearEasing), RepeatMode.Reverse),
        label = "eq2"
    )
    val b3 by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(tween(360, easing = LinearEasing), RepeatMode.Reverse),
        label = "eq3"
    )
    val b4 by transition.animateFloat(
        initialValue = 0.90f,
        targetValue = 0.30f,
        animationSpec = infiniteRepeatable(tween(480, easing = LinearEasing), RepeatMode.Reverse),
        label = "eq4"
    )
    val heights = if (isAnimating) listOf(b1, b2, b3, b4) else listOf(0.35f, 0.55f, 0.40f, 0.50f)

    Row(
        horizontalArrangement = Arrangement.spacedBy(3.5.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.height(24.dp)
    ) {
        heights.forEach { frac ->
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height((22f * frac).dp)
                    .clip(CircleShape)
                    .background(barColor)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LiveMarketRatesCard(
    language: AppLanguage,
    searchAiState: SearchAiUiState,
    strings: AppStrings,
    onSearchMarket: (String) -> Unit,
    marketSyncState: MarketSyncUiState = MarketSyncUiState(),
    onSyncMarketRates: () -> Unit = {},
    onSelectToolTab: (MarketToolTab) -> Unit = {}
) {
    val crystal = LocalCrystalColors.current
    val uriHandler = LocalUriHandler.current
    val primary = MaterialTheme.colorScheme.primary
    val modern = strings.insightsModern
    val hub = strings.marketHub
    var marketQuery by remember { mutableStateOf("") }

    val quickPresets = remember(language, crystal.assetGreen, crystal.warningAmber, primary) {
        if (language == AppLanguage.BN) {
            listOf(
                MarketQuickPreset(
                    shortLabel = "মুদ্রা ও USD রেট",
                    fullQuery = "আজকের ইউএস ডলার থেকে টাকা রেট কত?",
                    icon = Icons.Filled.CurrencyExchange,
                    tint = crystal.assetGreen,
                    toolTab = MarketToolTab.CURRENCY
                ),
                MarketQuickPreset(
                    shortLabel = "স্বর্ণের বাজারদর",
                    fullQuery = "বাংলাদেশে আজকের ২২ ক্যারেট স্বর্ণের ভরি প্রতি দাম কত?",
                    icon = Icons.Filled.Diamond,
                    tint = crystal.warningAmber,
                    toolTab = MarketToolTab.GOLD
                ),
                MarketQuickPreset(
                    shortLabel = "ব্যাংক FDR ও DPS",
                    fullQuery = "বাংলাদেশের ব্যাংকের বর্তমান এফডিআর ও ডিপিএস রেট",
                    icon = Icons.Filled.AccountBalance,
                    tint = primary,
                    toolTab = MarketToolTab.BANK_FDR_DPS
                ),
                MarketQuickPreset(
                    shortLabel = "রেমিট্যান্স রেট",
                    fullQuery = "বাংলাদেশে আজকের রেমিট্যান্স ডলার রেট ও সরকারি প্রণোদনা কত?",
                    icon = Icons.Filled.VolunteerActivism,
                    tint = Color(0xFF7C3AED),
                    toolTab = MarketToolTab.REMITTANCE
                ),
                MarketQuickPreset(
                    shortLabel = "বিকাশ/নগদ চার্জ",
                    fullQuery = "বিকাশ ও নগদের ক্যাশ-আউট চার্জ কত?",
                    icon = Icons.Filled.PhoneAndroid,
                    tint = Color(0xFFE11D48),
                    toolTab = MarketToolTab.MFS_FEE
                ),
                MarketQuickPreset(
                    shortLabel = "নিত্যপণ্যের দর",
                    fullQuery = "ঢাকায় আজকের নিত্যপণ্যের বাজারদর",
                    icon = Icons.Filled.ShoppingBasket,
                    tint = Color(0xFF0D9488),
                    toolTab = MarketToolTab.ESSENTIALS
                )
            )
        } else {
            listOf(
                MarketQuickPreset(
                    shortLabel = "FX & USD Rate",
                    fullQuery = "Latest USD to BDT exchange rate today",
                    icon = Icons.Filled.CurrencyExchange,
                    tint = crystal.assetGreen,
                    toolTab = MarketToolTab.CURRENCY
                ),
                MarketQuickPreset(
                    shortLabel = "Gold Price BD",
                    fullQuery = "Today 22K gold price per bhori in Bangladesh BAJUS",
                    icon = Icons.Filled.Diamond,
                    tint = crystal.warningAmber,
                    toolTab = MarketToolTab.GOLD
                ),
                MarketQuickPreset(
                    shortLabel = "Bank FDR & DPS",
                    fullQuery = "Bangladesh bank FDR and DPS interest rates",
                    icon = Icons.Filled.AccountBalance,
                    tint = primary,
                    toolTab = MarketToolTab.BANK_FDR_DPS
                ),
                MarketQuickPreset(
                    shortLabel = "Remittance Rate",
                    fullQuery = "Bangladesh remittance USD exchange rate today",
                    icon = Icons.Filled.VolunteerActivism,
                    tint = Color(0xFF7C3AED),
                    toolTab = MarketToolTab.REMITTANCE
                ),
                MarketQuickPreset(
                    shortLabel = "bKash / Nagad Fee",
                    fullQuery = "bKash and Nagad cash-out charges in Bangladesh",
                    icon = Icons.Filled.PhoneAndroid,
                    tint = Color(0xFFE11D48),
                    toolTab = MarketToolTab.MFS_FEE
                ),
                MarketQuickPreset(
                    shortLabel = "Dhaka Essentials",
                    fullQuery = "Current essential grocery prices in Dhaka",
                    icon = Icons.Filled.ShoppingBasket,
                    tint = Color(0xFF0D9488),
                    toolTab = MarketToolTab.ESSENTIALS
                )
            )
        }
    }

    CrystalCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("live_market_card")
    ) {
        // Header + Connectivity Pill + Manual Sync Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(crystal.assetGreen.copy(alpha = if (crystal.isDark) 0.22f else 0.14f))
                    .border(
                        width = 1.dp,
                        color = crystal.assetGreen.copy(alpha = if (crystal.isDark) 0.42f else 0.28f),
                        shape = RoundedCornerShape(14.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Public,
                    contentDescription = modern.liveMarketTitle,
                    tint = crystal.assetGreen,
                    modifier = Modifier.size(24.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = modern.liveMarketTitle,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = HindSiliguriFamily
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = modern.liveMarketSubtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = HindSiliguriFamily
                    ),
                    color = crystal.mutedText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Connectivity Status Pill / Sync Action
            Surface(
                onClick = { if (marketSyncState.isOnline && !marketSyncState.isSyncing) onSyncMarketRates() },
                shape = CircleShape,
                color = when {
                    !marketSyncState.isOnline -> crystal.warningAmberBg
                    marketSyncState.isSyncing -> primary.copy(alpha = if (crystal.isDark) 0.22f else 0.12f)
                    else -> crystal.assetGreen.copy(alpha = if (crystal.isDark) 0.20f else 0.12f)
                },
                border = BorderStroke(
                    width = 0.9.dp,
                    color = when {
                        !marketSyncState.isOnline -> crystal.warningAmber.copy(alpha = 0.45f)
                        marketSyncState.isSyncing -> primary.copy(alpha = 0.45f)
                        else -> crystal.assetGreen.copy(alpha = 0.42f)
                    }
                ),
                modifier = Modifier
                    .heightIn(min = 36.dp)
                    .testTag("market_connectivity_badge")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (marketSyncState.isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(12.dp),
                            strokeWidth = 1.8.dp,
                            color = primary
                        )
                    } else {
                        Icon(
                            imageVector = if (marketSyncState.isOnline) Icons.Filled.Refresh else Icons.Filled.CloudOff,
                            contentDescription = hub.syncNowBtn,
                            tint = if (marketSyncState.isOnline) crystal.assetGreen else crystal.warningAmber,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                    Text(
                        text = when {
                            !marketSyncState.isOnline -> hub.offlineBadge
                            marketSyncState.isSyncing -> hub.syncingBadge
                            else -> hub.onlineLiveBadge
                        },
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = HindSiliguriFamily,
                            fontSize = 10.5.sp
                        ),
                        color = when {
                            !marketSyncState.isOnline -> crystal.warningAmber
                            marketSyncState.isSyncing -> primary
                            else -> crystal.assetGreen
                        },
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }

        // Smart Offline Notice — ONLY displayed when offline; automatically hidden once internet is active
        if (!marketSyncState.isOnline) {
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = crystal.warningAmberBg,
                border = BorderStroke(1.dp, crystal.warningAmber.copy(alpha = 0.40f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("market_offline_notice_banner")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.CloudOff,
                        contentDescription = hub.offlineBadge,
                        tint = crystal.warningAmber,
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .size(18.dp)
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = hub.offlineNoticeTitle,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = HindSiliguriFamily
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = hub.offlineNoticeBody,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = HindSiliguriFamily,
                                lineHeight = 17.sp
                            ),
                            color = crystal.mutedText
                        )
                        Text(
                            text = "${hub.lastUpdatedPrefix}: ${MarketCalculators.formatTimestamp(marketSyncState.snapshot.lastSyncedMillis, language)}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = JetBrainsMonoFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 10.5.sp
                            ),
                            color = crystal.warningAmber
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Cleanly Wrapping Quick-Query Pills (FlowRow -> Zero horizontal clipping or overflow)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("market_quick_chips_container")
        ) {
            quickPresets.forEachIndexed { index, preset ->
                val isSelected = marketQuery == preset.fullQuery
                Surface(
                    onClick = {
                        marketQuery = preset.fullQuery
                        onSelectToolTab(preset.toolTab)
                        onSearchMarket(preset.fullQuery)
                    },
                    shape = CircleShape,
                    color = if (isSelected) {
                        preset.tint.copy(alpha = if (crystal.isDark) 0.26f else 0.16f)
                    } else if (crystal.isDark) {
                        Color.White.copy(alpha = 0.06f)
                    } else {
                        Color(0xFFF3F7FC)
                    },
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (isSelected) {
                            preset.tint.copy(alpha = 0.65f)
                        } else {
                            crystal.glassBorder
                        }
                    ),
                    modifier = Modifier
                        .heightIn(min = 40.dp)
                        .testTag("market_quick_chip_$index")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = preset.icon,
                            contentDescription = null,
                            tint = preset.tint,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = preset.shortLabel,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = HindSiliguriFamily
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Cleanly Aligned Query Input + "খুঁজুন" Button (Zero text clipping)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = marketQuery,
                onValueChange = { marketQuery = it },
                placeholder = {
                    Text(
                        text = strings.askMarketHint,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = HindSiliguriFamily
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = primary,
                    unfocusedBorderColor = crystal.glassBorder
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        if (marketQuery.isNotBlank() && searchAiState !is SearchAiUiState.Loading) {
                            onSearchMarket(marketQuery)
                        }
                    }
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("market_search_input")
            )

            Button(
                onClick = { onSearchMarket(marketQuery) },
                enabled = marketQuery.isNotBlank() && searchAiState !is SearchAiUiState.Loading,
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                modifier = Modifier
                    .heightIn(min = 54.dp)
                    .testTag("market_search_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = strings.searchBtn,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = HindSiliguriFamily
                    ),
                    maxLines = 1,
                    softWrap = false
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Result / Loading / Graceful Quota & Offline Error Area
        when (searchAiState) {
            is SearchAiUiState.Idle -> Unit

            is SearchAiUiState.Loading -> {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = primary.copy(alpha = if (crystal.isDark) 0.14f else 0.08f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("market_loading_indicator")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.4.dp,
                            color = primary
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = modern.searchingMarketLabel,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Medium,
                                fontFamily = HindSiliguriFamily
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            is SearchAiUiState.Success -> {
                Surface(
                    color = if (crystal.isDark) Color(0xFF141B2B) else Color(0xFFF6FAFF),
                    border = BorderStroke(1.dp, crystal.glassBorder),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("market_success_card")
                ) {
                    Text(
                        text = searchAiState.result.answer,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = HindSiliguriFamily,
                            lineHeight = 22.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(14.dp)
                    )
                }

                if (searchAiState.result.sources.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = strings.sourcesLabel,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = HindSiliguriFamily
                        ),
                        color = crystal.mutedText
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        searchAiState.result.sources.take(5).forEach { source ->
                            Surface(
                                color = primary.copy(alpha = if (crystal.isDark) 0.20f else 0.11f),
                                border = BorderStroke(0.8.dp, primary.copy(alpha = 0.28f)),
                                shape = CircleShape,
                                modifier = Modifier.clickable {
                                    runCatching { uriHandler.openUri(source.uri) }
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = source.title,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = HindSiliguriFamily
                                        ),
                                        color = primary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                        contentDescription = source.title,
                                        tint = primary,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            is SearchAiUiState.Error -> {
                // Defensive UI-level sanitization guarantees raw quota strings or URLs are NEVER shown
                val safeDisplayError = remember(searchAiState.message, language) {
                    val lower = searchAiState.message.lowercase()
                    if (lower.contains("quota") ||
                        lower.contains("http://") ||
                        lower.contains("https://") ||
                        lower.contains("resource_exhausted") ||
                        lower.contains("429") ||
                        lower.contains("googleapis")
                    ) {
                        InsightsSummaryHelper.sanitizeAiError(
                            rawMessage = searchAiState.message,
                            language = language,
                            isVoice = false
                        ).message
                    } else {
                        searchAiState.message
                    }
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Calm, Polite Status Notice Banner
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = crystal.warningAmberBg,
                        border = BorderStroke(1.dp, crystal.warningAmber.copy(alpha = 0.36f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("market_error_card")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(9.dp)
                            ) {
                                Icon(
                                    imageVector = if (searchAiState.category == MarketErrorCategory.OFFLINE) {
                                        Icons.Filled.CloudOff
                                    } else {
                                        Icons.Filled.Info
                                    },
                                    contentDescription = null,
                                    tint = crystal.warningAmber,
                                    modifier = Modifier
                                        .padding(top = 2.dp)
                                        .size(19.dp)
                                )
                                Text(
                                    text = safeDisplayError,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = HindSiliguriFamily,
                                        lineHeight = 20.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            val retryQuery = searchAiState.failedQuery.ifBlank { marketQuery }
                            if (retryQuery.isNotBlank()) {
                                OutlinedButton(
                                    onClick = { onSearchMarket(retryQuery) },
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, crystal.warningAmber.copy(alpha = 0.55f)),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                    modifier = Modifier
                                        .heightIn(min = 42.dp)
                                        .testTag("market_retry_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Refresh,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = modern.retrySearchBtn,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = HindSiliguriFamily
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface
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
fun OfflineMarketReferenceCard(
    language: AppLanguage,
    strings: AppStrings,
    snapshot: MarketSnapshot = MarketSnapshot.defaultReference()
) {
    val crystal = LocalCrystalColors.current
    val primary = MaterialTheme.colorScheme.primary
    val modern = strings.insightsModern
    val offlineGuideItems = remember(language, snapshot) {
        InsightsSummaryHelper.offlineMarketGuide(language, snapshot)
    }

    CrystalCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("market_offline_guide_card")
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = modern.offlineGuideTitle,
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
                    color = primary.copy(alpha = if (crystal.isDark) 0.20f else 0.11f)
                ) {
                    Text(
                        text = modern.offlineReferenceBadge,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = HindSiliguriFamily,
                            fontSize = 10.5.sp
                        ),
                        color = primary,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Text(
                text = modern.offlineGuideSubtitle,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = HindSiliguriFamily
                ),
                color = crystal.mutedText
            )

            HorizontalDivider(thickness = 0.7.dp, color = crystal.glassBorder)

            offlineGuideItems.forEachIndexed { idx, guide ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = guide.title,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = HindSiliguriFamily
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = guide.note,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = HindSiliguriFamily
                            ),
                            color = crystal.mutedText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = guide.value,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = primary,
                        maxLines = 1
                    )
                }
                if (idx < offlineGuideItems.lastIndex) {
                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color = crystal.glassBorder.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}
