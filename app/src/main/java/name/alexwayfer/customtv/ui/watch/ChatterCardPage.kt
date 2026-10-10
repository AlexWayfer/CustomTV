package name.alexwayfer.customtv.ui.watch

/** The page the chatter card's sheet shows. */
internal enum class ChatterCardPage {
    Profile,

    /** The chatter's recent messages in this chat. */
    Messages,

    /** The channel's moderation logs on the chatter. */
    ModLogs,

    /** Every Chatter Labels label, to give to the chatter or take away. */
    Labels,

    /** The chatter's portrait, written on the device from their logs in this channel. */
    Portrait,
}
