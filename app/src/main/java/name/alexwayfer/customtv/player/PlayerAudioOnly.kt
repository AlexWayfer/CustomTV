package name.alexwayfer.customtv.player

/**
 * Whether the stream plays sound only: in the background without a picture-in-picture window, or with the screen off
 * or locked, where that window hides, nobody sees the video, so downloading and decoding it only spends data and
 * battery. The setting [enabled] trades that for no pause when the screen locks or the app comes back.
 */
internal fun playerAudioOnly(
    enabled: Boolean,
    background: Boolean,
    inPictureInPicture: Boolean,
    screenUnlocked: Boolean,
): Boolean = enabled && (!screenUnlocked || background && !inPictureInPicture)

private const val USHER_HOST = "usher.ttvnw.net"
private const val AUDIO_ONLY_PARAM = "allow_audio_only"

/**
 * The usher master playlist [url] with the audio-only rendition allowed, or null when [url] is no master playlist
 * or already allows it. The web player asks without it, so its quality list never offers sound only.
 */
internal fun usherUrlWithAudioOnly(url: String): String? {
    val schemeEnd = url.indexOf("://")
    if (schemeEnd < 0) return null
    val hostStart = schemeEnd + 3
    val pathStart = url.indexOf('/', hostStart).takeIf { it >= 0 } ?: return null
    if (!url.substring(hostStart, pathStart).equals(USHER_HOST, ignoreCase = true)) return null
    val queryStart = url.indexOf('?', pathStart)
    val path = if (queryStart < 0) url.substring(pathStart) else url.substring(pathStart, queryStart)
    val masterPlaylist = (path.startsWith("/api/channel/hls/") || path.startsWith("/vod/")) && path.endsWith(".m3u8")
    if (!masterPlaylist) return null
    if (queryStart < 0) return "$url?$AUDIO_ONLY_PARAM=true"
    val params = url.substring(queryStart + 1).split('&')
    val existing = params.indexOfFirst { it.substringBefore('=') == AUDIO_ONLY_PARAM }
    if (existing < 0) return "$url&$AUDIO_ONLY_PARAM=true"
    if (params[existing] == "$AUDIO_ONLY_PARAM=true") return null
    val rewritten = params.toMutableList().apply { this[existing] = "$AUDIO_ONLY_PARAM=true" }
    return url.substring(0, queryStart + 1) + rewritten.joinToString("&")
}
