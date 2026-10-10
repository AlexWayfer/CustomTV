package name.alexwayfer.customtv.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class LeavingItemsTest {
    @Test
    fun nothingLeavingLaysOutTheCurrentItems() =
        assertEquals(listOf("a", "b", "c"), itemsWithLeaving(listOf("a", "b"), listOf("a", "b", "c")))

    @Test
    fun aRemovedItemStaysWhereItWas() =
        assertEquals(listOf("a", "b", "c"), itemsWithLeaving(listOf("a", "b", "c"), listOf("a", "c")))

    @Test
    fun aRemovedFirstItemStaysFirst() =
        assertEquals(listOf("a", "b"), itemsWithLeaving(listOf("a", "b"), listOf("b")))

    @Test
    fun aRemovedItemStaysBeforeOneAddedAtTheEnd() =
        assertEquals(listOf("a", "b", "c"), itemsWithLeaving(listOf("a", "b"), listOf("a", "c")))

    @Test
    fun everyItemLeavingKeepsTheirOrder() =
        assertEquals(listOf("a", "b"), itemsWithLeaving(listOf("a", "b"), emptyList()))

    @Test
    fun nothingShownOrCurrentLaysOutNothing() =
        assertEquals(emptyList<String>(), itemsWithLeaving(emptyList<String>(), emptyList()))
}
