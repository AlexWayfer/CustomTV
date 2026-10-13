package name.alexwayfer.customtv.data

import androidx.annotation.StringRes
import java.io.IOException
import name.alexwayfer.customtv.ui.UiText

/** A Gist answer the user can fix, such as a wrong token; [text] is what the screen shows. */
internal class ChatterLabelsException(@StringRes id: Int, vararg args: Any) : IOException() {
    val text = UiText.Resource(id, args.toList())
}

/** What a failed Chatter Labels call shows: the app's own reason, or the library's message when it has one. */
internal fun Exception.chatterLabelsErrorText(fallback: UiText): UiText =
    (this as? ChatterLabelsException)?.text ?: message?.let { UiText.Raw(it) } ?: fallback
