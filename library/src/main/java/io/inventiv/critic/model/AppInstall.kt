package io.inventiv.critic.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AppInstall(
    @SerialName("id") val id: String,
)

@Serializable
data class PingResponse(
    @SerialName("app_install") val appInstall: AppInstall,
)
