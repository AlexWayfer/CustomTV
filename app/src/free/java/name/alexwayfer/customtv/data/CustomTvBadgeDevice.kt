package name.alexwayfer.customtv.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** The free build registers the free badge. */
internal fun customTvBadgeDevice(): Flow<CustomTvBadgeDevice> = flowOf(CustomTvBadgeDevice.Free)
