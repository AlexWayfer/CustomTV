package name.alexwayfer.customtv.telegram

import kotlinx.coroutines.suspendCancellableCoroutine
import org.drinkless.tdlib.Client
import org.drinkless.tdlib.TdApi
import kotlin.coroutines.resume

/** TDLib answers a failed request with [TdApi.Error], not an exception. */
internal suspend fun Client.request(function: TdApi.Function<*>): TdApi.Object =
    suspendCancellableCoroutine { cont ->
        send(function) { result ->
            if (cont.isActive) cont.resume(result)
        }
    }

internal fun telegramAuthSignal(state: TdApi.AuthorizationState): TelegramAuthSignal = when (state) {
    is TdApi.AuthorizationStateWaitTdlibParameters -> TelegramAuthSignal.WaitTdlibParameters
    is TdApi.AuthorizationStateWaitPhoneNumber -> TelegramAuthSignal.WaitPhoneNumber
    is TdApi.AuthorizationStateWaitCode -> TelegramAuthSignal.WaitCode
    is TdApi.AuthorizationStateWaitPassword -> TelegramAuthSignal.WaitPassword
    is TdApi.AuthorizationStateWaitRegistration -> TelegramAuthSignal.WaitRegistration
    is TdApi.AuthorizationStateWaitEmailAddress -> TelegramAuthSignal.WaitEmailAddress
    is TdApi.AuthorizationStateWaitEmailCode -> TelegramAuthSignal.WaitEmailCode
    is TdApi.AuthorizationStateWaitOtherDeviceConfirmation -> TelegramAuthSignal.WaitOtherDevice
    is TdApi.AuthorizationStateWaitPremiumPurchase -> TelegramAuthSignal.WaitPremiumPurchase
    is TdApi.AuthorizationStateReady -> TelegramAuthSignal.Ready
    is TdApi.AuthorizationStateLoggingOut -> TelegramAuthSignal.LoggingOut
    is TdApi.AuthorizationStateClosing -> TelegramAuthSignal.Closing
    is TdApi.AuthorizationStateClosed -> TelegramAuthSignal.Closed
    else -> TelegramAuthSignal.Unknown
}
