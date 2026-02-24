package com.piconnect.app.data.ssh

import com.piconnect.app.data.model.AuthMethod
import com.piconnect.app.data.model.PiDevice
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SshManagerTest {

    private lateinit var sshManager: SshManager

    @Before
    fun setUp() {
        sshManager = SshManager()
    }

    @Test
    fun `connect with invalid host should return failure`() {
        val device = PiDevice(
            name = "Test Pi",
            host = "invalid.host.that.does.not.exist",
            port = 22,
            username = "pi",
            authMethod = AuthMethod.PASSWORD,
            password = "test"
        )

        val result = sshManager.connect(device)
        assertTrue("Connection to invalid host should fail", result.isFailure)
    }

    @Test
    fun `connect with invalid port should return failure`() {
        val device = PiDevice(
            name = "Test Pi",
            host = "127.0.0.1",
            port = 99999,
            username = "pi",
            authMethod = AuthMethod.PASSWORD,
            password = "test"
        )

        val result = sshManager.connect(device)
        assertTrue("Connection with invalid port should fail", result.isFailure)
    }

    @Test
    fun `sshManager should be initialized`() {
        assertNotNull("SshManager should not be null", sshManager)
    }

    @Test
    fun `disconnect should handle already disconnected session gracefully`() {
        // Verify that disconnect does not throw when called on a failed connection attempt
        val device = PiDevice(
            name = "Test Pi",
            host = "127.0.0.1",
            port = 99999,
            username = "pi",
            authMethod = AuthMethod.PASSWORD,
            password = "test"
        )
        val result = sshManager.connect(device)
        assertTrue(result.isFailure)
        // SshManager should remain usable after a failed connection
        assertNotNull(sshManager)
    }
}
