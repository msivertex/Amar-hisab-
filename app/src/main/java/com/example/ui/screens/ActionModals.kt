package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.data.local.AccountEntity
import com.example.data.local.TransactionEntity
import com.example.data.model.ExpenseCategory
import com.example.data.model.SmartAlert
import com.example.data.model.TransactionType
import com.example.localization.AppLanguage
import com.example.localization.AppStrings
import com.example.localization.BdtFormatter
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalCrystalColors
import com.example.ui.viewmodel.ReceiptScanUiState
import com.example.ui.viewmodel.TtsUiState
import com.example.ui.viewmodel.VoiceParseUiState
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptScanSheet(
    accounts: List<AccountEntity>,
    receiptState: ReceiptScanUiState,
    language: AppLanguage,
    strings: AppStrings,
    onPickImageUri: (Uri) -> Unit,
    onSaveExpense: (title: String, amount: Double, category: ExpenseCategory, accountId: Long, note: String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val crystal = LocalCrystalColors.current
    val context = LocalContext.current
    var pickedUri by remember { mutableStateOf<Uri?>(null) }
    var pendingCameraUriString by rememberSaveable { mutableStateOf<String?>(null) }
    var cameraNotice by remember { mutableStateOf<String?>(null) }

    var editableMerchant by remember { mutableStateOf("") }
    var editableAmount by remember { mutableStateOf("") }
    var editableCategory by remember { mutableStateOf(ExpenseCategory.OTHER) }
    var editableSummary by remember { mutableStateOf("") }
    var selectedAccountId by remember(accounts) { mutableStateOf(accounts.firstOrNull()?.id ?: 0L) }

    LaunchedEffect(receiptState) {
        if (receiptState is ReceiptScanUiState.Success) {
            editableMerchant = receiptState.result.merchantName
            editableAmount = receiptState.result.amountBdt.let {
                if (it > 0) it.toLong().toString() else ""
            }
            editableCategory = receiptState.result.category
            editableSummary = receiptState.result.itemsSummary
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                cameraNotice = null
                pickedUri = uri
                onPickImageUri(uri)
            }
        }
    )

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { success ->
            val captured = pendingCameraUriString?.let(Uri::parse)
            if (success && captured != null) {
                cameraNotice = null
                pickedUri = captured
                onPickImageUri(captured)
            }
        }
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.DocumentScanner,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Column {
                    Text(text = strings.receiptScanTitle, style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = strings.receiptScanSubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = crystal.mutedText
                    )
                }
            }

            // Dual Scan Options: Live Camera Capture + Gallery Upload (Crystal Liquid Frosted-Glass Tiles)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ReceiptSourceActionTile(
                    label = strings.scanWithCamera,
                    icon = Icons.Filled.PhotoCamera,
                    highlighted = true,
                    testTag = "scan_receipt_camera_button",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        val outputUri = runCatching { createReceiptCaptureUri(context) }.getOrNull()
                        if (outputUri == null) {
                            cameraNotice = strings.cameraUnavailableNotice
                        } else {
                            pendingCameraUriString = outputUri.toString()
                            val launched = runCatching { cameraLauncher.launch(outputUri) }.isSuccess
                            if (!launched) {
                                cameraNotice = strings.cameraUnavailableNotice
                            }
                        }
                    }
                )

                ReceiptSourceActionTile(
                    label = strings.pickReceiptPhoto,
                    icon = Icons.Filled.PhotoLibrary,
                    highlighted = false,
                    testTag = "pick_receipt_photo_button",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                )
            }

            if (cameraNotice != null) {
                Surface(
                    color = crystal.warningAmberBg,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = cameraNotice!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (crystal.isDark) Color.White else crystal.warningAmber,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            if (pickedUri != null) {
                AsyncImage(
                    model = pickedUri,
                    contentDescription = "Picked Receipt Photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, crystal.glassBorder, RoundedCornerShape(16.dp))
                )
            }

            when (receiptState) {
                is ReceiptScanUiState.Idle -> Unit
                is ReceiptScanUiState.Loading -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp)
                        Spacer(modifier = Modifier.size(10.dp))
                        Text(strings.analyzingReceipt, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                is ReceiptScanUiState.Error -> {
                    Surface(
                        color = crystal.liabilityRedBg,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = receiptState.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = crystal.liabilityRed,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                is ReceiptScanUiState.Success -> {
                    OutlinedTextField(
                        value = editableMerchant,
                        onValueChange = { editableMerchant = it },
                        label = { Text(strings.detectedMerchant) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editableAmount,
                        onValueChange = { editableAmount = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text(strings.detectedAmount) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (editableSummary.isNotBlank()) {
                        OutlinedTextField(
                            value = editableSummary,
                            onValueChange = { editableSummary = it },
                            label = { Text(strings.itemsSummary) },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Text(text = strings.detectedCategory, style = MaterialTheme.typography.labelLarge)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ExpenseCategory.entries.filter {
                            it != ExpenseCategory.SALARY_INCOME && it != ExpenseCategory.TRANSFER
                        }.forEach { cat ->
                            FilterChip(
                                selected = editableCategory == cat,
                                onClick = { editableCategory = cat },
                                label = { Text(cat.label(language)) }
                            )
                        }
                    }

                    Text(text = strings.selectWalletLabel, style = MaterialTheme.typography.labelLarge)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        accounts.forEach { acc ->
                            FilterChip(
                                selected = selectedAccountId == acc.id,
                                onClick = { selectedAccountId = acc.id },
                                label = { Text(acc.name) }
                            )
                        }
                    }

                    val amountVal = editableAmount.toDoubleOrNull() ?: 0.0
                    Button(
                        onClick = {
                            onSaveExpense(
                                editableMerchant.ifBlank { editableCategory.label(language) },
                                amountVal,
                                editableCategory,
                                selectedAccountId,
                                editableSummary
                            )
                            onDismiss()
                        },
                        enabled = amountVal > 0.0 && selectedAccountId > 0L,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Text(strings.saveScannedExpense)
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun ReceiptSourceActionTile(
    label: String,
    icon: ImageVector,
    highlighted: Boolean,
    testTag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val crystal = LocalCrystalColors.current
    val shape = RoundedCornerShape(18.dp)
    val bgBrush = when {
        highlighted && crystal.isDark -> Brush.linearGradient(
            listOf(
                MaterialTheme.colorScheme.primaryContainer,
                MaterialTheme.colorScheme.surfaceVariant
            )
        )
        highlighted -> Brush.linearGradient(
            listOf(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                crystal.glassSurfaceElevated
            )
        )
        else -> Brush.linearGradient(
            listOf(crystal.glassSurfaceElevated, crystal.glassSurface)
        )
    }
    val borderColor = if (highlighted) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.72f)
    } else {
        crystal.glassBorder
    }

    Column(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .heightIn(min = 96.dp)
            .clip(shape)
            .background(bgBrush)
            .border(1.2.dp, borderColor, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 14.dp)
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = if (crystal.isDark) 0.22f else 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun createReceiptCaptureUri(context: Context): Uri {
    val dir = File(context.cacheDir, "receipts").apply { mkdirs() }
    val imageFile = File(dir, "receipt_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", imageFile)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceEntrySheet(
    accounts: List<AccountEntity>,
    voiceState: VoiceParseUiState,
    ttsState: TtsUiState,
    language: AppLanguage,
    strings: AppStrings,
    onParseUtterance: (String) -> Unit,
    onPlayBriefingTts: () -> Unit,
    onStopTts: () -> Unit,
    onSaveParsedTransaction: (title: String, amount: Double, type: TransactionType, category: ExpenseCategory, accountId: Long) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val crystal = LocalCrystalColors.current
    var utterance by remember { mutableStateOf("") }
    var micNotice by remember { mutableStateOf<String?>(null) }

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                utterance = spoken
                onParseUtterance(spoken)
            }
        }
    }

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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = strings.voiceEntryTitle, style = MaterialTheme.typography.titleLarge)
            Text(
                text = strings.voiceEntrySubtitle,
                style = MaterialTheme.typography.bodySmall,
                color = crystal.mutedText
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            putExtra(
                                RecognizerIntent.EXTRA_LANGUAGE,
                                if (language == AppLanguage.BN) "bn-BD" else "en-US"
                            )
                        }
                        try {
                            speechLauncher.launch(intent)
                        } catch (_: Exception) {
                            micNotice = if (language == AppLanguage.BN) {
                                "ভয়েস ইনপুট পাওয়া যায়নি—নিচে টাইপ করে স্মার্ট বিশ্লেষণ ব্যবহার করুন।"
                            } else {
                                "Device speech recognizer unavailable — type naturally below to parse."
                            }
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Mic, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(strings.tapMicToSpeak, maxLines = 1)
                }

                OutlinedButton(
                    onClick = {
                        if (ttsState is TtsUiState.Playing) onStopTts() else onPlayBriefingTts()
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.GraphicEq, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(
                        if (ttsState is TtsUiState.Playing) strings.stopAudio else strings.listenBriefingTts,
                        maxLines = 1
                    )
                }
            }

            if (micNotice != null) {
                Text(
                    text = micNotice!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = crystal.warningAmber
                )
            }

            OutlinedTextField(
                value = utterance,
                onValueChange = { utterance = it },
                placeholder = { Text(strings.voiceInputHint) },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = { onParseUtterance(utterance) },
                enabled = utterance.isNotBlank() && voiceState !is VoiceParseUiState.Loading,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (voiceState is VoiceParseUiState.Loading) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(strings.parseEntryBtn)
                }
            }

            when (voiceState) {
                is VoiceParseUiState.Success -> {
                    val parsed = voiceState.result
                    val matchedAccount = accounts.firstOrNull { acc ->
                        parsed.accountHint.isNotBlank() &&
                            (acc.name.contains(parsed.accountHint, true) ||
                                acc.bankOrProvider.contains(parsed.accountHint, true))
                    } ?: accounts.firstOrNull()

                    Surface(
                        color = crystal.assetGreenBg,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "${parsed.title} • ${BdtFormatter.format(parsed.amountBdt, language)}",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${parsed.category.label(language)} • ${matchedAccount?.name.orEmpty()}",
                                style = MaterialTheme.typography.bodySmall,
                                color = crystal.mutedText
                            )
                            Button(
                                onClick = {
                                    if (matchedAccount != null && parsed.amountBdt > 0) {
                                        onSaveParsedTransaction(
                                            parsed.title,
                                            parsed.amountBdt,
                                            parsed.type,
                                            parsed.category,
                                            matchedAccount.id
                                        )
                                        onDismiss()
                                    }
                                },
                                enabled = parsed.amountBdt > 0 && matchedAccount != null,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(strings.saveTransaction)
                            }
                        }
                    }
                }

                is VoiceParseUiState.Error -> {
                    Text(
                        text = voiceState.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = crystal.liabilityRed
                    )
                }

                else -> Unit
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalSearchSheet(
    accounts: List<AccountEntity>,
    transactions: List<TransactionEntity>,
    language: AppLanguage,
    strings: AppStrings,
    onOpenAccountList: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var query by remember { mutableStateOf("") }

    val matchingAccounts = remember(accounts, query) {
        if (query.isBlank()) accounts.take(3)
        else accounts.filter {
            it.name.contains(query, true) || it.bankOrProvider.contains(query, true)
        }
    }
    val matchingTx = remember(transactions, query) {
        if (query.isBlank()) transactions.take(5)
        else transactions.filter {
            it.title.contains(query, true) || it.note.contains(query, true) ||
                it.category.en.contains(query, true) || it.category.bn.contains(query, true)
        }
    }

    val crystal = LocalCrystalColors.current
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = strings.globalSearchTitle, style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text(strings.searchTransactionsHint) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            if (matchingAccounts.isNotEmpty()) {
                Text(text = strings.myWallets, style = MaterialTheme.typography.labelLarge)
                matchingAccounts.forEach { acc ->
                    Surface(
                        color = if (crystal.isDark) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onDismiss()
                                onOpenAccountList()
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(acc.name, style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = BdtFormatter.format(acc.balance, language),
                                style = MaterialTheme.typography.labelLarge.copy(fontFamily = JetBrainsMonoFamily)
                            )
                        }
                    }
                }
            }

            if (matchingTx.isNotEmpty()) {
                Text(text = strings.recentTransactions, style = MaterialTheme.typography.labelLarge)
                matchingTx.take(8).forEach { tx ->
                    TransactionRowItem(
                        tx = tx,
                        accountName = accounts.firstOrNull { it.id == tx.accountId }?.name.orEmpty(),
                        language = language
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsSheet(
    alerts: List<SmartAlert>,
    strings: AppStrings,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val crystal = LocalCrystalColors.current

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
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Filled.NotificationsActive, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(text = strings.notificationsTitle, style = MaterialTheme.typography.titleLarge)
            }

            alerts.forEach { alert ->
                Surface(
                    color = if (alert.isWarning) crystal.warningAmberBg else crystal.assetGreenBg,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = if (alert.isWarning) Icons.Filled.WarningAmber else Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = if (alert.isWarning) crystal.warningAmber else crystal.assetGreen
                        )
                        Column {
                            Text(
                                text = alert.title,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Text(
                                text = alert.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = crystal.mutedText
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }
    }
}

@Composable
fun EditProfileBudgetDialog(
    currentName: String,
    currentBudget: Double,
    strings: AppStrings,
    onDismiss: () -> Unit,
    onSave: (String, Double) -> Unit
) {
    val crystal = LocalCrystalColors.current
    var name by remember { mutableStateOf(currentName) }
    var budgetText by remember { mutableStateOf(currentBudget.toLong().toString()) }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = MaterialTheme.colorScheme.onSurface,
        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = if (crystal.isDark) Color.White.copy(alpha = 0.24f) else crystal.glassBorder,
        focusedLabelColor = MaterialTheme.colorScheme.primary,
        unfocusedLabelColor = crystal.mutedText,
        cursorColor = MaterialTheme.colorScheme.primary,
        focusedContainerColor = crystal.glassSurfaceElevated,
        unfocusedContainerColor = crystal.glassSurfaceElevated
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurface,
        title = {
            Text(
                text = strings.editProfileBudget,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(strings.yourNameLabel) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = fieldColors
                )
                OutlinedTextField(
                    value = budgetText,
                    onValueChange = { budgetText = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text(strings.monthlyBudgetInput) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = fieldColors
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val b = budgetText.toDoubleOrNull() ?: currentBudget
                    onSave(name, b)
                    onDismiss()
                }
            ) {
                Text(strings.saveChanges)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel)
            }
        }
    )
}
