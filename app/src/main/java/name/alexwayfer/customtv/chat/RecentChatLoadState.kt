package name.alexwayfer.customtv.chat

internal data class RecentChatLoadResult(
    val messages: List<ChatMessage> = emptyList(),
    val failed: Boolean = false,
    val moderation: List<ChatMessage> = emptyList(),
)

internal enum class RecentChatLoadState { Idle, Loading }
