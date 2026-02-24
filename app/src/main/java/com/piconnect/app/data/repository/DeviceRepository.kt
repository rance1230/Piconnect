package com.piconnect.app.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.piconnect.app.data.model.PiDevice
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "pi_devices")

class DeviceRepository(private val context: Context) {

    private val gson = Gson()
    private val devicesKey = stringPreferencesKey("devices")

    fun getDevices(): Flow<List<PiDevice>> {
        return context.dataStore.data.map { preferences ->
            val json = preferences[devicesKey] ?: "[]"
            val type = object : TypeToken<List<PiDevice>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        }
    }

    suspend fun saveDevice(device: PiDevice) {
        context.dataStore.edit { preferences ->
            val json = preferences[devicesKey] ?: "[]"
            val type = object : TypeToken<MutableList<PiDevice>>() {}.type
            val devices: MutableList<PiDevice> = gson.fromJson(json, type) ?: mutableListOf()

            val index = devices.indexOfFirst { it.id == device.id }
            if (index >= 0) {
                devices[index] = device
            } else {
                devices.add(device)
            }

            preferences[devicesKey] = gson.toJson(devices)
        }
    }

    suspend fun deleteDevice(deviceId: String) {
        context.dataStore.edit { preferences ->
            val json = preferences[devicesKey] ?: "[]"
            val type = object : TypeToken<MutableList<PiDevice>>() {}.type
            val devices: MutableList<PiDevice> = gson.fromJson(json, type) ?: mutableListOf()

            devices.removeAll { it.id == deviceId }
            preferences[devicesKey] = gson.toJson(devices)
        }
    }

    suspend fun getDevice(deviceId: String): PiDevice? {
        val devices = getDevices().first()
        return devices.find { it.id == deviceId }
    }
}
