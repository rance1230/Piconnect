package com.piconnect.app.data.api

import com.google.gson.annotations.SerializedName

data class LoginRequest(
    @SerializedName("username") val username: String,
    @SerializedName("password") val password: String
)

data class LoginResponse(
    @SerializedName("token") val token: String,
    @SerializedName("userId") val userId: String,
    @SerializedName("expiresAt") val expiresAt: String? = null
)

data class Device(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("status") val status: String,
    @SerializedName("ipAddress") val ipAddress: String? = null,
    @SerializedName("model") val model: String? = null,
    @SerializedName("osVersion") val osVersion: String? = null,
    @SerializedName("lastSeen") val lastSeen: String? = null
)

data class DevicesResponse(
    @SerializedName("devices") val devices: List<Device>
)

data class TunnelRequest(
    @SerializedName("protocol") val protocol: String = "ssh",
    @SerializedName("port") val port: Int = 22
)

data class TunnelResponse(
    @SerializedName("websocketUrl") val websocketUrl: String,
    @SerializedName("sessionId") val sessionId: String,
    @SerializedName("expiresAt") val expiresAt: String? = null
)
