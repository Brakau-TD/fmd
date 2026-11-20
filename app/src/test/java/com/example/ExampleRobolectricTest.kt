package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.security.SecurityUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Find My Device", appName)
  }

  @Test
  fun `verify hmac signatures with various formats`() {
    val clientId = "device-123"
    val timestamp = 1718000000L
    val payload = "{\"action\":\"test_alert\"}"
    val secretToken = "supersecret123"

    // 1. Generate base signature using standard HMAC-SHA256
    val dataToSign = "$clientId|$timestamp|$payload"
    val signature = SecurityUtils.generateHmacSignature(dataToSign, secretToken)

    // 2. Verify using our robust verifier
    val isValid = SecurityUtils.verifySignature(
      clientId = clientId,
      timestamp = timestamp,
      payload = payload,
      signature = signature,
      secretToken = secretToken
    )
    assertTrue("Signature verification should succeed", isValid)
  }

  @Test
  fun `buildSecureEnvelope creates valid JSON envelope with fresh timestamp and signature`() {
    val clientId = "device-123"
    val secretToken = "supersecret123"
    val payloadJson = "{\"type\":\"location\",\"lat\":37.7749,\"lon\":-122.4194}"

    val envelopeStr = SecurityUtils.buildSecureEnvelope(clientId, secretToken, payloadJson)
    val envelopeJson = org.json.JSONObject(envelopeStr)

    assertEquals(clientId, envelopeJson.getString("clientId"))
    assertEquals(payloadJson, envelopeJson.getString("payload"))
    assertTrue("Timestamp should be positive epoch ms", envelopeJson.getLong("timestamp") > 0)
    
    val timestamp = envelopeJson.getLong("timestamp")
    val signature = envelopeJson.getString("signature")
    assertTrue("Signature should not be empty", signature.isNotEmpty())

    // Verify signature
    val isValid = SecurityUtils.verifySignature(
      clientId = clientId,
      timestamp = timestamp,
      payload = payloadJson,
      signature = signature,
      secretToken = secretToken
    )
    assertTrue("Generated envelope signature must verify correctly", isValid)
  }

  @Test
  fun `consecutive sends of identical payloads have fresh timestamps and distinct signatures avoiding replay`() {
    val clientId = "device-456"
    val secretToken = "heartbeat-secret"
    val payloadJson = "{\"type\":\"heartbeat\",\"status\":\"stationary\"}"

    // Send 1 (e.g. heartbeat or first attempt)
    val envelopeStr1 = SecurityUtils.buildSecureEnvelope(clientId, secretToken, payloadJson)
    val json1 = org.json.JSONObject(envelopeStr1)
    val timestamp1 = json1.getLong("timestamp")
    val signature1 = json1.getString("signature")

    // Send 2 (e.g. retry or next heartbeat attempt)
    val envelopeStr2 = SecurityUtils.buildSecureEnvelope(clientId, secretToken, payloadJson)
    val json2 = org.json.JSONObject(envelopeStr2)
    val timestamp2 = json2.getLong("timestamp")
    val signature2 = json2.getString("signature")

    // Timestamps must be strictly different
    assertTrue("Successive sends must have different timestamps", timestamp2 > timestamp1)
    // Signatures must be strictly distinct to avoid replay rejection
    org.junit.Assert.assertNotEquals("Signatures must not be identical across successive sends", signature1, signature2)
  }

  @Test
  fun `parseEndpoint handles host, host-with-port, and full URLs for target server switching`() {
    val basic = TrackerViewModel.parseEndpoint("192.168.1.50", "8080")
    assertEquals("192.168.1.50", basic.host)
    assertEquals(8080, basic.port)
    assertEquals("", basic.websocketUrl)

    val inlinePort = TrackerViewModel.parseEndpoint("10.0.0.12:9090", "8080")
    assertEquals("10.0.0.12", inlinePort.host)
    assertEquals(9090, inlinePort.port)

    val secureUrl = TrackerViewModel.parseEndpoint("https://tracker.example.com", "8080")
    assertEquals("tracker.example.com", secureUrl.host)
    assertEquals(443, secureUrl.port)

    val customWs = TrackerViewModel.parseEndpoint("wss://portal.example.com/ws/custom", "8080")
    assertEquals("portal.example.com", customWs.host)
    assertEquals(443, customWs.port)
    assertEquals("wss://portal.example.com/ws/custom", customWs.websocketUrl)
  }
}

