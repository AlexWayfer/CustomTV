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
import name.alexwayfer.customtv.data.CustomTvBadgeEdition
import name.alexwayfer.customtv.data.CustomTvBadges
import name.alexwayfer.customtv.data.ThirdPartyBadgeLists

private object NoChatCosmetics : State<ChatCosmetics> {
    override val value: ChatCosmetics = ChatCosmetics()
}

/** The open chat's third-party badges and paints, as a state each row reads only its own author's of. */
internal val LocalChatCosmetics = staticCompositionLocalOf<State<ChatCosmetics>> { NoChatCosmetics }

/**
 * Gives the rows the badges and paints the [settings] turn on, loading FFZ's, BTTV's, and CustomTV's lists once they
 * are. Before the settings are restored nothing shows, so a turned off switch does not flash its badges in.
 */
@Composable
internal fun ProvideChatCosmetics(
    settings: AppSettings?,
    sevenTv: () -> SevenTvCosmetics,
    chatterIds: () -> Map<String, String>,
    content: @Composable () -> Unit,
) {
    val sources = settings?.let(::cosmeticSources)
    val ffzOn = sources?.ffzBadges == true
    val bttvOn = sources?.bttvBadges == true
    val customTvOn = sources?.customTvBadges == true
    LaunchedEffect(ffzOn) { if (ffzOn) ThirdPartyBadgeLists.loadFfz() }
    LaunchedEffect(bttvOn) { if (bttvOn) ThirdPartyBadgeLists.loadBttv() }
    LaunchedEffect(customTvOn) { if (customTvOn) CustomTvBadges.load() }
    val ffz by ThirdPartyBadgeLists.ffz.collectAsStateWithLifecycle()
    val bttv by ThirdPartyBadgeLists.bttv.collectAsStateWithLifecycle()
    val customTv by CustomTvBadges.list.collectAsStateWithLifecycle()
    val customTvPictures = customTvBadgePictures()
    val current = rememberUpdatedState(sevenTv)
    val currentChatterIds = rememberUpdatedState(chatterIds)
    val currentSources = rememberUpdatedState(sources)
    val currentFfz = rememberUpdatedState(ffz)
    val currentBttv = rememberUpdatedState(bttv)
    val currentCustomTv = rememberUpdatedState(customTv)
    // One state object for the pane's lifetime: a new paint, list, or chatter reaches only the rows it changes.
    val cosmetics = remember(customTvPictures) {
        derivedStateOf {
            val shown = currentSources.value ?: return@derivedStateOf ChatCosmetics()
            ChatCosmetics(
                sources = shown,
                sevenTv = current.value(),
                ffz = currentFfz.value,
                bttv = currentBttv.value,
                chatterIds = currentChatterIds.value(),
                customTv = currentCustomTv.value,
                customTvPictures = customTvPictures,
            )
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

/** The CustomTV badge of [userId]; a change for another chatter does not recompose the caller. */
@Composable
internal fun rememberChatterCustomTvEdition(userId: String?): CustomTvBadgeEdition? {
    val all = LocalChatCosmetics.current
    val edition by remember(userId, all) { derivedStateOf { all.value.customTvEdition(userId) } }
    return edition
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
