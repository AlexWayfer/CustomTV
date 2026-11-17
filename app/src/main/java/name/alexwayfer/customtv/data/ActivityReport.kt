package name.alexwayfer.customtv.data

import org.json.JSONException
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneOffset

private const val ACTIVITY_HTTP_OK = 200
private const val MAX_VERSION_BYTES = 32

/** Both editions report use without accounts, channels, viewing history, or hardware identifiers in the body. */
internal data class ActivityReport(
    val installationId: String,
    val appVersion: String,
    val osVersion: String,
    val edition: String = "free",
) {
    fun json(proof: JSONObject? = null): JSONObject = JSONObject()
        .put("installation_id", installationId)
        .put("edition", edition)
        .put("app_version", appVersion)
        .put("os_version", osVersion)
        .apply { if (proof != null) put("proof", proof) }

    fun signingMessage(challenge: String): String =
        listOf("mecena-activity", "customtv", installationId, edition, appVersion, osVersion, challenge)
            .joinToString("\n")
}

internal data class ActivityReceipt(val day: String, val appVersion: String, val osVersion: String)

internal fun activityReceipt(now: Instant, report: ActivityReport): ActivityReceipt =
    ActivityReceipt(now.atOffset(ZoneOffset.UTC).toLocalDate().toString(), report.appVersion, report.osVersion)

internal fun activityReportDue(now: Instant, report: ActivityReport, receipt: ActivityReceipt?): Boolean =
    activityReceipt(now, report) != receipt

internal fun activityEnrollmentMessage(id: String, challenge: String, keyId: String): String =
    listOf("mecena-activity-attest", "customtv", id, challenge, keyId).joinToString("\n")

internal fun activityReportAccepted(code: Int, body: String): Boolean = try {
    val json = JSONObject(body)
    code == ACTIVITY_HTTP_OK && json.optString("status") == "ok" && json.opt("verified") is Boolean
} catch (_: JSONException) {
    false
}

internal fun activityOsVersion(release: String, sdk: Int): String {
    val label = "Android ${release.trim()}"
    return if (release.isNotBlank() && label.toByteArray(Charsets.UTF_8).size <= MAX_VERSION_BYTES &&
        label.none(Char::isISOControl)
    ) label else "Android API $sdk"
}
