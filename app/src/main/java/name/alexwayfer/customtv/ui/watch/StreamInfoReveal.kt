package name.alexwayfer.customtv.ui.watch

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

internal data class StreamInfoSnapshot(
    val login: String,
    val title: String?,
    val categoryName: String?,
)

/**
 * A title or category that changes on the same channel expands the info panel. The first load
 * (nothing known yet), a value that goes away, and a switch to another channel do not.
 */
internal fun streamInfoChangeRevealsHeader(previous: StreamInfoSnapshot, current: StreamInfoSnapshot): Boolean {
    return previous.login.equals(current.login, ignoreCase = true) && (
        changedBetweenKnownValues(previous.title, current.title) ||
            changedBetweenKnownValues(previous.categoryName, current.categoryName)
    )
}

private fun changedBetweenKnownValues(previous: String?, current: String?): Boolean =
    !previous.isNullOrBlank() && !current.isNullOrBlank() && previous != current

@Composable
internal fun RevealHeaderOnStreamInfoChange(
    snapshot: StreamInfoSnapshot,
    chromeVisibility: PlayerChromeVisibility,
) {
    var previous by remember { mutableStateOf(snapshot) }
    LaunchedEffect(snapshot) {
        if (streamInfoChangeRevealsHeader(previous, snapshot)) chromeVisibility.revealHeader()
        previous = snapshot
    }
}
