package name.alexwayfer.customtv.chat

import name.alexwayfer.customtv.data.UserTwitchEmotes
import org.junit.Assert.assertEquals
import org.junit.Test

class EmotePickerSectionsTest {
    @Test
    fun channelSectionsComeBeforeTheUsersChannelAndThenGlobals() {
        val sections = emotePickerSections(
            channel = ChannelEmoteSource(
                label = "Quin69",
                twitch = listOf(
                    twitch("QuinLove", TwitchEmoteGroup.Follower),
                    twitch("QuinSub", TwitchEmoteGroup.Subscriptions),
                    twitch("QuinBits", TwitchEmoteGroup.Global),
                ),
                sevenTv = mapOf("Zed" to emote("z"), "aye" to emote("a")),
            ),
            own = ChannelEmoteSource(
                label = "alex",
                twitch = listOf(
                    twitch("MyFollow", TwitchEmoteGroup.Follower),
                    twitch("MySub", TwitchEmoteGroup.Subscriptions),
                ),
                sevenTv = mapOf("My7tv" to emote("m")),
                bttv = mapOf("MyBttv" to emote("b")),
                ffz = mapOf("MyFfz" to emote("ffz")),
            ),
            global = GlobalEmoteSource(
                twitch = listOf(twitch("Kappa", TwitchEmoteGroup.Global), twitch("HeyGuys", TwitchEmoteGroup.Global)),
                sevenTv = mapOf("Pepe" to emote("p")),
                bttv = mapOf("NaM" to emote("n")),
                ffz = mapOf("ZreknarF" to emote("f")),
            ),
            sources = EmoteSources(sevenTv = true, ffz = true, bttv = false),
        )

        assertEquals(
            listOf(
                "Channel Follow Quin69",
                "Channel Subscriptions Quin69",
                "Channel SevenTv Quin69",
                "OwnChannel Follow alex",
                "OwnChannel Subscriptions alex",
                "Global Twitch ",
                "Global SevenTv ",
                "Global Ffz ",
            ),
            sections.map { "${it.place} ${it.kind} ${it.label}" },
        )
        assertEquals(listOf("aye", "Zed"), sections[2].emotes.map { it.name })
        assertEquals(listOf("HeyGuys", "Kappa"), sections[5].emotes.map { it.name })
    }

    @Test
    fun frequentlyUsedMixesPlatformsAndHidesWhenNothingWasUsed() {
        val sections = emotePickerSections(
            channel = ChannelEmoteSource(
                label = "Quin69",
                twitch = listOf(twitch("Kappa", TwitchEmoteGroup.Global)),
                sevenTv = mapOf("Pepe" to emote("p")),
            ),
            own = null,
            global = GlobalEmoteSource(twitch = listOf(twitch("Kappa", TwitchEmoteGroup.Global))),
            sources = EmoteSources(sevenTv = true, ffz = false, bttv = false),
        )
        val usage = listOf(
            EmoteUse("Kappa", count = 2, usedAtMillis = 10L),
            EmoteUse("Pepe", count = 2, usedAtMillis = 20L),
            EmoteUse("Gone", count = 9, usedAtMillis = 30L),
        )

        assertEquals(sections, sectionsWithFrequentlyUsed(sections, emptyList()))
        val withUsage = sectionsWithFrequentlyUsed(sections, usage)
        assertEquals(EmotePickerPlace.Frequent, withUsage.first().place)
        assertEquals(listOf("Pepe", "Kappa"), withUsage.first().emotes.map { it.name })
        assertEquals("https://emote/Kappa", withUsage.first().emotes.first { it.name == "Kappa" }.url)
        assertEquals(sections, withUsage.drop(1))
    }

    @Test
    fun unlockedSetsComeBeforeGlobalTwitch() {
        val sections = emotePickerSections(
            channel = ChannelEmoteSource(label = "Quin69"),
            own = null,
            global = GlobalEmoteSource(twitch = listOf(twitch("Kappa", TwitchEmoteGroup.Global))),
            sources = noThirdParty,
            personal = UserTwitchEmotes(
                smilies = emptyList(),
                turbo = emptyList(),
                unlocked = listOf(UnlockedSourceEmote("HypeYawn", "https://emote/HypeYawn", "hypetrain")),
            ),
        )

        assertEquals(
            listOf("Unlocked Twitch TwitchHypeTrain", "Global Twitch "),
            sections.map { "${it.place} ${it.kind} ${it.label}" },
        )
    }

    @Test
    fun globalTwitchSectionUsesTheCollapsedTurboFace() {
        val sections = emotePickerSections(
            channel = ChannelEmoteSource(label = "Quin69"),
            own = null,
            global = GlobalEmoteSource(
                twitch = listOf(
                    twitch(":o", TwitchEmoteGroup.Global),
                    twitch("Kappa", TwitchEmoteGroup.Global),
                ),
            ),
            sources = noThirdParty,
            personal = UserTwitchEmotes(
                smilies = emptyList(),
                turbo = listOf(
                    TwitchCatalogEmote(":-O", "https://emote/turbo", TwitchEmoteGroup.Global),
                ),
            ),
        )

        val twitch = sections.single()
        assertEquals(listOf(":O", "Kappa"), twitch.emotes.map { it.name })
        assertEquals("https://emote/turbo", twitch.emotes[0].url)
    }

    private val noThirdParty = EmoteSources(sevenTv = false, ffz = false, bttv = false)

    private fun twitch(name: String, group: TwitchEmoteGroup) = TwitchCatalogEmote(name, "https://emote/$name", group)

    private fun emote(url: String) = SevenTvEmote(url = url)
}
