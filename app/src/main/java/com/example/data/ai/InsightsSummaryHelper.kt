package com.example.data.ai

import com.example.data.model.InsightTimeFilter
import com.example.localization.AppLanguage
import com.example.localization.BdtFormatter
import com.example.ui.viewmodel.MainUiState
import java.util.Locale
import kotlin.math.abs

enum class MarketErrorCategory {
    QUOTA_EXCEEDED,
    OFFLINE,
    TIMEOUT,
    GENERAL
}

data class SanitizedAiError(
    val message: String,
    val category: MarketErrorCategory
)

enum class ReceiptScanErrorCategory {
    APP_CHECK_INVALID,
    OFFLINE,
    TIMEOUT,
    QUOTA_EXCEEDED,
    GENERAL
}

data class SanitizedReceiptError(
    val message: String,
    val guidance: String,
    val category: ReceiptScanErrorCategory
)

data class OfflineMarketGuideItem(
    val title: String,
    val value: String,
    val note: String,
    val query: String
)

object InsightsSummaryHelper {

    /**
     * Generates a concise, natural period-aware summary highlighting net cash flow,
     * daily average spend, top spending categories, and period-over-period change.
     */
    fun buildPeriodSummary(state: MainUiState): String {
        val lang = state.prefs.language
        val analytics = state.analytics
        val filter = state.insightFilter
        val displayName = state.prefs.userName.ifBlank {
            if (lang == AppLanguage.BN) "আপনার" else "your"
        }

        val incomeStr = BdtFormatter.format(analytics.totalIncome, lang)
        val expenseStr = BdtFormatter.format(analytics.totalExpense, lang)
        val netFlowAbsStr = BdtFormatter.format(abs(analytics.netFlow), lang)
        val dailyAvgStr = BdtFormatter.format(analytics.dailyAverage, lang)
        val topCategories = analytics.categoryBreakdown.take(2)
        val changePct = analytics.expenseChangePercent

        return if (lang == AppLanguage.BN) {
            val periodPhrase = when (filter) {
                InsightTimeFilter.DAILY -> "আজকের (${analytics.periodLabel})"
                InsightTimeFilter.THIS_WEEK -> "এই সপ্তাহের (${analytics.periodLabel})"
                InsightTimeFilter.THIS_MONTH -> "এই মাসের (${analytics.periodLabel})"
                InsightTimeFilter.LAST_MONTH -> "গত মাসের (${analytics.periodLabel})"
            }
            val greeting = if (state.prefs.userName.isNotBlank()) {
                "হ্যালো ${state.prefs.userName}।"
            } else {
                "আমার হিসাব আর্থিক সারসংক্ষেপ:"
            }
            val cashflowSentence = when {
                analytics.totalIncome == 0.0 && analytics.totalExpense == 0.0 ->
                    "$periodPhrase কোনো লেনদেন এখনো যুক্ত হয়নি। আপনার মোট তরল ব্যালেন্স ${BdtFormatter.format(state.totalLiquidBalance, lang)}।"
                analytics.netFlow >= 0.0 ->
                    "$periodPhrase মোট আয় $incomeStr এবং ব্যয় $expenseStr, ফলে নিট উদ্বৃত্ত রয়েছে $netFlowAbsStr।"
                else ->
                    "$periodPhrase মোট আয় $incomeStr এবং ব্যয় $expenseStr, ফলে নিট ঘাটতি হয়েছে $netFlowAbsStr।"
            }
            val dailyAvgSentence = if (analytics.totalExpense > 0.0) {
                " দৈনিক গড় খরচ $dailyAvgStr।"
            } else {
                ""
            }
            val topCatSentence = when (topCategories.size) {
                0 -> ""
                1 -> {
                    val c1 = topCategories[0]
                    val pct1 = BdtFormatter.formatPercent((c1.share * 100).toInt(), lang)
                    " সর্বোচ্চ খরচ হয়েছে ${c1.category.label(lang)} খাতে ($pct1)।"
                }
                else -> {
                    val c1 = topCategories[0]
                    val c2 = topCategories[1]
                    val pct1 = BdtFormatter.formatPercent((c1.share * 100).toInt(), lang)
                    val pct2 = BdtFormatter.formatPercent((c2.share * 100).toInt(), lang)
                    " শীর্ষ দুটি ব্যয়ের খাত হলো ${c1.category.label(lang)} ($pct1) এবং ${c2.category.label(lang)} ($pct2)।"
                }
            }
            val changeSentence = when {
                changePct == null -> ""
                changePct < 0 -> " পূর্ববর্তী সময়ের তুলনায় ${BdtFormatter.formatPercent(abs(changePct), lang)} কম খরচ হয়েছে।"
                changePct > 0 -> " পূর্ববর্তী সময়ের তুলনায় ${BdtFormatter.formatPercent(changePct, lang)} বেশি খরচ হয়েছে।"
                else -> " খরচ পূর্ববর্তী সময়ের সমান রয়েছে।"
            }
            "$greeting $cashflowSentence$dailyAvgSentence$topCatSentence$changeSentence"
        } else {
            val periodPhrase = when (filter) {
                InsightTimeFilter.DAILY -> "Today (${analytics.periodLabel})"
                InsightTimeFilter.THIS_WEEK -> "This week (${analytics.periodLabel})"
                InsightTimeFilter.THIS_MONTH -> "This month (${analytics.periodLabel})"
                InsightTimeFilter.LAST_MONTH -> "Last month (${analytics.periodLabel})"
            }
            val greeting = if (state.prefs.userName.isNotBlank()) {
                "Hello $displayName."
            } else {
                "Here is your Amar Hisab summary."
            }
            val cashflowSentence = when {
                analytics.totalIncome == 0.0 && analytics.totalExpense == 0.0 ->
                    "No transactions recorded for $periodPhrase yet. Your liquid balance is ${BdtFormatter.format(state.totalLiquidBalance, lang)}."
                analytics.netFlow >= 0.0 ->
                    "For $periodPhrase, total income is $incomeStr and spending is $expenseStr, leaving a net surplus of $netFlowAbsStr."
                else ->
                    "For $periodPhrase, total income is $incomeStr against $expenseStr spent, resulting in a net deficit of $netFlowAbsStr."
            }
            val dailyAvgSentence = if (analytics.totalExpense > 0.0) {
                " Daily average spending is $dailyAvgStr."
            } else {
                ""
            }
            val topCatSentence = when (topCategories.size) {
                0 -> ""
                1 -> {
                    val c1 = topCategories[0]
                    " Your top expense is ${c1.category.label(lang)} (${(c1.share * 100).toInt()}%)."
                }
                else -> {
                    val c1 = topCategories[0]
                    val c2 = topCategories[1]
                    " Top spending categories are ${c1.category.label(lang)} (${(c1.share * 100).toInt()}%) and ${c2.category.label(lang)} (${(c2.share * 100).toInt()}%)."
                }
            }
            val changeSentence = when {
                changePct == null -> ""
                changePct < 0 -> " Spending is down ${abs(changePct)}% compared to the prior period."
                changePct > 0 -> " Spending is up $changePct% compared to the prior period."
                else -> " Spending pace matches the prior period."
            }
            "$greeting $cashflowSentence$dailyAvgSentence$topCatSentence$changeSentence"
        }
    }

    /**
     * Converts raw Gemini / Firebase / network exceptions into calm, polite, user-friendly
     * Bangla or English messages—never exposing raw quota strings, HTTP codes, or URLs.
     */
    fun sanitizeAiError(
        rawMessage: String?,
        throwable: Throwable? = null,
        language: AppLanguage,
        isVoice: Boolean = false
    ): SanitizedAiError {
        val combined = buildString {
            if (!rawMessage.isNullOrBlank()) append(rawMessage).append(' ')
            if (throwable?.message != null) append(throwable.message).append(' ')
            if (throwable?.cause?.message != null) append(throwable.cause?.message)
        }.lowercase(Locale.ROOT)

        val category = when {
            combined.contains("quota") ||
                combined.contains("resource_exhausted") ||
                combined.contains("429") ||
                combined.contains("rate limit") ||
                combined.contains("too many requests") ||
                combined.contains("billing") -> MarketErrorCategory.QUOTA_EXCEEDED

            combined.contains("unable to resolve host") ||
                combined.contains("unknownhost") ||
                combined.contains("network") ||
                combined.contains("offline") ||
                combined.contains("failed to connect") ||
                combined.contains("no address associated") ||
                combined.contains("socket") -> MarketErrorCategory.OFFLINE

            combined.contains("timeout") ||
                combined.contains("timed out") -> MarketErrorCategory.TIMEOUT

            else -> MarketErrorCategory.GENERAL
        }

        val message = if (language == AppLanguage.BN) {
            if (isVoice) {
                when (category) {
                    MarketErrorCategory.QUOTA_EXCEEDED ->
                        "ভয়েস সারসংক্ষেপের দৈনিক সীমা আপাতত পূর্ণ হয়েছে। কিছুক্ষণ পর আবার চেষ্টা করুন—এই সময়ে নিচের টেক্সট সারসংক্ষেপ পড়ে দেখতে পারেন।"
                    MarketErrorCategory.OFFLINE ->
                        "ভয়েস অডিও তৈরি করতে ইন্টারনেট সংযোগ প্রয়োজন। অফলাইনে নিচের টেক্সট সারসংক্ষেপ পড়তে পারবেন।"
                    MarketErrorCategory.TIMEOUT ->
                        "ভয়েস অডিও তৈরি করতে একটু বেশি সময় লাগছে। অনুগ্রহ করে আবার চেষ্টা করুন।"
                    MarketErrorCategory.GENERAL ->
                        "এই মুহূর্তে ভয়েস অডিও চালু করা যায়নি। নিচের টেক্সট সারসংক্ষেপ দেখুন অথবা একটু পর আবার চেষ্টা করুন।"
                }
            } else {
                when (category) {
                    MarketErrorCategory.QUOTA_EXCEEDED ->
                        "লাইভ বাজার সার্চের দৈনিক সীমা আপাতত পূর্ণ হয়েছে। কিছুক্ষণ পর আবার চেষ্টা করুন—নিচে অফলাইন রেফারেন্স গাইড দেওয়া হলো।"
                    MarketErrorCategory.OFFLINE ->
                        "ইন্টারনেট সংযোগ পাওয়া যায়নি। সংযোগ চালু হলে লাইভ রেট দেখতে পারবেন—এর মধ্যে নিচের অফলাইন রেফারেন্স দেখুন।"
                    MarketErrorCategory.TIMEOUT ->
                        "লাইভ তথ্য আনতে একটু দেরি হচ্ছে। অনুগ্রহ করে কিছুক্ষণ পর আবার চেষ্টা করুন।"
                    MarketErrorCategory.GENERAL ->
                        "এই মুহূর্তে লাইভ বাজার তথ্য পাওয়া যায়নি। কিছুক্ষণ পর আবার চেষ্টা করুন অথবা নিচের অফলাইন রেফারেন্স দেখুন।"
                }
            }
        } else {
            if (isVoice) {
                when (category) {
                    MarketErrorCategory.QUOTA_EXCEEDED ->
                        "Daily voice audio capacity is temporarily reached. Please try again shortly—you can read the full text summary below."
                    MarketErrorCategory.OFFLINE ->
                        "Internet connection is needed to synthesize voice audio. You can still read your offline text summary below."
                    MarketErrorCategory.TIMEOUT ->
                        "Voice synthesis took longer than expected. Please try again in a moment."
                    MarketErrorCategory.GENERAL ->
                        "Voice audio is temporarily unavailable. Expand the text summary below or try again shortly."
                }
            } else {
                when (category) {
                    MarketErrorCategory.QUOTA_EXCEEDED ->
                        "Live market search limit is temporarily reached. Please try again shortly or review the offline reference guide below."
                    MarketErrorCategory.OFFLINE ->
                        "You appear to be offline. Live rates will resume once connected—meanwhile, see the offline reference guide below."
                    MarketErrorCategory.TIMEOUT ->
                        "Live lookup took longer than expected. Please try again in a moment."
                    MarketErrorCategory.GENERAL ->
                        "Unable to fetch live market data right now. Please retry shortly or use the offline reference guide below."
                }
            }
        }

        return SanitizedAiError(message = message, category = category)
    }

    /**
     * Converts raw Firebase App Check, Gemini Vision, or network failures during AI receipt
     * scanning into clear, reassuring Bangla or English explanations with immediate recovery steps.
     */
    fun sanitizeReceiptScanError(
        rawMessage: String?,
        throwable: Throwable? = null,
        language: AppLanguage
    ): SanitizedReceiptError {
        val combined = buildString {
            if (!rawMessage.isNullOrBlank()) append(rawMessage).append(' ')
            if (throwable?.message != null) append(throwable.message).append(' ')
            if (throwable?.cause?.message != null) append(throwable.cause?.message)
        }.lowercase(Locale.ROOT)

        val category = when {
            combined.contains("app check") ||
                combined.contains("appcheck") ||
                combined.contains("token is invalid") ||
                combined.contains("attestation") ||
                combined.contains("403") ||
                combined.contains("permission_denied") ||
                combined.contains("unauthenticated") -> ReceiptScanErrorCategory.APP_CHECK_INVALID

            combined.contains("unable to resolve host") ||
                combined.contains("unknownhost") ||
                combined.contains("network") ||
                combined.contains("offline") ||
                combined.contains("failed to connect") ||
                combined.contains("no address associated") ||
                combined.contains("socket") -> ReceiptScanErrorCategory.OFFLINE

            combined.contains("timeout") ||
                combined.contains("timed out") -> ReceiptScanErrorCategory.TIMEOUT

            combined.contains("quota") ||
                combined.contains("resource_exhausted") ||
                combined.contains("429") ||
                combined.contains("rate limit") ||
                combined.contains("billing") -> ReceiptScanErrorCategory.QUOTA_EXCEEDED

            else -> ReceiptScanErrorCategory.GENERAL
        }

        return if (language == AppLanguage.BN) {
            when (category) {
                ReceiptScanErrorCategory.APP_CHECK_INVALID -> SanitizedReceiptError(
                    message = "এই মুহূর্তে এআই রসিদ যাচাইকরণ (App Check) সম্পন্ন করা যায়নি।",
                    guidance = "ইন্টারনেট সংযোগ যাচাই করে আবার চেষ্টা করুন, অথবা অপেক্ষা না করে নিচের বাটনে ট্যাপ করে রসিদ দেখেই এখনই ম্যানুয়ালি হিসাব যোগ করুন।",
                    category = category
                )
                ReceiptScanErrorCategory.OFFLINE -> SanitizedReceiptError(
                    message = "ইন্টারনেট সংযোগ না থাকায় এআই রসিদ স্ক্যান করা যায়নি।",
                    guidance = "ওয়াই-ফাই বা মোবাইল ডাটা চালু করে পুনরায় চেষ্টা করুন, অথবা অফলাইনেই নিচের বাটন থেকে ম্যানুয়ালি হিসাব যোগ করুন।",
                    category = category
                )
                ReceiptScanErrorCategory.TIMEOUT -> SanitizedReceiptError(
                    message = "রসিদটি বিশ্লেষণ করতে নির্ধারিত সময়ের চেয়ে বেশি সময় লেগেছে।",
                    guidance = "পরিষ্কার আলোতে আরেকটি ছবি তুলে আবার চেষ্টা করুন, অথবা নিচের বাটন থেকে দ্রুত ম্যানুয়ালি হিসাব যোগ করুন।",
                    category = category
                )
                ReceiptScanErrorCategory.QUOTA_EXCEEDED -> SanitizedReceiptError(
                    message = "এআই রসিদ স্ক্যানের বর্তমান সীমা সাময়িকভাবে পূর্ণ হয়েছে।",
                    guidance = "কিছুক্ষণ পর আবার চেষ্টা করুন, অথবা এখনই নিচের বাটন থেকে রসিদ দেখে ম্যানুয়ালি হিসাব যোগ করুন।",
                    category = category
                )
                ReceiptScanErrorCategory.GENERAL -> SanitizedReceiptError(
                    message = "রসিদের ছবিটি স্বয়ংক্রিয়ভাবে পড়া যায়নি।",
                    guidance = "আরও স্পষ্ট ছবি তুলে আবার চেষ্টা করুন, অথবা ছবিটি দেখেই নিচের বাটন থেকে সরাসরি ম্যানুয়ালি হিসাব যোগ করুন।",
                    category = category
                )
            }
        } else {
            when (category) {
                ReceiptScanErrorCategory.APP_CHECK_INVALID -> SanitizedReceiptError(
                    message = "AI receipt verification (App Check) could not be completed right now.",
                    guidance = "Check your connection and retry, or tap below to record this receipt manually right away while viewing your photo.",
                    category = category
                )
                ReceiptScanErrorCategory.OFFLINE -> SanitizedReceiptError(
                    message = "No internet connection detected for AI receipt scanning.",
                    guidance = "Reconnect and retry, or tap below to record the receipt manually offline right now.",
                    category = category
                )
                ReceiptScanErrorCategory.TIMEOUT -> SanitizedReceiptError(
                    message = "Receipt scanning took longer than expected.",
                    guidance = "Retry with a clear, well-lit photo, or tap below to enter the receipt details manually.",
                    category = category
                )
                ReceiptScanErrorCategory.QUOTA_EXCEEDED -> SanitizedReceiptError(
                    message = "AI receipt scanning capacity is temporarily reached.",
                    guidance = "Try again shortly, or tap below to log this receipt manually right now.",
                    category = category
                )
                ReceiptScanErrorCategory.GENERAL -> SanitizedReceiptError(
                    message = "Could not read this receipt automatically.",
                    guidance = "Try again with a sharper photo, or tap below to enter the currency, amount, merchant, and date manually.",
                    category = category
                )
            }
        }
    }

    /**
     * Indicative / cached Bangladesh reference benchmarks displayed cleanly alongside
     * polite error/offline notices when live search cannot be reached.
     */
    fun offlineMarketGuide(
        language: AppLanguage,
        snapshot: com.example.data.market.MarketSnapshot = com.example.data.market.MarketSnapshot.defaultReference()
    ): List<OfflineMarketGuideItem> {
        val usdToBdt = snapshot.convert(
            1.0,
            com.example.data.market.MarketCurrency.USD,
            com.example.data.market.MarketCurrency.BDT
        )
        val kwdToBdt = snapshot.convert(
            1.0,
            com.example.data.market.MarketCurrency.KWD,
            com.example.data.market.MarketCurrency.BDT
        )
        val sarToBdt = snapshot.convert(
            1.0,
            com.example.data.market.MarketCurrency.SAR,
            com.example.data.market.MarketCurrency.BDT
        )
        val gold22k = snapshot.gold22kPerVoriBdt
        val usdFmt = com.example.data.market.MarketCalculators.formatDecimal(usdToBdt, 2, language)
        val kwdFmt = com.example.data.market.MarketCalculators.formatDecimal(kwdToBdt, 2, language)
        val sarFmt = com.example.data.market.MarketCalculators.formatDecimal(sarToBdt, 2, language)
        val goldFmt = BdtFormatter.format(gold22k, language)

        return if (language == AppLanguage.BN) {
            listOf(
                OfflineMarketGuideItem(
                    title = "মুদ্রা রেট (১ USD / ১ SAR / ১ KWD)",
                    value = "৳$usdFmt • ৳$sarFmt • ৳$kwdFmt",
                    note = "ব্যাংক ও রেমিট্যান্স চ্যানেল অনুযায়ী পরিবর্তিত হয়",
                    query = "আজকের ইউএস ডলার থেকে টাকা রেট কত?"
                ),
                OfflineMarketGuideItem(
                    title = "২২ ক্যারেট স্বর্ণ (প্রতি ভরি)",
                    value = goldFmt,
                    note = "২১K, ১৮K ও প্রতি গ্রাম হিসাব উপরের টুলে দেখুন",
                    query = "বাংলাদেশে আজকের ২২ ক্যারেট স্বর্ণের ভরি প্রতি দাম কত?"
                ),
                OfflineMarketGuideItem(
                    title = "ব্যাংক FDR ও DPS",
                    value = "৮.৫% – ১০.৫% বার্ষিক",
                    note = "বেসরকারি ও রাষ্ট্রায়ত্ত ব্যাংকের সাধারণ হার",
                    query = "বাংলাদেশের ব্যাংকের বর্তমান এফডিআর ও ডিপিএস রেট"
                ),
                OfflineMarketGuideItem(
                    title = "বিকাশ / নগদ ক্যাশ-আউট",
                    value = "১.২৫% – ১.৮৫%",
                    note = "অ্যাপ, প্রিয় এজেন্ট ও USSD অনুযায়ী ভিন্ন",
                    query = "বিকাশ ও নগদের ক্যাশ-আউট চার্জ কত?"
                ),
                OfflineMarketGuideItem(
                    title = "রেমিট্যান্স সরকারি প্রণোদনা",
                    value = "২.৫% নগদ প্রণোদনা",
                    note = "বৈধ ব্যাংকিং চ্যানেলে প্রেরিত রেমিট্যান্সে",
                    query = "বাংলাদেশে আজকের রেমিট্যান্স ডলার রেট কত?"
                )
            )
        } else {
            listOf(
                OfflineMarketGuideItem(
                    title = "FX Rates (1 USD / 1 SAR / 1 KWD)",
                    value = "৳$usdFmt • ৳$sarFmt • ৳$kwdFmt",
                    note = "Varies by bank & remittance channel",
                    query = "Latest USD to BDT exchange rate today"
                ),
                OfflineMarketGuideItem(
                    title = "22K Gold (Per Vori)",
                    value = goldFmt,
                    note = "Check 21K, 18K & per-gram in the Gold tool above",
                    query = "Today 22K gold price per bhori in Bangladesh BAJUS"
                ),
                OfflineMarketGuideItem(
                    title = "Bank FDR & DPS Rates",
                    value = "8.5% – 10.5% p.a.",
                    note = "Typical private & state bank ranges",
                    query = "Bangladesh bank FDR and DPS interest rates"
                ),
                OfflineMarketGuideItem(
                    title = "bKash / Nagad Cash-out",
                    value = "1.25% – 1.85%",
                    note = "Depends on app, Priyo agent, or USSD",
                    query = "bKash and Nagad cash-out charges in Bangladesh"
                ),
                OfflineMarketGuideItem(
                    title = "Remittance Incentive",
                    value = "2.5% Govt. Incentive",
                    note = "Applicable via legal banking channels",
                    query = "Bangladesh remittance USD exchange rate today"
                )
            )
        }
    }
}
