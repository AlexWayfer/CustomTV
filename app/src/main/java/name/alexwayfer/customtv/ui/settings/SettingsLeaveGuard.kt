package name.alexwayfer.customtv.ui.settings

/** Lets a subpage with unsaved changes ask before Settings leaves it. */
internal class SettingsLeaveGuard {
    /** Set while the page has unsaved changes; it gets the leave to run once the user agrees. */
    var onLeave: ((leave: () -> Unit) -> Unit)? = null

    /** Runs [leave] now, or hands it to the page that asks first. */
    fun leave(leave: () -> Unit) {
        val ask = onLeave
        if (ask == null) leave() else ask(leave)
    }
}
