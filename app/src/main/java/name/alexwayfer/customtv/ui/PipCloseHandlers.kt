package name.alexwayfer.customtv.ui

import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.withFrameNanos
import androidx.core.content.edit
import kotlinx.coroutines.launch
import name.alexwayfer.customtv.MainActivity
import name.alexwayfer.customtv.chat.ChatViewModel
import name.alexwayfer.customtv.data.ChannelAvatarRepository
import name.alexwayfer.customtv.player.PlayerPlaybackService
import name.alexwayfer.customtv.player.StreamPlayback

@Composable
internal fun BindPipCloseHandlers(
    activity: MainActivity,
    channel: String,
    preferences: SharedPreferences,
    chatViewModel: ChatViewModel,
    onClosed: () -> Unit,
    onResumed: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    DisposableEffect(activity, channel, preferences, chatViewModel) {
        val closePip: () -> Unit = {
            // The ID, not the login, so a channel renamed before the next launch still opens.
            val channelId = ChannelAvatarRepository.cached(channel)?.id
            preferences.edit(commit = true) {
                if (channelId != null) putString(PIP_RESUME_CHANNEL_ID, channelId) else remove(PIP_RESUME_CHANNEL_ID)
            }
            StreamPlayback.stopForDismissal()
            PlayerPlaybackService.stopImmediately(activity)
            chatViewModel.setForeground(false)
            onClosed()
        }
        val resumeAfterPipClose: () -> Unit = {
            preferences.edit { remove(PIP_RESUME_CHANNEL_ID) }
            scope.launch {
                withFrameNanos { }
                withFrameNanos { }
                onResumed()
            }
        }
        activity.onPipClosed = closePip
        activity.onResumeAfterPipClosed = resumeAfterPipClose
        onDispose {
            if (activity.onPipClosed === closePip) activity.onPipClosed = null
            if (activity.onResumeAfterPipClosed === resumeAfterPipClose) {
                activity.onResumeAfterPipClosed = null
            }
        }
    }
}
