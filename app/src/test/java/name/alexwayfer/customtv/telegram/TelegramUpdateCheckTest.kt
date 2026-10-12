package name.alexwayfer.customtv.telegram

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import name.alexwayfer.customtv.update.AppRelease
import name.alexwayfer.customtv.update.AppVersion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class TelegramUpdateCheckTest {
    private val ui = MutableStateFlow(TelegramUiState(step = TelegramLoginStep.Ready))
    private var config = TelegramConfig(openChatId = 1L, premiumChatId = 2L)
    private var generation = 1
    private var client: String? = "client"
    private val loads = mutableListOf<String>()
    private var answer: suspend () -> TelegramGroupState = { TelegramGroupState.Open(null) }
    private val notices = mutableListOf<String>()
    private var connection: String? = "ConnectionStateReady"
    private val reopened = mutableListOf<String>()
    private var clockMillis = 0L
    private val release = AppRelease(fileId = 1, version = AppVersion.parse("9.9")!!, downloadedPath = null)

    private fun check(scope: CoroutineScope, timeout: Duration) = TelegramUpdateCheck(
        scope,
        ui,
        config = { config },
        clients = TelegramClients(
            current = { generation to client },
            isCurrent = { checked, source -> checked == generation && source == client },
        ),
        connection = { connection },
        io = TelegramCheckIo(
            loadGroup = { _, chatId, _ ->
                loads += "group $chatId"
                answer()
            },
            premiumAccess = { _, chatId, _ ->
                loads += "access $chatId"
                TelegramGroupState.Open(null)
            },
            showUpdate = { version -> notices += "show $version" },
            clearUpdate = { notices += "clear" },
            reopenConnections = { source -> reopened += source },
            elapsedRealtime = { clockMillis },
            timeout = timeout,
        ),
    )

    /** Runs [block] with a check, and returns once every check it started has ended. */
    private fun withCheck(timeout: Duration = 5.seconds, block: suspend (TelegramUpdateCheck<String>) -> Unit) =
        runBlocking {
            coroutineScope { block(check(this, timeout)) }
        }

    @Test
    fun aCheckThatFindsAnUpdateShowsItAndPostsTheNoticeWhenAsked() {
        answer = { TelegramGroupState.Open(release) }
        withCheck { it.launchCheck(generation, "client", notify = true) }

        assertEquals(TelegramGroupState.Open(release), ui.value.openGroup)
        assertEquals(TelegramGroupState.Open(null), ui.value.premiumGroup)
        assertEquals(listOf("show ${release.version}"), notices)
    }

    @Test
    fun theCheckUpdatesButtonPostsNoNotice() {
        answer = { TelegramGroupState.Open(release) }
        withCheck { it.checkAgain() }

        assertEquals(TelegramGroupState.Open(release), ui.value.openGroup)
        assertTrue(notices.isEmpty())
    }

    @Test
    fun aCheckWithoutANewerReleaseTakesAShownNoticeAway() {
        withCheck { it.checkAgain() }

        assertEquals(listOf("clear"), notices)
    }

    @Test
    fun aSecondCheckJoinsTheRunningOneAndCanTurnItsNoticeOn() {
        val answered = CompletableDeferred<TelegramGroupState>()
        answer = { answered.await() }
        withCheck { check ->
            check.launchCheck(generation, "client", notify = false)
            yield()
            check.launchCheck(generation, "client", notify = true)
            answered.complete(TelegramGroupState.Open(release))
        }

        assertEquals(listOf("group 1", "access 2"), loads)
        assertEquals(listOf("show ${release.version}"), notices)
    }

    @Test
    fun aCheckTelegramDoesNotAnswerShowsTheTimeout() {
        answer = { awaitCancellation() }
        withCheck(timeout = 50.milliseconds) { it.checkAgain() }

        assertTrue(ui.value.openGroup is TelegramGroupState.Failed)
        assertTrue(notices.isEmpty())
    }

    @Test
    fun aTimeoutWhileTdlibIsStillConnectingReopensTheConnections() {
        connection = "ConnectionStateConnecting"
        answer = { awaitCancellation() }
        withCheck(timeout = 50.milliseconds) { it.checkAgain() }

        assertEquals(listOf("client"), reopened)
    }

    @Test
    fun aTimeoutOnAWorkingConnectionKeepsIt() {
        answer = { awaitCancellation() }
        withCheck(timeout = 50.milliseconds) { it.checkAgain() }

        assertTrue(reopened.isEmpty())
    }

    @Test
    fun anAnsweredCheckReopensNothing() {
        connection = "ConnectionStateConnecting"
        withCheck { it.checkAgain() }

        assertTrue(reopened.isEmpty())
    }

    @Test
    fun aTimeoutAfterTheClientChangedLeavesTheSectionAndTheConnectionsAlone() {
        connection = "ConnectionStateConnecting"
        answer = {
            generation = 2
            awaitCancellation()
        }
        withCheck(timeout = 50.milliseconds) { it.checkAgain() }

        assertEquals(TelegramGroupState.Checking, ui.value.openGroup)
        assertTrue(reopened.isEmpty())
    }

    @Test
    fun onlyAReadyOrUpdatingConnectionIsKeptAfterATimeout() {
        assertFalse(telegramReopensAfterTimeout("ConnectionStateReady"))
        assertFalse(telegramReopensAfterTimeout("ConnectionStateUpdating"))
        assertTrue(telegramReopensAfterTimeout("ConnectionStateConnecting"))
        assertTrue(telegramReopensAfterTimeout("ConnectionStateConnectingToProxy"))
        assertTrue(telegramReopensAfterTimeout("ConnectionStateWaitingForNetwork"))
        assertTrue(telegramReopensAfterTimeout(null))
    }

    @Test
    fun anAnswerForAClientThatChangedIsDropped() {
        answer = {
            generation = 2
            TelegramGroupState.Open(release)
        }
        withCheck { it.launchCheck(1, "client", notify = true) }

        assertEquals(TelegramGroupState.Checking, ui.value.openGroup)
        assertTrue(notices.isEmpty())
    }

    @Test
    fun aReturnToTheAppChecksAgainOnlyOnceTheIntervalHasPassed() {
        withCheck { check ->
            check.launchCheck(generation, "client", notify = false).join()
            check.checkOnReturn()
            clockMillis = (TELEGRAM_RECHECK_INTERVAL - 1.minutes).inWholeMilliseconds
            check.checkOnReturn()
            assertEquals(listOf("group 1", "access 2"), loads)

            clockMillis = TELEGRAM_RECHECK_INTERVAL.inWholeMilliseconds
            check.checkOnReturn()
        }

        assertEquals(listOf("group 1", "access 2", "group 1", "access 2"), loads)
    }

    @Test
    fun nothingIsCheckedBeforeTelegramIsReadyOrWithoutAClient() {
        ui.value = TelegramUiState(step = TelegramLoginStep.Starting)
        withCheck { it.checkAgain() }
        ui.value = TelegramUiState(step = TelegramLoginStep.Ready)
        client = null
        withCheck { it.checkAgain() }

        assertTrue(loads.isEmpty())
    }

    @Test
    fun thePremiumEditionChecksOnlyItsOwnGroup() {
        config = config.copy(premiumEdition = true)
        answer = { TelegramGroupState.Open(release) }
        withCheck { it.checkAgain() }

        assertEquals(listOf("group 2"), loads)
        assertEquals(TelegramGroupState.Open(release), ui.value.premiumGroup)
    }
}
