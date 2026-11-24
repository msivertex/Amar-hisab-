package com.example

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.data.ai.AndroidNativeTtsDriver
import com.example.data.ai.BuiltInAudioFallbackDriver
import com.example.data.ai.InsightsSummaryHelper
import com.example.data.ai.MarketErrorCategory
import com.example.data.ai.NativeTtsDriver
import com.example.data.ai.NativeTtsReadyResult
import com.example.data.ai.SpeechEngineSource
import com.example.data.ai.SpeechTextChunker
import com.example.data.ai.VoiceLocaleMatch
import com.example.data.ai.VoiceSummaryPlaybackListener
import com.example.data.ai.VoiceSummarySpeechEngine
import com.example.data.local.AccountEntity
import com.example.data.local.LoanEntity
import com.example.data.local.SavingsGoalEntity
import com.example.data.local.TransactionEntity
import com.example.data.local.UserPrefs
import com.example.data.model.AccountType
import com.example.data.model.CategorySpend
import com.example.data.model.ExpenseCategory
import com.example.data.model.InsightTimeFilter
import com.example.data.model.LoanType
import com.example.data.model.TransactionType
import com.example.localization.AppLanguage
import com.example.localization.StringsProvider
import com.example.ui.screens.VoiceFinancialSummaryCard
import com.example.ui.theme.AmarHisabTheme
import com.example.ui.viewmodel.AnalyticsSnapshot
import com.example.ui.viewmodel.MainUiState
import com.example.ui.viewmodel.TtsUiState
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class VoiceFinancialSummaryRobolectricTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun sampleMainState(language: AppLanguage = AppLanguage.BN): MainUiState {
        val expenseBreakdown = listOf(
            CategorySpend(ExpenseCategory.FOOD_DINING, 14000.0, 0.45f),
            CategorySpend(ExpenseCategory.GROCERIES, 10000.0, 0.32f),
            CategorySpend(ExpenseCategory.TRANSPORT, 7000.0, 0.23f)
        )
        val incomeBreakdown = listOf(
            CategorySpend(ExpenseCategory.SALARY_INCOME, 75000.0, 0.88f),
            CategorySpend(ExpenseCategory.OTHER, 10000.0, 0.12f)
        )
        val analytics = AnalyticsSnapshot(
            periodLabel = if (language == AppLanguage.BN) "নভেম্বর ২০২৬" else "November 2026",
            totalIncome = 85000.0,
            totalExpense = 31000.0,
            netFlow = 54000.0,
            dailyAverage = 1033.0,
            peakAmount = 4200.0,
            categoryBreakdown = expenseBreakdown,
            incomeBreakdown = incomeBreakdown,
            previousPeriodExpense = 36500.0,
            expenseChangePercent = -15,
            savingsRatePercent = 63,
            topCategory = expenseBreakdown.first(),
            transactionCount = 18
        )
        return MainUiState(
            isPrefsLoaded = true,
            prefs = UserPrefs(
                language = language,
                userName = "শরিফুল",
                mainCurrencyCode = "BDT"
            ),
            accounts = listOf(
                AccountEntity(
                    id = 1L,
                    name = "City Bank",
                    type = AccountType.DEBIT_BANK,
                    balance = 95000.0,
                    bankOrProvider = "City Bank",
                    colorArgb = 0xFF0284C7L
                ),
                AccountEntity(
                    id = 2L,
                    name = "bKash",
                    type = AccountType.MFS,
                    balance = 12000.0,
                    bankOrProvider = "bKash",
                    colorArgb = 0xFFE2136EL
                )
            ),
            transactions = listOf(
                TransactionEntity(
                    id = 1L,
                    title = "Monthly Salary",
                    amount = 75000.0,
                    type = TransactionType.INCOME,
                    category = ExpenseCategory.SALARY_INCOME,
                    accountId = 1L
                )
            ),
            goals = listOf(
                SavingsGoalEntity(
                    id = 1L,
                    name = "Emergency Fund",
                    targetAmount = 200000.0,
                    savedAmount = 90000.0,
                    colorArgb = 0xFF0F766EL
                )
            ),
            loans = listOf(
                LoanEntity(
                    id = 1L,
                    personName = "Karim",
                    loanType = LoanType.OWED_TO_ME,
                    totalAmount = 5000.0,
                    paidAmount = 0.0
                )
            ),
            totalLiquidBalance = 107000.0,
            insightFilter = InsightTimeFilter.THIS_MONTH,
            analytics = analytics
        )
    }

    @Test
    fun `detailed summary includes all required sections in Bangla and global languages`() {
        val bnSummary = InsightsSummaryHelper.buildPeriodSummary(sampleMainState(AppLanguage.BN))
        assertTrue(bnSummary.contains("শরিফুল"))
        assertTrue(bnSummary.contains("এই মাসের"))
        assertTrue(bnSummary.contains("মোট আয়"))
        assertTrue(bnSummary.contains("মোট ব্যয়"))
        assertTrue(bnSummary.contains("নিট উদ্বৃত্ত"))
        assertTrue(bnSummary.contains("দৈনিক গড় খরচ"))
        assertTrue(bnSummary.contains("খাবার ও ডাইনিং"))
        assertTrue(bnSummary.contains("আয়ের শীর্ষ উৎস"))
        assertTrue(bnSummary.contains("১৫%"))
        assertTrue(bnSummary.contains("তরল ব্যালেন্স"))
        assertTrue(bnSummary.contains("সঞ্চয় লক্ষ্য"))

        val enSummary = InsightsSummaryHelper.buildPeriodSummary(sampleMainState(AppLanguage.EN))
        assertTrue(enSummary.contains("Hello"))
        assertTrue(enSummary.contains("total income"))
        assertTrue(enSummary.contains("net surplus"))
        assertTrue(enSummary.contains("daily average spending"))
        assertTrue(enSummary.contains("top three spending categories"))
        assertTrue(enSummary.contains("leading income sources"))
        assertTrue(enSummary.contains("down 15%"))

        val arSummary = InsightsSummaryHelper.buildPeriodSummary(sampleMainState(AppLanguage.AR))
        assertTrue(arSummary.contains("إجمالي الدخل"))
        assertTrue(arSummary.contains("متوسط الإنفاق اليومي"))

        val hiSummary = InsightsSummaryHelper.buildPeriodSummary(sampleMainState(AppLanguage.HI))
        assertTrue(hiSummary.contains("कुल आय"))
        assertTrue(hiSummary.contains("दैनिक औसत खर्च"))

        val urSummary = InsightsSummaryHelper.buildPeriodSummary(sampleMainState(AppLanguage.UR))
        assertTrue(urSummary.contains("کل آمدنی"))
        assertTrue(urSummary.contains("روزانہ اوسط خرچ"))
    }

    @Test
    fun `speech text chunker splits long multilingual summary cleanly without dropping content or decimals`() {
        val fullBangla = InsightsSummaryHelper.buildPeriodSummary(sampleMainState(AppLanguage.BN))
        val spoken = SpeechTextChunker.normalizeForSpeech(fullBangla, AppLanguage.BN, "BDT")
        assertFalse(spoken.contains("৳"))
        assertFalse(spoken.contains("%"))
        assertTrue(spoken.contains("টাকা"))
        assertTrue(spoken.contains("শতাংশ"))

        val chunks = SpeechTextChunker.splitIntoChunks(spoken, maxChunkLength = 140)
        assertTrue("Expected multiple chunks for detailed summary, got ${chunks.size}", chunks.size >= 3)
        chunks.forEach { chunk ->
            assertTrue("Chunk should not be blank", chunk.isNotBlank())
            assertTrue("Chunk length ${chunk.length} exceeded bound", chunk.length <= 140)
        }
        // Reconstructed words match original words
        val originalTokens = spoken.split(Regex("\\s+")).filter { it.isNotBlank() }
        val chunkTokens = chunks.joinToString(" ").split(Regex("\\s+")).filter { it.isNotBlank() }
        assertEquals(originalTokens, chunkTokens)

        // Decimal numbers stay intact
        val decimalChunks = SpeechTextChunker.splitIntoChunks(
            "Today you spent 12.50 dollars on coffee. Savings rose to 45,500.75 dollars!",
            maxChunkLength = 80
        )
        assertTrue(decimalChunks.any { it.contains("12.50") })
        assertTrue(decimalChunks.any { it.contains("45,500.75") })
    }

    @Test
    fun `candidate locales prioritize active language regional variants`() {
        val bnTags = AndroidNativeTtsDriver.candidateLocalesFor(AppLanguage.BN).map { it.toLanguageTag() }
        assertEquals("bn-BD", bnTags.first())
        assertTrue(bnTags.contains("bn-IN"))

        val arTags = AndroidNativeTtsDriver.candidateLocalesFor(AppLanguage.AR).map { it.toLanguageTag() }
        assertEquals("ar-SA", arTags.first())
        assertTrue(arTags.contains("ar-AE"))

        val urTags = AndroidNativeTtsDriver.candidateLocalesFor(AppLanguage.UR).map { it.toLanguageTag() }
        assertEquals("ur-PK", urTags.first())
        assertTrue(urTags.contains("ur-IN"))
    }

    @Test
    fun `speech engine speaks every chunk sequentially and supports pause resume stop and replay`() {
        val spokenChunks = mutableListOf<String>()
        var pendingOnDone: (() -> Unit)? = null
        var stoppedCount = 0

        val fakeNative = object : NativeTtsDriver {
            override suspend fun prepare(language: AppLanguage): NativeTtsReadyResult {
                return NativeTtsReadyResult.Ready(
                    VoiceLocaleMatch(Locale.forLanguageTag("bn-BD"), "বাংলা (bn-BD)", true)
                )
            }

            override fun speakChunk(
                utteranceId: String,
                text: String,
                onStart: () -> Unit,
                onDone: () -> Unit,
                onError: (String) -> Unit
            ): Boolean {
                spokenChunks.add(text)
                onStart()
                pendingOnDone = onDone
                return true
            }

            override fun stop() {
                stoppedCount++
                pendingOnDone = null
            }

            override fun shutdown() {
                stop()
            }
        }

        val unusedFallback = object : BuiltInAudioFallbackDriver {
            override suspend fun playScript(
                spokenScript: String,
                onProgress: (Float) -> Unit,
                onComplete: () -> Unit,
                onError: (String) -> Unit
            ) {
                error("Fallback should not be called when native TTS succeeds")
            }
            override fun pause() = Unit
            override fun resume() = Unit
            override fun stop() = Unit
        }

        val events = mutableListOf<String>()
        var lastPausedChunk = -1
        var completedTotalChunks = -1

        val listener = object : VoiceSummaryPlaybackListener {
            override fun onPreparing(script: String) {
                events.add("preparing")
            }
            override fun onPlaying(
                script: String,
                progress: Float,
                currentChunkIndex: Int,
                totalChunks: Int,
                engineSource: SpeechEngineSource,
                voiceLabel: String
            ) {
                events.add("playing:$currentChunkIndex/$totalChunks:${engineSource.name}")
            }
            override fun onPaused(
                script: String,
                progress: Float,
                currentChunkIndex: Int,
                totalChunks: Int,
                engineSource: SpeechEngineSource,
                voiceLabel: String
            ) {
                lastPausedChunk = currentChunkIndex
                events.add("paused:$currentChunkIndex/$totalChunks")
            }
            override fun onCompleted(
                script: String,
                totalChunks: Int,
                engineSource: SpeechEngineSource,
                voiceLabel: String
            ) {
                completedTotalChunks = totalChunks
                events.add("completed:$totalChunks")
            }
            override fun onStopped() {
                events.add("stopped")
            }
            override fun onUnavailable(script: String, reason: String) {
                events.add("unavailable:$reason")
            }
        }

        val engine = VoiceSummarySpeechEngine(
            nativeDriver = fakeNative,
            builtInAudioDriver = unusedFallback,
            mainDispatcher = Dispatchers.Unconfined
        )

        val script = InsightsSummaryHelper.buildPeriodSummary(sampleMainState(AppLanguage.BN))
        val expectedChunks = SpeechTextChunker.splitIntoChunks(
            SpeechTextChunker.normalizeForSpeech(script, AppLanguage.BN, "BDT")
        )
        assertTrue(expectedChunks.size >= 2)

        // Start playback -> chunk 0 is queued
        engine.play(script, AppLanguage.BN, "BDT", listener)
        assertEquals(1, spokenChunks.size)
        assertEquals(expectedChunks[0], spokenChunks[0])

        // Finish chunk 0 -> advances automatically to chunk 1
        pendingOnDone?.invoke()
        assertEquals(2, spokenChunks.size)
        assertEquals(expectedChunks[1], spokenChunks[1])

        // Pause at chunk 1 -> must preserve chunk index 1
        engine.pause()
        assertEquals(1, lastPausedChunk)

        // Resume -> must re-speak from chunk 1 (not restart from chunk 0)
        val beforeResumeSize = spokenChunks.size
        engine.resume()
        assertEquals(beforeResumeSize + 1, spokenChunks.size)
        assertEquals(expectedChunks[1], spokenChunks.last())

        // Finish all remaining chunks to completion
        var guard = 0
        while (pendingOnDone != null && guard < 30) {
            val cb = pendingOnDone
            pendingOnDone = null
            cb?.invoke()
            guard++
        }
        assertEquals(expectedChunks.size, completedTotalChunks)
        assertTrue(events.any { it == "completed:${expectedChunks.size}" })

        // Replay -> starts cleanly from chunk 0 again
        val beforeReplaySize = spokenChunks.size
        engine.replay(listener)
        assertEquals(beforeReplaySize + 1, spokenChunks.size)
        assertEquals(expectedChunks[0], spokenChunks.last())

        // Stop -> emits stopped
        engine.stop()
        assertTrue(events.last() == "stopped")
        engine.shutdown()
    }

    @Test
    fun `speech engine automatically falls back to built-in audio when native TTS is unavailable`() {
        var fallbackScriptReceived = ""
        var completedSource: SpeechEngineSource? = null

        val failingNative = object : NativeTtsDriver {
            override suspend fun prepare(language: AppLanguage): NativeTtsReadyResult {
                return NativeTtsReadyResult.Unavailable("No TTS service installed")
            }
            override fun speakChunk(
                utteranceId: String,
                text: String,
                onStart: () -> Unit,
                onDone: () -> Unit,
                onError: (String) -> Unit
            ): Boolean = false
            override fun stop() = Unit
            override fun shutdown() = Unit
        }

        val workingBuiltInFallback = object : BuiltInAudioFallbackDriver {
            override suspend fun playScript(
                spokenScript: String,
                onProgress: (Float) -> Unit,
                onComplete: () -> Unit,
                onError: (String) -> Unit
            ) {
                fallbackScriptReceived = spokenScript
                onProgress(0.5f)
                onProgress(1.0f)
                onComplete()
            }
            override fun pause() = Unit
            override fun resume() = Unit
            override fun stop() = Unit
        }

        val listener = object : VoiceSummaryPlaybackListener {
            override fun onPreparing(script: String) = Unit
            override fun onPlaying(
                script: String,
                progress: Float,
                currentChunkIndex: Int,
                totalChunks: Int,
                engineSource: SpeechEngineSource,
                voiceLabel: String
            ) = Unit
            override fun onPaused(
                script: String,
                progress: Float,
                currentChunkIndex: Int,
                totalChunks: Int,
                engineSource: SpeechEngineSource,
                voiceLabel: String
            ) = Unit
            override fun onCompleted(
                script: String,
                totalChunks: Int,
                engineSource: SpeechEngineSource,
                voiceLabel: String
            ) {
                completedSource = engineSource
            }
            override fun onStopped() = Unit
            override fun onUnavailable(script: String, reason: String) {
                error("Should not report unavailable when built-in audio fallback succeeds")
            }
        }

        val engine = VoiceSummarySpeechEngine(
            nativeDriver = failingNative,
            builtInAudioDriver = workingBuiltInFallback,
            mainDispatcher = Dispatchers.Unconfined
        )

        val script = InsightsSummaryHelper.buildPeriodSummary(sampleMainState(AppLanguage.EN))
        engine.play(script, AppLanguage.EN, "BDT", listener)

        assertTrue(fallbackScriptReceived.contains("total income"))
        assertEquals(SpeechEngineSource.BUILT_IN_AUDIO, completedSource)
        engine.shutdown()
    }

    @Test
    fun `voice summary card renders chunk progress replay and keeps full summary visible in completed and fallback states`() {
        val state = sampleMainState(AppLanguage.BN)
        val fullSummary = InsightsSummaryHelper.buildPeriodSummary(state)
        var ttsState by mutableStateOf<TtsUiState>(
            TtsUiState.Playing(
                script = fullSummary,
                progress = 0.4f,
                currentChunkIndex = 1,
                totalChunks = 5,
                engineLabel = "ডিভাইস ভয়েস",
                voiceLocaleLabel = "বাংলা (bn-BD)"
            )
        )
        var replayClicked = 0

        composeRule.setContent {
            AmarHisabTheme(darkTheme = false, isBangla = true) {
                VoiceFinancialSummaryCard(
                    state = state,
                    ttsState = ttsState,
                    strings = StringsProvider.bangla,
                    onPlayBriefingTts = {
                        replayClicked++
                        ttsState = TtsUiState.Playing(
                            script = fullSummary,
                            progress = 0.1f,
                            currentChunkIndex = 0,
                            totalChunks = 5,
                            engineLabel = "ডিভাইস ভয়েস",
                            voiceLocaleLabel = "বাংলা (bn-BD)"
                        )
                    },
                    onPauseTts = {
                        val cur = ttsState as? TtsUiState.Playing
                        if (cur != null) {
                            ttsState = TtsUiState.Paused(
                                script = cur.script,
                                progress = cur.progress,
                                currentChunkIndex = cur.currentChunkIndex,
                                totalChunks = cur.totalChunks,
                                engineLabel = cur.engineLabel,
                                voiceLocaleLabel = cur.voiceLocaleLabel
                            )
                        }
                    },
                    onResumeTts = {},
                    onStopTts = { ttsState = TtsUiState.Idle }
                )
            }
        }

        composeRule.waitForIdle()
        // Playing shows chunk progress (অংশ ২/৫), Pause, Replay, Stop, and auto-expanded readable summary
        composeRule.onNodeWithTag("tts_chunk_progress_label").assertIsDisplayed()
        composeRule.onNodeWithText("অংশ ২/৫").assertIsDisplayed()
        composeRule.onNodeWithTag("tts_pause_button").assertIsDisplayed()
        composeRule.onNodeWithTag("tts_replay_button").assertIsDisplayed()
        composeRule.onNodeWithTag("tts_stop_button").assertIsDisplayed()
        composeRule.onNodeWithTag("tts_summary_preview_box").assertIsDisplayed()

        // Completed state shows completion banner, full readable summary, and working Replay button
        ttsState = TtsUiState.Completed(
            script = fullSummary,
            totalChunks = 5,
            engineLabel = "ডিভাইস ভয়েস",
            voiceLocaleLabel = "বাংলা (bn-BD)"
        )
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tts_completed_notice").assertIsDisplayed()
        composeRule.onNodeWithTag("tts_summary_preview_box").assertIsDisplayed()
        composeRule.onNodeWithTag("tts_replay_button").assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        assertEquals(1, replayClicked)

        // Error fallback state keeps full detailed summary readable alongside retry button
        ttsState = TtsUiState.Error(
            message = "অফলাইনে নিচের টেক্সট সারসংক্ষেপ পড়তে পারবেন।",
            category = MarketErrorCategory.OFFLINE,
            script = fullSummary
        )
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tts_error_notice").assertIsDisplayed()
        composeRule.onNodeWithTag("tts_retry_button").assertIsDisplayed()
        composeRule.onNodeWithTag("tts_summary_preview_box").assertIsDisplayed()
    }
}
