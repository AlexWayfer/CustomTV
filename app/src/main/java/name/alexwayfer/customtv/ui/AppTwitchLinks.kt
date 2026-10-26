package name.alexwayfer.customtv.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import name.alexwayfer.customtv.links.LinkedVideos
import name.alexwayfer.customtv.links.TwitchLinkRequest
import name.alexwayfer.customtv.links.TwitchLinkTarget
import name.alexwayfer.customtv.links.loadLinkedClip
import name.alexwayfer.customtv.links.openInBrowser
import name.alexwayfer.customtv.links.twitchLinkTarget
import name.alexwayfer.customtv.ui.components.ChannelPreview

/**
 * Opens the twitch.tv links that Android or a tapped link hands to the app: a channel as a stream-start notification
 * does, a recording or a clip once it loads. One that does not load opens in the browser, so the link never
 * dead-ends.
 */
@Composable
internal fun AppTwitchLinkEffect(scene: AppScene) {
    val sceneLatest by rememberUpdatedState(scene)
    LaunchedEffect(Unit) {
        TwitchLinkRequest.url.collect { url ->
            if (url == null) return@collect
            TwitchLinkRequest.clear()
            when (val target = twitchLinkTarget(url)) {
                is TwitchLinkTarget.Channel ->
                    sceneLatest.openChannel(ChannelPreview(target.login), sceneLatest.shell.section == AppSection.Home)
                is TwitchLinkTarget.Video -> {
                    val video = LinkedVideos.load(target.id)
                    if (video != null) sceneLatest.openLinkedVideo(video) else openInBrowser(sceneLatest.activity, url)
                }
                is TwitchLinkTarget.Clip -> {
                    val clip = loadLinkedClip(target.slug)
                    if (clip != null) sceneLatest.openLinkedPlayback(clip) else openInBrowser(sceneLatest.activity, url)
                }
                null -> openInBrowser(sceneLatest.activity, url)
            }
        }
    }
}
