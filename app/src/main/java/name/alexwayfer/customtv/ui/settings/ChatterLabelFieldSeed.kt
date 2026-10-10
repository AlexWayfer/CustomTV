package name.alexwayfer.customtv.ui.settings

internal data class ChatterLabelFieldSeed(val token: String, val gistId: String)

internal fun chatterLabelFieldSeed(token: String?, gistId: String?): ChatterLabelFieldSeed =
    ChatterLabelFieldSeed(token = token.orEmpty(), gistId = gistId.orEmpty())
