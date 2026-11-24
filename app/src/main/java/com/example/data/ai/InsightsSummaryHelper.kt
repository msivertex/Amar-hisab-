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

    private data class IncomeSourceItem(
        val label: String,
        val amount: Double,
        val sharePercent: Int
    )

    /**
     * Generates a complete, detailed, period-aware financial summary suitable for both
     * full-length continuous voice playback and comfortable read-along review.
     *
     * Always covers: greeting, active period, total income, total expense, net balance/savings,
     * daily average & peak, top spending categories, top income categories, and detailed
     * financial breakdown/discussion (trend vs prior period, liquid balance, goals/dues, advice).
     */
    fun buildPeriodSummary(state: MainUiState): String {
        val lang = state.prefs.language
        val analytics = state.analytics
        val filter = state.insightFilter
        val currencyCode = state.prefs.mainCurrencyCode.ifBlank { "BDT" }
        val userName = state.prefs.userName.trim()

        val incomeStr = BdtFormatter.formatCurrency(analytics.totalIncome, currencyCode, lang)
        val expenseStr = BdtFormatter.formatCurrency(analytics.totalExpense, currencyCode, lang)
        val netFlowAbsStr = BdtFormatter.formatCurrency(abs(analytics.netFlow), currencyCode, lang)
        val dailyAvgStr = BdtFormatter.formatCurrency(analytics.dailyAverage, currencyCode, lang)
        val peakStr = BdtFormatter.formatCurrency(analytics.peakAmount, currencyCode, lang)
        val liquidBalanceStr = BdtFormatter.formatCurrency(state.totalLiquidBalance, currencyCode, lang)

        val topExpenses = analytics.categoryBreakdown.take(3)
        val topIncomes = resolveTopIncomeSources(state, lang)
        val changePct = analytics.expenseChangePercent
        val savingsRate = analytics.savingsRatePercent ?: if (analytics.totalIncome > 0.0) {
            (((analytics.totalIncome - analytics.totalExpense) / analytics.totalIncome) * 100.0).toInt().coerceIn(-999, 100)
        } else {
            null
        }
        val activeGoals = state.goals.filter { it.targetAmount > 0.0 }
        val openLoans = state.loans.filter { !it.isSettled && it.remainingAmount > 0.0 }
        val totalOwedByMe = openLoans
            .filter { it.loanType == com.example.data.model.LoanType.I_OWE }
            .sumOf { it.remainingAmount }
        val totalOwedToMe = openLoans
            .filter { it.loanType == com.example.data.model.LoanType.OWED_TO_ME }
            .sumOf { it.remainingAmount }
        val accountCount = state.accounts.size

        return when (lang) {
            AppLanguage.BN -> buildBanglaDetailedSummary(
                userName = userName,
                filter = filter,
                periodLabel = analytics.periodLabel,
                totalIncome = analytics.totalIncome,
                totalExpense = analytics.totalExpense,
                netFlow = analytics.netFlow,
                transactionCount = analytics.transactionCount,
                incomeStr = incomeStr,
                expenseStr = expenseStr,
                netFlowAbsStr = netFlowAbsStr,
                dailyAvgStr = dailyAvgStr,
                peakAmount = analytics.peakAmount,
                peakStr = peakStr,
                liquidBalanceStr = liquidBalanceStr,
                savingsRate = savingsRate,
                changePct = changePct,
                topExpenses = topExpenses,
                topIncomes = topIncomes,
                accountCount = accountCount,
                activeGoalsCount = activeGoals.size,
                totalOwedByMe = totalOwedByMe,
                totalOwedToMe = totalOwedToMe,
                currencyCode = currencyCode
            )
            AppLanguage.AR -> buildArabicDetailedSummary(
                userName = userName,
                filter = filter,
                periodLabel = analytics.periodLabel,
                totalIncome = analytics.totalIncome,
                totalExpense = analytics.totalExpense,
                netFlow = analytics.netFlow,
                incomeStr = incomeStr,
                expenseStr = expenseStr,
                netFlowAbsStr = netFlowAbsStr,
                dailyAvgStr = dailyAvgStr,
                liquidBalanceStr = liquidBalanceStr,
                savingsRate = savingsRate,
                changePct = changePct,
                topExpenses = topExpenses,
                topIncomes = topIncomes,
                currencyCode = currencyCode,
                lang = lang
            )
            AppLanguage.HI -> buildHindiDetailedSummary(
                userName = userName,
                filter = filter,
                periodLabel = analytics.periodLabel,
                totalIncome = analytics.totalIncome,
                totalExpense = analytics.totalExpense,
                netFlow = analytics.netFlow,
                incomeStr = incomeStr,
                expenseStr = expenseStr,
                netFlowAbsStr = netFlowAbsStr,
                dailyAvgStr = dailyAvgStr,
                liquidBalanceStr = liquidBalanceStr,
                savingsRate = savingsRate,
                changePct = changePct,
                topExpenses = topExpenses,
                topIncomes = topIncomes,
                currencyCode = currencyCode,
                lang = lang
            )
            AppLanguage.UR -> buildUrduDetailedSummary(
                userName = userName,
                filter = filter,
                periodLabel = analytics.periodLabel,
                totalIncome = analytics.totalIncome,
                totalExpense = analytics.totalExpense,
                netFlow = analytics.netFlow,
                incomeStr = incomeStr,
                expenseStr = expenseStr,
                netFlowAbsStr = netFlowAbsStr,
                dailyAvgStr = dailyAvgStr,
                liquidBalanceStr = liquidBalanceStr,
                savingsRate = savingsRate,
                changePct = changePct,
                topExpenses = topExpenses,
                topIncomes = topIncomes,
                currencyCode = currencyCode,
                lang = lang
            )
            else -> buildEnglishDetailedSummary(
                lang = lang,
                userName = userName,
                filter = filter,
                periodLabel = analytics.periodLabel,
                totalIncome = analytics.totalIncome,
                totalExpense = analytics.totalExpense,
                netFlow = analytics.netFlow,
                transactionCount = analytics.transactionCount,
                incomeStr = incomeStr,
                expenseStr = expenseStr,
                netFlowAbsStr = netFlowAbsStr,
                dailyAvgStr = dailyAvgStr,
                peakAmount = analytics.peakAmount,
                peakStr = peakStr,
                liquidBalanceStr = liquidBalanceStr,
                savingsRate = savingsRate,
                changePct = changePct,
                topExpenses = topExpenses,
                topIncomes = topIncomes,
                accountCount = accountCount,
                activeGoalsCount = activeGoals.size,
                totalOwedByMe = totalOwedByMe,
                totalOwedToMe = totalOwedToMe,
                currencyCode = currencyCode
            )
        }
    }

    private fun resolveTopIncomeSources(
        state: MainUiState,
        lang: AppLanguage
    ): List<IncomeSourceItem> {
        val analytics = state.analytics
        if (analytics.incomeBreakdown.isNotEmpty()) {
            return analytics.incomeBreakdown.take(2).map { item ->
                IncomeSourceItem(
                    label = item.category.label(lang),
                    amount = item.amount,
                    sharePercent = (item.share * 100).toInt().coerceIn(0, 100)
                )
            }
        }
        val incomeTxs = state.transactions.filter { it.type == com.example.data.model.TransactionType.INCOME }
        if (incomeTxs.isEmpty()) return emptyList()
        val total = incomeTxs.sumOf { it.amount }.coerceAtLeast(1.0)
        return incomeTxs
            .groupBy { tx -> tx.subcategory.ifBlank { tx.category.label(lang) } }
            .map { (label, list) ->
                val sum = list.sumOf { it.amount }
                IncomeSourceItem(
                    label = label,
                    amount = sum,
                    sharePercent = ((sum / total) * 100.0).toInt().coerceIn(0, 100)
                )
            }
            .sortedByDescending { it.amount }
            .take(2)
    }

    private fun buildBanglaDetailedSummary(
        userName: String,
        filter: InsightTimeFilter,
        periodLabel: String,
        totalIncome: Double,
        totalExpense: Double,
        netFlow: Double,
        transactionCount: Int,
        incomeStr: String,
        expenseStr: String,
        netFlowAbsStr: String,
        dailyAvgStr: String,
        peakAmount: Double,
        peakStr: String,
        liquidBalanceStr: String,
        savingsRate: Int?,
        changePct: Int?,
        topExpenses: List<com.example.data.model.CategorySpend>,
        topIncomes: List<IncomeSourceItem>,
        accountCount: Int,
        activeGoalsCount: Int,
        totalOwedByMe: Double,
        totalOwedToMe: Double,
        currencyCode: String
    ): String {
        val lang = AppLanguage.BN
        val periodSuffix = if (periodLabel.isNotBlank()) " ($periodLabel)" else ""
        val periodPhrase = when (filter) {
            InsightTimeFilter.DAILY -> "আজকের$periodSuffix"
            InsightTimeFilter.THIS_WEEK -> "এই সপ্তাহের$periodSuffix"
            InsightTimeFilter.THIS_MONTH -> "এই মাসের$periodSuffix"
            InsightTimeFilter.LAST_MONTH -> "গত মাসের$periodSuffix"
        }

        val greeting = if (userName.isNotBlank()) {
            "আসসালামু আলাইকুম $userName। আমার হিসাবের বিস্তারিত আর্থিক সারসংক্ষেপে স্বাগতম।"
        } else {
            "আমার হিসাবের বিস্তারিত আর্থিক সারসংক্ষেপে স্বাগতম।"
        }

        val sections = mutableListOf<String>()
        sections.add(greeting)

        // 1. Period overview: Income, Expense, Net Balance & Savings
        val txPart = if (transactionCount > 0) {
            " এই সময়ে মোট ${BdtFormatter.formatNumber(transactionCount, lang)}টি লেনদেন নথিভুক্ত হয়েছে।"
        } else {
            ""
        }
        val cashflowSentence = when {
            totalIncome == 0.0 && totalExpense == 0.0 ->
                "$periodPhrase কোনো আয় বা ব্যয় এখনো যুক্ত হয়নি। আপনার বর্তমান মোট তরল ব্যালেন্স $liquidBalanceStr।"
            netFlow >= 0.0 -> {
                val savingsPart = if (savingsRate != null && savingsRate >= 0) {
                    " আপনার সঞ্চয়ের হার ${BdtFormatter.formatPercent(savingsRate, lang)}।"
                } else {
                    ""
                }
                "$periodPhrase আপনার মোট আয় হয়েছে $incomeStr এবং মোট ব্যয় হয়েছে $expenseStr। ফলে নিট উদ্বৃত্ত বা সঞ্চয় রয়েছে $netFlowAbsStr।$savingsPart$txPart"
            }
            else ->
                "$periodPhrase আপনার মোট আয় হয়েছে $incomeStr এবং মোট ব্যয় হয়েছে $expenseStr। ফলে আয়ের তুলনায় নিট ঘাটতি হয়েছে $netFlowAbsStr।$txPart"
        }
        sections.add(cashflowSentence)

        // 2. Daily Average & Peak Spend
        if (totalExpense > 0.0) {
            val peakPart = if (peakAmount > 0.0) {
                " এবং এক ধাপে সর্বোচ্চ খরচ ছিল $peakStr।"
            } else {
                "।"
            }
            sections.add("এই সময়ে আপনার দৈনিক গড় খরচ $dailyAvgStr$peakPart")
        }

        // 3. Top Spending Categories
        if (topExpenses.isNotEmpty()) {
            val expenseBreakdownText = when (topExpenses.size) {
                1 -> {
                    val c1 = topExpenses[0]
                    val amt1 = BdtFormatter.formatCurrency(c1.amount, currencyCode, lang)
                    val pct1 = BdtFormatter.formatPercent((c1.share * 100).toInt(), lang)
                    "খরচের খাত বিশ্লেষণে দেখা যায়, সর্বোচ্চ খরচ হয়েছে ${c1.category.label(lang)} খাতে, যার পরিমাণ $amt1 ($pct1)।"
                }
                2 -> {
                    val c1 = topExpenses[0]
                    val c2 = topExpenses[1]
                    val amt1 = BdtFormatter.formatCurrency(c1.amount, currencyCode, lang)
                    val amt2 = BdtFormatter.formatCurrency(c2.amount, currencyCode, lang)
                    val pct1 = BdtFormatter.formatPercent((c1.share * 100).toInt(), lang)
                    val pct2 = BdtFormatter.formatPercent((c2.share * 100).toInt(), lang)
                    "খরচের শীর্ষ দুটি খাত হলো ${c1.category.label(lang)} খাতে $amt1 ($pct1) এবং ${c2.category.label(lang)} খাতে $amt2 ($pct2)।"
                }
                else -> {
                    val c1 = topExpenses[0]
                    val c2 = topExpenses[1]
                    val c3 = topExpenses[2]
                    val amt1 = BdtFormatter.formatCurrency(c1.amount, currencyCode, lang)
                    val amt2 = BdtFormatter.formatCurrency(c2.amount, currencyCode, lang)
                    val amt3 = BdtFormatter.formatCurrency(c3.amount, currencyCode, lang)
                    val pct1 = BdtFormatter.formatPercent((c1.share * 100).toInt(), lang)
                    val pct2 = BdtFormatter.formatPercent((c2.share * 100).toInt(), lang)
                    val pct3 = BdtFormatter.formatPercent((c3.share * 100).toInt(), lang)
                    "খরচের শীর্ষ তিনটি খাত হলো প্রথমত ${c1.category.label(lang)} খাতে $amt1 ($pct1), দ্বিতীয়ত ${c2.category.label(lang)} খাতে $amt2 ($pct2), এবং তৃতীয়ত ${c3.category.label(lang)} খাতে $amt3 ($pct3)।"
                }
            }
            sections.add(expenseBreakdownText)
        }

        // 4. Top Income Categories
        if (topIncomes.isNotEmpty() && totalIncome > 0.0) {
            val incomeBreakdownText = if (topIncomes.size == 1) {
                val i1 = topIncomes[0]
                val amt1 = BdtFormatter.formatCurrency(i1.amount, currencyCode, lang)
                val pct1 = BdtFormatter.formatPercent(i1.sharePercent, lang)
                "আয়ের প্রধান উৎস হলো ${i1.label}, যেখান থেকে এসেছে $amt1 ($pct1)।"
            } else {
                val i1 = topIncomes[0]
                val i2 = topIncomes[1]
                val amt1 = BdtFormatter.formatCurrency(i1.amount, currencyCode, lang)
                val amt2 = BdtFormatter.formatCurrency(i2.amount, currencyCode, lang)
                "আয়ের শীর্ষ উৎসগুলো হলো ${i1.label} থেকে $amt1 এবং ${i2.label} থেকে $amt2।"
            }
            sections.add(incomeBreakdownText)
        }

        // 5. Detailed Breakdown & Practical Discussion (Trend, Accounts, Goals/Dues, Guidance)
        val discussionParts = mutableListOf<String>()
        when {
            changePct == null -> Unit
            changePct < 0 -> discussionParts.add(
                "পূর্ববর্তী সময়ের তুলনায় ${BdtFormatter.formatPercent(abs(changePct), lang)} কম খরচ হয়েছে, যা আপনার চমৎকার বাজেট নিয়ন্ত্রণের প্রমাণ।"
            )
            changePct > 0 -> discussionParts.add(
                "পূর্ববর্তী সময়ের তুলনায় ${BdtFormatter.formatPercent(changePct, lang)} বেশি খরচ হয়েছে, তাই বড় খাতগুলোতে কিছুটা নজর রাখা ভালো হবে।"
            )
            else -> discussionParts.add("আপনার খরচের গতি পূর্ববর্তী সময়ের সমান রয়েছে।")
        }

        if (totalIncome > 0.0 || totalExpense > 0.0) {
            val accSuffix = if (accountCount > 0) {
                " (${BdtFormatter.formatNumber(accountCount, lang)}টি অ্যাকাউন্ট মিলিয়ে)"
            } else {
                ""
            }
            discussionParts.add("বর্তমানে আপনার মোট তরল ব্যালেন্স রয়েছে $liquidBalanceStr$accSuffix।")
        }

        if (activeGoalsCount > 0) {
            discussionParts.add("আপনার ${BdtFormatter.formatNumber(activeGoalsCount, lang)}টি চলমান সঞ্চয় লক্ষ্য রয়েছে।")
        }
        if (totalOwedByMe > 0.0 || totalOwedToMe > 0.0) {
            val loanBits = mutableListOf<String>()
            if (totalOwedByMe > 0.0) {
                loanBits.add("পরিশোধযোগ্য দেনা ${BdtFormatter.formatCurrency(totalOwedByMe, currencyCode, lang)}")
            }
            if (totalOwedToMe > 0.0) {
                loanBits.add("পাওনা ${BdtFormatter.formatCurrency(totalOwedToMe, currencyCode, lang)}")
            }
            discussionParts.add("বকেয়া ও ঋণের হিসাবে বর্তমানে ${loanBits.joinToString(" এবং ")} বাকি আছে।")
        }

        val closingAdvice = when {
            totalIncome == 0.0 && totalExpense == 0.0 ->
                "প্রতিদিনের আয় ও ব্যয় নিয়মিত যুক্ত করলে আরও নিখুঁত বিশ্লেষণ ও সঞ্চয় পরিকল্পনা পাবেন।"
            netFlow >= 0.0 ->
                "সামগ্রিকভাবে আপনার আর্থিক অবস্থা ইতিবাচক রয়েছে। এই উদ্বৃত্তের একটি অংশ জরুরি তহবিল বা সঞ্চয় লক্ষ্যে জমিয়ে রাখুন।"
            else ->
                "আগামী দিনগুলোতে শীর্ষ খরচের খাতে দৈনিক সীমা নির্ধারণ করলে দ্রুত ভারসাম্যে ফিরে আসতে পারবেন।"
        }
        discussionParts.add(closingAdvice)

        sections.add(discussionParts.joinToString(" "))
        return sections.joinToString("\n")
    }

    private fun buildEnglishDetailedSummary(
        lang: AppLanguage,
        userName: String,
        filter: InsightTimeFilter,
        periodLabel: String,
        totalIncome: Double,
        totalExpense: Double,
        netFlow: Double,
        transactionCount: Int,
        incomeStr: String,
        expenseStr: String,
        netFlowAbsStr: String,
        dailyAvgStr: String,
        peakAmount: Double,
        peakStr: String,
        liquidBalanceStr: String,
        savingsRate: Int?,
        changePct: Int?,
        topExpenses: List<com.example.data.model.CategorySpend>,
        topIncomes: List<IncomeSourceItem>,
        accountCount: Int,
        activeGoalsCount: Int,
        totalOwedByMe: Double,
        totalOwedToMe: Double,
        currencyCode: String
    ): String {
        val periodSuffix = if (periodLabel.isNotBlank()) " ($periodLabel)" else ""
        val periodPhrase = when (filter) {
            InsightTimeFilter.DAILY -> "today$periodSuffix"
            InsightTimeFilter.THIS_WEEK -> "this week$periodSuffix"
            InsightTimeFilter.THIS_MONTH -> "this month$periodSuffix"
            InsightTimeFilter.LAST_MONTH -> "last month$periodSuffix"
        }

        val greeting = if (userName.isNotBlank()) {
            "Hello $userName. Welcome to your detailed Amar Hisab financial summary."
        } else {
            "Welcome to your detailed Amar Hisab financial summary."
        }

        val sections = mutableListOf<String>()
        sections.add(greeting)

        val txPart = if (transactionCount > 0) {
            " A total of $transactionCount transactions were recorded in this period."
        } else {
            ""
        }
        val cashflowSentence = when {
            totalIncome == 0.0 && totalExpense == 0.0 ->
                "No income or expenses have been logged for $periodPhrase yet. Your current liquid balance is $liquidBalanceStr."
            netFlow >= 0.0 -> {
                val savingsPart = if (savingsRate != null && savingsRate >= 0) {
                    " Your savings rate stands at $savingsRate%."
                } else {
                    ""
                }
                "For $periodPhrase, total income is $incomeStr and total spending is $expenseStr, leaving a net surplus of $netFlowAbsStr.$savingsPart$txPart"
            }
            else ->
                "For $periodPhrase, total income is $incomeStr against $expenseStr spent, resulting in a net deficit of $netFlowAbsStr.$txPart"
        }
        sections.add(cashflowSentence)

        if (totalExpense > 0.0) {
            val peakPart = if (peakAmount > 0.0) {
                ", with a peak spending amount of $peakStr."
            } else {
                "."
            }
            sections.add("Your daily average spending is $dailyAvgStr$peakPart")
        }

        if (topExpenses.isNotEmpty()) {
            val expText = when (topExpenses.size) {
                1 -> {
                    val c1 = topExpenses[0]
                    val amt1 = BdtFormatter.formatCurrency(c1.amount, currencyCode, lang)
                    "Looking at spending categories, your top expense is ${c1.category.label(lang)} at $amt1 (${(c1.share * 100).toInt()}%)."
                }
                2 -> {
                    val c1 = topExpenses[0]
                    val c2 = topExpenses[1]
                    val amt1 = BdtFormatter.formatCurrency(c1.amount, currencyCode, lang)
                    val amt2 = BdtFormatter.formatCurrency(c2.amount, currencyCode, lang)
                    "Your top two spending categories are ${c1.category.label(lang)} at $amt1 (${(c1.share * 100).toInt()}%) and ${c2.category.label(lang)} at $amt2 (${(c2.share * 100).toInt()}%)."
                }
                else -> {
                    val c1 = topExpenses[0]
                    val c2 = topExpenses[1]
                    val c3 = topExpenses[2]
                    val amt1 = BdtFormatter.formatCurrency(c1.amount, currencyCode, lang)
                    val amt2 = BdtFormatter.formatCurrency(c2.amount, currencyCode, lang)
                    val amt3 = BdtFormatter.formatCurrency(c3.amount, currencyCode, lang)
                    "Your top three spending categories are ${c1.category.label(lang)} at $amt1 (${(c1.share * 100).toInt()}%), ${c2.category.label(lang)} at $amt2 (${(c2.share * 100).toInt()}%), and ${c3.category.label(lang)} at $amt3 (${(c3.share * 100).toInt()}%)."
                }
            }
            sections.add(expText)
        }

        if (topIncomes.isNotEmpty() && totalIncome > 0.0) {
            val incText = if (topIncomes.size == 1) {
                val i1 = topIncomes[0]
                val amt1 = BdtFormatter.formatCurrency(i1.amount, currencyCode, lang)
                "Your primary income source is ${i1.label}, contributing $amt1 (${i1.sharePercent}%)."
            } else {
                val i1 = topIncomes[0]
                val i2 = topIncomes[1]
                val amt1 = BdtFormatter.formatCurrency(i1.amount, currencyCode, lang)
                val amt2 = BdtFormatter.formatCurrency(i2.amount, currencyCode, lang)
                "Your leading income sources are ${i1.label} at $amt1 and ${i2.label} at $amt2."
            }
            sections.add(incText)
        }

        val discussion = mutableListOf<String>()
        when {
            changePct == null -> Unit
            changePct < 0 -> discussion.add(
                "Spending is down ${abs(changePct)}% compared to the prior period, showing strong budget discipline."
            )
            changePct > 0 -> discussion.add(
                "Spending is up $changePct% compared to the prior period, so keeping an eye on top categories will help."
            )
            else -> discussion.add("Your spending pace matches the prior period.")
        }

        if (totalIncome > 0.0 || totalExpense > 0.0) {
            val accSuffix = if (accountCount > 0) " across $accountCount accounts" else ""
            discussion.add("Your available liquid balance stands at $liquidBalanceStr$accSuffix.")
        }
        if (activeGoalsCount > 0) {
            discussion.add("You currently have $activeGoalsCount active savings goals in progress.")
        }
        if (totalOwedByMe > 0.0 || totalOwedToMe > 0.0) {
            val loanBits = mutableListOf<String>()
            if (totalOwedByMe > 0.0) {
                loanBits.add("${BdtFormatter.formatCurrency(totalOwedByMe, currencyCode, lang)} payable")
            }
            if (totalOwedToMe > 0.0) {
                loanBits.add("${BdtFormatter.formatCurrency(totalOwedToMe, currencyCode, lang)} receivable")
            }
            discussion.add("In dues and loans, you currently have ${loanBits.joinToString(" and ")} remaining.")
        }

        val closing = when {
            totalIncome == 0.0 && totalExpense == 0.0 ->
                "Log your daily income and expenses regularly to unlock richer trends and savings guidance."
            netFlow >= 0.0 ->
                "Overall your finances are in a healthy position. Consider directing part of your surplus toward savings goals or an emergency fund."
            else ->
                "Setting a modest daily cap on your top spending category will help bring cash flow back into balance."
        }
        discussion.add(closing)
        sections.add(discussion.joinToString(" "))
        return sections.joinToString("\n")
    }

    private fun buildArabicDetailedSummary(
        userName: String,
        filter: InsightTimeFilter,
        periodLabel: String,
        totalIncome: Double,
        totalExpense: Double,
        netFlow: Double,
        incomeStr: String,
        expenseStr: String,
        netFlowAbsStr: String,
        dailyAvgStr: String,
        liquidBalanceStr: String,
        savingsRate: Int?,
        changePct: Int?,
        topExpenses: List<com.example.data.model.CategorySpend>,
        topIncomes: List<IncomeSourceItem>,
        currencyCode: String,
        lang: AppLanguage
    ): String {
        val periodPhrase = when (filter) {
            InsightTimeFilter.DAILY -> "اليوم"
            InsightTimeFilter.THIS_WEEK -> "هذا الأسبوع"
            InsightTimeFilter.THIS_MONTH -> "هذا الشهر"
            InsightTimeFilter.LAST_MONTH -> "الشهر الماضي"
        } + if (periodLabel.isNotBlank()) " ($periodLabel)" else ""

        val greeting = if (userName.isNotBlank()) {
            "مرحباً $userName. إليك الملخص المالي المفصل من تطبيق আমার হিসাব."
        } else {
            "مرحباً بك في الملخص المالي المفصل."
        }
        val sections = mutableListOf(greeting)
        val cashflow = when {
            totalIncome == 0.0 && totalExpense == 0.0 ->
                "لم يتم تسجيل أي معاملات في $periodPhrase بعد. رصيدك المتاح حالياً هو $liquidBalanceStr."
            netFlow >= 0.0 -> {
                val sr = savingsRate?.takeIf { it >= 0 }?.let { " بنسبة ادخار $it%." } ?: ""
                "خلال $periodPhrase، بلغ إجمالي الدخل $incomeStr وإجمالي المصروفات $expenseStr، بصافي فائض قدره $netFlowAbsStr.$sr"
            }
            else ->
                "خلال $periodPhrase، بلغ إجمالي الدخل $incomeStr مقابل مصروفات قدرها $expenseStr، مما أدى إلى عجز صافٍ قدره $netFlowAbsStr."
        }
        sections.add(cashflow)
        if (totalExpense > 0.0) {
            sections.add("متوسط الإنفاق اليومي هو $dailyAvgStr.")
        }
        if (topExpenses.isNotEmpty()) {
            val topList = topExpenses.joinToString(" و") { c ->
                "${c.category.label(lang)} بقيمة ${BdtFormatter.formatCurrency(c.amount, currencyCode, lang)} (${(c.share * 100).toInt()}%)"
            }
            sections.add("أعلى فئات الإنفاق هي $topList.")
        }
        if (topIncomes.isNotEmpty() && totalIncome > 0.0) {
            val incList = topIncomes.joinToString(" و") { i ->
                "${i.label} بقيمة ${BdtFormatter.formatCurrency(i.amount, currencyCode, lang)}"
            }
            sections.add("مصادر الدخل الرئيسية هي $incList.")
        }
        val trend = when {
            changePct == null -> "رصيدك السائل الحالي هو $liquidBalanceStr."
            changePct < 0 -> "انخفضت المصروفات بنسبة ${abs(changePct)}% مقارنة بالفترة السابقة. رصيدك السائل الحالي هو $liquidBalanceStr."
            changePct > 0 -> "ارتفعت المصروفات بنسبة $changePct% مقارنة بالفترة السابقة. رصيدك السائل الحالي هو $liquidBalanceStr."
            else -> "معدل الإنفاق مماثل للفترة السابقة. رصيدك السائل الحالي هو $liquidBalanceStr."
        }
        sections.add(trend)
        return sections.joinToString("\n")
    }

    private fun buildHindiDetailedSummary(
        userName: String,
        filter: InsightTimeFilter,
        periodLabel: String,
        totalIncome: Double,
        totalExpense: Double,
        netFlow: Double,
        incomeStr: String,
        expenseStr: String,
        netFlowAbsStr: String,
        dailyAvgStr: String,
        liquidBalanceStr: String,
        savingsRate: Int?,
        changePct: Int?,
        topExpenses: List<com.example.data.model.CategorySpend>,
        topIncomes: List<IncomeSourceItem>,
        currencyCode: String,
        lang: AppLanguage
    ): String {
        val periodPhrase = when (filter) {
            InsightTimeFilter.DAILY -> "आज"
            InsightTimeFilter.THIS_WEEK -> "इस सप्ताह"
            InsightTimeFilter.THIS_MONTH -> "इस महीने"
            InsightTimeFilter.LAST_MONTH -> "पिछले महीने"
        } + if (periodLabel.isNotBlank()) " ($periodLabel)" else ""

        val greeting = if (userName.isNotBlank()) {
            "नमस्ते $userName। आपके विस्तृत वित्तीय सारांश में स्वागत है।"
        } else {
            "आपके विस्तृत वित्तीय सारांश में स्वागत है।"
        }
        val sections = mutableListOf(greeting)
        val cashflow = when {
            totalIncome == 0.0 && totalExpense == 0.0 ->
                "$periodPhrase में अभी तक कोई लेनदेन दर्ज नहीं हुआ है। आपका वर्तमान उपलब्ध बैलेंस $liquidBalanceStr है।"
            netFlow >= 0.0 -> {
                val sr = savingsRate?.takeIf { it >= 0 }?.let { " आपकी बचत दर $it% है।" } ?: ""
                "$periodPhrase में कुल आय $incomeStr और कुल खर्च $expenseStr रहा, जिससे शुद्ध बचत $netFlowAbsStr हुई।$sr"
            }
            else ->
                "$periodPhrase में कुल आय $incomeStr और कुल खर्च $expenseStr रहा, जिससे $netFlowAbsStr का शुद्ध घाटा हुआ।"
        }
        sections.add(cashflow)
        if (totalExpense > 0.0) {
            sections.add("आपका दैनिक औसत खर्च $dailyAvgStr है।")
        }
        if (topExpenses.isNotEmpty()) {
            val topList = topExpenses.joinToString(", ") { c ->
                "${c.category.label(lang)} में ${BdtFormatter.formatCurrency(c.amount, currencyCode, lang)} (${(c.share * 100).toInt()}%)"
            }
            sections.add("प्रमुख खर्च श्रेणियाँ हैं: $topList।")
        }
        if (topIncomes.isNotEmpty() && totalIncome > 0.0) {
            val incList = topIncomes.joinToString(", ") { i ->
                "${i.label} से ${BdtFormatter.formatCurrency(i.amount, currencyCode, lang)}"
            }
            sections.add("आय के प्रमुख स्रोत हैं: $incList।")
        }
        val trend = when {
            changePct == null -> "आपका कुल उपलब्ध बैलेंस $liquidBalanceStr है।"
            changePct < 0 -> "पिछली अवधि की तुलना में खर्च ${abs(changePct)}% कम हुआ है। आपका कुल उपलब्ध बैलेंस $liquidBalanceStr है।"
            changePct > 0 -> "पिछली अवधि की तुलना में खर्च $changePct% बढ़ा है। आपका कुल उपलब्ध बैलेंस $liquidBalanceStr है।"
            else -> "खर्च की गति पिछली अवधि के समान है। आपका कुल उपलब्ध बैलेंस $liquidBalanceStr है।"
        }
        sections.add(trend)
        return sections.joinToString("\n")
    }

    private fun buildUrduDetailedSummary(
        userName: String,
        filter: InsightTimeFilter,
        periodLabel: String,
        totalIncome: Double,
        totalExpense: Double,
        netFlow: Double,
        incomeStr: String,
        expenseStr: String,
        netFlowAbsStr: String,
        dailyAvgStr: String,
        liquidBalanceStr: String,
        savingsRate: Int?,
        changePct: Int?,
        topExpenses: List<com.example.data.model.CategorySpend>,
        topIncomes: List<IncomeSourceItem>,
        currencyCode: String,
        lang: AppLanguage
    ): String {
        val periodPhrase = when (filter) {
            InsightTimeFilter.DAILY -> "آج"
            InsightTimeFilter.THIS_WEEK -> "اس ہفتے"
            InsightTimeFilter.THIS_MONTH -> "اس ماہ"
            InsightTimeFilter.LAST_MONTH -> "پچھلے ماہ"
        } + if (periodLabel.isNotBlank()) " ($periodLabel)" else ""

        val greeting = if (userName.isNotBlank()) {
            "السلام علیکم $userName۔ آپ کے تفصیلی مالی خلاصے میں خوش آمدید۔"
        } else {
            "آپ کے تفصیلی مالی خلاصے میں خوش آمدید۔"
        }
        val sections = mutableListOf(greeting)
        val cashflow = when {
            totalIncome == 0.0 && totalExpense == 0.0 ->
                "$periodPhrase میں ابھی تک کوئی لین دین درج نہیں ہوا۔ آپ کا موجودہ بیلنس $liquidBalanceStr ہے۔"
            netFlow >= 0.0 -> {
                val sr = savingsRate?.takeIf { it >= 0 }?.let { " آپ کی بچت کی شرح $it% ہے۔" } ?: ""
                "$periodPhrase میں کل آمدنی $incomeStr اور کل اخراجات $expenseStr رہے، جس سے خالص بچت $netFlowAbsStr ہے۔$sr"
            }
            else ->
                "$periodPhrase میں کل آمدنی $incomeStr اور کل اخراجات $expenseStr رہے، جس سے $netFlowAbsStr کا خالص خسارہ ہوا۔"
        }
        sections.add(cashflow)
        if (totalExpense > 0.0) {
            sections.add("آپ کا روزانہ اوسط خرچ $dailyAvgStr ہے۔")
        }
        if (topExpenses.isNotEmpty()) {
            val topList = topExpenses.joinToString("، ") { c ->
                "${c.category.label(lang)} میں ${BdtFormatter.formatCurrency(c.amount, currencyCode, lang)} (${(c.share * 100).toInt()}%)"
            }
            sections.add("اخراجات کے نمایاں شعبے یہ ہیں: $topList۔")
        }
        if (topIncomes.isNotEmpty() && totalIncome > 0.0) {
            val incList = topIncomes.joinToString("، ") { i ->
                "${i.label} سے ${BdtFormatter.formatCurrency(i.amount, currencyCode, lang)}"
            }
            sections.add("آمدنی کے نمایاں ذرائع یہ ہیں: $incList۔")
        }
        val trend = when {
            changePct == null -> "آپ کا موجودہ کل بیلنس $liquidBalanceStr ہے۔"
            changePct < 0 -> "گزشتہ مدت کے مقابلے میں اخراجات ${abs(changePct)}% کم ہوئے ہیں۔ آپ کا موجودہ بیلنس $liquidBalanceStr ہے۔"
            changePct > 0 -> "گزشتہ مدت کے مقابلے میں اخراجات $changePct% بڑھے ہیں۔ آپ کا موجودہ بیلنس $liquidBalanceStr ہے۔"
            else -> "اخراجات کی رفتار گزشتہ مدت کے برابر ہے۔ آپ کا موجودہ بیلنس $liquidBalanceStr ہے۔"
        }
        sections.add(trend)
        return sections.joinToString("\n")
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
