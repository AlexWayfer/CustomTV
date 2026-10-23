package name.alexwayfer.customtv.ui.watch

import android.app.Application
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import name.alexwayfer.customtv.chat.EmoteEffects
import name.alexwayfer.customtv.chat.PickerEmote
import name.alexwayfer.customtv.ui.ComposeHost
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** A wide picture, twice as wide as tall, under a URL no load ever decodes. */
private val Wide = PickerEmote(name = "Wide", url = "file:///missing/wide.webp", aspectRatio = 2f)

@RunWith(RobolectricTestRunner::class)
// The platform's own text layout, which spaces a line's first letter differently from the others.
@GraphicsMode(GraphicsMode.Mode.NATIVE)
// The plain application: the app's own starts its stores and workers, which a field does not need.
@Config(application = Application::class)
class ChatDraftEmotePictureRenderTest {
    @get:Rule(order = 0)
    val host = ComposeHost()

    @get:Rule(order = 1)
    val compose = createComposeRule()

    private val output = ChatDraftEmoteOutput(
        isEmoteCode = { it == Wide.name },
        pictureFor = { if (it == Wide.name) Wide else null },
        modifierFor = { null },
    )

    /**
     * Lays [draft] out in a single-line field as the chat field does, and returns where the picture's room ends, as
     * the field draws the picture, and where the word after it starts.
     */
    private fun pictureEndAndNextWord(draft: String, pictureIndex: Int, nextWordIndex: Int): Pair<Float, Float> {
        var layout: TextLayoutResult? = null
        var metrics: ChatDraftEmoteMetrics? = null
        val style = TextStyle(fontSize = 16.sp)
        compose.setContent {
            val measured = rememberChatDraftEmoteMetrics(style)
            SideEffect {
                metrics = measured
                output.metrics = measured
            }
            BasicTextField(
                state = TextFieldState(draft),
                outputTransformation = output,
                lineLimits = TextFieldLineLimits.SingleLine,
                textStyle = style,
                onTextLayout = { result -> layout = result() },
                modifier = Modifier.width(400.dp),
            )
        }
        compose.waitForIdle()
        val shown = layout!!
        val pictureStart = shown.getHorizontalPosition(pictureIndex, usePrimaryDirection = true)
        val pictureEnd = pictureStart + metrics!!.slotWidthPx(Wide, EmoteEffects())
        return pictureEnd to shown.getHorizontalPosition(nextWordIndex, usePrimaryDirection = true)
    }

    @Test
    fun `a picture that starts the field keeps clear of the word after it`() {
        // Shown as "￼ pe": the picture, a space, and the word.
        val (pictureEnd, nextWord) = pictureEndAndNextWord("Wide pe", pictureIndex = 0, nextWordIndex = 2)

        assertTrue("picture ends at $pictureEnd, the next word starts at $nextWord", nextWord >= pictureEnd)
    }

    @Test
    fun `a picture after a word keeps clear of the word after it`() {
        // Shown as "a ￼ pe".
        val (pictureEnd, nextWord) = pictureEndAndNextWord("a Wide pe", pictureIndex = 2, nextWordIndex = 4)

        assertTrue("picture ends at $pictureEnd, the next word starts at $nextWord", nextWord >= pictureEnd)
    }
}
