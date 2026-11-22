package com.example.data.ai

import android.graphics.Bitmap
import com.example.data.model.ExpenseCategory
import com.example.data.model.GroundingSource
import com.example.data.model.ReceiptScanResult
import com.example.data.model.SearchGroundingResult
import com.example.data.model.TransactionType
import com.example.data.model.VoiceParsedTransaction
import com.example.data.support.CustomerSupportEngine
import com.example.data.support.SupportAiReply
import com.example.data.support.SupportChatMessage
import com.example.data.support.SupportSender
import com.example.localization.AppLanguage
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.InlineDataPart
import com.google.firebase.ai.type.PublicPreviewAPI
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.SpeechConfig
import com.google.firebase.ai.type.Tool
import com.google.firebase.ai.type.Voice
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

data class TtsAudioPayload(
    val audioBytes: ByteArray,
    val mimeType: String,
    val spokenScript: String
)

@OptIn(PublicPreviewAPI::class)
class GeminiAiService {

    // 1. Receipt & Voucher Image Understanding: gemini-3.8-flash
    private val receiptSchema = Schema.obj(
        properties = mapOf(
            "merchantName" to Schema.string(
                description = "Store, merchant, recipient, sender, or biller name on the receipt or transfer screenshot"
            ),
            "amountBdt" to Schema.double(
                description = "Exact primary transaction amount preserving decimals (e.g. 58.50 or 12.250), excluding separate fee/charge if listed separately"
            ),
            "feeAmount" to Schema.double(
                description = "Visible transaction fee, charge, or service fee if printed separately, otherwise 0"
            ),
            "currencyCode" to Schema.string(
                description = "3-letter currency code: BDT, KWD, SAR, AED, QAR, MYR, EUR, GBP, USD, or INR (map KD to KWD, Tk/৳ to BDT)"
            ),
            "transactionType" to Schema.enumeration(
                values = listOf("INCOME", "EXPENSE"),
                description = "INCOME (Money In) for Cash In, Received Money, Add Money, Remittance, Deposit, Credit, ক্যাশ ইন, জমা; EXPENSE (Money Out) for Send Money, Payment, Cash Out, Pay Bill, Mobile Recharge, Transfer, SALE, Debit, সেন্ড মানি, পেমেন্ট, ক্যাশ আউট"
            ),
            "detectedActionKeyword" to Schema.string(
                description = "Exact transaction type label printed on the slip/screenshot (e.g. Cash In, Received Money, Add Money, Remittance, Send Money, Cash Out, Payment, Pay Bill, Mobile Recharge, SALE, Bank Transfer, ক্যাশ ইন, সেন্ড মানি)"
            ),
            "providerName" to Schema.string(
                description = "Detected MFS wallet or bank/card provider (e.g. bKash, Nagad, Rocket, Upay, City Bank, DBBL, BRAC Bank, Islami Bank, KFH, NBK, Al Rajhi, stc pay, ENBD, or empty if unknown)"
            ),
            "accountLast4" to Schema.string(
                description = "Last 4 digits of the user's card, bank account, or wallet if visible on the slip, otherwise empty string"
            ),
            "referenceId" to Schema.string(
                description = "Transaction ID (TrxID), reference number, or approval code if visible, otherwise empty string"
            ),
            "category" to Schema.enumeration(
                values = listOf(
                    "FOOD_DINING",
                    "GROCERIES",
                    "RENT_UTILITIES",
                    "TRANSPORT",
                    "SHOPPING",
                    "HEALTH",
                    "ENTERTAINMENT",
                    "SALARY_INCOME",
                    "OTHER"
                ),
                description = "Best matching category (use SALARY_INCOME for salary, remittance, or incoming transfers when INCOME)"
            ),
            "dateLabel" to Schema.string(
                description = "Printed transaction date/time preferably in YYYY-MM-DD or DD MMM YYYY format, or Today"
            ),
            "itemsSummary" to Schema.string(
                description = "Brief summary of items purchased, counterparty, or transfer purpose"
            )
        ),
        optionalProperties = listOf(
            "feeAmount",
            "currencyCode",
            "detectedActionKeyword",
            "providerName",
            "accountLast4",
            "referenceId",
            "itemsSummary"
        )
    )

    private val receiptVisionModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = receiptSchema
            }
        )
    }

    // 2. Live Financial & Market Search Grounding: gemini-3.8-flash with googleSearch tool
    private val searchGroundedModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            tools = listOf(Tool.googleSearch())
        )
    }

    // 3. Text-to-Speech Briefing Synthesis: gemini-3.1-flash-tts-preview
    private val ttsModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.1-flash-tts-preview",
            generationConfig = generationConfig {
                responseModalities = listOf(ResponseModality.AUDIO)
                speechConfig = SpeechConfig(
                    voice = Voice("Kore")
                )
            }
        )
    }

    // 4. Natural Language Voice / Text Entry Parser (Expense, Income & Account Transfers): gemini-3.8-flash
    private val voiceParseSchema = Schema.obj(
        properties = mapOf(
            "title" to Schema.string(description = "Short merchant, purpose, or transfer title (e.g. 'Groceries', 'Trust Bank -> Dutch-Bangla Bank')"),
            "amountBdt" to Schema.double(description = "Exact numeric transaction amount preserving decimals (e.g. 5000, 1500, 12.500)"),
            "currencyCode" to Schema.string(description = "3-letter currency code: BDT, KWD, SAR, AED, QAR, MYR, EUR, GBP, USD, or INR (map KD to KWD, Tk/৳/টাকা to BDT)"),
            "type" to Schema.enumeration(
                values = listOf("EXPENSE", "INCOME", "TRANSFER"),
                description = "TRANSFER when moving money between two accounts/banks/wallets (e.g., Trust to Dutch-Bangla); INCOME for salary/received/cash in/deposit; EXPENSE for purchases/cash-outs/bills/groceries"
            ),
            "category" to Schema.enumeration(
                values = listOf(
                    "FOOD_DINING",
                    "GROCERIES",
                    "RENT_UTILITIES",
                    "TRANSPORT",
                    "SHOPPING",
                    "HEALTH",
                    "ENTERTAINMENT",
                    "SALARY_INCOME",
                    "TRANSFER",
                    "OTHER"
                ),
                description = "Best matching category (TRANSFER for account-to-account transfers, SALARY_INCOME for income)"
            ),
            "accountHint" to Schema.string(description = "Primary or source wallet/bank card mentioned (e.g., Trust Bank, bKash, Nagad, KFH, Cash, or empty)"),
            "toAccountHint" to Schema.string(description = "Destination wallet/bank card ONLY when type is TRANSFER (e.g., Dutch-Bangla Bank, bKash), otherwise empty string"),
            "note" to Schema.string(description = "Brief optional memo or original purpose if helpful")
        ),
        optionalProperties = listOf("currencyCode", "toAccountHint", "note")
    )

    private val voiceParserModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = voiceParseSchema
            }
        )
    }

    // 5. Customer Support Assistant: gemini-3.8-flash with structured JSON & full app knowledge
    private val customerSupportSchema = Schema.obj(
        properties = mapOf(
            "reply" to Schema.string(
                description = "Clear, helpful, step-by-step customer support answer in polite Bangla without including phone/WhatsApp numbers"
            ),
            "needsHumanEscalation" to Schema.boolean(
                description = "True ONLY for critical/emergency issues or when user explicitly asks for human support after prior troubleshooting"
            ),
            "suggestedFollowUps" to Schema.array(
                items = Schema.string(),
                description = "Up to 2 short Bangla follow-up quick-reply labels"
            )
        ),
        optionalProperties = listOf("suggestedFollowUps")
    )

    private val customerSupportModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = customerSupportSchema
            },
            systemInstruction = content {
                text(CustomerSupportEngine.AMAR_HISAB_SYSTEM_KNOWLEDGE)
            }
        )
    }

    suspend fun analyzeReceiptImage(
        bitmap: Bitmap,
        language: AppLanguage
    ): ReceiptScanResult {
        val scaledBitmap = scaleBitmapForUpload(bitmap, maxDim = 1440)
        val langInstruction = if (language == AppLanguage.BN) {
            "Respond with merchantName and itemsSummary in concise Bangla (or keep English proper store names if printed in English)."
        } else {
            "Respond with merchantName and itemsSummary in concise English."
        }

        val response = withTimeout(60_000L) {
            receiptVisionModel.generateContent(
                content {
                    image(scaledBitmap)
                    text(
                        "Analyze this financial receipt, POS slip, bank transfer slip, or mobile wallet screenshot (bKash, Nagad, Rocket, Upay, Kuwait/GCC/International/Bangladesh bank apps) in Bangla or English. " +
                            "FOCUS PRIMARILY ON: (1) Exact Final/Grand TOTAL Amount (amountBdt, preserving decimals like 58.50 or 12.250 — prefer Grand Total / Net Total / Total Paid / সর্বমোট over subtotals or item prices) and (2) Currency (currencyCode: KWD for KD/KWD, BDT for ৳/Tk/BDT, SAR, EUR, AED, QAR, MYR, GBP, USD, INR). " +
                            "Also classify transactionType carefully: " +
                            "INCOME (+) when keywords like Cash In, Received Money, Add Money, Remittance, Deposit, Credit, ক্যাশ ইন, or জমা appear; " +
                            "EXPENSE (-) when keywords like Send Money, Payment, Cash Out, Pay Bill, Mobile Recharge, Transfer, SALE, Debit, সেন্ড মানি, পেমেন্ট, or ক্যাশ আউট appear. " +
                            "Extract separate fee/charge in feeAmount if shown, merchantName, itemsSummary, providerName (e.g., bKash, Nagad, Rocket, Trust Bank, DBBL, City Bank, KFH, NBK, Al Rajhi), last 4 digits of account/card in accountLast4 if visible, TrxID/reference in referenceId, printed date in dateLabel (YYYY-MM-DD when possible), and detectedActionKeyword. " +
                            langInstruction
                    )
                }
            )
        }

        val rawJson = response.text
            ?: throw IllegalStateException("Gemini Vision returned an empty response.")
        val json = JSONObject(rawJson)
        val merchant = json.optString("merchantName", "Receipt Transaction").ifBlank { "Receipt Transaction" }
        val amount = json.optDouble("amountBdt", 0.0).let { if (it.isNaN()) 0.0 else it.coerceAtLeast(0.0) }
        val fee = json.optDouble("feeAmount", 0.0).let { if (it.isNaN()) 0.0 else it.coerceAtLeast(0.0) }
        val rawCurrency = json.optString("currencyCode", "BDT")
        val normalizedCurrency = ReceiptVoucherMatcher.normalizeCurrencyCode(rawCurrency)
        val detectedKeyword = json.optString("detectedActionKeyword", "").trim()
        val providerHint = json.optString("providerName", "").trim()
        val last4Hint = json.optString("accountLast4", "").trim()
        val referenceId = json.optString("referenceId", "").trim()
        val dateLabel = json.optString("dateLabel", "Today").ifBlank { "Today" }
        val summary = json.optString("itemsSummary", "").trim()

        val txType = ReceiptVoucherMatcher.classifyTransactionDirection(
            detectedKeyword = detectedKeyword,
            merchantName = merchant,
            itemsSummary = summary,
            modelTypeRaw = json.optString("transactionType", "EXPENSE")
        )
        val catName = json.optString("category", "OTHER")
        val parsedCategory = runCatching { ExpenseCategory.valueOf(catName) }
            .getOrDefault(if (txType == TransactionType.INCOME) ExpenseCategory.SALARY_INCOME else ExpenseCategory.OTHER)
        val finalCategory = if (txType == TransactionType.INCOME && parsedCategory == ExpenseCategory.OTHER) {
            ExpenseCategory.SALARY_INCOME
        } else {
            parsedCategory
        }
        val parsedTimestamp = ReceiptVoucherMatcher.parseReceiptTimestamp(dateLabel)

        return ReceiptScanResult(
            merchantName = merchant,
            amountBdt = amount,
            category = finalCategory,
            dateLabel = dateLabel,
            itemsSummary = summary,
            transactionType = txType,
            feeAmount = fee,
            currencyCode = normalizedCurrency,
            providerHint = providerHint,
            accountLast4Hint = last4Hint,
            referenceId = referenceId,
            timestampMillis = parsedTimestamp,
            detectedDirectionKeyword = detectedKeyword
        )
    }

    suspend fun askFinancialSearch(
        query: String,
        language: AppLanguage
    ): SearchGroundingResult {
        val langPrompt = if (language == AppLanguage.BN) {
            "Answer clearly and concisely in Bengali (বাংলা) with practical numbers for Bangladesh (BDT ৳). "
        } else {
            "Answer clearly and concisely in English with practical figures for Bangladesh (BDT ৳). "
        }

        val response = withTimeout(60_000L) {
            searchGroundedModel.generateContent(
                "$langPrompt Use up-to-date Google Search data to answer: $query"
            )
        }

        val answerText = response.text
            ?: throw IllegalStateException("Search-grounded model returned an empty response.")

        val metadata = response.candidates.firstOrNull()?.groundingMetadata
        val searchQueries = metadata?.webSearchQueries.orEmpty()
        val sources = metadata?.groundingChunks
            .orEmpty()
            .mapNotNull { chunk ->
                val web = chunk.web
                if (web != null && !web.uri.isNullOrBlank()) {
                    GroundingSource(
                        title = web.title?.ifBlank { web.uri } ?: "Web Source",
                        uri = web.uri ?: ""
                    )
                } else null
            }
            .distinctBy { it.uri }

        return SearchGroundingResult(
            answer = answerText,
            searchQueries = searchQueries,
            sources = sources
        )
    }

    suspend fun generateBriefingTts(
        spokenScript: String
    ): TtsAudioPayload {
        val response = withTimeout(60_000L) {
            ttsModel.generateContent(
                "Read the following personal finance briefing naturally and warmly:\n$spokenScript"
            )
        }

        val parts = response.candidates.firstOrNull()?.content?.parts.orEmpty()
        val audioPart = parts.filterIsInstance<InlineDataPart>().firstOrNull {
            it.mimeType.startsWith("audio/")
        } ?: throw IllegalStateException("TTS model did not return audio data.")

        return TtsAudioPayload(
            audioBytes = audioPart.inlineData,
            mimeType = audioPart.mimeType,
            spokenScript = spokenScript
        )
    }

    suspend fun parseVoiceOrTextEntry(
        utterance: String,
        language: AppLanguage,
        availableAccountNames: List<String> = emptyList(),
        defaultCurrencyCode: String = "BDT"
    ): VoiceParsedTransaction {
        val accountsContext = if (availableAccountNames.isNotEmpty()) {
            "User's existing accounts/cards: ${availableAccountNames.joinToString(", ")}. Match accountHint (source) and toAccountHint (destination for transfers) to these names when possible. "
        } else {
            ""
        }
        val response = withTimeout(45_000L) {
            voiceParserModel.generateContent(
                "Parse this personal finance voice/text instruction (Bangla, English, or Banglish) into structured transaction fields. " +
                    accountsContext +
                    "Default currency if none stated: $defaultCurrencyCode. " +
                    "Detect TRANSFER when transferring between two accounts (e.g., 'Trust to Dutch-Bangla 5000', 'Trust থেকে Dutch-Bangla তে ৫০০০ টাকা পাঠিয়েছি'). " +
                    "Detect EXPENSE for spending/groceries/bills/cash-out (e.g., 'bKash থেকে বাজার ১৫০০ টাকা'). " +
                    "Detect INCOME for salary/received money/cash in. " +
                    "Language preference: ${language.name}. Input: \"$utterance\""
            )
        }

        val rawJson = response.text
            ?: throw IllegalStateException("Voice parser returned an empty response.")
        val json = JSONObject(rawJson)
        val title = json.optString("title", utterance.take(36)).ifBlank { "Quick Entry" }
        val amount = json.optDouble("amountBdt", 0.0).let { if (it.isNaN()) 0.0 else it.coerceAtLeast(0.0) }
        val type = when (json.optString("type").trim().uppercase()) {
            "INCOME" -> TransactionType.INCOME
            "TRANSFER" -> TransactionType.TRANSFER
            else -> TransactionType.EXPENSE
        }
        val rawCurrency = json.optString("currencyCode", defaultCurrencyCode)
        val currencyCode = ReceiptVoucherMatcher.normalizeCurrencyCode(rawCurrency, defaultCurrencyCode)
        val category = runCatching {
            ExpenseCategory.valueOf(json.optString("category", "OTHER"))
        }.getOrDefault(
            when (type) {
                TransactionType.TRANSFER -> ExpenseCategory.TRANSFER
                TransactionType.INCOME -> ExpenseCategory.SALARY_INCOME
                TransactionType.EXPENSE -> ExpenseCategory.OTHER
            }
        )
        val accountHint = json.optString("accountHint", "").trim()
        val toAccountHint = json.optString("toAccountHint", "").trim()
        val note = json.optString("note", "").trim()

        return VoiceParsedTransaction(
            title = title,
            amountBdt = amount,
            type = type,
            category = category,
            accountHint = accountHint,
            currencyCode = currencyCode,
            toAccountHint = toAccountHint,
            note = note,
            isLocalFallback = false
        )
    }

    suspend fun askCustomerSupport(
        userMessage: String,
        recentHistory: List<SupportChatMessage>,
        hasAttemptedTroubleshooting: Boolean
    ): SupportAiReply {
        val historySummary = recentHistory
            .takeLast(6)
            .joinToString("\n") { msg ->
                val role = if (msg.sender == SupportSender.USER) "ব্যবহারকারী" else "সাপোর্ট"
                "$role: ${msg.text.take(220)}"
            }

        val prompt = buildString {
            appendLine("পূর্ববর্তী চ্যাট প্রসঙ্গ:")
            appendLine(historySummary.ifBlank { "(নতুন কথোপকথন)" })
            appendLine("ইতিমধ্যে ট্রাবলশুটিং চেষ্টা হয়েছে কিনা: $hasAttemptedTroubleshooting")
            appendLine("ব্যবহারকারীর বর্তমান বার্তা: $userMessage")
            append("নির্দেশনা: 'আমার হিসাব' অ্যাপের সঠিক ধাপসহ পরিষ্কার ও সহায়ক বাংলা উত্তর দিন।")
        }

        val response = withTimeout(45_000L) {
            customerSupportModel.generateContent(prompt)
        }

        val rawJson = response.text
            ?: throw IllegalStateException("Support model returned an empty response.")
        val json = JSONObject(rawJson)
        val rawReply = json.optString("reply", "").trim()
        if (rawReply.isBlank()) {
            throw IllegalStateException("Support model returned blank reply.")
        }
        // Guardrail: never leak phone number inline from LLM output; UI card handles controlled escalation
        val sanitizedReply = rawReply
            .replace("+8801708901947", "")
            .replace("01708901947", "")
            .trim()
        val aiEscalation = json.optBoolean("needsHumanEscalation", false)
        val followUpsArray = json.optJSONArray("suggestedFollowUps")
        val followUps = buildList {
            if (followUpsArray != null) {
                for (i in 0 until minOf(followUpsArray.length(), 2)) {
                    val label = followUpsArray.optString(i, "").trim()
                    if (label.isNotBlank()) add(label)
                }
            }
        }

        return SupportAiReply(
            replyText = sanitizedReply,
            needsHumanEscalation = aiEscalation,
            suggestedFollowUps = followUps
        )
    }

    private fun scaleBitmapForUpload(bitmap: Bitmap, maxDim: Int): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        if (w <= maxDim && h <= maxDim) return bitmap
        val ratio = minOf(maxDim.toFloat() / w, maxDim.toFloat() / h)
        val targetW = (w * ratio).toInt().coerceAtLeast(1)
        val targetH = (h * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
    }
}
