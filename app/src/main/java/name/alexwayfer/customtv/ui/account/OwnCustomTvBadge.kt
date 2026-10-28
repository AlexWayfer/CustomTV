package name.alexwayfer.customtv.ui.account

import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import name.alexwayfer.customtv.data.AppSettingsStore
import name.alexwayfer.customtv.data.CUSTOM_TV_BADGES_TAG
import name.alexwayfer.customtv.data.CustomTvBadgeApi
import name.alexwayfer.customtv.data.CustomTvBadgeDevice
import name.alexwayfer.customtv.data.CustomTvBadgeEdition
import name.alexwayfer.customtv.data.CustomTvBadges
import name.alexwayfer.customtv.data.OWN_BADGE_REFRESH
import name.alexwayfer.customtv.data.OwnBadgeKey
import name.alexwayfer.customtv.data.OwnBadgeRegistration
import name.alexwayfer.customtv.data.OwnBadgeStep
import name.alexwayfer.customtv.data.customTvBadgeDevice
import name.alexwayfer.customtv.data.ownBadgeKey
import name.alexwayfer.customtv.data.ownBadgeStep
import name.alexwayfer.customtv.data.ownBadgeWait
import name.alexwayfer.customtv.diagnostics.AppLog
import kotlin.time.Duration

/** Keeps the user's own CustomTV badge on the server while the app is on screen. */
@Composable
internal fun OwnCustomTvBadgeSync(session: AccountSession) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle, session) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { OwnCustomTvBadge.keepUp(session) }
    }
}

private data class OwnBadgeInputs(val enabled: Boolean, val twitchUserId: String?, val device: CustomTvBadgeDevice)

/** The own badge as the switch, the logged-in Twitch account, and the edition say, following [ownBadgeStep]. */
private object OwnCustomTvBadge {
    /** Kept for the run, so coming back to the app does not register the badge again within the hour. */
    @Volatile
    private var registered: OwnBadgeRegistration? = null

    suspend fun keepUp(session: AccountSession) {
        combine(
            AppSettingsStore.restored.filterNotNull().map { it.ownCustomTvBadge },
            session.sessions.account.map { it?.userId },
            customTvBadgeDevice(),
            ::OwnBadgeInputs,
        ).distinctUntilChanged().collectLatest { inputs -> follow(inputs, session) }
    }

    private suspend fun follow(inputs: OwnBadgeInputs, session: AccountSession) {
        val wanted = ownBadgeKey(inputs.twitchUserId, inputs.device)
        while (true) {
            val now = SystemClock.elapsedRealtime()
            val pause = when (val step = ownBadgeStep(inputs.enabled, wanted, registered, now)) {
                OwnBadgeStep.Wait -> ownBadgeWait(wanted, registered, now)
                is OwnBadgeStep.Register -> afterTry(register(step.key, inputs.device, session))
                is OwnBadgeStep.Remove -> afterTry(remove(step.key, inputs.device, session))
            }
            delay(pause)
        }
    }

    private fun afterTry(done: Boolean): Duration = if (done) Duration.ZERO else OWN_BADGE_REFRESH

    private suspend fun register(key: OwnBadgeKey, device: CustomTvBadgeDevice, session: AccountSession): Boolean {
        // A login that changed meanwhile restarts the inputs with its own account.
        val access = session.userAccess()?.takeIf { it.userId == key.twitchUserId } ?: return false
        val secret = (device as? CustomTvBadgeDevice.Premium)?.secret
        val edition = CustomTvBadgeApi.register(access.accessToken, secret) ?: return false
        if (registered?.key != key) AppLog.i(CUSTOM_TV_BADGES_TAG, "own badge registered $edition")
        registered = OwnBadgeRegistration(key, SystemClock.elapsedRealtime())
        CustomTvBadges.showOwn(key.twitchUserId, edition)
        return true
    }

    /**
     * Takes the badge off: a Premium one by the device's secret, a free one by the token of its account. Without
     * them it is left to the server, which ends a free badge a day later and a Premium one when access ends.
     */
    private suspend fun remove(key: OwnBadgeKey, device: CustomTvBadgeDevice, session: AccountSession): Boolean {
        val removed = when (key.edition) {
            CustomTvBadgeEdition.Premium -> (device as? CustomTvBadgeDevice.Premium)?.let {
                CustomTvBadgeApi.removePremium(it.secret, key.twitchUserId)
            }
            CustomTvBadgeEdition.Free -> session.userAccess()?.takeIf { it.userId == key.twitchUserId }?.let {
                CustomTvBadgeApi.removeFree(it.accessToken)
            }
        }
        if (removed == false) return false
        registered = null
        if (removed == true) {
            AppLog.i(CUSTOM_TV_BADGES_TAG, "own badge removed")
            CustomTvBadges.showOwn(key.twitchUserId, null)
        }
        return true
    }
}
