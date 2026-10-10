package name.alexwayfer.customtv.ui.watch

import org.junit.Assert.assertEquals
import org.junit.Test

class ChatterHeaderCollapseTest {
    @Test
    fun theProfileShowsItsOwnCollapseAndTheOtherPagesTheFoldedBanner() {
        assertEquals(10f, pinnedCollapsePx(collapsedPx = 10f, rangePx = 40f, pin = 0f))
        assertEquals(40f, pinnedCollapsePx(collapsedPx = 10f, rangePx = 40f, pin = 1f))
        assertEquals(25f, pinnedCollapsePx(collapsedPx = 10f, rangePx = 40f, pin = 0.5f))
    }

    @Test
    fun aPinPastItsEndsOrANegativeRangeStaysWithinTheFold() {
        assertEquals(40f, pinnedCollapsePx(collapsedPx = 10f, rangePx = 40f, pin = 1.2f))
        assertEquals(10f, pinnedCollapsePx(collapsedPx = 10f, rangePx = 40f, pin = -0.2f))
        assertEquals(0f, pinnedCollapsePx(collapsedPx = 0f, rangePx = -5f, pin = 1f))
    }

    @Test
    fun scrollUpCollapsesTheHeaderAndTakesOnlyWhatFits() {
        assertEquals(10f to -10f, collapseHeaderBy(0f, -10f, 24f))
        assertEquals(24f to -4f, collapseHeaderBy(20f, -30f, 24f))
    }

    @Test
    fun collapsedHeaderTakesNoMoreScrollUp() {
        assertEquals(24f to 0f, collapseHeaderBy(24f, -10f, 24f))
    }

    @Test
    fun scrollDownExpandsTheHeaderUpToItsFullHeight() {
        assertEquals(14f to 10f, collapseHeaderBy(24f, 10f, 24f))
        assertEquals(0f to 5f, collapseHeaderBy(5f, 30f, 24f))
        assertEquals(0f to 0f, collapseHeaderBy(0f, 30f, 24f))
    }

    @Test
    fun heightShrinksByTheCollapseAndNeverGoesBelowZero() {
        assertEquals(112, chatterHeaderHeightPx(112, 0f))
        assertEquals(88, chatterHeaderHeightPx(112, 24f))
        assertEquals(0, chatterHeaderHeightPx(10, 24f))
        assertEquals(0, chatterHeaderHeightPx(0, 0f))
    }

    @Test
    fun avatarRowMovesDownWithTheFoldUpToItsFoldedMargin() {
        assertEquals(0, chatterHeaderRowShiftPx(0f, 40f, 8))
        assertEquals(4, chatterHeaderRowShiftPx(20f, 40f, 8))
        assertEquals(8, chatterHeaderRowShiftPx(40f, 40f, 8))
        assertEquals(8, chatterHeaderRowShiftPx(60f, 40f, 8))
    }

    @Test
    fun avatarRowStaysWithoutARangeToFold() {
        assertEquals(0, chatterHeaderRowShiftPx(10f, 0f, 8))
    }

    @Test
    fun avatarShrinksWithTheFoldDownToItsFoldedSize() {
        assertEquals(56, chatterAvatarSizePx(0f, 40f, 56, 44))
        assertEquals(50, chatterAvatarSizePx(20f, 40f, 56, 44))
        assertEquals(44, chatterAvatarSizePx(40f, 40f, 56, 44))
        assertEquals(44, chatterAvatarSizePx(60f, 40f, 56, 44))
    }

    @Test
    fun avatarKeepsItsSizeWithoutARangeToFold() {
        assertEquals(56, chatterAvatarSizePx(10f, 0f, 56, 44))
    }

    @Test
    fun bannerFoldsDownToTheRowWithItsMarginAround() {
        assertEquals(56f, chatterHeaderFoldRangePx(112, 40, 8))
        assertEquals(48f, chatterHeaderFoldRangePx(112, 48, 8))
    }

    @Test
    fun bannerDoesNotFoldWhenTheRowFillsIt() {
        assertEquals(0f, chatterHeaderFoldRangePx(112, 120, 8))
    }

    @Test
    fun avatarSizeNeverGoesBelowZero() {
        assertEquals(0, chatterAvatarSizePx(40f, 40f, 0, -8))
    }
}
