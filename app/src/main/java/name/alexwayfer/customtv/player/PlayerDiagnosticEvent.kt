package name.alexwayfer.customtv.player

/** Only fixed names, origins, and bounded numeric metadata may enter a persistent problem report. */
internal fun isPlayerDiagnosticEvent(event: String): Boolean =
    event in PLAYER_DIAGNOSTIC_EVENTS || PLAYER_DIAGNOSTIC_PATTERNS.any { it.matches(event) }

private val PLAYER_DIAGNOSTIC_PATTERNS = listOf(
    Regex(
        "play-(?:request|already-playing|ad-skipped|button-before|button-after|button-failed|" +
            "native-before|native-after|native-resolved|native-rejected|native-failed)(?:-stale)?/" +
            "(?:${PlayerPlaySource.entries.joinToString("|") { it.diagnosticName }})/[1-9][0-9]{0,9}",
    ),
    Regex("error-(?:shown|reload-before|reload-after|reload-click-auto|reload-click-manual)/[0-9]{1,6}"),
    Regex(
        "quality-(?:request|fixed-before|fixed-after|auto-before|auto-after|" +
            "queued|confirmed|superseded|failed|deferred|timeout)" +
            "(?:-stale)?/(?:${PlayerQualityMode.entries.joinToString("|") { it.script }}|audio-only|video-return)/" +
            "[1-9][0-9]{0,9}",
    ),
)

private val PLAYER_DIAGNOSTIC_EVENTS = setOf(
    "bound", "pause-request", "play-request", "restart-request", "restart-play", "error-reload-missing",
    "playing", "pause", "seeking", "seeked", "emptied", "ended", "error",
    "timeline-reset-before", "timeline-reset-after", "buffer-split-before", "buffer-split-after",
    "buffer-joined-before", "buffer-joined-after", "quality-before", "quality-after",
)
