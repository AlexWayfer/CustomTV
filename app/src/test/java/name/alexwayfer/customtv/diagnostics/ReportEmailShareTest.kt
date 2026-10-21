package name.alexwayfer.customtv.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Test

class ReportEmailShareTest {
    @Test
    fun noEmailAppFallsBackToTheShareSheet() {
        assertEquals(ReportEmailShare.ShareSheet, reportEmailShare(emptyList()))
    }

    @Test
    fun oneEmailAppOpensDirectly() {
        assertEquals(ReportEmailShare.Direct("mail.app"), reportEmailShare(listOf("mail.app")))
    }

    @Test
    fun appWithSeveralMailtoActivitiesCountsOnce() {
        assertEquals(ReportEmailShare.Direct("mail.app"), reportEmailShare(listOf("mail.app", "mail.app")))
    }

    @Test
    fun severalEmailAppsAreOfferedAlone() {
        assertEquals(
            ReportEmailShare.Choose(listOf("mail.app", "other.mail")),
            reportEmailShare(listOf("mail.app", "other.mail", "mail.app")),
        )
    }
}
