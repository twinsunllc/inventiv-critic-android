package io.inventiv.critic.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DeviceStatus(
    @SerialName("battery_charging") val batteryCharging: Boolean? = null,
    @SerialName("battery_health") val batteryHealth: String? = null,
    @SerialName("battery_level") val batteryLevel: Float? = null,
    @SerialName("disk_free") val diskFree: Long? = null,
    @SerialName("disk_total") val diskTotal: Long? = null,
    @SerialName("disk_usable") val diskUsable: Long? = null,
    @SerialName("memory_free") val memoryFree: Long? = null,
    @SerialName("memory_total") val memoryTotal: Long? = null,
    @SerialName("network_cell_connected") val networkCellConnected: Boolean? = null,
    @SerialName("network_wifi_connected") val networkWifiConnected: Boolean? = null,
)
