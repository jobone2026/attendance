package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.domain.location.GeofenceValidator
import com.example.domain.ml.FaceMatcher
import com.example.domain.time.TimeWindowValidator
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun read_string_from_context() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Teacher Attendance", appName)
  }

  @Test
  fun test_face_matcher_cosine_similarity() {
    val matcher = FaceMatcher()
    val vecA = listOf(1.0f, 0.0f, 0.0f)
    val vecB = listOf(1.0f, 0.0f, 0.0f)
    val vecC = listOf(0.0f, 1.0f, 0.0f)

    assertEquals(1.0f, matcher.cosineSimilarity(vecA, vecB), 0.01f)
    assertEquals(0.0f, matcher.cosineSimilarity(vecA, vecC), 0.01f)
  }

  @Test
  fun test_haversine_distance_calculation() {
    val validator = GeofenceValidator()
    // Bangalore to Mysore approx ~128 km
    val dist = validator.calculateDistanceMeters(12.9716, 77.5946, 12.2958, 76.6394)
    assertTrue(dist > 100000)
  }
}
