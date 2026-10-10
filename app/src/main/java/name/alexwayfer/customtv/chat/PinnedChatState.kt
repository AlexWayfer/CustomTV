package name.alexwayfer.customtv.chat

internal fun shouldClearPinnedChat(current: PinnedChat?, requestedPinId: String?): Boolean {
    return requestedPinId == null || current == null || current.pinId == requestedPinId
}

internal fun updatePinnedChatDuration(
    current: PinnedChat?,
    requestedPinId: String?,
    endsAtMillis: Long?,
): PinnedChat? {
    if (current == null) return null
    if (requestedPinId != null && current.pinId != requestedPinId) return current
    return current.copy(endsAtMillis = endsAtMillis ?: current.endsAtMillis)
}

internal fun visiblePinnedChat(current: PinnedChat?, hiddenPinId: String?): PinnedChat? {
    return current.takeUnless { it != null && it.pinId == hiddenPinId }
}
