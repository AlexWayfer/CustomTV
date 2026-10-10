package name.alexwayfer.customtv.ui.notifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationPromptRulesTest {
    @Test
    fun homeShowsTheCardWhileNotificationsAreOffAndUnanswered() {
        assertTrue(homeNotificationPromptVisible(notificationsEnabled = false, dismissed = false))
    }

    @Test
    fun homeHidesTheCardOnceAnswered() {
        assertFalse(homeNotificationPromptVisible(notificationsEnabled = false, dismissed = true))
    }

    @Test
    fun homeHidesTheCardWhenNotificationsAreOn() {
        assertFalse(homeNotificationPromptVisible(notificationsEnabled = true, dismissed = false))
    }

    @Test
    fun homeWaitsForTheStoredAnswerBeforeShowingTheCard() {
        assertFalse(homeNotificationPromptVisible(notificationsEnabled = false, dismissed = null))
    }

    @Test
    fun notificationsTurnedOnClearTheAnswer() {
        assertTrue(notificationPromptAnswerResets(notificationsEnabled = true, dismissed = true))
    }

    @Test
    fun notificationsStillOffKeepTheAnswer() {
        assertFalse(notificationPromptAnswerResets(notificationsEnabled = false, dismissed = true))
    }

    @Test
    fun anUnansweredCardHasNothingToClear() {
        assertFalse(notificationPromptAnswerResets(notificationsEnabled = true, dismissed = false))
    }

    @Test
    fun theFirstAskFromHomeRequestsThePermission() {
        assertEquals(
            NotificationPromptAction.RequestPermission,
            notificationPromptAction(permissionNeeded = true, firstAsk = true, showRationale = false),
        )
    }

    @Test
    fun aRetryAfterOneRefusalRequestsThePermissionAgain() {
        assertEquals(
            NotificationPromptAction.RequestPermission,
            notificationPromptAction(permissionNeeded = true, firstAsk = false, showRationale = true),
        )
    }

    @Test
    fun aRetryWhenTheDialogNoLongerShowsOpensSettings() {
        assertEquals(
            NotificationPromptAction.OpenSettings,
            notificationPromptAction(permissionNeeded = true, firstAsk = false, showRationale = false),
        )
    }

    @Test
    fun notificationsTurnedOffWithTheGrantedPermissionOpenSettings() {
        assertEquals(
            NotificationPromptAction.OpenSettings,
            notificationPromptAction(permissionNeeded = false, firstAsk = true, showRationale = false),
        )
    }

    @Test
    fun aFirstAskRefusedInTheDialogAnswersTheCard() {
        assertEquals(
            NotificationPermissionRefusal.Refused,
            notificationPermissionRefusal(firstAsk = true, showRationale = true),
        )
    }

    @Test
    fun aFirstAskRefusedWithoutTheDialogNeedsTheSystemSettings() {
        assertEquals(
            NotificationPermissionRefusal.Blocked,
            notificationPermissionRefusal(firstAsk = true, showRationale = false),
        )
    }

    @Test
    fun aRetryRefusedInTheDialogStaysARefusal() {
        assertEquals(
            NotificationPermissionRefusal.Refused,
            notificationPermissionRefusal(firstAsk = false, showRationale = false),
        )
    }
}
