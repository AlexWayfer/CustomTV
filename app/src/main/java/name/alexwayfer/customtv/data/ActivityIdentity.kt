package name.alexwayfer.customtv.data

import org.json.JSONObject
import java.io.IOException

internal data class ActivitySigningKey(
    val alias: String,
    val keyId: String,
    val certificates: List<String>,
    val renewAt: Long,
)

/** The pending key is saved before enrollment, so a lost success response can be recovered. */
internal data class ActivityIdentity(
    val installationId: String,
    val key: ActivitySigningKey? = null,
    val pending: ActivitySigningKey? = null,
    val receipts: Map<String, ActivityReceipt> = emptyMap(),
    val enrollAfter: Long = 0,
)

internal interface ActivityIdentityStorage {
    fun read(): ActivityIdentity
    fun write(identity: ActivityIdentity)
}

internal interface ActivitySigningKeys {
    fun create(challenge: ByteArray): ActivitySigningKey
    fun exists(key: ActivitySigningKey): Boolean
    fun sign(key: ActivitySigningKey, message: String): String
    fun delete(key: ActivitySigningKey)
}

internal data class ActivityApiAnswer(val code: Int, val body: JSONObject) {
    val accepted: Boolean get() = activityReportAccepted(code, body.toString())
    val error: String get() = body.optString("error")
}

internal fun interface ActivityTransport {
    suspend fun post(endpoint: String, body: JSONObject): ActivityApiAnswer
}

internal data class ActivityReportPorts(
    val storage: ActivityIdentityStorage,
    val keys: ActivitySigningKeys,
    val transport: ActivityTransport,
)

internal class ActivityRequestFailed(val answer: ActivityApiAnswer) : IOException(
    "Activity request refused HTTP ${answer.code}: ${answer.error}",
)
