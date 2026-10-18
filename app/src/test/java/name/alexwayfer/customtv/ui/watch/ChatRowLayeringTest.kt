package name.alexwayfer.customtv.ui.watch

import name.alexwayfer.customtv.chat.EmoteEffects
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatRowLayeringTest {
    @Test
    fun aWideEmoteTurnedOnItsSideSpillsOutOfItsLine() {
        assertTrue(emoteSpillsOutOfLine(aspect = 4f, effects = EmoteEffects(rotationDegrees = 90f)))
        assertTrue(emoteSpillsOutOfLine(aspect = 4f, effects = EmoteEffects(rotationDegrees = -90f)))
    }

    @Test
    fun aSquareEmoteTurnedOnItsSideStaysInItsLine() {
        assertFalse(emoteSpillsOutOfLine(aspect = 1f, effects = EmoteEffects(rotationDegrees = 90f)))
    }

    @Test
    fun aWideEmoteTurnedUpsideDownStaysInItsLine() {
        assertFalse(emoteSpillsOutOfLine(aspect = 4f, effects = EmoteEffects(rotationDegrees = 180f)))
        assertFalse(emoteSpillsOutOfLine(aspect = 4f, effects = EmoteEffects(flipY = true)))
    }

    @Test
    fun aWidenedSquareEmoteTurnedOnItsSideSpills() {
        assertTrue(
            emoteSpillsOutOfLine(aspect = 1f, effects = EmoteEffects(widthMultiplier = 2f, rotationDegrees = 90f)),
        )
    }

    @Test
    fun anUnturnedWideEmoteStaysInItsLine() {
        assertFalse(emoteSpillsOutOfLine(aspect = 4f, effects = EmoteEffects()))
    }
}
