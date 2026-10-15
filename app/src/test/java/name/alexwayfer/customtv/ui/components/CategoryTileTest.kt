package name.alexwayfer.customtv.ui.components

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class CategoryTileTest {
    @Test
    fun aPhoneGridKeepsThreeColumnsAndAWideScreenAddsMore() {
        assertEquals(3, categoryGridColumns(360.dp))
        assertEquals(3, categoryGridColumns(200.dp))
        assertEquals(6, categoryGridColumns(800.dp))
    }
}
