package com.piconnect.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.piconnect.app.data.repository.PiConnectRepository
import com.piconnect.app.ui.screens.DeviceListScreen
import com.piconnect.app.ui.screens.LoginScreen
import com.piconnect.app.ui.screens.SshTerminalScreen
import com.piconnect.app.viewmodel.DeviceListViewModel
import com.piconnect.app.viewmodel.LoginViewModel
import com.piconnect.app.viewmodel.SshTerminalViewModel

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object DeviceList : Screen("devices")
    object SshTerminal : Screen("ssh/{deviceId}/{deviceName}") {
        fun createRoute(deviceId: String, deviceName: String) = "ssh/$deviceId/$deviceName"
    }
}

@Composable
fun NavGraph() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val repository = PiConnectRepository(context)

    NavHost(navController = navController, startDestination = Screen.Login.route) {
        composable(Screen.Login.route) {
            val viewModel = LoginViewModel(repository)
            LoginScreen(
                viewModel = viewModel,
                onLoginSuccess = {
                    navController.navigate(Screen.DeviceList.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.DeviceList.route) {
            val viewModel = DeviceListViewModel(repository)
            DeviceListScreen(
                viewModel = viewModel,
                onDeviceSelected = { deviceId, deviceName ->
                    navController.navigate(Screen.SshTerminal.createRoute(deviceId, deviceName))
                },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.DeviceList.route) { inclusive = true }
                    }
                }
            )
        }
        composable(
            route = Screen.SshTerminal.route,
            arguments = listOf(
                navArgument("deviceId") { type = NavType.StringType },
                navArgument("deviceName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val deviceId = backStackEntry.arguments?.getString("deviceId") ?: ""
            val deviceName = backStackEntry.arguments?.getString("deviceName") ?: ""
            val viewModel = SshTerminalViewModel(repository, deviceId)
            SshTerminalScreen(
                viewModel = viewModel,
                deviceName = deviceName,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
