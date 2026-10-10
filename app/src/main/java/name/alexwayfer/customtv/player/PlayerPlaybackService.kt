package name.alexwayfer.customtv.player

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import name.alexwayfer.customtv.MainActivity
import name.alexwayfer.customtv.R
import java.util.Collections
import java.util.IdentityHashMap

class PlayerPlaybackService : MediaSessionService() {
    private lateinit var player: StreamMedia3Player
    private var mediaSession: MediaSession? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()
        instance = this
        getSystemService(NotificationManager::class.java)?.cancel(LEGACY_NOTIFICATION_ID)
        player = StreamMedia3Player(
            looper = mainLooper,
            appName = getString(R.string.app_name),
            appContext = applicationContext,
            scope = scope,
        )
        val sessionActivity = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(sessionActivity)
            .build()
            .also(::addSession)
    }

    override fun onGetSession(
        controllerInfo: MediaSession.ControllerInfo,
    ): MediaSession? = mediaSession

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        requestedTarget?.let(player::setTarget)
        return super.onStartCommand(intent, flags, startId)
    }

    @OptIn(UnstableApi::class)
    override fun onDestroy() {
        if (instance === this) instance = null
        scope.cancel()
        mediaSession?.release()
        mediaSession = null
        player.release()
        super.onDestroy()
    }

    companion object {
        private const val LEGACY_NOTIFICATION_ID = 7

        @Volatile
        private var instance: PlayerPlaybackService? = null
        // The process is the same: the service reads what to show from here rather than from intent extras.
        @Volatile
        private var requestedTarget: TwitchPlaybackTarget? = null
        private val lifecycleHandler = Handler(Looper.getMainLooper())
        private var pendingStop: Runnable? = null
        private val owners = Collections.newSetFromMap(
            IdentityHashMap<Any, Boolean>(),
        )

        internal fun start(context: Context, owner: Any, target: TwitchPlaybackTarget) {
            val app = context.applicationContext
            owners.add(owner)
            pendingStop?.let(lifecycleHandler::removeCallbacks)
            pendingStop = null
            requestedTarget = target
            app.startService(Intent(app, PlayerPlaybackService::class.java))
        }

        fun stop(context: Context, owner: Any) {
            val app = context.applicationContext
            owners.remove(owner)
            if (owners.isNotEmpty()) return
            pendingStop?.let(lifecycleHandler::removeCallbacks)
            val task = Runnable {
                pendingStop = null
                if (owners.isNotEmpty()) return@Runnable
                app.stopService(Intent(app, PlayerPlaybackService::class.java))
            }
            pendingStop = task
            lifecycleHandler.postDelayed(task, PLAYER_HANDOFF_GRACE_MS)
        }

        fun stopImmediately(context: Context) {
            pendingStop?.let(lifecycleHandler::removeCallbacks)
            pendingStop = null
            owners.clear()
            context.applicationContext.stopService(
                Intent(context, PlayerPlaybackService::class.java),
            )
        }

        fun sync() {
            instance?.player?.syncFromPlayback()
        }

        private const val PLAYER_HANDOFF_GRACE_MS = 500L
    }
}
