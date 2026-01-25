package com.example.room307.files.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FileDto(
    @SerialName("file_id")
    val id: String? = null,

    @SerialName("file_name")
    val name: String? = null,

    @SerialName("file_size")
    val size: String? = null,

    @SerialName("file_date")
    val date: String? = null,

    @SerialName("origin_node")
    val origin: String? = null
)