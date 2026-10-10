package name.alexwayfer.customtv.ui.settings

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.semantics.Role
import name.alexwayfer.customtv.R
import kotlin.math.roundToInt

/** A page of the settings: the main list, or a subpage it opens. */
internal enum class SettingsPage {
    Main,
    Chat,
    Mentions,
    Notifications,
    Playback,
    ChatterPortraits,
    ChatterLabels,
    Experimental,
    Gestures,
}

/**
 * The page that holds the section another screen opens Settings at. Only the main list needs a scroll to it: a
 * subpage is that whole section and opens at its top.
 */
internal fun settingsPageFor(target: SettingsScrollTarget): SettingsPage = when (target) {
    SettingsScrollTarget.Updates -> SettingsPage.Main
    SettingsScrollTarget.Notifications -> SettingsPage.Notifications
    SettingsScrollTarget.ChatterPortraits -> SettingsPage.ChatterPortraits
}

@StringRes
internal fun SettingsPage.titleRes(): Int = when (this) {
    SettingsPage.Main -> R.string.settings
    SettingsPage.Chat -> R.string.settings_page_chat
    SettingsPage.Mentions -> R.string.settings_page_mentions
    SettingsPage.Notifications -> NotificationSettingsTitle
    SettingsPage.Playback -> R.string.settings_section_playback
    SettingsPage.ChatterPortraits -> ChatterPortraitSettingsTitle
    SettingsPage.ChatterLabels -> ChatterLabelsSettingsTitle
    SettingsPage.Experimental -> ExperimentalSettingsTitle
    SettingsPage.Gestures -> R.string.gestures
}

/** A subpage slides in from the end and the list moves aside; going back reverses it. */
internal fun AnimatedContentTransitionScope<SettingsPage>.settingsPageTransition(): ContentTransform {
    val forward = targetState != SettingsPage.Main
    val direction = if (forward) 1 else -1
    return (slideInHorizontally { direction * it / 4 } + fadeIn()) togetherWith
        (slideOutHorizontally { -direction * it / 4 } + fadeOut())
}

/** A row in the main settings list that opens a subpage, with an outlined [icon] for the page. */
@Composable
internal fun SettingsPageEntry(icon: Painter, title: String, hint: String, onOpen: () -> Unit) {
    ListItem(
        leadingContent = { Icon(icon, contentDescription = null) },
        headlineContent = { Text(title) },
        supportingContent = { Text(hint) },
        trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onOpen),
    )
}

/** Where on its page the block another screen opens Settings at is placed. */
internal class SettingsScrollAnchor {
    var top by mutableStateOf<Int?>(null)
}

/** Marks the block a page scrolls to when Settings opens at it. */
internal fun Modifier.settingsScrollAnchor(anchor: SettingsScrollAnchor): Modifier =
    onPlaced { anchor.top = it.positionInParent().y.roundToInt() }

/**
 * A scrolling settings page. When Settings opened at [scrollTo], it scrolls to the block marked with the anchor once that
 * block is placed, then reports [onScrolled].
 */
@Composable
internal fun SettingsPageColumn(
    modifier: Modifier = Modifier,
    scrollTo: SettingsScrollTarget? = null,
    onScrolled: () -> Unit = {},
    scroll: ScrollState = rememberScrollState(),
    content: @Composable ColumnScope.(SettingsScrollAnchor) -> Unit,
) {
    val anchor = remember { SettingsScrollAnchor() }
    // A section is placed only once the stored settings are read, so wait for its position.
    LaunchedEffect(scrollTo, anchor.top) {
        if (scrollTo == null) return@LaunchedEffect
        val top = anchor.top ?: return@LaunchedEffect
        scroll.scrollTo(top)
        onScrolled()
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(scroll),
    ) {
        content(anchor)
    }
}
