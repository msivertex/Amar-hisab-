package com.example.notifications

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.net.Uri
import android.os.Looper
import android.util.Log
import com.example.R
import com.example.core.ProfilePhotoUrlNormalizer
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.min

/**
 * Resolves crisp, high-resolution (`192x192` ARGB_8888) notification visuals for Android's
 * notification banner and shade (`NotificationCompat.Builder.setLargeIcon`):
 *
 * 1. If the user has an active high-resolution Google profile photo or custom local avatar,
 *    renders a crisp circular-cropped high-DPI bitmap (cached persistently in app-private storage
 *    so background AlarmManager broadcasts render instantly even offline or after reboot).
 * 2. Otherwise falls back to the lossless high-resolution Amar Hisab brand logo
 *    (`R.drawable.img_amar_hisab_logo`), framed cleanly to match the in-app `AmarHisabLogoBadge`.
 */
object NotificationVisualProvider {

    private const val TAG = "NotifVisualProvider"

    /** Amar Hisab signature cobalt-cerulean brand accent for Android notification badge & title tint. */
    const val BRAND_ACCENT_COLOR: Int = 0xFF0684D1.toInt()

    /** 48dp at 4x (xxxhdpi) = 192px — guarantees razor-sharp rendering on all modern Android phones. */
    const val LARGE_ICON_SIZE_PX: Int = 192

    private const val PREFS_VISUAL = "amar_hisab_notif_visual_prefs"
    private const val KEY_CACHED_AVATAR_URL = "cached_avatar_url"
    private const val AVATAR_CACHE_FILENAME = "amar_hisab_notif_avatar.png"

    @Volatile
    private var cachedLogoBitmap: Bitmap? = null

    /**
     * Synchronizes the persistent circular profile avatar file on disk for instant, offline-safe
     * notification rendering. Safe to call on a background IO dispatcher whenever `effectiveAvatarUrl`
     * changes or on app launch.
     */
    fun syncProfileAvatarCache(context: Context, rawAvatarUrl: String?) {
        val appContext = context.applicationContext
        val normalized = ProfilePhotoUrlNormalizer.toHighResUrl(rawAvatarUrl)
        val cacheFile = avatarCacheFile(appContext)
        val prefs = appContext.getSharedPreferences(PREFS_VISUAL, Context.MODE_PRIVATE)

        if (normalized.isBlank() || normalized.startsWith("preset:", ignoreCase = true)) {
            if (cacheFile.exists()) {
                runCatching { cacheFile.delete() }
            }
            prefs.edit().remove(KEY_CACHED_AVATAR_URL).apply()
            return
        }

        val previousUrl = prefs.getString(KEY_CACHED_AVATAR_URL, null)
        if (previousUrl == normalized && cacheFile.exists() && cacheFile.length() > 0L) {
            return
        }

        val decoded = decodeAvatarBitmap(
            context = appContext,
            normalizedUrl = normalized,
            allowNetworkFetch = true
        ) ?: return

        val circular = toCrispCircularBitmap(decoded, LARGE_ICON_SIZE_PX)
        runCatching {
            val tempFile = File(appContext.filesDir, "$AVATAR_CACHE_FILENAME.tmp")
            FileOutputStream(tempFile).use { out ->
                circular.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            if (cacheFile.exists()) cacheFile.delete()
            tempFile.renameTo(cacheFile)
            prefs.edit().putString(KEY_CACHED_AVATAR_URL, normalized).apply()
        }.onFailure { e ->
            Log.w(TAG, "Unable to cache notification profile avatar", e)
        }
    }

    /**
     * Returns a sharp `192x192` notification large-icon bitmap:
     * - User's circular high-resolution profile image when available
     * - Otherwise the high-resolution Amar Hisab brand logo (`R.drawable.img_amar_hisab_logo`)
     */
    fun resolveNotificationLargeIcon(
        context: Context,
        rawAvatarUrl: String?,
        allowNetworkFetch: Boolean = false
    ): Bitmap? {
        val appContext = context.applicationContext
        val normalized = ProfilePhotoUrlNormalizer.toHighResUrl(rawAvatarUrl)

        if (normalized.isNotBlank() && !normalized.startsWith("preset:", ignoreCase = true)) {
            // 1. Try persistent pre-rendered circular avatar cache first (zero network latency)
            val cacheFile = avatarCacheFile(appContext)
            val prefs = appContext.getSharedPreferences(PREFS_VISUAL, Context.MODE_PRIVATE)
            val cachedUrl = prefs.getString(KEY_CACHED_AVATAR_URL, null)
            if (cachedUrl == normalized && cacheFile.exists() && cacheFile.length() > 0L) {
                val cached = runCatching {
                    BitmapFactory.decodeFile(cacheFile.absolutePath)
                }.getOrNull()
                if (cached != null && !cached.isRecycled && cached.width > 0 && cached.height > 0) {
                    return cached
                }
            }

            // 2. Decode local file/content URI directly, or fetch remote photo on background thread
            val isMainThread = runCatching { Looper.myLooper() == Looper.getMainLooper() }.getOrDefault(false)
            val canFetchNetwork = allowNetworkFetch && !isMainThread
            val liveDecoded = decodeAvatarBitmap(
                context = appContext,
                normalizedUrl = normalized,
                allowNetworkFetch = canFetchNetwork
            )
            if (liveDecoded != null) {
                val circular = toCrispCircularBitmap(liveDecoded, LARGE_ICON_SIZE_PX)
                if (!isMainThread) {
                    runCatching {
                        FileOutputStream(cacheFile).use { out ->
                            circular.compress(Bitmap.CompressFormat.PNG, 100, out)
                        }
                        prefs.edit().putString(KEY_CACHED_AVATAR_URL, normalized).apply()
                    }
                }
                return circular
            }
        }

        return loadCrispAppLogoBitmap(appContext)
    }

    /**
     * Loads and caches the lossless `1024x1024` Amar Hisab brand logo scaled crisply to `192x192`
     * with rounded squircle framing matching the in-app `AmarHisabLogoBadge`.
     */
    fun loadCrispAppLogoBitmap(context: Context): Bitmap? {
        cachedLogoBitmap?.takeIf { !it.isRecycled && it.width == LARGE_ICON_SIZE_PX }?.let { return it }

        val raw = runCatching {
            val opts = BitmapFactory.Options().apply {
                inScaled = false
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            BitmapFactory.decodeResource(context.resources, R.drawable.img_amar_hisab_logo, opts)
        }.getOrNull() ?: return null

        val rendered = toCrispLogoBadgeBitmap(raw, LARGE_ICON_SIZE_PX)
        if (raw !== rendered && !raw.isRecycled) {
            raw.recycle()
        }
        cachedLogoBitmap = rendered
        return rendered
    }

    fun clearCacheForTesting(context: Context) {
        cachedLogoBitmap = null
        val appContext = context.applicationContext
        runCatching { avatarCacheFile(appContext).delete() }
        appContext.getSharedPreferences(PREFS_VISUAL, Context.MODE_PRIVATE).edit().clear().apply()
    }

    private fun avatarCacheFile(context: Context): File =
        File(context.filesDir, AVATAR_CACHE_FILENAME)

    private fun decodeAvatarBitmap(
        context: Context,
        normalizedUrl: String,
        allowNetworkFetch: Boolean
    ): Bitmap? {
        return runCatching {
            when {
                normalizedUrl.startsWith("file://", ignoreCase = true) -> {
                    val path = Uri.parse(normalizedUrl).path
                    if (!path.isNullOrBlank()) {
                        val f = File(path)
                        if (f.exists()) BitmapFactory.decodeFile(f.absolutePath) else null
                    } else {
                        null
                    }
                }
                normalizedUrl.startsWith("content://", ignoreCase = true) -> {
                    val uri = Uri.parse(normalizedUrl)
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        BitmapFactory.decodeStream(stream)
                    }
                }
                normalizedUrl.startsWith("/") -> {
                    val f = File(normalizedUrl)
                    if (f.exists()) BitmapFactory.decodeFile(f.absolutePath) else null
                }
                (normalizedUrl.startsWith("https://", ignoreCase = true) ||
                    normalizedUrl.startsWith("http://", ignoreCase = true)) && allowNetworkFetch -> {
                    val conn = (URL(normalizedUrl).openConnection() as HttpURLConnection).apply {
                        connectTimeout = 2500
                        readTimeout = 2500
                        instanceFollowRedirects = true
                        doInput = true
                    }
                    try {
                        if (conn.responseCode in 200..299) {
                            conn.inputStream.use { stream ->
                                BitmapFactory.decodeStream(stream)
                            }
                        } else {
                            null
                        }
                    } finally {
                        conn.disconnect()
                    }
                }
                else -> null
            }
        }.onFailure { e ->
            Log.w(TAG, "Unable to decode notification avatar from $normalizedUrl", e)
        }.getOrNull()
    }

    private fun toCrispCircularBitmap(source: Bitmap, targetPx: Int): Bitmap {
        val side = min(source.width, source.height).coerceAtLeast(1)
        val left = (source.width - side) / 2
        val top = (source.height - side) / 2
        val srcRect = Rect(left, top, left + side, top + side)
        val destRect = RectF(0f, 0f, targetPx.toFloat(), targetPx.toFloat())
        val radius = targetPx / 2f

        val output = Bitmap.createBitmap(targetPx, targetPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val clipPath = android.graphics.Path().apply {
            addCircle(radius, radius, radius, android.graphics.Path.Direction.CW)
        }
        canvas.save()
        canvas.clipPath(clipPath)
        val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(source, srcRect, destRect, bitmapPaint)
        canvas.restore()

        // Subtle crisp inner highlight rim so avatars pop cleanly on light & dark notification shades
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 3f
            color = 0x33FFFFFF
        }
        canvas.drawCircle(radius, radius, radius - 1.5f, borderPaint)
        return output
    }

    private fun toCrispLogoBadgeBitmap(source: Bitmap, targetPx: Int): Bitmap {
        // Trim outer ~7% margin so the banknote & coin emblem fills the notification badge boldly
        val trim = (min(source.width, source.height) * 0.07f).toInt().coerceAtLeast(0)
        val srcRect = Rect(
            trim,
            trim,
            (source.width - trim).coerceAtLeast(trim + 1),
            (source.height - trim).coerceAtLeast(trim + 1)
        )
        val output = Bitmap.createBitmap(targetPx, targetPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val destRect = RectF(0f, 0f, targetPx.toFloat(), targetPx.toFloat())
        val cornerRadius = targetPx * 0.25f

        val clipPath = android.graphics.Path().apply {
            addRoundRect(destRect, cornerRadius, cornerRadius, android.graphics.Path.Direction.CW)
        }
        canvas.save()
        canvas.clipPath(clipPath)
        val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(source, srcRect, destRect, bitmapPaint)
        canvas.restore()
        return output
    }
}
