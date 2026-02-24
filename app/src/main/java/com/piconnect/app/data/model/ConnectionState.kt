package com.piconnect.app.data.model

sealed class ConnectionState {
    object Disconnected : ConnectionState()
    object Connecting : ConnectionState()
    data class Connected(val sessionInfo: String) : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}
