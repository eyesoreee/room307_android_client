package com.example.room307.files.data.remote

import com.example.room307.files.domain.model.FileItem
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FileDto(
    @SerialName("id")
    val id: String? = null,
    @SerialName("filename")
    val name: String? = null,
    @SerialName("size")
    val size: Long? = 0L,
    @SerialName("uploaded_at")
    val uploadedAt: Double? = 0.0,
    @SerialName("status")
    val status: String? = null
)

fun FileDto.toFileItem(): FileItem {
    return FileItem(
        id = id ?: "",
        name = name ?: "Unknown",
        size = size ?: 0L,
        uploadedAt = uploadedAt ?: 0.0,
        status = status ?: "unknown"
    )
}
