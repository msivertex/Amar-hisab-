package com.example.data.ai

import android.graphics.Bitmap
import android.util.Log
import com.example.data.model.AppCurrency
import com.example.data.model.BuiltInCategoriesCatalog
import com.example.data.model.ExpenseCategory
import com.example.data.model.ManagedCategoryItem
import com.example.data.model.ReceiptScanResult
import com.example.data.model.TransactionType
import com.example.localization.AppLanguage
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import kotlin.coroutines.resume

private const val TAG = "LocalReceiptParser"

/**
 * On-device ML Kit OCR + deterministic receipt/voucher text parser.
 * Focuses primarily on extracting the **Total Amount** and **Currency** (KWD/KD, BDT/৳, SAR,
 * EUR, AED, QAR, MYR, GBP, USD, INR), plus merchant name, direction (Money In vs Money Out),
 * wallet/bank provider, fee, TrxID, and date — ensuring Receipt Scan works reliably even when
 * cloud AI is unavailable or blocked.
 */
object LocalReceiptTextParser {

    private val totalLineKeywords = listOf(
        "grand total", "net total", "total payable", "total paid", "amount paid",
        "total amount", "net amount", "total due", "final total", "total",
        "amount", "paid", "payable", "sale", "sum", "সর্বমোট", "মোট টাকা", "মোট"
    )

    private val feeLineKeywords = listOf(
        "fee", "charge", "service charge", "vat", "tax", "commission", "ফি", "চার্জ"
    )

    private val ignoredAmountLineKeywords = listOf(
        "balance", "avail", "available", "remaining", "limit", "trx", "ref", "auth",
        "account", "card", "phone", "mobile", "tel", "invoice", "receipt", "order",
        "qty", "date", "time", "ব্যালেন্স"
    )

    /**
     * Runs on-device ML Kit Text Recognition on [bitmap] and parses the recognized text.
     * Returns null if OCR throws or produces no readable text.
     */
    suspend fun scanBitmapLocally(
        bitmap: Bitmap,
        defaultCurrencyCode: String = "BDT",
        managedCategories: List<ManagedCategoryItem> = BuiltInCategoriesCatalog.all,
        language: AppLanguage = AppLanguage.BN
    ): ReceiptScanResult? {
        val rawText = recognizeTextOnDevice(bitmap)
        if (rawText.isNullOrBlank()) return null
        return parseRawReceiptText(
            rawText = rawText,
            defaultCurrencyCode = defaultCurrencyCode,
            managedCategories = managedCategories,
            language = language
        )
    }

    private suspend fun recognizeTextOnDevice(bitmap: Bitmap): String? =
        suspendCancellableCoroutine { cont ->
            try {
                val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                val inputImage = InputImage.fromBitmap(bitmap, 0)
                recognizer.process(inputImage)
                    .addOnSuccessListener { visionText ->
                        runCatching { recognizer.close() }
                        if (cont.isActive) cont.resume(visionText.text)
                    }
                    .addOnFailureListener { e ->
                        runCatching { recognizer.close() }
                        Log.w(TAG, "On-device ML Kit OCR failed", e)
                        if (cont.isActive) cont.resume(null)
                    }
            } catch (e: Throwable) {
                Log.w(TAG, "ML Kit OCR unavailable in runtime environment", e)
                if (cont.isActive) cont.resume(null)
            }
        }

    /**
     * Pure deterministic receipt/voucher text parser (unit-testable on JVM without ML Kit native libs).
     * Returns null only when no positive numeric total amount can be extracted.
     */
    fun parseRawReceiptText(
        rawText: String,
        defaultCurrencyCode: String = "BDT",
        managedCategories: List<ManagedCategoryItem> = BuiltInCategoriesCatalog.all,
        language: AppLanguage = AppLanguage.BN
    ): ReceiptScanResult? {
        val asciiText = ReceiptVoucherMatcher.toAsciiDigits(rawText)
        val lines = asciiText.lines().map { it.trim() }.filter { it.isNotBlank() }
        if (lines.isEmpty()) return null

        val lowerAll = asciiText.lowercase(Locale.US)

        // 1. Detect Currency (highest priority after Total Amount)
        val currencyCode = detectCurrencyFromReceipt(rawText, lowerAll, defaultCurrencyCode)

        // 2. Detect Total Amount (prioritize explicit Total lines, then currency-tagged lines, then max plausible amount)
        val totalAmount = extractTotalAmount(lines) ?: return null

        // 3. Detect separate fee/charge if present
        val feeAmount = extractFeeAmount(lines, totalAmount)

        // 4. Detect provider (bKash, Nagad, Rocket, KFH, DBBL, Trust Bank, etc.)
        val providerHint = detectProvider(lowerAll)

        // 5. Detect transaction direction (Money In vs Money Out)
        val detectedKeyword = detectActionKeyword(lowerAll)
        val txType = ReceiptVoucherMatcher.classifyTransactionDirection(
            detectedKeyword = detectedKeyword,
            merchantName = lines.take(4).joinToString(" "),
            itemsSummary = asciiText.take(300)
        )

        // 6. Detect merchant / recipient header
        val merchant = extractMerchantName(lines, providerHint)

        // 7. Detect Reference / TrxID, Card Last-4, and Date
        val refId = extractReferenceId(asciiText)
        val last4 = extractCardLast4(asciiText)
        val dateLabel = extractDateLabel(asciiText) ?: "Today"
        val parsedTimestamp = ReceiptVoucherMatcher.parseReceiptTimestamp(dateLabel)

        val detectedCat = UnifiedCategoryDetector.detectCategory(
            text = "$merchant $rawText",
            type = txType,
            managedCategories = managedCategories,
            language = language
        )

        return ReceiptScanResult(
            merchantName = merchant,
            amountBdt = totalAmount,
            category = detectedCat.legacyCategory,
            dateLabel = dateLabel,
            itemsSummary = buildSummary(merchant, providerHint, refId),
            transactionType = txType,
            feeAmount = feeAmount,
            currencyCode = currencyCode,
            providerHint = providerHint,
            accountLast4Hint = last4,
            referenceId = refId,
            timestampMillis = parsedTimestamp,
            detectedDirectionKeyword = detectedKeyword,
            isLocalFallback = true,
            managedCategoryId = detectedCat.managedCategory.id,
            managedCategoryLabel = detectedCat.managedCategory.label(language)
        )
    }

    private fun detectCurrencyFromReceipt(
        rawText: String,
        lowerAll: String,
        defaultCurrencyCode: String
    ): String {
        return when {
            Regex("(?<![a-z])(kwd|kd)(?![a-z])").containsMatchIn(lowerAll) ||
                lowerAll.contains("kuwaiti dinar") || rawText.contains("د.ك") || rawText.contains("দিনার") -> "KWD"
            Regex("(?<![a-z])(sar|sr)(?![a-z])").containsMatchIn(lowerAll) ||
                lowerAll.contains("saudi riyal") || rawText.contains("ر.س") -> "SAR"
            Regex("(?<![a-z])(aed|dhs)(?![a-z])").containsMatchIn(lowerAll) ||
                lowerAll.contains("dirham") || rawText.contains("د.إ") -> "AED"
            Regex("(?<![a-z])(qar|qr)(?![a-z])").containsMatchIn(lowerAll) ||
                lowerAll.contains("qatari") -> "QAR"
            Regex("(?<![a-z])(myr|rm)(?![a-z])").containsMatchIn(lowerAll) ||
                lowerAll.contains("ringgit") -> "MYR"
            rawText.contains("€") || Regex("(?<![a-z])eur(?![a-z])").containsMatchIn(lowerAll) -> "EUR"
            rawText.contains("£") || Regex("(?<![a-z])gbp(?![a-z])").containsMatchIn(lowerAll) -> "GBP"
            rawText.contains("$") || Regex("(?<![a-z])usd(?![a-z])").containsMatchIn(lowerAll) -> "USD"
            rawText.contains("₹") || Regex("(?<![a-z])inr(?![a-z])").containsMatchIn(lowerAll) -> "INR"
            rawText.contains("৳") || Regex("(?<![a-z])(bdt|tk|taka)(?![a-z])").containsMatchIn(lowerAll) ||
                rawText.contains("টাকা") -> "BDT"
            else -> AppCurrency.fromCode(defaultCurrencyCode).code
        }
    }

    private fun extractTotalAmount(lines: List<String>): Double? {
        // Priority 1: Explicit total lines (bottom-most grand/net total wins)
        var bestTotalScore = -1
        var bestTotalValue: Double? = null

        lines.forEachIndexed { idx, line ->
            val lower = line.lowercase(Locale.US)
            if (ignoredAmountLineKeywords.any { lower.contains(it) } &&
                !totalLineKeywords.any { lower.contains(it) }
            ) {
                return@forEachIndexed
            }

            val numsOnLine = extractNumbersFromLine(line)
            val candidateValue = numsOnLine.lastOrNull()
                ?: lines.getOrNull(idx + 1)?.let { next ->
                    if (totalLineKeywords.any { lower.contains(it) }) extractNumbersFromLine(next).firstOrNull() else null
                }

            if (candidateValue != null && candidateValue > 0.0) {
                val score = when {
                    lower.contains("grand total") || lower.contains("net total") ||
                        lower.contains("total payable") || lower.contains("total paid") ||
                        lower.contains("সর্বমোট") -> 100 + idx
                    lower.contains("total") || lower.contains("মোট") -> 80 + idx
                    lower.contains("amount") || lower.contains("paid") || lower.contains("sale") -> 60 + idx
                    hasCurrencySymbolOrCode(line) && !feeLineKeywords.any { lower.contains(it) } -> 45 + idx
                    else -> -1
                }
                if (score > bestTotalScore) {
                    bestTotalScore = score
                    bestTotalValue = candidateValue
                }
            }
        }

        if (bestTotalValue != null) return bestTotalValue

        // Priority 2: Largest plausible non-phone/non-date decimal or numeric value
        return lines
            .filterNot { line ->
                val l = line.lowercase(Locale.US)
                ignoredAmountLineKeywords.any { l.contains(it) } || feeLineKeywords.any { l.contains(it) }
            }
            .flatMap { extractNumbersFromLine(it) }
            .filter { it in 0.05..50_000_000.0 }
            .maxOrNull()
    }

    private fun extractFeeAmount(lines: List<String>, totalAmount: Double): Double {
        for (line in lines) {
            val lower = line.lowercase(Locale.US)
            if (feeLineKeywords.any { lower.contains(it) } && !lower.contains("total")) {
                val fee = extractNumbersFromLine(line).lastOrNull()
                if (fee != null && fee > 0.0 && fee < totalAmount) {
                    return fee
                }
            }
        }
        return 0.0
    }

    private fun extractNumbersFromLine(line: String): List<Double> {
        // Strip dates, times, card masks, and long phone/account IDs (8+ continuous digits)
        val cleaned = line
            .replace(Regex("\\b\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4}\\b"), " ")
            .replace(Regex("\\b\\d{4}[/-]\\d{1,2}[/-]\\d{1,2}\\b"), " ")
            .replace(Regex("\\b\\d{1,2}:\\d{2}(?::\\d{2})?\\b"), " ")
            .replace(Regex("[*xX•]{2,}\\s*\\d{4}"), " ")
            .replace(Regex("\\b\\d{8,}\\b"), " ")

        val regex = Regex("(\\d{1,3}(?:,\\d{2,3})+(?:\\.\\d{1,3})?|\\d+(?:\\.\\d{1,3})?)")
        return regex.findAll(cleaned)
            .mapNotNull { m ->
                m.value.replace(",", "").toDoubleOrNull()?.takeIf { it > 0.0 && it < 100_000_000.0 }
            }
            .toList()
    }

    private fun hasCurrencySymbolOrCode(line: String): Boolean {
        val lower = line.lowercase(Locale.US)
        return line.contains("৳") || line.contains("$") || line.contains("€") ||
            line.contains("£") || line.contains("₹") ||
            Regex("(?<![a-z])(kd|kwd|bdt|tk|sar|aed|qar|myr|eur|gbp|usd|inr)(?![a-z])").containsMatchIn(lower)
    }

    private fun detectProvider(lowerAll: String): String {
        val providers = listOf(
            "bKash" to listOf("bkash", "b-kash", "বিকাশ"),
            "Nagad" to listOf("nagad", "নগদ"),
            "Rocket" to listOf("rocket", "রকেট"),
            "Upay" to listOf("upay", "উপায়", "উপায়"),
            "Dutch-Bangla Bank" to listOf("dutch-bangla", "dutch bangla", "dbbl", "nexuspay"),
            "Trust Bank" to listOf("trust bank", "tap"),
            "City Bank" to listOf("city bank", "citytouch"),
            "BRAC Bank" to listOf("brac bank", "astha"),
            "Islami Bank" to listOf("islami bank", "ibbl", "cellfin"),
            "KFH" to listOf("kfh", "kuwait finance house"),
            "NBK" to listOf("nbk", "national bank of kuwait"),
            "Gulf Bank" to listOf("gulf bank"),
            "Boubyan" to listOf("boubyan"),
            "Al Rajhi" to listOf("al rajhi", "alrajhi"),
            "stc pay" to listOf("stc pay", "stcpay"),
            "ENBD" to listOf("enbd", "emirates nbd"),
            "QNB" to listOf("qnb", "qatar national bank")
        )
        for ((name, aliases) in providers) {
            if (aliases.any { lowerAll.contains(it) }) return name
        }
        return ""
    }

    private fun detectActionKeyword(lowerAll: String): String {
        val orderedKeywords = listOf(
            "cash in", "received money", "add money", "remittance", "deposit",
            "send money", "cash out", "pay bill", "mobile recharge", "payment",
            "bank transfer", "transfer", "sale", "ক্যাশ ইন", "সেন্ড মানি", "ক্যাশ আউট", "পেমেন্ট"
        )
        return orderedKeywords.firstOrNull { lowerAll.contains(it) }.orEmpty()
    }

    private fun extractMerchantName(lines: List<String>, providerHint: String): String {
        val candidate = lines.take(5).firstOrNull { line ->
            val lower = line.lowercase(Locale.US)
            line.length in 3..48 &&
                !Regex("^[\\d\\s.,:/-]+$").matches(line) &&
                !lower.startsWith("receipt") &&
                !lower.startsWith("invoice") &&
                !lower.startsWith("date") &&
                !lower.startsWith("time") &&
                !lower.startsWith("tel") &&
                !lower.startsWith("trx")
        }
        return candidate ?: providerHint.ifBlank { "Receipt Transaction" }
    }

    private fun extractReferenceId(asciiText: String): String {
        val match = Regex("(?:trx\\s*id|txn\\s*id|ref(?:erence)?|approval)\\s*[:#-]?\\s*([A-Za-z0-9]{5,24})", RegexOption.IGNORE_CASE)
            .find(asciiText)
        return match?.groupValues?.getOrNull(1)?.trim().orEmpty()
    }

    private fun extractCardLast4(asciiText: String): String {
        val match = Regex("(?:[*xX•]{2,}|ending\\s+in|card\\s*#?)\\s*(\\d{4})\\b", RegexOption.IGNORE_CASE)
            .find(asciiText)
        return match?.groupValues?.getOrNull(1).orEmpty()
    }

    private fun extractDateLabel(asciiText: String): String? {
        val datePatterns = listOf(
            Regex("\\b\\d{4}[-/]\\d{1,2}[-/]\\d{1,2}\\b"),
            Regex("\\b\\d{1,2}[-/]\\d{1,2}[-/]\\d{2,4}\\b"),
            Regex("\\b\\d{1,2}\\s+(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\\s+\\d{4}\\b", RegexOption.IGNORE_CASE)
        )
        for (regex in datePatterns) {
            val found = regex.find(asciiText)?.value
            if (!found.isNullOrBlank()) return found
        }
        return null
    }

    private fun buildSummary(merchant: String, providerHint: String, refId: String): String = buildString {
        if (providerHint.isNotBlank() && !merchant.contains(providerHint, ignoreCase = true)) {
            append(providerHint)
        }
        if (refId.isNotBlank()) {
            if (isNotEmpty()) append(" • ")
            append("TrxID: $refId")
        }
    }
}
