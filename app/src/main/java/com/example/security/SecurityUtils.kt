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
     * Supports multiple common signature payload formats, whitespace patterns,
     * and signature encodings (Base64 standard, Base64 URL-safe, and Hexadecimal).
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

        // 1. Gather all candidate payload strings
        val candidatePayloads = mutableSetOf<String>()
        candidatePayloads.add(payload)
        candidatePayloads.add(payload.trim())
        
        // Try compact payload format (no spaces around : and ,)
        val compactPayload = payload.replace(" ", "")
        candidatePayloads.add(compactPayload)

        // Try standard json formatting variations
        candidatePayloads.add(payload.replace(": ", ":").replace(", ", ","))

        if (originalJson != null) {
            val rawExtracted = extractRawPayload(originalJson)
            if (rawExtracted != null) {
                candidatePayloads.add(rawExtracted)
                candidatePayloads.add(rawExtracted.trim())
                candidatePayloads.add(rawExtracted.replace(" ", ""))
                candidatePayloads.add(rawExtracted.replace(": ", ":").replace(", ", ","))
            }
        }

        // 2. Gather all candidate dataToSign structures
        val candidateStringsToSign = mutableSetOf<String>()
        for (p in candidatePayloads) {
            // Standard format
            candidateStringsToSign.add("$clientId|$timestamp|$p")
            // Formatting without pipes
            candidateStringsToSign.add("$clientId$timestamp$p")
            // Formatting with swapped timestamp and client ID
            candidateStringsToSign.add("$timestamp|$clientId|$p")
            // Formatting without client ID
            candidateStringsToSign.add("$timestamp|$p")
            // Formatting with payload only
            candidateStringsToSign.add(p)
        }

        // 3. For each candidate, generate HMAC and check all possible encodings
        for (dataToSign in candidateStringsToSign) {
            val hmacBytes = try {
                val keyBytes = secretToken.toByteArray(Charsets.UTF_8)
                val secretKey = SecretKeySpec(keyBytes, "HmacSHA256")
                val mac = Mac.getInstance("HmacSHA256")
                mac.init(secretKey)
                mac.doFinal(dataToSign.toByteArray(Charsets.UTF_8))
            } catch (e: Exception) {
                null
            }

            if (hmacBytes != null) {
                val hexLower = bytesToHex(hmacBytes)
                val hexUpper = hexLower.uppercase()
                
                val b64UrlSafeNoWrap = Base64.encodeToString(hmacBytes, Base64.NO_WRAP or Base64.URL_SAFE).trim()
                val b64UrlSafeNoWrapNoPadding = Base64.encodeToString(hmacBytes, Base64.NO_WRAP or Base64.URL_SAFE or Base64.NO_PADDING).trim()
                val b64StandardNoWrap = Base64.encodeToString(hmacBytes, Base64.NO_WRAP).trim()
                val b64StandardNoWrapNoPadding = Base64.encodeToString(hmacBytes, Base64.NO_WRAP or Base64.NO_PADDING).trim()

                if (incomingSig == hexLower ||
                    incomingSig == hexUpper ||
                    incomingSig == b64UrlSafeNoWrap ||
                    incomingSig == b64UrlSafeNoWrapNoPadding ||
                    incomingSig == b64StandardNoWrap ||
                    incomingSig == b64StandardNoWrapNoPadding
                ) {
                    return true
                }
            }
        }

        return false
    }

    private fun bytesToHex(bytes: ByteArray): String {
        val hexChars = CharArray(bytes.size * 2)
        val hexArray = "0123456789abcdef".toCharArray()
        for (i in bytes.indices) {
            val v = bytes[i].toInt() and 0xFF
            hexChars[i * 2] = hexArray[v ushr 4]
            hexChars[i * 2 + 1] = hexArray[v and 0x0F]
        }
        return String(hexChars)
    }

    private fun extractRawPayload(jsonText: String): String? {
        try {
            val payloadKeyIndex = jsonText.indexOf("\"payload\"")
            if (payloadKeyIndex == -1) return null
            
            val colonIndex = jsonText.indexOf(":", payloadKeyIndex)
            if (colonIndex == -1) return null
            
            var valueStartIndex = colonIndex + 1
            while (valueStartIndex < jsonText.length && jsonText[valueStartIndex].isWhitespace()) {
                valueStartIndex++
            }
            if (valueStartIndex >= jsonText.length) return null
            
            val firstChar = jsonText[valueStartIndex]
            if (firstChar == '"') {
                var inEscape = false
                for (i in (valueStartIndex + 1) until jsonText.length) {
                    val char = jsonText[i]
                    if (inEscape) {
                        inEscape = false
                    } else if (char == '\\') {
                        inEscape = true
                    } else if (char == '"') {
                        return jsonText.substring(valueStartIndex + 1, i)
                    }
                }
            } else if (firstChar == '{') {
                var braceCount = 0
                var inQuote = false
                var inEscape = false
                for (i in valueStartIndex until jsonText.length) {
                    val char = jsonText[i]
                    if (inQuote) {
                        if (inEscape) {
                            inEscape = false
                        } else if (char == '\\') {
                            inEscape = true
                        } else if (char == '"') {
                            inQuote = false
                        }
                    } else {
                        if (char == '"') {
                            inQuote = true
                        } else if (char == '{') {
                            braceCount++
                        } else if (char == '}') {
                            braceCount--
                            if (braceCount == 0) {
                                return jsonText.substring(valueStartIndex, i + 1)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Ignored
        }
        return null
    }
}
