package name.alexwayfer.customtv.ui.settings

import name.alexwayfer.customtv.data.AppSettings
import name.alexwayfer.customtv.data.ChatSettings

/** Chat can use the defaults until DataStore emits. A switch must not. */
internal fun AppSettings?.beforeRestore(): AppSettings = this ?: AppSettings()

/**
 * A switch, segmented button, or floating label animates when its value changes
 * after the first frame. Null keeps that control out of composition until storage
 * has emitted, so the first frame is already the saved value.
 */
internal fun <T> animatedControlValue(stored: T?): T? = stored

internal data class SettingsBody(
    val settings: AppSettings,
    val labelSeed: ChatterLabelFieldSeed,
)

/** Gist fields and switches share one first frame, so one section cannot appear ahead of the other. */
internal fun settingsBody(
    settings: AppSettings?,
    labelSeed: ChatterLabelFieldSeed?,
): SettingsBody? {
    if (settings == null || labelSeed == null) return null
    return SettingsBody(settings, labelSeed)
}

/** Chat rows can use the defaults until DataStore emits. The chat settings sheet must not. */
internal fun ChatSettings?.beforeRestore(): ChatSettings = this ?: ChatSettings()
