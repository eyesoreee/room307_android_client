package com.example.room307.nodes.presentation

import com.example.room307.nodes.domain.model.NodeItem

data class NodeUIState(
    val nodes: List<NodeItem> = emptyList(),
    val displayedNodes: List<NodeItem> = emptyList(),
    val searchQuery: String = "",
    val isRefreshing: Boolean = false,
)
