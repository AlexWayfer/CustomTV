package name.alexwayfer.customtv.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EventSubSubscriptionsTest {
    @Test
    fun forbiddenModeratorSubscriptionMeansNotAModeratorAndKeepsTheSocket() {
        assertEquals(EventSubSubscribeOutcome.NotModerator, eventSubSubscribeOutcome(403, EventSubCondition.Moderator, "a", "a"))
    }

    @Test
    fun forbiddenStreamStatusSubscriptionIsARejectedLogin() {
        assertEquals(EventSubSubscribeOutcome.LoginRejected, eventSubSubscribeOutcome(403, EventSubCondition.Broadcaster, "a", "a"))
    }

    @Test
    fun forbiddenOwnMessageSubscriptionIsRejectedWithoutClosingTheSocket() {
        assertEquals(EventSubSubscribeOutcome.Rejected, eventSubSubscribeOutcome(403, EventSubCondition.User, "a", "a"))
    }

    @Test
    fun expiredLoginIsRejectedForEitherKind() {
        assertEquals(EventSubSubscribeOutcome.LoginRejected, eventSubSubscribeOutcome(401, EventSubCondition.Moderator, "a", "a"))
        assertEquals(EventSubSubscribeOutcome.LoginRejected, eventSubSubscribeOutcome(401, EventSubCondition.Broadcaster, "a", "a"))
    }

    @Test
    fun repeatedModeratorSubscriptionCountsAsSubscribed() {
        assertEquals(EventSubSubscribeOutcome.Subscribed, eventSubSubscribeOutcome(409, EventSubCondition.Moderator, "a", "a"))
        assertEquals(EventSubSubscribeOutcome.Rejected, eventSubSubscribeOutcome(409, EventSubCondition.Broadcaster, "a", "a"))
    }

    @Test
    fun answerForAClosedSessionIsOutdated() {
        assertEquals(EventSubSubscribeOutcome.Outdated, eventSubSubscribeOutcome(400, EventSubCondition.Broadcaster, "a", "b"))
        assertEquals(EventSubSubscribeOutcome.Outdated, eventSubSubscribeOutcome(400, EventSubCondition.Moderator, "a", null))
    }

    @Test
    fun serverErrorFailsWithoutRejecting() {
        assertEquals(EventSubSubscribeOutcome.Failed, eventSubSubscribeOutcome(503, EventSubCondition.Moderator, "a", "a"))
        assertEquals(EventSubSubscribeOutcome.Subscribed, eventSubSubscribeOutcome(202, EventSubCondition.Moderator, "a", "a"))
    }

    @Test
    fun readsTheSubscriptionTypeOfTheRequestedMessageKind() {
        val revocation = """{"metadata":{"message_type":"revocation"},"payload":{"subscription":{"type":"automod.message.hold","status":"authorization_revoked"}}}"""
        assertEquals("automod.message.hold", eventSubMessageSubscriptionType(revocation, "revocation"))
        assertNull(eventSubMessageSubscriptionType(revocation, "notification"))
        assertNull(eventSubMessageSubscriptionType("not json", "revocation"))
    }

    @Test
    fun aRefusalForTheSocketLimitIsToldApartFromOtherRefusals() {
        val limit = "number of websocket transports limit exceeded"
        assertEquals(EventSubSubscribeOutcome.TransportLimit, eventSubSubscribeOutcome(429, EventSubCondition.Broadcaster, "a", "a", limit))
        assertEquals(EventSubSubscribeOutcome.Rejected, eventSubSubscribeOutcome(429, EventSubCondition.Broadcaster, "a", "a", "cost exceeded"))
        assertEquals(EventSubSubscribeOutcome.Rejected, eventSubSubscribeOutcome(400, EventSubCondition.Broadcaster, "a", "a", limit))
    }

    @Test
    fun aSocketLimitAnswerForAnEndedSessionIsOutdated() {
        assertEquals(
            EventSubSubscribeOutcome.Outdated,
            eventSubSubscribeOutcome(429, EventSubCondition.Broadcaster, "old", "new", "number of websocket transports limit exceeded"),
        )
    }

    @Test
    fun theWelcomeNamesTheKeepaliveTimeout() {
        val welcome = """{"metadata":{"message_type":"session_welcome"},"payload":{"session":{"id":"s","keepalive_timeout_seconds":10}}}"""
        assertEquals(10L, eventSubKeepaliveSeconds(welcome))
        assertNull(eventSubKeepaliveSeconds("""{"metadata":{"message_type":"session_keepalive"},"payload":{}}"""))
        assertNull(eventSubKeepaliveSeconds("""{"metadata":{"message_type":"session_welcome"},"payload":{"session":{"id":"s"}}}"""))
    }

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
