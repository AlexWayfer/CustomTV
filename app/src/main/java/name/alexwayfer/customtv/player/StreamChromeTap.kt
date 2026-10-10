package name.alexwayfer.customtv.player

/**
 * The app shows or hides the chrome: the player's controls follow once per change. A repeated call
 * with the same state, such as a second expand, does not tap the player again.
 */
internal fun chromeChangeSyncsControls(headerExpanded: Boolean, minimizeControl: Boolean, visible: Boolean): Boolean =
    headerExpanded != visible || minimizeControl != visible

internal enum class PlayerControlsSync { None, Tap, ShowWithoutTap, HideWithoutTap }

/**
 * How the player's controls follow the wanted state, only when they differ from it. On a stream a tap toggles them.
 * On a recording a tap on the video also plays or pauses it, so the page shows or hides the controls without one.
 */
internal fun playerControlsSync(recording: Boolean, wanted: Boolean, controlsVisible: Boolean): PlayerControlsSync =
    when {
        wanted == controlsVisible -> PlayerControlsSync.None
        !recording -> PlayerControlsSync.Tap
        wanted -> PlayerControlsSync.ShowWithoutTap
        else -> PlayerControlsSync.HideWithoutTap
    }

internal fun playerTapShowsChrome(controlsVisible: Boolean): Boolean = !controlsVisible

/**
 * A tap on a recording's bare video only shows or hides the controls: the page, which would also play or pause it,
 * does not get it. A tap on a button or a note still reaches the page, and a stream's tap always does.
 */
internal fun playerTapStaysOffPage(recording: Boolean, touchOnVideo: Boolean): Boolean = recording && touchOnVideo

/** The stream info collapses on a timer, except while something on it, such as a menu, is open. */
internal fun headerCollapseTimerRuns(
    headerExpanded: Boolean,
    minimizeControl: Boolean,
    held: Boolean,
    videoLoading: Boolean,
): Boolean = !held && !videoLoading && (headerExpanded || minimizeControl)

/** Controls that appear on their own, such as when the video starts, bring the stream info along. */
internal fun controlsShowChrome(
    controlsVisible: Boolean,
    headerExpanded: Boolean,
    minimizeControl: Boolean,
    minimized: Boolean,
): Boolean = controlsVisible && !minimized && !(headerExpanded && minimizeControl)

/**
 * The stream info under the player stays collapsed while the keyboard or the emote picker is open, even with the
 * controls up, so the chat keeps its room.
 */
internal fun streamInfoExpandedUnderPlayer(headerExpanded: Boolean, composerOpen: Boolean): Boolean =
    headerExpanded && !composerOpen

/** Once the timer is over, the stream info still waits while the player's own controls are on screen. */
internal fun headerCollapsesAfterTimer(playerControlsVisible: Boolean): Boolean = !playerControlsVisible
