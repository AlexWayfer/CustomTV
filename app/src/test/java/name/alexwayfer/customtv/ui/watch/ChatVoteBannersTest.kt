package name.alexwayfer.customtv.ui.watch

import org.junit.Assert.assertEquals
import org.junit.Test

class ChatVoteBannersTest {
    @Test
    fun anOpenPredictionOnAnotherChannelOffersTheBrowser() {
        assertEquals(
            PredictionBrowserOffer.Browser,
            predictionBrowserOffer(active = true, channelLogin = "ilame", selfLogin = "alexwayfer"),
        )
    }

    @Test
    fun anOpenPredictionOnTheViewersOwnChannelSaysTheyCannotPredictInAnyCase() {
        assertEquals(
            PredictionBrowserOffer.OwnChannel,
            predictionBrowserOffer(active = true, channelLogin = "alexwayfer", selfLogin = "AlexWayfer"),
        )
    }

    @Test
    fun aViewerWhoIsNotLoggedInIsOfferedTheBrowser() {
        assertEquals(
            PredictionBrowserOffer.Browser,
            predictionBrowserOffer(active = true, channelLogin = "alexwayfer", selfLogin = null),
        )
    }

    @Test
    fun aResolvedPredictionOffersNothingEvenOnTheOwnChannel() {
        assertEquals(
            PredictionBrowserOffer.None,
            predictionBrowserOffer(active = false, channelLogin = "alexwayfer", selfLogin = "alexwayfer"),
        )
    }
}
