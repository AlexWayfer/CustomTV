package name.alexwayfer.customtv.diagnostics

import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

/** The journal keeps this many distinct problems; a repeat of one only raises its count. */
internal const val DIAGNOSTIC_JOURNAL_LIMIT = 20

/** This many HTTP 429 answers from one source within [RATE_LIMIT_WINDOW] is no longer routine. */
internal const val RATE_LIMIT_NOTICE_HITS = 3
internal val RATE_LIMIT_WINDOW: Duration = 1.hours

/** How many of the latest log lines go with each problem. */
internal const val RECENT_LOG_LINES = 20

/** The latest log lines, dropping the oldest past [RECENT_LOG_LINES]. */
internal class RecentLog {
    private val lines = ArrayDeque<String>()

    fun add(line: String) = synchronized(lines) {
        lines.addLast(line)
        while (lines.size > RECENT_LOG_LINES) lines.removeFirst()
    }

    fun lines(): List<String> = synchronized(lines) { lines.toList() }
}

private val logTimeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")

internal fun recentLogLine(atMillis: Long, zone: ZoneId, level: Char, tag: String, message: String): String =
    "${Instant.ofEpochMilli(atMillis).atZone(zone).format(logTimeFormat)} $level/$tag: $message"

internal enum class DiagnosticKind { Rejected, RateLimited, GqlError, Crash, Anr }

/**
 * One problem the developer should hear about. [source] is the log tag, [detail] the endpoint or
 * the exception and its top frame, [message] what the server said or the stack, and [recentLog]
 * the app's log lines just before the latest time it happened. Never a token.
 */
internal data class DiagnosticEvent(
    val kind: DiagnosticKind,
    val source: String,
    val detail: String,
    val httpCode: Int? = null,
    val message: String = "",
    val firstAtMillis: Long,
    val lastAtMillis: Long = firstAtMillis,
    val count: Int = 1,
    val recentLog: List<String> = emptyList(),
) {
    fun sameProblem(other: DiagnosticEvent): Boolean =
        kind == other.kind && source == other.source && detail == other.detail && httpCode == other.httpCode
}

/** Adds [event] to the journal: a repeat updates the earlier entry, and the oldest entries fall off. */
internal fun recordDiagnosticEvent(events: List<DiagnosticEvent>, event: DiagnosticEvent): List<DiagnosticEvent> {
    val earlier = events.firstOrNull { it.sameProblem(event) }
    val merged = earlier?.copy(
        message = event.message,
        lastAtMillis = event.lastAtMillis,
        count = earlier.count + 1,
        recentLog = event.recentLog,
    ) ?: event
    return (events.filterNot { it.sameProblem(event) } + merged)
        .sortedByDescending { it.lastAtMillis }
        .take(DIAGNOSTIC_JOURNAL_LIMIT)
}

/**
 * A notice names a problem once; its repeats only raise the count in the report, until a report goes
 * out at [sentAtMillis]. After that a repeat is news again.
 */
internal fun diagnosticNoticeDue(events: List<DiagnosticEvent>, event: DiagnosticEvent, sentAtMillis: Long): Boolean =
    events.none { it.sameProblem(event) && it.lastAtMillis > sentAtMillis }

/**
 * What the next report holds: the problems that happened since the last report at [sentAtMillis],
 * repeats of sent ones included. Android does not tell whether the report really went out, so with
 * nothing new it holds the whole journal again.
 */
internal fun diagnosticEventsToSend(events: List<DiagnosticEvent>, sentAtMillis: Long): List<DiagnosticEvent> =
    events.filter { it.lastAtMillis > sentAtMillis }.ifEmpty { events }

/** Codes a user token routinely gets: an expired login, fixed by logging in again. */
internal val HELIX_EXPECTED_CODES: Set<Int> = setOf(401)

/** What a 4xx answer means for the report: nothing when it is routine for that request. */
internal fun httpDiagnostic(httpCode: Int, expected: Set<Int>): DiagnosticKind? = when (httpCode) {
    !in 400..499, in expected -> null
    429 -> DiagnosticKind.RateLimited
    else -> DiagnosticKind.Rejected
}

internal data class RateLimitHits(val hits: List<Long>, val tooOften: Boolean)

/**
 * Counts a 429 from one source. Only the hit that brings the window to [RATE_LIMIT_NOTICE_HITS]
 * counts as too often, so a steady limit adds one entry per run, not one per check.
 */
internal fun rateLimitHit(previous: List<Long>, nowMillis: Long): RateLimitHits {
    val from = nowMillis - RATE_LIMIT_WINDOW.inWholeMilliseconds
    val hits = previous.filter { it > from } + nowMillis
    return RateLimitHits(hits, tooOften = hits.size == RATE_LIMIT_NOTICE_HITS)
}

/** Crashes and freezes are found on the next start; these are the ones no notice has named yet. */
internal fun unseenExitEvents(events: List<DiagnosticEvent>, noticedAtMillis: Long): List<DiagnosticEvent> =
    events.filter {
        (it.kind == DiagnosticKind.Crash || it.kind == DiagnosticKind.Anr) && it.lastAtMillis > noticedAtMillis
    }

/** Keeps the main thread of an ANR trace, or nothing without one; the other threads only make the report long. */
internal fun anrMainThread(trace: Sequence<String>): String =
    trace.dropWhile { !it.startsWith("\"main\"") }
        .takeWhile { it.isNotBlank() }
        .joinToString("\n")

/** The first frame of a thread dump, which names where the thread was stuck. */
internal fun topStackFrame(thread: String): String? =
    thread.lineSequence().map { it.trim() }.firstOrNull { it.startsWith("at ") }?.removePrefix("at ")

internal data class DiagnosticDevice(
    val appVersion: String,
    val premium: Boolean,
    val androidRelease: String,
    val sdk: Int,
    val manufacturer: String,
    val model: String,
    /** The system's languages in order, as language tags. */
    val languages: List<String>,
    /** The country of the SIM card, null without one. */
    val simCountry: String?,
    val debug: Boolean,
    /** When this APK was installed or last updated: tells which deploy of a debug build a report came from. */
    val installedAtMillis: Long?,
)

internal data class DiagnosticAccounts(
    val twitchLogin: String?,
    val twitchUserId: String?,
    val telegramUsername: String?,
)

internal fun diagnosticReport(
    device: DiagnosticDevice,
    accounts: DiagnosticAccounts,
    events: List<DiagnosticEvent>,
    zone: ZoneId,
    appLog: List<String> = emptyList(),
): String = buildString {
    appendLine("CustomTV problem report")
    appendLine("App: ${diagnosticApp(device)}")
    device.installedAtMillis?.let { appendLine("Installed: ${diagnosticTime(it, zone)}") }
    appendLine("Android: ${device.androidRelease} (API ${device.sdk}), ${device.manufacturer} ${device.model}")
    appendLine("Languages: ${device.languages.joinToString(", ").ifEmpty { "-" }}")
    appendLine("SIM country: ${device.simCountry?.takeIf { it.isNotBlank() }?.uppercase(Locale.ROOT) ?: "-"}")
    val twitch = listOfNotNull(
        accounts.twitchLogin?.takeIf { it.isNotBlank() },
        accounts.twitchUserId?.takeIf { it.isNotBlank() }?.let { "id $it" },
    )
    appendLine("Twitch: ${twitch.joinToString(", ").ifEmpty { "not logged in" }}")
    appendLine("Telegram: ${accounts.telegramUsername?.takeIf { it.isNotBlank() }?.let { "@$it" } ?: "-"}")
    events.forEach { event ->
        appendLine()
        appendLine(diagnosticHeadline(event))
        val times = if (event.count > 1) {
            "${diagnosticTime(event.lastAtMillis, zone)}, ${event.count} times since ${diagnosticTime(event.firstAtMillis, zone)}"
        } else {
            diagnosticTime(event.lastAtMillis, zone)
        }
        appendLine(times)
        if (event.message.isNotBlank()) appendLine(event.message.trimEnd())
        if (event.recentLog.isNotEmpty()) {
            appendLine("Log before it:")
            event.recentLog.forEach { appendLine("  $it") }
        }
    }
    if (appLog.isNotEmpty()) {
        appendLine()
        appendLine("App log:")
        appLog.forEach { appendLine("  $it") }
    }
}.trimEnd() + "\n"

private val fileTimeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")

/**
 * The report file's name with the time it was made, so reports saved on a computer sort by time instead of piling
 * up as copies. The time is in UTC, marked with Z, since the name cannot tell the device's zone. Dashes stand for
 * colons, which Windows does not allow in a file name.
 */
internal fun diagnosticReportFileName(millis: Long): String =
    "customtv-report_${Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).format(fileTimeFormat)}Z.txt"

/**
 * When the running code arrived: the package's install or update, or a later deploy from Android Studio, which
 * changes a debug build without reinstalling it. [studioOverlayMillis] is 0 without such a deploy.
 */
internal fun diagnosticInstalledAt(packageUpdatedMillis: Long?, studioOverlayMillis: Long): Long? =
    listOfNotNull(packageUpdatedMillis, studioOverlayMillis.takeIf { it > 0 }).maxOrNull()

/** The build: version, edition, and a mark on a debug build. */
internal fun diagnosticApp(device: DiagnosticDevice): String =
    "${device.appVersion} ${if (device.premium) "premium" else "free"}${if (device.debug) " debug" else ""}"

/**
 * The message the report file goes with: only the build and how many problems the file holds. The problems
 * themselves stay in the file, so the person who sends it does not read a list of exceptions.
 */
internal fun diagnosticShareText(device: DiagnosticDevice, problems: Int): String =
    "CustomTV ${diagnosticApp(device)}" + if (problems > 0) "\nProblems: $problems" else ""

/** One line naming the problem, used as the first line of an event and in the notice. */
internal fun diagnosticHeadline(event: DiagnosticEvent): String = when (event.kind) {
    DiagnosticKind.Rejected -> "${event.source}: HTTP ${event.httpCode} ${event.detail}"
    DiagnosticKind.RateLimited -> "${event.source}: HTTP 429 ${event.detail}"
    DiagnosticKind.GqlError -> "${event.source}: error in ${event.detail}"
    DiagnosticKind.Crash -> "Crash: ${event.detail}"
    DiagnosticKind.Anr -> "Freeze: ${event.detail}"
}

private val timeFormat: DateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME

internal fun diagnosticTime(millis: Long, zone: ZoneId): String =
    Instant.ofEpochMilli(millis).truncatedTo(ChronoUnit.SECONDS).atZone(zone).toOffsetDateTime().format(timeFormat)

internal fun encodeDiagnosticEvents(events: List<DiagnosticEvent>): String =
    JSONArray().apply {
        events.forEach { event ->
            put(
                JSONObject()
                    .put("kind", event.kind.name)
                    .put("source", event.source)
                    .put("detail", event.detail)
                    .put("httpCode", event.httpCode ?: JSONObject.NULL)
                    .put("message", event.message)
                    .put("firstAt", event.firstAtMillis)
                    .put("lastAt", event.lastAtMillis)
                    .put("count", event.count)
                    .put("recentLog", JSONArray(event.recentLog)),
            )
        }
    }.toString()

internal fun decodeDiagnosticEvents(text: String): List<DiagnosticEvent> {
    val array = try {
        JSONArray(text)
    } catch (_: Exception) {
        return emptyList()
    }
    return (0 until array.length()).mapNotNull { index ->
        val item = array.optJSONObject(index) ?: return@mapNotNull null
        val kind = DiagnosticKind.entries.firstOrNull { it.name == item.optString("kind") } ?: return@mapNotNull null
        val firstAt = item.optLong("firstAt")
        DiagnosticEvent(
            kind = kind,
            source = item.optString("source"),
            detail = item.optString("detail"),
            httpCode = if (item.isNull("httpCode")) null else item.optInt("httpCode"),
            message = item.optString("message"),
            firstAtMillis = firstAt,
            lastAtMillis = item.optLong("lastAt", firstAt),
            count = item.optInt("count", 1).coerceAtLeast(1),
            recentLog = item.optJSONArray("recentLog")?.let { lines ->
                (0 until lines.length()).map { lines.optString(it) }
            }.orEmpty(),
        )
    }
}
