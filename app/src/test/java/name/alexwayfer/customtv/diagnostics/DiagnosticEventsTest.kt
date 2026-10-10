package name.alexwayfer.customtv.diagnostics

import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DiagnosticEventsTest {
    private val minute = 60_000L
    private val pixel = DiagnosticDevice(
        "1.0 (1)",
        premium = true,
        "15",
        35,
        "Google",
        "Pixel 8",
        languages = listOf("ru-RU", "en-US"),
        simCountry = "ru",
        debug = false,
        installedAtMillis = minute,
    )

    private fun rejected(code: Int, at: Long, message: String = "") = DiagnosticEvent(
        kind = DiagnosticKind.Rejected,
        source = "StreamStart",
        detail = "helix/streams/followed",
        httpCode = code,
        message = message,
        firstAtMillis = at,
    )

    @Test
    fun aRepeatRaisesTheCountAndKeepsTheFirstTime() {
        val first = recordDiagnosticEvent(emptyList(), rejected(403, 1_000, "old"))
        val events = recordDiagnosticEvent(first, rejected(403, 5_000, "new"))

        assertEquals(1, events.size)
        assertEquals(2, events[0].count)
        assertEquals(1_000L, events[0].firstAtMillis)
        assertEquals(5_000L, events[0].lastAtMillis)
        assertEquals("new", events[0].message)
    }

    @Test
    fun anotherCodeIsAnotherProblemAndTheNewestComesFirst() {
        val events = recordDiagnosticEvent(listOf(rejected(403, 1_000)), rejected(404, 2_000))

        assertEquals(listOf(404, 403), events.map { it.httpCode })
    }

    @Test
    fun theOldestProblemFallsOffPastTheLimit() {
        val full = (1..DIAGNOSTIC_JOURNAL_LIMIT).fold(emptyList<DiagnosticEvent>()) { events, code ->
            recordDiagnosticEvent(events, rejected(400 + code, code.toLong()))
        }
        val events = recordDiagnosticEvent(full, rejected(499, 1_000))

        assertEquals(DIAGNOSTIC_JOURNAL_LIMIT, events.size)
        assertFalse(events.any { it.httpCode == 401 })
        assertEquals(499, events.first().httpCode)
    }

    @Test
    fun aRepeatedProblemIsNotPushedOutByItsOwnRepeats() {
        val events = (1..50).fold(listOf(rejected(404, 0))) { list, index ->
            recordDiagnosticEvent(list, rejected(403, index.toLong()))
        }

        assertEquals(listOf(403, 404), events.map { it.httpCode })
        assertEquals(50, events.first().count)
    }

    @Test
    fun theThirdRateLimitWithinAnHourIsTooOften() {
        val first = rateLimitHit(emptyList(), 0)
        val second = rateLimitHit(first.hits, 10 * minute)
        val third = rateLimitHit(second.hits, 20 * minute)

        assertFalse(first.tooOften)
        assertFalse(second.tooOften)
        assertTrue(third.tooOften)
    }

    @Test
    fun aSteadyRateLimitCountsOncePerRun() {
        val third = (0..2).fold(RateLimitHits(emptyList(), tooOften = false)) { hits, index ->
            rateLimitHit(hits.hits, index * minute)
        }
        val fourth = rateLimitHit(third.hits, 3 * minute)

        assertTrue(third.tooOften)
        assertFalse(fourth.tooOften)
    }

    @Test
    fun rateLimitsOlderThanAnHourDoNotCount() {
        val hits = listOf(0L, 10 * minute)
        val later = rateLimitHit(hits, 61 * minute)

        assertEquals(listOf(10 * minute, 61 * minute), later.hits)
        assertFalse(later.tooOften)
    }

    @Test
    fun aNewProblemIsNoticedAndItsRepeatIsNot() {
        val journal = listOf(rejected(403, 1_000))

        assertTrue(diagnosticNoticeDue(journal, rejected(404, 2_000), sentAtMillis = 0))
        assertFalse(diagnosticNoticeDue(journal, rejected(403, 2_000, "again"), sentAtMillis = 0))
    }

    @Test
    fun aRepeatOfAProblemAlreadySentIsNoticedAgain() {
        val journal = listOf(rejected(403, 1_000))

        assertTrue(diagnosticNoticeDue(journal, rejected(403, 3_000), sentAtMillis = 2_000))
    }

    @Test
    fun aRepeatOfAProblemNotSentYetIsNotNoticedAgain() {
        val journal = listOf(rejected(403, 3_000))

        assertFalse(diagnosticNoticeDue(journal, rejected(403, 4_000), sentAtMillis = 2_000))
    }

    @Test
    fun theNextReportHoldsOnlyWhatHappenedSinceTheLastOne() {
        val sent = rejected(403, 1_000)
        val repeatedAfter = rejected(404, 500).copy(lastAtMillis = 3_000)
        val new = rejected(500, 4_000)

        assertEquals(listOf(repeatedAfter, new), diagnosticEventsToSend(listOf(sent, repeatedAfter, new), sentAtMillis = 2_000))
    }

    @Test
    fun withNothingNewSinceTheLastReportItHoldsTheWholeJournalAgain() {
        val journal = listOf(rejected(403, 1_000), rejected(404, 1_500))

        assertEquals(journal, diagnosticEventsToSend(journal, sentAtMillis = 2_000))
    }

    @Test
    fun aRoutineCodeOrAServerFailureIsNotReported() {
        assertNull(httpDiagnostic(401, HELIX_EXPECTED_CODES))
        assertNull(httpDiagnostic(500, emptySet()))
        assertNull(httpDiagnostic(200, emptySet()))
    }

    @Test
    fun anUnexpected4xxIsARejectionAnd429ARateLimit() {
        assertEquals(DiagnosticKind.Rejected, httpDiagnostic(403, HELIX_EXPECTED_CODES))
        assertEquals(DiagnosticKind.Rejected, httpDiagnostic(401, emptySet()))
        assertEquals(DiagnosticKind.RateLimited, httpDiagnostic(429, HELIX_EXPECTED_CODES))
        assertNull(httpDiagnostic(429, setOf(429)))
    }

    @Test
    fun onlyCrashesAndFreezesAfterTheLastNoticeAreUnseen() {
        val crash = DiagnosticEvent(DiagnosticKind.Crash, "main", "IllegalStateException", firstAtMillis = 5_000)
        val oldFreeze = DiagnosticEvent(DiagnosticKind.Anr, "main", "frame", firstAtMillis = 1_000)
        val events = listOf(crash, oldFreeze, rejected(403, 6_000))

        assertEquals(listOf(crash), unseenExitEvents(events, noticedAtMillis = 2_000))
    }

    @Test
    fun anrTraceKeepsOnlyTheMainThread() {
        val trace = """
            ----- pid 123 at 2026-10-01 -----
            "Signal Catcher" daemon prio=10 tid=2 Runnable
              at dalvik.Other.run(Other.java:1)

            "main" prio=5 tid=1 Blocked
              native: #00 pc 000 libc.so
              at name.alexwayfer.customtv.Slow.run(Slow.kt:10)
              at android.os.Handler.dispatchMessage(Handler.java:99)

            "worker" prio=5 tid=3 Waiting
        """.trimIndent()

        val main = anrMainThread(trace.lineSequence())

        assertEquals(
            listOf(
                "\"main\" prio=5 tid=1 Blocked",
                "  native: #00 pc 000 libc.so",
                "  at name.alexwayfer.customtv.Slow.run(Slow.kt:10)",
                "  at android.os.Handler.dispatchMessage(Handler.java:99)",
            ),
            main.lines(),
        )
        assertEquals("name.alexwayfer.customtv.Slow.run(Slow.kt:10)", topStackFrame(main))
    }

    @Test
    fun anrTraceWithoutAMainThreadKeepsNothing() {
        assertEquals("", anrMainThread("\"worker\" prio=5\n  at a.b(c)".lineSequence()))
        assertNull(topStackFrame(""))
    }

    @Test
    fun reportNamesTheBuildTheAccountsAndEachProblem() {
        val report = diagnosticReport(
            device = pixel,
            accounts = DiagnosticAccounts("viewer", "42", "viewer_tg"),
            events = listOf(rejected(403, 0, "Missing scope").copy(lastAtMillis = 2 * minute, count = 3)),
            zone = ZoneOffset.UTC,
        )

        assertEquals(
            """
            CustomTV problem report
            App: 1.0 (1) premium
            Installed: 1970-01-01T00:01:00Z
            Android: 15 (API 35), Google Pixel 8
            Languages: ru-RU, en-US
            SIM country: RU
            Twitch: viewer, id 42
            Telegram: @viewer_tg

            StreamStart: HTTP 403 helix/streams/followed
            1970-01-01T00:02:00Z, 3 times since 1970-01-01T00:00:00Z
            Missing scope

            """.trimIndent(),
            report,
        )
    }

    @Test
    fun recentLogKeepsTheLatestLinesOldestFirst() {
        val log = RecentLog()
        (1..RECENT_LOG_LINES + 2).forEach { log.add("line $it") }

        val lines = log.lines()
        assertEquals(RECENT_LOG_LINES, lines.size)
        assertEquals("line 3", lines.first())
        assertEquals("line ${RECENT_LOG_LINES + 2}", lines.last())
    }

    @Test
    fun recentLogLineNamesTimeLevelAndTag() {
        assertEquals(
            "00:01:02.345 W/StreamStart: check rate limited",
            recentLogLine(62_345, ZoneOffset.UTC, 'W', "StreamStart", "check rate limited"),
        )
    }

    @Test
    fun aRepeatKeepsTheLogBeforeItsLatestTime() {
        val first = recordDiagnosticEvent(emptyList(), rejected(403, 1_000).copy(recentLog = listOf("old")))
        val events = recordDiagnosticEvent(first, rejected(403, 2_000).copy(recentLog = listOf("new")))

        assertEquals(listOf("new"), events.single().recentLog)
    }

    @Test
    fun reportListsTheLogUnderTheProblem() {
        val report = diagnosticReport(
            device = pixel,
            accounts = DiagnosticAccounts(null, null, null),
            events = listOf(rejected(404, 0, "Not Found").copy(recentLog = listOf("a", "b"))),
            zone = ZoneOffset.UTC,
        )

        assertTrue(report.endsWith("Not Found\nLog before it:\n  a\n  b\n"))
    }

    @Test
    fun reportFromTheSettingsEndsWithTheAppLogEvenWithoutProblems() {
        val report = diagnosticReport(
            device = pixel,
            accounts = DiagnosticAccounts(null, null, null),
            events = emptyList(),
            zone = ZoneOffset.UTC,
            appLog = listOf("one", "two"),
        )

        assertTrue(report.endsWith("Telegram: -\n\nApp log:\n  one\n  two\n"))
    }

    @Test
    fun reportWithoutAppLogHasNoAppLogSection() {
        val report = diagnosticReport(
            device = pixel,
            accounts = DiagnosticAccounts(null, null, null),
            events = emptyList(),
            zone = ZoneOffset.UTC,
        )

        assertFalse(report.contains("App log:"))
    }

    @Test
    fun reportWithoutAccountsSaysSo() {
        val report = diagnosticReport(
            device = DiagnosticDevice(
                "1.0 (1)",
                premium = false,
                "9",
                28,
                "Acme",
                "One",
                languages = emptyList(),
                simCountry = "",
                debug = false,
                installedAtMillis = null,
            ),
            accounts = DiagnosticAccounts(null, null, null),
            events = emptyList(),
            zone = ZoneOffset.UTC,
        )

        assertTrue(report.contains("App: 1.0 (1) free\n"))
        assertTrue(report.contains("Twitch: not logged in\n"))
        assertTrue(report.contains("Telegram: -\n"))
        assertFalse(report.contains("Installed:"))
        assertTrue(report.contains("Languages: -\nSIM country: -\n"))
    }

    @Test
    fun shareTextNamesTheBuildAndCountsTheProblemsWithoutListingThem() {
        assertEquals("CustomTV 1.0 (1) premium debug\nProblems: 4", diagnosticShareText(pixel.copy(debug = true), 4))
    }

    @Test
    fun reportFileIsNamedByItsUtcTimeWithoutColons() {
        assertEquals("customtv-report_1970-01-01_00-02-05Z.txt", diagnosticReportFileName(2 * minute + 5_000))
    }

    @Test
    fun aLaterStudioDeployIsTheInstallTime() {
        assertEquals(5_000L, diagnosticInstalledAt(packageUpdatedMillis = 1_000, studioOverlayMillis = 5_000)!!)
    }

    @Test
    fun withoutAStudioDeployThePackageUpdateIsTheInstallTime() {
        assertEquals(1_000L, diagnosticInstalledAt(packageUpdatedMillis = 1_000, studioOverlayMillis = 0)!!)
    }

    @Test
    fun anOverlayOlderThanAReinstallIsIgnored() {
        assertEquals(5_000L, diagnosticInstalledAt(packageUpdatedMillis = 5_000, studioOverlayMillis = 1_000)!!)
    }

    @Test
    fun shareTextWithoutProblemsNamesOnlyTheBuild() {
        assertEquals("CustomTV 1.0 (1) premium", diagnosticShareText(pixel, 0))
    }

    @Test
    fun reportMarksADebugBuild() {
        val report = diagnosticReport(
            device = pixel.copy(debug = true),
            accounts = DiagnosticAccounts(null, null, null),
            events = emptyList(),
            zone = ZoneOffset.UTC,
        )

        assertTrue(report.contains("App: 1.0 (1) premium debug\nInstalled: "))
    }

    @Test
    fun journalSurvivesEncodingAndSkipsUnknownKinds() {
        val events = listOf(
            rejected(404, 1_000, "Not Found").copy(lastAtMillis = 3_000, count = 2, recentLog = listOf("a")),
            DiagnosticEvent(DiagnosticKind.Crash, "main", "Boom", message = "stack", firstAtMillis = 500),
        )

        assertEquals(events, decodeDiagnosticEvents(encodeDiagnosticEvents(events)))
        assertEquals(emptyList<DiagnosticEvent>(), decodeDiagnosticEvents("""[{"kind":"Other"}]"""))
        assertEquals(emptyList<DiagnosticEvent>(), decodeDiagnosticEvents("not json"))
    }
}
