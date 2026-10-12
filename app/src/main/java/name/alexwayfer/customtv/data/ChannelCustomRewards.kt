package name.alexwayfer.customtv.data

import name.alexwayfer.customtv.chat.ChatReward
import java.util.concurrent.ConcurrentHashMap

/** The custom channel point rewards each channel profile listed, so a redemption in chat can show its reward. */
internal object ChannelCustomRewards {
    private val byLogin = ConcurrentHashMap<String, Map<String, ChatReward>>()

    fun remember(login: String, rewards: Map<String, ChatReward>) {
        byLogin[login] = rewards
    }

    fun reward(login: String, id: String): ChatReward? {
        return byLogin[login.lowercase()]?.get(id)
    }
}
