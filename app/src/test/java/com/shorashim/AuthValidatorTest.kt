package com.shorashim

import com.shorashim.core.common.AuthValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AuthValidatorTest {

    @Test
    fun nameValidation() {
        assertEquals(R.string.validation_name_empty, AuthValidator.validateName(""))
        assertEquals(R.string.validation_name_empty, AuthValidator.validateName("   "))
        assertEquals(R.string.validation_name_too_short, AuthValidator.validateName("א"))
        assertNull(AuthValidator.validateName("אברהם"))
        assertNull(AuthValidator.validateName("שרה כהן"))
    }

    @Test
    fun emailValidation() {
        assertEquals(R.string.validation_email_empty, AuthValidator.validateEmail(""))
        assertEquals(R.string.validation_email_empty, AuthValidator.validateEmail("   "))
        assertEquals(R.string.validation_email_invalid, AuthValidator.validateEmail("invalid"))
        assertEquals(R.string.validation_email_invalid, AuthValidator.validateEmail("invalid@"))
        assertEquals(R.string.validation_email_invalid, AuthValidator.validateEmail("invalid@domain"))
        assertNull(AuthValidator.validateEmail("test@example.com"))
        assertNull(AuthValidator.validateEmail("user.name+tag@domain.co.il"))
    }

    @Test
    fun passwordValidation() {
        assertEquals(R.string.validation_password_empty, AuthValidator.validatePassword(""))
        assertEquals(R.string.validation_password_too_short, AuthValidator.validatePassword("12345"))
        assertNull(AuthValidator.validatePassword("123456"))
        assertNull(AuthValidator.validatePassword("MyStrongPass123!"))
    }

    @Test
    fun confirmPasswordValidation() {
        assertEquals(
            R.string.validation_confirm_password_empty,
            AuthValidator.validateConfirmPassword("123456", ""),
        )
        assertEquals(
            R.string.validation_passwords_do_not_match,
            AuthValidator.validateConfirmPassword("123456", "123457"),
        )
        assertNull(AuthValidator.validateConfirmPassword("123456", "123456"))
    }

    @Test
    fun termsAcceptedValidation() {
        assertEquals(R.string.validation_terms_required, AuthValidator.validateTermsAccepted(false))
        assertNull(AuthValidator.validateTermsAccepted(true))
    }

    @Test
    fun usernameValidation() {
        assertEquals(R.string.validation_username_empty, AuthValidator.validateUsername(""))
        assertEquals(R.string.validation_username_empty, AuthValidator.validateUsername("   "))
        assertEquals(R.string.validation_username_too_short, AuthValidator.validateUsername("ab"))
        assertEquals(R.string.validation_username_invalid, AuthValidator.validateUsername("user-name!"))
        assertEquals(R.string.validation_username_invalid, AuthValidator.validateUsername("משתמש"))
        assertNull(AuthValidator.validateUsername("naor_123"))
        assertNull(AuthValidator.validateUsername("@naor_123"))
        assertNull(AuthValidator.validateUsername("david_cohen"))
    }

    @Test
    fun optionalEmailValidation() {
        assertNull(AuthValidator.validateOptionalEmail(null))
        assertNull(AuthValidator.validateOptionalEmail(""))
        assertNull(AuthValidator.validateOptionalEmail("   "))
        assertEquals(R.string.validation_email_invalid, AuthValidator.validateOptionalEmail("invalid"))
        assertNull(AuthValidator.validateOptionalEmail("valid@example.com"))
    }

    @Test
    fun resetCodeValidation() {
        assertEquals(R.string.validation_code_empty, AuthValidator.validateResetCode(""))
        assertEquals(R.string.validation_code_empty, AuthValidator.validateResetCode("   "))
        assertEquals(R.string.validation_code_invalid, AuthValidator.validateResetCode("12345"))
        assertEquals(R.string.validation_code_invalid, AuthValidator.validateResetCode("1234567"))
        assertEquals(R.string.validation_code_invalid, AuthValidator.validateResetCode("12345a"))
        assertNull(AuthValidator.validateResetCode("123456"))
        assertNull(AuthValidator.validateResetCode(" 987654 "))
    }
}
