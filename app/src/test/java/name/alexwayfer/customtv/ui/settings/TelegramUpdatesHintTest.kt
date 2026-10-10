package name.alexwayfer.customtv.ui.settings

import name.alexwayfer.customtv.telegram.TelegramLoginStep
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TelegramUpdatesHintTest {
    @Test
    fun theLoginFieldsExplainWhyToLogIn() {
        listOf(
            TelegramLoginStep.Phone,
            TelegramLoginStep.Confirm,
            TelegramLoginStep.Code,
            TelegramLoginStep.Password,
            TelegramLoginStep.Unsupported,
        ).forEach { step -> assertTrue(step.name, telegramUpdatesHintVisible(step)) }
    }

    @Test
    fun aLoggedInAccountHidesTheHint() {
        assertFalse(telegramUpdatesHintVisible(TelegramLoginStep.Ready))
    }

    @Test
    fun theHintDoesNotFlashWhileTheSessionOpensOrLogsOut() {
        assertFalse(telegramUpdatesHintVisible(TelegramLoginStep.Starting))
        assertFalse(telegramUpdatesHintVisible(TelegramLoginStep.LoggingOut))
    }
}
