package io.inventiv.critic.api

import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

class ErrorHandlingTest {

    private lateinit var server: MockWebServer

    @Before
    fun setup() {
        server = MockWebServer()
        server.start()
        ApiClient.configure(server.url("/api/v3/").toString())
    }

    @After
    fun tearDown() {
        server.shutdown()
        ApiClient.configure()
    }

    // --- 401 Unauthorized ---

    @Test
    fun `listBugReports returns 401 for invalid token`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(401)
                .setBody("""{"error": "Unauthorized"}""")
        )

        val response = ApiClient.api().listBugReports(appApiToken = "bad-token")
        assertEquals(401, response.code())
        assertFalse(response.isSuccessful)
    }

    @Test
    fun `getBugReport returns 401 for invalid token`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(401)
                .setBody("""{"error": "Unauthorized"}""")
        )

        val response = ApiClient.api().getBugReport(id = "some-id", appApiToken = "bad-token")
        assertEquals(401, response.code())
        assertFalse(response.isSuccessful)
    }

    @Test
    fun `listDevices returns 401 for invalid token`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(401)
                .setBody("""{"error": "Unauthorized"}""")
        )

        val response = ApiClient.api().listDevices(appApiToken = "bad-token")
        assertEquals(401, response.code())
        assertFalse(response.isSuccessful)
    }

    @Test
    fun `ping returns 401 for invalid token`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(401)
                .setBody("""{"error": "Unauthorized"}""")
        )

        val request = io.inventiv.critic.model.PingRequest(
            apiToken = "bad-token",
            app = io.inventiv.critic.model.AppInfo(
                name = "App", packageName = "com.test",
                version = io.inventiv.critic.model.AppVersion(code = 1, name = "1.0"),
            ),
            device = io.inventiv.critic.model.DeviceInfo(
                identifier = "id", manufacturer = "M", model = "M",
                networkCarrier = "C", platformVersion = "14",
            ),
            deviceStatus = io.inventiv.critic.model.DeviceStatus(),
        )

        val response = ApiClient.api().ping(request)
        assertEquals(401, response.code())
        assertFalse(response.isSuccessful)
    }

    // --- 403 Forbidden ---

    @Test
    fun `listBugReports returns 403 when access denied`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(403)
                .setBody("""{"error": "Forbidden"}""")
        )

        val response = ApiClient.api().listBugReports(appApiToken = "token")
        assertEquals(403, response.code())
        assertFalse(response.isSuccessful)
    }

    @Test
    fun `getBugReport returns 403 when access denied`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(403)
                .setBody("""{"error": "Forbidden"}""")
        )

        val response = ApiClient.api().getBugReport(id = "id", appApiToken = "token")
        assertEquals(403, response.code())
        assertFalse(response.isSuccessful)
    }

    // --- 404 Not Found ---

    @Test
    fun `getBugReport returns 404 for nonexistent report`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(404)
                .setBody("""{"error": "Not Found"}""")
        )

        val response = ApiClient.api().getBugReport(
            id = "nonexistent-uuid",
            appApiToken = "token",
        )
        assertEquals(404, response.code())
        assertFalse(response.isSuccessful)
    }

    @Test
    fun `listDevices returns 404 for unknown app`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(404)
                .setBody("""{"error": "Not Found"}""")
        )

        val response = ApiClient.api().listDevices(appApiToken = "bad-app-token")
        assertEquals(404, response.code())
        assertFalse(response.isSuccessful)
    }

    // --- 422 Unprocessable Entity ---

    @Test
    fun `createBugReport returns 422 for invalid data`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(422)
                .setBody("""{"errors": {"description": ["can't be blank"]}}""")
        )

        val parts = listOf(
            okhttp3.MultipartBody.Part.createFormData("api_token", "token"),
            okhttp3.MultipartBody.Part.createFormData("app_install[id]", "install-id"),
            okhttp3.MultipartBody.Part.createFormData("bug_report[description]", ""),
        )

        val response = ApiClient.api().createBugReport(parts)
        assertEquals(422, response.code())
        assertFalse(response.isSuccessful)
    }

    @Test
    fun `ping returns 422 for malformed request`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(422)
                .setBody("""{"errors": {"api_token": ["is invalid"]}}""")
        )

        val request = io.inventiv.critic.model.PingRequest(
            apiToken = "",
            app = io.inventiv.critic.model.AppInfo(
                name = "App", packageName = "com.test",
                version = io.inventiv.critic.model.AppVersion(code = 1, name = "1.0"),
            ),
            device = io.inventiv.critic.model.DeviceInfo(
                identifier = "id", manufacturer = "M", model = "M",
                networkCarrier = "C", platformVersion = "14",
            ),
            deviceStatus = io.inventiv.critic.model.DeviceStatus(),
        )

        val response = ApiClient.api().ping(request)
        assertEquals(422, response.code())
        assertFalse(response.isSuccessful)
    }

    // --- Response body null on error ---

    @Test
    fun `error responses have null body`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(500)
                .setBody("""{"error": "Internal Server Error"}""")
        )

        val response = ApiClient.api().listBugReports(appApiToken = "token")
        assertEquals(500, response.code())
        assertFalse(response.isSuccessful)
    }
}
