package name.alexwayfer.customtv.ui.watch

import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/** FFZ's custom moderator badge is a transparent picture that FFZ draws on the moderator green. */
internal fun isFfzModBadge(model: Any?): Boolean =
    model is String && "frankerfacez.com/room-badge/mod/" in model

internal fun Modifier.ffzModBadgeBackground(model: Any?): Modifier =
    if (isFfzModBadge(model)) background(FfzModBadgeGreen) else this

private val FfzModBadgeGreen = Color(0xFF34AE0A)
