package name.alexwayfer.customtv.chat

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EmoteEffectsTest {
    @Test
    fun plainEmoteDoesNotChangeLook() {
        assertFalse(EmoteEffects().changesLook)
    }

    @Test
    fun removedSpaceOnlyMovesTheEmote() {
        assertFalse(EmoteEffects(removeSpaceBefore = true).changesLook)
    }

    @Test
    fun transformChangesLook() {
        assertTrue(EmoteEffects(flipX = true).changesLook)
        assertTrue(EmoteEffects(widthMultiplier = 2f).changesLook)
        assertTrue(EmoteEffects(rotationDegrees = 90f).changesLook)
    }

    @Test
    fun colorAndMotionChangeLook() {
        assertTrue(EmoteEffects(cursed = true).changesLook)
        assertTrue(EmoteEffects(rainbow = true, removeSpaceBefore = true).changesLook)
        assertTrue(EmoteEffects(bounce = true).changesLook)
    }
}
