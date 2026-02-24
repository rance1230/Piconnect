package com.piconnect.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.piconnect.app.viewmodel.SshTerminalViewModel
import com.piconnect.app.viewmodel.SshConnectionState

val TerminalBackground = Color(0xFF1A1A2E)
val TerminalGreen = Color(0xFF00FF41)
val TerminalYellow = Color(0xFFFFD700)
val TerminalRed = Color(0xFFFF4444)
val TerminalWhite = Color(0xFFE0E0E0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SshTerminalScreen(
    viewModel: SshTerminalViewModel,
    deviceName: String,
    onNavigateBack: () -> Unit
) {
    val connectionState by viewModel.connectionState.collectAsState()
    val terminalOutput by viewModel.terminalOutput.collectAsState()
    val commandInput by viewModel.commandInput.collectAsState()
    val sshUsername by viewModel.sshUsername.collectAsState()
    val sshPassword by viewModel.sshPassword.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(terminalOutput.size) {
        if (terminalOutput.isNotEmpty()) {
            listState.animateScrollToItem(terminalOutput.size - 1)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.disconnect()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = deviceName,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = when (connectionState) {
                                is SshConnectionState.Connected -> "Connected"
                                is SshConnectionState.Connecting -> "Connecting…"
                                is SshConnectionState.Disconnected -> "Disconnected"
                                is SshConnectionState.Error -> "Error"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = when (connectionState) {
                                is SshConnectionState.Connected -> Color(0xFF4CAF50)
                                is SshConnectionState.Connecting -> Color(0xFFFFD700)
                                is SshConnectionState.Disconnected -> Color.Gray
                                is SshConnectionState.Error -> Color(0xFFFF4444)
                            }
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0D0D1A),
                    titleContentColor = Color.White
                )
            )
        },
        bottomBar = {
            if (connectionState is SshConnectionState.Connected) {
                Surface(
                    color = Color(0xFF0D0D1A),
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                            .navigationBarsPadding()
                            .imePadding(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$ ",
                            color = TerminalGreen,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                        OutlinedTextField(
                            value = commandInput,
                            onValueChange = { viewModel.onCommandInputChanged(it) },
                            modifier = Modifier.weight(1f),
                            placeholder = {
                                Text(
                                    "Enter command…",
                                    color = Color.Gray,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 14.sp
                                )
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(
                                onSend = { viewModel.sendCommand() }
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TerminalGreen,
                                unfocusedTextColor = TerminalGreen,
                                cursorColor = TerminalGreen,
                                focusedBorderColor = Color(0xFF333366),
                                unfocusedBorderColor = Color(0xFF222244)
                            ),
                            textStyle = LocalTextStyle.current.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 14.sp
                            )
                        )
                        IconButton(
                            onClick = { viewModel.sendCommand() },
                            enabled = commandInput.isNotBlank()
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = if (commandInput.isNotBlank()) TerminalGreen else Color.Gray
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(TerminalBackground)
                .padding(paddingValues)
        ) {
            when (connectionState) {
                is SshConnectionState.Connecting -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(color = TerminalGreen)
                        Text("Establishing SSH tunnel…", color = TerminalGreen, fontFamily = FontFamily.Monospace)
                    }
                }
                is SshConnectionState.Error -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Connection Failed",
                            color = TerminalRed,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 18.sp
                        )
                        Text(
                            text = (connectionState as SshConnectionState.Error).message,
                            color = TerminalWhite,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp
                        )
                        Button(
                            onClick = { viewModel.connect() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF333366)
                            )
                        ) {
                            Text("Retry", color = TerminalGreen, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
                is SshConnectionState.Disconnected -> {
                    SshCredentialsForm(
                        modifier = Modifier.align(Alignment.Center),
                        username = sshUsername,
                        password = sshPassword,
                        onUsernameChanged = viewModel::onSshUsernameChanged,
                        onPasswordChanged = viewModel::onSshPasswordChanged,
                        onConnect = viewModel::connect
                    )
                }
                is SshConnectionState.Connected -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        items(terminalOutput) { line ->
                            TerminalLine(line)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TerminalLine(line: com.piconnect.app.viewmodel.TerminalLine) {
    val color = when (line.type) {
        com.piconnect.app.viewmodel.TerminalLineType.COMMAND -> TerminalYellow
        com.piconnect.app.viewmodel.TerminalLineType.OUTPUT -> TerminalWhite
        com.piconnect.app.viewmodel.TerminalLineType.ERROR -> TerminalRed
        com.piconnect.app.viewmodel.TerminalLineType.SYSTEM -> TerminalGreen
    }
    Text(
        text = line.text,
        color = color,
        fontFamily = FontFamily.Monospace,
        fontSize = 13.sp,
        lineHeight = 18.sp
    )
}

@Composable
fun SshCredentialsForm(
    modifier: Modifier = Modifier,
    username: String,
    password: String,
    onUsernameChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onConnect: () -> Unit
) {
    var passwordVisible by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.padding(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0D0D1A)),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "SSH Credentials",
                color = TerminalGreen,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            OutlinedTextField(
                value = username,
                onValueChange = onUsernameChanged,
                label = { Text("Username", color = Color.Gray, fontFamily = FontFamily.Monospace) },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = TerminalGreen) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TerminalGreen,
                    unfocusedTextColor = TerminalGreen,
                    cursorColor = TerminalGreen,
                    focusedBorderColor = TerminalGreen,
                    unfocusedBorderColor = Color(0xFF333366)
                ),
                textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = password,
                onValueChange = onPasswordChanged,
                label = { Text("Password", color = Color.Gray, fontFamily = FontFamily.Monospace) },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = TerminalGreen) },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = null,
                            tint = Color.Gray
                        )
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { onConnect() }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TerminalGreen,
                    unfocusedTextColor = TerminalGreen,
                    cursorColor = TerminalGreen,
                    focusedBorderColor = TerminalGreen,
                    unfocusedBorderColor = Color(0xFF333366)
                ),
                textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace),
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = onConnect,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333366))
            ) {
                Text("Connect via SSH", color = TerminalGreen, fontFamily = FontFamily.Monospace)
            }
        }
    }
}
