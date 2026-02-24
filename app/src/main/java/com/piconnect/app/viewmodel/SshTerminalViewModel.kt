package com.piconnect.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.piconnect.app.data.repository.PiConnectRepository
import com.piconnect.app.data.ssh.SshClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed class SshConnectionState {
    object Connecting : SshConnectionState()
    object Connected : SshConnectionState()
    object Disconnected : SshConnectionState()
    data class Error(val message: String) : SshConnectionState()
}

enum class TerminalLineType { COMMAND, OUTPUT, ERROR, SYSTEM }

data class TerminalLine(val text: String, val type: TerminalLineType)

class SshTerminalViewModel(
    private val repository: PiConnectRepository,
    private val deviceId: String
) : ViewModel() {

    private val _connectionState = MutableStateFlow<SshConnectionState>(SshConnectionState.Connecting)
    val connectionState: StateFlow<SshConnectionState> = _connectionState.asStateFlow()

    private val _terminalOutput = MutableStateFlow<List<TerminalLine>>(emptyList())
    val terminalOutput: StateFlow<List<TerminalLine>> = _terminalOutput.asStateFlow()

    private val _commandInput = MutableStateFlow("")
    val commandInput: StateFlow<String> = _commandInput.asStateFlow()

    private var sshClient: SshClient? = null

    fun onCommandInputChanged(value: String) {
        _commandInput.value = value
    }

    fun connect() {
        viewModelScope.launch {
            _connectionState.value = SshConnectionState.Connecting
            appendSystemMessage("Creating SSH tunnel via Raspberry Pi Connect…")

            val tunnelResult = repository.createTunnel(deviceId)
            tunnelResult.fold(
                onSuccess = { tunnel ->
                    appendSystemMessage("Tunnel established. Connecting via SSH…")
                    startSshSession(tunnel.websocketUrl)
                },
                onFailure = { error ->
                    _connectionState.value = SshConnectionState.Error(
                        error.message ?: "Failed to create tunnel"
                    )
                }
            )
        }
    }

    private fun startSshSession(websocketUrl: String) {
        // SSH credentials: use 'pi' as default user.
        // In a real app, you would prompt the user or store credentials securely.
        val sshUser = "pi"
        val sshPass = ""

        sshClient = SshClient(
            websocketUrl = websocketUrl,
            sshUsername = sshUser,
            sshPassword = sshPass,
            onOutput = { output ->
                appendOutput(output)
                if (_connectionState.value != SshConnectionState.Connected) {
                    _connectionState.value = SshConnectionState.Connected
                }
            },
            onError = { error ->
                appendErrorMessage(error)
                _connectionState.value = SshConnectionState.Error(error)
            },
            onDisconnected = {
                _connectionState.value = SshConnectionState.Disconnected
                appendSystemMessage("Session disconnected.")
            }
        )
        sshClient?.connect()
    }

    fun sendCommand() {
        val cmd = _commandInput.value.trim()
        if (cmd.isBlank()) return
        _commandInput.value = ""
        appendCommandLine(cmd)
        sshClient?.sendCommand(cmd)
    }

    fun disconnect() {
        sshClient?.disconnect()
    }

    private fun appendOutput(text: String) {
        val lines = text.split("\n").filter { it.isNotEmpty() }
        _terminalOutput.update { current ->
            current + lines.map { TerminalLine(it, TerminalLineType.OUTPUT) }
        }
    }

    private fun appendCommandLine(cmd: String) {
        _terminalOutput.update { current ->
            current + TerminalLine("$ $cmd", TerminalLineType.COMMAND)
        }
    }

    private fun appendErrorMessage(msg: String) {
        _terminalOutput.update { current ->
            current + TerminalLine("⚠ $msg", TerminalLineType.ERROR)
        }
    }

    private fun appendSystemMessage(msg: String) {
        _terminalOutput.update { current ->
            current + TerminalLine("• $msg", TerminalLineType.SYSTEM)
        }
    }

    override fun onCleared() {
        super.onCleared()
        disconnect()
    }
}
