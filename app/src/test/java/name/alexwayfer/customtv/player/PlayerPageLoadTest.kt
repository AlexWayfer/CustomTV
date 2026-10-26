package name.alexwayfer.customtv.player

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerPageLoadTest {
    private val load = PlayerPageLoad()

    @Test
    fun theFirstFinishOfALoadCounts() {
        load.start()
        assertTrue(load.finish())
    }

    @Test
    fun aSecondFinishOfTheSameLoadIsIgnored() {
        load.start()
        load.finish()
        assertFalse(load.finish())
    }

    @Test
    fun theAbortedLoadsFinishDoesNotCountForTheNewLoad() {
        load.start()
        load.start()
        assertFalse(load.finish())
        assertTrue(load.finish())
    }

    @Test
    fun everyAbortedLoadReportsItsOwnFinish() {
        load.start()
        load.start()
        load.start()
        assertFalse(load.finish())
        assertFalse(load.finish())
        assertTrue(load.finish())
    }

    @Test
    fun aRestartAfterTheFinishHasNoStaleFinish() {
        load.start()
        load.finish()
        load.start()
        assertTrue(load.finish())
    }

    @Test
    fun aFailedLoadFailsOnceAndItsErrorPageDoesNotCountAsLoaded() {
        load.start()
        assertTrue(load.fail())
        assertFalse(load.fail())
        assertFalse(load.finish())
    }

    @Test
    fun aReloadRightAfterAFailureSkipsTheErrorPagesFinish() {
        load.start()
        load.fail()
        load.start()
        assertFalse(load.finish())
        assertTrue(load.finish())
    }

    @Test
    fun aLoadedPageDoesNotFailAnyMore() {
        load.start()
        load.finish()
        assertFalse(load.fail())
    }

    @Test
    fun nothingFailsBeforeALoadStarts() {
        assertFalse(load.fail())
    }

    @Test
    fun aStartedLoadAwaitsTheResponseUntilThePageStarts() {
        load.start()
        assertTrue(load.awaitingResponse)
        load.respond()
        assertFalse(load.awaitingResponse)
    }

    @Test
    fun aPageThatStartedButStillDownloadsIsNotAwaitingTheResponse() {
        load.start()
        load.respond()
        assertFalse(load.awaitingResponse)
        assertTrue(load.finish())
    }

    @Test
    fun aRestartAwaitsTheResponseAgain() {
        load.start()
        load.respond()
        load.start()
        assertTrue(load.awaitingResponse)
    }

    @Test
    fun aFailedOrLoadedPageIsNotAwaitingTheResponse() {
        load.start()
        load.fail()
        assertFalse(load.awaitingResponse)
        load.start()
        load.finish()
        load.finish()
        assertFalse(load.awaitingResponse)
    }

    @Test
    fun onlyTheFirstAnswerOfALoadCounts() {
        load.start()
        assertTrue(load.respond())
        assertFalse(load.respond())
    }

    @Test
    fun aRestartCountsItsAnswerAgain() {
        load.start()
        load.respond()
        load.start()
        assertTrue(load.respond())
    }

    @Test
    fun aFailedOrNotStartedLoadHasNoAnswer() {
        assertFalse(load.respond())
        load.start()
        load.fail()
        assertFalse(load.respond())
    }

    @Test
    fun nothingAwaitsTheResponseBeforeALoadStarts() {
        assertFalse(load.awaitingResponse)
    }

    @Test
    fun aLoadIsCurrentOnceItsPageStarts() {
        load.start()
        assertFalse(load.current)
        load.respond()
        assertTrue(load.current)
    }

    @Test
    fun aRestartIsNotCurrentUntilItsOwnPageStarts() {
        load.start()
        load.respond()
        load.start()
        assertFalse(load.current)
    }

    @Test
    fun aPageThatLoadedWithoutAReportedStartIsCurrent() {
        load.start()
        load.finish()
        assertTrue(load.current)
    }

    @Test
    fun theAbortedLoadsFinishDoesNotMakeTheNewLoadCurrent() {
        load.start()
        load.respond()
        load.start()
        load.finish()
        assertFalse(load.current)
    }

    @Test
    fun aFailedLoadIsNotCurrent() {
        load.start()
        load.fail()
        assertFalse(load.current)
    }
}
