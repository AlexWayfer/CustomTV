package name.alexwayfer.customtv.ui.watch

import android.app.Application
import androidx.compose.foundation.clickable
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.ui.ComposeHost
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class ChatMenuSubpageTouchTest {
    @get:Rule(order = 0)
    val host = ComposeHost()

    @get:Rule(order = 1)
    val compose = createComposeRule()

    private val app = RuntimeEnvironment.getApplication()

    private val page = ChatMenuSubpage(R.string.timestamps) { Text("Panel") }

    private fun showMenu(initialSubpage: ChatMenuSubpage?) {
        compose.setContent {
            ChatMenuPages(maxHeight = 400.dp, initialSubpage = initialSubpage) { openSubpage ->
                Text(
                    text = "Row",
                    modifier = Modifier.clickable { openSubpage(page) },
                )
            }
        }
    }

    @Test
    fun rowOpensItsPageInPlaceOfTheRows() {
        showMenu(initialSubpage = null)
        compose.onNodeWithText("Row").performClick()

        compose.onNodeWithText(app.getString(R.string.timestamps)).assertIsDisplayed()
        compose.onNodeWithText("Panel").assertIsDisplayed()
        compose.onNodeWithText("Row").assertDoesNotExist()
    }

    @Test
    fun arrowGoesBackToTheRows() {
        showMenu(initialSubpage = null)
        compose.onNodeWithText("Row").performClick()
        compose.onNodeWithContentDescription(app.getString(R.string.navigate_back)).performClick()

        compose.onNodeWithText("Row").assertIsDisplayed()
        compose.onNodeWithText("Panel").assertDoesNotExist()
    }

    @Test
    fun aMenuOpenedAtAPageShowsItAndItsArrowLeadsToTheRows() {
        showMenu(initialSubpage = page)

        compose.onNodeWithText("Panel").assertIsDisplayed()
        compose.onNodeWithText("Row").assertDoesNotExist()

        compose.onNodeWithContentDescription(app.getString(R.string.navigate_back)).performClick()
        compose.onNodeWithText("Row").assertIsDisplayed()
    }
}
