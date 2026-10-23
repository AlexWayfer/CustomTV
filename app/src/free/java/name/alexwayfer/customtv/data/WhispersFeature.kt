package name.alexwayfer.customtv.data

import android.app.Application
import android.content.Context
import android.content.Intent

/** Whispers are a Premium feature; the free build starts nothing. */
internal object WhispersFeature {
    fun start(application: Application) = Unit
}

/** A tapped whisper notification only exists in Premium. */
internal object WhisperOpenRequest {
    fun offer(context: Context, intent: Intent?) = Unit
}
