package name.alexwayfer.customtv.telegram

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TelegramPhoneTest {
    @Test
    fun formattingCharactersGo() {
        assertEquals("+79161234567", telegramPhoneNumber("+7 (916) 123-45-67"))
    }

    @Test
    fun aNumberWithoutAPlusGetsOne() {
        assertEquals("+79161234567", telegramPhoneNumber("79161234567"))
    }

    @Test
    fun noDigitsIsNoNumber() {
        assertNull(telegramPhoneNumber(" + - "))
        assertNull(telegramPhoneNumber(""))
    }
}
