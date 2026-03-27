package io.inventiv.critic.api

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class CriticInterceptorTest {

    private lateinit var server: MockWebServer
    private lateinit var client: OkHttpClient

    @Before
    fun setup() {
        server = MockWebServer()
        server.start()
        client = OkHttpClient.Builder()
            .addInterceptor(CriticInterceptor("Test-Agent"))
            .build()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `adds Accept header when missing`() {
        server.enqueue(MockResponse().setBody("{}"))

        val request = Request.Builder().url(server.url("/test")).build()
        client.newCall(request).execute()

        val recorded = server.takeRequest()
        assertEquals("application/json", recorded.getHeader("Accept"))
    }

    @Test
    fun `adds User-Agent header when missing`() {
        server.enqueue(MockResponse().setBody("{}"))

        val request = Request.Builder().url(server.url("/test")).build()
        client.newCall(request).execute()

        val recorded = server.takeRequest()
        assertEquals("Test-Agent", recorded.getHeader("User-Agent"))
    }

    @Test
    fun `preserves existing Accept header`() {
        server.enqueue(MockResponse().setBody("{}"))

        val request = Request.Builder()
            .url(server.url("/test"))
            .header("Accept", "text/html")
            .build()
        client.newCall(request).execute()

        val recorded = server.takeRequest()
        assertEquals("text/html", recorded.getHeader("Accept"))
    }

    @Test
    fun `preserves existing User-Agent header`() {
        server.enqueue(MockResponse().setBody("{}"))

        val request = Request.Builder()
            .url(server.url("/test"))
            .header("User-Agent", "Custom-Agent")
            .build()
        client.newCall(request).execute()

        val recorded = server.takeRequest()
        assertEquals("Custom-Agent", recorded.getHeader("User-Agent"))
    }
}
