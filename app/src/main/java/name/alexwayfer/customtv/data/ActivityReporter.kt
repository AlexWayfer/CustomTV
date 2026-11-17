package name.alexwayfer.customtv.data

import name.alexwayfer.customtv.diagnostics.AppLog
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.security.GeneralSecurityException
import java.time.Instant
import java.util.Base64
import java.util.UUID
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

/** One serialized attempt. Cancellation preserves pending enrollment and never becomes an unsigned retry. */
internal class ActivityReporter(private val ports: ActivityReportPorts) {
    private var identity = ports.storage.read()

    suspend fun submit(version: String, os: String, edition: String, now: Instant, attest: Boolean) {
        var report = ActivityReport(identity.installationId, version, os, edition)
        if (!activityReportDue(now, report, identity.receipts[edition])) return
        recoverLostKeys()
        report = report.copy(installationId = identity.installationId)
        if (recoverEnrollment(report, now)) return
        recoverLostKeys()
        report = report.copy(installationId = identity.installationId)
        val renewalDue = identity.key?.let { it.renewAt <= now.epochSecond } ?: true
        if (attest && renewalDue && now.epochSecond >= identity.enrollAfter) enroll(now)
        val answer = send(report, identity.key)
        if (!accepted(answer, report)) {
            if (answer.code == HTTP_FORBIDDEN && answer.error == "invalid_attestation") {
                save(identity.copy(key = identity.key?.copy(renewAt = 0)))
            }
            throw ActivityRequestFailed(answer)
        }
        receipt(report, now)
    }

    private fun recoverLostKeys() {
        val pending = identity.pending
        val key = identity.key
        val lost = if (pending != null) !ports.keys.exists(pending) else key != null && !ports.keys.exists(key)
        if (lost) save(ActivityIdentity(UUID.randomUUID().toString()))
    }

    private suspend fun recoverEnrollment(report: ActivityReport, now: Instant): Boolean {
        val pending = identity.pending ?: return false
        val answer = send(report, pending)
        if (accepted(answer, report)) {
            AppLog.i(ACTIVITY_STATISTICS_TAG, "key registration recovered from accepted report")
            promote(pending)
            receipt(report, now)
            return true
        }
        when (answer.error) {
            "unknown_activity_key", "invalid_signature" -> {
                val current = identity.key.takeUnless { answer.error == "unknown_activity_key" }
                save(identity.copy(key = current, pending = null))
                ports.keys.delete(pending)
            }
            "invalid_attestation" -> {
                // The signature was accepted before attestation failed: the server has this pending key.
                promote(pending.copy(renewAt = 0))
            }
            else -> throw ActivityRequestFailed(answer)
        }
        return false
    }

    private suspend fun enroll(now: Instant) {
        val challenge = try {
            challenge()
        } catch (error: ActivityRequestFailed) {
            if (identity.key != null) throw error
            save(identity.copy(enrollAfter = now.epochSecond + ENROLL_RETRY.inWholeSeconds))
            return
        }
        val key = try {
            ports.keys.create(decodeActivityChallenge(challenge))
        } catch (_: GeneralSecurityException) {
            save(identity.copy(enrollAfter = now.epochSecond + UNSUPPORTED_RETRY.inWholeSeconds))
            return
        }
        try {
            save(identity.copy(pending = key))
        } catch (error: IOException) {
            ports.keys.delete(key)
            throw error
        }
        val message = activityEnrollmentMessage(identity.installationId, challenge, key.keyId)
        val body = JSONObject().put("installation_id", identity.installationId).put("challenge", challenge)
            .put("certificate_chain", JSONArray(key.certificates)).put("signature", ports.keys.sign(key, message))
        identity.key?.let { body.put("previous_signature", ports.keys.sign(it, message)) }
        val answer = ports.transport.post("activity/attest", body)
        if (answer.code in HTTP_SUCCESS && answer.body.optString("status") == "ok" &&
            answer.body.optString("key_id") == key.keyId
        ) {
            AppLog.i(ACTIVITY_STATISTICS_TAG, "key registration accepted, rotation=${identity.key != null}")
            promote(key)
        } else {
            enrollmentRefused(answer, key, now)
        }
    }

    private fun enrollmentRefused(answer: ActivityApiAnswer, key: ActivitySigningKey, now: Instant) {
        val definite = answer.code in HTTP_CLIENT_ERROR || answer.error == "attestation_unavailable"
        if (!definite) throw ActivityRequestFailed(answer)
        val wait = if (answer.code == HTTP_FORBIDDEN) UNSUPPORTED_RETRY else ENROLL_RETRY
        save(identity.copy(pending = null, enrollAfter = now.epochSecond + wait.inWholeSeconds))
        ports.keys.delete(key)
    }

    private suspend fun send(report: ActivityReport, key: ActivitySigningKey?): ActivityApiAnswer {
        val proof = key?.let {
            val challenge = challenge()
            JSONObject().put("challenge", challenge)
                .put("signature", ports.keys.sign(it, report.signingMessage(challenge)))
        }
        val answer = ports.transport.post("activity", report.json(proof))
        if (accepted(answer, report)) {
            AppLog.i(ACTIVITY_STATISTICS_TAG,
                "report accepted: edition=${report.edition}, app=${report.appVersion}, " +
                    "signed=${key != null}, verified=${answer.body.getBoolean("verified")}")
        }
        return answer
    }

    private suspend fun challenge(): String {
        val answer = ports.transport.post("activity/challenge",
            JSONObject().put("installation_id", identity.installationId))
        if (answer.code !in HTTP_SUCCESS) throw ActivityRequestFailed(answer)
        val text = answer.body.optString("challenge")
        decodeActivityChallenge(text)
        return text
    }

    private fun promote(key: ActivitySigningKey) {
        val old = identity.key
        save(identity.copy(key = key, pending = null, enrollAfter = 0))
        if (old != null && old.alias != key.alias) ports.keys.delete(old)
    }

    private fun receipt(report: ActivityReport, now: Instant) {
        save(identity.copy(receipts = identity.receipts + (report.edition to activityReceipt(now, report))))
    }

    private fun save(next: ActivityIdentity) {
        ports.storage.write(next)
        identity = next
    }

    private fun accepted(answer: ActivityApiAnswer, report: ActivityReport): Boolean =
        answer.accepted && answer.body.optString("edition") == report.edition

    private companion object {
        val ENROLL_RETRY = 1.hours
        val UNSUPPORTED_RETRY = 1.days
    }
}

private const val CHALLENGE_BYTES = 32

internal fun decodeActivityChallenge(text: String): ByteArray {
    val bytes = try {
        Base64.getUrlDecoder().decode(text)
    } catch (error: IllegalArgumentException) {
        throw IOException("Invalid activity challenge", error)
    }
    if (bytes.size != CHALLENGE_BYTES) throw IOException("Invalid activity challenge length")
    return bytes
}
