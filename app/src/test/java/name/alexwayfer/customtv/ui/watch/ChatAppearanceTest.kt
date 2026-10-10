package name.alexwayfer.customtv.ui.watch

import name.alexwayfer.customtv.chat.DEFAULT_CHAT_TEXT_SIZE
import name.alexwayfer.customtv.chat.EmotePlatform
import name.alexwayfer.customtv.chat.EmoteSources
import name.alexwayfer.customtv.chat.SevenTvEmote
import org.junit.Assert.assertEquals
import org.junit.Test

class ChatAppearanceTest {
    private val sevenTv = mapOf(
        "Sadge" to SevenTvEmote("https://cdn.7tv.app/emote/sadge/2x.webp"),
        "7TV" to SevenTvEmote("https://cdn.7tv.app/emote/logo/2x.webp"),
    )
    private val ffz = mapOf(
        "Sadge" to SevenTvEmote("https://cdn.frankerfacez.com/emote/sadge/2"),
        "CatBag" to SevenTvEmote("https://cdn.frankerfacez.com/emote/catbag/2"),
    )
    private val bttv = mapOf(
        "Sadge" to SevenTvEmote("https://cdn.betterttv.net/emote/sadge/2x.webp"),
        "CatBag" to SevenTvEmote("https://cdn.betterttv.net/emote/catbag/2x.webp"),
        "catJAM" to SevenTvEmote("https://cdn.betterttv.net/emote/catjam/2x.webp"),
    )

    private fun appearance(sources: EmoteSources) = chatAppearance(
        readableColors = true,
        textSize = DEFAULT_CHAT_TEXT_SIZE,
        meMessageItalic = true,
        badgeUrls = emptyMap(),
        sources = sources,
        sevenTvEmotes = sevenTv,
        ffzEmotes = ffz,
        bttvEmotes = bttv,
    )

    @Test
    fun allSourcesOnMergeSevenTvOverFfzOverBttv() {
        val result = appearance(EmoteSources(sevenTv = true, ffz = true, bttv = true))
        assertEquals(sevenTv.getValue("Sadge"), result.thirdPartyEmotes["Sadge"])
        assertEquals(ffz.getValue("CatBag"), result.thirdPartyEmotes["CatBag"])
        assertEquals(bttv.getValue("catJAM"), result.thirdPartyEmotes["catJAM"])
        assertEquals(setOf(EmotePlatform.Bttv, EmotePlatform.Ffz), result.modifierPlatforms)
    }

    @Test
    fun sourceOffLeavesOutItsEmotesAndModifiers() {
        val result = appearance(EmoteSources(sevenTv = false, ffz = true, bttv = false))
        assertEquals(ffz, result.thirdPartyEmotes)
        assertEquals(setOf(EmotePlatform.Ffz), result.modifierPlatforms)
    }

    @Test
    fun allSourcesOffHaveNoEmotesOrModifiers() {
        val result = appearance(EmoteSources(sevenTv = false, ffz = false, bttv = false))
        assertEquals(emptyMap<String, SevenTvEmote>(), result.thirdPartyEmotes)
        assertEquals(emptySet<EmotePlatform>(), result.modifierPlatforms)
    }

    @Test
    fun platformLogoIsFoundByNameIgnoringCase() {
        val result = appearance(EmoteSources(sevenTv = true, ffz = true, bttv = true))
        assertEquals("7TV", result.platformLogos[EmotePlatform.SevenTv]?.name)
        assertEquals(null, result.platformLogos[EmotePlatform.Bttv])
    }
}
