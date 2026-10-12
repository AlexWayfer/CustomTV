package name.alexwayfer.customtv.data

sealed interface StreamStatusEvent {
    val login: String

    data class Title(
        override val login: String,
        val title: String,
        val categoryName: String?,
    ) : StreamStatusEvent

    data class Online(
        override val login: String,
        val startedAtMillis: Long?,
    ) : StreamStatusEvent

    data class Offline(
        override val login: String,
    ) : StreamStatusEvent
}
