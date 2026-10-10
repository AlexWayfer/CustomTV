package name.alexwayfer.customtv.ui.watch

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.TextUnit
import name.alexwayfer.customtv.chat.ChatWarningNotice

/** Moderator tools are Premium: the free chat never gets a warning row. */
@Suppress("unused")
@Composable
internal fun ChatWarningNoticeLine(
    notice: ChatWarningNotice,
    timestampText: String?,
    textSize: TextUnit,
    lineHeight: TextUnit,
) = Unit
