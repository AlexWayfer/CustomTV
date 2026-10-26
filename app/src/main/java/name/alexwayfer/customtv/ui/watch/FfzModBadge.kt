package name.alexwayfer.customtv.ui.watch

import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified

/** FFZ's custom moderator badge is a transparent picture that FFZ draws on the moderator green. */
internal fun isFfzModBadge(model: Any?): Boolean =
    model is String && "frankerfacez.com/room-badge/mod/" in model

internal fun Modifier.ffzModBadgeBackground(model: Any?): Modifier =
    if (isFfzModBadge(model)) background(FfzModBadgeGreen) else this

/** The color a third-party badge is drawn on, such as an FFZ badge's; nothing for an unspecified one. */
internal fun Modifier.badgeBackground(color: Color): Modifier =
    if (color.isSpecified) background(color) else this

private val FfzModBadgeGreen = Color(0xFF34AE0A)
