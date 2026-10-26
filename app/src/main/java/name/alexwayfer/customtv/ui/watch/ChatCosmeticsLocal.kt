package name.alexwayfer.customtv.ui.watch

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.TextLayoutResult
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import name.alexwayfer.customtv.chat.ChatCosmetics
import name.alexwayfer.customtv.chat.ChatterCosmetics
import name.alexwayfer.customtv.chat.SevenTvCosmetics
import name.alexwayfer.customtv.chat.cosmeticSources
import name.alexwayfer.customtv.data.AppSettings
import name.alexwayfer.customtv.data.ThirdPartyBadgeLists

private object NoChatCosmetics : State<ChatCosmetics> {
    override val value: ChatCosmetics = ChatCosmetics()
}

/** The open chat's third-party badges and paints, as a state each row reads only its own author's of. */
internal val LocalChatCosmetics = staticCompositionLocalOf<State<ChatCosmetics>> { NoChatCosmetics }

/**
 * Gives the rows the badges and paints the [settings] turn on, loading FFZ's and BTTV's lists once they are. Before the
 * settings are restored nothing shows, so a turned off switch does not flash its badges in.
 */
@Composable
internal fun ProvideChatCosmetics(
    settings: AppSettings?,
    sevenTv: () -> SevenTvCosmetics,
    content: @Composable () -> Unit,
) {
    val sources = settings?.let(::cosmeticSources)
    val ffzOn = sources?.ffzBadges == true
    val bttvOn = sources?.bttvBadges == true
    LaunchedEffect(ffzOn) { if (ffzOn) ThirdPartyBadgeLists.loadFfz() }
    LaunchedEffect(bttvOn) { if (bttvOn) ThirdPartyBadgeLists.loadBttv() }
    val ffz by ThirdPartyBadgeLists.ffz.collectAsStateWithLifecycle()
    val bttv by ThirdPartyBadgeLists.bttv.collectAsStateWithLifecycle()
    val current = rememberUpdatedState(sevenTv)
    val currentSources = rememberUpdatedState(sources)
    val currentFfz = rememberUpdatedState(ffz)
    val currentBttv = rememberUpdatedState(bttv)
    // One state object for the pane's lifetime: a new paint or list reaches only the rows it changes.
    val cosmetics = remember {
        derivedStateOf {
            val shown = currentSources.value ?: return@derivedStateOf ChatCosmetics()
            ChatCosmetics(shown, current.value(), currentFfz.value, currentBttv.value)
        }
    }
    CompositionLocalProvider(LocalChatCosmetics provides cosmetics, content = content)
}

/** The badges and paint of [userId]; a change for another chatter does not recompose the caller. */
@Composable
internal fun rememberChatterCosmetics(userId: String?): ChatterCosmetics {
    val all = LocalChatCosmetics.current
    val cosmetics by remember(userId, all) {
        derivedStateOf(structuralEqualityPolicy()) { all.value.forUser(userId) }
    }
    return cosmetics
}

/** Where the author's name lies in a message's [layout], for a paint to cover only it; null while it is stale. */
internal fun chatAuthorNameBox(text: AnnotatedString, layout: TextLayoutResult): Rect? {
    if (!layoutTextMatches(layout.layoutInput.text, text)) return null
    val name = text.getLinkAnnotations(0, text.length).firstOrNull { range ->
        (range.item as? LinkAnnotation.Clickable)?.tag == CHAT_AUTHOR_LINK_TAG
    } ?: return null
    if (name.end <= name.start || name.end > layout.layoutInput.text.length) return null
    return layout.getPathForRange(name.start, name.end).getBounds()
}
