package name.alexwayfer.customtv.ui.explore

import androidx.compose.runtime.Composable
import name.alexwayfer.customtv.data.TwitchCategory
import name.alexwayfer.customtv.ui.ExploreCategoryOpen
import name.alexwayfer.customtv.ui.home.HomeViewModel
import name.alexwayfer.customtv.ui.watch.OneShotRequest

/** Explore's recommendations are a Premium feature: the free build has no Explore section. */
@Suppress("FunctionOnlyReturningConstant")
internal fun exploreNavEnabled(): Boolean = false

@Suppress("unused")
@Composable
internal fun ExploreSection(
    homeViewModel: HomeViewModel,
    signedIn: Boolean,
    backEnabled: Boolean,
    reselected: OneShotRequest<Unit>?,
    showCategories: OneShotRequest<Unit>?,
    openCategory: OneShotRequest<ExploreCategoryOpen>?,
    closeCategory: OneShotRequest<Unit>?,
    onBackToPlayer: (() -> Unit)?,
    onBack: () -> Unit,
    onOpen: (login: String, live: Boolean) -> Unit,
    onOpenCategory: (TwitchCategory) -> Unit,
    onSearchOpenChange: (Boolean) -> Unit,
) = Unit
