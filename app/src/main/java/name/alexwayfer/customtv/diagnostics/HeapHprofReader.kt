package name.alexwayfer.customtv.diagnostics

import java.io.IOException

/** Android HPROF 1.0.3. Only names referenced by class layouts, class loads and heap names are kept. */
internal class HeapHprofReader(
    private val wire: HeapHprofWire,
    private val index: HeapStructureIndex,
    private val sanitizing: Boolean = false,
) {
    fun read() {
        wire.header()
        while (true) {
            val tag = wire.tag() ?: break
            wire.number(HPROF_INT_BYTES)
            val length = wire.number(HPROF_INT_BYTES)
            val end = wire.position + length
            record(tag, length)
            if (wire.position != end) throw IOException("Heap record length mismatch")
        }
    }

    private fun record(tag: Int, length: Long) {
        when (tag) {
            UTF8 -> {
                val id = wire.id()
                wire.transfer(length - wire.idSize, if (sanitizing && id !in index.names) '?'.code else null)
            }
            LOAD_CLASS -> {
                wire.number(HPROF_INT_BYTES)
                wire.id()
                wire.number(HPROF_INT_BYTES)
                index.names.add(wire.id())
            }
            HEAP_DUMP, HEAP_SEGMENT -> heap(wire.position + length)
            REDACTED_EXTENSION -> {
                // Android 17 adds an opaque eight-byte header record. None of its payload is shared.
                if (length != HPROF_LONG_BYTES.toLong()) throw IOException("Unknown heap extension layout")
                wire.transfer(length, if (sanitizing) 0 else null)
            }
            STACK_FRAME, STACK_TRACE, START_THREAD, END_THREAD, HEAP_END -> wire.transfer(length)
            else -> throw IOException("Unknown heap record")
        }
    }

    private fun heap(end: Long) {
        while (wire.position < end) {
            when (wire.tag()) {
                CLASS -> classLayout()
                INSTANCE -> instance()
                OBJECT_ARRAY -> objectArray()
                PRIMITIVE_ARRAY -> primitiveArray(hasData = true)
                PRIMITIVE_ARRAY_NODATA -> primitiveArray(hasData = false)
                HEAP_INFO -> { wire.number(HPROF_INT_BYTES); index.names.add(wire.id()) }
                ROOT_UNKNOWN, ROOT_STICKY, ROOT_MONITOR, ROOT_INTERNED, ROOT_FINALIZING,
                ROOT_DEBUGGER, ROOT_REFERENCE, ROOT_VM, ROOT_UNREACHABLE -> wire.id()
                ROOT_JNI_GLOBAL -> wire.transfer(wire.idSize * 2L)
                ROOT_JNI_LOCAL, ROOT_JAVA_FRAME, ROOT_THREAD_OBJECT, ROOT_JNI_MONITOR ->
                    wire.transfer(wire.idSize + HPROF_INT_BYTES * 2L)
                ROOT_NATIVE_STACK, ROOT_THREAD_BLOCK -> wire.transfer(wire.idSize + HPROF_INT_BYTES.toLong())
                else -> throw IOException("Unknown heap subrecord")
            }
        }
    }

    private fun classLayout() {
        val id = wire.id()
        wire.number(HPROF_INT_BYTES)
        val parent = wire.id()
        wire.transfer(wire.idSize * CLASS_OTHER_IDS.toLong() + HPROF_INT_BYTES)
        repeat(wire.number(2).toInt()) {
            wire.number(2)
            wire.value(wire.number(1).toInt(), sanitizing)
        }
        repeat(wire.number(2).toInt()) {
            index.names.add(wire.id())
            wire.value(wire.number(1).toInt(), sanitizing)
        }
        val fields = IntArray(wire.number(2).toInt()) {
            index.names.add(wire.id())
            wire.number(1).toInt().also { type -> wire.typeSize(type) }
        }
        if (!sanitizing) index.layouts[id] = HeapClassLayout(parent, fields)
    }

    private fun instance() {
        wire.id()
        wire.number(HPROF_INT_BYTES)
        val classId = wire.id()
        val length = wire.number(HPROF_INT_BYTES)
        if (sanitizing) {
            val start = wire.position
            for (type in index.fieldTypes(classId)) wire.value(type, sanitize = true)
            if (wire.position - start != length) throw IOException("Heap instance layout mismatch")
        } else {
            wire.transfer(length)
        }
    }

    private fun objectArray() {
        wire.id()
        wire.number(HPROF_INT_BYTES)
        val count = wire.number(HPROF_INT_BYTES)
        wire.id()
        wire.transfer(count * wire.idSize)
    }

    private fun primitiveArray(hasData: Boolean) {
        wire.id()
        wire.number(HPROF_INT_BYTES)
        val count = wire.number(HPROF_INT_BYTES)
        val type = wire.number(1).toInt()
        if (type == HPROF_REFERENCE) throw IOException("Reference in primitive heap array")
        val width = wire.typeSize(type)
        if (hasData) wire.transfer(count * width, if (sanitizing) 0 else null)
    }
}

private const val UTF8 = 0x01
private const val LOAD_CLASS = 0x02
private const val STACK_FRAME = 0x04
private const val STACK_TRACE = 0x05
private const val START_THREAD = 0x0a
private const val END_THREAD = 0x0b
private const val HEAP_DUMP = 0x0c
private const val HEAP_SEGMENT = 0x1c
private const val HEAP_END = 0x2c
private const val REDACTED_EXTENSION = 0xa0
private const val CLASS = 0x20
private const val INSTANCE = 0x21
private const val OBJECT_ARRAY = 0x22
private const val PRIMITIVE_ARRAY = 0x23
private const val PRIMITIVE_ARRAY_NODATA = 0xc3
private const val HEAP_INFO = 0xfe
private const val CLASS_OTHER_IDS = 5
private const val ROOT_UNKNOWN = 0xff
private const val ROOT_JNI_GLOBAL = 0x01
private const val ROOT_JNI_LOCAL = 0x02
private const val ROOT_JAVA_FRAME = 0x03
private const val ROOT_NATIVE_STACK = 0x04
private const val ROOT_STICKY = 0x05
private const val ROOT_THREAD_BLOCK = 0x06
private const val ROOT_MONITOR = 0x07
private const val ROOT_THREAD_OBJECT = 0x08
private const val ROOT_INTERNED = 0x89
private const val ROOT_FINALIZING = 0x8a
private const val ROOT_DEBUGGER = 0x8b
private const val ROOT_REFERENCE = 0x8c
private const val ROOT_VM = 0x8d
private const val ROOT_JNI_MONITOR = 0x8e
private const val ROOT_UNREACHABLE = 0x90
