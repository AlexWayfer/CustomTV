package name.alexwayfer.customtv.chat

import androidx.compose.ui.graphics.Color
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SevenTvCosmeticsTest {
    @Test
    fun parseImagePaintWithItsFirstShadow() {
        val frame = SevenTvEventParser.parseFrame(
            dispatch(
                "cosmetic.create",
                JSONObject().put(
                    "object",
                    JSONObject().put("kind", "PAINT").put(
                        "data",
                        JSONObject()
                            .put("id", "paint-1")
                            .put("color", JSONObject.NULL)
                            .put("gradients", JSONArray())
                            .put(
                                "shadows",
                                JSONArray()
                                    .put(shadow(x = 1.0, y = 2.0, radius = 0.5, color = 0xFF0000FF.toInt()))
                                    .put(shadow(x = 0.0, y = 0.0, radius = 4.0, color = 0x00FF00FF)),
                            )
                            .put("function", "URL")
                            .put("image_url", "https://cdn.7tv.app/paint/paint-1/layer/a/1x.webp")
                            .put("stops", JSONArray()),
                    ),
                ),
            ),
        )

        assertEquals(
            SevenTvFrame.Cosmetic(
                SevenTvCosmeticEvent.PaintDefined(
                    id = "paint-1",
                    paint = NamePaint(
                        function = PaintFunction.Image,
                        imageUrl = "https://cdn.7tv.app/paint/paint-1/layer/a/1x.webp",
                        shadow = PaintShadow(x = 1f, y = 2f, radius = 0.5f, color = Color.Red),
                    ),
                ),
            ),
            frame,
        )
    }

    @Test
    fun parseRepeatingLinearPaintWithRgbaStopsAndItsOwnColor() {
        val frame = SevenTvEventParser.parseFrame(
            dispatch(
                "cosmetic.create",
                JSONObject().put(
                    "object",
                    JSONObject().put("kind", "PAINT").put(
                        "data",
                        JSONObject()
                            .put("id", "paint-2")
                            .put("color", 0xFFFFFFFF.toInt())
                            .put("function", "LINEAR_GRADIENT")
                            .put("angle", 90)
                            .put("repeat", true)
                            .put(
                                "stops",
                                JSONArray()
                                    .put(JSONObject().put("at", 0.25).put("color", 0x0000FF80))
                                    .put(JSONObject().put("at", 0.75).put("color", 0xFF000080.toInt())),
                            ),
                    ),
                ),
            ),
        )

        val paint = ((frame as SevenTvFrame.Cosmetic).event as SevenTvCosmeticEvent.PaintDefined).paint
        assertEquals(PaintFunction.Linear, paint.function)
        assertEquals(90f, paint.angle)
        assertTrue(paint.repeat)
        assertEquals(Color.White, paint.color!!)
        assertEquals(
            listOf(
                PaintStop(0.25f, Color(red = 0, green = 0, blue = 255, alpha = 0x80)),
                PaintStop(0.75f, Color(red = 255, green = 0, blue = 0, alpha = 0x80)),
            ),
            paint.stops,
        )
    }

    @Test
    fun paintWithoutLegacyFieldsTakesItsFirstGradient() {
        val frame = SevenTvEventParser.parseFrame(
            dispatch(
                "cosmetic.create",
                JSONObject().put(
                    "object",
                    JSONObject().put("kind", "PAINT").put(
                        "data",
                        JSONObject()
                            .put("id", "paint-3")
                            .put(
                                "gradients",
                                JSONArray().put(
                                    JSONObject()
                                        .put("function", "RADIAL_GRADIENT")
                                        .put("shape", "ellipse")
                                        .put("repeat", false)
                                        .put(
                                            "stops",
                                            JSONArray()
                                                .put(JSONObject().put("at", 0).put("color", 0xFFFFFFFF.toInt()))
                                                .put(JSONObject().put("at", 1).put("color", 0x000000FF)),
                                        ),
                                ),
                            ),
                    ),
                ),
            ),
        )

        val paint = ((frame as SevenTvFrame.Cosmetic).event as SevenTvCosmeticEvent.PaintDefined).paint
        assertEquals(PaintFunction.Radial, paint.function)
        assertTrue(paint.ellipse)
        assertEquals(listOf(Color.White, Color.Black), paint.stops.map { it.color })
    }

    @Test
    fun parseBadgeAtTwiceTheBaseSize() {
        val frame = SevenTvEventParser.parseFrame(
            dispatch(
                "cosmetic.create",
                JSONObject().put(
                    "object",
                    JSONObject().put("kind", "BADGE").put(
                        "data",
                        JSONObject()
                            .put("id", "badge-1")
                            .put("name", "7TV Subscriber - 6 Months")
                            .put("tooltip", "7TV Subscriber (6 Months)")
                            .put("host", JSONObject().put("url", "//cdn.7tv.app/badge/badge-1")),
                    ),
                ),
            ),
        )

        assertEquals(
            SevenTvFrame.Cosmetic(
                SevenTvCosmeticEvent.BadgeDefined(
                    id = "badge-1",
                    badge = ThirdPartyBadge(
                        url = "https://cdn.7tv.app/badge/badge-1/2x.webp",
                        title = "7TV Subscriber (6 Months)",
                    ),
                ),
            ),
            frame,
        )
    }

    @Test
    fun paintEntitlementTakesThePaintTheChatterChoseForTheirTwitchAccount() {
        val frame = SevenTvEventParser.parseFrame(
            dispatch("entitlement.create", entitlement("PAINT", refId = "paint-old", chosenPaint = "paint-1")),
        )

        assertEquals(
            SevenTvFrame.Cosmetic(
                SevenTvCosmeticEvent.Assigned(SevenTvCosmeticKind.Paint, "paint-1", listOf("187003302")),
            ),
            frame,
        )
    }

    @Test
    fun badgeEntitlementWithoutStyleTakesItsReference() {
        val frame = SevenTvEventParser.parseFrame(
            dispatch("entitlement.create", entitlement("BADGE", refId = "badge-1")),
        )

        assertEquals(
            SevenTvFrame.Cosmetic(
                SevenTvCosmeticEvent.Assigned(SevenTvCosmeticKind.Badge, "badge-1", listOf("187003302")),
            ),
            frame,
        )
    }

    @Test
    fun deletedEntitlementRevokesItsReference() {
        val frame = SevenTvEventParser.parseFrame(
            dispatch("entitlement.delete", entitlement("PAINT", refId = "paint-1", chosenPaint = "paint-2")),
        )

        assertEquals(
            SevenTvFrame.Cosmetic(
                SevenTvCosmeticEvent.Revoked(SevenTvCosmeticKind.Paint, "paint-1", listOf("187003302")),
            ),
            frame,
        )
    }

    @Test
    fun emoteSetEntitlementStaysAPersonalEmoteEvent() {
        val frame = SevenTvEventParser.parseFrame(
            dispatch("entitlement.create", entitlement("EMOTE_SET", refId = "set-1")),
        )

        assertEquals(
            SevenTvFrame.Personal(PersonalEmoteEvent.SevenTvSetAssigned("set-1", listOf("187003302"))),
            frame,
        )
    }

    @Test
    fun chatterGetsTheirPaintAndBadgeOnceBothAreKnown() {
        val paint = NamePaint(function = PaintFunction.Image, imageUrl = "https://paint")
        val badge = ThirdPartyBadge(url = "https://badge", title = "Badge")
        val cosmetics = SevenTvCosmetics()
            .apply(SevenTvCosmeticEvent.Assigned(SevenTvCosmeticKind.Paint, "p", listOf("1", "2")))
            .apply(SevenTvCosmeticEvent.Assigned(SevenTvCosmeticKind.Badge, "b", listOf("1")))

        assertNull(cosmetics.paintFor("1"))
        val defined = cosmetics
            .apply(SevenTvCosmeticEvent.PaintDefined("p", paint))
            .apply(SevenTvCosmeticEvent.BadgeDefined("b", badge))
        assertEquals(paint, defined.paintFor("1"))
        assertEquals(paint, defined.paintFor("2"))
        assertEquals(badge, defined.badgeFor("1"))
        assertNull(defined.badgeFor("2"))
    }

    @Test
    fun revokingAnotherPaintKeepsTheChosenOne() {
        val cosmetics = SevenTvCosmetics()
            .apply(SevenTvCosmeticEvent.PaintDefined("p", NamePaint(function = PaintFunction.Image, imageUrl = "u")))
            .apply(SevenTvCosmeticEvent.Assigned(SevenTvCosmeticKind.Paint, "p", listOf("1")))

        assertEquals("u", cosmetics.apply(revoke("other")).paintFor("1")?.imageUrl)
        assertNull(cosmetics.apply(revoke("p")).paintFor("1"))
    }

    @Test
    fun anotherChannelForgetsTheChattersAndKeepsTheCosmetics() {
        val cosmetics = SevenTvCosmetics()
            .apply(SevenTvCosmeticEvent.PaintDefined("p", NamePaint(function = PaintFunction.Image, imageUrl = "u")))
            .apply(SevenTvCosmeticEvent.Assigned(SevenTvCosmeticKind.Paint, "p", listOf("1")))
            .withoutChatters()

        assertNull(cosmetics.paintFor("1"))
        assertEquals(
            "u",
            cosmetics.apply(SevenTvCosmeticEvent.Assigned(SevenTvCosmeticKind.Paint, "p", listOf("1")))
                .paintFor("1")?.imageUrl,
        )
    }

    private fun revoke(refId: String) =
        SevenTvCosmeticEvent.Revoked(SevenTvCosmeticKind.Paint, refId, listOf("1"))

    private fun shadow(x: Double, y: Double, radius: Double, color: Int) = JSONObject()
        .put("x_offset", x)
        .put("y_offset", y)
        .put("radius", radius)
        .put("color", color)

    private fun entitlement(kind: String, refId: String, chosenPaint: String? = null): JSONObject = JSONObject().put(
        "object",
        JSONObject()
            .put("kind", kind)
            .put("ref_id", refId)
            .put(
                "user",
                JSONObject()
                    .put("style", JSONObject().apply { chosenPaint?.let { put("paint_id", it) } })
                    .put(
                        "connections",
                        JSONArray()
                            .put(JSONObject().put("id", "187003302").put("platform", "TWITCH"))
                            .put(JSONObject().put("id", "1895590").put("platform", "KICK")),
                    ),
            ),
    )

    private fun dispatch(type: String, body: JSONObject): String = JSONObject()
        .put("op", 0)
        .put("d", JSONObject().put("type", type).put("body", body))
        .toString()
}
