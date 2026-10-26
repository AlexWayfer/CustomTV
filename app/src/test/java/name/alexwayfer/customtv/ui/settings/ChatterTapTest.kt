package name.alexwayfer.customtv.ui.settings

import name.alexwayfer.customtv.data.ChatterLabelAssignee
import name.alexwayfer.customtv.ui.components.ChannelPreview
import org.junit.Assert.assertEquals
import org.junit.Test

class ChatterTapTest {
    private val withLogin = ChatterLabelAssignee("1", login = "anna", displayName = "Anna", assignedAtMillis = null)
    private val withoutLogin = ChatterLabelAssignee("2", login = "", displayName = "Gone", assignedAtMillis = null)

    private fun tap(
        assignee: ChatterLabelAssignee,
        namesLoading: Boolean = false,
        banned: Set<String> = emptySet(),
        notFound: Set<String> = emptySet(),
    ): ChatterTap = chatterTap(assignee, namesLoading, banned, notFound)

    @Test
    fun `a chatter with a login opens the profile under their name even while the names load`() {
        assertEquals(ChatterTap.Open(ChannelPreview("anna", "Anna")), tap(withLogin, namesLoading = true))
    }

    @Test
    fun `a banned chatter tells so even with a login`() {
        assertEquals(ChatterTap.Banned, tap(withLogin, banned = setOf("1")))
    }

    @Test
    fun `a chatter Twitch does not know tells so`() {
        assertEquals(ChatterTap.NotFound, tap(withoutLogin, notFound = setOf("2")))
    }

    @Test
    fun `a chatter without a login while the names load tells to wait`() {
        assertEquals(ChatterTap.Loading, tap(withoutLogin, namesLoading = true))
    }

    @Test
    fun `a chatter whose login did not load tells it failed`() {
        assertEquals(ChatterTap.LoadFailed, tap(withoutLogin))
    }
}
