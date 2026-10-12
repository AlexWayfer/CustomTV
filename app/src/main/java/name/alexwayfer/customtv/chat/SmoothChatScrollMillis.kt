package name.alexwayfer.customtv.chat

private const val SMOOTH_CHAT_SCROLL_MILLIS = 300

internal fun smoothChatScrollMillis(enabled: Boolean): Int? = if (enabled) SMOOTH_CHAT_SCROLL_MILLIS else null
