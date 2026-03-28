package io.inventiv.critic.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Attachment(
    @SerialName("id") val id: String,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("file_file_name") val fileFileName: String? = null,
    @SerialName("file_content_type") val fileContentType: String? = null,
    @SerialName("file_file_size") val fileFileSize: Long? = null,
    @SerialName("file_updated_at") val fileUpdatedAt: String? = null,
    @SerialName("url") val fileUrl: String? = null,
)
