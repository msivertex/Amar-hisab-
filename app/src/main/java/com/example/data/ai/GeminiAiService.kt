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

    // 1. Receipt & Document Image Understanding: gemini-4-argon
    private val receiptSchema = Schema.obj(
        properties = mapOf(
            "merchantName" to Schema.string(description = "Store, restaurant, or biller name"),
            "amountBdt" to Schema.double(description = "Total amount in BDT Taka"),
            "category" to Schema.enumeration(
                values = listOf(
                    "FOOD_DINING",
                    "GROCERIES",
                    "RENT_UTILITIES",
                    "TRANSPORT",
                    "SHOPPING",
                    "HEALTH",
                    "ENTERTAINMENT",
                    "OTHER"
                ),
                description = "Best matching expense category"
            ),
            "dateLabel" to Schema.string(description = "Date on receipt or Today"),
            "itemsSummary" to Schema.string(description = "Brief summary of line items purchased")
        )
    )

    private val receiptVisionModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-4-argon",
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

    // 4. Natural Language Voice / Text Expense Parser: gemini-3.8-flash
    private val voiceParseSchema = Schema.obj(
        properties = mapOf(
            "title" to Schema.string(description = "Short merchant or transaction title"),
            "amountBdt" to Schema.double(description = "Amount in Bangladeshi Taka"),
            "type" to Schema.enumeration(
                values = listOf("EXPENSE", "INCOME"),
                description = "Whether this is an expense or income"
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
                description = "Matching category"
            ),
            "accountHint" to Schema.string(description = "Mentioned wallet like bKash, Nagad, Cash, City Bank, or empty")
        )
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
                        "Extract the receipt or cash memo details from this photo for a Bangladeshi personal finance tracker (currency BDT Taka ৳). " +
                            "$langInstruction Return accurate numeric total in amountBdt."
                    )
                }
            )
        }

        val rawJson = response.text
            ?: throw IllegalStateException("Gemini Vision returned an empty response.")
        val json = JSONObject(rawJson)
        val merchant = json.optString("merchantName", "Receipt Expense").ifBlank { "Receipt Expense" }
        val amount = json.optDouble("amountBdt", 0.0).coerceAtLeast(0.0)
        val catName = json.optString("category", "OTHER")
        val category = runCatching { ExpenseCategory.valueOf(catName) }
            .getOrDefault(ExpenseCategory.OTHER)
        val dateLabel = json.optString("dateLabel", "Today")
        val summary = json.optString("itemsSummary", "")

        return ReceiptScanResult(
            merchantName = merchant,
            amountBdt = amount,
            category = category,
            dateLabel = dateLabel,
            itemsSummary = summary
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
        language: AppLanguage
    ): VoiceParsedTransaction {
        val response = withTimeout(45_000L) {
            voiceParserModel.generateContent(
                "Parse this Bangladeshi personal finance entry (Bangla or English) into structured transaction fields. " +
                    "Language preference: ${language.name}. Input: \"$utterance\""
            )
        }

        val rawJson = response.text
            ?: throw IllegalStateException("Voice parser returned an empty response.")
        val json = JSONObject(rawJson)
        val title = json.optString("title", utterance.take(32)).ifBlank { "Quick Entry" }
        val amount = json.optDouble("amountBdt", 0.0).coerceAtLeast(0.0)
        val type = if (json.optString("type") == "INCOME") TransactionType.INCOME else TransactionType.EXPENSE
        val category = runCatching {
            ExpenseCategory.valueOf(json.optString("category", "OTHER"))
        }.getOrDefault(ExpenseCategory.OTHER)
        val accountHint = json.optString("accountHint", "")

        return VoiceParsedTransaction(
            title = title,
            amountBdt = amount,
            type = type,
            category = category,
            accountHint = accountHint
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
