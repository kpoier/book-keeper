package com.example.book_keeper.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Home : Screen("home", "主頁", Icons.Default.Home)
    object Records : Screen("records", "紀錄", Icons.Default.DateRange)
    object Settings : Screen("settings", "設置", Icons.Default.Settings)
}