package name.alexwayfer.customtv.ui.watch

import androidx.compose.runtime.Composable

/** Managing a prediction needs the experimental login, a Premium feature: the free build has no `/prediction`. */
@Suppress("unused", "FunctionOnlyReturningConstant")
@Composable
internal fun rememberPredictionCommand(channelId: String?): (() -> Unit)? = null
