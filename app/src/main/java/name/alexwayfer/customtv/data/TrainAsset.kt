package name.alexwayfer.customtv.data

import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener

/** Only read literals from Twitch's web build. Asset names are assembled from validated manifest entries. */
internal enum class TrainAsset(val chunk: String) {
    Banner("features.hype-train.components.banner-v3-container"),
    Names("69399"),
    Russian("core.locales.ru"),
    Palette("34609"),
}

private val chunkMaps = Regex("""\.u=\w+=>"assets/"\+\(\{([^{}]*)\}\[\w+]\|\|\w+\)\+"-"\+\{([^{}]*)\}""")
private val chunkEntry = Regex("""(\d+):"([\w.-]+)"""")
private val assetHash = Regex("[a-f0-9]{20}")
private val scriptAsset = Regex("""<script\s+src="https://assets\.twitch\.tv/assets/([\w.-]+)-([a-f0-9]{20})\.js"""")

internal fun trainAssetFiles(page: String): Map<TrainAsset, String> {
    val maps = requireNotNull(chunkMaps.find(page)) { "homepage: missing JavaScript filename maps" }
    val names = chunkEntry.findAll(maps.groupValues[1]).associate { it.groupValues[2] to it.groupValues[1] }
    val hashes = chunkEntry.findAll(maps.groupValues[2]).associate { it.groupValues[1] to it.groupValues[2] }
    val scripts = scriptAsset.findAll(page).associate { it.groupValues[1] to it.groupValues[2] }
    return TrainAsset.entries.associateWith { asset ->
        val id = names[asset.chunk] ?: asset.chunk
        val hash = scripts[asset.chunk] ?: hashes[id]
        require(hash != null && assetHash.matches(hash)) { "homepage: missing asset hash for $asset ($id)" }
        "${asset.chunk}-$hash.js"
    }
}

private const val JS_STRING = """("(?:[^"\\]++|\\.)*")"""
private const val STYLE_ID = """[a-z0-9_-]+"""
private val definition = Regex("""(?:case"($STYLE_ID)":return|return)\{id:"($STYLE_ID)",backgroundColors:\[""")
private val explicitColors = Regex("""backgroundColors:\[""")
private val nextProperty = Regex("""\],\s*\w+:""")
private val hex = Regex("#[A-Fa-f0-9]{6}")
private val paletteRef = Regex("""[\w.]+\["(color-[\w-]+)"]""")
private val midpoint = Regex("""positionPercent:(\d+(?:\.\d+)?)""")
private val title = Regex("""\b(trainName|sharedTrainName):\([^)]*\)\($JS_STRING,"(HypeTrainBannerV3)"\)""")
private val token = Regex(""""(color-[\w-]+)":"(#[0-9a-fA-F]{6})"""")
private val translation = Regex(
    """"(\[(?:HypeTrainBannerV3|HypeTrainBanner_AriaLabel)] (?:[^"\\]++|\\.)*)":$JS_STRING""",
)
private const val ARIA_TITLE = """\([^)]*\)\($JS_STRING,"HypeTrainBanner_AriaLabel"\)"""
private val namedType = Regex("""case [\w.]+\.([A-Z_]+):return \w+\?$ARIA_TITLE:\s*$ARIA_TITLE""")

internal fun parseTrainAsset(asset: TrainAsset, source: String): JSONObject {
    val result = when (asset) {
        TrainAsset.Banner -> parseDefinitions(source)
        TrainAsset.Palette -> JSONObject().apply {
            token.findAll(source).forEach { put(it.groupValues[1], it.groupValues[2]) }
        }
        TrainAsset.Russian -> JSONObject().apply {
            translation.findAll(source).forEach { put(jsText("\"${it.groupValues[1]}\""), jsText(it.groupValues[2])) }
        }
        TrainAsset.Names -> JSONObject().apply {
            namedType.findAll(source).forEach {
                val key = it.groupValues[1].lowercase().replace('_', '-')
                put(key, jsText(it.groupValues[3]))
                put("shared:$key", jsText(it.groupValues[2]))
            }
        }
    }
    require(result.length() > 0) { "no $asset entries" }
    return result
}

private fun parseDefinitions(source: String): JSONObject {
    val result = JSONObject()
    val middle = midpoint.find(source)?.groupValues?.get(1)?.toDoubleOrNull()
    definition.findAll(source).forEach { match ->
        val key = match.groupValues[1].ifEmpty { match.groupValues[2] }.replace('_', '-')
        val start = match.range.last + 1
        val end = nextProperty.find(source, start)?.range?.first ?: -1
        require(end >= start) { "$key: unterminated backgroundColors" }
        val colors = source.substring(start, end).split(',').map { color ->
            val literal = color.trim().removeSurrounding("\"")
            if (hex.matches(literal)) literal else requireNotNull(paletteRef.matchEntire(color.trim())) {
                "$key: unsupported background color expression"
            }.groupValues[1]
        }
        require(colors.size in 2..THREE_COLOR_GRADIENT) { "$key: unsupported gradient with ${colors.size} colors" }
        val style = JSONObject().put("colors", JSONArray(colors))
        if (colors.size == THREE_COLOR_GRADIENT) {
            require(middle != null && middle > 0 && middle < 100) { "$key: missing or invalid gradient midpoint" }
            style.put("middle", middle)
        }
        val finish = source.indexOf('}', end)
        require(finish >= 0) { "$key: incomplete theme" }
        title.findAll(source.substring(end, finish)).forEach { style.put(it.groupValues[1], jsText(it.groupValues[2])) }
        require(!result.has(key)) { "$key: duplicate theme" }
        result.put(key, style)
    }
    require(result.length() == explicitColors.findAll(source).count()) { "unrecognized backgroundColors definition" }
    return result
}

private fun jsText(literal: String): String = JSONTokener(literal).nextValue() as String
