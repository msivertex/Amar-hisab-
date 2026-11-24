package com.example.data.ai

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.local.AccountEntity
import com.example.data.model.AppCurrency
import com.example.data.model.InternationalBankCatalog
import com.example.data.model.TransactionType
import com.example.localization.AppLanguage
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class MatchedAccountResult(
    val account: AccountEntity?,
    val isAutoMatched: Boolean,
    val matchReasonEn: String,
    val matchReasonBn: String
) {
    fun reason(language: AppLanguage): String =
        if (language == AppLanguage.BN) matchReasonBn else matchReasonEn
}

/**
 * Deterministic voucher classifier, multi-region wallet/card auto-matcher, receipt date parser,
 * and temporary capture file hygiene helper for Amar Hisab's AI Receipt & Voucher Scanner.
 */
object ReceiptVoucherMatcher {

    // Priority Money-In / Income keywords (English + Bangla) for bKash, Nagad, Rocket & Bank Transfers
    private val incomeKeywords = listOf(
        "cash in",
        "cash-in",
        "cashin",
        "received money",
        "money received",
        "receive money",
        "add money",
        "remittance",
        "deposit",
        "salary",
        "refund",
        "cashback",
        "received",
        "income",
        "credited",
        "ক্যাশ ইন",
        "ক্যাশ-ইন",
        "রিসিভড মানি",
        "টাকা পেয়েছেন",
        "টাকা পেয়েছেন",
        "পেয়েছি",
        "পেয়েছি",
        "ঢুকেছে",
        "ঢুকছে",
        "ঢুকলো",
        "ঢুকল",
        "জমা হয়েছে",
        "জমা হয়েছে",
        "টাকা এসেছে",
        "অ্যাড মানি",
        "এড মানি",
        "রেমিট্যান্স",
        "জমা",
        "ডিপোজিট",
        "বেতন",
        "রিফান্ড",
        "ক্যাশব্যাক"
    )

    // Priority Money-Out / Expense keywords (English + Bangla) for bKash, Nagad, Rocket, POS & Bank Transfers
    private val expenseKeywords = listOf(
        "send money",
        "cash out",
        "cash-out",
        "cashout",
        "pay bill",
        "bill pay",
        "bill payment",
        "mobile recharge",
        "top up",
        "top-up",
        "payment",
        "purchase",
        "withdraw",
        "withdrawal",
        "transfer",
        "debited",
        "debit",
        "sale",
        "paid",
        "সেন্ড মানি",
        "ক্যাশ আউট",
        "ক্যাশ-আউট",
        "পেমেন্ট",
        "বিল পরিশোধ",
        "বিল পে",
        "মোবাইল রিচার্জ",
        "রিচার্জ",
        "ট্রান্সফার",
        "খরচ",
        "উত্তোলন",
        "কেনাকাটা"
    )

    private val providerAliases: Map<String, List<String>> = mapOf(
        "bkash" to listOf("bkash", "b-kash", "বিকাশ"),
        "nagad" to listOf("nagad", "নগদ"),
        "rocket" to listOf("rocket", "রকেট", "dbbl mobile"),
        "upay" to listOf("upay", "উপায়", "উপায়"),
        "city bank" to listOf("city bank", "citytouch", "city", "সিটি ব্যাংক", "সিটি"),
        "dutch-bangla bank" to listOf("dutch-bangla", "dutch bangla", "dutchbangla", "dbbl", "nexuspay", "ডাচ-বাংলা", "ডাচ বাংলা", "ডাচবাংলা", "ডিবিবিএল"),
        "brac bank" to listOf("brac bank", "brac", "astha", "ব্র্যাক ব্যাংক", "ব্র্যাক"),
        "islami bank" to listOf("islami bank", "ibbl", "cellfin", "ইসলামী ব্যাংক", "ইসলামি ব্যাংক"),
        "trust bank" to listOf("trust bank", "tap", "ট্রাস্ট ব্যাংক"),
        "sonali bank" to listOf("sonali bank", "sonali", "সোনালী ব্যাংক"),
        "eastern bank" to listOf("eastern bank", "ebl", "ইস্টার্ন ব্যাংক", "ইবিএল"),
        "prime bank" to listOf("prime bank", "প্রাইম ব্যাংক"),
        "pubali bank" to listOf("pubali bank", "pubali", "পূবালী ব্যাংক", "পূবালী"),
        "ucb" to listOf("united commercial bank", "ucb", "ইউসিবি"),
        "mutual trust bank" to listOf("mutual trust bank", "mutual trust", "mtb", "মিউচুয়াল ট্রাস্ট"),
        "bank asia" to listOf("bank asia", "ব্যাংক এশিয়া", "ব্যাংক এশিয়া"),
        "dhaka bank" to listOf("dhaka bank", "ঢাকা ব্যাংক"),
        "ab bank" to listOf("ab bank", "এবি ব্যাংক"),
        "al-arafah bank" to listOf("al-arafah", "al arafah", "আল আরাফাহ"),
        "abk" to listOf("abk", "al ahli bank of kuwait", "al ahli kuwait", "ahli bank", "এবিকে", "আল আহলি"),
        "kfh" to listOf("kfh", "kuwait finance house", "কুয়েত ফিন্যান্স", "কুয়েত ফিন্যান্স"),
        "nbk" to listOf("nbk", "national bank of kuwait", "ন্যাশনাল ব্যাংক অফ কুয়েত"),
        "gulf bank" to listOf("gulf bank", "গালফ ব্যাংক"),
        "boubyan" to listOf("boubyan", "boubyan bank", "বুবিয়ান", "বুবিয়ান"),
        "burgan bank" to listOf("burgan", "burgan bank", "বুরগান"),
        "al tijari" to listOf("al tijari", "cbk", "commercial bank of kuwait", "আল তিজারি"),
        "warba bank" to listOf("warba", "warba bank", "ওয়ারবা", "ওয়ারবা"),
        "kib" to listOf("kib", "kuwait international bank"),
        "al rajhi" to listOf("al rajhi", "alrajhi", "rajhi", "আল রাজি"),
        "snb" to listOf("snb", "saudi national bank", "alahli", "সৌদি ন্যাশনাল"),
        "riyad bank" to listOf("riyad bank", "riyad", "রিয়াদ ব্যাংক"),
        "alinma" to listOf("alinma", "আলইনমা"),
        "stc pay" to listOf("stc pay", "stcpay", "stc bank", "এসটিসি"),
        "qnb" to listOf("qnb", "qatar national bank", "কাতার ন্যাশনাল"),
        "cbq" to listOf("cbq", "commercial bank of qatar"),
        "dukhan" to listOf("dukhan", "দুখান"),
        "masraf al rayan" to listOf("masraf", "al rayan", "মাসরাফ"),
        "enbd" to listOf("enbd", "emirates nbd", "এমিরেটস"),
        "fab" to listOf("fab", "first abu dhabi", "ফার্স্ট আবুধাবি"),
        "dib" to listOf("dib", "dubai islamic", "দুবাই ইসলামী"),
        "mashreq" to listOf("mashreq", "মাশরেক"),
        "adcb" to listOf("adcb", "এডিসিবি"),
        "maybank" to listOf("maybank", "মেব্যাংক"),
        "cimb" to listOf("cimb", "সিআইএমবি"),
        "public bank" to listOf("public bank", "pbb", "পাবলিক ব্যাংক"),
        "rhb" to listOf("rhb", "আরএইচবি"),
        "hong leong" to listOf("hong leong", "hlb", "হং লিওং"),
        "revolut" to listOf("revolut", "রেভোলুট"),
        "wise" to listOf("wise", "transferwise", "ওয়াইজ", "ওয়াইজ"),
        "barclays" to listOf("barclays", "বার্কলেজ"),
        "hsbc" to listOf("hsbc", "এইচএসবিসি"),
        "monzo" to listOf("monzo", "মনজো"),
        "cash" to listOf("cash", "নগদ টাকা", "ক্যাশ")
    )

    /**
     * Normalizes symbols, local spellings, and codes (e.g., "KD", "৳", "Tk", "SAR", "AED")
     * into a supported [AppCurrency.code].
     */
    fun normalizeCurrencyCode(raw: String?, fallback: String = "BDT"): String {
        if (raw.isNullOrBlank()) return AppCurrency.fromCode(fallback).code
        val cleaned = raw.trim().uppercase(Locale.US)
        AppCurrency.fromCodeOrNull(cleaned)?.let { return it.code }
        return when {
            cleaned == "KD" || cleaned.contains("KWD") || cleaned.contains("DINAR") || cleaned.contains("দিনার") -> "KWD"
            cleaned == "TK" || cleaned == "BDT" || cleaned.contains("৳") || cleaned.contains("TAKA") || cleaned.contains("টাকা") -> "BDT"
            cleaned == "SR" || cleaned.contains("SAR") || cleaned.contains("SAUDI") || cleaned.contains("সৌদি") -> "SAR"
            cleaned == "DH" || cleaned == "DHS" || cleaned.contains("AED") || cleaned.contains("DIRHAM") || cleaned.contains("দিরহাম") -> "AED"
            cleaned == "QR" || cleaned.contains("QAR") || cleaned.contains("QATARI") || cleaned.contains("কাতার") -> "QAR"
            cleaned == "RM" || cleaned.contains("MYR") || cleaned.contains("RINGGIT") || cleaned.contains("রিংগিত") -> "MYR"
            cleaned.contains("€") || cleaned.contains("EUR") || cleaned.contains("EURO") || cleaned.contains("ইউরো") -> "EUR"
            cleaned.contains("£") || cleaned.contains("GBP") || cleaned.contains("POUND") || cleaned.contains("পাউন্ড") -> "GBP"
            cleaned.contains("$") || cleaned.contains("USD") || cleaned.contains("DOLLAR") || cleaned.contains("ডলার") -> "USD"
            cleaned.contains("₹") || cleaned.contains("INR") || cleaned.contains("RUPEE") || cleaned.contains("রুপি") -> "INR"
            else -> AppCurrency.fromCode(fallback).code
        }
    }

    /**
     * Deterministic direction classification (Money In / Income vs Money Out / Expense).
     * Checks explicit voucher action keywords first, then merchant/summary context, then AI model hint.
     */
    fun classifyTransactionDirection(
        detectedKeyword: String,
        merchantName: String = "",
        itemsSummary: String = "",
        modelTypeRaw: String? = null
    ): TransactionType {
        val keywordNorm = normalizeForMatch(detectedKeyword)
        if (keywordNorm.isNotBlank()) {
            classifyTextSnippet(keywordNorm)?.let { return it }
        }

        val combinedNorm = normalizeForMatch("$merchantName $itemsSummary")
        if (combinedNorm.isNotBlank()) {
            classifyTextSnippet(combinedNorm)?.let { return it }
        }

        return when (modelTypeRaw?.trim()?.uppercase(Locale.US)) {
            "INCOME", "MONEY_IN", "CREDIT", "IN" -> TransactionType.INCOME
            else -> TransactionType.EXPENSE
        }
    }

    private fun classifyTextSnippet(normalizedText: String): TransactionType? {
        // Check explicit income keywords first (e.g., "cash in", "received money", "add money", "remittance", "deposit", "ক্যাশ ইন", "জমা")
        val hasIncomePhrase = incomeKeywords.any { kw -> matchesToken(normalizedText, kw) }
        // Guard standalone "credit" so "credit card" on a POS slip doesn't falsely classify as income
        val hasStandaloneCredit = matchesStandaloneWord(normalizedText, "credit") &&
            !normalizedText.contains("credit card") &&
            !normalizedText.contains("credit limit")

        // Check explicit expense keywords ("send money", "payment", "cash out", "pay bill", "mobile recharge", "sale", "debit", "transfer")
        val hasExpensePhrase = expenseKeywords.any { kw -> matchesToken(normalizedText, kw) }

        return when {
            (hasIncomePhrase || hasStandaloneCredit) && !hasExpensePhrase -> TransactionType.INCOME
            hasExpensePhrase && !(hasIncomePhrase || hasStandaloneCredit) -> TransactionType.EXPENSE
            // Both appear (e.g. "Remittance Cash In — Bank Transfer" or "Credit Card Payment SALE")
            hasIncomePhrase && hasExpensePhrase -> {
                val incomePos = incomeKeywords.mapNotNull { idxOf(normalizedText, it) }.minOrNull() ?: Int.MAX_VALUE
                val expensePos = expenseKeywords.mapNotNull { idxOf(normalizedText, it) }.minOrNull() ?: Int.MAX_VALUE
                if (incomePos <= expensePos) TransactionType.INCOME else TransactionType.EXPENSE
            }
            else -> null
        }
    }

    private fun idxOf(text: String, keyword: String): Int? {
        val pos = text.indexOf(keyword)
        return if (pos >= 0 && matchesToken(text, keyword)) pos else null
    }

    private fun matchesToken(text: String, keyword: String): Boolean {
        val kw = keyword.lowercase(Locale.US)
        // For short ASCII words like "sale", "paid", "debit", require word boundaries
        val isPureAscii = kw.all { it.code < 128 }
        return if (isPureAscii) {
            Regex("(?<![a-z0-9])${Regex.escape(kw)}(?![a-z0-9])").containsMatchIn(text)
        } else {
            text.contains(kw)
        }
    }

    private fun matchesStandaloneWord(text: String, word: String): Boolean =
        Regex("(?<![a-z0-9])${Regex.escape(word)}(?![a-z0-9])").containsMatchIn(text)

    /**
     * Automatically matches the detected provider (bKash, Nagad, Rocket, Bank/Card name)
     * and/or card last-4 digits and currency to the user's added accounts/cards.
     */
    fun matchAccount(
        accounts: List<AccountEntity>,
        providerHint: String,
        accountLast4Hint: String = "",
        currencyCode: String = "BDT",
        allowCurrencyFallback: Boolean = true
    ): MatchedAccountResult {
        if (accounts.isEmpty()) {
            return MatchedAccountResult(
                account = null,
                isAutoMatched = false,
                matchReasonEn = "No wallet or card added yet",
                matchReasonBn = "এখনো কোনো ওয়ালেট বা কার্ড যোগ করা হয়নি"
            )
        }

        val cleanLast4 = Regex("\\d{4}").findAll(toAsciiDigits(accountLast4Hint))
            .lastOrNull()?.value.orEmpty()
        val normProvider = normalizeForMatch(providerHint)
        val canonicalProviderKey = resolveCanonicalProviderKey(normProvider)
        val normCurrency = if (currencyCode.isBlank()) "" else normalizeCurrencyCode(currencyCode)

        data class Candidate(
            val account: AccountEntity,
            val score: Int,
            val matchedByLast4: Boolean,
            val matchedByProvider: Boolean,
            val matchedByCurrency: Boolean
        )

        val candidates = accounts.map { acc ->
            val accLast4 = Regex("\\d{4}").findAll(toAsciiDigits(acc.accountNumberLast4))
                .lastOrNull()?.value.orEmpty()
            val accSearchText = normalizeForMatch("${acc.name} ${acc.bankOrProvider} ${acc.cardNetwork}")
            val accCanonicalKey = resolveCanonicalProviderKey(accSearchText)

            val last4Match = cleanLast4.isNotBlank() && accLast4 == cleanLast4
            val providerMatch = normProvider.isNotBlank() && (
                (canonicalProviderKey != null && canonicalProviderKey == accCanonicalKey) ||
                    accSearchText.contains(normProvider) ||
                    (acc.bankOrProvider.isNotBlank() && normProvider.contains(normalizeForMatch(acc.bankOrProvider))) ||
                    matchesInternationalCatalog(normProvider, accSearchText)
                )
            val currencyMatch = acc.currencyCode.equals(normCurrency, ignoreCase = true)

            var score = 0
            if (last4Match) score += 120
            if (providerMatch) score += 70
            if (currencyMatch) score += 20

            Candidate(
                account = acc,
                score = score,
                matchedByLast4 = last4Match,
                matchedByProvider = providerMatch,
                matchedByCurrency = currencyMatch
            )
        }

        val best = candidates.maxByOrNull { it.score }
        if (best != null && (best.matchedByLast4 || best.matchedByProvider)) {
            val label = providerHint.ifBlank { best.account.name }
            val reasonEn = when {
                best.matchedByLast4 && best.matchedByProvider ->
                    "Auto-matched: ${best.account.name} (•••• ${cleanLast4})"
                best.matchedByLast4 ->
                    "Auto-matched by card •••• $cleanLast4"
                else ->
                    "Auto-matched: $label → ${best.account.name}"
            }
            val reasonBn = when {
                best.matchedByLast4 && best.matchedByProvider ->
                    "স্বয়ংক্রিয়ভাবে মিলেছে: ${best.account.name} (•••• ${cleanLast4})"
                best.matchedByLast4 ->
                    "কার্ড নম্বর •••• $cleanLast4 দিয়ে মিলেছে"
                else ->
                    "স্বয়ংক্রিয়ভাবে মিলেছে: ${best.account.name}"
            }
            return MatchedAccountResult(
                account = best.account,
                isAutoMatched = true,
                matchReasonEn = reasonEn,
                matchReasonBn = reasonBn
            )
        }

        // Secondary fallback: match by currency (e.g., KD receipt -> KFH KWD account)
        if (allowCurrencyFallback && normCurrency.isNotBlank()) {
            val currencyMatched = accounts.firstOrNull { it.currencyCode.equals(normCurrency, ignoreCase = true) }
            if (currencyMatched != null) {
                val cleanBadge = AppCurrency.fromCode(normCurrency).cleanBadgeLabel()
                return MatchedAccountResult(
                    account = currencyMatched,
                    isAutoMatched = true,
                    matchReasonEn = "Matched $cleanBadge wallet: ${currencyMatched.name}",
                    matchReasonBn = "$cleanBadge ওয়ালেট মিলেছে: ${currencyMatched.name}"
                )
            }
        }

        val fallback = accounts.first()
        return MatchedAccountResult(
            account = fallback,
            isAutoMatched = false,
            matchReasonEn = "No exact match — select card/wallet below",
            matchReasonBn = "সরাসরি মেলেনি — নিচ থেকে সঠিক কার্ড/ওয়ালেট বাছাই করুন"
        )
    }

    private fun resolveCanonicalProviderKey(normalizedText: String): String? {
        if (normalizedText.isBlank()) return null
        for ((canonical, aliases) in providerAliases) {
            if (aliases.any { alias -> matchesToken(normalizedText, alias) }) {
                return canonical
            }
        }
        return null
    }

    private fun matchesInternationalCatalog(normProvider: String, accSearchText: String): Boolean {
        return InternationalBankCatalog.all.any { bank ->
            val en = normalizeForMatch(bank.en)
            val bn = normalizeForMatch(bank.bn)
            val code = normalizeForMatch(bank.shortCode)
            val providerHitsBank = matchesToken(normProvider, code) ||
                normProvider.contains(en) ||
                (bn.isNotBlank() && normProvider.contains(bn))
            val accountHitsBank = matchesToken(accSearchText, code) ||
                accSearchText.contains(en) ||
                (bn.isNotBlank() && accSearchText.contains(bn))
            providerHitsBank && accountHitsBank
        }
    }

    /**
     * Parses common receipt/SMS/voucher date strings (English or Bangla digits) into epoch millis.
     * Returns null when the date is blank, "Today", or unparseable so the caller can keep current time.
     */
    fun parseReceiptTimestamp(dateLabel: String?, fallbackMillis: Long = System.currentTimeMillis()): Long? {
        val raw = toAsciiDigits(dateLabel.orEmpty()).trim()
        if (raw.isBlank()) return null
        val lower = raw.lowercase(Locale.US)
        if (lower == "today" || lower == "আজ") return fallbackMillis
        if (lower == "yesterday" || lower == "গতকাল") {
            return Calendar.getInstance().apply {
                timeInMillis = fallbackMillis
                add(Calendar.DAY_OF_MONTH, -1)
            }.timeInMillis
        }

        val patterns = listOf(
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd HH:mm",
            "yyyy-MM-dd",
            "dd/MM/yyyy HH:mm",
            "dd/MM/yyyy hh:mm a",
            "dd/MM/yyyy",
            "dd-MM-yyyy HH:mm",
            "dd-MM-yyyy",
            "dd MMM yyyy HH:mm",
            "dd MMM yyyy hh:mm a",
            "dd MMM yyyy",
            "MMM dd, yyyy",
            "dd/MM/yy"
        )
        for (pattern in patterns) {
            val parsed = runCatching {
                SimpleDateFormat(pattern, Locale.US).apply {
                    isLenient = false
                }.parse(raw)
            }.getOrNull()
            if (parsed != null) return parsed.time
        }
        return null
    }

    fun toAsciiDigits(input: String): String = buildString(input.length) {
        for (ch in input) {
            append(
                when (ch) {
                    in '০'..'৯' -> ('0'.code + (ch.code - '০'.code)).toChar()
                    in '٠'..'٩' -> ('0'.code + (ch.code - '٠'.code)).toChar()
                    else -> ch
                }
            )
        }
    }

    private fun normalizeForMatch(input: String): String =
        toAsciiDigits(input)
            .lowercase(Locale.US)
            .replace(Regex("\\s+"), " ")
            .trim()
}

/**
 * Ensures temporary camera-captured receipt images live strictly in app cache (`cacheDir/receipts`)
 * and are deleted automatically after preview/save/dismiss so zero duplicate photos accumulate.
 * Never deletes user-selected gallery URIs.
 */
object TempReceiptImageCleaner {

    private const val RECEIPTS_CACHE_SUBDIR = "receipts"

    fun receiptCacheDir(context: Context): File =
        File(context.cacheDir, RECEIPTS_CACHE_SUBDIR).apply { mkdirs() }

    fun createTempCaptureFileAndUri(context: Context): Pair<File, Uri> {
        val dir = receiptCacheDir(context)
        val file = File(dir, "receipt_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        return file to uri
    }

    fun deleteFileQuietly(file: File?) {
        if (file == null) return
        runCatching {
            if (file.exists()) file.delete()
        }
    }

    /**
     * Deletes a URI ONLY if it belongs to this app's FileProvider receipt cache directory.
     */
    fun deleteTempUriIfOwned(context: Context, uri: Uri?) {
        if (uri == null) return
        val expectedAuthority = "${context.packageName}.fileprovider"
        if (uri.authority != expectedAuthority) return
        val fileName = uri.lastPathSegment ?: return
        val target = File(receiptCacheDir(context), fileName)
        deleteFileQuietly(target)
    }

    /**
     * Cleans all temporary camera capture files in `cacheDir/receipts`, optionally sparing one active file.
     */
    fun cleanupAllTempReceipts(context: Context, keepFileName: String? = null) {
        runCatching {
            val dir = File(context.cacheDir, RECEIPTS_CACHE_SUBDIR)
            if (!dir.exists() || !dir.isDirectory) return
            dir.listFiles()?.forEach { child ->
                if (child.isFile && child.name != keepFileName) {
                    child.delete()
                }
            }
        }
    }
}
