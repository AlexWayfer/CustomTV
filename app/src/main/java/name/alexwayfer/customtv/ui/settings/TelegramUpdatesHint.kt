package name.alexwayfer.customtv.ui.settings

import name.alexwayfer.customtv.telegram.TelegramLoginStep

/**
 * The line on why to log in to Telegram shows with the login fields. Once logged in, the account and the version
 * below say it, and the line would only push them down.
 */
internal fun telegramUpdatesHintVisible(step: TelegramLoginStep): Boolean = when (step) {
    TelegramLoginStep.Phone,
    TelegramLoginStep.Confirm,
    TelegramLoginStep.Code,
    TelegramLoginStep.Password,
    TelegramLoginStep.Unsupported,
    -> true
    TelegramLoginStep.Starting,
    TelegramLoginStep.Ready,
    TelegramLoginStep.LoggingOut,
    -> false
}
