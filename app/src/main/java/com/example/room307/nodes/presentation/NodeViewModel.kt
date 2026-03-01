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

    private fun loadNodes(isRefreshing: Boolean = false) {
        if (!isRefreshing) _state.update { NodeState.Loading }
        else updateReadyState { it.copy(isRefreshing = true) }

        viewModelScope.launch {
            repository.getAllNodes()
                .onSuccess { nodes ->
                    _state.update { current ->
                        val searchQuery = (current as? NodeState.Ready)?.uiState?.searchQuery ?: ""
                        val filtered = if (searchQuery.isBlank()) nodes
                        else nodes.filter {
                            it.ip.contains(searchQuery, ignoreCase = true) ||
                                    it.port.toString().contains(searchQuery, ignoreCase = true)
                        }
                        
                        NodeState.Ready(NodeUIState(
                            nodes = nodes,
                            displayedNodes = filtered,
                            searchQuery = searchQuery,
                            isRefreshing = false
                        ))
                    }
                }
                .onFailure { e ->
                    if (isRefreshing) {
                        updateReadyState { it.copy(isRefreshing = false) }
                        _events.trySend(NodeEvent.ShowSnackbar(e.message ?: "Refresh failed"))
                    } else {
                        _state.update { NodeState.Error(e.message ?: "Failed to load nodes") }
                    }
                }
        }
    }

    private fun refreshNodes() = loadNodes(isRefreshing = true)

    private fun searchNodes(query: String) {
        updateReadyState { current ->
            val filtered = if (query.isBlank()) current.nodes
            else current.nodes.filter {
                it.ip.contains(query, ignoreCase = true) ||
                        it.port.toString().contains(query, ignoreCase = true)
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
