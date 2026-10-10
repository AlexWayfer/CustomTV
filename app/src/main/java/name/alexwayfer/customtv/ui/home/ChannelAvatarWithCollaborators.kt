package name.alexwayfer.customtv.ui.home

import android.os.SystemClock
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.Coil
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import name.alexwayfer.customtv.data.ChannelProfile
import name.alexwayfer.customtv.ui.components.ChannelAvatar
import name.alexwayfer.customtv.ui.theme.TwitchSurfaceAlt
import kotlin.time.Duration.Companion.milliseconds

private const val COLLABORATOR_AVATAR_SCALE = 0.75f
private val COLLABORATOR_AVATAR_OVERLAP = 6.dp
private val CHANNEL_AVATAR_RING = 2.dp

/**
 * The channel avatar with the collaborators' avatars tucked under its right edge.
 * They appear once every avatar has loaded and then take turns on the shared clock;
 * the channel avatar keeps the accessibility description.
 */
@Composable
internal fun ChannelAvatarWithCollaborators(
    channel: String,
    profile: ChannelProfile?,
    collaboratorAvatarUrls: List<String>,
    ringColor: Color,
    size: Dp,
    modifier: Modifier = Modifier,
    grayscale: Boolean = false,
) {
    val loaded = rememberLoadedCollaboratorAvatars(collaboratorAvatarUrls)
    if (collaboratorAvatarUrls.isEmpty() || loaded?.urls?.isEmpty() == true) {
        ChannelAvatar(
            channel = channel,
            profile = profile,
            modifier = modifier,
            size = size,
            grayscale = grayscale,
        )
        return
    }
    Box(modifier = modifier, contentAlignment = Alignment.CenterStart) {
        val slot = Modifier
            .padding(start = size - COLLABORATOR_AVATAR_OVERLAP)
            .size(size * COLLABORATOR_AVATAR_SCALE)
        // The slot keeps its place while the avatars load, so the row text does not jump.
        if (loaded == null) {
            Spacer(slot)
        } else {
            CollaboratorAvatarCarousel(avatars = loaded, modifier = slot)
        }
        ChannelAvatar(
            channel = channel,
            profile = profile,
            modifier = Modifier.drawBehind {
                drawCircle(
                    color = ringColor,
                    radius = this.size.minDimension / 2f + CHANNEL_AVATAR_RING.toPx(),
                )
            },
            size = size,
            grayscale = grayscale,
        )
    }
}

/** The collaborator avatars that loaded, in order, and when their carousel started. */
private class LoadedCollaboratorAvatars(val urls: List<String>, val startedAtMillis: Long)

/**
 * Fully loaded sets by the requested URLs, kept for the process, so a row scrolled back
 * into view continues its carousel instead of starting over.
 */
private val loadedCollaboratorAvatars = mutableMapOf<List<String>, LoadedCollaboratorAvatars>()

/**
 * The loaded avatars; null while the first set is still loading.
 * A changed set keeps showing the previous one until the new one has loaded.
 */
@Composable
private fun rememberLoadedCollaboratorAvatars(urls: List<String>): LoadedCollaboratorAvatars? {
    val context = LocalContext.current
    val loaded by produceState(loadedCollaboratorAvatars[urls], urls) {
        loadedCollaboratorAvatars[urls]?.let { known ->
            value = known
            return@produceState
        }
        val loader = Coil.imageLoader(context)
        val ready = coroutineScope {
            urls.map { url ->
                async {
                    val request = ImageRequest.Builder(context).data(url).build()
                    url.takeIf { loader.execute(request) is SuccessResult }
                }
            }.awaitAll().filterNotNull()
        }
        val set = LoadedCollaboratorAvatars(ready, SystemClock.uptimeMillis())
        // A set with a failed avatar is not kept, so the next appearance tries it again.
        if (ready.size == urls.size) loadedCollaboratorAvatars[urls] = set
        value = set
    }
    return loaded
}

@Composable
private fun CollaboratorAvatarCarousel(
    avatars: LoadedCollaboratorAvatars,
    modifier: Modifier = Modifier,
) {
    val urls = avatars.urls
    val startedAt = avatars.startedAtMillis
    val index by produceState(
        collaboratorAvatarIndex(SystemClock.uptimeMillis(), startedAt, urls.size),
        avatars,
    ) {
        value = collaboratorAvatarIndex(SystemClock.uptimeMillis(), startedAt, urls.size)
        if (urls.size <= 1) return@produceState
        while (true) {
            delay(millisUntilNextCollaboratorAvatar(SystemClock.uptimeMillis(), startedAt).milliseconds)
            value = collaboratorAvatarIndex(SystemClock.uptimeMillis(), startedAt, urls.size)
        }
    }
    // The new avatar slides up from below while the previous one slides out the top.
    AnimatedContent(
        targetState = urls[index.coerceAtMost(urls.lastIndex)],
        modifier = modifier
            .clip(CircleShape)
            .clearAndSetSemantics {},
        transitionSpec = {
            slideInVertically { height -> height } togetherWith slideOutVertically { height -> -height }
        },
        label = "collaboratorAvatar",
    ) { url ->
        AsyncImage(
            model = url,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(TwitchSurfaceAlt),
            contentScale = ContentScale.Crop,
        )
    }
}
