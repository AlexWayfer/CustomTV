package name.alexwayfer.customtv.ui.account

/**
 * What a channel's profile menu holds besides the message logs, which need no account: a whisper, which needs one,
 * and a report, whose sheet asks to log in itself.
 */
internal data class ChannelProfileMenuItems(val whisper: Boolean, val report: Boolean)

/** The menu of [channelLogin]'s profile seen from [accountLogin]: the user's own profile has only the logs. */
internal fun channelProfileMenuItems(accountLogin: String?, channelLogin: String): ChannelProfileMenuItems {
    val ownProfile = accountLogin != null && channelLogin.equals(accountLogin, ignoreCase = true)
    return ChannelProfileMenuItems(whisper = accountLogin != null && !ownProfile, report = !ownProfile)
}
