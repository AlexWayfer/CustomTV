package name.alexwayfer.customtv.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

internal val activityStatisticsAuthorization: Flow<ActivityAuthorization?> = flowOf(ActivityAuthorization("free"))
