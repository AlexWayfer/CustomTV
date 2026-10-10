package name.alexwayfer.customtv.ui.watch

/**
 * A request handed down as state for an effect to act on once, such as opening the keyboard or a
 * card. An effect runs again when its composable comes back into composition, after picture-in-picture
 * or the mini player, and would act on the same request a second time; [take] gives it out only once.
 * A new request is a new instance, so the same action can be asked for again.
 */
internal class OneShotRequest<T : Any>(private val value: T) {
    private var taken = false

    fun take(): T? {
        if (taken) return null
        taken = true
        return value
    }
}
