package com.piconnect.app.data.model

import java.util.UUID

data class PiDevice(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val host: String,
    val port: Int = 22,
    val username: String,
    val authMethod: AuthMethod = AuthMethod.PASSWORD,
    val password: String? = null,
    val privateKey: String? = null,
    val useRpiConnect: Boolean = false
)

enum class AuthMethod {
    PASSWORD, KEY
}
