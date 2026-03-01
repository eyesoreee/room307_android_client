package com.example.room307.nodes.data.remote

import com.example.room307.nodes.domain.model.NodeItem
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
)

fun NodeDto.toNodeItem(): NodeItem {
    return NodeItem(
        id = id ?: "",
        ip = ip ?: "0.0.0.0",
        port = port ?: 7001,
        status = status,
        latency = latency ?: -1.0,
        uptime = uptime ?: 0.0,
        used = used ?: 0L,
        total = total ?: 0L
    )
}
