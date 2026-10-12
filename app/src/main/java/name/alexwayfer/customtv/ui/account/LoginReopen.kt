package name.alexwayfer.customtv.ui.account

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Whether the login screen offers to open the login page again. It does once the page was opened and the app is in
 * front again while the login still waits, as when a plain tab closed unseen or the page failed to open. It does not
 * while the page covers the app, before the page opens, or after the login was approved and the page closed for it.
 */
internal fun loginReopenShown(pageOpened: Boolean, pageOnScreen: Boolean, approved: Boolean): Boolean =
    pageOpened && !pageOnScreen && !approved

/** Follows the login page over the app and offers to open it again by [loginReopenShown]. */
internal class LoginReopen {
    private val mutableShown = MutableStateFlow(false)
    val shown: StateFlow<Boolean> = mutableShown
    private var launched = CompletableDeferred<Unit>()
    private var pageOpened = false
    private var pageOnScreen = false
    private var approved = false

    /** A new login page is about to open. */
    fun reset() {
        launched = CompletableDeferred()
        pageOpened = false
        pageOnScreen = false
        approved = false
        update()
    }

    /** The page was launched; [shown] is false when it failed to open. */
    fun pageLaunched(shown: Boolean) {
        pageOpened = true
        pageOnScreen = shown
        launched.complete(Unit)
        update()
    }

    /** Waits until the page opened since the last [reset] was launched. */
    suspend fun awaitLaunch() {
        launched.await()
    }

    /** The app is in front again, so the login page no longer covers it. */
    fun appResumed() {
        pageOnScreen = false
        update()
    }

    fun approved() {
        approved = true
        update()
    }

    private fun update() {
        mutableShown.value = loginReopenShown(pageOpened, pageOnScreen, approved)
    }
}
