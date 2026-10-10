package name.alexwayfer.customtv.ui.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MentionSoundTest {
    private val defaultUri = "content://settings/system/notification_sound"
    private val chosenUri = "content://media/internal/audio/media/12"

    @Test
    fun aBlankStoredSoundFollowsTheDefaultNotification() {
        assertEquals(defaultUri, mentionSoundUri("", defaultUri))
    }

    @Test
    fun pickingTheDefaultStoresBlankSoItKeepsFollowing() {
        assertEquals("", storedMentionSound(defaultUri, defaultUri))
    }

    @Test
    fun pickingASpecificSoundStoresThatUri() {
        assertEquals(chosenUri, storedMentionSound(chosenUri, defaultUri))
        assertEquals(chosenUri, mentionSoundUri(chosenUri, defaultUri))
    }

    @Test
    fun aCancelledPickLeavesTheStoredSoundUnchanged() {
        assertNull(storedMentionSound(null, defaultUri))
        assertNull(storedMentionSound("", defaultUri))
    }

    @Test
    fun playbackContinuesWhileTheSoundIsStillPlaying() {
        assertTrue(mentionSoundStillPlaying(elapsedMs = 1_000, playing = true))
    }

    @Test
    fun playbackStopsWhenTheSoundFinishesOrReachesFourSeconds() {
        assertFalse(mentionSoundStillPlaying(elapsedMs = 500, playing = false))
        assertFalse(mentionSoundStillPlaying(elapsedMs = MentionSoundLimitMs, playing = true))
    }

    @Test
    fun pickingTheSameSoundAgainKeepsItsPermission() {
        assertNull(mentionSoundPermissionToRelease(chosenUri, chosenUri))
        assertNull(mentionSoundPermissionToRelease("", chosenUri))
    }

    @Test
    fun pickingAnotherSoundReleasesThePreviousPermission() {
        assertEquals(
            chosenUri,
            mentionSoundPermissionToRelease(chosenUri, "content://media/internal/audio/media/9"),
        )
        assertEquals(chosenUri, mentionSoundPermissionToRelease(chosenUri, ""))
    }
}
