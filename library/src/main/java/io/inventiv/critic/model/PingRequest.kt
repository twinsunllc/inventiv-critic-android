package io.inventiv.critic.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PingRequest(
    @SerialName("api_token") val apiToken: String,
    @SerialName("app") val app: AppInfo,
    @SerialName("device") val device: DeviceInfo,
    @SerialName("device_status") val deviceStatus: DeviceStatus,
)

@Serializable
data class AppInfo(
    @SerialName("name") val name: String,
    @SerialName("package") val packageName: String,
    @SerialName("platform") val platform: String = "Android",
    @SerialName("version") val version: AppVersion,
)

@Serializable
data class AppVersion(
    @SerialName("code") val code: Int,
    @SerialName("name") val name: String,
)

@Serializable
data class DeviceInfo(
    @SerialName("identifier") val identifier: String,
    @SerialName("manufacturer") val manufacturer: String,
    @SerialName("model") val model: String,
    @SerialName("network_carrier") val networkCarrier: String,
    @SerialName("platform") val platform: String = "Android",
    @SerialName("platform_version") val platformVersion: String,
)
