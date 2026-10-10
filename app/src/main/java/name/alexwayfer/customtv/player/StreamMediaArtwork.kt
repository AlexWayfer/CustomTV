package name.alexwayfer.customtv.player

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Shader
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toBitmap
import coil.imageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.CancellationException
import java.io.ByteArrayOutputStream
import kotlin.math.min
import kotlin.time.Duration.Companion.minutes

internal fun streamMediaTitle(login: String, displayName: String?): String {
    return displayName?.trim()?.takeIf { it.isNotEmpty() } ?: login
}

internal fun streamMediaArtist(appName: String, categoryName: String?): String {
    return categoryName?.trim()?.takeIf { it.isNotEmpty() } ?: appName
}

internal object StreamMediaArtwork {
    const val WIDTH = 640
    const val HEIGHT = 360
    val PREVIEW_MAX_AGE = 5.minutes

    fun previewUrl(login: String, fetchedAt: Long? = null): String {
        val user = login.trim().lowercase()
        val base = "https://static-cdn.jtvnw.net/previews-ttv/live_user_$user-${WIDTH}x${HEIGHT}.jpg"
        return if (fetchedAt == null) base else "$base?t=$fetchedAt"
    }

    /** The picture at [imageUrl], or the channel's avatar on a dark ground when that fails. */
    suspend fun loadJpeg(context: Context, imageUrl: String?, avatarUrl: String?): ByteArray? {
        imageUrl?.let { decodeBitmap(context, it) }?.let { return it.toJpeg() }
        val avatar = avatarUrl?.let { decodeBitmap(context, it) } ?: return null
        return artworkFromAvatar(avatar).toJpeg()
    }

    private suspend fun decodeBitmap(context: Context, url: String): Bitmap? {
        val result = try {
            context.imageLoader.execute(
                ImageRequest.Builder(context)
                    .data(url)
                    .allowHardware(false)
                    .memoryCachePolicy(CachePolicy.DISABLED)
                    .diskCachePolicy(CachePolicy.DISABLED)
                    .build(),
            )
        } catch (error: CancellationException) {
            throw error
        } catch (_: Throwable) {
            return null
        }
        val bitmap = (result as? SuccessResult)?.drawable?.toBitmap() ?: return null
        return bitmap.takeIf { it.width > 1 && it.height > 1 }
    }

    private fun artworkFromAvatar(avatar: Bitmap): Bitmap {
        val output = createBitmap(WIDTH, HEIGHT)
        val canvas = Canvas(output)
        canvas.drawColor(BACKGROUND_COLOR)
        val diameter = HEIGHT * 0.62f
        val scale = diameter / min(avatar.width, avatar.height).toFloat()
        val shader = BitmapShader(avatar, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        shader.setLocalMatrix(
            Matrix().apply {
                setScale(scale, scale)
                postTranslate(
                    (WIDTH - avatar.width * scale) / 2f,
                    (HEIGHT - avatar.height * scale) / 2f,
                )
            },
        )
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.shader = shader }
        canvas.drawCircle(WIDTH / 2f, HEIGHT / 2f, diameter / 2f, paint)
        return output
    }

    private fun Bitmap.toJpeg(quality: Int = 85): ByteArray {
        val out = ByteArrayOutputStream()
        compress(Bitmap.CompressFormat.JPEG, quality, out)
        return out.toByteArray()
    }

    private const val BACKGROUND_COLOR = 0xFF18181B.toInt()
}
