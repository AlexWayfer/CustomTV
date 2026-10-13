package name.alexwayfer.customtv.update

import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.ui.UiText

/** What the updates section shows for the last check of the edition's releases. */
internal sealed interface AppUpdateStatus {
    data object Checking : AppUpdateStatus

    /**
     * [update] is the newest release when it is newer than this build; [latest] is the newest version read,
     * newer or not, or null when no release was read.
     */
    data class Read(val update: AppRelease?, val latest: AppVersion?) : AppUpdateStatus

    data class Failed(val message: UiText) : AppUpdateStatus
}

/** The status of a check without an update: it names the newest version it read, so a working check shows. */
internal fun appNoUpdateText(latest: AppVersion?): UiText = if (latest == null) {
    UiText.Resource(R.string.app_update_none)
} else {
    UiText.Resource(R.string.app_update_none_latest, listOf(latest.toString()))
}

internal sealed interface AppUpdateNotice {
    data class Show(val version: AppVersion) : AppUpdateNotice
    data object Clear : AppUpdateNotice
    data object Keep : AppUpdateNotice
}

/**
 * What a check that read the releases does with the update notification: only a check that found no newer
 * release takes a shown notice away, and only a check that may post one ([notify]) shows it.
 */
internal fun appUpdateNotice(update: AppRelease?, notify: Boolean): AppUpdateNotice = when {
    update == null -> AppUpdateNotice.Clear
    notify -> AppUpdateNotice.Show(update.version)
    else -> AppUpdateNotice.Keep
}
