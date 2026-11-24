package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.AccountEntity
import com.example.data.market.MarketSnapshot
import com.example.data.model.AccountNameResolver
import com.example.data.model.AccountType
import com.example.data.model.AppCountry
import com.example.data.model.AppCurrency
import com.example.data.model.BankSubtype
import com.example.data.model.CardColorCatalog
import com.example.data.model.CardNetwork
import com.example.data.model.InternationalBank
import com.example.data.model.InternationalBankCatalog
import com.example.data.model.MfsProvider
import com.example.data.model.defaultColorForCash
import com.example.localization.AppLanguage
import com.example.localization.AppStrings
import com.example.localization.BdtFormatter
import com.example.ui.components.AccountVisualCard
import com.example.ui.components.AppCountryPickerSheet
import com.example.ui.components.AppCurrencyPickerSheet
import com.example.ui.theme.LocalCrystalColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAccountWizardSheet(
    language: AppLanguage,
    strings: AppStrings,
    onDismiss: () -> Unit,
    onSaveAccount: (AccountEntity) -> Unit,
    defaultCurrencyCode: String = "BDT",
    secondaryCurrencyCode: String = "",
    userCountryCode: String = "BD",
    marketSnapshot: MarketSnapshot = MarketSnapshot.defaultReference()
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val crystal = LocalCrystalColors.current

    val initialCountry = remember(userCountryCode, defaultCurrencyCode, secondaryCurrencyCode) {
        val secondaryCur = AppCurrency.fromCodeOrNull(secondaryCurrencyCode)
        when {
            userCountryCode.isNotBlank() && !userCountryCode.equals("BD", ignoreCase = true) ->
                AppCountry.fromCode(userCountryCode)
            secondaryCur != null -> AppCountry.preferredForCurrency(secondaryCur)
            else -> AppCountry.fromCode(userCountryCode)
        }
    }

    var selectedType by remember { mutableStateOf(AccountType.DEBIT_BANK) }
    var selectedCountry by remember { mutableStateOf(initialCountry) }
    var selectedBank by remember {
        mutableStateOf<InternationalBank?>(
            InternationalBankCatalog.banksForCountry(initialCountry).firstOrNull()
        )
    }
    var isManualCustomBank by remember {
        mutableStateOf(InternationalBankCatalog.banksForCountry(initialCountry).isEmpty())
    }
    var customBankName by remember { mutableStateOf("") }
    var selectedCardColor by remember {
        mutableStateOf(
            InternationalBankCatalog.banksForCountry(initialCountry).firstOrNull()?.brandColor
                ?: CardColorCatalog.swatches.first()
        )
    }
    var selectedCurrency by remember { mutableStateOf(initialCountry.suggestedCurrency) }
    var showCountryPicker by remember { mutableStateOf(false) }
    var showCurrencyPicker by remember { mutableStateOf(false) }

    var selectedSubtype by remember { mutableStateOf(BankSubtype.SALARY) }
    var selectedNetwork by remember { mutableStateOf(CardNetwork.VISA) }
    var selectedMfs by remember { mutableStateOf(MfsProvider.BKASH) }

    var customName by remember { mutableStateOf("") }
    var digitsOrMobile by remember { mutableStateOf("") }
    var balanceText by remember { mutableStateOf("") }
    var creditLimitText by remember { mutableStateOf("100000") }

    val parsedBalance = balanceText.toDoubleOrNull() ?: 0.0
    val parsedLimit = creditLimitText.toDoubleOrNull() ?: 100000.0

    fun applySelectedCountry(country: AppCountry) {
        selectedCountry = country
        selectedCurrency = country.suggestedCurrency
        val countryBanks = InternationalBankCatalog.banksForCountry(country)
        if (countryBanks.isNotEmpty()) {
            isManualCustomBank = false
            selectedBank = countryBanks.first()
            selectedCardColor = countryBanks.first().brandColor
        } else {
            isManualCustomBank = true
            selectedBank = null
            selectedCardColor = CardColorCatalog.swatches.first()
        }
    }

    val previewColor = when (selectedType) {
        AccountType.DEBIT_BANK, AccountType.CREDIT_CARD -> selectedCardColor
        AccountType.MFS -> selectedMfs.brandColor
        AccountType.CASH -> defaultColorForCash()
    }

    val manualFallbackLabel = if (language == AppLanguage.BN) "কাস্টম ব্যাংক" else "Custom Bank"
    val resolvedProvider = when (selectedType) {
        AccountType.DEBIT_BANK, AccountType.CREDIT_CARD -> {
            if (isManualCustomBank || selectedBank == null) {
                customBankName.trim().ifBlank { manualFallbackLabel }
            } else {
                selectedBank!!.label(language)
            }
        }
        AccountType.MFS -> selectedMfs.label(language)
        AccountType.CASH -> if (language == AppLanguage.BN) "নগদ টাকা" else "Physical Cash"
    }
    val resolvedSubtype = if (selectedType == AccountType.DEBIT_BANK) selectedSubtype.label(language) else ""
    val resolvedNetwork = if (selectedType == AccountType.DEBIT_BANK || selectedType == AccountType.CREDIT_CARD) {
        selectedNetwork.label
    } else ""

    val defaultTitle = AccountNameResolver.defaultAccountName(
        type = selectedType,
        bankOrProvider = resolvedProvider,
        accountSubtype = resolvedSubtype,
        cardNetwork = resolvedNetwork,
        currencyCode = selectedCurrency.code,
        language = language
    )

    val trimmedCustomName = customName.trim()
    val previewAccount = AccountEntity(
        name = trimmedCustomName.ifBlank { defaultTitle },
        type = selectedType,
        balance = parsedBalance,
        creditLimit = if (selectedType == AccountType.CREDIT_CARD) parsedLimit else 0.0,
        bankOrProvider = resolvedProvider,
        accountSubtype = resolvedSubtype,
        cardNetwork = resolvedNetwork,
        accountNumberLast4 = digitsOrMobile.trim().ifBlank {
            when (selectedType) {
                AccountType.MFS -> "017XX-XXXX"
                AccountType.CASH -> selectedCurrency.code
                else -> "4829"
            }
        },
        colorArgb = previewColor.toArgb().toLong() and 0xFFFFFFFFL,
        currencyCode = selectedCurrency.code
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.testTag("add_account_wizard_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = strings.addAccountWizardTitle,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Live Real-Time Card Preview
            Text(
                text = strings.liveCardPreview,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            AccountVisualCard(
                account = previewAccount,
                language = language,
                strings = strings,
                compact = true,
                modifier = Modifier.testTag("wizard_live_card_preview")
            )

            // Account Type Selector (4 types)
            Text(
                text = strings.accountTypeLabel,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    AccountType.DEBIT_BANK to strings.typeDebitBank,
                    AccountType.CREDIT_CARD to strings.typeCreditCard,
                    AccountType.MFS to strings.typeMfs,
                    AccountType.CASH to strings.typeCash
                ).forEach { (type, label) ->
                    FilterChip(
                        selected = selectedType == type,
                        onClick = {
                            selectedType = type
                            selectedCurrency = when (type) {
                                AccountType.MFS -> AppCurrency.BDT
                                AccountType.DEBIT_BANK, AccountType.CREDIT_CARD -> selectedCountry.suggestedCurrency
                                AccountType.CASH -> AppCurrency.fromCodeOrNull(secondaryCurrencyCode)
                                    ?: AppCurrency.fromCode(defaultCurrencyCode)
                            }
                        },
                        label = { Text(label) },
                        modifier = Modifier.testTag("wizard_type_${type.name}")
                    )
                }
            }

            // Conditional Wizard Fields per Account Type
            when (selectedType) {
                AccountType.DEBIT_BANK, AccountType.CREDIT_CARD -> {
                    // Synchronized Country Selector (reads directly from unified AppCountry.entries)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = strings.bankCountryRegionLabel,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Surface(
                            onClick = { showCountryPicker = true },
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = if (crystal.isDark) 0.22f else 0.12f),
                            modifier = Modifier.testTag("wizard_country_picker_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Search,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = if (language == AppLanguage.BN) {
                                        "সব দেশ (${BdtFormatter.formatNumber(AppCountry.entries.size, language)})"
                                    } else {
                                        "All ${AppCountry.entries.size} Countries"
                                    },
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    // Active Country Card (tappable to open searchable 28-country picker)
                    Surface(
                        color = if (crystal.isDark) Color(0xFF161C2B) else crystal.glassSurfaceElevated,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, crystal.glassBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { showCountryPicker = true }
                            .testTag("wizard_selected_country_card")
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
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            MaterialTheme.colorScheme.primary.copy(
                                                alpha = if (crystal.isDark) 0.22f else 0.14f
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = selectedCountry.flag, fontSize = 20.sp)
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = selectedCountry.label(language),
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${selectedCountry.group.label(language)} • ${selectedCountry.suggestedCurrency.code}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = crystal.mutedText,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = strings.bankCountryRegionLabel,
                                tint = crystal.mutedText,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Quick scrollable country chips for featured expatriate & neighboring countries
                    // (plus any country chosen from the full 195-country picker above)
                    val quickCountries = remember(selectedCountry) {
                        if (AppCountry.quickSelectCountries.any { it.code == selectedCountry.code }) {
                            AppCountry.quickSelectCountries
                        } else {
                            listOf(selectedCountry) + AppCountry.quickSelectCountries
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        quickCountries.forEach { country ->
                            FilterChip(
                                selected = selectedCountry == country,
                                onClick = { applySelectedCountry(country) },
                                label = { Text("${country.flag} ${country.label(language)}") },
                                modifier = Modifier.testTag("wizard_country_${country.code}")
                            )
                        }
                    }

                    // Dynamic Country Bank Selection Heading (e.g., "Select Bangladeshi Bank", "Select Kuwait Bank")
                    Text(
                        text = selectedCountry.bankSelectionHeading(language),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.testTag("wizard_bank_section_heading")
                    )

                    val countryBanks = remember(selectedCountry) {
                        InternationalBankCatalog.banksForCountry(selectedCountry)
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        countryBanks.forEach { bank ->
                            FilterChip(
                                selected = !isManualCustomBank && selectedBank?.id == bank.id,
                                onClick = {
                                    isManualCustomBank = false
                                    selectedBank = bank
                                    selectedCardColor = bank.brandColor
                                    selectedCurrency = bank.defaultCurrency
                                },
                                label = { Text(bank.label(language)) },
                                modifier = Modifier.testTag("wizard_bank_${bank.id}")
                            )
                        }

                        // Always available for every country: Manual / Custom Bank option
                        FilterChip(
                            selected = isManualCustomBank,
                            onClick = { isManualCustomBank = true },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = {
                                Text(
                                    if (language == AppLanguage.BN) {
                                        "ম্যানুয়াল / কাস্টম ব্যাংক"
                                    } else {
                                        "Manual / Custom Bank"
                                    }
                                )
                            },
                            modifier = Modifier.testTag("wizard_bank_custom")
                        )
                    }

                    // Manual / Custom Bank Name input when user chooses Manual / Custom Bank
                    if (isManualCustomBank) {
                        OutlinedTextField(
                            value = customBankName,
                            onValueChange = { customBankName = it.take(60) },
                            label = {
                                Text(
                                    if (language == AppLanguage.BN) {
                                        "ব্যাংক বা কার্ড প্রতিষ্ঠানের নাম লিখুন"
                                    } else {
                                        "Enter Bank / Card Institution Name"
                                    }
                                )
                            },
                            placeholder = {
                                Text(
                                    if (language == AppLanguage.BN) {
                                        "যেমন: Citizens Bank, Private Card..."
                                    } else {
                                        "e.g. Citizens Bank, Local Private Bank..."
                                    }
                                )
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("wizard_custom_bank_input")
                        )
                    }

                    // Card Color Picker (swatches update Live Card Preview immediately)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Palette,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = if (language == AppLanguage.BN) "কার্ডের রঙ (Card Color)" else "Card Color",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .testTag("wizard_card_color_picker"),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CardColorCatalog.swatches.forEachIndexed { idx, swatch ->
                            val isSelectedColor = selectedCardColor.toArgb() == swatch.toArgb()
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .clickable { selectedCardColor = swatch }
                                    .testTag("wizard_card_color_$idx"),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(if (isSelectedColor) 38.dp else 32.dp)
                                        .clip(CircleShape)
                                        .background(swatch)
                                        .border(
                                            width = if (isSelectedColor) 2.5.dp else 1.dp,
                                            color = if (isSelectedColor) {
                                                MaterialTheme.colorScheme.onSurface
                                            } else {
                                                Color.White.copy(alpha = 0.45f)
                                            },
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelectedColor) {
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = if (language == AppLanguage.BN) "নির্বাচিত রঙ" else "Selected color",
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (selectedType == AccountType.DEBIT_BANK) {
                        Text(
                            text = strings.accountSubtype,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            BankSubtype.entries.forEach { subtype ->
                                FilterChip(
                                    selected = selectedSubtype == subtype,
                                    onClick = { selectedSubtype = subtype },
                                    label = { Text(subtype.label(language)) }
                                )
                            }
                        }
                    }

                    Text(
                        text = strings.cardNetworkLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CardNetwork.entries.forEach { network ->
                            FilterChip(
                                selected = selectedNetwork == network,
                                onClick = { selectedNetwork = network },
                                label = { Text(network.label) }
                            )
                        }
                    }
                }

                AccountType.MFS -> {
                    Text(
                        text = strings.mfsProviderLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MfsProvider.entries.forEach { provider ->
                            FilterChip(
                                selected = selectedMfs == provider,
                                onClick = { selectedMfs = provider },
                                label = { Text(provider.label(language)) }
                            )
                        }
                    }
                }

                AccountType.CASH -> Unit
            }

            // Interactive Account / Card Currency Selector (opens synchronized flag+name+rate picker)
            Text(
                text = strings.accountCurrencyLabel,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Surface(
                color = if (crystal.isDark) Color(0xFF161C2B) else crystal.glassSurfaceElevated,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, crystal.glassBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { showCurrencyPicker = true }
                    .testTag("wizard_currency_selector")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
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
                                .size(38.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    MaterialTheme.colorScheme.primary.copy(
                                        alpha = if (crystal.isDark) 0.22f else 0.14f
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = selectedCurrency.flag,
                                fontSize = 20.sp
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = selectedCurrency.countryName(language),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = selectedCurrency.currencyName(language),
                                style = MaterialTheme.typography.bodySmall,
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
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = if (crystal.isDark) 0.24f else 0.12f),
                            shape = CircleShape
                        ) {
                            Text(
                                text = selectedCurrency.codeAndSymbolLabel(),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = strings.accountCurrencyLabel,
                            tint = crystal.mutedText,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            OutlinedTextField(
                value = customName,
                onValueChange = { customName = it },
                label = { Text(strings.walletNameLabel) },
                placeholder = { Text(defaultTitle) },
                supportingText = {
                    val hintText = if (language == AppLanguage.BN) {
                        "ফাঁকা রাখলে স্বয়ংক্রিয়ভাবে \"$defaultTitle\" নাম সেট হবে"
                    } else {
                        "Leave blank to auto-name as \"$defaultTitle\""
                    }
                    Text(
                        text = hintText,
                        style = MaterialTheme.typography.labelSmall,
                        color = crystal.mutedText,
                        modifier = Modifier.testTag("wizard_name_auto_hint")
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("wizard_name_input")
            )

            if (selectedType != AccountType.CASH) {
                OutlinedTextField(
                    value = digitsOrMobile,
                    onValueChange = { digitsOrMobile = it.take(14) },
                    label = { Text(strings.mobileOrLast4Label) },
                    placeholder = {
                        Text(if (selectedType == AccountType.MFS) "01711-XXXXXX" else "4829")
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (selectedType == AccountType.CREDIT_CARD) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = creditLimitText,
                        onValueChange = { creditLimitText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                        label = { Text("${strings.creditLimitLabel} (${selectedCurrency.code})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = balanceText,
                        onValueChange = { balanceText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                        label = { Text("${strings.outstandingBalanceLabel} (${selectedCurrency.code})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    )
                }

                val util = previewAccount.utilizationPercent
                val isHealthy = util <= 30
                Surface(
                    color = if (isHealthy) crystal.assetGreenBg else crystal.warningAmberBg,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${strings.creditUtilization}: ${BdtFormatter.formatPercent(util, language)}",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isHealthy) crystal.assetGreen else crystal.warningAmber
                        )
                        Text(
                            text = when {
                                util <= 30 -> strings.statusHealthy
                                util <= 70 -> strings.statusModerate
                                else -> strings.statusHigh
                            },
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (isHealthy) crystal.assetGreen else crystal.warningAmber
                        )
                    }
                }
            } else {
                OutlinedTextField(
                    value = balanceText,
                    onValueChange = { balanceText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                    label = { Text("${strings.initialBalanceLabel} (${selectedCurrency.code})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("wizard_balance_input")
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Button(
                onClick = {
                    onSaveAccount(previewAccount)
                    onDismiss()
                },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("wizard_save_account_button")
            ) {
                Text(strings.saveAccount, style = MaterialTheme.typography.titleSmall)
            }
            Spacer(modifier = Modifier.height(14.dp))
        }
    }

    if (showCountryPicker) {
        AppCountryPickerSheet(
            title = strings.bankCountryRegionLabel,
            subtitle = if (language == AppLanguage.BN) {
                "দেশ নির্বাচন করলে সংশ্লিষ্ট দেশের প্রধান ব্যাংক ও মুদ্রা স্বয়ংক্রিয়ভাবে দেখানো হবে"
            } else {
                "Selecting a country automatically loads its major banks and default currency"
            },
            selectedCountry = selectedCountry,
            language = language,
            onSelect = { chosen ->
                applySelectedCountry(chosen)
                showCountryPicker = false
            },
            onDismiss = { showCountryPicker = false }
        )
    }

    if (showCurrencyPicker) {
        AppCurrencyPickerSheet(
            title = strings.accountCurrencyLabel,
            subtitle = strings.mainCurrencySubtitle,
            selectedCurrency = selectedCurrency,
            language = language,
            referenceCurrency = AppCurrency.fromCode(defaultCurrencyCode),
            marketSnapshot = marketSnapshot,
            allowClear = false,
            onSelect = { chosen ->
                selectedCurrency = chosen
                showCurrencyPicker = false
            },
            onDismiss = { showCurrencyPicker = false }
        )
    }
}
