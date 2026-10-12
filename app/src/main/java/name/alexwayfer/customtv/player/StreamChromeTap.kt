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

internal enum class PlayerTouchEnd {
    /** A tap the page gets too, such as on a button; the chrome follows the controls the page toggles with it. */
    PageTap,
    /** A tap only the app gets: it shows or hides the chrome, seeks, or expands the mini player. */
    AppTap,
    /** A tap on a disclosure, such as the audience plaque: the page opens or closes it, and the chrome stays. */
    DisclosureTap,
    /** The finger moved past a tap without starting a drag; the page gets the touch as it is. */
    PageTouch,
    /** The same touch where only the app takes taps: the page, which would pause a recording on it, does not get it. */
    Dropped,
}

/** What a finger lifted from the player without a drag does; [offPage]: a tap there is the app's alone. */
internal fun playerTouchEnd(withinTapSlop: Boolean, offPage: Boolean, onDisclosure: Boolean): PlayerTouchEnd =
    when {
        offPage -> if (withinTapSlop) PlayerTouchEnd.AppTap else PlayerTouchEnd.Dropped
        !withinTapSlop -> PlayerTouchEnd.PageTouch
        onDisclosure -> PlayerTouchEnd.DisclosureTap
        else -> PlayerTouchEnd.PageTap
    }

/** The stream info collapses on a timer, except while something on it, such as a menu, is open. */
internal fun headerCollapseTimerRuns(
    headerExpanded: Boolean,
    minimizeControl: Boolean,
    held: Boolean,
    videoLoading: Boolean,
): Boolean = !held && !videoLoading && (headerExpanded || minimizeControl)

internal enum class ShrinkChrome { Hide, Show, Keep }

/**
 * The chrome hides while the player is shrunk, into the corner or picture-in-picture, and shows in full once the
 * player is back to its full size, so the stream info and the controls come back together.
 */
internal fun chromeOnPlayerShrink(shrunk: Boolean, wasShrunk: Boolean): ShrinkChrome =
    when {
        shrunk -> ShrinkChrome.Hide
        wasShrunk -> ShrinkChrome.Show
        else -> ShrinkChrome.Keep
    }

/** Controls that appear on their own, such as when the video starts, bring the stream info along. */
internal fun controlsShowChrome(
    controlsVisible: Boolean,
    headerExpanded: Boolean,
    minimizeControl: Boolean,
    minimized: Boolean,
): Boolean = controlsVisible && !minimized && !(headerExpanded && minimizeControl)

/**
 * The stream info under the player leaves beside a full screen player, and while the keyboard or the emote picker is
 * open, even with the controls up, so the chat keeps its room.
 */
internal fun streamInfoShownUnderPlayer(fullscreen: Boolean, composerOpen: Boolean): Boolean =
    !fullscreen && !composerOpen

/** Once the timer is over, the stream info still waits while the player's own controls are on screen. */
internal fun headerCollapsesAfterTimer(playerControlsVisible: Boolean): Boolean = !playerControlsVisible
