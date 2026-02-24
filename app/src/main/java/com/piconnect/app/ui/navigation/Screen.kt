package com.piconnect.app.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object AddDevice : Screen("add_device")
    object SshTerminal : Screen("ssh_terminal/{deviceId}") {
        fun createRoute(deviceId: String) = "ssh_terminal/$deviceId"
    }
    object PiConnect : Screen("pi_connect")
}
