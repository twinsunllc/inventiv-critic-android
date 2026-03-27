package io.inventiv.critic.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class App(
    @SerialName("id") val id: String? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("package") val packageName: String? = null,
    @SerialName("platform") val platform: String? = null,
    @SerialName("version") val version: AppVersion? = null,
)
