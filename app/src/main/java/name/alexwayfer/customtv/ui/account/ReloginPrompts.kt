package name.alexwayfer.customtv.ui.account

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import name.alexwayfer.customtv.auth.rejectedSessionAsksToLogInAgain

/** The offers to log in again: for permissions the saved token lacks, or after Twitch refused the saved session. */
internal class ReloginPrompts {
    private val _forPermissions = MutableStateFlow(false)
    val forPermissions: StateFlow<Boolean> = _forPermissions
    private val _forSession = MutableStateFlow(false)
    val forSession: StateFlow<Boolean> = _forSession
    /** The saved token lacks a permission the app asks for, so a login click offers to log in again first. */
    var scopesPending = false
    private var dismissedScopePrompt = false
    private var dismissedSessionPrompt = false

    fun askForPermissions() {
        _forPermissions.value = true
    }

    fun dismissPermissions() {
        dismissedScopePrompt = true
        _forPermissions.value = false
    }

    fun promptForNewScopes() {
        scopesPending = true
        if (!dismissedScopePrompt) _forPermissions.value = true
    }

    /** The token has every permission the app asks for, or there is no token left to ask about. */
    fun clearPermissions() {
        scopesPending = false
        _forPermissions.value = false
    }

    fun dismissSession() {
        dismissedSessionPrompt = true
        _forSession.value = false
    }

    fun hideSession() {
        _forSession.value = false
    }

    /** A login succeeded, so a later refusal of it offers to log in again even if the last offer was put away. */
    fun loggedIn() {
        dismissedSessionPrompt = false
        _forSession.value = false
    }

    fun sessionRejected(hadSession: Boolean) {
        if (rejectedSessionAsksToLogInAgain(hadSession, dismissedSessionPrompt)) {
            _forSession.value = true
        }
    }
}
