package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.DeviceTimeContext
import com.example.data.support.CustomerSupportCatalog
import com.example.data.support.SUPPORT_WHATSAPP_DIGITS
import com.example.data.support.SUPPORT_WHATSAPP_DISPLAY
import com.example.data.support.SupportCategory
import com.example.data.support.SupportCategoryId
import com.example.data.support.SupportChatMessage
import com.example.data.support.SupportQuickOption
import com.example.data.support.SupportSender
import com.example.data.support.SupportUiState
import com.example.localization.AppLanguage
import com.example.localization.AppStrings
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalCrystalColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CustomerSupportScreen(
    supportState: SupportUiState,
    language: AppLanguage,
    deviceTimeContext: DeviceTimeContext,
    strings: AppStrings,
    onEnsureInitialized: () -> Unit,
    onSelectCategory: (SupportCategoryId?) -> Unit,
    onSelectQuickOption: (SupportQuickOption) -> Unit,
    onSendMessage: (String) -> Unit,
    onRequestLiveCareAfterTroubleshooting: () -> Unit,
    onResetChat: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val crystal = LocalCrystalColors.current
    val csStrings = strings.customerSupport
    val context = LocalContext.current
    val listState = rememberLazyListState()
    var inputText by remember { mutableStateOf("") }

    // Immediately initiate greeting on open (idempotent — zero duplicates)
    LaunchedEffect(Unit) {
        onEnsureInitialized()
    }

    // Auto-scroll smoothly whenever messages, WhatsApp cards, or typing indicator update
    val lazyItemCount = remember(supportState.messages, supportState.isTyping) {
        supportState.messages.sumOf { msg ->
            1 + if (msg.sender == SupportSender.ASSISTANT && msg.showWhatsAppCard) 1 else 0
        } + if (supportState.isTyping) 1 else 0
    }
    LaunchedEffect(lazyItemCount) {
        if (lazyItemCount > 0) {
            listState.animateScrollToItem(lazyItemCount - 1)
        }
    }

    val timeFormatter = remember(deviceTimeContext.timeZoneId, language) {
        val locale = if (language == AppLanguage.BN) Locale("bn", "BD") else Locale.US
        SimpleDateFormat("hh:mm a", locale).apply {
            timeZone = deviceTimeContext.timeZone
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("customer_support_screen"),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxSize()
                .navigationBarsPadding()
                .imePadding()
        ) {
            // 1. Sleek Frosted Crystal Support Header
            SupportChatHeader(
                title = csStrings.headerTitle,
                isOnline = supportState.isOnline,
                statusText = if (supportState.isOnline) csStrings.statusOnline else csStrings.statusOffline,
                backDesc = strings.close,
                resetDesc = csStrings.resetChatDesc,
                onBack = onBack,
                onReset = onResetChat
            )

            // 2. Smart Auto-Hiding Offline Notice Banner
            AnimatedVisibility(
                visible = !supportState.isOnline,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = crystal.warningAmberBg.copy(alpha = if (crystal.isDark) 0.48f else 0.85f),
                    border = BorderStroke(1.dp, crystal.warningAmber.copy(alpha = 0.45f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .testTag("support_offline_banner")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CloudOff,
                            contentDescription = null,
                            tint = crystal.warningAmber,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = csStrings.offlineBannerText,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 3. Conversation Stream
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .testTag("support_messages_list"),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                supportState.messages.forEach { message ->
                    item(key = message.id) {
                        val timeLabel = remember(message.timestampMillis, deviceTimeContext.timeZoneId) {
                            timeFormatter.format(Date(message.timestampMillis))
                        }
                        SupportMessageBubbleItem(
                            message = message,
                            timeLabel = timeLabel,
                            strings = strings,
                            onTapFollowUp = { chipText -> onSendMessage(chipText) },
                            onTapNotResolved = onRequestLiveCareAfterTroubleshooting
                        )
                    }
                    if (message.sender == SupportSender.ASSISTANT && message.showWhatsAppCard) {
                        item(key = "${message.id}_whatsapp") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 38.dp, end = 12.dp)
                            ) {
                                WhatsAppEscalationCard(
                                    strings = strings,
                                    onOpenWhatsApp = { openWhatsAppCustomerCare(context, message.text) },
                                    onCopyNumber = { copyWhatsAppNumber(context) }
                                )
                            }
                        }
                    }
                }

                if (supportState.isTyping) {
                    item(key = "typing_indicator") {
                        SupportTypingIndicatorBubble(
                            label = csStrings.typingLabel
                        )
                    }
                }
            }

            // 4. Categorized Quick Replies (6 Categories + Smooth Expandable Sub-Options)
            SupportQuickReplyDock(
                selectedCategoryId = supportState.selectedCategoryId,
                strings = strings,
                onSelectCategory = onSelectCategory,
                onSelectOption = onSelectQuickOption
            )

            // 5. Frosted Glass Message Composer Bar
            SupportComposerBar(
                text = inputText,
                placeholder = csStrings.inputPlaceholder,
                sendDesc = csStrings.sendDesc,
                isSending = supportState.isTyping,
                onTextChange = { inputText = it },
                onSend = {
                    val trimmed = inputText.trim()
                    if (trimmed.isNotEmpty() && !supportState.isTyping) {
                        inputText = ""
                        onSendMessage(trimmed)
                    }
                }
            )
        }
    }
}

@Composable
private fun SupportChatHeader(
    title: String,
    isOnline: Boolean,
    statusText: String,
    backDesc: String,
    resetDesc: String,
    onBack: () -> Unit,
    onReset: () -> Unit
) {
    val crystal = LocalCrystalColors.current
    Surface(
        color = crystal.glassSurfaceElevated,
        border = BorderStroke(1.dp, crystal.glassBorder),
        shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("support_back_button")
            ) {
                Surface(
                    shape = CircleShape,
                    color = crystal.glassSurface,
                    border = BorderStroke(1.dp, crystal.glassBorder),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = backDesc,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Frosted Support Emblem
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.secondary
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.SupportAgent,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isOnline) crystal.assetGreen else crystal.warningAmber)
                    )
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = if (isOnline) crystal.assetGreen else crystal.warningAmber,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("support_status_badge")
                    )
                }
            }

            IconButton(
                onClick = onReset,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("support_reset_chat_button")
            ) {
                Surface(
                    shape = CircleShape,
                    color = crystal.glassSurface,
                    border = BorderStroke(1.dp, crystal.glassBorder),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = resetDesc,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SupportMessageBubbleItem(
    message: SupportChatMessage,
    timeLabel: String,
    strings: AppStrings,
    onTapFollowUp: (String) -> Unit,
    onTapNotResolved: () -> Unit
) {
    val crystal = LocalCrystalColors.current
    val csStrings = strings.customerSupport
    val isUser = message.sender == SupportSender.USER

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(0.92f)
        ) {
            if (!isUser) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = if (crystal.isDark) 0.24f else 0.14f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .size(30.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.SupportAgent,
                            contentDescription = csStrings.assistantLabel,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.weight(0.12f))
            }

            Column(
                modifier = Modifier.weight(1f, fill = false),
                horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
            ) {
                val bubbleShape = if (isUser) {
                    RoundedCornerShape(topStart = 20.dp, topEnd = 6.dp, bottomStart = 20.dp, bottomEnd = 20.dp)
                } else {
                    RoundedCornerShape(topStart = 6.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 20.dp)
                }

                if (isUser) {
                    Box(
                        modifier = Modifier
                            .clip(bubbleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.secondary
                                    )
                                )
                            )
                            .padding(horizontal = 14.dp, vertical = 11.dp)
                    ) {
                        Text(
                            text = message.text,
                            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                            color = Color.White
                        )
                    }
                } else {
                    val borderCol = if (message.isOfflineNotice) {
                        crystal.warningAmber.copy(alpha = 0.45f)
                    } else {
                        crystal.glassBorder
                    }
                    Surface(
                        shape = bubbleShape,
                        color = crystal.glassSurfaceElevated,
                        border = BorderStroke(1.dp, borderCol)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = message.text,
                                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            if (message.followUpChips.isNotEmpty()) {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    message.followUpChips.forEach { chip ->
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.primary.copy(
                                                alpha = if (crystal.isDark) 0.18f else 0.10f
                                            ),
                                            border = BorderStroke(
                                                1.dp,
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                                            ),
                                            modifier = Modifier
                                                .clip(CircleShape)
                                                .clickable { onTapFollowUp(chip) }
                                        ) {
                                            Text(
                                                text = chip,
                                                style = MaterialTheme.typography.labelMedium.copy(
                                                    fontWeight = FontWeight.SemiBold
                                                ),
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = timeLabel,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = crystal.mutedText,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                // Controlled Escalation Path: Offer "Not resolved — Request live support" ONLY after troubleshooting
                if (!isUser && message.offerNotResolvedAction && !message.showWhatsAppCard) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        shape = CircleShape,
                        color = crystal.glassSurface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { onTapNotResolved() }
                            .testTag("support_not_resolved_chip")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.HelpOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = csStrings.notResolvedChip,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WhatsAppEscalationCard(
    strings: AppStrings,
    onOpenWhatsApp: () -> Unit,
    onCopyNumber: () -> Unit
) {
    val crystal = LocalCrystalColors.current
    val csStrings = strings.customerSupport
    val scope = rememberCoroutineScope()
    var copied by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = crystal.assetGreen.copy(alpha = if (crystal.isDark) 0.16f else 0.10f),
        border = BorderStroke(1.2.dp, crystal.assetGreen.copy(alpha = 0.45f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("support_whatsapp_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(crystal.assetGreen),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Chat,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = csStrings.whatsappCardTitle,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Icon(
                            imageVector = Icons.Filled.Verified,
                            contentDescription = null,
                            tint = crystal.assetGreen,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Text(
                        text = SUPPORT_WHATSAPP_DISPLAY,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = crystal.assetGreen,
                        modifier = Modifier.testTag("support_whatsapp_number")
                    )
                }
                if (copied) {
                    Text(
                        text = csStrings.whatsappCopiedNotice,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = crystal.assetGreen
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onOpenWhatsApp,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = crystal.assetGreen,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("support_whatsapp_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Chat,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = csStrings.whatsappOpenBtn,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                OutlinedButton(
                    onClick = {
                        onCopyNumber()
                        copied = true
                        scope.launch {
                            delay(2000L)
                            copied = false
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, crystal.assetGreen.copy(alpha = 0.55f)),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("support_copy_whatsapp_button")
                ) {
                    Icon(
                        imageVector = if (copied) Icons.Filled.Check else Icons.Filled.ContentCopy,
                        contentDescription = csStrings.whatsappCopyBtn,
                        tint = crystal.assetGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = csStrings.whatsappCopyBtn,
                        color = crystal.assetGreen,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }
        }
    }
}

@Composable
private fun SupportTypingIndicatorBubble(label: String) {
    val crystal = LocalCrystalColors.current
    val transition = rememberInfiniteTransition(label = "supportTyping")
    val dot1 by transition.animateFloat(
        initialValue = 0f,
        targetValue = -5f,
        animationSpec = infiniteRepeatable(
            animation = tween(420, delayMillis = 0, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot1"
    )
    val dot2 by transition.animateFloat(
        initialValue = 0f,
        targetValue = -5f,
        animationSpec = infiniteRepeatable(
            animation = tween(420, delayMillis = 140, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot2"
    )
    val dot3 by transition.animateFloat(
        initialValue = 0f,
        targetValue = -5f,
        animationSpec = infiniteRepeatable(
            animation = tween(420, delayMillis = 280, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot3"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.testTag("support_typing_indicator")
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary.copy(alpha = if (crystal.isDark) 0.24f else 0.14f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
            modifier = Modifier.size(30.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.SupportAgent,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(topStart = 6.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 18.dp),
            color = crystal.glassSurfaceElevated,
            border = BorderStroke(1.dp, crystal.glassBorder)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(dot1, dot2, dot3).forEach { yOffset ->
                    Box(
                        modifier = Modifier
                            .offset(y = yOffset.dp)
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = crystal.mutedText
                )
            }
        }
    }
}

@Composable
private fun SupportQuickReplyDock(
    selectedCategoryId: SupportCategoryId?,
    strings: AppStrings,
    onSelectCategory: (SupportCategoryId?) -> Unit,
    onSelectOption: (SupportQuickOption) -> Unit
) {
    val crystal = LocalCrystalColors.current
    val csStrings = strings.customerSupport
    val selectedCategory = selectedCategoryId?.let { CustomerSupportCatalog.findCategory(it) }

    Surface(
        color = crystal.glassSurface,
        border = BorderStroke(1.dp, crystal.glassBorder),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("support_quick_reply_dock")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = selectedCategory?.titleBn ?: csStrings.quickTopicsLabel,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                if (selectedCategory != null) {
                    Text(
                        text = csStrings.allCategoriesChip,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = crystal.mutedText,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { onSelectCategory(null) }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("support_all_categories_btn")
                    )
                }
            }

            // Row of 6 Main Categories
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CustomerSupportCatalog.categories.forEach { category ->
                    val isSelected = category.id == selectedCategoryId
                    SupportCategoryChip(
                        category = category,
                        selected = isSelected,
                        onClick = {
                            onSelectCategory(if (isSelected) null else category.id)
                        }
                    )
                }
            }

            // Expandable Sub-Options for the Selected Category
            AnimatedVisibility(
                visible = selectedCategory != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                selectedCategory?.let { cat ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 14.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        cat.options.forEach { option ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = crystal.glassSurfaceElevated,
                                border = BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.38f)
                                ),
                                modifier = Modifier
                                    .heightIn(min = 40.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { onSelectOption(option) }
                                    .testTag("support_opt_${option.id}")
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(horizontal = 13.dp, vertical = 9.dp)
                                ) {
                                    Text(
                                        text = option.labelBn,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SupportCategoryChip(
    category: SupportCategory,
    selected: Boolean,
    onClick: () -> Unit
) {
    val crystal = LocalCrystalColors.current
    val icon: ImageVector = when (category.id) {
        SupportCategoryId.TRANSACTIONS_ENTRY -> Icons.Filled.AccountBalanceWallet
        SupportCategoryId.LIVE_MARKET -> Icons.Filled.CurrencyExchange
        SupportCategoryId.INSIGHTS_REPORTS -> Icons.Filled.Insights
        SupportCategoryId.DATETIME_PERMISSIONS -> Icons.Filled.Schedule
        SupportCategoryId.BACKUP_SECURITY -> Icons.Filled.Security
        SupportCategoryId.OTHER_SUPPORT -> Icons.Filled.SupportAgent
    }

    val containerColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        crystal.glassSurfaceElevated
    }
    val contentColor = if (selected) {
        Color.White
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val borderColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        crystal.glassBorder
    }

    Surface(
        shape = CircleShape,
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier
            .heightIn(min = 40.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .testTag("support_cat_${category.id.name.lowercase(Locale.ROOT)}")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) Color.White else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = category.titleBn,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = contentColor,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun SupportComposerBar(
    text: String,
    placeholder: String,
    sendDesc: String,
    isSending: Boolean,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit
) {
    val crystal = LocalCrystalColors.current
    val canSend = text.trim().isNotEmpty() && !isSending

    Surface(
        color = crystal.glassSurfaceElevated,
        border = BorderStroke(1.dp, crystal.glassBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                placeholder = {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyMedium,
                        color = crystal.mutedText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                singleLine = false,
                maxLines = 3,
                shape = RoundedCornerShape(22.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = crystal.glassBorder,
                    focusedContainerColor = crystal.glassSurface,
                    unfocusedContainerColor = crystal.glassSurface
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { if (canSend) onSend() }),
                modifier = Modifier
                    .weight(1f)
                    .testTag("support_message_input")
            )

            Surface(
                shape = CircleShape,
                color = if (canSend) {
                    MaterialTheme.colorScheme.primary
                } else {
                    crystal.glassSurface
                },
                border = BorderStroke(
                    1.dp,
                    if (canSend) MaterialTheme.colorScheme.primary else crystal.glassBorder
                ),
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .clickable(enabled = canSend, onClick = onSend)
                    .testTag("support_send_button")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = sendDesc,
                        tint = if (canSend) Color.White else crystal.mutedText,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

private fun openWhatsAppCustomerCare(context: Context, contextNote: String) {
    val prefilled = Uri.encode("আসসালামু আলাইকুম, 'আমার হিসাব' অ্যাপের সাপোর্ট প্রয়োজন।")
    val waUri = Uri.parse("https://wa.me/$SUPPORT_WHATSAPP_DIGITS?text=$prefilled")
    val intent = Intent(Intent.ACTION_VIEW, waUri).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching {
        context.startActivity(intent)
    }.onFailure {
        // Fallback to dialer if WhatsApp & browser are unavailable
        val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$SUPPORT_WHATSAPP_DISPLAY")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(dialIntent) }
    }
}

private fun copyWhatsAppNumber(context: Context) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    clipboard?.setPrimaryClip(ClipData.newPlainText("Amar Hisab Support", SUPPORT_WHATSAPP_DISPLAY))
}
