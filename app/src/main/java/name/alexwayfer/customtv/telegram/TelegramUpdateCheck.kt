package name.alexwayfer.customtv.telegram

import android.content.Context
import android.os.SystemClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.diagnostics.AppLog
import name.alexwayfer.customtv.ui.UiText
import name.alexwayfer.customtv.update.cancelAppUpdateNotification
import name.alexwayfer.customtv.update.showAppUpdateNotification
import org.drinkless.tdlib.Client
import org.drinkless.tdlib.TdApi
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

private val BACKGROUND_CHECK_TIMEOUT = 1.minutes

// TDLib waits for Telegram instead of failing, so a check without an answer ends here.
private val CHECK_TIMEOUT = 30.seconds
private val CHECK_TIMEOUT_MESSAGE = UiText.Resource(R.string.telegram_did_not_answer)

/**
 * The session's client: [current] gives its generation and the client; [isCurrent] tells whether a result still
 * belongs to them.
 */
internal data class TelegramClients<C : Any>(
    val current: () -> Pair<Int, C?>,
    val isCurrent: (generation: Int, source: C) -> Boolean,
)

/**
 * What a check reads and posts: the edition's groups through the client, the update notification, a reopening of
 * the client's connections, the clock that times the last open check, and how long a check waits for Telegram.
 */
internal data class TelegramCheckIo<C : Any>(
    val loadGroup: suspend (source: C, chatId: Long, topicId: Int) -> TelegramGroupState,
    val premiumAccess: suspend (source: C, chatId: Long, topicId: Int) -> TelegramGroupState,
    val showUpdate: (version: String) -> Unit,
    val clearUpdate: () -> Unit,
    val reopenConnections: (source: C) -> Unit,
    val elapsedRealtime: () -> Long,
    val timeout: Duration = CHECK_TIMEOUT,
)

/** [TelegramCheckIo] through TDLib and the app's notifications, while [appContext] gives the app. */
internal fun telegramCheckIo(appContext: () -> Context?): TelegramCheckIo<Client> = TelegramCheckIo(
    loadGroup = TelegramGroups::load,
    premiumAccess = TelegramGroups::premiumAccess,
    showUpdate = { version -> appContext()?.let { showAppUpdateNotification(it, version) } },
    clearUpdate = { appContext()?.let(::cancelAppUpdateNotification) },
    // TDLib reopens every connection on a new network type. Other is what it assumes when the app never sets one,
    // so only the connections change, and None, which would keep it off the network, is never sent.
    reopenConnections = { client ->
        client.send(TdApi.SetNetworkType(TdApi.NetworkTypeOther())) { result ->
            if (result is TdApi.Error) AppLog.w(TELEGRAM_TAG, "connections reopen failed ${result.code}")
        }
    },
    elapsedRealtime = SystemClock::elapsedRealtime,
)

/**
 * Whether a check that timed out reopens the client's connections: TDLib can stay connecting for good, while a
 * ready or updating connection works and only Telegram was slow. [connection] is the last state's class name.
 */
internal fun telegramReopensAfterTimeout(connection: String?): Boolean =
    connection != "ConnectionStateReady" && connection != "ConnectionStateUpdating"

/**
 * Checks the edition's release group for an update: from the Check updates button, on a return to the app, after
 * the login, and for the daily worker. [C] is the client, which the check only passes on.
 */
internal class TelegramUpdateCheck<C : Any>(
    private val scope: CoroutineScope,
    private val ui: MutableStateFlow<TelegramUiState>,
    private val config: () -> TelegramConfig,
    private val clients: TelegramClients<C>,
    private val connection: () -> String?,
    private val io: TelegramCheckIo<C>,
) {
    private val lock = Any()
    private var runningCheck: RunningCheck? = null

    /** `elapsedRealtime` of the last check that found the group open; only in memory, so a new process checks. */
    private var openCheckAt: Long? = null

    /** The Check updates button: the result shows in the section, so it posts no notification. */
    fun checkAgain() {
        startCheck(notify = false)
    }

    /** A return to the app: checks again only when the last open check is older than the interval. */
    fun checkOnReturn() {
        if (telegramRecheckDue(sinceOpenCheck())) startCheck(notify = false)
    }

    /**
     * A check for the daily worker, which posts the update notification. Returns once it finishes, at once
     * when Telegram is not logged in or a recent check found the group open, or after a timeout.
     */
    suspend fun checkInBackground() {
        withTimeoutOrNull(BACKGROUND_CHECK_TIMEOUT) {
            val step = ui.first { it.step != TelegramLoginStep.Starting }.step
            if (step != TelegramLoginStep.Ready) return@withTimeoutOrNull
            // A process the worker started has usually just checked on Ready, notification included.
            if (!telegramRecheckDue(sinceOpenCheck())) return@withTimeoutOrNull
            startCheck(notify = true)?.join()
        }
    }

    private fun sinceOpenCheck(): Duration? =
        synchronized(lock) { openCheckAt }?.let { (io.elapsedRealtime() - it).milliseconds }

    private fun startCheck(notify: Boolean): Job? {
        if (ui.value.step != TelegramLoginStep.Ready) return null
        val (generation, source) = clients.current()
        if (source == null) return null
        return launchCheck(generation, source, notify)
    }

    /** Starts a check, or joins the one already running for this client, so two never run at once. */
    fun launchCheck(generation: Int, source: C, notify: Boolean): Job {
        val (check, fresh) = synchronized(lock) {
            val running = runningCheck
            if (running != null && running.generation == generation && running.job.isActive) {
                if (notify) running.notify = true
                running to false
            } else {
                val created = RunningCheck(generation, notify)
                created.job = scope.launch(start = CoroutineStart.LAZY) { runCheck(generation, source, created) }
                runningCheck = created
                openCheckAt = null
                created to true
            }
        }
        if (fresh) {
            AppLog.i(TELEGRAM_TAG, "check started, connection ${connection()}")
            ui.update { it.withEditionChecking(config()) }
            check.job.start()
        } else {
            AppLog.i(TELEGRAM_TAG, "check joined")
        }
        return check.job
    }

    private suspend fun runCheck(generation: Int, source: C, check: RunningCheck) {
        var answered = false
        val fresh = withTimeoutOrNull(io.timeout) {
            refresh(generation, source).also { answered = true }
        }
        if (answered) {
            fresh?.let { finishCheck(it, check) }
        } else {
            val state = connection()
            AppLog.w(TELEGRAM_TAG, "check timed out, connection $state")
            if (clients.isCurrent(generation, source)) {
                if (telegramReopensAfterTimeout(state)) {
                    AppLog.i(TELEGRAM_TAG, "connections reopened")
                    io.reopenConnections(source)
                }
                ui.update {
                    it.withEditionGroup(config().premiumEdition, TelegramGroupState.Failed(CHECK_TIMEOUT_MESSAGE))
                }
            }
        }
    }

    /** A check that answered: an open group is the time of the last check, and its release the update notice. */
    private fun finishCheck(group: TelegramGroupState, check: RunningCheck) {
        AppLog.i(TELEGRAM_TAG, "check finished")
        if (group !is TelegramGroupState.Open) return
        synchronized(lock) { openCheckAt = io.elapsedRealtime() }
        // Only a finished check that found no newer release takes a shown notice away.
        val update = group.update
        if (update == null) {
            io.clearUpdate()
        } else if (check.notify) {
            io.showUpdate(update.version.toString())
        }
    }

    /** The edition's group, or null when a log out or a new client made the result stale. */
    private suspend fun refresh(generation: Int, source: C): TelegramGroupState? =
        if (config().premiumEdition) refreshPremium(generation, source) else refreshOpen(generation, source)

    private suspend fun refreshPremium(generation: Int, source: C): TelegramGroupState? {
        val config = config()
        val group = io.loadGroup(source, config.premiumChatId, config.premiumTopicId)
        if (!clients.isCurrent(generation, source)) return null
        ui.update { it.copy(premiumGroup = group) }
        return group
    }

    private suspend fun refreshOpen(generation: Int, source: C): TelegramGroupState? {
        val config = config()
        val group = io.loadGroup(source, config.openChatId, config.openTopicId)
        val premium = io.premiumAccess(source, config.premiumChatId, config.premiumTopicId)
        if (!clients.isCurrent(generation, source)) return null
        ui.update { it.copy(openGroup = group, premiumGroup = premium) }
        return group
    }

    /** [notify] turns on when a check that posts the notification joins one that does not. */
    private class RunningCheck(val generation: Int, @Volatile var notify: Boolean) {
        lateinit var job: Job
    }
}
