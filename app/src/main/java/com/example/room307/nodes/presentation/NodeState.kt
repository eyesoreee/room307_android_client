package com.example.room307.nodes.presentation

sealed interface NodeState {
    object Idle : NodeState
    object Loading : NodeState
    data class Error(val message: String) : NodeState
    data class Ready(val uiState: NodeUIState) : NodeState
}