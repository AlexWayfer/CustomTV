package name.alexwayfer.customtv.ui.watch

import androidx.compose.runtime.Composable

/** Editing the Twitch chat identity requires the experimental login in Premium. */
@Suppress("FunctionOnlyReturningConstant")
@Composable
internal fun rememberChatIdentityPage(
    userId: String?,
    channelId: String?,
    readableColors: Boolean,
): (() -> ChatMenuSubpage)? = null

@Composable
internal fun ChatIdentityRow(onOpen: (() -> Unit)?) = Unit
