package dev.harness.pocket.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EventParserTest {
    @Test
    fun extractsPathTag() {
        assertEquals(
            "C:\\Users\\Administrator\\harnessApk\\tools\\h2.png",
            EventParser.extractMediaSource("<path>C:\\Users\\Administrator\\harnessApk\\tools\\h2.png</path>\n<type>image</type>")
        )
    }

    @Test
    fun extractsJsonFilePath() {
        assertEquals(
            "E:\\gitlabSpace\\th18-bases\\sheet-1114.jpg",
            EventParser.extractMediaSource("{\"file_path\":\"E:\\\\gitlabSpace\\\\th18-bases\\\\sheet-1114.jpg\"}")
        )
    }

    @Test
    fun prefersPathTag() {
        assertEquals(
            "/workspace/a.png",
            EventParser.extractMediaSource("<path>/workspace/a.png</path>\n{\"file_path\":\"/tmp/b.png\"}")
        )
    }

    @Test
    fun returnsNullWithoutSource() {
        assertNull(EventParser.extractMediaSource("plain tool output"))
    }
}
