package com.example.room307.nodes.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.room307.ui.EmptyState
import com.example.room307.ui.ErrorState
import com.example.room307.ui.SearchBar
import kotlinx.coroutines.flow.collectLatest

@Composable
fun NodeScreen(
    modifier: Modifier = Modifier,
    viewModel: NodeViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is NodeEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    val onSearchQueryChanged = remember(viewModel) {
        { query: String -> viewModel.onAction(NodeAction.SearchNodes(query)) }
    }

    Box(modifier = modifier.fillMaxSize()) {
        when (val currentState = state) {
            is NodeState.Error -> {
                ErrorState(
                    message = currentState.message,
                    onRetry = { viewModel.onAction(NodeAction.LoadNodes) }
                )
            }

            is NodeState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            is NodeState.Ready -> {
                val uiState = currentState.uiState

                PullToRefreshBox(
                    modifier = Modifier.fillMaxSize(),
                    isRefreshing = uiState.isRefreshing,
                    onRefresh = { viewModel.onAction(NodeAction.RefreshNodes) }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        SearchBar(
                            searchQuery = uiState.searchQuery,
                            onSearchQueryChanged = onSearchQueryChanged,
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = "Search nodes by IP or Port..."
                        )

                        if (uiState.displayedNodes.isEmpty()) {
                            EmptyState(
                                title = "No nodes found",
                                message = if (uiState.searchQuery.isEmpty())
                                    "Your network seems empty. Make sure your nodes are running."
                                else
                                    "No nodes match your search query.",
                                icon = {
                                    Icon(
                                        imageVector = Icons.Outlined.Dns,
                                        contentDescription = null,
                                        modifier = Modifier.size(64.dp),
                                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                    )
                                }
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(bottom = 16.dp)
                            ) {
                                items(
                                    items = uiState.displayedNodes,
                                    key = { it.id }
                                ) { node ->
                                    NodeCard(
                                        node = node,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }
            }

            else -> {}
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
