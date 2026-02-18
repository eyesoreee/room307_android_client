package com.example.room307.nodes.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.room307.nodes.domain.repository.NodeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface NodeEvent {
    data class ShowSnackbar(val message: String) : NodeEvent
}

@HiltViewModel
class NodeViewModel @Inject constructor(
    private val repository: NodeRepository
) : ViewModel() {
    private val _state = MutableStateFlow<NodeState>(NodeState.Idle)
    val state: StateFlow<NodeState> = _state.asStateFlow()

    private val _events = Channel<NodeEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        loadNodes()
    }

    fun onAction(action: NodeAction) {
        when (action) {
            is NodeAction.LoadNodes -> loadNodes()
            is NodeAction.RefreshNodes -> refreshNodes()
            is NodeAction.SearchNodes -> searchNodes(action.query)
        }
    }

    private fun refreshNodes() {
        updateReadyState { it.copy(isRefreshing = true) }
        viewModelScope.launch {
            repository.getAllNodes()
                .onSuccess { nodes ->
                    updateReadyState { state ->
                        val filtered = if (state.searchQuery.isBlank()) nodes
                        else nodes.filter {
                            it.ip?.contains(state.searchQuery, ignoreCase = true) == true ||
                                    it.port?.toString()?.contains(state.searchQuery, ignoreCase = true) == true
                        }
                        state.copy(nodes = nodes, displayedNodes = filtered, isRefreshing = false)
                    }
                }
                .onFailure { e ->
                    updateReadyState { it.copy(isRefreshing = false) }
                    _events.trySend(NodeEvent.ShowSnackbar(e.message ?: "Refresh failed"))
                }
        }
    }

    private fun loadNodes() {
        _state.update { NodeState.Loading }
        viewModelScope.launch {
            repository.getAllNodes()
                .onSuccess { nodes ->
                    _state.update {
                        NodeState.Ready(NodeUIState(nodes = nodes, displayedNodes = nodes))
                    }
                }
                .onFailure { e ->
                    _state.update { NodeState.Error(e.message ?: "Failed to load nodes") }
                }
        }
    }

    private fun searchNodes(query: String) {
        updateReadyState { current ->
            val filtered = if (query.isBlank()) current.nodes
            else current.nodes.filter {
                it.ip?.contains(query, ignoreCase = true) == true ||
                        it.port?.toString()?.contains(query, ignoreCase = true) == true
            }
            current.copy(searchQuery = query, displayedNodes = filtered)
        }
    }

    private fun updateReadyState(transform: (NodeUIState) -> NodeUIState) {
        _state.update { current ->
            if (current is NodeState.Ready) {
                NodeState.Ready(transform(current.uiState))
            } else current
        }
    }
}
