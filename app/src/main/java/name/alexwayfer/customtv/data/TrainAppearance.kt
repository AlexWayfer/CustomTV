package name.alexwayfer.customtv.data

import name.alexwayfer.customtv.chat.opaqueArgbFromHex
import org.json.JSONObject

internal data class TrainAppearance(val english: String, val russian: String, val stops: List<Pair<Float, Long>>)

internal const val THREE_COLOR_GRADIENT = 3

/** Build and validate a replacement without mutating the last good snapshot. */
internal fun refreshTrainCatalog(page: String, old: JSONObject, download: (String) -> String): JSONObject {
    val next = JSONObject()
    for ((asset, file) in trainAssetFiles(page)) {
        val previous = old.optJSONObject(asset.name)
        val entry = if (previous?.optString("file") == file) {
            previous
        } else {
            val source = download(file)
            val data = try {
                parseTrainAsset(asset, source)
            } catch (error: IllegalArgumentException) {
                throw IllegalArgumentException("$file (${source.length} characters): ${error.message}", error)
            }
            JSONObject().put("file", file).put("data", data)
        }
        next.put(asset.name, entry)
    }
    trainAppearances(next)
    return next
}

internal fun trainAppearances(cache: JSONObject): Map<String, TrainAppearance> {
    val definitions = cache.assetData(TrainAsset.Banner)
    val names = cache.assetData(TrainAsset.Names)
    val russian = cache.assetData(TrainAsset.Russian)
    val palette = cache.assetData(TrainAsset.Palette)
    require(definitions.length() > 0) { "no themes in catalog" }
    return buildMap {
        for (key in definitions.keys()) {
            val style = definitions.getJSONObject(key)
            val stops = gradientStops(key, style, palette)
            for ((id, field) in listOf(key to "trainName", "shared:$key" to "sharedTrainName")) {
                val explicit = style.trimmedText(field)
                val en = requireNotNull(explicit ?: names.trimmedText(id)) { "$id: missing English title" }
                val group = if (explicit == null) "HypeTrainBanner_AriaLabel" else "HypeTrainBannerV3"
                val ru = requireNotNull(russian.trimmedText("[$group] $en")) { "$id: missing Russian title in $group" }
                put(id, TrainAppearance(en, ru, stops))
            }
        }
    }
}

private fun JSONObject.assetData(asset: TrainAsset): JSONObject =
    requireNotNull(optJSONObject(asset.name)?.optJSONObject("data")) { "missing cached $asset data" }

private fun gradientStops(key: String, style: JSONObject, palette: JSONObject): List<Pair<Float, Long>> {
    val colors = style.getJSONArray("colors")
    require(colors.length() in 2..THREE_COLOR_GRADIENT) { "$key: invalid cached gradient" }
    return (0 until colors.length()).map { index ->
        val value = colors.getString(index)
        val argb = opaqueArgbFromHex(if (value.startsWith('#')) value else palette.trimmedText(value))
        val stop = when (index) {
            0 -> 0f
            colors.length() - 1 -> 1f
            else -> (style.getDouble("middle") / 100).toFloat()
        }
        require(stop in 0f..1f) { "$key: invalid gradient stop" }
        stop to requireNotNull(argb) { "$key: unresolved color $value" }
    }
}
