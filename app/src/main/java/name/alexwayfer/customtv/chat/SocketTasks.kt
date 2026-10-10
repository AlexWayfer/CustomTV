package name.alexwayfer.customtv.chat

internal fun socketTaskAllowed(closedByUser: Boolean, executorShutdown: Boolean): Boolean {
    return !closedByUser && !executorShutdown
}
