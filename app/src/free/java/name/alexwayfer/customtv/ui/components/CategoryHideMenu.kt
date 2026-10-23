package name.alexwayfer.customtv.ui.components

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import name.alexwayfer.customtv.data.TwitchCategory

/** Hiding categories is a Premium feature: the free build has no long press for it. */
@Suppress("FunctionOnlyReturningConstant")
@Composable
internal fun categoryMenuLabel(category: TwitchCategory?): String? = null

@Composable
internal fun CategoryHideMenu(category: TwitchCategory?, expanded: Boolean, onDismiss: () -> Unit) = Unit

@Composable
internal fun HiddenCategoryUndoSnackbar(snackbarHostState: SnackbarHostState) = Unit
