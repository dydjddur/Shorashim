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

    private val USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{3,30}$")

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
    fun validateUsername(username: String): Int? {
        val clean = username.trim().removePrefix("@")
        return when {
            clean.isEmpty() -> R.string.validation_username_empty
            clean.length < 3 -> R.string.validation_username_too_short
            !USERNAME_PATTERN.matcher(clean).matches() -> R.string.validation_username_invalid
            else -> null
        }
    }

    @StringRes
    fun validateLoginIdentifier(identifier: String): Int? {
        val trimmed = identifier.trim()
        return if (trimmed.isEmpty()) {
            R.string.validation_username_empty
        } else null
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
    fun validateOptionalEmail(email: String?): Int? {
        if (email.isNullOrBlank()) return null
        val trimmed = email.trim()
        return if (!EMAIL_PATTERN.matcher(trimmed).matches()) {
            R.string.validation_email_invalid
        } else null
    }

    @StringRes
    fun validateResetCode(code: String): Int? {
        val trimmed = code.trim()
        return when {
            trimmed.isEmpty() -> R.string.validation_code_empty
            trimmed.length != 6 || !trimmed.all { it.isDigit() } -> R.string.validation_code_invalid
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
