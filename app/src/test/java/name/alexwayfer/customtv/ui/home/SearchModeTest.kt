package name.alexwayfer.customtv.ui.home

import org.junit.Assert.assertEquals
import org.junit.Test

class SearchModeTest {
    @Test
    fun theSearchOpensInTheModeItIsOpenedIn() {
        assertEquals(SearchMode.Categories, shownSearchMode(null, SearchMode.Categories, categoriesOffered = true))
        assertEquals(SearchMode.Channels, shownSearchMode(null, SearchMode.Channels, categoriesOffered = true))
    }

    @Test
    fun aPickedModeStaysWhileTheSearchIsOpen() {
        assertEquals(
            SearchMode.Categories,
            shownSearchMode(SearchMode.Categories, SearchMode.Channels, categoriesOffered = true),
        )
        assertEquals(
            SearchMode.Channels,
            shownSearchMode(SearchMode.Channels, SearchMode.Categories, categoriesOffered = true),
        )
    }

    @Test
    fun withoutCategoriesTheSearchLooksForChannels() {
        assertEquals(SearchMode.Channels, shownSearchMode(null, SearchMode.Categories, categoriesOffered = false))
        assertEquals(
            SearchMode.Channels,
            shownSearchMode(SearchMode.Categories, SearchMode.Categories, categoriesOffered = false),
        )
    }
}
