package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.BuiltInCategoriesCatalog
import com.example.data.model.CategoryEditorCatalog
import com.example.data.model.ManagedCategoryItem
import com.example.data.model.TransactionType
import com.example.localization.AppLanguage
import com.example.ui.screens.resolveManagedCategoryIcon
import com.example.ui.theme.LocalCrystalColors

/**
 * Unified, synchronized category selector with inline manual custom category creation.
 * Shared across Plus (+) Quick Entry, Voice Entry, and Receipt / Voucher Scan so every
 * expanded Income (5) and Expense (12) category plus any custom category created in one
 * flow is immediately persisted and available everywhere.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun UnifiedCategoryPickerSection(
    transactionType: TransactionType,
    categories: List<ManagedCategoryItem>,
    selectedCategoryId: String,
    language: AppLanguage,
    label: String,
    testTagPrefix: String,
    onSelectCategory: (ManagedCategoryItem) -> Unit,
    onAddCustomCategory: ((ManagedCategoryItem) -> Unit)? = null,
    modifier: Modifier = Modifier,
    autoDetectedBadge: Boolean = false
) {
    val crystal = LocalCrystalColors.current
    val isBn = language.isBangla
    var showAddInline by remember(transactionType) { mutableStateOf(false) }
    var customName by remember(transactionType) { mutableStateOf("") }
    var selectedColor by remember(transactionType) {
        mutableStateOf(
            if (transactionType == TransactionType.INCOME) CategoryEditorCatalog.colorSwatches[1]
            else CategoryEditorCatalog.colorSwatches[0]
        )
    }

    val activeCategories = remember(categories) {
        categories.filter { it.isActive }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge
                )
                if (autoDetectedBadge) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = if (crystal.isDark) 0.22f else 0.12f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (isBn) "অটো শনাক্ত" else "Auto",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            if (onAddCustomCategory != null) {
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = if (crystal.isDark) 0.20f else 0.10f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(0.9.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(role = Role.Button) { showAddInline = !showAddInline }
                        .testTag("${testTagPrefix}_add_category_chip")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = if (isBn) "নতুন ক্যাটাগরি যোগ করুন" else "Add Category",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = if (isBn) "নতুন ক্যাটাগরি" else "Add Category",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // Inline manual custom category creator (instantaneously synced across all flows)
        if (showAddInline && onAddCustomCategory != null) {
            Surface(
                color = crystal.glassSurfaceElevated,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.32f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("${testTagPrefix}_custom_cat_panel")
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = when (transactionType) {
                            TransactionType.INCOME -> if (isBn) "নতুন আয়ের ক্যাটাগরি যোগ করুন" else "Add Custom Income Category"
                            TransactionType.TRANSFER -> if (isBn) "নতুন ট্রান্সফার ক্যাটাগরি যোগ করুন" else "Add Custom Transfer Category"
                            TransactionType.EXPENSE -> if (isBn) "নতুন খরচের ক্যাটাগরি যোগ করুন" else "Add Custom Expense Category"
                        },
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )

                    OutlinedTextField(
                        value = customName,
                        onValueChange = { customName = it },
                        label = {
                            Text(if (isBn) "ক্যাটাগরির নাম লিখুন" else "Category Name")
                        },
                        placeholder = {
                            Text(
                                if (transactionType == TransactionType.INCOME) {
                                    if (isBn) "যেমন: টিউশনি, রিয়েল এস্টেট" else "e.g., Tuition, Rental Income"
                                } else {
                                    if (isBn) "যেমন: জিম, পোষা প্রাণী, পড়াশোনা" else "e.g., Gym, Pets, Education"
                                }
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("${testTagPrefix}_custom_cat_name_input")
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CategoryEditorCatalog.colorSwatches.take(8).forEach { swatch ->
                            val isSelectedSwatch = swatch == selectedColor
                            Box(
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(swatch)
                                    .border(
                                        width = if (isSelectedSwatch) 2.5.dp else 1.dp,
                                        color = if (isSelectedSwatch) MaterialTheme.colorScheme.onSurface else Color.White.copy(alpha = 0.6f),
                                        shape = CircleShape
                                    )
                                    .clickable(role = Role.RadioButton) { selectedColor = swatch },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelectedSwatch) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                showAddInline = false
                                customName = ""
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 44.dp)
                        ) {
                            Text(if (isBn) "বাতিল" else "Cancel")
                        }
                        Button(
                            onClick = {
                                val trimmed = customName.trim()
                                if (trimmed.isNotEmpty()) {
                                    val newItem = BuiltInCategoriesCatalog.createCustomCategory(
                                        type = transactionType,
                                        name = trimmed,
                                        accentColor = selectedColor
                                    )
                                    onAddCustomCategory(newItem)
                                    onSelectCategory(newItem)
                                    customName = ""
                                    showAddInline = false
                                }
                            },
                            enabled = customName.trim().isNotEmpty(),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 44.dp)
                                .testTag("${testTagPrefix}_custom_cat_save_button")
                        ) {
                            Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(if (isBn) "যোগ করুন" else "Save")
                        }
                    }
                }
            }
        }

        // Multi-line FlowRow of all expanded built-in + custom categories for easy visual scanning
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            activeCategories.forEach { item ->
                val isSelected = item.id == selectedCategoryId
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelectCategory(item) },
                    leadingIcon = {
                        Icon(
                            imageVector = resolveManagedCategoryIcon(item.iconKey),
                            contentDescription = null,
                            tint = if (isSelected) MaterialTheme.colorScheme.primary else item.accentColor,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    label = {
                        Text(
                            text = item.label(language),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = item.accentColor.copy(alpha = if (crystal.isDark) 0.24f else 0.16f),
                        selectedLabelColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.testTag("${testTagPrefix}_managed_cat_${item.id}")
                )
            }
        }
    }
}
