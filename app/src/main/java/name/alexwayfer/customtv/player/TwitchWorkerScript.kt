package name.alexwayfer.customtv.player

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/** Recognizes the public player worker, without intercepting media, credentials, or other Twitch scripts. */
internal fun isTwitchWorkerScript(url: String): Boolean {
    val parsed = url.toHttpUrlOrNull() ?: return false
    return parsed.isHttps && parsed.host == "assets.twitch.tv" && parsed.port == HTTPS_PORT &&
        WORKER_PATH.matches(parsed.encodedPath)
}

/** Replaces only the recognized sink-level removal function; an unknown SDK is left untouched. */
internal fun patchTwitchWorkerScript(source: String, cleanup: String): String? {
    val removal = WORKER_REMOVE.findAll(source).take(2).toList().singleOrNull() ?: return null
    return source.replaceRange(removal.range, cleanup.trim())
}

private const val HTTPS_PORT = 443
private val WORKER_PATH = Regex("""/assets/amazon-ivs-wasmworker\.min-[a-f0-9]+\.js""")

// Match the complete sink operation, including all references, before replacing external SDK code.
// This layer knows the MediaSource duration and owns every per-track operation queue.
private val WORKER_REMOVE = Regex(
    """function\((?<start>[$\w]+),(?<end>[$\w]+)\)\{for\(var (?<buffers>[$\w]+)=this\.sourceBuffers,""" +
        """(?<index>[$\w]+)=0,(?<keys>[$\w]+)=Object\.keys\(\k<buffers>\);""" +
        """\k<index><\k<keys>\.length;\k<index>\+\+\)""" +
        """\k<buffers>\[\k<keys>\[\k<index>\]\]\.remove\(\k<start>,\k<end>\)\}""",
)
