package com.piconnect.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.piconnect.app.ui.screens.connect.PiConnectScreen
import com.piconnect.app.ui.screens.device.AddDeviceScreen
import com.piconnect.app.ui.screens.home.HomeScreen
import com.piconnect.app.ui.screens.ssh.SshTerminalScreen

@Composable
fun NavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onAddDevice = { navController.navigate(Screen.AddDevice.route) },
                onConnectSsh = { deviceId ->
                    navController.navigate(Screen.SshTerminal.createRoute(deviceId))
                },
                onOpenPiConnect = { navController.navigate(Screen.PiConnect.route) }
            )
        }

        composable(Screen.AddDevice.route) {
            AddDeviceScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.SshTerminal.route,
            arguments = listOf(navArgument("deviceId") { type = NavType.StringType })
        ) { backStackEntry ->
            val deviceId = backStackEntry.arguments?.getString("deviceId") ?: return@composable
            SshTerminalScreen(
                deviceId = deviceId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.PiConnect.route) {
            PiConnectScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
