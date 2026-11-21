package name.alexwayfer.customtv.ui.watch

import androidx.compose.runtime.Composable

@Composable
internal fun WatchDropsSession(scene: WatchScene, content: @Composable () -> Unit) = content()

@Composable
internal fun WatchDrops(scene: WatchScene, browserSheetHeightPx: () -> Int, content: @Composable () -> Unit) = content()

@Composable
internal fun WatchDropsMenuRow(openAfterMenu: (() -> Unit) -> Unit) = Unit
