package name.alexwayfer.customtv.data

data class AppSettings(
    val sevenTvEmotes: Boolean = true,
    val ffzEmotes: Boolean = true,
    val bttvEmotes: Boolean = true,
    val sevenTvBadges: Boolean = true,
    /** 7TV's gradients and pictures over chatters' names. */
    val sevenTvPaints: Boolean = true,
    /** FFZ's chatter badges, and the channel's custom moderator and VIP badges from FFZ. */
    val ffzBadges: Boolean = true,
    val bttvBadges: Boolean = true,
    val meMessageItalic: Boolean = true,
    val highlightFirstMessages: Boolean = false,
    val markRaiders: Boolean = true,
    /** Premium: a badge with the place of each of the channel's top chatters, as StreamElements counts them. */
    val topChatterBadges: Boolean = true,
    val highlightMentions: Boolean = true,
    val mentionVibration: Boolean = true,
    val mentionVibrationMs: Int = 400,
    val mentionVibrationPercent: Int = 80,
    val mentionSound: Boolean = false,
    val mentionSoundUri: String = "",
    val keywordPhrases: List<KeywordPhrase> = emptyList(),
    val temporarilyPinHighlightedMessages: Boolean = false,
    val highlightPinSeconds: Int = 15,
    val loadRecentChatOnOpen: Boolean = true,
    val emoteCompletionWithoutColon: Boolean = false,
    val keepKeyboardAfterSend: Boolean = false,
    val scrollToLatestAfterSend: Boolean = true,
    /** A button in the message field that brings back the user's own messages in the chat, and Up and Down keys. */
    val sentMessageHistory: Boolean = true,
    /** Premium: chatters' messages with one of these phrases do not show in chat. */
    val hiddenPhrases: List<KeywordPhrase> = emptyList(),
    /** Whether [hiddenPhrases] also hide messages in the channels the user moderates. */
    val hidePhrasesWhenModerator: Boolean = false,
    val backgroundSoundOnly: Boolean = true,
    /** The player's sound fades in as it starts or plays, and out as it pauses or closes. */
    val smoothSound: Boolean = true,
    val streamStartNotifications: Boolean = true,
    val streamChangeNotifications: Boolean = false,
    val watchStreakNotifications: Boolean = true,
    /** A red glow along the bottom of an expanded player while a whisper is unread. */
    val whisperUnreadGlow: Boolean = true,
    /** The home screen widget lists the channels that went offline last under the live ones. */
    val liveWidgetEnded: Boolean = true,
    val linkPreviewMode: LinkPreviewMode = LinkPreviewMode.None,
    val autoClaimBonus: Boolean = false,
    val portraitLanguage: PortraitLanguage = PortraitLanguage.System,
    val portraitModel: PortraitModelKind = PortraitModelKind.GroqQwen,
    /** The sleep timer's time set last, in minutes, which its picker opens on; zero before the first one. */
    val sleepTimerMinutes: Int = 0,
) {
    /** Title and category alerts go with the go-live ones: with those off, the saved switch waits and sends nothing. */
    val streamChangesActive: Boolean get() = streamStartNotifications && streamChangeNotifications
}
