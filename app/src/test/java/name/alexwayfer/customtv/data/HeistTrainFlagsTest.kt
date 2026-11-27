package name.alexwayfer.customtv.data

import name.alexwayfer.customtv.chat.HeistTrainFlags
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

class HeistTrainFlagsTest {
    private fun flag(allocations: String, enabled: Boolean = true) = JSONObject(
        """{"enabled":$enabled,"entityId":144,"totalShards":10000,
            "variations":{"on":{"value":true},"off":{"value":false}},
            "allocations":[$allocations]}""",
    )

    private val on = """{"variationKey":"on","shards":[{"ranges":[{"start":0,"end":10000}]}]}"""
    private val off = """{"variationKey":"off","shards":[]}"""
    private val viewer = HeistTrainViewer("device1", null)

    @Test
    fun thePublicAllocationAppliesOnlyDuringItsWindow() {
        val targeting = """{"rules":[{"conditions":[{"attribute":"user_id","operator":"ONE_OF","value":["1"]}]}],
            "splits":[$off]}"""
        val window = """{"startAt":"2026-11-18T10:55:00Z","endAt":"2026-12-01T08:00:00Z","splits":[$on]}"""
        val flags = JSONObject()
            .put("Hype-Train-GTA-VI-Theme", flag("""$targeting,$window,{"splits":[$off]}"""))
            .put("Hype-Train-GTA-VI-Skip-Category-Check", flag("""{"splits":[$on]}"""))
        val start = instantMillisOrNull("2026-11-18T10:55:00Z")!!
        val end = instantMillisOrNull("2026-12-01T08:00:00Z")!!
        assertEquals(HeistTrainFlags(false, true), heistTrainFlags(flags, start - 1, viewer))
        assertEquals(HeistTrainFlags(true, true), heistTrainFlags(flags, start, viewer))
        assertEquals(HeistTrainFlags(false, true), heistTrainFlags(flags, end, viewer))
    }

    @Test
    fun disabledAndMissingFlagsDoNotEnableTheTheme() {
        val flags = JSONObject().put("Hype-Train-GTA-VI-Theme", flag("""{"splits":[$on]}""", enabled = false))
        assertEquals(HeistTrainFlags(false, false), heistTrainFlags(flags, 0L, viewer))
    }

    @Test
    fun twitchAssignmentsUseTheViewerForAllowlistsAndTheRightSubjectForTrafficSplits() {
        val rules = """[{"conditions":[{"attribute":"user_id","operator":"ONE_OF","value":["test-user"]}]}]"""
        val half = """{"salt":"campaign","ranges":[{"start":0,"end":5000}]}"""
        val deviceSplit = """{"variationKey":"on","shards":[$half]}"""
        val skip = flag("""{"rules":$rules,"splits":[$on]}""").put("entityId", 143)
        val flags = JSONObject()
            .put("Hype-Train-GTA-VI-Theme", flag("""{"splits":[$deviceSplit]}""").put("entityId", 144))
            .put("Hype-Train-GTA-VI-Skip-Category-Check", skip)
        assertEquals(HeistTrainFlags(true, false), heistTrainFlags(flags, 0L, HeistTrainViewer("device1", null)))
        assertEquals(HeistTrainFlags(false, true), heistTrainFlags(flags, 0L, HeistTrainViewer("device2", "test-user")))
    }
}
