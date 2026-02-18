package com.example.room307.nodes.data.repository

import com.example.room307.nodes.data.remote.NodeApi
import com.example.room307.nodes.data.remote.NodeDto
import com.example.room307.nodes.domain.repository.NodeRepository
import jakarta.inject.Inject

class NodeRepositoryImp @Inject constructor(
    private val api: NodeApi
) : NodeRepository {
    override suspend fun getAllNodes(): Result<List<NodeDto>> {
        return try {
            val response = api.getAllNodes()

            if (response.isSuccessful && response.body() != null)
                Result.success(response.body()!!)
            else
                Result.failure(Exception("Failed to fetch nodes"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}