package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.UserPrefs
import com.example.data.market.MarketSnapshot
import com.example.data.model.AppCurrency
import com.example.data.model.AuthMode
import com.example.data.model.BankCategory
import com.example.data.model.InternationalBank
import com.example.data.model.InternationalBankCatalog
import com.example.data.model.InternationalMfsCatalog
import com.example.localization.AppLanguage
import com.example.localization.AppStrings
import com.example.ui.components.AppCountryPickerSheet
import com.example.ui.components.AppCurrencyPickerSheet
import com.example.ui.components.CrystalCard
import com.example.ui.components.GoogleGLogo
import com.example.ui.components.UserProfileAvatar
import com.example.ui.theme.LocalCrystalColors
import com.example.localization.BdtFormatter

@Composable
fun DetailedProfileSubPage(
    prefs: UserPrefs,
    strings: AppStrings,
    isGoogleSignInLoading: Boolean,
    googleNotice: String?,
    marketSnapshot: MarketSnapshot = MarketSnapshot.defaultReference(),
    onBack: () -> Unit,
    onOpenAvatarSelector: () -> Unit,
    onUpdateDisplayName: (String) -> Unit,
    onTriggerGoogleSignIn: () -> Unit,
    onUnlinkGoogleAccount: () -> Unit,
    onOpenBackupRestore: () -> Unit = {},
    onSetUserCountry: (String) -> Unit = {},
    onSetMainCurrency: (String) -> Unit = {},
    onSetSecondaryCurrency: (String) -> Unit = {},
    onLogOut: () -> Unit = {}
) {
    BackHandler(onBack = onBack)
    val crystal = LocalCrystalColors.current
    val lang = prefs.language
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val effectiveName = prefs.userName.ifBlank { strings.profileDetails.guestUserFallback }
    var showEditNameDialog by remember { mutableStateOf(false) }
    var draftName by remember(prefs.userName) { mutableStateOf(prefs.userName) }
    var showNameError by remember { mutableStateOf(false) }

    var showCountryPicker by remember { mutableStateOf(false) }
    var showMainCurrencyPicker by remember { mutableStateOf(false) }
    var showSecondaryCurrencyPicker by remember { mutableStateOf(false) }
    var showBankDirectoryDialog by remember { mutableStateOf(false) }
    var showMfsDirectoryDialog by remember { mutableStateOf(false) }

    fun commitDisplayName() {
        val trimmed = draftName.trim()
        if (trimmed.isEmpty()) {
            showNameError = true
            return
        }
        onUpdateDisplayName(trimmed)
        showNameError = false
        showEditNameDialog = false
        focusManager.clearFocus()
        keyboardController?.hide()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("detailed_profile_screen"),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 620.dp)
                .fillMaxSize()
                .testTag("detailed_profile_list"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Top App Bar
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("detailed_profile_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = strings.close,
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Text(
                        text = strings.profileDetails.detailedProfileTitle,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            // 1. Floating Profile Avatar & Header with interactive pencil badge
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .testTag("detailed_profile_hero_card"),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier.size(118.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(106.dp)
                                .shadow(
                                    elevation = if (crystal.isDark) 16.dp else 10.dp,
                                    shape = CircleShape,
                                    ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                                    spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
                                )
                                .clip(CircleShape)
                                .background(
                                    if (crystal.isDark) Color(0xFF161C2B) else Color.White.copy(alpha = 0.85f)
                                )
                                .border(
                                    width = 2.5.dp,
                                    brush = Brush.linearGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.secondary
                                        )
                                    ),
                                    shape = CircleShape
                                )
                                .clickable(onClick = onOpenAvatarSelector)
                                .padding(4.dp)
                                .testTag("detailed_profile_avatar_tap"),
                            contentAlignment = Alignment.Center
                        ) {
                            UserProfileAvatar(
                                userName = effectiveName,
                                avatarUrl = prefs.effectiveAvatarUrl,
                                authMode = prefs.authMode,
                                size = 96.dp,
                                contentDescription = strings.chooseProfilePhotoTitle,
                                showBorder = false
                            )
                        }

                        // Interactive Pencil Edit Badge anchored to bottom-right (48dp touch target)
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(48.dp)
                                .clip(CircleShape)
                                .clickable(onClick = onOpenAvatarSelector)
                                .testTag("detailed_profile_avatar_edit_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                border = BorderStroke(
                                    width = 2.dp,
                                    color = if (crystal.isDark) Color(0xFF0B0F19) else Color.White
                                ),
                                shadowElevation = 6.dp,
                                modifier = Modifier.size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Filled.Edit,
                                        contentDescription = strings.changeProfilePhoto,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Spacer(modifier = Modifier.width(34.dp))
                        Text(
                            text = effectiveName,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .testTag("detailed_profile_hero_name")
                        )
                        IconButton(
                            onClick = {
                                draftName = prefs.userName
                                showNameError = false
                                showEditNameDialog = true
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("detailed_profile_edit_name_button")
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary.copy(
                                    alpha = if (crystal.isDark) 0.18f else 0.10f
                                ),
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.primary.copy(
                                        alpha = if (crystal.isDark) 0.34f else 0.24f
                                    )
                                ),
                                modifier = Modifier.size(28.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Filled.Edit,
                                        contentDescription = strings.editNameDesc,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .size(14.dp)
                                            .testTag("detailed_profile_hero_name_edit_icon")
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2. Adaptive Google Account Card (Signed-in Gmail + Switch to Offline Guest, or Guest Mode + Google Sign In)
            item {
                CrystalCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("detailed_profile_google_card"),
                    elevated = true
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (crystal.isDark) Color(0xFF1B2335) else Color.White.copy(alpha = 0.85f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            GoogleGLogo(size = 20.dp)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.googleAccountRowTitle,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (prefs.authMode == AuthMode.GOOGLE) {
                                    prefs.userEmail.ifBlank { strings.profileDetails.googleStatusSignedIn }
                                } else {
                                    strings.profileDetails.googleStatusNotSignedIn
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (prefs.authMode == AuthMode.GOOGLE) {
                                    crystal.assetGreen
                                } else {
                                    crystal.mutedText
                                },
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.testTag("detailed_profile_google_email")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (prefs.authMode == AuthMode.GUEST) {
                        Button(
                            onClick = onTriggerGoogleSignIn,
                            enabled = !isGoogleSignInLoading,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("detailed_profile_google_signin_button")
                        ) {
                            if (isGoogleSignInLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                            } else {
                                GoogleGLogo(size = 18.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = strings.profileDetails.googleSignInShortBtn,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    } else {
                        OutlinedButton(
                            onClick = onUnlinkGoogleAccount,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, crystal.glassBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("detailed_profile_unlink_google_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Logout,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = strings.unlinkGoogleAccountBtn,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    if (googleNotice != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = googleNotice,
                            style = MaterialTheme.typography.bodySmall,
                            color = crystal.warningAmber
                        )
                    }
                }
            }

            // 4. Interactive Region & Currencies (Country, Main Currency, Second Currency)
            item {
                val selectedCountry = prefs.userCountry
                val activeMainCurrency = prefs.mainCurrency
                val activeSecondaryCurrency = prefs.secondaryCurrency
                CrystalCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("detailed_profile_region_currency_card"),
                    elevated = true
                ) {
                    Text(
                        text = strings.profileDetails.accountRegionSectionTitle,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    ProfileInfoDetailRow(
                        icon = Icons.Filled.Public,
                        label = strings.profileDetails.countryLabel,
                        value = "${selectedCountry.flag} ${selectedCountry.label(lang)}",
                        isHighlighted = true,
                        testTag = "detailed_profile_country_row",
                        onClick = { showCountryPicker = true }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = crystal.glassBorder
                    )
                    ProfileInfoDetailRow(
                        icon = Icons.Filled.Payments,
                        label = strings.mainCurrencyLabel,
                        value = activeMainCurrency.fullDisplay(lang),
                        isHighlighted = true,
                        testTag = "detailed_profile_main_currency_row",
                        onClick = { showMainCurrencyPicker = true }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = crystal.glassBorder
                    )
                    ProfileInfoDetailRow(
                        icon = Icons.Filled.CurrencyExchange,
                        label = strings.secondaryCurrencyLabel,
                        value = activeSecondaryCurrency?.fullDisplay(lang) ?: strings.secondaryCurrencyNotSet,
                        isHighlighted = activeSecondaryCurrency != null,
                        testTag = "detailed_profile_secondary_currency_row",
                        onClick = { showSecondaryCurrencyPicker = true }
                    )
                }
            }

            // 5. "ব্যাংক ও এমএফএস ডাটাবেস" (Bank & MFS Database) section — synced to selected Country
            item {
                val selectedCountry = prefs.userCountry
                val dynamicBankSubtitle = remember(selectedCountry, lang) {
                    InternationalMfsCatalog.bankDirectorySubtitle(selectedCountry, lang)
                }
                val dynamicMfsSubtitle = remember(selectedCountry, lang) {
                    InternationalMfsCatalog.mfsDirectorySubtitle(selectedCountry, lang)
                }

                CrystalCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("detailed_profile_bank_mfs_db_card"),
                    elevated = true
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Storage,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = strings.profileDetails.bankMfsDatabaseTitle,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = if (crystal.isDark) 0.22f else 0.12f),
                            border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                            modifier = Modifier.testTag("bank_mfs_db_country_badge")
                        ) {
                            Text(
                                text = "${selectedCountry.flag} ${selectedCountry.label(lang)}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    // Local Bank Directory Row + "আপডেট দেখুন" button
                    DatabaseDirectoryRow(
                        icon = Icons.Filled.AccountBalance,
                        title = strings.profileDetails.localBankDirectoryTitle,
                        subtitle = dynamicBankSubtitle,
                        buttonLabel = strings.profileDetails.checkUpdatesBtn,
                        testTag = "bank_directory_updates_button",
                        subtitleTestTag = "bank_directory_subtitle",
                        onClick = { showBankDirectoryDialog = true }
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 10.dp),
                        color = crystal.glassBorder
                    )

                    // MFS Provider Directory Row + "ব্রাউজ করুন" button
                    DatabaseDirectoryRow(
                        icon = Icons.Filled.PhoneAndroid,
                        title = strings.profileDetails.mfsDirectoryTitle,
                        subtitle = dynamicMfsSubtitle,
                        buttonLabel = strings.profileDetails.browseBtn,
                        testTag = "mfs_directory_browse_button",
                        subtitleTestTag = "mfs_directory_subtitle",
                        onClick = { showMfsDirectoryDialog = true }
                    )
                }
            }

            // 6. Dedicated Log Out button at the very bottom of Profile Details
            item {
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedButton(
                    onClick = onLogOut,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.2.dp, crystal.liabilityRed.copy(alpha = 0.55f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = crystal.liabilityRedBg.copy(alpha = if (crystal.isDark) 0.45f else 0.72f),
                        contentColor = crystal.liabilityRed
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .testTag("detailed_profile_logout_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = "Log Out",
                        tint = crystal.liabilityRed,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Log Out",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = crystal.liabilityRed
                    )
                }
            }
        }
    }

    // Interactive Country Picker Sheet
    if (showCountryPicker) {
        AppCountryPickerSheet(
            title = strings.profileDetails.countryLabel,
            subtitle = strings.profileDetails.regionDialogNote,
            selectedCountry = prefs.userCountry,
            language = lang,
            onSelect = { selected ->
                onSetUserCountry(selected.code)
                showCountryPicker = false
            },
            onDismiss = { showCountryPicker = false }
        )
    }

    // Interactive Main Currency Picker Sheet
    if (showMainCurrencyPicker) {
        AppCurrencyPickerSheet(
            title = strings.mainCurrencyLabel,
            subtitle = strings.mainCurrencySubtitle,
            selectedCurrency = prefs.mainCurrency,
            language = lang,
            referenceCurrency = prefs.secondaryCurrency ?: AppCurrency.BDT,
            marketSnapshot = marketSnapshot,
            allowClear = false,
            onSelect = { selected ->
                onSetMainCurrency(selected.code)
                showMainCurrencyPicker = false
            },
            onDismiss = { showMainCurrencyPicker = false }
        )
    }

    // Interactive Second Currency Picker Sheet (supports search & clear)
    if (showSecondaryCurrencyPicker) {
        AppCurrencyPickerSheet(
            title = strings.secondaryCurrencyLabel,
            subtitle = strings.secondaryCurrencySubtitle,
            selectedCurrency = prefs.secondaryCurrency,
            language = lang,
            referenceCurrency = prefs.mainCurrency,
            marketSnapshot = marketSnapshot,
            allowClear = true,
            onSelect = { selected ->
                onSetSecondaryCurrency(selected.code)
                showSecondaryCurrencyPicker = false
            },
            onClear = {
                onSetSecondaryCurrency("")
                showSecondaryCurrencyPicker = false
            },
            onDismiss = { showSecondaryCurrencyPicker = false }
        )
    }

    // Country-Synced Local Bank Directory Dialog
    if (showBankDirectoryDialog) {
        val selectedCountry = prefs.userCountry
        val countryBanks = remember(selectedCountry) {
            InternationalBankCatalog.banksForCountry(selectedCountry)
        }
        val governmentBanks = remember(countryBanks) {
            countryBanks.filter { it.category == BankCategory.GOVERNMENT }
        }
        val privateBanks = remember(countryBanks) {
            countryBanks.filter { it.category == BankCategory.PRIVATE }
        }
        val hasBothCategories = governmentBanks.isNotEmpty() && privateBanks.isNotEmpty()

        AlertDialog(
            onDismissRequest = { showBankDirectoryDialog = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurface,
            icon = {
                Icon(
                    imageVector = Icons.Filled.AccountBalance,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = {
                Text(
                    "${selectedCountry.flag} ${strings.profileDetails.localBankDirectoryTitle} (${BdtFormatter.formatNumber(countryBanks.size, lang)})"
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState())
                        .testTag("bank_directory_dialog_content"),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = crystal.assetGreenBg,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = crystal.assetGreen,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "${selectedCountry.label(lang)} (${selectedCountry.suggestedCurrency.code}) • ${strings.profileDetails.databaseUpToDateNote}",
                                style = MaterialTheme.typography.labelMedium,
                                color = crystal.assetGreen
                            )
                        }
                    }

                    if (hasBothCategories) {
                        Text(
                            text = "${BankCategory.GOVERNMENT.label(lang)} (${BdtFormatter.formatNumber(governmentBanks.size, lang)})",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        governmentBanks.forEach { bank ->
                            BankDirectoryItemCard(bank = bank, lang = lang)
                        }

                        Text(
                            text = "${BankCategory.PRIVATE.label(lang)} (${BdtFormatter.formatNumber(privateBanks.size, lang)})",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                        privateBanks.forEach { bank ->
                            BankDirectoryItemCard(bank = bank, lang = lang)
                        }
                    } else {
                        Text(
                            text = "${selectedCountry.label(lang)} (${BdtFormatter.formatNumber(countryBanks.size, lang)})",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        countryBanks.forEach { bank ->
                            BankDirectoryItemCard(bank = bank, lang = lang)
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showBankDirectoryDialog = false }) {
                    Text(strings.close)
                }
            }
        )
    }

    // Country-Synced MFS & Digital Wallet Provider Directory Dialog
    if (showMfsDirectoryDialog) {
        val selectedCountry = prefs.userCountry
        val countryProviders = remember(selectedCountry) {
            InternationalMfsCatalog.providersForCountry(selectedCountry)
        }
        AlertDialog(
            onDismissRequest = { showMfsDirectoryDialog = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurface,
            icon = {
                Icon(
                    imageVector = Icons.Filled.PhoneAndroid,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = {
                Text(
                    "${selectedCountry.flag} ${strings.profileDetails.mfsDirectoryTitle} (${BdtFormatter.formatNumber(countryProviders.size, lang)})"
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState())
                        .testTag("mfs_directory_dialog_content"),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    countryProviders.forEach { provider ->
                        Surface(
                            color = if (crystal.isDark) Color(0xFF161C2B) else crystal.glassSurfaceElevated,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(0.8.dp, crystal.glassBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.linearGradient(
                                                listOf(provider.brandColor, provider.secondaryColor)
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = provider.en.take(1).uppercase(),
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = provider.directoryLabel(lang),
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${selectedCountry.label(lang)} • ${strings.typeMfs}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = crystal.mutedText
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showMfsDirectoryDialog = false }) {
                    Text(strings.close)
                }
            }
        )
    }

    if (showEditNameDialog) {
        AlertDialog(
            onDismissRequest = {
                draftName = prefs.userName
                showNameError = false
                showEditNameDialog = false
                focusManager.clearFocus()
                keyboardController?.hide()
            },
            modifier = Modifier.testTag("detailed_profile_edit_name_dialog"),
            containerColor = if (crystal.isDark) Color(0xFF101522) else Color.White,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    text = strings.editNameDesc,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedTextField(
                        value = draftName,
                        onValueChange = {
                            if (it.length <= 40) {
                                draftName = it
                                showNameError = false
                            }
                        },
                        label = { Text(strings.yourNameLabel) },
                        isError = showNameError,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { commitDisplayName() }),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = if (crystal.isDark) Color.White.copy(alpha = 0.24f) else crystal.glassBorder,
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor = crystal.mutedText,
                            cursorColor = MaterialTheme.colorScheme.primary,
                            focusedContainerColor = crystal.glassSurfaceElevated,
                            unfocusedContainerColor = crystal.glassSurfaceElevated
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("detailed_profile_name_input")
                    )

                    if (showNameError) {
                        Text(
                            text = strings.nameEmptyError,
                            style = MaterialTheme.typography.bodySmall,
                            color = crystal.liabilityRed,
                            modifier = Modifier
                                .padding(start = 4.dp)
                                .testTag("detailed_profile_name_error")
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { commitDisplayName() },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("detailed_profile_name_save_button")
                ) {
                    Text(
                        text = strings.saveChanges,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        draftName = prefs.userName
                        showNameError = false
                        showEditNameDialog = false
                        focusManager.clearFocus()
                        keyboardController?.hide()
                    },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, crystal.glassBorder),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("detailed_profile_name_cancel_button")
                ) {
                    Text(
                        text = strings.cancel,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium)
                    )
                }
            }
        )
    }
}

@Composable
private fun BankDirectoryItemCard(
    bank: InternationalBank,
    lang: AppLanguage
) {
    val crystal = LocalCrystalColors.current
    Surface(
        color = if (crystal.isDark) Color(0xFF161C2B) else crystal.glassSurfaceElevated,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(0.8.dp, crystal.glassBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(bank.brandColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = bank.shortCode.take(2),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    ),
                    color = Color.White
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = bank.label(lang),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${bank.shortCode} • ${bank.defaultCurrency.code}",
                    style = MaterialTheme.typography.labelSmall,
                    color = crystal.mutedText
                )
            }
        }
    }
}

@Composable
private fun ProfileInfoDetailRow(
    icon: ImageVector,
    label: String,
    value: String,
    isHighlighted: Boolean = false,
    testTag: String? = null,
    onClick: () -> Unit
) {
    val crystal = LocalCrystalColors.current
    val primary = MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .let { mod -> if (testTag != null) mod.testTag(testTag) else mod }
            .padding(horizontal = 4.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(primary.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = primary,
                modifier = Modifier.size(18.dp)
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = if (isHighlighted) {
                    primary.copy(alpha = if (crystal.isDark) 0.22f else 0.11f)
                } else if (crystal.isDark) {
                    Color.White.copy(alpha = 0.08f)
                } else {
                    crystal.glassSurfaceElevated
                },
                border = BorderStroke(
                    width = 0.8.dp,
                    color = if (isHighlighted) primary.copy(alpha = 0.35f) else crystal.glassBorder
                )
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (isHighlighted) primary else crystal.mutedText,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = label,
                tint = crystal.mutedText,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun DatabaseDirectoryRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    buttonLabel: String,
    testTag: String,
    subtitleTestTag: String? = null,
    onClick: () -> Unit
) {
    val crystal = LocalCrystalColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(19.dp)
                )
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = crystal.mutedText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = if (subtitleTestTag != null) Modifier.testTag(subtitleTestTag) else Modifier
                )
            }
        }

        FilledTonalButton(
            onClick = onClick,
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            modifier = Modifier
                .heightIn(min = 48.dp)
                .testTag(testTag)
        ) {
            Text(
                text = buttonLabel,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1
            )
        }
    }
}
