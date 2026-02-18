package com.example.room307.nodes.presentation

sealed class NodeAction {
    object LoadNodes : NodeAction()
    object RefreshNodes : NodeAction()
    data class SearchNodes(val query: String) : NodeAction()
}
