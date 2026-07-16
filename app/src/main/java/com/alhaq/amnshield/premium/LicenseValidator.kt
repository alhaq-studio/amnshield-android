package com.alhaq.amnshield.premium

import com.google.gson.Gson
import java.security.KeyFactory
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

object LicenseValidator {
    // NIST P-256 (secp256r1) Public Key keyring in X.509 format (Base64)
    private val KEYRING = mapOf(
        1 to "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE7EFR1qxpfZTMeR52M1+04+tPb6ItmVmhPbRCIJYje3jtglTdBbcct+/xvc1D1NZtXuvSb4Egtdqm/EJ6H67fEA=="
    )

    fun verifyLicense(licenseString: String): LicensePayload? {
        try {
            val parts = licenseString.split(".")
            if (parts.size != 2) return null

            val payloadBase64 = parts[0]
            val signatureBase64 = parts[1]

            val payloadJson = String(Base64.getDecoder().decode(payloadBase64), Charsets.UTF_8)
            val signatureBytes = Base64.getDecoder().decode(signatureBase64)

            // Parse payload
            val gson = Gson()
            val payload = gson.fromJson(payloadJson, LicensePayload::class.java) ?: return null

            // Check expiration
            if (payload.expires < System.currentTimeMillis()) {
                return null
            }

            // Retrieve the public key corresponding to the payload schema version
            val publicKeyBase64 = KEYRING[payload.version] ?: return null

            // Verify signature
            val publicKeyBytes = Base64.getDecoder().decode(publicKeyBase64)
            val keySpec = X509EncodedKeySpec(publicKeyBytes)
            val keyFactory = KeyFactory.getInstance("EC")
            val publicKey = keyFactory.generatePublic(keySpec)

            val sig = Signature.getInstance("SHA256withECDSA")
            sig.initVerify(publicKey)
            sig.update(Base64.getDecoder().decode(payloadBase64))

            if (sig.verify(signatureBytes)) {
                return payload
            }
        } catch (e: Exception) {
            // Quietly fail or log
        }
        return null
    }

    // Return the default public key for debug and backward compatibility
    val debugPublicKey: String
        get() = KEYRING[1] ?: ""
}
