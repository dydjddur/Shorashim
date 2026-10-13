package com.shorashim

import com.shorashim.core.common.InvitationUrlValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InvitationUrlValidatorTest {

    @Test
    fun testValidInvitationUrl() {
        val validUrl = "https://shorashim-invite.vercel.app/invite/abc-123-xyz"
        assertTrue(InvitationUrlValidator.isValidInvitationUrl(validUrl))
        assertEquals("abc-123-xyz", InvitationUrlValidator.extractToken(validUrl))
    }

    @Test
    fun testValidInvitationUrlWithWhitespace() {
        val validUrlWithSpaces = "  https://shorashim-invite.vercel.app/invite/token_456  "
        assertTrue(InvitationUrlValidator.isValidInvitationUrl(validUrlWithSpaces))
        assertEquals("token_456", InvitationUrlValidator.extractToken(validUrlWithSpaces))
    }

    @Test
    fun testRejectInsecureHttp() {
        val httpUrl = "http://shorashim-invite.vercel.app/invite/abc-123-xyz"
        assertFalse(InvitationUrlValidator.isValidInvitationUrl(httpUrl))
        assertNull(InvitationUrlValidator.extractToken(httpUrl))
    }

    @Test
    fun testRejectLookalikeDomain() {
        val lookalike1 = "https://shorashim-invite.vercel.app.evil.com/invite/abc-123"
        val lookalike2 = "https://evil-shorashim-invite.vercel.app/invite/abc-123"
        val lookalike3 = "https://shorashim-invite.vercel.org/invite/abc-123"

        assertFalse(InvitationUrlValidator.isValidInvitationUrl(lookalike1))
        assertFalse(InvitationUrlValidator.isValidInvitationUrl(lookalike2))
        assertFalse(InvitationUrlValidator.isValidInvitationUrl(lookalike3))
    }

    @Test
    fun testRejectWrongPathOrSubpaths() {
        val wrongPath = "https://shorashim-invite.vercel.app/trees/abc-123"
        val subpath = "https://shorashim-invite.vercel.app/invite/abc/extra/path"
        val emptyToken = "https://shorashim-invite.vercel.app/invite/"

        assertFalse(InvitationUrlValidator.isValidInvitationUrl(wrongPath))
        assertFalse(InvitationUrlValidator.isValidInvitationUrl(subpath))
        assertFalse(InvitationUrlValidator.isValidInvitationUrl(emptyToken))
    }

    @Test
    fun testBuildInvitationUrl() {
        val token = "test-token-789"
        val built = InvitationUrlValidator.buildInvitationUrl(token)
        assertEquals("https://shorashim-invite.vercel.app/invite/test-token-789", built)
        assertTrue(InvitationUrlValidator.isValidInvitationUrl(built))
    }
}
