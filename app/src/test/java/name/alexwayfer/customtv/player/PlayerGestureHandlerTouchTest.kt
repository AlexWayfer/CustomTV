package name.alexwayfer.customtv.player

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.view.InputDevice
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

private const val TOUCH_SLOP = 10

/** The status bar's band at the top of the screen; a touch below it starts at [DOWN_Y]. */
private const val STATUS_BAR_PX = 50
private const val DOWN_X = 100f
private const val DOWN_Y = 200f
private const val SWIPE_DOWN_PX = 60f
private const val FRAME_MILLIS = 16L
private const val PLAYER_WIDTH = 1600
private const val PLAYER_HEIGHT = 900
private const val CENTER_X = PLAYER_WIDTH / 2f
private const val CENTER_Y = PLAYER_HEIGHT / 2f

/** Writes down what the page under the gestures sees: the touch events it gets, and the clicks. */
@SuppressLint("ClickableViewAccessibility")
private class Page(context: Context) : View(context) {
    val actions = mutableListOf<String>()
    var clicks = 0

    override fun onTouchEvent(event: MotionEvent): Boolean {
        actions += MotionEvent.actionToString(event.actionMasked)
        return true
    }

    override fun performClick(): Boolean {
        clicks++
        return true
    }
}

/** Writes down when the player keeps its parent from taking the touch, and when it lets go. */
private class Parent(context: Context) : FrameLayout(context) {
    val disallowed = mutableListOf<Boolean>()

    override fun requestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
        disallowed += disallowIntercept
        super.requestDisallowInterceptTouchEvent(disallowIntercept)
    }
}

private class Finger(val id: Int, val x: Float, val y: Float)

@RunWith(RobolectricTestRunner::class)
// The plain application: the app's own starts its stores and workers, which a touch of the player does not need.
@Config(application = Application::class)
class PlayerGestureHandlerTouchTest {
    private val context: Context = RuntimeEnvironment.getApplication()
    private val page = Page(context)
    private val parent = Parent(context).apply { addView(page) }

    private var appTap = false
    private var canMinimize = true
    private var canDismissHorizontally = false
    private var controlsVisible = false
    private var touchOnControls = false
    private var touchOnDisclosure = false
    private var tapStaysOffPage = false
    private var canZoom = true

    /** What the gestures told the player, in order. */
    private val calls = mutableListOf<String>()
    private var endVelocity = 0f

    /** Each time the picture got zoomed or whole again. */
    private val zoomedChanges = mutableListOf<Boolean>()

    /** Whether the zoom rested zoomed, each time the fingers lifted. */
    private val settled = mutableListOf<Boolean>()
    private val zoom = PlayerZoomGesture(page, onSettled = { settled += it }) { transform ->
        if (transform.zoomed != (zoomedChanges.lastOrNull() ?: false)) zoomedChanges += transform.zoomed
    }

    init {
        parent.measure(
            View.MeasureSpec.makeMeasureSpec(PLAYER_WIDTH, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(PLAYER_HEIGHT, View.MeasureSpec.EXACTLY),
        )
        parent.layout(0, 0, PLAYER_WIDTH, PLAYER_HEIGHT)
        PlayerGestureHandler(
            view = page,
            touchSlop = TOUCH_SLOP,
            state = PlayerTouchState(
                isReleased = { false },
                isAppTap = { appTap },
                canMinimize = { canMinimize },
                canExpand = { true },
                canDismissHorizontally = { canDismissHorizontally },
                canZoom = { canZoom },
                controlsVisible = { controlsVisible },
                touchOnControls = { touchOnControls },
                touchOnScrollableContent = { false },
                touchOnDisclosure = { touchOnDisclosure },
                statusBarPx = { STATUS_BAR_PX },
                tapExpands = { false },
                tapStaysOffPage = { tapStaysOffPage },
            ),
            callbacks = PlayerTouchCallbacks(
                onTap = { reachedPage, x -> calls += if (reachedPage) "page tap $x" else "app tap $x" },
                onVerticalDrag = { calls += "vertical $it" },
                onVerticalDragEnd = { offset, velocity ->
                    calls += "vertical end $offset"
                    endVelocity = velocity
                },
                onHorizontalDrag = { calls += "horizontal $it" },
                onHorizontalDragEnd = { offset, velocity ->
                    calls += "horizontal end $offset"
                    endVelocity = velocity
                },
            ),
            zoom = zoom,
        ).install()
    }

    private var time = 0L

    /**
     * Sends the page an event of [action] with [fingers] on it, each frame a little later. The fingers are where they
     * touch the screen; the page gets them in its own coordinates, under its zoom, as its parent would hand them on.
     */
    private fun send(action: Int, vararg fingers: Finger) {
        val properties = fingers.map { finger ->
            MotionEvent.PointerProperties().apply {
                id = finger.id
                toolType = MotionEvent.TOOL_TYPE_FINGER
            }
        }
        val coords = fingers.map { finger ->
            MotionEvent.PointerCoords().apply {
                x = CENTER_X + (finger.x - CENTER_X - page.translationX) / page.scaleX
                y = CENTER_Y + (finger.y - CENTER_Y - page.translationY) / page.scaleY
                pressure = 1f
                size = 1f
            }
        }
        val event = MotionEvent.obtain(
            0L, time, action, fingers.size, properties.toTypedArray(), coords.toTypedArray(),
            0, 0, 1f, 1f, 0, 0, InputDevice.SOURCE_TOUCHSCREEN, 0,
        )
        time += FRAME_MILLIS
        page.dispatchTouchEvent(event)
        event.recycle()
    }

    private fun down(x: Float = DOWN_X, y: Float = DOWN_Y) = send(MotionEvent.ACTION_DOWN, Finger(0, x, y))

    private fun move(x: Float, y: Float) = send(MotionEvent.ACTION_MOVE, Finger(0, x, y))

    private fun up(x: Float, y: Float) = send(MotionEvent.ACTION_UP, Finger(0, x, y))

    private fun tap() {
        down()
        up(DOWN_X, DOWN_Y)
    }

    /** A finger put down, moved [SWIPE_DOWN_PX] down in two steps, and lifted. */
    private fun swipeDown(downY: Float = DOWN_Y) {
        down(y = downY)
        move(DOWN_X, downY + SWIPE_DOWN_PX / 2)
        move(DOWN_X, downY + SWIPE_DOWN_PX)
        up(DOWN_X, downY + SWIPE_DOWN_PX)
    }

    @Test
    fun `a tap on a stream reaches the page and the player`() {
        tap()

        assertEquals(listOf("ACTION_DOWN", "ACTION_UP"), page.actions)
        assertEquals(listOf("page tap $DOWN_X"), calls)
        assertEquals(1, page.clicks)
    }

    @Test
    fun `a tap on a recording's video is the app's alone`() {
        tapStaysOffPage = true

        tap()

        assertEquals(listOf("ACTION_DOWN", "ACTION_CANCEL"), page.actions)
        assertEquals(listOf("app tap $DOWN_X"), calls)
        assertEquals(1, page.clicks)
    }

    @Test
    fun `a tap on a disclosure opens it on the page and leaves the chrome`() {
        touchOnDisclosure = true

        tap()

        assertEquals(listOf("ACTION_DOWN", "ACTION_UP"), page.actions)
        assertEquals(emptyList<String>(), calls)
        assertEquals(1, page.clicks)
    }

    @Test
    fun `the app's own tap on the page goes straight to it`() {
        appTap = true

        tap()

        assertEquals(listOf("ACTION_DOWN", "ACTION_UP"), page.actions)
        assertEquals(emptyList<String>(), calls)
        assertEquals(0, page.clicks)
    }

    @Test
    fun `a drag down takes the touch from the page and minimizes`() {
        swipeDown()

        assertEquals(listOf("ACTION_DOWN", "ACTION_CANCEL"), page.actions)
        assertEquals(listOf("vertical 30.0", "vertical 60.0", "vertical end 60.0"), calls)
        assertTrue("velocity $endVelocity", endVelocity > 0f)
        assertEquals(listOf(true, false), parent.disallowed)
        assertEquals(0, page.clicks)
    }

    @Test
    fun `a drag down where the player cannot minimize stays the page's touch`() {
        canMinimize = false

        swipeDown()

        assertEquals(listOf("ACTION_DOWN", "ACTION_MOVE", "ACTION_MOVE", "ACTION_UP"), page.actions)
        assertEquals(emptyList<String>(), calls)
        assertEquals(0, page.clicks)
    }

    @Test
    fun `a moved touch on a recording's video reaches neither the page nor the player`() {
        canMinimize = false
        tapStaysOffPage = true

        swipeDown()

        assertEquals(listOf("ACTION_DOWN", "ACTION_MOVE", "ACTION_MOVE", "ACTION_CANCEL"), page.actions)
        assertEquals(emptyList<String>(), calls)
        assertEquals(0, page.clicks)
    }

    @Test
    fun `a swipe down from the status bar band is left to the system`() {
        swipeDown(downY =STATUS_BAR_PX - 20f)

        assertEquals(emptyList<String>(), calls)
    }

    @Test
    fun `a drag that starts on the shown controls stays with the page`() {
        controlsVisible = true
        touchOnControls = true

        swipeDown()

        assertEquals(listOf("ACTION_DOWN", "ACTION_MOVE", "ACTION_MOVE", "ACTION_UP"), page.actions)
        assertEquals(emptyList<String>(), calls)
    }

    @Test
    fun `a drag down on hidden controls still minimizes`() {
        touchOnControls = true

        swipeDown()

        assertEquals(listOf("vertical 30.0", "vertical 60.0", "vertical end 60.0"), calls)
    }

    @Test
    fun `a sideways drag dismisses where the player allows it`() {
        canDismissHorizontally = true

        down()
        move(DOWN_X + 30f, DOWN_Y)
        up(DOWN_X + 30f, DOWN_Y)

        assertEquals(listOf("ACTION_DOWN", "ACTION_CANCEL"), page.actions)
        assertEquals(listOf("horizontal 30.0", "horizontal end 30.0"), calls)
        assertTrue("velocity $endVelocity", endVelocity > 0f)
    }

    @Test
    fun `a drag ends when its finger lifts before a second one`() {
        down()
        move(DOWN_X, DOWN_Y + 60f)
        val first = Finger(0, DOWN_X, DOWN_Y + 80f)
        val second = Finger(1, 300f, 500f)
        send(MotionEvent.ACTION_POINTER_DOWN or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT), first, second)
        // The first finger lifts while the second stays down: no up of the first comes after this.
        send(MotionEvent.ACTION_POINTER_UP, first, second)
        send(MotionEvent.ACTION_MOVE, Finger(1, 300f, 600f))
        send(MotionEvent.ACTION_UP, Finger(1, 300f, 600f))

        assertEquals(listOf("vertical 60.0", "vertical end 80.0"), calls)
        assertEquals(listOf(true, false), parent.disallowed)
        assertEquals(emptyList<Boolean>(), zoomedChanges)
    }

    /**
     * Two fingers put down [startSpan] apart across the center, spread to [span] apart in [steps], and lifted. One
     * step keeps the zoom exact: the next step comes back through the zoomed view's coordinates, rounded.
     */
    private fun pinch(startSpan: Float, span: Float, steps: Int = 1, lift: Boolean = true) {
        down(CENTER_X - startSpan / 2, CENTER_Y)
        send(
            POINTER_1_DOWN,
            Finger(0, CENTER_X - startSpan / 2, CENTER_Y),
            Finger(1, CENTER_X + startSpan / 2, CENTER_Y),
        )
        var fingers = emptyArray<Finger>()
        for (step in 1..steps) {
            val half = (startSpan + (span - startSpan) * step / steps) / 2
            fingers = arrayOf(Finger(0, CENTER_X - half, CENTER_Y), Finger(1, CENTER_X + half, CENTER_Y))
            send(MotionEvent.ACTION_MOVE, *fingers)
        }
        if (!lift) return
        send(POINTER_1_UP, *fingers)
        up(fingers[0].x, fingers[0].y)
    }

    @Test
    fun `two fingers spreading zoom the picture, away from the page and the player`() {
        pinch(startSpan = 200f, span = 400f)

        assertEquals(PlayerZoomTransform(scale = 2f), zoom.transform)
        assertEquals(2f, page.scaleX)
        assertEquals(listOf("ACTION_DOWN", "ACTION_CANCEL"), page.actions)
        assertEquals(emptyList<String>(), calls)
        assertEquals(listOf(true, false), parent.disallowed)
        assertEquals(0, page.clicks)
    }

    @Test
    fun `one finger moves a zoomed picture instead of minimizing it`() {
        pinch(startSpan = 200f, span = 400f)
        page.actions.clear()

        down(CENTER_X, CENTER_Y)
        move(CENTER_X + 50f, CENTER_Y + 30f)
        move(CENTER_X + 100f, CENTER_Y + 60f)
        up(CENTER_X + 100f, CENTER_Y + 60f)

        assertEquals(PlayerZoomTransform(scale = 2f, x = 100f, y = 60f), zoom.transform)
        assertEquals(emptyList<String>(), page.actions)
        assertEquals(emptyList<String>(), calls)
    }

    @Test
    fun `a tap on a zoomed picture does nothing`() {
        pinch(startSpan = 200f, span = 400f)
        page.actions.clear()

        tap()

        assertEquals(emptyList<String>(), page.actions)
        assertEquals(emptyList<String>(), calls)
        assertEquals(0, page.clicks)
    }

    @Test
    fun `a pinch back brings the whole picture, and the swipes work again`() {
        pinch(startSpan = 200f, span = 400f)

        pinch(startSpan = 400f, span = 100f, steps = 3)

        assertEquals(PlayerZoomTransform(), zoom.transform)
        assertEquals(listOf(true, false), zoomedChanges)
        assertEquals(listOf(true, false), settled)

        swipeDown()

        assertEquals(listOf("vertical 30.0", "vertical 60.0", "vertical end 60.0"), calls)
    }

    @Test
    fun `a barely zoomed picture goes back to the whole one when the fingers lift`() {
        pinch(startSpan = 200f, span = 210f)

        assertEquals(PlayerZoomTransform(), zoom.transform)
        assertEquals(listOf(true, false), zoomedChanges)
        // The quality stays: the zoom never rested zoomed.
        assertEquals(listOf(false), settled)
    }

    @Test
    fun `a cancelled pinch keeps its zoom and lets the parent have touches again`() {
        pinch(startSpan = 200f, span = 400f, lift = false)
        assertEquals(emptyList<Boolean>(), settled)
        send(MotionEvent.ACTION_CANCEL, Finger(0, CENTER_X - 200f, CENTER_Y), Finger(1, CENTER_X + 200f, CENTER_Y))

        assertEquals(2f, zoom.transform.scale)
        assertEquals(listOf(true), settled)
        assertEquals(listOf(true, false), parent.disallowed)
    }

    @Test
    fun `a player that cannot zoom leaves two fingers to the page`() {
        canZoom = false

        pinch(startSpan = 200f, span = 400f)

        assertEquals(PlayerZoomTransform(), zoom.transform)
        assertEquals(emptyList<Boolean>(), zoomedChanges)
        assertEquals("ACTION_UP", page.actions.last())
    }
}

private const val POINTER_1_DOWN = MotionEvent.ACTION_POINTER_DOWN or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT)
private const val POINTER_1_UP = MotionEvent.ACTION_POINTER_UP or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT)
