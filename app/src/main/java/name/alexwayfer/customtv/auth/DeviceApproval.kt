package name.alexwayfer.customtv.auth

import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

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
    while (nowMillis() < deadline) {
        wait(devicePollDelayMillis(completedPolls, login.intervalSeconds))
        if (nowMillis() >= deadline) break
        val grant = try {
            poll(login.deviceCode)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            if (!devicePollKeepsWaiting(error)) throw error
            log("device poll failed ${error.javaClass.simpleName}")
            continue
        }
        completedPolls += 1
        when (grant) {
            DeviceGrant.Pending -> Unit
            DeviceGrant.SlowDown -> log("device poll slow down")
            is DeviceGrant.Unavailable -> log("device poll server HTTP ${grant.httpCode}")
            DeviceGrant.Denied -> {
                log("device login denied")
                return DeviceApproval.Failed
            }
            DeviceGrant.Expired -> {
                log("device login expired")
                return DeviceApproval.TimedOut
            }
            DeviceGrant.Failed -> return DeviceApproval.Failed
            is DeviceGrant.Approved -> return DeviceApproval.Approved(grant.tokens)
        }
    }
    log("device login expired")
    return DeviceApproval.TimedOut
}
