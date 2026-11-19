package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.StartOfWeek
import com.example.localization.AppLanguage
import com.example.localization.AppStrings
import com.example.ui.theme.HindSiliguriFamily
import com.example.ui.theme.LocalCrystalColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeekStartSelectorSheet(
    selectedDay: StartOfWeek,
    language: AppLanguage,
    strings: AppStrings,
    onSelectDay: (StartOfWeek) -> Unit,
    onDismiss: () -> Unit
) {
    val crystal = LocalCrystalColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val refined = strings.settingsRefinement

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = if (crystal.isDark) Color(0xFF101522) else MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = crystal.mutedText.copy(alpha = 0.45f)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 20.dp)
                .testTag("week_start_bottom_sheet")
        ) {
            // Header row: Title + Subtitle on left, Close (✕) button on top-right
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = refined.weekStartSheetTitle,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = HindSiliguriFamily,
                            fontSize = 19.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = refined.weekStartSheetSubtitle,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = HindSiliguriFamily,
                            fontSize = 12.5.sp,
                            lineHeight = 18.sp
                        ),
                        color = crystal.mutedText
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("week_start_sheet_close_button")
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (crystal.isDark) Color(0xFF1B2335) else crystal.glassSurfaceElevated,
                        border = BorderStroke(1.dp, crystal.glassBorder),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = strings.close,
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // All 7 Days in exact order: শনিবার -> শুক্রবার
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                StartOfWeek.entries.forEach { day ->
                    WeekStartDayRadioRow(
                        label = day.label(language),
                        selected = day == selectedDay,
                        testTag = "week_start_option_${day.name}",
                        radioSelectedTag = "week_start_radio_selected_${day.name}",
                        onClick = {
                            onSelectDay(day)
                            onDismiss()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun WeekStartDayRadioRow(
    label: String,
    selected: Boolean,
    testTag: String,
    radioSelectedTag: String,
    onClick: () -> Unit
) {
    val crystal = LocalCrystalColors.current
    val primary = MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape(16.dp)

    val containerColor by animateColorAsState(
        targetValue = when {
            selected && crystal.isDark -> primary.copy(alpha = 0.18f)
            selected -> primary.copy(alpha = 0.10f)
            crystal.isDark -> Color(0xFF161C2B)
            else -> crystal.glassSurfaceElevated
        },
        label = "weekDayContainer"
    )
    val borderColor by animateColorAsState(
        targetValue = when {
            selected -> primary
            crystal.isDark -> Color.White.copy(alpha = 0.12f)
            else -> crystal.glassBorder
        },
        label = "weekDayBorder"
    )
    val textColor by animateColorAsState(
        targetValue = if (selected) primary else MaterialTheme.colorScheme.onSurface,
        label = "weekDayText"
    )

    Surface(
        onClick = onClick,
        shape = shape,
        color = containerColor,
        border = BorderStroke(if (selected) 1.5.dp else 1.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .shadow(
                elevation = if (selected) 6.dp else 1.dp,
                shape = shape,
                ambientColor = if (selected) primary.copy(alpha = 0.24f) else Color.Black.copy(alpha = 0.08f),
                spotColor = if (selected) primary.copy(alpha = 0.28f) else Color.Black.copy(alpha = 0.10f)
            )
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    fontFamily = HindSiliguriFamily,
                    fontSize = 15.sp
                ),
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            // Crisp custom radio indicator on the right
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .border(
                        width = if (selected) 2.dp else 1.6.dp,
                        color = if (selected) {
                            primary
                        } else if (crystal.isDark) {
                            Color.White.copy(alpha = 0.30f)
                        } else {
                            MaterialTheme.colorScheme.outline
                        },
                        shape = CircleShape
                    )
                    .let { mod ->
                        if (selected) mod.testTag(radioSelectedTag) else mod
                    },
                contentAlignment = Alignment.Center
            ) {
                if (selected) {
                    Box(
                        modifier = Modifier
                            .size(11.dp)
                            .clip(CircleShape)
                            .background(primary)
                    )
                }
            }
        }
    }
}
