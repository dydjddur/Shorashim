package com.shorashim.core.common

import java.net.URI

/**
 * Validates and extracts tokens from supported Shorashim family tree invitation URLs.
 *
 * Exact requirement:
 * Only treat a URL as a Shorashim family-tree invitation when it matches this exact HTTPS URL prefix:
 * https://shorashim-invite.vercel.app/invite
 *
 * Rejects lookalike domains, unexpected subdomains, HTTP links, malformed URLs, and unrelated paths.
 */
object InvitationUrlValidator {
    const val EXPECTED_PREFIX = "https://shorashim-invite.vercel.app/invite"
    private const val EXPECTED_SCHEME = "https"
    private const val EXPECTED_HOST = "shorashim-invite.vercel.app"
    private const val EXPECTED_PATH_PREFIX = "/invite"

    /**
     * Structurally validates the given URL against the exact required origin and path.
     */
    fun isValidInvitationUrl(rawUrl: String?): Boolean {
        if (rawUrl.isNullOrBlank()) return false
        val token = extractToken(rawUrl)
        return !token.isNullOrBlank()
    }

    /**
     * Extracts the invitation token from a validated URL, or returns null if invalid.
     */
    fun extractToken(rawUrl: String): String? {
        val trimmed = rawUrl.trim()
        if (!trimmed.startsWith(EXPECTED_PREFIX, ignoreCase = false)) {
            return null
        }

        return try {
            val uri = URI(trimmed)
            if (uri.scheme?.lowercase() != EXPECTED_SCHEME) return null
            if (uri.host?.lowercase() != EXPECTED_HOST) return null
            if (uri.port != -1 && uri.port != 443) return null

            val path = uri.path ?: return null
            if (!path.startsWith(EXPECTED_PATH_PREFIX)) return null

            val remainingPath = path.removePrefix(EXPECTED_PATH_PREFIX).trim()
            val token = remainingPath.removePrefix("/").trim()

            // Token must be non-empty and have valid path characters (no sub-paths)
            if (token.isEmpty() || token.contains("/")) return null
            token
        } catch (e: Throwable) {
            null
        }
    }

    /**
     * Constructs a valid shareable invitation URL from a token.
     */
    fun buildInvitationUrl(token: String): String {
        return "$EXPECTED_PREFIX/${token.trim()}"
    }
}
