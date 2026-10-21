package name.alexwayfer.customtv.diagnostics

/** Where Send by email takes the report, by the apps that answer `mailto:`. */
internal sealed interface ReportEmailShare {
    /** No email app: the share sheet, as Another way opens it. */
    data object ShareSheet : ReportEmailShare

    data class Direct(val packageName: String) : ReportEmailShare

    /** A choice among the email apps only, the first one as the chooser's target. */
    data class Choose(val packageNames: List<String>) : ReportEmailShare
}

/** [handlers] are the packages of the activities that answer `mailto:`; an app with several counts once. */
internal fun reportEmailShare(handlers: List<String>): ReportEmailShare {
    val packages = handlers.distinct()
    return when (packages.size) {
        0 -> ReportEmailShare.ShareSheet
        1 -> ReportEmailShare.Direct(packages.single())
        else -> ReportEmailShare.Choose(packages)
    }
}
