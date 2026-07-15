package com.example.security

import android.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object SecurityUtils {
    /**
     * Generates a HMAC-SHA256 signature using the secret paired token as the key.
     * This secures every outgoing request payload by confirming authenticity.
     */
    fun generateHmacSignature(data: String, secret: String): String {
        return try {
            val keyBytes = secret.toByteArray(Charsets.UTF_8)
            val secretKey = SecretKeySpec(keyBytes, "HmacSHA256")
            val mac = Mac.getInstance("HmacSHA256")
            mac.init(secretKey)
            val hmacBytes = mac.doFinal(data.toByteArray(Charsets.UTF_8))
            Base64.encodeToString(hmacBytes, Base64.NO_WRAP or Base64.URL_SAFE or Base64.NO_PADDING).trim()
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Wraps the payload with security headers containing the client identifier,
     * timestamp, and HMAC-SHA256 token signature.
     * Complies with section 3 of the integration spec by using a compact JSON string
     * representation where the "payload" field contains the exact compact string signed.
     */
    fun securePayload(clientId: String, secretToken: String, payloadJson: String): String {
        val timestamp = System.currentTimeMillis()
        val dataToSign = "$clientId|$timestamp|$payloadJson"
        val signature = generateHmacSignature(dataToSign, secretToken)
        
        val envelope = org.json.JSONObject()
        envelope.put("clientId", clientId)
        envelope.put("timestamp", timestamp)
        envelope.put("signature", signature)
        envelope.put("payload", payloadJson)
        return envelope.toString()
    }

    /**
     * Verifies if an incoming command's signature matches.
     * Only accepts the canonical form: clientId|timestamp|payload string layout
     * with base64url-no-padding signature encoding.
     */
    fun verifySignature(
        clientId: String,
        timestamp: Long,
        payload: String,
        signature: String,
        secretToken: String,
        originalJson: String? = null
    ): Boolean {
        val incomingSig = signature.trim()
        if (incomingSig.isEmpty()) return false

        val dataToSign = "$clientId|$timestamp|$payload"
        return try {
            val keyBytes = secretToken.toByteArray(Charsets.UTF_8)
            val secretKey = SecretKeySpec(keyBytes, "HmacSHA256")
            val mac = Mac.getInstance("HmacSHA256")
            mac.init(secretKey)
            val hmacBytes = mac.doFinal(dataToSign.toByteArray(Charsets.UTF_8))
            val expectedSig = Base64.encodeToString(hmacBytes, Base64.NO_WRAP or Base64.URL_SAFE or Base64.NO_PADDING).trim()
            incomingSig == expectedSig
        } catch (e: Exception) {
            false
        }
    }
}
