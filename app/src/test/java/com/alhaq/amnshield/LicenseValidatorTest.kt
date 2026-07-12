package com.alhaq.amnshield

import com.alhaq.amnshield.premium.LicensePayload
import com.alhaq.amnshield.premium.LicenseValidator
import com.google.gson.Gson
import org.junit.Assert.*
import org.junit.Test
import java.security.KeyFactory
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Base64

class LicenseValidatorTest {

    private val privateKeyBase64 = "MEECAQAwEwYHKoZIzj0CAQYIKoZIzj0DAQcEJzAlAgEBBCADPX9tuEQ27m1lup+nj/tar6XV7sqbp3IHCWE/7Yh1qQ=="

    @Test
    fun testLicenseVerificationSuccess() {
        val email = "valid-user@alhaq.org"
        val expires = System.currentTimeMillis() + 1000 * 60 * 60 // 1 hour in future
        val type = "lifetime"
        val payload = LicensePayload(email, type, expires)

        val licenseString = generateLicenseString(payload)
        val verifiedPayload = LicenseValidator.verifyLicense(licenseString)

        assertNotNull(verifiedPayload)
        assertEquals(email, verifiedPayload?.email)
        assertEquals(type, verifiedPayload?.type)
        assertEquals(expires, verifiedPayload?.expires)
    }

    @Test
    fun testLicenseVerificationExpired() {
        val email = "expired-user@alhaq.org"
        val expires = System.currentTimeMillis() - 1000 * 60 * 60 // 1 hour in past
        val type = "yearly"
        val payload = LicensePayload(email, type, expires)

        val licenseString = generateLicenseString(payload)
        val verifiedPayload = LicenseValidator.verifyLicense(licenseString)

        assertNull("Expired licenses must return null", verifiedPayload)
    }

    @Test
    fun testLicenseVerificationInvalidSignature() {
        val email = "hacker@evil.com"
        val expires = System.currentTimeMillis() + 1000 * 60 * 60
        val type = "lifetime"
        val payload = LicensePayload(email, type, expires)

        val licenseString = generateLicenseString(payload)
        
        // Corrupt the signature part (after the dot)
        val parts = licenseString.split(".")
        val corruptedLicense = parts[0] + "." + parts[1].reversed()

        val verifiedPayload = LicenseValidator.verifyLicense(corruptedLicense)
        assertNull("Corrupted signatures must fail verification", verifiedPayload)
    }

    @Test
    fun testLicenseVerificationInvalidPayload() {
        // Tamper with the payload data
        val payloadJson = """{"email":"user@alhaq.org","type":"lifetime","expires":2815412196091}"""
        val payloadBase64 = Base64.getEncoder().encodeToString(payloadJson.toByteArray(Charsets.UTF_8))

        // Create a signature for this payload
        val sig = Signature.getInstance("SHA256withECDSA")
        val privateKeyBytes = Base64.getDecoder().decode(privateKeyBase64)
        val keySpec = PKCS8EncodedKeySpec(privateKeyBytes)
        val keyFactory = KeyFactory.getInstance("EC")
        val privateKey = keyFactory.generatePrivate(keySpec)
        
        sig.initSign(privateKey)
        sig.update(payloadBase64.toByteArray(Charsets.UTF_8))
        val signatureBase64 = Base64.getEncoder().encodeToString(sig.sign())

        val licenseString = "$payloadBase64.$signatureBase64"

        // Verification of untampered license
        assertNotNull(LicenseValidator.verifyLicense(licenseString))

        // Tamper with payload text
        val tamperedPayloadJson = """{"email":"hacker@alhaq.org","type":"lifetime","expires":2815412196091}"""
        val tamperedPayloadBase64 = Base64.getEncoder().encodeToString(tamperedPayloadJson.toByteArray(Charsets.UTF_8))
        val tamperedLicenseString = "$tamperedPayloadBase64.$signatureBase64"

        assertNull("Tampered payloads must fail signature verification", LicenseValidator.verifyLicense(tamperedLicenseString))
    }

    private fun generateLicenseString(payload: LicensePayload): String {
        val gson = Gson()
        val payloadJson = gson.toJson(payload)
        val payloadBase64 = Base64.getEncoder().encodeToString(payloadJson.toByteArray(Charsets.UTF_8))

        val sig = Signature.getInstance("SHA256withECDSA")
        val privateKeyBytes = Base64.getDecoder().decode(privateKeyBase64)
        val keySpec = PKCS8EncodedKeySpec(privateKeyBytes)
        val keyFactory = KeyFactory.getInstance("EC")
        val privateKey = keyFactory.generatePrivate(keySpec)

        sig.initSign(privateKey)
        sig.update(payloadBase64.toByteArray(Charsets.UTF_8))
        val signatureBytes = sig.sign()
        val signatureBase64 = Base64.getEncoder().encodeToString(signatureBytes)

        return "$payloadBase64.$signatureBase64"
    }
}
