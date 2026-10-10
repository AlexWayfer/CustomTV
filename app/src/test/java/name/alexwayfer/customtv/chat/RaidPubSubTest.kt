package name.alexwayfer.customtv.chat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RaidPubSubTest {
    @Test
    fun raidGoStartsACountdownFromTheJitter() {
        val event = parseRaidPubSubMessage(
            """
            {"type":"raid_go_v2","raid":{
              "id":"raid-1",
              "target_login":"Target",
              "target_display_name":"Target",
              "target_profile_image":"https://example.com/target.png",
              "viewer_count":12,
              "transition_jitter_seconds":5
            }}
            """.trimIndent(),
            nowMillis = 1_000L,
        ) as RaidPubSubEvent.Go

        assertEquals("target", event.raid.targetLogin)
        assertEquals("Target", event.raid.targetDisplayName)
        assertEquals("https://example.com/target.png", event.raid.targetAvatarUrl)
        assertEquals(12, event.raid.viewerCount)
        assertEquals(6_000L, event.raid.goAtMillis)
        assertEquals(true, event.raid.leaving)
    }

    @Test
    fun raidUpdateStartsTheWebsiteCountdownWithoutLeaving() {
        val event = parseRaidPubSubMessage(
            """
            {"type":"raid_update_v2","raid":{
              "id":"raid-1",
              "target_login":"Target",
              "target_display_name":"Target",
              "viewer_count":4,
              "force_raid_now_seconds":90,
              "transition_jitter_seconds":0
            }}
            """.trimIndent(),
            nowMillis = 1_000L,
        ) as RaidPubSubEvent.Update

        assertEquals(91_000L, event.raid.goAtMillis)
        assertEquals(false, event.raid.leaving)
    }

    @Test
    fun aRepeatedUpdateKeepsTheOriginalDeadline() {
        val current = raid(goAtMillis = 50_000L, leaving = false)
        val update = RaidPubSubEvent.Update(current.copy(goAtMillis = 91_000L, viewerCount = 9))

        val merged = applyRaidEvent(current, update)

        assertEquals(50_000L, merged?.goAtMillis)
        assertEquals(9, merged?.viewerCount)
        assertEquals(false, merged?.leaving)
    }

    @Test
    fun raidGoReplacesThePrepCountdownWithTheJitter() {
        val current = raid(goAtMillis = 91_000L, leaving = false)
        val go = parseRaidPubSubMessage(
            """
            {"type":"raid_go_v2","raid":{
              "id":"raid-1",
              "target_login":"target",
              "transition_jitter_seconds":5,
              "force_raid_now_seconds":90
            }}
            """.trimIndent(),
            nowMillis = 1_000L,
        ) as RaidPubSubEvent.Go

        val merged = applyRaidEvent(current, go)

        assertEquals(6_000L, merged?.goAtMillis)
        assertEquals(true, merged?.leaving)
    }

    @Test
    fun anUpdateAfterGoDoesNotRestoreThePrepCountdown() {
        val current = raid(goAtMillis = 6_000L, leaving = true)
        val update = RaidPubSubEvent.Update(current.copy(goAtMillis = 91_000L, leaving = false))

        val merged = applyRaidEvent(current, update)

        assertEquals(6_000L, merged?.goAtMillis)
        assertEquals(true, merged?.leaving)
    }

    @Test
    fun aRaidFrameWithoutATargetIsIgnored() {
        assertNull(
            parseRaidPubSubFrame(
                """
                {"type":"MESSAGE","data":{"topic":"raid.9","message":"{\"type\":\"raid_go_v2\",\"raid\":{\"id\":\"raid-1\"}}"}}
                """.trimIndent(),
                nowMillis = 0L,
            ),
        )
    }

    @Test
    fun raidCancelClearsThatRaid() {
        val event = parseRaidPubSubMessage(
            """{"type":"raid_cancel_v2","raid":{"id":"raid-1"}}""",
            nowMillis = 0L,
        )

        assertEquals(RaidPubSubEvent.Cancel("raid-1"), event)
    }

    @Test
    fun aRaidUpdateWithoutATargetIsIgnored() {
        assertNull(
            parseRaidPubSubMessage(
                """{"type":"raid_update_v2","raid":{"id":"raid-1","force_raid_now_seconds":90}}""",
                nowMillis = 0L,
            ),
        )
    }

    @Test
    fun oneMillisecondLeftStillShowsOneSecond() {
        assertEquals(2, raidSecondsRemaining(goAtMillis = 1_001L, nowMillis = 0L))
        assertEquals(1, raidSecondsRemaining(goAtMillis = 1_000L, nowMillis = 0L))
        assertEquals(1, raidSecondsRemaining(goAtMillis = 1L, nowMillis = 0L))
        assertEquals(0, raidSecondsRemaining(goAtMillis = 1_000L, nowMillis = 1_000L))
        assertEquals(0, raidSecondsRemaining(goAtMillis = 1_000L, nowMillis = 1_500L))
    }

    @Test
    fun theBarShrinksSmoothlyAcrossTheWholeCountdown() {
        assertEquals(1f, raidFractionRemaining(goAtMillis = 10_000L, nowMillis = 0L, durationMillis = 10_000L))
        assertEquals(0.5f, raidFractionRemaining(goAtMillis = 10_000L, nowMillis = 5_000L, durationMillis = 10_000L))
        assertEquals(0f, raidFractionRemaining(goAtMillis = 10_000L, nowMillis = 10_000L, durationMillis = 10_000L))
        assertEquals(0f, raidFractionRemaining(goAtMillis = 10_000L, nowMillis = 11_000L, durationMillis = 10_000L))
        assertEquals(0f, raidFractionRemaining(goAtMillis = 10_000L, nowMillis = 0L, durationMillis = 0L))
    }

    @Test
    fun theCurrentChannelIsNotOpenedAgain() {
        assertNull(raidTargetToOpen("Foo", "foo"))
        assertNull(raidTargetToOpen("foo", "  "))
        assertEquals("bar", raidTargetToOpen("foo", " bar "))
    }

    @Test
    fun aNewRaidUpdateAnnouncesThatTheRaidWasCreated() {
        val update = RaidPubSubEvent.Update(raid(goAtMillis = 1_000L, leaving = false))

        assertEquals("raid-1", outgoingRaidCreatedId(current = null, update))
        assertNull(outgoingRaidCreatedId(raid(goAtMillis = 1_000L, leaving = false), update))
    }

    @Test
    fun aRaidGoWithoutAPriorUpdateAnnouncesCreationOnce() {
        val go = RaidPubSubEvent.Go(raid(goAtMillis = 1_000L, leaving = true))

        assertEquals("raid-1", outgoingRaidCreatedId(current = null, go))
        assertNull(outgoingRaidCreatedId(raid(goAtMillis = 90_000L, leaving = false), go))
    }

    @Test
    fun cancellingARaidDoesNotAnnounceCreation() {
        assertNull(
            outgoingRaidCreatedId(
                raid(goAtMillis = 1_000L, leaving = false),
                RaidPubSubEvent.Cancel("raid-1"),
            ),
        )
    }

    private fun raid(goAtMillis: Long, leaving: Boolean) = OutgoingRaid(
        id = "raid-1",
        targetLogin = "target",
        targetDisplayName = "Target",
        viewerCount = 1,
        goAtMillis = goAtMillis,
        leaving = leaving,
    )
}
