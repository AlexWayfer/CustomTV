package name.alexwayfer.customtv.ui.watch

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imeAnimationTarget
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.milliseconds

internal fun chatMenuOpensNow(imeBottomPx: Int): Boolean = imeBottomPx <= 0

internal fun inputFocusClearsWhenImeStartsHiding(
    imeBottomPx: Int,
    imeTargetBottomPx: Int,
    keepFocus: Boolean = false,
): Boolean = imeBottomPx > 0 && imeTargetBottomPx == 0 && !keepFocus

/**
 * The keyboard that replaces a closed emote picker has finished rising, so the picker's reserved
 * space can go. Before the rise starts, the target is still 0.
 */
internal fun keyboardReturnFinished(imeBottomPx: Int, imeTargetBottomPx: Int): Boolean =
    imeTargetBottomPx in 1..imeBottomPx

/**
 * Whether the keyboard or emote picker holds the bottom of the screen. Follows where the keyboard is
 * heading, so what gives way to it returns as soon as it starts to close, not after. A switch between
 * the picker and the keyboard stays open throughout.
 */
internal fun chatComposerOpen(imeTargetBottomPx: Int, pickerOpen: Boolean, keyboardReturning: Boolean): Boolean =
    imeTargetBottomPx > 0 || pickerOpen || keyboardReturning

/** Set before the emote picker hides the keyboard, so that hide does not clear the message field. */
internal class ImeDismissFocus {
    var keepFocus: Boolean = false
}

internal val LocalImeDismissFocus = compositionLocalOf { ImeDismissFocus() }

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ClearFocusWhenImeDismissed() {
    val focusManager = LocalFocusManager.current
    val dismissal = LocalImeDismissFocus.current
    val density = LocalDensity.current
    val imeBottom = WindowInsets.ime.getBottom(density)
    val imeTarget = WindowInsets.imeAnimationTarget.getBottom(density)
    LaunchedEffect(imeBottom, imeTarget) {
        if (inputFocusClearsWhenImeStartsHiding(imeBottom, imeTarget, dismissal.keepFocus)) {
            focusManager.clearFocus(force = true)
        }
        if (imeBottom == 0) dismissal.keepFocus = false
    }
}

internal fun loggedOutChatFieldOpensLogin(canSend: Boolean): Boolean = !canSend

internal class SheetKeyboard(
    val open: (() -> Unit) -> Unit,
    val onDismiss: () -> Unit,
)

internal val LocalSheetKeyboard = compositionLocalOf {
    SheetKeyboard(open = { it() }, onDismiss = {})
}

@Composable
internal fun rememberSheetKeyboard(): SheetKeyboard {
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val view = LocalView.current
    val density = LocalDensity.current
    // Read only when a menu opens, so the caller does not recompose on every IME frame.
    val ime = WindowInsets.ime
    var opening by remember { mutableStateOf(false) }
    // One instance while its inputs stay, so callbacks that capture it stay equal and chat rows skip.
    return remember(scope, focusManager, keyboard, view, density, ime) {
        SheetKeyboard(
            open = open@{ action ->
                if (opening) return@open
                opening = true
                scope.launch {
                    try {
                        focusManager.clearFocus(force = true)
                        keyboard?.hide()
                        if (!chatMenuOpensNow(ime.getBottom(density))) {
                            withTimeoutOrNull(800.milliseconds) {
                                snapshotFlow { ime.getBottom(density) }.first { chatMenuOpensNow(it) }
                            }
                        }
                        action()
                    } finally {
                        opening = false
                    }
                }
            },
            onDismiss = {
                view.post {
                    focusManager.clearFocus(force = true)
                    keyboard?.hide()
                }
            },
        )
    }
}

internal data class ChatFieldSideButtons(val start: Boolean, val endButton: Boolean)

/**
 * Which buttons beside the message field show. While the keyboard or emote picker is up, the start
 * button (channel points) and the chat menu give their room to the field; the end button comes back as Send once there
 * is a message, and stays while it sends. Beside a full screen player the narrow column turns this around: the closed
 * field shows alone, and the buttons come once the keyboard opens and the chat widens.
 */
internal fun chatFieldSideButtons(
    composerOpen: Boolean,
    hasMessage: Boolean,
    sending: Boolean,
    fullscreen: Boolean,
) = if (fullscreen) {
    ChatFieldSideButtons(start = composerOpen, endButton = composerOpen || sending)
} else {
    ChatFieldSideButtons(start = !composerOpen, endButton = !composerOpen || hasMessage || sending)
}
