package name.alexwayfer.customtv.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChannelPanelsTest {
    @Test
    fun panelsKeepTheirTitleImageLinkAndText() {
        val panels = parseChannelPanels(
            """
            {"data":{"user":{"panels":[{"id":"1","type":"DEFAULT","title":"Rules","imageURL":"https://panels/a.png",
            "linkURL":"https://x.com/ada","description":" Be **nice** "}]}}}
            """.trimIndent(),
        )!!
        assertEquals(
            listOf(ChannelPanel("1", "Rules", "https://panels/a.png", "https://x.com/ada", "Be **nice**")),
            panels.panels,
        )
        assertEquals(0, panels.extensionCount)
    }

    @Test
    fun extensionsAreCountedButNotShown() {
        val panels = parseChannelPanels(
            """{"data":{"user":{"panels":[{"id":"1","type":"EXTENSION"},{"id":"2","type":"EXTENSION"}]}}}""",
        )!!
        assertEquals(emptyList<ChannelPanel>(), panels.panels)
        assertEquals(2, panels.extensionCount)
    }

    @Test
    fun aPanelWithNothingToShowIsSkipped() {
        val panels = parseChannelPanels(
            """
            {"data":{"user":{"panels":[{"id":"1","type":"DEFAULT","title":null,"imageURL":null,"linkURL":null,
            "description":null}]}}}
            """.trimIndent(),
        )!!
        assertEquals(emptyList<ChannelPanel>(), panels.panels)
    }

    @Test
    fun aChannelWithoutPanelsHasAnEmptyList() {
        val panels = parseChannelPanels("""{"data":{"user":{"panels":[]}}}""")!!
        assertEquals(ChannelPanels(emptyList(), 0), panels)
    }

    @Test
    fun panelsTwitchLeftOutAreNull() {
        assertNull(parseChannelPanels("""{"data":{"user":{"panels":null}},"errors":[{"message":"failed"}]}"""))
        assertNull(parseChannelPanels("""{"data":{"user":{}}}"""))
    }

    @Test
    fun anAnswerWithoutTheUserIsNull() {
        assertNull(parseChannelPanels("""{"data":{"user":null}}"""))
    }
}
