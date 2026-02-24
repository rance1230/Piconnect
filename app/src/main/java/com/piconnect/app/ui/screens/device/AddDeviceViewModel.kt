package com.piconnect.app.ui.screens.device

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.piconnect.app.data.model.AuthMethod
import com.piconnect.app.data.model.PiDevice
import com.piconnect.app.data.repository.DeviceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AddDeviceViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DeviceRepository(application)

    private val _name = MutableStateFlow("")
    val name: StateFlow<String> = _name.asStateFlow()

    private val _host = MutableStateFlow("")
    val host: StateFlow<String> = _host.asStateFlow()

    private val _port = MutableStateFlow("22")
    val port: StateFlow<String> = _port.asStateFlow()

    private val _username = MutableStateFlow("pi")
    val username: StateFlow<String> = _username.asStateFlow()

    private val _authMethod = MutableStateFlow(AuthMethod.PASSWORD)
    val authMethod: StateFlow<AuthMethod> = _authMethod.asStateFlow()

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()

    private val _privateKey = MutableStateFlow("")
    val privateKey: StateFlow<String> = _privateKey.asStateFlow()

    private val _useRpiConnect = MutableStateFlow(false)
    val useRpiConnect: StateFlow<Boolean> = _useRpiConnect.asStateFlow()

    private val _isSaved = MutableStateFlow(false)
    val isSaved: StateFlow<Boolean> = _isSaved.asStateFlow()

    fun updateName(value: String) { _name.value = value }
    fun updateHost(value: String) { _host.value = value }
    fun updatePort(value: String) { _port.value = value }
    fun updateUsername(value: String) { _username.value = value }
    fun updateAuthMethod(value: AuthMethod) { _authMethod.value = value }
    fun updatePassword(value: String) { _password.value = value }
    fun updatePrivateKey(value: String) { _privateKey.value = value }
    fun updateUseRpiConnect(value: Boolean) { _useRpiConnect.value = value }

    fun isValid(): Boolean {
        return _name.value.isNotBlank() &&
                _host.value.isNotBlank() &&
                _username.value.isNotBlank() &&
                (_port.value.toIntOrNull() ?: 0) > 0
    }

    fun saveDevice() {
        if (!isValid()) return

        viewModelScope.launch {
            val device = PiDevice(
                name = _name.value.trim(),
                host = _host.value.trim(),
                port = _port.value.toIntOrNull() ?: 22,
                username = _username.value.trim(),
                authMethod = _authMethod.value,
                password = if (_authMethod.value == AuthMethod.PASSWORD) _password.value else null,
                privateKey = if (_authMethod.value == AuthMethod.KEY) _privateKey.value else null,
                useRpiConnect = _useRpiConnect.value
            )
            repository.saveDevice(device)
            _isSaved.value = true
        }
    }
}
