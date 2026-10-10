package name.alexwayfer.customtv.chat

import java.time.Instant
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChannelEventsTest {
    @Test
    fun pollUpdateFrameCountsVotesAndTheTimeLeftFromNow() {
        val poll = ChannelPollParser.parseFrame(pollFrame("POLL_UPDATE", "ACTIVE", votes = listOf(0, 1, 0, 1)), NOW)

        assertEquals("5fc45bdd", poll?.id)
        assertEquals("Тест", poll?.title)
        assertEquals(ChannelPollStatus.Active, poll?.status)
        assertEquals(listOf(0, 1, 0, 1), poll?.choices?.map { it.votes })
        assertEquals(2, poll?.totalVotes)
        assertEquals(600_000L, poll?.durationMillis)
        assertEquals(NOW + 222_502L, poll?.endsAtMillis)
    }

    @Test
    fun completedPollShowsItsResultsAndAnArchivedOneIsHidden() {
        assertEquals(
            ChannelPollStatus.Ended,
            ChannelPollParser.parseFrame(pollFrame("POLL_COMPLETE", "COMPLETED"), NOW)?.status,
        )
        assertEquals(
            ChannelPollStatus.Hidden,
            ChannelPollParser.parseFrame(pollFrame("POLL_ARCHIVE", "COMPLETED"), NOW)?.status,
        )
    }

    @Test
    fun pollSharesRoundToPercentAndAnEmptyPollHasNoLeader() {
        assertEquals(33, pollChoicePercent(1, 3))
        assertEquals(0, pollChoicePercent(0, 0))
        val empty = ChannelPollParser.parseFrame(pollFrame("POLL_CREATE", "ACTIVE"), NOW)!!
        assertEquals(emptySet<String>(), pollLeaderIds(empty))
        val tied = ChannelPollParser.parseFrame(pollFrame("POLL_UPDATE", "ACTIVE", votes = listOf(0, 1, 0, 1)), NOW)!!
        assertEquals(setOf("c2", "c4"), pollLeaderIds(tied))
    }

    @Test
    fun predictionWindowCountsFromTheServerTimeOfTheFrame() {
        val server = Instant.parse("2026-09-29T15:28:43Z").toEpochMilli()
        val phoneAhead = server + 5_000L
        val prediction = ChannelPredictionParser.parseFrame(predictionFrame("ACTIVE"), phoneAhead)

        assertEquals(ChannelPredictionStatus.Active, prediction?.status)
        assertEquals(listOf(0L, 0L, 10L), prediction?.outcomes?.map { it.points })
        assertEquals(1_800_000L, prediction?.windowMillis)
        val created = Instant.parse("2026-09-29T15:28:33Z").toEpochMilli()
        assertEquals(created + 1_800_000L + 5_000L, prediction?.endsAtMillis)
    }

    @Test
    fun closedPendingAndCanceledPredictionsAreHiddenAndAResolvedOneNamesItsWinner() {
        listOf("LOCKED", "RESOLVE_PENDING", "CANCEL_PENDING", "CANCELED").forEach { status ->
            assertEquals(
                ChannelPredictionStatus.Hidden,
                ChannelPredictionParser.parseFrame(predictionFrame(status), NOW)?.status,
            )
        }
        val resolved = ChannelPredictionParser.parseFrame(predictionFrame("RESOLVED", winner = "o3"), NOW)
        assertEquals(ChannelPredictionStatus.Resolved, resolved?.status)
        assertEquals("o3", resolved?.winningOutcomeId)
    }

    @Test
    fun aHiddenUpdateRemovesOnlyTheSamePollOrPrediction() {
        val poll = ChannelPollParser.parseFrame(pollFrame("POLL_UPDATE", "ACTIVE"), NOW)!!
        val archived = poll.copy(status = ChannelPollStatus.Hidden)
        assertNull(applyPollUpdate(poll, archived))
        assertEquals(poll, applyPollUpdate(poll, archived.copy(id = "older")))

        val prediction = ChannelPredictionParser.parseFrame(predictionFrame("ACTIVE"), NOW)!!
        val locked = prediction.copy(status = ChannelPredictionStatus.Hidden)
        assertNull(applyPredictionUpdate(prediction, locked))
        val resolved = locked.copy(status = ChannelPredictionStatus.Resolved)
        assertEquals(resolved, applyPredictionUpdate(null, resolved))
    }

    @Test
    fun gqlAnswerBringsTheRunningPollAndPrediction() {
        val votes = parseChannelEventsGql(
            """
            {"data":{"user":{"viewablePoll":{"id":"p1","title":"Да?","status":"ACTIVE","durationSeconds":600,
              "remainingDurationMilliseconds":502824,"choices":[{"id":"c1","title":"1","votes":{"total":2}}],
              "votes":{"total":2}},
             "channel":{"activePredictionEvents":[{"id":"e1","title":"Тест","status":"ACTIVE",
              "createdAt":"2026-09-29T15:28:33Z","predictionWindowSeconds":1800,"winningOutcome":null,
              "outcomes":[{"id":"o1","title":"Да","totalPoints":10,"totalUsers":1}]}]}}}}
            """.trimIndent(),
            NOW,
        )

        assertEquals(NOW + 502_824L, votes?.poll?.endsAtMillis)
        assertEquals(listOf(10L), votes?.prediction?.outcomes?.map { it.points })
        assertEquals(ChannelEvents(), parseChannelEventsGql("""{"data":{"user":{"viewablePoll":null}}}""", NOW))
        assertNull(parseChannelEventsGql("""{"errors":[{"message":"x"}]}""", NOW))
    }

    @Test
    fun aLoadThatFinishesAfterAPubSubChangeDoesNotBringBackTheOlderState() {
        val answer = CompletableDeferred<ChannelEvents?>()
        val controller = ChannelEventsController(CoroutineScope(Dispatchers.Unconfined), fetch = { answer.await() })
        val running = ChannelPredictionParser.parseFrame(predictionFrame("ACTIVE"), NOW)!!
        val poll = ChannelPollParser.parseFrame(pollFrame("POLL_UPDATE", "ACTIVE"), NOW)!!

        controller.load("117474239")
        controller.onPrediction(running.copy(status = ChannelPredictionStatus.Hidden))
        answer.complete(ChannelEvents(poll = poll, prediction = running))

        assertEquals(poll, controller.events.value.poll)
        assertNull(controller.events.value.prediction)
    }

    @Test
    fun aHiddenPollStaysHiddenUntilANewPollStarts() {
        val controller = ChannelEventsController(CoroutineScope(Dispatchers.Unconfined), fetch = { null })
        val poll = ChannelPollParser.parseFrame(pollFrame("POLL_UPDATE", "ACTIVE"), NOW)!!

        controller.onPoll(poll)
        controller.hide(poll.id)
        controller.onPoll(poll.copy(totalVotes = 5))
        assertNull(controller.events.value.poll)

        controller.onPoll(poll.copy(id = "next"))
        assertEquals("next", controller.events.value.poll?.id)
    }

    @Test
    fun aPredictionResultHidesOnceItsMinuteIsUpAndARepeatDoesNotRestartIt() {
        val waits = mutableListOf<CompletableDeferred<Unit>>()
        val controller = ChannelEventsController(
            scope = CoroutineScope(Dispatchers.Unconfined),
            fetch = { null },
            wait = { CompletableDeferred<Unit>().also(waits::add).await() },
        )
        val resolved = ChannelPredictionParser.parseFrame(predictionFrame("RESOLVED", winner = "o3"), NOW)!!

        controller.onPrediction(resolved)
        controller.onPrediction(resolved)
        assertEquals(1, waits.size)
        assertEquals(resolved, controller.events.value.prediction)

        waits.single().complete(Unit)
        assertNull(controller.events.value.prediction)
    }

    @Test
    fun aNewPredictionCancelsThePreviousResultTimer() {
        val waits = mutableListOf<CompletableDeferred<Unit>>()
        val controller = ChannelEventsController(
            scope = CoroutineScope(Dispatchers.Unconfined),
            fetch = { null },
            wait = { CompletableDeferred<Unit>().also(waits::add).await() },
        )
        val resolved = ChannelPredictionParser.parseFrame(predictionFrame("RESOLVED", winner = "o3"), NOW)!!
        val next = ChannelPredictionParser.parseFrame(predictionFrame("ACTIVE"), NOW)!!.copy(id = "next")

        controller.onPrediction(resolved)
        controller.onPrediction(next)
        waits.single().complete(Unit)

        assertEquals("next", controller.events.value.prediction?.id)
    }

    @Test
    fun onlyWhatPubSubBringsCountsAsLiveNotWhatTheLoadFound() {
        val answer = CompletableDeferred<ChannelEvents?>()
        val controller = ChannelEventsController(CoroutineScope(Dispatchers.Unconfined), fetch = { answer.await() })
        val poll = ChannelPollParser.parseFrame(pollFrame("POLL_UPDATE", "ACTIVE"), NOW)!!
        val prediction = ChannelPredictionParser.parseFrame(predictionFrame("ACTIVE"), NOW)!!

        controller.load("117474239")
        answer.complete(ChannelEvents(poll = poll))
        controller.onPrediction(prediction)

        assertEquals(setOf(prediction.id), controller.events.value.liveIds)
    }

    @Test
    fun anEndedHypeTrainHidesOnItsOwnTimerNotTheApproachOne() {
        val waits = mutableListOf<CompletableDeferred<Unit>>()
        val controller = ChannelEventsController(
            scope = CoroutineScope(Dispatchers.Unconfined),
            fetch = { null },
            wait = { CompletableDeferred<Unit>().also(waits::add).await() },
            now = { NOW },
        )
        val train = ChannelHypeTrain("t1", HypeTrainPhase.Active, 2, 900, 1800, NOW + 60_000L, null, emptyList(), emptyList())

        controller.onHypeTrain(HypeTrainEvent.Approaching(1, NOW + 30_000L, emptyList()))
        controller.onHypeTrain(HypeTrainEvent.Execution(train))
        assertEquals(train, controller.events.value.hypeTrain)
        waits.first().complete(Unit)
        assertEquals(train, controller.events.value.hypeTrain)

        controller.onHypeTrain(HypeTrainEvent.End("t1"))
        waits.last().complete(Unit)
        assertNull(controller.events.value.hypeTrain)
    }

    private fun pollFrame(type: String, status: String, votes: List<Int> = listOf(0, 0, 0, 0)): String {
        val choices = votes.mapIndexed { index, count ->
            """{"choice_id":"c${index + 1}","title":"${index + 1}","votes":{"total":$count}}"""
        }.joinToString(",")
        val message = """{"type":"$type","data":{"poll":{"poll_id":"5fc45bdd","title":"Тест","duration_seconds":600,
            "status":"$status","choices":[$choices],"votes":{"total":${votes.sum()}},
            "remaining_duration_milliseconds":222502}}}"""
        return frame("polls.117474239", message)
    }

    private fun predictionFrame(status: String, winner: String? = null): String {
        val winning = winner?.let { "\"$it\"" } ?: "null"
        val message = """{"type":"event-updated","data":{"timestamp":"2026-09-29T15:28:43Z","event":{
            "id":"a79cb891","created_at":"2026-09-29T15:28:33Z","prediction_window_seconds":1800,
            "status":"$status","title":"Тест","winning_outcome_id":$winning,"outcomes":[
            {"id":"o1","title":"Да","total_points":0,"total_users":0},
            {"id":"o2","title":"Нет","total_points":0,"total_users":0},
            {"id":"o3","title":"Другое","total_points":10,"total_users":1}]}}}"""
        return frame("predictions-channel-v1.117474239", message)
    }

    private fun frame(topic: String, message: String): String = JSONObject()
        .put("type", "MESSAGE")
        .put("data", JSONObject().put("topic", topic).put("message", message))
        .toString()

    private companion object {
        const val NOW = 1_000_000L
    }
}
