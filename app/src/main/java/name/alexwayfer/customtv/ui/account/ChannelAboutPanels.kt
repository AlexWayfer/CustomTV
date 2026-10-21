package name.alexwayfer.customtv.ui.account

import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Size
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownTypography
import com.mikepenz.markdown.model.MarkdownTypography
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.data.ChannelPanel
import name.alexwayfer.customtv.data.ChannelPanels
import name.alexwayfer.customtv.data.ChannelPanelsLoad
import name.alexwayfer.customtv.data.ChannelPanelsRepository
import name.alexwayfer.customtv.ui.components.OutlinedButton
import name.alexwayfer.customtv.ui.theme.TwitchPurpleText

/** The About tab's state: loading, the panels, or why there are none to show. */
private sealed interface AboutTab {
    data object Loading : AboutTab
    data class Shown(val panels: ChannelPanels) : AboutTab
    data object LeftOut : AboutTab
    data object Failed : AboutTab
}

/**
 * The channel's About panels, loaded when the tab opens. When Twitch leaves them out or the load fails, the tab
 * offers the channel's About page on Twitch instead, and a failed load can be retried.
 */
@Composable
internal fun ChannelAboutPanels(userId: String?, onOpenOnTwitch: () -> Unit) {
    var attempt by rememberSaveable(userId) { mutableIntStateOf(0) }
    val state by produceState(
        initialValue = userId?.let(ChannelPanelsRepository::cached)?.let { AboutTab.Shown(it) } ?: AboutTab.Loading,
        userId,
        attempt,
    ) {
        // The id comes with the profile; until then the tab waits.
        val id = userId ?: return@produceState
        if (value is AboutTab.Shown && attempt == 0) return@produceState
        value = AboutTab.Loading
        value = when (val load = ChannelPanelsRepository.load(id)) {
            is ChannelPanelsLoad.Loaded -> {
                PanelMarkdown.parse(load.panels)
                AboutTab.Shown(load.panels)
            }
            ChannelPanelsLoad.LeftOut -> AboutTab.LeftOut
            ChannelPanelsLoad.Failed -> AboutTab.Failed
        }
    }
    // The spinner fades into the panels, or into the note on why there are none.
    AnimatedContent(
        targetState = state,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        contentKey = { it.javaClass },
        label = "aboutPanels",
    ) { shown ->
        Column(modifier = Modifier.fillMaxWidth()) {
            when (shown) {
                AboutTab.Loading -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 32.dp).size(32.dp),
                )
                is AboutTab.Shown -> PanelList(shown.panels, onOpenOnTwitch)
                AboutTab.LeftOut -> {
                    AboutNote(stringResource(R.string.profile_about_unavailable))
                    OpenOnTwitchButton(onOpenOnTwitch)
                }
                AboutTab.Failed -> {
                    Text(
                        text = stringResource(R.string.profile_about_failed),
                        modifier = Modifier.padding(top = 20.dp),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Row(
                        modifier = Modifier.padding(top = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Button(onClick = { attempt++ }) { Text(stringResource(R.string.retry)) }
                        OutlinedButton(onClick = onOpenOnTwitch) { Text(stringResource(R.string.profile_open_about)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun PanelList(panels: ChannelPanels, onOpenOnTwitch: () -> Unit) {
    if (panels.panels.isEmpty() && panels.extensionCount == 0) {
        AboutNote(stringResource(R.string.profile_about_empty))
        return
    }
    val context = LocalContext.current
    Column(
        modifier = Modifier.padding(top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        panels.panels.forEach { panel -> AboutPanel(panel) { link -> openProfileLink(context, link) } }
    }
    if (panels.extensionCount > 0) {
        AboutNote(stringResource(R.string.profile_about_extensions))
        OpenOnTwitchButton(onOpenOnTwitch)
    }
}

/**
 * The image of a panel as [AboutPanel] loads it, so a request made ahead of time leaves the image in memory for the
 * panel's first frame.
 */
internal fun panelImageRequest(context: Context, imageUrl: String): ImageRequest =
    ImageRequest.Builder(context).data(imageUrl).size(Size.ORIGINAL).build()

/** One About panel; [onOpenLink] opens the link of its image, and its text's links go to the `LocalUriHandler`. */
@Composable
internal fun AboutPanel(panel: ChannelPanel, onOpenLink: (String) -> Unit) {
    val context = LocalContext.current
    // The text is parsed and an image kept in memory is drawn in the first frame, so the panel opens at its size.
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        panel.title?.let { title ->
            Text(
                text = title,
                modifier = Modifier.semantics { heading() },
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleLarge,
            )
        }
        panel.imageUrl?.let { image ->
            val link = panel.linkUrl
            AsyncImage(
                // A fixed size lets Coil take the image from memory at once instead of waiting for the layout.
                model = remember(image) { panelImageRequest(context, image) },
                contentDescription = if (link != null) stringResource(R.string.profile_panel_link) else null,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .then(
                        if (link != null) {
                            Modifier.clickable(role = Role.Button) { onOpenLink(link) }
                        } else {
                            Modifier
                        },
                    ),
                contentScale = ContentScale.FillWidth,
            )
        }
        panel.description?.let { text ->
            Markdown(state = remember(text) { PanelMarkdown.parsed(text) }, typography = panelTypography())
        }
    }
}

/**
 * Panel text sized as Twitch shows it: every heading level a little above the text, and links only colored, so a
 * link in a heading keeps the heading's size.
 */
@Composable
private fun panelTypography(): MarkdownTypography {
    val typography = MaterialTheme.typography
    val heading = typography.titleMedium.copy(fontWeight = FontWeight.Bold)
    val text = typography.bodyMedium
    return markdownTypography(
        h1 = heading,
        h2 = heading,
        h3 = heading,
        h4 = heading,
        h5 = heading,
        h6 = heading,
        text = text,
        quote = text,
        paragraph = text,
        ordered = text,
        bullet = text,
        list = text,
        textLink = TextLinkStyles(SpanStyle(color = TwitchPurpleText)),
    )
}

@Composable
private fun AboutNote(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(top = 16.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyLarge,
    )
}

@Composable
private fun OpenOnTwitchButton(onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.padding(top = 12.dp)) {
        Text(stringResource(R.string.profile_open_about))
    }
}
