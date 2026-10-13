package com.shorashim

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.shorashim.core.crypto.ChatCryptoManager
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

/**
 * Runs on a real device against the hardware-backed AndroidKeyStore (the JVM unit tests cannot): Alice encrypts for
 * Bob, then both ends must be able to decrypt with their own Keystore private key.
 */
@RunWith(AndroidJUnit4::class)
class ChatCryptoDeviceTest {
    private val ctx = InstrumentationRegistry.getInstrumentation().targetContext
    private val crypto = ChatCryptoManager(ctx)
    private val alice = "e2ee-test-alice-" + UUID.randomUUID()
    private val bob = "e2ee-test-bob-" + UUID.randomUUID()

    @After
    fun cleanup() {
        crypto.deleteKeyPair(alice)
        crypto.deleteKeyPair(bob)
    }

    @Test
    fun recipientAndSenderCanBothDecrypt() {
        val bobPublic = crypto.getPublicKeyBase64(bob)
        val message = "שלום בוב! 👋 זו הודעה מוצפנת מקצה לקצה " + "x".repeat(300)

        val enc = crypto.encryptMessage(alice, bobPublic, message)

        val atBob = crypto.decryptMessage(bob, false, enc.encryptedKeySenderBase64, enc.encryptedKeyRecipientBase64, enc.ivBase64, enc.ciphertextBase64)
        assertTrue("recipient failed: ${atBob.exceptionOrNull()}", atBob.isSuccess)
        assertEquals(message, atBob.getOrNull())

        val atAlice = crypto.decryptMessage(alice, true, enc.encryptedKeySenderBase64, enc.encryptedKeyRecipientBase64, enc.ivBase64, enc.ciphertextBase64)
        assertTrue("sender failed: ${atAlice.exceptionOrNull()}", atAlice.isSuccess)
        assertEquals(message, atAlice.getOrNull())
    }

    @Test
    fun strangerCannotDecrypt() {
        val bobPublic = crypto.getPublicKeyBase64(bob)
        val enc = crypto.encryptMessage(alice, bobPublic, "סוד")
        val eve = "e2ee-test-eve-" + UUID.randomUUID()
        try {
            val r = crypto.decryptMessage(eve, false, enc.encryptedKeySenderBase64, enc.encryptedKeyRecipientBase64, enc.ivBase64, enc.ciphertextBase64)
            assertTrue("a third party must not be able to read the message", r.isFailure)
        } finally {
            crypto.deleteKeyPair(eve)
        }
    }

    @Test
    fun publicKeyIsStableAcrossInstances() {
        val first = crypto.getPublicKeyBase64(alice)
        val second = ChatCryptoManager(ctx).getPublicKeyBase64(alice)
        assertEquals("the published key must not change between app launches", first, second)
    }

    @Test
    fun tamperedCiphertextIsRejected() {
        val bobPublic = crypto.getPublicKeyBase64(bob)
        val enc = crypto.encryptMessage(alice, bobPublic, "authentic")
        val bytes = ChatCryptoManager.fromBase64(enc.ciphertextBase64)
        bytes[0] = (bytes[0].toInt() xor 0x01).toByte()
        val r = crypto.decryptMessage(bob, false, enc.encryptedKeySenderBase64, enc.encryptedKeyRecipientBase64, enc.ivBase64, ChatCryptoManager.toBase64(bytes))
        assertTrue(r.isFailure)
    }
}
