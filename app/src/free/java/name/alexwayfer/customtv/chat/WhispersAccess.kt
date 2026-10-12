package name.alexwayfer.customtv.chat

import androidx.compose.runtime.Composable

@Suppress("FunctionOnlyReturningConstant")
internal fun whispersNavEnabled(): Boolean = false

/** Whispers are a Premium feature, so nothing is ever unread. */
@Suppress("FunctionOnlyReturningConstant")
@Composable
internal fun whispersUnreadCount(): Int = 0
