package name.alexwayfer.customtv.chat

/**
 * When each chatter of the session last wrote, by lowercase login, so nick completion offers the
 * latest ones first. Plain mutable state touched on the main thread: every message updates it, and
 * only a nick query reads it, so a busy chat does not copy a map or recompose anything per message.
 */
internal class ChatterRecency {
    val lastSpokeAt: Map<String, Long>
        field = HashMap<String, Long>()

    /** History loaded after live messages is older, so a time never moves back. */
    fun record(message: ChatMessage) {
        val login = message.sessionAuthorLogin() ?: return
        val time = message.timestampMillis
        if (time > (lastSpokeAt[login] ?: Long.MIN_VALUE)) lastSpokeAt[login] = time
    }

    fun clear() {
        lastSpokeAt.clear()
    }
}
