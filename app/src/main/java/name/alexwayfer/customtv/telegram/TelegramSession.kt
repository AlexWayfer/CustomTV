package name.alexwayfer.customtv.telegram

import android.content.Context
import android.os.Build
import android.os.SystemClock
import java.io.File
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import name.alexwayfer.customtv.BuildConfig
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.diagnostics.AppLog
import name.alexwayfer.customtv.ui.UiText
import name.alexwayfer.customtv.update.AppRelease
import name.alexwayfer.customtv.update.AppUpdateDownload
import name.alexwayfer.customtv.update.AppVersion
import name.alexwayfer.customtv.update.appRelease
import name.alexwayfer.customtv.update.cancelAppUpdateNotification
import name.alexwayfer.customtv.update.downloadFraction
import name.alexwayfer.customtv.update.installedDownloads
import name.alexwayfer.customtv.update.newestRelease
import name.alexwayfer.customtv.update.showAppUpdateNotification
import org.drinkless.tdlib.Client
import org.drinkless.tdlib.TdApi

internal object TelegramSession {
    private const val TAG = "Telegram"
    private const val POST_PAGE = 10

    // The highest TDLib priority: the user waits for this file.
    private const val DOWNLOAD_PRIORITY = 32
    private val BACKGROUND_CHECK_TIMEOUT = 1.minutes

    // TDLib waits for Telegram instead of failing, so a check without an answer ends here.
    private val CHECK_TIMEOUT = 30.seconds
    private val CHECK_TIMEOUT_MESSAGE = UiText.Resource(R.string.telegram_did_not_answer)
    private val UNEXPECTED_RESPONSE = UiText.Resource(R.string.telegram_unexpected_response)

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
    private var apiId = 0
    private var apiHash = ""
    private var openChatId = 0L
    private var openTopicId = 0
    private var premiumChatId = 0L
    private var premiumTopicId = 0
    private var premiumEdition = false
    private var directory: File? = null
    private var appContext: Context? = null
    private var runningCheck: RunningCheck? = null

    /** `elapsedRealtime` of the last check that found the group open; only in memory, so a new process checks. */
    private var openCheckAt: Long? = null

    /** The last connection state TDLib reported, for the check's log lines. */
    @Volatile
    private var connection: String? = null

    fun start(
        context: Context,
        apiId: Int,
        apiHash: String,
        openChatId: Long,
        openTopicId: Int,
        premiumChatId: Long,
        premiumTopicId: Int,
        premiumEdition: Boolean,
    ) {
        synchronized(gate) {
            if (started) return
            started = true
            this.apiId = apiId
            this.apiHash = apiHash
            this.openChatId = openChatId
            this.openTopicId = openTopicId
            this.premiumChatId = premiumChatId
            this.premiumTopicId = premiumTopicId
            this.premiumEdition = premiumEdition
            appContext = context.applicationContext
            directory = context.filesDir.resolve("telegram")
        }
        openClient()
    }

    fun submitPhone(phone: String) = submit(
        phone,
        TelegramLoginStep.Phone,
        TelegramLoginStep.Confirm,
    ) { value ->
        TdApi.SetAuthenticationPhoneNumber(
            value,
            TdApi.PhoneNumberAuthenticationSettings(
                false,
                false,
                false,
                false,
                false,
                null,
                emptyArray(),
            ),
        )
    }

    fun submitCode(code: String) = submit(code, TelegramLoginStep.Code) { value ->
        TdApi.CheckAuthenticationCode(value)
    }

    fun submitPassword(password: String) = submit(password, TelegramLoginStep.Password) { value ->
        TdApi.CheckAuthenticationPassword(value)
    }

    /** Sends the code again the next way Telegram named; the new state brings where it went. */
    fun resendCode() {
        if (_ui.value.step != TelegramLoginStep.Code || _ui.value.code?.resendTo == null) return
        val current = synchronized(gate) { client } ?: return
        scope.launch {
            _ui.update { it.copy(request = TelegramRequest.Resend, notice = null) }
            val result = send(current, TdApi.ResendAuthenticationCode(TdApi.ResendCodeReasonUserRequest()))
            if (result is TdApi.Error) {
                AppLog.w(TAG, "code resend rejected ${result.code}")
                _ui.update { it.copy(request = null, notice = result.message) }
            } else {
                _ui.update { it.copy(request = null) }
            }
        }
    }

    fun logOut() {
        val current = synchronized(gate) {
            generation += 1
            recreateOnClosed = true
            wipeOnClosed = false
            client
        } ?: return
        scope.launch {
            val result = send(current, TdApi.LogOut())
            if (result is TdApi.Error) {
                AppLog.w(TAG, "log out failed ${result.code}")
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
        scope.launch { send(current, TdApi.Close()) }
    }

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
        synchronized(gate) { openCheckAt }?.let { (SystemClock.elapsedRealtime() - it).milliseconds }

    fun downloadUpdate(fileId: Int) {
        val current = synchronized(gate) { client } ?: return
        if (_ui.value.download is AppUpdateDownload.Running) return
        AppLog.i(TAG, "download started")
        _ui.update { it.copy(download = AppUpdateDownload.Running(fileId, 0f)) }
        scope.launch {
            val result = send(current, TdApi.DownloadFile(fileId, DOWNLOAD_PRIORITY, 0L, 0L, true))
            val path = (result as? TdApi.File)?.local?.takeIf { it.isDownloadingCompleted }?.path
            val state = when {
                !path.isNullOrEmpty() -> {
                    AppLog.i(TAG, "download finished")
                    AppUpdateDownload.Ready(fileId, path)
                }
                result is TdApi.Error -> {
                    AppLog.w(TAG, "download failed ${result.code}")
                    AppUpdateDownload.Failed(fileId, UiText.Raw(result.message))
                }
                else -> {
                    AppLog.w(TAG, "download stopped ${result.javaClass.simpleName}")
                    AppUpdateDownload.Failed(fileId, UiText.Resource(R.string.telegram_download_stopped))
                }
            }
            _ui.update { it.copy(download = state) }
        }
    }

    private fun startCheck(notify: Boolean): Job? {
        if (_ui.value.step != TelegramLoginStep.Ready) return null
        val (generation, current) = synchronized(gate) { this.generation to client }
        if (current == null) return null
        return launchCheck(generation, current, notify)
    }

    /** Starts a check, or joins the one already running for this client, so two never run at once. */
    private fun launchCheck(generation: Int, source: Client, notify: Boolean): Job {
        val (check, fresh) = synchronized(gate) {
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
            AppLog.i(TAG, "check started, connection $connection")
            _ui.update { withEditionChecking(it) }
            check.job.start()
        } else {
            AppLog.i(TAG, "check joined")
        }
        return check.job
    }

    private fun onFileUpdate(file: TdApi.File) {
        _ui.update { state ->
            val running = state.download as? AppUpdateDownload.Running
            if (running == null || running.fileId != file.id) return@update state
            val fraction = downloadFraction(file.local?.downloadedSize ?: 0L, file.size, file.expectedSize)
            state.copy(download = running.copy(fraction = fraction))
        }
    }

    private fun submit(
        raw: String,
        vararg allowed: TelegramLoginStep,
        function: (String) -> TdApi.Function<*>,
    ) {
        val value = raw.trim()
        if (value.isEmpty() || _ui.value.step !in allowed) return
        val current = synchronized(gate) { client } ?: return
        scope.launch {
            _ui.update { it.copy(request = TelegramRequest.Submit, notice = null) }
            val result = send(current, function(value))
            if (result is TdApi.Error) {
                AppLog.w(TAG, "authentication rejected ${result.code}")
                _ui.update { it.copy(request = null, notice = result.message) }
            } else {
                _ui.update { it.copy(request = null) }
            }
        }
    }

    private fun openClient() {
        if (directory == null) return
        try {
            System.loadLibrary("tdjni")
            Client.execute(TdApi.SetLogVerbosityLevel(1))
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            AppLog.e(TAG, "native library failed", error)
            _ui.update { it.copy(step = TelegramLoginStep.Unsupported, notice = error.message, request = null) }
            return
        }
        parametersSent = false
        sawReady = false
        val opened = arrayOfNulls<Client>(1)
        val created = Client.create(
            { update -> opened[0]?.let { onUpdate(it, update) } },
            { error -> AppLog.e(TAG, "update failed", error) },
            { error -> AppLog.e(TAG, "client failed", error) },
        )
        opened[0] = created
        synchronized(gate) { client = created }
        AppLog.i(TAG, "client opened")
        scope.launch {
            val result = send(created, TdApi.GetAuthorizationState())
            if (result is TdApi.AuthorizationState) {
                onUpdate(created, TdApi.UpdateAuthorizationState(result))
            }
        }
    }

    private fun onUpdate(source: Client, update: TdApi.Object) {
        if (synchronized(gate) { client !== source }) return
        if (update is TdApi.UpdateConnectionState) {
            connection = update.state?.javaClass?.simpleName
            AppLog.i(TAG, "connection $connection")
            return
        }
        if (update is TdApi.UpdateFile) {
            update.file?.let(::onFileUpdate)
            return
        }
        if (update !is TdApi.UpdateAuthorizationState) return
        val auth = update.authorizationState ?: return
        val signal = signalOf(auth)
        val step = telegramLoginStep(signal)
        AppLog.i(TAG, "authorization $signal")
        // Where the code went decides where the user looks for it; the phone number stays out of the log.
        (auth as? TdApi.AuthorizationStateWaitCode)?.codeInfo?.let { info ->
            AppLog.i(
                TAG,
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
                code = (auth as? TdApi.AuthorizationStateWaitCode)?.codeInfo?.let(::telegramCodeInfo),                registrationRejected = signal == TelegramAuthSignal.WaitRegistration,
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
            val result = send(
                source,
                TdApi.SetTdlibParameters(
                    false,
                    dir.absolutePath,
                    File(dir, "files").absolutePath,
                    ByteArray(0),
                    true,
                    true,
                    true,
                    false,
                    apiId,
                    apiHash,
                    Locale.getDefault().language.ifBlank { "en" },
                    Build.MODEL,
                    Build.VERSION.RELEASE,
                    BuildConfig.VERSION_NAME,
                ),
            )
            if (result is TdApi.Error) {
                AppLog.w(TAG, "parameters rejected ${result.code}")
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
            withEditionChecking(it).copy(
                notice = null,
                registrationRejected = false,
            )
        }
        scope.launch {
            AppLog.i(TAG, "account requested")
            val me = send(source, TdApi.GetMe())
            if (!same(generation, source)) return@launch
            AppLog.i(TAG, "account answered ${me.javaClass.simpleName}")
            if (me is TdApi.User) {
                val username = me.usernames?.activeUsernames?.firstOrNull()
                val label = telegramAccountLabel(
                    me.firstName.orEmpty(),
                    me.lastName.orEmpty(),
                    username,
                ).ifBlank { me.id.toString() }
                _ui.update { it.copy(accountLabel = label, accountUsername = username) }
                AppLog.i(TAG, "logged in")
            } else if (me is TdApi.Error) {
                AppLog.w(TAG, "account failed ${me.code}")
            }
            launchCheck(generation, source, notify = true)
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

    private suspend fun runCheck(generation: Int, source: Client, check: RunningCheck) {
        try {
            var answered = false
            val fresh = withTimeoutOrNull(CHECK_TIMEOUT) {
                refresh(generation, source).also { answered = true }
            }
            if (!answered) {
                AppLog.w(TAG, "check timed out, connection $connection")
                if (same(generation, source)) {
                    _ui.update { editionGroup(it, TelegramGroupState.Failed(CHECK_TIMEOUT_MESSAGE)) }
                }
                return
            }
            val group = fresh ?: return
            AppLog.i(TAG, "check finished")
            if (group is TelegramGroupState.Open) {
                synchronized(gate) { openCheckAt = SystemClock.elapsedRealtime() }
            }
            val context = appContext ?: return
            // Only a finished check that found no newer release takes a shown notice away.
            if (group !is TelegramGroupState.Open) return
            val update = group.update
            if (update == null) {
                cancelAppUpdateNotification(context)
            } else if (check.notify) {
                showAppUpdateNotification(context, update.version.toString())
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            AppLog.w(TAG, "check failed ${error.javaClass.simpleName}")
            if (!same(generation, source)) return
            val message = error.message ?: error.javaClass.simpleName
            _ui.update { editionGroup(it, TelegramGroupState.Failed(UiText.Raw(message))) }
        }
    }

    private fun editionGroup(state: TelegramUiState, group: TelegramGroupState): TelegramUiState =
        if (premiumEdition) {
            state.copy(premiumGroup = group)
        } else {
            state.copy(openGroup = group)
        }

    /** The edition's group, or null when a log out or a new client made the result stale. */
    private suspend fun refresh(generation: Int, source: Client): TelegramGroupState? {
        if (premiumEdition) {
            val group = loadGroup(source, premiumChatId, premiumTopicId)
            if (!same(generation, source)) return null
            _ui.update { it.copy(premiumGroup = group) }
            return group
        }
        val group = loadGroup(source, openChatId, openTopicId)
        val premium = premiumGroupAccess(source)
        if (!same(generation, source)) return null
        _ui.update { it.copy(openGroup = group, premiumGroup = premium) }
        return group
    }

    /**
     * The free build only asks whether this account opens the Premium group, so Upgrade to Premium can
     * lead a buyer straight to its topic. Open means access; its releases are not read.
     */
    private suspend fun premiumGroupAccess(source: Client): TelegramGroupState {
        if (premiumChatId == 0L) return TelegramGroupState.NotConfigured
        return when (val opened = openChat(source, premiumChatId, premiumTopicId)) {
            is OpenedChat.Ready -> TelegramGroupState.Open(null)
            is OpenedChat.Stopped -> opened.state
        }
    }

    private fun withEditionChecking(state: TelegramUiState): TelegramUiState =
        if (premiumEdition) {
            state.copy(premiumGroup = groupPlaceholder(premiumChatId))
        } else {
            state.copy(openGroup = groupPlaceholder(openChatId))
        }

    private suspend fun loadGroup(source: Client, chatId: Long, topicId: Int): TelegramGroupState {
        AppLog.i(TAG, "check chat $chatId topic $topicId")
        if (chatId == 0L) return TelegramGroupState.NotConfigured
        val chat = when (val opened = openChat(source, chatId, topicId)) {
            is OpenedChat.Ready -> opened.chat
            is OpenedChat.Stopped -> return opened.state
        }
        return when (val request = telegramHistoryRequest(chat.id, chat.viewAsTopics, topicId)) {
            TelegramHistoryRequest.ChatMissing -> TelegramGroupState.NotConfigured
            TelegramHistoryRequest.TopicMissing -> TelegramGroupState.TopicNotConfigured
            TelegramHistoryRequest.WholeChat -> openGroup(
                source,
                loadReleases(source, TdApi.GetChatHistory(chat.id, 0L, 0, POST_PAGE, false)),
            )
            is TelegramHistoryRequest.Topic -> loadTopic(source, chat, request.topicId)
        }
    }

    private suspend fun openChat(source: Client, chatId: Long, topicId: Int): OpenedChat {
        val link = telegramSupergroupLink(chatId, topicId)
        val resolvedId = if (link == null) {
            chatId
        } else {
            when (val info = send(source, TdApi.GetMessageLinkInfo(link))) {
                is TdApi.Error -> {
                    AppLog.w(TAG, "chat $chatId failed ${info.code} ${info.message}")
                    return OpenedChat.Stopped(groupError(info.code, info.message))
                }
                is TdApi.MessageLinkInfo -> {
                    if (info.chatId == 0L) return OpenedChat.Stopped(TelegramGroupState.Unavailable)
                    AppLog.i(TAG, "check link ${info.chatId}")
                    info.chatId
                }
                else -> return OpenedChat.Stopped(TelegramGroupState.Failed(UNEXPECTED_RESPONSE))
            }
        }
        return when (val chat = send(source, TdApi.GetChat(resolvedId))) {
            is TdApi.Chat -> OpenedChat.Ready(chat)
            is TdApi.Error -> {
                AppLog.w(TAG, "chat $resolvedId failed ${chat.code} ${chat.message}")
                OpenedChat.Stopped(
                    if (link == null) {
                        groupError(chat.code, chat.message)
                    } else {
                        TelegramGroupState.Failed(UiText.Raw(chat.message))
                    },
                )
            }
            else -> OpenedChat.Stopped(TelegramGroupState.Failed(UNEXPECTED_RESPONSE))
        }
    }

    private fun groupError(code: Int, message: String): TelegramGroupState =
        if (telegramChatUnavailable(code, message)) {
            TelegramGroupState.Unavailable
        } else {
            TelegramGroupState.Failed(UiText.Raw(message))
        }

    private suspend fun loadTopic(source: Client, chat: TdApi.Chat, topicId: Int): TelegramGroupState {
        val topic = send(source, TdApi.GetForumTopic(chat.id, topicId))
        if (topic is TdApi.Error) {
            AppLog.w(TAG, "topic ${chat.id} $topicId failed ${topic.code} ${topic.message}")
            return TelegramGroupState.Failed(UiText.Raw(topic.message))
        }
        if (topic !is TdApi.ForumTopic) return TelegramGroupState.Failed(UNEXPECTED_RESPONSE)
        return openGroup(
            source,
            loadReleases(source, TdApi.GetForumTopicHistory(chat.id, topicId, 0L, 0, POST_PAGE)),
        )
    }

    /** Drops the downloaded files of releases already installed, then offers the newest one if newer. */
    private suspend fun openGroup(source: Client, releases: List<AppRelease>): TelegramGroupState.Open {
        val installed = AppVersion.parse(BuildConfig.VERSION_NAME) ?: AppVersion.ZERO
        for (release in installedDownloads(releases, installed)) {
            val result = send(source, TdApi.DeleteFile(release.fileId))
            if (result is TdApi.Error) AppLog.w(TAG, "delete file failed ${result.code}")
        }
        val newest = releases.maxByOrNull { it.version }
        AppLog.i(TAG, "releases ${releases.size}, newest ${newest?.version}, installed $installed")
        return TelegramGroupState.Open(newestRelease(releases, installed))
    }

    private suspend fun loadReleases(
        source: Client,
        request: TdApi.Function<TdApi.Messages>,
    ): List<AppRelease> {
        val result = send(source, request)
        if (result is TdApi.Error) {
            AppLog.w(TAG, "posts failed ${result.code}")
            return emptyList()
        }
        if (result !is TdApi.Messages) return emptyList()
        return result.messages?.filterNotNull()?.mapNotNull(::releaseOf).orEmpty()
    }

    private fun releaseOf(message: TdApi.Message): AppRelease? {
        val content = message.content as? TdApi.MessageDocument ?: return null
        val document = content.document ?: return null
        val file = document.document ?: return null
        return appRelease(
            fileId = file.id,
            fileName = document.fileName.orEmpty(),
            mimeType = document.mimeType.orEmpty(),
            downloadedPath = file.local?.takeIf { it.isDownloadingCompleted }?.path,
        )
    }

    private fun groupPlaceholder(chatId: Long): TelegramGroupState =
        if (chatId == 0L) TelegramGroupState.NotConfigured else TelegramGroupState.Checking

    private fun same(generation: Int, source: Client): Boolean =
        synchronized(gate) { this.generation == generation && client === source }

    private suspend fun send(source: Client, function: TdApi.Function<*>): TdApi.Object = try {
        suspendCancellableCoroutine { cont ->
            source.send(function) { result ->
                if (cont.isActive) cont.resume(result)
            }
        }
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        AppLog.w(TAG, "request failed ${error.javaClass.simpleName}")
        TdApi.Error(500, error.message ?: error.javaClass.simpleName)
    }

    /** [notify] turns on when a check that posts the notification joins one that does not. */
    private class RunningCheck(val generation: Int, @Volatile var notify: Boolean) {
        lateinit var job: Job
    }

    private sealed class OpenedChat {
        class Ready(val chat: TdApi.Chat) : OpenedChat()
        class Stopped(val state: TelegramGroupState) : OpenedChat()
    }

    private fun signalOf(state: TdApi.AuthorizationState): TelegramAuthSignal = when (state) {
        is TdApi.AuthorizationStateWaitTdlibParameters -> TelegramAuthSignal.WaitTdlibParameters
        is TdApi.AuthorizationStateWaitPhoneNumber -> TelegramAuthSignal.WaitPhoneNumber
        is TdApi.AuthorizationStateWaitCode -> TelegramAuthSignal.WaitCode
        is TdApi.AuthorizationStateWaitPassword -> TelegramAuthSignal.WaitPassword
        is TdApi.AuthorizationStateWaitRegistration -> TelegramAuthSignal.WaitRegistration
        is TdApi.AuthorizationStateWaitEmailAddress -> TelegramAuthSignal.WaitEmailAddress
        is TdApi.AuthorizationStateWaitEmailCode -> TelegramAuthSignal.WaitEmailCode
        is TdApi.AuthorizationStateWaitOtherDeviceConfirmation -> TelegramAuthSignal.WaitOtherDevice
        is TdApi.AuthorizationStateWaitPremiumPurchase -> TelegramAuthSignal.WaitPremiumPurchase
        is TdApi.AuthorizationStateReady -> TelegramAuthSignal.Ready
        is TdApi.AuthorizationStateLoggingOut -> TelegramAuthSignal.LoggingOut
        is TdApi.AuthorizationStateClosing -> TelegramAuthSignal.Closing
        is TdApi.AuthorizationStateClosed -> TelegramAuthSignal.Closed
        else -> TelegramAuthSignal.Unknown
    }
}
