package name.alexwayfer.customtv.chat

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HeistTrainThemeTest {
    private val enabled = HeistTrainFlags(enabled = true, skipCategory = false)

    private fun theme(
        game: String = "1867279146",
        train: String = "{}",
        phase: HypeTrainPhase = HypeTrainPhase.Active,
        flags: HeistTrainFlags? = enabled,
    ): HypeTrainTheme? {
        val key = if (phase == HypeTrainPhase.Approaching) "approaching" else "execution"
        val user = JSONObject("""{"stream":{"game":{"id":"$game"}},"channel":{"hypeTrain":{"$key":$train}}}""")
        return hypeTrainTheme(user, phase) { flags }
    }

    @Test
    fun regularTrainRequiresTheCampaignAndCategoryUnlessTwitchSkipsTheCategory() {
        assertEquals(HypeTrainTheme.Heist, theme())
        assertEquals(HypeTrainTheme.Regular, theme(game = "123"))
        assertEquals(HypeTrainTheme.Regular, theme(flags = enabled.copy(enabled = false, skipCategory = true)))
        assertEquals(HypeTrainTheme.Heist, theme(game = "123", flags = enabled.copy(skipCategory = true)))
        assertNull(theme(flags = null))
    }

    @Test
    fun aSharedTrainChecksAllParticipatingStreams() {
        val shared = """{"sharedHypeTrainDetails":{"sharedProgress":[{"user":{"stream":null}},
            {"user":{"stream":{"game":{"id":"1867279146"}}}}]}}"""
        assertEquals(HypeTrainTheme.SharedHeist, theme(game = "123", train = shared))
        assertEquals(HypeTrainTheme.Regular, theme(game = "123", train = shared.replace("1867279146", "456")))
    }

    @Test
    fun specialTrainsKeepTheirOwnThemeDuringTheCampaign() {
        val pinata = """{"variantTrainDetails":{"__typename":"PinataTrainDetails"}}"""
        assertEquals(HypeTrainTheme.Regular, theme(train = pinata))
        for (phase in listOf(HypeTrainPhase.Approaching, HypeTrainPhase.Active)) {
            assertEquals("golden-kappa", theme(train = """{"isGoldenKappaTrain":true}""", phase = phase)?.key)
            val treasure = """{"isTreasureTrain":true,"treasureTrainDetails":{}}"""
            assertEquals("treasure", theme(train = treasure, phase = phase)?.key)
        }
        for (type in listOf("MYTHIC", "COMMUNITY", "FUTURE_TYPE")) {
            val train = """{"hypeTrainType":"$type"}"""
            val expected = type.lowercase().replace('_', '-')
            assertEquals(expected, theme(train = train, phase = HypeTrainPhase.Approaching)?.key)
        }
        val community = """{"variantTrainDetails":{"__typename":"CommunityTrainDetails"}}"""
        assertEquals("community", theme(train = community)?.key)
        val shared = """{"variantTrainDetails":{"__typename":"FutureTypeTrainDetails"},"sharedHypeTrainDetails":{}}"""
        assertEquals("shared:future-type", theme(train = shared)?.key)
        assertEquals(HypeTrainTheme.Heist, theme(phase = HypeTrainPhase.Approaching))
    }

    @Test
    fun anApproachWithNoVariantOrWithoutTreasureDetailsCanUseTheCampaign() {
        for (train in listOf("""{"hypeTrainType":null}""", """{"hypeTrainType":"TREASURE","isTreasureTrain":true}""")) {
            assertEquals(HypeTrainTheme.Heist, theme(train = train, phase = HypeTrainPhase.Approaching))
        }
    }
}
