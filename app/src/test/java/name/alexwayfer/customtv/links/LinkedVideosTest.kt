package name.alexwayfer.customtv.links

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LinkedVideosTest {
    @Test
    fun aRecordingComesWithTheLoginOfItsChannel() {
        val body = """
            {"data":{"video":{"id":"123","title":" Speedrun ","previewThumbnailURL":"https://thumb/640x360.jpg",
            "createdAt":"2026-10-01T12:00:00Z","publishedAt":"2026-10-01T12:00:05Z","viewCount":42,
            "lengthSeconds":3600,"owner":{"login":"lirik"}}}}
        """.trimIndent()

        val linked = parseLinkedVideo(body)!!

        assertEquals("lirik", linked.channelLogin)
        assertEquals("123", linked.video.id)
        assertEquals("Speedrun", linked.video.title)
        assertEquals("https://thumb/640x360.jpg", linked.video.thumbnailUrl)
        assertEquals(42L, linked.video.viewCount)
        assertEquals(3600L, linked.video.durationSeconds)
    }

    @Test
    fun aRecordingThatIsGoneOpensNothing() {
        assertNull(parseLinkedVideo("""{"data":{"video":null}}"""))
    }

    @Test
    fun aRecordingWithoutItsChannelOpensNothing() {
        assertNull(parseLinkedVideo("""{"data":{"video":{"id":"123","title":"Run","owner":null}}}"""))
    }

    @Test
    fun anAnswerWithoutDataOpensNothing() {
        assertNull(parseLinkedVideo("""{"errors":[{"message":"service error"}]}"""))
        assertNull(parseLinkedVideo("not json"))
    }
}
