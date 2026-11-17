package name.alexwayfer.customtv.data

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

/** The protocol and UTC accounting are shared by both editions. */
class ActivityReportTest {
    private val report = ActivityReport(
        "01234567-89ab-4cde-8012-3456789abcde", "1.16.1", "Android 16",
    )

    @Test
    fun payloadContainsOnlyInstallationAndVersions() {
        val body = report.json()
        assertEquals(setOf("installation_id", "edition", "app_version", "os_version"), body.keys().asSequence().toSet())
        assertEquals("free", body.getString("edition"))
        assertEquals(report.installationId, body.getString("installation_id"))
        assertEquals("1.16.1", body.getString("app_version"))
        assertEquals("Android 16", body.getString("os_version"))
    }

    @Test
    fun signaturesUseExactProtocolMessagesWithoutTrailingNewline() {
        assertEquals(
            "mecena-activity\ncustomtv\n${report.installationId}\nfree\n1.16.1\nAndroid 16\nnonce",
            report.signingMessage("nonce"),
        )
        assertEquals(
            "mecena-activity-attest\ncustomtv\n${report.installationId}\nnonce\nkey",
            activityEnrollmentMessage(report.installationId, "nonce", "key"),
        )
    }

    @Test
    fun editionIsBoundToTheSignedMessage() {
        assertEquals("premium", report.copy(edition = "premium").json().getString("edition"))
        assertFalse(report.signingMessage("nonce") == report.copy(edition = "premium").signingMessage("nonce"))
    }

    @Test
    fun proofIsSeparateFromReportedFields() {
        val proof = JSONObject().put("challenge", "nonce").put("signature", "signature")
        assertEquals(proof.toString(), report.json(proof).getJSONObject("proof").toString())
    }

    @Test
    fun onlyAcknowledgedReportsSuppressSameUtcDayAndVersions() {
        val now = Instant.parse("2026-11-10T23:59:59Z")
        val receipt = ActivityReceipt("2026-11-10", report.appVersion, report.osVersion)
        assertTrue(activityReportDue(now, report, null))
        assertFalse(activityReportDue(now, report, receipt))
        assertTrue(activityReportDue(now.plusSeconds(1), report, receipt))
        assertTrue(activityReportDue(now, report.copy(appVersion = "1.17.0"), receipt))
        assertTrue(activityReportDue(now, report.copy(osVersion = "Android 17"), receipt))
    }

    @Test
    fun successfulResponseRequiresAnExplicitAcknowledgement() {
        assertTrue(activityReportAccepted(200, "{\"status\":\"ok\",\"verified\":true}"))
        assertTrue(activityReportAccepted(200, "{\"status\":\"ok\",\"verified\":false}"))
        assertFalse(activityReportAccepted(503, "{\"status\":\"ok\"}"))
        assertFalse(activityReportAccepted(200, "{}"))
        assertFalse(activityReportAccepted(200, "<html>unavailable</html>"))
    }

    @Test
    fun unusualAndroidReleaseLabelsStillProduceAValidVersion() {
        assertEquals("Android 16", activityOsVersion("16", 36))
        assertEquals("Android API 36", activityOsVersion("", 36))
        assertEquals("Android API 36", activityOsVersion("vendor\n16", 36))
        assertEquals("Android API 36", activityOsVersion("я".repeat(20), 36))
    }
}
