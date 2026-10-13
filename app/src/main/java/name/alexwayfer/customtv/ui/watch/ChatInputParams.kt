package name.alexwayfer.customtv.ui.watch

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import name.alexwayfer.customtv.chat.ChatMessage
import name.alexwayfer.customtv.chat.ChatModePlaque
import name.alexwayfer.customtv.chat.ChatNick
import name.alexwayfer.customtv.chat.SevenTvEmote
import name.alexwayfer.customtv.data.ChatSettings

/** The channel whose chat the bar writes to. */
internal data class ChatInputChannel(
    val login: String,
    val broadcasterId: String?,
    val owner: ChatNick,
    val rules: List<String>?,
    val liveMessages: Flow<ChatMessage>,
    /** How tall a page opened in the browser from the chat is, so its sheet stops at the player. */
    val browserSheetHeightPx: () -> Int,
)

/** Who writes: the signed-in user; without one, [canSend] is false and the field offers Log in. */
internal data class ChatInputUser(
    val canSend: Boolean,
    val userId: String?,
    val login: String?,
    val displayName: String?,
    val accessToken: suspend () -> String?,
    val onLogIn: () -> Unit,
)

/** The third-party emotes the field completes and the picker shows. */
internal data class ChatInputEmotes(
    val sevenTvEnabled: Boolean,
    val bttvEnabled: Boolean,
    val ffzEnabled: Boolean,
    val sevenTv: Map<String, SevenTvEmote>,
    val bttv: Map<String, SevenTvEmote>,
    val ffz: Map<String, SevenTvEmote>,
    val completionWithoutColon: Boolean,
)

/** What the user writes, what it answers, and what happens once it is sent. */
internal data class ChatCompose(
    val draft: TextFieldState,
    val emotes: ChatInputEmotes,
    val replyParentMessageId: String?,
    val replyFocusRequest: OneShotRequest<Unit>?,
    /** Read only while a nick is typed or a command sent, so new chatters do not recompose the input. */
    val sessionChatters: () -> Map<String, String>,
    /** When each of [sessionChatters] last wrote, read with them to put the latest nicks first. */
    val chatterLastSpokeAt: () -> Map<String, Long>,
    /** What Send posts instead of a plain message, such as a watch streak; null for plain chat. */
    val shareOffer: ChatShareOffer?,
    val keepKeyboardAfterSend: Boolean,
    val slowSendBlocked: Boolean,
    val onSent: () -> Unit,
    /** Opens the chatter card of a login typed after `/user`. */
    val onOpenChatterCard: (login: String) -> Unit,
)

/** The chat menu behind the field's end button: the chat settings and what changes them. */
internal data class ChatMenu(
    val settings: ChatSettings?,
    val onReadableColorsChange: (Boolean) -> Unit,
    val onTimestampsChange: (Boolean) -> Unit,
    val onTextSizeChange: (Int) -> Unit,
    val onSmoothChatScrollChange: (Boolean) -> Unit,
    val onRefreshEmotes: () -> Job,
    val onRefreshLabels: () -> Job,
    /** Opens the signed-in user's own chatter card; null when logged out. */
    val onOpenOwnProfile: (() -> Unit)?,
)

/** Where the bar sits, what it shows around the field, and what it tells the screen. */
internal data class ChatInputBarLook(
    /** The field sits in the narrow chat beside a full screen player. */
    val fullscreen: Boolean,
    /** The chat lies over the video, so the closed bar lets it show through. */
    val overVideo: Boolean,
    val plaques: List<ChatModePlaque>,
    val onFollow: () -> Unit,
    val onSubscribe: () -> Unit,
    /** The channel points button before the field; nothing in the free build. */
    val channelPoints: @Composable (visible: Boolean) -> Unit,
    val onComposerOpenChange: (Boolean) -> Unit,
    /** The emote picker's height while it takes the keyboard's place; 0 otherwise. */
    val onPickerPanelPx: (Int) -> Unit,
)
