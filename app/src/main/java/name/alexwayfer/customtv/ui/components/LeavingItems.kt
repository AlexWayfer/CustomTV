package name.alexwayfer.customtv.ui.components

/**
 * The items to lay out while some animate away: every [current] item in its order, and each item of
 * [shown] that is no longer current kept right after the item it followed, so it leaves from its place.
 */
internal fun <T> itemsWithLeaving(shown: List<T>, current: List<T>): List<T> {
    val result = current.toMutableList()
    val currentSet = current.toSet()
    var previous: T? = null
    var hasPrevious = false
    shown.forEach { item ->
        if (item !in currentSet && item !in result) {
            val at = if (hasPrevious) result.indexOf(previous) + 1 else 0
            result.add(at, item)
        }
        if (item in result) {
            previous = item
            hasPrevious = true
        }
    }
    return result
}
