package name.alexwayfer.customtv.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Test

class DiagnosticPrivateItemTest {
    @Test
    fun loggedInAccountsComeFirstByTheirNames() {
        val items = diagnosticPrivateItems(DiagnosticAccounts("viewer", "123", "viewer_tg", 7L))

        assertEquals(
            listOf(
                DiagnosticPrivateItem.TwitchAccount("viewer"),
                DiagnosticPrivateItem.TelegramAccount("@viewer_tg"),
                DiagnosticPrivateItem.Viewing,
                DiagnosticPrivateItem.Device,
            ),
            items,
        )
    }

    @Test
    fun twitchAccountWithoutLoginIsNamedByItsId() {
        val items = diagnosticPrivateItems(DiagnosticAccounts(twitchLogin = "", twitchUserId = "123", null, null))

        assertEquals(DiagnosticPrivateItem.TwitchAccount("123"), items.first())
    }

    @Test
    fun telegramAccountWithoutUsernameIsNamedByItsId() {
        val items = diagnosticPrivateItems(DiagnosticAccounts(null, null, " ", 7L))

        assertEquals(DiagnosticPrivateItem.TelegramAccount("7"), items.first())
    }

    @Test
    fun withoutAccountsOnlyViewingAndDeviceAreListed() {
        val items = diagnosticPrivateItems(DiagnosticAccounts(null, null, " ", null))

        assertEquals(listOf(DiagnosticPrivateItem.Viewing, DiagnosticPrivateItem.Device), items)
    }
}
