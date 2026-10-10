package name.alexwayfer.customtv.ui.watch

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imeAnimationTarget
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import name.alexwayfer.customtv.chat.KeyboardHeightMemory
import name.alexwayfer.customtv.chat.KeyboardHeights
import name.alexwayfer.customtv.chat.settledKeyboardHeightPx
import name.alexwayfer.customtv.data.KeyboardHeightStore
import name.alexwayfer.customtv.diagnostics.AppLog

/** The keyboard heights the emote picker takes, kept per orientation and saved across restarts. */
internal class RememberedKeyboardHeight(private val store: KeyboardHeightStore) {
    val memory = KeyboardHeightMemory()
    var saved by mutableStateOf<KeyboardHeights?>(null)

    /** Call before the picker opens: heights of a keyboard the user has since switched away from would size it wrong. */
    fun useCurrentKeyboard() {
        val id = store.currentKeyboardId()
        if (memory.useKeyboard(id)) AppLog.i(KEYBOARD_HEIGHT_TAG, "Keyboard is $id, heights dropped")
    }

    suspend fun restore() {
        useCurrentKeyboard()
        val loaded = store.load()
        memory.restore(loaded)
        AppLog.i(KEYBOARD_HEIGHT_TAG, "Loaded $loaded, using ${memory.heights}")
        saved = loaded
    }

    suspend fun keyboardSettled(settledPx: Int, landscape: Boolean) {
        val previous = saved ?: return
        memory.keyboardShown(store.currentKeyboardId(), landscape)
        val heights = memory.heights
        if (heights == previous) return
        AppLog.i(KEYBOARD_HEIGHT_TAG, "Saving $heights, landscape=$landscape, settled=$settledPx")
        store.save(heights)
        saved = heights
    }
}

@Composable
internal fun rememberKeyboardHeight(): RememberedKeyboardHeight {
    val context = LocalContext.current
    val keyboardHeight = remember { RememberedKeyboardHeight(KeyboardHeightStore(context)) }
    LaunchedEffect(keyboardHeight) { keyboardHeight.restore() }
    return keyboardHeight
}

/** The keyboard's height in the window's orientation, 0 before it was seen; each settled keyboard is saved. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun RememberedKeyboardHeight.heightPx(): Int {
    val density = LocalDensity.current
    val windowSize = LocalWindowInfo.current.containerSize
    val landscape = windowSize.width > windowSize.height
    val imePx = WindowInsets.ime.getBottom(density)
    val imeTargetPx = WindowInsets.imeAnimationTarget.getBottom(density)
    val heightPx = memory.heightPx(imePx, imeTargetPx, landscape)
    val settledPx = settledKeyboardHeightPx(imePx, imeTargetPx, rememberedPx = 0)
    // A new keyboard or a resized one replaces the saved height.
    LaunchedEffect(settledPx, landscape, saved != null) {
        if (settledPx > 0) keyboardSettled(settledPx, landscape)
    }
    return heightPx
}

private const val KEYBOARD_HEIGHT_TAG = "KeyboardHeight"
