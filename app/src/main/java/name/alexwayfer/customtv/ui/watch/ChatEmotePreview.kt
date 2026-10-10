package name.alexwayfer.customtv.ui.watch

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.chat.ChatPart
import name.alexwayfer.customtv.chat.EmoteLibrary
import name.alexwayfer.customtv.chat.EmoteModifier
import name.alexwayfer.customtv.chat.EmoteOrigin
import name.alexwayfer.customtv.chat.EmotePlatform
import name.alexwayfer.customtv.chat.EmoteEffects
import name.alexwayfer.customtv.chat.FfzGiantEmote
import name.alexwayfer.customtv.chat.emoteLibraryFromUrl
import name.alexwayfer.customtv.chat.libraryEmoteOrigin
import name.alexwayfer.customtv.chat.twitchEmoteId
import name.alexwayfer.customtv.chat.twitchEmoteOrigin
import name.alexwayfer.customtv.data.BttvRepository
import name.alexwayfer.customtv.data.FfzRepository
import name.alexwayfer.customtv.data.SevenTvRepository
import name.alexwayfer.customtv.data.TwitchEmoteDetails
import name.alexwayfer.customtv.data.TwitchEmoteDetailsRepository
import name.alexwayfer.customtv.ui.components.resolvedEmoteAspectRatio

internal data class EmotePreviewChannel(
    val twitchUserId: String?,
    val displayName: String,
)

internal val LocalEmotePreviewChannel = compositionLocalOf { EmotePreviewChannel(null, "") }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ChatEmotePreview(
    emote: ChatPart.Emote,
    onDismiss: () -> Unit,
) {
    DismissWhenPlayerContentHidden(onDismiss)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var dismissing by remember { mutableStateOf(false) }
    val windowHeight = with(LocalDensity.current) {
        LocalWindowInfo.current.containerSize.height.toDp()
    }
    val maxContentHeight = minOf(480.dp, windowHeight * 0.65f)
    ModalBottomSheet(
        onDismissRequest = {
            if (!dismissing) {
                dismissing = true
                scope.launch {
                    sheetState.hide()
                    onDismiss()
                }
            }
        },
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxContentHeight)
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            EmotePreviewItem(emote)
            emote.modifiers.forEach { modifier ->
                HorizontalDivider(modifier = Modifier.padding(vertical = 20.dp))
                EmoteModifierPreviewItem(emote, modifier)
            }
            emote.overlays.forEach { overlay ->
                HorizontalDivider(modifier = Modifier.padding(vertical = 20.dp))
                EmotePreviewItem(overlay, isOverlay = true)
                overlay.modifiers.forEach { modifier ->
                    HorizontalDivider(modifier = Modifier.padding(vertical = 20.dp))
                    EmoteModifierPreviewItem(overlay, modifier)
                }
            }
        }
    }
}

@Composable
private fun EmotePreviewItem(emote: ChatPart.Emote, isOverlay: Boolean = false) {
    if (isOverlay) {
        Text(
            text = stringResource(R.string.emote_preview_overlay),
            modifier = Modifier.padding(bottom = 12.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium,
        )
    }
    EmotePreviewImage(emote.copy(effects = EmoteEffects()))
    Text(
        text = emote.name,
        modifier = Modifier.padding(top = 16.dp),
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.titleLarge,
    )
    EmotePreviewCaption(name = emote.name, url = emote.url)
}

@Composable
private fun EmoteModifierPreviewItem(base: ChatPart.Emote, modifier: EmoteModifier) {
    Text(
        text = stringResource(R.string.emote_preview_modifier),
        modifier = Modifier.padding(bottom = 12.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.labelMedium,
    )
    if (!modifier.effects.removeSpaceBefore) {
        EmotePreviewImage(base.copy(effects = modifier.effects), maxHeight = 96.dp)
    }
    Text(
        text = modifier.name,
        modifier = Modifier.padding(top = 12.dp),
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.titleLarge,
    )
    Text(
        text = when (modifier.platform) {
            EmotePlatform.Bttv -> stringResource(R.string.chat_emote_platform_bttv)
            EmotePlatform.Ffz -> stringResource(R.string.chat_emote_platform_ffz)
            EmotePlatform.SevenTv -> stringResource(R.string.chat_emote_platform_7tv)
        },
        modifier = Modifier.padding(top = 4.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyMedium,
    )
}

@Composable
private fun EmotePreviewImage(emote: ChatPart.Emote, maxHeight: Dp = 160.dp) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        val height = minOf(maxHeight, maxWidth / resolvedEmoteAspectRatio(emote.url, emote.aspectRatio))
        ChatEmoteEffectImage(
            url = FfzGiantEmote.highResolutionUrl(emote.url),
            contentDescription = emote.name,
            size = height,
            aspectRatio = emote.aspectRatio,
            effects = emote.effects,
        )
    }
}

private fun twitchDetailsOrigin(details: TwitchEmoteDetails): EmoteOrigin {
    return twitchEmoteOrigin(details.type, details.subscriptionTier, details.ownerDisplayName)
}

@Composable
internal fun EmotePreviewCaption(name: String, url: String) {
    val origin = rememberEmotePreviewOrigin(name, url) ?: return
    Text(
        text = emoteOriginText(origin),
        modifier = Modifier.padding(top = 4.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.bodyMedium,
    )
}

@Composable
private fun rememberEmotePreviewOrigin(name: String, url: String): EmoteOrigin? {
    val channel = LocalEmotePreviewChannel.current
    val library = emoteLibraryFromUrl(url)
    val twitchId = twitchEmoteId(url)
    var origin by remember(url, name, channel) {
        mutableStateOf(
            twitchId?.let { TwitchEmoteDetailsRepository.peek(it) }?.let(::twitchDetailsOrigin)
                ?: if (twitchId != null) {
                    EmoteOrigin.Twitch
                } else {
                    library?.let { cachedLibraryOrigin(it, name, channel) }
                },
        )
    }
    LaunchedEffect(twitchId, name, channel) {
        val known = twitchId?.let { TwitchEmoteDetailsRepository.peek(it) }
        if (known != null) {
            origin = twitchDetailsOrigin(known)
        } else if (twitchId != null) {
            TwitchEmoteDetailsRepository.details(twitchId)?.let { origin = twitchDetailsOrigin(it) }
        } else {
            origin = library?.let { cachedLibraryOrigin(it, name, channel) }
        }
    }
    return origin
}

@Composable
private fun emoteOriginText(origin: EmoteOrigin): String = when (origin) {
    EmoteOrigin.TwitchGlobal -> stringResource(R.string.emote_preview_twitch_global)
    is EmoteOrigin.TwitchSubscription -> {
        val tier = origin.tier
        if (tier == null) {
            stringResource(R.string.emote_preview_twitch_sub, origin.channel)
        } else {
            stringResource(R.string.emote_preview_twitch_sub_tier, tier, origin.channel)
        }
    }
    is EmoteOrigin.TwitchFollower -> stringResource(R.string.emote_preview_twitch_follower, origin.channel)
    is EmoteOrigin.TwitchBits -> stringResource(R.string.emote_preview_twitch_bits, origin.channel)
    is EmoteOrigin.TwitchPrime -> stringResource(R.string.emote_preview_twitch_prime, origin.channel)
    is EmoteOrigin.TwitchChannel -> stringResource(R.string.emote_preview_twitch_channel, origin.channel)
    EmoteOrigin.Twitch -> stringResource(R.string.emote_preview_twitch)
    is EmoteOrigin.LibraryGlobal -> stringResource(
        R.string.emote_preview_library_global,
        libraryName(origin.library),
    )
    is EmoteOrigin.LibraryChannel -> stringResource(
        R.string.emote_preview_library_channel,
        libraryName(origin.library),
        origin.channel,
    )
    is EmoteOrigin.Library -> stringResource(R.string.emote_preview_library, libraryName(origin.library))
}

@Composable
private fun libraryName(library: EmoteLibrary): String = when (library) {
    EmoteLibrary.SevenTv -> stringResource(R.string.chat_emote_platform_7tv)
    EmoteLibrary.Bttv -> stringResource(R.string.chat_emote_platform_bttv)
    EmoteLibrary.Ffz -> stringResource(R.string.chat_emote_platform_ffz)
}

private fun cachedLibraryOrigin(
    library: EmoteLibrary,
    name: String,
    channel: EmotePreviewChannel,
): EmoteOrigin {
    val userId = channel.twitchUserId
    val onChannel = when (library) {
        EmoteLibrary.SevenTv -> SevenTvRepository.isChannelEmote(userId, name)
        EmoteLibrary.Bttv -> BttvRepository.isChannelEmote(userId, name)
        EmoteLibrary.Ffz -> FfzRepository.isChannelEmote(userId, name)
    }
    val global = when (library) {
        EmoteLibrary.SevenTv -> SevenTvRepository.isGlobalEmote(name)
        EmoteLibrary.Bttv -> BttvRepository.isGlobalEmote(name)
        EmoteLibrary.Ffz -> FfzRepository.isGlobalEmote(name)
    }
    return libraryEmoteOrigin(library, onChannel, channel.displayName, global)
}
