package name.alexwayfer.customtv.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Test

class DiagnosticPrivateItemTest {
    @Test
    fun loggedInAccountsComeFirstByTheirNames() {
        val items = diagnosticPrivateItems(DiagnosticAccounts("viewer", "123", "viewer_tg"))

        assertEquals(
            listOf(
                DiagnosticPrivateItem.TwitchAccount("viewer"),
                DiagnosticPrivateItem.TelegramAccount("viewer_tg"),
                DiagnosticPrivateItem.Viewing,
                DiagnosticPrivateItem.Device,
            ),
            items,
        )
    }

    @Test
    fun twitchAccountWithoutLoginIsNamedByItsId() {
        val items = diagnosticPrivateItems(DiagnosticAccounts(twitchLogin = "", twitchUserId = "123", null))

        assertEquals(DiagnosticPrivateItem.TwitchAccount("123"), items.first())
    }

    @Test
    fun withoutAccountsOnlyViewingAndDeviceAreListed() {
        val items = diagnosticPrivateItems(DiagnosticAccounts(null, null, " "))

        assertEquals(listOf(DiagnosticPrivateItem.Viewing, DiagnosticPrivateItem.Device), items)
    }
}
