package name.alexwayfer.customtv.diagnostics

import name.alexwayfer.customtv.data.HTTP_CLIENT_ERROR
import name.alexwayfer.customtv.data.HTTP_TOO_MANY_REQUESTS
import name.alexwayfer.customtv.data.HTTP_UNAUTHORIZED
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

/** The journal keeps this many distinct problems; a repeat of one only raises its count. */
internal const val DIAGNOSTIC_JOURNAL_LIMIT = 20

/** This many HTTP 429 answers from one source within [RATE_LIMIT_WINDOW] is no longer routine. */
internal const val RATE_LIMIT_NOTICE_HITS = 3
internal val RATE_LIMIT_WINDOW: Duration = 1.hours

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
internal val HELIX_EXPECTED_CODES: Set<Int> = setOf(HTTP_UNAUTHORIZED)

/** What a 4xx answer means for the report: nothing when it is routine for that request. */
internal fun httpDiagnostic(httpCode: Int, expected: Set<Int>): DiagnosticKind? = when (httpCode) {
    !in HTTP_CLIENT_ERROR, in expected -> null
    HTTP_TOO_MANY_REQUESTS -> DiagnosticKind.RateLimited
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
