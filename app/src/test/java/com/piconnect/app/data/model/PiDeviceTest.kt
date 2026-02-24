package com.piconnect.app.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class PiDeviceTest {

    @Test
    fun `default values should be set correctly`() {
        val device = PiDevice(
            name = "My Pi",
            host = "192.168.1.100",
            username = "pi"
        )

        assertNotNull("ID should be generated", device.id)
        assertEquals("Default port should be 22", 22, device.port)
        assertEquals("Default auth method should be PASSWORD", AuthMethod.PASSWORD, device.authMethod)
        assertNull("Default password should be null", device.password)
        assertNull("Default private key should be null", device.privateKey)
        assertFalse("Default useRpiConnect should be false", device.useRpiConnect)
    }

    @Test
    fun `custom values should be set correctly`() {
        val device = PiDevice(
            id = "custom-id",
            name = "Custom Pi",
            host = "10.0.0.1",
            port = 2222,
            username = "admin",
            authMethod = AuthMethod.KEY,
            privateKey = "ssh-rsa AAAA...",
            useRpiConnect = true
        )

        assertEquals("custom-id", device.id)
        assertEquals("Custom Pi", device.name)
        assertEquals("10.0.0.1", device.host)
        assertEquals(2222, device.port)
        assertEquals("admin", device.username)
        assertEquals(AuthMethod.KEY, device.authMethod)
        assertEquals("ssh-rsa AAAA...", device.privateKey)
        assertEquals(true, device.useRpiConnect)
    }

    @Test
    fun `auth methods should have correct values`() {
        assertEquals(2, AuthMethod.values().size)
        assertEquals(AuthMethod.PASSWORD, AuthMethod.valueOf("PASSWORD"))
        assertEquals(AuthMethod.KEY, AuthMethod.valueOf("KEY"))
    }

    @Test
    fun `password auth device should have password`() {
        val device = PiDevice(
            name = "Pi",
            host = "192.168.1.1",
            username = "pi",
            authMethod = AuthMethod.PASSWORD,
            password = "raspberry"
        )

        assertEquals(AuthMethod.PASSWORD, device.authMethod)
        assertEquals("raspberry", device.password)
        assertNull(device.privateKey)
    }

    @Test
    fun `key auth device should have private key`() {
        val device = PiDevice(
            name = "Pi",
            host = "192.168.1.1",
            username = "pi",
            authMethod = AuthMethod.KEY,
            privateKey = "-----BEGIN RSA PRIVATE KEY-----"
        )

        assertEquals(AuthMethod.KEY, device.authMethod)
        assertEquals("-----BEGIN RSA PRIVATE KEY-----", device.privateKey)
        assertNull(device.password)
    }

    @Test
    fun `each device should have unique id by default`() {
        val device1 = PiDevice(name = "Pi1", host = "192.168.1.1", username = "pi")
        val device2 = PiDevice(name = "Pi2", host = "192.168.1.2", username = "pi")

        assertNotNull(device1.id)
        assertNotNull(device2.id)
        assertFalse("IDs should be unique", device1.id == device2.id)
    }
}
