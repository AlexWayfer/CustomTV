package name.alexwayfer.customtv.data

import org.json.JSONObject

internal fun requestChatterCheckTwitch(request: JSONObject): JSONObject =
    JSONObject().put("error", "twitch-login")
