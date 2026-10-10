package name.alexwayfer.customtv.ui.watch

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StreamInfoRevealTest {
    private val known = StreamInfoSnapshot(login = "streamer", title = "Morning run", categoryName = "Elden Ring")

    @Test
    fun changedTitleOnSameChannelReveals() {
        assertTrue(streamInfoChangeRevealsHeader(known, known.copy(title = "Evening run")))
    }

    @Test
    fun changedCategoryOnSameChannelReveals() {
        assertTrue(streamInfoChangeRevealsHeader(known, known.copy(categoryName = "Just Chatting")))
    }

    @Test
    fun loginCaseDoesNotCountAsAnotherChannel() {
        assertTrue(streamInfoChangeRevealsHeader(known, known.copy(login = "Streamer", title = "Evening run")))
    }

    @Test
    fun unchangedInfoDoesNotReveal() {
        assertFalse(streamInfoChangeRevealsHeader(known, known.copy()))
    }

    @Test
    fun firstLoadFromEmptyDoesNotReveal() {
        val empty = StreamInfoSnapshot(login = "streamer", title = null, categoryName = null)
        assertFalse(streamInfoChangeRevealsHeader(empty, known))
    }

    @Test
    fun infoThatGoesAwayDoesNotReveal() {
        assertFalse(streamInfoChangeRevealsHeader(known, known.copy(title = null, categoryName = null)))
    }

    @Test
    fun switchToAnotherChannelDoesNotReveal() {
        assertFalse(
            streamInfoChangeRevealsHeader(
                known,
                StreamInfoSnapshot(login = "other", title = "Other title", categoryName = "Chess"),
            ),
        )
    }
}
