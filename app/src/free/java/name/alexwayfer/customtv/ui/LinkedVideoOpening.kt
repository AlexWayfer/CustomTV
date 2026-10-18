package name.alexwayfer.customtv.ui

import name.alexwayfer.customtv.links.LinkedVideo

/** The recording player is Premium: a twitch.tv link to a recording opens its channel instead. */
internal fun AppScene.openLinkedVideo(linked: LinkedVideo) {
    openChannel(linked.channelLogin, shell.section == AppSection.Home)
}
