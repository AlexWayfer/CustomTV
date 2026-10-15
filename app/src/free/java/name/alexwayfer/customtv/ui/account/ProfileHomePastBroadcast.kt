package name.alexwayfer.customtv.ui.account

import androidx.compose.runtime.Composable

/** Recordings play only in Premium, so the free Home does not offer one. */
@Suppress("unused", "FunctionOnlyReturningConstant")
internal fun profileHomePastBroadcast(videos: List<TwitchVideo>): TwitchVideo? = null

@Suppress("unused")
@Composable
internal fun ProfileHomePastBroadcast(
    video: TwitchVideo,
    channelLogin: String,
    chapters: VideoChaptersLoad,
    onRetryChapters: () -> Unit,
    onOpenVideo: (ProfileVideoPlayback) -> Unit,
) = Unit
