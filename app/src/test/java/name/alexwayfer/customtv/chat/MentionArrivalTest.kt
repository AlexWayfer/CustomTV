package name.alexwayfer.customtv.chat

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MentionArrivalTest {
    @Test
    fun theFirstChatFrameIsTheBaselineNotAnArrival() {
        assertTrue(mentionBaselineResets(baselineGeneration = null, timelineGeneration = 0))
    }

    @Test
    fun messagesArrivingWhilePlayingKeepTheBaseline() {
        assertFalse(mentionBaselineResets(baselineGeneration = 2, timelineGeneration = 2))
    }

    @Test
    fun aSeekOrLoadedReplayMakesTheShownChatTheNewBaseline() {
        assertTrue(mentionBaselineResets(baselineGeneration = 2, timelineGeneration = 3))
    }

    @Test
    fun onlyANewlyArrivedMentionAsksForFeedback() {
        val seen = mention("old")
        val fresh = mention("new")

        assertFalse(newMentionArrived(setOf("old"), listOf(seen), "alex", "Alex"))
        assertTrue(newMentionArrived(setOf("old"), listOf(seen, fresh), "alex", "Alex"))
    }

    @Test
    fun anAppOnScreenOrInPictureInPictureAlerts() {
        assertTrue(mentionAlertAudible(appStarted = true, stoppedByLock = false, screenUnlocked = true))
    }

    @Test
    fun aScreenLockedOverTheAppAlerts() {
        assertTrue(mentionAlertAudible(appStarted = false, stoppedByLock = true, screenUnlocked = false))
    }

    @Test
    fun anAppInTheBackgroundOfAnUnlockedPhoneStaysSilent() {
        assertFalse(mentionAlertAudible(appStarted = false, stoppedByLock = false, screenUnlocked = true))
    }

    @Test
    fun aScreenLockedOverAnotherAppStaysSilent() {
        assertFalse(mentionAlertAudible(appStarted = false, stoppedByLock = false, screenUnlocked = false))
    }

    @Test
    fun unlockingIntoAnotherAppAfterALockSilencesTheAlerts() {
        assertFalse(mentionAlertAudible(appStarted = false, stoppedByLock = true, screenUnlocked = true))
    }

    private fun mention(id: String) = testChatMessage(
        id,
        "hey alex",
        login = id,
        color = Color.Unspecified,
        parts = emptyList(),
        timestampMillis = 0L,
    )
}
