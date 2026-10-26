package name.alexwayfer.customtv.player

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.os.SystemClock
import android.view.ViewConfiguration
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.createBitmap
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.currentStateAsState
import name.alexwayfer.customtv.data.AppSettingsStore
import name.alexwayfer.customtv.diagnostics.AppLog
import name.alexwayfer.customtv.ui.settings.beforeRestore
import kotlin.math.roundToInt

internal const val PLAYER_LOG_TAG = "Player"

/** What the player page reports back to the screen around it. */
internal data class PlayerEvents(
    val onPositionSeconds: (Double) -> Unit = {},
    val onSeekSeconds: (Double) -> Unit = {},
    val onTap: (playerControlsVisible: Boolean, reachedPage: Boolean) -> Unit = { _, _ -> },
    val onControlsVisible: (Boolean) -> Unit = {},
    val onControlBarHeightPx: (Int) -> Unit = {},
    /** Where a recording's seek bar sits on the page. */
    val onSeekbar: (PlayerSeekbar) -> Unit = {},
    val onSettingsMenuOpen: (Boolean) -> Unit = {},
    val onWaitingForVideo: (Boolean) -> Unit = {},
    val onDismissDrag: (offsetY: Float) -> Unit = {},
    val onDismissDragEnd: (offsetY: Float, velocityY: Float) -> Unit = { _, _ -> },
    val onHorizontalDrag: (offsetX: Float) -> Unit = {},
    val onHorizontalDragEnd: (offsetX: Float, velocityX: Float) -> Unit = { _, _ -> },
    /** A pinch zoomed the picture in: the chrome and the player's controls hide. */
    val onZoomIn: () -> Unit = {},
    /** Takes the page's control matcher, so the app's own buttons line up with the player's controls. */
    val chrome: PlayerChrome? = null,
)

/** Which swipes and taps on the player move it rather than reach the page. */
internal data class PlayerGestures(
    val enableMinimizeSwipe: Boolean = true,
    val enableExpandSwipe: Boolean = false,
    val enableHorizontalDismiss: Boolean = false,
    val tapExpands: Boolean = false,
)

@Composable
internal fun TwitchPlayer(
    target: TwitchPlaybackTarget,
    events: PlayerEvents,
    /** The screen shows its chrome with the player's controls, so Pause shows in the middle. */
    controlsUp: Boolean,
    /** The player rests at its full size, with nothing of the screen's over its middle. */
    centerFree: Boolean,
    modifier: Modifier = Modifier,
    gestures: PlayerGestures = PlayerGestures(),
    inPictureInPicture: Boolean = false,
    /**
     * The player is in the corner or on its way there: Twitch's controls stay out of sight, as in picture-in-picture,
     * without a tap that would play a paused stream.
     */
    shrunk: Boolean = false,
    fullscreen: Boolean = false,
    /** Room between a recording's time labels and its seek bar, where the screen draws over the page. */
    seekbarGap: Dp = 0.dp,
) {
    val context = LocalContext.current
    val serviceOwner = remember { Any() }
    var host by remember { mutableStateOf<TwitchWebViewHost?>(null) }
    var waitingForVideo by remember(target.key) { mutableStateOf(true) }
    var tapSeekSeries by remember(target.key) { mutableStateOf<TapSeekSeries?>(null) }
    var zoom by remember { mutableStateOf(PlayerZoomTransform()) }
    val showWebView = rememberPlayerWebViewStarts()
    PlayerHostEffects(host, inPictureInPicture, shrunk, fullscreen, seekbarGap, serviceOwner)
    Box(modifier.background(ComposeColor.Black)) {
        if (showWebView) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    TwitchWebViewHost(context).also { host = it }
                },
                update = { view ->
                    view.bind(events, gestures)
                    view.onTapSeekSeries = { tapSeekSeries = it }
                    view.onZoom = { zoom = it }
                    val waiting = { value: Boolean ->
                        waitingForVideo = value
                        events.onWaitingForVideo(value)
                    }
                    view.onChannelLoading = {
                        waiting(true)
                        // Picture-in-picture looks up the media session only when it opens: one that appears
                        // with the first frame never gets its play and pause button there.
                        PlayerPlaybackService.start(context, serviceOwner, target)
                    }
                    view.onPlaybackStarted = {
                        waiting(false)
                        // A channel that was offline has no session yet when it starts.
                        PlayerPlaybackService.start(context, serviceOwner, target)
                    }
                    view.onOffline = {
                        waiting(false)
                        PlayerPlaybackService.stop(context, serviceOwner)
                    }
                    view.onVideoHidden = { hidden -> waiting(hidden) }
                    view.load(target)
                    events.chrome?.matchControls = view.controls::match
                },
                onRelease = {
                    it.release()
                    host = null
                },
            )
        }
        PlayerOverlays(
            waitingForVideo = waitingForVideo,
            tapSeekSeries = tapSeekSeries,
            zoom = zoom,
            inPictureInPicture = inPictureInPicture,
            controlsUp = controlsUp,
            centerFree = centerFree,
            onResetZoom = { host?.zoom?.reset() },
        )
    }
}

/** What the player draws over the page: the loading spinner, the seeking taps' count, Play or Pause, and the zoom. */
@Composable
private fun BoxScope.PlayerOverlays(
    waitingForVideo: Boolean,
    tapSeekSeries: TapSeekSeries?,
    zoom: PlayerZoomTransform,
    inPictureInPicture: Boolean,
    controlsUp: Boolean,
    centerFree: Boolean,
    onResetZoom: () -> Unit,
) {
    if (waitingForVideo && !inPictureInPicture) {
        CircularProgressIndicator(
            modifier = Modifier
                .align(Alignment.Center)
                .size(36.dp),
            color = ComposeColor.White,
            strokeWidth = 3.dp,
        )
    }
    TapSeekIndicator(series = tapSeekSeries.takeUnless { inPictureInPicture })
    val playback by StreamPlayback.playback.collectAsStateWithLifecycle()
    PlayerPlayPauseButton(
        playerCenterButton(
            playback = playback,
            controlsUp = controlsUp,
            centerFree = centerFree && !inPictureInPicture && !waitingForVideo && tapSeekSeries == null,
        ),
    )
    PlayerZoomResetButton(zoom.scale.takeIf { zoom.zoomed && !inPictureInPicture }, onResetZoom)
}

/** The WebView starts a few frames after the player opens, so the screen around it draws first. */
@Composable
private fun rememberPlayerWebViewStarts(): Boolean {
    var framesDrawn by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (!playerWebViewStarts(framesDrawn)) {
            withFrameNanos { }
            framesDrawn += 1
        }
    }
    return playerWebViewStarts(framesDrawn)
}

/**
 * Keeps the page playing out of sight, plays sound only where nobody sees it, and passes on picture-in-picture, the
 * mini player, full screen, and the room over a recording's seek bar.
 */
@Composable
private fun PlayerHostEffects(
    host: TwitchWebViewHost?,
    inPictureInPicture: Boolean,
    shrunk: Boolean,
    fullscreen: Boolean,
    seekbarGap: Dp,
    serviceOwner: Any,
) {
    DisposableEffect(host, seekbarGap) {
        host?.seekbarGap?.set(seekbarGap.value.roundToInt())
        onDispose { }
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    val lifecycleState by lifecycleOwner.lifecycle.currentStateAsState()
    val context = LocalContext.current
    val keepAwake = inPictureInPicture || lifecycleState < Lifecycle.State.RESUMED
    DisposableEffect(context, serviceOwner) {
        onDispose { PlayerPlaybackService.stop(context, serviceOwner) }
    }
    DisposableEffect(host, keepAwake) {
        host?.keepAlive?.keepAwake(keepAwake)
        onDispose { }
    }
    val appSettings by AppSettingsStore.restored.collectAsStateWithLifecycle()
    val screenUnlocked by rememberScreenUnlocked(lifecycleState)
    val soundOnly = playerAudioOnly(
        enabled = appSettings.beforeRestore().backgroundSoundOnly,
        // Not RESUMED: entering or leaving picture-in-picture pauses first and reports the mode after, which would
        // switch to sound only and back on the way. A visible activity stays STARTED.
        background = lifecycleState < Lifecycle.State.STARTED,
        inPictureInPicture = inPictureInPicture,
        screenUnlocked = screenUnlocked,
    )
    DisposableEffect(host, soundOnly) {
        host?.setSoundOnly(soundOnly)
        onDispose { }
    }
    val smoothSound = appSettings.beforeRestore().smoothSound
    DisposableEffect(host, smoothSound) {
        host?.smoothSound?.set(smoothSound)
        onDispose { }
    }
    DisposableEffect(host, inPictureInPicture, shrunk) {
        host?.setPlacement(inPictureInPicture, shrunk)
        onDispose { }
    }
    DisposableEffect(host, fullscreen) {
        host?.quality?.setFullscreen(fullscreen)
        onDispose { }
    }
}

/** Hands the page's reports to [events] and sets which gestures move the player. */
private fun TwitchWebViewHost.bind(events: PlayerEvents, gestures: PlayerGestures) {
    onTap = events.onTap
    onControlsVisible = events.onControlsVisible
    onControlBarHeightPx = events.onControlBarHeightPx
    onSeekbar = events.onSeekbar
    onSettingsMenuOpen = events.onSettingsMenuOpen
    onPositionSeconds = events.onPositionSeconds
    onSeekSeconds = events.onSeekSeconds
    onDismissDrag = events.onDismissDrag
    onDismissDragEnd = events.onDismissDragEnd
    onHorizontalDrag = events.onHorizontalDrag
    onHorizontalDragEnd = events.onHorizontalDragEnd
    onZoomIn = events.onZoomIn
    enableMinimizeSwipe = gestures.enableMinimizeSwipe
    enableExpandSwipe = gestures.enableExpandSwipe
    enableHorizontalDismiss = gestures.enableHorizontalDismiss
    tapExpands = gestures.tapExpands
}

@SuppressLint("SetJavaScriptEnabled")
private class TwitchWebViewHost(context: Context) : FrameLayout(context) {
    private val appContext = context.applicationContext
    private val scripts = TwitchPlayerScripts(appContext.assets)
    val webView = PlayerWebView(context)
    var onTap: ((playerControlsVisible: Boolean, reachedPage: Boolean) -> Unit)? = null
    var onControlsVisible: ((Boolean) -> Unit)? = null
    var onControlBarHeightPx: ((Int) -> Unit)? = null
    var onSeekbar: ((PlayerSeekbar) -> Unit)? = null
    var onSettingsMenuOpen: ((Boolean) -> Unit)? = null
    var onPositionSeconds: ((Double) -> Unit)? = null
    var onSeekSeconds: ((Double) -> Unit)? = null
    var onTapSeekSeries: ((TapSeekSeries?) -> Unit)? = null
    var onZoom: ((PlayerZoomTransform) -> Unit)? = null
    var onZoomIn: (() -> Unit)? = null
    private var zoomedIn = false
    val quality = PlayerQuality(webView, scripts, isReleased = { released })
    val zoom = PlayerZoomGesture(webView, onSettled = quality::settleZoom) { transform ->
        if (transform.zoomed != zoomedIn && !released) {
            if (transform.zoomed) onZoomIn?.invoke()
            webView.evaluateJavascript(scripts.zoomOverlays(transform.zoomed), null)
        }
        zoomedIn = transform.zoomed
        onZoom?.invoke(transform)
    }
    @Volatile private var touchOnScrollableContent = false
    @Volatile private var touchOnControls = false
    @Volatile private var touchOnVideo = false
    @Volatile private var touchOnDisclosure = false
    private var controlBarPx = 0
    var onDismissDrag: ((Float) -> Unit)? = null
    var onDismissDragEnd: ((Float, Float) -> Unit)? = null
    var onHorizontalDrag: ((Float) -> Unit)? = null
    var onHorizontalDragEnd: ((Float, Float) -> Unit)? = null
    var onChannelLoading: (() -> Unit)? = null
    var onPlaybackStarted: (() -> Unit)? = null
    var onOffline: (() -> Unit)? = null
    var onVideoHidden: ((Boolean) -> Unit)? = null
    var enableMinimizeSwipe = true
    var enableExpandSwipe = false
    var enableHorizontalDismiss = false
    var tapExpands = false
        set(value) {
            field = value
            // The mini player shows the whole picture.
            if (value) zoom.reset()
        }
    private val mainHandler = Handler(Looper.getMainLooper())
    private var loadedTarget: TwitchPlaybackTarget? = null
    private var surfaceRevealed = false
    private var released = false
    val controls = PlayerControlsMirror(
        handler = mainHandler,
        webView = webView,
        scripts = scripts,
        isReleased = { released },
        isRecording = { loadedTarget is TwitchPlaybackTarget.Video },
        onVisible = { visible -> onControlsVisible?.invoke(visible) },
    )
    private val recovery = PlayerPageRecovery(
        handler = mainHandler,
        isReleased = { released },
        isSurfaceRevealed = { surfaceRevealed },
        reloadPlayer = { webView.evaluateJavascript(scripts.reloadPlayer, null) },
        reloadPage = { restorePlayer(resetStartupReloads = false) },
        concealSurface = ::concealSurface,
    )
    private val liveStall = LiveStallRecovery(
        isLive = { loadedTarget is TwitchPlaybackTarget.Channel },
        isPlaybackConfirmed = { recovery.playbackConfirmed },
        restartPlayer = { webView.evaluateJavascript(scripts.restartLive, null) },
        reloadPage = { restorePlayer(resetStartupReloads = false) },
    )
    private val networkWatch = PlayerNetworkWatch(
        appContext = appContext,
        handler = mainHandler,
        isLive = { loadedTarget is TwitchPlaybackTarget.Channel },
        reloadPage = { restorePlayer() },
    )
    val keepAlive = PlayerKeepAlive(appContext, mainHandler, webView, scripts, isReleased = { released })
    private var soundOnly = false
    val smoothSound = PlayerSmoothSound(webView, scripts, isReleased = { released })
    val seekbarGap = PlayerSeekbarGap(webView, scripts, isReleased = { released })
    private var pictureInPicture = false
    private var shrunk = false
    private var reportedPlaying = false
    private val playbackCommands = StreamPlayback.Commands(
        play = { setStreamPlaying(true) {} },
        pause = { onRun -> setStreamPlaying(false, onRun) },
        seek = { seconds ->
            if (!released) webView.evaluateJavascript(scripts.seek(seconds), null)
        },
        close = { release() },
    )
    private var positionSeconds = 0.0
    private val tapSeek = RecordingTapSeek(
        handler = mainHandler,
        doubleTapTimeoutMs = ViewConfiguration.getDoubleTapTimeout().toLong(),
        now = SystemClock::uptimeMillis,
        player = TapSeekPlayer(
            positionSeconds = { positionSeconds },
            durationSeconds = { (loadedTarget as? TwitchPlaybackTarget.Video)?.durationSeconds ?: 0L },
            controlsVisible = { controls.visible },
            seek = playbackCommands.seek,
            chromeTap = { controlsVisible -> onTap?.invoke(controlsVisible, false) },
            onSeries = { series -> onTapSeekSeries?.invoke(series) },
        ),
    )
    private val gestureHandler: PlayerGestureHandler

    init {
        setBackgroundColor(Color.TRANSPARENT)
        webView.setBackgroundColor(Color.TRANSPARENT)
        webView.alpha = 0f
        webView.isVerticalScrollBarEnabled = false
        webView.isHorizontalScrollBarEnabled = false
        webView.overScrollMode = OVER_SCROLL_NEVER
        addView(
            webView,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT),
        )
        webView.setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_IMPORTANT, false)
        webView.settings.offscreenPreRaster = true
        webView.addJavascriptInterface(
            PlayerPlaybackBridge(
                handler = mainHandler,
                events = PlayerPlaybackEvents(
                    onPlaybackState = ::handlePlaybackState,
                    onInterrupted = StreamInterruption::report,
                    onPositionSeconds = { seconds ->
                        positionSeconds = seconds
                        StreamPlayback.reportPosition(seconds)
                        onPositionSeconds?.invoke(seconds)
                    },
                    onSeekSeconds = { seconds ->
                        positionSeconds = seconds
                        StreamPlayback.reportSeek(seconds)
                        onSeekSeconds?.invoke(seconds)
                    },
                    onPlayerReady = recovery::playerReady,
                    onOffline = ::handleOffline,
                    // Twitch's audience gate waits for a tap, and the video may not start under it: the page shows.
                    onAudienceGate = {
                        if (!surfaceRevealed) AppLog.i(PLAYER_LOG_TAG, "Audience gate after ${recovery.sinceLoadMs} ms")
                        recovery.videoWithheld()
                        revealSurface()
                    },
                    onPlayerError = recovery::playerError,
                    onPlayerReload = recovery::errorScreenReload,
                    onLiveTick = liveStall::tick,
                ),
                isReleased = { released },
                isCurrentPage = { recovery.pageLoad.current },
            ),
            "CustomTvPlayback",
        )
        webView.addJavascriptInterface(
            PlayerControlsBridge(
                handler = mainHandler,
                events = PlayerControlsEvents(
                    onControlsVisible = controls::setVisible,
                    onPageTouch = { scrollable, onControls, onVideo, onDisclosure ->
                        touchOnScrollableContent = scrollable
                        touchOnControls = onControls
                        touchOnVideo = onVideo
                        touchOnDisclosure = onDisclosure
                    },
                    onControlBarFraction = { fraction ->
                        controlBarPx = controlBarHeightPx(fraction, webView.height)
                        onControlBarHeightPx?.invoke(controlBarPx)
                    },
                    onSeekbar = { seekbar -> onSeekbar?.invoke(seekbar) },
                    onSettingsMenu = { open -> onSettingsMenuOpen?.invoke(open) },
                    onHoverAreaMissing = {
                        AppLog.w(PLAYER_LOG_TAG, "Recording controls cannot hide: the player's hover area is missing")
                    },
                ),
            ),
            "CustomTvControls",
        )
        StreamPlayback.bind(playbackCommands)
        val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
        gestureHandler = PlayerGestureHandler(
            view = webView,
            touchSlop = touchSlop,
            state = PlayerTouchState(
                isReleased = { released },
                isAppTap = { controls.dispatchingTap },
                canMinimize = { enableMinimizeSwipe },
                canExpand = { enableExpandSwipe },
                canDismissHorizontally = { enableHorizontalDismiss },
                canZoom = { surfaceRevealed && !tapExpands && !pictureInPicture },
                controlsVisible = { controls.visible },
                touchOnControls = { touchOnControls },
                touchOnScrollableContent = { touchOnScrollableContent },
                touchOnDisclosure = { touchOnDisclosure },
                statusBarPx = {
                    ViewCompat.getRootWindowInsets(webView)
                        ?.getInsetsIgnoringVisibility(WindowInsetsCompat.Type.statusBars())
                        ?.top ?: 0
                },
                tapExpands = { tapExpands },
                tapStaysOffPage = {
                    playerTapStaysOffPage(recording = loadedTarget is TwitchPlaybackTarget.Video, touchOnVideo)
                },
            ),
            callbacks = PlayerTouchCallbacks(
                onTap = { reachedPage, x ->
                    // A tap past the page on a full player lands on a recording's video, where quick side taps seek.
                    if (!reachedPage && !tapExpands && !pictureInPicture) {
                        tapSeek.onTap(x, webView.width.toFloat())
                    } else {
                        onTap?.invoke(controls.visible, reachedPage)
                    }
                },
                onVerticalDrag = { onDismissDrag?.invoke(it) },
                onVerticalDragEnd = { offset, velocity -> onDismissDragEnd?.invoke(offset, velocity) },
                onHorizontalDrag = { onHorizontalDrag?.invoke(it) },
                onHorizontalDragEnd = { offset, velocity ->
                    onHorizontalDragEnd?.invoke(offset, velocity)
                },
            ),
            zoom = zoom,
        ).also { it.install() }
        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            javaScriptCanOpenWindowsAutomatically = false
            mediaPlaybackRequiresUserGesture = false
            loadsImagesAutomatically = true
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            cacheMode = WebSettings.LOAD_DEFAULT
        }
        keepAlive.register()
        networkWatch.register()
        webView.webViewClient = PlayerNavigationClient(
            handler = mainHandler,
            isReleased = { released },
            events = PlayerPageEvents(
                hideTopOverlay = ::injectHideTopOverlay,
                onPlayerPageStarted = recovery::pageResponded,
                onPlayerPageLoaded = recovery::pageLoaded,
                onPlayerPageFailed = recovery::pageFailed,
                restorePlayer = ::restorePlayer,
            ),
        )
        webView.webChromeClient = object : WebChromeClient() {
            // Without frames, such as right after sound only, the WebView shows its gray poster with a stretched play
            // icon; a clear one leaves the player's black background.
            override fun getDefaultVideoPoster(): Bitmap = createBitmap(1, 1)

            override fun onCreateWindow(
                view: WebView?,
                isDialog: Boolean,
                isUserGesture: Boolean,
                resultMsg: Message?,
            ): Boolean = false
        }
    }

    fun load(target: TwitchPlaybackTarget, resetStartupReloads: Boolean = true) {
        if (released) return
        if (loadedTarget?.key == target.key) return
        loadedTarget = target
        positionSeconds = 0.0
        tapSeek.cancel()
        zoom.reset()
        quality.pageLoading()
        AppLog.i(PLAYER_LOG_TAG, "Loading ${target.key}")
        recovery.start(resetStartupReloads)
        if (resetStartupReloads) liveStall.reset()
        controls.setVisible(false)
        surfaceRevealed = false
        onSettingsMenuOpen?.invoke(false)
        webView.alpha = 0f
        onChannelLoading?.invoke()
        StreamPlayback.prepareForChannel()
        webView.loadUrl(playerEmbedUrl(target))
    }

    private fun restorePlayer(resetStartupReloads: Boolean = true) {
        val target = loadedTarget ?: return
        loadedTarget = null
        load(target, resetStartupReloads)
    }

    fun release() {
        if (released) return
        released = true
        StreamPlayback.unbind(playbackCommands)
        tapSeek.cancel()
        onTap = null
        onTapSeekSeries = null
        onZoom = null
        onZoomIn = null
        onControlsVisible?.invoke(false)
        onControlsVisible = null
        onSettingsMenuOpen?.invoke(false)
        onSettingsMenuOpen = null
        onControlBarHeightPx = null
        onSeekbar = null
        onDismissDrag = null
        onDismissDragEnd = null
        onHorizontalDrag = null
        onHorizontalDragEnd = null
        onChannelLoading = null
        onPlaybackStarted = null
        onOffline = null
        onVideoHidden = null
        gestureHandler.release()
        keepAlive.release()
        networkWatch.release()
        mainHandler.removeCallbacksAndMessages(null)
        val destroyPage = Runnable {
            webView.stopLoading()
            webView.loadUrl("about:blank")
            removeView(webView)
            webView.destroy()
        }
        val closeDelayMs = playerCloseDelayMs(smoothSound.enabled)
        if (closeDelayMs == 0L) {
            destroyPage.run()
            return
        }
        // The sound fades out before the page goes; the view has already left the screen.
        webView.evaluateJavascript(scripts.pause, null)
        mainHandler.postDelayed(destroyPage, closeDelayMs)
    }

    /** Plays or pauses the page; [onRun] once the page has run it, so its reports from then on are after it. */
    private fun setStreamPlaying(playing: Boolean, onRun: () -> Unit) {
        if (released) return
        webView.onResume()
        webView.resumeTimers()
        val js = if (playing) scripts.play else scripts.pause
        webView.evaluateJavascript(js) { onRun() }
        mainHandler.postDelayed({
            if (!released) webView.evaluateJavascript(js, null)
        }, PLAY_STATE_REPEAT_MS)
        if (playing) {
            mainHandler.postDelayed({
                if (!released && !StreamPlayback.userPaused) {
                    webView.evaluateJavascript(scripts.play, null)
                }
            }, PLAY_LATE_REPEAT_MS)
        }
    }

    private fun injectHideTopOverlay() {
        if (released) return
        smoothSound.pageLoaded()
        webView.evaluateJavascript(scripts.hideTopOverlay, null)
        // A page that loaded again in picture-in-picture or the corner starts with the control bar.
        if (controlsCovered) webView.evaluateJavascript(scripts.pictureInPicture(true), null)
        seekbarGap.pageLoaded()
    }

    /**
     * In picture-in-picture, and in the corner or on the way there ([shrunk]), the player's controls stay out of
     * sight without a tap.
     */
    fun setPlacement(pictureInPicture: Boolean, shrunk: Boolean) {
        if (released || (this.pictureInPicture == pictureInPicture && this.shrunk == shrunk)) return
        if (pictureInPicture && !this.pictureInPicture) zoom.reset()
        this.pictureInPicture = pictureInPicture
        this.shrunk = shrunk
        webView.evaluateJavascript(scripts.pictureInPicture(controlsCovered), null)
    }

    private val controlsCovered: Boolean
        get() = pictureInPicture || shrunk

    private fun revealSurface() {
        if (released || surfaceRevealed) return
        surfaceRevealed = true
        webView.alpha = 1f
        recovery.cancelErrorReload()
        onVideoHidden?.invoke(false)
    }

    /** Hides the player page while it recovers from an error, with the loading spinner over it. */
    private fun concealSurface() {
        surfaceRevealed = false
        webView.alpha = 0f
        onVideoHidden?.invoke(true)
    }

    /** Plays sound only while nobody sees the video, and brings the video back as it was. */
    fun setSoundOnly(value: Boolean) {
        if (released || soundOnly == value) return
        soundOnly = value
        AppLog.i(PLAYER_LOG_TAG, if (value) "Sound only in the background" else "Video back")
        webView.evaluateJavascript(
            if (value) scripts.soundOnly else scripts.videoQuality,
            null,
        )
    }

    private fun handlePlaybackState(playing: Boolean) {
        // The page reports the state every tick; only a start matters for sound only.
        val started = playing && !reportedPlaying
        reportedPlaying = playing
        if (playing) {
            recovery.resetAutomaticReloads()
            revealSurface()
        }
        if (started) webView.evaluateJavascript(scripts.fullVolume, null)
        if (started) quality.playing()
        // A page that loaded again in the background starts with video.
        if (started && soundOnly) webView.evaluateJavascript(scripts.soundOnly, null)
        if (playing && recovery.confirmPlayback()) onPlaybackStarted?.invoke()
        StreamPlayback.reportActualState(playing)
    }

    private fun handleOffline() {
        if (recovery.playbackConfirmed) return
        AppLog.i(PLAYER_LOG_TAG, "Offline after ${recovery.sinceLoadMs} ms")
        recovery.videoWithheld()
        revealSurface()
        onOffline?.invoke()
    }

    private companion object {
        // Play or pause is sent again a moment later, for a page that missed the first call.
        const val PLAY_STATE_REPEAT_MS = 200L
        const val PLAY_LATE_REPEAT_MS = 700L
    }
}
