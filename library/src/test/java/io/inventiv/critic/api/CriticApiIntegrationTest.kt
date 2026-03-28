package io.inventiv.critic.api

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CriticApiIntegrationTest {

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

    // --- Ping ---

    @Test
    fun `ping sends correct request body and parses response`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""{"app_install": {"id": "install-uuid-123"}}""")
        )

        val request = io.inventiv.critic.model.PingRequest(
            apiToken = "test-token",
            app = io.inventiv.critic.model.AppInfo(
                name = "TestApp",
                packageName = "com.test.app",
                version = io.inventiv.critic.model.AppVersion(code = 1, name = "1.0"),
            ),
            device = io.inventiv.critic.model.DeviceInfo(
                identifier = "device-id",
                manufacturer = "Google",
                model = "Pixel",
                networkCarrier = "T-Mobile",
                platformVersion = "14",
            ),
            deviceStatus = io.inventiv.critic.model.DeviceStatus(
                batteryCharging = true,
                batteryHealth = "Good",
                batteryLevel = 85.0f,
            ),
        )

        val response = ApiClient.api().ping(request)

        assertEquals(200, response.code())
        assertEquals("install-uuid-123", response.body()?.appInstall?.id)

        val recorded = server.takeRequest()
        assertEquals("POST", recorded.method)
        assertEquals("/api/v3/ping", recorded.path)
        val body = recorded.body.readUtf8()
        assertTrue("api_token missing from body", body.contains("\"api_token\":\"test-token\""))
        assertTrue("platform missing from body", body.contains("\"platform\":\"Android\""))
    }

    // --- List Bug Reports ---

    @Test
    fun `listBugReports sends correct query parameters`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                    {
                        "count": 2,
                        "current_page": 1,
                        "total_pages": 1,
                        "bug_reports": [
                            {"id": "uuid-1", "description": "Bug 1"},
                            {"id": "uuid-2", "description": "Bug 2"}
                        ]
                    }
                """.trimIndent())
        )

        val response = ApiClient.api().listBugReports(
            appApiToken = "app-token-123",
            archived = true,
            deviceId = "device-456",
            since = "2026-03-01T00:00:00Z",
        )

        assertEquals(200, response.code())
        val body = response.body()!!
        assertEquals(2, body.count)
        assertEquals(1, body.currentPage)
        assertEquals(1, body.totalPages)
        assertEquals(2, body.bugReports.size)
        assertEquals("uuid-1", body.bugReports[0].id)
        assertEquals("Bug 1", body.bugReports[0].description)

        val recorded = server.takeRequest()
        assertEquals("GET", recorded.method)
        assertTrue("app_api_token missing from path", recorded.path!!.contains("app_api_token=app-token-123"))
        assertTrue("archived param missing from path", recorded.path!!.contains("archived=true"))
        assertTrue("device_id param missing from path", recorded.path!!.contains("device_id=device-456"))
        assertTrue("since param missing from path", recorded.path!!.contains("since=2026-03-01T00%3A00%3A00Z"))
    }

    @Test
    fun `listBugReports with no optional params omits query parameters`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""{"count": 0, "current_page": 1, "total_pages": 0, "bug_reports": []}""")
        )

        ApiClient.api().listBugReports(appApiToken = "token")

        val recorded = server.takeRequest()
        assertEquals("/api/v3/bug_reports?app_api_token=token", recorded.path)
    }

    @Test
    fun `listBugReports returns empty list`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""{"count": 0, "current_page": 1, "total_pages": 0, "bug_reports": []}""")
        )

        val response = ApiClient.api().listBugReports(appApiToken = "token")
        val body = response.body()!!
        assertEquals(0, body.count)
        assertEquals(0, body.bugReports.size)
    }

    // --- Get Bug Report ---

    @Test
    fun `getBugReport sends correct path and query`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                    {
                        "bug_report": {
                            "id": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
                            "description": "App crashes on login",
                            "steps_to_reproduce": "1. Open app\n2. Tap login",
                            "user_identifier": "user@example.com",
                            "created_at": "2026-03-27T10:00:00Z",
                            "attachments": [
                                {
                                    "id": "att-uuid",
                                    "file_file_name": "logcat.txt",
                                    "file_content_type": "text/plain",
                                    "file_file_size": 1024,
                                    "url": "https://example.com/logcat.txt"
                                }
                            ],
                            "device": {
                                "id": "dev-uuid",
                                "manufacturer": "Samsung",
                                "model": "Galaxy S24"
                            }
                        }
                    }
                """.trimIndent())
        )

        val response = ApiClient.api().getBugReport(
            id = "f47ac10b-58cc-4372-a567-0e02b2c3d479",
            appApiToken = "app-token",
        )

        assertEquals(200, response.code())
        val report = response.body()!!.bugReport
        assertEquals("f47ac10b-58cc-4372-a567-0e02b2c3d479", report.id)
        assertEquals("App crashes on login", report.description)
        assertEquals("1. Open app\n2. Tap login", report.stepsToReproduce)
        assertEquals("user@example.com", report.userIdentifier)
        assertEquals(1, report.attachments?.size)
        assertEquals("logcat.txt", report.attachments?.first()?.fileFileName)
        assertEquals("Samsung", report.device?.manufacturer)

        val recorded = server.takeRequest()
        assertEquals("GET", recorded.method)
        assertEquals(
            "/api/v3/bug_reports/f47ac10b-58cc-4372-a567-0e02b2c3d479?app_api_token=app-token",
            recorded.path,
        )
    }

    // --- List Devices ---

    @Test
    fun `listDevices sends correct request and parses paginated response`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                    {
                        "count": 3,
                        "current_page": 1,
                        "total_pages": 1,
                        "devices": [
                            {"id": "dev-1", "manufacturer": "Google", "model": "Pixel 8", "platform": "Android", "platform_version": "14"},
                            {"id": "dev-2", "manufacturer": "Samsung", "model": "Galaxy S24", "platform": "Android"},
                            {"id": "dev-3", "manufacturer": "OnePlus", "model": "12", "platform": "Android"}
                        ]
                    }
                """.trimIndent())
        )

        val response = ApiClient.api().listDevices(appApiToken = "my-token")

        assertEquals(200, response.code())
        val body = response.body()!!
        assertEquals(3, body.count)
        assertEquals(3, body.devices.size)
        assertEquals("Google", body.devices[0].manufacturer)
        assertEquals("Pixel 8", body.devices[0].model)
        assertEquals("14", body.devices[0].platformVersion)

        val recorded = server.takeRequest()
        assertEquals("GET", recorded.method)
        assertEquals("/api/v3/devices?app_api_token=my-token", recorded.path)
    }

    // --- Create Bug Report (multipart) ---

    @Test
    fun `createBugReport sends multipart request and parses response`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(201)
                .setHeader("Content-Type", "application/json")
                .setBody("""{"bug_report": {"id": "new-report-uuid", "description": "Test bug"}}""")
        )

        val parts = mutableListOf<okhttp3.MultipartBody.Part>()
        parts += okhttp3.MultipartBody.Part.createFormData("api_token", "test-token")
        parts += okhttp3.MultipartBody.Part.createFormData("app_install[id]", "install-id")
        parts += okhttp3.MultipartBody.Part.createFormData("bug_report[description]", "Test bug")

        val response = ApiClient.api().createBugReport(parts)

        assertEquals(201, response.code())
        assertEquals("new-report-uuid", response.body()?.bugReport?.id)

        val recorded = server.takeRequest()
        assertEquals("POST", recorded.method)
        assertEquals("/api/v3/bug_reports", recorded.path)
        val body = recorded.body.readUtf8()
        assertTrue("api_token missing from body", body.contains("api_token"))
        assertTrue("test-token value missing from body", body.contains("test-token"))
        assertTrue("bug_report[description] missing from body", body.contains("bug_report[description]"))
        assertTrue("Test bug value missing from body", body.contains("Test bug"))
    }
}
