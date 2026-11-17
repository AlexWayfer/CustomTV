package name.alexwayfer.customtv.data

import android.content.Context
import android.os.Build
import android.os.SystemClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import name.alexwayfer.customtv.BuildConfig
import name.alexwayfer.customtv.diagnostics.AppLog
import java.io.IOException
import java.security.GeneralSecurityException
import java.security.ProviderException
import java.time.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.milliseconds

/** Runs only while the app's visible lifecycle and the restored privacy switch permit it. */
internal object ActivityStatisticsCollector {
    private val mutex = Mutex()
    private var retryAt = 0L
    private var failures = 0

    suspend fun collect(context: Context) = withContext(Dispatchers.IO) {
        try {
            val enabled = ActivityStatisticsPreferences(context).enabled.onEach { allowed ->
                if (!allowed) AppLog.i(ACTIVITY_STATISTICS_TAG, "collection disabled by setting")
            }
            collectActivityStatistics(enabled, activityStatisticsAuthorization) {
                while (currentCoroutineContext().isActive) {
                    val wait = mutex.withLock { attempt(context, it) }
                    delay(wait)
                }
            }
        } catch (error: IOException) {
            AppLog.w(ACTIVITY_STATISTICS_TAG, "settings unavailable: ${error.javaClass.simpleName}")
        }
    }

    private suspend fun attempt(context: Context, access: ActivityAuthorization): Duration {
        val remaining = retryAt - SystemClock.elapsedRealtime()
        if (remaining > 0) return remaining.milliseconds
        try {
            val ports = ActivityReportPorts(
                ActivityIdentityFile(context), ActivityKeyStore(), ActivityHttpTransport(access),
            )
            ActivityReporter(ports).submit(BuildConfig.VERSION_NAME, activityOsVersion(Build.VERSION.RELEASE,
                Build.VERSION.SDK_INT), access.edition, Instant.now(), attest = !BuildConfig.DEBUG)
            failures = 0
            retryAt = 0
            return POLL_INTERVAL
        } catch (error: ActivityRequestFailed) {
            AppLog.w(ACTIVITY_STATISTICS_TAG, "report deferred: HTTP ${error.answer.code}")
        } catch (error: IOException) {
            AppLog.w(ACTIVITY_STATISTICS_TAG, "report deferred: ${error.javaClass.simpleName}")
        } catch (error: GeneralSecurityException) {
            AppLog.w(ACTIVITY_STATISTICS_TAG, "key unavailable: ${error.javaClass.simpleName}")
        } catch (error: ProviderException) {
            AppLog.w(ACTIVITY_STATISTICS_TAG, "keystore unavailable: ${error.javaClass.simpleName}")
        }
        failures = (failures + 1).coerceAtMost(MAX_FAILURE_EXPONENT)
        val wait = (1L shl (failures - 1)).minutes.coerceAtMost(MAX_RETRY)
        retryAt = SystemClock.elapsedRealtime() + wait.inWholeMilliseconds
        return wait
    }
}

internal const val ACTIVITY_STATISTICS_TAG = "ActivityStatistics"
private val POLL_INTERVAL = 1.minutes
private val MAX_RETRY = 1.hours
private const val MAX_FAILURE_EXPONENT = 7
