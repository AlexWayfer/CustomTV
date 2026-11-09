package name.alexwayfer.customtv.diagnostics

import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.IOException
import java.util.zip.GZIPInputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class HeapStructureTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test
    fun `only structural names references and sizes survive for both Android ID sizes`() {
        for (idSize in listOf(4, 8)) {
            val fixture = HeapStructureFixture(idSize)
            val input = temporary.newFile()
            val output = temporary.newFile()
            input.writeBytes(fixture.bytes)
            sanitizeHeapStructure(input, output)
            val sanitized = GZIPInputStream(output.inputStream()).use { it.readBytes() }
            assertArrayEquals(fixture.expected, sanitized)
            assertFalse(sanitized.toString(Charsets.ISO_8859_1).contains("private-token"))
        }
    }

    @Test
    fun `truncated input never produces a successful sanitized snapshot`() {
        val input = temporary.newFile()
        val output = temporary.newFile()
        input.writeBytes(HeapStructureFixture(4).bytes.dropLast(2).toByteArray())
        assertThrows(IOException::class.java) { sanitizeHeapStructure(input, output) }
    }

    @Test
    fun `unknown records fail closed instead of copying potentially private payloads`() {
        val fixture = HeapStructureFixture(4)
        val input = temporary.newFile()
        val output = temporary.newFile()
        input.writeBytes(fixture.bytes + byteArrayOf(0x77, 0, 0, 0, 0, 0, 0, 0, 1, 42))
        assertThrows(IOException::class.java) { sanitizeHeapStructure(input, output) }
    }

}

/** The expected bytes are authored with the fixture, independently of the sanitizer's parser. */
internal class HeapStructureFixture(private val idSize: Int) {
    private val source = ByteArrayOutputStream()
    private val output = DataOutputStream(source)
    private val erased = mutableListOf<Pair<Int, Int>>()
    private val masked = mutableListOf<Pair<Int, Int>>()
    val bytes: ByteArray
    val expected: ByteArray

    init {
        output.writeBytes("JAVA PROFILE 1.0.3\u0000")
        output.writeInt(idSize)
        output.writeLong(123)
        record(0xa0) { secret(8) }
        string(1, "example.Owner")
        string(2, "reference")
        string(3, "primitive")
        string(4, "private-token", sensitive = true)
        string(5, "example.Base")
        string(6, "app")
        for ((serial, name) in listOf(100 to 1, 101 to 5)) {
            record(2) { writeInt(serial); id(serial); writeInt(0); id(name) }
        }
        record(0x1c) {
            writeByte(0xff); id(200)
            writeByte(0xfe); writeInt(1); id(6)
            // Instances may precede their class definitions. Keep references, erase every primitive type.
            writeByte(0x21); id(200); writeInt(0); id(100); writeInt(idSize + 30 + 4)
            id(201)
            for (width in listOf(1, 2, 4, 8, 1, 2, 4, 8)) secret(width)
            secret(4)
            writeByte(0x22); id(201); writeInt(0); writeInt(2); id(100); id(200); id(202)
            // A primitive array's payload is private, while its length and type are structural.
            for ((type, width) in listOf(4 to 1, 5 to 2, 6 to 4, 7 to 8, 8 to 1, 9 to 2, 10 to 4, 11 to 8)) {
                writeByte(0x23); id(300 + type); writeInt(0); writeInt(3); writeByte(type); secret(3 * width)
            }
            writeByte(0x20); id(100); writeInt(0); id(101)
            repeat(5) { id(0) }; writeInt(40)
            writeShort(1); writeShort(0); writeByte(11); secret(8)
            writeShort(2); id(2); writeByte(2); id(201); id(3); writeByte(11); secret(8)
            writeShort(9); id(2); writeByte(2)
            for (type in listOf(4, 5, 6, 7, 8, 9, 10, 11)) { id(3); writeByte(type) }
            writeByte(0x20); id(101); writeInt(0); repeat(6) { id(0) }; writeInt(16)
            writeShort(0); writeShort(0); writeShort(1); id(3); writeByte(10)
        }
        record(0x2c) { }
        output.flush()
        bytes = source.toByteArray()
        expected = bytes.copyOf()
        erased.forEach { (start, size) -> expected.fill(0, start, start + size) }
        masked.forEach { (start, size) -> expected.fill('?'.code.toByte(), start, start + size) }
    }

    private fun DataOutputStream.id(value: Int) {
        if (idSize == 4) writeInt(value) else writeLong(value.toLong())
    }

    private fun DataOutputStream.secret(size: Int) {
        erased.add(source.size() to size)
        write(ByteArray(size) { 0x41 })
    }

    private fun string(id: Int, value: String, sensitive: Boolean = false) = record(1) {
        id(id)
        if (sensitive) masked.add(source.size() to value.length)
        writeBytes(value)
    }

    private fun record(tag: Int, content: DataOutputStream.() -> Unit) {
        output.writeByte(tag)
        output.writeInt(0)
        val lengthAt = source.size()
        output.writeInt(0)
        val start = source.size()
        output.content()
        val end = source.size()
        val copy = source.toByteArray()
        val length = end - start
        for (index in 0 until 4) copy[lengthAt + index] = (length ushr (24 - index * 8)).toByte()
        source.reset()
        source.write(copy)
    }
}
