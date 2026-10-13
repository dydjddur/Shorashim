package com.shorashim.core.crypto

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.PublicKey
import java.security.SecureRandom
import java.security.spec.MGF1ParameterSpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.OAEPParameterSpec
import javax.crypto.spec.PSource
import javax.crypto.spec.SecretKeySpec

/**
 * End-to-End Encryption (E2EE) Manager for Shorashim text chat.
 *
 * Architecture:
 * - Hybrid Cryptography (Envelope Encryption):
 *   - Each user maintains an RSA-2048 key pair.
 *   - Private keys are stored in hardware-backed Android KeyStore whenever available.
 *   - Public keys are published to the database for peer discovery.
 *   - Each message generates a fresh, ephemeral 256-bit AES symmetric key.
 *   - The payload is encrypted with authenticated AES-256-GCM (12-byte random IV, 128-bit authentication tag)
 *     protecting against tampering and replay.
 *   - The AES key is encrypted separately for the recipient and the sender using RSA-OAEP with SHA-256.
 *   - Only the sender and recipient devices possessing the corresponding private keys can decrypt the message.
 *   - Plaintext message bodies are NEVER sent or stored on Supabase.
 */
class ChatCryptoManager(
    private val context: Context? = null,
) {
    companion object {
        private const val TAG = "ChatCrypto"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS_PREFIX = "shorashim_chat_rsa_"
        /**
         * RSA-OAEP with SHA-256 as the main digest and **SHA-1 as the MGF1 digest**. This is the only OAEP
         * combination every Android Keystore (API 23+) implements for *private-key* operations, so using the exact
         * same explicit parameters for encrypt and decrypt is what makes a peer's message decryptable on their
         * device. (Relying on provider defaults produced "cannot decrypt" errors because Conscrypt and the Keystore
         * disagree on the default MGF1 digest.)
         */
        private const val RSA_TRANSFORMATION = "RSA/ECB/OAEPPadding"
        private val OAEP_PARAMS: OAEPParameterSpec
            get() = OAEPParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA1, PSource.PSpecified.DEFAULT)
        private const val AES_TRANSFORMATION = "AES/GCM/NoPadding"
        private const val AES_KEY_SIZE = 256
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_LENGTH = 128

        /** Safe Base64 encoder compatible with Android and standard JVM unit tests */
        fun toBase64(bytes: ByteArray): String {
            return try {
                java.util.Base64.getEncoder().encodeToString(bytes)
            } catch (t: Throwable) {
                android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
            }
        }

        /** Safe Base64 decoder compatible with Android and standard JVM unit tests */
        fun fromBase64(str: String): ByteArray {
            return try {
                java.util.Base64.getDecoder().decode(str.trim())
            } catch (t: Throwable) {
                android.util.Base64.decode(str, android.util.Base64.NO_WRAP)
            }
        }
    }

    private val secureRandom = SecureRandom()
    // In-memory fallback for test environments or legacy platforms where AndroidKeyStore is absent
    private val fallbackKeyPairs = mutableMapOf<String, KeyPair>()

    private fun getAlias(userId: String): String = "$KEY_ALIAS_PREFIX$userId"

    /**
     * Gets or generates an RSA-2048 key pair for the given user ID.
     */
    @Synchronized
    fun getOrCreateKeyPair(userId: String): KeyPair {
        val alias = getAlias(userId)

        // 1. Try AndroidKeyStore first
        try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
            keyStore.load(null)
            if (keyStore.containsAlias(alias)) {
                val privateKey = keyStore.getKey(alias, null) as? PrivateKey
                val certificate = keyStore.getCertificate(alias)
                val publicKey = certificate?.publicKey
                if (privateKey != null && publicKey != null) {
                    return KeyPair(publicKey, privateKey)
                }
            }

            // Generate new RSA-2048 in AndroidKeyStore
            val kpg = KeyPairGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_RSA,
                ANDROID_KEYSTORE,
            )
            val spec = KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA1)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_OAEP)
                .setKeySize(2048)
                .build()

            kpg.initialize(spec)
            return kpg.generateKeyPair()
        } catch (e: Throwable) {
            // On a real device a Keystore failure must surface: silently using a throw-away in-memory key would
            // publish a public key whose private half is lost when the process dies, making every message to this
            // user undecryptable. The in-memory pair exists only for plain-JVM unit tests (SDK_INT == 0).
            if (Build.VERSION.SDK_INT > 0) throw IllegalStateException("Secure key storage is unavailable: ${e.message}", e)
        }

        // 2. Fallback for unit testing and devices without AndroidKeyStore
        fallbackKeyPairs[userId]?.let { return it }
        val kpg = KeyPairGenerator.getInstance("RSA")
        kpg.initialize(2048, secureRandom)
        val kp = kpg.generateKeyPair()
        fallbackKeyPairs[userId] = kp
        return kp
    }

    /**
     * Returns the user's public key encoded in Base64 (X.509 format).
     */
    fun getPublicKeyBase64(userId: String): String {
        val keyPair = getOrCreateKeyPair(userId)
        return toBase64(keyPair.public.encoded)
    }

    /**
     * Parses a peer's public key from a Base64 X.509 encoded string.
     */
    fun parsePublicKey(base64PublicKey: String): PublicKey {
        val bytes = fromBase64(base64PublicKey)
        val spec = X509EncodedKeySpec(bytes)
        val kf = KeyFactory.getInstance("RSA")
        return kf.generatePublic(spec)
    }

    data class EncryptionResult(
        val ciphertextBase64: String,
        val ivBase64: String,
        val encryptedKeySenderBase64: String,
        val encryptedKeyRecipientBase64: String,
    )

    /**
     * Encrypts a plaintext message for sender and recipient.
     */
    fun encryptMessage(
        senderUserId: String,
        recipientPublicKeyBase64: String,
        plaintext: String,
    ): EncryptionResult {
        // 1. Generate ephemeral 256-bit AES key
        val keyGen = KeyGenerator.getInstance("AES")
        keyGen.init(AES_KEY_SIZE, secureRandom)
        val aesKey = keyGen.generateKey()
        val aesKeyBytes = aesKey.encoded

        // 2. Encrypt plaintext with AES-256-GCM
        val iv = ByteArray(GCM_IV_LENGTH).also { secureRandom.nextBytes(it) }
        val aesCipher = Cipher.getInstance(AES_TRANSFORMATION)
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        aesCipher.init(Cipher.ENCRYPT_MODE, aesKey, gcmSpec)
        val ciphertext = aesCipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))

        // 3. Encrypt AES key with recipient's RSA public key
        val recipientPublicKey = parsePublicKey(recipientPublicKeyBase64)
        val encryptedKeyRecipient = encryptKeyWithRsa(aesKeyBytes, recipientPublicKey)

        // 4. Encrypt AES key with sender's RSA public key (so sender can view message history)
        val senderKeyPair = getOrCreateKeyPair(senderUserId)
        val encryptedKeySender = encryptKeyWithRsa(aesKeyBytes, senderKeyPair.public)

        return EncryptionResult(
            ciphertextBase64 = toBase64(ciphertext),
            ivBase64 = toBase64(iv),
            encryptedKeySenderBase64 = toBase64(encryptedKeySender),
            encryptedKeyRecipientBase64 = toBase64(encryptedKeyRecipient),
        )
    }

    /**
     * Decrypts a message using the user's private key.
     */
    fun decryptMessage(
        currentUserId: String,
        isSender: Boolean,
        encryptedKeySenderBase64: String,
        encryptedKeyRecipientBase64: String,
        ivBase64: String,
        ciphertextBase64: String,
    ): Result<String> {
        return runCatching {
            val keyPair = getOrCreateKeyPair(currentUserId)
            val keyToDecryptBase64 = if (isSender) encryptedKeySenderBase64 else encryptedKeyRecipientBase64
            val encryptedAesKey = fromBase64(keyToDecryptBase64)

            // 1. Decrypt AES key with private key
            val aesKeyBytes = decryptKeyWithRsa(encryptedAesKey, keyPair.private)
            val aesKey: SecretKey = SecretKeySpec(aesKeyBytes, "AES")

            // 2. Decrypt ciphertext with AES-256-GCM
            val iv = fromBase64(ivBase64)
            val ciphertext = fromBase64(ciphertextBase64)
            val aesCipher = Cipher.getInstance(AES_TRANSFORMATION)
            val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            aesCipher.init(Cipher.DECRYPT_MODE, aesKey, gcmSpec)
            val decryptedBytes = aesCipher.doFinal(ciphertext)

            String(decryptedBytes, Charsets.UTF_8)
        }
    }

    private fun encryptKeyWithRsa(keyBytes: ByteArray, publicKey: PublicKey): ByteArray {
        val cipher = Cipher.getInstance(RSA_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, publicKey, OAEP_PARAMS)
        return cipher.doFinal(keyBytes)
    }

    private fun decryptKeyWithRsa(encryptedKey: ByteArray, privateKey: PrivateKey): ByteArray {
        val cipher = Cipher.getInstance(RSA_TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, privateKey, OAEP_PARAMS)
        return cipher.doFinal(encryptedKey)
    }

    /** Deletes this user's key pair (used by tests and when a user explicitly resets encryption). */
    @Synchronized
    fun deleteKeyPair(userId: String) {
        fallbackKeyPairs.remove(userId)
        runCatching {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
            keyStore.load(null)
            keyStore.deleteEntry(getAlias(userId))
        }
    }
}
