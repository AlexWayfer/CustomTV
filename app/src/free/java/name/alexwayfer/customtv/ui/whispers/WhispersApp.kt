package name.alexwayfer.customtv.ui.whispers

import androidx.compose.runtime.Composable

/** Whispers are a Premium feature: the free build has no whispers section and no notifications. */
@Suppress("unused")
@Composable
internal fun WhispersBackground(userId: String?, onOpenRequested: () -> Unit) = Unit

@Suppress("unused")
@Composable
internal fun WhispersSection(onClosed: () -> Unit, onOpenChannel: (String) -> Unit) = Unit
