package name.alexwayfer.customtv.telegram

import name.alexwayfer.customtv.BuildConfig
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.diagnostics.AppLog
import name.alexwayfer.customtv.ui.UiText
import name.alexwayfer.customtv.update.AppRelease
import name.alexwayfer.customtv.update.AppVersion
import name.alexwayfer.customtv.update.appRelease
import name.alexwayfer.customtv.update.installedDownloads
import name.alexwayfer.customtv.update.newestRelease
import org.drinkless.tdlib.Client
import org.drinkless.tdlib.TdApi

private const val POST_PAGE = 10
private val UNEXPECTED_RESPONSE = UiText.Resource(R.string.telegram_unexpected_response)

/** Reads a release group through TDLib: its chat or topic, and the releases posted there. */
internal object TelegramGroups {
    suspend fun load(source: Client, chatId: Long, topicId: Int): TelegramGroupState {
        AppLog.i(TELEGRAM_TAG, "check chat $chatId topic $topicId")
        if (chatId == 0L) return TelegramGroupState.NotConfigured
        return when (val opened = openChat(source, chatId, topicId)) {
            is OpenedChat.Ready -> loadOpenedGroup(source, opened.chat, topicId)
            is OpenedChat.Stopped -> opened.state
        }
    }

    /**
     * The free build only asks whether this account opens the Premium group, so Upgrade to Premium can
     * lead a buyer straight to its topic. Open means access; its releases are not read.
     */
    suspend fun premiumAccess(source: Client, chatId: Long, topicId: Int): TelegramGroupState {
        if (chatId == 0L) return TelegramGroupState.NotConfigured
        return when (val opened = openChat(source, chatId, topicId)) {
            is OpenedChat.Ready -> TelegramGroupState.Open(null)
            is OpenedChat.Stopped -> opened.state
        }
    }

    private suspend fun loadOpenedGroup(source: Client, chat: TdApi.Chat, topicId: Int): TelegramGroupState {
        return when (val request = telegramHistoryRequest(chat.id, chat.viewAsTopics, topicId)) {
            TelegramHistoryRequest.ChatMissing -> TelegramGroupState.NotConfigured
            TelegramHistoryRequest.TopicMissing -> TelegramGroupState.TopicNotConfigured
            TelegramHistoryRequest.WholeChat -> openGroup(
                source,
                loadReleases(source, TdApi.GetChatHistory(chat.id, 0L, 0, POST_PAGE, false)),
            )
            is TelegramHistoryRequest.Topic -> loadTopic(source, chat, request.topicId)
        }
    }

    private suspend fun openChat(source: Client, chatId: Long, topicId: Int): OpenedChat {
        val link = telegramSupergroupLink(chatId, topicId)
        val resolvedId = if (link == null) {
            chatId
        } else {
            when (val info = source.request(TdApi.GetMessageLinkInfo(link))) {
                is TdApi.Error -> {
                    AppLog.w(TELEGRAM_TAG, "chat $chatId failed ${info.code} ${info.message}")
                    return OpenedChat.Stopped(groupError(info.code, info.message))
                }
                is TdApi.MessageLinkInfo -> {
                    if (info.chatId == 0L) return OpenedChat.Stopped(TelegramGroupState.Unavailable)
                    AppLog.i(TELEGRAM_TAG, "check link ${info.chatId}")
                    info.chatId
                }
                else -> return OpenedChat.Stopped(TelegramGroupState.Failed(UNEXPECTED_RESPONSE))
            }
        }
        return when (val chat = source.request(TdApi.GetChat(resolvedId))) {
            is TdApi.Chat -> OpenedChat.Ready(chat)
            is TdApi.Error -> {
                AppLog.w(TELEGRAM_TAG, "chat $resolvedId failed ${chat.code} ${chat.message}")
                OpenedChat.Stopped(
                    if (link == null) {
                        groupError(chat.code, chat.message)
                    } else {
                        TelegramGroupState.Failed(UiText.Raw(chat.message))
                    },
                )
            }
            else -> OpenedChat.Stopped(TelegramGroupState.Failed(UNEXPECTED_RESPONSE))
        }
    }

    private fun groupError(code: Int, message: String): TelegramGroupState =
        if (telegramChatUnavailable(code, message)) {
            TelegramGroupState.Unavailable
        } else {
            TelegramGroupState.Failed(UiText.Raw(message))
        }

    private suspend fun loadTopic(source: Client, chat: TdApi.Chat, topicId: Int): TelegramGroupState {
        val topic = source.request(TdApi.GetForumTopic(chat.id, topicId))
        if (topic is TdApi.Error) {
            AppLog.w(TELEGRAM_TAG, "topic ${chat.id} $topicId failed ${topic.code} ${topic.message}")
            return TelegramGroupState.Failed(UiText.Raw(topic.message))
        }
        if (topic !is TdApi.ForumTopic) return TelegramGroupState.Failed(UNEXPECTED_RESPONSE)
        return openGroup(
            source,
            loadReleases(source, TdApi.GetForumTopicHistory(chat.id, topicId, 0L, 0, POST_PAGE)),
        )
    }

    /** Drops the downloaded files of releases already installed, then offers the newest one if newer. */
    private suspend fun openGroup(source: Client, releases: List<AppRelease>): TelegramGroupState.Open {
        val installed = AppVersion.parse(BuildConfig.VERSION_NAME) ?: AppVersion.ZERO
        for (release in installedDownloads(releases, installed)) {
            val result = source.request(TdApi.DeleteFile(release.fileId))
            if (result is TdApi.Error) AppLog.w(TELEGRAM_TAG, "delete file failed ${result.code}")
        }
        val newest = releases.maxByOrNull { it.version }
        AppLog.i(TELEGRAM_TAG, "releases ${releases.size}, newest ${newest?.version}, installed $installed")
        return TelegramGroupState.Open(newestRelease(releases, installed), newest?.version)
    }

    private suspend fun loadReleases(
        source: Client,
        request: TdApi.Function<TdApi.Messages>,
    ): List<AppRelease> {
        val result = source.request(request)
        if (result is TdApi.Error) {
            AppLog.w(TELEGRAM_TAG, "posts failed ${result.code}")
            return emptyList()
        }
        if (result !is TdApi.Messages) return emptyList()
        return result.messages?.filterNotNull()?.mapNotNull(::releaseOf).orEmpty()
    }

    private fun releaseOf(message: TdApi.Message): AppRelease? {
        val content = message.content as? TdApi.MessageDocument ?: return null
        val document = content.document ?: return null
        val file = document.document ?: return null
        return appRelease(
            fileId = file.id,
            fileName = document.fileName.orEmpty(),
            mimeType = document.mimeType.orEmpty(),
            downloadedPath = file.local?.takeIf { it.isDownloadingCompleted }?.path,
        )
    }

    private sealed interface OpenedChat {
        class Ready(val chat: TdApi.Chat) : OpenedChat
        class Stopped(val state: TelegramGroupState) : OpenedChat
    }
}
