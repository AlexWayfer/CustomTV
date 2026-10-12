package name.alexwayfer.customtv.chat

import name.alexwayfer.customtv.data.instantMillisOrNull
import name.alexwayfer.customtv.data.text
import org.json.JSONObject

// GQL names pin fields in camel case and PubSub in snake case, so each read tries every spelling.

internal fun JSONObject.pinObject(vararg keys: String): JSONObject? {
    for (key in keys) {
        if (has(key) && !isNull(key)) optJSONObject(key)?.let { return it }
    }
    return null
}

internal fun JSONObject.pinText(vararg keys: String): String? {
    for (key in keys) {
        text(key)?.let { return it }
    }
    return null
}

internal fun pinTimeMillis(raw: String?): Long? {
    if (raw.isNullOrBlank()) return null
    return instantMillisOrNull(raw)
}
