package name.alexwayfer.customtv.chat

import name.alexwayfer.customtv.data.ChannelLookup

internal fun recentChatNotice(result: RecentChatLoadResult): ChatNotice? =
    ChatNotice.RecentChatFailed.takeIf { result.failed && result.messages.isEmpty() }

internal fun channelLookupNotice(result: ChannelLookup): ChatNotice? =
    ChatNotice.ChannelNotFound.takeIf { result == ChannelLookup.NotFound }
