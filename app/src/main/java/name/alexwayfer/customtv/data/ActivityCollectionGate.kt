package name.alexwayfer.customtv.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

/** A restored opt-out or logout cancels the entire operation, including OkHttp's asynchronous call. */
internal suspend fun collectActivityStatistics(
    enabled: Flow<Boolean>,
    authorization: Flow<ActivityAuthorization?>,
    report: suspend (ActivityAuthorization) -> Unit,
) {
    combine(enabled, authorization) { allowed, access -> access.takeIf { allowed } }
        .distinctUntilChanged()
        .collectLatest { access -> if (access != null) report(access) }
}
