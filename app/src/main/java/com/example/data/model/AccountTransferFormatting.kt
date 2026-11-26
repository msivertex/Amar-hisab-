package com.example.data.model

import com.example.data.local.AccountEntity
import com.example.data.local.TransactionEntity
import com.example.data.market.MarketSnapshot
import com.example.localization.AppLanguage
import com.example.localization.BdtFormatter
import java.util.Locale

data class ParsedTransferDetail(
    val sourceCurrencyCode: String,
    val destCurrencyCode: String,
    val sourceAmount: Double,
    val receivedAmount: Double,
    val exchangeRate: Double,
    val userNote: String,
    val isCrossCurrency: Boolean,
    val isRecordedRate: Boolean
)

object AccountNameResolver {

    private val placeholderNames = setOf("-", "—", "–", "n/a", "none", "null")

    fun currencyShortLabel(currencyCode: String): String {
        return AppCurrency.fromCode(currencyCode).code
    }

    fun defaultAccountName(
        type: AccountType,
        bankOrProvider: String = "",
        accountSubtype: String = "",
        cardNetwork: String = "",
        currencyCode: String = AppCurrency.BDT.code,
        language: AppLanguage = AppLanguage.EN
    ): String {
        val provider = bankOrProvider.trim().takeUnless { isPlaceholder(it) }.orEmpty()
        val subtype = accountSubtype.trim().takeUnless { isPlaceholder(it) }.orEmpty()
        val network = cardNetwork.trim().takeUnless { isPlaceholder(it) }.orEmpty()
        val curLabel = currencyShortLabel(currencyCode)

        return when (type) {
            AccountType.CASH -> {
                if (language.isBangla) {
                    "নগদ টাকা ($curLabel)"
                } else {
                    "Cash ($curLabel)"
                }
            }
            AccountType.MFS -> {
                val baseProvider = provider.ifBlank {
                    if (language.isBangla) "মোবাইল ওয়ালেট" else "Mobile Wallet"
                }
                val suffix = if (language.isBangla) "ওয়ালেট" else "Wallet"
                if (baseProvider.contains("wallet", ignoreCase = true) ||
                    baseProvider.contains("ওয়ালেট") ||
                    baseProvider.contains("ওয়ালেট")
                ) {
                    baseProvider
                } else {
                    "$baseProvider $suffix"
                }
            }
            AccountType.CREDIT_CARD -> {
                val baseProvider = provider.ifBlank {
                    if (language.isBangla) "ব্যাংক" else "Bank"
                }
                val suffix = when {
                    subtype.isNotBlank() && !baseProvider.contains(subtype, ignoreCase = true) -> subtype
                    network.isNotBlank() && !baseProvider.contains(network, ignoreCase = true) ->
                        "$network ${if (language.isBangla) "ক্রেডিট কার্ড" else "Credit Card"}"
                    else -> if (language.isBangla) "ক্রেডিট কার্ড" else "Credit Card"
                }
                "$baseProvider $suffix".trim()
            }
            AccountType.DEBIT_BANK -> {
                val baseProvider = provider.ifBlank {
                    if (language.isBangla) "ব্যাংক অ্যাকাউন্ট" else "Bank Account"
                }
                val suffix = when {
                    subtype.isNotBlank() && !baseProvider.contains(subtype, ignoreCase = true) -> subtype
                    network.isNotBlank() && !baseProvider.contains(network, ignoreCase = true) -> network
                    else -> ""
                }
                if (suffix.isNotBlank()) "$baseProvider $suffix".trim() else baseProvider
            }
        }
    }

    fun resolveAccountName(
        account: AccountEntity,
        language: AppLanguage = AppLanguage.EN
    ): String {
        val trimmed = account.name.trim()
        if (trimmed.isNotBlank() && !isPlaceholder(trimmed)) {
            return trimmed
        }
        return defaultAccountName(
            type = account.type,
            bankOrProvider = account.bankOrProvider,
            accountSubtype = account.accountSubtype,
            cardNetwork = account.cardNetwork,
            currencyCode = account.currencyCode,
            language = language
        )
    }

    fun normalizeAccountForSave(
        account: AccountEntity,
        language: AppLanguage = AppLanguage.EN
    ): AccountEntity {
        val normalizedCurrency = AppCurrency.fromCode(account.currencyCode).code
        val resolvedName = resolveAccountName(
            account.copy(currencyCode = normalizedCurrency),
            language = language
        )
        return account.copy(
            name = resolvedName,
            currencyCode = normalizedCurrency,
            bankOrProvider = account.bankOrProvider.trim(),
            accountSubtype = account.accountSubtype.trim(),
            cardNetwork = account.cardNetwork.trim(),
            accountNumberLast4 = account.accountNumberLast4.trim()
        )
    }

    fun formatTransferEndpoint(
        account: AccountEntity,
        allAccounts: List<AccountEntity>,
        language: AppLanguage = AppLanguage.EN,
        forceDisambiguate: Boolean = false
    ): String {
        val baseName = resolveAccountName(account, language)
        val hasSameNamePeer = allAccounts.any { peer ->
            peer.id != account.id &&
                resolveAccountName(peer, language).equals(baseName, ignoreCase = true)
        }
        if (!hasSameNamePeer && !forceDisambiguate) {
            return baseName
        }
        val curLabel = currencyShortLabel(account.currencyCode)
        val provider = account.bankOrProvider.trim()
            .takeUnless { isPlaceholder(it) || baseName.contains(it, ignoreCase = true) }
            .orEmpty()
        val typeHint = when {
            provider.isNotBlank() -> provider
            account.type == AccountType.CASH -> if (language.isBangla) "নগদ" else "Cash"
            account.accountSubtype.isNotBlank() && !baseName.contains(account.accountSubtype, ignoreCase = true) ->
                account.accountSubtype.trim()
            else -> ""
        }
        val tagParts = buildList {
            if (typeHint.isNotBlank()) add(typeHint)
            if (!baseName.contains(curLabel, ignoreCase = true)) add(curLabel)
        }
        return if (tagParts.isNotEmpty()) {
            "$baseName (${tagParts.joinToString(" • ")})"
        } else {
            baseName
        }
    }

    fun formatTransferRouteTitle(
        fromAccount: AccountEntity,
        toAccount: AccountEntity,
        allAccounts: List<AccountEntity> = listOf(fromAccount, toAccount),
        language: AppLanguage = AppLanguage.EN
    ): String {
        val sameBaseName = resolveAccountName(fromAccount, language)
            .equals(resolveAccountName(toAccount, language), ignoreCase = true)
        val fromLabel = formatTransferEndpoint(
            account = fromAccount,
            allAccounts = allAccounts,
            language = language,
            forceDisambiguate = sameBaseName
        )
        val toLabel = formatTransferEndpoint(
            account = toAccount,
            allAccounts = allAccounts,
            language = language,
            forceDisambiguate = sameBaseName
        )
        return "$fromLabel → $toLabel"
    }

    fun resolveTransferEndpoints(
        tx: TransactionEntity,
        accountsById: Map<Long, AccountEntity>,
        language: AppLanguage = AppLanguage.EN
    ): Pair<String, String> {
        val allAccounts = accountsById.values.toList()
        val fromAcc = accountsById[tx.accountId]
        val toAcc = tx.toAccountId?.let { accountsById[it] }
        val sameBaseName = fromAcc != null && toAcc != null &&
            resolveAccountName(fromAcc, language)
                .equals(resolveAccountName(toAcc, language), ignoreCase = true)

        val titleParts = tx.title
            .split("→", "->")
            .map { it.trim() }
            .filter { it.isNotBlank() && !isPlaceholder(it) }

        val parsedTransfer = TransferNoteCodec.parse(tx, toAcc)
        val fromFallback = titleParts.getOrNull(0)
            ?: defaultWalletFallback(tx.currencyCode, language)
        val toFallback = titleParts.getOrNull(1)
            ?: defaultWalletFallback(parsedTransfer?.destCurrencyCode ?: tx.currencyCode, language)

        val fromLabel = if (fromAcc != null) {
            formatTransferEndpoint(
                account = fromAcc,
                allAccounts = allAccounts,
                language = language,
                forceDisambiguate = sameBaseName
            )
        } else {
            fromFallback
        }

        val toLabel = if (toAcc != null) {
            formatTransferEndpoint(
                account = toAcc,
                allAccounts = allAccounts,
                language = language,
                forceDisambiguate = sameBaseName
            )
        } else {
            toFallback
        }

        return fromLabel to toLabel
    }

    fun resolveTransactionWalletDisplay(
        tx: TransactionEntity,
        accountsById: Map<Long, AccountEntity>,
        language: AppLanguage = AppLanguage.EN
    ): String {
        if (tx.type == TransactionType.TRANSFER) {
            val (fromLabel, toLabel) = resolveTransferEndpoints(tx, accountsById, language)
            return "$fromLabel → $toLabel"
        }
        val account = accountsById[tx.accountId]
        if (account != null) {
            return resolveAccountName(account, language)
        }
        return defaultWalletFallback(tx.currencyCode, language)
    }

    fun defaultWalletFallback(currencyCode: String, language: AppLanguage): String {
        val curLabel = currencyShortLabel(currencyCode)
        return if (language.isBangla) {
            "ওয়ালেট ($curLabel)"
        } else {
            "$curLabel Wallet"
        }
    }

    private fun isPlaceholder(value: String): Boolean {
        return value.trim().lowercase(Locale.US) in placeholderNames
    }
}

object TransferNoteCodec {

    private val recordedRateRegex = Regex(
        pattern = """Rate:\s*1\s+([A-Z]{3})\s*=\s*([0-9]+(?:\.[0-9]+)?)\s+([A-Z]{3})\s*[•|]\s*Received:\s*([0-9]+(?:\.[0-9]+)?)\s+([A-Z]{3})""",
        option = RegexOption.IGNORE_CASE
    )

    private val legacyPairRegex = Regex(
        pattern = """^([A-Z]{3})\s*(?:→|->)\s*([A-Z]{3})$""",
        option = RegexOption.IGNORE_CASE
    )

    fun encode(
        userNote: String,
        sourceCurrencyCode: String,
        destCurrencyCode: String,
        exchangeRate: Double,
        receivedAmount: Double
    ): String {
        val cleanUserNote = stripEncodedSegment(userNote)
        val src = AppCurrency.fromCode(sourceCurrencyCode).code
        val dst = AppCurrency.fromCode(destCurrencyCode).code
        if (src.equals(dst, ignoreCase = true)) {
            return cleanUserNote.ifBlank { "Internal wallet transfer" }
        }
        val safeRate = if (exchangeRate > 0.0 && exchangeRate.isFinite()) exchangeRate else 1.0
        val safeReceived = if (receivedAmount > 0.0 && receivedAmount.isFinite()) receivedAmount else 0.0
        val rateStr = String.format(Locale.US, "%.4f", safeRate)
            .trimEnd('0')
            .trimEnd('.')
            .let { if (!it.contains('.')) "$it.00" else it }
        val recvDecimals = AppCurrency.fromCode(dst).decimalPlaces.coerceIn(2, 3)
        val recvStr = String.format(Locale.US, "%.${recvDecimals}f", safeReceived)
        val metaSegment = "Rate: 1 $src = $rateStr $dst • Received: $recvStr $dst"
        return if (cleanUserNote.isNotBlank()) {
            "$cleanUserNote • $metaSegment"
        } else {
            metaSegment
        }
    }

    fun stripEncodedSegment(note: String): String {
        val trimmed = note.trim()
        if (trimmed.isBlank()) return ""
        val match = recordedRateRegex.find(trimmed) ?: return trimmed
        val before = trimmed.substring(0, match.range.first).trim().trimEnd('•', '|', '-', ' ')
        val after = trimmed.substring(match.range.last + 1).trim().trimStart('•', '|', '-', ' ')
        return listOf(before, after).filter { it.isNotBlank() }.joinToString(" • ")
    }

    fun parse(
        tx: TransactionEntity,
        toAccount: AccountEntity? = null,
        marketSnapshot: MarketSnapshot? = null
    ): ParsedTransferDetail? {
        if (tx.type != TransactionType.TRANSFER) return null

        val srcCode = AppCurrency.fromCode(tx.currencyCode).code
        val rawNote = tx.note.trim()

        val match = recordedRateRegex.find(rawNote)
        if (match != null) {
            val matchedSrc = AppCurrency.fromCode(match.groupValues[1]).code
            val rate = match.groupValues[2].toDoubleOrNull() ?: 1.0
            val matchedDst = AppCurrency.fromCode(match.groupValues[3]).code
            val received = match.groupValues[4].toDoubleOrNull() ?: (tx.amount * rate)
            val cleanUserNote = stripEncodedSegment(rawNote)
            return ParsedTransferDetail(
                sourceCurrencyCode = matchedSrc,
                destCurrencyCode = matchedDst,
                sourceAmount = tx.amount,
                receivedAmount = received,
                exchangeRate = rate,
                userNote = cleanUserNote,
                isCrossCurrency = !matchedSrc.equals(matchedDst, ignoreCase = true),
                isRecordedRate = true
            )
        }

        val legacyMatch = legacyPairRegex.matchEntire(rawNote)
        val inferredDst = when {
            toAccount != null -> AppCurrency.fromCode(toAccount.currencyCode).code
            legacyMatch != null -> AppCurrency.fromCode(legacyMatch.groupValues[2]).code
            else -> srcCode
        }
        val isCross = !srcCode.equals(inferredDst, ignoreCase = true)
        val cleanUserNote = if (legacyMatch != null) "" else rawNote

        if (!isCross) {
            return ParsedTransferDetail(
                sourceCurrencyCode = srcCode,
                destCurrencyCode = inferredDst,
                sourceAmount = tx.amount,
                receivedAmount = tx.amount,
                exchangeRate = 1.0,
                userNote = cleanUserNote,
                isCrossCurrency = false,
                isRecordedRate = false
            )
        }

        val snapshot = marketSnapshot ?: MarketSnapshot.defaultReference()
        val rate = snapshot.convertCurrencyCode(1.0, srcCode, inferredDst)
        val estimatedReceived = tx.amount * rate
        return ParsedTransferDetail(
            sourceCurrencyCode = srcCode,
            destCurrencyCode = inferredDst,
            sourceAmount = tx.amount,
            receivedAmount = estimatedReceived,
            exchangeRate = rate,
            userNote = cleanUserNote,
            isCrossCurrency = true,
            isRecordedRate = false
        )
    }

    fun formatConversionSummary(
        detail: ParsedTransferDetail,
        language: AppLanguage
    ): String {
        if (!detail.isCrossCurrency) return ""
        val receivedFormatted = BdtFormatter.formatCurrency(
            detail.receivedAmount,
            detail.destCurrencyCode,
            language
        )
        val srcLabel = AccountNameResolver.currencyShortLabel(detail.sourceCurrencyCode)
        val rateFormatted = BdtFormatter.formatCurrency(
            detail.exchangeRate,
            detail.destCurrencyCode,
            language
        )
        val oneDigit = if (language.isBangla) "১" else "1"
        val approxPrefix = if (detail.isRecordedRate) "" else "≈ "
        return "→ $approxPrefix$receivedFormatted • $oneDigit $srcLabel = $rateFormatted"
    }

    fun formatConversionAsciiSummary(detail: ParsedTransferDetail): String {
        if (!detail.isCrossCurrency) return ""
        val srcLabel = AccountNameResolver.currencyShortLabel(detail.sourceCurrencyCode)
        val dstLabel = AccountNameResolver.currencyShortLabel(detail.destCurrencyCode)
        val rateStr = String.format(Locale.US, "%.2f", detail.exchangeRate)
        val recvStr = String.format(Locale.US, "%.2f", detail.receivedAmount)
        val approxPrefix = if (detail.isRecordedRate) "" else "~"
        return "-> ${approxPrefix}$dstLabel $recvStr @ 1 $srcLabel = $dstLabel $rateStr"
    }
}

/**
 * Shared balance protection guard ensuring outgoing expenses, transfers, goal contributions,
 * and debt repayments never exceed available balance and balances always stop at 0 (never negative).
 */
object AccountBalanceGuard {
    const val EPSILON: Double = 0.0005

    fun clampNonNegative(value: Double): Double {
        if (!value.isFinite() || value <= EPSILON) return 0.0
        return value
    }

    fun availableBalance(account: AccountEntity): Double = clampNonNegative(account.balance)

    fun isInsufficientForDeduction(account: AccountEntity?, requestedAmount: Double): Boolean {
        if (account == null || requestedAmount <= 0.0 || !requestedAmount.isFinite()) return false
        if (account.type == AccountType.CREDIT_CARD) return false
        return requestedAmount > availableBalance(account) + EPSILON
    }

    fun deductOrZero(currentBalance: Double, amount: Double): Double {
        val available = clampNonNegative(currentBalance)
        return clampNonNegative(available - amount.coerceAtLeast(0.0))
    }

    fun insufficientBalanceMessage(
        account: AccountEntity,
        requestedAmount: Double,
        language: AppLanguage
    ): String {
        val accountLabel = AccountNameResolver.resolveAccountName(account, language)
        val availFormatted = BdtFormatter.formatBalance(account.balance, account.currencyCode, language)
        val reqFormatted = BdtFormatter.formatBalance(requestedAmount, account.currencyCode, language)
        return when (language) {
            AppLanguage.BN ->
                "পর্যাপ্ত ব্যালেন্স নেই! $accountLabel-এ বর্তমান ব্যালেন্স $availFormatted (অনুরোধকৃত: $reqFormatted)।"
            AppLanguage.AR ->
                "الرصيد غير كافٍ! الرصيد المتاح في $accountLabel هو $availFormatted (المطلوب: $reqFormatted)."
            AppLanguage.HI ->
                "पर्याप्त बैलेंस नहीं है! $accountLabel में उपलब्ध बैलेंस $availFormatted है (अनुरोधित: $reqFormatted)।"
            AppLanguage.UR ->
                "ناکافی بیلنس! $accountLabel میں دستیاب بیلنس $availFormatted ہے (درخواست کردہ: $reqFormatted)۔"
            else ->
                "Insufficient balance! Available in $accountLabel: $availFormatted (requested: $reqFormatted)."
        }
    }
}

