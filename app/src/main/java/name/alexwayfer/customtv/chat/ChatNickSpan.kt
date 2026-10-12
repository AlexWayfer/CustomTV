package name.alexwayfer.customtv.chat

data class ChatNickSpan(
    val start: Int,
    val endExclusive: Int,
    val raw: String,
    val key: String,
    val mentioned: Boolean,
)
