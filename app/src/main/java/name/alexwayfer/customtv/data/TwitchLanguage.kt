package name.alexwayfer.customtv.data

import android.content.Context
import name.alexwayfer.customtv.R

private const val DEFAULT_TWITCH_LANGUAGE = "en-US"

/**
 * The language Twitch's GQL answers in, such as category names and tags: the one the app shows, read from its own
 * resources at each request, so it follows a change of the app's language at once.
 */
internal object TwitchLanguage {
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    /** The app's language tag, such as `ru-RU`; English before [init], as in a unit test. */
    fun current(): String =
        appContext?.getString(R.string.twitch_language)?.trim()?.ifEmpty { null } ?: DEFAULT_TWITCH_LANGUAGE
}
