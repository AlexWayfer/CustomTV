package name.alexwayfer.customtv.chat

/** Message text size in sp. The other chat sizes follow it through `chatSp` and `chatDp`. */
internal const val DEFAULT_CHAT_TEXT_SIZE = 13
internal const val MIN_CHAT_TEXT_SIZE = 10
internal const val MAX_CHAT_TEXT_SIZE = 20

internal fun chatTextSizeFromStored(stored: Int?): Int =
    stored?.coerceIn(MIN_CHAT_TEXT_SIZE, MAX_CHAT_TEXT_SIZE) ?: DEFAULT_CHAT_TEXT_SIZE
