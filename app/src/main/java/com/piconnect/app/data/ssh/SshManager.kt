package com.piconnect.app.data.ssh

import com.jcraft.jsch.Channel
import com.jcraft.jsch.ChannelExec
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import com.piconnect.app.data.model.AuthMethod
import com.piconnect.app.data.model.PiDevice
import java.io.ByteArrayOutputStream
import java.util.Properties

class SshManager {

    private val jsch = JSch()

    fun connect(device: PiDevice): Result<Session> {
        return try {
            if (device.authMethod == AuthMethod.KEY && device.privateKey != null) {
                jsch.addIdentity("device_key", device.privateKey.toByteArray(), null, null)
            }

            val session = jsch.getSession(device.username, device.host, device.port)

            if (device.authMethod == AuthMethod.PASSWORD && device.password != null) {
                session.setPassword(device.password)
            }

            // WARNING: StrictHostKeyChecking is disabled for convenience.
            // This makes connections vulnerable to man-in-the-middle attacks.
            // For production use, implement proper host key verification.
            val config = Properties()
            config["StrictHostKeyChecking"] = "no"
            session.setConfig(config)
            session.timeout = 10000

            session.connect()
            Result.success(session)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun executeCommand(session: Session, command: String): Result<String> {
        return try {
            val channel = session.openChannel("exec") as ChannelExec
            channel.setCommand(command)

            val outputStream = ByteArrayOutputStream()
            val errorStream = ByteArrayOutputStream()
            channel.outputStream = outputStream
            channel.setErrStream(errorStream)

            channel.connect()

            while (!channel.isClosed) {
                Thread.sleep(100)
            }

            val output = outputStream.toString("UTF-8")
            val error = errorStream.toString("UTF-8")
            channel.disconnect()

            if (error.isNotEmpty() && output.isEmpty()) {
                Result.failure(Exception(error))
            } else {
                Result.success(output + error)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun startShell(session: Session): Result<Channel> {
        return try {
            val channel = session.openChannel("shell")
            channel.connect()
            Result.success(channel)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun disconnect(session: Session) {
        try {
            if (session.isConnected) {
                session.disconnect()
            }
        } catch (_: Exception) {
            // Ignore errors during disconnect
        }
    }
}
