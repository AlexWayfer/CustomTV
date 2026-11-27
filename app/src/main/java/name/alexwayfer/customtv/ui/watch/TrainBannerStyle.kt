package name.alexwayfer.customtv.ui.watch

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.chat.HypeTrainTheme
import name.alexwayfer.customtv.data.TwitchTrainCatalog

internal data class TrainBannerStyle(val title: String, val gradient: Brush)

/** Before the first successful refresh the bundled Heist card also works offline. */
@Composable
internal fun trainBannerStyle(theme: HypeTrainTheme?): TrainBannerStyle? {
    if (theme == null || theme == HypeTrainTheme.Regular) return null
    val catalog by TwitchTrainCatalog.appearances.collectAsStateWithLifecycle()
    val appearance = catalog?.get(theme.key)
    if (appearance != null) {
        val language = stringResource(R.string.twitch_language)
        return remember(appearance, language) {
            val stops = appearance.stops.map { (stop, argb) -> stop to Color(argb) }
            val brush = if (stops.size == 2) Brush.horizontalGradient(stops[0], stops[1])
            else Brush.horizontalGradient(stops[0], stops[1], stops[2])
            TrainBannerStyle(
                if (language.startsWith("ru")) appearance.russian else appearance.english,
                brush,
            )
        }
    }
    if (catalog != null) return null
    val title = when (theme) {
        HypeTrainTheme.Heist -> R.string.chat_heist_train
        HypeTrainTheme.SharedHeist -> R.string.chat_shared_heist_train
        else -> return null
    }
    return TrainBannerStyle(stringResource(title), bundledHeistGradient)
}

private val bundledHeistGradient = Brush.horizontalGradient(
    0f to Color(0xFF445ED6),
    0.55f to Color(0xFFFC52AE),
    1f to Color(0xFFED815F),
)
