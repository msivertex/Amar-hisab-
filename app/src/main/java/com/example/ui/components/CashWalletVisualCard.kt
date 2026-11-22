package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.AccountEntity
import com.example.data.model.AppCurrency
import com.example.localization.AppLanguage
import com.example.localization.AppStrings
import com.example.localization.BdtFormatter
import com.example.ui.theme.LocalCrystalColors
import com.example.ui.theme.NumericCardBalanceStyle
import com.example.ui.theme.NumericCompactCardBalanceStyle
import com.example.ui.theme.NumericPeekingBadgeStyle

/**
 * Dedicated Crystal Liquid visual card exclusively for Cash / নগদ টাকা (`AccountType.CASH`).
 * Displays banknote (নোট) and coin (কয়েন) graphics with zero bank-card elements
 * (no EMV chip, no contactless waves, no 16-digit card number formatting).
 */
@Composable
fun CashWalletVisualCard(
    account: AccountEntity,
    language: AppLanguage,
    strings: AppStrings,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    isPeekingBackground: Boolean = false,
    elevation: Dp = 18.dp,
    onClick: (() -> Unit)? = null
) {
    val crystal = LocalCrystalColors.current
    val shape = RoundedCornerShape(24.dp)
    val accountCurrency = AppCurrency.fromCode(account.currencyCode)
    val cardTypeLabel = strings.dashboard.cardTypeCash
    val shadowTint = Color(0xFF059669)
    val cashInHandLabel = if (language == AppLanguage.BN) "নগদ নোট ও কয়েন" else "Banknotes & Coins"

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
                ambientColor = shadowTint.copy(alpha = if (crystal.isDark) 0.36f else 0.44f),
                spotColor = shadowTint.copy(alpha = if (crystal.isDark) 0.42f else 0.50f)
            )
            .clip(shape)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFF0D7C5F), Color(0xFF065F46), Color(0xFF043B2D)),
                    start = Offset(0f, 0f),
                    end = Offset(980f, 700f)
                )
            )
            .border(
                width = 1.3.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = if (crystal.isDark) 0.52f else 0.72f),
                        Color(0xFFFDE68A).copy(alpha = 0.34f),
                        Color.White.copy(alpha = 0.18f)
                    )
                ),
                shape = shape
            )
            .let { m -> if (onClick != null) m.clickable(onClick = onClick) else m }
            .testTag("cash_wallet_visual_card_${account.id}")
    ) {
        // 1. High-polish banknote & gold coin artwork anchored cleanly to the right
        Image(
            painter = painterResource(id = R.drawable.img_cash_banknote_coins),
            contentDescription = cashInHandLabel,
            contentScale = ContentScale.Crop,
            alignment = Alignment.CenterEnd,
            alpha = if (isPeekingBackground) 0.65f else 0.96f,
            modifier = Modifier.fillMaxSize()
        )

        // 2. Smooth Crystal Liquid emerald frosted scrim on the left so all balances & titles stay ultra-crisp
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.horizontalGradient(
                        0.0f to Color(0xFF043B2D).copy(alpha = 0.90f),
                        0.46f to Color(0xFF065F46).copy(alpha = 0.78f),
                        0.72f to Color(0xFF065F46).copy(alpha = 0.28f),
                        1.0f to Color(0xFF022C22).copy(alpha = 0.14f)
                    )
                )
        )

        // 3. Subtle Crystal Liquid top diagonal sheen & bottom readability vignette
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        0.0f to Color.White.copy(alpha = 0.16f),
                        0.32f to Color.Transparent,
                        0.76f to Color.Black.copy(alpha = 0.14f),
                        1.0f to Color.Black.copy(alpha = 0.36f)
                    )
                )
        )

        // 4. Card Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = if (isPeekingBackground) 18.dp else if (compact) 20.dp else 22.dp,
                    vertical = if (isPeekingBackground) 10.dp else if (compact) 15.dp else 19.dp
                ),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Row: Banknote & Coin Emblem + Cash Name + Currency Pill (or Peeking Balance)
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
                        color = Color.White.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(0.9.dp, Color(0xFFFDE68A).copy(alpha = 0.55f))
                    ) {
                        CashBanknoteCoinEmblem(
                            modifier = Modifier
                                .padding(if (isPeekingBackground) 6.dp else 7.dp)
                                .size(if (isPeekingBackground) 20.dp else 24.dp),
                            contentDesc = cashInHandLabel
                        )
                    }
                    Column {
                        Text(
                            text = account.name.ifBlank { cardTypeLabel },
                            style = (if (isPeekingBackground) {
                                MaterialTheme.typography.titleSmall
                            } else {
                                MaterialTheme.typography.titleMedium
                            }).copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val subLabel = if (account.accountSubtype.isNotBlank()) {
                            "$cardTypeLabel • ${account.accountSubtype}"
                        } else {
                            cashInHandLabel
                        }
                        Text(
                            text = subLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFD1FAE5).copy(alpha = 0.92f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))
                if (isPeekingBackground) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.34f),
                        shape = CircleShape,
                        border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.34f))
                    ) {
                        Text(
                            text = BdtFormatter.formatCurrency(account.balance, account.currencyCode, language),
                            style = NumericPeekingBadgeStyle,
                            color = Color.White,
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                } else {
                    Surface(
                        color = Color(0xFF022C22).copy(alpha = 0.62f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(0.9.dp, Color(0xFFFDE68A).copy(alpha = 0.50f))
                    ) {
                        Text(
                            text = accountCurrency.cleanBadgeLabel(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            ),
                            color = Color(0xFFFEF3C7),
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Middle Row: Tactile Banknote + Gold Coins Graphic Badge (NO EMV chip, NO card digits)
            if (!isPeekingBackground) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    Surface(
                        color = Color(0xFF022C22).copy(alpha = 0.44f),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(0.9.dp, Color.White.copy(alpha = 0.28f)),
                        modifier = Modifier.testTag("cash_banknote_coin_graphic_${account.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CashBanknoteCoinEmblem(
                                modifier = Modifier.size(width = 34.dp, height = 22.dp),
                                contentDesc = cashInHandLabel
                            )
                            Text(
                                text = cardTypeLabel,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 0.4.sp
                                ),
                                color = Color.White.copy(alpha = 0.96f),
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // Bottom Row: Crystal Frosted Balance Display
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = strings.availableBalance,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                    color = Color(0xFFD1FAE5).copy(alpha = 0.86f)
                )
                Text(
                    text = BdtFormatter.formatCurrency(account.balance, account.currencyCode, language),
                    style = NumericCardBalanceStyle.copy(
                        fontSize = if (compact) 23.5.sp else 26.5.sp
                    ),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Compact horizontal-strip Cash card used on the Home dashboard when Compact Cards layout is active.
 */
@Composable
fun CashWalletCompactCard(
    account: AccountEntity,
    cardWidth: Dp,
    language: AppLanguage,
    strings: AppStrings,
    onClick: () -> Unit
) {
    val crystal = LocalCrystalColors.current
    val cardShape = RoundedCornerShape(22.dp)
    val accCur = AppCurrency.fromCode(account.currencyCode)
    val cardTypeLabel = strings.dashboard.cardTypeCash
    val cashInHandLabel = if (language == AppLanguage.BN) "নগদ নোট ও কয়েন" else "Banknotes & Coins"

    Box(
        modifier = Modifier
            .width(cardWidth)
            .height(166.dp)
            .shadow(
                elevation = 12.dp,
                shape = cardShape,
                ambientColor = Color(0xFF059669).copy(alpha = 0.40f),
                spotColor = Color(0xFF059669).copy(alpha = 0.46f)
            )
            .clip(cardShape)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFF0D7C5F), Color(0xFF065F46), Color(0xFF043B2D)),
                    start = Offset(0f, 0f),
                    end = Offset(860f, 520f)
                )
            )
            .border(
                width = 1.25.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = if (crystal.isDark) 0.52f else 0.70f),
                        Color(0xFFFDE68A).copy(alpha = 0.34f),
                        Color.White.copy(alpha = 0.16f)
                    )
                ),
                shape = cardShape
            )
            .clickable(onClick = onClick)
            .testTag("compact_wallet_card_${account.id}")
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_cash_banknote_coins),
            contentDescription = cashInHandLabel,
            contentScale = ContentScale.Crop,
            alignment = Alignment.CenterEnd,
            alpha = 0.94f,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.horizontalGradient(
                        0.0f to Color(0xFF043B2D).copy(alpha = 0.90f),
                        0.50f to Color(0xFF065F46).copy(alpha = 0.76f),
                        0.76f to Color(0xFF065F46).copy(alpha = 0.25f),
                        1.0f to Color(0xFF022C22).copy(alpha = 0.14f)
                    )
                )
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
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
                        color = Color.White.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(11.dp),
                        border = BorderStroke(0.8.dp, Color(0xFFFDE68A).copy(alpha = 0.55f))
                    ) {
                        CashBanknoteCoinEmblem(
                            modifier = Modifier
                                .padding(7.dp)
                                .size(22.dp),
                            contentDesc = cashInHandLabel
                        )
                    }
                    Column {
                        Text(
                            text = account.name.ifBlank { cardTypeLabel },
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = cashInHandLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFD1FAE5).copy(alpha = 0.92f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                    color = Color(0xFF022C22).copy(alpha = 0.62f),
                    shape = RoundedCornerShape(9.dp),
                    border = BorderStroke(0.8.dp, Color(0xFFFDE68A).copy(alpha = 0.50f))
                ) {
                    Text(
                        text = accCur.cleanBadgeLabel(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.5.sp
                        ),
                        color = Color(0xFFFEF3C7),
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = strings.availableBalance,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                    color = Color(0xFFD1FAE5).copy(alpha = 0.86f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = BdtFormatter.formatCurrency(account.balance, account.currencyCode, language),
                    style = NumericCompactCardBalanceStyle,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Crisp custom vector illustration combining a layered paper banknote (নোট) with
 * an overlapping gleaming golden metallic coin (কয়েন).
 */
@Composable
fun CashBanknoteCoinEmblem(
    modifier: Modifier = Modifier,
    contentDesc: String? = null
) {
    val semanticsModifier = if (contentDesc != null) {
        modifier.semantics { contentDescription = contentDesc }
    } else {
        modifier
    }

    Canvas(modifier = semanticsModifier) {
        val w = size.width
        val h = size.height

        // Back banknote layer (slightly raised for stacked bill depth)
        val backNoteWidth = w * 0.78f
        val backNoteHeight = h * 0.56f
        drawRoundRect(
            color = Color(0xFFA7F3D0).copy(alpha = 0.55f),
            topLeft = Offset(w * 0.08f, h * 0.06f),
            size = Size(backNoteWidth, backNoteHeight),
            cornerRadius = CornerRadius(h * 0.14f, h * 0.14f)
        )

        // Front paper banknote body
        val noteTop = h * 0.20f
        val noteLeft = w * 0.02f
        val noteWidth = w * 0.80f
        val noteHeight = h * 0.60f
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFFECFDF5), Color(0xFF6EE7B7)),
                start = Offset(noteLeft, noteTop),
                end = Offset(noteLeft + noteWidth, noteTop + noteHeight)
            ),
            topLeft = Offset(noteLeft, noteTop),
            size = Size(noteWidth, noteHeight),
            cornerRadius = CornerRadius(h * 0.14f, h * 0.14f)
        )

        // Ornamental inner security border on the banknote
        val inset = h * 0.08f
        drawRoundRect(
            color = Color(0xFF047857).copy(alpha = 0.55f),
            topLeft = Offset(noteLeft + inset, noteTop + inset),
            size = Size(noteWidth - inset * 2f, noteHeight - inset * 2f),
            cornerRadius = CornerRadius(h * 0.08f, h * 0.08f),
            style = Stroke(width = (h * 0.045f).coerceAtLeast(1f))
        )

        // Center oval medallion on the banknote
        drawCircle(
            color = Color(0xFF059669).copy(alpha = 0.85f),
            radius = noteHeight * 0.24f,
            center = Offset(noteLeft + noteWidth * 0.44f, noteTop + noteHeight * 0.50f)
        )

        // Gleaming metallic gold coin overlapping bottom-right of the banknote
        val coinRadius = h * 0.29f
        val coinCenter = Offset(w * 0.77f, h * 0.68f)
        // Subtle shadow behind coin
        drawCircle(
            color = Color(0xFF022C22).copy(alpha = 0.35f),
            radius = coinRadius * 1.08f,
            center = Offset(coinCenter.x, coinCenter.y + h * 0.04f)
        )
        // Outer gold coin body
        drawCircle(
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFFFEF08A), Color(0xFFF59E0B), Color(0xFFB45309)),
                start = Offset(coinCenter.x - coinRadius, coinCenter.y - coinRadius),
                end = Offset(coinCenter.x + coinRadius, coinCenter.y + coinRadius)
            ),
            radius = coinRadius,
            center = coinCenter
        )
        // Inner embossed coin rim
        drawCircle(
            color = Color(0xFFFEF9C3).copy(alpha = 0.85f),
            radius = coinRadius * 0.72f,
            center = coinCenter,
            style = Stroke(width = (h * 0.04f).coerceAtLeast(1f))
        )
    }
}
