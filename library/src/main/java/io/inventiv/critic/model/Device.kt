package io.inventiv.critic.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Device(
    @SerialName("id") val id: String,
    @SerialName("identifier") val identifier: String? = null,
    @SerialName("manufacturer") val manufacturer: String? = null,
    @SerialName("model") val model: String? = null,
    @SerialName("network_carrier") val networkCarrier: String? = null,
    @SerialName("platform") val platform: String? = null,
    @SerialName("platform_version") val platformVersion: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)

@Serializable
data class DeviceListResponse(
    @SerialName("count") val count: Int,
    @SerialName("current_page") val currentPage: Int,
    @SerialName("total_pages") val totalPages: Int,
    @SerialName("devices") val devices: List<Device>,
)
