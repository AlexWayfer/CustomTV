package name.alexwayfer.customtv.chat

internal sealed interface TwitchPlaqueAction {
    data object LogIn : TwitchPlaqueAction
    data class Open(val url: String) : TwitchPlaqueAction
}

/** What a chat mode plaque's button is called. */
internal enum class ChatModeButton { Follow, Subscribe, LogIn }

/**
 * The button a plaque offers, if any. Logged out, following or subscribing starts with logging in, so the
 * button says Log in, which is what it does then.
 */
internal fun chatModeButton(plaque: ChatModePlaque, signedIn: Boolean): ChatModeButton? {
    val button = when (plaque) {
        is ChatModePlaque.FollowersNeedFollow -> ChatModeButton.Follow
        is ChatModePlaque.Subscribers if plaque.offerSubscribe -> ChatModeButton.Subscribe
        else -> return null
    }
    return if (signedIn) button else ChatModeButton.LogIn
}

internal fun followersPlaqueAction(signedIn: Boolean, channelLogin: String): TwitchPlaqueAction =
    twitchPlaqueAction(signedIn, twitchChannelPage(channelLogin))

internal fun subscribersPlaqueAction(signedIn: Boolean, channelLogin: String): TwitchPlaqueAction =
    twitchPlaqueAction(signedIn, twitchSubscribePage(channelLogin))

internal fun twitchPlaqueAction(signedIn: Boolean, url: String): TwitchPlaqueAction =
    if (!signedIn) TwitchPlaqueAction.LogIn else TwitchPlaqueAction.Open(url)

internal fun twitchChannelPage(login: String): String {
    return "https://www.twitch.tv/${twitchLoginPath(login)}"
}

internal fun twitchSubscribePage(login: String): String {
    return "https://www.twitch.tv/subs/${twitchLoginPath(login)}"
}

private fun twitchLoginPath(login: String): String = login.trim().removePrefix("#").lowercase()
