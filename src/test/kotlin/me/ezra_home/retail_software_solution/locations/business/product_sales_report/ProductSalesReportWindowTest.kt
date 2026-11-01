package me.ezra_home.retail_software_solution.locations.business.product_sales_report

import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

class ProductSalesReportWindowTest {

  private val firstDay = LocalDate.of(2030, 6, 1)

  @Test
  fun `both dates are part of the window`() {
    val window = ProductSalesReportWindow.ofInclusiveDates(firstDay, firstDay.plusDays(2))

    assertEquals(ProductSalesReportWindow(firstDay, firstDay.plusDays(3)), window)
    assertEquals(listOf(firstDay, firstDay.plusDays(1), firstDay.plusDays(2)), window.days)
  }

  @Test
  fun `one date is a window of one day`() {
    assertEquals(listOf(firstDay), ProductSalesReportWindow.ofInclusiveDates(firstDay, firstDay).days)
  }

  @Test
  fun `a missing or inverted date is rejected`() {
    assertThrows<RtsGenericException> { ProductSalesReportWindow.ofInclusiveDates(null, firstDay) }
    assertThrows<RtsGenericException> { ProductSalesReportWindow.ofInclusiveDates(firstDay, null) }
    assertThrows<RtsGenericException> { ProductSalesReportWindow.ofInclusiveDates(firstDay.plusDays(1), firstDay) }
  }

  @Test
  fun `a range of a whole year or more is rejected`() {
    ProductSalesReportWindow.ofInclusiveDates(firstDay, firstDay.plusYears(1).minusDays(1))

    assertThrows<RtsGenericException> { ProductSalesReportWindow.ofInclusiveDates(firstDay, firstDay.plusYears(1)) }
  }
}
