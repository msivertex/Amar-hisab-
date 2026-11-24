package com.example.data.market

import com.example.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.Locale

enum class CurrencyApiProvider {
    AUTO,
    OPEN_EXCHANGE_RATES,
    CURRENCY_FREAKS,
    EXCHANGE_RATE_API,
    FRANKFURTER;

    companion object {
        fun fromConfig(raw: String?): CurrencyApiProvider =
            entries.firstOrNull { it.name.equals(raw?.trim(), ignoreCase = true) } ?: AUTO
    }
}

enum class GoldApiProvider {
    AUTO,
    GOLD_API_IO,
    FAST_FOREX;

    companion object {
        fun fromConfig(raw: String?): GoldApiProvider =
            entries.firstOrNull { it.name.equals(raw?.trim(), ignoreCase = true) } ?: AUTO
    }
}

/**
 * Environment-driven API configuration sourced from BuildConfig (populated via Secrets Gradle Plugin / AI Studio Secrets).
 * Never exposes API keys to UI components or user-visible error messages.
 */
data class MarketApiConfig(
    val currencyProvider: CurrencyApiProvider = CurrencyApiProvider.fromConfig(BuildConfig.CURRENCY_API_PROVIDER),
    val currencyApiKey: String = sanitizeKey(BuildConfig.CURRENCY_API_KEY),
    val goldProvider: GoldApiProvider = GoldApiProvider.fromConfig(BuildConfig.GOLD_API_PROVIDER),
    val goldApiKey: String = sanitizeKey(BuildConfig.GOLD_API_KEY)
) {
    val hasCurrencyApiKey: Boolean get() = currencyApiKey.isNotEmpty()
    val hasGoldApiKey: Boolean get() = goldApiKey.isNotEmpty()

    companion object {
        internal fun sanitizeKey(raw: String?): String {
            val trimmed = raw?.trim().orEmpty()
            if (trimmed.isEmpty()) return ""
            val upper = trimmed.uppercase(Locale.ROOT)
            if (upper == "UNSET" || upper == "NONE" || upper == "NULL" || upper.startsWith("YOUR_")) {
                return ""
            }
            return trimmed
        }
    }
}

data class RemoteCurrencyRatesResult(
    val ratesPerUsd: Map<String, Double>,
    val xauPerUsd: Double?,
    val sourceLabel: String,
    val providerTimestampMillis: Long? = null
)

data class RemoteGoldSpotResult(
    val xauUsdPerOunce: Double,
    val usdPerGram24k: Double? = null,
    val usdPerGram22k: Double? = null,
    val usdPerGram21k: Double? = null,
    val usdPerGram18k: Double? = null,
    val sourceLabel: String,
    val providerTimestampMillis: Long? = null
)

/**
 * Modular external market data client supporting:
 * - Currency APIs: Open Exchange Rates, CurrencyFreaks, ExchangeRate-API v6, Open ER-API,
 *   Global FX CDN, and Frankfurter (ECB) — covering KWD, SAR, AED, QAR, MYR, EUR, GBP, USD, INR, BDT & more.
 * - Precious-Metals / Gold (XAU) APIs: GoldAPI.io, fastFOREX, public Gold-API XAU spot, and FX XAU fallback.
 */
class MarketRemoteApiService(
    private val httpClient: OkHttpClient,
    private val config: MarketApiConfig = MarketApiConfig()
) {

    fun fetchCurrencyRates(): RemoteCurrencyRatesResult? {
        val configuredResult = if (config.hasCurrencyApiKey) {
            when (config.currencyProvider) {
                CurrencyApiProvider.OPEN_EXCHANGE_RATES -> fetchFromOpenExchangeRates(config.currencyApiKey)
                CurrencyApiProvider.CURRENCY_FREAKS -> fetchFromCurrencyFreaks(config.currencyApiKey)
                CurrencyApiProvider.EXCHANGE_RATE_API -> fetchFromExchangeRateApiV6(config.currencyApiKey)
                CurrencyApiProvider.FRANKFURTER -> fetchFromFrankfurter()
                CurrencyApiProvider.AUTO -> {
                    fetchFromOpenExchangeRates(config.currencyApiKey)
                        ?: fetchFromCurrencyFreaks(config.currencyApiKey)
                        ?: fetchFromExchangeRateApiV6(config.currencyApiKey)
                }
            }
        } else if (config.currencyProvider == CurrencyApiProvider.FRANKFURTER) {
            fetchFromFrankfurter()
        } else {
            null
        }

        if (configuredResult != null && configuredResult.ratesPerUsd.size >= 8) {
            return configuredResult
        }

        // Reliable zero-key live fallback chain so users always receive fresh multi-region rates
        // (KWD, SAR, AED, QAR, MYR, EUR, GBP, USD, INR, BDT, etc.) even before configuring a custom key.
        val openEr = fetchFromOpenErApi()
        if (openEr != null) return openEr

        val fawaz = fetchFromFawazCurrencyApi()
        if (fawaz != null) return fawaz

        // If a partial configured result or Frankfurter ECB feed succeeded, enrich/return it gracefully.
        return configuredResult ?: fetchFromFrankfurter()
    }

    fun fetchGoldSpot(fallbackXauPerUsd: Double? = null): RemoteGoldSpotResult? {
        val configuredResult = if (config.hasGoldApiKey) {
            when (config.goldProvider) {
                GoldApiProvider.GOLD_API_IO -> fetchFromGoldApiIo(config.goldApiKey)
                GoldApiProvider.FAST_FOREX -> fetchFromFastForex(config.goldApiKey)
                GoldApiProvider.AUTO -> {
                    fetchFromGoldApiIo(config.goldApiKey)
                        ?: fetchFromFastForex(config.goldApiKey)
                }
            }
        } else {
            null
        }
        if (configuredResult != null && configuredResult.xauUsdPerOunce in 800.0..12_000.0) {
            return configuredResult
        }

        // Public live XAU spot endpoint fallback
        val publicGold = fetchFromPublicGoldApi()
        if (publicGold != null && publicGold.xauUsdPerOunce in 800.0..12_000.0) {
            return publicGold
        }

        // Secondary fallback: XAU rate from FX CDN if available
        if (fallbackXauPerUsd != null && fallbackXauPerUsd > 0.0) {
            val usdPerOz = 1.0 / fallbackXauPerUsd
            if (usdPerOz in 800.0..12_000.0) {
                return RemoteGoldSpotResult(
                    xauUsdPerOunce = usdPerOz,
                    sourceLabel = "Global XAU Spot"
                )
            }
        }
        return null
    }

    private fun executeGet(url: String, headers: Map<String, String> = emptyMap()): String? = runCatching {
        val builder = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
        headers.forEach { (k, v) -> builder.header(k, v) }
        httpClient.newCall(builder.build()).execute().use { response ->
            if (!response.isSuccessful) return@runCatching null
            response.body?.string()?.takeIf { it.isNotBlank() }
        }
    }.getOrNull()

    private fun fetchFromOpenExchangeRates(apiKey: String): RemoteCurrencyRatesResult? {
        val body = executeGet("https://openexchangerates.org/api/latest.json?app_id=$apiKey") ?: return null
        return parseOpenExchangeRatesJson(body)
    }

    private fun fetchFromCurrencyFreaks(apiKey: String): RemoteCurrencyRatesResult? {
        val body = executeGet("https://api.currencyfreaks.com/v2.0/rates/latest?apikey=$apiKey") ?: return null
        return parseCurrencyFreaksJson(body)
    }

    private fun fetchFromExchangeRateApiV6(apiKey: String): RemoteCurrencyRatesResult? {
        val body = executeGet("https://v6.exchangerate-api.com/v6/$apiKey/latest/USD") ?: return null
        return parseExchangeRateApiV6Json(body)
    }

    private fun fetchFromOpenErApi(): RemoteCurrencyRatesResult? {
        val body = executeGet("https://open.er-api.com/v6/latest/USD") ?: return null
        return parseOpenErApiJson(body)
    }

    private fun fetchFromFawazCurrencyApi(): RemoteCurrencyRatesResult? {
        val body = executeGet("https://cdn.jsdelivr.net/npm/@fawazahmed0/currency-api@latest/v1/currencies/usd.json")
            ?: return null
        return parseFawazCurrencyJson(body)
    }

    private fun fetchFromFrankfurter(): RemoteCurrencyRatesResult? {
        val body = executeGet("https://api.frankfurter.dev/v1/latest?base=USD") ?: return null
        return parseFrankfurterJson(body)
    }

    private fun fetchFromGoldApiIo(apiKey: String): RemoteGoldSpotResult? {
        val body = executeGet(
            url = "https://www.goldapi.io/api/XAU/USD",
            headers = mapOf("x-access-token" to apiKey)
        ) ?: return null
        return parseGoldApiIoJson(body)
    }

    private fun fetchFromFastForex(apiKey: String): RemoteGoldSpotResult? {
        val body = executeGet("https://api.fastforex.io/fetch-one?from=XAU&to=USD&api_key=$apiKey") ?: return null
        return parseFastForexJson(body)
    }

    private fun fetchFromPublicGoldApi(): RemoteGoldSpotResult? {
        val body = executeGet("https://api.gold-api.com/price/XAU") ?: return null
        return parsePublicGoldApiJson(body)
    }

    companion object {

        internal fun parseOpenExchangeRatesJson(body: String): RemoteCurrencyRatesResult? = runCatching {
            val root = JSONObject(body)
            val ratesObj = root.optJSONObject("rates") ?: return@runCatching null
            val parsed = extractSupportedRates(ratesObj, lowercaseKeys = false)
            if (parsed.size < 8) return@runCatching null
            val xau = readPositiveDouble(ratesObj, "XAU")
            val tsSec = root.optLong("timestamp", 0L)
            RemoteCurrencyRatesResult(
                ratesPerUsd = parsed,
                xauPerUsd = xau,
                sourceLabel = "Open Exchange Rates",
                providerTimestampMillis = tsSec.takeIf { it > 0L }?.times(1000L)
            )
        }.getOrNull()

        internal fun parseCurrencyFreaksJson(body: String): RemoteCurrencyRatesResult? = runCatching {
            val root = JSONObject(body)
            val ratesObj = root.optJSONObject("rates") ?: return@runCatching null
            val parsed = extractSupportedRates(ratesObj, lowercaseKeys = false)
            if (parsed.size < 8) return@runCatching null
            val xau = readPositiveDouble(ratesObj, "XAU")
            RemoteCurrencyRatesResult(
                ratesPerUsd = parsed,
                xauPerUsd = xau,
                sourceLabel = "CurrencyFreaks"
            )
        }.getOrNull()

        internal fun parseExchangeRateApiV6Json(body: String): RemoteCurrencyRatesResult? = runCatching {
            val root = JSONObject(body)
            if (root.optString("result").equals("error", ignoreCase = true)) return@runCatching null
            val ratesObj = root.optJSONObject("conversion_rates") ?: return@runCatching null
            val parsed = extractSupportedRates(ratesObj, lowercaseKeys = false)
            if (parsed.size < 8) return@runCatching null
            val xau = readPositiveDouble(ratesObj, "XAU")
            val tsSec = root.optLong("time_last_update_unix", 0L)
            RemoteCurrencyRatesResult(
                ratesPerUsd = parsed,
                xauPerUsd = xau,
                sourceLabel = "ExchangeRate-API",
                providerTimestampMillis = tsSec.takeIf { it > 0L }?.times(1000L)
            )
        }.getOrNull()

        internal fun parseOpenErApiJson(body: String): RemoteCurrencyRatesResult? = runCatching {
            val root = JSONObject(body)
            val ratesObj = root.optJSONObject("rates") ?: return@runCatching null
            val parsed = extractSupportedRates(ratesObj, lowercaseKeys = false)
            if (parsed.size < 8) return@runCatching null
            val xau = readPositiveDouble(ratesObj, "XAU")
            val tsSec = root.optLong("time_last_update_unix", 0L)
            RemoteCurrencyRatesResult(
                ratesPerUsd = parsed,
                xauPerUsd = xau,
                sourceLabel = "Open ER-API",
                providerTimestampMillis = tsSec.takeIf { it > 0L }?.times(1000L)
            )
        }.getOrNull()

        internal fun parseFawazCurrencyJson(body: String): RemoteCurrencyRatesResult? = runCatching {
            val root = JSONObject(body)
            val usdObj = root.optJSONObject("usd") ?: return@runCatching null
            val parsed = extractSupportedRates(usdObj, lowercaseKeys = true)
            if (parsed.size < 8) return@runCatching null
            val xau = readPositiveDouble(usdObj, "xau")
            RemoteCurrencyRatesResult(
                ratesPerUsd = parsed,
                xauPerUsd = xau,
                sourceLabel = "Global FX CDN"
            )
        }.getOrNull()

        internal fun parseFrankfurterJson(body: String): RemoteCurrencyRatesResult? = runCatching {
            val root = JSONObject(body)
            val ratesObj = root.optJSONObject("rates") ?: return@runCatching null
            val parsed = extractSupportedRates(ratesObj, lowercaseKeys = false).toMutableMap()
            parsed["USD"] = 1.0
            if (parsed.size < 4) return@runCatching null
            RemoteCurrencyRatesResult(
                ratesPerUsd = parsed,
                xauPerUsd = null,
                sourceLabel = "Frankfurter (ECB)"
            )
        }.getOrNull()

        internal fun parseGoldApiIoJson(body: String): RemoteGoldSpotResult? = runCatching {
            val root = JSONObject(body)
            val price = readPositiveDouble(root, "price") ?: return@runCatching null
            if (price !in 800.0..12_000.0) return@runCatching null
            val tsSec = root.optLong("timestamp", 0L)
            RemoteGoldSpotResult(
                xauUsdPerOunce = price,
                usdPerGram24k = readPositiveDouble(root, "price_gram_24k"),
                usdPerGram22k = readPositiveDouble(root, "price_gram_22k"),
                usdPerGram21k = readPositiveDouble(root, "price_gram_21k"),
                usdPerGram18k = readPositiveDouble(root, "price_gram_18k"),
                sourceLabel = "GoldAPI.io (XAU)",
                providerTimestampMillis = tsSec.takeIf { it > 0L }?.times(1000L)
            )
        }.getOrNull()

        internal fun parseFastForexJson(body: String): RemoteGoldSpotResult? = runCatching {
            val root = JSONObject(body)
            val resultObj = root.optJSONObject("result") ?: return@runCatching null
            val price = readPositiveDouble(resultObj, "USD") ?: return@runCatching null
            if (price !in 800.0..12_000.0) return@runCatching null
            RemoteGoldSpotResult(
                xauUsdPerOunce = price,
                sourceLabel = "fastFOREX (XAU)"
            )
        }.getOrNull()

        internal fun parsePublicGoldApiJson(body: String): RemoteGoldSpotResult? = runCatching {
            val root = JSONObject(body)
            val price = readPositiveDouble(root, "price") ?: return@runCatching null
            if (price !in 800.0..12_000.0) return@runCatching null
            RemoteGoldSpotResult(
                xauUsdPerOunce = price,
                sourceLabel = "XAU Spot API"
            )
        }.getOrNull()

        private fun extractSupportedRates(obj: JSONObject, lowercaseKeys: Boolean): Map<String, Double> {
            val parsed = mutableMapOf<String, Double>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val rawKey = keys.next() ?: continue
                val normalized = rawKey.trim().uppercase(Locale.ROOT)
                if (normalized.length == 3 && normalized.all { it in 'A'..'Z' }) {
                    val v = readPositiveDouble(obj, rawKey)
                    if (v != null) {
                        parsed[normalized] = v
                    }
                }
            }
            // Also guarantee core MarketCurrency keys are probed explicitly
            MarketCurrency.entries.forEach { cur ->
                if (!parsed.containsKey(cur.code)) {
                    val key = if (lowercaseKeys) cur.code.lowercase(Locale.ROOT) else cur.code
                    val v = readPositiveDouble(obj, key)
                    if (v != null) parsed[cur.code] = v
                }
            }
            parsed["USD"] = 1.0
            return parsed
        }

        private fun readPositiveDouble(obj: JSONObject, key: String): Double? {
            if (!obj.has(key) || obj.isNull(key)) return null
            val raw = obj.opt(key) ?: return null
            val value = when (raw) {
                is Number -> raw.toDouble()
                is String -> raw.trim().toDoubleOrNull()
                else -> null
            }
            return value?.takeIf { !it.isNaN() && !it.isInfinite() && it > 0.0 }
        }
    }
}
