package name.alexwayfer.customtv.telegram

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TelegramLoginErrorTest {
    @Test
    fun aWrongPasswordHashIsAWrongPassword() {
        assertEquals(TelegramLoginError.WrongPassword, telegramLoginError("PASSWORD_HASH_INVALID"))
    }

    @Test
    fun anInvalidOrExpiredCodeIsExplained() {
        assertEquals(TelegramLoginError.WrongCode, telegramLoginError("PHONE_CODE_INVALID"))
        assertEquals(TelegramLoginError.CodeExpired, telegramLoginError("PHONE_CODE_EXPIRED"))
    }

    @Test
    fun anInvalidOrBannedPhoneIsExplained() {
        assertEquals(TelegramLoginError.InvalidPhone, telegramLoginError("PHONE_NUMBER_INVALID"))
        assertEquals(TelegramLoginError.BannedPhone, telegramLoginError("PHONE_NUMBER_BANNED"))
    }

    @Test
    fun aFloodLimitOrRetryDelayIsTooManyAttempts() {
        assertEquals(TelegramLoginError.TooManyAttempts, telegramLoginError("PHONE_NUMBER_FLOOD"))
        assertEquals(TelegramLoginError.TooManyAttempts, telegramLoginError("Too Many Requests: retry after 42"))
    }

    @Test
    fun anUnknownMessageIsShownAsIs() {
        assertNull(telegramLoginError("AUTH_KEY_UNREGISTERED"))
    }

    @Test
    fun aLoginFieldShowsTheErrorUnderItAndOtherStepsBelowTheSection() {
        assertTrue(telegramErrorUnderField(TelegramLoginStep.Password))
        assertTrue(telegramErrorUnderField(TelegramLoginStep.Code))
        assertFalse(telegramErrorUnderField(TelegramLoginStep.Ready))
        assertFalse(telegramErrorUnderField(TelegramLoginStep.Unsupported))
    }
}
