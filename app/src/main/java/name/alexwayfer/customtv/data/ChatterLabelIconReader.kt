package name.alexwayfer.customtv.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import java.io.ByteArrayOutputStream

/**
 * The picked image as a label icon: resized the way the extension does it and encoded as a PNG data URL.
 * Throws an `IOException` when the image cannot be read.
 */
internal fun readChatterLabelIcon(context: Context, uri: Uri): String {
    val source = ImageDecoder.createSource(context.contentResolver, uri)
    val bitmap = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
        val size = chatterLabelIconSize(info.size.width, info.size.height)
        decoder.setTargetSize(size.width, size.height)
        // A hardware bitmap cannot be read back to encode it.
        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
    }
    val png = ByteArrayOutputStream().use { output ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        output.toByteArray()
    }
    bitmap.recycle()
    return chatterLabelIconDataUrl(png)
}
