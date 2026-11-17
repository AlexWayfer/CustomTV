package name.alexwayfer.customtv.data

import android.content.Context
import android.util.AtomicFile
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.util.UUID

/** Installation state is shared across edition upgrades but excluded from both backup and device transfer. */
internal class ActivityIdentityFile(context: Context) : ActivityIdentityStorage {
    private val file = AtomicFile(File(context.noBackupFilesDir, "activity-statistics.json"))

    override fun read(): ActivityIdentity = try {
        // openRead also restores AtomicFile's backup after an interrupted write.
        readIdentity(file.openRead().bufferedReader().use { it.readText() })
    } catch (error: FileNotFoundException) {
        if (file.baseFile.exists()) throw error
        ActivityIdentity(UUID.randomUUID().toString()).also(::write)
    }

    override fun write(identity: ActivityIdentity) {
        val receipts = JSONObject()
        identity.receipts.forEach { (edition, receipt) ->
            receipts.put(edition, JSONObject().put("day", receipt.day)
                .put("app_version", receipt.appVersion).put("os_version", receipt.osVersion))
        }
        val json = JSONObject().put("installation_id", identity.installationId)
            .put("key", identity.key?.let(::writeKey))
            .put("pending", identity.pending?.let(::writeKey))
            .put("receipts", receipts).put("enroll_after", identity.enrollAfter)
        val output = file.startWrite()
        try {
            output.write(json.toString().toByteArray(Charsets.UTF_8))
            file.finishWrite(output)
        } catch (error: IOException) {
            file.failWrite(output)
            throw error
        }
    }
}

private const val UUID_VERSION = 4

private fun readIdentity(text: String): ActivityIdentity = try {
    val json = JSONObject(text)
    val id = json.getString("installation_id")
    require(UUID.fromString(id).version() == UUID_VERSION)
    ActivityIdentity(id, json.optJSONObject("key")?.let(::readKey), json.optJSONObject("pending")?.let(::readKey),
        readReceipts(json.optJSONObject("receipts") ?: JSONObject()), json.optLong("enroll_after"))
} catch (error: JSONException) {
    throw IOException("Unreadable activity identity", error)
} catch (error: IllegalArgumentException) {
    throw IOException("Invalid activity identity", error)
}

private fun readKey(json: JSONObject): ActivitySigningKey {
    val chain = json.getJSONArray("certificates")
    return ActivitySigningKey(json.getString("alias"), json.getString("key_id"),
        List(chain.length()) { chain.getString(it) }, json.getLong("renew_at"))
}

private fun writeKey(key: ActivitySigningKey): JSONObject = JSONObject().put("alias", key.alias)
    .put("key_id", key.keyId).put("certificates", JSONArray(key.certificates)).put("renew_at", key.renewAt)

private fun readReceipts(json: JSONObject): Map<String, ActivityReceipt> = json.keys().asSequence().associateWith {
    val receipt = json.getJSONObject(it)
    ActivityReceipt(receipt.getString("day"), receipt.getString("app_version"), receipt.getString("os_version"))
}
