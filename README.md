# Inventiv Critic Android SDK

Use this library to add [Inventiv Critic](https://inventiv.io/critic/) bug reporting to your Android app.

![Critic Android default feedback screen](https://assets.inventiv.io/github/inventiv-critic-android/critic-android-half-shot-feedback-screen.png)

## Requirements

- Android SDK 24+ (Android 7.0)
- Kotlin 2.0+

## Installation

### JitPack

Add the JitPack repository to your root `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

Add the dependency to your module's `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.github.twinsunllc:inventiv-critic-android:2.0.0")
}
```

## Getting Started

### 1. Initialize the SDK

Find your Product Access Token in the [Critic Web Portal](https://critic.inventiv.io/products) and
initialize Critic in your `Application.onCreate()`:

```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Critic.initialize(this, "YOUR_PRODUCT_ACCESS_TOKEN")
    }
}
```

You can optionally provide a custom base URL:

```kotlin
Critic.initialize(this, "YOUR_PRODUCT_ACCESS_TOKEN", baseUrl = "https://custom.example.com/api/v3/")
```

### 2. Shake-to-Report (Enabled by Default)

By default, Critic prompts users for feedback when they shake their device. Disable it if desired:

```kotlin
Critic.stopShakeDetection()
```

Re-enable it at any time:

```kotlin
Critic.startShakeDetection()
```

### 3. Show Feedback Screen Programmatically

```kotlin
Critic.showFeedbackReportActivity()
```

## Submitting Reports Programmatically

Use `submitReport` to build and submit bug reports from your own UI:

```kotlin
import io.inventiv.critic.Critic
import kotlinx.coroutines.launch

// In a coroutine scope (e.g., viewModelScope, lifecycleScope):
lifecycleScope.launch {
    try {
        val report = Critic.submitReport(
            description = "The app crashes when I tap the save button.",
            stepsToReproduce = "1. Open settings\n2. Change name\n3. Tap save",
            userIdentifier = "user@example.com",
            attachments = listOf(screenshotFile),
        )
        println("Report submitted: ${report.id}")
    } catch (e: Critic.CriticException) {
        println("Submission failed: ${e.message}")
    }
}
```

## Product Metadata

Attach custom metadata to every report:

```kotlin
Critic.setProductMetadata(mapOf(
    "user_email" to "user@example.com",
    "plan" to "premium",
    "build_flavor" to "staging",
))
```

> **Note:** Keys starting with `ic_` are reserved and will throw an `IllegalArgumentException`.

## v3 API: Querying Reports and Devices

The SDK exposes suspend functions for the Critic v3 read API. These are intended for admin dashboards,
automation, or backend integrations — not typical end-user flows.

All methods require an `appApiToken` (your Product Access Token).

### List Bug Reports

```kotlin
val response = Critic.listBugReports(
    appApiToken = "YOUR_PRODUCT_ACCESS_TOKEN",
    archived = false,            // optional: filter by archived status
    deviceId = "device-uuid",    // optional: filter by device
    since = "2026-01-01T00:00:00Z",  // optional: only reports after this date
)

println("Total reports: ${response.count}")
println("Page ${response.currentPage} of ${response.totalPages}")
for (report in response.bugReports) {
    println("${report.id}: ${report.description}")
}
```

### Get a Single Bug Report

```kotlin
val report = Critic.getBugReport(
    appApiToken = "YOUR_PRODUCT_ACCESS_TOKEN",
    id = "f47ac10b-58cc-4372-a567-0e02b2c3d479",
)

println("Description: ${report.description}")
println("Steps: ${report.stepsToReproduce}")
println("Attachments: ${report.attachments?.size ?: 0}")
println("Device: ${report.device?.manufacturer} ${report.device?.model}")
```

### List Devices

```kotlin
val response = Critic.listDevices(
    appApiToken = "YOUR_PRODUCT_ACCESS_TOKEN",
)

println("Total devices: ${response.count}")
for (device in response.devices) {
    println("${device.manufacturer} ${device.model} (${device.platformVersion})")
}
```

### Error Handling

All v3 API methods throw `Critic.CriticException` on failure (non-2xx response or missing body):

```kotlin
try {
    val report = Critic.getBugReport(appApiToken = "token", id = "uuid")
} catch (e: Critic.CriticException) {
    // e.g., "getBugReport failed with response code: 404"
    Log.e("Critic", "API call failed", e)
}
```

## Viewing Reports

Visit the [Critic web portal](https://critic.inventiv.io/) to view submitted reports.

![Critic Android app info](https://assets.inventiv.io/github/inventiv-critic-android/critic-android-app-info.png)
![Critic Android device info](https://assets.inventiv.io/github/inventiv-critic-android/critic-android-device-info.png)

## License

This library is released under the MIT License.
