package com.example.data.ai

import com.example.data.local.AccountEntity
import com.example.data.model.AppCurrency
import com.example.data.model.ExpenseCategory
import com.example.data.model.TransactionType
import com.example.data.model.VoiceParsedTransaction
import com.example.localization.AppLanguage
import java.util.Locale

/**
 * Deterministic Bangla + English + Banglish natural-language transaction parser.
 * Serves as an instant on-device fallback whenever cloud AI is unreachable or fails,
 * supporting transfers between accounts (e.g., Trust -> Dutch-Bangla), expenses/cash-outs
 * (e.g., bKash groceries/market), income, and multi-currency detection (KD/KWD, BDT, SAR, EUR, AED, USD).
 */
object LocalVoiceTransactionParser {

    private data class AccountMention(
        val label: String,
        val matchedAccount: AccountEntity?,
        val startIndex: Int
    )

    private val knownProviderTokens: List<Pair<String, List<String>>> = listOf(
        "Trust Bank" to listOf("trust bank", "trust", "ট্রাস্ট ব্যাংক", "ট্রাস্ট", "tap"),
        "Dutch-Bangla Bank" to listOf("dutch-bangla", "dutch bangla", "dbbl", "nexuspay", "ডাচ-বাংলা", "ডাচ বাংলা"),
        "City Bank" to listOf("city bank", "citytouch", "city", "সিটি ব্যাংক", "সিটি"),
        "BRAC Bank" to listOf("brac bank", "brac", "astha", "ব্র্যাক ব্যাংক", "ব্র্যাক"),
        "Islami Bank" to listOf("islami bank", "ibbl", "cellfin", "ইসলামী ব্যাংক", "ইসলামি ব্যাংক", "ইসলামী"),
        "Sonali Bank" to listOf("sonali bank", "sonali", "সোনালী ব্যাংক", "সোনালী"),
        "bKash" to listOf("bkash", "b-kash", "bikash", "বিকাশ"),
        "Nagad" to listOf("nagad", "নগদ"),
        "Rocket" to listOf("rocket", "রকেট"),
        "Upay" to listOf("upay", "উপায়", "উপায়"),
        "KFH" to listOf("kfh", "kuwait finance house", "কুয়েত ফিন্যান্স", "কুয়েত ফিন্যান্স"),
        "NBK" to listOf("nbk", "national bank of kuwait"),
        "Gulf Bank" to listOf("gulf bank", "গালফ ব্যাংক"),
        "Boubyan" to listOf("boubyan", "বুবিয়ান", "বুবিয়ান"),
        "Al Rajhi" to listOf("al rajhi", "alrajhi", "rajhi", "আল রাজি"),
        "SNB" to listOf("snb", "saudi national bank", "alahli"),
        "stc pay" to listOf("stc pay", "stcpay", "stc"),
        "ENBD" to listOf("enbd", "emirates nbd"),
        "FAB" to listOf("fab", "first abu dhabi"),
        "QNB" to listOf("qnb", "qatar national bank"),
        "Maybank" to listOf("maybank"),
        "Wise" to listOf("wise", "transferwise"),
        "Revolut" to listOf("revolut"),
        "Cash" to listOf("cash", "নগদ টাকা", "ক্যাশ", "পকেট")
    )

    private val transferKeywords = listOf(
        "transfer", "transferred", "move", "moved", "send to", "sent to",
        "ট্রান্সফার", "স্থানান্তর", "পাঠিয়েছি", "পাঠিয়েছি", "পাঠালাম", "পাঠানো", "সরালাম", "ট্রান্সফার করেছি",
        "pathiyechi", "pathalam", "pathano", "->"
    )

    private val incomeKeywords = listOf(
        "salary", "income", "received", "cash in", "cash-in", "add money", "deposit",
        "bonus", "remittance", "refund", "cashback", "freelance", "credited", "got paid",
        "বেতন", "আয়", "আয়", "পেয়েছি", "পেয়েছি", "এসেছে", "জমা", "ক্যাশ ইন", "ক্যাশ-ইন",
        "বোনাস", "রেমিট্যান্স", "অ্যাড মানি", "এড মানি", "রিফান্ড", "ডিপোজিট", "beton", "peyechi"
    )

    private val expenseKeywords = listOf(
        "spent", "spend", "paid", "pay", "bought", "buy", "expense", "cost",
        "cash out", "cash-out", "cashout", "bill", "recharge", "grocery", "groceries",
        "market", "bazar", "bazaar", "food", "lunch", "dinner", "breakfast", "coffee",
        "uber", "cng", "bus", "rent", "medicine", "doctor", "shopping",
        "খরচ", "বাজার", "কিনেছি", "দিয়েছি", "দিয়েছি", "দিলাম", "ক্যাশ আউট", "ক্যাশ-আউট",
        "বিল", "রিচার্জ", "ভাড়া", "ভাড়া", "খাবার", "কেনাকাটা", "ঔষধ", "ওষুধ", "ডাক্তার", "bazar", "khoroch"
    )

    fun parse(
        utterance: String,
        accounts: List<AccountEntity>,
        language: AppLanguage,
        defaultCurrencyCode: String = "BDT"
    ): VoiceParsedTransaction {
        val raw = utterance.trim()
        val ascii = ReceiptVoucherMatcher.toAsciiDigits(raw)
        val lower = ascii.lowercase(Locale.US)

        // 1. Extract amount (supports decimals like 12.500, commas like 5,000, and k/hajar shorthand)
        val amount = extractAmount(ascii)

        // 2. Detect all account/bank/wallet mentions in order of appearance
        val mentions = findAccountMentions(lower, accounts)

        // 3. Detect transaction type (TRANSFER vs INCOME vs EXPENSE)
        val hasTransferPhrase = transferKeywords.any { lower.contains(it) } ||
            Regex("(?:from|theke|থেকে).+(?:to|te|e|তে|এ)").containsMatchIn(lower)
        val hasTwoDistinctAccounts = mentions.size >= 2 &&
            mentions[0].label != mentions[1].label &&
            (mentions[0].matchedAccount == null || mentions[1].matchedAccount == null ||
                mentions[0].matchedAccount?.id != mentions[1].matchedAccount?.id)

        val hasExpenseHint = expenseKeywords.any { lower.contains(it) }
        val hasIncomeHint = incomeKeywords.any { lower.contains(it) }

        val type = when {
            hasTwoDistinctAccounts && (hasTransferPhrase || !hasExpenseHint) -> TransactionType.TRANSFER
            hasTransferPhrase && mentions.size >= 2 -> TransactionType.TRANSFER
            hasIncomeHint && !hasExpenseHint -> TransactionType.INCOME
            else -> TransactionType.EXPENSE
        }

        // 4. Resolve source & destination account hints (respecting "from X to Y" or "X থেকে Y")
        var fromHint = ""
        var toHint = ""
        var matchedSourceAccount: AccountEntity? = null
        if (type == TransactionType.TRANSFER && mentions.size >= 2) {
            val ordered = orderTransferMentions(lower, mentions)
            fromHint = ordered.first.matchedAccount?.name ?: ordered.first.label
            toHint = ordered.second.matchedAccount?.name ?: ordered.second.label
            matchedSourceAccount = ordered.first.matchedAccount
        } else if (mentions.isNotEmpty()) {
            fromHint = mentions.first().matchedAccount?.name ?: mentions.first().label
            matchedSourceAccount = mentions.first().matchedAccount
        }

        // 5. Detect currency (explicit symbol/code > matched source account currency > default)
        val explicitCurrency = detectExplicitCurrency(raw, lower)
        val resolvedCurrency = explicitCurrency
            ?: matchedSourceAccount?.currencyCode?.takeIf { it.isNotBlank() }
            ?: AppCurrency.fromCode(defaultCurrencyCode).code

        // 6. Infer category
        val category = inferCategory(lower, type)

        // 7. Build a clean, concise title
        val title = buildTitle(
            raw = raw,
            type = type,
            category = category,
            fromHint = fromHint,
            toHint = toHint,
            language = language
        )

        return VoiceParsedTransaction(
            title = title,
            amountBdt = amount,
            type = type,
            category = category,
            accountHint = fromHint,
            currencyCode = resolvedCurrency,
            toAccountHint = toHint,
            note = raw.take(120),
            isLocalFallback = true
        )
    }

    fun matchAccountByHint(
        accounts: List<AccountEntity>,
        hint: String,
        preferredCurrency: String? = null,
        excludeAccountId: Long? = null
    ): AccountEntity? {
        val candidates = accounts.filter { excludeAccountId == null || it.id != excludeAccountId }
        if (candidates.isEmpty()) return null

        val cleanHint = ReceiptVoucherMatcher.toAsciiDigits(hint).trim().lowercase(Locale.US)
        if (cleanHint.isNotBlank()) {
            // 1. Direct name or bank/provider substring match
            val directMatches = candidates.filter { acc ->
                val name = ReceiptVoucherMatcher.toAsciiDigits(acc.name).lowercase(Locale.US)
                val bank = ReceiptVoucherMatcher.toAsciiDigits(acc.bankOrProvider).lowercase(Locale.US)
                (name.isNotBlank() && (name.contains(cleanHint) || cleanHint.contains(name))) ||
                    (bank.isNotBlank() && (bank.contains(cleanHint) || cleanHint.contains(bank)))
            }
            if (directMatches.isNotEmpty()) {
                return preferredCurrency?.let { cur ->
                    directMatches.firstOrNull { it.currencyCode.equals(cur, ignoreCase = true) }
                } ?: directMatches.first()
            }

            // 2. Canonical bank/MFS alias match via ReceiptVoucherMatcher
            val canonical = knownProviderTokens.firstOrNull { (name, aliases) ->
                cleanHint.contains(name.lowercase(Locale.US)) ||
                    aliases.any { cleanHint.contains(it) }
            }?.first ?: hint

            val matched = ReceiptVoucherMatcher.matchAccount(
                accounts = candidates,
                providerHint = canonical,
                currencyCode = preferredCurrency ?: ""
            )
            if (matched.isAutoMatched && matched.account != null) {
                return matched.account
            }
        }
        return null
    }

    private fun extractAmount(ascii: String): Double {
        // Check shorthand like "5k" or "2.5k"
        val kMatch = Regex("(?<![a-z0-9])(\\d+(?:\\.\\d+)?)\\s*k(?![a-z0-9])", RegexOption.IGNORE_CASE)
            .find(ascii)
        if (kMatch != null) {
            val base = kMatch.groupValues[1].toDoubleOrNull()
            if (base != null && base > 0.0) return base * 1000.0
        }

        // Check Bangla "হাজার" (thousand) e.g. "৫ হাজার" or "5 hajar"
        val hajarMatch = Regex("(\\d+(?:\\.\\d+)?)\\s*(?:হাজার|hajar|hazar|thousand)", RegexOption.IGNORE_CASE)
            .find(ascii)
        if (hajarMatch != null) {
            val base = hajarMatch.groupValues[1].toDoubleOrNull()
            if (base != null && base > 0.0) return base * 1000.0
        }

        // General numbers (ignoring 4-digit card last4 preceded by **** or xxxx)
        val cleaned = ascii.replace(Regex("[*xX•]{2,}\\s*\\d{4}"), " ")
        val numberRegex = Regex("(\\d{1,3}(?:,\\d{2,3})+(?:\\.\\d{1,3})?|\\d+(?:\\.\\d{1,3})?)")
        val numbers = numberRegex.findAll(cleaned)
            .mapNotNull { m ->
                val rawNum = m.value.replace(",", "")
                rawNum.toDoubleOrNull()?.takeIf { it > 0.0 && it < 1_000_000_000.0 }
            }
            .toList()

        return numbers.firstOrNull() ?: 0.0
    }

    private fun detectExplicitCurrency(raw: String, lower: String): String? {
        return when {
            Regex("(?<![a-z])(kwd|kd)(?![a-z])").containsMatchIn(lower) ||
                lower.contains("dinar") || raw.contains("দিনার") || raw.contains("কেডি") -> "KWD"
            Regex("(?<![a-z])(sar|sr)(?![a-z])").containsMatchIn(lower) ||
                lower.contains("riyal") || raw.contains("রিয়াল") || raw.contains("রিয়াল") -> "SAR"
            Regex("(?<![a-z])(aed|dhs|dh)(?![a-z])").containsMatchIn(lower) ||
                lower.contains("dirham") || raw.contains("দিরহাম") -> "AED"
            Regex("(?<![a-z])(qar|qr)(?![a-z])").containsMatchIn(lower) ||
                lower.contains("qatari") -> "QAR"
            Regex("(?<![a-z])(myr|rm)(?![a-z])").containsMatchIn(lower) ||
                lower.contains("ringgit") || raw.contains("রিংগিত") -> "MYR"
            raw.contains("€") || Regex("(?<![a-z])eur(?![a-z])").containsMatchIn(lower) ||
                lower.contains("euro") || raw.contains("ইউরো") -> "EUR"
            raw.contains("£") || Regex("(?<![a-z])gbp(?![a-z])").containsMatchIn(lower) ||
                lower.contains("pound") || raw.contains("পাউন্ড") -> "GBP"
            raw.contains("$") || Regex("(?<![a-z])usd(?![a-z])").containsMatchIn(lower) ||
                lower.contains("dollar") || raw.contains("ডলার") -> "USD"
            raw.contains("₹") || Regex("(?<![a-z])inr(?![a-z])").containsMatchIn(lower) ||
                lower.contains("rupee") || raw.contains("রুপি") -> "INR"
            raw.contains("৳") || Regex("(?<![a-z])(bdt|tk|taka)(?![a-z])").containsMatchIn(lower) ||
                raw.contains("টাকা") -> "BDT"
            else -> null
        }
    }

    private fun findAccountMentions(lower: String, accounts: List<AccountEntity>): List<AccountMention> {
        val found = mutableListOf<AccountMention>()

        // 1. Direct matches against the user's own account names & banks/providers
        for (acc in accounts) {
            val candidates = listOf(acc.name, acc.bankOrProvider)
                .map { ReceiptVoucherMatcher.toAsciiDigits(it).lowercase(Locale.US).trim() }
                .filter { it.length >= 3 }
            var bestIdx = Int.MAX_VALUE
            for (cand in candidates) {
                val idx = lower.indexOf(cand)
                if (idx >= 0 && idx < bestIdx) bestIdx = idx
            }
            if (bestIdx != Int.MAX_VALUE) {
                found += AccountMention(label = acc.name, matchedAccount = acc, startIndex = bestIdx)
            }
        }

        // 2. Canonical bank/MFS alias matches mapped to user's accounts when possible
        for ((canonical, aliases) in knownProviderTokens) {
            var bestIdx = Int.MAX_VALUE
            for (alias in aliases) {
                val idx = indexOfToken(lower, alias)
                if (idx >= 0 && idx < bestIdx) bestIdx = idx
            }
            if (bestIdx != Int.MAX_VALUE) {
                val alreadyMatchedAtPos = found.any { kotlin.math.abs(it.startIndex - bestIdx) <= 2 }
                if (!alreadyMatchedAtPos) {
                    val matched = ReceiptVoucherMatcher.matchAccount(
                        accounts = accounts,
                        providerHint = canonical
                    ).takeIf { it.isAutoMatched }?.account
                    // Avoid duplicating same account already found
                    if (matched == null || found.none { it.matchedAccount?.id == matched.id }) {
                        found += AccountMention(
                            label = matched?.name ?: canonical,
                            matchedAccount = matched,
                            startIndex = bestIdx
                        )
                    }
                }
            }
        }

        return found.sortedBy { it.startIndex }
    }

    private fun orderTransferMentions(
        lower: String,
        mentions: List<AccountMention>
    ): Pair<AccountMention, AccountMention> {
        val first = mentions[0]
        val second = mentions[1]
        // Handle reverse phrasing like "to DBBL from Trust"
        val toIdx = listOf(" to ", "->").map { lower.indexOf(it) }.filter { it >= 0 }.minOrNull()
        val fromIdx = listOf(" from ").map { lower.indexOf(it) }.filter { it >= 0 }.minOrNull()
        if (toIdx != null && fromIdx != null && toIdx < fromIdx) {
            return second to first
        }
        return first to second
    }

    private fun indexOfToken(text: String, token: String): Int {
        val isPureAscii = token.all { it.code < 128 }
        return if (isPureAscii) {
            val match = Regex("(?<![a-z0-9])${Regex.escape(token)}(?![a-z0-9])").find(text)
            match?.range?.first ?: -1
        } else {
            text.indexOf(token)
        }
    }

    private fun inferCategory(lower: String, type: TransactionType): ExpenseCategory {
        if (type == TransactionType.TRANSFER) return ExpenseCategory.TRANSFER
        if (type == TransactionType.INCOME) return ExpenseCategory.SALARY_INCOME

        return when {
            listOf("grocer", "market", "bazar", "bazaar", "super shop", "shwapno", "vegetable", "fish", "meat",
                "বাজার", "কাঁচাবাজার", "গ্রোসারি", "সবজি", "মাছ", "মাংস", "চাল", "ডাল")
                .any { lower.contains(it) } -> ExpenseCategory.GROCERIES

            listOf("food", "lunch", "dinner", "breakfast", "restaurant", "tea", "coffee", "snack", "biryani", "burger", "pizza",
                "খাবার", "নাস্তা", "দুপুরের খাবার", "রাতের খাবার", "চা", "কফি", "রেস্টুরেন্ট", "বিরিয়ানি", "বিরিয়ানি")
                .any { lower.contains(it) } -> ExpenseCategory.FOOD_DINING

            listOf("rent", "electricity", "gas", "water", "wifi", "internet", "bill", "desco", "dpdc", "utility", "recharge",
                "বাসা ভাড়া", "বাসা ভাড়া", "বিদ্যুৎ", "গ্যাস", "পানি", "বিল", "ইন্টারনেট", "ওয়াইফাই", "রিচার্জ")
                .any { lower.contains(it) } -> ExpenseCategory.RENT_UTILITIES

            listOf("uber", "pathao", "cng", "rickshaw", "bus", "metro", "train", "fuel", "petrol", "octane", "taxi", "flight", "fare",
                "রিকশা", "উবার", "পাঠাও", "সিএনজি", "বাস", "মেট্রো", "ভাড়া", "ভাড়া", "যাতায়াত", "তেল", "ফুয়েল")
                .any { lower.contains(it) } -> ExpenseCategory.TRANSPORT

            listOf("medicine", "pharmacy", "doctor", "hospital", "clinic", "health", "test", "lab",
                "ঔষধ", "ওষুধ", "ডাক্তার", "হাসপাতাল", "ফার্মেসি", "চিকিৎসা", "স্বাস্থ্য")
                .any { lower.contains(it) } -> ExpenseCategory.HEALTH

            listOf("shop", "clothes", "shirt", "shoe", "dress", "electronics", "daraz", "gadget", "gift",
                "কেনাকাটা", "পোশাক", "জামা", "জুতা", "শপিং", "উপহার")
                .any { lower.contains(it) } -> ExpenseCategory.SHOPPING

            listOf("movie", "cinema", "netflix", "spotify", "outing", "game", "subscription", "tour",
                "সিনেমা", "বিনোদন", "ভ্রমণ", "সাবস্ক্রিপশন")
                .any { lower.contains(it) } -> ExpenseCategory.ENTERTAINMENT

            else -> ExpenseCategory.OTHER
        }
    }

    private fun buildTitle(
        raw: String,
        type: TransactionType,
        category: ExpenseCategory,
        fromHint: String,
        toHint: String,
        language: AppLanguage
    ): String {
        val isBn = language == AppLanguage.BN
        if (type == TransactionType.TRANSFER) {
            return if (fromHint.isNotBlank() && toHint.isNotBlank()) {
                "$fromHint → $toHint"
            } else {
                if (isBn) "অ্যাকাউন্ট ট্রান্সফার" else "Account Transfer"
            }
        }
        if (category != ExpenseCategory.OTHER) {
            val baseLabel = category.label(language)
            return if (fromHint.isNotBlank() && raw.lowercase(Locale.US).contains("cash out") ||
                raw.contains("ক্যাশ আউট")
            ) {
                if (isBn) "$fromHint ক্যাশ আউট ($baseLabel)" else "$fromHint Cash Out ($baseLabel)"
            } else {
                baseLabel
            }
        }
        val cleaned = raw
            .replace(Regex("\\s+"), " ")
            .trim()
            .take(36)
        return cleaned.ifBlank { if (isBn) "দ্রুত এন্ট্রি" else "Quick Entry" }
    }
}
