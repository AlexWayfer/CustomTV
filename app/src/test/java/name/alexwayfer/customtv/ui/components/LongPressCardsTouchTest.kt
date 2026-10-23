package name.alexwayfer.customtv.ui.components

import android.app.Application
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import name.alexwayfer.customtv.data.ChannelProfile
import name.alexwayfer.customtv.data.TwitchCategory
import name.alexwayfer.customtv.ui.ComposeHost
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private const val CARD = "card"

// Pictures that fail without the network, so nothing loads.
private const val NO_PICTURE = "file:///nonexistent.png"

/** A stream's card and a category's tile: a tap opens, a long press opens the menu when there is one. */
@RunWith(RobolectricTestRunner::class)
// The plain application: the app's own starts its stores and workers, which a card does not need.
@Config(application = Application::class)
class LongPressCardsTouchTest {
    @get:Rule(order = 0)
    val host = ComposeHost()

    @get:Rule(order = 1)
    val compose = createComposeRule()

    private var opened = 0
    private var longPressed = 0
    private var vibrations = 0
    private val haptics = object : HapticFeedback {
        override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
            vibrations++
        }
    }

    private fun showStreamCard(withLongPress: Boolean) {
        compose.setContent {
            CompositionLocalProvider(LocalHapticFeedback provides haptics) { StreamCard(withLongPress) }
        }
    }

    @Composable
    private fun StreamCard(withLongPress: Boolean) {
        LiveStreamCard(
            profile = ChannelProfile(
                login = "streamer",
                displayName = "Streamer",
                avatarUrl = null,
                streamTitle = "Hi",
                categoryName = "Dota 2",
                isLive = true,
            ),
            previewUrl = NO_PICTURE,
            onClick = { opened++ },
            modifier = Modifier.width(320.dp).testTag(CARD),
            onLongClick = if (withLongPress) ({ longPressed++ }) else null,
            onLongClickLabel = if (withLongPress) "Hide" else null,
        )
    }

    private fun showCategoryTile(withLongPress: Boolean) {
        compose.setContent {
            CompositionLocalProvider(LocalHapticFeedback provides haptics) { Tile(withLongPress) }
        }
    }

    @Composable
    private fun Tile(withLongPress: Boolean) {
        CategoryTile(
            category = TwitchCategory(name = "Dota 2", boxArtUrl = NO_PICTURE, id = "29595"),
            modifier = Modifier.width(120.dp).testTag(CARD),
            onClick = { opened++ },
            onLongClick = if (withLongPress) ({ longPressed++ }) else null,
            onLongClickLabel = if (withLongPress) "Hide" else null,
        )
    }

    private fun tap() = compose.onNodeWithTag(CARD).performClick()

    private fun longPress() = compose.onNodeWithTag(CARD).performTouchInput { longClick() }

    @Test
    fun aTapOnAStreamCardOpensTheStream() {
        showStreamCard(withLongPress = true)
        tap()

        assertEquals(1, opened)
        assertEquals(0, longPressed)
    }

    @Test
    fun aLongPressOnAStreamCardOpensTheMenu() {
        showStreamCard(withLongPress = true)
        longPress()

        assertEquals(1, longPressed)
        assertEquals(0, opened)
    }

    @Test
    fun aLongPressOnAStreamCardVibratesOnce() {
        showStreamCard(withLongPress = true)
        longPress()

        assertEquals(1, vibrations)
    }

    @Test
    fun aLongPressOnACategoryTileVibratesOnce() {
        showCategoryTile(withLongPress = true)
        longPress()

        assertEquals(1, vibrations)
    }

    @Test
    fun withoutAMenuALongPressOnAStreamCardOpensTheStreamAsATapDid() {
        showStreamCard(withLongPress = false)
        longPress()

        assertEquals(1, opened)
        assertEquals(0, longPressed)
    }

    @Test
    fun aTapOnACategoryTileOpensTheCategory() {
        showCategoryTile(withLongPress = true)
        tap()

        assertEquals(1, opened)
        assertEquals(0, longPressed)
    }

    @Test
    fun aLongPressOnACategoryTileOpensTheMenu() {
        showCategoryTile(withLongPress = true)
        longPress()

        assertEquals(1, longPressed)
        assertEquals(0, opened)
    }

    @Test
    fun withoutAMenuALongPressOnACategoryTileOpensTheCategoryAsATapDid() {
        showCategoryTile(withLongPress = false)
        longPress()

        assertEquals(1, opened)
        assertEquals(0, longPressed)
    }
}
