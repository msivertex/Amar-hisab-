package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.AccountEntity
import com.example.data.model.NetWorthSummary
import com.example.localization.AppLanguage
import com.example.localization.AppStrings
import com.example.localization.BdtFormatter
import com.example.ui.components.AccountVisualCard
import com.example.ui.components.CrystalCard
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalCrystalColors

@Composable
fun AccountsScreen(
    accounts: List<AccountEntity>,
    netWorth: NetWorthSummary,
    language: AppLanguage,
    strings: AppStrings,
    onBack: () -> Unit,
    onOpenAddWizard: () -> Unit,
    onDeleteAccount: (Long) -> Unit
) {
    BackHandler(onBack = onBack)
    val crystal = LocalCrystalColors.current

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 620.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 112.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("accounts_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = strings.close,
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        Text(
                            text = strings.accountsTitle,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    Button(
                        onClick = onOpenAddWizard,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.testTag("add_account_wizard_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.size(6.dp))
                        Text(strings.addAccount)
                    }
                }
            }

            // Net Worth Header with Green (Assets) vs. Red (Liabilities) bar
            item {
                NetWorthHeaderCard(
                    netWorth = netWorth,
                    language = language,
                    strings = strings
                )
            }

            items(accounts, key = { it.id }) { account ->
                Box(modifier = Modifier.fillMaxWidth()) {
                    AccountVisualCard(
                        account = account,
                        language = language,
                        strings = strings
                    )
                    if (accounts.size > 1) {
                        Surface(
                            color = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.36f),
                            shape = CircleShape,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 12.dp, end = 78.dp)
                        ) {
                            IconButton(
                                onClick = { onDeleteAccount(account.id) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.DeleteOutline,
                                    contentDescription = strings.deleteAccount,
                                    tint = androidx.compose.ui.graphics.Color.White,
                                    modifier = Modifier.size(18.dp)
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
fun NetWorthHeaderCard(
    netWorth: NetWorthSummary,
    language: AppLanguage,
    strings: AppStrings
) {
    val crystal = LocalCrystalColors.current
    val animatedAssetRatio by animateFloatAsState(
        targetValue = netWorth.assetRatio,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "netWorthRatio"
    )

    CrystalCard(
        modifier = Modifier.fillMaxWidth(),
        elevated = true
    ) {
        Text(
            text = strings.netWorth,
            style = MaterialTheme.typography.labelMedium,
            color = crystal.mutedText
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = BdtFormatter.format(netWorth.netWorth, language),
            style = MaterialTheme.typography.headlineLarge.copy(
                fontFamily = JetBrainsMonoFamily,
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Visual Green (Assets) vs Red (Liabilities) Ratio Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(CircleShape)
                .background(crystal.liabilityRed.copy(alpha = 0.25f))
        ) {
            if (animatedAssetRatio > 0f) {
                Box(
                    modifier = Modifier
                        .weight(animatedAssetRatio.coerceAtLeast(0.02f))
                        .height(12.dp)
                        .background(crystal.assetGreen)
                )
            }
            val liabilityRatio = (1f - animatedAssetRatio)
            if (liabilityRatio > 0.01f) {
                Box(
                    modifier = Modifier
                        .weight(liabilityRatio.coerceAtLeast(0.02f))
                        .height(12.dp)
                        .background(crystal.liabilityRed)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(crystal.assetGreen)
                    )
                    Text(
                        text = strings.assetsLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = crystal.mutedText
                    )
                }
                Text(
                    text = BdtFormatter.format(netWorth.totalAssets, language),
                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFamily),
                    color = crystal.assetGreen
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(crystal.liabilityRed)
                    )
                    Text(
                        text = strings.liabilitiesLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = crystal.mutedText
                    )
                }
                Text(
                    text = BdtFormatter.format(netWorth.totalLiabilities, language),
                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFamily),
                    color = crystal.liabilityRed
                )
            }
        }
    }
}
