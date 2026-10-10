package name.alexwayfer.customtv.chat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class OutgoingRaidRecheckTest {
    private val raid = OutgoingRaid(
        id = "raid-1",
        targetLogin = "target",
        targetDisplayName = "Target",
        targetAvatarUrl = "https://example.com/target.png",
        viewerCount = 10,
        goAtMillis = 5_000L,
        leaving = false,
    )

    @Test
    fun goneAfterTheCountdownEndedGoesNow() {
        val result = recheckOutgoingRaid(raid, RaidLookup.None, nowMillis = 9_000L)

        assertEquals(raid.copy(leaving = true, goAtMillis = 9_000L), result)
    }

    @Test
    fun goneAtTheDeadlineGoesNow() {
        val result = recheckOutgoingRaid(raid, RaidLookup.None, nowMillis = 5_000L)

        assertEquals(true, result?.leaving)
    }

    @Test
    fun goneBeforeTheCountdownEndedWasCanceled() {
        assertNull(recheckOutgoingRaid(raid, RaidLookup.None, nowMillis = 4_999L))
    }

    @Test
    fun failedLookupKeepsTheRaid() {
        assertSame(raid, recheckOutgoingRaid(raid, RaidLookup.Failed, nowMillis = 9_000L))
    }

    @Test
    fun activeRaidKeepsTheCountdownAndTakesTheCount() {
        val lookup = RaidLookup.Active("raid-1", "target", "Target", null, viewerCount = 25)

        val result = recheckOutgoingRaid(raid, lookup, nowMillis = 9_000L)

        assertEquals(raid.copy(viewerCount = 25), result)
    }

    @Test
    fun activeRaidToAnotherChannelTakesTheNewTarget() {
        val lookup = RaidLookup.Active("raid-2", "other", "Other", null, viewerCount = 25)

        val result = recheckOutgoingRaid(raid, lookup, nowMillis = 1_000L)

        assertEquals(
            raid.copy(id = "raid-2", targetLogin = "other", targetDisplayName = "Other", targetAvatarUrl = null, viewerCount = 25),
            result,
        )
    }

    @Test
    fun leavingRaidIsLeftAlone() {
        val leaving = raid.copy(leaving = true)

        assertSame(leaving, recheckOutgoingRaid(leaving, RaidLookup.None, nowMillis = 1_000L))
    }

    @Test
    fun noRaidStaysEmpty() {
        assertNull(recheckOutgoingRaid(null, RaidLookup.None, nowMillis = 9_000L))
    }

    @Test
    fun parsesAnActiveRaid() {
        val lookup = parseRaidLookup(
            """
            {"data":{"user":{"raid":{"id":"raid-1","viewerCount":12,
              "targetChannel":{"login":"Target","displayName":"Target",
                "profileImageURL":"https://example.com/target.png"}}}}}
            """.trimIndent(),
        )

        assertEquals(RaidLookup.Active("raid-1", "target", "Target", "https://example.com/target.png", 12), lookup)
    }

    @Test
    fun parsesNoRaid() {
        assertEquals(RaidLookup.None, parseRaidLookup("""{"data":{"user":{"raid":null}}}"""))
    }

    @Test
    fun missingUserOrErrorsIsAFailure() {
        assertEquals(RaidLookup.Failed, parseRaidLookup("""{"data":{"user":null}}"""))
        assertEquals(RaidLookup.Failed, parseRaidLookup("""{"errors":[{"message":"boom"}]}"""))
        assertEquals(RaidLookup.Failed, parseRaidLookup("not json"))
    }
}
