package name.alexwayfer.customtv.ui.watch

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.semantics.SemanticsProperties
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.chat.chatMentionName
import name.alexwayfer.customtv.data.ChatModerator
import name.alexwayfer.customtv.ui.ComposeHost
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class ChatModeratorsTouchTest {
    @get:Rule(order = 0)
    val host = ComposeHost()

    @get:Rule(order = 1)
    val compose = createComposeRule()

    private val app = RuntimeEnvironment.getApplication()

    @Test
    fun selectingALocalizedModeratorMentionsTheirLogin() {
        var mention: String? = null
        compose.setContent {
            ChatModeratorsContent(listOf(ChatModerator("1", "alice", "Алиса", null))) {
                mention = chatMentionName(it.login, it.displayName)
            }
        }

        assertNull(mention)
        compose.onNodeWithText("Алиса").performClick()
        assertEquals("alice", mention)
    }

    @Test
    fun presenceSectionsKeepUnknownModeratorsSeparate() {
        compose.setContent {
            ChatModeratorsContent(
                listOf(
                    ChatModerator("1", "alice", "Alice", true),
                    ChatModerator("2", "bob", "Bob", false),
                    ChatModerator("3", "carol", "Carol", null),
                ),
                onPick = {},
            )
        }

        compose.onNode(
            SemanticsMatcher.expectValue(
                SemanticsProperties.StateDescription, app.getString(R.string.chat_moderator_in_chat),
            ),
        ).assertIsDisplayed()
        compose.onNode(
            SemanticsMatcher.expectValue(
                SemanticsProperties.StateDescription, app.getString(R.string.chat_moderator_not_in_chat),
            ),
        ).assertIsDisplayed()
        compose.onNodeWithText(
            app.resources.getQuantityString(R.plurals.chat_moderators_in_chat_count, 1, 1),
        ).assertIsDisplayed()
        compose.onNodeWithText(
            app.resources.getQuantityString(R.plurals.chat_moderators_not_in_chat_count, 1, 1),
        ).assertIsDisplayed()
        compose.onNodeWithText(
            app.resources.getQuantityString(R.plurals.chat_moderators_presence_unknown_count, 1, 1),
        ).assertIsDisplayed()
        compose.onNodeWithText("Carol").assertIsDisplayed()
    }

    @Test
    fun aPublicListHasNoPresenceHeadings() {
        compose.setContent {
            ChatModeratorsContent(listOf(ChatModerator("1", "alice", "Alice", null)), onPick = {})
        }

        compose.onNodeWithText("Alice").assertIsDisplayed()
        compose.onNodeWithText(
            app.resources.getQuantityString(R.plurals.chat_moderators_in_chat_count, 1, 1),
        ).assertDoesNotExist()
        compose.onNodeWithText(
            app.resources.getQuantityString(R.plurals.chat_moderators_not_in_chat_count, 1, 1),
        ).assertDoesNotExist()
        compose.onNodeWithText(
            app.resources.getQuantityString(R.plurals.chat_moderators_presence_unknown_count, 1, 1),
        ).assertDoesNotExist()
    }

    @Test
    fun anEmptyListSaysThereAreNoModerators() {
        compose.setContent { ChatModeratorsContent(emptyList(), onPick = {}) }

        compose.onNodeWithText(app.getString(R.string.chat_moderators_empty)).assertIsDisplayed()
    }
}
