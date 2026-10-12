package name.alexwayfer.customtv.data

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/** This text as a JSON object, or null when it is not one, such as an HTML error page. */
internal fun String.toJsonObjectOrNull(): JSONObject? = try {
    JSONObject(this)
} catch (_: JSONException) {
    null
}

/** This text as a JSON array, or null when it is not one. */
internal fun String.toJsonArrayOrNull(): JSONArray? = try {
    JSONArray(this)
} catch (_: JSONException) {
    null
}

/** This value, or null when it is blank or a null a server wrote as the word. */
internal fun String?.textOrNull(): String? = this?.takeIf { it.isNotBlank() && it != "null" }

/** The text at [name] as sent, or null when it is missing, null, or blank. */
internal fun JSONObject.text(name: String): String? = optString(name).textOrNull()

/** The text at [name] without the spaces around it, or null when nothing else is left. */
internal fun JSONObject.trimmedText(name: String): String? = optString(name).trim().textOrNull()

/** The value at [name] as a string, empty included, or null when it is missing or null. */
internal fun JSONObject.stringOrNull(name: String): String? = if (has(name) && !isNull(name)) optString(name) else null

internal fun JSONObject.intOrNull(name: String): Int? = if (has(name) && !isNull(name)) optInt(name) else null

internal fun JSONObject.longOrNull(name: String): Long? = if (has(name) && !isNull(name)) optLong(name) else null
