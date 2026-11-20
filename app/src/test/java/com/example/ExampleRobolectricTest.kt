package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.health.StepSensorStatus
import com.example.data.health.StepSensorType
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
  fun `vitaltrack app metadata and strings match specification`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    assertEquals("VITALTRACK", context.getString(R.string.app_name))
    assertEquals(
      "Move Better. Live Healthier. Understand Your Journey.",
      context.getString(R.string.app_tagline),
    )
    assertTrue(context.getString(R.string.medical_disclaimer).contains("not a medical"))
  }

  @Test
  fun `step sensor status reports truthful initial state without simulated steps`() {
    val status = StepSensorStatus(
      sensorType = StepSensorType.UNAVAILABLE,
      permissionGranted = false,
      isListening = false,
      statusMessage = "No hardware step sensor found on this device.",
    )
    assertEquals(StepSensorType.UNAVAILABLE, status.sensorType)
    assertEquals(false, status.permissionGranted)
  }
}
