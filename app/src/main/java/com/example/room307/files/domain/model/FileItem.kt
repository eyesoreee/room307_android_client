package com.example.room307.files.domain.model

import androidx.compose.runtime.Immutable
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Immutable
data class FileItem(
    val id: String,
    val name: String,
    val size: Long,
    val uploadedAt: Double,
    val status: String
) {
    val formattedDate: String by lazy {
        if (uploadedAt == 0.0) return@lazy "Unknown Date"
        val instant = Instant.ofEpochSecond(uploadedAt.toLong())
        val zonedDateTime = instant.atZone(ZoneId.systemDefault())
        DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm", Locale.getDefault()).format(zonedDateTime)
    }

    val formattedSize: String by lazy {
        when {
            size < 1024 -> "$size B"
            size < 1024 * 1024 -> "${size / 1024} KB"
            else -> "%.2f MB".format(size / (1024.0 * 1024.0))
        }
    }
}
