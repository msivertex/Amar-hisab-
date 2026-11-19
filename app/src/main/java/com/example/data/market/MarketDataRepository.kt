package com.example.data.market

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.localization.AppLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.pow
import kotlin.math.roundToLong

private val Context.marketCacheDataStore by preferencesDataStore(name = "amar_hisab_market_cache")

/**
 * 15 supported multi-region currencies for flexible any-to-any conversion.
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
    BDT("BDT", "৳", "🇧🇩", "Bangladeshi Taka", "বাংলাদেশি টাকা", "Bangladesh", "বাংলাদেশ", 121.50),
    USD("USD", "$", "🇺🇸", "US Dollar", "ইউএস ডলার", "USA", "যুক্তরাষ্ট্র", 1.0),
    EUR("EUR", "€", "🇪🇺", "Euro", "ইউরো", "Italy / Europe", "ইতালি / ইউরোপ", 0.92),
    GBP("GBP", "£", "🇬🇧", "British Pound", "ব্রিটিশ পাউন্ড", "United Kingdom", "যুক্তরাজ্য", 0.785),
    KWD("KWD", "KD", "🇰🇼", "Kuwaiti Dinar", "কুয়েতি দিনার", "Kuwait", "কুয়েত", 0.307),
    SAR("SAR", "SR", "🇸🇦", "Saudi Riyal", "সৌদি রিয়াল", "Saudi Arabia", "সৌদি আরব", 3.75),
    AED("AED", "AED", "🇦🇪", "UAE Dirham", "ইউএই দিরহাম", "UAE / Dubai", "সংযুক্ত আরব আমিরাত", 3.6725),
    QAR("QAR", "QR", "🇶🇦", "Qatari Riyal", "কাতারি রিয়াল", "Qatar", "কাতার", 3.64),
    BHD("BHD", "BD", "🇧🇭", "Bahraini Dinar", "বাহরাইনি দিনার", "Bahrain", "বাহরাইন", 0.376),
    MYR("MYR", "RM", "🇲🇾", "Malaysian Ringgit", "মালয়েশিয়ান রিঙ্গিত", "Malaysia", "মালয়েশিয়া", 4.42),
    INR("INR", "₹", "🇮🇳", "Indian Rupee", "ভারতীয় রুপি", "India", "ভারত", 84.40),
    PKR("PKR", "Rs", "🇵🇰", "Pakistani Rupee", "পাকিস্তানি রুপি", "Pakistan", "পাকিস্তান", 278.00),
    LKR("LKR", "Rs", "🇱🇰", "Sri Lankan Rupee", "শ্রীলঙ্কান রুপি", "Sri Lanka", "শ্রীলঙ্কা", 292.50),
    NPR("NPR", "Rs", "🇳🇵", "Nepalese Rupee", "নেপালি রুপি", "Nepal", "নেপাল", 135.04),
    BTN("BTN", "Nu", "🇧🇹", "Bhutanese Ngultrum", "ভুটানি গুলট্রাম", "Bhutan", "ভুটান", 84.40);

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
    BKASH_APP(1.85, "bKash App (Standard)", "বিকাশ অ্যাপ (সাধারণ)", "৳18.50 per ৳1,000", "প্রতি ৳১,০০০-এ ৳১৮.৫০"),
    BKASH_PRIYO(1.49, "bKash Priyo Agent", "বিকাশ প্রিয় এজেন্ট", "৳14.90 per ৳1,000", "প্রতি ৳১,০০০-এ ৳১৪.৯০"),
    NAGAD_APP(1.25, "Nagad App Cash-out", "নগদ অ্যাপ ক্যাশ-আউট", "৳12.50 per ৳1,000 (incl. VAT)", "প্রতি ৳১,০০০-এ ৳১২.৫০"),
    NAGAD_USSD(1.50, "Nagad USSD (*167#)", "নগদ USSD (*167#)", "৳15.00 per ৳1,000", "প্রতি ৳১,০০০-এ ৳১৫.০০"),
    ROCKET_AGENT(1.67, "Rocket Agent", "রকেট এজেন্ট", "৳16.70 per ৳1,000", "প্রতি ৳১,০০০-এ ৳১৬.৭০");

    fun label(language: AppLanguage): String = if (language == AppLanguage.BN) labelBn else labelEn
    fun note(language: AppLanguage): String = if (language == AppLanguage.BN) noteBn else noteEn
}

/**
 * Snapshot of exchange rates (units per 1 USD), gold prices (BDT per 22K/21K/18K/Traditional Vori),
 * bank benchmarks, remittance incentive, and cache metadata.
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
    val sourceLabel: String = "Reference"
) {
    fun ratePerUsd(currency: MarketCurrency): Double =
        ratesPerUsd[currency.code]?.takeIf { it > 0.0 } ?: currency.defaultPerUsd

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

    fun goldPricePerVoriBdt(carat: GoldCarat): Double = when (carat) {
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

    fun isStale(nowMillis: Long = System.currentTimeMillis(), maxAgeMillis: Long = DAILY_SYNC_INTERVAL_MS): Boolean {
        if (!isLiveSynced || lastSyncedMillis <= 0L) return true
        return (nowMillis - lastSyncedMillis) >= maxAgeMillis
    }

    companion object {
        const val GRAMS_PER_VORI = 11.664
        const val GRAMS_PER_TROY_OUNCE = 31.1034768
        const val DAILY_SYNC_INTERVAL_MS = 24L * 60L * 60L * 1000L

        fun defaultRatesPerUsd(): Map<String, Double> =
            MarketCurrency.entries.associate { it.code to it.defaultPerUsd }

        fun defaultReference(): MarketSnapshot = MarketSnapshot()
    }
}

data class MarketSyncUiState(
    val snapshot: MarketSnapshot = MarketSnapshot.defaultReference(),
    val isOnline: Boolean = true,
    val isSyncing: Boolean = false
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
        val effectiveRatePerUnit: Double
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
        val gross = safePrincipal * (annualRatePercent / 100.0) * years
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
        val r = (annualRatePercent / 100.0) / 12.0
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
        includeGovtIncentive: Boolean = true
    ): RemittanceEstimate {
        val safeAmount = foreignAmount.coerceAtLeast(0.0)
        val baseBdt = snapshot.convert(safeAmount, fromCurrency, MarketCurrency.BDT)
        val incentive = if (includeGovtIncentive) {
            baseBdt * (snapshot.remittanceIncentivePercent / 100.0)
        } else {
            0.0
        }
        val total = baseBdt + incentive
        val effectiveRate = if (safeAmount > 0.0) total / safeAmount else 0.0
        return RemittanceEstimate(
            baseBdt = baseBdt,
            incentiveBdt = incentive,
            totalReceivableBdt = total,
            effectiveRatePerUnit = effectiveRate
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
 * - Fetches live multi-region exchange rates (all 15 currencies + spot gold where available)
 *   using zero-key public FX endpoints over OkHttp.
 * - Persists the latest synced snapshot with timestamp into DataStore for seamless offline use.
 * - Never leaks raw HTTP/JSON/network exceptions to the UI.
 */
class MarketDataRepository(
    context: Context,
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
) {
    private val appContext = context.applicationContext

    private object Keys {
        val RATES_JSON = stringPreferencesKey("market_rates_json")
        val GOLD_22K_VORI = stringPreferencesKey("market_gold_22k_vori")
        val LAST_SYNCED_MS = longPreferencesKey("market_last_synced_ms")
        val IS_LIVE_SYNCED = booleanPreferencesKey("market_is_live_synced")
        val SOURCE_LABEL = stringPreferencesKey("market_source_label")
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
        val gold21k = ((gold22k * (GoldCarat.K21.purityFactor / GoldCarat.K22.purityFactor)) / 50.0).roundToLong() * 50.0
        val gold18k = ((gold22k * (GoldCarat.K18.purityFactor / GoldCarat.K22.purityFactor)) / 50.0).roundToLong() * 50.0
        val goldTrad = ((gold22k * (GoldCarat.TRADITIONAL.purityFactor / GoldCarat.K22.purityFactor)) / 50.0).roundToLong() * 50.0

        MarketSnapshot(
            ratesPerUsd = defaultMap,
            gold22kPerVoriBdt = gold22k,
            gold21kPerVoriBdt = gold21k,
            gold18kPerVoriBdt = gold18k,
            goldTraditionalPerVoriBdt = goldTrad,
            lastSyncedMillis = prefs[Keys.LAST_SYNCED_MS] ?: 0L,
            isLiveSynced = prefs[Keys.IS_LIVE_SYNCED] ?: false,
            sourceLabel = prefs[Keys.SOURCE_LABEL] ?: "Reference"
        )
    }

    suspend fun getCachedSnapshot(): MarketSnapshot = snapshotFlow.first()

    /**
     * Fetches fresh exchange rates from public open FX APIs and persists them to DataStore.
     * Returns the updated [MarketSnapshot] on success or null on failure (so cached data stays intact).
     */
    suspend fun syncLiveMarketRates(nowMillis: Long = System.currentTimeMillis()): MarketSnapshot? =
        withContext(Dispatchers.IO) {
            val fetched = fetchFromOpenErApi() ?: fetchFromFawazCurrencyApi() ?: return@withContext null
            val merged = MarketSnapshot.defaultRatesPerUsd().toMutableMap()
            fetched.rates.forEach { (k, v) ->
                if (v > 0.0) merged[k] = v
            }
            merged["USD"] = 1.0

            // Derive updated 22K BDT per vori if live XAU (gold oz) rate is available,
            // otherwise scale gracefully from BDT/USD shift against benchmark.
            val currentBdt = merged["BDT"] ?: MarketCurrency.BDT.defaultPerUsd
            val gold22kVori = if (fetched.xauPerUsd != null && fetched.xauPerUsd > 0.0) {
                val usdPerOz = 1.0 / fetched.xauPerUsd
                val bdtPerGram24k = (usdPerOz * currentBdt) / MarketSnapshot.GRAMS_PER_TROY_OUNCE
                // Include standard Bangladesh local import/hallmark adjustment (~1.06x)
                val bdtPerVori22k = bdtPerGram24k * MarketSnapshot.GRAMS_PER_VORI * GoldCarat.K22.purityFactor * 1.06
                ((bdtPerVori22k / 50.0).roundToLong() * 50.0).coerceIn(90_000.0, 280_000.0)
            } else {
                val fxRatio = (currentBdt / MarketCurrency.BDT.defaultPerUsd).coerceIn(0.85, 1.35)
                (((MarketSnapshot.defaultReference().gold22kPerVoriBdt * fxRatio) / 50.0).roundToLong() * 50.0)
            }

            saveSnapshot(
                rates = merged,
                gold22kVoriBdt = gold22kVori,
                syncedAtMillis = nowMillis,
                sourceLabel = fetched.sourceLabel
            )
            snapshotFlow.first()
        }

    suspend fun saveSnapshot(
        rates: Map<String, Double>,
        gold22kVoriBdt: Double,
        syncedAtMillis: Long,
        sourceLabel: String
    ) {
        val json = JSONObject()
        rates.forEach { (code, value) -> json.put(code, value) }
        appContext.marketCacheDataStore.edit { prefs ->
            prefs[Keys.RATES_JSON] = json.toString()
            prefs[Keys.GOLD_22K_VORI] = gold22kVoriBdt.toString()
            prefs[Keys.LAST_SYNCED_MS] = syncedAtMillis
            prefs[Keys.IS_LIVE_SYNCED] = true
            prefs[Keys.SOURCE_LABEL] = sourceLabel
        }
    }

    private data class RemoteFxResult(
        val rates: Map<String, Double>,
        val xauPerUsd: Double?,
        val sourceLabel: String
    )

    private fun fetchFromOpenErApi(): RemoteFxResult? = runCatching {
        val request = Request.Builder()
            .url("https://open.er-api.com/v6/latest/USD")
            .header("Accept", "application/json")
            .build()
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@runCatching null
            val body = response.body?.string().orEmpty()
            if (body.isBlank()) return@runCatching null
            val root = JSONObject(body)
            val ratesObj = root.optJSONObject("rates") ?: return@runCatching null
            val parsed = mutableMapOf<String, Double>()
            MarketCurrency.entries.forEach { cur ->
                val v = ratesObj.optDouble(cur.code, -1.0)
                if (v > 0.0) parsed[cur.code] = v
            }
            if (parsed.size < 8) return@runCatching null
            val xau = ratesObj.optDouble("XAU", -1.0).takeIf { it > 0.0 }
            RemoteFxResult(parsed, xau, "Open ER-API")
        }
    }.getOrNull()

    private fun fetchFromFawazCurrencyApi(): RemoteFxResult? = runCatching {
        val request = Request.Builder()
            .url("https://cdn.jsdelivr.net/npm/@fawazahmed0/currency-api@latest/v1/currencies/usd.json")
            .header("Accept", "application/json")
            .build()
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@runCatching null
            val body = response.body?.string().orEmpty()
            if (body.isBlank()) return@runCatching null
            val root = JSONObject(body)
            val usdObj = root.optJSONObject("usd") ?: return@runCatching null
            val parsed = mutableMapOf<String, Double>()
            MarketCurrency.entries.forEach { cur ->
                val v = usdObj.optDouble(cur.code.lowercase(Locale.ROOT), -1.0)
                if (v > 0.0) parsed[cur.code] = v
            }
            if (parsed.size < 8) return@runCatching null
            val xau = usdObj.optDouble("xau", -1.0).takeIf { it > 0.0 }
            RemoteFxResult(parsed, xau, "Global FX CDN")
        }
    }.getOrNull()
}
