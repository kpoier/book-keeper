package com.example.book_keeper.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.book_keeper.ui.screens.HomeScreen
import com.example.book_keeper.ui.screens.RecordsScreen
import com.example.book_keeper.ui.screens.SettingsScreen

/**
 * 應用程式的主要佈局框架。
 * 
 * @param onLogout 當使用者點擊登出時的回調。
 * @param onLanguageChange 當使用者更改語言時的回調。
 * @param onThemeChange 當使用者更改主題時的回調。
 */
@Composable
fun MainScaffold(
    onLogout: () -> Unit, 
    onLanguageChange: (String) -> Unit,
    onThemeChange: (String) -> Unit
) {
    val navController = rememberNavController()
    val items = listOf(Screen.Home, Screen.Records, Screen.Settings)

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                items.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = null) },
                        label = { Text(stringResource(screen)) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen()
            }
            composable(Screen.Records.route) {
                RecordsScreen()
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    onLogout = onLogout, 
                    onLanguageChange = onLanguageChange,
                    onThemeChange = onThemeChange
                )
            }
        }
    }
}

/**
 * 根據 Screen 物件獲取對應的資源字串。
 */
@Composable
fun stringResource(screen: Screen): String {
    return when (screen) {
        Screen.Home -> androidx.compose.ui.res.stringResource(com.example.book_keeper.R.string.nav_home)
        Screen.Records -> androidx.compose.ui.res.stringResource(com.example.book_keeper.R.string.nav_records)
        Screen.Settings -> androidx.compose.ui.res.stringResource(com.example.book_keeper.R.string.nav_settings)
    }
}
