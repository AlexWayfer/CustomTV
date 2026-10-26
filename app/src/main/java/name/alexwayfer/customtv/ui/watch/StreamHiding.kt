package name.alexwayfer.customtv.ui.watch

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Constraints
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import name.alexwayfer.customtv.data.GestureHint
import name.alexwayfer.customtv.player.PlayerPlayback
import name.alexwayfer.customtv.player.StreamPlayback
import name.alexwayfer.customtv.ui.LocalGestureHints

/**
 * The stream screen's chat-only mode: the player slides up out of sight and pauses, and the stream info and the chat
 * take its room. It lasts while the stream stays paused; playing it again anywhere, such as in the mini player or
 * picture-in-picture, shows the stream.
 */
@Stable
internal class StreamHiding(
    private val scope: CoroutineScope,
    private val pause: () -> Unit,
    private val play: () -> Unit,
) {
    /** The stream is hidden or on its way out; false once it is on its way back. */
    var hidden by mutableStateOf(false)
        private set

    /**
     * In landscape the stream slides over the chat as the full screen picture, out of full screen and back to it,
     * instead of opening its room above the stream info.
     */
    var overScreen by mutableStateOf(false)

    private val settled = Animatable(0f)
    private var dragStartHidden by mutableStateOf<Boolean?>(null)
    private var dragFraction by mutableFloatStateOf(0f)
    private var settleJob: Job? = null

    /** How far the stream is out of sight, from 0 shown to 1 gone; read when laid out or drawn. */
    val fraction: Float
        get() = if (dragStartHidden != null) dragFraction else settled.value

    /** Some of the stream is out of sight; changes once at the start and once at the end of a motion. */
    val partlyHidden: Boolean by derivedStateOf { fraction > 0f }

    /** The stream is all out of sight. */
    val fullyHidden: Boolean by derivedStateOf { fraction >= 1f }

    /**
     * In landscape the full screen stream info stays under the video while the stream slides out of full screen from
     * it, or back to it; the stream info over the chat takes over once the stream is on its way to hiding.
     */
    val keepsFullscreenInfo: Boolean
        get() = overScreen && partlyHidden && !hidden

    /** How much of the player's room over the stream info is closed; read when laid out. */
    val roomClosed: Float
        get() = streamRoomClosedFraction(fraction, overScreen)

    /** The full player's video covers the whole screen; read in composition, it changes at a motion's ends. */
    fun coversScreen(collapse: Float): Boolean = hiddenStreamCoversScreen(overScreen, partlyHidden, collapse)

    /** Hides or shows the stream, as the chat menu's switch does. */
    fun set(hide: Boolean) {
        if (hide == hidden) return
        switchTo(hide)
        settleTo(if (hide) 1f else 0f)
    }

    /** Follows a drag on the stream info, [offsetY] from where it started; up hides, down shows. */
    fun drag(offsetY: Float, travelPx: Float) {
        val startHidden = dragStartHidden ?: hidden.also { start ->
            settleJob?.cancel()
            dragStartHidden = start
        }
        dragFraction = streamHideDragProgress(startHidden, offsetY, travelPx)
    }

    /**
     * Ends a drag: past the threshold, or flung that way, hides or shows the stream; short of it, slides it back.
     * True when the stream switched.
     */
    fun dragEnd(offsetY: Float, velocityY: Float, travelPx: Float): Boolean {
        val startHidden = dragStartHidden ?: return false
        val progress = streamHideDragProgress(startHidden, offsetY, travelPx)
        val hide = streamHiddenAfterDrag(startHidden, progress, velocityY)
        val switched = hide != hidden
        if (switched) switchTo(hide)
        settleFromDrag(if (hide) 1f else 0f)
        return switched
    }

    /** A cancelled drag slides the stream back to where it was. */
    fun dragCancel() {
        val startHidden = dragStartHidden ?: return
        settleFromDrag(if (startHidden) 1f else 0f)
    }

    /** The stream played again while hidden, such as from the mini player or a headset: it shows. */
    fun playbackChanged(playback: PlayerPlayback) {
        if (!hidden || streamStaysHidden(hidden = true, playback = playback)) return
        hidden = false
        if (dragStartHidden == null) settleTo(0f)
    }

    private fun switchTo(hide: Boolean) {
        hidden = hide
        if (hide) pause() else play()
    }

    private fun settleTo(target: Float) {
        settleJob?.cancel()
        settleJob = scope.launch { settled.animateTo(target, PlayerCollapseTween) }
    }

    /** Hands the place over from the finger to the settling motion in the same frame, so it does not jump. */
    private fun settleFromDrag(target: Float) {
        val from = dragFraction
        settleJob?.cancel()
        settleJob = scope.launch {
            settled.snapTo(from)
            dragStartHidden = null
            settled.animateTo(target, PlayerCollapseTween)
        }
    }
}

/** The stream screen's [StreamHiding] for [channel]: a new channel opens with its stream shown. */
@Composable
internal fun rememberStreamHiding(channel: String, overScreen: Boolean): StreamHiding {
    val scope = rememberCoroutineScope()
    val hiding = remember(channel) { StreamHiding(scope, StreamPlayback::pause, StreamPlayback::play) }
    SideEffect { hiding.overScreen = overScreen }
    LaunchedEffect(hiding) {
        StreamPlayback.playback.collect { playback -> hiding.playbackChanged(playback) }
    }
    return hiding
}

/** What a drag on the stream info reads beyond the hiding: how far it travels and where the info is on screen. */
@Stable
internal class StreamHideDrag {
    var travelPx = 1f
    var onSwitched: () -> Unit = {}
    var coordinates: LayoutCoordinates? = null
}

@Composable
internal fun rememberStreamHideDrag(travelPx: Float, onSwitched: () -> Unit): StreamHideDrag {
    val drag = remember { StreamHideDrag() }
    SideEffect {
        drag.travelPx = travelPx
        drag.onSwitched = onSwitched
    }
    return drag
}

/** The stream screen's drag on the stream info: it travels the full player's height, and counts for the gesture tip. */
@Composable
internal fun rememberWatchStreamHideDrag(hiding: StreamHiding): StreamHideDrag {
    val gestureHints = LocalGestureHints.current
    val size = LocalWindowInfo.current.containerSize
    val travelPx = streamHideTravelPx(size.width, size.height, hiding.overScreen)
    return rememberStreamHideDrag(travelPx) { gestureHints?.gestureUsed(GestureHint.HideStream) }
}

/**
 * A drag up on the stream info hides the stream, and a drag down brings it back. The info moves along with the
 * finger, so the drag is measured on the screen rather than on the info.
 */
internal fun Modifier.streamHideDrag(hiding: StreamHiding, drag: StreamHideDrag): Modifier =
    onPlaced { drag.coordinates = it }
        .pointerInput(hiding, drag) {
            val velocity = VelocityTracker()
            var startY = Float.NaN
            var offsetY = 0f
            fun screenY(change: PointerInputChange): Float =
                drag.coordinates?.takeIf { it.isAttached }?.localToRoot(change.position)?.y ?: change.position.y
            try {
                detectVerticalDragGestures(
                    onDragStart = {
                        velocity.resetTracking()
                        startY = Float.NaN
                        offsetY = 0f
                    },
                    onDragEnd = {
                        val switched = hiding.dragEnd(offsetY, velocity.calculateVelocity().y, drag.travelPx)
                        if (switched) drag.onSwitched()
                    },
                    onDragCancel = hiding::dragCancel,
                ) { change, dragAmount ->
                    val y = screenY(change)
                    if (startY.isNaN()) startY = y - dragAmount
                    velocity.addPosition(change.uptimeMillis, Offset(0f, y))
                    change.consume()
                    offsetY = y - startY
                    hiding.drag(offsetY, drag.travelPx)
                }
            } catch (e: CancellationException) {
                hiding.dragCancel()
                throw e
            }
        }

/**
 * Slides the video up out of its box as the stream hides, smaller and fading, with its bottom on the stream info
 * that follows it up. [collapse]: how far the player is on its way to the corner, where the video comes back.
 * The layer exists only while some of the stream is hidden: around the WebView at rest it would draw every frame
 * into an offscreen layer.
 */
internal fun Modifier.hiddenStreamMotion(hiding: StreamHiding?, collapse: Float): Modifier {
    if (hiding == null || !hiding.partlyHidden) return this
    return graphicsLayer {
        val hidden = hiddenVideoFraction(hiding.fraction, collapse)
        val scale = 1f - (1f - HIDDEN_VIDEO_SCALE) * hidden
        transformOrigin = TransformOrigin(TransformOrigin.Center.pivotFractionX, 1f)
        scaleX = scale
        scaleY = scale
        translationY = -size.height * hidden
        alpha = 1f - hidden
    }
}

/**
 * In landscape, moves the stream info and the chat down under the video as it comes over them, and back up as it
 * leaves; [roomPx] is the full screen stream info's room under the video, read when drawn. The layer exists only
 * while some of the stream is hidden there.
 */
internal fun Modifier.hiddenStreamContentShift(hiding: StreamHiding?, roomPx: () -> Int): Modifier {
    if (hiding == null || !hiding.overScreen || !hiding.partlyHidden) return this
    return graphicsLayer {
        translationY = hiddenStreamContentShiftPx(size.height, hiding.fraction, overScreen = true, roomPx = roomPx())
    }
}

/**
 * Lays out the video that covers the screen while the stream slides out of full screen or back, [widthPx] by
 * [heightPx] but the full screen stream info's room under it ([roomPx], read when laid out).
 */
internal fun Modifier.streamCover(widthPx: Int, heightPx: Int, roomPx: () -> Int): Modifier =
    layout { measurable, _ ->
        val placeable = measurable.measure(
            Constraints.fixed(widthPx.coerceAtLeast(0), streamCoverHeightPx(heightPx.coerceAtLeast(0), roomPx())),
        )
        layout(placeable.width, placeable.height) { placeable.placeRelative(0, 0) }
    }

/** The player's room over the stream info, closing as the stream hides; [hiding] null keeps it whole. */
internal fun Modifier.streamRoom(hiding: StreamHiding?): Modifier =
    if (hiding == null) this else collapsingHeight { hiding.roomClosed }

/** Reports [fraction] less of the height its content takes, read when laid out. */
internal fun Modifier.collapsingHeight(fraction: () -> Float): Modifier =
    layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        layout(placeable.width, streamRoomHeightPx(placeable.height, fraction())) {
            placeable.placeRelative(0, 0)
        }
    }
