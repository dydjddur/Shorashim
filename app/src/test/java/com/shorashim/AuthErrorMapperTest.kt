package com.shorashim

import com.shorashim.core.common.AuthErrorMapper
import com.shorashim.core.common.SupabaseNotConfiguredException
import org.junit.Assert.assertEquals
import org.junit.Test

class AuthErrorMapperTest {

    @Test
    fun mapErrorMessageReturnsAppropriateHebrewResId() {
        assertEquals(
            R.string.auth_error_invalid_credentials,
            AuthErrorMapper.mapErrorMessage("Invalid login credentials"),
        )
        assertEquals(
            R.string.auth_error_invalid_credentials,
            AuthErrorMapper.mapErrorMessage("invalid_grant"),
        )
        assertEquals(
            R.string.auth_error_email_already_in_use,
            AuthErrorMapper.mapErrorMessage("User already registered"),
        )
        assertEquals(
            R.string.auth_error_weak_password,
            AuthErrorMapper.mapErrorMessage("Password should be at least 6 characters"),
        )
        assertEquals(
            R.string.auth_error_too_many_requests,
            AuthErrorMapper.mapErrorMessage("For security purposes, you can only request this once every 60 seconds"),
        )
        assertEquals(
            R.string.auth_error_network,
            AuthErrorMapper.mapErrorMessage("Unable to resolve host: ConnectException"),
        )
        assertEquals(
            R.string.auth_error_generic,
            AuthErrorMapper.mapErrorMessage("Some unknown server error"),
        )
    }

    @Test
    fun mapThrowableHandlesUnconfiguredAndNetwork() {
        val notConfigured = SupabaseNotConfiguredException()
        assertEquals(
            R.string.auth_error_not_configured,
            AuthErrorMapper.mapThrowable(notConfigured),
        )

        val networkException = Exception("A network error occurred")
        assertEquals(
            R.string.auth_error_network,
            AuthErrorMapper.mapThrowable(networkException),
        )

        val genericException = RuntimeException("Something unexpected")
        assertEquals(
            R.string.auth_error_generic,
            AuthErrorMapper.mapThrowable(genericException),
        )
    }
}
