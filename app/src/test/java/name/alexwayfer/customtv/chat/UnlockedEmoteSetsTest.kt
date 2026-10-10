package name.alexwayfer.customtv.chat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UnlockedEmoteSetsTest {
    @Test
    fun eventSetsComeBeforeGlobalsAndChannelSubscriptionsStayOut() {
        val groups = unlockedEmoteGroups(
            listOf(
                source("SirPrise", "twofactor"),
                source("PrideWave", "limitedtime"),
                source("PrideCute", "limitedtime"),
                source("LuvCool", "limitedtime"),
                source("HypeYawn", "hypetrain"),
                source("OWL2019Tracer", "owl2019"),
                source("NasaSparkle", "rewards"),
                source("HahaCat", "limitedtime"),
                source("QuinSub", "subscriptions"),
                source("Kappa", "globals"),
                source(":)", "smilies"),
            ),
        )

        assertEquals(
            listOf("TwitchHypeTrain", "ow_esports", "StreamWithPride", "streamerluv", "Haha", "Unlocked"),
            groups.map { it.title },
        )
        assertEquals(listOf("PrideCute", "PrideWave"), groups.first { it.title == "StreamWithPride" }.emotes.map { it.name })
        assertEquals(listOf("NasaSparkle", "SirPrise"), groups.first { it.title == "Unlocked" }.emotes.map { it.name })
        assertNull(unlockedEmoteSet("subscriptions", "QuinSub"))
        assertNull(unlockedEmoteSet("follower", "FollowEmote"))
    }

    private fun source(name: String, type: String) = UnlockedSourceEmote(name, "https://emote/$name", type)
}
