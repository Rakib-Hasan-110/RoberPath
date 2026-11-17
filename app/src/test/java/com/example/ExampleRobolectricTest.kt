package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.RoberPathCatalog
import com.example.ui.viewmodel.AgencyViewModel
import com.example.util.PdfReportGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    assertEquals("Rober Path", appName)
  }

  @Test
  fun `catalog has official 2026-27 packages and 14 day itinerary`() {
    assertEquals(7, RoberPathCatalog.defaultPackages.size)
    assertEquals(139500L, RoberPathCatalog.defaultPackages.first().priceBdt)
    assertEquals(14, RoberPathCatalog.fourteenDayItinerary.size)
  }

  @Test
  fun `currency formatting and passport 6-month check work`() {
    assertEquals("৳1,65,000", PdfReportGenerator.formatAmount(165000L, "BDT", 32.0))
    assertEquals("5,156 SAR", PdfReportGenerator.formatAmount(165000L, "SAR", 32.0))
    assertTrue(AgencyViewModel.isPassportExpiringSoon("2026-12-01", 6))
    assertFalse(AgencyViewModel.isPassportExpiringSoon("2029-05-01", 6))
  }
}
