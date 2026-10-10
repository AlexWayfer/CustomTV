package name.alexwayfer.customtv.telegram

import name.alexwayfer.customtv.ui.UiText
import name.alexwayfer.customtv.update.AppRelease
import name.alexwayfer.customtv.update.AppUpdateDownload

internal enum class TelegramAuthSignal {
    WaitTdlibParameters,
    WaitPhoneNumber,
    WaitCode,
    WaitPassword,
    WaitRegistration,
    WaitEmailAddress,
    WaitEmailCode,
    WaitOtherDevice,
    WaitPremiumPurchase,
    Ready,
    LoggingOut,
    Closing,
    Closed,
    Unknown,
}

internal enum class TelegramLoginStep {
    Starting,
    Phone,
    Confirm,
    Code,
    Password,
    Ready,
    LoggingOut,
    Unsupported,
}

internal fun telegramLoginStep(signal: TelegramAuthSignal): TelegramLoginStep = when (signal) {
    // Telegram confirms a log out and TDLib deletes the local data before it closes: seconds.
    TelegramAuthSignal.LoggingOut -> TelegramLoginStep.LoggingOut
    TelegramAuthSignal.WaitTdlibParameters,
    TelegramAuthSignal.Closing,
    TelegramAuthSignal.Closed,
    -> TelegramLoginStep.Starting
    TelegramAuthSignal.WaitPhoneNumber -> TelegramLoginStep.Phone
    TelegramAuthSignal.WaitOtherDevice -> TelegramLoginStep.Confirm
    TelegramAuthSignal.WaitCode -> TelegramLoginStep.Code
    TelegramAuthSignal.WaitPassword -> TelegramLoginStep.Password
    TelegramAuthSignal.Ready -> TelegramLoginStep.Ready
    TelegramAuthSignal.WaitRegistration,
    TelegramAuthSignal.WaitEmailAddress,
    TelegramAuthSignal.WaitEmailCode,
    TelegramAuthSignal.WaitPremiumPurchase,
    TelegramAuthSignal.Unknown,
    -> TelegramLoginStep.Unsupported
}

internal sealed class TelegramHistoryRequest {
    data object ChatMissing : TelegramHistoryRequest()
    data object TopicMissing : TelegramHistoryRequest()
    data class Topic(val topicId: Int) : TelegramHistoryRequest()
    data object WholeChat : TelegramHistoryRequest()
}

internal fun telegramEditionGroupLink(
    premium: Boolean,
    openChatId: Long,
    openTopicId: Int,
    premiumChatId: Long,
    premiumTopicId: Int,
): String? = if (premium) {
    telegramSupergroupLink(premiumChatId, premiumTopicId)
} else {
    telegramSupergroupLink(openChatId, openTopicId)
}

internal fun telegramHistoryRequest(chatId: Long, forum: Boolean, topicId: Int): TelegramHistoryRequest = when {
    chatId == 0L -> TelegramHistoryRequest.ChatMissing
    !forum -> TelegramHistoryRequest.WholeChat
    topicId == 0 -> TelegramHistoryRequest.TopicMissing
    else -> TelegramHistoryRequest.Topic(topicId)
}

internal fun telegramSupergroupLink(chatId: Long, topicId: Int): String? {
    if (topicId <= 0 || chatId >= -1_000_000_000_000L) return null
    val channelId = -chatId - 1_000_000_000_000L
    return "https://t.me/c/$channelId/$topicId"
}

internal fun telegramChatUnavailable(code: Int, message: String): Boolean {
    if (code != 400) return false
    val text = message.lowercase().replace('_', ' ')
    return text.contains("not found") || text.contains("not accessible")
}

internal fun telegramAccountLabel(firstName: String, lastName: String, username: String?): String {
    val name = listOf(firstName, lastName).filter { it.isNotBlank() }.joinToString(" ")
    val handle = username?.trim()?.takeIf { it.isNotEmpty() }?.let { "@$it" }
    return listOfNotNull(name.takeIf { it.isNotEmpty() }, handle).joinToString(" ")
}

internal sealed class TelegramGroupState {
    data object Checking : TelegramGroupState()
    /** [update] is the newest posted release when it is newer than this build. */
    data class Open(val update: AppRelease?) : TelegramGroupState()
    data object Unavailable : TelegramGroupState()
    data class Failed(val message: UiText) : TelegramGroupState()
    data object NotConfigured : TelegramGroupState()
    data object TopicNotConfigured : TelegramGroupState()
}

internal data class TelegramUiState(
    val step: TelegramLoginStep = TelegramLoginStep.Starting,
    val passwordHint: String? = null,
    /** Set while Telegram waits for the login code. */
    val code: TelegramCodeInfo? = null,    val notice: String? = null,
    val registrationRejected: Boolean = false,
    /** The request Telegram is answering, so its button shows progress; null while none is out. */
    val request: TelegramRequest? = null,
    val accountLabel: String? = null,
    /** Only for a problem report, so the developer can answer; null without a public username. */
    val accountUsername: String? = null,
    val openGroup: TelegramGroupState = TelegramGroupState.Checking,
    val premiumGroup: TelegramGroupState = TelegramGroupState.Checking,
    val download: AppUpdateDownload = AppUpdateDownload.Idle,
) {
    /** While any request is out, the login buttons wait for it. */
    val busy: Boolean get() = request != null
}

/** A login request the viewer sent: the number, code, or password, or a new code. */
internal enum class TelegramRequest { Submit, Resend }
