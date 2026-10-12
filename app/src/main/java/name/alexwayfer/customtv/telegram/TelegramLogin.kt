package name.alexwayfer.customtv.telegram

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import name.alexwayfer.customtv.diagnostics.AppLog
import org.drinkless.tdlib.Client
import org.drinkless.tdlib.TdApi

/** The login steps the user answers: the phone number, the code, the password, and a new code. */
internal class TelegramLogin(
    private val scope: CoroutineScope,
    private val ui: MutableStateFlow<TelegramUiState>,
    private val client: () -> Client?,
) {
    fun submitPhone(phone: String) = submit(
        phone,
        TelegramLoginStep.Phone,
        TelegramLoginStep.Confirm,
    ) { value ->
        TdApi.SetAuthenticationPhoneNumber(
            value,
            TdApi.PhoneNumberAuthenticationSettings(
                false,
                false,
                false,
                false,
                false,
                null,
                emptyArray(),
            ),
        )
    }

    fun submitCode(code: String) = submit(code, TelegramLoginStep.Code) { value ->
        TdApi.CheckAuthenticationCode(value)
    }

    fun submitPassword(password: String) = submit(password, TelegramLoginStep.Password) { value ->
        TdApi.CheckAuthenticationPassword(value)
    }

    /** Sends the code again the next way Telegram named; the new state brings where it went. */
    fun resendCode() {
        if (ui.value.step != TelegramLoginStep.Code || ui.value.code?.resendTo == null) return
        val current = client() ?: return
        scope.launch {
            ui.update { it.copy(request = TelegramRequest.Resend, notice = null) }
            val result = current.request(TdApi.ResendAuthenticationCode(TdApi.ResendCodeReasonUserRequest()))
            if (result is TdApi.Error) {
                AppLog.w(TELEGRAM_TAG, "code resend rejected ${result.code}")
                ui.update { it.copy(request = null, notice = result.message) }
            } else {
                ui.update { it.copy(request = null) }
            }
        }
    }

    private fun submit(
        raw: String,
        vararg allowed: TelegramLoginStep,
        function: (String) -> TdApi.Function<*>,
    ) {
        val value = raw.trim()
        if (value.isEmpty() || ui.value.step !in allowed) return
        val current = client() ?: return
        scope.launch {
            ui.update { it.copy(request = TelegramRequest.Submit, notice = null) }
            val result = current.request(function(value))
            if (result is TdApi.Error) {
                AppLog.w(TELEGRAM_TAG, "authentication rejected ${result.code}")
                ui.update { it.copy(request = null, notice = result.message) }
            } else {
                ui.update { it.copy(request = null) }
            }
        }
    }
}
