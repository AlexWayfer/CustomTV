package name.alexwayfer.customtv.chat

internal const val COMMUNITY_GIFT_PLAQUE_MILLIS = 15_000L

internal fun latestCommunityGiftMessage(messages: List<ChatMessage>): ChatMessage? =
    messages.lastOrNull { it.communityGift != null }

internal fun communityGiftPlaqueRemainingMillis(
    message: ChatMessage,
    nowMillis: Long,
): Long = (message.timestampMillis + COMMUNITY_GIFT_PLAQUE_MILLIS - nowMillis)
    .coerceIn(0L, COMMUNITY_GIFT_PLAQUE_MILLIS)

internal fun visibleCommunityGiftMessage(
    latest: ChatMessage?,
    hiddenMessageId: String?,
    nowMillis: Long,
): ChatMessage? = latest
    ?.takeUnless { it.id == hiddenMessageId }
    ?.takeIf { communityGiftPlaqueRemainingMillis(it, nowMillis) > 0L }
