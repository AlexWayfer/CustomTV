package name.alexwayfer.customtv.ui.watch

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import name.alexwayfer.customtv.BuildConfig
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.chat.ChannelEmoteSource
import name.alexwayfer.customtv.chat.EmotePickerKind
import name.alexwayfer.customtv.chat.EmotePickerPlace
import name.alexwayfer.customtv.chat.EmotePickerSection
import name.alexwayfer.customtv.chat.PickerEmote
import name.alexwayfer.customtv.chat.SevenTvEmote
import name.alexwayfer.customtv.chat.emotePickerSections
import name.alexwayfer.customtv.data.BttvRepository
import name.alexwayfer.customtv.data.FfzRepository
import name.alexwayfer.customtv.data.SevenTvRepository
import name.alexwayfer.customtv.data.TwitchEmoteCatalogRepository
import name.alexwayfer.customtv.ui.components.SharedEmoteImage
import name.alexwayfer.customtv.ui.theme.TwitchPurple
import name.alexwayfer.customtv.ui.theme.TwitchSurface
import name.alexwayfer.customtv.ui.theme.TwitchSurfaceAlt
import name.alexwayfer.customtv.ui.theme.TwitchTextSecondary

internal data class EmotePickerCatalog(
    val ready: Boolean,
    val sections: List<EmotePickerSection>,
)

@Composable
internal fun rememberEmotePickerSections(
    enabled: Boolean,
    channelId: String?,
    channelLabel: String,
    ownUserId: String?,
    ownLabel: String?,
    sevenTvEnabled: Boolean,
    bttvEnabled: Boolean,
    ffzEnabled: Boolean,
    sevenTvEmotes: Map<String, SevenTvEmote>,
    bttvEmotes: Map<String, SevenTvEmote>,
    ffzEmotes: Map<String, SevenTvEmote>,
    accessToken: suspend () -> String?,
): EmotePickerCatalog {
    var sections by remember(channelId, ownUserId) { mutableStateOf<List<EmotePickerSection>>(emptyList()) }
    var ready by remember(channelId, ownUserId) { mutableStateOf(false) }
    LaunchedEffect(
        enabled,
        channelId,
        channelLabel,
        ownUserId,
        ownLabel,
        sevenTvEnabled,
        bttvEnabled,
        ffzEnabled,
        sevenTvEmotes,
        bttvEmotes,
        ffzEmotes,
    ) {
        if (!enabled) {
            sections = emptyList()
            ready = true
            return@LaunchedEffect
        }
        val ownId = ownUserId?.takeIf { it.isNotBlank() && it != channelId }
        val token = accessToken()?.takeIf { it.isNotBlank() }
        val clientId = BuildConfig.TWITCH_CLIENT_ID
        val channelTwitch = if (token != null && !channelId.isNullOrBlank()) {
            TwitchEmoteCatalogRepository.channelEmotes(clientId, token, channelId)
        } else {
            emptyList()
        }
        val ownTwitch = if (token != null && ownId != null) {
            TwitchEmoteCatalogRepository.channelEmotes(clientId, token, ownId)
        } else {
            emptyList()
        }
        val globalTwitch = if (token != null) {
            TwitchEmoteCatalogRepository.globalEmotes(clientId, token)
        } else {
            emptyList()
        }
        val personalTwitch = if (token != null && !ownUserId.isNullOrBlank()) {
            TwitchEmoteCatalogRepository.userEmotes(clientId, token, ownUserId)
        } else {
            null
        }
        sections = emotePickerSections(
            channel = ChannelEmoteSource(
                label = channelLabel,
                twitch = channelTwitch,
                sevenTv = SevenTvRepository.cachedChannel(channelId),
                bttv = BttvRepository.cachedChannel(channelId),
                ffz = FfzRepository.cachedChannel(channelId),
            ),
            own = ownId?.let {
                ChannelEmoteSource(
                    label = ownLabel?.ifBlank { null } ?: it,
                    twitch = ownTwitch,
                )
            },
            globalTwitch = globalTwitch,
            globalSevenTv = SevenTvRepository.cachedGlobal(),
            globalBttv = BttvRepository.cachedGlobal(),
            globalFfz = FfzRepository.cachedGlobal(),
            sevenTvEnabled = sevenTvEnabled,
            bttvEnabled = bttvEnabled,
            ffzEnabled = ffzEnabled,
            personalSmilies = personalTwitch?.smilies.orEmpty(),
            turboTwitch = personalTwitch?.turbo.orEmpty(),
            unlockedTwitch = personalTwitch?.unlocked.orEmpty(),
        )
        ready = true
    }
    return EmotePickerCatalog(ready, sections)
}

@Composable
internal fun EmotePickerPanel(
    catalog: EmotePickerCatalog,
    onPick: (String) -> Unit,
    // Null where the field has no `:` completion to search with.
    onSearch: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize()) {
        if (onSearch != null) EmotePickerSearchRow(onSearch)
        EmotePickerGrid(catalog, onPick, Modifier.weight(1f))
    }
}

/** Looks like a search field; a tap hands the search to the `:` completion in the message field. */
@Composable
private fun EmotePickerSearchRow(onSearch: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .height(36.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(TwitchSurfaceAlt)
            .clickable(role = Role.Button, onClick = onSearch)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = TwitchTextSecondary,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.emote_picker_search),
            color = TwitchTextSecondary,
            fontSize = 14.sp,
            maxLines = 1,
        )
    }
}

@Composable
private fun EmotePickerGrid(
    catalog: EmotePickerCatalog,
    onPick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!catalog.ready) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = TwitchPurple, strokeWidth = 2.dp)
        }
        return
    }
    BoxWithConstraints(modifier.fillMaxSize()) {
        val columns = (maxWidth / 52.dp).toInt().coerceAtLeast(1)
        LazyColumn(Modifier.fillMaxSize()) {
            catalog.sections.forEach { section ->
                val headerKey = "${section.place}-${section.kind}-${section.label}"
                stickyHeader(key = headerKey) {
                    Text(
                        text = emotePickerTitle(section),
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(TwitchSurface)
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        color = TwitchTextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
                items(
                    count = section.emotes.chunked(columns).size,
                    key = { index -> "$headerKey-$index" },
                ) { index ->
                    val row = section.emotes.chunked(columns)[index]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.Start,
                    ) {
                        row.forEach { emote ->
                            PickerEmoteCell(emote, onPick, Modifier.weight(1f))
                        }
                        repeat(columns - row.size) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun emotePickerTitle(section: EmotePickerSection): String {
    val platform = when (section.kind) {
        EmotePickerKind.SevenTv -> stringResource(R.string.chat_emote_platform_7tv)
        EmotePickerKind.Bttv -> stringResource(R.string.chat_emote_platform_bttv)
        EmotePickerKind.Ffz -> stringResource(R.string.chat_emote_platform_ffz)
        else -> ""
    }
    return when (section.place) {
        EmotePickerPlace.Frequent -> stringResource(R.string.emote_picker_frequent)
        EmotePickerPlace.Unlocked -> section.label
        EmotePickerPlace.Global -> when (section.kind) {
            EmotePickerKind.Twitch -> stringResource(R.string.emote_picker_twitch)
            else -> platform
        }
        else -> when (section.kind) {
            EmotePickerKind.Follow -> stringResource(R.string.emote_picker_channel_follow, section.label)
            EmotePickerKind.Subscriptions ->
                stringResource(R.string.emote_picker_channel_subscriptions, section.label)
            else -> stringResource(R.string.emote_picker_channel_platform, section.label, platform)
        }
    }
}

@Composable
private fun PickerEmoteCell(
    emote: PickerEmote,
    onPick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(R.string.emote_picker_emote, emote.name)
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(role = Role.Button, onClickLabel = description, onClick = { onPick(emote.name) }),
        contentAlignment = Alignment.Center,
    ) {
        SharedEmoteImage(
            url = emote.url,
            contentDescription = null,
            size = 28.dp,
            aspectRatio = emote.aspectRatio,
        )
    }
}
