package com.example.data.export

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import com.example.R
import com.example.data.local.AccountEntity
import com.example.data.local.TransactionEntity
import com.example.data.market.MarketSnapshot
import com.example.data.model.AccountNameResolver
import com.example.data.model.AppCurrency
import com.example.data.model.CategorySpend
import com.example.data.model.InsightTimeFilter
import com.example.data.model.TransactionType
import com.example.data.model.TransferNoteCodec
import com.example.localization.AppLanguage
import com.example.localization.AppStrings
import com.example.localization.BdtFormatter
import com.example.ui.viewmodel.MainUiState
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

enum class ReportExportFormat(
    val extension: String,
    val mimeType: String
) {
    PDF("pdf", "application/pdf"),
    CSV("csv", "text/csv")
}

data class CurrencyMonthlyTotals(
    val currencyCode: String,
    val income: Double,
    val expense: Double,
    val transferOut: Double
) {
    val netFlow: Double get() = income - expense
}

data class MonthlyReportPayload(
    val periodLabel: String,
    val fileSlug: String,
    val generatedAtMillis: Long,
    val userName: String,
    val language: AppLanguage,
    val totalIncome: Double,
    val totalExpense: Double,
    val netFlow: Double,
    val dailyAverage: Double,
    val peakAmount: Double,
    val monthlyBudget: Double,
    val savingsRatePercent: Int?,
    val expenseChangePercent: Int?,
    val categoryBreakdown: List<CategorySpend>,
    val transactions: List<TransactionEntity>,
    val accountNames: Map<Long, String>,
    val wallets: List<AccountEntity>,
    val mainCurrencyCode: String = AppCurrency.BDT.code,
    val perCurrencyTotals: List<CurrencyMonthlyTotals> = emptyList(),
    val marketSnapshot: MarketSnapshot = MarketSnapshot.defaultReference()
)

data class ExportSharePayload(
    val format: ReportExportFormat,
    val file: File,
    val uri: Uri,
    val mimeType: String,
    val shareIntent: Intent,
    val chooserTitle: String,
    val periodLabel: String
)

object MonthlyReportExporter {

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN_X = 36f
    private const val CONTENT_WIDTH = PAGE_WIDTH - (MARGIN_X * 2)

    fun buildMonthlyPayload(
        state: MainUiState,
        nowMillis: Long = System.currentTimeMillis(),
        marketSnapshot: MarketSnapshot = MarketSnapshot.defaultReference()
    ): MonthlyReportPayload {
        val monthOffset = if (state.insightFilter == InsightTimeFilter.LAST_MONTH) {
            -1
        } else {
            state.insightMonthOffset
        }

        val startCal = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.MONTH, monthOffset)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val endCal = Calendar.getInstance().apply {
            timeInMillis = startCal.timeInMillis
            add(Calendar.MONTH, 1)
        }
        val prevStartCal = Calendar.getInstance().apply {
            timeInMillis = startCal.timeInMillis
            add(Calendar.MONTH, -1)
        }

        val periodLabel = SimpleDateFormat("MMMM yyyy", Locale.ENGLISH).format(startCal.time)
        val fileSlug = SimpleDateFormat("yyyy_MM", Locale.US).format(startCal.time)
        val mainCurCode = AppCurrency.fromCode(state.prefs.mainCurrencyCode).code

        val monthTx = state.transactions
            .filter { it.timestamp >= startCal.timeInMillis && it.timestamp < endCal.timeInMillis }
            .sortedByDescending { it.timestamp }

        val prevMonthTx = state.transactions
            .filter { it.timestamp >= prevStartCal.timeInMillis && it.timestamp < startCal.timeInMillis }

        fun toMainCurrency(tx: TransactionEntity): Double {
            val txCur = AppCurrency.fromCode(tx.currencyCode).code
            return if (txCur.equals(mainCurCode, ignoreCase = true)) {
                tx.amount
            } else {
                marketSnapshot.convertCurrencyCode(tx.amount, txCur, mainCurCode)
            }
        }

        val totalIncome = monthTx
            .filter { it.type == TransactionType.INCOME }
            .sumOf { toMainCurrency(it) }
        val totalExpense = monthTx
            .filter { it.type == TransactionType.EXPENSE }
            .sumOf { toMainCurrency(it) }
        val prevExpense = prevMonthTx
            .filter { it.type == TransactionType.EXPENSE }
            .sumOf { toMainCurrency(it) }

        val perCurrencyTotals = monthTx
            .groupBy { AppCurrency.fromCode(it.currencyCode).code }
            .map { (curCode, list) ->
                CurrencyMonthlyTotals(
                    currencyCode = curCode,
                    income = list.filter { it.type == TransactionType.INCOME }.sumOf { it.amount },
                    expense = list.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount },
                    transferOut = list.filter { it.type == TransactionType.TRANSFER }.sumOf { it.amount }
                )
            }
            .sortedWith(
                compareByDescending<CurrencyMonthlyTotals> { it.currencyCode.equals(mainCurCode, ignoreCase = true) }
                    .thenBy { it.currencyCode }
            )

        val daysInMonth = startCal.getActualMaximum(Calendar.DAY_OF_MONTH).coerceAtLeast(1)
        val nowCal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val isCurrentMonth = startCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR) &&
            startCal.get(Calendar.MONTH) == nowCal.get(Calendar.MONTH)
        val elapsedDays = if (isCurrentMonth) {
            nowCal.get(Calendar.DAY_OF_MONTH).coerceIn(1, daysInMonth)
        } else {
            daysInMonth
        }
        val dailyAverage = if (totalExpense > 0.0) totalExpense / elapsedDays else 0.0

        val dayFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val peakAmount = monthTx
            .filter { it.type == TransactionType.EXPENSE }
            .groupBy { dayFmt.format(Date(it.timestamp)) }
            .values
            .maxOfOrNull { list -> list.sumOf { toMainCurrency(it) } } ?: 0.0

        val expenseChangePercent = if (prevExpense > 0.0) {
            (((totalExpense - prevExpense) / prevExpense) * 100.0).toInt()
        } else {
            null
        }
        val savingsRatePercent = if (totalIncome > 0.0) {
            (((totalIncome - totalExpense) / totalIncome) * 100.0).toInt().coerceIn(-999, 100)
        } else {
            null
        }

        val breakdown = monthTx
            .filter { it.type == TransactionType.EXPENSE }
            .groupBy { it.category }
            .map { (cat, list) ->
                val sum = list.sumOf { toMainCurrency(it) }
                CategorySpend(
                    category = cat,
                    amount = sum,
                    share = if (totalExpense > 0.0) (sum / totalExpense).toFloat() else 0f
                )
            }
            .sortedByDescending { it.amount }

        val accountNames = state.accounts.associate { acc ->
            acc.id to AccountNameResolver.resolveAccountName(acc, state.prefs.language)
        }
        val effectiveUser = state.prefs.userName.ifBlank {
            if (state.prefs.language == AppLanguage.BN) "গেস্ট ইউজার" else "Guest"
        }

        return MonthlyReportPayload(
            periodLabel = periodLabel,
            fileSlug = fileSlug,
            generatedAtMillis = nowMillis,
            userName = effectiveUser,
            language = state.prefs.language,
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            netFlow = totalIncome - totalExpense,
            dailyAverage = dailyAverage,
            peakAmount = peakAmount,
            monthlyBudget = state.prefs.monthlyBudget,
            savingsRatePercent = savingsRatePercent,
            expenseChangePercent = expenseChangePercent,
            categoryBreakdown = breakdown,
            transactions = monthTx,
            accountNames = accountNames,
            wallets = state.accounts,
            mainCurrencyCode = mainCurCode,
            perCurrencyTotals = perCurrencyTotals,
            marketSnapshot = marketSnapshot
        )
    }

    fun generateMonthlyCsv(payload: MonthlyReportPayload): String {
        val dateFmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
        val accountsById = payload.wallets.associateBy { it.id }
        val sb = StringBuilder()

        // Section 1: Monthly Financial Summary
        sb.appendLine("Section,Metric,Value")
        sb.appendLine("Summary,App,${csvEscape("Amar Hisab (আমার হিসাব)")}")
        sb.appendLine("Summary,Report_Period,${csvEscape(payload.periodLabel)}")
        sb.appendLine("Summary,Account_Holder,${csvEscape(payload.userName)}")
        sb.appendLine("Summary,Main_Currency,${payload.mainCurrencyCode}")
        sb.appendLine("Summary,Generated_At,${csvEscape(dateFmt.format(Date(payload.generatedAtMillis)))}")
        sb.appendLine("Summary,Total_Income_BDT,${formatNumeric(payload.totalIncome)}")
        sb.appendLine("Summary,Total_Expense_BDT,${formatNumeric(payload.totalExpense)}")
        sb.appendLine("Summary,Net_Cash_Flow_BDT,${formatNumeric(payload.netFlow)}")
        sb.appendLine("Summary,Monthly_Budget_BDT,${formatNumeric(payload.monthlyBudget)}")
        sb.appendLine("Summary,Daily_Average_Expense_BDT,${formatNumeric(payload.dailyAverage)}")
        sb.appendLine("Summary,Peak_Daily_Expense_BDT,${formatNumeric(payload.peakAmount)}")
        sb.appendLine("Summary,Savings_Rate_Percent,${payload.savingsRatePercent?.toString() ?: "N/A"}")
        sb.appendLine("Summary,Expense_Change_Vs_Prev_Month_Percent,${payload.expenseChangePercent?.toString() ?: "N/A"}")
        sb.appendLine("Summary,Transaction_Count,${payload.transactions.size}")
        sb.appendLine()

        // Section 1B: Per-Currency Breakdown
        if (payload.perCurrencyTotals.isNotEmpty()) {
            sb.appendLine("Currency_Summary,Currency,Income,Expense,Net_Flow,Transfer_Out")
            payload.perCurrencyTotals.forEach { cur ->
                sb.appendLine(
                    listOf(
                        "Currency_Summary",
                        cur.currencyCode,
                        formatNumeric(cur.income),
                        formatNumeric(cur.expense),
                        formatNumeric(cur.netFlow),
                        formatNumeric(cur.transferOut)
                    ).joinToString(",")
                )
            }
            sb.appendLine()
        }

        // Section 2: Category Expense Breakdown
        sb.appendLine("Category_Breakdown,Category,Amount_${payload.mainCurrencyCode},Share_Percent")
        if (payload.categoryBreakdown.isEmpty()) {
            sb.appendLine("Category_Breakdown,None,0.00,0.0")
        } else {
            payload.categoryBreakdown.forEach { item ->
                val catLabel = if (payload.language == AppLanguage.BN) {
                    "${item.category.bn} (${item.category.en})"
                } else {
                    item.category.en
                }
                val sharePct = String.format(Locale.US, "%.1f", item.share * 100f)
                sb.appendLine(
                    "Category_Breakdown,${csvEscape(catLabel)},${formatNumeric(item.amount)},$sharePct"
                )
            }
        }
        sb.appendLine()

        // Section 3: Monthly Transactions Ledger
        sb.appendLine("Date,Title,Type,Category,Subcategory,Wallet,To_Wallet,Currency,Amount,Conversion_Details,Note")
        payload.transactions.forEach { tx ->
            val dateStr = dateFmt.format(Date(tx.timestamp))
            val txCurCode = AppCurrency.fromCode(tx.currencyCode).code
            val walletName: String
            val toWalletName: String
            val displayTitle: String
            val conversionDetails: String
            val cleanNote: String
            if (tx.type == TransactionType.TRANSFER) {
                val (fromLabel, toLabel) = AccountNameResolver.resolveTransferEndpoints(tx, accountsById, payload.language)
                walletName = fromLabel
                toWalletName = toLabel
                displayTitle = "$fromLabel → $toLabel"
                val parsed = TransferNoteCodec.parse(tx, tx.toAccountId?.let { accountsById[it] }, payload.marketSnapshot)
                conversionDetails = if (parsed != null && parsed.isCrossCurrency) {
                    TransferNoteCodec.formatConversionAsciiSummary(parsed)
                } else ""
                cleanNote = TransferNoteCodec.stripEncodedSegment(tx.note)
            } else {
                walletName = payload.accountNames[tx.accountId]?.takeIf { it.isNotBlank() }
                    ?: AccountNameResolver.resolveTransactionWalletDisplay(tx, accountsById, payload.language)
                toWalletName = ""
                displayTitle = tx.title
                conversionDetails = ""
                cleanNote = tx.note
            }
            val catName = if (payload.language == AppLanguage.BN) tx.category.bn else tx.category.en
            sb.appendLine(
                listOf(
                    csvEscape(dateStr),
                    csvEscape(displayTitle),
                    tx.type.name,
                    csvEscape(catName),
                    csvEscape(tx.subcategory),
                    csvEscape(walletName),
                    csvEscape(toWalletName),
                    txCurCode,
                    formatNumeric(tx.amount),
                    csvEscape(conversionDetails),
                    csvEscape(cleanNote)
                ).joinToString(",")
            )
        }

        return sb.toString()
    }

    fun generateMonthlyPdf(
        context: Context,
        payload: MonthlyReportPayload,
        outputFile: File
    ): File {
        outputFile.parentFile?.mkdirs()
        val nativeResult = runCatching {
            renderWithAndroidPdfDocument(context, payload, outputFile)
        }
        if (nativeResult.isSuccess && outputFile.exists() && outputFile.length() > 0L) {
            return outputFile
        }
        return writeSpecCompliantFallbackPdf(payload, outputFile)
    }

    private fun renderWithAndroidPdfDocument(
        context: Context,
        payload: MonthlyReportPayload,
        outputFile: File
    ): File {
        val isBn = payload.language == AppLanguage.BN
        val baseTypeface = runCatching {
            ResourcesCompat.getFont(context, R.font.hind_siliguri)
        }.getOrNull() ?: Typeface.SANS_SERIF
        val boldTypeface = Typeface.create(baseTypeface, Typeface.BOLD)
        val monoTypeface = runCatching {
            ResourcesCompat.getFont(context, R.font.jetbrains_mono)
        }.getOrNull() ?: Typeface.MONOSPACE

        val document = PdfDocument()
        try {
            var pageNumber = 1
            var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            var page = document.startPage(pageInfo)
            var canvas = page.canvas

            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 18f
                typeface = boldTypeface
            }
            val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#DBEAFE")
                textSize = 10.5f
                typeface = baseTypeface
            }
            val sectionTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#0F172A")
                textSize = 12.5f
                typeface = boldTypeface
            }
            val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#1E293B")
                textSize = 9.5f
                typeface = baseTypeface
            }
            val mutedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#64748B")
                textSize = 8.5f
                typeface = baseTypeface
            }
            val valueBoldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#0F172A")
                textSize = 11.5f
                typeface = Typeface.create(monoTypeface, Typeface.BOLD)
            }
            val cardFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = Color.parseColor("#F8FAFC")
            }
            val cardStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 0.9f
                color = Color.parseColor("#CBD5E1")
            }
            val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#E2E8F0")
                strokeWidth = 0.8f
            }

            val generatedDateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.ENGLISH)
                .format(Date(payload.generatedAtMillis))

            fun drawFooter(c: Canvas, pageNum: Int) {
                val footerY = PAGE_HEIGHT - 24f
                c.drawLine(MARGIN_X, footerY - 12f, PAGE_WIDTH - MARGIN_X, footerY - 12f, dividerPaint)
                val leftText = if (isBn) {
                    "আমার হিসাব • অফলাইন ব্যক্তিগত আর্থিক রিপোর্ট"
                } else {
                    "Amar Hisab • Offline Personal Finance Vault"
                }
                c.drawText(leftText, MARGIN_X, footerY, mutedPaint)
                val rightText = if (isBn) "পৃষ্ঠা $pageNum" else "Page $pageNum"
                val rightW = mutedPaint.measureText(rightText)
                c.drawText(rightText, PAGE_WIDTH - MARGIN_X - rightW, footerY, mutedPaint)
            }

            fun startNextPage(): Float {
                drawFooter(canvas, pageNumber)
                document.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                page = document.startPage(pageInfo)
                canvas = page.canvas

                val headerBar = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#0F172A") }
                canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 44f, headerBar)
                val contTitle = if (isBn) {
                    "আমার হিসাব • মাসিক আর্থিক সারসংক্ষেপ (${payload.periodLabel})"
                } else {
                    "Amar Hisab • Monthly Financial Summary (${payload.periodLabel})"
                }
                val smallWhite = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.WHITE
                    textSize = 10.5f
                    typeface = boldTypeface
                }
                canvas.drawText(contTitle, MARGIN_X, 27f, smallWhite)
                return 64f
            }

            // Top Branded Header Banner
            val bannerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#0F172A") }
            canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 106f, bannerPaint)
            val accentStripePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#0284C7") }
            canvas.drawRect(0f, 102f, PAGE_WIDTH.toFloat(), 106f, accentStripePaint)

            val appBannerTitle = if (isBn) "আমার হিসাব • মাসিক আর্থিক সারসংক্ষেপ" else "Amar Hisab • Monthly Financial Summary"
            canvas.drawText(appBannerTitle, MARGIN_X, 38f, titlePaint)
            val holderLine = if (isBn) {
                "অ্যাকাউন্ট হোল্ডার: ${payload.userName}   •   তৈরির তারিখ: $generatedDateStr"
            } else {
                "Account Holder: ${payload.userName}   •   Generated: $generatedDateStr"
            }
            canvas.drawText(holderLine, MARGIN_X, 60f, subtitlePaint)

            val pillBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#1E3A5F") }
            val periodText = if (isBn) "সময়কাল: ${payload.periodLabel}" else "Period: ${payload.periodLabel}"
            val pillWidth = subtitlePaint.measureText(periodText) + 24f
            val pillRect = RectF(MARGIN_X, 70f, MARGIN_X + pillWidth, 92f)
            canvas.drawRoundRect(pillRect, 11f, 11f, pillBg)
            canvas.drawText(periodText, MARGIN_X + 12f, 84.5f, subtitlePaint)

            var y = 124f

            // 4 KPI Summary Cards
            val kpiGap = 10f
            val kpiWidth = (CONTENT_WIDTH - (kpiGap * 3)) / 4f
            val kpiHeight = 62f

            data class KpiCardSpec(
                val label: String,
                val value: String,
                val sublabel: String,
                val accentColor: Int
            )

            val budgetPct = if (payload.monthlyBudget > 0.0) {
                ((payload.totalExpense / payload.monthlyBudget) * 100.0).toInt()
            } else {
                0
            }
            val savingsText = payload.savingsRatePercent?.let { "$it%" } ?: "—"
            val deltaSub = payload.expenseChangePercent?.let { delta ->
                when {
                    delta < 0 -> if (isBn) "গত মাসের চেয়ে ${abs(delta)}% কম" else "${abs(delta)}% lower vs prev"
                    delta > 0 -> if (isBn) "গত মাসের চেয়ে $delta% বেশি" else "$delta% higher vs prev"
                    else -> if (isBn) "গত মাসের সমান" else "Same as last month"
                }
            } ?: if (isBn) "মোট ${payload.transactions.size} টি লেনদেন" else "${payload.transactions.size} transactions"

            val kpiSpecs = listOf(
                KpiCardSpec(
                    label = if (isBn) "মোট আয়" else "Total Income",
                    value = BdtFormatter.formatCurrency(payload.totalIncome, payload.mainCurrencyCode, AppLanguage.EN),
                    sublabel = if (isBn) "সঞ্চয় হার: $savingsText" else "Savings rate: $savingsText",
                    accentColor = Color.parseColor("#059669")
                ),
                KpiCardSpec(
                    label = if (isBn) "মোট ব্যয়" else "Total Expense",
                    value = BdtFormatter.formatCurrency(payload.totalExpense, payload.mainCurrencyCode, AppLanguage.EN),
                    sublabel = deltaSub,
                    accentColor = Color.parseColor("#DC2626")
                ),
                KpiCardSpec(
                    label = if (isBn) "নিট প্রবাহ" else "Net Cash Flow",
                    value = BdtFormatter.formatCurrency(payload.netFlow, payload.mainCurrencyCode, AppLanguage.EN, includeSign = true),
                    sublabel = if (payload.netFlow >= 0) {
                        if (isBn) "ইতিবাচক উদ্বৃত্ত" else "Positive surplus"
                    } else {
                        if (isBn) "ঘাটতি প্রবাহ" else "Net deficit"
                    },
                    accentColor = if (payload.netFlow >= 0) Color.parseColor("#0284C7") else Color.parseColor("#DC2626")
                ),
                KpiCardSpec(
                    label = if (isBn) "দৈনিক গড় ব্যয়" else "Daily Average",
                    value = BdtFormatter.formatCurrency(payload.dailyAverage, payload.mainCurrencyCode, AppLanguage.EN),
                    sublabel = if (isBn) "বাজেট ব্যবহার: $budgetPct%" else "Budget used: $budgetPct%",
                    accentColor = Color.parseColor("#7C3AED")
                )
            )

            kpiSpecs.forEachIndexed { index, spec ->
                val left = MARGIN_X + index * (kpiWidth + kpiGap)
                val rect = RectF(left, y, left + kpiWidth, y + kpiHeight)
                canvas.drawRoundRect(rect, 10f, 10f, cardFillPaint)
                canvas.drawRoundRect(rect, 10f, 10f, cardStrokePaint)

                val topAccent = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = spec.accentColor }
                canvas.drawRoundRect(RectF(left + 10f, y + 8f, left + 34f, y + 11f), 2f, 2f, topAccent)

                canvas.drawText(spec.label, left + 10f, y + 24f, mutedPaint)
                val valPaint = Paint(valueBoldPaint).apply {
                    color = spec.accentColor
                    textSize = 10.5f
                }
                canvas.drawText(ellipsize(spec.value, valPaint, kpiWidth - 18f), left + 10f, y + 40f, valPaint)
                canvas.drawText(ellipsize(spec.sublabel, mutedPaint, kpiWidth - 18f), left + 10f, y + 53f, mutedPaint)
            }

            y += kpiHeight + 16f

            val hasMultiCurrencyOrForeign = payload.perCurrencyTotals.size > 1 ||
                payload.perCurrencyTotals.any { !it.currencyCode.equals(payload.mainCurrencyCode, ignoreCase = true) }
            if (hasMultiCurrencyOrForeign && payload.perCurrencyTotals.isNotEmpty()) {
                val stripTitle = if (isBn) "মুদ্রাভিত্তিক প্রকৃত সারসংক্ষেপ" else "Per-Currency Activity Summary"
                canvas.drawText(stripTitle, MARGIN_X, y + 6f, sectionTitlePaint)
                y += 14f
                payload.perCurrencyTotals.forEach { curSummary ->
                    val curBadge = AccountNameResolver.currencyShortLabel(curSummary.currencyCode)
                    val incStr = BdtFormatter.formatCurrency(curSummary.income, curSummary.currencyCode, AppLanguage.EN)
                    val expStr = BdtFormatter.formatCurrency(curSummary.expense, curSummary.currencyCode, AppLanguage.EN)
                    val trStr = BdtFormatter.formatCurrency(curSummary.transferOut, curSummary.currencyCode, AppLanguage.EN)
                    val summaryLine = if (isBn) {
                        "$curBadge  •  আয়: $incStr   |   ব্যয়: $expStr   |   ট্রান্সফার: $trStr"
                    } else {
                        "$curBadge  •  Income: $incStr   |   Expense: $expStr   |   Transfers: $trStr"
                    }
                    canvas.drawText(ellipsize(summaryLine, bodyPaint, CONTENT_WIDTH), MARGIN_X, y + 6f, bodyPaint)
                    y += 15f
                }
                y += 8f
            } else {
                y += 6f
            }

            // Section 2: Category Expense Breakdown
            val catSectionTitle = if (isBn) "খাতভিত্তিক ব্যয়ের বিভাজন" else "Expense Breakdown by Category"
            canvas.drawText(catSectionTitle, MARGIN_X, y, sectionTitlePaint)
            y += 8f
            canvas.drawLine(MARGIN_X, y, PAGE_WIDTH - MARGIN_X, y, dividerPaint)
            y += 14f

            if (payload.categoryBreakdown.isEmpty()) {
                val emptyCatMsg = if (isBn) "এই মাসে কোনো ব্যয়ের রেকর্ড নেই।" else "No expense records for this month."
                canvas.drawText(emptyCatMsg, MARGIN_X, y + 4f, mutedPaint)
                y += 22f
            } else {
                val barTrackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#E2E8F0") }
                val barFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#0284C7") }
                val barColors = listOf(
                    Color.parseColor("#0284C7"),
                    Color.parseColor("#10B981"),
                    Color.parseColor("#F59E0B"),
                    Color.parseColor("#8B5CF6"),
                    Color.parseColor("#EC4899"),
                    Color.parseColor("#14B8A6")
                )

                payload.categoryBreakdown.take(8).forEachIndexed { idx, catSpend ->
                    if (y > PAGE_HEIGHT - 84f) {
                        y = startNextPage()
                    }
                    val catLabel = if (isBn) catSpend.category.bn else catSpend.category.en
                    val pctInt = (catSpend.share * 100f).toInt()
                    val amountLabel = "${BdtFormatter.formatCurrency(catSpend.amount, payload.mainCurrencyCode, AppLanguage.EN)}  ($pctInt%)"

                    canvas.drawText(ellipsize(catLabel, bodyPaint, 240f), MARGIN_X, y, bodyPaint)
                    val amtW = bodyPaint.measureText(amountLabel)
                    canvas.drawText(amountLabel, PAGE_WIDTH - MARGIN_X - amtW, y, bodyPaint)

                    val barTop = y + 5f
                    val barRect = RectF(MARGIN_X, barTop, PAGE_WIDTH - MARGIN_X, barTop + 6f)
                    canvas.drawRoundRect(barRect, 3f, 3f, barTrackPaint)

                    val fillW = (CONTENT_WIDTH * catSpend.share.coerceIn(0f, 1f)).coerceAtLeast(4f)
                    barFillPaint.color = barColors[idx % barColors.size]
                    canvas.drawRoundRect(
                        RectF(MARGIN_X, barTop, MARGIN_X + fillW, barTop + 6f),
                        3f,
                        3f,
                        barFillPaint
                    )
                    y += 24f
                }
            }

            y += 10f

            // Section 3: Monthly Transactions Ledger
            if (y > PAGE_HEIGHT - 120f) {
                y = startNextPage()
            }
            val txTitle = if (isBn) {
                "মাসিক লেনদেনের বিবরণী (${payload.transactions.size} টি)"
            } else {
                "Monthly Transactions Ledger (${payload.transactions.size})"
            }
            canvas.drawText(txTitle, MARGIN_X, y, sectionTitlePaint)
            y += 8f

            fun drawTableHeader(topY: Float): Float {
                val headerBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#F1F5F9") }
                canvas.drawRect(MARGIN_X, topY, PAGE_WIDTH - MARGIN_X, topY + 20f, headerBg)
                val thPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#334155")
                    textSize = 8.5f
                    typeface = boldTypeface
                }
                val textY = topY + 13.5f
                canvas.drawText(if (isBn) "তারিখ" else "Date", MARGIN_X + 6f, textY, thPaint)
                canvas.drawText(if (isBn) "বিবরণ ও খাত" else "Title & Category", MARGIN_X + 64f, textY, thPaint)
                canvas.drawText(if (isBn) "ওয়ালেট" else "Wallet", MARGIN_X + 296f, textY, thPaint)
                val amtHdr = if (isBn) "পরিমাণ" else "Amount"
                val amtHdrW = thPaint.measureText(amtHdr)
                canvas.drawText(amtHdr, PAGE_WIDTH - MARGIN_X - 6f - amtHdrW, textY, thPaint)
                return topY + 22f
            }

            y = drawTableHeader(y)

            if (payload.transactions.isEmpty()) {
                val noTxMsg = if (isBn) "এই মাসে কোনো লেনদেন পাওয়া যায়নি।" else "No transactions recorded for this month."
                canvas.drawText(noTxMsg, MARGIN_X + 6f, y + 14f, mutedPaint)
            } else {
                val rowDateFmt = SimpleDateFormat("dd MMM", Locale.ENGLISH)
                val altRowBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#F8FAFC") }
                val accountsById = payload.wallets.associateBy { it.id }

                payload.transactions.forEachIndexed { idx, tx ->
                    val dateText = rowDateFmt.format(Date(tx.timestamp))
                    val catText = if (isBn) tx.category.bn else tx.category.en
                    val isTransfer = tx.type == TransactionType.TRANSFER
                    val parsedTransfer = if (isTransfer) {
                        TransferNoteCodec.parse(tx, tx.toAccountId?.let { accountsById[it] }, payload.marketSnapshot)
                    } else null
                    val displayTitle = if (isTransfer) {
                        val (fromLabel, toLabel) = AccountNameResolver.resolveTransferEndpoints(tx, accountsById, payload.language)
                        "$fromLabel → $toLabel"
                    } else {
                        tx.title
                    }
                    val walletText = payload.accountNames[tx.accountId]
                        ?.takeIf { !isTransfer && it.isNotBlank() }
                        ?: AccountNameResolver.resolveTransactionWalletDisplay(tx, accountsById, payload.language)
                    val formattedCurrencyAmt = BdtFormatter.formatCurrency(tx.amount, tx.currencyCode, AppLanguage.EN)
                    val signedAmount = when (tx.type) {
                        TransactionType.INCOME -> "+$formattedCurrencyAmt"
                        TransactionType.EXPENSE -> "-$formattedCurrencyAmt"
                        TransactionType.TRANSFER -> formattedCurrencyAmt
                    }
                    val amtColor = when (tx.type) {
                        TransactionType.INCOME -> Color.parseColor("#059669")
                        TransactionType.EXPENSE -> Color.parseColor("#DC2626")
                        TransactionType.TRANSFER -> Color.parseColor("#0284C7")
                    }

                    val titleLines = wrapTwoLines(displayTitle, bodyPaint, 224f)
                    val walletLines = wrapTwoLines(walletText, mutedPaint, 132f)
                    val conversionText = if (parsedTransfer != null && parsedTransfer.isCrossCurrency) {
                        TransferNoteCodec.formatConversionSummary(parsedTransfer, AppLanguage.EN)
                    } else ""
                    val subtitleLine = if (conversionText.isNotBlank()) {
                        "$catText • $conversionText"
                    } else {
                        catText
                    }
                    val rowHeight = when {
                        titleLines.size > 1 && conversionText.isNotBlank() -> 42f
                        titleLines.size > 1 || walletLines.size > 1 -> 34f
                        else -> 27f
                    }

                    if (y + rowHeight > PAGE_HEIGHT - 44f) {
                        y = startNextPage()
                        y = drawTableHeader(y)
                    }
                    if (idx % 2 == 1) {
                        canvas.drawRect(MARGIN_X, y, PAGE_WIDTH - MARGIN_X, y + rowHeight, altRowBg)
                    }

                    canvas.drawText(dateText, MARGIN_X + 6f, y + 14f, bodyPaint)
                    canvas.drawText(titleLines[0], MARGIN_X + 64f, y + 12f, bodyPaint)
                    if (titleLines.size > 1) {
                        canvas.drawText(titleLines[1], MARGIN_X + 64f, y + 22f, bodyPaint)
                        canvas.drawText(ellipsize(subtitleLine, mutedPaint, 224f), MARGIN_X + 64f, y + 33f, mutedPaint)
                    } else {
                        canvas.drawText(ellipsize(subtitleLine, mutedPaint, 224f), MARGIN_X + 64f, y + 22f, mutedPaint)
                    }

                    canvas.drawText(walletLines[0], MARGIN_X + 296f, y + 14f, mutedPaint)
                    if (walletLines.size > 1) {
                        canvas.drawText(walletLines[1], MARGIN_X + 296f, y + 24f, mutedPaint)
                    }

                    val amtPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = amtColor
                        textSize = 9.2f
                        typeface = Typeface.create(monoTypeface, Typeface.BOLD)
                    }
                    val amtW = amtPaint.measureText(signedAmount)
                    canvas.drawText(signedAmount, PAGE_WIDTH - MARGIN_X - 6f - amtW, y + 14f, amtPaint)

                    canvas.drawLine(MARGIN_X, y + rowHeight, PAGE_WIDTH - MARGIN_X, y + rowHeight, dividerPaint)
                    y += rowHeight
                }
            }

            drawFooter(canvas, pageNumber)
            document.finishPage(page)

            FileOutputStream(outputFile).use { fos ->
                document.writeTo(fos)
            }
            return outputFile
        } finally {
            runCatching { document.close() }
        }
    }

    private fun writeSpecCompliantFallbackPdf(
        payload: MonthlyReportPayload,
        outputFile: File
    ): File {
        val dateFmt = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)
        val accountsById = payload.wallets.associateBy { it.id }
        val mainCurLabel = AccountNameResolver.currencyShortLabel(payload.mainCurrencyCode)
        val lines = buildList {
            add("Amar Hisab - Monthly Financial Summary (${payload.periodLabel})")
            add("Account Holder: ${toPdfAscii(payload.userName)} | Generated: ${dateFmt.format(Date(payload.generatedAtMillis))}")
            add("----------------------------------------------------------------------")
            add("Total Income : $mainCurLabel ${formatNumeric(payload.totalIncome)}")
            add("Total Expense: $mainCurLabel ${formatNumeric(payload.totalExpense)}")
            add("Net Cash Flow: $mainCurLabel ${formatNumeric(payload.netFlow)}")
            add("Daily Average: $mainCurLabel ${formatNumeric(payload.dailyAverage)} | Budget: $mainCurLabel ${formatNumeric(payload.monthlyBudget)}")
            add("Savings Rate : ${payload.savingsRatePercent?.let { "$it%" } ?: "N/A"} | Transactions: ${payload.transactions.size}")
            if (payload.perCurrencyTotals.isNotEmpty()) {
                add("Per-Currency Summary:")
                payload.perCurrencyTotals.forEach { cur ->
                    val label = AccountNameResolver.currencyShortLabel(cur.currencyCode)
                    add("  $label | Inc: ${formatNumeric(cur.income)} | Exp: ${formatNumeric(cur.expense)} | Trf: ${formatNumeric(cur.transferOut)}")
                }
            }
            add("----------------------------------------------------------------------")
            add("Category Expense Breakdown:")
            if (payload.categoryBreakdown.isEmpty()) {
                add("  No expenses recorded in this month.")
            } else {
                payload.categoryBreakdown.take(10).forEach { item ->
                    val pct = (item.share * 100f).toInt()
                    add("  - ${toPdfAscii(item.category.en)}: $mainCurLabel ${formatNumeric(item.amount)} ($pct%)")
                }
            }
            add("----------------------------------------------------------------------")
            add("Transactions:")
            payload.transactions.take(28).forEach { tx ->
                val d = dateFmt.format(Date(tx.timestamp))
                val sign = when (tx.type) {
                    TransactionType.INCOME -> "+"
                    TransactionType.EXPENSE -> "-"
                    TransactionType.TRANSFER -> ""
                }
                val curLabel = AccountNameResolver.currencyShortLabel(tx.currencyCode)
                val walletDisplay = AccountNameResolver.resolveTransactionWalletDisplay(tx, accountsById, AppLanguage.EN)
                val parsed = if (tx.type == TransactionType.TRANSFER) {
                    TransferNoteCodec.parse(tx, tx.toAccountId?.let { accountsById[it] }, payload.marketSnapshot)
                } else null
                val titleText = if (tx.type == TransactionType.TRANSFER) walletDisplay else tx.title
                val convSuffix = if (parsed != null && parsed.isCrossCurrency) {
                    " ${TransferNoteCodec.formatConversionAsciiSummary(parsed)}"
                } else ""
                add("  $d | ${toPdfAscii(titleText)} | ${toPdfAscii(walletDisplay)} | ${sign}$curLabel ${formatNumeric(tx.amount)}${toPdfAscii(convSuffix)}")
            }
        }

        val streamContent = buildString {
            appendLine("BT")
            appendLine("/F1 11 Tf")
            appendLine("42 790 Td")
            appendLine("15 TL")
            lines.forEachIndexed { idx, raw ->
                val escaped = pdfEscape(raw)
                if (idx == 0) {
                    appendLine("($escaped) Tj")
                } else {
                    appendLine("T* ($escaped) Tj")
                }
            }
            appendLine("ET")
        }
        val streamBytes = streamContent.toByteArray(Charsets.US_ASCII)

        val objects = listOf(
            "1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n",
            "2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n",
            "3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>\nendobj\n",
            "4 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>\nendobj\n",
            "5 0 obj\n<< /Length ${streamBytes.size} >>\nstream\n$streamContent\nendstream\nendobj\n"
        )

        val pdfBuilder = StringBuilder()
        pdfBuilder.append("%PDF-1.4\n")
        val offsets = mutableListOf<Int>()
        objects.forEach { obj ->
            offsets += pdfBuilder.length
            pdfBuilder.append(obj)
        }
        val xrefOffset = pdfBuilder.length
        pdfBuilder.append("xref\n0 ${objects.size + 1}\n")
        pdfBuilder.append("0000000000 65535 f \n")
        offsets.forEach { off ->
            pdfBuilder.append(String.format(Locale.US, "%010d 00000 n \n", off))
        }
        pdfBuilder.append("trailer\n<< /Size ${objects.size + 1} /Root 1 0 R >>\nstartxref\n$xrefOffset\n%%EOF\n")

        outputFile.writeBytes(pdfBuilder.toString().toByteArray(Charsets.US_ASCII))
        return outputFile
    }

    private fun toPdfAscii(input: String): String =
        input.map { ch -> if (ch.code in 32..126) ch else '?' }.joinToString("")

    private fun pdfEscape(input: String): String =
        toPdfAscii(input)
            .replace("\\", "\\\\")
            .replace("(", "\\(")
            .replace(")", "\\)")

    fun exportAndBuildShareIntent(
        context: Context,
        state: MainUiState,
        format: ReportExportFormat,
        strings: AppStrings,
        nowMillis: Long = System.currentTimeMillis(),
        marketSnapshot: MarketSnapshot = MarketSnapshot.defaultReference()
    ): Result<ExportSharePayload> = runCatching {
        val payload = buildMonthlyPayload(state, nowMillis, marketSnapshot)
        val reportsDir = File(context.cacheDir, "reports").apply { mkdirs() }
        val fileName = "amar_hisab_monthly_summary_${payload.fileSlug}.${format.extension}"
        val outFile = File(reportsDir, fileName)

        when (format) {
            ReportExportFormat.CSV -> {
                val csvContent = generateMonthlyCsv(payload)
                outFile.outputStream().use { out ->
                    // UTF-8 BOM ensures Excel / Sheets displays Bangla & ৳ cleanly
                    out.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
                    out.write(csvContent.toByteArray(Charsets.UTF_8))
                }
            }
            ReportExportFormat.PDF -> {
                generateMonthlyPdf(context, payload, outFile)
            }
        }

        val authority = "${context.packageName}.fileprovider"
        val contentUri = FileProvider.getUriForFile(context, authority, outFile)
        val exp = strings.reportExport
        val subject = "${exp.shareSubjectPrefix} — ${payload.periodLabel}"
        val shareBody = buildShareBodyText(payload)

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = format.mimeType
            putExtra(Intent.EXTRA_STREAM, contentUri)
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, shareBody)
            clipData = ClipData.newRawUri(fileName, contentUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        ExportSharePayload(
            format = format,
            file = outFile,
            uri = contentUri,
            mimeType = format.mimeType,
            shareIntent = shareIntent,
            chooserTitle = exp.shareChooserTitle,
            periodLabel = payload.periodLabel
        )
    }

    private fun buildShareBodyText(payload: MonthlyReportPayload): String {
        val lang = payload.language
        val income = BdtFormatter.formatCurrency(payload.totalIncome, payload.mainCurrencyCode, lang)
        val expense = BdtFormatter.formatCurrency(payload.totalExpense, payload.mainCurrencyCode, lang)
        val net = BdtFormatter.formatCurrency(payload.netFlow, payload.mainCurrencyCode, lang, includeSign = true)
        val base = if (lang == AppLanguage.BN) {
            "আমার হিসাব — মাসিক আর্থিক সারসংক্ষেপ (${payload.periodLabel})\nআয়: $income | ব্যয়: $expense | নিট প্রবাহ: $net"
        } else {
            "Amar Hisab — Monthly Financial Summary (${payload.periodLabel})\nIncome: $income | Expense: $expense | Net Flow: $net"
        }
        val foreignLines = payload.perCurrencyTotals
            .filter { !it.currencyCode.equals(payload.mainCurrencyCode, ignoreCase = true) }
            .map { cur ->
                val curLbl = AccountNameResolver.currencyShortLabel(cur.currencyCode)
                val trf = BdtFormatter.formatCurrency(cur.transferOut, cur.currencyCode, lang)
                val exp = BdtFormatter.formatCurrency(cur.expense, cur.currencyCode, lang)
                val inc = BdtFormatter.formatCurrency(cur.income, cur.currencyCode, lang)
                if (lang == AppLanguage.BN) {
                    "$curLbl: আয় $inc | ব্যয় $exp | ট্রান্সফার $trf"
                } else {
                    "$curLbl: Income $inc | Expense $exp | Transfers $trf"
                }
            }
        return if (foreignLines.isEmpty()) base else "$base\n${foreignLines.joinToString("\n")}"
    }

    private fun wrapTwoLines(text: String, paint: Paint, maxWidth: Float): List<String> {
        if (paint.measureText(text) <= maxWidth) return listOf(text)
        var splitIdx = text.length
        while (splitIdx > 1 && paint.measureText(text.substring(0, splitIdx)) > maxWidth) {
            splitIdx--
        }
        val spaceIdx = text.lastIndexOf(' ', splitIdx).takeIf { it > 3 }
        val breakAt = spaceIdx ?: splitIdx
        val firstLine = text.substring(0, breakAt).trimEnd()
        val remainder = text.substring(breakAt).trimStart()
        if (remainder.isBlank()) return listOf(ellipsize(firstLine, paint, maxWidth))
        return listOf(firstLine, ellipsize(remainder, paint, maxWidth))
    }

    private fun ellipsize(text: String, paint: Paint, maxWidth: Float): String {
        if (paint.measureText(text) <= maxWidth) return text
        val ellipsis = "…"
        var end = text.length
        while (end > 1 && paint.measureText(text.substring(0, end) + ellipsis) > maxWidth) {
            end--
        }
        return text.substring(0, end).trimEnd() + ellipsis
    }

    private fun formatNumeric(value: Double): String =
        String.format(Locale.US, "%.2f", value)

    private fun csvEscape(raw: String): String {
        val cleaned = raw.replace("\"", "\"\"")
        return "\"$cleaned\""
    }
}
