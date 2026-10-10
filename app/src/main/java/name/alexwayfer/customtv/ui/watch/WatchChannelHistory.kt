package name.alexwayfer.customtv.ui.watch

internal const val WATCH_CHANNEL_ROUTE = "watch/{channel}"

internal fun watchBackReturnsToPreviousChannel(previousRoute: String?): Boolean =
    previousRoute == WATCH_CHANNEL_ROUTE
