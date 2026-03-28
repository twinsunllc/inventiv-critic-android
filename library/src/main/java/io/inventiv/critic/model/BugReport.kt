package io.inventiv.critic.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class BugReport(
    @SerialName("id") val id: String,
    @SerialName("description") val description: String? = null,
    @SerialName("steps_to_reproduce") val stepsToReproduce: String? = null,
    @SerialName("user_identifier") val userIdentifier: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("metadata") val metadata: JsonElement? = null,
    @SerialName("attachments") val attachments: List<Attachment>? = null,
    @SerialName("device") val device: Device? = null,
    @SerialName("app") val app: App? = null,
)

