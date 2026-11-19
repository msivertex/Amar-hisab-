package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.localization.AppLanguage
import com.example.localization.AppStrings
import com.example.ui.components.CrystalCard
import com.example.ui.theme.LocalCrystalColors

@Composable
fun LanguageSelectionSubPage(
    activeLanguage: AppLanguage,
    strings: AppStrings,
    onSelectLanguage: (AppLanguage) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val crystal = LocalCrystalColors.current
    val copy = strings.settingsRefinement

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("language_selection_screen"),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 620.dp)
                .fillMaxSize()
                .testTag("language_selection_list"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header with frosted-glass circular Back button
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("language_back_button")
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = crystal.glassSurfaceElevated,
                            border = BorderStroke(1.dp, crystal.glassBorder),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = strings.close,
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = copy.languagePageTitle,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = copy.languagePageSubtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = crystal.mutedText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Frosted-glass Language Options Card
            item {
                CrystalCard(
                    modifier = Modifier.fillMaxWidth(),
                    elevated = true
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    MaterialTheme.colorScheme.primary.copy(
                                        alpha = if (crystal.isDark) 0.22f else 0.14f
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Translate,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Text(
                            text = strings.languageLabel,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    LanguageOptionCard(
                        glyph = "অ",
                        title = copy.languageOptionBanglaTitle,
                        subtitle = copy.languageOptionBanglaSubtitle,
                        activeBadgeLabel = strings.themeSettings.activeThemeBadge,
                        selected = activeLanguage == AppLanguage.BN,
                        testTag = "language_option_BN",
                        checkTestTag = "language_check_BN",
                        onClick = { onSelectLanguage(AppLanguage.BN) }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    LanguageOptionCard(
                        glyph = "Aa",
                        title = copy.languageOptionEnglishTitle,
                        subtitle = copy.languageOptionEnglishSubtitle,
                        activeBadgeLabel = strings.themeSettings.activeThemeBadge,
                        selected = activeLanguage == AppLanguage.EN,
                        testTag = "language_option_EN",
                        checkTestTag = "language_check_EN",
                        onClick = { onSelectLanguage(AppLanguage.EN) }
                    )
                }
            }

            // Instant sync informational frosted card
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.primary.copy(
                        alpha = if (crystal.isDark) 0.14f else 0.08f
                    ),
                    border = BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.primary.copy(
                            alpha = if (crystal.isDark) 0.30f else 0.22f
                        )
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Translate,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = copy.languageInstantNote,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LanguageOptionCard(
    glyph: String,
    title: String,
    subtitle: String,
    activeBadgeLabel: String,
    selected: Boolean,
    testTag: String,
    checkTestTag: String,
    onClick: () -> Unit
) {
    val crystal = LocalCrystalColors.current
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val shape = RoundedCornerShape(18.dp)

    val containerColor by animateColorAsState(
        targetValue = when {
            selected && crystal.isDark -> primary.copy(alpha = 0.18f)
            selected -> primary.copy(alpha = 0.10f)
            crystal.isDark -> Color(0xFF161C2B)
            else -> crystal.glassSurfaceElevated
        },
        label = "langOptionContainer"
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) primary else crystal.glassBorder,
        label = "langOptionBorder"
    )

    Surface(
        onClick = onClick,
        shape = shape,
        color = containerColor,
        border = BorderStroke(if (selected) 1.5.dp else 1.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 74.dp)
            .shadow(
                elevation = if (selected) 8.dp else 2.dp,
                shape = shape,
                ambientColor = if (selected) primary.copy(alpha = 0.26f) else Color.Black.copy(alpha = 0.12f),
                spotColor = if (selected) primary.copy(alpha = 0.30f) else Color.Black.copy(alpha = 0.14f)
            )
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (selected) {
                            Brush.linearGradient(listOf(primary, secondary))
                        } else {
                            Brush.linearGradient(
                                listOf(
                                    primary.copy(alpha = if (crystal.isDark) 0.22f else 0.14f),
                                    secondary.copy(alpha = if (crystal.isDark) 0.16f else 0.10f)
                                )
                            )
                        }
                    )
                    .border(
                        width = 0.8.dp,
                        color = if (selected) Color.White.copy(alpha = 0.50f) else primary.copy(alpha = 0.25f),
                        shape = RoundedCornerShape(14.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = glyph,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 19.sp
                    ),
                    color = if (selected) Color.White else primary
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (selected) {
                        Surface(
                            color = primary.copy(alpha = if (crystal.isDark) 0.28f else 0.16f),
                            shape = CircleShape,
                            border = BorderStroke(0.7.dp, primary.copy(alpha = 0.45f))
                        ) {
                            Text(
                                text = activeBadgeLabel,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = crystal.mutedText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (selected) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(primary)
                        .border(1.2.dp, Color.White.copy(alpha = 0.65f), CircleShape)
                        .testTag(checkTestTag),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = activeBadgeLabel,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .border(
                            width = 1.5.dp,
                            color = if (crystal.isDark) Color.White.copy(alpha = 0.24f) else MaterialTheme.colorScheme.outline,
                            shape = CircleShape
                        )
                )
            }
        }
    }
}
