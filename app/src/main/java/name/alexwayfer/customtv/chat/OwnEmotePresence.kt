package name.alexwayfer.customtv.chat

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import name.alexwayfer.customtv.data.SevenTvPresence
import name.alexwayfer.customtv.data.SevenTvRepository
import name.alexwayfer.customtv.data.sevenTvPresenceDue

/**
 * Tells 7TV and BTTV that the signed-in user is in the open channel, as their extensions do, so the chats there show
 * the user's personal emotes and this one learns them for the picker. 7TV hears it once each socket has subscribed
 * and after the user's own messages; BTTV as its socket joins.
 */
internal class OwnEmotePresence(
    private val scope: CoroutineScope,
    private val bttv: BttvEventClient,
) {
    private var userId: String? = null
    private var sevenTvUserId: String? = null
    private var subscribedChannel: String? = null
    private var lastSentAtMillis: Long? = null
    private var lookup: Job? = null

    /** The signed-in user's Twitch ID; null when logged out. */
    fun setUser(twitchUserId: String?) {
        val normalized = twitchUserId?.takeIf { it.isNotBlank() }
        if (normalized == userId) return
        userId = normalized
        sevenTvUserId = null
        bttv.setSelf(normalized)
        lookup?.cancel()
        if (normalized == null) return
        lookup = scope.launch {
            val account = SevenTvRepository.sevenTvAccount(normalized) ?: return@launch
            if (userId != normalized) return@launch
            sevenTvUserId = account
            subscribedChannel?.let(::send)
        }
    }

    /** A 7TV socket now follows [channelId]; it hears of the user only from a presence sent after this. */
    fun onSubscribed(channelId: String) {
        subscribedChannel = channelId
        send(channelId)
    }

    /** A live chat [message] of the channel [channelId]; the user's own one announces them again now and then. */
    fun onChatMessage(message: ChatMessage, channelId: String?) {
        if (channelId == null || userId == null || message.userId != userId) return
        if (sevenTvPresenceDue(lastSentAtMillis, System.currentTimeMillis())) send(channelId)
    }

    private fun send(channelId: String) {
        val account = sevenTvUserId ?: return
        lastSentAtMillis = System.currentTimeMillis()
        scope.launch { SevenTvPresence.send(account, channelId) }
    }
}
