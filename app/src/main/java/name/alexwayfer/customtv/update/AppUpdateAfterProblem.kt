package name.alexwayfer.customtv.update

import kotlin.time.Duration

/** What a problem notice does about updates, since a newer release may already fix the problem. */
internal sealed interface AppUpdateAfterProblem {
    /** Checks again and posts the update notification for a newer release it finds. */
    data object Check : AppUpdateAfterProblem

    /** Posts the update notification again for the release a recent check found. */
    data class Show(val version: AppVersion) : AppUpdateAfterProblem

    data object None : AppUpdateAfterProblem
}

/**
 * [sinceCheck] is the time since the last check that read the releases, as for [appRecheckDue]; [known] is the newer
 * release that check found, or null when it found none.
 */
internal fun appUpdateAfterProblem(sinceCheck: Duration?, known: AppVersion?): AppUpdateAfterProblem = when {
    appRecheckDue(sinceCheck) -> AppUpdateAfterProblem.Check
    known != null -> AppUpdateAfterProblem.Show(known)
    else -> AppUpdateAfterProblem.None
}
