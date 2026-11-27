package name.alexwayfer.customtv.player

import android.app.Application
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import android.webkit.ValueCallback
import android.webkit.WebView
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class PlayerKeepAliveTest {
    private val context = RuntimeEnvironment.getApplication()
    private val audio = shadowOf(context.getSystemService(AudioManager::class.java))
    private val looper = shadowOf(Looper.getMainLooper())
    private val webView = ScriptWebView(context)
    private var pauseRequests = 0
    private val commands = StreamPlayback.Commands(
        play = {},
        pause = { onRun ->
            pauseRequests++
            onRun()
        },
        seek = {},
        close = {},
    )
    private val keepAlive = PlayerKeepAlive(
        appContext = context,
        handler = Handler(Looper.getMainLooper()),
        webView = webView,
        scripts = TwitchPlayerScripts(context.assets) { "page" },
        isReleased = { false },
        onBackgroundChanged = {},
    )

    @Before
    fun startPlayback() {
        StreamPlayback.bind(commands, context)
        StreamPlayback.reportActualState(playing = true)
        keepAlive.register()
        keepAlive.keepAwake(background = true)
    }

    @After
    fun releasePlayer() {
        keepAlive.release()
        StreamPlayback.unbind(commands)
        webView.destroy()
    }

    @Test
    fun backgroundPlaybackStillReceivesRepeatedPlayCommands() {
        looper.idleFor(Duration.ofSeconds(6))

        assertEquals(3, webView.scripts.size)
        assertTrue(webView.scripts.all { it.endsWith("})('background');") })
    }

    @Test
    fun screenLockStillRestartsPlaybackWithoutVoiceRecognition() {
        StreamInterruption.report()
        context.sendBroadcast(Intent(Intent.ACTION_SCREEN_OFF))
        looper.idleFor(Duration.ofSeconds(6))

        assertEquals(3, webView.scripts.size)
    }

    @Test
    fun interruptedPlaybackWaitsForOtherMediaToStopBeforePlayingAgain() {
        StreamInterruption.report()
        audio.setActivePlaybackConfigurationsFor(listOf(mediaAudio))
        looper.idleFor(Duration.ofSeconds(6))

        assertEquals(0, webView.scripts.size)

        audio.setActivePlaybackConfigurationsFor(emptyList())
        looper.idleFor(Duration.ofSeconds(2))

        assertEquals(1, webView.scripts.size)
    }

    @Test
    fun activeAudioWithoutAnInterruptionKeepsBackgroundPlayback() {
        audio.setActivePlaybackConfigurationsFor(listOf(mediaAudio))
        looper.idleFor(Duration.ofSeconds(2))

        assertEquals(1, webView.scripts.size)
    }

    @Test
    fun aNetworkStallRecoversWhileTheStreamAudioTrackIsStillActive() {
        val steps = mutableListOf<LiveStallAction>()
        val recovery = liveRecovery(steps)
        val stalled = LiveTick(100.0, 11.0, "Buffering", false, "[70.0, 100.0]")
        recovery.tick(stalled.copy(state = "Playing", hasPlaybackBuffer = true))
        recovery.networkChanged()
        audio.setActivePlaybackConfigurationsFor(listOf(mediaAudio))
        StreamInterruption.report()

        reportLiveTicks(recovery, stalled, seconds = 9)
        assertTrue(steps.isEmpty())
        reportLiveTicks(recovery, stalled, seconds = 1)
        assertEquals(listOf(LiveStallAction.RestartPlayer), steps)
        reportLiveTicks(recovery, stalled, seconds = 10)
        assertEquals(listOf(LiveStallAction.RestartPlayer, LiveStallAction.ReloadPage), steps)
    }

    @Test
    fun aLaterStallStillWaitsForOtherMediaAfterTheStreamRecoversFromANetworkSwitch() {
        val steps = mutableListOf<LiveStallAction>()
        val recovery = liveRecovery(steps)
        val playing = LiveTick(100.0, 11.0, "Playing", false, "[70.0, 103.0]", hasPlaybackBuffer = true)
        recovery.tick(playing)
        recovery.networkChanged()
        reportLiveTicks(recovery, playing.copy(positionSeconds = 101.0), seconds = 1)

        audio.setActivePlaybackConfigurationsFor(listOf(mediaAudio))
        StreamInterruption.report()
        val stalled = playing.copy(positionSeconds = 101.0, state = "Buffering", hasPlaybackBuffer = false)
        reportLiveTicks(recovery, stalled, seconds = 15)

        assertTrue(steps.isEmpty())
    }

    @Test
    fun voiceRecognitionStopsAutomaticPlayBeforeItCanTakeFocusBack() {
        recording(MediaRecorder.AudioSource.VOICE_RECOGNITION)
        StreamInterruption.report()
        looper.idleFor(Duration.ofSeconds(6))

        assertEquals(0, webView.scripts.size)
    }

    @Test
    fun recognitionStartingAfterTheFocusInterruptionAlsoStopsAutomaticPlay() {
        StreamInterruption.report()
        recording(MediaRecorder.AudioSource.VOICE_RECOGNITION)
        looper.idleFor(Duration.ofSeconds(6))

        assertEquals(0, webView.scripts.size)
    }

    @Test
    fun aTwitchPauseReportInTheBackgroundAlsoWaitsForFocusAfterRecognition() {
        recording(MediaRecorder.AudioSource.VOICE_RECOGNITION)
        StreamPlayback.reportActualState(playing = false)
        audio.setActiveRecordingConfigurations(emptyList(), true)
        looper.idleFor(Duration.ofSeconds(6))

        assertEquals(0, webView.scripts.size)
    }

    @Test
    fun aBackgroundPauseBeforeTheRecognitionCallbackAlsoWaitsForFocus() {
        StreamPlayback.reportActualState(playing = false)
        recording(MediaRecorder.AudioSource.VOICE_RECOGNITION)
        audio.setActiveRecordingConfigurations(emptyList(), true)
        looper.idleFor(Duration.ofSeconds(6))

        assertEquals(0, webView.scripts.size)
    }

    @Test
    fun endingTheRecordingWaitsForTheSystemToResumePlayback() {
        recording(MediaRecorder.AudioSource.VOICE_RECOGNITION)
        StreamInterruption.report()
        audio.setActiveRecordingConfigurations(emptyList(), true)
        looper.idleFor(Duration.ofSeconds(6))
        assertEquals(0, webView.scripts.size)

        StreamPlayback.reportActualState(playing = true)
        looper.idleFor(Duration.ofSeconds(2))
        assertEquals(1, webView.scripts.size)
    }

    @Test
    fun manualPlayCanResumeAfterTheCommand() {
        recording(MediaRecorder.AudioSource.VOICE_RECOGNITION)
        StreamInterruption.report()
        audio.setActiveRecordingConfigurations(emptyList(), true)
        StreamPlayback.play()
        looper.idleFor(Duration.ofSeconds(2))

        assertEquals(1, webView.scripts.size)
    }

    @Test
    fun manualPauseStaysPausedAfterRecognitionEnds() {
        recording(MediaRecorder.AudioSource.VOICE_RECOGNITION)
        StreamInterruption.report()
        StreamPlayback.pause()
        audio.setActiveRecordingConfigurations(emptyList(), true)
        looper.idleFor(Duration.ofSeconds(6))

        assertEquals(0, webView.scripts.size)
    }

    @Test
    fun chatOnlyStaysPausedWhenTwitchResumesAfterThePauseCommand() {
        StreamPlayback.pause(holdUntilPlay = true)
        StreamPlayback.reportActualState(playing = false)
        repeat(2) { StreamPlayback.reportActualState(playing = true) }

        assertEquals(PlayerPlayback.Paused, StreamPlayback.playback.value)
        assertEquals(2, pauseRequests)

        StreamPlayback.reportActualState(playing = false)
        StreamPlayback.reportActualState(playing = true)

        assertEquals(PlayerPlayback.Paused, StreamPlayback.playback.value)
        assertEquals(3, pauseRequests)

        StreamPlayback.play()
        StreamPlayback.reportActualState(playing = true)

        assertEquals(PlayerPlayback.Playing, StreamPlayback.playback.value)
        assertEquals(3, pauseRequests)
    }

    @Test
    fun interruptedMicrophoneRecordingHoldsBackgroundRecovery() {
        recording(MediaRecorder.AudioSource.MIC)
        StreamInterruption.report()
        looper.idleFor(Duration.ofSeconds(2))

        assertEquals(0, webView.scripts.size)
    }

    @Test
    fun microphoneRecordingWithoutAnInterruptionKeepsBackgroundPlayback() {
        recording(MediaRecorder.AudioSource.MIC)
        looper.idleFor(Duration.ofSeconds(2))

        assertEquals(1, webView.scripts.size)
    }

    @Test
    fun voiceCommunicationHoldsAutomaticPlayWhileRecording() {
        StreamPlayback.reportActualState(playing = false)
        recording(MediaRecorder.AudioSource.VOICE_COMMUNICATION)
        looper.idleFor(Duration.ofSeconds(6))

        assertEquals(0, webView.scripts.size)
    }

    @Test
    fun aMicrophoneRecordingCanResumeAfterItEndsWithoutAFocusGainCallback() {
        recording(MediaRecorder.AudioSource.MIC)
        StreamPlayback.reportActualState(playing = false)
        looper.idleFor(Duration.ofSeconds(2))
        assertEquals(0, webView.scripts.size)

        audio.setActiveRecordingConfigurations(emptyList(), true)
        looper.idleFor(Duration.ofSeconds(2))
        assertEquals(1, webView.scripts.size)
    }

    @Test
    fun voiceCommunicationCanResumeAfterItEndsWithoutAFocusGainCallback() {
        recording(MediaRecorder.AudioSource.VOICE_COMMUNICATION)
        StreamPlayback.reportActualState(playing = false)
        looper.idleFor(Duration.ofSeconds(2))
        assertEquals(0, webView.scripts.size)

        audio.setActiveRecordingConfigurations(emptyList(), true)
        looper.idleFor(Duration.ofSeconds(2))
        assertEquals(1, webView.scripts.size)
    }

    @Test
    fun aVoiceConversationHoldsBeforeTheMicrophoneOpensAndUntilCommunicationModeEnds() {
        for (mode in listOf(AudioManager.MODE_IN_COMMUNICATION, AudioManager.MODE_IN_CALL)) {
            context.getSystemService(AudioManager::class.java).mode = mode
            StreamPlayback.reportActualState(playing = false)
            looper.idleFor(Duration.ofSeconds(2))
            assertEquals(0, webView.scripts.size)

            recording(MediaRecorder.AudioSource.VOICE_COMMUNICATION)
            audio.setActiveRecordingConfigurations(emptyList(), true)
            looper.idleFor(Duration.ofSeconds(2))
            assertEquals(0, webView.scripts.size)

            context.getSystemService(AudioManager::class.java).mode = AudioManager.MODE_NORMAL
            looper.idleFor(Duration.ofSeconds(2))
            assertEquals(1, webView.scripts.size)
            webView.scripts.clear()
        }
    }

    @Test
    fun microphoneRecordingDoesNotCountAsALiveStreamStallAfterANetworkSwitch() {
        val sources = listOf(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            MediaRecorder.AudioSource.MIC,
            MediaRecorder.AudioSource.VOICE_COMMUNICATION,
        )
        for (source in sources) {
            val steps = mutableListOf<LiveStallAction>()
            val recovery = liveRecovery(steps)
            recovery.networkChanged()
            recording(source)
            StreamInterruption.report()

            reportLiveTicks(recovery, LiveTick(1.0, 0.0, "Playing", false, ""), seconds = 15)

            assertTrue(steps.isEmpty())
            audio.setActiveRecordingConfigurations(emptyList(), true)
            StreamPlayback.reportActualState(playing = true)
        }
    }

    private fun liveRecovery(steps: MutableList<LiveStallAction>) = LiveStallRecovery(
        isLive = { true },
        isPlaybackConfirmed = { StreamPlayback.isPlaybackConfirmed },
        restartPlayer = { steps += LiveStallAction.RestartPlayer },
        reloadPage = { steps += LiveStallAction.ReloadPage },
    )

    private fun reportLiveTicks(recovery: LiveStallRecovery, tick: LiveTick, seconds: Int) {
        repeat(seconds) {
            looper.idleFor(Duration.ofSeconds(1))
            recovery.tick(tick)
        }
    }

    private fun recording(source: Int) {
        audio.setActiveRecordingConfigurations(
            listOf(audio.createActiveRecordingConfiguration(1, source, "")),
            true,
        )
        looper.idle()
    }

    private val mediaAudio = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).build()

    private class ScriptWebView(context: Context) : WebView(context) {
        val scripts = mutableListOf<String>()

        override fun evaluateJavascript(script: String, resultCallback: ValueCallback<String>?) {
            scripts += script
            resultCallback?.onReceiveValue("null")
        }
    }
}
