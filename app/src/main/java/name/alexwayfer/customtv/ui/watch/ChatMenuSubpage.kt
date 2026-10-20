package name.alexwayfer.customtv.ui.watch

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.ui.components.lateralTransform

/** A page the chat menu opens in place of its rows, such as the donation panels; Back returns to the rows. */
internal class ChatMenuSubpage(@param:StringRes val title: Int, val content: @Composable () -> Unit)

/**
 * The chat menu's [rows], or the page one of them opened in their place. A page comes in from the end and goes back
 * the way it came, as the chatter card's pages do; the system Back leaves it for the rows, like its arrow, instead
 * of closing the sheet. Call it inside the sheet, whose window takes Back first.
 */
@Composable
internal fun ChatMenuPages(maxHeight: Dp, rows: @Composable (openSubpage: (ChatMenuSubpage) -> Unit) -> Unit) {
    var subpage by remember { mutableStateOf<ChatMenuSubpage?>(null) }
    BackHandler(enabled = subpage != null) { subpage = null }
    AnimatedContent(
        targetState = subpage,
        transitionSpec = { lateralTransform(forward = targetState != null) },
        label = "chatMenuPage",
    ) { shownPage ->
        if (shownPage != null) {
            ChatMenuSubpageContent(shownPage, maxHeight, onBack = { subpage = null })
        } else {
            rows { subpage = it }
        }
    }
}

/** The [page] under its title and a back arrow; its content scrolls, and a drag back up stops at its top. */
@Composable
internal fun ChatMenuSubpageContent(page: ChatMenuSubpage, maxHeight: Dp, onBack: () -> Unit) {
    val scrollState = rememberScrollState()
    val overscroll = rememberOverscrollEffect()
    val stopAtTop = rememberStopAtScrollTop(scrollState, overscroll)
    Column(modifier = Modifier.heightIn(max = maxHeight)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(end = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.navigate_back),
                )
            }
            Text(
                text = stringResource(page.title),
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Column(
            // Only the height under the title, so the end of the content stays reachable.
            modifier = Modifier
                .weight(1f, fill = false)
                .nestedScroll(stopAtTop)
                .verticalScroll(scrollState, overscrollEffect = overscroll)
                .padding(bottom = 12.dp),
        ) {
            page.content()
        }
    }
}
