package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CategoryEditorCatalog
import com.example.data.model.ManagedCategoryItem
import com.example.data.model.SubcategoryOption
import com.example.data.model.TransactionType
import com.example.localization.AppLanguage
import com.example.localization.AppStrings
import com.example.localization.BdtFormatter
import com.example.ui.theme.HindSiliguriFamily
import com.example.ui.theme.LocalCrystalColors

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryEditorScreen(
    initialItem: ManagedCategoryItem?,
    defaultType: TransactionType,
    language: AppLanguage,
    strings: AppStrings,
    onSave: (ManagedCategoryItem) -> Unit,
    onDelete: ((ManagedCategoryItem) -> Unit)? = null,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val crystal = LocalCrystalColors.current
    val copy = strings.categoriesManagement
    val isEditMode = initialItem != null

    var selectedType by remember(initialItem, defaultType) {
        mutableStateOf(initialItem?.type ?: defaultType)
    }
    var nameInput by remember(initialItem, language) {
        mutableStateOf(initialItem?.label(language) ?: "")
    }
    var nameError by remember { mutableStateOf(false) }
    var isActive by remember(initialItem) {
        mutableStateOf(initialItem?.isActive ?: true)
    }
    var selectedColor by remember(initialItem) {
        mutableStateOf(initialItem?.accentColor ?: CategoryEditorCatalog.colorSwatches.first())
    }
    var selectedIconKey by remember(initialItem) {
        mutableStateOf(initialItem?.iconKey ?: CategoryEditorCatalog.iconKeys.first())
    }
    val subcategories = remember(initialItem) {
        mutableStateListOf<SubcategoryOption>().apply {
            if (initialItem != null) addAll(initialItem.subcategories)
        }
    }
    var newSubcategoryInput by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf(false) }

    fun addSubcategory() {
        val trimmed = newSubcategoryInput.trim()
        if (trimmed.isEmpty()) return
        val alreadyExists = subcategories.any {
            it.en.equals(trimmed, ignoreCase = true) || it.bn.equals(trimmed, ignoreCase = true)
        }
        if (!alreadyExists) {
            val safeKey = "sub_${System.currentTimeMillis()}_${subcategories.size}"
            subcategories.add(SubcategoryOption(key = safeKey, en = trimmed, bn = trimmed))
        }
        newSubcategoryInput = ""
    }

    fun handleSave() {
        val trimmedName = nameInput.trim()
        if (trimmedName.isEmpty()) {
            nameError = true
            return
        }
        val resultItem = if (initialItem != null) {
            val updatedEn = if (language == AppLanguage.EN || !initialItem.isBuiltIn) trimmedName else initialItem.en
            val updatedBn = if (language == AppLanguage.BN || !initialItem.isBuiltIn) trimmedName else initialItem.bn
            initialItem.copy(
                type = selectedType,
                en = updatedEn,
                bn = updatedBn,
                accentColor = selectedColor,
                iconKey = selectedIconKey,
                isActive = isActive,
                subcategories = subcategories.toList()
            )
        } else {
            ManagedCategoryItem(
                id = "custom_${System.currentTimeMillis()}",
                type = selectedType,
                en = trimmedName,
                bn = trimmedName,
                accentColor = selectedColor,
                iconKey = selectedIconKey,
                subcategories = subcategories.toList(),
                isBuiltIn = false,
                isActive = isActive,
                legacyCategory = null
            )
        }
        onSave(resultItem)
    }

    val cardBg = if (crystal.isDark) Color(0xFF161C2B) else crystal.glassSurfaceElevated
    val cardBorder = if (crystal.isDark) Color.White.copy(alpha = 0.14f) else crystal.glassBorder

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("category_editor_screen"),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 620.dp)
                .fillMaxSize()
        ) {
            // Top Bar: Back + Title + Delete (Edit mode)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 8.dp)
                    .testTag("category_editor_top_bar"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("category_editor_back_button")
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

                Text(
                    text = if (isEditMode) copy.editCategoryTitle else copy.createCategoryTitle,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = HindSiliguriFamily
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (isEditMode && onDelete != null) {
                    IconButton(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("category_editor_delete_button")
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = crystal.liabilityRed.copy(alpha = if (crystal.isDark) 0.20f else 0.12f),
                            border = BorderStroke(
                                1.dp,
                                crystal.liabilityRed.copy(alpha = if (crystal.isDark) 0.42f else 0.28f)
                            ),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.DeleteOutline,
                                    contentDescription = copy.deleteCategoryDesc,
                                    tint = crystal.liabilityRed,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Scrollable Form
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("category_editor_form"),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Live Preview Header Card
                item {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = cardBg,
                        border = BorderStroke(1.dp, selectedColor.copy(alpha = if (crystal.isDark) 0.40f else 0.28f)),
                        shadowElevation = if (crystal.isDark) 6.dp else 4.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(15.dp))
                                    .background(selectedColor.copy(alpha = if (crystal.isDark) 0.24f else 0.14f))
                                    .border(
                                        1.dp,
                                        selectedColor.copy(alpha = if (crystal.isDark) 0.44f else 0.30f),
                                        RoundedCornerShape(15.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = resolveManagedCategoryIcon(selectedIconKey),
                                    contentDescription = null,
                                    tint = selectedColor,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    text = nameInput.trim().ifEmpty { copy.categoryNamePlaceholder },
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = HindSiliguriFamily
                                    ),
                                    color = if (nameInput.isBlank()) crystal.mutedText else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val typeBadgeText = when (selectedType) {
                                        TransactionType.INCOME -> "↓ ${copy.tabIncome}"
                                        TransactionType.EXPENSE -> "↑ ${copy.tabExpense}"
                                        TransactionType.TRANSFER -> "⇄ ${copy.tabTransfer}"
                                    }
                                    Surface(
                                        shape = CircleShape,
                                        color = selectedColor.copy(alpha = if (crystal.isDark) 0.22f else 0.12f)
                                    ) {
                                        Text(
                                            text = typeBadgeText,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                fontFamily = HindSiliguriFamily
                                            ),
                                            color = selectedColor,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                    if (!isActive) {
                                        Surface(
                                            shape = CircleShape,
                                            color = crystal.mutedText.copy(alpha = 0.16f)
                                        ) {
                                            Text(
                                                text = copy.inactiveTag,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontFamily = HindSiliguriFamily
                                                ),
                                                color = crystal.mutedText,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 1. Category Type Pills: ↓ আয়, ↑ খরচ, ⇄ ট্রান্সফার
                item {
                    EditorSectionCard(
                        title = copy.typeLabel,
                        cardBg = cardBg,
                        cardBorder = cardBorder
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CategoryTypeChoicePill(
                                label = "↓ ${copy.tabIncome}",
                                selected = selectedType == TransactionType.INCOME,
                                activeColor = crystal.assetGreen,
                                testTag = "editor_type_INCOME",
                                modifier = Modifier.weight(1f),
                                onClick = { selectedType = TransactionType.INCOME }
                            )
                            CategoryTypeChoicePill(
                                label = "↑ ${copy.tabExpense}",
                                selected = selectedType == TransactionType.EXPENSE,
                                activeColor = crystal.liabilityRed,
                                testTag = "editor_type_EXPENSE",
                                modifier = Modifier.weight(1f),
                                onClick = { selectedType = TransactionType.EXPENSE }
                            )
                            CategoryTypeChoicePill(
                                label = "⇄ ${copy.tabTransfer}",
                                selected = selectedType == TransactionType.TRANSFER,
                                activeColor = MaterialTheme.colorScheme.primary,
                                testTag = "editor_type_TRANSFER",
                                modifier = Modifier.weight(1f),
                                onClick = { selectedType = TransactionType.TRANSFER }
                            )
                        }
                    }
                }

                // 2. Category Name & Active Toggle
                item {
                    EditorSectionCard(
                        title = copy.categoryNameLabel,
                        cardBg = cardBg,
                        cardBorder = cardBorder
                    ) {
                        OutlinedTextField(
                            value = nameInput,
                            onValueChange = {
                                nameInput = it
                                if (nameError && it.isNotBlank()) nameError = false
                            },
                            placeholder = {
                                Text(
                                    text = copy.categoryNamePlaceholder,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = HindSiliguriFamily
                                    ),
                                    color = crystal.mutedText
                                )
                            },
                            singleLine = true,
                            isError = nameError,
                            supportingText = if (nameError) {
                                {
                                    Text(
                                        text = copy.categoryNameRequiredError,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = HindSiliguriFamily
                                        )
                                    )
                                }
                            } else null,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = selectedColor,
                                unfocusedBorderColor = cardBorder,
                                cursorColor = selectedColor
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("editor_name_input")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (crystal.isDark) Color.White.copy(alpha = 0.04f) else Color(0xFFF6F9FC),
                            border = BorderStroke(1.dp, cardBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isActive = !isActive }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = copy.activeCategoryTitle,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            fontFamily = HindSiliguriFamily
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = copy.activeCategorySubtitle,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontFamily = HindSiliguriFamily
                                        ),
                                        color = crystal.mutedText
                                    )
                                }
                                Switch(
                                    checked = isActive,
                                    onCheckedChange = { isActive = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = selectedColor
                                    ),
                                    modifier = Modifier.testTag("editor_active_switch")
                                )
                            }
                        }
                    }
                }

                // 3. Color Swatch Picker (রং নির্বাচন করুন)
                item {
                    EditorSectionCard(
                        title = copy.selectColorLabel,
                        cardBg = cardBg,
                        cardBorder = cardBorder
                    ) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            CategoryEditorCatalog.colorSwatches.forEachIndexed { index, swatchColor ->
                                val isSelected = swatchColor == selectedColor
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .clickable { selectedColor = swatchColor }
                                        .testTag("editor_color_$index"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(if (isSelected) 42.dp else 36.dp)
                                            .clip(CircleShape)
                                            .background(swatchColor)
                                            .border(
                                                width = if (isSelected) 2.5.dp else 1.dp,
                                                color = if (isSelected) {
                                                    MaterialTheme.colorScheme.onSurface
                                                } else {
                                                    Color.White.copy(alpha = 0.4f)
                                                },
                                                shape = CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Filled.Check,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 4. Icon Picker (আইকন নির্বাচন করুন)
                item {
                    EditorSectionCard(
                        title = copy.selectIconLabel,
                        cardBg = cardBg,
                        cardBorder = cardBorder
                    ) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            CategoryEditorCatalog.iconKeys.forEach { iconKey ->
                                val isSelected = iconKey == selectedIconKey
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(
                                            if (isSelected) {
                                                selectedColor.copy(alpha = if (crystal.isDark) 0.28f else 0.16f)
                                            } else if (crystal.isDark) {
                                                Color.White.copy(alpha = 0.05f)
                                            } else {
                                                Color(0xFFF3F6FA)
                                            }
                                        )
                                        .border(
                                            width = if (isSelected) 1.6.dp else 1.dp,
                                            color = if (isSelected) selectedColor else cardBorder,
                                            shape = RoundedCornerShape(14.dp)
                                        )
                                        .clickable { selectedIconKey = iconKey }
                                        .testTag("editor_icon_$iconKey"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = resolveManagedCategoryIcon(iconKey),
                                        contentDescription = iconKey,
                                        tint = if (isSelected) selectedColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // 5. Subcategories Manager (সাব-ক্যাটাগরি)
                item {
                    EditorSectionCard(
                        title = if (subcategories.isEmpty()) {
                            copy.subcategoriesLabel
                        } else {
                            "${copy.subcategoriesLabel} (${BdtFormatter.formatNumber(subcategories.size, language)})"
                        },
                        cardBg = cardBg,
                        cardBorder = cardBorder
                    ) {
                        if (subcategories.isNotEmpty()) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp)
                            ) {
                                subcategories.forEach { sub ->
                                    Surface(
                                        shape = CircleShape,
                                        color = selectedColor.copy(alpha = if (crystal.isDark) 0.20f else 0.12f),
                                        border = BorderStroke(
                                            1.dp,
                                            selectedColor.copy(alpha = if (crystal.isDark) 0.40f else 0.28f)
                                        ),
                                        modifier = Modifier.testTag("editor_subcategory_chip_${sub.key}")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = sub.label(language),
                                                style = MaterialTheme.typography.labelMedium.copy(
                                                    fontWeight = FontWeight.Medium,
                                                    fontFamily = HindSiliguriFamily
                                                ),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .clip(CircleShape)
                                                    .clickable { subcategories.remove(sub) }
                                                    .testTag("editor_remove_subcategory_${sub.key}"),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Close,
                                                    contentDescription = strings.close,
                                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                                                    modifier = Modifier.size(15.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = newSubcategoryInput,
                                onValueChange = { newSubcategoryInput = it },
                                placeholder = {
                                    Text(
                                        text = copy.subcategoryPlaceholder,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontFamily = HindSiliguriFamily
                                        ),
                                        color = crystal.mutedText,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = selectedColor,
                                    unfocusedBorderColor = cardBorder,
                                    cursorColor = selectedColor
                                ),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = { addSubcategory() }),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("editor_subcategory_input")
                            )

                            FilledTonalButton(
                                onClick = { addSubcategory() },
                                enabled = newSubcategoryInput.isNotBlank(),
                                shape = RoundedCornerShape(14.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                                modifier = Modifier
                                    .heightIn(min = 52.dp)
                                    .testTag("editor_add_subcategory_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = copy.addSubcategoryLabel,
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontFamily = HindSiliguriFamily
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Pinned Primary Save Button
            Surface(
                color = if (crystal.isDark) Color(0xFF101522).copy(alpha = 0.94f) else crystal.glassSurfaceElevated.copy(alpha = 0.96f),
                border = BorderStroke(1.dp, cardBorder),
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Button(
                        onClick = { handleSave() },
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .shadow(
                                elevation = 8.dp,
                                shape = RoundedCornerShape(18.dp),
                                ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.28f),
                                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.34f)
                            )
                            .testTag("editor_save_category_button")
                    ) {
                        Text(
                            text = copy.saveCategoryBtn,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = HindSiliguriFamily,
                                fontSize = 15.5.sp
                            )
                        )
                    }
                }
            }
        }
    }

    if (showDeleteDialog && initialItem != null && onDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = {
                Text(
                    text = copy.deleteCategoryConfirmTitle,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = HindSiliguriFamily
                    )
                )
            },
            text = {
                Text(
                    text = copy.deleteCategoryConfirmMessage,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = HindSiliguriFamily
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        onDelete(initialItem)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = crystal.liabilityRed,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.testTag("confirm_delete_category_button")
                ) {
                    Text(
                        text = copy.deleteActionBtn,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = HindSiliguriFamily
                        )
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false },
                    modifier = Modifier.testTag("cancel_delete_category_button")
                ) {
                    Text(
                        text = strings.cancel,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontFamily = HindSiliguriFamily
                        )
                    )
                }
            },
            modifier = Modifier.testTag("category_delete_confirm_dialog")
        )
    }
}

@Composable
private fun EditorSectionCard(
    title: String,
    cardBg: Color,
    cardBorder: Color,
    content: @Composable () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder),
        shadowElevation = 3.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = HindSiliguriFamily,
                    fontSize = 13.5.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            content()
        }
    }
}

@Composable
private fun CategoryTypeChoicePill(
    label: String,
    selected: Boolean,
    activeColor: Color,
    testTag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val crystal = LocalCrystalColors.current
    val bgColor by animateColorAsState(
        targetValue = if (selected) {
            activeColor
        } else if (crystal.isDark) {
            Color.White.copy(alpha = 0.06f)
        } else {
            Color(0xFFF3F6FA)
        },
        label = "editorTypeBg"
    )
    val textColor by animateColorAsState(
        targetValue = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
        label = "editorTypeText"
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) activeColor else crystal.glassBorder,
        label = "editorTypeBorder"
    )

    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = bgColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier
            .heightIn(min = 48.dp)
            .testTag(testTag)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                    fontFamily = HindSiliguriFamily,
                    fontSize = 13.sp
                ),
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
