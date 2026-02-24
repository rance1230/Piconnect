package com.piconnect.app.ui.screens.ssh

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.piconnect.app.R
import com.piconnect.app.data.model.ConnectionState
import com.piconnect.app.ui.theme.TerminalBackground
import com.piconnect.app.ui.theme.TerminalGreen
import com.piconnect.app.ui.theme.TerminalText
import com.piconnect.app.ui.theme.TerminalTypography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SshTerminalScreen(
    deviceId: String,
    onNavigateBack: () -> Unit,
    viewModel: SshViewModel = viewModel()
) {
    val connectionState by viewModel.connectionState.collectAsState()
    val terminalOutput by viewModel.terminalOutput.collectAsState()
    val scrollState = rememberScrollState()
    var commandInput by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.connect(deviceId)
    }

    LaunchedEffect(terminalOutput) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    val statusText = when (connectionState) {
        is ConnectionState.Disconnected -> stringResource(R.string.disconnected)
        is ConnectionState.Connecting -> stringResource(R.string.connecting)
        is ConnectionState.Connected -> (connectionState as ConnectionState.Connected).sessionInfo
        is ConnectionState.Error -> (connectionState as ConnectionState.Error).message
    }

    val statusColor = when (connectionState) {
        is ConnectionState.Connected -> TerminalGreen
        is ConnectionState.Error -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurface
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.ssh_terminal))
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.bodySmall,
                            color = statusColor
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.disconnect()
                        onNavigateBack()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.disconnect))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = TerminalBackground,
                    titleContentColor = TerminalText
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(TerminalBackground)
        ) {
            // Terminal output area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(8.dp)
            ) {
                Text(
                    text = terminalOutput,
                    style = TerminalTypography,
                    color = TerminalText
                )
            }

            // Command input
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TerminalBackground)
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = commandInput,
                    onValueChange = { commandInput = it },
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text(
                            stringResource(R.string.enter_command),
                            color = Color.Gray
                        )
                    },
                    textStyle = TerminalTypography.copy(color = TerminalGreen),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF2D2D2D),
                        unfocusedContainerColor = Color(0xFF2D2D2D),
                        focusedIndicatorColor = TerminalGreen,
                        cursorColor = TerminalGreen
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (commandInput.isNotBlank()) {
                                viewModel.sendCommand(commandInput)
                                commandInput = ""
                            }
                        }
                    ),
                    singleLine = true,
                    enabled = connectionState is ConnectionState.Connected
                )
                IconButton(
                    onClick = {
                        if (commandInput.isNotBlank()) {
                            viewModel.sendCommand(commandInput)
                            commandInput = ""
                        }
                    },
                    enabled = connectionState is ConnectionState.Connected
                ) {
                    Icon(
                        Icons.Default.Send,
                        contentDescription = "Send",
                        tint = if (connectionState is ConnectionState.Connected)
                            TerminalGreen else Color.Gray
                    )
                }
            }
        }
    }
}
