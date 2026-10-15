package name.alexwayfer.customtv.data

sealed interface StreamStatusEvent {
    val login: String

    /** A new title or category; [categoryId] names the category in the app's language, as Twitch's is English. */
    data class Title(
        override val login: String,
        val title: String,
        val categoryName: String?,
        val categoryId: String? = null,
    ) : StreamStatusEvent

    data class Online(
        override val login: String,
        val startedAtMillis: Long?,
    ) : StreamStatusEvent

    data class Offline(
        override val login: String,
    ) : StreamStatusEvent
}
