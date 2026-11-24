package com.example.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
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
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
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
import coil.compose.AsyncImage
import com.example.core.DevicePermissionsHelper
import com.example.data.ai.LocalVoiceTransactionParser
import com.example.data.ai.MatchedAccountResult
import com.example.data.ai.ReceiptVoucherMatcher
import com.example.data.ai.TempReceiptImageCleaner
import com.example.data.ai.UnifiedCategoryDetector
import com.example.data.local.AccountEntity
import com.example.data.model.AccountNameResolver
import com.example.data.local.TransactionEntity
import com.example.data.model.AppCurrency
import com.example.data.model.BuiltInCategoriesCatalog
import com.example.data.model.ExpenseCategory
import com.example.data.model.ManagedCategoryItem
import com.example.data.model.SmartAlert
import com.example.data.model.TransactionType
import com.example.localization.AppLanguage
import com.example.localization.AppStrings
import com.example.localization.BdtFormatter
import com.example.ui.components.UnifiedCategoryPickerSection
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalCrystalColors
import com.example.ui.viewmodel.ReceiptScanUiState
import com.example.ui.viewmodel.TtsUiState
import com.example.ui.viewmodel.VoiceParseUiState
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptScanSheet(
    accounts: List<AccountEntity>,
    receiptState: ReceiptScanUiState,
    language: AppLanguage,
    strings: AppStrings,
    onPickImageUri: (Uri) -> Unit,
    onSaveExpense: (title: String, amount: Double, category: ExpenseCategory, accountId: Long, note: String) -> Unit,
    onDismiss: () -> Unit,
    defaultCurrencyCode: String = "BDT",
    onSaveExpenseWithDate: ((title: String, amount: Double, category: ExpenseCategory, accountId: Long, note: String, timestamp: Long) -> Unit)? = null,
    onAddAccount: (() -> Unit)? = null,
    onSaveVoucherTransaction: ((title: String, amount: Double, type: TransactionType, category: ExpenseCategory, accountId: Long, note: String, timestamp: Long) -> Unit)? = null,
    managedCategoryOverrides: List<ManagedCategoryItem> = emptyList(),
    deletedCategoryIds: Set<String> = emptySet(),
    onAddCustomCategory: ((ManagedCategoryItem) -> Unit)? = null,
    onSaveVoucherWithSubcategory: ((title: String, amount: Double, type: TransactionType, category: ExpenseCategory, subcategory: String, accountId: Long, note: String, timestamp: Long) -> Unit)? = null,
    onExecuteTransfer: ((fromAccountId: Long, toAccountId: Long, amount: Double, note: String, timestamp: Long) -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val crystal = LocalCrystalColors.current
    val context = LocalContext.current
    val isBn = language.isBangla
    var pickedUri by remember { mutableStateOf<Uri?>(null) }
    var pendingCameraUriString by rememberSaveable { mutableStateOf<String?>(null) }
    var activeTempCaptureFile by remember { mutableStateOf<File?>(null) }
    var cameraNotice by remember { mutableStateOf<String?>(null) }
    var isManualFallbackOpen by remember { mutableStateOf(false) }

    val mergedExpenseCategories = remember(managedCategoryOverrides, deletedCategoryIds) {
        BuiltInCategoriesCatalog.mergedFor(TransactionType.EXPENSE, managedCategoryOverrides, deletedCategoryIds)
    }
    val mergedIncomeCategories = remember(managedCategoryOverrides, deletedCategoryIds) {
        BuiltInCategoriesCatalog.mergedFor(TransactionType.INCOME, managedCategoryOverrides, deletedCategoryIds)
    }
    val mergedAllCategories = remember(managedCategoryOverrides, deletedCategoryIds) {
        BuiltInCategoriesCatalog.mergedAll(managedCategoryOverrides, deletedCategoryIds)
    }

    var editableMerchant by remember { mutableStateOf("") }
    var editableAmount by remember { mutableStateOf("") }
    var editableFee by remember { mutableStateOf("") }
    var includeFeeInTotal by remember { mutableStateOf(true) }
    var editableTransactionType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var editableCategory by remember { mutableStateOf(ExpenseCategory.OTHER) }
    var selectedManagedCategoryId by remember { mutableStateOf("exp_other") }
    var hasManualCategoryOverride by remember { mutableStateOf(false) }
    var isCategoryAutoDetected by remember { mutableStateOf(false) }
    var editableSummary by remember { mutableStateOf("") }
    var selectedAccountId by remember(accounts) { mutableStateOf(accounts.firstOrNull()?.id ?: 0L) }
    var selectedToAccountId by remember(accounts) {
        mutableStateOf(accounts.getOrNull(1)?.id ?: accounts.firstOrNull()?.id ?: 0L)
    }
    var matchedAccountResult by remember { mutableStateOf<MatchedAccountResult?>(null) }
    var detectedCurrencyCode by remember { mutableStateOf<String?>(null) }
    var detectedProviderHint by remember { mutableStateOf("") }
    var detectedDirectionKeyword by remember { mutableStateOf("") }
    var detectedReferenceId by remember { mutableStateOf("") }
    var selectedDateOption by remember { mutableStateOf(0) } // 0 = Today, 1 = Yesterday, 2 = Custom
    var selectedTimestamp by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }

    val activeManagedCategories = remember(editableTransactionType, mergedExpenseCategories, mergedIncomeCategories) {
        when (editableTransactionType) {
            TransactionType.INCOME -> mergedIncomeCategories
            TransactionType.EXPENSE -> mergedExpenseCategories
            TransactionType.TRANSFER -> BuiltInCategoriesCatalog.defaultsFor(TransactionType.TRANSFER)
        }
    }
    val selectedManagedCategory = remember(selectedManagedCategoryId, activeManagedCategories) {
        activeManagedCategories.firstOrNull { it.id == selectedManagedCategoryId }
            ?: activeManagedCategories.firstOrNull()
    }

    val selectedAccount = accounts.firstOrNull { it.id == selectedAccountId }
    val selectedToAccount = accounts.firstOrNull { it.id == selectedToAccountId }
    val activeCurrency = AppCurrency.fromCode(
        detectedCurrencyCode?.takeIf { it.isNotBlank() }
            ?: selectedAccount?.currencyCode
            ?: defaultCurrencyCode
    )
    val manualEntryButtonLabel = if (isBn) "ম্যানুয়ালি হিসাব যোগ করুন" else "Add Transaction Manually"
    val retryScanLabel = if (isBn) "আবার স্ক্যান করুন" else strings.retry
    val manualSectionTitle = if (isBn) "ম্যানুয়াল রসিদ / ভাউচার এন্ট্রি" else "Manual Receipt / Voucher Entry"
    val dateFormatter = remember(language) {
        SimpleDateFormat("dd MMM yyyy", if (isBn) Locale.forLanguageTag("bn-BD") else Locale.ENGLISH)
    }

    fun cleanupTempCaptures() {
        TempReceiptImageCleaner.deleteFileQuietly(activeTempCaptureFile)
        activeTempCaptureFile = null
        pendingCameraUriString?.let { uriStr ->
            runCatching { TempReceiptImageCleaner.deleteTempUriIfOwned(context, Uri.parse(uriStr)) }
        }
        pendingCameraUriString = null
        pickedUri?.let { uri ->
            TempReceiptImageCleaner.deleteTempUriIfOwned(context, uri)
        }
        pickedUri = null
        TempReceiptImageCleaner.cleanupAllTempReceipts(context)
    }

    fun handleDismiss() {
        cleanupTempCaptures()
        onDismiss()
    }

    // Ensure zero leftover temp camera files when sheet opens or disposes
    DisposableEffect(Unit) {
        TempReceiptImageCleaner.cleanupAllTempReceipts(context)
        onDispose {
            TempReceiptImageCleaner.deleteFileQuietly(activeTempCaptureFile)
            TempReceiptImageCleaner.cleanupAllTempReceipts(context)
        }
    }

    LaunchedEffect(receiptState, accounts, mergedAllCategories) {
        if (receiptState is ReceiptScanUiState.Success) {
            val res = receiptState.result
            isManualFallbackOpen = false
            hasManualCategoryOverride = false
            editableMerchant = res.merchantName
            editableAmount = formatEditableDecimal(res.amountBdt)
            editableFee = if (res.feeAmount > 0.0) formatEditableDecimal(res.feeAmount) else ""
            includeFeeInTotal = res.feeAmount > 0.0 && res.transactionType == TransactionType.EXPENSE
            editableTransactionType = res.transactionType
            val unifiedCat = UnifiedCategoryDetector.detectCategory(
                text = "${res.merchantName} ${res.itemsSummary} ${res.detectedDirectionKeyword}",
                type = res.transactionType,
                managedCategories = mergedAllCategories,
                language = language
            )
            val resolvedManaged = mergedAllCategories.firstOrNull {
                it.id == res.managedCategoryId && it.type == res.transactionType
            } ?: if (unifiedCat.isExactCustomMatch ||
                (unifiedCat.managedCategory.id != "exp_other" && unifiedCat.managedCategory.id != "inc_other")
            ) {
                unifiedCat.managedCategory
            } else {
                BuiltInCategoriesCatalog.resolveForLegacy(res.transactionType, res.category, mergedAllCategories)
                    ?: unifiedCat.managedCategory
            }
            selectedManagedCategoryId = resolvedManaged.id
            editableCategory = resolvedManaged.legacyCategory ?: res.category
            isCategoryAutoDetected = true
            detectedCurrencyCode = ReceiptVoucherMatcher.normalizeCurrencyCode(res.currencyCode, defaultCurrencyCode)
            detectedProviderHint = res.providerHint
            detectedDirectionKeyword = res.detectedDirectionKeyword
            detectedReferenceId = res.referenceId
            editableSummary = buildString {
                if (res.itemsSummary.isNotBlank()) append(res.itemsSummary)
                if (res.referenceId.isNotBlank() && !res.itemsSummary.contains(res.referenceId, ignoreCase = true)) {
                    if (isNotEmpty()) append(" • ")
                    append("TrxID: ${res.referenceId}")
                }
            }

            // Auto-match detected wallet/MFS/bank/card (bKash, Nagad, Rocket, Bank/Last-4/Currency)
            val match = ReceiptVoucherMatcher.matchAccount(
                accounts = accounts,
                providerHint = res.providerHint,
                accountLast4Hint = res.accountLast4Hint,
                currencyCode = detectedCurrencyCode ?: defaultCurrencyCode
            )
            matchedAccountResult = match
            match.account?.let { matchedAcc ->
                selectedAccountId = matchedAcc.id
            }

            // Apply detected receipt date if parsed
            val parsedMillis = res.timestampMillis
                ?: ReceiptVoucherMatcher.parseReceiptTimestamp(res.dateLabel)
            if (parsedMillis != null) {
                selectedTimestamp = parsedMillis
                val nowCal = Calendar.getInstance()
                val parsedCal = Calendar.getInstance().apply { timeInMillis = parsedMillis }
                val sameDay = nowCal.get(Calendar.YEAR) == parsedCal.get(Calendar.YEAR) &&
                    nowCal.get(Calendar.DAY_OF_YEAR) == parsedCal.get(Calendar.DAY_OF_YEAR)
                val yesterdayCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, -1) }
                val isYesterday = yesterdayCal.get(Calendar.YEAR) == parsedCal.get(Calendar.YEAR) &&
                    yesterdayCal.get(Calendar.DAY_OF_YEAR) == parsedCal.get(Calendar.DAY_OF_YEAR)
                selectedDateOption = when {
                    sameDay -> 0
                    isYesterday -> 1
                    else -> 2
                }
            }
        }
    }

    // Live unified category auto-detection when merchant/details are edited manually
    LaunchedEffect(editableMerchant, editableSummary, editableTransactionType, mergedAllCategories) {
        if (hasManualCategoryOverride || editableTransactionType == TransactionType.TRANSFER) return@LaunchedEffect
        val combined = "$editableMerchant $editableSummary".trim()
        if (combined.length < 2) return@LaunchedEffect
        val detected = UnifiedCategoryDetector.detectCategory(
            text = combined,
            type = editableTransactionType,
            managedCategories = mergedAllCategories,
            language = language
        )
        val isSpecific = detected.isExactCustomMatch ||
            (detected.managedCategory.id != "exp_other" && detected.managedCategory.id != "inc_other")
        if (isSpecific) {
            selectedManagedCategoryId = detected.managedCategory.id
            editableCategory = detected.legacyCategory
            isCategoryAutoDetected = true
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                // Delete any prior temporary camera capture when user switches to gallery pick
                TempReceiptImageCleaner.deleteFileQuietly(activeTempCaptureFile)
                activeTempCaptureFile = null
                TempReceiptImageCleaner.cleanupAllTempReceipts(context)
                cameraNotice = null
                isManualFallbackOpen = false
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
                isManualFallbackOpen = false
                pickedUri = captured
                onPickImageUri(captured)
            } else {
                // User cancelled camera — immediately remove empty temp capture file
                TempReceiptImageCleaner.deleteFileQuietly(activeTempCaptureFile)
                activeTempCaptureFile = null
                pendingCameraUriString = null
            }
        }
    )

    fun launchCameraCapture() {
        // Delete any previous temp camera capture before creating a new one
        TempReceiptImageCleaner.deleteFileQuietly(activeTempCaptureFile)
        activeTempCaptureFile = null
        val created = runCatching { TempReceiptImageCleaner.createTempCaptureFileAndUri(context) }.getOrNull()
        if (created == null) {
            cameraNotice = strings.cameraUnavailableNotice
        } else {
            val (tempFile, outputUri) = created
            activeTempCaptureFile = tempFile
            pendingCameraUriString = outputUri.toString()
            val launched = runCatching { cameraLauncher.launch(outputUri) }.isSuccess
            if (!launched) {
                TempReceiptImageCleaner.deleteFileQuietly(tempFile)
                activeTempCaptureFile = null
                pendingCameraUriString = null
                cameraNotice = strings.cameraUnavailableNotice
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (granted) {
                cameraNotice = null
                launchCameraCapture()
            } else {
                cameraNotice = strings.dateTimePermissions.cameraDeniedNotice
            }
        }
    )

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = selectedTimestamp)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            selectedTimestamp = millis
                            selectedDateOption = 2
                        }
                        showDatePicker = false
                    }
                ) {
                    Text(strings.saveChanges)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(strings.cancel)
                }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }

    ModalBottomSheet(
        onDismissRequest = { handleDismiss() },
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

            // Dual Scan Options: Live Camera Capture (with runtime CAMERA permission) + Zero-Permission Gallery Upload
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
                        if (DevicePermissionsHelper.isCameraGranted(context)) {
                            launchCameraCapture()
                        } else {
                            cameraNotice = strings.dateTimePermissions.cameraRationale
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
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
                is ReceiptScanUiState.Idle -> {
                    if (!isManualFallbackOpen) {
                        OutlinedButton(
                            onClick = { isManualFallbackOpen = true },
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.42f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("receipt_idle_manual_entry_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = manualEntryButtonLabel,
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
                    }
                }

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
                    val guidanceText = receiptState.guidance.ifBlank {
                        if (isBn) {
                            "রসিদের ছবিটি দেখে নিচের বাটন থেকে এখনই ম্যানুয়ালি হিসাব যোগ করুন অথবা পুনরায় চেষ্টা করুন।"
                        } else {
                            "Retry scanning or tap below to record this receipt manually without losing progress."
                        }
                    }
                    Surface(
                        color = if (crystal.isDark) {
                            crystal.warningAmberBg.copy(alpha = 0.85f)
                        } else {
                            crystal.liabilityRedBg
                        },
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(
                            1.dp,
                            crystal.liabilityRed.copy(alpha = if (crystal.isDark) 0.42f else 0.30f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("receipt_error_card")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.WarningAmber,
                                    contentDescription = null,
                                    tint = if (crystal.isDark) crystal.warningAmber else crystal.liabilityRed,
                                    modifier = Modifier
                                        .padding(top = 2.dp)
                                        .size(20.dp)
                                )
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = receiptState.message,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = guidanceText,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = crystal.mutedText
                                    )
                                }
                            }

                            if (!isManualFallbackOpen) {
                                Button(
                                    onClick = { isManualFallbackOpen = true },
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                        .testTag("receipt_manual_entry_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Edit,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = manualEntryButtonLabel,
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                            }

                            if (pickedUri != null) {
                                OutlinedButton(
                                    onClick = {
                                        isManualFallbackOpen = false
                                        onPickImageUri(pickedUri!!)
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                        .testTag("receipt_retry_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Refresh,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(retryScanLabel)
                                }
                            }
                        }
                    }
                }

                is ReceiptScanUiState.Success -> Unit
            }

            // Editable Voucher / Expense Confirmation Form (AI Vision success OR one-tap Manual Fallback)
            val showExpenseForm = receiptState is ReceiptScanUiState.Success || isManualFallbackOpen
            if (showExpenseForm) {
                val primaryAmountVal = editableAmount.toDoubleOrNull() ?: 0.0
                val feeVal = editableFee.toDoubleOrNull() ?: 0.0
                val effectiveTotalVal = when {
                    feeVal > 0.0 && includeFeeInTotal && editableTransactionType == TransactionType.EXPENSE ->
                        primaryAmountVal + feeVal
                    feeVal > 0.0 && includeFeeInTotal && editableTransactionType == TransactionType.INCOME ->
                        (primaryAmountVal - feeVal).coerceAtLeast(0.0)
                    else -> primaryAmountVal
                }

                if (isManualFallbackOpen && receiptState !is ReceiptScanUiState.Success) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = if (crystal.isDark) 0.18f else 0.10f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("receipt_manual_form_header")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = manualSectionTitle,
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                if (receiptState is ReceiptScanUiState.Success) {
                    val isLocalOcr = receiptState.result.isLocalFallback
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = if (crystal.isDark) 0.16f else 0.10f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("receipt_detection_source_badge")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = when {
                                        isLocalOcr && isBn -> "অন-ডিভাইস স্মার্ট স্ক্যান • মোট ও মুদ্রা শনাক্ত"
                                        isLocalOcr -> "On-Device Smart Scan • Total & Currency Detected"
                                        isBn -> "AI স্মার্ট স্ক্যান • মোট ও মুদ্রা শনাক্ত"
                                        else -> "AI Smart Scan • Total & Currency Detected"
                                    },
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = activeCurrency.cleanBadgeLabel(),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontFamily = JetBrainsMonoFamily,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // Smart Confirmation Preview Card: Money In (+), Money Out (-), or Transfer, Auto-Matched Card, Balance Impact
                ReceiptVoucherPreviewCard(
                    transactionType = editableTransactionType,
                    onSelectTransactionType = { newType ->
                        editableTransactionType = newType
                        hasManualCategoryOverride = false
                        if (newType == TransactionType.TRANSFER) {
                            editableCategory = ExpenseCategory.TRANSFER
                            selectedManagedCategoryId = "trf_wallet"
                            if (selectedToAccountId == selectedAccountId) {
                                accounts.firstOrNull { it.id != selectedAccountId }?.let {
                                    selectedToAccountId = it.id
                                }
                            }
                        } else {
                            val detected = UnifiedCategoryDetector.detectCategory(
                                text = "$editableMerchant $editableSummary",
                                type = newType,
                                managedCategories = mergedAllCategories,
                                language = language
                            )
                            selectedManagedCategoryId = detected.managedCategory.id
                            editableCategory = detected.legacyCategory
                        }
                    },
                    primaryAmount = primaryAmountVal,
                    feeAmount = feeVal,
                    includeFeeInTotal = includeFeeInTotal,
                    effectiveTotalAmount = effectiveTotalVal,
                    currency = activeCurrency,
                    selectedAccount = selectedAccount,
                    matchedAccountResult = matchedAccountResult,
                    detectedDirectionKeyword = detectedDirectionKeyword,
                    detectedProviderHint = detectedProviderHint,
                    referenceId = detectedReferenceId,
                    isAiDetected = receiptState is ReceiptScanUiState.Success,
                    language = language,
                    showTransferOption = onExecuteTransfer != null
                )

                // Wallet / Bank Card Selector
                Text(
                    text = "${strings.selectWalletLabel} (${activeCurrency.cleanBadgeLabel()})",
                    style = MaterialTheme.typography.labelLarge
                )
                if (accounts.isEmpty()) {
                    Surface(
                        color = crystal.warningAmberBg,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = if (isBn) {
                                    "হিসাব সংরক্ষণের আগে অন্তত একটি ওয়ালেট বা ব্যাংক কার্ড যোগ করুন।"
                                } else {
                                    "Add at least one wallet or bank card first to update its balance."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (onAddAccount != null) {
                                OutlinedButton(
                                    onClick = {
                                        cleanupTempCaptures()
                                        onAddAccount()
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.heightIn(min = 48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(strings.addAccount)
                                }
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        accounts.forEach { acc ->
                            val accCur = AppCurrency.fromCode(acc.currencyCode)
                            val isAutoMatch = matchedAccountResult?.isAutoMatched == true &&
                                matchedAccountResult?.account?.id == acc.id
                            FilterChip(
                                selected = selectedAccountId == acc.id,
                                onClick = { selectedAccountId = acc.id },
                                leadingIcon = if (isAutoMatch) {
                                    {
                                        Icon(
                                            imageVector = Icons.Filled.CheckCircle,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                } else null,
                                label = { Text("${acc.name} • ${accCur.cleanBadgeLabel()}") },
                                modifier = Modifier.testTag("receipt_wallet_chip_${acc.id}")
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = editableAmount,
                    onValueChange = { input -> editableAmount = sanitizeDecimalInput(input) },
                    label = { Text("${strings.detectedAmount} (${activeCurrency.cleanBadgeLabel()})") },
                    prefix = {
                        Text(
                            text = "${activeCurrency.symbol} ",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontFamily = JetBrainsMonoFamily,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("receipt_amount_input")
                )

                // Optional Fee / Charge field (bKash Cash Out, Send Money, Bank transfer charges)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = editableFee,
                        onValueChange = { input -> editableFee = sanitizeDecimalInput(input) },
                        label = {
                            Text(
                                if (isBn) "ফি / চার্জ (যদি থাকে)" else "Fee / Charge (optional)"
                            )
                        },
                        prefix = {
                            Text(
                                text = "${activeCurrency.symbol} ",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = JetBrainsMonoFamily,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = crystal.mutedText
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("receipt_fee_input")
                    )

                    if (feeVal > 0.0) {
                        FilterChip(
                            selected = includeFeeInTotal,
                            onClick = { includeFeeInTotal = !includeFeeInTotal },
                            label = {
                                Text(
                                    if (isBn) "ফি যোগ" else "Add Fee",
                                    maxLines = 1
                                )
                            },
                            modifier = Modifier.testTag("receipt_include_fee_toggle")
                        )
                    }
                }

                OutlinedTextField(
                    value = editableMerchant,
                    onValueChange = { editableMerchant = it },
                    label = {
                        Text(
                            if (isBn) {
                                if (editableTransactionType == TransactionType.INCOME) {
                                    "প্রেরক / উৎস (Sender / Source)"
                                } else {
                                    "মার্চেন্ট / প্রাপক (Merchant / Recipient)"
                                }
                            } else {
                                if (editableTransactionType == TransactionType.INCOME) {
                                    "Sender / Source"
                                } else {
                                    strings.detectedMerchant
                                }
                            }
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("receipt_merchant_input")
                )

                OutlinedTextField(
                    value = editableSummary,
                    onValueChange = { editableSummary = it },
                    label = {
                        Text(
                            if (receiptState is ReceiptScanUiState.Success) {
                                if (isBn) "বিবরণ / রেফারেন্স (TrxID)" else "Details / Reference (TrxID)"
                            } else {
                                strings.noteOptionalLabel
                            }
                        )
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("receipt_note_input")
                )

                // Date Picker Chips (Today / Yesterday / Pick Date)
                Text(text = strings.dateTimeLabel, style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .testTag("receipt_date_row"),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = selectedDateOption == 0,
                        onClick = {
                            selectedDateOption = 0
                            selectedTimestamp = System.currentTimeMillis()
                        },
                        label = { Text(strings.todayLabel) },
                        modifier = Modifier.testTag("receipt_date_today")
                    )
                    FilterChip(
                        selected = selectedDateOption == 1,
                        onClick = {
                            selectedDateOption = 1
                            selectedTimestamp = Calendar.getInstance().apply {
                                add(Calendar.DAY_OF_MONTH, -1)
                            }.timeInMillis
                        },
                        label = { Text(strings.yesterdayLabel) },
                        modifier = Modifier.testTag("receipt_date_yesterday")
                    )
                    FilterChip(
                        selected = selectedDateOption == 2,
                        onClick = { showDatePicker = true },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.CalendarMonth,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = {
                            Text(
                                if (selectedDateOption == 2) {
                                    dateFormatter.format(Date(selectedTimestamp))
                                } else {
                                    strings.pickDateLabel
                                }
                            )
                        },
                        modifier = Modifier.testTag("receipt_date_custom")
                    )
                }

                if (editableTransactionType == TransactionType.TRANSFER) {
                    Text(
                        text = if (isBn) "কোন অ্যাকাউন্টে (To)" else "To Account / Card",
                        style = MaterialTheme.typography.labelLarge
                    )
                    val destOptions = accounts.filter { it.id != selectedAccountId }
                    if (destOptions.isEmpty()) {
                        Text(
                            text = if (isBn) {
                                "ট্রান্সফার করতে অন্তত ২টি অ্যাকাউন্ট প্রয়োজন।"
                            } else {
                                "At least 2 accounts are needed for a transfer."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = crystal.warningAmber
                        )
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            destOptions.forEach { acc ->
                                val accCur = AppCurrency.fromCode(acc.currencyCode)
                                FilterChip(
                                    selected = selectedToAccountId == acc.id,
                                    onClick = { selectedToAccountId = acc.id },
                                    label = { Text("${acc.name} • ${accCur.cleanBadgeLabel()}") },
                                    modifier = Modifier.testTag("receipt_to_account_${acc.id}")
                                )
                            }
                        }
                    }
                } else {
                    UnifiedCategoryPickerSection(
                        transactionType = editableTransactionType,
                        categories = activeManagedCategories,
                        selectedCategoryId = selectedManagedCategory?.id.orEmpty(),
                        language = language,
                        label = strings.detectedCategory,
                        testTagPrefix = "receipt",
                        autoDetectedBadge = isCategoryAutoDetected,
                        onSelectCategory = { picked ->
                            hasManualCategoryOverride = true
                            isCategoryAutoDetected = false
                            selectedManagedCategoryId = picked.id
                            editableCategory = picked.legacyCategory ?: if (editableTransactionType == TransactionType.INCOME) {
                                ExpenseCategory.SALARY_INCOME
                            } else {
                                ExpenseCategory.OTHER
                            }
                        },
                        onAddCustomCategory = onAddCustomCategory
                    )
                }

                val isTransferReceipt = editableTransactionType == TransactionType.TRANSFER
                val canSaveReceiptTransfer = isTransferReceipt &&
                    effectiveTotalVal > 0.0 &&
                    selectedAccount != null &&
                    selectedToAccount != null &&
                    selectedAccount.id != selectedToAccount.id &&
                    onExecuteTransfer != null
                val canSaveReceiptStandard = !isTransferReceipt && effectiveTotalVal > 0.0 && selectedAccountId > 0L

                val saveButtonText = when (editableTransactionType) {
                    TransactionType.INCOME -> if (isBn) "নিশ্চিত করুন ও কার্ডে যোগ করুন (+)" else "Confirm & Add to Card (+)"
                    TransactionType.TRANSFER -> if (isBn) "ট্রান্সফার নিশ্চিত করুন" else "Confirm Transfer"
                    TransactionType.EXPENSE -> if (isBn) "নিশ্চিত করুন ও ব্যালেন্স আপডেট করুন" else strings.saveScannedExpense
                }

                Button(
                    onClick = {
                        val managedLabel = selectedManagedCategory?.label(language).orEmpty()
                        val finalTitle = editableMerchant.ifBlank {
                            managedLabel.ifBlank { editableCategory.label(language) }
                        }
                        val finalNote = buildString {
                            append(editableSummary.trim())
                            if (feeVal > 0.0) {
                                val feeTag = if (isBn) {
                                    "ফি: ${formatVoucherAmount(feeVal, activeCurrency.code, language)}"
                                } else {
                                    "Fee: ${formatVoucherAmount(feeVal, activeCurrency.code, language)}"
                                }
                                if (!contains(feeTag, ignoreCase = true)) {
                                    if (isNotEmpty()) append(" • ")
                                    append(feeTag)
                                }
                            }
                        }
                        when {
                            canSaveReceiptTransfer && selectedAccount != null && selectedToAccount != null -> {
                                onExecuteTransfer?.invoke(
                                    selectedAccount.id,
                                    selectedToAccount.id,
                                    effectiveTotalVal,
                                    finalNote.ifBlank { finalTitle },
                                    selectedTimestamp
                                )
                            }
                            onSaveVoucherWithSubcategory != null -> {
                                onSaveVoucherWithSubcategory(
                                    finalTitle,
                                    effectiveTotalVal,
                                    editableTransactionType,
                                    editableCategory,
                                    managedLabel,
                                    selectedAccountId,
                                    finalNote,
                                    selectedTimestamp
                                )
                            }
                            onSaveVoucherTransaction != null -> {
                                onSaveVoucherTransaction(
                                    finalTitle,
                                    effectiveTotalVal,
                                    editableTransactionType,
                                    editableCategory,
                                    selectedAccountId,
                                    finalNote,
                                    selectedTimestamp
                                )
                            }
                            onSaveExpenseWithDate != null -> {
                                onSaveExpenseWithDate(
                                    finalTitle,
                                    effectiveTotalVal,
                                    editableCategory,
                                    selectedAccountId,
                                    finalNote,
                                    selectedTimestamp
                                )
                            }
                            else -> {
                                onSaveExpense(
                                    finalTitle,
                                    effectiveTotalVal,
                                    editableCategory,
                                    selectedAccountId,
                                    finalNote
                                )
                            }
                        }
                        handleDismiss()
                    },
                    enabled = canSaveReceiptTransfer || canSaveReceiptStandard,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("receipt_save_expense_button")
                ) {
                    Text(saveButtonText)
                }
            }

            // Always visible storage privacy note: temporary scan photos are never saved permanently
            ReceiptTempStorageNoticeBanner(language = language)
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

private fun formatEditableDecimal(amount: Double): String = when {
    amount <= 0.0 -> ""
    abs(amount - amount.roundToLong()) < 0.005 -> amount.roundToLong().toString()
    abs(amount * 100.0 - (amount * 100.0).roundToLong()) >= 0.05 -> String.format(Locale.US, "%.3f", amount)
    else -> String.format(Locale.US, "%.2f", amount)
}

private fun sanitizeDecimalInput(input: String): String {
    val ascii = ReceiptVoucherMatcher.toAsciiDigits(input)
    var dotSeen = false
    return buildString {
        for (c in ascii) {
            if (c.isDigit()) {
                append(c)
            } else if (c == '.' && !dotSeen) {
                dotSeen = true
                append(c)
            }
        }
    }
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
    onDismiss: () -> Unit,
    defaultCurrencyCode: String = "BDT",
    onExecuteTransfer: ((fromAccountId: Long, toAccountId: Long, amount: Double, note: String) -> Unit)? = null,
    managedCategoryOverrides: List<ManagedCategoryItem> = emptyList(),
    deletedCategoryIds: Set<String> = emptySet(),
    onAddCustomCategory: ((ManagedCategoryItem) -> Unit)? = null,
    onSaveParsedTransactionWithSubcategory: ((title: String, amount: Double, type: TransactionType, category: ExpenseCategory, subcategory: String, accountId: Long, note: String) -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val crystal = LocalCrystalColors.current
    val context = LocalContext.current
    val isBn = language.isBangla
    var utterance by remember { mutableStateOf("") }
    var micNotice by remember { mutableStateOf<String?>(null) }
    var isManualEntryOpen by remember { mutableStateOf(false) }

    val mergedExpenseCategories = remember(managedCategoryOverrides, deletedCategoryIds) {
        BuiltInCategoriesCatalog.mergedFor(TransactionType.EXPENSE, managedCategoryOverrides, deletedCategoryIds)
    }
    val mergedIncomeCategories = remember(managedCategoryOverrides, deletedCategoryIds) {
        BuiltInCategoriesCatalog.mergedFor(TransactionType.INCOME, managedCategoryOverrides, deletedCategoryIds)
    }
    val mergedAllCategories = remember(managedCategoryOverrides, deletedCategoryIds) {
        BuiltInCategoriesCatalog.mergedAll(managedCategoryOverrides, deletedCategoryIds)
    }

    // Editable parsed transaction state (supports Expense, Income & Account-to-Account Transfer)
    var editableTitle by remember { mutableStateOf("") }
    var editableAmount by remember { mutableStateOf("") }
    var editableType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var editableCategory by remember { mutableStateOf(ExpenseCategory.OTHER) }
    var selectedManagedCategoryId by remember { mutableStateOf("exp_food_dining") }
    var hasManualCategoryOverride by remember { mutableStateOf(false) }
    var isCategoryAutoDetected by remember { mutableStateOf(false) }
    var editableCurrencyCode by remember { mutableStateOf(defaultCurrencyCode) }
    var editableNote by remember { mutableStateOf("") }
    var selectedFromAccountId by remember(accounts) { mutableStateOf(accounts.firstOrNull()?.id ?: 0L) }
    var selectedToAccountId by remember(accounts) {
        mutableStateOf(accounts.getOrNull(1)?.id ?: accounts.firstOrNull()?.id ?: 0L)
    }

    val activeManagedCategories = remember(editableType, mergedExpenseCategories, mergedIncomeCategories) {
        when (editableType) {
            TransactionType.INCOME -> mergedIncomeCategories
            TransactionType.EXPENSE -> mergedExpenseCategories
            TransactionType.TRANSFER -> BuiltInCategoriesCatalog.defaultsFor(TransactionType.TRANSFER)
        }
    }
    val selectedManagedCategory = remember(selectedManagedCategoryId, activeManagedCategories) {
        activeManagedCategories.firstOrNull { it.id == selectedManagedCategoryId }
            ?: activeManagedCategories.firstOrNull()
    }

    LaunchedEffect(voiceState, accounts, mergedAllCategories) {
        if (voiceState is VoiceParseUiState.Success) {
            val parsed = voiceState.result
            isManualEntryOpen = false
            hasManualCategoryOverride = false
            editableTitle = parsed.title
            editableAmount = formatEditableDecimal(parsed.amountBdt)
            editableType = parsed.type
            val unifiedCat = UnifiedCategoryDetector.detectCategory(
                text = "$utterance ${parsed.title} ${parsed.note}",
                type = parsed.type,
                managedCategories = mergedAllCategories,
                language = language
            )
            val resolvedManaged = mergedAllCategories.firstOrNull {
                it.id == parsed.managedCategoryId && it.type == parsed.type
            } ?: if (unifiedCat.isExactCustomMatch ||
                (unifiedCat.managedCategory.id != "exp_other" && unifiedCat.managedCategory.id != "inc_other")
            ) {
                unifiedCat.managedCategory
            } else {
                BuiltInCategoriesCatalog.resolveForLegacy(parsed.type, parsed.category, mergedAllCategories)
                    ?: unifiedCat.managedCategory
            }
            selectedManagedCategoryId = resolvedManaged.id
            editableCategory = resolvedManaged.legacyCategory ?: parsed.category
            isCategoryAutoDetected = true
            editableCurrencyCode = parsed.currencyCode.ifBlank { defaultCurrencyCode }
            editableNote = parsed.note

            if (parsed.type == TransactionType.TRANSFER) {
                val initialTo = LocalVoiceTransactionParser.matchAccountByHint(
                    accounts = accounts,
                    hint = parsed.toAccountHint,
                    preferredCurrency = null
                )
                val matchedFrom = LocalVoiceTransactionParser.matchAccountByHint(
                    accounts = accounts,
                    hint = parsed.accountHint,
                    preferredCurrency = editableCurrencyCode,
                    excludeAccountId = initialTo?.id
                ) ?: accounts.firstOrNull {
                    it.id != initialTo?.id && it.currencyCode.equals(editableCurrencyCode, ignoreCase = true)
                } ?: accounts.firstOrNull { it.id != initialTo?.id }
                    ?: accounts.firstOrNull()

                val matchedTo = initialTo?.takeIf { it.id != matchedFrom?.id }
                    ?: LocalVoiceTransactionParser.matchAccountByHint(
                        accounts = accounts,
                        hint = parsed.toAccountHint,
                        preferredCurrency = null,
                        excludeAccountId = matchedFrom?.id
                    ) ?: accounts.firstOrNull { it.id != matchedFrom?.id }

                if (matchedFrom != null) {
                    selectedFromAccountId = matchedFrom.id
                }
                if (matchedTo != null) {
                    selectedToAccountId = matchedTo.id
                }
            } else {
                val matchedFrom = LocalVoiceTransactionParser.matchAccountByHint(
                    accounts = accounts,
                    hint = parsed.accountHint,
                    preferredCurrency = editableCurrencyCode
                ) ?: accounts.firstOrNull {
                    it.currencyCode.equals(editableCurrencyCode, ignoreCase = true)
                } ?: accounts.firstOrNull()

                if (matchedFrom != null) {
                    selectedFromAccountId = matchedFrom.id
                }
            }
        }
    }

    // Live unified category detection when title is edited manually inside Voice Entry
    LaunchedEffect(editableTitle, editableType, mergedAllCategories, isManualEntryOpen) {
        if (!isManualEntryOpen || hasManualCategoryOverride || editableType == TransactionType.TRANSFER) return@LaunchedEffect
        val text = editableTitle.trim()
        if (text.length < 2) return@LaunchedEffect
        val detected = UnifiedCategoryDetector.detectCategory(
            text = text,
            type = editableType,
            managedCategories = mergedAllCategories,
            language = language
        )
        val isSpecific = detected.isExactCustomMatch ||
            (detected.managedCategory.id != "exp_other" && detected.managedCategory.id != "inc_other")
        if (isSpecific) {
            selectedManagedCategoryId = detected.managedCategory.id
            editableCategory = detected.legacyCategory
            isCategoryAutoDetected = true
        }
    }

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

    fun launchSpeechRecognizer() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE,
                language.localeTag
            )
        }
        try {
            speechLauncher.launch(intent)
        } catch (_: Exception) {
            micNotice = if (isBn) {
                "ভয়েস ইনপুট পাওয়া যায়নি—নিচে টাইপ করে AI স্মার্ট বিশ্লেষণ ব্যবহার করুন।"
            } else {
                "Device speech recognizer unavailable — type naturally below for AI parsing."
            }
        }
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (granted) {
                micNotice = null
                launchSpeechRecognizer()
            } else {
                micNotice = strings.dateTimePermissions.microphoneDeniedNotice
            }
        }
    )

    val fromAccount = accounts.firstOrNull { it.id == selectedFromAccountId }
    val toAccount = accounts.firstOrNull { it.id == selectedToAccountId }
    val activeCurrency = AppCurrency.fromCode(
        editableCurrencyCode.ifBlank { fromAccount?.currencyCode ?: defaultCurrencyCode }
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
                    imageVector = Icons.Filled.Mic,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(26.dp)
                )
                Column {
                    Text(text = strings.voiceEntryTitle, style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = strings.voiceEntrySubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = crystal.mutedText
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        if (DevicePermissionsHelper.isMicrophoneGranted(context)) {
                            launchSpeechRecognizer()
                        } else {
                            micNotice = strings.dateTimePermissions.microphoneRationale
                            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("voice_entry_mic_button")
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
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
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
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("voice_entry_input")
            )

            // Quick natural-language hint examples (expense & bank-to-bank transfer)
            val quickExamples = remember(isBn) {
                if (isBn) {
                    listOf(
                        "বিকাশ থেকে বাজার খরচ ১৫০০ টাকা",
                        "Trust থেকে Dutch-Bangla তে ৫০০০ টাকা পাঠিয়েছি"
                    )
                } else {
                    listOf(
                        "bKash groceries 1500 BDT",
                        "Transfer 5000 BDT from Trust to Dutch-Bangla"
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                quickExamples.forEach { sample ->
                    FilterChip(
                        selected = false,
                        onClick = {
                            utterance = sample
                            onParseUtterance(sample)
                        },
                        label = {
                            Text(
                                text = sample,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1
                            )
                        }
                    )
                }
            }

            Button(
                onClick = { onParseUtterance(utterance) },
                enabled = utterance.isNotBlank() && voiceState !is VoiceParseUiState.Loading,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("voice_parse_button")
            ) {
                if (voiceState is VoiceParseUiState.Loading) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.parseEntryBtn)
                }
            }

            if (voiceState !is VoiceParseUiState.Success && !isManualEntryOpen) {
                OutlinedButton(
                    onClick = { isManualEntryOpen = true },
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.40f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("voice_manual_entry_button")
                ) {
                    Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isBn) "ম্যানুয়ালি হিসাব যোগ করুন" else "Add Entry Manually",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }

            if (voiceState is VoiceParseUiState.Error) {
                Surface(
                    color = crystal.warningAmberBg,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("voice_error_card")
                ) {
                    Text(
                        text = voiceState.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            if (voiceState is VoiceParseUiState.Success || isManualEntryOpen) {
                val parsed = (voiceState as? VoiceParseUiState.Success)?.result
                val parsedAmount = editableAmount.toDoubleOrNull() ?: 0.0
                val isTransfer = editableType == TransactionType.TRANSFER
                val canSaveTransfer = isTransfer &&
                    parsedAmount > 0.0 &&
                    fromAccount != null &&
                    toAccount != null &&
                    fromAccount.id != toAccount.id &&
                    onExecuteTransfer != null
                val canSaveStandard = !isTransfer && parsedAmount > 0.0 && fromAccount != null

                Surface(
                    color = crystal.glassSurfaceElevated,
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.30f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("voice_parsed_card")
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // AI vs On-Device Parser vs Manual Entry Badge + Currency Pill
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = if (parsed != null) Icons.Filled.AutoAwesome else Icons.Filled.Edit,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = when {
                                        parsed == null && isBn -> "ম্যানুয়াল ভয়েস / দ্রুত এন্ট্রি"
                                        parsed == null -> "Manual Quick Entry"
                                        parsed.isLocalFallback && isBn -> "স্মার্ট লোকাল বিশ্লেষণ"
                                        parsed.isLocalFallback -> "Smart Local Parse"
                                        isBn -> "AI স্মার্ট বিশ্লেষণ"
                                        else -> "AI Smart Parse"
                                    },
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Surface(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = activeCurrency.cleanBadgeLabel(),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = JetBrainsMonoFamily,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        // Transaction Type Switcher (Expense / Income / Transfer)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                TransactionType.EXPENSE to (if (isBn) "খরচ" else "Expense"),
                                TransactionType.INCOME to (if (isBn) "আয়" else "Income"),
                                TransactionType.TRANSFER to (if (isBn) "ট্রান্সফার" else "Transfer")
                            ).forEach { (txType, label) ->
                                FilterChip(
                                    selected = editableType == txType,
                                    onClick = {
                                        editableType = txType
                                        hasManualCategoryOverride = false
                                        if (txType == TransactionType.TRANSFER) {
                                            editableCategory = ExpenseCategory.TRANSFER
                                            selectedManagedCategoryId = "trf_wallet"
                                            if (selectedToAccountId == selectedFromAccountId) {
                                                accounts.firstOrNull { it.id != selectedFromAccountId }?.let {
                                                    selectedToAccountId = it.id
                                                }
                                            }
                                        } else {
                                            val detected = UnifiedCategoryDetector.detectCategory(
                                                text = "$utterance $editableTitle",
                                                type = txType,
                                                managedCategories = mergedAllCategories,
                                                language = language
                                            )
                                            selectedManagedCategoryId = detected.managedCategory.id
                                            editableCategory = detected.legacyCategory
                                        }
                                    },
                                    label = { Text(label) },
                                    modifier = Modifier.testTag("voice_type_${txType.name}")
                                )
                            }
                        }

                            // Editable Amount & Title
                            OutlinedTextField(
                                value = editableAmount,
                                onValueChange = { editableAmount = sanitizeDecimalInput(it) },
                                label = {
                                    Text(
                                        if (isBn) {
                                            "টাকার পরিমাণ (${activeCurrency.cleanBadgeLabel()})"
                                        } else {
                                            "Amount (${activeCurrency.cleanBadgeLabel()})"
                                        }
                                    )
                                },
                                prefix = {
                                    Text(
                                        text = "${activeCurrency.symbol} ",
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontFamily = JetBrainsMonoFamily,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("voice_amount_input")
                            )

                            OutlinedTextField(
                                value = editableTitle,
                                onValueChange = { editableTitle = it },
                                label = { Text(if (isBn) "বিবরণ / শিরোনাম" else "Title / Description") },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("voice_title_input")
                            )

                            // Source Account / Card selector
                            Text(
                                text = if (isTransfer) {
                                    if (isBn) "কোন অ্যাকাউন্ট থেকে (From)" else "From Account / Card"
                                } else {
                                    strings.selectWalletLabel
                                },
                                style = MaterialTheme.typography.labelLarge
                            )
                            if (accounts.isEmpty()) {
                                Text(
                                    text = if (isBn) {
                                        "প্রথমে অন্তত একটি অ্যাকাউন্ট বা ওয়ালেট যোগ করুন।"
                                    } else {
                                        "Add at least one account or wallet first."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = crystal.warningAmber
                                )
                            } else {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    accounts.forEach { acc ->
                                        val accCur = AppCurrency.fromCode(acc.currencyCode)
                                        FilterChip(
                                            selected = selectedFromAccountId == acc.id,
                                            onClick = {
                                                selectedFromAccountId = acc.id
                                                editableCurrencyCode = acc.currencyCode
                                                if (isTransfer && selectedToAccountId == acc.id) {
                                                    accounts.firstOrNull { it.id != acc.id }?.let {
                                                        selectedToAccountId = it.id
                                                    }
                                                }
                                            },
                                            label = {
                                                Text(
                                                    "${AccountNameResolver.resolveAccountName(acc, language)} • ${accCur.cleanBadgeLabel()}"
                                                )
                                            },
                                            modifier = Modifier.testTag("voice_from_account_${acc.id}")
                                        )
                                    }
                                }
                            }

                            // Destination Account selector when TRANSFER
                            if (isTransfer) {
                                Text(
                                    text = if (isBn) "কোন অ্যাকাউন্টে (To)" else "To Account / Card",
                                    style = MaterialTheme.typography.labelLarge
                                )
                                val destinationOptions = accounts.filter { it.id != selectedFromAccountId }
                                if (destinationOptions.isEmpty()) {
                                    Text(
                                        text = if (isBn) {
                                            "ট্রান্সফার করতে অন্তত ২টি অ্যাকাউন্ট প্রয়োজন।"
                                        } else {
                                            "At least 2 accounts are needed for a transfer."
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = crystal.warningAmber
                                    )
                                } else {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        destinationOptions.forEach { acc ->
                                            val accCur = AppCurrency.fromCode(acc.currencyCode)
                                            FilterChip(
                                                selected = selectedToAccountId == acc.id,
                                                onClick = { selectedToAccountId = acc.id },
                                                label = {
                                                    Text(
                                                        "${AccountNameResolver.resolveAccountName(acc, language)} • ${accCur.cleanBadgeLabel()}"
                                                    )
                                                },
                                                modifier = Modifier.testTag("voice_to_account_${acc.id}")
                                            )
                                        }
                                    }

                                    if (fromAccount != null && toAccount != null && fromAccount.id != toAccount.id) {
                                        Surface(
                                            color = crystal.assetGreenBg,
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.SwapHoriz,
                                                    contentDescription = null,
                                                    tint = crystal.assetGreen,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Text(
                                                    text = "${AccountNameResolver.resolveAccountName(fromAccount, language)} → ${AccountNameResolver.resolveAccountName(toAccount, language)}",
                                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            } else {
                                // Unified synchronized Category Picker (12 Expense / 5 Income + Custom Add)
                                UnifiedCategoryPickerSection(
                                    transactionType = editableType,
                                    categories = activeManagedCategories,
                                    selectedCategoryId = selectedManagedCategory?.id.orEmpty(),
                                    language = language,
                                    label = strings.detectedCategory,
                                    testTagPrefix = "voice",
                                    autoDetectedBadge = isCategoryAutoDetected,
                                    onSelectCategory = { picked ->
                                        hasManualCategoryOverride = true
                                        isCategoryAutoDetected = false
                                        selectedManagedCategoryId = picked.id
                                        editableCategory = picked.legacyCategory ?: if (editableType == TransactionType.INCOME) {
                                            ExpenseCategory.SALARY_INCOME
                                        } else {
                                            ExpenseCategory.OTHER
                                        }
                                    },
                                    onAddCustomCategory = onAddCustomCategory
                                )
                            }

                            Button(
                                onClick = {
                                    if (canSaveTransfer && fromAccount != null && toAccount != null) {
                                        val finalNote = editableNote.ifBlank { editableTitle.ifBlank { utterance.trim() } }
                                        onExecuteTransfer?.invoke(
                                            fromAccount.id,
                                            toAccount.id,
                                            parsedAmount,
                                            finalNote
                                        )
                                        onDismiss()
                                    } else if (canSaveStandard && fromAccount != null) {
                                        val managedLabel = selectedManagedCategory?.label(language).orEmpty()
                                        val finalTitle = editableTitle.ifBlank {
                                            managedLabel.ifBlank { editableCategory.label(language) }
                                        }
                                        if (onSaveParsedTransactionWithSubcategory != null) {
                                            onSaveParsedTransactionWithSubcategory(
                                                finalTitle,
                                                parsedAmount,
                                                editableType,
                                                editableCategory,
                                                managedLabel,
                                                fromAccount.id,
                                                editableNote
                                            )
                                        } else {
                                            onSaveParsedTransaction(
                                                finalTitle,
                                                parsedAmount,
                                                editableType,
                                                editableCategory,
                                                fromAccount.id
                                            )
                                        }
                                        onDismiss()
                                    }
                                },
                                enabled = canSaveTransfer || canSaveStandard,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 50.dp)
                                    .testTag("voice_save_button")
                            ) {
                                Text(
                                    text = if (isTransfer) {
                                        if (isBn) "ট্রান্সফার নিশ্চিত করুন" else "Confirm Transfer"
                                    } else {
                                        strings.saveTransaction
                                    },
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }
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
                            Text(
                                text = AccountNameResolver.resolveAccountName(acc, language),
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = BdtFormatter.formatCurrency(acc.balance, acc.currencyCode, language),
                                style = MaterialTheme.typography.labelLarge.copy(fontFamily = JetBrainsMonoFamily)
                            )
                        }
                    }
                }
            }

            if (matchingTx.isNotEmpty()) {
                val accountsById = remember(accounts) { accounts.associateBy { it.id } }
                Text(text = strings.recentTransactions, style = MaterialTheme.typography.labelLarge)
                matchingTx.take(8).forEach { tx ->
                    TransactionRowItem(
                        tx = tx,
                        accountName = accountsById[tx.accountId]?.let {
                            AccountNameResolver.resolveAccountName(it, language)
                        }.orEmpty(),
                        language = language,
                        accountsById = accountsById
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
    onDismiss: () -> Unit,
    language: AppLanguage = AppLanguage.EN,
    onSelectActionRoute: ((com.example.notifications.NotificationActionRoute) -> Unit)? = null,
    onClearHistory: (() -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val crystal = LocalCrystalColors.current
    val isBn = language == AppLanguage.BN
    val timeFmt = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }

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
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.NotificationsActive,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = strings.notificationsTitle,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
                if (alerts.isNotEmpty() && onClearHistory != null) {
                    TextButton(
                        onClick = onClearHistory,
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("notifications_clear_all")
                    ) {
                        Text(if (isBn) "সব মুছুন" else "Clear All")
                    }
                }
            }

            if (alerts.isEmpty()) {
                Surface(
                    color = crystal.glassSurfaceElevated,
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, crystal.glassBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("notifications_empty_state")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.NotificationsNone,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = if (isBn) "কোনো নতুন নোটিফিকেশন নেই" else "You're All Caught Up",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isBn) {
                                "আপনার স্থানীয় সময় অনুযায়ী দৈনিক রিমাইন্ডার, দেনা-পাওনা ও বাজেট সতর্কতা এখানে দেখা যাবে।"
                            } else {
                                "Scheduled local-time reminders, dues alerts, and real spending warnings will appear here."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = crystal.mutedText,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                alerts.forEach { alert ->
                    val containerColor = if (alert.isWarning) crystal.warningAmberBg else crystal.glassSurfaceElevated
                    val accentColor = if (alert.isWarning) crystal.warningAmber else MaterialTheme.colorScheme.primary
                    val iconVector = when (alert.category) {
                        com.example.notifications.NotificationCategory.DAILY_REMINDER -> Icons.Filled.Alarm
                        com.example.notifications.NotificationCategory.DUE_LOAN -> Icons.Filled.EventNote
                        com.example.notifications.NotificationCategory.BUDGET_THRESHOLD -> Icons.Filled.WarningAmber
                        com.example.notifications.NotificationCategory.HIGH_SPEND_CARD -> Icons.Filled.CreditCard
                    }
                    Surface(
                        color = containerColor,
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(
                            1.dp,
                            if (alert.isWarning) crystal.warningAmber.copy(alpha = 0.35f) else crystal.glassBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .clickable {
                                onDismiss()
                                onSelectActionRoute?.invoke(alert.actionRoute)
                            }
                            .testTag("notification_item_${alert.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                color = accentColor.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = iconVector,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.padding(9.dp)
                                )
                            }
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    text = alert.title,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = alert.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = crystal.mutedText
                                )
                                if (alert.timestampMillis != null) {
                                    Text(
                                        text = timeFmt.format(Date(alert.timestampMillis)),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = crystal.mutedText.copy(alpha = 0.85f)
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = if (isBn) "খুলুন" else "Open",
                                tint = crystal.mutedText,
                                modifier = Modifier.size(20.dp)
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
