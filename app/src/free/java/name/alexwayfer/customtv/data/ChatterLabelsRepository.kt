package name.alexwayfer.customtv.data

import android.content.Context
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow

object ChatterLabelsRepository {
    internal val credentials = MutableStateFlow<ChatterLabelsCredentials?>(null)
    internal val credentialsLoaded = MutableStateFlow(true)
    val labelsByUserId = MutableStateFlow<Map<String, List<ChatterLabelIcon>>>(emptyMap())

    fun init(context: Context) {
        if (context.applicationContext.packageName.isEmpty()) credentials.value = null
    }

    fun refresh(): Job = Job().apply { complete() }
}
