package com.shorashim.core.common

import androidx.annotation.StringRes
import com.shorashim.R
import java.util.regex.Pattern

/**
 * Validation logic for authentication fields.
 * Returns null if the value is valid, or the string resource ID of the validation error.
 */
object AuthValidator {

    private val EMAIL_PATTERN = Pattern.compile(
        "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,64}$",
        Pattern.CASE_INSENSITIVE,
    )

    @StringRes
    fun validateName(name: String): Int? {
        val trimmed = name.trim()
        return when {
            trimmed.isEmpty() -> R.string.validation_name_empty
            trimmed.length < 2 -> R.string.validation_name_too_short
            else -> null
        }
    }

    @StringRes
    fun validateEmail(email: String): Int? {
        val trimmed = email.trim()
        return when {
            trimmed.isEmpty() -> R.string.validation_email_empty
            !EMAIL_PATTERN.matcher(trimmed).matches() -> R.string.validation_email_invalid
            else -> null
        }
    }

    @StringRes
    fun validatePassword(password: String): Int? {
        return when {
            password.isEmpty() -> R.string.validation_password_empty
            password.length < 6 -> R.string.validation_password_too_short
            else -> null
        }
    }

    @StringRes
    fun validateConfirmPassword(password: String, confirmPassword: String): Int? {
        return when {
            confirmPassword.isEmpty() -> R.string.validation_confirm_password_empty
            password != confirmPassword -> R.string.validation_passwords_do_not_match
            else -> null
        }
    }

    @StringRes
    fun validateTermsAccepted(accepted: Boolean): Int? {
        return if (!accepted) {
            R.string.validation_terms_required
        } else {
            null
        }
    }
}
