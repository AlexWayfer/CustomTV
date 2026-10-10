package name.alexwayfer.customtv.data

import name.alexwayfer.customtv.chat.ChatterFollow

/**
 * The follow dates chatter cards loaded while one chat is open: a card opened again shows them at once and still
 * loads them, replacing them with the fresh answer. A failed load keeps what was there. It lives with the chat,
 * so reopening the stream starts empty. Profiles are kept for the whole app by [ProfileDetailsRepository].
 */
internal class ChatterCardCache {
    private val follows = HashMap<String, ChatterFollow>()

    fun cachedFollow(channelId: String, login: String): ChatterFollow? = follows[followKey(channelId, login)]

    /** Keeps a loaded follow answer; a failed one (null) leaves the cached answer, which it returns. */
    fun rememberFollow(channelId: String, login: String, loaded: ChatterFollow?): ChatterFollow? =
        loaded?.also { follows[followKey(channelId, login)] = it } ?: cachedFollow(channelId, login)

    private fun followKey(channelId: String, login: String) = "$channelId/${login.lowercase()}"
}
