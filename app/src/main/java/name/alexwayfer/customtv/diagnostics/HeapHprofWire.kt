package name.alexwayfer.customtv.diagnostics

import java.io.EOFException
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

internal const val HPROF_INT_BYTES = 4
internal const val HPROF_LONG_BYTES = 8
internal const val HPROF_REFERENCE = 2
private const val HPROF_BUFFER_BYTES = 8 * 1024
private const val BYTE_BITS = 8

/** A bounded buffer is reused even for very large arrays; offsets and lengths are unsigned longs. */
internal class HeapHprofWire(private val source: InputStream, private val sink: OutputStream? = null) {
    var position: Long = 0
        private set
    var idSize: Int = HPROF_INT_BYTES
        private set
    private val buffer = ByteArray(HPROF_BUFFER_BYTES)

    fun tag(): Int? {
        val value = source.read()
        if (value < 0) return null
        sink?.write(value)
        position++
        return value
    }

    fun number(size: Int): Long {
        var value = 0L
        repeat(size) { value = (value shl BYTE_BITS) or (tag() ?: throw EOFException()).toLong() }
        return value
    }

    fun id(): Long = number(idSize)

    fun transfer(size: Long, replacement: Int? = null) {
        if (size < 0) throw IOException("Invalid heap record size")
        var remaining = size
        while (remaining > 0) {
            val count = source.read(buffer, 0, minOf(remaining, buffer.size.toLong()).toInt())
            if (count < 0) throw EOFException()
            if (replacement != null) buffer.fill(replacement.toByte(), 0, count)
            sink?.write(buffer, 0, count)
            position += count
            remaining -= count
        }
    }

    fun value(type: Int, sanitize: Boolean) {
        transfer(typeSize(type).toLong(), if (sanitize && type != HPROF_REFERENCE) 0 else null)
    }

    fun typeSize(type: Int): Int = when (type) {
        HPROF_REFERENCE -> idSize
        HPROF_BOOLEAN, HPROF_BYTE -> 1
        HPROF_CHAR, HPROF_SHORT -> 2
        HPROF_FLOAT, HPROF_INT -> HPROF_INT_BYTES
        HPROF_DOUBLE, HPROF_LONG -> HPROF_LONG_BYTES
        else -> throw IOException("Unknown heap value type")
    }

    fun header() {
        val expected = "JAVA PROFILE 1.0.3\u0000"
        for (character in expected) {
            if (tag() != character.code) throw IOException("Unsupported heap header")
        }
        idSize = number(HPROF_INT_BYTES).toInt()
        if (idSize != HPROF_INT_BYTES && idSize != HPROF_LONG_BYTES) throw IOException("Unsupported heap ID size")
        number(HPROF_LONG_BYTES)
    }
}

private const val HPROF_BOOLEAN = 4
private const val HPROF_CHAR = 5
private const val HPROF_FLOAT = 6
private const val HPROF_DOUBLE = 7
private const val HPROF_BYTE = 8
private const val HPROF_SHORT = 9
private const val HPROF_INT = 10
private const val HPROF_LONG = 11
