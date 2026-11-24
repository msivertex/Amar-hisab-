package com.example.data.ai

import com.example.data.model.AppCurrency
import com.example.localization.AppLanguage

/**
 * Splits long financial summaries into natural sentence-aligned chunks so Android
 * TextToSpeech and built-in audio engines speak the entire summary continuously
 * without dropping utterances or hitting platform input-length limits.
 */
object SpeechTextChunker {

    const val DEFAULT_MAX_CHUNK_LENGTH = 220

    private val sentenceTerminators = setOf('।', '.', '!', '?', '؟', '。', '！', '？')
    private val clauseDelimiters = setOf(';', ',', '،', '—', '–', ':', '•', '、', '，')

    /**
     * Splits [text] into ordered, non-empty, sentence-aligned chunks each targeting
     * at most [maxChunkLength] characters without truncating any words or numbers.
     */
    fun splitIntoChunks(
        text: String,
        maxChunkLength: Int = DEFAULT_MAX_CHUNK_LENGTH
    ): List<String> {
        val cleaned = text.trim()
        if (cleaned.isEmpty()) return emptyList()
        val limit = maxChunkLength.coerceAtLeast(40)

        val rawSentences = mutableListOf<String>()
        val paragraphs = cleaned.split(Regex("\\r?\\n+"))
        for (paragraph in paragraphs) {
            val trimmedParagraph = paragraph.trim()
            if (trimmedParagraph.isEmpty()) continue
            rawSentences.addAll(splitParagraphIntoSentences(trimmedParagraph))
        }

        val bounded = mutableListOf<String>()
        for (sentence in rawSentences) {
            if (sentence.length <= limit) {
                bounded.add(sentence)
            } else {
                bounded.addAll(splitLongSentence(sentence, limit))
            }
        }

        // Merge very short fragments with their neighbor if they stay well within the limit
        val merged = mutableListOf<String>()
        for (chunk in bounded) {
            val normalized = chunk.trim()
            if (normalized.isEmpty()) continue
            if (merged.isNotEmpty() &&
                (normalized.length < 22 || merged.last().length < 22) &&
                merged.last().length + 1 + normalized.length <= limit
            ) {
                merged[merged.lastIndex] = "${merged.last()} $normalized"
            } else {
                merged.add(normalized)
            }
        }
        return merged
    }

    private fun splitParagraphIntoSentences(paragraph: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        val len = paragraph.length
        var i = 0
        while (i < len) {
            val ch = paragraph[i]
            current.append(ch)
            if (ch in sentenceTerminators) {
                val prev = if (i > 0) paragraph[i - 1] else ' '
                val next = if (i + 1 < len) paragraph[i + 1] else ' '
                // Do not split decimal numbers such as 12.50 or ১২.৫০
                val isDecimalDot = ch == '.' && isDigitChar(prev) && isDigitChar(next)
                if (!isDecimalDot) {
                    // Consume any trailing closing quotes/brackets directly after punctuation
                    while (i + 1 < len && paragraph[i + 1] in setOf(')', ']', '"', '\'', '”', '’')) {
                        i++
                        current.append(paragraph[i])
                    }
                    val piece = current.toString().trim()
                    if (piece.isNotEmpty()) {
                        result.add(piece)
                    }
                    current.setLength(0)
                }
            }
            i++
        }
        val tail = current.toString().trim()
        if (tail.isNotEmpty()) {
            result.add(tail)
        }
        return result
    }

    private fun splitLongSentence(sentence: String, limit: Int): List<String> {
        val parts = mutableListOf<String>()
        var remaining = sentence.trim()
        while (remaining.length > limit) {
            val window = remaining.substring(0, limit)
            // 1. Prefer splitting at a natural clause boundary near the end of the window
            var splitIdx = -1
            for (i in window.lastIndex downTo (limit / 3).coerceAtLeast(1)) {
                val ch = window[i]
                if (ch in clauseDelimiters) {
                    val prev = if (i > 0) window[i - 1] else ' '
                    val next = if (i + 1 < window.length) window[i + 1] else ' '
                    // Avoid splitting numeric thousands separators like 50,000 or ৫০,০০০
                    val isNumericComma = ch == ',' && isDigitChar(prev) && isDigitChar(next)
                    if (!isNumericComma) {
                        splitIdx = i + 1
                        break
                    }
                }
            }
            // 2. Otherwise split at the last whitespace boundary
            if (splitIdx <= 0) {
                val spaceIdx = window.lastIndexOf(' ')
                if (spaceIdx > limit / 4) {
                    splitIdx = spaceIdx + 1
                }
            }
            // 3. Hard fallback if there is no whitespace at all
            if (splitIdx <= 0) {
                splitIdx = limit
            }
            val head = remaining.substring(0, splitIdx).trim()
            if (head.isNotEmpty()) {
                parts.add(head)
            }
            remaining = remaining.substring(splitIdx).trim()
        }
        if (remaining.isNotEmpty()) {
            parts.add(remaining)
        }
        return parts
    }

    private fun isDigitChar(ch: Char): Boolean {
        return ch.isDigit() || ch in '০'..'৯' || ch in '٠'..'٩' || ch in '०'..'९'
    }

    /**
     * Converts visual symbols (currency signs, bullets, arrows, percentages) into
     * speech-friendly phrasing for Android TextToSpeech while preserving all numbers
     * and full meaning.
     */
    fun normalizeForSpeech(
        text: String,
        language: AppLanguage,
        currencyCode: String = AppCurrency.BDT.code
    ): String {
        val currency = AppCurrency.fromCode(currencyCode)
        val spokenCurrency = spokenCurrencyName(currency, language)

        var s = text
            .replace("\u00A0", " ")
            .replace("•", ", ")
            .replace("→", if (language == AppLanguage.BN) " থেকে " else " to ")
            .replace("↓", "")
            .replace("↑", "")
            .replace("—", ", ")
            .replace("–", " ")

        // Replace standardized "[ISO Code] [Symbol] [Amount]" or "[Symbol] [Amount]" with natural spoken currency
        val escapedCode = Regex.escape(currency.code)
        val escapedSym = Regex.escape(currency.symbol)
        val numPattern = """([+\-]?[0-9০-৯٠-٩०-९,]+(?:\.[0-9০-৯٠-٩०-९]+)?)"""
        val fullPrefixRegex = Regex("""(?:$escapedCode\s+)?$escapedSym\s*$numPattern""")
        s = fullPrefixRegex.replace(s) { match ->
            "${match.groupValues[1]} $spokenCurrency"
        }
        val codeOnlyRegex = Regex("""\b$escapedCode\s+$numPattern""")
        s = codeOnlyRegex.replace(s) { match ->
            "${match.groupValues[1]} $spokenCurrency"
        }

        // Also handle stray taka symbol if summary mentions BDT explicitly
        if (currency.symbol != "৳") {
            val bdtRegex = Regex("""(?:BDT\s+)?৳\s*([+\-]?[0-9০-৯,]+(?:\.[0-9০-৯]+)?)""")
            val takaWord = if (language == AppLanguage.BN) "টাকা" else "taka"
            s = bdtRegex.replace(s) { match -> "${match.groupValues[1]} $takaWord" }
        }

        // Pronounce percentage smoothly
        val percentWord = when (language) {
            AppLanguage.BN -> " শতাংশ"
            AppLanguage.HI -> " प्रतिशत"
            AppLanguage.UR -> " فیصد"
            AppLanguage.AR -> " بالمئة"
            AppLanguage.ES -> " por ciento"
            AppLanguage.FR -> " pour cent"
            AppLanguage.DE -> " Prozent"
            AppLanguage.IT -> " per cento"
            AppLanguage.PT -> " por cento"
            AppLanguage.TR -> " yüzde"
            AppLanguage.MS, AppLanguage.ID -> " persen"
            AppLanguage.RU -> " процентов"
            else -> " percent"
        }
        s = s.replace("%", percentWord)

        // Collapse duplicate spaces without removing paragraph newlines
        return s.lines()
            .joinToString("\n") { line ->
                line.replace(Regex("\\s{2,}"), " ").trim()
            }
            .trim()
    }

    private fun spokenCurrencyName(currency: AppCurrency, language: AppLanguage): String {
        return when (currency.code.uppercase()) {
            "BDT" -> when (language) {
                AppLanguage.BN -> "টাকা"
                AppLanguage.HI -> "टका"
                AppLanguage.UR -> "ٹکا"
                AppLanguage.AR -> "تاكا"
                else -> "taka"
            }
            "USD" -> when (language) {
                AppLanguage.BN -> "ডলার"
                AppLanguage.HI -> "डॉलर"
                AppLanguage.UR -> "ڈالر"
                AppLanguage.AR -> "دولار"
                else -> "dollars"
            }
            "INR" -> when (language) {
                AppLanguage.BN -> "রুপি"
                AppLanguage.HI -> "रुपये"
                AppLanguage.UR -> "روپے"
                else -> "rupees"
            }
            "SAR" -> when (language) {
                AppLanguage.BN -> "রিয়াল"
                AppLanguage.AR -> "ريال"
                else -> "riyal"
            }
            "AED" -> when (language) {
                AppLanguage.BN -> "দিরহাম"
                AppLanguage.AR -> "درهم"
                else -> "dirham"
            }
            "KWD" -> when (language) {
                AppLanguage.BN -> "কুয়েতি দিনার"
                AppLanguage.AR -> "دينار كويتي"
                else -> "Kuwaiti dinar"
            }
            "GBP" -> if (language == AppLanguage.BN) "পাউন্ড" else "pounds"
            "EUR" -> if (language == AppLanguage.BN) "ইউরো" else "euro"
            "MYR" -> if (language == AppLanguage.BN) "রিঙ্গিত" else "ringgit"
            "PKR" -> when (language) {
                AppLanguage.BN -> "পাকিস্তানি রুপি"
                AppLanguage.UR -> "روپے"
                else -> "rupees"
            }
            else -> if (language == AppLanguage.BN) currency.nameBn else currency.nameEn
        }
    }
}
