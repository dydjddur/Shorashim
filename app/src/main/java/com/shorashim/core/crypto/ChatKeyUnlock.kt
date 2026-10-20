package com.shorashim.core.crypto

/**
 * Hands the password typed on the login/register screen to the chat key backup logic exactly once.
 *
 * The password is needed to encrypt (backup) or decrypt (restore) the chat private key so that chat history survives
 * reinstalls. It is held in memory only until the next key sync consumes it and is never persisted.
 */
object ChatKeyUnlock {
    @Volatile
    private var pending: String? = null

    fun offer(password: String) {
        pending = password
    }

    fun take(): String? = pending.also { pending = null }
}
