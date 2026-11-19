package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.AccountEntity
import com.example.data.model.AccountType
import com.example.data.model.BangladeshiBank
import com.example.data.model.BankSubtype
import com.example.data.model.CardNetwork
import com.example.data.model.MfsProvider
import com.example.data.model.defaultColorForCash
import com.example.localization.AppLanguage
import com.example.localization.AppStrings
import com.example.localization.BdtFormatter
import com.example.ui.components.AccountVisualCard
import com.example.ui.theme.LocalCrystalColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAccountWizardSheet(
    language: AppLanguage,
    strings: AppStrings,
    onDismiss: () -> Unit,
    onSaveAccount: (AccountEntity) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val crystal = LocalCrystalColors.current

    var selectedType by remember { mutableStateOf(AccountType.DEBIT_BANK) }
    var selectedBank by remember { mutableStateOf(BangladeshiBank.CITY_BANK) }
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

    val defaultTitle = when (selectedType) {
        AccountType.DEBIT_BANK -> "${selectedBank.label(language)} ${selectedSubtype.label(language)}"
        AccountType.CREDIT_CARD -> "${selectedBank.label(language)} ${selectedNetwork.label}"
        AccountType.MFS -> "${selectedMfs.label(language)} Wallet"
        AccountType.CASH -> if (language == AppLanguage.BN) "নগদ ওয়ালেট" else "Cash Wallet"
    }

    val previewAccount = AccountEntity(
        name = customName.ifBlank { defaultTitle },
        type = selectedType,
        balance = parsedBalance,
        creditLimit = if (selectedType == AccountType.CREDIT_CARD) parsedLimit else 0.0,
        bankOrProvider = when (selectedType) {
            AccountType.DEBIT_BANK, AccountType.CREDIT_CARD -> selectedBank.label(language)
            AccountType.MFS -> selectedMfs.label(language)
            AccountType.CASH -> if (language == AppLanguage.BN) "নগদ টাকা" else "Physical Cash"
        },
        accountSubtype = if (selectedType == AccountType.DEBIT_BANK) selectedSubtype.label(language) else "",
        cardNetwork = if (selectedType == AccountType.DEBIT_BANK || selectedType == AccountType.CREDIT_CARD) {
            selectedNetwork.label
        } else "",
        accountNumberLast4 = digitsOrMobile.ifBlank {
            when (selectedType) {
                AccountType.MFS -> "017XX-XXXX"
                AccountType.CASH -> "BDT"
                else -> "4829"
            }
        },
        colorArgb = previewColor.toArgb().toLong() and 0xFFFFFFFFL
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
                        onClick = { selectedType = type },
                        label = { Text(label) },
                        modifier = Modifier.testTag("wizard_type_${type.name}")
                    )
                }
            }

            // Conditional Wizard Fields per Account Type
            when (selectedType) {
                AccountType.DEBIT_BANK, AccountType.CREDIT_CARD -> {
                    Text(
                        text = strings.selectBank,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BangladeshiBank.entries.forEach { bank ->
                            FilterChip(
                                selected = selectedBank == bank,
                                onClick = { selectedBank = bank },
                                label = { Text(bank.label(language)) }
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

            OutlinedTextField(
                value = customName,
                onValueChange = { customName = it },
                label = { Text(strings.walletNameLabel) },
                placeholder = { Text(defaultTitle) },
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
                        label = { Text(strings.creditLimitLabel) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = balanceText,
                        onValueChange = { balanceText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                        label = { Text(strings.outstandingBalanceLabel) },
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
                    label = { Text(strings.initialBalanceLabel) },
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
}
