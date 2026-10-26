package name.alexwayfer.customtv.ui.components

import androidx.compose.runtime.saveable.Saver
import name.alexwayfer.customtv.data.ChannelProfile

/**
 * A channel to open: its login, and its display name and avatar when the opener already shows them, so the page
 * shows those instead of the login and an empty avatar until the channel's profile loads.
 */
internal data class ChannelPreview(
    val login: String,
    val displayName: String? = null,
    val avatarUrl: String? = null,
) {
    companion object {
        val Saver: Saver<ChannelPreview?, Any> = Saver(
            save = { preview -> preview?.let { arrayListOf(it.login, it.displayName, it.avatarUrl) } },
            restore = { saved ->
                val (login, displayName, avatarUrl) = saved as List<*>
                ChannelPreview(login as String, displayName as String?, avatarUrl as String?)
            },
        )
    }
}

/** What a page that opens from [this] profile shows of it before it loads there. */
internal fun ChannelProfile.preview(): ChannelPreview = ChannelPreview(login, displayName, avatarUrl)

/** The profile a page shows until the channel's own loads: what is known of it, and the login for a missing name. */
internal fun ChannelPreview.fallbackProfile(): ChannelProfile = ChannelProfile(
    login = login.lowercase(),
    displayName = displayName?.takeIf { it.isNotBlank() } ?: login,
    avatarUrl = avatarUrl?.takeIf { it.isNotBlank() },
)
