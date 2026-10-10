package name.alexwayfer.customtv.ui.account

internal fun loginClickStarts(loggingIn: Boolean): Boolean = !loggingIn

internal fun playerExpandsAfterLoginCancelled(openedFromStream: Boolean, watching: Boolean): Boolean =
    openedFromStream && watching
