package name.alexwayfer.customtv.ui.watch

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.runtime.Composable
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.awaitCancellation
import name.alexwayfer.customtv.chat.EmoteEffects
import name.alexwayfer.customtv.chat.PickerEmote
import name.alexwayfer.customtv.ui.components.SharedEmoteStore
import name.alexwayfer.customtv.ui.components.drawDrawableKeepAspect
import name.alexwayfer.customtv.ui.components.rememberScreenReaderOn
import name.alexwayfer.customtv.ui.components.resolvedEmoteAspectRatio
import name.alexwayfer.customtv.ui.theme.TwitchPurple
import kotlin.math.abs
import kotlin.math.min

private val EmotePlaqueColor = TwitchPurple.copy(alpha = 0.45f)
private val EmotePlaqueRadius = 4.dp
private val EmotePlaquePadding = 2.dp

// One step of letter spacing is one plaque padding.
private val EmoteGapStep = EmotePlaquePadding.value.sp

private const val EMOTE_PICTURE_TEXT = CHAT_DRAFT_EMOTE_PICTURE_CHAR.toString()

/**
 * The field's text size for emote pictures: how wide the stand-in character is, how tall a picture
 * is (one line), and the font size that letter spacing in `em` scales by, all in px.
 */
internal class ChatDraftEmoteMetrics(val glyphWidthPx: Float, val pictureHeightPx: Float, val fontSizePx: Float) {
    fun pictureWidthPx(picture: PickerEmote): Float =
        pictureHeightPx * resolvedEmoteAspectRatio(picture.url, picture.aspectRatio)

    /** The room the picture takes in the line: wider for a modifier such as `w!`, as chat lays it out. */
    fun slotWidthPx(picture: PickerEmote, effects: EmoteEffects): Float =
        pictureWidthPx(picture) * effects.widthMultiplier
}

/** Measures the stand-in character in the field's [style]; a picture is as tall as its line. */
@Composable
internal fun rememberChatDraftEmoteMetrics(style: TextStyle): ChatDraftEmoteMetrics? {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    return remember(style, measurer, density) {
        if (!style.fontSize.isSp) return@remember null
        val layout = measurer.measure(EMOTE_PICTURE_TEXT, style, maxLines = 1)
        ChatDraftEmoteMetrics(
            glyphWidthPx = layout.getHorizontalPosition(1, usePrimaryDirection = true) -
                layout.getHorizontalPosition(0, usePrimaryDirection = true),
            pictureHeightPx = layout.getLineBottom(0) - layout.getLineTop(0),
            fontSizePx = with(density) { style.fontSize.toPx() },
        )
    }
}

/**
 * Shows the draft's finished emote codes as pictures and widens the spaces around the others with
 * letter spacing. A picture's code is shown as one transparent character as wide as the picture,
 * which the field's cursor never enters; the draft itself keeps the code, so copy, paste, and
 * sending see the real text.
 *
 * Keep one instance per field: a new instance makes the field rebuild its text layout. Read
 * changing emote lists from snapshot state inside [isEmoteCode] and [pictureFor] instead.
 */
internal class ChatDraftEmoteOutput(
    private val isEmoteCode: (String) -> Boolean,
    private val pictureFor: (String) -> PickerEmote?,
    private val modifierFor: (String) -> ChatDraftModifier?,
) : OutputTransformation {
    /**
     * Set by the field once it knows its text style, and cleared while a screen reader is on;
     * without it every code stays text.
     */
    var metrics by mutableStateOf<ChatDraftEmoteMetrics?>(null)

    fun shownEmotes(text: CharSequence): List<ChatDraftShownEmote> =
        chatDraftShownEmotes(text, isEmoteCode, if (metrics == null) { _ -> null } else pictureFor, modifierFor)

    override fun TextFieldBuffer.transformOutput() {
        val emotes = shownEmotes(asCharSequence())
        for (emote in emotes.asReversed()) {
            if (emote.picture != null) replace(emote.source.start, emote.source.end, EMOTE_PICTURE_TEXT)
        }
        // Without metrics no code became a picture.
        val sizes = metrics
        if (sizes != null) {
            for (emote in emotes) {
                val picture = emote.picture ?: continue
                val spacing = chatDraftPictureSpacingEm(
                    slotPx = sizes.slotWidthPx(picture, emote.effects),
                    glyphPx = sizes.glyphWidthPx,
                    fontSizePx = sizes.fontSizePx,
                    // The field holds one line.
                    startsLine = emote.shown.start == 0,
                )
                val style = SpanStyle(color = Color.Transparent, letterSpacing = spacing.em)
                addStyle(style, emote.shown.start, emote.shown.end)
            }
        }
        val text = asCharSequence()
        for ((index, plaques) in chatDraftEmoteGaps(text, plaqueRanges(emotes))) {
            val steps = chatDraftEmoteGapSteps(plaques, lastCharacter = index == text.length - 1)
            addStyle(SpanStyle(letterSpacing = EmoteGapStep * steps), index, index + 1)
        }
    }
}

/**
 * The letter spacing, in `em`, that widens a picture's stand-in character to its [slotPx]. Android lays half of a
 * letter's spacing before it and half after, but drops the half before the first letter of a line, so a picture that
 * [startsLine] takes twice the spacing to get the same room.
 */
internal fun chatDraftPictureSpacingEm(slotPx: Float, glyphPx: Float, fontSizePx: Float, startsLine: Boolean): Float {
    val extra = slotPx - glyphPx
    return (if (startsLine) extra * 2 else extra) / fontSizePx
}

private fun plaqueRanges(emotes: List<ChatDraftShownEmote>): List<TextRange> =
    emotes.filter { it.picture == null }.map { it.shown }

/** The URLs of the pictures the field shows for [draft], in order. */
internal fun ChatDraftEmoteOutput.pictureUrls(draft: CharSequence): List<String> =
    shownEmotes(draft).mapNotNull { it.picture?.url }.distinct()

/** Keeps the pictures in [urls] loaded and playing while the field shows them, by URL. */
@Composable
internal fun rememberDraftEmotePictures(urls: List<String>): Map<String, SharedEmoteStore.Holder> {
    val context = LocalContext.current
    val holders = remember { mutableStateMapOf<String, SharedEmoteStore.Holder>() }
    for (url in urls) {
        key(url) {
            LaunchedEffect(url) {
                val holder = SharedEmoteStore.acquire(url)
                holders[url] = holder
                try {
                    holder.load(context)
                    awaitCancellation()
                } finally {
                    holders.remove(url)
                    SharedEmoteStore.release(url)
                }
            }
        }
    }
    return holders
}

/**
 * The pictures the field's emote marks show, loaded, with their effects ticking. Pictures stay off
 * while a screen reader is on.
 */
@Composable
internal fun rememberChatDraftEmotePictures(
    draft: TextFieldState,
    output: ChatDraftEmoteOutput,
    textStyle: TextStyle,
): DraftEmotePictures {
    val metrics = chatDraftPictureMetrics(rememberChatDraftEmoteMetrics(textStyle), rememberScreenReaderOn())
    SideEffect { output.metrics = metrics }
    val pictureUrls by remember(draft, output) { derivedStateOf { output.pictureUrls(draft.text) } }
    val pictures = rememberDraftEmotePictures(pictureUrls)
    val effectsMove by remember(draft, output) {
        derivedStateOf { output.shownEmotes(draft.text).any { emoteEffectsMove(it.effects) } }
    }
    val effectTime = rememberEmoteEffectTime(effectsMove)
    return DraftEmotePictures(pictures, effectTime = { effectTime.value })
}

/** The pictures the field shows, by URL, and the clock their moving effects read. */
internal class DraftEmotePictures(val holders: Map<String, SharedEmoteStore.Holder>, val effectTime: () -> Long)

/**
 * Draws a rounded plaque behind every emote code the draft shows as text, and the picture of every
 * other one. Put it on the box around the field's inner text, which does not scroll: the
 * single-line text inside moves by [scrollState].
 */
internal fun Modifier.chatDraftEmoteMarks(
    output: ChatDraftEmoteOutput,
    draft: () -> CharSequence,
    textLayout: () -> TextLayoutResult?,
    pictures: DraftEmotePictures,
    scrollState: ScrollState,
): Modifier = drawBehind {
    val layout = textLayout() ?: return@drawBehind
    val source = draft()
    val emotes = output.shownEmotes(source)
    if (emotes.isEmpty()) return@drawBehind
    val text = layout.layoutInput.text
    // The layout trails the draft by a frame after an edit; its offsets would point elsewhere.
    if (text.length != chatDraftShownLength(source.length, emotes)) return@drawBehind
    val gaps = chatDraftEmoteGaps(text, plaqueRanges(emotes))
    val padding = EmotePlaquePadding.toPx()
    clipRect(left = -padding, right = size.width + padding) {
        translate(left = -scrollState.value.toFloat()) {
            for (emote in emotes) {
                val range = emote.shown
                // The layout reports the word's start half the letter spacing of the space in front of
                // it earlier than Android draws the first letter; without this the left side is wider.
                val spacingBefore = EmoteGapStep.toPx() * chatDraftEmoteGapStepsBefore(range, gaps)
                val start = layout.getHorizontalPosition(range.start, usePrimaryDirection = true) +
                    spacingBefore / 2
                val picture = emote.picture
                val metrics = output.metrics
                if (picture == null) {
                    drawEmotePlaque(layout, range, start, padding)
                } else if (metrics != null) {
                    drawEmotePicture(layout, emote, start, metrics, pictures)
                }
            }
        }
    }
}

private fun DrawScope.drawEmotePlaque(layout: TextLayoutResult, range: TextRange, start: Float, padding: Float) {
    val line = layout.getLineForOffset(range.start)
    val end = layout.getHorizontalPosition(range.end, usePrimaryDirection = true)
    val top = layout.getLineTop(line)
    drawRoundRect(
        color = EmotePlaqueColor,
        topLeft = Offset(min(start, end) - padding, top),
        size = Size(abs(end - start) + padding * 2, layout.getLineBottom(line) - top),
        cornerRadius = CornerRadius(EmotePlaqueRadius.toPx()),
    )
}

/** Draws [emote]'s picture over its stand-in from [start], with its modifiers' effects as chat draws them. */
private fun DrawScope.drawEmotePicture(
    layout: TextLayoutResult,
    emote: ChatDraftShownEmote,
    start: Float,
    metrics: ChatDraftEmoteMetrics,
    pictures: DraftEmotePictures,
) {
    val picture = emote.picture ?: return
    val holder = pictures.holders[picture.url] ?: return
    val drawable = holder.drawable ?: return
    // Reading the animation tick here redraws a new frame without recomposing the field.
    if (holder.tick < 0) return
    val effects = emote.effects
    // Only moving effects read the clock, so a still picture does not redraw on its ticks.
    val time = if (emoteEffectsMove(effects)) pictures.effectTime() else 0L
    val line = layout.getLineForOffset(emote.shown.start)
    val lineTop = layout.getLineTop(line)
    val width = metrics.pictureWidthPx(picture)
    val height = metrics.pictureHeightPx
    val top = lineTop + (layout.getLineBottom(line) - lineTop - height) / 2
    // Like chat, the picture keeps its size in the middle of its slot, and the effects stretch it there.
    val left = start + (metrics.slotWidthPx(picture, effects) - width) / 2
    val motion = emoteEffectMotion(effects, time)
    val pivot = Offset(width * motion.origin.pivotFractionX, height * motion.origin.pivotFractionY)
    translate(left = left, top = top) {
        withTransform({
            translate(left = motion.translationX, top = motion.translationY)
            rotate(motion.rotationDegrees, pivot)
            scale(motion.scaleX, motion.scaleY, pivot)
        }) {
            drawDrawableKeepAspect(drawable, emoteEffectColorFilter(effects, time), area = Size(width, height))
        }
    }
}
