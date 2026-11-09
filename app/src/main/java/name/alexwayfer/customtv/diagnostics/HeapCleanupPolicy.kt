package name.alexwayfer.customtv.diagnostics

internal fun heapCleanupDue(startupAt: Long?, currentAt: Long?, hasWork: Boolean): Boolean =
    hasWork && startupAt == currentAt
