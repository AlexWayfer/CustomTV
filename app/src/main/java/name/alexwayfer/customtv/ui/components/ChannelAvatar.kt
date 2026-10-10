package name.alexwayfer.customtv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.data.ChannelAvatarRepository
import name.alexwayfer.customtv.data.ChannelProfile
import name.alexwayfer.customtv.ui.theme.TwitchSurfaceAlt
import kotlin.time.Duration
import kotlin.time.TimeSource

@Composable
fun rememberChannelProfile(
    login: String,
    pollEvery: Duration? = null,
    forceRefresh: Boolean = false,
): ChannelProfile {
    val fallback = remember(login) {
        ChannelProfile(
            login = login.lowercase(),
            displayName = login,
            avatarUrl = null,
        )
    }
    val profile by produceState(
        initialValue = ChannelAvatarRepository.cached(login) ?: fallback,
        login,
        pollEvery,
        forceRefresh,
        fallback,
    ) {
        val cached = ChannelAvatarRepository.cached(login)
        value = cached ?: fallback
        val updates = launch {
            ChannelAvatarRepository.profileUpdates.collect { updated ->
                if (updated.login.equals(login, ignoreCase = true)) value = updated
            }
        }
        var force = forceRefresh
        try {
        while (true) {
            val started = TimeSource.Monotonic.markNow()
            try {
                val fresh = ChannelAvatarRepository.refresh(login, force = force)
                force = false
                if (fresh != null) {
                    value = fresh
                }
            } catch (_: CancellationException) {
                currentCoroutineContext().ensureActive()
                continue
            } catch (_: Throwable) {
                // Keep the last known profile and retry on the next interval.
            }
            if (pollEvery == null) break
            val remaining = pollEvery - started.elapsedNow()
            if (remaining.isPositive()) {
                delay(remaining)
            }
        }
        } finally {
            updates.cancel()
        }
    }
    return profile
}

@Composable
fun ChannelAvatar(
    channel: String,
    profile: ChannelProfile?,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    grayscale: Boolean = false,
) {
    val displayName = profile?.displayName ?: channel
    val avatarDescription = stringResource(R.string.channel_avatar, displayName)
    val url = profile?.avatarUrl
    val grayscaleFilter = remember {
        ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(TwitchSurfaceAlt)
            .semantics { contentDescription = avatarDescription },
        contentAlignment = Alignment.Center,
    ) {
        if (url != null) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(url)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .crossfade(false)
                    .build(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape),
                contentScale = ContentScale.Crop,
                colorFilter = if (grayscale) grayscaleFilter else null,
            )
        }
    }
}
