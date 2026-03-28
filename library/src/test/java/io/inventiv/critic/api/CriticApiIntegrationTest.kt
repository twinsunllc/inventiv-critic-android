package io.inventiv.critic.api

import io.inventiv.critic.model.AppInfo
import io.inventiv.critic.model.AppVersion
import io.inventiv.critic.model.DeviceInfo
import io.inventiv.critic.model.DeviceStatus
import io.inventiv.critic.model.PingRequest
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Integration test demonstrating the full Critic SDK flow:
 * 1. Configure API client (pointing at a local server)
 * 2. Ping to register the device and obtain an app_install ID
 * 3. Submit a bug report using the app_install ID
 *
 * Uses MockWebServer so no real server is needed.
 *
 * To run against a REAL Critic instance instead, see
 * [CriticApiLiveTest] (requires CRITIC_BASE_URL and CRITIC_API_TOKEN env vars).
 */
class CriticApiIntegrationTest {

    private lateinit var server: MockWebServer
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    @Before
    fun setup() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    // -- helpers --

    private fun buildPingRequest(apiToken: String = "test-token"): PingRequest {
        return PingRequest(
            apiToken = apiToken,
            app = AppInfo(
                name = "ExampleApp",
                packageName = "io.inventiv.critic.example",
                version = AppVersion(code = 1, name = "1.0.0"),
            ),
            device = DeviceInfo(
                identifier = "test-device-uuid",
                manufacturer = "Google",
                model = "Pixel 9",
                networkCarrier = "T-Mobile",
                platformVersion = "15",
            ),
            deviceStatus = DeviceStatus(
                batteryCharging = false,
                batteryHealth = "Good",
                batteryLevel = 72.0f,
                diskFree = 5_000_000_000L,
                diskTotal = 64_000_000_000L,
                diskUsable = 4_500_000_000L,
                memoryFree = 2_000_000_000L,
                memoryTotal = 8_000_000_000L,
                networkCellConnected = true,
                networkWifiConnected = true,
            ),
        )
    }

    // -- tests --

    @Test
    fun `full flow - ping then submit bug report`() = runTest {
        // 1. Enqueue a successful ping response
        val appInstallId = "install-uuid-12345"
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""{"app_install": {"id": "$appInstallId"}}""")
        )

        // 2. Enqueue a successful bug report response (v3 API returns flat, no wrapper)
        server.enqueue(
            MockResponse()
                .setResponseCode(201)
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """
                    {
                        "id": "report-uuid-67890",
                        "description": "The app crashes when I tap the login button",
                        "created_at": "2026-03-28T01:00:00Z",
                        "updated_at": "2026-03-28T01:00:00Z",
                        "attachments": []
                    }
                    """.trimIndent()
                )
        )

        // 3. Configure ApiClient to point at mock server
        val baseUrl = server.url("/").toString().trimEnd('/')
        ApiClient.configure(baseUrl)
        val api = ApiClient.api()

        // 4. Ping
        val pingRequest = buildPingRequest()
        val pingResponse = api.ping(pingRequest)
        assertEquals(200, pingResponse.code())
        val pingBody = pingResponse.body()
        assertNotNull(pingBody)
        assertEquals(appInstallId, pingBody!!.appInstall.id)

        // Verify the ping request was sent correctly
        val recordedPing = server.takeRequest()
        assertEquals("POST", recordedPing.method)
        assertTrue(recordedPing.path!!.endsWith("/api/v3/ping"))
        val sentPingBody = recordedPing.body.readUtf8()
        assertTrue(sentPingBody.contains("\"api_token\":\"test-token\""))
        assertTrue(sentPingBody.contains("\"platform\":\"Android\""))

        // 5. Submit a bug report (multipart)
        val parts = mutableListOf<okhttp3.MultipartBody.Part>()
        parts += okhttp3.MultipartBody.Part.createFormData("api_token", "test-token")
        parts += okhttp3.MultipartBody.Part.createFormData("app_install[id]", appInstallId)
        parts += okhttp3.MultipartBody.Part.createFormData(
            "bug_report[description]",
            "The app crashes when I tap the login button"
        )

        val reportResponse = api.createBugReport(parts)
        assertEquals(201, reportResponse.code())
        val report = reportResponse.body()
        assertNotNull(report)
        assertEquals("report-uuid-67890", report!!.id)
        assertEquals("The app crashes when I tap the login button", report.description)

        // Verify the bug report request
        val recordedReport = server.takeRequest()
        assertEquals("POST", recordedReport.method)
        assertTrue(recordedReport.path!!.endsWith("/api/v3/bug_reports"))
    }

    @Test
    fun `ping request serializes all device fields`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""{"app_install": {"id": "uuid-123"}}""")
        )

        ApiClient.configure(server.url("/").toString().trimEnd('/'))
        val api = ApiClient.api()
        api.ping(buildPingRequest())

        val recorded = server.takeRequest()
        val body = recorded.body.readUtf8()

        // Verify all expected fields are present
        assertTrue("Should contain api_token", body.contains("\"api_token\""))
        assertTrue("Should contain app name", body.contains("\"ExampleApp\""))
        assertTrue("Should contain package", body.contains("\"io.inventiv.critic.example\""))
        assertTrue("Should contain manufacturer", body.contains("\"Google\""))
        assertTrue("Should contain model", body.contains("\"Pixel 9\""))
        assertTrue("Should contain battery_level", body.contains("\"battery_level\""))
        assertTrue("Should contain disk_free", body.contains("\"disk_free\""))
    }

    @Test
    fun `ping returns error for invalid token`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(401)
                .setHeader("Content-Type", "application/json")
                .setBody("""{"error": "Invalid API token"}""")
        )

        ApiClient.configure(server.url("/").toString().trimEnd('/'))
        val api = ApiClient.api()
        val response = api.ping(buildPingRequest(apiToken = "bad-token"))

        assertEquals(401, response.code())
    }

    @Test
    fun `bug report with optional fields`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(201)
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """
                    {
                        "id": "uuid-with-extras",
                        "description": "Login crash",
                        "steps_to_reproduce": "1. Open app\n2. Tap login",
                        "user_identifier": "user@example.com",
                        "attachments": []
                    }
                    """.trimIndent()
                )
        )

        ApiClient.configure(server.url("/").toString().trimEnd('/'))
        val api = ApiClient.api()

        val parts = mutableListOf<okhttp3.MultipartBody.Part>()
        parts += okhttp3.MultipartBody.Part.createFormData("api_token", "test-token")
        parts += okhttp3.MultipartBody.Part.createFormData("app_install[id]", "install-id")
        parts += okhttp3.MultipartBody.Part.createFormData("bug_report[description]", "Login crash")
        parts += okhttp3.MultipartBody.Part.createFormData(
            "bug_report[steps_to_reproduce]", "1. Open app\n2. Tap login"
        )
        parts += okhttp3.MultipartBody.Part.createFormData(
            "bug_report[user_identifier]", "user@example.com"
        )

        val response = api.createBugReport(parts)
        assertEquals(201, response.code())

        val report = response.body()!!
        assertEquals("Login crash", report.description)
        assertEquals("1. Open app\n2. Tap login", report.stepsToReproduce)
        assertEquals("user@example.com", report.userIdentifier)
    }
}
