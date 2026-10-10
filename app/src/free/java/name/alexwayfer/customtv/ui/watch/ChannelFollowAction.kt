package name.alexwayfer.customtv.ui.watch

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import name.alexwayfer.customtv.chat.TwitchPlaqueAction
import name.alexwayfer.customtv.chat.followersPlaqueAction

/**
 * Following in the app goes through the experimental login, which is Premium. Here the heart and the
 * followers-only plaque's Follow open the channel on Twitch, where the user follows or unfollows it.
 */
@Stable
internal class ChannelFollowAction(private val open: () -> Unit) {
    val sending: Boolean get() = false

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
): ChannelFollowAction {
    val context = LocalContext.current
    val signedInState = rememberUpdatedState(signedIn)
    val logIn = rememberUpdatedState(onLogIn)
    return remember(channel, context) {
        ChannelFollowAction {
            when (val action = followersPlaqueAction(signedInState.value, channel)) {
                TwitchPlaqueAction.LogIn -> logIn.value()
                is TwitchPlaqueAction.Open -> context.startActivity(Intent(Intent.ACTION_VIEW, action.url.toUri()))
            }
        }
    }
}

/** The free build follows on Twitch, so it has no dialogs of its own. */
@Suppress("unused")
@Composable
internal fun ChannelFollowDialogs(action: ChannelFollowAction, hidden: Boolean) = Unit
