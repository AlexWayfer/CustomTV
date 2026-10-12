package name.alexwayfer.customtv.chat

data class ChatUrlSpan(
    val start: Int,
    val endExclusive: Int,
    val raw: String,
    val href: String,
)
