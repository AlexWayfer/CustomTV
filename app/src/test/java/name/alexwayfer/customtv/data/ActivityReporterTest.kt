package name.alexwayfer.customtv.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.security.GeneralSecurityException
import java.time.Instant
import java.util.Base64

class ActivityReporterTest {
    private val now = Instant.parse("2026-11-10T10:00:00Z")

    @Test
    fun enrollsThenSignsAndDoesNotReportTwice() = runBlocking {
        val fixture = Fixture()
        fixture.submit(now)
        assertEquals(listOf("activity/challenge", "activity/attest", "activity/challenge", "activity"),
            fixture.requests.map { it.first })
        assertNotNull(fixture.identity.key)
        assertNull(fixture.identity.pending)
        assertTrue(fixture.requests.last().second.has("proof"))
        fixture.submit(now)
        assertEquals(4, fixture.requests.size)
    }

    @Test
    fun unsupportedAttestationFallsBackAndBacksOffEnrollment() = runBlocking {
        val fixture = Fixture()
        fixture.unsupported = true
        fixture.submit(now)
        assertFalse(fixture.requests.last().second.has("proof"))
        assertNull(fixture.identity.key)
        assertTrue(fixture.identity.enrollAfter > now.epochSecond)
        fixture.submit(now.plusSeconds(86400))
        assertEquals(2, fixture.requests.count { it.first == "activity" })
    }

    @Test
    fun lostEnrollmentResponseRecoversPendingKeyWithoutNewIdentity() = runBlocking {
        val fixture = Fixture()
        fixture.loseEnrollmentAnswer = true
        expectIoFailure { fixture.submit(now) }
        val pending = fixture.identity.pending
        assertNotNull(pending)
        fixture.loseEnrollmentAnswer = false
        fixture.submit(now)
        assertEquals(pending, fixture.identity.key)
        assertEquals(Fixture.INSTALLATION_ID, fixture.identity.installationId)
        assertEquals(1, fixture.requests.count { it.first == "activity/attest" })
    }

    @Test
    fun registeredKeyNeverFallsBackToUnsignedAfterRefusal() = runBlocking {
        val fixture = Fixture()
        fixture.submit(now)
        fixture.refuseReports = true
        expectIoFailure { fixture.submit(now.plusSeconds(86400)) }
        assertTrue(fixture.requests.filter { it.first == "activity" }.all { it.second.has("proof") })
        assertEquals("2026-11-10", fixture.identity.receipts.getValue("free").day)
    }

    @Test
    fun failedUnsignedReportDoesNotSaveReceipt() = runBlocking {
        val fixture = Fixture()
        fixture.refuseReports = true
        expectIoFailure { fixture.submit(now, attest = false) }
        assertTrue(fixture.identity.receipts.isEmpty())
        fixture.refuseReports = false
        fixture.submit(now, attest = false)
        assertEquals(2, fixture.requests.size)
    }

    @Test
    fun cancellationDoesNotTurnIntoUnsignedFallback() {
        val fixture = Fixture()
        fixture.cancelEnrollment = true
        assertThrows(CancellationException::class.java) { runBlocking { fixture.submit(now) } }
        assertTrue(fixture.requests.none { it.first == "activity" })
        assertNotNull(fixture.identity.pending)
    }

    @Test
    fun renewalKeepsIdentityAndProvesPossessionOfPreviousKey() = runBlocking {
        val fixture = Fixture()
        fixture.submit(now)
        val old = fixture.identity.key
        fixture.identity = fixture.identity.copy(key = old!!.copy(renewAt = now.epochSecond))
        fixture.submit(now.plusSeconds(86400))
        val enroll = fixture.requests.last { it.first == "activity/attest" }.second
        assertTrue(enroll.has("previous_signature"))
        assertEquals(Fixture.INSTALLATION_ID, fixture.identity.installationId)
        assertTrue(old.alias in fixture.deletedKeys)
        assertFalse(old.alias == fixture.identity.key!!.alias)
    }

    @Test
    fun editionsKeepSeparateReceiptsForTheSameInstallation() = runBlocking {
        val fixture = Fixture()
        fixture.submit(now)
        fixture.submit(now, edition = "premium")
        fixture.submit(now)
        assertEquals(setOf("free", "premium"), fixture.identity.receipts.keys)
        assertEquals(2, fixture.requests.count { it.first == "activity" })
        assertEquals(Fixture.INSTALLATION_ID, fixture.identity.installationId)
        assertEquals("premium", fixture.requests.last().second.getString("edition"))
        assertEquals(setOf("challenge", "signature"),
            fixture.requests.last().second.getJSONObject("proof").keys().asSequence().toSet())
    }

    @Test
    fun failedPendingSaveDeletesNewKeyWithoutSendingEnrollment() = runBlocking {
        val fixture = Fixture()
        fixture.failPendingSave = true
        expectIoFailure { fixture.submit(now) }
        assertEquals(listOf("key1"), fixture.deletedKeys)
        assertTrue(fixture.requests.none { it.first == "activity/attest" })
        assertNull(fixture.identity.pending)
    }

    private suspend fun expectIoFailure(block: suspend () -> Unit) {
        try {
            block()
            error("Expected I/O failure")
        } catch (_: IOException) {
            // The next actual use retries without marking this attempt as reported.
        }
    }

    private class Fixture : ActivityIdentityStorage, ActivitySigningKeys, ActivityTransport {
        var identity = ActivityIdentity(INSTALLATION_ID)
        val requests = mutableListOf<Pair<String, JSONObject>>()
        val deletedKeys = mutableListOf<String>()
        var unsupported = false
        var loseEnrollmentAnswer = false
        var refuseReports = false
        var cancelEnrollment = false
        var failPendingSave = false
        private var createdKeys = 0

        suspend fun submit(now: Instant, attest: Boolean = true, edition: String = "free") {
            ActivityReporter(ActivityReportPorts(this, this, this)).submit("1.16.1", "Android 16", edition, now, attest)
        }

        override fun read(): ActivityIdentity = identity
        override fun write(identity: ActivityIdentity) {
            if (failPendingSave && identity.pending != null) throw IOException("Storage full")
            this.identity = identity
        }
        override fun exists(key: ActivitySigningKey): Boolean = key.alias !in deletedKeys
        override fun delete(key: ActivitySigningKey) { deletedKeys += key.alias }
        override fun sign(key: ActivitySigningKey, message: String): String = "${key.alias}:$message"

        override fun create(challenge: ByteArray): ActivitySigningKey {
            assertEquals(32, challenge.size)
            if (unsupported) throw GeneralSecurityException("Unsupported")
            createdKeys += 1
            return ActivitySigningKey("key$createdKeys", "digest$createdKeys", listOf("certificate"), Long.MAX_VALUE)
        }

        override suspend fun post(endpoint: String, body: JSONObject): ActivityApiAnswer {
            requests += endpoint to body
            return when (endpoint) {
                "activity/challenge" -> ActivityApiAnswer(200, JSONObject()
                    .put("challenge", Base64.getUrlEncoder().withoutPadding().encodeToString(ByteArray(32))))
                "activity/attest" -> {
                    // The pending key must reach disk before a request that may commit it on the server.
                    assertNotNull(identity.pending)
                    if (cancelEnrollment) throw CancellationException("Closed")
                    if (loseEnrollmentAnswer) throw IOException("Response lost")
                    ActivityApiAnswer(200, JSONObject().put("status", "ok").put("key_id", identity.pending!!.keyId))
                }
                "activity" -> if (refuseReports) {
                    ActivityApiAnswer(403, JSONObject().put("error", "invalid_attestation"))
                } else {
                    ActivityApiAnswer(200, JSONObject().put("status", "ok").put("verified", body.has("proof"))
                        .put("edition", body.getString("edition")))
                }
                else -> error("Unexpected endpoint $endpoint")
            }
        }

        companion object {
            const val INSTALLATION_ID = "01234567-89ab-4cde-8012-3456789abcde"
        }
    }
}
