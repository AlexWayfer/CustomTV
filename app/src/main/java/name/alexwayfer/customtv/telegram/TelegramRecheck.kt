package name.alexwayfer.customtv.telegram

import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

internal val TELEGRAM_RECHECK_INTERVAL = 30.minutes

/**
 * Whether a return to the app or the daily worker checks the group again. [sinceOpenCheck] is the time since
 * the last check in this process that found the group open; null when there was none, or the last check was
 * refused, failed, or timed out, so those always check again.
 */
internal fun telegramRecheckDue(sinceOpenCheck: Duration?): Boolean =
    sinceOpenCheck == null || sinceOpenCheck >= TELEGRAM_RECHECK_INTERVAL
