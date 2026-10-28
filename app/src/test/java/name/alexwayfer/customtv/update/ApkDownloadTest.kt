package name.alexwayfer.customtv.update

import org.junit.Assert.assertEquals
import org.junit.Test

class ApkDownloadTest {
    @Test
    fun aCheckKeepsOnlyTheOfferedApkAndItsDownload() {
        val names = listOf("CustomTV-free-1.6.0.apk", "CustomTV-free-1.7.0.apk", "CustomTV-free-1.7.0.apk.part")
        assertEquals(listOf("CustomTV-free-1.6.0.apk"), staleUpdateFiles(names, "CustomTV-free-1.7.0.apk"))
    }

    @Test
    fun aCheckWithoutAnUpdateLeavesNoFile() {
        val names = listOf("CustomTV-free-1.7.0.apk", "CustomTV-free-1.7.0.apk.part")
        assertEquals(names, staleUpdateFiles(names, null))
    }

    @Test
    fun theDownloadWritesAPartFileFirst() {
        assertEquals("CustomTV-free-1.7.0.apk.part", partFileName("CustomTV-free-1.7.0.apk"))
    }

    @Test
    fun hexIsLowercaseWithLeadingZeros() {
        assertEquals("000fa0ff", byteArrayOf(0, 15, -96, -1).toLowerHex())
    }
}
