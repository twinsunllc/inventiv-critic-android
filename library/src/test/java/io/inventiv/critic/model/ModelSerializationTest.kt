package io.inventiv.critic.model

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ModelSerializationTest {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Test
    fun `PingResponse deserializes UUID app install id`() {
        val raw = """{"app_install": {"id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"}}"""
        val response = json.decodeFromString<PingResponse>(raw)
        assertEquals("a1b2c3d4-e5f6-7890-abcd-ef1234567890", response.appInstall.id)
    }

    @Test
    fun `PingRequest serializes correctly`() {
        val request = PingRequest(
            apiToken = "test-token",
            app = AppInfo(
                name = "TestApp",
                packageName = "com.test.app",
                version = AppVersion(code = 1, name = "1.0"),
            ),
            device = DeviceInfo(
                identifier = "device-uuid",
                manufacturer = "Google",
                model = "Pixel",
                networkCarrier = "T-Mobile",
                platformVersion = "14",
            ),
            deviceStatus = DeviceStatus(
                batteryCharging = true,
                batteryHealth = "Good",
                batteryLevel = 85.0f,
            ),
        )

        val serialized = json.encodeToString(PingRequest.serializer(), request)
        assertNotNull(serialized)
        assert(serialized.contains("\"api_token\":\"test-token\""))
        assert(serialized.contains("\"platform\":\"Android\""))
    }

    @Test
    fun `BugReport deserializes with UUID id and nested objects`() {
        val raw = """
        {
            "id": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
            "description": "App crashes on login",
            "created_at": "2026-03-27T10:00:00Z",
            "updated_at": "2026-03-27T10:00:00Z",
            "attachments": [
                {
                    "id": "aabbccdd-1122-3344-5566-778899001122",
                    "file_file_name": "logcat.txt",
                    "file_content_type": "text/plain",
                    "file_file_size": 1024,
                    "file_url": "https://example.com/logcat.txt"
                }
            ],
            "device": {
                "id": "ddccbbaa-9988-7766-5544-332211009988",
                "manufacturer": "Samsung",
                "model": "Galaxy S24"
            }
        }
        """.trimIndent()

        val report = json.decodeFromString<BugReport>(raw)
        assertEquals("f47ac10b-58cc-4372-a567-0e02b2c3d479", report.id)
        assertEquals("App crashes on login", report.description)
        assertEquals(1, report.attachments?.size)
        assertEquals("aabbccdd-1122-3344-5566-778899001122", report.attachments?.first()?.id)
        assertEquals("logcat.txt", report.attachments?.first()?.fileFileName)
        assertEquals("ddccbbaa-9988-7766-5544-332211009988", report.device?.id)
        assertEquals("Samsung", report.device?.manufacturer)
    }

    @Test
    fun `BugReportListResponse deserializes paginated response`() {
        val raw = """
        {
            "count": 42,
            "current_page": 1,
            "total_pages": 5,
            "bug_reports": [
                {"id": "uuid-1", "description": "Bug 1"},
                {"id": "uuid-2", "description": "Bug 2"}
            ]
        }
        """.trimIndent()

        val response = json.decodeFromString<BugReportListResponse>(raw)
        assertEquals(42, response.count)
        assertEquals(1, response.currentPage)
        assertEquals(5, response.totalPages)
        assertEquals(2, response.bugReports.size)
        assertEquals("uuid-1", response.bugReports[0].id)
    }

    @Test
    fun `DeviceListResponse deserializes paginated response`() {
        val raw = """
        {
            "count": 10,
            "current_page": 1,
            "total_pages": 1,
            "devices": [
                {"id": "device-uuid-1", "manufacturer": "Google", "model": "Pixel 8", "platform": "Android"},
                {"id": "device-uuid-2", "manufacturer": "Samsung", "model": "Galaxy S24", "platform": "Android"}
            ]
        }
        """.trimIndent()

        val response = json.decodeFromString<DeviceListResponse>(raw)
        assertEquals(10, response.count)
        assertEquals(2, response.devices.size)
        assertEquals("Google", response.devices[0].manufacturer)
    }

    @Test
    fun `DeviceStatus serializes with null fields omitted when defaults`() {
        val status = DeviceStatus(
            batteryCharging = true,
            batteryLevel = 75.0f,
            diskFree = 1000000L,
            diskTotal = 5000000L,
        )

        val serialized = json.encodeToString(DeviceStatus.serializer(), status)
        assert(serialized.contains("\"battery_charging\":true"))
        assert(serialized.contains("\"battery_level\":75.0"))
        assert(serialized.contains("\"disk_free\":1000000"))
    }

    @Test
    fun `BugReport handles missing optional fields`() {
        val raw = """{"id": "test-uuid"}"""
        val report = json.decodeFromString<BugReport>(raw)
        assertEquals("test-uuid", report.id)
        assertNull(report.description)
        assertNull(report.attachments)
        assertNull(report.device)
    }

    @Test
    fun `BugReportResponse wraps bug report correctly`() {
        val raw = """{"bug_report": {"id": "uuid-123", "description": "Test"}}"""
        val response = json.decodeFromString<BugReportResponse>(raw)
        assertEquals("uuid-123", response.bugReport.id)
        assertEquals("Test", response.bugReport.description)
    }

    @Test
    fun `Attachment deserializes all fields`() {
        val raw = """
        {
            "id": "att-uuid",
            "created_at": "2026-03-27T10:00:00Z",
            "updated_at": "2026-03-27T10:00:00Z",
            "file_file_name": "screenshot.png",
            "file_content_type": "image/png",
            "file_file_size": 204800,
            "file_updated_at": "2026-03-27T10:00:00Z",
            "file_url": "https://example.com/screenshot.png"
        }
        """.trimIndent()

        val attachment = json.decodeFromString<Attachment>(raw)
        assertEquals("att-uuid", attachment.id)
        assertEquals("screenshot.png", attachment.fileFileName)
        assertEquals("image/png", attachment.fileContentType)
        assertEquals(204800L, attachment.fileFileSize)
        assertEquals("https://example.com/screenshot.png", attachment.fileUrl)
    }

    @Test
    fun `AppInfo defaults platform to Android`() {
        val appInfo = AppInfo(
            name = "TestApp",
            packageName = "com.test",
            version = AppVersion(code = 1, name = "1.0"),
        )
        assertEquals("Android", appInfo.platform)
    }

    @Test
    fun `DeviceInfo defaults platform to Android`() {
        val deviceInfo = DeviceInfo(
            identifier = "id",
            manufacturer = "Google",
            model = "Pixel",
            networkCarrier = "N/A",
            platformVersion = "14",
        )
        assertEquals("Android", deviceInfo.platform)
    }

    @Test
    fun `PingResponse ignores unknown JSON keys`() {
        val raw = """{"app_install": {"id": "uuid-123", "unknown_field": "value"}, "extra": true}"""
        val response = json.decodeFromString<PingResponse>(raw)
        assertEquals("uuid-123", response.appInstall.id)
    }
}
