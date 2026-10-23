package name.alexwayfer.customtv.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Hiding categories is a Premium feature: the free build hides none. */
internal object HiddenCategoriesStore {
    val ids: StateFlow<Set<String>> = MutableStateFlow(emptySet())

    fun start(context: Context) = Unit
}
