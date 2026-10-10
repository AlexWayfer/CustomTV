package name.alexwayfer.customtv.telegram

import org.drinkless.tdlib.TdApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TelegramCodeTest {
    @Test
    fun aTelegramMessageGoesToTheApp() {
        assertEquals(
            TelegramCodeDelivery.TelegramApp,
            telegramCodeDelivery(TdApi.AuthenticationCodeTypeTelegramMessage(5)),
        )
    }

    @Test
    fun everySmsKindIsAnSms() {
        assertEquals(TelegramCodeDelivery.Sms, telegramCodeDelivery(TdApi.AuthenticationCodeTypeSms(5)))
        assertEquals(TelegramCodeDelivery.Sms, telegramCodeDelivery(TdApi.AuthenticationCodeTypeSmsWord("")))
        assertEquals(TelegramCodeDelivery.Sms, telegramCodeDelivery(TdApi.AuthenticationCodeTypeSmsPhrase("")))
    }

    @Test
    fun everyCallKindIsACall() {
        assertEquals(TelegramCodeDelivery.Call, telegramCodeDelivery(TdApi.AuthenticationCodeTypeCall(5)))
        assertEquals(TelegramCodeDelivery.Call, telegramCodeDelivery(TdApi.AuthenticationCodeTypeFlashCall("")))
        assertEquals(TelegramCodeDelivery.Call, telegramCodeDelivery(TdApi.AuthenticationCodeTypeMissedCall("", 4)))
    }

    @Test
    fun firebaseAndNothingHaveNoHint() {
        assertNull(telegramCodeDelivery(TdApi.AuthenticationCodeTypeFirebaseAndroid()))
        assertNull(telegramCodeDelivery(null))
    }

    @Test
    fun noNextTypeMeansNoResend() {
        val info = telegramCodeInfo(
            TdApi.AuthenticationCodeInfo("", TdApi.AuthenticationCodeTypeTelegramMessage(5), null, 0),
        )
        assertEquals(TelegramCodeInfo(TelegramCodeDelivery.TelegramApp, null, 0), info)
    }

    @Test
    fun aNextTypeOffersResendAfterTheTimeout() {
        val info = telegramCodeInfo(
            TdApi.AuthenticationCodeInfo(
                "",
                TdApi.AuthenticationCodeTypeTelegramMessage(5),
                TdApi.AuthenticationCodeTypeSms(5),
                60,
            ),
        )
        assertEquals(TelegramCodeInfo(TelegramCodeDelivery.TelegramApp, TelegramCodeDelivery.Sms, 60), info)
    }
}
