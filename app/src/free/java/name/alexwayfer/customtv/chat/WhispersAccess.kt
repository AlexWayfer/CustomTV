package name.alexwayfer.customtv.chat

import androidx.compose.runtime.Composable

internal fun whispersNavEnabled(): Boolean = false

/** Whispers are a Premium feature, so nothing is ever unread. */
@Composable
internal fun whispersUnreadCount(): Int = 0
