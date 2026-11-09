package name.alexwayfer.customtv.diagnostics

import java.io.File
import java.io.IOException
import java.util.zip.GZIPOutputStream

/**
 * Two streaming passes retain the reference graph and allocation sizes, never runtime values.
 * The first indexes only class layouts and structural name IDs; it does not load the heap into memory.
 * Unknown records, missing layouts and truncated dumps fail closed. No unvalidated output is published.
 */
internal fun sanitizeHeapStructure(input: File, output: File) {
    val index = HeapStructureIndex()
    input.inputStream().buffered().use { HeapHprofReader(HeapHprofWire(it), index).read() }
    if (index.layouts.isEmpty()) throw IOException("Heap has no class layouts")
    var finished = false
    try {
        input.inputStream().buffered().use { source ->
            GZIPOutputStream(output.outputStream().buffered()).buffered().use { sink ->
                HeapHprofReader(HeapHprofWire(source, sink), index, sanitizing = true).read()
            }
        }
        finished = true
    } finally {
        if (!finished) output.delete()
    }
}

internal class HeapStructureIndex {
    val names = mutableSetOf<Long>()
    val layouts = mutableMapOf<Long, HeapClassLayout>()
    private val inheritedTypes = mutableMapOf<Long, IntArray>()

    fun fieldTypes(classId: Long): IntArray = inheritedTypes.getOrPut(classId) {
        val visited = mutableSetOf<Long>()
        val fields = mutableListOf<Int>()
        var current = classId
        while (current != 0L) {
            if (!visited.add(current)) throw IOException("Cyclic heap class hierarchy")
            val layout = layouts[current] ?: throw IOException("Missing heap class layout")
            fields.addAll(layout.types.toList())
            current = layout.parent
        }
        fields.toIntArray()
    }
}

internal class HeapClassLayout(val parent: Long, val types: IntArray)
