package name.alexwayfer.customtv.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlayerStateWaitTest {
    @Test
    fun theAnswerIsLoggedAsThePageGaveIt() {
        val wait = PlayerStateWait()

        assertEquals(
            """Player state before the reload: {"ready":1,"state":"Buffering"}""",
            wait.finish("""{"ready":1,"state":"Buffering"}"""),
        )
    }

    @Test
    fun aTimeoutBeforeTheAnswerSaysThePageDidNotAnswer() {
        val wait = PlayerStateWait()

        assertEquals("Player state before the reload: no answer", wait.finish(null))
    }

    @Test
    fun aPageThatGaveNothingSaysSo() {
        val wait = PlayerStateWait()

        assertEquals("Player state before the reload: none", wait.finish("null"))
    }

    @Test
    fun theAnswerAfterTheTimeoutIsDropped() {
        val wait = PlayerStateWait()
        wait.finish(null)

        assertNull(wait.finish("""{"ready":4}"""))
    }

    @Test
    fun theTimeoutAfterTheAnswerIsDropped() {
        val wait = PlayerStateWait()
        wait.finish("""{"ready":4}""")

        assertNull(wait.finish(null))
    }
}
