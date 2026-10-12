package name.alexwayfer.customtv.ui.account

import android.app.Application
import android.net.ConnectivityManager
import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import name.alexwayfer.customtv.auth.DeviceApproval
import name.alexwayfer.customtv.auth.TWITCH_AUTH_TAG
import name.alexwayfer.customtv.auth.TwitchAccount
import name.alexwayfer.customtv.auth.TwitchAuthClient
import name.alexwayfer.customtv.auth.TwitchAuthException
import name.alexwayfer.customtv.auth.TwitchDeviceLogin
import name.alexwayfer.customtv.auth.TwitchLoginService
import name.alexwayfer.customtv.auth.TwitchSession
import name.alexwayfer.customtv.auth.TwitchTokens
import name.alexwayfer.customtv.auth.accessExpiryMillis
import name.alexwayfer.customtv.auth.awaitDeviceApproval
import name.alexwayfer.customtv.auth.loginNeedsLinkWarning
import name.alexwayfer.customtv.auth.pendingLoginPayload
import name.alexwayfer.customtv.auth.restorePendingLogin
import name.alexwayfer.customtv.diagnostics.AppLog
import java.io.IOException
import kotlin.time.Duration.Companion.milliseconds

private const val PENDING_LOGIN = "pending_login"

/**
 * A login through Twitch's device code: the page the user approves it on, the wait for that approval, and the login
 * kept in [savedState] while the process may be killed. The Twitch app claiming the login links holds it back first.
 */
internal class DeviceLoginFlow(
    private val application: Application,
    private val scope: CoroutineScope,
    private val savedState: SavedStateHandle,
    private val client: TwitchAuthClient,
    private val accountSession: AccountSession,
    private val notice: MutableStateFlow<AccountNotice?>,
) {
    private val _prompt = MutableStateFlow<DeviceLoginPrompt?>(null)
    val prompt: StateFlow<DeviceLoginPrompt?> = _prompt
    private val _twitchLinkWarning = MutableStateFlow(false)
    val twitchLinkWarning: StateFlow<Boolean> = _twitchLinkWarning
    private val _loggingIn = MutableStateFlow(false)
    val loggingIn: StateFlow<Boolean> = _loggingIn
    private var loginJob: Job? = null
    private var loginFinishing = false
    val reopen = LoginReopen()

    /** Goes on with the login the process was killed during, when it has not expired yet. */
    fun resumePending() {
        readPendingLogin()?.let { login ->
            _loggingIn.value = true
            _prompt.value = login.prompt()
            AppLog.i(TWITCH_AUTH_TAG, "device login resumed")
            startLogin(login)
        }
    }

    /** Starts a new login, or warns first when the Twitch app would take the login page's links. */
    fun open() {
        if (twitchHandlesLogin()) {
            _twitchLinkWarning.value = true
            return
        }
        startLogin(existing = null)
    }

    fun refreshTwitchLinks() {
        if (!_twitchLinkWarning.value) return
        if (twitchHandlesLogin()) return
        continueAfterTwitchLinks()
    }

    fun dismissTwitchLinkWarning() {
        _twitchLinkWarning.value = false
    }

    private fun continueAfterTwitchLinks() {
        if (twitchHandlesLogin()) {
            notice.value = AccountNotice.LinksStillClaimed
            return
        }
        _twitchLinkWarning.value = false
        _loggingIn.value = true
        startLogin(existing = null)
    }

    fun pageClosed() {
        if (loginFinishing) return
        AppLog.i(TWITCH_AUTH_TAG, "login page dismissed")
        cancel()
    }

    fun cancel() {
        savedState[PENDING_LOGIN] = null
        _prompt.value = null
        _twitchLinkWarning.value = false
        _loggingIn.value = false
        loginJob?.cancel()
    }

    private fun twitchHandlesLogin(): Boolean {
        val allowed = twitchLinkHandlingAllowed(application)
        val handler = if (allowed == null) {
            loginLinkHandlerPackage(application.packageManager)
        } else {
            null
        }
        AppLog.i(TWITCH_AUTH_TAG, "twitch link handling allowed=$allowed")
        return loginNeedsLinkWarning(allowed, handler)
    }

    private fun startLogin(existing: TwitchDeviceLogin?) {
        accountSession.beginLogin()
        val previousLogin = loginJob
        loginFinishing = false
        _loggingIn.value = true
        loginJob = scope.launch {
            val generation = accountSession.sessions.generation()
            previousLogin?.cancel()
            previousLogin?.join()
            try {
                client.useNetwork(application.getSystemService(ConnectivityManager::class.java).activeNetwork)
                TwitchLoginService.stayAwake(application, true)
                approveLogin(existing, generation)
            } catch (error: TwitchAuthException) {
                AppLog.w(TWITCH_AUTH_TAG, "login failed HTTP ${error.httpCode}")
                notice.value = AccountNotice.Failed
            } catch (error: IOException) {
                AppLog.w(TWITCH_AUTH_TAG, "login failed ${error.javaClass.simpleName}")
                notice.value = AccountNotice.Failed
            } finally {
                val job = coroutineContext[Job]
                if (loginJob === job) {
                    client.useNetwork(null)
                    TwitchLoginService.stayAwake(application, false)
                    _prompt.value = null
                    _loggingIn.value = false
                    loginFinishing = false
                    if (job?.isCancelled != true) savedState[PENDING_LOGIN] = null
                }
            }
        }
    }

    /** Shows the code page, waits for the user to approve the login there, and loads the account it gives. */
    private suspend fun approveLogin(existing: TwitchDeviceLogin?, generation: Int) {
        val login = existing ?: client.requestDeviceLogin()
        if (existing == null) {
            savedState[PENDING_LOGIN] = pendingLoginPayload(login, System.currentTimeMillis())
            AppLog.i(TWITCH_AUTH_TAG, "device login started")
        }
        reopen.reset()
        _prompt.value = login.prompt()
        withTimeoutOrNull(3_000.milliseconds) { reopen.awaitLaunch() }
        val approval = awaitDeviceApproval(
            login,
            client::pollDeviceLogin,
            log = { AppLog.w(TWITCH_AUTH_TAG, it) },
        )
        val tokens = (approval as? DeviceApproval.Approved)?.tokens
        if (tokens == null) {
            notice.value = if (approval == DeviceApproval.TimedOut) AccountNotice.TimedOut else AccountNotice.Failed
            return
        }
        loginFinishing = true
        reopen.approved()
        closeTwitchLoginPage(application)
        val account = accountSession.loadAccount(deviceLoginSession(tokens), generation, deviceGrant = true) ?: return
        accountSession.sessions.showAccount(account, generation) { accountSession.relogin.loggedIn() }
        AppLog.i(TWITCH_AUTH_TAG, "device login finished")
    }

    private fun readPendingLogin(): TwitchDeviceLogin? {
        val json = savedState.get<String>(PENDING_LOGIN) ?: return null
        val login = restorePendingLogin(json, System.currentTimeMillis())
        if (login == null) savedState[PENDING_LOGIN] = null
        return login
    }
}

internal data class DeviceLoginPrompt(
    val verificationUri: String,
)

/** The session a device login's tokens start, before the account is loaded into it. */
private fun deviceLoginSession(tokens: TwitchTokens): TwitchSession = TwitchSession(
    accessToken = tokens.accessToken,
    refreshToken = tokens.refreshToken,
    expiresAtMillis = accessExpiryMillis(System.currentTimeMillis(), tokens.expiresInSeconds),
    account = TwitchAccount(login = "", displayName = "", avatarUrl = null),
    grantedScopes = tokens.grantedScopes,
)

private fun TwitchDeviceLogin.prompt(): DeviceLoginPrompt {
    return DeviceLoginPrompt(verificationUri = verificationUri)
}
