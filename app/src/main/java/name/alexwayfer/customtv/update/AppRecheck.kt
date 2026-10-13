package name.alexwayfer.customtv.update

import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

internal val APP_RECHECK_INTERVAL = 30.minutes

/**
 * Whether a return to the app or the daily worker checks for an update again. [sinceOpenCheck] is the time since
 * the last check in this process that read the releases; null when there was none, or the last check was
 * refused, failed, or timed out, so those always check again.
 */
internal fun appRecheckDue(sinceOpenCheck: Duration?): Boolean =
    sinceOpenCheck == null || sinceOpenCheck >= APP_RECHECK_INTERVAL
