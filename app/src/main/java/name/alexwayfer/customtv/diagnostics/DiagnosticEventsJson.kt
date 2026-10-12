package name.alexwayfer.customtv.diagnostics

import name.alexwayfer.customtv.data.intOrNull
import org.json.JSONArray
import org.json.JSONObject

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
            httpCode = item.intOrNull("httpCode"),
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
