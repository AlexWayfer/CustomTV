package name.alexwayfer.customtv.telegram

import org.drinkless.tdlib.TdApi

/** Where Telegram sent a login code, so the user knows where to look for it. */
internal enum class TelegramCodeDelivery {
    TelegramApp,
    Sms,
    Call,
    Fragment,
}

/**
 * Null when the code went where this app cannot point to, such as Firebase, which only the official app
 * receives. A call, a flash call, and a missed call all reach the user as a call.
 */
internal fun telegramCodeDelivery(type: TdApi.AuthenticationCodeType?): TelegramCodeDelivery? = when (type) {
    is TdApi.AuthenticationCodeTypeTelegramMessage -> TelegramCodeDelivery.TelegramApp
    is TdApi.AuthenticationCodeTypeSms,
    is TdApi.AuthenticationCodeTypeSmsWord,
    is TdApi.AuthenticationCodeTypeSmsPhrase,
    -> TelegramCodeDelivery.Sms
    is TdApi.AuthenticationCodeTypeCall,
    is TdApi.AuthenticationCodeTypeFlashCall,
    is TdApi.AuthenticationCodeTypeMissedCall,
    -> TelegramCodeDelivery.Call
    is TdApi.AuthenticationCodeTypeFragment -> TelegramCodeDelivery.Fragment
    else -> null
}

/** Where the code went, and where Send again would send it once [resendAfterSeconds] pass. */
internal data class TelegramCodeInfo(
    val sentTo: TelegramCodeDelivery?,
    val resendTo: TelegramCodeDelivery?,
    val resendAfterSeconds: Int,
)

internal fun telegramCodeInfo(info: TdApi.AuthenticationCodeInfo): TelegramCodeInfo = TelegramCodeInfo(
    sentTo = telegramCodeDelivery(info.type),
    resendTo = telegramCodeDelivery(info.nextType),
    resendAfterSeconds = info.timeout.coerceAtLeast(0),
)
