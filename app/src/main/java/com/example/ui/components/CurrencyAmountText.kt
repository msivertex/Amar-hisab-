package com.example.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp
import com.example.data.model.AccountBalanceGuard
import com.example.data.model.AppCurrency
import com.example.localization.AppLanguage
import com.example.localization.BdtFormatter
import com.example.ui.theme.LocalCrystalColors
import kotlin.math.abs

/**
 * Currency display policy helper.
 *
 * Across Amar Hisab, all currency amounts render in clean ISO code-only format
 * (`BDT 50,000`, `KWD 244.000`, `INR 25,000`, `LKR 10,000`) without currency symbols.
 */
object CurrencyGlyphSupport {
    private val rtlSymbolRegex = Regex("[\\u0600-\\u06FF\\u0750-\\u077F\\u08A0-\\u08FF\\uFB50-\\uFDFF\\uFE70-\\uFEFF]")

    fun containsArabicOrRtlScript(value: String?): Boolean =
        !value.isNullOrEmpty() && rtlSymbolRegex.containsMatchIn(value)

    fun canRenderSymbol(symbol: String): Boolean = false

    /**
     * Always returns null because currency display is standardized globally to clean
     * 3-letter ISO codes without symbols (`CODE AMOUNT`).
     */
    fun resolveDisplaySymbol(
        currency: AppCurrency,
        arabicSymbolSupportedOverride: Boolean? = null
    ): String? = null
}

/**
 * Unified, auto-fitting, symbol-free currency display composable for Home summaries, balances,
 * wallet cards, and transaction figures.
 *
 * - Code-only format: renders `CODE AMOUNT` (e.g. `BDT 50,000`, `KWD 244.000`, `BDT ৫০,০০০`,
 *   `KWD ২৪৪.০০০`) or `AMOUNT` when placed directly beside a leading ISO code badge pill.
 * - BDT: uses a slightly reduced font size & tighter spacing in regular black ink (`crystal.bdtNumericInk`)
 *   and auto-fits cleanly so full numbers display without `...` truncation.
 * - KWD: uses rich dark blue ink (`crystal.kwdNumericInk`) with fixed 3-decimal formatting.
 */
@Composable
fun CurrencyAmountText(
    amount: Double,
    currencyCode: String,
    language: AppLanguage,
    style: TextStyle,
    modifier: Modifier = Modifier,
    includeIsoCode: Boolean = true,
    isBalance: Boolean = true,
    prefix: String = "",
    colorOverride: Color? = null,
    bdtFontScale: Float = 0.85f,
    minFontSize: TextUnit = 10.5.sp,
    @Suppress("UNUSED_PARAMETER") arabicSymbolSupportedOverride: Boolean? = null
) {
    val crystal = LocalCrystalColors.current
    val currency = remember(currencyCode) { AppCurrency.fromCode(currencyCode) }
    val isBdt = currency.code.equals("BDT", ignoreCase = true)
    val isKwd = currency.code.equals("KWD", ignoreCase = true)

    val safeAmount = if (isBalance) {
        AccountBalanceGuard.clampNonNegative(amount)
    } else {
        abs(if (amount.isFinite()) amount else 0.0)
    }
    val numericPart = remember(safeAmount, currency.code, language) {
        BdtFormatter.formatAmountOnly(safeAmount, currency.code, language, includeSign = false)
    }

    val displayText = remember(prefix, includeIsoCode, currency.code, numericPart) {
        buildString {
            append(prefix)
            if (includeIsoCode) {
                append(currency.code)
                append(' ')
            }
            append(numericPart)
        }
    }

    val resolvedColor = colorOverride ?: crystal.amountColorForCurrency(currency.code)
    val baseSp = if (style.fontSize.isSpecified) style.fontSize.value else 16f
    val initialSp = when {
        isBdt -> (baseSp * bdtFontScale).coerceAtLeast(minFontSize.value)
        else -> baseSp.coerceAtLeast(minFontSize.value)
    }
    val tightLetterSpacing = when {
        isBdt -> (-0.55).sp
        isKwd -> (-0.35).sp
        style.letterSpacing.isSpecified -> style.letterSpacing
        else -> (-0.25).sp
    }

    var fittedSp by remember(displayText, initialSp) { mutableStateOf(initialSp) }
    val fittedStyle = style.copy(
        fontSize = fittedSp.sp,
        lineHeight = (fittedSp * 1.24f).sp,
        letterSpacing = tightLetterSpacing,
        textDirection = TextDirection.Ltr
    )

    Text(
        text = displayText,
        style = fittedStyle,
        color = resolvedColor,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Clip,
        onTextLayout = { result ->
            if (result.didOverflowWidth && fittedSp > minFontSize.value) {
                fittedSp = (fittedSp * 0.90f).coerceAtLeast(minFontSize.value)
            }
        },
        modifier = modifier
    )
}
