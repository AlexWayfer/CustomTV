package name.alexwayfer.customtv.player

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.os.SystemClock
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
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

internal const val PLAYER_LOG_TAG = "Player"

@Composable
internal fun TwitchPlayer(
    target: TwitchPlaybackTarget,
    modifier: Modifier = Modifier,
    onPositionSeconds: (Double) -> Unit = {},
    onSeekSeconds: (Double) -> Unit = {},
    onTap: (playerControlsVisible: Boolean, reachedPage: Boolean) -> Unit = { _, _ -> },
    onControlsVisible: (Boolean) -> Unit = {},
    onControlBarHeightPx: (Int) -> Unit = {},
    onSettingsMenuOpen: (Boolean) -> Unit = {},
    onWaitingForVideo: (Boolean) -> Unit = {},
    onDismissDrag: (offsetY: Float) -> Unit = {},
    onDismissDragEnd: (offsetY: Float, velocityY: Float) -> Unit = { _, _ -> },
    onHorizontalDrag: (offsetX: Float) -> Unit = {},
    onHorizontalDragEnd: (offsetX: Float, velocityX: Float) -> Unit = { _, _ -> },
    enableMinimizeSwipe: Boolean = true,
    enableExpandSwipe: Boolean = false,
    enableHorizontalDismiss: Boolean = false,
    tapExpands: Boolean = false,
    inPictureInPicture: Boolean = false,
    chrome: PlayerChrome? = null,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val lifecycleState by lifecycleOwner.lifecycle.currentStateAsState()
    val context = LocalContext.current
    val serviceOwner = remember { Any() }
    var host by remember { mutableStateOf<TwitchWebViewHost?>(null) }
    var waitingForVideo by remember(target.key) { mutableStateOf(true) }
    var tapSeekSeries by remember(target.key) { mutableStateOf<TapSeekSeries?>(null) }
    var framesDrawn by remember { mutableIntStateOf(0) }
    val showWebView = playerWebViewStarts(framesDrawn)
    val keepAwake = inPictureInPicture || lifecycleState < Lifecycle.State.RESUMED

    LaunchedEffect(Unit) {
        while (!playerWebViewStarts(framesDrawn)) {
            withFrameNanos { }
            framesDrawn += 1
        }
    }

    DisposableEffect(context, serviceOwner) {
        onDispose { PlayerPlaybackService.stop(context, serviceOwner) }
    }
    DisposableEffect(host, keepAwake) {
        host?.keepWebViewAwake(keepAwake)
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
    DisposableEffect(host, inPictureInPicture) {
        host?.setPictureInPicture(inPictureInPicture)
        onDispose { }
    }

    Box(modifier.background(ComposeColor.Black)) {
        if (showWebView) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    TwitchWebViewHost(context).also { host = it }
                },
                update = { view ->
                    view.onTap = onTap
                    view.onControlsVisible = onControlsVisible
                    view.onControlBarHeightPx = onControlBarHeightPx
                    view.onSettingsMenuOpen = onSettingsMenuOpen
                    view.onPositionSeconds = onPositionSeconds
                    view.onSeekSeconds = onSeekSeconds
                    view.onTapSeekSeries = { tapSeekSeries = it }
                    view.onDismissDrag = onDismissDrag
                    view.onDismissDragEnd = onDismissDragEnd
                    view.onHorizontalDrag = onHorizontalDrag
                    view.onHorizontalDragEnd = onHorizontalDragEnd
                    view.enableMinimizeSwipe = enableMinimizeSwipe
                    view.enableExpandSwipe = enableExpandSwipe
                    view.enableHorizontalDismiss = enableHorizontalDismiss
                    view.tapExpands = tapExpands
                    view.onChannelLoading = {
                        waitingForVideo = true
                        onWaitingForVideo(true)
                        // Picture-in-picture looks up the media session only when it opens: one that appears
                        // with the first frame never gets its play and pause button there.
                        PlayerPlaybackService.start(context, serviceOwner, target)
                    }
                    view.onPlaybackStarted = {
                        waitingForVideo = false
                        onWaitingForVideo(false)
                        // A channel that was offline has no session yet when it starts.
                        PlayerPlaybackService.start(context, serviceOwner, target)
                    }
                    view.onOffline = {
                        waitingForVideo = false
                        onWaitingForVideo(false)
                        PlayerPlaybackService.stop(context, serviceOwner)
                    }
                    view.load(target)
                    chrome?.matchControls = view::matchControls
                },
                onRelease = {
                    it.release()
                    host = null
                },
            )
        }
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
    }
}

@SuppressLint("SetJavaScriptEnabled")
private class TwitchWebViewHost(context: Context) : FrameLayout(context) {
    private val appContext = context.applicationContext
    val webView = PlayerWebView(context)
    var onTap: ((playerControlsVisible: Boolean, reachedPage: Boolean) -> Unit)? = null
    var onControlsVisible: ((Boolean) -> Unit)? = null
    var onControlBarHeightPx: ((Int) -> Unit)? = null
    var onSettingsMenuOpen: ((Boolean) -> Unit)? = null
    var onPositionSeconds: ((Double) -> Unit)? = null
    var onSeekSeconds: ((Double) -> Unit)? = null
    var onTapSeekSeries: ((TapSeekSeries?) -> Unit)? = null
    @Volatile private var playerControlsVisible = false
    @Volatile private var touchOnScrollableContent = false
    @Volatile private var touchOnControls = false
    @Volatile private var touchOnVideo = false
    private var controlBarPx = 0
    private var dispatchingTap = false
    var onDismissDrag: ((Float) -> Unit)? = null
    var onDismissDragEnd: ((Float, Float) -> Unit)? = null
    var onHorizontalDrag: ((Float) -> Unit)? = null
    var onHorizontalDragEnd: ((Float, Float) -> Unit)? = null
    var onChannelLoading: (() -> Unit)? = null
    var onPlaybackStarted: (() -> Unit)? = null
    var onOffline: (() -> Unit)? = null
    var enableMinimizeSwipe = true
    var enableExpandSwipe = false
    var enableHorizontalDismiss = false
    var tapExpands = false
    private val mainHandler = Handler(Looper.getMainLooper())
    private var loadedTarget: TwitchPlaybackTarget? = null
    private var playbackConfirmedForChannel = false
    private var playerReadyForChannel = false
    private var surfaceRevealed = false
    private var released = false
    private var automaticReloadAttempts = 0
    private var playerReloadPending = false
    private var startupReloadAttempts = 0
    private val reloadPlayer = Runnable {
        playerReloadPending = false
        if (released || StreamPlayback.isExplicitlyPaused()) return@Runnable
        webView.evaluateJavascript(TwitchPlayerScripts.RELOAD_PLAYER_JS, null)
    }
    private var loadStartedAt = 0L
    private val pageLoad = PlayerPageLoad()
    private val pageResponseWatchdog = Runnable {
        if (pageLoad.awaitingResponse) handlePlayerPageFailed("no response")
    }
    private val startupWatchdog = Runnable {
        if (released || StreamPlayback.userPaused || playbackConfirmedForChannel || playerReadyForChannel) {
            return@Runnable
        }
        startupReloadAttempts += 1
        AppLog.w(
            PLAYER_LOG_TAG,
            "Not ready after ${sinceLoadMs()} ms, reloading the page (attempt $startupReloadAttempts)",
        )
        restorePlayer(resetStartupReloads = false)
    }
    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_SCREEN_OFF) {
                keepWebViewAwake(true)
            }
        }
    }
    private var keepAliveInBackground = false
    private var soundOnly = false
    private var pictureInPicture = false
    private var reportedPlaying = false
    private val keepAlive = object : Runnable {
        override fun run() {
            if (released || !keepAliveInBackground) return
            webView.onResume()
            webView.resumeTimers()
            if (!StreamPlayback.userPaused) {
                webView.evaluateJavascript(TwitchPlayerScripts.PLAY_JS, null)
            }
            mainHandler.postDelayed(this, TwitchPlayerScripts.KEEP_ALIVE_MS)
        }
    }
    private val playbackCommands = StreamPlayback.Commands(
        play = { setStreamPlaying(true) },
        pause = { setStreamPlaying(false) },
        seek = { seconds ->
            if (!released) webView.evaluateJavascript(TwitchPlayerScripts.seekJs(seconds), null)
        },
        close = { release() },
    )
    private var positionSeconds = 0.0
    private val tapSeek = RecordingTapSeek(
        handler = mainHandler,
        doubleTapTimeoutMs = ViewConfiguration.getDoubleTapTimeout().toLong(),
        now = SystemClock::uptimeMillis,
        positionSeconds = { positionSeconds },
        durationSeconds = { (loadedTarget as? TwitchPlaybackTarget.Video)?.durationSeconds ?: 0L },
        controlsVisible = { playerControlsVisible },
        seek = playbackCommands.seek,
        chromeTap = { controlsVisible -> onTap?.invoke(controlsVisible, false) },
        onSeries = { series -> onTapSeekSeries?.invoke(series) },
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
                onPlaybackState = ::handlePlaybackState,
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
                onPlayerReady = ::handlePlayerReady,
                onOffline = ::handleOffline,
                onPlayerError = ::handlePlayerError,
                onPlayerReload = ::handlePlayerReload,
                onControlsVisible = ::setPlayerControlsVisible,
                onPageTouch = { scrollable, onControls, onVideo ->
                    touchOnScrollableContent = scrollable
                    touchOnControls = onControls
                    touchOnVideo = onVideo
                },
                onControlBarFraction = { fraction ->
                    controlBarPx = controlBarHeightPx(fraction, webView.height)
                    onControlBarHeightPx?.invoke(controlBarPx)
                },
                onSettingsMenu = { open -> onSettingsMenuOpen?.invoke(open) },
                onHoverAreaMissing = {
                    AppLog.w(PLAYER_LOG_TAG, "Recording controls cannot hide: the player's hover area is missing")
                },
            ),
            "CustomTvPlayback",
        )
        StreamPlayback.bind(playbackCommands)
        val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
        gestureHandler = PlayerGestureHandler(
            view = webView,
            touchSlop = touchSlop,
            isReleased = { released },
            isAppTap = { dispatchingTap },
            canMinimize = { enableMinimizeSwipe },
            canExpand = { enableExpandSwipe },
            canDismissHorizontally = { enableHorizontalDismiss },
            controlsVisible = { playerControlsVisible },
            touchOnControls = { touchOnControls },
            touchOnScrollableContent = { touchOnScrollableContent },
            statusBarPx = {
                ViewCompat.getRootWindowInsets(webView)
                    ?.getInsetsIgnoringVisibility(WindowInsetsCompat.Type.statusBars())
                    ?.top ?: 0
            },
            tapExpands = { tapExpands },
            tapStaysOffPage = {
                playerTapStaysOffPage(recording = loadedTarget is TwitchPlaybackTarget.Video, touchOnVideo)
            },
            onTap = { reachedPage, x ->
                // A tap past the page on a full player lands on a recording's video, where quick taps on a side seek.
                if (!reachedPage && !tapExpands && !pictureInPicture) {
                    tapSeek.onTap(x, webView.width.toFloat())
                } else {
                    onTap?.invoke(playerControlsVisible, reachedPage)
                }
            },
            onVerticalDrag = { onDismissDrag?.invoke(it) },
            onVerticalDragEnd = { offset, velocity -> onDismissDragEnd?.invoke(offset, velocity) },
            onHorizontalDrag = { onHorizontalDrag?.invoke(it) },
            onHorizontalDragEnd = { offset, velocity ->
                onHorizontalDragEnd?.invoke(offset, velocity)
            },
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
        ContextCompat.registerReceiver(
            appContext,
            screenOffReceiver,
            IntentFilter(Intent.ACTION_SCREEN_OFF),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        webView.webViewClient = PlayerNavigationClient(
            handler = mainHandler,
            isReleased = { released },
            hideTopOverlay = ::injectHideTopOverlay,
            onPlayerPageStarted = pageLoad::respond,
            onPlayerPageLoaded = ::handlePlayerPageLoaded,
            onPlayerPageFailed = ::handlePlayerPageFailed,
            restorePlayer = ::restorePlayer,
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

    private fun setPlayerControlsVisible(visible: Boolean) {
        playerControlsVisible = visible
        mainHandler.post { onControlsVisible?.invoke(visible) }
    }

    /** Acts after the current pass, once the page has its final size, if the controls still differ. */
    fun matchControls(visible: Boolean) {
        mainHandler.post {
            if (released) return@post
            val recording = loadedTarget is TwitchPlaybackTarget.Video
            when (playerControlsSync(recording, visible, playerControlsVisible)) {
                PlayerControlsSync.None -> Unit
                PlayerControlsSync.Tap -> dispatchPlayerTap()
                PlayerControlsSync.ShowWithoutTap ->
                    webView.evaluateJavascript(TwitchPlayerScripts.SHOW_CONTROLS_JS, null)
                PlayerControlsSync.HideWithoutTap ->
                    webView.evaluateJavascript(TwitchPlayerScripts.HIDE_CONTROLS_JS, null)
            }
        }
    }

    /** Taps the player's center past the gestures; the chrome already shows the result. */
    private fun dispatchPlayerTap() {
        val x = webView.width / 2f
        val y = webView.height / 2f
        if (x <= 0f || y <= 0f) return
        val now = SystemClock.uptimeMillis()
        val down = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, x, y, 0)
        val up = MotionEvent.obtain(now, now, MotionEvent.ACTION_UP, x, y, 0)
        dispatchingTap = true
        try {
            webView.dispatchTouchEvent(down)
            webView.dispatchTouchEvent(up)
        } finally {
            dispatchingTap = false
        }
        down.recycle()
        up.recycle()
    }

    fun keepWebViewAwake(background: Boolean) {
        if (released) return
        if (background && keepAliveInBackground) return
        webView.onResume()
        webView.resumeTimers()
        keepAliveInBackground = background
        StreamPlayback.setSuppressInferredPause(background)
        mainHandler.removeCallbacks(keepAlive)
        if (background) {
            mainHandler.postDelayed(keepAlive, TwitchPlayerScripts.KEEP_ALIVE_MS)
        }
    }

    fun load(target: TwitchPlaybackTarget) {
        load(target, resetStartupReloads = true)
    }

    private fun load(target: TwitchPlaybackTarget, resetStartupReloads: Boolean) {
        if (released) return
        if (loadedTarget?.key == target.key) return
        loadedTarget = target
        playbackConfirmedForChannel = false
        playerReadyForChannel = false
        positionSeconds = 0.0
        tapSeek.cancel()
        if (resetStartupReloads) startupReloadAttempts = 0
        loadStartedAt = SystemClock.elapsedRealtime()
        AppLog.i(PLAYER_LOG_TAG, "Loading ${target.key}")
        resetAutomaticReloads()
        mainHandler.removeCallbacks(startupWatchdog)
        pageLoad.start()
        mainHandler.removeCallbacks(pageResponseWatchdog)
        mainHandler.postDelayed(pageResponseWatchdog, PAGE_RESPONSE_TIMEOUT_MS)
        setPlayerControlsVisible(false)
        surfaceRevealed = false
        onSettingsMenuOpen?.invoke(false)
        webView.alpha = 0f
        onChannelLoading?.invoke()
        StreamPlayback.prepareForChannel()
        webView.loadUrl(TwitchPlayerScripts.playerEmbedUrl(target))
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
        onControlsVisible?.invoke(false)
        onControlsVisible = null
        onSettingsMenuOpen?.invoke(false)
        onSettingsMenuOpen = null
        onControlBarHeightPx = null
        onDismissDrag = null
        onDismissDragEnd = null
        onHorizontalDrag = null
        onHorizontalDragEnd = null
        onChannelLoading = null
        onPlaybackStarted = null
        onOffline = null
        gestureHandler.release()
        keepAliveInBackground = false
        StreamPlayback.setSuppressInferredPause(false)
        runCatching { appContext.unregisterReceiver(screenOffReceiver) }
        mainHandler.removeCallbacksAndMessages(null)
        webView.stopLoading()
        webView.loadUrl("about:blank")
        removeView(webView)
        webView.destroy()
    }

    private fun setStreamPlaying(playing: Boolean) {
        if (released) return
        webView.onResume()
        webView.resumeTimers()
        val js = if (playing) TwitchPlayerScripts.PLAY_JS else TwitchPlayerScripts.PAUSE_JS
        webView.evaluateJavascript(js, null)
        mainHandler.postDelayed({
            if (!released) webView.evaluateJavascript(js, null)
        }, 200)
        if (playing) {
            mainHandler.postDelayed({
                if (!released && !StreamPlayback.userPaused) {
                    webView.evaluateJavascript(TwitchPlayerScripts.PLAY_JS, null)
                }
            }, 700)
        }
    }

    private fun injectHideTopOverlay() {
        if (released) return
        webView.evaluateJavascript(TwitchPlayerScripts.HIDE_TOP_OVERLAY_JS, null)
        // A page that loaded again in picture-in-picture starts with the control bar.
        if (pictureInPicture) webView.evaluateJavascript(TwitchPlayerScripts.pictureInPictureJs(true), null)
    }

    fun setPictureInPicture(value: Boolean) {
        if (released || pictureInPicture == value) return
        pictureInPicture = value
        webView.evaluateJavascript(TwitchPlayerScripts.pictureInPictureJs(value), null)
    }

    private fun revealSurface() {
        if (released || surfaceRevealed) return
        surfaceRevealed = true
        webView.alpha = 1f
    }

    private fun concealSurface() {
        surfaceRevealed = false
        webView.alpha = 0f
    }

    /** Plays sound only while nobody sees the video, and brings the video back as it was. */
    fun setSoundOnly(value: Boolean) {
        if (released || soundOnly == value) return
        soundOnly = value
        AppLog.i(PLAYER_LOG_TAG, if (value) "Sound only in the background" else "Video back")
        webView.evaluateJavascript(
            if (value) TwitchPlayerScripts.SOUND_ONLY_JS else TwitchPlayerScripts.VIDEO_QUALITY_JS,
            null,
        )
    }

    private fun handlePlaybackState(playing: Boolean) {
        // The page reports the state every tick; only a start matters for sound only.
        val started = playing && !reportedPlaying
        reportedPlaying = playing
        if (playing) {
            resetAutomaticReloads()
            revealSurface()
        }
        if (started) webView.evaluateJavascript(TwitchPlayerScripts.FULL_VOLUME_JS, null)
        // A page that loaded again in the background starts with video.
        if (started && soundOnly) webView.evaluateJavascript(TwitchPlayerScripts.SOUND_ONLY_JS, null)
        if (playing && !playbackConfirmedForChannel) {
            playbackConfirmedForChannel = true
            AppLog.i(PLAYER_LOG_TAG, "Playing after ${sinceLoadMs()} ms")
            onPlaybackStarted?.invoke()
        }
        StreamPlayback.reportActualState(playing)
    }

    private fun handlePlayerPageLoaded() {
        if (!pageLoad.finish()) return
        AppLog.i(PLAYER_LOG_TAG, "Page loaded after ${sinceLoadMs()} ms")
        if (!playerReadyForChannel && !playbackConfirmedForChannel) scheduleStartupWatchdog()
    }

    private fun handlePlayerPageFailed(reason: String) {
        if (released || !pageLoad.fail()) return
        AppLog.w(PLAYER_LOG_TAG, "Page failed after ${sinceLoadMs()} ms: $reason")
        if (StreamPlayback.userPaused) return
        if (startupReloadAttempts >= STARTUP_RELOAD_DELAYS_MS.size) {
            AppLog.w(PLAYER_LOG_TAG, "No page reloads left")
            return
        }
        startupReloadAttempts += 1
        AppLog.i(PLAYER_LOG_TAG, "Reloading the page (attempt $startupReloadAttempts)")
        restorePlayer(resetStartupReloads = false)
    }

    private fun handlePlayerReady() {
        if (!playerReadyForChannel) AppLog.i(PLAYER_LOG_TAG, "Ready after ${sinceLoadMs()} ms")
        playerReadyForChannel = true
        mainHandler.removeCallbacks(startupWatchdog)
    }

    private fun handlePlayerError() {
        if (released || StreamPlayback.isExplicitlyPaused() || playerReloadPending) return
        val delay = PLAYER_RELOAD_DELAYS_MS.getOrNull(automaticReloadAttempts)
        if (delay == null) {
            AppLog.w(PLAYER_LOG_TAG, "Player error, no automatic reloads left")
            return
        }
        concealSurface()
        automaticReloadAttempts += 1
        playerReloadPending = true
        AppLog.w(PLAYER_LOG_TAG, "Player error, reload $automaticReloadAttempts in $delay ms")
        mainHandler.postDelayed(reloadPlayer, delay)
    }

    private fun handlePlayerReload() {
        if (released) return
        AppLog.i(PLAYER_LOG_TAG, "Error screen reload pressed")
        concealSurface()
    }

    private fun sinceLoadMs(): Long = SystemClock.elapsedRealtime() - loadStartedAt

    private fun resetAutomaticReloads() {
        automaticReloadAttempts = 0
        playerReloadPending = false
        mainHandler.removeCallbacks(reloadPlayer)
    }

    private fun scheduleStartupWatchdog() {
        mainHandler.removeCallbacks(startupWatchdog)
        val delay = STARTUP_RELOAD_DELAYS_MS.getOrNull(startupReloadAttempts) ?: return
        mainHandler.postDelayed(startupWatchdog, delay)
    }

    private fun handleOffline() {
        if (playbackConfirmedForChannel) return
        AppLog.i(PLAYER_LOG_TAG, "Offline after ${sinceLoadMs()} ms")
        revealSurface()
        onOffline?.invoke()
    }

    private companion object {
        val PLAYER_RELOAD_DELAYS_MS = longArrayOf(1_000, 4_000, 12_000)
        val STARTUP_RELOAD_DELAYS_MS = longArrayOf(10_000, 20_000)
        const val PAGE_RESPONSE_TIMEOUT_MS = 10_000L
    }
}
