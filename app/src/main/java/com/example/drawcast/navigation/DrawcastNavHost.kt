package com.amishsxt.drawcast.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.amishsxt.drawcast.ui.call.CallScreen
import com.amishsxt.drawcast.ui.history.HistoryScreen
import com.amishsxt.drawcast.ui.home.HomeScreen
import com.amishsxt.drawcast.ui.settings.SettingsScreen

@Composable
fun DrawcastNavHost() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val showBottomBar = currentRoute?.startsWith("call/") == false

    Scaffold(
        bottomBar = {
            if (showBottomBar) BottomNavBar(navController)
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(if (showBottomBar) innerPadding else PaddingValues())
        ) {
            composable("home") {
                HomeScreen(
                    onCreateRoom = { roomId ->
                        navController.navigate("call/$roomId/true")
                    },
                    onJoinRoom = { roomId ->
                        navController.navigate("call/$roomId/false")
                    },
                    onViewHistory = {
                        navController.navigate("history") {
                            popUpTo("home") { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
            composable("history") { HistoryScreen() }
            composable("settings") { SettingsScreen() }
            composable("call/{roomId}/{isExpert}") { backStack ->
                val roomId = backStack.arguments?.getString("roomId") ?: ""
                val isExpert = backStack.arguments?.getString("isExpert") == "true"
                CallScreen(
                    roomId = roomId,
                    isExpert = isExpert,
                    onClose = { navController.popBackStack() }
                )
            }
        }
    }
}
