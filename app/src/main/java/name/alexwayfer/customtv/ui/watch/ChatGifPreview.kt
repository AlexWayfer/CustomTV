package name.alexwayfer.customtv.ui.watch

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import name.alexwayfer.customtv.chat.ChatPart
import name.alexwayfer.customtv.ui.components.SharedEmoteImage
import name.alexwayfer.customtv.ui.components.resolvedEmoteAspectRatio

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ChatGifPreview(gif: ChatPart.Gif, onDismiss: () -> Unit) {
    DismissWhenPlayerContentHidden(onDismiss)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var dismissing by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = {
            if (!dismissing) {
                dismissing = true
                scope.launch {
                    sheetState.hide()
                    onDismiss()
                }
            }
        },
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BoxWithConstraints(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                val aspectRatio = resolvedEmoteAspectRatio(gif.url, gif.aspectRatio)
                val imageHeight = minOf(280.dp, maxWidth / aspectRatio)
                SharedEmoteImage(
                    url = gif.url,
                    contentDescription = gif.name,
                    size = imageHeight,
                    aspectRatio = aspectRatio,
                )
            }
            val name = gif.name.removeSurrounding("[", "]")
            Text(
                text = name,
                modifier = Modifier.padding(top = 16.dp),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            EmotePreviewCaption(name = name, url = gif.url)
        }
    }
}
