package com.piconnect.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.piconnect.app.data.api.Device
import com.piconnect.app.data.repository.PiConnectRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class DeviceListUiState {
    object Loading : DeviceListUiState()
    object Empty : DeviceListUiState()
    data class Success(val devices: List<Device>) : DeviceListUiState()
    data class Error(val message: String) : DeviceListUiState()
}

class DeviceListViewModel(
    private val repository: PiConnectRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<DeviceListUiState>(DeviceListUiState.Loading)
    val uiState: StateFlow<DeviceListUiState> = _uiState.asStateFlow()

    fun loadDevices() {
        viewModelScope.launch {
            _uiState.value = DeviceListUiState.Loading
            val result = repository.getDevices()
            _uiState.value = result.fold(
                onSuccess = { devices ->
                    if (devices.isEmpty()) DeviceListUiState.Empty
                    else DeviceListUiState.Success(devices)
                },
                onFailure = { DeviceListUiState.Error(it.message ?: "Failed to load devices") }
            )
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.clearToken()
        }
    }
}
