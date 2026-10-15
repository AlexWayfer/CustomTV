package name.alexwayfer.customtv.ui.account

import com.mikepenz.markdown.model.State
import com.mikepenz.markdown.model.parseMarkdown
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import name.alexwayfer.customtv.data.ChannelPanels

/**
 * About panel texts parsed once for the rest of the run, as [name.alexwayfer.customtv.data.ChannelPanelsRepository]
 * keeps the panels. A panel then shows its text in its first frame every time the tab opens, instead of parsing it
 * again and growing a moment later.
 */
internal object PanelMarkdown {
    private val parsed = ConcurrentHashMap<String, State>()

    /** [text] as parsed before, or parsed now on the calling thread when it was not. */
    fun parsed(text: String): State = parsed.getOrPut(text) { parseMarkdown(text) }

    /** Parses the texts of [panels] off the main thread, so the tab finds them ready. */
    suspend fun parse(panels: ChannelPanels) = withContext(Dispatchers.Default) {
        panels.panels.forEach { panel -> panel.description?.let(::parsed) }
    }
}
