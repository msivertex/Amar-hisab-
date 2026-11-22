package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
import com.example.data.model.AppCurrency
import com.example.data.model.BankRegion
import com.example.data.model.BankSubtype
import com.example.data.model.CardNetwork
import com.example.data.model.InternationalBankCatalog
import com.example.data.model.MfsProvider
import com.example.data.model.defaultColorForCash
import com.example.localization.AppLanguage
import com.example.localization.AppStrings
import com.example.localization.BdtFormatter
import com.example.ui.components.AccountVisualCard
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
    marketSnapshot: MarketSnapshot = MarketSnapshot.defaultReference()
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val crystal = LocalCrystalColors.current

    val initialCurrency = remember(defaultCurrencyCode, secondaryCurrencyCode) {
        AppCurrency.fromCodeOrNull(secondaryCurrencyCode) ?: AppCurrency.fromCode(defaultCurrencyCode)
    }
    val initialRegion = remember(initialCurrency) {
        BankRegion.preferredForCurrency(initialCurrency)
    }

    var selectedType by remember { mutableStateOf(AccountType.DEBIT_BANK) }
    var selectedRegion by remember { mutableStateOf(initialRegion) }
    var selectedBank by remember {
        mutableStateOf(
            InternationalBankCatalog.banksForRegion(initialRegion).firstOrNull()
                ?: InternationalBankCatalog.all.first()
        )
    }
    var selectedCurrency by remember { mutableStateOf(initialRegion.defaultCurrency) }
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

    val previewColor = when (selectedType) {
        AccountType.DEBIT_BANK, AccountType.CREDIT_CARD -> selectedBank.brandColor
        AccountType.MFS -> selectedMfs.brandColor
        AccountType.CASH -> defaultColorForCash()
    }

    val resolvedProvider = when (selectedType) {
        AccountType.DEBIT_BANK, AccountType.CREDIT_CARD -> selectedBank.label(language)
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
        contentColor = MaterialTheme.colorScheme.onSurface
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
                compact = true
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
                                AccountType.DEBIT_BANK, AccountType.CREDIT_CARD -> selectedRegion.defaultCurrency
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
                    // Country / Region Picker (Bangladesh + 8 international expat hubs)
                    Text(
                        text = strings.bankCountryRegionLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BankRegion.entries.forEach { region ->
                            FilterChip(
                                selected = selectedRegion == region,
                                onClick = {
                                    selectedRegion = region
                                    val regionBanks = InternationalBankCatalog.banksForRegion(region)
                                    if (regionBanks.isNotEmpty()) {
                                        selectedBank = regionBanks.first()
                                    }
                                    selectedCurrency = region.defaultCurrency
                                },
                                label = { Text("${region.flag} ${region.label(language)}") },
                                modifier = Modifier.testTag("wizard_region_${region.name}")
                            )
                        }
                    }

                    // Bank Selection for active Country / Region
                    Text(
                        text = strings.selectBank,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    val regionBanks = remember(selectedRegion) {
                        InternationalBankCatalog.banksForRegion(selectedRegion)
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        regionBanks.forEach { bank ->
                            FilterChip(
                                selected = selectedBank.id == bank.id,
                                onClick = {
                                    selectedBank = bank
                                    selectedCurrency = bank.defaultCurrency
                                },
                                label = { Text(bank.label(language)) },
                                modifier = Modifier.testTag("wizard_bank_${bank.id}")
                            )
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

            // Interactive Account / Card Currency Selector (opens full flag+name picker)
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
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "${selectedCurrency.code} (${selectedCurrency.symbol})",
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
