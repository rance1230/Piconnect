package com.piconnect.app.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.piconnect.app.data.api.Device
import com.piconnect.app.data.api.LoginRequest
import com.piconnect.app.data.api.PiConnectApiService
import com.piconnect.app.data.api.TunnelRequest
import com.piconnect.app.data.api.TunnelResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "piconnect_prefs")

class PiConnectRepository(private val context: Context) {

    private val apiService = PiConnectApiService.create()

    private object Keys {
        val AUTH_TOKEN = stringPreferencesKey("auth_token")
        val USER_ID = stringPreferencesKey("user_id")
    }

    val authToken: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[Keys.AUTH_TOKEN]
    }

    suspend fun login(username: String, password: String): Result<String> {
        return try {
            val response = apiService.login(LoginRequest(username, password))
            if (response.isSuccessful) {
                val loginResponse = response.body()!!
                saveToken(loginResponse.token, loginResponse.userId)
                Result.success(loginResponse.token)
            } else {
                when (response.code()) {
                    401 -> Result.failure(Exception("Invalid credentials"))
                    403 -> Result.failure(Exception("Account locked"))
                    else -> Result.failure(Exception("Login failed: ${response.code()}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network error: ${e.message}"))
        }
    }

    suspend fun getDevices(): Result<List<Device>> {
        return try {
            val token = getToken() ?: return Result.failure(Exception("Not authenticated"))
            val response = apiService.getDevices("Bearer $token")
            if (response.isSuccessful) {
                Result.success(response.body()?.devices ?: emptyList())
            } else {
                when (response.code()) {
                    401 -> {
                        clearToken()
                        Result.failure(Exception("Session expired. Please login again."))
                    }
                    else -> Result.failure(Exception("Failed to load devices: ${response.code()}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network error: ${e.message}"))
        }
    }

    suspend fun createTunnel(deviceId: String): Result<TunnelResponse> {
        return try {
            val token = getToken() ?: return Result.failure(Exception("Not authenticated"))
            val response = apiService.createTunnel("Bearer $token", deviceId, TunnelRequest())
            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to create tunnel: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network error: ${e.message}"))
        }
    }

    suspend fun getToken(): String? {
        return context.dataStore.data.first()[Keys.AUTH_TOKEN]
    }

    private suspend fun saveToken(token: String, userId: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.AUTH_TOKEN] = token
            prefs[Keys.USER_ID] = userId
        }
    }

    suspend fun clearToken() {
        context.dataStore.edit { prefs ->
            prefs.remove(Keys.AUTH_TOKEN)
            prefs.remove(Keys.USER_ID)
        }
    }

    suspend fun isLoggedIn(): Boolean = getToken() != null
}
