package name.alexwayfer.customtv.chat

import androidx.compose.ui.graphics.Color
import name.alexwayfer.customtv.data.SevenTvRepository
import name.alexwayfer.customtv.data.objects
import name.alexwayfer.customtv.data.text
import org.json.JSONObject

/** Reads the paints and badges of the 7TV socket, as the 7TV extension and Xtra read them. */
internal object SevenTvCosmeticParser {
    /** The cosmetic event of a dispatch of [type] with [body]; null for anything else, such as an emote set. */
    fun parse(type: String, body: JSONObject): SevenTvCosmeticEvent? {
        val item = body.optJSONObject("object") ?: return null
        return when (type) {
            "cosmetic.create" -> parseCosmetic(item)
            "entitlement.create" -> parseEntitlement(item, assigned = true)
            "entitlement.delete" -> parseEntitlement(item, assigned = false)
            else -> null
        }
    }

    private fun parseCosmetic(item: JSONObject): SevenTvCosmeticEvent? {
        val data = item.optJSONObject("data") ?: return null
        val id = data.text("id") ?: item.text("id") ?: return null
        return when (item.optString("kind")) {
            "PAINT" -> parsePaint(data)?.let { SevenTvCosmeticEvent.PaintDefined(id, it) }
            "BADGE" -> parseBadge(data)?.let { SevenTvCosmeticEvent.BadgeDefined(id, it) }
            else -> null
        }
    }

    // The paint's legacy fields describe its first layer; a paint without them has the layer in its gradients.
    private fun parsePaint(data: JSONObject): NamePaint? {
        val layer = data.takeIf { it.text("function") != null }
            ?: data.optJSONArray("gradients")?.optJSONObject(0)
            ?: return null
        val function = when (layer.text("function")) {
            "LINEAR_GRADIENT" -> PaintFunction.Linear
            "RADIAL_GRADIENT" -> PaintFunction.Radial
            "CONIC_GRADIENT" -> PaintFunction.Conic
            "URL" -> PaintFunction.Image
            else -> return null
        }
        val imageUrl = layer.text("image_url")
        if (function == PaintFunction.Image && imageUrl == null) return null
        return NamePaint(
            function = function,
            stops = layer.optJSONArray("stops")?.objects().orEmpty().map { stop ->
                PaintStop(stop.optDouble("at").toFloat(), rgbaColor(stop.optLong("color")))
            },
            angle = layer.optDouble("angle", 0.0).toFloat(),
            repeat = layer.optBoolean("repeat"),
            ellipse = layer.text("shape") == "ellipse",
            imageUrl = imageUrl.takeIf { function == PaintFunction.Image },
            shadow = data.optJSONArray("shadows")?.optJSONObject(0)?.let { shadow ->
                PaintShadow(
                    x = shadow.optDouble("x_offset").toFloat(),
                    y = shadow.optDouble("y_offset").toFloat(),
                    radius = shadow.optDouble("radius").toFloat(),
                    color = rgbaColor(shadow.optLong("color")),
                )
            },
            color = if (data.isNull("color")) null else rgbaColor(data.optLong("color")),
        )
    }

    private fun parseBadge(data: JSONObject): ThirdPartyBadge? {
        val url = SevenTvRepository.hostImageUrl(data.optJSONObject("host")) ?: return null
        val title = data.text("tooltip") ?: data.text("name") ?: return null
        return ThirdPartyBadge(url = url, title = title)
    }

    // The paint or badge a chatter chose is in their style; an entitlement without it names its own.
    private fun parseEntitlement(item: JSONObject, assigned: Boolean): SevenTvCosmeticEvent? {
        val kind = cosmeticKind(item.optString("kind")) ?: return null
        val user = item.optJSONObject("user")
        val userIds = user?.optJSONArray("connections")?.objects().orEmpty()
            .filter { it.optString("platform") == "TWITCH" }
            .mapNotNull { it.text("id") }
        val chosen = if (assigned) user?.optJSONObject("style")?.text(styleKey(kind)) else null
        val refId = chosen ?: item.text("ref_id")
        if (userIds.isEmpty() || refId == null) return null
        return if (assigned) {
            SevenTvCosmeticEvent.Assigned(kind, refId, userIds)
        } else {
            SevenTvCosmeticEvent.Revoked(kind, refId, userIds)
        }
    }

    private fun cosmeticKind(kind: String): SevenTvCosmeticKind? = when (kind) {
        "PAINT" -> SevenTvCosmeticKind.Paint
        "BADGE" -> SevenTvCosmeticKind.Badge
        else -> null
    }

    private fun styleKey(kind: SevenTvCosmeticKind): String = when (kind) {
        SevenTvCosmeticKind.Paint -> "paint_id"
        SevenTvCosmeticKind.Badge -> "badge_id"
    }

    /** A color 7TV sends as one RGBA number, red in its highest byte and alpha in its lowest. */
    private fun rgbaColor(value: Long): Color {
        val rgba = value.toInt()
        return Color(
            red = (rgba ushr 24) and 0xFF,
            green = (rgba ushr 16) and 0xFF,
            blue = (rgba ushr 8) and 0xFF,
            alpha = rgba and 0xFF,
        )
    }
}
