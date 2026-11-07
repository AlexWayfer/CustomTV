package name.alexwayfer.customtv.player

import android.app.Application
import android.os.Handler
import android.os.Looper
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class PlayerPageRecoveryTest {
    @Test
    fun aStreamThatRestartsAfterGoingOfflineConfirmsPlaybackAgain() {
        val recovery = PlayerPageRecovery(
            handler = Handler(Looper.getMainLooper()),
            isReleased = { false },
            isSurfaceRevealed = { true },
            actions = PlayerPageActions(
                reloadPlayer = {},
                reloadPage = {},
                concealSurface = {},
                logPlayerState = { it() },
            ),
        )
        assertTrue(recovery.confirmPlayback())
        assertFalse(recovery.confirmPlayback())

        recovery.videoWithheld()

        assertFalse(recovery.playbackConfirmed)
        assertTrue(recovery.confirmPlayback())
        assertFalse(recovery.confirmPlayback())
    }
}
