package name.alexwayfer.customtv.ui.watch

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import name.alexwayfer.customtv.chat.ChannelEvents
import name.alexwayfer.customtv.chat.ChannelShoutout
import name.alexwayfer.customtv.chat.ChatMessage
import name.alexwayfer.customtv.chat.ChatReplySession
import name.alexwayfer.customtv.chat.ChatViewModel
import name.alexwayfer.customtv.chat.OutgoingRaid
import name.alexwayfer.customtv.chat.PersonalEmotes
import name.alexwayfer.customtv.chat.SevenTvCosmetics
import name.alexwayfer.customtv.chat.PinnedChat
import name.alexwayfer.customtv.chat.RecentChatLoadState
import name.alexwayfer.customtv.chat.SevenTvEmote

/** What the stream screen keeps for the open channel's chat: its list, its draft, the reply, and what is open. */
@Stable
internal class WatchChatState {
    val listState = LazyListState()
    var stickToBottom by mutableStateOf(true)
    val draft = TextFieldState()
    var pendingChatLink by mutableStateOf<String?>(null)
    var showCollaboration by mutableStateOf(false)
    var replySession by mutableStateOf<ChatReplySession?>(null)
    var replyFocusRequest by mutableStateOf<OneShotRequest<Unit>?>(null)
    var mentionRequest by mutableStateOf<OneShotRequest<String>?>(null)
    var fieldCardOpen by mutableStateOf<OneShotRequest<ChatterCardRequest>?>(null)

    /** The chat recap's sheet is to open again, as Back from the settings it opened returns to the stream. */
    var recapOpen by mutableStateOf<OneShotRequest<Unit>?>(null)
    var composerOpen by mutableStateOf(false)
}

/**
 * The chat as the view model streams it. The messages, the chatters, and the nick colors are read only where they
 * are shown, so a new chat message does not recompose the stream screen.
 */
internal data class WatchChatData(
    val messages: State<List<ChatMessage>>,
    val recentChatLoadState: State<RecentChatLoadState>,
    val reconnecting: State<Boolean>,
    val sessionChatters: State<Map<String, String>>,
    val badgeUrls: State<Map<String, String>>,
    val sevenTvEmotes: State<Map<String, SevenTvEmote>>,
    val ffzEmotes: State<Map<String, SevenTvEmote>>,
    val bttvEmotes: State<Map<String, SevenTvEmote>>,
    val personalEmotes: State<PersonalEmotes>,
    val sevenTvCosmetics: State<SevenTvCosmetics>,
    val nickColors: State<Map<String, Color>>,
    val chatterIds: State<Map<String, String>>,
    val pinnedChat: State<PinnedChat?>,
    val outgoingRaid: State<OutgoingRaid?>,
    val shoutout: State<ChannelShoutout?>,
    val channelEvents: State<ChannelEvents>,
)

@Composable
internal fun collectWatchChatData(chatViewModel: ChatViewModel): WatchChatData = WatchChatData(
    messages = chatViewModel.messages.collectAsStateWithLifecycle(),
    recentChatLoadState = chatViewModel.recentChatLoadState.collectAsStateWithLifecycle(),
    reconnecting = chatViewModel.reconnecting.collectAsStateWithLifecycle(),
    sessionChatters = chatViewModel.sessionChatters.collectAsStateWithLifecycle(),
    badgeUrls = chatViewModel.badgeUrls.collectAsStateWithLifecycle(),
    sevenTvEmotes = chatViewModel.sevenTvEmotes.collectAsStateWithLifecycle(),
    ffzEmotes = chatViewModel.ffzEmotes.collectAsStateWithLifecycle(),
    bttvEmotes = chatViewModel.bttvEmotes.collectAsStateWithLifecycle(),
    personalEmotes = chatViewModel.personalEmotes.collectAsStateWithLifecycle(),
    sevenTvCosmetics = chatViewModel.sevenTvCosmetics.collectAsStateWithLifecycle(),
    nickColors = chatViewModel.nickColors.collectAsStateWithLifecycle(),
    chatterIds = chatViewModel.chatterIds.collectAsStateWithLifecycle(),
    pinnedChat = chatViewModel.pinnedChat.collectAsStateWithLifecycle(),
    outgoingRaid = chatViewModel.outgoingRaid.collectAsStateWithLifecycle(),
    shoutout = chatViewModel.shoutout.collectAsStateWithLifecycle(),
    channelEvents = chatViewModel.channelEvents.collectAsStateWithLifecycle(),
)
