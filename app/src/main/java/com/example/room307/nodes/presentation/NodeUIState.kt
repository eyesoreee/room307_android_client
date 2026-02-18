package com.example.room307.nodes.presentation

import com.example.room307.nodes.data.remote.NodeDto

data class NodeUIState(
    val nodes: List<NodeDto> = emptyList(),
    val displayedNodes: List<NodeDto> = emptyList(),
    val searchQuery: String = "",
    val isRefreshing: Boolean = false,
)