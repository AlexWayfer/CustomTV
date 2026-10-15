package name.alexwayfer.customtv.ui.notifications

/** Home offers notifications once: until the card is answered, and only while they are off. */
internal fun homeNotificationPromptVisible(notificationsEnabled: Boolean, dismissed: Boolean?): Boolean =
    !notificationsEnabled && dismissed == false

/** Notifications turned on make the answer on Home stale: once they are off again, Home offers them anew. */
internal fun notificationPromptAnswerResets(notificationsEnabled: Boolean, dismissed: Boolean): Boolean =
    notificationsEnabled && dismissed

internal enum class NotificationPromptAction { RequestPermission, OpenSettings }

/**
 * Android shows its permission dialog twice at most, so only Home's first ask and a retry after a single
 * refusal use it. Otherwise, or when the permission is granted but notifications are off, the system
 * settings of the app open instead.
 */
internal fun notificationPromptAction(
    permissionNeeded: Boolean,
    firstAsk: Boolean,
    showRationale: Boolean,
): NotificationPromptAction =
    if (permissionNeeded && (firstAsk || showRationale)) {
        NotificationPromptAction.RequestPermission
    } else {
        NotificationPromptAction.OpenSettings
    }

internal enum class NotificationPermissionRefusal { Refused, Blocked }

/**
 * A refused first ask leaves Android ready to ask once more; when it is not, its dialog never showed,
 * because notifications were turned off in the system before, and only the system settings can turn
 * them on. A later ask is always a refusal the user made.
 */
internal fun notificationPermissionRefusal(firstAsk: Boolean, showRationale: Boolean): NotificationPermissionRefusal =
    if (firstAsk && !showRationale) NotificationPermissionRefusal.Blocked else NotificationPermissionRefusal.Refused
