package com.example.data.ai

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

private const val TAG = "TtsAudioPlayer"

class TtsAudioPlayer(private val context: Context) {
    private var audioTrack: AudioTrack? = null
    private var mediaPlayer: MediaPlayer? = null

    @Volatile
    private var isPaused: Boolean = false

    @Volatile
    private var isStopped: Boolean = false

    suspend fun playAudio(
        audioBytes: ByteArray,
        mimeType: String,
        onProgress: (Float) -> Unit = {},
        onComplete: () -> Unit,
        onError: (String) -> Unit
    ) {
        stop()
        isPaused = false
        isStopped = false

        withContext(Dispatchers.IO) {
            try {
                val lowerMime = mimeType.lowercase(Locale.ROOT)
                if (lowerMime.contains("pcm") || lowerMime.contains("l16")) {
                    playPcm(audioBytes, lowerMime, onProgress, onComplete)
                } else {
                    playEncoded(audioBytes, lowerMime, onProgress, onComplete, onError)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Audio playback failed", e)
                if (!isStopped) {
                    withContext(Dispatchers.Main) {
                        onError(e.localizedMessage ?: "Audio playback failed")
                    }
                }
            }
        }
    }

    fun pause() {
        isPaused = true
        try {
            audioTrack?.let { track ->
                if (track.state == AudioTrack.STATE_INITIALIZED && track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                    track.pause()
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error pausing AudioTrack", e)
        }
        try {
            mediaPlayer?.let { player ->
                if (player.isPlaying) {
                    player.pause()
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error pausing MediaPlayer", e)
        }
    }

    fun resume() {
        isPaused = false
        try {
            audioTrack?.let { track ->
                if (track.state == AudioTrack.STATE_INITIALIZED && track.playState != AudioTrack.PLAYSTATE_PLAYING) {
                    track.play()
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error resuming AudioTrack", e)
        }
        try {
            mediaPlayer?.let { player ->
                if (!player.isPlaying) {
                    player.start()
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error resuming MediaPlayer", e)
        }
    }

    private suspend fun playPcm(
        pcmBytes: ByteArray,
        mimeType: String,
        onProgress: (Float) -> Unit,
        onComplete: () -> Unit
    ) {
        val sampleRate = parseSampleRate(mimeType) ?: 24000
        val minBufSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(4096)

        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setBufferSizeInBytes(minBufSize)
            .build()

        audioTrack = track
        track.play()

        var offset = 0
        val chunkSize = minBufSize
        val totalSize = pcmBytes.size.coerceAtLeast(1)

        while (offset < pcmBytes.size && !isStopped && audioTrack === track) {
            while (isPaused && !isStopped && audioTrack === track) {
                delay(60L)
            }
            if (isStopped || audioTrack !== track) break

            val toWrite = minOf(chunkSize, pcmBytes.size - offset)
            val written = track.write(pcmBytes, offset, toWrite)
            if (written <= 0) break
            offset += written
            val progress = (offset.toFloat() / totalSize.toFloat()).coerceIn(0f, 1f)
            withContext(Dispatchers.Main) {
                onProgress(progress)
            }
        }

        // Wait briefly for tail buffer drain if not stopped
        val durationMs = ((pcmBytes.size / 2) * 1000L) / sampleRate
        if (durationMs > 0 && !isStopped && audioTrack === track) {
            delay(minOf(350L, durationMs / 4))
        }

        try {
            if (track.state == AudioTrack.STATE_INITIALIZED) {
                track.stop()
            }
            track.release()
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing AudioTrack", e)
        }
        if (audioTrack === track) {
            audioTrack = null
        }

        if (!isStopped) {
            withContext(Dispatchers.Main) {
                onProgress(1f)
                onComplete()
            }
        }
    }

    private suspend fun playEncoded(
        bytes: ByteArray,
        mimeType: String,
        onProgress: (Float) -> Unit,
        onComplete: () -> Unit,
        onError: (String) -> Unit
    ) {
        val ext = when {
            mimeType.contains("wav") -> "wav"
            mimeType.contains("ogg") -> "ogg"
            mimeType.contains("mp3") || mimeType.contains("mpeg") -> "mp3"
            else -> "wav"
        }
        val tempFile = File.createTempFile("amar_hisab_tts_", ".$ext", context.cacheDir)
        tempFile.writeBytes(bytes)

        var completedOrErrored = false

        val player = withContext(Dispatchers.Main) {
            MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(tempFile.absolutePath)
                setOnCompletionListener {
                    completedOrErrored = true
                    it.release()
                    if (mediaPlayer === it) mediaPlayer = null
                    tempFile.delete()
                    onProgress(1f)
                    onComplete()
                }
                setOnErrorListener { mp, what, extra ->
                    completedOrErrored = true
                    mp.release()
                    if (mediaPlayer === mp) mediaPlayer = null
                    tempFile.delete()
                    onError("MediaPlayer error ($what, $extra)")
                    true
                }
                prepare()
                start()
            }.also { mediaPlayer = it }
        }

        // Emit periodic playback progress while active
        while (!completedOrErrored && !isStopped && mediaPlayer === player) {
            try {
                val dur = player.duration.coerceAtLeast(1)
                val pos = player.currentPosition.coerceIn(0, dur)
                val progress = (pos.toFloat() / dur.toFloat()).coerceIn(0f, 1f)
                withContext(Dispatchers.Main) {
                    if (!completedOrErrored && !isStopped) {
                        onProgress(progress)
                    }
                }
            } catch (_: Exception) {
                break
            }
            delay(150L)
        }
    }

    private fun parseSampleRate(mime: String): Int? {
        val match = Regex("rate=(\\d+)").find(mime)
        return match?.groupValues?.getOrNull(1)?.toIntOrNull()
    }

    fun stop() {
        isStopped = true
        isPaused = false
        try {
            audioTrack?.let { track ->
                if (track.state == AudioTrack.STATE_INITIALIZED) {
                    track.pause()
                    track.flush()
                    track.stop()
                }
                track.release()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping AudioTrack", e)
        } finally {
            audioTrack = null
        }

        try {
            mediaPlayer?.let { player ->
                if (player.isPlaying) player.stop()
                player.release()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping MediaPlayer", e)
        } finally {
            mediaPlayer = null
        }
    }
}
