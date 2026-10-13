package name.alexwayfer.customtv.data

import android.content.Context

internal class ChatterLabelsCredentialsStore(context: Context) {
    private val file = EncryptedTextFile(context, "chatter_labels_credentials", keyAlias = "customtv_chatter_labels")

    fun read(): ChatterLabelsCredentials? = try {
        file.read()?.let { plain ->
            val separator = plain.indexOf('\n')
            if (separator <= 0) null else ChatterLabelsCredentials(
                gistId = plain.substring(0, separator),
                token = plain.substring(separator + 1),
            )
        }
    } catch (_: Exception) {
        null
    }

    fun write(credentials: ChatterLabelsCredentials) {
        file.write("${credentials.gistId}\n${credentials.token}")
    }

    fun clear() {
        file.delete()
    }
}
