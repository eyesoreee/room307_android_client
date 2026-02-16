package com.example.room307.files.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Serializable
data class FileDto(
    @SerialName("id")
    val id: String? = null,

    @SerialName("filename")
    val name: String? = null,

    @SerialName("size")
    val size: Long? = 0L,

    @SerialName("uploaded_at")
    val date: Double? = 0.0,

    @SerialName("status")
    val status: String? = null
) {
    companion object {
        private val dateFormatter: DateTimeFormatter by lazy {
            DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm", Locale.getDefault())
        }
    }

    fun getFormattedDate(): String {
        val timestamp = date ?: return "Unknown Date"
        if (timestamp == 0.0) return "Unknown Date"

        val instant = Instant.ofEpochMilli((timestamp * 1000).toLong())
        val zonedDateTime = instant.atZone(ZoneId.systemDefault())

        return dateFormatter.format(zonedDateTime)
    }

    fun getFormattedSize(): String {
        val s = size ?: 0L
        return when {
            s < 1024 -> "$s B"
            s < 1024 * 1024 -> "${s / 1024} KB"
            else -> {
                val mb = s / (1024.0 * 1024.0)
                "%.2f MB".format(mb)
            }
        }
    }
}