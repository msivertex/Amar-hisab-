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
    CSV("csv", "text/csv"),
    QR_PNG("png", "image/png")
}

data class CurrencyMonthlyTotals(
    val currencyCode: String,
    val income: Double,
    val expense: Double,
    val transferOut: Double
) {
    val netFlow: Double get() = (income - expense).coerceAtLeast(0.0)
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
    val marketSnapshot: MarketSnapshot = MarketSnapshot.defaultReference(),
    val periodType: ReportPeriodType = ReportPeriodType.MONTHLY,
    val verificationId: String = "",
    val qrPayload: String = ""
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
    ): MonthlyReportPayload = buildReportPayload(
        state = state,
        periodType = ReportPeriodType.MONTHLY,
        nowMillis = nowMillis,
        marketSnapshot = marketSnapshot
    )

    fun buildReportPayload(
        state: MainUiState,
        periodType: ReportPeriodType = ReportPeriodType.MONTHLY,
        nowMillis: Long = System.currentTimeMillis(),
        marketSnapshot: MarketSnapshot = MarketSnapshot.defaultReference()
    ): MonthlyReportPayload {
        val lang = state.prefs.language
        val locale = Locale.forLanguageTag(lang.localeTag)

        val startCal = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val endCal = Calendar.getInstance()
        val prevStartCal = Calendar.getInstance()

        val periodLabel: String
        val fileSlug: String

        when (periodType) {
            ReportPeriodType.DAILY -> {
                endCal.timeInMillis = startCal.timeInMillis
                endCal.add(Calendar.DAY_OF_YEAR, 1)
                prevStartCal.timeInMillis = startCal.timeInMillis
                prevStartCal.add(Calendar.DAY_OF_YEAR, -1)
                val rawDate = SimpleDateFormat("dd MMM yyyy", locale).format(startCal.time)
                periodLabel = if (lang.isBangla) BdtFormatter.toBanglaDigits(rawDate) else rawDate
                fileSlug = SimpleDateFormat("yyyy_MM_dd", Locale.US).format(startCal.time)
            }
            ReportPeriodType.WEEKLY -> {
                val targetStartDow = when (state.prefs.startOfWeek) {
                    com.example.data.model.StartOfWeek.SATURDAY -> Calendar.SATURDAY
                    com.example.data.model.StartOfWeek.SUNDAY -> Calendar.SUNDAY
                    com.example.data.model.StartOfWeek.MONDAY -> Calendar.MONDAY
                    com.example.data.model.StartOfWeek.TUESDAY -> Calendar.TUESDAY
                    com.example.data.model.StartOfWeek.WEDNESDAY -> Calendar.WEDNESDAY
                    com.example.data.model.StartOfWeek.THURSDAY -> Calendar.THURSDAY
                    com.example.data.model.StartOfWeek.FRIDAY -> Calendar.FRIDAY
                }
                while (startCal.get(Calendar.DAY_OF_WEEK) != targetStartDow) {
                    startCal.add(Calendar.DAY_OF_YEAR, -1)
                }
                endCal.timeInMillis = startCal.timeInMillis
                endCal.add(Calendar.DAY_OF_YEAR, 7)
                prevStartCal.timeInMillis = startCal.timeInMillis
                prevStartCal.add(Calendar.DAY_OF_YEAR, -7)
                val endInclusive = Calendar.getInstance().apply {
                    timeInMillis = endCal.timeInMillis - 1L
                }
                val startStr = SimpleDateFormat("dd MMM", locale).format(startCal.time)
                val endStr = SimpleDateFormat("dd MMM yyyy", locale).format(endInclusive.time)
                val rawRange = "$startStr – $endStr"
                periodLabel = if (lang.isBangla) BdtFormatter.toBanglaDigits(rawRange) else rawRange
                fileSlug = "week_${SimpleDateFormat("yyyy_MM_dd", Locale.US).format(startCal.time)}"
            }
            ReportPeriodType.MONTHLY -> {
                val monthOffset = if (state.insightFilter == InsightTimeFilter.LAST_MONTH) {
                    -1
                } else {
                    state.insightMonthOffset
                }
                startCal.add(Calendar.MONTH, monthOffset)
                startCal.set(Calendar.DAY_OF_MONTH, 1)
                endCal.timeInMillis = startCal.timeInMillis
                endCal.add(Calendar.MONTH, 1)
                prevStartCal.timeInMillis = startCal.timeInMillis
                prevStartCal.add(Calendar.MONTH, -1)
                val rawMonth = SimpleDateFormat("MMMM yyyy", locale).format(startCal.time)
                periodLabel = if (lang.isBangla) BdtFormatter.toBanglaDigits(rawMonth) else rawMonth
                fileSlug = SimpleDateFormat("yyyy_MM", Locale.US).format(startCal.time)
            }
        }
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

        val elapsedDays = when (periodType) {
            ReportPeriodType.DAILY -> 1
            ReportPeriodType.WEEKLY -> {
                val elapsed = (((nowMillis - startCal.timeInMillis) / (24L * 60L * 60L * 1000L)).toInt() + 1)
                elapsed.coerceIn(1, 7)
            }
            ReportPeriodType.MONTHLY -> {
                val daysInMonth = startCal.getActualMaximum(Calendar.DAY_OF_MONTH).coerceAtLeast(1)
                val nowCal = Calendar.getInstance().apply { timeInMillis = nowMillis }
                val isCurrentMonth = startCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR) &&
                    startCal.get(Calendar.MONTH) == nowCal.get(Calendar.MONTH)
                if (isCurrentMonth) nowCal.get(Calendar.DAY_OF_MONTH).coerceIn(1, daysInMonth) else daysInMonth
            }
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
            (((totalIncome - totalExpense) / totalIncome) * 100.0).toInt().coerceIn(0, 100)
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
            acc.id to AccountNameResolver.resolveAccountName(acc, lang)
        }
        val effectiveUser = state.prefs.userName.ifBlank {
            if (lang.isBangla) "গেস্ট ইউজার" else "Guest"
        }

        val periodTag = when (periodType) {
            ReportPeriodType.DAILY -> "D"
            ReportPeriodType.WEEKLY -> "W"
            ReportPeriodType.MONTHLY -> "M"
        }
        val checksum = abs((fileSlug + "|" + monthTx.size + "|" + totalIncome.toLong() + "|" + totalExpense.toLong()).hashCode()) % 100_000
        val verificationId = "AH-$periodTag-${fileSlug.replace("_", "")}-${String.format(Locale.US, "%05d", checksum)}"

        val draftPayload = MonthlyReportPayload(
            periodLabel = periodLabel,
            fileSlug = fileSlug,
            generatedAtMillis = nowMillis,
            userName = effectiveUser,
            language = lang,
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            netFlow = (totalIncome - totalExpense).coerceAtLeast(0.0),
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
            marketSnapshot = marketSnapshot,
            periodType = periodType,
            verificationId = verificationId
        )

        return draftPayload.copy(qrPayload = buildQrPayload(draftPayload))
    }

    /**
     * Builds a clean, scannable UTF-8 QR verification payload localized in the active app language.
     */
    fun buildQrPayload(payload: MonthlyReportPayload): String {
        val lang = payload.language
        val copy = ReportLocalizedStrings.forLanguage(lang)
        val inc = BdtFormatter.formatCurrency(payload.totalIncome, payload.mainCurrencyCode, lang)
        val exp = BdtFormatter.formatCurrency(payload.totalExpense, payload.mainCurrencyCode, lang)
        val net = BdtFormatter.formatBalance(payload.netFlow, payload.mainCurrencyCode, lang)
        val countText = if (lang.isBangla) {
            BdtFormatter.toBanglaDigits(payload.transactions.size.toString())
        } else {
            payload.transactions.size.toString()
        }
        return buildString {
            appendLine("${copy.brandTitle} • ${copy.reportTitleFor(payload.periodType)}")
            appendLine("${copy.periodLabel}: ${payload.periodLabel}")
            appendLine("${copy.accountHolderLabel}: ${payload.userName}")
            appendLine("${copy.totalIncomeLabel}: $inc")
            appendLine("${copy.totalExpenseLabel}: $exp")
            appendLine("${copy.netFlowLabel}: $net")
            appendLine("${copy.transactionsLedgerTitle}: $countText ${copy.txCountSuffix}")
            append("${copy.qrVerifyCodeLabel}: ${payload.verificationId} [${lang.code}]")
        }
    }

    /**
     * Builds a localized QR verification payload for an individual transaction voucher.
     */
    fun buildTransactionVoucherQrPayload(
        tx: TransactionEntity,
        walletLabel: String,
        language: AppLanguage
    ): String {
        val copy = ReportLocalizedStrings.forLanguage(language)
        val locale = Locale.forLanguageTag(language.localeTag)
        val dateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", locale).format(Date(tx.timestamp))
        val formattedDate = if (language.isBangla) BdtFormatter.toBanglaDigits(dateStr) else dateStr
        val amountStr = BdtFormatter.formatCurrency(tx.amount, tx.currencyCode, language)
        val verifyId = "AH-TX-${tx.id}-${abs((tx.timestamp xor tx.amount.toBits()).toInt()) % 100_000}"
        val catName = if (language.isBangla) tx.category.bn else tx.category.en
        return buildString {
            appendLine("${copy.brandTitle} • ${copy.voucherQrTitle}")
            appendLine("${copy.colTitleCategory}: ${tx.title} ($catName)")
            appendLine("${copy.colAmount}: $amountStr (${tx.currencyCode})")
            appendLine("${copy.colWallet}: $walletLabel")
            appendLine("${copy.colDate}: $formattedDate")
            append("${copy.qrVerifyCodeLabel}: $verifyId [${language.code}]")
        }
    }

    fun generateMonthlyCsv(payload: MonthlyReportPayload): String {
        val copy = ReportLocalizedStrings.forLanguage(payload.language)
        val dateFmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
        val accountsById = payload.wallets.associateBy { it.id }
        val sb = StringBuilder()

        // Section 1: Localized Financial Summary
        sb.appendLine("Section,Metric,Value")
        sb.appendLine("Summary,App,${csvEscape(copy.brandTitle)}")
        sb.appendLine("Summary,Report_Type,${csvEscape(copy.reportTitleFor(payload.periodType))}")
        sb.appendLine("Summary,Language,${csvEscape("${payload.language.nativeName} (${payload.language.code})")}")
        sb.appendLine("Summary,Report_Period,${csvEscape(payload.periodLabel)}")
        sb.appendLine("Summary,Verification_ID,${csvEscape(payload.verificationId)}")
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
                val catLabel = if (payload.language.isBangla) {
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
            val catName = if (payload.language.isBangla) tx.category.bn else tx.category.en
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
        val lang = payload.language
        val isBn = lang.isBangla
        val copy = ReportLocalizedStrings.forLanguage(lang)
        val locale = Locale.forLanguageTag(lang.localeTag)

        // Use Hind Siliguri for Bangla & English; use system SANS_SERIF for global scripts (Arabic, Hindi, Urdu, CJK, Cyrillic, etc.)
        // so Android Skia PDF Canvas renders every international script cleanly without missing glyphs.
        val baseTypeface = if (lang.isBangla || lang == AppLanguage.EN) {
            runCatching { ResourcesCompat.getFont(context, R.font.hind_siliguri) }.getOrNull() ?: Typeface.SANS_SERIF
        } else {
            Typeface.SANS_SERIF
        }
        val boldTypeface = Typeface.create(baseTypeface, Typeface.BOLD)
        val monoTypeface = if (lang.isBangla || lang == AppLanguage.EN) {
            runCatching { ResourcesCompat.getFont(context, R.font.jetbrains_mono) }.getOrNull() ?: Typeface.MONOSPACE
        } else {
            Typeface.SANS_SERIF
        }

        val document = PdfDocument()
        try {
            var pageNumber = 1
            var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            var page = document.startPage(pageInfo)
            var canvas = page.canvas

            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 16.5f
                typeface = boldTypeface
            }
            val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#DBEAFE")
                textSize = 9.8f
                typeface = baseTypeface
            }
            val sectionTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#0F172A")
                textSize = 12f
                typeface = boldTypeface
            }
            val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#1E293B")
                textSize = 9.3f
                typeface = baseTypeface
            }
            val mutedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#64748B")
                textSize = 8.3f
                typeface = baseTypeface
            }
            val valueBoldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#0F172A")
                textSize = 11f
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

            val rawGenDate = SimpleDateFormat("dd MMM yyyy, hh:mm a", locale).format(Date(payload.generatedAtMillis))
            val generatedDateStr = if (isBn) BdtFormatter.toBanglaDigits(rawGenDate) else rawGenDate

            fun drawFooter(c: Canvas, pageNum: Int) {
                val footerY = PAGE_HEIGHT - 24f
                c.drawLine(MARGIN_X, footerY - 12f, PAGE_WIDTH - MARGIN_X, footerY - 12f, dividerPaint)
                val leftText = "${copy.footerText}  •  ${lang.nativeName}"
                c.drawText(ellipsize(leftText, mutedPaint, CONTENT_WIDTH - 64f), MARGIN_X, footerY, mutedPaint)
                val rightText = if (isBn) "পৃষ্ঠা ${BdtFormatter.toBanglaDigits(pageNum.toString())}" else "Page $pageNum"
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
                val contTitle = "${copy.brandTitle} • ${copy.reportTitleFor(payload.periodType)} (${payload.periodLabel})"
                val smallWhite = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.WHITE
                    textSize = 10.2f
                    typeface = boldTypeface
                }
                canvas.drawText(ellipsize(contTitle, smallWhite, CONTENT_WIDTH), MARGIN_X, 27f, smallWhite)
                return 64f
            }

            // Top Branded Header Banner with Embedded Scannable Verification QR Code
            val bannerHeight = 114f
            val bannerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#0F172A") }
            canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), bannerHeight, bannerPaint)
            val accentStripePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#0284C7") }
            canvas.drawRect(0f, bannerHeight - 4f, PAGE_WIDTH.toFloat(), bannerHeight, accentStripePaint)

            val qrBoxSize = 78f
            val qrCardLeft = PAGE_WIDTH - MARGIN_X - qrBoxSize - 8f
            val textMaxWidth = qrCardLeft - MARGIN_X - 14f

            val appBannerTitle = "${copy.brandTitle} • ${copy.reportTitleFor(payload.periodType)}"
            canvas.drawText(ellipsize(appBannerTitle, titlePaint, textMaxWidth), MARGIN_X, 34f, titlePaint)
            val holderLine = "${copy.accountHolderLabel}: ${payload.userName}  •  ${copy.generatedLabel}: $generatedDateStr"
            canvas.drawText(ellipsize(holderLine, subtitlePaint, textMaxWidth), MARGIN_X, 55f, subtitlePaint)

            val pillBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#1E3A5F") }
            val periodText = "${copy.periodLabel}: ${payload.periodLabel}  •  ${lang.nativeName}"
            val pillWidth = (subtitlePaint.measureText(periodText) + 22f).coerceAtMost(textMaxWidth)
            val pillRect = RectF(MARGIN_X, 66f, MARGIN_X + pillWidth, 88f)
            canvas.drawRoundRect(pillRect, 11f, 11f, pillBg)
            canvas.drawText(ellipsize(periodText, subtitlePaint, pillWidth - 18f), MARGIN_X + 10f, 80.5f, subtitlePaint)

            val verifyLine = "${copy.qrVerifyCodeLabel}: ${payload.verificationId}  •  ${copy.qrScanCaption}"
            val tinyBannerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#93C5FD")
                textSize = 7.6f
                typeface = baseTypeface
            }
            canvas.drawText(ellipsize(verifyLine, tinyBannerPaint, textMaxWidth), MARGIN_X, 102f, tinyBannerPaint)

            // Draw crisp white QR card on right side of banner
            val qrCardBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
            val qrCardRect = RectF(qrCardLeft, 14f, qrCardLeft + qrBoxSize + 8f, 14f + qrBoxSize + 8f)
            canvas.drawRoundRect(qrCardRect, 8f, 8f, qrCardBg)
            val qrContent = payload.qrPayload.ifBlank { buildQrPayload(payload) }
            val qrBitmap = QrCodeBitmapGenerator.generateQrBitmap(qrContent, sizePx = 220)
            canvas.drawBitmap(
                qrBitmap,
                null,
                RectF(qrCardLeft + 4f, 18f, qrCardLeft + qrBoxSize + 4f, 18f + qrBoxSize),
                null
            )
            qrBitmap.recycle()

            var y = bannerHeight + 16f

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
                    label = copy.totalIncomeLabel,
                    value = BdtFormatter.formatCurrency(payload.totalIncome, payload.mainCurrencyCode, lang),
                    sublabel = "${copy.savingsRateLabel}: $savingsText",
                    accentColor = Color.parseColor("#059669")
                ),
                KpiCardSpec(
                    label = copy.totalExpenseLabel,
                    value = BdtFormatter.formatCurrency(payload.totalExpense, payload.mainCurrencyCode, lang),
                    sublabel = deltaSub,
                    accentColor = Color.parseColor("#DC2626")
                ),
                KpiCardSpec(
                    label = copy.netFlowLabel,
                    value = BdtFormatter.formatCurrency(payload.netFlow, payload.mainCurrencyCode, lang, includeSign = true),
                    sublabel = "${payload.transactions.size} ${copy.txCountSuffix}",
                    accentColor = if (payload.netFlow >= 0) Color.parseColor("#0284C7") else Color.parseColor("#DC2626")
                ),
                KpiCardSpec(
                    label = copy.dailyAverageLabel,
                    value = BdtFormatter.formatCurrency(payload.dailyAverage, payload.mainCurrencyCode, lang),
                    sublabel = "${copy.budgetUsedLabel}: $budgetPct%",
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

                canvas.drawText(ellipsize(spec.label, mutedPaint, kpiWidth - 18f), left + 10f, y + 24f, mutedPaint)
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
                canvas.drawText(copy.perCurrencyTitle, MARGIN_X, y + 6f, sectionTitlePaint)
                y += 14f
                payload.perCurrencyTotals.forEach { curSummary ->
                    val curBadge = AccountNameResolver.currencyShortLabel(curSummary.currencyCode)
                    val incStr = BdtFormatter.formatCurrency(curSummary.income, curSummary.currencyCode, lang)
                    val expStr = BdtFormatter.formatCurrency(curSummary.expense, curSummary.currencyCode, lang)
                    val trStr = BdtFormatter.formatCurrency(curSummary.transferOut, curSummary.currencyCode, lang)
                    val summaryLine = "$curBadge  •  ${copy.totalIncomeLabel}: $incStr  |  ${copy.totalExpenseLabel}: $expStr  |  ${copy.transferShortLabel}: $trStr"
                    canvas.drawText(ellipsize(summaryLine, bodyPaint, CONTENT_WIDTH), MARGIN_X, y + 6f, bodyPaint)
                    y += 15f
                }
                y += 8f
            } else {
                y += 6f
            }

            // Section 2: Category Expense Breakdown
            canvas.drawText(copy.categoryBreakdownTitle, MARGIN_X, y, sectionTitlePaint)
            y += 8f
            canvas.drawLine(MARGIN_X, y, PAGE_WIDTH - MARGIN_X, y, dividerPaint)
            y += 14f

            if (payload.categoryBreakdown.isEmpty()) {
                canvas.drawText(copy.noExpenseMessage, MARGIN_X, y + 4f, mutedPaint)
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
                    val amountLabel = "${BdtFormatter.formatCurrency(catSpend.amount, payload.mainCurrencyCode, lang)}  ($pctInt%)"

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

            // Section 3: Transactions Ledger
            if (y > PAGE_HEIGHT - 120f) {
                y = startNextPage()
            }
            val txCountStr = if (isBn) BdtFormatter.toBanglaDigits(payload.transactions.size.toString()) else payload.transactions.size.toString()
            val txTitle = "${copy.transactionsLedgerTitle} ($txCountStr)"
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
                canvas.drawText(copy.colDate, MARGIN_X + 6f, textY, thPaint)
                canvas.drawText(copy.colTitleCategory, MARGIN_X + 64f, textY, thPaint)
                canvas.drawText(copy.colWallet, MARGIN_X + 296f, textY, thPaint)
                val amtHdrW = thPaint.measureText(copy.colAmount)
                canvas.drawText(copy.colAmount, PAGE_WIDTH - MARGIN_X - 6f - amtHdrW, textY, thPaint)
                return topY + 22f
            }

            y = drawTableHeader(y)

            if (payload.transactions.isEmpty()) {
                canvas.drawText(copy.noTransactionsMessage, MARGIN_X + 6f, y + 14f, mutedPaint)
            } else {
                val rowDateFmt = SimpleDateFormat("dd MMM", locale)
                val altRowBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#F8FAFC") }
                val accountsById = payload.wallets.associateBy { it.id }

                payload.transactions.forEachIndexed { idx, tx ->
                    val rawRowDate = rowDateFmt.format(Date(tx.timestamp))
                    val dateText = if (isBn) BdtFormatter.toBanglaDigits(rawRowDate) else rawRowDate
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
                    val formattedCurrencyAmt = BdtFormatter.formatCurrency(tx.amount, tx.currencyCode, lang)
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
        marketSnapshot: MarketSnapshot = MarketSnapshot.defaultReference(),
        periodType: ReportPeriodType = ReportPeriodType.MONTHLY
    ): Result<ExportSharePayload> = runCatching {
        val payload = buildReportPayload(state, periodType, nowMillis, marketSnapshot)
        val copy = ReportLocalizedStrings.forLanguage(payload.language)
        val reportsDir = File(context.cacheDir, "reports").apply { mkdirs() }
        val periodPrefix = when (periodType) {
            ReportPeriodType.DAILY -> "daily_receipt"
            ReportPeriodType.WEEKLY -> "weekly_receipt"
            ReportPeriodType.MONTHLY -> "monthly_summary"
        }
        val safeLangTag = payload.language.code.lowercase(Locale.US).replace("-", "_")
        val fileName = "amar_hisab_${periodPrefix}_${payload.fileSlug}_${safeLangTag}.${format.extension}"
        val outFile = File(reportsDir, fileName)

        when (format) {
            ReportExportFormat.CSV -> {
                val csvContent = generateMonthlyCsv(payload)
                outFile.outputStream().use { out ->
                    // UTF-8 BOM ensures Excel / Sheets displays Bangla, Arabic, Hindi, CJK & currency symbols cleanly
                    out.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
                    out.write(csvContent.toByteArray(Charsets.UTF_8))
                }
            }
            ReportExportFormat.PDF -> {
                generateMonthlyPdf(context, payload, outFile)
            }
            ReportExportFormat.QR_PNG -> {
                val inc = BdtFormatter.formatCurrency(payload.totalIncome, payload.mainCurrencyCode, payload.language)
                val expAmt = BdtFormatter.formatCurrency(payload.totalExpense, payload.mainCurrencyCode, payload.language)
                val net = BdtFormatter.formatCurrency(payload.netFlow, payload.mainCurrencyCode, payload.language, includeSign = true)
                QrCodeBitmapGenerator.renderQrReceiptPngToFile(
                    outFile = outFile,
                    qrPayload = payload.qrPayload.ifBlank { buildQrPayload(payload) },
                    brandHeader = copy.brandTitle,
                    reportTitle = copy.reportTitleFor(periodType),
                    periodLabel = payload.periodLabel,
                    summaryLine = "${copy.totalIncomeLabel}: $inc  |  ${copy.totalExpenseLabel}: $expAmt  |  ${copy.netFlowLabel}: $net",
                    verificationCaption = "${copy.qrScanCaption} • ${payload.verificationId}",
                    languageBadge = payload.language.nativeName
                )
            }
        }

        val authority = "${context.packageName}.fileprovider"
        val contentUri = runCatching {
            FileProvider.getUriForFile(context, authority, outFile)
        }.getOrElse {
            android.net.Uri.fromFile(outFile)
        }
        val exp = strings.reportExport
        val subject = "${copy.shareTitlePrefix} (${copy.periodTabLabel(periodType)}) — ${payload.periodLabel}"
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
        val copy = ReportLocalizedStrings.forLanguage(lang)
        val income = BdtFormatter.formatCurrency(payload.totalIncome, payload.mainCurrencyCode, lang)
        val expense = BdtFormatter.formatCurrency(payload.totalExpense, payload.mainCurrencyCode, lang)
        val net = BdtFormatter.formatCurrency(payload.netFlow, payload.mainCurrencyCode, lang, includeSign = true)
        val base = "${copy.brandTitle} — ${copy.reportTitleFor(payload.periodType)} (${payload.periodLabel})\n" +
            "${copy.totalIncomeLabel}: $income | ${copy.totalExpenseLabel}: $expense | ${copy.netFlowLabel}: $net\n" +
            "${copy.qrVerifyCodeLabel}: ${payload.verificationId}"
        val foreignLines = payload.perCurrencyTotals
            .filter { !it.currencyCode.equals(payload.mainCurrencyCode, ignoreCase = true) }
            .map { cur ->
                val curLbl = AccountNameResolver.currencyShortLabel(cur.currencyCode)
                val trf = BdtFormatter.formatCurrency(cur.transferOut, cur.currencyCode, lang)
                val exp = BdtFormatter.formatCurrency(cur.expense, cur.currencyCode, lang)
                val inc = BdtFormatter.formatCurrency(cur.income, cur.currencyCode, lang)
                "$curLbl: ${copy.totalIncomeLabel} $inc | ${copy.totalExpenseLabel} $exp | ${copy.transferShortLabel} $trf"
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
