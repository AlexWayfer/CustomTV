package name.alexwayfer.customtv.diagnostics

import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

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
            val first = diagnosticTime(event.firstAtMillis, zone)
            "${diagnosticTime(event.lastAtMillis, zone)}, ${event.count} times since $first"
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
