package name.alexwayfer.customtv.ui.account

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import name.alexwayfer.customtv.diagnostics.AppLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import name.alexwayfer.customtv.BuildConfig
import name.alexwayfer.customtv.auth.TWITCH_AUTH_TAG
import name.alexwayfer.customtv.auth.TwitchAccount
import name.alexwayfer.customtv.auth.TwitchAuthClient
import name.alexwayfer.customtv.auth.loginClickShowsScopePrompt
import name.alexwayfer.customtv.data.FollowedChannelsCache
import name.alexwayfer.customtv.data.HomeLogInPromptStore
import name.alexwayfer.customtv.data.TwitchSessionStore

internal class AccountViewModel(
    application: Application,
    savedState: SavedStateHandle,
) : AndroidViewModel(application) {
    private val client = TwitchAuthClient(BuildConfig.TWITCH_CLIENT_ID)
    private val followsCache = FollowedChannelsCache(application)
    private val logInPrompt = HomeLogInPromptStore(application)
    private val _notice = MutableStateFlow<AccountNotice?>(null)
    val notice: StateFlow<AccountNotice?> = _notice
    val relogin = ReloginPrompts()
    val session = AccountSession(viewModelScope, accountSessionIo(client, TwitchSessionStore(application)), relogin)
    val deviceLogin = DeviceLoginFlow(application, viewModelScope, savedState, client, session, _notice)
    val account: StateFlow<TwitchAccount?> = session.sessions.account
    val loginPrompt: StateFlow<DeviceLoginPrompt?> = deviceLogin.prompt
    val twitchLinkWarning: StateFlow<Boolean> = deviceLogin.twitchLinkWarning
    val loggingIn: StateFlow<Boolean> = deviceLogin.loggingIn
    val reloginForPermissions: StateFlow<Boolean> = relogin.forPermissions
    val reloginForSession: StateFlow<Boolean> = relogin.forSession
    val followsReady: StateFlow<Boolean> = session.followsReady

    init {
        deviceLogin.resumePending()
    }

    fun logIn() {
        if (loginClickShowsScopePrompt(relogin.scopesPending)) {
            relogin.askForPermissions()
            return
        }
        if (!loginClickStarts(loggingIn.value)) return
        if (BuildConfig.TWITCH_CLIENT_ID.isBlank()) {
            _notice.value = AccountNotice.MissingClient
            return
        }
        deviceLogin.open()
    }

    fun confirmScopeRelogin() {
        relogin.clearPermissions()
        logIn()
        if (!loggingIn.value) relogin.scopesPending = true
    }

    fun confirmSessionRelogin() {
        relogin.hideSession()
        logIn()
    }

    fun rejectForNewPermissions() {
        if (loggingIn.value) return
        if (account.value == null && relogin.forPermissions.value) return
        session.clear()
        relogin.askForPermissions()
        AppLog.i(TWITCH_AUTH_TAG, "token scopes differ")
    }

    fun logOut() {
        session.clear {
            followsCache.clear()
            relogin.clearPermissions()
            relogin.hideSession()
        }
        deviceLogin.cancel()
        // Logged out, the offer to log in on Home shows again even if it was put away.
        viewModelScope.launch { logInPrompt.clearDismissed() }
    }

    fun consumeNotice() {
        _notice.value = null
    }
}

internal data class TwitchUserAccess(
    val accessToken: String,
    val userId: String,
)

internal enum class AccountNotice {
    Failed,
    TimedOut,
    MissingClient,
    LinksStillClaimed,
}
