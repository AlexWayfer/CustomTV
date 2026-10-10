package name.alexwayfer.customtv.ui.account

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
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
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownTypography
import com.mikepenz.markdown.model.MarkdownTypography
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.data.ChannelPanel
import name.alexwayfer.customtv.data.ChannelPanels
import name.alexwayfer.customtv.data.ChannelPanelsLoad
import name.alexwayfer.customtv.data.ChannelPanelsRepository

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
            is ChannelPanelsLoad.Loaded -> AboutTab.Shown(load.panels)
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
    Column(
        modifier = Modifier.padding(top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        panels.panels.forEach { panel -> AboutPanel(panel) }
    }
    if (panels.extensionCount > 0) {
        AboutNote(stringResource(R.string.profile_about_extensions))
        OpenOnTwitchButton(onOpenOnTwitch)
    }
}

@Composable
private fun AboutPanel(panel: ChannelPanel) {
    val context = LocalContext.current
    // An image grows the panel once it arrives.
    Column(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
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
                model = image,
                contentDescription = if (link != null) stringResource(R.string.profile_panel_link) else null,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .then(
                        if (link != null) {
                            Modifier.clickable(role = Role.Button) { openProfileLink(context, link) }
                        } else {
                            Modifier
                        },
                    ),
                contentScale = ContentScale.FillWidth,
            )
        }
        panel.description?.let { text -> Markdown(text, typography = panelTypography()) }
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
        textLink = TextLinkStyles(SpanStyle(color = MaterialTheme.colorScheme.primary)),
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
