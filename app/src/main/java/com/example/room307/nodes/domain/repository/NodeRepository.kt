package com.example.room307.nodes.domain.repository

import com.example.room307.nodes.data.remote.NodeDto

interface NodeRepository {
    suspend fun getAllNodes(): Result<List<NodeDto>>
}