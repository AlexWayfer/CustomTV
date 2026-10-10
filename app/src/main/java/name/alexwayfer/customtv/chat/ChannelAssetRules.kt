package name.alexwayfer.customtv.chat

import name.alexwayfer.customtv.data.AppSettings

/** Third-party emote sources the settings turn on. A source that is off is not loaded. */
internal data class EmoteSources(
    val sevenTv: Boolean,
    val ffz: Boolean,
    val bttv: Boolean,
)

internal fun emoteSources(settings: AppSettings?): EmoteSources {
    val current = settings ?: AppSettings()
    return EmoteSources(
        sevenTv = current.sevenTvEmotes,
        ffz = current.ffzEmotes,
        bttv = current.bttvEmotes,
    )
}

/** A result loaded for [requested] is dropped once the chat has moved to another channel. */
internal fun channelResultApplies(requested: String, current: String?): Boolean = requested == current

/** Every enabled source has this channel's emotes. A source that is off is not waited for. */
internal fun emoteSourcesReady(
    channelId: String?,
    sources: EmoteSources,
    sevenTvLoaded: Boolean,
    ffzLoaded: Boolean,
    bttvLoaded: Boolean,
): Boolean {
    return !channelId.isNullOrBlank() &&
        (!sources.sevenTv || sevenTvLoaded) &&
        (!sources.ffz || ffzLoaded) &&
        (!sources.bttv || bttvLoaded)
}

/** An empty result replaces the shown emotes only when the source confirms the channel has none. */
internal fun emoteLoadPublishes(emoteCount: Int, sourceLoaded: Boolean): Boolean =
    emoteCount > 0 || sourceLoaded

/** Turning a source on while a channel is open loads it. Turning one off keeps what is loaded. */
internal fun emoteSourceTurnedOn(previous: EmoteSources, next: EmoteSources): Boolean =
    (next.sevenTv && !previous.sevenTv) ||
        (next.ffz && !previous.ffz) ||
        (next.bttv && !previous.bttv)
