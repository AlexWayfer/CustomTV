package name.alexwayfer.customtv.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EventSubSessionsTest {
    @Test
    fun answerForTheOpenSessionIsCurrent() {
        assertFalse(eventSubAnswerOutdated("a", "a"))
    }

    @Test
    fun answerAfterTheSocketClosedIsOutdated() {
        assertTrue(eventSubAnswerOutdated("a", null))
    }

    @Test
    fun answerAfterReconnectToANewSessionIsOutdated() {
        assertTrue(eventSubAnswerOutdated("a", "b"))
    }
}
