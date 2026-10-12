package name.alexwayfer.customtv.data

import org.json.JSONArray
import org.json.JSONObject

/** The items of this array that are objects, in order. */
internal fun JSONArray.objects(): List<JSONObject> = (0 until length()).mapNotNull { optJSONObject(it) }
