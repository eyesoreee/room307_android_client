package com.example.room307.nodes.data.remote

import retrofit2.Response
import retrofit2.http.GET

interface NodeApi {
    @GET("/api/v1/nodes")
    suspend fun getAllNodes(): Response<List<NodeDto>>
}