package name.alexwayfer.customtv.chat

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class ChannelHypeTrainTest {
    @Test
    fun startCountsTheTimeLeftFromItsOwnUpdateTimeAndKeepsTheColor() {
        val event = HypeTrainParser.parseFrame(frame("hype-train-start", START), NOW) as HypeTrainEvent.Execution
        val train = event.train

        assertEquals("t1", train.id)
        assertEquals(HypeTrainPhase.Active, train.phase)
        assertEquals(2, train.level)
        assertEquals(900, train.progress)
        assertEquals(1800, train.goal)
        assertEquals(NOW + 300_000L, train.endsAtMillis)
        assertEquals("00CCFF", train.colorHex)
        assertEquals(listOf(HypeTrainEmote("e1", "KittyHype")), train.rewards)
        assertEquals(50, hypeTrainPercent(train))
    }

    @Test
    fun levelUpCarriesTheWholeTrainWithItsConductors() {
        val message = """{"time_to_expire":1,"hype_train":$START_WITH_CONDUCTOR}"""
        val event = HypeTrainParser.parseFrame(frame("hype-train-level-up", message), NOW) as HypeTrainEvent.Execution

        assertEquals(listOf(HypeTrainConductor("SUBS", "pumi1986")), event.train.conductors)
    }

    @Test
    fun progressionMovesTheLevelAndKeepsTheColorOfTheShownTrain() {
        val start = (HypeTrainParser.parseFrame(frame("hype-train-start", START), NOW) as HypeTrainEvent.Execution).train
        val progress = HypeTrainParser.parseFrame(
            frame(
                "hype-train-progression",
                """{"id":"t1","progress":{"level":{"value":2,"goal":3400,"rewards":[]},"value":1400,"goal":1800,
                  "total":3000,"remaining_seconds":261}}""",
            ),
            NOW,
        )!!

        val moved = applyHypeTrainEvent(start, progress)!!
        assertEquals(1400, moved.progress)
        assertEquals(NOW + 261_000L, moved.endsAtMillis)
        assertEquals("00CCFF", moved.colorHex)
        assertEquals(start.rewards, moved.rewards)
    }

    @Test
    fun aConductorUpdateReplacesTheConductorOfTheSameCurrency() {
        val start = (HypeTrainParser.parseFrame(frame("hype-train-start", START_WITH_CONDUCTOR), NOW) as HypeTrainEvent.Execution).train
        val update = HypeTrainParser.parseFrame(
            frame(
                "hype-train-conductor-update",
                """{"id":"t1-SUBS","source":"SUBS","user":{"id":"1","login":"next","display_name":"Next"}}""",
            ),
            NOW,
        )!!

        assertEquals(listOf(HypeTrainConductor("SUBS", "Next")), applyHypeTrainEvent(start, update)?.conductors)
    }

    @Test
    fun anApproachShowsTheEventsLeftAndDoesNotReplaceARunningTrain() {
        val approaching = HypeTrainParser.parseFrame(
            frame(
                "hype-train-approaching",
                """{"channel_id":"1","goal":3,"events_remaining_durations":{"1":196},
                  "level_one_rewards":[{"type":"EMOTE","id":"e9","token":"ConfettiHype"}],"creator_color":"EB0400"}""",
            ),
            NOW,
        )!!

        val shown = applyHypeTrainEvent(null, approaching)!!
        assertEquals(HypeTrainPhase.Approaching, shown.phase)
        assertEquals(1, shown.eventsToStart)
        assertEquals(NOW + 196_000L, shown.endsAtMillis)
        assertEquals(listOf(HypeTrainEmote("e9", "ConfettiHype")), shown.rewards)
        assertEquals("EB0400", shown.colorHex)

        val running = (HypeTrainParser.parseFrame(frame("hype-train-start", START), NOW) as HypeTrainEvent.Execution).train
        assertEquals(running, applyHypeTrainEvent(running, approaching))
    }

    @Test
    fun theEndOfTheShownTrainKeepsItsLastLevelAndAnotherTrainsEndIsIgnored() {
        val running = (HypeTrainParser.parseFrame(frame("hype-train-start", START), NOW) as HypeTrainEvent.Execution).train
        val end = HypeTrainParser.parseFrame(frame("hype-train-end", """{"id":"t1","ending_reason":"COMPLETED"}"""), NOW)!!

        val ended = applyHypeTrainEvent(running, end)!!
        assertEquals(HypeTrainPhase.Ended, ended.phase)
        assertEquals(2, ended.level)
        assertEquals(running, applyHypeTrainEvent(running, HypeTrainEvent.End("other")))
    }

    @Test
    fun anEndedTrainHidesAfterAMinuteAndAnApproachWhenItsTimeRunsOut() {
        val running = (HypeTrainParser.parseFrame(frame("hype-train-start", START), NOW) as HypeTrainEvent.Execution).train
        assertNull(hypeTrainHidesAfter(running, NOW))
        assertEquals(1.minutes, hypeTrainHidesAfter(running.copy(phase = HypeTrainPhase.Ended), NOW)!!)
        val approaching = running.copy(phase = HypeTrainPhase.Approaching, endsAtMillis = NOW + 30_000L)
        assertEquals(30.seconds, hypeTrainHidesAfter(approaching, NOW)!!)
    }

    @Test
    fun gqlExecutionUsesTheRemainingSeconds() {
        val train = HypeTrainParser.execution(
            JSONObject(
                """{"id":"t2","updatedAt":"2026-09-29T18:02:09Z","expiresAt":"2026-09-29T18:07:09Z","endedAt":null,
                  "config":{"primaryHexColor":null},"progress":{"goal":5000,"progression":4000,"total":4000,
                  "remainingSeconds":88,"level":{"value":1,"goal":5000,"rewards":[{"emote":{"id":"e2","token":"Hype"}}]}},
                  "conductors":[{"source":"BITS","user":{"login":"a","displayName":"A"}}]}""",
            ),
            NOW,
        )!!

        assertEquals(NOW + 88_000L, train.endsAtMillis)
        assertNull(train.colorHex)
        assertEquals(80, hypeTrainPercent(train))
        assertEquals(listOf(HypeTrainConductor("BITS", "A")), train.conductors)
        assertEquals(listOf(HypeTrainEmote("e2", "Hype")), train.rewards)
    }

    @Test
    fun gqlApproachReadsTheEventsLeftAsAnObjectOrAListAndIgnoresAnExpiredOne() {
        val now = java.time.Instant.parse("2026-09-29T19:00:00Z").toEpochMilli()
        val single = HypeTrainParser.gqlApproaching(
            JSONObject(
                """{"goal":3,"expiresAt":"2026-09-29T19:03:16Z","eventsRemaining":{"events":1},"creatorColor":"00CCFF",
                  "levelOneRewards":[{"emote":{"id":"e9","token":"ConfettiHype"}}]}""",
            ),
            now,
        )!!
        assertEquals(HypeTrainPhase.Approaching, single.phase)
        assertEquals(1, single.eventsToStart)
        assertEquals(now + 196_000L, single.endsAtMillis)
        assertEquals(listOf(HypeTrainEmote("e9", "ConfettiHype")), single.rewards)
        assertEquals("00CCFF", single.colorHex)

        val list = HypeTrainParser.gqlApproaching(
            JSONObject("""{"goal":3,"expiresAt":"2026-09-29T19:03:16Z","eventsRemaining":[{"events":2},{"events":1}]}"""),
            now,
        )
        assertEquals(1, list?.eventsToStart)
        assertNull(list?.colorHex)

        assertNull(HypeTrainParser.gqlApproaching(JSONObject("""{"goal":3,"expiresAt":"2026-09-29T18:59:00Z"}"""), now))
    }

    private fun frame(type: String, data: String): String = JSONObject()
        .put("type", "MESSAGE")
        .put(
            "data",
            JSONObject()
                .put("topic", "hype-train-events-v2.1")
                .put("message", """{"type":"$type","data":$data}"""),
        )
        .toString()

    private companion object {
        const val NOW = 1_000_000L
        const val START = """{"__typename":"HypeTrainExecution","id":"t1","startedAt":"2026-09-29T17:50:25Z",
            "expiresAt":"2026-09-29T17:55:25Z","updatedAt":"2026-09-29T17:50:25Z","endedAt":null,
            "progress":{"goal":1800,"progression":900,"total":2500,"level":{"value":2,"goal":3400,
            "rewards":[{"__typename":"HypeTrainEmoteReward","id":"e1","type":"EMOTE","emote":{"id":"e1","token":"KittyHype"}}]}},
            "conductors":[],"config":{"willUseCreatorColor":true,"primaryHexColor":"00CCFF"}}"""
        const val START_WITH_CONDUCTOR = """{"id":"t1","expiresAt":"2026-09-29T17:56:35Z","updatedAt":"2026-09-29T17:51:35Z",
            "endedAt":null,"progress":{"goal":2100,"progression":100,"total":3500,"level":{"value":3,"goal":5500,"rewards":[]}},
            "conductors":[{"source":"SUBS","user":{"id":"606686045","displayName":"pumi1986","login":"pumi1986"}}],
            "config":{"primaryHexColor":null}}"""
    }
}
