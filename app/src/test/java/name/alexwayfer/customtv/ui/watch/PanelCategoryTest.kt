package name.alexwayfer.customtv.ui.watch

import name.alexwayfer.customtv.data.TwitchCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PanelCategoryTest {
    private val category = TwitchCategory("Just Chatting", "jc.jpg", "509658")

    @Test
    fun theOpenPanelsCategoryWithAnIdOpens() {
        assertEquals(category, panelCategoryToOpen(category, expanded = true))
    }

    @Test
    fun aTapOnTheCollapsedPanelOpensThePanelNotTheCategory() {
        assertNull(panelCategoryToOpen(category, expanded = false))
    }

    @Test
    fun aCategoryWithoutAnIdHasNothingToOpen() {
        assertNull(panelCategoryToOpen(TwitchCategory("Chapter 1", null), expanded = true))
        assertNull(panelCategoryToOpen(category.copy(id = " "), expanded = true))
    }

    @Test
    fun noCategoryOpensNothing() {
        assertNull(panelCategoryToOpen(null, expanded = true))
    }
}
