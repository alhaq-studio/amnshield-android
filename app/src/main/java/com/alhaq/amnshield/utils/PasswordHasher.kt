package com.alhaq.amnshield.utils

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Lightweight password hashing for the anti-uninstall password.
 *
 * Format: "v2$<base64-salt>$<base64-sha256(salt|password)>"
 *
 * Notes:
 * - Stored values are local-only and never transmitted.
 * - The legacy format (no prefix) is still accepted via [verify] so existing
 *   users are not locked out; their stored value is silently upgraded to v2
 *   on the next successful verification.
 * - SHA-256 with a 16-byte random salt is sufficient for a local 4+ digit PIN
 *   given Android sandboxing; this hashing primarily prevents trivial leakage
 *   via accidental backup, log capture, or rooted-device prefs inspection.
 */
object PasswordHasher {

    private const val PREFIX = "v2$"
    private const val SALT_BYTES = 16

    fun hash(password: String): String {
        val salt = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }
        val digest = sha256(salt + password.toByteArray(Charsets.UTF_8))
        val saltB64 = Base64.encodeToString(salt, Base64.NO_WRAP or Base64.URL_SAFE or Base64.NO_PADDING)
        val hashB64 = Base64.encodeToString(digest, Base64.NO_WRAP or Base64.URL_SAFE or Base64.NO_PADDING)
        return "$PREFIX$saltB64\$$hashB64"
    }

    fun verify(input: String, stored: String?): Boolean {
        if (stored.isNullOrEmpty()) return false
        return if (stored.startsWith(PREFIX)) {
            val parts = stored.removePrefix(PREFIX).split('$')
            if (parts.size != 2) return false
            val salt = runCatching {
                Base64.decode(parts[0], Base64.NO_WRAP or Base64.URL_SAFE or Base64.NO_PADDING)
            }.getOrNull() ?: return false
            val expected = runCatching {
                Base64.decode(parts[1], Base64.NO_WRAP or Base64.URL_SAFE or Base64.NO_PADDING)
            }.getOrNull() ?: return false
            val actual = sha256(salt + input.toByteArray(Charsets.UTF_8))
            constantTimeEquals(expected, actual)
        } else {
            // Legacy plaintext path: callers should rewrite to hashed form on success.
            constantTimeEquals(stored.toByteArray(Charsets.UTF_8), input.toByteArray(Charsets.UTF_8))
        }
    }

    fun isPlainText(stored: String?): Boolean {
        if (stored.isNullOrEmpty()) return false
        return !stored.startsWith(PREFIX)
    }

    private fun sha256(data: ByteArray): ByteArray =
        MessageDigest.getInstance("SHA-256").digest(data)

    private fun constantTimeEquals(a: ByteArray, b: ByteArray): Boolean {
        if (a.size != b.size) return false
        var result = 0
        for (i in a.indices) result = result or (a[i].toInt() xor b[i].toInt())
        return result == 0
    }
}
