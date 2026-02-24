package com.piconnect.app.ui.screens.ssh

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jcraft.jsch.ChannelShell
import com.jcraft.jsch.Session
import com.piconnect.app.data.model.ConnectionState
import com.piconnect.app.data.repository.DeviceRepository
import com.piconnect.app.data.ssh.SshManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream

class SshViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DeviceRepository(application)
    private val sshManager = SshManager()

    private var session: Session? = null
    private var shellChannel: ChannelShell? = null
    private var shellOutputStream: OutputStream? = null

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _terminalOutput = MutableStateFlow("")
    val terminalOutput: StateFlow<String> = _terminalOutput.asStateFlow()

    fun connect(deviceId: String) {
        viewModelScope.launch {
            _connectionState.value = ConnectionState.Connecting
            _terminalOutput.value = "Connecting...\n"

            withContext(Dispatchers.IO) {
                val device = repository.getDevice(deviceId)
                if (device == null) {
                    _connectionState.value = ConnectionState.Error("Device not found")
                    return@withContext
                }

                val sessionResult = sshManager.connect(device)
                sessionResult.fold(
                    onSuccess = { connectedSession ->
                        session = connectedSession
                        val shellResult = sshManager.startShell(connectedSession)
                        shellResult.fold(
                            onSuccess = { channel ->
                                shellChannel = channel as ChannelShell
                                shellOutputStream = channel.outputStream
                                _connectionState.value = ConnectionState.Connected(
                                    "${device.username}@${device.host}"
                                )
                                readShellOutput(channel.inputStream)
                            },
                            onFailure = { error ->
                                _connectionState.value = ConnectionState.Error(
                                    error.message ?: "Failed to open shell"
                                )
                            }
                        )
                    },
                    onFailure = { error ->
                        _connectionState.value = ConnectionState.Error(
                            error.message ?: "Connection failed"
                        )
                        _terminalOutput.value += "Error: ${error.message}\n"
                    }
                )
            }
        }
    }

    private fun readShellOutput(inputStream: InputStream) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val buffer = ByteArray(1024)
                while (shellChannel?.isClosed == false) {
                    val available = inputStream.available()
                    if (available > 0) {
                        val bytesRead = inputStream.read(buffer, 0, minOf(available, buffer.size))
                        if (bytesRead > 0) {
                            val output = String(buffer, 0, bytesRead)
                            _terminalOutput.value += output
                        }
                    } else {
                        delay(100)
                    }
                }
            } catch (_: Exception) {
                // Stream closed
            }
        }
    }

    fun sendCommand(command: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                shellOutputStream?.write("$command\n".toByteArray())
                shellOutputStream?.flush()
            } catch (e: Exception) {
                _terminalOutput.value += "Error sending command: ${e.message}\n"
            }
        }
    }

    fun disconnect() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                shellChannel?.disconnect()
                session?.let { sshManager.disconnect(it) }
            } catch (_: Exception) {
                // Ignore disconnect errors
            } finally {
                shellChannel = null
                shellOutputStream = null
                session = null
                _connectionState.value = ConnectionState.Disconnected
                _terminalOutput.value += "\nDisconnected.\n"
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Synchronously clean up SSH resources to ensure they are released before ViewModel is destroyed
        runBlocking(Dispatchers.IO) {
            try {
                shellChannel?.disconnect()
                session?.let { sshManager.disconnect(it) }
            } catch (_: Exception) {
                // Ignore cleanup errors
            }
        }
    }
}
