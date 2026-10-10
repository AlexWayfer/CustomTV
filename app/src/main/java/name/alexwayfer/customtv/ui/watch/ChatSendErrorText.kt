package name.alexwayfer.customtv.ui.watch

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.data.ChatSendError

@Composable
internal fun chatSendErrorText(error: ChatSendError): String = when (error) {
    ChatSendError.LogIn -> stringResource(R.string.chat_send_log_in)
    ChatSendError.NotAllowed -> stringResource(R.string.chat_send_not_allowed)
    ChatSendError.TooLong -> stringResource(R.string.chat_send_too_long)
    ChatSendError.TooFast -> stringResource(R.string.chat_send_too_fast)
    is ChatSendError.Twitch -> error.message
    ChatSendError.Failed -> stringResource(R.string.chat_send_failed)
}
