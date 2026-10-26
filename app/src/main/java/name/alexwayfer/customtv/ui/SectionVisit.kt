package name.alexwayfer.customtv.ui

import androidx.compose.runtime.saveable.Saver

/**
 * The open section, and which visit of the settings their kept state belongs to. Back from a profile the settings
 * opened returns to the same visit, so the settings come back as they were left: the open page, its scroll, and the
 * labels being edited. Any other way into the settings starts a new visit, which starts them afresh.
 */
internal data class SectionVisit(val section: AppSection, val settingsVisit: Int = 0) {
    /** The section turned to [section], from a profile that [profileOpenedFrom] opened when one is open. */
    fun moveTo(section: AppSection, profileOpenedFrom: AppSection?): SectionVisit = SectionVisit(
        section = section,
        settingsVisit = if (settingsVisitContinues(this.section, section, profileOpenedFrom)) {
            settingsVisit
        } else {
            settingsVisit + 1
        },
    )

    companion object {
        val Saver: Saver<SectionVisit, Any> = Saver(
            save = { arrayListOf(it.section, it.settingsVisit) },
            restore = { saved ->
                val (section, visit) = saved as List<*>
                SectionVisit(section as AppSection, visit as Int)
            },
        )
    }
}

private fun settingsVisitContinues(from: AppSection, to: AppSection, profileOpenedFrom: AppSection?): Boolean =
    to != AppSection.Settings ||
        from == AppSection.Settings ||
        (from == AppSection.ChannelProfile && profileOpenedFrom == AppSection.Settings)

/** The key the settings of one visit keep their state under in the app's saveable state holder. */
internal fun settingsStateKey(visit: Int): String = "settings:$visit"
