package io.inventiv.critic

import android.Manifest
import android.app.Activity
import android.app.ActivityManager
import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.SensorManager
import android.net.ConnectivityManager
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.telephony.TelephonyManager
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.squareup.seismic.ShakeDetector
import io.inventiv.critic.api.ApiClient
import io.inventiv.critic.model.AppInfo
import io.inventiv.critic.model.AppVersion
import io.inventiv.critic.model.BugReport
import io.inventiv.critic.model.DeviceInfo
import io.inventiv.critic.model.DeviceStatus
import io.inventiv.critic.model.PingRequest
import io.inventiv.critic.util.Logs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.util.UUID

object Critic {

    private const val TAG = "Critic"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private lateinit var application: Application
    private var apiToken: String? = null
    private var appInstallId: String? = null
    private var productMetadata: Map<String, String> = emptyMap()
    private var shakeDetector: ShakeDetector? = null
    private val lifecycleTracker = ApplicationLifecycleTracker()

    private var batteryCharging: Boolean? = null
    private var batteryHealth: String? = null
    private var batteryLevel: Float? = null

    val isInitialized: Boolean
        get() = apiToken != null

    /**
     * Initialize Critic.
     *
     * @param context  The Application instance.
     * @param apiToken Your organization's API token from the Critic dashboard.
     * @param host     Optional server host (scheme + host only, e.g. "https://critic.inventiv.io").
     *                 The SDK appends the API path internally. Defaults to production.
     */
    fun initialize(context: Application, apiToken: String, host: String? = null) {
        require(apiToken.isNotBlank()) {
            "You need to provide an API token. See the Critic Getting Started Guide at https://inventiv.io/critic/"
        }
        require(apiToken != "YOUR_PRODUCT_ACCESS_TOKEN") {
            "Your API token is invalid. Please use a valid one."
        }

        if (isInitialized) {
            Log.w(TAG, "Critic already initialized. Ignoring duplicate call.")
            return
        }

        this.application = context
        this.apiToken = apiToken

        if (host != null) {
            ApiClient.configure(host)
        }

        context.registerActivityLifecycleCallbacks(lifecycleTracker)
        registerBatteryReceiver()
        try {
            startShakeDetection()
        } catch (e: Exception) {
            Log.w(TAG, "Shake detection unavailable: ${e.message}")
        }

        scope.launch {
            try {
                performPing()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create ping.", e)
            }
        }
    }

    fun startShakeDetection() {
        if (shakeDetector != null) return
        val sensorManager = application.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        shakeDetector = ShakeDetector(ShakeListener()).also { it.start(sensorManager) }
    }

    fun stopShakeDetection() {
        shakeDetector?.stop()
        shakeDetector = null
    }

    fun showFeedbackReportActivity() {
        val intent = Intent(application, FeedbackReportActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        application.startActivity(intent)
    }

    fun getAppInstallId(): String? = appInstallId

    fun getApiToken(): String? = apiToken

    fun getProductMetadata(): Map<String, String> = productMetadata

    fun setProductMetadata(metadata: Map<String, String>) {
        for (key in metadata.keys) {
            require(!key.startsWith("ic_", ignoreCase = true)) {
                "Product metadata key [$key] uses reserved prefix [ic_]. Choose a key that does not start with [ic_]."
            }
        }
        this.productMetadata = metadata
    }

    suspend fun submitReport(
        description: String,
        stepsToReproduce: String? = null,
        userIdentifier: String? = null,
        attachments: List<File>? = null,
    ): BugReport {
        val installId = requireNotNull(appInstallId) {
            "Failed to authenticate with bug reporting service. Please try again later."
        }
        require(description.isNotBlank()) { "You need to provide a description." }

        val parts = mutableListOf<MultipartBody.Part>()
        parts += MultipartBody.Part.createFormData("api_token", apiToken!!)
        parts += MultipartBody.Part.createFormData("app_install[id]", installId)
        parts += MultipartBody.Part.createFormData("bug_report[description]", description)

        if (!stepsToReproduce.isNullOrBlank()) {
            parts += MultipartBody.Part.createFormData("bug_report[steps_to_reproduce]", stepsToReproduce)
        }
        if (!userIdentifier.isNullOrBlank()) {
            parts += MultipartBody.Part.createFormData("bug_report[user_identifier]", userIdentifier)
        }

        val metadataJson = buildMetadataJson()
        parts += MultipartBody.Part.createFormData("bug_report[metadata]", metadataJson.toString())

        val deviceStatus = collectDeviceStatus()
        val deviceStatusJson = kotlinx.serialization.json.Json.encodeToString(
            DeviceStatus.serializer(), deviceStatus
        )
        parts += MultipartBody.Part.createFormData("device_status", deviceStatusJson)

        val allAttachments = (attachments?.toMutableList() ?: mutableListOf()).apply {
            try {
                val logcat = Logs.readLogcat(application)
                if (logcat != null) add(logcat)
            } catch (e: Exception) {
                Log.e(TAG, "Could not read logcat", e)
            }
        }

        for (file in allAttachments) {
            val contentType = resolveContentType(file.name)
            val requestBody = file.asRequestBody(contentType.toMediaType())
            parts += MultipartBody.Part.createFormData(
                "bug_report[attachments][]", file.name, requestBody
            )
        }

        val response = withContext(Dispatchers.IO) {
            ApiClient.api().createBugReport(parts)
        }

        if (response.code() != 201) {
            throw CriticException("Invalid response code: ${response.code()}")
        }

        return response.body()
            ?: throw CriticException("No report returned from server.")
    }

    internal fun collectAppInfo(): AppInfo {
        val appInfo = application.applicationInfo
        val stringId = appInfo.labelRes
        val appName = if (stringId == 0) {
            appInfo.nonLocalizedLabel?.toString() ?: ""
        } else {
            application.getString(stringId)
        }
        val packageName = application.packageName
        val packageInfo = application.packageManager.getPackageInfo(packageName, 0)

        return AppInfo(
            name = appName,
            packageName = packageName,
            version = AppVersion(
                code = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    packageInfo.longVersionCode.toInt()
                } else {
                    @Suppress("DEPRECATION")
                    packageInfo.versionCode
                },
                name = packageInfo.versionName ?: "",
            ),
        )
    }

    internal fun collectDeviceInfo(): DeviceInfo {
        val telephonyManager = application.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        val carrier = telephonyManager.networkOperatorName.let {
            if (it.isNullOrBlank()) "N/A" else it
        }

        return DeviceInfo(
            identifier = getApplicationInstanceId(),
            manufacturer = Build.MANUFACTURER,
            model = Build.MODEL,
            networkCarrier = carrier,
            platformVersion = Build.VERSION.RELEASE,
        )
    }

    internal fun collectDeviceStatus(): DeviceStatus {
        val externalDir = Environment.getExternalStorageDirectory()
        val activityManager = application.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo().also { activityManager.getMemoryInfo(it) }

        val connectivityManager = application.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        var cellConnected = false
        var wifiConnected = false

        if (ContextCompat.checkSelfPermission(application, Manifest.permission.ACCESS_NETWORK_STATE) == PackageManager.PERMISSION_GRANTED) {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            @Suppress("DEPRECATION")
            if (networkInfo != null && networkInfo.isConnected) {
                @Suppress("DEPRECATION")
                cellConnected = networkInfo.type == ConnectivityManager.TYPE_MOBILE
                @Suppress("DEPRECATION")
                wifiConnected = networkInfo.type == ConnectivityManager.TYPE_WIFI
            }
        } else {
            Log.w(TAG, "ACCESS_NETWORK_STATE not granted. Cannot retrieve NetworkInfo.")
        }

        return DeviceStatus(
            batteryCharging = batteryCharging,
            batteryHealth = batteryHealth,
            batteryLevel = batteryLevel,
            diskFree = externalDir.freeSpace,
            diskTotal = externalDir.totalSpace,
            diskUsable = externalDir.usableSpace,
            memoryActive = readProcMeminfoValue("Active"),
            memoryFree = memoryInfo.availMem,
            memoryTotal = memoryInfo.totalMem,
            networkCellConnected = cellConnected,
            networkWifiConnected = wifiConnected,
        )
    }

    internal fun readProcMeminfoValue(key: String): Long? {
        return try {
            parseProcMeminfoValue(java.io.File("/proc/meminfo").readText(), key)
        } catch (e: Exception) {
            Log.w(TAG, "Could not read /proc/meminfo for key '$key': ${e.message}")
            null
        }
    }

    internal fun parseProcMeminfoValue(content: String, key: String): Long? {
        return content.lineSequence()
            .firstOrNull { it.startsWith("$key:") }
            ?.substringAfter(":")
            ?.trim()
            ?.split("\\s+".toRegex())
            ?.firstOrNull()
            ?.toLongOrNull()
            ?.let { it * 1024 } // /proc/meminfo reports in kB, convert to bytes
    }

    private suspend fun performPing() {
        val request = PingRequest(
            apiToken = apiToken!!,
            app = collectAppInfo(),
            device = collectDeviceInfo(),
            deviceStatus = collectDeviceStatus(),
        )

        val response = withContext(Dispatchers.IO) {
            ApiClient.api().ping(request)
        }

        if (response.code() != 200) {
            throw CriticException("Ping failed with response code: ${response.code()}")
        }

        val pingResponse = response.body()
            ?: throw CriticException("No response returned from ping.")

        appInstallId = pingResponse.appInstall.id
        Log.d(TAG, "App install ID: $appInstallId")
    }

    private fun buildMetadataJson(): JsonObject {
        val map = mutableMapOf<String, kotlinx.serialization.json.JsonElement>()
        for ((key, value) in productMetadata) {
            map[key] = JsonPrimitive(value)
        }
        return JsonObject(map)
    }

    @Synchronized
    private fun getApplicationInstanceId(): String {
        val prefKey = "PREF_UNIQUE_ID"
        val prefs = application.getSharedPreferences(prefKey, Context.MODE_PRIVATE)
        return prefs.getString(prefKey, null) ?: UUID.randomUUID().toString().also {
            prefs.edit().putString(prefKey, it).apply()
        }
    }

    private fun registerBatteryReceiver() {
        application.registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val healthExtra = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)
                batteryHealth = when (healthExtra) {
                    BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
                    BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
                    BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
                    BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
                    BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheat"
                    BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "Unspecified Failure"
                    else -> "Unknown"
                }

                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                batteryLevel = if (scale > 0) level / scale.toFloat() * 100 else null

                val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                batteryCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL
            }
        }, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    }

    private fun resolveContentType(filename: String): String {
        val extension = filename.substringAfterLast('.', "").lowercase()
        if (extension.isEmpty()) return "*/*"
        val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
        if (mime != null) return mime
        if (extension == "log") return "text/plain"
        return "*/*"
    }

    private class ShakeListener : ShakeDetector.Listener {
        override fun hearShake() {
            val activity = lifecycleTracker.currentActivity ?: run {
                Log.w(TAG, "No current activity, ignoring shake.")
                return
            }

            if (lifecycleTracker.isDialogShowing) {
                Log.w(TAG, "Dialog already showing, ignoring shake.")
                return
            }

            lifecycleTracker.isDialogShowing = true

            MaterialAlertDialogBuilder(activity)
                .setTitle("Easy, easy!")
                .setMessage("Do you want to send us feedback?")
                .setPositiveButton("Yes") { _, _ ->
                    showFeedbackReportActivity()
                    lifecycleTracker.isDialogShowing = false
                }
                .setNegativeButton("No") { _, _ ->
                    lifecycleTracker.isDialogShowing = false
                }
                .setOnCancelListener {
                    lifecycleTracker.isDialogShowing = false
                }
                .setOnDismissListener {
                    lifecycleTracker.isDialogShowing = false
                }
                .show()
        }
    }

    private class ApplicationLifecycleTracker : Application.ActivityLifecycleCallbacks {
        var currentActivity: Activity? = null
        var isDialogShowing: Boolean = false

        override fun onActivityResumed(activity: Activity) {
            currentActivity = activity
        }

        override fun onActivityPaused(activity: Activity) {
            currentActivity = null
            isDialogShowing = false
        }

        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
        override fun onActivityStarted(activity: Activity) {}
        override fun onActivityStopped(activity: Activity) {}
        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
        override fun onActivityDestroyed(activity: Activity) {}
    }

    class CriticException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
}
