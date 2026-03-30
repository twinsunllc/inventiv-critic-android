package io.inventiv.critic.api

import io.inventiv.critic.model.AppInfo
import io.inventiv.critic.model.AppVersion
import io.inventiv.critic.model.DeviceInfo
import io.inventiv.critic.model.DeviceStatus
import io.inventiv.critic.model.PingRequest
import kotlinx.coroutines.test.runTest
import org.junit.Assume.assumeTrue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import java.net.HttpURLConnection
import java.net.URL

/**
 * Live integration test that runs against a REAL Critic instance.
 *
 * Skipped automatically when the required environment variables are not set
 * or when the server is not reachable.
 *
 * Configuration (via environment variables or system properties):
 *   CRITIC_BASE_URL  - Host only, e.g. https://critic.inventiv.io (no trailing path)
 *   CRITIC_API_TOKEN - e.g. your-api-token
 *
 * Run manually:
 *   ./gradlew :library:test --tests "*.CriticApiLiveTest" \
 *       -DCRITIC_BASE_URL=https://critic.inventiv.io \
 *       -DCRITIC_API_TOKEN=your-api-token
 */
class CriticApiLiveTest {

    private val baseUrl: String? =
        System.getProperty("CRITIC_BASE_URL")
            ?: System.getenv("CRITIC_BASE_URL")

    private val apiToken: String? =
        System.getProperty("CRITIC_API_TOKEN")
            ?: System.getenv("CRITIC_API_TOKEN")

    @Before
    fun checkEnvironmentAndServer() {
        assumeTrue(
            "Skipping live test: CRITIC_BASE_URL not set",
            !baseUrl.isNullOrBlank()
        )
        assumeTrue(
            "Skipping live test: CRITIC_API_TOKEN not set",
            !apiToken.isNullOrBlank()
        )
        val reachable = try {
            val url = URL(baseUrl!!.trimEnd('/') + "/ping")
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 2000
            connection.readTimeout = 2000
            connection.requestMethod = "HEAD"
            connection.connect()
            connection.responseCode > 0
        } catch (_: Exception) {
            false
        }
        assumeTrue(
            "Skipping live test: Critic server not reachable at $baseUrl",
            reachable
        )
    }

    @Test
    fun `ping against live server returns app install id`() = runTest {
        ApiClient.configure(baseUrl!!)
        val api = ApiClient.api()

        val request = PingRequest(
            apiToken = apiToken!!,
            app = AppInfo(
                name = "CriticAndroidSDK-Test",
                packageName = "io.inventiv.critic.test",
                version = AppVersion(code = 1, name = "1.0.0-test"),
            ),
            device = DeviceInfo(
                identifier = "live-test-device-001",
                manufacturer = "JUnit",
                model = "TestRunner",
                networkCarrier = "N/A",
                platformVersion = "15",
            ),
            deviceStatus = DeviceStatus(
                batteryCharging = true,
                batteryHealth = "Good",
                batteryLevel = 100.0f,
            ),
        )

        val response = api.ping(request)
        assertEquals("Ping should return 200", 200, response.code())
        assertNotNull("Response body should not be null", response.body())
        assertNotNull("App install ID should not be null", response.body()!!.appInstall.id)
        println("Live ping successful. App install ID: ${response.body()!!.appInstall.id}")
    }

    @Test
    fun `submit bug report against live server`() = runTest {
        ApiClient.configure(baseUrl!!)
        val api = ApiClient.api()

        // Step 1: Ping to get app_install id
        val pingRequest = PingRequest(
            apiToken = apiToken!!,
            app = AppInfo(
                name = "CriticAndroidSDK-Test",
                packageName = "io.inventiv.critic.test",
                version = AppVersion(code = 1, name = "1.0.0-test"),
            ),
            device = DeviceInfo(
                identifier = "live-test-device-001",
                manufacturer = "JUnit",
                model = "TestRunner",
                networkCarrier = "N/A",
                platformVersion = "15",
            ),
            deviceStatus = DeviceStatus(),
        )

        val pingResponse = api.ping(pingRequest)
        assertEquals(200, pingResponse.code())
        val appInstallId = pingResponse.body()!!.appInstall.id

        // Step 2: Submit a bug report
        val parts = mutableListOf<okhttp3.MultipartBody.Part>()
        parts += okhttp3.MultipartBody.Part.createFormData("api_token", apiToken!!)
        parts += okhttp3.MultipartBody.Part.createFormData("app_install[id]", appInstallId)
        parts += okhttp3.MultipartBody.Part.createFormData(
            "bug_report[description]",
            "Test bug report from Android SDK integration test"
        )
        parts += okhttp3.MultipartBody.Part.createFormData(
            "bug_report[steps_to_reproduce]",
            "1. Run CriticApiLiveTest\n2. This report is auto-generated"
        )

        val reportResponse = api.createBugReport(parts)
        assertEquals("Bug report should return 201", 201, reportResponse.code())
        val bugReport = reportResponse.body()!!
        assertNotNull("Bug report ID should not be null", bugReport.id)
        println("Live bug report submitted. ID: ${bugReport.id}")
    }
}
