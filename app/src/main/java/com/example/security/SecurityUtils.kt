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

    private var lastGeneratedTimestamp = 0L
    private val timestampLock = Any()

    /**
     * Wraps the payload with security headers containing the client identifier,
     * timestamp, and HMAC-SHA256 token signature.
     * Complies with section 3 of the integration spec by using a compact JSON string
     * representation where the "payload" field contains the exact compact string signed.
     * Generates a fresh timestamp per outbound send attempt, or accepts an explicit timestamp.
     */
    fun securePayload(
        clientId: String,
        secretToken: String,
        payloadJson: String,
        explicitTimestamp: Long? = null
    ): String {
        val timestamp = explicitTimestamp ?: synchronized(timestampLock) {
            val now = System.currentTimeMillis()
            val ts = if (now <= lastGeneratedTimestamp) lastGeneratedTimestamp + 1 else now
            lastGeneratedTimestamp = ts
            ts
        }
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
     * Builds a brand-new signed envelope with a fresh timestamp and HMAC-SHA256 signature
     * recomputed over that fresh timestamp and payload.
     */
    fun buildSecureEnvelope(
        clientId: String,
        secretToken: String,
        payloadJson: String,
        explicitTimestamp: Long? = null
    ): String {
        return securePayload(clientId, secretToken, payloadJson, explicitTimestamp)
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
            java.security.MessageDigest.isEqual(incomingSig.toByteArray(Charsets.UTF_8), expectedSig.toByteArray(Charsets.UTF_8))
        } catch (e: Exception) {
            false
        }
    }
}
