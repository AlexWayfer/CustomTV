package name.alexwayfer.customtv.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EventSubSubscriptionsTest {
    @Test
    fun aSubscriptionBodyNamesTheBroadcasterAndTheSocketSession() {
        val body = eventSubSubscriptionBody(
            EventSubRequest("token", "123", userId = "456", "session", EventSubType("stream.online", "1")),
        )
        assertEquals("stream.online", body.getString("type"))
        assertEquals("1", body.getString("version"))
        assertEquals("123", body.getJSONObject("condition").getString("broadcaster_user_id"))
        assertEquals(false, body.getJSONObject("condition").has("user_id"))
        assertEquals("websocket", body.getJSONObject("transport").getString("method"))
        assertEquals("session", body.getJSONObject("transport").getString("session_id"))
    }

    @Test
    fun aUserSubscriptionBodyNamesTheUserToo() {
        val body = eventSubSubscriptionBody(
            EventSubRequest("token", "123", userId = "456", "session", OWN_MESSAGE_HOLD_EVENTSUB_TYPES.first()),
        )
        assertEquals("456", body.getJSONObject("condition").getString("user_id"))
    }

    @Test
    fun aReconnectSubscribesWithTheFreshTokenInsteadOfTheExpiredSavedOne() {
        assertEquals("fresh", eventSubToken(" fresh ", "saved"))
    }

    @Test
    fun withoutAFreshTokenTheSavedOneIsUsed() {
        assertEquals("saved", eventSubToken(null, "saved"))
        assertEquals("saved", eventSubToken("  ", "saved"))
        assertNull(eventSubToken(null, null))
    }

    @Test
    fun forbiddenModeratorSubscriptionMeansNotAModeratorAndKeepsTheSocket() {
        assertEquals(EventSubSubscribeOutcome.NotModerator, outcome(403, EventSubCondition.Moderator))
    }

    @Test
    fun forbiddenStreamStatusSubscriptionIsARejectedLogin() {
        assertEquals(EventSubSubscribeOutcome.LoginRejected, outcome(403, EventSubCondition.Broadcaster))
    }

    @Test
    fun forbiddenOwnMessageSubscriptionIsRejectedWithoutClosingTheSocket() {
        assertEquals(EventSubSubscribeOutcome.Rejected, outcome(403, EventSubCondition.User))
    }

    @Test
    fun expiredLoginIsRejectedForEitherKind() {
        assertEquals(EventSubSubscribeOutcome.LoginRejected, outcome(401, EventSubCondition.Moderator))
        assertEquals(EventSubSubscribeOutcome.LoginRejected, outcome(401, EventSubCondition.Broadcaster))
    }

    @Test
    fun repeatedModeratorSubscriptionCountsAsSubscribed() {
        assertEquals(EventSubSubscribeOutcome.Subscribed, outcome(409, EventSubCondition.Moderator))
        assertEquals(EventSubSubscribeOutcome.Rejected, outcome(409, EventSubCondition.Broadcaster))
    }

    @Test
    fun answerForAClosedSessionIsOutdated() {
        assertEquals(
            EventSubSubscribeOutcome.Outdated,
            outcome(400, EventSubCondition.Broadcaster, currentSession = "b"),
        )
        assertEquals(
            EventSubSubscribeOutcome.Outdated,
            outcome(400, EventSubCondition.Moderator, currentSession = null),
        )
    }

    @Test
    fun serverErrorFailsWithoutRejecting() {
        assertEquals(EventSubSubscribeOutcome.Failed, outcome(503, EventSubCondition.Moderator))
        assertEquals(EventSubSubscribeOutcome.Subscribed, outcome(202, EventSubCondition.Moderator))
    }

    @Test
    fun readsTheSubscriptionTypeOfTheRequestedMessageKind() {
        val revocation = """
            {
              "metadata":{"message_type":"revocation"},
              "payload":{"subscription":{"type":"automod.message.hold","status":"authorization_revoked"}}
            }
        """.trimIndent()
        assertEquals("automod.message.hold", eventSubMessageSubscriptionType(revocation, "revocation"))
        assertNull(eventSubMessageSubscriptionType(revocation, "notification"))
        assertNull(eventSubMessageSubscriptionType("not json", "revocation"))
    }

    @Test
    fun aRefusalForTheSocketLimitIsToldApartFromOtherRefusals() {
        val limit = "number of websocket transports limit exceeded"
        assertEquals(
            EventSubSubscribeOutcome.TransportLimit,
            outcome(429, EventSubCondition.Broadcaster, message = limit),
        )
        assertEquals(
            EventSubSubscribeOutcome.Rejected,
            outcome(429, EventSubCondition.Broadcaster, message = "cost exceeded"),
        )
        assertEquals(EventSubSubscribeOutcome.Rejected, outcome(400, EventSubCondition.Broadcaster, message = limit))
    }

    @Test
    fun aSocketLimitAnswerForAnEndedSessionIsOutdated() {
        val limit = "number of websocket transports limit exceeded"
        assertEquals(
            EventSubSubscribeOutcome.Outdated,
            eventSubSubscribeOutcome(429, EventSubCondition.Broadcaster, "old", "new", limit),
        )
    }

    @Test
    fun theWelcomeNamesTheKeepaliveTimeout() {
        val welcome = """
            {
              "metadata":{"message_type":"session_welcome"},
              "payload":{"session":{"id":"s","keepalive_timeout_seconds":10}}
            }
        """.trimIndent()
        assertEquals(10L, eventSubKeepaliveSeconds(welcome))
        assertNull(eventSubKeepaliveSeconds("""{"metadata":{"message_type":"session_keepalive"},"payload":{}}"""))
        val withoutTimeout = """{"metadata":{"message_type":"session_welcome"},"payload":{"session":{"id":"s"}}}"""
        assertNull(eventSubKeepaliveSeconds(withoutTimeout))
    }

    /** [eventSubSubscribeOutcome] for an answer to the session that is still current. */
    private fun outcome(
        httpCode: Int,
        condition: EventSubCondition,
        currentSession: String? = "a",
        message: String = "",
    ) = eventSubSubscribeOutcome(httpCode, condition, "a", currentSession, message)

    @Test
    fun aSocketLimitRetryWaitsAKeepaliveTimeoutAndThenTwiceAsLongEachTime() {
        assertEquals(10L, eventSubTransportRetrySeconds(10, attempt = 0))
        assertEquals(20L, eventSubTransportRetrySeconds(10, attempt = 1))
        assertEquals(40L, eventSubTransportRetrySeconds(10, attempt = 2))
    }

    @Test
    fun withoutAKeepaliveTimeoutTheRetryUsesTwitchsDefault() {
        assertEquals(10L, eventSubTransportRetrySeconds(null, attempt = 0))
        assertEquals(10L, eventSubTransportRetrySeconds(0, attempt = 0))
    }

    @Test
    fun aSocketCountsAsDeadAfterItsKeepaliveTimeoutAndAMargin() {
        assertEquals(65L, eventSubSilenceTimeoutSeconds(60))
        assertEquals(15L, eventSubSilenceTimeoutSeconds(10))
    }

    @Test
    fun beforeTheWelcomeTheSilenceTimeoutUsesTwitchsDefault() {
        assertEquals(15L, eventSubSilenceTimeoutSeconds(null))
        assertEquals(15L, eventSubSilenceTimeoutSeconds(0))
    }
}
