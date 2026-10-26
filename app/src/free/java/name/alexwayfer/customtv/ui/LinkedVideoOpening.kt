package name.alexwayfer.customtv.ui

import name.alexwayfer.customtv.links.LinkedVideo
import name.alexwayfer.customtv.ui.components.ChannelPreview

/** The recording player is Premium: a twitch.tv link to a recording opens its channel instead. */
internal fun AppScene.openLinkedVideo(linked: LinkedVideo) {
    openChannel(ChannelPreview(linked.channelLogin), shell.section == AppSection.Home)
}
