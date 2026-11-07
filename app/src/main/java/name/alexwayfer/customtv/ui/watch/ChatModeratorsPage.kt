package name.alexwayfer.customtv.ui.watch

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.withTimeoutOrNull
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.data.ChatModerator
import name.alexwayfer.customtv.data.ChatModeratorAppearance
import name.alexwayfer.customtv.data.loadModeratorAppearances
import name.alexwayfer.customtv.data.sortedChatModerators
import name.alexwayfer.customtv.data.loadPublicChatModerators
import name.alexwayfer.customtv.data.loadTwitchChatModerators
import name.alexwayfer.customtv.ui.rememberHeldWhileLoading
import name.alexwayfer.customtv.ui.components.TextButton
import name.alexwayfer.customtv.ui.theme.TwitchPurple
import name.alexwayfer.customtv.ui.theme.TwitchTextSecondary
import kotlin.time.Duration.Companion.seconds

private sealed interface ModeratorsLoad {
    data object Loading : ModeratorsLoad
    data object Failed : ModeratorsLoad
    data class Loaded(
        val moderators: List<ChatModerator>,
        val fromTwitch: Boolean,
        val appearances: Map<String, ChatModeratorAppearance>,
    ) : ModeratorsLoad
}

/** The same page for the menu row and `/mods`, holding the latest pick callback through menu animations. */
@Composable
internal fun rememberChatModeratorsSubpage(
    channelLogin: String,
    channelId: String?,
    readableColors: Boolean,
    hideThen: (() -> Unit) -> Unit,
    onMention: ((ChatModerator) -> Unit)?,
): ChatMenuSubpage {
    val pick = rememberUpdatedState<(ChatModerator) -> Unit> { moderator ->
        hideThen { onMention?.invoke(moderator) }
    }
    val readable = rememberUpdatedState(readableColors)
    return remember(channelLogin, channelId) {
        ChatMenuSubpage(R.string.chat_moderators) {
            ChatModeratorsPage(
                channelLogin, channelId, readable.value,
                onPick = { pick.value(it) },
            )
        }
    }
}

/** Loads only while this menu page is open; re-entry asks for a fresh snapshot. */
@Composable
private fun ChatModeratorsPage(
    channelLogin: String,
    channelId: String?,
    readableColors: Boolean,
    onPick: (ChatModerator) -> Unit,
) {
    val twitch = experimentalLoginOffer() == ExperimentalLoginOffer.LoggedIn
    var retry by remember { mutableStateOf(false) }
    var loaded by remember(channelLogin, twitch) { mutableStateOf<ModeratorsLoad>(ModeratorsLoad.Loading) }
    // A late channel ID must not repeat a load already made by login on this visit.
    LaunchedEffect(channelLogin, twitch, retry) {
        loaded = ModeratorsLoad.Loading
        loaded = loadModerators(channelLogin, channelId, twitch)
    }
    val shown = rememberHeldWhileLoading(loaded, loaded == ModeratorsLoad.Loading)
    AnimatedContent(targetState = shown, label = "chat moderators load") { state ->
        when (state) {
            ModeratorsLoad.Loading -> {
                val loading = stringResource(R.string.chat_moderators_loading)
                Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        color = TwitchPurple,
                        modifier = Modifier.semantics { contentDescription = loading },
                    )
                }
            }
            ModeratorsLoad.Failed -> Column(Modifier.padding(16.dp)) {
                Text(stringResource(R.string.chat_moderators_failed), color = TwitchTextSecondary)
                TextButton(onClick = { retry = !retry }) { Text(stringResource(R.string.retry)) }
            }
            is ModeratorsLoad.Loaded -> Column {
                if (!state.fromTwitch) {
                    Text(
                        stringResource(
                            if (twitch) R.string.chat_moderators_presence_failed else R.string.chat_moderators_public,
                        ),
                        color = TwitchTextSecondary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
                ChatModeratorsContent(state.moderators, state.appearances, readableColors, onPick)
            }
        }
    }
}

private suspend fun loadModerators(channelLogin: String, channelId: String?, twitch: Boolean): ModeratorsLoad {
    val direct = if (twitch) {
        withTimeoutOrNull(MODERATORS_LOAD_TIMEOUT) { loadTwitchChatModerators(channelId, channelLogin) }
    } else {
        null
    }
    val moderators = direct ?: run {
        withTimeoutOrNull(MODERATORS_LOAD_TIMEOUT) { loadPublicChatModerators(channelLogin) }
    } ?: return ModeratorsLoad.Failed
    val appearances = withTimeoutOrNull(MODERATORS_LOAD_TIMEOUT) {
        val missing = moderators.filter { it.appearance == null }.map { it.id }
        loadModeratorAppearances(missing, channelId, channelLogin)
    }.orEmpty()
    return ModeratorsLoad.Loaded(sortedChatModerators(moderators), fromTwitch = direct != null, appearances)
}

private val MODERATORS_LOAD_TIMEOUT = 30.seconds
