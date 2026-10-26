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
    ChatSendError.SlowUsage -> stringResource(R.string.chat_mode_slow_usage)
    ChatSendError.FollowersUsage -> stringResource(R.string.chat_mode_followers_usage)
    ChatSendError.ChatModeFailed -> stringResource(R.string.chat_mode_change_failed)
    ChatSendError.ChatModesPremium -> stringResource(R.string.chat_modes_premium_hint)
    ChatSendError.ShoutoutUsage -> stringResource(R.string.chat_shoutout_usage)
    ChatSendError.ShoutoutOffline -> stringResource(R.string.chat_shoutout_offline)
    ChatSendError.ShoutoutTooOften -> stringResource(R.string.chat_shoutout_too_often)
    ChatSendError.ShoutoutNoChannel -> stringResource(R.string.chat_shoutout_no_channel)
    ChatSendError.ShoutoutOwnChannel -> stringResource(R.string.chat_shoutout_own_channel)
    ChatSendError.ShoutoutFailed -> stringResource(R.string.chat_shoutout_failed)
    ChatSendError.ShoutoutPremium -> stringResource(R.string.chat_shoutout_premium_hint)
}
