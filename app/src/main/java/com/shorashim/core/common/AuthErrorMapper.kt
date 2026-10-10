package com.shorashim.core.common

import androidx.annotation.StringRes
import com.shorashim.R
import io.github.jan.supabase.auth.exception.AuthErrorCode
import io.github.jan.supabase.auth.exception.AuthRestException
import io.github.jan.supabase.exceptions.HttpRequestException
import io.github.jan.supabase.exceptions.RestException

/**
 * Maps Supabase authentication exceptions and error messages to user-friendly Hebrew string resource IDs.
 */
object AuthErrorMapper {

    @StringRes
    fun mapThrowable(throwable: Throwable?): Int {
        if (throwable == null) return R.string.auth_error_generic

        if (throwable is SupabaseNotConfiguredException) {
            return R.string.auth_error_not_configured
        }

        if (throwable is HttpRequestException) {
            return R.string.auth_error_network
        }

        if (throwable is AuthRestException) {
            return when (throwable.errorCode) {
                AuthErrorCode.InvalidCredentials,
                AuthErrorCode.UserNotFound -> R.string.auth_error_invalid_credentials

                AuthErrorCode.EmailNotConfirmed,
                AuthErrorCode.ProviderEmailNeedsVerification -> R.string.auth_error_email_not_confirmed

                AuthErrorCode.EmailExists,
                AuthErrorCode.UserAlreadyExists -> R.string.auth_error_email_already_in_use

                AuthErrorCode.WeakPassword -> R.string.auth_error_weak_password

                AuthErrorCode.OverRequestRateLimit,
                AuthErrorCode.OverEmailSendRateLimit -> R.string.auth_error_too_many_requests

                AuthErrorCode.UserBanned -> R.string.auth_error_user_disabled

                else -> {
                    val combined = buildString {
                        append(throwable.errorCode?.name.orEmpty())
                        append(" ")
                        append(throwable.errorDescription.orEmpty())
                        append(" ")
                        append(throwable.description.orEmpty())
                        append(" ")
                        append(throwable.error)
                        append(" ")
                        append(throwable.message.orEmpty())
                    }
                    mapErrorMessage(combined)
                }
            }
        }

        val combinedMessage = buildString {
            if (throwable is RestException) {
                append(throwable.error)
                append(" ")
                append(throwable.description.orEmpty())
                append(" ")
            }
            append(throwable.message.orEmpty())
            throwable.cause?.let {
                append(" ")
                append(it.message.orEmpty())
            }
        }

        return mapErrorMessage(combinedMessage)
    }

    @StringRes
    fun mapErrorMessage(message: String?): Int {
        if (message.isNullOrBlank()) return R.string.auth_error_generic

        return when {
            message.contains("email_not_confirmed", ignoreCase = true) ||
            message.contains("Email not confirmed", ignoreCase = true) -> R.string.auth_error_email_not_confirmed

            message.contains("Invalid login credentials", ignoreCase = true) ||
            message.contains("invalid_grant", ignoreCase = true) ||
            message.contains("invalid_credentials", ignoreCase = true) ||
            message.contains("wrong password", ignoreCase = true) ||
            message.contains("user not found", ignoreCase = true) -> R.string.auth_error_invalid_credentials

            message.contains("already registered", ignoreCase = true) ||
            message.contains("already exists", ignoreCase = true) ||
            message.contains("user_already_exists", ignoreCase = true) -> R.string.auth_error_email_already_in_use

            message.contains("at least 6 characters", ignoreCase = true) ||
            message.contains("weak_password", ignoreCase = true) ||
            message.contains("password is too short", ignoreCase = true) -> R.string.auth_error_weak_password

            message.contains("rate limit", ignoreCase = true) ||
            message.contains("over_request_rate_limit", ignoreCase = true) ||
            message.contains("over_email_send_rate_limit", ignoreCase = true) ||
            message.contains("security purposes", ignoreCase = true) ||
            message.contains("too many requests", ignoreCase = true) -> R.string.auth_error_too_many_requests

            message.contains("network", ignoreCase = true) ||
            message.contains("Unable to resolve host", ignoreCase = true) ||
            message.contains("ConnectException", ignoreCase = true) ||
            message.contains("SocketTimeout", ignoreCase = true) -> R.string.auth_error_network

            message.contains("not configured", ignoreCase = true) ||
            message.contains(".env", ignoreCase = true) -> R.string.auth_error_not_configured

            else -> R.string.auth_error_generic
        }
    }
}

class SupabaseNotConfiguredException(
    message: String = "Supabase is not configured. Please add SUPABASE_URL and SUPABASE_ANON_KEY to .env.",
) : IllegalStateException(message)
