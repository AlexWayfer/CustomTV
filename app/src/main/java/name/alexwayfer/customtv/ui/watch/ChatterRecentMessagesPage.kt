package name.alexwayfer.customtv.ui.watch

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import name.alexwayfer.customtv.R

@Composable
internal fun chatterMentionsTitle(count: Int): String =
    pluralStringResource(R.plurals.chatter_card_last_mentions_count, count, count)

@Composable
internal fun ChatterRecentMessagesHeader(
    title: String,
    onBack: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.chatter_card_back_to_profile),
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/**
 * Whether the recent messages or mentions page keeps its newest message in view. A changed scroll range, such
 * as a new message, keeps the current choice; a scroll by the reader follows only at the end.
 */
internal fun followChatterMessagesEnd(following: Boolean, previousMax: Int, value: Int, max: Int): Boolean =
    if (max != previousMax) following else value >= max
