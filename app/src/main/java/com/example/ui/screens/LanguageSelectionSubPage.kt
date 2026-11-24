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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.example.localization.GlobalLanguageStrings
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
    val pickerCopy = remember(activeLanguage) { GlobalLanguageStrings.pickerCopy(activeLanguage) }

    var searchQuery by rememberSaveable { mutableStateOf("") }

    val matchingSuggested by remember(searchQuery) {
        derivedStateOf {
            AppLanguage.suggestedLanguages.filter { it.matchesQuery(searchQuery) }
        }
    }
    val matchingAllOther by remember(searchQuery) {
        derivedStateOf {
            AppLanguage.allOtherLanguages.filter { it.matchesQuery(searchQuery) }
        }
    }
    val totalMatches = matchingSuggested.size + matchingAllOther.size

    Box(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .testTag("language_selection_screen"),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 620.dp)
                .fillMaxSize()
                .testTag("language_selection_list"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 44.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. Header with frosted-glass Back button and active language status pill
            item(key = "header") {
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

                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(
                            alpha = if (crystal.isDark) 0.24f else 0.14f
                        ),
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.42f)
                        ),
                        modifier = Modifier.testTag("language_active_summary_pill")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Text(
                                text = activeLanguage.code.uppercase(),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (activeLanguage.isRtl) {
                                Text(
                                    text = "• ${pickerCopy.rtlBadgeLabel}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            // 2. Gboard-Style Live Search Bar
            item(key = "search_bar") {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    singleLine = true,
                    placeholder = {
                        Text(
                            text = pickerCopy.searchPlaceholder,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = strings.searchBtn,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { searchQuery = "" },
                                modifier = Modifier.testTag("language_search_clear_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = pickerCopy.clearSearchLabel,
                                    tint = crystal.mutedText,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    shape = RoundedCornerShape(26.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = if (crystal.isDark) Color(0xFF151C2C) else crystal.glassSurfaceElevated,
                        unfocusedContainerColor = if (crystal.isDark) Color(0xFF131927) else crystal.glassSurface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = crystal.glassBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("language_search_input")
                )
            }

            // 3. Suggested / Popular Languages Section (Gboard-style pinned quick picks with regional variants)
            if (matchingSuggested.isNotEmpty()) {
                item(key = "suggested_header") {
                    LanguageSectionLabel(
                        title = pickerCopy.primarySectionLabel,
                        badge = pickerCopy.resultsCountFormat(matchingSuggested.size),
                        icon = Icons.Filled.Translate,
                        testTag = "language_primary_section_header"
                    )
                }

                items(
                    items = matchingSuggested,
                    key = { "suggested_${it.name}" }
                ) { lang ->
                    LanguageOptionCard(
                        language = lang,
                        title = lang.nativeName,
                        subtitle = "${lang.englishName} • ${lang.regionHint}",
                        activeBadgeLabel = strings.themeSettings.activeThemeBadge,
                        rtlBadgeLabel = pickerCopy.rtlBadgeLabel,
                        selected = activeLanguage == lang,
                        testTag = "language_option_${lang.name}",
                        checkTestTag = "language_check_${lang.name}",
                        onClick = { onSelectLanguage(lang) }
                    )
                }
            }

            // 4. All Languages Section (Global, Expatriate & Major Indian Languages)
            if (matchingAllOther.isNotEmpty()) {
                item(key = "global_header") {
                    Spacer(modifier = Modifier.height(4.dp))
                    LanguageSectionLabel(
                        title = pickerCopy.allLanguagesSectionLabel,
                        badge = pickerCopy.resultsCountFormat(matchingAllOther.size),
                        icon = Icons.Filled.Language,
                        testTag = "language_global_section_header"
                    )
                }

                items(
                    items = matchingAllOther,
                    key = { "global_${it.name}" }
                ) { lang ->
                    LanguageOptionCard(
                        language = lang,
                        title = lang.nativeName,
                        subtitle = "${lang.englishName} • ${lang.regionHint}",
                        activeBadgeLabel = strings.themeSettings.activeThemeBadge,
                        rtlBadgeLabel = pickerCopy.rtlBadgeLabel,
                        selected = activeLanguage == lang,
                        testTag = "language_option_${lang.name}",
                        checkTestTag = "language_check_${lang.name}",
                        onClick = { onSelectLanguage(lang) }
                    )
                }
            }

            // 5. Empty Search State
            if (totalMatches == 0) {
                item(key = "empty_state") {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (crystal.isDark) Color(0xFF151C2C) else crystal.glassSurfaceElevated,
                        border = BorderStroke(1.dp, crystal.glassBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("language_empty_search_state")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Language,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.72f),
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = pickerCopy.noMatchingLanguages,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            OutlinedButton(
                                onClick = { searchQuery = "" },
                                shape = CircleShape,
                                modifier = Modifier.heightIn(min = 48.dp)
                            ) {
                                Text(text = pickerCopy.clearSearchLabel)
                            }
                        }
                    }
                }
            }

            // 6. Instant sync & RTL/fallback informational frosted card
            item(key = "instant_note") {
                Spacer(modifier = Modifier.height(4.dp))
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
private fun LanguageSectionLabel(
    title: String,
    badge: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    testTag: String
) {
    val crystal = LocalCrystalColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        MaterialTheme.colorScheme.primary.copy(
                            alpha = if (crystal.isDark) 0.22f else 0.12f
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
        }
        Text(
            text = badge,
            style = MaterialTheme.typography.labelSmall,
            color = crystal.mutedText
        )
    }
}

@Composable
private fun LanguageOptionCard(
    language: AppLanguage,
    title: String,
    subtitle: String,
    activeBadgeLabel: String,
    rtlBadgeLabel: String,
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
            .heightIn(min = 68.dp)
            .shadow(
                elevation = if (selected) 6.dp else 1.dp,
                shape = shape,
                ambientColor = if (selected) primary.copy(alpha = 0.24f) else Color.Black.copy(alpha = 0.10f),
                spotColor = if (selected) primary.copy(alpha = 0.28f) else Color.Black.copy(alpha = 0.12f)
            )
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Native script glyph tile
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(
                        if (selected) {
                            Brush.linearGradient(listOf(primary, secondary))
                        } else {
                            Brush.linearGradient(
                                listOf(
                                    primary.copy(alpha = if (crystal.isDark) 0.22f else 0.13f),
                                    secondary.copy(alpha = if (crystal.isDark) 0.16f else 0.09f)
                                )
                            )
                        }
                    )
                    .border(
                        width = 0.8.dp,
                        color = if (selected) Color.White.copy(alpha = 0.50f) else primary.copy(alpha = 0.24f),
                        shape = RoundedCornerShape(13.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = language.scriptGlyph,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = if (language.scriptGlyph.length > 2) 14.sp else 18.sp
                    ),
                    color = if (selected) Color.White else primary,
                    maxLines = 1
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    // ISO Code chip
                    Surface(
                        color = if (crystal.isDark) Color.White.copy(alpha = 0.08f) else primary.copy(alpha = 0.09f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = language.code.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            ),
                            color = if (selected) primary else crystal.mutedText,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    if (language.isRtl) {
                        Surface(
                            color = MaterialTheme.colorScheme.tertiary.copy(
                                alpha = if (crystal.isDark) 0.22f else 0.20f
                            ),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = rtlBadgeLabel,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 10.sp
                                ),
                                color = if (crystal.isDark) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onTertiary,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                    if (selected) {
                        Surface(
                            color = primary.copy(alpha = if (crystal.isDark) 0.28f else 0.16f),
                            shape = CircleShape,
                            border = BorderStroke(0.7.dp, primary.copy(alpha = 0.45f))
                        ) {
                            Text(
                                text = activeBadgeLabel,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                ),
                                color = primary,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
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
                        .size(28.dp)
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
                        modifier = Modifier.size(16.dp)
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(24.dp)
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
