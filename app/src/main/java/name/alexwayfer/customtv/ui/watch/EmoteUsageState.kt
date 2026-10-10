package name.alexwayfer.customtv.ui.watch

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import name.alexwayfer.customtv.chat.ChatMessage
import name.alexwayfer.customtv.chat.EmoteUse
import name.alexwayfer.customtv.chat.PickerEmote
import name.alexwayfer.customtv.chat.ownEmoteUsageText
import name.alexwayfer.customtv.data.EmoteUsageStore

/**
 * Emote usage on this channel, counted from the user's own messages as live chat shows them,
 * so messages sent from another device count too and this device's own echo counts once.
 */
@Composable
internal fun rememberEmoteUsage(
    channelId: String?,
    ownUserId: String?,
    liveMessages: Flow<ChatMessage>,
    emotes: List<PickerEmote>,
): List<EmoteUse> {
    val context = LocalContext.current
    val store = remember { EmoteUsageStore(context) }
    var usage by remember(channelId) { mutableStateOf<List<EmoteUse>>(emptyList()) }
    val knownNames = remember(emotes) { emotes.mapTo(HashSet()) { it.name } }
    val readKnownNames = rememberUpdatedState(knownNames)
    LaunchedEffect(channelId, ownUserId, liveMessages) {
        val id = channelId?.takeIf { it.isNotBlank() } ?: return@LaunchedEffect
        usage = withContext(Dispatchers.IO) { store.uses(id) }
        liveMessages.collect { message ->
            val text = ownEmoteUsageText(message, ownUserId) ?: return@collect
            usage = withContext(Dispatchers.IO) {
                store.record(id, text, readKnownNames.value, System.currentTimeMillis())
            }
        }
    }
    return usage
}
