package name.alexwayfer.customtv.data

import android.content.Context
import name.alexwayfer.customtv.chat.EmoteUse
import name.alexwayfer.customtv.chat.decodeEmoteUsage
import name.alexwayfer.customtv.chat.emoteUsesAfterMessage
import name.alexwayfer.customtv.chat.encodeEmoteUsage
import name.alexwayfer.customtv.diagnostics.AppLog
import java.io.File

internal class EmoteUsageStore(context: Context) {
    private val file = File(context.applicationContext.filesDir, "emote-usage.json")

    fun uses(channelId: String): List<EmoteUse> {
        if (channelId.isBlank()) return emptyList()
        synchronized(gate) {
            return read()?.get(channelId).orEmpty()
        }
    }

    fun record(
        channelId: String,
        message: String,
        knownNames: Set<String>,
        usedAtMillis: Long,
    ): List<EmoteUse> {
        if (channelId.isBlank()) return emptyList()
        synchronized(gate) {
            val channels = read() ?: return emoteUsesAfterMessage(emptyList(), message, knownNames, usedAtMillis)
            val next = emoteUsesAfterMessage(channels[channelId].orEmpty(), message, knownNames, usedAtMillis)
            if (next == channels[channelId].orEmpty()) return next
            val updated = channels.toMutableMap()
            updated[channelId] = next
            if (!write(updated)) return next
            return next
        }
    }

    private fun read(): Map<String, List<EmoteUse>>? {
        if (!file.exists()) return emptyMap()
        val raw = runCatching { file.readText() }.getOrElse { failure ->
            AppLog.w(TAG, "emote usage read failed: ${failure.javaClass.simpleName}")
            return null
        }
        return decodeEmoteUsage(raw) ?: run {
            AppLog.w(TAG, "emote usage read failed: JSONException")
            null
        }
    }

    private fun write(channels: Map<String, List<EmoteUse>>): Boolean {
        return runCatching {
            file.parentFile?.mkdirs()
            file.writeText(encodeEmoteUsage(channels))
        }.onFailure { failure ->
            AppLog.w(TAG, "emote usage write failed: ${failure.javaClass.simpleName}")
        }.isSuccess
    }

    private companion object {
        const val TAG = "EmoteUsage"
        val gate = Any()
    }
}
