package io.inventiv.critic.api

import kotlinx.coroutines.test.runTest
import okhttp3.MultipartBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MultipartRequestBuildingTest {

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

    @Test
    fun `multipart request includes all required form fields`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(201)
                .setHeader("Content-Type", "application/json")
                .setBody("""{"bug_report": {"id": "uuid-123", "description": "Test"}}""")
        )

        val parts = mutableListOf<MultipartBody.Part>()
        parts += MultipartBody.Part.createFormData("api_token", "my-api-token")
        parts += MultipartBody.Part.createFormData("app_install[id]", "install-uuid")
        parts += MultipartBody.Part.createFormData("bug_report[description]", "Something is broken")
        parts += MultipartBody.Part.createFormData("bug_report[steps_to_reproduce]", "1. Open\n2. Tap")
        parts += MultipartBody.Part.createFormData("bug_report[user_identifier]", "user@test.com")

        val response = ApiClient.api().createBugReport(parts)
        assertEquals(201, response.code())

        val recorded = server.takeRequest()
        val body = recorded.body.readUtf8()
        assertTrue("api_token missing", body.contains("my-api-token"))
        assertTrue("install id missing", body.contains("install-uuid"))
        assertTrue("description missing", body.contains("Something is broken"))
        assertTrue("steps missing", body.contains("1. Open"))
        assertTrue("user id missing", body.contains("user@test.com"))
    }

    @Test
    fun `multipart request includes metadata JSON`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(201)
                .setHeader("Content-Type", "application/json")
                .setBody("""{"bug_report": {"id": "uuid", "description": "d"}}""")
        )

        val metadataJson = """{"email":"user@example.com","plan":"pro"}"""
        val parts = mutableListOf<MultipartBody.Part>()
        parts += MultipartBody.Part.createFormData("api_token", "token")
        parts += MultipartBody.Part.createFormData("app_install[id]", "install")
        parts += MultipartBody.Part.createFormData("bug_report[description]", "desc")
        parts += MultipartBody.Part.createFormData("bug_report[metadata]", metadataJson)

        ApiClient.api().createBugReport(parts)

        val recorded = server.takeRequest()
        val body = recorded.body.readUtf8()
        assertTrue("metadata missing", body.contains("email"))
        assertTrue("metadata missing", body.contains("user@example.com"))
    }

    @Test
    fun `multipart request includes device status JSON`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(201)
                .setHeader("Content-Type", "application/json")
                .setBody("""{"bug_report": {"id": "uuid", "description": "d"}}""")
        )

        val deviceStatusJson = """{"battery_charging":true,"battery_level":85.0}"""
        val parts = mutableListOf<MultipartBody.Part>()
        parts += MultipartBody.Part.createFormData("api_token", "token")
        parts += MultipartBody.Part.createFormData("app_install[id]", "install")
        parts += MultipartBody.Part.createFormData("bug_report[description]", "desc")
        parts += MultipartBody.Part.createFormData("device_status", deviceStatusJson)

        ApiClient.api().createBugReport(parts)

        val recorded = server.takeRequest()
        val body = recorded.body.readUtf8()
        assertTrue("device_status missing", body.contains("battery_charging"))
        assertTrue("device_status missing", body.contains("battery_level"))
    }

    @Test
    fun `multipart request includes file attachment with correct content type`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(201)
                .setHeader("Content-Type", "application/json")
                .setBody("""{"bug_report": {"id": "uuid", "description": "d"}}""")
        )

        val fileContent = "fake log content\nline 2\n"
        val requestBody = fileContent.toRequestBody("text/plain".toMediaType())

        val parts = mutableListOf<MultipartBody.Part>()
        parts += MultipartBody.Part.createFormData("api_token", "token")
        parts += MultipartBody.Part.createFormData("app_install[id]", "install")
        parts += MultipartBody.Part.createFormData("bug_report[description]", "desc")
        parts += MultipartBody.Part.createFormData(
            "bug_report[attachments][]", "logcat.txt", requestBody
        )

        ApiClient.api().createBugReport(parts)

        val recorded = server.takeRequest()
        val contentType = recorded.getHeader("Content-Type")!!
        assertTrue("Should be multipart", contentType.startsWith("multipart/form-data"))

        val body = recorded.body.readUtf8()
        assertTrue("attachment filename missing", body.contains("logcat.txt"))
        assertTrue("attachment content missing", body.contains("fake log content"))
    }

    @Test
    fun `multipart request includes multiple file attachments`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(201)
                .setHeader("Content-Type", "application/json")
                .setBody("""{"bug_report": {"id": "uuid", "description": "d"}}""")
        )

        val logContent = "log data"
        val imageContent = "fake png data"
        val logBody = logContent.toRequestBody("text/plain".toMediaType())
        val imageBody = imageContent.toRequestBody("image/png".toMediaType())

        val parts = mutableListOf<MultipartBody.Part>()
        parts += MultipartBody.Part.createFormData("api_token", "token")
        parts += MultipartBody.Part.createFormData("app_install[id]", "install")
        parts += MultipartBody.Part.createFormData("bug_report[description]", "desc")
        parts += MultipartBody.Part.createFormData("bug_report[attachments][]", "logcat.txt", logBody)
        parts += MultipartBody.Part.createFormData("bug_report[attachments][]", "screenshot.png", imageBody)

        ApiClient.api().createBugReport(parts)

        val recorded = server.takeRequest()
        val body = recorded.body.readUtf8()
        assertTrue("logcat attachment missing", body.contains("logcat.txt"))
        assertTrue("screenshot attachment missing", body.contains("screenshot.png"))
    }

    @Test
    fun `request uses POST method for bug report creation`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(201)
                .setHeader("Content-Type", "application/json")
                .setBody("""{"bug_report": {"id": "uuid", "description": "d"}}""")
        )

        val parts = listOf(
            MultipartBody.Part.createFormData("api_token", "token"),
            MultipartBody.Part.createFormData("app_install[id]", "install"),
            MultipartBody.Part.createFormData("bug_report[description]", "desc"),
        )

        ApiClient.api().createBugReport(parts)

        val recorded = server.takeRequest()
        assertEquals("POST", recorded.method)
        assertEquals("/api/v3/bug_reports", recorded.path)
    }
}
