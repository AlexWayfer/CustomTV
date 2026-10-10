package name.alexwayfer.customtv.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class AbbreviatedCountTest {
    @Test
    fun belowAThousandIsShownWhole() {
        assertEquals("0", abbreviatedCount(0))
        assertEquals("999", abbreviatedCount(999))
    }

    @Test
    fun belowAHundredOfAUnitKeepsOneCutDecimalAndDropsAZeroOne() {
        assertEquals("1K", abbreviatedCount(1_000))
        assertEquals("1K", abbreviatedCount(1_099))
        assertEquals("1.2K", abbreviatedCount(1_250))
        assertEquals("12.1K", abbreviatedCount(12_100))
        assertEquals("11.9K", abbreviatedCount(11_999))
        assertEquals("99.9K", abbreviatedCount(99_999))
    }

    @Test
    fun fromAHundredOfAUnitTheDecimalIsDropped() {
        assertEquals("100K", abbreviatedCount(100_500))
        assertEquals("123K", abbreviatedCount(123_456))
        assertEquals("999K", abbreviatedCount(999_999))
    }

    @Test
    fun millionsAndBillionsUseMAndB() {
        assertEquals("1M", abbreviatedCount(1_000_000))
        assertEquals("19.4M", abbreviatedCount(19_423_869))
        assertEquals("1.8B", abbreviatedCount(1_820_540_120))
    }
}
