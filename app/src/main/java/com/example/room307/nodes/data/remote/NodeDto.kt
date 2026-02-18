package com.example.room307.nodes.data.remote

import androidx.compose.runtime.Immutable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class NodeStatus {
    @SerialName("ONLINE")
    ONLINE,

    @SerialName("OFFLINE")
    OFFLINE,

    @SerialName("UNKNOWN")
    UNKNOWN,

    @SerialName("ERROR")
    ERROR,
}

@Immutable
@Serializable
data class NodeDto(
    @SerialName("id")
    val id: String? = null,

    @SerialName("ip")
    val ip: String? = null,

    @SerialName("port")
    val port: Int? = 7001,

    @SerialName("status")
    val status: NodeStatus = NodeStatus.UNKNOWN,

    @SerialName("latency")
    val latency: Double? = -1.0,

    @SerialName("uptime")
    val uptime: Double? = 0.0,

    @SerialName("used")
    val used: Long? = 0L,

    @SerialName("total")
    val total: Long? = 0L,
) {
    fun getFormattedUptime(): String {
        val totalSeconds = (uptime ?: 0.0).toLong()
        if (totalSeconds <= 0) return "N/A"
        if (totalSeconds < 60) return "${totalSeconds}s"

        val days = totalSeconds / (24 * 3600)
        val hours = (totalSeconds % (24 * 3600)) / 3600
        val minutes = (totalSeconds % 3600) / 60

        return buildString {
            if (days > 0) append("${days}d ")
            if (hours > 0) append("${hours}h ")
            if (minutes > 0) append("${minutes}m")
        }.trim()
    }

    fun getFormattedLatency(): String {
        val lat = latency ?: -1.0
        return if (lat < 0) "N/A" else "${lat.toInt()} ms"
    }

    fun getFormattedDiskUsage(): String {
        val usedBytes = used ?: 0L
        val totalBytes = total ?: 0L
        if (totalBytes <= 0) return "N/A"

        val usedGB = usedBytes / (1024.0 * 1024.0 * 1024.0)
        val totalGB = totalBytes / (1024.0 * 1024.0 * 1024.0)

        return "%.1f GB / %.1f GB".format(usedGB, totalGB)
    }

    fun getDiskUsagePercentage(): Float {
        val usedBytes = used ?: 0L
        val totalBytes = total ?: 1L
        if (totalBytes <= 0) return 0f
        return (usedBytes.toFloat() / totalBytes.toFloat())
    }
}
