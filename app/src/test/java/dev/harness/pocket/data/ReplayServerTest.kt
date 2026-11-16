package dev.harness.pocket.data

import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URL

class ReplayServerTest {
    @Test
    fun servesReplayHtml() {
        val url = ReplayServer.start("<html><body>replay-ok</body></html>")
        assertTrue(url != null)
        val text = URL("http://127.0.0.1:${ReplayServer.port}/").readText()
        ReplayServer.stop()
        assertTrue(text.contains("replay-ok"))
    }
}
