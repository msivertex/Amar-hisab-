package com.example

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.example.data.market.CurrencyApiProvider
import com.example.data.market.GoldApiProvider
import com.example.data.market.GoldCarat
import com.example.data.market.MarketApiConfig
import com.example.data.market.MarketCalculators
import com.example.data.market.MarketCurrency
import com.example.data.market.MarketDataRepository
import com.example.data.market.MarketDataStatus
import com.example.data.market.MarketRemoteApiService
import com.example.data.market.MarketSnapshot
import com.example.data.market.MarketSyncUiState
import com.example.data.market.RemoteGoldSpotResult
import com.example.localization.AppLanguage
import com.example.localization.StringsProvider
import com.example.ui.screens.LiveMarketOption
import com.example.ui.screens.LiveMarketScreen
import com.example.ui.theme.AmarHisabTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LiveMarketPipelineRobolectricTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun externalCurrencyAndGoldParsersSupportAllConfiguredProvidersAndTargetCurrencies() {
        // Key sanitization
        assertEquals("", MarketApiConfig.sanitizeKey("UNSET"))
        assertEquals("", MarketApiConfig.sanitizeKey("NONE"))
        assertEquals("", MarketApiConfig.sanitizeKey("YOUR_API_KEY"))
        assertEquals("", MarketApiConfig.sanitizeKey("   "))
        assertEquals("live-token-123", MarketApiConfig.sanitizeKey(" live-token-123 "))
        assertEquals(CurrencyApiProvider.CURRENCY_FREAKS, CurrencyApiProvider.fromConfig("currency_freaks"))
        assertEquals(GoldApiProvider.GOLD_API_IO, GoldApiProvider.fromConfig("gold_api_io"))

        // 1. Open Exchange Rates JSON (numeric rates + XAU)
        val oxrJson = """
            {
              "timestamp": 1735689600,
              "base": "USD",
              "rates": {
                "USD": 1, "BDT": 121.85, "KWD": 0.308, "SAR": 3.75, "AED": 3.6725,
                "QAR": 3.64, "MYR": 4.46, "EUR": 0.96, "GBP": 0.795, "INR": 85.40,
                "XAU": 0.000375
              }
            }
        """.trimIndent()
        val oxr = MarketRemoteApiService.parseOpenExchangeRatesJson(oxrJson)
        assertNotNull(oxr)
        assertEquals("Open Exchange Rates", oxr!!.sourceLabel)
        assertEquals(121.85, oxr.ratesPerUsd["BDT"]!!, 0.001)
        assertEquals(0.308, oxr.ratesPerUsd["KWD"]!!, 0.001)
        assertEquals(3.75, oxr.ratesPerUsd["SAR"]!!, 0.001)
        assertEquals(3.6725, oxr.ratesPerUsd["AED"]!!, 0.001)
        assertEquals(3.64, oxr.ratesPerUsd["QAR"]!!, 0.001)
        assertEquals(4.46, oxr.ratesPerUsd["MYR"]!!, 0.001)
        assertEquals(0.96, oxr.ratesPerUsd["EUR"]!!, 0.001)
        assertEquals(0.795, oxr.ratesPerUsd["GBP"]!!, 0.001)
        assertEquals(85.40, oxr.ratesPerUsd["INR"]!!, 0.001)
        assertEquals(0.000375, oxr.xauPerUsd!!, 0.000001)

        // 2. CurrencyFreaks JSON (string-formatted rate values)
        val cfJson = """
            {
              "base": "USD",
              "rates": {
                "USD": "1.0", "BDT": "122.10", "KWD": "0.3075", "SAR": "3.751",
                "AED": "3.6725", "QAR": "3.641", "MYR": "4.45", "EUR": "0.955",
                "GBP": "0.79", "INR": "85.20"
              }
            }
        """.trimIndent()
        val cf = MarketRemoteApiService.parseCurrencyFreaksJson(cfJson)
        assertNotNull(cf)
        assertEquals("CurrencyFreaks", cf!!.sourceLabel)
        assertEquals(122.10, cf.ratesPerUsd["BDT"]!!, 0.001)
        assertEquals(0.3075, cf.ratesPerUsd["KWD"]!!, 0.0001)

        // 3. ExchangeRate-API v6 JSON
        val erV6Json = """
            {
              "result": "success",
              "time_last_update_unix": 1735689600,
              "conversion_rates": {
                "USD": 1, "BDT": 122.40, "KWD": 0.307, "SAR": 3.75, "AED": 3.6725,
                "QAR": 3.64, "MYR": 4.43, "EUR": 0.94, "GBP": 0.785, "INR": 84.90
              }
            }
        """.trimIndent()
        val erV6 = MarketRemoteApiService.parseExchangeRateApiV6Json(erV6Json)
        assertNotNull(erV6)
        assertEquals("ExchangeRate-API", erV6!!.sourceLabel)
        assertEquals(122.40, erV6.ratesPerUsd["BDT"]!!, 0.001)

        // 4. Frankfurter ECB JSON
        val frankfurterJson = """
            {
              "base": "USD",
              "rates": { "EUR": 0.95, "GBP": 0.79, "MYR": 4.44, "INR": 85.10 }
            }
        """.trimIndent()
        val ff = MarketRemoteApiService.parseFrankfurterJson(frankfurterJson)
        assertNotNull(ff)
        assertEquals("Frankfurter (ECB)", ff!!.sourceLabel)
        assertEquals(0.95, ff.ratesPerUsd["EUR"]!!, 0.001)

        // 5. GoldAPI.io JSON (XAU spot + direct carat gram prices)
        val goldApiIoJson = """
            {
              "timestamp": 1735689600,
              "metal": "XAU",
              "currency": "USD",
              "price": 2680.50,
              "price_gram_24k": 86.18,
              "price_gram_22k": 79.00,
              "price_gram_21k": 75.41,
              "price_gram_18k": 64.64
            }
        """.trimIndent()
        val goldIo = MarketRemoteApiService.parseGoldApiIoJson(goldApiIoJson)
        assertNotNull(goldIo)
        assertEquals("GoldAPI.io (XAU)", goldIo!!.sourceLabel)
        assertEquals(2680.50, goldIo.xauUsdPerOunce, 0.01)
        assertEquals(86.18, goldIo.usdPerGram24k!!, 0.01)
        assertEquals(79.00, goldIo.usdPerGram22k!!, 0.01)

        // 6. fastFOREX JSON
        val fastForexJson = """{"base":"XAU","result":{"USD":2710.25}}"""
        val ffGold = MarketRemoteApiService.parseFastForexJson(fastForexJson)
        assertNotNull(ffGold)
        assertEquals("fastFOREX (XAU)", ffGold!!.sourceLabel)
        assertEquals(2710.25, ffGold.xauUsdPerOunce, 0.01)

        // 7. Public XAU Spot API JSON
        val publicGoldJson = """{"name":"Gold","price":2695.0,"symbol":"XAU"}"""
        val pubGold = MarketRemoteApiService.parsePublicGoldApiJson(publicGoldJson)
        assertNotNull(pubGold)
        assertEquals(2695.0, pubGold!!.xauUsdPerOunce, 0.01)
    }

    @Test
    fun goldXauSpotConversionFreshnessCadenceCacheAndRemittanceCustomMath() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val repo = MarketDataRepository(context)

        // Derive 24K and 22K per vori from live XAU spot ($2,700/oz) at 120 BDT/USD
        val derived = repo.deriveGoldVoriPrices(
            goldResult = RemoteGoldSpotResult(xauUsdPerOunce = 2700.0, sourceLabel = "GoldAPI.io (XAU)"),
            currentBdtPerUsd = 120.0,
            previous = MarketSnapshot.defaultReference()
        )
        // Expected 24K BDT/vori = (2700 / 31.1034768) * 120 * 11.664 ≈ 121,500
        assertEquals(121_500.0, derived.gold24kPerVoriBdt, 100.0)
        // Expected 22K BDT/vori ≈ 121,500 * 0.9167 ≈ 111,380
        assertEquals(111_400.0, derived.gold22kPerVoriBdt, 150.0)
        assertEquals(2700.0, derived.xauUsdPerOunce, 0.01)

        // Persist and verify round-trip caching with timestamps & XAU spot
        val syncTs = 1_740_000_000_000L
        runBlocking {
            repo.saveSnapshot(
                rates = mapOf("USD" to 1.0, "BDT" to 120.0, "KWD" to 0.30, "SAR" to 3.75),
                gold22kVoriBdt = derived.gold22kPerVoriBdt,
                syncedAtMillis = syncTs,
                sourceLabel = "CurrencyFreaks",
                gold24kVoriBdt = derived.gold24kPerVoriBdt,
                xauUsdPerOunce = 2700.0,
                goldSyncedAtMillis = syncTs,
                isGoldLiveSynced = true,
                goldSourceLabel = "GoldAPI.io (XAU)"
            )
            val restored = repo.getCachedSnapshot()
            assertEquals(MarketDataStatus.LIVE_SYNCED, restored.resolveCurrencyStatus(isOnline = true))
            assertEquals(MarketDataStatus.CACHED_OFFLINE, restored.resolveCurrencyStatus(isOnline = false))
            assertEquals(MarketDataStatus.LIVE_SYNCED, restored.resolveGoldStatus(isOnline = true))
            assertEquals("CurrencyFreaks", restored.sourceLabel)
            assertEquals("GoldAPI.io (XAU)", restored.goldSourceLabel)
            assertEquals(2700.0, restored.xauUsdPerOunce, 0.01)
            assertEquals(derived.gold24kPerVoriBdt, restored.goldPricePerVoriBdt(GoldCarat.K24), 50.0)
            // Verify 15-minute fresh sync cadence
            assertFalse(restored.isFreshSyncNeeded(nowMillis = syncTs + 5 * 60_000L))
            assertTrue(restored.isFreshSyncNeeded(nowMillis = syncTs + MarketSnapshot.FRESH_SYNC_INTERVAL_MS + 1000L))

            // Verify Gold price conversion into KWD (1 USD = 0.30 KWD, 1 USD = 120 BDT => 1 KWD = 400 BDT)
            val gold24kPerGramKwd = restored.goldPriceInCurrency(GoldCarat.K24, perGram = true, MarketCurrency.KWD)
            assertTrue("Expected realistic 24K KWD/gram (~26 KD), got $gold24kPerGramKwd", gold24kPerGramKwd in 20.0..35.0)

            // Verify customizable Remittance calculator with custom exchange rate & transfer fee
            val remCustom = MarketCalculators.calculateRemittance(
                foreignAmount = 100.0,
                fromCurrency = MarketCurrency.KWD,
                snapshot = restored,
                includeGovtIncentive = true,
                customRateOverride = 405.0,
                transferFeeBdt = 250.0
            )
            // Base = 40,500; Incentive (2.5%) = 1,012.5; Fee = 250 => Net = 41,262.5
            assertEquals(40_500.0, remCustom.baseBdt, 0.01)
            assertEquals(1_012.5, remCustom.incentiveBdt, 0.01)
            assertEquals(250.0, remCustom.feeBdt, 0.01)
            assertEquals(41_262.5, remCustom.totalReceivableBdt, 0.01)
        }
    }

    @Test
    fun liveMarketUiDisplaysSourceTransparency24kGoldCustomInputsAndGracefulFallback() {
        val syncedSnapshot = MarketSnapshot(
            ratesPerUsd = mapOf(
                "USD" to 1.0,
                "BDT" to 122.0,
                "KWD" to 0.305,
                "SAR" to 3.75,
                "AED" to 3.6725,
                "QAR" to 3.64,
                "MYR" to 4.44,
                "EUR" to 0.94,
                "GBP" to 0.79,
                "INR" to 85.0
            ),
            gold24kPerVoriBdt = 156_000.0,
            gold22kPerVoriBdt = 143_000.0,
            xauUsdPerOunce = 2715.50,
            lastSyncedMillis = System.currentTimeMillis(),
            isLiveSynced = true,
            sourceLabel = "Open ER-API",
            goldLastSyncedMillis = System.currentTimeMillis(),
            isGoldLiveSynced = true,
            goldSourceLabel = "GoldAPI.io (XAU)"
        )

        var syncState by mutableStateOf(
            MarketSyncUiState(
                snapshot = syncedSnapshot,
                isOnline = true,
                isSyncing = false,
                lastSyncFailed = true
            )
        )
        var refreshCount = 0

        composeRule.setContent {
            AmarHisabTheme(darkTheme = true, isBangla = true) {
                LiveMarketScreen(
                    language = AppLanguage.BN,
                    marketSyncState = syncState,
                    strings = StringsProvider.bangla,
                    onRefreshMarketRates = { refreshCount++ },
                    onBack = {}
                )
            }
        }

        // Sync-failed fallback banner with Retry button appears when online fetch fails
        composeRule.onNodeWithTag("market_offline_notice_banner").assertIsDisplayed()
        composeRule.onNodeWithTag("market_retry_sync_button").assertIsDisplayed()
        composeRule.onNodeWithTag("market_retry_sync_button").performClick()
        assertEquals(1, refreshCount)

        // Clearing sync failure hides the fallback banner cleanly
        syncState = syncState.copy(lastSyncFailed = false)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("market_offline_notice_banner").assertDoesNotExist()

        // 1. Gold Panel: 24K card, XAU spot pill, and source transparency badge
        composeRule.onNodeWithTag("live_market_option_${LiveMarketOption.GOLD_PRICE.name}").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("gold_source_transparency_badge").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("gold_xau_spot_pill").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("gold_carat_${GoldCarat.K24.name}").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("gold_carat_${GoldCarat.K22.name}").performScrollTo().assertIsDisplayed()

        // Back to options menu -> Essentials Panel: Reference badge + editable local price dialog
        composeRule.onNodeWithTag("live_market_all_options_chip").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("live_market_option_${LiveMarketOption.ESSENTIALS.name}").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("essentials_source_transparency_badge").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("essential_edit_price_rice_miniket").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("essential_price_dialog_input").performTextClearance()
        composeRule.onNodeWithTag("essential_price_dialog_input").performTextInput("82")
        composeRule.onNodeWithTag("essential_price_dialog_save").performClick()
        composeRule.waitForIdle()

        // Back to options menu -> Bank FDR/DPS Panel: Calculator badge + custom rate & tenure inputs
        composeRule.onNodeWithTag("live_market_all_options_chip").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("live_market_option_${LiveMarketOption.BANK_FDR_DPS.name}").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("bank_source_transparency_badge").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("bank_custom_tenure_input").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("bank_custom_rate_input").performScrollTo().assertIsDisplayed()

        // Back to options menu -> Remittance Panel: Indicative estimate disclaimer + custom rate & fee inputs
        composeRule.onNodeWithTag("live_market_all_options_chip").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("live_market_option_${LiveMarketOption.REMITTANCE_WALLET.name}").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("remittance_source_transparency_badge").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("remittance_estimate_disclaimer").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("remittance_custom_rate_input").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("remittance_fee_input").performScrollTo().assertIsDisplayed()
    }
}
