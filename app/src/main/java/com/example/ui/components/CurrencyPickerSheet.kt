package com.example.ui.components

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.market.MarketCalculators
import com.example.data.market.MarketSnapshot
import com.example.data.model.AppCountry
import com.example.data.model.AppCurrency
import com.example.data.model.CountryRegionGroup
import com.example.localization.AppLanguage
import com.example.ui.theme.HindSiliguriFamily
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalCrystalColors

/**
 * Crystal Liquid frosted-glass bottom sheet for selecting Main Currency,
 * Second Currency, or an individual Card/Account's currency.
 *
 * Reads directly from the unified [AppCountry] dataset grouped by [CountryRegionGroup]
 * so Main Currency, Second Currency, Profile Details > Country, and the Card / New Account Wizard
 * stay 100% synchronized automatically without manual duplication.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppCurrencyPickerSheet(
    title: String,
    subtitle: String,
    selectedCurrency: AppCurrency?,
    language: AppLanguage,
    referenceCurrency: AppCurrency = AppCurrency.BDT,
    marketSnapshot: MarketSnapshot = MarketSnapshot.defaultReference(),
    allowClear: Boolean = false,
    onSelect: (AppCurrency) -> Unit,
    onClear: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val crystal = LocalCrystalColors.current
    val primary = MaterialTheme.colorScheme.primary
    var searchQuery by remember { mutableStateOf("") }

    // Canonical first country for each currency preserves deterministic test tags (e.g., app_currency_option_EUR)
    val canonicalCountryForCurrency = remember {
        AppCurrency.entries.associateWith { cur ->
            AppCountry.entries.firstOrNull { it.suggestedCurrency == cur }
        }
    }

    val groupedEntries = remember(searchQuery) {
        val q = searchQuery.trim()
        CountryRegionGroup.entries.mapNotNull { group ->
            val matches = AppCountry.entries.filter { country ->
                country.group == group && (
                    q.isEmpty() ||
                        country.code.contains(q, ignoreCase = true) ||
                        country.nameEn.contains(q, ignoreCase = true) ||
                        country.nameBn.contains(q, ignoreCase = true) ||
                        country.suggestedCurrency.code.contains(q, ignoreCase = true) ||
                        country.suggestedCurrency.symbol.contains(q, ignoreCase = true) ||
                        country.suggestedCurrency.nameEn.contains(q, ignoreCase = true) ||
                        country.suggestedCurrency.nameBn.contains(q, ignoreCase = true) ||
                        group.en.contains(q, ignoreCase = true) ||
                        group.bn.contains(q, ignoreCase = true)
                    )
            }
            if (matches.isEmpty()) null else group to matches
        }
    }

    val searchHint = if (language == AppLanguage.BN) {
        "দেশ বা মুদ্রা খুঁজুন (BDT, KWD, সৌদি, দুবাই, ইতালি, সাইপ্রাস, UK)..."
    } else {
        "Search country or currency (BDT, KWD, Saudi, Italy, Cyprus, UK)..."
    }
    val clearLabel = if (language == AppLanguage.BN) "দ্বিতীয় মুদ্রা মুছুন" else "Clear Second Currency"

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = if (crystal.isDark) Color(0xFF0E1726) else Color(0xFFF6FAFF),
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.testTag("app_currency_picker_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 18.dp)
                .padding(bottom = 22.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = HindSiliguriFamily
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = HindSiliguriFamily),
                        color = crystal.mutedText
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("app_currency_picker_close")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = if (language == AppLanguage.BN) "বন্ধ করুন" else "Close",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        text = searchHint,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = HindSiliguriFamily),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = null,
                        tint = primary,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = primary,
                    unfocusedBorderColor = crystal.glassBorder
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("app_currency_picker_search")
            )

            if (allowClear && selectedCurrency != null && onClear != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onClear,
                        modifier = Modifier.testTag("app_currency_picker_clear_btn")
                    ) {
                        Text(
                            text = clearLabel,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = HindSiliguriFamily
                            ),
                            color = crystal.liabilityRed
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 440.dp)
                    .testTag("app_currency_picker_list"),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                groupedEntries.forEach { (group, countries) ->
                    item(key = "header_${group.name}") {
                        Text(
                            text = group.label(language),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = HindSiliguriFamily
                            ),
                            color = primary,
                            modifier = Modifier.padding(top = 6.dp, bottom = 2.dp, start = 4.dp)
                        )
                    }

                    items(countries, key = { "${group.name}_${it.code}" }) { country ->
                        val currency = country.suggestedCurrency
                        val isSelected = currency == selectedCurrency
                        val rateText = if (!currency.code.equals(referenceCurrency.code, ignoreCase = true)) {
                            val converted = marketSnapshot.convertCurrencyCode(
                                amount = 1.0,
                                fromCode = currency.code,
                                toCode = referenceCurrency.code
                            )
                            val decimals = if (referenceCurrency.code.equals("KWD", ignoreCase = true) || converted < 1.0) 3 else 2
                            val refPrefix = referenceCurrency.codeAndSymbolLabel()
                            "1 ${currency.code} ≈ $refPrefix ${MarketCalculators.formatDecimal(converted, decimals, language)}"
                        } else {
                            if (language == AppLanguage.BN) "মূল ভিত্তি" else "Base"
                        }

                        val isCanonical = canonicalCountryForCurrency[currency] == country
                        val rowTag = if (isCanonical) {
                            "app_currency_option_${currency.code}"
                        } else {
                            "app_currency_option_${currency.code}_${country.code}"
                        }

                        val badgeText = currency.codeAndSymbolLabel()

                        Surface(
                            onClick = { onSelect(currency) },
                            shape = RoundedCornerShape(18.dp),
                            color = if (isSelected) {
                                primary.copy(alpha = if (crystal.isDark) 0.24f else 0.12f)
                            } else if (crystal.isDark) {
                                Color.White.copy(alpha = 0.05f)
                            } else {
                                Color.White.copy(alpha = 0.88f)
                            },
                            border = BorderStroke(
                                width = if (isSelected) 1.4.dp else 0.9.dp,
                                color = if (isSelected) primary else crystal.glassBorder
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 64.dp)
                                .testTag(rowTag)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 11.dp),
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
                                            .size(44.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(
                                                if (crystal.isDark) Color.White.copy(alpha = 0.08f)
                                                else primary.copy(alpha = 0.08f)
                                            )
                                            .border(0.8.dp, crystal.glassBorder, RoundedCornerShape(14.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = country.flag, fontSize = 23.sp)
                                    }

                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Text(
                                            text = country.label(language),
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = HindSiliguriFamily
                                            ),
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${currency.currencyName(language)} • $rateText",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontFamily = JetBrainsMonoFamily
                                            ),
                                            color = crystal.mutedText,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = if (isSelected) primary else primary.copy(alpha = if (crystal.isDark) 0.18f else 0.11f)
                                    ) {
                                        Text(
                                            text = badgeText,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontFamily = JetBrainsMonoFamily,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = if (isSelected) Color.White else primary,
                                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                        )
                                    }
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Filled.CheckCircle,
                                            contentDescription = null,
                                            tint = primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

/**
 * Crystal Liquid frosted-glass bottom sheet for selecting a Country/Region.
 * Shared across Profile Details > Country and the Card / New Account Wizard.
 * Displays all 28 synchronized countries grouped by [CountryRegionGroup] with instant search.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppCountryPickerSheet(
    title: String,
    subtitle: String,
    selectedCountry: AppCountry,
    language: AppLanguage,
    onSelect: (AppCountry) -> Unit,
    onDismiss: () -> Unit
) {
    val crystal = LocalCrystalColors.current
    val primary = MaterialTheme.colorScheme.primary
    var searchQuery by remember { mutableStateOf("") }

    val groupedCountries = remember(searchQuery) {
        val q = searchQuery.trim()
        CountryRegionGroup.entries.mapNotNull { group ->
            val matches = AppCountry.entries.filter { country ->
                country.group == group && (
                    q.isEmpty() ||
                        country.code.contains(q, ignoreCase = true) ||
                        country.nameEn.contains(q, ignoreCase = true) ||
                        country.nameBn.contains(q, ignoreCase = true) ||
                        country.suggestedCurrency.code.contains(q, ignoreCase = true) ||
                        country.suggestedCurrency.nameEn.contains(q, ignoreCase = true) ||
                        country.suggestedCurrency.nameBn.contains(q, ignoreCase = true) ||
                        group.en.contains(q, ignoreCase = true) ||
                        group.bn.contains(q, ignoreCase = true)
                    )
            }
            if (matches.isEmpty()) null else group to matches
        }
    }

    val searchHint = if (language == AppLanguage.BN) {
        "দেশ খুঁজুন (Bangladesh, Kuwait, Saudi, Oman, Italy, Cyprus)..."
    } else {
        "Search country (Bangladesh, Kuwait, Saudi, Oman, Italy, Cyprus)..."
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = if (crystal.isDark) Color(0xFF0E1726) else Color(0xFFF6FAFF),
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.testTag("app_country_picker_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 18.dp)
                .padding(bottom = 22.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = HindSiliguriFamily
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = HindSiliguriFamily),
                        color = crystal.mutedText
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("app_country_picker_close")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = if (language == AppLanguage.BN) "বন্ধ করুন" else "Close",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        text = searchHint,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = HindSiliguriFamily),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = null,
                        tint = primary,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = primary,
                    unfocusedBorderColor = crystal.glassBorder
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("app_country_picker_search")
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 440.dp)
                    .testTag("app_country_picker_list"),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                groupedCountries.forEach { (group, countries) ->
                    item(key = "country_group_${group.name}") {
                        Text(
                            text = group.label(language),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = HindSiliguriFamily
                            ),
                            color = primary,
                            modifier = Modifier.padding(top = 6.dp, bottom = 2.dp, start = 4.dp)
                        )
                    }

                    items(countries, key = { it.code }) { country ->
                        val isSelected = country == selectedCountry
                        val cur = country.suggestedCurrency
                        val badgeText = cur.codeAndSymbolLabel()
                        Surface(
                            onClick = { onSelect(country) },
                            shape = RoundedCornerShape(18.dp),
                            color = if (isSelected) {
                                primary.copy(alpha = if (crystal.isDark) 0.24f else 0.12f)
                            } else if (crystal.isDark) {
                                Color.White.copy(alpha = 0.05f)
                            } else {
                                Color.White.copy(alpha = 0.88f)
                            },
                            border = BorderStroke(
                                width = if (isSelected) 1.4.dp else 0.9.dp,
                                color = if (isSelected) primary else crystal.glassBorder
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 64.dp)
                                .testTag("app_country_option_${country.code}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 11.dp),
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
                                            .size(44.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(
                                                if (crystal.isDark) Color.White.copy(alpha = 0.08f)
                                                else primary.copy(alpha = 0.08f)
                                            )
                                            .border(0.8.dp, crystal.glassBorder, RoundedCornerShape(14.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = country.flag, fontSize = 23.sp)
                                    }

                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Text(
                                            text = country.label(language),
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = HindSiliguriFamily
                                            ),
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = cur.currencyName(language),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontFamily = HindSiliguriFamily
                                            ),
                                            color = crystal.mutedText,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = if (isSelected) primary else primary.copy(alpha = if (crystal.isDark) 0.18f else 0.11f)
                                    ) {
                                        Text(
                                            text = badgeText,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontFamily = JetBrainsMonoFamily,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = if (isSelected) Color.White else primary,
                                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                        )
                                    }
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Filled.CheckCircle,
                                            contentDescription = null,
                                            tint = primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}
