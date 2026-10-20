package com.shorashim.core.crypto

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.PublicKey
import java.security.SecureRandom
import java.security.spec.MGF1ParameterSpec
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.OAEPParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.PSource
import javax.crypto.spec.SecretKeySpec

/**
 * End-to-End Encryption (E2EE) Manager for Shorashim text chat.
 *
 * Architecture:
 * - Hybrid Cryptography (Envelope Encryption):
 *   - Each user has an RSA-2048 key pair; the public key is published for peer discovery.
 *   - Each message generates a fresh, ephemeral 256-bit AES key; the payload is encrypted with AES-256-GCM.
 *   - The AES key is encrypted separately for the recipient and the sender using RSA-OAEP with SHA-256.
 *   - Plaintext message bodies are NEVER sent or stored on Supabase.
 *
 * Key storage (why this changed):
 * - Previously the RSA private key was generated *inside* the Android Keystore. Keystore keys are wiped on
 *   uninstall / "clear data" and can never be exported, so after reinstalling the app every earlier message became
 *   permanently unreadable ("לא ניתן לפענוח במכשיר זה") and the history was lost.
 * - Now the key pair is generated in software. Locally it is stored encrypted with a non-exportable Keystore AES key,
 *   and a **backup** can be produced that is encrypted with a key derived from the user's password (PBKDF2).
 *   The backup is uploaded to the server; the server only ever sees ciphertext. After a reinstall, logging in with
 *   the password restores the key and therefore the whole chat history.
 * - Every key the user ever had on this device is kept, so old messages stay readable after a key rotation.
 *   A pre-existing Keystore-only key (from older app versions) is still used for decryption when present.
 */
class ChatCryptoManager(
    private val context: Context? = null,
) {
    companion object {
        private const val TAG = "ChatCrypto"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        /** Alias used by app versions <= 1.5 for Keystore-resident (non-exportable) RSA keys. Read-only now. */
        private const val LEGACY_KEY_ALIAS_PREFIX = "shorashim_chat_rsa_"
        private const val LOCAL_WRAP_ALIAS = "shorashim_chat_local_wrap_v1"
        private const val PREFS_NAME = "shorashim_chat_keys"

        /**
         * RSA-OAEP with SHA-256 as the main digest and **SHA-1 as the MGF1 digest** - the only OAEP combination
         * every Android Keystore implements for private-key operations, so legacy Keystore keys keep working.
         */
        private const val RSA_TRANSFORMATION = "RSA/ECB/OAEPPadding"
        private val OAEP_PARAMS: OAEPParameterSpec
            get() = OAEPParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA1, PSource.PSpecified.DEFAULT)
        private const val AES_TRANSFORMATION = "AES/GCM/NoPadding"
        private const val AES_KEY_SIZE = 256
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_LENGTH = 128

        const val BACKUP_ITERATIONS = 150_000

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

        fun fingerprint(publicKeyBase64: String): String {
            val digest = MessageDigest.getInstance("SHA-256").digest(fromBase64(publicKeyBase64))
            return toBase64(digest).take(22)
        }
    }

    /** A password-protected copy of one private key, safe to store on the server. */
    data class KeyBackup(
        val fingerprint: String,
        val publicKey: String,
        val wrappedPrivateKey: String,
        val salt: String,
        val iv: String,
        val kdf: String,
        val iterations: Int,
    )

    private class KeyEntry(val fingerprint: String, val keyPair: KeyPair)

    private val secureRandom = SecureRandom()
    /** userId -> keys, active (newest / published) key first. */
    private val cache = mutableMapOf<String, MutableList<KeyEntry>>()

    private val prefs: SharedPreferences? =
        context?.applicationContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val canUseKeystore: Boolean get() = context != null && Build.VERSION.SDK_INT > 0

    // ---------------------------------------------------------------------------------------------
    // Key management
    // ---------------------------------------------------------------------------------------------

    /** Returns the active key pair for [userId], generating (and persisting) one if none exists yet. */
    @Synchronized
    fun getOrCreateKeyPair(userId: String): KeyPair {
        keysFor(userId).firstOrNull()?.let { return it.keyPair }

        val kpg = KeyPairGenerator.getInstance("RSA")
        kpg.initialize(2048, secureRandom)
        val kp = kpg.generateKeyPair()
        val entry = KeyEntry(fingerprint(toBase64(kp.public.encoded)), kp)
        keysFor(userId).add(0, entry)
        persist(userId)
        return kp
    }

    /** True when this device already holds at least one exportable key for [userId]. */
    @Synchronized
    fun hasLocalKeys(userId: String): Boolean = keysFor(userId).isNotEmpty()

    /** Returns the user's active public key encoded in Base64 (X.509 format). */
    fun getPublicKeyBase64(userId: String): String {
        val keyPair = getOrCreateKeyPair(userId)
        return toBase64(keyPair.public.encoded)
    }

    /** Parses a peer's public key from a Base64 X.509 encoded string. */
    fun parsePublicKey(base64PublicKey: String): PublicKey {
        val bytes = fromBase64(base64PublicKey)
        val spec = X509EncodedKeySpec(bytes)
        val kf = KeyFactory.getInstance("RSA")
        return kf.generatePublic(spec)
    }

    private fun keysFor(userId: String): MutableList<KeyEntry> =
        cache.getOrPut(userId) { load(userId).toMutableList() }

    /** Every private key that may decrypt this user's history: exportable keys first, then a legacy Keystore key. */
    private fun allPrivateKeys(userId: String): List<PrivateKey> {
        val keys = synchronized(this) { keysFor(userId).map { it.keyPair.private } }
        val legacy = legacyKeystoreKey(userId)
        return if (legacy != null) keys + legacy else keys
    }

    private fun legacyKeystoreKey(userId: String): PrivateKey? {
        if (!canUseKeystore) return null
        return runCatching {
            val ks = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            ks.getKey("$LEGACY_KEY_ALIAS_PREFIX$userId", null) as? PrivateKey
        }.getOrNull()
    }

    // ---------------------------------------------------------------------------------------------
    // Password-protected backups
    // ---------------------------------------------------------------------------------------------

    /** Wraps every local key of [userId] with a key derived from [password]. */
    fun createBackups(userId: String, password: String): List<KeyBackup> {
        val entries = synchronized(this) { keysFor(userId).toList() }
        return entries.map { entry ->
            val salt = ByteArray(16).also { secureRandom.nextBytes(it) }
            val (kdf, kek) = deriveKey(password, salt, BACKUP_ITERATIONS, preferredKdf = null)
            val iv = ByteArray(GCM_IV_LENGTH).also { secureRandom.nextBytes(it) }
            val cipher = Cipher.getInstance(AES_TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, kek, GCMParameterSpec(GCM_TAG_LENGTH, iv))
            val wrapped = cipher.doFinal(entry.keyPair.private.encoded)
            KeyBackup(
                fingerprint = entry.fingerprint,
                publicKey = toBase64(entry.keyPair.public.encoded),
                wrappedPrivateKey = toBase64(wrapped),
                salt = toBase64(salt),
                iv = toBase64(iv),
                kdf = kdf,
                iterations = BACKUP_ITERATIONS,
            )
        }
    }

    /**
     * Imports every backup that [password] can unlock. The key whose public half equals [publishedPublicKey]
     * (what peers currently encrypt to) becomes active; otherwise the current active key is kept.
     * Returns how many keys were newly restored.
     */
    fun restoreFromBackups(
        userId: String,
        password: String,
        backups: List<KeyBackup>,
        publishedPublicKey: String?,
    ): Int {
        val restored = backups.mapNotNull { b ->
            runCatching {
                val (_, kek) = deriveKey(password, fromBase64(b.salt), b.iterations, b.kdf)
                val cipher = Cipher.getInstance(AES_TRANSFORMATION)
                cipher.init(Cipher.DECRYPT_MODE, kek, GCMParameterSpec(GCM_TAG_LENGTH, fromBase64(b.iv)))
                val pkcs8 = cipher.doFinal(fromBase64(b.wrappedPrivateKey))
                val kf = KeyFactory.getInstance("RSA")
                val priv = kf.generatePrivate(PKCS8EncodedKeySpec(pkcs8))
                val pub = parsePublicKey(b.publicKey)
                KeyEntry(fingerprint(b.publicKey), KeyPair(pub, priv))
            }.onFailure { Log.w(TAG, "Backup ${b.fingerprint} could not be unlocked: ${it.message}") }.getOrNull()
        }
        if (restored.isEmpty()) return 0

        synchronized(this) {
            val list = keysFor(userId)
            val known = list.map { it.fingerprint }.toSet()
            val fresh = restored.filter { it.fingerprint !in known }
            list.addAll(fresh)
            val publishedFp = publishedPublicKey?.let { runCatching { fingerprint(it) }.getOrNull() }
            val activeIdx = list.indexOfFirst { it.fingerprint == publishedFp }
            if (activeIdx > 0) list.add(0, list.removeAt(activeIdx))
            persist(userId)
            return fresh.size
        }
    }

    private fun deriveKey(password: String, salt: ByteArray, iterations: Int, preferredKdf: String?): Pair<String, SecretKey> {
        val candidates = listOfNotNull(preferredKdf, "PBKDF2WithHmacSHA256", "PBKDF2WithHmacSHA1").distinct()
        var lastError: Throwable? = null
        for (alg in candidates) {
            try {
                val factory = SecretKeyFactory.getInstance(alg)
                val spec = PBEKeySpec(password.toCharArray(), salt, iterations, 256)
                val bytes = factory.generateSecret(spec).encoded
                return alg to SecretKeySpec(bytes, "AES")
            } catch (t: Throwable) {
                lastError = t
                if (preferredKdf != null) break // a backup must be opened with exactly the KDF it was made with
            }
        }
        throw IllegalStateException("No PBKDF2 implementation available", lastError)
    }

    // ---------------------------------------------------------------------------------------------
    // Local persistence (private keys encrypted with a non-exportable Keystore AES key)
    // ---------------------------------------------------------------------------------------------

    private fun localWrapKey(): SecretKey? {
        if (!canUseKeystore) return null
        return runCatching {
            val ks = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            (ks.getKey(LOCAL_WRAP_ALIAS, null) as? SecretKey) ?: run {
                val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
                gen.init(
                    KeyGenParameterSpec.Builder(
                        LOCAL_WRAP_ALIAS,
                        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                    )
                        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        .setKeySize(256)
                        .build(),
                )
                gen.generateKey()
            }
        }.onFailure { Log.e(TAG, "Local wrap key unavailable", it) }.getOrNull()
    }

    private fun persist(userId: String) {
        val p = prefs ?: return
        val wrapKey = localWrapKey() ?: return
        val arr = JSONArray()
        for (entry in cache[userId].orEmpty()) {
            val cipher = Cipher.getInstance(AES_TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, wrapKey)
            val wrapped = cipher.doFinal(entry.keyPair.private.encoded)
            arr.put(
                JSONObject()
                    .put("fp", entry.fingerprint)
                    .put("pub", toBase64(entry.keyPair.public.encoded))
                    .put("priv", toBase64(wrapped))
                    .put("iv", toBase64(cipher.iv)),
            )
        }
        p.edit().putString("keys_$userId", arr.toString()).apply()
    }

    private fun load(userId: String): List<KeyEntry> {
        val raw = prefs?.getString("keys_$userId", null) ?: return emptyList()
        val wrapKey = localWrapKey() ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i ->
                runCatching {
                    val o = arr.getJSONObject(i)
                    val cipher = Cipher.getInstance(AES_TRANSFORMATION)
                    cipher.init(Cipher.DECRYPT_MODE, wrapKey, GCMParameterSpec(GCM_TAG_LENGTH, fromBase64(o.getString("iv"))))
                    val pkcs8 = cipher.doFinal(fromBase64(o.getString("priv")))
                    val kf = KeyFactory.getInstance("RSA")
                    val priv = kf.generatePrivate(PKCS8EncodedKeySpec(pkcs8))
                    val pub = parsePublicKey(o.getString("pub"))
                    KeyEntry(o.getString("fp"), KeyPair(pub, priv))
                }.getOrNull()
            }
        }.getOrDefault(emptyList())
    }

    // ---------------------------------------------------------------------------------------------
    // Message encryption
    // ---------------------------------------------------------------------------------------------

    data class EncryptionResult(
        val ciphertextBase64: String,
        val ivBase64: String,
        val encryptedKeySenderBase64: String,
        val encryptedKeyRecipientBase64: String,
    )

    /** Encrypts a plaintext message for sender and recipient. */
    fun encryptMessage(
        senderUserId: String,
        recipientPublicKeyBase64: String,
        plaintext: String,
    ): EncryptionResult {
        val keyGen = KeyGenerator.getInstance("AES")
        keyGen.init(AES_KEY_SIZE, secureRandom)
        val aesKey = keyGen.generateKey()
        val aesKeyBytes = aesKey.encoded

        val iv = ByteArray(GCM_IV_LENGTH).also { secureRandom.nextBytes(it) }
        val aesCipher = Cipher.getInstance(AES_TRANSFORMATION)
        aesCipher.init(Cipher.ENCRYPT_MODE, aesKey, GCMParameterSpec(GCM_TAG_LENGTH, iv))
        val ciphertext = aesCipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))

        val recipientPublicKey = parsePublicKey(recipientPublicKeyBase64)
        val encryptedKeyRecipient = encryptKeyWithRsa(aesKeyBytes, recipientPublicKey)

        // Also encrypted for the sender so they can read their own sent history.
        val senderKeyPair = getOrCreateKeyPair(senderUserId)
        val encryptedKeySender = encryptKeyWithRsa(aesKeyBytes, senderKeyPair.public)

        return EncryptionResult(
            ciphertextBase64 = toBase64(ciphertext),
            ivBase64 = toBase64(iv),
            encryptedKeySenderBase64 = toBase64(encryptedKeySender),
            encryptedKeyRecipientBase64 = toBase64(encryptedKeyRecipient),
        )
    }

    /** Decrypts a message, trying every key this user has had on this device. */
    fun decryptMessage(
        currentUserId: String,
        isSender: Boolean,
        encryptedKeySenderBase64: String,
        encryptedKeyRecipientBase64: String,
        ivBase64: String,
        ciphertextBase64: String,
    ): Result<String> {
        return runCatching {
            val keyToDecryptBase64 = if (isSender) encryptedKeySenderBase64 else encryptedKeyRecipientBase64
            val encryptedAesKey = fromBase64(keyToDecryptBase64)
            val candidates = allPrivateKeys(currentUserId)
            if (candidates.isEmpty()) error("No private key on this device")

            var aesKeyBytes: ByteArray? = null
            var lastError: Throwable? = null
            for (key in candidates) {
                try {
                    aesKeyBytes = decryptKeyWithRsa(encryptedAesKey, key)
                    break
                } catch (t: Throwable) {
                    lastError = t
                }
            }
            val keyBytes = aesKeyBytes ?: throw (lastError ?: IllegalStateException("Cannot decrypt"))

            val aesCipher = Cipher.getInstance(AES_TRANSFORMATION)
            aesCipher.init(
                Cipher.DECRYPT_MODE,
                SecretKeySpec(keyBytes, "AES"),
                GCMParameterSpec(GCM_TAG_LENGTH, fromBase64(ivBase64)),
            )
            String(aesCipher.doFinal(fromBase64(ciphertextBase64)), Charsets.UTF_8)
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

    /** Deletes this user's keys from this device (used by tests and when a user explicitly resets encryption). */
    @Synchronized
    fun deleteKeyPair(userId: String) {
        cache.remove(userId)
        prefs?.edit()?.remove("keys_$userId")?.apply()
        if (canUseKeystore) {
            runCatching {
                val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
                keyStore.load(null)
                keyStore.deleteEntry("$LEGACY_KEY_ALIAS_PREFIX$userId")
            }
        }
    }
}
