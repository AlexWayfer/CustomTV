package name.alexwayfer.customtv.ui.account

import androidx.compose.runtime.Composable

/** Recordings play only in Premium, so the free Home does not offer one. */
@Suppress("unused")
internal fun profileHomePastBroadcast(videos: List<TwitchVideo>): TwitchVideo? = null

@Suppress("unused")
@Composable
internal fun ProfileHomePastBroadcast(
    video: TwitchVideo,
    channelLogin: String,
    chapters: VodCategories?,
    onOpenVideo: (ProfileVideoPlayback) -> Unit,
) = Unit
