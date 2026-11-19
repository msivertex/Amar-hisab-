package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalGroceryStore
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.EventNote
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import coil.compose.AsyncImage
import com.example.R
import com.example.core.GoogleAccountProfile
import com.example.core.GoogleAuthManager
import com.example.data.local.AccountEntity
import com.example.data.model.AccountType
import com.example.data.model.AppTab
import com.example.data.model.AuthMode
import com.example.data.model.ExpenseCategory
import com.example.localization.AppLanguage
import com.example.localization.AppStrings
import com.example.localization.BdtFormatter
import com.example.ui.theme.HindSiliguriFamily
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalCrystalColors
import com.example.ui.theme.SkyCyanBright
import com.example.ui.theme.SkyCyanPrimary

@Composable
fun CrystalMeshBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val crystal = LocalCrystalColors.current
    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            crystal.canvasTop,
                            crystal.canvasMid,
                            crystal.canvasBottom
                        )
                    )
                )
                if (!crystal.isDark) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(crystal.orbPrimary, Color.Transparent),
                            center = Offset(size.width * 0.85f, size.height * 0.10f),
                            radius = size.width * 0.65f
                        ),
                        center = Offset(size.width * 0.85f, size.height * 0.10f),
                        radius = size.width * 0.65f
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(crystal.orbSecondary, Color.Transparent),
                            center = Offset(size.width * 0.12f, size.height * 0.42f),
                            radius = size.width * 0.55f
                        ),
                        center = Offset(size.width * 0.12f, size.height * 0.42f),
                        radius = size.width * 0.55f
                    )
                }
            }
    ) {
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onBackground) {
            content()
        }
    }
}

@Composable
fun CrystalCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    elevated: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val crystal = LocalCrystalColors.current
    val shape = RoundedCornerShape(cornerRadius)
    val baseColor = if (elevated) crystal.glassSurfaceElevated else crystal.glassSurface
    val cardGradient = if (crystal.isDark) {
        listOf(baseColor, baseColor)
    } else {
        listOf(
            baseColor,
            baseColor.copy(alpha = (baseColor.alpha - 0.08f).coerceAtLeast(0.5f))
        )
    }

    val cardModifier = modifier
        .shadow(
            elevation = if (crystal.isDark) 12.dp else 8.dp,
            shape = shape,
            ambientColor = if (crystal.isDark) Color.Black.copy(alpha = 0.65f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
            spotColor = if (crystal.isDark) Color.Black.copy(alpha = 0.70f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
        )
        .clip(shape)
        .background(brush = Brush.linearGradient(colors = cardGradient))
        .border(
            width = 1.dp,
            brush = Brush.linearGradient(
                colors = listOf(
                    crystal.glassBorder,
                    crystal.glassHighlight
                )
            ),
            shape = shape
        )
        .let { m ->
            if (onClick != null) m.clickable(onClick = onClick) else m
        }

    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
        Column(
            modifier = cardModifier.padding(16.dp),
            content = content
        )
    }
}

fun resolveAccountCardGradient(account: AccountEntity): List<Color> {
    val providerLower = account.bankOrProvider.lowercase()
    return when {
        account.type == AccountType.MFS && providerLower.contains("bkash") ->
            listOf(Color(0xFFE91E79), Color(0xFFBE185D), Color(0xFF6B0F3A))
        account.type == AccountType.MFS && providerLower.contains("nagad") ->
            listOf(Color(0xFFFB923C), Color(0xFFEA580C), Color(0xFF9A3412))
        account.type == AccountType.MFS && providerLower.contains("rocket") ->
            listOf(Color(0xFFA855F7), Color(0xFF7E22CE), Color(0xFF4C1D95))
        account.type == AccountType.MFS && providerLower.contains("upay") ->
            listOf(Color(0xFFF59E0B), Color(0xFF0284C7), Color(0xFF0C4A6E))
        account.type == AccountType.CASH ->
            listOf(Color(0xFF10B981), Color(0xFF047857), Color(0xFF064E3B))
        account.type == AccountType.DEBIT_BANK && providerLower.contains("trust") ->
            listOf(Color(0xFF16A34A), Color(0xFF1E5631), Color(0xFF0A2E18))
        account.type == AccountType.DEBIT_BANK && (providerLower.contains("islami") || providerLower.contains("sonali")) ->
            listOf(Color(0xFF059669), Color(0xFF065F46), Color(0xFF022C22))
        account.type == AccountType.DEBIT_BANK && (providerLower.contains("city") || providerLower.contains("dbbl") || providerLower.contains("dutch")) ->
            listOf(Color(0xFF2563EB), Color(0xFF1E40AF), Color(0xFF0F172A))
        else -> {
            val baseColor = Color(account.colorArgb)
            val midColor = Color(
                red = (baseColor.red * 0.78f).coerceIn(0f, 1f),
                green = (baseColor.green * 0.78f).coerceIn(0f, 1f),
                blue = (baseColor.blue * 0.86f).coerceIn(0f, 1f),
                alpha = 1f
            )
            val deeperColor = Color(
                red = (baseColor.red * 0.42f).coerceIn(0f, 1f),
                green = (baseColor.green * 0.45f).coerceIn(0f, 1f),
                blue = (baseColor.blue * 0.62f).coerceIn(0f, 1f),
                alpha = 1f
            )
            listOf(baseColor, midColor, deeperColor)
        }
    }
}

@Composable
fun AccountVisualCard(
    account: AccountEntity,
    language: AppLanguage,
    strings: AppStrings,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    isPeekingBackground: Boolean = false,
    elevation: Dp = 18.dp,
    onClick: (() -> Unit)? = null
) {
    val gradientColors = resolveAccountCardGradient(account)
    val shadowTint = gradientColors.first()
    val shape = RoundedCornerShape(24.dp)
    val cardTypeLabel = when (account.type) {
        AccountType.DEBIT_BANK -> strings.dashboard.cardTypeDebit
        AccountType.CREDIT_CARD -> strings.dashboard.cardTypeCredit
        AccountType.MFS -> strings.dashboard.cardTypeMfs
        AccountType.CASH -> strings.dashboard.cardTypeCash
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(if (compact) 1.76f else 1.586f, matchHeightConstraintsFirst = false)
            .heightIn(
                min = if (compact) 164.dp else 208.dp,
                max = if (compact) 202.dp else 252.dp
            )
            .shadow(
                elevation = elevation,
                shape = shape,
                ambientColor = shadowTint.copy(alpha = 0.42f),
                spotColor = shadowTint.copy(alpha = 0.48f)
            )
            .clip(shape)
            .background(
                brush = Brush.linearGradient(
                    colors = gradientColors,
                    start = Offset(0f, 0f),
                    end = Offset(980f, 700f)
                )
            )
            .border(
                width = 1.25.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.62f),
                        Color.White.copy(alpha = 0.16f)
                    )
                ),
                shape = shape
            )
            .let { m -> if (onClick != null) m.clickable(onClick = onClick) else m }
    ) {
        // Metallic card geometric arcs & diagonal glass sheen
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = Color.White.copy(alpha = 0.14f),
                radius = size.height * 0.82f,
                center = Offset(size.width * 0.90f, size.height * 0.08f)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.07f),
                radius = size.height * 0.58f,
                center = Offset(size.width * 0.70f, size.height * 0.98f)
            )
            drawCircle(
                color = Color.Black.copy(alpha = 0.14f),
                radius = size.height * 0.52f,
                center = Offset(size.width * 0.05f, size.height * 0.94f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = if (isPeekingBackground) 18.dp else if (compact) 20.dp else 22.dp,
                    vertical = if (isPeekingBackground) 10.dp else if (compact) 15.dp else 19.dp
                ),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Row: Wallet/Bank Identity + (Balance Preview when peeking OR Network Emblem when front)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        color = Color.White.copy(alpha = 0.22f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.38f))
                    ) {
                        Icon(
                            imageVector = when (account.type) {
                                AccountType.DEBIT_BANK -> Icons.Filled.AccountBalance
                                AccountType.CREDIT_CARD -> Icons.Filled.CreditCard
                                AccountType.MFS -> Icons.Filled.PhoneAndroid
                                AccountType.CASH -> Icons.Filled.Payments
                            },
                            contentDescription = cardTypeLabel,
                            tint = Color.White,
                            modifier = Modifier
                                .padding(if (isPeekingBackground) 6.dp else 7.dp)
                                .size(if (isPeekingBackground) 16.dp else 18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = account.name.ifBlank { account.bankOrProvider },
                            style = (if (isPeekingBackground) {
                                MaterialTheme.typography.titleSmall
                            } else {
                                MaterialTheme.typography.titleMedium
                            }).copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val subLabel = if (isPeekingBackground) {
                            if (account.bankOrProvider.isNotBlank() &&
                                !account.bankOrProvider.equals(account.name, ignoreCase = true)
                            ) {
                                "$cardTypeLabel • ${account.bankOrProvider}"
                            } else {
                                cardTypeLabel
                            }
                        } else {
                            buildString {
                                append(account.bankOrProvider)
                                if (account.accountSubtype.isNotBlank()) {
                                    append(" • ")
                                    append(account.accountSubtype)
                                }
                            }
                        }
                        Text(
                            text = subLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.86f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))
                if (isPeekingBackground) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.24f),
                        shape = CircleShape,
                        border = BorderStroke(0.7.dp, Color.White.copy(alpha = 0.28f))
                    ) {
                        Text(
                            text = BdtFormatter.format(account.balance, language),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontFamily = JetBrainsMonoFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            ),
                            color = Color.White.copy(alpha = 0.92f),
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                } else if (account.type != AccountType.CASH) {
                    CardNetworkOrProviderBadge(account = account)
                }
            }

            // Middle Row: EMV Metallic Chip + Contactless Payment Waves + Masked Digits
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    EmvCardChip(subtle = account.type == AccountType.MFS || account.type == AccountType.CASH)
                    Icon(
                        imageVector = Icons.Filled.Wifi,
                        contentDescription = "Contactless Payment",
                        tint = Color.White.copy(alpha = 0.82f),
                        modifier = Modifier
                            .size(18.dp)
                            .rotate(90f)
                    )
                }

                val last4 = account.accountNumberLast4.takeLast(4).ifBlank { "1947" }
                val maskedDigits = when (account.type) {
                    AccountType.MFS -> "••••  $last4"
                    AccountType.CASH -> strings.typeCash
                    else -> "••••  ••••  ••••  $last4"
                }
                Text(
                    text = maskedDigits,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 1.2.sp
                    ),
                    color = Color.White.copy(alpha = 0.94f)
                )
            }

            // Bottom Row: Bold Balance & Optional Credit Card Utilization Bar
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text = if (account.type == AccountType.CREDIT_CARD) {
                                strings.outstandingBalanceLabel
                            } else {
                                strings.availableBalance
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.82f)
                        )
                        Text(
                            text = BdtFormatter.format(account.balance, language),
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontFamily = JetBrainsMonoFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = if (compact) 23.sp else 26.sp
                            ),
                            color = Color.White
                        )
                    }

                    if (account.type == AccountType.CREDIT_CARD && account.creditLimit > 0.0) {
                        val util = account.utilizationPercent
                        val statusText = when {
                            util <= 30 -> strings.statusHealthy
                            util <= 70 -> strings.statusModerate
                            else -> strings.statusHigh
                        }
                        val statusTint = when {
                            util <= 30 -> Color(0xFF6EE7B7)
                            util <= 70 -> Color(0xFFFDE68A)
                            else -> Color(0xFFFDA4AF)
                        }
                        Surface(
                            color = Color.Black.copy(alpha = 0.30f),
                            shape = RoundedCornerShape(99.dp)
                        ) {
                            Text(
                                text = "${BdtFormatter.formatPercent(util, language)} • $statusText",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = statusTint,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                if (account.type == AccountType.CREDIT_CARD && account.creditLimit > 0.0) {
                    Text(
                        text = "${strings.creditLimitShort}: ${BdtFormatter.format(account.creditLimit, language)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.84f)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmvCardChip(subtle: Boolean = false) {
    Box(
        modifier = Modifier
            .size(width = 38.dp, height = 27.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(
                if (subtle) {
                    Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.36f),
                            Color.White.copy(alpha = 0.16f)
                        )
                    )
                } else {
                    Brush.linearGradient(
                        colors = listOf(Color(0xFFFDE68A), Color(0xFFD97706))
                    )
                }
            )
            .border(0.9.dp, Color.White.copy(alpha = 0.58f), RoundedCornerShape(7.dp))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val lineColor = if (subtle) {
                Color.White.copy(alpha = 0.40f)
            } else {
                Color(0xFF78350F).copy(alpha = 0.45f)
            }
            drawLine(lineColor, Offset(size.width * 0.35f, 0f), Offset(size.width * 0.35f, size.height), 1.4f)
            drawLine(lineColor, Offset(size.width * 0.65f, 0f), Offset(size.width * 0.65f, size.height), 1.4f)
            drawLine(lineColor, Offset(0f, size.height * 0.5f), Offset(size.width * 0.35f, size.height * 0.5f), 1.4f)
            drawLine(lineColor, Offset(size.width * 0.65f, size.height * 0.5f), Offset(size.width, size.height * 0.5f), 1.4f)
        }
    }
}

@Composable
fun CardNetworkOrProviderBadge(account: AccountEntity) {
    val networkUpper = account.cardNetwork.uppercase()
    when {
        networkUpper.contains("MASTERCARD") -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Canvas(modifier = Modifier.size(width = 34.dp, height = 20.dp)) {
                    drawCircle(
                        color = Color(0xFFEF4444).copy(alpha = 0.95f),
                        radius = size.height * 0.46f,
                        center = Offset(size.width * 0.34f, size.height * 0.5f)
                    )
                    drawCircle(
                        color = Color(0xFFF59E0B).copy(alpha = 0.90f),
                        radius = size.height * 0.46f,
                        center = Offset(size.width * 0.66f, size.height * 0.5f)
                    )
                }
            }
        }

        account.cardNetwork.isNotBlank() -> {
            Surface(
                color = Color.White.copy(alpha = 0.22f),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(0.7.dp, Color.White.copy(alpha = 0.35f))
            ) {
                Text(
                    text = if (networkUpper.contains("AMERICAN")) "AMEX" else networkUpper,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.9.sp
                    ),
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        else -> {
            Surface(
                color = Color.White.copy(alpha = 0.20f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = account.bankOrProvider.take(6).uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
fun CategoryIconBadge(
    category: ExpenseCategory,
    modifier: Modifier = Modifier,
    size: Dp = 42.dp
) {
    val icon: ImageVector = when (category) {
        ExpenseCategory.FOOD_DINING -> Icons.Filled.Restaurant
        ExpenseCategory.GROCERIES -> Icons.Filled.LocalGroceryStore
        ExpenseCategory.RENT_UTILITIES -> Icons.Filled.Home
        ExpenseCategory.TRANSPORT -> Icons.Filled.DirectionsCar
        ExpenseCategory.SHOPPING -> Icons.Filled.ShoppingBag
        ExpenseCategory.HEALTH -> Icons.Filled.MedicalServices
        ExpenseCategory.ENTERTAINMENT -> Icons.Filled.Movie
        ExpenseCategory.SALARY_INCOME -> Icons.Filled.TrendingUp
        ExpenseCategory.TRANSFER -> Icons.AutoMirrored.Filled.CompareArrows
        ExpenseCategory.OTHER -> Icons.Filled.AccountBalanceWallet
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(14.dp))
            .background(category.color.copy(alpha = 0.16f))
            .border(1.dp, category.color.copy(alpha = 0.30f), RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = category.en,
            tint = category.color,
            modifier = Modifier.size(size * 0.52f)
        )
    }
}

@Composable
fun FloatingCrystalBottomBar(
    selectedTab: AppTab,
    strings: AppStrings,
    onSelectTab: (AppTab) -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val crystal = LocalCrystalColors.current
    val purpleFabLight = Color(0xFF9333EA)
    val purpleFabDeep = Color(0xFF6D28D9)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Row(
            modifier = Modifier
                .widthIn(max = 580.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Left Frosted-Glass Pill Bar containing the 4 tabs
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .shadow(
                        elevation = 18.dp,
                        shape = RoundedCornerShape(28.dp),
                        ambientColor = if (crystal.isDark) Color.Black.copy(alpha = 0.55f) else SkyCyanPrimary.copy(alpha = 0.20f),
                        spotColor = if (crystal.isDark) Color.Black.copy(alpha = 0.60f) else SkyCyanPrimary.copy(alpha = 0.24f)
                    ),
                shape = RoundedCornerShape(28.dp),
                color = crystal.navBarBackground,
                border = BorderStroke(1.dp, crystal.glassBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NavTabItem(
                        label = strings.navHome,
                        selected = selectedTab == AppTab.HOME,
                        selectedIcon = Icons.Filled.Home,
                        unselectedIcon = Icons.Outlined.Home,
                        testTag = "nav_tab_home",
                        onClick = { onSelectTab(AppTab.HOME) }
                    )
                    NavTabItem(
                        label = strings.navActivity,
                        selected = selectedTab == AppTab.ACTIVITY,
                        selectedIcon = Icons.Filled.ReceiptLong,
                        unselectedIcon = Icons.Outlined.ReceiptLong,
                        testTag = "nav_tab_activity",
                        onClick = { onSelectTab(AppTab.ACTIVITY) }
                    )
                    NavTabItem(
                        label = strings.navPlan,
                        selected = selectedTab == AppTab.PLAN,
                        selectedIcon = Icons.Filled.EventNote,
                        unselectedIcon = Icons.Outlined.EventNote,
                        testTag = "nav_tab_plan",
                        onClick = { onSelectTab(AppTab.PLAN) }
                    )
                    NavTabItem(
                        label = strings.navInsights,
                        selected = selectedTab == AppTab.INSIGHTS,
                        selectedIcon = Icons.Filled.Analytics,
                        unselectedIcon = Icons.Outlined.Analytics,
                        testTag = "nav_tab_insights",
                        onClick = { onSelectTab(AppTab.INSIGHTS) }
                    )
                }
            }

            // Right Circular Purple Floating + Action Button
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .shadow(
                        elevation = 14.dp,
                        shape = CircleShape,
                        ambientColor = purpleFabLight,
                        spotColor = purpleFabDeep
                    )
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(purpleFabLight, purpleFabDeep)
                        )
                    )
                    .border(1.5.dp, Color.White.copy(alpha = 0.50f), CircleShape)
                    .clickable(onClick = onAddClick)
                    .testTag("fab_add_transaction"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = strings.addTransactionTitle,
                    tint = Color.White,
                    modifier = Modifier.size(30.dp)
                )
            }
        }
    }
}

@Composable
private fun NavTabItem(
    label: String,
    selected: Boolean,
    selectedIcon: ImageVector,
    unselectedIcon: ImageVector,
    testTag: String,
    onClick: () -> Unit
) {
    val pillWidth by animateDpAsState(
        targetValue = if (selected) 56.dp else 0.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "navPillWidth"
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else LocalCrystalColors.current.mutedText,
        label = "navItemColor"
    )

    Column(
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 4.dp)
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .width(if (selected) pillWidth else 40.dp)
                .height(30.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(
                    if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                    else Color.Transparent
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (selected) selectedIcon else unselectedIcon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            ),
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Modern minimalist financial emblem combining a geometric Wallet/Ledger with the
 * Bangladeshi Taka symbol (৳) inside a glossy Sky Cyan & Royal Blue squircle badge.
 */
@Composable
fun AmarHisabLogoBadge(
    modifier: Modifier = Modifier,
    size: Dp = 116.dp
) {
    val cornerRadius = size * 0.28f
    val shape = RoundedCornerShape(cornerRadius)
    val takaFontSize = (size.value * 0.27f).sp

    Box(
        modifier = modifier
            .size(size)
            .shadow(
                elevation = 24.dp,
                shape = shape,
                ambientColor = Color(0xFF0284C7).copy(alpha = 0.44f),
                spotColor = Color(0xFF1D4ED8).copy(alpha = 0.48f)
            )
            .clip(shape)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF38BDF8), // Sky Cyan Highlight
                        Color(0xFF0284C7), // Sky Cyan Primary
                        Color(0xFF1D4ED8), // Royal Blue
                        Color(0xFF1E3A8A)  // Deep Royal Indigo
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                )
            )
            .border(
                width = 1.6.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.85f),
                        Color.White.copy(alpha = 0.20f)
                    )
                ),
                shape = shape
            ),
        contentAlignment = Alignment.Center
    ) {
        // Glossy specular sheen & geometric Wallet/Ledger vector paths
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height

            // Soft top-left crystal sheen
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.34f), Color.Transparent),
                    center = Offset(w * 0.24f, h * 0.16f),
                    radius = w * 0.62f
                ),
                center = Offset(w * 0.24f, h * 0.16f),
                radius = w * 0.62f
            )

            // Subtle bottom-right glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF67E8F9).copy(alpha = 0.22f), Color.Transparent),
                    center = Offset(w * 0.88f, h * 0.88f),
                    radius = w * 0.50f
                ),
                center = Offset(w * 0.88f, h * 0.88f),
                radius = w * 0.50f
            )

            // Top ledger bookmark / page fold silhouette behind the main wallet body
            val flapPath = Path().apply {
                moveTo(w * 0.26f, h * 0.33f)
                lineTo(w * 0.64f, h * 0.22f)
                quadraticTo(w * 0.72f, h * 0.20f, w * 0.75f, h * 0.27f)
                lineTo(w * 0.78f, h * 0.34f)
                close()
            }
            drawPath(
                path = flapPath,
                color = Color.White.copy(alpha = 0.28f)
            )

            // Main frosted-white Wallet / Ledger body
            val walletLeft = w * 0.20f
            val walletTop = h * 0.31f
            val walletW = w * 0.60f
            val walletH = h * 0.45f
            val walletRadius = w * 0.095f

            drawRoundRect(
                color = Color.White.copy(alpha = 0.18f),
                topLeft = Offset(walletLeft, walletTop),
                size = Size(walletW, walletH),
                cornerRadius = CornerRadius(walletRadius, walletRadius)
            )
            drawRoundRect(
                color = Color.White.copy(alpha = 0.96f),
                topLeft = Offset(walletLeft, walletTop),
                size = Size(walletW, walletH),
                cornerRadius = CornerRadius(walletRadius, walletRadius),
                style = Stroke(
                    width = w * 0.044f,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )

            // Ledger spine / top fold rule
            drawLine(
                color = Color.White.copy(alpha = 0.55f),
                start = Offset(walletLeft + w * 0.06f, walletTop + h * 0.09f),
                end = Offset(walletLeft + walletW - w * 0.14f, walletTop + h * 0.09f),
                strokeWidth = w * 0.022f,
                cap = StrokeCap.Round
            )

            // Right-edge minimalist wallet clasp tab
            val claspW = w * 0.14f
            val claspH = h * 0.14f
            drawRoundRect(
                color = Color.White,
                topLeft = Offset(walletLeft + walletW - claspW * 0.65f, walletTop + (walletH - claspH) * 0.5f),
                size = Size(claspW, claspH),
                cornerRadius = CornerRadius(claspH * 0.42f, claspH * 0.42f)
            )
            drawCircle(
                color = Color(0xFF1D4ED8),
                radius = w * 0.022f,
                center = Offset(walletLeft + walletW - claspW * 0.08f, walletTop + walletH * 0.5f)
            )
        }

        // Crisp Taka (৳) glyph centered inside the wallet/ledger emblem
        Text(
            text = "৳",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontFamily = HindSiliguriFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = takaFontSize
            ),
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = size * 0.08f, end = size * 0.05f)
        )
    }
}

/**
 * Multi-color Google 'G' vector emblem drawn cleanly on Canvas.
 */
@Composable
fun GoogleGLogo(
    modifier: Modifier = Modifier,
    size: Dp = 20.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val strokeWidth = this.size.minDimension * 0.20f
        val inset = strokeWidth / 2f
        val arcSize = Size(this.size.width - strokeWidth, this.size.height - strokeWidth)
        val topLeft = Offset(inset, inset)

        // Red arc (top)
        drawArc(
            color = Color(0xFFEA4335),
            startAngle = -145f,
            sweepAngle = 100f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
        )
        // Yellow arc (left)
        drawArc(
            color = Color(0xFFFBBC05),
            startAngle = 145f,
            sweepAngle = 70f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
        )
        // Green arc (bottom)
        drawArc(
            color = Color(0xFF34A853),
            startAngle = 45f,
            sweepAngle = 100f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
        )
        // Blue arc (right + bottom-right)
        drawArc(
            color = Color(0xFF4285F4),
            startAngle = -5f,
            sweepAngle = 52f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
        )
        // Horizontal crossbar of 'G'
        drawLine(
            color = Color(0xFF4285F4),
            start = Offset(this.size.width * 0.50f, this.size.height * 0.50f),
            end = Offset(this.size.width - inset, this.size.height * 0.50f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Butt
        )
    }
}

data class PresetAvatarOption(
    val id: String,
    val labelEn: String,
    val labelBn: String,
    val gradient: List<Color>,
    val icon: ImageVector? = null,
    val glyphText: String? = null
)

object BuiltInPresetAvatars {
    val all: List<PresetAvatarOption> = listOf(
        PresetAvatarOption(
            id = "preset:sky_vault",
            labelEn = "Sky Vault",
            labelBn = "স্কাই ভল্ট",
            gradient = listOf(Color(0xFF06B6D4), Color(0xFF1D4ED8)),
            icon = Icons.Filled.AccountBalanceWallet
        ),
        PresetAvatarOption(
            id = "preset:taka_emblem",
            labelEn = "Taka Crest",
            labelBn = "টাকা প্রতীক",
            gradient = listOf(Color(0xFF10B981), Color(0xFF0E7490)),
            glyphText = "৳"
        ),
        PresetAvatarOption(
            id = "preset:emerald_saver",
            labelEn = "Saver",
            labelBn = "সেভার",
            gradient = listOf(Color(0xFF059669), Color(0xFF047857)),
            icon = Icons.Filled.Savings
        ),
        PresetAvatarOption(
            id = "preset:golden_crown",
            labelEn = "Premier",
            labelBn = "প্রিমিয়ার",
            gradient = listOf(Color(0xFFF59E0B), Color(0xFFEA580C)),
            icon = Icons.Filled.WorkspacePremium
        ),
        PresetAvatarOption(
            id = "preset:orchid_aura",
            labelEn = "Aura",
            labelBn = "অরা",
            gradient = listOf(Color(0xFFEC4899), Color(0xFF7C3AED)),
            icon = Icons.Filled.AutoAwesome
        ),
        PresetAvatarOption(
            id = "preset:vault_shield",
            labelEn = "Guardian",
            labelBn = "গার্ডিয়ান",
            gradient = listOf(Color(0xFF3B82F6), Color(0xFF1E293B)),
            icon = Icons.Filled.Shield
        ),
        PresetAvatarOption(
            id = "preset:growth_wave",
            labelEn = "Growth",
            labelBn = "প্রবৃদ্ধি",
            gradient = listOf(Color(0xFF0EA5E9), Color(0xFF0369A1)),
            icon = Icons.Filled.TrendingUp
        ),
        PresetAvatarOption(
            id = "preset:crimson_spark",
            labelEn = "Crimson",
            labelBn = "ক্রিমসন",
            gradient = listOf(Color(0xFFF43F5E), Color(0xFFBE123C)),
            icon = Icons.Filled.Favorite
        )
    )

    fun find(id: String): PresetAvatarOption? = all.firstOrNull { it.id == id }
}

/**
 * Shared profile avatar supporting linked Google photo URL, local photo URI,
 * built-in curated preset emblems, Google initials, or local fallback avatar.
 */
@Composable
fun UserProfileAvatar(
    userName: String,
    avatarUrl: String,
    authMode: AuthMode,
    size: Dp,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    val presetOption = if (avatarUrl.startsWith("preset:")) {
        BuiltInPresetAvatars.find(avatarUrl) ?: BuiltInPresetAvatars.all.first()
    } else null

    val borderColor = if (authMode == AuthMode.GOOGLE) {
        Color(0xFF2563EB).copy(alpha = 0.75f)
    } else {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
    }

    val backgroundBrush = if (presetOption != null) {
        Brush.linearGradient(presetOption.gradient)
    } else {
        Brush.linearGradient(listOf(Color(0xFF38BDF8), Color(0xFF1D4ED8)))
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .border(2.dp, borderColor, CircleShape)
            .background(backgroundBrush),
        contentAlignment = Alignment.Center
    ) {
        if (presetOption != null) {
            if (presetOption.glyphText != null) {
                Text(
                    text = presetOption.glyphText,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = (size.value * 0.46f).sp
                    ),
                    color = Color.White
                )
            } else if (presetOption.icon != null) {
                Icon(
                    imageVector = presetOption.icon,
                    contentDescription = contentDescription,
                    tint = Color.White,
                    modifier = Modifier.size(size * 0.48f)
                )
            }
        } else if (avatarUrl.isNotBlank()) {
            Icon(
                imageVector = Icons.Filled.Person,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.90f),
                modifier = Modifier.size(size * 0.52f)
            )
            AsyncImage(
                model = avatarUrl,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else if (authMode == AuthMode.GOOGLE && userName.isNotBlank()) {
            val initials = userName
                .trim()
                .split(Regex("\\s+"))
                .take(2)
                .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                .joinToString("")
                .ifBlank { "G" }
            Text(
                text = initials,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = (size.value * 0.36f).sp
                ),
                color = Color.White
            )
        } else {
            Icon(
                imageVector = Icons.Filled.Person,
                contentDescription = contentDescription,
                tint = Color.White,
                modifier = Modifier
                    .size(size * 0.54f)
                    .testTag("default_guest_avatar_icon")
            )
        }
    }
}

/**
 * Smooth preview Google Account chooser dialog used when the device/emulator lacks
 * a signed-in Google account in Credential Manager, allowing full end-to-end testing
 * of setting profile name, email, and avatar while preserving Credential Manager flow.
 */
@Composable
fun GooglePreviewAccountChooserDialog(
    strings: AppStrings,
    onSelectAccount: (GoogleAccountProfile) -> Unit,
    onDismiss: () -> Unit
) {
    val crystal = LocalCrystalColors.current
    var showCustomForm by remember { mutableStateOf(false) }
    var customName by remember { mutableStateOf("") }
    var customEmail by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurface,
        icon = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GoogleGLogo(size = 24.dp)
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                    shape = CircleShape
                ) {
                    Text(
                        text = strings.googlePreviewBadge,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        },
        title = {
            Text(
                text = strings.googleChooserTitle,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = strings.googleChooserSubtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = crystal.mutedText
                )

                GoogleAuthManager.previewAccounts.forEach { acc ->
                    Surface(
                        color = if (crystal.isDark) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, crystal.glassBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onSelectAccount(acc) }
                            .testTag("preview_google_account_${acc.email}")
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            UserProfileAvatar(
                                userName = acc.displayName,
                                avatarUrl = acc.avatarUrl,
                                authMode = AuthMode.GOOGLE,
                                size = 40.dp,
                                contentDescription = acc.displayName
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = acc.displayName,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = acc.email,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = crystal.mutedText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = crystal.glassBorder)

                if (!showCustomForm) {
                    TextButton(
                        onClick = { showCustomForm = true },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.useCustomAccountBtn)
                    }
                } else {
                    OutlinedTextField(
                        value = customName,
                        onValueChange = { customName = it },
                        label = { Text(strings.customGoogleNameLabel) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = customEmail,
                        onValueChange = { customEmail = it },
                        label = { Text(strings.customGoogleEmailLabel) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            val cleanEmail = customEmail.trim().ifBlank { "user@gmail.com" }
                            val cleanName = customName.trim().ifBlank { cleanEmail.substringBefore("@") }
                            onSelectAccount(
                                GoogleAccountProfile(
                                    displayName = cleanName,
                                    email = cleanEmail,
                                    avatarUrl = "",
                                    isPreviewSession = true
                                )
                            )
                        },
                        enabled = customName.isNotBlank() || customEmail.contains("@"),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(strings.useCustomAccountBtn)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel)
            }
        }
    )
}

