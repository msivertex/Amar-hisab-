package com.example.core

import java.net.URI

/**
 * Normalizes and upgrades Google account profile picture URLs (`photoURL`) so circular avatars
 * render sharply across high-DPI screens (Profile Details, Settings, Home) instead of stretching
 * Google's default 96x96 thumbnail (`=s96-c`).
 *
 * Non-Google URLs (local `file://`, `content://`, `preset:`, or third-party hosts) are preserved
 * verbatim without modification.
 */
object ProfilePhotoUrlNormalizer {

    const val DEFAULT_HIGH_RES_PX: Int = 400

    private val trailingSizeRegex = Regex("=s(\\d+)(-[a-zA-Z0-9-]+)?$")
    private val trailingWidthHeightRegex = Regex("=w(\\d+)-h(\\d+)(-[a-zA-Z0-9-]+)?$")
    private val pathSizeSegmentRegex = Regex("/s(\\d+)(-c)?/")
    private val querySizeParamRegex = Regex("([?&])sz=(\\d+)")

    fun toHighResUrl(rawUrl: String?, minSizePx: Int = DEFAULT_HIGH_RES_PX): String {
        val trimmed = rawUrl?.trim().orEmpty()
        if (trimmed.isEmpty()) return ""
        if (
            trimmed.startsWith("preset:", ignoreCase = true) ||
            trimmed.startsWith("file:", ignoreCase = true) ||
            trimmed.startsWith("content:", ignoreCase = true) ||
            trimmed.startsWith("android.resource:", ignoreCase = true)
        ) {
            return trimmed
        }
        if (!isGoogleProfilePhotoUrl(trimmed)) return trimmed

        // 1. Standard trailing Google size token: `=s96-c`, `=s64`, `=s120-c-mo`, etc.
        trailingSizeRegex.find(trimmed)?.let { match ->
            val currentSize = match.groupValues[1].toIntOrNull() ?: 0
            if (currentSize >= minSizePx) return trimmed
            val suffix = match.groupValues[2].ifBlank { "-c" }
            val normalizedSuffix = if (suffix.contains("-c", ignoreCase = true)) suffix else "$suffix-c"
            return trimmed.replaceRange(match.range, "=s$minSizePx$normalizedSuffix")
        }

        // 2. Trailing width/height token: `=w96-h96-c`
        trailingWidthHeightRegex.find(trimmed)?.let { match ->
            val w = match.groupValues[1].toIntOrNull() ?: 0
            val h = match.groupValues[2].toIntOrNull() ?: 0
            if (minOf(w, h) >= minSizePx) return trimmed
            return trimmed.replaceRange(match.range, "=s$minSizePx-c")
        }

        // 3. Legacy path-based Google avatar size segment: `/s96-c/photo.jpg`
        pathSizeSegmentRegex.find(trimmed)?.let { match ->
            val currentSize = match.groupValues[1].toIntOrNull() ?: 0
            if (currentSize >= minSizePx) return trimmed
            return trimmed.replaceRange(match.range, "/s$minSizePx-c/")
        }

        // 4. Query parameter size: `?sz=96` or `&sz=96`
        querySizeParamRegex.find(trimmed)?.let { match ->
            val currentSize = match.groupValues[2].toIntOrNull() ?: 0
            if (currentSize >= minSizePx) return trimmed
            val delimiter = match.groupValues[1]
            return trimmed.replaceRange(match.range, "${delimiter}sz=$minSizePx")
        }

        // 5. Bare Google avatar URL without any size directive (e.g. `https://lh3.googleusercontent.com/a/ACg8oc...`)
        if (!trimmed.contains('?') && !trimmed.substringAfterLast('/').contains('=')) {
            return "$trimmed=s$minSizePx-c"
        }

        return trimmed
    }

    /**
     * Merges an incoming photo URL with an existing stored URL while preserving high-resolution
     * Google `photoURL` values and never wiping an existing valid URL with a blank string.
     */
    fun mergePreservingHighRes(
        incomingUrl: String?,
        existingUrl: String?,
        minSizePx: Int = DEFAULT_HIGH_RES_PX
    ): String {
        val normalizedIncoming = toHighResUrl(incomingUrl, minSizePx)
        if (normalizedIncoming.isNotBlank()) return normalizedIncoming
        return toHighResUrl(existingUrl, minSizePx)
    }

    fun isGoogleProfilePhotoUrl(url: String): Boolean {
        if (!url.startsWith("http://", ignoreCase = true) && !url.startsWith("https://", ignoreCase = true)) {
            return false
        }
        val host = runCatching { URI(url).host.orEmpty().lowercase() }.getOrDefault("")
        if (host.isEmpty()) return false
        return host == "googleusercontent.com" ||
            host.endsWith(".googleusercontent.com") ||
            host == "ggpht.com" ||
            host.endsWith(".ggpht.com")
    }
}
