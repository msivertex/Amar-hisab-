package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalGroceryStore
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.data.local.TransactionEntity
import com.example.data.local.UserPrefs
import com.example.data.model.BuiltInCategoriesCatalog
import com.example.data.model.CategorySortMode
import com.example.data.model.ManagedCategoryItem
import com.example.data.model.TransactionType
import com.example.localization.AppLanguage
import com.example.localization.AppStrings
import com.example.localization.BdtFormatter
import com.example.ui.theme.HindSiliguriFamily
import com.example.ui.theme.LocalCrystalColors
import kotlinx.coroutines.delay

private sealed interface CategoryEditorTarget {
    data class Create(val defaultType: TransactionType) : CategoryEditorTarget
    data class Edit(val item: ManagedCategoryItem) : CategoryEditorTarget
}

@Composable
fun CategoriesManagementSubPage(
    prefs: UserPrefs,
    strings: AppStrings,
    transactions: List<TransactionEntity> = emptyList(),
    onReorderCategories: (TransactionType, List<String>) -> Unit,
    onSetSortMode: (CategorySortMode) -> Unit = {},
    onUpsertCategory: (ManagedCategoryItem) -> Unit = {},
    onDeleteCategory: (categoryId: String, isBuiltIn: Boolean) -> Unit = { _, _ -> },
    onResetDefaults: () -> Unit = {},
    onBack: () -> Unit
) {
    var editorTarget by remember { mutableStateOf<CategoryEditorTarget?>(null) }
    var selectedTab by remember { mutableStateOf(TransactionType.EXPENSE) }
    var statusNotice by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(statusNotice) {
        if (statusNotice != null) {
            delay(2800)
            statusNotice = null
        }
    }

    AnimatedContent(
        targetState = editorTarget,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "categoryEditorTransition"
    ) { target ->
        when (target) {
            is CategoryEditorTarget.Create -> {
                CategoryEditorScreen(
                    initialItem = null,
                    defaultType = target.defaultType,
                    language = prefs.language,
                    strings = strings,
                    onSave = { created ->
                        onUpsertCategory(created)
                        selectedTab = created.type
                        editorTarget = null
                    },
                    onDelete = null,
                    onBack = { editorTarget = null }
                )
            }

            is CategoryEditorTarget.Edit -> {
                CategoryEditorScreen(
                    initialItem = target.item,
                    defaultType = target.item.type,
                    language = prefs.language,
                    strings = strings,
                    onSave = { updated ->
                        onUpsertCategory(updated)
                        selectedTab = updated.type
                        editorTarget = null
                    },
                    onDelete = { toDelete ->
                        onDeleteCategory(toDelete.id, toDelete.isBuiltIn)
                        editorTarget = null
                    },
                    onBack = { editorTarget = null }
                )
            }

            null -> {
                CategoriesListContent(
                    prefs = prefs,
                    strings = strings,
                    transactions = transactions,
                    selectedTab = selectedTab,
                    statusNotice = statusNotice,
                    onSelectTab = { selectedTab = it },
                    onOpenCreate = { editorTarget = CategoryEditorTarget.Create(selectedTab) },
                    onOpenEdit = { item -> editorTarget = CategoryEditorTarget.Edit(item) },
                    onReorderCategories = onReorderCategories,
                    onSetSortMode = onSetSortMode,
                    onResetDefaults = {
                        onResetDefaults()
                        statusNotice = strings.categoriesManagement.resetSuccessNotice
                    },
                    onBack = onBack
                )
            }
        }
    }
}

@Composable
private fun CategoriesListContent(
    prefs: UserPrefs,
    strings: AppStrings,
    transactions: List<TransactionEntity>,
    selectedTab: TransactionType,
    statusNotice: String?,
    onSelectTab: (TransactionType) -> Unit,
    onOpenCreate: () -> Unit,
    onOpenEdit: (ManagedCategoryItem) -> Unit,
    onReorderCategories: (TransactionType, List<String>) -> Unit,
    onSetSortMode: (CategorySortMode) -> Unit,
    onResetDefaults: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val crystal = LocalCrystalColors.current
    val haptic = LocalHapticFeedback.current
    val lang = prefs.language
    val copy = strings.categoriesManagement
    val listState = rememberLazyListState()

    var showSortMenu by remember { mutableStateOf(false) }

    val expenseMerged = remember(prefs.managedCategoryOverrides, prefs.deletedCategoryIds) {
        BuiltInCategoriesCatalog.mergedFor(
            type = TransactionType.EXPENSE,
            overrides = prefs.managedCategoryOverrides,
            deletedIds = prefs.deletedCategoryIds
        )
    }
    val incomeMerged = remember(prefs.managedCategoryOverrides, prefs.deletedCategoryIds) {
        BuiltInCategoriesCatalog.mergedFor(
            type = TransactionType.INCOME,
            overrides = prefs.managedCategoryOverrides,
            deletedIds = prefs.deletedCategoryIds
        )
    }
    val transferMerged = remember(prefs.managedCategoryOverrides, prefs.deletedCategoryIds) {
        BuiltInCategoriesCatalog.mergedFor(
            type = TransactionType.TRANSFER,
            overrides = prefs.managedCategoryOverrides,
            deletedIds = prefs.deletedCategoryIds
        )
    }

    val activeMerged = when (selectedTab) {
        TransactionType.EXPENSE -> expenseMerged
        TransactionType.INCOME -> incomeMerged
        TransactionType.TRANSFER -> transferMerged
    }

    val savedOrder = prefs.categoryOrderFor(selectedTab)
    val sortMode = prefs.categorySortMode

    val sortedItems = remember(activeMerged, savedOrder, sortMode, transactions) {
        when (sortMode) {
            CategorySortMode.DEFAULT -> activeMerged
            CategorySortMode.CUSTOM -> BuiltInCategoriesCatalog.applyCustomOrder(activeMerged, savedOrder)
            CategorySortMode.MOST_USED -> {
                val freqById = activeMerged.associate { item ->
                    item.id to computeCategoryUsageCount(item, transactions)
                }
                activeMerged.sortedByDescending { freqById[it.id] ?: 0 }
            }
        }
    }

    var orderedItems by remember(sortedItems) {
        mutableStateOf(sortedItems)
    }

    var draggedItemId by remember { mutableStateOf<String?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var avgCardHeightPx by remember { mutableFloatStateOf(168f) }
    var resetScrollToken by remember { mutableStateOf(0) }

    LaunchedEffect(selectedTab, sortMode, resetScrollToken) {
        draggedItemId = null
        dragOffsetY = 0f
        listState.scrollToItem(0)
    }

    fun triggerHaptic() {
        if (prefs.hapticFeedbackEnabled) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    fun moveDraggedItemIfNeeded(itemId: String, deltaY: Float) {
        dragOffsetY += deltaY
        val currentIdx = orderedItems.indexOfFirst { it.id == itemId }
        if (currentIdx == -1) return
        val stepPx = (avgCardHeightPx * 0.72f).coerceAtLeast(88f)
        if (dragOffsetY > stepPx && currentIdx < orderedItems.lastIndex) {
            val mutable = orderedItems.toMutableList()
            val moved = mutable.removeAt(currentIdx)
            mutable.add(currentIdx + 1, moved)
            orderedItems = mutable
            dragOffsetY -= stepPx
            triggerHaptic()
        } else if (dragOffsetY < -stepPx && currentIdx > 0) {
            val mutable = orderedItems.toMutableList()
            val moved = mutable.removeAt(currentIdx)
            mutable.add(currentIdx - 1, moved)
            orderedItems = mutable
            dragOffsetY += stepPx
            triggerHaptic()
        }
    }

    fun finishDrag() {
        if (draggedItemId != null) {
            onReorderCategories(selectedTab, orderedItems.map { it.id })
        }
        draggedItemId = null
        dragOffsetY = 0f
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("categories_screen"),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 620.dp)
                .fillMaxSize()
        ) {
            // Top App Bar: Back + Title + Sort/Filter Dropdown + Add (+) Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 12.dp, top = 10.dp, bottom = 8.dp)
                    .testTag("categories_top_bar"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("categories_back_button")
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

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = copy.pageTitle,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = HindSiliguriFamily
                        ),
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = copy.pageSubtitle,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = HindSiliguriFamily
                        ),
                        color = crystal.mutedText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Sort / Filter Menu Trigger
                Box {
                    IconButton(
                        onClick = { showSortMenu = true },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("categories_sort_button")
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (sortMode != CategorySortMode.DEFAULT) {
                                MaterialTheme.colorScheme.primary.copy(alpha = if (crystal.isDark) 0.24f else 0.14f)
                            } else {
                                crystal.glassSurfaceElevated
                            },
                            border = BorderStroke(
                                1.dp,
                                if (sortMode != CategorySortMode.DEFAULT) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                                } else {
                                    crystal.glassBorder
                                }
                            ),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Sort,
                                    contentDescription = copy.sortMenuDesc,
                                    tint = if (sortMode != CategorySortMode.DEFAULT) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false },
                        shape = RoundedCornerShape(18.dp),
                        containerColor = if (crystal.isDark) Color(0xFF1A2234) else Color(0xFFFCFDFF),
                        border = BorderStroke(
                            1.dp,
                            if (crystal.isDark) Color.White.copy(alpha = 0.14f) else crystal.glassBorder
                        ),
                        shadowElevation = 10.dp,
                        modifier = Modifier
                            .widthIn(min = 248.dp)
                            .testTag("categories_sort_menu")
                    ) {
                        CategorySortDropdownRow(
                            title = copy.sortDefaultTitle,
                            subtitle = copy.sortDefaultSubtitle,
                            selected = sortMode == CategorySortMode.DEFAULT,
                            optionTag = "categories_sort_option_DEFAULT",
                            checkTag = "categories_sort_check_DEFAULT",
                            onClick = {
                                showSortMenu = false
                                onSetSortMode(CategorySortMode.DEFAULT)
                            }
                        )
                        CategorySortDropdownRow(
                            title = copy.sortCustomTitle,
                            subtitle = copy.sortCustomSubtitle,
                            selected = sortMode == CategorySortMode.CUSTOM,
                            optionTag = "categories_sort_option_CUSTOM",
                            checkTag = "categories_sort_check_CUSTOM",
                            onClick = {
                                showSortMenu = false
                                onSetSortMode(CategorySortMode.CUSTOM)
                            }
                        )
                        CategorySortDropdownRow(
                            title = copy.sortMostUsedTitle,
                            subtitle = copy.sortMostUsedSubtitle,
                            selected = sortMode == CategorySortMode.MOST_USED,
                            optionTag = "categories_sort_option_MOST_USED",
                            checkTag = "categories_sort_check_MOST_USED",
                            onClick = {
                                showSortMenu = false
                                onSetSortMode(CategorySortMode.MOST_USED)
                            }
                        )

                        HorizontalDivider(
                            thickness = 0.8.dp,
                            color = crystal.glassBorder,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )

                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = copy.resetDefaultsLabel,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontFamily = HindSiliguriFamily
                                    ),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.History,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            onClick = {
                                showSortMenu = false
                                onResetDefaults()
                                resetScrollToken++
                            },
                            modifier = Modifier.testTag("categories_reset_defaults_option")
                        )
                    }
                }

                // Add (+) Button
                IconButton(
                    onClick = onOpenCreate,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("categories_add_button")
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        shadowElevation = 4.dp,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = copy.addCategoryDesc,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }

            // Pill-shaped Selection Tabs: খরচ, আয়, ট্রান্সফার (with live counts)
            Surface(
                shape = RoundedCornerShape(26.dp),
                color = if (crystal.isDark) Color(0xFF141A29) else crystal.glassSurfaceElevated,
                border = BorderStroke(1.dp, crystal.glassBorder),
                shadowElevation = if (crystal.isDark) 6.dp else 3.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .testTag("categories_tab_bar")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CategoryTypePillTab(
                        label = copy.tabExpense,
                        countText = BdtFormatter.formatNumber(expenseMerged.size, lang),
                        selected = selectedTab == TransactionType.EXPENSE,
                        testTag = "categories_tab_EXPENSE",
                        modifier = Modifier.weight(1f),
                        onClick = { onSelectTab(TransactionType.EXPENSE) }
                    )
                    CategoryTypePillTab(
                        label = copy.tabIncome,
                        countText = BdtFormatter.formatNumber(incomeMerged.size, lang),
                        selected = selectedTab == TransactionType.INCOME,
                        testTag = "categories_tab_INCOME",
                        modifier = Modifier.weight(1f),
                        onClick = { onSelectTab(TransactionType.INCOME) }
                    )
                    CategoryTypePillTab(
                        label = copy.tabTransfer,
                        countText = BdtFormatter.formatNumber(transferMerged.size, lang),
                        selected = selectedTab == TransactionType.TRANSFER,
                        testTag = "categories_tab_TRANSFER",
                        modifier = Modifier.weight(1f),
                        onClick = { onSelectTab(TransactionType.TRANSFER) }
                    )
                }
            }

            // Optional brief notice banner (e.g., defaults restored)
            AnimatedVisibility(visible = statusNotice != null) {
                if (statusNotice != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = crystal.assetGreenBg,
                        border = BorderStroke(1.dp, crystal.assetGreen.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .testTag("categories_status_notice")
                    ) {
                        Text(
                            text = statusNotice,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = HindSiliguriFamily
                            ),
                            color = crystal.assetGreen,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            // Instruction Banner: "সাজাতে লং প্রেস করে ড্র্যাগ করুন"
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primary.copy(
                    alpha = if (crystal.isDark) 0.14f else 0.08f
                ),
                border = BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.primary.copy(
                        alpha = if (crystal.isDark) 0.30f else 0.20f
                    )
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .testTag("categories_drag_instruction")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.DragIndicator,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(
                        text = copy.dragInstruction,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Medium,
                            fontFamily = HindSiliguriFamily,
                            fontSize = 12.5.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface.copy(
                            alpha = if (crystal.isDark) 0.90f else 0.85f
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Category Cards List with Long-Press & Handle Drag-to-Reorder + Tap to Edit
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("categories_list"),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(
                    items = orderedItems,
                    key = { _, item -> item.id }
                ) { _, item ->
                    val isDragging = draggedItemId == item.id

                    CategoryManagementCard(
                        item = item,
                        lang = lang,
                        builtInLabel = copy.builtInTag,
                        customLabel = copy.customTag,
                        inactiveLabel = copy.inactiveTag,
                        dragHandleDesc = copy.dragHandleDesc,
                        subcategoriesSuffix = copy.subcategoriesSuffix,
                        isDragging = isDragging,
                        dragOffsetY = if (isDragging) dragOffsetY else 0f,
                        onMeasuredHeight = { h ->
                            if (h > 0) {
                                avgCardHeightPx = h.toFloat()
                            }
                        },
                        onClick = {
                            if (draggedItemId == null) {
                                onOpenEdit(item)
                            }
                        },
                        onDragStart = {
                            draggedItemId = item.id
                            dragOffsetY = 0f
                            triggerHaptic()
                        },
                        onDragDelta = { deltaY ->
                            moveDraggedItemIfNeeded(item.id, deltaY)
                        },
                        onDragEnd = { finishDrag() }
                    )
                }
            }
        }
    }
}

@Composable
private fun CategorySortDropdownRow(
    title: String,
    subtitle: String,
    selected: Boolean,
    optionTag: String,
    checkTag: String,
    onClick: () -> Unit
) {
    val crystal = LocalCrystalColors.current
    val primary = MaterialTheme.colorScheme.primary

    DropdownMenuItem(
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                        fontFamily = HindSiliguriFamily
                    ),
                    color = if (selected) primary else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = HindSiliguriFamily
                    ),
                    color = crystal.mutedText
                )
            }
        },
        trailingIcon = if (selected) {
            {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = primary,
                    modifier = Modifier
                        .size(20.dp)
                        .testTag(checkTag)
                )
            }
        } else null,
        onClick = onClick,
        modifier = Modifier
            .background(
                if (selected) primary.copy(alpha = if (crystal.isDark) 0.14f else 0.08f) else Color.Transparent
            )
            .testTag(optionTag)
    )
}

@Composable
private fun CategoryTypePillTab(
    label: String,
    countText: String,
    selected: Boolean,
    testTag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val crystal = LocalCrystalColors.current
    val primary = MaterialTheme.colorScheme.primary

    val containerColor by animateColorAsState(
        targetValue = if (selected) primary else Color.Transparent,
        label = "catTabContainer"
    )
    val textColor by animateColorAsState(
        targetValue = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
        label = "catTabText"
    )
    val badgeBg by animateColorAsState(
        targetValue = if (selected) {
            Color.White.copy(alpha = 0.22f)
        } else {
            primary.copy(alpha = if (crystal.isDark) 0.20f else 0.12f)
        },
        label = "catTabBadgeBg"
    )
    val badgeText by animateColorAsState(
        targetValue = if (selected) Color.White else primary,
        label = "catTabBadgeText"
    )

    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = containerColor,
        modifier = modifier
            .heightIn(min = 44.dp)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                    fontFamily = HindSiliguriFamily,
                    fontSize = 13.5.sp
                ),
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.size(5.dp))
            Surface(
                shape = CircleShape,
                color = badgeBg
            ) {
                Text(
                    text = countText,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = HindSiliguriFamily,
                        fontSize = 11.sp
                    ),
                    color = badgeText,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                )
            }
        }
    }
}

@Composable
private fun CategoryManagementCard(
    item: ManagedCategoryItem,
    lang: AppLanguage,
    builtInLabel: String,
    customLabel: String,
    inactiveLabel: String,
    dragHandleDesc: String,
    subcategoriesSuffix: String,
    isDragging: Boolean,
    dragOffsetY: Float,
    onMeasuredHeight: (Int) -> Unit,
    onClick: () -> Unit,
    onDragStart: () -> Unit,
    onDragDelta: (Float) -> Unit,
    onDragEnd: () -> Unit
) {
    val crystal = LocalCrystalColors.current
    val primary = MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape(18.dp)

    val latestDragStart by rememberUpdatedState(onDragStart)
    val latestDragDelta by rememberUpdatedState(onDragDelta)
    val latestDragEnd by rememberUpdatedState(onDragEnd)

    val cardElevation by animateDpAsState(
        targetValue = if (isDragging) 16.dp else if (crystal.isDark) 6.dp else 4.dp,
        label = "catCardElevation"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isDragging) primary else if (crystal.isDark) Color.White.copy(alpha = 0.14f) else crystal.glassBorder,
        label = "catCardBorder"
    )

    Surface(
        shape = shape,
        color = if (crystal.isDark) Color(0xFF161C2B) else crystal.glassSurfaceElevated,
        border = BorderStroke(if (isDragging) 1.6.dp else 1.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .zIndex(if (isDragging) 10f else 0f)
            .graphicsLayer {
                translationY = dragOffsetY
                val scale = if (isDragging) 1.02f else 1f
                scaleX = scale
                scaleY = scale
                alpha = if (item.isActive) 1f else 0.72f
            }
            .shadow(
                elevation = cardElevation,
                shape = shape,
                ambientColor = if (isDragging) primary.copy(alpha = 0.30f) else Color.Black.copy(alpha = 0.16f),
                spotColor = if (isDragging) primary.copy(alpha = 0.34f) else Color.Black.copy(alpha = 0.20f)
            )
            .onSizeChanged { onMeasuredHeight(it.height) }
            .pointerInput(item.id) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { latestDragStart() },
                    onDragEnd = { latestDragEnd() },
                    onDragCancel = { latestDragEnd() },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        latestDragDelta(dragAmount.y)
                    }
                )
            }
            .clickable(onClick = onClick)
            .testTag("category_card_${item.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 6.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Unique Category Icon with Soft Background Tint
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        item.accentColor.copy(alpha = if (crystal.isDark) 0.22f else 0.14f)
                    )
                    .border(
                        width = 1.dp,
                        color = item.accentColor.copy(alpha = if (crystal.isDark) 0.38f else 0.28f),
                        shape = RoundedCornerShape(14.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = resolveManagedCategoryIcon(item.iconKey),
                    contentDescription = item.label(lang),
                    tint = item.accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Category Title + "বিল্ট-ইন" / "কাস্টম" Tag + Subcategory count
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = item.label(lang),
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = HindSiliguriFamily,
                        fontSize = 15.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val badgeLabel = if (item.isBuiltIn) builtInLabel else customLabel
                    val badgeColor = if (item.isBuiltIn) primary else item.accentColor
                    Surface(
                        shape = CircleShape,
                        color = badgeColor.copy(alpha = if (crystal.isDark) 0.20f else 0.11f),
                        border = BorderStroke(
                            width = 0.7.dp,
                            color = badgeColor.copy(alpha = if (crystal.isDark) 0.36f else 0.24f)
                        ),
                        modifier = Modifier.testTag(
                            if (item.isBuiltIn) "category_builtin_badge_${item.id}" else "category_custom_badge_${item.id}"
                        )
                    ) {
                        Text(
                            text = badgeLabel,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = HindSiliguriFamily,
                                fontSize = 11.sp
                            ),
                            color = badgeColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }

                    if (!item.isActive) {
                        Surface(
                            shape = CircleShape,
                            color = crystal.mutedText.copy(alpha = 0.16f)
                        ) {
                            Text(
                                text = inactiveLabel,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Medium,
                                    fontFamily = HindSiliguriFamily,
                                    fontSize = 10.5.sp
                                ),
                                color = crystal.mutedText,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                    }

                    if (item.subcategories.isNotEmpty()) {
                        val subCountLabel = if (lang == AppLanguage.BN) {
                            "${BdtFormatter.formatNumber(item.subcategories.size, lang)}$subcategoriesSuffix"
                        } else {
                            "${item.subcategories.size} $subcategoriesSuffix"
                        }
                        Text(
                            text = subCountLabel,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = HindSiliguriFamily
                            ),
                            color = crystal.mutedText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Drag Handle (::) on the right side (48dp touch target)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .pointerInput(item.id) {
                        detectDragGestures(
                            onDragStart = { latestDragStart() },
                            onDragEnd = { latestDragEnd() },
                            onDragCancel = { latestDragEnd() },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                latestDragDelta(dragAmount.y)
                            }
                        )
                    }
                    .testTag("category_drag_handle_${item.id}"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.DragIndicator,
                    contentDescription = dragHandleDesc,
                    tint = if (isDragging) primary else crystal.mutedText.copy(alpha = 0.85f),
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

private fun computeCategoryUsageCount(
    item: ManagedCategoryItem,
    transactions: List<TransactionEntity>
): Int {
    if (transactions.isEmpty()) return 0
    return transactions.count { tx ->
        tx.type == item.type && (
            (item.legacyCategory != null && tx.category == item.legacyCategory) ||
                (item.en.isNotBlank() && (tx.subcategory.equals(item.en, ignoreCase = true) || tx.title.contains(item.en, ignoreCase = true))) ||
                (item.bn.isNotBlank() && (tx.subcategory.equals(item.bn, ignoreCase = true) || tx.title.contains(item.bn, ignoreCase = true)))
            )
    }
}

internal fun resolveManagedCategoryIcon(iconKey: String): ImageVector = when (iconKey) {
    "restaurant" -> Icons.Filled.Restaurant
    "groceries" -> Icons.Filled.LocalGroceryStore
    "home" -> Icons.Filled.Home
    "transport" -> Icons.Filled.DirectionsCar
    "shopping" -> Icons.Filled.ShoppingBag
    "health" -> Icons.Filled.MedicalServices
    "entertainment" -> Icons.Filled.Movie
    "payments" -> Icons.Filled.Payments
    "work" -> Icons.Filled.Work
    "business" -> Icons.Filled.Storefront
    "trending_up" -> Icons.AutoMirrored.Filled.TrendingUp
    "gift" -> Icons.Filled.CardGiftcard
    "transfer" -> Icons.AutoMirrored.Filled.CompareArrows
    "bank" -> Icons.Filled.AccountBalance
    "mfs" -> Icons.Filled.PhoneAndroid
    "education" -> Icons.Filled.School
    "travel" -> Icons.Filled.Flight
    "bills" -> Icons.AutoMirrored.Filled.ReceiptLong
    "savings" -> Icons.Filled.Savings
    else -> Icons.Filled.Category
}
