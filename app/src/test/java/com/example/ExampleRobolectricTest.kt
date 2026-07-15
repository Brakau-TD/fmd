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
}
