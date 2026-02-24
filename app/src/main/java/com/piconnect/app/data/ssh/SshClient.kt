package com.piconnect.app.data.ssh

import com.jcraft.jsch.ChannelShell
import com.jcraft.jsch.JSch
import com.jcraft.jsch.SocketFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.net.Socket
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * SshClient wraps JSch to tunnel SSH traffic through a WebSocket connection.
 * The WebSocket is provided by the Raspberry Pi Connect service.
 */
class SshClient(
    private val websocketUrl: String,
    private val sshUsername: String,
    private val sshPassword: String,
    private val onOutput: (String) -> Unit,
    private val onError: (String) -> Unit,
    private val onDisconnected: () -> Unit
) {
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var session: com.jcraft.jsch.Session? = null
    private var channel: ChannelShell? = null
    private var webSocket: WebSocket? = null
    private var outputStream: OutputStream? = null
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS) // No timeout for WebSocket
        .build()

    fun connect() {
        scope.launch {
            try {
                connectInternal()
            } catch (e: Exception) {
                onError("SSH connection failed: ${e.message}")
            }
        }
    }

    private fun connectInternal() {
        // Pipes to bridge WebSocket <-> JSch
        val wsToSshIn = PipedInputStream()
        val wsToSshOut = PipedOutputStream(wsToSshIn)
        val sshToWsIn = PipedInputStream()
        val sshToWsOut = PipedOutputStream(sshToWsIn)

        val connectLatch = CountDownLatch(1)
        var wsConnectError: String? = null

        val wsListener = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                this@SshClient.webSocket = webSocket
                connectLatch.countDown()
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                try {
                    wsToSshOut.write(bytes.toByteArray())
                    wsToSshOut.flush()
                } catch (e: IOException) {
                    // Connection closed
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    wsToSshOut.write(text.toByteArray())
                    wsToSshOut.flush()
                } catch (e: IOException) {
                    // Connection closed
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                wsConnectError = t.message
                connectLatch.countDown()
                onError("WebSocket error: ${t.message}")
                onDisconnected()
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                onDisconnected()
            }
        }

        val request = Request.Builder().url(websocketUrl).build()
        okHttpClient.newWebSocket(request, wsListener)

        // Wait for WebSocket to connect
        if (!connectLatch.await(30, TimeUnit.SECONDS)) {
            onError("WebSocket connection timed out")
            return
        }
        if (wsConnectError != null) {
            onError("WebSocket failed: $wsConnectError")
            return
        }

        // Forward SSH output to WebSocket
        scope.launch {
            val buffer = ByteArray(4096)
            try {
                while (true) {
                    val read = sshToWsIn.read(buffer)
                    if (read == -1) break
                    webSocket?.send(ByteString.of(*buffer.copyOf(read)))
                }
            } catch (e: IOException) {
                // Channel closed
            }
        }

        // Set up JSch with custom socket factory that uses our pipes
        val jsch = JSch()
        val socketFactory = object : SocketFactory {
            override fun createSocket(host: String, port: Int): Socket {
                return object : Socket() {
                    override fun getInputStream(): InputStream = wsToSshIn
                    override fun getOutputStream(): OutputStream = sshToWsOut
                    override fun isConnected(): Boolean = true
                    override fun isClosed(): Boolean = false
                    override fun close() {
                        webSocket?.close(1000, "SSH session ended")
                    }
                }
            }

            override fun getInputStream(socket: Socket): InputStream = socket.getInputStream()
            override fun getOutputStream(socket: Socket): OutputStream = socket.getOutputStream()
        }

        val jschSession = jsch.getSession(sshUsername, "localhost", 22)
        jschSession.setPassword(sshPassword)
        jschSession.setConfig("StrictHostKeyChecking", "no")
        jschSession.setConfig("PreferredAuthentications", "password")
        jschSession.socketFactory = socketFactory
        jschSession.connect(30000)
        session = jschSession

        val shellChannel = jschSession.openChannel("shell") as ChannelShell
        shellChannel.setPtyType("xterm")
        shellChannel.setPtySize(220, 50, 1920, 1080)

        val channelOutput = shellChannel.outputStream
        outputStream = channelOutput

        shellChannel.connect(15000)
        channel = shellChannel

        // Read SSH channel output and send to UI
        scope.launch {
            val inputStream = shellChannel.inputStream
            val buffer = ByteArray(4096)
            try {
                while (shellChannel.isConnected) {
                    if (inputStream.available() > 0) {
                        val read = inputStream.read(buffer)
                        if (read > 0) {
                            val text = String(buffer, 0, read, Charsets.UTF_8)
                            onOutput(text)
                        }
                    } else {
                        delay(50)
                    }
                }
            } catch (e: Exception) {
                // Channel disconnected
            } finally {
                onDisconnected()
            }
        }
    }

    fun sendCommand(command: String) {
        scope.launch {
            try {
                outputStream?.write((command + "\n").toByteArray(Charsets.UTF_8))
                outputStream?.flush()
            } catch (e: Exception) {
                onError("Failed to send command: ${e.message}")
            }
        }
    }

    fun disconnect() {
        try {
            channel?.disconnect()
            session?.disconnect()
            webSocket?.close(1000, "User disconnected")
        } catch (e: Exception) {
            // Ignore disconnect errors
        }
    }
}
