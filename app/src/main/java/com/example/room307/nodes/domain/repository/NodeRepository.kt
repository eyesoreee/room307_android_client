package com.example.room307.nodes.domain.repository

import com.example.room307.nodes.domain.model.NodeItem

interface NodeRepository {
    suspend fun getAllNodes(): Result<List<NodeItem>>
    suspend fun testConnection(ip: String, port: String): Result<Int>
}
