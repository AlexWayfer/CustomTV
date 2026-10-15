package name.alexwayfer.customtv.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.memory.MemoryCache
import coil.request.ImageRequest

/** A live stream's preview; it keeps the previous image while a newer one loads, so a refresh never blanks it. */
@Composable
internal fun StreamPreviewImage(url: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var shownKey by remember { mutableStateOf<MemoryCache.Key?>(null) }
    val request = remember(url) {
        ImageRequest.Builder(context)
            .data(url)
            .placeholderMemoryCacheKey(shownKey)
            .crossfade(true)
            .build()
    }
    AsyncImage(
        model = request,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier,
        onSuccess = { state -> shownKey = state.result.memoryCacheKey },
    )
}
