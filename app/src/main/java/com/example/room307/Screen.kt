package com.example.room307

sealed class Screen {
    data object Files : Screen()
    data object Nodes : Screen()
    data object Settings : Screen()
}