package com.example.data.market

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.localization.AppLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.pow
import kotlin.math.roundToLong

private val Context.marketCacheDataStore by preferencesDataStore(name = "amar_hisab_market_cache")

/**
 * Transparent data-freshness indicator shown in Live Market panels so users always know
 * whether figures are Live Synced, Cached / Offline, or Reference / Calculator Estimates.
 */
enum class MarketDataStatus {
    LIVE_SYNCED,
    CACHED_OFFLINE,
    REFERENCE_ESTIMATE
}

/**
 * Multi-region currencies synchronized with the unified 28-country dataset for flexible any-to-any conversion.
 */
enum class MarketCurrency(
    val code: String,
    val symbol: String,
    val flag: String,
    val nameEn: String,
    val nameBn: String,
    val countryEn: String,
    val countryBn: String,
    val defaultPerUsd: Double
) {
    BDT("BDT", "৳", "🇧🇩", "Bangladeshi Taka", "বাংলাদেশী টাকা", "Bangladesh", "বাংলাদেশ", 121.50),
    USD("USD", "$", "🇺🇸", "US Dollar", "মার্কিন ডলার", "USA", "যুক্তরাষ্ট্র", 1.0),
    EUR("EUR", "€", "🇪🇺", "Euro", "ইউরো", "Italy / Europe", "ইতালি / ইউরোপ", 0.92),
    GBP("GBP", "£", "🇬🇧", "British Pound", "ব্রিটিশ পাউন্ড", "UK / England", "যুক্তরাজ্য / ইংল্যান্ড", 0.785),
    KWD("KWD", "د.ك", "🇰🇼", "Kuwaiti Dinar", "কুয়েতি দিনার", "Kuwait", "কুয়েত", 0.307),
    SAR("SAR", "SAR", "🇸🇦", "Saudi Riyal", "সৌদি রিয়াল", "Saudi Arabia", "সৌদি আরব", 3.75),
    AED("AED", "AED", "🇦🇪", "UAE Dirham", "ইউএই দিরহাম", "UAE (Dubai)", "সংযুক্ত আরব আমিরাত", 3.6725),
    QAR("QAR", "QAR", "🇶🇦", "Qatari Riyal", "কাতারি রিয়াল", "Qatar", "কাতার", 3.64),
    OMR("OMR", "OMR", "🇴🇲", "Omani Rial", "ওমানি রিয়াল", "Oman", "ওমান", 0.3845),
    BHD("BHD", "BHD", "🇧🇭", "Bahraini Dinar", "বাহরাইনি দিনার", "Bahrain", "বাহরাইন", 0.376),
    MYR("MYR", "MYR", "🇲🇾", "Malaysian Ringgit", "মালয়েশিয়ান রিঙ্গিত", "Malaysia", "মালয়েশিয়া", 4.42),
    SGD("SGD", "S$", "🇸🇬", "Singapore Dollar", "সিঙ্গাপুর ডলার", "Singapore", "সিঙ্গাপুর", 1.34),
    KRW("KRW", "₩", "🇰🇷", "South Korean Won", "দক্ষিণ কোরিয়ান ওন", "South Korea", "দক্ষিণ কোরিয়া", 1395.0),
    JPY("JPY", "¥", "🇯🇵", "Japanese Yen", "জাপানি ইয়েন", "Japan", "জাপান", 154.0),
    AUD("AUD", "A$", "🇦🇺", "Australian Dollar", "অস্ট্রেলিয়ান ডলার", "Australia", "অস্ট্রেলিয়া", 1.54),
    CAD("CAD", "C$", "🇨🇦", "Canadian Dollar", "কানাডিয়ান ডলার", "Canada", "কানাডা", 1.40),
    INR("INR", "₹", "🇮🇳", "Indian Rupee", "ভারতীয় রুপি", "India", "ভারত", 84.40),
    PKR("PKR", "Rs", "🇵🇰", "Pakistani Rupee", "পাকিস্তানি রুপি", "Pakistan", "পাকিস্তান", 278.00),
    LKR("LKR", "Rs", "🇱🇰", "Sri Lankan Rupee", "শ্রীলঙ্কান রুপি", "Sri Lanka", "শ্রীলঙ্কা", 292.50),
    NPR("NPR", "₨", "🇳🇵", "Nepalese Rupee", "নেপালি রুপি", "Nepal", "নেপাল", 135.04),
    BTN("BTN", "Nu.", "🇧🇹", "Bhutanese Ngultrum", "ভুটানি গুলট্রাম", "Bhutan", "ভুটান", 84.40),
    MMK("MMK", "K", "🇲🇲", "Burmese Kyat", "বার্মিজ কিয়াত", "Myanmar", "মায়ানমার", 2100.0),
    MVR("MVR", "Rf", "🇲🇻", "Maldivian Rufiyaa", "মালদ্বীভিয়ান রুফিয়া", "Maldives", "মালদ্বীপ", 15.42);

    fun displayName(language: AppLanguage): String = if (language == AppLanguage.BN) nameBn else nameEn
    fun countryName(language: AppLanguage): String = if (language == AppLanguage.BN) countryBn else countryEn

    companion object {
        fun fromCode(code: String): MarketCurrency? =
            entries.firstOrNull { it.code.equals(code.trim(), ignoreCase = true) }
    }
}

enum class MarketToolTab(val id: String) {
    CURRENCY("currency"),
    GOLD("gold"),
    ESSENTIALS("essentials"),
    BANK_FDR_DPS("bank"),
    REMITTANCE("remittance"),
    MFS_FEE("mfs")
}

enum class GoldCarat(
    val purityFactor: Double,
    val labelEn: String,
    val labelBn: String
) {
    K24(1.0000, "24K Pure (999)", "২৪ ক্যারেট (খাঁটি)"),
    K22(0.9167, "22K Hallmark", "২২ ক্যারেট হলমার্ক"),
    K21(0.8750, "21K Hallmark", "২১ ক্যারেট হলমার্ক"),
    K18(0.7500, "18K Hallmark", "১৮ ক্যারেট হলমার্ক"),
    TRADITIONAL(0.6250, "Traditional", "সনাতন পদ্ধতি");

    fun label(language: AppLanguage): String = if (language == AppLanguage.BN) labelBn else labelEn
}

enum class EssentialGroup(val labelEn: String, val labelBn: String) {
    ALL("All", "সব"),
    GRAINS("Rice & Flour", "চাল ও আটা"),
    OIL_LENTILS("Oil & Lentils", "তেল ও ডাল"),
    PROTEIN("Protein & Eggs", "প্রোটিন ও ডিম"),
    PRODUCE("Veggies & Spices", "সবজি ও মসলা");

    fun label(language: AppLanguage): String = if (language == AppLanguage.BN) labelBn else labelEn
}

data class EssentialGroceryItem(
    val id: String,
    val group: EssentialGroup,
    val nameEn: String,
    val nameBn: String,
    val unitEn: String,
    val unitBn: String,
    val minPriceBdt: Int,
    val maxPriceBdt: Int
) {
    fun name(language: AppLanguage): String = if (language == AppLanguage.BN) nameBn else nameEn
    fun unit(language: AppLanguage): String = if (language == AppLanguage.BN) unitBn else unitEn
    val midPriceBdt: Double get() = (minPriceBdt + maxPriceBdt) / 2.0
}

enum class MfsChannel(
    val ratePercent: Double,
    val labelEn: String,
    val labelBn: String,
    val noteEn: String,
    val noteBn: String
) {
    BKASH_APP(1.85, "bKash App (Standard)", "বিকাশ অ্যাপ (সাধারণ)", "BDT 18.50 per BDT 1,000", "প্রতি BDT ১,০০০-এ BDT ১৮.৫০"),
    BKASH_PRIYO(1.49, "bKash Priyo Agent", "বিকাশ প্রিয় এজেন্ট", "BDT 14.90 per BDT 1,000", "প্রতি BDT ১,০০০-এ BDT ১৪.৯০"),
    NAGAD_APP(1.25, "Nagad App Cash-out", "নগদ অ্যাপ ক্যাশ-আউট", "BDT 12.50 per BDT 1,000 (incl. VAT)", "প্রতি BDT ১,০০০-এ BDT ১২.৫০"),
    NAGAD_USSD(1.50, "Nagad USSD (*167#)", "নগদ USSD (*167#)", "BDT 15.00 per BDT 1,000", "প্রতি BDT ১,০০০-এ BDT ১৫.০০"),
    ROCKET_AGENT(1.67, "Rocket Agent", "রকেট এজেন্ট", "BDT 16.70 per BDT 1,000", "প্রতি BDT ১,০০০-এ BDT ১৬.৭০");

    fun label(language: AppLanguage): String = if (language == AppLanguage.BN) labelBn else labelEn
    fun note(language: AppLanguage): String = if (language == AppLanguage.BN) noteBn else noteEn
}

/**
 * Snapshot of exchange rates (units per 1 USD), gold prices (24K/22K/21K/18K/Traditional per Vori + live XAU/USD spot),
 * bank benchmarks, remittance incentive, and per-feed cache/source metadata.
 */
data class MarketSnapshot(
    val ratesPerUsd: Map<String, Double> = defaultRatesPerUsd(),
    val gold22kPerVoriBdt: Double = 142_800.0,
    val gold21kPerVoriBdt: Double = 136_300.0,
    val gold18kPerVoriBdt: Double = 116_850.0,
    val goldTraditionalPerVoriBdt: Double = 96_050.0,
    val remittanceIncentivePercent: Double = 2.5,
    val avgBankFdrRatePercent: Double = 9.5,
    val avgBankDpsRatePercent: Double = 9.0,
    val lastSyncedMillis: Long = 0L,
    val isLiveSynced: Boolean = false,
    val sourceLabel: String = "Reference",
    val gold24kPerVoriBdt: Double = ((gold22kPerVoriBdt / GoldCarat.K22.purityFactor) / 50.0).roundToLong() * 50.0,
    val xauUsdPerOunce: Double = 0.0,
    val goldLastSyncedMillis: Long = 0L,
    val isGoldLiveSynced: Boolean = false,
    val goldSourceLabel: String = "Reference"
) {
    fun ratePerUsd(currency: MarketCurrency): Double =
        ratesPerUsd[currency.code]?.takeIf { it > 0.0 } ?: currency.defaultPerUsd

    fun ratePerUsd(currencyCode: String): Double {
        val normalized = currencyCode.trim().uppercase()
        return ratesPerUsd[normalized]?.takeIf { it > 0.0 }
            ?: MarketCurrency.fromCode(normalized)?.defaultPerUsd
            ?: com.example.data.model.WorldCountryCatalog.defaultPerUsd(normalized)
    }

    /**
     * Converts [amount] from any supported [from] currency to any supported [to] currency
     * using USD cross-rates. Works both online (live synced) and offline (cached/reference).
     */
    fun convert(amount: Double, from: MarketCurrency, to: MarketCurrency): Double {
        if (amount <= 0.0) return 0.0
        if (from == to) return amount
        val fromRate = ratePerUsd(from)
        val toRate = ratePerUsd(to)
        return (amount / fromRate) * toRate
    }

    /**
     * Sign-safe currency conversion using ISO currency codes across all 195 countries' currencies
     * (e.g. "SAR" -> "BDT", "TRY" -> "BDT", "PHP" -> "USD", "KWD" -> "EUR").
     * Returns [amount] unchanged when codes match or either code is unrecognized.
     */
    fun convertCurrencyCode(amount: Double, fromCode: String, toCode: String): Double {
        if (amount == 0.0 || fromCode.equals(toCode, ignoreCase = true)) return amount
        val fromNorm = fromCode.trim().uppercase()
        val toNorm = toCode.trim().uppercase()
        val fromKnown = ratesPerUsd.containsKey(fromNorm) ||
            MarketCurrency.fromCode(fromNorm) != null ||
            com.example.data.model.WorldCountryCatalog.isKnownCurrencyCode(fromNorm)
        val toKnown = ratesPerUsd.containsKey(toNorm) ||
            MarketCurrency.fromCode(toNorm) != null ||
            com.example.data.model.WorldCountryCatalog.isKnownCurrencyCode(toNorm)
        if (!fromKnown || !toKnown) return amount
        val fromRate = ratePerUsd(fromNorm)
        val toRate = ratePerUsd(toNorm)
        if (fromRate <= 0.0 || toRate <= 0.0) return amount
        val sign = if (amount < 0.0) -1.0 else 1.0
        return sign * ((kotlin.math.abs(amount) / fromRate) * toRate)
    }

    fun goldPricePerVoriBdt(carat: GoldCarat): Double = when (carat) {
        GoldCarat.K24 -> gold24kPerVoriBdt
        GoldCarat.K22 -> gold22kPerVoriBdt
        GoldCarat.K21 -> gold21kPerVoriBdt
        GoldCarat.K18 -> gold18kPerVoriBdt
        GoldCarat.TRADITIONAL -> goldTraditionalPerVoriBdt
    }

    fun goldPricePerGramBdt(carat: GoldCarat): Double =
        goldPricePerVoriBdt(carat) / GRAMS_PER_VORI

    fun goldPriceInCurrency(carat: GoldCarat, perGram: Boolean, targetCurrency: MarketCurrency): Double {
        val bdtValue = if (perGram) goldPricePerGramBdt(carat) else goldPricePerVoriBdt(carat)
        return convert(bdtValue, MarketCurrency.BDT, targetCurrency)
    }

    fun resolveCurrencyStatus(isOnline: Boolean): MarketDataStatus = when {
        isLiveSynced && lastSyncedMillis > 0L && isOnline -> MarketDataStatus.LIVE_SYNCED
        isLiveSynced && lastSyncedMillis > 0L -> MarketDataStatus.CACHED_OFFLINE
        else -> MarketDataStatus.REFERENCE_ESTIMATE
    }

    fun resolveGoldStatus(isOnline: Boolean): MarketDataStatus = when {
        isGoldLiveSynced && goldLastSyncedMillis > 0L && isOnline -> MarketDataStatus.LIVE_SYNCED
        (isGoldLiveSynced && goldLastSyncedMillis > 0L) || (isLiveSynced && lastSyncedMillis > 0L) ->
            MarketDataStatus.CACHED_OFFLINE
        else -> MarketDataStatus.REFERENCE_ESTIMATE
    }

    fun isStale(nowMillis: Long = System.currentTimeMillis(), maxAgeMillis: Long = DAILY_SYNC_INTERVAL_MS): Boolean {
        if (!isLiveSynced || lastSyncedMillis <= 0L) return true
        return (nowMillis - lastSyncedMillis) >= maxAgeMillis
    }

    /**
     * Freshness check used on app launch, resume, and opening Live Market (15-minute cadence)
     * so rates and XAU gold spot stay current without hammering external APIs on every recomposition.
     */
    fun isFreshSyncNeeded(
        nowMillis: Long = System.currentTimeMillis(),
        maxAgeMillis: Long = FRESH_SYNC_INTERVAL_MS
    ): Boolean {
        if (!isLiveSynced || lastSyncedMillis <= 0L) return true
        return (nowMillis - lastSyncedMillis) >= maxAgeMillis
    }

    companion object {
        const val GRAMS_PER_VORI = 11.664
        const val GRAMS_PER_TROY_OUNCE = 31.1034768
        const val DAILY_SYNC_INTERVAL_MS = 24L * 60L * 60L * 1000L
        const val FRESH_SYNC_INTERVAL_MS = 15L * 60L * 1000L

        fun defaultRatesPerUsd(): Map<String, Double> = buildMap {
            putAll(com.example.data.model.WorldCountryCatalog.defaultPerUsdMap)
            MarketCurrency.entries.forEach { put(it.code, it.defaultPerUsd) }
        }

        fun defaultReference(): MarketSnapshot = MarketSnapshot()
    }
}

data class MarketSyncUiState(
    val snapshot: MarketSnapshot = MarketSnapshot.defaultReference(),
    val isOnline: Boolean = true,
    val isSyncing: Boolean = false,
    val lastSyncFailed: Boolean = false,
    val lastAttemptedMillis: Long = 0L
)

/**
 * Pure financial calculation helpers for Bank FDR/DPS, Remittance, and MFS fees.
 */
object MarketCalculators {

    data class FdrEstimate(
        val principal: Double,
        val grossProfit: Double,
        val sourceTax: Double,
        val netProfit: Double,
        val maturityAmount: Double
    )

    data class DpsEstimate(
        val totalDeposited: Double,
        val grossProfit: Double,
        val sourceTax: Double,
        val netProfit: Double,
        val maturityAmount: Double
    )

    data class RemittanceEstimate(
        val baseBdt: Double,
        val incentiveBdt: Double,
        val totalReceivableBdt: Double,
        val effectiveRatePerUnit: Double,
        val appliedRatePerUnit: Double = 0.0,
        val feeBdt: Double = 0.0
    )

    data class MfsFeeEstimate(
        val amount: Double,
        val feeBdt: Double,
        val totalDeductedBdt: Double,
        val netReceivedBdt: Double
    )

    fun calculateFdr(
        principal: Double,
        annualRatePercent: Double,
        months: Int,
        hasTinCertificate: Boolean = true
    ): FdrEstimate {
        val safePrincipal = principal.coerceAtLeast(0.0)
        val years = months.coerceAtLeast(1) / 12.0
        val gross = safePrincipal * (annualRatePercent.coerceAtLeast(0.0) / 100.0) * years
        val taxRate = if (hasTinCertificate) 0.10 else 0.15
        val tax = gross * taxRate
        val net = (gross - tax).coerceAtLeast(0.0)
        return FdrEstimate(
            principal = safePrincipal,
            grossProfit = gross,
            sourceTax = tax,
            netProfit = net,
            maturityAmount = safePrincipal + net
        )
    }

    fun calculateDps(
        monthlyDeposit: Double,
        annualRatePercent: Double,
        months: Int,
        hasTinCertificate: Boolean = true
    ): DpsEstimate {
        val safeMonthly = monthlyDeposit.coerceAtLeast(0.0)
        val n = months.coerceAtLeast(1)
        val r = (annualRatePercent.coerceAtLeast(0.0) / 100.0) / 12.0
        val totalDeposited = safeMonthly * n
        val futureValue = if (r > 0.0) {
            safeMonthly * (((1.0 + r).pow(n) - 1.0) / r) * (1.0 + r)
        } else {
            totalDeposited
        }
        val gross = (futureValue - totalDeposited).coerceAtLeast(0.0)
        val taxRate = if (hasTinCertificate) 0.10 else 0.15
        val tax = gross * taxRate
        val net = (gross - tax).coerceAtLeast(0.0)
        return DpsEstimate(
            totalDeposited = totalDeposited,
            grossProfit = gross,
            sourceTax = tax,
            netProfit = net,
            maturityAmount = totalDeposited + net
        )
    }

    fun calculateRemittance(
        foreignAmount: Double,
        fromCurrency: MarketCurrency,
        snapshot: MarketSnapshot,
        includeGovtIncentive: Boolean = true,
        customRateOverride: Double? = null,
        transferFeeBdt: Double = 0.0
    ): RemittanceEstimate {
        val safeAmount = foreignAmount.coerceAtLeast(0.0)
        val marketRate = snapshot.convert(1.0, fromCurrency, MarketCurrency.BDT)
        val appliedRate = customRateOverride?.takeIf { it > 0.0 } ?: marketRate
        val baseBdt = safeAmount * appliedRate
        val incentive = if (includeGovtIncentive) {
            baseBdt * (snapshot.remittanceIncentivePercent / 100.0)
        } else {
            0.0
        }
        val safeFee = transferFeeBdt.coerceAtLeast(0.0)
        val total = (baseBdt + incentive - safeFee).coerceAtLeast(0.0)
        val effectiveRate = if (safeAmount > 0.0) total / safeAmount else 0.0
        return RemittanceEstimate(
            baseBdt = baseBdt,
            incentiveBdt = incentive,
            totalReceivableBdt = total,
            effectiveRatePerUnit = effectiveRate,
            appliedRatePerUnit = appliedRate,
            feeBdt = safeFee
        )
    }

    fun calculateMfsCashOut(amountBdt: Double, channel: MfsChannel): MfsFeeEstimate {
        val safeAmount = amountBdt.coerceAtLeast(0.0)
        val fee = safeAmount * (channel.ratePercent / 100.0)
        return MfsFeeEstimate(
            amount = safeAmount,
            feeBdt = fee,
            totalDeductedBdt = safeAmount + fee,
            netReceivedBdt = (safeAmount - fee).coerceAtLeast(0.0)
        )
    }

    fun defaultEssentialsCatalog(): List<EssentialGroceryItem> = listOf(
        EssentialGroceryItem("rice_miniket", EssentialGroup.GRAINS, "Miniket Rice", "মিনিকেট চাল", "kg", "কেজি", 72, 78),
        EssentialGroceryItem("rice_nazir", EssentialGroup.GRAINS, "Nazirshail Rice", "নাজিরশাইল চাল", "kg", "কেজি", 78, 86),
        EssentialGroceryItem("rice_coarse", EssentialGroup.GRAINS, "Coarse Rice (BR-28)", "মোটা চাল (বিআর-২৮)", "kg", "কেজি", 54, 60),
        EssentialGroceryItem("atta", EssentialGroup.GRAINS, "Packaged Atta (Flour)", "প্যাকেট আটা", "2 kg", "২ কেজি", 110, 125),
        EssentialGroceryItem("soybean_oil", EssentialGroup.OIL_LENTILS, "Soybean Oil (Bottled)", "সয়াবিন তেল (বোতলজাত)", "L", "লিটার", 167, 175),
        EssentialGroceryItem("mustard_oil", EssentialGroup.OIL_LENTILS, "Mustard Oil", "সরিষার তেল", "L", "লিটার", 260, 290),
        EssentialGroceryItem("masoor_dal", EssentialGroup.OIL_LENTILS, "Red Lentils (Deshi)", "দেশি মসুর ডাল", "kg", "কেজি", 130, 142),
        EssentialGroceryItem("sugar", EssentialGroup.OIL_LENTILS, "Sugar (Packaged)", "চিনি (প্যাকেট)", "kg", "কেজি", 130, 140),
        EssentialGroceryItem("broiler_chicken", EssentialGroup.PROTEIN, "Broiler Chicken", "ব্রয়লার মুরগি", "kg", "কেজি", 185, 210),
        EssentialGroceryItem("farm_eggs", EssentialGroup.PROTEIN, "Farm Eggs", "ফার্মের ডিম", "dozen", "ডজন", 140, 155),
        EssentialGroceryItem("rohu_fish", EssentialGroup.PROTEIN, "Rui Fish (Medium)", "রুই মাছ (মাঝারি)", "kg", "কেজি", 320, 380),
        EssentialGroceryItem("beef", EssentialGroup.PROTEIN, "Beef", "গরুর মাংস", "kg", "কেজি", 750, 780),
        EssentialGroceryItem("onion", EssentialGroup.PRODUCE, "Onion (Local)", "দেশি পেঁয়াজ", "kg", "কেজি", 85, 105),
        EssentialGroceryItem("potato", EssentialGroup.PRODUCE, "Potato", "আলু", "kg", "কেজি", 50, 62),
        EssentialGroceryItem("garlic", EssentialGroup.PRODUCE, "Garlic", "রসুন", "kg", "কেজি", 190, 230),
        EssentialGroceryItem("green_chili", EssentialGroup.PRODUCE, "Green Chili", "কাঁচামরিচ", "kg", "কেজি", 120, 160)
    )

    fun formatTimestamp(millis: Long, language: AppLanguage): String {
        if (millis <= 0L) {
            return if (language == AppLanguage.BN) "সংরক্ষিত রেফারেন্স" else "Saved Reference"
        }
        val pattern = "dd MMM, hh:mm a"
        val formatted = SimpleDateFormat(pattern, Locale.US).apply {
            timeZone = java.util.TimeZone.getDefault()
        }.format(Date(millis))
        return if (language == AppLanguage.BN) toBanglaDigits(formatted) else formatted
    }

    fun formatDecimal(value: Double, decimals: Int = 2, language: AppLanguage): String {
        val pattern = "%.${decimals}f"
        val formatted = String.format(Locale.US, pattern, value)
        return if (language == AppLanguage.BN) toBanglaDigits(formatted) else formatted
    }

    fun toBanglaDigits(input: String): String {
        val map = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
        return buildString(input.length) {
            for (ch in input) {
                if (ch in '0'..'9') append(map[ch - '0']) else append(ch)
            }
        }
    }
}

/**
 * Connectivity-aware Market Data Repository:
 * - Fetches fresh multi-region FX rates (KWD, SAR, AED, QAR, MYR, EUR, GBP, USD, INR, BDT & more)
 *   and live XAU spot gold benchmarks via [MarketRemoteApiService] (configurable external API keys
 *   from BuildConfig + reliable live public fallbacks).
 * - Persists the latest synced snapshot, XAU spot price, per-feed timestamps, and source attribution
 *   into DataStore for instant offline use.
 * - Never leaks raw HTTP/JSON/API-key errors to the UI.
 */
class MarketDataRepository(
    context: Context,
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build(),
    private val remoteApiService: MarketRemoteApiService = MarketRemoteApiService(httpClient)
) {
    private val appContext = context.applicationContext

    private object Keys {
        val RATES_JSON = stringPreferencesKey("market_rates_json")
        val GOLD_24K_VORI = stringPreferencesKey("market_gold_24k_vori")
        val GOLD_22K_VORI = stringPreferencesKey("market_gold_22k_vori")
        val XAU_USD_OZ = stringPreferencesKey("market_xau_usd_oz")
        val LAST_SYNCED_MS = longPreferencesKey("market_last_synced_ms")
        val IS_LIVE_SYNCED = booleanPreferencesKey("market_is_live_synced")
        val SOURCE_LABEL = stringPreferencesKey("market_source_label")
        val GOLD_LAST_SYNCED_MS = longPreferencesKey("market_gold_last_synced_ms")
        val IS_GOLD_LIVE_SYNCED = booleanPreferencesKey("market_is_gold_live_synced")
        val GOLD_SOURCE_LABEL = stringPreferencesKey("market_gold_source_label")
    }

    val snapshotFlow: Flow<MarketSnapshot> = appContext.marketCacheDataStore.data.map { prefs ->
        val defaultMap = MarketSnapshot.defaultRatesPerUsd().toMutableMap()
        val rawJson = prefs[Keys.RATES_JSON]
        if (!rawJson.isNullOrBlank()) {
            runCatching {
                val obj = JSONObject(rawJson)
                MarketCurrency.entries.forEach { cur ->
                    if (obj.has(cur.code)) {
                        val v = obj.optDouble(cur.code, -1.0)
                        if (v > 0.0) defaultMap[cur.code] = v
                    }
                }
            }
        }
        val gold22k = prefs[Keys.GOLD_22K_VORI]?.toDoubleOrNull()?.takeIf { it > 50_000.0 }
            ?: MarketSnapshot.defaultReference().gold22kPerVoriBdt
        val gold24k = prefs[Keys.GOLD_24K_VORI]?.toDoubleOrNull()?.takeIf { it > 50_000.0 }
            ?: roundTo50(gold22k / GoldCarat.K22.purityFactor)
        val gold21k = roundTo50(gold24k * GoldCarat.K21.purityFactor)
        val gold18k = roundTo50(gold24k * GoldCarat.K18.purityFactor)
        val goldTrad = roundTo50(gold24k * GoldCarat.TRADITIONAL.purityFactor)
        val xauUsd = prefs[Keys.XAU_USD_OZ]?.toDoubleOrNull()?.takeIf { it > 0.0 } ?: 0.0
        val lastSynced = prefs[Keys.LAST_SYNCED_MS] ?: 0L
        val goldLastSynced = prefs[Keys.GOLD_LAST_SYNCED_MS] ?: 0L
        val isGoldLive = prefs[Keys.IS_GOLD_LIVE_SYNCED] ?: false

        MarketSnapshot(
            ratesPerUsd = defaultMap,
            gold24kPerVoriBdt = gold24k,
            gold22kPerVoriBdt = gold22k,
            gold21kPerVoriBdt = gold21k,
            gold18kPerVoriBdt = gold18k,
            goldTraditionalPerVoriBdt = goldTrad,
            xauUsdPerOunce = xauUsd,
            lastSyncedMillis = lastSynced,
            isLiveSynced = prefs[Keys.IS_LIVE_SYNCED] ?: false,
            sourceLabel = prefs[Keys.SOURCE_LABEL] ?: "Reference",
            goldLastSyncedMillis = goldLastSynced,
            isGoldLiveSynced = isGoldLive,
            goldSourceLabel = prefs[Keys.GOLD_SOURCE_LABEL] ?: "Reference"
        )
    }

    suspend fun getCachedSnapshot(): MarketSnapshot = snapshotFlow.first()

    /**
     * Fetches fresh exchange rates and live XAU spot gold prices concurrently and persists them to DataStore.
     * Returns the updated [MarketSnapshot] on success, or null when offline/unreachable (leaving cached data intact).
     */
    suspend fun syncLiveMarketRates(nowMillis: Long = System.currentTimeMillis()): MarketSnapshot? =
        withContext(Dispatchers.IO) {
            val previous = getCachedSnapshot()
            val (fxResult, initialGoldResult) = coroutineScope {
                val fxDeferred = async { remoteApiService.fetchCurrencyRates() }
                val goldDeferred = async { remoteApiService.fetchGoldSpot(fallbackXauPerUsd = null) }
                fxDeferred.await() to goldDeferred.await()
            }

            // If primary gold endpoint didn't respond, retry with XAU carried inside FX feed
            val goldResult = initialGoldResult
                ?: fxResult?.xauPerUsd?.let { remoteApiService.fetchGoldSpot(fallbackXauPerUsd = it) }

            if (fxResult == null && goldResult == null) {
                return@withContext null
            }

            val merged = previous.ratesPerUsd.toMutableMap()
            MarketSnapshot.defaultRatesPerUsd().forEach { (code, fallbackVal) ->
                if ((merged[code] ?: 0.0) <= 0.0) merged[code] = fallbackVal
            }
            fxResult?.ratesPerUsd?.forEach { (k, v) ->
                if (v > 0.0) merged[k] = v
            }
            merged["USD"] = 1.0

            val currentBdtPerUsd = merged["BDT"] ?: MarketCurrency.BDT.defaultPerUsd
            val goldComputed = deriveGoldVoriPrices(
                goldResult = goldResult,
                currentBdtPerUsd = currentBdtPerUsd,
                previous = previous
            )

            saveSnapshot(
                rates = merged,
                gold22kVoriBdt = goldComputed.gold22kPerVoriBdt,
                syncedAtMillis = if (fxResult != null) nowMillis else previous.lastSyncedMillis.coerceAtLeast(nowMillis),
                sourceLabel = fxResult?.sourceLabel ?: previous.sourceLabel,
                gold24kVoriBdt = goldComputed.gold24kPerVoriBdt,
                xauUsdPerOunce = goldComputed.xauUsdPerOunce,
                goldSyncedAtMillis = if (goldResult != null) nowMillis else previous.goldLastSyncedMillis,
                isGoldLiveSynced = goldResult != null || previous.isGoldLiveSynced,
                goldSourceLabel = goldResult?.sourceLabel ?: previous.goldSourceLabel
            )
            snapshotFlow.first()
        }

    suspend fun saveSnapshot(
        rates: Map<String, Double>,
        gold22kVoriBdt: Double,
        syncedAtMillis: Long,
        sourceLabel: String,
        gold24kVoriBdt: Double = roundTo50(gold22kVoriBdt / GoldCarat.K22.purityFactor),
        xauUsdPerOunce: Double = 0.0,
        goldSyncedAtMillis: Long = if (xauUsdPerOunce > 0.0) syncedAtMillis else 0L,
        isGoldLiveSynced: Boolean = xauUsdPerOunce > 0.0,
        goldSourceLabel: String = if (xauUsdPerOunce > 0.0) sourceLabel else "Reference"
    ) {
        val json = JSONObject()
        rates.forEach { (code, value) -> json.put(code, value) }
        appContext.marketCacheDataStore.edit { prefs ->
            prefs[Keys.RATES_JSON] = json.toString()
            prefs[Keys.GOLD_24K_VORI] = gold24kVoriBdt.toString()
            prefs[Keys.GOLD_22K_VORI] = gold22kVoriBdt.toString()
            if (xauUsdPerOunce > 0.0) {
                prefs[Keys.XAU_USD_OZ] = xauUsdPerOunce.toString()
            }
            prefs[Keys.LAST_SYNCED_MS] = syncedAtMillis
            prefs[Keys.IS_LIVE_SYNCED] = true
            prefs[Keys.SOURCE_LABEL] = sourceLabel
            prefs[Keys.GOLD_LAST_SYNCED_MS] = goldSyncedAtMillis
            prefs[Keys.IS_GOLD_LIVE_SYNCED] = isGoldLiveSynced
            prefs[Keys.GOLD_SOURCE_LABEL] = goldSourceLabel
        }
    }

    internal data class DerivedGoldPrices(
        val gold24kPerVoriBdt: Double,
        val gold22kPerVoriBdt: Double,
        val xauUsdPerOunce: Double
    )

    internal fun deriveGoldVoriPrices(
        goldResult: RemoteGoldSpotResult?,
        currentBdtPerUsd: Double,
        previous: MarketSnapshot
    ): DerivedGoldPrices {
        if (goldResult != null && goldResult.xauUsdPerOunce > 0.0) {
            val usdPerGram24k = goldResult.usdPerGram24k
                ?: (goldResult.xauUsdPerOunce / MarketSnapshot.GRAMS_PER_TROY_OUNCE)
            val usdPerGram22k = goldResult.usdPerGram22k
                ?: (usdPerGram24k * GoldCarat.K22.purityFactor)
            val bdt24kVori = roundTo50(usdPerGram24k * currentBdtPerUsd * MarketSnapshot.GRAMS_PER_VORI)
                .coerceIn(70_000.0, 450_000.0)
            val bdt22kVori = roundTo50(usdPerGram22k * currentBdtPerUsd * MarketSnapshot.GRAMS_PER_VORI)
                .coerceIn(65_000.0, 420_000.0)
            return DerivedGoldPrices(
                gold24kPerVoriBdt = bdt24kVori,
                gold22kPerVoriBdt = bdt22kVori,
                xauUsdPerOunce = goldResult.xauUsdPerOunce
            )
        }
        // Preserve previous live gold value if already cached; otherwise scale reference cleanly with FX
        if (previous.isGoldLiveSynced && previous.xauUsdPerOunce > 0.0) {
            val usdPerGram24k = previous.xauUsdPerOunce / MarketSnapshot.GRAMS_PER_TROY_OUNCE
            val bdt24kVori = roundTo50(usdPerGram24k * currentBdtPerUsd * MarketSnapshot.GRAMS_PER_VORI)
                .coerceIn(70_000.0, 450_000.0)
            val bdt22kVori = roundTo50(bdt24kVori * GoldCarat.K22.purityFactor)
            return DerivedGoldPrices(
                gold24kPerVoriBdt = bdt24kVori,
                gold22kPerVoriBdt = bdt22kVori,
                xauUsdPerOunce = previous.xauUsdPerOunce
            )
        }
        val fxRatio = (currentBdtPerUsd / MarketCurrency.BDT.defaultPerUsd).coerceIn(0.85, 1.35)
        val scaled22k = roundTo50(MarketSnapshot.defaultReference().gold22kPerVoriBdt * fxRatio)
        val scaled24k = roundTo50(scaled22k / GoldCarat.K22.purityFactor)
        return DerivedGoldPrices(
            gold24kPerVoriBdt = scaled24k,
            gold22kPerVoriBdt = scaled22k,
            xauUsdPerOunce = previous.xauUsdPerOunce
        )
    }

    private fun roundTo50(value: Double): Double = ((value / 50.0).roundToLong() * 50.0)
}
