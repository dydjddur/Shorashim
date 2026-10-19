package com.shorashim

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RealUsernameAuthLogicTest {

    private fun normalizeUsername(raw: String): String {
        return raw.trim().removePrefix("@").lowercase()
    }

    private fun isSyntheticEmail(email: String): Boolean {
        return email.trim().lowercase().endsWith("@shorashim.app")
    }

    private fun generateSyntheticEmail(username: String): String {
        return "${normalizeUsername(username)}@shorashim.app"
    }

    private fun isValidDbUsername(username: String): Boolean {
        val regex = Regex("^[a-zA-Z0-9_]{3,30}$")
        return regex.matches(username)
    }

    @Test
    fun testUsernameNormalization() {
        assertEquals("david_cohen", normalizeUsername("@david_cohen"))
        assertEquals("david_cohen", normalizeUsername("  @David_Cohen  "))
        assertEquals("user123", normalizeUsername("User123"))
        assertEquals("test_user", normalizeUsername("test_user"))
    }

    @Test
    fun testSyntheticEmailGenerationAndDetection() {
        val email = generateSyntheticEmail("Naor_Av")
        assertEquals("naor_av@shorashim.app", email)
        assertTrue(isSyntheticEmail(email))

        assertFalse(isSyntheticEmail("naor@gmail.com"))
        assertFalse(isSyntheticEmail("user@shorashim.org"))
        assertFalse(isSyntheticEmail("shorashim.app@gmail.com"))
    }

    @Test
    fun testDbUsernameRegexConstraint() {
        // Must match 3-30 chars, alphanumeric and underscore only
        assertTrue(isValidDbUsername("abc"))
        assertTrue(isValidDbUsername("user_name_123"))
        assertTrue(isValidDbUsername("a".repeat(30)))

        assertFalse(isValidDbUsername("ab")) // too short
        assertFalse(isValidDbUsername("a".repeat(31))) // too long
        assertFalse(isValidDbUsername("user-name")) // hyphen not allowed
        assertFalse(isValidDbUsername("user name")) // space not allowed
        assertFalse(isValidDbUsername("משתמש")) // hebrew not allowed in username
        assertFalse(isValidDbUsername("@username")) // unstripped @ not allowed
    }
}
