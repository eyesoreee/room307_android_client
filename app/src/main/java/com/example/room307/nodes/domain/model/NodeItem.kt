package com.example.room307.nodes.domain.model

import androidx.compose.runtime.Immutable
import com.example.room307.nodes.data.remote.NodeStatus

@Immutable
data class NodeItem(
    val id: String,
    val ip: String,
    val port: Int,
    val status: NodeStatus,
    val latency: Double,
    val uptime: Double,
    val used: Long,
    val total: Long
) {
    val formattedUptime: String by lazy {
        val totalSeconds = uptime.toLong()
        if (totalSeconds <= 0) return@lazy "N/A"
        
        val days = totalSeconds / (24 * 3600)
        val hours = (totalSeconds % (24 * 3600)) / 3600
        val minutes = (totalSeconds % 3600) / 60

        buildString {
            if (days > 0) append("${days}d ")
            if (hours > 0) append("${hours}h ")
            if (minutes > 0) append("${minutes}m")
        }.trim().ifEmpty { "${totalSeconds}s" }
    }

    val formattedLatency: String by lazy {
        if (latency < 0) "N/A" else "${latency.toInt()} ms"
    }

    val formattedDiskUsage: String by lazy {
        if (total <= 0) return@lazy "N/A"
        val usedGB = used / (1024.0 * 1024.0 * 1024.0)
        val totalGB = total / (1024.0 * 1024.0 * 1024.0)
        "%.1f GB / %.1f GB".format(usedGB, totalGB)
    }

    val diskUsagePercentage: Float by lazy {
        if (total <= 0) 0f else (used.toFloat() / total.toFloat())
    }
}
