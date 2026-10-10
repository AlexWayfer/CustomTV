package name.alexwayfer.customtv.ui.account

import android.app.Application
import android.net.ConnectivityManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import name.alexwayfer.customtv.diagnostics.AppLog
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import name.alexwayfer.customtv.BuildConfig
import name.alexwayfer.customtv.auth.DeviceApproval
import name.alexwayfer.customtv.auth.TwitchSessionHolder
import name.alexwayfer.customtv.auth.TwitchAccount
import name.alexwayfer.customtv.auth.TwitchAuthClient
import name.alexwayfer.customtv.auth.TwitchAuthException
import name.alexwayfer.customtv.auth.TwitchDeviceLogin
import name.alexwayfer.customtv.auth.TwitchSession
import name.alexwayfer.customtv.auth.accessExpiryMillis
import name.alexwayfer.customtv.auth.awaitDeviceApproval
import name.alexwayfer.customtv.auth.freshSavedFollowAccess
import name.alexwayfer.customtv.auth.pendingLoginPayload
import name.alexwayfer.customtv.auth.restorePendingLogin
import name.alexwayfer.customtv.auth.TwitchLoginService
import name.alexwayfer.customtv.data.ProfileDetailsRepository
import name.alexwayfer.customtv.auth.profileAfterRefresh
import name.alexwayfer.customtv.auth.rejectedAuthClearsSession
import name.alexwayfer.customtv.auth.followListCanLoad
import name.alexwayfer.customtv.auth.loginClickShowsScopePrompt
import name.alexwayfer.customtv.auth.rejectedSessionAsksToLogInAgain
import name.alexwayfer.customtv.auth.savedSessionNeedsNewLogin
import name.alexwayfer.customtv.auth.twitchLoginScopes
import name.alexwayfer.customtv.auth.ValidatedScopes
import name.alexwayfer.customtv.auth.loginNeedsLinkWarning
import name.alexwayfer.customtv.data.FollowedChannelsCache
import name.alexwayfer.customtv.data.HomeLogInPromptStore
import name.alexwayfer.customtv.data.TwitchSessionStore

internal class AccountViewModel(
    application: Application,
    private val savedState: SavedStateHandle,
) : AndroidViewModel(application) {
    private val client = TwitchAuthClient(BuildConfig.TWITCH_CLIENT_ID)
    private val store = TwitchSessionStore(application)
    private val followsCache = FollowedChannelsCache(application)
    private val logInPrompt = HomeLogInPromptStore(application)
    private val sessions = TwitchSessionHolder(store::read, store::write, store::clear, client::refresh)
    val account: StateFlow<TwitchAccount?> = sessions.account
    private val _notice = MutableStateFlow<AccountNotice?>(null)
    val notice: StateFlow<AccountNotice?> = _notice
    private val _loginPrompt = MutableStateFlow<DeviceLoginPrompt?>(null)
    val loginPrompt: StateFlow<DeviceLoginPrompt?> = _loginPrompt
    private val _twitchLinkWarning = MutableStateFlow(false)
    val twitchLinkWarning: StateFlow<Boolean> = _twitchLinkWarning
    private val _loggingIn = MutableStateFlow(false)
    val loggingIn: StateFlow<Boolean> = _loggingIn
    private val _reloginForPermissions = MutableStateFlow(false)
    val reloginForPermissions: StateFlow<Boolean> = _reloginForPermissions
    private val _reloginForSession = MutableStateFlow(false)
    val reloginForSession: StateFlow<Boolean> = _reloginForSession
    private val _scopeReloginPending = MutableStateFlow(false)
    private var dismissedScopePrompt = false
    private var dismissedSessionPrompt = false
    private val _followsReady = MutableStateFlow(false)
    val followsReady: StateFlow<Boolean> = _followsReady
    private var loginJob: Job? = null
    private var restoreJob: Job? = null
    private var loginFinishing = false
    private var loginOpenGeneration = 0

    init {
        val saved = sessions.session
        _followsReady.value = followListCanLoad(saved?.grantedScopes)
        if (saved != null && savedSessionNeedsNewLogin(saved.grantedScopes, twitchLoginScopes())) {
            promptForNewScopes()
            AppLog.i(TAG, "token scopes differ")
        }
        restoreJob = viewModelScope.launch { restore(saved) }
        readPendingLogin()?.let { login ->
            _loggingIn.value = true
            _loginPrompt.value = login.prompt()
            AppLog.i(TAG, "device login resumed")
            startLogin(login)
        }
    }

    fun logIn() {
        if (loginClickShowsScopePrompt(_scopeReloginPending.value)) {
            _reloginForPermissions.value = true
            return
        }
        if (!loginClickStarts(_loggingIn.value)) return
        if (BuildConfig.TWITCH_CLIENT_ID.isBlank()) {
            _notice.value = AccountNotice.MissingClient
            return
        }
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

    fun dismissPermissionsRelogin() {
        dismissedScopePrompt = true
        _reloginForPermissions.value = false
    }

    fun confirmScopeRelogin() {
        _scopeReloginPending.value = false
        _reloginForPermissions.value = false
        logIn()
        if (!_loggingIn.value) _scopeReloginPending.value = true
    }

    fun dismissSessionRelogin() {
        dismissedSessionPrompt = true
        _reloginForSession.value = false
    }

    fun confirmSessionRelogin() {
        _reloginForSession.value = false
        logIn()
    }

    fun rejectForNewPermissions() {
        if (_loggingIn.value) return
        if (account.value == null && _reloginForPermissions.value) return
        sessions.clear { _followsReady.value = false }
        restoreJob?.cancel()
        _reloginForPermissions.value = true
        AppLog.i(TAG, "token scopes differ")
    }

    private fun continueAfterTwitchLinks() {
        if (twitchHandlesLogin()) {
            _notice.value = AccountNotice.LinksStillClaimed
            return
        }
        _twitchLinkWarning.value = false
        _loggingIn.value = true
        startLogin(existing = null)
    }

    fun loginPageClosed() {
        if (loginFinishing) return
        AppLog.i(TAG, "login page dismissed")
        cancelLogin()
    }

    fun cancelLogin() {
        savedState[PENDING_LOGIN] = null
        _loginPrompt.value = null
        _twitchLinkWarning.value = false
        _loggingIn.value = false
        loginJob?.cancel()
    }

    private fun twitchHandlesLogin(): Boolean {
        val application = getApplication<Application>()
        val allowed = twitchLinkHandlingAllowed(application)
        val handler = if (allowed == null) {
            loginLinkHandlerPackage(application.packageManager)
        } else {
            null
        }
        AppLog.i(TAG, "twitch link handling allowed=$allowed")
        return loginNeedsLinkWarning(allowed, handler)
    }

    private fun startLogin(existing: TwitchDeviceLogin?) {
        restoreJob?.cancel()
        sessions.startGeneration { _followsReady.value = false }
        val previousLogin = loginJob
        loginFinishing = false
        _loggingIn.value = true
        loginJob = viewModelScope.launch {
            val generation = sessions.generation()
            previousLogin?.cancel()
            previousLogin?.join()
            try {
                val network = getApplication<Application>()
                    .getSystemService(ConnectivityManager::class.java)
                    .activeNetwork
                client.useNetwork(network)
                TwitchLoginService.stayAwake(getApplication(), true)
                val login = existing ?: client.requestDeviceLogin()
                if (existing == null) {
                    savedState[PENDING_LOGIN] = pendingLoginPayload(login, System.currentTimeMillis())
                    AppLog.i(TAG, "device login started")
                }
                val opened = CompletableDeferred<Unit>()
                val openGeneration = ++loginOpenGeneration
                onLoginPageLaunchAttempted = {
                    if (openGeneration == loginOpenGeneration) opened.complete(Unit)
                }
                _loginPrompt.value = login.prompt()
                withTimeoutOrNull(3_000.milliseconds) { opened.await() }
                val tokens = when (
                    val approval = awaitDeviceApproval(login, client::pollDeviceLogin, log = { AppLog.w(TAG, it) })
                ) {
                    is DeviceApproval.Approved -> approval.tokens
                    DeviceApproval.TimedOut -> {
                        _notice.value = AccountNotice.TimedOut
                        return@launch
                    }
                    DeviceApproval.Failed -> {
                        _notice.value = AccountNotice.Failed
                        return@launch
                    }
                }
                loginFinishing = true
                closeTwitchLoginPage(getApplication())
                val session = TwitchSession(
                    accessToken = tokens.accessToken,
                    refreshToken = tokens.refreshToken,
                    expiresAtMillis = accessExpiryMillis(System.currentTimeMillis(), tokens.expiresInSeconds),
                    account = TwitchAccount(login = "", displayName = "", avatarUrl = null),
                    grantedScopes = tokens.grantedScopes,
                )
                val account = loadAccount(session, generation, deviceGrant = true) ?: return@launch
                sessions.showAccount(account, generation) {
                    dismissedSessionPrompt = false
                    _reloginForSession.value = false
                }
                AppLog.i(TAG, "device login finished")
            } catch (error: CancellationException) {
                throw error
            } catch (error: TwitchAuthException) {
                AppLog.w(TAG, "login failed HTTP ${error.httpCode}")
                _notice.value = AccountNotice.Failed
            } catch (error: Exception) {
                AppLog.w(TAG, "login failed ${error.javaClass.simpleName}")
                _notice.value = AccountNotice.Failed
            } finally {
                val job = coroutineContext[Job]
                if (loginJob === job) {
                    client.useNetwork(null)
                    TwitchLoginService.stayAwake(getApplication(), false)
                    onLoginPageLaunchAttempted = null
                    _loginPrompt.value = null
                    _loggingIn.value = false
                    loginFinishing = false
                    if (job?.isCancelled != true) savedState[PENDING_LOGIN] = null
                }
            }
        }
    }

    private fun readPendingLogin(): TwitchDeviceLogin? {
        val json = savedState.get<String>(PENDING_LOGIN) ?: return null
        val login = restorePendingLogin(json, System.currentTimeMillis())
        if (login == null) savedState[PENDING_LOGIN] = null
        return login
    }

    fun logOut() {
        sessions.clear {
            followsCache.clear()
            _followsReady.value = false
            _scopeReloginPending.value = false
            _reloginForPermissions.value = false
            _reloginForSession.value = false
        }
        restoreJob?.cancel()
        cancelLogin()
        // Logged out, the offer to log in on Home shows again even if it was put away.
        viewModelScope.launch { logInPrompt.clearDismissed() }
    }

    fun consumeNotice() {
        _notice.value = null
    }

    private suspend fun restore(saved: TwitchSession?) {
        if (saved == null) return
        val generation = sessions.generation()
        try {
            val account = loadAccount(saved, generation) ?: return
            sessions.showAccount(account, generation)
        } catch (error: CancellationException) {
            throw error
        } catch (error: TwitchAuthException) {
            AppLog.w(TAG, "restore failed HTTP ${error.httpCode}")
            if (!rejectedAuthClearsSession(error.httpCode)) return
            dropRejectedSession(generation)
        } catch (error: Exception) {
            AppLog.w(TAG, "restore failed", error)
        }
    }

    internal fun freshSavedAccess(): TwitchUserAccess? {
        val session = sessions.session ?: return null
        val (token, userId) = freshSavedFollowAccess(
            session.accessToken,
            session.account.userId,
            session.expiresAtMillis,
            System.currentTimeMillis(),
        ) ?: return null
        return TwitchUserAccess(accessToken = token, userId = userId)
    }

    internal suspend fun userAccess(): TwitchUserAccess? {
        restoreJob?.join()
        val generation = sessions.generation()
        val session = sessions.session ?: return null
        val current = try {
            sessions.refreshIfDue(session, generation)
        } catch (error: CancellationException) {
            throw error
        } catch (error: TwitchAuthException) {
            AppLog.w(TAG, "access refresh failed HTTP ${error.httpCode}")
            if (rejectedAuthClearsSession(error.httpCode)) dropRejectedSession(generation)
            return null
        } catch (error: Exception) {
            AppLog.w(TAG, "access refresh failed ${error.javaClass.simpleName}")
            return null
        } ?: return null
        val userId = current.account.userId ?: loadUserId(current, generation) ?: return null
        return TwitchUserAccess(accessToken = current.accessToken, userId = userId)
    }

    private suspend fun loadUserId(session: TwitchSession, generation: Int): String? {
        return try {
            val fresh = client.account(session.accessToken)
            val merged = profileAfterRefresh(session.account, fresh, null)
            sessions.publish(session, merged, generation)?.userId
        } catch (error: CancellationException) {
            throw error
        } catch (error: TwitchAuthException) {
            AppLog.w(TAG, "user id failed HTTP ${error.httpCode}")
            if (rejectedAuthClearsSession(error.httpCode)) dropRejectedSession(generation)
            null
        } catch (error: Exception) {
            AppLog.w(TAG, "user id failed ${error.javaClass.simpleName}")
            null
        }
    }

    private suspend fun loadAccount(
        session: TwitchSession,
        generation: Int,
        deviceGrant: Boolean = false,
    ): TwitchAccount? {
        val current = sessions.refreshIfDue(session, generation, deviceGrant) ?: return null
        val account = try {
            val fresh = client.account(current.accessToken)
            sessions.publish(
                current,
                profileAfterRefresh(current.account, fresh, ProfileDetailsRepository.load(fresh.login)),
                generation,
            )
        } catch (error: TwitchAuthException) {
            if (error.httpCode != 401) throw error
            AppLog.i(TAG, "access rejected, refreshing")
            val refreshed = sessions.refreshRejected(current, generation) ?: return null
            val fresh = client.account(refreshed.accessToken)
            sessions.publish(
                refreshed,
                profileAfterRefresh(refreshed.account, fresh, ProfileDetailsRepository.load(fresh.login)),
                generation,
            )
        }
        if (account == null) return null
        return if (keepSessionScopes(generation)) account else null
    }

    private suspend fun keepSessionScopes(generation: Int): Boolean {
        val session = sessions.session ?: return false
        return when (val validated = client.validatedScopes(session.accessToken)) {
            is ValidatedScopes.Known -> {
                if (savedSessionNeedsNewLogin(validated.scopes, twitchLoginScopes())) {
                    AppLog.i(TAG, "token scopes differ")
                    if (session.grantedScopes != validated.scopes) {
                        sessions.publish(session.copy(grantedScopes = validated.scopes), session.account, generation)
                    }
                    keepSessionDespiteScopes(session.copy(grantedScopes = validated.scopes), generation)
                    true
                } else {
                    _scopeReloginPending.value = false
                    _reloginForPermissions.value = false
                    if (session.grantedScopes != validated.scopes) {
                        sessions.publish(session.copy(grantedScopes = validated.scopes), session.account, generation)
                    }
                    sessions.ifCurrent(generation) { _followsReady.value = true }
                    true
                }
            }
            ValidatedScopes.Unavailable -> true
            ValidatedScopes.Rejected -> {
                dropRejectedSession(generation)
                false
            }
        }
    }

    private fun promptForNewScopes() {
        _scopeReloginPending.value = true
        if (!dismissedScopePrompt) _reloginForPermissions.value = true
    }

    private fun keepSessionDespiteScopes(session: TwitchSession, generation: Int) {
        promptForNewScopes()
        if (followListCanLoad(session.grantedScopes)) {
            sessions.ifCurrent(generation) { _followsReady.value = true }
        }
    }

    private suspend fun dropRejectedSession(generation: Int) {
        sessions.clearIfCurrent(generation) { hadSession ->
            _followsReady.value = false
            _scopeReloginPending.value = false
            _reloginForPermissions.value = false
            if (rejectedSessionAsksToLogInAgain(hadSession, dismissedSessionPrompt)) {
                _reloginForSession.value = true
            }
        }
    }

    private companion object {
        const val TAG = "TwitchAuth"
        const val PENDING_LOGIN = "pending_login"
    }
}

internal data class TwitchUserAccess(
    val accessToken: String,
    val userId: String,
)

internal data class DeviceLoginPrompt(
    val verificationUri: String,
)

private fun TwitchDeviceLogin.prompt(): DeviceLoginPrompt {
    return DeviceLoginPrompt(verificationUri = verificationUri)
}

internal enum class AccountNotice {
    Failed,
    TimedOut,
    MissingClient,
    LinksStillClaimed,
}
