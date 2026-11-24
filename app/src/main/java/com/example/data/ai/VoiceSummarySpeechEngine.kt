package com.example.data.ai

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.example.localization.AppLanguage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger

private const val VOICE_ENGINE_TAG = "VoiceSummarySpeech"
private const val GOOGLE_TTS_PACKAGE = "com.google.android.tts"

enum class SpeechEngineSource {
    NATIVE_TTS,
    BUILT_IN_AUDIO
}

data class VoiceLocaleMatch(
    val locale: Locale,
    val displayLabel: String,
    val isExactLanguageMatch: Boolean
)

/**
 * Callback interface emitted by [VoiceSummarySpeechEngine] so ViewModels can mirror
 * live chunked playback progress, pause/resume, completion, and fallback notices.
 */
interface VoiceSummaryPlaybackListener {
    fun onPreparing(script: String)
    fun onPlaying(
        script: String,
        progress: Float,
        currentChunkIndex: Int,
        totalChunks: Int,
        engineSource: SpeechEngineSource,
        voiceLabel: String
    )
    fun onPaused(
        script: String,
        progress: Float,
        currentChunkIndex: Int,
        totalChunks: Int,
        engineSource: SpeechEngineSource,
        voiceLabel: String
    )
    fun onCompleted(
        script: String,
        totalChunks: Int,
        engineSource: SpeechEngineSource,
        voiceLabel: String
    )
    fun onStopped()
    fun onUnavailable(script: String, reason: String)
}

/**
 * Abstraction over Android [TextToSpeech] so production uses real Google/system TTS
 * while Robolectric unit tests can drive deterministic chunked utterance lifecycles.
 */
interface NativeTtsDriver {
    suspend fun prepare(language: AppLanguage): NativeTtsReadyResult
    fun speakChunk(
        utteranceId: String,
        text: String,
        onStart: () -> Unit,
        onDone: () -> Unit,
        onError: (String) -> Unit
    ): Boolean
    fun stop()
    fun shutdown()
}

sealed interface NativeTtsReadyResult {
    data class Ready(val localeMatch: VoiceLocaleMatch) : NativeTtsReadyResult
    data class Unavailable(val reason: String) : NativeTtsReadyResult
}

/**
 * Abstraction over built-in app audio playback (Gemini/Google cloud TTS -> TtsAudioPlayer)
 * used as an automatic fallback when native Android TextToSpeech is unavailable or fails.
 */
interface BuiltInAudioFallbackDriver {
    suspend fun playScript(
        spokenScript: String,
        onProgress: (Float) -> Unit,
        onComplete: () -> Unit,
        onError: (String) -> Unit
    )
    fun pause()
    fun resume()
    fun stop()
}

/**
 * Coordinates full, continuous, chunked voice playback for the Financial Summary:
 * 1. Primary path: Native Android TextToSpeech (preferring Google Speech Services) matched
 *    to the active [AppLanguage], speaking every sentence chunk sequentially without truncation.
 * 2. Automatic fallback: Built-in app audio playback ([TtsAudioPlayer] + [GeminiAiService])
 *    if native TTS is missing, uninitialized, or encounters an utterance error.
 * 3. Full Pause / Resume (preserving exact chunk index), Stop, and Replay controls.
 */
class VoiceSummarySpeechEngine(
    private val nativeDriver: NativeTtsDriver,
    private val builtInAudioDriver: BuiltInAudioFallbackDriver,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
    private val chunkStartTimeoutMs: Long = 10_000L,
    private val chunkDoneTimeoutMs: Long = 35_000L
) {
    private val scope = CoroutineScope(SupervisorJob() + mainDispatcher)
    private val sessionCounter = AtomicInteger(0)

    private var activeJob: Job? = null
    private var activeEngine: SpeechEngineSource? = null
    private var activeListener: VoiceSummaryPlaybackListener? = null

    private var currentDisplayScript: String = ""
    private var currentSpokenScript: String = ""
    private var currentLanguage: AppLanguage = AppLanguage.BN
    private var currentChunks: List<String> = emptyList()
    private var currentChunkIndex: Int = 0
    private var currentProgress: Float = 0f
    private var currentVoiceLabel: String = ""
    private var isPausedState: Boolean = false

    fun play(
        displayScript: String,
        language: AppLanguage,
        currencyCode: String,
        listener: VoiceSummaryPlaybackListener
    ) {
        val cleanDisplay = displayScript.trim()
        if (cleanDisplay.isEmpty()) {
            listener.onStopped()
            return
        }

        stopInternal(notifyStopped = false)

        val spoken = SpeechTextChunker.normalizeForSpeech(cleanDisplay, language, currencyCode)
        val chunks = SpeechTextChunker.splitIntoChunks(spoken)
        if (chunks.isEmpty()) {
            listener.onStopped()
            return
        }

        currentDisplayScript = cleanDisplay
        currentSpokenScript = spoken
        currentLanguage = language
        currentChunks = chunks
        currentChunkIndex = 0
        currentProgress = 0f
        currentVoiceLabel = language.nativeName
        isPausedState = false
        activeListener = listener

        startNativePlaybackFrom(chunkIndex = 0)
    }

    fun replay(listener: VoiceSummaryPlaybackListener? = activeListener) {
        val targetListener = listener ?: return
        if (currentDisplayScript.isBlank() || currentChunks.isEmpty()) return
        stopInternal(notifyStopped = false)
        currentChunkIndex = 0
        currentProgress = 0f
        isPausedState = false
        activeListener = targetListener
        startNativePlaybackFrom(chunkIndex = 0)
    }

    fun pause() {
        val listener = activeListener ?: return
        if (isPausedState) return
        val engine = activeEngine ?: return
        isPausedState = true

        when (engine) {
            SpeechEngineSource.NATIVE_TTS -> {
                sessionCounter.incrementAndGet()
                activeJob?.cancel()
                activeJob = null
                nativeDriver.stop()
                listener.onPaused(
                    script = currentDisplayScript,
                    progress = currentProgress,
                    currentChunkIndex = currentChunkIndex,
                    totalChunks = currentChunks.size.coerceAtLeast(1),
                    engineSource = SpeechEngineSource.NATIVE_TTS,
                    voiceLabel = currentVoiceLabel
                )
            }
            SpeechEngineSource.BUILT_IN_AUDIO -> {
                builtInAudioDriver.pause()
                listener.onPaused(
                    script = currentDisplayScript,
                    progress = currentProgress,
                    currentChunkIndex = currentChunkIndex,
                    totalChunks = currentChunks.size.coerceAtLeast(1),
                    engineSource = SpeechEngineSource.BUILT_IN_AUDIO,
                    voiceLabel = currentVoiceLabel
                )
            }
        }
    }

    fun resume() {
        val listener = activeListener ?: return
        if (!isPausedState) return
        val engine = activeEngine ?: return
        isPausedState = false

        when (engine) {
            SpeechEngineSource.NATIVE_TTS -> {
                val resumeIdx = currentChunkIndex.coerceIn(0, (currentChunks.size - 1).coerceAtLeast(0))
                startNativePlaybackFrom(chunkIndex = resumeIdx)
            }
            SpeechEngineSource.BUILT_IN_AUDIO -> {
                builtInAudioDriver.resume()
                listener.onPlaying(
                    script = currentDisplayScript,
                    progress = currentProgress,
                    currentChunkIndex = currentChunkIndex,
                    totalChunks = currentChunks.size.coerceAtLeast(1),
                    engineSource = SpeechEngineSource.BUILT_IN_AUDIO,
                    voiceLabel = currentVoiceLabel
                )
            }
        }
    }

    fun stop() {
        stopInternal(notifyStopped = true)
    }

    fun shutdown() {
        stopInternal(notifyStopped = false)
        nativeDriver.shutdown()
        scope.cancel()
    }

    private fun stopInternal(notifyStopped: Boolean) {
        sessionCounter.incrementAndGet()
        activeJob?.cancel()
        activeJob = null
        isPausedState = false
        activeEngine = null
        nativeDriver.stop()
        builtInAudioDriver.stop()
        if (notifyStopped) {
            activeListener?.onStopped()
        }
    }

    private fun startNativePlaybackFrom(chunkIndex: Int) {
        val sessionId = sessionCounter.incrementAndGet()
        val listener = activeListener ?: return
        val chunks = currentChunks
        val total = chunks.size.coerceAtLeast(1)

        activeJob?.cancel()
        activeJob = scope.launch {
            listener.onPreparing(currentDisplayScript)

            val ready = try {
                nativeDriver.prepare(currentLanguage)
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: Exception) {
                NativeTtsReadyResult.Unavailable(e.localizedMessage ?: "Native TTS initialization failed")
            }

            if (sessionId != sessionCounter.get()) return@launch

            when (ready) {
                is NativeTtsReadyResult.Ready -> {
                    activeEngine = SpeechEngineSource.NATIVE_TTS
                    currentVoiceLabel = ready.localeMatch.displayLabel

                    for (idx in chunkIndex until chunks.size) {
                        if (sessionId != sessionCounter.get()) return@launch

                        currentChunkIndex = idx
                        val startProgress = (idx.toFloat() / total.toFloat()).coerceIn(0f, 0.99f)
                        currentProgress = startProgress

                        val startedSignal = CompletableDeferred<Boolean>()
                        val doneSignal = CompletableDeferred<Result<Unit>>()
                        val utteranceId = "amar_hisab_summary_${sessionId}_$idx"

                        val queued = nativeDriver.speakChunk(
                            utteranceId = utteranceId,
                            text = chunks[idx],
                            onStart = {
                                if (sessionId == sessionCounter.get()) {
                                    startedSignal.complete(true)
                                    val activeProgress = ((idx + 0.15f) / total.toFloat()).coerceIn(0f, 0.99f)
                                    currentProgress = activeProgress
                                    scope.launch {
                                        if (sessionId == sessionCounter.get() && !isPausedState) {
                                            listener.onPlaying(
                                                script = currentDisplayScript,
                                                progress = activeProgress,
                                                currentChunkIndex = idx,
                                                totalChunks = total,
                                                engineSource = SpeechEngineSource.NATIVE_TTS,
                                                voiceLabel = currentVoiceLabel
                                            )
                                        }
                                    }
                                }
                            },
                            onDone = {
                                if (sessionId == sessionCounter.get()) {
                                    startedSignal.complete(true)
                                    doneSignal.complete(Result.success(Unit))
                                }
                            },
                            onError = { err ->
                                if (sessionId == sessionCounter.get()) {
                                    startedSignal.complete(false)
                                    doneSignal.complete(Result.failure(IllegalStateException(err)))
                                }
                            }
                        )

                        if (!queued) {
                            fallbackToBuiltInAudio(
                                sessionId = sessionId,
                                fromChunkIndex = idx,
                                nativeFailureReason = "Native TTS rejected utterance"
                            )
                            return@launch
                        }

                        // Emit playing state immediately so UI transitions crisply without waiting
                        listener.onPlaying(
                            script = currentDisplayScript,
                            progress = startProgress,
                            currentChunkIndex = idx,
                            totalChunks = total,
                            engineSource = SpeechEngineSource.NATIVE_TTS,
                            voiceLabel = currentVoiceLabel
                        )

                        val didStart = withTimeoutOrNull(chunkStartTimeoutMs) { startedSignal.await() }
                        if (sessionId != sessionCounter.get()) return@launch
                        if (didStart != true) {
                            nativeDriver.stop()
                            fallbackToBuiltInAudio(
                                sessionId = sessionId,
                                fromChunkIndex = idx,
                                nativeFailureReason = "Native TTS did not start playback"
                            )
                            return@launch
                        }

                        val chunkResult = withTimeoutOrNull(chunkDoneTimeoutMs) { doneSignal.await() }
                        if (sessionId != sessionCounter.get()) return@launch
                        if (chunkResult == null || chunkResult.isFailure) {
                            nativeDriver.stop()
                            val reason = chunkResult?.exceptionOrNull()?.message ?: "Native TTS timed out"
                            fallbackToBuiltInAudio(
                                sessionId = sessionId,
                                fromChunkIndex = idx,
                                nativeFailureReason = reason
                            )
                            return@launch
                        }

                        val finishedProgress = ((idx + 1).toFloat() / total.toFloat()).coerceIn(0f, 1f)
                        currentProgress = finishedProgress
                        listener.onPlaying(
                            script = currentDisplayScript,
                            progress = finishedProgress,
                            currentChunkIndex = idx,
                            totalChunks = total,
                            engineSource = SpeechEngineSource.NATIVE_TTS,
                            voiceLabel = currentVoiceLabel
                        )
                    }

                    if (sessionId == sessionCounter.get() && !isPausedState) {
                        activeEngine = null
                        currentChunkIndex = total - 1
                        currentProgress = 1f
                        listener.onCompleted(
                            script = currentDisplayScript,
                            totalChunks = total,
                            engineSource = SpeechEngineSource.NATIVE_TTS,
                            voiceLabel = currentVoiceLabel
                        )
                    }
                }

                is NativeTtsReadyResult.Unavailable -> {
                    fallbackToBuiltInAudio(
                        sessionId = sessionId,
                        fromChunkIndex = chunkIndex,
                        nativeFailureReason = ready.reason
                    )
                }
            }
        }
    }

    private suspend fun fallbackToBuiltInAudio(
        sessionId: Int,
        fromChunkIndex: Int,
        nativeFailureReason: String
    ) {
        if (sessionId != sessionCounter.get()) return
        val listener = activeListener ?: return
        val remainingText = currentChunks
            .drop(fromChunkIndex.coerceAtLeast(0))
            .joinToString(" ")
            .ifBlank { currentSpokenScript }
        val total = currentChunks.size.coerceAtLeast(1)
        val audioVoiceLabel = if (currentLanguage == AppLanguage.BN) {
            "বিল্ট-ইন অডিও"
        } else {
            "Built-in Audio"
        }

        activeEngine = SpeechEngineSource.BUILT_IN_AUDIO
        currentVoiceLabel = audioVoiceLabel
        listener.onPreparing(currentDisplayScript)

        try {
            builtInAudioDriver.playScript(
                spokenScript = remainingText,
                onProgress = { pct ->
                    if (sessionId != sessionCounter.get()) return@playScript
                    val baseOffset = fromChunkIndex.toFloat() / total.toFloat()
                    val mapped = (baseOffset + (1f - baseOffset) * pct).coerceIn(0f, 1f)
                    currentProgress = mapped
                    currentChunkIndex = ((mapped * total).toInt()).coerceIn(0, total - 1)
                    if (!isPausedState) {
                        listener.onPlaying(
                            script = currentDisplayScript,
                            progress = mapped,
                            currentChunkIndex = currentChunkIndex,
                            totalChunks = total,
                            engineSource = SpeechEngineSource.BUILT_IN_AUDIO,
                            voiceLabel = audioVoiceLabel
                        )
                    }
                },
                onComplete = {
                    if (sessionId != sessionCounter.get()) return@playScript
                    activeEngine = null
                    currentProgress = 1f
                    currentChunkIndex = total - 1
                    listener.onCompleted(
                        script = currentDisplayScript,
                        totalChunks = total,
                        engineSource = SpeechEngineSource.BUILT_IN_AUDIO,
                        voiceLabel = audioVoiceLabel
                    )
                },
                onError = { cloudErr ->
                    if (sessionId != sessionCounter.get()) return@playScript
                    activeEngine = null
                    listener.onUnavailable(
                        script = currentDisplayScript,
                        reason = "$nativeFailureReason | $cloudErr"
                    )
                }
            )
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            if (sessionId != sessionCounter.get()) return
            activeEngine = null
            listener.onUnavailable(
                script = currentDisplayScript,
                reason = "$nativeFailureReason | ${e.localizedMessage ?: "Audio fallback failed"}"
            )
        }
    }

    companion object {
        fun createDefault(
            context: Context,
            aiService: GeminiAiService,
            ttsAudioPlayer: TtsAudioPlayer
        ): VoiceSummarySpeechEngine {
            return VoiceSummarySpeechEngine(
                nativeDriver = AndroidNativeTtsDriver(context.applicationContext),
                builtInAudioDriver = GeminiBuiltInAudioFallbackDriver(aiService, ttsAudioPlayer)
            )
        }
    }
}

/**
 * Production Android [TextToSpeech] driver that prefers Google Speech Services when installed,
 * resolves the best voice/locale for the active [AppLanguage], manages transient audio focus,
 * and dispatches utterance callbacks cleanly.
 */
class AndroidNativeTtsDriver(
    private val context: Context
) : NativeTtsDriver {

    private var tts: TextToSpeech? = null
    private var isInitialized: Boolean = false
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null

    private val callbacksLock = Any()
    private val utteranceCallbacks = mutableMapOf<String, UtteranceCallbacks>()

    private data class UtteranceCallbacks(
        val onStart: () -> Unit,
        val onDone: () -> Unit,
        val onError: (String) -> Unit
    )

    override suspend fun prepare(language: AppLanguage): NativeTtsReadyResult {
        val engine = ensureInitialized()
            ?: return NativeTtsReadyResult.Unavailable("Android TextToSpeech engine is not available on this device.")

        val localeMatch = resolveAndApplyVoice(engine, language)
        engine.setSpeechRate(0.94f)
        engine.setPitch(1.0f)
        try {
            engine.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
        } catch (e: Exception) {
            Log.w(VOICE_ENGINE_TAG, "Unable to set TTS audio attributes", e)
        }
        return NativeTtsReadyResult.Ready(localeMatch)
    }

    private suspend fun ensureInitialized(): TextToSpeech? {
        if (isInitialized && tts != null) return tts

        // Try Google Speech Services first if present, then default system engine
        val googleTts = initSingleEngine(preferGoogleEngine = true)
        if (googleTts != null) {
            tts = googleTts
            isInitialized = true
            return googleTts
        }

        val systemTts = initSingleEngine(preferGoogleEngine = false)
        if (systemTts != null) {
            tts = systemTts
            isInitialized = true
            return systemTts
        }

        return null
    }

    private suspend fun initSingleEngine(preferGoogleEngine: Boolean): TextToSpeech? {
        return withContext(Dispatchers.Main) {
            val initResult = CompletableDeferred<Int>()
            var instance: TextToSpeech? = null
            try {
                val listener = TextToSpeech.OnInitListener { status ->
                    if (!initResult.isCompleted) {
                        initResult.complete(status)
                    }
                }
                instance = if (preferGoogleEngine) {
                    TextToSpeech(context, listener, GOOGLE_TTS_PACKAGE)
                } else {
                    TextToSpeech(context, listener)
                }
            } catch (e: Exception) {
                Log.w(VOICE_ENGINE_TAG, "TextToSpeech constructor failed (google=$preferGoogleEngine)", e)
                return@withContext null
            }

            val status = withTimeoutOrNull(4_500L) { initResult.await() }
            if (status == TextToSpeech.SUCCESS) {
                instance.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        val cb = utteranceId?.let { id -> synchronized(callbacksLock) { utteranceCallbacks[id] } }
                        cb?.onStart?.invoke()
                    }

                    override fun onDone(utteranceId: String?) {
                        val cb = utteranceId?.let { id -> synchronized(callbacksLock) { utteranceCallbacks.remove(id) } }
                        cb?.onDone?.invoke()
                    }

                    @Deprecated("Deprecated in Android API")
                    override fun onError(utteranceId: String?) {
                        val cb = utteranceId?.let { id -> synchronized(callbacksLock) { utteranceCallbacks.remove(id) } }
                        cb?.onError?.invoke("TTS synthesis error")
                    }

                    override fun onError(utteranceId: String?, errorCode: Int) {
                        val cb = utteranceId?.let { id -> synchronized(callbacksLock) { utteranceCallbacks.remove(id) } }
                        cb?.onError?.invoke("TTS synthesis error ($errorCode)")
                    }
                })
                instance
            } else {
                try {
                    instance.shutdown()
                } catch (_: Exception) {
                }
                null
            }
        }
    }

    override fun speakChunk(
        utteranceId: String,
        text: String,
        onStart: () -> Unit,
        onDone: () -> Unit,
        onError: (String) -> Unit
    ): Boolean {
        val engine = tts ?: return false
        requestAudioFocus()
        synchronized(callbacksLock) {
            utteranceCallbacks[utteranceId] = UtteranceCallbacks(onStart, onDone, onError)
        }
        val params = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
        }
        val code = try {
            engine.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
        } catch (e: Exception) {
            Log.e(VOICE_ENGINE_TAG, "TTS speak failed", e)
            TextToSpeech.ERROR
        }
        if (code != TextToSpeech.SUCCESS) {
            synchronized(callbacksLock) {
                utteranceCallbacks.remove(utteranceId)
            }
            return false
        }
        return true
    }

    override fun stop() {
        synchronized(callbacksLock) {
            utteranceCallbacks.clear()
        }
        try {
            tts?.stop()
        } catch (e: Exception) {
            Log.w(VOICE_ENGINE_TAG, "Error stopping TTS", e)
        }
        abandonAudioFocus()
    }

    override fun shutdown() {
        stop()
        try {
            tts?.shutdown()
        } catch (e: Exception) {
            Log.w(VOICE_ENGINE_TAG, "Error shutting down TTS", e)
        }
        tts = null
        isInitialized = false
    }

    private fun resolveAndApplyVoice(engine: TextToSpeech, language: AppLanguage): VoiceLocaleMatch {
        val candidates = candidateLocalesFor(language)

        // 1. Try selecting the best installed Voice matching candidate locales
        val installedVoices = try {
            engine.voices?.toList().orEmpty()
        } catch (_: Exception) {
            emptyList()
        }

        for (candidate in candidates) {
            val matchingVoices = installedVoices.filter { voice ->
                val vLocale = voice.locale ?: return@filter false
                val sameLang = vLocale.language.equals(candidate.language, ignoreCase = true)
                if (!sameLang) return@filter false
                if (candidate.country.isNotBlank()) {
                    vLocale.country.equals(candidate.country, ignoreCase = true)
                } else {
                    true
                }
            }.sortedWith(
                compareBy<android.speech.tts.Voice> { it.isNetworkConnectionRequired }
                    .thenByDescending { it.quality }
            )

            val bestVoice = matchingVoices.firstOrNull()
            if (bestVoice != null) {
                val res = try {
                    engine.setVoice(bestVoice)
                } catch (_: Exception) {
                    TextToSpeech.ERROR
                }
                if (res == TextToSpeech.SUCCESS) {
                    return VoiceLocaleMatch(
                        locale = bestVoice.locale,
                        displayLabel = "${language.nativeName} (${bestVoice.locale.toLanguageTag()})",
                        isExactLanguageMatch = true
                    )
                }
            }

            // 2. Try setLanguage directly for the candidate locale
            val availability = try {
                engine.isLanguageAvailable(candidate)
            } catch (_: Exception) {
                TextToSpeech.LANG_NOT_SUPPORTED
            }
            if (availability >= TextToSpeech.LANG_AVAILABLE) {
                val setRes = try {
                    engine.setLanguage(candidate)
                } catch (_: Exception) {
                    TextToSpeech.LANG_NOT_SUPPORTED
                }
                if (setRes >= TextToSpeech.LANG_AVAILABLE) {
                    return VoiceLocaleMatch(
                        locale = candidate,
                        displayLabel = "${language.nativeName} (${candidate.toLanguageTag()})",
                        isExactLanguageMatch = true
                    )
                }
            }
        }

        // 3. Clean fallback: try English or system default so playback still speaks smoothly
        val fallbackLocales = listOf(Locale.US, Locale.UK, Locale.getDefault())
        for (fallback in fallbackLocales) {
            val setRes = try {
                engine.setLanguage(fallback)
            } catch (_: Exception) {
                TextToSpeech.LANG_NOT_SUPPORTED
            }
            if (setRes >= TextToSpeech.LANG_AVAILABLE) {
                return VoiceLocaleMatch(
                    locale = fallback,
                    displayLabel = fallback.displayLanguage.ifBlank { fallback.toLanguageTag() },
                    isExactLanguageMatch = false
                )
            }
        }

        return VoiceLocaleMatch(
            locale = Locale.getDefault(),
            displayLabel = language.nativeName,
            isExactLanguageMatch = false
        )
    }

    private fun requestAudioFocus() {
        val am = audioManager ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                    )
                    .build()
                audioFocusRequest = req
                am.requestAudioFocus(req)
            } else {
                @Suppress("DEPRECATION")
                am.requestAudioFocus(
                    null,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
                )
            }
        } catch (e: Exception) {
            Log.w(VOICE_ENGINE_TAG, "Unable to request audio focus", e)
        }
    }

    private fun abandonAudioFocus() {
        val am = audioManager ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest?.let { am.abandonAudioFocusRequest(it) }
                audioFocusRequest = null
            } else {
                @Suppress("DEPRECATION")
                am.abandonAudioFocus(null)
            }
        } catch (e: Exception) {
            Log.w(VOICE_ENGINE_TAG, "Unable to abandon audio focus", e)
        }
    }

    companion object {
        fun candidateLocalesFor(language: AppLanguage): List<Locale> {
            val primary = Locale.forLanguageTag(language.localeTag)
            val extras = when (language) {
                AppLanguage.BN -> listOf(
                    Locale.forLanguageTag("bn-BD"),
                    Locale.forLanguageTag("bn-IN"),
                    Locale("bn")
                )
                AppLanguage.EN -> listOf(
                    Locale.US,
                    Locale.UK,
                    Locale.forLanguageTag("en-IN"),
                    Locale.ENGLISH
                )
                AppLanguage.AR -> listOf(
                    Locale.forLanguageTag("ar-SA"),
                    Locale.forLanguageTag("ar-AE"),
                    Locale.forLanguageTag("ar-EG"),
                    Locale("ar")
                )
                AppLanguage.HI -> listOf(
                    Locale.forLanguageTag("hi-IN"),
                    Locale("hi")
                )
                AppLanguage.UR -> listOf(
                    Locale.forLanguageTag("ur-PK"),
                    Locale.forLanguageTag("ur-IN"),
                    Locale("ur")
                )
                AppLanguage.MS -> listOf(
                    Locale.forLanguageTag("ms-MY"),
                    Locale("ms")
                )
                AppLanguage.ES -> listOf(
                    Locale.forLanguageTag("es-ES"),
                    Locale.forLanguageTag("es-US"),
                    Locale("es")
                )
                AppLanguage.PT -> listOf(
                    Locale.forLanguageTag("pt-PT"),
                    Locale.forLanguageTag("pt-BR"),
                    Locale("pt")
                )
                AppLanguage.ZH -> listOf(
                    Locale.SIMPLIFIED_CHINESE,
                    Locale.TRADITIONAL_CHINESE,
                    Locale.CHINESE
                )
                else -> listOf(Locale(language.code))
            }
            return (listOf(primary) + extras)
                .distinctBy { it.toLanguageTag().lowercase(Locale.ROOT) }
        }
    }
}

/**
 * Built-in app audio fallback backed by [GeminiAiService] + [TtsAudioPlayer].
 * Automatically invoked only if Android's native TextToSpeech engine is unavailable or fails.
 */
class GeminiBuiltInAudioFallbackDriver(
    private val aiService: GeminiAiService,
    private val ttsPlayer: TtsAudioPlayer
) : BuiltInAudioFallbackDriver {

    override suspend fun playScript(
        spokenScript: String,
        onProgress: (Float) -> Unit,
        onComplete: () -> Unit,
        onError: (String) -> Unit
    ) {
        val payload = aiService.generateBriefingTts(spokenScript)
        ttsPlayer.playAudio(
            audioBytes = payload.audioBytes,
            mimeType = payload.mimeType,
            onProgress = onProgress,
            onComplete = onComplete,
            onError = onError
        )
    }

    override fun pause() {
        ttsPlayer.pause()
    }

    override fun resume() {
        ttsPlayer.resume()
    }

    override fun stop() {
        ttsPlayer.stop()
    }
}
