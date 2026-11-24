package com.example.data.ai

import android.graphics.Bitmap
import android.util.Base64
import com.example.BuildConfig
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

data class TtsAudioPayload(
    val audioBytes: ByteArray,
    val mimeType: String,
    val spokenScript: String
)

/**
 * Direct Gemini REST API service completely decoupled from Firebase AI Logic and Firebase App Check.
 *
 * - Voice Entry and Receipt Scan never require Firebase Auth or Firebase App Check tokens.
 * - When `BuildConfig.GEMINI_API_KEY` is configured via AI Studio Secrets (`.env`), requests go
 *   directly to the Gemini REST API and return structured transaction payloads ready to populate
 *   the app's entry/card state.
 * - When `GEMINI_API_KEY` is blank/unset or the device is offline, calls fail cleanly so
 *   `MainViewModel` seamlessly uses on-device ML Kit OCR (`LocalReceiptTextParser`) and
 *   `LocalVoiceTransactionParser` without any blocking errors.
 */
class GeminiAiService(
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
) {
    companion object {
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"
        private const val MODEL_FLASH = "gemini-2.5-flash"
        private const val MODEL_TTS = "gemini-2.5-flash-preview-tts"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }

    private fun resolveApiKey(): String {
        val key = runCatching { BuildConfig.GEMINI_API_KEY }.getOrDefault("").trim()
        if (key.isBlank() || key.equals("UNSET", ignoreCase = true) || key.equals("null", ignoreCase = true)) {
            throw IllegalStateException("GEMINI_API_KEY_UNSET")
        }
        return key
    }

    fun isCloudApiConfigured(): Boolean = runCatching { resolveApiKey().isNotBlank() }.getOrDefault(false)

    suspend fun analyzeReceiptImage(
        bitmap: Bitmap,
        language: AppLanguage
    ): ReceiptScanResult {
        val apiKey = resolveApiKey()
        val scaledBitmap = scaleBitmapForUpload(bitmap, maxDim = 1440)
        val jpegBase64 = withContext(Dispatchers.IO) {
            val baos = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, baos)
            Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
        }

        val langInstruction = if (language == AppLanguage.BN) {
            "Respond with merchantName and itemsSummary in concise Bangla (or keep English proper store names if printed in English)."
        } else {
            "Respond with merchantName and itemsSummary in concise English."
        }

        val promptText =
            "Analyze this financial receipt, POS slip, bank transfer slip, or mobile wallet screenshot (bKash, Nagad, Rocket, Upay, Kuwait/GCC/International/Bangladesh bank apps) in Bangla or English. " +
                "FOCUS PRIMARILY ON: (1) Exact Final/Grand TOTAL Amount (amountBdt, preserving decimals like 58.50 or 12.250 — prefer Grand Total / Net Total / Total Paid / সর্বমোট over subtotals or item prices) and (2) Currency (currencyCode: KWD for KD/KWD, BDT for ৳/Tk/BDT, SAR, EUR, AED, QAR, MYR, GBP, USD, INR). " +
                "Also classify transactionType carefully: " +
                "INCOME (+) when keywords like Cash In, Received Money, Add Money, Remittance, Deposit, Credit, ক্যাশ ইন, or জমা appear; " +
                "EXPENSE (-) when keywords like Send Money, Payment, Cash Out, Pay Bill, Mobile Recharge, Transfer, SALE, Debit, সেন্ড মানি, পেমেন্ট, or ক্যাশ আউট appear. " +
                "Extract separate fee/charge in feeAmount if shown, merchantName, itemsSummary, providerName (e.g., bKash, Nagad, Rocket, Trust Bank, DBBL, City Bank, KFH, NBK, Al Rajhi), last 4 digits of account/card in accountLast4 if visible, TrxID/reference in referenceId, printed date in dateLabel (YYYY-MM-DD when possible), and detectedActionKeyword. " +
                "Return ONLY valid JSON with keys: merchantName, amountBdt (number), feeAmount (number), currencyCode, transactionType (INCOME or EXPENSE), detectedActionKeyword, providerName, accountLast4, referenceId, category (FOOD_DINING|GROCERIES|RENT_UTILITIES|TRANSPORT|SHOPPING|HEALTH|ENTERTAINMENT|SALARY_INCOME|OTHER), dateLabel, itemsSummary. " +
                langInstruction

        val body = JSONObject().apply {
            put(
                "contents",
                JSONArray().put(
                    JSONObject().apply {
                        put(
                            "parts",
                            JSONArray()
                                .put(
                                    JSONObject().apply {
                                        put(
                                            "inline_data",
                                            JSONObject().apply {
                                                put("mime_type", "image/jpeg")
                                                put("data", jpegBase64)
                                            }
                                        )
                                    }
                                )
                                .put(JSONObject().apply { put("text", promptText) })
                        )
                    }
                )
            )
            put(
                "generationConfig",
                JSONObject().apply {
                    put("responseMimeType", "application/json")
                    put("temperature", 0.1)
                }
            )
        }

        val responseJson = postGenerateContent(MODEL_FLASH, apiKey, body, timeoutMs = 45_000L)
        val rawText = extractFirstText(responseJson)
            ?: throw IllegalStateException("Gemini Vision returned an empty response.")
        val json = parseLenientJsonObject(rawText)

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

    suspend fun parseVoiceOrTextEntry(
        utterance: String,
        language: AppLanguage,
        availableAccountNames: List<String> = emptyList(),
        defaultCurrencyCode: String = "BDT"
    ): VoiceParsedTransaction {
        val apiKey = resolveApiKey()
        val accountsContext = if (availableAccountNames.isNotEmpty()) {
            "User's existing accounts/cards: ${availableAccountNames.joinToString(", ")}. Match accountHint (source) and toAccountHint (destination for transfers) to these names when possible. "
        } else {
            ""
        }

        val prompt =
            "Parse this personal finance voice/text instruction (Bangla, English, or Banglish) into structured transaction JSON. " +
                accountsContext +
                "Default currency if none stated: $defaultCurrencyCode. " +
                "Rules: " +
                "(1) INCOME: Classify incoming money phrases ('ঢুকেছে', 'পেয়েছি', 'জমা হয়েছে', 'received', 'credited', 'income', 'salary', 'cash in', 'deposit') as INCOME (never EXPENSE). " +
                "(2) TRANSFER: Detect TRANSFER for phrases like 'থেকে ... তে পাঠিয়েছি', 'পাঠিয়েছি', 'transfer', 'sent to' including cross-border remittances (e.g., 'আমি কুয়েত থেকে বাংলাদেশের ডাচ বাংলা একাউন্টে ৫০০ দিনার পাঠিয়েছি' => type=TRANSFER, currencyCode=KWD, amountBdt=500, accountHint=Kuwait/KWD account, toAccountHint=Dutch-Bangla Bank). " +
                "(3) CURRENCY: Map 'দিনার'/'dinar'/'KD'/'KWD' to KWD, 'টাকা'/'tk'/'BDT' to BDT, 'রিয়াল'/'SAR' to SAR, 'দিরহাম'/'AED' to AED. " +
                "(4) CATEGORY: Use FOOD_DINING for meals, eating out, lunch, dinner, breakfast, snacks, biryani, restaurant, cafe, or food items; reserve GROCERIES strictly for groceries/raw market shopping ('বাজার', 'কাঁচাবাজার', 'গ্রোসারি', 'groceries', 'supermarket'). " +
                "Return ONLY valid JSON with keys: title, amountBdt (number), currencyCode (3-letter code like BDT/KWD/SAR/USD/EUR), type (EXPENSE|INCOME|TRANSFER), category (FOOD_DINING|GROCERIES|RENT_UTILITIES|TRANSPORT|SHOPPING|HEALTH|ENTERTAINMENT|SALARY_INCOME|TRANSFER|OTHER), accountHint, toAccountHint, note. " +
                "Language preference: ${language.name}. Input: \"$utterance\""

        val body = JSONObject().apply {
            put(
                "contents",
                JSONArray().put(
                    JSONObject().apply {
                        put("parts", JSONArray().put(JSONObject().apply { put("text", prompt) }))
                    }
                )
            )
            put(
                "generationConfig",
                JSONObject().apply {
                    put("responseMimeType", "application/json")
                    put("temperature", 0.1)
                }
            )
        }

        val responseJson = postGenerateContent(MODEL_FLASH, apiKey, body, timeoutMs = 30_000L)
        val rawText = extractFirstText(responseJson)
            ?: throw IllegalStateException("Voice parser returned an empty response.")
        val json = parseLenientJsonObject(rawText)

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

    suspend fun askFinancialSearch(
        query: String,
        language: AppLanguage
    ): SearchGroundingResult {
        val apiKey = resolveApiKey()
        val langPrompt = if (language == AppLanguage.BN) {
            "Answer clearly and concisely in Bengali (বাংলা) with practical numbers for Bangladesh (BDT ৳). "
        } else {
            "Answer clearly and concisely in English with practical figures for Bangladesh (BDT ৳). "
        }

        val body = JSONObject().apply {
            put(
                "contents",
                JSONArray().put(
                    JSONObject().apply {
                        put(
                            "parts",
                            JSONArray().put(
                                JSONObject().apply {
                                    put("text", "$langPrompt Use up-to-date Google Search data to answer: $query")
                                }
                            )
                        )
                    }
                )
            )
            put("tools", JSONArray().put(JSONObject().apply { put("google_search", JSONObject()) }))
        }

        val responseJson = postGenerateContent(MODEL_FLASH, apiKey, body, timeoutMs = 45_000L)
        val answerText = extractFirstText(responseJson)
            ?: throw IllegalStateException("Search-grounded model returned an empty response.")

        val candidate = responseJson.optJSONArray("candidates")?.optJSONObject(0)
        val metadata = candidate?.optJSONObject("groundingMetadata")
        val queriesArray = metadata?.optJSONArray("webSearchQueries")
        val searchQueries = buildList {
            if (queriesArray != null) {
                for (i in 0 until queriesArray.length()) {
                    val q = queriesArray.optString(i, "").trim()
                    if (q.isNotBlank()) add(q)
                }
            }
        }
        val chunksArray = metadata?.optJSONArray("groundingChunks")
        val sources = buildList {
            if (chunksArray != null) {
                for (i in 0 until chunksArray.length()) {
                    val web = chunksArray.optJSONObject(i)?.optJSONObject("web")
                    val uri = web?.optString("uri", "").orEmpty().trim()
                    if (uri.isNotBlank()) {
                        val title = web?.optString("title", "").orEmpty().ifBlank { uri }
                        add(GroundingSource(title = title, uri = uri))
                    }
                }
            }
        }.distinctBy { it.uri }

        return SearchGroundingResult(
            answer = answerText,
            searchQueries = searchQueries,
            sources = sources
        )
    }

    suspend fun generateBriefingTts(
        spokenScript: String
    ): TtsAudioPayload {
        val apiKey = resolveApiKey()
        val body = JSONObject().apply {
            put(
                "contents",
                JSONArray().put(
                    JSONObject().apply {
                        put(
                            "parts",
                            JSONArray().put(
                                JSONObject().apply {
                                    put("text", "Read the following personal finance briefing naturally and warmly:\n$spokenScript")
                                }
                            )
                        )
                    }
                )
            )
            put(
                "generationConfig",
                JSONObject().apply {
                    put("responseModalities", JSONArray().put("AUDIO"))
                    put(
                        "speechConfig",
                        JSONObject().apply {
                            put(
                                "voiceConfig",
                                JSONObject().apply {
                                    put(
                                        "prebuiltVoiceConfig",
                                        JSONObject().apply { put("voiceName", "Kore") }
                                    )
                                }
                            )
                        }
                    )
                }
            )
        }

        val responseJson = postGenerateContent(MODEL_TTS, apiKey, body, timeoutMs = 45_000L)
        val parts = responseJson.optJSONArray("candidates")
            ?.optJSONObject(0)
            ?.optJSONObject("content")
            ?.optJSONArray("parts")

        if (parts != null) {
            for (i in 0 until parts.length()) {
                val inlineData = parts.optJSONObject(i)?.optJSONObject("inlineData")
                    ?: parts.optJSONObject(i)?.optJSONObject("inline_data")
                val mime = inlineData?.optString("mimeType", "").orEmpty()
                    .ifBlank { inlineData?.optString("mime_type", "").orEmpty() }
                val b64 = inlineData?.optString("data", "").orEmpty()
                if (mime.startsWith("audio/") && b64.isNotBlank()) {
                    val bytes = Base64.decode(b64, Base64.DEFAULT)
                    return TtsAudioPayload(
                        audioBytes = bytes,
                        mimeType = mime,
                        spokenScript = spokenScript
                    )
                }
            }
        }
        throw IllegalStateException("TTS model did not return audio data.")
    }

    suspend fun askCustomerSupport(
        userMessage: String,
        recentHistory: List<SupportChatMessage>,
        hasAttemptedTroubleshooting: Boolean
    ): SupportAiReply {
        val apiKey = resolveApiKey()
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
            append("নির্দেশনা: 'আমার হিসাব' অ্যাপের সঠিক ধাপসহ পরিষ্কার ও সহায়ক বাংলা উত্তর JSON আকারে দিন (reply, needsHumanEscalation, suggestedFollowUps)।")
        }

        val body = JSONObject().apply {
            put(
                "systemInstruction",
                JSONObject().apply {
                    put(
                        "parts",
                        JSONArray().put(
                            JSONObject().apply {
                                put("text", CustomerSupportEngine.AMAR_HISAB_SYSTEM_KNOWLEDGE)
                            }
                        )
                    )
                }
            )
            put(
                "contents",
                JSONArray().put(
                    JSONObject().apply {
                        put("parts", JSONArray().put(JSONObject().apply { put("text", prompt) }))
                    }
                )
            )
            put(
                "generationConfig",
                JSONObject().apply {
                    put("responseMimeType", "application/json")
                    put("temperature", 0.2)
                }
            )
        }

        val responseJson = postGenerateContent(MODEL_FLASH, apiKey, body, timeoutMs = 35_000L)
        val rawText = extractFirstText(responseJson)
            ?: throw IllegalStateException("Support model returned an empty response.")
        val json = parseLenientJsonObject(rawText)
        val rawReply = json.optString("reply", "").trim()
        if (rawReply.isBlank()) {
            throw IllegalStateException("Support model returned blank reply.")
        }
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

    private suspend fun postGenerateContent(
        model: String,
        apiKey: String,
        payload: JSONObject,
        timeoutMs: Long
    ): JSONObject = withTimeout(timeoutMs) {
        withContext(Dispatchers.IO) {
            val url = "$BASE_URL/$model:generateContent?key=$apiKey"
            val request = Request.Builder()
                .url(url)
                .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            httpClient.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    throw IllegalStateException("Gemini API HTTP ${response.code}: ${responseBody.take(240)}")
                }
                JSONObject(responseBody)
            }
        }
    }

    private fun extractFirstText(responseJson: JSONObject): String? {
        val parts = responseJson.optJSONArray("candidates")
            ?.optJSONObject(0)
            ?.optJSONObject("content")
            ?.optJSONArray("parts")
            ?: return null
        val sb = StringBuilder()
        for (i in 0 until parts.length()) {
            val text = parts.optJSONObject(i)?.optString("text", "").orEmpty()
            if (text.isNotBlank()) sb.append(text)
        }
        return sb.toString().trim().takeIf { it.isNotBlank() }
    }

    private fun parseLenientJsonObject(raw: String): JSONObject {
        val cleaned = raw.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
        val startIdx = cleaned.indexOf('{')
        val endIdx = cleaned.lastIndexOf('}')
        val jsonSlice = if (startIdx >= 0 && endIdx > startIdx) {
            cleaned.substring(startIdx, endIdx + 1)
        } else {
            cleaned
        }
        return JSONObject(jsonSlice)
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
