package name.alexwayfer.customtv.ui.watch

import android.app.Application
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.chat.ChannelShoutout
import name.alexwayfer.customtv.chat.DEFAULT_CHAT_TEXT_SIZE
import name.alexwayfer.customtv.data.ChannelFollowStatus
import name.alexwayfer.customtv.ui.ComposeHost
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
// The plain application: the app's own starts its stores and workers, which a plaque does not need.
@Config(application = Application::class)
class ChatShoutoutBannerTouchTest {
    @get:Rule(order = 0)
    val host = ComposeHost()

    @get:Rule(order = 1)
    val compose = createComposeRule()

    private val app = RuntimeEnvironment.getApplication()
    private val events = mutableListOf<String>()

    private val shoutout = ChannelShoutout(
        id = "shoutout-1",
        channelId = "100",
        targetId = "200",
        targetLogin = "target",
        targetDisplayName = "Target",
        targetAvatarUrl = "file:///missing/target.png",
        sourceLogin = "helper",
        receivedAtMillis = 0L,
    )

    private fun showBanner(status: ChannelFollowStatus, sourceName: String? = "Helper") {
        compose.setContent {
            val action = rememberChannelFollowAction(
                channel = shoutout.targetLogin,
                channelId = shoutout.targetId,
                channelName = shoutout.targetDisplayName,
                signedIn = false,
                following = false,
                onFollowChanged = {},
                onLogIn = { events += "log in" },
            )
            ChatShoutoutBanner(
                shoutout = shoutout,
                sourceName = sourceName,
                follow = ShoutoutFollow(status, action),
                chatTextSize = DEFAULT_CHAT_TEXT_SIZE,
                onOpenCard = { events += "card" },
                onHide = { events += "hide" },
                bottomInset = 0.dp,
            )
        }
    }

    @Test
    fun aTapOnThePlaqueOpensTheChannelsCard() {
        showBanner(ChannelFollowStatus.NotFollowing)

        compose.onNodeWithContentDescription("Helper gives a shoutout to Target").performClick()

        assertEquals(listOf("card"), events)
    }

    @Test
    fun theHideButtonHidesThePlaque() {
        showBanner(ChannelFollowStatus.NotFollowing)

        compose.onNodeWithContentDescription(app.getString(R.string.chat_shoutout_hide)).performClick()

        assertEquals(listOf("hide"), events)
    }

    @Test
    fun theHeartWaitsForTheFollowToBeKnown() {
        showBanner(ChannelFollowStatus.Unknown)

        compose.onAllNodesWithContentDescription(app.getString(R.string.channel_follow)).assertCountEquals(0)
    }

    @Test
    fun theHeartOfAChannelNotFollowedAsksToLogInWhenSignedOut() {
        showBanner(ChannelFollowStatus.NotFollowing)

        compose.onNodeWithContentDescription(app.getString(R.string.channel_follow)).performClick()

        assertEquals(listOf("log in"), events)
    }

    @Test
    fun aShoutoutWithoutItsSourceNamesOnlyTheChannel() {
        showBanner(ChannelFollowStatus.OwnChannel, sourceName = null)

        compose.onNodeWithContentDescription("Shoutout to Target").performClick()

        assertEquals(listOf("card"), events)
        compose.onAllNodesWithContentDescription(app.getString(R.string.channel_follow)).assertCountEquals(0)
    }
}
