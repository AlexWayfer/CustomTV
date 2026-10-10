package name.alexwayfer.customtv.update

/** What a tap on Install leads to. */
internal enum class AppInstallStep {
    /** The system settings page that allows this app to install updates. */
    AllowInstalls,

    /** The hint on what to tap when Play Protect blocks the update. */
    PlayProtectHint,
}

/** Install asks for the permission first; once Android allows installs, it explains Play Protect. */
internal fun appInstallStep(canInstall: Boolean): AppInstallStep =
    if (canInstall) AppInstallStep.PlayProtectHint else AppInstallStep.AllowInstalls

/** Back from the settings page: the hint shows only when the user allowed installs there. */
internal fun showsHintAfterSettings(canInstall: Boolean): Boolean =
    appInstallStep(canInstall) == AppInstallStep.PlayProtectHint
