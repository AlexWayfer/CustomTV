package name.alexwayfer.customtv.telegram

import android.content.Context
import android.os.Build
import java.io.File
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import name.alexwayfer.customtv.BuildConfig
import name.alexwayfer.customtv.diagnostics.AppLog
import org.drinkless.tdlib.Client
import org.drinkless.tdlib.TdApi

/** The log tag of the Telegram login, the update check, and the download. */
internal const val TELEGRAM_TAG = "Telegram"

internal object TelegramSession {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val gate = Any()
    private val _ui = MutableStateFlow(TelegramUiState())
    val ui: StateFlow<TelegramUiState> = _ui

    private var client: Client? = null
    private var started = false
    private var generation = 0
    private var parametersSent = false
    private var sawReady = false
    private var recreateOnClosed = false
    private var wipeOnClosed = false
    private var config = TelegramConfig()
    private var directory: File? = null
    private var appContext: Context? = null

    /** The last connection state TDLib reported, for the check's log lines. */
    @Volatile
    private var connection: String? = null

    val login = TelegramLogin(scope, _ui) { synchronized(gate) { client } }
    val download = TelegramUpdateDownload(scope, _ui) { synchronized(gate) { client } }
    val updates = TelegramUpdateCheck(
        scope,
        _ui,
        config = { config },
        clients = TelegramClients(current = { synchronized(gate) { generation to client } }, isCurrent = ::same),
        connection = { connection },
        io = telegramCheckIo { appContext },
    )

    fun start(context: Context, config: TelegramConfig) {
        synchronized(gate) {
            if (started) return
            started = true
            this.config = config
            appContext = context.applicationContext
            directory = context.filesDir.resolve("telegram")
        }
        openClient()
    }

    fun logOut() {
        val current = synchronized(gate) {
            generation += 1
            recreateOnClosed = true
            wipeOnClosed = false
            client
        } ?: return
        scope.launch {
            val result = current.request(TdApi.LogOut())
            if (result is TdApi.Error) {
                AppLog.w(TELEGRAM_TAG, "log out failed ${result.code}")
                synchronized(gate) { recreateOnClosed = false }
                _ui.update { it.copy(request = null, notice = result.message) }
            }
        }
    }

    fun startOver() {
        val current = synchronized(gate) { client }
        if (current == null) {
            synchronized(gate) {
                directory?.deleteRecursively()
                _ui.value = TelegramUiState()
            }
            openClient()
            return
        }
        synchronized(gate) {
            generation += 1
            recreateOnClosed = true
            wipeOnClosed = true
        }
        scope.launch { current.request(TdApi.Close()) }
    }

    private fun openClient() {
        if (directory == null) return
        try {
            System.loadLibrary("tdjni")
            Client.execute(TdApi.SetLogVerbosityLevel(1))
        } catch (error: UnsatisfiedLinkError) {
            AppLog.e(TELEGRAM_TAG, "native library failed", error)
            _ui.update { it.copy(step = TelegramLoginStep.Unsupported, notice = error.message, request = null) }
            return
        }
        parametersSent = false
        sawReady = false
        val opened = arrayOfNulls<Client>(1)
        val created = Client.create(
            { update -> opened[0]?.let { onUpdate(it, update) } },
            { error -> AppLog.e(TELEGRAM_TAG, "update failed", error) },
            { error -> AppLog.e(TELEGRAM_TAG, "client failed", error) },
        )
        opened[0] = created
        synchronized(gate) { client = created }
        AppLog.i(TELEGRAM_TAG, "client opened")
        scope.launch {
            val result = created.request(TdApi.GetAuthorizationState())
            if (result is TdApi.AuthorizationState) {
                onUpdate(created, TdApi.UpdateAuthorizationState(result))
            }
        }
    }

    private fun onUpdate(source: Client, update: TdApi.Object) {
        if (synchronized(gate) { client !== source }) return
        if (update is TdApi.UpdateConnectionState) {
            connection = update.state?.javaClass?.simpleName
            AppLog.i(TELEGRAM_TAG, "connection $connection")
            return
        }
        if (update is TdApi.UpdateFile) {
            update.file?.let(download::onFileUpdate)
            return
        }
        if (update !is TdApi.UpdateAuthorizationState) return
        val auth = update.authorizationState ?: return
        val signal = telegramAuthSignal(auth)
        val step = telegramLoginStep(signal)
        AppLog.i(TELEGRAM_TAG, "authorization $signal")
        // Where the code went decides where the user looks for it; the phone number stays out of the log.
        (auth as? TdApi.AuthorizationStateWaitCode)?.codeInfo?.let { info ->
            AppLog.i(
                TELEGRAM_TAG,
                "code sent ${info.type?.javaClass?.simpleName}, next ${info.nextType?.javaClass?.simpleName}, " +
                    "timeout ${info.timeout}",
            )
        }
        _ui.update {
            it.copy(
                step = step,
                passwordHint = (auth as? TdApi.AuthorizationStateWaitPassword)
                    ?.passwordHint
                    ?.takeIf { hint -> hint.isNotBlank() },
                code = (auth as? TdApi.AuthorizationStateWaitCode)?.codeInfo?.let(::telegramCodeInfo),
                registrationRejected = signal == TelegramAuthSignal.WaitRegistration,
                request = null,
            )
        }
        when (signal) {
            TelegramAuthSignal.WaitTdlibParameters -> sendParameters(source)
            TelegramAuthSignal.Ready -> onReady(source)
            TelegramAuthSignal.Closed -> onClosed(source)
            else -> Unit
        }
    }

    private fun sendParameters(source: Client) {
        val dir = directory ?: return
        synchronized(gate) {
            if (client !== source || parametersSent) return
            parametersSent = true
        }
        scope.launch {
            val result = source.request(
                TdApi.SetTdlibParameters(
                    false,
                    dir.absolutePath,
                    File(dir, "files").absolutePath,
                    ByteArray(0),
                    true,
                    true,
                    true,
                    false,
                    config.apiId,
                    config.apiHash,
                    Locale.getDefault().language.ifBlank { "en" },
                    Build.MODEL,
                    Build.VERSION.RELEASE,
                    BuildConfig.VERSION_NAME,
                ),
            )
            if (result is TdApi.Error) {
                AppLog.w(TELEGRAM_TAG, "parameters rejected ${result.code}")
                synchronized(gate) {
                    if (client === source) parametersSent = false
                }
                _ui.update { it.copy(step = TelegramLoginStep.Unsupported, notice = result.message) }
            }
        }
    }

    private fun onReady(source: Client) {
        val generation = synchronized(gate) {
            if (client !== source || sawReady) return
            sawReady = true
            this.generation
        }
        _ui.update {
            it.withEditionChecking(config).copy(
                notice = null,
                registrationRejected = false,
            )
        }
        scope.launch {
            AppLog.i(TELEGRAM_TAG, "account requested")
            val me = source.request(TdApi.GetMe())
            if (!same(generation, source)) return@launch
            AppLog.i(TELEGRAM_TAG, "account answered ${me.javaClass.simpleName}")
            if (me is TdApi.User) {
                val username = me.usernames?.activeUsernames?.firstOrNull()
                val label = telegramAccountLabel(
                    me.firstName.orEmpty(),
                    me.lastName.orEmpty(),
                    username,
                ).ifBlank { me.id.toString() }
                _ui.update { it.copy(accountLabel = label, accountUsername = username) }
                AppLog.i(TELEGRAM_TAG, "logged in")
            } else if (me is TdApi.Error) {
                AppLog.w(TELEGRAM_TAG, "account failed ${me.code}")
            }
            updates.launchCheck(generation, source, notify = true)
        }
    }

    private fun onClosed(source: Client) {
        val action = synchronized(gate) {
            if (client !== source) return
            client = null
            parametersSent = false
            val recreate = recreateOnClosed
            val wipe = wipeOnClosed
            recreateOnClosed = false
            wipeOnClosed = false
            recreate to wipe
        }
        if (!action.first) return
        synchronized(gate) {
            if (action.second) directory?.deleteRecursively()
            _ui.value = TelegramUiState()
        }
        openClient()
    }

    private fun same(generation: Int, source: Client): Boolean =
        synchronized(gate) { this.generation == generation && client === source }
}
