package name.alexwayfer.customtv.auth

import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import java.io.IOException

internal sealed interface DeviceApproval {
    data class Approved(val tokens: TwitchTokens) : DeviceApproval
    data object TimedOut : DeviceApproval
    data object Failed : DeviceApproval
}

/**
 * Polls Twitch until the user approves the device login, refuses it, or the code expires.
 * A network error keeps waiting; a rejected request is thrown to the caller.
 */
internal suspend fun awaitDeviceApproval(
    login: TwitchDeviceLogin,
    poll: suspend (deviceCode: String) -> DeviceGrant,
    log: (String) -> Unit,
    nowMillis: () -> Long = System::currentTimeMillis,
    wait: suspend (millis: Long) -> Unit = { delay(it.milliseconds) },
): DeviceApproval {
    val deadline = nowMillis() + login.expiresInSeconds * 1000L
    var completedPolls = 0
    var outcome: DeviceApproval? = null
    while (outcome == null && nowMillis() < deadline) {
        wait(devicePollDelayMillis(completedPolls, login.intervalSeconds))
        if (nowMillis() >= deadline) break
        val grant = pollDevice(poll, login.deviceCode, log)
        if (grant != null) {
            completedPolls += 1
            outcome = deviceGrantOutcome(grant, log)
        }
    }
    if (outcome == null) log("device login expired")
    return outcome ?: DeviceApproval.TimedOut
}

/** One poll; null after a network error, which keeps waiting. */
private suspend fun pollDevice(
    poll: suspend (deviceCode: String) -> DeviceGrant,
    deviceCode: String,
    log: (String) -> Unit,
): DeviceGrant? = try {
    poll(deviceCode)
} catch (error: IOException) {
    if (!devicePollKeepsWaiting(error)) throw error
    log("device poll failed ${error.javaClass.simpleName}")
    null
}

/** How the login ends on [grant], or null while it keeps waiting. */
private fun deviceGrantOutcome(grant: DeviceGrant, log: (String) -> Unit): DeviceApproval? = when (grant) {
    DeviceGrant.Pending -> null
    DeviceGrant.SlowDown -> {
        log("device poll slow down")
        null
    }
    is DeviceGrant.Unavailable -> {
        log("device poll server HTTP ${grant.httpCode}")
        null
    }
    DeviceGrant.Denied -> {
        log("device login denied")
        DeviceApproval.Failed
    }
    DeviceGrant.Expired -> {
        log("device login expired")
        DeviceApproval.TimedOut
    }
    DeviceGrant.Failed -> DeviceApproval.Failed
    is DeviceGrant.Approved -> DeviceApproval.Approved(grant.tokens)
}
