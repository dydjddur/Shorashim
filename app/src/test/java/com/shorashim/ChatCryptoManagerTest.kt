package com.shorashim

import com.shorashim.core.crypto.ChatCryptoManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ChatCryptoManagerTest {

    private lateinit var cryptoManager: ChatCryptoManager

    @Before
    fun setup() {
        // null Context uses the JVM standard JCA Keystore fallback for testing
        cryptoManager = ChatCryptoManager(context = null)
    }

    @Test
    fun testGenerateAndRetrievePublicKey() {
        val userId = "user-123"
        val pubKey = cryptoManager.getPublicKeyBase64(userId)

        assertNotNull(pubKey)
        assertTrue(pubKey.isNotBlank())

        // Calling again for the same user should return the same public key
        val pubKeySecond = cryptoManager.getPublicKeyBase64(userId)
        assertEquals(pubKey, pubKeySecond)
    }

    @Test
    fun testEndToEndEncryptionAndDecryptionRoundtrip() {
        val aliceId = "alice-user"
        val bobId = "bob-user"

        val alicePubKey = cryptoManager.getPublicKeyBase64(aliceId)
        val bobPubKey = cryptoManager.getPublicKeyBase64(bobId)

        assertNotNull(alicePubKey)
        assertNotNull(bobPubKey)

        val originalMessage = "שלום בוב, זהו מסר סודי ומוצפן מקצה לקצה 🛡️"

        // Alice encrypts for Bob
        val encrypted = cryptoManager.encryptMessage(
            senderUserId = aliceId,
            recipientPublicKeyBase64 = bobPubKey,
            plaintext = originalMessage,
        )

        assertNotNull(encrypted.ciphertextBase64)
        assertNotNull(encrypted.ivBase64)
        assertNotNull(encrypted.encryptedKeySenderBase64)
        assertNotNull(encrypted.encryptedKeyRecipientBase64)
        assertFalse(encrypted.ciphertextBase64.contains(originalMessage))

        // Bob decrypts the message
        val bobDecryptedResult = cryptoManager.decryptMessage(
            currentUserId = bobId,
            isSender = false,
            encryptedKeySenderBase64 = encrypted.encryptedKeySenderBase64,
            encryptedKeyRecipientBase64 = encrypted.encryptedKeyRecipientBase64,
            ivBase64 = encrypted.ivBase64,
            ciphertextBase64 = encrypted.ciphertextBase64,
        )

        assertTrue(bobDecryptedResult.isSuccess)
        assertEquals(originalMessage, bobDecryptedResult.getOrNull())

        // Alice (the sender) can also decrypt her own sent message
        val aliceDecryptedResult = cryptoManager.decryptMessage(
            currentUserId = aliceId,
            isSender = true,
            encryptedKeySenderBase64 = encrypted.encryptedKeySenderBase64,
            encryptedKeyRecipientBase64 = encrypted.encryptedKeyRecipientBase64,
            ivBase64 = encrypted.ivBase64,
            ciphertextBase64 = encrypted.ciphertextBase64,
        )

        assertTrue(aliceDecryptedResult.isSuccess)
        assertEquals(originalMessage, aliceDecryptedResult.getOrNull())
    }

    @Test
    fun testDecryptionFailsWithTamperedCiphertext() {
        val aliceId = "alice-user-2"
        val bobId = "bob-user-2"

        val bobPubKey = cryptoManager.getPublicKeyBase64(bobId)
        val encrypted = cryptoManager.encryptMessage(
            senderUserId = aliceId,
            recipientPublicKeyBase64 = bobPubKey,
            plaintext = "הודעה סודית",
        )

        // Tamper with ciphertext by altering characters
        val tamperedCiphertext = if (encrypted.ciphertextBase64.startsWith("A")) {
            "B" + encrypted.ciphertextBase64.substring(1)
        } else {
            "A" + encrypted.ciphertextBase64.substring(1)
        }

        val decryptResult = cryptoManager.decryptMessage(
            currentUserId = bobId,
            isSender = false,
            encryptedKeySenderBase64 = encrypted.encryptedKeySenderBase64,
            encryptedKeyRecipientBase64 = encrypted.encryptedKeyRecipientBase64,
            ivBase64 = encrypted.ivBase64,
            ciphertextBase64 = tamperedCiphertext,
        )

        assertTrue(decryptResult.isFailure)
    }

    @Test
    fun testUnauthorizedThirdPartyCannotDecrypt() {
        val aliceId = "alice-user-3"
        val bobId = "bob-user-3"
        val eveId = "eve-eavesdropper"

        val bobPubKey = cryptoManager.getPublicKeyBase64(bobId)
        val evePubKey = cryptoManager.getPublicKeyBase64(eveId)

        val encrypted = cryptoManager.encryptMessage(
            senderUserId = aliceId,
            recipientPublicKeyBase64 = bobPubKey,
            plaintext = "מסר חסוי בין אליס לבוב בלבד",
        )

        // Eve tries to decrypt pretending to be recipient
        val eveDecryptResult = cryptoManager.decryptMessage(
            currentUserId = eveId,
            isSender = false,
            encryptedKeySenderBase64 = encrypted.encryptedKeySenderBase64,
            encryptedKeyRecipientBase64 = encrypted.encryptedKeyRecipientBase64,
            ivBase64 = encrypted.ivBase64,
            ciphertextBase64 = encrypted.ciphertextBase64,
        )

        assertTrue(eveDecryptResult.isFailure)
    }

    @Test
    fun testPasswordBackupRestoresHistoryAfterReinstall() {
        val aliceId = "alice-backup"
        val bobId = "bob-backup"
        val bobPubKey = cryptoManager.getPublicKeyBase64(bobId)
        val alicePubKey = cryptoManager.getPublicKeyBase64(aliceId)
        val encrypted = cryptoManager.encryptMessage(aliceId, bobPubKey, "מה דיברנו אתמול")
        val backups = cryptoManager.createBackups(bobId, "correct-horse")

        // "Reinstall": a fresh manager has no keys for Bob and cannot read the old message.
        val fresh = ChatCryptoManager(context = null)
        assertFalse(fresh.hasLocalKeys(bobId))

        // Wrong password restores nothing.
        assertEquals(0, fresh.restoreFromBackups(bobId, "wrong", backups, bobPubKey))

        assertEquals(1, fresh.restoreFromBackups(bobId, "correct-horse", backups, bobPubKey))
        assertEquals(bobPubKey, fresh.getPublicKeyBase64(bobId))
        val decrypted = fresh.decryptMessage(
            currentUserId = bobId,
            isSender = false,
            encryptedKeySenderBase64 = encrypted.encryptedKeySenderBase64,
            encryptedKeyRecipientBase64 = encrypted.encryptedKeyRecipientBase64,
            ivBase64 = encrypted.ivBase64,
            ciphertextBase64 = encrypted.ciphertextBase64,
        )
        assertEquals("מה דיברנו אתמול", decrypted.getOrNull())
        assertNotNull(alicePubKey)
    }
}
