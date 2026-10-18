package name.alexwayfer.customtv.ui.watch

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import name.alexwayfer.customtv.chat.TwitchPlaqueAction
import name.alexwayfer.customtv.chat.followersPlaqueAction
import name.alexwayfer.customtv.links.openOutsideApp

/**
 * Following in the app goes through the experimental login, which is Premium. Here the heart and the
 * followers-only plaque's Follow open the channel on Twitch, where the user follows or unfollows it.
 */
@Stable
internal class FollowAction(private val open: () -> Unit) {
    val sending: Boolean get() = false

    /** Twitch does the follow, so the heart shows what the app knows. */
    fun shownFollowing(following: Boolean): Boolean = following

    fun tap() = open()
}

@Suppress("unused")
@Composable
internal fun rememberChannelFollowAction(
    channel: String,
    channelId: String?,
    channelName: String,
    signedIn: Boolean,
    following: Boolean,
    onFollowChanged: (following: Boolean) -> Unit,
    onLogIn: () -> Unit,
): FollowAction {
    val context = LocalContext.current
    val signedInState = rememberUpdatedState(signedIn)
    val logIn = rememberUpdatedState(onLogIn)
    return remember(channel, context) {
        FollowAction {
            when (val action = followersPlaqueAction(signedInState.value, channel)) {
                TwitchPlaqueAction.LogIn -> logIn.value()
                is TwitchPlaqueAction.Open -> openOutsideApp(context, action.url)
            }
        }
    }
}

/** The free build follows on Twitch, so it has no dialogs of its own. */
@Suppress("unused")
@Composable
internal fun FollowDialogs(action: FollowAction, hidden: Boolean) = Unit
